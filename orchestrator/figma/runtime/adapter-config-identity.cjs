'use strict';

// Capability-local identity for the shared project-adapters.json envelope.
// A token-only decision must never be invalidated by component-only fields.
// Component extraction does consume the token section of a dual-capability
// adapter (for token-binding evidence), so that dependency is intentionally
// included in the component projection.

var crypto = require('crypto');

function canonical(value, depth) {
  if (depth > 64) throw new Error('adapter-config-identity-depth-limit');
  if (value === null) return 'null';
  if (typeof value === 'string') return JSON.stringify(value);
  if (typeof value === 'boolean') return value ? 'true' : 'false';
  if (typeof value === 'number') {
    if (!Number.isFinite(value) || Object.is(value, -0)) throw new Error('adapter-config-identity-number-invalid');
    return JSON.stringify(value);
  }
  if (Array.isArray(value)) return '[' + value.map(function (item) { return canonical(item, depth + 1); }).join(',') + ']';
  if (value && typeof value === 'object') {
    return '{' + Object.keys(value).sort().filter(function (key) { return value[key] !== undefined; }).map(function (key) {
      return JSON.stringify(key) + ':' + canonical(value[key], depth + 1);
    }).join(',') + '}';
  }
  throw new Error('adapter-config-identity-value-invalid');
}

function capabilityProjection(document, capability) {
  if (capability !== 'tokens' && capability !== 'components') throw new Error('adapter-config-capability-invalid');
  if (!document || document.schemaVersion !== 2 || !Array.isArray(document.adapters)) {
    throw new Error('adapter-config-document-invalid');
  }
  return {
    schemaVersion: document.schemaVersion,
    capability: capability,
    adapters: document.adapters.filter(function (adapter) {
      return adapter && adapter.enabled === true && Array.isArray(adapter.capabilities) &&
        adapter.capabilities.indexOf(capability) >= 0;
    }).map(function (adapter) {
      var projected = {
        id: adapter.id,
        kind: adapter.kind,
        version: adapter.version,
        enabled: true,
        capability: capability
      };
      if (capability === 'tokens') projected.tokens = adapter.tokens;
      else {
        projected.platform = adapter.platform;
        projected.components = adapter.components;
        // Deliberate directed dependency: the component extractor receives
        // tokensConfig for dual-capability adapters.
        if (adapter.capabilities.indexOf('tokens') >= 0) projected.tokens = adapter.tokens;
      }
      return projected;
    })
  };
}

function capabilityHash(document, capability) {
  return 'sha256:' + crypto.createHash('sha256').update(capabilityJson(document, capability), 'utf8').digest('hex');
}

function capabilityJson(document, capability) {
  return canonical(capabilityProjection(document, capability));
}

module.exports = {
  capabilityJson: capabilityJson,
  capabilityHash: capabilityHash
};
