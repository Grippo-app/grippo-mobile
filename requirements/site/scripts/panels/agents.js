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
  // Renderers.
  // ----------------------------------------------------------------------

  var ROLE_ORDER = ['helper', 'builder', 'validator'];
  var ROLE_LABELS = {
    helper:    { title: 'Helpers',    badge: 'helper' },
    builder:   { title: 'Builders',   badge: 'builder' },
    validator: { title: 'Validators', badge: 'validator' }
  };

  var INSTALL_SYMLINK =
    'mkdir -p .claude/agents\n' +
    'ln -sf "$(pwd)/requirements/sub-agents/builders/"*.md   .claude/agents/\n' +
    'ln -sf "$(pwd)/requirements/sub-agents/validators/"*.md .claude/agents/\n' +
    'ln -sf "$(pwd)/requirements/sub-agents/helpers/"*.md    .claude/agents/';

  var INSTALL_COPY =
    'mkdir -p .claude/agents\n' +
    'cp requirements/sub-agents/builders/*.md   .claude/agents/\n' +
    'cp requirements/sub-agents/validators/*.md .claude/agents/\n' +
    'cp requirements/sub-agents/helpers/*.md    .claude/agents/';

  function renderBlurb() {
    sectionEl.appendChild(el('h2', { class: 'panel-title', text: 'Sub-agents' }));
    sectionEl.appendChild(el('p', {
      class: 'panel-lead',
      text: 'Sub-agents are specialized Claude Code agents that read requirements/ and do one job. The orchestrator coordinates them; in practice you invoke only the orchestrator by telling Claude "run task TASK_<N>_<title>.md".'
    }));
  }

  function renderSetupBanner(setupDone) {
    if (setupDone) return;
    var banner = el('div', { class: 'banner banner--info' });
    banner.appendChild(document.createTextNode(
      'Setup incomplete — install commands below still show but reference '
    ));
    banner.appendChild(el('code', { text: '<placeholder>' }));
    banner.appendChild(document.createTextNode(' tokens until you fill the Setup panel. '));
    banner.appendChild(el('a', { href: '#setup', text: 'Open Setup' }));
    sectionEl.appendChild(banner);
  }

  function renderInstall() {
    sectionEl.appendChild(el('h3', { class: 'panel-section-title', text: 'Install into .claude/agents/' }));
    sectionEl.appendChild(el('p', {
      class: 'panel-lead',
      text: 'Sub-agents are NOT auto-discovered by Claude Code. Pick one of the two commands below; run it from your project root.'
    }));

    sectionEl.appendChild(el('h4', { class: 'agents-install-subtitle', text: 'Symlink (recommended — edits propagate)' }));
    sectionEl.appendChild(codeBlock(INSTALL_SYMLINK));

    sectionEl.appendChild(el('h4', { class: 'agents-install-subtitle', text: 'Copy (snapshot — no propagation)' }));
    sectionEl.appendChild(codeBlock(INSTALL_COPY));

    var verifyWrap = el('div', { class: 'agents-verify' });
    verifyWrap.appendChild(el('p', { class: 'panel-lead', text: 'Verify after install:' }));
    verifyWrap.appendChild(codeBlock('ls .claude/agents/ | wc -l'));
    verifyWrap.appendChild(el('p', {
      class: 'panel-lead',
      text: 'Expected: 24 entries — 10 builders + 8 validators + 6 helpers.'
    }));
    sectionEl.appendChild(verifyWrap);
  }

  function renderFlowDiagram() {
    sectionEl.appendChild(el('h3', { class: 'panel-section-title', text: 'Execution flow' }));
    var diagram = el('div', { class: 'agent-flow' });
    var steps = ['Task File', 'Orchestrator', 'Builders', 'Validators', 'External Review', 'Done'];
    for (var i = 0; i < steps.length; i++) {
      diagram.appendChild(el('div', { class: 'agent-flow-step', text: steps[i] }));
    }
    sectionEl.appendChild(diagram);
  }

  function renderSpecHelp() {
    sectionEl.appendChild(el('p', {
      class: 'agents-spec-help panel-lead',
      text: 'Spec links open the agent\'s source markdown. Browsers usually render markdown as plain text or trigger a download — both are fine for reading the spec.'
    }));
  }

  function renderCard(agent) {
    var card = el('div', { class: 'agent-card', data: { role: agent.role } });

    var head = el('div', { class: 'agent-card-head' });
    head.appendChild(el('h4', { class: 'agent-card-title' }, [
      el('code', { text: agent.name })
    ]));
    head.appendChild(el('span', {
      class: 'agent-role-badge agent-role-badge--' + agent.role,
      text: ROLE_LABELS[agent.role].badge
    }));
    card.appendChild(head);

    var what = el('p', { class: 'agent-card-line' });
    what.appendChild(el('strong', { text: 'What: ' }));
    what.appendChild(document.createTextNode(agent.what));
    card.appendChild(what);

    var when = el('p', { class: 'agent-card-line' });
    when.appendChild(el('strong', { text: 'When: ' }));
    when.appendChild(document.createTextNode(agent.when));
    card.appendChild(when);

    var actions = el('div', { class: 'agent-card-actions' });
    actions.appendChild(el('a', {
      class: 'btn agent-spec-link',
      href: agent.specPath,
      text: 'Open spec',
      attrs: { 'target': '_blank', 'rel': 'noopener' }
    }));
    card.appendChild(actions);

    return card;
  }

  function renderRoleSection(role, agents) {
    var meta = ROLE_LABELS[role];
    sectionEl.appendChild(el('h3', { class: 'panel-section-title agents-role-title', text: meta.title }));
    var grid = el('div', { class: 'agent-grid' });
    for (var i = 0; i < agents.length; i++) {
      if (agents[i].role === role) grid.appendChild(renderCard(agents[i]));
    }
    sectionEl.appendChild(grid);
  }

  function renderFooterNote() {
    sectionEl.appendChild(el('p', {
      class: 'panel-lead agents-footer-note',
      text: 'Sub-agents read requirements/00-overview/03-project-config.md at the start of every task. Keep that file in sync with the Setup panel.'
    }));
  }

  function render() {
    if (!sectionEl) return;
    while (sectionEl.firstChild) sectionEl.removeChild(sectionEl.firstChild);

    var state = App.store.get();
    var setupDone = state.progress && state.progress.setupDone === true;
    var agents = App.data.agents || [];

    renderBlurb();
    renderSetupBanner(setupDone);
    renderInstall();
    renderFlowDiagram();
    renderSpecHelp();
    for (var i = 0; i < ROLE_ORDER.length; i++) {
      renderRoleSection(ROLE_ORDER[i], agents);
    }
    renderFooterNote();
  }

  App.panels.agents = {
    mount: function (rootEl) {
      sectionEl = rootEl;
      render();
    },
    refresh: function () {
      render();
    }
  };
})();
