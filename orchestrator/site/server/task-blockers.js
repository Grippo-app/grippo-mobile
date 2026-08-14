'use strict';

// Normalizes task/runtime diagnostics into the bounded, human-facing blocker
// vocabulary consumed by both the list and task details. Raw paths, receipts
// and recovery codes deliberately stay in the integrity/Advanced endpoint.

var crypto = require('crypto');

var KINDS = Object.freeze([
  'dependency-incomplete', 'dependency-missing', 'dependency-cycle',
  'task-integrity', 'finalization-required', 'finalization-corrupt',
  'generation-outdated', 'awaiting-answer', 'figma-design-invalid', 'figma-screens-missing',
  'figma-review-required',
  'setup-incomplete', 'reviewer-unavailable', 'runner-unavailable',
  'stopped-run', 'validation-required', 'unknown-recovery'
]);
var PRIORITY = Object.freeze({
  'task-integrity': 1,
  'finalization-corrupt': 2,
  'dependency-cycle': 3,
  'unknown-recovery': 4,
  'finalization-required': 10,
  'generation-outdated': 11,
  'awaiting-answer': 20,
  'dependency-missing': 30,
  'dependency-incomplete': 31,
  'figma-review-required': 40,
  'figma-design-invalid': 41,
  'figma-screens-missing': 42,
  'setup-incomplete': 43,
  'reviewer-unavailable': 44,
  'stopped-run': 50,
  'validation-required': 60,
  'runner-unavailable': 70
});

function idFor(stem, kind, related) {
  return 'blk-' + crypto.createHash('sha256')
    .update(String(stem || '') + '\0' + kind + '\0' + String(related || ''), 'utf8')
    .digest('hex').slice(0, 24);
}

function blocker(stem, kind, severity, title, summary, options) {
  options = options || {};
  if (KINDS.indexOf(kind) < 0) throw new Error('unsupported task blocker kind');
  return {
    id: idFor(stem, kind, options.relatedTaskStem),
    kind: kind,
    severity: severity,
    title: String(title || '').slice(0, 160),
    summary: String(summary || '').slice(0, 320),
    relatedTaskStem: options.relatedTaskStem || null,
    source: options.source || 'runtime',
    recoverable: options.recoverable === true
  };
}

function findingApplies(item, stem) {
  return !!item && (item.stem === stem || item.stem == null);
}

