import { i18n } from './i18n.js';
import { store } from './store.js';
import { dom } from './dom.js';
import { clipboard } from './clipboard.js';
import { tasksApi } from './data/tasks-api.js';
import { popovers } from './popovers.js';
import { errorCode } from './data/request-json.js';

// ----------------------------------------------------------------------
// Header CLI-readiness indicator. The whole system runs tasks through the
// `claude` CLI, so the header surfaces whether it's installed + logged in,
// with one-click Install / Login. Both are global per machine (one login
// covers every project). State comes from store.get().cli (server/cli.js):
//   { installed, version, loggedIn, authMethod, subscriptionType, email,
//     jobs: { install: {running,exitCode}|null, login: {running,url}|null } }
//
// Login is interactive: clicking Login spawns `claude auth login`, which opens
// the browser; the user authorizes, copies the code, and pastes it here — we
// relay it to the CLI's stdin. No PTY, no extra deps.
// ----------------------------------------------------------------------

function t(key, params) {
  return (i18n && typeof i18n.t === 'function') ? i18n.t(key, params) : key;
}
function el(tag, attrs, kids) { return dom.el(tag, attrs, kids); }

export function cliErrorMessage(error) {
  var code = errorCode(error);
  var key = 'cli.requestError.' + code;
  var translated = t(key);
  if (translated !== key) return translated;
  var commonKey = 'common.requestError.' + code;
  var common = t(commonKey);
  return common === commonKey ? t('cli.requestError.unknown') : common;
}

function showActionError(error) {
  if (clipboard && typeof clipboard.toast === 'function') clipboard.toast(cliErrorMessage(error));
}

function cliOf() {
  var s = (store && typeof store.get === 'function') ? store.get() : null;
  return (s && s.cli) || null;
}

function inferState(cli) {
  if (!cli) return 'unknown';
  if (!cli.installed) return 'install';
  if (!cli.loggedIn) return 'login';
  // Logged in per the keychain, but the stored token is expired/revoked —
  // every real CLI call 401s, so the pill must NOT stay green (server/cli.js
  // sets authProblem from the token's expiresAt + the usage endpoint's status).
  if (cli.authProblem) return 'relogin';
  return 'ready';
}

var els = null;   // { container, pill, dot, label, pop }
var open = false;
var lastSig = null;   // popover-content signature; skip rebuild when unchanged
// Latch so the OAuth code <input> is auto-focused exactly once per entry into
// the login-running state. Reset when we leave that state (in render), so the
// ~1.5s signature-change rebuilds (e.g. when the auth URL is captured) can't
// steal focus back mid-type.
var loginFocused = false;
// A running login job may legitimately change its rendered signature when the
// auth URL arrives. Preserve the in-flight code in module memory so that one
// canonical UI rebuild cannot discard typed text, focus, or the caret.
var loginDraft = '';

// Stable signature of the usage block, so a ~60s usage refresh rebuilds the
// popover but the ~1.5s store churn (unchanged usage) does not.
function usageSig(u) {
  if (!u || !u.windows) return '';
  var parts = [];
  for (var i = 0; i < u.windows.length; i++) parts.push(u.windows[i].key + ':' + u.windows[i].utilization);
  if (u.extra && u.extra.enabled) parts.push('x:' + u.extra.utilization);
  return parts.join(',');
}

// A stable signature of everything the popover renders. SSE fires a store
// 'change' every ~1.5s; without this guard we'd rebuild the popover each time
// and wipe the OAuth-code <input> (and re-enable the Submit button) mid-type.
function popSignature(cli) {
  var ij = cli && cli.jobs && cli.jobs.install;
  var lj = cli && cli.jobs && cli.jobs.login;
  return [inferState(cli), cli && cli.installed, cli && cli.loggedIn, cli && cli.version,
    ij && ij.running, ij && ij.exitCode, lj && lj.running, lj && lj.url,
    cli && cli.subscriptionType, cli && cli.email, usageSig(cli && cli.usage),
    cli && cli.authProblem, cli && cli.authExpiresAt].join('|');
}

function closeSelf() { setOpen(false); }
function setOpen(next) {
  open = next;
  els.pop.hidden = !open;
  els.pill.setAttribute('aria-expanded', open ? 'true' : 'false');
  if (open) { popovers.open(closeSelf); lastSig = null; render(); }   // force a fresh build on open
  else popovers.close(closeSelf);
}

