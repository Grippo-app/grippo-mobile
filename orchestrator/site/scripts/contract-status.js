import { i18n } from './i18n.js';
import { store } from './store.js';
import { dom } from './dom.js';
import { popovers } from './popovers.js';
import { backendAuthText, backendPillText, backendState } from './backend-labels.js';

// Header indicator for the typed Backend integration. Contract coverage and
// client drift belong to Project -> API and are intentionally not duplicated
// in this integration status popover.

function t(key, params) { return i18n && typeof i18n.t === 'function' ? i18n.t(key, params) : key; }
function el(tag, attrs, kids) { return dom.el(tag, attrs, kids); }
function model() { var state = store && store.get ? store.get() : null; return state && state.backend || null; }
function visualState(value) {
  value = backendState(value);
  if (value === 'refreshing') return 'checking';
  if (value === 'changes-available') return 'changes';
  if (value === 'source-unavailable') return 'stale';
  if (value === 'attention-required') return 'stale';
  if (value === 'needs-setup' || value === 'needs-test') return 'required';
  return value === 'ready' ? 'ok' : 'required';
}
function authLabel(value) { return backendAuthText(value); }
function contractLabel(backend) {
  if (!backend) return t('backend.contract.unknown');
  if (backend.snapshot && backend.snapshot.invalid) return t('backend.contract.invalid');
  if (!backend.preview || !backend.preview.fresh) return backend.snapshot && backend.snapshot.present
    ? t('backend.contract.saved') : t('backend.contract.notSaved');
  var delta = backend.preview.delta || {};
  return delta.added || delta.changed || delta.removed ? t('backend.contract.changed') : t('backend.contract.unchanged');
}
function shortUrl(value) {
  try { var parsed = new URL(value); var out = parsed.origin + parsed.pathname; return out.length > 56 ? out.slice(0, 53) + '…' : out; }
  catch (e) { return value || '—'; }
}

var els = null, open = false, lastSig = null;
function closeSelf() { setOpen(false); }
function setOpen(next) {
  open = next; els.pop.hidden = !open; els.pill.setAttribute('aria-expanded', open ? 'true' : 'false');
  if (open) { popovers.open(closeSelf); lastSig = null; render(); }
  else popovers.close(closeSelf);
}
function row(label, value) {
  return el('div', { class: 'site-status-row' }, [
    el('span', { class: 'site-status-row-label', text: t(label) }),
    el('span', { class: 'site-status-row-value', text: value })
  ]);
}
function openTab(hash) { setOpen(false); if (location.hash !== hash) location.hash = hash; }
function action(label, hash, primary) {
  var button = el('button', { type: 'button', class: 'btn' + (primary ? ' btn--primary' : ''), text: t(label) });
  button.addEventListener('click', function (event) { event.stopPropagation(); openTab(hash); }); return button;
}
function buildPop(backend) {
  var source = backend && backend.source;
  var nodes = [el('div', { class: 'site-status-pop-head' }, [el('p', { class: 'site-status-pop-title', text: t('nav.backend') })])];
  nodes.push(row('backend.field.source', source ? shortUrl(source.url) : t('backend.value.notConfigured')));
  nodes.push(row('backend.field.environment', source ? source.title : '—'));
  nodes.push(row('backend.field.authentication', source ? authLabel(backend && backend.authentication && backend.authentication.state) : '—'));
  nodes.push(row('backend.field.contract', contractLabel(backend)));
  if (backend && backend.snapshot && backend.snapshot.environmentMismatch) {
    nodes.push(el('p', { class: 'site-status-hint site-status-hint--warn', text: t('backend.warning.environmentMismatch') }));
  }
  if (backend && backend.state === 'source-unavailable') {
    nodes.push(el('p', { class: 'site-status-hint site-status-hint--warn', text: t('backend.error.source-unreachable') }));
  }
  if (backend && backend.state === 'attention-required' && backend.diagnostics && backend.diagnostics.lastError) {
    var code = backend.diagnostics.lastError.code, key = 'backend.error.' + code, message = t(key);
    nodes.push(el('p', { class: 'site-status-hint site-status-hint--warn', text: message === key ? t('backend.error.unknown') : message }));
  }
  var actions = el('div', { class: 'figma-actions' }, [
    action('backend.openBackend', '#backend', true), action('backend.openApi', '#api', false)
  ]);
  nodes.push(actions); return nodes;
}
function signature(backend) {
  return JSON.stringify(backend ? { state: backend.state, active: backend.activeEnvironmentId, source: backend.source,
    auth: backend.authentication, preview: backend.preview && { fresh: backend.preview.fresh, delta: backend.preview.delta },
    mismatch: backend.snapshot && backend.snapshot.environmentMismatch,
    error: backend.diagnostics && backend.diagnostics.lastError } : null);
}
function render() {
  if (!els) return;
  var backend = model(), state = backendState(backend && backend.state);
  els.dot.setAttribute('data-state', 'contract-' + visualState(state));
  els.label.textContent = backendPillText(state);
  els.pill.classList.toggle('site-status-pill--alert', ['needs-setup', 'needs-test', 'source-unavailable', 'attention-required'].indexOf(state) >= 0);
  els.pill.setAttribute('aria-label', t('backend.pill.toggle'));
  if (open) {
    var sig = signature(backend);
    if (sig !== lastSig) {
      lastSig = sig; while (els.pop.firstChild) els.pop.removeChild(els.pop.firstChild);
      buildPop(backend).forEach(function (node) { els.pop.appendChild(node); });
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
  els = { pill: pill, dot: dot, label: label, pop: pop };
  pill.addEventListener('click', function (event) { event.stopPropagation(); setOpen(!open); });
  document.addEventListener('click', function (event) { if (open && !pop.contains(event.target) && !pill.contains(event.target)) setOpen(false); });
  document.addEventListener('keydown', function (event) { if (open && event.key === 'Escape') { setOpen(false); pill.focus(); } });
  if (store && store.on) store.on('change', render);
  if (i18n && i18n.onChange) i18n.onChange(render);
  render();
}

export const contractStatus = { init: init, render: render };
