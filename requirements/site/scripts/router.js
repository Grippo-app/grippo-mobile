(function () {
  window.App = window.App || {};

  var ALLOWED = ['setup', 'wizard', 'agents', 'codex', 'tasks'];
  var DEFAULT = 'setup';

  var panels = null;
  var mounted = {};
  var sectionEls = {};
  var navBtns = {};
  var activeName = null;

  function resolveHash() {
    var raw = (location.hash || '').replace(/^#/, '');
    if (ALLOWED.indexOf(raw) === -1) {
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
    return raw;
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

  function init(panelMap) {
    panels = panelMap || {};
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

    var initial = resolveHash();
    activeName = initial;
    show(initial);
    mountOrRefresh(initial);
  }

  window.App.router = {
    init: init,
    go: go,
    current: current
  };
})();
