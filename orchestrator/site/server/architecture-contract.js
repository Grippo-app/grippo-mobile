'use strict';

// Shared Architecture Map v2 semantic contract. The canonical generator and
// this reader intentionally implement the same closed fields, bounds, sorting,
// ownership, cross-reference and structural-hash rules. Only the current
// canonical contract is accepted at this boundary.

var crypto = require('crypto');

var TOP_FIELDS = ['analysis', 'edges', 'findings', 'generatedAt',
  'generatedAtRevision', 'generatorVersion', 'nodes', 'schemaVersion',
  'structuralHash', 'summary'];
var ANALYSIS_FIELDS = ['capabilities', 'coverage', 'limitations', 'status'];
var COVERAGE_FIELDS = ['dependencyExpressions', 'excludedModules', 'gradleFiles',
  'kotlinFiles', 'parserErrors', 'schemaVersion', 'supportedApiPatterns',
  'supportedPersistencePatterns', 'unsupportedDependencyExpressions'];
var SUMMARY_FIELDS = ['dataSources', 'databaseEntities', 'features',
  'findingsBySeverity', 'modules', 'screens'];
var NODE_FIELDS = ['id', 'kind', 'layer', 'metadata', 'name', 'path', 'platform'];
var EDGE_FIELDS = ['evidence', 'from', 'id', 'kind', 'to'];
var EVIDENCE_FIELDS = ['analyzer', 'confidence', 'line', 'sourcePath'];
var FINDING_FIELDS = ['affectedNodeIds', 'evidence', 'fingerprint', 'firstSeenRevision',
  'id', 'ruleId', 'severity', 'summary', 'title', 'type'];
var FINDING_EVIDENCE_FIELDS = ['edgeId', 'line', 'nodeId', 'reasonCode', 'sourcePath'];
var HASH_RE = /^sha256:[0-9a-f]{64}$/;
var ID_RE = /^[a-z][a-z0-9-]{0,31}:[A-Za-z0-9._~/-]{1,147}$/;
var SMALL_ID_RE = /^[a-z][a-z0-9.-]{0,63}$/;
var RULE_ID_RE = /^[a-z][a-z0-9.-]{0,95}$/;
var NODE_KINDS = Object.freeze({ module: 1, feature: 1, screen: 1, component: 1,
  repository: 1, 'data-source': 1, api: 1, 'database-entity': 1 });
var EDGE_KINDS = Object.freeze({ 'depends-on': 1, owns: 1, implements: 1,
  consumes: 1, renders: 1, persists: 1, 'navigates-to': 1 });
var FINDING_TYPES = Object.freeze({ 'dependency-cycle': 1, 'forbidden-dependency': 1,
  'orphan-module': 1, 'unused-repository': 1, 'screen-without-owner': 1 });
