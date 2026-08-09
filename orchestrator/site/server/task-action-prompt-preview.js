'use strict';

var crypto = require('crypto');
var taskSummary = require('./task-summary');
var taskCheckpoints = require('./task-checkpoints');
var taskRequirement = require('./task-requirement');
var prompts = require('./task-action-prompts');
var taskSource = require('../../tasks/task-source-contract.cjs');

function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(String(value), 'utf8').digest('hex');
}

function build(stem, query, dependencies) {
  if (!taskSource.safeTaskStem(stem) || !query ||
      Object.keys(query).sort().join('\0') !== ['actionRevision'].sort().join('\0') ||
      !/^sha256:[a-f0-9]{64}$/.test(String(query.actionRevision || ''))) {
    return { ok: false, status: 400, error: 'bad-action-prompt-query' };
  }
  var summary = dependencies && dependencies.summary || taskSummary.single(stem, dependencies);
  if (!summary) return { ok: false, status: 404, error: 'task-not-found' };
  var copyAction = (summary.task.secondaryActions || []).find(function (item) {
    return item && item.kind === 'copy-prompt' &&
      item.actionRevision === query.actionRevision && item.enabled !== false;
  });
  if (!copyAction || summary.task.runtimeStatus && summary.task.runtimeStatus.active) {
    return { ok: false, status: 409, error: 'action-stale' };
  }
  var primary = summary.task.primaryAction;
  var text = null;
  if (primary.kind === 'prepare') text = prompts.prepare(stem);
  if (primary.kind === 'run') text = prompts.run(stem);
  if (primary.kind === 'retry-phase' && primary.checkpointId) {
    var checkpoint = taskCheckpoints.read(stem, primary.checkpointId);
    if (checkpoint && taskCheckpoints.freshness(checkpoint).current) {
      text = prompts.retry(stem, checkpoint);
    }
  }
  if (!text) return { ok: false, status: 409, error: 'action-stale' };
  return {
    ok: true,
    status: 200,
    schemaVersion: 1,
    stem: stem,
    actionRevision: copyAction.actionRevision,
    taskSourceRevision: summary.task.sourceRevision,
    promptHash: hash(text),
    expiresAt: new Date(Date.now() + 5 * 60 * 1000).toISOString(),
    manualFallback: true,
    text: text
  };
}

function buildAnswers(stem, body, dependencies) {
  var keys = body && typeof body === 'object' && !Array.isArray(body)
    ? Object.keys(body).sort().join('\0') : '';
  if (!taskSource.safeTaskStem(stem) ||
      keys !== ['actionRevision', 'answers', 'expectedQuestionsRevision', 'questionRound'].sort().join('\0') ||
      !/^sha256:[a-f0-9]{64}$/.test(String(body.actionRevision || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(body.expectedQuestionsRevision || '')) ||
      !Number.isSafeInteger(body.questionRound) || body.questionRound < 1 || body.questionRound > 99 ||
      prompts.cleanAnswers(body.answers) === null) {
    return { ok: false, status: 400, error: 'bad-action-prompt-request' };
  }
  var summary = dependencies && dependencies.summary || taskSummary.single(stem, dependencies);
  if (!summary) return { ok: false, status: 404, error: 'task-not-found' };
  var copyAction = (summary.task.secondaryActions || []).find(function (item) {
    return item && item.kind === 'copy-prompt' &&
      item.actionRevision === body.actionRevision && item.enabled !== false;
  });
  if (!copyAction || !summary.task.primaryAction ||
      summary.task.primaryAction.kind !== 'submit-answers' ||
      summary.task.runtimeStatus && summary.task.runtimeStatus.active) {
    return { ok: false, status: 409, error: 'action-stale' };
  }
  var source = dependencies && dependencies.source || taskRequirement.load(stem);
  var questions = source && taskRequirement.questions(source);
  if (!source || !questions || !questions.valid ||
      questions.round !== body.questionRound ||
      questions.revision !== body.expectedQuestionsRevision) {
    return { ok: false, status: 409, error: 'action-stale' };
  }
  var generated = summary.task.state === 'todo'
    ? prompts.submitTaskAnswers(stem, source, body.answers)
    : prompts.submitAnswers(stem, source, body.answers);
  if (!generated.ok) return { ok: false, status: 400, error: generated.error };
  return {
    ok: true,
    status: 200,
    schemaVersion: 1,
    stem: stem,
    actionRevision: copyAction.actionRevision,
    taskSourceRevision: summary.task.sourceRevision,
    promptHash: hash(generated.prompt),
    expiresAt: new Date(Date.now() + 5 * 60 * 1000).toISOString(),
    manualFallback: true,
    text: generated.prompt
  };
}

module.exports = Object.freeze({ build: build, buildAnswers: buildAnswers });
