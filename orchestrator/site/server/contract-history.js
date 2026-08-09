'use strict';

// Bounded durable job reports and a small cursor-paginated history index.
// Report producers write versioned, already-redacted objects; this module adds
// a second structural redaction pass before any byte reaches disk.

var fs = require('fs');
var path = require('path');
var paths = require('./paths');
var generation = require('./contract-generation');
var fileGuards = require('./file-guards');
var historyPagination = require('./history-pagination');

var REPORTS_DIR = path.join(paths.API_CONTRACT_CACHE_DIR, 'reports');
var INDEX_FILE = path.join(REPORTS_DIR, 'history-index.json');
var CHANGE_INDEX_FILE = path.join(REPORTS_DIR, 'change-set-index.json');
var JOB_RE = /^job-[a-f0-9]{32}$/;
var PREVIEW_RE = /^preview-[a-f0-9]{32}$/;
var CHANGE_RE = /^changes-[a-f0-9]{24}$/;
var REPORT_MAX = 512 * 1024;
var CHANGE_REPORT_MAX = 10 * 1024 * 1024;
var INDEX_MAX = 1024 * 1024;
var RETENTION = 100;
var CHANGE_RETENTION = 20;
var writeQueue = Promise.resolve();

function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function reportFile(type, jobId) {
  if ((type !== 'probe' && type !== 'refresh') || !JOB_RE.test(String(jobId || ''))) throw new Error('report-id-invalid');
  return path.join(REPORTS_DIR, type + '-' + jobId + '.json');
}
function sameFileGeneration(left, right) {
  return left && right && left.isFile() && right.isFile() && !left.isSymbolicLink() && !right.isSymbolicLink() &&
    left.dev === right.dev && left.ino === right.ino && left.mode === right.mode && left.nlink === right.nlink &&
    left.size === right.size && left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs;
}
function parentAnchored(file) {
  var root = path.resolve(paths.PROJECT_ROOT), parent = path.resolve(path.dirname(file));
  var rel = path.relative(root, parent);
  if (rel === '..' || rel.startsWith('..' + path.sep) || path.isAbsolute(rel)) return false;
  return fs.realpathSync(parent) === path.join(fs.realpathSync(root), rel);
}
function safeBytes(file, max, optional) {
  var fd;
  try {
    if (!parentAnchored(file)) throw new Error('report-parent-unsafe');
    var before = fs.lstatSync(file, { bigint: true });
    if (!before.isFile() || before.isSymbolicLink() || before.nlink !== 1n || before.size > BigInt(max)) throw new Error('report-file-unsafe');
    fd = fs.openSync(file, fs.constants.O_RDONLY | (fs.constants.O_NOFOLLOW || 0));
    var opened = fs.fstatSync(fd, { bigint: true });
    if (!sameFileGeneration(before, opened)) throw new Error('report-file-raced');
    var bytes = Buffer.alloc(Number(opened.size)), offset = 0;
    while (offset < bytes.length) { var count = fs.readSync(fd, bytes, offset, bytes.length - offset, offset); if (!count) throw new Error('short-read'); offset += count; }
    var afterFd = fs.fstatSync(fd, { bigint: true }), afterPath = fs.lstatSync(file, { bigint: true });
    if (!sameFileGeneration(opened, afterFd) || !sameFileGeneration(opened, afterPath) || !parentAnchored(file)) throw new Error('report-file-raced');
    return bytes;
  } catch (e) { if (optional && e && e.code === 'ENOENT') return null; throw e; }
  finally { if (fd !== undefined) try { fs.closeSync(fd); } catch (ignore) {} }
}
function atomicWrite(file, bytes, overrideMax) {
  var maxBytes = overrideMax || (file === INDEX_FILE || file === CHANGE_INDEX_FILE ? INDEX_MAX : REPORT_MAX);
  if (bytes.length > maxBytes) throw new Error('report-size-limit');
  var published = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, path.dirname(file), file, bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: maxBytes });
  if (!published.ok) throw new Error(published.code || 'report-write-failed');
}
function changeSetFile(changeSetId) {
  if (!CHANGE_RE.test(String(changeSetId || ''))) throw new Error('change-set-id-invalid');
  return path.join(REPORTS_DIR, 'change-set-' + changeSetId.slice('changes-'.length) + '.json');
}
function readChangeIndex() {
  try {
    var bytes = safeBytes(CHANGE_INDEX_FILE, INDEX_MAX, true);
    if (bytes === null) return { schemaVersion: 1, ids: [] };
    var value = JSON.parse(bytes.toString('utf8'));
    if (!exactKeys(value, ['ids', 'schemaVersion']) || value.schemaVersion !== 1 ||
        !Array.isArray(value.ids) || value.ids.length > CHANGE_RETENTION ||
        value.ids.some(function (id, index) {
          return !CHANGE_RE.test(String(id || '')) || value.ids.indexOf(id) !== index;
        })) return null;
    return value;
  } catch (error) { return null; }
}
function validateChangeSet(changeSet) {
  if (!changeSet || changeSet.schemaVersion !== 2 ||
      !CHANGE_RE.test(String(changeSet.changeSetId || '')) ||
      !Array.isArray(changeSet.changes) || changeSet.changes.length > 10000) {
    throw new Error('change-set-contract-invalid');
  }
}
function writeChangeSetArtifact(changeSet) {
  validateChangeSet(changeSet);
  // The classifier applies an 8 MiB cap to compact change rows. Persisting the
  // immutable artifact in the same representation keeps that guarantee below
  // the generation's 10 MiB artifact limit even at the boundary.
  var bytes = Buffer.from(JSON.stringify(changeSet) + '\n');
  var file = changeSetFile(changeSet.changeSetId);
  var existing = safeBytes(file, CHANGE_REPORT_MAX, true);
  if (existing !== null) {
    if (existing.equals(bytes)) return { file: file, created: false };
    throw new Error('change-set-immutable-conflict');
  }
  atomicWrite(file, bytes, CHANGE_REPORT_MAX);
  return { file: file, created: true };
}
function indexChangeSet(changeSetId) {
  var file = changeSetFile(changeSetId);
  safeBytes(file, CHANGE_REPORT_MAX, false);
  var index = readChangeIndex();
  if (!index) throw new Error('change-set-index-invalid');
  index.ids = index.ids.filter(function (id) { return id !== changeSetId; });
  index.ids.unshift(changeSetId);
  var removed = index.ids.slice(CHANGE_RETENTION);
  index.ids = index.ids.slice(0, CHANGE_RETENTION);
  atomicWrite(CHANGE_INDEX_FILE, Buffer.from(JSON.stringify(index, null, 2) + '\n'));
  removed.forEach(function (id) {
    var target = changeSetFile(id);
    try {
      var held = safeBytes(target, CHANGE_REPORT_MAX, true);
      if (held !== null) fileGuards.unlinkRegularFileMatchingUnder(
        paths.PROJECT_ROOT, REPORTS_DIR, target, CHANGE_REPORT_MAX, held
      );
    } catch (ignore) {}
  });
}
function discardChangeSet(changeSetId) {
  var file = changeSetFile(changeSetId);
  var held = safeBytes(file, CHANGE_REPORT_MAX, true);
  var index = readChangeIndex();
  if (index && index.ids.indexOf(changeSetId) >= 0) {
    index.ids = index.ids.filter(function (id) { return id !== changeSetId; });
    atomicWrite(CHANGE_INDEX_FILE, Buffer.from(JSON.stringify(index, null, 2) + '\n'));
  }
  if (held !== null && !fileGuards.unlinkRegularFileMatchingUnder(
    paths.PROJECT_ROOT, REPORTS_DIR, file, CHANGE_REPORT_MAX, held
  )) throw new Error('change-set-delete-unproven');
  if (!index) throw new Error('change-set-index-invalid');
}
function writeChangeSet(changeSet) {
  var artifact = writeChangeSetArtifact(changeSet);
  try {
    indexChangeSet(changeSet.changeSetId);
  } catch (error) {
    if (artifact.created) {
      try { discardChangeSet(changeSet.changeSetId); } catch (ignore) {}
    }
    throw error;
  }
  return artifact.file;
}
function boundedString(value, max) { var out = String(value == null ? '' : value); return out.length <= max ? out : out.slice(0, max); }
function redact(value, key, depth) {
  if (depth > 12) return null;
  if (value === null || typeof value === 'boolean' || (typeof value === 'number' && Number.isFinite(value))) return value;
  if (typeof value === 'string') {
    if (/token|secret|password|cookie|authorization|credential|requestbody|headers?/i.test(String(key || ''))) return '[redacted]';
    return boundedString(value.replace(/Bearer\s+[A-Za-z0-9._~+\/-]+/gi, 'Bearer [redacted]'), 4096);
  }
  if (Array.isArray(value)) return value.slice(0, 1000).map(function (item) { return redact(item, key, depth + 1); });
  if (!value || typeof value !== 'object') return null;
  var out = {};
  Object.keys(value).slice(0, 200).forEach(function (childKey) {
    if (/token|secret|password|cookie|authorization|credential|requestbody|headers?/i.test(childKey)) return;
    out[childKey] = redact(value[childKey], childKey, depth + 1);
  });
  return out;
}
function writeReport(type, jobId, report) {
  var clean = redact(report, '', 0);
  if (!clean || clean.schemaVersion !== 1 || clean.jobId !== jobId || clean.reportType !== type) throw new Error('report-contract-invalid');
  var bytes = Buffer.from(JSON.stringify(clean, null, 2) + '\n');
  atomicWrite(reportFile(type, jobId), bytes);
  return clean;
}
function readReport(type, jobId) {
  var bytes;
  try { bytes = safeBytes(reportFile(type, jobId), REPORT_MAX, true); } catch (e) { return null; }
  if (bytes === null) return null;
  try {
    var parsed = JSON.parse(bytes.toString('utf8'));
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) && parsed.schemaVersion === 1 &&
      parsed.jobId === jobId && parsed.reportType === type ? parsed : null;
  } catch (e2) { return null; }
}
function initialIndex() { return { schemaVersion: 1, jobs: [] }; }
function validateIndex(value) {
  if (!exactKeys(value, ['jobs', 'schemaVersion']) || value.schemaVersion !== 1 || !Array.isArray(value.jobs) || value.jobs.length > RETENTION) return false;
  var jobIds = Object.create(null), idempotencyKeys = Object.create(null);
  return value.jobs.every(function (row) {
    if (!exactKeys(row, ['environmentId', 'finishedAt', 'idempotencyFingerprint', 'idempotencyKey', 'jobId', 'reportType', 'startedAt', 'state'])) return false;
    if (jobIds[row.jobId] || idempotencyKeys[row.idempotencyKey]) return false;
    jobIds[row.jobId] = 1; idempotencyKeys[row.idempotencyKey] = 1;
    return JOB_RE.test(String(row.jobId || '')) && (row.reportType === 'probe' || row.reportType === 'refresh') &&
      ['local', 'dev', 'stage', 'prod'].indexOf(row.environmentId) >= 0 && ['queued', 'running', 'success', 'partial', 'failed', 'interrupted'].indexOf(row.state) >= 0 &&
      typeof row.startedAt === 'string' && Number.isFinite(Date.parse(row.startedAt)) &&
      (row.finishedAt === null || (typeof row.finishedAt === 'string' && Number.isFinite(Date.parse(row.finishedAt)))) &&
      /^[A-Za-z0-9._:-]{8,128}$/.test(String(row.idempotencyKey || '')) && /^sha256:[a-f0-9]{64}$/.test(String(row.idempotencyFingerprint || ''));
  });
}
function readIndex() {
  try {
    var bytes = safeBytes(INDEX_FILE, INDEX_MAX, true);
    if (bytes === null) return initialIndex();
    var parsed = JSON.parse(bytes.toString('utf8'));
    return validateIndex(parsed) ? parsed : null;
  } catch (e) { return null; }
}
function writeIndex(index) { atomicWrite(INDEX_FILE, Buffer.from(JSON.stringify(index, null, 2) + '\n')); }
function serialize(fn) {
  var next = writeQueue.then(fn, fn);
  writeQueue = next.catch(function () {});
  return next;
}
function upsert(row) {
  return serialize(function () {
    var index = readIndex();
    if (!index) throw new Error('history-index-invalid');
    index.jobs = index.jobs.filter(function (item) { return item.jobId !== row.jobId; });
    index.jobs.unshift(row);
    var removed = index.jobs.slice(RETENTION);
    index.jobs = index.jobs.slice(0, RETENTION);
    writeIndex(index);
    removed.forEach(function (item) {
      var file = reportFile(item.reportType, item.jobId);
      try {
        var held = safeBytes(file, REPORT_MAX, true);
        if (held !== null) fileGuards.unlinkRegularFileMatchingUnder(
          paths.PROJECT_ROOT, REPORTS_DIR, file, REPORT_MAX, held
        );
      } catch (ignore) {}
    });
    return row;
  });
}
function findIdempotency(key) {
  var index = readIndex();
  if (!index) throw new Error('history-index-invalid');
  return index.jobs.find(function (row) { return row.idempotencyKey === key; }) || null;
}
function findJob(jobId) {
  var index = readIndex();
  return index ? index.jobs.find(function (row) { return row.jobId === jobId; }) || null : null;
}
function findPreview(previewId) {
  var index = readIndex();
  if (!index) return null;
  var rows = index.jobs.filter(function (row) { return row.reportType === 'probe'; });
  for (var i = 0; i < rows.length; i++) {
    var report = readReport('probe', rows[i].jobId);
    if (report && report.previewId === previewId) return report;
  }
  return null;
}
function latest(reportType, environmentId) {
  var index = readIndex();
  if (!index) return null;
  var row = index.jobs.find(function (item) {
    return item.reportType === reportType && item.environmentId === environmentId;
  });
  return row ? readReport(row.reportType, row.jobId) : null;
}
function list(cursor, limit) {
  limit = Number.isInteger(limit) ? Math.max(1, Math.min(100, limit)) : 20;
  var index = readIndex();
  if (!index) return { ok: false, error: 'history-index-invalid' };
  var page = historyPagination.page(index.jobs, cursor, limit, 'jobId', JOB_RE);
  if (!page.ok) return page;
  var items = page.rows.map(function (row) {
    var report = readReport(row.reportType, row.jobId);
    return { jobId: row.jobId, reportType: row.reportType, environmentId: row.environmentId, state: row.state,
      startedAt: row.startedAt, finishedAt: row.finishedAt, summary: report && (report.delta || report.summary || null) };
  });
  return { ok: true, items: items, nextCursor: page.nextCursor };
}
function recoverInterrupted() {
  return serialize(function () {
    var index = readIndex(), changed = false, now = new Date().toISOString();
    if (!index) throw new Error('history-index-invalid');
    index.jobs.forEach(function (row) {
      if (row.state !== 'queued' && row.state !== 'running') return;
      var terminal = readReport(row.reportType, row.jobId);
      var terminalState = terminal && ['success', 'partial', 'failed'].indexOf(terminal.state) >= 0 &&
        terminal.environmentId === row.environmentId && typeof terminal.finishedAt === 'string' && Number.isFinite(Date.parse(terminal.finishedAt));
      if (terminalState && row.reportType === 'probe' && terminal.state === 'success') {
        terminalState = PREVIEW_RE.test(String(terminal.previewId || '')) && /^sha256:[a-f0-9]{64}$/.test(String(terminal.sourceFingerprint || ''));
      }
      if (terminalState && row.reportType === 'refresh' && (terminal.state === 'success' || terminal.state === 'partial')) {
        var current = generation.current();
        terminalState = !!(current.ok && current.mode === 'generation' && current.manifest &&
          current.manifest.generationId === terminal.committedGenerationId && current.environmentId === row.environmentId &&
          current.snapshotHash === terminal.currentHash);
      }
      if (terminalState) {
        row.state = terminal.state; row.finishedAt = terminal.finishedAt; changed = true; return;
      }
      row.state = 'interrupted'; row.finishedAt = now; changed = true;
      writeReport(row.reportType, row.jobId, { schemaVersion: 1, reportType: row.reportType, jobId: row.jobId,
        state: 'interrupted', environmentId: row.environmentId, startedAt: row.startedAt, finishedAt: now,
        error: { code: 'job-interrupted', message: 'The site restarted before the sidecar process could be proven alive.' } });
    });
    if (changed) writeIndex(index);
    return changed;
  });
}

