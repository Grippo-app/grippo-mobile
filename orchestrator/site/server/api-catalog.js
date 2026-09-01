'use strict';

// Normalized generation-bound Project -> API catalog. List projections never
// include schemas or examples; those are loaded only by detail/model reads.

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var relations = require('./api-relations');

var DEFAULT_LIMIT = 100;
var MAX_LIMIT = 500;
var RESPONSE_MAX = 1024 * 1024;
var DETAIL_RESPONSE_MAX = 2 * 1024 * 1024;
var ARTIFACT_MAX = 10 * 1024 * 1024;
var MODEL_SCAN_MAX = 64 * 1024 * 1024;
var EXAMPLE_NODE_MAX = 10000;
var MODEL_RELATION_VISITS_MAX = 200000;
var CURSOR_SECRET = crypto.randomBytes(32);
var catalogCache = { key: null, value: null };
var METHODS = Object.freeze({ GET: 1, POST: 1, PUT: 1, PATCH: 1, DELETE: 1 });
var IMPLEMENTATION_STATES = Object.freeze({
  implemented: 1, partial: 1, unknown: 1
});
var CHANGE_SEVERITIES = Object.freeze({
  breaking: 1, 'potentially-breaking': 1, compatible: 1, info: 1
});
var MISMATCH_STATUSES = Object.freeze({ present: 1, none: 1, 'not-checked': 1 });
var CONSUMER_PRESENCE = Object.freeze({ yes: 1, no: 1, unknown: 1 });

