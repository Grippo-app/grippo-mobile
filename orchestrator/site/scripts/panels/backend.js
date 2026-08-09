import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { store } from '../store.js';
import { clipboard } from '../clipboard.js';
import { confirmDialog } from '../ui-dialog.js';
import { requestJson, errorCode } from '../data/request-json.js';
import { backendAuthText, backendStateText } from '../backend-labels.js';
import { siteEvents } from '../event-stream.js';

// Integrations -> Backend. All executable actions are typed server-owned API
// calls. The browser never sends a URL to Test/Refresh and never constructs a
// prompt or command for the primary flow.

var el = dom.el;
var sectionEl = null;
var model = null;
var loading = false;
var loadPromise = null;
var loadError = null;
var pendingAction = null;
var eventSubscriptions = null;
var pollTimer = null;
var mounted = false;
var advancedOpen = false;

function t(key, params) { return i18n && typeof i18n.t === 'function' ? i18n.t(key, params) : key; }
function idempotencyKey(prefix) {
  var suffix = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() :
    Date.now().toString(36) + '-' + Math.random().toString(36).slice(2);
  return prefix + ':' + suffix;
}
function postJson(url, body) {
  var headers = { 'content-type': 'application/json' };
  var csrf = typeof window !== 'undefined' && window.__ORCHESTRATOR_CSRF__;
  if (csrf) headers['x-orchestrator-csrf'] = csrf;
  return requestJson(url, { method: 'POST', headers: headers, body: JSON.stringify(body), cache: 'no-store' });
}
function load() {
  if (loadPromise) return loadPromise;
  loading = true; loadError = null;
  render();
  loadPromise = requestJson('/api/backend/integration', { cache: 'no-store', timeoutMs: 10000 })
  .then(function (parsed) { model = parsed; }, function (error) { loadError = errorCode(error); })
  .finally(function () { loading = false; loadPromise = null; render(); });
  return loadPromise;
}
function activeEnvironment() {
  if (!model || !Array.isArray(model.environments)) return null;
  return model.environments.find(function (row) { return row.id === model.activeEnvironmentId; }) || null;
}
// KEEP IN SYNC with postmanUrlInfo in api-contract/scripts/resolve-source.mjs.
var POSTMAN_LONG_UID = /^[0-9]+-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
function isPostmanPageHost(host) {
  return host === 'postman.com' || host === 'www.postman.com' || host === 'postman.co' || /\.postman\.co$/.test(host);
}
function isPostmanSourceHost(host) { return host === 'api.getpostman.com' || isPostmanPageHost(host); }
export function sourceInputProfile(value, failureCode) {
  var host = '';
  try { host = new URL(String(value || '').trim()).hostname.toLowerCase(); } catch (e) {}
  if (isPostmanSourceHost(host)) return { sourceKind: 'postman', authenticationRequired: true, authKind: 'x-api-key' };
  return { sourceKind: 'openapi', authenticationRequired: failureCode === 'auth-missing' || failureCode === 'auth-rejected', authKind: 'bearer' };
}
export function sourceCredentialMustReset(prior, authentication, next) {
  if (!prior || !next) return false;
  var boundaryChanged = ['sourceKind', 'sourceUrl', 'authKind'].some(function (key) {
    return prior[key] !== next[key];
  });
  var stored = authentication && (authentication.state === 'configured' ||
    authentication.state === 'invalid' || authentication.dormant);
  return boundaryChanged && !!stored;
}
function resolvedPostmanCredentialCanCarry(prior, authentication, next) {
  // A candidate click is an explicit continuation of the authenticated
  // Postman discovery flow. Keep that verified PMAK only inside Postman's
  // source-host family; manual URL edits still use sourceCredentialMustReset.
  if (!prior || !next || !authentication ||
      authentication.state !== 'configured' || authentication.dormant) return false;
  return prior.sourceKind === 'postman' && next.sourceKind === 'postman' &&
    prior.authRef === prior.id && next.authRef === next.id &&
    prior.authKind === 'x-api-key' && next.authKind === 'x-api-key' &&
    sourceInputProfile(prior.sourceUrl).sourceKind === 'postman' &&
    sourceInputProfile(next.sourceUrl).sourceKind === 'postman';
}
function resolutionOffersAction(resolution, environment) {
  if (!resolution) return false;
  var candidates = Array.isArray(resolution.candidates) ? resolution.candidates : [];
  return resolution.reason === 'auth-required' || !!resolution.resolvedUrl || candidates.length > 0 ||
    (resolution.detectedKind === 'postman' && environment && environment.sourceKind !== 'postman');
}
export function previewFailureNeedsErrorBanner(preview, environment, environmentRevision) {
  return !(preview && preview.environmentRevision === environmentRevision &&
    resolutionOffersAction(preview.resolution, environment));
}
export function dialogProbeOutcome(job, environment) {
  var report = job && job.result || {};
  var resolution = report.resolution || null;
  var failure = job && job.error && job.error.code || report.error && report.error.code || null;
  var authEnabled = !!(environment && environment.authRef === environment.id);
  if (authEnabled && (['auth-missing', 'auth-invalid', 'auth-rejected'].indexOf(failure) >= 0 ||
      resolution && resolution.reason === 'auth-required')) {
    return { state: 'replace-credential', errorCode:
      ['auth-missing', 'auth-invalid', 'auth-rejected'].indexOf(failure) >= 0 ? failure : 'auth-rejected',
      resolution: resolution };
  }
  if (!authEnabled && (failure === 'auth-missing' || failure === 'auth-rejected' ||
      resolution && resolution.reason === 'auth-required')) {
    return { state: 'require-credential', errorCode: 'auth-missing', resolution: resolution };
  }
  if (resolutionOffersAction(resolution, environment)) {
    return { state: 'resolution', errorCode: failure, resolution: resolution };
  }
  if (failure) return { state: 'failed', errorCode: failure, resolution: resolution };
  return { state: 'success', errorCode: null, resolution: resolution };
}
export function normalizeSourceUrl(value) {
  var raw = String(value || '').trim();
  var parsed;
  try { parsed = new URL(raw); } catch (e) { return { value: raw, pmatRejected: false }; }
  var pmatRejected = false;
  parsed.searchParams.forEach(function (entry) { if (/^PMAT-/i.test(entry)) pmatRejected = true; });
  parsed.search = ''; parsed.hash = '';
  var host = parsed.hostname.toLowerCase();
  if (isPostmanPageHost(host)) {
    var parts = parsed.pathname.split('/').filter(Boolean);
    var collectionIndex = parts.indexOf('collection');
    var uid = collectionIndex >= 0 ? parts[collectionIndex + 1] : null;
    if (uid && POSTMAN_LONG_UID.test(uid)) return {
      value: 'https://api.getpostman.com/collections/' + uid,
      pmatRejected: pmatRejected
    };
  }
  return { value: parsed.toString(), pmatRejected: pmatRejected };
}
function isPostmanWebUrl(value) {
  try {
    var host = new URL(value).hostname.toLowerCase();
    return isPostmanPageHost(host);
  } catch (e) { return false; }
}
function shortUrl(value) {
  if (!value) return '—';
  try {
    var parsed = new URL(value), shown = parsed.origin + parsed.pathname;
    return shown.length > 72 ? shown.slice(0, 69) + '…' : shown;
  } catch (e) { return String(value).slice(0, 72); }
}
function shortHash(value) { return value ? String(value).replace(/^sha256:/, '').slice(0, 12) : '—'; }
function dateLabel(value) {
  if (!value) return t('backend.value.never');
  var date = new Date(value);
  return isNaN(date.getTime()) ? t('backend.value.unknown') : date.toLocaleString();
}
function lastPulledLabel(value) {
  var label = dateLabel(value);
  if (!value) return label;
  var instant = Date.parse(value);
  return Number.isFinite(instant) && Date.now() - instant > 14 * 24 * 60 * 60 * 1000
    ? t('backend.value.stale', { date: label }) : label;
}
function stateLabel(value) { return backendStateText(value); }
function authLabel(value) { return backendAuthText(value); }
function contractLabel() {
  if (!model) return t('backend.contract.unknown');
  if (model.snapshot && model.snapshot.invalid) return t('backend.contract.invalid');
  if (!model.preview || !model.preview.fresh) return model.snapshot && model.snapshot.present
    ? t('backend.contract.saved') : t('backend.contract.notSaved');
  var delta = model.preview.delta || {};
  return delta.added || delta.changed || delta.removed ? t('backend.contract.changed') : t('backend.contract.unchanged');
}
export function errorLabel(code) {
  var key = 'backend.error.' + String(code || 'unknown');
  var translated = t(key);
  return translated === key ? t('backend.error.unknown') : translated;
}
function button(labelKey, className, handler, disabled) {
  var node = el('button', { type: 'button', class: className || 'btn', text: t(labelKey), disabled: !!disabled });
  if (handler) node.addEventListener('click', handler);
  return node;
}
function submitButton(labelKey, className) { return el('button', { type: 'submit', class: className || 'btn', text: t(labelKey) }); }
function statusRow(labelKey, value, action) {
  var valueNode = typeof value === 'string' ? el('span', { class: 'backend-integration-value', text: value }) : value;
  return el('div', { class: 'backend-integration-row' }, [
    el('dt', { class: 'backend-integration-label', text: t(labelKey) }),
    el('dd', { class: 'backend-integration-content' }, [valueNode, action || null])
  ]);
}
function setBusy(action, actionKind) {
  if (!sectionEl) return;
  pendingAction = actionKind || 'updating';
  render();
  return Promise.resolve().then(action).then(function () { return load(); }, function (error) {
    clipboard.toast(errorLabel(error && error.code));
    return load();
  }).finally(function () { pendingAction = null; render(); });
}
function selectEnvironment(event) {
  var environmentId = event.target.value;
  setBusy(function () { return postJson('/api/backend/environment/select', {
    environmentId: environmentId, expectedStateRevision: model.selectionRevision
  }); });
}
function runTest() {
  setBusy(function () { return postJson('/api/backend/test', {
    environmentId: model.activeEnvironmentId,
    expectedEnvironmentRevision: model.environmentRevision,
    expectedAuthRevision: model.authentication.revision,
    idempotencyKey: idempotencyKey('probe')
  }).then(function (response) { if (response.job) pollJob(response.job.jobId); return response; }); }, 'test');
}
function runRefresh() {
  var acknowledgements = [];
  var warnings = model.preview && model.preview.warnings || [];
  function submitRefresh() {
    setBusy(function () { return postJson('/api/backend/refresh', {
      previewId: model.preview.previewId,
      expectedSnapshotHash: model.snapshot.hash || null,
      acknowledgements: acknowledgements,
      idempotencyKey: idempotencyKey('refresh')
    }).then(function (response) { if (response.job) pollJob(response.job.jobId); return response; }); }, 'refresh');
  }
  if (!warnings.some(function (warning) { return warning.code === 'enrichment-unavailable'; })) { submitRefresh(); return; }
  // The acknowledgement is what makes the server accept a base-only refresh, so
  // it is still pushed only once the trade-off is accepted — that answer now
  // arrives a turn later, which moves the refresh into the continuation with it.
  // Declining posts nothing and marks nothing busy.
  var actionKey = model.snapshot && model.snapshot.present ? 'backend.action.refresh' : 'backend.action.import';
  confirmDialog({ title: t(actionKey), message: t('backend.confirm.baseOnly'), confirmLabel: t(actionKey) })
  .then(function (accepted) {
    if (!accepted) return;
    acknowledgements.push('refresh-base-without-enrichment');
    submitRefresh();
  });
}
function pollJob(jobId) {
  if (pollTimer) clearTimeout(pollTimer);
  requestJson('/api/backend/jobs/' + encodeURIComponent(jobId), { cache: 'no-store', timeoutMs: 10000 }).then(function (parsed) {
    if (!parsed || !parsed.job) return;
    if (['queued', 'running'].indexOf(parsed.job.state) >= 0) {
      pollTimer = setTimeout(function () { pollJob(jobId); }, 750);
    } else load();
  }, function () { pollTimer = setTimeout(function () { pollJob(jobId); }, 1500); });
}
function reloadIntegrationModel() {
  return requestJson('/api/backend/integration', { cache: 'no-store', timeoutMs: 10000 }).then(function (parsed) {
    model = parsed; loadError = null; render(); return parsed;
  });
}
function waitForJobResult(jobId, onProgress) {
  var attempts = 0;
  return new Promise(function (resolve, reject) {
    function poll() {
      requestJson('/api/backend/jobs/' + encodeURIComponent(jobId), { cache: 'no-store', timeoutMs: 10000 }).then(function (parsed) {
        var job = parsed && parsed.job;
        if (!job) { reject({ code: 'invalid-response' }); return; }
        if (onProgress) onProgress(job);
        if (['queued', 'running'].indexOf(job.state) < 0) { resolve(job); return; }
        attempts++;
        if (attempts >= 80) { reject({ code: 'sidecar-failed' }); return; }
        setTimeout(poll, 750);
      }, reject);
    }
    poll();
  });
}

