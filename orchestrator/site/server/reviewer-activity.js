'use strict';

// Bounded projection of structured review journal events. INDEX.json is the
// only task inventory; terminal transcripts and free-form prompts are never
// parsed. One corrupt journal marks the aggregate partial without hiding valid
// activity from other tasks.

var crypto = require('crypto');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var tasksLog = require('./tasks-log');
var taskSource = require('./task-source');
var sessions = require('./sessions');
var journalContract = require('../../tasks/task-journal-contract.cjs');
var taskCore = require('../../tasks/task-state-core.cjs');
var outcomeShape = require('../../contracts/outcome-shape.json');

var MAX_TASKS = 500;
var MAX_DONE_BYTES = 1024 * 1024;
var MAX_HISTORY_PER_TASK = 100;
var MAX_HISTORY_ROWS = 10000;
var DEFAULT_LIMIT = 20;
var HARD_LIMIT = 100;
var CURSOR_SECRET = crypto.randomBytes(32);
var cache = null;
var revisionCache = { at: 0, value: 'unread' };

function validInstant(value) {
  if (typeof value !== 'string') return false;
  var match = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,3}))?Z$/.exec(value);
  if (!match || !Number.isFinite(Date.parse(value))) return false;
  var canonical = match[1] + '.' + String(match[2] || '').padEnd(3, '0') + 'Z';
  return new Date(value).toISOString() === canonical;
}

function readIndex() {
  var current;
  try { current = taskSource.readIndex(); }
  catch (error) { return { rows: [], partial: true, revision: 'invalid' }; }
  if (!current) return { rows: [], partial: true, revision: 'invalid' };
  var parsed = current.value;
  var order = ['todo', 'pending', 'done', 'backlog'];
  var rows = [];
  var seen = Object.create(null);
  var partial = false;
  var totalRows = 0;
  for (var countIndex = 0; countIndex < order.length; countIndex++) {
    var counted = parsed[order[countIndex]];
    if (!Array.isArray(counted)) return { rows: [], partial: true, revision: 'invalid' };
    totalRows += counted.length;
  }
  if (totalRows > MAX_TASKS) partial = true;
  for (var c = 0; c < order.length; c++) {
    var column = order[c];
    var list = parsed[column];
    for (var i = 0; i < list.length && rows.length < MAX_TASKS; i++) {
      var item = list[i];
      if (!item || !journalContract.validStem(item.stem) || seen[item.stem]) { partial = true; continue; }
      seen[item.stem] = true;
      rows.push({
        stem: item.stem,
        title: typeof item.title === 'string' && item.title ? item.title.slice(0, 240) : item.stem,
        column: column,
        doneAt: validInstant(item.doneAt) ? item.doneAt : null
      });
    }
  }
  return {
    rows: rows,
    partial: partial,
    revision: current.revision
  };
}

function statusFromEvent(event) {
  if (event.status === 'ok') return 'passed';
  if (event.status === 'blocked') return 'blocked';
  if (event.status === 'escalate') return 'escalated';
  if (event.status === 'fail') return 'failed';
  return 'unknown';
}

function attemptNumber(meta) {
  var raw = meta && meta.reviewAttempt;
  if (typeof raw !== 'string' || !/^[1-9][0-9]*$/.test(raw)) return null;
  var number = Number(raw);
  return Number.isSafeInteger(number) && number <= 99 ? number : null;
}

function reviewer(meta) {
  var value = meta && meta.reviewer;
  return journalContract.REVIEWERS.indexOf(value) >= 0 ? value : 'unknown';
}

function liveSessionKey(sessionMap, stem) {
  var key = 'task:' + stem;
  return sessionMap[key] && sessionMap[key].running === true ? key : null;
}

function takeOpenAttempt(open, number) {
  if (number === null) return null;
  for (var index = open.length - 1; index >= 0; index--) {
    if (open[index].reviewAttempt === number) return open.splice(index, 1)[0];
  }
  return null;
}

