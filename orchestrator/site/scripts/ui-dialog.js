import { dom } from './dom.js';
import { i18n } from './i18n.js';

// ----------------------------------------------------------------------
// Shared confirm / prompt dialogs.
//
// The product ships one dialog language: a native <dialog> on --surface with
// the site's own buttons, backdrop and focus handling. window.confirm/prompt
// render OS chrome that ignores the theme, cannot be localized beyond their
// body text, truncate long bodies, and cannot show structure — the API package
// preview in particular was a multi-section report squeezed into an alert.
//
// Both entry points resolve rather than reject: `confirm` → boolean,
// `prompt` → the typed string, or null when the user cancels (never '' — an
// empty string is a real answer and must stay distinguishable from a dismissal).
// ----------------------------------------------------------------------

var el = dom.el;

function t(key, params) {
  return i18n && typeof i18n.t === 'function' ? i18n.t(key, params) : key;
}

function buildShell(options) {
  var dialog = el('dialog', { class: 'ui-dialog' + (options.danger ? ' ui-dialog--danger' : '') });
  var titleId = 'ui-dialog-title-' + Math.random().toString(36).slice(2);
  dialog.setAttribute('aria-labelledby', titleId);
  var body = el('div', { class: 'ui-dialog__body' });
  body.appendChild(el('h3', { id: titleId, class: 'ui-dialog__title', text: options.title }));
  if (options.message) body.appendChild(el('p', { class: 'ui-dialog__message', text: options.message }));
  return { dialog: dialog, body: body };
}

// `lines` renders the structured half of a preview: [{ text, level }] where
// level 0 is a group and 1 a child. A plain string is treated as level 0.
function appendLines(body, lines) {
  if (!lines || !lines.length) return;
  var list = el('ul', { class: 'ui-dialog__lines' });
  for (var index = 0; index < lines.length; index++) {
    var row = lines[index];
    var text = typeof row === 'string' ? row : row && row.text;
    if (!text) continue;
    var level = typeof row === 'string' ? 0 : Number(row.level) || 0;
    list.appendChild(el('li', {
      class: 'ui-dialog__line' + (level > 0 ? ' ui-dialog__line--child' : ''),
      text: String(text)
    }));
  }
  if (list.childNodes.length) body.appendChild(list);
}

function present(dialog, body, actions, focusTarget, onClose) {
  body.appendChild(actions);
  dialog.appendChild(body);
  dialog.addEventListener('close', function () { if (dialog.parentNode) dialog.parentNode.removeChild(dialog); });
  dialog.addEventListener('cancel', function (event) { event.preventDefault(); onClose(); });
  document.body.appendChild(dialog);
  if (typeof dialog.showModal === 'function') dialog.showModal(); else dialog.setAttribute('open', '');
  setTimeout(function () { try { focusTarget.focus(); } catch (error) {} }, 0);
}

function closeWith(dialog, opener, resolve, value) {
  if (dialog.open && typeof dialog.close === 'function') dialog.close();
  else if (dialog.parentNode) dialog.parentNode.removeChild(dialog);
  resolve(value);
  // WCAG 2.4.3: focus returns to whatever opened the dialog.
  if (opener && typeof opener.focus === 'function') setTimeout(function () { try { opener.focus(); } catch (error) {} }, 0);
}

export function confirmDialog(options) {
  options = options || {};
  return new Promise(function (resolve) {
    var opener = document.activeElement;
    var shell = buildShell(options);
    appendLines(shell.body, options.lines);
    var actions = el('div', { class: 'ui-dialog__actions' });
    var cancel = el('button', {
      type: 'button', class: 'btn btn--ghost',
      text: options.cancelLabel || t('common.cancel')
    });
    var confirm = el('button', {
      type: 'button',
      class: 'btn ' + (options.danger ? 'btn--danger' : 'btn--primary'),
      text: options.confirmLabel || t('common.confirm')
    });
    cancel.addEventListener('click', function () { closeWith(shell.dialog, opener, resolve, false); });
    confirm.addEventListener('click', function () { closeWith(shell.dialog, opener, resolve, true); });
    actions.appendChild(cancel);
    actions.appendChild(confirm);
    // A destructive action focuses Cancel, so Enter never confirms it by reflex.
    present(shell.dialog, shell.body, actions, options.danger ? cancel : confirm,
      function () { closeWith(shell.dialog, opener, resolve, false); });
  });
}

export function promptDialog(options) {
  options = options || {};
  return new Promise(function (resolve) {
    var opener = document.activeElement;
    var shell = buildShell(options);
    var field = el(options.multiline === false ? 'input' : 'textarea', {
      class: 'input ui-dialog__field',
      value: options.value || '',
      attrs: options.multiline === false
        ? { type: 'text', 'aria-label': options.fieldLabel || options.title }
        : { rows: '3', 'aria-label': options.fieldLabel || options.title }
    });
    shell.body.appendChild(field);
    var actions = el('div', { class: 'ui-dialog__actions' });
    var cancel = el('button', {
      type: 'button', class: 'btn btn--ghost',
      text: options.cancelLabel || t('common.cancel')
    });
    var confirm = el('button', {
      type: 'button', class: 'btn btn--primary',
      text: options.confirmLabel || t('common.confirm')
    });
    // null is a dismissal, '' is an empty answer: callers rely on the difference.
    cancel.addEventListener('click', function () { closeWith(shell.dialog, opener, resolve, null); });
    confirm.addEventListener('click', function () { closeWith(shell.dialog, opener, resolve, field.value); });
    actions.appendChild(cancel);
    actions.appendChild(confirm);
    present(shell.dialog, shell.body, actions, field,
      function () { closeWith(shell.dialog, opener, resolve, null); });
  });
}
