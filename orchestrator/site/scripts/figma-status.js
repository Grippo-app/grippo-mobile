import { i18n } from './i18n.js';
import { store } from './store.js';
import { dom } from './dom.js';
import { tasksApi } from './data/tasks-api.js';
import { popovers } from './popovers.js';
import { parseFileKey } from './figma-actions.js';
import { clipboard } from './clipboard.js';
import { figmaActionError } from './panels/figma.js';

// ----------------------------------------------------------------------
// Header Figma-connector indicator. State comes from store.get().figma
// (server/figma.js), a cached probe of `claude mcp list`:
//   { state: 'connected'|'needs-auth'|'local-absent'|'misconfigured'|'cli-missing'|'unknown',
//     local: {present,status}, global: {present,name,status}, account, checkedAt }
//   state reflects THIS project's local "figma" server; global is any competing
//   account-level connector; account is the bound Figma identity (whoami).
//
// Binding + OAuth happen in the Figma tab (via /mcp) — not something this pill
// can complete itself. So it reports status + the bound account, flags a global
// conflict, and offers a "Re-check" plus a jump to the tab. See panels/figma.js.
// ----------------------------------------------------------------------

function t(key, params) {
  return (i18n && typeof i18n.t === 'function') ? i18n.t(key, params) : key;
}
function el(tag, attrs, kids) { return dom.el(tag, attrs, kids); }

function figmaOf() {
  var s = (store && typeof store.get === 'function') ? store.get() : null;
  return (s && s.figma) || null;
}
function integrationOf() {
  var s = (store && typeof store.get === 'function') ? store.get() : null;
  return (s && s.figmaIntegration) || null;
}
// The currently-entered Figma file key (parsed). Mirrors figma-actions.js parseFileKey.
function currentFigmaKey() {
  var s = (store && typeof store.get === 'function') ? store.get() : null;
  var integrationUrl = s && s.figmaIntegration && s.figmaIntegration.projectFile && s.figmaIntegration.projectFile.url;
  return parseFileKey(typeof integrationUrl === 'string' ? integrationUrl : '');
}

// The server contract (server/figma.js) is exactly these known states. Guard the
// passthrough: any value outside the current contract must fall back to 'unknown' rather
// than reach t('figma.pill.'+state) / t('figma.pop.'+state) verbatim and render
// the raw i18n key. Every render path (buildPill label, buildPop, dot data-state,
// popSignature) routes through here, so this one guard covers them all.
var KNOWN_STATES = { 'connected': 1, 'needs-auth': 1, 'local-absent': 1, 'misconfigured': 1, 'cli-missing': 1, 'unknown': 1 };
function inferState(f) {
  if (!f || !f.state) return 'unknown';
  return KNOWN_STATES[f.state] ? f.state : 'unknown';
}

var els = null;   // { container, pill, dot, label, pop }
var open = false;
var lastSig = null;

// SSE fires a store 'change' every ~1.5s; rebuild the popover only when its
// content actually changes, so a Re-check click isn't swept out from under the
// pointer mid-interaction.
function popSignature(f, integration) {
  var g = (f && f.global) || {};
  var a = (f && f.account) || {};
  var gate = integration && integration.syncGate || {};
  // Use the account timestamp, not the connector probe timestamp: whoami changes
  // rarely, and its freshness is part of the text the popover presents.
  return [inferState(f), g.present, g.name, a.handle, a.email,
          a.checkedAt, (f && f.tokensInfo && f.tokensInfo.count), currentFigmaKey(),
          integration && integration.status, integration && integration.reasonCode,
          gate.state, gate.reasonCode].join('|');
}
function hasConflict(f) {
  var l = (f && f.local) || {}, g = (f && f.global) || {};
  return !!(l.present && g.present);
}
function visualState(f, integration) {
  var connector = inferState(f);
  if (hasConflict(f)) return 'conflict';
  if (connector !== 'connected') return connector;
  if (integration && (integration.status === 'needs-attention' || integration.status === 'unavailable')) return 'attention';
  if (integration && (integration.status === 'syncing' || integration.syncGate && integration.syncGate.state === 'blocked')) return 'busy';
  return 'connected';
}

