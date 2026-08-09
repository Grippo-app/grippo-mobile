import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { router } from '../router.js';
import { errorCode } from '../data/request-json.js';
import { architectureApi } from '../data/architecture-api.js';
import { renderArchitectureOverview } from '../architecture/overview.js';
import {
  renderArchitectureControls,
  renderNodeCatalog,
  tabKind
} from '../architecture/catalog.js';
import { renderArchitectureFindings } from '../architecture/findings.js';
import { createNodeDrawer } from '../architecture/node-detail.js';
import { renderArchitectureDiff } from '../architecture/diff.js';
import { renderArchitectureGraph } from '../architecture/graph.js';
import { siteEvents } from '../event-stream.js';
import { confirmDialog } from '../ui-dialog.js';

var el = dom.el;
function t(key, params) {
  return i18n && typeof i18n.t === 'function' ? i18n.t(key, params) : key;
}
export function archmapErrorMessage(error) {
  var code = errorCode(error);
  var key = 'archmap.error.' + code;
  var translated = t(key);
  return translated === key ? t('archmap.error.unknown') : translated;
}

var sectionEl = null;
var overviewData = null;
var listData = null;
var diffData = null;
var graphData = null;
var loading = false;
var listLoading = false;
var loadError = null;
var requestGeneration = 0;
var contentRequestGeneration = 0;
var drawer = null;
var eventSubscriptions = null;
var activeJob = null;
var jobPollTimer = null;
var jobPollGeneration = 0;
var pendingFindingTasks = Object.create(null);
var graphViewport = { x: 0, y: 0, scale: 1 };
var state = {
  tab: 'modules',
  view: 'list',
  search: '',
  platform: '',
  layer: '',
  ownership: '',
  severity: '',
  findingType: '',
  confidence: '',
  changed: false,
  node: ''
};

