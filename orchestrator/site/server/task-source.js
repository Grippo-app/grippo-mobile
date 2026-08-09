'use strict';

// Canonical task provenance reader/renderer used by domain-owned task
// producers. The browser never supplies this Markdown block: a domain server
// resolves and validates the source, then passes the typed envelope to the
// deterministic creator, which alone injects the canonical Markdown block.

var path = require('path');
var TextDecoder = require('util').TextDecoder;
var paths = require('./paths');
var fileGuards = require('./file-guards');
var contract = require('../../tasks/task-source-contract.cjs');
var outcomeShape = require('../../contracts/outcome-shape.json');

var HASH_RE = contract.HASH_RE;
var STEM_RE = contract.STEM_RE;
var SOURCE_TYPES = contract.SOURCE_TYPES;
var safeTaskStem = contract.safeTaskStem;
var OUTCOME_STATUSES = Object.freeze(outcomeShape.statusValid.slice());
var INDEX_MAX = 8 * 1024 * 1024;
var TASK_MAX = 128 * 1024;
var MAX_TASK_ROWS = 10000;
var MAX_TASK_SCAN_BYTES = 64 * 1024 * 1024;
var INDEX_FIELDS = ['backlog', 'done', 'generatedAt', 'pending', 'todo', 'version'];
var ROW_FIELDS = ['createdAt', 'dependsOn', 'doneAt', 'origin', 'outcomeStatus', 'questionsCount',
  'round', 'sourceRevision', 'splitFrom', 'state', 'stem', 'title'];
var TASKS_AUTHORITY_ROOT = path.resolve(process.env.ORCHESTRATOR_PROJECT_ROOT
  ? paths.PROJECT_ROOT
  : (process.env.ORCHESTRATOR_TASKS_DIR ? path.dirname(paths.TASKS_DIR) : paths.PROJECT_ROOT));

function exactKeys(value, fields) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === fields.slice().sort().join('\0');
}

function exactTaskInstant(value) {
  if (typeof value !== 'string') return false;
  var match = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,3}))?Z$/.exec(value);
  if (!match || !Number.isFinite(Date.parse(value))) return false;
  var canonical = match[1] + '.' + String(match[2] || '').padEnd(3, '0') + 'Z';
  return new Date(value).toISOString() === canonical;
}

function validIndexRow(row, column) {
  var outcomeStatusValid = column === 'done'
    ? OUTCOME_STATUSES.indexOf(row && row.outcomeStatus) >= 0
    : row && row.outcomeStatus === null;
  if (!exactKeys(row, ROW_FIELDS) || !safeTaskStem(row.stem) || row.state !== column ||
      typeof row.title !== 'string' || !row.title || !HASH_RE.test(String(row.sourceRevision || '')) ||
      !exactTaskInstant(row.createdAt) || (column === 'done' ? !exactTaskInstant(row.doneAt) : row.doneAt !== null) ||
      !validate(row.origin) || !Array.isArray(row.dependsOn) ||
      row.dependsOn.some(function (stem) { return !safeTaskStem(stem); }) ||
      new Set(row.dependsOn).size !== row.dependsOn.length ||
      (row.splitFrom !== null && !safeTaskStem(row.splitFrom)) ||
      !outcomeStatusValid) return false;
  return column === 'pending'
    ? Number.isSafeInteger(row.questionsCount) && row.questionsCount >= 0 && Number.isSafeInteger(row.round) && row.round >= 1
    : row.questionsCount === null && row.round === null;
}

function sha(value) {
  return contract.sha256(value);
}

function validate(source) {
  return contract.validate(source);
}

function render(source) {
  return contract.render(source);
}

function inject(body, source) {
  return contract.injectBody(body, source);
}

function parse(markdown) {
  return contract.parse(markdown);
}

function readIndex() {
  try {
    var file = path.join(paths.TASKS_DIR, 'INDEX.json');
    var hit = fileGuards.boundedRegularFileUnder(TASKS_AUTHORITY_ROOT, paths.TASKS_DIR, file, INDEX_MAX);
    if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') return null;
    var value = JSON.parse(hit.bytes.toString('utf8'));
    if (!exactKeys(value, INDEX_FIELDS) || value.version !== 2 || !exactTaskInstant(value.generatedAt)) return null;
    var rows = [];
    var columns = ['backlog', 'pending', 'todo', 'done'];
    if (columns.some(function (column) { return !Array.isArray(value[column]) || value[column].some(function (row) { return !validIndexRow(row, column); }); })) return null;
    if (columns.reduce(function (total, column) { return total + value[column].length; }, 0) > MAX_TASK_ROWS) {
      return null;
    }
    var seen = Object.create(null), valid = true;
    columns.forEach(function (column) {
      value[column].forEach(function (row) {
        var stem = row && typeof row.stem === 'string' ? row.stem : '';
        if (!safeTaskStem(stem) || seen[stem]) { valid = false; return; }
        seen[stem] = 1;
        rows.push({ column: column, row: row });
      });
    });
    if (!valid) return null;
    return { bytes: hit.bytes, value: value, rows: rows, revision: sha(hit.bytes) };
  } catch (error) { return null; }
}

