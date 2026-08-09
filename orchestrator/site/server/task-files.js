'use strict';

// Narrow, read-only task-body surface for the Details modal. Static assets are
// served from the installed orchestrator code root, while mutable task data may
// live under an explicit ORCHESTRATOR_PROJECT_ROOT. Keep those trust domains
// separate and expose only one validated direct child of a canonical column.

var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var taskSourceContract = require('../../tasks/task-source-contract.cjs');

var STEM_RE = taskSourceContract.STEM_RE;
var COLUMN_SUFFIX = Object.freeze({
  backlog: '.md',
  pending: '.questions.md',
  todo: '.md',
  done: '.md'
});
var MAX_TASK_BYTES = 128 * 1024;

function fail(code, status, message) {
  var error = new Error(message);
  error.code = code;
  error.httpStatus = status;
  throw error;
}

function publicErrorCode(error) {
  var code = error && error.code;
  return code === 'task-file-column-invalid' ||
    code === 'task-file-stem-invalid' ||
    code === 'task-file-not-found'
    ? code : 'task-file-unavailable';
}

function read(column, stem) {
  if (!Object.prototype.hasOwnProperty.call(COLUMN_SUFFIX, column)) {
    fail('task-file-column-invalid', 400, 'Task column is invalid.');
  }
  if (!taskSourceContract.safeTaskStem(stem)) {
    fail('task-file-stem-invalid', 400, 'Task stem is invalid.');
  }
  var directory = path.join(paths.TASKS_DIR, column);
  var file = path.join(directory, stem + COLUMN_SUFFIX[column]);
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, directory, file, MAX_TASK_BYTES);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') {
    fail('task-file-not-found', 404, 'Task file is unavailable.');
  }
  return { bytes: hit.bytes, column: column, stem: stem };
}

module.exports = Object.freeze({
  STEM_RE: STEM_RE,
  COLUMN_SUFFIX: COLUMN_SUFFIX,
  MAX_TASK_BYTES: MAX_TASK_BYTES,
  publicErrorCode: publicErrorCode,
  read: read
});
