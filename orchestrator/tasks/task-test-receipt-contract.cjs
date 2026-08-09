'use strict';

// ---------------------------------------------------------------------------
// Typed test receipt contract (improvement 05, Phase 3) — one module, two
// strict kinds:
//   test-command receipts        (domain test-command-receipt\0)
//   test-structural-gate receipts (domain test-structural-receipt\0)
// Executor-owned, immutable, two-stage (started → terminal). A structural
// receipt can never satisfy an executable behavior anchor, lane or suite.
// evaluateCommandReceipt() is the single fail-closed reading of a terminal
// command receipt: zero discovered tests, NO-SOURCE where tests are required,
// all-skipped, cache substitution on the direct tier and timeouts are typed
// violations, never a silent PASS.
// ---------------------------------------------------------------------------

const crypto = require('crypto');

const COMMAND_DOMAIN = 'test-command-receipt';
const STRUCTURAL_DOMAIN = 'test-structural-receipt';
const HASH_RE = /^sha256:[0-9a-f]{64}$/;
const ISO_RE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/;
const STEM_RE = /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/;
const RUN_RE = /^run-[A-Za-z0-9][A-Za-z0-9-]{0,79}$/;
// Bounded to hold any id the canonical orchestrator lock can carry
// (`LOCK_SESSION_ID_RE` in task-state-core.cjs, max 163 chars); a narrower
// bound would reject a legal session and strand the run it just started.
const SESSION_RE = /^[A-Za-z0-9][A-Za-z0-9._:-]{0,162}$/;
const ID_RE = /^[a-z][a-z0-9-]{0,79}$/;
const TASK_PATH_RE = /^:[A-Za-z0-9._:-]*[A-Za-z0-9]$/;
const LANES = Object.freeze(['android-device', 'common', 'host', 'ios-simulator', 'screenshot', 'structural']);
const PARSERS = Object.freeze(['junit-xml', 'kotlin-native-xml', 'android-connected-xml', 'roborazzi-report']);
const TIERS = Object.freeze(['affected-closure', 'builder-feedback', 'certification-direct', 'full-suite', 'owner-module', 'platform-lanes']);
const COMMAND_KEYS = Object.freeze([
  'counts', 'cwd', 'discoveredTestIdentities', 'disposition', 'durationMs', 'endedAt',
  'envFingerprint', 'executionRootKind', 'exitCode', 'impactHash', 'kind', 'lane',
  'leafResults', 'lockStage', 'outputDigest', 'pid', 'policyHash', 'processGroup',
  'receiptHash', 'reportArtifacts', 'retryHistory', 'runId', 'sessionId', 'signal',
  'sourceSnapshotHash', 'stage', 'startedAt', 'startedReceiptHash', 'suite',
  'taskInputHash', 'taskPaths', 'taskStem', 'tier', 'timedOut', 'toolchain', 'version'
]);
const STRUCTURAL_KEYS = Object.freeze([
  'artifacts', 'durationMs', 'endedAt', 'executionMode', 'exitCode', 'gateId', 'kind',
  'lockStage', 'outputDigest', 'pid', 'policyHash', 'processGroup', 'receiptHash',
  'result', 'runId', 'sessionId', 'signal', 'sourceSnapshotHash', 'stage', 'startedAt',
  'startedReceiptHash', 'taskInputHash', 'taskStem', 'timedOut', 'tool',
  'validatorCodeHash', 'version'
]);
const COUNT_KEYS = Object.freeze(['aborted', 'discovered', 'executed', 'failed', 'passed', 'skipped']);

class TestReceiptError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'TestReceiptError';
    this.code = code;
  }
}

function fail(code, message) { throw new TestReceiptError(code, message); }

function sha256(value) {
  return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex');
}

function canonicalJson(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
  return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
}

function receiptHashOf(receipt) {
  const domain = receipt.kind === 'test-command' ? COMMAND_DOMAIN : STRUCTURAL_DOMAIN;
  const model = {};
  for (const key of Object.keys(receipt)) {
    if (key !== 'receiptHash') model[key] = receipt[key];
  }
  return sha256(domain + '\0' + canonicalJson(model));
}

