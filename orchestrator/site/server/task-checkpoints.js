'use strict';

// Bounded checkpoint storage + freshness admission. Checkpoints are immutable
// receipts written by the orchestrator producer; the site can inspect and
// execute them but never derives one from journal text.

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var taskIntegrity = require('./task-integrity');
var projectSourceRevision = require('../../tasks/project-source-revision.cjs');
var worktreeManager = require('./worktree-manager');
var contract = require('../../tasks/task-checkpoint-contract.cjs');
var testPolicy = require('../../tasks/task-test-policy-contract.cjs');
var receiptRegistry = require('../../tasks/task-receipt-registry.cjs');
var taskSource = require('../../tasks/task-source-contract.cjs');
var taskCore = require('../../tasks/task-state-core.cjs');

var INDEX_NAME = 'index.json';
var INDEX_MAX_BYTES = 64 * 1024;
var PREVIEW_CONFIRMATION_TTL_MS = 5 * 60 * 1000;
var PREVIEW_CONFIRMATION_MAX = 200;
var PREVIEW_CONFIRMATION_SECRET = crypto.randomBytes(32);
var previewTokens = new Map();

function checkpointDirectory(stem, create) {
  if (!taskSource.safeTaskStem(stem)) return null;
  var root = paths.CHECKPOINTS_DIR;
  var directory = path.join(root, stem);
  try {
    if (create) {
      var rootProof = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, root, {
        create: true, mode: 0o700
      });
      if (!rootProof || !rootProof.exists) return null;
      var directoryProof = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, directory, {
        create: true, mode: 0o700
      });
      if (!directoryProof || !directoryProof.exists) return null;
    }
    var proof = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, directory, { allowMissing: !create });
    if (!proof || !proof.exists) return null;
    var relative = path.relative(paths.PROJECT_ROOT, directory);
    if (!relative || relative === '..' || relative.indexOf('..' + path.sep) === 0 ||
        path.isAbsolute(relative)) return null;
    return directory;
  } catch (error) {
    if (!create && error && error.code === 'ENOENT') return null;
    return null;
  }
}

function readJson(directory, name, maxBytes) {
  var file = path.join(directory, name);
  var read = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, file, maxBytes);
  if (!read) return null;
  try {
    var text = new TextDecoder('utf-8', { fatal: true }).decode(read.bytes);
    return JSON.parse(text);
  } catch (_) { return null; }
}

function readIndex(stem) {
  var directory = checkpointDirectory(stem, false);
  if (!directory) return { schemaVersion: 1, stem: stem, entries: [], invalid: false };
  var value = readJson(directory, INDEX_NAME, INDEX_MAX_BYTES);
  if (!value) {
    var names;
    try { names = fs.readdirSync(directory); } catch (_) { return { schemaVersion: 1, stem: stem, entries: [], invalid: true }; }
    return { schemaVersion: 1, stem: stem, entries: [], invalid: names.some(function (name) { return name !== INDEX_NAME; }) };
  }
  var valid = value && value.schemaVersion === 1 && value.stem === stem &&
    Array.isArray(value.entries) && value.entries.length <= contract.MAX_PER_TASK &&
    Object.keys(value).sort().join('\0') === ['entries', 'schemaVersion', 'stem'].sort().join('\0') &&
    value.entries.every(function (entry) {
      return entry && typeof entry === 'object' && !Array.isArray(entry) &&
        Object.keys(entry).sort().join('\0') === ['checkpointHash', 'checkpointId', 'createdAt', 'status'].sort().join('\0') &&
        contract.CHECKPOINT_ID_RE.test(entry.checkpointId || '') &&
        contract.HASH_RE.test(entry.checkpointHash || '') &&
        contract.STATUSES.indexOf(entry.status) >= 0 &&
        typeof entry.createdAt === 'string' && Number.isFinite(Date.parse(entry.createdAt));
    }) && new Set(value.entries.map(function (entry) { return entry.checkpointId; })).size === value.entries.length;
  if (valid) {
    var listed;
    try { listed = fs.readdirSync(directory).sort(); } catch (_) { listed = null; }
    var expected = [INDEX_NAME].concat(value.entries.map(function (entry) {
      return entry.checkpointId + '.json';
    })).sort();
    valid = !!listed && listed.length <= contract.MAX_PER_TASK + 1 &&
      JSON.stringify(listed) === JSON.stringify(expected);
  }
  return valid ? Object.assign({ invalid: false }, value) :
    { schemaVersion: 1, stem: stem, entries: [], invalid: true };
}

