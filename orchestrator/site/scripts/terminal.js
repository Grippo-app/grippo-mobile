import { i18n } from './i18n.js';
import { dom } from './dom.js';
import { store } from './store.js';
import { tasksApi } from './data/tasks-api.js';
import { figmaSessionKind } from './figma-session-labels.js';
import { runErrorMessage } from './run-errors.js';
import { errorCode } from './data/request-json.js';

// ----------------------------------------------------------------------
// Terminal — the ONE interactive console used everywhere (Setup, every Wizard
// step, every Board task). A structured live view of one interactive session
// (server/sessions.js), keyed by CONTEXT: it polls /api/session/events?key=
// and renders each event (Claude's text, tool/file activity, "needs input",
// turn-done) as a row, with an answer box so the user can reply when Claude
// asks. Unsafe/ended task and Figma follow-ups continue server-side with
// read-only tools. NOT a raw terminal — a clean structured panel, no extra deps. All text
// goes in via textContent (never innerHTML), so nothing a session prints can
// inject.
// ----------------------------------------------------------------------

function t(key, params) {
  return (i18n && typeof i18n.t === 'function') ? i18n.t(key, params) : key;
}
function el(tag, attrs, kids) { return dom.el(tag, attrs, kids); }

var POLL_MS = 1000;
var els = null;        // { overlay, body, input, sendBtn, statusEl, spinner, titleEl }
var open = false;
var curKey = null;     // the session key this terminal is currently showing
var lastSeq = 0;
var pollTimer = null;
var gen = 0;           // bumped on reset()/key-switch; stale polls (older gen) are dropped
var pollEpoch = 0;     // bumped on close(); a reopen must not inherit the previous poll chain
var lastStartedAt = null;  // session startedAt last seen; a change = same key restarted
var focusedOnce = false; // focus the input only once per open
var cancelBtn = null;      // kept for disable-during-cancel and re-enable after
var cancelPending = false; // prevents a live Stop request from being re-armed by polling
var tabTrapHandler = null; // document keydown handler; installed on open, removed on close
var hashHandler = null;    // hashchange handler; closes the overlay on navigation (it lives on body)
var i18nOff = null;        // i18n.onChange unsubscribe; re-localizes the open terminal on toggle
var opener = null;         // element focused when the terminal opened; focus returns here on close
var lastStatus = null;     // last status seen; re-applied on language toggle so setStatus re-localizes
var sendFailureCode = null; // sticky until retry/state progress; polling must not erase a delivery refusal

// Human label for a context key. "setup" → the shared title; a task key shows
// its stem (a filename, locale-free).
function titleFor(key) {
  if (key === 'setup') return t('setup.console.title');
  if (key && key.indexOf('task:') === 0) return key.slice('task:'.length);
  if (key && key.indexOf('figma:') === 0) return t('figma.sessionLabel', { kind: figmaSessionKind(key) });
  if (key && key.indexOf('contract:') === 0) return t('backend.sessionLabel', { kind: key.slice('contract:'.length) });
  return t('setup.console.title');
}
function isFigmaSession(key) {
  return !!(key && key.indexOf('figma:') === 0);
}
function isTaskSession(key) {
  return !!(key && key.indexOf('task:') === 0);
}

// Header labels can be much wider than the dialog (task stems in particular
// are intentionally locale-free and contain no natural wrap points). CSS
// truncates them without disturbing the status/Close controls; the native
// title keeps the complete value available on hover.
function setHeaderLabel(element, value) {
  var text = String(value || '');
  element.textContent = text;
  if (text) element.setAttribute('title', text);
  else element.removeAttribute('title');
}

function scrollToBottom() {
  if (els && els.body) els.body.scrollTop = els.body.scrollHeight;
}