function readTask(column, stem) {
  if (['backlog', 'pending', 'todo', 'done'].indexOf(column) < 0 || !safeTaskStem(stem)) return null;
  try {
    var directory = path.join(paths.TASKS_DIR, column);
    var file = path.join(directory, stem + '.md');
    var hit = fileGuards.boundedRegularFileUnder(TASKS_AUTHORITY_ROOT, directory, file, TASK_MAX);
    return hit && hit.stat && String(hit.stat.nlink) === '1'
      ? { bytes: hit.bytes, text: new TextDecoder('utf-8', { fatal: true }).decode(hit.bytes) } : null;
  } catch (error) { return null; }
}

function scanOpen() {
  var index = readIndex();
  if (!index) return {
    ok: false, historyOk: false, revision: null,
    items: [], byRef: Object.create(null), allItems: [], allByRef: Object.create(null),
    malformed: [], historyMalformed: [], error: 'task-index-invalid'
  };
  var items = [], byRef = Object.create(null), allItems = [], allByRef = Object.create(null);
  var malformed = [], historyMalformed = [];
  var taskFingerprints = [], totalBytes = 0, scanLimitExceeded = false;
  index.rows.forEach(function (entry) {
    if (totalBytes >= MAX_TASK_SCAN_BYTES) {
      scanLimitExceeded = true;
      taskFingerprints.push({ stem: entry.row.stem, column: entry.column, hash: null });
      historyMalformed.push(entry.row.stem);
      if (entry.column !== 'done') malformed.push(entry.row.stem);
      return;
    }
    var task = readTask(entry.column, entry.row.stem);
    if (task === null) {
      taskFingerprints.push({ stem: entry.row.stem, column: entry.column, hash: null });
      historyMalformed.push(entry.row.stem);
      if (entry.column !== 'done') malformed.push(entry.row.stem);
      return;
    }
    if (totalBytes + task.bytes.length > MAX_TASK_SCAN_BYTES) {
      scanLimitExceeded = true;
      taskFingerprints.push({ stem: entry.row.stem, column: entry.column, hash: null });
      historyMalformed.push(entry.row.stem);
      if (entry.column !== 'done') malformed.push(entry.row.stem);
      return;
    }
    totalBytes += task.bytes.length;
    taskFingerprints.push({ stem: entry.row.stem, column: entry.column, hash: sha(task.bytes) });
    var parsed = parse(task.text);
    if (!parsed.valid) {
      historyMalformed.push(entry.row.stem);
      if (entry.column !== 'done') malformed.push(entry.row.stem);
      return;
    }
    var sourceRefs = [parsed.source.ref];
    if (parsed.source.kind === 'api' && parsed.source.type === 'api-work-package') {
      sourceRefs = sourceRefs.concat(parsed.package.value.sourceIds);
    }
    var item = {
      stem: entry.row.stem,
      title: typeof entry.row.title === 'string' ? entry.row.title : entry.row.stem,
      column: entry.column,
      source: parsed.source
    };
    allItems.push(item);
    sourceRefs.forEach(function (ref) {
      if (!allByRef[ref]) allByRef[ref] = [];
      allByRef[ref].push(item);
    });
    if (entry.column === 'done') return;
    items.push(item);
    sourceRefs.forEach(function (ref) {
      if (!byRef[ref]) byRef[ref] = [];
      byRef[ref].push(item);
    });
  });
  return {
    ok: malformed.length === 0,
    revision: sha(JSON.stringify({
      index: sha(index.bytes),
      tasks: taskFingerprints
    })),
    items: items,
    byRef: byRef,
    allItems: allItems,
    allByRef: allByRef,
    malformed: malformed.slice(0, 50),
    historyOk: historyMalformed.length === 0,
    historyMalformed: historyMalformed.slice(0, 50),
    scanLimitExceeded: scanLimitExceeded,
    error: malformed.length ? scanLimitExceeded ? 'task-source-scan-limit' : 'task-source-partial' : null
  };
}

module.exports = {
  HASH_RE: HASH_RE,
  STEM_RE: STEM_RE,
  SOURCE_TYPES: SOURCE_TYPES,
  safeTaskStem: safeTaskStem,
  sha: sha,
  validate: validate,
  render: render,
  inject: inject,
  parse: parse,
  manualForIntent: contract.manualForIntent,
  followUp: contract.followUp,
  same: contract.same,
  validIndexRow: validIndexRow,
  readIndex: readIndex,
  readTask: readTask,
  scanOpen: scanOpen
};
