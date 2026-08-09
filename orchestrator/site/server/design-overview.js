'use strict';

var catalog = require('./design-catalog');
var history = require('./design-history');
var figmaIntegration = require('./figma-integration');
var figmaSyncHistory = require('./figma-sync-history');
var figma = require('./figma');
var figmaTestJob = require('./figma-test-job');
var configUpdate = require('./project-config-update');
var generation = require('./figma-generation');

var STALE_MS = 24 * 60 * 60 * 1000;

function metric(value, state, detail) {
  return { value: value, state: state, detail: detail || null };
}
function get(params) {
  params = params || {};
  var snap = catalog.snapshot();
  if (!snap.ok) return snap;
  if (Object.keys(params).some(function (key) { return key !== 'expectedGenerationRevision'; }) ||
      params.expectedGenerationRevision &&
        !/^sha256:[a-f0-9]{64}$/.test(String(params.expectedGenerationRevision))) {
    return Object.assign(catalog.publicMeta(snap), {
      ok: false, status: 400, error: 'bad-design-overview-query'
    });
  }
  var conflict = catalog.checkRevision(snap, params.expectedGenerationRevision);
  if (conflict) return conflict;
  var integration = figmaIntegration.get(snap.active);
  var config = configUpdate.read();
  var accountFingerprint = figmaTestJob.accountFingerprint(figma.account());
  var fileFingerprint = config.ok && config.figmaFileKey ? figmaTestJob.fileKeyFingerprint(config.figmaFileKey) : null;
  var accountMismatch = snap.active.mode === 'generation' && !!accountFingerprint &&
    snap.active.manifest.accountFingerprint !== accountFingerprint;
  var fileMismatch = snap.active.mode === 'generation' && !!fileFingerprint &&
    snap.active.manifest.fileKeyFingerprint !== fileFingerprint;
  var incompatible = accountMismatch || fileMismatch;
  var latestSuccessfulSync = figmaSyncHistory.latestSuccessful();
  var latestSyncAttempt = integration.sync && integration.sync.lastResult || null;
  var lastSuccess = snap.generationMode === 'generation' && latestSuccessfulSync &&
    latestSuccessfulSync.committedGenerationId === snap.committedGenerationId
    ? latestSuccessfulSync : null;
  var lastAt = lastSuccess && lastSuccess.finishedAt ||
    snap.active.mode === 'generation' && snap.active.manifest.createdAt || null;
  var age = lastAt ? Date.now() - Date.parse(lastAt) : null;
  var freshnessState = incompatible ? 'incompatible-source' :
    snap.active.mode !== 'generation' ? 'design-never-synced' :
      age !== null && age > STALE_MS ? 'stale' :
        generationPartial(snap.active) || snap.active.availability.missingOptional.length ? 'partial-artifacts' : 'fresh';
  var driftedSurfaces = snap.surfaces.rows.filter(function (row) { return row.status === 'drifted'; }).length;
  var tokenCoverage = snap.tokens.coverage;
  var componentCoverage = snap.components.coverage;
  var designTasks = snap.tasks.designItems || [];
  var openTasks = snap.tasks.ok ? designTasks.length : null;
  var historyStatus = history.status();
  var latestHistory = historyStatus.available ? history.latest() : null;
  var historyCurrent = snap.generationMode === 'generation' && latestHistory &&
    latestHistory.generationId === snap.committedGenerationId;
  var recent = historyCurrent ? history.latestChanges(snap.committedGenerationId).slice(0, 20) : [];
  var pendingFindings = snap.findings.filter(function (row) { return !row.openTask; });
  var severityRank = Object.assign(Object.create(null), { high: 0, medium: 1, low: 2 });
  var attention = pendingFindings.slice().sort(function (left, right) {
    var severity = (severityRank[left.severity] == null ? 3 : severityRank[left.severity]) -
      (severityRank[right.severity] == null ? 3 : severityRank[right.severity]);
    if (severity) return severity;
    return String(left.entityName || left.entityId).localeCompare(String(right.entityName || right.entityId)) ||
      left.id.localeCompare(right.id);
  }).slice(0, 25);
  var publicAttention = attention.map(function (row) {
    return {
      id: row.id, kind: row.kind, entityId: row.entityId, entityType: row.entityType,
      entityName: row.entityName, severity: row.severity, status: row.status,
      title: row.title, detail: row.detail, openTask: catalog.publicTask(row.openTask)
    };
  });
  var limitations = snap.analysisLimitations.slice();
  if (!snap.tasks.ok) limitations.push('task-origin-index-partial');
  if (!snap.tasks.historyOk) limitations.push('task-history-index-partial');
  if (!historyStatus.available) limitations.push(historyStatus.reason || 'history-starts-after-update');
  else if (snap.generationMode === 'generation' && !historyCurrent) limitations.push('changed-history-not-current');
  if (!snap.surfaces.rows.some(function (row) { return row.drift !== null; })) limitations.push('surface-drift-not-checked');
  return Object.assign(catalog.publicMeta(snap), {
    ok: true,
    status: 200,
    readiness: {
      state: integration.status === 'ready' ? 'ready' : 'figma-not-ready',
      reasonCode: integration.reasonCode,
      actions: {
        openIntegration: '#figma',
        canSync: integration.actions.canSync,
        reviewTarget: pendingFindings.some(function (row) {
          return row.entityType === 'surface';
        }) ? '#design?tab=surfaces' : '#design?tab=components'
      }
    },
    freshness: {
      state: freshnessState,
      lastSuccessfulSyncAt: lastAt,
      ageMs: age,
      selectedFileMatches: snap.active.mode !== 'generation' || !fileFingerprint ? null : !fileMismatch,
      accountMatches: snap.active.mode !== 'generation' || !accountFingerprint ? null : !accountMismatch,
      partial: generationPartial(snap.active)
    },
    metrics: {
      figmaSyncFreshness: metric(lastAt, lastAt ? freshnessState : 'unknown'),
      tokenCoverage: tokenCoverage
        ? metric(tokenCoverage.percent, tokenCoverage.percent === null ? 'unsupported' : 'known', tokenCoverage)
        : metric(null, 'not-checked'),
      componentCoverage: componentCoverage
        ? metric(componentCoverage.percent, componentCoverage.percent === null ? 'unsupported' : 'known', componentCoverage)
        : metric(null, 'not-checked'),
      driftedSurfaces: metric(
        snap.surfaces.rows.some(function (row) { return row.drift !== null; }) ? driftedSurfaces : null,
        snap.surfaces.rows.some(function (row) { return row.drift !== null; }) ? 'known' : 'not-checked'
      ),
      openTasks: metric(openTasks, openTasks === null ? 'unknown' : 'known')
    },
    attentionItems: publicAttention,
    recentChanges: recent,
    openTasks: designTasks.slice(0, 50).map(catalog.publicTask).filter(Boolean),
    lastSync: latestSyncAttempt,
    analysisLimitations: limitations.filter(function (item, index, list) { return list.indexOf(item) === index; })
  });
}

function generationPartial(active) {
  return !active || active.mode !== 'generation' || generation.isPartial(active.manifest);
}

module.exports = { STALE_MS: STALE_MS, get: get };