var PUBLICATION_EVENT_KEYS = Object.freeze({
  'task-publication-marker-invalid': 'stateInvalid',
  'task-publication-marker-write-failed': 'stateWriteFailed',
  'task-publication-marker-cleanup-failed': 'stateCleanupFailed',
  'task-publication-artifact-unsafe': 'evidenceUnsafe',
  'task-surface-directory-invalid': 'evidenceUnsafe',
  'task-surface-entry-unsafe': 'evidenceUnsafe',
  'task-surface-entry-invalid': 'evidenceInvalid',
  'task-surface-index-invalid': 'evidenceInvalid',
  'task-surface-required-artifact-missing': 'evidenceMissing',
  'task-surface-drift-invalid': 'evidenceInvalid',
  'task-surface-artifact-limit': 'evidenceLimit',
  'task-surface-size-limit': 'evidenceLimit',
  'task-publication-binding-unavailable': 'bindingUnavailable',
  'task-publication-completion-proof-missing': 'proofMissing',
  'task-token-plan-invalid': 'tokenEvidenceInvalid',
  'task-token-health-update-failed': 'tokenHealthFailed',
  'TOKEN_TASK_PLAN_INVALID': 'tokenEvidenceInvalid',
  'TOKEN_TASK_SCREEN_INDEX_UNSAFE': 'tokenEvidenceUnsafe',
  'TOKEN_TASK_SCREEN_INDEX_INVALID': 'tokenEvidenceInvalid',
  'TOKEN_TASK_SCREEN_VARIANT_DUPLICATE': 'tokenEvidenceInvalid',
  'TOKEN_TASK_SCREEN_VARIANT_MISMATCH': 'tokenEvidenceInvalid',
  'TOKEN_TASK_SIDECAR_UNSAFE': 'tokenEvidenceUnsafe',
  'TOKEN_TASK_SIDECAR_INVALID': 'tokenEvidenceInvalid',
  'TOKEN_TASK_SIDECAR_PLAN_MISMATCH': 'tokenEvidenceMismatch',
  'TOKEN_TASK_UNPLANNED_VARIANT': 'tokenEvidenceMismatch'
});
function eventText(ev) {
  if (!ev || !ev.code) return t('setup.console.event.unknown');
  if (ev.code === 'design-publication-failed') {
    var publicationKey = PUBLICATION_EVENT_KEYS[ev.reason] || 'validationFailed';
    return t('setup.console.event.designPublicationFailed.' + publicationKey);
  }
  if (ev.code === 'session-ended') {
    return ev.signal
      ? t('setup.console.event.sessionEndedSignal', { signal: ev.signal })
      : t('setup.console.event.sessionEnded', { code: ev.exitCode == null ? '—' : ev.exitCode });
  }
  var key = 'setup.console.event.' + ev.code;
  var translated = t(key);
  return translated === key ? t('setup.console.event.unknown') : translated;
}
function appendEvent(ev) {
  var kind = ev.kind || 'system';
  var row = el('div', { class: 'terminal__row terminal__row--' + kind });
  if (kind === 'user') {
    row.appendChild(el('span', { class: 'terminal__tag', text: t('setup.console.you') }));
    row.appendChild(el('span', { class: 'terminal__text', text: ev.text }));
  } else if (kind === 'assistant') {
    row.appendChild(el('span', { class: 'terminal__tag', text: t('setup.console.claude') }));
    row.appendChild(el('span', { class: 'terminal__text', text: ev.text }));
  } else if (kind === 'tool') {
    row.appendChild(el('span', { class: 'terminal__tool', text: '⚙ ' + (ev.tool || 'tool') + (ev.text ? ' · ' + ev.text : '') }));
  } else if (kind === 'needs_action') {
    var needsLabel = isFigmaSession(curKey) ? t('figma.console.ownerActionRequired') : t('setup.console.needsInput');
    row.appendChild(el('span', { class: 'terminal__needs', text: '⚠ ' + needsLabel + ' ' + ev.text }));
  } else if (kind === 'result') {
    row.appendChild(el('span', { class: 'terminal__result', text: '✓ ' + t('setup.console.turnDone') }));
  } else {
    // system / exit / error / stderr / rate
    row.appendChild(el('span', { class: 'terminal__muted', text: eventText(ev) }));
  }
  els.body.appendChild(row);
}

// True when the terminal's run is still QUEUED (the request sits in the queue
// and no interactive session is live yet) — opening the terminal on a Run that
// the in-process runner hasn't claimed, or that a /loop worker will drain. In
// that window status.running is false but the run hasn't "stopped"; show
// "waiting to start" instead of the alarming "stopped". Matches task:<stem> by
// stem.
function isQueued(key) {
  if (!key) return false;
  var snap = (store && typeof store.get === 'function') ? store.get() : null;
  var reqs = snap && snap.progress && snap.progress.requests;
  if (!Array.isArray(reqs) || !reqs.length) return false;
  var stem = key.indexOf('task:') === 0 ? key.slice('task:'.length) : null;
  for (var i = 0; i < reqs.length; i++) {
    if (stem && reqs[i] && reqs[i].stem === stem) return true;
  }
  return false;
}

