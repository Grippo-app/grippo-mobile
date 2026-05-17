(function () {
  window.App = window.App || {};

  var STORAGE_KEY = 'kmp-wizard-state';

  function initialState() {
    return {
      version: 1,
      setup: {},
      progress: {
        setupDone: false,
        wizardStepsDone: [],
        taskCounter: 1
      }
    };
  }

  var cache = null;
  var listeners = [];

  function clone(value) {
    return JSON.parse(JSON.stringify(value));
  }

  function persist() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(cache));
    } catch (e) {
      // localStorage unavailable (private mode, quota); fail silently.
    }
  }

  function notify() {
    var snapshot = clone(cache);
    for (var i = 0; i < listeners.length; i++) {
      try {
        listeners[i](snapshot);
      } catch (e) {
        // listener errors must not break the chain
      }
    }
  }

  function load() {
    var raw = null;
    try {
      raw = localStorage.getItem(STORAGE_KEY);
    } catch (e) {
      raw = null;
    }
    if (raw == null) {
      cache = initialState();
      persist();
      return clone(cache);
    }
    var parsed = null;
    try {
      parsed = JSON.parse(raw);
    } catch (e) {
      parsed = null;
    }
    if (!parsed || typeof parsed !== 'object' || parsed.version !== 1 ||
        !parsed.setup || !parsed.progress) {
      cache = initialState();
      persist();
      return clone(cache);
    }
    // Patch any missing fields from initial shape (forward-compatible).
    var seed = initialState();
    if (typeof parsed.progress.setupDone !== 'boolean') {
      parsed.progress.setupDone = seed.progress.setupDone;
    }
    if (!Array.isArray(parsed.progress.wizardStepsDone)) {
      parsed.progress.wizardStepsDone = seed.progress.wizardStepsDone;
    }
    if (typeof parsed.progress.taskCounter !== 'number') {
      parsed.progress.taskCounter = seed.progress.taskCounter;
    }
    cache = parsed;
    return clone(cache);
  }

  function get() {
    if (cache == null) {
      load();
    }
    return clone(cache);
  }

  function saveSetup(partial) {
    if (cache == null) load();
    if (partial && typeof partial === 'object') {
      var keys = Object.keys(partial);
      for (var i = 0; i < keys.length; i++) {
        cache.setup[keys[i]] = partial[keys[i]];
      }
    }
    persist();
    notify();
    return clone(cache);
  }

  function saveProgress(partial) {
    if (cache == null) load();
    if (partial && typeof partial === 'object') {
      var keys = Object.keys(partial);
      for (var i = 0; i < keys.length; i++) {
        cache.progress[keys[i]] = partial[keys[i]];
      }
    }
    persist();
    notify();
    return clone(cache);
  }

  function reset() {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch (e) {
      // ignore
    }
    cache = initialState();
    persist();
    notify();
    return clone(cache);
  }

  function on(event, fn) {
    if (event !== 'change' || typeof fn !== 'function') return function () {};
    listeners.push(fn);
    return function off() {
      var idx = listeners.indexOf(fn);
      if (idx >= 0) listeners.splice(idx, 1);
    };
  }

  window.App.store = {
    load: load,
    get: get,
    saveSetup: saveSetup,
    saveProgress: saveProgress,
    reset: reset,
    on: on
  };
})();
