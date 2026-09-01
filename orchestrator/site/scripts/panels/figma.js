import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { store } from '../store.js';
import { clipboard } from '../clipboard.js';
import { confirmDialog } from '../ui-dialog.js';
import { tasksApi } from '../data/tasks-api.js';
import { runErrorMessage } from '../run-errors.js';
import { fileCandidateDisplayName, parseFileKey } from '../figma-actions.js';
import { createFigmaFeatureView, createIntegrationView } from '../figma/integration-view.js';
import { createSyncView } from '../figma/sync-view.js';
import { createHistoryPagination, createHistoryView } from '../figma/history-view.js';
import { figmaEnumText } from '../figma/enum-labels.js';

// Integrations -> Figma. The browser sends typed operations and opaque ids;
// executable prompts, stage paths and artifact lists remain server-owned.

var el = dom.el;
var sectionEl = null;
var featureView = null;
var integrationView = null;
var syncView = null;
var historyView = null;
var historyPagination = null;
var accountDialog = null;
var fileDialog = null;
var clearDialog = null;
var fileInput = null;
var fileStatus = null;
var fileSave = null;
var accountStatus = null;
var fileStatusError = null;
var busy = false;
var unsubscribe = null;
var lastGeneration = null;
var generationObserved = false;
var fileEditorInitialized = false;
var fileVerificationCandidateId = null;
var fileVerificationKey = null;
var fileVerificationInput = null;
var blockedIntentKey = null;
var intentTestAttempted = false;
var INTENT_AUTO_TEST_REASONS = {
  'account-stale': 1,
  'access-unverified': 1,
  'auth-required': 1,
  'connector-unavailable': 1
};