// The status line, from most-specific to least: working (mid-turn) → needs
// your answer (paused on a question) → finishing up (stdin closed, child
// exiting) → idle → queued → how it ended (stopped by user / error code /
// normal end). Every context remains sendable; unsafe task/Figma continuations
// are restricted read-only by the server.
function statusText(status, waiting) {
  var figma = isFigmaSession(curKey);
  if (status && status.queuedInputCount > 0) return t('setup.console.inputQueued');
  if (status && status.running) {
    if (status.awaitingTurn) return t('setup.console.working');
    if (status.askedThisTurn) return t('setup.console.needsAnswer');
    if (status.closing) return t('setup.console.closing');
    return figma ? t('figma.console.actionComplete') : t('setup.console.idle');
  }
  if (waiting) return t('setup.console.waiting');
  if (figma || isTaskSession(curKey)) return t('setup.console.endedReadOnly');
  if (status && status.canceled) return t('setup.console.endedCanceled');
  if (status && typeof status.exitCode === 'number' && status.exitCode !== 0) {
    return t('setup.console.endedError', { code: status.exitCode });
  }
  return t('setup.console.endedResumable');
}

function enableInput() {
  els.input.hidden = false;
  els.sendBtn.hidden = false;
  els.input.disabled = false;
  els.sendBtn.disabled = false;
}

function setStatus(status) {
  lastStatus = status;   // remembered so a language toggle can re-localize the status line
  var running = status && status.running;
  var awaiting = status && status.awaitingTurn;
  var waiting = !running && isQueued(curKey);
  if (awaiting) sendFailureCode = null; // authoritative progress wins over an ambiguous/lost HTTP response
  els.spinner.hidden = !(running && awaiting);
  setHeaderLabel(els.statusEl, sendFailureCode ? runErrorMessage({ kind: sendFailureCode }) : statusText(status, waiting));
  els.statusEl.setAttribute('data-running', running ? '1' : '0');
  // Composition and Send stay available in every state. The server delivers at
  // the next safe boundary, or resumes an ended/unsafe context read-only.
  enableInput();
  // Stop only applies to a live child. A live cancel stays disabled until polling
  // observes the process exit; otherwise a mid-cancel poll could re-arm it.
  if (cancelBtn) {
    if (!running) cancelPending = false;
    cancelBtn.disabled = !running || (running && cancelPending);
  }
  // Every terminal context has the same input rail.
  if (!focusedOnce) {
    focusedOnce = true;
    try { els.input.focus(); } catch (e) {}
  }
}

function poll() {
  if (!open || !curKey) return;
  var myGen = gen;
  var myEpoch = pollEpoch;
  var myKey = curKey;
  tasksApi.sessionEvents(myKey, lastSeq).then(function (r) {
    if (!open || myGen !== gen || myEpoch !== pollEpoch) return;   // closed, or reset()/key-switch happened → drop stale response
    // Same key, fresh startedAt = the session was restarted ("Run again"); its
    // seq is back near 0, so the stale lastSeq would skip every new event. Wipe
    // the prior transcript and re-poll from 0. (reset() clears lastStartedAt, so a
    // key-switch never trips this — only a true restart does.)
    if (r && r.status && r.status.startedAt) {
      if (lastStartedAt && r.status.startedAt !== lastStartedAt) {
        reset(myKey);                  // bumps gen, lastSeq → 0; this stale response is now dropped
        lastStartedAt = r.status.startedAt;   // record FIRST so the immediate re-poll doesn't re-trip the restart branch
        poll();                        // re-fetch from 0 NOW, not after POLL_MS, so the new run's first events aren't missed
        return;
      }
      lastStartedAt = r.status.startedAt;
    }
    if (r && Array.isArray(r.events) && r.events.length) {
      var atBottom = (els.body.scrollTop + els.body.clientHeight >= els.body.scrollHeight - 24);
      for (var i = 0; i < r.events.length; i++) appendEvent(r.events[i]);
      if (atBottom) scrollToBottom();
    }
    if (r && r.status) {
      if (typeof r.status.nextSeq === 'number') lastSeq = r.status.nextSeq;
      setStatus(r.status);
    }
    pollTimer = setTimeout(poll, POLL_MS);
  }, function (error) {
    if (open && myGen === gen && myEpoch === pollEpoch) {
      setHeaderLabel(els.statusEl, runErrorMessage(error));
      pollTimer = setTimeout(poll, 2000);
    }
  });
}

function restoreUnsentText(text) {
  var draft = String(els.input.value || '').trim();
  if (!draft) els.input.value = text;
  else if (draft !== text) els.input.value = text + ' ' + draft;
}