function projectTask(row, sessionMap) {
  var log = tasksLog.readLog(row.stem);
  var attempts = [];
  var open = [];
  var seenAttempts = Object.create(null);
  var waiting = null;
  var inconsistent = false;
  for (var i = 0; i < log.events.length; i++) {
    var event = log.events[i];
    if (event.phase !== 'review') continue;
    var meta = event.meta || {};
    var number = attemptNumber(meta);
    if (event.kind === 'gate' && event.status === 'info') {
      waiting = {
        taskStem: row.stem,
        taskTitle: row.title,
        column: row.column,
        reviewer: reviewer(meta),
        reviewAttempt: number,
        selectionReason: null,
        reasonCode: meta.reasonCode || null,
        startedAt: event.ts,
        finishedAt: null,
        status: 'pending',
        waitingToStart: true,
        sessionKey: liveSessionKey(sessionMap, row.stem)
      };
      continue;
    }
    if (event.kind === 'phase-start') {
      waiting = null;
      if (number === null || reviewer(meta) === 'unknown') {
        inconsistent = true;
        continue;
      }
      if (seenAttempts[number]) inconsistent = true;
      seenAttempts[number] = true;
      var started = {
        taskStem: row.stem,
        taskTitle: row.title,
        column: row.column,
        reviewer: reviewer(meta),
        reviewAttempt: number,
        selectionReason: meta.selectionReason || null,
        reasonCode: meta.reasonCode || null,
        startedAt: event.ts,
        finishedAt: null,
        status: 'pending',
        sessionKey: liveSessionKey(sessionMap, row.stem)
      };
      attempts.push(started);
      open.push(started);
      continue;
    }
    if (event.kind !== 'phase-end' && event.kind !== 'stop' && event.kind !== 'gate') continue;
    if (event.kind !== 'phase-end' && ['blocked', 'escalate', 'fail'].indexOf(event.status) < 0) continue;
    waiting = null;
    var target = takeOpenAttempt(open, number);
    if (!target) {
      inconsistent = true;
      target = {
        taskStem: row.stem,
        taskTitle: row.title,
        column: row.column,
        reviewer: reviewer(meta),
        reviewAttempt: number,
        selectionReason: null,
        reasonCode: null,
        startedAt: null,
        finishedAt: null,
        status: 'unknown',
        sessionKey: liveSessionKey(sessionMap, row.stem)
      };
      attempts.push(target);
    }
    var terminalReviewer = reviewer(meta);
    if (target.reviewer !== 'unknown' && terminalReviewer !== 'unknown' &&
        target.reviewer !== terminalReviewer) {
      inconsistent = true;
    } else if (target.reviewer === 'unknown' && terminalReviewer !== 'unknown') {
      target.reviewer = terminalReviewer;
    }
    target.finishedAt = event.ts;
    target.status = statusFromEvent(event);
    target.reasonCode = meta.reasonCode || target.reasonCode;
  }
  // Keep the active projection bounded to the newest row for each possible
  // reviewer identity while preserving cross-reviewer conflicts.
  var active = [];
  var seenReviewers = Object.create(null);
  for (var a = open.length - 1; a >= 0; a--) {
    var activeReviewer = open[a].reviewer || 'unknown';
    if (seenReviewers[activeReviewer]) continue;
    seenReviewers[activeReviewer] = true;
    active.push(open[a]);
  }
  return {
    attempts: attempts,
    active: active,
    latestActive: open.length ? open[open.length - 1] : null,
    waiting: waiting,
    partial: log.truncated || inconsistent
  };
}

function doneReview(row, sessionMap) {
  if (row.column !== 'done') return { review: null, partial: false };
  var file = path.join(paths.TASKS_DIR, 'done', row.stem + '.md');
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, path.join(paths.TASKS_DIR, 'done'), file, MAX_DONE_BYTES);
  if (!hit || String(hit.stat.nlink) !== '1') return { review: null, partial: true };
  var parsed;
  try { parsed = taskCore.parseOutcome(hit.bytes.toString('utf8'), outcomeShape); }
  catch (error) { return { review: null, partial: true }; }
  if (!parsed.valid) return { review: null, partial: true };
  return {
    review: {
      taskStem: row.stem,
      taskTitle: row.title,
      column: row.column,
      reviewer: parsed.reviewer,
      reviewAttempt: null,
      selectionReason: null,
      reasonCode: null,
      startedAt: null,
      finishedAt: parsed.completedAt || row.doneAt,
      status: 'passed',
      sessionKey: liveSessionKey(sessionMap, row.stem),
      fromOutcome: true
    },
    partial: false
  };
}

function compareActivity(left, right) {
  var la = Date.parse(left.finishedAt || left.startedAt || 0) || 0;
  var ra = Date.parse(right.finishedAt || right.startedAt || 0) || 0;
  if (la !== ra) return ra - la;
  if ((left.reviewAttempt || 0) !== (right.reviewAttempt || 0)) {
    return (right.reviewAttempt || 0) - (left.reviewAttempt || 0);
  }
  return left.taskStem.localeCompare(right.taskStem);
}

