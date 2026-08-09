'use strict';

// Generation-bound optional report and relation projection shared by the API
// overview/catalog/changes/task modules. Required inventory corruption remains
// blocking in contract-generation; every optional report degrades independently.

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var generation = require('./contract-generation');
var backendIntegration = require('./backend-integration');
var taskSource = require('./task-source');
var arch = require('./arch');
var fileGuards = require('./file-guards');
var projectInputs = require('./api-project-inputs');

var REPORTS_DIR = path.join(paths.API_CONTRACT_CACHE_DIR, 'reports');
var REPORT_MAX = 16 * 1024 * 1024;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var GENERATION_RE = /^gen-[0-9]{8}T[0-9]{6}Z-[a-f0-9]{12}$/;
var inputCache = { at: 0, value: null };

function sha(value) {
  return 'sha256:' + crypto.createHash('sha256')
    .update(Buffer.isBuffer(value) ? value : Buffer.from(String(value), 'utf8')).digest('hex');
}
function safeString(value, max) {
  if (typeof value !== 'string') return null;
  var clean = value.normalize('NFC')
    .replace(/[\x00-\x1f\x7f-\x9f\u2028\u2029]+/g, ' ')
    .replace(/\s+/g, ' ').trim();
  return clean ? Array.from(clean).slice(0, max || 500).join('') : null;
}
function safePath(value) {
  var clean = safeString(value, 500);
  if (!clean || clean.indexOf('\\') >= 0 || path.posix.isAbsolute(clean) ||
      /^[A-Za-z]:/.test(clean) || clean.split('/').some(function (part) {
        return !part || part === '.' || part === '..';
      })) return null;
  return clean;
}
function unique(values) {
  return values.filter(function (value, index, list) {
    return value && list.indexOf(value) === index;
  });
}
function reportLimitations(value) {
  return value && Array.isArray(value.limitations)
    ? value.limitations.filter(function (row) {
      return typeof row === 'string' && row.length > 0 && row.length <= 100;
    }) : [];
}
function plain(value) {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}
function exact(value, keys) {
  return plain(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function closed(value, required, optional) {
  if (!plain(value)) return false;
  var allowed = required.concat(optional || []);
  return required.every(function (key) {
    return Object.prototype.hasOwnProperty.call(value, key);
  }) && Object.keys(value).every(function (key) {
    return allowed.indexOf(key) >= 0;
  });
}
function boundedLine(value, maximum) {
  return typeof value === 'string' && value.length > 0 &&
    value.length <= maximum && safeString(value, maximum) === value;
}
function boundedLines(value, maximumItems, maximumLength) {
  return Array.isArray(value) && value.length <= maximumItems &&
    value.every(function (row) { return boundedLine(row, maximumLength); });
}
function validParameter(parameter) {
  return exact(parameter, ['name', 'required', 'type']) &&
    typeof parameter.name === 'string' &&
    parameter.name.length > 0 && parameter.name.length <= 200 &&
    typeof parameter.type === 'string' && parameter.type.length > 0 &&
    parameter.type.length <= 200 &&
    (parameter.required === true || parameter.required === false);
}
function uniqueParameterNames(parameters) {
  var seen = Object.create(null);
  return parameters.every(function (parameter) {
    if (seen[parameter.name]) return false;
    seen[parameter.name] = 1;
    return true;
  });
}
function boundedNullable(value, maximum) {
  return value === null ||
    typeof value === 'string' && value.length <= maximum &&
      !/[\x00-\x1f\x7f]/.test(value);
}
function nullableString(value, maximum) {
  return value === null ||
    typeof value === 'string' && value.length <= maximum;
}
function validInventory(value) {
  if (!exact(value, ['areas', 'endpoints', 'schemaVersion', 'source', 'stats']) ||
      value.schemaVersion !== 1 || !plain(value.source) ||
      !exact(value.stats, ['areas', 'endpoints', 'schemas']) ||
      !plain(value.areas) || !Array.isArray(value.endpoints) ||
      value.endpoints.length > 10000 ||
      !exact(value.source, [
        'fetchedAt', 'kind', 'openApiUrl', 'openApiVersion',
        'postmanImportedAt', 'specHash', 'title'
      ]) ||
      ['openapi', 'postman', 'merged'].indexOf(value.source.kind) < 0 ||
      !boundedNullable(value.source.openApiUrl, 2000) ||
      !boundedNullable(value.source.openApiVersion, 100) ||
      !boundedNullable(value.source.title, 1000) ||
      !boundedNullable(value.source.fetchedAt, 50) ||
      !boundedNullable(value.source.postmanImportedAt, 50) ||
      !(value.source.specHash === null ||
        HASH_RE.test(String(value.source.specHash || ''))) ||
      Object.keys(value.areas).length > 10000 ||
      !Number.isSafeInteger(value.stats.endpoints) ||
      !Number.isSafeInteger(value.stats.areas) ||
      !Number.isSafeInteger(value.stats.schemas) || value.stats.schemas < 0 ||
      value.stats.endpoints < 0 || value.stats.endpoints > 10000 ||
      value.stats.areas < 0 || value.stats.areas > 10000 ||
      value.stats.schemas > 100000 ||
      value.stats.endpoints !== value.endpoints.length ||
      value.stats.areas !== Object.keys(value.areas).length) return false;
  var operationIds = Object.create(null);
  var routes = Object.create(null);
  for (var index = 0; index < value.endpoints.length; index++) {
    var endpoint = value.endpoints[index];
    if (!exact(endpoint, [
      'area', 'auth', 'deprecated', 'errors', 'examples', 'method',
      'operationId', 'path', 'request', 'response', 'summary'
    ]) || typeof endpoint.operationId !== 'string' ||
        !endpoint.operationId || endpoint.operationId.length > 200 ||
        /[\x00-\x1f\x7f]/.test(endpoint.operationId) ||
        operationIds[endpoint.operationId] ||
        ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'].indexOf(endpoint.method) < 0 ||
        typeof endpoint.path !== 'string' || endpoint.path.charAt(0) !== '/' ||
        endpoint.path.length > 2000 || /[\x00-\x1f\x7f]/.test(endpoint.path) ||
        typeof endpoint.area !== 'string' ||
        !/^[a-z0-9][a-z0-9-]{0,99}$/.test(endpoint.area) ||
        (endpoint.summary !== null &&
          (typeof endpoint.summary !== 'string' || endpoint.summary.length > 1000)) ||
        (endpoint.auth !== null &&
          (typeof endpoint.auth !== 'string' || endpoint.auth.length > 100)) ||
        (endpoint.deprecated !== true && endpoint.deprecated !== false) ||
        !exact(endpoint.request, ['body', 'pathParams', 'query']) ||
        !Array.isArray(endpoint.request.pathParams) ||
        !Array.isArray(endpoint.request.query) ||
        endpoint.request.pathParams.length > 500 || endpoint.request.query.length > 500 ||
        endpoint.request.pathParams.some(function (row) { return !validParameter(row); }) ||
        endpoint.request.query.some(function (row) { return !validParameter(row); }) ||
        !uniqueParameterNames(endpoint.request.pathParams) ||
        !uniqueParameterNames(endpoint.request.query) ||
        (endpoint.request.body !== null &&
            (!exact(endpoint.request.body, ['contentType', 'schemaRef']) ||
            typeof endpoint.request.body.schemaRef !== 'string' ||
            !endpoint.request.body.schemaRef ||
            endpoint.request.body.schemaRef.length > 200 ||
            typeof endpoint.request.body.contentType !== 'string' ||
            !endpoint.request.body.contentType ||
            endpoint.request.body.contentType.length > 200)) ||
        !plain(endpoint.response) || Object.keys(endpoint.response).length > 100 ||
        Object.keys(endpoint.response).some(function (status) {
          var row = endpoint.response[status];
          return !/^(?:[0-9]{3}|default)$/.test(status) ||
            !exact(row, ['array', 'schemaRef']) ||
            (row.schemaRef !== null && typeof row.schemaRef !== 'string') ||
            (typeof row.schemaRef === 'string' && row.schemaRef.length > 200) ||
            (row.array !== true && row.array !== false);
        }) ||
        !Array.isArray(endpoint.errors) || endpoint.errors.length > 100 ||
        endpoint.errors.some(function (status, position) {
          return !/^[0-9]{3}$/.test(status) ||
            endpoint.errors.indexOf(status) !== position ||
            Object.prototype.hasOwnProperty.call(endpoint.response, status);
        }) ||
        !exact(endpoint.examples, ['request', 'response']) ||
        (endpoint.examples.request !== true && endpoint.examples.request !== false) ||
        (endpoint.examples.response !== true && endpoint.examples.response !== false)) return false;
    var route = endpoint.method + ' ' + endpoint.path;
    if (routes[route]) return false;
    operationIds[endpoint.operationId] = endpoint.area;
    routes[route] = 1;
  }
  var projected = Object.create(null);
  for (var area of Object.keys(value.areas)) {
    var ids = value.areas[area];
    if (!/^[a-z0-9][a-z0-9-]{0,99}$/.test(area) || !Array.isArray(ids) ||
        ids.length > 10000 || ids.some(function (id) {
          if (typeof id !== 'string' || operationIds[id] !== area || projected[id]) return true;
          projected[id] = 1;
          return false;
        })) return false;
  }
  return Object.keys(operationIds).every(function (id) { return projected[id] === 1; });
}
function validAreaField(field) {
  if (!closed(field, ['name', 'jsonName', 'required', 'type'], [
    'constraints', 'enum', 'enum_observed', 'enum_observed_truncated',
    'example', 'format', 'itemsRef', 'nullable_declared', 'nullable_observed'
  ]) ||
      typeof field.name !== 'string' || !field.name || field.name.length > 200 ||
      typeof field.jsonName !== 'string' || !field.jsonName ||
      field.jsonName.length > 200 ||
      !/^(?:string|integer|number|boolean|object|array|ref:[A-Za-z][A-Za-z0-9_.-]{0,199})$/
        .test(String(field.type || '')) ||
      (field.required !== true && field.required !== false) ||
      (Object.prototype.hasOwnProperty.call(field, 'itemsRef') &&
        field.itemsRef !== null &&
        !/^[A-Za-z][A-Za-z0-9_.-]{0,199}$/.test(String(field.itemsRef || ''))) ||
      (Object.prototype.hasOwnProperty.call(field, 'format') &&
        !nullableString(field.format, 100)) ||
      (Object.prototype.hasOwnProperty.call(field, 'nullable_declared') &&
        field.nullable_declared !== null &&
        field.nullable_declared !== true && field.nullable_declared !== false) ||
      (Object.prototype.hasOwnProperty.call(field, 'nullable_observed') &&
        field.nullable_observed !== null &&
        field.nullable_observed !== true && field.nullable_observed !== false) ||
      (Object.prototype.hasOwnProperty.call(field, 'enum') &&
        field.enum !== null &&
        (!Array.isArray(field.enum) || field.enum.length > 10000)) ||
      (Object.prototype.hasOwnProperty.call(field, 'enum_observed') &&
        field.enum_observed !== null &&
        (!Array.isArray(field.enum_observed) || field.enum_observed.length > 10)) ||
      (Object.prototype.hasOwnProperty.call(field, 'enum_observed_truncated') &&
        field.enum_observed_truncated !== true)) return false;
  if (!Object.prototype.hasOwnProperty.call(field, 'constraints')) return true;
  var constraints = field.constraints;
  var numeric = ['exclusiveMaximum', 'exclusiveMinimum', 'maximum', 'minimum'];
  var nonNegative = [
    'maxItems', 'maxLength', 'maxProperties',
    'minItems', 'minLength', 'minProperties'
  ];
  if (!exact(constraints, numeric.concat(nonNegative, ['pattern', 'patternHash'])) ||
      numeric.some(function (key) {
        return constraints[key] !== null &&
          (typeof constraints[key] !== 'number' || !Number.isFinite(constraints[key]));
      }) ||
      nonNegative.some(function (key) {
        return constraints[key] !== null &&
          (!Number.isSafeInteger(constraints[key]) || constraints[key] < 0);
      }) ||
      !nullableString(constraints.pattern, 2000) ||
      !(constraints.patternHash === null ||
        HASH_RE.test(String(constraints.patternHash || '')))) return false;
  return true;
}
function validArea(value, expectedArea) {
  if (!exact(value, ['area', 'schemaVersion', 'schemas']) ||
      value.schemaVersion !== 1 ||
      !/^[a-z0-9][a-z0-9-]{0,99}$/.test(String(value.area || '')) ||
      expectedArea && value.area !== expectedArea ||
      !plain(value.schemas) || Object.keys(value.schemas).length > 10000) return false;
  var schemaNames = Object.keys(value.schemas);
  var valid = schemaNames.every(function (name) {
    var schema = value.schemas[name];
    if (!/^[A-Za-z][A-Za-z0-9_.-]{0,199}$/.test(name) ||
        !exact(schema, ['fields']) || !Array.isArray(schema.fields) ||
        schema.fields.length > 10000) return false;
    var jsonNames = Object.create(null);
    return schema.fields.every(function (field) {
      if (!validAreaField(field) || jsonNames[field.jsonName]) return false;
      jsonNames[field.jsonName] = 1;
      return true;
    });
  });
  if (!valid) return false;
  return schemaNames.every(function (name) {
    return value.schemas[name].fields.every(function (field) {
      var ref = typeof field.type === 'string' && field.type.indexOf('ref:') === 0
        ? field.type.slice(4) : null;
      return (!ref || Object.prototype.hasOwnProperty.call(value.schemas, ref)) &&
        (!field.itemsRef ||
          Object.prototype.hasOwnProperty.call(value.schemas, field.itemsRef));
    });
  });
}
function readOptional(name) {
  var file = path.join(REPORTS_DIR, name);
  try {
    var hit = fileGuards.boundedRegularFileUnder(
      paths.PROJECT_ROOT, REPORTS_DIR, file, REPORT_MAX
    );
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') {
      return { state: 'missing', value: null, hash: null };
    }
    var value = JSON.parse(hit.bytes.toString('utf8'));
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
      return { state: 'invalid', value: null, hash: null };
    }
    return { state: 'present', value: value, hash: sha(hit.bytes) };
  } catch (error) {
    return error && error.code === 'ENOENT'
      ? { state: 'missing', value: null, hash: null }
      : { state: 'invalid', value: null, hash: null };
  }
}
function currentInputs(force) {
  if (!force && inputCache.value && Date.now() - inputCache.at < 1000) return inputCache.value;
  inputCache = {
    at: Date.now(),
    value: projectInputs.collect(paths.PROJECT_ROOT, { includeText: false })
  };
  return inputCache.value;
}
function activeEnvironment(integration) {
  integration = integration || backendIntegration.get();
  var active = (integration.environments || []).find(function (row) {
    return row.id === integration.activeEnvironmentId;
  });
  return active ? { id: active.id, label: active.label, sourceKind: active.sourceKind } : null;
}
function environmentMismatch(active, snapshotEnvironmentId) {
  return !active || active.id !== snapshotEnvironmentId;
}
function reportMetaMatches(report, current, inputs) {
  return !!report && report.schemaVersion === 1 &&
    report.analyzerVersion === projectInputs.ANALYZER_VERSION &&
    report.committedGenerationId === current.manifest.generationId &&
    report.contractHash === current.snapshotHash &&
    report.environmentId === current.environmentId &&
    inputs.ok && report.projectCodeRevision === inputs.projectCodeRevision;
}
function inventoryOperationSet(inventory) {
  var ids = Object.create(null);
  (inventory && Array.isArray(inventory.endpoints) ? inventory.endpoints : [])
    .forEach(function (endpoint) { ids[endpoint.operationId] = 1; });
  return ids;
}
function exactOperationCoverage(rows, inventory, validate) {
  if (!Array.isArray(rows) || rows.length !== inventory.endpoints.length ||
      rows.length > 10000) return false;
  var expected = inventoryOperationSet(inventory);
  return rows.every(function (row) {
    if (!validate(row) || !expected[row.operationId]) return false;
    delete expected[row.operationId];
    return true;
  }) && Object.keys(expected).length === 0;
}
function validImplementation(report, inventory) {
  if (!exact(report, [
    'analysisStatus', 'analyzerVersion', 'committedGenerationId', 'contractHash',
    'coverage', 'environmentId', 'generatedAt', 'limitations', 'operations',
    'projectCodeRevision', 'receipt', 'schemaVersion', 'unresolved'
  ]) || report.schemaVersion !== 1 ||
      ['complete', 'partial'].indexOf(report.analysisStatus) < 0 ||
      !Number.isFinite(Date.parse(report.generatedAt)) ||
      !boundedLines(report.limitations, 50, 100) ||
      !exact(report.coverage, [
        'analyzedFiles', 'implemented', 'missing', 'partial', 'total', 'unknown'
      ]) ||
      !['analyzedFiles', 'implemented', 'missing', 'partial', 'total', 'unknown']
        .every(function (key) {
          return Number.isSafeInteger(report.coverage[key]) &&
            report.coverage[key] >= 0 &&
            report.coverage[key] <= (key === 'analyzedFiles' ? 20000 : 10000);
        }) ||
      report.coverage.total !== inventory.endpoints.length ||
      report.coverage.implemented + report.coverage.missing +
        report.coverage.partial + report.coverage.unknown !== report.coverage.total ||
      !exact(report.receipt, [
        'directoryCount', 'fileCount', 'files', 'totalBytes'
      ]) ||
      !Number.isSafeInteger(report.receipt.fileCount) ||
      report.receipt.fileCount < 0 || report.receipt.fileCount > 20000 ||
      !Number.isSafeInteger(report.receipt.directoryCount) ||
      report.receipt.directoryCount < 1 || report.receipt.directoryCount > 20000 ||
      !Number.isSafeInteger(report.receipt.totalBytes) ||
      report.receipt.totalBytes < 0 || report.receipt.totalBytes > 64 * 1024 * 1024 ||
      !Array.isArray(report.receipt.files) ||
      report.receipt.files.length !== report.receipt.fileCount) return false;
  var pathsSeen = Object.create(null), receiptBytes = 0;
  if (!report.receipt.files.every(function (row) {
    if (!exact(row, ['hash', 'path', 'size']) || safePath(row.path) !== row.path ||
        pathsSeen[row.path] || !Number.isSafeInteger(row.size) ||
        row.size < 0 || row.size > 2 * 1024 * 1024 ||
        !HASH_RE.test(String(row.hash || ''))) return false;
    pathsSeen[row.path] = 1;
    receiptBytes += row.size;
    return receiptBytes <= 64 * 1024 * 1024;
  }) || receiptBytes !== report.receipt.totalBytes) return false;
  var states = { implemented: 0, missing: 0, partial: 0, unknown: 0 };
  if (!exactOperationCoverage(report.operations, inventory, function (row) {
    if (!exact(row, [
      'confidence', 'evidence', 'file', 'operationId', 'state', 'symbol'
    ]) || !boundedLine(row.operationId, 200) ||
        !Object.prototype.hasOwnProperty.call(states, row.state) ||
        (row.file !== null && safePath(row.file) !== row.file) ||
        (row.symbol !== null && !boundedLine(row.symbol, 300)) ||
        (row.confidence !== null &&
          ['exact', 'derived', 'heuristic'].indexOf(row.confidence) < 0) ||
        !boundedLines(row.evidence, 10, 300) ||
        row.state === 'implemented' &&
          (row.file === null || ['exact', 'derived'].indexOf(row.confidence) < 0) ||
        (row.state === 'missing' || row.state === 'unknown') &&
          (row.file !== null || row.symbol !== null || row.confidence !== null)) return false;
    states[row.state]++;
    return true;
  })) return false;
  if (Object.keys(states).some(function (state) {
    return states[state] !== report.coverage[state];
  })) return false;
  var operationsById = Object.create(null);
  report.operations.forEach(function (row) { operationsById[row.operationId] = row; });
  if (!Array.isArray(report.unresolved) || report.unresolved.length > 10000) return false;
  var unresolved = Object.create(null);
  if (!report.unresolved.every(function (row) {
    if (!exact(row, ['candidates', 'operationId', 'reason']) ||
        !boundedLine(row.operationId, 200) || unresolved[row.operationId] ||
        states.partial === 0 ||
        ['ambiguous-mapping', 'heuristic-only'].indexOf(row.reason) < 0 ||
        !Array.isArray(row.candidates) || !row.candidates.length ||
        row.candidates.length > 20 ||
        !row.candidates.every(function (candidate) {
          return exact(candidate, ['confidence', 'file', 'symbol']) &&
            !!candidate.file && safePath(candidate.file) === candidate.file &&
            (candidate.symbol === null || boundedLine(candidate.symbol, 300)) &&
            ['exact', 'derived', 'heuristic'].indexOf(candidate.confidence) >= 0;
        })) return false;
    var operation = operationsById[row.operationId];
    if (!operation || operation.state !== 'partial') return false;
    unresolved[row.operationId] = 1;
    return true;
  })) return false;
  return report.operations.every(function (row) {
    return row.state !== 'partial' || unresolved[row.operationId];
  });
}
function validConsumers(report, inventory) {
  if (!exact(report, [
    'analysisStatus', 'analyzerVersion', 'architectureStructuralHash',
    'committedGenerationId', 'contractHash', 'environmentId', 'generatedAt',
    'limitations', 'operations', 'projectCodeRevision', 'schemaVersion'
  ]) || report.schemaVersion !== 1 ||
      ['complete', 'partial'].indexOf(report.analysisStatus) < 0 ||
      !Number.isFinite(Date.parse(report.generatedAt)) ||
      !(report.architectureStructuralHash === null ||
        HASH_RE.test(String(report.architectureStructuralHash || ''))) ||
      !boundedLines(report.limitations, 50, 100)) {
    return false;
  }
  var consumerIds = Object.create(null);
  var validCoverage = exactOperationCoverage(report.operations, inventory, function (row) {
    return exact(row, ['analysisStatus', 'consumers', 'operationId']) &&
      boundedLine(row.operationId, 200) &&
      ['complete', 'partial', 'not-checked'].indexOf(row.analysisStatus) >= 0 &&
      Array.isArray(row.consumers) && row.consumers.length <= 500 &&
      (row.analysisStatus !== 'not-checked' || row.consumers.length === 0) &&
      row.consumers.every(function (consumer) {
        if (!exact(consumer, [
          'architectureId', 'file', 'id', 'kind', 'symbol'
        ]) || !/^consumer-[a-f0-9]{24}$/.test(String(consumer.id || '')) ||
            consumerIds[consumer.id] || safePath(consumer.file) !== consumer.file ||
            (consumer.architectureId !== null &&
              !boundedLine(consumer.architectureId, 300)) ||
            (consumer.symbol !== null && !boundedLine(consumer.symbol, 300)) ||
            ['architecture-node', 'source-reference'].indexOf(consumer.kind) < 0) {
          return false;
        }
        consumerIds[consumer.id] = 1;
        return true;
      });
  });
  return validCoverage && (report.analysisStatus !== 'complete' ||
    report.operations.every(function (row) { return row.analysisStatus === 'complete'; }));
}
function operationMap(rows) {
  var out = Object.create(null);
  (Array.isArray(rows) ? rows : []).forEach(function (row) {
    if (row && typeof row.operationId === 'string' && !out[row.operationId]) {
      out[row.operationId] = row;
    }
  });
  return out;
}
function sourceId(kind, id) {
  if (kind === 'missing') {
    return 'api:missing:missing-' + sha(String(id)).slice(7, 31);
  }
  var prefixes = {
    change: 'api:change:',
    mismatch: 'api:mismatch:'
  };
  return (prefixes[kind] || 'api:item:') + String(id);
}
function publicTask(row) {
  if (!row) return null;
  return {
    stem: safeString(row.stem, 120),
    title: safeString(row.title, 500),
    column: safeString(row.column, 40)
  };
}
function taskProjection(tasks, refs) {
  var open = [], resolved = [];
  refs = unique(refs);
  refs.forEach(function (ref) {
    (tasks.byRef[ref] || []).forEach(function (row) { open.push(publicTask(row)); });
    (tasks.allByRef[ref] || []).forEach(function (row) {
      if (row.column === 'done') resolved.push(publicTask(row));
    });
  });
  function dedup(rows) {
    var seen = Object.create(null);
    return rows.filter(function (row) {
      if (!row || !row.stem || seen[row.stem]) return false;
      seen[row.stem] = 1;
      return true;
    }).slice(0, 100);
  }
  return { open: dedup(open), resolved: dedup(resolved) };
}
function mismatchId(finding, occurrence) {
  var stable = {
    kind: finding.kind || null,
    operationId: finding.operationId || null,
    area: finding.area || null,
    schemaRef: finding.schemaRef || null,
    field: finding.field || null,
    dtoFile: finding.dtoFile || null,
    occurrence: Number.isSafeInteger(occurrence) && occurrence >= 0 ? occurrence : 0
  };
  return 'mismatch-' + sha(JSON.stringify(stable)).slice(7, 31);
}
function validDrift(report) {
  if (!exact(report, [
    'analyzerVersion', 'checkedAt', 'committedGenerationId', 'contractHash',
    'environmentId', 'findings', 'limitations', 'projectCodeRevision',
    'schemaVersion', 'specHash', 'summary'
  ]) || report.schemaVersion !== 1 ||
      !Number.isFinite(Date.parse(report.checkedAt)) ||
      !GENERATION_RE.test(String(report.committedGenerationId || '')) ||
      !HASH_RE.test(String(report.contractHash || '')) ||
      !HASH_RE.test(String(report.projectCodeRevision || '')) ||
      (report.specHash !== null && !HASH_RE.test(String(report.specHash || ''))) ||
      !boundedLine(report.environmentId, 100) ||
      !boundedLine(report.analyzerVersion, 100) ||
      !boundedLines(report.limitations, 50, 100) ||
      !exact(report.summary, ['errors', 'infos', 'warnings']) ||
      !Array.isArray(report.findings) || report.findings.length > 10000) return false;
  var counts = { errors: 0, warnings: 0, infos: 0 };
  if (!report.findings.every(function (finding) {
    if (!exact(finding, [
      'area', 'dtoFile', 'field', 'kind', 'message', 'operationId',
      'schemaRef', 'severity', 'suggestion'
    ]) || ['ERROR', 'WARNING', 'INFO'].indexOf(finding.severity) < 0 ||
        !boundedLine(finding.kind, 100) || !boundedLine(finding.message, 1000) ||
        (finding.area !== null && !boundedLine(finding.area, 100)) ||
        (finding.schemaRef !== null && !boundedLine(finding.schemaRef, 200)) ||
        (finding.operationId !== null && !boundedLine(finding.operationId, 200)) ||
        (finding.field !== null && !boundedLine(finding.field, 200)) ||
        (finding.dtoFile !== null && safePath(finding.dtoFile) !== finding.dtoFile) ||
        (finding.suggestion !== null && !boundedLine(finding.suggestion, 1000))) return false;
    counts[finding.severity === 'ERROR'
      ? 'errors' : finding.severity === 'WARNING' ? 'warnings' : 'infos']++;
    return true;
  })) return false;
  return Object.keys(counts).every(function (key) {
    return Number.isSafeInteger(report.summary[key]) &&
      report.summary[key] === counts[key];
  });
}
function driftState(current, inputs) {
  var raw = readOptional('drift.json');
  if (raw.state !== 'present') return {
    state: raw.state, current: false, value: null,
    limitation: raw.state === 'invalid' ? 'drift-report-invalid' : 'drift-report-missing'
  };
  var value = raw.value;
  var currentMatch = validDrift(value) &&
    value.committedGenerationId === current.manifest.generationId &&
    value.contractHash === current.snapshotHash &&
    value.environmentId === current.environmentId &&
    value.specHash === ((current.inventory.source && current.inventory.source.specHash) || null) &&
    inputs.ok && value.projectCodeRevision === inputs.projectCodeRevision &&
    value.analyzerVersion === projectInputs.ANALYZER_VERSION;
  return {
    state: 'present',
    current: currentMatch,
    value: value,
    hash: raw.hash,
    limitation: currentMatch ? null : 'drift-report-stale'
  };
}
function validChangeSet(value) {
  if (!exact(value, [
    'changeSetId', 'changes', 'classifierVersion', 'committedGenerationId',
    'currentHash', 'environmentId', 'generatedAt', 'jobId',
    'limitations', 'previousHash', 'schemaVersion', 'sourceFingerprint', 'summary'
  ]) || value.schemaVersion !== 2 ||
      !/^api-change-classifier-v[1-9][0-9]*$/.test(String(value.classifierVersion || '')) ||
      !/^changes-[a-f0-9]{24}$/.test(String(value.changeSetId || '')) ||
      !GENERATION_RE.test(String(value.committedGenerationId || '')) ||
      !/^job-[a-f0-9]{32}$/.test(String(value.jobId || '')) ||
      !HASH_RE.test(String(value.sourceFingerprint || '')) ||
      !HASH_RE.test(String(value.currentHash || '')) ||
      (value.previousHash !== null && !HASH_RE.test(String(value.previousHash || ''))) ||
      ['local', 'dev', 'stage', 'prod'].indexOf(value.environmentId) < 0 ||
      !Number.isFinite(Date.parse(value.generatedAt)) ||
      !boundedLines(value.limitations, 50, 100) ||
      !exact(value.summary, [
        'breaking', 'compatible', 'info', 'potentiallyBreaking', 'total'
      ]) || !Array.isArray(value.changes) || value.changes.length > 10000) return false;
  var summary = {
    breaking: 0, potentiallyBreaking: 0, compatible: 0, info: 0
  };
  var seen = Object.create(null);
  if (!value.changes.every(function (change) {
    if (!exact(change, [
      'affectedConsumers', 'affectedImplementation', 'afterSummary',
      'beforeSummary', 'evidence', 'id', 'kind', 'linkedTasks', 'modelId',
      'operationId', 'severity'
    ]) || !/^chg-[a-f0-9]{24}$/.test(String(change.id || '')) || seen[change.id] ||
        ['breaking', 'potentially-breaking', 'compatible', 'info']
          .indexOf(change.severity) < 0 ||
        !boundedLine(change.kind, 80) ||
        (change.operationId !== null && !boundedLine(change.operationId, 200)) ||
        (change.modelId !== null && !boundedLine(change.modelId, 200)) ||
        (change.beforeSummary !== null && !boundedLine(change.beforeSummary, 500)) ||
        (change.afterSummary !== null && !boundedLine(change.afterSummary, 500)) ||
        (change.affectedImplementation !== null &&
          !boundedLine(change.affectedImplementation, 500)) ||
        !boundedLines(change.affectedConsumers, 500, 500) ||
        !boundedLines(change.evidence, 20, 500) ||
        !Array.isArray(change.linkedTasks) || change.linkedTasks.length > 100 ||
        change.linkedTasks.some(function (stem) {
          return !/^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/.test(String(stem || ''));
        })) return false;
    seen[change.id] = 1;
    summary[change.severity === 'potentially-breaking'
      ? 'potentiallyBreaking' : change.severity]++;
    return true;
  })) return false;
  var summaryValid = Object.keys(summary).every(function (key) {
    return Number.isSafeInteger(value.summary[key]) &&
      value.summary[key] === summary[key];
  }) && Number.isSafeInteger(value.summary.total) &&
    value.summary.total === value.changes.length;
  return summaryValid;
}
function changeState(current) {
  var role = current.artifacts['change-report'];
  var manifestRow = current.manifest.artifacts.find(function (row) {
    return row.role === 'change-report';
  });
  if (!role || !manifestRow) return {
    state: 'missing', current: false, value: null,
    limitation: (current.optionalArtifactIssues || []).some(function (row) {
      return row.role === 'change-report';
    }) ? 'change-report-invalid' : 'change-report-missing'
  };
  try {
    var hit = fileGuards.boundedRegularFileUnder(
      paths.PROJECT_ROOT, path.dirname(role), role, Math.min(REPORT_MAX, manifestRow.size)
    );
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1' ||
        hit.bytes.length !== manifestRow.size ||
        sha(hit.bytes) !== manifestRow.hash) throw new Error('unsafe');
    var value = JSON.parse(hit.bytes.toString('utf8'));
    var valid = validChangeSet(value);
    var currentMatch = valid &&
      value.committedGenerationId === current.manifest.generationId &&
      value.currentHash === current.snapshotHash &&
      value.environmentId === current.environmentId;
    return {
      state: currentMatch ? 'present' : 'invalid',
      current: currentMatch,
      value: currentMatch ? value : null,
      hash: currentMatch ? sha(hit.bytes) : null,
      limitation: currentMatch ? null : 'change-report-invalid'
    };
  } catch (error) {
    return { state: 'invalid', current: false, value: null, limitation: 'change-report-invalid' };
  }
}
function snapshot(options) {
  options = options || {};
  var current = generation.current();
  var integration = backendIntegration.get();
  var active = activeEnvironment(integration);
  if (!current.ok) {
    return {
      ok: false, status: 409, error: current.error,
      current: current, integration: integration, activeEnvironment: active,
      limitations: ['contract-generation-invalid']
    };
  }
  if (current.mode === 'none') {
    return {
      ok: true, empty: true, current: current, integration: integration,
      activeEnvironment: active, tasks: taskSource.scanOpen(),
      committedGenerationId: null, contractHash: null, environmentId: null,
      projectCodeRevision: null,
      reportHashes: { implementation: null, consumers: null, drift: null, changes: null },
      limitations: ['contract-missing']
    };
  }
  if (!validInventory(current.inventory)) {
    return {
      ok: false, status: 409, error: 'generation-inventory-invalid',
      current: current, integration: integration, activeEnvironment: active,
      limitations: ['contract-generation-invalid']
    };
  }
  var inputs = currentInputs(options.freshInputs === true);
  var limitations = [];
  if (!inputs.ok) limitations.push(inputs.error || 'project-input-revision-unavailable');
  var implementationRaw = readOptional('implementation-map.json');
  var implementationCurrent = implementationRaw.state === 'present' &&
    reportMetaMatches(implementationRaw.value, current, inputs) &&
    validImplementation(implementationRaw.value, current.inventory);
  if (!implementationCurrent) {
    limitations.push(implementationRaw.state === 'invalid' ||
      implementationRaw.state === 'present' &&
        !validImplementation(implementationRaw.value, current.inventory)
      ? 'implementation-report-invalid'
      : implementationRaw.state === 'missing'
        ? 'implementation-report-missing' : 'implementation-report-stale');
  } else {
    limitations = limitations.concat(reportLimitations(implementationRaw.value));
    if (implementationRaw.value.analysisStatus === 'partial') {
      limitations.push('implementation-analysis-partial');
    }
  }
  var consumerRaw = readOptional('consumer-map.json');
  var architecture = arch.readValidated();
  var consumersCurrent = consumerRaw.state === 'present' &&
    reportMetaMatches(consumerRaw.value, current, inputs) &&
    validConsumers(consumerRaw.value, current.inventory) &&
    ((consumerRaw.value.architectureStructuralHash || null) ===
      (architecture.present ? architecture.map.structuralHash : null));
  if (!consumersCurrent) {
    limitations.push(consumerRaw.state === 'invalid' ||
      consumerRaw.state === 'present' &&
        !validConsumers(consumerRaw.value, current.inventory)
      ? 'consumer-report-invalid'
      : consumerRaw.state === 'missing'
        ? 'consumer-report-missing' : 'consumer-report-stale');
  } else {
    limitations = limitations.concat(reportLimitations(consumerRaw.value));
    if (consumerRaw.value.analysisStatus === 'partial' &&
        !consumerRaw.value.limitations.length) {
      limitations.push('consumer-analysis-partial');
    }
  }
  var drift = driftState(current, inputs);
  var changes = changeState(current);
  if (drift.limitation) limitations.push(drift.limitation);
  if (drift.current) limitations = limitations.concat(reportLimitations(drift.value));
  if (changes.limitation) limitations.push(changes.limitation);
  if (changes.current) limitations = limitations.concat(reportLimitations(changes.value));
  var tasks = taskSource.scanOpen();
  if (!tasks.ok) limitations.push(tasks.error || 'task-origin-index-partial');
  if (!tasks.historyOk) limitations.push('task-origin-history-partial');
  return {
    ok: true,
    empty: false,
    current: current,
    inventory: current.inventory,
    integration: integration,
    activeEnvironment: active,
    environmentMismatch: environmentMismatch(active, current.environmentId),
    inputs: inputs,
    committedGenerationId: current.manifest.generationId,
    contractHash: current.snapshotHash,
    environmentId: current.environmentId,
    projectCodeRevision: inputs.ok ? inputs.projectCodeRevision :
      implementationCurrent ? implementationRaw.value.projectCodeRevision : null,
    reportHashes: {
      implementation: implementationCurrent ? implementationRaw.hash : null,
      consumers: consumersCurrent ? consumerRaw.hash : null,
      drift: drift.current ? drift.hash : null,
      changes: changes.current ? changes.hash : null
    },
    implementation: implementationCurrent ? implementationRaw.value : null,
    implementationByOperation: implementationCurrent
      ? operationMap(implementationRaw.value.operations) : Object.create(null),
    consumers: consumersCurrent ? consumerRaw.value : null,
    consumersByOperation: consumersCurrent
      ? operationMap(consumerRaw.value.operations) : Object.create(null),
    drift: drift,
    changes: changes,
    tasks: tasks,
    architecture: architecture,
    limitations: unique(limitations)
  };
}
function meta(value) {
  return {
    schemaVersion: 1,
    committedGenerationId: value.committedGenerationId || null,
    contractHash: value.contractHash || null,
    environmentId: value.environmentId || null,
    projectCodeRevision: value.projectCodeRevision || null,
    reportHashes: value.reportHashes || {
      implementation: null, consumers: null, drift: null, changes: null
    },
    taskIndexRevision: value.tasks && value.tasks.revision || null,
    limitations: (value.limitations || []).slice(0, 50)
  };
}
function checkExpected(value, expected) {
  if (expected === null || expected === undefined || expected === '') return null;
  if (!GENERATION_RE.test(String(expected))) {
    return { ok: false, status: 400, error: 'bad-api-generation' };
  }
  if (expected !== value.committedGenerationId) {
    return Object.assign(meta(value), {
      ok: false, status: 409, error: 'api-generation-conflict'
    });
  }
  return null;
}

module.exports = {
  HASH_RE: HASH_RE,
  GENERATION_RE: GENERATION_RE,
  sha: sha,
  safeString: safeString,
  safePath: safePath,
  sourceId: sourceId,
  publicTask: publicTask,
  taskProjection: taskProjection,
  mismatchId: mismatchId,
  validArea: validArea,
  snapshot: snapshot,
  meta: meta,
  checkExpected: checkExpected,
  _test: {
    activeEnvironment: activeEnvironment,
    environmentMismatch: environmentMismatch,
    validInventory: validInventory,
    validImplementation: validImplementation,
    validConsumers: validConsumers,
    validDrift: validDrift,
    validChangeSet: validChangeSet,
    reportLimitations: reportLimitations
  }
};
