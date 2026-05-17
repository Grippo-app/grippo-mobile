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
  // Presets — pick one at the top of the form to seed Goal / Inputs /
  // Acceptance / Out of scope / builder kinds. <X>, <Y>, <Feature A> etc.
  // are USER placeholders kept literal; <iosFrameworkName> /
  // <IosFrameworkName> are SETUP tokens substituted via App.templates.render
  // at apply-time so the build-gate line uses the real framework name.
  // ----------------------------------------------------------------------
  var PRESETS = [
    {
      key: 'screen',
      label: 'New screen (sub-screen inside existing feature)',
      friendlyTitle: 'Add the <X> screen',
      goal: 'User can open the <X> screen from <existing feature> and see <what content>. Replace <X> with your screen name and fill in the parent-feature + content.',
      inputs: '- existing :ui-screen-features:<feature> module\n' +
        '- existing <ParentRouter> in :screen-api\n' +
        '- existing <SomeFeature> from :data-features:feature-api (or note none)',
      acceptance: '- screen renders the <content> via existing design-system components\n' +
        '- navigation from <ParentScreen> opens it; back/close return to the parent\n' +
        '- ./gradlew :<iosFrameworkName>:assemble<IosFrameworkName>DebugXCFramework + ./gradlew :androidApp:assembleDebug',
      outOfScope: '- new endpoints\n' +
        '- new entities\n' +
        '- new design-system components',
      selectedKinds: ['New sub-screen inside an existing feature']
    },
    {
      key: 'dialog',
      label: 'New dialog (bottom sheet)',
      friendlyTitle: 'Add the <X> picker dialog',
      goal: 'User can open the <X> bottom-sheet from <caller screen> and pick a value; the dialog returns the choice via callback. Replace <X> with the dialog name and fill in the caller + value type.',
      inputs: '- existing :ui-dialog-features:dialog-api/DialogConfig.kt\n' +
        '- existing <CallerComponent> that will call DialogController.show(...)\n' +
        '- existing <ValueType> the dialog returns',
      acceptance: '- new :ui-dialog-features:<x> module with the seven MVI files\n' +
        '- DialogConfig.<X>Picker case added with onResult callback (@Transient)\n' +
        '- :shared/dialog routing dispatches to the new module\n' +
        '- ./gradlew :<iosFrameworkName>:assemble<IosFrameworkName>DebugXCFramework + ./gradlew :androidApp:assembleDebug',
      outOfScope: '- new data feature\n' +
        '- new endpoints',
      selectedKinds: ['New dialog (bottom sheet)']
    },
    {
      key: 'endpoint',
      label: 'New API endpoint + DTO',
      friendlyTitle: 'Add <Verb> <Noun> endpoint',
      goal: 'Backend route <METHOD> /<path> is callable from the mobile client as a typed method on the api class returning Result<T>. Replace <Verb>, <Noun>, <METHOD>, <path>.',
      inputs: '- existing :data-services:backend api class\n' +
        '- existing repository that will consume the new method (or note that one will be added separately)',
      acceptance: '- new <X>Response/<X>Body DTO under dto/<area>/, all fields nullable with @SerialName + default = null\n' +
        '- new method on the api class in the matching section comment\n' +
        '- ./gradlew :data-services:backend:assemble + ./gradlew :androidApp:assembleDebug',
      outOfScope: '- repository wiring (separate task if needed)\n' +
        '- UI changes',
      selectedKinds: ['New API endpoint + DTO']
    },
    {
      key: 'feature',
      label: 'New domain feature + repository',
      friendlyTitle: 'Add <X>Feature data feature',
      goal: 'Add :data-features:<x> module so the UI can read/write <X> via the typed <X>Feature interface. Replace <X> with the domain noun.',
      inputs: '- existing :data-features:feature-api module\n' +
        '- existing :data-services:backend endpoints (or note which still need adding)\n' +
        '- existing :data-services:database (if caching planned)',
      acceptance: '- :data-features:<x> module added to settings.gradle.kts\n' +
        '- <X>Feature / <X>UseCase interfaces in :feature-api\n' +
        '- <X>RepositoryImpl + <X>FeatureImpl in :data-features:<x> with @Single bindings\n' +
        '- <X>FeatureModule added to :shared/Koin.kt\n' +
        '- ./gradlew :data-features:<x>:assemble + ./gradlew :androidApp:assembleDebug',
      outOfScope: '- UI screens\n' +
        '- new endpoints (write separate task if missing)',
      selectedKinds: ['New domain capability + data feature']
    },
    {
      key: 'nav',
      label: 'Cross-feature navigation',
      friendlyTitle: 'Open <X screen in Feature B> from <Y screen in Feature A>',
      goal: 'User on <Y screen> can tap <CTA> and land on <X screen> in a different feature module. Back returns to <Y screen>. Replace <X>, <Y>, <Feature A/B>, <CTA>.',
      inputs: '- existing <Feature A>Router with <Y> entry\n' +
        '- existing <Feature B>Router with <X> entry\n' +
        '- existing RootRouter / RootDirection / RootComponent',
      acceptance: '- new RootDirection case + RootContract method\n' +
        '- RootComponent.eventListener wires the new direction to navigation.push\n' +
        '- <Feature A>RootComponent threads a new (() -> Unit) constructor param down to <Y component>\n' +
        '- ./gradlew :<iosFrameworkName>:assemble<IosFrameworkName>DebugXCFramework + ./gradlew :androidApp:assembleDebug',
      outOfScope: '- changes inside <Feature B>\n' +
        '- new screens',
      selectedKinds: ['Cross-feature navigation']
    }
  ];

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
  // Preset selector — fills the form with a realistic skeleton the user
  // then customizes. Confirms before clobbering non-default content.
  // ----------------------------------------------------------------------

  function presetByKey(key) {
    for (var i = 0; i < PRESETS.length; i++) {
      if (PRESETS[i].key === key) return PRESETS[i];
    }
    return null;
  }

  // The shared App.dom.el helper currently emits data-attribute names
  // verbatim via setAttribute, which the HTML parser lowercases. So
  // `data: { taskField: 'goal' }` lands as `data-taskfield="goal"` not
  // `data-task-field="goal"`. Probe both spellings so this code is robust
  // to either behaviour (and works once that helper is fixed).
  function findField(root, name) {
    if (!root) return null;
    return root.querySelector('[data-task-field="' + name + '"]') ||
      root.querySelector('[data-taskfield="' + name + '"]');
  }
  function findKindCheckboxes(root) {
    if (!root) return [];
    var nodes = root.querySelectorAll('[data-builder-kind]');
    if (nodes.length === 0) nodes = root.querySelectorAll('[data-builderkind]');
    return nodes;
  }
  function kindOfCheckbox(cb) {
    return cb.getAttribute('data-builder-kind') || cb.getAttribute('data-builderkind');
  }

  function formHasUserContent(form) {
    // True if any text field is non-empty (besides defaults), or any
    // builder-kind checkbox is checked. outOfScope defaults to a literal,
    // so it's only "user content" when it differs from that literal.
    var named = ['friendlyTitle', 'goal', 'inputs', 'acceptance', 'dependsOn'];
    for (var i = 0; i < named.length; i++) {
      var inp = findField(form, named[i]);
      if (inp && String(inp.value || '').length > 0) return true;
    }
    var oos = findField(form, 'outOfScope');
    if (oos && String(oos.value || '') !== '- nothing else') return true;
    var cbs = findKindCheckboxes(sectionEl || form);
    for (var k = 0; k < cbs.length; k++) {
      if (cbs[k].checked) return true;
    }
    return false;
  }

  function applyPreset(form, preset) {
    var setup = (App.store.get() || {}).setup || {};
    var fields = {
      friendlyTitle: preset.friendlyTitle,
      goal: preset.goal,
      inputs: preset.inputs,
      acceptance: preset.acceptance,
      outOfScope: preset.outOfScope
    };
    var keys = Object.keys(fields);
    for (var i = 0; i < keys.length; i++) {
      var name = keys[i];
      var inp = findField(form, name);
      if (inp) inp.value = App.templates.render(fields[name], setup);
    }
    // Re-derive short title from friendly title (since presets seed
    // friendlyTitle but no shortTitle).
    shortTitleTouched = false;
    var stInput = findField(form, 'shortTitle');
    var ftInput = findField(form, 'friendlyTitle');
    if (stInput && ftInput) stInput.value = deriveShortTitle(ftInput.value);

    // Clear all builder-kind checkboxes, then check those listed by the
    // preset. Builder fieldset lives outside `form` — scope to sectionEl.
    var cbs = findKindCheckboxes(sectionEl || form);
    for (var c = 0; c < cbs.length; c++) {
      var k = kindOfCheckbox(cbs[c]);
      cbs[c].checked = preset.selectedKinds.indexOf(k) >= 0;
    }

    // Fire one synthetic event so handleFieldEvent recomputes preview,
    // validation, action state, and the selected-builder cards.
    if (ftInput) {
      var ev = (typeof Event === 'function')
        ? new Event('input', { bubbles: true })
        : (function () {
            var e = document.createEvent('Event');
            e.initEvent('input', true, true);
            return e;
          })();
      ftInput.dispatchEvent(ev);
    }
  }

  function buildPresetSelector(form) {
    var fid = 'task-preset';
    var field = el('div', { class: 'form-field' });
    field.appendChild(el('label', {
      attrs: { 'for': fid },
      text: 'Start from template'
    }));
    var select = el('select', {
      id: fid,
      attrs: { autocomplete: 'off' }
    });
    var defaultOpt = el('option', {
      value: '',
      text: '(no preset — empty form)'
    });
    defaultOpt.selected = true;
    select.appendChild(defaultOpt);
    for (var i = 0; i < PRESETS.length; i++) {
      select.appendChild(el('option', {
        value: PRESETS[i].key,
        text: PRESETS[i].label
      }));
    }
    select.addEventListener('change', function () {
      var key = select.value;
      if (!key) return;
      var preset = presetByKey(key);
      if (!preset) {
        select.value = '';
        return;
      }
      if (formHasUserContent(form)) {
        var ok = window.confirm("Replace current form contents with the '" + preset.label + "' template?");
        if (!ok) {
          select.value = '';
          return;
        }
      }
      applyPreset(form, preset);
      // Reset so the same preset can be picked again later.
      select.value = '';
    });
    field.appendChild(select);
    field.appendChild(el('small', {
      class: 'field-help',
      text: 'Fills Goal / Inputs / Acceptance / Out of scope and ticks the matching builder kind. You then customize the placeholders.'
    }));
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

    // Two-column layout wrapper: LEFT = title + form + builder picker,
    // RIGHT (sticky on ≥1200px) = preview heading + filename + code block
    // + action buttons. Below 1200px collapses to a single column.
    var layout = el('div', { class: 'task-layout' });
    var leftCol = el('div', { class: 'task-col task-col--form' });
    var rightCol = el('div', { class: 'task-col task-col--preview' });

    leftCol.appendChild(el('h2', { class: 'panel-title', text: 'Task form' }));
    leftCol.appendChild(el('p', {
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

    // Preset selector — placed FIRST so it visually sits above the form
    // and pre-fills the rest of the fields when picked.
    form.appendChild(buildPresetSelector(form));

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

    leftCol.appendChild(form);

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
    leftCol.appendChild(fs);

    // Preview pane — RIGHT column (sticky on ≥1200px).
    rightCol.appendChild(el('h3', { class: 'panel-section-title', text: 'Preview' }));
    rightCol.appendChild(el('p', {
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
    rightCol.appendChild(previewWrap);

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
      var hist = (App.store.get().progress || {}).taskHistory || [];
      hist = hist.concat([{
        n: parseInt(formValues.taskNumber, 10) || current,
        filename: fn,
        friendlyTitle: String(formValues.friendlyTitle || '').trim(),
        createdAt: new Date().toISOString(),
        status: 'active',
        doneAt: null
      }]);
      App.store.saveProgress({ taskCounter: next, taskHistory: hist });
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
      var next = current + 1;
      var hist = (App.store.get().progress || {}).taskHistory || [];
      hist = hist.concat([{
        n: parseInt(formValues.taskNumber, 10) || current,
        filename: fn,
        friendlyTitle: String(formValues.friendlyTitle || '').trim(),
        createdAt: new Date().toISOString(),
        status: 'active',
        doneAt: null
      }]);
      App.store.saveProgress({ taskCounter: next, taskHistory: hist });
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

    rightCol.appendChild(actions);

    layout.appendChild(leftCol);
    layout.appendChild(rightCol);
    sectionEl.appendChild(layout);

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
