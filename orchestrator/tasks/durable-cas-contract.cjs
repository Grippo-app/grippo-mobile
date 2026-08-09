'use strict';

// Pure shared vocabulary for crash-recoverable conditional publication.
// Filesystem scanners consume the exact name predicate; recovery authorities
// additionally validate the small canonical manifest before using it.

var PREFIX = '.durable-cas-';
var NAME_RE = /^\.durable-cas-[a-f0-9]{16}-[a-f0-9]{16}-[a-f0-9]{16}$/;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var DECIMAL_U64_RE = /^(?:0|[1-9][0-9]{0,19})$/;
var DECIMAL_I64_RE = /^-?(?:0|[1-9][0-9]{0,19})$/;
var MANIFEST_FIELDS = ['candidateHash', 'expectedProof', 'maxBytes', 'owner', 'targetName', 'version'].sort();
var PROOF_FIELDS = ['ctimeNs', 'dev', 'hash', 'ino', 'mode', 'mtimeNs', 'size'].sort();
var PARTIAL_RE = /^\.(manifest|candidate)-partial-[a-f0-9]{16}$/;
var SCHEMA = Object.freeze({
  prefix: PREFIX,
  namePattern: NAME_RE.source,
  manifestVersion: 1,
  manifestFields: Object.freeze(MANIFEST_FIELDS.slice()),
  proofFields: Object.freeze(PROOF_FIELDS.slice()),
  decimalStringProofFields: Object.freeze(['ctimeNs', 'dev', 'ino', 'mtimeNs'])
});

