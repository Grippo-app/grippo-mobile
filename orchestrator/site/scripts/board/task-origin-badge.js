import { dom } from '../dom.js';

var el = dom.el;

function textFor(origin, t) {
  if (!origin) return t('board.origin.unavailable');
  return t('board.origin.' + origin.kind.replace(/-/g, '_'));
}

export function taskOriginBadge(row, options) {
  options = options || {};
  var t = options.t;
  var origin = row.origin;
  // A task-split lineage remains available through the Source overflow action
  // and the origin filters. Showing "Follow-up" on almost every child card is
  // repetitive chrome, not useful at-a-glance status.
  if (origin && origin.kind === 'follow-up') return null;
  var description = origin
    ? t('board.origin.description', { type: origin.type, ref: origin.ref })
    : t('board.origin.unavailableDescription');
  var target = row.sourceTarget;
  if (target && target.availability !== 'available') {
    description += ' ' + t('board.origin.targetMissing');
  }
  var className = 'board-card__origin board-card__origin--' + (origin ? origin.kind : 'unavailable');
  var attrs = {
    title: description,
    'aria-label': textFor(origin, t) + '. ' + description
  };
  return el('span', { class: className, text: textFor(origin, t), attrs: attrs });
}
