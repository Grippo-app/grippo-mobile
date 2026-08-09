'use strict';

// CAS-guarded mutations of the project-owned component mapping registry
// (orchestrator/figma/component-mappings.json). The browser sends stable ids
// and an exact expected revision; every referenced identity is re-resolved
// server-side against the active generation's design component inventory and
// the published project component analysis before a byte is written.
// Suggestions never reach this module — confirmation is always an explicit
// operation, and no name/API/screenshot similarity ever confirms anything.

var crypto = require('crypto');
var path = require('path');
var pathToFileURL = require('url').pathToFileURL;
var paths = require('./paths');
var fileGuards = require('./file-guards');
var finalizations = require('./finalizations');
var generation = require('./figma-generation');
var componentState = require('./design-component-state');
var mappingMutation = require('./mapping-mutation');

var MAPPING_FILE = componentState.MAPPING_FILE;
var MAPPING_MAX = 8 * 1024 * 1024;
var OPERATIONS_MAX = 20;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var MAPPING_ID_RE = /^cmap-[a-f0-9]{24}$/;
var DISPOSITION_ID_RE = /^dcp-[a-f0-9]{24}$/;
var RELATIONS = ['direct', 'wrapper', 'composite', 'shared-implementation', 'external'];
var DISPOSITION_KINDS = ['intentionally-project-only', 'intentionally-design-only', 'external', 'deprecated', 'unsupported-by-policy', 'ignored', 'deferred'];
var ACTOR = 'owner';

var modulesPromise = null;
function esm() {
  if (!modulesPromise) {
    // Code/helper resolution stays in the installed template (paths.js
    // doctrine): only project DATA follows ORCHESTRATOR_PROJECT_ROOT.
    var figmaDir = path.join(paths.ORCHESTRATOR_DIR, 'figma');
    modulesPromise = import(pathToFileURL(path.join(figmaDir, 'components', 'mapping-contract.mjs')).href)
      .then(function (contract) {
        var Ajv = require(path.join(figmaDir, 'node_modules', 'ajv'));
        var AjvCtor = Ajv.default || Ajv;
        var schema = JSON.parse(require('fs').readFileSync(path.join(figmaDir, 'schemas', 'component-mappings.schema.json'), 'utf8'));
        var validate = new AjvCtor({ allErrors: true, strict: false }).compile(schema);
        return { contract: contract, validate: validate };
      });
  }
  return modulesPromise;
}

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function isRecord(value) { return !!value && typeof value === 'object' && !Array.isArray(value); }
function now() { return new Date().toISOString(); }
function newId(prefix) { return prefix + '-' + crypto.randomBytes(12).toString('hex'); }
function typedFailure(status, code, detail, extra) {
  return Object.assign({ ok: false, status: status, error: code, detail: String(detail || '').slice(0, 500) }, extra || {});
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
  if (!bytes) throw new Error('COMPONENT_GENERATION_ARTIFACT_INVALID');
  try { return JSON.parse(bytes.toString('utf8')); }
  catch (error) { throw new Error('COMPONENT_GENERATION_ARTIFACT_INVALID'); }
}

// The mutation context: active design component inventory + latest published
// project component analysis (via the component comparison domain artifacts).
// All identity resolution happens against these exact immutable inputs.
function mutationContext() {
  var active = generation.current();
  if (!active.ok) {
    return typedFailure(409, 'COMPONENT_GENERATION_RESYNC_REQUIRED',
      active.error || 'the committed design generation is invalid');
  }
  if (active.mode !== 'generation') {
    return typedFailure(409, 'COMPONENT_DESIGN_SOURCE_NOT_SYNCED',
      'no committed component generation');
  }
  var design, comparison, index, inventories = {};
  try {
    design = jsonRole(active, 'design-component-inventory');
    comparison = jsonRole(active, 'component-comparison');
    index = jsonRole(active, 'project-component-analysis-index');
    if (index && Array.isArray(index.adapters)) {
      index.adapters.forEach(function (row) {
        var inventory = jsonRole(active, row.role);
        if (inventory) inventories[row.adapterId] = inventory;
      });
    }
  } catch (error) {
    return typedFailure(409, 'COMPONENT_GENERATION_RESYNC_REQUIRED',
      'an immutable component artifact is missing or corrupt');
  }
  if (!design) return typedFailure(409, 'COMPONENT_DESIGN_SOURCE_NOT_SYNCED', 'sync design components before editing mappings');
  return { ok: true, active: active, design: design, comparison: comparison, index: index, inventories: inventories };
}

