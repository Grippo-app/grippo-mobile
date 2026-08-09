import { dom } from '../dom.js';
import { taskQuestions } from './task-questions.js';
import { taskActivitySummaryText } from './task-activity.js';

const el = dom.el;

function list(items, emptyText, className) {
  if (!items || !items.length) return el('p', { class: 'task-details__empty', text: emptyText });
  const node = el('ul', { class: className || 'task-details__list' });
  items.forEach(function (item) {
    node.appendChild(el('li', { text: typeof item === 'string' ? item : item.label || item.stem || '' }));
  });
  return node;
}

function fact(label, value) {
  const displayValue = value || '—';
  const wrap = el('div', { class: 'task-details__fact' });
  wrap.appendChild(el('dt', { text: label, attrs: { title: label } }));
  wrap.appendChild(el('dd', {
    text: displayValue,
    attrs: { title: displayValue }
  }));
  return wrap;
}

function originText(origin, t) {
  if (!origin) return t('taskDetails.none');
  const key = 'board.origin.' + origin.kind.replace(/-/g, '_');
  const translated = t(key);
  return (translated === key ? origin.kind : translated) + (origin.ref ? ' · ' + origin.ref : '');
}

function blockerText(details, t) {
  const blocker = details.blockers && details.blockers[0];
  if (!blocker) return t('taskDetails.none');
  const key = blocker.summaryKey || 'board.blocker.summary.' + blocker.kind.replace(/-/g, '_');
  const translated = t(key);
  const summary = translated === key ? blocker.kind : translated;
  const issue = blocker.kind === 'figma-design-invalid' &&
    details.designIssues && details.designIssues.first;
  if (!issue) return summary;
  const issueKey = 'taskDetails.designIssue.kind.' + issue.kind;
  const issueText = t(issueKey);
  return summary + ' · ' + (issueText === issueKey
    ? t('taskDetails.designIssue.kind.unknown') : issueText) +
    (issue.line ? ' · ' + t('taskDetails.designIssue.line', { line: issue.line }) : '');
}

function actionText(details, t) {
  const action = details.primaryAction;
  return action ? t(action.labelKey) : t('taskDetails.none');
}

function retryRecovery(details, t) {
  const recovery = details.retryRecovery;
  if (!recovery || !recovery.freshness || recovery.freshness.current !== false) return null;
  const reason = String(recovery.freshness.reasonCode || 'unknown').replace(/-/g, '_');
  const reasonKey = 'taskDetails.retry.reason.' + reason;
  const translated = t(reasonKey);
  const section = el('section', { class: 'task-details__section task-details__retry-recovery' });
  section.appendChild(el('h3', {
    class: 'task-details__section-title',
    text: t('taskDetails.retry.unavailable')
  }));
  section.appendChild(el('p', {
    class: 'task-details__meta',
    text: translated === reasonKey ? t('taskDetails.retry.reason.unknown') : translated
  }));
  section.appendChild(el('p', {
    class: 'task-details__meta',
    text: t('taskDetails.retry.safeFallback', { action: actionText(details, t) })
  }));
  return section;
}

function dependencyText(details, t) {
  const deps = details.dependencies || {};
  if (!deps.count) return t('taskDetails.none');
  return t('taskDetails.dependencies.count', {
    satisfied: deps.satisfiedCount || 0,
    total: deps.count || 0
  });
}

function lastActivityText(details, t, formatTimestamp) {
  const activity = details.lastActivity;
  if (!activity) return t('taskDetails.none');
  const label = t(activity.labelKey || 'taskDetails.activity.unknown');
  return label + (activity.occurredAt ? ' · ' + formatTimestamp(activity.occurredAt) : '');
}

