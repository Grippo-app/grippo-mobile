'use strict';

// Hardened Architecture Map v2 reader plus bounded Architecture API
// projections. The storage boundary accepts only the canonical v2 contract.

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var childProcess = require('child_process');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var contract = require('./architecture-contract');

var ARCH_MAP_FILE = path.join(paths.PROJECT_ORCHESTRATOR_DIR, '.arch-map.json');
var ARCH_CACHE_DIR = path.join(paths.PROJECT_ORCHESTRATOR_DIR, '.cache', 'architecture');
var HISTORY_DIR = path.join(ARCH_CACHE_DIR, 'history');
var HISTORY_INDEX = path.join(ARCH_CACHE_DIR, 'history-index.json');
var LATEST_DIFF = path.join(ARCH_CACHE_DIR, 'latest-diff.json');
var LATEST_TASK_DIFF = path.join(ARCH_CACHE_DIR, 'latest-task-diff.json');
var JOB_DIR = path.join(ARCH_CACHE_DIR, 'jobs');
var SETTINGS_FILE = path.join(paths.PROJECT_ROOT, 'settings.gradle.kts');
var GENERATOR = path.join(paths.PROJECT_ORCHESTRATOR_DIR, 'tasks', 'regen-arch.py');
var CURSOR_SECRET = crypto.randomBytes(32);
var RESPONSE_MAX = 1024 * 1024;
var DEFAULT_LIMIT = 100;
var MAX_LIMIT = 500;
var cache = null;
var revisionCache = null;

