'use strict';

// Canonical component task-binding contract shared by the server-side task
// creator (writes the frozen evidence) and the finalizer (reads it back).
// One owner for the path derivation, the field validation the runtime relies
// on, and the byte hashing that pins the task's Source fingerprint to the
// exact binding artifact. The JSON schema
// (orchestrator/figma/schemas/component-task-binding.schema.json) remains the
// outer contract; this module enforces the invariants the runtime consumes.

const crypto = require('crypto');
const path = require('path');

const HASH_RE = /^sha256:[a-f0-9]{64}$/;
const SOURCE_ID_RE = /^design:component:(figma-component:[a-f0-9]{16}:(?:none|[a-f0-9]{16}):[A-Za-z0-9][A-Za-z0-9:;_-]{0,79}):(component-(?:implement|update-api|update-visual|add-platform|reconcile-mapping|remap))$/;
const SCOPE_RE = /^figma:file:[a-f0-9]{16}:branch:(?:none|[a-f0-9]{16}):components:[a-f0-9]{16}$/;
const GENERATION_RE = /^gen-[a-f0-9]{32}$/;
const ADAPTER_RE = /^[a-z0-9][a-z0-9-]{0,63}$/;
const PLATFORM_RE = /^[a-z][a-z0-9-]{0,59}$/;
const FINDING_RE = /^cmpf-[a-f0-9]{24}$/;
const MAPPING_RE = /^cmap-[a-f0-9]{24}$/;
const PROJECT_PROPERTY_RE = /^param:[^\u0000-\u001f]{1,200}$/;
const OBSERVED_TOKEN_RE = /^otk:sha256:[a-f0-9]{64}$/;
const TOKEN_SOURCE_RE = /^otsrc:sha256:[a-f0-9]{64}$/;
const INTENTS = Object.freeze(['implement', 'update-api', 'update-visual', 'add-platform', 'reconcile-mapping', 'remap']);
const RELATIONS = Object.freeze(['direct', 'wrapper', 'composite', 'shared-implementation']);
const FIELDS = Object.freeze([
  'schemaVersion', 'sourceId', 'intent', 'designComponentId', 'designScopeId', 'designGenerationId',
  'designInventoryHash', 'expectedKind', 'frozenStructuralHash', 'frozenSourceHash', 'frozenSpec',
  'intendedAdapterId', 'intendedPlatform', 'intendedRelation', 'findingId', 'comparisonSemanticHash',
  'mappingRevision'
]);
const OPTIONAL_FIELDS = Object.freeze(['mappingId', 'intendedProjectSymbol', 'intendedPropertyMappings']);

function sha256(value) {
  return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex');
}

// The binding family a design-finding Source ref belongs to, or null when the
// ref is not a design-origin component binding at all.
function bindingSourceId(ref) {
  const match = SOURCE_ID_RE.exec(String(ref || ''));
  return match ? { sourceId: match[0], designComponentId: match[1], kind: match[2] } : null;
}

function bindingRelativePath(sourceId) {
  const digest = crypto.createHash('sha256').update(String(sourceId), 'utf8').digest('hex').slice(0, 32);
  return path.posix.join('orchestrator', 'tasks', 'evidence', 'component-bindings', digest + '.json');
}

