import { i18n } from './i18n.js';
import { store } from './store.js';
import { dom } from './dom.js';
import { terminal } from './terminal.js';
import { popovers } from './popovers.js';
import { figmaSessionKind } from './figma-session-labels.js';

// ----------------------------------------------------------------------
// Header Sessions menu. Lists every persisted interactive-session context from
// store.sessions and opens the chosen context's terminal — so current and older
// terminals stay reachable from any panel. The pill is permanent: an empty
// history renders an honest empty state instead of making the navigation entry
// disappear. Mounts next to the CLI-status pill in #site-status.
// ----------------------------------------------------------------------

function t(key, params) { return (i18n && typeof i18n.t === 'function') ? i18n.t(key, params) : key; }
function el(tag, attrs, kids) { return dom.el(tag, attrs, kids); }

var els = null;   // { pill, dot, label, pop }
var open = false;

// store.sessions carries every live session plus every valid persisted sidecar
// discovered in .cache/tasks/runs/. Do not scope it to the current board: a
// completed task may have left the board while its transcript is still useful.
function sessionsFromSnapshot(snap) {
  var s = (snap && snap.sessions) || null;
  if (!s) return [];
  var out = [];
  Object.keys(s).forEach(function (k) {
    var v = s[k] || {};
    var running = !!v.running;
    var stem = v.stem || null;
    out.push({ key: k, running: running, awaitingTurn: !!v.awaitingTurn, askedThisTurn: !!v.askedThisTurn,
      closing: !!v.closing, canceled: !!v.canceled, exitCode: (v.exitCode === undefined ? null : v.exitCode),
      stem: stem, action: v.action || null, startedAt: v.startedAt || null, endedAt: v.endedAt || null });
  });
  return out;
}

function liveSessions() {
  var snap = (store && typeof store.get === 'function') ? store.get() : null;
  return sessionsFromSnapshot(snap);
}

// A session is BUSY when a turn is in flight or it paused on a question — the
// only states the header should advertise as "active". A warm-but-idle session
// (turn finished; e.g. the persistent 'setup' context between wizard steps)
// must NOT light the active dot: the terminal for it says "готово"/idle, and a
// pill claiming activity then is exactly the "says running, shows done" lie.
function isBusy(s) { return !!(s.running && (s.awaitingTurn || s.askedThisTurn)); }

// Row status label: distinguish working / paused-on-question / winding-down /
// idle-warm among live sessions, and stopped-by-user / crashed / finished among
// ended ones — so a canceled or crashed run never reads as "finished".
function rowStatus(s) {
  if (s.running && s.awaitingTurn) return t('sessions.running');
  if (s.running && s.askedThisTurn) return t('sessions.awaitingAnswer');
  if (s.running && s.closing) return t('sessions.closing');
  if (s.running) return t('sessions.idle');
  if (s.canceled) return t('sessions.stopped');
  if (typeof s.exitCode === 'number' && s.exitCode !== 0) return t('sessions.failed');
  return t('sessions.finished');
}

function ctxLabel(key) {
  if (key === 'setup') return t('sessions.ctx.setup');
  if (key.indexOf('task:') === 0) return key.slice('task:'.length);
  if (key.indexOf('figma:') === 0) return t('figma.sessionLabel', { kind: figmaSessionKind(key) });
  if (key.indexOf('contract:') === 0) return t('backend.sessionLabel', { kind: key.slice('contract:'.length) });
  if (key.indexOf('skills:') === 0) return t('skills.sessionLabel', { kind: key.slice('skills:'.length) });
  return key;
}

// Sort key for "most recent": a finished session by when it ended, a still-running
// one by when it started. Unparseable/missing → 0 (sorts last among its group).
function recencyOf(s) {
  var ms = Date.parse((s && (s.endedAt || s.startedAt)) || '');
  return isNaN(ms) ? 0 : ms;
}

