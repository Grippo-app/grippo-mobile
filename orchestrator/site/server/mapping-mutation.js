'use strict';

// Domain-neutral Mapping Review mutation envelope. The operation receipt is
// committed in the same project-owned registry bytes as the mapping change,
// so a browser retry can distinguish an exact replay from a reused intent
// without relying on process memory or a mutable cache.

var crypto = require('crypto');

var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var OPERATION_ID_RE = /^mop-[a-f0-9]{32}$/;
var RECEIPTS_MAX = 1000;

function canonical(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']';
  return '{' + Object.keys(value).sort().map(function (key) {
    return JSON.stringify(key) + ':' + canonical(value[key]);
  }).join(',') + '}';
}

function hashRequest(request) {
  return 'sha256:' + crypto.createHash('sha256').update(canonical({
    expectedMappingRevision: request.expectedMappingRevision,
    expectedDesignGenerationRevision: request.expectedDesignGenerationRevision,
    expectedProjectInventoryRevision: request.expectedProjectInventoryRevision,
    expectedComparisonSemanticHash: request.expectedComparisonSemanticHash,
    operations: request.operations
  }), 'utf8').digest('hex');
}

function validateEnvelope(request, exact) {
  var keys = ['operationId', 'expectedMappingRevision', 'expectedDesignGenerationRevision',
    'expectedProjectInventoryRevision', 'expectedComparisonSemanticHash', 'operations'];
  if (!exact(request || {}, keys) || !OPERATION_ID_RE.test(String(request.operationId || '')) ||
      !Number.isSafeInteger(request.expectedMappingRevision) || request.expectedMappingRevision < 0 ||
      !HASH_RE.test(String(request.expectedDesignGenerationRevision || '')) ||
      request.expectedProjectInventoryRevision !== null && !HASH_RE.test(String(request.expectedProjectInventoryRevision || '')) ||
      request.expectedComparisonSemanticHash !== null && !HASH_RE.test(String(request.expectedComparisonSemanticHash || '')) ||
      !Array.isArray(request.operations)) return null;
  return { requestHash: hashRequest(request), keys: keys };
}

function replay(document, operationId, requestHash) {
  var receipts = Array.isArray(document.operationReceipts) ? document.operationReceipts : [];
  var receipt = receipts.find(function (row) { return row.operationId === operationId; });
  if (!receipt) return null;
  if (receipt.requestHash !== requestHash) return { conflict: true };
  return {
    ok: true,
    status: 200,
    operationId: receipt.operationId,
    requestHash: receipt.requestHash,
    revision: receipt.revision,
    mappings: receipt.mappings,
    dispositions: receipt.dispositions,
    replayed: true
  };
}

function appendReceipt(document, operationId, requestHash, stamp) {
  if (!Array.isArray(document.operationReceipts)) document.operationReceipts = [];
  // Retain the newest bounded replay window. An evicted request still carries
  // its old expected mapping revision, so a late retry cannot execute again:
  // it deterministically loses CAS against the current registry revision.
  if (document.operationReceipts.length >= RECEIPTS_MAX) {
    document.operationReceipts.splice(0, document.operationReceipts.length - RECEIPTS_MAX + 1);
  }
  document.operationReceipts.push({
    operationId: operationId,
    requestHash: requestHash,
    revision: document.revision,
    mappings: document.mappings.length,
    dispositions: document.dispositions.length,
    committedAt: stamp
  });
}

function casFromContext(active, indexRole) {
  var entry = active && active.manifest && active.manifest.artifacts.find(function (row) { return row.role === indexRole; });
  return {
    designGenerationRevision: active && active.pointer ? active.pointer.manifestHash : null,
    projectInventoryRevision: entry ? entry.hash : null
  };
}

module.exports = {
  HASH_RE: HASH_RE,
  OPERATION_ID_RE: OPERATION_ID_RE,
  validateEnvelope: validateEnvelope,
  replay: replay,
  appendReceipt: appendReceipt,
  casFromContext: casFromContext
};
