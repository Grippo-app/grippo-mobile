'use strict';

// Canonical test-task input identity. The structural Outcome parser remains
// owned by task-state-core; this adapter only freezes the exact byte domain
// used by certification and finalization. It applies the same trailing-space
// canonicalization as Outcome installation, so adding/replacing the anchored
// Outcome cannot stale evidence while task-body edits do.

const crypto = require('crypto');
const taskState = require('./task-state-core.cjs');

const DOMAIN = 'test-task-input';

class TestTaskInputError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'TestTaskInputError';
    this.code = code;
  }
}

function fail(code, message) { throw new TestTaskInputError(code, message); }

function canonicalTaskInput(value) {
  const bytes = Buffer.isBuffer(value) ? Buffer.from(value) : Buffer.from(String(value == null ? '' : value));
  let text;
  try { text = new TextDecoder('utf-8', { fatal: true }).decode(bytes); }
  catch (_) { fail('TASK_INPUT_INVALID', 'task input must be valid UTF-8'); }
  if (text.startsWith('\uFEFF') || text.includes('\r')) {
    fail('TASK_INPUT_INVALID', 'task input must use canonical UTF-8 without BOM and LF line endings');
  }
  const outcomeStart = taskState.outcomeAppendixStart(text);
  const prefix = outcomeStart < 0 ? text : text.slice(0, outcomeStart);
  return Buffer.from(prefix.trimEnd() + '\n');
}

function taskInputHashOf(value) {
  return 'sha256:' + crypto.createHash('sha256')
    .update(DOMAIN + '\0')
    .update(canonicalTaskInput(value))
    .digest('hex');
}

module.exports = {
  taskInputHashOf
};
