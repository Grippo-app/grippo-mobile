import { i18n } from './i18n.js';
import { store } from './store.js';
import { dom } from './dom.js';
import { popovers } from './popovers.js';
import { tasksApi } from './data/tasks-api.js';
import { terminal } from './terminal.js';
import { clipboard } from './clipboard.js';
import { runErrorMessage } from './run-errors.js';

// ----------------------------------------------------------------------
// Header project-skills indicator. State comes from store.get().skills
// (server/skills.js + state.js):
//   { state: 'ok'|'missing'|'unwired'|'unknown', installed, total,
//     missing:[names], contractsOk, wiring:{inGit,wired,hooksPath,expected,
//     optOut}, runHeld, checkedAt }
// It answers "is the authored skill set actually installed here?" — the
// manifest skills in .claude/skills/, the contracts in .claude/contracts/,
// and the screenshot-gate net (core.hooksPath). While the run-gate holds
// `run` execution (runHeld), THIS pill is the surface that says so and names
// the wiring command — there is no board banner.
// ----------------------------------------------------------------------

function t(key, params) {
  return (i18n && typeof i18n.t === 'function') ? i18n.t(key, params) : key;
}
function el(tag, attrs, kids) { return dom.el(tag, attrs, kids); }

function skillsOf() {
  var s = (store && typeof store.get === 'function') ? store.get() : null;
  return (s && s.skills) || null;
}

// Server contract is exactly these states; anything else (stale server) must
// fall back to 'unknown' rather than render a raw i18n key.
var KNOWN_STATES = { 'ok': 1, 'missing': 1, 'unwired': 1, 'unknown': 1 };
function inferState(s) {
  if (!s || !s.state) return 'unknown';
  return KNOWN_STATES[s.state] ? s.state : 'unknown';
}

// The fix action: a spawned `skills:install` session runs launch Step 14's
// install-skills.sh (installs .claude/skills/ + contracts AND wires the net) —
// the server stays observe-only, the session does the mutation. The pill's
// 10s probe TTL picks the result up on its own once the session lands.
var INSTALL_KEY = 'skills:install';
function installRunning() {
  var s = (store.get().sessions || {})[INSTALL_KEY];
  return !!(s && s.running);
}
var installQueued = false;   // debounce the in-flight POST (mirrors design.js drift buttons)
function installPrompt() {
  return 'Install this project\'s authored skills (launch.md Step 14). From the repo root run:\n\n' +
    '  bash orchestrator/skills/install-skills.sh .\n\n' +
    'Then verify and report:\n' +
    '- every skill in orchestrator/skills/_index/install-manifest.json (minus externalSourceException entries) exists under .claude/skills/<name>/ (SKILL.md + references/)\n' +
    '- .claude/contracts/ exists\n' +
    '- `git config --get core.hooksPath` prints orchestrator/skills/checks/hooks\n\n' +
    'Surface any warning the script prints. Do not commit anything.';
}
function triggerInstall() {
  if (installQueued || installRunning()) { if (clipboard && clipboard.toast) clipboard.toast(t('run.busy')); return; }
  installQueued = true;
  Promise.resolve(tasksApi.sessionStart(INSTALL_KEY, installPrompt())).then(function () {
    installQueued = false;
    if (clipboard && clipboard.toast) clipboard.toast(t('skills.fix.started'));
    lastSig = null; render();
    terminal.open(INSTALL_KEY);
  }, function (error) {
    installQueued = false;
    if (clipboard && clipboard.toast) clipboard.toast(t('skills.fix.failed', { detail: runErrorMessage(error) }));
    lastSig = null; render();
  });
}

var els = null;   // { container, pill, dot, label, pop }
var open = false;
var lastSig = null;

