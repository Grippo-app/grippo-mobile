'use strict';

// CAS-guarded mutations of the observed-token mapping registry. Every
// observed identity and project target is re-resolved against one committed
// generation before the project-owned registry is atomically replaced.

var crypto = require('crypto');
var path = require('path');
var pathToFileURL = require('url').pathToFileURL;
var paths = require('./paths');
var fileGuards = require('./file-guards');
var finalizations = require('./finalizations');
var generation = require('./figma-generation');
var tokenState = require('./design-token-state');
var mappingMutation = require('./mapping-mutation');

var MAPPING_FILE = tokenState.MAPPING_FILE;
var MAPPING_MAX = 8 * 1024 * 1024;
var OPERATIONS_MAX = 20;
var MAPPING_ID_RE = /^map-[a-f0-9]{24}$/;
var DISPOSITION_ID_RE = /^dsp-[a-f0-9]{24}$/;
var TOKEN_KEY_RE = /^otk:sha256:[a-f0-9]{64}$/;
var ACTOR = 'owner';
var CONTEXT_KEYS = ['theme', 'locale', 'platform', 'state'];
var RELATIONS = ['one-to-one', 'one-to-many', 'many-to-one', 'alias', 'transform'];
var DISPOSITION_KINDS = ['project-only-intentional', 'observed-only-intentional', 'ignored', 'unsupported', 'retirement-under-review'];

var modulesPromise = null;
function esm() {
  if (!modulesPromise) {
    var figmaDir = path.join(paths.ORCHESTRATOR_DIR, 'figma');
    modulesPromise = import(pathToFileURL(path.join(figmaDir, 'tokens', 'mapping-contract.mjs')).href)
      .then(function (contract) {
        var Ajv = require(path.join(figmaDir, 'node_modules', 'ajv'));
        var AjvCtor = Ajv.default || Ajv;
        var schema = JSON.parse(require('fs').readFileSync(path.join(figmaDir, 'schemas', 'token-mappings.schema.json'), 'utf8'));
        var validate = new AjvCtor({ allErrors: true, strict: false, validateFormats: false }).compile(schema);
        return { contract: contract, validate: validate };
      });
  }
  return modulesPromise;
}

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function now() { return new Date().toISOString(); }
function newId(prefix) { return prefix + '-' + crypto.randomBytes(12).toString('hex'); }
function typedFailure(status, code, detail, extra) {
  return Object.assign({ ok: false, status: status, error: code, detail: String(detail || '').slice(0, 500) }, extra || {});
}
function sameScope(left, right) {
  return !!left && !!right &&
    left.fileKeyFingerprint === right.fileKeyFingerprint &&
    left.branchKey === right.branchKey;
}
function contextMatches(context, selector) {
  return Object.keys(selector || {}).every(function (key) { return context[key] === selector[key]; });
}
function canonicalObservedKind(kind) {
  return ['color', 'number', 'string', 'boolean'].indexOf(kind) >= 0 ? kind : null;
}
function compatibleProjectKind(observedKind, projectKind) {
  return observedKind === projectKind || observedKind === 'number' && projectKind === 'dimension';
}

function readRegistry() {
  var directory = path.dirname(MAPPING_FILE);
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, MAPPING_FILE);
  if (inspected && inspected.status === 'missing') return { state: 'absent', document: null };
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, MAPPING_FILE, MAPPING_MAX);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return { state: 'invalid', document: null };
  try {
    return { state: 'present', document: JSON.parse(hit.bytes.toString('utf8')), bytes: hit.bytes };
  } catch (error) {
    return { state: 'invalid', document: null };
  }
}

function jsonRole(active, role) {
  var entry = active.manifest.artifacts.find(function (row) { return row.role === role; });
  if (!entry) return null;
  var bytes = generation.readEntry(entry);
  if (!bytes) throw new Error('TOKEN_GENERATION_ARTIFACT_INVALID');
  try { return JSON.parse(bytes.toString('utf8')); }
  catch (error) { throw new Error('TOKEN_GENERATION_ARTIFACT_INVALID'); }
}

