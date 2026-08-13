'use strict';

// Durable idempotency reservations for typed Task Details actions. The public
// key is never a path segment; a hash names one immutable intent slot. A crash
// while the slot is pending fails closed as recovery-required rather than
// risking a second workspace mutation.

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var TextDecoder = require('util').TextDecoder;
var paths = require('./paths');
var fileGuards = require('./file-guards');
var primaryAction = require('./task-primary-action');

var DIR = paths.TASK_ACTION_RECEIPTS_DIR;
var MAX_FILES = 1000;
var MAX_BYTES = 16 * 1024;
var NAME_RE = /^[a-f0-9]{64}\.json$/;
var GUARD_PRIVATE_RE = /^(?:\.guard-txn-[a-f0-9]{64}(?:\.json(?:\.stage)?|\.decision\.json(?:\.stage)?|\.receipt\.json(?:\.stage)?)|\.guard-(?:transfer|publish)-[a-f0-9]{64}(?:\.json(?:\.stage)?|\.link\.json(?:\.stage)?|\.receipt\.json(?:\.stage)?)|\.guard-(?:(?:transfer-)?capture|publish-data|cas-old)-[a-f0-9]{32})$/;

function parseJsonBytes(bytes) {
  return JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(bytes));
}

function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(String(value), 'utf8').digest('hex');
}

function fileFor(key) {
  return path.join(DIR, hash(key).slice('sha256:'.length) + '.json');
}

function recordIssue(value, expectedKeyHash) {
  var fields = ['schemaVersion', 'keyHash', 'bodyHash', 'nonce', 'state', 'response', 'createdAt'];
  if (!value || typeof value !== 'object' || Array.isArray(value) ||
      Object.keys(value).sort().join('\0') !== fields.sort().join('\0') ||
      value.schemaVersion !== 1 || value.keyHash !== expectedKeyHash ||
      !/^sha256:[a-f0-9]{64}$/.test(value.bodyHash || '') ||
      !/^[a-f0-9]{32}$/.test(value.nonce || '') ||
      ['pending', 'completed'].indexOf(value.state) < 0 ||
      typeof value.createdAt !== 'string' || !Number.isFinite(Date.parse(value.createdAt)) ||
      (value.state === 'pending' && value.response !== null) ||
      (value.state === 'completed' && (!value.response || typeof value.response !== 'object' ||
        Array.isArray(value.response)))) return 'task action receipt is invalid';
  return null;
}

function readHeld(key) {
  var keyHash = hash(key);
  var file = fileFor(key);
  var bounded = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, DIR, file, MAX_BYTES);
  if (!bounded) return null;
  var value;
  try { value = parseJsonBytes(bounded.bytes); } catch (_) { return null; }
  return recordIssue(value, keyHash) ? null : {
    value: value, bytes: bounded.bytes, proof: bounded.stat
  };
}

function read(key) {
  var held = readHeld(key);
  return held ? held.value : null;
}

function bytes(value) {
  var result = Buffer.from(primaryAction.canonical(value) + '\n', 'utf8');
  return result.length <= MAX_BYTES ? result : null;
}

function sameProof(left, right) {
  return !!left && !!right &&
    ['dev', 'ino', 'modeExact', 'sizeExact', 'mtimeNs', 'ctimeNs', 'nlink'].every(function (field) {
      return String(left[field]) === String(right[field]);
    });
}

function directoryReady() {
  var failure = {};
  var proof = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, DIR, {
    create: true, mode: 0o700, failure: failure
  });
  // Two Site processes may create the idempotency authority on the same first
  // request and then publish into it concurrently. The losing proof may observe
  // either the create race or a stale directory stat caused by the winner's
  // publication. Re-walk a bounded number of times without creation and adopt
  // only a complete fresh proof; durability/unsafe-path failures are not retried.
  var race = [
    'guard-component-raced', 'guard-component-proof-stale',
    'guard-chain-raced', 'guard-root-proof-stale'
  ].indexOf(failure.code) >= 0;
  for (var attempt = 0; !proof && race && attempt < 4; attempt++) {
    proof = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, DIR);
  }
  return !!(proof && proof.exists);
}

