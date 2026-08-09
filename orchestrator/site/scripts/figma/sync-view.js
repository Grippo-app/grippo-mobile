import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { figmaEnumText } from './enum-labels.js';

var el = dom.el;
function t(key, params) { return i18n && typeof i18n.t === 'function' ? i18n.t(key, params) : key; }
function closeDialog(dialog) {
  if (typeof dialog.close === 'function') dialog.close(); else dialog.removeAttribute('open');
  var trigger = dialog._figmaTrigger; dialog._figmaTrigger = null;
  if (trigger && typeof trigger.focus === 'function') setTimeout(function () { trigger.focus(); }, 0);
}
function showDialog(dialog) {
  dialog._figmaTrigger = document.activeElement;
  if (typeof dialog.showModal === 'function') dialog.showModal(); else dialog.setAttribute('open', '');
}

export function createSyncView(handlers) {
  var dialog = el('dialog', { class: 'figma-integration-dialog', attrs: { 'aria-labelledby': 'figma-sync-title' } });
  dialog.addEventListener('close', function () {
    var trigger = dialog._figmaTrigger; dialog._figmaTrigger = null;
    if (trigger && typeof trigger.focus === 'function') setTimeout(function () { trigger.focus(); }, 0);
  });
  dialog.addEventListener('cancel', function (event) { event.preventDefault(); closeDialog(dialog); });
  dialog.addEventListener('keydown', function (event) {
    if (event.key !== 'Escape') return;
    event.preventDefault(); event.stopPropagation(); closeDialog(dialog);
  });
  var title = el('h3', { id: 'figma-sync-title', text: t('figma.syncPlan.title') });
  var summary = el('p');
  var groups = el('ul', { class: 'figma-integration-plan-groups' });
  var warnings = el('div', { class: 'figma-integration-plan-warning', hidden: true });
  var cancel = el('button', { type: 'button', class: 'btn btn--ghost', text: t('common.cancel') });
  var confirm = el('button', { type: 'button', class: 'btn btn--primary', text: t('figma.action.startSync') });
  dialog.appendChild(el('div', { class: 'figma-integration-dialog-head' }, [title, cancel]));
  dialog.appendChild(summary); dialog.appendChild(groups); dialog.appendChild(warnings);
  dialog.appendChild(el('div', { class: 'figma-integration-dialog-actions' }, [cancel.cloneNode(true), confirm]));
  var footerCancel = dialog.querySelector('.figma-integration-dialog-actions .btn--ghost');
  [cancel, footerCancel].forEach(function (node) { node.addEventListener('click', function () { closeDialog(dialog); }); });
  confirm.addEventListener('click', function () { closeDialog(dialog); handlers.confirm(); });
  var progress = el('section', { class: 'figma-integration-progress', hidden: true, attrs: { 'aria-live': 'polite', 'aria-atomic': 'true' } });
  var progressTitle = el('strong');
  var progressBar = el('progress', { max: 100, value: 0 });
  var progressText = el('span');
  var progressGroups = el('ul');
  var cancelJob = el('button', { type: 'button', class: 'btn btn--ghost btn--small', text: t('figma.action.cancelSync') });
  cancelJob.addEventListener('click', handlers.cancel);
  progress.appendChild(progressTitle); progress.appendChild(progressBar); progress.appendChild(progressText); progress.appendChild(progressGroups); progress.appendChild(cancelJob);

  var currentPlan = null;
  function open(plan) {
    currentPlan = plan;
    summary.textContent = figmaEnumText('syncPlan', plan.mode, { reads: plan.estimatedReads });
    while (groups.firstChild) groups.removeChild(groups.firstChild);
    plan.groups.forEach(function (group) { groups.appendChild(el('li', { text: figmaEnumText('group', group) })); });
    warnings.hidden = !plan.warnings.length;
    warnings.textContent = plan.warnings.map(function (warning) { return figmaEnumText('warning', warning.code); }).join(' ');
    showDialog(dialog); setTimeout(function () { confirm.focus(); }, 0);
  }
  function update(job) {
    progress.hidden = !job;
    if (!job) return;
    progressTitle.textContent = figmaEnumText('syncState', job.state || 'queued');
    progressBar.value = job.progress || 0;
    progressText.textContent = t('figma.progress', { n: job.progress || 0 });
    while (progressGroups.firstChild) progressGroups.removeChild(progressGroups.firstChild);
    (job.groups || []).forEach(function (group) {
      progressGroups.appendChild(el('li', {
        text: figmaEnumText('group', group.group) + ' — ' + figmaEnumText('groupState', group.status)
      }));
    });
    cancelJob.disabled = ['completed', 'failed', 'cancelled'].indexOf(job.state) >= 0;
  }
  return { dialog: dialog, progress: progress, open: open, update: update, plan: function () { return currentPlan; } };
}
