'use strict';

// Exact v1 schema for manager-owned per-task worktree lifecycle records
// (pipeline improvement 01, Phase 1). One record = one execution generation of
// one `run` task. Records live in the control cache
// (orchestrator/.cache/tasks/worktrees/) and are the ONLY durable authority on
// which worktrees the manager owns: discovery classifies a checkout as managed
// exclusively by an exact record match, never by path shape or age. The
// contract is pure (no fs/git I/O) — bounded reading and identity re-checks
// belong to the manager; this module owns shape, grammar and the record hash.
//
// No extension fields: schemaVersion 1 accepts exactly these fields, and any
// future capability/field addition is a schema revision, not a tolerated
// extra. The status enum deliberately carries the FULL lifecycle from the plan
// (§9/§10/§20) so later phases extend behavior, not the schema.

var crypto = require('crypto');
var TextDecoder = require('util').TextDecoder;

var VERSION = 1;
var MAX_BYTES = 64 * 1024;

var FIELDS = ['version', 'worktreeId', 'runId', 'requestId', 'stem', 'status',
  'controlProjectId', 'gitCommonDirIdentity', 'controlRoot', 'executionRoot',
  'targetRef', 'candidateRef', 'baseCommit', 'baseTree',
  'taskState', 'taskSourceRevision', 'taskSnapshotHash', 'projectConfigHash',
  'dependencySnapshotHash', 'figmaGenerationHash', 'apiGenerationHash',
  'capabilities', 'owner', 'createdAt', 'updatedAt', 'recordHash'].sort();
var IDENTITY_FIELDS = ['path', 'dev', 'ino'].sort();
var OWNER_FIELDS = ['hostname', 'pid', 'processStartId', 'startedAt'].sort();

// Full lifecycle (plan §9.2, §9.5, §9.6, §10, §20). Phase 1 only ever
// OBSERVES records; producers arrive with Phase 2+. 'create-intent' is
// published before `git worktree add`; 'ready' only after a full post-create
// re-read; 'recovery-required' is the fail-closed crash outcome and is never
// left implicit.
// 'integrating' and 'completed' were reserved for a design the transaction did
// not take: the integration WAL carries its own status, and a generation is
// either materialized or released. Nothing ever produced them, so the filters
// that read them were unreachable — a closed enum must describe the states that
// exist, not the ones that were once imagined.
var STATUSES = new Set(['create-intent', 'ready', 'sealing', 'ready-for-integration',
  'revalidation-required', 'recovery-required', 'releasing', 'released']);
// Statuses whose execution root must already exist on disk (identity pinned).
var MATERIALIZED_STATUSES = new Set(['ready', 'sealing', 'ready-for-integration',
  'revalidation-required']);
// Ownership and execution admission are deliberately different questions.
// Recovery/release records may still own an exact checkout while no new child
// is allowed to bind to it. Both discovery and cleanup consume these shared
// sets so the same physical bytes cannot be called foreign by one and owned by
// the other.
var CHECKOUT_OWNING_STATUSES = new Set(['ready', 'sealing', 'ready-for-integration',
  'revalidation-required', 'recovery-required', 'releasing']);
var RELEASABLE_STATUSES = new Set(['ready', 'ready-for-integration',
  'revalidation-required', 'recovery-required', 'releasing']);

var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var STEM_RE = /^TASK_([1-9][0-9]{0,15})_[A-Za-z0-9_]{1,120}$/;
var WORKTREE_ID_RE = /^wt-[a-f0-9]{32}$/;
// Request ids name HTTP queue records. Execution ids name a worktree/lock/test
// generation and use the certification namespace. Accept the legacy unprefixed
// execution form for already-published records, while keeping request ids
// strictly in their original namespace.
var REQUEST_ID_RE = /^[0-9]{1,16}-[a-z0-9]{1,32}$/;
var RUN_ID_RE = /^(?:run-)?[0-9]{1,16}-[a-z0-9]{1,32}$/;
var COMMIT_RE = /^[a-f0-9]{40}$/; // SHA-1 object format only; SHA-256 repos are a typed unsupported state upstream
var PROCESS_START_ID_RE = /^psid-v1:[a-z0-9-]+:[a-f0-9]{16,128}$/;
var UNSIGNED_DECIMAL_RE = /^(?:0|[1-9][0-9]{0,19})$/;
// Manager-generated candidate refs only (plan §7.3): no raw titles, bounded
// run-id component. The target must be a plain local branch head.
var CANDIDATE_REF_RE = /^refs\/heads\/orchestrator\/task\/TASK_[1-9][0-9]{0,15}-[a-f0-9]{12}\/[a-z0-9]{1,32}$/;
// Static mirror of the git ref-name rules this contract can check without
// spawning git (the manager still re-proves via `git check-ref-format`):
// printable ASCII/UTF-8 only, no space, no git-special characters, no '..',
// '@{' or '//', per-component dot/.lock rules, bounded length, and always
// outside the manager candidate namespace.
function targetRefValid(value) {
  if (typeof value !== 'string') return false;
  if (value.length < 'refs/heads/x'.length || value.length > 220) return false;
  if (value.indexOf('refs/heads/') !== 0) return false;
  var name = value.slice('refs/heads/'.length);
  if (!name || name.indexOf('orchestrator/task/') === 0) return false;
  if (name === 'HEAD') return false; // git check-ref-format --branch refuses the reserved name
  if (/[\x00-\x20~^:?*\\[\]\x7f]/.test(name)) return false;
  if (name.indexOf('..') >= 0 || name.indexOf('@{') >= 0 || name.indexOf('//') >= 0) return false;
  if (name[0] === '/' || name.slice(-1) === '/' || name[0] === '-') return false;
  return name.split('/').every(function (component) {
    return component.length > 0 && component[0] !== '.' &&
      component.slice(-1) !== '.' && component.slice(-5) !== '.lock';
  });
}

