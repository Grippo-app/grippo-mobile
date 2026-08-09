'use strict';

// ---------------------------------------------------------------------------
// Test-foundation contract (pipeline improvement 05, Phase 2).
//
// Pure, filesystem-free authority for the doctor's typed states and the
// bootstrap coordinator's durable marker protocol (§10 of the plan). The
// runtime engine (task-test-foundation.mjs) enforces filesystem safety; this
// module owns shapes, domains and transitions so tests and future consumers
// validate against one source.
//
// Hash domains (never cross-compared with any other namespace):
//   test-foundation-intent\0    — global bootstrap intent identity
//   test-foundation-inventory\0 — doctor inventory content hash
//   test-foundation-marker\0    — marker body integrity hash
// ---------------------------------------------------------------------------

const crypto = require('crypto');

const DOCTOR_STATES = Object.freeze([
  'READY',
  'ABSENT_CAN_INSTALL',
  'PARTIAL_CORRUPT',
  'CONFLICTING_STACK',
  'UNSUPPORTED_VERSION',
  'TOOLCHAIN_UNAVAILABLE'
]);

const MARKER_PHASES = Object.freeze(['claimed', 'child-created', 'child-promoted', 'ready']);
const MARKER_DOMAIN = 'test-foundation-marker';
const INTENT_DOMAIN = 'test-foundation-intent';
const INVENTORY_DOMAIN = 'test-foundation-inventory';
const MARKER_KEYS = Object.freeze([
  'childStem', 'createdAt', 'domain', 'intentHash', 'markerHash',
  'ownerPid', 'ownerSessionId', 'ownerStartedAt', 'phase', 'updatedAt', 'version'
]);
const MARKER_MAX_BYTES = 4096;
const HASH_RE = /^sha256:[0-9a-f]{64}$/;
const HEX64_RE = /^[0-9a-f]{64}$/;
const STEM_RE = /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/;
const ISO_RE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/;
const SESSION_RE = /^[A-Za-z0-9][A-Za-z0-9._:-]{0,162}$/;

class TestFoundationError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'TestFoundationError';
    this.code = code;
  }
}

function fail(code, message) { throw new TestFoundationError(code, message); }

function sha256(value) {
  return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex');
}

function canonicalJson(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
  return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
}

// Global bootstrap intent: one absent foundation + one policy = one intent.
// Two concurrent preps that see the same absent product MUST derive the same
// hash, which is what makes the coordinator's no-clobber marker a singleton.
function foundationIntentHash(policyHash, doctorInventoryHash) {
  if (typeof policyHash !== 'string' || !HASH_RE.test(policyHash)) fail('INTENT_INVALID', 'policyHash grammar');
  if (typeof doctorInventoryHash !== 'string' || !HASH_RE.test(doctorInventoryHash)) fail('INTENT_INVALID', 'doctorInventoryHash grammar');
  return sha256(INTENT_DOMAIN + '\0' + policyHash + '\0' + doctorInventoryHash);
}

function inventoryHashOf(inventory) {
  if (!inventory || typeof inventory !== 'object' || Array.isArray(inventory)) fail('INVENTORY_INVALID', 'inventory must be an object');
  return sha256(INVENTORY_DOMAIN + '\0' + canonicalJson(inventory));
}

// The marker's on-disk path component is exactly the 64 lowercase hex of the
// intent hash — anything else is rejected before the filesystem is touched.
function markerPathComponent(intentHash) {
  if (typeof intentHash !== 'string' || !HASH_RE.test(intentHash)) fail('INTENT_INVALID', 'intentHash grammar');
  const hex = intentHash.slice('sha256:'.length);
  if (!HEX64_RE.test(hex)) fail('INTENT_INVALID', 'intent hash hex component');
  return hex;
}

function markerHashOf(marker) {
  const model = {};
  for (const key of Object.keys(marker)) {
    if (key !== 'markerHash') model[key] = marker[key];
  }
  return sha256(MARKER_DOMAIN + '\0' + canonicalJson(model));
}

