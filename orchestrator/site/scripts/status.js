import { i18n } from './i18n.js';
import { store } from './store.js';
import { dom } from './dom.js';
import { popovers } from './popovers.js';

  // ----------------------------------------------------------------------
  // Header live-status indicator (Goal 4 — honest live status).
  //
  // A compact pill (dot + label) always visible in the header; clicking it
  // opens a popover with four rows: Dev server / SSE, Queue, Locks, and an
  // INFERRED "Claude activity" reading.
  //
  // /api/state.status carries only absolute timestamps + counts (see
  // server/status.js). This module turns those into relative times and the
  // ACTIVE / IN-PROGRESS / IDLE / UNKNOWN label against the local clock, so
  // the server's SSE change-hash never churns on wall-clock alone. The label
  // is an inference from queue + lock files on disk — never a process claim.
  // ----------------------------------------------------------------------

  var ACTIVE_MS = 2 * 60 * 1000;   // "recent" window for ACTIVE
  var STALE_MS = 20 * 60 * 1000;   // a lock older than this is flagged stale
  var TICK_MS = 5000;              // re-evaluate relative times + label
  // A heartbeat fresher than this means the standby /loop is actively cycling
  // (online). The loop writes one each pass; 90 s tolerates a slow idle cadence
  // without ever falsely reading "online" for a worker that has gone away.
  var WORKER_ONLINE_MS = 90 * 1000;
  // Tolerance for benign worker/page clock skew. A heartbeat dated more than
  // this into the future is treated as not-fresh, so a worker that stamps a far
  // future time and then dies cannot read as "online" indefinitely.
  var WORKER_SKEW_MS = 10 * 1000;

  function t(key, params) {
    if (i18n && typeof i18n.t === 'function') {
      return i18n.t(key, params);
    }
    return key;
  }

  function el(tag, attrs, kids) { return dom.el(tag, attrs, kids); }

  function parseMs(iso) {
    if (!iso) return null;
    var ms = Date.parse(iso);
    return isNaN(ms) ? null : ms;
  }

  // Mirrors board.js's relativeTime thresholds on purpose — kept local so the
  // header doesn't depend on the Board panel.
  function relTime(iso) {
    var ms = parseMs(iso);
    if (ms == null) return t('board.timeUnknown');
    var diff = Date.now() - ms;
    if (diff < 0) diff = 0;
    var sec = Math.round(diff / 1000);
    if (sec < 30) return t('time.justNow');
    if (sec < 60) return t('time.secondsAgo', { s: sec });
    var min = Math.round(sec / 60);
    if (min < 60) return t('time.minutesAgo', { m: min });
    var hr = Math.round(min / 60);
    if (hr < 24) return t('time.hoursAgo', { h: hr });
    var day = Math.round(hr / 24);
    return t('time.daysAgo', { d: day });
  }

  // Inferred from disk state only — never asserts a running process.
  function inferActivity(status) {
    if (!status) return 'unknown';
    var now = Date.now();
    var drained = parseMs(status.queue && status.queue.lastDrainedAt);
    var lastAct = parseMs(status.activity && status.activity.lastActivityAt);
    if (drained != null && now - drained < ACTIVE_MS) return 'active';
    if (!status.locks || status.locks.available !== true) return 'unknown';
    var newestLock = parseMs(status.locks && status.locks.newestStartedAt);
    var lockCount = (status.locks && status.locks.count) || 0;
    if (newestLock != null && now - newestLock < ACTIVE_MS) return 'active';
    if (lockCount > 0) return 'inProgress';
    if (!status.queue || status.queue.available !== true) return 'unknown';
    if (lastAct == null && drained == null) return 'unknown';
    return 'idle';
  }

  // Worker liveness, derived against the local clock from the absolute
  // heartbeat timestamp + active locks. 'busy' wins when a task lock is held
  // AND recently active (the loop stops heart-beating while deep in a run), so
  // a stale heartbeat mid-task never reads as offline — but a lock whose run
  // died long ago (no activity for > STALE_MS) must NOT pin the pill on busy
  // forever and mask the worker-offline alert; it falls through to the
  // heartbeat check. Ages come only from newestActivityAt, the current
  // lock-activity contract; an absent value is unknown rather than inferred.
  // 'online' = recent heartbeat, idle. 'offline' = neither.
  function inferWorker(status) {
    if (!status) return 'offline';
    var locksAvailable = !!status.locks && status.locks.available === true;
    var lockCount = locksAvailable ? ((status.locks && status.locks.count) || 0) : 0;
    if (locksAvailable && lockCount > 0) {
      var act = parseMs(status.locks && status.locks.newestActivityAt);
      if (act != null && Date.now() - act < STALE_MS) return 'busy';
    }
    var hb = parseMs(status.worker && status.worker.heartbeatAt);
    if (hb != null) {
      var age = Date.now() - hb;
      if (age > -WORKER_SKEW_MS && age < WORKER_ONLINE_MS) return 'online';
    }
    if (!locksAvailable) return 'unknown';
    return 'offline';
  }

  function lockUnavailableText(errorCode) {
    if (errorCode === 'runtime-locks-entry-limit') {
      return t('status.locks.entryLimit');
    }
    return t('status.locks.unavailable');
  }

  function queueUnavailableText(errorCode) {
    if (errorCode === 'request-record-invalid' || errorCode === 'request-record-unsafe' ||
        errorCode === 'request-name-invalid') {
      return t('status.queue.invalid');
    }
    if (errorCode === 'request-record-limit' || errorCode === 'request-byte-limit' ||
        errorCode === 'request-directory-entry-limit') {
      return t('status.queue.limit');
    }
    return t('status.queue.unavailable');
  }

  function pendingCount(status) {
    return (status && status.queue && status.queue.pending) || 0;
  }

  // True when the in-process CLI runner drains the queue itself (top-level
  // state field). When so, the /loop worker is unnecessary and the pill shows
  // "runner active" rather than worker heartbeat states.
  function runnerActive() {
    return !!(store && typeof store.get === 'function' && store.get().runnerActive);
  }

  // All runner slots full → "busy" rather than "ready", so a saturated/wedged
  // runner isn't shown as falsely available. Returns true (saturated),
  // false (has capacity), or null (unknown) — `| 0` would have coerced a
  // missing count to 0 and falsely asserted "ready", so guard with finite
  // checks and report unknown when either field is absent/non-numeric.
  function runnerSaturated() {
    var s = (store && typeof store.get === 'function') ? store.get() : null;
    var max = Number(s && s.runnerMax);
    var running = Number(s && s.runnerRunning);
    if (!Number.isFinite(max) || !Number.isFinite(running) || max <= 0) return null;
    return running >= max;
  }

  var els = null;   // { container, pill, dot, label, pop }
  var open = false;

  function row(labelKey, valueNodes) {
    return el('div', { class: 'site-status-row' }, [
      el('span', { class: 'site-status-row-label', text: t(labelKey) }),
      el('span', { class: 'site-status-row-value' }, valueNodes)
    ]);
  }

  function sseText() {
    var conn = (store && typeof store.connection === 'function')
      ? store.connection()
      : { sseReadyState: -1, lastEventAt: null, refreshError: null };
    if (conn.sseReadyState === 1) {
      // An open channel whose refetch is failing is NOT "live": the panels are
      // frozen on the last good snapshot. A closed channel keeps its own wording.
      if (conn.refreshError) return t('status.sse.stale');
      var extra = conn.lastEventAt
        ? ' · ' + relTime(new Date(conn.lastEventAt).toISOString())
        : '';
      return t('status.sse.connected') + extra;
    }
    if (conn.sseReadyState === 0) return t('status.sse.connecting');
    return t('status.sse.offline');
  }

  function buildRows(status) {
    var rows = [];

    rows.push(el('div', { class: 'site-status-pop-head' }, [
      el('p', { class: 'site-status-pop-title', text: t('status.title') }),
      el('p', { class: 'site-status-disclaimer', text: t('status.disclaimer') })
    ]));

    // Worker row first — it's the one actionable signal ("did I start the
    // drainer?"). online / busy / offline with last-beat time and, when
    // offline, a pointer to the Run button that explains how to start it.
    var workerNodes;
    if (runnerActive()) {
      // The in-process CLI runner drains the queue — the /loop worker is moot.
      // saturated: true=full (busy), false=has capacity (online), null=counts
      // unknown — render neutral (offline dot styling) rather than assert ready.
      var rsat = runnerSaturated();
      var rsatTone = rsat === true ? 'busy' : (rsat === false ? 'online' : 'offline');
      workerNodes = [
        el('span', { class: 'site-status-act site-status-act--worker-' + rsatTone,
          text: t(rsat === true ? 'status.worker.runnerBusy' : 'status.worker.runner') }),
        el('span', { class: 'site-status-hint', text: t('status.worker.runnerHint') })
      ];
    } else {
      var worker = inferWorker(status);
      workerNodes = [
        el('span', { class: 'site-status-act site-status-act--worker-' + worker, text: t('status.worker.' + worker) })
      ];
      var hbAt = status && status.worker && status.worker.heartbeatAt;
      if (hbAt) {
        workerNodes.push(el('span', { class: 'site-status-hint', text: t('status.worker.lastSeen', { rel: relTime(hbAt) }) }));
      }
      if (worker === 'offline') {
        workerNodes.push(el('span', { class: 'site-status-hint', text: t('status.worker.offlineHint') }));
      } else if (worker === 'unknown') {
        workerNodes.push(el('span', { class: 'site-status-hint', text: t('status.worker.unknownHint') }));
      }
    }
    rows.push(row('status.worker.label', workerNodes));

    rows.push(row('status.server.label', [
      el('span', { text: t('status.server.up') + ' · ' + sseText() })
    ]));

    var q = (status && status.queue) ||
      { available: false, errorCode: 'request-scan-unavailable', pending: 0, lastDrainedAt: null };
    if (q.available !== true) {
      rows.push(row('status.queue.label', [
        el('span', { class: 'site-status-warn', text: queueUnavailableText(q.errorCode) })
      ]));
    } else {
      var queueText = (q.pending > 0)
        ? t('status.queue.pending', { n: q.pending })
        : t('status.queue.empty');
      queueText += ' · ' + (q.lastDrainedAt
        ? t('status.queue.lastDrained', { rel: relTime(q.lastDrainedAt) })
        : t('status.queue.neverDrained'));
      rows.push(row('status.queue.label', [el('span', { text: queueText })]));
    }

    var lk = (status && status.locks) ||
      { available: false, errorCode: 'runtime-locks-unavailable', count: 0,
        newestStartedAt: null, oldestStartedAt: null, oldestActivityAt: null };
    var lockNodes = [];
    if (lk.available !== true) {
      lockNodes.push(el('span', {
        class: 'site-status-warn',
        text: lockUnavailableText(lk.errorCode)
      }));
    } else if (lk.count > 0) {
      var lockText = t('status.locks.count', { n: lk.count });
      if (lk.newestStartedAt) {
        lockText += ' · ' + t('status.locks.freshest', { rel: relTime(lk.newestStartedAt) });
      }
      lockNodes.push(el('span', { text: lockText }));
      // Stale = the least-recently-ACTIVE lock has been quiet > STALE_MS. Age from
      // oldestActivityAt (server: per-lock max of startedAt & lastActivityAt), so a
      // long-but-live run is not flagged. The current status contract always
      // supplies oldestActivityAt.
      var staleRef = lk.oldestActivityAt;
      var oldMs = parseMs(staleRef);
      if (oldMs != null && Date.now() - oldMs > STALE_MS) {
        lockNodes.push(el('span', {
          class: 'site-status-warn',
          text: t('status.locks.stale', { rel: relTime(staleRef) })
        }));
      }
    } else {
      lockNodes.push(el('span', { text: t('status.locks.none') }));
    }
    rows.push(row('status.locks.label', lockNodes));

    var state = inferActivity(status);
    var actNodes = [
      el('span', { class: 'site-status-act site-status-act--' + state, text: t('status.activity.' + state) }),
      el('span', { class: 'site-status-hint', text: t('status.activity.' + state + 'Hint') })
    ];
    var lastAct = status && status.activity && status.activity.lastActivityAt;
    if (lastAct) {
      actNodes.push(el('span', {
        class: 'site-status-hint',
        text: t('status.activity.lastSeen', { rel: relTime(lastAct) })
      }));
    }
    rows.push(row('status.activity.label', actNodes));

    return rows;
  }

  function render() {
    if (!els) return;
    var status = (store && typeof store.get === 'function')
      ? (store.get().status || null)
      : null;
    // The pill headlines the WORKER — that's the thing the user can act on.
    // When the worker is offline AND requests are waiting, it escalates to an
    // amber "Worker not running" alert: that is exactly the "you clicked Run
    // but nothing will happen" situation.
    if (runnerActive()) {
      // Honest: green "active" only when the runner has free capacity; amber
      // "busy" when all slots are full (so a wedged/saturated runner isn't shown
      // as falsely ready). null = counts unknown — show the neutral (offline)
      // dot rather than asserting either ready or busy on absent data.
      var saturated = runnerSaturated();
      els.dot.setAttribute('data-state', saturated === true ? 'worker-busy' : (saturated === false ? 'worker-online' : 'worker-offline'));
      els.label.textContent = t(saturated === true ? 'status.worker.runnerBusy' : 'status.worker.runner');
      if (els.pill.classList) els.pill.classList.toggle('site-status-pill--alert', false);
    } else {
      var worker = inferWorker(status);
      var alert = (worker === 'offline' && pendingCount(status) > 0);
      els.dot.setAttribute('data-state', alert ? 'worker-alert' : 'worker-' + worker);
      els.label.textContent = alert ? t('status.worker.alert') : t('status.worker.' + worker);
      if (els.pill.classList) els.pill.classList.toggle('site-status-pill--alert', alert);
    }
    els.pill.setAttribute('aria-label', t('status.toggle'));

    if (open) {   // only rebuild the popover when it's actually visible
      while (els.pop.firstChild) els.pop.removeChild(els.pop.firstChild);
      var rows = buildRows(status);
      for (var i = 0; i < rows.length; i++) els.pop.appendChild(rows[i]);
    }
  }

  function closeSelf() { setOpen(false); }
  function setOpen(next) {
    open = next;
    els.pop.hidden = !open;
    els.pill.setAttribute('aria-expanded', open ? 'true' : 'false');
    if (open) { popovers.open(closeSelf); render(); }
    else popovers.close(closeSelf);
  }

  function init() {
    var container = document.getElementById('site-status');
    if (!container || !dom) return;
    if (els) { render(); return; }

    var dot = el('span', { class: 'site-status-dot', attrs: { 'aria-hidden': 'true' } });
    var label = el('span', { class: 'site-status-label' });
    var pill = el('button', {
      type: 'button',
      class: 'site-status-pill',
      attrs: { 'aria-haspopup': 'true', 'aria-expanded': 'false' }
    }, [dot, label]);
    var pop = el('div', { class: 'site-status-pop', hidden: true, attrs: { role: 'region' } });

    container.appendChild(el('div', { class: 'site-status-item' }, [pill, pop]));
    els = { container: container, pill: pill, dot: dot, label: label, pop: pop };

    pill.addEventListener('click', function (e) {
      e.stopPropagation();
      setOpen(!open);
    });
    document.addEventListener('click', function (e) {
      if (open && !pop.contains(e.target) && e.target !== pill && !pill.contains(e.target)) setOpen(false);
    });
    document.addEventListener('keydown', function (e) {
      if (open && (e.key === 'Escape' || e.key === 'Esc')) {
        setOpen(false);
        pill.focus();
      }
    });

    if (store && typeof store.on === 'function') store.on('change', render);
    if (i18n && typeof i18n.onChange === 'function') i18n.onChange(render);
    setInterval(render, TICK_MS);

    render();
  }

  export const statusIndicator = { init: init, render: render };