function designComponentById(design, designComponentId) {
  var component = design.components.find(function (row) { return row.designComponentId === designComponentId; });
  if (component) return { component: component, unsupported: false };
  var unsupported = design.unsupportedComponents.find(function (row) { return row.designComponentId === designComponentId; });
  if (unsupported) return { component: unsupported, unsupported: true };
  return null;
}

function get() {
  return esm().then(function (modules) {
    var read = readRegistry();
    var signals = componentState.readComponentSignals();
    if (read.state === 'invalid') {
      return typedFailure(409, 'COMPONENT_MAPPING_INVALID', 'component-mappings.json is unreadable; restore or replace it explicitly');
    }
    var active = generation.current();
    if (!active.ok) {
      return typedFailure(409, 'COMPONENT_GENERATION_RESYNC_REQUIRED',
        active.error || 'the committed design generation is invalid');
    }
    var design;
    try {
      design = active.ok && active.mode === 'generation'
        ? jsonRole(active, 'design-component-inventory') : null;
    } catch (error) {
      return typedFailure(409, 'COMPONENT_GENERATION_RESYNC_REQUIRED',
        'an immutable component artifact is missing or corrupt');
    }
    var cas = active.ok && active.mode === 'generation'
      ? mappingMutation.casFromContext(active, 'project-component-analysis-index')
      : { designGenerationRevision: null, projectInventoryRevision: null };
    var scopeId = design ? design.scopeId : null;
    if (read.state === 'absent' && !scopeId) {
      return typedFailure(409, 'COMPONENT_DESIGN_SOURCE_NOT_SYNCED',
        'sync a complete component inventory before creating mappings');
    }
    var document = read.state === 'absent'
      ? modules.contract.emptyMappingRegistry(scopeId)
      : read.document;
    if (read.state === 'present') {
      if (modules.validate && !modules.validate(document)) {
        return typedFailure(409, 'COMPONENT_MAPPING_INVALID', 'component-mappings.json fails its schema');
      }
      var semantic = modules.contract.mappingRegistrySemanticError(document);
      if (semantic) return typedFailure(409, 'COMPONENT_MAPPING_INVALID', semantic);
    }
    return {
      ok: true,
      status: 200,
      present: read.state === 'present',
      revision: document.revision,
      designGenerationRevision: cas.designGenerationRevision,
      projectInventoryRevision: cas.projectInventoryRevision,
      designScopeId: document.designScopeId,
      currentDesignScopeId: scopeId,
      scopeMatchesDesign: !scopeId || document.designScopeId === scopeId,
      mappingState: signals.mappingState,
      mappings: document.mappings,
      dispositions: document.dispositions
    };
  });
}

function validImplementationShape(implementation) {
  if (!isRecord(implementation)) return 'implementation must be an object';
  var allowed = ['adapterId', 'relation', 'projectComponentIds', 'externalRef', 'externalPlatform', 'externalScopeFingerprint', 'required'];
  if (Object.keys(implementation).some(function (key) { return allowed.indexOf(key) < 0; })) return 'implementation carries an unknown key';
  if (typeof implementation.adapterId !== 'string' || RELATIONS.indexOf(implementation.relation) < 0 ||
      !Array.isArray(implementation.projectComponentIds) || implementation.projectComponentIds.length > 8 ||
      typeof implementation.required !== 'boolean') return 'implementation is missing required fields';
  if (implementation.externalRef !== undefined &&
      (typeof implementation.externalRef !== 'string' || !implementation.externalRef.trim() || implementation.externalRef.length > 300)) {
    return 'externalRef must be a bounded non-empty string';
  }
  if (implementation.externalPlatform !== undefined && !/^[a-z][a-z0-9-]{0,59}$/.test(String(implementation.externalPlatform))) {
    return 'externalPlatform must be an explicit platform identity';
  }
  if (implementation.externalScopeFingerprint !== undefined && !HASH_RE.test(String(implementation.externalScopeFingerprint))) {
    return 'externalScopeFingerprint must be an exact sha256 identity';
  }
  return null;
}