function sendAnswer() {
  if (!curKey) return;
  var myGen = gen;
  var myKey = curKey;
  var text = (els.input.value || '').trim();
  if (!text) { els.input.focus(); return; }
  sendFailureCode = null;
  els.input.value = '';
  tasksApi.sessionSend(myKey, text).then(function (r) {
    if (myGen !== gen || myKey !== curKey) return;   // context switched → drop stale response
    if (r && r.sent === false) {
      restoreUnsentText(text);
      sendFailureCode = 'session-busy';
      if (lastStatus) setStatus(lastStatus);
      else setHeaderLabel(els.statusEl, runErrorMessage({ kind: sendFailureCode }));
    } else {
      sendFailureCode = null;
      setHeaderLabel(els.statusEl, t(r && r.queued ? 'setup.console.inputQueued' : 'setup.console.working')); // optimistic; poll confirms
    }
  }, function (error) {
    if (myGen !== gen || myKey !== curKey) return;
    restoreUnsentText(text);
    sendFailureCode = errorCode(error);
    if (lastStatus) setStatus(lastStatus);
    else setHeaderLabel(els.statusEl, runErrorMessage(error));
  });
}

function build() {
  var overlay = el('div', { class: 'terminal' });
  var panel = el('div', { class: 'terminal__panel', attrs: { role: 'dialog', 'aria-modal': 'true', 'aria-labelledby': 'terminal-title' } });

  var head = el('div', { class: 'terminal__head' });
  var initialTitle = t('setup.console.title');
  var titleEl = el('h3', {
    id: 'terminal-title',
    class: 'terminal__title',
    text: initialTitle,
    attrs: { title: initialTitle }
  });
  head.appendChild(titleEl);
  var spinner = el('span', { class: 'terminal__spinner', hidden: true, attrs: { 'aria-hidden': 'true' } });
  head.appendChild(spinner);
  var statusEl = el('span', { class: 'terminal__status' });
  head.appendChild(statusEl);
  var closeBtn = el('button', { type: 'button', class: 'btn terminal__close', text: t('setup.console.close') });
  closeBtn.addEventListener('click', close);
  head.appendChild(closeBtn);
  panel.appendChild(head);

  var body = el('div', { class: 'terminal__body' });
  panel.appendChild(body);

  var foot = el('div', { class: 'terminal__foot' });
  var input = el('input', { type: 'text', class: 'input terminal__input', maxLength: 60000, attrs: { placeholder: t('setup.console.answerPlaceholder'), spellcheck: 'false', 'aria-label': t('setup.console.answerPlaceholder') } });
  input.addEventListener('keydown', function (e) { if (e.key === 'Enter') { e.preventDefault(); sendAnswer(); } else if (e.key === 'Escape') { e.preventDefault(); close(); } });
  var sendBtn = el('button', { type: 'button', class: 'btn btn--primary', text: t('setup.console.send') });
  sendBtn.addEventListener('click', sendAnswer);
  var stopBtn = el('button', { type: 'button', class: 'btn btn--danger', text: t('setup.console.stop') });
  stopBtn.addEventListener('click', function () {
    if (!curKey) return;
    var myGen = gen;
    var myKey = curKey;
    cancelPending = true;
    stopBtn.disabled = true;
    tasksApi.sessionCancel(myKey).then(function () {
      // poll will update setStatus when the session is gone; leave disabled until then
      if (myGen === gen && myKey === curKey) els.statusEl.textContent = t('setup.console.stopped');
    }, function (error) {
      if (myGen === gen && myKey === curKey) {
        cancelPending = false;
        stopBtn.disabled = false;
        setHeaderLabel(els.statusEl, runErrorMessage(error));
      }
    });
  });
  cancelBtn = stopBtn;
  foot.appendChild(input);
  foot.appendChild(sendBtn);
  foot.appendChild(stopBtn);
  panel.appendChild(foot);

  overlay.appendChild(panel);
  overlay.addEventListener('click', function (e) { if (e.target === overlay) close(); });
  document.body.appendChild(overlay);
  els = { overlay: overlay, panel: panel, body: body, input: input, sendBtn: sendBtn, statusEl: statusEl, spinner: spinner, titleEl: titleEl, closeBtn: closeBtn };
}

