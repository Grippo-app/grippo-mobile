(function () {
  window.App = window.App || {};
  App.panels = App.panels || {};

  var sectionEl = null;

  // ----------------------------------------------------------------------
  // DOM helper — el() lives in scripts/dom.js (App.dom.el).
  // ----------------------------------------------------------------------

  var el = App.dom.el;

  function codeBlock(text) {
    var wrap = el('div', { class: 'code-block-wrapper' });
    var pre = el('pre', { class: 'code-block' });
    var code = el('code', { text: text });
    pre.appendChild(code);
    wrap.appendChild(pre);
    var btn = el('button', {
      type: 'button',
      class: 'copy-btn',
      text: 'Copy',
      attrs: { 'aria-label': 'Copy to clipboard' }
    });
    App.clipboard.attach(btn, function () { return code.textContent; });
    wrap.appendChild(btn);
    return wrap;
  }

  // ----------------------------------------------------------------------
  // Step-list helpers.
  // ----------------------------------------------------------------------

  function autoSkippedIds(setup) {
    var ids = [];
    var steps = App.data.wizardSteps;
    for (var i = 0; i < steps.length; i++) {
      if (App.helpers.isAutoSkipped(steps[i], setup)) ids.push(steps[i].id);
    }
    return ids;
  }

  function effectiveDoneSet(state) {
    var setup = state.setup || {};
    var stored = (state.progress && state.progress.wizardStepsDone) || [];
    var set = {};
    for (var i = 0; i < stored.length; i++) set[stored[i]] = true;
    var skipped = autoSkippedIds(setup);
    for (var j = 0; j < skipped.length; j++) set[skipped[j]] = true;
    return set;
  }

  function renderStepBody(step, setup) {
    if (typeof step.build === 'function') return step.build(setup);
    return App.templates.render(step.promptTemplate || '', setup);
  }

  // ----------------------------------------------------------------------
  // Single-shot bootstrap prompt: composes the YAML + every non-skipped
  // step body so an agent can drive launch.md end-to-end in one go.
  // Re-generated on every clipboard read so live setup edits are picked up.
  // ----------------------------------------------------------------------

  function buildSingleShotPrompt() {
    var state = App.store.get();
    var setup = state.setup || {};
    var steps = App.data.wizardSteps;
    var parts = [];
    parts.push(
      'You are bootstrapping a new KMP project from requirements/. ' +
      'Execute the steps below end-to-end without pausing between them. ' +
      'Stop only when you hit a blocker from ' +
      'requirements/13-anti-patterns/02-when-to-stop-and-ask.md, ' +
      'then surface the blocker and wait for user input.'
    );
    parts.push('');
    parts.push(App.helpers.buildYaml(setup));
    for (var i = 0; i < steps.length; i++) {
      var step = steps[i];
      if (App.helpers.isAutoSkipped(step, setup)) continue;
      parts.push('\n\n========== STEP ' + step.id + ' ==========\n');
      parts.push(renderStepBody(step, setup));
      var hint = App.templates.render(step.verifyHint || '', setup);
      if (hint) parts.push('Verify: ' + hint);
    }
    parts.push('');
    parts.push("When every step is green and verify passes, report 'Bootstrap complete'.");
    return parts.join('\n');
  }

  // ----------------------------------------------------------------------
  // Event handling.
  // ----------------------------------------------------------------------

  function handleCheckChange(stepId, checked) {
    // Compute the union of user-checked ids + auto-skipped ids from current
    // form state. We re-read every visible checkbox so the persisted array
    // exactly mirrors what the user sees.
    var setup = (App.store.get().setup) || {};
    var unionSet = {};
    var checkboxes = sectionEl.querySelectorAll('input[data-step-check]');
    for (var i = 0; i < checkboxes.length; i++) {
      var cb = checkboxes[i];
      var id = cb.getAttribute('data-step-check');
      if (id === stepId) {
        if (checked) unionSet[id] = true;
      } else if (cb.checked) {
        unionSet[id] = true;
      }
    }
    var skipped = autoSkippedIds(setup);
    for (var s = 0; s < skipped.length; s++) unionSet[skipped[s]] = true;

    var out = [];
    var steps = App.data.wizardSteps;
    for (var k = 0; k < steps.length; k++) {
      if (unionSet[steps[k].id]) out.push(steps[k].id);
    }
    App.store.saveProgress({ wizardStepsDone: out });
    render();
  }

  // ----------------------------------------------------------------------
  // Renderers.
  // ----------------------------------------------------------------------

  function renderGate() {
    var banner = el('div', { class: 'banner banner--warn' });
    banner.appendChild(document.createTextNode('Complete Setup first. '));
    banner.appendChild(el('a', { href: '#setup', text: 'Open Setup' }));
    sectionEl.appendChild(banner);
  }

  function renderStep(step, setup, doneSet, prevDone) {
    var skipped = App.helpers.isAutoSkipped(step, setup);
    var details = el('details', {
      class: 'wizard-step',
      data: { 'step-id': step.id, skipped: skipped ? 'true' : 'false' }
    });

    // Summary row.
    var summary = el('summary');
    summary.appendChild(el('span', { class: 'step-badge', text: step.id.replace('_', '.') }));
    summary.appendChild(el('span', { class: 'step-title', text: step.title }));
    if (skipped) {
      summary.appendChild(el('span', { class: 'step-skip-tag', text: '[skipped — N/A for your setup]' }));
    } else if (doneSet[step.id]) {
      summary.appendChild(el('span', { class: 'step-skip-tag', text: '[done]' }));
    }
    details.appendChild(summary);

    // Body.
    var body = el('div', { class: 'step-body' });

    body.appendChild(el('p', { class: 'step-hook', text: step.hook }));

    body.appendChild(el('h4', { class: 'step-subtitle', text: 'Prompt' }));
    if (skipped) {
      var reason = step.skipReason || 'this step is not applicable to your setup.';
      body.appendChild(el('p', {
        class: 'panel-lead',
        text: 'N/A for your setup (' + reason + ')'
      }));
    } else {
      var promptText = renderStepBody(step, setup);
      body.appendChild(codeBlock(promptText));
    }

    body.appendChild(el('h4', { class: 'step-subtitle', text: 'Verify' }));
    if (skipped) {
      body.appendChild(el('p', { class: 'panel-lead', text: 'No verify check — step is auto-passed.' }));
    } else {
      var hint = App.templates.render(step.verifyHint || '', setup);
      body.appendChild(codeBlock(hint));
    }

    // Checkbox row.
    var checkRow = el('div', { class: 'wizard-step-check' });
    var cb = el('input', {
      type: 'checkbox',
      id: 'step-' + step.id + '-done',
      data: { 'step-check': step.id }
    });
    if (skipped) {
      cb.checked = true;
      cb.disabled = true;
    } else {
      cb.checked = doneSet[step.id] === true;
      cb.disabled = !prevDone;
    }
    cb.addEventListener('change', function () {
      handleCheckChange(step.id, cb.checked);
    });
    var lbl = el('label', { attrs: { 'for': 'step-' + step.id + '-done' } });
    lbl.appendChild(document.createTextNode(' I ran the prompt and the verify check passed.'));
    checkRow.appendChild(cb);
    checkRow.appendChild(lbl);
    body.appendChild(checkRow);

    details.appendChild(body);
    return details;
  }

  function render() {
    if (!sectionEl) return;
    while (sectionEl.firstChild) sectionEl.removeChild(sectionEl.firstChild);

    var state = App.store.get();
    var setupDone = state.progress && state.progress.setupDone === true;

    if (!setupDone) {
      renderGate();
      return;
    }

    var setup = state.setup || {};
    var steps = App.data.wizardSteps;
    var doneSet = effectiveDoneSet(state);

    // Header: progress (label + bar) + action buttons.
    var doneCount = 0;
    for (var d = 0; d < steps.length; d++) if (doneSet[steps[d].id]) doneCount++;
    var pct = steps.length === 0 ? 0 : Math.round((doneCount / steps.length) * 100);

    var header = el('div', { class: 'wizard-header' });

    var progressWrap = el('div', { class: 'wizard-progress-wrap' });
    progressWrap.appendChild(el('div', {
      class: 'wizard-progress',
      text: doneCount + ' / ' + steps.length + ' done'
    }));
    var bar = el('div', { class: 'wizard-progress-bar' });
    var fill = el('div', { class: 'wizard-progress-fill' });
    fill.style.width = pct + '%';
    bar.appendChild(fill);
    progressWrap.appendChild(bar);
    header.appendChild(progressWrap);

    var headerActions = el('div', { class: 'wizard-header-actions' });
    var singleShot = el('button', {
      type: 'button',
      class: 'btn',
      text: 'Copy single-shot prompt'
    });
    App.clipboard.attach(singleShot, buildSingleShotPrompt);
    headerActions.appendChild(singleShot);

    var retemplate = el('button', {
      type: 'button',
      class: 'btn',
      text: 'Re-template prompts'
    });
    retemplate.addEventListener('click', function () { render(); });
    headerActions.appendChild(retemplate);
    header.appendChild(headerActions);

    sectionEl.appendChild(header);

    sectionEl.appendChild(el('p', {
      class: 'panel-lead',
      text: 'Work top-to-bottom. Copy each prompt into your AI agent session, run the verify check, then tick the checkbox to unlock the next step.'
    }));

    // Stepper.
    var stepper = el('div', { class: 'wizard-stepper' });
    var prevDone = true; // Step 0 has no predecessor; treat as "previous done".
    for (var i = 0; i < steps.length; i++) {
      var step = steps[i];
      stepper.appendChild(renderStep(step, setup, doneSet, prevDone));
      prevDone = doneSet[step.id] === true;
    }
    sectionEl.appendChild(stepper);

    // Closing card.
    if (App.helpers.wizardComplete(state)) {
      var card = el('div', { class: 'card wizard-complete-card' });
      card.appendChild(el('strong', { text: 'Wizard complete' }));
      card.appendChild(el('p', {
        text: 'Open the Task Form to write your first product task.',
        attrs: { style: 'margin: 0;' }
      }));
      card.appendChild(el('div', {}, [
        el('a', { class: 'btn btn--primary', href: '#tasks', text: 'Open Task Form' })
      ]));
      sectionEl.appendChild(card);
    }
  }

  App.panels.wizard = {
    mount: function (rootEl) {
      sectionEl = rootEl;
      render();
    },
    refresh: function () {
      render();
    }
  };
})();
