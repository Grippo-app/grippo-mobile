import { clipboard } from './clipboard.js';
import { i18n } from './i18n.js';
import { requestJson, errorCode } from './data/request-json.js';
import { siteEvents } from './event-stream.js';

  // ----------------------------------------------------------------------
  // Store — server-backed snapshot of the orchestrator state.
  //
  // The data this store exposes comes from two places, mediated by the
  // local dev server (orchestrator/site/server.js):
  //
  //   - Form values & manual flags  -> persisted in orchestrator/.cache/site/.site-state.json
  //   - Per-step progress & gates   -> derived from the project's filesystem
  //
  // The client never talks to localStorage. Multiple browsers see the
  // same state because they all read from /api/state. Progress updates
  // (e.g. a wizard step completing) arrive via SSE on /api/events; the
  // store re-fetches and notifies subscribers without a page reload.
  //
  // Public API:
  //
  //   store.load()                -> Promise<state>
  //   store.readFresh()           -> Promise<state>   // read-only, no cache
  //   store.get()                 -> latest cached snapshot (synchronous)
  //   store.saveSetup(partial)    -> Promise<state>   // form fields
  //   store.saveManualStep(id, b) -> Promise<state>   // confirms a
  //                                                    dialog-only step
  //                                                    OR overrides a
  //                                                    FS-derived step
  //   store.reset()               -> Promise<state>
  //   store.on('change', fn)      -> unsubscribe()
  //
  // Server requirement: the page MUST be served by `node orchestrator/
  // site/server.js`. Opening index.html via file:// or any other static
  // server will leave the store stuck in its initial empty snapshot and
  // every action will fail with a network error toast.
  // ----------------------------------------------------------------------

  function initialSnapshot() {
    return {
      version: 1,
      setup: {},
      // Mirrors deriveState()'s top-level sessions map so run-control / the
      // sessions menu read a stable shape even before the first /api/state load.
      sessions: {},
      backend: null,
      startupRecovery: { version: 1, status: 'pending', attempts: 0, startedAt: null,
        updatedAt: null, readyAt: null, reasonCode: null, findingCount: 0 },
      taskIntegrity: { version: 1, ok: true, indexStatus: 'unchecked', affectedStems: [], findings: [] },
      progress: {
        setupDone: false,
        requirementsVerified: false,
        yamlPasted: false,
        agentsInstalled: false,
        finalizations: [],
        shallowIntake: {},
        creationRecoveries: [],
        editRecoveries: [],
        publicationRecoveryIssues: [],
        wizardStepsDone: [],
        stepStatus: {}
      },
      status: null
    };
  }

  var cache = initialSnapshot();
  var listeners = [];
  var eventSubscription = null;
  var lastEventAt = null;
  // Set when a live-update refetch fails: the channel is open but the snapshot
  // on screen is stale, and the header must not claim otherwise.
  var refreshError = null;
  var inflightLoad = null;

  function clone(value) {
    return JSON.parse(JSON.stringify(value));
  }

  function notify() {
    var snap = clone(cache);
    for (var i = 0; i < listeners.length; i++) {
      try { listeners[i](snap); } catch (e) { /* keep chain */ }
    }
  }

  function applyState(parsed) {
    if (!parsed || typeof parsed !== 'object' || parsed.version !== 1 ||
        !parsed.setup || !parsed.progress) {
      var invalid = new Error('invalid-state-response');
      invalid.code = 'invalid-state-response';
      invalid.kind = invalid.code;
      throw invalid;
    }
    cache = parsed;
    // Any accepted snapshot proves the UI is current again — otherwise one
    // transient refetch failure would pin the header on "stalled" until the next
    // SSE change happened to arrive.
    refreshError = null;
    if (typeof window !== 'undefined' && parsed.csrfToken) window.__ORCHESTRATOR_CSRF__ = parsed.csrfToken;
    return clone(cache);
  }

  function fetchState() {
    return requestJson('/api/state', { cache: 'no-store' }).then(applyState);
  }

  // Independent, read-only state proof for workflows that must make a decision
  // from one specific server response. It deliberately neither mutates `cache`
  // nor notifies listeners: concurrent SSE/load responses therefore cannot
  // replace the snapshot between the caller's read and its decision.
  function readFresh() {
    return requestJson('/api/state', { cache: 'no-store' }).then(function (parsed) {
      if (!parsed || typeof parsed !== 'object' || parsed.version !== 1 ||
          !parsed.setup || !parsed.progress) {
        var invalid = new Error('invalid-state-response');
        invalid.code = 'invalid-state-response';
        invalid.kind = invalid.code;
        throw invalid;
      }
      return clone(parsed);
    });
  }

  function t(key, params) {
    return (i18n && typeof i18n.t === 'function') ? i18n.t(key, params) : key;
  }

  function postJson(url, body) {
    var headers = { 'content-type': 'application/json' };
    if (typeof window !== 'undefined' && window.__ORCHESTRATOR_CSRF__) headers['x-orchestrator-csrf'] = window.__ORCHESTRATOR_CSRF__;
    return requestJson(url, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(body || {}),
      cache: 'no-store'
    }).then(function (parsed) {
      var snap = applyState(parsed);
      notify();
      return snap;
    }, function (err) {
      // The header comment promises a network-error toast; deliver it here so
      // every state-patch write (saveSetup / saveManualStep) gets the feedback.
      // Re-throw to keep the reject contract for callers.
      var key = 'common.requestError.' + errorCode(err);
      var message = t(key);
      clipboard.toastError(message === key ? t('common.saveFailed') : message);
      throw err;
    });
  }

  // SSE: server pushes "change" whenever its derived snapshot changes
  // (form save, manual flag toggle, or filesystem mutation under any
  // watched path). We re-fetch + notify on every event; the initial
  // event fires right after connect so the first paint is current.
  function connectEvents() {
    if (eventSubscription) return;
    eventSubscription = siteEvents.on('change', function () {
      // Stamp liveness only once the snapshot actually arrived. Stamping on the
      // event made the header read "connected · just now" while every panel was
      // frozen on the last good state, so a silent refresh failure looked live.
      fetchState().then(function () {
        lastEventAt = Date.now();
        notify();
      }, function (err) {
        refreshError = errorCode(err) || 'unknown';
        notify();
      });
    });
  }

  function load() {
    if (inflightLoad) return inflightLoad;
    inflightLoad = fetchState().then(function (snap) {
      inflightLoad = null;
      connectEvents();
      notify();
      return snap;
    }, function (err) {
      inflightLoad = null;
      var key = 'common.requestError.' + errorCode(err);
      var message = t(key);
      clipboard.toastError(message === key ? t('common.requestError.unknown') : message);
      throw err;
    });
    return inflightLoad;
  }

  function get() {
    return clone(cache);
  }

  function saveSetup(partial) {
    if (!partial || typeof partial !== 'object') return Promise.resolve(get());
    // Optimistic local update so the form doesn't flicker while the round
    // trip lands. The server's response (via the POST and then SSE)
    // overwrites this with the canonical snapshot.
    cache.setup = Object.assign({}, cache.setup || {}, partial);
    notify();
    return postJson('/api/state-patch', { setup: partial });
  }

  function saveManualStep(stepId, done) {
    if (!stepId) return Promise.resolve(get());
    var patch = { manualSteps: {} };
    patch.manualSteps[stepId] = done === true;
    return postJson('/api/state-patch', patch);
  }

  // UI language ('en'|'ru'|'uk') — top-level state-patch key (NOT a setup field;
  // see server/http.js). Persisted server-side so the choice survives the
  // explicitly changed port that invalidates the per-origin localStorage copy.
  function saveUiLang(lang) {
    if (typeof lang !== 'string' || !lang) return Promise.resolve(get());
    return postJson('/api/state-patch', { uiLang: lang });
  }

  function reset() {
    return postJson('/api/reset', {});
  }

  function on(event, fn) {
    if (event !== 'change' || typeof fn !== 'function') return function () {};
    listeners.push(fn);
    return function off() {
      var idx = listeners.indexOf(fn);
      if (idx >= 0) listeners.splice(idx, 1);
    };
  }

  // Live-update channel health, for the header status indicator. sseReadyState
  // mirrors EventSource: 0 connecting, 1 open, 2 closed, -1 not yet created.
  function connection() {
    return {
      sseReadyState: siteEvents.connection().readyState,
      lastEventAt: lastEventAt,
      refreshError: refreshError
    };
  }

  export const store = {
    load: load,
    readFresh: readFresh,
    get: get,
    saveSetup: saveSetup,
    saveManualStep: saveManualStep,
    saveUiLang: saveUiLang,
    reset: reset,
    on: on,
    connection: connection
  };
