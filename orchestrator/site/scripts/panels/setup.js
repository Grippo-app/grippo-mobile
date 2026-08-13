import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { store } from '../store.js';
import { clipboard } from '../clipboard.js';
import { helpers } from '../data/wizard-steps.js';
import { router } from '../router.js';
import { runControl } from '../run-control.js';
import { reviewerApi } from '../reviewer-api.js';
import { reviewerErrorMessage } from '../reviewer-errors.js';

  var sectionEl = null;
  var lastAppIdSuggestion = '';
  var setupIntroDismissed = false;
  // Tracks CLI readiness so logging in (header) can surgically add/remove the
  // ▶ Run affordance without a full re-render — see updateCliRunButton() and
  // onStoreChange (a full render would drop the user's in-progress typing).
  var lastCliReady = false;

  function t(key, params) {
    if (i18n && typeof i18n.t === 'function') {
      return i18n.t(key, params);
    }
    return key;
  }

  // Field defs reference i18n keys; the actual user-facing strings live in
  // the domain dictionaries under scripts/i18n/dictionaries/ and are resolved
  // at render time so the form re-localizes when the user flips EN/RU/UA.
  var FIELDS = [
    {
      name: 'productName',
      labelKey: 'setup.field.productName.label',
      placeholder: 'SampleApp',
      required: true,
      pattern: /^[A-Z][A-Za-z0-9]*$/,
      hintKey: 'setup.field.productName.hint',
      tooltipKey: 'setup.field.productName.tooltip'
    },
    {
      name: 'orgName',
      labelKey: 'setup.field.orgName.label',
      placeholder: 'example',
      required: false,
      pattern: /^[a-z][a-z0-9]*$/,
      hintKey: 'setup.field.orgName.hint',
      tooltipKey: 'setup.field.orgName.tooltip'
    },
    {
      name: 'backendHost',
      labelKey: 'setup.field.backendHost.label',
      placeholder: 'api.example.com',
      required: true,
      pattern: /^[a-z0-9.-]+\.[a-z]{2,}$/,
      hintKey: 'setup.field.backendHost.hint',
      tooltipKey: 'setup.field.backendHost.tooltip'
    },
    {
      name: 'applicationId',
      labelKey: 'setup.field.applicationId.label',
      placeholder: 'com.example.sampleapp',
      required: true,
      pattern: /^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/,
      hintKey: 'setup.field.applicationId.hint',
      tooltipKey: 'setup.field.applicationId.tooltip'
    },
    {
      name: 'iosFrameworkName',
      labelKey: 'setup.field.iosFrameworkName.label',
      placeholder: 'shared',
      required: true,
      pattern: /^[a-zA-Z][a-zA-Z0-9]*$/,
      hintKey: 'setup.field.iosFrameworkName.hint',
      tooltipKey: 'setup.field.iosFrameworkName.tooltip'
    },
    {
      name: 'typefaceFactory',
      labelKey: 'setup.field.typefaceFactory.label',
      placeholder: 'inter',
      required: true,
      pattern: /^[a-z][a-zA-Z0-9_]*$/,
      hintKey: 'setup.field.typefaceFactory.hint',
      tooltipKey: 'setup.field.typefaceFactory.tooltip'
    },
    {
      name: 'firstDomain',
      labelKey: 'setup.field.firstDomain.label',
      placeholder: 'Item',
      required: true,
      pattern: /^[A-Z][A-Za-z0-9]*$/,
      hintKey: 'setup.field.firstDomain.hint',
      tooltipKey: 'setup.field.firstDomain.tooltip'
    }
  ];

  var LOCALE_OPTIONS = ['en', 'uk', 'ru', 'de', 'fr', 'es'];

  var AUTH_OPTIONS = [
    { value: 'email-password', labelKey: 'setup.auth.email' },
    { value: 'google',         labelKey: 'setup.auth.google' },
    { value: 'apple',          labelKey: 'setup.auth.apple' }
  ];

  var TOGGLE_DEFS = [
    { key: 'iosEnabled',      labelKey: 'setup.toggle.iosEnabled.label',      helpKey: 'setup.toggle.iosEnabled.help' },
    { key: 'firebaseEnabled', labelKey: 'setup.toggle.firebaseEnabled.label', helpKey: 'setup.toggle.firebaseEnabled.help' },
    { key: 'prelaunch',       labelKey: 'setup.toggle.prelaunch.label',       helpKey: 'setup.toggle.prelaunch.help' }
  ];

  var REVIEWER_MODES = [
    { mode: 'automatic', value: 'auto', labelKey: 'codex.mode.automatic.label' },
    { mode: 'require-codex', value: 'true', labelKey: 'codex.mode.require.label' },
    { mode: 'internal-only', value: 'false', labelKey: 'codex.mode.internal.label' }
  ];
  var SCREENSHOT_PIXEL_VALUES = ['strict', 'advisory', 'off'];   // Figma-only verdict routing; strict is the safe default.

  // Manual-install reference for .claude/skills/ — step 1's prompt already does
  // this, these are the by-hand alternatives surfaced under "Next steps".
  var INSTALL_SYMLINK = 'bash orchestrator/skills/install-skills.sh . --symlink';

  var INSTALL_COPY = 'bash orchestrator/skills/install-skills.sh .';

  var INSTALL_CLAUDE_CODE =
    'Install the skills into .claude/skills/ — from the project root run `bash orchestrator/skills/install-skills.sh .` (use --symlink to keep edits in orchestrator/skills/ live).\n' +
    '\n' +
    INSTALL_SYMLINK +
    '\n' +
    '\nThen verify with `bash orchestrator/skills/checks/install-sync.sh .`; it checks all 11 skills, references, frozen contracts, the queue command, and launch configuration.\n' +
    '\nThen wire the screenshot-gate enforcement (install-skills.sh does this automatically — run by hand only if skipped):\n' +
    '\n' +
    'git config core.hooksPath orchestrator/skills/checks/hooks';

  // SYNC: mirrors the body of orchestrator/project-config.md
  // (everything after the YAML frontmatter). When you edit that file, update
  // this constant — the site embeds CONFIG_BODY into the Claude Code prompt
  // that writes the file (the dev server does not expose the .md for live read).
  var CONFIG_BODY = [
    '',
    '> **Fresh-project state.** Every value in the frontmatter above is a placeholder or a neutral default. Before invoking any skill (`task-orchestrator`, the builder skills, the validation gates), replace the required identity/build placeholders with project-specific values per `orchestrator/launch.md` Step 1.5. The optional Figma placeholder may remain until that integration is enabled. Fresh projects start without `orchestrator/api-contract/environments.json`; Integrations → Backend creates the sole canonical Backend source manifest when the first source is added. Empty arrays (`featuresWithRootComponentSuffix: []`, `diHandWrittenModules: []`) stay empty until the project actually needs them — the orchestrator appends the former on demand; you append the latter by hand when a module deliberately uses a hand-written Koin `module { … }` block.',
    '',
    '# Project config — core scalar source of truth',
    '',
    'Replace every required identity/build value in the frontmatter above before the first bootstrap. Domain manifests may own structured integration settings without duplicating them here: `orchestrator/api-contract/environments.json` is the canonical Backend source manifest whenever it exists.',
    '',
    'The skills under `.claude/skills/` read this config before acting. When you bootstrap a new project from these skills, copy this file and update the required values.',
    '',
    '## Field meanings',
    '',
    '- `productName` — used in class prefixes (`<Product>Api`, `<Product>Component`).',
    '- `productPackage` — Kotlin package root.',
    '- `apiClassName` — name of the flat backend API class.',
    '- `backendHost` — used in `BackendClient.defaultRequest`.',
    '- `applicationId` — Android Play Store id; also used as the iOS bundle id (no platform suffix).',
    '- `iosFrameworkName` — XCFramework target name.',
    '- `iosEnabled` — if false, build-validator skips iOS gates.',
    '- `firebaseEnabled` — if false, agents skip Firebase wiring.',
    '- `codexEnabled` — machine backing field for the Reviewer mode. Manage it through',
    '  the Site\'s Reviewer control: **Automatic** selects Codex only when the shared',
    '  local readiness detector confirms it is available, **Require Codex** blocks',
    '  review when it is unavailable, and **Internal review only** always selects',
    '  the internal reviewer. Once an attempt starts, the selected reviewer is',
    '  locked; invocation failure never silently switches reviewers.',
    '- `verifyEnabled` — controls the post-validator runtime-verify gate (orchestrator Step 4.6 — runs after validators are green, before external review). One of:',
    '  - `auto` (default) — orchestrator invokes the Anthropic `verify` skill if the `Skill` tool is available in its runtime; if not, emits a manual-verify hint in the summary and records the gate as `deferred`. No hard fail.',
    '  - `true` — force runtime verify; orchestrator hard-fails when the `Skill` tool is unavailable (use this when you want CI-like discipline).',
    '  - `false` — skip the runtime-verify gate entirely. Useful for headless / CI environments where the app cannot be launched.',
    '- `prelaunch` — if true, room-migration-builder allows destructive fallback without a migration.',
    '- `supportedLocales` — resource-builder requires every locale to receive each new key.',
    '- `typefaceFactory` — resource-builder uses this factory function name when registering fonts.',
    '- `featuresWithRootComponentSuffix` — features that use `*RootComponent.kt` instead of bare `*Component.kt` (because they have a sub-screen with the same name as the feature). Start empty (`[]`) on a fresh project; the orchestrator appends a feature name only when its first sub-screen collides with the feature root.',
    '- `diHandWrittenModules` — Koin modules that legitimately use the hand-written `module { … }` DSL outside annotated `@Single` classes (platform-edge wrappers, etc.). `di-validator` reads this list before flagging hand-DSL hits. Start empty (`[]`) on a fresh project; append a module name only when a hand-written `module { … }` block is deliberately introduced (typically platform-edge wrappers like Google/Apple auth).',
    '- `figmaEnabled` — opt-in gate for the Figma → MCP → Compose tooling (the `orchestrator/figma/` sidecar + the Figma-aware skill flow). Default `false`: `launch.md` Step 6.5 is skipped, the Figma validators no-op, and the project is byte-for-byte a non-Figma project. When `true`, beyond the validators it also activates the UI-task design discipline: the `## Design`-section rule on screen/dialog tasks, the per-task screen cache (`figma:screens:<stem>` session, gated by the orchestrator\'s Step 1b pre-flight), the component census + design-system-first split (`task-prep` Step 5.5), and the `figma-spec-validator` gate. Set `true` only when the project has a Figma design library to bind. See the `implement-figma` skill.',
    '- `figmaLibraryUrl` — the project\'s Figma design-library URL (the file/library the MCP binds to and the token/component pipelines read). Per-project; leave the `<figma-library-url>` placeholder until the project actually binds a library. The MCP is OAuth-bound per project — no access secret lives in config (there is no REST/token fallback).',
    '- `screenshotPixelGate` — routes the screenshot gate\'s **pixel-similarity** verdict (only relevant when `figmaEnabled: true`; harmless otherwise). One of:',
    '  - `strict` (default) — a pixel-similarity divergence (SSIM band / per-zone floor / colour) may **BLOCK**. Every bootstrap starts strict so pixel drift fails closed from day one.',
    '  - `advisory` — the pixel comparison is computed and the 3-frame Figma/overlay/app evidence is always shown, but a similarity divergence is a **WARN**, never a hard block. The design-agnostic **structural** gate (`figma-spec-validator`) and the completeness/anti-forgery net (missing/stale/tampered captures, coverage) remain the strict signals, unaffected by this knob. Downgrade to this per-project only when a single global pixel threshold demonstrably over-blocks the design language.',
    '  - `off` — computed and shown only; similarity findings are suppressed entirely. The comparison always runs at full canonical metric strictness regardless of the mode (only the verdict is routed); the orchestrator passes the value to `compare-screenshots.mjs` via `SCREENSHOT_PIXEL_GATE`.',
    '- `designLocale` — OPTIONAL: the DESIGN language (one of `supportedLocales`, e.g. `designLocale: uk`) — the locale the Figma frames are written in, which the screenshot capture must render (`check-capture-config` derives the `@Config` locale segment from it; the comparator cross-checks the capture manifest\'s `localeTag` against it). Absent = auto-detected deterministically from the pulled spec texts × the app\'s string resources (`lib/design-locale.mjs`); add the key when detection reports `CAPTURE_LOCALE_UNDERIVABLE` (votable design text with no decisive locale match) or to pin the language explicitly. A value outside `supportedLocales` is a config error (fail-closed), never a silent skip.',
    '- `androidAssembleTask` / `sharedFrameworkTask` / `roborazziRecordTask` / `moduleCompileTask` — the project\'s CANONICAL Gradle task names, read by the Figma tooling instead of hardcoding a module layout. `androidAssembleTask`/`sharedFrameworkTask` are FULL task paths (the standard build-gate acceptance bullets embed them; the framework gate applies only when `iosEnabled: true`); `roborazziRecordTask` is the per-module record-task SUFFIX (`run-figma-gates.mjs --stage screenshot` invokes `<module>:<suffix>`); `moduleCompileTask` is the per-module KMP compile SUFFIX (`compileAndroidMain` for `com.android.kotlin.multiplatform.library` modules). Absent keys use the documented template defaults.',
    '- `backendContractEnabled` — tri-state gate for the backend API contract tooling (the `orchestrator/api-contract/` sidecar + the contract-aware data-layer flow). See the `backend-contract-client` skill. One of:',
    '  - `auto` (default) — agents use the validated current generation when it exists; `backend-contract-drift` may report `SKIPPED (no snapshot)` for unrelated greenfield work, but endpoint/DTO work is `BLOCKED` until Backend Test + Refresh publishes a snapshot. Task text is never a substitute for the server contract.',
    '  - `true` — require the snapshot: `endpoint-builder` stops (`BLOCKED`) when an endpoint is missing from the inventory instead of guessing; the drift validator must run.',
    '  - `false` — disable the contract tooling: `launch.md` Step 6.6 is skipped and the contract validator no-ops.',
    'Backend credentials never belong in this file. Bearer tokens or Postman API keys are guarded local files under `orchestrator/api-contract/.secrets/<environment-id>.token`; the manifest stores only `authRef` and `authKind`.',
    '',
    '## Updating',
    '',
    'When a value changes (new locale added, app ships and `prelaunch` flips, codex installed), update this file. The skills read it lazily — no rebuild needed.',
    ''
  ].join('\n');

  function debounce(fn, ms) {
    var t = null;
    var lastArgs = null;
    var lastSelf = null;
    function debounced() {
      lastArgs = arguments;
      lastSelf = this;
      if (t) clearTimeout(t);
      t = setTimeout(function () {
        t = null;
        fn.apply(lastSelf, lastArgs);
      }, ms);
    }
    // Run a pending call immediately (e.g. before a re-render that would discard
    // the last keystrokes not yet flushed by the timer).
    debounced.flush = function () {
      if (!t) return;
      clearTimeout(t);
      t = null;
      fn.apply(lastSelf, lastArgs);
    };
    return debounced;
  }

  function setupDefaults() {
    return {
      productName: '',
      orgName: '',
      backendHost: '',
      applicationId: '',
      iosFrameworkName: 'shared',
      typefaceFactory: '',
      firstDomain: '',
      supportedLocales: ['en'],
      authMethods: [],
      iosEnabled: true,
      firebaseEnabled: true,
      codexEnabled: 'auto',
      prelaunch: true,
      figmaEnabled: false,
      figmaLibraryUrl: '',
      screenshotPixelGate: 'strict'
    };
  }

  function currentSetup() {
    var stored = store.get().setup || {};
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

  function canonicalReviewerValue() {
    var state = store.get();
    var config = state.reviewerConfig || {};
    var value = (state.setup || {}).codexEnabled;
    return config.state === 'ready' && (value === 'auto' || value === 'true' || value === 'false')
      ? value : null;
  }

  function reviewerValueForForm() {
    var canonical = canonicalReviewerValue();
    if (canonical !== null) return canonical;
    var config = store.get().reviewerConfig || {};
    // A genuinely absent project-config needs a complete bootstrap draft.
    // Automatic is the documented bootstrap default, not an operational
    // fallback: the disabled control cannot persist until the file exists.
    return config.state === 'missing' && config.revision === null &&
      config.canUpdate !== true ? 'auto' : null;
  }

  function syncReviewerControls(controls, value) {
    if (!controls) return;
    for (var i = 0; i < controls.length; i++) {
      controls[i].checked = value !== null && controls[i].value === value;
    }
  }

  function currentChecks() {
    // The three setup gates (yamlPasted, agentsInstalled, requirementsVerified)
    // are derived server-side from the filesystem and surfaced in
    // state.progress. Each indicator mirrors that server-derived gate, plus an
    // escape-hatch override (buildStatusItem's "mark done manually / clear"
    // toggle) the user can set/clear, OR-combined into the gate server-side.
    var prog = store.get().progress || {};
    return {
      yamlPasted:            prog.yamlPasted === true,
      agentsInstalled:       prog.agentsInstalled === true,
      requirementsVerified:  prog.requirementsVerified === true
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
    return pkg;
  }

  function validate(setup) {
    var errors = {};
    for (var i = 0; i < FIELDS.length; i++) {
      var f = FIELDS[i];
      var s = setup[f.name] == null ? '' : String(setup[f.name]);
      if (s.length === 0) {
        if (f.required) errors[f.name] = t('common.required');
        continue;
      }
      if (f.pattern && !f.pattern.test(s)) {
        errors[f.name] = f.hintKey ? t(f.hintKey) : '—';
      }
    }
    if (setup.codexEnabled !== 'auto' && setup.codexEnabled !== 'true' && setup.codexEnabled !== 'false') {
      errors.reviewerMode = t('setup.reviewerInvalid');
    }
    return errors;
  }

  function buildSetupClaudePrompt(setup) {
    var content = helpers.buildYaml(setup) + '\n' + CONFIG_BODY;
    return [
      'Bootstrap this project. Three steps — stop and report if any fails.',
      '',
      '## 1. Verify orchestrator/ is at the project root',
      '',
      'Run `ls orchestrator/` via the Bash tool. Expect at minimum these entries: `skills/`, `project-config.md`, `tasks/`, `site/`, `launch.md`, `README.md`. Then run `ls orchestrator/skills/` and expect the 11 skill directories plus `_index/`.',
      '',
      'If any are missing, STOP and report verbatim: `BLOCKED: orchestrator/ not at project root. Copy it from the source repo before retrying.` Do not proceed to step 2.',
      '',
      '## 2. Write the project config',
      '',
      'If `orchestrator/project-config.md` is absent, create it with exactly the fenced block below. If it already exists, read it first and replace only its opening YAML frontmatter (the first `---` block) with the fenced block\'s frontmatter; preserve every byte after the existing closing `---`, including product-specific notes. Never overwrite or normalize the existing Markdown body.',
      '',
      '```markdown',
      content,
      '```',
      '',
      '## 3. Install the skills',
      '',
      'Run this from the project root via the Bash tool. `--symlink` keeps edits in `orchestrator/skills/` live in `.claude/skills/`:',
      '',
      '```bash',
      INSTALL_SYMLINK,
      '```',
      '',
      'Verify with `bash orchestrator/skills/checks/install-sync.sh .` — it must confirm all 11 skills, references, frozen contracts, the queue command, and launch configuration.',
      '',
      'Report `Bootstrap setup complete` once all three steps succeed.'
    ].join('\n');
  }

  // DOM helper — el() lives in scripts/dom.js (dom.el).
  var el = dom.el;

  function codeBlock(text) {
    var wrap = el('div', { class: 'code-block-wrapper' });
    var pre  = el('pre', { class: 'code-block' });
    var code = el('code', { text: text });
    pre.appendChild(code);
    wrap.appendChild(pre);
    var btn = el('button', { type: 'button', class: 'copy-btn', text: t('common.copy'), attrs: { 'aria-label': t('common.copyAria') } });
    clipboard.attach(btn, function () { return code.textContent; });
    wrap.appendChild(btn);
    return { wrap: wrap, code: code };
  }

  function buildTextField(f, value, errorMsg) {
    var fid = 'setup-' + f.name;
    var field = el('div', { class: 'form-field' });
    field.appendChild(el('label', {
      attrs: { 'for': fid },
      text:  t(f.labelKey) + (f.required ? '' : t('common.optional'))
    }));
    var input = el('input', {
      type: 'text',
      class: 'input',
      id: fid,
      value: value,
      data: { field: f.name },
      // The server drops any setup string over 200 chars and answers 200 OK, so
      // without this cap a longer value looked saved, survived in the form (and
      // in the copied bootstrap prompt), and vanished on reload.
      attrs: { placeholder: f.placeholder, spellcheck: 'false', autocomplete: 'off', maxlength: '200' }
    });
    field.appendChild(input);
    if (f.tooltipKey) field.appendChild(el('small', { class: 'field-help', text: t(f.tooltipKey) }));
    var err = el('small', { class: 'field-error', text: errorMsg || '' });
    if (!errorMsg) err.hidden = true;
    field.appendChild(err);
    return field;
  }

  function readFormSetup(form) {
    var out = currentSetup();
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
    for (var ti = 0; ti < tcbs.length; ti++) {
      var tc = tcbs[ti];
      out[tc.getAttribute('data-toggle')] = tc.checked;
    }

    var codex = form.querySelector('[data-reviewer-mode]:checked');
    out.codexEnabled = codex ? codex.value : '<choose-reviewer-mode>';

    var sp = form.querySelector('[data-sp]:checked');
    out.screenshotPixelGate = sp ? sp.value : 'strict';

    return out;
  }

  function collectProblems(setup, errors, checks) {
    var probs = [];
    for (var i = 0; i < FIELDS.length; i++) {
      var f = FIELDS[i];
      if (errors[f.name]) probs.push(t('setup.fieldInvalid', { label: t(f.labelKey) }));
    }
    if (errors.reviewerMode) probs.push(t('setup.problem.reviewerMode'));
    if (!checks.requirementsVerified) probs.push(t('setup.problem.requirementsVerified'));
    if (!checks.yamlPasted)           probs.push(t('setup.problem.yamlPasted'));
    if (!checks.agentsInstalled)      probs.push(t('setup.problem.agentsInstalled'));
    return probs;
  }

  function updatePrimaryState(setup, errors) {
    var primary = sectionEl && sectionEl.querySelector('[data-action="mark-complete"]');
    if (!primary) return;
    // Show/hide the connective hint in step with the button — the disabled-button
    // title never surfaces (disabled controls don't fire hover/focus), so the
    // visible line is the only on-screen pointer back to the gate checklist.
    var hint = sectionEl.querySelector('.setup-continue-hint');
    var probs = collectProblems(setup, errors, currentChecks());
    if (probs.length === 0) {
      primary.disabled = false;
      primary.removeAttribute('title');
      if (hint) hint.hidden = true;
    } else {
      primary.disabled = true;
      primary.setAttribute('title', t('common.resolveBefore', { problems: probs.join('; ') }));
      if (hint) hint.hidden = false;
    }
  }

  // Fields that actually feed buildYaml / the bootstrap prompt. firstDomain is a
  // Step-8 input — it is NOT serialized into project-config.md — so it must
  // not gate Copy/Run (its pattern is still validated for the Step-8 gate).
  var BOOTSTRAP_FIELDS = ['productName', 'orgName', 'backendHost', 'applicationId', 'iosFrameworkName', 'typefaceFactory'];

  // Field-error names (empty required / pattern-fail) among the YAML-feeding
  // fields, for the bootstrap-button gate below. Distinct from collectProblems
  // (which also folds in the three FS gates) — Copy/Run is what SATISFIES those
  // gates, so we block it only on invalid form input, never on a not-yet-passed
  // gate.
  function invalidFieldLabels(errors) {
    var labels = [];
    for (var i = 0; i < BOOTSTRAP_FIELDS.length; i++) {
      var f = fieldByName(BOOTSTRAP_FIELDS[i]);
      if (f && errors[f.name]) labels.push(t(f.labelKey));
    }
    if (errors.reviewerMode) labels.push(t('setup.cardReview'));
    return labels;
  }

  // Gate the Copy-prompt and ▶ Run buttons on form validity. An empty/invalid
  // form yields a placeholder-laden project-config.md that permanently fails
  // the yamlPasted gate, so disabling here stops the bootstrap at the source and
  // the title surfaces the cause (which the Continue tooltip alone hid).
  function updateBootstrapButtonsState(errors) {
    if (!sectionEl) return;
    var actions = sectionEl.querySelector('.setup-step-actions');
    if (!actions) return;
    var bad = invalidFieldLabels(errors);
    var btns = actions.querySelectorAll('[data-bootstrap-prompt], [data-run-control="setup"]');
    for (var i = 0; i < btns.length; i++) {
      var b = btns[i];
      // A run-control button already in the Terminal state reopens a LIVE
      // session — never disable it. The form-validity gate is about blocking the
      // START of a placeholder-laden bootstrap, not reaching an in-flight
      // terminal; disabling it here would strand the running session's console.
      if (b.getAttribute('data-state') === 'terminal') continue;
      if (bad.length === 0) {
        b.disabled = false;
        b.removeAttribute('title');
      } else {
        b.disabled = true;
        b.setAttribute('title', t('setup.fillBeforeBootstrap', { fields: bad.join(', ') }));
      }
    }
  }

  // The "Saved" pill must follow the SERVER outcome. Bound to the store's
  // optimistic notify() it pulsed before the write was even attempted, so a
  // failed save showed "Saved" and an error at the same time — and the value
  // the user typed was silently never persisted. The store toasts failures.
  function userSaveSetup(snapshot) {
    var persisted = Object.assign({}, snapshot);
    delete persisted.codexEnabled;
    store.saveSetup(persisted).then(pulseSaveIndicator, function () {});
  }
  function userSaveManualStep(stepId, done) {
    store.saveManualStep(stepId, done).then(pulseSaveIndicator, function () {});
  }

  var debouncedSaveSetup = debounce(function (snapshot) {
    // saveSetup posts the diff to the server; the optimistic local
    // update inside the store keeps the form responsive while the round
    // trip completes.
    userSaveSetup(snapshot);
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
      if (yamlEl) yamlEl.textContent = helpers.buildYaml(snap);
      refreshErrors(form, errs);
      updatePrimaryState(snap, errs);
      updateBootstrapButtonsState(errs);
    }

    var inputs = form.querySelectorAll('input');
    for (var i = 0; i < inputs.length; i++) {
      var ip = inputs[i];
      if (ip.hasAttribute('data-reviewer-mode')) continue;
      var ipType = ip.type;
      if (ipType === 'checkbox' || ipType === 'radio') ip.addEventListener('change', handle);
      else ip.addEventListener('input', handle);
    }
  }

  // Small builders for sub-groups. Each returns a fieldset that snaps into
  // a parent card. Kept here (not at module scope) so they close over `setup`
  // — the form re-builds on every render, so capture is fine.
  function buildLocalesGroup(setup) {
    var fs = el('fieldset', { class: 'form-field form-field--group' });
    fs.appendChild(el('legend', { text: t('setup.localesLegend') }));
    fs.appendChild(el('small', { class: 'field-help', text: t('setup.localesHelp') }));
    var row = el('div', { class: 'checkbox-group' });
    for (var li = 0; li < LOCALE_OPTIONS.length; li++) {
      var loc = LOCALE_OPTIONS[li];
      var lbl = el('label', { class: 'checkbox-label' });
      var cb  = el('input', { type: 'checkbox', class: 'choice-input', data: { locale: loc } });
      cb.checked = setup.supportedLocales.indexOf(loc) >= 0;
      if (loc === 'en') { cb.checked = true; cb.disabled = true; }
      lbl.appendChild(cb);
      lbl.appendChild(document.createTextNode(' ' + loc));
      row.appendChild(lbl);
    }
    fs.appendChild(row);
    return fs;
  }

  function buildAuthGroup(setup) {
    var fs = el('fieldset', { class: 'form-field form-field--group' });
    fs.appendChild(el('legend', { text: t('setup.authLegend') }));
    fs.appendChild(el('small', { class: 'field-help', text: t('setup.authHelp') }));
    var row = el('div', { class: 'checkbox-group' });
    for (var ai = 0; ai < AUTH_OPTIONS.length; ai++) {
      var opt = AUTH_OPTIONS[ai];
      var lbl = el('label', { class: 'checkbox-label' });
      var cb  = el('input', { type: 'checkbox', class: 'choice-input', data: { auth: opt.value } });
      cb.checked = setup.authMethods.indexOf(opt.value) >= 0;
      lbl.appendChild(cb);
      lbl.appendChild(document.createTextNode(' ' + t(opt.labelKey)));
      row.appendChild(lbl);
    }
    fs.appendChild(row);
    return fs;
  }

  function buildTogglesGroup(setup) {
    var row = el('div', { class: 'toggles-grid' });
    for (var ti = 0; ti < TOGGLE_DEFS.length; ti++) {
      var td = TOGGLE_DEFS[ti];
      var wrap = el('div', { class: 'toggle-wrap' });
      var lbl  = el('label', { class: 'checkbox-label' });
      var cb   = el('input', { type: 'checkbox', class: 'choice-input', data: { toggle: td.key } });
      cb.checked = setup[td.key] === true;
      lbl.appendChild(cb);
      lbl.appendChild(document.createTextNode(' ' + t(td.labelKey)));
      wrap.appendChild(lbl);
      wrap.appendChild(el('small', { class: 'field-help', text: t(td.helpKey) }));
      row.appendChild(wrap);
    }
    return row;
  }

  function buildCodexGroup() {
    var wrap = el('div', { class: 'form-field' });
    wrap.appendChild(el('small', { class: 'field-help', text: t('setup.codexHelp') }));
    var row = el('div', {
      class: 'radio-group',
      attrs: { role: 'radiogroup', 'aria-label': t('setup.codexHelp') }
    });
    var reviewerConfig = store.get().reviewerConfig || {};
    var canUpdate = reviewerConfig.canUpdate === true;
    var current = reviewerValueForForm();
    for (var ci = 0; ci < REVIEWER_MODES.length; ci++) {
      var def = REVIEWER_MODES[ci];
      var lbl = el('label', { class: 'radio-label' });
      var rb  = el('input', {
        type: 'radio',
        class: 'choice-input',
        name: 'setup-codex',
        value: def.value,
        data: { 'reviewer-mode': def.mode }
      });
      rb.checked = current === def.value;
      rb.disabled = !canUpdate;
      lbl.appendChild(rb);
      lbl.appendChild(document.createTextNode(' ' + t(def.labelKey)));
      row.appendChild(lbl);
    }
    wrap.appendChild(row);
    var feedback = el('small', {
      class: 'field-help',
      text: reviewerConfig.state === 'ready' ? ''
        : reviewerConfig.state === 'missing'
          ? t(canUpdate ? 'setup.reviewerAddMissing' : 'setup.reviewerBootstrapDefault')
          : t('setup.reviewerInvalid'),
      attrs: { 'aria-live': 'polite', 'data-reviewer-feedback': '1' }
    });
    wrap.appendChild(feedback);
    row.addEventListener('change', function (event) {
      var input = event.target;
      if (!input || !input.hasAttribute('data-reviewer-mode')) return;
      var currentConfig = store.get().reviewerConfig || {};
      var revision = currentConfig.revision || '';
      var controls = row.querySelectorAll('input');
      var canonicalBeforeSave = canonicalReviewerValue();
      if (currentConfig.canUpdate !== true || !revision) {
        feedback.textContent = currentConfig.state === 'missing'
          ? t('setup.reviewerBootstrapDefault') : t('setup.reviewerInvalid');
        syncReviewerControls(controls, canonicalBeforeSave);
        return;
      }
      row.setAttribute('data-reviewer-saving', '1');
      for (var i = 0; i < controls.length; i++) controls[i].disabled = true;
      feedback.textContent = t('codex.settings.saving');
      var yamlEl = sectionEl.querySelector('#setup-yaml');
      var form = sectionEl.querySelector('.setup-form');
      if (yamlEl && form) yamlEl.textContent = helpers.buildYaml(readFormSetup(form));
      reviewerApi.save(input.getAttribute('data-reviewer-mode'), revision).then(function () {
        feedback.textContent = t('codex.settings.saved');
        // The endpoint response already confirms the canonical write.
        return store.load().catch(function () {});
      }, function (error) {
        var failureMessage = reviewerErrorMessage(error, 'settings');
        return store.load().catch(function () {}).then(function () {
          var canonical = canonicalReviewerValue();
          feedback.textContent = canonical === input.value ? t('codex.settings.saved') : failureMessage;
          syncReviewerControls(controls, canonical);
          var currentForm = sectionEl.querySelector('.setup-form');
          if (currentForm) {
            var restored = readFormSetup(currentForm);
            var restoredErrors = validate(restored);
            if (yamlEl) yamlEl.textContent = helpers.buildYaml(restored);
            updatePrimaryState(restored, restoredErrors);
            updateBootstrapButtonsState(restoredErrors);
          }
        });
      }).finally(function () {
        row.removeAttribute('data-reviewer-saving');
        var latestConfig = store.get().reviewerConfig || {};
        for (var i = 0; i < controls.length; i++) {
          controls[i].disabled = latestConfig.canUpdate !== true;
        }
      });
    });
    return wrap;
  }

  function buildScreenshotPixelGroup(setup) {
    var wrap = el('div', { class: 'form-field' });
    wrap.appendChild(el('small', { class: 'field-help', text: t('setup.screenshotPixelHelp') }));
    var row = el('div', { class: 'radio-group' });
    var current = setup.screenshotPixelGate || 'strict';
    for (var si = 0; si < SCREENSHOT_PIXEL_VALUES.length; si++) {
      var sv  = SCREENSHOT_PIXEL_VALUES[si];
      var lbl = el('label', { class: 'radio-label' });
      var rb  = el('input', { type: 'radio', class: 'choice-input', name: 'setup-sp', value: sv, data: { sp: sv } });
      rb.checked = current === sv;
      lbl.appendChild(rb);
      lbl.appendChild(document.createTextNode(' ' + sv));
      row.appendChild(lbl);
    }
    wrap.appendChild(row);
    return wrap;
  }

  function fieldByName(name) {
    for (var i = 0; i < FIELDS.length; i++) {
      if (FIELDS[i].name === name) return FIELDS[i];
    }
    return null;
  }

  function buildCard(index, titleKey, descriptionKey) {
    var card = el('section', { class: 'setup-card' });
    card.appendChild(el('header', { class: 'setup-card-head' }, [
      el('span', { class: 'setup-card-num', text: String(index), attrs: { 'aria-hidden': 'true' } }),
      el('div', { class: 'setup-card-heading' }, [
        el('h3', { class: 'setup-card-title', text: t(titleKey) }),
        el('p', { class: 'setup-card-description', text: t(descriptionKey) })
      ])
    ]));
    return card;
  }

  function isFreshSetup(setup, setupDone) {
    if (setupDone) return false;
    return !setup.productName && !setup.orgName && !setup.backendHost && !setup.applicationId &&
      !setup.typefaceFactory && !setup.firstDomain;
  }

  function renderFirstSetup() {
    var titleId = 'setup-first-run-title';
    var action = el('button', {
      type: 'button',
      class: 'btn btn--primary first-run-card__action',
      text: t('setup.intro.action')
    });
    action.addEventListener('click', function () {
      setupIntroDismissed = true;
      render();
      setTimeout(function () {
        var firstField = sectionEl && sectionEl.querySelector('#setup-productName');
        if (firstField) firstField.focus();
      }, 0);
    });
    sectionEl.appendChild(el('section', {
      class: 'first-run-card first-run-card--setup',
      attrs: { 'aria-labelledby': titleId }
    }, [
      el('div', { class: 'first-run-card__main' }, [
        el('span', { class: 'first-run-card__eyebrow', text: t('setup.intro.eyebrow') }),
        el('h3', { id: titleId, class: 'panel-section-title first-run-card__title', text: t('setup.intro.title') }),
        el('p', { class: 'first-run-card__body', text: t('setup.intro.body') }),
        action
      ]),
      el('ol', { class: 'first-run-card__steps', attrs: { 'aria-label': t('setup.intro.stepsLabel') } }, [
        el('li', { text: t('setup.intro.stepConfig') }),
        el('li', { text: t('setup.intro.stepBootstrap') }),
        el('li', { text: t('setup.intro.stepWizard') })
      ])
    ]));
  }

  function appendField(parent, fieldName, setup, errors) {
    var f = fieldByName(fieldName);
    if (!f) return;
    parent.appendChild(buildTextField(f, setup[f.name] || '', errors[f.name]));
  }

  function buildStatusItem(checkKey, done) {
    // Status row for a setup gate. ✓ = the bootstrap prompt landed its FS effect
    // (or the user manually marked it); ○ = not yet. The server is the source of
    // truth; the indicator flips automatically when the watcher detects the
    // change. An escape-hatch toggle (mirroring the wizard's per-step "Mark
    // done") lets the user assert a gate when a validator false-negatives — so an
    // unusual-but-valid setup can't dead-end the pipeline with no recourse.
    var gates = (store.get().progress && store.get().progress.setupGates) || {};
    var g = gates[checkKey] || { fsDone: done, overridden: false };
    var li = el('li', {
      class: 'setup-check-item' + (done ? ' setup-check-item--done' : ' setup-check-item--pending')
    });
    // The ✓/○ glyph is decorative, but the done/pending state must not be
    // color-only — expose it to assistive tech via role=img + aria-label so a
    // screen reader announces "Detected on disk" / "Marked done manually" /
    // "Waiting…" for each gate (reusing the wizard's status strings). Branch on
    // overridden first so a manually-asserted gate isn't mis-announced as
    // FS-detected. Mirrors the labelled wizard rows (wizard.js status branch).
    li.appendChild(el('span', {
      class: 'setup-check-indicator',
      text: done ? '✓' : '○',
      attrs: {
        role: 'img',
        'aria-label': g.overridden
          ? t('wizard.status.overridden')
          : (done ? t('wizard.status.detected') : t('wizard.status.waiting'))
      }
    }));
    li.appendChild(document.createTextNode(' ' + t('setup.check.' + checkKey)));
    // Escape-hatch: clear an active override, or manually mark a currently-failing
    // gate. A truly FS-detected gate needs no toggle.
    if (g.overridden) {
      li.appendChild(el('span', { class: 'setup-check-override', text: ' · ' + t('setup.check.overridden') }));
      var clearBtn = el('button', { type: 'button', class: 'setup-check-override-btn', text: t('setup.check.clearOverride') });
      clearBtn.addEventListener('click', function () { userSaveManualStep('setup:' + checkKey, false); });
      li.appendChild(clearBtn);
    } else if (!g.fsDone) {
      var markBtn = el('button', { type: 'button', class: 'setup-check-override-btn', text: t('setup.check.markDone') });
      markBtn.addEventListener('click', function () { userSaveManualStep('setup:' + checkKey, true); });
      li.appendChild(markBtn);
    }
    return li;
  }

  // Collapsed-by-default manual-install reference. The bootstrap prompt (step 1)
  // already installs the skills; this exposes the by-hand alternatives.
  function buildInstallSection() {
    var details = el('details', { class: 'setup-install-disclosure' });
    details.appendChild(el('summary', { text: t('setup.installHeading') }));
    details.appendChild(el('p', { class: 'panel-lead', text: t('setup.installLead') }));

    details.appendChild(el('h4', { class: 'agents-install-subtitle', text: t('setup.installSub.cc') }));
    details.appendChild(codeBlock(INSTALL_CLAUDE_CODE).wrap);

    details.appendChild(el('h4', { class: 'agents-install-subtitle', text: t('setup.installSub.sym') }));
    details.appendChild(codeBlock(INSTALL_SYMLINK).wrap);

    details.appendChild(el('h4', { class: 'agents-install-subtitle', text: t('setup.installSub.cp') }));
    details.appendChild(codeBlock(INSTALL_COPY).wrap);

    var verifyWrap = el('div', { class: 'agents-verify' });
    verifyWrap.appendChild(el('p', { class: 'panel-lead', text: t('setup.verifyLead') }));
    verifyWrap.appendChild(codeBlock('ls .claude/skills/').wrap);
    verifyWrap.appendChild(el('p', { class: 'panel-lead', text: t('setup.verifyExpected') }));
    details.appendChild(verifyWrap);
    return details;
  }

  function buildStepCard(num, titleKey, bodyKey, actionEl, statusKeys, checks) {
    var card = el('div', { class: 'setup-step' });
    card.appendChild(el('div', { class: 'setup-step-num', text: String(num) }));
    var body = el('div', { class: 'setup-step-body' });
    body.appendChild(el('h4', { class: 'setup-step-title', text: t(titleKey) }));
    if (bodyKey) body.appendChild(el('p', { class: 'setup-step-text', text: t(bodyKey) }));
    if (actionEl) body.appendChild(actionEl);
    if (statusKeys && statusKeys.length) {
      // Polite live region so ○→✓ gate flips (driven by the FS watcher, not a
      // user action) are announced to screen readers.
      var ul = el('ul', { class: 'setup-checklist', attrs: { role: 'status', 'aria-live': 'polite' } });
      for (var i = 0; i < statusKeys.length; i++) {
        ul.appendChild(buildStatusItem(statusKeys[i], checks[statusKeys[i]] === true));
      }
      body.appendChild(ul);
    }
    card.appendChild(body);
    return card;
  }

  function cliReady() {
    var c = store.get().cli;
    // !authProblem: a dead (expired/revoked) token means sessions would 401
    // even though loggedIn reads true — the ▶ Run path is not actually usable.
    return !!(c && c.installed && c.loggedIn && !c.authProblem);
  }

  // The ▶ Run button for the "setup" session. getPrompt reads the LATEST form
  // snapshot so the bootstrap prompt never goes stale. Shared by render() and
  // the surgical updateCliRunButton() path so the two never drift.
  function makeSetupRunButton() {
    return runControl.button({
      key: 'setup',
      getPrompt: function () {
        var form3 = sectionEl.querySelector('.setup-form');
        var snap = form3 ? readFormSetup(form3) : currentSetup();
        return buildSetupClaudePrompt(snap);
      }
    });
  }

  function render() {
    if (!sectionEl) return;
    var setup    = currentSetup();
    var reviewerValue = reviewerValueForForm();
    setup.codexEnabled = reviewerValue === null ? '<choose-reviewer-mode>' : reviewerValue;
    var checks   = currentChecks();
    var prog     = store.get().progress || {};
    var setupDone = prog.setupDone === true;
    var errors   = validate(setup);

    while (sectionEl.firstChild) sectionEl.removeChild(sectionEl.firstChild);

    if (setupDone) {
      sectionEl.appendChild(el('div', {
        class: 'banner banner--info',
        text: t('setup.bannerDone')
      }));
    }

    sectionEl.appendChild(el('h2', { class: 'panel-title' }, [
      document.createTextNode(t('setup.title') + ' '),
      el('span', { class: 'save-indicator', text: t('setup.savedIndicator'), attrs: { 'aria-live': 'polite' } })
    ]));
    var showSetupIntro = isFreshSetup(setup, setupDone) && !setupIntroDismissed;
    sectionEl.appendChild(el('p', {
      class: 'panel-lead',
      text: t(showSetupIntro ? 'setup.intro.lead' : 'setup.lead')
    }));

    if (showSetupIntro) {
      renderFirstSetup();
      return;
    }

    // Form — four focused sections. The card headers explain why each group
    // matters, while the two-column field grid keeps related values scannable.
    var form = el('form', { class: 'setup-form', attrs: { novalidate: '', autocomplete: 'off' } });
    form.addEventListener('submit', function (e) { e.preventDefault(); });

    var card1 = buildCard(1, 'setup.cardProject', 'setup.cardProjectDescription');
    appendField(card1, 'productName',   setup, errors);
    appendField(card1, 'orgName',       setup, errors);
    appendField(card1, 'backendHost',   setup, errors);
    appendField(card1, 'applicationId', setup, errors);
    form.appendChild(card1);

    var card2 = buildCard(2, 'setup.cardPlatform', 'setup.cardPlatformDescription');
    appendField(card2, 'iosFrameworkName', setup, errors);
    card2.appendChild(buildTogglesGroup(setup));
    form.appendChild(card2);

    var card3 = buildCard(3, 'setup.cardResources', 'setup.cardResourcesDescription');
    appendField(card3, 'typefaceFactory', setup, errors);
    card3.appendChild(buildLocalesGroup(setup));
    appendField(card3, 'firstDomain', setup, errors);
    card3.appendChild(buildAuthGroup(setup));
    form.appendChild(card3);

    var card4 = buildCard(4, 'setup.cardReview', 'setup.cardReviewDescription');
    card4.appendChild(buildCodexGroup());
    card4.appendChild(buildScreenshotPixelGroup(setup));
    form.appendChild(card4);

    sectionEl.appendChild(form);

    // Collapsed YAML preview — diagnostic, hidden by default.
    var details = el('details', { class: 'setup-yaml-disclosure' });
    details.appendChild(el('summary', { text: t('setup.showYaml') }));
    var yaml = codeBlock(helpers.buildYaml(setup));
    yaml.code.id = 'setup-yaml';
    details.appendChild(yaml.wrap);
    sectionEl.appendChild(details);

    // "Next steps" — 3 numbered cards + final CTA.
    sectionEl.appendChild(el('h3', { class: 'panel-section-title setup-next-heading', text: t('setup.nextStepsHeading') }));

    var stepsWrap = el('div', { class: 'setup-steps' });

    // Single step — Claude verifies orchestrator/ is in place, writes the
    // config, and installs the skills (all inside the same prompt).
    // The three status rows below the button are live read-outs of what
    // the server has observed on disk; the user does NOT tick them — the
    // file watcher flips them as soon as Claude lands the changes.
    lastCliReady = cliReady();
    var promptBtn = el('button', {
      type: 'button',
      class: 'step-copy-btn' + (lastCliReady ? ' step-copy-btn--icon' : ''),
      text: lastCliReady ? '⧉' : t('setup.copyPromptBtn'),
      data: { 'bootstrap-prompt': '1' },
      attrs: {
        'title': t('setup.copyPromptBtn'),
        'aria-label': t('setup.copyPromptAria')
      }
    });
    clipboard.attach(promptBtn, function () {
      var form3 = sectionEl.querySelector('.setup-form');
      var snap = form3 ? readFormSetup(form3) : currentSetup();
      return buildSetupClaudePrompt(snap);
    }, { keepLabel: lastCliReady });

    // Run → Terminal — bootstrap through the shared "setup" session (this is
    // turn 1 of the very session the wizard steps reuse, so context carries
    // from bootstrap into the build steps). Shown only when the CLI is ready;
    // otherwise Copy-prompt remains the path.
    var setupActions = el('div', { class: 'setup-step-actions' });
    if (lastCliReady) {
      setupActions.appendChild(makeSetupRunButton());
    }
    setupActions.appendChild(promptBtn);

    stepsWrap.appendChild(buildStepCard(
      1,
      'setup.step1.title',
      'setup.step1.body',
      setupActions,
      ['requirementsVerified', 'yamlPasted', 'agentsInstalled'],
      checks
    ));

    sectionEl.appendChild(stepsWrap);
    sectionEl.appendChild(buildInstallSection());

    // Final CTA — `setupDone` is derived (the three status rows above
    // are the gate). Clicking just navigates; the server already knows
    // the setup is complete, no progress write needed here.
    var action = el('div', { class: 'setup-action' });
    var primary = el('button', {
      type: 'button',
      class: 'btn btn--primary setup-continue-btn',
      text: t('setup.primaryBtn'),
      data: { action: 'mark-complete' }
    });
    primary.addEventListener('click', function () {
      if (primary.disabled) return;
      var form2 = sectionEl.querySelector('.setup-form');
      var snap = form2 ? readFormSetup(form2) : currentSetup();
      // Flush any not-yet-debounced form edits before navigating away.
      userSaveSetup(snap);
      router.go('wizard');
    });
    action.appendChild(primary);
    // Visible pointer for the disabled CTA. The disabled-button title tooltip is
    // unreachable (no hover/focus on a disabled control), so when the Continue
    // gate isn't met this connective line ties the greyed button to the step
    // above. updatePrimaryState() toggles its hidden state alongside the button;
    // it deliberately does NOT restate collectProblems — the checklist already
    // lists those.
    var continueHint = el('small', {
      class: 'field-help setup-continue-hint',
      text: t('setup.continueHint')
    });
    continueHint.hidden = true;
    action.appendChild(continueHint);
    sectionEl.appendChild(action);

    wireForm(form);
    updatePrimaryState(setup, errors);
    updateBootstrapButtonsState(errors);
  }

  // Saved-indicator: fades in the .save-indicator span next to the panel
  // title for ~1.5s whenever any store write happens while the user is on
  // this panel. Subscribed once per mount; subsequent renders don't re-wire.
  var saveIndicatorTimer = null;
  function pulseSaveIndicator() {
    if (!sectionEl) return;
    if (router && typeof router.current === 'function' &&
        router.current() !== 'setup') return;
    var ind = sectionEl.querySelector('.save-indicator');
    if (!ind) return;
    ind.classList.add('save-indicator--visible');
    if (saveIndicatorTimer) clearTimeout(saveIndicatorTimer);
    saveIndicatorTimer = setTimeout(function () {
      ind.classList.remove('save-indicator--visible');
    }, 1500);
  }

  // Surgical update for live state changes — replaces the three status
  // rows and recomputes the Continue-button gate WITHOUT rebuilding the
  // form. A full re-render here would yank focus and clear the user's
  // in-progress typing every time the debounced save fires.
  function updateLiveStatus() {
    if (!sectionEl) return;
    var checks = currentChecks();
    var checklist = sectionEl.querySelector('.setup-checklist');
    if (checklist) {
      while (checklist.firstChild) checklist.removeChild(checklist.firstChild);
      var keys = ['requirementsVerified', 'yamlPasted', 'agentsInstalled'];
      for (var i = 0; i < keys.length; i++) {
        checklist.appendChild(buildStatusItem(keys[i], checks[keys[i]] === true));
      }
    }
    var form = sectionEl.querySelector('.setup-form');
    var reviewerControls = form && form.querySelectorAll('[data-reviewer-mode]');
    var reviewerSaving = form && !!form.querySelector('[data-reviewer-saving="1"]');
    if (reviewerControls && !reviewerSaving) {
      var reviewerConfig = store.get().reviewerConfig || {};
      for (var i = 0; i < reviewerControls.length; i++) {
        reviewerControls[i].disabled = reviewerConfig.canUpdate !== true;
      }
      syncReviewerControls(reviewerControls, reviewerValueForForm());
      var reviewerFeedback = form.querySelector('[data-reviewer-feedback]');
      if (reviewerFeedback) {
        reviewerFeedback.textContent = reviewerConfig.state === 'ready' ? ''
          : reviewerConfig.state === 'missing'
            ? t(reviewerConfig.canUpdate === true
              ? 'setup.reviewerAddMissing' : 'setup.reviewerBootstrapDefault')
            : t('setup.reviewerInvalid');
      }
    }
    var snap = form ? readFormSetup(form) : currentSetup();
    var yamlEl = sectionEl.querySelector('#setup-yaml');
    if (yamlEl) yamlEl.textContent = helpers.buildYaml(snap);
    updatePrimaryState(snap, validate(snap));
  }

  // Surgically add/remove ONLY the ▶ Run button when CLI readiness flips
  // (header install/login). Doing this without a full render() preserves the
  // user's focus and any keystrokes not yet flushed by debouncedSaveSetup —
  // a full rebuild would reseed the form from the last server snapshot and
  // drop in-progress typing.
  function updateCliRunButton() {
    if (!sectionEl) return false;
    var actions = sectionEl.querySelector('.setup-step-actions');
    if (!actions) return false;
    var ready = cliReady();
    var existing = actions.querySelector('[data-run-control="setup"]');
    if (ready && !existing) {
      // Insert before the Copy-prompt button so order matches render().
      actions.insertBefore(makeSetupRunButton(), actions.firstChild);
      // A freshly-added Run button must inherit the current form-validity gate.
      var form = sectionEl.querySelector('.setup-form');
      updateBootstrapButtonsState(validate(form ? readFormSetup(form) : currentSetup()));
    } else if (!ready && existing) {
      actions.removeChild(existing);
    }
    lastCliReady = ready;
  }

  function onStoreChange() {
    if (router && typeof router.current === 'function' &&
        router.current() === 'setup') {
      // A CLI-readiness flip (header install/login) adds or removes the ▶ Run
      // affordance. Handle it surgically (just that button) so the form's focus
      // and not-yet-debounced keystrokes survive — a full render() would reseed
      // the form from the last server snapshot and lose in-progress typing.
      if (cliReady() !== lastCliReady) updateCliRunButton();
      updateLiveStatus();
    }
  }

  var _unsub = null;

  export const setup = {
    mount: function (rootEl) {
      sectionEl = rootEl;
      render();
      _unsub = store.on('change', onStoreChange);
    },
    unmount: function () {
      if (_unsub) { _unsub(); _unsub = null; }
    },
    refresh: function () {
      // A language flip rebuilds the form from the store snapshot; flush any
      // debounced edit first so the last <200ms of typing isn't discarded.
      debouncedSaveSetup.flush();
      render();
    }
  };