function mutationContext() {
  var active = generation.current();
  if (!active.ok) {
    return typedFailure(409, 'TOKEN_GENERATION_RESYNC_REQUIRED',
      active.error || 'the committed design generation is invalid');
  }
  if (active.mode !== 'generation') {
    return typedFailure(409, 'TOKEN_GENERATION_RESYNC_REQUIRED',
      'no committed observed token generation');
  }
  var catalog, sourceIndex, comparison, index, inventories = {};
  try {
    catalog = jsonRole(active, 'observed-token-catalog');
    sourceIndex = jsonRole(active, 'observed-token-source-index');
    comparison = jsonRole(active, 'token-comparison');
    index = jsonRole(active, 'project-token-analysis-index');
    if (index && Array.isArray(index.adapters)) {
      index.adapters.forEach(function (row) {
        var inventory = jsonRole(active, row.role);
        if (inventory) inventories[row.adapterId] = inventory;
      });
    }
  } catch (error) {
    return typedFailure(409, 'TOKEN_GENERATION_RESYNC_REQUIRED',
      'an immutable token artifact is missing or corrupt');
  }
  if (!catalog || !sourceIndex || !sameScope(catalog.scope, sourceIndex.scope)) {
    return typedFailure(409, 'TOKEN_GENERATION_RESYNC_REQUIRED', 'sync a complete observed token domain before editing mappings');
  }
  return {
    ok: true,
    active: active,
    catalog: catalog,
    sourceIndex: sourceIndex,
    comparison: comparison,
    index: index,
    inventories: inventories
  };
}

function observedByKey(catalog, observedTokenKey) {
  return catalog.tokens.find(function (row) { return row.observedTokenKey === observedTokenKey; }) || null;
}

function observedKindFor(token, selector) {
  if (!token || token.presenceStatus === 'not-observed') return null;
  var kinds = new Set();
  token.coordinates.filter(function (coordinate) {
    return coordinate.status === 'consistent' && contextMatches(coordinate.context, selector);
  }).forEach(function (coordinate) {
    coordinate.values.forEach(function (value) {
      var kind = canonicalObservedKind(value.kind);
      if (kind) kinds.add(kind);
    });
  });
  return kinds.size === 1 ? kinds.values().next().value : null;
}

function get() {
  return esm().then(function (modules) {
    var read = readRegistry();
    var signals = tokenState.readTokenSignals();
    if (read.state === 'invalid') {
      return typedFailure(409, 'TOKEN_MAPPING_INVALID', 'token-mappings.json is unreadable; restore or replace it explicitly');
    }
    var active = generation.current();
    if (!active.ok) {
      return typedFailure(409, 'TOKEN_GENERATION_RESYNC_REQUIRED',
        active.error || 'the committed design generation is invalid');
    }
    var catalog;
    try {
      catalog = active.ok && active.mode === 'generation'
        ? jsonRole(active, 'observed-token-catalog') : null;
    } catch (error) {
      return typedFailure(409, 'TOKEN_GENERATION_RESYNC_REQUIRED',
        'an immutable token artifact is missing or corrupt');
    }
    var cas = active.ok && active.mode === 'generation'
      ? mappingMutation.casFromContext(active, 'project-token-analysis-index')
      : { designGenerationRevision: null, projectInventoryRevision: null };
    var scope = catalog && catalog.scope || null;
    if (read.state === 'absent' && !scope) {
      return typedFailure(409, 'TOKEN_GENERATION_RESYNC_REQUIRED',
        'sync a complete observed token domain before creating mappings');
    }
    var document = read.state === 'absent'
      ? modules.contract.emptyMappingRegistry(scope)
      : read.document;
    if (read.state === 'present') {
      if (!modules.validate(document)) return typedFailure(409, 'TOKEN_MAPPING_INVALID', 'token-mappings.json fails its schema');
      var semantic = modules.contract.mappingRegistrySemanticError(document);
      if (semantic) return typedFailure(409, 'TOKEN_MAPPING_INVALID', semantic);
    }
    return {
      ok: true,
      status: 200,
      present: read.state === 'present',
      revision: document.revision,
      designGenerationRevision: cas.designGenerationRevision,
      projectInventoryRevision: cas.projectInventoryRevision,
      scope: document.scope,
      currentObservedScope: scope,
      scopeMatchesObserved: !scope || sameScope(document.scope, scope),
      mappingState: signals.mappingState,
      mappings: document.mappings,
      dispositions: document.dispositions
    };
  });
}

function validSelector(selector) {
  return !!selector && typeof selector === 'object' && !Array.isArray(selector) &&
    Object.keys(selector).every(function (key) {
      return CONTEXT_KEYS.indexOf(key) >= 0 && typeof selector[key] === 'string' && selector[key] && selector[key].length <= 64;
    });
}

