import { dom } from '../dom.js';

var el = dom.el;

export function taskBlocker(row, options) {
  var blocker = row.primaryBlocker;
  if (!blocker) return null;
  options = options || {};
  var t = options.t;
  var label = t('board.blocker.' + blocker.kind.replace(/-/g, '_'), {
    count: row.dependencySummary && row.dependencySummary.blockedCount || 0,
    task: blocker.relatedTaskStem || ''
  });
  var summary = t('board.blocker.summary.' + blocker.kind.replace(/-/g, '_'), {
    count: row.dependencySummary && row.dependencySummary.blockedCount || 0,
    task: blocker.relatedTaskStem || ''
  });
  var node = el('div', {
    class: 'board-card__blocker board-card__blocker--' + blocker.severity,
    attrs: {
      title: summary,
      'aria-label': label + '. ' + summary,
      'data-task-status': 'blocker'
    }
  });
  node.appendChild(el('span', { class: 'board-card__blocker-icon', text: blocker.severity === 'blocking' ? '!' : 'i', attrs: { 'aria-hidden': 'true' } }));
  node.appendChild(el('span', { class: 'board-card__blocker-text', text: label }));
  if (row.blockerCount > 1) node.appendChild(el('span', { class: 'board-card__blocker-more', text: '+' + (row.blockerCount - 1) }));
  return node;
}
