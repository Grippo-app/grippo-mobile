'use strict';

// Server-owned deterministic executors for the token and component
// pipelines. Three duties:
//
//  1. tokens scope post-processing — after the figma:sync-tokens session
//     staged its strict capture, run the trusted normalizer child
//     (orchestrator/figma/runtime/run-plan.mjs, op normalize-capture) and
//     write the stage manifest the generation validator consumes.
//  2. components scope post-processing — after the figma:sync-components
//     session staged its witnessed capture (+ visual/ PNGs), run the
//     deterministic component normalizer child (op
//     normalize-component-capture) and write the stage manifest covering the
//     inventory plus its content-addressed visual evidence.
//  3. drift scope execution — "Сверить с проектом" runs with zero AI
//     sessions and zero Figma reads: the token and component comparison
//     runners are spawned as sanitized child processes over byte-verified
//     copies of the active generation's inputs; each domain fails
//     independently and honestly.
//
// Every child: fixed executable (process.execPath), fixed script path,
// shell:false, minimal env, own process group, hard timeout, bounded output.

var path = require('path');
var pathToFileURL = require('url').pathToFileURL;
var spawn = require('child_process').spawn;
var paths = require('./paths');
var fileGuards = require('./file-guards');
var generation = require('./figma-generation');
var syncErrors = require('./figma-sync-errors');
var programLimits = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'program-limits.cjs'));

// Child SCRIPTS come from the installed template (paths.js doctrine); the
// PROJECT data they operate on is passed explicitly (plan.projectRoot / env).
var RUN_PLAN_SCRIPT = path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'run-plan.mjs');
var CHILD_TIMEOUT_MS = programLimits.runnerTimeoutMs;
var CHILD_OUTPUT_MAX = programLimits.runnerOutputBytesMax;
var PLAN_FILE_MAX = programLimits.stageManifestBytesMax;
var STAGE_WRITE_MAX = programLimits.artifactBytesMax;
var TOKEN_DIR = generation.TOKEN_COMPARISON_REPORT_DIR;
var COMPONENT_DIR = generation.COMPONENT_COMPARISON_REPORT_DIR;

function atomicWrite(file, bytes, max, mode) {
  var result = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, path.dirname(file), file, bytes,
    { create: true, directoryMode: 0o700, mode: mode || 0o600, maxBytes: max });
  if (!result.ok) throw new Error(result.code || 'token-stage-write-failed');
}

// Spawn one sanitized child. Resolves { ok, timedOut, exitCode, stdout,
// stderr } — never rejects for child-side failures.
function runChild(argv, options) {
  return new Promise(function (resolvePromise) {
    var child;
    try {
      child = spawn(process.execPath, argv, {
        cwd: options && options.cwd || paths.PROJECT_ROOT,
        env: Object.assign({ PATH: '' }, options && options.env || {}),
        stdio: ['ignore', 'pipe', 'pipe'],
        shell: false,
        detached: process.platform !== 'win32'
      });
    } catch (error) {
      resolvePromise({ ok: false, timedOut: false, exitCode: null, stdout: '', stderr: String(error && error.message || error).slice(0, 500) });
      return;
    }
    var stdout = '', stderr = '', settled = false, timedOut = false;
    var timer = setTimeout(function () {
      timedOut = true;
      try {
        if (process.platform !== 'win32') process.kill(-child.pid, 'SIGKILL');
        else child.kill('SIGKILL');
      } catch (error) {}
    }, options && options.timeoutMs || CHILD_TIMEOUT_MS);
    if (typeof timer.unref === 'function') timer.unref();
    child.stdout.on('data', function (chunk) { if (stdout.length < CHILD_OUTPUT_MAX) stdout += chunk.toString('utf8'); });
    child.stderr.on('data', function (chunk) { if (stderr.length < CHILD_OUTPUT_MAX) stderr += chunk.toString('utf8'); });
    function finish(exitCode, error) {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      resolvePromise({
        ok: !timedOut && exitCode === 0 && !error,
        timedOut: timedOut,
        exitCode: exitCode,
        stdout: stdout.slice(0, CHILD_OUTPUT_MAX),
        stderr: stderr.slice(0, CHILD_OUTPUT_MAX)
      });
    }
    child.on('error', function (error) { finish(null, error); });
    child.on('close', function (code) { finish(code, null); });
  });
}

// Run the trusted runner over a plan object written into the stage directory.
// Returns the parsed result line ({ ok, code?, ... }) or a synthesized typed
// failure when the child itself broke.
function runPlan(stageDir, plan) {
  var planFile = path.join(stageDir, '.run-plan.json');
  var bytes = Buffer.from(JSON.stringify(Object.assign({}, plan, { stageDir: stageDir }), null, 2) + '\n');
  if (bytes.length > PLAN_FILE_MAX) return Promise.resolve({ ok: false, code: 'RUN_PLAN_INVALID', detail: 'plan exceeds size bound' });
  try { atomicWrite(planFile, bytes, PLAN_FILE_MAX, 0o600); } catch (error) {
    return Promise.resolve({ ok: false, code: 'RUN_PLAN_INVALID', detail: 'run plan could not be written safely' });
  }
  return runChild([RUN_PLAN_SCRIPT, '--plan', planFile], {}).then(function (child) {
    if (child.timedOut) return { ok: false, code: 'PROJECT_ADAPTER_TIMEOUT', detail: 'token runner exceeded its time budget' };
    var lines = child.stdout.split('\n').filter(function (line) { return line.trim(); });
    var last = lines.length ? lines[lines.length - 1] : '';
    var parsed = null;
    try { parsed = JSON.parse(last); } catch (error) {}
    if (!parsed || typeof parsed.ok !== 'boolean') {
      return {
        ok: false,
        code: 'PROJECT_ADAPTER_OUTPUT_INVALID',
        detail: ('runner produced no result line; exit ' + child.exitCode + '; stderr: ' + child.stderr.slice(0, 200)).slice(0, 500)
      };
    }
    return parsed;
  });
}