function stable(value) {
  if (Array.isArray(value)) return value.map(stable);
  if (!value || typeof value !== 'object') return value;
  var out = Object.create(null);
  Object.keys(value).sort().forEach(function (key) { out[key] = stable(value[key]); });
  return out;
}
function hash(value) { return relations.sha(JSON.stringify(stable(value))); }
function own(value, key) {
  return !!value && Object.prototype.hasOwnProperty.call(value, key);
}
function exactQuery(params, allowed) {
  return Object.keys(params || {}).every(function (key) {
    return allowed.indexOf(key) >= 0;
  });
}
function safeQuery(value) {
  if (value == null || value === '') return '';
  if (typeof value !== 'string' || value.normalize('NFC') !== value ||
      /[\x00-\x1f\x7f]/.test(value) || Buffer.byteLength(value, 'utf8') > 200) return null;
  return value.trim().toLowerCase();
}
function normalizeLimit(value) {
  if (value == null || value === '') return DEFAULT_LIMIT;
  if (!/^[1-9][0-9]{0,2}$/.test(String(value))) return null;
  var parsed = Number(value);
  return parsed <= MAX_LIMIT ? parsed : null;
}
function cursorSignature(body) {
  return crypto.createHmac('sha256', CURSOR_SECRET).update(body, 'utf8').digest('hex');
}
function encodeCursor(value) {
  var body = Buffer.from(JSON.stringify(value), 'utf8').toString('base64url');
  return body + '.' + cursorSignature(body);
}
function decodeCursor(cursor) {
  if (typeof cursor !== 'string' || cursor.length > 2048) return null;
  var match = /^([A-Za-z0-9_-]+)\.([a-f0-9]{64})$/.exec(cursor);
  if (!match) return null;
  var expected = cursorSignature(match[1]);
  var supplied = match[2];
  if (!crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(supplied))) return null;
  try {
    var value = JSON.parse(Buffer.from(match[1], 'base64url').toString('utf8'));
    return value && typeof value === 'object' && !Array.isArray(value) ? value : null;
  } catch (error) { return null; }
}
function roleRow(snapshot, role) {
  if (!snapshot._apiRoleIndex) {
    var index = Object.create(null);
    snapshot.current.manifest.artifacts.forEach(function (row) {
      if (row && typeof row.role === 'string') index[row.role] = row;
    });
    snapshot._apiRoleIndex = index;
  }
  return own(snapshot._apiRoleIndex, role) ? snapshot._apiRoleIndex[role] : null;
}
function readRoleJson(snapshot, role) {
  var file = snapshot.current.artifacts[role];
  var row = roleRow(snapshot, role);
  if (!file || !row) return null;
  try {
    var hit = fileGuards.boundedRegularFileUnder(
      paths.PROJECT_ROOT, path.dirname(file), file, Math.min(ARTIFACT_MAX, row.size)
    );
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1' ||
        hit.bytes.length !== row.size || relations.sha(hit.bytes) !== row.hash) return null;
    var value = JSON.parse(hit.bytes.toString('utf8'));
    if (!value || typeof value !== 'object' || Array.isArray(value)) return null;
    if (role.indexOf('area:') === 0 &&
        !relations.validArea(value, role.slice(5))) return null;
    return value;
  } catch (error) { return null; }
}
function allAreas(snapshot) {
  var out = Object.create(null);
  var roles = Object.keys(snapshot.current.artifacts).filter(function (role) {
    return role.indexOf('area:') === 0;
  }).sort();
  var total = roles.reduce(function (sum, role) {
    var row = roleRow(snapshot, role);
    return sum + (row && Number.isSafeInteger(row.size) ? row.size : MODEL_SCAN_MAX + 1);
  }, 0);
  if (total > MODEL_SCAN_MAX) {
    return { ok: false, error: 'api-model-catalog-too-large' };
  }
  for (var index = 0; index < roles.length; index++) {
    var role = roles[index];
    var value = readRoleJson(snapshot, role);
    if (!value) return { ok: false, error: 'api-area-unavailable' };
    out[role.slice(5)] = value;
  }
  return { ok: true, value: out };
}
function endpointModels(endpoint) {
  var requestIds = [], responseIds = [];
  if (endpoint.request && endpoint.request.body && endpoint.request.body.schemaRef) {
    requestIds.push(endpoint.request.body.schemaRef);
  }
  Object.keys(endpoint.response || {}).sort().forEach(function (status) {
    var ref = endpoint.response[status] && endpoint.response[status].schemaRef;
    if (ref && responseIds.indexOf(ref) < 0) responseIds.push(ref);
  });
  return { requestIds: requestIds, responseIds: responseIds };
}
function endpointRootsAvailable(endpoint, schemas) {
  var models = endpointModels(endpoint);
  return models.requestIds.concat(models.responseIds).every(function (modelId) {
    return own(schemas, modelId);
  });
}
function modelReachable(rootIds, schemas, targetId, budget) {
  var pending = rootIds.slice();
  var seen = Object.create(null);
  var visited = 0;
  budget = budget || { visits: 0 };
  while (pending.length && visited < 10000 &&
      budget.visits < MODEL_RELATION_VISITS_MAX) {
    var id = pending.shift();
    if (!id || seen[id]) continue;
    if (id === targetId) return { found: true, truncated: false };
    seen[id] = 1;
    visited++;
    budget.visits++;
    var schema = own(schemas, id) ? schemas[id] : null;
    (schema && Array.isArray(schema.fields) ? schema.fields : []).forEach(function (field) {
      if (typeof field.type === 'string' && field.type.indexOf('ref:') === 0) {
        pending.push(field.type.slice(4));
      }
      if (typeof field.itemsRef === 'string' && field.itemsRef) pending.push(field.itemsRef);
    });
  }
  return { found: false, truncated: pending.length > 0 };
}
function relationModelIndex(rows, endpoints, snapshot) {
  var byModel = Object.create(null);
  var operations = Object.create(null);
  endpoints.forEach(function (endpoint) {
    operations[endpoint.operationId] = 1;
  });
  var targets = Object.create(null);
  (Array.isArray(rows) ? rows : []).forEach(function (row) {
    if (row && row.modelId && !(row.operationId && operations[row.operationId])) {
      targets[row.modelId] = 1;
    }
  });
  if (!Object.keys(targets).length) return { byModel: byModel, truncated: false };

  var areaEndpoints = Object.create(null);
  endpoints.forEach(function (endpoint) {
    if (!areaEndpoints[endpoint.area]) areaEndpoints[endpoint.area] = [];
    areaEndpoints[endpoint.area].push(endpoint);
  });
  var total = Object.keys(areaEndpoints).reduce(function (sum, area) {
    var row = roleRow(snapshot, 'area:' + area);
    return sum + (row && Number.isSafeInteger(row.size) ? row.size : MODEL_SCAN_MAX + 1);
  }, 0);
  if (total > MODEL_SCAN_MAX) return { byModel: byModel, truncated: true };

  var visits = 0;
  var truncated = false;
  Object.keys(areaEndpoints).sort().forEach(function (area) {
    if (visits >= MODEL_RELATION_VISITS_MAX) {
      truncated = true;
      return;
    }
    var slice = readRoleJson(snapshot, 'area:' + area);
    if (!slice || !slice.schemas) {
      truncated = true;
      return;
    }
    areaEndpoints[area].forEach(function (endpoint) {
      if (visits >= MODEL_RELATION_VISITS_MAX) {
        truncated = true;
        return;
      }
      var pending = endpointModels(endpoint).requestIds
        .concat(endpointModels(endpoint).responseIds);
      var seen = Object.create(null);
      while (pending.length && visits < MODEL_RELATION_VISITS_MAX) {
        var id = pending.shift();
        if (!id || seen[id]) continue;
        seen[id] = 1;
        visits++;
        if (targets[id]) {
          if (!byModel[id]) byModel[id] = [];
          if (byModel[id].indexOf(endpoint.operationId) < 0) {
            byModel[id].push(endpoint.operationId);
          }
        }
        var schema = own(slice.schemas, id) ? slice.schemas[id] : null;
        (schema && Array.isArray(schema.fields) ? schema.fields : [])
          .forEach(function (field) {
            if (typeof field.type === 'string' && field.type.indexOf('ref:') === 0) {
              pending.push(field.type.slice(4));
            }
            if (typeof field.itemsRef === 'string' && field.itemsRef) {
              pending.push(field.itemsRef);
            }
          });
      }
      if (pending.length) truncated = true;
    });
  });
  return { byModel: byModel, truncated: truncated };
}
function mismatchRows(snapshot) {
  if (!snapshot.drift.current || !snapshot.drift.value) return [];
  var occurrences = Object.create(null);
  return snapshot.drift.value.findings.slice(0, 10000).map(function (finding) {
    if (!finding || typeof finding !== 'object') return null;
    var severity = String(finding.severity || '').toUpperCase();
    var identity = JSON.stringify([
      finding.kind || null, finding.operationId || null, finding.area || null,
      finding.schemaRef || null, finding.field || null, finding.dtoFile || null
    ]);
    var occurrence = occurrences[identity] || 0;
    occurrences[identity] = occurrence + 1;
    var id = relations.mismatchId(finding, occurrence);
    var operationId = relations.safeString(finding.operationId, 200);
    return {
      id: id,
      sourceId: relations.sourceId('mismatch', id),
      operationId: operationId,
      area: relations.safeString(finding.area, 100),
      modelId: relations.safeString(finding.schemaRef, 200),
      field: relations.safeString(finding.field, 200),
      kind: relations.safeString(finding.kind, 100) || 'observed-contract-mismatch',
      severity: severity === 'ERROR' ? 'error' : severity === 'WARNING' ? 'warning' : 'info',
      message: relations.safeString(finding.message, 1000),
      suggestion: relations.safeString(finding.suggestion, 1000),
      file: relations.safePath(finding.dtoFile),
      tasks: relations.taskProjection(snapshot.tasks, [relations.sourceId('mismatch', id)])
    };
  }).filter(Boolean);
}
function changeRows(snapshot) {
  if (!snapshot.changes.current || !snapshot.changes.value ||
      !Array.isArray(snapshot.changes.value.changes)) return [];
  return snapshot.changes.value.changes.slice(0, 10000).map(function (change) {
    if (!change || typeof change !== 'object' ||
        !/^chg-[a-f0-9]{24}$/.test(String(change.id || '')) ||
        !CHANGE_SEVERITIES[change.severity]) return null;
    var sourceId = relations.sourceId('change', change.id);
    return {
      id: change.id,
      sourceId: sourceId,
      kind: relations.safeString(change.kind, 100),
      severity: change.severity,
      operationId: relations.safeString(change.operationId, 200),
      modelId: relations.safeString(change.modelId, 200),
      beforeSummary: relations.safeString(change.beforeSummary, 500),
      afterSummary: relations.safeString(change.afterSummary, 500),
      evidence: (Array.isArray(change.evidence) ? change.evidence : [])
        .slice(0, 20).map(function (row) { return relations.safeString(row, 500); }).filter(Boolean),
      tasks: relations.taskProjection(snapshot.tasks, [sourceId])
    };
  }).filter(Boolean);
}
function mismatchIndex(rows, endpoints, relationModels) {
  var byOperation = Object.create(null);
  var byModel = relationModels || Object.create(null);
  var byArea = Object.create(null);
  endpoints.forEach(function (endpoint) { byOperation[endpoint.operationId] = []; });
  endpoints.forEach(function (endpoint) {
    if (!byArea[endpoint.area]) byArea[endpoint.area] = [];
    byArea[endpoint.area].push(endpoint.operationId);
  });
  var truncated = false;
  (Array.isArray(rows) ? rows : []).forEach(function (row) {
    if (row.operationId && byOperation[row.operationId]) {
      if (byOperation[row.operationId].length < 500) byOperation[row.operationId].push(row);
      else truncated = true;
      return;
    }
    var targets = row.modelId && byModel[row.modelId] || [];
    if (!targets.length && row.area && byArea[row.area] && byArea[row.area].length === 1) {
      targets = byArea[row.area];
    }
    if (targets.length > 500) truncated = true;
    targets.slice(0, 500).forEach(function (operationId) {
      if (byOperation[operationId].length < 500) byOperation[operationId].push(row);
      else truncated = true;
    });
  });
  return { byOperation: byOperation, truncated: truncated };
}
function changeIndex(rows, endpoints, relationModels) {
  var byOperation = Object.create(null);
  var byModel = relationModels || Object.create(null);
  endpoints.forEach(function (endpoint) { byOperation[endpoint.operationId] = []; });
  var truncated = false;
  rows.forEach(function (row) {
    if (row.operationId && byOperation[row.operationId]) {
      if (byOperation[row.operationId].length < 500) byOperation[row.operationId].push(row);
      else truncated = true;
      return;
    }
    var targets = row.modelId && byModel[row.modelId] || [];
    if (targets.length > 500) truncated = true;
    targets.slice(0, 500).forEach(function (operationId) {
      if (byOperation[operationId].length < 500) byOperation[operationId].push(row);
      else truncated = true;
    });
  });
  return { byOperation: byOperation, truncated: truncated };
}
function mismatchSeverity(rows) {
  if (!rows) return 'not-checked';
  if (rows.some(function (row) { return row.severity === 'error'; })) return 'error';
  if (rows.some(function (row) { return row.severity === 'warning'; })) return 'warning';
  return rows.length ? 'info' : 'none';
}
function operationRowIndex(rows) {
  var out = Object.create(null);
  rows.forEach(function (row) { out[row.operationId] = row; });
  return out;
}
function modelRowIndex(rows, rowsByOperation, relationModels) {
  var out = Object.create(null);
  rows.forEach(function (row) {
    Array.from(new Set(row.models.requestIds.concat(row.models.responseIds)))
      .forEach(function (modelId) {
        if (!out[modelId]) out[modelId] = [];
        out[modelId].push(row);
      });
  });
  Object.keys(relationModels || {}).forEach(function (modelId) {
    if (!out[modelId]) out[modelId] = [];
    relationModels[modelId].forEach(function (operationId) {
      var row = own(rowsByOperation, operationId) ? rowsByOperation[operationId] : null;
      if (row && out[modelId].indexOf(row) < 0) out[modelId].push(row);
    });
  });
  return out;
}
function normalized(snapshot) {
  if (snapshot._catalog) return snapshot._catalog;
  var cacheKey = [
    snapshot.committedGenerationId,
    snapshot.contractHash,
    snapshot.projectCodeRevision,
    snapshot.reportHashes && snapshot.reportHashes.implementation,
    snapshot.reportHashes && snapshot.reportHashes.consumers,
    snapshot.reportHashes && snapshot.reportHashes.drift,
    snapshot.reportHashes && snapshot.reportHashes.changes,
    snapshot.activeEnvironment && snapshot.activeEnvironment.id,
    snapshot.tasks && snapshot.tasks.revision
  ].join('|');
  if (catalogCache.key === cacheKey && catalogCache.value) {
    if (catalogCache.value.relationTruncated &&
        snapshot.limitations.indexOf('api-relation-cap') < 0) {
      snapshot.limitations.push('api-relation-cap');
    }
    snapshot._catalog = catalogCache.value;
    return catalogCache.value;
  }
  var endpoints = Array.isArray(snapshot.inventory.endpoints)
    ? snapshot.inventory.endpoints : [];
  var mismatch = mismatchRows(snapshot);
  var changes = changeRows(snapshot);
  var relationModels = relationModelIndex(mismatch.concat(changes), endpoints, snapshot);
  var mismatchRelations = mismatchIndex(
    snapshot.drift.current ? mismatch : null, endpoints, relationModels.byModel
  );
  var changeRelations = changeIndex(changes, endpoints, relationModels.byModel);
  var mismatchesByOperation = mismatchRelations.byOperation;
  var changesByOperation = changeRelations.byOperation;
  var relationTruncated = relationModels.truncated ||
    mismatchRelations.truncated || changeRelations.truncated;
  if (relationTruncated && snapshot.limitations.indexOf('api-relation-cap') < 0) {
    snapshot.limitations.push('api-relation-cap');
  }
  var rows = endpoints.map(function (endpoint) {
    var implementation = snapshot.implementationByOperation[endpoint.operationId] || {
      state: 'unknown', file: null, symbol: null, confidence: null
    };
    var consumer = snapshot.consumersByOperation[endpoint.operationId] || {
      analysisStatus: 'not-checked', consumers: []
    };
    var endpointChanges = changesByOperation[endpoint.operationId] || [];
    var endpointMismatches = snapshot.drift.current
      ? (mismatchesByOperation[endpoint.operationId] || []) : null;
    var models = endpointModels(endpoint);
    var taskRefs = endpointChanges.map(function (change) { return change.sourceId; })
      .concat((endpointMismatches || []).map(function (finding) { return finding.sourceId; }));
    var tasks = relations.taskProjection(snapshot.tasks, taskRefs);
    var row = {
      operationId: endpoint.operationId,
      method: endpoint.method,
      path: endpoint.path,
      area: endpoint.area,
      summary: relations.safeString(endpoint.summary, 500),
      auth: relations.safeString(endpoint.auth, 100) || 'none',
      deprecated: endpoint.deprecated === true,
      contract: {
        sourceHash: snapshot.contractHash,
        lastChangedAt: endpointChanges.length && snapshot.changes.value
          ? snapshot.changes.value.generatedAt : null,
        changeId: endpointChanges.length ? endpointChanges[0].id : null
      },
      implementation: {
        state: IMPLEMENTATION_STATES[implementation.state] ? implementation.state : 'unknown',
        file: relations.safePath(implementation.file),
        symbol: relations.safeString(implementation.symbol, 300),
        confidence: ['exact', 'derived', 'heuristic'].indexOf(implementation.confidence) >= 0
          ? implementation.confidence : null
      },
      consumers: {
        ids: (consumer.consumers || []).slice(0, 500).map(function (item) { return item.id; }),
        analysisStatus: ['complete', 'partial', 'not-checked'].indexOf(consumer.analysisStatus) >= 0
          ? consumer.analysisStatus : 'not-checked'
      },
      models: models,
      mismatch: {
        count: endpointMismatches ? endpointMismatches.length : null,
        severity: mismatchSeverity(endpointMismatches)
      },
      tasks: tasks,
      latestChange: endpointChanges.length ? {
        id: endpointChanges[0].id,
        kind: endpointChanges[0].kind,
        severity: endpointChanges[0].severity
      } : null,
      _changes: endpointChanges,
      _mismatches: endpointMismatches,
      _consumerRows: consumer.consumers || []
    };
    row._search = [
      row.path, row.operationId, row.summary, row.implementation.file,
      row.implementation.symbol
    ].concat(
      models.requestIds,
      models.responseIds,
      endpointChanges.flatMap(function (change) {
        return [change.id, change.kind, change.beforeSummary, change.afterSummary];
      }),
      (endpointMismatches || []).flatMap(function (finding) {
        return [finding.id, finding.kind, finding.message, finding.modelId, finding.field];
      })
    ).filter(Boolean).join(' ').toLowerCase();
    return row;
  });
  var rowsByOperation = operationRowIndex(rows);
  var rowsByModel = modelRowIndex(rows, rowsByOperation, relationModels.byModel);
  snapshot._catalog = {
    rows: rows,
    byOperation: rowsByOperation,
    byModel: rowsByModel,
    mismatches: mismatch,
    changes: changes,
    relationTruncated: relationTruncated
  };
  catalogCache = { key: cacheKey, value: snapshot._catalog };
  return snapshot._catalog;
}
function publicEndpoint(row) {
  return {
    operationId: row.operationId,
    method: row.method,
    path: row.path,
    area: row.area,
    summary: row.summary,
    auth: row.auth,
    deprecated: row.deprecated,
    contract: row.contract,
    implementation: row.implementation,
    consumers: row.consumers,
    models: row.models,
    mismatch: row.mismatch,
    tasks: row.tasks,
    latestChange: row.latestChange
  };
}

