import { dom } from './dom.js';
import { i18n } from './i18n.js';
import { store } from './store.js';
import { clipboard } from './clipboard.js';
import { tasksApi } from './data/tasks-api.js';
import { terminal } from './terminal.js';
import { runErrorMessage } from './run-errors.js';

// ----------------------------------------------------------------------
// Run → Terminal control. The single button used by Setup, every Wizard step,
// and every Board task. Its state is derived from the session keyed by context
// (store.get().sessions[key]):
//   not running        → ▶ Run        (start the session + open the terminal)
//   running            → ⊡ Terminal   (reopen the same terminal — no re-send)
//   finished (recent)  → ↻ Run again
// This preserves the same running session when the user closes and reopens its
// terminal.
//
// One module-level store subscription refreshes every live button; buttons
// detached from the DOM are simply not matched by the sweep, so there is no
// per-button listener to leak.
// ----------------------------------------------------------------------

var el = dom.el;
function t(key, params) { return (i18n && typeof i18n.t === 'function') ? i18n.t(key, params) : key; }

var subscribed = false;
function ensureSubscribed() {
  if (subscribed) return;
  subscribed = true;
  if (store && typeof store.on === 'function') store.on('change', refreshAll);
  if (i18n && typeof i18n.onChange === 'function') i18n.onChange(refreshAll);
}
function refreshAll() {
  var btns = document.querySelectorAll('[data-run-control]');
  for (var i = 0; i < btns.length; i++) {
    if (typeof btns[i].__rcUpdate === 'function') btns[i].__rcUpdate();
  }
}

function sessionFor(state, key) {
  var s = state && state.sessions;
  return (s && Object.prototype.hasOwnProperty.call(s, key)) ? s[key] : null;
}

// Default "is this context busy?" — a turn is in flight (awaitingTurn), OR the
// session paused on a needs_action question (askedThisTurn) and is waiting for
// the user's answer: the button must keep pointing at the Terminal then, or a
// Run click would inject the next prompt as the "answer". A session that is
// running but idle (between wizard steps, or after a task's turn finished)
// reads as NOT active, so the button offers Run/Run-again for the next turn
// while the shared child stays warm. The terminal is still reachable from the
// header Sessions menu in that idle window.
function defaultActive(state, key) {
  var s = sessionFor(state, key);
  return !!(s && s.running && (s.awaitingTurn || s.askedThisTurn));
}

// Default Run action — send the prompt as a new turn into a live session
// (carries context across wizard steps), else start a fresh one. The Board
// overrides this to enqueue through the run queue instead.
function defaultRun(key, prompt) {
  var s = sessionFor(store.get(), key);
  if (s && s.running) return tasksApi.sessionSend(key, prompt);
  return tasksApi.sessionStart(key, prompt);
}