function validateOperationShape(operation) {
  if (!operation || typeof operation !== 'object' || Array.isArray(operation)) return 'operation must be an object';
  if (operation.op === 'upsert-mapping') {
    var allowed = ['op', 'mappingId', 'observedTokenKey', 'contextSelector', 'adapterId', 'projectTokenIds', 'relation', 'transform'];
    if (Object.keys(operation).some(function (key) { return allowed.indexOf(key) < 0; })) return 'upsert-mapping carries an unknown key';
    if (operation.mappingId !== undefined && !MAPPING_ID_RE.test(String(operation.mappingId))) return 'mappingId malformed';
    if (!TOKEN_KEY_RE.test(String(operation.observedTokenKey || '')) || !validSelector(operation.contextSelector) ||
        typeof operation.adapterId !== 'string' || !Array.isArray(operation.projectTokenIds) ||
        !operation.projectTokenIds.length || operation.projectTokenIds.length > 8 ||
        RELATIONS.indexOf(operation.relation) < 0) return 'upsert-mapping is missing required v2 fields';
    if (operation.relation === 'transform' && (!operation.transform || typeof operation.transform !== 'object')) {
      return 'transform relation requires transform';
    }
    if (operation.relation !== 'transform' && operation.transform !== undefined) return 'transform is allowed only for transform relation';
    return null;
  }
  if (operation.op === 'retire-mapping') {
    if (!exact(operation, ['op', 'mappingId', 'reason'])) return 'retire-mapping requires exactly mappingId and reason';
    if (!MAPPING_ID_RE.test(String(operation.mappingId))) return 'mappingId malformed';
    if (typeof operation.reason !== 'string' || !operation.reason.trim() || operation.reason.length > 500) return 'reason must be bounded';
    return null;
  }
  if (operation.op === 'add-disposition') {
    var dispositionAllowed = ['op', 'side', 'observedTokenKey', 'projectTokenId', 'adapterId', 'kind', 'reason', 'reviewAt'];
    if (Object.keys(operation).some(function (key) { return dispositionAllowed.indexOf(key) < 0; })) return 'add-disposition carries an unknown key';
    if (operation.side !== 'observed' && operation.side !== 'project') return 'side must be observed or project';
    if (DISPOSITION_KINDS.indexOf(operation.kind) < 0 ||
        typeof operation.reason !== 'string' || !operation.reason.trim() || operation.reason.length > 500) {
      return 'add-disposition requires a current kind and bounded reason';
    }
    return null;
  }
  if (operation.op === 'remove-disposition') {
    if (!exact(operation, ['op', 'dispositionId']) || !DISPOSITION_ID_RE.test(String(operation.dispositionId))) {
      return 'remove-disposition requires exactly a valid dispositionId';
    }
    return null;
  }
  if (operation.op === 'onboard-fresh') {
    if (!exact(operation, ['op'])) return 'onboard-fresh takes no fields';
    return null;
  }
  return 'unknown operation ' + JSON.stringify(operation.op);
}