function read(stem, checkpointId) {
  if (!taskSource.safeTaskStem(stem) || !contract.CHECKPOINT_ID_RE.test(String(checkpointId || ''))) return null;
  var index = readIndex(stem);
  if (index.invalid || !index.entries.some(function (entry) { return entry.checkpointId === checkpointId; })) return null;
  var directory = checkpointDirectory(stem, false);
  if (!directory) return null;
  var value = readJson(directory, checkpointId + '.json', contract.MAX_BYTES);
  if (!value || value.checkpointId !== checkpointId || contract.validate(value, stem)) return null;
  var indexed = index.entries.find(function (entry) { return entry.checkpointId === checkpointId; });
  if (!indexed || indexed.checkpointHash !== value.checkpointHash ||
      indexed.status !== value.status || indexed.createdAt !== value.createdAt) return null;
  return value;
}

function writeJson(directory, name, value, maxBytes, preserveExisting) {
  var bytes = Buffer.from(contract.canonical(value) + '\n', 'utf8');
  if (bytes.length > maxBytes) return false;
  var result = preserveExisting === true
    ? fileGuards.publishNoClobberRegularFileUnder(
      paths.PROJECT_ROOT, directory, path.join(directory, name), bytes,
      { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: maxBytes }
    )
    : fileGuards.atomicReplaceRegularFileResult(
      paths.PROJECT_ROOT, directory, path.join(directory, name), bytes,
      { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: maxBytes }
    );
  return !!(result && result.ok);
}

function publish(value) {
  var issue = contract.validate(value, value && value.stem);
  if (issue) return { ok: false, error: 'checkpoint-invalid', detail: issue };
  var directory = checkpointDirectory(value.stem, true);
  if (!directory) return { ok: false, error: 'checkpoint-storage-unavailable' };
  var index = readIndex(value.stem);
  if (index.invalid) return { ok: false, error: 'checkpoint-index-invalid' };
  var existing = index.entries.find(function (entry) { return entry.checkpointId === value.checkpointId; });
  if (existing) {
    var current = read(value.stem, value.checkpointId);
    return current && current.checkpointHash === value.checkpointHash
      ? { ok: true, checkpoint: current, idempotentReplay: true }
      : { ok: false, error: 'checkpoint-conflict' };
  }
  if (index.entries.length >= contract.MAX_PER_TASK) {
    return { ok: false, error: 'checkpoint-retention-exhausted' };
  }
  if (!writeJson(directory, value.checkpointId + '.json', value, contract.MAX_BYTES, true)) {
    return { ok: false, error: 'checkpoint-publication-failed' };
  }
  var next = {
    schemaVersion: 1,
    stem: value.stem,
    entries: index.entries.concat([{
      checkpointId: value.checkpointId,
      checkpointHash: value.checkpointHash,
      status: value.status,
      createdAt: value.createdAt
    }]).sort(function (a, b) {
      return Date.parse(b.createdAt) - Date.parse(a.createdAt) ||
        a.checkpointId.localeCompare(b.checkpointId);
    })
  };
  if (!writeJson(directory, INDEX_NAME, next, INDEX_MAX_BYTES, false)) {
    // The receipt was published before the index because readers never trust
    // an index entry without immutable content. If index publication fails,
    // remove only the exact just-published receipt so the directory does not
    // become an orphaned, permanently invalid store.
    fileGuards.unlinkRegularFileIfUnder(
      paths.PROJECT_ROOT, directory, path.join(directory, value.checkpointId + '.json'),
      contract.MAX_BYTES,
      function (bounded) {
        var observed;
        try { observed = JSON.parse(bounded.bytes.toString('utf8')); } catch (_) { return false; }
        return !contract.validate(observed, value.stem) &&
          observed.checkpointId === value.checkpointId &&
          observed.checkpointHash === value.checkpointHash;
      }
    );
    return { ok: false, error: 'checkpoint-index-publication-failed' };
  }
  var proven = read(value.stem, value.checkpointId);
  return proven && proven.checkpointHash === value.checkpointHash
    ? { ok: true, checkpoint: proven, idempotentReplay: false }
    : { ok: false, error: 'checkpoint-publication-unverified' };
}

