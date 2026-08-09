import { store } from './store.js';

  var ALLOWED = [];
  var DEFAULT = 'setup';

  var panels = null;
  var mounted = {};
  var sectionEls = {};
  var navBtns = {};
  var activeName = null;

  // Routes may carry a "?key=value" suffix for panel-owned deep links.
  // that the owning panel consumes — only the part before "?" picks the
  // panel. The suffix is left in the URL untouched: the panel reads it
  // during its mount/refresh and strips it itself once consumed.
  function baseOf(raw) {
    var qi = raw.indexOf('?');
    return qi === -1 ? raw : raw.slice(0, qi);
  }

  function resolveHash() {
    var raw = (location.hash || '').replace(/^#/, '');
    var base = baseOf(raw);
    if (ALLOWED.indexOf(base) === -1) {
      // Update the URL so it stays in sync with the resolved default.
      if (raw !== '') {
        try {
          history.replaceState(null, '', '#' + DEFAULT);
        } catch (e) {
          location.hash = DEFAULT;
        }
      }
      return DEFAULT;
    }
    return base;
  }

  // Smart initial landing: with no explicit hash, land on Board once Setup
  // is complete, otherwise Setup. Applied ONCE at load (see resolveInitial);
  // a later setupDone flip via SSE must NOT move the active panel.
  function smartDefault() {
    try {
      var st = store && store.get && store.get();
      if (st && st.progress && st.progress.setupDone === true) return 'board';
    } catch (e) { /* fall through to DEFAULT */ }
    return DEFAULT;
  }

  // Initial-load resolution only. Empty hash -> smartDefault(); invalid hash
  // -> DEFAULT; a valid explicit hash (deep link, with or without a "?"
  // suffix) is honored as-is — the URL keeps the suffix for the panel.
  function resolveInitial() {
    var raw = (location.hash || '').replace(/^#/, '');
    var base = baseOf(raw);
    var pick;
    if (raw === '') {
      pick = smartDefault();
    } else if (ALLOWED.indexOf(base) === -1) {
      pick = DEFAULT;
    } else {
      return base;
    }
    try {
      history.replaceState(null, '', '#' + pick);
    } catch (e) {
      location.hash = pick;
    }
    return pick;
  }

  function cacheElements() {
    for (var i = 0; i < ALLOWED.length; i++) {
      var n = ALLOWED[i];
      sectionEls[n] = document.getElementById('panel-' + n);
    }
    var buttons = document.querySelectorAll('[data-target]');
    for (var j = 0; j < buttons.length; j++) {
      var btn = buttons[j];
      var target = btn.getAttribute('data-target');
      if (ALLOWED.indexOf(target) >= 0) {
        navBtns[target] = navBtns[target] || [];
        navBtns[target].push(btn);
      }
    }
  }

  function show(name) {
    for (var i = 0; i < ALLOWED.length; i++) {
      var n = ALLOWED[i];
      var el = sectionEls[n];
      if (!el) continue;
      if (n === name) {
        el.hidden = false;
      } else {
        el.hidden = true;
      }
    }
    var allBtns = document.querySelectorAll('[data-target]');
    for (var j = 0; j < allBtns.length; j++) {
      var b = allBtns[j];
      if (b.getAttribute('data-target') === name) {
        b.setAttribute('aria-current', 'page');
      } else {
        b.removeAttribute('aria-current');
      }
    }
  }

  function mountOrRefresh(name) {
    if (!panels || !panels[name]) return;
    var sectionEl = sectionEls[name];
    if (!sectionEl) return;
    if (!mounted[name]) {
      panels[name].mount(sectionEl);
      mounted[name] = true;
    } else if (typeof panels[name].refresh === 'function') {
      panels[name].refresh();
    }
  }

  function go(name) {
    if (ALLOWED.indexOf(name) === -1) name = DEFAULT;
    if (location.hash.replace(/^#/, '') !== name) {
      try {
        history.pushState(null, '', '#' + name);
      } catch (e) {
        location.hash = name;
      }
    }
    activeName = name;
    show(name);
    mountOrRefresh(name);
  }

  function current() {
    return activeName;
  }

  function openTarget(target) {
    if (!target || typeof target !== 'object' || typeof target.panel !== 'string') return false;
    var name = target.panel;
    if (ALLOWED.indexOf(name) === -1) return false;
    go(name);
    var panel = panels && panels[name];
    if (panel && typeof panel.openTarget === 'function') panel.openTarget(target.entityId);
    return true;
  }

  function init(panelMap) {
    panels = panelMap || {};
    // registry.js is the single source of panels; derive the allowed route
    // set from the registered map so adding a panel needs no router edit.
    var ids = Object.keys(panels);
    if (ids.length) ALLOWED = ids;
    cacheElements();

    window.addEventListener('hashchange', function () {
      var name = resolveHash();
      activeName = name;
      show(name);
      mountOrRefresh(name);
    });

    var navButtons = document.querySelectorAll('[data-target]');
    for (var i = 0; i < navButtons.length; i++) {
      (function (btn) {
        btn.addEventListener('click', function () {
          go(btn.getAttribute('data-target'));
        });
      })(navButtons[i]);
    }

    var initial = resolveInitial();
    activeName = initial;
    show(initial);
    mountOrRefresh(initial);
  }

  export const router = {
    init: init,
    go: go,
    current: current,
    openTarget: openTarget
  };