function readVerifiedEntry(active, role) {
  var entry = active.manifest.artifacts.find(function (candidate) { return candidate.role === role; });
  if (!entry) return null;
  var bytes = generation.readEntry(entry);
  return bytes ? { entry: entry, bytes: bytes } : null;
}

function stageManifestBytes(job, group, artifacts, messages) {
  return Buffer.from(JSON.stringify({
    schemaVersion: 1,
    jobId: job.id,
    group: group,
    inputFingerprint: job.inputFingerprint,
    fileKeyFingerprint: job.fileKeyFingerprint,
    artifacts: artifacts,
    messages: messages.slice(0, 100).map(function (message) { return String(message).slice(0, 2048); })
  }, null, 2) + '\n');
}

function prepareTokenCaptureStage(job, groupDir) {
  var capturePlan = job.tokenCapturePlan;
  if (!capturePlan || !Array.isArray(capturePlan.records)) return Promise.reject(new Error('token-capture-plan-missing'));
  var planDir = path.join(groupDir, 'capture-plan');
  var intakeDir = path.join(groupDir, 'capture-intake');
  if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, planDir, { create: true, mode: 0o700 }) ||
      !fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, intakeDir, { create: true, mode: 0o700 })) {
    return Promise.reject(new Error('token-capture-stage-unsafe'));
  }
  var byBucket = Object.create(null);
  capturePlan.records.forEach(function (record) {
    var key = String(record.bucket).padStart(3, '0');
    (byBucket[key] = byBucket[key] || []).push(record);
  });
  var planShardFiles = [], captureShardFiles = [];
  Object.keys(byBucket).sort().forEach(function (key) {
    var relativePlan = 'capture-plan/' + key + '.json';
    var relativeCapture = 'capture-intake/' + key + '.json';
    var value = {
      schemaVersion: 1,
      bucket: Number(key),
      scope: capturePlan.scope,
      sourceIndexHash: capturePlan.sourceIndexHash,
      records: byBucket[key].map(function (record) {
        return {
          captureOperationId: record.captureOperationId,
          captureSequence: record.captureSequence,
          accountFingerprint: record.accountFingerprint,
          connectorRevision: record.connectorRevision,
          semanticPreflightHash: record.semanticPreflightHash,
          source: record.source,
          origins: record.origins
        };
      })
    };
    atomicWrite(path.join(groupDir, relativePlan), Buffer.from(JSON.stringify(value, null, 2) + '\n'), STAGE_WRITE_MAX, 0o600);
    planShardFiles.push(relativePlan);
    captureShardFiles.push(relativeCapture);
  });
  var index = {
    schemaVersion: 1,
    sourceIndexHash: capturePlan.sourceIndexHash,
    revision: capturePlan.revision,
    scope: capturePlan.scope,
    sourceCount: capturePlan.records.length,
    planShardFiles: planShardFiles,
    captureShardFiles: captureShardFiles
  };
  atomicWrite(path.join(groupDir, 'capture-plan.json'), Buffer.from(JSON.stringify(index, null, 2) + '\n'), PLAN_FILE_MAX, 0o600);

  var previousCatalogFile = null, previousIndexFile = null;
  var existingSourceShardFiles = [];
  var active = generation.current();
  if (active.ok && active.mode === 'generation') {
    var previous = readVerifiedEntry(active, 'observed-token-catalog');
    if (previous) {
      previousCatalogFile = 'inputs/previous-catalog.json';
      atomicWrite(path.join(groupDir, previousCatalogFile), previous.bytes, STAGE_WRITE_MAX, 0o600);
    }
    var previousIndex = readVerifiedEntry(active, 'observed-token-source-index');
    if (previousIndex) {
      previousIndexFile = 'inputs/previous-index.json';
      atomicWrite(path.join(groupDir, previousIndexFile), previousIndex.bytes, STAGE_WRITE_MAX, 0o600);
    }
    active.manifest.artifacts.filter(function (entry) {
      return /^observed-token-source-shard:[0-9]{3}$/.test(entry.role);
    }).sort(function (left, right) { return left.role.localeCompare(right.role); }).forEach(function (entry, shardIndex) {
      var bytes = generation.readEntry(entry);
      if (!bytes) throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
      var relative = 'inputs/previous-source-shard-' + String(shardIndex).padStart(3, '0') + '.json';
      atomicWrite(path.join(groupDir, relative), bytes, STAGE_WRITE_MAX, 0o600);
      existingSourceShardFiles.push(relative);
    });
  }
  job.tokenCaptureStage = {
    planShardFiles: planShardFiles,
    captureShardFiles: captureShardFiles,
    previousCatalogFile: previousCatalogFile,
    previousIndexFile: previousIndexFile,
    existingSourceShardFiles: existingSourceShardFiles
  };
  return Promise.resolve({ sourceCount: capturePlan.records.length });
}

