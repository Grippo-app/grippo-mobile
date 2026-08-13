'use strict';

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var persistence = require('./persistence');
var finalizations = require('./finalizations');
var writerLeases = require('../../tasks/writer-leases.cjs');
var sourceRevision = require('../../tasks/project-source-revision.cjs');
var taskSourceContract = require('../../tasks/task-source-contract.cjs');
var appRunConfig = require('./app-run-config');
var discovery = require('./device-discovery');
var processRunner = require('./app-run-process');
var worktreeManager = require('./worktree-manager');
var storage = require('./app-run-storage');
var redaction = require('./app-run-redaction');
var artifacts = require('./app-run-artifacts');
var validation = require('./app-run-validation');
var historyPagination = require('./history-pagination');
var android = require('./android-runner');
var ios = require('./ios-runner');

var RUN_STAGES = Object.freeze([
  ['detecting', 'Detecting environment'],
  ['waiting-for-project', 'Waiting for project'],
  ['starting-device', 'Starting device'],
  ['building', 'Building'],
  ['resolving-artifact', 'Resolving artifact'],
  ['installing', 'Installing'],
  ['launching', 'Launching']
]);
var ACTIVE_STATES = Object.freeze({
  queued: 1, detecting: 1, 'waiting-for-project': 1, 'creating-device': 1,
  'starting-device': 1, building: 1, 'resolving-artifact': 1,
  installing: 1, launching: 1, stopping: 1
});
var CANCELLABLE = Object.freeze({
  queued: 1, detecting: 1, 'waiting-for-project': 1, 'creating-device': 1,
  'starting-device': 1, building: 1, 'resolving-artifact': 1, installing: 1, launching: 1
});
var TERMINAL = Object.freeze({ completed: 1, cancelled: 1, failed: 1, interrupted: 1 });
var BUILD_MODES = Object.freeze({ rebuild: 1, 'if-needed': 1, 'last-build': 1 });
var JOB_STATES = Object.freeze({
  queued: 1, detecting: 1, 'waiting-for-project': 1, 'creating-device': 1,
  'starting-device': 1, building: 1, 'resolving-artifact': 1, installing: 1,
  launching: 1, running: 1, stopping: 1, completed: 1, cancelled: 1,
  failed: 1, interrupted: 1
});
var RUN_ACTION_STATES = Object.freeze({
  queued: 1, detecting: 1, 'waiting-for-project': 1, 'starting-device': 1,
  building: 1, 'resolving-artifact': 1, installing: 1, launching: 1,
  running: 1, stopping: 1, completed: 1, cancelled: 1, failed: 1,
  interrupted: 1
});
var CREATE_ACTION_STATES = Object.freeze({
  queued: 1, detecting: 1, 'creating-device': 1, completed: 1,
  cancelled: 1, failed: 1, interrupted: 1
});
var JOB_ERROR_CODES = Object.freeze({
  'adb-unavailable': 1,
  'android-boot-timeout': 1,
  'android-project-missing': 1,
  'android-sdk-ambiguous': 1,
  'android-sdk-invalid': 1,
  'android-sdk-missing': 1,
  'android-toolchain-incomplete': 1,
  'android-run-config-missing': 1,
  'app-run-config-invalid': 1,
  'artifact-architecture-mismatch': 1,
  'artifact-config-mismatch': 1,
  'artifact-hash-mismatch': 1,
  'artifact-invalid': 1,
  'artifact-not-found': 1,
  'artifact-path-mismatch': 1,
  'artifact-scope-mismatch': 1,
  'artifact-toolchain-mismatch': 1,
  'build-failed': 1,
  cancelled: 1,
  'device-create-failed': 1,
  'discovery-failed': 1,
  'execution-binding-unavailable': 1,
  'install-failed': 1,
  'ios-boot-failed': 1,
  'ios-boot-timeout': 1,
  'ios-disabled': 1,
  'ios-project-missing': 1,
  'ios-requires-macos': 1,
  'ios-run-config-missing': 1,
  'launch-failed': 1,
  'no-ios-runtime': 1,
  'operation-failed': 1,
  'process-interrupted': 1,
  'project-busy': 1,
  'project-config-invalid': 1,
  'project-config-unavailable': 1,
  'project-not-generated': 1,
  'scan-limit-exceeded': 1,
  'symlink-input': 1,
  'hardlink-input': 1,
  'source-changed': 1,
  'source-race': 1,
  'simctl-output-invalid': 1,
  'target-not-found': 1,
  'task-context-changed': 1,
  'unsafe-source-input': 1,
  'unsafe-project-root': 1,
  'invalid-app-root': 1,
  'invalid-task-stem': 1,
  'unknown-profile': 1,
  'variant-not-found': 1,
  'writer-lease-lost': 1,
  'writer-lease-release-failed': 1,
  'writer-lease-unavailable': 1,
  'xcode-missing': 1,
  'xcode-setup-incomplete': 1
});
var RUN_JOB_FIELDS_PRIVATE = Object.freeze([
  'schemaVersion', 'jobId', 'jobRevision', 'action', 'linkedJobId', 'platform',
  'targetId', 'targetStableHint', 'rawTargetIdentifier', 'deviceSummary',
  'variantId', 'buildMode', 'taskStem', 'surfaceId',
  'worktreeId', 'executionRoot', 'executionRunId', 'candidateTree', 'applicationId',
  'requestedProjectSourceRevision', 'productSourceRevision',
  'appProjectSourceRevision', 'runConfigHash',
  'phase', 'state', 'progress', 'stages', 'artifactId', 'sessionId',
  'startedAt', 'updatedAt', 'finishedAt', 'result', 'errorCode',
  'processIdentities', 'taskContextRevision', 'whenBusy',
  'idempotencyKeyHash', 'requestHash', 'confirmedArtifactId'
]);
var CREATE_JOB_FIELDS_PRIVATE = Object.freeze([
  'schemaVersion', 'jobId', 'jobRevision', 'action', 'linkedJobId', 'platform',
  'profileId', 'runtimeId', 'creationPreviewHash', 'phase', 'state', 'progress',
  'stages', 'startedAt', 'updatedAt', 'finishedAt', 'result', 'errorCode',
  'processIdentities', 'idempotencyKeyHash', 'requestHash'
]);
var SESSION_FIELDS_PRIVATE = Object.freeze([
  'schemaVersion', 'sessionId', 'sessionRevision', 'jobId', 'state', 'platform',
  'targetId', 'targetStableHint', 'rawTargetIdentifier', 'deviceSummary', 'variantId',
  'worktreeId', 'executionRoot', 'executionRunId', 'candidateTree',
  'artifactId', 'applicationId', 'requestedProjectSourceRevision',
  'appProjectSourceRevision', 'runConfigHash', 'launchedAt', 'updatedAt',
  'taskStem', 'surfaceId'
]);
var LEASE_TTL_MS = 12 * 60 * 1000;
var LEASE_RENEW_MS = 4 * 60 * 1000;

var runtime = {
  jobs: new Map(),
  sessions: new Map(),
  logs: new Map(),
  logBytes: new Map(),
  logFlushTimers: new Map(),
  controllers: new Map(),
  idempotency: new Map(),
  mutations: new Map(),
  confirmations: new Map(),
  devicePreviews: new Map(),
  activeJobId: null,
  currentSessionId: null,
  notify: function () {},
  persistPreference: null,
  commandRunner: processRunner,
  androidTools: null,
  iosTools: null,
  forceIos: false,
  sourceCache: null,
  sourceWatcher: null,
  sourceWatcherState: null,
  integrityIssues: [],
  initialized: false
};

function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(String(value), 'utf8').digest('hex');
}
function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function now() { return new Date().toISOString(); }
function safeStem(value) {
  return value === null || taskSourceContract.safeTaskStem(String(value));
}
function safeSurface(value) { return value === null || /^srf-[a-f0-9]{24}$/.test(String(value)); }
function safeIdempotency(value) { return /^[A-Za-z0-9._:-]{8,120}$/.test(String(value || '')); }
function exactInstant(value) {
  return typeof value === 'string' && Number.isFinite(Date.parse(value)) &&
    new Date(value).toISOString() === value;
}
// The revision of the sources a build would actually compile. It MUST be taken
// from the tree the build uses: a task-bound job builds its own checkout, so
// hashing the control root would compare an unrelated tree on admission, before
// the build and after it, and stamp the artifact manifest with a revision the
// artifact was never built from. The cache is keyed by root for the same
// reason — a control-root value served for a candidate is the same bug.
function source(fresh, root) {
  // `null` is the caller saying "ownership could not be read". That is not the
  // control root — reporting the control root's revision for a task whose tree
  // we cannot identify is the misattribution this whole split exists to stop.
  if (root === null) {
    return {
      available: false, revision: null, inputCount: 0, contentBytes: 0,
      profile: 'app-build', profileVersion: null,
      limitations: ['execution-binding-unavailable'],
      reasonCode: 'execution-binding-unavailable',
      detail: 'the worktree record store could not be read, so the product tree is unknown'
    };
  }
  var productRoot = root === undefined ? paths.PROJECT_ROOT : root;
  if (!fresh && runtime.sourceCache && runtime.sourceCache.root === productRoot &&
      Date.now() - runtime.sourceCache.createdMs < 1000) {
    return runtime.sourceCache.value;
  }
  var config = appRunConfig.load();
  var value = sourceRevision.compute(productRoot, {
    profile: 'app-build',
    appRoots: appRunConfig.sourceRoots(config)
  });
  runtime.sourceCache = { createdMs: Date.now(), root: productRoot, value: value };
  return value;
}

function jobErrorCode(value) {
  if (value === null || value === undefined || value === '') return null;
  return JOB_ERROR_CODES[value] ? value : 'operation-failed';
}

function executionScopeFieldsValid(value) {
  var unbound = value && value.worktreeId === null && value.executionRoot === null &&
    value.executionRunId === null && value.candidateTree === null;
  var bound = value && /^wt-[a-f0-9]{32}$/.test(String(value.worktreeId || '')) &&
    path.isAbsolute(String(value.executionRoot || '')) &&
    /^[0-9]{1,16}-[a-z0-9]{1,32}$/.test(String(value.executionRunId || '')) &&
    /^[a-f0-9]{40}$/.test(String(value.candidateTree || ''));
  return (unbound || bound) && !(value.taskStem === null && bound);
}