function validateMarker(marker) {
  if (!marker || typeof marker !== 'object' || Array.isArray(marker)) fail('MARKER_INVALID', 'marker must be an object');
  const keys = Object.keys(marker).sort();
  if (keys.length !== MARKER_KEYS.length || keys.some((key, i) => key !== MARKER_KEYS[i])) {
    fail('MARKER_INVALID', 'marker keys must be exactly ' + MARKER_KEYS.join(','));
  }
  if (marker.version !== 1) fail('MARKER_INVALID', 'unsupported version');
  if (marker.domain !== MARKER_DOMAIN) fail('MARKER_INVALID', 'unsupported domain');
  if (!HASH_RE.test(String(marker.intentHash))) fail('MARKER_INVALID', 'intentHash grammar');
  if (!MARKER_PHASES.includes(marker.phase)) fail('MARKER_INVALID', 'unknown phase: ' + marker.phase);
  if (typeof marker.ownerSessionId !== 'string' || !SESSION_RE.test(marker.ownerSessionId)) fail('MARKER_INVALID', 'ownerSessionId grammar');
  if (!Number.isSafeInteger(marker.ownerPid) || marker.ownerPid <= 0) fail('MARKER_INVALID', 'ownerPid must be a positive integer');
  if (typeof marker.ownerStartedAt !== 'string' || !ISO_RE.test(marker.ownerStartedAt)) fail('MARKER_INVALID', 'ownerStartedAt must be exact ISO-8601 ms');
  if (typeof marker.createdAt !== 'string' || !ISO_RE.test(marker.createdAt)) fail('MARKER_INVALID', 'createdAt must be exact ISO-8601 ms');
  if (typeof marker.updatedAt !== 'string' || !ISO_RE.test(marker.updatedAt)) fail('MARKER_INVALID', 'updatedAt must be exact ISO-8601 ms');
  if (marker.updatedAt < marker.createdAt) fail('MARKER_INVALID', 'updatedAt precedes createdAt');
  if (marker.phase === 'claimed') {
    if (marker.childStem !== null) fail('MARKER_INVALID', 'claimed phase carries no childStem');
  } else if (typeof marker.childStem !== 'string' || !STEM_RE.test(marker.childStem)) {
    fail('MARKER_INVALID', 'phase ' + marker.phase + ' requires a canonical childStem');
  }
  if (!HASH_RE.test(String(marker.markerHash))) fail('MARKER_INVALID', 'markerHash grammar');
  if (markerHashOf(marker) !== marker.markerHash) fail('HASH_MISMATCH', 'markerHash does not match marker content');
  const bytes = Buffer.byteLength(JSON.stringify(marker), 'utf8');
  if (bytes > MARKER_MAX_BYTES) fail('MARKER_INVALID', 'marker exceeds ' + MARKER_MAX_BYTES + ' bytes');
  return Object.freeze(JSON.parse(JSON.stringify(marker)));
}

// Bounded monotonic transitions. `ready` is terminal; nothing skips a phase,
// nothing moves backwards, and a transition never changes the child identity
// once it exists.
function checkTransition(fromMarker, toMarker) {
  const from = validateMarker(fromMarker);
  const to = validateMarker(toMarker);
  if (from.intentHash !== to.intentHash) fail('TRANSITION_INVALID', 'intent identity changed');
  if (from.ownerSessionId !== to.ownerSessionId || from.ownerPid !== to.ownerPid ||
      from.ownerStartedAt !== to.ownerStartedAt) {
    fail('TRANSITION_INVALID', 'owner identity changed without recovery');
  }
  if (from.createdAt !== to.createdAt) fail('TRANSITION_INVALID', 'createdAt is immutable');
  const fromIndex = MARKER_PHASES.indexOf(from.phase);
  const toIndex = MARKER_PHASES.indexOf(to.phase);
  if (toIndex !== fromIndex + 1) fail('TRANSITION_INVALID', from.phase + ' -> ' + to.phase + ' is not the next bounded phase');
  if (from.childStem !== null && from.childStem !== to.childStem) fail('TRANSITION_INVALID', 'childStem is immutable once set');
  if (to.updatedAt < from.updatedAt) fail('TRANSITION_INVALID', 'updatedAt moved backwards');
  return to;
}

module.exports = {
  DOCTOR_STATES,
  MARKER_PHASES,
  MARKER_DOMAIN,
  INTENT_DOMAIN,
  INVENTORY_DOMAIN,
  MARKER_KEYS,
  MARKER_MAX_BYTES,
  TestFoundationError,
  canonicalJson,
  sha256,
  foundationIntentHash,
  inventoryHashOf,
  markerPathComponent,
  markerHashOf,
  validateMarker,
  checkTransition
};