function canonicalTaskSnapshot(stem) {
  var validation = taskIntegrity.validateAll('checkpoint-producer');
  if (!validation || !validation.ok || !validation._model) return null;
  var metadata = validation._model.metadata.get(stem);
  if (!metadata) return null;
  return {
    state: metadata.state,
    sourceRevision: metadata.revision,
    dependencies: (metadata.deps || []).slice().sort()
  };
}

function activeRunId(stem) {
  var directory = paths.LOCKS_DIR;
  var file = path.join(directory, stem + '.json');
  var read = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, file, 32 * 1024);
  if (!read) return null;
  var value;
  try { value = JSON.parse(read.bytes.toString('utf8')); } catch (_) { return null; }
  return taskCore.canonicalLockV1(value, stem) ? value.runId : null;
}

function hashConfig() {
  var directory = path.dirname(paths.PROJECT_CONFIG_FILE);
  var read = fileGuards.boundedRegularFileUnder(
    paths.PROJECT_ROOT, directory, paths.PROJECT_CONFIG_FILE, 1024 * 1024
  );
  return read
    ? 'sha256:' + crypto.createHash('sha256').update(read.bytes).digest('hex')
    : null;
}

function dependencyHash(dependencies) {
  return contract.hash(Array.isArray(dependencies) ? dependencies.slice().sort() : []);
}

// The live execution pin for a stem: the exact worktree generation, its base
// and the CURRENT tree of the checkout.
//   { ok: true, pin }        proven
//   { ok: true, pin: null }  proven: no execution root (after the Phase-2
//                            cutover only non-run flows can be in that state)
//   { ok: false, code }      unprovable — NEVER collapse this into pin:null.
//                            A receipt stored without a pin is exempt from the
//                            §6.13 gate for the rest of its life, so a momentary
//                            unreadable record store or an uncomputable tree
//                            would mint a permanently uninvalidatable green gate.
function currentExecutionPin(stem, runId) {
  try { return worktreeManager.executionPinFor(stem, runId); }
  catch (error) {
    return { ok: false, code: 'EXECUTION_PIN_UNAVAILABLE',
      message: String(error && error.message || error).slice(0, 200) };
  }
}

function inputFingerprint(value) {
  // The execution pin is part of the input identity: a receipt earned against
  // a different candidate tree or target revision is a different receipt.
  return contract.hash({
    schemaVersion: contract.SCHEMA_VERSION,
    stem: value.stem,
    phase: value.phase,
    taskSourceRevision: value.taskSourceRevision,
    projectSourceRevision: value.projectSourceRevision,
    configHash: value.configHash,
    dependencySnapshotHash: value.dependencySnapshotHash,
    testPolicyHash: value.testPolicyHash,
    priorPhaseReceiptIds: value.priorPhaseReceiptIds,
    executionPin: value.executionPin || null
  });
}