function validateOperationShape(operation) {
  if (!isRecord(operation)) return 'operation must be an object';
  var op = operation.op;
  if (op === 'upsert-mapping') {
    var keys = Object.keys(operation).filter(function (key) { return key !== 'op'; });
    var allowed = ['mappingId', 'designComponentId', 'implementations', 'propertyMappings', 'slotMappings', 'semantics'];
    if (keys.some(function (key) { return allowed.indexOf(key) < 0; })) return 'upsert-mapping carries an unknown key';
    if (operation.mappingId !== undefined && !MAPPING_ID_RE.test(String(operation.mappingId))) return 'mappingId malformed';
    if (typeof operation.designComponentId !== 'string' ||
        !Array.isArray(operation.implementations) || !operation.implementations.length || operation.implementations.length > 8) {
      return 'upsert-mapping is missing required fields';
    }
    for (var i = 0; i < operation.implementations.length; i++) {
      var problem = validImplementationShape(operation.implementations[i]);
      if (problem) return problem;
    }
    if (operation.propertyMappings !== undefined) {
      if (!Array.isArray(operation.propertyMappings) || operation.propertyMappings.length > 40) return 'propertyMappings must be a bounded array';
      for (var p = 0; p < operation.propertyMappings.length; p++) {
        var row = operation.propertyMappings[p];
        if (!isRecord(row) || typeof row.designPropertyId !== 'string' || typeof row.adapterId !== 'string' ||
            typeof row.projectPropertyId !== 'string') return 'property mapping rows require designPropertyId, adapterId, projectPropertyId';
        var rowAllowed = ['designPropertyId', 'adapterId', 'projectPropertyId', 'valueMap', 'ignoredValues', 'note'];
        if (Object.keys(row).some(function (key) { return rowAllowed.indexOf(key) < 0; })) return 'property mapping row carries an unknown key';
      }
    }
    if (operation.slotMappings !== undefined) {
      if (!Array.isArray(operation.slotMappings) || operation.slotMappings.length > 64) return 'slotMappings must be a bounded array';
      for (var s = 0; s < operation.slotMappings.length; s++) {
        var slotRow = operation.slotMappings[s];
        if (!isRecord(slotRow) || typeof slotRow.designSlotId !== 'string' || typeof slotRow.adapterId !== 'string') {
          return 'slot mapping rows require designSlotId and adapterId';
        }
        var slotAllowed = ['designSlotId', 'adapterId', 'projectSlotId', 'verification', 'role', 'ignoredReason'];
        if (Object.keys(slotRow).some(function (key) { return slotAllowed.indexOf(key) < 0; })) return 'slot mapping row carries an unknown key';
      }
    }
    return null;
  }
  if (op === 'retire-mapping') {
    if (!exact(operation, ['op', 'mappingId', 'reason'])) return 'retire-mapping requires exactly mappingId and reason';
    if (!MAPPING_ID_RE.test(String(operation.mappingId))) return 'mappingId malformed';
    if (typeof operation.reason !== 'string' || !operation.reason.trim() || operation.reason.length > 500) return 'reason must be a bounded non-empty string';
    return null;
  }
  if (op === 'set-render-class') {
    if (!exact(operation, ['op', 'mappingId', 'renderClass']) && !exact(operation, ['op', 'mappingId', 'renderClass', 'reason'])) {
      return 'set-render-class requires mappingId and renderClass (optional reason)';
    }
    if (!MAPPING_ID_RE.test(String(operation.mappingId))) return 'mappingId malformed';
    if (operation.renderClass !== null && operation.renderClass !== 'canvas' && operation.renderClass !== 'glass') {
      return 'renderClass must be canvas, glass, or null';
    }
    if (operation.reason !== undefined && (typeof operation.reason !== 'string' || !operation.reason.trim() || operation.reason.length > 500)) {
      return 'reason must be a bounded non-empty string';
    }
    return null;
  }
  if (op === 'add-disposition') {
    var dispositionKeys = Object.keys(operation).filter(function (key) { return key !== 'op'; });
    var dispositionAllowed = ['side', 'designComponentId', 'projectComponentId', 'adapterId', 'kind', 'reason', 'reviewAt', 'supersededBy'];
    if (dispositionKeys.some(function (key) { return dispositionAllowed.indexOf(key) < 0; })) return 'add-disposition carries an unknown key';
    if (operation.side !== 'design' && operation.side !== 'project') return 'side must be design or project';
    if (DISPOSITION_KINDS.indexOf(operation.kind) < 0) return 'unknown disposition kind';
    if (typeof operation.reason !== 'string' || !operation.reason.trim() || operation.reason.length > 500) {
      return 'add-disposition requires a bounded reason';
    }
    return null;
  }
  if (op === 'remove-disposition') {
    if (!exact(operation, ['op', 'dispositionId'])) return 'remove-disposition requires exactly dispositionId';
    if (!DISPOSITION_ID_RE.test(String(operation.dispositionId))) return 'dispositionId malformed';
    return null;
  }
  if (op === 'onboard-fresh') {
    if (!exact(operation, ['op'])) return 'onboard-fresh takes no fields';
    return null;
  }
  return 'unknown operation ' + JSON.stringify(op);
}