function applyOperation(document, operation, context, stamp) {
  if (operation.op === 'onboard-fresh') {
    document.scope = context.catalog.scope;
    document.mappings = [];
    document.dispositions = [];
    return null;
  }
  if (!sameScope(document.scope, context.catalog.scope)) {
    return typedFailure(409, 'TOKEN_MAPPING_SCOPE_CHANGED',
      'the registry is bound to another observed-token scope; run onboard-fresh explicitly');
  }
  if (operation.op === 'upsert-mapping') {
    var observed = observedByKey(context.catalog, operation.observedTokenKey);
    if (!observed) return typedFailure(409, 'TOKEN_FINDING_STALE', 'observed token is not in the active catalog');
    var expectedKind = observedKindFor(observed, operation.contextSelector);
    if (!expectedKind) {
      return typedFailure(409, 'TOKEN_BINDING_TARGET_INCOMPATIBLE',
        'the selected observed context is absent, conflicting, unsupported, or kind-ambiguous');
    }
    var inventory = context.inventories[operation.adapterId];
    if (!inventory || !context.index || !context.index.adapters.some(function (row) { return row.adapterId === operation.adapterId; })) {
      return typedFailure(409, 'PROJECT_TOKEN_INVENTORY_INCOMPLETE', 'compare the configured adapter before authoring mappings');
    }
    var byId = new Map(inventory.tokens.map(function (token) { return [token.projectTokenId, token]; }));
    var missing = operation.projectTokenIds.find(function (id) {
      return typeof id !== 'string' || id.indexOf(operation.adapterId + ':') !== 0 || !byId.has(id);
    });
    if (missing) return typedFailure(409, 'TOKEN_MAPPING_TARGET_MISSING', 'project token ' + missing + ' is not in the published adapter inventory');
    var incompatible = operation.projectTokenIds.find(function (id) {
      return !compatibleProjectKind(expectedKind, byId.get(id).kind);
    });
    if (incompatible) return typedFailure(409, 'TOKEN_BINDING_TARGET_INCOMPATIBLE', 'project token ' + incompatible + ' has another kind');
    var mapping = {
      mappingId: operation.mappingId || newId('map'),
      observedTokenKey: operation.observedTokenKey,
      contextSelector: operation.contextSelector,
      adapterId: operation.adapterId,
      projectTokenIds: operation.projectTokenIds.slice(),
      expectedKind: expectedKind,
      relation: operation.relation,
      state: 'active',
      provenance: { kind: 'manual-review', actor: ACTOR, at: stamp }
    };
    if (operation.transform !== undefined) mapping.transform = operation.transform;
    if (operation.mappingId) {
      var position = document.mappings.findIndex(function (row) { return row.mappingId === operation.mappingId; });
      if (position < 0) return typedFailure(409, 'TOKEN_MAPPING_CONFLICT', 'mapping does not exist');
      document.mappings[position] = mapping;
    } else {
      document.mappings.push(mapping);
    }
    return null;
  }
  if (operation.op === 'retire-mapping') {
    var target = document.mappings.find(function (row) { return row.mappingId === operation.mappingId; });
    if (!target || target.state === 'retired') return typedFailure(409, 'TOKEN_MAPPING_CONFLICT', 'mapping is absent or already retired');
    target.state = 'retired';
    target.retirement = { reason: operation.reason.trim(), actor: ACTOR, at: stamp };
    return null;
  }
  if (operation.op === 'add-disposition') {
    var disposition = {
      dispositionId: newId('dsp'),
      target: { side: operation.side },
      kind: operation.kind,
      reason: operation.reason.trim(),
      owner: ACTOR,
      createdAt: stamp,
      reviewPolicy: operation.reviewAt ? 'manual-date' : 'on-change'
    };
    if (operation.side === 'observed') {
      if (!TOKEN_KEY_RE.test(String(operation.observedTokenKey || '')) ||
          !observedByKey(context.catalog, operation.observedTokenKey)) {
        return typedFailure(409, 'TOKEN_FINDING_STALE', 'observed token is not in the active catalog');
      }
      disposition.target.observedTokenKey = operation.observedTokenKey;
    } else {
      var projectInventory = context.inventories[operation.adapterId];
      if (!projectInventory || !projectInventory.tokens.some(function (token) {
        return token.projectTokenId === operation.projectTokenId;
      })) return typedFailure(409, 'TOKEN_FINDING_STALE', 'project token is not in the published analysis');
      disposition.target.adapterId = operation.adapterId;
      disposition.target.projectTokenId = operation.projectTokenId;
    }
    if (operation.reviewAt) disposition.reviewAt = operation.reviewAt;
    document.dispositions.push(disposition);
    return null;
  }
  if (operation.op === 'remove-disposition') {
    var index = document.dispositions.findIndex(function (row) { return row.dispositionId === operation.dispositionId; });
    if (index < 0) return typedFailure(409, 'TOKEN_MAPPING_CONFLICT', 'disposition does not exist');
    document.dispositions.splice(index, 1);
    return null;
  }
  return typedFailure(400, 'TOKEN_MAPPING_INVALID', 'unknown operation');
}

