'use strict';

var crypto = require('crypto');
var taskSummary = require('./task-summary');
var taskRequirement = require('./task-requirement');
var taskActivity = require('./task-activity');
var taskArtifacts = require('./task-artifacts');
var taskCheckpoints = require('./task-checkpoints');
var taskIntegrity = require('./task-integrity');
var shallowIntake = require('./shallow-intake');
var state = require('./state');
var taskSource = require('../../tasks/task-source-contract.cjs');

function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(JSON.stringify(value), 'utf8').digest('hex');
}

function identity(row) {
  var number = /^TASK_([1-9][0-9]*)_/.exec(row.stem);
  return {
    stem: row.stem,
    number: number ? Number(number[1]) : null,
    title: row.title,
    filename: row.stem + (row.state === 'pending' ? '.questions.md' : '.md')
  };
}

function stateProjection(row) {
  var runtime = row.runtimeStatus || {};
  var blocking = (row.blockers || []).some(function (item) {
    return item && item.severity === 'blocking';
  });
  var display = runtime.state === 'awaiting' ? 'awaiting-user' :
    runtime.state === 'running' || runtime.state === 'queued' || runtime.state === 'waiting-runner' ? 'running' :
      runtime.state === 'finalizing' ? 'completing' :
        blocking ? 'blocked' :
          row.state === 'pending' ? 'needs-answers' :
            row.state === 'todo' ? 'ready' : row.state;
  return { column: row.state, display: display, runtime: runtime.state || 'idle', phase: runtime.phase || null };
}

function sourceProjection(row) {
  var origin = row.origin;
  if (!origin) return null;
  return {
    kind: origin.kind,
    type: origin.type,
    ref: origin.ref,
    target: row.sourceTarget || null
  };
}

function currentActivity(stem, row, source) {
  var activity = taskActivity.build(stem, { limit: 1 }, {
    summary: { revision: row.sourceRevision, task: row },
    source: source
  });
  return activity.ok ? activity.summary : {
    status: row.state, labelKey: 'taskDetails.activity.summary.' + row.state,
    currentPhase: row.runtimeStatus && row.runtimeStatus.phase || null,
    lastCompletedPhase: null, lastFailedPhase: null, totalElapsedMs: null,
    retryCount: 0, spawnedFollowUpCount: 0, nextRecovery: row.primaryAction
  };
}

function build(stem, dependencies) {
  dependencies = dependencies || {};
  if (!taskSource.safeTaskStem(stem)) return { ok: false, status: 400, error: 'bad-task-stem' };
  var current;
  try { current = dependencies.summary || taskSummary.single(stem); }
  catch (error) { return { ok: false, status: 503, error: taskSummary.publicErrorCode(error) }; }
  if (!current) return { ok: false, status: 404, error: 'task-not-found' };
  var source = dependencies.source || taskRequirement.load(stem);
  if (!source) return { ok: false, status: 503, error: 'task-details-unavailable' };
  var row = current.task;
  var origin = sourceProjection(row);
  if (!origin) return { ok: false, status: 503, error: 'task-details-unavailable' };
  var requirement = taskRequirement.requirement(source);
  requirement.sources = requirement.sources.map(function (item) {
    if (!source.metadata.origin || item.kind !== source.metadata.origin.kind ||
        item.type !== source.metadata.origin.type ||
        item.ref !== source.metadata.origin.ref) return item;
    return Object.assign({}, item, { target: row.sourceTarget || null });
  });
  var delivered = taskRequirement.outcome(source);
  var intake = dependencies.intake;
  if (intake === undefined && row.state === 'backlog') {
    var intakeSnapshot = shallowIntake.snapshot();
    intake = intakeSnapshot && intakeSnapshot[stem] || null;
  }
  var currentWork = taskRequirement.currentWork(source, row, intake);
  var activity = dependencies.activitySummary || currentActivity(stem, row, source);
  var artifactPage = taskArtifacts.summary(stem, { summary: current, source: source });
  var checkpointPage = taskCheckpoints.summary(stem);
  var limitations = (current.limitations || []).concat(
    requirement.partial ? ['requirement-partial'] : [],
    artifactPage.ok && artifactPage.partial ? artifactPage.limitations : [],
    checkpointPage.partial ? checkpointPage.limitations : []
  );
  var revision = hash({
    summary: current.revision,
    source: source.metadata.revision,
    questions: source.questionsRevision || source.taskQuestionsRevision,
    intake: intake || null,
    activity: activity,
    artifacts: artifactPage.ok ? artifactPage.revision : null,
    checkpoints: checkpointPage.revision
  });
  return {
    ok: true,
    status: 200,
    schemaVersion: 1,
    revision: revision,
    identity: identity(row),
    state: stateProjection(row),
    origin: origin,
    primaryAction: row.primaryAction,
    secondaryActions: row.secondaryActions || [],
    blockers: row.blockers || [],
    designIssues: row.figmaDesignIssues || null,
    dependencies: row.dependencySummary,
    lastActivity: row.lastActivity,
    requirement: requirement,
    outcome: delivered,
    currentWork: currentWork,
    retryRecovery: row.retryRecovery || null,
    activitySummary: activity,
    artifactSummary: artifactPage.ok ? { groups: artifactPage.groups, revision: artifactPage.revision } :
      { groups: [], revision: null },
    recovery: { finalization: row.finalization || null },
    appValidation: row.appValidation || { required: false, current: false, overall: null },
    advancedAvailable: true,
    partial: limitations.length > 0,
    limitations: Array.from(new Set(limitations)).sort()
  };
}