var SEVERITIES = Object.freeze({ error: 1, warning: 1, info: 1 });
var PLATFORMS = Object.freeze({ shared: 1, android: 1, ios: 1, tooling: 1, unknown: 1 });
var LAYERS = Object.freeze({ ui: 1, domain: 1, data: 1, infrastructure: 1, build: 1, unknown: 1 });
var CONFIDENCES = Object.freeze({ exact: 1, derived: 1, heuristic: 1 });
var FINDING_REASON_CODES = Object.freeze({
  'forbidden-dependency': 1,
  'module-dependency-cycle': 1,
  'module-has-no-incoming-relation': 1,
  'repository-has-no-proven-consumer': 1,
  'screen-has-no-owner': 1
});
var LIMITATIONS = Object.freeze({
  'analysis-coverage-partial': 1,
  'api-class-not-resolved': 1,
  'database-schema-not-resolved': 1,
  'dependency-target-not-in-settings': 1,
  'unsupported-gradle-dependency-expression': 1
});
var MAX_MAP_BYTES = 5 * 1024 * 1024;

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function stable(value) {
  if (Array.isArray(value)) return value.map(stable);
  if (!value || typeof value !== 'object') return value;
  var out = {};
  Object.keys(value).sort().forEach(function (key) { out[key] = stable(value[key]); });
  return out;
}
function sha(value) {
  return 'sha256:' + crypto.createHash('sha256')
    .update(Buffer.isBuffer(value) ? value : Buffer.from(String(value), 'utf8')).digest('hex');
}
function canonical(value) { return Buffer.from(JSON.stringify(stable(value)), 'utf8'); }
function structuralPayload(value) {
  return {
    analysisCapabilities: value.analysis.capabilities,
    edges: value.edges,
    findings: value.findings,
    nodes: value.nodes
  };
}
function structuralHash(value) { return sha(canonical(structuralPayload(value))); }
function sortedUniqueStrings(value, max, pattern, maxLength) {
  return Array.isArray(value) && value.length <= max &&
    value.every(function (item) {
      return typeof item === 'string' && (!maxLength || textLength(item) <= maxLength) &&
        (!pattern || pattern.test(item));
    }) &&
    JSON.stringify(value) === JSON.stringify(Array.from(new Set(value)).sort());
}
function nonNegativeInteger(value) {
  return Number.isSafeInteger(value) && value >= 0;
}
function textLength(value) {
  return typeof value === 'string' ? Array.from(value).length : 0;
}
function exactInstant(value, allowMilliseconds) {
  if (typeof value !== 'string' ||
      value.slice(0, 4) === '0000' ||
      !(allowMilliseconds
        ? /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3})?Z$/
        : /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$/).test(value)) return false;
  var parsed = Date.parse(value);
  if (!Number.isFinite(parsed)) return false;
  var canonical = new Date(parsed).toISOString();
  return canonical === (value.indexOf('.') >= 0
    ? value : value.slice(0, -1) + '.000Z');
}
function validRelativePath(value) {
  if (typeof value !== 'string' || !value || Buffer.byteLength(value, 'utf8') > 512 ||
      value.indexOf('\\') >= 0 || value.charAt(0) === '/' || /^[A-Za-z]:\//.test(value) ||
      /[\x00-\x1f\x7f]/.test(value)) return false;
  var parts = value.split('/');
  return parts.every(function (part) { return !!part && part !== '.' && part !== '..'; });
}
function validId(value) {
  return typeof value === 'string' && Buffer.byteLength(value, 'ascii') <= 180 &&
    Buffer.byteLength(value, 'ascii') === Buffer.byteLength(value, 'utf8') &&
    ID_RE.test(value);
}
function validClassName(value) {
  return typeof value === 'string' && /^[A-Z][A-Za-z0-9_]{0,199}$/.test(value);
}
function sortedIdentifiers(value, pattern) {
  return sortedUniqueStrings(value, 100, pattern);
}
function validMetadata(kind, metadata) {
  if (!metadata || typeof metadata !== 'object' || Array.isArray(metadata)) return false;
  if (kind === 'module') {
    return exact(metadata, ['gradlePath', 'sourceSets']) &&
      typeof metadata.gradlePath === 'string' && metadata.gradlePath.length <= 180 &&
      /^:[A-Za-z0-9_.:-]+$/.test(metadata.gradlePath) &&
      sortedIdentifiers(metadata.sourceSets, /^[A-Za-z][A-Za-z0-9]{0,79}$/);
  }
  if (kind === 'feature') {
    return exact(metadata, ['ownershipId', 'interfaceClass']) &&
      typeof metadata.ownershipId === 'string' &&
      /^[A-Za-z0-9._~-]{1,180}$/.test(metadata.ownershipId) &&
      (metadata.interfaceClass === null || validClassName(metadata.interfaceClass));
  }
  if (kind === 'screen') {
    return exact(metadata, ['routes', 'rootSuffix']) &&
      sortedIdentifiers(metadata.routes, /^[A-Z][A-Za-z0-9_]{0,199}$/) &&
      typeof metadata.rootSuffix === 'boolean';
  }
  if (kind === 'component') {
    return exact(metadata, ['className']) && validClassName(metadata.className);
  }
  if (kind === 'repository') {
    return exact(metadata, ['className', 'role']) && validClassName(metadata.className) &&
      ['interface', 'implementation'].indexOf(metadata.role) >= 0;
  }
  if (kind === 'data-source') {
    return exact(metadata, ['className', 'sourceType']) && validClassName(metadata.className) &&
      ['RemoteDataSource', 'LocalDataSource', 'DataSource', 'Store', 'Dao']
        .indexOf(metadata.sourceType) >= 0;
  }
  if (kind === 'api') {
    return exact(metadata, ['className', 'methods']) && validClassName(metadata.className) &&
      sortedIdentifiers(metadata.methods, /^[a-z][A-Za-z0-9_]{0,199}$/);
  }
  if (kind === 'database-entity') {
    return exact(metadata, ['entityClass', 'database', 'version']) &&
      validClassName(metadata.entityClass) && validClassName(metadata.database) &&
      nonNegativeInteger(metadata.version) && metadata.version <= 0x7fffffff;
  }
  return false;
}
function problem(code, message) {
  var error = new Error(message || code);
  error.code = code;
  return error;
}

