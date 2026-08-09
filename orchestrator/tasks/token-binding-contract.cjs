'use strict';

// Canonical observed-token task binding shared by task creation and
// finalization. A task freezes one catalog coordinate and one intended
// project address; it never carries provider variable IDs or mode inference.

const crypto = require('crypto');
const path = require('path');

const HASH_RE = /^sha256:[a-f0-9]{64}$/;
const TOKEN_RE = /^otk:sha256:[a-f0-9]{64}$/;
const SOURCE_ID_RE = /^design:observed-token:(otk:sha256:[a-f0-9]{64}):(token-(?:implement|reconcile-value|reconcile-mapping))$/;
const ADAPTER_RE = /^[a-z0-9][a-z0-9-]{0,63}$/;
const PROJECT_MODE_RE = /^[a-z][a-z0-9-]{0,59}$/;
const FINDING_RE = /^tokf-[a-f0-9]{24}$/;
const MAPPING_RE = /^map-[a-f0-9]{24}$/;
const INTENTS = Object.freeze(['implement', 'reconcile-value', 'reconcile-mapping']);
const FIELDS = Object.freeze([
  'schemaVersion', 'sourceId', 'intent', 'observedTokenKey', 'contextKey',
  'catalogHash', 'sourceIndexHash', 'bindingSnapshotHash', 'expectedKind',
  'frozenValue', 'intendedAdapterId', 'intendedSemanticPath',
  'intendedProjectMode', 'findingId', 'comparisonSemanticHash', 'mappingRevision'
]);
const OPTIONAL_FIELDS = Object.freeze(['mappingId']);

function sha256(value) {
  return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex');
}

function bindingSourceId(ref) {
  const match = SOURCE_ID_RE.exec(String(ref || ''));
  return match ? { sourceId: match[0], observedTokenKey: match[1], kind: match[2] } : null;
}

function bindingRelativePath(sourceId) {
  const digest = crypto.createHash('sha256').update(String(sourceId), 'utf8').digest('hex').slice(0, 32);
  return path.posix.join('orchestrator', 'tasks', 'evidence', 'token-bindings', digest + '.json');
}

function finiteJson(value, depth = 0) {
  if (depth > 16) return false;
  if (value === null || typeof value === 'string' || typeof value === 'boolean') return true;
  if (typeof value === 'number') return Number.isFinite(value) && !Object.is(value, -0);
  if (Array.isArray(value)) return value.length <= 256 && value.every((entry) => finiteJson(entry, depth + 1));
  if (!value || typeof value !== 'object') return false;
  const keys = Object.keys(value);
  return keys.length <= 256 && keys.every((key) => key.length <= 128 && finiteJson(value[key], depth + 1));
}

function bindingError(binding) {
  if (!binding || typeof binding !== 'object' || Array.isArray(binding)) return 'binding must be an object';
  for (const key of Object.keys(binding)) {
    if (!FIELDS.includes(key) && !OPTIONAL_FIELDS.includes(key)) return `binding carries unknown key ${JSON.stringify(key)}`;
  }
  for (const key of FIELDS) {
    if (!(key in binding)) return `binding is missing ${key}`;
  }
  if (binding.schemaVersion !== 2) return 'binding schemaVersion must be 2';
  const source = bindingSourceId(binding.sourceId);
  if (!source) return 'binding sourceId is not an observed-token source';
  if (!INTENTS.includes(binding.intent) || source.kind !== 'token-' + binding.intent) return 'binding intent does not match sourceId';
  if (!TOKEN_RE.test(String(binding.observedTokenKey || '')) ||
      binding.observedTokenKey !== source.observedTokenKey) return 'binding observedTokenKey must match sourceId';
  if (typeof binding.contextKey !== 'string' || !binding.contextKey || binding.contextKey.length > 256) {
    return 'binding contextKey malformed';
  }
  try {
    const context = JSON.parse(binding.contextKey);
    if (!context || typeof context !== 'object' || Array.isArray(context)) return 'binding contextKey is not an object';
  } catch (error) {
    return 'binding contextKey is not canonical JSON';
  }
  for (const key of ['catalogHash', 'sourceIndexHash', 'bindingSnapshotHash', 'comparisonSemanticHash']) {
    if (!HASH_RE.test(String(binding[key] || ''))) return `binding ${key} malformed`;
  }
  if (!['color', 'number', 'string', 'boolean'].includes(binding.expectedKind)) return 'binding expectedKind malformed';
  if (!finiteJson(binding.frozenValue) ||
      Buffer.byteLength(JSON.stringify(binding.frozenValue), 'utf8') > 16 * 1024) return 'binding frozenValue malformed';
  if (!ADAPTER_RE.test(String(binding.intendedAdapterId || ''))) return 'binding intendedAdapterId malformed';
  if (!Array.isArray(binding.intendedSemanticPath) || !binding.intendedSemanticPath.length ||
      binding.intendedSemanticPath.length > 32 ||
      binding.intendedSemanticPath.some((segment) => typeof segment !== 'string' || !segment || segment.length > 128)) {
    return 'binding intendedSemanticPath malformed';
  }
  if (!PROJECT_MODE_RE.test(String(binding.intendedProjectMode || ''))) return 'binding intendedProjectMode malformed';
  if (!FINDING_RE.test(String(binding.findingId || ''))) return 'binding findingId malformed';
  if (!Number.isSafeInteger(binding.mappingRevision) || binding.mappingRevision < 0) return 'binding mappingRevision malformed';
  if (binding.mappingId !== undefined && !MAPPING_RE.test(String(binding.mappingId || ''))) return 'binding mappingId malformed';
  return null;
}

module.exports = {
  HASH_RE: HASH_RE,
  bindingSourceId: bindingSourceId,
  bindingRelativePath: bindingRelativePath,
  bindingError: bindingError,
  sha256: sha256
};