// tokens scope: the provider session fills only the fixed capture-intake
// shards from the server-owned capture plan. The trusted runner alone writes
// normalized source shards/index/catalog under publication/.
function executeTokensStage(job, groupDir) {
  var prepared = job.tokenCaptureStage;
  if (!prepared) return Promise.reject(new Error('token-capture-stage-not-prepared'));
  return runPlan(groupDir, {
    op: 'normalize-token-captures',
    captureShardFiles: prepared.captureShardFiles,
    expectedCapturePlanFiles: prepared.planShardFiles,
    outDir: 'publication',
    scope: job.tokenCapturePlan.scope,
    revision: job.tokenCapturePlan.revision,
    ...(prepared.previousCatalogFile ? { previousCatalogFile: prepared.previousCatalogFile } : {}),
    ...(prepared.previousIndexFile ? { previousIndexFile: prepared.previousIndexFile } : {}),
    ...(prepared.existingSourceShardFiles.length
      ? { existingSourceShardFiles: prepared.existingSourceShardFiles }
      : {})
  }).then(function (result) {
    if (!result.ok) {
      var error = new Error((result.code || 'TOKEN_SOURCE_CAPTURE_INVALID') + ': ' + String(result.detail || '').slice(0, 200));
      error.tokenCode = result.code || 'TOKEN_SOURCE_CAPTURE_INVALID';
      throw error;
    }
    var messages = ['Observed token catalog: ' + result.counts.activeTokens + ' active key(s), ' +
      result.counts.activeSources + ' active source(s), ' + result.counts.conflicting + ' conflict(s), ' +
      result.counts.notObserved + ' not observed.'];
    if (result.noOp) {
      return {
        noOp: true,
        artifacts: [],
        messages: messages,
        sourceCount: job.tokenCapturePlan.records.length,
        sourceIndexHash: result.sourceIndexHash,
        healthEvidence: result.healthEvidence
      };
    }
    var artifacts = result.artifacts.map(function (artifact) {
      var shard = /^observed-token-source-shard:([0-9]{3})$/.exec(artifact.role);
      var logicalPath = artifact.role === 'observed-token-source-index'
        ? 'orchestrator/figma/tokens/source-index.json'
        : artifact.role === 'observed-token-catalog'
          ? 'orchestrator/figma/tokens/observed-token-catalog.json'
          : shard ? 'orchestrator/figma/tokens/sources/' + shard[1] + '.json' : null;
      if (!logicalPath) throw new Error('token-runner-artifact-unexpected');
      return {
        source: artifact.file,
        logicalPath: logicalPath,
        role: artifact.role,
        persistence: 'committed',
        required: true,
        schemaVersion: 1
      };
    });
    atomicWrite(path.join(groupDir, 'artifacts.json'), stageManifestBytes(job, 'tokens', artifacts, messages), PLAN_FILE_MAX, 0o600);
    return { noOp: false, messages: messages, healthEvidence: result.healthEvidence };
  });
}

// Rebase one committed task receipt onto the exact active token source set.
// The caller supplies already receipt-bound sidecar bytes; this function
// snapshots the active generation into a private stage and delegates all
// normalization/aggregation to the same fixed runner used by explicit sync.
function executeTaskTokenIngestion(options) {
  options = options || {};
  var active = generation.current();
  if (!active.ok || active.mode !== 'generation' && !(options.allowUninitialized === true && active.mode === 'none')) {
    return Promise.reject(new Error('TOKEN_GENERATION_RESYNC_REQUIRED'));
  }
  if (!options.intent || !Array.isArray(options.sidecars) ||
      (!options.sidecars.length && !(Array.isArray(options.retireSourceIds) && options.retireSourceIds.length) &&
        !options.detachOrigin) ||
      typeof options.stageDir !== 'string') return Promise.reject(new Error('TOKEN_TASK_RECEIPT_INVALID'));
  if (active.mode === 'generation' &&
      active.manifest.fileKeyFingerprint !== options.intent.scope.fileKeyFingerprint) {
    return Promise.reject(new Error('TOKEN_SOURCE_SCOPE_CHANGED'));
  }
  var stageDir = path.resolve(options.stageDir);
  var inputsDir = path.join(stageDir, 'inputs');
  if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, stageDir, { create: true, mode: 0o700 }) ||
      !fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, inputsDir, { create: true, mode: 0o700 })) {
    return Promise.reject(new Error('TOKEN_INGESTION_STAGE_UNSAFE'));
  }
  var captureFiles = [];
  options.sidecars.slice().sort(function (a, b) {
    return a.basename.localeCompare(b.basename);
  }).forEach(function (sidecar, index) {
    var relative = 'inputs/capture-' + String(index).padStart(3, '0') + '.json';
    atomicWrite(path.join(stageDir, relative), sidecar.bytes, STAGE_WRITE_MAX, 0o600);
    captureFiles.push(relative);
  });
  var existingSourceShardFiles = [];
  (active.mode === 'generation' ? active.manifest.artifacts : []).filter(function (entry) {
    return /^observed-token-source-shard:[0-9]{3}$/.test(entry.role);
  }).sort(function (a, b) { return a.role.localeCompare(b.role); }).forEach(function (entry, index) {
    var bytes = generation.readEntry(entry);
    if (!bytes) throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
    var relative = 'inputs/source-shard-' + String(index).padStart(3, '0') + '.json';
    atomicWrite(path.join(stageDir, relative), bytes, STAGE_WRITE_MAX, 0o600);
    existingSourceShardFiles.push(relative);
  });
  var previousCatalogFile = null, previousIndexFile = null, revision = 0;
  var priorCatalog = readVerifiedEntry(active, 'observed-token-catalog');
  var priorIndex = readVerifiedEntry(active, 'observed-token-source-index');
  if (priorCatalog) {
    previousCatalogFile = 'inputs/previous-catalog.json';
    atomicWrite(path.join(stageDir, previousCatalogFile), priorCatalog.bytes, STAGE_WRITE_MAX, 0o600);
  }
  if (priorIndex) {
    previousIndexFile = 'inputs/previous-index.json';
    atomicWrite(path.join(stageDir, previousIndexFile), priorIndex.bytes, STAGE_WRITE_MAX, 0o600);
    var indexDocument;
    try { indexDocument = JSON.parse(priorIndex.bytes.toString('utf8')); } catch (error) {
      return Promise.reject(new Error('TOKEN_GENERATION_RESYNC_REQUIRED'));
    }
    if (!Number.isSafeInteger(indexDocument.revision) || indexDocument.revision < 0) {
      return Promise.reject(new Error('TOKEN_GENERATION_RESYNC_REQUIRED'));
    }
    revision = indexDocument.revision + 1;
  }
  var plan = {
    op: 'ingest-token-receipt',
    captureFiles: captureFiles,
    existingSourceShardFiles: existingSourceShardFiles,
    intentSources: options.intent.sources,
    outDir: 'publication',
    scope: options.intent.scope,
    revision: revision,
    ...(Array.isArray(options.retireSourceIds) && options.retireSourceIds.length
      ? { retireSourceIds: options.retireSourceIds }
      : {}),
    ...(options.detachOrigin ? { detachOrigin: options.detachOrigin } : {}),
    ...(previousCatalogFile ? { previousCatalogFile: previousCatalogFile } : {}),
    ...(previousIndexFile ? { previousIndexFile: previousIndexFile } : {})
  };
  return runPlan(stageDir, plan).then(function (result) {
    if (!result.ok) {
      var error = new Error((result.code || 'TOKEN_TASK_INGESTION_FAILED') + ': ' + String(result.detail || '').slice(0, 300));
      error.tokenCode = result.code || 'TOKEN_TASK_INGESTION_FAILED';
      throw error;
    }
    if (result.noOp) return {
      noOp: true,
      active: active,
      sourceIndexHash: result.sourceIndexHash,
      healthEvidence: result.healthEvidence,
      acceptedSources: result.acceptedSources || [],
      supersededSources: result.supersededSources || []
    };
    var artifacts = result.artifacts.map(function (artifact) {
      var shard = /^observed-token-source-shard:([0-9]{3})$/.exec(artifact.role);
      var logicalPath = artifact.role === 'observed-token-source-index'
        ? 'orchestrator/figma/tokens/source-index.json'
        : artifact.role === 'observed-token-catalog'
          ? 'orchestrator/figma/tokens/observed-token-catalog.json'
          : shard ? 'orchestrator/figma/tokens/sources/' + shard[1] + '.json' : null;
      if (!logicalPath) throw new Error('TOKEN_RUNNER_ARTIFACT_UNEXPECTED');
      var source = artifact.file.split(path.sep).join('/');
      var file = path.join(stageDir, artifact.file);
      var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, path.dirname(file), file, generation.ARTIFACT_MAX);
      if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') throw new Error('TOKEN_RUNNER_ARTIFACT_UNSAFE');
      return {
        source: source,
        logicalPath: logicalPath,
        role: artifact.role,
        persistence: 'committed',
        required: true,
        schemaVersion: 1,
        bytes: hit.bytes,
        hash: generation.sha(hit.bytes),
        size: hit.bytes.length
      };
    });
    return {
      noOp: false,
      active: active,
      sourceIndexHash: result.sourceIndexHash,
      healthEvidence: result.healthEvidence,
      acceptedSources: result.acceptedSources || [],
      supersededSources: result.supersededSources || [],
      domainResult: {
        group: 'tokens',
        domain: 'tokens',
        inputFingerprint: options.intent.receiptManifestHash,
        stage: { artifacts: artifacts }
      }
    };
  });
}

