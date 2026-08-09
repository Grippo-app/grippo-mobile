'use strict';

var catalog = require('./api-catalog');
var relations = require('./api-relations');
var reviews = require('./api-change-reviews');

function daysSince(value) {
  var parsed = Date.parse(value || '');
  if (!Number.isFinite(parsed)) return null;
  return Math.max(0, Math.floor((Date.now() - parsed) / 86400000));
}
function metric(value, denominator, state) {
  return {
    value: Number.isInteger(value) ? value : null,
    denominator: Number.isInteger(denominator) ? denominator : null,
    analysisStatus: state
  };
}
function mismatchAffectedCount(rows) {
  var ids = Object.create(null);
  rows.forEach(function (row) {
    if (row.operationId) ids['operation:' + row.operationId] = 1;
    else if (row.modelId) ids['model:' + row.modelId] = 1;
    else ids['finding:' + row.id] = 1;
  });
  return Object.keys(ids).length;
}
function overview(expectedGenerationId) {
  var snapshot = relations.snapshot();
  if (!snapshot.ok) return snapshot;
  var conflict = relations.checkExpected(snapshot, expectedGenerationId);
  if (conflict) return conflict;
  var meta = relations.meta(snapshot);
  if (snapshot.empty) {
    return Object.assign(meta, {
      ok: true, status: 200, empty: true,
      context: {
        activeEnvironment: snapshot.activeEnvironment,
        snapshotEnvironmentId: null,
        environmentMismatch: false,
        freshness: { state: 'missing', pulledAt: null, ageDays: null },
        changeSourceHref: '#backend'
      },
      metrics: {
        implemented: metric(null, null, 'not-checked'),
        missing: metric(null, null, 'not-checked'),
        drifted: metric(null, null, 'not-checked'),
        breakingChanges: metric(null, null, 'not-checked')
      },
      primaryAction: { kind: 'refresh-contract', href: '#backend' },
      priorities: {
        breakingChanges: [], missingEndpoints: [], observedMismatches: [],
        recentlyChanged: [], openTasks: []
      }
    });
  }
  var normalized = catalog.normalized(snapshot);
  meta = relations.meta(snapshot);
  var changeReport = snapshot.changes.current ? snapshot.changes.value : null;
  var reviewState = reviews.state(changeReport && changeReport.changeSetId);
  if (reviewState.limitation) meta.limitations = meta.limitations.concat([
    reviewState.limitation
  ]);
  var total = normalized.rows.length;
  var implementationCoverage = snapshot.implementation && snapshot.implementation.coverage;
  var implementationState = snapshot.implementation
    ? snapshot.implementation.analysisStatus === 'complete' ? 'complete' : 'partial'
    : 'not-checked';
  var driftCurrent = snapshot.drift.current;
  var changeCurrent = snapshot.changes.current;
  var driftedCount = driftCurrent ? mismatchAffectedCount(normalized.mismatches) : null;
  var driftScope = driftCurrent
    ? Math.max(total + (snapshot.inventory.stats.schemas || 0), driftedCount)
    : null;
  var attentionChanges = normalized.changes.filter(function (row) {
    return (row.severity === 'breaking' || row.severity === 'potentially-breaking') &&
      !reviewState.reviewed[row.id];
  });
  var missing = normalized.rows.filter(function (row) {
    return row.implementation.state === 'missing';
  });
  var openTasks = (snapshot.tasks.items || []).filter(function (task) {
    return task.source && task.source.kind === 'api';
  }).slice(0, 10).map(relations.publicTask);
  var pulledAt = snapshot.current.manifest.committedAt ||
    snapshot.inventory.source && snapshot.inventory.source.fetchedAt || null;
  var ageDays = daysSince(pulledAt);
  var freshness = snapshot.environmentMismatch ? 'needs-refresh' :
    ageDays !== null && ageDays >= 7 ? 'stale' : 'current';
  var primaryAction = snapshot.environmentMismatch ? {
    kind: 'refresh-contract', href: '#backend'
  } : attentionChanges.length ? {
    kind: 'review-breaking-changes', href: '#api?tab=changes&severity=attention'
  } : missing.length ? {
    kind: 'implement-missing', href: '#api?tab=endpoints&implementation=missing'
  } : freshness !== 'current' ? {
    kind: 'refresh-contract', href: '#backend'
  } : {
    kind: 'view-endpoints', href: '#api?tab=endpoints'
  };
  return Object.assign(meta, {
    ok: true,
    status: 200,
    empty: false,
    context: {
      activeEnvironment: snapshot.activeEnvironment,
      snapshotEnvironmentId: snapshot.environmentId,
      environmentMismatch: snapshot.environmentMismatch,
      freshness: { state: freshness, pulledAt: pulledAt, ageDays: ageDays },
      sourceKind: snapshot.current.manifest.sourceKind,
      contractTitle: relations.safeString(snapshot.inventory.source && snapshot.inventory.source.title, 300),
      changeSourceHref: '#backend'
    },
    metrics: {
      implemented: metric(
        implementationCoverage && implementationCoverage.implemented,
        total,
        implementationState
      ),
      missing: metric(
        implementationCoverage && implementationCoverage.missing,
        total,
        implementationState
      ),
      drifted: metric(
        driftedCount,
        driftScope,
        driftCurrent ? 'complete' : 'not-checked'
      ),
      breakingChanges: metric(
        changeCurrent ? attentionChanges.length : null,
        changeCurrent ? normalized.changes.length : null,
        changeCurrent ? 'complete' : 'not-checked'
      )
    },
    primaryAction: primaryAction,
    priorities: {
      breakingChanges: attentionChanges.slice(0, 5).map(function (row) {
        return {
          id: row.id, severity: row.severity, operationId: row.operationId,
          endpointOperationId: row.operationId && normalized.byOperation[row.operationId]
            ? row.operationId : null,
          modelId: row.modelId, summary: row.afterSummary || row.beforeSummary,
          task: row.tasks.open[0] || null
        };
      }),
      missingEndpoints: missing.slice(0, 5).map(function (row) {
        return {
          operationId: row.operationId, method: row.method, path: row.path,
          area: row.area, task: row.tasks.open[0] || null
        };
      }),
      observedMismatches: normalized.mismatches.slice(0, 5).map(function (row) {
        return {
          id: row.id, severity: row.severity, operationId: row.operationId,
          endpointOperationId: row.operationId && normalized.byOperation[row.operationId]
            ? row.operationId : null,
          modelId: row.modelId, kind: row.kind,
          summary: row.message, task: row.tasks.open[0] || null
        };
      }),
      recentlyChanged: normalized.changes.slice(0, 5).map(function (row) {
        return {
          id: row.id, severity: row.severity, operationId: row.operationId,
          endpointOperationId: row.operationId && normalized.byOperation[row.operationId]
            ? row.operationId : null,
          modelId: row.modelId, summary: row.afterSummary || row.beforeSummary
        };
      }),
      openTasks: openTasks
    }
  });
}

module.exports = {
  overview: overview,
  _test: {
    daysSince: daysSince,
    metric: metric
  }
};