function publicError(value) {
  var known = {
    'architecture-map-missing': 1,
    'architecture-map-json-invalid': 1,
    'architecture-map-version-unsupported': 1,
    'architecture-map-contract-invalid': 1,
    'architecture-map-envelope-invalid': 1,
    'architecture-map-too-large': 1,
    'architecture-map-bounds-invalid': 1,
    'architecture-map-order-invalid': 1,
    'architecture-analysis-invalid': 1,
    'architecture-summary-invalid': 1,
    'architecture-node-invalid': 1,
    'architecture-edge-invalid': 1,
    'architecture-finding-invalid': 1,
    'architecture-finding-evidence-invalid': 1,
    'architecture-ownership-conflict': 1,
    'architecture-structural-hash-invalid': 1
  };
  var code = String(value && value.code || value || '');
  return known[code] ? code : 'architecture-map-invalid';
}
function statKey(stat) {
  return [stat.dev, stat.ino, stat.size, stat.mtimeMs, stat.ctimeMs, stat.nlink].join(':');
}
function settingsReady() {
  try {
    var hit = fileGuards.boundedRegularFileUnder(
      paths.PROJECT_ROOT, paths.PROJECT_ROOT, SETTINGS_FILE, 4 * 1024 * 1024
    );
    return !!(hit && hit.stat && String(hit.stat.nlink) === '1');
  } catch (error) { return false; }
}
function readValidated() {
  var hit;
  try {
    hit = fileGuards.boundedRegularFileUnder(
      paths.PROJECT_ROOT,
      paths.PROJECT_ORCHESTRATOR_DIR,
      ARCH_MAP_FILE,
      contract.MAX_MAP_BYTES
    );
  } catch (error) {
    return { present: false, ready: settingsReady(), error: publicError(error) };
  }
  if (!hit) return { present: false, ready: settingsReady(), error: null };
  if (!hit.stat || String(hit.stat.nlink) !== '1') {
    return { present: false, ready: settingsReady(), error: 'architecture-map-invalid' };
  }
  var sourceHash = contract._test.sha(hit.bytes);
  var key = statKey(hit.stat) + '|' + sourceHash;
  if (cache && cache.key === key) return cache.result;
  try {
    var parsed = contract.parse(hit.bytes);
    var result = {
      present: true,
      ready: true,
      error: null,
      sourceFileHash: sourceHash,
      map: parsed.map,
      nodeById: parsed.nodeById,
      edgeById: parsed.edgeById
    };
    cache = { key: key, result: result };
    return result;
  } catch (error2) {
    cache = { key: key, result: {
      present: false,
      ready: settingsReady(),
      error: publicError(error2)
    } };
    return cache.result;
  }
}
function invalidate() {
  cache = null;
  revisionCache = null;
}
function validRevisionProbe(value, snapshot) {
  if (!exactObject(value, [
    'actualHash', 'actualRevision', 'expectedHash', 'expectedRevision',
    'fresh', 'path', 'reason', 'status', 'version'
  ]) || value.version !== 2 || value.status !== 'ready' ||
      value.path !== 'orchestrator/.arch-map.json' ||
      typeof value.fresh !== 'boolean' ||
      !contract.HASH_RE.test(String(value.expectedHash || '')) ||
      !contract.HASH_RE.test(String(value.expectedRevision || '')) ||
      (value.actualHash === null) !== (value.actualRevision === null) ||
      value.actualHash !== null && (
        !contract.HASH_RE.test(String(value.actualHash || '')) ||
        !contract.HASH_RE.test(String(value.actualRevision || ''))
      )) return false;
  var expectedFresh = value.actualRevision !== null &&
    value.actualRevision === value.expectedRevision;
  if (value.fresh !== expectedFresh ||
      value.reason !== (expectedFresh ? null : 'source-revision-drift')) return false;
  return !snapshot.present || (
    value.actualHash === snapshot.map.structuralHash &&
    value.actualRevision === snapshot.map.generatedAtRevision
  );
}
function revisionProbe(snapshot) {
  if (!snapshot.ready) return null;
  var key = snapshot.present
    ? snapshot.map.generatedAtRevision + '|' + snapshot.map.structuralHash
    : 'missing|' + String(snapshot.error || '');
  if (revisionCache && revisionCache.key === key && Date.now() - revisionCache.at < 5000) {
    return revisionCache.value;
  }
  var result;
  try {
    result = childProcess.spawnSync(process.env.ARCHITECTURE_PYTHON || 'python3',
      [GENERATOR, '--revision-json'], {
        cwd: paths.PROJECT_ROOT,
        encoding: 'utf8',
        timeout: 30000,
        maxBuffer: 128 * 1024,
        env: Object.assign({}, process.env, {
          PYTHONDONTWRITEBYTECODE: '1',
          ORCHESTRATOR_PROJECT_ROOT: paths.PROJECT_ROOT
        })
      });
  } catch (error) {
    result = null;
  }
  var value = null;
  if (result && result.status === 0 && typeof result.stdout === 'string' &&
      Buffer.byteLength(result.stdout, 'utf8') <= 128 * 1024) {
    try {
      var parsed = JSON.parse(result.stdout);
      if (validRevisionProbe(parsed, snapshot)) value = parsed;
    } catch (ignore) {}
  }
  revisionCache = { key: key, at: Date.now(), value: value };
  return value;
}
function freshness(snapshot) {
  if (!snapshot.present) {
    var missingProbe = revisionProbe(snapshot);
    return {
      status: 'missing', reason: snapshot.error || (snapshot.ready ? 'not-generated' : 'project-not-ready'),
      generatedAt: null, generatedAtRevision: null,
      currentRevision: missingProbe && missingProbe.expectedRevision || null,
      generatorVersion: null
    };
  }
  var map = snapshot.map;
  var probe = revisionProbe(snapshot);
  var reason = null;
  if (!probe) reason = 'revision-unavailable';
  else if (probe.expectedRevision !== map.generatedAtRevision) reason = 'source-revision-drift';
  else if (probe.expectedHash !== map.structuralHash) reason = 'structural-drift';
  return {
    status: reason ? 'stale' : 'fresh',
    reason: reason,
    generatedAt: map.generatedAt,
    generatedAtRevision: map.generatedAtRevision,
    currentRevision: probe && probe.expectedRevision || null,
    generatorVersion: map.generatorVersion
  };
}
function ownerIndex(map) {
  var result = Object.create(null);
  map.edges.forEach(function (edge) {
    if (edge.kind === 'owns') result[edge.to] = edge.from;
  });
  return result;
}
function relationIndexes(map) {
  var incoming = Object.create(null), outgoing = Object.create(null);
  map.nodes.forEach(function (node) { incoming[node.id] = []; outgoing[node.id] = []; });
  map.edges.forEach(function (edge) {
    incoming[edge.to].push(edge);
    outgoing[edge.from].push(edge);
  });
  return { incoming: incoming, outgoing: outgoing };
}
function publicNode(node, owners, byId) {
  var owner = owners[node.id] && byId[owners[node.id]];
  return {
    id: node.id, kind: node.kind, name: node.name, path: node.path,
    platform: node.platform, layer: node.layer, metadata: node.metadata,
    owner: owner ? { id: owner.id, name: owner.name } : null
  };
}
function findingConfidence(row, edgeById, analysisStatus) {
  var rank = { exact: 0, derived: 1, heuristic: 2 };
  var values = row.evidence.map(function (evidence) {
    return evidence.edgeId && edgeById[evidence.edgeId]
      ? edgeById[evidence.edgeId].evidence.confidence : null;
  }).filter(Boolean);
  if (!values.length) {
    return row.type === 'unused-repository' && analysisStatus === 'partial'
      ? 'heuristic' : 'derived';
  }
  return values.sort(function (left, right) { return rank[right] - rank[left]; })[0];
}
function publicFinding(row, edgeById, analysisStatus) {
  return {
    id: row.id, type: row.type, severity: row.severity, title: row.title,
    summary: row.summary, affectedNodeIds: row.affectedNodeIds,
    evidence: row.evidence, ruleId: row.ruleId,
    firstSeenRevision: row.firstSeenRevision, fingerprint: row.fingerprint,
    confidence: findingConfidence(row, edgeById || Object.create(null), analysisStatus)
  };
}
function safeJsonSize(value) {
  return Buffer.byteLength(JSON.stringify(value), 'utf8');
}
function filterHash(value) { return contract._test.sha(contract._test.canonical(value)); }
function encodeCursor(structuralHash, filters, offset, direction) {
  var payload = {
    v: 1, h: structuralHash, f: filterHash(filters), o: offset,
    d: direction || 'rows'
  };
  var raw = Buffer.from(JSON.stringify(payload), 'utf8').toString('base64url');
  var sig = crypto.createHmac('sha256', CURSOR_SECRET).update(raw).digest('base64url');
  return raw + '.' + sig;
}
function decodeCursor(value, structuralHash, filters, direction) {
  if (!value) return 0;
  if (typeof value !== 'string' || value.length > 1024 || value.indexOf('.') < 0) {
    throw Object.assign(new Error('cursor invalid'), { code: 'architecture-cursor-invalid', httpStatus: 400 });
  }
  var parts = value.split('.');
  var expected = crypto.createHmac('sha256', CURSOR_SECRET).update(parts[0]).digest('base64url');
  if (parts.length !== 2 || parts[1].length !== expected.length ||
      !crypto.timingSafeEqual(Buffer.from(parts[1]), Buffer.from(expected))) {
    throw Object.assign(new Error('cursor invalid'), { code: 'architecture-cursor-invalid', httpStatus: 400 });
  }
  var payload;
  try { payload = JSON.parse(Buffer.from(parts[0], 'base64url').toString('utf8')); }
  catch (error) { throw Object.assign(new Error('cursor invalid'), { code: 'architecture-cursor-invalid', httpStatus: 400 }); }
  if (!payload || payload.v !== 1 || payload.h !== structuralHash ||
      payload.f !== filterHash(filters) || payload.d !== (direction || 'rows') ||
      !Number.isSafeInteger(payload.o) || payload.o < 0) {
    throw Object.assign(new Error('cursor stale'), { code: 'architecture-cursor-stale', httpStatus: 409 });
  }
  return payload.o;
}
function listLimit(value) {
  if (value === undefined || value === null || value === '') return DEFAULT_LIMIT;
  var result = Number(value);
  if (!Number.isSafeInteger(result) || result < 1 || result > MAX_LIMIT) {
    throw Object.assign(new Error('limit invalid'), { code: 'architecture-filter-invalid', httpStatus: 400 });
  }
  return result;
}
function boundedPage(rows, start, limit, envelope, maximumBytes) {
  var byteLimit = maximumBytes || RESPONSE_MAX;
  var selected = [];
  for (var index = start; index < rows.length && selected.length < limit; index++) {
    var candidate = selected.concat([rows[index]]);
    if (safeJsonSize(Object.assign({}, envelope, { rows: candidate })) > byteLimit) break;
    selected.push(rows[index]);
  }
  if (start < rows.length && !selected.length) {
    throw Object.assign(new Error('architecture row exceeds response limit'), {
      code: 'architecture-response-too-large', httpStatus: 503
    });
  }
  return { rows: selected, offset: start + selected.length };
}
function validateFilterValue(value, allowed, code) {
  if (value == null || value === '') return null;
  if (typeof value !== 'string' || allowed.indexOf(value) < 0) {
    throw Object.assign(new Error('filter invalid'), { code: code || 'architecture-filter-invalid', httpStatus: 400 });
  }
  return value;
}
function normalizedSearch(value) {
  if (value == null || value === '') return '';
  if (typeof value !== 'string' || Array.from(value).length > 200 ||
      /[\x00-\x1f\x7f]/.test(value)) {
    throw Object.assign(new Error('search invalid'), { code: 'architecture-filter-invalid', httpStatus: 400 });
  }
  return value.normalize('NFKC').trim().toLowerCase();
}
function nodeFilters(options) {
  options = options || {};
  return {
    search: normalizedSearch(options.search),
    kind: validateFilterValue(options.kind, [
      'module', 'feature', 'screen', 'component', 'repository', 'data-source',
      'api', 'database-entity', 'data'
    ]),
    platform: validateFilterValue(options.platform, ['shared', 'android', 'ios', 'tooling', 'unknown']),
    layer: validateFilterValue(options.layer, ['ui', 'domain', 'data', 'infrastructure', 'build', 'unknown']),
    ownership: normalizedSearch(options.ownership),
    changed: options.changed === true || options.changed === 'true'
  };
}
function ownerMatches(node, ownership, owners, byId) {
  if (!ownership) return true;
  var owner = owners[node.id] && byId[owners[node.id]];
  var values = [
    node.id,
    node.metadata && node.metadata.ownershipId,
    owner && owner.id,
    owner && owner.name,
    owner && owner.metadata && owner.metadata.ownershipId
  ].filter(Boolean).join('\n').normalize('NFKC').toLowerCase();
  return values.indexOf(ownership) >= 0;
}
function currentTaskDiff(snapshot) {
  var diff = readDiff('task', null, snapshot);
  if (diff.error) {
    throw Object.assign(new Error('task diff failed integrity validation'), {
      code: diff.error, httpStatus: 409
    });
  }
  if (!diff.present || !diff.diff || diff.diff.baselineCreated) {
    throw Object.assign(new Error('task diff is unavailable'), {
      code: 'architecture-diff-unavailable', httpStatus: 409
    });
  }
  if (diff.diff.truncated) {
    throw Object.assign(new Error('task diff is truncated'), {
      code: 'architecture-diff-truncated', httpStatus: 409
    });
  }
  return diff.diff;
}
function latestTaskChangedIds(snapshot) {
  var diff = currentTaskDiff(snapshot);
  var changed = Object.create(null);
  var changes = diff.changes || {};
  ['nodesAdded', 'nodesRemoved'].forEach(function (key) {
    (changes[key] || []).forEach(function (id) { changed[id] = 1; });
  });
  (changes.ownershipChanges || []).forEach(function (row) { if (row && row.nodeId) changed[row.nodeId] = 1; });
  return { ids: changed, diffId: diff.id };
}
function nodes(options) {
  var snapshot = readValidated();
  if (!snapshot.present) return { present: false, error: snapshot.error, ready: snapshot.ready };
  var map = snapshot.map, filters = nodeFilters(options), owners = ownerIndex(map);
  var changedSet = filters.changed ? latestTaskChangedIds(snapshot) : null;
  var changed = changedSet && changedSet.ids;
  var rows = map.nodes.filter(function (node) {
    if (filters.kind && (filters.kind === 'data'
      ? ['repository', 'data-source', 'api', 'database-entity'].indexOf(node.kind) < 0
      : node.kind !== filters.kind)) return false;
    if (filters.platform && node.platform !== filters.platform) return false;
    if (filters.layer && node.layer !== filters.layer) return false;
    if (!ownerMatches(node, filters.ownership, owners, snapshot.nodeById)) return false;
    if (changed && !changed[node.id]) return false;
    if (filters.search) {
      var haystack = [node.id, node.name, node.path || '', JSON.stringify(node.metadata)]
        .join('\n').normalize('NFKC').toLowerCase();
      if (haystack.indexOf(filters.search) < 0) return false;
    }
    return true;
  }).map(function (node) { return publicNode(node, owners, snapshot.nodeById); });
  var limit = listLimit(options && options.limit);
  var cursorFilters = Object.assign({}, filters);
  if (changedSet) cursorFilters.changedDiffId = changedSet.diffId;
  var start = decodeCursor(options && options.cursor, map.structuralHash, cursorFilters);
  if (start > rows.length) {
    throw Object.assign(new Error('cursor stale'), { code: 'architecture-cursor-stale', httpStatus: 409 });
  }
  var envelope = {
    schemaVersion: 1, present: true, structuralHash: map.structuralHash,
    total: rows.length, rows: []
  };
  var page = boundedPage(rows, start, limit, envelope);
  return Object.assign(envelope, {
    rows: page.rows,
    nextCursor: page.offset < rows.length
      ? encodeCursor(map.structuralHash, cursorFilters, page.offset) : null
  });
}
function findingFilters(options) {
  options = options || {};
  return {
    search: normalizedSearch(options.search),
    type: validateFilterValue(options.type, [
      'dependency-cycle', 'forbidden-dependency', 'orphan-module',
      'unused-repository', 'screen-without-owner'
    ]),
    severity: validateFilterValue(options.severity, ['error', 'warning', 'info']),
    confidence: validateFilterValue(options.confidence, ['exact', 'derived', 'heuristic']),
    platform: validateFilterValue(options.platform, ['shared', 'android', 'ios', 'tooling', 'unknown']),
    layer: validateFilterValue(options.layer, ['ui', 'domain', 'data', 'infrastructure', 'build', 'unknown']),
    ownership: normalizedSearch(options.ownership),
    changed: options.changed === true || options.changed === 'true'
  };
}
function latestTaskChangedFindingIds(snapshot) {
  var diff = currentTaskDiff(snapshot);
  var changed = Object.create(null);
  (diff.changes.findingsIntroduced || []).forEach(function (id) { changed[id] = 1; });
  return { ids: changed, diffId: diff.id };
}
function findings(options) {
  var snapshot = readValidated();
  if (!snapshot.present) return { present: false, error: snapshot.error, ready: snapshot.ready };
  var map = snapshot.map, filters = findingFilters(options);
  var owners = ownerIndex(map);
  var changedSet = filters.changed ? latestTaskChangedFindingIds(snapshot) : null;
  var changed = changedSet && changedSet.ids;
  var taskState = null;
  try { taskState = require('./task-source').scanOpen(); } catch (ignore) {}
  var rows = map.findings.filter(function (row) {
    var confidence = findingConfidence(row, snapshot.edgeById, map.analysis.status);
    if (filters.type && row.type !== filters.type) return false;
    if (filters.severity && row.severity !== filters.severity) return false;
    if (filters.confidence && confidence !== filters.confidence) return false;
    if (changed && !changed[row.id]) return false;
    var affected = row.affectedNodeIds.map(function (id) { return snapshot.nodeById[id]; }).filter(Boolean);
    if (filters.platform && !affected.some(function (node) { return node.platform === filters.platform; })) return false;
    if (filters.layer && !affected.some(function (node) { return node.layer === filters.layer; })) return false;
    if (filters.ownership && !affected.some(function (node) {
      return ownerMatches(node, filters.ownership, owners, snapshot.nodeById);
    })) return false;
    if (filters.search && [row.id, row.type, row.title, row.summary, row.ruleId]
      .join('\n').normalize('NFKC').toLowerCase().indexOf(filters.search) < 0) return false;
    return true;
  }).map(function (row) {
    var item = publicFinding(row, snapshot.edgeById, map.analysis.status);
    var task = taskState && (taskState.allByRef[row.id] || [])[0];
    item.linkedTask = task ? { stem: task.stem, title: task.title, column: task.column } : null;
    return item;
  });
  var limit = listLimit(options && options.limit);
  var cursorFilters = Object.assign({}, filters);
  if (changedSet) cursorFilters.changedDiffId = changedSet.diffId;
  var start = decodeCursor(options && options.cursor, map.structuralHash, cursorFilters);
  if (start > rows.length) throw Object.assign(new Error('cursor stale'), { code: 'architecture-cursor-stale', httpStatus: 409 });
  var envelope = { schemaVersion: 1, present: true, structuralHash: map.structuralHash,
    analysisStatus: map.analysis.status, total: rows.length, rows: [] };
  var page = boundedPage(rows, start, limit, envelope);
  return Object.assign(envelope, {
    rows: page.rows,
    nextCursor: page.offset < rows.length
      ? encodeCursor(map.structuralHash, cursorFilters, page.offset) : null
  });
}
function relationPage(snapshot, rows, cursor, limit, direction) {
  var filters = { nodeId: rows.nodeId, direction: direction };
  var start = decodeCursor(cursor, snapshot.map.structuralHash, filters, direction);
  var source = rows.items;
  if (start > source.length) throw Object.assign(new Error('cursor stale'), { code: 'architecture-cursor-stale', httpStatus: 409 });
  var publicRows = source.map(function (edge) {
    var otherId = direction === 'incoming' ? edge.from : edge.to;
    return {
      edge: edge,
      node: publicNode(snapshot.nodeById[otherId], ownerIndex(snapshot.map), snapshot.nodeById)
    };
  });
  var page = boundedPage(publicRows, start, limit, { rows: [] }, 300 * 1024);
  return {
    rows: page.rows,
    nextCursor: page.offset < source.length
      ? encodeCursor(snapshot.map.structuralHash, filters, page.offset, direction) : null,
    total: source.length
  };
}
function nodeDetail(id, options) {
  var snapshot = readValidated();
  if (!snapshot.present) return { present: false, error: snapshot.error, ready: snapshot.ready };
  var node = snapshot.nodeById[id];
  if (!node) {
    var diff = readDiff('latest', null, snapshot);
    return {
      present: false, error: 'architecture-node-not-found', structuralHash: snapshot.map.structuralHash,
      removedInLatestDiff: !!(diff.present && diff.diff.changes &&
        (diff.diff.changes.nodesRemoved || []).indexOf(id) >= 0)
    };
  }
  var indexes = relationIndexes(snapshot.map);
  var limit = listLimit(options && options.limit);
  var incoming = relationPage(snapshot, { nodeId: id, items: indexes.incoming[id] },
    options && options.incomingCursor, limit, 'incoming');
  var outgoing = relationPage(snapshot, { nodeId: id, items: indexes.outgoing[id] },
    options && options.outgoingCursor, limit, 'outgoing');
  var allRelatedFindings = snapshot.map.findings.filter(function (row) {
    return row.affectedNodeIds.indexOf(id) >= 0;
  }).map(function (row) {
    return publicFinding(row, snapshot.edgeById, snapshot.map.analysis.status);
  });
  var findingPage = boundedPage(allRelatedFindings, 0, 100, { rows: [] }, 200 * 1024);
  var relatedFindings = findingPage.rows;
  var linkedTasks = [];
  try {
    var tasks = require('./task-source').scanOpen();
    allRelatedFindings.forEach(function (finding) {
      (tasks.byRef[finding.id] || []).forEach(function (task) {
        if (!linkedTasks.some(function (row) { return row.stem === task.stem; })) {
          linkedTasks.push({ stem: task.stem, title: task.title, column: task.column });
        }
      });
    });
  } catch (ignore) {}
  return {
    schemaVersion: 1, present: true, structuralHash: snapshot.map.structuralHash,
    node: publicNode(node, ownerIndex(snapshot.map), snapshot.nodeById),
    incoming: incoming, outgoing: outgoing, findings: relatedFindings,
    findingsTotal: allRelatedFindings.length,
    findingsTruncated: relatedFindings.length < allRelatedFindings.length,
    linkedTasks: linkedTasks.slice(0, 100),
    linkedTasksTotal: linkedTasks.length,
    linkedTasksTruncated: linkedTasks.length > 100
  };
}
function topFindings(map) {
  var rank = { error: 0, warning: 1, info: 2 };
  var edgeById = Object.create(null);
  map.edges.forEach(function (edge) { edgeById[edge.id] = edge; });
  return map.findings.filter(function (row) {
    return row.severity === 'error' || row.severity === 'warning';
  }).sort(function (left, right) {
    return rank[left.severity] - rank[right.severity] || left.id.localeCompare(right.id);
  }).slice(0, 10).map(function (row) {
    return publicFinding(row, edgeById, map.analysis.status);
  });
}
function overview() {
  var snapshot = readValidated();
  var latestJob = latestGenerationJob();
  if (!snapshot.present) {
    return {
      schemaVersion: 1, present: false, ready: snapshot.ready, error: snapshot.error,
      freshness: freshness(snapshot), canGenerate: snapshot.ready,
      latestJob: latestJob
    };
  }
  var map = snapshot.map, owners = ownerIndex(map);
  var allUnowned = map.nodes.filter(function (node) {
    return node.kind === 'screen' && !owners[node.id];
  }).map(function (node) { return publicNode(node, owners, snapshot.nodeById); });
  var unowned = allUnowned.slice(0, 20);
  var latestTask = readDiff('task', null, snapshot);
  var taskState = null;
  try { taskState = require('./task-source').scanOpen(); } catch (ignore) {}
  var currentFreshness = freshness(snapshot);
  return {
    schemaVersion: 1,
    present: true,
    structuralHash: map.structuralHash,
    generatedAtRevision: map.generatedAtRevision,
    freshness: currentFreshness,
    canGenerate: true,
    latestJob: latestJob,
    analysis: map.analysis,
    taskIndex: {
      available: !!(taskState && taskState.revision),
      complete: !!(taskState && taskState.ok && taskState.historyOk),
      revision: taskState && taskState.revision || null
    },
    summary: map.summary,
    topFindings: topFindings(map),
    unownedScreens: unowned,
    unownedScreensTotal: allUnowned.length,
    unownedScreensTruncated: allUnowned.length > unowned.length,
    latestTaskDiff: latestTask.present ? latestTask.diff : null
  };
}
function readBoundedJson(file, directory, maxBytes) {
  try {
    var hit = fileGuards.boundedRegularFileUnder(
      paths.PROJECT_ROOT, directory, file, maxBytes
    );
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return null;
    return JSON.parse(hit.bytes.toString('utf8'));
  } catch (error) { return null; }
}
function readBoundedJsonState(file, directory, maxBytes) {
  var entry;
  try { entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, file); }
  catch (error) { return { state: 'invalid', value: null }; }
  if (!entry || entry.status === 'missing') return { state: 'missing', value: null };
  if (entry.status !== 'present') return { state: 'invalid', value: null };
  var value = readBoundedJson(file, directory, maxBytes);
  return value === null
    ? { state: 'invalid', value: null }
    : { state: 'present', value: value };
}
function validGenerationJob(value) {
  if (!exactObject(value, [
    'schemaVersion', 'id', 'state', 'phase', 'reason',
    'expectedSourceRevision', 'startedAt', 'finishedAt', 'structuralHash',
    'generatedAtRevision', 'error'
  ]) || value.schemaVersion !== 1 ||
      !/^archjob-[a-f0-9]{32}$/.test(String(value.id || '')) ||
      ['queued', 'running', 'succeeded', 'failed', 'interrupted'].indexOf(value.state) < 0 ||
      ['missing', 'stale', 'manual'].indexOf(value.reason) < 0 ||
      !contract.exactInstant(value.startedAt, true) ||
      value.expectedSourceRevision !== null &&
        !contract.HASH_RE.test(String(value.expectedSourceRevision || ''))) return false;
  if (value.state === 'queued' || value.state === 'running') {
    return (value.state === 'queued'
      ? value.phase === 'acquiring-writer'
      : ['analyzing', 'validating-publication'].indexOf(value.phase) >= 0) &&
      value.finishedAt === null && value.structuralHash === null &&
      value.generatedAtRevision === null && value.error === null;
  }
  if (!contract.exactInstant(value.finishedAt, true) ||
      Date.parse(value.finishedAt) < Date.parse(value.startedAt)) return false;
  if (value.state === 'succeeded') {
    return value.phase === 'completed' &&
      contract.HASH_RE.test(String(value.structuralHash || '')) &&
      contract.HASH_RE.test(String(value.generatedAtRevision || '')) &&
      value.error === null;
  }
  return value.phase === 'failed' && value.structuralHash === null &&
    value.generatedAtRevision === null &&
    exactObject(value.error, ['code']) &&
    /^architecture-[a-z0-9-]{1,100}$/.test(String(value.error.code || ''));
}
function latestGenerationJob() {
  var listed;
  try {
    listed = fileGuards.boundedDirectoryNamesUnder(
      paths.PROJECT_ROOT, JOB_DIR, 200
    );
  } catch (error) { return null; }
  if (!listed || !listed.ok) return null;
  var latest = null;
  (listed.names || []).filter(function (name) {
    return /^archjob-[a-f0-9]{32}\.json$/.test(name);
  }).forEach(function (name) {
    var value = readBoundedJson(path.join(JOB_DIR, name), JOB_DIR, 128 * 1024);
    if (!validGenerationJob(value)) return;
    if (!latest || Date.parse(value.startedAt) > Date.parse(latest.startedAt) ||
        value.startedAt === latest.startedAt && value.id > latest.id) latest = value;
  });
  return latest;
}
function exactObject(value, fields) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === fields.slice().sort().join('\0');
}
function sortedUnique(value, maximum, validator) {
  return Array.isArray(value) && value.length <= maximum &&
    value.every(validator) &&
    JSON.stringify(value) === JSON.stringify(Array.from(new Set(value)).sort());
}
function validDiff(value) {
  var baseFields = [
    'schemaVersion', 'id', 'trigger', 'triggerId', 'taskStem', 'createdAt',
    'previousHash', 'currentHash', 'previousRevision', 'currentRevision',
    'baselineCreated', 'generatorChanged', 'truncated', 'changeTotals', 'changes'
  ];
  var pointer = exactObject(value, baseFields.concat(['historyPath', 'followedByChanges']));
  if (!pointer && !exactObject(value, baseFields)) return false;
  if (value.schemaVersion !== 2 || !contract.ID_RE.test(String(value.id || '')) ||
      ['manual-refresh', 'task-finalization'].indexOf(value.trigger) < 0 ||
      !/^[A-Za-z0-9][A-Za-z0-9._-]{0,159}$/.test(String(value.triggerId || '')) ||
      !contract.exactInstant(value.createdAt, false) ||
      !contract.HASH_RE.test(String(value.currentHash || '')) ||
      !contract.HASH_RE.test(String(value.currentRevision || '')) ||
      value.previousHash !== null && !contract.HASH_RE.test(String(value.previousHash || '')) ||
      value.previousRevision !== null && !contract.HASH_RE.test(String(value.previousRevision || '')) ||
      typeof value.baselineCreated !== 'boolean' || typeof value.generatorChanged !== 'boolean' ||
      typeof value.truncated !== 'boolean' ||
      (value.previousHash === null) !== (value.previousRevision === null) ||
      value.baselineCreated !== (value.previousHash === null && value.previousRevision === null) ||
      value.trigger === 'task-finalization' &&
        !/^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/.test(String(value.taskStem || '')) ||
      value.trigger === 'manual-refresh' && value.taskStem !== null ||
      !exactObject(value.changes, [
        'nodesAdded', 'nodesRemoved', 'edgesAdded', 'edgesRemoved',
        'findingsIntroduced', 'findingsResolved', 'ownershipChanges',
        'capabilitiesAdded', 'capabilitiesRemoved'
      ]) ||
      !exactObject(value.changeTotals, [
        'nodesAdded', 'nodesRemoved', 'edgesAdded', 'edgesRemoved',
        'findingsIntroduced', 'findingsResolved', 'ownershipChanges',
        'capabilitiesAdded', 'capabilitiesRemoved'
      ])) return false;
  if (pointer && (
    typeof value.followedByChanges !== 'boolean' ||
    !contract.validRelativePath(value.historyPath) ||
    value.historyPath.indexOf('orchestrator/.cache/architecture/history/') !== 0
  )) return false;
  var changes = value.changes;
  if (!sortedUnique(changes.nodesAdded, 2000, function (id) { return contract.ID_RE.test(String(id || '')); }) ||
      !sortedUnique(changes.nodesRemoved, 2000, function (id) { return contract.ID_RE.test(String(id || '')); }) ||
      !sortedUnique(changes.edgesAdded, 5000, function (id) { return contract.ID_RE.test(String(id || '')); }) ||
      !sortedUnique(changes.edgesRemoved, 5000, function (id) { return contract.ID_RE.test(String(id || '')); }) ||
      !sortedUnique(changes.findingsIntroduced, 2000, function (id) { return contract.ID_RE.test(String(id || '')); }) ||
      !sortedUnique(changes.findingsResolved, 2000, function (id) { return contract.ID_RE.test(String(id || '')); }) ||
      !sortedUnique(changes.capabilitiesAdded, 32, function (id) { return /^[a-z][a-z0-9.-]{0,63}$/.test(id); }) ||
      !sortedUnique(changes.capabilitiesRemoved, 32, function (id) { return /^[a-z][a-z0-9.-]{0,63}$/.test(id); }) ||
      !Array.isArray(changes.ownershipChanges) || changes.ownershipChanges.length > 1000) return false;
  var prior = '';
  if (!changes.ownershipChanges.every(function (row) {
    if (!exactObject(row, ['nodeId', 'before', 'after']) ||
        !contract.ID_RE.test(String(row.nodeId || '')) ||
        row.before !== null && !contract.ID_RE.test(String(row.before || '')) ||
        row.after !== null && !contract.ID_RE.test(String(row.after || '')) ||
        row.before === row.after || row.nodeId <= prior) return false;
    prior = row.nodeId;
    return true;
  })) return false;
  var truncated = false;
  var totalsValid = Object.keys(value.changeTotals).every(function (key) {
    var total = value.changeTotals[key];
    var totalLimits = {
      nodesAdded: 10000, nodesRemoved: 10000,
      edgesAdded: 50000, edgesRemoved: 50000,
      findingsIntroduced: 5000, findingsResolved: 5000,
      ownershipChanges: 10000,
      capabilitiesAdded: 32, capabilitiesRemoved: 32
    };
    if (!Number.isSafeInteger(total) || total < changes[key].length ||
        total > totalLimits[key] ||
        value.baselineCreated && total !== 0) return false;
    if (total > changes[key].length) truncated = true;
    return true;
  });
  return totalsValid && value.truncated === truncated;
}
function historyDiff(selector) {
  var index = readBoundedJson(HISTORY_INDEX, ARCH_CACHE_DIR, 1024 * 1024);
  if (!index || index.schemaVersion !== 1 || !Array.isArray(index.entries) || index.entries.length > 100) return null;
  var row = index.entries.find(function (entry) {
    return entry && (entry.id === selector || entry.triggerId === selector ||
      entry.currentHash === selector || entry.taskStem === selector);
  });
  if (!row || !contract.validRelativePath(row.path) ||
      row.path.indexOf('orchestrator/.cache/architecture/history/') !== 0) return null;
  var file = path.join(paths.PROJECT_ROOT, row.path);
  var value = readBoundedJson(file, HISTORY_DIR, 2 * 1024 * 1024);
  return validDiff(value) ? value : null;
}
function readDiff(scope, selector, suppliedSnapshot) {
  var snapshot = suppliedSnapshot || readValidated();
  if (!snapshot.present) return { present: false, error: snapshot.error };
  var value, pointer;
  if (selector) {
    value = historyDiff(selector);
    pointer = value;
  } else {
    var file = scope === 'task' ? LATEST_TASK_DIFF : LATEST_DIFF;
    var state = readBoundedJsonState(file, ARCH_CACHE_DIR, 2 * 1024 * 1024);
    if (state.state === 'missing') return { present: false, error: null };
    if (state.state !== 'present') {
      return { present: false, error: 'architecture-diff-invalid' };
    }
    pointer = state.value;
    value = pointer;
  }
  if (!validDiff(value)) {
    return { present: false, error: 'architecture-diff-invalid' };
  }
  if (pointer.historyPath) {
    if (!contract.validRelativePath(pointer.historyPath) ||
        pointer.historyPath.indexOf('orchestrator/.cache/architecture/history/') !== 0) {
      return { present: false, error: 'architecture-diff-invalid' };
    }
    var immutable = readBoundedJson(path.join(paths.PROJECT_ROOT, pointer.historyPath),
      HISTORY_DIR, 2 * 1024 * 1024);
    if (!validDiff(immutable) || immutable.id !== pointer.id ||
        immutable.currentHash !== pointer.currentHash) {
      return { present: false, error: 'architecture-diff-invalid' };
    }
    var pointerBase = Object.assign({}, pointer);
    delete pointerBase.historyPath;
    delete pointerBase.followedByChanges;
    if (JSON.stringify(pointerBase) !== JSON.stringify(immutable)) {
      return { present: false, error: 'architecture-diff-invalid' };
    }
    value = immutable;
  }
  if (scope !== 'task' && !selector && value.currentHash !== snapshot.map.structuralHash) {
    return { present: false, error: 'architecture-diff-stale' };
  }
  var result = JSON.parse(JSON.stringify(value));
  result.followedByChanges = result.currentHash !== snapshot.map.structuralHash;
  return { schemaVersion: 1, present: true, structuralHash: snapshot.map.structuralHash, diff: result };
}
function graph(options) {
  var snapshot = readValidated();
  if (!snapshot.present) return { present: false, error: snapshot.error, ready: snapshot.ready };
  var filters = nodeFilters(options || {});
  var owners = ownerIndex(snapshot.map);
  var changedSet = filters.changed ? latestTaskChangedIds(snapshot) : null;
  var changed = changedSet && changedSet.ids;
  var selected = snapshot.map.nodes.filter(function (node) {
    if (filters.kind && (filters.kind === 'data'
      ? ['repository', 'data-source', 'api', 'database-entity'].indexOf(node.kind) < 0
      : node.kind !== filters.kind)) return false;
    if (filters.platform && node.platform !== filters.platform) return false;
    if (filters.layer && node.layer !== filters.layer) return false;
    if (!ownerMatches(node, filters.ownership, owners, snapshot.nodeById)) return false;
    if (changed && !changed[node.id]) return false;
    if (filters.search && [node.id, node.name, node.path || '', JSON.stringify(node.metadata)]
      .join('\n').normalize('NFKC').toLowerCase().indexOf(filters.search) < 0) return false;
    return true;
  });
  var selectedIds = Object.create(null);
  selected.forEach(function (node) { selectedIds[node.id] = true; });
  var edges = snapshot.map.edges.filter(function (edge) {
    return selectedIds[edge.from] && selectedIds[edge.to];
  });
  if (selected.length > 150 || edges.length > 500) {
    return {
      schemaVersion: 1, present: true, structuralHash: snapshot.map.structuralHash,
      tooLarge: true, nodeCount: selected.length, edgeCount: edges.length,
      nodes: [], edges: []
    };
  }
  var response = {
    schemaVersion: 1, present: true, structuralHash: snapshot.map.structuralHash,
    tooLarge: false, nodeCount: selected.length, edgeCount: edges.length,
    nodes: selected.map(function (node) {
      var projected = publicNode(node, owners, snapshot.nodeById);
      var severityRank = { error: 0, warning: 1, info: 2 };
      var related = snapshot.map.findings.filter(function (finding) {
        return finding.affectedNodeIds.indexOf(node.id) >= 0;
      }).sort(function (left, right) {
        return severityRank[left.severity] - severityRank[right.severity];
      });
      projected.findingSeverity = related.length ? related[0].severity : null;
      return projected;
    }),
    edges: edges
  };
  if (safeJsonSize(response) > RESPONSE_MAX) {
    return {
      schemaVersion: 1, present: true, structuralHash: snapshot.map.structuralHash,
      tooLarge: true, nodeCount: selected.length, edgeCount: edges.length,
      nodes: [], edges: []
    };
  }
  return response;
}
module.exports = {
  DEFAULT_LIMIT: DEFAULT_LIMIT,
  MAX_LIMIT: MAX_LIMIT,
  invalidate: invalidate,
  readValidated: readValidated,
  overview: overview,
  nodes: nodes,
  nodeDetail: nodeDetail,
  findings: findings,
  graph: graph,
  readDiff: readDiff,
  publicError: publicError,
  _test: {
    freshness: freshness,
    filterHash: filterHash,
    encodeCursor: encodeCursor,
    decodeCursor: decodeCursor,
    validDiff: validDiff
  }
};