function validateV2(value, encodedBytes) {
  if (!exact(value, TOP_FIELDS) || value.schemaVersion !== 2) {
    throw problem('architecture-map-contract-invalid', 'Architecture map does not match the exact v2 envelope.');
  }
  if (!exactInstant(value.generatedAt, false) ||
      !HASH_RE.test(String(value.generatedAtRevision || '')) ||
      !HASH_RE.test(String(value.structuralHash || '')) ||
      !SMALL_ID_RE.test(String(value.generatorVersion || ''))) {
    throw problem('architecture-map-envelope-invalid', 'Architecture map envelope fields are invalid.');
  }
  if (encodedBytes && encodedBytes.length > MAX_MAP_BYTES) {
    throw problem('architecture-map-too-large', 'Architecture map exceeds 5 MiB.');
  }
  var analysis = value.analysis;
  if (!exact(analysis, ANALYSIS_FIELDS) || ['complete', 'partial'].indexOf(analysis.status) < 0 ||
      !sortedUniqueStrings(analysis.capabilities, 32, SMALL_ID_RE) ||
      !sortedUniqueStrings(analysis.limitations, 100, null, 240) ||
      !analysis.limitations.every(function (item) { return !!LIMITATIONS[item]; }) ||
      !exact(analysis.coverage, COVERAGE_FIELDS) || analysis.coverage.schemaVersion !== 1) {
    throw problem('architecture-analysis-invalid', 'Architecture analysis metadata is invalid.');
  }
  if (analysis.status !== (analysis.limitations.length ? 'partial' : 'complete')) {
    throw problem('architecture-analysis-invalid', 'Architecture analysis status does not match its limitations.');
  }
  var coverageLimits = {
    gradleFiles: 20000,
    dependencyExpressions: 1000000,
    unsupportedDependencyExpressions: 1000000,
    kotlinFiles: 20000,
    parserErrors: 10000,
    excludedModules: 10000
  };
  Object.keys(coverageLimits).forEach(function (field) {
    if (!nonNegativeInteger(analysis.coverage[field]) ||
        analysis.coverage[field] > coverageLimits[field]) {
      throw problem('architecture-analysis-invalid', 'Architecture coverage count is invalid.');
    }
  });
  ['supportedPersistencePatterns', 'supportedApiPatterns'].forEach(function (field) {
    if (!sortedUniqueStrings(analysis.coverage[field], 20, SMALL_ID_RE)) {
      throw problem('architecture-analysis-invalid', 'Architecture supported-pattern ids are invalid.');
    }
  });
  var summary = value.summary;
  if (!exact(summary, SUMMARY_FIELDS) ||
      !exact(summary.findingsBySeverity, ['error', 'warning', 'info'])) {
    throw problem('architecture-summary-invalid', 'Architecture summary fields are invalid.');
  }
  ['modules', 'features', 'screens', 'dataSources', 'databaseEntities'].forEach(function (field) {
    if (summary[field] !== null && !nonNegativeInteger(summary[field])) {
      throw problem('architecture-summary-invalid', 'Architecture summary count is invalid.');
    }
  });
  ['error', 'warning', 'info'].forEach(function (field) {
    if (!nonNegativeInteger(summary.findingsBySeverity[field])) {
      throw problem('architecture-summary-invalid', 'Architecture finding summary is invalid.');
    }
  });
  if (!Array.isArray(value.nodes) || value.nodes.length > 10000 ||
      !Array.isArray(value.edges) || value.edges.length > 50000 ||
      !Array.isArray(value.findings) || value.findings.length > 5000) {
    throw problem('architecture-map-bounds-invalid', 'Architecture arrays exceed their bounds.');
  }
  [['nodes', value.nodes], ['edges', value.edges], ['findings', value.findings]].forEach(function (pair) {
    var ids = pair[1].map(function (row) { return row && row.id; });
    if (JSON.stringify(ids) !== JSON.stringify(ids.slice().sort()) || new Set(ids).size !== ids.length) {
      throw problem('architecture-map-order-invalid', pair[0] + ' must be sorted with unique ids.');
    }
  });
  var nodeById = Object.create(null);
  value.nodes.forEach(function (node) {
    if (!exact(node, NODE_FIELDS) || !validId(node.id) || !NODE_KINDS[node.kind] ||
        typeof node.name !== 'string' || !node.name || textLength(node.name) > 200 ||
        node.path !== null && !validRelativePath(node.path) ||
        !PLATFORMS[node.platform] || !LAYERS[node.layer] ||
        !validMetadata(node.kind, node.metadata)) {
      throw problem('architecture-node-invalid', 'Architecture node is invalid.');
    }
    nodeById[node.id] = node;
  });
  var edgeById = Object.create(null);
  var owners = Object.create(null);
  var allowedEdgePairs = {
    'depends-on': { 'module>module': 1 },
    owns: { 'feature>screen': 1, 'feature>database-entity': 1 },
    implements: { 'feature>repository': 1 },
    consumes: {
      'module>repository': 1,
      'module>data-source': 1,
      'module>api': 1
    },
    renders: { 'screen>module': 1, 'screen>component': 1 },
    persists: { 'repository>database-entity': 1 },
    'navigates-to': { 'screen>screen': 1 }
  };
  value.edges.forEach(function (edge) {
    var evidence = edge && edge.evidence;
    if (!exact(edge, EDGE_FIELDS) || !validId(edge.id) || !nodeById[edge.from] ||
        !nodeById[edge.to] || !EDGE_KINDS[edge.kind] ||
        !allowedEdgePairs[edge.kind][
          nodeById[edge.from].kind + '>' + nodeById[edge.to].kind
        ] ||
        !exact(evidence, EVIDENCE_FIELDS) || !validRelativePath(evidence.sourcePath) ||
        evidence.line !== null && (!Number.isSafeInteger(evidence.line) ||
          evidence.line < 1 || evidence.line > 10000000) ||
        !SMALL_ID_RE.test(String(evidence.analyzer || '')) || !CONFIDENCES[evidence.confidence]) {
      throw problem('architecture-edge-invalid', 'Architecture edge or evidence is invalid.');
    }
    if (edge.kind === 'owns') {
      if (owners[edge.to] && owners[edge.to] !== edge.from) {
        throw problem('architecture-ownership-conflict', 'Architecture entity has conflicting owners.');
      }
      owners[edge.to] = edge.from;
    }
    edgeById[edge.id] = edge;
  });
  var severityCounts = { error: 0, warning: 0, info: 0 };
  value.findings.forEach(function (finding) {
    if (!exact(finding, FINDING_FIELDS) || !validId(finding.id) ||
        !FINDING_TYPES[finding.type] || !SEVERITIES[finding.severity] ||
        typeof finding.title !== 'string' || !finding.title || textLength(finding.title) > 200 ||
        typeof finding.summary !== 'string' || !finding.summary || textLength(finding.summary) > 1000 ||
        !Array.isArray(finding.affectedNodeIds) || !finding.affectedNodeIds.length ||
        finding.affectedNodeIds.length > 100 ||
        JSON.stringify(finding.affectedNodeIds) !== JSON.stringify(Array.from(new Set(finding.affectedNodeIds)).sort()) ||
        finding.affectedNodeIds.some(function (id) { return !nodeById[id]; }) ||
        !Array.isArray(finding.evidence) || !finding.evidence.length || finding.evidence.length > 20 ||
        !RULE_ID_RE.test(String(finding.ruleId || '')) ||
        !HASH_RE.test(String(finding.firstSeenRevision || '')) ||
        !HASH_RE.test(String(finding.fingerprint || ''))) {
      throw problem('architecture-finding-invalid', 'Architecture finding is invalid.');
    }
    finding.evidence.forEach(function (evidence) {
      if (!exact(evidence, FINDING_EVIDENCE_FIELDS) ||
          !validRelativePath(evidence.sourcePath) ||
          evidence.line !== null && (!Number.isSafeInteger(evidence.line) ||
            evidence.line < 1 || evidence.line > 10000000) ||
          evidence.edgeId !== null && !edgeById[evidence.edgeId] ||
          evidence.nodeId !== null && !nodeById[evidence.nodeId] ||
          !FINDING_REASON_CODES[evidence.reasonCode]) {
        throw problem('architecture-finding-evidence-invalid', 'Architecture finding evidence is invalid.');
      }
    });
    var expectedFingerprint = sha(canonical({
      type: finding.type,
      ruleId: finding.ruleId,
      affectedNodeIds: finding.affectedNodeIds,
      evidence: finding.evidence.map(function (row) {
        return {
          sourcePath: row.sourcePath,
          line: row.line,
          edgeId: row.edgeId,
          nodeId: row.nodeId,
          reasonCode: row.reasonCode
        };
      })
    }));
    if (finding.fingerprint !== expectedFingerprint) {
      throw problem('architecture-finding-invalid', 'Architecture finding fingerprint does not verify.');
    }
    severityCounts[finding.severity]++;
  });
  var expected = {
    modules: value.nodes.filter(function (row) { return row.kind === 'module'; }).length,
    features: value.nodes.filter(function (row) { return row.kind === 'feature'; }).length,
    screens: value.nodes.filter(function (row) { return row.kind === 'screen'; }).length,
    dataSources: value.nodes.filter(function (row) {
      return row.kind === 'repository' || row.kind === 'data-source';
    }).length
  };
  Object.keys(expected).forEach(function (key) {
    if (summary[key] !== expected[key]) {
      throw problem('architecture-summary-invalid', 'Architecture summary does not match nodes.');
    }
  });
  var entities = value.nodes.filter(function (row) { return row.kind === 'database-entity'; }).length;
  var databaseUnknown = analysis.limitations.indexOf('database-schema-not-resolved') >= 0;
  if ((summary.databaseEntities === null) !== databaseUnknown ||
      summary.databaseEntities !== null && summary.databaseEntities !== entities ||
      JSON.stringify(summary.findingsBySeverity) !== JSON.stringify(severityCounts)) {
    throw problem('architecture-summary-invalid', 'Architecture summary does not match findings/entities.');
  }
  if (value.structuralHash !== structuralHash(value)) {
    throw problem('architecture-structural-hash-invalid', 'Architecture structural hash does not verify.');
  }
  return {
    schemaVersion: 2,
    map: value,
    nodeById: nodeById,
    edgeById: edgeById
  };
}

function parse(bytes) {
  if (!Buffer.isBuffer(bytes) || bytes.length > MAX_MAP_BYTES) {
    throw problem('architecture-map-too-large');
  }
  var value;
  try {
    var decoded = new TextDecoder('utf-8', { fatal: true }).decode(bytes);
    value = JSON.parse(decoded);
  }
  catch (error) { throw problem('architecture-map-json-invalid', 'Architecture map is not valid UTF-8 JSON.'); }
  if (value && value.schemaVersion === 2) return validateV2(value, bytes);
  throw problem('architecture-map-version-unsupported');
}

module.exports = {
  MAX_MAP_BYTES: MAX_MAP_BYTES,
  HASH_RE: HASH_RE,
  ID_RE: ID_RE,
  exactInstant: exactInstant,
  validRelativePath: validRelativePath,
  structuralHash: structuralHash,
  validateV2: validateV2,
  parse: parse,
  _test: {
    canonical: canonical,
    stable: stable,
    sha: sha
  }
};