function activityTime(row) {
  return Date.parse(row && (row.finishedAt || row.startedAt) || 0) || 0;
}

function dedupeRows(rows) {
  var seen = new Set();
  var out = [];
  for (var i = 0; i < rows.length; i++) {
    var row = rows[i];
    var key = JSON.stringify([
      row.taskStem,
      row.reviewAttempt,
      row.reviewer,
      row.status,
      row.startedAt,
      row.finishedAt,
      row.selectionReason,
      row.reasonCode,
      row.waitingToStart === true,
      row.fromOutcome === true,
      row.sessionKey
    ]);
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(row);
  }
  return out;
}

function build() {
  var index = readIndex();
  var sessionMap = {};
  var sessionPartial = false;
  try { sessionMap = sessions.list(); }
  catch (error) { sessionPartial = true; }
  var pending = [];
  var failed = [];
  var completed = [];
  var history = [];
  var active = [];
  var partial = index.partial || sessionPartial;
  var historyBounded = false;
  for (var i = 0; i < index.rows.length; i++) {
    var row = index.rows[i];
    var projected = projectTask(row, sessionMap);
    partial = partial || projected.partial;
    active = active.concat(projected.active);
    var taskHistory = projected.attempts.filter(function (attempt) { return !!attempt.finishedAt; });
    if (taskHistory.length > MAX_HISTORY_PER_TASK) {
      taskHistory = taskHistory.slice(taskHistory.length - MAX_HISTORY_PER_TASK);
      historyBounded = true;
    }
    history = history.concat(taskHistory);
    var latest = null;
    for (var a = projected.attempts.length - 1; a >= 0; a--) {
      if (projected.attempts[a].finishedAt) { latest = projected.attempts[a]; break; }
    }
    if (projected.latestActive) pending.push(projected.latestActive);
    else if (projected.waiting) pending.push(projected.waiting);
    var completion = latest;
    var durableProjection = doneReview(row, sessionMap);
    partial = partial || durableProjection.partial;
    var durable = durableProjection.review;
    if (durable) {
      if (!latest) {
        completion = durable;
        history.push(durable);
      } else if (latest.status !== 'passed' && activityTime(durable) >= activityTime(latest)) {
        // Outcome is the durable shipped truth. If best-effort journaling lost
        // the successful retry, a newer done Outcome closes the stale failure
        // in the current queue while the failed attempt remains in history.
        completion = durable;
        history.push(durable);
      } else if (latest.status === 'passed' && latest.reviewer === 'unknown') {
        latest.reviewer = durable.reviewer;
      }
    }
    if (completion && completion.finishedAt) {
      completed.push(completion);
      if (['failed', 'escalated', 'blocked'].indexOf(completion.status) >= 0) {
        failed.push(completion);
        if (completion.reasonCode === 'require-codex-blocked' && !projected.latestActive) pending.push(completion);
      }
    }
  }
  pending.sort(compareActivity);
  failed.sort(compareActivity);
  completed.sort(compareActivity);
  history.sort(compareActivity);
  if (history.length > MAX_HISTORY_ROWS) {
    history = history.slice(0, MAX_HISTORY_ROWS);
    historyBounded = true;
  }
  active.sort(compareActivity);
  return {
    revision: revision(),
    partial: partial || historyBounded,
    reasonCode: partial ? 'journal-partial' : historyBounded ? 'activity-bounded' : null,
    active: active,
    pending: pending,
    failed: failed,
    completed: completed,
    history: history,
    lastReview: completed.length ? completed[0] : null
  };
}

