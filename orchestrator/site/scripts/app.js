import { store } from './store.js';
import { router } from './router.js';
import { i18n } from './i18n.js';
import './i18n/en.js'; // side-effect: registers the EN dictionary
import './i18n/ru.js'; // side-effect: registers the RU dictionary
import './i18n/uk.js'; // side-effect: registers the Ukrainian dictionary (UA toggle)
import { statusIndicator } from './status.js';
import { cliStatus } from './cli-status.js';
import { figmaStatus } from './figma-status.js';
import { contractStatus } from './contract-status.js';
import { skillsStatus } from './skills-status.js';
import { sessionsMenu } from './sessions-menu.js';
import { orderedPanels, GROUPS } from './registry.js';
import { errorCode } from './data/request-json.js';
import { appRunControl } from './app-run-control.js';

// ----------------------------------------------------------------------
// Composition root. Wires the server-backed store, the router, i18n, and
// the status indicator. The nav bar and the per-route <section> shells are
// generated from the panel registry (registry.js) so adding a panel touches
// only that one file.
// ----------------------------------------------------------------------

function t(key, params) {
  if (i18n && typeof i18n.t === 'function') {
    return i18n.t(key, params);
  }
  return key;
}

// --- chrome title ------------------------------------------------------
// The tab <title> and sidebar <h1> identify the project this orchestrator
// serves: the basename of the server-reported projectRoot (e.g.
// "my-product"), suffixed with the localized "Orchestrator" descriptor
// ("{project} · Orchestrator"). Owned here rather than via i18n.applyStatic
// because the title is a template with a {project} placeholder applyStatic
// can't fill, and the project name comes from runtime state, not the dict.
// Falls back to the bare descriptor before the first /api/state lands or if
// projectRoot is somehow absent. Re-run on language change.
function projectName() {
  try {
    var root = store.get().projectRoot;
    if (typeof root === 'string') {
      var base = root.replace(/[\\/]+$/, '').split(/[\\/]/).pop();
      if (base) return base;
    }
  } catch (e) { /* fall through to bare descriptor */ }
  return '';
}

function applyChromeTitle() {
  var name = projectName();
  var text = name ? t('chrome.title', { project: name }) : t('chrome.titleBare');
  document.title = text;
  var h1 = document.querySelector('.site-title');
  if (!h1) return;

  // Keep the browser title descriptive, but give the compact sidebar title
  // a clearer visual hierarchy instead of forcing the full template onto one
  // line. aria-label preserves the exact localized accessible name.
  h1.replaceChildren();
  h1.setAttribute('aria-label', text);
  h1.setAttribute('title', text);

  var project = document.createElement('span');
  project.className = 'site-title-project';
  project.textContent = name || t('chrome.titleBare');
  h1.appendChild(project);

  if (name) {
    var descriptor = document.createElement('span');
    descriptor.className = 'site-title-kind';
    descriptor.textContent = t('chrome.titleBare');
    h1.appendChild(descriptor);
  }
}

// --- registry-driven chrome -------------------------------------------
// Build the grouped nav buttons and the route sections from the registry,
// then return the { id -> panel } map the router mounts. Groups render into
// two regions (see GROUPS in registry.js): labelled sections in the
// sidebar body ('main') and the collapsible footer group ('start').
function buildChrome() {
  var panels = orderedPanels();
  var groupsHost = document.getElementById('nav-groups');
  var root = document.getElementById('panel-root');
  var map = {};

  // Resolve each group id to the <nav> its buttons append into. 'main'
  // groups get a generated "<section><h2/><nav/></section>" in the sidebar
  // body; 'start' reuses the footer container from index.html.
  var navFor = {};
  for (var g = 0; g < GROUPS.length; g++) {
    var grp = GROUPS[g];
    if (grp.region === 'start') {
      navFor[grp.id] = document.getElementById('start-nav');
      continue;
    }
    if (!groupsHost) continue;
    var section = document.createElement('section');
    section.className = 'nav-group';
    if (grp.labelKey) {
      var title = document.createElement('h2');
      title.className = 'nav-group-title';
      title.setAttribute('data-i18n', grp.labelKey);
      title.textContent = t(grp.labelKey);
      section.appendChild(title);
    }
    var nav = document.createElement('nav');
    nav.className = 'sidebar-nav';
    nav.setAttribute('aria-label', grp.id);
    if (grp.labelKey) nav.setAttribute('data-i18n-attr', 'aria-label:' + grp.labelKey);
    section.appendChild(nav);
    groupsHost.appendChild(section);
    navFor[grp.id] = nav;
  }

  for (var i = 0; i < panels.length; i++) {
    var p = panels[i];
    map[p.id] = p.panel;
    var dest = navFor[p.group];
    if (dest) {
      var btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'nav-btn';
      btn.setAttribute('data-target', p.id);
      btn.setAttribute('data-i18n', p.navLabelKey);
      btn.textContent = t(p.navLabelKey);
      dest.appendChild(btn);
    }
    if (root && !document.getElementById('panel-' + p.id)) {
      var routeSection = document.createElement('section');
      routeSection.id = 'panel-' + p.id;
      routeSection.className = 'panel';
      routeSection.hidden = true;
      root.appendChild(routeSection);
    }
  }
  return map;
}

