'use strict';

// Exact read-only mirror of edit-backlog.py's durable edit-marker contract.
// Site request admission and both task-prep runners use this scanner as the
// second half of the edit/task-prep handshake.  An incomplete marker is the
// per-stem edit lease; malformed state is a global fail-closed issue.

var fs = require('fs');
var path = require('path');
var childProcess = require('child_process');
var durableCas = require('./durable-cas-contract.cjs');
var taskSource = require('./task-source-contract.cjs');

var VERSION = 1;
var MAX_BYTES = 160 * 1024;
var TASK_MAX_BYTES = 64 * 1024 + 4096;
var MAX_ENTRIES = 10000;
var MAX_TOTAL_BYTES = 8 * 1024 * 1024;
var SNAPSHOT_MAX_BYTES = 20 * 1024 * 1024;
var SNAPSHOT_TIMEOUT_MS = 10000;
var SNAPSHOT_HELPER = path.join(__dirname, 'anchored-marker-scan.py');
var CANONICAL_ROOT = path.resolve(__dirname, '..', '..');
var STEM_RE = /^TASK_([0-9]+)_[A-Za-z0-9_]+$/;
var NAME_RE = /^(TASK_[0-9]+_[A-Za-z0-9_]+)\.json$/;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var TX_RE = /^[a-f0-9]{32}$/;
var PHASES = new Set(['claimed', 'writing-file', 'file-published', 'regenerating-index', 'index-published', 'verifying', 'completed']);
var FIELDS = new Set([
  'version', 'transactionId', 'stem', 'expectedSourceHash',
  'requestedSourceHash', 'recoveryMarkdownBase64', 'status', 'phase',
  'effect', 'sourceHash', 'createdAt', 'updatedAt', 'revision', 'lastError'
]);

