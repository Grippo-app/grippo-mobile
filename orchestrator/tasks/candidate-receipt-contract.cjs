'use strict';

// Exact v1 schema for the candidate receipt (pipeline improvement 01, §9.5).
// One receipt = one sealed candidate: the exact Git trees a task produced and
// the canonical, byte-precise manifest of what changed between them.
//
// This is what REPLACES the old footprint mechanic. `comm -13` over two
// `git status` snapshots compared PATH SETS: a file already modified before
// the run and modified again by the task appeared in both snapshots and fell
// out of the footprint entirely. A receipt compares TREES, so the same path
// carries its exact old and new blob ids and cannot vanish; renames, mode
// changes, deletions and symlinks are first-class entries rather than
// inferred from names.
//
// Pure contract: no fs/git I/O. Sealing lives in the Git mutation owner and
// hands its output here for validation and hashing.

var crypto = require('crypto');
var TextDecoder = require('util').TextDecoder;
var worktreeContract = require('./worktree-record-contract.cjs');

var VERSION = 1;
var MAX_BYTES = 4 * 1024 * 1024;
var MAX_ENTRIES = 5000;

var FIELDS = ['version', 'worktreeId', 'runId', 'stem', 'candidateRef',
  'baseCommit', 'baseTree', 'expectedRefCommit', 'candidateCommit', 'candidateTree',
  'entries', 'diffHash', 'inputs', 'sealedAt', 'owner', 'receiptHash'].sort();
var ENTRY_FIELDS = ['path', 'operation', 'oldMode', 'newMode', 'oldBlob', 'newBlob', 'renameFrom'].sort();
var INPUT_FIELDS = ['taskSnapshotHash', 'projectConfigHash', 'dependencySnapshotHash',
  'targetRef', 'targetCommit', 'figmaGenerationHash', 'apiGenerationHash'].sort();
var OWNER_FIELDS = ['hostname', 'pid', 'processStartId', 'startedAt'].sort();

var OPERATIONS = new Set(['add', 'modify', 'delete', 'rename', 'mode']);
// Git object modes a product deliverable may carry. Gitlinks (160000) are a
// typed unsupported state (§18) and never appear in a candidate.
var MODES = new Set(['100644', '100755', '120000']);
var NULL_BLOB = '0'.repeat(40);
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var BLOB_RE = /^[a-f0-9]{40}$/;

// Control-owned prefixes a product candidate may never contain (§12.1). The
// task board, INDEX, architecture map, registries, caches, runtime skills and
// pipeline code stay control-owned; so do agent instructions and CI wiring.
var FORBIDDEN_PREFIXES = ['orchestrator/', '.claude/', '.github/'];
var FORBIDDEN_EXACT = new Set(['.gitignore', '.gitattributes', '.gitmodules']);

