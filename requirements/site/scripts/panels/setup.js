(function () {
  window.App = window.App || {};
  App.panels = App.panels || {};

  var sectionEl = null;
  var lastAppIdSuggestion = '';

  var FIELDS = [
    {
      name: 'productName',
      label: 'Product name',
      placeholder: 'Pulse',
      required: true,
      pattern: /^[A-Z][A-Za-z0-9]*$/,
      patternHint: 'PascalCase, letters and digits only.',
      tooltip: 'Fills the <Product> / <product> placeholders in every templated prompt.'
    },
    {
      name: 'orgName',
      label: 'Org name',
      placeholder: 'acme',
      required: false,
      pattern: /^[a-z][a-z0-9]*$/,
      patternHint: 'lowercase letters/digits; leave empty for a single-org package.',
      tooltip: 'Empty value yields the single-org package com.<product>.* .'
    },
    {
      name: 'backendHost',
      label: 'Backend host',
      placeholder: 'pulse-app.com',
      required: true,
      pattern: /^[a-z0-9.-]+\.[a-z]{2,}$/,
      patternHint: 'A hostname like pulse-app.com.',
      tooltip: 'Used in BackendClient.defaultRequest.host.'
    },
    {
      name: 'applicationId',
      label: 'Application ID',
      placeholder: 'com.acme.pulse.android',
      required: true,
      pattern: /^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/,
      patternHint: 'Dot-separated lowercase package, e.g. com.acme.pulse.android.',
      tooltip: 'Android Play Store id; defaults to <productPackage>.android.'
    },
    {
      name: 'iosFrameworkName',
      label: 'iOS framework name',
      placeholder: 'shared',
      required: true,
      pattern: /^[a-zA-Z][a-zA-Z0-9]*$/,
      patternHint: 'Identifier-style, letters and digits only.',
      tooltip: 'XCFramework target name. Keep "shared" unless renaming.'
    },
    {
      name: 'typefaceFactory',
      label: 'Typeface factory',
      placeholder: 'inter',
      required: true,
      pattern: /^[a-z][a-zA-Z0-9_]*$/,
      patternHint: 'Kotlin function name; must start lowercase.',
      tooltip: 'Function name resource-builder uses when registering fonts.'
    },
    {
      name: 'firstDomain',
      label: 'First product domain',
      placeholder: 'Note',
      required: true,
      pattern: /^[A-Z][A-Za-z0-9]*$/,
      patternHint: 'PascalCase noun (e.g. Note, Task).',
      tooltip: 'Used in Wizard Step 8 prompt as the first product domain. Not written into the YAML.'
    }
  ];

  var LOCALE_OPTIONS = ['en', 'uk', 'ru', 'de', 'fr', 'es'];

  var AUTH_OPTIONS = [
    { value: 'email-password', label: 'Email / password' },
    { value: 'google',         label: 'Google Sign-In' },
    { value: 'apple',          label: 'Apple Sign-In' }
  ];

  var TOGGLE_DEFS = [
    { key: 'iosEnabled',      label: 'iOS enabled',                          help: 'When off, build-validator skips iOS gates and Wizard Step 11 auto-skips.' },
    { key: 'firebaseEnabled', label: 'Firebase enabled',                     help: 'When off, agents skip Firebase wiring.' },
    { key: 'prelaunch',       label: 'Prelaunch (allow destructive Room fallback)', help: 'Default on for a fresh project; flip off after the app ships.' }
  ];

  var CODEX_VALUES = ['auto', 'true', 'false'];

  // SYNC: mirrors the body of requirements/00-overview/03-project-config.md
  // (everything after the YAML frontmatter). When you edit that file, update
  // this constant — the site renders the download from CONFIG_BODY, not from
  // disk (file:// has no fetch).
  var CONFIG_BODY = [
    '> **Fresh-project state.** Every value in the frontmatter above is a placeholder or a neutral default. Before invoking any sub-agent (`orchestrator`, builders, validators), replace every `<placeholder>` with project-specific values per `requirements/launch.md` Step 1.5. Empty arrays (`featuresWithRootComponentSuffix: []`, `diHandWrittenModules: []`) stay empty until the project actually needs them — sub-agents update them on demand.',
    '',
    '# Project config — single source of truth',
    '',
    'Replace every value in the frontmatter above before the first bootstrap. Placeholders use the same syntax as `00-overview/05-template-conventions.md` §1.',
    '',
    'Sub-agents under `requirements/sub-agents/` read this file before acting. When you bootstrap a new project from these requirements, copy this file and update every value.',
    '',
    '## Field meanings',
    '',
    '- `productName` — used in class prefixes (`<Product>Api`, `<Product>Component`).',
    '- `productPackage` — Kotlin package root.',
    '- `apiClassName` — name of the flat backend API class.',
    '- `backendHost` — used in `BackendClient.defaultRequest`.',
    '- `applicationId` — Android Play Store id.',
    '- `iosFrameworkName` — XCFramework target name.',
    '- `iosEnabled` — if false, build-validator skips iOS gates.',
    '- `firebaseEnabled` — if false, agents skip Firebase wiring.',
    '- `codexEnabled` — controls the external-review gate. One of:',
    '  - `auto` (default) — orchestrator detects the [openai/codex-plugin-cc](https://github.com/openai/codex-plugin-cc) plugin at runtime. If present, runs `codex-review-loop`; otherwise falls back to `internal-reviewer` (Claude-backed local review).',
    '  - `true` — force `codex-review-loop`; orchestrator hard-fails if the plugin is missing.',
    '  - `false` — force `internal-reviewer`; skip Codex detection entirely.',
    '- `prelaunch` — if true, room-migration-builder allows destructive fallback without a migration.',
    '- `supportedLocales` — resource-builder requires every locale to receive each new key.',
    '- `typefaceFactory` — resource-builder uses this factory function name when registering fonts.',
    '- `featuresWithRootComponentSuffix` — features that use `*RootComponent.kt` instead of bare `*Component.kt` (because they have a sub-screen with the same name as the feature). Start empty (`[]`) on a fresh project; the orchestrator appends a feature name only when its first sub-screen collides with the feature root.',
    '- `diHandWrittenModules` — Koin modules that legitimately use the hand-written `module { … }` DSL outside annotated `@Single` classes (platform-edge wrappers, etc.). `di-validator` reads this list before flagging hand-DSL hits. Start empty (`[]`) on a fresh project; append a module name only when a hand-written `module { … }` block is deliberately introduced (typically platform-edge wrappers like Google/Apple auth).',
    '',
    '## Updating',
    '',
    'When a value changes (new locale added, app ships and `prelaunch` flips, codex installed), update this file. Sub-agents read it lazily — no rebuild needed.'
  ].join('\n');

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

  function setupDefaults() {
    return {
      productName: '',
      orgName: '',
      backendHost: '',
      applicationId: '',
      iosFrameworkName: 'shared',
      typefaceFactory: 'inter',
      firstDomain: '',
      supportedLocales: ['en'],
      authMethods: [],
      iosEnabled: true,
      firebaseEnabled: true,
      codexEnabled: 'auto',
      prelaunch: true
    };
  }

  function currentSetup() {
    var stored = App.store.get().setup || {};
    var d = setupDefaults();
    var out = {};
    var keys = Object.keys(d);
    for (var i = 0; i < keys.length; i++) {
      var k = keys[i];
      out[k] = Object.prototype.hasOwnProperty.call(stored, k) ? stored[k] : d[k];
    }
    if (!Array.isArray(out.supportedLocales)) out.supportedLocales = ['en'];
    if (out.supportedLocales.indexOf('en') === -1) {
      out.supportedLocales = ['en'].concat(out.supportedLocales);
    }
    if (!Array.isArray(out.authMethods)) out.authMethods = [];
    return out;
  }

  function currentChecks() {
    var prog = App.store.get().progress || {};
    var c = prog.setupChecks || {};
    return {
      yamlPasted:            c.yamlPasted === true,
      agentsInstalled:       c.agentsInstalled === true,
      requirementsVerified:  c.requirementsVerified === true
    };
  }

  function derivedProductPackage(setup) {
    var product = (setup.productName || '').toLowerCase();
    if (!product) return '';
    if (setup.orgName) return 'com.' + setup.orgName + '.' + product;
    return 'com.' + product;
  }

  function suggestedApplicationId(setup) {
    if (!/^[A-Z][A-Za-z0-9]*$/.test(setup.productName || '')) return '';
    if (setup.orgName && !/^[a-z][a-z0-9]*$/.test(setup.orgName)) return '';
    var pkg = derivedProductPackage(setup);
    if (!pkg) return '';
    return pkg + '.android';
  }

  function validate(setup) {
    var errors = {};
    for (var i = 0; i < FIELDS.length; i++) {
      var f = FIELDS[i];
      var s = setup[f.name] == null ? '' : String(setup[f.name]);
      if (s.length === 0) {
        if (f.required) errors[f.name] = 'Required.';
        continue;
      }
      if (f.pattern && !f.pattern.test(s)) {
        errors[f.name] = f.patternHint || 'Invalid value.';
      }
    }
    return errors;
  }

  function installCommandsText() {
    return [
      'mkdir -p .claude/agents',
      'ln -sf "$(pwd)/requirements/sub-agents/builders/"*.md   .claude/agents/',
      'ln -sf "$(pwd)/requirements/sub-agents/validators/"*.md .claude/agents/',
      'ln -sf "$(pwd)/requirements/sub-agents/helpers/"*.md    .claude/agents/'
    ].join('\n');
  }

  function downloadConfigMd(setup) {
    var content = App.helpers.buildYaml(setup) + '\n\n' + CONFIG_BODY;
    try {
      var blob = new Blob([content], { type: 'text/markdown' });
      var url = URL.createObjectURL(blob);
      var a = document.createElement('a');
      a.href = url;
      a.download = '03-project-config.md';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(function () { URL.revokeObjectURL(url); }, 0);
    } catch (e) {
      // Fall through silently — browsers without Blob/URL still get the
      // YAML "Copy" path.
    }
  }

  // DOM helper — el() lives in scripts/dom.js (App.dom.el).
  var el = App.dom.el;

  function codeBlock(text) {
    var wrap = el('div', { class: 'code-block-wrapper' });
    var pre  = el('pre', { class: 'code-block' });
    var code = el('code', { text: text });
    pre.appendChild(code);
    wrap.appendChild(pre);
    var btn = el('button', { type: 'button', class: 'copy-btn', text: 'Copy', attrs: { 'aria-label': 'Copy to clipboard' } });
    App.clipboard.attach(btn, function () { return code.textContent; });
    wrap.appendChild(btn);
    return { wrap: wrap, code: code };
  }

  function buildTextField(f, value, errorMsg) {
    var fid = 'setup-' + f.name;
    var field = el('div', { class: 'form-field' });
    field.appendChild(el('label', {
      attrs: { 'for': fid },
      text:  f.label + (f.required ? '' : ' (optional)')
    }));
    var input = el('input', {
      type: 'text',
      id: fid,
      value: value,
      data: { field: f.name },
      attrs: { placeholder: f.placeholder, spellcheck: 'false', autocomplete: 'off' }
    });
    field.appendChild(input);
    if (f.tooltip) field.appendChild(el('small', { class: 'field-help', text: f.tooltip }));
    var err = el('small', { class: 'field-error', text: errorMsg || '' });
    if (!errorMsg) err.hidden = true;
    field.appendChild(err);
    return field;
  }

  function readFormSetup(form) {
    var out = {};
    for (var i = 0; i < FIELDS.length; i++) {
      var f = FIELDS[i];
      var inp = form.querySelector('[data-field="' + f.name + '"]');
      out[f.name] = inp ? inp.value.trim() : '';
    }
    var locales = ['en'];
    var lcbs = form.querySelectorAll('[data-locale]');
    for (var j = 0; j < lcbs.length; j++) {
      var lc = lcbs[j];
      var lv = lc.getAttribute('data-locale');
      if (lv === 'en') continue;
      if (lc.checked) locales.push(lv);
    }
    out.supportedLocales = locales;

    var auth = [];
    var acbs = form.querySelectorAll('[data-auth]');
    for (var k = 0; k < acbs.length; k++) {
      var ac = acbs[k];
      if (ac.checked) auth.push(ac.getAttribute('data-auth'));
    }
    out.authMethods = auth;

    var tcbs = form.querySelectorAll('[data-toggle]');
    for (var t = 0; t < tcbs.length; t++) {
      var tc = tcbs[t];
      out[tc.getAttribute('data-toggle')] = tc.checked;
    }

    var codex = form.querySelector('[data-codex]:checked');
    out.codexEnabled = codex ? codex.value : 'auto';

    return out;
  }

  function collectProblems(setup, errors, checks) {
    var probs = [];
    for (var i = 0; i < FIELDS.length; i++) {
      var f = FIELDS[i];
      if (errors[f.name]) probs.push(f.label + ' invalid');
    }
    if (!checks.yamlPasted)            probs.push('YAML-copied check unticked');
    if (!checks.agentsInstalled)       probs.push('Agents-installed check unticked');
    if (!checks.requirementsVerified)  probs.push('Requirements-verified check unticked');
    return probs;
  }

  function updatePrimaryState(setup, errors) {
    var primary = sectionEl && sectionEl.querySelector('[data-action="mark-complete"]');
    if (!primary) return;
    var probs = collectProblems(setup, errors, currentChecks());
    if (probs.length === 0) {
      primary.disabled = false;
      primary.removeAttribute('title');
    } else {
      primary.disabled = true;
      primary.setAttribute('title', 'Resolve before continuing: ' + probs.join('; '));
    }
  }

  var debouncedSaveSetup = debounce(function (snapshot) {
    App.store.saveSetup(snapshot);
  }, 200);

  function refreshErrors(form, errors) {
    for (var i = 0; i < FIELDS.length; i++) {
      var f = FIELDS[i];
      var inp = form.querySelector('[data-field="' + f.name + '"]');
      if (!inp) continue;
      var err = inp.parentNode.querySelector('.field-error');
      if (!err) continue;
      if (errors[f.name]) {
        err.textContent = errors[f.name];
        err.hidden = false;
      } else {
        err.textContent = '';
        err.hidden = true;
      }
    }
  }

  function wireForm(form) {
    var pnInput    = form.querySelector('[data-field="productName"]');
    var orgInput   = form.querySelector('[data-field="orgName"]');
    var appIdInput = form.querySelector('[data-field="applicationId"]');

    // Seed `lastAppIdSuggestion` so an unchanged-from-derivation applicationId
    // continues to track the derived value, while a user-edited value stays.
    var snap0 = readFormSetup(form);
    var sug0  = suggestedApplicationId(snap0);
    if (sug0 && appIdInput && appIdInput.value.trim() === sug0) {
      lastAppIdSuggestion = sug0;
    }

    function handle(ev) {
      if (ev && (ev.target === pnInput || ev.target === orgInput) && appIdInput) {
        var partial = readFormSetup(form);
        var sug = suggestedApplicationId(partial);
        var currentApp = appIdInput.value.trim();
        if (sug && (currentApp === '' || currentApp === lastAppIdSuggestion)) {
          appIdInput.value = sug;
        }
        lastAppIdSuggestion = sug;
      }
      var snap = readFormSetup(form);
      debouncedSaveSetup(snap);
      var errs = validate(snap);
      var yamlEl = sectionEl.querySelector('#setup-yaml');
      if (yamlEl) yamlEl.textContent = App.helpers.buildYaml(snap);
      refreshErrors(form, errs);
      updatePrimaryState(snap, errs);
    }

    var inputs = form.querySelectorAll('input');
    for (var i = 0; i < inputs.length; i++) {
      var ip = inputs[i];
      var t = ip.type;
      if (t === 'checkbox' || t === 'radio') ip.addEventListener('change', handle);
      else ip.addEventListener('input', handle);
    }
  }

  function wireChecks(checksRoot) {
    var cbs = checksRoot.querySelectorAll('[data-check]');
    for (var i = 0; i < cbs.length; i++) {
      (function (cb) {
        cb.addEventListener('change', function () {
          var existing = (App.store.get().progress || {}).setupChecks || {};
          existing[cb.getAttribute('data-check')] = cb.checked;
          App.store.saveProgress({ setupChecks: existing });
          var form = sectionEl.querySelector('.setup-form');
          var snap = form ? readFormSetup(form) : currentSetup();
          updatePrimaryState(snap, validate(snap));
        });
      })(cbs[i]);
    }
  }

  function render() {
    if (!sectionEl) return;
    var setup    = currentSetup();
    var checks   = currentChecks();
    var prog     = App.store.get().progress || {};
    var setupDone = prog.setupDone === true;
    var errors   = validate(setup);

    while (sectionEl.firstChild) sectionEl.removeChild(sectionEl.firstChild);

    if (setupDone) {
      sectionEl.appendChild(el('div', {
        class: 'banner banner--info',
        text: 'Setup complete. You can re-edit values and re-mark complete to update the YAML.'
      }));
    }

    sectionEl.appendChild(el('h2', { class: 'panel-title', text: 'Project configuration' }));
    sectionEl.appendChild(el('p', {
      class: 'panel-lead',
      text: 'These values populate the YAML frontmatter of requirements/00-overview/03-project-config.md and seed the templated prompts in the Launch Wizard.'
    }));

    var form = el('form', { class: 'setup-form', attrs: { novalidate: '', autocomplete: 'off' } });
    form.addEventListener('submit', function (e) { e.preventDefault(); });
    sectionEl.appendChild(form);

    for (var i = 0; i < FIELDS.length; i++) {
      form.appendChild(buildTextField(FIELDS[i], setup[FIELDS[i].name] || '', errors[FIELDS[i].name]));
    }

    // Locales group
    var localesFs = el('fieldset', { class: 'form-field form-field--group' });
    localesFs.appendChild(el('legend', { text: 'Supported locales' }));
    localesFs.appendChild(el('small', {
      class: 'field-help',
      text: 'Resource-builder requires every locale to receive each new string key. "en" is always required.'
    }));
    var localesRow = el('div', { class: 'checkbox-group' });
    for (var li = 0; li < LOCALE_OPTIONS.length; li++) {
      var loc = LOCALE_OPTIONS[li];
      var lbl = el('label', { class: 'checkbox-label' });
      var cb  = el('input', { type: 'checkbox', data: { locale: loc } });
      cb.checked = setup.supportedLocales.indexOf(loc) >= 0;
      if (loc === 'en') { cb.checked = true; cb.disabled = true; }
      lbl.appendChild(cb);
      lbl.appendChild(document.createTextNode(' ' + loc));
      localesRow.appendChild(lbl);
    }
    localesFs.appendChild(localesRow);
    form.appendChild(localesFs);

    // Auth group
    var authFs = el('fieldset', { class: 'form-field form-field--group' });
    authFs.appendChild(el('legend', { text: 'Auth methods' }));
    authFs.appendChild(el('small', {
      class: 'field-help',
      text: 'Leave all unchecked for no third-party auth.'
    }));
    var authRow = el('div', { class: 'checkbox-group' });
    for (var ai = 0; ai < AUTH_OPTIONS.length; ai++) {
      var opt = AUTH_OPTIONS[ai];
      var alb = el('label', { class: 'checkbox-label' });
      var acb = el('input', { type: 'checkbox', data: { auth: opt.value } });
      acb.checked = setup.authMethods.indexOf(opt.value) >= 0;
      alb.appendChild(acb);
      alb.appendChild(document.createTextNode(' ' + opt.label));
      authRow.appendChild(alb);
    }
    authFs.appendChild(authRow);
    form.appendChild(authFs);

    // Toggles
    var togglesFs = el('fieldset', { class: 'form-field form-field--group' });
    togglesFs.appendChild(el('legend', { text: 'Platform & build flags' }));
    var togglesRow = el('div', { class: 'toggles-grid' });
    for (var ti = 0; ti < TOGGLE_DEFS.length; ti++) {
      var td = TOGGLE_DEFS[ti];
      var wrap = el('div', { class: 'toggle-wrap' });
      var tlb  = el('label', { class: 'checkbox-label' });
      var tcb  = el('input', { type: 'checkbox', data: { toggle: td.key } });
      tcb.checked = setup[td.key] === true;
      tlb.appendChild(tcb);
      tlb.appendChild(document.createTextNode(' ' + td.label));
      wrap.appendChild(tlb);
      wrap.appendChild(el('small', { class: 'field-help', text: td.help }));
      togglesRow.appendChild(wrap);
    }
    togglesFs.appendChild(togglesRow);
    form.appendChild(togglesFs);

    // codexEnabled
    var codexFs = el('fieldset', { class: 'form-field form-field--group' });
    codexFs.appendChild(el('legend', { text: 'External review (codexEnabled)' }));
    codexFs.appendChild(el('small', {
      class: 'field-help',
      text: 'auto: detect Codex plugin at runtime. true: require Codex (hard-fail otherwise). false: always use internal-reviewer.'
    }));
    var codexRow = el('div', { class: 'radio-group' });
    var codexCurrent = setup.codexEnabled || 'auto';
    for (var ci = 0; ci < CODEX_VALUES.length; ci++) {
      var cv  = CODEX_VALUES[ci];
      var rlb = el('label', { class: 'radio-label' });
      var rcb = el('input', { type: 'radio', name: 'setup-codex', value: cv, data: { codex: cv } });
      rcb.checked = codexCurrent === cv;
      rlb.appendChild(rcb);
      rlb.appendChild(document.createTextNode(' ' + cv));
      codexRow.appendChild(rlb);
    }
    codexFs.appendChild(codexRow);
    form.appendChild(codexFs);

    // YAML preview
    sectionEl.appendChild(el('h3', { class: 'panel-section-title', text: 'YAML preview' }));
    sectionEl.appendChild(el('p', {
      class: 'panel-lead',
      text: 'Replace the frontmatter block (between the first two --- delimiters) in requirements/00-overview/03-project-config.md with the YAML below.'
    }));
    var yaml = codeBlock(App.helpers.buildYaml(setup));
    yaml.code.id = 'setup-yaml';
    sectionEl.appendChild(yaml.wrap);

    var yamlActions = el('div', { class: 'setup-action' });
    var downloadBtn = el('button', {
      type: 'button',
      class: 'btn',
      text: 'Download .md',
      attrs: { 'aria-label': 'Download 03-project-config.md' }
    });
    downloadBtn.addEventListener('click', function () {
      var form3 = sectionEl.querySelector('.setup-form');
      var snap = form3 ? readFormSetup(form3) : currentSetup();
      downloadConfigMd(snap);
    });
    yamlActions.appendChild(downloadBtn);
    sectionEl.appendChild(yamlActions);

    // Completion checks
    sectionEl.appendChild(el('h3', { class: 'panel-section-title', text: 'Completion checks' }));
    sectionEl.appendChild(el('p', {
      class: 'panel-lead',
      text: 'The site cannot inspect your filesystem — tick each box yourself once you have done the corresponding step.'
    }));
    var checksList = el('div', { class: 'completion-checks' });

    // a) yamlPasted
    var aItem = el('div', { class: 'completion-item' });
    var aLbl  = el('label', { class: 'checkbox-label' });
    var aCb   = el('input', { type: 'checkbox', data: { check: 'yamlPasted' } });
    aCb.checked = checks.yamlPasted;
    aLbl.appendChild(aCb);
    aLbl.appendChild(document.createTextNode(' I copied the YAML (or downloaded the full file) and placed it at requirements/00-overview/03-project-config.md.'));
    aItem.appendChild(aLbl);
    checksList.appendChild(aItem);

    // b) agentsInstalled
    var bItem = el('div', { class: 'completion-item' });
    var bLbl  = el('label', { class: 'checkbox-label' });
    var bCb   = el('input', { type: 'checkbox', data: { check: 'agentsInstalled' } });
    bCb.checked = checks.agentsInstalled;
    bLbl.appendChild(bCb);
    bLbl.appendChild(document.createTextNode(' I installed the sub-agents into .claude/agents/.'));
    bItem.appendChild(bLbl);
    bItem.appendChild(codeBlock(installCommandsText()).wrap);
    checksList.appendChild(bItem);

    // c) requirementsVerified
    var cItem = el('div', { class: 'completion-item' });
    var cLbl  = el('label', { class: 'checkbox-label' });
    var cCb   = el('input', { type: 'checkbox', data: { check: 'requirementsVerified' } });
    cCb.checked = checks.requirementsVerified;
    cLbl.appendChild(cCb);
    cLbl.appendChild(document.createTextNode(' I verified requirements/ is present at the repo root.'));
    cItem.appendChild(cLbl);
    cItem.appendChild(codeBlock('ls requirements/').wrap);
    cItem.appendChild(el('small', {
      class: 'field-help',
      text: 'Expected entries: 00-overview, 01-tech-stack, 02-module-structure, 03-architecture-patterns, 04-base-classes, 05-design-system, 06-data-layer, 07-mappers, 08-dependency-injection, 09-conventions, 10-toolkit, 11-state-and-formatters, 12-gradle-build, 13-anti-patterns, 14-cookbook, README.md, launch.md, sub-agents/, tasks/.'
    }));
    checksList.appendChild(cItem);

    sectionEl.appendChild(checksList);

    // Primary action
    var action = el('div', { class: 'setup-action' });
    var primary = el('button', {
      type: 'button',
      class: 'btn btn--primary',
      text: 'Mark setup complete',
      data: { action: 'mark-complete' }
    });
    primary.addEventListener('click', function () {
      if (primary.disabled) return;
      var form2 = sectionEl.querySelector('.setup-form');
      var snap = form2 ? readFormSetup(form2) : currentSetup();
      App.store.saveSetup(snap);
      App.store.saveProgress({ setupDone: true });
      App.router.go('wizard');
    });
    action.appendChild(primary);
    sectionEl.appendChild(action);

    wireForm(form);
    wireChecks(checksList);
    updatePrimaryState(setup, errors);
  }

  App.panels.setup = {
    mount: function (rootEl) {
      sectionEl = rootEl;
      render();
    },
    refresh: function () {
      render();
    }
  };
})();
