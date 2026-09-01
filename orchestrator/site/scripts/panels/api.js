import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { clipboard } from '../clipboard.js';
import { requestJson, errorCode } from '../data/request-json.js';
import { apiOverview } from '../api/overview.js';
import { apiEndpoints } from '../api/endpoints.js';
import { apiChanges } from '../api/changes.js';
import { apiDiagnostics } from '../api/diagnostics.js';
import { apiEndpointDetail } from '../api/endpoint-detail.js';
import { apiTaskSelection } from '../api/task-selection.js';
import { siteEvents } from '../event-stream.js';

var el = dom.el;
var rootEl = null;
var shell = null;
var tabbar = null;
var toolbar = null;
var content = null;
var batchHost = null;
var state = null;
var generationKey = null;
var currentMeta = null;
var selected = new Set();
var eventSubscriptions = null;
var eventTimer = null;
var automaticRefreshPaused = false;
var renderSignature = Object.create(null);
var renderedTab = null;
var TABS = ['overview', 'endpoints', 'changes', 'diagnostics'];
var RENDERERS = {
  overview: apiOverview,
  endpoints: apiEndpoints,
  changes: apiChanges,
  diagnostics: apiDiagnostics
};

export function createApiRenderGeneration() {
  var requested = 0;
  var rendered = null;
  return {
    begin: function (preserveRendered) {
      requested++;
      if (!preserveRendered) rendered = null;
      return requested;
    },
    isLatest: function (version) { return version === requested; },
    commit: function (version) {
      if (version !== requested) return false;
      rendered = version;
      return true;
    },
    isCurrent: function (version) { return version === undefined || version === rendered; },
    invalidateRendered: function () { rendered = null; }
  };
}
var renderGeneration = createApiRenderGeneration();

export function createApiRefreshCoordinator() {
  var active = null;
  var pending = false;
  return {
    begin: function (request) { active = request; },
    queue: function () {
      if (!active) return true;
      pending = true;
      return false;
    },
    settle: function (request, succeeded) {
      if (active !== request) return false;
      active = null;
      var queued = pending;
      pending = false;
      return succeeded === true && queued;
    },
    reset: function () { active = null; pending = false; }
  };
}
var refreshCoordinator = createApiRefreshCoordinator();