function fail(message) {
  var error = new Error(message);
  error.code = 'CANDIDATE_RECEIPT_INVALID';
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
// Repo-relative product path: bounded, NFC, no control bytes, no traversal.
// Spaces and non-ASCII are legal (plan §7.1/§21).
function productPath(value, label) {
  if (typeof value !== 'string' || !value || value[0] === '/' || value.slice(-1) === '/' ||
      Buffer.byteLength(value, 'utf8') > 4096 || /[\x00-\x1f\x7f]/.test(value) ||
      value !== value.normalize('NFC')) fail(label + ' must be a bounded repo-relative path');
  if (value.split('/').some(function (component) {
    return component === '' || component === '.' || component === '..';
  })) fail(label + ' must not contain empty, dot or dot-dot components');
  if (FORBIDDEN_EXACT.has(value) ||
      FORBIDDEN_PREFIXES.some(function (prefix) { return value.indexOf(prefix) === 0; }) ||
      value === '.git' || value.indexOf('.git/') === 0) {
    fail(label + ' is control-owned and can never be part of a product candidate');
  }
}
function blobOrNull(value, label, nullable) {
  if (value === null) {
    if (!nullable) fail(label + ' must be a blob id');
    return;
  }
  if (typeof value !== 'string' || !BLOB_RE.test(value) || value === NULL_BLOB) fail(label + ' must be a real blob id');
}
function modeOrNull(value, label, nullable) {
  if (value === null) {
    if (!nullable) fail(label + ' must be a file mode');
    return;
  }
  if (typeof value !== 'string' || !MODES.has(value)) fail(label + ' must be a supported file mode');
}

function validateEntry(entry, index) {
  var label = 'entries[' + index + ']';
  if (!sameKeys(entry, ENTRY_FIELDS)) fail(label + ' must carry exactly the v1 entry fields');
  productPath(entry.path, label + '.path');
  if (!OPERATIONS.has(entry.operation)) fail(label + '.operation is not a known operation');
  if (entry.operation === 'add') {
    modeOrNull(entry.oldMode, label + '.oldMode', true);
    if (entry.oldMode !== null || entry.oldBlob !== null) fail(label + ' add must have no old side');
    modeOrNull(entry.newMode, label + '.newMode', false);
    blobOrNull(entry.newBlob, label + '.newBlob', false);
    if (entry.renameFrom !== null) fail(label + ' add must not carry a rename source');
  } else if (entry.operation === 'delete') {
    modeOrNull(entry.oldMode, label + '.oldMode', false);
    blobOrNull(entry.oldBlob, label + '.oldBlob', false);
    if (entry.newMode !== null || entry.newBlob !== null) fail(label + ' delete must have no new side');
    if (entry.renameFrom !== null) fail(label + ' delete must not carry a rename source');
  } else if (entry.operation === 'modify' || entry.operation === 'mode' || entry.operation === 'rename') {
    modeOrNull(entry.oldMode, label + '.oldMode', false);
    modeOrNull(entry.newMode, label + '.newMode', false);
    blobOrNull(entry.oldBlob, label + '.oldBlob', false);
    blobOrNull(entry.newBlob, label + '.newBlob', false);
    if (entry.operation === 'modify' && entry.oldBlob === entry.newBlob) {
      fail(label + ' modify must change the blob');
    }
    if (entry.operation === 'mode') {
      if (entry.oldBlob !== entry.newBlob) fail(label + ' mode must keep the same blob');
      if (entry.oldMode === entry.newMode) fail(label + ' mode must change the mode');
    }
    if (entry.operation === 'rename') {
      if (entry.renameFrom === null) fail(label + ' rename requires its source path');
      productPath(entry.renameFrom, label + '.renameFrom');
      if (entry.renameFrom === entry.path) fail(label + ' rename source and destination must differ');
    } else if (entry.renameFrom !== null) {
      fail(label + ' only a rename may carry a rename source');
    }
  }
}

// The diff hash lives in its own literal domain so it can never be compared
// against a content-snapshot, task-state or project-source-revision hash.
function diffHashOf(entries) {
  var canonical = entries.map(function (entry) {
    return [entry.operation, entry.path, entry.renameFrom || '', entry.oldMode || '',
      entry.newMode || '', entry.oldBlob || '', entry.newBlob || ''].join('\0');
  }).join('\u0001');
  return 'sha256:' + crypto.createHash('sha256')
    .update('candidate-diff-v1\u0000', 'utf8').update(canonical, 'utf8').digest('hex');
}

function validate(receipt) {
  if (!sameKeys(receipt, FIELDS)) fail('candidate receipt fields do not match the exact v1 schema');
  if (receipt.version !== VERSION) fail('candidate receipt version must be ' + VERSION);
  if (typeof receipt.worktreeId !== 'string' || !worktreeContract.WORKTREE_ID_RE.test(receipt.worktreeId)) fail('worktreeId is not canonical');
  if (typeof receipt.runId !== 'string' || !worktreeContract.RUN_ID_RE.test(receipt.runId)) fail('runId is not canonical');
  if (typeof receipt.stem !== 'string' || !/^TASK_[1-9][0-9]{0,15}_[A-Za-z0-9_]{1,120}$/.test(receipt.stem)) fail('stem is not canonical');
  if (typeof receipt.candidateRef !== 'string' || !worktreeContract.CANDIDATE_REF_RE.test(receipt.candidateRef)) fail('candidateRef must be a manager-generated ref');
  ['baseCommit', 'baseTree', 'expectedRefCommit', 'candidateCommit', 'candidateTree'].forEach(function (field) {
    if (typeof receipt[field] !== 'string' || !worktreeContract.COMMIT_RE.test(receipt[field])) fail(field + ' must be a full object id');
  });
  if (receipt.candidateCommit === receipt.baseCommit) fail('a candidate commit must differ from its base');
  if (receipt.candidateCommit === receipt.expectedRefCommit) {
    fail('candidateCommit must differ from the ref generation it replaces');
  }

  if (!Array.isArray(receipt.entries)) fail('entries must be an array');
  if (receipt.entries.length === 0) fail('a candidate must change at least one product path');
  if (receipt.entries.length > MAX_ENTRIES) fail('entries exceed the bounded count');
  var seen = new Set();
  var seenSources = new Set();
  receipt.entries.forEach(function (entry, index) {
    validateEntry(entry, index);
    if (seen.has(entry.path)) fail('entries must not repeat a destination path');
    seen.add(entry.path);
    if (entry.renameFrom !== null) {
      if (seenSources.has(entry.renameFrom)) fail('entries must not repeat a rename source');
      seenSources.add(entry.renameFrom);
    }
  });
  // Entries are canonically ordered so two byte-identical candidates always
  // hash identically regardless of how git enumerated them.
  var ordered = receipt.entries.map(function (entry) { return entry.path; });
  var sorted = ordered.slice().sort();
  if (ordered.join('\0') !== sorted.join('\0')) fail('entries must be sorted by destination path');
  if (receipt.diffHash !== diffHashOf(receipt.entries)) fail('diffHash does not match the canonical entry manifest');

  if (!sameKeys(receipt.inputs, INPUT_FIELDS)) fail('inputs must carry exactly the v1 input pins');
  if (typeof receipt.inputs.taskSnapshotHash !== 'string' || !HASH_RE.test(receipt.inputs.taskSnapshotHash)) fail('inputs.taskSnapshotHash must be a sha256 hash');
  if (typeof receipt.inputs.projectConfigHash !== 'string' || !HASH_RE.test(receipt.inputs.projectConfigHash)) fail('inputs.projectConfigHash must be a sha256 hash');
  if (typeof receipt.inputs.dependencySnapshotHash !== 'string' || !HASH_RE.test(receipt.inputs.dependencySnapshotHash)) fail('inputs.dependencySnapshotHash must be a sha256 hash');
  if (!worktreeContract.targetRefValid(receipt.inputs.targetRef)) fail('inputs.targetRef must be a plain local branch');
  if (typeof receipt.inputs.targetCommit !== 'string' || !worktreeContract.COMMIT_RE.test(receipt.inputs.targetCommit)) fail('inputs.targetCommit must be a full commit id');
  ['figmaGenerationHash', 'apiGenerationHash'].forEach(function (field) {
    var value = receipt.inputs[field];
    if (value !== null && (typeof value !== 'string' || !HASH_RE.test(value))) fail('inputs.' + field + ' must be a sha256 hash or null');
  });
  // A receipt sealed against a target that already moved is stale by
  // construction: gates were green for a base that is no longer current.
  if (receipt.inputs.targetCommit !== receipt.baseCommit) {
    fail('inputs.targetCommit must equal baseCommit — a moved target invalidates the candidate');
  }

  if (!sameKeys(receipt.owner, OWNER_FIELDS)) fail('owner must carry exactly hostname/pid/processStartId/startedAt');
  if (typeof receipt.owner.hostname !== 'string' || !receipt.owner.hostname ||
      Buffer.byteLength(receipt.owner.hostname, 'utf8') > 255) fail('owner.hostname is not canonical');
  if (!Number.isSafeInteger(receipt.owner.pid) || receipt.owner.pid <= 0) fail('owner.pid must be a positive integer');
  if (receipt.owner.processStartId !== null && (typeof receipt.owner.processStartId !== 'string' ||
      !/^psid-v1:[a-z0-9-]+:[a-f0-9]{16,128}$/.test(receipt.owner.processStartId))) fail('owner.processStartId is not canonical');
  if (!iso(receipt.owner.startedAt)) fail('owner.startedAt must be an exact UTC timestamp');
  if (!iso(receipt.sealedAt)) fail('sealedAt must be an exact UTC timestamp');
  if (typeof receipt.receiptHash !== 'string' || !HASH_RE.test(receipt.receiptHash)) fail('receiptHash must be a sha256 hash');
  if (receipt.receiptHash !== receiptHash(receipt)) fail('receiptHash does not match the canonical receipt content');
  return receipt;
}

function receiptHash(receipt) {
  var copy = {};
  Object.keys(receipt).sort().forEach(function (key) {
    if (key !== 'receiptHash') copy[key] = receipt[key];
  });
  return worktreeContract.digest(copy);
}

function validateBytes(bytes) {
  if (!Buffer.isBuffer(bytes)) fail('candidate receipt bytes must be a Buffer');
  if (bytes.length === 0 || bytes.length > MAX_BYTES) fail('candidate receipt exceeds the bounded size');
  var parsed, text;
  try { text = new TextDecoder('utf-8', { fatal: true }).decode(bytes); }
  catch (error) { fail('candidate receipt is not valid UTF-8'); }
  try { parsed = JSON.parse(text); }
  catch (error) { fail('candidate receipt is not valid JSON'); }
  return validate(parsed);
}

// A path the CHILD wrote where it must not (control-owned territory). Kept
// separate from grammar failures so the sealer can report the right thing.
function controlOwnedPath(value) {
  if (typeof value !== 'string' || !value) return false;
  return FORBIDDEN_EXACT.has(value) ||
    FORBIDDEN_PREFIXES.some(function (prefix) { return value.indexOf(prefix) === 0; }) ||
    value === '.git' || value.indexOf('.git/') === 0;
}

// Predicate used by the sealer BEFORE staging: a path the candidate must
// never contain (control-owned OR unrepresentable). Kept here so the block
// list has exactly one definition.
function forbiddenProductPath(value) {
  try { productPath(value, 'path'); return false; }
  catch (error) { return true; }
}

// Grammar ONLY — no ownership clause. The integration transaction stages both
// halves of one commit: the candidate's product paths and the finalizer's
// control-owned artifacts. Judging the second half with the product block list
// would reject every legitimate `orchestrator/**` artifact, so ownership is
// asserted separately by whichever side owns that path set.
function unrepresentablePath(value) {
  if (typeof value !== 'string' || !value || value[0] === '/' || value.slice(-1) === '/' ||
      Buffer.byteLength(value, 'utf8') > 4096 || /[\x00-\x1f\x7f]/.test(value) ||
      value !== value.normalize('NFC')) return true;
  return value.split('/').some(function (component) {
    return component === '' || component === '.' || component === '..';
  });
}

module.exports = {
  VERSION: VERSION,
  controlOwnedPath: controlOwnedPath,
  MAX_BYTES: MAX_BYTES,
  MAX_ENTRIES: MAX_ENTRIES,
  FIELDS: FIELDS,
  MODES: MODES,
  validate: validate,
  validateBytes: validateBytes,
  receiptHash: receiptHash,
  diffHashOf: diffHashOf,
  forbiddenProductPath: forbiddenProductPath,
  unrepresentablePath: unrepresentablePath
};