// The live machine test-policy hash. Unreadable/invalid policy is a typed
// failure — a checkpoint can never be created or stay fresh against an
// unknown policy (unknown versions fail closed by contract).
function currentTestPolicyHash() {
  try { return testPolicy.loadPolicy().policyHash; }
  catch (_) { return null; }
}

// Default receipt verifier for the registry-owned test evidence families. A
// receipt id outside those families keeps the historical fail-closed answer
// (`receipt-verification-unavailable`) until its own owner registers here —
// this module never impersonates a verifier for Figma/action receipts.
function testReceiptLoaderFor(value) {
  return function load(kind, hash) {
    var hex = hash.slice('sha256:'.length);
    var base = path.join(paths.TEST_CERTIFICATION_DIR, value.stem, value.runId);
    var candidates = [];
    if (kind === 'test-summary') candidates.push(path.join(base, 'summary.json'));
    else if (kind === 'test-policy') candidates.push(path.join(base, 'policy.json'));
    else if (kind === 'source-snapshot') candidates.push(path.join(base, 'source-snapshot.json'));
    else if (kind === 'test-impact-planned') candidates.push(path.join(base, 'planned-impact.json'));
    else if (kind === 'test-impact-observed') candidates.push(path.join(base, 'observed-impact.json'));
    else {
      var family = kind === 'test-command' ? 'commands' : 'structural';
      var directory = path.join(base, family);
      var names = [];
      try { names = fs.readdirSync(directory); } catch (_) { return null; }
      names.filter(function (name) { return name.endsWith('-' + hex + '.json'); })
        .forEach(function (name) { candidates.push(path.join(directory, name)); });
    }
    for (var i = 0; i < candidates.length; i++) {
      try {
        var candidate = candidates[i];
        var bounded = fileGuards.boundedRegularFileUnder(
          paths.PROJECT_ROOT, path.dirname(candidate), candidate, 2 * 1024 * 1024
        );
        if (!bounded) continue;
        return JSON.parse(bounded.bytes.toString('utf8'));
      } catch (_) { }
    }
    return null;
  };
}

function defaultReceiptVerifier(value) {
  return function verify(receiptId) {
    if (!receiptRegistry.RECEIPT_ID_RE.test(String(receiptId || ''))) return false;
    var result;
    try { result = receiptRegistry.verifyReceiptId(receiptId, testReceiptLoaderFor(value)); }
    catch (_) { return false; }
    if (!result.verified || !result.receipt ||
        result.receipt.taskStem !== value.stem || result.receipt.runId !== value.runId ||
        result.receipt.policyHash !== value.testPolicyHash) return false;
    if (result.kind === 'test-summary') {
      return (result.receipt.verdict === 'PASS' ||
          value.phase === 'ship' && value.status === 'completed' && result.receipt.verdict === 'SKIPPED') &&
        result.receipt.snapshotVerification === 'current';
    }
    return true;
  };
}

