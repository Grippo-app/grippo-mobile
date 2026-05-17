(function () {
  window.App = window.App || {};
  App.panels = App.panels || {};

  var sectionEl = null;
  // Tracks whether the user has manually edited the Short title input.
  // While false, the short title auto-derives from the friendly title.
  var shortTitleTouched = false;
  // Cached current values so the live preview can debounce without losing
  // intermediate edits.
  var formValues = null;

  // ----------------------------------------------------------------------
  // DOM helpers — el() lives in scripts/dom.js (App.dom.el).
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
    return { wrap: wrap, code: code };
  }

  function debounce(fn, ms) {
    var t = null;
    return function () {
      var args = arguments;
      var self = this;
      if (t) clearTimeout(t);
      t = setTimeout(function () {
        t = null;
        fn.apply(self, args);
      }, ms);
    };
  }

  // ----------------------------------------------------------------------
  // Pure data helpers.
  // ----------------------------------------------------------------------

  function deriveShortTitle(friendly) {
    var s = String(friendly || '').toLowerCase();
    // Replace spaces, hyphens, dots with underscores; strip everything else.
    s = s.replace(/[\s\-.]+/g, '_').replace(/[^a-z0-9_]+/g, '');
    // Collapse runs of underscores, trim leading underscores/digits.
    s = s.replace(/_+/g, '_').replace(/^_+/, '');
    if (/^[0-9]/.test(s)) s = 't_' + s;
    return s;
  }

  function bulletize(text) {
    var lines = String(text || '').split(/\r?\n/);
    var out = [];
    for (var i = 0; i < lines.length; i++) {
      var raw = lines[i];
      if (raw.trim().length === 0) continue;
      var trimmed = raw.replace(/^\s+/, '');
      if (/^[-*]\s+/.test(trimmed)) out.push('- ' + trimmed.replace(/^[-*]\s+/, ''));
      else out.push('- ' + trimmed);
    }
    return out;
  }

  function defaults(state) {
    var prog = (state && state.progress) || {};
    return {
      taskNumber: prog.taskCounter || 1,
      friendlyTitle: '',
      shortTitle: '',
      goal: '',
      inputs: '',
      acceptance: '',
      outOfScope: '- nothing else',
      dependsOn: '',
      selectedKinds: []
    };
  }

  function validate(values, state) {
    var errors = {};
    var minCounter = (state.progress && state.progress.taskCounter) || 1;

    var n = parseInt(values.taskNumber, 10);
    if (!isFinite(n) || n <= 0) {
      errors.taskNumber = 'Positive integer required.';
    } else if (n < minCounter) {
      errors.taskNumber = 'The site cannot verify requirements/tasks/done/ — reusing a number may collide.';
    }

    if (String(values.friendlyTitle || '').trim().length === 0) {
      errors.friendlyTitle = 'Required.';
    }

    var st = String(values.shortTitle || '').trim();
    if (st.length === 0) errors.shortTitle = 'Required.';
    else if (!/^[a-z][a-z0-9_]*$/.test(st)) {
      errors.shortTitle = 'Must match ^[a-z][a-z0-9_]*$ (lowercase, snake_case).';
    }

    if (String(values.goal || '').trim().length < 20) {
      errors.goal = 'Write at least 20 characters describing the user-visible capability.';
    }

    if (bulletize(values.inputs).length === 0) {
      errors.inputs = 'List at least one input (existing Feature, route, widget, deeplink).';
    }

    if (bulletize(values.acceptance).length === 0) {
      errors.acceptance = 'List at least one observable acceptance bullet (include the build gate).';
    }

    if (bulletize(values.outOfScope).length === 0) {
      errors.outOfScope = 'Required — write "nothing else" if the boundary is trivial.';
    }

    return errors;
  }

  function isWarningOnly(errors) {
    // Lowering the task counter is a warning, not a blocker.
    var keys = Object.keys(errors);
    return keys.length === 1 &&
      keys[0] === 'taskNumber' &&
      /reusing a number/.test(errors.taskNumber);
  }

  function buildMarkdown(values) {
    var n = parseInt(values.taskNumber, 10);
    if (!isFinite(n)) n = 1;

    var lines = [];
    lines.push('# TASK ' + n + ' — ' + String(values.friendlyTitle || '').trim());
    lines.push('');
    lines.push('## Goal');
    lines.push(String(values.goal || '').trim());
    lines.push('');
    lines.push('## Inputs');
    var inputs = bulletize(values.inputs);
    for (var i = 0; i < inputs.length; i++) lines.push(inputs[i]);
    lines.push('');
    lines.push('## Acceptance');
    var acc = bulletize(values.acceptance);
    for (var a = 0; a < acc.length; a++) lines.push(acc[a]);
    lines.push('');
    lines.push('## Out of scope');
    var oos = bulletize(values.outOfScope);
    for (var o = 0; o < oos.length; o++) lines.push(oos[o]);

    var deps = bulletize(values.dependsOn);
    if (deps.length > 0) {
      lines.push('');
      lines.push('## Depends on');
      for (var d = 0; d < deps.length; d++) lines.push(deps[d]);
    }

    lines.push('');
    return lines.join('\n');
  }

  function filename(values) {
    var n = parseInt(values.taskNumber, 10);
    if (!isFinite(n) || n < 1) n = 1;
    var st = String(values.shortTitle || '').trim();
    if (!/^[a-z][a-z0-9_]*$/.test(st)) st = 'task';
    return 'TASK_' + n + '_' + st + '.md';
  }

  // ----------------------------------------------------------------------
  // Field builders.
  // ----------------------------------------------------------------------

  function buildTextField(opts) {
    var fid = 'task-' + opts.name;
    var field = el('div', { class: 'form-field' });
    field.appendChild(el('label', {
      attrs: { 'for': fid },
      text: opts.label
    }));
    var attrs = { spellcheck: 'false', autocomplete: 'off' };
    if (opts.placeholder) attrs.placeholder = opts.placeholder;
    if (opts.inputmode) attrs.inputmode = opts.inputmode;
    if (opts.min != null) attrs.min = String(opts.min);
    var input = el('input', {
      type: opts.type || 'text',
      id: fid,
      value: opts.value == null ? '' : String(opts.value),
      data: { taskField: opts.name },
      attrs: attrs
    });
    field.appendChild(input);
    if (opts.help) field.appendChild(el('small', { class: 'field-help', text: opts.help }));
    var err = el('small', { class: 'field-error', data: { errorFor: opts.name } });
    err.hidden = true;
    field.appendChild(err);
    return field;
  }

  function buildTextarea(opts) {
    var fid = 'task-' + opts.name;
    var field = el('div', { class: 'form-field' });
    field.appendChild(el('label', {
      attrs: { 'for': fid },
      text: opts.label
    }));
    var ta = el('textarea', {
      id: fid,
      data: { taskField: opts.name },
      attrs: {
        rows: String(opts.rows || 4),
        spellcheck: 'false',
        autocomplete: 'off'
      }
    });
    ta.value = opts.value == null ? '' : String(opts.value);
    field.appendChild(ta);
    if (opts.help) field.appendChild(el('small', { class: 'field-help', text: opts.help }));
    var err = el('small', { class: 'field-error', data: { errorFor: opts.name } });
    err.hidden = true;
    field.appendChild(err);
    return field;
  }

  // ----------------------------------------------------------------------
  // Reading and writing form state.
  // ----------------------------------------------------------------------

  function readFormValues(form) {
    var out = {};
    var fields = form.querySelectorAll('[data-task-field]');
    for (var i = 0; i < fields.length; i++) {
      var f = fields[i];
      out[f.getAttribute('data-task-field')] = f.value;
    }
    var kinds = [];
    // The builder-picker fieldset is a sibling of `form` (appended to
    // sectionEl). Scope the query to sectionEl so the checkboxes are found.
    var kindRoot = sectionEl || form;
    var cbs = kindRoot.querySelectorAll('[data-builder-kind]');
    for (var k = 0; k < cbs.length; k++) {
      if (cbs[k].checked) kinds.push(cbs[k].getAttribute('data-builder-kind'));
    }
    out.selectedKinds = kinds;
    return out;
  }

  function refreshErrors(form, errors) {
    var errEls = form.querySelectorAll('[data-error-for]');
    for (var i = 0; i < errEls.length; i++) {
      var name = errEls[i].getAttribute('data-error-for');
      if (errors[name]) {
        errEls[i].textContent = errors[name];
        errEls[i].hidden = false;
      } else {
        errEls[i].textContent = '';
        errEls[i].hidden = true;
      }
    }
  }

  function updateActionState(values, errors) {
    if (!sectionEl) return;
    var blocking = {};
    var keys = Object.keys(errors);
    for (var i = 0; i < keys.length; i++) blocking[keys[i]] = errors[keys[i]];
    if (isWarningOnly(errors)) blocking = {};
    var disabled = Object.keys(blocking).length > 0;

    var btns = sectionEl.querySelectorAll('[data-action]');
    var problems = Object.keys(blocking).map(function (k) { return blocking[k]; });
    for (var b = 0; b < btns.length; b++) {
      btns[b].disabled = disabled;
      if (disabled) btns[b].setAttribute('title', 'Resolve before continuing: ' + problems.join(' '));
      else btns[b].removeAttribute('title');
    }
  }

  function updatePreview(values) {
    var pre = sectionEl && sectionEl.querySelector('#task-preview');
    if (pre) pre.textContent = buildMarkdown(values);
    var fnEls = sectionEl && sectionEl.querySelectorAll('[data-filename]');
    if (fnEls) {
      var fn = filename(values);
      for (var i = 0; i < fnEls.length; i++) fnEls[i].textContent = fn;
    }
  }

  function updateSelectedKindCards(values) {
    var listRoot = sectionEl && sectionEl.querySelector('[data-selected-builders]');
    if (!listRoot) return;
    while (listRoot.firstChild) listRoot.removeChild(listRoot.firstChild);
    if (values.selectedKinds.length === 0) {
      listRoot.appendChild(el('p', {
        class: 'panel-lead',
        text: 'Tick a change kind above to preview the builder that would run.'
      }));
      return;
    }
    for (var i = 0; i < App.data.builders.length; i++) {
      var b = App.data.builders[i];
      if (values.selectedKinds.indexOf(b.kind) < 0) continue;
      var card = el('div', { class: 'card task-builder-card' });
      var head = el('p', { class: 'task-builder-line' });
      head.appendChild(document.createTextNode(b.kind + ' → '));
      head.appendChild(el('code', { text: b.builder }));
      card.appendChild(head);
      card.appendChild(el('p', { class: 'task-builder-line' }, [
        el('a', {
          href: b.cookbook,
          text: 'Cookbook recipe: ' + b.cookbook,
          attrs: { target: '_blank', rel: 'noopener' }
        })
      ]));
      listRoot.appendChild(card);
    }
  }

  // ----------------------------------------------------------------------
  // Action handlers.
  // ----------------------------------------------------------------------

  function downloadMarkdown(values) {
    var md = buildMarkdown(values);
    var fn = filename(values);
    try {
      var blob = new Blob([md], { type: 'text/markdown' });
      var url = URL.createObjectURL(blob);
      var a = document.createElement('a');
      a.href = url;
      a.download = fn;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(function () { URL.revokeObjectURL(url); }, 0);
    } catch (e) {
      // Fall through silently — browsers without Blob/URL still get the
      // "Copy markdown" path.
    }
  }

  function showInstructionToast(fn, message) {
    var region = document.getElementById('toast-region');
    if (!region) return;
    region.innerHTML = '';
    var text = message != null
      ? message
      : "Drop the file at requirements/tasks/" + fn + " and tell Claude: 'run task " + fn + "'.";
    var toast = el('div', {
      class: 'toast toast--instruction',
      text: text
    });
    region.appendChild(toast);
    void toast.offsetWidth;
    toast.classList.add('toast--visible');
    setTimeout(function () {
      toast.classList.remove('toast--visible');
      setTimeout(function () {
        if (toast.parentNode === region) region.removeChild(toast);
      }, 250);
    }, 4500);
  }

  // ----------------------------------------------------------------------
  // Wiring.
  // ----------------------------------------------------------------------

  var debouncedPreview = debounce(function (values) {
    updatePreview(values);
  }, 100);

  function handleFieldEvent(form, ev) {
    var target = ev && ev.target;
    if (target && target.getAttribute && target.getAttribute('data-task-field') === 'shortTitle') {
      // Once the user types into Short title, lock it.
      shortTitleTouched = true;
    }
    if (target && target.getAttribute &&
        target.getAttribute('data-task-field') === 'friendlyTitle' &&
        !shortTitleTouched) {
      // Auto-derive the short title until the user takes over.
      var stInput = form.querySelector('[data-task-field="shortTitle"]');
      if (stInput) stInput.value = deriveShortTitle(target.value);
    }
    formValues = readFormValues(form);
    var state = App.store.get();
    var errs = validate(formValues, state);
    refreshErrors(form, errs);
    updateActionState(formValues, errs);
    updateSelectedKindCards(formValues);
    debouncedPreview(formValues);
  }

  function wireForm(form) {
    var inputs = form.querySelectorAll('input, textarea');
    var onAny = function (ev) { handleFieldEvent(form, ev); };
    for (var i = 0; i < inputs.length; i++) {
      var ip = inputs[i];
      var t = ip.type;
      if (t === 'checkbox' || t === 'radio') ip.addEventListener('change', onAny);
      else ip.addEventListener('input', onAny);
    }
  }

  // ----------------------------------------------------------------------
  // Renderers.
  // ----------------------------------------------------------------------

  function renderGate() {
    var banner = el('div', { class: 'banner banner--warn' });
    banner.appendChild(document.createTextNode('Complete the Launch Wizard first. '));
    banner.appendChild(el('a', { href: '#wizard', text: 'Open Wizard' }));
    sectionEl.appendChild(banner);
  }

  function renderForm(state) {
    var vals = defaults(state);
    formValues = vals;
    shortTitleTouched = false;

    sectionEl.appendChild(el('h2', { class: 'panel-title', text: 'Task form' }));
    sectionEl.appendChild(el('p', {
      class: 'panel-lead'
    }, [
      document.createTextNode('Produces a valid '),
      el('code', { text: 'TASK_<N>_<title>.md' }),
      document.createTextNode(' for the orchestrator. The four required sections are enforced; '),
      el('code', { text: '## Depends on' }),
      document.createTextNode(' is optional. Reference example: '),
      el('a', {
        href: '../tasks/TASK_0_example_note_archive.md.example',
        text: 'TASK_0_example_note_archive.md.example',
        attrs: { target: '_blank', rel: 'noopener' }
      }),
      document.createTextNode('.')
    ]));

    var form = el('form', {
      class: 'task-form',
      attrs: { novalidate: '', autocomplete: 'off' }
    });
    form.addEventListener('submit', function (e) { e.preventDefault(); });

    form.appendChild(buildTextField({
      name: 'taskNumber',
      label: 'Task number',
      type: 'number',
      inputmode: 'numeric',
      min: 1,
      value: vals.taskNumber,
      help: 'Defaults to the next free counter. Lowering it shows a warning — the site cannot verify requirements/tasks/done/.'
    }));

    form.appendChild(buildTextField({
      name: 'friendlyTitle',
      label: 'Friendly title',
      placeholder: 'Add the note archive screen',
      value: vals.friendlyTitle,
      help: 'Rendered as "# TASK <N> — <Friendly title>" at the top of the file.'
    }));

    form.appendChild(buildTextField({
      name: 'shortTitle',
      label: 'Short title',
      placeholder: 'note_archive_screen',
      value: vals.shortTitle,
      help: 'snake_case. Auto-derived from Friendly title until you edit it.'
    }));

    form.appendChild(buildTextarea({
      name: 'goal',
      label: 'Goal',
      rows: 4,
      value: vals.goal,
      help: 'What capability does the user gain when this lands? At least 20 characters.'
    }));

    form.appendChild(buildTextarea({
      name: 'inputs',
      label: 'Inputs',
      rows: 4,
      value: vals.inputs,
      help: 'Existing artifacts the task depends on: existing *Feature, existing route, existing widget, deeplink. One per line — "- " is auto-prefixed.'
    }));

    form.appendChild(buildTextarea({
      name: 'acceptance',
      label: 'Acceptance',
      rows: 4,
      value: vals.acceptance,
      help: 'Observable success criteria. Always include the build gate (substitute <iosFrameworkName> from setup; omit the iOS line entirely when iosEnabled: false): ./gradlew :<iosFrameworkName>:assemble<IosFrameworkName>DebugXCFramework + ./gradlew :androidApp:assembleDebug.'
    }));

    form.appendChild(buildTextarea({
      name: 'outOfScope',
      label: 'Out of scope',
      rows: 3,
      value: vals.outOfScope,
      help: 'Explicit non-goals. Required even if trivial. Prevents builders from drifting.'
    }));

    form.appendChild(buildTextarea({
      name: 'dependsOn',
      label: 'Depends on (optional)',
      rows: 2,
      value: vals.dependsOn,
      help: 'Task stems (no .md) that must already live in requirements/tasks/done/.'
    }));

    sectionEl.appendChild(form);

    // Builder picker fieldset.
    var fs = el('fieldset', { class: 'form-field form-field--group task-builder-picker' });
    fs.appendChild(el('legend', { text: 'Builder picker (informational)' }));
    fs.appendChild(el('small', {
      class: 'field-help',
      text: 'Builder selection is informational — task-intake reclassifies from your task text when the orchestrator runs.'
    }));
    var grid = el('div', { class: 'task-builder-grid' });
    for (var i = 0; i < App.data.builders.length; i++) {
      var b = App.data.builders[i];
      var lbl = el('label', { class: 'checkbox-label task-builder-option' });
      var cb = el('input', {
        type: 'checkbox',
        data: { builderKind: b.kind }
      });
      lbl.appendChild(cb);
      lbl.appendChild(document.createTextNode(' ' + b.kind));
      grid.appendChild(lbl);
    }
    fs.appendChild(grid);
    fs.appendChild(el('div', { class: 'task-selected-builders', data: { selectedBuilders: '' } }));
    fs.appendChild(el('p', {
      class: 'panel-lead task-validators-line',
      text: 'Validators that run on every task: architecture-validator, mvi-contract-validator, anti-pattern-scanner, naming-convention-validator, di-validator, compose-stability-validator, data-layer-validator, build-validator.'
    }));
    // Delegate change events on the builder-kind checkboxes — they live
    // outside `form`, so wireForm(form) does not catch them.
    fs.addEventListener('change', function (ev) {
      var t = ev && ev.target;
      if (t && t.getAttribute && t.getAttribute('data-builder-kind') != null) {
        handleFieldEvent(form, ev);
      }
    });
    sectionEl.appendChild(fs);

    // Preview pane.
    sectionEl.appendChild(el('h3', { class: 'panel-section-title', text: 'Preview' }));
    sectionEl.appendChild(el('p', {
      class: 'panel-lead'
    }, [
      document.createTextNode('Filename: '),
      el('code', { text: filename(vals), data: { filename: '' } })
    ]));
    var previewWrap = el('div', { class: 'code-block-wrapper' });
    var pre = el('pre', { class: 'code-block' });
    var code = el('code', { id: 'task-preview', text: buildMarkdown(vals) });
    pre.appendChild(code);
    previewWrap.appendChild(pre);
    var copyBtn = el('button', {
      type: 'button',
      class: 'copy-btn',
      text: 'Copy',
      attrs: { 'aria-label': 'Copy markdown to clipboard' }
    });
    App.clipboard.attach(copyBtn, function () { return code.textContent; });
    previewWrap.appendChild(copyBtn);
    sectionEl.appendChild(previewWrap);

    // Action buttons.
    var actions = el('div', { class: 'task-actions' });

    var promptAction = el('button', {
      type: 'button',
      class: 'btn btn--primary',
      text: 'Copy as Claude prompt',
      data: { action: 'prompt' }
    });
    promptAction.addEventListener('click', function () {
      if (promptAction.disabled) return;
      var md = buildMarkdown(formValues);
      var fn = filename(formValues);
      var prompt = 'Create the file requirements/tasks/' + fn + ' with the following content:\n\n' +
        md + '\n' +
        'Then run task ' + fn + '.';
      App.clipboard.copy(prompt);
      var prog = App.store.get().progress || {};
      var current = prog.taskCounter || 1;
      var next = current + 1;
      App.store.saveProgress({ taskCounter: next });
      showInstructionToast(
        fn,
        'Pasted into Claude? Counter advanced to ' + next +
          '. The agent will create the file and run the task.'
      );
    });
    actions.appendChild(promptAction);

    var saveAction = el('button', {
      type: 'button',
      class: 'btn',
      text: 'Save & increment counter',
      data: { action: 'save' }
    });
    saveAction.addEventListener('click', function () {
      if (saveAction.disabled) return;
      var md = buildMarkdown(formValues);
      var fn = filename(formValues);
      App.clipboard.copy(md);
      var prog = App.store.get().progress || {};
      var current = prog.taskCounter || 1;
      App.store.saveProgress({ taskCounter: current + 1 });
      showInstructionToast(fn);
    });
    actions.appendChild(saveAction);

    var copyAction = el('button', {
      type: 'button',
      class: 'btn',
      text: 'Copy markdown',
      data: { action: 'copy' }
    });
    copyAction.addEventListener('click', function () {
      if (copyAction.disabled) return;
      App.clipboard.copy(buildMarkdown(formValues));
    });
    actions.appendChild(copyAction);

    var dlAction = el('button', {
      type: 'button',
      class: 'btn',
      text: 'Download .md',
      data: { action: 'download' }
    });
    dlAction.addEventListener('click', function () {
      if (dlAction.disabled) return;
      downloadMarkdown(formValues);
    });
    actions.appendChild(dlAction);

    sectionEl.appendChild(actions);

    wireForm(form);
    var errs = validate(vals, state);
    refreshErrors(form, errs);
    updateActionState(vals, errs);
    updateSelectedKindCards(vals);
  }

  function render() {
    if (!sectionEl) return;
    while (sectionEl.firstChild) sectionEl.removeChild(sectionEl.firstChild);

    var state = App.store.get();
    if (!App.helpers || typeof App.helpers.wizardComplete !== 'function' ||
        !App.helpers.wizardComplete(state)) {
      renderGate();
      return;
    }
    renderForm(state);
  }

  App.panels.tasks = {
    mount: function (rootEl) {
      sectionEl = rootEl;
      render();
    },
    refresh: function () {
      render();
    }
  };
})();