function closeSelf() { setOpen(false); }
function setOpen(next) {
  open = next;
  els.pop.hidden = !open;
  els.pill.setAttribute('aria-expanded', open ? 'true' : 'false');
  if (open) { popovers.open(closeSelf); renderPop(); }
  else popovers.close(closeSelf);
}

function renderPop() {
  var pop = els.pop;
  while (pop.firstChild) pop.removeChild(pop.firstChild);
  pop.appendChild(el('div', { class: 'site-status-pop-head' }, [
    el('p', { class: 'site-status-pop-title', text: t('sessions.title') })
  ]));
  var list = liveSessions();
  if (!list.length) {
    pop.appendChild(el('p', { class: 'site-status-hint', text: t('sessions.none') }));
    return;
  }
  // Busy first (actionable now), then merely-running, then most-recent — each
  // row opens that context's terminal. The list itself scrolls, so no older
  // persisted context has to be discarded to keep the popover on screen.
  list.sort(function (a, b) {
    if (isBusy(a) !== isBusy(b)) return (isBusy(b) ? 1 : 0) - (isBusy(a) ? 1 : 0);
    if (!!a.running !== !!b.running) return (b.running ? 1 : 0) - (a.running ? 1 : 0);
    return recencyOf(b) - recencyOf(a);
  });
  var listEl = el('div', { class: 'sessions-list' });
  for (var i = 0; i < list.length; i++) {
    (function (s) {
      var row = el('button', { type: 'button', class: 'sessions-row' + (isBusy(s) ? ' sessions-row--running' : '') });
      row.appendChild(el('span', { class: 'sessions-row__dot', attrs: { 'aria-hidden': 'true' } }));
      row.appendChild(el('span', { class: 'sessions-row__label', text: ctxLabel(s.key) }));
      row.appendChild(el('span', { class: 'sessions-row__status', text: rowStatus(s) }));
      row.addEventListener('click', function (e) { e.stopPropagation(); terminal.open(s.key); setOpen(false); });
      listEl.appendChild(row);
    })(list[i]);
  }
  pop.appendChild(listEl);
}

function render() {
  if (!els) return;
  var list = liveSessions();
  // Count only BUSY sessions into the "(N)" active count — a warm-but-idle
  // session (its turn already finished) must not light the header as active.
  var busy = 0;
  for (var i = 0; i < list.length; i++) if (isBusy(list[i])) busy++;
  els.label.textContent = busy > 0 ? t('sessions.pill') + ' (' + busy + ')' : t('sessions.pill');
  els.pill.setAttribute('aria-label', els.label.textContent);
  els.dot.setAttribute('data-state', busy > 0 ? 'sessions-active' : 'sessions-idle');
  if (open) renderPop();
}

function init() {
  var container = document.getElementById('site-status');
  if (!container || !dom) return;
  if (els) { render(); return; }

  var dot = el('span', { class: 'site-status-dot', attrs: { 'aria-hidden': 'true' } });
  var label = el('span', { class: 'site-status-label' });
  var pill = el('button', { type: 'button', class: 'site-status-pill sessions-pill', attrs: { 'aria-haspopup': 'true', 'aria-expanded': 'false' } }, [dot, label]);
  var pop = el('div', { class: 'site-status-pop', hidden: true, attrs: { role: 'region' } });
  container.appendChild(el('div', { class: 'site-status-item' }, [pill, pop]));
  els = { pill: pill, dot: dot, label: label, pop: pop };

  pill.addEventListener('click', function (e) { e.stopPropagation(); setOpen(!open); });
  document.addEventListener('click', function (e) { if (open && !pop.contains(e.target) && e.target !== pill && !pill.contains(e.target)) setOpen(false); });
  document.addEventListener('keydown', function (e) { if (open && (e.key === 'Escape' || e.key === 'Esc')) { setOpen(false); pill.focus(); } });

  if (store && typeof store.on === 'function') store.on('change', render);
  if (i18n && typeof i18n.onChange === 'function') i18n.onChange(render);
  render();
}

export const sessionsMenu = {
  init: init,
  render: render,
  _test: { sessionsFromSnapshot: sessionsFromSnapshot }
};
