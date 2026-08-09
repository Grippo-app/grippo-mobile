'use strict';

// Public, redacted Integrations -> Figma projection. Connector internals stay
// in collapsed diagnostics; account/file/access/sync readiness is classified
// once on the server so browser views do not duplicate state logic.

var path = require('path');
var figma = require('./figma');
var configUpdate = require('./project-config-update');
var testJobs = require('./figma-test-job');
var sync = require('./figma-sync');
var taskPublication = require('./figma-task-publication');
var tokenSources = require('./design-token-sources');
var history = require('./figma-sync-history');
var generation = require('./figma-generation');
var sessions = require('./sessions');
var persistence = require('./persistence');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var writerLeases = require('../../tasks/writer-leases.cjs');
var FIGMA_MCP_URL = 'https://mcp.figma.com/mcp';
var resetActive = false;
var resetIdempotency = new Map();
var RESET_ENTRY_MAX = 50000;
var RESET_DEPTH_MAX = 16;
var FIGMA_DIR = generation.FIGMA_DIR;
var RESET_FILES = [
  path.join(FIGMA_DIR, '.account.json'),
  path.join(FIGMA_DIR, '.rest-token'),
  path.join(FIGMA_DIR, '.env'),
  path.join(FIGMA_DIR, 'token-mappings.json'),
  path.join(FIGMA_DIR, 'component-mappings.json'),
  path.join(FIGMA_DIR, 'tokens', 'source-index.json'),
  path.join(FIGMA_DIR, 'tokens', 'observed-token-catalog.json'),
  path.join(FIGMA_DIR, 'components', 'design-component-inventory.json')
];
var RESET_TREES = [
  { directory: paths.FIGMA_CACHE_DIR, preserve: [], removeRoot: true },
  { directory: generation.MANIFESTS_DIR, preserve: ['.gitkeep'], removeRoot: false },
  { directory: path.join(FIGMA_DIR, 'tokens', 'sources'), preserve: [], removeRoot: true },
  { directory: path.join(FIGMA_DIR, 'tokens', 'normalized'), preserve: ['.gitkeep'], removeRoot: false },
  { directory: path.join(FIGMA_DIR, 'components', 'visual'), preserve: [], removeRoot: true }
];

