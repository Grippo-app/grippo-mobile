'use strict';

var crypto = require('crypto');
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var SOURCE_RE = /^otsrc:sha256:[a-f0-9]{64}$/;
var CONTEXT_KEYS = ['theme', 'locale', 'platform', 'state'];
var REQUIRED_CONTEXT_KEYS = ['theme', 'locale', 'platform'];
var CONTROL_RE = /[\u0000-\u001f\u007f-\u009f]/;

function canonical(value) {
  if (value === null) return 'null';
  if (typeof value === 'string') return JSON.stringify(value);
  if (typeof value === 'number' || typeof value === 'boolean') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']';
  return '{' + Object.keys(value).sort().map(function (key) {
    return JSON.stringify(key) + ':' + canonical(value[key]);
  }).join(',') + '}';
}
function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(canonical(value), 'utf8').digest('hex');
}
function canonicalContext(context) {
  if (!context || typeof context !== 'object' || Array.isArray(context)) {
    throw new Error('TOKEN_SOURCE_CONTEXT_INVALID');
  }
  if (Object.keys(context).some(function (key) { return CONTEXT_KEYS.indexOf(key) < 0; })) {
    throw new Error('TOKEN_SOURCE_CONTEXT_INVALID');
  }
  if (REQUIRED_CONTEXT_KEYS.some(function (key) {
    return typeof context[key] !== 'string' || !context[key];
  })) throw new Error('TOKEN_SOURCE_CONTEXT_INVALID');
  var out = {};
  CONTEXT_KEYS.forEach(function (key) {
    if (!Object.prototype.hasOwnProperty.call(context, key)) return;
    if (typeof context[key] !== 'string') throw new Error('TOKEN_SOURCE_CONTEXT_INVALID');
    var value = context[key].normalize('NFC');
    if (!value || CONTROL_RE.test(value) || Buffer.byteLength(value, 'utf8') > 64) {
      throw new Error('TOKEN_SOURCE_CONTEXT_INVALID');
    }
    out[key] = value;
  });
  return out;
}
function contextKey(context) {
  return canonical(canonicalContext(context));
}
function parseContextKey(value) {
  if (typeof value !== 'string') throw new Error('TOKEN_SOURCE_CONTEXT_INVALID');
  var parsed;
  try { parsed = JSON.parse(value); } catch (error) {
    throw new Error('TOKEN_SOURCE_CONTEXT_INVALID');
  }
  var normalized = canonicalContext(parsed);
  if (canonical(normalized) !== value) throw new Error('TOKEN_SOURCE_CONTEXT_INVALID');
  return normalized;
}
function sourceIdFor(source) {
  if (!HASH_RE.test(String(source && source.fileKeyFingerprint || '')) ||
      !source || typeof source.branchKey !== 'string' || !source.branchKey ||
      !/^[0-9]+:[0-9]+$/.test(String(source.nodeId || ''))) throw new Error('token-source-identity-invalid');
  return 'otsrc:' + hash({
    namespace: 'observed-token-source/v1',
    provider: 'figma-mcp',
    fileKeyFingerprint: source.fileKeyFingerprint,
    branchKey: source.branchKey.normalize('NFC'),
    nodeId: source.nodeId,
    context: canonicalContext(source.context)
  });
}
function observedTokenKeyFor(input) {
  if (!HASH_RE.test(String(input && input.fileKeyFingerprint || '')) ||
      !input || typeof input.branchKey !== 'string' || !input.branchKey ||
      typeof input.providerName !== 'string' || !input.providerName) throw new Error('token-key-identity-invalid');
  return 'otk:' + hash({
    namespace: 'observed-token-key/v1',
    fileKeyFingerprint: input.fileKeyFingerprint,
    branchKey: input.branchKey.normalize('NFC'),
    providerName: input.providerName.normalize('NFC')
  });
}
function sourceBucket(sourceId) {
  if (!SOURCE_RE.test(String(sourceId || ''))) throw new Error('token-source-identity-invalid');
  return parseInt(sourceId.slice('otsrc:sha256:'.length, 'otsrc:sha256:'.length + 2), 16) >>> 1;
}

module.exports = {
  canonical: canonical,
  hash: hash,
  canonicalContext: canonicalContext,
  contextKey: contextKey,
  parseContextKey: parseContextKey,
  sourceIdFor: sourceIdFor,
  observedTokenKeyFor: observedTokenKeyFor,
  sourceBucket: sourceBucket
};
