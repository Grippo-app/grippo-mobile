'use strict';

var catalog = require('./api-catalog');
var relations = require('./api-relations');
var reviews = require('./api-change-reviews');

var SEVERITIES = Object.freeze({
  attention: 1, breaking: 1, 'potentially-breaking': 1, compatible: 1, info: 1
});
var IMPACT_MAX = 100;

function queryValue(value, max) {
  if (value == null || value === '') return '';
  if (typeof value !== 'string' || value.normalize('NFC') !== value ||
      /[\x00-\x1f\x7f]/.test(value) || Buffer.byteLength(value, 'utf8') > (max || 200)) return null;
  return value.trim();
}
function parse(params) {
  var allowed = [
    'severity', 'kind', 'query', 'operationId', 'modelId',
    'hasTask', 'cursor', 'limit', 'expectedGenerationId'
  ];
  if (Object.keys(params || {}).some(function (key) { return allowed.indexOf(key) < 0; })) {
    return { error: 'bad-api-changes-query' };
  }
  var query = queryValue(params.query, 200);
  var operationId = queryValue(params.operationId, 200);
  var modelId = queryValue(params.modelId, 200);
  var kind = queryValue(params.kind, 100);
  if (query === null || operationId === null || modelId === null || kind === null ||
      params.severity && !SEVERITIES[params.severity] ||
      params.hasTask && ['yes', 'no'].indexOf(params.hasTask) < 0 ||
      kind && !/^[a-z][a-z0-9-]{0,99}$/.test(kind) ||
      params.limit && (!/^[1-9][0-9]{0,2}$/.test(params.limit) ||
        Number(params.limit) > catalog.MAX_LIMIT)) {
    return { error: 'bad-api-changes-query' };
  }
  return {
    severity: params.severity || '',
    kind: kind || '',
    query: (query || '').toLowerCase(),
    operationId: operationId || '',
    modelId: modelId || '',
    hasTask: params.hasTask || '',
    cursor: params.cursor || '',
    limit: params.limit ? Number(params.limit) : catalog.DEFAULT_LIMIT
  };
}
function impact(row, normalized) {
  var hasOperation = row.operationId &&
    Object.prototype.hasOwnProperty.call(normalized.byOperation, row.operationId);
  var endpoints = hasOperation
    ? [normalized.byOperation[row.operationId]] : [];
  if (!endpoints.length && row.modelId &&
      Object.prototype.hasOwnProperty.call(normalized.byModel, row.modelId)) {
    endpoints = normalized.byModel[row.modelId] || [];
  }
  var truncated = endpoints.length > IMPACT_MAX;
  endpoints = endpoints.slice(0, IMPACT_MAX);
  var consumerSeen = Object.create(null);
  var consumers = [];
  endpoints.forEach(function (endpoint) {
    endpoint._consumerRows.forEach(function (consumer) {
      if (consumers.length >= IMPACT_MAX || consumerSeen[consumer.id]) {
        if (!consumerSeen[consumer.id]) truncated = true;
        return;
      }
      consumerSeen[consumer.id] = 1;
      consumers.push({
        id: consumer.id,
        architectureId: relations.safeString(consumer.architectureId, 300),
        file: relations.safePath(consumer.file),
        symbol: relations.safeString(consumer.symbol, 300)
      });
    });
  });
  var states = endpoints.map(function (endpoint) {
    return endpoint.implementation;
  });
  var statuses = endpoints.map(function (endpoint) {
    return endpoint.consumers.analysisStatus;
  });
  var affectedModels = row.modelId ? [row.modelId] : endpoints.length
    ? endpoints.flatMap(function (endpoint) {
      return endpoint.models.requestIds.concat(endpoint.models.responseIds);
    }).filter(function (id, index, list) {
      return list.indexOf(id) === index;
    }) : [];
  if (affectedModels.length > IMPACT_MAX) truncated = true;
  return {
    affectedImplementation: states.length === 1 ? states[0] : null,
    affectedImplementations: states,
    affectedModels: affectedModels.slice(0, IMPACT_MAX),
    affectedConsumers: consumers,
    consumerAnalysisStatus: statuses.length && statuses.every(function (status) {
      return status === 'complete';
    }) ? 'complete' : statuses.some(function (status) {
      return status === 'partial';
    }) ? 'partial' : 'not-checked',
    noKnownConsumersIsConclusive: !!(!truncated && endpoints.length && !consumers.length &&
      statuses.every(function (status) { return status === 'complete'; })),
    truncated: truncated
  };
}
function list(params) {
  params = params || {};
  var snapshot = relations.snapshot();
  if (!snapshot.ok) return snapshot;
  var conflict = relations.checkExpected(snapshot, params.expectedGenerationId);
  if (conflict) return conflict;
  var filters = parse(params || {});
  if (filters.error) return { ok: false, status: 400, error: filters.error };
  if (snapshot.empty) return Object.assign(relations.meta(snapshot), {
    ok: true, status: 200, empty: true, changeSet: null, items: [],
    page: { returned: 0, total: 0, nextCursor: null, responseTruncated: false }
  });
  var normalized = catalog.normalized(snapshot);
  var report = snapshot.changes.current ? snapshot.changes.value : null;
  var reviewState = reviews.state(report && report.changeSetId);
  var rows = normalized.changes.map(function (row) {
    return Object.assign({}, row, {
      endpointOperationId: row.operationId &&
        Object.prototype.hasOwnProperty.call(normalized.byOperation, row.operationId)
        ? row.operationId : null,
      impact: impact(row, normalized),
      reviewed: !!reviewState.reviewed[row.id]
    });
  }).filter(function (row) {
    if (filters.severity === 'attention' &&
        (row.reviewed ||
          row.severity !== 'breaking' && row.severity !== 'potentially-breaking')) return false;
    if (filters.severity && filters.severity !== 'attention' &&
        row.severity !== filters.severity) return false;
    if (filters.kind && row.kind !== filters.kind) return false;
    if (filters.operationId && row.operationId !== filters.operationId) return false;
    if (filters.modelId && row.modelId !== filters.modelId) return false;
    if (filters.hasTask === 'yes' && !row.tasks.open.length) return false;
    if (filters.hasTask === 'no' && row.tasks.open.length) return false;
    if (filters.query) {
      var text = [
        row.id, row.sourceId, row.kind, row.operationId, row.modelId,
        row.beforeSummary, row.afterSummary
      ].concat(row.evidence || []).filter(Boolean).join(' ').toLowerCase();
      if (text.indexOf(filters.query) < 0) return false;
    }
    return true;
  });
  var filterHash = relations.sha(JSON.stringify({
    severity: filters.severity, kind: filters.kind, query: filters.query,
    operationId: filters.operationId, modelId: filters.modelId, hasTask: filters.hasTask,
    reviewRevision: reviewState.revision,
    projectCodeRevision: snapshot.projectCodeRevision,
    reportHashes: snapshot.reportHashes,
    taskIndexRevision: snapshot.tasks && snapshot.tasks.revision || null
  }));
  var offset = 0;
  if (filters.cursor) {
    var cursor = catalog.decodeCursor(filters.cursor);
    if (!cursor || cursor.generationId !== snapshot.committedGenerationId ||
        cursor.filterHash !== filterHash || !Number.isSafeInteger(cursor.offset) ||
        cursor.offset < 0 || cursor.offset > rows.length) {
      return Object.assign(relations.meta(snapshot), {
        ok: false, status: 409, error: 'api-cursor-invalid'
      });
    }
    offset = cursor.offset;
  }
  var limitations = snapshot.limitations.slice();
  if (reviewState.limitation) limitations.push(reviewState.limitation);
  if (rows.some(function (row) { return row.impact.truncated; })) {
    limitations.push('api-change-impact-cap');
  }
  var base = Object.assign(relations.meta(snapshot), {
    ok: true,
    status: 200,
    empty: false,
    changeSet: report ? {
      id: report.changeSetId || null,
      classifierVersion: report.classifierVersion || null,
      previousHash: report.previousHash || null,
      currentHash: report.currentHash,
      generatedAt: report.generatedAt,
      summary: report.summary || null,
      reviewRevision: reviewState.revision
    } : null,
    limitations: limitations
  });
  return catalog.boundedListResponse(
    base, rows, offset, filters.limit,
    { generationId: snapshot.committedGenerationId, filterHash: filterHash }
  );
}

module.exports = {
  list: list,
  _test: {
    parse: parse,
    impact: impact
  }
};