// button(opts) → HTMLButtonElement.
//   key            session key ("setup" | "task:<stem>")
//   getPrompt      () => string — the prompt sent on Run
//   onRun          optional (key, prompt) => Promise — default tasksApi.sessionStart;
//                  the Board overrides this to enqueue through the run queue
//   isActive       optional (state) => bool — default "session running"; the
//                  Board widens it to also count a queued request / lock
//   isFinished     optional (state, session, active) => bool — override when a
//                  finished session should switch the label to "Run again"
//   onAfterStart   optional (resp) => void — e.g. the Board closes its modal
//   onError        optional (err) => bool — typed caller recovery; returning
//                  true suppresses the generic failure toast
//   labels         optional { run, terminal, rerun } overrides
//   extraClass     optional class string
//   status         optional () => { done, label } | null — when NOT active, a
//                  truthy { done:true } paints the green "done" variant with
//                  `label` while staying clickable (re-run). Re-read on every
//                  store change via refreshAll, so it flips live.
//   confirm        optional () => Promise<bool> — an async pre-run gate; the run
//                  proceeds only when it resolves truthy (reject/falsy cancels
//                  cleanly). Used for the Board's "screens not pulled" warning.
//   isDisabled     optional (state) => bool — live prerequisite gate while idle;
//                  active sessions stay reachable so Terminal can still reopen.
function button(opts) {
  opts = opts || {};
  var key = opts.key;
  var labels = opts.labels || {};

  var btn = el('button', {
    type: 'button',
    class: 'btn' + (opts.extraClass ? ' ' + opts.extraClass : ''),
    data: { 'run-control': key }
  });

  function state() {
    var st = store.get();
    var sess = sessionFor(st, key);
    var active = typeof opts.isActive === 'function' ? !!opts.isActive(st) : defaultActive(st, key);
    var finished = typeof opts.isFinished === 'function'
      ? !!opts.isFinished(st, sess, active)
      : !active && !!sess && sess.running === false;
    return { active: active, finished: finished };
  }

  function update() {
    if (btn.__rcBusy) return;   // mid-start; leave the "Starting…" label untouched
    var s = state();
    // Only a caller that owns the gate may have its verdict recomputed here.
    // Assigning unconditionally re-ENABLED buttons that Setup/Wizard had gated
    // on form validity: the next SSE tick (their own debounced save) silently
    // reopened ▶ Run on an invalid form, tooltip and all.
    if (typeof opts.isDisabled === 'function') btn.disabled = !s.active && !!opts.isDisabled(store.get());
    if (s.active) {
      btn.textContent = labels.terminal || t('run.terminal');
      btn.setAttribute('data-state', 'terminal');
      btn.classList.add('run-control--active');
      btn.classList.add('btn--terminal');
      btn.classList.remove('run-control--done');
      return;
    }
    btn.classList.remove('run-control--active');
    btn.classList.remove('btn--terminal');
    // A "done" status (e.g. this task's figma screens are already cached) paints
    // the green re-run variant but keeps the button live (click = re-pull). It
    // takes precedence over the transient session-finished label.
    var stat = typeof opts.status === 'function' ? opts.status() : null;
    if (stat && stat.done) {
      btn.textContent = stat.label || labels.rerun || t('run.rerun');
      btn.setAttribute('data-state', 'done');
      btn.classList.add('run-control--done');
      return;
    }
    btn.classList.remove('run-control--done');
    btn.textContent = s.finished ? (labels.rerun || t('run.rerun')) : (labels.run || t('run.run'));
    btn.setAttribute('data-state', s.finished ? 'rerun' : 'run');
  }
  btn.__rcUpdate = update;

  // An optional async pre-run confirm (e.g. the Board's "screens not pulled —
  // run anyway?" gate). The run proceeds only when it resolves truthy; a reject
  // or falsy result cancels cleanly (the user picked Cancel, or chose to pull
  // first). Without opts.confirm this is a direct passthrough to runNow.
  function doRun() {
    if (typeof opts.confirm === 'function') {
      Promise.resolve(opts.confirm()).then(function (ok) { if (ok) runNow(); }, function () {});
      return;
    }
    runNow();
  }

  function runNow() {
    if (typeof opts.isDisabled === 'function' && opts.isDisabled(store.get())) { update(); return; }
    // getPrompt may THROW as a validation signal (the Board's modal buttons
    // throw e.g. "title required"); a throw must cancel the run cleanly and
    // restore the button so the caller can surface its own field error.
    // It may also return null/'' when there is nothing to send. In both cases
    // bail before entering the busy state — never start/enqueue an empty
    // prompt (mirrors board.js enqueueButton).
    var prompt;
    try { prompt = opts.getPrompt ? opts.getPrompt() : ''; }
    catch (e) { update(); return; }
    if (prompt == null || prompt === '') { update(); return; }
    btn.__rcBusy = true;
    btn.disabled = true;
    btn.textContent = t('run.starting');
    var p = typeof opts.onRun === 'function' ? opts.onRun(key, prompt) : defaultRun(key, prompt);
    Promise.resolve(p).then(function (resp) {
      btn.__rcBusy = false;
      btn.disabled = false;
      // Default: open the terminal. When onAfterStart is supplied (the Board),
      // IT decides — it gets an opener so it can open the terminal only when a
      // queue drainer exists, else route to the worker-help modal instead.
      if (typeof opts.onAfterStart === 'function') {
        try { opts.onAfterStart(resp, function () { terminal.open(key); }); } catch (e) {}
      } else {
        // A live session that's mid-turn rejects the send with {sent:false}
        // (server returns {sent, busy}); the prompt was dropped, so don't
        // report success. Still open the terminal so the user sees the state.
        if (resp && resp.sent === false) {
          terminal.open(key);
          update();
          if (clipboard && typeof clipboard.toast === 'function') clipboard.toast(t('run.busy'));
          return;
        }
        terminal.open(key);
      }
      update();
    }, function (err) {
      btn.__rcBusy = false;
      btn.disabled = false;
      update();
      var handled = false;
      if (typeof opts.onError === 'function') {
        try { handled = opts.onError(err) === true; } catch (e) { handled = false; }
      }
      if (handled) return;
      if (clipboard && typeof clipboard.toast === 'function') clipboard.toast(runErrorMessage(err));
    });
  }

  btn.addEventListener('click', function (e) {
    e.preventDefault();
    e.stopPropagation();
    if (btn.getAttribute('data-state') === 'terminal') { terminal.open(key); return; }
    doRun();
  });

  ensureSubscribed();
  update();
  return btn;
}

export const runControl = { button: button };
