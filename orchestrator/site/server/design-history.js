'use strict';

// Compact generation-to-generation Design history. The snapshot is written
// before its index row, both through the guarded atomic replacement primitive.
// Absence is an honest baseline state; no previous changes are invented.

var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var catalog = require('./design-catalog');

var DIR = path.join(paths.FIGMA_CACHE_DIR, 'design-history');
var INDEX_FILE = path.join(DIR, 'index.json');
var RECORD_MAX = 2 * 1024 * 1024;
var INDEX_MAX = 1024 * 1024;
var RETENTION = 100;
var GENERATION_RE = /^gen-[a-f0-9]{32}$/;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var ENTITY_RE = /^(?:tok|tokp|cmp|cmpp|srf)-[a-f0-9]{24}$/;
var writeQueue = Promise.resolve();

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function iso(value) {
  if (typeof value !== 'string') return false;
  var parsed = new Date(value);
  return Number.isFinite(parsed.getTime()) && parsed.toISOString() === value;
}
function recordFile(generationId) {
  if (!GENERATION_RE.test(String(generationId || ''))) throw new Error('design-history-generation-invalid');
  return path.join(DIR, generationId + '.json');
}
function safeBytes(file, max) {
  try {
    var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, DIR, file, max);
    return hit && hit.stat && String(hit.stat.nlink) === '1' ? hit.bytes : null;
  } catch (error) { return null; }
}
function atomicWrite(file, bytes, max) {
  if (!Buffer.isBuffer(bytes) || bytes.length > max) throw new Error('design-history-size-limit');
  var result = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, DIR, file, bytes, {
    create: true, directoryMode: 0o700, mode: 0o600, maxBytes: max
  });
  if (!result.ok) throw new Error(result.code || 'design-history-write-failed');
}
function jsonHash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(JSON.stringify(value), 'utf8').digest('hex');
}
function boundedJson(value, depth) {
  if (value === null || typeof value === 'boolean' ||
      typeof value === 'number' && Number.isFinite(value)) return true;
  if (typeof value === 'string') return value.length <= 500;
  if (depth <= 0 || !value || typeof value !== 'object') return false;
  if (Array.isArray(value)) {
    return value.length <= 20 && value.every(function (item) { return boundedJson(item, depth - 1); });
  }
  var keys = Object.keys(value);
  return keys.length <= 50 && keys.every(function (key) {
    return key.length <= 100 && boundedJson(value[key], depth - 1);
  });
}
function validateSummary(value) {
  if (!exact(value, ['id', 'entityType', 'name', 'status', 'summary']) ||
      !ENTITY_RE.test(String(value.id || '')) ||
      ['token', 'project-token', 'component', 'project-component', 'surface'].indexOf(value.entityType) < 0 ||
      typeof value.name !== 'string' || !value.name.length || value.name.length > 500 ||
      !value.summary || typeof value.summary !== 'object' || Array.isArray(value.summary)) return false;
  // The status axis is per entity type: comparison statuses for design-side
  // tokens and components, the classification for project-only rows, and the
  // established healthy/drifted status set for surfaces.
  var validStatus = value.entityType === 'token'
    ? Object.prototype.hasOwnProperty.call(catalog.TOKEN_STATUS, value.status)
    : value.entityType === 'project-token'
      ? Object.prototype.hasOwnProperty.call(catalog.PROJECT_TOKEN_CLASSIFICATION, value.status)
      : value.entityType === 'component'
        ? Object.prototype.hasOwnProperty.call(catalog.COMPONENT_STATUS, value.status)
        : value.entityType === 'project-component'
          ? Object.prototype.hasOwnProperty.call(catalog.PROJECT_COMPONENT_CLASSIFICATION, value.status)
          : ['healthy', 'drifted', 'missing', 'unknown'].indexOf(value.status) >= 0;
  if (!validStatus) return false;
  if (value.entityType === 'token') {
    return exact(value.summary, ['kind', 'values', 'changedSide', 'mappingState']) &&
      typeof value.summary.kind === 'string' && value.summary.kind.length <= 20 &&
      value.summary.values && typeof value.summary.values === 'object' &&
      !Array.isArray(value.summary.values) && boundedJson(value.summary.values, 4) &&
      Object.prototype.hasOwnProperty.call(catalog.TOKEN_CHANGED_SIDE, value.summary.changedSide) &&
      (value.summary.mappingState === null ||
        typeof value.summary.mappingState === 'string' && value.summary.mappingState.length <= 40);
  }
  if (value.entityType === 'project-token') {
    return exact(value.summary, ['kind', 'projectTokenId', 'classification']) &&
      typeof value.summary.kind === 'string' && value.summary.kind.length <= 20 &&
      typeof value.summary.projectTokenId === 'string' && value.summary.projectTokenId.length >= 1 &&
      value.summary.projectTokenId.length <= 400 &&
      value.summary.classification === value.status;
  }
  if (value.entityType === 'component') {
    return exact(value.summary, ['kind', 'changedSide', 'mappingState', 'mappingId']) &&
      typeof value.summary.kind === 'string' && value.summary.kind.length <= 20 &&
      Object.prototype.hasOwnProperty.call(catalog.COMPONENT_CHANGED_SIDE, value.summary.changedSide) &&
      (value.summary.mappingState === null ||
        typeof value.summary.mappingState === 'string' && value.summary.mappingState.length <= 40) &&
      (value.summary.mappingId === null ||
        typeof value.summary.mappingId === 'string' && value.summary.mappingId.length <= 40);
  }
  if (value.entityType === 'project-component') {
    return exact(value.summary, ['kind', 'projectComponentId', 'classification']) &&
      typeof value.summary.kind === 'string' && value.summary.kind.length <= 40 &&
      typeof value.summary.projectComponentId === 'string' && value.summary.projectComponentId.length >= 1 &&
      value.summary.projectComponentId.length <= 400 &&
      value.summary.classification === value.status;
  }
  return exact(value.summary, ['type', 'themes', 'locales']) &&
    ['screen', 'dialog', 'overlay'].indexOf(value.summary.type) >= 0 &&
    [value.summary.themes, value.summary.locales].every(function (items) {
      return Array.isArray(items) && items.length <= 100 && items.every(function (item) {
        return typeof item === 'string' && item.length <= 80;
      });
    });
}
function validateChange(value) {
  if (!exact(value, ['id', 'kind', 'previous', 'current']) ||
      !ENTITY_RE.test(String(value.id || '')) ||
      ['added', 'changed', 'removed'].indexOf(value.kind) < 0 ||
      (value.previous !== null && (!validateSummary(value.previous) || value.previous.id !== value.id)) ||
      (value.current !== null && (!validateSummary(value.current) || value.current.id !== value.id))) return false;
  if (value.kind === 'added') return value.previous === null && value.current !== null;
  if (value.kind === 'removed') return value.previous !== null && value.current === null;
  return value.previous !== null && value.current !== null &&
    jsonHash(value.previous) !== jsonHash(value.current);
}
function validateRecord(value) {
  var expectedKeys = ['schemaVersion', 'generationId', 'generationRevision', 'sourceFingerprint',
    'artifactHash', 'createdAt', 'syncJobId', 'previousGenerationId', 'entities', 'changes'];
  if (!exact(value, expectedKeys)) return false;
  if (value.schemaVersion !== 2 ||
      !HASH_RE.test(String(value.sourceFingerprint || '')) ||
      !HASH_RE.test(String(value.artifactHash || '')) ||
      value.artifactHash !== value.generationRevision ||
      !GENERATION_RE.test(String(value.generationId || '')) ||
      !HASH_RE.test(String(value.generationRevision || '')) || !iso(value.createdAt) ||
      (value.syncJobId !== null && !/^fsj-[a-f0-9]{32}$/.test(String(value.syncJobId || ''))) ||
      (value.previousGenerationId !== null && !GENERATION_RE.test(String(value.previousGenerationId || ''))) ||
      value.previousGenerationId === value.generationId ||
      !value.entities || typeof value.entities !== 'object' || Array.isArray(value.entities) ||
      Object.keys(value.entities).length > 10000 || !Array.isArray(value.changes) ||
      value.changes.length > 10000 || !value.changes.every(validateChange)) return false;
  var entityIds = Object.keys(value.entities);
  if (!entityIds.every(function (id) {
    var entity = value.entities[id];
    return ENTITY_RE.test(id) && exact(entity, ['hash', 'projection']) &&
      HASH_RE.test(String(entity.hash || '')) && validateSummary(entity.projection) &&
      entity.projection.id === id && jsonHash(entity.projection) === entity.hash;
  })) return false;
  var seenChanges = Object.create(null);
  if (!value.changes.every(function (change) {
    if (seenChanges[change.id]) return false;
    seenChanges[change.id] = 1;
    var entity = value.entities[change.id];
    if (change.current === null) {
      return !Object.prototype.hasOwnProperty.call(value.entities, change.id);
    }
    return !!entity && entity.hash === jsonHash(change.current);
  })) return false;
  if (value.previousGenerationId === null) {
    return value.changes.length === entityIds.length &&
      value.changes.every(function (change) { return change.kind === 'added'; });
  }
  return true;
}
function initialIndex() { return { schemaVersion: 1, generations: [] }; }
function validateIndex(value) {
  if (!exact(value, ['schemaVersion', 'generations']) || value.schemaVersion !== 1 ||
      !Array.isArray(value.generations) || value.generations.length > RETENTION) return false;
  var seen = Object.create(null);
  return value.generations.every(function (row) {
    if (!exact(row, ['generationId', 'generationRevision', 'createdAt', 'syncJobId', 'changeCount']) ||
        seen[row.generationId] || !GENERATION_RE.test(String(row.generationId || '')) ||
        !HASH_RE.test(String(row.generationRevision || '')) || !iso(row.createdAt) ||
        (row.syncJobId !== null && !/^fsj-[a-f0-9]{32}$/.test(String(row.syncJobId || ''))) ||
        !Number.isSafeInteger(row.changeCount) || row.changeCount < 0 || row.changeCount > 10000) return false;
    seen[row.generationId] = 1; return true;
  });
}
function readIndex() {
  try {
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, DIR, INDEX_FILE);
    if (inspected && inspected.status === 'missing') {
      var parent = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(DIR), DIR);
      if (parent && parent.status === 'missing') return initialIndex();
      if (!parent || parent.status !== 'present' || !parent.stat || !parent.stat.isDirectory() ||
          parent.stat.isSymbolicLink()) return null;
      var unindexed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, DIR, RETENTION * 20);
      if (!unindexed.ok || unindexed.names.length > 1 ||
          unindexed.names.some(function (name) { return !/^gen-[a-f0-9]{32}\.json$/.test(name); })) return null;
      // A single record without index.json is the only valid crash residue:
      // recordCurrent writes the record first, then the index. Returning an
      // empty index lets the existing-record branch validate and recover it.
      return initialIndex();
    }
    if (!inspected || inspected.status !== 'present' || !inspected.stat || !inspected.stat.isFile() ||
        inspected.stat.isSymbolicLink() || String(inspected.stat.nlink) !== '1') return null;
    var bytes = safeBytes(INDEX_FILE, INDEX_MAX), value = bytes && JSON.parse(bytes.toString('utf8'));
    if (!validateIndex(value)) return null;
    var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, DIR, RETENTION * 20);
    if (!listed.ok || listed.names.some(function (name) {
      return name !== 'index.json' && !/^gen-[a-f0-9]{32}\.json$/.test(name);
    })) return null;
    return value;
  } catch (error) { return null; }
}
function read(generationId) {
  try {
    var bytes = safeBytes(recordFile(generationId), RECORD_MAX);
    if (!bytes) return null;
    var value = JSON.parse(bytes.toString('utf8'));
    return validateRecord(value) ? value : null;
  } catch (error) { return null; }
}
function indexedRecord(row) {
  if (!row || typeof row !== 'object') return null;
  var record = read(row.generationId);
  return record && record.generationRevision === row.generationRevision &&
    record.createdAt === row.createdAt && record.syncJobId === row.syncJobId &&
    record.changes.length === row.changeCount ? record : null;
}
function serialize(fn) {
  var next = writeQueue.then(fn, fn);
  writeQueue = next.catch(function () {});
  return next;
}
function removeRecords(rows) {
  rows.forEach(function (row) {
    if (!fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, DIR, recordFile(row.generationId), { allowMissing: true })) {
      throw new Error('design-history-retention-cleanup-failed');
    }
  });
}
function cleanupUnindexedRecords(index) {
  var indexed = Object.create(null);
  index.generations.forEach(function (row) { indexed[row.generationId] = 1; });
  var listed = fileGuards.boundedDirectoryNamesUnder(paths.PROJECT_ROOT, DIR, RETENTION * 20);
  if (!listed.ok) throw new Error('design-history-directory-invalid');
  listed.names.forEach(function (name) {
    var match = /^(gen-[a-f0-9]{32})\.json$/.exec(name);
    if (!match || indexed[match[1]]) return;
    if (!read(match[1])) throw new Error('design-history-unindexed-record-invalid');
    if (!fileGuards.unlinkRegularFileUnder(paths.PROJECT_ROOT, DIR, recordFile(match[1]), { allowMissing: true })) {
      throw new Error('design-history-retention-cleanup-failed');
    }
  });
}
function buildRecord(projection, previous) {
  var entities = projection.entities, prior = previous ? previous.entities : {};
  var ids = Object.keys(entities).concat(Object.keys(prior)).filter(function (id, index, list) {
    return list.indexOf(id) === index;
  }).sort();
  var changes = [];
  ids.forEach(function (id) {
    var before = prior[id] || null, after = entities[id] || null;
    if (!before && after) changes.push({ id: id, kind: 'added', previous: null, current: after.projection });
    else if (before && !after) changes.push({ id: id, kind: 'removed', previous: before.projection, current: null });
    else if (before.hash !== after.hash) changes.push({
      id: id, kind: 'changed', previous: before.projection, current: after.projection
    });
  });
  return {
    schemaVersion: 2,
    generationId: projection.committedGenerationId,
    generationRevision: projection.generationRevision,
    sourceFingerprint: projection.designManifest.figmaArtifactInputFingerprint,
    artifactHash: projection.generationRevision,
    createdAt: projection.createdAt,
    syncJobId: projection.syncJobId,
    previousGenerationId: previous ? previous.generationId : null,
    entities: entities,
    changes: changes
  };
}
function recordCurrent() {
  return serialize(function () {
    var projection = catalog.historyProjection();
    if (!projection.ok || projection.generationMode !== 'generation') {
      return { ok: false, reason: projection.error || 'generation-not-committed' };
    }
    var index = readIndex();
    if (!index) throw new Error('design-history-index-invalid');
    var indexed = index.generations.find(function (row) {
      return row.generationId === projection.committedGenerationId;
    });
    var existing = read(projection.committedGenerationId);
    if (existing && existing.generationRevision === projection.generationRevision) {
      if (indexed) {
        if (indexed.generationRevision !== existing.generationRevision ||
            indexed.createdAt !== existing.createdAt || indexed.syncJobId !== existing.syncJobId ||
            indexed.changeCount !== existing.changes.length) {
          throw new Error('design-history-index-record-mismatch');
        }
        cleanupUnindexedRecords(index);
        return { ok: true, recorded: false, record: existing };
      }
      var expectedPrevious = index.generations.length ? index.generations[0].generationId : null;
      if (existing.previousGenerationId !== expectedPrevious) {
        throw new Error('design-history-orphan-previous-invalid');
      }
      index.generations.unshift({
        generationId: existing.generationId, generationRevision: existing.generationRevision,
        createdAt: existing.createdAt, syncJobId: existing.syncJobId, changeCount: existing.changes.length
      });
      var recoveredRemoved = index.generations.slice(RETENTION);
      index.generations = index.generations.slice(0, RETENTION);
      atomicWrite(INDEX_FILE, Buffer.from(JSON.stringify(index, null, 2) + '\n'), INDEX_MAX);
      removeRecords(recoveredRemoved);
      cleanupUnindexedRecords(index);
      return { ok: true, recorded: true, recovered: true, record: existing };
    }
    if (existing) throw new Error('design-history-generation-revision-conflict');
    var previous = index.generations.length ? indexedRecord(index.generations[0]) : null;
    if (index.generations.length && !previous) throw new Error('design-history-previous-invalid');
    var record = buildRecord(projection, previous);
    if (!validateRecord(record)) throw new Error('design-history-record-invalid');
    atomicWrite(recordFile(record.generationId), Buffer.from(JSON.stringify(record, null, 2) + '\n'), RECORD_MAX);
    index.generations.unshift({
      generationId: record.generationId, generationRevision: record.generationRevision,
      createdAt: record.createdAt, syncJobId: record.syncJobId, changeCount: record.changes.length
    });
    var removed = index.generations.slice(RETENTION);
    index.generations = index.generations.slice(0, RETENTION);
    atomicWrite(INDEX_FILE, Buffer.from(JSON.stringify(index, null, 2) + '\n'), INDEX_MAX);
    removeRecords(removed);
    cleanupUnindexedRecords(index);
    return { ok: true, recorded: true, record: record };
  });
}
function latest() {
  var index = readIndex();
  if (!index || !index.generations.length) return null;
  return indexedRecord(index.generations[0]);
}
function forEntity(id) {
  var index = readIndex();
  if (!index) return { available: false, reason: 'history-invalid', items: [] };
  if (!index.generations.length) return { available: false, reason: 'history-starts-after-update', items: [] };
  var items = [];
  for (var i = 0; i < index.generations.length && items.length < 100; i++) {
    var record = indexedRecord(index.generations[i]);
    if (!record) return { available: false, reason: 'history-invalid', items: [] };
    record.changes.filter(function (change) { return change.id === id; }).forEach(function (change) {
      items.push({
        generationId: record.generationId, syncJobId: record.syncJobId, timestamp: record.createdAt,
        sourceFingerprint: record.sourceFingerprint,
        artifactHash: record.artifactHash,
        kind: change.kind,
        previousHash: change.previous ? jsonHash(change.previous) : null,
        currentHash: change.current ? jsonHash(change.current) : null,
        previous: change.previous, current: change.current
      });
    });
  }
  return { available: true, items: items };
}
function latestChanges(expectedGenerationId) {
  var value = latest();
  if (expectedGenerationId && (!value || value.generationId !== expectedGenerationId)) return [];
  return value ? value.changes.map(function (change) {
    return {
      id: change.id, kind: change.kind, timestamp: value.createdAt,
      generationId: value.generationId, syncJobId: value.syncJobId,
      entity: change.current || change.previous
    };
  }) : [];
}
function changedIds(expectedGenerationId) {
  return latestChanges(expectedGenerationId).reduce(function (out, row) { out[row.id] = 1; return out; }, Object.create(null));
}
function status() {
  var index = readIndex();
  if (!index) return { available: false, reason: 'history-invalid', retained: 0 };
  if (index.generations.length) {
    var latestRecord = indexedRecord(index.generations[0]);
    if (!latestRecord) {
      return { available: false, reason: 'history-invalid', retained: 0 };
    }
  }
  return {
    available: index.generations.length > 0,
    reason: index.generations.length ? null : 'history-starts-after-update',
    retained: index.generations.length,
    latestGenerationId: index.generations.length ? index.generations[0].generationId : null
  };
}

module.exports = {
  DIR: DIR,
  INDEX_FILE: INDEX_FILE,
  RETENTION: RETENTION,
  RECORD_MAX: RECORD_MAX,
  recordCurrent: recordCurrent,
  latest: latest,
  forEntity: forEntity,
  latestChanges: latestChanges,
  changedIds: changedIds,
  status: status,
  _readIndexForTests: readIndex,
  _test: {
    validateSummary: validateSummary
  }
};
