'use strict';

// Pure mirror of create-backlog.py's durable creation receipt contract. Every
// created effect is bound to an exact target file generation. Both the site
// mutation guard and standalone finalizer use this module.

var crypto = require('crypto');
var durableCas = require('./durable-cas-contract.cjs');
var taskSource = require('./task-source-contract.cjs');

var MAX_BYTES = 288 * 1024; // create-backlog.py: MAX_REQUEST_BYTES + 32 KiB
var MAX_SAFE = 9007199254740991;
var BASE_FIELDS = ['version', 'transactionId', 'keyHash', 'payloadHash', 'intent', 'status', 'phase', 'effect',
  'number', 'slug', 'stem', 'sourceHash', 'column', 'createdAt', 'updatedAt', 'revision', 'lastError'].sort();
var FIELDS = BASE_FIELDS.concat(['targetProof']).sort();
var INTENT_FIELDS = ['version', 'title', 'body', 'originStem', 'dedupKey', 'dedupReport', 'source'].sort();
var PHASES = new Set(['claimed', 'reserving-number', 'number-reserved', 'publishing-file', 'file-published',
  'regenerating-index', 'index-published', 'verifying', 'completed']);
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var STEM_RE = /^TASK_([0-9]+)_([A-Za-z0-9_]+)$/;
var TX_RE = /^[a-f0-9]{32}$/;
var KEY_RE = /^[A-Za-z0-9_.:-]{1,240}$/;
var UNSIGNED_DECIMAL_RE = /^(?:0|[1-9][0-9]{0,19})$/;
var SIGNED_DECIMAL_RE = /^-?(?:0|[1-9][0-9]{0,19})$/;
var PROOF_FIELDS = ['dev', 'ino', 'mode', 'size', 'mtimeNs', 'ctimeNs', 'hash'].sort();

function codePoints(value) { return Array.from(value).length; }
function utf8(value) { return Buffer.byteLength(value, 'utf8'); }
function unicodeScalar(value) {
  if (typeof value !== 'string') return false;
  for (var i = 0; i < value.length; i++) {
    var unit = value.charCodeAt(i);
    if (unit >= 0xd800 && unit <= 0xdbff) {
      if (i + 1 >= value.length) return false;
      var next = value.charCodeAt(++i);
      if (next < 0xdc00 || next > 0xdfff) return false;
    } else if (unit >= 0xdc00 && unit <= 0xdfff) return false;
  }
  return true;
}
function iso(value) {
  if (typeof value !== 'string') return false;
  var match = /^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})(?:\.([0-9]{1,6}))?Z$/.exec(value);
  if (!match) return false;
  var year = Number(match[1]), month = Number(match[2]), day = Number(match[3]);
  var hour = Number(match[4]), minute = Number(match[5]), second = Number(match[6]);
  if (year < 1 || month < 1 || month > 12 || day < 1 || hour > 23 || minute > 59 || second > 59) return false;
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
function sameKeys(value, keys) { return value && typeof value === 'object' && !Array.isArray(value) && Object.keys(value).sort().join('\0') === keys.join('\0'); }
function canonical(value) {
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']';
  if (value && typeof value === 'object') return '{' + Object.keys(value).sort().map(function (key) { return JSON.stringify(key) + ':' + canonical(value[key]); }).join(',') + '}';
  return JSON.stringify(value);
}
function digest(value) { return 'sha256:' + crypto.createHash('sha256').update(Buffer.from(canonical(value), 'utf8')).digest('hex'); }
function fail(message) { var error = new Error(message); error.code = 'CREATION_MARKER_INVALID'; throw error; }