function delivered(details, options) {
  const t = options.t;
  const outcome = details.outcome || {};
  const node = el('article', { class: 'task-details__compare-card' });
  node.appendChild(el('h3', { class: 'task-details__compare-title', text: t('taskDetails.delivered') }));
  if (!outcome.present) {
    node.appendChild(el('p', {
      class: 'task-details__empty',
      text: details.state.column === 'done'
        ? t('taskDetails.outcome.missing') : t('taskDetails.outcome.inProgress')
    }));
    return node;
  }
  const statusKey = 'taskDetails.outcome.status.' +
    String(outcome.status || '').replace(/-/g, '_');
  const translatedStatus = t(statusKey);
  node.appendChild(el('p', {
    class: 'task-details__outcome-status' + (outcome.valid ? '' : ' task-details__outcome-status--invalid'),
    text: outcome.valid
      ? (translatedStatus === statusKey ? outcome.status : translatedStatus)
      : t('taskDetails.outcome.malformed')
  }));
  if (outcome.completedAt) node.appendChild(el('p', {
    class: 'task-details__meta',
    text: (options.formatTimestamp
      ? options.formatTimestamp(outcome.completedAt) : outcome.completedAt) +
      (outcome.reviewer ? ' · ' + outcome.reviewer : '')
  }));
  node.appendChild(list(outcome.acceptance, t('taskDetails.outcome.noAcceptance')));
  if (outcome.caveats && outcome.caveats.length) {
    node.appendChild(el('h4', { text: t('taskDetails.outcome.caveats') }));
    node.appendChild(list(outcome.caveats, ''));
  }
  if (outcome.followUps && outcome.followUps.length) {
    node.appendChild(el('h4', { text: t('taskDetails.outcome.followUps') }));
    node.appendChild(list(outcome.followUps, ''));
  }
  const counts = [
    [t('taskDetails.outcome.files'), outcome.files && outcome.files.length || 0],
    [t('taskDetails.outcome.buildResults'), outcome.buildGates && outcome.buildGates.length || 0],
    [t('taskDetails.outcome.runtimeResults'), outcome.runtimeVerify && outcome.runtimeVerify.length || 0]
  ];
  const countList = el('dl', { class: 'task-details__result-counts' });
  counts.forEach(function (row) {
    countList.appendChild(el('dt', { text: row[0] }));
    countList.appendChild(el('dd', { text: String(row[1]) }));
  });
  node.appendChild(countList);
  if (details.appValidation && details.appValidation.required) {
    node.appendChild(el('p', {
      class: 'task-details__validation-status',
      text: details.appValidation.current
        ? t('taskDetails.validation.current')
        : t('taskDetails.validation.required')
    }));
  }
  return node;
}

function requested(details, options) {
  const t = options.t;
  const requirement = details.requirement || {};
  const node = el('article', { class: 'task-details__compare-card' });
  node.appendChild(el('h3', { class: 'task-details__compare-title', text: t('taskDetails.requested') }));
  node.appendChild(el('p', { class: 'task-details__goal', text: requirement.goal || t('taskDetails.none') }));
  if (requirement.inputs && requirement.inputs.length) {
    node.appendChild(el('h4', { text: t('taskDetails.inputs') }));
    node.appendChild(list(requirement.inputs, ''));
  }
  node.appendChild(el('h4', { text: t('taskDetails.acceptance.automated') }));
  node.appendChild(list(requirement.acceptance && requirement.acceptance.automated,
    t('taskDetails.acceptance.empty')));
  node.appendChild(el('h4', { text: t('taskDetails.acceptance.manual') }));
  node.appendChild(list(requirement.acceptance && requirement.acceptance.manual,
    t('taskDetails.acceptance.empty')));
  if (requirement.outOfScope && requirement.outOfScope.length) {
    node.appendChild(el('h4', { text: t('taskDetails.outOfScope') }));
    node.appendChild(list(requirement.outOfScope, ''));
  }
  if (requirement.sources && requirement.sources.length) {
    node.appendChild(el('h4', { text: t('taskDetails.sources') }));
    const sources = el('ul', { class: 'task-details__sources' });
    requirement.sources.forEach(function (item) {
      const row = el('li', { class: 'task-details__source' });
      row.appendChild(el('span', {
        class: 'task-details__source-label',
        text: item.label || item.ref || item.type || item.kind
      }));
      const target = item.target;
      if (target && target.availability !== 'missing' && options.onOpenTarget) {
        const open = el('button', {
          type: 'button',
          class: 'btn btn--sm',
          text: t('taskDetails.sources.open')
        });
        open.addEventListener('click', function () { options.onOpenTarget(target); });
        row.appendChild(open);
      } else if (target && target.availability === 'missing') {
        row.appendChild(el('span', {
          class: 'task-details__meta',
          text: t('taskDetails.sources.unavailable')
        }));
      }
      sources.appendChild(row);
    });
    node.appendChild(sources);
  }
  return node;
}