function parseHash() {
  var raw = (location.hash || '').replace(/^#archmap\??/, '');
  var params = new URLSearchParams(raw);
  var tab = params.get('tab');
  var view = params.get('view');
  var platform = params.get('platform');
  var layer = params.get('layer');
  var severity = params.get('severity');
  var findingType = params.get('findingType');
  var confidence = params.get('confidence');
  var node = params.get('node');
  state = {
    tab: ['modules', 'features', 'screens', 'data', 'findings'].indexOf(tab) >= 0
      ? tab : 'modules',
    view: view === 'graph' && tab !== 'findings' ? 'graph' : 'list',
    search: Array.from(params.get('search') || '').slice(0, 200).join(''),
    platform: ['shared', 'android', 'ios', 'tooling', 'unknown'].indexOf(platform) >= 0
      ? platform : '',
    layer: ['ui', 'domain', 'data', 'infrastructure', 'build', 'unknown'].indexOf(layer) >= 0
      ? layer : '',
    ownership: Array.from(params.get('ownership') || '').slice(0, 180).join(''),
    severity: ['error', 'warning', 'info'].indexOf(severity) >= 0 ? severity : '',
    findingType: [
      'dependency-cycle', 'forbidden-dependency', 'orphan-module',
      'unused-repository', 'screen-without-owner'
    ].indexOf(findingType) >= 0 ? findingType : '',
    confidence: ['exact', 'derived', 'heuristic'].indexOf(confidence) >= 0
      ? confidence : '',
    changed: params.get('changed') === 'true',
    node: /^[a-z][a-z0-9-]{0,31}:[A-Za-z0-9._~/-]{1,147}$/.test(node || '')
      ? node : ''
  };
  if (state.tab !== 'findings') {
    state.severity = '';
    state.findingType = '';
    state.confidence = '';
  }
}

function syncHash() {
  if (router.current() && router.current() !== 'archmap') return;
  var params = new URLSearchParams();
  if (state.tab !== 'modules') params.set('tab', state.tab);
  if (state.view !== 'list') params.set('view', state.view);
  ['search', 'platform', 'layer', 'ownership', 'severity', 'findingType',
    'confidence', 'node'].forEach(function (key) {
    if (state[key]) params.set(key, state[key]);
  });
  if (state.changed) params.set('changed', 'true');
  var next = '#archmap' + (params.toString() ? '?' + params.toString() : '');
  if (location.hash !== next) {
    try { history.replaceState(null, '', next); }
    catch (error) { location.hash = next; }
  }
}

function filters() {
  return {
    search: state.search || null,
    kind: tabKind(state.tab),
    platform: state.platform || null,
    layer: state.layer || null,
    ownership: state.ownership || null,
    severity: state.severity || null,
    type: state.findingType || null,
    confidence: state.confidence || null,
    changed: state.changed || null,
    limit: 100
  };
}

function resetContentData(resetViewport) {
  contentRequestGeneration++;
  // The reset is always followed by a fetch, so the render in between must say
  // "loading" — with `false` it rendered the empty state and told the user their
  // filters matched nothing on every load, tab switch and keystroke.
  listLoading = true;
  listData = null;
  graphData = null;
  if (resetViewport) graphViewport = { x: 0, y: 0, scale: 1 };
}

function updateState(patch) {
  if (router.current() && router.current() !== 'archmap') return;
  var changed = false;
  Object.keys(patch).forEach(function (key) {
    if (state[key] !== patch[key]) {
      state[key] = patch[key];
      changed = true;
    }
  });
  if (!changed) return;
  if (patch.tab && patch.tab !== 'findings') {
    state.severity = '';
    state.findingType = '';
    state.confidence = '';
  }
  if (state.tab === 'findings' && state.view === 'graph') state.view = 'list';
  syncHash();
  resetContentData(true);
  render();
  fetchContent();
}

function appendPage(response) {
  if (!listData || listData.structuralHash !== response.structuralHash) {
    listData = response;
    return;
  }
  listData = Object.assign({}, response, {
    rows: listData.rows.concat(response.rows)
  });
}

function fetchContent(cursor) {
  if (!overviewData || !overviewData.present || cursor && listLoading) {
    // resetContentData() flips the flag on for the fetch that follows it; if we
    // bail here it must go back off, or `cursor && listLoading` would wedge
    // "load more" permanently.
    if (!cursor) listLoading = false;
    return;
  }
  var generation = ++contentRequestGeneration;
  listLoading = true;
  var params = filters();
  if (cursor) params.cursor = cursor;
  var request;
  if (state.view === 'graph' && state.tab !== 'findings') {
    delete params.limit;
    delete params.severity;
    delete params.type;
    delete params.confidence;
    request = architectureApi.graph(params);
  } else if (state.tab === 'findings') {
    delete params.kind;
    request = architectureApi.findings(params);
  } else {
    delete params.severity;
    delete params.type;
    delete params.confidence;
    request = architectureApi.nodes(params);
  }
  request.then(function (data) {
    if (generation !== contentRequestGeneration) return;
    listLoading = false;
    loadError = null;
    if (state.view === 'graph' && state.tab !== 'findings') graphData = data;
    else if (cursor) appendPage(data);
    else listData = data;
    render();
  }, function (error) {
    if (generation !== contentRequestGeneration) return;
    listLoading = false;
    loadError = error;
    render();
  });
}

function fetchAll() {
  var generation = ++requestGeneration;
  contentRequestGeneration++;
  loading = true;
  // Content is fetched as soon as the overview lands, so the panel is loading
  // throughout — not empty. `false` here painted "no results match your filters"
  // during every normal load, tab switch and keystroke.
  listLoading = true;
  loadError = null;
  render();
  Promise.all([
    architectureApi.overview(),
    architectureApi.diff('task').catch(function (error) {
      return { present: false, error: errorCode(error) };
    })
  ]).then(function (values) {
    if (generation !== requestGeneration) return;
    loading = false;
    overviewData = values[0];
    diffData = values[1];
    if (state.changed && (!diffData.present || !diffData.diff ||
        diffData.diff.baselineCreated || diffData.diff.truncated)) {
      state.changed = false;
      syncHash();
    }
    if (overviewData.latestJob &&
        (!activeJob || Date.parse(overviewData.latestJob.startedAt) >= Date.parse(activeJob.startedAt))) {
      activeJob = overviewData.latestJob;
    }
    if (activeJob && ['queued', 'running'].indexOf(activeJob.state) >= 0) {
      beginJobPolling(activeJob.id);
    } else {
      jobPollGeneration++;
      clearTimeout(jobPollTimer);
    }
    render();
    fetchContent();
    if (state.node && drawer) drawer.open(state.node);
  }, function (error) {
    if (generation !== requestGeneration) return;
    loading = false;
    // No content fetch will follow a failed overview, so the list must stop
    // claiming to load — otherwise the error box renders under a permanent
    // "Loading map…" and `cursor && listLoading` wedges "load more".
    listLoading = false;
    loadError = error;
    render();
  });
}

function openNode(id) {
  state.node = id;
  syncHash();
  if (drawer) drawer.open(id);
}

function closeNode() {
  state.node = '';
  syncHash();
}

function renderJob(host) {
  if (!activeJob) return;
  var failed = activeJob.state === 'failed' || activeJob.state === 'interrupted';
  var className = failed
    ? 'banner banner--error'
    : activeJob.state === 'succeeded'
      ? 'banner banner--success'
      : 'banner banner--info';
  var key = failed
    ? 'archmap.job.failed'
    : activeJob.state === 'succeeded'
      ? 'archmap.job.succeeded'
      : 'archmap.job.running';
  var phaseKey = 'archmap.job.phase.' + (activeJob.phase || activeJob.state);
  var translatedPhase = t(phaseKey);
  host.appendChild(el('div', {
    class: className,
    text: t(key, {
      phase: translatedPhase === phaseKey
        ? activeJob.phase || activeJob.state : translatedPhase
    })
  }));
}

function renderEmpty(host) {
  if (!overviewData) return;
  var box = el('div', { class: 'architecture-empty-state' });
  box.appendChild(el('h3', {
    text: overviewData.ready ? t('archmap.empty.title') : t('archmap.notReady.title')
  }));
  box.appendChild(el('p', {
    text: overviewData.ready ? t('archmap.empty.body') : t('archmap.notReady.body')
  }));
  if (!overviewData.ready) {
    var setup = el('button', {
      type: 'button', class: 'btn btn--primary', text: t('archmap.openSetup')
    });
    setup.addEventListener('click', function () { router.go('setup'); });
    box.appendChild(setup);
  } else {
    var diagnostics = el('button', {
      type: 'button', class: 'btn', text: t('archmap.viewSetupDiagnostics')
    });
    diagnostics.addEventListener('click', function () { router.go('setup'); });
    box.appendChild(diagnostics);
  }
  if (overviewData.error) {
    box.appendChild(el('div', {
      class: 'banner banner--warn',
      text: t('archmap.parseError')
    }));
  }
  host.appendChild(box);
}

function beginJobPolling(id) {
  var generation = ++jobPollGeneration;
  clearTimeout(jobPollTimer);
  pollJob(id, 0, generation);
}

function pollJob(id, failures, generation) {
  if (generation !== jobPollGeneration) return;
  architectureApi.job(id).then(function (response) {
    if (generation !== jobPollGeneration) return;
    activeJob = response.job;
    render();
    if (activeJob && ['queued', 'running'].indexOf(activeJob.state) >= 0) {
      jobPollTimer = setTimeout(function () { pollJob(id, 0, generation); }, 1000);
    } else if (activeJob && activeJob.state === 'succeeded') {
      jobPollGeneration++;
      fetchAll();
    }
  }, function (error) {
    if (generation !== jobPollGeneration) return;
    var count = Number(failures) + 1;
    if (count >= 10) {
      loadError = error;
      render();
      return;
    }
    jobPollTimer = setTimeout(function () {
      pollJob(id, count, generation);
    }, Math.min(5000, 750 + count * 500));
  });
}

function generate(reason) {
  if (!overviewData || activeJob &&
      ['queued', 'running'].indexOf(activeJob.state) >= 0) return;
  architectureApi.generate(
    overviewData.freshness && overviewData.freshness.currentRevision || null,
    reason
  ).then(function (response) {
    activeJob = response.job;
    render();
    if (activeJob) beginJobPolling(activeJob.id);
  }, function (error) {
    loadError = error;
    render();
  });
}

function createFindingTask(finding) {
  if (!overviewData || !overviewData.taskIndex || !overviewData.taskIndex.complete ||
      pendingFindingTasks[finding.id]) return;
  pendingFindingTasks[finding.id] = true;
  render();
  architectureApi.previewTask(
    finding,
    overviewData.structuralHash,
    overviewData.taskIndex.revision
  ).then(function (response) {
    if (response.preview.existingTask) {
      router.openTarget({ panel: 'board', entityId: response.preview.existingTask.stem });
      return;
    }
    // The held preview is released on cancel exactly as before; only the OS
    // prompt is gone.
    return confirmDialog({
      title: t('archmap.taskConfirmTitle'),
      message: t('archmap.taskConfirm', { title: finding.title }),
      confirmLabel: t('archmap.taskConfirmAction')
    }).then(function (accepted) {
      if (!accepted) {
        architectureApi.cancelTask(response.preview.id).catch(function () {});
        return null;
      }
      return architectureApi.createTask(
        response.preview.id,
        response.structuralHash,
        response.taskIndexRevision
      ).then(function (created) {
        fetchAll();
        if (created.task && created.task.stem) {
          router.openTarget({ panel: 'board', entityId: created.task.stem });
        }
      });
    });
  }).catch(function (error) {
    loadError = error;
    render();
  }).finally(function () {
    delete pendingFindingTasks[finding.id];
    render();
  });
}

function captureInteraction() {
  var active = typeof document !== 'undefined' ? document.activeElement : null;
  if (!active || !sectionEl || !sectionEl.contains(active)) return null;
  var key = active.getAttribute && active.getAttribute('data-architecture-control');
  if (!key) return null;
  var editable = /^(INPUT|SELECT|TEXTAREA)$/.test(active.tagName || '');
  return {
    key: key,
    value: editable ? active.value : null,
    checked: editable && active.type === 'checkbox' ? active.checked : null,
    start: editable && typeof active.selectionStart === 'number'
      ? active.selectionStart : null,
    end: editable && typeof active.selectionEnd === 'number'
      ? active.selectionEnd : null
  };
}

function restoreInteraction(snapshot) {
  if (!snapshot || !sectionEl) return;
  var candidates = sectionEl.querySelectorAll('[data-architecture-control]');
  var replacement = null;
  for (var index = 0; index < candidates.length; index++) {
    if (candidates[index].getAttribute('data-architecture-control') === snapshot.key) {
      replacement = candidates[index];
      break;
    }
  }
  if (!replacement || replacement.disabled) return;
  if (snapshot.value !== null &&
      /^(INPUT|SELECT|TEXTAREA)$/.test(replacement.tagName || '')) {
    replacement.value = snapshot.value;
  }
  if (snapshot.checked !== null && replacement.type === 'checkbox') {
    replacement.checked = snapshot.checked;
  }
  try { replacement.focus({ preventScroll: true }); }
  catch (error) { replacement.focus(); }
  if (snapshot.start !== null && typeof replacement.setSelectionRange === 'function') {
    replacement.setSelectionRange(snapshot.start, snapshot.end);
  }
}

function finishRender(interaction, scrollY) {
  if (typeof window === 'undefined') return;
  requestAnimationFrame(function () {
    restoreInteraction(interaction);
    if (scrollY > 0) window.scrollTo(0, scrollY);
  });
}

function render() {
  if (!sectionEl) return;
  var scrollY = typeof window !== 'undefined' ? window.scrollY : 0;
  var interaction = captureInteraction();
  sectionEl.replaceChildren();
  if (!overviewData && loading) {
    sectionEl.appendChild(el('h2', { class: 'panel-title', text: t('archmap.title') }));
    sectionEl.appendChild(el('p', { class: 'panel-lead', text: t('archmap.loading') }));
    finishRender(interaction, scrollY);
    return;
  }
  renderArchitectureOverview(sectionEl, overviewData || {
    present: false,
    canGenerate: false,
    freshness: { status: 'missing' }
  }, {
    t: t,
    view: state.view,
    allowGraph: state.tab !== 'findings',
    onView: function (view) { updateState({ view: view }); },
    onGenerate: generate,
    onSelect: openNode
  });
  renderJob(sectionEl);
  if (loadError) {
    var errorBox = el('div', {
      class: 'agent-cards-status agent-cards-status--error'
    });
    errorBox.appendChild(el('p', {
      class: 'panel-lead',
      text: archmapErrorMessage(loadError)
    }));
    var retry = el('button', {
      type: 'button', class: 'btn', text: t('archmap.retry')
    });
    retry.addEventListener('click', function () {
      loadError = null;
      fetchAll();
    });
    errorBox.appendChild(retry);
    sectionEl.appendChild(errorBox);
  }
  if (!overviewData || !overviewData.present) {
    renderEmpty(sectionEl);
    finishRender(interaction, scrollY);
    return;
  }
  renderArchitectureDiff(sectionEl, diffData, { t: t });
  renderArchitectureControls(sectionEl, state, {
    t: t,
    changedAvailable: !!(diffData && diffData.present && diffData.diff &&
      !diffData.diff.baselineCreated && !diffData.diff.truncated),
    onChange: updateState
  });
  var content = el('div', {
    class: 'architecture-content',
    attrs: { 'aria-live': 'polite' }
  });
  if (listLoading && !listData && !graphData) {
    content.appendChild(el('p', { class: 'panel-lead', text: t('archmap.loading') }));
  } else if (state.view === 'graph' && state.tab !== 'findings') {
    renderArchitectureGraph(content, graphData, {
      t: t,
      onSelect: openNode,
      viewport: graphViewport,
      onViewport: function (viewport) { graphViewport = viewport; }
    });
  } else if (state.tab === 'findings') {
    renderArchitectureFindings(content, listData, {
      t: t,
      onSelect: openNode,
      onMore: fetchContent,
      taskCreationEnabled: !!(overviewData.taskIndex && overviewData.taskIndex.complete),
      taskCreationPending: pendingFindingTasks,
      onCreateTask: createFindingTask,
      onOpenTask: function (stem) {
        router.openTarget({ panel: 'board', entityId: stem });
      }
    });
  } else {
    renderNodeCatalog(content, listData, {
      t: t,
      onSelect: openNode,
      onMore: fetchContent
    });
  }
  sectionEl.appendChild(content);
  finishRender(interaction, scrollY);
}

function wireEvents() {
  if (eventSubscriptions) return;
  var openSubscription = siteEvents.on('open', function () {
    if (sectionEl && !sectionEl.hidden) fetchAll();
  });
  var jobSubscription = siteEvents.on('architecture-job', function (event) {
    var job;
    try { job = JSON.parse(event.data); } catch (error) { return; }
    if (!activeJob || activeJob.id === job.id ||
        Date.parse(job.startedAt) > Date.parse(activeJob.startedAt) ||
        job.startedAt === activeJob.startedAt && job.id > activeJob.id) {
      activeJob = job;
      if (['queued', 'running'].indexOf(job.state) < 0) {
        jobPollGeneration++;
        clearTimeout(jobPollTimer);
      }
      render();
    }
  });
  var changedSubscription = siteEvents.on('architecture-changed', function () {
    fetchAll();
  });
  eventSubscriptions = [openSubscription, jobSubscription, changedSubscription];
}

export const archmap = {
  mount: function (rootEl) {
    sectionEl = rootEl;
    parseHash();
    drawer = createNodeDrawer({
      t: t,
      load: function (id, params) {
        return architectureApi.node(id, Object.assign({ limit: 100 }, params || {}));
      },
      onClose: closeNode,
      onNavigate: openNode,
      onOpenTask: function (stem) {
        router.openTarget({ panel: 'board', entityId: stem });
      },
      onShowDiff: function () {
        var latest = sectionEl && sectionEl.querySelector('.architecture-diff');
        if (latest) {
          latest.open = true;
          latest.scrollIntoView({ block: 'center' });
          if (typeof latest.focus === 'function') {
            latest.setAttribute('tabindex', '-1');
            latest.focus();
          }
        }
      }
    });
    wireEvents();
    syncHash();
    render();
    fetchAll();
  },
  refresh: function () {
    var priorNode = state.node;
    parseHash();
    if (priorNode && !state.node && drawer) drawer.close();
    resetContentData();
    syncHash();
    fetchAll();
  },
  openTarget: function (entityId) {
    if (typeof entityId !== 'string' ||
        !/^[a-z][a-z0-9-]{0,31}:[A-Za-z0-9._~/-]{1,147}$/.test(entityId)) return;
    state.node = entityId;
    syncHash();
    if (drawer) drawer.open(entityId);
  }
};
