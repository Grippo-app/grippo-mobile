import { dom } from '../dom.js';
import { taskActionDisabledReason } from './task-action-copy.js';

var el = dom.el;

export function taskCardAction(row, options) {
  options = options || {};
  var action = row.primaryAction;
  var t = options.t;
  var params = { phase: action && action.checkpointId || '' };
  var labelKey = action && typeof action.labelKey === 'string' && action.labelKey.indexOf('board.action.') === 0
    ? action.labelKey
    : 'board.action.' + (action && action.kind ? action.kind.replace(/-/g, '_') : 'unavailable');
  var label = t(labelKey, params);
  var disabledReason = taskActionDisabledReason(action, t);
  var attrs = {
    'data-task-control': 'primary',
    'data-action-kind': action && action.kind || 'unavailable',
    'data-action-revision': action && action.actionRevision || ''
  };
  if (!action || action.enabled === false) {
    attrs['aria-disabled'] = 'true';
    attrs['aria-label'] = label + '. ' + disabledReason;
    attrs.title = disabledReason;
  }
  var opensTerminal = action && action.behavior === 'open-terminal';
  var button = el('button', {
    type: 'button',
    // Every terminal opener uses the shared accent-outline treatment; execution
    // actions keep the filled primary treatment.
    class: 'btn ' + (opensTerminal ? 'btn--terminal' : 'btn--primary') + ' board-card__primary-action',
    text: label,
    attrs: attrs
  });
  if (!action || action.enabled === false) button.disabled = true;
  button.addEventListener('click', function () {
    if (!action || action.enabled === false) return;
    if (action.behavior === 'execute' && typeof options.onExecute === 'function') options.onExecute(row, action, button);
    else if (typeof options.onNavigate === 'function') options.onNavigate(action.target, row, action);
  });
  return button;
}