function intakeWork(details, options) {
  const intake = details.currentWork && details.currentWork.intake;
  if (!intake) return null;
  const t = options.t;
  const section = el('details', {
    class: 'task-details__current-work board-intake',
    attrs: { 'data-task-section': 'intake', tabindex: '-1' }
  });
  const title = el('summary', { class: 'board-intake__head' });
  title.appendChild(el('strong', { text: t('board.intake.title') }));
  const statusKey = intake.status === 'complete'
    ? 'board.intake.readiness.' + intake.readiness
    : 'board.intake.' + intake.status;
  title.appendChild(el('span', {
    class: 'board-intake__status board-intake__status--' + (intake.readiness || intake.status),
    text: t(statusKey)
  }));
  section.appendChild(title);
  section.appendChild(el('p', {
    class: 'board-intake__note',
    text: t('board.intake.advisory')
  }));
  if (intake.status === 'queued' || intake.status === 'checking') {
    section.appendChild(el('p', { class: 'task-details__meta', text: t('board.intake.waiting') }));
  } else if (intake.status === 'failed') {
    const failureKeys = {
      MODEL_KEYCHAIN_SANDBOX_INCOMPATIBLE: 'board.intake.failureKeychainSandbox',
      MODEL_SCHEMA_TRANSPORT_INCOMPATIBLE: 'board.intake.failureSchemaTransport',
      INTAKE_CONTEXT_TOO_LARGE: 'board.intake.failureContextTooLarge',
      INTAKE_CONTEXT_INVALID: 'board.intake.failureContextInvalid',
      INTAKE_BUSY: 'board.intake.failureBusy',
      INTAKE_CLI_UNAVAILABLE: 'board.intake.failureCliUnavailable',
      INTAKE_MODEL_TIMEOUT: 'board.intake.failureTimeout',
      INTAKE_MODEL_OUTPUT_TOO_LARGE: 'board.intake.failureOutputTooLarge',
      INTAKE_MODEL_OUTPUT_INVALID: 'board.intake.failureOutputInvalid',
      INTAKE_MODEL_FAILED: 'board.intake.failureModel',
      INTAKE_RUNTIME_UNAVAILABLE: 'board.intake.failureRuntime'
    };
    section.appendChild(el('p', {
      class: 'banner banner--warn',
      text: t(failureKeys[intake.errorCode] || 'board.intake.failureDetail')
    }));
  } else if (intake.status === 'complete') {
    section.appendChild(el('p', {
      class: 'board-intake__summary',
      text: intake.summary || t('taskDetails.none')
    }));
    if (Array.isArray(intake.likelyAreas) && intake.likelyAreas.length) {
      const areas = el('div', { class: 'board-intake__chips' });
      intake.likelyAreas.forEach(function (area) {
        areas.appendChild(el('code', { class: 'board-intake__chip', text: area }));
      });
      section.appendChild(areas);
    }
    [
      ['board.intake.duplicates', intake.possibleDuplicates, 'reason'],
      ['board.intake.missing', intake.missingContext, 'item'],
      ['board.intake.risks', intake.riskFlags, 'reason']
    ].forEach(function (group) {
      if (!Array.isArray(group[1]) || !group[1].length) return;
      const block = el('div', { class: 'board-intake__block' });
      block.appendChild(el('strong', { text: t(group[0]) }));
      const rows = el('ul', { class: 'board-intake__list' });
      group[1].forEach(function (item) {
        const value = item && (item[group[2]] || item.stem);
        if (value) rows.appendChild(el('li', { text: value }));
      });
      block.appendChild(rows);
      section.appendChild(block);
    });
  }
  if ((intake.status === 'failed' || intake.status === 'complete') &&
      intake.sourceHash && options.onRetryIntake && options.onDismissIntake) {
    const actions = el('div', { class: 'board-intake__actions' });
    const retry = el('button', {
      type: 'button', class: 'btn btn--ghost', text: t('board.intake.retry')
    });
    retry.disabled = intake.status === 'failed' && intake.retryable === false;
    retry.addEventListener('click', function () { options.onRetryIntake(intake, retry); });
    const dismiss = el('button', {
      type: 'button', class: 'btn btn--ghost', text: t('board.intake.dismiss')
    });
    dismiss.addEventListener('click', function () { options.onDismissIntake(intake, dismiss); });
    actions.appendChild(retry);
    actions.appendChild(dismiss);
    section.appendChild(actions);
  }
  return section;
}

