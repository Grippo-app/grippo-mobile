import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { syncFailureText } from './sync-errors.js';
import { presentFigmaError } from './error-presenter.js';
import { figmaEnumText } from './enum-labels.js';

var el = dom.el;
function t(key, params) { return i18n && typeof i18n.t === 'function' ? i18n.t(key, params) : key; }
function date(value) {
  if (!value) return t('figma.value.never');
  var parsed = new Date(value);
  return isNaN(parsed.getTime()) ? t('figma.value.unknown') : parsed.toLocaleString();
}
function button(key, cls, handler) {
  var node = el('button', { type: 'button', class: cls || 'btn', text: t(key) });
  node.addEventListener('click', handler);
  return node;
}
function readinessRow(nameKey, stateEl, secondaryEl, actionEl) {
  return el('li', { class: 'figma-integration-readiness-row' }, [
    el('span', { class: 'figma-integration-readiness-name', text: t(nameKey) }),
    el('span', { class: 'figma-integration-readiness-content' }, [stateEl, secondaryEl]),
    actionEl || el('span')
  ]);
}
function recoveryActionAllowed(model, action) {
  var actions = model && model.actions || {};
  if (action === 'test') return actions.canTest === true;
  if (action === 'reconnect' || action === 'changeAccount') return actions.canChangeAccount !== false;
  if (action === 'selectFile' || action === 'selectAnotherFile') return actions.canChangeFile === true;
  return true;
}
var STATE_KEYS = {
  account: {
    connected: 'figma.account.connected',
    unknown: 'figma.account.unknown',
    missing: 'figma.account.missing'
  },
  file: {
    selected: 'figma.file.selected',
    missing: 'figma.file.missing',
    invalid: 'figma.file.invalid'
  },
  access: {
    verified: 'figma.access.verified',
    unverified: 'figma.access.unverified',
    unknown: 'figma.access.unknown',
    denied: 'figma.access.denied'
  },
  quota: {
    ok: 'figma.quota.ok',
    warning: 'figma.quota.warning',
    blocked: 'figma.quota.blocked'
  }
};
var DIAGNOSTIC_LABEL_KEYS = Object.freeze({
  connectorState: 'figma.diagnostic.connectorState',
  connectorScope: 'figma.diagnostic.connectorScope',
  connectorUrl: 'figma.diagnostic.connectorUrl',
  competingConnector: 'figma.diagnostic.competingConnector',
  checkedAt: 'figma.diagnostic.checkedAt',
  syncRecoveryState: 'figma.diagnostic.syncRecoveryState',
  taskPublicationRecoveryState: 'figma.diagnostic.taskPublicationRecoveryState',
  verificationGeneration: 'figma.diagnostic.verificationGeneration',
  accountReceipt: 'figma.diagnostic.accountReceipt',
  generationPointer: 'figma.diagnostic.generationPointer'
});
function diagnosticValue(key, value) {
  if (key === 'connectorState') return figmaEnumText('connectorState', value);
  if (key === 'connectorScope') return figmaEnumText('connectorScope', value);
  if (key === 'syncRecoveryState' || key === 'taskPublicationRecoveryState') {
    return figmaEnumText('recoveryState', value);
  }
  if (key === 'checkedAt') return date(value);
  return String(value);
}
function stateText(category, state) {
  var stateKeys = STATE_KEYS[category] || {};
  var labelKey = stateKeys[state] || stateKeys.unknown || 'figma.value.unknown';
  return el('span', { class: 'figma-integration-state figma-integration-state--' + state }, [
    el('span', { attrs: { 'aria-hidden': 'true' }, text: state === 'connected' || state === 'selected' || state === 'verified' ? '✓' : state === 'denied' ? '!' : '–' }),
    el('span', { text: t(labelKey) })
  ]);
}
export function createFigmaFeatureView(handlers) {
  var root = el('div', { class: 'figma-feature' });
  var title = el('h2', { class: 'panel-title', text: t('figma.title') });
  var lead = el('p', { class: 'panel-lead', text: t('figma.lead') });
  var badge = el('span', { class: 'figma-integration-badge' });
  var header = el('div', { class: 'figma-integration-header' }, [
    el('div', null, [title, lead]),
    el('div', { class: 'figma-integration-header-side' }, [badge])
  ]);
  var stateTitle = el('h3', {
    id: 'figma-feature-state-title', attrs: { tabindex: '-1' }
  });
  var stateBody = el('p');
  var enable = button('figma.feature.enable', 'btn btn--primary', handlers.enable);
  var command = el('code', { class: 'figma-feature-command', text: 'npm start' });
  var restart = el('div', { class: 'figma-feature-restart', hidden: true }, [command]);
  var state = el('section', {
    class: 'figma-feature-state', attrs: {
      'aria-labelledby': 'figma-feature-state-title',
      'aria-live': 'polite',
      'aria-atomic': 'true',
      role: 'status'
    }
  }, [stateTitle, stateBody, enable, restart]);
  root.appendChild(header);
  root.appendChild(state);

  function update(feature, busy) {
    feature = feature || {};
    var kind = ['disabled', 'restart-required', 'invalid'].indexOf(feature.state) >= 0
      ? feature.state : 'invalid';
    var copyKind = kind === 'restart-required' ? 'restart' : kind;
    root.setAttribute('data-state', kind);
    badge.className = 'figma-integration-badge figma-integration-badge--' +
      (kind === 'disabled' ? 'needs-attention' : 'unavailable');
    badge.textContent = t('figma.feature.' + copyKind + '.title');
    stateTitle.textContent = t('figma.feature.' + copyKind + '.title');
    stateBody.textContent = t('figma.feature.' + copyKind + '.body');
    enable.hidden = kind !== 'disabled';
    enable.disabled = !!busy || feature.canEnable !== true;
    enable.textContent = busy ? t('figma.feature.enabling') : t('figma.feature.enable');
    restart.hidden = kind !== 'restart-required';
  }

  return {
    el: root,
    update: update,
    focusAction: function () {
      if (!enable.hidden && typeof enable.focus === 'function') enable.focus();
      else if (typeof stateTitle.focus === 'function') {
        stateTitle.focus();
      }
    }
  };
}
export function createIntegrationView(handlers) {
  var root = el('div', { class: 'figma-integration' });
  var title = el('h2', {
    class: 'panel-title', text: t('figma.title'), attrs: { tabindex: '-1' }
  });
  var lead = el('p', { class: 'panel-lead', text: t('figma.lead') });
  var badge = el('span', { class: 'figma-integration-badge' });
  var test = button('figma.action.test', 'btn btn--primary', handlers.test);
  var clear = button('figma.action.clearIntegration', 'btn btn--ghost figma-clear-action', handlers.clear);
  var header = el('div', { class: 'figma-integration-header' }, [
    el('div', null, [title, lead]),
    el('div', { class: 'figma-integration-header-side' }, [badge, el('div', { class: 'figma-integration-header-actions' }, [test, clear])])
  ]);
  var errorBox = el('div', { class: 'figma-integration-error', hidden: true });
  var intentBox = el('div', {
    class: 'figma-integration-intent', hidden: true,
    attrs: { role: 'status', 'aria-live': 'polite' }
  });
  var syncButtons = {
    tokens: button('figma.action.syncTokens', 'btn', function () { handlers.sync('tokens'); }),
    components: button('figma.action.syncComponents', 'btn', function () { handlers.sync('components'); }),
    drift: button('figma.action.checkDrift', 'btn', function () { handlers.sync('drift'); })
  };
  var syncActionsTitle = el('h3', {
    id: 'figma-sync-actions-title', text: t('figma.syncActions.title'), attrs: { tabindex: '-1' }
  });
  var syncActions = el('section', { class: 'figma-integration-sync-actions', attrs: { 'aria-labelledby': 'figma-sync-actions-title' } }, [
    el('div', null, [
      syncActionsTitle,
      el('p', { text: t('figma.syncActions.help') })
    ]),
    el('div', { class: 'figma-integration-sync-buttons' }, [syncButtons.tokens, syncButtons.components, syncButtons.drift])
  ]);
  var list = el('ul', { class: 'figma-integration-readiness' });
  var context = el('div', { class: 'figma-integration-context' });
  var diagnostics = el('details', { class: 'figma-integration-diagnostics' }, [el('summary', { text: t('figma.diagnostics') })]);
  root.appendChild(header); root.appendChild(errorBox); root.appendChild(intentBox); root.appendChild(syncActions); root.appendChild(list); root.appendChild(context); root.appendChild(diagnostics);
  var lastReasonCode = null;

  function renderError(model, busy) {
    while (errorBox.firstChild) errorBox.removeChild(errorBox.firstChild);
    errorBox.hidden = !model.reasonCode;
    if (!model.reasonCode) { errorBox.removeAttribute('role'); lastReasonCode = null; return; }
    if (model.reasonCode !== lastReasonCode) errorBox.setAttribute('role', 'alert');
    else errorBox.removeAttribute('role');
    lastReasonCode = model.reasonCode;
    var view = presentFigmaError(model.reasonCode);
    errorBox.appendChild(el('div', null, [el('strong', { text: t(view.titleKey) }), el('p', { text: t(view.bodyKey) })]));
    if (view.action) {
      var action = button(view.actionKey, 'btn btn--small', function () { handlers.recover(view.action); });
      action.disabled = !!busy || !recoveryActionAllowed(model, view.action);
      errorBox.appendChild(action);
    }
  }
  function renderIntent(model, scope) {
    while (intentBox.firstChild) intentBox.removeChild(intentBox.firstChild);
    intentBox.hidden = !scope;
    if (!scope) return;
    var gate = scope === 'drift' ? model.compareGate || {} : model.syncGate || {};
    var reasonKey = 'figma.syncIntent.blocked.' + String(gate.reasonCode || 'unknown');
    var reason = t(reasonKey);
    if (reason === reasonKey) reason = t('figma.syncIntent.blocked.unknown');
    intentBox.appendChild(el('strong', { text: t('figma.syncIntent.title.' + scope) }));
    intentBox.appendChild(el('p', { text: reason }));
  }
  function update(model, busy, intentScope) {
    model = model || {};
    badge.className = 'figma-integration-badge figma-integration-badge--' + (model.status || 'unavailable');
    badge.textContent = figmaEnumText('integrationStatus', model.status || 'unavailable');
    test.disabled = busy || !model.actions || !model.actions.canTest;
    test.textContent = model.test && model.test.state === 'running'
      ? figmaEnumText('testPhase', model.test.phase) : t('figma.action.test');
    clear.hidden = !(model.context && model.context.resetAvailable);
    clear.disabled = busy || !model.actions || !model.actions.canClearIntegration;
    Object.keys(syncButtons).forEach(function (scope) {
      var allowed = scope === 'drift' ? model.actions && model.actions.canCompare : model.actions && model.actions.canSync;
      syncButtons[scope].disabled = busy || !allowed;
    });
    renderError(model, busy);
    renderIntent(model, intentScope);
    while (list.firstChild) list.removeChild(list.firstChild);
    var account = model.account || {}, file = model.projectFile || {}, access = model.access || {}, quota = model.quota || {}, sync = model.sync || {};
    var accountAction = button(account.state === 'connected' ? 'figma.action.change' : 'figma.action.reconnect', 'btn btn--ghost btn--small', handlers.account);
    accountAction.disabled = !!busy || !model.actions || model.actions.canChangeAccount === false;
    list.appendChild(readinessRow('figma.row.account', stateText('account', account.state || 'unknown'),
      el('span', { class: 'figma-integration-secondary', text: [account.displayName, account.email].filter(Boolean).join(' · ') }),
      accountAction));
    var fileAction = button(file.state === 'selected' ? 'figma.action.change' : 'figma.action.selectFile', 'btn btn--ghost btn--small', handlers.file);
    fileAction.disabled = !!busy || !model.actions || !model.actions.canChangeFile;
    list.appendChild(readinessRow('figma.row.file', stateText('file', file.state || 'missing'),
      el('span', { class: 'figma-integration-secondary', text: file.name ? file.name + (file.key ? ' · ' + file.key : '') : file.key || '' }),
      fileAction));
    var accessAction = access.state === 'verified' ? null : button('figma.action.test', 'btn btn--ghost btn--small', handlers.test);
    if (accessAction) accessAction.disabled = !!busy || !model.actions || !model.actions.canTest;
    list.appendChild(readinessRow('figma.row.access', stateText('access', access.state || 'unknown'),
      el('time', { class: 'figma-integration-secondary', text: access.checkedAt ? date(access.checkedAt) : '', attrs: { datetime: access.checkedAt || '', title: access.checkedAt || '' } }),
      accessAction));
    var testAttempt = model.test;
    if (testAttempt) {
      var testDetail = testAttempt.state === 'running'
        ? figmaEnumText('testPhase', testAttempt.phase)
        : testAttempt.state === 'failed'
          ? t(presentFigmaError(testAttempt.reasonCode).bodyKey)
          : testAttempt.finishedAt ? date(testAttempt.finishedAt) : '';
      list.appendChild(readinessRow('figma.row.lastTest',
        el('span', {
          class: 'figma-integration-state figma-integration-state--' + (testAttempt.state || 'unknown'),
          text: figmaEnumText('testState', testAttempt.state)
        }),
        el('span', { class: 'figma-integration-secondary', text: testDetail }),
        null));
    }
    var lastSyncAction = button('figma.action.history', 'btn btn--ghost btn--small', handlers.history);
    lastSyncAction.disabled = !!busy;
    var lastResult = sync.lastResult;
    var lastResultState = lastResult && lastResult.result || null;
    var lastResultAt = lastResult && (lastResult.finishedAt || lastResult.startedAt);
    var lastSyncState = lastResult
      ? el('span', {
        class: 'figma-integration-state figma-integration-state--' + lastResultState,
        text: figmaEnumText('historyResult', lastResultState) + (lastResultAt ? ' · ' + date(lastResultAt) : '')
      })
      : el('span', { class: 'figma-integration-state', text: t('figma.value.never') });
    var lastSyncSecondary = lastResult && (lastResultState === 'failed' || lastResultState === 'interrupted')
      ? syncFailureText(lastResult.errorCode)
      : lastResult ? t('figma.updated', { updated: lastResult.updated, unchanged: lastResult.unchanged }) : '';
    list.appendChild(readinessRow('figma.row.lastSync', lastSyncState,
      el('span', { class: 'figma-integration-secondary', text: lastSyncSecondary }),
      lastSyncAction));
    if (quota.state === 'warning' || quota.state === 'blocked') {
      list.appendChild(readinessRow('figma.row.quota', stateText('quota', quota.state),
        el('span', { class: 'figma-integration-secondary', text: t('figma.quota.help') }),
        button('figma.action.details', 'btn btn--ghost btn--small', function () { diagnostics.open = true; diagnostics.querySelector('summary').focus(); })));
    }
    while (context.firstChild) context.removeChild(context.firstChild);
    var contextSync = model.context && model.context.lastSuccessfulSync;
    context.appendChild(el('div', null, [el('span', { text: t('figma.context.lastSuccess') }), contextSync
      ? el('time', { text: date(contextSync), attrs: { datetime: contextSync, title: contextSync } })
      : el('strong', { text: t('figma.value.never') })]));
    context.appendChild(el('div', null, [el('span', { text: t('figma.context.updated') }), el('strong', { text: String(model.context && model.context.updatedArtifacts || 0) })]));
    context.appendChild(button('figma.action.history', 'btn btn--ghost', handlers.history));
    context.appendChild(el('a', { class: 'btn btn--ghost', href: '#design', text: t('figma.action.openDesign') }));
    while (diagnostics.childNodes.length > 1) diagnostics.removeChild(diagnostics.lastChild);
    var diag = model.diagnostics || {};
    diagnostics.appendChild(el('dl', { class: 'figma-integration-diagnostic-list' }, Object.keys(diag).filter(function (key) {
      return diag[key] != null && Object.prototype.hasOwnProperty.call(DIAGNOSTIC_LABEL_KEYS, key);
    }).map(function (key) {
      return el('div', null, [
        el('dt', { text: t(DIAGNOSTIC_LABEL_KEYS[key]) }),
        el('dd', { text: diagnosticValue(key, diag[key]) })
      ]);
    })));
  }
  return {
    el: root,
    update: update,
    focusPrimary: function () {
      if (title && typeof title.focus === 'function') title.focus();
    },
    focusSyncActions: function (scope) {
      var target = syncButtons[scope] || syncButtons.tokens;
      if (target && !target.disabled && typeof target.focus === 'function') target.focus();
      else if (syncActionsTitle && typeof syncActionsTitle.focus === 'function') syncActionsTitle.focus();
    }
  };
}