function prepareComponentCaptureStage(job, groupDir) {
  var plan = job.tokenCapturePlan;
  if (!plan || plan.schemaVersion !== 1 || !Array.isArray(plan.captureShardFiles) ||
      plan.captureShardFiles.length !== 128 || !Array.isArray(plan.knownSources) ||
      !Array.isArray(plan.newSourceReservations) || plan.newSourceReservations.length !== 128 ||
      plan.newSourceReservations.some(function (reservation) {
        return !reservation || reservation.captureSequence !== 1 ||
          !/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(reservation.captureOperationId || ''));
      }) || new Set(plan.newSourceReservations.map(function (reservation) {
        return reservation.captureOperationId;
      })).size !== 128) {
    return Promise.reject(new Error('component-token-capture-plan-missing'));
  }
  if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, path.join(groupDir, 'component-token-intake'),
    { create: true, mode: 0o700 })) {
    return Promise.reject(new Error('component-token-capture-stage-unsafe'));
  }
  atomicWrite(path.join(groupDir, 'component-token-plan.json'),
    Buffer.from(JSON.stringify(plan, null, 2) + '\n'), PLAN_FILE_MAX, 0o600);
  return Promise.resolve({ sourceCount: plan.knownSources.length });
}

function activeTokenArtifactsIntoStage(groupDir) {
  var active = generation.current();
  if (!active.ok || active.mode !== 'generation') return [];
  var out = [];
  active.manifest.artifacts.filter(function (entry) {
    return entry.domain === 'tokens';
  }).sort(function (left, right) { return left.role.localeCompare(right.role); }).forEach(function (entry, index) {
    var bytes = generation.readEntry(entry);
    if (!bytes) throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
    var relative = 'token-domain/carried-' + String(index).padStart(3, '0') + '.json';
    atomicWrite(path.join(groupDir, relative), bytes, STAGE_WRITE_MAX, 0o600);
    out.push({
      source: relative, logicalPath: entry.logicalPath, role: entry.role,
      persistence: 'committed', required: true, schemaVersion: 1
    });
  });
  return out;
}

function componentCaptureRoots(inventory) {
  if (!inventory || inventory.schemaVersion !== 2 || !Array.isArray(inventory.components)) {
    throw new Error('COMPONENT_DESIGN_CAPTURE_INVALID');
  }
  var roots = Object.create(null);
  inventory.components.forEach(function (component) {
    var nodeId = component && component.providerIdentity && component.providerIdentity.nodeId;
    var pageId = component && component.page && component.page.pageId;
    if (!nodeId || !pageId) throw new Error('COMPONENT_DESIGN_CAPTURE_INVALID');
    roots[nodeId] = 1;
    roots[pageId] = 1;
  });
  return roots;
}