function mutate(request) {
  var envelope = mappingMutation.validateEnvelope(request, exact);
  if (!envelope || !Array.isArray(request.operations) || !request.operations.length || request.operations.length > OPERATIONS_MAX) {
    return Promise.resolve(typedFailure(400, 'bad-token-mapping-request', 'exact CAS revisions and 1..20 operations are required'));
  }
  for (var i = 0; i < request.operations.length; i++) {
    var shapeError = validateOperationShape(request.operations[i]);
    if (shapeError) return Promise.resolve(typedFailure(400, 'bad-token-mapping-request', shapeError));
  }
  return esm().then(function (modules) {
    var lease = finalizations.beginMutation({ kind: 'token-mappings', key: 'token-mappings', pendingChild: false, requireSoleWriter: true });
    if (!lease.ok) return typedFailure(409, 'token-mapping-writer-busy', lease.reason || 'another writer holds the workspace');
    var settle = function (result) { finalizations.endMutation(lease.handle); return result; };
    try {
      var read = readRegistry();
      if (read.state === 'invalid') return settle(typedFailure(409, 'TOKEN_MAPPING_INVALID', 'token-mappings.json is unreadable'));
      if (read.state === 'present') {
        if (!modules.validate(read.document)) return settle(typedFailure(409, 'TOKEN_MAPPING_INVALID', 'token-mappings.json fails its schema'));
        var existingError = modules.contract.mappingRegistrySemanticError(read.document);
        if (existingError) return settle(typedFailure(409, 'TOKEN_MAPPING_INVALID', existingError));
        var prior = mappingMutation.replay(read.document, request.operationId, envelope.requestHash);
        if (prior && prior.conflict) return settle(typedFailure(409, 'TOKEN_MAPPING_OPERATION_CONFLICT', 'operationId was reused'));
        if (prior) return settle(prior);
      }
      var context = mutationContext();
      if (!context.ok) return settle(context);
      var needsComparison = request.operations.some(function (operation) {
        return operation.op === 'upsert-mapping' || operation.op === 'add-disposition';
      });
      if (needsComparison && (!context.comparison || context.comparison.complete !== true ||
          context.comparison.operationalState !== 'current' || !context.comparison.inputs ||
          context.comparison.inputs.sourceFreshness !== 'current' ||
          request.expectedComparisonSemanticHash !== context.comparison.semanticHash)) {
        return settle(typedFailure(409, 'TOKEN_FINDING_STALE',
          'the reviewed comparison is absent, read-only, or no longer current'));
      }
      var document = read.state === 'absent'
        ? modules.contract.emptyMappingRegistry(context.catalog.scope)
        : JSON.parse(JSON.stringify(read.document));
      var cas = mappingMutation.casFromContext(context.active, 'project-token-analysis-index');
      if (request.expectedDesignGenerationRevision !== cas.designGenerationRevision) {
        return settle(typedFailure(409, 'TOKEN_DESIGN_GENERATION_CONFLICT', 'observed token generation moved'));
      }
      if (request.expectedProjectInventoryRevision !== cas.projectInventoryRevision) {
        return settle(typedFailure(409, 'TOKEN_PROJECT_INVENTORY_CONFLICT', 'project inventory revision moved'));
      }
      if (document.revision !== request.expectedMappingRevision) {
        return settle(typedFailure(409, 'TOKEN_MAPPING_REVISION_CONFLICT', 'registry revision moved', { currentRevision: document.revision }));
      }
      var stamp = now();
      for (var index = 0; index < request.operations.length; index++) {
        var failure = applyOperation(document, request.operations[index], context, stamp);
        if (failure) return settle(failure);
      }
      document.revision = request.expectedMappingRevision + 1;
      mappingMutation.appendReceipt(document, request.operationId, envelope.requestHash, stamp);
      var latestReceipt = document.operationReceipts[document.operationReceipts.length - 1];
      delete latestReceipt.mappings;
      delete latestReceipt.dispositions;
      if (!modules.validate(document)) {
        var first = (modules.validate.errors || [])[0];
        return settle(typedFailure(400, 'TOKEN_MAPPING_INVALID',
          'mutated registry fails its schema: ' + ((first && (first.instancePath || '/') + ' ' + first.message) || '')));
      }
      var semanticError = modules.contract.mappingRegistrySemanticError(document);
      if (semanticError) return settle(typedFailure(400, 'TOKEN_MAPPING_INVALID', semanticError));
      var bytes = Buffer.from(JSON.stringify(document, null, 2) + '\n');
      var write = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, path.dirname(MAPPING_FILE), MAPPING_FILE, bytes,
        { create: true, directoryMode: 0o755, mode: 0o644, maxBytes: MAPPING_MAX });
      if (!write.ok) return settle(typedFailure(500, 'token-mapping-write-failed', 'atomic mapping publication failed'));
      var verify = readRegistry();
      if (verify.state !== 'present' || verify.document.revision !== document.revision) {
        return settle(typedFailure(500, 'TOKEN_MAPPING_PUBLICATION_RECOVERY_REQUIRED', 'post-write verification failed'));
      }
      var committed = settle({
        ok: true,
        status: 200,
        operationId: request.operationId,
        requestHash: envelope.requestHash,
        revision: document.revision,
        mappings: document.mappings.length,
        dispositions: document.dispositions.length,
        replayed: false
      });
      try { require('./design-token-compare').ensureFresh('mapping-mutation'); } catch (error) {}
      return committed;
    } catch (error) {
      return settle(typedFailure(500, 'token-mapping-write-failed', 'mapping mutation failed safely'));
    }
  });
}
function publicErrorCode() {
  return 'token-mapping-write-failed';
}

module.exports = {
  MAPPING_FILE: MAPPING_FILE,
  get: get,
  mutate: mutate,
  publicErrorCode: publicErrorCode
};
