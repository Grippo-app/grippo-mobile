'use strict';

// Strict read-only projection of deterministic backlog creation receipts.
// Every path operation is pinned through the isolated filesystem worker. The
// pure tasks contract is the sole receipt/CAS-name schema; Site never reads CAS
// operation internals because only the Python recovery owner may interpret
// their publication lattice.

var path = require('path');
var os = require('os');
var crypto = require('crypto');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var writerLeaseInspector = require('./writer-lease-inspector');
var markerContract = require('../../tasks/creation-marker-contract.cjs');

var DIR = paths.TASK_CREATIONS_DIR;
var NAME_RE = /^[a-f0-9]{64}\.json$/;
var MAX_MARKER = markerContract.MAX_BYTES;
var MAX_ENTRIES = 10000;
var MAX_RECORDS = 1000;
var MAX_TOTAL_BYTES = 8 * 1024 * 1024;
// A combined-publication projection is an in-process, non-serializable proof:
// edit-markers may consume only an object that this module produced after
// independently proving the exact creation marker generation and writer
// lease.  A caller-supplied boolean/object therefore cannot turn recovery
// findings into queued status.
var combinedRecoveryProjections = new WeakSet();

function bounded(value, max) { var text = String(value == null ? '' : value); max = max || 300; return text.length <= max ? text : text.slice(0, max - 1) + '…'; }
function digest(bytes) { return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'); }
function issue(code, message, filename, keyHash, file) {
  return { code: code, message: bounded(message), filename: filename || null, keyHash: keyHash || null, file: file || null };
}
function scanRaw() {
  var out = { blocking: [], incomplete: [], completed: [], markerProofs: [], snapshotInputs: [], cas: [], truncated: false };
  var directory = fileGuards.realDirectoryUnder(paths.TASK_CREATIONS_AUTHORITY_ROOT, DIR, { allowMissing: true });
  if (!directory) { out.blocking.push(issue('CREATION_MARKER_DIR_UNSAFE', 'creation marker directory must be a real anchored directory', null, null, DIR)); return out; }
  if (!directory.exists) return out;
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.TASK_CREATIONS_AUTHORITY_ROOT, DIR, MAX_ENTRIES);
  if (!listed.ok) {
    out.blocking.push(issue(listed.code === 'directory-entry-limit' ? 'CREATION_MARKER_DIR_TOO_LARGE' : 'CREATION_MARKER_DIR_UNAVAILABLE',
      listed.code === 'directory-entry-limit' ? 'creation marker directory exceeds the bounded entry limit' : 'creation marker directory cannot be enumerated safely', null, null, DIR));
    out.truncated = listed.code === 'directory-entry-limit'; return out;
  }
  var names = listed.names.slice().sort();
  out.snapshotInputs.push({ owner: 'creation-markers', kind: 'directory', path: DIR,
    hash: digest(Buffer.from(JSON.stringify(names), 'utf8')), size: names.length });
  names.forEach(function (name) {
    var classification = markerContract.durableCas.classifyName(name);
    if (classification === null) return;
    var file = path.join(DIR, name);
    var entry = fileGuards.inspectEntryUnder(paths.TASK_CREATIONS_AUTHORITY_ROOT, DIR, file);
    if (classification === 'recovery-required' && entry.status === 'present' && entry.stat && entry.stat.isDirectory() && !entry.stat.isSymbolicLink()) {
      out.cas.push({ name: name, path: file });
      out.blocking.push(issue('CREATION_MARKER_CAS_RECOVERY_REQUIRED', 'durable creation CAS publication requires exact Python-owner recovery', name, null, file));
    } else out.blocking.push(issue('CREATION_MARKER_CAS_NAME_UNSAFE', 'durable creation CAS entry has an unsafe name or type', name, null, file));
  });
  var markerNames = names.filter(function (name) { return name.endsWith('.json'); });
  if (markerNames.length > MAX_RECORDS) { out.blocking.push(issue('CREATION_MARKER_SCAN_LIMIT', 'creation marker count exceeds its bounded limit', null, null, DIR)); out.truncated = true; return out; }
  var total = 0, setEntries = [];
  markerNames.forEach(function (name) {
    var file = path.join(DIR, name);
    if (!NAME_RE.test(name)) { out.blocking.push(issue('CREATION_MARKER_NAME_INVALID', 'unexpected creation marker filename', name, null, file)); return; }
    var keyHash = 'sha256:' + name.slice(0, -5);
    var read = fileGuards.boundedRegularFileUnder(paths.TASK_CREATIONS_AUTHORITY_ROOT, DIR, file, MAX_MARKER);
    if (!read) {
      var inspected = fileGuards.inspectEntryUnder(paths.TASK_CREATIONS_AUTHORITY_ROOT, DIR, file);
      var tooLarge = inspected && inspected.status === 'present' && inspected.stat && inspected.stat.isFile() &&
        !inspected.stat.isSymbolicLink() && inspected.stat.size > MAX_MARKER;
      out.blocking.push(issue(tooLarge ? 'CREATION_MARKER_TOO_LARGE' : 'CREATION_MARKER_UNSAFE',
        tooLarge ? 'creation marker exceeds its v1 size limit' : 'creation marker must be a stable bounded regular file', name, keyHash, file)); return;
    }
    total += read.bytes.length;
    var contentHash = digest(read.bytes);
    out.snapshotInputs.push({ owner: 'creation-markers', kind: 'marker', path: file, hash: contentHash, size: read.bytes.length });
    if (total > MAX_TOTAL_BYTES) { out.truncated = true; return; }
    var parsed;
    try { parsed = JSON.parse(read.bytes.toString('utf8')); }
    catch (error) { out.blocking.push(issue('CREATION_MARKER_CORRUPT', error && error.message || error, name, keyHash, file)); return; }
    try { markerContract.validate(parsed, name); }
    catch (error) { out.blocking.push(issue('CREATION_MARKER_INVALID', error && error.message || error, name, keyHash, file)); return; }
    setEntries.push({ filename: name, value: parsed });
    out.markerProofs.push({ record: parsed, contentHash: contentHash, file: file });
    (parsed.status === 'completed' ? out.completed : out.incomplete).push(parsed);
  });
  if (total > MAX_TOTAL_BYTES) out.blocking.push(issue('CREATION_MARKER_SCAN_LIMIT', 'creation marker bytes exceed the bounded aggregate limit', null, null, DIR));
  try { markerContract.validateSet(setEntries); }
  catch (error) { out.blocking.push(issue('CREATION_MARKER_INVALID', error && error.message || error, null, null, DIR)); }
  var after = fileGuards.boundedDirectoryNamesUnder(paths.TASK_CREATIONS_AUTHORITY_ROOT, DIR, MAX_ENTRIES);
  if (!after.ok || JSON.stringify(after.names.slice().sort()) !== JSON.stringify(names)) {
    out.blocking.push(issue('CREATION_MARKER_DIR_CHANGED', 'creation marker directory changed during its integrity snapshot', null, null, DIR));
  }
  out.incomplete.forEach(function (record) {
    out.blocking.push(issue('CREATION_INCOMPLETE', 'deterministic backlog creation requires recovery', record.keyHash.slice(7) + '.json', record.keyHash,
      path.join(DIR, record.keyHash.slice(7) + '.json')));
  });
  return out;
}
function scan() {
  var raw = scanRaw();
  return {
    blocking: raw.blocking.map(function (row) { return { code: row.code, message: row.message, filename: row.filename, keyHash: row.keyHash }; }),
    incomplete: raw.incomplete.map(function (record) { return { keyHash: record.keyHash, transactionId: record.transactionId,
      stem: record.stem || null, number: record.number || null, phase: record.phase, revision: record.revision,
      updatedAt: record.updatedAt }; }),
    completedCount: raw.completed.length
  };
}
function blockingIssue(allowedKeyHash, allowAllIncomplete) {
  var state = scan();
  for (var i = 0; i < state.blocking.length; i++) {
    var current = state.blocking[i];
    if (current.code === 'CREATION_INCOMPLETE' && allowAllIncomplete) continue;
    if (current.code === 'CREATION_INCOMPLETE' && allowedKeyHash && current.keyHash === allowedKeyHash && state.incomplete.length === 1) continue;
    return current;
  }
  return null;
}
function samePath(left, right) { return path.resolve(left) === path.resolve(right); }
function fixtureProof(context, inspection) {
  var canonicalProject = path.dirname(paths.ORCHESTRATOR_DIR);
  var variable = process.env.CREATE_BACKLOG_TEST_ALLOW_UNLEASED;
  return context.mode === 'fixture-unleased' && variable === '1' && !samePath(paths.PROJECT_ROOT, canonicalProject) &&
    samePath(paths.WRITER_AUTHORITY_ROOT, paths.PROJECT_ROOT) && samePath(paths.TASK_CREATIONS_AUTHORITY_ROOT, paths.PROJECT_ROOT) &&
    inspection.stable === true && !inspection.truncated && inspection.active.length === 0 && inspection.stale.length === 0 && inspection.issues.length === 0 &&
    writerLeaseInspector.exactProcess(context.pid, context.processStartId);
}
function ownedLeaseProof(context, inspection) {
  if (inspection.stable !== true || inspection.truncated || inspection.issues.length || inspection.stale.length ||
      !writerLeaseInspector.exactProcess(context.pid, context.processStartId)) return false;
  var active = inspection.active;
  if (context.mode === 'owned-lease') {
    if (active.length !== 1) return false;
    var row = active[0];
    return row.leaseId === context.authorityLeaseId && row.kind === 'workspace-session' && row.key === context.publicationKey &&
      row.stem === null && row.childPid === context.pid && row.childProcessStartId === context.processStartId &&
      row.owner && row.owner.hostname === os.hostname() && row.unverified === false && row.expiresAt === null;
  }
  if (context.mode !== 'nested-guard' || active.length !== 2) return false;
  var guard = active.find(function (row) { return row.leaseId === context.publicationGuardLeaseId; });
  var parent = active.find(function (row) { return row.leaseId === context.authorityLeaseId; });
  return !!guard && !!parent && guard.kind === 'lock-writer' && guard.key === context.publicationKey && guard.stem === null &&
    guard.owner && guard.owner.pid === context.pid && guard.owner.processStartId === context.processStartId &&
    guard.childPid === null && guard.childProcessStartId === null && guard.unverified === false && guard.expiresAt === null &&
    typeof guard.delegationHash === 'string' && /^sha256:[a-f0-9]{64}$/.test(guard.delegationHash) &&
    parent.kind === 'task-session' && parent.stem !== null && parent.sessionId !== null && parent.sessionId === guard.sessionId &&
    parent.owner && parent.owner.hostname === os.hostname() &&
    (writerLeaseInspector.exactProcess(parent.owner.pid, parent.owner.processStartId) ||
      parent.childPid !== null && writerLeaseInspector.exactProcess(parent.childPid, parent.childProcessStartId)) &&
    parent.unverified === false;
}
function activeTransactions(raw, scope) {
  var active = new Set(), context = scope && scope.creationContext;
  if (!context || raw.truncated || raw.cas.length || raw.blocking.some(function (row) { return row.code !== 'CREATION_INCOMPLETE'; })) return active;
  var own = raw.markerProofs.find(function (proof) {
    var record = proof.record;
    return record.status === 'incomplete' && record.keyHash === context.keyHash && record.transactionId === context.transactionId &&
      record.revision === context.revision && proof.contentHash === context.contentHash;
  });
  if (!own) return active;
  var inspection = scope && scope.writerLeaseInspection || writerLeaseInspector.inspect();
  if (!fixtureProof(context, inspection) && !ownedLeaseProof(context, inspection)) return active;
  if (context.publicationKey === 'task:recover-backlog-creations') {
    raw.incomplete.forEach(function (record) { active.add(record.transactionId); });
  } else if (raw.incomplete.length === 1) active.add(own.record.transactionId);
  return active;
}
function freezeEnvelope(envelope) {
  envelope.statuses.forEach(Object.freeze);
  envelope.findings.forEach(function (row) { Object.freeze(row.paths); Object.freeze(row); });
  envelope.snapshotInputs.forEach(Object.freeze);
  Object.freeze(envelope.statuses); Object.freeze(envelope.findings); Object.freeze(envelope.snapshotInputs);
  return Object.freeze(envelope);
}
function inspectIntegrity(scope) {
  var raw = scanRaw();
  var owned = activeTransactions(raw, scope);
  var statuses = raw.incomplete.map(function (record) { return { owner: 'creation-markers', kind: 'marker', stem: record.stem || null,
    state: owned.has(record.transactionId) ? 'active' : 'recovery-required', phase: record.phase, createdAt: record.createdAt, updatedAt: record.updatedAt,
    contentHash: digest(Buffer.from(record.transactionId, 'utf8')) }; }).concat(raw.cas.map(function (row) {
      return { owner: 'creation-markers', kind: 'durable-cas', stem: null, state: 'recovery-required', contentHash: digest(Buffer.from(row.name, 'utf8')) };
    }));
  var findings = raw.blocking.filter(function (row) {
    if (row.code !== 'CREATION_INCOMPLETE') return true;
    var matching = raw.incomplete.find(function (record) { return record.keyHash === row.keyHash; });
    return !matching || !owned.has(matching.transactionId);
  }).map(function (row) { return { code: row.code, severity: 'error',
    stem: row.code === 'CREATION_INCOMPLETE' && raw.incomplete.length === 1 ? raw.incomplete[0].stem : null,
    paths: row.file ? [row.file] : [], message: row.message,
    recovery: 'Run the exact deterministic creation recovery authority; do not delete creation/CAS state manually.' }; });
  var envelope = freezeEnvelope({ version: 1, owner: 'creation-markers', statuses: statuses, findings: findings,
    snapshotInputs: raw.snapshotInputs, truncated: raw.truncated });
  var exactRecoverAll = !!(scope && scope.combinedPublicationRecoveryRequested === true && scope.creationContext &&
    scope.creationContext.publicationKey === 'task:recover-backlog-creations' && raw.incomplete.length > 0 &&
    owned.size === raw.incomplete.length && !raw.truncated && raw.cas.length === 0 &&
    raw.blocking.every(function (row) { return row.code === 'CREATION_INCOMPLETE'; }));
  var identities = exactRecoverAll ? raw.incomplete.map(function (record) {
    return Object.freeze({ transactionId: record.transactionId, stem: record.stem || null,
      number: Number.isSafeInteger(record.number) ? record.number : null });
  }) : [];
  Object.freeze(identities);
  var projection = Object.freeze({ envelope: envelope, combinedRecoveryActive: exactRecoverAll,
    activeIdentities: identities });
  combinedRecoveryProjections.add(projection);
  return projection;
}
function isCombinedRecoveryProjection(value) {
  return !!value && combinedRecoveryProjections.has(value) && value.combinedRecoveryActive === true;
}
function scanIntegrity(scope) {
  return inspectIntegrity(scope).envelope;
}
function dirMtime() {
  var directory = fileGuards.realDirectoryUnder(paths.TASK_CREATIONS_AUTHORITY_ROOT, DIR, { allowMissing: true });
  return directory && directory.exists && directory.stat ? directory.stat.mtimeMs : 0;
}

module.exports = { scan: scan, blockingIssue: blockingIssue, dirMtime: dirMtime, scanIntegrity: scanIntegrity,
  inspectIntegrity: inspectIntegrity, isCombinedRecoveryProjection: isCombinedRecoveryProjection };
