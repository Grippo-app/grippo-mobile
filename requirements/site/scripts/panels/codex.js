(function () {
  window.App = window.App || {};
  App.panels = App.panels || {};

  var sectionEl = null;

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
    return wrap;
  }

  // ----------------------------------------------------------------------
  // Data.
  // ----------------------------------------------------------------------

  var MATRIX = [
    {
      codexEnabled: 'auto',
      codexInstalled: 'yes',
      reviewer: 'codex-review-loop',
      escalation: false
    },
    {
      codexEnabled: 'auto',
      codexInstalled: 'no',
      reviewer: 'internal-reviewer',
      escalation: false
    },
    {
      codexEnabled: 'true',
      codexInstalled: 'yes',
      reviewer: 'codex-review-loop',
      escalation: false
    },
    {
      codexEnabled: 'true',
      codexInstalled: 'no',
      reviewer: 'escalation — orchestrator refuses to silently downgrade',
      escalation: true
    },
    {
      codexEnabled: 'false',
      codexInstalled: '(skipped)',
      reviewer: 'internal-reviewer',
      escalation: false
    }
  ];

  var INSTALL_MARKETPLACE = '/plugin marketplace add openai/codex-plugin-cc';
  var INSTALL_PLUGIN      = '/plugin install codex@openai-codex';

  var RADIO_OPTIONS = [
    { value: 'auto',  label: 'auto (default)',
      hint: 'Detect Codex at runtime; fall back to internal-reviewer when missing.' },
    { value: 'true',  label: 'true',
      hint: 'Force codex-review-loop. Orchestrator hard-fails if the plugin is missing.' },
    { value: 'false', label: 'false',
      hint: 'Skip Codex detection entirely; always use internal-reviewer.' }
  ];

  // ----------------------------------------------------------------------
  // Renderers.
  // ----------------------------------------------------------------------

  function renderBlurb() {
    sectionEl.appendChild(el('h2', { class: 'panel-title', text: 'Codex Loop' }));
    sectionEl.appendChild(el('p', {
      class: 'panel-lead',
      text: 'After internal validators are green, one external reviewer runs on every task. It catches what structural validators don\'t — logic bugs, scope leaks, missed edge cases.'
    }));
  }

  function renderSetupBanner(setupDone) {
    if (setupDone) return;
    var banner = el('div', { class: 'banner banner--info' });
    banner.appendChild(document.createTextNode(
      'Setup incomplete — your codexEnabled choice below saves to localStorage, but the YAML in '
    ));
    banner.appendChild(el('code', { text: 'requirements/00-overview/03-project-config.md' }));
    banner.appendChild(document.createTextNode(' is still a placeholder until you finish Setup. '));
    banner.appendChild(el('a', { href: '#setup', text: 'Open Setup' }));
    sectionEl.appendChild(banner);
  }

  function renderMatrix() {
    sectionEl.appendChild(el('h3', {
      class: 'panel-section-title',
      text: 'codexEnabled × Codex detection'
    }));

    var tableWrap = el('div', { class: 'codex-table-wrap' });
    var table = el('table', { class: 'codex-matrix' });

    var thead = el('thead');
    var headRow = el('tr');
    headRow.appendChild(el('th', { text: 'codexEnabled' }));
    headRow.appendChild(el('th', { text: 'Codex installed' }));
    headRow.appendChild(el('th', { text: 'Reviewer that runs' }));
    thead.appendChild(headRow);
    table.appendChild(thead);

    var tbody = el('tbody');
    for (var i = 0; i < MATRIX.length; i++) {
      var row = MATRIX[i];
      var tr = el('tr', { data: { escalation: row.escalation ? 'true' : 'false' } });
      tr.appendChild(el('td', {}, [el('code', { text: row.codexEnabled })]));
      tr.appendChild(el('td', { text: row.codexInstalled }));
      var td = el('td');
      if (row.escalation) {
        td.appendChild(el('strong', { text: row.reviewer }));
      } else {
        td.appendChild(el('code', { text: row.reviewer }));
      }
      tr.appendChild(td);
      tbody.appendChild(tr);
    }
    table.appendChild(tbody);
    tableWrap.appendChild(table);
    sectionEl.appendChild(tableWrap);
  }

  function renderCallouts() {
    var row = el('div', { class: 'codex-callouts' });

    var why = el('div', { class: 'card codex-callout' });
    why.appendChild(el('h4', { class: 'codex-callout-title', text: 'Why Codex' }));
    why.appendChild(el('p', {
      class: 'codex-callout-body',
      text: 'A non-Claude model reviews the writer model\'s work. Cross-provider review reduces sycophancy and catches assumptions the writer rationalised away.'
    }));
    row.appendChild(why);

    var internal = el('div', { class: 'card codex-callout' });
    internal.appendChild(el('h4', { class: 'codex-callout-title', text: 'Why internal-reviewer is fine' }));
    internal.appendChild(el('p', {
      class: 'codex-callout-body',
      text: 'Same model family, so no provider independence — but it still catches obvious bugs and scope leaks. The safe default when Codex isn\'t available.'
    }));
    row.appendChild(internal);

    sectionEl.appendChild(row);
  }

  function renderInstall() {
    sectionEl.appendChild(el('h3', {
      class: 'panel-section-title',
      text: 'Install the Codex plugin'
    }));
    sectionEl.appendChild(el('p', {
      class: 'panel-lead',
      text: 'Run inside a Claude Code session (not a terminal). The first command registers the marketplace; the second installs the plugin from it.'
    }));
    sectionEl.appendChild(codeBlock(INSTALL_MARKETPLACE));
    sectionEl.appendChild(codeBlock(INSTALL_PLUGIN));
  }

  function renderRadioPicker(currentValue) {
    sectionEl.appendChild(el('h3', {
      id: 'codex-radio-label',
      class: 'panel-section-title',
      text: 'Which value should I pick?'
    }));
    sectionEl.appendChild(el('p', {
      class: 'panel-lead',
      text: 'Updates your saved setup immediately; you still need to mirror the change in requirements/00-overview/03-project-config.md.'
    }));

    var group = el('div', {
      class: 'codex-radio-group',
      attrs: { role: 'radiogroup', 'aria-labelledby': 'codex-radio-label' }
    });
    for (var i = 0; i < RADIO_OPTIONS.length; i++) {
      var opt = RADIO_OPTIONS[i];
      var wrap = el('label', { class: 'codex-radio-option' });
      var input = el('input', {
        type: 'radio',
        name: 'codex-enabled',
        value: opt.value
      });
      if (opt.value === currentValue) input.checked = true;
      input.addEventListener('change', onRadioChange);
      wrap.appendChild(input);
      var meta = el('div', { class: 'codex-radio-meta' });
      meta.appendChild(el('span', { class: 'codex-radio-label', text: opt.label }));
      meta.appendChild(el('span', { class: 'field-help', text: opt.hint }));
      wrap.appendChild(meta);
      group.appendChild(wrap);
    }
    sectionEl.appendChild(group);

    var yamlWrap = el('div', { class: 'codex-yaml-banner' });
    var warn = el('div', { class: 'banner banner--warn' });
    warn.appendChild(document.createTextNode('Update '));
    warn.appendChild(el('code', { text: 'requirements/00-overview/03-project-config.md' }));
    warn.appendChild(document.createTextNode(' to match — the YAML line is below.'));
    yamlWrap.appendChild(warn);
    yamlWrap.appendChild(codeBlock('codexEnabled: ' + currentValue));
    sectionEl.appendChild(yamlWrap);
  }

  function renderMiniFlow() {
    sectionEl.appendChild(el('h3', {
      class: 'panel-section-title',
      text: 'How a task reaches the gate'
    }));
    var flow = el('div', { class: 'codex-flow' });
    var steps = [
      'Validators green',
      'Read codexEnabled + detect Codex',
      'Route to Codex OR internal-reviewer',
      'Loop on findings',
      'Task done'
    ];
    for (var i = 0; i < steps.length; i++) {
      flow.appendChild(el('div', { class: 'codex-flow-step', text: steps[i] }));
    }
    sectionEl.appendChild(flow);
  }

  // ----------------------------------------------------------------------
  // Event handlers.
  // ----------------------------------------------------------------------

  function onRadioChange(ev) {
    var value = ev.target.value;
    App.store.saveSetup({ codexEnabled: value });
    render();
  }

  // ----------------------------------------------------------------------
  // Render orchestration.
  // ----------------------------------------------------------------------

  function render() {
    if (!sectionEl) return;
    while (sectionEl.firstChild) sectionEl.removeChild(sectionEl.firstChild);

    var state = App.store.get();
    var setupDone = state.progress && state.progress.setupDone === true;
    var stored = state.setup && state.setup.codexEnabled;
    var currentValue = (stored === 'auto' || stored === 'true' || stored === 'false')
      ? stored
      : 'auto';

    renderBlurb();
    renderSetupBanner(setupDone);
    renderMatrix();
    renderCallouts();
    renderInstall();
    renderRadioPicker(currentValue);
    renderMiniFlow();
  }

  App.panels.codex = {
    mount: function (rootEl) {
      sectionEl = rootEl;
      render();
    },
    refresh: function () {
      render();
    }
  };
})();