function taskSourceFactsFromCatalog(current, sourceIds) {
  current = current || {};
  var changesBySource = Object.create(null);
  var mismatchesBySource = Object.create(null);
  (current.changes || []).forEach(function (change) {
    if (change && change.sourceId) changesBySource[change.sourceId] = change;
  });
  (current.mismatches || []).forEach(function (mismatch) {
    if (mismatch && mismatch.sourceId) mismatchesBySource[mismatch.sourceId] = mismatch;
  });

  return sourceIds.map(function (sourceId) {
    var endpoint = null;
    var finding = null;
    var type = null;
    if (sourceId.indexOf('api:change:') === 0) {
      type = 'api-change';
      finding = changesBySource[sourceId] || null;
    } else if (sourceId.indexOf('api:mismatch:') === 0) {
      type = 'api-mismatch';
      finding = mismatchesBySource[sourceId] || null;
    }
    if (!endpoint && finding && finding.operationId &&
        current.byOperation &&
        Object.prototype.hasOwnProperty.call(current.byOperation, finding.operationId)) {
      endpoint = current.byOperation[finding.operationId];
    }
    var currentSource = !!finding;
    var fallback = finding && (
      finding.afterSummary || finding.beforeSummary ||
      finding.message || finding.kind
    );
    return {
      sourceId: sourceId,
      type: type,
      status: currentSource ? 'current' : 'unavailable',
      label: endpoint
        ? [endpoint.method, endpoint.path].filter(Boolean).join(' ')
        : relations.safeString(fallback, 500) || sourceId,
      operationId: endpoint && endpoint.operationId ||
        finding && finding.operationId || null,
      method: endpoint && endpoint.method || null,
      path: endpoint && endpoint.path || null,
      area: endpoint && endpoint.area ||
        finding && finding.area || null,
      implementationStatus: endpoint && endpoint.implementation &&
        endpoint.implementation.state || null,
      changeStatus: finding && finding.severity ||
        endpoint && endpoint.latestChange && endpoint.latestChange.severity || null
    };
  });
}

