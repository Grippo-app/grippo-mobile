'use strict';

// ---------------------------------------------------------------------------
// Per-task pipeline journal reader (read-only feed for Task Details → Activity's
// phase timeline). The pipeline agents append events to
// orchestrator/.cache/tasks/journal/<STEM>.jsonl via orchestrator/tasks/log-event.py
// (one JSON object per line; task-journal-contract.cjs is the shared enum/schema
// SOURCE OF TRUTH used by both writer and reader). The site only ever READS
// these local files.
//
//   readLog(stem) → { stem, events: [ <event> ], truncated }
//
// Keyed by stem so it follows a task across column moves, exactly like
// .cache/tasks/locks/<STEM>.json. The .cache/tasks/journal/ dir is dot-denied by static.safeResolve,
// so this purpose-built reader — guarded by the shared canonical journal
// contract — is the only way journal lines reach the browser.
//
// Tolerant but not permissive: a partial/corrupt/non-canonical line is omitted
// and marks the response truncated. Unknown fields are never passed through to
// the browser. The file is read through a rooted bounded-tail transaction, so
// symlinks, hardlinks, ancestor swaps and oversized history cannot redirect or
// exhaust the request path.
// ---------------------------------------------------------------------------

var fs = require('fs');
var path  = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var journalContract = require('../../tasks/task-journal-contract.cjs');

var JOURNAL_DIR  = paths.JOURNAL_DIR;

// Defensive upper bound on events returned for one task. A normal run emits a
// few dozen; this only guards a runaway/hand-edited file. When tripped we keep
// the MOST RECENT events — the tail is what the timeline cares about.
var MAX_EVENTS = 2000;
var MAX_TAIL_BYTES = 4 * 1024 * 1024;
var LATEST_TAIL_BYTES = 64 * 1024;
var REVISION_ENTRY_MAX = 10000;

// SSE invalidation must observe appends to an existing journal file. Directory
// mtime alone changes only when a dirent is added/removed, so retain a bounded
// max(file mtime,size) watermark as well. This never reads journal contents.
function revision() {
  try {
    var directory = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, JOURNAL_DIR, { allowMissing: true });
    if (!directory || !directory.exists) return '0:0:0:0';
    var names = fs.readdirSync(JOURNAL_DIR).filter(function (name) { return name.endsWith('.jsonl'); }).sort();
    if (names.length > REVISION_ENTRY_MAX) return directory.stat.mtimeMs + ':limit:' + names.length;
    var newest = 0, newestSize = 0, totalSize = 0, unsafe = 0;
    names.forEach(function (name) {
      var entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, JOURNAL_DIR, path.join(JOURNAL_DIR, name));
      if (!entry || entry.status !== 'present' || !entry.stat || !entry.stat.isFile() ||
          entry.stat.isSymbolicLink() || String(entry.stat.nlink) !== '1') { unsafe++; return; }
      totalSize += entry.stat.size;
      if (entry.stat.mtimeMs > newest || entry.stat.mtimeMs === newest && entry.stat.size > newestSize) {
        newest = entry.stat.mtimeMs; newestSize = entry.stat.size;
      }
    });
    return [directory.stat.mtimeMs, newest, newestSize, totalSize, names.length, unsafe].join(':');
  } catch (_) { return 'unavailable'; }
}

function readLog(stem) {
  if (!journalContract.validStem(stem)) {
    return { stem: null, events: [], truncated: false };
  }
  var file = path.join(JOURNAL_DIR, stem + '.jsonl');
  var entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, JOURNAL_DIR, file);
  if (!entry || entry.status === 'missing') return { stem: stem, events: [], truncated: false };
  if (entry.status !== 'present' || !entry.stat || !entry.stat.isFile() || entry.stat.isSymbolicLink() || String(entry.stat.nlink) !== '1') {
    return { stem: stem, events: [], truncated: true };
  }
  var bytes = fileGuards.tailRegularFileUnder(paths.PROJECT_ROOT, JOURNAL_DIR, file, MAX_TAIL_BYTES);
  if (!bytes) return { stem: stem, events: [], truncated: true };
  var byteTruncated = bytes.length === MAX_TAIL_BYTES || entry.stat.size > bytes.length;
  var raw;
  try { raw = new TextDecoder('utf-8', { fatal: true }).decode(bytes); }
  catch (utf8Error) { return { stem: stem, events: [], truncated: true }; }
  var lines = raw.split('\n');
  if (byteTruncated && lines.length) lines.shift();
  if (raw && !raw.endsWith('\n') && lines.length) lines.pop();
  var events = [];
  var omitted = byteTruncated || (raw && !raw.endsWith('\n'));
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i];
    if (!line) continue;                                   // blank / trailing newline
    var ev;
    try { ev = JSON.parse(line); } catch (e) { omitted = true; continue; }
    var projected = journalContract.publicEvent(ev, stem);
    if (!projected) {
      omitted = true;
      continue;
    }
    events.push(projected);
  }
  var truncated = omitted;
  if (events.length > MAX_EVENTS) {
    events = events.slice(events.length - MAX_EVENTS);
    truncated = true;
  }
  return { stem: stem, events: events, truncated: truncated };
}

// Summary cards need only one semantic event. Keep that hot path bounded to a
// small tail and scan newest-first; a malformed suffix marks the projection as
// partial but never fabricates a clean activity state.
function readLatest(stem) {
  if (!journalContract.validStem(stem)) return { event: null, truncated: false };
  var file = path.join(JOURNAL_DIR, stem + '.jsonl');
  var entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, JOURNAL_DIR, file);
  if (!entry || entry.status === 'missing') return { event: null, truncated: false };
  if (entry.status !== 'present' || !entry.stat || !entry.stat.isFile() ||
      entry.stat.isSymbolicLink() || String(entry.stat.nlink) !== '1') {
    return { event: null, truncated: true };
  }
  var bytes = fileGuards.tailRegularFileUnder(paths.PROJECT_ROOT, JOURNAL_DIR, file, LATEST_TAIL_BYTES);
  if (!bytes) return { event: null, truncated: true };
  var raw;
  try { raw = new TextDecoder('utf-8', { fatal: true }).decode(bytes); }
  catch (_) { return { event: null, truncated: true }; }
  var truncated = entry.stat.size > bytes.length || (raw && !raw.endsWith('\n'));
  var lines = raw.split('\n');
  if (entry.stat.size > bytes.length && lines.length) lines.shift();
  if (raw && !raw.endsWith('\n') && lines.length) lines.pop();
  for (var i = lines.length - 1; i >= 0; i--) {
    if (!lines[i]) continue;
    try {
      var projected = journalContract.publicEvent(JSON.parse(lines[i]), stem);
      if (projected) return { event: projected, truncated: truncated };
    } catch (_) {}
    truncated = true;
  }
  return { event: null, truncated: truncated };
}

module.exports = {
  MAX_EVENTS: MAX_EVENTS,
  readLog: readLog,
  readLatest: readLatest,
  revision: revision
};
