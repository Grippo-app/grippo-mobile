import { dom } from '../dom.js';

const el = dom.el;
const ACTIVITY_STATUSES = [
  'completed', 'awaiting-user', 'failed', 'running',
  'needs-answers', 'ready', 'preparing'
];

export function taskActivitySummaryText(summary, t) {
  const status = summary && ACTIVITY_STATUSES.indexOf(summary.status) >= 0
    ? summary.status : 'unknown';
  return t('taskDetails.activity.summary.' + status.replace(/-/g, '_'));
}

export function renderTaskActivity(target, page, options) {
  while (target.firstChild) target.removeChild(target.firstChild);
  const t = options.t;
  const summary = page.summary || {};
  target.appendChild(el('section', { class: 'task-details__activity-summary' }, [
    el('h3', { class: 'task-details__section-title', text: t('taskDetails.activity.title') }),
    el('p', {
      class: 'task-details__activity-lead',
      text: taskActivitySummaryText(summary, t)
    })
  ]));
  const facts = el('dl', { class: 'task-details__progress-facts' });
  [
    [t('taskDetails.progress.currentPhase'), summary.currentPhase, true],
    [t('taskDetails.progress.lastCompleted'), summary.lastCompletedPhase, true],
    [t('taskDetails.progress.lastFailed'), summary.lastFailedPhase, true],
    [t('taskDetails.progress.retries'), Number.isSafeInteger(summary.retryCount)
      ? String(summary.retryCount) : null],
    [t('taskDetails.progress.followUps'), Number.isSafeInteger(summary.spawnedFollowUpCount)
      ? String(summary.spawnedFollowUpCount) : null],
    [t('taskDetails.progress.elapsed'), Number.isSafeInteger(summary.totalElapsedMs)
      ? t('taskDetails.progress.seconds', {
        seconds: Math.round(summary.totalElapsedMs / 1000)
      }) : null]
  ].forEach(function (row) {
    if (row[1] == null || row[1] === '') return;
    facts.appendChild(el('dt', { text: row[0] }));
    facts.appendChild(el('dd', {
      text: row[2] === true
        ? t('taskDetails.phase.' + String(row[1]).replace(/-/g, '_')) : row[1]
    }));
  });
  target.appendChild(facts);
  if (page.outcomeDigest && page.outcomeDigest.length) {
    const digest = el('details', { class: 'task-details__phase' });
    digest.appendChild(el('summary', {
      text: t('taskDetails.activity.outcomeDigest')
    }));
    const rows = el('ul', { class: 'task-details__phase-events' });
    page.outcomeDigest.forEach(function (item) {
      rows.appendChild(el('li', { text: item }));
    });
    digest.appendChild(rows);
    target.appendChild(digest);
  }
  if (!page.groups.length) {
    if (!page.outcomeDigest || !page.outcomeDigest.length) {
      target.appendChild(el('p', {
        class: 'task-details__empty',
        text: t('taskDetails.activity.empty')
      }));
    }
    return;
  }
  const list = el('ol', { class: 'task-details__timeline' });
  page.groups.forEach(function (group) {
    const item = el('li', { class: 'task-details__timeline-item' });
    const disclosure = el('details', { class: 'task-details__phase' });
    const title = (group.phase ? t('taskDetails.phase.' + group.phase.replace(/-/g, '_')) :
      t('taskDetails.activity.lifecycle')) +
      (Number.isSafeInteger(group.attempt)
        ? ' · ' + t('taskDetails.activity.attempt', { attempt: group.attempt }) : '') + ' · ' +
      t('taskDetails.status.' + (group.status || 'unknown').replace(/-/g, '_'));
    disclosure.appendChild(el('summary', { text: title }));
    const details = el('dl', { class: 'task-details__phase-details' });
    [
      [t('taskDetails.activity.started'), group.startedAt, true],
      [t('taskDetails.activity.ended'), group.endedAt, true],
      [t('taskDetails.activity.retries'), String(group.retryCount || 0)],
      [t('taskDetails.activity.stopReason'), group.stopReason],
      [t('taskDetails.activity.checkpoint'), group.checkpointId],
      [t('taskDetails.activity.report'), group.reportId],
      [t('taskDetails.activity.children'), group.children && group.children.join(', ')]
    ].forEach(function (row) {
      if (!row[1]) return;
      details.appendChild(el('dt', { text: row[0] }));
      details.appendChild(el('dd', {
        text: row[2] && options.formatTimestamp
          ? options.formatTimestamp(row[1]) : row[1]
      }));
    });
    disclosure.appendChild(details);
    if (group.events && group.events.length) {
      const events = el('ol', { class: 'task-details__phase-events' });
      group.events.forEach(function (event) {
        const labelKey = 'board.activity.' + String(event.kind || '').replace(/-/g, '_');
        const translated = t(labelKey);
        const statusKey = 'taskDetails.status.' + String(event.status || 'unknown').replace(/-/g, '_');
        const parts = [
          event.ts && options.formatTimestamp ? options.formatTimestamp(event.ts) : event.ts,
          translated === labelKey ? event.kind : translated,
          t(statusKey),
          event.detail || event.meta && event.meta.reasonCode || null
        ].filter(Boolean);
        events.appendChild(el('li', { text: parts.join(' · ') }));
      });
      disclosure.appendChild(events);
    }
    item.appendChild(disclosure);
    list.appendChild(item);
  });
  target.appendChild(list);
  if (page.nextCursor && options.loadMore) {
    const more = el('button', {
      type: 'button', class: 'btn btn--sm task-details__load-more',
      text: t('taskDetails.loadMore')
    });
    more.addEventListener('click', function () { options.loadMore(page.nextCursor, more); });
    target.appendChild(more);
  }
  if (page.partial) target.appendChild(el('p', {
    class: 'banner banner--warn',
    text: t('taskDetails.activity.partial')
  }));
}
