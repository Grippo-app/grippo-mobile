'use strict';

var crypto = require('crypto');
var tasksLog = require('./tasks-log');
var taskSummary = require('./task-summary');
var taskRequirement = require('./task-requirement');
var taskSource = require('../../tasks/task-source-contract.cjs');

var DEFAULT_LIMIT = 50;
var MAX_LIMIT = 200;

function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(JSON.stringify(value), 'utf8').digest('hex');
}

function summaryFor(row, events) {
  var last = events.length ? events[events.length - 1] : null;
  var completed = null, failed = null, current = null, retries = 0, followUps = 0;
  events.forEach(function (event) {
    if (event.kind === 'phase-start') current = event.phase || current;
    if (event.kind === 'phase-end' && event.status === 'ok') {
      completed = event.phase || completed;
      if (current === event.phase) current = null;
    }
    if ((event.kind === 'phase-end' || event.kind === 'stop' || event.kind === 'gate') &&
        (event.status === 'fail' || event.status === 'blocked' || event.status === 'escalate')) {
      failed = event.phase || failed;
      if (current === event.phase) current = null;
    }
    if (event.kind === 'retry') retries++;
    if (event.kind === 'follow-up' || event.kind === 'task-split') followUps++;
  });
  var firstAt = events.length ? Date.parse(events[0].ts) : NaN;
  var lastAt = last ? Date.parse(last.ts) : NaN;
  var status = row.state === 'done' ? 'completed' :
    row.runtimeStatus && row.runtimeStatus.state === 'awaiting' ? 'awaiting-user' :
      failed || row.runtimeStatus && row.runtimeStatus.state === 'stopped' ? 'failed' :
        row.runtimeStatus && row.runtimeStatus.active ? 'running' :
        row.state === 'pending' ? 'needs-answers' :
          row.state === 'todo' ? 'ready' : 'preparing';
  return {
    status: status,
    labelKey: 'taskDetails.activity.summary.' + status.replace(/-/g, '_'),
    currentPhase: row.runtimeStatus && row.runtimeStatus.phase || current,
    lastCompletedPhase: completed,
    lastFailedPhase: failed,
    totalElapsedMs: Number.isFinite(firstAt) && Number.isFinite(lastAt) ? Math.max(0, lastAt - firstAt) : null,
    retryCount: retries,
    spawnedFollowUpCount: followUps,
    eventCount: events.length,
    nextRecovery: row.primaryAction || null
  };
}

function buildEventGroups(events) {
  var groups = [];
  var byId = Object.create(null);
  var currentByPhase = Object.create(null);
  var attemptByPhase = Object.create(null);
  var assignments = [];
  events.forEach(function (event, index) {
    var key = event.phase || 'lifecycle';
    var group = currentByPhase[key];
    if (!group || event.kind === 'phase-start') {
      var attempt = (attemptByPhase[key] || 0) + 1;
      attemptByPhase[key] = attempt;
      group = {
        id: 'phase-' + key + '-attempt-' + attempt,
        phase: event.phase || null,
        attempt: attempt,
        status: 'info',
        startedAt: null,
        endedAt: null,
        durationMs: null,
        retryCount: 0,
        stopReason: null,
        checkpointId: null,
        reportId: null,
        children: [],
        events: []
      };
      byId[group.id] = group;
      currentByPhase[key] = group;
      groups.push(group);
    }
    assignments.push(group.id);
    group.events.push(event);
    if (event.kind === 'phase-start' && !group.startedAt) group.startedAt = event.ts;
    if (event.kind === 'phase-end' || event.kind === 'stop' || event.kind === 'gate') {
      group.endedAt = event.ts;
      group.status = event.status || group.status;
    }
    if (event.kind === 'retry') group.retryCount++;
    if (event.kind === 'stop') group.stopReason = event.detail || event.meta && event.meta.reasonCode || null;
    if (event.durationMs !== undefined) group.durationMs = event.durationMs;
    if (event.meta && event.meta.checkpointId) group.checkpointId = event.meta.checkpointId;
    if (event.meta && event.meta.reportId) group.reportId = event.meta.reportId;
    if (event.meta && event.meta.children) {
      String(event.meta.children).split(',').map(function (item) {
        return item.trim();
      }).filter(Boolean).forEach(function (child) {
        if (group.children.indexOf(child) < 0 && group.children.length < 100) {
          group.children.push(child);
        }
      });
    }
  });
  groups.forEach(function (group) {
    if (group.durationMs == null && group.startedAt && group.endedAt) {
      group.durationMs = Math.max(0, Date.parse(group.endedAt) - Date.parse(group.startedAt));
    }
    var last = group.events[group.events.length - 1];
    if (!group.endedAt && last === events[events.length - 1] &&
        last && last.kind !== 'stop' && last.kind !== 'phase-end') {
      group.status = 'running';
    }
  });
  return { groups: groups, byId: byId, assignments: assignments };
}