function componentTokenReferenceCovered(component, batch, ref, sourceContract) {
  var nodeId = component && component.providerIdentity && component.providerIdentity.nodeId;
  var pageId = component && component.page && component.page.pageId;
  return !!(batch && nodeId && pageId &&
    (batch.nodeId === nodeId || batch.nodeId === pageId) &&
    sourceContract.contextKey(batch.context) === ref.contextKey &&
    batch.observations.some(function (observation) {
      return observation.observedTokenKey === ref.observedTokenKey &&
        observation.providerName === ref.providerName;
    }));
}

function newComponentSourceReservationMap(sourceIds, known, reservations) {
  var out = Object.create(null);
  var pending = sourceIds.filter(function (sourceId) {
    return !known[sourceId];
  }).sort();
  if (pending.length > reservations.length) {
    throw new Error('TOKEN_SOURCE_CAPTURE_LIMIT_EXCEEDED');
  }
  pending.forEach(function (sourceId, index) {
    out[sourceId] = reservations[index];
  });
  return out;
}

function supersedeUnobservedComponentReservations(healthReservation, sourceIds) {
  if (!healthReservation || !sourceIds.length) return;
  var unobserved = Object.create(null);
  sourceIds.forEach(function (sourceId) { unobserved[sourceId] = 1; });
  var retained = [];
  healthReservation.reservations.forEach(function (reservation) {
    if (unobserved[reservation.sourceId]) {
      healthReservation.unusedReservations.push(reservation);
    } else {
      retained.push(reservation);
    }
  });
  healthReservation.reservations = retained;
}