function advanced(stem, sections, dependencies) {
  dependencies = dependencies || {};
  if (!taskSource.safeTaskStem(stem)) return { ok: false, status: 400, error: 'bad-task-stem' };
  var source = dependencies.source || taskRequirement.load(stem);
  if (!source) return { ok: false, status: 404, error: 'task-not-found' };
  var allowed = [
    'raw', 'revisions', 'runtime', 'integrity', 'outcome',
    'checkpoints', 'diagnostics'
  ];
  var requested = sections && sections.length ? sections : allowed;
  if (requested.some(function (item) { return allowed.indexOf(item) < 0; })) {
    return { ok: false, status: 400, error: 'bad-advanced-section' };
  }
  var snapshot = dependencies.snapshot || state.deriveState();
  var result = {};
  if (requested.indexOf('raw') >= 0) result.raw = {
    taskMarkdown: source.raw,
    questionsMarkdown: source.questionsRaw || source.taskQuestionsRaw
  };
  if (requested.indexOf('revisions') >= 0) result.revisions = {
    taskSourceRevision: source.metadata.revision,
    taskBodyRevision: source.bodyRevision,
    questionsRevision: source.questionsRevision || source.taskQuestionsRevision,
    taskState: source.metadata.state
  };
  if (requested.indexOf('outcome') >= 0) result.outcome = taskRequirement.outcome(source);
  if (requested.indexOf('checkpoints') >= 0) result.checkpoints = taskCheckpoints.list(stem);
  if (requested.indexOf('runtime') >= 0) {
    var progress = snapshot.progress || {};
    var lock = (progress.inProgress || []).find(function (item) { return item && item.stem === stem; });
    var request = (progress.requests || []).find(function (item) { return item && item.stem === stem; });
    var finalization = (progress.finalizations || []).find(function (item) { return item && item.stem === stem; });
    var session = snapshot.sessions && snapshot.sessions['task:' + stem];
    result.runtime = {
      lock: lock ? {
        status: 'held', owner: lock.ownerKind || lock.owner || null,
        issuedAt: lock.startedAt || null, updatedAt: lock.updatedAt || null,
        recoverable: false, nextAction: 'Review owner status'
      } : null,
      request: request ? { id: request.id || null, action: request.action || null, createdAt: request.createdAt || null } : null,
      session: session ? {
        key: 'task:' + stem,
        id: session.sessionId || null,
        revision: session.revision || null,
        running: session.running === true,
        startedAt: session.startedAt || null, endedAt: session.endedAt || null
      } : null,
      finalization: finalization || null
    };
  }
  if (requested.indexOf('integrity') >= 0) {
    var validation = dependencies.validation || source.validation;
    var projected = taskIntegrity.publicResult(validation);
    result.integrity = {
      ok: projected.ok,
      indexStatus: projected.indexStatus,
      findings: (projected.findings || []).filter(function (item) {
        return item.stem === stem || item.stem == null;
      })
    };
  }
  if (requested.indexOf('diagnostics') >= 0) {
    var taskPaths = source.metadata.state === 'pending'
      ? [
        'orchestrator/tasks/backlog/' + stem + '.md',
        'orchestrator/tasks/pending/' + stem + '.questions.md'
      ]
      : ['orchestrator/tasks/' + source.metadata.state + '/' + stem + '.md'];
    result.diagnostics = {
      paths: taskPaths.concat([
        'orchestrator/.cache/tasks/journal/' + stem + '.jsonl',
        'orchestrator/.cache/tasks/checkpoints/' + stem + '/'
      ]),
      commands: [
        'node orchestrator/tasks/validate-task-state.mjs --stem ' +
          stem + ' --check-index --json'
      ]
    };
  }
  return {
    ok: true,
    status: 200,
    schemaVersion: 1,
    stem: stem,
    sections: result,
    partial: false,
    limitations: []
  };
}

module.exports = Object.freeze({ build: build, advanced: advanced });