// Clear the rendered transcript (a brand-new session for the same key). Bumps
// `gen` to invalidate any in-flight poll from the previous transcript.
function reset(key) {
  gen++;
  sendFailureCode = null;
  cancelPending = false;
  lastSeq = 0;
  lastStartedAt = null;   // next poll re-records startedAt, so a key-switch won't look like a restart
  if (key) curKey = key;
  if (els && els.body) { while (els.body.firstChild) els.body.removeChild(els.body.firstChild); }
}

// Re-apply every baked label after a language toggle because the terminal builds
// its strings once and an open panel must update immediately.
function relocalize() {
  if (!els) return;
  setHeaderLabel(els.titleEl, titleFor(curKey));
  if (lastStatus) setStatus(lastStatus);   // re-derives the status line in the new language
  els.sendBtn.textContent = t('setup.console.send');
  if (cancelBtn) cancelBtn.textContent = t('setup.console.stop');
  els.closeBtn.textContent = t('setup.console.close');
  els.input.setAttribute('placeholder', t('setup.console.answerPlaceholder'));
  els.input.setAttribute('aria-label', t('setup.console.answerPlaceholder'));
}

function openTerminal(key) {
  if (!els) build();
  opener = document.activeElement;   // remembered so focus returns here on close (WCAG 2.4.3)
  // Switching to a different context → wipe the transcript and re-poll from 0.
  if (key && key !== curKey) {
    reset(key);
    // reset() clears the transcript but not the composer: an answer typed for
    // task A would otherwise sit under task B's title and be delivered to B.
    if (els && els.input) els.input.value = '';
  } else if (key) curKey = key;
  if (!curKey) return;
  sendFailureCode = null;
  setHeaderLabel(els.titleEl, titleFor(curKey));
  enableInput();
  // Status is not authoritative until the first poll. Do not expose a stale
  // Stop affordance carried over from the previously viewed context.
  if (cancelBtn) cancelBtn.disabled = true;
  open = true;
  focusedOnce = false;   // focus once the first poll confirms the session is running (setStatus)
  els.overlay.hidden = false;
  if (pollTimer) clearTimeout(pollTimer);

  // Focus trap: keep Tab/Shift+Tab cycling within the panel's focusable elements.
  if (tabTrapHandler) document.removeEventListener('keydown', tabTrapHandler);
  tabTrapHandler = function (e) {
    if (e.key !== 'Tab') return;
    var focusable = Array.prototype.slice.call(
      els.panel.querySelectorAll('input:not([disabled]), button:not([disabled])')
    );
    if (focusable.length === 0) return;
    var first = focusable[0];
    var last = focusable[focusable.length - 1];
    if (e.shiftKey) {
      if (document.activeElement === first) { e.preventDefault(); last.focus(); }
    } else {
      if (document.activeElement === last) { e.preventDefault(); first.focus(); }
    }
  };
  document.addEventListener('keydown', tabTrapHandler);

  // The overlay lives on document.body, not inside any panel, so it would
  // strand over the next view on navigation. Close it when the hash route
  // changes. Re-armed each open; removed in close().
  if (hashHandler) window.removeEventListener('hashchange', hashHandler);
  hashHandler = function () { close(); };
  window.addEventListener('hashchange', hashHandler);

  // Re-localize the open terminal on a language toggle. Subscribed only while
  // open; the returned unsubscribe is called in close().
  if (i18nOff) { try { i18nOff(); } catch (e) {} i18nOff = null; }
  if (i18n && typeof i18n.onChange === 'function') i18nOff = i18n.onChange(relocalize);

  poll();
}

function close() {
  open = false;
  // A reopen on the SAME key skips reset(), so an in-flight poll from before the
  // close would survive and re-arm its own timer next to the new chain —
  // duplicating every transcript line and doubling the polling rate. Bump a
  // dedicated epoch (never `gen`: sendAnswer reads that to restore an unsent draft).
  pollEpoch++;
  if (pollTimer) { clearTimeout(pollTimer); pollTimer = null; }
  if (tabTrapHandler) { document.removeEventListener('keydown', tabTrapHandler); tabTrapHandler = null; }
  if (hashHandler) { window.removeEventListener('hashchange', hashHandler); hashHandler = null; }
  if (i18nOff) { try { i18nOff(); } catch (e) {} i18nOff = null; }
  if (els) els.overlay.hidden = true;
  // Return focus to whatever opened the terminal (WCAG 2.4.3); after hiding so
  // focus doesn't bounce back into the now-hidden panel.
  try { opener && opener.focus && opener.focus(); } catch (e) {}
  opener = null;
}

export const terminal = { open: openTerminal, close: close, reset: reset, eventText: eventText };