// components scope: the session left capture.json (+ visual/*.png) in the
// stage directory; run the deterministic component normalizer and write the
// stage manifest listing the published inventory plus its content-addressed
// visual evidence artifacts.
function executeComponentsStage(job, groupDir) {
  return runPlan(groupDir, {
    op: 'normalize-component-capture',
    captureFile: 'capture.json',
    outFile: 'design-component-inventory.json'
  }).then(function (result) {
    if (!result.ok) {
      var error = new Error((result.code || 'COMPONENT_DESIGN_CAPTURE_INVALID') + ': ' + String(result.detail || '').slice(0, 200));
      error.componentCode = result.code || 'COMPONENT_DESIGN_CAPTURE_INVALID';
      throw error;
    }
    var artifacts = [{
      source: 'design-component-inventory.json',
      logicalPath: generation.COMPONENT_INVENTORY_LOGICAL_PATH,
      role: 'design-component-inventory',
      persistence: 'committed',
      required: true,
      schemaVersion: generation.artifactContractVersion('design-component-inventory')
    }];
    var seenVisualRoles = Object.create(null);
    (result.visualArtifacts || []).forEach(function (entry) {
      var digest = /^sha256:([a-f0-9]{64})$/.exec(String(entry.sha256 || ''));
      if (!digest) throw new Error('component-visual-artifact-invalid');
      var suffix = digest[1].slice(0, 32);
      if (seenVisualRoles[suffix]) return;
      seenVisualRoles[suffix] = 1;
      artifacts.push({
        source: entry.file,
        logicalPath: generation.COMPONENT_VISUAL_LOGICAL_DIR + suffix + '.png',
        role: 'component-visual-evidence:' + suffix,
        persistence: 'committed',
        required: false,
        schemaVersion: 1
      });
    });
    return Promise.all([
      import(pathToFileURL(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'schema-registry.mjs')).href),
      import(pathToFileURL(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'canonical-json.mjs')).href),
      import(pathToFileURL(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'tokens', 'source-contract.mjs')).href),
      import(pathToFileURL(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'tokens', 'source-normalizer.mjs')).href)
    ]).then(function (loaded) {
      var schemas = loaded[0].createSchemaRegistry(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'schemas'));
      var canonical = loaded[1], sourceContract = loaded[2], normalizer = loaded[3];
      var validateShard = schemas.validate('observed-token-capture-shard.schema.json');
      var plan = job.tokenCapturePlan;
      var known = Object.create(null), seen = Object.create(null), batches = [], sidecars = [];
      var capturedRecords = [];
      var intakeBytes = 0;
      plan.knownSources.forEach(function (row) { known[row.sourceId] = row; });
      var inventoryFile = path.join(groupDir, 'design-component-inventory.json');
      var inventoryHit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, groupDir, inventoryFile, STAGE_WRITE_MAX);
      if (!inventoryHit || !inventoryHit.stat || String(inventoryHit.stat.nlink) !== '1') {
        throw new Error('COMPONENT_DESIGN_CAPTURE_INVALID');
      }
      var inventory = JSON.parse(inventoryHit.bytes.toString('utf8'));
      var roots = componentCaptureRoots(inventory);
      plan.captureShardFiles.forEach(function (relative, bucket) {
        var file = path.join(groupDir, relative);
        var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, path.dirname(file), file, STAGE_WRITE_MAX);
        if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') throw new Error('TOKEN_SOURCE_CAPTURE_INCOMPLETE');
        intakeBytes += hit.bytes.length;
        if (intakeBytes > programLimits.phaseBytesMax) throw new Error('TOKEN_SOURCE_CAPTURE_LIMIT_EXCEEDED');
        var shard;
        try { shard = JSON.parse(hit.bytes.toString('utf8')); } catch (error) {
          throw new Error('TOKEN_SOURCE_CAPTURE_INVALID');
        }
        if (!validateShard(shard) || shard.bucket !== bucket) throw new Error('TOKEN_SOURCE_CAPTURE_INVALID');
        shard.captures.forEach(function (record) {
          var capture = record.capture, source = capture.source, sourceId = source && source.sourceId;
          if (seen[sourceId] || sourceContract.sourceBucket(sourceId) !== bucket ||
              sourceContract.sourceIdFor(source) !== sourceId || source.kind !== 'component' ||
              source.fileKeyFingerprint !== plan.scope.fileKeyFingerprint ||
              source.branchKey !== plan.scope.branchKey ||
              !source.origin || source.origin.kind !== 'component-inventory' ||
              source.origin.componentScopeId !== plan.componentSourceScopeId ||
              source.origin.captureRootNodeId !== source.nodeId || !roots[source.nodeId] ||
              capture.accountFingerprint !== plan.accountFingerprint ||
              capture.connectorRevision !== plan.connectorRevision) {
            throw new Error('TOKEN_SOURCE_CAPTURE_INVALID');
          }
          var logicalBytes = Buffer.from(canonical.canonicalJson(capture), 'utf8');
          if (record.captureOperationId !== capture.captureOperationId ||
              record.captureSequence !== capture.captureSequence ||
              record.captureBytesHash !== canonical.bytesHash(logicalBytes)) {
            throw new Error('TOKEN_SOURCE_CAPTURE_INVALID');
          }
          seen[sourceId] = true;
          capturedRecords.push({ capture: capture, logicalBytes: logicalBytes, record: record });
        });
      });
      if (capturedRecords.length > 128) throw new Error('TOKEN_SOURCE_CAPTURE_LIMIT_EXCEEDED');
      var newSourceIds = capturedRecords.map(function (row) {
        return row.capture.source.sourceId;
      });
      var expectedNew = newComponentSourceReservationMap(
        newSourceIds,
        known,
        plan.newSourceReservations
      );
      seen = Object.create(null);
      capturedRecords.sort(function (left, right) {
        return left.capture.source.sourceId.localeCompare(right.capture.source.sourceId);
      }).forEach(function (captured) {
          var capture = captured.capture, source = capture.source, sourceId = source.sourceId;
          var expected = known[sourceId];
          var expectedReservation = expected || expectedNew[sourceId];
          if (!expectedReservation ||
              capture.captureOperationId !== expectedReservation.captureOperationId ||
              capture.captureSequence !== expectedReservation.captureSequence ||
              captured.record.semanticPreflightHash !== canonical.canonicalHash({
                captureOperationId: expectedReservation.captureOperationId,
                captureSequence: expectedReservation.captureSequence,
                sourceId: sourceId
              })) throw new Error('TOKEN_SOURCE_CAPTURE_INVALID');
          var origins = (expected && Array.isArray(expected.origins) ? expected.origins : [])
            .filter(function (origin) {
              return !(origin.kind === 'component-inventory' &&
                origin.componentScopeId === plan.componentSourceScopeId);
            }).concat([source.origin]);
          var originKeys = Object.create(null);
          origins = origins.filter(function (origin) {
            var key = canonical.canonicalJson(origin);
            if (originKeys[key]) return false;
            originKeys[key] = 1;
            return true;
          });
          var batch = normalizer.normalizeSourceCapture(capture, captured.logicalBytes, {
            sourceId: sourceId,
            captureOperationId: expectedReservation.captureOperationId,
            captureSequence: expectedReservation.captureSequence,
            accountFingerprint: plan.accountFingerprint,
            connectorRevision: plan.connectorRevision,
            origins: origins
          });
          seen[sourceId] = batch;
          batches.push(batch);
          if (!expected && job.tokenHealthReservation &&
              !job.tokenHealthReservation.reservations.some(function (row) { return row.sourceId === sourceId; })) {
            job.tokenHealthReservation.reservations.push({
              sourceId: sourceId,
              captureOperationId: expectedReservation.captureOperationId,
              captureSequence: expectedReservation.captureSequence
            });
          } else if (expected && job.tokenHealthReservation) {
            var guardIndex = (job.tokenHealthReservation.unusedReservations || []).findIndex(function (row) {
              return row.sourceId === sourceId;
            });
            if (guardIndex >= 0) {
              job.tokenHealthReservation.reservations.push(
                job.tokenHealthReservation.unusedReservations.splice(guardIndex, 1)[0]
              );
            }
          }
          sidecars.push({
            basename: 'component-' + String(sidecars.length).padStart(3, '0') + '.json',
            bytes: captured.logicalBytes
          });
      });
      inventory.components.forEach(function (component) {
        component.tokenRefs.forEach(function (ref) {
          var batch = seen[ref.sourceId];
          var covered = componentTokenReferenceCovered(component, batch, ref, sourceContract);
          if (!covered) throw new Error('COMPONENT_TOKEN_REFERENCE_UNCOVERED');
        });
      });
      var retiredSourceIds = Object.keys(known).filter(function (sourceId) {
        return known[sourceId].componentOwned && !seen[sourceId];
      });
      supersedeUnobservedComponentReservations(job.tokenHealthReservation, retiredSourceIds);
      var retiredCount = retiredSourceIds.length;
      return executeTaskTokenIngestion({
        intent: {
          scope: plan.scope,
          receiptManifestHash: job.inputFingerprint,
          sources: batches.map(function (batch) {
            return {
              sourceId: batch.sourceId,
              captureOperationId: batch.captureOperationId,
              captureSequence: batch.captureSequence,
              semanticHash: batch.batchSemanticHash
            };
          })
        },
        sidecars: sidecars,
        detachOrigin: {
          kind: 'component-inventory',
          componentScopeId: plan.componentSourceScopeId
        },
        stageDir: path.join(groupDir, 'token-domain'),
        allowUninitialized: true
      }).then(function (tokenResult) {
        if (tokenResult.noOp) return {
          tokenArtifacts: activeTokenArtifactsIntoStage(groupDir),
          tokenCount: batches.length,
          retiredCount: retiredCount,
          healthEvidence: tokenResult.healthEvidence
        };
        return {
          tokenArtifacts: tokenResult.domainResult.stage.artifacts.map(function (entry) {
            return {
              source: 'token-domain/' + entry.source,
              logicalPath: entry.logicalPath,
              role: entry.role,
              persistence: entry.persistence,
              required: entry.required,
              schemaVersion: entry.schemaVersion
            };
          }),
          tokenCount: batches.length,
          retiredCount: retiredCount,
          healthEvidence: tokenResult.healthEvidence
        };
      });
    }).then(function (tokenPublication) {
      job.tokenHealthEvidence = tokenPublication.healthEvidence;
      artifacts = artifacts.concat(tokenPublication.tokenArtifacts);
      if (artifacts.length > programLimits.compositePublicationArtifactsMax) {
        throw new Error('component-token-composite-budget-exceeded');
      }
      var messages = ['Normalized ' + result.counts.components + ' component(s) (' + result.counts.componentSets +
      ' set(s), ' + result.counts.standaloneComponents + ' standalone), ' + result.counts.unsupportedComponents +
      ' unsupported, ' + result.counts.variants + ' variant(s), scope ' + result.scopeId +
      (result.absenceProofEligible ? '' : ' (absence proof not eligible)') +
      '; ' + tokenPublication.tokenCount + ' token source capture(s), ' +
      tokenPublication.retiredCount + ' retired source(s).'];
      atomicWrite(path.join(groupDir, 'artifacts.json'), stageManifestBytes(job, 'components', artifacts, messages), PLAN_FILE_MAX, 0o600);
      return { messages: messages };
    });
  });
}