// Applies one operation to the draft document. Returns null or an error
// object. context carries design/index/inventories for identity resolution.
function applyOperation(document, operation, context, stamp) {
  if (operation.op === 'onboard-fresh') {
    document.designScopeId = context.design.scopeId;
    document.mappings = [];
    document.dispositions = [];
    return null;
  }
  if (document.designScopeId !== context.design.scopeId) {
    return typedFailure(409, 'COMPONENT_DESIGN_SCOPE_CHANGED',
      'the registry is bound to another design scope; run onboard-fresh explicitly before authoring mappings for the new scope');
  }
  if (operation.op === 'upsert-mapping') {
    var found = designComponentById(context.design, operation.designComponentId);
    if (!found) return typedFailure(409, 'COMPONENT_FINDING_STALE', 'design component ' + operation.designComponentId + ' is not in the active inventory');
    if (found.unsupported) return typedFailure(409, 'COMPONENT_PROPERTY_UNSUPPORTED', 'design component ' + operation.designComponentId + ' is unsupported and cannot be mapped');
    var implementations = [];
    for (var i = 0; i < operation.implementations.length; i++) {
      var requested = operation.implementations[i];
      if (requested.relation === 'external') {
        if (requested.projectComponentIds.length) {
          return typedFailure(400, 'COMPONENT_MAPPING_INVALID', 'external implementations carry no project components');
        }
        if (!requested.externalRef) return typedFailure(400, 'COMPONENT_MAPPING_INVALID', 'external implementations require externalRef');
        // External implementations still need exact owner-reviewed scope and
        // platform identities. A published adapter row is authoritative when
        // present; otherwise both values must be explicit in the mutation —
        // no synthetic zero hash or adapter-name-as-platform fallback.
        var externalIndexRow = context.index && context.index.adapters.find(function (row) { return row.adapterId === requested.adapterId; });
        if (!externalIndexRow && (!requested.externalPlatform || !requested.externalScopeFingerprint)) {
          return typedFailure(400, 'COMPONENT_MAPPING_INVALID',
            'external implementations outside published analysis require externalPlatform and externalScopeFingerprint');
        }
        if (externalIndexRow && (requested.externalPlatform && requested.externalPlatform !== externalIndexRow.platform ||
            requested.externalScopeFingerprint && requested.externalScopeFingerprint !== externalIndexRow.scopeFingerprint)) {
          return typedFailure(409, 'COMPONENT_FINDING_STALE', 'external implementation identity no longer matches the published adapter scope');
        }
        implementations.push({
          adapterId: requested.adapterId,
          platform: externalIndexRow ? externalIndexRow.platform : requested.externalPlatform,
          projectScopeFingerprint: externalIndexRow ? externalIndexRow.scopeFingerprint : requested.externalScopeFingerprint,
          relation: 'external',
          projectComponentIds: [],
          externalRef: requested.externalRef,
          required: requested.required
        });
        continue;
      }
      var inventory = context.inventories[requested.adapterId];
      var indexRow = context.index && context.index.adapters.find(function (row) { return row.adapterId === requested.adapterId; });
      if (!inventory || !indexRow) {
        return typedFailure(409, 'COMPONENT_PROJECT_INVENTORY_INCOMPLETE', 'adapter ' + requested.adapterId + ' has no published analysis; compare with the project first');
      }
      var projectIds = new Set(inventory.components.map(function (component) { return component.projectComponentId; }));
      var missing = requested.projectComponentIds.filter(function (id) { return !projectIds.has(id); });
      if (missing.length) {
        return typedFailure(409, 'COMPONENT_MAPPING_TARGET_MISSING', 'project component ' + missing[0] + ' is not in the published analysis');
      }
      implementations.push({
        adapterId: requested.adapterId,
        platform: indexRow.platform,
        projectScopeFingerprint: indexRow.scopeFingerprint,
        relation: requested.relation,
        projectComponentIds: requested.projectComponentIds.slice(),
        required: requested.required
      });
    }
    // Property/slot binding identities must exist on both sides.
    var propertyMappings = [];
    for (var p = 0; p < (operation.propertyMappings || []).length; p++) {
      var row = operation.propertyMappings[p];
      if (!found.component.properties.some(function (property) { return property.propertyId === row.designPropertyId; })) {
        return typedFailure(409, 'COMPONENT_FINDING_STALE', 'design property ' + row.designPropertyId + ' does not exist on ' + operation.designComponentId);
      }
      var propertyInventory = context.inventories[row.adapterId];
      var target = implementations.find(function (implementation) { return implementation.adapterId === row.adapterId; });
      if (!propertyInventory || !target) {
        return typedFailure(409, 'COMPONENT_MAPPING_INVALID', 'property mapping cites adapter ' + row.adapterId + ' without an implementation');
      }
      var owner = propertyInventory.components.find(function (component) {
        return target.projectComponentIds.indexOf(component.projectComponentId) >= 0 &&
          component.variantProperties.some(function (property) { return property.projectPropertyId === row.projectPropertyId; });
      });
      if (!owner) {
        return typedFailure(409, 'COMPONENT_MAPPING_TARGET_MISSING', 'project property ' + row.projectPropertyId + ' is not declared by the mapped implementation');
      }
      var cleanRow = { designPropertyId: row.designPropertyId, adapterId: row.adapterId, projectPropertyId: row.projectPropertyId };
      if (row.valueMap !== undefined) cleanRow.valueMap = row.valueMap;
      if (row.ignoredValues !== undefined) cleanRow.ignoredValues = row.ignoredValues;
      if (row.note !== undefined) cleanRow.note = row.note;
      propertyMappings.push(cleanRow);
    }
    var slotMappings = [];
    for (var s = 0; s < (operation.slotMappings || []).length; s++) {
      var slotRow = operation.slotMappings[s];
      if (!found.component.semanticSlots.some(function (slot) { return slot.slotId === slotRow.designSlotId; })) {
        return typedFailure(409, 'COMPONENT_FINDING_STALE', 'design slot ' + slotRow.designSlotId + ' does not exist on ' + operation.designComponentId);
      }
      var slotTarget = implementations.find(function (implementation) { return implementation.adapterId === slotRow.adapterId; });
      if (!slotTarget) {
        return typedFailure(409, 'COMPONENT_MAPPING_INVALID', 'slot mapping cites adapter ' + slotRow.adapterId + ' without an implementation');
      }
      if (slotRow.projectSlotId !== undefined) {
        var slotInventory = context.inventories[slotRow.adapterId];
        var slotOwner = slotInventory && slotInventory.components.find(function (component) {
          return slotTarget.projectComponentIds.indexOf(component.projectComponentId) >= 0 &&
            component.slots.some(function (slot) { return slot.slotId === slotRow.projectSlotId; });
        });
        if (!slotOwner) {
          return typedFailure(409, 'COMPONENT_MAPPING_TARGET_MISSING', 'project slot ' + slotRow.projectSlotId + ' is not declared by the mapped implementation');
        }
      }
      var cleanSlot = { designSlotId: slotRow.designSlotId, adapterId: slotRow.adapterId, verification: slotRow.verification || 'static' };
      if (slotRow.projectSlotId !== undefined) cleanSlot.projectSlotId = slotRow.projectSlotId;
      if (slotRow.role !== undefined) cleanSlot.role = slotRow.role;
      if (slotRow.ignoredReason !== undefined) cleanSlot.ignoredReason = slotRow.ignoredReason;
      slotMappings.push(cleanSlot);
    }

    var mapping = {
      mappingId: operation.mappingId || newId('cmap'),
      designComponentId: operation.designComponentId,
      expectedKind: found.component.kind,
      implementations: implementations,
      propertyMappings: propertyMappings,
      slotMappings: slotMappings,
      state: 'active',
      provenance: { kind: 'user-confirmed', actor: ACTOR, at: stamp }
    };
    if (operation.semantics !== undefined) mapping.semantics = operation.semantics;
    if (operation.mappingId) {
      var position = document.mappings.findIndex(function (row) { return row.mappingId === operation.mappingId; });
      if (position < 0) return typedFailure(409, 'COMPONENT_MAPPING_CONFLICT', 'mapping ' + operation.mappingId + ' does not exist');
      if (document.mappings[position].visualPolicy) mapping.visualPolicy = document.mappings[position].visualPolicy;
      document.mappings[position] = mapping;
    } else {
      document.mappings.push(mapping);
    }
    return null;
  }
  if (operation.op === 'retire-mapping') {
    var target = document.mappings.find(function (row) { return row.mappingId === operation.mappingId; });
    if (!target) return typedFailure(409, 'COMPONENT_MAPPING_CONFLICT', 'mapping ' + operation.mappingId + ' does not exist');
    if (target.state === 'retired') return typedFailure(409, 'COMPONENT_MAPPING_CONFLICT', 'mapping ' + operation.mappingId + ' is already retired');
    target.state = 'retired';
    var lastSeen = designComponentById(context.design, target.designComponentId);
    target.retirement = {
      reason: operation.reason.trim(),
      actor: ACTOR,
      at: stamp
    };
    if (context.comparison && HASH_RE.test(String(context.comparison.semanticHash || ''))) {
      target.retirement.lastSeenComparisonHash = context.comparison.semanticHash;
    }
    if (lastSeen) target.retirement.lastSeenDisplayName = lastSeen.component.name;
    return null;
  }
  if (operation.op === 'set-render-class') {
    var policyTarget = document.mappings.find(function (row) { return row.mappingId === operation.mappingId; });
    if (!policyTarget) return typedFailure(409, 'COMPONENT_MAPPING_CONFLICT', 'mapping ' + operation.mappingId + ' does not exist');
    if (policyTarget.state !== 'active') return typedFailure(409, 'COMPONENT_MAPPING_CONFLICT', 'render policy applies to active mappings only');
    policyTarget.visualPolicy = {
      renderClass: operation.renderClass,
      by: 'owner',
      at: stamp
    };
    if (operation.reason !== undefined) policyTarget.visualPolicy.reason = operation.reason.trim();
    return null;
  }
  if (operation.op === 'add-disposition') {
    var disposition = {
      dispositionId: newId('dcp'),
      target: { side: operation.side },
      kind: operation.kind,
      reason: operation.reason.trim(),
      owner: ACTOR,
      createdAt: stamp
    };
    if (operation.side === 'design') {
      if (typeof operation.designComponentId !== 'string') return typedFailure(400, 'COMPONENT_MAPPING_INVALID', 'design disposition requires designComponentId');
      if (!designComponentById(context.design, operation.designComponentId)) {
        return typedFailure(409, 'COMPONENT_FINDING_STALE', 'design component ' + operation.designComponentId + ' is not in the active inventory');
      }
      disposition.target.designComponentId = operation.designComponentId;
    } else {
      if (typeof operation.projectComponentId !== 'string' || typeof operation.adapterId !== 'string') {
        return typedFailure(400, 'COMPONENT_MAPPING_INVALID', 'project disposition requires projectComponentId and adapterId');
      }
      var projectInventory = context.inventories[operation.adapterId];
      if (!projectInventory || !projectInventory.components.some(function (component) { return component.projectComponentId === operation.projectComponentId; })) {
        return typedFailure(409, 'COMPONENT_FINDING_STALE', 'project component ' + operation.projectComponentId + ' is not in the published analysis');
      }
      disposition.target.projectComponentId = operation.projectComponentId;
      disposition.target.adapterId = operation.adapterId;
    }
    if (operation.supersededBy !== undefined) disposition.supersededBy = operation.supersededBy;
    if (operation.reviewAt !== undefined) disposition.reviewAt = operation.reviewAt;
    document.dispositions.push(disposition);
    return null;
  }
  if (operation.op === 'remove-disposition') {
    var index = document.dispositions.findIndex(function (row) { return row.dispositionId === operation.dispositionId; });
    if (index < 0) return typedFailure(409, 'COMPONENT_MAPPING_CONFLICT', 'disposition ' + operation.dispositionId + ' does not exist');
    document.dispositions.splice(index, 1);
    return null;
  }
  return typedFailure(400, 'COMPONENT_MAPPING_INVALID', 'unknown operation');
}

