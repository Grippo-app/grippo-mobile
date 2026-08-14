'use strict';

// Typed task action admission. The server re-resolves the current action and
// rejects stale state/source/session/checkpoint generations before any prompt
// reaches the durable request queue.

var taskSummary = require('./task-summary');
var taskIntegrity = require('./task-integrity');
var taskRequirement = require('./task-requirement');
var taskCheckpoints = require('./task-checkpoints');
var prompts = require('./task-action-prompts');
var taskSource = require('../../tasks/task-source-contract.cjs');

var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var ACTION_ID_RE = /^act-[a-f0-9]{24}$/;
var IDEMPOTENCY_RE = /^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$/;
var CHECKPOINT_ID_RE = /^cp-[a-f0-9]{32}$/;
var ACTIONS = Object.freeze([
  'prepare', 'submit-answers', 'run', 'continue-live',
  'retry-phase', 'reopen', 'drop'
]);
var ACTION_FIELDS = [
  'action', 'actionId', 'actionRevision', 'answers', 'checkpointId', 'confirmation',
  'confirmationToken', 'expectedQuestionsRevision', 'expectedSessionRevision',
  'expectedSourceRevision', 'expectedState', 'idempotencyKey',
  'liveSessionId', 'questionRound', 'stem'
];

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}

function cleanConfirmation(value) {
  if (value === null || value === true) return value;
  if (!exact(value, ['dependents', 'impactHash', 'sourceRevision']) ||
      !HASH_RE.test(String(value.impactHash || '')) ||
      !HASH_RE.test(String(value.sourceRevision || '')) ||
      !Array.isArray(value.dependents) || value.dependents.length > 1000) return undefined;
  var dependents = value.dependents.slice();
  if (dependents.some(function (stem) { return !taskSource.safeTaskStem(stem); }) ||
      dependents.some(function (stem, index) { return index > 0 && dependents[index - 1] >= stem; })) {
    return undefined;
  }
  return {
    sourceRevision: value.sourceRevision,
    impactHash: value.impactHash,
    dependents: dependents
  };
}

function validateRequest(value) {
  if (!exact(value, ACTION_FIELDS)) return null;
  var confirmation = cleanConfirmation(value.confirmation);
  var normalized = Object.assign({}, value, {
    confirmation: confirmation,
    kind: value.action
  });
  if (!taskSource.safeTaskStem(normalized.stem) ||
      !ACTION_ID_RE.test(String(normalized.actionId || '')) ||
      !HASH_RE.test(String(normalized.actionRevision || '')) ||
      !HASH_RE.test(String(normalized.expectedSourceRevision || '')) ||
      ['backlog', 'pending', 'todo', 'done', 'corrupt'].indexOf(normalized.expectedState) < 0 ||
      typeof normalized.kind !== 'string' || confirmation === undefined ||
      normalized.checkpointId !== null &&
        !CHECKPOINT_ID_RE.test(String(normalized.checkpointId || '')) ||
      normalized.confirmationToken !== null &&
        (typeof normalized.confirmationToken !== 'string' || normalized.confirmationToken.length > 256) ||
      !IDEMPOTENCY_RE.test(String(normalized.idempotencyKey || '')) ||
      ACTIONS.indexOf(normalized.kind) < 0) return null;
  if (normalized.expectedState === 'corrupt' && normalized.kind !== 'drop') return null;

  var answerAction = normalized.kind === 'submit-answers';
  var liveAction = normalized.kind === 'continue-live';
  if (answerAction) {
    if (!taskActionAnswerShape(normalized)) return null;
  } else if (liveAction) {
    if (!liveAnswerShape(normalized)) return null;
  } else if (normalized.answers !== null || normalized.questionRound !== null ||
      normalized.expectedQuestionsRevision !== null) return null;
  if (!liveAction &&
      (normalized.liveSessionId !== null || normalized.expectedSessionRevision !== null)) return null;
  if (normalized.kind !== 'retry-phase' &&
      (normalized.confirmationToken !== null || normalized.checkpointId !== null)) return null;
  return normalized;
}