function taskSourceFacts(sourceIds) {
  if (!Array.isArray(sourceIds) || sourceIds.length > 12 ||
      sourceIds.some(function (sourceId) {
        return typeof sourceId !== 'string' ||
          !/^(?:api:change:chg-[a-f0-9]{24}|api:mismatch:mismatch-[a-f0-9]{24})$/.test(sourceId);
      })) {
    return { ok: false, status: 400, error: 'bad-api-query' };
  }
  var snapshot = relations.snapshot();
  if (!snapshot.ok) return snapshot;
  var current = snapshot.empty
    ? { rows: [], changes: [], mismatches: [], byOperation: Object.create(null) }
    : normalized(snapshot);
  var items = taskSourceFactsFromCatalog(current, sourceIds).map(function (item) {
    return Object.assign({}, item, {
      environmentId: snapshot.environmentId || null,
      contractHash: snapshot.contractHash || null,
      committedGenerationId: snapshot.committedGenerationId || null
    });
  });
  return Object.assign(relations.meta(snapshot), {
    ok: true,
    status: 200,
    items: items
  });
}

function endpointFilters(params) {
  var allowed = [
    'query', 'area', 'method', 'implementation', 'auth', 'hasTask',
    'changeSeverity', 'mismatch', 'consumers', 'cursor', 'limit',
    'expectedGenerationId'
  ];
  if (!exactQuery(params, allowed)) return { error: 'bad-api-query' };
  var query = safeQuery(params.query);
  if (query === null) return { error: 'bad-api-query' };
  var limit = normalizeLimit(params.limit);
  if (limit === null) return { error: 'bad-api-query' };
  if (params.area && !/^[a-z0-9][a-z0-9-]{0,99}$/.test(params.area)) return { error: 'bad-api-query' };
  if (params.method && !METHODS[params.method]) return { error: 'bad-api-query' };
  if (params.implementation && !IMPLEMENTATION_STATES[params.implementation]) return { error: 'bad-api-query' };
  if (params.auth && relations.safeString(params.auth, 100) !== params.auth) {
    return { error: 'bad-api-query' };
  }
  if (params.hasTask && ['yes', 'no'].indexOf(params.hasTask) < 0) return { error: 'bad-api-query' };
  if (params.changeSeverity && !CHANGE_SEVERITIES[params.changeSeverity]) return { error: 'bad-api-query' };
  if (params.mismatch && !MISMATCH_STATUSES[params.mismatch]) return { error: 'bad-api-query' };
  if (params.consumers && !CONSUMER_PRESENCE[params.consumers]) return { error: 'bad-api-query' };
  return {
    query: query,
    area: params.area || '',
    method: params.method || '',
    implementation: params.implementation || '',
    auth: params.auth || '',
    hasTask: params.hasTask || '',
    changeSeverity: params.changeSeverity || '',
    mismatch: params.mismatch || '',
    consumers: params.consumers || '',
    cursor: params.cursor || '',
    limit: limit
  };
}
function matchesEndpoint(row, filters) {
  if (filters.query && row._search.indexOf(filters.query) < 0) return false;
  if (filters.area && row.area !== filters.area) return false;
  if (filters.method && row.method !== filters.method) return false;
  if (filters.implementation && row.implementation.state !== filters.implementation) return false;
  if (filters.auth && row.auth !== filters.auth) return false;
  if (filters.hasTask === 'yes' && !row.tasks.open.length) return false;
  if (filters.hasTask === 'no' && row.tasks.open.length) return false;
  if (filters.changeSeverity && (!row.latestChange ||
      row.latestChange.severity !== filters.changeSeverity)) return false;
  if (filters.mismatch === 'present' && !(row.mismatch.count > 0)) return false;
  if (filters.mismatch === 'none' && row.mismatch.severity !== 'none') return false;
  if (filters.mismatch === 'not-checked' && row.mismatch.severity !== 'not-checked') return false;
  if (filters.consumers === 'yes' && !row.consumers.ids.length) return false;
  if (filters.consumers === 'no' &&
      (row.consumers.analysisStatus !== 'complete' || row.consumers.ids.length)) return false;
  if (filters.consumers === 'unknown' && row.consumers.analysisStatus !== 'not-checked' &&
      row.consumers.analysisStatus !== 'partial') return false;
  return true;
}
function boundedListResponse(base, all, offset, limit, cursorMeta) {
  var desired = Math.min(limit, Math.max(0, all.length - offset));
  function response(count, truncated) {
    var index = offset + count;
    var nextCursor = index < all.length
      ? encodeCursor(Object.assign({}, cursorMeta, { offset: index })) : null;
    var limitations = base.limitations.slice();
    if (truncated) limitations.push('api-response-size-cap');
    return Object.assign({}, base, {
      items: all.slice(offset, index),
      page: {
        returned: count,
        total: all.length,
        nextCursor: nextCursor,
        responseTruncated: truncated
      },
      limitations: limitations
    });
  }
  var full = response(desired, false);
  if (Buffer.byteLength(JSON.stringify(full), 'utf8') <= RESPONSE_MAX) return full;
  var low = 0, high = desired, best = response(0, true);
  while (low <= high) {
    var middle = Math.floor((low + high) / 2);
    var candidate = response(middle, true);
    if (Buffer.byteLength(JSON.stringify(candidate), 'utf8') <= RESPONSE_MAX) {
      best = candidate;
      low = middle + 1;
    } else {
      high = middle - 1;
    }
  }
  return best;
}
function list(params) {
  params = params || {};
  var snapshot = relations.snapshot();
  if (!snapshot.ok) return snapshot;
  var conflict = relations.checkExpected(snapshot, params.expectedGenerationId);
  if (conflict) return conflict;
  var filters = endpointFilters(params || {});
  if (filters.error) return { ok: false, status: 400, error: filters.error };
  if (snapshot.empty) return Object.assign(relations.meta(snapshot), {
    ok: true, status: 200, empty: true, filters: {}, items: [],
    page: { returned: 0, total: 0, nextCursor: null, responseTruncated: false },
    facets: { areas: [], methods: [], auth: [] }
  });
  var catalog = normalized(snapshot);
  var rows = catalog.rows.filter(function (row) { return matchesEndpoint(row, filters); });
  var filterHash = hash({
    query: filters.query, area: filters.area, method: filters.method,
    implementation: filters.implementation, auth: filters.auth, hasTask: filters.hasTask,
    changeSeverity: filters.changeSeverity, mismatch: filters.mismatch,
    consumers: filters.consumers,
    projectCodeRevision: snapshot.projectCodeRevision,
    reportHashes: snapshot.reportHashes,
    taskIndexRevision: snapshot.tasks && snapshot.tasks.revision || null
  });
  var offset = 0;
  if (filters.cursor) {
    var cursor = decodeCursor(filters.cursor);
    if (!cursor || cursor.generationId !== snapshot.committedGenerationId ||
        cursor.filterHash !== filterHash || !Number.isSafeInteger(cursor.offset) ||
        cursor.offset < 0 || cursor.offset > rows.length) {
      return Object.assign(relations.meta(snapshot), {
        ok: false, status: 409, error: 'api-cursor-invalid'
      });
    }
    offset = cursor.offset;
  }
  var allAreas = Object.keys(snapshot.inventory.areas || {}).sort();
  var allAuth = Array.from(new Set(catalog.rows.map(function (row) { return row.auth; }))).sort();
  var facets = {
    areas: allAreas.slice(0, 500),
    methods: Object.keys(METHODS).filter(function (method) {
      return catalog.rows.some(function (row) { return row.method === method; });
    }),
    auth: allAuth.slice(0, 500)
  };
  var listMeta = relations.meta(snapshot);
  if (allAreas.length > facets.areas.length || allAuth.length > facets.auth.length) {
    listMeta.limitations = listMeta.limitations.concat(['api-facets-cap']);
  }
  var base = Object.assign(listMeta, {
    ok: true, status: 200, empty: false, facets: facets
  });
  return boundedListResponse(
    base,
    rows.map(publicEndpoint),
    offset,
    filters.limit,
    { generationId: snapshot.committedGenerationId, filterHash: filterHash }
  );
}
function findOperation(snapshot, operationId) {
  if (typeof operationId !== 'string' || !operationId ||
      operationId.length > 200 || /[\x00-\x1f\x7f]/.test(operationId)) return null;
  var byOperation = normalized(snapshot).byOperation;
  return own(byOperation, operationId) ? byOperation[operationId] : null;
}
function markExampleTruncated(budget) {
  budget.truncated = true;
  budget.truncationRevision = (budget.truncationRevision || 0) + 1;
}
function fieldExample(field, schemas, seen, depth, budget) {
  if (!field) return null;
  if (depth > 7) {
    markExampleTruncated(budget);
    return null;
  }
  if (budget.nodes >= EXAMPLE_NODE_MAX) {
    markExampleTruncated(budget);
    return null;
  }
  budget.nodes++;
  if (typeof field.type === 'string' && field.type.indexOf('ref:') === 0) {
    return schemaExample(field.type.slice(4), schemas, seen, depth + 1, budget);
  }
  if (field.type === 'array') {
    return field.itemsRef
      ? [schemaExample(field.itemsRef, schemas, seen, depth + 1, budget)] : [];
  }
  if (Array.isArray(field.enum) && field.enum.length) return field.enum[0];
  if (field.type === 'boolean') return false;
  if (field.type === 'integer' || field.type === 'number') return 0;
  if (field.type === 'string') {
    if (field.format === 'date-time') return '2000-01-01T00:00:00Z';
    if (field.format === 'date') return '2000-01-01';
    if (field.format === 'uuid') return '00000000-0000-0000-0000-000000000000';
    if (field.format === 'email') return 'user@example.invalid';
    return 'string';
  }
  return {};
}
function schemaExample(modelId, schemas, seen, depth, budget) {
  if (!modelId) return {};
  if (depth > 7 || seen[modelId]) {
    markExampleTruncated(budget);
    return {};
  }
  var schema = own(schemas, modelId) ? schemas[modelId] : null;
  if (!schema || !Array.isArray(schema.fields)) return {};
  seen[modelId] = true;
  var out = Object.create(null);
  schema.fields.slice(0, 100).forEach(function (field) {
    out[field.jsonName || field.name] = fieldExample(
      field, schemas, seen, depth + 1, budget
    );
  });
  if (schema.fields.length > 100) {
    markExampleTruncated(budget);
  }
  delete seen[modelId];
  return out;
}
function secretKey(key) {
  var compact = String(key || '').replace(/[^A-Za-z0-9]/g, '').toLowerCase();
  return /(?:token|secret|password|passwd|authorization|apikey|cookie|sessionid|signature|credential|privatekey|clientkey)/
    .test(compact);
}
function sanitizeExample(value, key, budget, depth) {
  if (budget.nodes >= 1000 || depth > 8) {
    budget.truncated = true;
    return '[truncated]';
  }
  budget.nodes++;
  if (secretKey(String(key || ''))) return '[redacted]';
  if (value === null || typeof value === 'boolean' || typeof value === 'number') return value;
  if (typeof value === 'string') {
    if (/^Bearer\s+/i.test(value) || /^eyJ[A-Za-z0-9_-]{10,}\./.test(value) ||
        /(?:token|secret|password|api[_-]?key)=/i.test(value)) return '[redacted]';
    var codePoints = Array.from(value);
    if (codePoints.length > 1000) budget.truncated = true;
    return codePoints.slice(0, 1000).join('');
  }
  if (Array.isArray(value)) {
    if (value.length > 20) budget.truncated = true;
    return value.slice(0, 20).map(function (item) {
      return sanitizeExample(item, key, budget, depth + 1);
    });
  }
  if (value && typeof value === 'object') {
    var out = Object.create(null);
    var keys = Object.keys(value).sort();
    if (keys.length > 100) budget.truncated = true;
    keys.slice(0, 100).forEach(function (childKey) {
      out[childKey] = sanitizeExample(value[childKey], childKey, budget, depth + 1);
    });
    return out;
  }
  return null;
}
function mediaExample(media) {
  if (!media || typeof media !== 'object') return { present: false, value: null };
  if (Object.prototype.hasOwnProperty.call(media, 'example')) {
    return { present: true, value: media.example };
  }
  var keys = media.examples && typeof media.examples === 'object'
    ? Object.keys(media.examples).sort() : [];
  if (!keys.length) return { present: false, value: null };
  var first = media.examples[keys[0]];
  return first && typeof first === 'object' &&
    Object.prototype.hasOwnProperty.call(first, 'value')
    ? { present: true, value: first.value }
    : { present: false, value: null };
}
function localReference(spec, value, prefix) {
  var current = value;
  for (var depth = 0; depth < 5; depth++) {
    if (!current || typeof current !== 'object' || Array.isArray(current) ||
        typeof current.$ref !== 'string') return current;
    if (current.$ref.indexOf(prefix) !== 0 || current.$ref.length > 1000) return null;
    var parts = current.$ref.slice(2).split('/').map(function (part) {
      return part.replace(/~1/g, '/').replace(/~0/g, '~');
    });
    current = parts.reduce(function (node, part) {
      return node && typeof node === 'object' &&
        Object.prototype.hasOwnProperty.call(node, part) ? node[part] : null;
    }, spec);
  }
  return null;
}
function explicitExamples(snapshot, endpoint) {
  var spec = readRoleJson(snapshot, 'normalized-spec');
  if (!spec || !spec.paths || !spec.paths[endpoint.path]) {
    return {
      request: null, requestPresent: false, requestTruncated: false,
      requestContentType: null, responses: {}
    };
  }
  var operation = spec.paths[endpoint.path][endpoint.method.toLowerCase()];
  if (!operation || typeof operation !== 'object') {
    return {
      request: null, requestPresent: false, requestTruncated: false,
      requestContentType: null, responses: {}
    };
  }
  var request = null;
  var requestPresent = false;
  var requestBody = localReference(spec, operation.requestBody, '#/components/requestBodies/');
  var requestContent = requestBody && requestBody.content || {};
  var requestType = requestContent['application/json'] ? 'application/json' : Object.keys(requestContent).sort()[0];
  if (requestType) {
    var requestExample = mediaExample(requestContent[requestType]);
    requestPresent = requestExample.present;
    request = requestExample.value;
  }
  var responses = {};
  Object.keys(operation.responses || {}).sort().forEach(function (status) {
    var response = localReference(spec, operation.responses[status], '#/components/responses/');
    var content = response && response.content || {};
    var contentType = content['application/json'] ? 'application/json' : Object.keys(content).sort()[0];
    if (!contentType) return;
    var example = mediaExample(content[contentType]);
    if (example.present) {
      var responseBudget = { nodes: 0, truncated: false };
      responses[status] = {
        contentType: contentType,
        value: sanitizeExample(example.value, '', responseBudget, 0),
        truncated: responseBudget.truncated
      };
    }
  });
  var requestBudget = { nodes: 0, truncated: false };
  return {
    request: requestPresent ? sanitizeExample(request, '', requestBudget, 0) : null,
    requestPresent: requestPresent,
    requestTruncated: requestPresent && requestBudget.truncated,
    requestContentType: requestType || null,
    responses: responses
  };
}
function generatedExamples(endpoint, area, explicit) {
  var schemas = area && area.schemas || {};
  var budget = { nodes: 0, truncated: false, truncationRevision: 0 };
  var requestSanitizeBudget = { nodes: 0, truncated: false };
  var requestTruncationBefore = budget.truncationRevision;
  var request = !explicit.requestPresent && endpoint.request && endpoint.request.body
    ? sanitizeExample(
      schemaExample(
        endpoint.request.body.schemaRef, schemas, Object.create(null), 0, budget
      ),
      '', requestSanitizeBudget, 0
    ) : null;
  var requestTruncated = !explicit.requestPresent &&
    (budget.truncationRevision > requestTruncationBefore ||
      requestSanitizeBudget.truncated);
  var responses = {};
  var truncatedResponses = Object.create(null);
  Object.keys(endpoint.response || {}).sort().forEach(function (status) {
    if (explicit.responses[status]) return;
    var row = endpoint.response[status];
    if (!row || !row.schemaRef) return;
    var truncationBefore = budget.truncationRevision;
    var sanitizeBudget = { nodes: 0, truncated: false };
    var value = sanitizeExample(
      schemaExample(row.schemaRef, schemas, Object.create(null), 0, budget),
      '', sanitizeBudget, 0
    );
    responses[status] = {
      contentType: 'application/json',
      value: row.array ? [value] : value
    };
    truncatedResponses[status] =
      budget.truncationRevision > truncationBefore || sanitizeBudget.truncated;
  });
  return {
    request: request,
    requestTruncated: requestTruncated,
    responses: responses,
    truncatedResponses: truncatedResponses,
    truncated: requestTruncated || Object.keys(truncatedResponses).some(function (status) {
      return truncatedResponses[status];
    })
  };
}
function boundedExample(value) {
  if (value === undefined) return { value: null, truncated: false };
  var text = JSON.stringify(value);
  if (Buffer.byteLength(text, 'utf8') <= 64 * 1024) return {
    value: value, truncated: false
  };
  return { value: null, truncated: true };
}
function relatedSchemas(endpoint, schemas, includeObserved) {
  var out = Object.create(null);
  var queue = endpointModels(endpoint).requestIds.concat(endpointModels(endpoint).responseIds);
  var seen = Object.create(null);
  var count = 0;
  var visits = 0;
  var truncated = false;
  while (queue.length && count < 500 && visits < 10000) {
    var id = queue.shift();
    if (!id || seen[id]) continue;
    seen[id] = 1;
    visits++;
    var schema = own(schemas, id) ? schemas[id] : null;
    if (!schema) continue;
    out[id] = includeObserved ? detailSchema(schema) : publicSchema(schema);
    count++;
    if (out[id].fieldsTruncated) truncated = true;
    (Array.isArray(schema.fields) ? schema.fields : []).forEach(function (field) {
      if (typeof field.type === 'string' && field.type.indexOf('ref:') === 0) {
        queue.push(field.type.slice(4));
      }
      if (field.itemsRef) queue.push(field.itemsRef);
    });
  }
  if (queue.some(function (id) { return id && !seen[id]; })) truncated = true;
  return { schemas: out, truncated: truncated };
}
function publicSchema(schema) {
  if (!schema || !Array.isArray(schema.fields)) {
    return { fields: [], fieldsTruncated: false };
  }
  return {
    fields: schema.fields.slice(0, 1000).map(function (field) {
      var copy = Object.assign({}, field);
      // Postman observations can originate from a real environment. They are
      // useful for server-side mismatch analysis but are never API payloads.
      delete copy.example;
      delete copy.enum_observed;
      return copy;
    }),
    fieldsTruncated: schema.fields.length > 1000
  };
}
function detailSchema(schema) {
  var value = publicSchema(schema);
  value.fields = value.fields.map(function (field, index) {
    var observed = schema.fields[index] && schema.fields[index].enum_observed;
    if (!Array.isArray(observed) || !observed.length) return field;
    var copy = Object.assign({}, field);
    copy.enumObserved = observed.slice(0, 20).map(function (item) {
      return sanitizeExample(item, field.jsonName || field.name || '', { nodes: 0 }, 0);
    });
    return copy;
  });
  return value;
}
function boundDetailResponse(response) {
  if (Buffer.byteLength(JSON.stringify(response), 'utf8') <= DETAIL_RESPONSE_MAX) return response;
  response.contract.schemaIds = Object.keys(response.contract.schemas || {}).sort();
  response.contract.schemas = {};
  response.contract.schemasTruncated = true;
  response.changes = response.changes.slice(0, 100);
  response.mismatches = Array.isArray(response.mismatches)
    ? response.mismatches.slice(0, 100) : response.mismatches;
  response.limitations = response.limitations.concat(['api-detail-size-cap']);
  if (Buffer.byteLength(JSON.stringify(response), 'utf8') > DETAIL_RESPONSE_MAX) {
    response.examples = {
      request: response.examples.request ? {
        source: response.examples.request.source,
        generated: response.examples.request.generated,
        contentType: response.examples.request.contentType,
        value: null,
        truncated: true
      } : null,
      responses: {}
    };
    if (response.limitations.indexOf('api-example-size-cap') < 0) {
      response.limitations.push('api-example-size-cap');
    }
  }
  if (Buffer.byteLength(JSON.stringify(response), 'utf8') > DETAIL_RESPONSE_MAX) {
    response.consumers.items = response.consumers.items.slice(0, 100);
    response.changes = response.changes.slice(0, 50);
    response.mismatches = Array.isArray(response.mismatches)
      ? response.mismatches.slice(0, 50) : response.mismatches;
  }
  if (Buffer.byteLength(JSON.stringify(response), 'utf8') > DETAIL_RESPONSE_MAX) {
    response.contract = {
      request: null,
      responses: {},
      errors: [],
      schemas: {},
      schemaIds: response.contract.schemaIds || [],
      schemasTruncated: true,
      contractTruncated: true
    };
    response.consumers.items = [];
    response.changes = [];
    response.mismatches = null;
  }
  if (Buffer.byteLength(JSON.stringify(response), 'utf8') > DETAIL_RESPONSE_MAX) {
    response.contract.schemaIds = [];
    response.examples = { request: null, responses: {} };
    response.consumers.items = [];
    response.changes = [];
    response.mismatches = null;
    if (response.limitations.indexOf('api-detail-size-cap') < 0) {
      response.limitations.push('api-detail-size-cap');
    }
  }
  return response;
}
function detail(operationId, expectedGenerationId) {
  var snapshot = relations.snapshot();
  if (!snapshot.ok) return snapshot;
  var conflict = relations.checkExpected(snapshot, expectedGenerationId);
  if (conflict) return conflict;
  if (snapshot.empty) return Object.assign(relations.meta(snapshot), {
    ok: false, status: 404, error: 'api-endpoint-not-found'
  });
  var row = findOperation(snapshot, operationId);
  if (!row) return Object.assign(relations.meta(snapshot), {
    ok: false, status: 404, error: 'api-endpoint-not-found'
  });
  var raw = (snapshot.inventory.endpoints || []).find(function (endpoint) {
    return endpoint.operationId === operationId;
  });
  var area = readRoleJson(snapshot, 'area:' + raw.area);
  if (!area || !endpointRootsAvailable(raw, area.schemas)) {
    return Object.assign(relations.meta(snapshot), {
    ok: false, status: 409, error: 'api-area-unavailable'
  });
  }
  var explicit = explicitExamples(snapshot, raw);
  var generated = generatedExamples(raw, area, explicit);
  var requestValue = explicit.requestPresent ? explicit.request : generated.request;
  var responses = {};
  Object.keys(raw.response || {}).sort().forEach(function (status) {
    var selected = explicit.responses[status] || generated.responses[status] || null;
    if (!selected) return;
    var bounded = boundedExample(selected.value);
    responses[status] = {
      source: explicit.responses[status] ? 'explicit' : 'generated',
      generated: !explicit.responses[status],
      contentType: selected.contentType,
      value: bounded.value,
      truncated: bounded.truncated || selected.truncated === true ||
        !!generated.truncatedResponses[status]
    };
  });
  var requestBounded = boundedExample(requestValue);
  var hasRequestExample = explicit.requestPresent || generated.request !== null;
  var related = relatedSchemas(raw, area.schemas || {}, true);
  var response = Object.assign(relations.meta(snapshot), {
    ok: true,
    status: 200,
    endpoint: publicEndpoint(row),
    contract: {
      request: raw.request,
      responses: raw.response,
      errors: raw.errors || [],
      schemas: related.schemas,
      schemasTruncated: related.truncated
    },
    examples: {
      request: !hasRequestExample ? null : {
        source: explicit.requestPresent ? 'explicit' : 'generated',
        generated: !explicit.requestPresent,
        contentType: explicit.requestContentType ||
          raw.request && raw.request.body && raw.request.body.contentType || 'application/json',
        value: requestBounded.value,
        truncated: requestBounded.truncated || explicit.requestTruncated ||
          generated.requestTruncated
      },
      responses: responses
    },
    implementation: row.implementation,
    consumers: {
      analysisStatus: row.consumers.analysisStatus,
      items: row._consumerRows.slice(0, 500).map(function (consumer) {
        return {
          id: consumer.id,
          architectureId: relations.safeString(consumer.architectureId, 300),
          file: relations.safePath(consumer.file),
          symbol: relations.safeString(consumer.symbol, 300),
          kind: consumer.kind
        };
      })
    },
    changes: row._changes.slice(0, 500),
    mismatches: Array.isArray(row._mismatches) ? row._mismatches.slice(0, 500) : null,
    tasks: row.tasks
  });
  if (generated.truncated) {
    response.limitations = response.limitations.concat(['api-example-generation-cap']);
  }
  if (explicit.requestTruncated || Object.keys(explicit.responses).some(function (status) {
    return explicit.responses[status].truncated === true;
  })) {
    response.limitations = response.limitations.concat(['api-example-size-cap']);
  }
  if (related.truncated) {
    response.limitations = response.limitations.concat(['api-schema-relation-cap']);
  }
  return boundDetailResponse(response);
}
function modelDetail(modelId, expectedGenerationId) {
  var snapshot = relations.snapshot();
  if (!snapshot.ok) return snapshot;
  var conflict = relations.checkExpected(snapshot, expectedGenerationId);
  if (conflict) return conflict;
  if (snapshot.empty || typeof modelId !== 'string' || !modelId || modelId.length > 200 ||
      /[\x00-\x1f\x7f]/.test(modelId)) {
    return Object.assign(relations.meta(snapshot), {
      ok: false, status: 404, error: 'api-model-not-found'
    });
  }
  var areaCatalog = allAreas(snapshot), matches = [];
  if (!areaCatalog.ok) return Object.assign(relations.meta(snapshot), {
    ok: false, status: 409, error: areaCatalog.error
  });
  var areas = areaCatalog.value;
  Object.keys(areas).sort().forEach(function (area) {
    if (areas[area].schemas &&
        Object.prototype.hasOwnProperty.call(areas[area].schemas, modelId)) {
      matches.push({ area: area, schema: areas[area].schemas[modelId] });
    }
  });
  if (!matches.length) return Object.assign(relations.meta(snapshot), {
    ok: false, status: 404, error: 'api-model-not-found'
  });
  var catalog = normalized(snapshot);
  var usageTruncated = false;
  var usageBudget = { visits: 0 };
  var usage = (snapshot.inventory.endpoints || []).map(function (endpoint) {
    var area = areas[endpoint.area];
    var schemas = area && area.schemas || {};
    var models = endpointModels(endpoint);
    var request = modelReachable(models.requestIds, schemas, modelId, usageBudget);
    var response = modelReachable(models.responseIds, schemas, modelId, usageBudget);
    if (request.truncated || response.truncated) usageTruncated = true;
    if (!request.found && !response.found) return null;
    return {
      operationId: endpoint.operationId,
      method: endpoint.method,
      path: endpoint.path,
      request: request.found,
      response: response.found
    };
  }).filter(Boolean);
  var latestChanges = catalog.changes.filter(function (row) {
    return row.modelId === modelId;
  });
  var selectedSchema = publicSchema(matches[0].schema);
  var response = Object.assign(relations.meta(snapshot), {
    ok: true, status: 200,
    model: {
      id: modelId,
      areas: matches.map(function (row) { return row.area; }),
      schema: selectedSchema,
      schemaTruncated: selectedSchema.fieldsTruncated,
      duplicateDefinitions: matches.length > 1,
      usage: usage,
      latestChanges: latestChanges
    }
  });
  if (usageTruncated) response.limitations = response.limitations.concat([
    'api-model-usage-cap'
  ]);
  if (selectedSchema.fieldsTruncated) {
    response.limitations = response.limitations.concat(['api-schema-field-cap']);
  }
  if (Buffer.byteLength(JSON.stringify(response), 'utf8') > DETAIL_RESPONSE_MAX) {
    response.model.schema = null;
    response.model.schemaTruncated = true;
    response.model.latestChanges = response.model.latestChanges.slice(0, 100);
    response.model.usage = response.model.usage.slice(0, 500);
    response.limitations = response.limitations.concat(['api-model-size-cap']);
  }
  if (Buffer.byteLength(JSON.stringify(response), 'utf8') > DETAIL_RESPONSE_MAX) {
    response.model.usage = [];
    response.model.latestChanges = [];
  }
  if (Buffer.byteLength(JSON.stringify(response), 'utf8') > DETAIL_RESPONSE_MAX) {
    response.model.areas = response.model.areas.slice(0, 500);
    response.limitations = response.limitations.concat(['api-model-size-cap']);
  }
  return response;
}
function diagnostics(expectedGenerationId, sourceId) {
  var snapshot = relations.snapshot();
  if (!snapshot.ok) return snapshot;
  var conflict = relations.checkExpected(snapshot, expectedGenerationId);
  if (conflict) return conflict;
  if (sourceId !== undefined && sourceId !== null && sourceId !== '' &&
      !/^api:mismatch:mismatch-[a-f0-9]{24}$/.test(String(sourceId))) {
    return { ok: false, status: 400, error: 'bad-api-query' };
  }
  var report = function (state, current, value) {
    return {
      state: state,
      current: current,
      generatedAt: value && relations.safeString(value.generatedAt || value.checkedAt, 40),
      analyzerVersion: value && relations.safeString(value.analyzerVersion, 100),
      analysisStatus: current && value &&
        ['complete', 'partial'].indexOf(value.analysisStatus) >= 0
        ? value.analysisStatus : null,
      limitations: current && value && Array.isArray(value.limitations)
        ? value.limitations.slice(0, 50).map(function (row) {
          return relations.safeString(row, 100);
        }).filter(Boolean) : []
    };
  };
  var mismatches = snapshot.empty ? [] : normalized(snapshot).mismatches;
  var meta = relations.meta(snapshot);
  var selectedMismatches = sourceId
    ? mismatches.filter(function (row) { return row.sourceId === sourceId; })
    : mismatches;
  if (sourceId && !selectedMismatches.length) {
    return Object.assign(meta, {
      ok: false, status: 404, error: 'api-mismatch-not-found'
    });
  }
  var mismatchItems = selectedMismatches.slice(0, 100).map(function (row) {
    return {
      id: row.id,
      sourceId: row.sourceId,
      kind: row.kind,
      severity: row.severity,
      operationId: row.operationId,
      modelId: row.modelId,
      file: row.file,
      task: row.tasks.open[0] || null
    };
  });
  if (selectedMismatches.length > mismatchItems.length) {
    meta.limitations = meta.limitations.concat(['api-diagnostics-mismatch-cap']);
  }
  return Object.assign(meta, {
    ok: true, status: 200, empty: !!snapshot.empty,
    summary: snapshot.empty ? 'contract-missing' :
      snapshot.environmentMismatch ? 'environment-mismatch' :
        meta.limitations.length ? 'partial' : 'ready',
    activeEnvironment: snapshot.activeEnvironment,
    snapshotEnvironmentId: snapshot.environmentId,
    environmentMismatch: !!snapshot.environmentMismatch,
    reports: snapshot.empty ? {} : {
      implementation: report(
        snapshot.implementation ? 'present' :
          snapshot.limitations.indexOf('implementation-report-invalid') >= 0 ? 'invalid' : 'missing-or-stale',
        !!snapshot.implementation, snapshot.implementation
      ),
      consumers: report(
        snapshot.consumers ? 'present' :
          snapshot.limitations.indexOf('consumer-report-invalid') >= 0 ? 'invalid' : 'missing-or-stale',
        !!snapshot.consumers, snapshot.consumers
      ),
      drift: report(snapshot.drift.state, snapshot.drift.current, snapshot.drift.value),
      changes: report(snapshot.changes.state, snapshot.changes.current, snapshot.changes.value)
    },
    commands: [
      'cd orchestrator/api-contract && npm run contract:doctor',
      'cd orchestrator/api-contract && npm run contract:analyze',
      'cd orchestrator/api-contract && npm run contract:diff'
    ],
    reportPaths: [
      'orchestrator/.cache/api-contract/reports/implementation-map.json',
      'orchestrator/.cache/api-contract/reports/consumer-map.json',
      'orchestrator/.cache/api-contract/reports/drift.json'
    ],
    observedMismatches: {
      items: mismatchItems,
      total: selectedMismatches.length,
      truncated: selectedMismatches.length > mismatchItems.length
    }
  });
}

module.exports = {
  DEFAULT_LIMIT: DEFAULT_LIMIT,
  MAX_LIMIT: MAX_LIMIT,
  RESPONSE_MAX: RESPONSE_MAX,
  list: list,
  detail: detail,
  modelDetail: modelDetail,
  diagnostics: diagnostics,
  taskSourceFacts: taskSourceFacts,
  snapshot: relations.snapshot,
  normalized: normalized,
  boundedListResponse: boundedListResponse,
  encodeCursor: encodeCursor,
  decodeCursor: decodeCursor,
  _test: {
    boundedListResponse: boundedListResponse,
    endpointFilters: endpointFilters,
    generatedExamples: generatedExamples,
    modelReachable: modelReachable,
    boundDetailResponse: boundDetailResponse,
    publicSchema: publicSchema,
    sanitizeExample: sanitizeExample,
    secretKey: secretKey,
    mediaExample: mediaExample,
    endpointRootsAvailable: endpointRootsAvailable,
    operationRowIndex: operationRowIndex,
    modelRowIndex: modelRowIndex,
    taskSourceFactsFromCatalog: taskSourceFactsFromCatalog
  }
};