function create(stem, input, dependencies) {
  dependencies = dependencies || {};
  if (!taskSource.safeTaskStem(stem) || !input || typeof input !== 'object' || Array.isArray(input)) {
    return { ok: false, error: 'checkpoint-producer-input-invalid' };
  }
  var allowed = ['runId', 'phase', 'attempt', 'status', 'outputReceiptIds',
    'priorPhaseReceiptIds', 'failureCode', 'retryPolicy'];
  if (Object.keys(input).some(function (key) { return allowed.indexOf(key) < 0; }) ||
      allowed.some(function (key) { return !Object.prototype.hasOwnProperty.call(input, key); })) {
    return { ok: false, error: 'checkpoint-producer-input-invalid' };
  }
  var ownedRunId = dependencies.activeRunId || activeRunId(stem);
  if (!ownedRunId || ownedRunId !== input.runId) {
    return { ok: false, error: 'checkpoint-owner-unverified' };
  }
  var snapshot = dependencies.taskSnapshot || canonicalTaskSnapshot(stem);
  var source = dependencies.projectRevision ||
    projectSourceRevision.compute(paths.PROJECT_ROOT, { profile: 'task-run', taskStem: stem });
  var configHash = dependencies.configHash || hashConfig();
  if (!snapshot || !source || !source.available || !configHash) {
    return { ok: false, error: 'checkpoint-input-revision-unavailable' };
  }
  var policyHash = dependencies.testPolicyHash || currentTestPolicyHash();
  if (!policyHash) {
    return { ok: false, error: 'test-policy-unavailable' };
  }
  var value = {
    schemaVersion: contract.SCHEMA_VERSION,
    checkpointId: 'cp-' + crypto.randomBytes(16).toString('hex'),
    stem: stem,
    runId: input.runId,
    phase: input.phase,
    attempt: input.attempt,
    status: input.status,
    createdAt: new Date().toISOString(),
    taskState: snapshot.state,
    taskSourceRevision: snapshot.sourceRevision,
    projectSourceRevision: source.revision,
    configHash: configHash,
    dependencySnapshotHash: dependencyHash(snapshot.dependencies),
    inputFingerprint: 'sha256:' + '0'.repeat(64),
    testPolicyHash: policyHash,
    outputReceiptIds: input.outputReceiptIds.slice(),
    priorPhaseReceiptIds: input.priorPhaseReceiptIds.slice(),
    failureCode: input.failureCode,
    retryPolicy: Object.assign({}, input.retryPolicy),
    // Bind the receipt to the exact candidate tree and target revision it was
    // earned against (pipeline improvement 01 §6.13): a green gate never
    // survives a changed tree, a moved target, or a new generation. Filled in
    // below, because an unprovable pin REFUSES rather than storing null.
    executionPin: null,
    checkpointHash: 'sha256:' + '0'.repeat(64)
  };
  if (dependencies.executionPin === undefined) {
    var pinned = currentExecutionPin(stem, input.runId);
    // Every other unprovable input in this function refuses
    // ('checkpoint-input-revision-unavailable', 'test-policy-unavailable').
    // The execution pin is the one that must NOT be the exception: null here is
    // not "no gate applies", it is "this receipt can never be invalidated".
    if (!pinned.ok || pinned.pin === null) {
      return { ok: false, error: 'execution-context-unavailable',
        detail: String(pinned.message || pinned.code || '').slice(0, 300) };
    }
    value.executionPin = pinned.pin;
  } else {
    value.executionPin = dependencies.executionPin;
  }
  value.inputFingerprint = inputFingerprint(value);
  try { value = contract.seal(value); }
  catch (error) { return { ok: false, error: 'checkpoint-invalid', detail: String(error.message).slice(0, 300) }; }
  return publish(value);
}

