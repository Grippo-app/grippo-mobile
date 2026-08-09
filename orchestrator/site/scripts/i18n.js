  // ----------------------------------------------------------------------
  // Thin i18n layer for UI strings only. Agent prompts (wizard step bodies,
  // task templates, install commands, verify hints, code samples) STAY in
  // English — this is an invariant of the project (agents work best in en).
  //
  // Dictionaries are registered from scripts/i18n/<lang>.js. Each panel
  // calls i18n.t(key) at render time, so re-render = re-translation.
  // Static HTML (index.html chrome) uses [data-i18n="key"] markers; the
  // applyStatic() pass updates them on init and on every language change.
  // ----------------------------------------------------------------------

  var STORAGE_KEY = 'kmp-wizard-lang';
  var SUPPORTED = ['en', 'ru', 'uk'];
  var DEFAULT = 'en';

  var dicts = {};
  var current = DEFAULT;
  var listeners = [];

  try {
    var saved = localStorage.getItem(STORAGE_KEY);
    if (SUPPORTED.indexOf(saved) >= 0) current = saved;
  } catch (e) {
    // localStorage unavailable; keep default.
  }

  function register(lang, dict) {
    if (SUPPORTED.indexOf(lang) === -1) return;
    if (!dict || typeof dict !== 'object') return;
    dicts[lang] = dicts[lang] || {};
    var keys = Object.keys(dict);
    for (var i = 0; i < keys.length; i++) {
      dicts[lang][keys[i]] = dict[keys[i]];
    }
  }

  function lookup(lang, key) {
    var d = dicts[lang];
    if (!d) return null;
    return Object.prototype.hasOwnProperty.call(d, key) ? d[key] : null;
  }

  function t(key, params) {
    var val = lookup(current, key);
    if (val == null) val = lookup(DEFAULT, key);
    if (val == null) val = key;
    if (params && typeof params === 'object') {
      val = String(val).replace(/\{(\w+)\}/g, function (_, name) {
        return Object.prototype.hasOwnProperty.call(params, name)
          ? String(params[name])
          : '{' + name + '}';
      });
    }
    return val;
  }

  function get() { return current; }

  function set(lang) {
    if (SUPPORTED.indexOf(lang) === -1) return;
    if (lang === current) return;
    current = lang;
    try { localStorage.setItem(STORAGE_KEY, lang); } catch (e) { /* ignore */ }
    applyStatic();
    for (var i = 0; i < listeners.length; i++) {
      try { listeners[i](lang); } catch (e) { /* listener errors must not break the chain */ }
    }
  }

  function onChange(fn) {
    if (typeof fn !== 'function') return function () {};
    listeners.push(fn);
    return function off() {
      var idx = listeners.indexOf(fn);
      if (idx >= 0) listeners.splice(idx, 1);
    };
  }

  // Walks a subtree (default: whole document) and translates anything
  // tagged with [data-i18n] (textContent) or [data-i18n-attr="attr:key; ..."]
  // (attribute values).
  function applyStatic(root) {
    root = root || document;
    if (document.documentElement) {
      document.documentElement.setAttribute('lang', current);
    }
    var textEls = root.querySelectorAll('[data-i18n]');
    for (var i = 0; i < textEls.length; i++) {
      var node = textEls[i];
      var key = node.getAttribute('data-i18n');
      if (key) node.textContent = t(key);
    }
    var attrEls = root.querySelectorAll('[data-i18n-attr]');
    for (var j = 0; j < attrEls.length; j++) {
      var an = attrEls[j];
      var spec = an.getAttribute('data-i18n-attr') || '';
      var parts = spec.split(/\s*;\s*/);
      for (var k = 0; k < parts.length; k++) {
        if (!parts[k]) continue;
        var pieces = parts[k].split(/\s*:\s*/);
        if (pieces.length === 2 && pieces[0] && pieces[1]) {
          an.setAttribute(pieces[0], t(pieces[1]));
        }
      }
    }
  }

  var toggleWired = false;
  function wireToggle(root) {
    // The boot-failure branch calls this on every Retry; without the latch each
    // attempt stacks another click listener and another onChange subscriber.
    if (toggleWired) return;
    toggleWired = true;
    root = root || document;
    var btns = root.querySelectorAll('[data-lang-set]');
    function syncPressed() {
      for (var i = 0; i < btns.length; i++) {
        var b = btns[i];
        var lang = b.getAttribute('data-lang-set');
        b.setAttribute('aria-pressed', lang === current ? 'true' : 'false');
      }
    }
    for (var i = 0; i < btns.length; i++) {
      (function (btn) {
        btn.addEventListener('click', function () {
          set(btn.getAttribute('data-lang-set'));
        });
      })(btns[i]);
    }
    syncPressed();
    onChange(syncPressed);
  }

  export const i18n = {
    t: t,
    get: get,
    set: set,
    register: register,
    onChange: onChange,
    applyStatic: applyStatic,
    wireToggle: wireToggle,
    SUPPORTED: SUPPORTED.slice()
  };