function tokenCompareRoleFor(name) {
  if (name === 'analysis-index.json') return 'project-token-analysis-index';
  if (name === 'binding-snapshot.json') return 'token-binding-snapshot';
  if (name === 'mapping-snapshot.json') return 'token-mapping-snapshot';
  if (name === 'comparison.json') return 'token-comparison';
  if (name === 'baseline.json') return 'token-baseline';
  var match = /^project-inventory-([a-z0-9][a-z0-9-]{0,63})\.json$/.exec(name);
  return match ? 'project-token-inventory:' + match[1] : null;
}

function componentCompareRoleFor(name) {
  if (name === 'analysis-index.json') return 'project-component-analysis-index';
  if (name === 'mapping-snapshot.json') return 'component-mapping-snapshot';
  if (name === 'comparison.json') return 'component-comparison';
  if (name === 'suggestions.json') return 'component-mapping-suggestions';
  if (name === 'task-suggestions.json') return 'component-task-suggestions';
  if (name === 'baseline.json') return 'component-baseline';
  var match = /^project-inventory-([a-z0-9][a-z0-9-]{0,63})\.json$/.exec(name);
  return match ? 'project-component-inventory:' + match[1] : null;
}

function adaptersConfigured() {
  var configPresent = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT,
    path.join(paths.PROJECT_ROOT, 'orchestrator', 'figma'),
    path.join(paths.PROJECT_ROOT, 'orchestrator', 'figma', 'project-adapters.json'));
  return !(configPresent && configPresent.status === 'missing');
}

// One comparison domain inside the drift scope. Runs the trusted runner in
// its own stage subdirectory (token and component runners emit identically
// named files) and appends artifact rows with subdirectory-relative sources.
function runCompareDomain(options) {
  var subdir = path.join(options.groupDir, options.subdir);
  if (!fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, subdir, { create: true, mode: 0o700 })) {
    return Promise.reject(new Error('stage-directory-unsafe'));
  }
  var inputsDir = path.join(subdir, 'inputs');
  var plan = Object.assign({}, options.plan);
  Object.keys(options.inputFiles).forEach(function (planKey) {
    var input = options.inputFiles[planKey];
    if (!input) return;
    atomicWrite(path.join(inputsDir, input.name), input.bytes, STAGE_WRITE_MAX, 0o600);
    plan[planKey] = path.join('inputs', input.name);
  });
  return runPlan(subdir, plan).then(function (result) {
    if (!result.ok) return result;
    var names = result.artifacts || [];
    for (var i = 0; i < names.length; i++) {
      var role = options.roleFor(names[i]);
      if (!role) return Promise.reject(new Error(options.domain + '-runner-artifact-unexpected'));
      options.artifacts.push({
        source: options.subdir + '/' + names[i],
        logicalPath: options.reportDir + names[i],
        role: role,
        persistence: 'committed',
        required: role !== 'token-baseline' && role !== 'component-baseline',
        schemaVersion: generation.artifactContractVersion(role)
      });
    }
    return result;
  });
}

