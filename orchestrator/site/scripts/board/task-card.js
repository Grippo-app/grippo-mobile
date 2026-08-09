import { dom } from '../dom.js';
import { taskOriginBadge } from './task-origin-badge.js';
import { taskBlocker } from './task-blocker.js';
import { taskCardAction } from './task-card-action.js';
import { taskOverflow } from './task-overflow.js';

var el = dom.el;

function taskNumber(stem) {
  var match = /^TASK_([1-9][0-9]*)_/.exec(stem || '');
  return match ? 'TASK ' + match[1] : stem;
}

function dependencyLine(row, t) {
  var summary = row.dependencySummary;
  if (!summary || !summary.count) return t('board.dependencies.none');
  if (summary.blockedCount > 0) return t('board.dependencies.blocked', { count: summary.blockedCount });
  return t('board.dependencies.count', { count: summary.count });
}

function activityLine(row, options) {
  var activity = row.lastActivity;
  if (!activity) return options.t('board.activity.unavailable');
  var event = options.t(activity.labelKey || 'board.activity.' + activity.kind.replace(/-/g, '_'), {
    phase: activity.phase || '', status: activity.status || ''
  });
  var when = typeof options.formatRelative === 'function' ? options.formatRelative(activity.occurredAt) : activity.occurredAt;
  return options.t('board.activity.line', { event: event, when: when || options.t('board.timeUnknown') });
}

function compactStatus(row, t) {
  var statusKeys = {
    queued: 'queued', checking: 'checking', complete: 'complete', failed: 'failed',
    ready: 'ready', 'possible-duplicate': 'possible_duplicate', 'needs-context': 'needs_context',
    pending: 'pending', completed: 'completed',
    'completed-with-caveats': 'completed_with_caveats', 'partially-completed': 'partially_completed'
  };
  var labels = (row.compactSignals || []).slice(0, 2).map(function (signal) {
    var statusKey = statusKeys[signal.status] || 'unknown';
    return t('board.signal.' + signal.kind.replace(/-/g, '_'), {
      status: t('board.signalStatus.' + statusKey),
      count: signal.count == null ? '' : signal.count
    });
  });
  return labels.join(' · ');
}

export function taskCard(row, options) {
  options = options || {};
  var t = options.t;
  var titleId = 'task-card-title-' + row.stem;
  var contextId = 'task-card-context-' + row.stem;
  var health = row.taskHealth && ['error', 'warning'].indexOf(row.taskHealth.severity) >= 0
    ? row.taskHealth.severity : null;
  var card = el('article', {
    class: 'board-card' +
      (row.primaryBlocker && row.primaryBlocker.severity === 'blocking' ? ' board-card--blocked' : '') +
      (health ? ' board-card--' + health : ''),
    attrs: {
      'data-folder': row.state, 'data-stem': row.stem,
      'aria-labelledby': titleId, 'aria-describedby': contextId
    }
  });
  var header = el('header', { class: 'board-card__header' });
  var identity = el('div', { class: 'board-card__identity' });
  identity.appendChild(el('span', { class: 'board-card__number', text: taskNumber(row.stem) }));
  var title = el('h4', { class: 'board-card__title', id: titleId });
  var titleButton = el('button', {
    type: 'button', class: 'board-card__title-button', text: row.title,
    attrs: { 'data-task-control': 'details', title: t('board.openDetails') }
  });
  titleButton.addEventListener('click', function () { options.onOpenDetails(row); });
  title.appendChild(titleButton);
  identity.appendChild(title);
  header.appendChild(identity);
  header.appendChild(taskOverflow(row, options));
  var originBadge = taskOriginBadge(row, options);
  if (originBadge) {
    var badges = el('div', { class: 'board-card__header-badges' });
    badges.appendChild(originBadge);
    if (health) badges.appendChild(el('span', {
      class: 'board-card__health board-card__health--' + health,
      text: t('board.integrity.severity.' + health)
    }));
    header.appendChild(badges);
  } else if (health) {
    var healthBadges = el('div', { class: 'board-card__header-badges' });
    healthBadges.appendChild(el('span', {
      class: 'board-card__health board-card__health--' + health,
      text: t('board.integrity.severity.' + health)
    }));
    header.appendChild(healthBadges);
  }
  card.appendChild(header);

  var context = el('div', { class: 'board-card__context', id: contextId });
  var blocker = taskBlocker(row, options);
  if (blocker) context.appendChild(blocker);
  if (row.dependencySummary && row.dependencySummary.count > 0) {
    context.appendChild(el('p', { class: 'board-card__dependencies', text: dependencyLine(row, t) }));
  }
  if (row.runtimeStatus && row.runtimeStatus.state === 'waiting-runner') {
    context.appendChild(el('p', {
      class: 'board-card__runtime board-card__runtime--waiting',
      text: t('board.runtime.waiting_runner')
    }));
  }
  context.appendChild(el('p', { class: 'board-card__activity', text: activityLine(row, options) }));
  var signals = compactStatus(row, t);
  if (signals) context.appendChild(el('p', { class: 'board-card__signals', text: signals }));
  card.appendChild(context);

  if (row.state !== 'done' && row.primaryAction && row.primaryAction.enabled !== false) {
    var footer = el('footer', { class: 'board-card__footer' });
    footer.appendChild(taskCardAction(row, options));
    card.appendChild(footer);
  }

  // The card surface is a pointer shortcut to the same details view as the
  // title. Nested controls keep their own behavior and never bubble into a
  // second action. The semantic, keyboard-accessible entry point remains the
  // title button because an article cannot itself become a button while it
  // contains other buttons.
  card.addEventListener('click', function (event) {
    var target = event.target;
    var control = target && target.closest
      ? target.closest('button, a, input, select, textarea, [role="button"], [role="menuitem"]')
      : null;
    if (control && card.contains(control)) return;
    if (typeof options.onOpenDetails === 'function') options.onOpenDetails(row);
  });
  return card;
}