function bounded(value, max) {
  var text = String(value == null ? '' : value);
  max = max || 500;
  return text.length <= max ? text : text.slice(0, max - 1) + '…';
}
function validStem(value) {
  return taskSource.safeTaskStem(value);
}
function validTimestamp(value) {
  if (typeof value !== 'string') return false;
  var match = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})(?:\.[0-9]{1,6})?Z$/.exec(value);
  if (!match) return false;
  var year = Number(match[1]), month = Number(match[2]), day = Number(match[3]);
  var hour = Number(match[4]), minute = Number(match[5]), second = Number(match[6]);
  if (year < 1 || month < 1 || month > 12 || day < 1 || hour > 23 || minute > 59 || second > 59) return false;
  // Date.UTC treats years 0..99 as 1900..1999; setUTCFullYear avoids that
  // year coercion, then an exact component round-trip rejects Feb 30 etc.
  var parsed = new Date(0);
  parsed.setUTCFullYear(year, month - 1, day);
  parsed.setUTCHours(hour, minute, second, 0);
  return parsed.getUTCFullYear() === year && parsed.getUTCMonth() === month - 1 && parsed.getUTCDate() === day &&
    parsed.getUTCHours() === hour && parsed.getUTCMinutes() === minute && parsed.getUTCSeconds() === second;
}
function timestampKey(value) {
  var match = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})(?:\.([0-9]{1,6}))?Z$/.exec(value);
  return match.slice(1, 7).join('') + String(match[7] || '').padEnd(6, '0');
}
function exactFields(record) {
  var keys = Object.keys(record);
  return keys.length === FIELDS.size && keys.every(function (key) { return FIELDS.has(key); });
}
function validUnicodeScalarString(value, maxChars, nonempty) {
  if (typeof value !== 'string' || (nonempty && !value)) return false;
  for (var i = 0; i < value.length; i++) {
    var unit = value.charCodeAt(i);
    if (unit >= 0xD800 && unit <= 0xDBFF) {
      if (i + 1 >= value.length) return false;
      var next = value.charCodeAt(++i);
      if (next < 0xDC00 || next > 0xDFFF) return false;
    } else if (unit >= 0xDC00 && unit <= 0xDFFF) {
      return false;
    }
  }
  // Python's len(str) counts Unicode scalar/code-point values, while JS
  // String.length counts UTF-16 code units. Array.from aligns the bounds.
  return Array.from(value).length <= maxChars;
}
function headingValid(stem, bytes) {
  var number;
  try { number = String(BigInt(STEM_RE.exec(stem)[1])); }
  catch (error) { return false; }
  var text;
  try { text = new (require('util').TextDecoder)('utf-8', { fatal: true }).decode(bytes); }
  catch (error) { return false; }
  var first = text.split(/\n/, 1)[0];
  var match = /^# TASK ([0-9]+) — (\S(?:[^\r\n]*\S)?)$/.exec(first);
  return !!match && match[1] === number &&
    !/[\u0000-\u001f\u007f-\u009f\u2028\u2029\u202a-\u202e\u2066-\u2069\ufeff]/.test(match[2]);
}
function recoveryBytes(value) {
  if (typeof value !== 'string' || value.length > 4 * Math.ceil(TASK_MAX_BYTES / 3) || !/^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(value)) {
    throw new Error('edit marker recovery body is invalid');
  }
  var bytes = Buffer.from(value, 'base64');
  if (!bytes.length || bytes.length > TASK_MAX_BYTES || bytes.toString('base64') !== value || bytes.includes(0) || bytes[bytes.length - 1] !== 10 || bytes.includes(13)) {
    throw new Error('edit marker recovery body is unsafe or non-canonical');
  }
  return bytes;
}
function sha256(bytes) {
  return 'sha256:' + require('crypto').createHash('sha256').update(bytes).digest('hex');
}
function sorted(value) {
  if (Array.isArray(value)) return value.map(sorted);
  if (value && typeof value === 'object') {
    var out = {};
    Object.keys(value).sort().forEach(function (key) { out[key] = sorted(value[key]); });
    return out;
  }
  return value;
}
function canonicalBytes(record) {
  return Buffer.from(JSON.stringify(sorted(record), null, 2) + '\n', 'utf8');
}
function decodeUtf8(raw, label) {
  try { return new (require('util').TextDecoder)('utf-8', { fatal: true }).decode(raw); }
  catch (error) { throw new Error(label + ' is not valid UTF-8'); }
}
function validateRecord(record, expectedStem) {
  if (!record || typeof record !== 'object' || Array.isArray(record) || record.version !== VERSION || !exactFields(record)) return 'edit marker fields do not match the exact v1 contract';
  if (record.stem !== expectedStem || !validStem(expectedStem)) return 'edit marker stem does not match its filename';
  if (typeof record.transactionId !== 'string' || !TX_RE.test(record.transactionId)) return 'edit marker transaction id is invalid';
  if (typeof record.expectedSourceHash !== 'string' || !HASH_RE.test(record.expectedSourceHash) || typeof record.requestedSourceHash !== 'string' || !HASH_RE.test(record.requestedSourceHash)) return 'edit marker request hashes are invalid';
  if (record.sourceHash !== null && (typeof record.sourceHash !== 'string' || !HASH_RE.test(record.sourceHash))) return 'edit marker sourceHash is invalid';
  if (record.status !== 'incomplete' && record.status !== 'completed') return 'edit marker status is invalid';
  if (!PHASES.has(record.phase) || ((record.status === 'completed') !== (record.phase === 'completed'))) return 'edit marker status/phase are inconsistent';
  if (!Number.isSafeInteger(record.revision) || record.revision < 1) return 'edit marker revision is invalid';
  if (!validTimestamp(record.createdAt) || !validTimestamp(record.updatedAt)) return 'edit marker timestamps are invalid';
  if (timestampKey(record.updatedAt) < timestampKey(record.createdAt)) return 'edit marker timestamps are out of order';
  if (record.lastError !== null) {
    var err = record.lastError;
    if (!err || typeof err !== 'object' || Array.isArray(err) || Object.keys(err).sort().join(',') !== 'at,code,message' ||
        !validUnicodeScalarString(err.code, 120, true) || !validUnicodeScalarString(err.message, 1200, false) || !validTimestamp(err.at)) {
      return 'edit marker lastError is invalid';
    }
    if (timestampKey(err.at) < timestampKey(record.createdAt) || timestampKey(err.at) > timestampKey(record.updatedAt)) {
      return 'edit marker lastError timestamp is out of bounds';
    }
  }
  if (record.status === 'incomplete') {
    if (record.effect !== null || record.sourceHash !== null) return 'incomplete edit marker has a final effect';
    try {
      var bytes = recoveryBytes(record.recoveryMarkdownBase64);
      if (!headingValid(expectedStem, bytes)) return 'edit marker recovery heading is invalid';
      if (sha256(bytes) !== record.requestedSourceHash) return 'edit marker recovery body hash is invalid';
    } catch (error) { return error.message; }
  } else {
    if (record.revision < 2) return 'completed edit marker revision is invalid';
    if (record.recoveryMarkdownBase64 !== null) return 'completed edit marker retained its recovery body';
    if (record.effect !== 'changed' && record.effect !== 'unchanged' && record.effect !== 'aborted') return 'completed edit marker has no final effect';
    if (record.effect === 'changed' && !(record.sourceHash === record.requestedSourceHash && record.expectedSourceHash !== record.requestedSourceHash && record.lastError === null)) return 'changed edit receipt is inconsistent';
    if (record.effect === 'unchanged' && !(record.expectedSourceHash === record.requestedSourceHash && record.sourceHash === record.requestedSourceHash && record.lastError === null)) return 'unchanged edit receipt is inconsistent';
    if (record.effect === 'aborted' && !(record.sourceHash === null && record.lastError !== null)) return 'aborted edit receipt is inconsistent';
  }
  return null;
}
function readRecord(file, expectedStem, authorityRoot) {
  var target = path.resolve(file);
  var expectedName = expectedStem + '.json';
  if (path.basename(target) !== expectedName) throw new Error('edit marker path does not match its stem');
  var snapshot = anchoredSnapshot(path.dirname(target), authorityRoot);
  if (snapshot.missing) throw new Error('edit marker directory is missing');
  var entry = snapshot.entries.find(function (item) { return item.name === expectedName; });
  if (!entry) throw new Error('edit marker is missing');
  if (entry.kind !== 'file') throw new Error('edit marker must be a regular file');
  if (entry.oversized === true) throw new Error('edit marker exceeds its byte limit');
  if (!Buffer.isBuffer(entry.raw)) throw new Error('anchored edit marker bytes are missing');
  return readRecordBytes(entry.raw, expectedStem);
}
function readRecordBytes(raw, expectedStem) {
  var parsed;
  try { parsed = JSON.parse(decodeUtf8(raw, 'edit marker')); }
  catch (error) { throw new Error('edit marker is not valid JSON: ' + error.message); }
  var shape = validateRecord(parsed, expectedStem);
  if (shape) throw new Error(shape);
  if (!raw.equals(canonicalBytes(parsed))) throw new Error('edit marker JSON is not in its canonical serialized form');
  return parsed;
}
function isDurableCasName(name) { return durableCas.isName(name); }
function pathInside(root, candidate) {
  var relative = path.relative(path.resolve(root), path.resolve(candidate));
  return relative === '' || (relative !== '..' && !relative.startsWith('..' + path.sep) && !path.isAbsolute(relative));
}
function markerAuthorityRoot(dir, explicitRoot) {
  var target = path.resolve(dir);
  if (explicitRoot !== undefined && explicitRoot !== null) {
    if (typeof explicitRoot !== 'string' || !explicitRoot || !pathInside(explicitRoot, target)) {
      throw new Error('explicit edit-marker authority root does not contain the marker directory');
    }
    return path.resolve(explicitRoot);
  }
  var candidates = [
    process.env.EDIT_BACKLOG_AUTHORITY_ROOT,
    process.env.CREATE_BACKLOG_AUTHORITY_ROOT,
    process.env.ORCHESTRATOR_PROJECT_ROOT,
    process.env.FINALIZE_PROJECT_ROOT,
    CANONICAL_ROOT
  ].filter(function (value) { return typeof value === 'string' && value; });
  for (var i = 0; i < candidates.length; i++) {
    if (pathInside(candidates[i], target)) return path.resolve(candidates[i]);
  }
  // Isolated contract tests and CLI fixtures retain the canonical project
  // shape.  Treat the component before `orchestrator/` as their explicit
  // workspace trust boundary; for a standalone relocated marker directory,
  // the supplied parent is the narrowest possible authority.
  var components = target.split(path.sep);
  var orchestratorIndex = components.lastIndexOf('orchestrator');
  if (orchestratorIndex > 0) {
    var project = components.slice(0, orchestratorIndex).join(path.sep) || path.parse(target).root;
    if (pathInside(project, target)) return path.resolve(project);
  }
  return path.dirname(target);
}
function anchoredSnapshot(dir, authorityRoot) {
  var helper = fs.lstatSync(SNAPSHOT_HELPER);
  if (helper.isSymbolicLink() || !helper.isFile() || helper.size > 128 * 1024) {
    throw new Error('anchored marker scanner is unsafe');
  }
  var env = {
    PATH: process.env.PATH || '/usr/bin:/bin',
    LANG: 'C.UTF-8', LC_ALL: 'C.UTF-8', PYTHONDONTWRITEBYTECODE: '1'
  };
  if (process.env.EDIT_MARKER_SCAN_TEST_SWAP_WITH) env.EDIT_MARKER_SCAN_TEST_SWAP_WITH = process.env.EDIT_MARKER_SCAN_TEST_SWAP_WITH;
  if (process.env.EDIT_MARKER_SCAN_TEST_ROOT) env.EDIT_MARKER_SCAN_TEST_ROOT = process.env.EDIT_MARKER_SCAN_TEST_ROOT;
  if (process.env.EDIT_MARKER_SCAN_TEST_CREATE_MISSING) env.EDIT_MARKER_SCAN_TEST_CREATE_MISSING = process.env.EDIT_MARKER_SCAN_TEST_CREATE_MISSING;
  var result = childProcess.spawnSync('python3', [
    SNAPSHOT_HELPER, path.resolve(dir), markerAuthorityRoot(dir, authorityRoot),
    String(MAX_ENTRIES), String(MAX_BYTES),
    String(MAX_TOTAL_BYTES), CANONICAL_ROOT
  ], {
    cwd: __dirname, env: env, encoding: 'utf8', timeout: SNAPSHOT_TIMEOUT_MS,
    maxBuffer: SNAPSHOT_MAX_BYTES, windowsHide: true
  });
  if (result.error || result.signal || result.status !== 0 || !result.stdout || result.stderr) {
    throw new Error('anchored marker scan failed or exceeded its resource bound');
  }
  var envelope;
  try { envelope = JSON.parse(result.stdout); }
  catch (error) { throw new Error('anchored marker scan returned invalid JSON'); }
  if (!envelope || typeof envelope !== 'object' || Array.isArray(envelope) ||
      Object.keys(envelope).sort().join(',') !== (envelope.ok ? 'ok,snapshot' : 'error,ok')) {
    throw new Error('anchored marker scan returned an invalid envelope');
  }
  if (envelope.ok !== true) {
    var detail = envelope.error;
    throw new Error(detail && typeof detail.message === 'string' ? bounded(detail.message, 1000) : 'anchored marker scan failed closed');
  }
  var snapshot = envelope.snapshot;
  if (!snapshot || typeof snapshot !== 'object' || Array.isArray(snapshot) || snapshot.version !== 1 ||
      typeof snapshot.missing !== 'boolean' || !Array.isArray(snapshot.entries) || snapshot.entries.length > MAX_ENTRIES ||
      Object.keys(snapshot).sort().join(',') !== 'entries,missing,version') {
    throw new Error('anchored marker snapshot contract is invalid');
  }
  var seen = new Set(), total = 0;
  snapshot.entries.forEach(function (entry) {
    if (!entry || typeof entry !== 'object' || Array.isArray(entry) || typeof entry.name !== 'string' ||
        !entry.name || Buffer.byteLength(entry.name, 'utf8') > 255 || seen.has(entry.name) ||
        !['file', 'directory', 'symlink', 'other'].includes(entry.kind) ||
        !Number.isSafeInteger(entry.size) || entry.size < 0) throw new Error('anchored marker entry is invalid');
    seen.add(entry.name);
    var allowed = ['kind', 'name', 'size'];
    if (entry.oversized === true) allowed.push('oversized');
    if (entry.rawBase64 !== undefined) allowed.push('rawBase64');
    if (Object.keys(entry).sort().join(',') !== allowed.sort().join(',')) throw new Error('anchored marker entry fields are invalid');
    if (entry.rawBase64 !== undefined) {
      if (typeof entry.rawBase64 !== 'string' || !/^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(entry.rawBase64)) throw new Error('anchored marker bytes are invalid');
      var raw = Buffer.from(entry.rawBase64, 'base64');
      if (raw.toString('base64') !== entry.rawBase64 || raw.length !== entry.size || raw.length > MAX_BYTES) throw new Error('anchored marker bytes violate their bound');
      total += raw.length;
      if (total > MAX_TOTAL_BYTES) throw new Error('anchored marker corpus exceeds its total byte bound');
      entry.raw = raw;
      delete entry.rawBase64;
    }
  });
  return snapshot;
}
function scan(dir, authorityRoot) {
  var snapshot;
  try {
    snapshot = anchoredSnapshot(dir, authorityRoot);
  } catch (error) {
    return { incomplete: [], completed: [], issues: [{ code: 'EDIT_MARKER_DIR_UNSAFE', message: bounded(error && error.message || error) }] };
  }
  if (snapshot.missing) return { incomplete: [], completed: [], issues: [] };
  var incomplete = [], completed = [], issues = [];
  snapshot.entries.filter(function (entry) { return !entry.name.startsWith('.'); }).sort(function (a, b) { return a.name.localeCompare(b.name); }).forEach(function (entry) {
    var name = entry.name;
    var match = NAME_RE.exec(name);
    if (!match) {
      if (name.endsWith('.json')) issues.push({ code: 'EDIT_MARKER_NAME_UNSAFE', message: 'unsafe edit marker filename: ' + bounded(name, 180) });
      return;
    }
    try {
      if (entry.kind !== 'file') throw new Error('edit marker must be a regular file');
      if (entry.oversized === true) throw new Error('edit marker exceeds its byte limit');
      if (!Buffer.isBuffer(entry.raw)) throw new Error('anchored marker bytes are missing');
      var record = readRecordBytes(entry.raw, match[1]);
      (record.status === 'incomplete' ? incomplete : completed).push(record);
    } catch (error) {
      issues.push({ code: 'EDIT_MARKER_INVALID', stem: match[1], message: bounded(error && error.message || error) });
    }
  });
  snapshot.entries.filter(function (entry) { return durableCas.classifyName(entry.name) !== null; }).sort(function (a, b) { return a.name.localeCompare(b.name); }).forEach(function (entry) {
    issues.push({ code: isDurableCasName(entry.name) ? 'EDIT_MARKER_CAS_RECOVERY_REQUIRED' : 'EDIT_MARKER_CAS_NAME_UNSAFE', message: 'durable edit-marker CAS recovery is required: ' + bounded(entry.name, 180) });
  });
  var tx = incomplete.concat(completed).map(function (record) { return record.transactionId; });
  if (new Set(tx).size !== tx.length) issues.push({ code: 'EDIT_MARKER_INVALID', message: 'edit marker transaction ids are not unique' });
  return { incomplete: incomplete, completed: completed, issues: issues };
}
function blockingIssue(dir, stem, allowAllRecovery, authorityRoot) {
  var result = scan(dir, authorityRoot);
  if (result.issues.length) return result.issues[0];
  // Only the server-owned --recover-all helper may bypass valid incomplete
  // markers, and it still cannot bypass malformed state.  The helper repeats
  // the exact scan under the shared kernel mutex before touching a task.
  if (allowAllRecovery === true) return null;
  // The body lease is per-stem, but its recovery also republishes shared
  // INDEX.json.  Therefore every task-prep second guard must treat any
  // incomplete edit as a global publication gate, just like creation recovery.
  if (!result.incomplete.length) return null;
  return {
    code: 'EDIT_INCOMPLETE', stem: result.incomplete[0].stem,
    requestedStem: stem || null,
    message: 'durable backlog edit recovery owns shared task publication for ' + result.incomplete[0].stem
  };
}

module.exports = {
  VERSION: VERSION,
  MAX_BYTES: MAX_BYTES,
  STEM_RE: STEM_RE,
  validStem: validStem,
  NAME_RE: NAME_RE,
  validateRecord: validateRecord,
  readRecord: readRecord,
  readRecordBytes: readRecordBytes,
  scan: scan,
  blockingIssue: blockingIssue,
  isDurableCasName: isDurableCasName,
  DURABLE_CAS_SCHEMA: durableCas.SCHEMA,
  durableCas: durableCas
};