function checkIdentity(receipt) {
  if (!STEM_RE.test(String(receipt.taskStem))) fail('RECEIPT_INVALID', 'taskStem grammar');
  if (!RUN_RE.test(String(receipt.runId))) fail('RECEIPT_INVALID', 'runId grammar');
  if (!SESSION_RE.test(String(receipt.sessionId))) fail('RECEIPT_INVALID', 'sessionId grammar');
  if (!/^[a-z][a-z0-9-]{0,39}$/.test(String(receipt.lockStage))) fail('RECEIPT_INVALID', 'lockStage grammar');
  for (const name of ['taskInputHash', 'sourceSnapshotHash', 'policyHash']) {
    if (!HASH_RE.test(String(receipt[name]))) fail('RECEIPT_INVALID', name + ' grammar');
  }
  if (!ISO_RE.test(String(receipt.startedAt))) fail('RECEIPT_INVALID', 'startedAt grammar');
  if (typeof receipt.timedOut !== 'boolean') fail('RECEIPT_INVALID', 'timedOut must be boolean');
}

function checkStage(receipt, { requireExitEvidence = true } = {}) {
  if (receipt.stage === 'started') {
    for (const name of ['endedAt', 'durationMs', 'exitCode', 'signal', 'startedReceiptHash']) {
      if (receipt[name] !== null) fail('RECEIPT_INVALID', 'started stage requires null ' + name);
    }
    if (receipt.timedOut !== false) fail('RECEIPT_INVALID', 'started stage cannot be timed out');
  } else if (receipt.stage === 'terminal') {
    if (!ISO_RE.test(String(receipt.endedAt))) fail('RECEIPT_INVALID', 'terminal stage requires endedAt');
    if (!Number.isSafeInteger(receipt.durationMs) || receipt.durationMs < 0) fail('RECEIPT_INVALID', 'terminal durationMs');
    if (!HASH_RE.test(String(receipt.startedReceiptHash))) fail('RECEIPT_INVALID', 'terminal stage binds startedReceiptHash');
    // In-process structural gates have no child process: their terminal proof
    // is the typed result + validator code hash instead of exit evidence.
    if (requireExitEvidence && receipt.exitCode === null && receipt.signal === null && !receipt.timedOut) {
      fail('RECEIPT_INVALID', 'terminal stage needs exitCode, signal or timeout');
    }
    if (receipt.exitCode !== null && (!Number.isSafeInteger(receipt.exitCode) || receipt.exitCode < 0 || receipt.exitCode > 255)) {
      fail('RECEIPT_INVALID', 'terminal exitCode');
    }
    if (receipt.signal !== null && (typeof receipt.signal !== 'string' || !/^SIG[A-Z0-9]{1,16}$/.test(receipt.signal))) {
      fail('RECEIPT_INVALID', 'terminal signal');
    }
  } else fail('RECEIPT_INVALID', 'unknown stage');
}

function checkDigest(digest) {
  if (!digest || typeof digest !== 'object' || Array.isArray(digest)) fail('RECEIPT_INVALID', 'outputDigest shape');
  const keys = Object.keys(digest).sort();
  if (keys.join(',') !== 'bytes,hash,redacted') fail('RECEIPT_INVALID', 'outputDigest keys');
  if (digest.redacted !== true) fail('RECEIPT_INVALID', 'output digests are redacted before persistence');
  if (!Number.isSafeInteger(digest.bytes) || digest.bytes < 0 || digest.bytes > 16 * 1024 * 1024) fail('RECEIPT_INVALID', 'outputDigest bytes');
  if (!HASH_RE.test(String(digest.hash))) fail('RECEIPT_INVALID', 'outputDigest hash');
}