function groupEvents(events) {
  return buildEventGroups(events).groups;
}

function encodeCursor(offset, revision) {
  var raw = Buffer.from(JSON.stringify({ offset: offset, revision: revision }), 'utf8').toString('base64url');
  return raw + '.' + hash(raw).slice(7, 23);
}

function decodeCursor(value, revision) {
  if (!value) return 0;
  if (typeof value !== 'string' || value.length > 1000) return null;
  var parts = value.split('.');
  if (parts.length !== 2 || parts[1] !== hash(parts[0]).slice(7, 23)) return null;
  try {
    var parsed = JSON.parse(Buffer.from(parts[0], 'base64url').toString('utf8'));
    return parsed && parsed.revision === revision && Number.isSafeInteger(parsed.offset) &&
      parsed.offset >= 0 ? parsed.offset : null;
  } catch (_) { return null; }
}

function build(stem, options, dependencies) {
  options = options || {};
  dependencies = dependencies || {};
  if (!taskSource.safeTaskStem(stem)) return { ok: false, status: 400, error: 'bad-task-stem' };
  var current = dependencies.summary || taskSummary.single(stem);
  if (!current) return { ok: false, status: 404, error: 'task-not-found' };
  var log = dependencies.log || tasksLog.readLog(stem);
  var events = Array.isArray(log.events) ? log.events : [];
  var source = dependencies.source ||
    (!dependencies.summary ? taskRequirement.load(stem) : null);
  var projectedOutcome = source ? taskRequirement.outcome(source) : null;
  var outcomeDigest = projectedOutcome && Array.isArray(projectedOutcome.executionLog)
    ? projectedOutcome.executionLog.slice(0, 6) : [];
  var revision = hash({
    stem: stem, summary: current.revision, events: events,
    outcomeDigest: outcomeDigest, truncated: !!log.truncated
  });
  var limit = Number(options.limit);
  limit = Number.isSafeInteger(limit) && limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
  var offset = decodeCursor(options.cursor, revision);
  if (offset == null) return { ok: false, status: 409, error: 'task-activity-cursor-stale' };
  var page = events.slice(offset, offset + limit);
  var grouped = buildEventGroups(events);
  var pageGroups = [], pageGroupsById = Object.create(null);
  page.forEach(function (event, index) {
    var id = grouped.assignments[offset + index];
    var full = grouped.byId[id];
    if (!full) return;
    var projected = pageGroupsById[id];
    if (!projected) {
      projected = Object.assign({}, full, {
        children: full.children.slice(),
        events: []
      });
      pageGroupsById[id] = projected;
      pageGroups.push(projected);
    }
    projected.events.push(event);
  });
  return {
    ok: true,
    status: 200,
    schemaVersion: 1,
    stem: stem,
    revision: revision,
    summary: summaryFor(current.task, events),
    outcomeDigest: outcomeDigest,
    groups: pageGroups,
    events: page,
    nextCursor: offset + page.length < events.length ? encodeCursor(offset + page.length, revision) : null,
    partial: !!log.truncated,
    limitations: log.truncated ? ['task-journal-truncated'] : []
  };
}

module.exports = Object.freeze({
  DEFAULT_LIMIT: DEFAULT_LIMIT,
  MAX_LIMIT: MAX_LIMIT,
  summaryFor: summaryFor,
  groupEvents: groupEvents,
  build: build
});
