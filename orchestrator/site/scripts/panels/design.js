import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { store } from '../store.js';
import { clipboard } from '../clipboard.js';
import { designFilters } from '../design/filters.js';
import { designOverview } from '../design/overview.js';
import { designTokens } from '../design/tokens.js';
import { designComponents } from '../design/components.js';
import { designSurfaces } from '../design/surfaces.js';
import { designEntityDrawer } from '../design/entity-drawer.js';
import { siteEvents } from '../event-stream.js';

var el = dom.el;
function t(key, params) { return i18n.t(key, params); }

var rootEl = null;
var shell = null;
var titleEl = null;
var leadEl = null;
var tabbar = null;
var toolbar = null;
var content = null;
var batchStatus = null;
var state = null;
var generationRevision = null;
var unsubscribe = null;
var eventSubscriptions = null;
var backgroundRefreshRunning = false;
var backgroundRefreshQueued = false;
var comparisonStartPending = false;
// Mapping-review ops baskets. They live here (panel state, not DOM) so a
// background poll or a drawer re-render never drops drafted operations.
// Tokens and components each keep their own basket and apply endpoint.
var tokenOps = [];
var tokenOpsApplying = false;
var tokenComparisonHash = null;
var componentOps = [];
var componentOpsApplying = false;
var componentComparisonHash = null;
var renderSignatures = Object.create(null);
var selections = { tokens: new Set(), components: new Set(), surfaces: new Set() };
var TABS = ['overview', 'tokens', 'components', 'surfaces'];
var renderers = Object.assign(Object.create(null), {
  overview: designOverview,
  tokens: designTokens,
  components: designComponents,
  surfaces: designSurfaces
});

export function createDesignRenderGeneration() {
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
    isCurrent: function (version) {
      return version === requested || version === rendered;
    }
  };
}
var renderGeneration = createDesignRenderGeneration();

function newMappingOperationId() {
  var bytes = new Uint8Array(16);
  window.crypto.getRandomValues(bytes);
  return 'mop-' + Array.from(bytes, function (value) { return value.toString(16).padStart(2, '0'); }).join('');
}

function build() {
  if (!rootEl) return;
  rootEl.replaceChildren();
  state = designFilters.readState();
  shell = el('div', { class: 'design-workspace' });
  var header = el('div', { class: 'design-workspace__head' });
  var titleBox = el('div');
  titleEl = el('h2', { class: 'panel-title', text: t('design.title') });
  leadEl = el('p', { class: 'panel-lead', text: t('design.lead') });
  titleBox.appendChild(titleEl);
  titleBox.appendChild(leadEl);
  header.appendChild(titleBox);
  shell.appendChild(header);

  tabbar = el('div', { class: 'design-tabs design-tabs--stable', attrs: { role: 'tablist', 'aria-label': t('design.tabsAria') } });
  shell.appendChild(tabbar);
  toolbar = el('div', { class: 'design-toolbar' });
  shell.appendChild(toolbar);
  content = el('div', {
    class: 'design-tabpanel',
    attrs: { role: 'tabpanel', tabindex: '0', 'aria-live': 'polite' }
  });
  shell.appendChild(content);
  batchStatus = el('div', { class: 'design-batch-status', attrs: { 'aria-live': 'polite' } });
  shell.appendChild(batchStatus);
  rootEl.appendChild(shell);
  rootEl.appendChild(designEntityDrawer.element());
  renderTabs();
  render();
}