function taskActionAnswerShape(value) {
  return Array.isArray(value.answers) && taskActionPromptsAnswers(value.answers) &&
    Number.isSafeInteger(value.questionRound) && value.questionRound >= 1 &&
    value.questionRound <= 99 &&
    HASH_RE.test(String(value.expectedQuestionsRevision || '')) &&
    value.liveSessionId === null && value.expectedSessionRevision === null &&
    value.checkpointId === null && value.confirmationToken === null;
}

function taskActionPromptsAnswers(value) {
  return prompts.cleanAnswers(value) !== null;
}

function liveAnswerShape(value) {
  var cleaned = prompts.cleanAnswers(value.answers);
  return cleaned && cleaned.length === 1 && cleaned[0].questionId === 1 &&
    cleaned[0].optionIds.length === 0 && cleaned[0].text.length > 0 &&
    value.questionRound === null && value.expectedQuestionsRevision === null &&
    value.checkpointId === null && value.confirmationToken === null &&
    typeof value.liveSessionId === 'string' &&
    /^[A-Za-z0-9][A-Za-z0-9._:-]{0,255}$/.test(value.liveSessionId) &&
    HASH_RE.test(String(value.expectedSessionRevision || ''));
}

function stale(current) {
  return { ok: false, status: 409, error: 'action-stale', currentAction: current || null };
}

function queueRequest(action, stem, prompt, clean) {
  if (!prompt) return { ok: false, status: 400, error: 'bad-prompt' };
  var request = { action: action, stem: stem, prompt: prompt };
  if (clean.idempotencyKey) request.dedupKey = 'task-action:' + clean.idempotencyKey;
  return { ok: true, status: 202, operation: 'enqueue', request: request };
}