function t(key, params) { return i18n && typeof i18n.t === 'function' ? i18n.t(key, params) : key; }
function model() { return store.get().figmaIntegration || null; }
function featureModel() { return store.get().figmaFeature || { state: 'invalid', canEnable: false, configRevision: null }; }
function idempotencyKey(prefix) {
  var suffix = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() :
    Date.now().toString(36) + '-' + Math.random().toString(36).slice(2);
  return prefix + ':' + suffix;
}
function closeDialog(dialog) {
  if (!dialog) return;
  if (typeof dialog.close === 'function') dialog.close(); else dialog.removeAttribute('open');
  var trigger = dialog._figmaTrigger;
  dialog._figmaTrigger = null;
  if (trigger && typeof trigger.focus === 'function') setTimeout(function () { trigger.focus(); }, 0);
}
function showDialog(dialog) {
  if (!dialog) return;
  dialog._figmaTrigger = document.activeElement;
  if (typeof dialog.showModal === 'function') dialog.showModal(); else dialog.setAttribute('open', '');
}
function restoreDialogFocus(dialog) {
  var trigger = dialog && dialog._figmaTrigger;
  if (!trigger) return;
  dialog._figmaTrigger = null;
  if (typeof trigger.focus === 'function') setTimeout(function () { trigger.focus(); }, 0);
}
function closeOnEscape(dialog) {
  dialog.addEventListener('keydown', function (event) {
    if (event.key !== 'Escape') return;
    event.preventDefault(); event.stopPropagation(); closeDialog(dialog);
  });
}
export function figmaActionError(error) {
  var key = 'figma.requestError.' + String(error && error.kind || error && error.code || 'unknown');
  var translated = t(key);
  if (translated !== key) return translated;
  var reasonKey = 'figma.error.' + String(error && error.kind || error && error.code || 'unknown') + '.body';
  var reason = t(reasonKey);
  if (reason !== reasonKey) return reason;
  // Figma pulls use the shared session runtime. Preserve its typed start
  // failures instead of collapsing e.g. a settling writer or unsafe transcript
  // into the opaque "unknown Figma error" fallback.
  var sessionReason = runErrorMessage(error);
  return sessionReason === t('run.error.unknown')
    ? t('figma.requestError.unknown') : sessionReason;
}
// A dialog action fails with a typed reason, and a toast over a modal is easy
// to miss — so the dialog states it too. Errors are assertive; clearing the
// line restores its plain-hint role.
function setDialogError(node, message) {
  if (!node) return;
  node.textContent = message || '';
  node.className = 'field-help' + (message ? ' figma-integration-field-error' : '');
  if (message) { node.setAttribute('role', 'alert'); node.setAttribute('aria-live', 'assertive'); }
  else { node.removeAttribute('role'); node.setAttribute('aria-live', 'polite'); }
}
// render() is the only writer of fileStatus, so a rejection is held here and
// outranks the candidate line until the user edits, retries, or closes.
function showFileError(message) {
  fileStatusError = message || null;
  render();
}
function withBusy(action) {
  // A second click inside a dialog used to be a silent no-op: every call site
  // swallows this rejection, so the reason is announced here instead.
  if (busy) { clipboard.toastError(t('figma.requestError.busy')); return Promise.reject({ kind: 'busy' }); }
  busy = true; render();
  return Promise.resolve().then(action).then(function (result) {
    busy = false; render(); return result;
  }, function (error) {
    busy = false; render(); clipboard.toastError(figmaActionError(error)); throw error;
  });
}
function currentFileKey() {
  var current = model(), url = current && current.projectFile && current.projectFile.url;
  return parseFileKey(url || '') || null;
}
function runTest() {
  return withBusy(function () { return tasksApi.figmaTest(currentFileKey(), false); }).catch(function () {});
}
export function focusFigmaEnableResult(feature, integration, views) {
  var available = !!(feature && feature.state === 'enabled' && integration);
  if (available && views && views.integration &&
      typeof views.integration.focusPrimary === 'function') {
    views.integration.focusPrimary();
    return 'integration';
  }
  if (views && views.feature && typeof views.feature.focusAction === 'function') {
    views.feature.focusAction();
  }
  return 'feature';
}
function enableFigma() {
  var feature = featureModel();
  if (!feature.canEnable || !feature.configRevision) return;
  return withBusy(function () {
    return tasksApi.figmaEnable(feature.configRevision).then(function () {
      return store.load().then(function (next) {
        focusFigmaEnableResult(featureModel(), model(), {
          feature: featureView, integration: integrationView
        });
        return next;
      });
    });
  }).catch(function () {});
}
function openAccount() { showDialog(accountDialog); }
function openFile() {
  var current = model();
  if (!fileEditorInitialized && fileInput) {
    fileInput.value = current && current.projectFile && current.projectFile.url || '';
    fileEditorInitialized = true;
  }
  showDialog(fileDialog);
  setTimeout(function () { if (fileInput) fileInput.focus(); }, 0);
}
function recover(action) {
  if (action === 'connect') {
    withBusy(function () { return tasksApi.figmaAddLocal(); }).then(runTest, function () {});
  } else if (action === 'reconnect' || action === 'changeAccount') openAccount();
  else if (action === 'selectFile' || action === 'selectAnotherFile') openFile();
  else runTest();
}
function verifyFile() {
  var current = model();
  var value = fileInput && fileInput.value.trim();
  var key = parseFileKey(value || '');
  if (!value || !key) {
    var invalid = t('figma.fileEditor.invalid');
    showFileError(invalid); return;
  }
  fileStatusError = null;
  withBusy(function () { return tasksApi.figmaFileVerify(value, current.configRevision); }).then(function (response) {
    fileVerificationCandidateId = response && response.candidate && response.candidate.id || null;
    fileVerificationKey = fileVerificationCandidateId ? key : null;
    fileVerificationInput = fileVerificationCandidateId ? value : null;
    render();
  }, function (error) { if (error && error.kind === 'busy') return; showFileError(figmaActionError(error)); });
}
function saveFile() {
  var current = model(), candidate = current && current.fileCandidate;
  if (!candidate || candidate.state !== 'verified' || candidate.id !== fileVerificationCandidateId ||
      parseFileKey(fileInput && fileInput.value || '') !== fileVerificationKey ||
      (fileInput && fileInput.value.trim()) !== fileVerificationInput) return;
  // An expired or superseded candidate is refused with a typed 409; the dialog
  // stays open and says why instead of consuming the rejection silently.
  fileStatusError = null;
  withBusy(function () { return tasksApi.figmaFileSave(candidate.id, current.configRevision); }).then(function () {
    fileEditorInitialized = false; fileVerificationCandidateId = null; fileVerificationKey = null; fileVerificationInput = null;
    closeDialog(fileDialog); clipboard.toast(t('figma.fileEditor.saved'));
  }, function (error) { if (error && error.kind === 'busy') return; showFileError(figmaActionError(error)); });
}
function openTerminal() {
  // The account dialog stays open behind this one, so declining has to leave it
  // untouched: its status line is cleared only once the flow is actually armed,
  // and withBusy is never entered on the refusal path.
  confirmDialog({
    title: t('figma.account.title'), message: t('figma.account.confirm'),
    confirmLabel: t('figma.action.openTerminal')
  }).then(function (accepted) {
    if (!accepted) return;
    setDialogError(accountStatus, null);
    withBusy(function () { return tasksApi.figmaOpenTerminal(); }).then(function () {
      closeDialog(accountDialog); clipboard.toast(t('figma.account.opened'));
    }, function (error) { if (error && error.kind === 'busy') return; setDialogError(accountStatus, figmaActionError(error)); });
  });
}
function startPlan(plan) {
  var acknowledgements = (plan.warnings || []).map(function (warning) { return warning.code; });
  return withBusy(function () { return tasksApi.figmaSyncStart(plan.id, acknowledgements); }).catch(function () {});
}
function runSync(scope) {
  if (busy) return;
  withBusy(function () { return tasksApi.figmaSyncPlan(scope); }).then(function (response) {
    var plan = response && response.plan;
    if (!plan) return;
    if (plan.mode === 'targeted' && (!plan.warnings || !plan.warnings.length)) { startPlan(plan); return; }
    syncView.open(plan);
  }, function () {});
}
// The router never unmounts a panel, so this subscription outlives the visit.
// Panel-owned feedback has to check the route itself.
function onFigmaRoute() {
  var raw = (location.hash || '').replace(/^#/, '');
  return raw === 'figma' || raw.indexOf('figma?') === 0;
}
function syncIntent() {
  var raw = (location.hash || '').replace(/^#/, '');
  if (raw.indexOf('figma?') !== 0) return '';
  var scope = new URLSearchParams(raw.slice('figma?'.length)).get('sync') || '';
  return ['tokens', 'components', 'drift'].indexOf(scope) >= 0 ? scope : '';
}
function consumeSyncIntent() {
  var scope = syncIntent();
  if (!scope) { blockedIntentKey = null; intentTestAttempted = false; return; }
  if (busy) return;
  if (featureModel().state !== 'enabled') {
    blockedIntentKey = scope + '|figma-feature-unavailable';
    if (featureView && featureView.focusAction) featureView.focusAction();
    return;
  }
  var current = model();
  var allowed = current && current.actions && (scope === 'drift' ? current.actions.canCompare : current.actions.canSync);
  if (!allowed) {
    var gate = current && (scope === 'drift' ? current.compareGate : current.syncGate) || {};
    var reason = String(gate.reasonCode || current && current.reasonCode || 'unknown');
    var blockedKey = scope + '|' + reason;
    if (blockedIntentKey !== blockedKey && integrationView && integrationView.focusSyncActions) {
      blockedIntentKey = blockedKey;
      integrationView.focusSyncActions(scope);
    }
    if (scope !== 'drift' && !intentTestAttempted && current && current.actions && current.actions.canTest && INTENT_AUTO_TEST_REASONS[reason]) {
      intentTestAttempted = true;
      runTest();
    }
    return;
  }
  blockedIntentKey = null;
  intentTestAttempted = false;
  try { history.replaceState(null, '', '#figma'); }
  catch (error) { location.hash = 'figma'; return; }
  runSync(scope);
}
function confirmSync() { var plan = syncView.plan(); if (plan) startPlan(plan); }
function cancelSync() {
  var current = model(), job = current && current.sync && current.sync.active;
  if (!job) return;
  // The id and revision stay the pair the question was asked about: re-reading
  // them after the answer would cancel whatever is running by then. The confirm
  // button keeps the generic label — "Cancel" beside "Cancel sync" would read as
  // two ways out of the same dialog. Declining never reaches withBusy.
  confirmDialog({ title: t('figma.action.cancelSync'), message: t('figma.cancelConfirm'), danger: true })
  .then(function (accepted) {
    if (!accepted) return;
    withBusy(function () { return tasksApi.figmaSyncCancel(job.id, job.revision); }).catch(function () {});
  });
}
function openHistory() {
  historyPagination.open();
  historyView.open();
}

function openClearIntegration() {
  if (!clearDialog) return;
  if (typeof clearDialog._prepare === 'function') clearDialog._prepare();
  clearDialog._figmaResetKey = idempotencyKey('figma-reset');
  showDialog(clearDialog);
}

function figmaClearError(error) {
  var code = String(error && error.kind || error && error.code || 'unknown');
  var key = 'figma.clear.error.' + code;
  var translated = t(key);
  return translated === key ? figmaActionError(error) : translated;
}

function buildClearDialog() {
  var dialog = el('dialog', { class: 'figma-integration-dialog', attrs: { 'aria-labelledby': 'figma-clear-title' } });
  var resetting = false;
  var status = el('p', { class: 'figma-integration-field-error', hidden: true,
    attrs: { role: 'status', 'aria-live': 'polite', 'aria-atomic': 'true' } });
  var cancel = el('button', { type: 'button', class: 'btn btn--ghost', text: t('common.cancel') });
  var confirm = el('button', { type: 'button', class: 'btn figma-clear-confirm', text: t('figma.clear.confirm') });
  function setResetting(next) {
    resetting = next; busy = next;
    if (next) dialog.setAttribute('aria-busy', 'true');
    else dialog.removeAttribute('aria-busy');
    cancel.disabled = next; confirm.disabled = next;
    render();
  }
  dialog._prepare = function () {
    status.hidden = true; status.className = 'figma-integration-field-error';
    status.setAttribute('role', 'status'); status.setAttribute('aria-live', 'polite');
    status.textContent = '';
    confirm.textContent = t('figma.clear.confirm');
    dialog.removeAttribute('aria-busy');
  };
  cancel.addEventListener('click', function () { if (!resetting) closeDialog(dialog); });
  dialog.addEventListener('close', function () {
    dialog._figmaResetKey = null;
    restoreDialogFocus(dialog);
  });
  dialog.addEventListener('cancel', function (event) { event.preventDefault(); if (!resetting) closeDialog(dialog); });
  dialog.addEventListener('keydown', function (event) {
    if (event.key !== 'Escape') return;
    event.preventDefault(); event.stopPropagation();
    if (!resetting) closeDialog(dialog);
  });
  confirm.addEventListener('click', function () {
    var current = model();
    if (!current || resetting) return;
    setResetting(true);
    confirm.textContent = t('figma.clear.progressButton');
    status.hidden = false; status.className = 'figma-integration-field-error integration-clear-progress';
    status.setAttribute('role', 'status'); status.setAttribute('aria-live', 'polite');
    status.textContent = t('figma.clear.progress');
    tasksApi.figmaIntegrationReset(
      current.configRevision,
      current.context && current.context.generationId || null,
      dialog._figmaResetKey || (dialog._figmaResetKey = idempotencyKey('figma-reset'))
    ).then(function () {
      setResetting(false); closeDialog(dialog);
    }, function (error) {
      setResetting(false);
      confirm.textContent = t('figma.clear.confirm');
      status.hidden = false;
      status.className = 'figma-integration-field-error';
      status.setAttribute('role', 'alert'); status.setAttribute('aria-live', 'assertive');
      status.textContent = figmaClearError(error);
      confirm.focus();
    });
  });
  dialog.appendChild(el('div', { class: 'figma-integration-dialog-head' }, [
    el('h3', { id: 'figma-clear-title', text: t('figma.clear.title') }), cancel
  ]));
  dialog.appendChild(el('p', { class: 'figma-clear-copy', text: t('figma.clear.body') }));
  dialog.appendChild(status);
  dialog.appendChild(el('div', { class: 'figma-integration-dialog-actions' }, [confirm]));
  return dialog;
}

function buildAccountDialog() {
  var dialog = el('dialog', { class: 'figma-integration-dialog', attrs: { 'aria-labelledby': 'figma-account-title' } });
  dialog.addEventListener('close', function () { setDialogError(accountStatus, null); restoreDialogFocus(dialog); });
  dialog.addEventListener('cancel', function (event) { event.preventDefault(); closeDialog(dialog); });
  closeOnEscape(dialog);
  var cancel = el('button', { type: 'button', class: 'btn btn--ghost', text: t('common.cancel') });
  cancel.addEventListener('click', function () { closeDialog(dialog); });
  var terminal = el('button', { type: 'button', class: 'btn btn--terminal', text: t('figma.action.openTerminal') });
  terminal.addEventListener('click', openTerminal);
  // The sign-in flow refuses for typed reasons (unsupported platform, an active
  // session, a held finalization). This line is where they are reported.
  accountStatus = el('p', { id: 'figma-account-status', class: 'field-help', attrs: { 'aria-live': 'polite' } });
  dialog.appendChild(el('div', { class: 'figma-integration-dialog-head' }, [el('h3', { id: 'figma-account-title', text: t('figma.account.title') }), cancel]));
  dialog.appendChild(el('p', { text: t('figma.account.body') }));
  dialog.appendChild(el('p', { class: 'field-help', text: t('figma.account.artifactsSafe') }));
  dialog.appendChild(accountStatus);
  dialog.appendChild(el('div', { class: 'figma-integration-dialog-actions' }, [terminal]));
  return dialog;
}
function buildFileDialog() {
  var dialog = el('dialog', { class: 'figma-integration-dialog', attrs: { 'aria-labelledby': 'figma-file-title' } });
  dialog.addEventListener('close', function () {
    // Cancel has to mean cancel: drop the typed URL and the verification it
    // produced, so reopening cannot show "Access verified" with Save armed.
    fileEditorInitialized = false;
    fileVerificationCandidateId = null; fileVerificationKey = null; fileVerificationInput = null;
    fileStatusError = null;
    render();
    restoreDialogFocus(dialog);
  });
  dialog.addEventListener('cancel', function (event) { event.preventDefault(); closeDialog(dialog); });
  closeOnEscape(dialog);
  var cancel = el('button', { type: 'button', class: 'btn btn--ghost', text: t('common.cancel') });
  cancel.addEventListener('click', function () { closeDialog(dialog); });
  fileInput = el('input', { id: 'figma-file-input', type: 'text', class: 'input', maxLength: 500, attrs: { spellcheck: 'false', autocomplete: 'off', placeholder: t('figma.fileEditor.placeholder'), 'aria-describedby': 'figma-file-status figma-file-warning' } });
  fileInput.addEventListener('input', function () {
    if (parseFileKey(fileInput.value) !== fileVerificationKey || fileInput.value.trim() !== fileVerificationInput) {
      fileVerificationCandidateId = null; fileVerificationInput = null;
    }
    // Editing the URL is a retry: the held rejection no longer describes it.
    fileStatusError = null;
    render();
  });
  fileStatus = el('p', { id: 'figma-file-status', class: 'field-help', attrs: { 'aria-live': 'polite' } });
  var verify = el('button', { type: 'button', class: 'btn', text: t('figma.action.verifyFile') });
  verify.addEventListener('click', verifyFile);
  fileSave = el('button', { type: 'button', class: 'btn btn--primary', text: t('common.save'), disabled: true });
  fileSave.addEventListener('click', saveFile);
  dialog.appendChild(el('div', { class: 'figma-integration-dialog-head' }, [el('h3', { id: 'figma-file-title', text: t('figma.fileEditor.title') }), cancel]));
  dialog.appendChild(el('p', { text: t('figma.fileEditor.body') }));
  dialog.appendChild(el('label', { text: t('figma.fileEditor.label'), attrs: { for: 'figma-file-input' } })); dialog.appendChild(fileInput); dialog.appendChild(fileStatus);
  dialog.appendChild(el('p', { id: 'figma-file-warning', class: 'field-help', text: t('figma.fileEditor.staleWarning') }));
  dialog.appendChild(el('div', { class: 'figma-integration-dialog-actions' }, [verify, fileSave]));
  return dialog;
}
function build() {
  if (!sectionEl) return;
  while (sectionEl.firstChild) sectionEl.removeChild(sectionEl.firstChild);
  featureView = createFigmaFeatureView({ enable: enableFigma });
  integrationView = createIntegrationView({ test: runTest, sync: runSync, recover: recover, account: openAccount,
    file: openFile, history: openHistory, clear: openClearIntegration });
  syncView = createSyncView({ confirm: confirmSync, cancel: cancelSync });
  historyView = createHistoryView({
    onLoadMore: function () { if (historyPagination) historyPagination.loadMore(); },
    onRetry: function () { if (historyPagination) historyPagination.retry(); }
  });
  historyPagination = createHistoryPagination(function (cursor) {
    return tasksApi.figmaSyncHistory(cursor, 20);
  }, function (snapshot) {
    if (!historyView) return;
    historyView.render(Object.assign({}, snapshot, {
      errorText: snapshot.error ? figmaActionError(snapshot.error) : null
    }));
  });
  accountDialog = buildAccountDialog(); fileDialog = buildFileDialog(); clearDialog = buildClearDialog();
  integrationView.el.appendChild(syncView.progress);
  sectionEl.appendChild(featureView.el);
  sectionEl.appendChild(integrationView.el);
  document.body.appendChild(syncView.dialog); document.body.appendChild(historyView.dialog); document.body.appendChild(accountDialog);
  document.body.appendChild(fileDialog); document.body.appendChild(clearDialog);
  render();
}
function render() {
  if (!featureView || !integrationView) return;
  var feature = featureModel();
  var current = model();
  var available = feature.state === 'enabled' && !!current;
  featureView.el.hidden = available;
  integrationView.el.hidden = !available;
  featureView.update(available ? feature : (feature.state === 'enabled'
    ? { state: 'invalid', canEnable: false, configRevision: feature.configRevision }
    : feature), busy);
  if (!available) {
    syncView.update(null);
    return;
  }
  integrationView.update(current, busy, syncIntent());
  syncView.update(current.sync && current.sync.active || null);
  var candidate = current.fileCandidate && current.fileCandidate.id === fileVerificationCandidateId ? current.fileCandidate : null;
  if (fileStatus) {
    if (fileStatusError) setDialogError(fileStatus, fileStatusError);
    else {
      fileStatus.removeAttribute('role');
      fileStatus.setAttribute('aria-live', 'polite');
      fileStatus.className = 'field-help' + (candidate && candidate.state === 'failed' ? ' figma-integration-field-error' : '');
      fileStatus.textContent = candidate
        ? figmaEnumText('fileCandidate', candidate.state, { name: fileCandidateDisplayName(candidate) })
        : '';
    }
  }
  // The server expires a verification candidate after ~10 minutes and answers a
  // late save with file-candidate-expired, so an armed Save on a dead candidate
  // promises something the dialog cannot deliver.
  var candidateFresh = !!(candidate && candidate.expiresAt && Date.parse(candidate.expiresAt) > Date.now());
  if (fileSave) fileSave.disabled = busy || !candidate || candidate.state !== 'verified' || !candidateFresh ||
    parseFileKey(fileInput && fileInput.value || '') !== fileVerificationKey || (fileInput && fileInput.value.trim()) !== fileVerificationInput;
}
// Confirming a sync belongs to a state change, not to a repaint: render() also
// runs on every busy flip. The first committed generation is exactly when the
// user needs the confirmation, so the baseline is "a generation was observed",
// not "a generation existed". The pointer is tracked off-route too, so
// returning to the panel later cannot replay an old completion.
function observeSyncCompletion() {
  var current = model();
  if (!current) return;
  var generation = current.sync && current.sync.committedGenerationId || null;
  if (!generationObserved) { generationObserved = true; lastGeneration = generation; return; }
  if (generation && generation !== lastGeneration && onFigmaRoute()) clipboard.toast(t('figma.syncComplete'));
  lastGeneration = generation || lastGeneration;
}
function handleStoreChange() {
  render();
  observeSyncCompletion();
  consumeSyncIntent();
}
function teardownDialogs() {
  if (historyPagination) historyPagination.dispose();
  historyPagination = null;
  [syncView && syncView.dialog, historyView && historyView.dialog, accountDialog, fileDialog, clearDialog].forEach(function (dialog) {
    if (dialog && dialog.parentNode) dialog.parentNode.removeChild(dialog);
  });
}

export const figma = {
  mount: function (root) {
    sectionEl = root;
    blockedIntentKey = null;
    intentTestAttempted = false;
    fileEditorInitialized = false;
    fileVerificationCandidateId = null;
    fileVerificationKey = null;
    fileVerificationInput = null;
    fileStatusError = null;
    teardownDialogs();
    build();
    // Seed the completion baseline from the state present at mount, so the
    // first store change cannot report an already-committed generation as new.
    observeSyncCompletion();
    if (unsubscribe) unsubscribe();
    unsubscribe = store.on('change', handleStoreChange);
    consumeSyncIntent();
  },
  refresh: function () {
    blockedIntentKey = null;
    intentTestAttempted = false;
    teardownDialogs();
    fileEditorInitialized = false;
    fileVerificationCandidateId = null;
    fileVerificationKey = null;
    fileVerificationInput = null;
    fileStatusError = null;
    build();
    consumeSyncIntent();
  }
};