function clearAll() {
  return serialize(function () {
    var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, REPORTS_DIR, 512);
    if (!listed.ok) throw new Error('history-index-invalid');
    for (var i = 0; i < listed.names.length; i++) {
      var name = listed.names[i];
      if (name === '.gitkeep') continue;
      var target = path.join(REPORTS_DIR, name);
      var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, REPORTS_DIR, target);
      if (!inspected || inspected.status === 'missing') continue;
      if (inspected.status !== 'present' || !inspected.stat || !inspected.stat.isFile() || inspected.stat.isSymbolicLink() ||
          !fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, REPORTS_DIR, target, { allowMissing: true })) {
        throw new Error('history-index-invalid');
      }
    }
    var after = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, REPORTS_DIR, 2);
    if (!after.ok || after.names.some(function (name) { return name !== '.gitkeep'; })) throw new Error('history-index-invalid');
    return true;
  });
}

module.exports = {
  REPORTS_DIR: REPORTS_DIR,
  JOB_RE: JOB_RE,
  PREVIEW_RE: PREVIEW_RE,
  reportFile: reportFile,
  writeReport: writeReport,
  readReport: readReport,
  upsert: upsert,
  findIdempotency: findIdempotency,
  findJob: findJob,
  findPreview: findPreview,
  latest: latest,
  list: list,
  recoverInterrupted: recoverInterrupted,
  clearAll: clearAll,
  writeChangeSet: writeChangeSet,
  discardChangeSet: discardChangeSet,
  redact: redact,
  _readIndexForTests: readIndex,
  _pageRowsForTests: function (rows, cursor, limit) {
    return historyPagination.page(rows, cursor, limit, 'jobId', JOB_RE);
  }
};