function t(key, params) { return i18n.t(key, params); }
function bounded(value, max) {
  return Array.from(String(value || '').normalize('NFC')
    .replace(/[\u0000-\u001f\u007f]/g, '')).slice(0, max).join('');
}
function boundedUtf8(value, maxBytes) {
  var encoder = new TextEncoder();
  var output = '';
  var bytes = 0;
  Array.from(String(value || '').normalize('NFC')
    .replace(/[\u0000-\u001f\u007f]/g, '')).some(function (character) {
    var size = encoder.encode(character).length;
    if (bytes + size > maxBytes) return true;
    bytes += size;
    output += character;
    return false;
  });
  return output;
}
function enumValue(value, values) { return values.indexOf(value) >= 0 ? value : ''; }
function readState() {
  var raw = (location.hash || '#api').replace(/^#/, '');
  raw = raw.indexOf('api?') === 0 ? raw.slice(4) : '';
  var params = new URLSearchParams(raw);
  var tab = params.get('tab') || 'overview';
  if (TABS.indexOf(tab) < 0) tab = 'overview';
  return {
    tab: tab,
    query: boundedUtf8(params.get('query'), 200),
    area: /^[a-z0-9][a-z0-9-]{0,99}$/.test(params.get('area') || '') ? params.get('area') : '',
    method: enumValue(params.get('method') || '', ['GET', 'POST', 'PUT', 'PATCH', 'DELETE']),
    implementation: enumValue(params.get('implementation') || '', ['implemented', 'partial', 'unknown']),
    auth: bounded(params.get('auth'), 100),
    hasTask: enumValue(params.get('hasTask') || '', ['yes', 'no']),
    changeSeverity: enumValue(params.get('changeSeverity') || '', ['breaking', 'potentially-breaking', 'compatible', 'info']),
    mismatch: enumValue(params.get('mismatch') || '', ['present', 'none', 'not-checked']),
    consumers: enumValue(params.get('consumers') || '', ['yes', 'no', 'unknown']),
    severity: enumValue(params.get('severity') || '', ['attention', 'breaking', 'potentially-breaking', 'compatible', 'info']),
    kind: /^[a-z][a-z0-9-]{0,99}$/.test(params.get('kind') || '') ? params.get('kind') : '',
    operationId: bounded(params.get('operationId'), 200),
    modelId: bounded(params.get('modelId'), 200),
    entity: bounded(params.get('entity'), 200)
  };
}
function writeState(next, replace) {
  var params = new URLSearchParams();
  Object.keys(next).forEach(function (key) {
    var value = next[key];
    if (!value || key === 'tab' && value === 'overview') return;
    params.set(key, value);
  });
  var target = '#api' + (params.toString() ? '?' + params.toString() : '');
  try {
    if (replace) history.replaceState(null, '', target);
    else history.pushState(null, '', target);
  } catch (error) { location.hash = target.slice(1); }
}
function post(url, body) {
  var headers = { Accept: 'application/json', 'content-type': 'application/json' };
  if (window.__ORCHESTRATOR_CSRF__) {
    headers['x-orchestrator-csrf'] = window.__ORCHESTRATOR_CSRF__;
  }
  return requestJson(url, {
    method: 'POST', cache: 'no-store', headers: headers, body: JSON.stringify(body || {})
  });
}
function get(url) {
  return requestJson(url, { cache: 'no-store', timeoutMs: 15000, headers: { Accept: 'application/json' } });
}
export function apiErrorMessage(error) {
  var code = errorCode(error);
  var key = 'api.error.' + code;
  var translated = t(key);
  return translated === key ? t('api.error.unknown') : translated;
}
var LIMITATION_GROUPS = Object.freeze({
  'contract-generation-invalid': 'contract',
  'contract-missing': 'contract',
  'implementation-report-invalid': 'implementation',
  'implementation-report-missing': 'implementation',
  'implementation-report-stale': 'implementation',
  'implementation-analysis-partial': 'implementation',
  'static-implementation-analysis-not-conclusive': 'implementation',
  'test-sources-excluded': 'implementation',
  'consumer-report-invalid': 'consumers',
  'consumer-report-missing': 'consumers',
  'consumer-report-stale': 'consumers',
  'consumer-analysis-partial': 'consumers',
  'static-consumer-analysis-not-conclusive': 'consumers',
  'architecture-map-invalid': 'architecture',
  'architecture-map-missing': 'architecture',
  'drift-report-invalid': 'drift',
  'drift-report-missing': 'drift',
  'drift-report-stale': 'drift',
  'change-report-invalid': 'changes',
  'change-report-missing': 'changes',
  'baseline-established': 'baseline',
  'task-origin-index-partial': 'tasks',
  'task-origin-history-partial': 'tasks',
  'task-index-unavailable': 'tasks',
  'api-change-review-state-invalid': 'reviews'
});
function limitationGroup(value) {
  var code = String(value || '');
  if (LIMITATION_GROUPS[code]) return LIMITATION_GROUPS[code];
  if (code.indexOf('analyzer-') === 0 ||
      code === 'project-input-revision-unavailable') return 'inputs';
  if (/(?:count|byte|size|response|detail|example|facets|impact|log|mismatch|relation|usage|graph|generation|field|candidate)-cap$/.test(code)) {
    return 'truncated';
  }
  return 'unknown';
}
function apiLimitationMessage(value) {
  return t('api.limitation.' + limitationGroup(value));
}
var DRIFT_FINDING_KINDS = Object.assign(Object.create(null), {
  'dto-layer-absent': 1,
  'dto-schema-unmatched': 1,
  'dto-field-unknown': 1,
  'type-mismatch': 1,
  'server-field-missing-in-dto': 1,
  'endpoint-missing-server-side': 1,
  'nullability-mismatch': 1,
  'enum-new-value': 1,
  'enum-declared-unobserved': 1
});
function driftFindingKind(value) {
  var kind = String(value || '');
  return Object.prototype.hasOwnProperty.call(DRIFT_FINDING_KINDS, kind) ? kind : 'unknown';
}
export function apiDriftFindingMessage(kind) {
  return t('api.drift.finding.' + driftFindingKind(kind));
}
export function apiDriftFindingSuggestion(kind) {
  return t('api.drift.fix.' + driftFindingKind(kind));
}
export function apiDriftSeverity(value) {
  var severity = String(value || '').toUpperCase();
  return t('api.drift.severity.' + (severity === 'ERROR' ? 'error' :
    severity === 'WARNING' ? 'warning' : severity === 'INFO' ? 'info' : 'unknown'));
}
var CHANGE_KIND_GROUPS = Object.freeze({
  'endpoint-added': 'endpointAdded',
  'endpoint-removed': 'endpointRemoved',
  'endpoint-route-changed': 'route',
  'endpoint-route-changed-with-compatible-alias': 'route',
  'operation-id-changed': 'operationId',
  'auth-requirement-changed': 'auth',
  'request-required-parameter-added': 'parameter',
  'request-optional-parameter-added': 'parameter',
  'request-parameter-narrowed': 'parameter',
  'request-parameter-compatible': 'parameter',
  'request-parameter-removed': 'parameter',
  'request-body-added': 'requestBody',
  'request-body-removed': 'requestBody',
  'request-body-content-type-changed': 'requestBody',
  'status-code-set-changed': 'status',
  'response-schema-reference-changed': 'responseShape',
  'response-shape-changed': 'responseShape',
  'request-model-reference-changed': 'model',
  'request-model-changed': 'model',
  'response-model-changed': 'model',
  'request-array-item-reference-changed': 'model',
  'response-array-item-reference-changed': 'model',
  'request-field-removed': 'field',
  'response-field-removed': 'field',
  'request-field-type-changed': 'field',
  'response-field-type-changed': 'field',
  'request-field-type-compatible': 'field',
  'response-field-type-compatible': 'field',
  'request-nullability-narrowed': 'nullability',
  'response-nullability-widened': 'nullability',
  'request-nullability-compatible': 'nullability',
  'response-nullability-compatible': 'nullability',
  'request-enum-changed': 'enum',
  'request-enum-narrowed': 'enum',
  'request-enum-widened': 'enum',
  'response-enum-changed': 'enum',
  'response-enum-narrowed': 'enum',
  'response-enum-widened': 'enum',
  'request-constraints-changed': 'constraints',
  'request-constraints-narrowed': 'constraints',
  'request-constraints-widened': 'constraints',
  'response-constraints-changed': 'constraints',
  'response-constraints-narrowed': 'constraints',
  'response-constraints-widened': 'constraints',
  'request-format-changed': 'format',
  'response-format-changed': 'format',
  'request-field-became-required': 'requiredness',
  'response-field-became-optional': 'requiredness',
  'request-requiredness-compatible': 'requiredness',
  'response-requiredness-compatible': 'requiredness',
  'request-required-field-added': 'requiredness',
  'response-required-field-added': 'requiredness',
  'request-optional-field-added': 'field',
  'response-optional-field-added': 'field',
  'documentation-changed': 'documentation'
});
function apiChangeKindMessage(value) {
  return t('api.changeKind.' + (CHANGE_KIND_GROUPS[String(value || '')] || 'unknown'));
}
function apiConfidenceMessage(value) {
  var confidence = ['exact', 'derived', 'heuristic'].indexOf(value) >= 0
    ? value : 'unknown';
  return t('api.confidence.' + confidence);
}
function payloadKey(payload) {
  return payload && payload.committedGenerationId
    ? [
      payload.committedGenerationId,
      payload.contractHash,
      payload.projectCodeRevision,
      payload.taskIndexRevision,
      payload.reportHashes && payload.reportHashes.implementation,
      payload.reportHashes && payload.reportHashes.consumers,
      payload.reportHashes && payload.reportHashes.drift,
      payload.reportHashes && payload.reportHashes.changes
    ].join('|') : null;
}
function adopt(payload) {
  var next = payloadKey(payload);
  if (generationKey && generationKey !== next && selected.size) {
    selected.clear();
    // A background refresh can destroy a hand-built selection while the user
    // is scrolling or on another tab, where the toast is never seen. Record
    // the loss in the batch box too, so it is still explained afterwards.
    apiTaskSelection.invalidate();
    // Not a failure — creating tasks bumps the generation itself, so an
    // assertive red toast would report the user's own successful action as an
    // error and clobber its summary. The batch box keeps the durable record.
    clipboard.toast(t('api.selection.invalidated'));
    apiTaskSelection.render(batchHost, context());
  }
  generationKey = next;
  currentMeta = payload && payload.committedGenerationId ? {
    committedGenerationId: payload.committedGenerationId,
    contractHash: payload.contractHash,
    environmentId: payload.environmentId,
    projectCodeRevision: payload.projectCodeRevision,
    taskIndexRevision: payload.taskIndexRevision,
    reportHashes: payload.reportHashes || {}
  } : null;
}
function changeState(patch, replace) {
  var previousTab = state.tab;
  state = Object.assign({}, state, patch || {});
  if (patch && patch.tab && patch.tab !== previousTab) {
    if (!Object.prototype.hasOwnProperty.call(patch, 'entity')) state.entity = '';
    if (!Object.prototype.hasOwnProperty.call(patch, 'operationId')) state.operationId = '';
  }
  writeState(state, replace !== false);
  automaticRefreshPaused = false;
  var tabChanged = state.tab !== previousTab;
  if (tabChanged) renderTabs();
  render({ background: !tabChanged, force: true });
}
function randomKey(prefix) {
  var bytes = new Uint8Array(16);
  window.crypto.getRandomValues(bytes);
  return prefix + Array.from(bytes, function (value) {
    return value.toString(16).padStart(2, '0');
  }).join('');
}
function context(version) {
  return {
    el: el,
    t: t,
    get: get,
    post: post,
    errorMessage: apiErrorMessage,
    limitationMessage: apiLimitationMessage,
    driftFindingMessage: apiDriftFindingMessage,
    driftFindingSuggestion: apiDriftFindingSuggestion,
    driftSeverity: apiDriftSeverity,
    changeKindMessage: apiChangeKindMessage,
    confidenceMessage: apiConfidenceMessage,
    boundedQuery: boundedUtf8,
    toastError: function (error) { clipboard.toastError(apiErrorMessage(error)); },
    state: state,
    meta: currentMeta,
    isCurrent: function () { return renderGeneration.isCurrent(version); },
    adopt: adopt,
    setState: changeState,
    selected: selected,
    randomKey: randomKey,
    refresh: function () { return render({ background: true, force: true }); },
    toggleSource: function (sourceId, checked) {
      if (checked) selected.add(sourceId);
      else selected.delete(sourceId);
      apiTaskSelection.render(batchHost, context());
      return true;
    },
    addSources: function (sourceIds) {
      var before = selected.size;
      (sourceIds || []).forEach(function (sourceId) { selected.add(sourceId); });
      apiTaskSelection.render(batchHost, context());
      return selected.size - before;
    },
    toast: function (key, params) { clipboard.toast(t(key, params)); },
    openEndpoint: function (operationId, trigger) {
      state = Object.assign({}, state, { entity: operationId });
      writeState(state, true);
      apiEndpointDetail.open(context(), operationId, trigger, function () {
        state = Object.assign({}, state, { entity: '' });
        writeState(state, true);
      });
    }
  };
}
function renderTabs() {
  if (!tabbar) return;
  tabbar.replaceChildren();
  TABS.forEach(function (id, index) {
    var button = el('button', {
      type: 'button',
      id: 'api-tab-' + id,
      class: 'api-tab' + (state.tab === id ? ' api-tab--active' : ''),
      text: t('api.tab.' + id),
      attrs: {
        role: 'tab',
        'aria-selected': state.tab === id ? 'true' : 'false',
        'aria-controls': 'api-panel-main',
        tabindex: state.tab === id ? '0' : '-1'
      }
    });
    button.addEventListener('click', function () {
      if (state.tab !== id) changeState({ tab: id }, false);
    });
    button.addEventListener('keydown', function (event) {
      if (['ArrowLeft', 'ArrowRight', 'Home', 'End'].indexOf(event.key) < 0) return;
      event.preventDefault();
      var next = event.key === 'Home' ? 0 : event.key === 'End' ? TABS.length - 1 :
        (index + (event.key === 'ArrowRight' ? 1 : -1) + TABS.length) % TABS.length;
      changeState({ tab: TABS[next] }, false);
      setTimeout(function () {
        var target = document.getElementById('api-tab-' + TABS[next]);
        if (target) target.focus();
      }, 0);
    });
    tabbar.appendChild(button);
  });
  content.setAttribute('aria-labelledby', 'api-tab-' + state.tab);
}
function captureInteraction() {
  var active = document.activeElement;
  var ownsFocus = active && shell && shell.contains(active);
  var editable = ownsFocus && /^(INPUT|SELECT|TEXTAREA)$/.test(active.tagName || '');
  return {
    focus: ownsFocus ? active.getAttribute('data-api-focus') : null,
    draft: editable ? active.value : null,
    selectionStart: editable && typeof active.selectionStart === 'number'
      ? active.selectionStart : null,
    selectionEnd: editable && typeof active.selectionEnd === 'number'
      ? active.selectionEnd : null,
    top: content.scrollTop,
    x: window.scrollX,
    y: window.scrollY
  };
}
function restoreInteraction(snapshot) {
  if (!snapshot) return;
  if (snapshot.focus) {
    var nodes = shell.querySelectorAll('[data-api-focus]');
    for (var index = 0; index < nodes.length; index++) {
      if (nodes[index].getAttribute('data-api-focus') === snapshot.focus) {
        var replacement = nodes[index];
        var draftChanged = false;
        if (snapshot.draft !== null &&
            /^(INPUT|SELECT|TEXTAREA)$/.test(replacement.tagName || '')) {
          draftChanged = replacement.value !== snapshot.draft;
          replacement.value = snapshot.draft;
        }
        try { replacement.focus({ preventScroll: true }); }
        catch (error) { replacement.focus(); }
        if (snapshot.selectionStart !== null &&
            typeof replacement.setSelectionRange === 'function') {
          replacement.setSelectionRange(snapshot.selectionStart, snapshot.selectionEnd);
        }
        // Re-arm the replacement input's debounce when the user typed ahead
        // of the response. The detached input's pending timer intentionally
        // refuses to write state, so without this event the visible draft
        // could otherwise differ from the applied filters indefinitely.
        if (draftChanged) replacement.dispatchEvent(new Event('input', {
          bubbles: true
        }));
        break;
      }
    }
  }
  content.scrollTop = snapshot.top;
  window.scrollTo(snapshot.x, snapshot.y);
}
function clearRefreshNotice() {
  if (!content) return;
  var notice = content.querySelector('[data-api-refresh-notice]');
  if (notice) notice.remove();
}
// A paused background refresh is invisible from the preserved content alone,
// so it carries the same message and retry control the foreground branch
// renders. It sits above the content it stopped updating and is replaced,
// never stacked, when the next background attempt fails the same way.
function showRefreshNotice(error) {
  clearRefreshNotice();
  var box = el('div', {
    class: 'agent-cards-status agent-cards-status--error',
    attrs: { 'data-api-refresh-notice': '1' }
  });
  box.appendChild(el('p', { class: 'panel-lead', text: apiErrorMessage(error) }));
  var retry = el('button', { type: 'button', class: 'btn', text: t('api.retry') });
  retry.addEventListener('click', function () {
    automaticRefreshPaused = false;
    render({ force: true });
  });
  box.appendChild(retry);
  content.insertBefore(box, content.firstChild);
}
function settleRender(request, succeeded) {
  if (!refreshCoordinator.settle(request, succeeded)) return;
  clearTimeout(eventTimer);
  eventTimer = setTimeout(function () {
    if (rootEl && !rootEl.hidden) render({ background: true });
  }, 0);
}
function render(options) {
  options = options || {};
  var tab = state.tab;
  // A queued SSE refresh may overtake the foreground request that installed
  // the loading placeholder. It is a background refresh only when this tab
  // has actually committed content in the current DOM; a cached signature
  // alone is not proof that there is anything usable to preserve.
  var background = options.background === true && renderedTab === tab;
  var version = renderGeneration.begin(background);
  if (!background) {
    renderedTab = null;
    toolbar.replaceChildren();
    content.replaceChildren(el('p', { class: 'api-state', text: t('api.loading') }));
  }
  content.setAttribute('aria-busy', 'true');
  var renderer = RENDERERS[tab] || apiOverview;
  var succeeded = false;
  var request = Promise.resolve().then(function () {
    return renderer.load(context(version));
  }).then(function (payload) {
    if (!renderGeneration.isLatest(version)) return;
    adopt(payload);
    var signature = JSON.stringify(payload);
    if (!options.force && background && renderSignature[tab] === signature) {
      content.setAttribute('aria-busy', 'false');
      automaticRefreshPaused = false;
      clearRefreshNotice();
      succeeded = true;
      return;
    }
    renderSignature[tab] = signature;
    if (!renderGeneration.commit(version)) return;
    // Capture immediately before replacement: the user may keep typing while
    // the background request is in flight, so a request-start snapshot is
    // already stale by the time the response arrives. restoreInteraction also
    // replays a changed draft into the replacement input's debounce.
    var interaction = background ? captureInteraction() : null;
    renderer.render(context(version), payload, toolbar, content);
    renderedTab = tab;
    content.setAttribute('aria-busy', 'false');
    apiTaskSelection.render(batchHost, context());
    restoreInteraction(interaction);
    if (state.entity && state.tab === 'endpoints') {
      var entity = state.entity;
      state.entity = '';
      apiEndpointDetail.open(context(), entity, null, function () {
        state = Object.assign({}, state, { entity: '' });
        writeState(state, true);
      });
    }
    automaticRefreshPaused = false;
    clearRefreshNotice();
    succeeded = true;
  }).catch(function (error) {
    if (!renderGeneration.isLatest(version)) return;
    if (background && renderedTab === tab) {
      content.setAttribute('aria-busy', 'false');
      // The notice below is persistent and sits in the panel the user is looking
      // at; a toast for a background event would only duplicate it.
      // Left running, every later SSE event re-fires the same failing request
      // and re-toasts it. Pausing is only honest with a visible way back, so
      // the preserved content keeps the foreground branch's retry control.
      automaticRefreshPaused = true;
      showRefreshNotice(error);
      return;
    }
    renderGeneration.invalidateRendered();
    renderedTab = null;
    automaticRefreshPaused = true;
    toolbar.replaceChildren();
    content.replaceChildren();
    var box = el('div', { class: 'agent-cards-status agent-cards-status--error' });
    box.appendChild(el('p', { class: 'panel-lead', text: apiErrorMessage(error) }));
    var retry = el('button', { type: 'button', class: 'btn', text: t('api.retry') });
    retry.addEventListener('click', function () {
      automaticRefreshPaused = false;
      render({ force: true });
    });
    box.appendChild(retry);
    content.appendChild(box);
    content.setAttribute('aria-busy', 'false');
  });
  refreshCoordinator.begin(request);
  return request.then(function (value) {
    settleRender(request, succeeded);
    return value;
  });
}
function build() {
  if (!rootEl) return;
  state = readState();
  renderedTab = null;
  automaticRefreshPaused = false;
  refreshCoordinator.reset();
  renderGeneration.invalidateRendered();
  renderSignature = Object.create(null);
  rootEl.replaceChildren();
  shell = el('div', { class: 'api-workspace' });
  var head = el('div', { class: 'api-workspace__head' });
  var title = el('div');
  title.appendChild(el('h2', { class: 'panel-title', text: t('api.title') }));
  title.appendChild(el('p', { class: 'panel-lead', text: t('api.lead') }));
  head.appendChild(title);
  shell.appendChild(head);
  tabbar = el('div', {
    class: 'api-tabs api-tabs--stable',
    attrs: { role: 'tablist', 'aria-label': t('api.tabsAria') }
  });
  shell.appendChild(tabbar);
  toolbar = el('div', { class: 'api-toolbar' });
  shell.appendChild(toolbar);
  content = el('div', {
    id: 'api-panel-main',
    class: 'api-tabpanel',
    attrs: { role: 'tabpanel', tabindex: '0', 'aria-live': 'polite' }
  });
  shell.appendChild(content);
  batchHost = el('div', { class: 'api-batch-host', attrs: { 'aria-live': 'polite' } });
  shell.appendChild(batchHost);
  rootEl.appendChild(shell);
  rootEl.appendChild(apiEndpointDetail.element());
  renderTabs();
  render();
}
function connectEvents() {
  if (eventSubscriptions) return;
  function queue() {
    clearTimeout(eventTimer);
    eventTimer = setTimeout(function () {
      if (!rootEl || rootEl.hidden) return;
      if (automaticRefreshPaused) return;
      if (!refreshCoordinator.queue()) return;
      render({ background: true });
    }, 150);
  }
  eventSubscriptions = [
    siteEvents.on('api-overview', queue),
    siteEvents.on('api-mock', queue),
    siteEvents.on('change', queue)
  ];
}

export const api = {
  mount: function (node) {
    rootEl = node;
    build();
    connectEvents();
  },
  refresh: function () {
    automaticRefreshPaused = false;
    var next = readState();
    var tabChanged = !state || next.tab !== state.tab;
    state = next;
    if (tabChanged) renderTabs();
    render({ background: !tabChanged, force: tabChanged });
  },
  openTarget: function (entityId) {
    if (typeof entityId !== 'string' || !entityId) return;
    if (entityId.indexOf('api:package:') === 0) {
      changeState({ tab: 'overview', entity: '' }, false);
    } else if (entityId.indexOf('api:change:') === 0) {
      changeState({ tab: 'changes', query: entityId.slice('api:change:'.length) }, false);
    } else if (entityId.indexOf('api:mismatch:') === 0) {
      changeState({ tab: 'diagnostics', query: entityId }, false);
    } else {
      changeState({ tab: 'endpoints', entity: entityId }, false);
    }
  }
};