function quota(account) {
  if (!account) return { state: 'unknown', reasonCode: null };
  var tier = String(account.tier || '').toLowerCase(), seat = String(account.seat || '').toLowerCase();
  if (tier === 'starter' || /\b(?:view|viewer|collab)\b/.test(seat)) return { state: 'warning', reasonCode: 'quota-risk' };
  if (tier || seat) return { state: 'ok', reasonCode: null };
  return { state: 'unknown', reasonCode: null };
}
function boundedText(value, max) {
  var text = typeof value === 'string' ? value.replace(/[\x00-\x1f\x7f]/g, '').trim() : '';
  return text ? text.slice(0, max) : null;
}
function connectorReason(connector) {
  if (connector && connector.global && connector.global.present) return 'connector-conflict';
  if (!connector || connector.state === 'unknown') return 'connector-unavailable';
  if (connector.state === 'connected' && (!connector.local || !connector.local.present)) return 'connector-missing';
  if (connector.state === 'local-absent' || connector.state === 'cli-missing') return 'connector-missing';
  if (connector.state === 'needs-auth') return 'auth-required';
  if (connector.state === 'misconfigured') return 'connector-conflict';
  return null;
}
function syncSummary(record) {
  if (!record) return null;
  var counters = record.groups.reduce(function (out, group) {
    out.updated += group.updated || 0; out.unchanged += group.unchanged || 0; out.warnings += group.warnings || 0; return out;
  }, { updated: 0, unchanged: 0, warnings: 0 });
  return {
    id: record.id,
    result: record.result,
    errorCode: boundedText(record.errorCode, 80),
    updated: counters.updated,
    unchanged: counters.unchanged,
    warnings: counters.warnings,
    durationMs: record.durationMs,
    startedAt: record.startedAt,
    finishedAt: record.finishedAt,
    committedGenerationId: record.committedGenerationId,
    groups: record.groups.map(function (group) {
      return {
        group: group.group, status: group.status,
        updated: group.updated, unchanged: group.unchanged, warnings: group.warnings
      };
    })
  };
}
function newer(a, b) {
  if (!a) return b || null;
  if (!b) return a;
  return Date.parse(a.finishedAt || a.startedAt || 0) >= Date.parse(b.finishedAt || b.startedAt || 0) ? a : b;
}
function publicSyncState(record) {
  if (!record) return 'idle';
  if (record.result === 'success') return 'completed';
  if (record.result === 'partial') return 'partial';
  if (record.result === 'queued' || record.result === 'running') return record.result;
  return 'failed';
}
function connectorSessionActive() {
  try {
    var active = sessions.list();
    return Object.keys(active).some(function (key) {
      return active[key] && active[key].running && (key.indexOf('figma:') === 0 || key.indexOf('task:') === 0);
    });
  } catch (error) { return true; }
}
function generationId(current) {
  return current && current.ok && current.mode === 'generation' && current.manifest
    ? current.manifest.generationId : null;
}
function inspectFile(file) {
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(file), file);
  return inspected && (inspected.status === 'missing' || inspected.status === 'present' && inspected.stat &&
    inspected.stat.isFile() && !inspected.stat.isSymbolicLink() && String(inspected.stat.nlink) === '1');
}
function resetStatePresent(config, currentGeneration, connector) {
  if (config && config.ok && config.figmaFieldState !== 'missing') return true;
  if (currentGeneration && (!currentGeneration.ok || currentGeneration.mode === 'generation')) return true;
  if (connector && connector.local && connector.local.present) return true;
  for (var i = 0; i < RESET_FILES.length; i++) {
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(RESET_FILES[i]), RESET_FILES[i]);
    if (!inspected || inspected.status !== 'missing') return true;
  }
  return RESET_TREES.some(function (target) {
    var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, target.directory, RESET_ENTRY_MAX);
    return !listed.ok || listed.names.some(function (name) { return target.preserve.indexOf(name) < 0; });
  });
}
function inspectTree(directory, preserve, budget, depth) {
  if (depth > RESET_DEPTH_MAX) return false;
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(directory), directory);
  if (inspected && inspected.status === 'missing') return true;
  if (!inspected || inspected.status !== 'present' || !inspected.stat ||
      !inspected.stat.isDirectory() || inspected.stat.isSymbolicLink()) return false;
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory, RESET_ENTRY_MAX);
  if (!listed.ok) return false;
  for (var i = 0; i < listed.names.length; i++) {
    var child = path.join(directory, listed.names[i]);
    var entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, child);
    if (!entry || entry.status !== 'present' || !entry.stat || entry.stat.isSymbolicLink()) return false;
    if (depth === 0 && preserve.indexOf(listed.names[i]) >= 0) {
      if (!entry.stat.isFile() || String(entry.stat.nlink) !== '1') return false;
      continue;
    }
    budget.count++;
    if (budget.count > RESET_ENTRY_MAX) return false;
    if (entry.stat.isDirectory()) {
      if (!inspectTree(child, [], budget, depth + 1)) return false;
    } else if (!entry.stat.isFile() || String(entry.stat.nlink) !== '1') return false;
  }
  return true;
}
function clearTree(directory, preserve, removeRoot, budget, depth) {
  if (depth > RESET_DEPTH_MAX) return false;
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(directory), directory);
  if (inspected && inspected.status === 'missing') return true;
  if (!inspected || inspected.status !== 'present' || !inspected.stat ||
      !inspected.stat.isDirectory() || inspected.stat.isSymbolicLink()) return false;
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, directory, RESET_ENTRY_MAX);
  if (!listed.ok) return false;
  for (var i = 0; i < listed.names.length; i++) {
    var child = path.join(directory, listed.names[i]);
    var entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, child);
    if (!entry || entry.status === 'missing') continue;
    if (entry.status !== 'present' || !entry.stat || entry.stat.isSymbolicLink()) return false;
    if (depth === 0 && preserve.indexOf(listed.names[i]) >= 0) {
      if (!entry.stat.isFile() || String(entry.stat.nlink) !== '1') return false;
      continue;
    }
    budget.count++;
    if (budget.count > RESET_ENTRY_MAX) return false;
    if (entry.stat.isDirectory()) {
      if (!clearTree(child, [], true, budget, depth + 1)) return false;
    } else if (!entry.stat.isFile() || String(entry.stat.nlink) !== '1' ||
        !fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, directory, child, { allowMissing: true })) return false;
  }
  return !removeRoot || fileGuards.removeEmptyDirectoryUnder(paths.PROJECT_ROOT, path.dirname(directory), directory);
}
function exactResetRequest(value) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === ['expectedConfigRevision', 'expectedGenerationId', 'idempotencyKey'].sort().join('\0') &&
    (value.expectedConfigRevision === null || /^sha256:[a-f0-9]{64}$/.test(String(value.expectedConfigRevision || ''))) &&
    (value.expectedGenerationId === null || generation.GENERATION_RE.test(String(value.expectedGenerationId || ''))) &&
    /^[A-Za-z0-9._:-]{8,128}$/.test(String(value.idempotencyKey || ''));
}
function rememberReset(key, fingerprint, result) {
  if (resetIdempotency.size >= 100) resetIdempotency.delete(resetIdempotency.keys().next().value);
  resetIdempotency.set(key, { fingerprint: fingerprint, result: result });
}
function releaseResetLease(handle) {
  if (handle) try { writerLeases.release(handle); } catch (ignore) {}
}
function removeConnector() {
  return new Promise(function (resolve, reject) {
    figma.removeLocalServer(function (error) { if (error) reject(error); else resolve(); });
  });
}
async function reset(request) {
  if (!exactResetRequest(request)) return { ok: false, status: 400, error: 'bad-request' };
  var fingerprint = JSON.stringify(request), prior = resetIdempotency.get(request.idempotencyKey);
  if (prior) return prior.fingerprint === fingerprint ? prior.result :
    { ok: false, status: 409, error: 'idempotency-conflict' };
  if (resetActive) return { ok: false, status: 409, error: 'writer-lease-conflict' };
  resetActive = true;
  var lease = null;
  var testsPaused = false;
  var syncPaused = false;
  var tokenSourcesPaused = false;
  var publicationPaused = false;
  function fail(status, error, extra) {
    return Object.assign({ ok: false, status: status, error: error }, extra || {});
  }
  try {
    if (figma.mutationBusy()) return fail(409, 'writer-lease-conflict');
    if (!testJobs.beginReset()) return fail(409, 'figma-test-active');
    testsPaused = true;
    if (!sync.beginReset()) return fail(409, 'figma-sync-active');
    syncPaused = true;
    if (!tokenSources.beginReset()) return fail(409, 'figma-sync-active');
    tokenSourcesPaused = true;
    if (!taskPublication.beginReset()) return fail(409, 'figma-sync-active');
    publicationPaused = true;
    if (connectorSessionActive()) return fail(409, 'figma-session-active');
    var config = configUpdate.read(), currentGeneration = generation.current();
    if (!config.ok) return fail(409, config.error || 'project-config-unavailable');
    if (config.revision !== request.expectedConfigRevision) {
      return fail(409, 'project-config-revision-conflict', { currentRevision: config.revision });
    }
    if (generationId(currentGeneration) !== request.expectedGenerationId) return fail(409, 'plan-stale');

    try {
      lease = writerLeases.acquire(paths.WRITER_LEASES_DIR, { kind: 'site-config', key: 'figma:integration-reset',
        ownerPid: process.pid, ttlMs: 5 * 60 * 1000, rootDir: paths.WRITER_AUTHORITY_ROOT });
    } catch (leaseError) { return fail(409, 'writer-lease-conflict'); }
    var scan = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT);
    if (scan.issues.length || scan.stale.length || !scan.active.some(function (row) { return row.leaseId === lease.leaseId; }) ||
        scan.active.some(function (row) { return row.leaseId !== lease.leaseId; })) return fail(409, 'writer-lease-conflict');
    if (figma.mutationBusy() || !testJobs.resetReady() || !sync.resetReady() || !tokenSources.resetReady() ||
        !taskPublication.resetReady() || connectorSessionActive()) {
      return fail(409, 'figma-session-active');
    }
    config = configUpdate.read(); currentGeneration = generation.current();
    if (!config.ok || config.revision !== request.expectedConfigRevision) {
      return fail(409, 'project-config-revision-conflict', { currentRevision: config.revision || null });
    }
    if (generationId(currentGeneration) !== request.expectedGenerationId) return fail(409, 'plan-stale');

    var inspectionBudget = { count: 0 };
    for (var i = 0; i < RESET_FILES.length; i++) if (!inspectFile(RESET_FILES[i])) return fail(409, 'generation-evidence-invalid');
    for (var j = 0; j < RESET_TREES.length; j++) {
      if (!inspectTree(RESET_TREES[j].directory, RESET_TREES[j].preserve, inspectionBudget, 0)) {
        return fail(409, 'generation-evidence-invalid');
      }
    }

    await removeConnector();
    await history.clearAll();
    var clearBudget = { count: 0 };
    for (var treeIndex = 0; treeIndex < RESET_TREES.length; treeIndex++) {
      var tree = RESET_TREES[treeIndex];
      if (!clearTree(tree.directory, tree.preserve, tree.removeRoot, clearBudget, 0)) {
        return fail(500, 'internal');
      }
    }
    for (var fileIndex = 0; fileIndex < RESET_FILES.length; fileIndex++) {
      var file = RESET_FILES[fileIndex];
      if (!fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, path.dirname(file), file, { allowMissing: true })) {
        return fail(500, 'internal');
      }
    }
    var persisted = persistence.readPersisted();
    delete persisted.setupForm.figmaLibraryUrl;
    persistence.writePersisted(persisted);
    var configResult = configUpdate.clearFigmaLibraryUrl(request.expectedConfigRevision, lease);
    if (!configResult.ok) return configResult;
    if (!testJobs.clearRuntime() || !sync.clearRuntime() || !taskPublication.clearRuntime()) return fail(500, 'internal');
    releaseResetLease(lease); lease = null;
    if (publicationPaused) taskPublication.endReset();
    publicationPaused = false;
    if (tokenSourcesPaused) tokenSources.endReset();
    tokenSourcesPaused = false;
    if (syncPaused) sync.endReset();
    syncPaused = false;
    if (testsPaused) testJobs.endReset();
    testsPaused = false;
    resetActive = false;
    var success = { ok: true, status: 200, integration: get() };
    rememberReset(request.idempotencyKey, fingerprint, success);
    return success;
  } catch (error) {
    var code = String(error && error.message || '');
    return fail(500, code === 'sync-history-index-invalid' ? code : code === 'integration-failed' ? code : 'internal');
  } finally {
    releaseResetLease(lease);
    if (publicationPaused) taskPublication.endReset();
    if (tokenSourcesPaused) tokenSources.endReset();
    if (syncPaused) sync.endReset();
    if (testsPaused) testJobs.endReset();
    resetActive = false;
  }
}
function resetting() { return resetActive; }
function get(currentGenerationOverride) {
  var connector = figma.status() || {};
  var config = configUpdate.read();
  var accountRaw = figma.account();
  var accountFresh = accountRaw && testJobs.fresh(accountRaw.checkedAt, testJobs.ACCOUNT_TTL);
  var accountHash = accountFresh ? testJobs.accountFingerprint(accountRaw) : null;
  var fileKey = config.ok ? config.figmaFileKey : null;
  var accessRaw = fileKey && accountHash ? testJobs.accessFor(fileKey, accountHash) : null;
  var latestSuccess = history.latestSuccessful();
  var latest = newer(history.latest(), sync.latestTerminal());
  var activeSync = sync.active();
  var activeTest = testJobs.currentJob();
  var testBusy = testJobs.busy();
  var syncBusy = sync.busy();
  var taskPublicationBusy = taskPublication.busy();
  var lastTest = activeTest || testJobs.lastJob();
  var currentGeneration = currentGenerationOverride || generation.current();
  var syncRecoveryState = sync.recoveryState();
  var taskPublicationRecoveryState = taskPublication.recoveryState();
  var sessionActive = connectorSessionActive();
  var resetState = resetStatePresent(config, currentGeneration, connector);
  var accessState = !fileKey ? 'unknown' : accessRaw ? (accessRaw.state === 'verified' ? 'verified' : accessRaw.state === 'denied' ? 'denied' : 'unverified') : 'unverified';
  var quotaState = accessRaw && accessRaw.reasonCode === 'quota-risk'
    ? { state: 'blocked', reasonCode: 'quota-risk' }
    : quota(accountFresh ? accountRaw : null);
  var reasonCode = syncRecoveryState === 'failed' || taskPublicationRecoveryState === 'failed'
    ? 'sync-recovery-failed' : connectorReason(connector);
  if (!reasonCode && !accountFresh) reasonCode = accountRaw ? 'account-stale' : 'auth-required';
  if (!reasonCode && !fileKey) reasonCode = config.ok && config.figmaFieldState === 'invalid' ? 'file-invalid' : 'file-missing';
  if (!reasonCode && accessState !== 'verified') reasonCode = accessRaw && accessRaw.reasonCode || 'access-unverified';
  if (!reasonCode && quotaState.state === 'blocked') reasonCode = quotaState.reasonCode || 'quota-risk';
  var status = activeSync && ['queued', 'running'].indexOf(activeSync.state) >= 0 ? 'syncing'
    : connector.state === 'cli-missing' || connector.state === 'unknown' ? 'unavailable'
      : reasonCode ? 'needs-attention' : 'ready';
  var syncGateReason = reasonCode ||
    (activeSync ? 'sync-already-running' : null) ||
    (syncRecoveryState !== 'ready' ? 'figma-sync-recovering' : null) ||
    (taskPublicationRecoveryState !== 'ready' ? 'figma-task-publication-recovering' : null) ||
    (taskPublicationBusy ? 'figma-task-publication-active' : null) ||
    (testBusy ? 'figma-test-active' : null) ||
    (sessionActive ? 'figma-session-active' : null) ||
    (status !== 'ready' ? 'figma-integration-not-ready' : null);
  // Global project comparison consumes only the immutable active generation
  // and product source. It remains available when the live Figma proof is
  // stale/disconnected; token/component refreshes keep the strict live gate.
  var compareGateReason = activeSync ? 'sync-already-running' :
    syncRecoveryState !== 'ready' ? (syncRecoveryState === 'failed' ? 'sync-recovery-failed' : 'figma-sync-recovering') :
      taskPublicationRecoveryState !== 'ready' ? (taskPublicationRecoveryState === 'failed' ? 'sync-recovery-failed' : 'figma-task-publication-recovering') :
        taskPublicationBusy ? 'figma-task-publication-active' :
          testBusy ? 'figma-test-active' :
            sessionActive ? 'figma-session-active' :
              !sync.comparisonSourceAvailable(currentGeneration) ? 'design-source-not-synced' : null;
  return {
    status: status,
    reasonCode: reasonCode,
    account: {
      state: accountFresh ? 'connected' : accountRaw ? 'unknown' : 'missing',
      displayName: accountRaw && accountRaw.handle || null,
      email: accountRaw && accountRaw.email || null,
      checkedAt: accountRaw && accountRaw.checkedAt || null
    },
    projectFile: {
      state: fileKey ? 'selected' : config.ok && config.figmaFieldState === 'missing' ? 'missing' : 'invalid',
      key: fileKey ? testJobs.maskKey(fileKey) : null,
      name: accessRaw && accessRaw.fileName || null,
      url: config.ok ? config.figmaLibraryUrl : null
    },
    access: {
      state: accessState,
      checkedAt: accessRaw && accessRaw.checkedAt || null,
      reasonCode: accessRaw && accessRaw.reasonCode || null
    },
    quota: quotaState,
    sync: {
      state: activeSync ? activeSync.state : syncRecoveryState === 'failed' ? 'failed' : publicSyncState(latest),
      activeJobId: activeSync && activeSync.id || null,
      lastSuccessAt: latestSuccess && latestSuccess.finishedAt || null,
      committedGenerationId: latestSuccess && latestSuccess.committedGenerationId || null,
      lastResult: syncSummary(latest),
      active: activeSync
    },
    test: lastTest,
    fileCandidate: testJobs.latestCandidate(),
    configRevision: config.revision,
    syncGate: {
      state: syncGateReason ? 'blocked' : 'ready',
      reasonCode: syncGateReason
    },
    compareGate: {
      state: compareGateReason ? 'blocked' : 'ready',
      reasonCode: compareGateReason
    },
    actions: {
      canTest: !resetActive && !testBusy && !syncBusy && !taskPublicationBusy && !sessionActive,
      canSync: !resetActive && syncGateReason === null,
      canCompare: !resetActive && compareGateReason === null,
      canChangeFile: !resetActive && !syncBusy && !taskPublicationBusy && !testBusy && !sessionActive,
      canChangeAccount: !resetActive && !testBusy && !syncBusy && !taskPublicationBusy && !sessionActive,
      canClearIntegration: !resetActive && !figma.mutationBusy() && resetState && !testBusy && sync.resetReady() &&
        tokenSources.resetReady() &&
        taskPublication.resetReady() && !sessionActive
    },
    context: {
      lastSuccessfulSync: latestSuccess && latestSuccess.finishedAt || null,
      updatedArtifacts: latestSuccess ? syncSummary(latestSuccess).updated : 0,
      generationAvailable: currentGeneration.ok && currentGeneration.mode === 'generation',
      generationError: currentGeneration.ok ? null : currentGeneration.error,
      generationId: generationId(currentGeneration),
      resetAvailable: resetState
    },
    diagnostics: {
      connectorState: connector.state || 'unknown',
      connectorScope: connector.local && connector.local.present ? 'project-local' : 'missing',
      connectorUrl: connector.local && connector.local.url === FIGMA_MCP_URL ? FIGMA_MCP_URL : null,
      competingConnector: connector.global && connector.global.present ? boundedText(connector.global.name, 200) || 'Figma' : null,
      checkedAt: connector.checkedAt || null,
      syncRecoveryState: syncRecoveryState,
      taskPublicationRecoveryState: taskPublicationRecoveryState,
      verificationGeneration: connector.verificationNonce ? String(connector.verificationNonce).slice(0, 8) : null,
      accountReceipt: 'orchestrator/figma/.account.json',
      generationPointer: 'orchestrator/figma/manifests/current-generation.json'
    }
  };
}

module.exports = { get: get, quota: quota, connectorReason: connectorReason, reset: reset, resetting: resetting };