// Returns null or the first error string. Mirrors the runtime-critical schema
// invariants; the ajv schema stays authoritative for producers.
function bindingError(binding) {
  if (!binding || typeof binding !== 'object' || Array.isArray(binding)) return 'binding must be an object';
  const keys = Object.keys(binding);
  for (const key of keys) {
    if (!FIELDS.includes(key) && !OPTIONAL_FIELDS.includes(key)) return `binding carries unknown key ${JSON.stringify(key)}`;
  }
  for (const key of FIELDS) {
    if (!(key in binding)) return `binding is missing ${key}`;
  }
  if (binding.schemaVersion !== 2) return 'binding schemaVersion must be 2';
  const source = bindingSourceId(binding.sourceId);
  if (!source) return 'binding sourceId is not a design-origin component source';
  if (!INTENTS.includes(binding.intent)) return 'binding intent is unknown';
  if ('component-' + binding.intent !== source.kind) return 'binding intent must match its sourceId';
  if (binding.designComponentId !== source.designComponentId) return 'binding designComponentId must match its sourceId';
  if (!SCOPE_RE.test(String(binding.designScopeId || ''))) return 'binding designScopeId malformed';
  if (!GENERATION_RE.test(String(binding.designGenerationId || ''))) return 'binding designGenerationId malformed';
  if (!HASH_RE.test(String(binding.designInventoryHash || ''))) return 'binding designInventoryHash malformed';
  if (!['component-set', 'component'].includes(binding.expectedKind)) return 'binding expectedKind malformed';
  if (!HASH_RE.test(String(binding.frozenStructuralHash || ''))) return 'binding frozenStructuralHash malformed';
  if (!HASH_RE.test(String(binding.frozenSourceHash || ''))) return 'binding frozenSourceHash malformed';
  const spec = binding.frozenSpec;
  if (!spec || typeof spec !== 'object' || Array.isArray(spec)) return 'binding frozenSpec malformed';
  if (typeof spec.name !== 'string' || !spec.name || spec.name.length > 300) return 'binding frozenSpec.name malformed';
  if (!Array.isArray(spec.properties) || spec.properties.length > 40) return 'binding frozenSpec.properties malformed';
  if (!Array.isArray(spec.variants) || spec.variants.length > 500) return 'binding frozenSpec.variants malformed';
  if (!Array.isArray(spec.slots) || spec.slots.length > 128) return 'binding frozenSpec.slots malformed';
  if (!Array.isArray(spec.tokenRefs) || spec.tokenRefs.length > 128) return 'binding frozenSpec.tokenRefs malformed';
  const tokenRefs = new Set();
  for (const ref of spec.tokenRefs) {
    if (!ref || typeof ref !== 'object' || Array.isArray(ref)) return 'binding frozenSpec.tokenRefs row malformed';
    const allowed = ['observedTokenKey', 'contextKey', 'sourceId', 'providerName', 'field'];
    if (Object.keys(ref).some((key) => !allowed.includes(key))) return 'binding frozenSpec.tokenRefs row carries an unknown key';
    if (!OBSERVED_TOKEN_RE.test(String(ref.observedTokenKey || ''))) return 'binding frozenSpec.tokenRefs observedTokenKey malformed';
    if (typeof ref.contextKey !== 'string' || !ref.contextKey || ref.contextKey.length > 256) return 'binding frozenSpec.tokenRefs contextKey malformed';
    if (!TOKEN_SOURCE_RE.test(String(ref.sourceId || ''))) return 'binding frozenSpec.tokenRefs sourceId malformed';
    if (typeof ref.providerName !== 'string' || !ref.providerName || ref.providerName.length > 512) return 'binding frozenSpec.tokenRefs providerName malformed';
    if (typeof ref.field !== 'string' || !ref.field || ref.field.length > 120) return 'binding frozenSpec.tokenRefs field malformed';
    const key = ref.observedTokenKey + '\u0000' + ref.contextKey + '\u0000' + ref.field;
    if (tokenRefs.has(key)) return 'binding frozenSpec.tokenRefs contains a duplicate exact dependency';
    tokenRefs.add(key);
  }
  if (!ADAPTER_RE.test(String(binding.intendedAdapterId || ''))) return 'binding intendedAdapterId malformed';
  if (!PLATFORM_RE.test(String(binding.intendedPlatform || ''))) return 'binding intendedPlatform malformed';
  if (!RELATIONS.includes(binding.intendedRelation)) return 'binding intendedRelation malformed';
  if (binding.intendedProjectSymbol !== undefined &&
      (typeof binding.intendedProjectSymbol !== 'string' || !binding.intendedProjectSymbol ||
        binding.intendedProjectSymbol.length > 320)) {
    return 'binding intendedProjectSymbol malformed';
  }
  if ((binding.intent === 'implement' || binding.intent === 'update-api' || binding.intent === 'update-visual' ||
       binding.intent === 'add-platform') && binding.intendedProjectSymbol === undefined) {
    return `binding intent ${binding.intent} requires intendedProjectSymbol`;
  }
  if (binding.intendedPropertyMappings !== undefined) {
    if (!Array.isArray(binding.intendedPropertyMappings) || binding.intendedPropertyMappings.length > 40) {
      return 'binding intendedPropertyMappings malformed';
    }
    const seen = new Set();
    for (const row of binding.intendedPropertyMappings) {
      if (!row || typeof row !== 'object' || Array.isArray(row)) return 'binding intendedPropertyMappings row malformed';
      const rowKeys = Object.keys(row);
      if (rowKeys.some((key) => !['designPropertyId', 'projectPropertyId', 'valueMap'].includes(key))) {
        return 'binding intendedPropertyMappings row carries an unknown key';
      }
      if (typeof row.designPropertyId !== 'string' || !row.designPropertyId || row.designPropertyId.length > 160) {
        return 'binding intendedPropertyMappings designPropertyId malformed';
      }
      if (seen.has(row.designPropertyId)) return 'binding intendedPropertyMappings binds one design property twice';
      seen.add(row.designPropertyId);
      if (!PROJECT_PROPERTY_RE.test(String(row.projectPropertyId || ''))) return 'binding intendedPropertyMappings projectPropertyId malformed';
      if (row.valueMap !== undefined) {
        if (!row.valueMap || typeof row.valueMap !== 'object' || Array.isArray(row.valueMap)) return 'binding intendedPropertyMappings valueMap malformed';
        for (const value of Object.values(row.valueMap)) {
          if (typeof value !== 'string' || !value || value.length > 200) return 'binding intendedPropertyMappings valueMap value malformed';
        }
      }
    }
  }
  if (!FINDING_RE.test(String(binding.findingId || ''))) return 'binding findingId malformed';
  if (!HASH_RE.test(String(binding.comparisonSemanticHash || ''))) return 'binding comparisonSemanticHash malformed';
  if (!Number.isSafeInteger(binding.mappingRevision) || binding.mappingRevision < 0) return 'binding mappingRevision malformed';
  if (binding.mappingId !== undefined && !MAPPING_RE.test(String(binding.mappingId || ''))) return 'binding mappingId malformed';
  if ((binding.intent === 'reconcile-mapping' || binding.intent === 'remap' || binding.intent === 'update-api' ||
       binding.intent === 'update-visual' || binding.intent === 'add-platform') && binding.mappingId === undefined) {
    return `binding intent ${binding.intent} requires mappingId`;
  }
  return null;
}

module.exports = {
  HASH_RE: HASH_RE,
  bindingSourceId: bindingSourceId,
  bindingRelativePath: bindingRelativePath,
  bindingError: bindingError,
  sha256: sha256
};
