'use strict';

// Strict read-only composition boundary for runtime metadata. Each subsystem
// owns and applies its exact record schema; this module only validates the
// small transport envelope, removes capability-bearing fields, and produces a
// deterministic verdict shared by the Site adapter and local CLI.

var cp = require('child_process');
var crypto = require('crypto');
var path = require('path');
var paths = require('./paths');
var writerLeaseInspector = require('./writer-lease-inspector');
var taskSource = require('../../tasks/task-source-contract.cjs');

var MAX_STATUSES = 5000;
var MAX_FINDINGS = 2000;
// Canonical task-state core admits at most its 10k filesystem bound. Keep a
// margin for lock/task snapshot inputs merged alongside this composite.
var MAX_SNAPSHOT_INPUTS = 9000;
var TRANSITION_OUTPUT_MAX = 16 * 1024 * 1024;
var OWNER_CONTEXT_MAX = 4096;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var TX_RE = /^[a-f0-9]{32}$/;
var LEASE_RE = /^wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159}$/;
var PROCESS_RE = /^psid-v1:(?:linux|darwin|win32):[a-f0-9]{64}$/;

function bounded(value, max) { var text = String(value == null ? '' : value); return text.length <= max ? text : text.slice(0, max); }
function canonicalJson(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
  return '{' + Object.keys(value).sort().map(function (key) {
    return JSON.stringify(key) + ':' + canonicalJson(value[key]);
  }).join(',') + '}';
}
function digest(value) {
  return 'sha256:' + crypto.createHash('sha256').update(canonicalJson(value), 'utf8').digest('hex');
}
function redactedText(value, max) {
  var text = String(value == null ? '' : value);
  var root = path.resolve(paths.PROJECT_ROOT || '.');
  if (root && root !== path.parse(root).root) text = text.split(root).join('<project-root>');
  text = text.replace(/(^|[\s("'`:=])((?:[A-Za-z]:[\\/]|\/)[^\s"'`<>]*)/g, '$1<absolute-path>');
  return bounded(text, max);
}
// Runtime owners operate on absolute paths, but neither the canonical result
// nor its hash contract may expose host-local roots.  Accept a path only when
// it resolves under the configured project authority and project it to one
// canonical slash-separated repository-relative spelling.
function projectPath(value) {
  if (typeof value !== 'string' || value.length === 0 || value.length > 4096 || value.indexOf('\0') >= 0) return null;
  if (path.sep === '/' && value.indexOf('\\') >= 0) return null;
  var root = path.resolve(paths.PROJECT_ROOT || '.');
  var absolute = path.isAbsolute(value) ? path.resolve(value) : path.resolve(root, value);
  var relative = path.relative(root, absolute);
  if (relative === '') return '.';
  if (relative === '..' || relative.indexOf('..' + path.sep) === 0 || path.isAbsolute(relative)) return null;
  return relative.split(path.sep).join('/');
}
function canonicalStem(value) { return value === null || taskSource.safeTaskStem(value); }
function finding(code, owner, stem, message, pathsList) {
  return { code: bounded(code || 'RUNTIME_INSPECTOR_INVALID', 80), severity: 'error', stem: canonicalStem(stem) ? stem : null,
    paths: Array.isArray(pathsList) ? pathsList.slice(0, 20).map(projectPath).filter(Boolean) : [],
    message: redactedText(message || 'Runtime inspector failed.', 500),
    recovery: 'Inspect and recover the exact ' + bounded(owner || 'runtime', 80) + ' owner record; do not clear runtime state by age.' };
}
function safeStatus(row, owner) {
  if (!row || typeof row !== 'object' || Array.isArray(row)) return null;
  var out = { owner: bounded(row.owner || owner, 80), kind: bounded(row.kind || 'record', 80), stem: canonicalStem(row.stem) ? row.stem : null,
    state: bounded(row.state || 'active', 80) };
  ['action', 'requestId', 'key', 'leaseId', 'writerKind', 'operation', 'phase', 'createdAt', 'updatedAt', 'contentHash'].forEach(function (field) {
    if (typeof row[field] === 'string' && row[field].length <= 300) out[field] = row[field];
  });
  if (HASH_RE.test(String(row.lockGenerationHash || ''))) out.lockGenerationHash = row.lockGenerationHash;
  if (Number.isSafeInteger(row.revision) && row.revision >= 0) out.revision = row.revision;
  return out;
}
function safeSnapshot(row, owner) {
  if (!row || typeof row !== 'object' || Array.isArray(row) || typeof row.hash !== 'string' || !/^sha256:[a-f0-9]{64}$/.test(row.hash)) return null;
  var safePath = projectPath(row.path);
  if (!safePath) return null;
  var out = { owner: bounded(row.owner || owner, 80), kind: bounded(row.kind || 'record', 80), path: safePath, hash: row.hash };
  if (Number.isSafeInteger(row.size) && row.size >= 0) out.size = row.size;
  return out;
}
function sameFields(value, fields) {
  return value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === fields.slice().sort().join('\0');
}
function parseOwnerContext(name, kind) {
  var raw = process.env[name];
  if (!raw || Buffer.byteLength(raw, 'utf8') > OWNER_CONTEXT_MAX) return null;
  var value;
  try { value = JSON.parse(raw); } catch (error) { return null; }
  var common = value && value.version === 1 && Number.isSafeInteger(value.pid) && value.pid > 0 &&
    Number.isSafeInteger(value.revision) && value.revision > 0 && HASH_RE.test(String(value.contentHash || '')) &&
    TX_RE.test(String(value.transactionId || '')) && PROCESS_RE.test(String(value.processStartId || ''));
  if (!common) return null;
  if (kind === 'creation') {
    var fields = ['authorityLeaseId', 'contentHash', 'keyHash', 'mode', 'pid', 'processStartId', 'publicationKey',
      'publicationGuardLeaseId', 'revision', 'transactionId', 'version'];
    if (!sameFields(value, fields) || !HASH_RE.test(String(value.keyHash || '')) ||
        ['task:create-backlog', 'task:recover-backlog-creations'].indexOf(value.publicationKey) < 0 ||
        ['owned-lease', 'nested-guard', 'fixture-unleased'].indexOf(value.mode) < 0) return null;
    if (value.mode === 'owned-lease' && (!LEASE_RE.test(String(value.authorityLeaseId || '')) || value.publicationGuardLeaseId !== null)) return null;
    if (value.mode === 'nested-guard' && (!LEASE_RE.test(String(value.authorityLeaseId || '')) || !LEASE_RE.test(String(value.publicationGuardLeaseId || '')) ||
        value.authorityLeaseId === value.publicationGuardLeaseId)) return null;
    if (value.mode === 'fixture-unleased' && (value.authorityLeaseId !== null || value.publicationGuardLeaseId !== null)) return null;
    return Object.freeze(value);
  }
  var editFields = ['authorityLeaseId', 'contentHash', 'mode', 'pid', 'processStartId', 'publicationKey', 'revision', 'stem', 'transactionId', 'version'];
  if (!sameFields(value, editFields) || !canonicalStem(value.stem) || value.stem === null ||
      [value.stem && 'task:edit-backlog:' + value.stem, 'task:recover-backlog-edits'].indexOf(value.publicationKey) < 0 ||
      ['owned-lease', 'fixture-unleased'].indexOf(value.mode) < 0) return null;
  if (value.mode === 'owned-lease' && !LEASE_RE.test(String(value.authorityLeaseId || ''))) return null;
  if (value.mode === 'fixture-unleased' && value.authorityLeaseId !== null) return null;
  return Object.freeze(value);
}
function normalizeOwner(value, owner) {
  if (!value || typeof value !== 'object' || Array.isArray(value) || value.version !== 1 ||
      !Array.isArray(value.statuses) || !Array.isArray(value.findings) || !Array.isArray(value.snapshotInputs) ||
      typeof value.truncated !== 'boolean') {
    return { version: 1, owner: owner, statuses: [], snapshotInputs: [], truncated: false,
      findings: [finding('RUNTIME_OWNER_ENVELOPE_INVALID', owner, null, 'Owner runtime inspector returned an invalid version-1 envelope.')] };
  }
  var unsafePath = false;
  var findings = value.findings.map(function (row) {
    if (!row || typeof row !== 'object' || Array.isArray(row)) return finding('RUNTIME_OWNER_FINDING_INVALID', owner, null, 'Owner runtime inspector returned an invalid finding.');
    if (Array.isArray(row.paths) && row.paths.some(function (entry) { return !projectPath(entry); })) unsafePath = true;
    var severity = ['info', 'warning', 'error', 'blocker'].indexOf(row.severity) >= 0 ? row.severity : 'error';
    var safe = finding(row.code, owner, row.stem, row.message, row.paths);
    safe.severity = severity;
    if (typeof row.recovery === 'string') safe.recovery = redactedText(row.recovery, 500);
    return safe;
  });
  var snapshots = value.snapshotInputs.map(function (row) {
    var safe = safeSnapshot(row, owner);
    if (!safe) unsafePath = true;
    return safe;
  }).filter(Boolean);
  if (unsafePath) findings.push(finding('RUNTIME_OWNER_PATH_UNSAFE', owner, null,
    'Owner runtime inspector returned a path outside the configured project authority.'));
  return {
    version: 1, owner: owner,
    statuses: value.statuses.map(function (row) { return safeStatus(row, owner); }).filter(Boolean),
    findings: findings,
    snapshotInputs: snapshots,
    truncated: value.truncated
  };
}
function transitionScan(scope) {
  var script = path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'transition-task-state.mjs');
  var args = [script, 'inspect-integrity'];
  if (scope && scope.stem) args.push('--stem', scope.stem);
  var env = Object.assign({}, process.env, {
    ORCHESTRATOR_PROJECT_ROOT: paths.PROJECT_ROOT,
    ORCHESTRATOR_TASKS_DIR: paths.TASKS_DIR,
    ORCHESTRATOR_TRANSITIONS_DIR: paths.TRANSITIONS_DIR,
    ORCHESTRATOR_WRITER_LEASES_DIR: paths.WRITER_LEASES_DIR,
    ORCHESTRATOR_FINALIZATIONS_DIR: paths.FINALIZATIONS_DIR,
    ORCHESTRATOR_LOCKS_DIR: paths.LOCKS_DIR,
    ORCHESTRATOR_JOURNAL_DIR: paths.JOURNAL_DIR
  });
  delete env.NODE_OPTIONS; delete env.NODE_PATH;
  var result = cp.spawnSync(process.execPath, args, {
    cwd: paths.PROJECT_ROOT, env: env, encoding: 'utf8', timeout: 15000, maxBuffer: TRANSITION_OUTPUT_MAX,
    windowsHide: true
  });
  if (!result || result.error || result.status !== 0 || typeof result.stdout !== 'string') {
    throw new Error('transition inspector failed: ' + bounded(result && (result.stderr || result.error && result.error.message) || 'no result', 300));
  }
  return JSON.parse(result.stdout);
}
function ownerScanners(creationProjection, creationError) {
  // Lazy loads avoid the sessions -> task-integrity -> runtime-integrity cycle
  // during server boot. By the time validation calls this function every owner
  // module has completed initialization.
  return [
    ['requests', require('./requests').scanIntegrity],
    ['sessions', require('./sessions').scanIntegrity],
    ['creation-markers', function () {
      if (creationError) throw creationError;
      return creationProjection.envelope;
    }],
    ['edit-markers', require('./edit-markers').scanIntegrity],
    ['shallow-intake', require('./shallow-intake').scanIntegrity],
    ['finalizations', require('./finalizations').scanIntegrity],
    ['integrations', require('./integrations').scanIntegrity],
    ['transitions', transitionScan]
  ];
}
function loadedRoots() {
  return {
    projectRoot: paths.PROJECT_ROOT,
    tasksDir: paths.TASKS_DIR,
    locksDir: paths.LOCKS_DIR,
    requestsDir: paths.REQUESTS_DIR,
    requestReservationsDir: paths.REQUEST_RESERVATIONS_DIR,
    runsDir: paths.RUNS_DIR,
    finalizationsDir: paths.FINALIZATIONS_DIR,
    writerLeasesDir: paths.WRITER_LEASES_DIR,
    taskCreationsDir: paths.TASK_CREATIONS_DIR,
    taskEditsDir: paths.TASK_EDITS_DIR,
    taskIntakeDir: paths.TASK_INTAKE_DIR,
    transitionsDir: paths.TRANSITIONS_DIR,
    journalDir: paths.JOURNAL_DIR,
    writerAuthorityRoot: paths.WRITER_AUTHORITY_ROOT,
    taskCreationsAuthorityRoot: paths.TASK_CREATIONS_AUTHORITY_ROOT,
    taskEditsAuthorityRoot: paths.TASK_EDITS_AUTHORITY_ROOT
  };
}
function exactRoots(value) {
  var expected = loadedRoots(), fields = Object.keys(expected).sort();
  if (!value || typeof value !== 'object' || Array.isArray(value) || Object.keys(value).sort().join('\0') !== fields.join('\0')) return false;
  return fields.every(function (field) { return typeof value[field] === 'string' && path.resolve(value[field]) === path.resolve(expected[field]); });
}
function order(left, right) {
  var a = [left.owner || '', left.stem || '', left.kind || '', left.code || '', left.state || '', left.path || '', left.requestId || '', left.leaseId || ''].join('\0');
  var b = [right.owner || '', right.stem || '', right.kind || '', right.code || '', right.state || '', right.path || '', right.requestId || '', right.leaseId || ''].join('\0');
  return a < b ? -1 : a > b ? 1 : 0;
}
function leaseFingerprint(value) {
  function rows(items) {
    return (items || []).map(function (row) {
      return [row.leaseId || '', row.kind || '', row.stem || '', row.key || '', row.sessionId || '',
        row.owner && row.owner.pid || 0, row.owner && row.owner.processStartId || '', row.childPid || 0,
        row.childProcessStartId || '', row.unverified === true, row.updatedAt || ''];
    }).sort();
  }
  return JSON.stringify({ stable: value && value.stable === true, truncated: value && value.truncated === true,
    active: rows(value && value.active), stale: rows(value && value.stale),
    issues: (value && value.issues || []).map(function (row) { return [row.code || '', row.leaseId || '']; }).sort(),
    snapshots: (value && value.snapshotInputs || []).map(function (row) { return [row.path || '', row.hash || '', row.size || 0]; }).sort() });
}
function scanIntegrity(scope) {
  if (!scope || typeof scope !== 'object' || Array.isArray(scope) || !exactRoots(scope.roots)) {
    throw new Error('runtime inspector roots do not match its isolated owner-module environment');
  }
  var stem = scope.stem || null;
  var leaseInspection;
  try { leaseInspection = writerLeaseInspector.inspect(); }
  catch (error) {
    leaseInspection = { version: 1, active: [], stale: [], issues: [{ code: 'WRITER_LEASE_DIR_UNSAFE', message: bounded(error && error.message || error, 300) }],
      snapshotInputs: [], truncated: false, stable: false };
  }
  // Freeze the exact snapshot passed to all owners. No owner can add a broad
  // exemption or mutate a lease row after another owner made its decision.
  function freezeLeaseRow(row) { if (row && row.owner) Object.freeze(row.owner); Object.freeze(row); }
  (leaseInspection.active || []).forEach(freezeLeaseRow);
  (leaseInspection.stale || []).forEach(freezeLeaseRow);
  (leaseInspection.issues || []).forEach(Object.freeze);
  (leaseInspection.snapshotInputs || []).forEach(Object.freeze);
  Object.freeze(leaseInspection.active); Object.freeze(leaseInspection.stale);
  Object.freeze(leaseInspection.issues); Object.freeze(leaseInspection.snapshotInputs);
  Object.freeze(leaseInspection);
  var baseOwnerScope = Object.freeze({ stem: stem, writerLeaseInspection: leaseInspection,
    creationContext: parseOwnerContext('ORCHESTRATOR_ACTIVE_CREATION_CONTEXT', 'creation'),
    editContext: parseOwnerContext('ORCHESTRATOR_ACTIVE_EDIT_CONTEXT', 'edit'),
    combinedPublicationRecoveryRequested: process.env.ORCHESTRATOR_COMBINED_PUBLICATION_RECOVERY === '1' });
  // Creation is the first phase of combined publication recovery. Inspect it
  // exactly once and pass edit-markers only the non-serializable projection
  // minted by that owner module. This avoids both a double-scan TOCTOU and any
  // broad environment-driven exemption for edit recovery state.
  var creationProjection = null, creationError = null;
  try { creationProjection = require('./creation-markers').inspectIntegrity(baseOwnerScope); }
  catch (error) { creationError = error; }
  var ownerScope = Object.freeze({ stem: baseOwnerScope.stem, writerLeaseInspection: leaseInspection,
    creationContext: baseOwnerScope.creationContext, editContext: baseOwnerScope.editContext,
    combinedPublicationRecoveryRequested: baseOwnerScope.combinedPublicationRecoveryRequested,
    combinedCreationProjection: creationProjection });
  var combined = { version: 1, scope: stem || 'all', statuses: [], findings: [], snapshotInputs: [], truncated: false };
  ownerScanners(creationProjection, creationError).forEach(function (entry) {
    var owner = entry[0], value;
    try { value = entry[1](ownerScope); }
    catch (error) {
      combined.findings.push(finding('RUNTIME_OWNER_UNAVAILABLE', owner, stem, owner + ' runtime inspector failed: ' + bounded(error && error.message || error, 300)));
      return;
    }
    var normalized = normalizeOwner(value, owner);
    combined.statuses.push.apply(combined.statuses, normalized.statuses);
    combined.findings.push.apply(combined.findings, normalized.findings);
    combined.snapshotInputs.push.apply(combined.snapshotInputs, normalized.snapshotInputs);
    combined.truncated = combined.truncated || normalized.truncated;
  });
  try {
    var leaseAfter = writerLeaseInspector.inspect();
    if (leaseFingerprint(leaseAfter) !== leaseFingerprint(leaseInspection)) {
      combined.findings.push(finding('WRITER_LEASE_SNAPSHOT_CHANGED', 'finalizations', stem,
        'Writer lease ownership changed during the composite runtime snapshot.', [paths.WRITER_LEASES_DIR]));
    }
  } catch (error) {
    combined.findings.push(finding('WRITER_LEASE_SNAPSHOT_CHANGED', 'finalizations', stem,
      'Writer lease ownership could not be revalidated after the composite runtime snapshot.', [paths.WRITER_LEASES_DIR]));
  }
  if (combined.statuses.length > MAX_STATUSES || combined.findings.length > MAX_FINDINGS || combined.snapshotInputs.length > MAX_SNAPSHOT_INPUTS - 1) {
    combined.findings.push(finding('RUNTIME_COMPOSITE_SCAN_LIMIT', 'composite', stem, 'Composite runtime inspection exceeded its aggregate result limit.'));
    combined.truncated = true;
  }
  combined.statuses = combined.statuses.slice(0, MAX_STATUSES).sort(order);
  combined.findings = combined.findings.slice(0, MAX_FINDINGS).sort(order);
  combined.snapshotInputs = combined.snapshotInputs.slice(0, MAX_SNAPSHOT_INPUTS - 1).sort(order);
  // Durable bytes alone are insufficient for an admission snapshot: exact
  // process-generation verdicts and narrowly scoped active-writer authority
  // can change a runtime finding without changing an owner file. Commit one
  // privacy-preserving synthetic input over those bounded normalized verdicts
  // so task-state snapshotHash fences every input that affected this result.
  var verdictMaterial = {
    version: 1,
    scope: stem || 'all',
    authority: {
      creation: baseOwnerScope.creationContext,
      edit: baseOwnerScope.editContext,
      combinedPublicationRecoveryRequested: baseOwnerScope.combinedPublicationRecoveryRequested
    },
    leaseInspection: leaseFingerprint(leaseInspection),
    statuses: combined.statuses,
    findings: combined.findings,
    truncated: combined.truncated
  };
  var verdictBytes = Buffer.byteLength(canonicalJson(verdictMaterial), 'utf8');
  combined.snapshotInputs.push({
    owner: 'composite', kind: 'verdict',
    path: 'orchestrator/.cache/tasks/.runtime-verdict/' + (stem || 'all'),
    hash: digest(verdictMaterial), size: verdictBytes
  });
  combined.snapshotInputs.sort(order);
  combined.ok = !combined.truncated && !combined.findings.some(function (row) { return row.severity === 'error' || row.severity === 'blocker'; });
  combined.stats = { statuses: combined.statuses.length, findings: combined.findings.length, snapshotInputs: combined.snapshotInputs.length };
  return combined;
}

module.exports = { scanIntegrity: scanIntegrity, loadedRoots: loadedRoots, projectPath: projectPath,
  _parseOwnerContext: parseOwnerContext, _safeStatus: safeStatus };