// Phase-aware collapse for the footer "Start" group (Setup + Launch Wizard).
// Rules, in order:
//   1. On a Start route (setup/wizard): force open — never hide the button
//      for the page you're on — and re-arm the collapse for when you leave.
//   2. Setup not done (onboarding, or it regressed): keep open & discoverable.
//   3. Setup done, viewing elsewhere: collapse ONCE (the `latch`), then leave
//      the user's manual toggle alone on later SSE ticks (don't fight them).
// The collapse latches rather than tracking the setupDone transition, because
// that transition usually fires while the user is still ON the Setup panel
// (rule 1) — so the collapse must trigger on the first non-Start view AFTER
// done, not on the flip itself. This only toggles <details open>; it never
// navigates, preserving the router invariant that a late setupDone flip must
// not move the active panel.
var startCollapsedForDone = false;
function syncStartGroup() {
  var details = document.querySelector('.start-group');
  if (!details) return;
  var hash = (location.hash || '').replace(/^#/, '').split('?')[0];
  var done = false;
  try {
    var st = store.get();
    done = !!(st && st.progress && st.progress.setupDone === true);
  } catch (e) { /* default to expanded */ }

  if (hash && document.querySelector('#start-nav [data-target="' + hash + '"]')) {
    details.open = true;
    startCollapsedForDone = false;
    return;
  }
  if (!done) {
    startCollapsedForDone = false;
    details.open = true;
    return;
  }
  if (!startCollapsedForDone) {
    details.open = false;
    startCollapsedForDone = true;
  }
}

// --- boot --------------------------------------------------------------
function start() {
  // The store fetches the initial snapshot from /api/state. Panels mount
  // only after the first fetch lands so they see canonical server-derived
  // state, not the empty defaults.
  var _root = document.getElementById('panel-root');
  var _bl = document.createElement('p');
  _bl.id = '_boot-loading';
  _bl.style.cssText = 'padding:2rem;color:var(--muted)';
  _bl.textContent = '…';
  if (_root) _root.appendChild(_bl);
  store.load().then(function () {
    var el = document.getElementById('_boot-loading');
    if (el && el.parentNode) el.parentNode.removeChild(el);
    // Adopt the server-persisted language BEFORE the chrome/panels mount, so
    // the first paint is already in the right language. localStorage is only
    // a per-origin boot cache — an explicitly changed port changes the origin and
    // wipes it, so the server value (uiLang) is the durable copy. The
    // persist-on-toggle hook below is not registered yet, so this cannot
    // echo a POST back to the server.
    if (i18n) {
      var serverLang = store.get().uiLang;
      if (serverLang && i18n.SUPPORTED.indexOf(serverLang) >= 0) {
        i18n.set(serverLang); // no-op when it already matches the local cache
      }
    }
    applyChromeTitle();
    var panelMap = buildChrome();
    router.init(panelMap);
    syncStartGroup();
    window.addEventListener('hashchange', syncStartGroup);
    // Collapse Start the moment setupDone flips (SSE). Toggles <details>
    // only — never navigates (keeps the router's active panel put).
    store.on('change', syncStartGroup);

    if (statusIndicator && typeof statusIndicator.init === 'function') {
      statusIndicator.init();
    }
    if (cliStatus && typeof cliStatus.init === 'function') {
      cliStatus.init();
    }
    if (figmaStatus && typeof figmaStatus.init === 'function') {
      figmaStatus.init();
    }
    if (contractStatus && typeof contractStatus.init === 'function') {
      contractStatus.init();
    }
    if (skillsStatus && typeof skillsStatus.init === 'function') {
      skillsStatus.init();
    }
    if (sessionsMenu && typeof sessionsMenu.init === 'function') {
      sessionsMenu.init();
    }
    if (appRunControl && typeof appRunControl.init === 'function') {
      appRunControl.init();
    }

    // i18n: translate the static chrome, wire the EN/RU/UA toggle, and
    // re-render the active panel whenever the user switches language.
    // Other (already-mounted but hidden) panels re-render naturally the
    // next time the user navigates to them.
    if (i18n) {
      i18n.applyStatic();
      applyChromeTitle();
      i18n.wireToggle();
      i18n.onChange(function (lang) {
        // Persist the toggle server-side (fire-and-forget; store toasts on
        // failure). Boot-time adoption above ran before this registration,
        // so every onChange here is a real user toggle — no echo loop.
        store.saveUiLang(lang);
        i18n.applyStatic();
        applyChromeTitle();
        var active = router.current();
        if (active && panelMap[active] &&
            typeof panelMap[active].refresh === 'function') {
          panelMap[active].refresh();
        }
      });
    }
  }, function (error) {
    var code = errorCode(error);
    var key = 'common.requestError.' + code;
    var message = t(key);
    if (message === key) message = t('common.requestError.unknown');
    if (!_bl) return;
    // The boot failure screen is the whole UI in this state: translate the
    // chrome around it and wire the language toggle, which otherwise never runs
    // because both live in the success branch only.
    if (i18n) {
      i18n.applyStatic();
      applyChromeTitle();
      i18n.wireToggle();
    }
    // role first: assistive tech does not announce text set before the live
    // region exists.
    _bl.setAttribute('role', 'alert');
    _bl.textContent = message + ' ';
    var retry = document.createElement('button');
    retry.type = 'button';
    retry.className = 'btn btn--secondary btn--small';
    retry.textContent = t('common.retry');
    retry.addEventListener('click', function () {
      if (_bl.parentNode) _bl.parentNode.removeChild(_bl);
      start();
    });
    _bl.appendChild(retry);
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', start);
} else {
  start();
}