function iso(value) {
  if (typeof value !== 'string') return false;
  var match = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})\.([0-9]{3})Z$/.exec(value);
  if (!match) return false;
  var parsed = new Date(value);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString() === value;
}
function sameKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.join('\0');
}
function canonical(value) {
  // Hash-domain hardening: undefined and non-finite numbers stringify to
  // forms that collide with [] / null. No v1 field can carry them, but the
  // digest must stay injective for every input it is ever handed.
  if (value === undefined) fail('canonical form is undefined');
  if (typeof value === 'number' && !Number.isFinite(value)) fail('canonical form is a non-finite number');
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']';
  if (value && typeof value === 'object') {
    return '{' + Object.keys(value).sort().map(function (key) {
      return JSON.stringify(key) + ':' + canonical(value[key]);
    }).join(',') + '}';
  }
  return JSON.stringify(value);
}
function digest(value) {
  return 'sha256:' + crypto.createHash('sha256').update(Buffer.from(canonical(value), 'utf8')).digest('hex');
}
function fail(message) {
  var error = new Error(message);
  error.code = 'WORKTREE_RECORD_INVALID';
  throw error;
}
function hashOrNull(value, label) {
  if (value !== null && (typeof value !== 'string' || !HASH_RE.test(value))) fail(label + ' must be a sha256 hash or null');
}
function exactHash(value, label) {
  if (typeof value !== 'string' || !HASH_RE.test(value)) fail(label + ' must be an exact sha256 hash');
}
function identity(value, label, nullable) {
  if (value === null) {
    if (!nullable) fail(label + ' must be a filesystem identity');
    return;
  }
  if (!sameKeys(value, IDENTITY_FIELDS)) fail(label + ' must carry exactly path/dev/ino');
  // Spaces and non-ASCII are legal path bytes (plan §7.1 requires them in
  // tests); only control characters, non-absolute, non-NFC and oversized
  // paths are rejected here.
  if (typeof value.path !== 'string' || !value.path || value.path[0] !== '/' ||
      /[\x00-\x1f\x7f]/.test(value.path) || Buffer.byteLength(value.path, 'utf8') > 4096 ||
      value.path !== value.path.normalize('NFC')) fail(label + '.path must be an absolute canonical path');
  if (value.path !== '/' && value.path.slice(-1) === '/') fail(label + '.path must not end with a slash');
  ['dev', 'ino'].forEach(function (field) {
    if (typeof value[field] !== 'string' || !UNSIGNED_DECIMAL_RE.test(value[field])) {
      fail(label + '.' + field + ' must be an unsigned decimal string');
    }
  });
}