function validateCommandReceipt(receipt) {
  if (!receipt || typeof receipt !== 'object' || Array.isArray(receipt)) fail('RECEIPT_INVALID', 'receipt must be an object');
  const keys = Object.keys(receipt).sort();
  if (keys.length !== COMMAND_KEYS.length || keys.some((key, i) => key !== COMMAND_KEYS[i])) {
    fail('RECEIPT_INVALID', 'command receipt keys must be exactly the v1 contract');
  }
  if (receipt.version !== 1) fail('RECEIPT_INVALID', 'unsupported version');
  if (receipt.kind !== 'test-command') fail('RECEIPT_INVALID', 'kind must be test-command');
  checkIdentity(receipt);
  checkStage(receipt);
  if (!ID_RE.test(String(receipt.suite))) fail('RECEIPT_INVALID', 'suite grammar');
  if (!TIERS.includes(receipt.tier)) fail('RECEIPT_INVALID', 'unknown execution tier');
  if (!LANES.includes(receipt.lane)) fail('RECEIPT_INVALID', 'unknown lane');
  if (!Array.isArray(receipt.taskPaths) || receipt.taskPaths.length === 0 || receipt.taskPaths.length > 64) {
    fail('RECEIPT_INVALID', 'taskPaths bounds');
  }
  for (const taskPath of receipt.taskPaths) {
    if (!TASK_PATH_RE.test(String(taskPath))) fail('RECEIPT_INVALID', 'taskPath grammar: ' + taskPath);
  }
  if (new Set(receipt.taskPaths).size !== receipt.taskPaths.length) fail('RECEIPT_INVALID', 'duplicate taskPath');
  if (typeof receipt.cwd !== 'string' || receipt.cwd.length === 0 || receipt.cwd.length > 500) fail('RECEIPT_INVALID', 'cwd grammar');
  if (!HASH_RE.test(String(receipt.envFingerprint))) fail('RECEIPT_INVALID', 'envFingerprint grammar');
  if (!HASH_RE.test(String(receipt.impactHash))) fail('RECEIPT_INVALID', 'impactHash grammar');
  const toolchainKeys = Object.keys(receipt.toolchain || {}).sort();
  if (toolchainKeys.join(',') !== 'agp,arch,gradle,jdk,kotlin,os') fail('RECEIPT_INVALID', 'toolchain keys');
  for (const value of Object.values(receipt.toolchain || {})) {
    if (typeof value !== 'string' || value.length === 0 || value.length > 40) fail('RECEIPT_INVALID', 'toolchain value grammar');
  }
  if (!['pending', 'executed', 'from-cache', 'up-to-date', 'no-source', 'skipped', 'aborted'].includes(receipt.disposition)) {
    fail('RECEIPT_INVALID', 'unknown disposition');
  }
  if (receipt.stage === 'started' && receipt.disposition !== 'pending') fail('RECEIPT_INVALID', 'started stage is pending');
  if (receipt.stage === 'terminal' && receipt.disposition === 'pending') fail('RECEIPT_INVALID', 'terminal stage cannot stay pending');
  const countKeys = Object.keys(receipt.counts || {}).sort();
  if (countKeys.join(',') !== COUNT_KEYS.join(',')) fail('RECEIPT_INVALID', 'counts keys');
  for (const key of COUNT_KEYS) {
    const count = receipt.counts[key];
    if (!Number.isSafeInteger(count) || count < 0 || count > 1000000) fail('RECEIPT_INVALID', 'count ' + key);
  }
  if (!Array.isArray(receipt.leafResults) || receipt.leafResults.length > 256) fail('RECEIPT_INVALID', 'leafResults bounds');
  for (const leaf of receipt.leafResults) {
    const leafKeys = Object.keys(leaf || {}).sort();
    if (leafKeys.join(',') !== 'disposition,outcome,taskPath') fail('RECEIPT_INVALID', 'leaf keys');
    if (!['passed', 'failed', 'no-source', 'skipped', 'aborted'].includes(leaf.outcome)) fail('RECEIPT_INVALID', 'leaf outcome');
    if (!['executed', 'from-cache', 'up-to-date', 'no-source', 'skipped'].includes(leaf.disposition)) fail('RECEIPT_INVALID', 'leaf disposition');
    if (!TASK_PATH_RE.test(String(leaf.taskPath)) || !receipt.taskPaths.includes(leaf.taskPath)) {
      fail('RECEIPT_INVALID', 'leaf taskPath is not a declared task path');
    }
  }
  if (receipt.stage === 'terminal' && receipt.leafResults.length !== receipt.taskPaths.length) {
    fail('RECEIPT_INVALID', 'terminal receipt requires one leaf result per task path');
  }
  if (!Array.isArray(receipt.discoveredTestIdentities) || receipt.discoveredTestIdentities.length > 4096) {
    fail('RECEIPT_INVALID', 'discoveredTestIdentities bounds');
  }
  for (const identity of receipt.discoveredTestIdentities) {
    if (typeof identity !== 'string' || identity.length === 0 || identity.length > 300) {
      fail('RECEIPT_INVALID', 'discovered test identity grammar');
    }
  }
  if (!Array.isArray(receipt.reportArtifacts) || receipt.reportArtifacts.length > 256) fail('RECEIPT_INVALID', 'reportArtifacts bounds');
  for (const artifact of receipt.reportArtifacts) {
    const artifactKeys = Object.keys(artifact || {}).sort();
    if (artifactKeys.join(',') !== 'bytes,hash,parser,path') fail('RECEIPT_INVALID', 'report artifact keys');
    if (typeof artifact.path !== 'string' || artifact.path.length === 0 || artifact.path.length > 400 ||
        artifact.path.includes('..') || artifact.path.startsWith('/')) {
      fail('RECEIPT_INVALID', 'report artifact path must be a bounded relative path');
    }
    if (!PARSERS.includes(artifact.parser)) fail('RECEIPT_INVALID', 'unknown report parser: ' + artifact.parser);
    if (!HASH_RE.test(String(artifact.hash))) fail('RECEIPT_INVALID', 'report artifact hash');
    if (!Number.isSafeInteger(artifact.bytes) || artifact.bytes < 0 || artifact.bytes > 64 * 1024 * 1024) fail('RECEIPT_INVALID', 'report artifact bytes');
  }
  checkDigest(receipt.outputDigest);
  if (!Array.isArray(receipt.retryHistory) || receipt.retryHistory.length > 1) {
    fail('RECEIPT_INVALID', 'retry history is bounded to one diagnostic retry');
  }
  for (const retry of receipt.retryHistory) {
    const retryKeys = Object.keys(retry || {}).sort();
    if (retryKeys.join(',') !== 'priorReceiptHash,reason' ||
        !HASH_RE.test(String(retry.priorReceiptHash)) ||
        !/^[a-z][a-z0-9-]{0,79}$/.test(String(retry.reason))) {
      fail('RECEIPT_INVALID', 'retry history entry');
    }
  }
  if (receipt.stage === 'started' && (receipt.leafResults.length !== 0 || receipt.discoveredTestIdentities.length !== 0 ||
      receipt.reportArtifacts.length !== 0 || receipt.retryHistory.length !== 0 ||
      COUNT_KEYS.some((key) => receipt.counts[key] !== 0) || receipt.outputDigest.bytes !== 0)) {
    fail('RECEIPT_INVALID', 'started receipt cannot carry terminal evidence');
  }
  if (receipt.stage === 'terminal') {
    if (receipt.counts.executed !== receipt.counts.passed + receipt.counts.failed ||
        receipt.counts.discovered !== receipt.counts.passed + receipt.counts.failed + receipt.counts.skipped) {
      fail('RECEIPT_INVALID', 'terminal counts are inconsistent');
    }
    if (receipt.counts.discovered !== receipt.discoveredTestIdentities.length) {
      fail('RECEIPT_INVALID', 'discovered count must equal concrete test identities');
    }
  }
  if (!Number.isSafeInteger(receipt.pid) || receipt.pid < 1) fail('RECEIPT_INVALID', 'pid');
  if (!Number.isSafeInteger(receipt.processGroup) || receipt.processGroup < 1) fail('RECEIPT_INVALID', 'processGroup');
  if (!['shared-serial', 'task-worktree', 'integrated'].includes(receipt.executionRootKind)) {
    fail('RECEIPT_INVALID', 'unknown executionRootKind');
  }
  if (!HASH_RE.test(String(receipt.receiptHash))) fail('RECEIPT_INVALID', 'receiptHash grammar');
  if (receiptHashOf(receipt) !== receipt.receiptHash) fail('HASH_MISMATCH', 'receiptHash does not match content');
  return Object.freeze(JSON.parse(JSON.stringify(receipt)));
}

