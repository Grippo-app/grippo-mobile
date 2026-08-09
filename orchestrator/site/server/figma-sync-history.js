'use strict';

// Bounded, prompt-free Figma sync audit trail. Each record is written before
// its index row; the index is atomically replaced and retained to 100 jobs.

var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var historyPagination = require('./history-pagination');

var DIR = path.join(paths.FIGMA_CACHE_DIR, 'sync-history');
var INDEX_FILE = path.join(DIR, 'index.json');
var JOB_RE = /^fsj-[a-f0-9]{32}$/;
var GENERATION_RE = /^gen-[a-f0-9]{32}$/;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var JOB_MAX = 256 * 1024;
var INDEX_MAX = 1024 * 1024;
var RETENTION = 100;
var MESSAGE_MAX = 500;
var writeQueue = Promise.resolve();

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function iso(value, nullable) {
  if (nullable && value === null) return true;
  if (typeof value !== 'string' || !/^\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d\.\d{3}Z$/.test(value)) return false;
  var parsed = new Date(value);
  return Number.isFinite(parsed.getTime()) && parsed.toISOString() === value;
}
function fileFor(id) { if (!JOB_RE.test(String(id || ''))) throw new Error('sync-job-id-invalid'); return path.join(DIR, id + '.json'); }
function safeBytes(file, max) {
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, DIR, file, max);
  return hit && hit.stat && String(hit.stat.nlink) === '1' ? hit.bytes : null;
}
function atomicWrite(file, bytes, max) {
  if (bytes.length > max) throw new Error('sync-history-size-limit');
  var result = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, DIR, file, bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: max });
  if (!result.ok) throw new Error(result.code || 'sync-history-write-failed');
}
function boundedString(value, max) {
  var out = String(value == null ? '' : value).replace(/[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]/g, '');
  return out.length <= max ? out : out.slice(0, max);
}
function redact(value, key, depth) {
  if (depth > 10) return null;
  if (value === null || typeof value === 'boolean' || (typeof value === 'number' && Number.isFinite(value))) return value;
  if (typeof value === 'string') {
    if (key !== 'fileKeyFingerprint' && /token|secret|password|cookie|authorization|credential|prompt|transcript|filekey|figmaurl/i.test(String(key || ''))) return '[redacted]';
    return boundedString(value
      .replace(/Bearer\s+\S+/gi, 'Bearer [redacted]')
      .replace(/https:\/\/(?:www\.)?figma\.com\/\S+/gi, '[figma-url-redacted]')
      .replace(/https?:\/\/\S+/gi, '[url-redacted]')
      .replace(/(file[ -]?key\s*(?::|=)?\s*)[A-Za-z0-9]{8,}/gi, '$1[redacted]')
      .replace(/(^|[\s("'])\/(?!\/)[^\s"'<>]*/g, '$1[local-path-redacted]')
      .replace(/\b[A-Za-z]:\\[^\s"'<>]*/g, '[local-path-redacted]'), 2048);
  }
  if (Array.isArray(value)) return value.slice(0, MESSAGE_MAX).map(function (item) { return redact(item, key, depth + 1); });
  if (!value || typeof value !== 'object') return null;
  var out = {};
  Object.keys(value).slice(0, 100).forEach(function (child) {
    if (/token|secret|password|cookie|authorization|credential|prompt|transcript|filekey|figmaurl/i.test(child) &&
        child !== 'fileKeyFingerprint') return;
    out[child] = redact(value[child], child, depth + 1);
  });
  return out;
}
function validateGroup(row) {
  return exact(row, ['group', 'status', 'updated', 'unchanged', 'warnings']) &&
    ['tokens', 'components', 'surfaces', 'drift'].indexOf(row.group) >= 0 &&
    ['pending', 'running', 'completed', 'failed', 'cancelled'].indexOf(row.status) >= 0 &&
    ['updated', 'unchanged', 'warnings'].every(function (key) { return Number.isSafeInteger(row[key]) && row[key] >= 0; });
}
function validateRecord(value) {
  if (!exact(value, ['schemaVersion', 'id', 'startedAt', 'finishedAt', 'committedGenerationId', 'accountFingerprint', 'fileKeyFingerprint', 'planGroups', 'groups', 'result', 'errorCode', 'durationMs', 'messages'])) return false;
  var terminal = ['success', 'partial', 'failed', 'cancelled', 'interrupted'].indexOf(value.result) >= 0;
  var planSeen = Object.create(null), groupSeen = Object.create(null);
  return value.schemaVersion === 1 && JOB_RE.test(String(value.id || '')) && iso(value.startedAt) && iso(value.finishedAt, true) &&
    (value.committedGenerationId === null || GENERATION_RE.test(String(value.committedGenerationId || ''))) &&
    HASH_RE.test(String(value.accountFingerprint || '')) && HASH_RE.test(String(value.fileKeyFingerprint || '')) &&
    Array.isArray(value.planGroups) && value.planGroups.length >= 1 && value.planGroups.length <= 4 && value.planGroups.every(function (group) {
      if (['tokens', 'components', 'surfaces', 'drift'].indexOf(group) < 0 || planSeen[group]) return false;
      planSeen[group] = 1; return true;
    }) &&
    Array.isArray(value.groups) && value.groups.length >= 1 && value.groups.length <= 4 && value.groups.every(function (group) {
      if (!validateGroup(group) || groupSeen[group.group]) return false;
      groupSeen[group.group] = 1; return true;
    }) &&
    Object.keys(planSeen).every(function (group) { return groupSeen[group]; }) &&
    ['queued', 'running', 'success', 'partial', 'failed', 'cancelled', 'interrupted'].indexOf(value.result) >= 0 &&
    (value.errorCode === null || /^[a-z0-9][a-z0-9-]{0,79}$/.test(String(value.errorCode || ''))) &&
    (terminal ? value.finishedAt !== null && Number.isSafeInteger(value.durationMs) && value.durationMs >= 0
      : value.finishedAt === null && value.durationMs === null && value.committedGenerationId === null) &&
    ((value.result === 'success' || value.result === 'partial') ? value.committedGenerationId !== null : value.committedGenerationId === null) &&
    Array.isArray(value.messages) && value.messages.length <= MESSAGE_MAX && value.messages.every(function (message) { return typeof message === 'string' && message.length <= 2048; });
}
function initialIndex() { return { schemaVersion: 1, jobs: [] }; }
function validateIndex(value) {
  if (!exact(value, ['schemaVersion', 'jobs']) || value.schemaVersion !== 1 || !Array.isArray(value.jobs) || value.jobs.length > RETENTION) return false;
  var seen = Object.create(null);
  return value.jobs.every(function (row) {
    if (!exact(row, ['id', 'startedAt', 'finishedAt', 'result', 'committedGenerationId']) || seen[row.id]) return false;
    seen[row.id] = 1;
    var terminal = ['success', 'partial', 'failed', 'cancelled', 'interrupted'].indexOf(row.result) >= 0;
    var successful = row.result === 'success' || row.result === 'partial';
    return JOB_RE.test(String(row.id || '')) && iso(row.startedAt) && iso(row.finishedAt, true) &&
      ['queued', 'running', 'success', 'partial', 'failed', 'cancelled', 'interrupted'].indexOf(row.result) >= 0 &&
      (terminal ? row.finishedAt !== null : row.finishedAt === null) &&
      (successful ? GENERATION_RE.test(String(row.committedGenerationId || '')) : row.committedGenerationId === null);
  });
}
function recordMatchesRow(record, row) {
  return !!record && record.id === row.id && record.startedAt === row.startedAt &&
    record.finishedAt === row.finishedAt && record.result === row.result &&
    record.committedGenerationId === row.committedGenerationId;
}
function readIndexed(row) {
  var record = read(row.id);
  return recordMatchesRow(record, row) ? record : null;
}
function readIndex() {
  try {
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, DIR, INDEX_FILE);
    if (inspected && inspected.status === 'missing') {
      var directoryEntry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(DIR), DIR);
      if (directoryEntry && directoryEntry.status === 'missing') return initialIndex();
      if (!directoryEntry || directoryEntry.status !== 'present' || !directoryEntry.stat ||
          !directoryEntry.stat.isDirectory() || directoryEntry.stat.isSymbolicLink()) return null;
      var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, DIR, RETENTION * 20);
      if (!listed.ok) return null;
      return listed.names.length ? null : initialIndex();
    }
    if (!inspected || inspected.status !== 'present' || !inspected.stat || !inspected.stat.isFile() ||
        inspected.stat.isSymbolicLink() || String(inspected.stat.nlink) !== '1') return null;
    var bytes = safeBytes(INDEX_FILE, INDEX_MAX);
    if (!bytes) return null;
    var value = JSON.parse(bytes.toString('utf8'));
    if (!validateIndex(value)) return null;
    var indexed = Object.create(null);
    value.jobs.forEach(function (row) { indexed[row.id + '.json'] = 1; });
    var names = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, DIR, RETENTION * 20);
    if (!names.ok || names.names.some(function (name) {
      return /^fsj-[a-f0-9]{32}\.json$/.test(name) && !indexed[name];
    })) return null;
    return value;
  } catch (error) { return null; }
}
function read(id) {
  try {
    var bytes = safeBytes(fileFor(id), JOB_MAX);
    if (!bytes) return null;
    var value = JSON.parse(bytes.toString('utf8'));
    return validateRecord(value) ? value : null;
  } catch (error) { return null; }
}
function serialize(fn) {
  var next = writeQueue.then(fn, fn);
  writeQueue = next.catch(function () {});
  return next;
}
function write(record) {
  var clean = redact(record, '', 0);
  if (!validateRecord(clean)) return Promise.reject(new Error('sync-history-record-invalid'));
  return serialize(function () {
    var index = readIndex();
    if (!index) throw new Error('sync-history-index-invalid');
    atomicWrite(fileFor(clean.id), Buffer.from(JSON.stringify(clean, null, 2) + '\n'), JOB_MAX);
    index.jobs = index.jobs.filter(function (row) { return row.id !== clean.id; });
    index.jobs.unshift({ id: clean.id, startedAt: clean.startedAt, finishedAt: clean.finishedAt, result: clean.result, committedGenerationId: clean.committedGenerationId });
    var removed = index.jobs.slice(RETENTION);
    index.jobs = index.jobs.slice(0, RETENTION);
    atomicWrite(INDEX_FILE, Buffer.from(JSON.stringify(index, null, 2) + '\n'), INDEX_MAX);
    removed.forEach(function (row) {
      if (!fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, DIR, fileFor(row.id), { allowMissing: true })) {
        throw new Error('sync-history-retention-cleanup-failed');
      }
    });
    return clean;
  });
}
function latestSuccessful() {
  var index = readIndex();
  if (!index) return null;
  var row = index.jobs.find(function (item) { return item.result === 'success' || item.result === 'partial'; });
  return row ? readIndexed(row) : null;
}
function latest() {
  var index = readIndex();
  if (!index || !index.jobs.length) return null;
  return readIndexed(index.jobs[0]);
}
function latestForGroup(group) {
  if (['tokens', 'components', 'surfaces', 'drift'].indexOf(group) < 0) return null;
  var index = readIndex();
  if (!index) return null;
  for (var i = 0; i < index.jobs.length; i++) {
    var record = readIndexed(index.jobs[i]);
    if (!record) return null;
    if (record.planGroups.indexOf(group) >= 0) return record;
  }
  return null;
}
function retainedGenerationIds(limit) {
  var index = readIndex();
  if (!index) throw new Error('sync-history-index-invalid');
  var max = Number.isSafeInteger(limit) ? Math.max(0, Math.min(RETENTION, limit)) : RETENTION;
  if (max === 0) return [];
  var seen = Object.create(null), out = [];
  index.jobs.some(function (row) {
    var id = row.committedGenerationId;
    if (id && !seen[id]) { seen[id] = 1; out.push(id); }
    return out.length >= max;
  });
  return out;
}
function list(cursor, limit) {
  limit = Number.isInteger(limit) ? Math.max(1, Math.min(100, limit)) : 20;
  var index = readIndex();
  if (!index) return { ok: false, error: 'sync-history-index-invalid' };
  var page = historyPagination.page(index.jobs, cursor, limit, 'id', JOB_RE);
  if (!page.ok) return page;
  var items = page.rows.map(readIndexed);
  if (items.some(function (item) { return !item; })) return { ok: false, error: 'sync-history-record-invalid' };
  return { ok: true, items: items, nextCursor: page.nextCursor };
}
function publicRecord(record) {
  if (!record || typeof record !== 'object' || Array.isArray(record)) return null;
  return {
    schemaVersion: record.schemaVersion,
    id: record.id,
    startedAt: record.startedAt,
    finishedAt: record.finishedAt,
    committedGenerationId: record.committedGenerationId,
    accountFingerprint: record.accountFingerprint,
    fileKeyFingerprint: record.fileKeyFingerprint,
    planGroups: record.planGroups.slice(),
    groups: record.groups.map(function (group) { return Object.assign({}, group); }),
    result: record.result,
    errorCode: record.errorCode,
    durationMs: record.durationMs
  };
}
function recoverInterrupted(resolveCommitted) {
  var index = readIndex();
  if (!index) return Promise.reject(new Error('sync-history-index-invalid'));
  var records = index.jobs.map(readIndexed);
  if (records.some(function (record) { return !record; })) return Promise.reject(new Error('sync-history-record-invalid'));
  var pending = records.filter(function (record) { return record.result === 'queued' || record.result === 'running'; });
  return pending.reduce(function (promise, record) {
    return promise.then(function () {
      var committed = null;
      if (typeof resolveCommitted === 'function') committed = resolveCommitted(record);
      if (committed && (!exact(committed, ['generationId', 'updatedGroups', 'groups', 'partial', 'finishedAt']) ||
          !GENERATION_RE.test(String(committed.generationId || '')) || !Array.isArray(committed.updatedGroups) ||
          !committed.updatedGroups.length || committed.updatedGroups.length > 4 ||
          committed.updatedGroups.some(function (group, index) {
            return record.planGroups.indexOf(group) < 0 || committed.updatedGroups.indexOf(group) !== index;
          }) || !committed.groups || typeof committed.groups !== 'object' || Array.isArray(committed.groups) ||
          !Object.keys(committed.groups).length || Object.keys(committed.groups).length > 4 ||
          Object.keys(committed.groups).some(function (group) {
            return !validateGroup(Object.assign({ group: group }, committed.groups[group])) ||
              committed.groups[group].status === 'running';
          }) || record.planGroups.some(function (group) { return !committed.groups[group]; }) ||
          committed.updatedGroups.some(function (group) { return committed.groups[group].status !== 'completed'; }) ||
          typeof committed.partial !== 'boolean' || !iso(committed.finishedAt) ||
          Date.parse(committed.finishedAt) < Date.parse(record.startedAt))) {
        throw new Error('sync-history-committed-recovery-invalid');
      }
      var finishedAt = committed ? committed.finishedAt : new Date().toISOString();
      record.finishedAt = finishedAt;
      record.durationMs = Math.max(0, Date.parse(finishedAt) - Date.parse(record.startedAt));
      record.committedGenerationId = committed ? committed.generationId : null;
      record.result = committed ? (committed.partial ? 'partial' : 'success') : 'interrupted';
      record.errorCode = committed && committed.partial ? (record.errorCode || 'sync-partial') : committed ? null : 'job-interrupted';
      if (committed) {
        var groupOrder = record.planGroups.concat(['tokens', 'components', 'surfaces', 'drift'].filter(function (group) {
          return record.planGroups.indexOf(group) < 0;
        }));
        record.groups = groupOrder.filter(function (group) { return committed.groups[group]; }).map(function (group) {
          return Object.assign({ group: group }, committed.groups[group]);
        });
      } else record.groups.forEach(function (group) { if (group.status === 'running') group.status = 'failed'; });
      record.messages = record.messages.concat([committed
        ? 'Committed generation was reconciled after a site restart.'
        : 'Sync was interrupted by a site restart.']).slice(-MESSAGE_MAX);
      return write(record);
    });
  }, Promise.resolve()).then(function () { return pending.length; });
}

function clearAll() {
  return serialize(function () {
    var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, DIR, RETENTION + 4);
    if (!listed.ok) throw new Error('sync-history-index-invalid');
    for (var i = 0; i < listed.names.length; i++) {
      var target = path.join(DIR, listed.names[i]);
      var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, DIR, target);
      if (!inspected || inspected.status === 'missing') continue;
      if (inspected.status !== 'present' || !inspected.stat || !inspected.stat.isFile() ||
          inspected.stat.isSymbolicLink() || !fileGuards.unlinkRegularFileUnder(
            paths.PROJECT_ROOT, DIR, target, { allowMissing: true }
          )) throw new Error('sync-history-index-invalid');
    }
    return true;
  });
}

module.exports = {
  DIR: DIR,
  INDEX_FILE: INDEX_FILE,
  JOB_RE: JOB_RE,
  INDEX_MAX: INDEX_MAX,
  RETENTION: RETENTION,
  MESSAGE_MAX: MESSAGE_MAX,
  validateRecord: validateRecord,
  read: read,
  write: write,
  latest: latest,
  latestForGroup: latestForGroup,
  latestSuccessful: latestSuccessful,
  retainedGenerationIds: retainedGenerationIds,
  list: list,
  publicRecord: publicRecord,
  recoverInterrupted: recoverInterrupted,
  clearAll: clearAll,
  redact: redact,
  _readIndexForTests: readIndex,
  _pageRowsForTests: function (rows, cursor, limit) {
    return historyPagination.page(rows, cursor, limit, 'id', JOB_RE);
  }
};