function sameKeys(value, fields) {
  return value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === fields.join('\0');
}
function isName(name) { return typeof name === 'string' && NAME_RE.test(name); }
function classifyName(name) {
  if (typeof name !== 'string' || !name.startsWith(PREFIX)) return null;
  return isName(name) ? 'recovery-required' : 'unsafe';
}
function classifyArtifactName(name) {
  if (name === 'manifest.json') return 'manifest';
  if (name === 'candidate') return 'candidate';
  if (name === 'source') return 'source';
  var match = typeof name === 'string' && PARTIAL_RE.exec(name);
  return match ? match[1] + '-partial' : null;
}
function validateManifest(value) {
  if (!sameKeys(value, MANIFEST_FIELDS) || value.version !== 1 ||
      typeof value.targetName !== 'string' || !value.targetName || value.targetName.length > 180 ||
      /[\\/\u0000]/.test(value.targetName) || value.targetName === '.' || value.targetName === '..' ||
      typeof value.owner !== 'string' || !/^[A-Za-z0-9_.:-]{1,240}$/.test(value.owner) ||
      typeof value.candidateHash !== 'string' || !HASH_RE.test(value.candidateHash) ||
      !Number.isSafeInteger(value.maxBytes) || value.maxBytes < 1 || value.maxBytes > 16 * 1024 * 1024 ||
      !sameKeys(value.expectedProof, PROOF_FIELDS)) {
    throw new Error('durable CAS manifest fields are invalid');
  }
  var proof = value.expectedProof;
  for (var field of ['dev', 'ino']) {
    if (typeof proof[field] !== 'string' || !DECIMAL_U64_RE.test(proof[field])) {
      throw new Error('durable CAS source proof is invalid');
    }
  }
  for (var timeField of ['mtimeNs', 'ctimeNs']) {
    if (typeof proof[timeField] !== 'string' || !DECIMAL_I64_RE.test(proof[timeField]) || proof[timeField] === '-0') {
      throw new Error('durable CAS source proof is invalid');
    }
  }
  if (!Number.isSafeInteger(proof.mode) || proof.mode < 0 || proof.mode > 0o7777 ||
      !Number.isSafeInteger(proof.size) || proof.size < 0 ||
      !HASH_RE.test(String(proof.hash || '')) || proof.size > value.maxBytes) {
    throw new Error('durable CAS source proof is invalid');
  }
  return value;
}
function canonical(value) {
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']';
  if (value && typeof value === 'object') return '{' + Object.keys(value).sort().map(function (key) {
    return JSON.stringify(key) + ':' + canonical(value[key]);
  }).join(',') + '}';
  return JSON.stringify(value);
}
function canonicalManifest(value) {
  validateManifest(value);
  return Buffer.from(canonical(value) + '\n', 'utf8');
}
function validateOperationSnapshot(entries) {
  if (!Array.isArray(entries) || entries.length > 8) throw new Error('durable CAS artifact set exceeds its bound');
  var seenNames = new Set(), seenKinds = new Set(), normalized = [];
  entries.forEach(function (entry) {
    if (!entry || typeof entry !== 'object' || Array.isArray(entry) ||
        !['kind', 'name', 'size'].every(function (field) { return Object.prototype.hasOwnProperty.call(entry, field); }) ||
        Object.keys(entry).some(function (field) { return !['bytes', 'kind', 'name', 'size'].includes(field); }) ||
        entry.kind !== 'file' || !Number.isSafeInteger(entry.size) || entry.size < 0 ||
        (entry.bytes !== undefined && !Buffer.isBuffer(entry.bytes))) {
      throw new Error('durable CAS artifact snapshot is invalid');
    }
    var artifactKind = classifyArtifactName(entry.name);
    if (!artifactKind || seenNames.has(entry.name) || seenKinds.has(artifactKind)) {
      throw new Error('durable CAS artifact names are invalid or duplicated');
    }
    seenNames.add(entry.name); seenKinds.add(artifactKind);
    normalized.push({ name: entry.name, artifactKind: artifactKind, size: entry.size, bytes: entry.bytes });
  });
  var byKind = Object.create(null);
  normalized.forEach(function (entry) { byKind[entry.artifactKind] = entry; });
  if (!byKind.manifest && (byKind.candidate || byKind.source || byKind['candidate-partial'])) {
    throw new Error('durable CAS artifacts crossed the manifest boundary out of order');
  }
  if (byKind.source && !byKind.candidate) throw new Error('durable CAS source has no completed candidate');
  if (byKind.manifest && byKind['manifest-partial']) throw new Error('durable CAS retained a manifest partial after publication');
  if (byKind.candidate && byKind['candidate-partial']) throw new Error('durable CAS retained a candidate partial after publication');
  if (byKind['manifest-partial'] && byKind['manifest-partial'].size > 16 * 1024) throw new Error('durable CAS manifest partial is oversized');
  var manifest = null;
  if (byKind.manifest) {
    if (byKind.manifest.size > 16 * 1024 || !Buffer.isBuffer(byKind.manifest.bytes) ||
        byKind.manifest.bytes.length !== byKind.manifest.size) throw new Error('durable CAS manifest bytes are missing or oversized');
    var text;
    try { text = new (require('util').TextDecoder)('utf-8', { fatal: true }).decode(byKind.manifest.bytes); }
    catch (error) { throw new Error('durable CAS manifest is not valid UTF-8'); }
    try { manifest = JSON.parse(text); }
    catch (error) { throw new Error('durable CAS manifest is not valid JSON'); }
    if (!byKind.manifest.bytes.equals(canonicalManifest(manifest))) throw new Error('durable CAS manifest is not canonical');
    ['candidate', 'source', 'candidate-partial'].forEach(function (kind) {
      if (byKind[kind] && byKind[kind].size > manifest.maxBytes) throw new Error('durable CAS artifact exceeds the manifest byte bound');
    });
  }
  return { manifest: manifest, entries: normalized, phase: byKind.source ? 'detached' : (byKind.candidate ? 'armed' : (byKind.manifest ? 'staging' : 'unarmed')) };
}

module.exports = {
  PREFIX: PREFIX,
  NAME_RE: NAME_RE,
  SCHEMA: SCHEMA,
  isName: isName,
  classifyName: classifyName,
  classifyArtifactName: classifyArtifactName,
  validateManifest: validateManifest,
  canonicalManifest: canonicalManifest,
  validateOperationSnapshot: validateOperationSnapshot
};
