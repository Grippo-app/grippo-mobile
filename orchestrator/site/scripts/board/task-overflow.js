import { dom } from '../dom.js';
import { taskOverflowItems } from './task-overflow-policy.js';

var el = dom.el;

export function taskOverflow(row, options) {
  options = options || {};
  var t = options.t;
  var wrap = el('div', { class: 'board-card__overflow' });
  var menuId = 'task-overflow-' + row.stem.replace(/[^A-Za-z0-9_-]/g, '-');
  var trigger = el('button', {
    type: 'button', class: 'btn btn--ghost board-card__overflow-trigger', text: '⋯',
    attrs: {
      'aria-label': t('board.overflow.label'), 'aria-haspopup': 'menu',
      'aria-controls': menuId, 'aria-expanded': 'false',
      'data-task-control': 'overflow'
    }
  });
  var menu = el('div', { class: 'board-card__overflow-menu', id: menuId, attrs: { role: 'menu' } });
  menu.hidden = true;

  function setOpen(open, focusTrigger, focusFirst) {
    menu.hidden = !open;
    trigger.setAttribute('aria-expanded', open ? 'true' : 'false');
    if (open) {
      if (typeof options.onMenuOpen === 'function') options.onMenuOpen(row.stem, menu, trigger);
      if (options.menuState) options.menuState.openMenu(row.stem);
      if (focusFirst !== false) {
        var first = menu.querySelector('button:not([disabled])');
        if (first) first.focus();
      }
    } else {
      if (options.menuState) options.menuState.closeMenu(row.stem);
      if (focusTrigger) trigger.focus();
    }
  }

  taskOverflowItems(row).forEach(function (itemSpec) {
    var action = itemSpec.kind;
    var serverAction = itemSpec.serverAction;
    var mutationBlocked = options.mutationsBlocked === true &&
      (action === 'edit' || action === 'reopen');
    // Menus expose available choices only. Keep navigation/copy actions usable
    // during startup recovery, and omit task mutations instead of rendering
    // dead controls that imply a broken click target.
    if (mutationBlocked) return;
    var itemClass = 'board-card__overflow-item';
    if (itemSpec.separated) itemClass += ' board-card__overflow-item--separated';
    if (itemSpec.tone) itemClass += ' board-card__overflow-item--' + itemSpec.tone;
    var item = el('button', {
      type: 'button', class: itemClass,
      text: t(itemSpec.labelKey),
      attrs: {
        role: 'menuitem', 'data-overflow-action': action,
        'data-action-group': itemSpec.group,
        'data-task-control': 'overflow-' + action
      }
    });
    item.addEventListener('click', function () {
      setOpen(false, true);
      if (typeof options.onAction === 'function') options.onAction(action, row, serverAction);
    });
    item.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') { event.preventDefault(); setOpen(false, true); }
    });
    menu.appendChild(item);
  });
  trigger.addEventListener('click', function () { setOpen(menu.hidden); });
  trigger.addEventListener('keydown', function (event) {
    if (event.key === 'ArrowDown') { event.preventDefault(); setOpen(true); }
    if (event.key === 'Escape') { event.preventDefault(); setOpen(false); }
  });
  menu.addEventListener('keydown', function (event) {
    var items = Array.prototype.slice.call(menu.querySelectorAll('[role="menuitem"]:not([disabled])'));
    var at = items.indexOf(document.activeElement);
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      var next = (at + (event.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length;
      if (items[next]) items[next].focus();
    }
    if (event.key === 'Escape') { event.preventDefault(); setOpen(false, true); }
  });
  wrap.appendChild(trigger);
  wrap.appendChild(menu);
  if (options.menuState && options.menuState.isMenuOpen(row.stem)) setOpen(true, false, false);
  return wrap;
}