// request: exact mapping/design/project CAS revisions + durable operation id.
function mutate(request) {
  var envelope = mappingMutation.validateEnvelope(request, exact);
  if (!envelope ||
      !Array.isArray(request.operations) || !request.operations.length || request.operations.length > OPERATIONS_MAX) {
    return Promise.resolve(typedFailure(400, 'bad-component-mapping-request',
      'exact operationId and expected mapping/design/project/comparison revisions plus operations are required'));
  }
  for (var i = 0; i < request.operations.length; i++) {
    var shapeError = validateOperationShape(request.operations[i]);
    if (shapeError) return Promise.resolve(typedFailure(400, 'bad-component-mapping-request', shapeError));
  }
  return esm().then(function (modules) {
    var lease = finalizations.beginMutation({ kind: 'component-mappings', key: 'component-mappings', pendingChild: false, requireSoleWriter: true });
    if (!lease.ok) {
      return typedFailure(409, 'component-mapping-writer-busy', lease.reason || 'another writer holds the workspace');
    }
    var settle = function (result) { finalizations.endMutation(lease.handle); return result; };
    try {
      // The committed registry receipt is authoritative for exact retries,
      // even if the post-commit recompare already moved the active generation.
      var read = readRegistry();
      if (read.state === 'invalid') return settle(typedFailure(409, 'COMPONENT_MAPPING_INVALID', 'component-mappings.json is unreadable'));
      if (read.state === 'present') {
        if (modules.validate && !modules.validate(read.document)) {
          return settle(typedFailure(409, 'COMPONENT_MAPPING_INVALID', 'component-mappings.json fails its schema'));
        }
        var existingError = modules.contract.mappingRegistrySemanticError(read.document);
        if (existingError) return settle(typedFailure(409, 'COMPONENT_MAPPING_INVALID', existingError));
        var prior = mappingMutation.replay(read.document, request.operationId, envelope.requestHash);
        if (prior && prior.conflict) {
          return settle(typedFailure(409, 'COMPONENT_MAPPING_OPERATION_CONFLICT', 'operationId was already used for another request'));
        }
        if (prior) return settle(prior);
      }
      var context = mutationContext();
      if (!context.ok) return settle(context);
      var needsComparison = request.operations.some(function (operation) {
        return operation.op === 'upsert-mapping' || operation.op === 'add-disposition';
      });
      if (needsComparison) {
        if (!context.comparison) {
          return settle(typedFailure(409, 'component-comparison-required', 'run a project comparison before confirming mappings'));
        }
        if (request.expectedComparisonSemanticHash !== context.comparison.semanticHash) {
          return settle(typedFailure(409, 'COMPONENT_FINDING_STALE', 'the comparison this review was based on is no longer current'));
        }
      }
      var document = read.state === 'absent'
        ? modules.contract.emptyMappingRegistry(context.design.scopeId)
        : JSON.parse(JSON.stringify(read.document));
      var cas = mappingMutation.casFromContext(context.active, 'project-component-analysis-index');
      if (request.expectedDesignGenerationRevision !== cas.designGenerationRevision) {
        return settle(typedFailure(409, 'COMPONENT_DESIGN_GENERATION_CONFLICT', 'design generation moved'));
      }
      if (request.expectedProjectInventoryRevision !== cas.projectInventoryRevision) {
        return settle(typedFailure(409, 'COMPONENT_PROJECT_INVENTORY_CONFLICT', 'project inventory revision moved'));
      }
      if (document.revision !== request.expectedMappingRevision) {
        return settle(typedFailure(409, 'COMPONENT_MAPPING_REVISION_CONFLICT', 'registry revision moved', { currentRevision: document.revision }));
      }
      var stamp = now();
      for (var index = 0; index < request.operations.length; index++) {
        var failure = applyOperation(document, request.operations[index], context, stamp);
        if (failure) return settle(failure);
      }
      document.revision = request.expectedMappingRevision + 1;
      mappingMutation.appendReceipt(document, request.operationId, envelope.requestHash, stamp);
      if (modules.validate && !modules.validate(document)) {
        var first = (modules.validate.errors || [])[0];
        return settle(typedFailure(400, 'COMPONENT_MAPPING_INVALID', 'mutated registry fails its schema: ' + ((first && (first.instancePath || '/') + ' ' + first.message) || '')));
      }
      var semanticError = modules.contract.mappingRegistrySemanticError(document);
      if (semanticError) return settle(typedFailure(400, 'COMPONENT_MAPPING_INVALID', semanticError));
      var bytes = Buffer.from(JSON.stringify(document, null, 2) + '\n');
      var write = fileGuards.atomicReplaceRegularFileResult(paths.PROJECT_ROOT, path.dirname(MAPPING_FILE), MAPPING_FILE, bytes,
        { create: true, directoryMode: 0o755, mode: 0o644, maxBytes: MAPPING_MAX });
      if (!write.ok) return settle(typedFailure(500, 'component-mapping-write-failed', 'atomic mapping publication failed'));
      var verify = readRegistry();
      if (verify.state !== 'present' || verify.document.revision !== document.revision) {
        return settle(typedFailure(500, 'COMPONENT_MAPPING_PUBLICATION_RECOVERY_REQUIRED', 'post-write verification failed'));
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
      // Post-commit compare enqueue (REQ-CONC-006): the bumped revision is
      // the durable invalidation; the recompare only shortens the stale
      // window and runs strictly after the writer lease above was released.
      try { require('./design-component-compare').ensureFresh('mapping-mutation'); } catch (error) {}
      return committed;
    } catch (error) {
      return settle(typedFailure(500, 'component-mapping-write-failed', 'mapping mutation failed safely'));
    }
  });
}
function publicErrorCode() {
  return 'component-mapping-write-failed';
}

module.exports = {
  MAPPING_FILE: MAPPING_FILE,
  get: get,
  mutate: mutate,
  publicErrorCode: publicErrorCode
};