function changeState(patch, replace) {
  var previousTab = state.tab;
  var previousScope = state.scope;
  state = Object.assign({}, state, patch || {});
  var tabChanged = !!(patch && patch.tab && patch.tab !== previousTab);
  // The tokens sub-view (design vs project-only) exposes a different filter
  // set, so a scope flip must rebuild the toolbar instead of preserving it.
  var scopeChanged = !!(patch && 'scope' in patch && patch.scope !== previousScope);
  if (tabChanged) {
    designEntityDrawer.close(true);
    state.type = '';
    state.changedSide = '';
    state.mappingState = '';
    state.scope = '';
    if (!selections[patch.tab]) selections[patch.tab] = new Set();
  }
  if (scopeChanged) {
    // The batch bar counts selections per tab, not per scope: without this the
    // Sources view kept offering "3 selected · Create tasks" for findings that
    // exist in no view on screen.
    if (selections[state.tab]) selections[state.tab].clear();
    state.type = '';
    state.status = '';
    state.changed = '';
    state.changedSide = '';
    state.hasTask = '';
    state.mappingState = '';
  }
  designFilters.writeState(state, replace !== false);
  if (tabChanged) renderTabs();
  // Filter controls already contain the user's newest value. Keep that exact
  // subtree mounted while the matching result set reloads; replacing it here
  // would discard focus, selection/caret and IME composition after debounce.
  render({ preserveToolbar: !tabChanged && !scopeChanged });
}
function writeEntityState(entityType, id) {
  state = Object.assign({}, state, { entityType: entityType, entity: id });
  designFilters.writeState(state, true);
}
function renderTabs() {
  tabbar.replaceChildren();
  TABS.forEach(function (id, index) {
    var button = el('button', {
      type: 'button',
      id: 'design-tab-' + id,
      class: 'design-tab' + (id === state.tab ? ' design-tab--active' : ''),
      text: t('design.tab.' + id),
      attrs: {
        role: 'tab', 'aria-selected': id === state.tab ? 'true' : 'false',
        'aria-controls': 'design-panel-main', tabindex: id === state.tab ? '0' : '-1'
      }
    });
    button.addEventListener('click', function () {
      if (state.tab !== id) changeState({ tab: id, entity: '', entityType: '' }, false);
    });
    button.addEventListener('keydown', function (event) {
      if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft' && event.key !== 'Home' && event.key !== 'End') return;
      event.preventDefault();
      var next = event.key === 'Home' ? 0 : event.key === 'End' ? TABS.length - 1 :
        (index + (event.key === 'ArrowRight' ? 1 : -1) + TABS.length) % TABS.length;
      changeState({ tab: TABS[next], entity: '', entityType: '' }, false);
      setTimeout(function () {
        var hit = document.getElementById('design-tab-' + TABS[next]);
        if (hit) hit.focus();
      }, 0);
    });
    tabbar.appendChild(button);
  });
  content.id = 'design-panel-main';
  content.setAttribute('aria-labelledby', 'design-tab-' + state.tab);
}
function adopt(payload) {
  if (payload && payload.generationRevision) {
    if (generationRevision && generationRevision !== payload.generationRevision) {
      Object.keys(selections).forEach(function (key) { selections[key].clear(); });
    }
    generationRevision = payload.generationRevision;
  }
}
function payloadSignature(tab, payload) {
  var copy;
  try { copy = JSON.parse(JSON.stringify(payload)); }
  catch (error) { return ''; }
  // The server returns exact ageMs for diagnostics, but the Design overview
  // renders the stable sync timestamp. Do not rebuild the whole panel merely
  // because wall-clock age advanced between two otherwise identical reads.
  if (tab === 'overview' && copy && copy.freshness) delete copy.freshness.ageMs;
  return JSON.stringify(copy);
}
function captureInteraction() {
  var active = document.activeElement;
  return {
    focusKey: active && content.contains(active)
      ? active.getAttribute('data-design-focus') : null,
    contentTop: content.scrollTop,
    contentLeft: content.scrollLeft,
    windowX: typeof window.scrollX === 'number' ? window.scrollX : 0,
    windowY: typeof window.scrollY === 'number' ? window.scrollY : 0
  };
}
function restoreInteraction(snapshot) {
  if (!snapshot) return;
  if (snapshot.focusKey) {
    var candidates = content.querySelectorAll('[data-design-focus]');
    for (var index = 0; index < candidates.length; index++) {
      if (candidates[index].getAttribute('data-design-focus') === snapshot.focusKey) {
        try { candidates[index].focus({ preventScroll: true }); }
        catch (error) { candidates[index].focus(); }
        break;
      }
    }
  }
  content.scrollTop = snapshot.contentTop;
  content.scrollLeft = snapshot.contentLeft;
  if (typeof window.scrollTo === 'function') window.scrollTo(snapshot.windowX, snapshot.windowY);
}
function commonContext(version, forceRender, beforeRender) {
  var committed = false;
  return {
    state: state,
    isCurrent: function () { return renderGeneration.isCurrent(version); },
    adopt: adopt,
    shouldRender: function (payload) {
      var signature = payloadSignature(state.tab, payload);
      if (!forceRender && signature && renderSignatures[state.tab] === signature) return false;
      if (!renderGeneration.commit(version)) return false;
      renderSignatures[state.tab] = signature;
      if (typeof beforeRender === 'function') beforeRender();
      committed = true;
      return true;
    },
    comparisonState: function (value) {
      return comparisonStartPending ? { state: 'running', reasonCode: null, job: null } : value;
    },
    startTokenComparison: startTokenComparison,
    startComponentComparison: startComponentComparison,
    adoptTokenAnalysis: adoptTokenAnalysis,
    adoptComponentAnalysis: adoptComponentAnalysis,
    addComponentOperation: addComponentOperation,
    refresh: function () { return render({ background: true, force: true }); },
    didRender: function () { return committed; },
    setState: changeState,
    selected: selections[state.tab],
    toggleFinding: function (id, checked) {
      var selected = selections[state.tab];
      if (checked) selected.add(id); else selected.delete(id);
      renderBatchBar();
    },
    openEntity: function (entityType, id, trigger) {
      writeEntityState(entityType, id);
      designEntityDrawer.open(entityType, id, generationRevision, trigger, function () {
        writeEntityState('', '');
      });
    }
  };
}
function render(options) {
  options = options || {};
  var background = options.background === true && content.hasChildNodes();
  var preserveToolbar = options.preserveToolbar === true &&
    state.tab !== 'overview' && toolbar.hasChildNodes();
  var version = renderGeneration.begin(background);
  if (!background) {
    if (!preserveToolbar) toolbar.replaceChildren();
    content.replaceChildren(el('p', { class: 'design-state', text: t('design.loading') }));
    batchStatus.textContent = '';
    if (state.tab !== 'overview' && !preserveToolbar) {
      toolbar.appendChild(designFilters.element(state, state.tab, function (patch) { changeState(patch, true); }));
    }
  }
  content.setAttribute('aria-busy', 'true');
  var renderer = Object.prototype.hasOwnProperty.call(renderers, state.tab)
    ? renderers[state.tab] : designOverview;
  var interaction = null;
  var context = commonContext(version, !background || options.force === true, function () {
    // Capture immediately before the renderer replaces content. Capturing at
    // request start would restore stale focus/scroll if the user interacted
    // while the background request was in flight.
    if (background) interaction = captureInteraction();
  });
  return Promise.resolve(renderer.render(content, context)).then(function () {
    if (!context.isCurrent()) return;
    content.removeAttribute('aria-busy');
    if (background && context.didRender()) restoreInteraction(interaction);
    if (!background || context.didRender()) renderBatchBar();
    if (!background && state.entity && state.entityType) {
      designEntityDrawer.open(state.entityType, state.entity, generationRevision, null, function () {
        state = Object.assign({}, state, { entityType: '', entity: '' });
        designFilters.writeState(state, true);
      });
    }
  }, function (error) {
    if (!renderGeneration.isLatest(version)) return;
    content.removeAttribute('aria-busy');
    // A transient background refresh must never erase a usable Design view.
    // Explicit navigation/retry still exposes the normal actionable error.
    if (background) return;
    content.replaceChildren(errorState(error));
  });
}
function startComparison(trigger, domain) {
  if (comparisonStartPending || !generationRevision) return;
  comparisonStartPending = true;
  // Capture BEFORE the busy label overwrites it, or the restore re-applies
  // "Comparing…" to an enabled button.
  var triggerLabel = trigger ? trigger.textContent : null;
  if (trigger) {
    trigger.disabled = true;
    trigger.textContent = t('design.analysis.action.comparing');
  }
  var request = {
    expectedGenerationRevision: generationRevision
  };
  if (domain === 'tokens' || domain === 'components') request.domain = domain;
  function restoreTrigger() {
    if (!trigger || !trigger.isConnected) return;
    trigger.disabled = false;
    if (triggerLabel !== null) trigger.textContent = triggerLabel;
  }
  designFilters.post('/api/design/compare', request).then(function () {
    comparisonStartPending = false;
    restoreTrigger();
    clipboard.toast(t('design.comparisonStarted'));
    return render({ background: true, force: true });
  }).catch(function (error) {
    comparisonStartPending = false;
    restoreTrigger();
    // The comparison did NOT start. On the neutral toast this is announced as a
    // status and styled exactly like "comparison started" one line above it.
    clipboard.toastError(designFilters.errorMessage(error));
    render({ background: true, force: true });
  });
}
function startTokenComparison(trigger) {
  startComparison(trigger, 'tokens');
}
function startComponentComparison(trigger) {
  startComparison(trigger, 'components');
}
function adoptTokenAnalysis(analysis) {
  tokenComparisonHash = analysis && typeof analysis.comparisonSemanticHash === 'string' &&
    analysis.comparisonSemanticHash ? analysis.comparisonSemanticHash : null;
}
function adoptComponentAnalysis(analysis) {
  componentComparisonHash = analysis && typeof analysis.comparisonSemanticHash === 'string' &&
    analysis.comparisonSemanticHash ? analysis.comparisonSemanticHash : null;
}
function addTokenOperation(operation, label) {
  if (!operation || typeof operation !== 'object') return { ok: false, reason: t('design.error.bad-token-mapping-request') };
  if (tokenOps.length >= 20) return { ok: false, reason: t('design.mapping.basketLimit') };
  tokenOps.push({ operation: operation, label: String(label || operation.op) });
  renderTokenOpsBar();
  clipboard.toast(t('design.mapping.opAdded'));
  return { ok: true };
}
function renderTokenOpsBar() {
  if (!shell) return;
  var old = shell.querySelector('.design-ops-bar--tokens');
  if (old) old.remove();
  if (!tokenOps.length) return;
  var bar = el('div', { class: 'design-ops-bar design-ops-bar--tokens' });
  var copy = el('div', { class: 'design-ops-bar__copy' });
  copy.appendChild(el('strong', { text: t('design.mapping.pendingOps', { count: tokenOps.length }) }));
  copy.appendChild(el('span', {
    class: 'design-ops-bar__list',
    text: tokenOps.map(function (row) { return row.label; }).join(' · ')
  }));
  bar.appendChild(copy);
  var discard = el('button', {
    type: 'button', class: 'btn btn--ghost btn--small', text: t('design.mapping.discard'),
    disabled: tokenOpsApplying
  });
  discard.addEventListener('click', function () {
    tokenOps = [];
    renderTokenOpsBar();
  });
  var apply = el('button', {
    type: 'button', class: 'btn btn--small', text: tokenOpsApplying ? t('design.mapping.applying') : t('design.mapping.apply'),
    disabled: tokenOpsApplying
  });
  apply.addEventListener('click', function () { applyTokenOps(tokenOps.slice()); });
  bar.appendChild(discard);
  bar.appendChild(apply);
  shell.insertBefore(bar, batchStatus);
}
function addComponentOperation(operation, label) {
  if (!operation || typeof operation !== 'object') return { ok: false, reason: t('design.error.bad-component-mapping-request') };
  if (componentOps.length >= 20) return { ok: false, reason: t('design.mapping.basketLimit') };
  componentOps.push({ operation: operation, label: String(label || operation.op) });
  renderComponentOpsBar();
  clipboard.toast(t('design.mapping.opAdded'));
  return { ok: true };
}
function renderComponentOpsBar() {
  if (!shell) return;
  var old = shell.querySelector('.design-ops-bar--components');
  if (old) old.remove();
  if (!componentOps.length) return;
  var bar = el('div', { class: 'design-ops-bar design-ops-bar--components' });
  var copy = el('div', { class: 'design-ops-bar__copy' });
  copy.appendChild(el('strong', { text: t('design.componentMapping.pendingOps', { count: componentOps.length }) }));
  copy.appendChild(el('span', {
    class: 'design-ops-bar__list',
    text: componentOps.map(function (row) { return row.label; }).join(' · ')
  }));
  bar.appendChild(copy);
  var discard = el('button', {
    type: 'button', class: 'btn btn--ghost btn--small', text: t('design.mapping.discard'),
    disabled: componentOpsApplying
  });
  discard.addEventListener('click', function () {
    componentOps = [];
    renderComponentOpsBar();
  });
  var apply = el('button', {
    type: 'button', class: 'btn btn--small', text: componentOpsApplying ? t('design.mapping.applying') : t('design.mapping.apply'),
    disabled: componentOpsApplying
  });
  apply.addEventListener('click', function () { applyComponentOps(componentOps.slice()); });
  bar.appendChild(discard);
  bar.appendChild(apply);
  shell.insertBefore(bar, batchStatus);
}
function applyComponentOps(ops, operationId, preparedRequest) {
  if (componentOpsApplying || !ops.length) return;
  operationId = operationId || newMappingOperationId();
  componentOpsApplying = true;
  renderComponentOpsBar();
  batchStatus.textContent = t('design.mapping.applying');
  // Prepare the CAS envelope once. If the response is lost after commit, an
  // exact replay must retain every expected revision and the operation id so
  // the same-file receipt can return the committed result.
  var prepare = preparedRequest ? Promise.resolve(preparedRequest) :
    designFilters.request('/api/design/component-mappings').then(function (registry) {
    var needsComparison = ops.some(function (row) {
      return row.operation.op === 'upsert-mapping' || row.operation.op === 'add-disposition';
    });
    return {
      operationId: operationId,
      expectedMappingRevision: registry.revision,
      expectedDesignGenerationRevision: registry.designGenerationRevision,
      expectedProjectInventoryRevision: registry.projectInventoryRevision,
      expectedComparisonSemanticHash: needsComparison ? componentComparisonHash : null,
      operations: ops.map(function (row) { return row.operation; })
    };
  });
  prepare.then(function (request) {
    preparedRequest = request;
    return designFilters.post('/api/design/component-mappings', request);
  }).then(function (result) {
    componentOpsApplying = false;
    componentOps = [];
    renderComponentOpsBar();
    batchStatus.textContent = '';
    clipboard.toast(t('design.mapping.applied', { revision: result.revision }));
    // The server enqueues its own recompare after a committed mutation.
    return render({ background: true, force: true });
  }).catch(function (error) {
    componentOpsApplying = false;
    renderComponentOpsBar();
    renderComponentOpsError(error, ops, operationId, preparedRequest);
  });
}
function renderComponentOpsError(error, ops, operationId, preparedRequest) {
  batchStatus.replaceChildren();
  batchStatus.appendChild(el('strong', { text: designFilters.errorMessage(error) }));
  if (error.code === 'COMPONENT_MAPPING_REVISION_CONFLICT') {
    var retry = el('button', { type: 'button', class: 'btn btn--small', text: t('design.mapping.retryConflict') });
    retry.addEventListener('click', function () {
      batchStatus.textContent = '';
      applyComponentOps(ops);
    });
    batchStatus.appendChild(retry);
  } else if (error.code === 'COMPONENT_FINDING_STALE' || error.code === 'component-comparison-required') {
    batchStatus.appendChild(el('span', { text: t('design.mapping.staleComparison') }));
    var recompare = el('button', { type: 'button', class: 'btn btn--small', text: t('design.mapping.recompareComponents') });
    recompare.addEventListener('click', function () { startComponentComparison(recompare); });
    batchStatus.appendChild(recompare);
  } else {
    var retryExact = el('button', { type: 'button', class: 'btn btn--small', text: t('design.retry') });
    retryExact.addEventListener('click', function () {
      batchStatus.textContent = '';
      applyComponentOps(ops, operationId, preparedRequest);
    });
    batchStatus.appendChild(retryExact);
  }
}
function applyTokenOps(ops, operationId, preparedRequest) {
  if (tokenOpsApplying || !ops.length) return;
  operationId = operationId || newMappingOperationId();
  tokenOpsApplying = true;
  renderTokenOpsBar();
  batchStatus.textContent = t('design.mapping.applying');
  var prepare = preparedRequest ? Promise.resolve(preparedRequest) :
    designFilters.request('/api/design/token-mappings').then(function (registry) {
    var needsComparison = ops.some(function (row) {
      return row.operation.op === 'upsert-mapping' || row.operation.op === 'add-disposition';
    });
    return {
      operationId: operationId,
      expectedMappingRevision: registry.revision,
      expectedDesignGenerationRevision: registry.designGenerationRevision,
      expectedProjectInventoryRevision: registry.projectInventoryRevision,
      expectedComparisonSemanticHash: needsComparison ? tokenComparisonHash : null,
      operations: ops.map(function (row) { return row.operation; })
    };
  });
  prepare.then(function (request) {
    preparedRequest = request;
    return designFilters.post('/api/design/token-mappings', request);
  }).then(function (result) {
    tokenOpsApplying = false;
    tokenOps = [];
    renderTokenOpsBar();
    batchStatus.textContent = '';
    clipboard.toast(t('design.mapping.applied', { revision: result.revision }));
    // The server enqueues its own recompare after a committed mutation.
    return render({ background: true, force: true });
  }).catch(function (error) {
    tokenOpsApplying = false;
    renderTokenOpsBar();
    renderTokenOpsError(error, ops, operationId, preparedRequest);
  });
}
function renderTokenOpsError(error, ops, operationId, preparedRequest) {
  batchStatus.replaceChildren();
  batchStatus.appendChild(el('strong', { text: designFilters.errorMessage(error) }));
  if (error.code === 'TOKEN_MAPPING_REVISION_CONFLICT') {
    var retry = el('button', { type: 'button', class: 'btn btn--small', text: t('design.mapping.retryConflict') });
    retry.addEventListener('click', function () {
      batchStatus.textContent = '';
      applyTokenOps(ops);
    });
    batchStatus.appendChild(retry);
  } else if (error.code === 'TOKEN_FINDING_STALE' || error.code === 'token-comparison-required') {
    batchStatus.appendChild(el('span', { text: t('design.mapping.staleComparison') }));
    var recompare = el('button', { type: 'button', class: 'btn btn--small', text: t('design.mapping.recompare') });
    recompare.addEventListener('click', function () { startTokenComparison(recompare); });
    batchStatus.appendChild(recompare);
  } else {
    var retryExact = el('button', { type: 'button', class: 'btn btn--small', text: t('design.retry') });
    retryExact.addEventListener('click', function () {
      batchStatus.textContent = '';
      applyTokenOps(ops, operationId, preparedRequest);
    });
    batchStatus.appendChild(retryExact);
  }
}
function errorState(error) {
  var wrap = el('div', { class: 'design-state design-state--error' });
  wrap.appendChild(el('strong', { text: t('design.errorTitle') }));
  wrap.appendChild(el('p', { text: t('design.loadError', { detail: designFilters.errorMessage(error) }) }));
  var retry = el('button', { type: 'button', class: 'btn', text: t('design.retry') });
  retry.addEventListener('click', render);
  wrap.appendChild(retry);
  return wrap;
}
function renderBatchBar() {
  renderTokenOpsBar();
  renderComponentOpsBar();
  var old = shell.querySelector('.design-batch-bar');
  if (old) old.remove();
  if (state.tab === 'overview') return;
  var selected = selections[state.tab];
  if (!selected || !selected.size) return;
  var bar = el('div', { class: 'design-batch-bar' });
  bar.appendChild(el('strong', { text: t('design.selected', { count: selected.size }) }));
  var clear = el('button', { type: 'button', class: 'btn btn--ghost btn--small', text: t('design.clearSelection') });
  clear.addEventListener('click', function () { selected.clear(); render(); });
  var create = el('button', { type: 'button', class: 'btn btn--small', text: t('design.createSelected') });
  create.addEventListener('click', function () { createSelected(create); });
  bar.appendChild(clear);
  bar.appendChild(create);
  shell.insertBefore(bar, batchStatus);
}
function createSelected(trigger) {
  var selectionTab = state.tab;
  var ids = Array.from(selections[selectionTab] || []);
  var pendingPreviewId = null;
  if (!ids.length || !generationRevision) return;
  trigger.disabled = true;
  batchStatus.textContent = t('design.previewingTasks');
  designFilters.post('/api/design/tasks/preview', {
    findingIds: ids, expectedGenerationRevision: generationRevision
  }).then(function (payload) {
    adopt(payload);
    pendingPreviewId = payload.preview.id;
    return confirmPreview(payload.preview, trigger);
  }).then(function (preview) {
    if (!preview) {
      return designFilters.post('/api/design/tasks/cancel', {
        previewId: pendingPreviewId
      }).then(function () { return null; }, function () { return null; });
    }
    batchStatus.textContent = t('design.creatingTasks');
    return designFilters.post('/api/design/tasks/create', {
      previewId: preview.id,
      expectedFindingSetHash: preview.findingSetHash,
      expectedTaskIndexRevision: preview.taskIndexRevision
    });
  }).then(function (result) {
    trigger.disabled = false;
    if (!result) { batchStatus.textContent = ''; return; }
    selections[selectionTab].clear();
    return render().then(function () { renderTaskResult(result); });
  }).catch(function (error) {
    trigger.disabled = false;
    batchStatus.textContent = t('design.taskError', { detail: designFilters.errorMessage(error) });
  });
}
function renderTaskResult(result) {
  var summary = t('design.taskResult', {
    created: result.counts.created, existing: result.counts.existing,
    skipped: result.counts.skipped, failed: result.counts.failed
  });
  var list = el('ul');
  function addTasks(items, stateKey) {
    (items || []).forEach(function (item) {
      var task = item.task || {}, stem = task.stem || item.stem;
      var label = t('design.taskResultState.' + stateKey) + ': ' + (task.title || stem || item.findingId);
      var content = stem
        ? el('a', { href: '#board?task=' + encodeURIComponent(stem), text: label })
        : el('span', { text: label });
      list.appendChild(el('li', {}, [content]));
    });
  }
  addTasks(result.result && result.result.created, 'created');
  addTasks(result.result && result.result.existing, 'existing');
  (result.result && result.result.skipped || []).forEach(function (item) {
    list.appendChild(el('li', {
      text: t('design.taskResultDetail', {
        state: t('design.taskResultState.skipped'),
        finding: item.findingId,
        detail: designFilters.errorMessage(item.reason)
      })
    }));
  });
  (result.result && result.result.failed || []).forEach(function (item) {
    list.appendChild(el('li', {
      text: t('design.taskResultDetail', {
        state: t('design.taskResultState.failed'),
        finding: item.findingId,
        detail: designFilters.errorMessage(item.error)
      })
    }));
  });
  batchStatus.replaceChildren(el('strong', { text: summary }));
  if (list.childNodes.length) batchStatus.appendChild(list);
  clipboard.toast(summary);
}
function confirmPreview(preview, trigger) {
  return new Promise(function (resolve) {
    var dialog = el('dialog', { class: 'design-confirm', attrs: { 'aria-labelledby': 'design-confirm-title' } });
    dialog.appendChild(el('h3', { id: 'design-confirm-title', text: t('design.confirmTitle') }));
    dialog.appendChild(el('p', { text: t('design.confirmSummary', {
      create: preview.counts.create, existing: preview.counts.existing, skipped: preview.counts.skipped
    }) }));
    var actions = el('div', { class: 'design-dialog-actions' });
    var cancel = el('button', { type: 'button', class: 'btn btn--ghost', text: t('design.cancel') });
    var confirm = el('button', { type: 'button', class: 'btn', text: t('design.confirmCreate') });
    function close(value) {
      if (dialog.open && dialog.close) dialog.close(); else dialog.removeAttribute('open');
      dialog.remove(); resolve(value);
      if (trigger && trigger.focus) setTimeout(function () { trigger.focus(); }, 0);
    }
    cancel.addEventListener('click', function () { close(null); });
    confirm.addEventListener('click', function () { close(preview); });
    dialog.addEventListener('cancel', function (event) { event.preventDefault(); close(null); });
    actions.appendChild(cancel); actions.appendChild(confirm); dialog.appendChild(actions);
    document.body.appendChild(dialog);
    if (dialog.showModal) dialog.showModal(); else dialog.setAttribute('open', '');
    setTimeout(function () { confirm.focus(); }, 0);
  });
}
function refreshPreservingState() {
  if (!rootEl || rootEl.hidden) return;
  if (backgroundRefreshRunning) {
    backgroundRefreshQueued = true;
    return;
  }
  backgroundRefreshRunning = true;
  Promise.resolve(render({ background: true })).then(finishBackgroundRefresh, finishBackgroundRefresh);
}
function finishBackgroundRefresh() {
  backgroundRefreshRunning = false;
  if (!backgroundRefreshQueued) return;
  backgroundRefreshQueued = false;
  refreshPreservingState();
}
function connectDesignEvents() {
  if (eventSubscriptions) return;
  eventSubscriptions = ['design-overview', 'figma-sync-job'].map(function (name) {
    return siteEvents.on(name, refreshPreservingState);
  });
}