// SSE fires a store 'change' every ~1.5s; rebuild the popover only when its
// content actually changes (mirrors figma-status.js). The locale is part of
// the signature so an EN/RU/UA switch re-translates an already-open popover.
function popSignature(s) {
  var w = (s && s.wiring) || {};
  var locale = (i18n && typeof i18n.get === 'function') ? i18n.get() : '';
  return [locale, inferState(s), s && s.installed, s && s.total,
          (s && s.missing || []).join(','), s && s.contractsOk,
          w.wired, w.optOut, s && s.runHeld,
          installRunning(), installQueued].join('|');
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

function act(ok, text) {
  return el('span', {
    class: 'site-status-act ' + (ok ? 'site-status-act--worker-online' : 'site-status-act--worker-offline'),
    text: text
  });
}

function buildPop(s) {
  var state = inferState(s);
  var w = (s && s.wiring) || null;
  var nodes = [];
  nodes.push(el('div', { class: 'site-status-pop-head' }, [
    el('p', { class: 'site-status-pop-title', text: t('skills.pop.title') })
  ]));

  if (state === 'unknown') {
    nodes.push(el('p', { class: 'site-status-hint site-status-hint--warn', text: t('skills.pop.unknownHint') }));
    return nodes;
  }

  var full = s.installed === s.total;
  nodes.push(row('skills.pop.installedLabel', [act(full, s.installed + '/' + s.total)]));
  if (s.missing && s.missing.length) {
    nodes.push(el('p', { class: 'site-status-hint site-status-hint--warn', text: t('skills.pop.missingList', { list: s.missing.join(', ') }) }));
  }
  nodes.push(row('skills.pop.contractsLabel', [act(!!s.contractsOk, t(s.contractsOk ? 'skills.pop.contractsOk' : 'skills.pop.contractsMissing'))]));
  if (w) {
    nodes.push(row('skills.pop.netLabel', [
      w.optOut ? act(true, t('skills.pop.netOptOut')) : act(!!w.wired, t(w.wired ? 'skills.pop.netWired' : 'skills.pop.netUnwired'))
    ]));
  }

  // One actionable fix line. install-skills.sh installs AND wires, so it is
  // the answer whenever anything is missing; the bare git-config command only
  // when the install is complete and just the net is unwired.
  if (state === 'missing') {
    nodes.push(el('p', { class: 'site-status-hint', text: t('skills.pop.hintInstall') }));
    nodes.push(el('p', { class: 'site-status-hint' }, [el('code', { text: 'orchestrator/skills/install-skills.sh .' })]));
  } else if (state === 'unwired') {
    nodes.push(el('p', { class: 'site-status-hint', text: t('skills.pop.hintWire') }));
    nodes.push(el('p', { class: 'site-status-hint' }, [
      el('code', { text: 'git config core.hooksPath ' + ((w && w.expected) || 'orchestrator/skills/checks/hooks') })
    ]));
  }
  if (s.runHeld) {
    nodes.push(el('p', { class: 'site-status-hint site-status-hint--warn', text: t('skills.pop.runHeld') }));
  }

  // Action button. `install-skills.sh` is the same script in every state: it
  // installs when missing/unwired (Fix), and re-copies .claude/skills/ from the
  // authored orchestrator/skills/ when already complete (Refresh — the on-demand
  // re-sync that reconciles installed-vs-canonical drift, e.g. after skills were
  // edited or synced from the template). While a session runs, the button flips
  // to opening its terminal (input↔session lock rule — same Run→Terminal flip as
  // run-control). `state` is ok/missing/unwired here (unknown returned early).
  var running = installRunning();
  var actions = el('div', { class: 'figma-actions' });
  var btn;
  if (running) {
    btn = el('button', { type: 'button', class: 'btn btn--terminal', text: t('skills.fix.terminal') });
    btn.addEventListener('click', function (e) { e.stopPropagation(); terminal.open(INSTALL_KEY); });
  } else if (state === 'missing' || state === 'unwired') {
    btn = el('button', { type: 'button', class: 'btn btn--primary', text: t('skills.fix.install') });
    btn.disabled = installQueued;
    btn.addEventListener('click', function (e) { e.stopPropagation(); triggerInstall(); });
  } else {
    btn = el('button', { type: 'button', class: 'btn btn--primary', text: t('skills.fix.refresh') });
    btn.disabled = installQueued;
    btn.addEventListener('click', function (e) { e.stopPropagation(); triggerInstall(); });
  }
  actions.appendChild(btn);
  if (running) actions.appendChild(el('span', { class: 'site-status-hint', text: t('skills.fix.runningNote') }));
  nodes.push(actions);
  return nodes;
}

function render() {
  if (!els) return;
  var s = skillsOf();
  var state = inferState(s);
  // runHeld = the run-gate is actively refusing task runs — the blocking
  // (red) situation; otherwise the state itself picks the dot tone.
  els.dot.setAttribute('data-state', 'skills-' + ((s && s.runHeld && state !== 'ok') ? 'blocked' : state));
  els.label.textContent = t('skills.pill.' + state);
  if (els.pill.classList) els.pill.classList.toggle('site-status-pill--alert', state !== 'ok');
  els.pill.setAttribute('aria-label', t('skills.toggle'));

  if (open) {
    var sig = popSignature(s);
    if (sig !== lastSig) {
      lastSig = sig;
      while (els.pop.firstChild) els.pop.removeChild(els.pop.firstChild);
      var rows = buildPop(s);
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

export const skillsStatus = { init: init, render: render };
