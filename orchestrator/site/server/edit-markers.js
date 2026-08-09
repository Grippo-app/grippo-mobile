'use strict';

// Sanitized, strictly anchored Site projection of durable backlog-edit
// receipts. The pure tasks contract remains the sole record/CAS-name schema;
// the Site never calls its path-based scanner and never reads CAS internals.

var path = require('path');
var os = require('os');
var crypto = require('crypto');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var writerLeaseInspector = require('./writer-lease-inspector');
var creationMarkers = require('./creation-markers');
var contract = require(path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'edit-marker-contract.cjs'));

var MAX_ENTRIES = 10000;
var MAX_RECORDS = 1000;
var MAX_TOTAL_BYTES = 8 * 1024 * 1024;

function bounded(value, max) {
  var text = String(value == null ? '' : value); max = max || 500;
  return text.length <= max ? text : text.slice(0, max - 1) + '…';
}
function digest(bytes) { return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'); }
function issue(code, stem, file, message) { return { code: code, stem: stem || null, file: file || null, message: bounded(message) }; }
function scanRaw(stem) {
  var out = { incomplete: [], completed: [], markerProofs: [], issues: [], snapshotInputs: [], cas: [], truncated: false };
  var directory = fileGuards.realDirectoryUnder(paths.TASK_EDITS_AUTHORITY_ROOT, paths.TASK_EDITS_DIR, { allowMissing: true });
  if (!directory) { out.issues.push(issue('EDIT_MARKER_DIR_UNSAFE', stem, paths.TASK_EDITS_DIR, 'edit marker directory is unsafe')); return out; }
  if (!directory.exists) return out;
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.TASK_EDITS_AUTHORITY_ROOT, paths.TASK_EDITS_DIR, MAX_ENTRIES);
  if (!listed.ok) {
    out.issues.push(issue(listed.code === 'directory-entry-limit' ? 'EDIT_MARKER_SCAN_LIMIT' : 'EDIT_MARKER_DIR_UNSAFE', stem, paths.TASK_EDITS_DIR,
      listed.code === 'directory-entry-limit' ? 'edit marker directory exceeds its bounded entry limit' : 'edit marker directory cannot be enumerated safely'));
    out.truncated = listed.code === 'directory-entry-limit'; return out;
  }
  var names = listed.names.slice().sort();
  out.snapshotInputs.push({ owner: 'edit-markers', kind: 'directory', path: paths.TASK_EDITS_DIR,
    hash: digest(Buffer.from(JSON.stringify(names), 'utf8')), size: names.length });
  names.forEach(function (name) {
    var classification = contract.durableCas.classifyName(name);
    if (classification !== null) {
      var casPath = path.join(paths.TASK_EDITS_DIR, name);
      var entry = fileGuards.inspectEntryUnder(paths.TASK_EDITS_AUTHORITY_ROOT, paths.TASK_EDITS_DIR, casPath);
      if (classification === 'recovery-required' && entry.status === 'present' && entry.stat && entry.stat.isDirectory() && !entry.stat.isSymbolicLink()) {
        out.cas.push({ name: name, path: casPath, state: 'recovery-required' });
        out.issues.push(issue('EDIT_MARKER_CAS_RECOVERY_REQUIRED', null, casPath, 'durable edit CAS publication requires exact Python-owner recovery'));
      } else out.issues.push(issue('EDIT_MARKER_CAS_NAME_UNSAFE', null, casPath, 'durable edit CAS entry has an unsafe name or type'));
    }
  });
  var markerNames;
  if (stem) markerNames = [stem + '.json'];
  else markerNames = names.filter(function (name) { return !name.startsWith('.') && name.endsWith('.json'); });
  if (markerNames.length > MAX_RECORDS) { out.issues.push(issue('EDIT_MARKER_SCAN_LIMIT', null, paths.TASK_EDITS_DIR, 'edit marker count exceeds its bounded limit')); out.truncated = true; return out; }
  var total = 0;
  markerNames.forEach(function (name) {
    var match = contract.NAME_RE.exec(name), file = path.join(paths.TASK_EDITS_DIR, name);
    if (!match) { out.issues.push(issue('EDIT_MARKER_NAME_UNSAFE', null, file, 'unsafe edit marker filename')); return; }
    var entry = fileGuards.inspectEntryUnder(paths.TASK_EDITS_AUTHORITY_ROOT, paths.TASK_EDITS_DIR, file);
    if (stem && entry.status === 'missing') return;
    var read = fileGuards.boundedRegularFileUnder(paths.TASK_EDITS_AUTHORITY_ROOT, paths.TASK_EDITS_DIR, file, contract.MAX_BYTES);
    if (!read) { out.issues.push(issue('EDIT_MARKER_INVALID', match[1], file, 'edit marker is unsafe, unstable, or oversized')); return; }
    total += read.bytes.length;
    var contentHash = digest(read.bytes);
    out.snapshotInputs.push({ owner: 'edit-markers', kind: 'marker', path: file, hash: contentHash, size: read.bytes.length });
    if (total > MAX_TOTAL_BYTES) { out.truncated = true; return; }
    try {
      var record = contract.readRecordBytes(read.bytes, match[1]);
      out.markerProofs.push({ record: record, contentHash: contentHash, file: file });
      (record.status === 'incomplete' ? out.incomplete : out.completed).push(record);
    } catch (error) { out.issues.push(issue('EDIT_MARKER_INVALID', match[1], file, error && error.message || error)); }
  });
  if (total > MAX_TOTAL_BYTES) out.issues.push(issue('EDIT_MARKER_SCAN_LIMIT', null, paths.TASK_EDITS_DIR, 'edit marker bytes exceed the bounded aggregate limit'));
  var transactionIds = out.incomplete.concat(out.completed).map(function (record) { return record.transactionId; });
  if (new Set(transactionIds).size !== transactionIds.length) out.issues.push(issue('EDIT_MARKER_INVALID', null, paths.TASK_EDITS_DIR, 'edit marker transaction ids are not unique'));
  var after = fileGuards.boundedDirectoryNamesUnder(paths.TASK_EDITS_AUTHORITY_ROOT, paths.TASK_EDITS_DIR, MAX_ENTRIES);
  if (!after.ok || JSON.stringify(after.names.slice().sort()) !== JSON.stringify(names)) {
    out.issues.push(issue('EDIT_MARKER_DIR_CHANGED', null, paths.TASK_EDITS_DIR, 'edit marker directory changed during its integrity snapshot'));
  }
  return out;
}
function scan() {
  var raw = scanRaw(null);
  return {
    blocking: raw.issues.map(function (row) { return { code: row.code, stem: row.stem, message: row.message }; }).concat(raw.incomplete.map(function (marker) {
      return { code: 'EDIT_INCOMPLETE', stem: marker.stem, message: 'durable backlog edit requires recovery' };
    })),
    incomplete: raw.incomplete.map(function (marker) {
      return { stem: marker.stem, transactionId: marker.transactionId, phase: marker.phase, revision: marker.revision,
        updatedAt: marker.updatedAt };
    }),
    completedCount: raw.completed.length
  };
}
function blockingIssue(stem, allowAllRecovery) {
  var raw = scanRaw(null);
  if (raw.issues.length) return raw.issues[0];
  if (allowAllRecovery === true || !raw.incomplete.length) return null;
  return { code: 'EDIT_INCOMPLETE', stem: raw.incomplete[0].stem, requestedStem: stem || null,
    message: 'durable backlog edit recovery owns shared task publication for ' + raw.incomplete[0].stem };
}
function samePath(left, right) { return path.resolve(left) === path.resolve(right); }
function fixtureProof(context, inspection) {
  return context.mode === 'fixture-unleased' && process.env.EDIT_BACKLOG_TEST_ALLOW_UNLEASED === '1' &&
    !samePath(paths.PROJECT_ROOT, path.dirname(paths.ORCHESTRATOR_DIR)) && samePath(paths.WRITER_AUTHORITY_ROOT, paths.PROJECT_ROOT) &&
    samePath(paths.TASK_EDITS_AUTHORITY_ROOT, paths.PROJECT_ROOT) && inspection.stable === true && !inspection.truncated &&
    inspection.active.length === 0 && inspection.stale.length === 0 && inspection.issues.length === 0 &&
    writerLeaseInspector.exactProcess(context.pid, context.processStartId);
}
function ownedLeaseProof(context, inspection) {
  if (context.mode !== 'owned-lease' || inspection.stable !== true || inspection.truncated || inspection.issues.length ||
      inspection.stale.length || inspection.active.length !== 1 || !writerLeaseInspector.exactProcess(context.pid, context.processStartId)) return false;
  var row = inspection.active[0];
  var expectedStem = context.publicationKey === 'task:recover-backlog-edits' ? null : context.stem;
  return row.leaseId === context.authorityLeaseId && row.kind === 'workspace-session' && row.key === context.publicationKey &&
    row.stem === expectedStem && row.childPid === context.pid && row.childProcessStartId === context.processStartId &&
    row.owner && row.owner.hostname === os.hostname() &&
    row.unverified === false && row.expiresAt === null;
}
function activeTransactions(raw, scope) {
  var owned = new Set(), context = scope && scope.editContext;
  if (!context || raw.truncated || raw.cas.length || raw.issues.length) return owned;
  var own = raw.markerProofs.find(function (proof) {
    var record = proof.record;
    return record.status === 'incomplete' && record.stem === context.stem && record.transactionId === context.transactionId &&
      record.revision === context.revision && proof.contentHash === context.contentHash;
  });
  if (!own) return owned;
  var inspection = scope && scope.writerLeaseInspection || writerLeaseInspector.inspect();
  if (!fixtureProof(context, inspection) && !ownedLeaseProof(context, inspection)) return owned;
  if (context.publicationKey === 'task:recover-backlog-edits') raw.incomplete.forEach(function (record) { owned.add(record.transactionId); });
  else if (raw.incomplete.length === 1) owned.add(own.record.transactionId);
  return owned;
}
function combinedQueue(raw, scope) {
  var result = { transactions: new Set(), collision: null };
  var projection = scope && scope.combinedCreationProjection;
  if (!creationMarkers.isCombinedRecoveryProjection(projection) || raw.truncated || raw.cas.length || raw.issues.length) return result;
  for (var i = 0; i < projection.activeIdentities.length; i++) {
    var creation = projection.activeIdentities[i];
    for (var j = 0; j < raw.incomplete.length; j++) {
      var edit = raw.incomplete[j];
      var numberMatch = /^TASK_([1-9][0-9]*)_/.exec(edit.stem);
      var editNumber = numberMatch ? Number(numberMatch[1]) : null;
      if (creation.stem !== null && creation.stem === edit.stem ||
          creation.number !== null && creation.number === editNumber) {
        result.collision = { creation: creation, edit: edit };
        return result;
      }
    }
  }
  raw.incomplete.forEach(function (record) { result.transactions.add(record.transactionId); });
  return result;
}
function scanIntegrity(scope) {
  // Edit publication is globally serialized through the shared task corpus and
  // INDEX. Even a stem-scoped validator must inspect every edit receipt; using
  // its requested stem as a marker filter would hide unrelated recovery state.
  var raw = scanRaw(null);
  var owned = activeTransactions(raw, scope);
  var queued = combinedQueue(raw, scope);
  var statuses = raw.incomplete.map(function (record) { return { owner: 'edit-markers', kind: 'marker', stem: record.stem,
    state: owned.has(record.transactionId) ? 'active' : (queued.transactions.has(record.transactionId) ? 'queued' : 'recovery-required'),
    phase: record.phase, createdAt: record.createdAt, updatedAt: record.updatedAt,
    contentHash: digest(Buffer.from(record.transactionId, 'utf8')) }; }).concat(raw.completed.map(function (record) {
      return { owner: 'edit-markers', kind: 'marker', stem: record.stem, state: 'completed', phase: record.phase,
        createdAt: record.createdAt, updatedAt: record.updatedAt, contentHash: digest(Buffer.from(record.transactionId, 'utf8')) };
    })).concat(raw.cas.map(function (row) { return { owner: 'edit-markers', kind: 'durable-cas', stem: null,
      state: row.state, contentHash: digest(Buffer.from(row.name, 'utf8')) }; }));
  var findings = raw.issues.map(function (row) { return { code: row.code, severity: 'error', stem: row.stem, paths: row.file ? [row.file] : [],
    message: row.message, recovery: 'Run the exact deterministic edit recovery authority; do not delete edit/CAS state manually.' }; });
  if (queued.collision) findings.push({ code: 'COMBINED_PUBLICATION_IDENTITY_INTERSECTION', severity: 'error', stem: queued.collision.edit.stem,
    paths: [path.join(paths.TASK_EDITS_DIR, queued.collision.edit.stem + '.json')],
    message: 'Creation and edit recovery claim the same task identity; combined publication recovery is refused.',
    recovery: 'Inspect the exact creation/edit marker generations; do not choose a recovery order manually.' });
  raw.incomplete.filter(function (record) { return !owned.has(record.transactionId) && !queued.transactions.has(record.transactionId); }).forEach(function (record) { findings.push({ code: 'EDIT_INCOMPLETE', severity: 'error', stem: record.stem,
    paths: [path.join(paths.TASK_EDITS_DIR, record.stem + '.json')], message: 'Durable backlog edit requires recovery.',
    recovery: 'Run the deterministic edit recovery flow for the exact marker generation.' }); });
  return { version: 1, owner: 'edit-markers', statuses: statuses, findings: findings,
    snapshotInputs: raw.snapshotInputs, truncated: raw.truncated };
}
function dirMtime() {
  var directory = fileGuards.realDirectoryUnder(paths.TASK_EDITS_AUTHORITY_ROOT, paths.TASK_EDITS_DIR, { allowMissing: true });
  return directory && directory.exists && directory.stat ? directory.stat.mtimeMs : 0;
}

module.exports = { scan: scan, blockingIssue: blockingIssue, dirMtime: dirMtime, scanIntegrity: scanIntegrity };