function closeSelf() { setOpen(false); }
function setOpen(next) {
  open = next;
  els.pop.hidden = !open;
  els.pill.setAttribute('aria-expanded', open ? 'true' : 'false');
  if (open) { popovers.open(closeSelf); lastSig = null; render(); }
  else popovers.close(closeSelf);
}

function row(labelKey, valueNodes) {
  return el('div', { class: 'site-status-row' }, [
    el('span', { class: 'site-status-row-label', text: t(labelKey) }),
    el('span', { class: 'site-status-row-value' }, valueNodes)
  ]);
}

function actionButton(labelKey, onClick, opts) {
  var btn = el('button', {
    type: 'button',
    class: 'btn' + (opts && opts.primary ? ' btn--primary' : ''),
    text: t(labelKey)
  });
  btn.addEventListener('click', function (e) { e.stopPropagation(); onClick(); });
  return btn;
}

// Routes to the Figma panel and closes the popover. Uses the hash router so it
// behaves exactly like a nav button (no full reload).
function openFigmaTab() {
  setOpen(false);
  if (location.hash !== '#figma') location.hash = '#figma';
}

function buildPop(f) {
  var state = inferState(f);
  var conflict = hasConflict(f);
  var integration = integrationOf();
  var displayState = visualState(f, integration);
  var fileKey = currentFigmaKey();
  var nodes = [];
  nodes.push(el('div', { class: 'site-status-pop-head' }, [
    el('p', { class: 'site-status-pop-title', text: t('nav.figma') })
  ]));

  var statusClass = state === 'connected' && !conflict
    ? 'site-status-act site-status-act--worker-online'
    : 'site-status-act site-status-act--worker-offline';
  nodes.push(row('figma.pop.mcpLabel', [
    el('span', { class: statusClass, text: t(conflict ? 'figma.pop.conflict' : 'figma.pop.' + state) })
  ]));
  if (fileKey) {
    nodes.push(row('figma.pop.fileLabel', [
      el('code', { text: fileKey })
    ]));
  } else if (state === 'connected') {
    nodes.push(el('p', { class: 'site-status-hint site-status-hint--warn', text: t('figma.pop.fileMissing') }));
  }

  // When connected, show the last whoami snapshot without claiming it is current.
  // Falls back to a "verify in the tab" hint when not recorded.
  if (state === 'connected') {
    var a = f && f.account;
    if (a && (a.handle || a.email)) {
      var who = a.handle || '';
      if (a.email) who += (who ? ' · ' : '') + a.email;
      var when = a.checkedAt ? new Date(a.checkedAt).toLocaleString() : '';
      nodes.push(el('p', { class: 'site-status-hint', text: t('figma.pop.account', { who: who, when: when }) }));
    } else {
      nodes.push(el('p', { class: 'site-status-hint', text: t('figma.pop.accountUnknown') }));
    }
    if (hasConflict(f)) {
      nodes.push(el('p', { class: 'site-status-hint site-status-hint--warn', text: t('figma.pop.hintConflict') }));
    }
  }

  // What's been pulled so far (colors), when known.
  var ti = f && f.tokensInfo;
  if (ti && typeof ti.count === 'number') {
    nodes.push(el('p', { class: 'site-status-hint', text: t('figma.pop.pulled', { n: ti.count }) }));
  }

  // Non-connected states: point at the tab where binding / auth lives.
  if (state === 'needs-auth' || state === 'local-absent') {
    nodes.push(el('p', { class: 'site-status-hint', text: t('figma.pop.hintConnectInTab') }));
  } else if (state === 'cli-missing') {
    nodes.push(el('p', { class: 'site-status-hint', text: t('figma.pop.hintCliMissing') }));
  } else if (state === 'misconfigured') {
    nodes.push(el('p', { class: 'site-status-hint', text: t('figma.pop.hintMisconfigured') }));
  }
  if (displayState === 'attention') {
    nodes.push(el('p', { class: 'site-status-hint site-status-hint--warn', text: t('figma.pop.integrationAttention') }));
  } else if (displayState === 'busy') {
    nodes.push(el('p', { class: 'site-status-hint site-status-hint--warn', text: t('figma.pop.integrationBusy') }));
  }

  var actions = el('div', { class: 'figma-actions' });
  actions.appendChild(actionButton('figma.openTab', openFigmaTab, { primary: true }));
  // A re-check that finds nothing changed rebuilds no popover, so the click had
  // no visible result at all. It now reports itself: disabled while in flight,
  // then a confirmation that the probe was queued.
  var recheck = actionButton('figma.recheck', function () {
    if (recheck.disabled) return;
    recheck.disabled = true;
    tasksApi.figmaRecheck().then(function () {
      recheck.disabled = false;
      clipboard.toast(t('figma.pop.rechecking'));
    }, function (error) {
      recheck.disabled = false;
      clipboard.toastError(figmaActionError(error));
    });
  });
  actions.appendChild(recheck);
  nodes.push(actions);
  return nodes;
}