export function taskOverview(details, options) {
  const t = options.t;
  const root = el('div', { class: 'task-details__overview' });
  const facts = el('dl', { class: 'task-details__facts' });
  facts.appendChild(fact(t('taskDetails.fact.blocker'), blockerText(details, t)));
  facts.appendChild(fact(t('taskDetails.fact.lastActivity'),
    lastActivityText(details, t, options.formatTimestamp)));
  facts.appendChild(fact(t('taskDetails.fact.source'), originText(details.origin, t)));
  facts.appendChild(fact(t('taskDetails.fact.dependencies'), dependencyText(details, t)));
  root.appendChild(facts);

  if (details.partial) root.appendChild(el('p', {
    class: 'banner banner--warn',
    text: t('taskDetails.partial')
  }));
  const compare = el('div', { class: 'task-details__compare' });
  compare.appendChild(requested(details, options));
  compare.appendChild(delivered(details, options));
  root.appendChild(compare);

  const activity = details.activitySummary || {};
  const progress = el('section', { class: 'task-details__section task-details__progress' });
  progress.appendChild(el('h3', {
    class: 'task-details__section-title',
    text: t('taskDetails.progress.title')
  }));
  progress.appendChild(el('p', {
    class: 'task-details__activity-lead',
    text: taskActivitySummaryText(activity, t)
  }));
  const progressFacts = el('dl', { class: 'task-details__progress-facts' });
  [
    [t('taskDetails.progress.currentPhase'), activity.currentPhase, true],
    [t('taskDetails.progress.lastCompleted'), activity.lastCompletedPhase, true],
    [t('taskDetails.progress.lastFailed'), activity.lastFailedPhase, true],
    [t('taskDetails.progress.retries'), Number.isSafeInteger(activity.retryCount)
      ? String(activity.retryCount) : null],
    [t('taskDetails.progress.followUps'), Number.isSafeInteger(activity.spawnedFollowUpCount)
      ? String(activity.spawnedFollowUpCount) : null]
  ].forEach(function (row) {
    if (row[1] == null || row[1] === '') return;
    progressFacts.appendChild(el('dt', { text: row[0] }));
    progressFacts.appendChild(el('dd', {
      text: row[2] === true
        ? t('taskDetails.phase.' + String(row[1]).replace(/-/g, '_')) : row[1]
    }));
  });
  progress.appendChild(progressFacts);
  root.appendChild(progress);

  const recovery = retryRecovery(details, t);
  if (recovery) root.appendChild(recovery);

  const intake = intakeWork(details, options);
  if (intake) root.appendChild(intake);
  let questions = null;
  if (details.currentWork && details.currentWork.kind === 'questions') {
    questions = taskQuestions(details.currentWork, t, {
      notice: (details.blockers || []).some(function (item) { return item && item.kind === 'stopped-run'; })
        ? 'taskDetails.questions.stoppedRun' : null
    });
    root.appendChild(questions.node);
  }
  let liveAnswer = null;
  if (details.currentWork && details.currentWork.kind === 'awaiting-user') {
    const section = el('section', {
      class: 'task-details__current-work task-details__questions',
      attrs: { 'data-task-section': 'questions', 'aria-labelledby': 'task-details-live-answer-title' }
    });
    section.appendChild(el('h3', {
      id: 'task-details-live-answer-title',
      class: 'task-details__section-title',
      text: t('taskDetails.liveAnswer.title')
    }));
    section.appendChild(el('p', {
      class: 'task-details__meta',
      text: t('taskDetails.liveAnswer.hint')
    }));
    const input = el('textarea', {
      class: 'input task-details__question-text',
      attrs: {
        rows: '4',
        'data-task-live-answer': 'true',
        placeholder: t('taskDetails.liveAnswer.placeholder'),
        'aria-label': t('taskDetails.liveAnswer.label')
      }
    });
    section.appendChild(input);
    root.appendChild(section);
    liveAnswer = {
      node: section,
      read: function () {
        const text = input.value.trim();
        return text ? {
          answers: [{ questionId: 1, optionIds: [], text: text }],
          liveSessionId: details.currentWork.sessionId,
          expectedSessionRevision: details.currentWork.sessionRevision
        } : null;
      }
    };
  }

  if (details.blockers && details.blockers.length) {
    const blockers = el('section', { class: 'task-details__section' });
    blockers.appendChild(el('h3', {
      class: 'task-details__section-title',
      text: t('taskDetails.blockers')
    }));
    blockers.appendChild(list(details.blockers.map(function (item) {
      const key = item.summaryKey || 'board.blocker.summary.' + item.kind.replace(/-/g, '_');
      const value = t(key);
      return value === key ? item.kind : value;
    }), t('taskDetails.none')));
    root.appendChild(blockers);
  }

  const dependencies = el('section', {
    class: 'task-details__section',
    attrs: { 'data-task-section': 'dependencies', tabindex: '-1' }
  });
  dependencies.appendChild(el('h3', {
    class: 'task-details__section-title',
    text: t('taskDetails.dependencies.title')
  }));
  if (details.dependencies && details.dependencies.blocksCount) {
    dependencies.appendChild(el('p', {
      class: 'task-details__meta',
      text: t('taskDetails.dependencies.blocks', {
        count: details.dependencies.blocksCount
      })
    }));
  }
  const dependencyItems = details.dependencies && details.dependencies.items || [];
  if (!dependencyItems.length) {
    dependencies.appendChild(el('p', {
      class: 'task-details__empty', text: t('taskDetails.dependencies.empty')
    }));
  } else {
    const dependencyList = el('ul', { class: 'task-details__dependencies' });
    dependencyItems.forEach(function (item) {
      const row = el('li', { class: 'task-details__dependency' });
      const status = item.missing ? 'missing' : item.satisfied ? 'satisfied' : 'blocked';
      const summary = el('span', { class: 'task-details__dependency-summary' });
      summary.appendChild(el('span', {
        text: (item.title || item.stem) +
          (item.title && item.title !== item.stem ? ' · ' + item.stem : '') +
          ' · ' + t('taskDetails.dependencies.' + status)
      }));
      if (item.lastActivity) summary.appendChild(el('span', {
        class: 'task-details__meta',
        text: t(item.lastActivity.labelKey || 'taskDetails.activity.unknown') +
          (item.lastActivity.occurredAt
            ? ' · ' + options.formatTimestamp(item.lastActivity.occurredAt) : '')
      }));
      row.appendChild(summary);
      if (!item.missing && options.onOpenTarget) {
        const open = el('button', {
          type: 'button', class: 'btn btn--sm',
          text: t('taskDetails.dependencies.open')
        });
        open.addEventListener('click', function () {
          options.onOpenTarget({ type: 'task', stem: item.stem, section: 'overview' });
        });
        row.appendChild(open);
      }
      dependencyList.appendChild(row);
    });
    dependencies.appendChild(dependencyList);
  }
  root.appendChild(dependencies);
  return { node: root, questions: questions, liveAnswer: liveAnswer };
}