function freshness(value, dependencies) {
  dependencies = dependencies || {};
  if (!value || contract.validate(value, value && value.stem)) {
    return { current: false, reasonCode: 'checkpoint-invalid', limitations: [] };
  }
  var currentRunId = Object.prototype.hasOwnProperty.call(dependencies, 'activeRunId')
    ? dependencies.activeRunId : activeRunId(value.stem);
  if (currentRunId !== value.runId) {
    return { current: false, reasonCode: 'run-owner-changed', limitations: [] };
  }
  // Pipeline improvement 01 §6.13/§6.15: a receipt earned inside an execution
  // root is current ONLY while that generation, its candidate tree and its
  // target revision are unchanged. A pin that cannot be recomputed is not a
  // pass — an unprovable execution context invalidates the receipt.
  var recordedPin = value.executionPin || null;
  if (recordedPin !== null) {
    var livePin;
    if (dependencies.executionPin === undefined) {
      var live = currentExecutionPin(value.stem, value.runId);
      if (!live.ok) return { current: false, reasonCode: 'execution-context-unavailable', limitations: [] };
      livePin = live.pin;
    } else {
      livePin = dependencies.executionPin;
    }
    if (!livePin) return { current: false, reasonCode: 'execution-context-unavailable', limitations: [] };
    var pinFields = ['worktreeId', 'baseCommit', 'baseTree', 'executionTree', 'targetRef', 'targetCommit'];
    var pinChanged = pinFields.some(function (field) { return livePin[field] !== recordedPin[field]; });
    if (pinChanged) return { current: false, reasonCode: 'execution-changed', limitations: [] };
  }
  var snapshot = dependencies.taskSnapshot || canonicalTaskSnapshot(value.stem);
  if (!snapshot) return { current: false, reasonCode: 'task-integrity', limitations: [] };
  if (snapshot.state !== value.taskState ||
      snapshot.sourceRevision !== value.taskSourceRevision) {
    return { current: false, reasonCode: 'task-changed', limitations: [] };
  }
  if (dependencyHash(snapshot.dependencies) !== value.dependencySnapshotHash) {
    return { current: false, reasonCode: 'dependency-changed', limitations: [] };
  }
  var configHash = dependencies.configHash || hashConfig();
  if (!configHash || configHash !== value.configHash) {
    return { current: false, reasonCode: 'config-changed', limitations: [] };
  }
  var source = dependencies.projectRevision ||
    projectSourceRevision.compute(paths.PROJECT_ROOT, { profile: 'task-run', taskStem: value.stem });
  if (!source || !source.available) {
    return { current: false, reasonCode: 'project-revision-unavailable',
      limitations: source && source.limitations || [] };
  }
  if (source.revision !== value.projectSourceRevision) {
    return { current: false, reasonCode: 'project-changed', limitations: [] };
  }
  var livePolicyHash = dependencies.testPolicyHash || currentTestPolicyHash();
  if (!livePolicyHash) {
    return { current: false, reasonCode: 'test-policy-unavailable', limitations: [] };
  }
  if (livePolicyHash !== value.testPolicyHash) {
    return { current: false, reasonCode: 'test-policy-changed', limitations: [] };
  }
  if (inputFingerprint(value) !== value.inputFingerprint) {
    return { current: false, reasonCode: 'input-fingerprint-changed', limitations: [] };
  }
  var receipts = value.priorPhaseReceiptIds.concat(value.outputReceiptIds);
  if (receipts.length) {
    for (var i = 0; i < receipts.length; i++) {
      var receiptId = receipts[i];
      var verified;
      if (typeof dependencies.receiptVerifier === 'function') {
        verified = dependencies.receiptVerifier(receiptId, value) === true;
      } else if (receiptRegistry.RECEIPT_ID_RE.test(String(receiptId || ''))) {
        // The registry closes the historical fail-closed stub for the test
        // evidence families; every other family still has no verifier here.
        verified = defaultReceiptVerifier(value)(receiptId);
      } else {
        return {
          current: false,
          reasonCode: 'receipt-verification-unavailable',
          limitations: ['receipt-registry-unavailable']
        };
      }
      if (!verified) {
        return { current: false, reasonCode: 'receipt-stale', limitations: [] };
      }
    }
  }
  return { current: true, reasonCode: null, limitations: [] };
}

function list(stem, options) {
  options = options || {};
  var index = readIndex(stem);
  var limitations = [];
  if (index.invalid) limitations.push('checkpoint-index-invalid');
  var freshnessDependencies = options.dependencies;
  if (options.checkFreshness !== false && !freshnessDependencies) {
    freshnessDependencies = {
      taskSnapshot: canonicalTaskSnapshot(stem),
      projectRevision: projectSourceRevision.compute(
        paths.PROJECT_ROOT, { profile: 'task-run', taskStem: stem }),
      configHash: hashConfig()
    };
  }
  var rows = [];
  index.entries.slice(0, contract.MAX_PER_TASK).forEach(function (entry) {
    var value = read(stem, entry.checkpointId);
    if (!value) { limitations.push('checkpoint-invalid'); return; }
    var state = options.checkFreshness === false
      ? { current: null, reasonCode: 'freshness-not-checked', limitations: [] }
      : freshness(value, freshnessDependencies);
    rows.push(contract.publicProjection(value, state));
  });
  return {
    schemaVersion: 1,
    stem: taskSource.safeTaskStem(stem) ? stem : null,
    checkpoints: rows,
    partial: limitations.length > 0,
    limitations: Array.from(new Set(limitations)).sort()
  };
}