function row(labelKey, valueNodes, opts) {
  return el('div', { class: 'site-status-row' + (opts && opts.stacked ? ' site-status-row--stacked' : '') }, [
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
  if (opts && opts.disabled) btn.disabled = true;
  btn.addEventListener('click', function (e) { e.stopPropagation(); onClick(); });
  return btn;
}

// Known window keys → i18n labels. Unknown keys (the endpoint may add
// experimental ones) are humanized so "everything reported" still renders.
var WINDOW_LABELS = {
  'five_hour': 'cli.usage.win.fiveHour',
  'seven_day': 'cli.usage.win.weekly',
  'seven_day_opus': 'cli.usage.win.weeklyOpus',
  'seven_day_sonnet': 'cli.usage.win.weeklySonnet',
  'seven_day_oauth_apps': 'cli.usage.win.weeklyApps',
  'seven_day_cowork': 'cli.usage.win.weeklyCowork'
};
function winLabel(key) {
  if (WINDOW_LABELS[key]) return t(WINDOW_LABELS[key]);
  var s = String(key).replace(/^seven_day_?/, '').replace(/_/g, ' ').trim();
  if (!s) s = String(key).replace(/_/g, ' ');
  return s.charAt(0).toUpperCase() + s.slice(1);
}
// Compact reset hint: a time-of-day when within a day, else a short date.
function fmtReset(iso) {
  if (!iso) return '';
  var ms = Date.parse(iso);
  if (isNaN(ms)) return '';
  var diff = ms - Date.now();
  if (diff <= 0) return t('cli.usage.resetsSoon');
  var d = new Date(ms);
  if (diff < 24 * 3600 * 1000) {
    var hh = ('0' + d.getHours()).slice(-2), mm = ('0' + d.getMinutes()).slice(-2);
    return t('cli.usage.resetsAt', { time: hh + ':' + mm });
  }
  var date;
  var loc = (document.documentElement && document.documentElement.lang) || undefined;
  try { date = d.toLocaleDateString(loc, { month: 'short', day: 'numeric' }); }
  catch (e) { date = d.toISOString().slice(0, 10); }
  return t('cli.usage.resetsOn', { date: date });
}
// The usage section: one bar per reported window (remaining % + reset), plus an
// extra-usage line when overage is enabled. Returns null when there's nothing to
// show so the caller appends nothing (hard fallback for the degraded case).
function buildUsage(usage) {
  if (!usage || !usage.windows || !usage.windows.length) return null;
  var box = el('div', { class: 'cli-usage' });
  box.appendChild(el('p', { class: 'cli-usage__title', text: t('cli.usage.title') }));
  for (var i = 0; i < usage.windows.length; i++) {
    var w = usage.windows[i];
    var u = typeof w.utilization === 'number' ? w.utilization : 0;
    if (u < 0) u = 0; if (u > 100) u = 100;
    var remaining = Math.round(100 - u);
    var low = remaining <= 10;
    var item = el('div', { class: 'cli-usage__item' }, [
      el('div', { class: 'cli-usage__head' }, [
        el('span', { class: 'cli-usage__label', text: winLabel(w.key) }),
        el('span', { class: 'cli-usage__pct' + (low ? ' cli-usage__pct--low' : ''), text: t('cli.usage.left', { pct: remaining }) })
      ]),
      el('div', { class: 'cli-usage__bar' }, [
        el('div', { class: 'cli-usage__fill' + (low ? ' cli-usage__fill--low' : ''), attrs: { style: 'width:' + u + '%' } })
      ])
    ]);
    var reset = fmtReset(w.resetsAt);
    if (reset) item.appendChild(el('p', { class: 'cli-usage__reset', text: reset }));
    box.appendChild(item);
  }
  var ex = usage.extra;
  if (ex && ex.enabled) {
    var tail = (typeof ex.utilization === 'number') ? t('cli.usage.left', { pct: Math.round(100 - ex.utilization) }) : t('cli.usage.extraOn');
    box.appendChild(el('p', { class: 'cli-usage__reset', text: t('cli.usage.extra') + ' · ' + tail }));
  }
  return box;
}

// When the stored token expired — a short localized timestamp for the
// explanation line ("expired Jul 5, 21:52"). Empty string when unknown.
function fmtExpiry(iso) {
  if (!iso) return '';
  var ms = Date.parse(iso);
  if (isNaN(ms)) return '';
  var d = new Date(ms);
  var loc = (document.documentElement && document.documentElement.lang) || undefined;
  try { return d.toLocaleString(loc, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }); }
  catch (e) { return d.toISOString().slice(0, 16).replace('T', ' '); }
}

// The interactive login flow (auth URL + paste-the-code box). Shared by the
// signed-out branch and the dead-token re-login branch — the server job is the
// same `claude auth login` child in both cases.
function buildLoginBox(loginJob) {
  var box = el('div', { class: 'cli-login' });
  box.appendChild(el('p', { class: 'site-status-hint', text: t('cli.loginStep1') }));
  if (loginJob.url) {
    box.appendChild(el('a', { class: 'cli-login__link', href: loginJob.url, text: t('cli.loginOpenLink'), attrs: { target: '_blank', rel: 'noopener' } }));
  } else {
    box.appendChild(el('p', { class: 'site-status-hint', text: t('cli.loginOpening') }));
  }
  box.appendChild(el('p', { class: 'site-status-hint', text: t('cli.loginStep2') }));
  var input = el('input', { type: 'text', class: 'input input--compact cli-login__input', value: loginDraft, attrs: { placeholder: t('cli.loginCodePlaceholder'), 'aria-label': t('cli.loginCodePlaceholder'), spellcheck: 'false' } });
  input.addEventListener('click', function (e) { e.stopPropagation(); });
  input.addEventListener('input', function () { loginDraft = input.value; });
  input.addEventListener('keydown', function (e) { if (e.key === 'Enter' || e.keyCode === 13) { submit.click(); } });
  box.appendChild(input);
  // Auto-focus once per login: deferred so the node is appended (render
  // appends after buildPop returns); latched so later rebuilds don't refocus.
  if (!loginFocused) {
    loginFocused = true;
    setTimeout(function () { try { input.focus(); } catch (e) {} }, 0);
  }
  var actions = el('div', { class: 'cli-login__actions' });
  var submit = actionButton('cli.loginSubmit', function () {
    var code = (input.value || '').trim();
    if (!code) { input.focus(); input.setAttribute('placeholder', t('cli.loginCodeEmpty')); return; }
    submit.disabled = true;
    // Re-enable on BOTH success and failure (the server always 200s, so a
    // catch-only chain would leave the button stuck disabled forever).
    tasksApi.cliLoginCode(code).then(function () { submit.disabled = false; }, function (error) {
      submit.disabled = false;
      showActionError(error);
    });
  }, { primary: true });
  actions.appendChild(submit);
  actions.appendChild(actionButton('cli.loginCancel', function () {
    tasksApi.cliLoginCancel().catch(showActionError);
  }));
  box.appendChild(actions);
  return box;
}

function buildPop(cli) {
  var state = inferState(cli);
  var nodes = [];

  nodes.push(el('div', { class: 'site-status-pop-head' }, [
    el('p', { class: 'site-status-pop-title', text: t('cli.title') })
  ]));

  if (state === 'unknown') {
    nodes.push(el('p', { class: 'site-status-hint', text: t('cli.checking') }));
    return nodes;
  }

  // --- Install row -------------------------------------------------------
  var installJob = cli.jobs && cli.jobs.install;
  if (!cli.installed) {
    var installNodes = [el('span', { class: 'site-status-act site-status-act--worker-offline', text: t('cli.notInstalled') })];
    if (installJob && installJob.running) {
      installNodes.push(el('span', { class: 'site-status-hint', text: t('cli.installing') }));
    } else {
      if (installJob && installJob.exitCode != null && installJob.exitCode !== 0) {
        installNodes.push(el('span', { class: 'site-status-warn', text: t('cli.installFailed') }));
      }
      var installBtn = actionButton('cli.installBtn', function () {
        // Optimistic: show "installing…" + lock the button immediately so a slow
        // POST can't be double-clicked. The SSE store update then rebuilds with
        // the real install-job state. On rejection, re-enable + toast (mirrors
        // the login-code submit's both-outcomes handling — the server may not
        // 200 here, so a catch-only chain could leave the button stuck).
        installBtn.disabled = true;
        installBtn.textContent = t('cli.installing');
        tasksApi.cliInstall().then(null, function (error) {
          installBtn.disabled = false;
          installBtn.textContent = t('cli.installBtn');
          showActionError(error);
        });
      }, { primary: true });
      installNodes.push(installBtn);
    }
    nodes.push(row('cli.installLabel', installNodes));
    return nodes;
  }
  nodes.push(row('cli.installLabel', [
    el('span', { class: 'site-status-act site-status-act--worker-online', text: t('cli.installed', { version: cli.version || '' }) })
  ]));

  // --- Login row ---------------------------------------------------------
  var loginJob = cli.jobs && cli.jobs.login;
  if (!cli.loggedIn) {
    if (loginJob && loginJob.running) {
      nodes.push(row('cli.loginLabel', [buildLoginBox(loginJob)], { stacked: true }));
    } else {
      nodes.push(row('cli.loginLabel', [
        el('span', { class: 'site-status-act site-status-act--worker-offline', text: t('cli.notLoggedIn') }),
        actionButton('cli.loginBtn', function () { tasksApi.cliLogin().catch(showActionError); }, { primary: true })
      ]));
    }
    return nodes;
  }

  // --- Logged in per the keychain, but the token is dead (expired/revoked):
  // every real CLI call 401s. Explain why + offer a fresh re-login (server
  // clears the dead credentials first, then the same interactive flow).
  if (cli.authProblem) {
    if (loginJob && loginJob.running) {
      nodes.push(row('cli.loginLabel', [buildLoginBox(loginJob)], { stacked: true }));
    } else {
      var why = cli.authProblem === 'revoked'
        ? t('cli.authRevoked')
        : t('cli.authExpired', { time: fmtExpiry(cli.authExpiresAt) });
      var reNodes = [el('span', { class: 'site-status-act site-status-act--worker-offline', text: t('cli.authBrokenShort') })];
      if (cli.email) reNodes.push(el('span', { class: 'site-status-hint', text: cli.email }));
      reNodes.push(el('span', { class: 'site-status-hint', text: why }));
      reNodes.push(actionButton('cli.reloginBtn', function () { tasksApi.cliLogin(true).catch(showActionError); }, { primary: true }));
      nodes.push(row('cli.loginLabel', reNodes));
    }
    return nodes;
  }

  // --- Ready -------------------------------------------------------------
  var readyNodes = [el('span', { class: 'site-status-act site-status-act--worker-online', text: t('cli.loggedIn') })];
  if (cli.subscriptionType) readyNodes.push(el('span', { class: 'site-status-hint', text: t('cli.plan', { plan: cli.subscriptionType }) }));
  if (cli.email) readyNodes.push(el('span', { class: 'site-status-hint', text: cli.email }));
  nodes.push(row('cli.loginLabel', readyNodes));
  // Subscription usage (5h + weekly windows). Null/degraded → buildUsage returns
  // null and nothing is appended (hard fallback).
  var usageBox = buildUsage(cli.usage);
  if (usageBox) nodes.push(usageBox);
  return nodes;
}

function render() {
  if (!els) return;
  var cli = cliOf();
  // Clear the auto-focus latch whenever the login box isn't showing (popover
  // closed or no running login job), so the NEXT login re-focuses. Keyed on the
  // job alone — NOT loggedIn — because the re-login flow shows the box while
  // the keychain still says logged-in (dead token).
  var loginJobNow = cli && cli.jobs && cli.jobs.login;
  if (!cli || !(loginJobNow && loginJobNow.running)) {
    loginFocused = false;
    loginDraft = '';
  } else if (!open) loginFocused = false;
  var state = inferState(cli);
  els.dot.setAttribute('data-state', 'cli-' + state);
  els.label.textContent = t('cli.pill.' + state);
  var alert = (state === 'install' || state === 'login' || state === 'relogin');
  if (els.pill.classList) els.pill.classList.toggle('site-status-pill--alert', alert);
  els.pill.setAttribute('aria-label', t('cli.toggle'));
  els.pop.setAttribute('aria-label', t('cli.title'));   // keep the region's name in sync with i18n

  if (open) {
    var sig = popSignature(cli);
    if (sig !== lastSig) {            // only rebuild when content actually changed
      var activeLoginInput = els.pop.querySelector('.cli-login__input');
      var restoreLoginFocus = activeLoginInput && document.activeElement === activeLoginInput;
      var selectionStart = restoreLoginFocus ? activeLoginInput.selectionStart : null;
      var selectionEnd = restoreLoginFocus ? activeLoginInput.selectionEnd : null;
      if (activeLoginInput) loginDraft = activeLoginInput.value;
      lastSig = sig;
      while (els.pop.firstChild) els.pop.removeChild(els.pop.firstChild);
      var rows = buildPop(cli);
      for (var i = 0; i < rows.length; i++) els.pop.appendChild(rows[i]);
      if (restoreLoginFocus) {
        var replacement = els.pop.querySelector('.cli-login__input');
        if (replacement) {
          replacement.focus();
          if (typeof replacement.setSelectionRange === 'function' &&
              selectionStart !== null && selectionEnd !== null) {
            replacement.setSelectionRange(selectionStart, selectionEnd);
          }
        }
      }
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
  var pop = el('div', { class: 'site-status-pop', hidden: true, attrs: { role: 'region', 'aria-label': t('cli.title') } });

  container.appendChild(el('div', { class: 'site-status-item' }, [pill, pop]));
  els = { container: container, pill: pill, dot: dot, label: label, pop: pop };

  pill.addEventListener('click', function (e) { e.stopPropagation(); setOpen(!open); });
  document.addEventListener('click', function (e) { if (open && !pop.contains(e.target) && e.target !== pill && !pill.contains(e.target)) setOpen(false); });
  document.addEventListener('keydown', function (e) { if (open && (e.key === 'Escape' || e.key === 'Esc')) { setOpen(false); pill.focus(); } });

  if (store && typeof store.on === 'function') store.on('change', render);
  if (i18n && typeof i18n.onChange === 'function') i18n.onChange(render);

  render();
}

export const cliStatus = { init: init, render: render };
