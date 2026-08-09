'use strict';

// One server-side primary-action resolver shared by list and details summary.
// The browser renders this result; it never replays lock/dependency precedence.

var crypto = require('crypto');

var KINDS = Object.freeze([
  'prepare', 'submit-answers', 'run', 'continue-live', 'retry-phase',
  'resume-finalization', 'review-result', 'validate-in-app',
  'resolve-blocker', 'open-run'
]);
var SECONDARY_KINDS = Object.freeze(['copy-prompt', 'drop', 'reopen']);

function canonical(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']';
  return '{' + Object.keys(value).sort().map(function (key) {
    return JSON.stringify(key) + ':' + canonical(value[key]);
  }).join(',') + '}';
}

function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(canonical(value), 'utf8').digest('hex');
}

function target(kind, context, blocker) {
  var stem = context.stem;
  if (kind === 'open-run') {
    return { type: 'terminal', key: 'task:' + stem };
  }
  if (kind === 'continue-live') return { type: 'task', stem: stem, section: 'questions' };
  if (kind === 'validate-in-app') return context.appValidationTarget ||
    { type: 'task', stem: stem, section: 'validation' };
  if (kind === 'resolve-blocker' && blocker &&
      ['dependency-incomplete', 'dependency-missing', 'dependency-cycle'].indexOf(blocker.kind) >= 0) {
    return { type: 'task', stem: stem, section: 'dependencies' };
  }
  if (kind === 'resolve-blocker' && blocker &&
      ['task-integrity', 'finalization-required', 'finalization-corrupt', 'dependency-cycle', 'unknown-recovery', 'stopped-run'].indexOf(blocker.kind) >= 0) {
    return { type: 'task', stem: stem, section: 'advanced' };
  }
  if (kind === 'resolve-blocker' && blocker && blocker.kind === 'setup-incomplete') {
    return { type: 'panel', panel: 'setup', entityId: null, section: null };
  }
  if (kind === 'resolve-blocker' && blocker && blocker.kind === 'reviewer-unavailable') {
    return { type: 'panel', panel: 'reviewer', entityId: null, section: null };
  }
  if (kind === 'resolve-blocker' && blocker &&
      ['figma-design-invalid', 'figma-screens-missing', 'figma-review-required'].indexOf(blocker.kind) >= 0) {
    return { type: 'task', stem: stem, section: 'artifacts' };
  }
  if (kind === 'review-result') return { type: 'task', stem: stem, section: context.visualReview ? 'artifacts' : 'overview' };
  if (kind === 'submit-answers') return { type: 'task', stem: stem, section: 'questions' };
  if (kind === 'resolve-blocker') return { type: 'task', stem: stem, section: 'dependencies' };
  return { type: 'task', stem: stem, section: 'overview' };
}

function labelKey(kind, context) {
  return 'board.action.' + kind.replace(/-/g, '_');
}

function blockerLabelKey(blocker) {
  if (!blocker) return 'board.action.resolve_blocker';
  if (['dependency-incomplete', 'dependency-missing', 'dependency-cycle'].indexOf(blocker.kind) >= 0) {
    return 'board.action.review_dependencies';
  }
  if (['task-integrity', 'finalization-corrupt', 'unknown-recovery'].indexOf(blocker.kind) >= 0) {
    return 'board.action.open_diagnostics';
  }
  if (blocker.kind === 'finalization-required') return 'board.action.open_recovery';
  if (blocker.kind === 'figma-design-invalid') return 'board.action.fix_design';
  if (blocker.kind === 'figma-screens-missing') return 'board.action.add_figma_screens';
  if (blocker.kind === 'figma-review-required') return 'board.action.open_visual_comparison';
  if (blocker.kind === 'setup-incomplete') return 'board.action.finish_setup';
  if (blocker.kind === 'reviewer-unavailable') return 'board.action.configure_reviewer';
  if (blocker.kind === 'stopped-run') return 'board.action.open_run_details';
  return 'board.action.resolve_blocker';
}