function openDialog(titleKey, formBuilder) {
  var dialog = el('dialog', { class: 'backend-dialog' });
  var close = button('common.cancel', 'btn btn--ghost backend-dialog-dismiss', function () { dialog.close(); });
  var content = el('div', { class: 'backend-dialog-body' }, [
    el('div', { class: 'backend-dialog-head' }, [el('h3', { text: t(titleKey) }), close])
  ]);
  dialog.appendChild(content);
  formBuilder(dialog, content);
  dialog.addEventListener('close', function () { if (dialog.parentNode) dialog.parentNode.removeChild(dialog); });
  document.body.appendChild(dialog);
  if (typeof dialog.showModal === 'function') dialog.showModal(); else dialog.setAttribute('open', '');
  return dialog;
}
function field(labelKey, input, helpKey) {
  var id = 'backend-field-' + Math.random().toString(36).slice(2);
  input.id = id;
  return el('div', { class: 'field backend-dialog-field' }, [
    el('label', { attrs: { for: id }, text: t(labelKey) }), input,
    helpKey ? el('p', { class: 'field-help', text: t(helpKey) }) : null
  ]);
}
function sourceEditor(isNew, advancedMode) {
  var current = activeEnvironment();
  openDialog(isNew ? 'backend.source.addTitle' : 'backend.source.editTitle', function (dialog, content) {
    dialog.classList.add('backend-dialog--source');
    var used = (model.environments || []).map(function (row) { return row.id; });
    var idSelect = el('select', { class: 'input', required: true });
    if (isNew) idSelect.appendChild(el('option', { value: '', text: t('backend.source.environmentPlaceholder'), selected: true, disabled: true }));
    ['local', 'dev', 'stage', 'prod'].forEach(function (id) {
      if (!isNew && current && current.id !== id) return;
      var option = el('option', { value: id, text: t('backend.environment.' + id) });
      if (current && current.id === id) option.selected = true;
      if (isNew && used.indexOf(id) >= 0) option.disabled = true;
      idSelect.appendChild(option);
    });
    var label = el('input', { type: 'text', class: 'input', maxLength: 64, required: true, value: current && !isNew ? current.label : '' });
    var detectedSourceKind = current && !isNew ? current.sourceKind : 'openapi';
    var detectedAuthKind = current && !isNew ? current.authKind : 'bearer';
    var sourceUrl = el('input', { type: 'url', class: 'input', maxLength: 2048, required: true,
      value: current && !isNew ? current.sourceUrl : '',
      placeholder: 'https://api.example.com/docs' });
    var detection = el('p', { class: 'backend-source-detection', attrs: { 'aria-live': 'polite' } });
    var enrichment = el('input', { type: 'url', class: 'input', maxLength: 2048,
      value: current && !isNew && current.postmanEnrichmentUrl || '', placeholder: 'https://api.example.com/postman.json' });
    var auth = el('input', { type: 'checkbox', class: 'choice-input', checked: !!(current && !isNew && current.authRef) });
    var authWrap = el('label', { class: 'backend-checkbox' }, [auth, el('span', { text: t('backend.source.bearer') })]);
    var credential = el('input', { type: 'password', class: 'input', autocomplete: 'new-password', maxLength: 16384 });
    var credentialId = 'backend-field-' + Math.random().toString(36).slice(2);
    credential.id = credentialId;
    var credentialLabel = el('label', { attrs: { for: credentialId } });
    var credentialHelp = el('p', { class: 'field-help' });
    var credentialStored = el('p', { class: 'field-help', text: t('backend.source.credentialStored'), hidden: true });
    var credentialField = el('div', { class: 'field backend-dialog-field backend-inline-credential' }, [
      credentialLabel, credential, credentialHelp, credentialStored
    ]);
    var status = el('p', { class: 'backend-notice', hidden: true,
      attrs: { role: 'status', 'aria-live': 'polite', 'aria-atomic': 'true' } });
    // The environment is written before the source is tested, so a dialog kept
    // open by a probe verdict already holds a saved environment. Say so from the
    // action bar, which never scrolls away, and relabel every dismiss control:
    // "Cancel" would read as "discard" once the write has landed.
    var savedNotice = el('p', { class: 'backend-dialog-saved', hidden: true,
      attrs: { role: 'status', 'aria-live': 'polite' } });
    var resolutionActions = el('div', { class: 'backend-dialog-resolution', hidden: true });
    var formFields = [field('backend.source.environment', idSelect), field('backend.source.label', label),
      field('backend.source.url', sourceUrl, 'backend.source.urlHelp'), detection];
    if (advancedMode) formFields.push(field('backend.source.enrichment', enrichment, 'backend.source.enrichmentHelp'));
    var testButton = button('backend.action.test', 'btn btn--primary', null);
    formFields.push(authWrap, credentialField, status, resolutionActions);
    var actionBar = el('div', { class: 'backend-dialog-actions' }, [
      savedNotice, button('common.cancel', 'btn btn--ghost backend-dialog-dismiss', function () { dialog.close(); }),
      submitButton('common.save', 'btn'), testButton
    ]);
    var form = el('form', { class: 'backend-dialog-form backend-dialog-form--scroll' }, [
      el('div', { class: 'backend-dialog-scroll' }, formFields), actionBar
    ]);
    var automaticLabel = isNew;
    idSelect.addEventListener('change', function () {
      if (automaticLabel) label.value = idSelect.value ? t('backend.environment.' + idSelect.value) : '';
    });
    label.addEventListener('input', function () { automaticLabel = false; });
    var newCredentialSlotChecked = !isNew;
    // A credential entered in this dialog may be rebound once to an
    // automatically discovered URL in the same explicit user flow. Stored
    // credentials are never reused across that URL boundary implicitly.
    var transientCredential = '';
    dialog.addEventListener('close', function () {
      transientCredential = '';
      credential.value = '';
    });
    function clearResolutionActions() {
      while (resolutionActions.firstChild) resolutionActions.removeChild(resolutionActions.firstChild);
      resolutionActions.hidden = true;
    }
    function clearEditorFeedback() {
      status.hidden = true;
      status.textContent = '';
      clearResolutionActions();
    }
    function syncAdvanced() {
      enrichment.disabled = detectedSourceKind !== 'openapi';
      if (enrichment.disabled) enrichment.value = '';
    }
    function syncAuthentication() {
      var postman = detectedSourceKind === 'postman';
      authWrap.hidden = postman;
      credentialField.hidden = !auth.checked;
      credentialLabel.textContent = t('backend.source.credentialOptional.' + detectedAuthKind);
      credentialHelp.textContent = t(detectedAuthKind === 'x-api-key'
        ? 'backend.credential.helpApiKey' : 'backend.credential.help');
      var credentialBoundaryUnchanged = !isNew && current &&
        current.sourceKind === detectedSourceKind && current.sourceUrl === sourceUrl.value &&
        current.authKind === detectedAuthKind;
      credentialStored.hidden = !(credentialBoundaryUnchanged &&
        (model.authentication.state === 'configured' || model.authentication.dormant));
      detection.className = 'backend-source-detection backend-source-detection--' + detectedSourceKind;
      detection.textContent = t('backend.source.detected.' + detectedSourceKind);
    }
    auth.addEventListener('change', function () {
      transientCredential = '';
      clearEditorFeedback(); syncAuthentication();
    });
    function setEditorBusy(busy) {
      var controls = dialog.querySelectorAll('button, input, select');
      for (var index = 0; index < controls.length; index++) controls[index].disabled = busy;
    }
    function setEditorStatus(text, error) {
      status.hidden = false;
      status.className = 'backend-notice' + (error ? ' backend-notice--error' : '');
      status.setAttribute('role', error ? 'alert' : 'status');
      status.setAttribute('aria-live', error ? 'assertive' : 'polite');
      status.textContent = text;
      // The form scrolls; a verdict the user never sees reads as "nothing happened".
      if (typeof status.scrollIntoView === 'function') status.scrollIntoView({ block: 'nearest' });
    }
    var environmentSaved = false;
    function markEnvironmentSaved() {
      if (environmentSaved) return;
      environmentSaved = true;
      savedNotice.hidden = false;
      savedNotice.textContent = t('backend.source.savedNotice');
      var dismissals = dialog.querySelectorAll('.backend-dialog-dismiss');
      for (var index = 0; index < dismissals.length; index++) dismissals[index].textContent = t('backend.action.close');
    }
    var sourceInputChanged = isNew;
    var automaticallyClassifiedPostman = detectedSourceKind === 'postman';
    function applyAutomaticProfile(failureCode) {
      var inferred = sourceInputProfile(sourceUrl.value, failureCode);
      var profile = !failureCode && !sourceInputChanged && current && inferred.sourceKind !== 'postman' ? {
        sourceKind: current.sourceKind,
        authenticationRequired: current.sourceKind === 'postman' || !!current.authRef,
        authKind: current.sourceKind === 'postman' ? 'x-api-key' : current.authKind
      } : inferred;
      detectedSourceKind = profile.sourceKind;
      detectedAuthKind = profile.authKind;
      if (profile.sourceKind === 'postman' || failureCode) auth.checked = true;
      else if (automaticallyClassifiedPostman) auth.checked = false;
      automaticallyClassifiedPostman = profile.sourceKind === 'postman';
      syncAdvanced(); syncAuthentication();
      return profile;
    }
    function normalizeSourceField() {
      var normalized = normalizeSourceUrl(sourceUrl.value);
      sourceUrl.value = normalized.value;
      // The field the user typed just changed under them. A message that
      // self-destructs cannot explain a change that persists, so it goes to the
      // dialog's own status line instead of a toast.
      if (normalized.pmatRejected) setEditorStatus(t('backend.resolution.pmatRejected'), true);
      applyAutomaticProfile();
      return normalized.value;
    }
    sourceUrl.addEventListener('input', function () {
      transientCredential = '';
      sourceInputChanged = true;
      clearEditorFeedback();
      applyAutomaticProfile();
    });
    sourceUrl.addEventListener('blur', normalizeSourceField);
    applyAutomaticProfile();
    function environmentFromForm() {
      var id = idSelect.value;
      var normalizedSourceUrl = normalizeSourceField();
      var enrichmentUrl = advancedMode ? (enrichment.value.trim() || null) :
        (current && !isNew ? current.postmanEnrichmentUrl : null);
      if (enrichmentUrl && isPostmanWebUrl(enrichmentUrl)) {
        // Returning null aborts the submit, so without a persistent message the
        // button just looks dead once the toast is gone.
        setEditorStatus(t('backend.source.enrichmentHelp'), true); enrichment.focus(); return null;
      }
      return { id: id, label: label.value.trim(), sourceKind: detectedSourceKind, sourceUrl: normalizedSourceUrl,
        postmanEnrichmentUrl: detectedSourceKind === 'openapi' ? enrichmentUrl : null, authRef: auth.checked ? id : null,
        authKind: auth.checked ? detectedAuthKind : 'bearer' };
    }
    function persistEnvironment(environment, carryResolvedPostmanCredential) {
      var prior = current && current.id === environment.id ? current : null;
      var resetCredential = sourceCredentialMustReset(prior, model.authentication, environment);
      if (carryResolvedPostmanCredential && resolvedPostmanCredentialCanCarry(prior, model.authentication, environment)) {
        resetCredential = false;
      }
      var authRevision = model.authentication.revision;
      function writeEnvironment(value, expectedRevision, operation) {
        return postJson('/api/backend/environments', {
          operation: operation, environment: value,
          expectedRevision: expectedRevision, idempotencyKey: idempotencyKey('environment'),
          defaultEnvironmentId: model.defaultEnvironmentId || environment.id
        }).then(function (written) { markEnvironmentSaved(); return written; });
      }
      var operation = model.sourceMode === 'missing' ? 'create' : 'upsert';
      if (!resetCredential) {
        return writeEnvironment(environment, model.environmentRevision, operation).then(function (environmentResult) {
          return { environment: environment, environmentResult: environmentResult,
            authRevision: authRevision, credentialReset: false };
        });
      }
      // Validate and publish a non-sending state before removing the old secret.
      // The final upsert can only re-enable auth after the incompatible secret is gone.
      var dormantEnvironment = Object.assign({}, environment, { authRef: null });
      return writeEnvironment(dormantEnvironment, model.environmentRevision, operation).then(function (dormantResult) {
        return postJson('/api/backend/credential', {
          environmentId: environment.id, operation: 'delete', expectedAuthRevision: authRevision,
          idempotencyKey: idempotencyKey('credential')
        }).then(function (credentialResult) {
          return writeEnvironment(environment, dormantResult.revision, 'upsert').then(function (environmentResult) {
            return { environment: environment, environmentResult: environmentResult,
              authRevision: credentialResult.revision, credentialReset: true };
          });
        });
      });
    }
    function selectAndReload(environmentId) {
      return reloadIntegrationModel().then(function (loaded) {
        if (loaded.activeEnvironmentId === environmentId) return loaded;
        return postJson('/api/backend/environment/select', {
          environmentId: environmentId, expectedStateRevision: loaded.selectionRevision
        }).then(reloadIntegrationModel);
      }).then(function (loaded) {
        // Keep credential-kind comparisons bound to the last environment state
        // persisted by this still-open editor, not the state captured at open.
        current = activeEnvironment();
        return loaded;
      });
    }
    function requestCredential(result, continuation, replacementCode, allowMissing) {
      var environment = result.environment;
      function requireInlineCredential() {
        credentialField.hidden = false;
        setEditorStatus(errorLabel(replacementCode || 'auth-missing'), !!replacementCode);
        setEditorBusy(false);
        credential.focus();
      }
      function storeInlineCredential(carriedValue) {
        var rebinding = typeof carriedValue === 'string';
        var value = rebinding ? carriedValue : credential.value;
        if (!value) {
          if (allowMissing) continuation(model.authentication.revision);
          else requireInlineCredential();
          return;
        }
        credential.value = '';
        if (rebinding) transientCredential = '';
        else transientCredential = value;
        postJson('/api/backend/credential', {
          environmentId: environment.id, operation: 'set', secret: value,
          expectedAuthRevision: model.authentication.revision,
          idempotencyKey: idempotencyKey('credential')
        }).then(function (credentialResult) {
          newCredentialSlotChecked = true;
          return selectAndReload(environment.id).then(function () {
            return continuation(credentialResult.revision);
          });
        }).catch(function (error) { transientCredential = ''; fail(error); });
      }
      function continueOrStore() {
        if (environment.authRef !== environment.id) {
          continuation(model.authentication.revision); return;
        }
        if (credential.value) { storeInlineCredential(); return; }
        if (result.credentialReset && transientCredential) {
          storeInlineCredential(transientCredential); return;
        }
        if (model.authentication.state === 'missing' || replacementCode) {
          if (allowMissing) continuation(model.authentication.revision);
          else requireInlineCredential();
          return;
        }
        continuation(model.authentication.revision);
      }
      var staleCredentialSlot = !newCredentialSlotChecked &&
        (model.authentication.state === 'configured' || model.authentication.state === 'invalid' ||
          model.authentication.dormant);
      if (staleCredentialSlot) {
        postJson('/api/backend/credential', { environmentId: environment.id, operation: 'delete',
          expectedAuthRevision: model.authentication.revision, idempotencyKey: idempotencyKey('credential') })
        .then(function () { newCredentialSlotChecked = true; return selectAndReload(environment.id); })
        .then(continueOrStore, fail);
        return;
      }
      continueOrStore();
    }
    function applyDialogResolution(resolution, candidate) {
      var selectedUrl = candidate && candidate.url || resolution.resolvedUrl;
      if (!selectedUrl && candidate && candidate.uid) selectedUrl = 'https://api.getpostman.com/collections/' + candidate.uid;
      if (selectedUrl) sourceUrl.value = selectedUrl;
      sourceInputChanged = true;
      if (resolution.detectedKind === 'postman') {
        detectedSourceKind = 'postman'; detectedAuthKind = 'x-api-key'; auth.checked = true;
        automaticallyClassifiedPostman = true;
      }
      syncAdvanced(); syncAuthentication(); clearEditorFeedback();
      persistAndContinue(true, true);
    }
    function renderDialogResolution(resolution, environment) {
      clearResolutionActions();
      if (!resolution) return { actionable: false, text: null };
      var postmanResolution = resolution.detectedKind === 'postman';
      if (resolution.resolvedUrl) {
        var resolved = (resolution.candidates || []).find(function (row) { return row.url === resolution.resolvedUrl; }) || {};
        var apply = button('backend.resolution.apply', 'btn btn--primary', function () {
          applyDialogResolution(resolution, { url: resolution.resolvedUrl, title: resolved.title });
        });
        resolutionActions.appendChild(apply); resolutionActions.hidden = false;
        return { actionable: true, text: postmanResolution ? t('backend.resolution.postmanPicker') :
          t('backend.resolution.found', { url: shortUrl(resolution.resolvedUrl), title: resolved.title || environment.label }) };
      }
      if (Array.isArray(resolution.candidates) && resolution.candidates.length) {
        resolution.candidates.forEach(function (candidate) {
          var labelText = candidate.title ? candidate.title + ' — ' + shortUrl(candidate.url || candidate.uid) : shortUrl(candidate.url || candidate.uid);
          var pick = el('button', { type: 'button', class: 'btn', text: labelText });
          pick.addEventListener('click', function () { applyDialogResolution(resolution, candidate); });
          resolutionActions.appendChild(pick);
        });
        if (resolution.truncated) resolutionActions.appendChild(el('p', { class: 'field-help', text: t('backend.resolution.postmanTruncated') }));
        resolutionActions.hidden = false;
        return { actionable: true, text: postmanResolution ? t('backend.resolution.postmanPicker') : t('backend.resolution.candidates') };
      }
      if (postmanResolution && environment.sourceKind !== 'postman') {
        resolutionActions.appendChild(button('backend.resolution.apply', 'btn btn--primary', function () {
          applyDialogResolution(resolution, null);
        }));
        resolutionActions.hidden = false;
        return { actionable: true, text: t('backend.resolution.postmanPicker') };
      }
      var messageKey = postmanResolution ? 'backend.resolution.postmanPicker' :
        (resolution.reason === 'openapi-2-unsupported' ? 'backend.resolution.openapi2' :
          (resolution.reason === 'cross-host-unsupported' ? 'backend.resolution.crossHost' : 'backend.resolution.unrecognized'));
      resolutionActions.appendChild(el('p', { class: 'field-help', text: t(messageKey) }));
      resolutionActions.hidden = false;
      return { actionable: false, text: null };
    }
    function runDialogProbe(environment) {
      clearResolutionActions();
      setEditorStatus(t('backend.job.testing'), false);
      return postJson('/api/backend/test', {
        environmentId: environment.id, expectedEnvironmentRevision: model.environmentRevision,
        expectedAuthRevision: model.authentication.revision, idempotencyKey: idempotencyKey('probe')
      }).then(function (started) {
        return waitForJobResult(started.job.jobId, function () { setEditorStatus(t('backend.job.testing'), false); });
      }).then(function (job) {
        return reloadIntegrationModel().then(function () {
          var outcome = dialogProbeOutcome(job, environment);
          if (outcome.state === 'replace-credential') {
            transientCredential = '';
            setEditorStatus(errorLabel(outcome.errorCode), true);
            requestCredential({ environment: environment }, function () {
              return runDialogProbe(environment).catch(fail);
            }, outcome.errorCode);
            return null;
          }
          if (outcome.state === 'require-credential') {
            transientCredential = '';
            applyAutomaticProfile('auth-rejected');
            setEditorStatus(errorLabel('auth-missing'), false);
            var authenticated = environmentFromForm();
            if (!authenticated) { setEditorBusy(false); return null; }
            return persistEnvironment(authenticated).then(function (result) {
              return selectAndReload(authenticated.id).then(function () {
                requestCredential(result, function () { return runDialogProbe(authenticated).catch(fail); });
              });
            });
          }
          var resolutionResult = renderDialogResolution(outcome.resolution, environment);
          if (outcome.state === 'resolution' && resolutionResult.actionable) {
            setEditorStatus(resolutionResult.text, false);
          } else if (outcome.state === 'failed') {
            transientCredential = '';
            setEditorStatus(errorLabel(outcome.errorCode), true);
          } else {
            transientCredential = '';
            dialog.close();
          }
          setEditorBusy(false); return job;
        });
      });
    }
    function fail(error) {
      var code = error && error.code;
      var environment = environmentFromForm();
      if (environment && environment.authRef === environment.id &&
          ['auth-missing', 'auth-invalid', 'auth-rejected'].indexOf(code) >= 0) {
        setEditorStatus(errorLabel(code), true);
        requestCredential({ environment: environment }, function () {
          return runDialogProbe(environment).catch(fail);
        }, code);
        return;
      }
      setEditorStatus(errorLabel(code), true); setEditorBusy(false);
    }
    function persistAndContinue(testAfterSave, carryResolvedPostmanCredential) {
      if (!form.reportValidity()) return;
      var environment = environmentFromForm();
      if (!environment) return;
      setEditorBusy(true);
      if (testAfterSave) { clearResolutionActions(); setEditorStatus(t('backend.job.testing'), false); }
      persistEnvironment(environment, carryResolvedPostmanCredential).then(function (result) {
        return selectAndReload(environment.id).then(function () {
          requestCredential(result, function () {
            if (testAfterSave) runDialogProbe(environment).catch(fail);
            else { transientCredential = ''; dialog.close(); reloadIntegrationModel(); }
          }, null, !testAfterSave);
        });
      }).catch(fail);
    }
    testButton.addEventListener('click', function () { persistAndContinue(true); });
    form.addEventListener('submit', function (event) {
      event.preventDefault(); persistAndContinue(false);
    });
    content.appendChild(form);
  });
}
function deleteEnvironment() {
  var current = activeEnvironment();
  if (!current) return;
  // Everything after the question keeps reading the environment the question
  // named, so the fallback lookup and the delete both move behind the answer.
  // A refusal is a full no-op: no toast, no request, no busy state.
  confirmDialog({
    title: t('backend.action.deleteEnvironment'),
    message: t('backend.confirm.deleteEnvironment', { name: current.label }),
    confirmLabel: t('backend.action.deleteEnvironment'), danger: true
  }).then(function (accepted) {
    if (!accepted) return;
    var fallback = (model.environments || []).find(function (row) { return row.id !== current.id; });
    if (!fallback) { clipboard.toast(t('backend.error.last-environment-delete-forbidden')); return; }
    setBusy(function () { return postJson('/api/backend/environments', { operation: 'delete', environmentId: current.id,
      expectedRevision: model.environmentRevision, idempotencyKey: idempotencyKey('environment'),
      defaultEnvironmentId: model.defaultEnvironmentId === current.id ? fallback.id : model.defaultEnvironmentId }); });
  });
}
function credentialDialog(environmentId, expectedAuthRevision, callbacks) {
  callbacks = callbacks || {};
  openDialog('backend.credential.title', function (dialog, content) {
    var completed = false;
    var environment = (model.environments || []).find(function (row) { return row.id === environmentId; });
    var apiKey = environment && environment.authKind === 'x-api-key';
    var secret = el('input', { type: 'password', class: 'input', autocomplete: 'new-password', maxLength: 16384, required: true });
    var status = el('p', { class: 'backend-notice backend-notice--error', hidden: true,
      attrs: { role: 'alert', 'aria-live': 'assertive', 'aria-atomic': 'true' } });
    // Opened from a resolution flow, the environment rewrite (and sometimes the
    // deletion of the old secret) has already been committed — "Cancel" would
    // promise an undo that does not exist.
    var applied = callbacks.applied === true;
    var savedNotice = el('p', { class: 'backend-dialog-saved', hidden: !applied,
      attrs: { role: 'status', 'aria-live': 'polite' }, text: t('backend.credential.appliedNotice') });
    var form = el('form', { class: 'backend-dialog-form' }, [
      field(apiKey ? 'backend.credential.labelApiKey' : 'backend.credential.label', secret,
        apiKey ? 'backend.credential.helpApiKey' : 'backend.credential.help'),
      status,
      el('div', { class: 'backend-dialog-actions' }, [savedNotice,
        button(applied ? 'backend.action.close' : 'common.cancel', 'btn btn--ghost backend-dialog-dismiss',
          function () { dialog.close(); }),
        submitButton('backend.credential.save', 'btn btn--primary')])
    ]);
    if (applied) {
      var head = dialog.querySelector('.backend-dialog-head .backend-dialog-dismiss');
      if (head) head.textContent = t('backend.action.close');
    }
    function setCredentialBusy(busy) {
      var controls = dialog.querySelectorAll('button, input');
      for (var index = 0; index < controls.length; index++) controls[index].disabled = busy;
    }
    form.addEventListener('submit', function (event) {
      event.preventDefault();
      setCredentialBusy(true);
      var value = secret.value; secret.value = '';
      postJson('/api/backend/credential', { environmentId: environmentId, operation: 'set', secret: value,
        expectedAuthRevision: Number.isSafeInteger(expectedAuthRevision) ? expectedAuthRevision : model.authentication.revision,
        idempotencyKey: idempotencyKey('credential') })
      .then(function (response) {
        completed = true; dialog.close();
        return reloadIntegrationModel().then(function () {
          if (callbacks.onStored) callbacks.onStored(response);
          return response;
        });
      }, function (error) {
        // The secret field was cleared before the request, so a toast that fades
        // leaves an empty form with no reason for the failure.
        setCredentialBusy(false);
        status.hidden = false; status.textContent = errorLabel(error && error.code);
        secret.focus();
      });
    });
    dialog.addEventListener('close', function () { if (!completed && callbacks.onCancel) callbacks.onCancel(); });
    content.appendChild(form); setTimeout(function () { secret.focus(); }, 0);
  });
}
function deleteCredential() {
  // The stored secret cannot be recovered, so the request leaves the browser
  // only after the answer; until then the panel is neither busy nor disabled.
  confirmDialog({
    title: t('backend.action.deleteCredential'), message: t('backend.confirm.deleteCredential'),
    confirmLabel: t('backend.action.deleteCredential'), danger: true
  }).then(function (accepted) {
    if (!accepted) return;
    setBusy(function () { return postJson('/api/backend/credential', { environmentId: model.activeEnvironmentId, operation: 'delete',
      expectedAuthRevision: model.authentication.revision, idempotencyKey: idempotencyKey('credential') }); });
  });
}
function clearIntegration() {
  openDialog('backend.clear.title', function (dialog, content) {
    var resetKey = idempotencyKey('backend-reset'), busy = false;
    var status = el('p', { class: 'backend-notice', hidden: true,
      attrs: { role: 'status', 'aria-live': 'polite', 'aria-atomic': 'true' } });
    var cancel = button('common.cancel', 'btn btn--ghost', function () { dialog.close(); });
    function setResetBusy(next) {
      busy = next;
      if (next) dialog.setAttribute('aria-busy', 'true');
      else dialog.removeAttribute('aria-busy');
      var controls = dialog.querySelectorAll('button');
      for (var index = 0; index < controls.length; index++) controls[index].disabled = next;
    }
    dialog.addEventListener('cancel', function (event) { if (busy) event.preventDefault(); });
    var confirm = button('backend.clear.confirm', 'btn backend-clear-confirm', function () {
      setResetBusy(true);
      confirm.textContent = t('backend.clear.progressButton');
      status.hidden = false; status.className = 'backend-notice integration-clear-progress';
      status.setAttribute('role', 'status'); status.setAttribute('aria-live', 'polite');
      status.textContent = t('backend.clear.progress');
      postJson('/api/backend/integration/reset', {
        expectedEnvironmentRevision: model.environmentRevision,
        expectedSnapshotHash: model.snapshot && model.snapshot.hash || null,
        expectedStateRevision: model.selectionRevision,
        idempotencyKey: resetKey
      }).then(function () {
        return reloadIntegrationModel();
      }).then(function () {
        dialog.close();
      }, function (error) {
        setResetBusy(false);
        confirm.textContent = t('backend.clear.confirm');
        status.hidden = false; status.className = 'backend-notice backend-notice--error';
        status.setAttribute('role', 'alert'); status.setAttribute('aria-live', 'assertive');
        status.textContent = errorLabel(error && error.code);
      });
    });
    content.appendChild(el('p', { class: 'backend-clear-copy', text: t('backend.clear.body') }));
    content.appendChild(status);
    content.appendChild(el('div', { class: 'backend-dialog-actions' }, [cancel, confirm]));
    setTimeout(function () { cancel.focus(); }, 0);
  });
}
function buildHeader() {
  var actionNodes = [];
  var activeJob = (model.jobs || [])[0];
  var activeAction = pendingAction || activeJob && activeJob.reportType;
  if (activeAction) {
    var activeLabel = activeAction === 'refresh' ? 'backend.job.refreshing' :
      (activeAction === 'test' || activeAction === 'probe' ? 'backend.job.testing' : 'backend.job.updating');
    actionNodes.push(button(activeLabel, 'btn btn--primary', null, true));
  } else if (model.actions.canCreateSource) {
    actionNodes.push(button('backend.action.addFirstEnvironment', 'btn btn--primary', function () { sourceEditor(true, false); }));
  } else if (model.actions.canRefresh) {
    actionNodes.push(button(model.snapshot && model.snapshot.present
      ? 'backend.action.refresh' : 'backend.action.import', 'btn btn--primary', runRefresh));
    if (model.actions.canTest) actionNodes.push(button('backend.action.test', 'btn', runTest));
  } else if (model.actions.canTest) {
    actionNodes.push(button('backend.action.test', 'btn btn--primary', runTest));
  }
  if (model.snapshot && model.snapshot.present) {
    actionNodes.push(el('a', { class: 'btn btn--ghost', href: '#api', text: t('backend.action.apiOverview') }));
  }
  if (model.actions.canClearIntegration) {
    actionNodes.push(button('backend.action.clearIntegration', 'btn btn--ghost backend-clear-action', clearIntegration));
  }
  var actions = el('div', { class: 'backend-header-actions' }, actionNodes);
  return el('div', { class: 'backend-integration-header' }, [
    el('div', { class: 'backend-header-copy' }, [
      el('h2', { class: 'panel-title', text: t('backend.title') }),
      el('p', { class: 'panel-lead', text: t('backend.lead') })
    ]),
    el('div', { class: 'backend-header-side' }, [el('span', { class: 'backend-overall backend-overall--' + model.state, text: stateLabel(model.state) }), actions])
  ]);
}
function environmentControl() {
  var id = 'backend-environment-select';
  var select = el('select', { id: id, class: 'input backend-environment-select', attrs: { 'aria-label': t('backend.field.environment') } });
  (model.environments || []).forEach(function (row) {
    select.appendChild(el('option', { value: row.id, text: row.label, selected: row.id === model.activeEnvironmentId }));
  });
  select.addEventListener('change', selectEnvironment);
  return el('span', { class: 'backend-inline-control' }, [
    el('label', { class: 'u-visually-hidden', attrs: { for: id }, text: t('backend.field.environment') }), select,
    model.actions.canEditSource ? button('backend.action.change', 'btn btn--ghost btn--small', function () { sourceEditor(false, false); }) : null
  ]);
}
function authActions() {
  var wrap = el('span', { class: 'backend-inline-control' }, [el('span', { text: authLabel(model.authentication.state) })]);
  if (model.actions.canSetCredential) wrap.appendChild(button('backend.action.setCredential', 'btn btn--ghost btn--small', function () { credentialDialog(model.activeEnvironmentId); }));
  if (model.actions.canDeleteCredential) wrap.appendChild(button('backend.action.deleteCredential', 'btn btn--ghost btn--small', deleteCredential));
  return wrap;
}
function sourceCard() {
  var source = model.source;
  if (model.sourceMode === 'missing') {
    var titleId = 'backend-first-environment-title';
    return el('section', { class: 'card backend-card backend-setup', attrs: { 'aria-labelledby': titleId } }, [
      el('div', { class: 'backend-setup-main' }, [
        el('span', { class: 'backend-setup-eyebrow', text: t('backend.setup.eyebrow') }),
        el('h3', { id: titleId, class: 'panel-section-title backend-setup-title', text: t('backend.setup.title') }),
        el('p', { class: 'backend-setup-body', text: t('backend.setup.body') }),
        button('backend.action.addFirstEnvironment', 'btn btn--primary backend-setup-action', function () { sourceEditor(true, false); })
      ]),
      el('ol', { class: 'backend-setup-steps', attrs: { 'aria-label': t('backend.setup.stepsLabel') } }, [
        el('li', { text: t('backend.setup.stepSource') }),
        el('li', { text: t('backend.setup.stepTest') }),
        el('li', { text: t('backend.setup.stepRefresh') })
      ])
    ]);
  }
  var list = el('dl', { class: 'backend-integration-list' }, [
    statusRow('backend.field.source', source ? shortUrl(source.url) : t('backend.value.notConfigured'),
      source && model.actions.canEditSource ? button('backend.action.change', 'btn btn--ghost btn--small', function () { sourceEditor(false, false); }) : null),
    statusRow('backend.field.environment', model.sourceMode === 'manifest' ? environmentControl() : (source ? source.title : '—')),
    statusRow('backend.field.lastPulled', lastPulledLabel(model.snapshot.pulledAt)),
    statusRow('backend.field.contract', contractLabel()),
    statusRow('backend.field.authentication', authActions())
  ]);
  var card = el('section', { class: 'card backend-card backend-source-status' }, [
    el('h3', { class: 'panel-section-title', text: t('backend.sourceStatus') }), list
  ]);
  if (model.snapshot.environmentMismatch) card.appendChild(el('p', { class: 'backend-notice backend-notice--warn', text: t('backend.warning.environmentMismatch') }));
  if (model.sourceMode === 'invalid') card.appendChild(el('p', { class: 'backend-notice backend-notice--error', text: errorLabel(model.sourceError) }));
  return card;
}
function previewWasPublished(preview) {
  var refresh = model.latestRefresh;
  return !!(preview && !preview.fresh && refresh && ['success', 'partial'].indexOf(refresh.state) >= 0 &&
    refresh.sourceFingerprint === preview.sourceFingerprint && model.snapshot && refresh.currentHash === model.snapshot.hash);
}
export function resolutionApplyPlan(current, authentication, resolution, candidate) {
  if (!current) return null;
  var detectedKind = resolution.detectedKind || current.sourceKind;
  var sourceUrl = candidate && candidate.url || resolution.resolvedUrl;
  if (!sourceUrl && candidate && candidate.uid) sourceUrl = 'https://api.getpostman.com/collections/' + candidate.uid;
  var postmanBootstrap = detectedKind === 'postman' && current.sourceKind !== 'postman';
  if (!sourceUrl && postmanBootstrap) sourceUrl = current.sourceUrl;
  if (!sourceUrl) return null;
  var environment = {
    id: current.id,
    label: current.label,
    sourceKind: detectedKind,
    sourceUrl: sourceUrl,
    postmanEnrichmentUrl: detectedKind === 'postman' ? null : current.postmanEnrichmentUrl,
    authRef: detectedKind === 'postman' ? current.id : current.authRef,
    authKind: detectedKind === 'postman' ? 'x-api-key' : current.authKind
  };
  var switchingToPostmanKey = environment.authKind === 'x-api-key' && current.authKind !== 'x-api-key';
  var resetCredential = sourceCredentialMustReset(current, authentication, environment) &&
    !resolvedPostmanCredentialCanCarry(current, authentication, environment);
  var needsCredential = !!(environment.authRef && (!current.authRef ||
    authentication.state === 'missing' || authentication.state === 'invalid'));
  var environmentChanged = ['sourceKind', 'sourceUrl', 'postmanEnrichmentUrl', 'authRef', 'authKind'].some(function (key) {
    return environment[key] !== current[key];
  });
  if (!environmentChanged && !switchingToPostmanKey && !needsCredential) return null;
  return { environment: environment, switchingToPostmanKey: switchingToPostmanKey,
    deleteCredential: resetCredential,
    openCredential: resetCredential || needsCredential };
}
function upsertResolutionEnvironment(environment) {
  return postJson('/api/backend/environments', {
    operation: 'upsert', environment: environment, expectedRevision: model.environmentRevision,
    idempotencyKey: idempotencyKey('environment'), defaultEnvironmentId: model.defaultEnvironmentId
  });
}
function applyResolution(resolution, candidate) {
  var current = activeEnvironment();
  var plan = resolutionApplyPlan(current, model.authentication, resolution, candidate);
  if (!plan) return;
  var environment = plan.environment;
  var authRevision = model.authentication.revision;
  function upsertAndContinue(expectedAuthRevision, openCredential) {
    return upsertResolutionEnvironment(environment).then(function (response) {
      if (openCredential) {
        // "Apply and test again" used to stop here: the credential dialog was
        // opened without a continuation, so storing the secret never probed and
        // the panel kept showing the original failure.
        return load().then(function () {
          credentialDialog(environment.id, expectedAuthRevision, {
            applied: true,
            onStored: function (stored) {
              setBusy(function () {
                return postJson('/api/backend/test', {
                  environmentId: environment.id,
                  expectedEnvironmentRevision: model.environmentRevision,
                  expectedAuthRevision: stored && stored.revision,
                  idempotencyKey: idempotencyKey('probe')
                }).then(function (tested) { if (tested.job) pollJob(tested.job.jobId); return tested; });
              }, 'test');
            }
          });
          return response;
        });
      }
      return postJson('/api/backend/test', {
        environmentId: environment.id,
        expectedEnvironmentRevision: response.revision,
        expectedAuthRevision: expectedAuthRevision,
        idempotencyKey: idempotencyKey('probe')
      }).then(function (tested) { if (tested.job) pollJob(tested.job.jobId); return tested; });
    });
  }
  setBusy(function () {
    if (plan.deleteCredential) {
      return postJson('/api/backend/credential', { environmentId: current.id, operation: 'delete',
        expectedAuthRevision: authRevision, idempotencyKey: idempotencyKey('credential') })
      .then(function (deleted) { return upsertAndContinue(deleted.revision, true); });
    }
    return upsertAndContinue(authRevision, plan.openCredential);
  });
}
function configureResolutionCredential() {
  var current = activeEnvironment();
  if (!current) return;
  var authRevision = model.authentication.revision;
  var environment = { id: current.id, label: current.label, sourceKind: current.sourceKind, sourceUrl: current.sourceUrl,
    postmanEnrichmentUrl: current.postmanEnrichmentUrl, authRef: current.id, authKind: current.authKind };
  setBusy(function () {
    return upsertResolutionEnvironment(environment).then(function (response) {
      return load().then(function () {
        credentialDialog(environment.id, authRevision, { applied: true });
        return response;
      });
    });
  });
}
function resolutionContent(preview) {
  var resolution = preview && preview.resolution;
  if (!resolution || preview.environmentRevision !== model.environmentRevision) return [];
  var body = [el('h4', { class: 'panel-section-title', text: t('backend.resolution.title') })];
  if (resolution.reason === 'auth-required') {
    body.push(el('p', { class: 'backend-notice backend-notice--warn', text: errorLabel('auth-missing') }));
    body.push(button('backend.action.setCredential', 'btn', configureResolutionCredential, !model.actions.canTest));
    return body;
  }
  var postmanResolution = resolution.detectedKind === 'postman';
  if (postmanResolution) body.push(el('p', { text: t('backend.resolution.postmanPicker') }));
  if (resolution.resolvedUrl) {
    var resolved = (resolution.candidates || []).find(function (row) { return row.url === resolution.resolvedUrl; }) || {};
    if (!postmanResolution) body.push(el('p', { text: t('backend.resolution.found', {
      url: shortUrl(resolution.resolvedUrl), title: resolved.title || activeEnvironment().label
    }) }));
    if (resolutionApplyPlan(activeEnvironment(), model.authentication, resolution,
      { url: resolution.resolvedUrl, title: resolved.title })) {
      var apply = button('backend.resolution.apply', 'btn btn--primary', function () {
        applyResolution(resolution, { url: resolution.resolvedUrl, title: resolved.title });
      }, !model.actions.canTest);
      apply.setAttribute('data-backend-focus', 'resolution-apply');
      body.push(apply);
    }
    return body;
  }
  if (Array.isArray(resolution.candidates) && resolution.candidates.length) {
    if (!postmanResolution) body.push(el('p', { text: t('backend.resolution.candidates') }));
    var list = el('div', { class: 'backend-advanced-actions' });
    resolution.candidates.forEach(function (candidate) {
      var label = candidate.title ? candidate.title + ' — ' + shortUrl(candidate.url || candidate.uid) : shortUrl(candidate.url || candidate.uid);
      var pick = el('button', { type: 'button', class: 'btn', text: label, disabled: !model.actions.canTest });
      pick.addEventListener('click', function () { applyResolution(resolution, candidate); });
      list.appendChild(pick);
    });
    body.push(list);
    if (resolution.truncated) body.push(el('p', { class: 'field-help', text: t('backend.resolution.postmanTruncated') }));
    return body;
  }
  if (postmanResolution) {
    if (resolutionApplyPlan(activeEnvironment(), model.authentication, resolution, null)) {
      body.push(button('backend.resolution.apply', 'btn btn--primary', function () {
        applyResolution(resolution, null);
      }, !model.actions.canTest));
    }
    return body;
  }
  var messageKey = resolution.reason === 'openapi-2-unsupported' ? 'backend.resolution.openapi2' :
    (resolution.reason === 'cross-host-unsupported' ? 'backend.resolution.crossHost' : 'backend.resolution.unrecognized');
  body.push(el('p', { class: 'field-help', text: t(messageKey) }));
  return body;
}
function previewCard() {
  var preview = model.preview;
  if (!preview) return el('section', { class: 'card backend-card backend-preview' }, [
    el('h3', { class: 'panel-section-title', text: t('backend.preview.title') }),
    el('p', { class: 'field-help', text: t('backend.preview.empty') })
  ]);
  if (preview.state === 'failed') {
    var failureBody = [el('h3', { class: 'panel-section-title', text: t('backend.preview.title') })];
    if (previewFailureNeedsErrorBanner(preview, activeEnvironment(), model.environmentRevision)) {
      failureBody.push(el('p', { class: 'backend-notice backend-notice--error', text: errorLabel(preview.error && preview.error.code) }));
    }
    return el('section', { class: 'card backend-card backend-preview' }, failureBody.concat(resolutionContent(preview)));
  }
  if (previewWasPublished(preview)) return null;
  var delta = preview.delta || {}, summary = t('backend.preview.delta', { added: delta.added || 0, changed: delta.changed || 0,
    removed: delta.removed || 0, breaking: delta.potentiallyBreaking || 0 });
  var body = [
    el('h3', { class: 'panel-section-title', text: t('backend.preview.title') }),
    el('div', { class: 'backend-preview-meta' }, [
      el('strong', { text: preview.sourceSummary && preview.sourceSummary.title || model.source.title }),
      el('span', { text: (preview.sourceSummary && preview.sourceSummary.kind || '') + ' ' + (preview.sourceSummary && preview.sourceSummary.version || '') }),
      el('span', { text: t('backend.preview.endpoints', { n: preview.sourceSummary && preview.sourceSummary.endpointCount || 0 }) }),
      el('span', { text: t('backend.preview.environment', { name: activeEnvironment() && activeEnvironment().label || preview.environmentId }) }),
      el('code', { text: shortHash(preview.sourceFingerprint) })
    ]),
    el('p', { class: 'backend-preview-delta', text: summary }),
    el('p', { class: 'field-help', text: t('backend.preview.checked', { date: dateLabel(preview.checkedAt) }) })
  ];
  if (!preview.fresh) body.push(el('p', { class: 'backend-notice backend-notice--warn', text: t('backend.warning.previewStale') }));
  if (preview.fresh) body.push(el('div', { class: 'backend-preview-actions' }, [
    el('p', { class: 'field-help', text: t('backend.preview.applyHint') }),
    button(model.snapshot && model.snapshot.present ? 'backend.action.refresh' : 'backend.action.import',
      'btn btn--primary', runRefresh)
  ]));
  (preview.warnings || []).forEach(function (warning) { body.push(el('p', { class: 'backend-notice backend-notice--warn', text: errorLabel(warning.code) })); });
  return el('section', { class: 'card backend-card backend-preview' }, body);
}
function jobsCard() {
  var running = (model.jobs || []).slice();
  if (pendingAction && !running.length) running.push({ reportType: pendingAction, progress: [] });
  if (!running.length) return null;
  var items = [];
  running.forEach(function (job) {
    var progress = (job.progress || []).map(function (row) {
      var key = 'backend.job.phase.' + String(row.phase || 'unknown');
      var label = t(key);
      return el('li', { text: label === key ? t('backend.job.phase.unknown') : label });
    });
    var jobLabel = job.reportType === 'refresh' ? 'backend.job.refreshing' :
      (job.reportType === 'test' || job.reportType === 'probe' ? 'backend.job.testing' : 'backend.job.updating');
    items.push(el('div', { class: 'backend-job', attrs: { 'aria-live': 'polite' } }, [
      el('strong', { text: t(jobLabel) }),
      el('ol', { class: 'backend-job-progress' }, progress)
    ]));
  });
  return el('section', { class: 'card backend-card' }, items);
}
function resultCard() {
  var result = model.latestRefresh;
  if (!result) return null;
  if (result.state === 'failed' || result.state === 'interrupted') return el('section', { class: 'card backend-card backend-result' }, [
    el('h3', { class: 'panel-section-title', text: t('backend.result.failed') }),
    el('p', { class: 'backend-notice backend-notice--error', text: errorLabel(result.error && result.error.code) }),
    button('backend.action.test', 'btn', runTest, !model.actions.canTest)
  ]);
  if (result.state !== 'success' && result.state !== 'partial') return null;
  var body = [
    el('h3', { class: 'panel-section-title', text: t('backend.result.title') }),
    el('p', { text: t('backend.result.delta', { added: result.addedEndpoints || 0, changed: result.changedEndpoints || 0,
      removed: result.removedEndpoints || 0, breaking: result.breakingChanges || 0 }) }),
    el('a', { href: '#api', class: 'btn btn--primary', text: t('backend.result.review') })
  ];
  if (result.state === 'partial') body.splice(2, 0, el('p', { class: 'backend-notice backend-notice--warn', text: t('backend.result.partial') }));
  return el('section', { class: 'card backend-card backend-result' }, body);
}
function advanced() {
  var env = activeEnvironment();
  var refreshCommand = env && env.sourceKind === 'postman' ? 'npm run contract:refresh-postman' : 'npm run contract:refresh-openapi';
  var details = el('details', { class: 'card backend-card backend-advanced' });
  details.open = advancedOpen;
  details.addEventListener('toggle', function () { advancedOpen = details.open; });
  details.appendChild(el('summary', { text: t('backend.advanced.title'), attrs: { 'data-backend-focus': 'advanced-summary' } }));
  var controls = el('div', { class: 'backend-advanced-actions' });
  function advancedButton(key, label, className, handler) {
    var node = button(label, className, handler); node.setAttribute('data-backend-focus', key); return node;
  }
  if (model.actions.canEditSource && env) controls.appendChild(advancedButton('advanced-change', 'backend.action.changeAdvanced', 'btn btn--small', function () { sourceEditor(false, true); }));
  if (model.actions.canEditSource && model.environments.length < 4) controls.appendChild(advancedButton('advanced-add', 'backend.action.addEnvironment', 'btn btn--small', function () { sourceEditor(true, true); }));
  if (model.actions.canEditSource && model.environments.length > 1) controls.appendChild(advancedButton('advanced-delete', 'backend.action.deleteEnvironment', 'btn btn--ghost btn--small', deleteEnvironment));
  controls.appendChild(advancedButton('advanced-copy', 'backend.copyPrompt', 'btn btn--ghost btn--small', function () {
    clipboard.copy('From the project root, run the typed Backend contract probe and refresh flow described in orchestrator/api-contract/README.md. Use environments.json and authRef; never put a credential, URL override, command, or raw collection into a prompt.');
  }));
  details.appendChild(controls);
  var lastError = model.diagnostics.lastError;
  details.appendChild(el('dl', { class: 'backend-diagnostics' }, [
    lastError ? statusRow('backend.advanced.lastError', el('span', { class: 'backend-diagnostic-error' }, [
      el('code', { text: lastError.code }), el('span', { text: errorLabel(lastError.code) })
    ])) : null,
    statusRow('backend.advanced.sourceKind', env ? env.sourceKind : model.source && model.source.kind || '—'),
    statusRow('backend.advanced.snapshotHash', shortHash(model.diagnostics.snapshotHash)),
    statusRow('backend.advanced.generation', model.diagnostics.generationId || '—'),
    statusRow('backend.advanced.pointer', el('code', { text: model.diagnostics.generationPointer })),
    statusRow('backend.advanced.reports', el('code', { text: model.diagnostics.reportsDirectory })),
    statusRow('backend.advanced.commands', el('code', { text: 'npm run contract:doctor · npm run contract:probe · ' + refreshCommand }))
  ]));
  return details;
}

