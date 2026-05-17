(function () {
  // ----------------------------------------------------------------------
  // "What's next" floating pointer.
  //
  // Reads progress + setup from App.store and renders a single text + link
  // pair. Subscribes to store changes so it stays current without a route
  // change. Persisted under state.progress.pointerHidden — closing it once
  // hides it forever (until the user resets state).
  // ----------------------------------------------------------------------

  function nextPointerInfo(state) {
    var setup = (state && state.setup) || {};
    var progress = (state && state.progress) || {};
    var helpers = window.App && App.helpers;
    var steps = (window.App && App.data && App.data.wizardSteps) || [];
    var done = progress.wizardStepsDone || [];

    if (progress.setupDone !== true) {
      return { text: 'Next: complete Setup.', href: '#setup' };
    }
    if (helpers && typeof helpers.wizardComplete === 'function' &&
        !helpers.wizardComplete(state)) {
      for (var i = 0; i < steps.length; i++) {
        var step = steps[i];
        var skipped = typeof helpers.isAutoSkipped === 'function' &&
          helpers.isAutoSkipped(step, setup);
        if (skipped) continue;
        if (done.indexOf(step.id) >= 0) continue;
        var label = step.id.replace('_', '.');
        // step.title already starts with "Step <id> — "; use the descriptive
        // tail so the pointer doesn't read "Wizard Step 0 — Step 0 — ...".
        var sep = step.title.indexOf('— ');
        var desc = sep >= 0 ? step.title.slice(sep + 2) : step.title;
        return {
          text: 'Next: Wizard Step ' + label + ' — ' + desc + '.',
          href: '#wizard'
        };
      }
      return { text: 'Next: continue the Launch Wizard.', href: '#wizard' };
    }
    if ((progress.taskCounter || 1) === 1) {
      return {
        text: 'Next: write your first task in Task Form.',
        href: '#tasks'
      };
    }
    return {
      text: 'Bootstrap complete. Open Task Form for your next task.',
      href: '#tasks'
    };
  }

  function updatePointer(state) {
    var card = document.getElementById('next-pointer');
    if (!card) return;
    var hidden = state && state.progress && state.progress.pointerHidden === true;
    if (hidden) {
      card.hidden = true;
      document.body.classList.add('no-pointer');
      return;
    }
    document.body.classList.remove('no-pointer');
    card.hidden = false;
    var info = nextPointerInfo(state);
    var textEl = card.querySelector('[data-pointer-text]');
    var linkEl = card.querySelector('[data-pointer-link]');
    if (textEl) textEl.textContent = info.text;
    if (linkEl) linkEl.setAttribute('href', info.href);
  }

  function wirePointer() {
    var card = document.getElementById('next-pointer');
    if (!card) return;
    var closeBtn = card.querySelector('[data-pointer-close]');
    if (closeBtn) {
      closeBtn.addEventListener('click', function () {
        App.store.saveProgress({ pointerHidden: true });
      });
    }
  }

  function wireReset() {
    var btn = document.getElementById('reset-state');
    if (!btn) return;
    btn.addEventListener('click', function () {
      var ok = window.confirm(
        'Reset wizard state? This clears Setup values, wizard checkboxes, and the task counter. The actual requirements/ files are NOT affected.'
      );
      if (!ok) return;
      App.store.reset();
      App.router.go('setup');
    });
  }

  function start() {
    App.store.load();
    App.router.init({
      setup: App.panels.setup,
      wizard: App.panels.wizard,
      agents: App.panels.agents,
      codex: App.panels.codex,
      tasks: App.panels.tasks,
      'tasks-list': App.panels['tasks-list']
    });
    wirePointer();
    wireReset();
    updatePointer(App.store.get());
    App.store.on('change', updatePointer);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start);
  } else {
    start();
  }
})();