function action(kind, behavior, context, winningBlocker, options) {
  options = options || {};
  var actionTarget = options.target === undefined ? target(kind, context, winningBlocker) : options.target;
  var enabled = options.enabled !== false;
  var disabledReasonCode = enabled ? null : options.disabledReasonCode || 'action-unavailable';
  var requiresConfirmation = options.requiresConfirmation === true;
  var resolvedLabelKey = options.labelKey || labelKey(kind, context);
  var checkpointId = options.checkpointId === undefined
    ? context.retryCheckpoint && context.retryCheckpoint.id || null
    : options.checkpointId;
  var revisionSeed = {
    role: options.role || 'primary',
    stem: context.stem,
    state: context.state,
    sourceRevision: context.sourceRevision,
    kind: kind,
    behavior: behavior,
    target: actionTarget,
    blockerId: winningBlocker && winningBlocker.id || null,
    checkpointId: checkpointId,
    liveSessionId: options.liveSessionId === undefined ? null : options.liveSessionId,
    sessionRevision: context.sessionRevision || null,
    finalizationRevision: context.finalizationRevision || null,
    enabled: enabled,
    disabledReasonCode: disabledReasonCode,
    requiresConfirmation: requiresConfirmation,
    labelKey: resolvedLabelKey
  };
  var actionRevision = hash(revisionSeed);
  return {
    id: 'act-' + actionRevision.slice('sha256:'.length, 'sha256:'.length + 24),
    kind: kind,
    labelKey: resolvedLabelKey,
    enabled: enabled,
    disabledReasonCode: disabledReasonCode,
    behavior: behavior,
    target: actionTarget,
    requiresConfirmation: requiresConfirmation,
    expectedState: context.state,
    expectedSourceRevision: context.sourceRevision,
    checkpointId: checkpointId,
    liveSessionId: options.liveSessionId === undefined ? null : options.liveSessionId,
    expectedSessionRevision: options.expectedSessionRevision === undefined
      ? null : options.expectedSessionRevision,
    actionRevision: actionRevision
  };
}

function resolve(context) {
  context = context || {};
  var blockers = Array.isArray(context.blockers) ? context.blockers : [];
  var integrity = blockers.find(function (item) {
    return item.kind === 'task-integrity' || item.kind === 'finalization-corrupt' ||
      item.kind === 'dependency-cycle' || item.kind === 'unknown-recovery';
  });
  var blocking = blockers.find(function (item) { return item.severity === 'blocking'; });
  var kind, behavior, winningBlocker = null;

  if (integrity) {
    kind = 'resolve-blocker'; behavior = 'open-details'; winningBlocker = integrity;
  } else if (context.finalization && context.finalization.recoverable && !context.finalization.recoveryRunning) {
    kind = 'resume-finalization'; behavior = 'execute';
  } else if (context.finalization && context.finalization.recoveryRunning) {
    kind = 'resolve-blocker'; behavior = 'open-details';
    winningBlocker = blockers.find(function (item) { return item.kind === 'finalization-required'; }) || blocking;
  } else if (context.active && !context.liveAwaiting &&
      !(context.questionsPending && context.terminalAvailable === false)) {
    kind = 'open-run'; behavior = 'open-terminal';
  } else if (context.liveAwaiting) {
    kind = 'continue-live'; behavior = 'execute';
  } else if (context.visualReview) {
    kind = 'review-result'; behavior = 'open-details';
  } else if (blocking) {
    winningBlocker = blocking;
    if (blocking.kind === 'validation-required') {
      kind = 'validate-in-app'; behavior = context.appValidationTarget ? 'open-panel' : 'open-details';
    } else if (blocking.kind === 'awaiting-answer') {
      var stoppedRun = blockers.find(function (item) { return item.kind === 'stopped-run'; });
      if (context.questionsPending && stoppedRun) {
        // Persisting a durable answer needs a fresh orchestrator lock, and the
        // escalated run left its own behind. Route to the canonical recovery
        // first instead of offering a submit that cannot acquire the lock.
        kind = 'resolve-blocker'; behavior = 'open-details'; winningBlocker = stoppedRun;
      } else {
        // A live paused session is already handled above, so a blocking
        // awaiting-answer here is always a durable question — the pending
        // sidecar and the in-body section share this one rail.
        kind = context.questionsPending || context.state === 'pending'
          ? 'submit-answers' : 'continue-live';
        behavior = kind === 'continue-live' ? 'execute' : 'open-details';
      }
    } else {
      kind = 'resolve-blocker'; behavior = 'open-details';
    }
  } else if (context.retryCheckpoint) {
    kind = 'retry-phase'; behavior = 'execute';
  } else if (context.state === 'backlog') {
    kind = 'prepare'; behavior = 'execute';
  } else if (context.state === 'pending') {
    kind = 'submit-answers'; behavior = 'open-details';
  } else if (context.state === 'todo') {
    kind = 'run'; behavior = 'execute';
  } else if (context.state === 'done' && context.validationPending) {
    kind = 'validate-in-app'; behavior = context.appValidationTarget ? 'open-panel' : 'open-details';
  } else {
    kind = 'review-result'; behavior = 'open-details';
  }

  if (KINDS.indexOf(kind) < 0) throw new Error('unsupported primary action kind');
  // Selecting a server-validated retry checkpoint is already an explicit user
  // action. A second generic confirmation adds no new impact information (and
  // ordinary Run starts the same mutation class directly), so only genuinely
  // destructive secondary actions keep confirmation flows.
  var options = { requiresConfirmation: false };
  if (kind === 'open-run') {
    options.labelKey = 'board.action.open_terminal';
  }
  if (kind === 'continue-live') {
    options.labelKey = 'board.action.continue_live';
    options.liveSessionId = context.liveSessionId || null;
    options.expectedSessionRevision = context.sessionRevision || null;
    if (!options.liveSessionId || !options.expectedSessionRevision || context.sessionInputReady !== true) {
      options.enabled = false;
      options.disabledReasonCode = 'session-input-unavailable';
    }
  }
  // A durable queue record is active ownership, but it does not prove that an
  // in-app terminal exists yet. Keep the queued action fenced and let the
  // runtime status ("waiting runner"/"queued") carry the state instead of
  // opening an empty terminal. The action becomes enabled automatically as
  // soon as the owning Site session is actually running.
  if (kind === 'open-run' && context.terminalAvailable === false) {
    options.enabled = false;
    options.disabledReasonCode = 'terminal-not-started';
  }
  if (kind === 'resolve-blocker') options.labelKey = blockerLabelKey(winningBlocker);
  if (kind === 'review-result' && context.visualReview) options.labelKey = 'board.action.open_visual_comparison';
  return action(kind, behavior, context, winningBlocker, options);
}

