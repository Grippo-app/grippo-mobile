import { dom } from '../dom.js';
import { taskActionDisabledReason } from './task-action-copy.js';

const el = dom.el;

export function taskActionBar(details, options) {
  const root = el('div', { class: 'board-modal__actions task-details__action-bar' });
  const action = details.primaryAction;
  if (action) {
    const disabledReason = taskActionDisabledReason(action, options.t);
    const label = options.t(action.labelKey);
    const attrs = { 'data-task-details-primary': action.kind };
    if (disabledReason) {
      attrs.title = disabledReason;
      attrs['aria-label'] = label + '. ' + disabledReason;
    }
    const primary = el('button', {
      type: 'button',
      class: action.behavior === 'open-terminal' ? 'btn btn--terminal' : 'btn btn--primary',
      text: label,
      disabled: action.enabled === false,
      attrs: attrs
    });
    primary.addEventListener('click', function () {
      if (action.enabled === false) return;
      options.onPrimary(action, primary);
    });
    root.appendChild(primary);
  }
  const close = el('button', {
    type: 'button',
    class: 'btn board-modal__close-btn',
    text: options.t('board.modal.close')
  });
  close.addEventListener('click', options.onClose);
  root.appendChild(close);
  return root;
}

export function taskDetailsOverflow(details, options) {
  const menu = el('details', { class: 'task-details__overflow' });
  const trigger = el('summary', {
    class: 'btn task-details__overflow-trigger',
    text: options.t('taskDetails.more'),
    attrs: { 'aria-label': options.t('taskDetails.more') }
  });
  menu.appendChild(trigger);
  const list = el('div', { class: 'task-details__overflow-menu', attrs: { role: 'menu' } });
  function closeAndReturnFocus() {
    menu.open = false;
    if (trigger.isConnected) trigger.focus();
  }
  menu.addEventListener('keydown', function (event) {
    if (event.key === 'Escape' && menu.open) {
      event.preventDefault();
      // The modal's document-level Escape handler runs on the bubble; without
      // this, dismissing the menu also closed the task the user was reading.
      event.stopPropagation();
      closeAndReturnFocus();
      return;
    }
    if (!menu.open || ['ArrowDown', 'ArrowUp', 'Home', 'End'].indexOf(event.key) < 0) return;
    const items = Array.from(list.querySelectorAll('[role="menuitem"]'));
    if (!items.length) return;
    event.preventDefault();
    const current = items.indexOf(document.activeElement);
    const next = event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1 :
      event.key === 'ArrowDown' ? (current + 1 + items.length) % items.length :
        (current - 1 + items.length) % items.length;
    items[next].focus();
  });
  const copyId = el('button', {
    type: 'button', class: 'task-details__overflow-item',
    text: options.t('board.overflow.copy_id'), attrs: { role: 'menuitem' }
  });
  copyId.addEventListener('click', function () {
    options.onAction('copy-id', null);
    closeAndReturnFocus();
  });
  list.appendChild(copyId);
  const common = [
    ['copy-link', 'taskDetails.overflow.copyLink', true],
    ['source', 'taskDetails.overflow.openSource', !!(details.origin && details.origin.target)],
    ['edit', 'taskDetails.overflow.edit', details.state && details.state.column === 'backlog'],
    ['open-terminal', 'taskDetails.overflow.openTerminal',
      !!(details.state && (details.state.runtime === 'running' || details.state.runtime === 'awaiting'))],
    ['export-result', 'taskDetails.overflow.exportResult', !!(details.outcome && details.outcome.present)],
    ['advanced', 'taskDetails.overflow.advanced', details.advancedAvailable === true]
  ];
  common.forEach(function (spec) {
    if (!spec[2]) return;
    const button = el('button', {
      type: 'button', class: 'task-details__overflow-item',
      text: options.t(spec[1]), attrs: { role: 'menuitem' }
    });
    button.addEventListener('click', function () {
      options.onAction(spec[0], null);
      closeAndReturnFocus();
    });
    list.appendChild(button);
  });
  (details.secondaryActions || []).forEach(function (action) {
    if (action.enabled === false) return;
    const labelKey = action.kind === 'copy-prompt' &&
      details.primaryAction && details.primaryAction.kind === 'submit-answers'
      ? 'board.overflow.copy_answers_prompt' : action.labelKey;
    const button = el('button', {
      type: 'button',
      class: 'task-details__overflow-item' +
        (action.kind === 'drop' ? ' task-details__overflow-item--danger' : ''),
      text: options.t(labelKey),
      attrs: { role: 'menuitem' }
    });
    button.addEventListener('click', function () {
      options.onAction(action.kind, action);
      closeAndReturnFocus();
    });
    list.appendChild(button);
  });
  menu.appendChild(list);
  return menu;
}