function revision() {
  var now = Date.now();
  if (now - revisionCache.at < 750) return revisionCache.value;
  var inventory = readIndex();
  var parts = ['index:' + inventory.revision, 'index-partial:' + String(inventory.partial)];
  for (var i = 0; i < inventory.rows.length; i++) {
    var inventoryRow = inventory.rows[i];
    var name = inventoryRow.stem + '.jsonl';
    var file = path.join(paths.JOURNAL_DIR, name);
    var entry = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, paths.JOURNAL_DIR, file);
    parts.push(name + ':' + (entry && entry.stat
      ? [
          entry.stat.devExact,
          entry.stat.inoExact,
          entry.stat.sizeExact,
          entry.stat.mtimeNs,
          entry.stat.ctimeNs
        ].join(':')
      : entry && entry.status || 'missing'));
    if (inventoryRow.column === 'done') {
      var outcomeName = inventoryRow.stem + '.md';
      var doneDir = path.join(paths.TASKS_DIR, 'done');
      var outcomeEntry = fileGuards.inspectEntryUnder(
        paths.PROJECT_ROOT,
        doneDir,
        path.join(doneDir, outcomeName)
      );
      parts.push('done/' + outcomeName + ':' + (outcomeEntry && outcomeEntry.stat
        ? [
            outcomeEntry.stat.devExact,
            outcomeEntry.stat.inoExact,
            outcomeEntry.stat.sizeExact,
            outcomeEntry.stat.mtimeNs,
            outcomeEntry.stat.ctimeNs
          ].join(':')
        : outcomeEntry && outcomeEntry.status || 'missing'));
    }
  }
  // Session links are live-only projections. Include the runs directory
  // generation so a started/finished session invalidates cached activity even
  // when INDEX and journals are unchanged.
  var runs = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, paths.RUNS_DIR, { allowMissing: true });
  parts.push('runs:' + (runs && runs.exists && runs.stat
    ? [
        runs.stat.devExact,
        runs.stat.inoExact,
        runs.stat.sizeExact,
        runs.stat.mtimeNs,
        runs.stat.ctimeNs
      ].join(':')
    : runs ? 'missing' : 'unsafe'));
  var sessionMap = {};
  var sessionsSafe = true;
  try { sessionMap = sessions.list(); }
  catch (error) { sessionsSafe = false; }
  var liveSessions = Object.keys(sessionMap).filter(function (key) {
    return key.indexOf('task:') === 0 && sessionMap[key] && sessionMap[key].running === true;
  }).sort().map(function (key) {
    var row = sessionMap[key];
    return [key, row.startedAt || '', row.awaitingTurn === true, row.closing === true].join(':');
  });
  parts.push('live-sessions:' + (sessionsSafe ? liveSessions.join(',') : 'unsafe'));
  revisionCache = {
    at: now,
    value: crypto.createHash('sha256').update(parts.join('\0')).digest('hex')
  };
  return revisionCache.value;
}

function snapshot() {
  var currentRevision = revision();
  if (cache && cache.revision === currentRevision) return cache.value;
  var value = build();
  cache = { revision: currentRevision, value: value };
  return value;
}

function cursorToken(payload) {
  var body = Buffer.from(JSON.stringify(payload)).toString('base64url');
  var signature = crypto.createHmac('sha256', CURSOR_SECRET).update(body).digest('base64url');
  return body + '.' + signature;
}

function parseCursor(token) {
  if (typeof token !== 'string' || token.length > 2048) return null;
  var parts = token.split('.');
  if (parts.length !== 2) return null;
  var expected = crypto.createHmac('sha256', CURSOR_SECRET).update(parts[0]).digest('base64url');
  if (parts[1].length !== expected.length ||
      !crypto.timingSafeEqual(Buffer.from(parts[1]), Buffer.from(expected))) return null;
  try {
    var parsed = JSON.parse(Buffer.from(parts[0], 'base64url').toString('utf8'));
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) &&
      Object.keys(parsed).sort().join('\0') === ['offset', 'revision', 'state', 'version'].join('\0') &&
      parsed.version === 1 ? parsed : null;
  } catch (error) { return null; }
}

function list(state, cursor, limit) {
  state = state || 'all';
  if (['all', 'pending', 'failed'].indexOf(state) < 0) return { ok: false, error: 'bad-activity-state' };
  limit = limit === undefined || limit === null ? DEFAULT_LIMIT : Number(limit);
  if (!Number.isInteger(limit) || limit < 1 || limit > HARD_LIMIT) return { ok: false, error: 'bad-limit' };
  var data = snapshot();
  var rows = state === 'pending' ? data.pending : state === 'failed' ? data.failed
    : dedupeRows(data.pending.concat(data.history)).sort(compareActivity);
  var offset = 0;
  if (cursor) {
    var parsed = parseCursor(cursor);
    if (!parsed || parsed.revision !== data.revision || parsed.state !== state ||
        !Number.isSafeInteger(parsed.offset) || parsed.offset < 0) {
      return { ok: false, error: 'stale-activity-cursor' };
    }
    offset = parsed.offset;
  }
  var page = rows.slice(offset, offset + limit);
  var nextOffset = offset + page.length;
  return {
    ok: true,
    schemaVersion: 1,
    revision: data.revision,
    state: state,
    rows: page,
    nextCursor: nextOffset < rows.length
      ? cursorToken({ version: 1, revision: data.revision, state: state, offset: nextOffset })
      : null,
    partial: data.partial,
    reasonCode: data.reasonCode
  };
}

module.exports = {
  revision: revision,
  snapshot: snapshot,
  list: list
};
