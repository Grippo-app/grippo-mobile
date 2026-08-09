'use strict';

// Durable pre-Setup task inbox. Inbox records are deliberately outside
// orchestrator/tasks: saving one cannot mutate INDEX.json, enqueue work, or
// expose a runnable task. Publication after Setup reuses the deterministic
// backlog creator with a stable idempotency key.

var path = require('path');
var crypto = require('crypto');
var TextDecoder = require('util').TextDecoder;
var paths = require('./paths');
var fileGuards = require('./file-guards');
var backlogCreate = require('./backlog-create');
var taskSource = require('./task-source');

var ID_RE = /^INBOX_[a-f0-9]{40}$/;
var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var FILE_RE = /^(INBOX_[a-f0-9]{40})\.json$/;
var RECORD_MAX_BYTES = 72 * 1024;
var MAX_RECORDS = 10000;
var RECORD_FIELDS = [
  'body', 'createdAt', 'id', 'keyHash', 'payloadHash',
  'publishedAt', 'taskStem', 'title', 'version'
];

function sha(bytes) {
  return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex');
}

function exactKeys(value, fields) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === fields.slice().sort().join('\0');
}

function exactInstant(value) {
  if (typeof value !== 'string') return false;
  try { return new Date(value).toISOString() === value; }
  catch (error) { return false; }
}

function payloadHash(title, body) {
  return sha(Buffer.from(JSON.stringify({ title: title, body: body }), 'utf8'));
}

function validRecord(value) {
  if (!exactKeys(value, RECORD_FIELDS) || value.version !== 1 ||
      !ID_RE.test(String(value.id || '')) || !HASH_RE.test(String(value.keyHash || '')) ||
      value.id !== 'INBOX_' + value.keyHash.slice('sha256:'.length, 'sha256:'.length + 40) ||
      !HASH_RE.test(String(value.payloadHash || '')) ||
      typeof value.title !== 'string' || !value.title ||
      typeof value.body !== 'string' || !exactInstant(value.createdAt) ||
      value.payloadHash !== payloadHash(value.title, value.body)) return false;
  if (value.publishedAt === null || value.taskStem === null) {
    return value.publishedAt === null && value.taskStem === null;
  }
  return exactInstant(value.publishedAt) && taskSource.safeTaskStem(value.taskStem);
}

function codedError(code, status) {
  var error = new Error(code);
  error.code = code;
  error.httpStatus = status || 500;
  return error;
}

function recordPath(id) {
  if (!ID_RE.test(String(id || ''))) throw codedError('bad-inbox-id', 400);
  return path.join(paths.TASK_INBOX_DIR, id + '.json');
}

function jsonBytes(value) {
  return Buffer.from(JSON.stringify(value, null, 2) + '\n', 'utf8');
}

function readRecord(id) {
  var file = recordPath(id);
  var hit = fileGuards.boundedRegularFileUnder(
    paths.TASK_INBOX_AUTHORITY_ROOT, paths.TASK_INBOX_DIR, file, RECORD_MAX_BYTES
  );
  if (!hit) return null;
  var value;
  try {
    value = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(hit.bytes));
  } catch (error) {
    throw codedError('task-inbox-record-invalid');
  }
  if (!validRecord(value) || value.id !== id || !hit.stat || String(hit.stat.nlink) !== '1') {
    throw codedError('task-inbox-record-invalid');
  }
  return { value: value, bytes: hit.bytes, proof: hit.stat };
}

function normalizeInput(input) {
  if (!exactKeys(input, ['body', 'idempotencyKey', 'title'])) {
    throw codedError('bad-task-inbox-request', 400);
  }
  var intentId = typeof input.idempotencyKey === 'string'
    ? 'intent-' + crypto.createHash('sha256').update(input.idempotencyKey, 'utf8').digest('hex')
    : 'none';
  try {
    return backlogCreate.validateInput({
      title: input.title,
      body: input.body,
      idempotencyKey: input.idempotencyKey,
      originStem: null,
      dedupKey: null,
      dedupReport: null,
      source: taskSource.manualForIntent(intentId, 'manual', intentId)
    });
  } catch (error) {
    error.httpStatus = Number(error.httpStatus) || 400;
    throw error;
  }
}

function publicEntry(record) {
  return {
    id: record.id,
    title: record.title,
    createdAt: record.createdAt
  };
}