function summary(stem) {
  var index = readIndex(stem);
  return {
    schemaVersion: 1,
    stem: taskSource.safeTaskStem(stem) ? stem : null,
    revision: contract.hash({
      stem: stem,
      entries: index.invalid ? [] : index.entries
    }),
    count: index.invalid ? 0 : index.entries.length,
    available: !index.invalid && index.entries.length > 0,
    partial: index.invalid,
    limitations: index.invalid ? ['checkpoint-index-invalid'] : []
  };
}

// The candidate sealer's sole gate authority (§28.4). A receipt is publishable
// only when this exact run has a completed `ship` checkpoint whose execution
// pin is still the live candidate tree/target and whose task, project, config,
// test-policy and referenced evidence are all fresh. Merely writing an
// executionPin into a checkpoint is decorative unless the sealing boundary
// actually consumes it.
function sealingGate(stem, runId, executionPin, dependencies) {
  var index = readIndex(stem);
  if (index.invalid) return { ok: false, code: 'SEAL_GATE_STORE_INVALID', message: 'the checkpoint store is invalid' };
  var candidates = index.entries.map(function (entry) { return read(stem, entry.checkpointId); })
    .filter(function (value) {
      return value && value.runId === runId && value.phase === 'ship' && value.status === 'completed';
    });
  if (!candidates.length) {
    return { ok: false, code: 'SEAL_GATE_ABSENT', message: 'the run has no completed ship checkpoint' };
  }
  var checkedDependencies = Object.assign({}, dependencies || {}, { executionPin: executionPin });
  for (var i = 0; i < candidates.length; i++) {
    var state = freshness(candidates[i], checkedDependencies);
    if (state.current) return { ok: true, checkpoint: candidates[i] };
  }
  var latestState = freshness(candidates[0], checkedDependencies);
  return { ok: false, code: 'SEAL_GATE_STALE',
    message: 'the completed ship checkpoint is stale: ' + latestState.reasonCode,
    reasonCode: latestState.reasonCode };
}

function retryCandidate(stem, checkpointId, options) {
  var status = retryStatus(stem, checkpointId, options);
  return status && status.candidate || null;
}

function retryStatus(stem, checkpointId, options) {
  var value = read(stem, checkpointId);
  if (!value) return {
    candidate: null,
    projection: {
      checkpointId: contract.CHECKPOINT_ID_RE.test(String(checkpointId || ''))
        ? checkpointId : null,
      phase: null,
      status: null,
      retryPolicy: null,
      freshness: { current: false, reasonCode: 'checkpoint-invalid', limitations: [] }
    }
  };
  var state = freshness(value, options && options.dependencies);
  var projection = contract.publicProjection(value, state);
  var eligible = state.current &&
    ['retry-phase', 'resume-run', 'restart-from-phase'].includes(value.retryPolicy.kind);
  return {
    projection: projection,
    candidate: eligible ? {
    id: value.checkpointId,
    hash: value.checkpointHash,
    phase: value.phase,
    safePhase: value.retryPolicy.safePhase,
    retryPolicy: value.retryPolicy.kind,
    freshness: state
    } : null
  };
}

function pruneTokens() {
  var now = Date.now();
  previewTokens.forEach(function (value, key) {
    if (value.expiresAt <= now || value.used) previewTokens.delete(key);
  });
  while (previewTokens.size >= PREVIEW_CONFIRMATION_MAX) {
    previewTokens.delete(previewTokens.keys().next().value);
  }
}

