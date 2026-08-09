'use strict';

// Server-issued operation plan for task screen token sidecars. The producer
// receives exact node/context/source identities only after the durable health
// store has reserved every sequence.

var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var configUpdate = require('./project-config-update');
var figma = require('./figma');
var health = require('./token-source-health-store');
var generation = require('./figma-generation');
var designParser = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'scripts', 'design-parser.cjs'));
var projectIdentity = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'project-identity.cjs'));
var identity = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'token-identity.cjs'));

var ROOT = path.join(paths.FIGMA_CACHE_DIR, 'screen-token-plans');
var STEM_RE = /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/;
var PLAN_RE = /^tokplan_[A-Za-z0-9_-]{16,96}$/;
var PLAN_MAX = 1024 * 1024;
var TASK_MAX = 1024 * 1024;
var PREPARE_ERROR_CODES = Object.freeze({
  TOKEN_TASK_STEM_INVALID: true,
  TOKEN_TASK_SOURCE_UNSAFE: true,
  TOKEN_TASK_SOURCE_MISSING: true,
  TOKEN_TASK_BINDING_UNAVAILABLE: true,
  TOKEN_TASK_FILE_SCOPE_MISMATCH: true,
  TOKEN_TASK_CONTEXT_INVALID: true,
  TOKEN_GENERATION_RESYNC_REQUIRED: true,
  TOKEN_SOURCE_SCOPE_MISMATCH: true,
  TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE: true,
  TOKEN_TASK_DESIGN_INVALID: true,
  TOKEN_TASK_SOURCE_LIMIT_EXCEEDED: true,
  TOKEN_TASK_SOURCE_DUPLICATE: true,
  TOKEN_TASK_PLAN_INVALID: true,
  TOKEN_TASK_PLAN_WRITE_FAILED: true
});

