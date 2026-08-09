'use strict';

// Anchored, read-only projection of writer leases for the Site/runtime
// integrity boundary. The mutation owner and its read-only scan share the same
// root/component-pinned BigInt guard in tasks/writer-leases.cjs; this adapter
// enforces the exact v1 projection and redacts plaintext lease capabilities.

var crypto = require('crypto');
var paths = require('./paths');
var contract = require('../../tasks/writer-leases.cjs');

var MAX_TOTAL_BYTES = 256 * 1024;
var RECORD_FIELDS = ['childPid', 'childProcessStartId', 'createdAt', 'delegationHash', 'expiresAt', 'key', 'kind',
  'leaseId', 'owner', 'sessionId', 'stem', 'token', 'unverified', 'unverifiedReason', 'updatedAt', 'version'].sort();
var OWNER_FIELDS = ['hostname', 'pid', 'processStartId', 'startedAt'].sort();

function digest(bytes) { return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'); }
function sameKeys(value, fields) {
  return value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === fields.join('\0');
}
function issue(code, message, leaseId) {
  return { code: code, message: String(message || code).slice(0, 500), leaseId: leaseId || null };
}
function exactProcess(pid, processStartId) {
  try { return contract.processIdentityMatches(pid, processStartId); }
  catch (error) { return false; }
}
function inspect() {
  var out = { version: 1, active: [], stale: [], issues: [], snapshotInputs: [], truncated: false, stable: true };
  var scan;
  try { scan = contract.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT); }
  catch (error) {
    out.issues.push(issue('WRITER_LEASE_DIR_UNSAFE', error && error.message || error));
    out.stable = false;
    return out;
  }
  out.issues = (scan.issues || []).map(function (row) { return issue(row.code || 'WRITER_LEASE_INVALID', row.message, row.leaseId); });
  function accept(record, active) {
    var invalid = contract.validateRecord(record, record && record.leaseId);
    if (invalid || !sameKeys(record, RECORD_FIELDS) || !sameKeys(record.owner, OWNER_FIELDS)) {
      out.issues.push(issue('WRITER_LEASE_INVALID', invalid || 'writer lease fields do not match the exact v1 schema', record && record.leaseId));
      return;
    }
    var projected = Object.assign({}, record, { owner: Object.assign({}, record.owner), token: null });
    (active ? out.active : out.stale).push(projected);
  }
  (scan.active || []).forEach(function (record) { accept(record, true); });
  (scan.stale || []).forEach(function (record) { accept(record, false); });
  var projection = Buffer.from(JSON.stringify({ active: out.active, stale: out.stale, issues: out.issues }), 'utf8');
  if (projection.length > MAX_TOTAL_BYTES || out.issues.some(function (row) { return row.code === 'WRITER_LEASE_SCAN_LIMIT'; })) {
    out.truncated = true;
    if (!out.issues.some(function (row) { return row.code === 'WRITER_LEASE_SCAN_LIMIT'; })) {
      out.issues.push(issue('WRITER_LEASE_SCAN_LIMIT', 'writer lease projection exceeds the bounded aggregate byte limit'));
    }
  }
  out.snapshotInputs.push({ owner: 'finalizations', kind: 'writer-lease-directory', path: paths.WRITER_LEASES_DIR,
    hash: digest(projection), size: projection.length });
  if (out.issues.length) out.stable = false;
  out.active.sort(function (a, b) { return a.leaseId < b.leaseId ? -1 : a.leaseId > b.leaseId ? 1 : 0; });
  out.stale.sort(function (a, b) { return a.leaseId < b.leaseId ? -1 : a.leaseId > b.leaseId ? 1 : 0; });
  return out;
}

module.exports = { inspect: inspect, exactProcess: exactProcess };