function validateStructuralReceipt(receipt) {
  if (!receipt || typeof receipt !== 'object' || Array.isArray(receipt)) fail('RECEIPT_INVALID', 'receipt must be an object');
  const keys = Object.keys(receipt).sort();
  if (keys.length !== STRUCTURAL_KEYS.length || keys.some((key, i) => key !== STRUCTURAL_KEYS[i])) {
    fail('RECEIPT_INVALID', 'structural receipt keys must be exactly the v1 contract');
  }
  if (receipt.version !== 1) fail('RECEIPT_INVALID', 'unsupported version');
  if (receipt.kind !== 'test-structural-gate') fail('RECEIPT_INVALID', 'kind must be test-structural-gate');
  checkIdentity(receipt);
  checkStage(receipt, { requireExitEvidence: receipt.executionMode === 'external' });
  if (!ID_RE.test(String(receipt.gateId))) fail('RECEIPT_INVALID', 'gateId grammar');
  if (receipt.executionMode === 'in-process') {
    if (!HASH_RE.test(String(receipt.validatorCodeHash))) fail('RECEIPT_INVALID', 'in-process mode binds validatorCodeHash');
    if (receipt.pid !== null || receipt.processGroup !== null) fail('RECEIPT_INVALID', 'in-process mode cannot claim external execution');
  } else if (receipt.executionMode === 'external') {
    if (receipt.validatorCodeHash !== null) fail('RECEIPT_INVALID', 'external mode carries no code hash');
    if (!Number.isSafeInteger(receipt.pid) || receipt.pid < 1) fail('RECEIPT_INVALID', 'external mode requires pid');
    if (!Number.isSafeInteger(receipt.processGroup) || receipt.processGroup < 1) fail('RECEIPT_INVALID', 'external mode requires processGroup');
  } else fail('RECEIPT_INVALID', 'unknown executionMode');
  if (typeof receipt.tool !== 'string' || receipt.tool.length === 0 || receipt.tool.length > 200) fail('RECEIPT_INVALID', 'tool grammar');
  if (!['pending', 'passed', 'failed', 'aborted'].includes(receipt.result)) fail('RECEIPT_INVALID', 'unknown result');
  if (receipt.stage === 'started' && receipt.result !== 'pending') fail('RECEIPT_INVALID', 'started stage is pending');
  if (receipt.stage === 'terminal' && receipt.result === 'pending') fail('RECEIPT_INVALID', 'terminal stage cannot stay pending');
  if (receipt.stage === 'terminal' && receipt.executionMode === 'in-process' &&
      (receipt.exitCode !== null || receipt.signal !== null || receipt.timedOut)) {
    fail('RECEIPT_INVALID', 'in-process terminal receipt cannot carry external exit evidence');
  }
  if (receipt.stage === 'terminal' && receipt.result === 'passed' &&
      (receipt.timedOut || receipt.signal !== null ||
        (receipt.executionMode === 'external' && receipt.exitCode !== 0))) {
    fail('RECEIPT_INVALID', 'a passed structural gate requires clean terminal evidence');
  }
  if (receipt.stage === 'terminal' && receipt.result === 'aborted' &&
      !receipt.timedOut && receipt.signal === null) {
    fail('RECEIPT_INVALID', 'an aborted structural gate requires timeout or signal evidence');
  }
  if (!Array.isArray(receipt.artifacts) || receipt.artifacts.length > 64) fail('RECEIPT_INVALID', 'artifacts bounds');
  for (const artifact of receipt.artifacts) {
    const artifactKeys = Object.keys(artifact || {}).sort();
    if (artifactKeys.join(',') !== 'bytes,hash,path') fail('RECEIPT_INVALID', 'artifact keys');
    if (typeof artifact.path !== 'string' || artifact.path.length === 0 || artifact.path.length > 400 ||
        artifact.path.includes('..') || artifact.path.startsWith('/')) {
      fail('RECEIPT_INVALID', 'artifact path must be a bounded relative path');
    }
    if (!HASH_RE.test(String(artifact.hash))) fail('RECEIPT_INVALID', 'artifact hash');
    if (!Number.isSafeInteger(artifact.bytes) || artifact.bytes < 0 || artifact.bytes > 64 * 1024 * 1024) {
      fail('RECEIPT_INVALID', 'artifact bytes');
    }
  }
  checkDigest(receipt.outputDigest);
  // A structural receipt has no test-count surface at all — the key set above
  // excludes counts/discovered identities by construction, so it can never be
  // read as executable-lane evidence.
  if (!HASH_RE.test(String(receipt.receiptHash))) fail('RECEIPT_INVALID', 'receiptHash grammar');
  if (receiptHashOf(receipt) !== receipt.receiptHash) fail('HASH_MISMATCH', 'receiptHash does not match content');
  return Object.freeze(JSON.parse(JSON.stringify(receipt)));
}

