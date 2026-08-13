'use strict';

// Exact v1 schema for the integration WAL (pipeline improvement 01, §10.3).
// One record = one attempt to move one sealed candidate into the canonical
// target branch as one commit. Records live in the control cache
// (orchestrator/.cache/tasks/integrations/). Phase 1 ships the schema and
// read-only validation only; the producer (Integrate preview/confirm) arrives
// with Phase 4. Every phase writes its INTENT before its effect, and recovery
// never rolls back: a mismatch between recorded intent and physical state is
// 'recovery-required', not a guess.
//
// No extension fields; any addition is a schema revision.

var worktreeContract = require('./worktree-record-contract.cjs');
var TextDecoder = require('util').TextDecoder;

var VERSION = 1;
var MAX_BYTES = 256 * 1024;

// §10.3 phase order. 'prepared' is the entry phase; 'completed' releases
// lock/markers and allows cleanup. Order is load-bearing: phase N may carry
// an intent only when phase N-1 is proven.
var PHASES = ['prepared', 'product-applying', 'product-applied',
  'finalizer-preparing', 'finalizer-prepared', 'commit-publishing',
  'commit-published', 'finalizer-confirming', 'completed'];

var FIELDS = ['version', 'integrationId', 'stem', 'runId', 'worktreeId', 'phase',
  'status', 'candidate', 'target', 'controlSnapshot', 'commitPin',
  'finalizerPrepared', 'phases', 'owner', 'createdAt', 'updatedAt', 'recordHash'].sort();
var CANDIDATE_FIELDS = ['commit', 'tree', 'diffHash', 'receiptHash'].sort();
var TARGET_FIELDS = ['ref', 'baseCommit', 'baseTree'].sort();
var CONTROL_SNAPSHOT_FIELDS = ['headCommit', 'dirtyAllowedPaths'].sort();
var COMMIT_PIN_FIELDS = ['stagedTreeHash', 'messageHash', 'expectedParent', 'publishedCommit'].sort();
var PHASE_MARK_FIELDS = ['intentAt', 'provenAt'].sort();
var PATH_PIN_FIELDS = ['path', 'hash'].sort();
var OWNER_FIELDS = ['hostname', 'pid', 'processStartId', 'startedAt'].sort();

// 'abandoned' is the ONLY terminal state an operator can reach by hand, and it
// records a decision rather than an effect: the evidence stays on disk, the
// commit (if any) stays published, and the record stops holding the
// repository-wide mutex. It is never reached automatically.
var STATUSES = new Set(['active', 'completed', 'recovery-required', 'abandoned']);
var INTEGRATION_ID_RE = /^ig-[a-f0-9]{32}$/;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var MAX_DIRTY_ALLOWED = 200;
var MAX_PREPARED_PATHS = 500;

function fail(message) {
  var error = new Error(message);
  error.code = 'INTEGRATION_RECORD_INVALID';
  throw error;
}
function sameKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.join('\0');
}
function iso(value) {
  if (typeof value !== 'string') return false;
  if (!/^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3}Z$/.test(value)) return false;
  var parsed = new Date(value);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString() === value;
}
function exactHash(value, label) {
  if (typeof value !== 'string' || !HASH_RE.test(value)) fail(label + ' must be an exact sha256 hash');
}
function hashOrNull(value, label) {
  if (value !== null && (typeof value !== 'string' || !HASH_RE.test(value))) fail(label + ' must be a sha256 hash or null');
}
function commitOrNull(value, label) {
  if (value !== null && (typeof value !== 'string' || !worktreeContract.COMMIT_RE.test(value))) {
    fail(label + ' must be a full commit id or null');
  }
}
// Repo-relative product path: never absolute, never escaping, no control
// bytes, NFC, bounded. Spaces/Unicode are legal (plan §7.1/§21).
function repoRelativePath(value, label) {
  if (typeof value !== 'string' || !value || value[0] === '/' || value.slice(-1) === '/' ||
      Buffer.byteLength(value, 'utf8') > 4096 || /[\x00-\x1f\x7f]/.test(value) ||
      value !== value.normalize('NFC')) fail(label + ' must be a bounded repo-relative path');
  var components = value.split('/');
  if (components.some(function (component) {
    return component === '' || component === '.' || component === '..';
  })) fail(label + ' must not contain empty, dot or dot-dot components');
}
function pathPins(value, label, max) {
  if (!Array.isArray(value)) fail(label + ' must be an array of path pins');
  if (value.length > max) fail(label + ' exceeds the bounded entry count');
  var seen = new Set();
  value.forEach(function (entry, index) {
    if (!sameKeys(entry, PATH_PIN_FIELDS)) fail(label + '[' + index + '] must carry exactly path/hash');
    repoRelativePath(entry.path, label + '[' + index + '].path');
    exactHash(entry.hash, label + '[' + index + '].hash');
    if (seen.has(entry.path)) fail(label + ' must not repeat a path');
    seen.add(entry.path);
  });
}

