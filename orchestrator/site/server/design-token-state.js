'use strict';

// Cheap, poll-safe currency signals for the token comparison projection.
// Reads exactly two small project-owned files (adapter config + mapping
// registry) per call and keeps an in-memory project-dirty latch fed by
// durable invalidation receipts and the startup reconciler. It never hashes
// repository sources on a GET (REQ-PERF-002): exact input fingerprints are
// re-taken only by plan/start/publish and by the startup pass.

var crypto = require('crypto');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var adapterConfigIdentity = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'runtime', 'adapter-config-identity.cjs'));

var CONFIG_FILE = path.join(paths.PROJECT_ROOT, 'orchestrator', 'figma', 'project-adapters.json');
var MAPPING_FILE = path.join(paths.PROJECT_ROOT, 'orchestrator', 'figma', 'token-mappings.json');
var CONFIG_MAX = 256 * 1024;
var MAPPING_MAX = 8 * 1024 * 1024;

var projectDirty = false;
var projectDirtyReason = null;

function sha(bytes) { return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'); }

function readBounded(file, max) {
  var directory = path.dirname(file);
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, directory, file);
  if (inspected && inspected.status === 'missing') return { status: 'absent' };
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, file, max);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return { status: 'unreadable' };
  return { status: 'present', bytes: hit.bytes };
}

// Adapter config presence signal. Full semantic validation belongs to the
// trusted runner; the projection only distinguishes the three states whose
// next action differs (configure / fix file / proceed).
function adaptersSignal() {
  var read = readBounded(CONFIG_FILE, CONFIG_MAX);
  if (read.status === 'absent') return { state: 'unconfigured', fileHash: null };
  if (read.status === 'unreadable') return { state: 'unreadable', fileHash: null };
  var document;
  try { document = JSON.parse(read.bytes.toString('utf8')); } catch (error) {
    return { state: 'unreadable', fileHash: sha(read.bytes) };
  }
  if (!document || document.schemaVersion !== 2 || !Array.isArray(document.adapters)) {
    return { state: 'unreadable', fileHash: sha(read.bytes) };
  }
  var enabled = document.adapters.filter(function (adapter) {
    return adapter && adapter.enabled === true &&
      Array.isArray(adapter.capabilities) && adapter.capabilities.indexOf('tokens') >= 0;
  }).length;
  var domainHash;
  try { domainHash = adapterConfigIdentity.capabilityHash(document, 'tokens'); }
  catch (error) { return { state: 'unreadable', fileHash: sha(read.bytes) }; }
  if (!enabled) return { state: 'unconfigured', fileHash: domainHash };
  return { state: 'configured', fileHash: domainHash };
}

// Mapping registry revision signal. An absent file is the exact revision-0
// empty registry; a malformed file is 'invalid' and must surface as its own
// state, never as empty.
function mappingSignal() {
  var read = readBounded(MAPPING_FILE, MAPPING_MAX);
  if (read.status === 'absent') return { state: 'absent', revision: 0, fileHash: null };
  if (read.status === 'unreadable') return { state: 'invalid', revision: null, fileHash: null };
  var document;
  try { document = JSON.parse(read.bytes.toString('utf8')); } catch (error) {
    return { state: 'invalid', revision: null, fileHash: sha(read.bytes) };
  }
  if (!document || document.schemaVersion !== 2 || !document.scope ||
      typeof document.scope.fileKeyFingerprint !== 'string' || typeof document.scope.branchKey !== 'string' ||
      !Number.isSafeInteger(document.revision) || document.revision < 0) {
    return { state: 'invalid', revision: null, fileHash: sha(read.bytes) };
  }
  return { state: 'present', revision: document.revision, fileHash: sha(read.bytes) };
}

function readTokenSignals() {
  var adapters = adaptersSignal();
  var mapping = mappingSignal();
  return {
    adapters: adapters,
    configFileHash: adapters.fileHash,
    mappingState: mapping.state,
    mappingRevision: mapping.revision,
    mappingFileHash: mapping.fileHash,
    projectDirty: projectDirty,
    projectDirtyReason: projectDirtyReason
  };
}

function markProjectDirty(reason) {
  projectDirty = true;
  projectDirtyReason = String(reason || 'project-sources-changed').slice(0, 120);
}

function clearProjectDirty() {
  projectDirty = false;
  projectDirtyReason = null;
}

module.exports = {
  CONFIG_FILE: CONFIG_FILE,
  MAPPING_FILE: MAPPING_FILE,
  readTokenSignals: readTokenSignals,
  markProjectDirty: markProjectDirty,
  clearProjectDirty: clearProjectDirty
};