function save(input) {
  var normalized = normalizeInput(input);
  var keyHash = sha(Buffer.from(normalized.idempotencyKey, 'utf8'));
  var id = 'INBOX_' + keyHash.slice('sha256:'.length, 'sha256:'.length + 40);
  var record = {
    version: 1,
    id: id,
    keyHash: keyHash,
    payloadHash: payloadHash(normalized.title, normalized.body),
    title: normalized.title,
    body: normalized.body,
    createdAt: new Date().toISOString(),
    publishedAt: null,
    taskStem: null
  };
  var bytes = jsonBytes(record);
  var target = recordPath(id);
  var published = fileGuards.publishNoClobberRegularFileUnder(
    paths.TASK_INBOX_AUTHORITY_ROOT, paths.TASK_INBOX_DIR, target, bytes,
    { create: true, directoryMode: 0o700, mode: 0o600, maxBytes: RECORD_MAX_BYTES }
  );
  if (published && published.ok) {
    return { created: true, published: false, entry: publicEntry(record) };
  }
  if (published && (published.code === 'exists' || published.code === 'published-unverified')) {
    // A retry may be the first caller able to acknowledge a publication whose
    // original response was lost after link but before the final durability
    // proof. Re-sync both the exact file and its directory before treating the
    // existing generation as a successful idempotent save.
    if (!fileGuards.fsyncRegularFileUnder(
      paths.TASK_INBOX_AUTHORITY_ROOT, paths.TASK_INBOX_DIR, target
    ) || !fileGuards.fsyncDirectoryUnder(
      paths.TASK_INBOX_AUTHORITY_ROOT, paths.TASK_INBOX_DIR
    )) throw codedError('task-inbox-write-unverified');
    var existing = readRecord(id);
    if (!existing) throw codedError('task-inbox-write-unverified');
    if (existing.value.keyHash !== keyHash || existing.value.payloadHash !== record.payloadHash) {
      throw codedError('TASK_INBOX_IDEMPOTENCY_CONFLICT', 409);
    }
    return {
      created: false,
      published: existing.value.publishedAt !== null,
      taskStem: existing.value.taskStem,
      entry: existing.value.publishedAt === null ? publicEntry(existing.value) : null
    };
  }
  throw codedError('task-inbox-write-failed');
}

function list() {
  var listed = fileGuards.boundedDirectoryNamesUnder(
    paths.TASK_INBOX_AUTHORITY_ROOT, paths.TASK_INBOX_DIR, MAX_RECORDS + 1
  );
  if (!listed || !listed.ok) throw codedError('task-inbox-unavailable');
  if (listed.names.length > MAX_RECORDS) throw codedError('task-inbox-capacity-exceeded', 507);
  var entries = [];
  listed.names.forEach(function (name) {
    var match = FILE_RE.exec(name);
    // File-guard transaction evidence is private implementation state. Any
    // other foreign public entry fails closed instead of being interpreted.
    if (!match) {
      if (name.charAt(0) === '.') return;
      throw codedError('task-inbox-entry-invalid');
    }
    var held = readRecord(match[1]);
    if (held && held.value.publishedAt === null) entries.push(publicEntry(held.value));
  });
  entries.sort(function (left, right) {
    return Date.parse(left.createdAt) - Date.parse(right.createdAt) || left.id.localeCompare(right.id);
  });
  return { schemaVersion: 1, entries: entries };
}

function markPublished(held, stem) {
  var next = Object.assign({}, held.value, {
    publishedAt: new Date().toISOString(),
    taskStem: stem
  });
  if (!validRecord(next)) return false;
  var swapped = fileGuards.compareAndSwapRegularFileUnder(
    paths.TASK_INBOX_AUTHORITY_ROOT, paths.TASK_INBOX_DIR, recordPath(next.id),
    RECORD_MAX_BYTES, { proof: held.proof, bytes: held.bytes }, jsonBytes(next), { mode: 0o600 }
  );
  if (swapped && swapped.ok) return true;
  var current = readRecord(next.id);
  return !!(current && current.value.publishedAt !== null && current.value.taskStem === stem);
}

function publish(id) {
  var held = readRecord(id);
  if (!held) return Promise.reject(codedError('task-inbox-not-found', 404));
  if (held.value.publishedAt !== null) {
    return Promise.resolve({
      published: true,
      replayed: true,
      inboxId: id,
      stem: held.value.taskStem,
      inboxRetained: false
    });
  }
  var key = 'setup-inbox:' + id;
  var intentId = 'intent-' + crypto.createHash('sha256').update(key, 'utf8').digest('hex');
  return backlogCreate.create({
    title: held.value.title,
    body: held.value.body,
    idempotencyKey: key,
    originStem: null,
    dedupKey: null,
    dedupReport: null,
    source: taskSource.manualForIntent(intentId, 'manual', intentId)
  }).then(function (result) {
    var settled = markPublished(held, result.stem);
    return Object.assign({}, result, {
      published: true,
      inboxId: id,
      inboxRetained: !settled
    });
  });
}

function publicErrorCode(error) {
  var code = error && error.code;
  var allowed = {
    'bad-inbox-id': 1,
    'bad-task-inbox-request': 1,
    'bad-title': 1,
    'bad-body': 1,
    'bad-idempotency-key': 1,
    'TASK_INBOX_IDEMPOTENCY_CONFLICT': 1,
    'task-inbox-not-found': 1,
    'task-inbox-capacity-exceeded': 1,
    'task-inbox-record-invalid': 1,
    'task-inbox-entry-invalid': 1,
    'task-inbox-unavailable': 1,
    'task-inbox-write-unverified': 1,
    'task-inbox-write-failed': 1
  };
  if (allowed[code]) return code;
  return backlogCreate.publicCreateErrorCode(error);
}

module.exports = Object.freeze({
  save: save,
  list: list,
  publish: publish,
  publicErrorCode: publicErrorCode
});