function validate(record) {
  if (!sameKeys(record, FIELDS)) fail('integration record fields do not match the exact v1 schema');
  if (record.version !== VERSION) fail('integration record version must be ' + VERSION);
  if (typeof record.integrationId !== 'string' || !INTEGRATION_ID_RE.test(record.integrationId)) fail('integrationId is not canonical');
  if (typeof record.stem !== 'string' || !/^TASK_[1-9][0-9]{0,15}_[A-Za-z0-9_]{1,120}$/.test(record.stem)) fail('stem is not canonical');
  if (typeof record.runId !== 'string' || !worktreeContract.RUN_ID_RE.test(record.runId)) fail('runId is not canonical');
  if (typeof record.worktreeId !== 'string' || !worktreeContract.WORKTREE_ID_RE.test(record.worktreeId)) fail('worktreeId is not canonical');
  if (PHASES.indexOf(record.phase) < 0) fail('phase is not a known WAL phase');
  if (typeof record.status !== 'string' || !STATUSES.has(record.status)) fail('status is not a known WAL status');

  if (!sameKeys(record.candidate, CANDIDATE_FIELDS)) fail('candidate must carry exactly commit/tree/diffHash/receiptHash');
  if (!worktreeContract.COMMIT_RE.test(String(record.candidate.commit || ''))) fail('candidate.commit must be a full commit id');
  if (!worktreeContract.COMMIT_RE.test(String(record.candidate.tree || ''))) fail('candidate.tree must be a full tree id');
  exactHash(record.candidate.diffHash, 'candidate.diffHash');
  exactHash(record.candidate.receiptHash, 'candidate.receiptHash');

  if (!sameKeys(record.target, TARGET_FIELDS)) fail('target must carry exactly ref/baseCommit/baseTree');
  if (!worktreeContract.targetRefValid(record.target.ref)) fail('target.ref must be a plain local branch outside the manager namespace');
  if (!worktreeContract.COMMIT_RE.test(String(record.target.baseCommit || ''))) fail('target.baseCommit must be a full commit id');
  if (!worktreeContract.COMMIT_RE.test(String(record.target.baseTree || ''))) fail('target.baseTree must be a full tree id');

  if (!sameKeys(record.controlSnapshot, CONTROL_SNAPSHOT_FIELDS)) fail('controlSnapshot must carry exactly headCommit/dirtyAllowedPaths');
  if (!worktreeContract.COMMIT_RE.test(String(record.controlSnapshot.headCommit || ''))) fail('controlSnapshot.headCommit must be a full commit id');
  if (record.controlSnapshot.headCommit !== record.target.baseCommit) {
    fail('controlSnapshot.headCommit must equal target.baseCommit (candidate base = current target HEAD)');
  }
  pathPins(record.controlSnapshot.dirtyAllowedPaths, 'controlSnapshot.dirtyAllowedPaths', MAX_DIRTY_ALLOWED);

  if (!sameKeys(record.commitPin, COMMIT_PIN_FIELDS)) fail('commitPin must carry exactly stagedTreeHash/messageHash/expectedParent/publishedCommit');
  hashOrNull(record.commitPin.stagedTreeHash, 'commitPin.stagedTreeHash');
  hashOrNull(record.commitPin.messageHash, 'commitPin.messageHash');
  commitOrNull(record.commitPin.expectedParent, 'commitPin.expectedParent');
  commitOrNull(record.commitPin.publishedCommit, 'commitPin.publishedCommit');
  if (record.commitPin.expectedParent !== null && record.commitPin.expectedParent !== record.target.baseCommit) {
    fail('commitPin.expectedParent must equal target.baseCommit (one canonical commit on the exact base)');
  }

  if (record.finalizerPrepared !== null) pathPins(record.finalizerPrepared, 'finalizerPrepared', MAX_PREPARED_PATHS);

  // Phase lattice: intent-before-effect, strictly ordered. Phase K may carry
  // an intent only when phase K-1 is proven; the top-level `phase` names the
  // furthest phase with an intent; proofs never exist without their intent.
  if (!sameKeys(record.phases, PHASES.slice().sort())) fail('phases must carry exactly the nine WAL phases');
  var furthestIntent = -1;
  PHASES.forEach(function (name, index) {
    var mark = record.phases[name];
    if (!sameKeys(mark, PHASE_MARK_FIELDS)) fail('phases.' + name + ' must carry exactly intentAt/provenAt');
    if (mark.intentAt !== null && !iso(mark.intentAt)) fail('phases.' + name + '.intentAt must be an exact UTC timestamp or null');
    if (mark.provenAt !== null && !iso(mark.provenAt)) fail('phases.' + name + '.provenAt must be an exact UTC timestamp or null');
    if (mark.provenAt !== null && mark.intentAt === null) fail('phases.' + name + ' proof requires a recorded intent');
    if (mark.provenAt !== null && Date.parse(mark.provenAt) < Date.parse(mark.intentAt)) {
      fail('phases.' + name + ' proof must not precede its intent');
    }
    if (mark.intentAt !== null) {
      if (index > 0 && record.phases[PHASES[index - 1]].provenAt === null) {
        fail('phases.' + name + ' intent requires the previous phase to be proven');
      }
      furthestIntent = index;
    }
  });
  if (furthestIntent < 0) fail('the WAL must carry at least the prepared intent');
  if (record.phase !== PHASES[furthestIntent]) fail('phase must name the furthest phase with a recorded intent');
  if (record.status === 'completed' &&
      (record.phase !== 'completed' || record.phases.completed.provenAt === null)) {
    fail('completed status requires the completed phase to be proven');
  }
  // Effects recorded out of order are recovery evidence, not tolerated drift.
  if (record.commitPin.publishedCommit !== null && record.phases['commit-publishing'].intentAt === null) {
    fail('a published commit requires the commit-publishing intent');
  }
  // §10.3 phase 6: the commit-publishing intent IS the exact staged tree,
  // message and parent — an intent without those pins proves nothing.
  if (record.phases['commit-publishing'].intentAt !== null &&
      (record.commitPin.stagedTreeHash === null || record.commitPin.messageHash === null ||
       record.commitPin.expectedParent === null)) {
    fail('commit-publishing intent requires exact stagedTreeHash/messageHash/expectedParent pins');
  }
  // §10.3 phase 7: a proven commit-published phase must name the commit it proved.
  if (record.phases['commit-published'].provenAt !== null && record.commitPin.publishedCommit === null) {
    fail('commit-published proof requires the published commit pin');
  }
  if (record.phases['finalizer-prepared'].provenAt !== null && record.finalizerPrepared === null) {
    fail('finalizer-prepared proof requires the prepared path pins');
  }
  // A proven completed phase with a still-'active' status would mean the lock
  // was released while the WAL claims to be mid-flight.
  if (record.phases.completed.provenAt !== null && record.status === 'active') {
    fail('a proven completed phase is incompatible with active status');
  }

  if (!sameKeys(record.owner, OWNER_FIELDS)) fail('owner must carry exactly hostname/pid/processStartId/startedAt');
  if (typeof record.owner.hostname !== 'string' || !record.owner.hostname ||
      Buffer.byteLength(record.owner.hostname, 'utf8') > 255) fail('owner.hostname is not canonical');
  if (!Number.isSafeInteger(record.owner.pid) || record.owner.pid <= 0) fail('owner.pid must be a positive integer');
  if (record.owner.processStartId !== null && (typeof record.owner.processStartId !== 'string' ||
      !/^psid-v1:[a-z0-9-]+:[a-f0-9]{16,128}$/.test(record.owner.processStartId))) fail('owner.processStartId is not canonical');
  if (!iso(record.owner.startedAt)) fail('owner.startedAt must be an exact UTC timestamp');
  if (!iso(record.createdAt)) fail('createdAt must be an exact UTC timestamp');
  if (!iso(record.updatedAt)) fail('updatedAt must be an exact UTC timestamp');
  if (Date.parse(record.updatedAt) < Date.parse(record.createdAt)) fail('updatedAt must not precede createdAt');
  exactHash(record.recordHash, 'recordHash');
  if (record.recordHash !== recordHash(record)) fail('recordHash does not match the canonical record content');
  return record;
}

function recordHash(record) {
  var copy = {};
  Object.keys(record).sort().forEach(function (key) {
    if (key !== 'recordHash') copy[key] = record[key];
  });
  return worktreeContract.digest(copy);
}

function validateBytes(bytes) {
  if (!Buffer.isBuffer(bytes)) fail('integration record bytes must be a Buffer');
  if (bytes.length === 0 || bytes.length > MAX_BYTES) fail('integration record exceeds the bounded size');
  var parsed, text;
  try { text = new TextDecoder('utf-8', { fatal: true }).decode(bytes); }
  catch (error) { fail('integration record is not valid UTF-8'); }
  try { parsed = JSON.parse(text); }
  catch (error) { fail('integration record is not valid JSON'); }
  return validate(parsed);
}

module.exports = {
  VERSION: VERSION,
  MAX_BYTES: MAX_BYTES,
  FIELDS: FIELDS,
  PHASES: PHASES,
  STATUSES: STATUSES,
  INTEGRATION_ID_RE: INTEGRATION_ID_RE,
  validate: validate,
  validateBytes: validateBytes,
  recordHash: recordHash
};