function fromContext(context) {
  context = context || {};
  var stem = context.stem || '';
  var out = [];
  var findings = Array.isArray(context.findings) ? context.findings : [];
  findings.forEach(function (item) {
    if (!findingApplies(item, stem) || (item.severity !== 'error' && item.severity !== 'blocker')) return;
    if (item.code === 'DEPENDENCY_CYCLE') {
      out.push(blocker(stem, 'dependency-cycle', 'blocking', 'Tasks block each other',
        'Review the dependency chain and remove the cycle before continuing.',
        { source: 'integrity' }));
      return;
    }
    out.push(blocker(stem, 'task-integrity', 'blocking', 'Task data needs attention',
      'Review the task integrity issue before starting a mutation.',
      { source: 'integrity' }));
  });

  var fin = context.finalization;
  if (fin) {
    if (fin.status === 'corrupt' || fin.recoverable === false && !fin.recoveryRunning) {
      out.push(blocker(stem, 'finalization-corrupt', 'blocking', 'Completion state needs attention',
        'Completion cannot be recovered automatically from the current marker.',
        { source: 'finalization' }));
    } else {
      out.push(blocker(stem, 'finalization-required', 'blocking', 'Completion needs to resume',
        fin.recoveryRunning ? 'Completion recovery is already running.' : 'Resume the durable completion transaction.',
        { source: 'finalization', recoverable: fin.recoverable === true }));
    }
  }

  if (context.integration && context.integration.state === 'revalidation-required') {
    out.push(blocker(stem, 'generation-outdated', 'blocking', 'The previous run is outdated',
      'Release the outdated run before continuing this task.',
      { source: 'worktree', recoverable: true }));
  }

  // Two rails reach this blocker: a live session that paused mid-turn, and a
  // durable question published into the task body, which survives a reaped
  // session. The copy stays rail-neutral because the card renders it from the
  // kind alone — a per-rail string here would never reach a screen.
  if (context.liveAwaiting || context.questionsPending) {
    out.push(blocker(stem, 'awaiting-answer', 'blocking', 'Waiting for your answer',
      'Answer the open question so the task can continue.',
      { source: context.liveAwaiting ? 'session' : 'task', recoverable: true }));
  }

  var dependencies = context.dependencySummary || {};
  var rows = Array.isArray(dependencies.items) ? dependencies.items : [];
  var missing = rows.filter(function (row) { return row && row.missing; });
  var waiting = rows.filter(function (row) { return row && !row.satisfied && !row.missing; });
  // Dependencies gate execution, not preparation. Backlog cards keep the
  // diagnostic visible as a warning and can still be prepared; once promoted
  // to todo the same condition becomes the blocking Run gate.
  var dependencySeverity = context.state === 'todo' ? 'blocking' : 'warning';
  if (missing.length && context.state !== 'done') {
    out.push(blocker(stem, 'dependency-missing', dependencySeverity, 'Required task is missing',
      missing.length === 1
        ? (context.state === 'backlog' ? 'Preparation can continue; Run will wait for this dependency.' : 'A required dependency is not present on the board.')
        : (context.state === 'backlog' ? 'Preparation can continue; Run will wait for these dependencies.' : missing.length + ' required dependencies are missing.'),
      { relatedTaskStem: missing.length === 1 ? missing[0].stem : null, source: 'dependency' }));
  } else if (waiting.length && context.state !== 'done') {
    var related = waiting.length === 1 ? waiting[0] : null;
    out.push(blocker(stem, 'dependency-incomplete', dependencySeverity,
      waiting.length === 1 ? 'Waiting for ' + related.title : 'Waiting for ' + waiting.length + ' dependencies',
      context.state === 'backlog'
        ? 'Preparation can continue; Run will wait until the dependencies are complete.'
        : (waiting.length === 1 ? 'Complete ' + related.stem + ' before running.' : 'Complete the remaining dependencies before running.'),
      { relatedTaskStem: related && related.stem, source: 'dependency', recoverable: true }));
  }

  // Figma authoring diagnostics must not prevent backlog preparation: Prepare
  // is the action that normalizes Design and gathers its context. Missing
  // cached screens are advisory even for Run because the Board already owns an
  // explicit "pull first / run anyway" choice and the execution/finalization
  // pipeline still enforces evidence before completion.
  var figmaAuthoringSeverity = context.state === 'todo' ? 'blocking' : 'warning';
  if (context.figmaReviewPending) {
    out.push(blocker(stem, 'figma-review-required', figmaAuthoringSeverity, 'Visual review is required',
      'Review the current Figma comparison before the task can continue.',
      { source: 'figma', recoverable: true }));
  } else if (context.figmaDesignInvalid) {
    out.push(blocker(stem, 'figma-design-invalid', figmaAuthoringSeverity, 'The Design declaration needs fixing',
      'Fix the indicated Design line before pulling its Figma nodes.',
      { source: 'figma', recoverable: true }));
  } else if (context.figmaScreensMissing) {
    out.push(blocker(stem, 'figma-screens-missing', 'warning', 'Figma screens are missing',
      context.state === 'todo'
        ? 'Pull the declared screens before Run, or continue with the explicit Run anyway path.'
        : 'Preparation can continue and gather the declared screens.',
      { source: 'figma', recoverable: true }));
  }

  if (context.setupIncomplete) {
    out.push(blocker(stem, 'setup-incomplete', 'blocking', 'Project setup is incomplete',
      'Finish the required setup gates before running this task.',
      { source: 'setup', recoverable: true }));
  }
  if (context.reviewerUnavailable) {
    out.push(blocker(stem, 'reviewer-unavailable', 'blocking', 'Required reviewer is unavailable',
      'Review the configured reviewer and its current availability.',
      { source: 'reviewer', recoverable: true }));
  }
  if (context.stoppedRun) {
    out.push(blocker(stem, 'stopped-run', 'blocking', 'The previous run stopped',
      'Review the last run and retry only from a valid checkpoint.',
      { source: 'session', recoverable: !!context.retryCheckpoint }));
  }
  if (context.validationPending) {
    out.push(blocker(stem, 'validation-required', 'blocking', 'Manual validation is required',
      'Validate the remaining manual acceptance checks in the app.',
      { source: 'validation', recoverable: true }));
  }
  if (context.runnerUnavailable) {
    out.push(blocker(stem, 'runner-unavailable', 'warning', 'Task runner is unavailable',
      'Queue the action now; it will start automatically when a runner attaches.',
      { source: 'runtime', recoverable: true }));
  }

  var seen = Object.create(null);
  return out.filter(function (item) {
    if (seen[item.kind]) return false;
    seen[item.kind] = true;
    return true;
  }).sort(function (left, right) {
    // Never let an advisory warning visually mask a real execution blocker.
    // The primary-action resolver already uses the first blocking item; keep
    // the card's primaryBlocker projection aligned with that decision.
    var ls = left.severity === 'blocking' ? 0 : 1;
    var rs = right.severity === 'blocking' ? 0 : 1;
    if (ls !== rs) return ls - rs;
    var lp = PRIORITY[left.kind] || 999, rp = PRIORITY[right.kind] || 999;
    return lp - rp || left.id.localeCompare(right.id);
  });
}

module.exports = Object.freeze({
  KINDS: KINDS,
  PRIORITY: PRIORITY,
  blocker: blocker,
  fromContext: fromContext
});