function assertStoredJobShape(job) {
  var fields = job && job.action === 'create-device'
    ? CREATE_JOB_FIELDS_PRIVATE : RUN_JOB_FIELDS_PRIVATE;
  if (!exactKeys(job, fields)) {
    var error = new Error('stored app-run job fields do not match the exact private contract');
    error.code = 'job-state-invalid';
    throw error;
  }
  var rawTargetValid = job.action === 'create-device' || (job.platform === 'android'
    ? /^(?:emulator-\d{1,10}|[A-Za-z0-9._-]{1,120})$/.test(String(job.rawTargetIdentifier || ''))
    : /^[A-Fa-f0-9]{8}(?:-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12}$/
      .test(String(job.rawTargetIdentifier || '')));
  var timestampsValid = [job.startedAt, job.updatedAt].every(function (value) {
    return typeof value === 'string' && Number.isFinite(Date.parse(value)) &&
      new Date(value).toISOString() === value;
  }) && (job.finishedAt === null || (typeof job.finishedAt === 'string' &&
    Number.isFinite(Date.parse(job.finishedAt)) &&
    new Date(job.finishedAt).toISOString() === job.finishedAt)) &&
    Date.parse(job.updatedAt) >= Date.parse(job.startedAt) &&
    (job.finishedAt === null || Date.parse(job.finishedAt) >= Date.parse(job.startedAt));
  if (!rawTargetValid || !timestampsValid ||
      !/^sha256:[a-f0-9]{64}$/.test(String(job.idempotencyKeyHash || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(job.requestHash || '')) ||
      (job.errorCode !== null && jobErrorCode(job.errorCode) !== job.errorCode)) {
    var commonInvalid = new Error('stored app-run job private fields are invalid');
    commonInvalid.code = 'job-state-invalid';
    throw commonInvalid;
  }
  if (job.action === 'create-device') {
    if (job.linkedJobId !== null ||
        (job.result !== null && (!exactKeys(job.result, ['displayName', 'stableMaterial']) ||
          typeof job.result.displayName !== 'string' ||
          Buffer.byteLength(job.result.displayName, 'utf8') > 240 ||
          !/^(?:android:[A-Za-z0-9._-]{1,120}|ios:[A-Fa-f0-9]{8}(?:-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12})$/
            .test(String(job.result.stableMaterial || ''))))) {
      var createInvalid = new Error('stored create-device job private fields are invalid');
      createInvalid.code = 'job-state-invalid';
      throw createInvalid;
    }
    return;
  }
  if ((job.linkedJobId !== null && !/^job-[a-f0-9]{36}$/.test(String(job.linkedJobId))) ||
      !/^hint-[a-f0-9]{32}$/.test(String(job.targetStableHint || '')) ||
      typeof job.deviceSummary !== 'string' || Buffer.byteLength(job.deviceSummary, 'utf8') > 300 ||
      (job.taskContextRevision !== null &&
        !/^sha256:[a-f0-9]{64}$/.test(String(job.taskContextRevision))) ||
      ['fail', 'queue'].indexOf(job.whenBusy) < 0 ||
      (job.confirmedArtifactId !== null &&
        !/^artifact-[a-f0-9]{36}$/.test(String(job.confirmedArtifactId))) ||
      !executionScopeFieldsValid(job) ||
      (job.result !== null && (!exactKeys(job.result,
        Object.prototype.hasOwnProperty.call(job.result, 'stopped')
          ? ['launched', 'rebuilt', 'pidObserved', 'stopped']
          : ['launched', 'rebuilt', 'pidObserved']) ||
        typeof job.result.launched !== 'boolean' || typeof job.result.rebuilt !== 'boolean' ||
        typeof job.result.pidObserved !== 'boolean' ||
        (Object.prototype.hasOwnProperty.call(job.result, 'stopped') &&
          typeof job.result.stopped !== 'boolean')))) {
    var runInvalid = new Error('stored run job private fields are invalid');
    runInvalid.code = 'job-state-invalid';
    throw runInvalid;
  }
}

function publicStage(row) {
  return {
    id: row.id, label: row.label, status: row.status,
    startedAt: row.startedAt, durationMs: row.durationMs, message: row.message
  };
}

function publicJob(job) {
  if (!job) return null;
  var out = {
    schemaVersion: 1,
    jobId: job.jobId,
    jobRevision: job.jobRevision,
    action: job.action,
    linkedJobId: job.linkedJobId || null,
    platform: job.platform,
    targetId: job.targetId || null,
    variantId: job.variantId || null,
    buildMode: job.buildMode || null,
    taskStem: job.taskStem || null,
    surfaceId: job.surfaceId || null,
    requestedProjectSourceRevision: job.requestedProjectSourceRevision || null,
    appProjectSourceRevision: job.appProjectSourceRevision || null,
    runConfigHash: job.runConfigHash || null,
    phase: job.phase,
    state: job.state,
    progress: job.progress,
    stages: (job.stages || []).map(publicStage),
    artifactId: job.artifactId || null,
    sessionId: job.sessionId || null,
    startedAt: job.startedAt,
    updatedAt: job.updatedAt,
    finishedAt: job.finishedAt || null,
    result: null,
    errorCode: jobErrorCode(job.errorCode)
  };
  if (job.result && job.action === 'create-device') {
    out.result = {
      managedDeviceCreated: true,
      displayName: String(job.result.displayName || '').slice(0, 120),
      targetStableHint: discovery.stableHint(job.result.stableMaterial)
    };
  } else if (job.result) {
    out.result = {
      launched: job.result.launched === true,
      rebuilt: job.result.rebuilt === true,
      pidObserved: job.result.pidObserved === true,
      stopped: Object.prototype.hasOwnProperty.call(job.result, 'stopped') ? job.result.stopped === true : null
    };
  }
  if (job.action === 'create-device') {
    delete out.targetId; delete out.variantId; delete out.buildMode; delete out.taskStem;
    delete out.surfaceId; delete out.requestedProjectSourceRevision;
    delete out.appProjectSourceRevision; delete out.runConfigHash;
    delete out.artifactId; delete out.sessionId;
    out.profileId = job.profileId;
    out.runtimeId = job.runtimeId;
    out.creationPreviewHash = job.creationPreviewHash;
  }
  return out;
}

function publicSession(session) {
  if (!session) return null;
  var current = source(false, sessionProductRoot(session));
  return {
    schemaVersion: 1,
    sessionId: session.sessionId,
    sessionRevision: session.sessionRevision,
    jobId: session.jobId,
    state: session.state,
    platform: session.platform,
    targetId: session.targetId,
    variantId: session.variantId,
    artifactId: session.artifactId,
    applicationId: session.applicationId,
    requestedProjectSourceRevision: session.requestedProjectSourceRevision,
    appProjectSourceRevision: session.appProjectSourceRevision,
    runConfigHash: session.runConfigHash,
    launchedAt: session.launchedAt,
    updatedAt: session.updatedAt,
    taskStem: session.taskStem,
    surfaceId: session.surfaceId,
    deviceSummary: session.deviceSummary,
    sourceState: current.available && current.revision === session.appProjectSourceRevision ? 'current' : 'changed'
  };
}

function latchIntegrity(code, recordId) {
  var duplicate = runtime.integrityIssues.some(function (row) {
    return row.code === code && row.recordId === (recordId || null);
  });
  if (!duplicate && runtime.integrityIssues.length < 100) {
    runtime.integrityIssues.push({ code: code, recordId: recordId || null });
  }
}

function persistJob(job) {
  assertStoredJobShape(job);
  assertJobInvariant(job);
  try { storage.writeJson(paths.APP_RUN_JOBS_DIR, job.jobId, job); }
  catch (error) {
    latchIntegrity('job-storage-unavailable', job.jobId);
    throw error;
  }
  persistIndex();
}
function persistSession(session) {
  assertSessionInvariant(session);
  try { storage.writeJson(paths.APP_RUN_SESSIONS_DIR, session.sessionId, session); }
  catch (error) {
    latchIntegrity('session-storage-unavailable', session.sessionId);
    throw error;
  }
  persistIndex();
}
function persistIndex() {
  var value = {
    schemaVersion: 1,
    activeJobId: runtime.activeJobId,
    currentSessionId: runtime.currentSessionId,
    jobIds: Array.from(runtime.jobs.keys()).sort(),
    sessionIds: Array.from(runtime.sessions.keys()).sort(),
    updatedAt: now()
  };
  try {
    storage.writeFileAtomic(paths.APP_RUN_INDEX_FILE,
      Buffer.from(JSON.stringify(value, null, 2) + '\n', 'utf8'), storage.MAX_JSON);
  } catch (error) {
    latchIntegrity('index-storage-unavailable', null);
    throw error;
  }
}
function safeProcessIdentity(value) {
  return !!value && exactKeys(value, ['kind', 'pid', 'processStartId']) &&
    value.kind === 'emulator' && Number.isSafeInteger(value.pid) && value.pid > 0 &&
    writerLeases.PROCESS_START_ID_RE.test(String(value.processStartId || ''));
}
function assertSessionInvariant(session) {
  var rawTargetValid = session && (session.platform === 'android'
    ? /^emulator-\d{1,10}$/.test(String(session.rawTargetIdentifier || ''))
    : /^[A-Fa-f0-9]{8}(?:-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12}$/
      .test(String(session.rawTargetIdentifier || '')));
  var unbound = session && session.worktreeId === null && session.executionRoot === null &&
    session.executionRunId === null && session.candidateTree === null;
  var bound = session && /^wt-[a-f0-9]{32}$/.test(String(session.worktreeId || '')) &&
    path.isAbsolute(String(session.executionRoot || '')) &&
    /^[0-9]{1,16}-[a-z0-9]{1,32}$/.test(String(session.executionRunId || '')) &&
    /^[a-f0-9]{40}$/.test(String(session.candidateTree || ''));
  if (!session || !exactKeys(session, SESSION_FIELDS_PRIVATE) || session.schemaVersion !== 1 ||
      !/^session-[a-f0-9]{36}$/.test(String(session.sessionId || '')) ||
      !Number.isSafeInteger(session.sessionRevision) || session.sessionRevision < 1 ||
      !/^job-[a-f0-9]{36}$/.test(String(session.jobId || '')) ||
      ['running', 'stopping', 'stopped', 'lost'].indexOf(session.state) < 0 ||
      ['android', 'ios'].indexOf(session.platform) < 0 || !rawTargetValid ||
      !/^target-[a-f0-9]{32}$/.test(String(session.targetId || '')) ||
      !/^hint-[a-f0-9]{32}$/.test(String(session.targetStableHint || '')) ||
      typeof session.deviceSummary !== 'string' || Buffer.byteLength(session.deviceSummary, 'utf8') > 300 ||
      /[\x00-\x1f\x7f]/.test(session.deviceSummary) ||
      !/^[a-z][a-z0-9-]{0,31}$/.test(String(session.variantId || '')) ||
      !/^artifact-[a-f0-9]{36}$/.test(String(session.artifactId || '')) ||
      !appRunConfig.APP_ID_RE.test(String(session.applicationId || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(session.requestedProjectSourceRevision || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(session.appProjectSourceRevision || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(session.runConfigHash || '')) ||
      (!unbound && !bound) || (session.taskStem === null && !unbound) ||
      !Number.isFinite(Date.parse(session.launchedAt)) ||
      new Date(session.launchedAt).toISOString() !== session.launchedAt ||
      !Number.isFinite(Date.parse(session.updatedAt)) ||
      new Date(session.updatedAt).toISOString() !== session.updatedAt ||
      Date.parse(session.updatedAt) < Date.parse(session.launchedAt) ||
      !safeStem(session.taskStem) || !safeSurface(session.surfaceId)) {
    var error = new Error('app-run session invariant failed');
    error.code = 'session-state-invalid';
    throw error;
  }
}
function validStage(value) {
  return !!value && exactKeys(value, ['id', 'label', 'status', 'startedAt', 'durationMs', 'message']) &&
    typeof value.id === 'string' && value.id.length >= 1 && value.id.length <= 80 &&
    typeof value.label === 'string' && value.label.length >= 1 &&
    Buffer.byteLength(value.label, 'utf8') <= 160 && !/[\x00-\x1f\x7f]/.test(value.label) &&
    ['queued', 'running', 'success', 'skipped', 'failed'].indexOf(value.status) >= 0 &&
    (value.startedAt === null || exactInstant(value.startedAt)) &&
    (value.durationMs === null || (Number.isSafeInteger(value.durationMs) && value.durationMs >= 0)) &&
    (value.message === null || (typeof value.message === 'string' &&
      Buffer.byteLength(value.message, 'utf8') <= 1000 &&
      !/[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]/.test(value.message)));
}
function assertJobInvariant(job) {
  var actionStates = job && job.action === 'create-device'
    ? CREATE_ACTION_STATES : RUN_ACTION_STATES;
  if (!job || job.schemaVersion !== 1 || !/^job-[a-f0-9]{36}$/.test(String(job.jobId || '')) ||
      !Number.isSafeInteger(job.jobRevision) || job.jobRevision < 1 ||
      ['run', 'restart', 'install', 'create-device'].indexOf(job.action) < 0 ||
      ['android', 'ios'].indexOf(job.platform) < 0 || !JOB_STATES[job.state] ||
      !JOB_STATES[job.phase] || !actionStates[job.state] || !actionStates[job.phase] ||
      !Number.isSafeInteger(job.progress) || job.progress < 0 || job.progress > 100 ||
      !Array.isArray(job.stages) || job.stages.length < 1 || job.stages.length > 12 ||
      job.stages.some(function (stage) { return !validStage(stage); }) ||
      !Array.isArray(job.processIdentities) || job.processIdentities.length > 4 ||
      job.processIdentities.some(function (identity) { return !safeProcessIdentity(identity); })) {
    var invalid = new Error('app-run job invariant failed'); invalid.code = 'job-state-invalid'; throw invalid;
  }
  var expectedStages = job.action === 'create-device'
    ? [['detecting', 'Detecting environment'], ['creating-device', 'Creating device']]
    : RUN_STAGES;
  if (job.stages.length !== expectedStages.length || job.stages.some(function (stage, index) {
    return stage.id !== expectedStages[index][0] || stage.label !== expectedStages[index][1];
  })) {
    var stagesInvalid = new Error('app-run job stages do not match its action');
    stagesInvalid.code = 'job-state-invalid';
    throw stagesInvalid;
  }
  if ((TERMINAL[job.state] && !exactInstant(job.finishedAt)) ||
      (!TERMINAL[job.state] && job.finishedAt !== null) ||
      (TERMINAL[job.state] && job.finishedAt !== job.updatedAt)) {
    var finishInvalid = new Error('app-run job completion timestamp does not match its state');
    finishInvalid.code = 'job-state-invalid';
    throw finishInvalid;
  }
  var runningStages = job.stages.filter(function (stage) { return stage.status === 'running'; });
  if (runningStages.length > 1 ||
      (runningStages.length === 1 && runningStages[0].id !== job.state && !TERMINAL[job.state])) {
    var stageStateInvalid = new Error('app-run job stage/state relationship is invalid');
    stageStateInvalid.code = 'job-state-invalid';
    throw stageStateInvalid;
  }
  if (job.state === 'queued' && job.stages.some(function (stage) { return stage.status !== 'queued'; })) {
    var queuedInvalid = new Error('queued app-run job already has active stages');
    queuedInvalid.code = 'job-state-invalid';
    throw queuedInvalid;
  }
  if ((job.state === 'running' || job.state === 'stopping' || job.state === 'completed') &&
      job.action !== 'create-device' &&
      (!/^artifact-[a-f0-9]{36}$/.test(String(job.artifactId || '')) ||
       !/^session-[a-f0-9]{36}$/.test(String(job.sessionId || '')) ||
       !/^sha256:[a-f0-9]{64}$/.test(String(job.appProjectSourceRevision || '')))) {
    var receiptInvalid = new Error('late app-run job is missing its artifact/session receipt');
    receiptInvalid.code = 'job-state-invalid';
    throw receiptInvalid;
  }
  if (job.action === 'create-device') {
    if (job.linkedJobId !== null ||
        !/^profile-[a-f0-9]{32}$/.test(String(job.profileId || '')) ||
        !/^runtime-[a-f0-9]{32}$/.test(String(job.runtimeId || '')) ||
        !/^sha256:[a-f0-9]{64}$/.test(String(job.creationPreviewHash || ''))) {
      var createInvalid = new Error('create-device job invariant failed'); createInvalid.code = 'job-state-invalid'; throw createInvalid;
    }
    if (job.state === 'completed' && (!job.result || typeof job.result.displayName !== 'string')) {
      var createResultInvalid = new Error('completed create-device job has no managed result');
      createResultInvalid.code = 'job-state-invalid';
      throw createResultInvalid;
    }
  } else if ((job.action === 'run' &&
        (job.linkedJobId !== null || job.buildMode === 'last-build')) ||
      (job.action === 'install' &&
        (job.linkedJobId !== null || job.buildMode !== 'last-build')) ||
      (job.action === 'restart' &&
        !/^job-[a-f0-9]{36}$/.test(String(job.linkedJobId || ''))) ||
      !/^target-[a-f0-9]{32}$/.test(String(job.targetId || '')) ||
      !/^[a-z][a-z0-9-]{0,31}$/.test(String(job.variantId || '')) ||
      !BUILD_MODES[job.buildMode] || !safeStem(job.taskStem) || !safeSurface(job.surfaceId) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(job.requestedProjectSourceRevision || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(job.runConfigHash || '')) ||
      ((job.state === 'running' || job.state === 'stopping' || job.state === 'completed') &&
        !job.result)) {
    var runInvalid = new Error('run job invariant failed'); runInvalid.code = 'job-state-invalid'; throw runInvalid;
  }
}
function transitionAllowed(action, from, to) {
  if (from === to) return true;
  if (TERMINAL[from]) return false;
  if (to === 'failed' || to === 'interrupted') return true;
  if (to === 'cancelled') return !!CANCELLABLE[from];
  var graph = action === 'create-device' ? {
    queued: ['detecting'],
    detecting: ['creating-device'],
    'creating-device': ['completed']
  } : {
    queued: ['detecting'],
    detecting: ['waiting-for-project', 'starting-device'],
    'waiting-for-project': ['starting-device'],
    'starting-device': ['building', 'resolving-artifact'],
    building: ['resolving-artifact'],
    'resolving-artifact': ['installing'],
    installing: ['launching'],
    launching: ['running'],
    running: ['stopping', 'completed'],
    stopping: ['completed']
  };
  return !!(graph[from] && graph[from].indexOf(to) >= 0);
}
function assertTransition(job, from, to) {
  if (!transitionAllowed(job.action, from, to)) {
    var error = new Error('illegal app-run job transition: ' + from + ' -> ' + to);
    error.code = 'job-transition-invalid';
    throw error;
  }
}
function notify(kind, payload) {
  try { runtime.notify(kind || 'app-run-status', payload || { t: Date.now() }); } catch (_) {}
}
function updateSourceWatcher() {
  var session = runtime.currentSessionId && runtime.sessions.get(runtime.currentSessionId);
  if (!session || session.state !== 'running') {
    if (runtime.sourceWatcher) clearInterval(runtime.sourceWatcher);
    runtime.sourceWatcher = null;
    runtime.sourceWatcherState = null;
    return;
  }
  if (runtime.sourceWatcher) return;
  runtime.sourceWatcherState = session.appProjectSourceRevision;
  runtime.sourceWatcher = setInterval(function () {
    var active = runtime.currentSessionId && runtime.sessions.get(runtime.currentSessionId);
    if (!active || active.state !== 'running') { updateSourceWatcher(); return; }
    var current = source(true, sessionProductRoot(active));
    var state = current.available ? current.revision : 'unavailable:' + current.reasonCode;
    if (state !== runtime.sourceWatcherState) {
      runtime.sourceWatcherState = state;
      notify('app-run-status', { sessionId: active.sessionId, sourceChanged: true });
    }
  }, 3000);
  if (runtime.sourceWatcher.unref) runtime.sourceWatcher.unref();
}
function updateJob(job, patch) {
  if (TERMINAL[job.state]) return job;
  var previousState = job.state;
  if (patch && patch.state) assertTransition(job, previousState, patch.state);
  Object.keys(patch || {}).forEach(function (key) { job[key] = patch[key]; });
  job.jobRevision++;
  job.updatedAt = now();
  assertJobInvariant(job);
  persistJob(job);
  notify('app-run-status', { jobId: job.jobId, revision: job.jobRevision });
  return job;
}
function finishJob(job, state, result, errorCode) {
  if (TERMINAL[job.state]) return job;
  assertTransition(job, job.state, state);
  var activeStage = job.stages.find(function (stage) { return stage.status === 'running'; });
  if (activeStage) {
    activeStage.status = state === 'cancelled' ? 'skipped' : 'failed';
    activeStage.durationMs = activeStage.startedAt
      ? Math.max(0, Date.now() - Date.parse(activeStage.startedAt)) : 0;
    activeStage.message = state === 'cancelled' ? 'Cancelled before completion.'
      : state === 'interrupted' ? 'Interrupted before completion.'
        : 'Stage failed. Open sanitized logs for details.';
  }
  job.state = state;
  job.phase = state;
  job.progress = state === 'completed' ? 100 : job.progress;
  job.result = result || null;
  job.errorCode = jobErrorCode(errorCode);
  job.finishedAt = now();
  job.updatedAt = job.finishedAt;
  job.jobRevision++;
  assertJobInvariant(job);
  if (runtime.activeJobId === job.jobId) runtime.activeJobId = null;
  persistJob(job);
  flushLogs(job.jobId);
  notify('app-run-status', { jobId: job.jobId, revision: job.jobRevision });
  pruneJobs();
  return job;
}

function setStage(job, id, status, message) {
  var stamp = Date.now();
  var row = (job.stages || []).find(function (item) { return item.id === id; });
  if (!row) return;
  if (status === 'running') {
    row.startedAt = now(); row.durationMs = null;
  } else if (row.startedAt && row.durationMs === null) {
    row.durationMs = Math.max(0, stamp - Date.parse(row.startedAt));
  }
  row.status = status;
  row.message = message || null;
  var index = job.stages.indexOf(row);
  job.progress = status === 'success' || status === 'skipped'
    ? Math.round(((index + 1) / job.stages.length) * 95) : Math.round((index / job.stages.length) * 95);
  updateJob(job, { phase: id, state: status === 'running' ? id : job.state });
}

function redactJobLine(job, value) {
  var text = redaction.line(value);
  var rawTarget = String(job && job.rawTargetIdentifier || '');
  if (rawTarget) {
    text = text.split(rawTarget).join('[DEVICE]');
  }
  return text;
}

function flushLogs(jobId) {
  var timer = runtime.logFlushTimers.get(jobId);
  if (timer) clearTimeout(timer);
  runtime.logFlushTimers.delete(jobId);
  var rows = runtime.logs.get(jobId);
  if (!rows) return;
  try {
    storage.writeFileAtomic(
      storage.fileFor(paths.APP_RUN_LOGS_DIR, jobId, '.json'),
      Buffer.from(JSON.stringify({ schemaVersion: 1, rows: rows }) + '\n', 'utf8'),
      2 * 1024 * 1024
    );
  } catch (_) {
    latchIntegrity('log-storage-unavailable', jobId);
  }
}

function scheduleLogFlush(jobId) {
  if (runtime.logFlushTimers.has(jobId)) return;
  var timer = setTimeout(function () { flushLogs(jobId); }, 250);
  runtime.logFlushTimers.set(jobId, timer);
}

function logRowBytes(row) {
  return Buffer.byteLength(JSON.stringify(row), 'utf8') + 1;
}

function appendLog(job, sourceName, phase, text) {
  var rows = runtime.logs.get(job.jobId) || [];
  var next = rows.length ? rows[rows.length - 1].sequence + 1 : 1;
  var row = {
    sequence: next, timestamp: now(), source: sourceName === 'stderr' ? 'stderr' : 'stdout',
    phase: String(phase || job.phase).slice(0, 80), text: redactJobLine(job, text)
  };
  rows.push(row);
  var bytes = runtime.logBytes.get(job.jobId) || 40;
  bytes += logRowBytes(row);
  while (rows.length > 10000 || bytes > 2 * 1024 * 1024) {
    bytes -= logRowBytes(rows.shift());
  }
  runtime.logs.set(job.jobId, rows);
  runtime.logBytes.set(job.jobId, bytes);
  scheduleLogFlush(job.jobId);
  notify('app-run-log', { jobId: job.jobId, cursor: 'cursor-' + next });
}

function validLogRow(row, previousSequence) {
  return !!row && exactKeys(row, ['sequence', 'timestamp', 'source', 'phase', 'text']) &&
    Number.isSafeInteger(row.sequence) && row.sequence > previousSequence &&
    exactInstant(row.timestamp) &&
    (row.source === 'stdout' || row.source === 'stderr') &&
    typeof row.phase === 'string' && row.phase.length >= 1 && row.phase.length <= 80 &&
    typeof row.text === 'string' && Buffer.byteLength(row.text, 'utf8') <= 4096 &&
    !/[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]/.test(row.text);
}

// §14/§15/§26. The ONE place that answers "which tree is this job's product".
// A job is either CONTROL-ROOT (no task, or a task that PROVABLY has no live
// generation) or TASK-BOUND (worktreeId + executionRoot, both from the same
// record). There is no third mode, and an unprovable binding is never resolved
// by falling back to the control root: that would compile the shared tree and
// stamp the artifact with a task's identity — the exact misattribution per-task
// isolation exists to prevent. Every consumer below takes `productRoot` from
// the job and never re-derives it.
//   { ok: true,  binding }  binding === null means a control-root job
//   { ok: false, error }    unprovable: refuse the job
function jobExecutionBinding(taskStem) {
  if (!taskStem) return { ok: true, binding: null };
  var got;
  try { got = worktreeManager.executionBindingFor(taskStem); }
  catch (error) { return { ok: false, error: 'execution-binding-unavailable' }; }
  if (!got.ok) return { ok: false, error: 'execution-binding-unavailable' };
  return { ok: true, binding: got.binding };
}

// Re-prove the exact generation after the execution lease is acquired. A job
// may have cached its binding before release claimed the record; the claim
// changes it to non-materialized `releasing`, so that stale job must withdraw
// every lease before it can spawn a build inside the checkout.
function jobExecutionBindingCurrent(job) {
  if (!job.taskStem) return true;
  var current = jobExecutionBinding(job.taskStem);
  if (!current.ok) return false;
  var scope = executionScopeFor(job.taskStem, current.binding);
  return scope.ok && executionScopeMatches(scope, job.worktreeId,
    job.executionRoot, job.executionRunId, job.candidateTree);
}

function executionBindingMatches(binding, worktreeId, executionRoot, executionRunId) {
  if (worktreeId === null && executionRoot === null && executionRunId === null) {
    return binding === null;
  }
  return !!binding && binding.worktreeId === worktreeId &&
    binding.executionRoot === executionRoot && binding.runId === executionRunId;
}

function executionScopeFor(taskStem, binding) {
  if (!binding) return { ok: true, binding: null, candidateTree: null };
  var pinned;
  try { pinned = worktreeManager.executionPinFor(taskStem, binding.runId); }
  catch (error) { return { ok: false, error: 'execution-binding-unavailable' }; }
  if (!pinned || !pinned.ok || !pinned.pin ||
      pinned.pin.worktreeId !== binding.worktreeId) {
    return { ok: false, error: 'execution-binding-unavailable' };
  }
  return { ok: true, binding: binding, candidateTree: pinned.pin.executionTree };
}

function executionScopeMatches(scope, worktreeId, executionRoot, executionRunId, candidateTree) {
  if (!scope || !scope.ok) return false;
  return executionBindingMatches(scope.binding, worktreeId, executionRoot, executionRunId) &&
    scope.candidateTree === candidateTree;
}

function storedExecutionScope(value) {
  return {
    ok: true,
    binding: value.worktreeId === null ? null : {
      worktreeId: value.worktreeId, executionRoot: value.executionRoot,
      runId: value.executionRunId
    },
    candidateTree: value.candidateTree
  };
}

// A launched app is evidence about the exact generation that built it. Looking
// up only taskStem would let a later checkout silently become the source and
// restart authority for the old installed app.
function sessionExecutionBinding(session) {
  if (!session) return { ok: false, error: 'execution-binding-unavailable' };
  if (!session.taskStem) {
    return executionScopeMatches({ ok: true, binding: null, candidateTree: null },
      session.worktreeId, session.executionRoot, session.executionRunId,
      session.candidateTree)
      ? { ok: true, binding: null, candidateTree: null }
      : { ok: false, error: 'execution-binding-unavailable' };
  }
  var current = jobExecutionBinding(session.taskStem);
  if (!current.ok) return { ok: false, error: 'execution-binding-unavailable' };
  var scope = executionScopeFor(session.taskStem, current.binding);
  if (!executionScopeMatches(scope, session.worktreeId,
      session.executionRoot, session.executionRunId, session.candidateTree)) {
    return { ok: false, error: 'execution-binding-unavailable' };
  }
  return scope;
}

function sessionProductRoot(session) {
  var current = sessionExecutionBinding(session);
  if (!current.ok) return null;
  return current.binding ? current.binding.executionRoot : paths.PROJECT_ROOT;
}

// The product root a job builds, installs and captures from. Total on a job
// that passed jobExecutionBinding: task-bound jobs carry a non-empty
// executionRoot, control-root jobs carry null and mean the control root.
function jobProductRoot(job) {
  return job && job.executionRoot ? job.executionRoot : paths.PROJECT_ROOT;
}

// The product root for a stem-scoped read (sessions, validation, the source
// watcher). Null means ownership is unprovable — the caller must report
// "unavailable", never substitute the control root.
function stemProductRoot(taskStem) {
  var execution = jobExecutionBinding(taskStem);
  if (!execution.ok) return null;
  return execution.binding ? execution.binding.executionRoot : paths.PROJECT_ROOT;
}

function newRunJob(body, config, target, linkedJobId, confirmedArtifactId, execution, productRevision) {
  var id = storage.randomId('job');
  var stages = RUN_STAGES.map(function (row) {
    return { id: row[0], label: row[1], status: 'queued', startedAt: null, durationMs: null, message: null };
  });
  var stamp = now();
  return {
    schemaVersion: 1,
    jobId: id, jobRevision: 1,
    action: linkedJobId ? 'restart' : body.buildMode === 'last-build' ? 'install' : 'run',
    linkedJobId: linkedJobId || null,
    platform: body.platform, targetId: body.targetId,
    targetStableHint: target.stableHint, rawTargetIdentifier: target.rawIdentifier,
    deviceSummary: target.displayName + (target.osVersion ? ' · ' + target.osVersion : ''),
    variantId: body.variantId, buildMode: body.buildMode,
    taskStem: body.taskStem, surfaceId: body.surfaceId,
    // The execution binding this job runs against (plan §15): a task with a
    // materialized generation builds, installs and captures from ITS OWN
    // checkout, and the record pins which one so an artifact can never be
    // attributed to the wrong tree. A job with no task, or a task with no live
    // generation, is a control-root run and carries nulls.
    worktreeId: execution.binding ? execution.binding.worktreeId : null,
    executionRoot: execution.binding ? execution.binding.executionRoot : null,
    executionRunId: execution.binding ? execution.binding.runId : null,
    candidateTree: execution.candidateTree,
    applicationId: config.project.applicationId,
    requestedProjectSourceRevision: body.expectedProjectSourceRevision,
    // The revision of the tree THIS job compiles. For a task-bound job that is
    // its own checkout, so build-time drift is measured against the sources the
    // build actually reads instead of an unrelated tree.
    productSourceRevision: productRevision,
    appProjectSourceRevision: null, runConfigHash: config.runConfigHash,
    phase: 'queued', state: 'queued', progress: 0, stages: stages,
    artifactId: null, sessionId: null, startedAt: stamp, updatedAt: stamp, finishedAt: null,
    result: null, errorCode: null, processIdentities: [],
    taskContextRevision: null,
    whenBusy: body.whenBusy,
    idempotencyKeyHash: hash(body.idempotencyKey),
    requestHash: hash(appRunConfig.canonicalJson(body)),
    confirmedArtifactId: confirmedArtifactId || null
  };
}

function newCreateJob(preview, body) {
  var id = storage.randomId('job'), stamp = now();
  return {
    schemaVersion: 1, jobId: id, jobRevision: 1, action: 'create-device', linkedJobId: null,
    platform: preview.platform, profileId: preview.profileId, runtimeId: preview.runtimeId,
    creationPreviewHash: preview.previewHash, phase: 'queued', state: 'queued', progress: 0,
    stages: [
      { id: 'detecting', label: 'Detecting environment', status: 'queued', startedAt: null, durationMs: null, message: null },
      { id: 'creating-device', label: 'Creating device', status: 'queued', startedAt: null, durationMs: null, message: null }
    ],
    startedAt: stamp, updatedAt: stamp, finishedAt: null, result: null, errorCode: null,
    processIdentities: [],
    idempotencyKeyHash: hash(body.idempotencyKey), requestHash: hash(appRunConfig.canonicalJson(body))
  };
}

function validateStart(body) {
  var keys = ['platform', 'targetId', 'discoveryRevision', 'variantId', 'buildMode',
    'taskStem', 'surfaceId', 'expectedProjectSourceRevision', 'confirmationToken',
    'whenBusy', 'idempotencyKey'];
  if (!exactKeys(body, keys) || ['android', 'ios'].indexOf(body.platform) < 0 ||
      !/^target-[a-f0-9]{32}$/.test(String(body.targetId || '')) ||
      !/^discovery-[a-f0-9]{36}$/.test(String(body.discoveryRevision || '')) ||
      !/^[a-z][a-z0-9-]{0,31}$/.test(String(body.variantId || '')) ||
      !BUILD_MODES[body.buildMode] || !safeStem(body.taskStem) || !safeSurface(body.surfaceId) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(body.expectedProjectSourceRevision || '')) ||
      (body.confirmationToken !== null && !/^confirm-[a-f0-9]{48}$/.test(String(body.confirmationToken))) ||
      ['fail', 'queue'].indexOf(body.whenBusy) < 0 || !safeIdempotency(body.idempotencyKey)) {
    return 'bad-app-run-request';
  }
  return null;
}

function idempotent(body) {
  var keyHash = hash(body.idempotencyKey), requestHash = hash(appRunConfig.canonicalJson(body));
  var previous = runtime.idempotency.get(keyHash);
  if (!previous) return null;
  if (previous.requestHash !== requestHash) return { ok: false, status: 409, error: 'idempotency-conflict' };
  var job = runtime.jobs.get(previous.jobId);
  return job ? { ok: true, status: 200, job: publicJob(job), idempotent: true } : null;
}

function idempotentMutation(scope, body, handler) {
  if (!body || !safeIdempotency(body.idempotencyKey)) return Promise.resolve().then(handler);
  var key = hash(scope + '\0' + body.idempotencyKey);
  var requestHash = hash(appRunConfig.canonicalJson(body));
  var previous = runtime.mutations.get(key);
  if (previous) {
    if (previous.requestHash !== requestHash) {
      return Promise.resolve({ ok: false, status: 409, error: 'idempotency-conflict' });
    }
    return previous.promise;
  }
  if (runtime.mutations.size >= 500) {
    Array.from(runtime.mutations.entries()).filter(function (entry) {
      return entry[1].settled;
    }).sort(function (a, b) {
      return a[1].createdMs - b[1].createdMs;
    }).forEach(function (entry) {
      if (runtime.mutations.size >= 500) runtime.mutations.delete(entry[0]);
    });
  }
  if (runtime.mutations.size >= 500) {
    return Promise.resolve({ ok: false, status: 503, error: 'app-run-capacity' });
  }
  var record = {
    requestHash: requestHash,
    promise: null,
    createdMs: Date.now(),
    settled: false
  };
  var promise = Promise.resolve().then(handler);
  record.promise = promise;
  runtime.mutations.set(key, record);
  promise.then(function () { record.settled = true; }, function () { record.settled = true; });
  return promise;
}

function putBoundedPreview(map, key, value) {
  var stamp = Date.now();
  map.forEach(function (row, rowKey) {
    if (!row || !Number.isFinite(row.expiresMs) || row.expiresMs <= stamp) map.delete(rowKey);
  });
  while (map.size >= 100) map.delete(map.keys().next().value);
  map.set(key, value);
}

function targetStableHint(snapshot, platform, targetId) {
  var p = snapshot.public.platforms.find(function (row) { return row.id === platform; });
  var row = p && p.devices.find(function (device) { return device.id === targetId; });
  return row && row.stableHint;
}
function discoveryOptions(extra) {
  return Object.assign({
    commandRunner: runtime.commandRunner,
    androidTools: runtime.androidTools || undefined,
    iosTools: runtime.iosTools || undefined,
    forceIos: runtime.forceIos
  }, extra || {});
}

function projectMutationBlocked() {
  try {
    return finalizations.mutationBlocked(null, {
      requireSoleWriter: true,
      key: 'app-run:status-probe'
    });
  } catch (_) {
    return true;
  }
}

// The gradle wrapper that belongs to the job's own product tree. A task-bound
// job must never drive the control root's wrapper against the candidate.
function jobGradleWrapper(job) {
  var root = job && job.executionRoot;
  if (!root) return null;
  var wrapper = path.join(root, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew');
  try { return processRunner.resolveExecutable(wrapper, [wrapper]); }
  catch (error) { return null; }
}

// §14: an explicit task-local build directory. Two checkouts that happen to
// share a run config must not share one DerivedData tree, or their products
// would overwrite each other while both claim to be built from their own
// candidate.
function derivedDataRoot(runConfigHash, worktreeId) {
  var scope = runConfigHash.replace(/^sha256:/, '').slice(0, 24);
  return worktreeId
    ? path.join(paths.APP_RUN_DIR, 'derived-data', scope + '-' + String(worktreeId).slice(-12))
    : path.join(paths.APP_RUN_DIR, 'derived-data', scope);
}

function allowedBuildRoot(platform, variant, runConfigHash, executionRoot, worktreeId) {
  if (platform === 'android') {
    return path.join(executionRoot, variant.module, 'build', 'outputs', 'apk', variant.id);
  }
  return path.join(derivedDataRoot(runConfigHash, worktreeId),
    'Build', 'Products', variant.configuration + '-iphonesimulator');
}

function artifactExpectation(body, config, target, variant, tools, execution) {
  var binding = execution && execution.binding;
  var executionRoot = binding ? binding.executionRoot : paths.PROJECT_ROOT;
  var worktreeId = binding ? binding.worktreeId : null;
  return {
    platform: body.platform,
    variantId: body.variantId,
    applicationId: config.project.applicationId,
    runConfigHash: config.runConfigHash,
    allowedBuildRoot: allowedBuildRoot(body.platform, variant, config.runConfigHash, executionRoot, worktreeId),
    executionRoot: executionRoot,
    worktreeId: worktreeId,
    executionRunId: binding ? binding.runId : null,
    candidateTree: execution ? execution.candidateTree : null,
    toolchainFingerprint: artifacts.toolchainFingerprint(body.platform, tools),
    targetArchitecture: target.architecture || null,
    runtimeKind: body.platform === 'ios' ? 'iphonesimulator' : null
  };
}

// A build root that does not exist yet is the normal state of a fresh
// checkout, not evidence that anything is corrupt.
function buildRootExists(root) {
  try { return fs.statSync(root).isDirectory(); }
  catch (error) { return false; }
}

function artifactIntegrityFailure(code) {
  return code === 'artifact-invalid' || code === 'artifact-hash-mismatch' ||
    code === 'artifact-path-mismatch';
}

function latchArtifactIntegrity(code, artifactId) {
  if (artifactIntegrityFailure(code)) {
    latchIntegrity('artifact-integrity-failed', artifactId || null);
  }
}

function olderBuildConfirmation(body, config, target, variant, tools, expectedExecution) {
  // The preflight and the run must agree about where the product is produced,
  // or a task-bound job would be preflighted against the control root's APK.
  // Same single decision the job itself uses; unprovable refuses here too.
  var execution = jobExecutionBinding(body.taskStem);
  if (!execution.ok) return { ok: false, status: 409, error: execution.error };
  var binding = execution.binding;
  var scope = executionScopeFor(body.taskStem, binding);
  if (!scope.ok) return { ok: false, status: 409, error: scope.error };
  if (expectedExecution !== undefined && !executionScopeMatches(scope,
      expectedExecution.binding ? expectedExecution.binding.worktreeId : null,
      expectedExecution.binding ? expectedExecution.binding.executionRoot : null,
      expectedExecution.binding ? expectedExecution.binding.runId : null,
      expectedExecution.candidateTree)) {
    return { ok: false, status: 409, error: 'execution-binding-unavailable' };
  }
  if (body.buildMode !== 'last-build') return { ok: true, artifactId: null };
  var manifest;
  try {
    manifest = artifacts.latest(body.platform, body.variantId);
  } catch (_) {
    latchArtifactIntegrity('artifact-invalid', null);
    return { ok: false, status: 409, error: 'artifact-invalid' };
  }
  if (!manifest) return { ok: false, status: 409, error: 'artifact-not-found' };
  var verified = artifacts.verify(manifest,
    artifactExpectation(body, config, target, variant, tools, scope));
  if (!verified.ok) {
    // A stored artifact built for a DIFFERENT tree is a stale reuse candidate,
    // not evidence that app-run's own state is corrupt: the job simply has to
    // rebuild. Latching integrity here would brick the panel for every task
    // whose last build came from another root.
    if (verified.error !== 'artifact-path-mismatch' &&
        verified.error !== 'artifact-scope-mismatch') {
      latchArtifactIntegrity(verified.error, manifest.artifactId);
    }
    return Object.assign({ status: 409 }, verified);
  }
  // An artifact is a fresh reuse candidate when it was built from the same
  // tree this request would build — the PRODUCT tree, which for a task-bound
  // request is its checkout, not whatever the client last saw.
  var productNow = source(true, binding ? binding.executionRoot : paths.PROJECT_ROOT);
  if (!productNow.available) {
    return { ok: false, status: 409, error: productNow.reasonCode, detail: productNow.detail };
  }
  if (manifest.appProjectSourceRevision === productNow.revision) {
    return { ok: true, artifactId: manifest.artifactId };
  }
  if (body.confirmationToken) {
    var confirmation = runtime.confirmations.get(body.confirmationToken);
    runtime.confirmations.delete(body.confirmationToken);
    if (confirmation && confirmation.expiresMs > Date.now() &&
        confirmation.artifactId === manifest.artifactId &&
        confirmation.artifactHash === manifest.artifactHash &&
        confirmation.artifactSourceRevision === manifest.appProjectSourceRevision &&
        confirmation.currentSourceRevision === body.expectedProjectSourceRevision &&
        confirmation.targetStableHint === target.stableHint &&
        confirmation.discoveryRevision === body.discoveryRevision &&
        confirmation.runConfigHash === config.runConfigHash) {
      return { ok: true, artifactId: manifest.artifactId };
    }
    return { ok: false, status: 409, error: 'confirmation-invalid' };
  }
  var token = 'confirm-' + crypto.randomBytes(24).toString('hex');
  putBoundedPreview(runtime.confirmations, token, {
    expiresMs: Date.now() + 5 * 60 * 1000,
    artifactId: manifest.artifactId, artifactHash: manifest.artifactHash,
    artifactSourceRevision: manifest.appProjectSourceRevision,
    currentSourceRevision: body.expectedProjectSourceRevision,
    targetStableHint: target.stableHint, discoveryRevision: body.discoveryRevision,
    runConfigHash: config.runConfigHash
  });
  return {
    ok: false, status: 409, error: 'artifact-stale', confirmationRequired: true,
    confirmation: {
      token: token,
      expiresAt: new Date(Date.now() + 5 * 60 * 1000).toISOString(),
      artifactId: manifest.artifactId,
      builtAt: manifest.builtAt,
      artifactSourceRevision: manifest.appProjectSourceRevision,
      currentSourceRevision: body.expectedProjectSourceRevision
    }
  };
}

function start(body, linkedJobId, preflight) {
  var issue = validateStart(body);
  if (issue) return { ok: false, status: 400, error: issue };
  var duplicate = idempotent(body);
  if (duplicate) return duplicate;
  if (runtime.integrityIssues.length) {
    return { ok: false, status: 409, error: 'runtime-recovery-required' };
  }
  if (runtime.activeJobId) {
    var active = runtime.jobs.get(runtime.activeJobId);
    if (active && ACTIVE_STATES[active.state]) return { ok: false, status: 409, error: 'app-run-active', active: publicJob(active) };
  }
  var activeSession = runtime.currentSessionId && runtime.sessions.get(runtime.currentSessionId);
  if (activeSession && (activeSession.state === 'running' || activeSession.state === 'stopping')) {
    return { ok: false, status: 409, error: 'app-run-active', session: publicSession(activeSession) };
  }
  var resolved;
  try {
    resolved = discovery.resolveTarget(body.platform, body.targetId, body.discoveryRevision, discoveryOptions());
  } catch (_) {
    return { ok: false, status: 409, error: 'discovery-failed' };
  }
  if (!resolved.ok) return { ok: false, status: 409, error: resolved.error };
  var stableHint = targetStableHint(resolved.snapshot, body.platform, body.targetId);
  resolved.target.stableHint = stableHint;
  var config = resolved.snapshot.config;
  var variant = appRunConfig.resolveVariant(config, body.platform, body.variantId);
  if (!variant) return { ok: false, status: 400, error: 'variant-not-found' };
  // The client's optimistic-concurrency token. It is the CONTROL root's
  // revision because that is what device discovery showed the panel; it says
  // "the project I saw is still the project you are about to act on". It is
  // deliberately NOT the tree the build compiles — see productSourceRevision.
  var current = source(true);
  if (!current.available) return { ok: false, status: 409, error: current.reasonCode, detail: current.detail };
  if (current.revision !== body.expectedProjectSourceRevision) {
    return { ok: false, status: 409, error: 'source-changed', projectSourceRevision: current.revision };
  }
  var taskContext = null;
  if (body.taskStem) {
    taskContext = validation.checklist(body.taskStem);
    if (!taskContext.ok) return { ok: false, status: 409, error: 'task-context-unavailable' };
  }
  // Decide and pin the job's exact generation BEFORE artifact preflight. A
  // preflight against another generation is not reusable authority.
  var executionBinding = jobExecutionBinding(body.taskStem);
  if (!executionBinding.ok) {
    return { ok: false, status: 409, error: executionBinding.error };
  }
  var execution = executionScopeFor(body.taskStem, executionBinding.binding);
  if (!execution.ok) return { ok: false, status: 409, error: execution.error };
  var confirmed = preflight && preflight.validated === true
    ? { ok: true, artifactId: preflight.artifactId || null }
    : olderBuildConfirmation(body, config, resolved.target, variant, resolved.tools, execution);
  if (!confirmed.ok) return confirmed;
  if (body.whenBusy === 'fail' && projectMutationBlocked()) {
    return { ok: false, status: 409, error: 'project-busy' };
  }
  if (preflight && Object.prototype.hasOwnProperty.call(preflight, 'executionBinding') &&
      !executionScopeMatches(execution,
        preflight.executionBinding.binding ? preflight.executionBinding.binding.worktreeId : null,
        preflight.executionBinding.binding ? preflight.executionBinding.binding.executionRoot : null,
        preflight.executionBinding.binding ? preflight.executionBinding.binding.runId : null,
        preflight.executionBinding.candidateTree)) {
    return { ok: false, status: 409, error: 'execution-binding-unavailable' };
  }
  var productSource = source(true,
    execution.binding ? execution.binding.executionRoot : paths.PROJECT_ROOT);
  if (!productSource.available) {
    return { ok: false, status: 409, error: productSource.reasonCode, detail: productSource.detail };
  }
  var job = newRunJob(body, config, resolved.target, linkedJobId, confirmed.artifactId,
    execution, productSource.revision);
  job.taskContextRevision = taskContext ? taskContext.taskSourceRevision : null;
  runtime.jobs.set(job.jobId, job);
  runtime.activeJobId = job.jobId;
  runtime.idempotency.set(job.idempotencyKeyHash, { jobId: job.jobId, requestHash: job.requestHash });
  persistJob(job);
  executeRun(job).catch(function (error) {
    try {
      if (!TERMINAL[job.state]) {
        finishJob(job, 'failed', null, error && error.code || 'internal');
      }
    } catch (_) {
      latchIntegrity('job-storage-unavailable', job.jobId);
    }
  });
  return { ok: true, status: 202, job: publicJob(job) };
}

function freshTarget(job) {
  var snapshot;
  try { snapshot = discovery.discover(discoveryOptions({ refresh: true })); }
  catch (_) { return { ok: false, error: 'discovery-failed' }; }
  var platform = snapshot.public.platforms.find(function (row) { return row.id === job.platform; });
  if (!platform || platform.availability === 'unavailable') {
    return {
      ok: false,
      error: platform && platform.reasonCode || 'discovery-failed'
    };
  }
  var publicTarget = platform && platform.devices.find(function (row) { return row.stableHint === job.targetStableHint; });
  if (!publicTarget) return { ok: false, error: 'target-not-found' };
  var privateTarget = snapshot.private[job.platform].devices[publicTarget.id];
  if (!privateTarget) return { ok: false, error: 'target-not-found' };
  job.targetId = publicTarget.id;
  job.rawTargetIdentifier = privateTarget.rawIdentifier;
  job.deviceSummary = publicTarget.displayName + (publicTarget.osVersion ? ' · ' + publicTarget.osVersion : '');
  return {
    ok: true, target: privateTarget, tools: snapshot.private[job.platform].tools,
    config: snapshot.config, snapshot: snapshot
  };
}

// §15/§16. A job takes exactly the leases its effects justify:
//   - the build lease, scoped to the tree it builds. A task-bound job builds in
//     its own isolated checkout, so it names that worktree and stays compatible
//     with a job in another one; a control-root job still demands sole-writer,
//     because it mutates the tree everything else shares.
//   - the DEVICE, because boot/install/launch/screenshot mutate device state
//     that no worktree isolates.
//   - the BUNDLE id, because two jobs installing the same application id on the
//     same device would overwrite each other's install regardless of platform
//     bookkeeping. The applicationId suffix is never altered for concurrency —
//     that would change the behaviour under test.
// Every one of these carries its identity in `key` and no stem, so conflicts are
// exact-resource conflicts and nothing else.
// The exclusion strength matches the isolation. A CONTROL-ROOT job builds the
// tree everything else shares, so it keeps its single globally exclusive
// runtime-build lease — which already excludes every device and every bundle,
// because while it is held nothing else may run at all. A TASK-BOUND job builds
// in its own checkout, so global exclusion would be exactly the blanket
// serialization §16 forbids: it takes three narrow leases instead, and only the
// device and the bundle can make two such jobs collide.
function leaseRequests(job) {
  if (!job.executionRoot || !job.worktreeId) {
    return [{ kind: 'runtime-build', key: 'app-run:runtime-build', requireSoleWriter: true }];
  }
  var requests = [{ kind: 'execution-writer', key: 'execution:' + job.worktreeId, requireSoleWriter: false }];
  if (job.targetStableHint) {
    requests.push({ kind: 'resource-writer', key: 'device:' + job.platform + ':' + job.targetStableHint,
      requireSoleWriter: false });
  }
  if (job.applicationId) {
    requests.push({ kind: 'resource-writer', key: 'bundle:' + job.platform + ':' + job.applicationId,
      requireSoleWriter: false });
  }
  return requests;
}

function acquireLease(job) {
  var handles = [];
  var requests = leaseRequests(job);
  for (var i = 0; i < requests.length; i++) {
    var admission;
    try {
      admission = finalizations.beginMutation({
        kind: requests[i].kind, stem: null, sessionId: finalizations.createWriterSessionId(),
        key: requests[i].key, pendingChild: false,
        requireSoleWriter: requests[i].requireSoleWriter, ttlMs: LEASE_TTL_MS
      });
    } catch (_) {
      releaseLeases(handles);
      return { ok: false, error: 'writer-lease-unavailable' };
    }
    if (!admission.ok) {
      // Partial acquisition is never left standing: a refused resource means
      // the job does not run, so every lease it already took comes back.
      releaseLeases(handles);
      return admission;
    }
    handles.push(admission.handle);
  }
  // The FIRST handle stays the build lease every existing call site drives
  // (pending-child fence, child pid binding, renewal); the rest are exclusions.
  return { ok: true, handle: handles[0], handles: handles };
}

function releaseLeases(handles) {
  (handles || []).forEach(function (handle) {
    try { finalizations.endMutation(handle); } catch (_) {}
  });
}

function phaseContext(job, resolved, controller, leaseState, lease) {
  function leaseFailure(message) {
    leaseState.healthy = false;
    try { controller.abort(); } catch (_) {}
    var error = new Error(message);
    error.code = 'writer-lease-lost';
    return error;
  }
  return {
    tools: resolved.tools,
    commandRunner: runtime.commandRunner,
    signal: controller.signal,
    // Where the product under test lives for THIS job (plan §14/§15). Absent an
    // execution binding the job is a control-root run and the control root is
    // the product.
    executionRoot: jobProductRoot(job),
    worktreeId: job.worktreeId || null,
    gradlew: jobGradleWrapper(job),
    applicationId: resolved.config.project.applicationId,
    runConfigHash: resolved.config.runConfigHash,
    targetArchitecture: resolved.target.architecture || null,
    onLine: function (stream, line) { appendLog(job, stream, job.phase, line); },
    beforeBuildSpawn: function () {
      if (controller.signal.aborted) {
        var cancelled = new Error('app-run operation was cancelled');
        cancelled.code = 'cancelled';
        throw cancelled;
      }
      if (!leaseState.healthy ||
          !finalizations.retainMutation(lease, writerLeases.PENDING_CHILD_REASON)) {
        throw leaseFailure('runtime build lease could not publish its pending child fence');
      }
    },
    onBuildSpawn: function (identity) {
      try {
        writerLeases.updateChildPid(lease, identity.pid);
      } catch (_) {
        throw leaseFailure('runtime build process could not be bound to its writer lease');
      }
    },
    onProcess: function (kind, identity) {
      var record = identity && {
        kind: String(kind || ''),
        pid: identity.pid,
        processStartId: String(identity.processStartId || '')
      };
      if (!safeProcessIdentity(record) || job.processIdentities.length >= 4) {
        var invalidIdentity = new Error('Detached emulator identity is invalid');
        invalidIdentity.code = 'operation-failed';
        throw invalidIdentity;
      }
      job.processIdentities.push({
        kind: record.kind,
        pid: record.pid,
        processStartId: record.processStartId
      });
      persistJob(job);
      appendLog(job, 'stdout', job.phase, kind + ' process started (identity captured)');
    },
    ensureLease: function () {
      if (controller.signal.aborted) {
        var cancelled = new Error('app-run operation was cancelled');
        cancelled.code = 'cancelled';
        throw cancelled;
      }
      if (!leaseState.healthy) {
        var error = new Error('runtime build lease ownership was lost'); error.code = 'writer-lease-lost'; throw error;
      }
    }
  };
}

// Every lease the job holds is renewed. Renewing only the build lease would let
// the device and bundle exclusions expire under a long build, so a second job
// could claim the same device while this one is still installing on it.
function beginRenewal(handles, controller, leaseState) {
  var list = Array.isArray(handles) ? handles : [handles];
  return setInterval(function () {
    try {
      list.forEach(function (handle) { writerLeases.renew(handle, LEASE_TTL_MS); });
    } catch (_) {
      leaseState.healthy = false;
      try { controller.abort(); } catch (_) {}
    }
  }, LEASE_RENEW_MS);
}

function artifactFromManifest(manifest, verified) {
  return {
    path: verified.path, hash: manifest.artifactHash, size: manifest.artifactSize,
    applicationId: manifest.applicationId, targetArchitectures: manifest.targetArchitectures
  };
}

async function waitForAdmission(job) {
  while (true) {
    if (job.state === 'cancelled') return null;
    var lease = acquireLease(job);
    if (lease.ok) {
      if (!jobExecutionBindingCurrent(job)) {
        releaseLeases(lease.handles);
        var staleBinding = new Error('Task execution binding changed before build admission');
        staleBinding.code = 'execution-binding-unavailable';
        throw staleBinding;
      }
      return { primary: lease.handle, all: lease.handles };
    }
    if (lease.error !== 'finalization-active') {
      var unavailable = new Error('Project writer lease is unavailable');
      unavailable.code = lease.error === 'writer-lease-release-failed'
        ? 'writer-lease-release-failed' : 'writer-lease-unavailable';
      throw unavailable;
    }
    if (job.whenBusy !== 'queue') {
      var busy = new Error('Project is being updated'); busy.code = 'project-busy'; throw busy;
    }
    if (job.state !== 'waiting-for-project') {
      setStage(job, 'waiting-for-project', 'running', 'Project is being updated; run is queued.');
    }
    await new Promise(function (resolve) {
      var controller = runtime.controllers.get(job.jobId);
      var signal = controller && controller.signal;
      if (signal && signal.aborted) { resolve(); return; }
      var timer = setTimeout(done, 1500);
      function done() {
        clearTimeout(timer);
        if (signal) signal.removeEventListener('abort', done);
        resolve();
      }
      if (signal) signal.addEventListener('abort', done, { once: true });
    });
  }
}

async function executeRun(job) {
  var leaseHandles = [];
  var controller = new AbortController();
  runtime.controllers.set(job.jobId, controller);
  var lease = null, renewal = null;
  try {
    setStage(job, 'detecting', 'running', 'Refreshing toolchains and target state.');
    var resolved = freshTarget(job);
    if (!resolved.ok) { var missing = new Error('Selected target is no longer available'); missing.code = resolved.error; throw missing; }
    var variant = appRunConfig.resolveVariant(resolved.config, job.platform, job.variantId);
    if (!variant) { var invalid = new Error('Selected variant is no longer configured'); invalid.code = 'variant-not-found'; throw invalid; }
    setStage(job, 'detecting', 'success', 'Environment and target resolved.');
    var admission = await waitForAdmission(job);
    if (!admission) return;
    lease = admission.primary;
    leaseHandles = admission.all;
    var leaseState = { healthy: true };
    renewal = beginRenewal(leaseHandles && leaseHandles.length ? leaseHandles : [lease], controller, leaseState);
    var current = source(true, jobProductRoot(job));
    if (!current.available || current.revision !== job.productSourceRevision) {
      var drift = new Error('Project changed before build started');
      drift.code = current.available ? 'source-changed' : current.reasonCode;
      throw drift;
    }
    if (job.taskStem) {
      var currentTask = validation.checklist(job.taskStem);
      if (!currentTask.ok || currentTask.taskSourceRevision !== job.taskContextRevision) {
        var taskDrift = new Error('Task context changed while the run was queued');
        taskDrift.code = 'task-context-changed';
        throw taskDrift;
      }
    }
    var context = phaseContext(job, resolved, controller, leaseState, lease);
    if (job.stages[1].status === 'running') setStage(job, 'waiting-for-project', 'success', 'Project writer lease acquired.');
    else setStage(job, 'waiting-for-project', 'skipped', 'Project was ready.');

    setStage(job, 'starting-device', 'running', 'Starting the selected virtual device.');
    var platformRunner = job.platform === 'android' ? android : ios;
    var rawTarget = await platformRunner.ensureStarted(context, resolved.target);
    context.ensureLease();
    job.rawTargetIdentifier = rawTarget;
    setStage(job, 'starting-device', 'success', 'Virtual device is ready.');

    var manifest = null, artifact = null, built = false, derived = null;
    var reuseFailure = null;
    try {
      if (job.confirmedArtifactId) manifest = artifacts.read(job.confirmedArtifactId);
      else if (job.buildMode !== 'rebuild') manifest = artifacts.latest(job.platform, job.variantId);
    } catch (_) {
      latchArtifactIntegrity('artifact-invalid', job.confirmedArtifactId);
      var unreadableArtifact = new Error('Stored application artifact metadata is invalid');
      unreadableArtifact.code = 'artifact-invalid';
      throw unreadableArtifact;
    }
    if (manifest) {
      var verified = artifacts.verify(manifest, artifactExpectation({
        platform: job.platform, variantId: job.variantId
      }, resolved.config, resolved.target, variant, resolved.tools,
      storedExecutionScope(job)));
      var sourceMatches = manifest.appProjectSourceRevision === job.productSourceRevision;
      var allowedOlder = job.confirmedArtifactId === manifest.artifactId;
      if (verified.ok && (sourceMatches || allowedOlder)) artifact = artifactFromManifest(manifest, verified);
      else {
        reuseFailure = verified.ok ? 'artifact-config-mismatch' : verified.error;
        // A cached artifact that belongs to ANOTHER tree — or whose build root
        // does not exist yet in a fresh checkout — is a stale reuse candidate.
        // The job simply rebuilds; treating it as tampering would latch a
        // permanent integrity failure the first time a task-bound run met an
        // artifact from an earlier control-root build.
        if (job.executionRoot && (reuseFailure === 'artifact-path-mismatch' ||
            reuseFailure === 'artifact-scope-mismatch' ||
            (reuseFailure === 'artifact-invalid' && !buildRootExists(
              allowedBuildRoot(job.platform, variant, resolved.config.runConfigHash,
                job.executionRoot, job.worktreeId))))) {
          reuseFailure = 'artifact-config-mismatch';
        }
        if (artifactIntegrityFailure(reuseFailure)) {
          latchArtifactIntegrity(reuseFailure, manifest.artifactId);
          var unsafeStoredArtifact = new Error('Stored application artifact failed its integrity proof');
          unsafeStoredArtifact.code = reuseFailure;
          throw unsafeStoredArtifact;
        }
      }
    }
    if (job.buildMode === 'last-build' && !artifact) {
      var rejectedStoredArtifact = new Error('The selected stored application artifact is no longer valid');
      rejectedStoredArtifact.code = reuseFailure || 'artifact-not-found';
      throw rejectedStoredArtifact;
    }
    if (job.buildMode === 'rebuild' || !artifact) {
      setStage(job, 'building', 'running', 'Building current app sources.');
      if (!resolved.tools.gradlew && job.platform === 'android') {
        var noGradle = new Error('Gradle wrapper is unavailable'); noGradle.code = 'android-sdk-missing'; throw noGradle;
      }
      if (job.platform === 'android') await android.build(context, variant, job.buildMode === 'rebuild');
      else derived = await ios.build(context, variant);
      context.ensureLease();
      var postBuild = source(true, jobProductRoot(job));
      if (!postBuild.available || postBuild.revision !== job.productSourceRevision) {
        var buildDrift = new Error('Project inputs changed during build');
        buildDrift.code = postBuild.available ? 'source-changed' : postBuild.reasonCode;
        throw buildDrift;
      }
      if (!jobExecutionBindingCurrent(job)) {
        var executionDrift = new Error('Execution candidate changed during build');
        executionDrift.code = 'execution-binding-unavailable';
        throw executionDrift;
      }
      built = true;
      setStage(job, 'building', 'success', 'Build completed.');
      setStage(job, 'resolving-artifact', 'running', 'Validating the built application.');
      artifact = job.platform === 'android'
        ? android.resolveArtifact(context, variant) : ios.resolveArtifact(context, variant, derived);
      try {
        manifest = artifacts.create({
          platform: job.platform, variantId: job.variantId, sourceRevision: job.productSourceRevision,
          runConfigHash: resolved.config.runConfigHash,
          execution: job.worktreeId === null ? null : {
            worktreeId: job.worktreeId, runId: job.executionRunId,
            executionRoot: job.executionRoot, candidateTree: job.candidateTree
          },
          artifact: artifact, tools: resolved.tools
        });
      } catch (artifactPublishError) {
        latchIntegrity('artifact-publish-failed', null);
        artifactPublishError.code = artifactPublishError.code || 'artifact-invalid';
        throw artifactPublishError;
      }
      setStage(job, 'resolving-artifact', 'success', 'Artifact identity and hash verified.');
    } else {
      setStage(job, 'building', 'skipped', 'Using a verified compatible build.');
      setStage(job, 'resolving-artifact', 'running', 'Revalidating the stored application artifact.');
      setStage(job, 'resolving-artifact', 'success', 'Stored artifact hash and configuration verified.');
    }
    context.ensureLease();
    job.artifactId = manifest.artifactId;
    job.appProjectSourceRevision = manifest.appProjectSourceRevision;
    persistJob(job);

    var installVerification = artifacts.verify(manifest, artifactExpectation({
      platform: job.platform, variantId: job.variantId
    }, resolved.config, resolved.target, variant, resolved.tools,
    storedExecutionScope(job)));
    if (!installVerification.ok) {
      latchArtifactIntegrity(installVerification.error, manifest.artifactId);
      var artifactChanged = new Error('Application artifact changed before installation');
      artifactChanged.code = installVerification.error || 'artifact-invalid';
      throw artifactChanged;
    }
    artifact = artifactFromManifest(manifest, installVerification);
    setStage(job, 'installing', 'running', 'Installing application on the selected device.');
    await platformRunner.install(context, rawTarget, artifact);
    context.ensureLease();
    setStage(job, 'installing', 'success', 'Application installed.');
    setStage(job, 'launching', 'running', 'Launching and verifying the application.');
    var launch = await platformRunner.launch(context, rawTarget);
    context.ensureLease();
    setStage(job, 'launching', 'success', 'Application is running.');

    var sessionId = storage.randomId('session'), stamp = now();
    var session = {
      schemaVersion: 1, sessionId: sessionId, sessionRevision: 1, jobId: job.jobId,
      state: 'running', platform: job.platform, targetId: job.targetId,
      targetStableHint: job.targetStableHint, rawTargetIdentifier: rawTarget,
      deviceSummary: job.deviceSummary, variantId: job.variantId,
      worktreeId: job.worktreeId, executionRoot: job.executionRoot,
      executionRunId: job.executionRunId, candidateTree: job.candidateTree,
      artifactId: manifest.artifactId, applicationId: resolved.config.project.applicationId,
      requestedProjectSourceRevision: job.requestedProjectSourceRevision,
      appProjectSourceRevision: manifest.appProjectSourceRevision,
      runConfigHash: resolved.config.runConfigHash, launchedAt: stamp, updatedAt: stamp,
      taskStem: job.taskStem, surfaceId: job.surfaceId
    };
    runtime.sessions.set(sessionId, session);
    runtime.currentSessionId = sessionId;
    persistSession(session);
    pruneSessions();
    updateSourceWatcher();
    job.sessionId = sessionId;
    job.state = 'running'; job.phase = 'running'; job.progress = 100;
    job.result = { launched: true, rebuilt: built, pidObserved: launch.pid !== null };
    job.jobRevision++; job.updatedAt = now();
    runtime.activeJobId = null;
    assertTransition(job, 'launching', 'running');
    assertJobInvariant(job);
    persistJob(job);
    if (runtime.persistPreference) {
      try {
        await Promise.resolve(runtime.persistPreference({
          platform: job.platform, targetStableHint: job.targetStableHint,
          variantId: job.variantId, buildMode: job.buildMode
        }));
      } catch (preferenceError) {
        appendLog(job, 'stderr', 'running', 'Run preference could not be persisted: ' +
          String(preferenceError && preferenceError.message || preferenceError));
      }
    }
    flushLogs(job.jobId);
    notify('app-run-status', { jobId: job.jobId, sessionId: sessionId });
  } catch (error) {
    if (job.state !== 'cancelled') {
      appendLog(job, 'stderr', job.phase, error && error.message || error);
      var failureCode = leaseState && !leaseState.healthy
        ? 'writer-lease-lost' : error && error.code || 'internal';
      finishJob(job, controller.signal.aborted ? 'interrupted' : 'failed', null,
        failureCode);
    }
  } finally {
    runtime.controllers.delete(job.jobId);
    if (renewal) clearInterval(renewal);
    // Resource exclusions come off AFTER the process tree is gone (§15): the
    // teardown above has already aborted the controller and settled the child.
    releaseLeases((leaseHandles || []).filter(function (handle) { return handle !== lease; }));
    if (lease) {
      try {
        if (writerLeases.release(lease) !== true) {
          throw new Error('runtime build writer lease release was not committed');
        }
      }
      catch (error) {
        latchIntegrity('writer-lease-release-failed', job.jobId);
        if (job.state === 'running') {
          try { updateJob(job, { errorCode: 'writer-lease-release-failed' }); }
          catch (_) {
            // persistJob already latched storage failure; keep the real running
            // session in memory instead of misclassifying it as a failed app.
          }
        }
      }
    }
  }
}

function cancel(body) {
  if (!exactKeys(body, ['jobId', 'expectedStateRevision', 'idempotencyKey']) ||
      !/^job-[a-f0-9]{36}$/.test(String(body.jobId || '')) ||
      !Number.isSafeInteger(body.expectedStateRevision) || body.expectedStateRevision < 1 ||
      !safeIdempotency(body.idempotencyKey)) return { ok: false, status: 400, error: 'bad-cancel-request' };
  var job = runtime.jobs.get(body.jobId);
  if (!job) return { ok: false, status: 404, error: 'job-not-found' };
  if (job.jobRevision !== body.expectedStateRevision) return { ok: false, status: 409, error: 'state-conflict', job: publicJob(job) };
  if (!CANCELLABLE[job.state]) return { ok: false, status: 409, error: 'job-not-cancellable' };
  var controller = runtime.controllers.get(job.jobId);
  if (controller) controller.abort();
  var activeStage = job.stages.find(function (row) { return row.status === 'running'; });
  if (activeStage) {
    activeStage.status = 'skipped';
    activeStage.durationMs = activeStage.startedAt
      ? Math.max(0, Date.now() - Date.parse(activeStage.startedAt)) : 0;
    activeStage.message = 'Cancelled before completion.';
  }
  finishJob(job, 'cancelled', null, null);
  return { ok: true, status: 200, job: publicJob(job) };
}

function resolveSession(sessionId, expectedRevision, runningRequired) {
  var session = runtime.sessions.get(sessionId);
  if (!session) return { ok: false, status: 404, error: 'session-not-found' };
  if (session.sessionRevision !== expectedRevision) return { ok: false, status: 409, error: 'state-conflict', session: publicSession(session) };
  if (runningRequired !== false && session.state !== 'running') return { ok: false, status: 409, error: 'session-not-running' };
  return { ok: true, session: session };
}

function toolsForPlatform(platform) {
  if (platform === 'android') return runtime.androidTools || android.resolveTools();
  return runtime.iosTools || ios.resolveTools();
}

async function stop(body) {
  if (!exactKeys(body, ['sessionId', 'expectedSessionRevision', 'idempotencyKey']) ||
      !/^session-[a-f0-9]{36}$/.test(String(body.sessionId || '')) ||
      !Number.isSafeInteger(body.expectedSessionRevision) || body.expectedSessionRevision < 1 ||
      !safeIdempotency(body.idempotencyKey)) return { ok: false, status: 400, error: 'bad-stop-request' };
  var found = resolveSession(body.sessionId, body.expectedSessionRevision);
  if (!found.ok) return found;
  var session = found.session;
  session.state = 'stopping'; session.sessionRevision++; session.updatedAt = now(); persistSession(session);
  notify('app-run-status', { sessionId: session.sessionId, revision: session.sessionRevision });
  var runner = session.platform === 'android' ? android : ios;
  var controller = new AbortController();
  var job = runtime.jobs.get(session.jobId);
  if (job && job.state === 'running') updateJob(job, { state: 'stopping', phase: 'stopping' });
  var result;
  try {
    result = await runner.stop({
      tools: toolsForPlatform(session.platform), commandRunner: runtime.commandRunner,
      signal: controller.signal, applicationId: session.applicationId, onLine: function () {}
    }, session.rawTargetIdentifier);
  } catch (_) {
    result = { ok: false, errorCode: 'invocation-invalid' };
  }
  session.state = result && result.ok ? 'stopped' : 'lost';
  session.sessionRevision++; session.updatedAt = now(); persistSession(session);
  if (runtime.currentSessionId === session.sessionId) runtime.currentSessionId = null;
  updateSourceWatcher();
  persistIndex();
  if (job && job.state === 'stopping') finishJob(job, 'completed', Object.assign({}, job.result, { stopped: session.state === 'stopped' }), null);
  pruneSessions();
  notify('app-run-status', { sessionId: session.sessionId, revision: session.sessionRevision });
  return { ok: result && result.ok, status: result && result.ok ? 200 : 409,
    error: result && result.ok ? null : 'stop-failed', session: publicSession(session) };
}

async function restart(body) {
  var keys = ['sessionId', 'expectedSessionRevision', 'buildMode', 'discoveryRevision',
    'expectedProjectSourceRevision', 'confirmationToken', 'idempotencyKey'];
  if (!exactKeys(body, keys) ||
      !/^session-[a-f0-9]{36}$/.test(String(body.sessionId || '')) ||
      !Number.isSafeInteger(body.expectedSessionRevision) || body.expectedSessionRevision < 1 ||
      !BUILD_MODES[body.buildMode] ||
      !/^discovery-[a-f0-9]{36}$/.test(String(body.discoveryRevision || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(body.expectedProjectSourceRevision || '')) ||
      (body.confirmationToken !== null && !/^confirm-[a-f0-9]{48}$/.test(String(body.confirmationToken))) ||
      !safeIdempotency(body.idempotencyKey)) return { ok: false, status: 400, error: 'bad-restart-request' };
  if (runtime.integrityIssues.length) {
    return { ok: false, status: 409, error: 'runtime-recovery-required' };
  }
  var found = resolveSession(body.sessionId, body.expectedSessionRevision);
  if (!found.ok) return found;
  var session = found.session;
  var exactExecution = sessionExecutionBinding(session);
  if (!exactExecution.ok) {
    return { ok: false, status: 409, error: exactExecution.error };
  }
  var targets;
  try { targets = discovery.discover(discoveryOptions()); }
  catch (_) { return { ok: false, status: 409, error: 'discovery-failed' }; }
  if (targets.discoveryRevision !== body.discoveryRevision) return { ok: false, status: 409, error: 'stale-discovery' };
  var platform = targets.public.platforms.find(function (row) { return row.id === session.platform; });
  if (!platform || platform.availability === 'unavailable') {
    return {
      ok: false,
      status: 409,
      error: platform && platform.reasonCode || 'discovery-failed'
    };
  }
  var target = platform && platform.devices.find(function (row) { return row.stableHint === session.targetStableHint; });
  if (!target) return { ok: false, status: 409, error: 'target-not-found' };
  var privateTarget = targets.private[session.platform] && targets.private[session.platform].devices[target.id];
  var config = targets.config;
  var variant = appRunConfig.resolveVariant(config, session.platform, session.variantId);
  if (!privateTarget || !variant) {
    return { ok: false, status: 409, error: privateTarget ? 'variant-not-found' : 'target-not-found' };
  }
  var currentControl = source(true);
  if (!currentControl.available) {
    return { ok: false, status: 409, error: currentControl.reasonCode, detail: currentControl.detail };
  }
  if (currentControl.revision !== body.expectedProjectSourceRevision) {
    return { ok: false, status: 409, error: 'source-changed', projectSourceRevision: currentControl.revision };
  }
  var currentProduct = source(true, exactExecution.binding
    ? exactExecution.binding.executionRoot : paths.PROJECT_ROOT);
  if (!currentProduct.available || currentProduct.revision !== session.appProjectSourceRevision) {
    return { ok: false, status: 409, error: currentProduct.available
      ? 'source-changed' : currentProduct.reasonCode, detail: currentProduct.detail };
  }
  var confirmed = olderBuildConfirmation({
    platform: session.platform,
    variantId: session.variantId,
    buildMode: body.buildMode,
    taskStem: session.taskStem,
    expectedProjectSourceRevision: body.expectedProjectSourceRevision,
    confirmationToken: body.confirmationToken,
    discoveryRevision: body.discoveryRevision
  }, config, privateTarget, variant, targets.private[session.platform].tools,
  exactExecution);
  if (!confirmed.ok) return confirmed;

  var stopped = await stop({
    sessionId: session.sessionId, expectedSessionRevision: session.sessionRevision,
    idempotencyKey: 'restart-stop-' + hash(body.idempotencyKey).slice('sha256:'.length)
  });
  if (!stopped.ok) return stopped;

  var refreshed;
  try { refreshed = discovery.discover(discoveryOptions({ refresh: true })); }
  catch (_) { return { ok: false, status: 409, error: 'discovery-failed' }; }
  var refreshedExecution = sessionExecutionBinding(session);
  if (!refreshedExecution.ok) {
    return { ok: false, status: 409, error: refreshedExecution.error };
  }
  var refreshedSource = source(true, refreshedExecution.binding
    ? refreshedExecution.binding.executionRoot : paths.PROJECT_ROOT);
  if (!refreshedSource.available) {
    return { ok: false, status: 409, error: refreshedSource.reasonCode, detail: refreshedSource.detail };
  }
  var refreshedControl = source(true);
  if (!refreshedControl.available ||
      refreshedControl.revision !== body.expectedProjectSourceRevision ||
      refreshedSource.revision !== session.appProjectSourceRevision ||
      !refreshed.config.ok || refreshed.config.runConfigHash !== config.runConfigHash) {
    return {
      ok: false, status: 409, error: 'source-changed',
      projectSourceRevision: refreshedControl.revision
    };
  }
  var refreshedPlatform = refreshed.public.platforms.find(function (row) {
    return row.id === session.platform;
  });
  if (!refreshedPlatform || refreshedPlatform.availability === 'unavailable') {
    return {
      ok: false,
      status: 409,
      error: refreshedPlatform && refreshedPlatform.reasonCode || 'discovery-failed'
    };
  }
  var refreshedTarget = refreshedPlatform && refreshedPlatform.devices.find(function (row) {
    return row.stableHint === session.targetStableHint;
  });
  if (!refreshedTarget) return { ok: false, status: 409, error: 'target-not-found' };
  if (!appRunConfig.resolveVariant(refreshed.config, session.platform, session.variantId)) {
    return { ok: false, status: 409, error: 'variant-not-found' };
  }
  return start({
    platform: session.platform, targetId: refreshedTarget.id,
    discoveryRevision: refreshed.discoveryRevision,
    variantId: session.variantId, buildMode: body.buildMode, taskStem: session.taskStem,
    surfaceId: session.surfaceId, expectedProjectSourceRevision: body.expectedProjectSourceRevision,
    confirmationToken: null, whenBusy: 'queue',
    idempotencyKey: body.idempotencyKey
  }, session.jobId, { validated: true, artifactId: confirmed.artifactId,
    executionBinding: refreshedExecution });
}

function pngInfo(bytes) {
  if (!Buffer.isBuffer(bytes) || bytes.length < 24 || bytes.length > 25 * 1024 * 1024 ||
      !bytes.slice(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])) ||
      bytes.toString('ascii', 12, 16) !== 'IHDR') return null;
  var width = bytes.readUInt32BE(16), height = bytes.readUInt32BE(20);
  if (!width || !height || width > 16384 || height > 16384) return null;
  return { width: width, height: height };
}

async function screenshot(body) {
  var keys = ['sessionId', 'expectedSessionRevision', 'taskStem', 'surfaceId', 'idempotencyKey'];
  if (!exactKeys(body, keys) || !safeStem(body.taskStem) || !safeSurface(body.surfaceId) ||
      !/^session-[a-f0-9]{36}$/.test(String(body.sessionId || '')) ||
      !Number.isSafeInteger(body.expectedSessionRevision) || body.expectedSessionRevision < 1 ||
      !safeIdempotency(body.idempotencyKey)) return { ok: false, status: 400, error: 'bad-screenshot-request' };
  if (runtime.integrityIssues.length) {
    return { ok: false, status: 409, error: 'runtime-recovery-required' };
  }
  var found = resolveSession(body.sessionId, body.expectedSessionRevision);
  if (!found.ok) return found;
  var session = found.session;
  if (body.taskStem !== null && body.taskStem !== session.taskStem) return { ok: false, status: 400, error: 'context-mismatch' };
  if (body.surfaceId !== null && body.surfaceId !== session.surfaceId) return { ok: false, status: 400, error: 'context-mismatch' };
  var sessionScreenshotCount = 0;
  try {
    storage.list(paths.APP_RUN_SCREENSHOTS_DIR, 'shot').forEach(function (id) {
      var existing = screenshotFile(id);
      if (existing && existing.metadata.sessionId === session.sessionId) {
        sessionScreenshotCount++;
      }
    });
  } catch (_) {
    latchIntegrity('screenshot-storage-unavailable', session.sessionId);
  }
  if (runtime.integrityIssues.length) {
    return { ok: false, status: 409, error: 'runtime-recovery-required' };
  }
  if (sessionScreenshotCount >= 50) {
    return { ok: false, status: 409, error: 'screenshot-capacity' };
  }
  var runner = session.platform === 'android' ? android : ios;
  var context = {
    tools: toolsForPlatform(session.platform), commandRunner: runtime.commandRunner,
    signal: new AbortController().signal, applicationId: session.applicationId, onLine: function () {}
  };
  var bytes, tmp = null, result;
  if (session.platform === 'android') {
    result = await runner.screenshot(context, session.rawTargetIdentifier);
    bytes = result.bytes;
  } else {
    var nonce = crypto.randomBytes(12).toString('hex');
    tmp = path.join(paths.APP_RUN_SCREENSHOTS_DIR, '.capture-' + nonce + '.png');
    result = await runner.screenshot(context, session.rawTargetIdentifier, tmp);
    try {
      var hardened = storage.hardenTemporary(tmp, 25 * 1024 * 1024);
      if (result.ok && hardened) bytes = storage.readFileSafe(tmp, 25 * 1024 * 1024);
    } catch (_) {
      latchIntegrity('screenshot-storage-unavailable', session.sessionId);
    }
  }
  var temporaryRemoved = true;
  if (tmp) {
    try { storage.removeTemporary(tmp, 25 * 1024 * 1024); }
    catch (_) {
      temporaryRemoved = false;
      latchIntegrity('screenshot-storage-unavailable', session.sessionId);
    }
  }
  var info = result && result.ok && pngInfo(bytes);
  if (!info || !temporaryRemoved) return { ok: false, status: 409, error: 'screenshot-failed' };
  var id = storage.randomId('shot');
  storage.writeFileAtomic(storage.fileFor(paths.APP_RUN_SCREENSHOTS_DIR, id, '.png'), bytes, 25 * 1024 * 1024);
  var metadata = {
    schemaVersion: 1, screenshotId: id, sessionId: session.sessionId,
    taskStem: session.taskStem, surfaceId: session.surfaceId, width: info.width, height: info.height,
    size: bytes.length, hash: 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'),
    createdAt: now()
  };
  try {
    storage.writeJson(paths.APP_RUN_SCREENSHOTS_DIR, id, metadata, 128 * 1024);
  } catch (metadataError) {
    try {
      storage.remove(paths.APP_RUN_SCREENSHOTS_DIR, id, '.png', 25 * 1024 * 1024);
    } catch (_) {
      latchIntegrity('screenshot-retention-failed', id);
    }
    throw metadataError;
  }
  return { ok: true, status: 201, screenshot: metadata };
}

function screenshotFile(id) {
  if (!/^shot-[a-f0-9]{36}$/.test(String(id || ''))) return null;
  var metadata;
  try {
    metadata = storage.readJson(paths.APP_RUN_SCREENSHOTS_DIR, id, 128 * 1024);
  } catch (_) {
    latchIntegrity('screenshot-recovery-invalid', id);
    return null;
  }
  if (!metadata) return null;
  try {
    var file = storage.fileFor(paths.APP_RUN_SCREENSHOTS_DIR, id, '.png');
    var bytes = storage.readFileSafe(file, 25 * 1024 * 1024);
    var info = pngInfo(bytes);
    if (!metadata || !exactKeys(metadata, [
      'schemaVersion', 'screenshotId', 'sessionId', 'taskStem', 'surfaceId',
      'width', 'height', 'size', 'hash', 'createdAt'
    ]) || metadata.schemaVersion !== 1 || metadata.screenshotId !== id ||
        !/^session-[a-f0-9]{36}$/.test(String(metadata.sessionId || '')) ||
        !safeStem(metadata.taskStem) || !safeSurface(metadata.surfaceId) ||
        !Number.isSafeInteger(metadata.width) || !Number.isSafeInteger(metadata.height) ||
        !info || metadata.width !== info.width || metadata.height !== info.height ||
        !Number.isSafeInteger(metadata.size) || metadata.size !== bytes.length ||
        !/^sha256:[a-f0-9]{64}$/.test(String(metadata.hash || '')) ||
        typeof metadata.createdAt !== 'string' || !Number.isFinite(Date.parse(metadata.createdAt)) ||
        metadata.hash !== 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex')) {
      latchIntegrity('screenshot-recovery-invalid', id);
      return null;
    }
    return { metadata: metadata, bytes: bytes };
  } catch (_) {
    latchIntegrity('screenshot-recovery-invalid', id);
    return null;
  }
}

function screenshotRead(id) {
  var hit = screenshotFile(id);
  if (hit) return { ok: true, status: 200, hit: hit };
  var corrupt = runtime.integrityIssues.some(function (issue) {
    return issue && issue.code === 'screenshot-recovery-invalid' &&
      issue.recordId === id;
  });
  return corrupt
    ? { ok: false, status: 409, error: 'runtime-recovery-required' }
    : { ok: false, status: 404, error: 'screenshot-not-found' };
}

function pruneScreenshots(activeId) {
  var ids = storage.list(paths.APP_RUN_SCREENSHOTS_DIR, 'shot');
  var pngIds = storage.list(paths.APP_RUN_SCREENSHOTS_DIR, 'shot', '.png');
  pngIds.forEach(function (id) {
    if (ids.indexOf(id) >= 0) return;
    // The PNG is written before its metadata. A PNG without metadata is an
    // unpublished crash residue, not validation evidence.
    try { storage.remove(paths.APP_RUN_SCREENSHOTS_DIR, id, '.png', 25 * 1024 * 1024); }
    catch (_) { latchIntegrity('screenshot-retention-failed', id); }
  });
  var rows = ids.map(function (id) {
    var hit = screenshotFile(id);
    return { id: id, metadata: hit && hit.metadata };
  });
  var validRows = rows.filter(function (row) { return !!row.metadata; }).sort(function (a, b) {
    return Date.parse(b.metadata.createdAt) - Date.parse(a.metadata.createdAt);
  });
  var protectedIds = Object.create(null);
  if (activeId) protectedIds[activeId] = true;
  runtime.sessions.forEach(function (session) {
    if (session && (session.state === 'running' || session.state === 'stopping')) {
      validRows.forEach(function (row) {
        if (row.metadata.sessionId === session.sessionId) protectedIds[row.id] = true;
      });
    }
  });
  try {
    validation.history(null, 100).forEach(function (receipt) {
      receipt.checklist.forEach(function (item) {
        item.screenshotIds.forEach(function (id) { protectedIds[id] = true; });
      });
    });
  } catch (historyError) {
    (historyError.recordIds || []).forEach(function (id) {
      latchIntegrity('validation-recovery-invalid', id);
    });
    if (!(historyError.recordIds || []).length) {
      latchIntegrity('validation-storage-unavailable', null);
    }
    // Receipt authority is unavailable, so no published screenshot can be
    // proven unreferenced. Preserve every valid pair.
    return;
  }
  var bytes = 0, kept = 0;
  // Rows with published but invalid metadata may already be referenced by a
  // validation receipt. screenshotFile latched them; preserve both files for
  // explicit recovery and retain only valid rows below.
  validRows.forEach(function (row) {
    var size = row.metadata.size;
    var retain = protectedIds[row.id] ||
      (kept < 50 && bytes + size <= 1024 * 1024 * 1024);
    if (retain) { kept++; bytes += size; return; }
    try { storage.remove(paths.APP_RUN_SCREENSHOTS_DIR, row.id, '.png', 25 * 1024 * 1024); }
    catch (_) { latchIntegrity('screenshot-retention-failed', row.id); }
    try { storage.remove(paths.APP_RUN_SCREENSHOTS_DIR, row.id, '.json', 128 * 1024); }
    catch (_) { latchIntegrity('screenshot-retention-failed', row.id); }
  });
}

function screenshotOwned(id, sessionId, taskStem) {
  var read = screenshotRead(id);
  if (!read.ok) {
    return read.status === 409 ? read : false;
  }
  return read.hit.metadata.sessionId === sessionId &&
    read.hit.metadata.taskStem === taskStem;
}

function logs(query) {
  if ((query.jobId && query.sessionId) || (!query.jobId && !query.sessionId)) {
    return { ok: false, status: 400, error: 'bad-log-query' };
  }
  var id = query.jobId || null;
  if (id) {
    if (!/^job-[a-f0-9]{36}$/.test(String(id))) {
      return { ok: false, status: 400, error: 'bad-log-query' };
    }
    if (!runtime.jobs.has(id)) return { ok: false, status: 404, error: 'job-not-found' };
  } else if (query.sessionId) {
    if (!/^session-[a-f0-9]{36}$/.test(String(query.sessionId))) {
      return { ok: false, status: 400, error: 'bad-log-query' };
    }
    var session = runtime.sessions.get(query.sessionId);
    if (!session) return { ok: false, status: 404, error: 'session-not-found' };
    id = session.jobId;
  }
  var requestedLimit = query.limit === null || query.limit === undefined ? 200 : Number(query.limit);
  if (!Number.isSafeInteger(requestedLimit) || requestedLimit < 1 || requestedLimit > 1000) {
    return { ok: false, status: 400, error: 'bad-log-limit' };
  }
  var limit = requestedLimit;
  var after = 0;
  if (query.cursor) {
    var match = /^cursor-(\d+)$/.exec(String(query.cursor));
    if (!match) return { ok: false, status: 400, error: 'bad-log-cursor' };
    after = Number(match[1]);
  }
  var rows = (runtime.logs.get(id) || []).filter(function (row) { return row.sequence > after; }).slice(0, limit);
  var response = {
    ok: true, status: 200, jobId: id, scope: 'build-install-launch',
    rows: rows, nextCursor: rows.length ? 'cursor-' + rows[rows.length - 1].sequence : query.cursor || 'cursor-' + after
  };
  while (response.rows.length &&
      Buffer.byteLength(JSON.stringify(response), 'utf8') > 256 * 1024) {
    response.rows.pop();
    response.nextCursor = response.rows.length
      ? 'cursor-' + response.rows[response.rows.length - 1].sequence
      : query.cursor || 'cursor-' + after;
  }
  return response;
}

function devicePreview(body) {
  if (!exactKeys(body, ['platform', 'profileId', 'runtimeId', 'discoveryRevision', 'idempotencyKey']) ||
      ['android', 'ios'].indexOf(body.platform) < 0 ||
      !/^profile-[a-f0-9]{32}$/.test(String(body.profileId || '')) ||
      !/^runtime-[a-f0-9]{32}$/.test(String(body.runtimeId || '')) ||
      !/^discovery-[a-f0-9]{36}$/.test(String(body.discoveryRevision || '')) ||
      !safeIdempotency(body.idempotencyKey)) {
    return { ok: false, status: 400, error: 'bad-device-preview-request' };
  }
  var resolved;
  try {
    resolved = discovery.resolveProfile(
      body.platform, body.profileId, body.runtimeId, body.discoveryRevision, discoveryOptions());
  } catch (_) {
    return { ok: false, status: 409, error: 'discovery-failed' };
  }
  if (!resolved.ok) return { ok: false, status: 409, error: resolved.error };
  var project = path.basename(paths.PROJECT_ROOT).replace(/[^A-Za-z0-9]+/g, '_').slice(0, 32) || 'project';
  var suffix = crypto.randomBytes(4).toString('hex');
  var generatedName = body.platform === 'android'
    ? ('orchestrator_' + project + '_' + suffix).slice(0, 80)
    : ('Orchestrator ' + project.replace(/_/g, ' ') + ' ' + suffix).slice(0, 80);
  var previewId = 'preview-' + crypto.randomBytes(18).toString('hex');
  var preview = {
    previewId: previewId, platform: body.platform, profileId: body.profileId, runtimeId: body.runtimeId,
    discoveryRevision: body.discoveryRevision, generatedName: generatedName,
    displayName: resolved.profile.displayName, runtimeName: resolved.profile.runtimeName,
    estimatedBytes: resolved.profile.estimatedBytes,
    rawProfileId: resolved.profile.rawProfileId, rawRuntimeId: resolved.profile.rawRuntimeId,
    profileStableMaterial: resolved.profile.stableMaterial,
    tools: resolved.tools, expiresMs: Date.now() + 5 * 60 * 1000
  };
  preview.previewHash = hash(appRunConfig.canonicalJson({
    platform: preview.platform, profileId: preview.profileId, runtimeId: preview.runtimeId,
    generatedName: preview.generatedName
  }));
  putBoundedPreview(runtime.devicePreviews, previewId, preview);
  return {
    ok: true, status: 200,
    preview: {
      previewId: previewId, platform: preview.platform, profileId: preview.profileId,
      runtimeId: preview.runtimeId, generatedName: generatedName,
      displayName: preview.displayName, runtimeName: preview.runtimeName,
      estimatedBytes: preview.estimatedBytes, warnings: [], acknowledgementCodes: ['create-device'],
      expiresAt: new Date(preview.expiresMs).toISOString()
    }
  };
}

async function createDevice(body) {
  if (!exactKeys(body, ['previewId', 'acknowledgements', 'idempotencyKey']) ||
      !/^preview-[a-f0-9]{36}$/.test(String(body.previewId || '')) ||
      !Array.isArray(body.acknowledgements) || body.acknowledgements.length !== 1 ||
      body.acknowledgements[0] !== 'create-device' || !safeIdempotency(body.idempotencyKey)) {
    return { ok: false, status: 400, error: 'bad-device-create-request' };
  }
  if (runtime.integrityIssues.length) {
    return { ok: false, status: 409, error: 'runtime-recovery-required' };
  }
  var preview = runtime.devicePreviews.get(body.previewId);
  if (!preview || preview.expiresMs <= Date.now()) {
    runtime.devicePreviews.delete(body.previewId);
    return { ok: false, status: 409, error: 'device-preview-expired' };
  }
  if (runtime.activeJobId) return { ok: false, status: 409, error: 'app-run-active' };
  var currentSession = runtime.currentSessionId && runtime.sessions.get(runtime.currentSessionId);
  if (currentSession && (currentSession.state === 'running' || currentSession.state === 'stopping')) {
    return { ok: false, status: 409, error: 'app-run-active' };
  }
  var fresh;
  try { fresh = discovery.discover(discoveryOptions({ refresh: true })); }
  catch (_) { return { ok: false, status: 409, error: 'discovery-failed' }; }
  var freshPlatform = fresh.public.platforms.find(function (row) { return row.id === preview.platform; });
  if (!freshPlatform || freshPlatform.availability === 'unavailable') {
    runtime.devicePreviews.delete(body.previewId);
    return {
      ok: false,
      status: 409,
      error: freshPlatform && freshPlatform.reasonCode || 'discovery-failed'
    };
  }
  var privateProfiles = fresh.private[preview.platform] && fresh.private[preview.platform].profiles || {};
  var freshProfile = Object.keys(privateProfiles).map(function (key) { return privateProfiles[key]; })
    .find(function (row) { return row.stableMaterial === preview.profileStableMaterial; });
  if (!freshProfile) {
    runtime.devicePreviews.delete(body.previewId);
    return { ok: false, status: 409, error: 'device-preview-stale' };
  }
  // Consume the one-shot preview only after all transient admission conflicts
  // are clear and its exact profile/runtime pair was rediscovered.
  runtime.devicePreviews.delete(body.previewId);
  preview.rawProfileId = freshProfile.rawProfileId;
  preview.rawRuntimeId = freshProfile.rawRuntimeId;
  preview.tools = fresh.private[preview.platform].tools;
  var job = newCreateJob(preview, body);
  runtime.jobs.set(job.jobId, job); runtime.activeJobId = job.jobId; persistJob(job);
  var controller = new AbortController(); runtime.controllers.set(job.jobId, controller);
  (async function () {
    try {
      setStage(job, 'detecting', 'running', 'Revalidating the confirmed virtual device profile.');
      setStage(job, 'detecting', 'success', 'Virtual device profile is still available.');
      setStage(job, 'creating-device', 'running', 'Creating the confirmed virtual device.');
      var runner = preview.platform === 'android' ? android : ios;
      var result = await runner.createDevice({
        tools: preview.tools, commandRunner: runtime.commandRunner, signal: controller.signal,
        onLine: function (stream, line) { appendLog(job, stream, 'creating-device', line); }
      }, preview);
      if (controller.signal.aborted) {
        var cancelled = new Error('virtual device creation was cancelled');
        cancelled.code = 'cancelled';
        throw cancelled;
      }
      setStage(job, 'creating-device', 'success', 'Virtual device created.');
      finishJob(job, 'completed', result, null);
      discovery.invalidate();
    } catch (error) {
      finishJob(job, controller.signal.aborted ? 'cancelled' : 'failed', null, error && error.code || 'device-create-failed');
    } finally { runtime.controllers.delete(job.jobId); }
  })();
  return { ok: true, status: 202, job: publicJob(job) };
}

function validationGet(query) {
  var current = validation.checklist(query.taskStem);
  if (!current.ok) return current;
  var session = query.sessionId ? runtime.sessions.get(query.sessionId) :
    (runtime.currentSessionId ? runtime.sessions.get(runtime.currentSessionId) : null);
  var currentProject = source(true, stemProductRoot(query.taskStem));
  var latest;
  try {
    latest = validation.history(query.taskStem, 1)[0] || null;
  } catch (historyError) {
    (historyError.recordIds || []).forEach(function (id) {
      latchIntegrity('validation-recovery-invalid', id);
    });
    if (!(historyError.recordIds || []).length) latchIntegrity('validation-storage-unavailable', null);
    return { ok: false, status: 409, error: 'runtime-recovery-required' };
  }
  return Object.assign(current, {
    status: 200,
    eligibleSession: session && session.state === 'running' && session.taskStem === query.taskStem
      ? publicSession(session) : null,
    latestReceipt: validation.currentReceipt(
      latest,
      current.taskSourceRevision,
      currentProject.available ? currentProject.revision : null
    )
  });
}

async function validationSave(body) {
  if (runtime.integrityIssues.length) {
    return { ok: false, status: 409, error: 'runtime-recovery-required' };
  }
  var result = await validation.save(body, {
    resolveSession: function (id, revision) { return resolveSession(id, revision); },
    screenshotOwned: screenshotOwned,
    currentSourceRevision: function () {
      var current = source(true, stemProductRoot(body.taskStem));
      return current.available ? current.revision : null;
    }
  });
  (result.retentionIssues || []).forEach(function (id) {
    latchIntegrity('validation-retention-failed', id);
  });
  delete result.retentionIssues;
  return result;
}

function history(limit, cursor) {
  var requestedLimit = limit === null || limit === undefined || limit === '' ? 20 : Number(limit);
  if (!Number.isSafeInteger(requestedLimit) || requestedLimit < 1 || requestedLimit > 100) {
    return { ok: false, status: 400, error: 'bad-history-limit' };
  }
  limit = requestedLimit;
  var rows = Array.from(runtime.jobs.values()).filter(function (job) {
    return TERMINAL[job.state] || job.state === 'running';
  });
  var page = historyPagination.page(rows, cursor, limit, 'jobId', /^job-[a-f0-9]{36}$/);
  if (!page.ok) return { ok: false, status: 400, error: 'bad-history-cursor' };
  var jobs = page.rows.map(publicJob);
  var validations;
  try {
    validations = validation.history(null, limit);
  } catch (historyError) {
    (historyError.recordIds || []).forEach(function (id) {
      latchIntegrity('validation-recovery-invalid', id);
    });
    if (!(historyError.recordIds || []).length) latchIntegrity('validation-storage-unavailable', null);
    return { ok: false, status: 409, error: 'runtime-recovery-required' };
  }
  return {
    ok: true, status: 200, jobs: jobs, validations: validations,
    nextCursor: page.nextCursor
  };
}

function taskJobs(stem, limit) {
  if (!safeStem(stem)) return { ok: false, status: 400, error: 'invalid-task-stem' };
  limit = limit === undefined ? 100 : Number(limit);
  if (!Number.isSafeInteger(limit) || limit < 1 || limit > 100) {
    return { ok: false, status: 400, error: 'bad-history-limit' };
  }
  return {
    ok: true,
    status: 200,
    jobs: Array.from(runtime.jobs.values()).filter(function (job) {
      return job && job.taskStem === stem && job.action !== 'create-device';
    }).sort(function (left, right) {
      return Date.parse(right.updatedAt) - Date.parse(left.updatedAt);
    }).slice(0, limit).map(publicJob)
  };
}

function status() {
  var currentJob = runtime.activeJobId ? runtime.jobs.get(runtime.activeJobId) : null;
  if (!currentJob) {
    currentJob = Array.from(runtime.jobs.values()).sort(function (a, b) {
      return Date.parse(b.updatedAt) - Date.parse(a.updatedAt);
    })[0] || null;
  }
  var session = runtime.currentSessionId ? runtime.sessions.get(runtime.currentSessionId) : null;
  var persisted;
  try {
    persisted = persistence.readPersisted();
  } catch (_) {
    return { ok: false, status: 409, error: 'runtime-recovery-required' };
  }
  var availability;
  try {
    availability = discovery.targets(null, false, discoveryOptions()).platforms.map(function (row) {
      return { platform: row.id, availability: row.availability, reasonCode: row.reasonCode, toolchain: row.toolchain };
    });
  } catch (_) {
    availability = [
      { platform: 'android', availability: 'unavailable', reasonCode: 'discovery-failed', toolchain: {} },
      { platform: 'ios', availability: 'unavailable', reasonCode: 'discovery-failed', toolchain: {} }
    ];
  }
  return {
    ok: true, status: 200, schemaVersion: 1,
    job: publicJob(currentJob),
    session: publicSession(session),
    preferences: persisted.appRunPreferences,
    availability: availability,
    integrity: {
      ok: runtime.integrityIssues.length === 0,
      issues: runtime.integrityIssues.slice(0, 20)
    },
    projectBusy: projectMutationBlocked(),
    actions: {
      canStart: !runtime.activeJobId && !(session && (session.state === 'running' || session.state === 'stopping')) &&
        runtime.integrityIssues.length === 0,
      canCancel: !!(currentJob && CANCELLABLE[currentJob.state]),
      canStop: !!(session && session.state === 'running'),
      canRestart: runtime.integrityIssues.length === 0 &&
        !!(session && session.state === 'running'),
      canScreenshot: runtime.integrityIssues.length === 0 &&
        !!(session && session.state === 'running')
    }
  };
}

function pruneJobs() {
  var rows = Array.from(runtime.jobs.values()).sort(function (a, b) {
    return Date.parse(b.updatedAt) - Date.parse(a.updatedAt);
  });
  rows.slice(50).forEach(function (job) {
    if (ACTIVE_STATES[job.state] || job.state === 'running') return;
    runtime.jobs.delete(job.jobId);
    runtime.logs.delete(job.jobId);
    runtime.logBytes.delete(job.jobId);
    var logTimer = runtime.logFlushTimers.get(job.jobId);
    if (logTimer) clearTimeout(logTimer);
    runtime.logFlushTimers.delete(job.jobId);
    runtime.idempotency.forEach(function (row, key) {
      if (row.jobId === job.jobId) runtime.idempotency.delete(key);
    });
    try { storage.remove(paths.APP_RUN_JOBS_DIR, job.jobId); }
    catch (_) { latchIntegrity('job-retention-failed', job.jobId); }
    try { storage.remove(paths.APP_RUN_LOGS_DIR, job.jobId, '.json', 2 * 1024 * 1024); }
    catch (_) { latchIntegrity('log-retention-failed', job.jobId); }
  });
  persistIndex();
}

function pruneSessions() {
  var rows = Array.from(runtime.sessions.values()).sort(function (a, b) {
    return Date.parse(b.updatedAt) - Date.parse(a.updatedAt);
  });
  rows.slice(50).forEach(function (session) {
    if (session.sessionId === runtime.currentSessionId ||
        session.state === 'running' || session.state === 'stopping') return;
    runtime.sessions.delete(session.sessionId);
    try { storage.remove(paths.APP_RUN_SESSIONS_DIR, session.sessionId); }
    catch (_) { latchIntegrity('session-retention-failed', session.sessionId); }
  });
  persistIndex();
}

function recover() {
  storage.list(paths.APP_RUN_JOBS_DIR, 'job').forEach(function (id) {
    try {
      var job = storage.readJson(paths.APP_RUN_JOBS_DIR, id);
      if (!job || job.jobId !== id || !Number.isSafeInteger(job.jobRevision)) {
        throw new Error('stored app-run job identity is invalid');
      }
      assertStoredJobShape(job);
      assertJobInvariant(job);
      if (ACTIVE_STATES[job.state] || job.state === 'running') {
        var wasQueued = job.state === 'queued';
        var recoveredConfig = wasQueued ? appRunConfig.load() : null;
        var recoveredSource = wasQueued ? source(true, jobProductRoot(job)) : null;
        var queuedStillValid = wasQueued && recoveredConfig.ok && recoveredSource.available &&
          recoveredConfig.runConfigHash === job.runConfigHash &&
          recoveredSource.revision === job.productSourceRevision;
        job.state = queuedStillValid ? 'queued' : 'interrupted';
        if (!queuedStillValid) {
          var interruptedStage = job.stages.find(function (stage) {
            return stage.status === 'running';
          });
          if (interruptedStage) {
            interruptedStage.status = 'failed';
            interruptedStage.durationMs = interruptedStage.startedAt
              ? Math.max(0, Date.now() - Date.parse(interruptedStage.startedAt)) : 0;
            interruptedStage.message = 'Interrupted by a server restart.';
          }
        }
        job.phase = job.state; job.errorCode = job.state === 'interrupted' ? 'process-interrupted' : null;
        var recoveryStamp = now();
        job.finishedAt = job.state === 'interrupted' ? recoveryStamp : null;
        job.updatedAt = recoveryStamp;
        job.jobRevision++;
        persistJob(job);
      }
      runtime.jobs.set(id, job);
      if (job.idempotencyKeyHash && job.requestHash) {
        runtime.idempotency.set(job.idempotencyKeyHash, { jobId: id, requestHash: job.requestHash });
      }
    } catch (_) {
      runtime.integrityIssues.push({ code: 'job-recovery-invalid', recordId: id });
    }
  });
  var queuedJobs = Array.from(runtime.jobs.values()).filter(function (job) {
    return job.state === 'queued';
  });
  if (queuedJobs.length > 1) {
    runtime.integrityIssues.push({ code: 'multiple-queued-jobs', recordId: null });
  }
  storage.list(paths.APP_RUN_SESSIONS_DIR, 'session').forEach(function (id) {
    try {
      var session = storage.readJson(paths.APP_RUN_SESSIONS_DIR, id);
      if (!session || session.sessionId !== id) {
        throw new Error('stored app-run session identity is invalid');
      }
      assertSessionInvariant(session);
      if (session.state === 'running' || session.state === 'stopping') {
        session.state = 'lost'; session.sessionRevision++; session.updatedAt = now(); persistSession(session);
      }
      runtime.sessions.set(id, session);
    } catch (_) {
      runtime.integrityIssues.push({ code: 'session-recovery-invalid', recordId: id });
    }
  });
  try {
    storage.list(paths.APP_RUN_LOGS_DIR, 'job').forEach(function (id) {
      try {
        var value = storage.readJson(paths.APP_RUN_LOGS_DIR, id, 2 * 1024 * 1024);
        if (!runtime.jobs.has(id) || !value || !exactKeys(value, ['schemaVersion', 'rows']) ||
            value.schemaVersion !== 1 || !Array.isArray(value.rows) || value.rows.length > 10000) {
          throw new Error('stored app-run log envelope is invalid');
        }
        var prior = 0, valid = value.rows.every(function (row) {
          var ok = validLogRow(row, prior);
          if (ok) prior = row.sequence;
          return ok;
        });
        if (!valid) throw new Error('stored app-run log row is invalid');
        runtime.logs.set(id, value.rows);
        runtime.logBytes.set(id, value.rows.reduce(function (total, row) {
          return total + logRowBytes(row);
        }, 40));
      } catch (_) {
        latchIntegrity('log-recovery-invalid', id);
      }
    });
  } catch (_) {
    latchIntegrity('log-storage-unavailable', null);
  }
  pruneJobs();
  pruneSessions();
  try {
    pruneScreenshots(null);
  } catch (_) {
    latchIntegrity('screenshot-storage-unavailable', null);
  }
  try {
    artifacts.prune().forEach(function (id) {
      latchIntegrity('artifact-retention-failed', id);
    });
  } catch (_) {
    latchIntegrity('artifact-storage-unavailable', null);
  }
  try {
    validation.pruneHistory(null).forEach(function (id) {
      latchIntegrity('validation-retention-failed', id);
    });
  } catch (_) {
    latchIntegrity('validation-storage-unavailable', null);
  }
  if (queuedJobs.length === 1 && runtime.integrityIssues.length === 0) {
    runtime.activeJobId = queuedJobs[0].jobId;
  }
  persistIndex();
  if (runtime.activeJobId) executeRun(queuedJobs[0]).catch(function () {
    latchIntegrity('job-recovery-execution-failed', queuedJobs[0].jobId);
  });
}

function configure(options) {
  options = options || {};
  if (typeof options.notify === 'function') runtime.notify = options.notify;
  if (typeof options.persistPreference === 'function') runtime.persistPreference = options.persistPreference;
  if (options.commandRunner) runtime.commandRunner = options.commandRunner;
  if (options.androidTools) runtime.androidTools = options.androidTools;
  if (options.iosTools) runtime.iosTools = options.iosTools;
  if (options.forceIos === true) runtime.forceIos = true;
}

function init(options) {
  if (runtime.initialized) { configure(options); return; }
  configure(options);
  storage.init();
  recover();
  runtime.initialized = true;
}

function resetForTests() {
  runtime.logFlushTimers.forEach(function (timer) { clearTimeout(timer); });
  runtime.jobs.clear(); runtime.sessions.clear(); runtime.logs.clear(); runtime.logBytes.clear();
  runtime.controllers.clear();
  runtime.logFlushTimers.clear();
  runtime.idempotency.clear(); runtime.confirmations.clear(); runtime.devicePreviews.clear();
  runtime.mutations.clear();
  runtime.integrityIssues = [];
  runtime.activeJobId = null; runtime.currentSessionId = null; runtime.initialized = false;
  if (runtime.sourceWatcher) clearInterval(runtime.sourceWatcher);
  runtime.sourceWatcher = null; runtime.sourceWatcherState = null; runtime.sourceCache = null;
  runtime.notify = function () {}; runtime.persistPreference = null; runtime.commandRunner = processRunner;
  runtime.androidTools = null; runtime.iosTools = null; runtime.forceIos = false;
}

function killAll() {
  if (runtime.sourceWatcher) clearInterval(runtime.sourceWatcher);
  runtime.sourceWatcher = null;
  runtime.controllers.forEach(function (controller, jobId) {
    try { controller.abort(); } catch (_) {}
    var job = runtime.jobs.get(jobId);
    if (job && !TERMINAL[job.state] && job.state !== 'running') {
      finishJob(job, 'interrupted', null, 'process-interrupted');
    }
  });
  Array.from(runtime.logs.keys()).forEach(flushLogs);
}

module.exports = {
  init: init,
  configure: configure,
  status: status,
  targets: function (platform, refresh) {
    try {
      return { ok: true, status: 200, targets: discovery.targets(platform, refresh, discoveryOptions()) };
    } catch (_) {
      return { ok: false, status: 409, error: 'discovery-failed' };
    }
  },
  start: start,
  cancel: function (body) { return idempotentMutation('cancel', body, function () { return cancel(body); }); },
  stop: function (body) { return idempotentMutation('stop', body, function () { return stop(body); }); },
  restart: function (body) { return idempotentMutation('restart', body, function () { return restart(body); }); },
  screenshot: function (body) {
    return idempotentMutation('screenshot', body, function () {
      return screenshot(body);
    }).then(function (result) {
      if (result && result.ok && result.screenshot) pruneScreenshots(result.screenshot.screenshotId);
      return result;
    });
  },
  screenshotFile: screenshotFile,
  screenshotRead: screenshotRead,
  logs: logs,
  devicePreview: function (body) {
    return idempotentMutation('device-preview', body, function () { return devicePreview(body); });
  },
  createDevice: function (body) { return idempotentMutation('create-device', body, function () { return createDevice(body); }); },
  validationGet: validationGet,
  validationSave: function (body) { return idempotentMutation('validation', body, function () { return validationSave(body); }); },
  history: history,
  taskJobs: taskJobs,
  publicJob: publicJob,
  publicSession: publicSession,
  pngInfo: pngInfo,
  resolveSession: resolveSession,
  transitionAllowed: transitionAllowed,
  assertJobInvariant: assertJobInvariant,
  assertSessionInvariant: assertSessionInvariant,
  killAll: killAll,
  _runtime: runtime,
  _resetForTests: resetForTests
};