function validateIntent(value, payloadHash) {
  if (!sameKeys(value, INTENT_FIELDS)) fail('incomplete receipt has no exact canonical intent');
  if (value.version !== 1) fail('intent version must be 1');
  if (typeof value.title !== 'string' || !unicodeScalar(value.title) || value.title.normalize('NFC').trim() !== value.title || !value.title ||
      codePoints(value.title) > 200 || utf8(value.title) > 512 || /[\u0000-\u001f\u007f-\u009f\u2028\u2029\u202a-\u202e\u2066-\u2069]/.test(value.title)) fail('intent title is not canonical');
  if (typeof value.body !== 'string' || !unicodeScalar(value.body) || codePoints(value.body) > 64 * 1024 || utf8(value.body) > 64 * 1024 ||
      value.body.indexOf('\u0000') >= 0 || /[\u0001-\u0008\u000b\u000c\u000e-\u001f]/.test(value.body) || value.body.indexOf('\r') >= 0) fail('intent body is not canonical');
  if (value.originStem !== null && (value.originStem.length > 120 || !taskSource.safeTaskStem(value.originStem))) fail('intent originStem is invalid');
  if (value.originStem !== null && /(^|\n)##[ \t]+Origin[ \t]*$/m.test(value.body)) fail('intent body duplicates Origin');
  if (value.dedupKey !== null && (typeof value.dedupKey !== 'string' || !KEY_RE.test(value.dedupKey))) fail('intent dedupKey is invalid');
  if (value.dedupReport !== null && (typeof value.dedupReport !== 'string' || !HASH_RE.test(value.dedupReport) || value.dedupKey === null)) fail('intent dedupReport is invalid');
  var source = taskSource.validate(value.source);
  if (!source) fail('intent source is invalid');
  if (source.kind === 'follow-up' && source.type === 'task-split' && source.ref !== value.originStem) {
    fail('intent source does not match originStem');
  }
  if (taskSource.realSourceHeadings(value.body).length) fail('intent body duplicates Source');
  if (digest(value) !== payloadHash) fail('intent does not match payloadHash');
}

function validateTargetProof(value, sourceHash) {
  if (value === null) return null;
  if (!sameKeys(value, PROOF_FIELDS) || typeof value.dev !== 'string' || typeof value.ino !== 'string' ||
      typeof value.mtimeNs !== 'string' || typeof value.ctimeNs !== 'string' ||
      !UNSIGNED_DECIMAL_RE.test(value.dev) || !UNSIGNED_DECIMAL_RE.test(value.ino) ||
      !SIGNED_DECIMAL_RE.test(value.mtimeNs) || !SIGNED_DECIMAL_RE.test(value.ctimeNs) ||
      value.mtimeNs === '-0' || value.ctimeNs === '-0' ||
      value.mode !== 0o600 || !Number.isSafeInteger(value.size) || value.size < 1 || value.size > 68 * 1024 ||
      !HASH_RE.test(String(value.hash || '')) || value.hash !== sourceHash) {
    fail('targetProof does not match the exact created target identity');
  }
  return value;
}

function validate(value, filename) {
  if (!value || typeof value !== 'object' || Array.isArray(value) || value.version !== 2 ||
      !sameKeys(value, FIELDS)) fail('receipt fields do not match the current contract');
  var nameMatch = /^([a-f0-9]{64})\.json$/.exec(String(filename || ''));
  if (!nameMatch || value.keyHash !== 'sha256:' + nameMatch[1] || !HASH_RE.test(value.keyHash) || !HASH_RE.test(value.payloadHash)) fail('receipt hashes do not match filename');
  if (!TX_RE.test(String(value.transactionId || ''))) fail('transaction id is invalid');
  if (!['incomplete', 'completed'].includes(value.status) || !PHASES.has(value.phase) ||
      ((value.status === 'completed') !== (value.phase === 'completed'))) fail('status and phase are inconsistent');
  if (!Number.isSafeInteger(value.revision) || value.revision < 1 || !iso(value.createdAt) || !iso(value.updatedAt)) fail('revision or timestamps are invalid');
  if (timestampKey(value.updatedAt) < timestampKey(value.createdAt)) fail('timestamps are out of order');
  if (![null, 'backlog', 'pending', 'todo', 'done'].includes(value.column)) fail('column is invalid');
  if (value.lastError !== null && (!sameKeys(value.lastError, ['at', 'code', 'message']) || typeof value.lastError.code !== 'string' ||
      !value.lastError.code || codePoints(value.lastError.code) > 120 || !unicodeScalar(value.lastError.code) ||
      typeof value.lastError.message !== 'string' || codePoints(value.lastError.message) > 1200 ||
      !unicodeScalar(value.lastError.message) || !iso(value.lastError.at))) fail('lastError is invalid');
  if (value.lastError !== null && (timestampKey(value.lastError.at) < timestampKey(value.createdAt) ||
      timestampKey(value.lastError.at) > timestampKey(value.updatedAt))) fail('lastError timestamp is out of bounds');
  if (value.number !== null && (!Number.isSafeInteger(value.number) || value.number < 1 || value.number > MAX_SAFE)) fail('number is invalid');
  var stemMatch = value.stem === null ? null : STEM_RE.exec(String(value.stem));
  if (value.stem !== null && !taskSource.safeTaskStem(value.stem)) fail('stem is invalid');
  if ((value.number === null) !== (value.stem === null) || (stemMatch && stemMatch[1] !== String(value.number))) fail('number/stem are inconsistent');
  if (value.slug !== null && (typeof value.slug !== 'string' || value.slug.length > 80 || !/^[a-z0-9_]+$/.test(value.slug))) fail('slug is invalid');
  if (value.stem !== null && value.effect !== 'domain-dedup' && (value.slug === null || value.stem !== 'TASK_' + value.number + '_' + value.slug)) fail('stem/slug are inconsistent');
  if (value.sourceHash !== null && !HASH_RE.test(String(value.sourceHash))) fail('sourceHash is invalid');
  var targetProof = validateTargetProof(value.targetProof, value.sourceHash);
  if (![null, 'created', 'domain-dedup'].includes(value.effect)) fail('effect is invalid');
  if (value.effect === 'domain-dedup' && value.status !== 'completed') fail('domain-dedup is not completed');
  if (value.effect === 'created' && (!value.stem || !HASH_RE.test(String(value.sourceHash || '')))) fail('created effect has no identity');
  if (value.status === 'incomplete') {
    validateIntent(value.intent, value.payloadHash);
    if (value.phase === 'claimed' && [value.number, value.stem, value.slug, value.sourceHash, targetProof, value.effect, value.column].some(function (item) { return item !== null; })) fail('claimed receipt already records effects');
    if (value.phase === 'reserving-number' && (value.slug === null || value.sourceHash !== null || targetProof !== null || value.effect !== null || value.column !== null)) fail('number reservation receipt has an impossible effect lattice');
    if (value.phase === 'number-reserved' && (value.number === null || value.slug === null || value.sourceHash !== null || targetProof !== null || value.effect !== null || value.column !== null)) fail('reserved-number receipt has an impossible effect lattice');
    if (value.phase === 'publishing-file' && (value.number === null || value.slug === null || value.sourceHash === null || value.effect !== null || value.column !== null)) fail('file-publication receipt has an impossible effect lattice');
    if (['file-published', 'regenerating-index', 'index-published', 'verifying'].includes(value.phase) &&
        (value.number === null || value.slug === null || value.sourceHash === null || value.effect !== 'created' || value.column !== null ||
         targetProof === null)) fail('published receipt has an impossible effect lattice');
  } else {
    if (value.revision < 2) fail('completed receipt cannot be the initial marker generation');
    if (value.intent !== null || value.lastError !== null || value.number === null || value.stem === null || value.sourceHash === null || value.column === null) fail('completed receipt retained recovery state or lacks identity');
    if (value.effect === 'created' && (value.slug === null || value.column !== 'backlog' ||
        targetProof === null)) fail('completed created receipt has an impossible result lattice');
    if (value.effect === 'domain-dedup' && (value.slug !== null || targetProof !== null)) fail('completed domain-dedup receipt claims a generated target');
    if (value.effect !== 'created' && value.effect !== 'domain-dedup') fail('completed receipt has no effect');
  }
  return value;
}

function validateSet(entries) {
  if (!Array.isArray(entries)) fail('receipt set is invalid');
  var seen = new Set();
  return entries.map(function (entry) {
    if (!entry || typeof entry !== 'object' || Array.isArray(entry) ||
        Object.keys(entry).sort().join(',') !== 'filename,value') fail('receipt-set entry is invalid');
    var record = validate(entry.value, entry.filename);
    if (seen.has(record.transactionId)) fail('creation marker transaction ids are not unique');
    seen.add(record.transactionId);
    return record;
  });
}

module.exports = {
  MAX_BYTES: MAX_BYTES,
  validate: validate,
  validateSet: validateSet,
  canonical: canonical,
  digest: digest,
  isDurableCasName: durableCas.isName,
  DURABLE_CAS_SCHEMA: durableCas.SCHEMA,
  durableCas: durableCas
};
