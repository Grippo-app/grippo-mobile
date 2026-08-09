import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { store } from '../store.js';
import { clipboard } from '../clipboard.js';
import { templates } from '../templates.js';
import { helpers, wizardSteps } from '../data/wizard-steps.js';
import { router } from '../router.js';
import { runControl } from '../run-control.js';
import { autoRun } from '../auto-run.js';
import { runErrorMessage } from '../run-errors.js';

  var sectionEl = null;
  var wizardIntroDismissed = false;
  var WIZARD_GROUPS = [
    { id: 'foundation', through: 5, titleKey: 'wizard.group.foundation.title', bodyKey: 'wizard.group.foundation.body' },
    { id: 'product',    through: 8, titleKey: 'wizard.group.product.title',    bodyKey: 'wizard.group.product.body' },
    { id: 'apps',       through: 11, titleKey: 'wizard.group.apps.title',       bodyKey: 'wizard.group.apps.body' },
    { id: 'finish',     through: Infinity, titleKey: 'wizard.group.finish.title', bodyKey: 'wizard.group.finish.body' }
  ];

  // ----------------------------------------------------------------------
  // DOM helper — el() lives in scripts/dom.js (App.dom.el).
  // ----------------------------------------------------------------------

  var el = dom.el;

  function t(key, params) {
    if (i18n && typeof i18n.t === 'function') {
      return i18n.t(key, params);
    }
    return key;
  }

  function codeBlock(text) {
    var wrap = el('div', { class: 'code-block-wrapper' });
    var pre = el('pre', { class: 'code-block' });
    var code = el('code', { text: text });
    pre.appendChild(code);
    wrap.appendChild(pre);
    var btn = el('button', {
      type: 'button',
      class: 'copy-btn',
      text: t('common.copy'),
      attrs: { 'aria-label': t('common.copyAria') }
    });
    clipboard.attach(btn, function () { return code.textContent; });
    wrap.appendChild(btn);
    return wrap;
  }

  // ----------------------------------------------------------------------
  // Step-list helpers.
  // ----------------------------------------------------------------------

  function autoSkippedIds(setup) {
    var ids = [];
    var steps = wizardSteps;
    for (var i = 0; i < steps.length; i++) {
      if (helpers.isAutoSkipped(steps[i], setup)) ids.push(steps[i].id);
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

  function hasWizardProgress(state) {
    var done = state.progress && Array.isArray(state.progress.wizardStepsDone)
      ? state.progress.wizardStepsDone : [];
    // Step 12 is a read-only end-to-end integrity check. In a deliberately
    // empty project it can already be green before the user has launched any
    // build step, so it must not dismiss the first-run introduction by itself.
    return done.some(function (id) { return String(id) !== '12'; });
  }

  // Single source: helpers.stepPrompt renders body + the unattended-run
  // footer, so the displayed block, the Copy payload, and the Run prompt are
  // identical to what auto-run sends.
  function renderStepBody(step, setup) {
    return helpers.stepPrompt(step, setup);
  }

  function wizardGroupForStep(step) {
    var numericId = Number(step.id);
    for (var i = 0; i < WIZARD_GROUPS.length; i++) {
      if (numericId <= WIZARD_GROUPS[i].through) return WIZARD_GROUPS[i];
    }
    return WIZARD_GROUPS[WIZARD_GROUPS.length - 1];
  }

  function buildWizardGroup(group, count) {
    var wrap = el('section', {
      class: 'wizard-group',
      data: { 'wizard-group': group.id }
    });
    wrap.appendChild(el('header', { class: 'wizard-group-head' }, [
      el('div', { class: 'wizard-group-copy' }, [
        el('h3', { class: 'wizard-group-title', text: t(group.titleKey) }),
        el('p', { class: 'wizard-group-description', text: t(group.bodyKey) })
      ]),
      el('span', { class: 'wizard-group-count', text: t('wizard.group.count', { count: count }) })
    ]));
    wrap.appendChild(el('div', { class: 'wizard-group-steps' }));
    return wrap;
  }

  // True when the CLI is installed + signed in with a LIVE token (Step 1) —
  // only then can a step auto-run. loggedIn alone lies when the stored token
  // is expired/revoked (cli.authProblem): sessions would fail with 401, so a
  // dead token counts as not-ready. Otherwise the copy-paste path remains.
  function cliReady() {
    var s = store.get();
    return !!(s && s.cli && s.cli.installed && s.cli.loggedIn && !s.cli.authProblem);
  }

  // ----------------------------------------------------------------------
  // Auto-run control — the one button that runs every remaining step in
  // order through the shared "setup" session (engine: scripts/auto-run.js).
  // Lives in the wizard header next to the progress bar. Rendered only when
  // the CLI is ready (same gate as the per-step ▶ Run, and part of gateKey so
  // it appears/disappears via a full re-render). The empty-form case disables
  // it with a hint, mirroring the per-step buttons.
  // ----------------------------------------------------------------------

  function autoRunStatusText(s) {
    var id = s.stepId || '';
    switch (s.status) {
      case 'running':        return t('wizard.auto.running', { id: id });
      case 'awaiting-input': return t('wizard.auto.awaitingInput', { id: id });
      case 'verifying':      return t('wizard.auto.verifying', { id: id });
      case 'paused':         return s.reason === 'unverified'
                                ? t('wizard.auto.pausedUnverified', { id: id })
                                : s.reason === 'busy'
                                ? t('wizard.auto.pausedBusy', { id: id })
                                // The per-step ▶ Run button already renders these
                                // typed refusals; auto-run was the only Run path
                                // that flattened them into "the session stopped".
                                : s.errorCode
                                ? t('wizard.auto.pausedReason', { id: id, detail: runErrorMessage({ kind: s.errorCode }) })
                                : t('wizard.auto.pausedStopped', { id: id });
      case 'done':           return t('wizard.auto.done');
      default:               return t('wizard.auto.idleHint');
    }
  }

  function paintAutoRunControl(wrap, setup) {
    if (!wrap) return;
    var btn = wrap.querySelector('.wizard-auto-btn');
    var statusEl = wrap.querySelector('.wizard-auto-status');
    var s = autoRun.getState();
    var active = autoRun.isActive();
    var formEmpty = !(setup && setup.productName);

    if (active) {
      btn.textContent = t('wizard.auto.btnStop');
      btn.setAttribute('data-auto-state', 'active');
      btn.disabled = false;
      btn.removeAttribute('title');
    } else if (s.status === 'paused') {
      btn.textContent = t('wizard.auto.btnResume');
      btn.setAttribute('data-auto-state', 'paused');
      btn.disabled = formEmpty;
      if (formEmpty) btn.setAttribute('title', t('wizard.emptyForm')); else btn.removeAttribute('title');
    } else {
      btn.textContent = t('wizard.auto.btnRunAll');
      btn.setAttribute('data-auto-state', 'idle');
      // Nothing left → disable with an "all done" hint (kept distinct from the
      // empty-form hint so the reason is unambiguous).
      var none = autoRun.remainingCount() === 0;
      btn.disabled = formEmpty || none;
      if (formEmpty) btn.setAttribute('title', t('wizard.emptyForm'));
      else if (none) btn.setAttribute('title', t('wizard.auto.allDone'));
      else btn.removeAttribute('title');
    }

    // Fresh-load with nothing left to run reads better as "all done" than the
    // generic idle hint (which talks about running steps that don't exist).
    var statusText = (s.status === 'idle' && autoRun.remainingCount() === 0)
      ? t('wizard.auto.allDone')
      : autoRunStatusText(s);
    statusEl.textContent = statusText;
    statusEl.setAttribute('data-auto-status', s.status + (s.reason ? ':' + s.reason : ''));
  }

  function buildAutoRunControl(setup) {
    var wrap = el('div', { class: 'wizard-auto', data: { 'auto-run': '1' } });
    var btn = el('button', { type: 'button', class: 'btn btn--primary wizard-auto-btn' });
    // One stable click handler that dispatches on the LIVE state — active
    // (running/verifying/awaiting-input) stops; otherwise start/resume.
    btn.addEventListener('click', function (e) {
      e.preventDefault();
      if (autoRun.isActive()) autoRun.stop(); else autoRun.start();
    });
    wrap.appendChild(btn);
    wrap.appendChild(el('span', { class: 'wizard-auto-status', attrs: { 'aria-live': 'polite' } }));
    paintAutoRunControl(wrap, setup);
    return wrap;
  }

  function updateAutoRunControl() {
    if (!sectionEl) return;
    var wrap = sectionEl.querySelector('[data-auto-run]');
    if (!wrap) return;
    paintAutoRunControl(wrap, (store.get() && store.get().setup) || {});
  }

  // ----------------------------------------------------------------------
  // Event handling.
  // ----------------------------------------------------------------------

  // Wired to the checkbox on dialog-only ("manual" kind) steps. FS-kind
  // steps have a separate "Mark done" button that calls saveManualStep
  // directly with the inverse of the current override flag — see the
  // FS branch of renderStep. Either way, the full re-render is deferred
  // to the store's 'change' event so the optimistic UI stays in sync
  // with whatever the server confirms.
  function handleManualStepChange(stepId, checked) {
    store.saveManualStep(stepId, checked === true);
  }

  // ----------------------------------------------------------------------
  // Renderers.
  // ----------------------------------------------------------------------

  function renderGate() {
    var titleId = 'wizard-setup-required-title';
    var action = el('button', {
      type: 'button',
      class: 'btn btn--primary first-run-card__action',
      text: t('wizard.gate.action')
    });
    action.addEventListener('click', function () { router.go('setup'); });
    sectionEl.appendChild(el('section', {
      class: 'first-run-card first-run-card--wizard-gate',
      attrs: { 'aria-labelledby': titleId }
    }, [
      el('div', { class: 'first-run-card__main' }, [
        el('span', { class: 'first-run-card__eyebrow', text: t('wizard.gate.eyebrow') }),
        el('h3', { id: titleId, class: 'panel-section-title first-run-card__title', text: t('wizard.gate.title') }),
        el('p', { class: 'first-run-card__body', text: t('wizard.gate.body') }),
        action
      ]),
      el('ol', { class: 'first-run-card__steps', attrs: { 'aria-label': t('wizard.gate.stepsLabel') } }, [
        el('li', { text: t('wizard.gate.stepConfig') }),
        el('li', { text: t('wizard.gate.stepBootstrap') }),
        el('li', { text: t('wizard.gate.stepReturn') })
      ])
    ]));
  }

  function renderWizardIntro() {
    var titleId = 'wizard-first-run-title';
    var action = el('button', {
      type: 'button',
      class: 'btn btn--primary first-run-card__action',
      text: t('wizard.intro.action')
    });
    action.addEventListener('click', function () {
      wizardIntroDismissed = true;
      render();
      setTimeout(function () {
        var firstStep = sectionEl && sectionEl.querySelector('.wizard-step > summary');
        if (firstStep) firstStep.focus();
      }, 0);
    });
    sectionEl.appendChild(el('section', {
      class: 'first-run-card first-run-card--wizard',
      attrs: { 'aria-labelledby': titleId }
    }, [
      el('div', { class: 'first-run-card__main' }, [
        el('span', { class: 'first-run-card__eyebrow', text: t('wizard.intro.eyebrow') }),
        el('h3', { id: titleId, class: 'panel-section-title first-run-card__title', text: t('wizard.intro.title') }),
        el('p', { class: 'first-run-card__body', text: t('wizard.intro.body') }),
        action
      ]),
      el('ol', { class: 'first-run-card__steps', attrs: { 'aria-label': t('wizard.intro.stepsLabel') } }, [
        el('li', { text: t('wizard.intro.stepRun') }),
        el('li', { text: t('wizard.intro.stepPause') }),
        el('li', { text: t('wizard.intro.stepDetect') })
      ])
    ]));
  }

  // setupDone is derived from the filesystem (git-trackable config + agents),
  // but the form values live only in the gitignored .cache/site/.site-state.json. A
  // teammate cloning a bootstrapped repo can get setupDone=true with an empty
  // cache. The server backfills the form from committed project-config.md in
  // that case (server/project-config.js), so this
  // warning fires only when BOTH the persisted form AND the config are
  // empty/placeholder — i.e. nothing on disk can fill the <Product>
  // placeholders. productName's absence remains the reliable signal that the
  // form must be filled before the Copy / Run prompts are usable.
  function renderEmptyFormWarning() {
    var banner = el('div', { class: 'banner banner--warn' });
    banner.appendChild(document.createTextNode(t('wizard.emptyForm') + ' '));
    banner.appendChild(el('a', { href: '#setup', text: t('common.openSetup') }));
    sectionEl.appendChild(banner);
  }

  function renderStep(step, setup, stepStatus, doneSet, prevDone) {
    var skipped = helpers.isAutoSkipped(step, setup);
    var status = stepStatus[step.id] || { kind: 'fs', done: false };
    // Treat auto-skipped steps the same way as manual-confirmed: there's
    // nothing for the watcher to detect, so don't render an indicator
    // that would only ever sit at ✗.
    var kind = skipped ? 'skipped' : status.kind;
    var done = doneSet[step.id] === true;
    // Visual "done" excludes skipped — skipped has its own treatment
    // (muted opacity + skip tag) and we don't want it to also turn green.
    var visuallyDone = done && !skipped;

    var details = el('details', {
      class: 'wizard-step',
      data: {
        'step-id': step.id,
        skipped: skipped ? 'true' : 'false',
        kind: kind,
        done: visuallyDone ? 'true' : 'false'
      }
    });

    // Summary row.
    var summary = el('summary');
    summary.appendChild(el('span', { class: 'step-badge', text: step.id }));
    summary.appendChild(el('span', { class: 'step-title', text: step.title }));
    if (visuallyDone) {
      summary.appendChild(el('span', {
        class: 'step-done-mark',
        text: '✓',
        attrs: { 'aria-label': t('wizard.tagDone') }
      }));
    }
    if (skipped) {
      summary.appendChild(el('span', { class: 'step-skip-tag', text: t('wizard.tagSkipped') }));
    } else {
      // Empty form (cloned repo — see renderEmptyFormWarning) collapses every
      // <Product> placeholder to "", so the Copy/Run prompts are degenerate.
      // Gate both on the same productName signal the banner uses; gateKey
      // re-renders the panel when productName presence flips, so the buttons
      // re-enable automatically once the Setup form is refilled.
      var formEmpty = !setup.productName;
      var runAvailable = cliReady();

      // Copy is the fallback beside the headline Run action. When Run is
      // available, mirror the Board modal's quiet icon-only copy affordance;
      // when the CLI is unavailable, retain the text label because Copy is the
      // only usable path. Stops the click from toggling <details>.
      var copyBtn = el('button', {
        type: 'button',
        class: 'step-copy-btn' + (runAvailable ? ' step-copy-btn--icon' : ''),
        text: runAvailable ? '⧉' : t('wizard.btnCopyPrompt'),
        attrs: {
          'title': t('wizard.btnCopyPrompt'),
          'aria-label': t('wizard.btnCopyPrompt')
        }
      });
      clipboard.attach(copyBtn, function () { return renderStepBody(step, setup); }, {
        keepLabel: runAvailable
      });
      copyBtn.addEventListener('click', function (e) {
        e.preventDefault();
        e.stopPropagation();
      });
      if (formEmpty) {
        copyBtn.disabled = true;
        copyBtn.setAttribute('title', t('wizard.emptyForm'));
      }
      summary.appendChild(copyBtn);

      // ▶ Run → ⊡ Terminal — run this step through the shared "setup" session
      // (Step 2). Shown only when the CLI is ready; otherwise the copy-paste
      // path remains. The control flips to Terminal while a turn is in flight
      // and reopens the same console, so a closed terminal is never lost; when
      // idle it returns to Run, ready to send the next step into the same
      // (context-carrying) session.
      if (runAvailable) {
        var runBtn = runControl.button({
          key: 'setup',
          // Latest setup values, not those captured at render time (the user
          // may have edited the form since), so the prompt is never stale.
          getPrompt: function () {
            var freshSetup = (store.get() && store.get().setup) || setup;
            return renderStepBody(step, freshSetup);
          },
          labels: { run: t('wizard.btnRunStep') },
          extraClass: 'step-run-btn',
          // The gate is re-evaluated from the live snapshot on every store tick.
          // A one-shot `disabled = true` here could never be lifted: this panel
          // does not rebuild step cards on a state change, so filling the form in
          // another tab left every ▶ Run dead until the user re-entered the panel.
          isDisabled: function (snapshot) {
            var empty = !(snapshot && snapshot.setup && snapshot.setup.productName);
            // Keep the reason on the control only while it is actually gated —
            // a tooltip explaining a block that no longer exists is its own lie.
            if (empty) runBtn.setAttribute('title', t('wizard.emptyForm'));
            else runBtn.removeAttribute('title');
            return empty;
          }
        });
        if (formEmpty) runBtn.setAttribute('title', t('wizard.emptyForm'));
        summary.appendChild(runBtn);
      }
    }
    details.appendChild(summary);

    // Body.
    var body = el('div', { class: 'step-body' });
    body.appendChild(el('p', { class: 'step-hook', text: step.hook }));

    body.appendChild(el('h4', { class: 'step-subtitle', text: t('wizard.subPrompt') }));
    if (skipped) {
      var reason = step.skipReasonKey
        ? t(step.skipReasonKey)
        : t('wizard.skipReasonDefault');
      body.appendChild(el('p', {
        class: 'panel-lead',
        text: t('wizard.skippedBody', { reason: reason })
      }));
    } else {
      var promptText = renderStepBody(step, setup);
      body.appendChild(codeBlock(promptText));
    }

    body.appendChild(el('h4', { class: 'step-subtitle', text: t('wizard.subVerify') }));
    if (skipped) {
      body.appendChild(el('p', { class: 'panel-lead', text: t('wizard.verifySkipped') }));
    } else {
      var hint = templates.render(step.verifyHint || '', setup);
      body.appendChild(codeBlock(hint));
    }

    // Status row — content depends on whether the step is FS-derived,
    // dialog-only (manual), or auto-skipped.
    var statusRow = el('div', { class: 'wizard-step-check wizard-step-status' });
    if (kind === 'skipped') {
      // Nothing actionable — the skip badge in the summary already says
      // it all. We do not render a status row to keep the body clean.
    } else if (kind === 'fs') {
      // Indicator flips automatically when the server's file watcher
      // sees the deliverable land. The "Mark done" button next to it is
      // an escape hatch for refactors/renames where the validator's
      // proxy path no longer matches: the user asserts the step is
      // done, the server stores manualSteps[id]=true, and done wins
      // over the FS check until the override is cleared.
      var overridden = status.overridden === true;
      statusRow.appendChild(el('span', {
        class: 'wizard-step-indicator' + (done ? ' wizard-step-indicator--done' : ' wizard-step-indicator--pending'),
        text: done ? '✓' : '○',
        attrs: { 'aria-hidden': 'true' }
      }));
      statusRow.appendChild(document.createTextNode(' '));
      statusRow.appendChild(el('span', {
        class: 'wizard-step-status-label',
        text: overridden
          ? t('wizard.status.overridden')
          : (done ? t('wizard.status.detected') : t('wizard.status.waiting'))
      }));
      var overrideBtn = el('button', {
        type: 'button',
        class: 'btn wizard-step-override-btn',
        text: overridden ? t('wizard.btnClearOverride') : t('wizard.btnMarkDone'),
        data: { 'step-override': step.id }
      });
      overrideBtn.addEventListener('click', function () {
        var snap = store.get();
        var st = (snap.progress && snap.progress.stepStatus &&
                  snap.progress.stepStatus[step.id]) || {};
        store.saveManualStep(step.id, !(st.overridden === true));
      });
      statusRow.appendChild(overrideBtn);
    } else {
      // Manual (dialog-only) step. The action is confirming with the
      // agent, so we show a checkbox the user ticks once the agent says
      // "confirmed". prevDone disables it so the user can't skip ahead.
      var cb = el('input', {
        type: 'checkbox',
        class: 'choice-input',
        id: 'step-' + step.id + '-done',
        data: { 'step-check': step.id }
      });
      cb.checked = done;
      cb.disabled = !prevDone;
      cb.addEventListener('change', function () {
        handleManualStepChange(step.id, cb.checked);
      });
      var lbl = el('label', { attrs: { 'for': 'step-' + step.id + '-done' } });
      lbl.appendChild(document.createTextNode(t('wizard.stepCheckLabel')));
      statusRow.appendChild(cb);
      statusRow.appendChild(lbl);
      // Disabled because the predecessor isn't done yet — explain why and
      // point at the "Mark done" override so the gate is discoverable.
      if (!prevDone) {
        var hintId = 'step-' + step.id + '-needs-prev';
        cb.setAttribute('title', t('wizard.step12NeedsPrev'));
        cb.setAttribute('aria-describedby', hintId);
        statusRow.appendChild(el('span', {
          class: 'wizard-step-needs-prev',
          text: t('wizard.step12NeedsPrev'),
          attrs: { id: hintId }
        }));
      }
    }
    if (statusRow.childNodes.length > 0) body.appendChild(statusRow);

    details.appendChild(body);
    return details;
  }

  function render() {
    if (!sectionEl) return;
    while (sectionEl.firstChild) sectionEl.removeChild(sectionEl.firstChild);

    var state = store.get();
    var setupDone = state.progress && state.progress.setupDone === true;

    sectionEl.appendChild(el('h2', { class: 'panel-title', text: t('wizard.title') }));

    if (!setupDone) {
      sectionEl.appendChild(el('p', { class: 'panel-lead', text: t('wizard.intro.lead') }));
      renderGate();
      return;
    }

    var setup = state.setup || {};
    var steps = wizardSteps;
    var doneSet = effectiveDoneSet(state);
    var stepStatus = (state.progress && state.progress.stepStatus) || {};

    if (setup.productName && !hasWizardProgress(state) && !wizardIntroDismissed) {
      sectionEl.appendChild(el('p', { class: 'panel-lead', text: t('wizard.intro.lead') }));
      renderWizardIntro();
      return;
    }

    // setupDone gate passed, but the form may still be empty (cloned repo —
    // see renderEmptyFormWarning). Warn before the steps so the user doesn't
    // copy/run prompts with blank substitutions.
    if (!setup.productName) renderEmptyFormWarning();

    // Header: progress label + bar. Each step card owns its copy action.
    var doneCount = 0;
    for (var d = 0; d < steps.length; d++) if (doneSet[steps[d].id]) doneCount++;
    var pct = steps.length === 0 ? 0 : Math.round((doneCount / steps.length) * 100);

    var header = el('div', { class: 'wizard-header' });

    var progressWrap = el('div', { class: 'wizard-progress-wrap' });
    progressWrap.appendChild(el('div', {
      class: 'wizard-progress',
      text: t('wizard.progress', { done: doneCount, total: steps.length })
    }));
    var bar = el('div', { class: 'wizard-progress-bar' });
    var fill = el('div', { class: 'wizard-progress-fill' });
    fill.style.width = pct + '%';
    bar.appendChild(fill);
    progressWrap.appendChild(bar);
    header.appendChild(progressWrap);

    // The "run every remaining step" control sits beside the progress bar.
    // Only when the CLI is ready (cliReady is in gateKey, so this whole header
    // is rebuilt when login state flips); the copy-paste path remains otherwise.
    if (cliReady()) header.appendChild(buildAutoRunControl(setup));

    sectionEl.appendChild(header);

    sectionEl.appendChild(el('p', {
      class: 'panel-lead',
      text: t('wizard.lead')
    }));

    // Stepper. The pipeline is split into four named phases, so the full build
    // reads as a short plan rather than one long list. All cards still render
    // collapsed; no auto-expand or auto-scroll keeps the viewport stable.
    var stepper = el('div', { class: 'wizard-stepper' });
    var groupCounts = {};
    for (var gc = 0; gc < steps.length; gc++) {
      var countedGroup = wizardGroupForStep(steps[gc]);
      groupCounts[countedGroup.id] = (groupCounts[countedGroup.id] || 0) + 1;
    }
    var activeGroupId = null;
    var activeGroupSteps = null;
    var prevDone = true; // Step 0 has no predecessor; treat as "previous done".
    for (var i = 0; i < steps.length; i++) {
      var step = steps[i];
      var group = wizardGroupForStep(step);
      if (group.id !== activeGroupId) {
        var groupEl = buildWizardGroup(group, groupCounts[group.id]);
        stepper.appendChild(groupEl);
        activeGroupId = group.id;
        activeGroupSteps = groupEl.querySelector('.wizard-group-steps');
      }
      activeGroupSteps.appendChild(renderStep(step, setup, stepStatus, doneSet, prevDone));
      prevDone = doneSet[step.id] === true;
    }
    sectionEl.appendChild(stepper);

    // Closing card.
    if (helpers.wizardComplete(state)) {
      var card = el('div', { class: 'card wizard-complete-card' });
      card.appendChild(el('strong', { text: t('wizard.completeTitle') }));
      card.appendChild(el('p', {
        text: t('wizard.completeBody')
      }));
      card.appendChild(el('div', {}, [
        el('a', { class: 'btn btn--primary', href: '#board', text: t('common.openBoard') })
      ]));
      sectionEl.appendChild(card);
    }
  }

  // render() rebuilds the stepper from scratch, so every <details open> the
  // user expanded collapses. Snapshot the open step-ids before a full
  // re-render and re-apply them after so a language toggle or a CLI-login
  // gateKey flip doesn't lose the user's place.
  function captureOpenStepIds() {
    var open = {};
    if (!sectionEl) return open;
    var nodes = sectionEl.querySelectorAll('.wizard-step[open]');
    for (var i = 0; i < nodes.length; i++) {
      var id = nodes[i].getAttribute('data-step-id');
      if (id) open[id] = true;
    }
    return open;
  }

  function restoreOpenStepIds(open) {
    if (!sectionEl || !open) return;
    for (var id in open) {
      if (!open.hasOwnProperty(id)) continue;
      var node = sectionEl.querySelector('.wizard-step[data-step-id="' + id + '"]');
      if (node) node.open = true;
    }
  }

  // render() that preserves which step cards were expanded.
  function renderPreservingOpen() {
    var open = captureOpenStepIds();
    render();
    restoreOpenStepIds(open);
  }

  // Surgical patch — flips per-step indicators / checkboxes / progress bar
  // without rebuilding the stepper. Avoids closing <details open> that the
  // user had expanded and avoids stealing focus from an in-flight click.
  // A full re-render is only triggered when a gate threshold flips
  // (setupDone or wizardComplete), since those swap the panel layout.
  var lastGateKey = null;
  function gateKey(state) {
    // cliReady is included so logging into the CLI (Step 1) triggers a full
    // re-render and the ▶ Run buttons appear without a manual page refresh.
    // productName presence is included so the empty-form warning banner is
    // added/removed via a full re-render once the user refills (or clears)
    // the Setup form — the surgical path doesn't touch the banner.
    return [
      state.progress && state.progress.setupDone === true ? '1' : '0',
      helpers.wizardComplete(state) ? '1' : '0',
      hasWizardProgress(state) ? '1' : '0',
      (state.cli && state.cli.installed && state.cli.loggedIn && !state.cli.authProblem) ? '1' : '0',
      (state.setup && state.setup.productName) ? '1' : '0'
    ].join(':');
  }

  function updateLiveStatus() {
    if (!sectionEl) return;
    var state = store.get();
    var key = gateKey(state);
    if (key !== lastGateKey) {
      lastGateKey = key;
      renderPreservingOpen();
      return;
    }

    var setup = state.setup || {};
    var steps = wizardSteps;
    var doneSet = effectiveDoneSet(state);
    var stepStatus = (state.progress && state.progress.stepStatus) || {};

    var prevDone = true; // Step 0 has no predecessor.
    for (var i = 0; i < steps.length; i++) {
      var step = steps[i];
      var stepEl = sectionEl.querySelector('[data-step-id="' + step.id + '"]');
      var skipped = helpers.isAutoSkipped(step, setup);
      var done = doneSet[step.id] === true;
      var kind = skipped ? 'skipped' : ((stepStatus[step.id] && stepStatus[step.id].kind) || 'fs');

      if (stepEl) {
        var visuallyDone = done && !skipped;
        stepEl.setAttribute('data-done', visuallyDone ? 'true' : 'false');

        var summary = stepEl.querySelector('summary');
        // Done checkmark in <summary> — inserted after .step-title so
        // it sits inline with the title rather than at the row's end.
        var mark = summary && summary.querySelector('.step-done-mark');
        if (visuallyDone) {
          if (!mark) {
            mark = el('span', {
              class: 'step-done-mark',
              text: '✓',
              attrs: { 'aria-label': t('wizard.tagDone') }
            });
            var titleEl = summary.querySelector('.step-title');
            if (titleEl && titleEl.nextSibling) {
              summary.insertBefore(mark, titleEl.nextSibling);
            } else {
              summary.appendChild(mark);
            }
          }
        } else if (mark && mark.parentNode) {
          mark.parentNode.removeChild(mark);
        }

        if (kind === 'fs') {
          var fsStatus = stepStatus[step.id] || {};
          var overridden = fsStatus.overridden === true;
          var ind = stepEl.querySelector('.wizard-step-indicator');
          if (ind) {
            ind.textContent = done ? '✓' : '○';
            ind.className = 'wizard-step-indicator ' +
              (done ? 'wizard-step-indicator--done' : 'wizard-step-indicator--pending');
          }
          var lblEl = stepEl.querySelector('.wizard-step-status-label');
          if (lblEl) {
            lblEl.textContent = overridden
              ? t('wizard.status.overridden')
              : (done ? t('wizard.status.detected') : t('wizard.status.waiting'));
          }
          var overrideBtnEl = stepEl.querySelector('.wizard-step-override-btn');
          if (overrideBtnEl) {
            overrideBtnEl.textContent = overridden
              ? t('wizard.btnClearOverride')
              : t('wizard.btnMarkDone');
          }
        } else if (kind === 'manual') {
          var cb = stepEl.querySelector('input[type="checkbox"][data-step-check]');
          if (cb) {
            // Disabled tracks the previous-step gate; checked tracks the
            // server's confirmed value, but we leave the user's in-flight
            // click alone if their focus is on this very checkbox.
            // Don't flip disabled/checked out from under the user mid-click.
            if (document.activeElement !== cb) {
              cb.disabled = !prevDone;
              cb.checked = done;
              // Keep the "finish the previous step" hint in sync with the
              // gate so it doesn't linger after the predecessor lands.
              var hint = stepEl.querySelector('.wizard-step-needs-prev');
              if (!prevDone && !hint) {
                var hId = 'step-' + step.id + '-needs-prev';
                cb.setAttribute('title', t('wizard.step12NeedsPrev'));
                cb.setAttribute('aria-describedby', hId);
                stepEl.querySelector('.wizard-step-status').appendChild(el('span', {
                  class: 'wizard-step-needs-prev',
                  text: t('wizard.step12NeedsPrev'),
                  attrs: { id: hId }
                }));
              } else if (prevDone && hint) {
                cb.removeAttribute('title');
                cb.removeAttribute('aria-describedby');
                hint.parentNode.removeChild(hint);
              }
            }
          }
        }
      }
      // prevDone for the NEXT step uses what we just observed for THIS
      // step — same rule renderStep() uses on first paint.
      prevDone = done;
    }

    // Progress label + bar.
    var doneCount = 0;
    for (var d = 0; d < steps.length; d++) if (doneSet[steps[d].id]) doneCount++;
    var pct = steps.length === 0 ? 0 : Math.round((doneCount / steps.length) * 100);
    var progressLine = sectionEl.querySelector('.wizard-progress');
    if (progressLine) progressLine.textContent = t('wizard.progress', { done: doneCount, total: steps.length });
    var fillEl = sectionEl.querySelector('.wizard-progress-fill');
    if (fillEl) fillEl.style.width = pct + '%';

    // Refresh the auto-run button/status against the latest store snapshot
    // (e.g. a step's ✓ just landed → the "all done" disable + done note flip).
    updateAutoRunControl();
  }

  function onStoreChange() {
    if (router && typeof router.current === 'function' &&
        router.current() === 'wizard') {
      updateLiveStatus();
    }
  }

  var _unsub = null;
  var _unsubAuto = null;

  export const wizard = {
    mount: function (rootEl) {
      sectionEl = rootEl;
      lastGateKey = gateKey(store.get());
      render();
      _unsub = store.on('change', onStoreChange);
      // The auto-runner advances independently of SSE (it polls session events),
      // so its state can change between store ticks — repaint the control on its
      // own 'change' too. updateAutoRunControl self-guards if the control isn't
      // in the DOM (CLI not ready), so calling it off-route is harmless.
      _unsubAuto = autoRun.on(updateAutoRunControl);
    },
    unmount: function () {
      if (_unsub) { _unsub(); _unsub = null; }
      if (_unsubAuto) { _unsubAuto(); _unsubAuto = null; }
    },
    refresh: function () {
      lastGateKey = gateKey(store.get());
      renderPreservingOpen();
    }
  };
