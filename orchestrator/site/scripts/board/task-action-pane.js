import { dom } from '../dom.js';
import { taskActionDisabledReason } from './task-action-copy.js';
import { taskQuestions } from './task-questions.js';

const el = dom.el;

function blockerSummary(item, t) {
  const key = item.summaryKey || 'board.blocker.summary.' + String(item.kind || '').replace(/-/g, '_');
  const translated = t(key);
  return translated === key ? String(item.kind || '') : translated;
}

function liveAnswer(details, t) {
  const work = details.currentWork;
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
  return {
    node: section,
    read: function () {
      const text = input.value.trim();
      return text ? {
        answers: [{ questionId: 1, optionIds: [], text: text }],
        liveSessionId: work.sessionId,
        expectedSessionRevision: work.sessionRevision
      } : null;
    }
  };
}

function genericAction(details, t) {
  const action = details.primaryAction;
  const work = details.currentWork || {};
  const section = el('section', { class: 'task-details__current-work task-details__action-card' });
  section.appendChild(el('h3', {
    class: 'task-details__section-title',
    text: t(action.labelKey)
  }));

  const deferred = (work.kind === 'questions' && action.kind !== 'submit-answers') ||
    (work.kind === 'awaiting-user' && action.kind !== 'continue-live');
  const stoppedRun = deferred && (details.blockers || []).some(function (item) {
    return item && item.kind === 'stopped-run';
  });
  if (deferred) section.appendChild(el('p', {
    class: 'banner banner--warn',
    text: t(stoppedRun
      ? 'taskDetails.questions.stoppedRun'
      : 'taskDetails.action.questionsDeferred')
  }));

  const inputMismatch = action.enabled !== false && (
    action.kind === 'submit-answers' && work.kind !== 'questions' ||
    action.kind === 'continue-live' && work.kind !== 'awaiting-user'
  );
  if (inputMismatch) section.appendChild(el('p', {
    class: 'banner banner--warn',
    text: t('taskDetails.questions.unavailable')
  }));

  if (details.blockers && details.blockers.length) {
    section.appendChild(el('h4', {
      class: 'task-details__action-subtitle',
      text: t('taskDetails.blockers')
    }));
    const list = el('ul', { class: 'task-details__list' });
    details.blockers.forEach(function (item) {
      list.appendChild(el('li', { text: blockerSummary(item, t) }));
    });
    section.appendChild(list);
  }

  const disabledReason = taskActionDisabledReason(action, t);
  section.appendChild(el('p', {
    class: disabledReason ? 'banner banner--warn' : 'task-details__action-hint',
    text: disabledReason || t('taskDetails.action.primaryHint', { action: t(action.labelKey) })
  }));
  return section;
}

export function taskActionPane(details, options) {
  const action = details && details.primaryAction;
  if (!action || action.attentionRequired !== true) return null;
  const t = options.t;
  const root = el('div', {
    class: 'task-details__action',
    attrs: { 'data-task-section': 'action', tabindex: '-1' }
  });
  let questions = null;
  let live = null;
  const work = details.currentWork || {};
  if (action.enabled !== false && action.kind === 'submit-answers' && work.kind === 'questions') {
    questions = taskQuestions(work, t);
    root.appendChild(questions.node);
  } else if (action.enabled !== false && action.kind === 'continue-live' && work.kind === 'awaiting-user') {
    live = liveAnswer(details, t);
    root.appendChild(live.node);
  } else {
    root.appendChild(genericAction(details, t));
  }
  return { node: root, questions: questions, liveAnswer: live };
}