function secondary(context) {
  context = context || {};
  var blockers = Array.isArray(context.blockers) ? context.blockers : [];
  var reopenUnsafe = context.active === true || blockers.some(function (item) {
    return item && ['task-integrity', 'finalization-corrupt', 'finalization-required',
      'dependency-cycle', 'unknown-recovery', 'stopped-run'].indexOf(item.kind) >= 0;
  });
  var reopenDisabledReason = context.active === true ? 'action-active' :
    reopenUnsafe ? 'task-integrity' : null;
  var result = [];
  if (context.state === 'done') {
    result.push(action('reopen', 'execute', context, null, {
      role: 'secondary', target: null, requiresConfirmation: true,
      enabled: !reopenUnsafe, disabledReasonCode: reopenDisabledReason, checkpointId: null,
      labelKey: 'board.overflow.reopen'
    }));
  }
  // Deletion is also the recovery path for malformed task content. Keep it
  // available independently of task-integrity blockers; exact runtime
  // ownership and filesystem safety are re-checked by the drop-impact rail.
  var dropUnsafe = context.active === true || context.dropAvailable === false;
  result.push(action('drop', 'execute', context, null, {
    role: 'secondary', target: null, requiresConfirmation: true,
    enabled: !dropUnsafe,
    disabledReasonCode: context.active === true
      ? 'action-active' : context.dropAvailable === false ? 'task-integrity' : null,
    checkpointId: null,
    labelKey: 'board.overflow.drop'
  }));
  var copyable = context.primaryAction && (
    ['prepare', 'submit-answers', 'run', 'retry-phase'].indexOf(context.primaryAction.kind) >= 0
  );
  if (!context.active && copyable) {
    var copyPromptLabelKey = context.primaryAction.kind === 'submit-answers'
      ? 'board.overflow.copy_answers_prompt'
      : context.primaryAction.kind === 'prepare'
      ? 'board.overflow.copy_prepare_prompt'
      : context.primaryAction.kind === 'retry-phase'
        ? 'board.overflow.copy_retry_prompt'
        : 'board.overflow.copy_run_prompt';
    result.push(action('copy-prompt', 'copy', context, null, {
      role: 'secondary', target: null,
      checkpointId: context.primaryAction.checkpointId || null,
      labelKey: copyPromptLabelKey
    }));
  }
  return result;
}

module.exports = Object.freeze({
  KINDS: KINDS,
  SECONDARY_KINDS: SECONDARY_KINDS,
  canonical: canonical,
  hash: hash,
  resolve: resolve,
  secondary: secondary
});