// Drift scope: run either the explicitly selected domain or both domains for
// an explicit global plan. Both shapes use the same job/lease/CAS publisher;
// a per-domain retry never re-runs its sibling as a hidden side effect.
function executeDriftScope(job, groupDir, active, capability) {
  var messages = [];
  var artifacts = [];
  var partialErrors = [];

  var componentsLineage = active.manifest.domains.find(function (lineage) { return lineage.id === 'components'; });

  return Promise.resolve().then(function () {
    // ── token comparison domain ───────────────────────────────────────────
    if (capability && capability !== 'tokens') return null;
    var catalogHit = readVerifiedEntry(active, 'observed-token-catalog');
    var sourceIndexHit = readVerifiedEntry(active, 'observed-token-source-index');
    if (!catalogHit || !sourceIndexHit) {
      messages.push('Token comparison skipped: no complete observed token domain is synced yet.');
      return null;
    }
    if (!adaptersConfigured()) {
      messages.push('Token comparison skipped: project adapters are not configured.');
      return null;
    }
    var baselineHit = readVerifiedEntry(active, 'token-baseline');
    var sourceIndex;
    try { sourceIndex = JSON.parse(sourceIndexHit.bytes.toString('utf8')); }
    catch (error) { throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED'); }
    var sourceFreshness = require('./token-source-health-store').sourceFreshness(sourceIndex);
    return runCompareDomain({
      groupDir: groupDir,
      subdir: 'token',
      domain: 'token-drift',
      reportDir: TOKEN_DIR,
      roleFor: tokenCompareRoleFor,
      artifacts: artifacts,
      plan: {
        op: 'token-compare',
        projectRoot: paths.PROJECT_ROOT,
        sourceFreshness: sourceFreshness.state,
        eligibleAt: new Date().toISOString()
      },
      inputFiles: {
        observedCatalogFile: { name: 'observed-token-catalog.json', bytes: catalogHit.bytes },
        sourceIndexFile: { name: 'source-index.json', bytes: sourceIndexHit.bytes },
        previousBaselineFile: baselineHit ? { name: 'previous-baseline.json', bytes: baselineHit.bytes } : null
      }
    }).then(function (result) {
      if (!result.ok) {
        partialErrors.push(syncErrors.classifyComparison('tokens', result.code));
        messages.push('Token comparison failed: ' + (result.code || 'unknown') + ' — ' + String(result.detail || '').slice(0, 200));
        return null;
      }
      messages.push('Token comparison: ' + result.coverage.matched + ' matched, ' + result.coverage.valueDrift +
        ' drifted, ' + result.coverage.unbound + ' unbound, ' + result.coverage.projectOnly + ' project-only' +
        (result.blockers.length ? '; ' + result.blockers.length + ' structural blocker(s)' : ''));
    });
  }).then(function () {
    // ── component comparison domain ───────────────────────────────────────
    if (capability && capability !== 'components') return null;
    var designHit = readVerifiedEntry(active, 'design-component-inventory');
    if (!designHit) {
      messages.push('Component comparison skipped: no design component inventory is synced yet.');
      return null;
    }
    if (!adaptersConfigured()) {
      messages.push('Component comparison skipped: project adapters are not configured.');
      return null;
    }
    var baselineHit = readVerifiedEntry(active, 'component-baseline');
    var tokenComparisonHit = readVerifiedEntry(active, 'token-comparison');
    if (tokenComparisonHit) {
      var tokenComparison;
      try { tokenComparison = JSON.parse(tokenComparisonHit.bytes.toString('utf8')); }
      catch (error) { tokenComparison = null; }
      if (!tokenComparison || tokenComparison.complete !== true ||
          !tokenComparison.inputs || tokenComparison.inputs.sourceFreshness !== 'current') {
        tokenComparisonHit = null;
      }
    }
    var tokenBindingHit = readVerifiedEntry(active, 'token-binding-snapshot');
    return runCompareDomain({
      groupDir: groupDir,
      subdir: 'component',
      domain: 'component-drift',
      reportDir: COMPONENT_DIR,
      roleFor: componentCompareRoleFor,
      artifacts: artifacts,
      plan: {
        op: 'component-compare',
        projectRoot: paths.PROJECT_ROOT,
        designGenerationId: componentsLineage ? componentsLineage.sourceGenerationId : active.manifest.generationId,
        eligibleAt: new Date().toISOString()
      },
      inputFiles: {
        designInventoryFile: { name: 'design-component-inventory.json', bytes: designHit.bytes },
        previousBaselineFile: baselineHit ? { name: 'previous-baseline.json', bytes: baselineHit.bytes } : null,
        tokenComparisonFile: tokenComparisonHit ? { name: 'token-comparison.json', bytes: tokenComparisonHit.bytes } : null,
        tokenBindingSnapshotFile: tokenBindingHit ? { name: 'token-binding-snapshot.json', bytes: tokenBindingHit.bytes } : null
      }
    }).then(function (result) {
      if (!result.ok) {
        partialErrors.push(syncErrors.classifyComparison('components', result.code));
        messages.push('Component comparison failed: ' + (result.code || 'unknown') + ' — ' + String(result.detail || '').slice(0, 200));
        return null;
      }
      messages.push('Component comparison: ' + result.coverage.matched + ' matched, ' + result.coverage.drifted +
        ' drifted, ' + result.coverage.unmapped + ' unmapped, ' + result.coverage.projectOnly + ' project-only' +
        (result.blockers.length ? '; ' + result.blockers.length + ' structural blocker(s)' : ''));
    });
  }).then(function () {
    if (!artifacts.length) {
      return { fatal: partialErrors[0] || 'sync-no-valid-groups', messages: messages };
    }
    atomicWrite(path.join(groupDir, 'artifacts.json'), stageManifestBytes(job, 'drift', artifacts, messages), PLAN_FILE_MAX, 0o600);
    return { messages: messages, partialErrors: partialErrors };
  });
}

module.exports = {
  runPlan: runPlan,
  prepareTokenCaptureStage: prepareTokenCaptureStage,
  prepareComponentCaptureStage: prepareComponentCaptureStage,
  executeTokensStage: executeTokensStage,
  executeTaskTokenIngestion: executeTaskTokenIngestion,
  executeComponentsStage: executeComponentsStage,
  executeDriftScope: executeDriftScope,
  _test: {
    componentCaptureRoots: componentCaptureRoots,
    componentTokenReferenceCovered: componentTokenReferenceCovered,
    newComponentSourceReservationMap: newComponentSourceReservationMap,
    supersedeUnobservedComponentReservations: supersedeUnobservedComponentReservations
  }
};