// Validates a parsed record object (already bounded and no-follow-read by the
// caller). Returns the record on success; throws WORKTREE_RECORD_INVALID.
function validate(record) {
  if (!sameKeys(record, FIELDS)) fail('worktree record fields do not match the exact v1 schema');
  if (record.version !== VERSION) fail('worktree record version must be ' + VERSION);
  if (typeof record.worktreeId !== 'string' || !WORKTREE_ID_RE.test(record.worktreeId)) fail('worktreeId is not canonical');
  if (typeof record.runId !== 'string' || !RUN_ID_RE.test(record.runId)) fail('runId is not canonical');
  if (typeof record.requestId !== 'string' || !REQUEST_ID_RE.test(record.requestId)) fail('requestId is not canonical');
  if (typeof record.stem !== 'string' || !STEM_RE.test(record.stem)) fail('stem is not canonical');
  if (typeof record.status !== 'string' || !STATUSES.has(record.status)) fail('status is not a known lifecycle state');
  exactHash(record.controlProjectId, 'controlProjectId');
  identity(record.gitCommonDirIdentity, 'gitCommonDirIdentity', false);
  identity(record.controlRoot, 'controlRoot', false);
  // identity() itself refuses null when the status requires a materialized
  // checkout, so the coupling lives in one place.
  identity(record.executionRoot, 'executionRoot', !MATERIALIZED_STATUSES.has(record.status));
  if (record.executionRoot !== null && record.controlRoot !== null &&
      record.executionRoot.path === record.controlRoot.path) {
    fail('execution root must never alias the control root');
  }
  if (!targetRefValid(record.targetRef)) fail('targetRef must be a plain local branch outside the manager namespace');
  if (typeof record.candidateRef !== 'string' || !CANDIDATE_REF_RE.test(record.candidateRef)) fail('candidateRef must be a manager-generated namespaced ref');
  var taskNumber = STEM_RE.exec(record.stem)[1];
  if (record.candidateRef.indexOf('refs/heads/orchestrator/task/TASK_' + taskNumber + '-') !== 0) {
    fail('candidateRef must embed the record task number');
  }
  if (typeof record.baseCommit !== 'string' || !COMMIT_RE.test(record.baseCommit)) fail('baseCommit must be a full commit id');
  if (typeof record.baseTree !== 'string' || !COMMIT_RE.test(record.baseTree)) fail('baseTree must be a full tree id');
  if (record.taskState !== 'todo') fail('worktrees are created only for run tasks in todo');
  exactHash(record.taskSourceRevision, 'taskSourceRevision');
  exactHash(record.taskSnapshotHash, 'taskSnapshotHash');
  exactHash(record.projectConfigHash, 'projectConfigHash');
  exactHash(record.dependencySnapshotHash, 'dependencySnapshotHash');
  hashOrNull(record.figmaGenerationHash, 'figmaGenerationHash');
  hashOrNull(record.apiGenerationHash, 'apiGenerationHash');
  // v1 grants children no control capabilities at all; widening this is a
  // schema revision, not a data change.
  if (!Array.isArray(record.capabilities) || record.capabilities.length !== 0) {
    fail('capabilities must be the empty v1 allowlist');
  }
  if (!sameKeys(record.owner, OWNER_FIELDS)) fail('owner must carry exactly hostname/pid/processStartId/startedAt');
  if (typeof record.owner.hostname !== 'string' || !record.owner.hostname ||
      Buffer.byteLength(record.owner.hostname, 'utf8') > 255) fail('owner.hostname is not canonical');
  if (!Number.isSafeInteger(record.owner.pid) || record.owner.pid <= 0) fail('owner.pid must be a positive integer');
  if (record.owner.processStartId !== null && (typeof record.owner.processStartId !== 'string' ||
      !PROCESS_START_ID_RE.test(record.owner.processStartId))) fail('owner.processStartId is not canonical');
  if (!iso(record.owner.startedAt)) fail('owner.startedAt must be an exact UTC timestamp');
  if (!iso(record.createdAt)) fail('createdAt must be an exact UTC timestamp');
  if (!iso(record.updatedAt)) fail('updatedAt must be an exact UTC timestamp');
  if (Date.parse(record.updatedAt) < Date.parse(record.createdAt)) fail('updatedAt must not precede createdAt');
  exactHash(record.recordHash, 'recordHash');
  if (record.recordHash !== recordHash(record)) fail('recordHash does not match the canonical record content');
  return record;
}

// Hash binds every field except recordHash itself over canonical JSON.
function recordHash(record) {
  var copy = {};
  Object.keys(record).sort().forEach(function (key) {
    if (key !== 'recordHash') copy[key] = record[key];
  });
  return digest(copy);
}

// Validates raw bytes: size bound + strict JSON parse + validate().
function validateBytes(bytes) {
  if (!Buffer.isBuffer(bytes)) fail('worktree record bytes must be a Buffer');
  if (bytes.length === 0 || bytes.length > MAX_BYTES) fail('worktree record exceeds the bounded size');
  var parsed, text;
  try { text = new TextDecoder('utf-8', { fatal: true }).decode(bytes); }
  catch (error) { fail('worktree record is not valid UTF-8'); }
  try { parsed = JSON.parse(text); }
  catch (error) { fail('worktree record is not valid JSON'); }
  return validate(parsed);
}

module.exports = {
  VERSION: VERSION,
  MAX_BYTES: MAX_BYTES,
  FIELDS: FIELDS,
  STATUSES: STATUSES,
  STEM_RE: STEM_RE,
  MATERIALIZED_STATUSES: MATERIALIZED_STATUSES,
  CHECKOUT_OWNING_STATUSES: CHECKOUT_OWNING_STATUSES,
  RELEASABLE_STATUSES: RELEASABLE_STATUSES,
  WORKTREE_ID_RE: WORKTREE_ID_RE,
  RUN_ID_RE: RUN_ID_RE,
  REQUEST_ID_RE: REQUEST_ID_RE,
  CANDIDATE_REF_RE: CANDIDATE_REF_RE,
  targetRefValid: targetRefValid,
  COMMIT_RE: COMMIT_RE,
  validate: validate,
  validateBytes: validateBytes,
  recordHash: recordHash,
  canonical: canonical,
  digest: digest
};