function inspect(request, dependencies) {
  dependencies = dependencies || {};
  var clean = validateRequest(request);
  if (!clean) return { ok: false, status: 400, error: 'bad-task-action-request' };
  // Mutations must never resolve actions or question generations through the
  // short-lived observational cache used by /api/state. A fresh canonical
  // snapshot closes the source edit → action submit window for every operation,
  // including live continuation paths that do not enter queue admission.
  var validation = dependencies.validation ||
    (dependencies.summary ? null : taskIntegrity.validateAll('task-action'));
  var summary;
  try {
    summary = dependencies.summary || taskSummary.single(clean.stem,
      Object.assign({}, dependencies, { validation: validation }));
  } catch (error) {
    return { ok: false, status: 503, error: 'task-summary-unavailable' };
  }
  if (!summary) return { ok: false, status: 404, error: 'task-summary-not-found' };
  var actions = [summary.task.primaryAction].concat(summary.task.secondaryActions || []);
  var current = actions.find(function (candidate) {
    return candidate && candidate.id === clean.actionId;
  });
  if (!current || current.id !== clean.actionId ||
      current.actionRevision !== clean.actionRevision ||
      current.kind !== clean.kind ||
      current.expectedState !== clean.expectedState ||
      current.expectedSourceRevision !== clean.expectedSourceRevision ||
      current.checkpointId !== clean.checkpointId ||
      (clean.kind === 'continue-live' &&
        (current.liveSessionId !== clean.liveSessionId ||
          current.expectedSessionRevision !== clean.expectedSessionRevision))) return stale(current);
  if (current.enabled === false) {
    return { ok: false, status: 409, error: 'action-disabled', currentAction: current };
  }

  if (current.kind === 'submit-answers') {
    var source = taskRequirement.load(clean.stem, { validation: validation });
    var questions = source && taskRequirement.questions(source);
    if (!source || !questions || !questions.valid ||
        questions.round !== clean.questionRound ||
        questions.revision !== clean.expectedQuestionsRevision) return stale(current);
    // The pending sidecar rail hands the answers to task-prep; the in-body rail
    // persists them and resumes the interrupted run under the same lock stage.
    if (clean.expectedState === 'todo') {
      var resumePrompt = prompts.submitTaskAnswers(clean.stem, source, clean.answers);
      if (!resumePrompt.ok) return { ok: false, status: 400, error: resumePrompt.error };
      return queueRequest('run', clean.stem, resumePrompt.prompt, clean);
    }
    var answerPrompt = prompts.submitAnswers(clean.stem, source, clean.answers);
    if (!answerPrompt.ok) return { ok: false, status: 400, error: answerPrompt.error };
    return queueRequest('prep', clean.stem, answerPrompt.prompt, clean);
  }
  if (current.kind === 'continue-live') {
    return {
      ok: true,
      status: 200,
      operation: 'continue-live',
      request: {
        key: 'task:' + clean.stem,
        text: prompts.cleanAnswers(clean.answers)[0].text,
        sessionId: clean.liveSessionId,
        sessionRevision: clean.expectedSessionRevision
      }
    };
  }

  // retry-phase begins in Details because the browser must preview the exact
  // checkpoint and obtain its one-shot confirmation token there. After that
  // modal-only input is present, the same server-owned action is executable.
  if (current.behavior !== 'execute' && current.kind !== 'retry-phase') {
    return { ok: false, status: 400, error: 'action-navigation-only', currentAction: current };
  }
  if (current.kind === 'drop') {
    if (!clean.confirmation || clean.confirmation === true) {
      return { ok: false, status: 400, error: 'drop-confirmation-required' };
    }
    var inspected = typeof dependencies.dropInspector === 'function'
      ? dependencies.dropInspector(clean.stem) : taskIntegrity.inspectDrop(clean.stem);
    var impact = inspected && inspected.impact;
    var result = inspected && inspected.result;
    var admission = inspected && inspected.admission;
    if (!impact || !result || !admission || !admission.ok ||
        impact.sourceRevision !== clean.confirmation.sourceRevision ||
        impact.impactHash !== clean.confirmation.impactHash ||
        JSON.stringify(impact.dependents.slice().sort()) !==
          JSON.stringify(clean.confirmation.dependents)) return stale(current);
    return queueRequest('drop', clean.stem, prompts.drop(clean.stem, clean.confirmation), clean);
  }
  if (current.kind === 'reopen') {
    if (clean.confirmation !== true) {
      return { ok: false, status: 400, error: 'reopen-confirmation-required' };
    }
    return queueRequest('reopen', clean.stem, prompts.reopen(clean.stem), clean);
  }
  if (current.kind === 'prepare') {
    return queueRequest('prep', clean.stem, prompts.prepare(clean.stem), clean);
  }
  if (current.kind === 'run') {
    return queueRequest('run', clean.stem, prompts.run(clean.stem), clean);
  }
  if (current.kind === 'retry-phase' && current.checkpointId) {
    var checkpoint = taskCheckpoints.read(clean.stem, current.checkpointId);
    if (!checkpoint || !taskCheckpoints.freshness(checkpoint).current ||
        !taskCheckpoints.consumeConfirmation(
          checkpoint, current.actionRevision, clean.confirmationToken)) {
      return { ok: false, status: 409, error: 'checkpoint-stale', currentAction: current };
    }
    return queueRequest('run', clean.stem, prompts.retry(clean.stem, checkpoint), clean);
  }
  return { ok: false, status: 409, error: 'action-not-executable', currentAction: current };
}

module.exports = Object.freeze({
  validateRequest: validateRequest,
  preparePrompt: prompts.prepare,
  runPrompt: prompts.run,
  retryPrompt: function (stem, checkpointId) {
    var checkpoint = taskCheckpoints.read(stem, checkpointId);
    return checkpoint ? prompts.retry(stem, checkpoint) : '';
  },
  dropPrompt: prompts.drop,
  reopenPrompt: prompts.reopen,
  inspect: inspect
});