function render() {
  if (!sectionEl || !mounted) return;
  var active = document.activeElement;
  var focusKey = active && sectionEl.contains(active) ? active.getAttribute('data-backend-focus') : null;
  while (sectionEl.firstChild) sectionEl.removeChild(sectionEl.firstChild);
  if (loading && !model) { sectionEl.appendChild(el('p', { text: t('backend.loading') })); return; }
  if (loadError && !model) {
    sectionEl.appendChild(el('div', { class: 'backend-notice backend-notice--error' }, [el('span', { text: errorLabel(loadError) }),
      button('api.retry', 'btn btn--small', load)])); return;
  }
  if (!model) return;
  sectionEl.appendChild(buildHeader());
  sectionEl.appendChild(sourceCard());
  if (model.sourceMode === 'missing') return;
  var job = jobsCard(); if (job) sectionEl.appendChild(job);
  var preview = previewCard(); if (preview) sectionEl.appendChild(preview);
  var result = resultCard(); if (result) sectionEl.appendChild(result);
  sectionEl.appendChild(advanced());
  if (pendingAction) {
    sectionEl.setAttribute('aria-busy', 'true');
    var controls = sectionEl.querySelectorAll('button, select');
    for (var controlIndex = 0; controlIndex < controls.length; controlIndex++) controls[controlIndex].disabled = true;
  } else sectionEl.removeAttribute('aria-busy');
  if (focusKey && /^[a-z-]+$/.test(focusKey)) {
    var replacement = sectionEl.querySelector('[data-backend-focus="' + focusKey + '"]');
    if (replacement) replacement.focus();
  }
}
function connectEvents() {
  if (eventSubscriptions) return;
  eventSubscriptions = [
    siteEvents.on('open', function () {
      if (loadError && !model) load();
    }),
    siteEvents.on('backend-job', load),
    siteEvents.on('backend-integration', load)
  ];
}

export const backend = {
  mount: function (rootEl) {
    sectionEl = rootEl; mounted = true; render(); load(); connectEvents();
  },
  refresh: function () { render(); }
};