function prepareErrorCode(error) {
  var code = error && error.message;
  return Object.prototype.hasOwnProperty.call(PREPARE_ERROR_CODES, code)
    ? code : 'TOKEN_TASK_PLAN_UNAVAILABLE';
}

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function planFile(planId) {
  if (!PLAN_RE.test(String(planId || ''))) throw new Error('TOKEN_TASK_PLAN_ID_INVALID');
  return path.join(ROOT, planId + '.json');
}
function relative(file) {
  var value = path.relative(paths.PROJECT_ROOT, file).split(path.sep).join('/');
  if (!value || value === '..' || value.indexOf('../') === 0) throw new Error('TOKEN_TASK_PLAN_PATH_INVALID');
  return value;
}
function taskBodies(stem) {
  if (!STEM_RE.test(String(stem || ''))) throw new Error('TOKEN_TASK_STEM_INVALID');
  var files = [
    path.join(paths.TASKS_DIR, 'todo', stem + '.md'),
    path.join(paths.TASKS_DIR, 'backlog', stem + '.md'),
    path.join(paths.TASKS_DIR, 'pending', stem + '.questions.md')
  ];
  var bodies = [];
  files.forEach(function (file) {
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(file), file);
    if (inspected && inspected.status === 'missing') return;
    var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, path.dirname(file), file, TASK_MAX);
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') throw new Error('TOKEN_TASK_SOURCE_UNSAFE');
    bodies.push(hit.bytes.toString('utf8'));
  });
  if (!bodies.length) throw new Error('TOKEN_TASK_SOURCE_MISSING');
  return bodies;
}
function binding(design) {
  var testJobs = require('./figma-test-job');
  var config = configUpdate.read();
  var account = figma.account();
  var accountFingerprint = testJobs.accountFingerprint(account);
  if (!config.ok || !config.figmaFileKey || !accountFingerprint) throw new Error('TOKEN_TASK_BINDING_UNAVAILABLE');
  var mismatched = design.entries.some(function (entry) {
    return !entry.none && Object.keys(entry.themes || {}).some(function (theme) {
      return entry.themes[theme].fileKey !== config.figmaFileKey;
    });
  });
  if (mismatched) throw new Error('TOKEN_TASK_FILE_SCOPE_MISMATCH');
  return {
    accountFingerprint: accountFingerprint,
    fileKeyFingerprint: testJobs.fileKeyFingerprint(config.figmaFileKey),
    branchKey: projectIdentity.projectBranchKey(paths.PROJECT_ROOT)
  };
}
function themeRecord(theme) {
  if (theme === 'primary') return { contextTheme: 'unknown', variantId: 'unknown-default-shared', suffix: '' };
  if (theme === 'light') return { contextTheme: 'light', variantId: 'light-default-shared', suffix: '' };
  if (theme === 'dark') return { contextTheme: 'dark', variantId: 'dark-default-shared', suffix: '.dark' };
  throw new Error('TOKEN_TASK_CONTEXT_INVALID');
}
function currentSourceState(scope, sourceIds) {
  var active = generation.current();
  if (!active.ok) throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
  var uninitializedHash = identity.hash({ state: 'uninitialized', scope: scope });
  if (active.mode === 'none') {
    return { sourceIndexHash: uninitializedHash, accepted: Object.create(null) };
  }
  if (active.manifest.fileKeyFingerprint !== scope.fileKeyFingerprint) {
    throw new Error('TOKEN_SOURCE_SCOPE_MISMATCH');
  }
  var entry = active.manifest.artifacts.find(function (candidate) {
    return candidate.role === 'observed-token-source-index';
  });
  if (!entry) return { sourceIndexHash: uninitializedHash, accepted: Object.create(null) };
  var bytes = generation.readEntry(entry), index;
  if (!bytes) throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
  try { index = JSON.parse(bytes.toString('utf8')); } catch (error) {
    throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
  }
  if (!index || index.schemaVersion !== 1 ||
      !/^sha256:[a-f0-9]{64}$/.test(String(index.semanticHash || '')) ||
      !index.scope || index.scope.fileKeyFingerprint !== scope.fileKeyFingerprint ||
      index.scope.branchKey !== scope.branchKey || !Array.isArray(index.sources)) {
    throw new Error('TOKEN_SOURCE_SCOPE_MISMATCH');
  }
  var requested = Object.create(null), accepted = Object.create(null);
  sourceIds.forEach(function (sourceId) { requested[sourceId] = 1; });
  index.sources.forEach(function (source) {
    if (!requested[source.sourceId]) return;
    var sequence = source.acceptedBatch && source.acceptedBatch.captureSequence;
    if (!Number.isSafeInteger(sequence) || sequence < 1) {
      throw new Error('TOKEN_GENERATION_RESYNC_REQUIRED');
    }
    accepted[source.sourceId] = sequence;
  });
  return { sourceIndexHash: index.semanticHash, accepted: accepted };
}
function validPlan(value) {
  if (!exact(value, ['schemaVersion', 'planId', 'taskStem', 'designSourceHash', 'sourceIndexHash',
    'accountFingerprint', 'connectorRevision', 'scope', 'createdAt', 'records']) ||
      value.schemaVersion !== 1 || !PLAN_RE.test(String(value.planId || '')) ||
      !STEM_RE.test(String(value.taskStem || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(value.designSourceHash || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(value.sourceIndexHash || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(value.accountFingerprint || '')) ||
      !value.scope || !/^sha256:[a-f0-9]{64}$/.test(String(value.scope.fileKeyFingerprint || '')) ||
      typeof value.scope.branchKey !== 'string' || !value.scope.branchKey ||
      !Array.isArray(value.records) || !value.records.length || value.records.length > 64) return false;
  var sources = Object.create(null), files = Object.create(null), origins = Object.create(null);
  return value.records.every(function (record) {
    var source = record && record.source;
    var originKey = record && record.screenKey + '\0' + record.variantId;
    var expectedPreflight = record && identity.hash({
      captureOperationId: record.captureOperationId,
      captureSequence: record.captureSequence,
      sourceId: source && source.sourceId
    });
    if (!record || !source || files[record.tokensFile] || origins[originKey] ||
        sources[source.sourceId] ||
        source.sourceId !== identity.sourceIdFor(source) ||
        source.fileKeyFingerprint !== value.scope.fileKeyFingerprint ||
        source.branchKey !== value.scope.branchKey ||
        source.kind !== 'screen' || !source.origin || source.origin.kind !== 'task-screen' ||
        source.origin.taskStem !== value.taskStem || source.origin.screenKey !== record.screenKey ||
        source.origin.variantId !== record.variantId || record.semanticPreflightHash !== expectedPreflight) return false;
    sources[source.sourceId] = record; files[record.tokensFile] = 1; origins[originKey] = 1;
    return true;
  });
}
function read(planId) {
  var file = planFile(planId);
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, ROOT, file, PLAN_MAX);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return null;
  var value;
  try { value = JSON.parse(hit.bytes.toString('utf8')); } catch (error) { return null; }
  return validPlan(value) && value.planId === planId ? value : null;
}
function recoveryPlans() {
  var entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(ROOT), ROOT);
  var plans = [];
  if (entry && entry.status !== 'missing') {
    if (entry.status !== 'present' || !entry.stat || !entry.stat.isDirectory() ||
        entry.stat.isSymbolicLink()) {
      throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
    }
    var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, ROOT, 1024);
    if (!listed.ok) throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
    listed.names.forEach(function (name) {
      if (!/^tokplan_[A-Za-z0-9_-]{16,96}\.json$/.test(name)) {
        throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
      }
      var plan = read(name.slice(0, -5));
      if (!plan) throw new Error('TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE');
      plans.push({
        planId: plan.planId,
        sourceIndexHash: plan.sourceIndexHash,
        createdAt: plan.createdAt,
        reservations: reservations(plan)
      });
    });
  }
  return plans;
}
function recoveryReservations() {
  return recoveryPlans().flatMap(function (plan) {
    return plan.reservations.map(function (reservation) {
      return {
        sourceId: reservation.sourceId,
        captureOperationId: reservation.captureOperationId,
        captureSequence: reservation.captureSequence,
        at: plan.createdAt
      };
    });
  });
}
function reconcileReservations() {
  var plans = recoveryPlans();
  return health.reconcileScreenReservations({ plans: plans });
}
function validateCaptured(planId) {
  var plan = read(planId);
  if (!plan) throw new Error('TOKEN_TASK_PLAN_INVALID');
  var directory = path.join(paths.FIGMA_CACHE_DIR, 'screens', plan.taskStem);
  var indexFile = path.join(directory, 'index.json');
  var indexHit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, indexFile, PLAN_MAX);
  var index;
  if (!indexHit || !indexHit.stat || String(indexHit.stat.nlink) !== '1') {
    throw new Error('TOKEN_TASK_SCREEN_INDEX_UNSAFE');
  }
  try { index = JSON.parse(indexHit.bytes.toString('utf8')); } catch (error) {
    throw new Error('TOKEN_TASK_SCREEN_INDEX_INVALID');
  }
  if (!index || index.schemaVersion !== 3 || index.taskStem !== plan.taskStem || !index.nodes) {
    throw new Error('TOKEN_TASK_SCREEN_INDEX_INVALID');
  }
  var variants = Object.create(null);
  Object.keys(index.nodes).forEach(function (screenKey) {
    var node = index.nodes[screenKey];
    if (!node || !Array.isArray(node.variants)) throw new Error('TOKEN_TASK_SCREEN_INDEX_INVALID');
    node.variants.forEach(function (variant) {
      var key = screenKey + '\0' + variant.id;
      if (variants[key]) throw new Error('TOKEN_TASK_SCREEN_VARIANT_DUPLICATE');
      variants[key] = variant;
    });
  });
  var sidecars = plan.records.map(function (record) {
    var variant = variants[record.screenKey + '\0' + record.variantId];
    if (!variant || variant.tokensFile !== record.tokensFile ||
        variant.captureOperationId !== record.captureOperationId ||
        variant.captureSequence !== record.captureSequence ||
        variant.nodeId !== record.source.nodeId ||
        variant.theme !== record.source.context.theme ||
        variant.locale !== record.source.context.locale ||
        variant.platform !== record.source.context.platform) {
      throw new Error('TOKEN_TASK_SCREEN_VARIANT_MISMATCH');
    }
    delete variants[record.screenKey + '\0' + record.variantId];
    var file = path.join(directory, record.tokensFile);
    var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, file, 4 * 1024 * 1024);
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') throw new Error('TOKEN_TASK_SIDECAR_UNSAFE');
    var capture;
    try { capture = JSON.parse(hit.bytes.toString('utf8')); } catch (error) {
      throw new Error('TOKEN_TASK_SIDECAR_INVALID');
    }
    var captureBytesHash = 'sha256:' + require('crypto').createHash('sha256').update(hit.bytes).digest('hex');
    if (variant.tokensHash !== captureBytesHash ||
        capture.captureOperationId !== record.captureOperationId ||
        capture.captureSequence !== record.captureSequence ||
        capture.accountFingerprint !== plan.accountFingerprint ||
        capture.connectorRevision !== plan.connectorRevision ||
        identity.canonical(capture.source) !== identity.canonical(record.source)) {
      throw new Error('TOKEN_TASK_SIDECAR_PLAN_MISMATCH');
    }
    return { record: record, bytes: hit.bytes, capture: capture, bytesHash: captureBytesHash };
  });
  if (Object.keys(variants).length) throw new Error('TOKEN_TASK_UNPLANNED_VARIANT');
  return { plan: plan, sidecars: sidecars };
}
function prepare(stem) {
  reconcileReservations();
  var design = designParser.parseDesignSources(taskBodies(stem));
  if (!design.hasPullable || design.issues.length) throw new Error('TOKEN_TASK_DESIGN_INVALID');
  var bound = binding(design);
  var sourceRows = [];
  design.entries.filter(function (entry) { return !entry.none; }).forEach(function (entry) {
    Object.keys(entry.themes).sort(function (left, right) {
      return ['primary', 'light', 'dark'].indexOf(left) - ['primary', 'light', 'dark'].indexOf(right);
    }).forEach(function (theme) {
      var parsed = entry.themes[theme], variant = themeRecord(theme);
      var source = {
        fileKeyFingerprint: bound.fileKeyFingerprint,
        branchKey: bound.branchKey,
        nodeId: parsed.nodeId,
        kind: 'screen',
        context: { theme: variant.contextTheme, locale: 'default', platform: 'shared' },
        origin: { kind: 'task-screen', taskStem: stem, screenKey: entry.screen, variantId: variant.variantId }
      };
      source.sourceId = identity.sourceIdFor(source);
      sourceRows.push({
        screenKey: entry.screen,
        variantId: variant.variantId,
        tokensFile: entry.screen + variant.suffix + '.tokens.json',
        source: source
      });
    });
  });
  if (!sourceRows.length || sourceRows.length > 64) throw new Error('TOKEN_TASK_SOURCE_LIMIT_EXCEEDED');
  var uniqueSources = [];
  var seenSources = Object.create(null);
  sourceRows.forEach(function (row) {
    if (seenSources[row.source.sourceId]) throw new Error('TOKEN_TASK_SOURCE_DUPLICATE');
    seenSources[row.source.sourceId] = 1;
    uniqueSources.push({ sourceId: row.source.sourceId, acceptedSequence: 0 });
  });
  var scope = { fileKeyFingerprint: bound.fileKeyFingerprint, branchKey: bound.branchKey };
  var currentSources = currentSourceState(scope, uniqueSources.map(function (row) { return row.sourceId; }));
  uniqueSources.forEach(function (row) {
    row.acceptedSequence = currentSources.accepted[row.sourceId] || 0;
  });
  var planId = 'tokplan_' + require('crypto').randomBytes(16).toString('hex');
  var reservation = health.reserveMany({
    sourceIndexHash: currentSources.sourceIndexHash,
    ownerId: planId,
    sources: uniqueSources
  });
  var reserved = Object.create(null);
  reservation.reservations.forEach(function (row) { reserved[row.sourceId] = row; });
  var plan = {
    schemaVersion: 1,
    planId: planId,
    taskStem: stem,
    designSourceHash: design.sourceHash,
    sourceIndexHash: currentSources.sourceIndexHash,
    accountFingerprint: bound.accountFingerprint,
    connectorRevision: 'figma-mcp-session-v1',
    scope: scope,
    createdAt: new Date().toISOString(),
    records: sourceRows.map(function (row) {
      var held = reserved[row.source.sourceId];
      return {
        screenKey: row.screenKey,
        variantId: row.variantId,
        tokensFile: row.tokensFile,
        captureOperationId: held.captureOperationId,
        captureSequence: held.captureSequence,
        semanticPreflightHash: identity.hash({
          captureOperationId: held.captureOperationId,
          captureSequence: held.captureSequence,
          sourceId: row.source.sourceId
        }),
        source: row.source
      };
    })
  };
  if (!validPlan(plan)) throw new Error('TOKEN_TASK_PLAN_INVALID');
  var bytes = Buffer.from(JSON.stringify(plan, null, 2) + '\n');
  var result = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, ROOT, planFile(planId), bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: PLAN_MAX });
  if (!result.ok) {
    try { fail(plan, 'TOKEN_TASK_PLAN_WRITE_FAILED'); } catch (error) {}
    throw new Error('TOKEN_TASK_PLAN_WRITE_FAILED');
  }
  return { planId: planId, path: relative(planFile(planId)), plan: plan };
}
function reservations(plan) {
  var seen = Object.create(null);
  return plan.records.filter(function (record) {
    if (seen[record.source.sourceId]) return false;
    seen[record.source.sourceId] = 1;
    return true;
  }).map(function (record) {
    return {
      sourceId: record.source.sourceId,
      captureOperationId: record.captureOperationId,
      captureSequence: record.captureSequence
    };
  });
}
function fail(planOrId, errorCode) {
  var plan = typeof planOrId === 'string' ? read(planOrId) : planOrId;
  if (!plan) return false;
  health.complete({
    sourceIndexHash: plan.sourceIndexHash,
    reservations: reservations(plan),
    outcome: 'failed',
    errorCode: errorCode || 'TOKEN_TASK_CAPTURE_FAILED',
    retryable: true,
    jobId: plan.planId,
    action: 'task-ingestion',
    startedAt: plan.createdAt,
    summaryOutcome: 'failed'
  });
  fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, ROOT, planFile(plan.planId), { allowMissing: true });
  return true;
}
function remove(planId) {
  return fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, ROOT, planFile(planId), { allowMissing: true });
}
function consumeReceipt(taskStem, intentSources) {
  if (!STEM_RE.test(String(taskStem || '')) || !Array.isArray(intentSources)) return false;
  var expected = Object.create(null);
  intentSources.forEach(function (row) {
    expected[row.sourceId] = row.captureOperationId + '\0' + row.captureSequence;
  });
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, ROOT, 1024);
  if (!listed.ok) return false;
  var consumed = false;
  listed.names.filter(function (name) { return /^tokplan_[A-Za-z0-9_-]{16,96}\.json$/.test(name); }).forEach(function (name) {
    var plan = read(name.slice(0, -5));
    if (!plan || plan.taskStem !== taskStem || plan.records.length !== intentSources.length) return;
    var match = plan.records.every(function (record) {
      return expected[record.source.sourceId] === record.captureOperationId + '\0' + record.captureSequence;
    });
    if (match && remove(plan.planId)) consumed = true;
  });
  return consumed;
}

module.exports = {
  prepare: prepare,
  read: read,
  validateCaptured: validateCaptured,
  fail: fail,
  remove: remove,
  consumeReceipt: consumeReceipt,
  reservations: reservations,
  reconcileReservations: reconcileReservations,
  recoveryReservations: recoveryReservations,
  prepareErrorCode: prepareErrorCode,
  ROOT: ROOT,
  _test: Object.freeze({ currentSourceState: currentSourceState })
};