function render() {
  if (!els) return;
  var f = figmaOf();
  var state = inferState(f);
  var integration = integrationOf();
  var displayState = visualState(f, integration);
  var hasAccount = !!(f && f.account && (f.account.handle || f.account.email));
  var hasFile = !!currentFigmaKey();
  els.dot.setAttribute('data-state', 'figma-' + displayState);
  // A competing global connector keeps state === 'connected', so without this
  // branch the pill claimed "Figma ✓" next to its own red dot.
  var labelKey = displayState === 'conflict' ? 'figma.pill.conflict'
    : displayState === 'attention' ? 'figma.pill.integrationAttention'
    : displayState === 'busy' ? 'figma.pill.busy'
    : state === 'connected' && !hasAccount ? 'figma.pill.connectedUnverified'
    : state === 'connected' && !hasFile ? 'figma.pill.connectedNoFile'
    : 'figma.pill.' + state;
  els.label.textContent = t(labelKey);
  var alert = displayState !== 'connected' || !hasAccount || !hasFile;
  if (els.pill.classList) els.pill.classList.toggle('site-status-pill--alert', alert);
  els.pill.setAttribute('aria-label', t('figma.toggle'));

  if (open) {
    var sig = popSignature(f, integration);
    if (sig !== lastSig) {
      lastSig = sig;
      while (els.pop.firstChild) els.pop.removeChild(els.pop.firstChild);
      var rows = buildPop(f);
      for (var i = 0; i < rows.length; i++) els.pop.appendChild(rows[i]);
    }
  }
}

function init() {
  var container = document.getElementById('site-status');
  if (!container || !dom) return;
  if (els) { render(); return; }

  var dot = el('span', { class: 'site-status-dot', attrs: { 'aria-hidden': 'true' } });
  var label = el('span', { class: 'site-status-label' });
  var pill = el('button', { type: 'button', class: 'site-status-pill', attrs: { 'aria-haspopup': 'true', 'aria-expanded': 'false' } }, [dot, label]);
  var pop = el('div', { class: 'site-status-pop', hidden: true, attrs: { role: 'region' } });

  container.appendChild(el('div', { class: 'site-status-item' }, [pill, pop]));
  els = { container: container, pill: pill, dot: dot, label: label, pop: pop };

  pill.addEventListener('click', function (e) { e.stopPropagation(); setOpen(!open); });
  document.addEventListener('click', function (e) { if (open && !pop.contains(e.target) && e.target !== pill && !pill.contains(e.target)) setOpen(false); });
  document.addEventListener('keydown', function (e) { if (open && (e.key === 'Escape' || e.key === 'Esc')) { setOpen(false); pill.focus(); } });

  if (store && typeof store.on === 'function') store.on('change', render);
  if (i18n && typeof i18n.onChange === 'function') i18n.onChange(render);

  render();
}

export const figmaStatus = { init: init, render: render };