function boundedNames() {
  if (!directoryReady()) return null;
  var names;
  try { names = fs.readdirSync(DIR).sort(); } catch (_) { return null; }
  if (names.some(function (name) {
    return !NAME_RE.test(name) && !GUARD_PRIVATE_RE.test(name);
  })) return null;
  var receipts = names.filter(function (name) { return NAME_RE.test(name); });
  return receipts.length <= MAX_FILES ? receipts : null;
}

function reserve(request) {
  if (!request || !request.idempotencyKey) return { ok: true, handle: null };
  var key = request.idempotencyKey;
  var keyHash = hash(key);
  var bodyHash = hash(primaryAction.canonical(request));
  var existing = read(key);
  if (existing) {
    if (existing.bodyHash !== bodyHash) {
      return { ok: false, status: 409, error: 'idempotency-key-conflict' };
    }
    if (existing.state === 'completed') {
      return { ok: true, replay: true, response: Object.assign({}, existing.response, {
        idempotentReplay: true
      }) };
    }
    return { ok: false, status: 409, error: 'task-action-recovery-required' };
  }
  var names = boundedNames();
  if (!names) return { ok: false, status: 503, error: 'task-action-idempotency-unavailable' };
  if (names.length >= MAX_FILES) {
    return { ok: false, status: 503, error: 'task-action-idempotency-retention-exhausted' };
  }
  var record = {
    schemaVersion: 1,
    keyHash: keyHash,
    bodyHash: bodyHash,
    nonce: crypto.randomBytes(16).toString('hex'),
    state: 'pending',
    response: null,
    createdAt: new Date().toISOString()
  };
  var payload = bytes(record);
  var published = payload && fileGuards.publishNoClobberRegularFileUnder(
    paths.PROJECT_ROOT, DIR, fileFor(key), payload,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: MAX_BYTES }
  );
  if (!published || !published.ok) {
    existing = read(key);
    if (existing && existing.bodyHash === bodyHash && existing.state === 'completed') {
      return { ok: true, replay: true, response: Object.assign({}, existing.response, {
        idempotentReplay: true
      }) };
    }
    return {
      ok: false,
      status: 409,
      error: existing && existing.bodyHash !== bodyHash
        ? 'idempotency-key-conflict' : 'task-action-recovery-required'
    };
  }
  return {
    ok: true,
    handle: { key: key, keyHash: keyHash, bodyHash: bodyHash, nonce: record.nonce }
  };
}

function complete(handle, response) {
  if (!handle) return true;
  var held = readHeld(handle.key);
  var current = held && held.value;
  if (!current || current.state !== 'pending' || current.nonce !== handle.nonce ||
      current.keyHash !== handle.keyHash || current.bodyHash !== handle.bodyHash) return false;
  var next = Object.assign({}, current, {
    state: 'completed',
    response: Object.assign({}, response, { idempotentReplay: false })
  });
  var payload = bytes(next);
  if (!payload) return false;
  var replaced = fileGuards.compareAndSwapRegularFileUnder(
    paths.PROJECT_ROOT, DIR, fileFor(handle.key), MAX_BYTES,
    { proof: held.proof, bytes: held.bytes }, payload, { mode: 0o600 });
  if (!replaced || !replaced.ok) return false;
  var proven = readHeld(handle.key);
  return !!(proven && proven.bytes.equals(payload) && sameProof(proven.proof, replaced.stat) &&
    proven.value.state === 'completed' && proven.value.nonce === handle.nonce &&
    proven.value.bodyHash === handle.bodyHash);
}

function release(handle) {
  if (!handle) return true;
  return fileGuards.unlinkRegularFileIfUnder(
    paths.PROJECT_ROOT, DIR, fileFor(handle.key), MAX_BYTES,
    function (bounded) {
      var value;
      try { value = parseJsonBytes(bounded.bytes); } catch (_) { return false; }
      return !recordIssue(value, handle.keyHash) && value.state === 'pending' &&
        value.nonce === handle.nonce && value.bodyHash === handle.bodyHash;
    }
  );
}

module.exports = Object.freeze({
  reserve: reserve,
  complete: complete,
  release: release,
  read: read,
  recordIssue: recordIssue
});
