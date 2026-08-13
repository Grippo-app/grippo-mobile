'use strict';

// A short critical section for publishing the ONE durable integration WAL.
// The WAL remains the repository-wide integration mutex for the transaction's
// whole lifetime; this guard exists only to make the read-empty -> create-WAL
// decision atomic across different stem filenames. It is released before any
// product mutation or finalizer child starts.

var crypto = require('crypto');
var os = require('os');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var writerLeases = require('../../tasks/writer-leases.cjs');

var DIR = paths.INTEGRATIONS_DIR;
var ROOT = paths.WORKTREE_RECORDS_AUTHORITY_ROOT;
var FILE = path.join(DIR, '.wal-publication-guard');
var MAX_BYTES = 4096;
var TOKEN_RE = /^[a-f0-9]{32}$/;
var OWNER_FIELDS = ['createdAt', 'hostname', 'pid', 'processStartId', 'token', 'version'].sort();

function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.join('\0');
}
function valid(record) {
  return exactKeys(record, OWNER_FIELDS) && record.version === 1 &&
    TOKEN_RE.test(String(record.token || '')) &&
    typeof record.hostname === 'string' && record.hostname.length > 0 && record.hostname.length <= 255 &&
    Number.isSafeInteger(record.pid) && record.pid > 0 &&
    writerLeases.PROCESS_START_ID_RE.test(String(record.processStartId || '')) &&
    typeof record.createdAt === 'string' && Number.isFinite(Date.parse(record.createdAt)) &&
    new Date(record.createdAt).toISOString() === record.createdAt;
}
function readCurrent() {
  var entry = fileGuards.inspectEntryUnder(ROOT, DIR, FILE);
  if (!entry || entry.status === 'missing') return { ok: true, record: null };
  if (entry.status !== 'present') return { ok: false, code: 'INTEGRATION_PUBLICATION_GUARD_UNSAFE' };
  var bounded = fileGuards.boundedRegularFileUnder(ROOT, DIR, FILE, MAX_BYTES);
  if (!bounded) return { ok: false, code: 'INTEGRATION_PUBLICATION_GUARD_UNSAFE' };
  var record;
  try { record = JSON.parse(bounded.bytes.toString('utf8')); }
  catch (error) { return { ok: false, code: 'INTEGRATION_PUBLICATION_GUARD_INVALID' }; }
  if (!valid(record) || !Buffer.from(JSON.stringify(record) + '\n').equals(bounded.bytes)) {
    return { ok: false, code: 'INTEGRATION_PUBLICATION_GUARD_INVALID' };
  }
  return { ok: true, record: record, bytes: bounded.bytes, proof: bounded.stat };
}
function removeExact(current) {
  var removed = fileGuards.unlinkRegularFileMatchingResultUnder(ROOT, DIR, FILE, MAX_BYTES,
    { bytes: current.bytes, proof: current.proof });
  return !!removed && removed.ok && (removed.committed || removed.alreadyMissing);
}
function acquire() {
  var processStartId;
  try { processStartId = writerLeases.captureProcessStartId(process.pid); }
  catch (error) { processStartId = null; }
  if (!writerLeases.PROCESS_START_ID_RE.test(String(processStartId || ''))) {
    return { ok: false, code: 'INTEGRATION_PUBLICATION_IDENTITY_UNAVAILABLE',
      message: 'the WAL publication owner process generation could not be proven' };
  }
  var record = {
    version: 1, token: crypto.randomBytes(16).toString('hex'), hostname: os.hostname(),
    pid: process.pid, processStartId: processStartId, createdAt: new Date().toISOString()
  };
  var bytes = Buffer.from(JSON.stringify(record) + '\n');
  for (var attempt = 0; attempt < 2; attempt++) {
    var published = fileGuards.publishNoClobberRegularFileUnder(ROOT, DIR, FILE, bytes,
      { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: MAX_BYTES });
    if (published && published.ok) {
      if (!fileGuards.fsyncRegularFileUnder(ROOT, DIR, FILE) ||
          !fileGuards.fsyncDirectoryUnder(ROOT, DIR)) {
        var uncertain = readCurrent();
        if (uncertain.ok && uncertain.record && uncertain.record.token === record.token) removeExact(uncertain);
        return { ok: false, code: 'INTEGRATION_PUBLICATION_GUARD_UNDURABLE',
          message: 'the WAL publication guard could not be made durable' };
      }
      var current = readCurrent();
      if (!current.ok || !current.record || current.record.token !== record.token ||
          current.record.pid !== process.pid || current.record.processStartId !== processStartId) {
        return { ok: false, code: 'INTEGRATION_PUBLICATION_GUARD_UNPROVEN',
          message: 'the WAL publication guard generation changed during acquisition' };
      }
      return { ok: true, token: record.token, record: record, bytes: current.bytes, proof: current.proof };
    }
    if (!published || published.code !== 'exists') {
      return { ok: false, code: 'INTEGRATION_PUBLICATION_GUARD_UNAVAILABLE',
        message: 'the WAL publication guard could not be created' };
    }
    var held = readCurrent();
    if (!held.ok || !held.record) return held;
    if (held.record.hostname !== os.hostname()) {
      return { ok: false, code: 'INTEGRATION_PUBLICATION_BUSY',
        message: 'another host may be publishing an integration WAL' };
    }
    var state = writerLeases.processIdentityState(held.record.pid, held.record.processStartId);
    if ((state === 'dead' || state === 'reused') && removeExact(held)) continue;
    return { ok: false,
      code: state === 'match' || state === 'pid-live' ? 'INTEGRATION_PUBLICATION_BUSY' :
        'INTEGRATION_PUBLICATION_GUARD_UNPROVEN',
      message: state === 'match' || state === 'pid-live' ?
        'another process is publishing an integration WAL' :
        'the current WAL publication owner cannot be classified safely' };
  }
  return { ok: false, code: 'INTEGRATION_PUBLICATION_GUARD_UNAVAILABLE',
    message: 'the stale WAL publication guard could not be replaced' };
}
function release(handle) {
  if (!handle || !TOKEN_RE.test(String(handle.token || ''))) return false;
  var current = readCurrent();
  if (!current.ok) return false;
  if (!current.record) return true;
  if (current.record.token !== handle.token || current.record.hostname !== os.hostname() ||
      current.record.pid !== process.pid || current.record.processStartId !== handle.record.processStartId) return false;
  return removeExact(current);
}

// Read-only projection for every other mutation surface. A stale or unreadable
// guard is still active until acquire() proves its exact owner generation dead
// and removes that exact file; readers never guess that it is safe.
function issue() {
  var current = readCurrent();
  if (!current.ok) return { active: true, reason: current.code, token: null };
  if (!current.record) return { active: false, reason: null, token: null };
  if (current.record.hostname !== os.hostname()) {
    return { active: true, reason: 'integration-publication-remote', token: current.record.token };
  }
  var state = writerLeases.processIdentityState(current.record.pid, current.record.processStartId);
  return { active: true,
    reason: state === 'match' || state === 'pid-live' ? 'integration-publication-active' :
      (state === 'dead' || state === 'reused' ? 'integration-publication-recovery-required' :
        'integration-publication-owner-unproven'),
    token: current.record.token };
}

module.exports = { FILE: FILE, acquire: acquire, release: release, issue: issue };