export const design = {
  mount: function (root) {
    rootEl = root;
    designEntityDrawer.setTokenHooks({
      addOperation: addTokenOperation,
      adoptAnalysis: adoptTokenAnalysis
    });
    designEntityDrawer.setComponentHooks({
      addOperation: addComponentOperation,
      adoptAnalysis: adoptComponentAnalysis
    });
    build();
    if (unsubscribe) unsubscribe();
    unsubscribe = store.on('change', refreshPreservingState);
    connectDesignEvents();
  },
  refresh: function () {
    if (titleEl) titleEl.textContent = t('design.title');
    if (leadEl) leadEl.textContent = t('design.lead');
    designEntityDrawer.refresh();
    state = designFilters.readState();
    renderTabs();
    render();
  },
  openTarget: function (entityId) {
    if (!state || typeof entityId !== 'string') return;
    var patch = { q: '', status: '', changed: '', hasTask: '', changedSide: '', mappingState: '', scope: '' };
    if (/^cmp-[a-f0-9]{24}$/.test(entityId)) {
      patch.tab = 'components'; patch.entityType = 'component'; patch.entity = entityId;
    } else if (/^cmpp-[a-f0-9]{24}$/.test(entityId)) {
      patch.tab = 'components'; patch.scope = 'project-only';
      patch.entityType = 'project-component'; patch.entity = entityId;
    } else if (/^srf-[a-f0-9]{24}$/.test(entityId)) {
      patch.tab = 'surfaces'; patch.entityType = 'surface'; patch.entity = entityId;
    } else if (/^tok-[a-f0-9]{24}$/.test(entityId)) {
      patch.tab = 'tokens'; patch.entityType = 'token'; patch.entity = entityId;
    } else if (/^tokp-[a-f0-9]{24}$/.test(entityId)) {
      patch.tab = 'tokens'; patch.scope = 'project-only';
      patch.entityType = 'project-token'; patch.entity = entityId;
    } else {
      return;
    }
    changeState(patch, false);
  }
};