// The one fail-closed reading of a terminal command receipt. The execution
// tier is signed into the receipt; callers cannot downgrade a direct run to
// an affected/cache-reusable run while evaluating it later.
function evaluateCommandReceipt(receipt, { testsRequired } = {}) {
  const valid = validateCommandReceipt(receipt);
  if (valid.stage !== 'terminal') fail('RECEIPT_NOT_TERMINAL', 'only terminal receipts are evidence');
  const violations = [];
  if (valid.timedOut) violations.push('timeout');
  if (valid.signal !== null) violations.push('killed-by-signal');
  if (valid.disposition === 'aborted') violations.push('aborted');
  if (valid.counts.failed > 0 || valid.leafResults.some((leaf) => leaf.outcome === 'failed')) violations.push('failed-tests');
  if (testsRequired) {
    if (valid.disposition === 'no-source' || valid.leafResults.some((leaf) => leaf.outcome === 'no-source')) {
      violations.push('no-source-with-required-tests');
    }
    if (valid.counts.discovered === 0) violations.push('zero-tests-discovered');
    else if (valid.counts.executed === 0) violations.push('zero-tests-executed');
    else if (valid.counts.skipped >= valid.counts.discovered) violations.push('all-tests-skipped');
    if (valid.reportArtifacts.length === 0 && valid.exitCode === 0) violations.push('green-exit-without-reports');
  }
  if (valid.tier === 'certification-direct' && ['from-cache', 'up-to-date'].includes(valid.disposition)) {
    violations.push('cache-substituted-direct-tier');
  }
  if (valid.tier === 'certification-direct' &&
      valid.leafResults.some((leaf) => leaf.disposition === 'from-cache' || leaf.disposition === 'up-to-date')) {
    violations.push('cached-leaf-on-direct-tier');
  }
  if (valid.retryHistory.length > 0 && valid.counts.failed === 0 && valid.exitCode === 0) {
    violations.push('flaky-fail-then-pass');
  }
  const passed = violations.length === 0 && valid.exitCode === 0;
  return { passed, violations, receipt: valid };
}

module.exports = {
  COMMAND_DOMAIN,
  STRUCTURAL_DOMAIN,
  TestReceiptError,
  canonicalJson,
  receiptHashOf,
  validateCommandReceipt,
  validateStructuralReceipt,
  evaluateCommandReceipt
};
