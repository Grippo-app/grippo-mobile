import { dom } from './dom.js';
import { i18n } from './i18n.js';
import { requestJson } from './data/request-json.js';
import { appRunErrorMessage } from './app-run-errors.js';
import { appRunMenu } from './app-run-menu.js';
import { appRunDrawer } from './app-run-drawer.js';
import { appRunLogs } from './app-run-logs.js';
import { appRunValidation } from './app-run-validation.js';
import { confirmDialog } from './ui-dialog.js';
import { siteEvents } from './event-stream.js';

var el = dom.el;
function t(key, params) { return i18n.t(key, params); }
var state = {
  mounted: false, root: null, menuOpen: false, drawerOpen: false,
  status: null, targets: null, context: { taskStem: null, surfaceId: null },
  selection: {
    platform: null, targetId: null, variantId: null, buildMode: null,
    whenBusy: 'queue', runAfterCreation: false
  },
  screenshotUrl: null, loading: false, refreshPromise: null, refreshAllPending: false,
  refreshedCreatedJobId: null, createdRefreshAttempts: 0,
  pendingCreatedRun: false, events: null, error: null, actionError: null,
  pendingFocus: null
};

function idempotency() {
  if (window.crypto && typeof window.crypto.randomUUID === 'function') return window.crypto.randomUUID();
  if (window.crypto && typeof window.crypto.getRandomValues === 'function') {
    var bytes = new Uint8Array(18);
    window.crypto.getRandomValues(bytes);
    return 'web-' + Array.from(bytes).map(function (byte) {
      return byte.toString(16).padStart(2, '0');
    }).join('');
  }
  throw new Error('secure browser randomness is unavailable');
}
function headers() {
  return {
    'content-type': 'application/json',
    'x-orchestrator-csrf': window.__ORCHESTRATOR_CSRF__ || ''
  };
}
function get(url) { return requestJson(url, { cache: 'no-store' }); }
function post(url, body) {
  return requestJson(url, { method: 'POST', headers: headers(), body: JSON.stringify(body), cache: 'no-store' });
}
function creationSize(value) {
  if (value === 0) return t('appRun.noDownload');
  if (!Number.isSafeInteger(value) || value < 0) return t('appRun.downloadSizeUnknown');
  return new Intl.NumberFormat(undefined, {
    style: 'unit', unit: 'megabyte', maximumFractionDigits: 1
  }).format(value / (1024 * 1024));
}
function errorText(error) {
  return appRunErrorMessage(error);
}
// A control that already fired a request must not be clickable again until that
// request settles: a second Run/Stop/Restart races the first and reports a state
// conflict the user never caused.
function busy(control, active) {
  if (control) control.disabled = active === true;
  // Starting a new attempt retires the previous attempt's verdict.
  if (active === true) state.actionError = null;
}
// Failures land in the panel's own warn banner instead of window.alert: modal
// alerts block the page, cannot be re-read, and drop the message on dismissal.
// A user action's failure is NOT refresh state: `state.error` is cleared by every
// successful refresh, and an app-run-status event fires on each job transition —
// exactly while a failed Stop/Cancel/Restart is being read. Hold it separately so
// only the next user action (or a successful one) replaces it.
function showError(text) {
  state.actionError = text;
  render();
}
function requestFocus(target) {
  state.pendingFocus = target;
}
function applyDefaults() {
  if (!state.targets) return;
  var preference = state.status && state.status.preferences || {};
  var platforms = state.targets.platforms || [];
  var current = platforms.find(function (row) { return row.id === (state.selection.platform || preference.platform); }) ||
    platforms.find(function (row) { return row.availability !== 'unavailable'; }) || platforms[0];
  if (!current) return;
  state.selection.platform = current.id;
  var preferred = current.devices.find(function (row) { return row.stableHint === preference.targetStableHint; });
  if (!state.selection.targetId || !current.devices.some(function (row) { return row.id === state.selection.targetId; })) {
    state.selection.targetId = preferred ? preferred.id : null;
  }
  if (!state.selection.variantId || !current.variants.some(function (row) { return row.id === state.selection.variantId; })) {
    state.selection.variantId = current.variants.some(function (row) { return row.id === preference.variantId; })
      ? preference.variantId : current.variants[0] && current.variants[0].id;
  }
  state.selection.buildMode = state.selection.buildMode || preference.buildMode || 'if-needed';
}
function refresh(all) {
  if (state.loading) {
    if (all) state.refreshAllPending = true;
    return state.refreshPromise || Promise.resolve();
  }
  state.loading = true;
  var requests = [get('/api/app-run/status')];
  if (all || !state.targets) requests.push(get('/api/app-run/targets' + (all ? '?refresh=1' : '')));
  var launchAfterRefresh = false;
  state.refreshPromise = Promise.all(requests).then(function (rows) {
    state.error = null;
    state.status = rows[0];
    if (rows[1]) state.targets = rows[1].targets;
    applyDefaults();
    var job = state.status && state.status.job;
    if (job && job.action === 'create-device' &&
        ['completed', 'failed', 'cancelled', 'interrupted'].indexOf(job.state) >= 0 &&
        state.refreshedCreatedJobId !== job.jobId) {
      if (job.state === 'completed' && rows[1] && job.result && job.result.displayName) {
        var createdPlatform = state.targets.platforms.find(function (row) { return row.id === job.platform; });
        var created = createdPlatform && createdPlatform.devices.find(function (row) {
          return row.stableHint === job.result.targetStableHint;
        });
        if (created) {
          state.selection.platform = job.platform;
          state.selection.targetId = created.id;
          state.refreshedCreatedJobId = job.jobId;
          state.createdRefreshAttempts = 0;
          launchAfterRefresh = state.pendingCreatedRun;
          state.pendingCreatedRun = false;
        } else {
          state.createdRefreshAttempts++;
          if (state.createdRefreshAttempts < 3) {
            state.refreshAllPending = true;
          } else {
            state.refreshedCreatedJobId = job.jobId;
            state.pendingCreatedRun = false;
            state.error = t('appRun.createdDeviceNotFound');
          }
        }
      } else if (job.state === 'completed' && !rows[1]) {
        state.refreshAllPending = true;
      } else {
        state.refreshedCreatedJobId = job.jobId;
        state.pendingCreatedRun = false;
      }
    }
  }).catch(function (error) {
    state.error = errorText(error);
  }).then(function () {
    state.loading = false;
    state.refreshPromise = null;
    render();
    if (state.refreshAllPending) {
      state.refreshAllPending = false;
      return refresh(true);
    }
    if (launchAfterRefresh) return startRun(null);
  });
  return state.refreshPromise;
}
function mergeSelection(patch) {
  Object.assign(state.selection, patch);
  render();
}
function clearContext() {
  state.context = { taskStem: null, surfaceId: null };
}
function runBody(token) {
  return {
    platform: state.selection.platform,
    targetId: state.selection.targetId,
    discoveryRevision: state.targets.discoveryRevision,
    variantId: state.selection.variantId,
    buildMode: state.selection.buildMode,
    taskStem: state.context.taskStem,
    surfaceId: state.context.surfaceId,
    expectedProjectSourceRevision: state.targets.projectSourceRevision,
    confirmationToken: token || null,
    whenBusy: state.selection.whenBusy || 'queue',
    idempotencyKey: idempotency()
  };
}
function startRun(token, control) {
  var body = runBody(token);
  busy(control, true);
  return post('/api/app-run/start', body).then(function (result) {
    busy(control, false);
    state.menuOpen = false; state.drawerOpen = true;
    requestFocus('drawer');
    clearContext();
    state.status = state.status || {};
    state.status.job = result.job;
    render(); refresh(false);
  }).catch(function (error) {
    busy(control, false);
    // The answer arrives a turn later than window.confirm's did, so the retry and the
    // report both move into it: reporting up front would label an accepted retry as a
    // failure, and returning early would drop the reason when the offer is declined.
    if (error.confirmation && error.confirmation.token) {
      return confirmDialog({
        title: t('appRun.sourceChanged'),
        message: t('appRun.confirmOlder', { date: error.confirmation.builtAt || '' })
      }).then(function (accepted) {
        if (accepted) return startRun(error.confirmation.token, control);
        showError(errorText(error));
      });
    }
    showError(errorText(error));
  });
}
function createDevice(profile, control) {
  if (!profile) return;
  busy(control, true);
  post('/api/app-run/devices/preview', {
    platform: state.selection.platform, profileId: profile.id,
    runtimeId: profile.runtimeId, discoveryRevision: state.targets.discoveryRevision,
    idempotencyKey: idempotency()
  }).then(function (result) {
    // The preview is one localized string built as a question plus one line per fact;
    // window.confirm was the only reason it had to be flat. The same string now feeds
    // the dialog's message and its line list, so the facts stay legible as rows.
    var preview = t('appRun.confirmCreate', {
      name: result.preview.generatedName,
      device: result.preview.displayName,
      runtime: result.preview.runtimeName,
      size: creationSize(result.preview.estimatedBytes)
    }).split('\n');
    return confirmDialog({
      title: t('appRun.createDevice'),
      message: preview[0],
      lines: preview.slice(1)
    }).then(function (accepted) {
      // Declining still resolves the chain with null, which is the value the next
      // step already reads as "nothing was created" while it clears the control.
      if (!accepted) return null;
      return post('/api/app-run/devices/create', {
        previewId: result.preview.previewId,
        acknowledgements: result.preview.acknowledgementCodes,
        idempotencyKey: idempotency()
      });
    });
  }).then(function (result) {
    busy(control, false);
    if (!result) return;
    state.pendingCreatedRun = state.selection.runAfterCreation === true;
    state.createdRefreshAttempts = 0;
    state.menuOpen = false; state.drawerOpen = true;
    requestFocus('drawer');
    refresh(false);
  }).catch(function (error) { busy(control, false); showError(errorText(error)); });
}
function cancel(control) {
  var job = state.status && state.status.job;
  if (!job) return;
  busy(control, true);
  post('/api/app-run/cancel', {
    jobId: job.jobId, expectedStateRevision: job.jobRevision, idempotencyKey: idempotency()
  }).then(function () {
    busy(control, false); refresh(false);
  }).catch(function (error) { busy(control, false); showError(errorText(error)); });
}
function stop(control) {
  var session = state.status && state.status.session;
  if (!session) return;
  // Stopping kills a running app, so the dialog is the danger one — Cancel holds the
  // focus and Enter cannot confirm by reflex. The request waits for the answer; a
  // declined stop stays the same no-op it was, with the control never disabled.
  confirmDialog({
    title: t('appRun.stop'),
    message: t('appRun.confirmStop'),
    confirmLabel: t('appRun.stop'),
    danger: true
  }).then(function (accepted) {
    if (!accepted) return;
    busy(control, true);
    post('/api/app-run/stop', {
      sessionId: session.sessionId, expectedSessionRevision: session.sessionRevision, idempotencyKey: idempotency()
    }).then(function () {
      busy(control, false); refresh(false);
    }).catch(function (error) { busy(control, false); showError(errorText(error)); });
  });
}
function restart(token, basis, control) {
  if (!(state.status && state.status.session)) return;
  busy(control, true);
  var ready = basis ? Promise.resolve(basis) : refresh(true).then(function () {
    var session = state.status && state.status.session;
    return session && state.targets ? { session: session, targets: state.targets } : null;
  });
  ready.then(function (current) {
    if (!current) return null;
    return post('/api/app-run/restart', {
      sessionId: current.session.sessionId,
      expectedSessionRevision: current.session.sessionRevision,
      buildMode: state.selection.buildMode || 'if-needed',
      discoveryRevision: current.targets.discoveryRevision,
      expectedProjectSourceRevision: current.targets.projectSourceRevision,
      confirmationToken: token || null, idempotencyKey: idempotency()
    });
  }).then(function (result) {
    busy(control, false);
    if (!result) return;
    state.drawerOpen = true; refresh(false);
  }).catch(function (error) {
    busy(control, false);
    if (error.confirmation && error.confirmation.token) {
      // The basis is read before the question goes up, not after it is answered:
      // window.confirm froze the page, so the retry always described the state the
      // refusal was read from, and a refresh landing meanwhile must not replace it.
      var retryBasis = basis || {
        session: state.status && state.status.session,
        targets: state.targets
      };
      return confirmDialog({
        title: t('appRun.sourceChanged'),
        message: t('appRun.confirmOlder', { date: error.confirmation.builtAt || '' })
      }).then(function (accepted) {
        if (accepted) return restart(error.confirmation.token, retryBasis, control);
        showError(errorText(error));
      });
    }
    showError(errorText(error));
  });
}
function captureScreenshot() {
  var session = state.status && state.status.session;
  // Rejections are rendered through errorCode(error); a bare Error carries neither
  // `code` nor `kind`, so a known state would surface as "unknown reason".
  if (!session) return Promise.reject({ kind: 'session-not-running' });
  return post('/api/app-run/screenshot', {
    sessionId: session.sessionId, expectedSessionRevision: session.sessionRevision,
    taskStem: session.taskStem,
    surfaceId: session.surfaceId,
    idempotencyKey: idempotency()
  }).then(function (result) {
    state.screenshotUrl = '/api/app-run/screenshots/' + result.screenshot.screenshotId; render();
    return result;
  });
}
function screenshot(control) {
  busy(control, true);
  captureScreenshot().then(function () {
    busy(control, false);
  }).catch(function (error) { busy(control, false); showError(errorText(error)); });
}
function logs() {
  var job = state.status && state.status.job;
  if (!job) return;
  appRunLogs.open({
    load: function (cursor) {
      return get('/api/app-run/logs?jobId=' + encodeURIComponent(job.jobId) +
        (cursor ? '&cursor=' + encodeURIComponent(cursor) : ''));
    }
  });
}
function validateTask() {
  var session = state.status && state.status.session;
  if (!session || !session.taskStem) return;
  appRunValidation.open({
    load: function () {
      return get('/api/app-run/validation?taskStem=' + encodeURIComponent(session.taskStem) +
        '&sessionId=' + encodeURIComponent(session.sessionId));
    },
    save: function (body) {
      // A stored receipt changes what the panel reports about this task, so the
      // status is re-read here instead of staying stale until the next event.
      return post('/api/app-run/validation', body).then(function (result) {
        refresh(false);
        return result;
      });
    },
    capture: captureScreenshot,
    error: errorText, idempotency: idempotency
  });
}
function primary() {
  if (state.status && state.status.integrity && !state.status.integrity.ok) {
    state.drawerOpen = true; requestFocus('drawer'); render(); return;
  }
  // `actions` is absent whenever the first status GET failed and a successful start
  // set `state.status = { job }` on its own, so it is guarded exactly like render().
  if (state.status && state.status.job &&
      (state.status.job.state === 'running' || state.status.actions && state.status.actions.canCancel)) {
    state.drawerOpen = true; requestFocus('drawer'); render(); return;
  }
  clearContext();
  if (window.matchMedia && window.matchMedia('(max-width: 640px)').matches) {
    state.menuOpen = true; requestFocus('menu'); refresh(true); render(); return;
  }
  refresh(true).then(function () {
    var preference = state.status && state.status.preferences || {};
    var preferredPlatform = state.targets && state.targets.platforms.find(function (row) {
      return row.id === preference.platform && row.availability !== 'unavailable';
    });
    var preferredTarget = preferredPlatform && preferredPlatform.devices.find(function (device) {
      return device.stableHint === preference.targetStableHint;
    });
    var preferredVariant = preferredPlatform && preferredPlatform.variants.find(function (variant) {
      return variant.id === preference.variantId;
    });
    var preferredMode = ['rebuild', 'if-needed', 'last-build'].indexOf(preference.buildMode) >= 0
      ? preference.buildMode : null;
    if (!state.error && preferredTarget && preferredVariant && preferredMode &&
        state.targets && state.targets.projectSourceRevision) {
      state.selection.platform = preferredPlatform.id;
      state.selection.targetId = preferredTarget.id;
      state.selection.variantId = preferredVariant.id;
      state.selection.buildMode = preferredMode;
      return startRun(null);
    }
    state.menuOpen = true;
    requestFocus('menu');
    render();
  });
}
function render() {
  if (!state.root) return;
  state.root.replaceChildren();
  if (state.error) {
    state.root.appendChild(el('p', {
      class: 'banner banner--warn app-run-refresh-error',
      attrs: { role: 'status' },
      text: state.error
    }));
  }
  if (state.actionError) {
    state.root.appendChild(el('p', {
      class: 'banner banner--warn app-run-refresh-error',
      attrs: { role: 'alert' },
      text: state.actionError
    }));
  }
  var wrap = el('div', { class: 'app-run-split' });
  var active = !!(state.status && state.status.job &&
    (state.status.job.state === 'running' || state.status.actions && state.status.actions.canCancel));
  var primaryButton = el('button', {
    type: 'button', class: 'btn btn--primary app-run-primary' +
      (active ? ' app-run-primary--active' : ''),
    text: state.status && state.status.job && state.status.job.state === 'running' ? t('appRun.running') : t('appRun.runApp'),
    attrs: {
      'aria-label': active ? t('appRun.openRunStatus') : t('appRun.runApp'),
      'aria-haspopup': 'dialog',
      'aria-expanded': state.menuOpen || state.drawerOpen ? 'true' : 'false'
    }
  });
  primaryButton.addEventListener('click', primary);
  var menuButton = el('button', {
    type: 'button', class: 'btn btn--primary app-run-chevron', text: '▾',
    attrs: { 'aria-label': t('appRun.options'), 'aria-haspopup': 'dialog', 'aria-expanded': state.menuOpen ? 'true' : 'false' }
  });
  menuButton.addEventListener('click', function () {
    var opening = !state.menuOpen;
    state.menuOpen = opening;
    if (opening) {
      clearContext();
      requestFocus('menu');
    } else {
      requestFocus('primary');
    }
    render();
    if (state.menuOpen) refresh(false);
  });
  wrap.appendChild(primaryButton); wrap.appendChild(menuButton); state.root.appendChild(wrap);
  if (state.menuOpen && state.targets) {
    state.root.appendChild(appRunMenu.render({
      targets: state.targets, selection: state.selection, context: state.context,
      onClose: function () {
        state.menuOpen = false; clearContext(); requestFocus('primary'); render();
      },
      onSelection: mergeSelection, onRefresh: function () { refresh(true); },
      onRun: function (control) { startRun(null, control); }, onCreate: createDevice
    }));
  }
  if (state.drawerOpen) {
    state.root.appendChild(appRunDrawer.render({
      status: state.status, screenshotUrl: state.screenshotUrl,
      onClose: function () {
        state.drawerOpen = false; requestFocus('primary'); render();
      },
      onCancel: cancel, onStop: stop, onRestart: function (control) { restart(null, null, control); },
      onScreenshot: screenshot, onLogs: logs, onValidate: validateTask
    }));
  }
  if (state.pendingFocus) {
    var requested = state.pendingFocus;
    queueMicrotask(function () {
      if (!state.root || state.pendingFocus !== requested) return;
      var selector = requested === 'menu'
        ? '.app-run-menu select:not([disabled]), .app-run-menu button:not([disabled])'
        : requested === 'drawer'
          ? '.app-run-drawer button:not([disabled]), .app-run-drawer a[href]'
          : '.app-run-primary';
      var target = state.root.querySelector(selector);
      if (target) {
        state.pendingFocus = null;
        target.focus();
      }
    });
  }
}
function connect() {
  if (state.events) return;
  state.events = [
    siteEvents.on('open', function () { refresh(false); }),
    siteEvents.on('app-run-status', function () { refresh(false); })
  ];
}
function init() {
  if (state.mounted) return;
  state.root = document.getElementById('app-run-root');
  if (!state.root) return;
  state.mounted = true; render(); connect(); refresh(true);
  document.addEventListener('keydown', function (event) {
    if (event.key !== 'Escape' || (!state.menuOpen && !state.drawerOpen)) return;
    // An open modal <dialog> (logs, validation) owns Escape while it is on top;
    // closing the drawer underneath would remove the surface the dialog came from.
    if (document.querySelector('dialog[open]')) return;
    state.menuOpen = false;
    state.drawerOpen = false;
    clearContext();
    requestFocus('primary');
    render();
  });
  i18n.onChange(render);
}
function open(context) {
  state.context = {
    taskStem: context && context.taskStem || null,
    surfaceId: context && context.surfaceId || null
  };
  state.menuOpen = true; state.drawerOpen = false;
  requestFocus('menu');
  render(); refresh(true);
}

export const appRunControl = { init: init, open: open };