function tokenFor(body) {
  pruneTokens();
  var nonce = crypto.randomBytes(18).toString('base64url');
  var signature = crypto.createHmac('sha256', PREVIEW_CONFIRMATION_SECRET)
    .update(nonce + '\0' + contract.canonical(body), 'utf8').digest('base64url');
  var token = nonce + '.' + signature;
  previewTokens.set(token, {
    bodyHash: contract.hash(body),
    expiresAt: Date.now() + PREVIEW_CONFIRMATION_TTL_MS,
    used: false
  });
  return token;
}

function retryNeedsConfirmation(value) {
  return value.retryPolicy.safePhase !== value.phase || [
    'assemble-gate', 'runtime-verify', 'screenshot-gate', 'design-pull'
  ].indexOf(value.phase) >= 0;
}

function preview(stem, body, summary, dependencies) {
  if (!body || typeof body !== 'object' || Array.isArray(body) ||
      Object.keys(body).sort().join('\0') !== ['actionRevision', 'checkpointHash', 'checkpointId'].sort().join('\0')) {
    return { ok: false, status: 400, error: 'bad-retry-preview' };
  }
  var action = summary && summary.task && summary.task.primaryAction;
  var value = read(stem, body.checkpointId);
  if (!action || action.kind !== 'retry-phase' || action.actionRevision !== body.actionRevision ||
      action.checkpointId !== body.checkpointId || !value ||
      value.checkpointHash !== body.checkpointHash) {
    return { ok: false, status: 409, error: 'checkpoint-stale' };
  }
  var state = freshness(value, dependencies);
  if (!state.current) return { ok: false, status: 409, error: 'checkpoint-stale', freshness: state };
  var confirmationRequired = retryNeedsConfirmation(value);
  var binding = {
    stem: stem,
    checkpointId: value.checkpointId,
    checkpointHash: value.checkpointHash,
    actionRevision: action.actionRevision,
    taskSourceRevision: value.taskSourceRevision,
    safePhase: value.retryPolicy.safePhase
  };
  return {
    ok: true,
    status: 200,
    schemaVersion: 1,
    checkpoint: contract.publicProjection(value, state),
    reuse: value.priorPhaseReceiptIds.slice(),
    rerun: [value.retryPolicy.safePhase || value.phase],
    confirmationRequired: confirmationRequired,
    confirmationToken: confirmationRequired ? tokenFor(binding) : null,
    expiresAt: confirmationRequired
      ? new Date(Date.now() + PREVIEW_CONFIRMATION_TTL_MS).toISOString()
      : null
  };
}

function consumeConfirmation(value, actionRevision, token) {
  if (!retryNeedsConfirmation(value)) return true;
  pruneTokens();
  var saved = previewTokens.get(token);
  var binding = {
    stem: value.stem,
    checkpointId: value.checkpointId,
    checkpointHash: value.checkpointHash,
    actionRevision: actionRevision,
    taskSourceRevision: value.taskSourceRevision,
    safePhase: value.retryPolicy.safePhase
  };
  if (!saved || saved.used || saved.expiresAt <= Date.now() ||
      saved.bodyHash !== contract.hash(binding)) return false;
  saved.used = true;
  previewTokens.delete(token);
  return true;
}

module.exports = Object.freeze({
  readIndex: readIndex,
  read: read,
  publish: publish,
  create: create,
  freshness: freshness,
  list: list,
  summary: summary,
  sealingGate: sealingGate,
  retryCandidate: retryCandidate,
  retryStatus: retryStatus,
  preview: preview,
  consumeConfirmation: consumeConfirmation,
  hashConfig: hashConfig,
  dependencyHash: dependencyHash,
  inputFingerprint: inputFingerprint,
  activeRunId: activeRunId
});
