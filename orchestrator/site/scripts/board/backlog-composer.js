import { dom } from '../dom.js';
import { assembleBacklogBody } from './backlog-body.js';

var el = dom.el;

export function createBacklogComposer(dependencies) {
  var t = dependencies.t;
  var clipboard = dependencies.clipboard;
  var store = dependencies.store;
  var tasksApi = dependencies.tasksApi;
  var globalMutationBlocked = dependencies.globalMutationBlocked;
  var createBacklogWithIntegrityFence = dependencies.createBacklogWithIntegrityFence;
  var closeModal = dependencies.closeModal;
  var createCloseButton = dependencies.createCloseButton;
  var loadTaskInbox = dependencies.loadTaskInbox;
  var handleTaskMutationConflict = dependencies.handleTaskMutationConflict;
  var boardRequestError = dependencies.boardRequestError;
  var openModal = dependencies.openModal;

  // ---- composer field builders (used only by open) ----

  // A labeled single input: a real <label> so clicking the caption focuses it.
  function composerField(labelText, inputNode) {
    var l = el('label', { class: 'board-modal__label' });
    l.appendChild(el('span', { class: 'board-modal__label-text', text: labelText }));
    l.appendChild(inputNode);
    return l;
  }
  // A COLLAPSIBLE section card for the create modal: a clickable header (chevron + title + a live
  // count badge) that toggles its body. Optional/secondary content lives here collapsed-by-default,
  // so the modal opens SHORT (Title + Goal + a few quiet section headers) and the author expands
  // only what they need. Returns { section, body, setCount(n) }. Fully keyboard/ARIA accessible.
  function composerSection(titleText, node, opts) {
    opts = opts || {};
    var section = el('section', { class: 'board-create__section' });
    var head = el('button', { type: 'button', class: 'board-create__section-head', attrs: { 'aria-expanded': opts.open ? 'true' : 'false' } });
    head.appendChild(el('span', { class: 'board-create__section-chev', attrs: { 'aria-hidden': 'true' } }));
    head.appendChild(el('span', { class: 'board-create__section-title', text: titleText }));
    var badge = el('span', { class: 'board-create__section-count' });
    badge.style.display = 'none';
    head.appendChild(badge);
    // Body is a grid-rows animator (0fr <-> 1fr) so expand/collapse EASES the modal's height instead
    // of jumping; the inner clips to that track (overflow:hidden, min-height:0). Open/closed is driven
    // by the --open class alone — NO inline display toggle, which would kill the CSS transition.
    var body = el('div', { class: 'board-create__section-body' });
    var inner = el('div', { class: 'board-create__section-inner' });
    inner.appendChild(node);
    body.appendChild(inner);
    var open = !!opts.open;
    function apply() {
      section.classList.toggle('board-create__section--open', open);
      head.setAttribute('aria-expanded', open ? 'true' : 'false');
    }
    head.addEventListener('click', function () { open = !open; apply(); });
    section.appendChild(head); section.appendChild(body); apply();
    return {
      section: section,
      body: body,
      // A count badge on the header so a collapsed section still shows whether it has content.
      setCount: function (n) { if (n > 0) { badge.textContent = String(n); badge.style.display = ''; } else { badge.style.display = 'none'; } },
    };
  }

  // A repeatable single-line bullet list with a per-row × and a "+ add" button.
  // Returns { wrap, get } — get() yields the raw string values in order (the
  // assembler trims + drops empties). Always keeps at least one row so the field
  // never collapses to nothing.
  function composerBulletList(addLabel, removeAria, inputAria) {
    var list = el('div', { class: 'board-create__list' });
    // The × only appears once there is a second row; a lone required row has nothing to remove.
    function syncRemove() {
      var rows = list.querySelectorAll('.board-create__row');
      var multi = rows.length > 1;
      for (var i = 0; i < rows.length; i++) {
        var b = rows[i].querySelector('.board-create__remove');
        if (b) b.style.display = multi ? '' : 'none';
      }
    }
    function addRow(val) {
      var row = el('div', { class: 'board-create__row' });
      var input = el('input', {
        type: 'text', class: 'input board-modal__input', value: val || '',
        attrs: { 'aria-label': inputAria }
      });
      var rm = el('button', { type: 'button', class: 'board-create__remove', text: '×',
        attrs: { 'aria-label': removeAria, title: removeAria } });
      rm.addEventListener('click', function () {
        list.removeChild(row); syncRemove();
        var first = list.querySelector('input'); if (first) first.focus();
      });
      row.appendChild(input); row.appendChild(rm);
      list.appendChild(row);
      syncRemove();
      return input;
    }
    addRow('');
    var add = el('button', { type: 'button', class: 'board-create__add', text: addLabel });
    add.addEventListener('click', function () { addRow('').focus(); });
    var wrap = el('div', { class: 'board-create__field' });
    wrap.appendChild(list); wrap.appendChild(add);
    return {
      wrap: wrap,
      get: function () {
        return Array.prototype.map.call(list.querySelectorAll('input'), function (i) { return i.value; });
      }
    };
  }

  // A repeatable Design row: ScreenName + kind <select> + Figma URL + optional
  // dark-URL. Returns { wrap, get } — get() yields [{ screen, kind, url, darkUrl }];
  // the assembler turns each into the FULL bullet grammar the census/spec gate
  // parse (`- <Screen> [kind] — light:<url> dark:<url>`): the [kind] tag only when
  // != screen, the light:/dark: tags only when the dark URL is filled, plain
  // `- <Screen> — <url>` otherwise. No-mock is an explicit toggle with a required
  // reason; leaving both URLs empty is incomplete input, never an implicit opt-out.
  // Only mounted when the project is figmaEnabled.
  var DESIGN_ROW_KINDS = ['screen', 'dialog', 'component', 'overlay'];
  // A repeatable Design BLOCK: a base screen (name + kind + optional own URL/dark-URL, i.e. its
  // primary/loaded state) with an UNLIMITED nested list of STATES (each: name + URL + dark-URL).
  // get() FLATTENS to the same [{screen, kind, url, darkUrl}] rows the assembler already emits:
  // the base bullet + one `<Base><State>` bullet per state (a state of a [dialog] inherits the
  // kind). This is the authoring mirror of the Screens-tab grouping (Wave 13): you enter Home +
  // states Loading/Empty and the pipeline gets `Home`, `HomeLoading`, `HomeEmpty` bullets, which
  // the gallery then re-groups under "Home". Only mounted when the project is figmaEnabled.
  function composerDesignList(labels) {
    var blocks = el('div', { class: 'board-create__design-blocks' });
    function cap(s) { s = String(s || '').trim(); return s ? s.charAt(0).toUpperCase() + s.slice(1) : ''; }

    // A STATE row is the carrier of a Figma ref: an optional state name (blank = the primary/loaded
    // state → the base bullet) + a light URL + a dark URL. The URL lives here, NOT on the name row,
    // so there is ALWAYS a place to say which state a ref is (even a single-state screen).
    function addStateRow(statesList, syncTag, ensureOne) {
      var row = el('div', { class: 'board-create__state-row' });
      var name = el('input', { type: 'text', class: 'input board-modal__input board-create__state-name', attrs: { placeholder: labels.state, 'aria-label': labels.state } });
      var url = el('input', { type: 'text', class: 'input board-modal__input board-create__design-url', attrs: { placeholder: labels.url, 'aria-label': labels.url } });
      var darkUrl = el('input', { type: 'text', class: 'input board-modal__input board-create__design-dark-url', attrs: { placeholder: labels.darkUrl, 'aria-label': labels.darkUrl } });
      var rm = el('button', { type: 'button', class: 'board-create__remove', text: '×', attrs: { 'aria-label': labels.removeState, title: labels.removeState } });
      rm.addEventListener('click', function () { statesList.removeChild(row); if (ensureOne) ensureOne(); if (syncTag) syncTag(); });
      url.addEventListener('input', syncTag); darkUrl.addEventListener('input', syncTag);
      row.appendChild(name); row.appendChild(url); row.appendChild(darkUrl); row.appendChild(rm);
      statesList.appendChild(row);
      return name;
    }

    function addBlock() {
      var block = el('div', { class: 'board-create__design-block' });
      var head = el('div', { class: 'board-create__design-row' });
      var screen = el('input', { type: 'text', class: 'input board-modal__input board-create__design-screen', attrs: { placeholder: labels.screen, 'aria-label': labels.screen } });
      // Node kind — the literal design-parser tag tokens (agent-facing grammar, en-only); only the
      // accessible label localizes. 'screen' emits no tag. The state rows inherit this kind.
      var kind = el('select', { class: 'input board-modal__input board-create__design-kind', attrs: { 'aria-label': labels.kind, title: labels.kind } });
      for (var k = 0; k < DESIGN_ROW_KINDS.length; k++) kind.appendChild(el('option', { value: DESIGN_ROW_KINDS[k], text: DESIGN_ROW_KINDS[k] }));
      // The no-mock pill is visible only after the explicit toggle is on. Empty URLs
      // are incomplete input, not a passive no-mock signal.
      var tag = el('span', { class: 'board-create__nomock-tag', text: labels.noMock });
      var statesList = el('div', { class: 'board-create__states' });
      // B3: explicit one-click "no mock" toggle + a reason input — states intent AND why in one
      // action. When ON: the state rows are removed (a no-mock screen has none) + the reason shown.
      // Persisted on `block.dataset.nomock` so get() can read it.
      var noMockOn = false;
      var noMockBtn = el('button', { type: 'button', class: 'board-create__nomock-toggle', text: labels.noMockToggle, attrs: { 'aria-pressed': 'false', title: labels.noMockToggle } });
      var reason = el('input', { type: 'text', class: 'input board-modal__input board-create__nomock-reason', attrs: { placeholder: labels.noMockReason, 'aria-label': labels.noMockReason } });
      reason.style.display = 'none';
      function syncTag() {
        tag.style.display = (noMockOn && screen.value.trim()) ? '' : 'none';
      }
      // At least ONE state row is mandatory (the URL carrier) whenever the block is not no-mock.
      function ensureOneState() {
        if (!noMockOn && statesList.querySelectorAll('.board-create__state-row').length === 0) addStateRow(statesList, syncTag, ensureOneState);
      }
      screen.addEventListener('input', syncTag);
      var rmScreen = el('button', { type: 'button', class: 'board-create__remove', text: '×', attrs: { 'aria-label': labels.remove, title: labels.remove } });
      rmScreen.addEventListener('click', function () {
        if (blocks.children.length > 1) blocks.removeChild(block);
        else { screen.value = ''; kind.value = 'screen'; while (statesList.firstChild) statesList.removeChild(statesList.firstChild); noMockOn = false; applyNoMock(); }
        var first = blocks.querySelector('input'); if (first) first.focus();
      });
      head.appendChild(screen); head.appendChild(kind); head.appendChild(noMockBtn); head.appendChild(tag); head.appendChild(rmScreen);
      var addState = el('button', { type: 'button', class: 'board-create__add board-create__add--state', text: labels.addState });
      addState.addEventListener('click', function () { if (noMockOn) return; var n = addStateRow(statesList, syncTag, ensureOneState); syncTag(); n.focus(); });
      function applyNoMock() {
        block.dataset.nomock = noMockOn ? '1' : '';
        noMockBtn.setAttribute('aria-pressed', noMockOn ? 'true' : 'false');
        noMockBtn.classList.toggle('board-create__nomock-toggle--on', noMockOn);
        addState.disabled = noMockOn;
        // no-mock ON → remove every state row (a no-mock screen has none); OFF → restore the one
        // mandatory URL carrier. Keeps the UI matching exactly what get() emits.
        if (noMockOn) { while (statesList.firstChild) statesList.removeChild(statesList.firstChild); }
        else { ensureOneState(); }
        reason.style.display = noMockOn ? '' : 'none';
        syncTag();
      }
      noMockBtn.addEventListener('click', function () { noMockOn = !noMockOn; applyNoMock(); if (noMockOn) reason.focus(); });
      block.appendChild(head); block.appendChild(reason); block.appendChild(statesList); block.appendChild(addState);
      blocks.appendChild(block);
      applyNoMock();   // noMockOn=false → creates the initial mandatory state row
      return screen;
    }
    addBlock();
    var add = el('button', { type: 'button', class: 'board-create__add', text: labels.add });
    add.addEventListener('click', function () { addBlock().focus(); });
    var hint = el('p', { class: 'board-create__hint', text: labels.hint });
    var wrap = el('div', { class: 'board-create__field' });
    wrap.appendChild(blocks); wrap.appendChild(add); wrap.appendChild(hint);
    return {
      wrap: wrap,
      validate: function () {
        var invalid = null;
        Array.prototype.forEach.call(blocks.children, function (block) {
          var baseEl = block.querySelector('.board-create__design-screen');
          Array.prototype.forEach.call(block.querySelectorAll('.board-create__design-screen, .board-create__design-url, .board-create__design-dark-url, .board-create__nomock-reason'), function (input) {
            input.classList.remove('board-modal__input--error');
          });
          if (!baseEl) return;
          if (!baseEl.value.trim()) {
            var hasUnnamedInput = block.getAttribute('data-nomock') === '1' || Array.prototype.some.call(
              block.querySelectorAll('.board-create__state-name, .board-create__design-url, .board-create__design-dark-url, .board-create__nomock-reason'),
              function (input) { return !!input.value.trim(); }
            );
            if (hasUnnamedInput) {
              baseEl.classList.add('board-modal__input--error');
              if (!invalid) invalid = { input: baseEl, key: 'board.create.designNameRequired' };
            }
            return;
          }
          if (block.getAttribute('data-nomock') === '1') {
            var reasonEl = block.querySelector('.board-create__nomock-reason');
            var cleanReason = reasonEl ? reasonEl.value.replace(/[()\r\n]+/g, ' ').trim() : '';
            if (!cleanReason) {
              if (reasonEl) reasonEl.classList.add('board-modal__input--error');
              if (!invalid) invalid = { input: reasonEl || baseEl, key: 'board.create.designReasonRequired' };
            }
            return;
          }
          var emitted = false;
          Array.prototype.forEach.call(block.querySelectorAll('.board-create__state-row'), function (sr) {
            var nameEl = sr.querySelector('.board-create__state-name');
            var urlEl = sr.querySelector('.board-create__design-url');
            var darkEl = sr.querySelector('.board-create__design-dark-url');
            var hasRow = (nameEl && nameEl.value.trim()) || (urlEl && urlEl.value.trim()) || (darkEl && darkEl.value.trim());
            if (!hasRow) return;
            emitted = true;
            if (!(urlEl && urlEl.value.trim()) && !(darkEl && darkEl.value.trim())) {
              if (urlEl) urlEl.classList.add('board-modal__input--error');
              if (!invalid) invalid = { input: urlEl || baseEl, key: 'board.create.designRequired' };
            }
          });
          if (!emitted) {
            var firstUrl = block.querySelector('.board-create__design-url');
            if (firstUrl) firstUrl.classList.add('board-modal__input--error');
            if (!invalid) invalid = { input: firstUrl || baseEl, key: 'board.create.designRequired' };
          }
        });
        if (!invalid) return true;
        clipboard.toastError(t(invalid.key));
        try { invalid.input.focus(); } catch (e) {}
        return false;
      },
      get: function () {
        var rows = [];
        Array.prototype.forEach.call(blocks.children, function (block) {
          var base = block.querySelector('.board-create__design-screen').value.trim();
          if (!base) return;                                            // unnamed block → skip (states too)
          var kind = block.querySelector('.board-create__design-kind').value;
          // B3: an explicit no-mock leaf → a single `none (<reason>)` row, no url, no states.
          if (block.getAttribute('data-nomock') === '1') {
            var reasonEl = block.querySelector('.board-create__nomock-reason');
            rows.push({ screen: base, kind: kind, url: '', darkUrl: '', noMock: true, noMockReason: (reasonEl ? reasonEl.value : '').trim() });
            return;
          }
          // Every ref comes from a STATE row now. A BLANK state name is the primary/loaded state →
          // the base bullet (`<base>`); a named state → `<base><State>`. A fully-blank row is
          // skipped. If NO row carries anything, emit the incomplete base row; structured-mode
          // validation blocks creation until the owner supplies a URL or explicitly selects no mock.
          var emitted = 0;
          Array.prototype.forEach.call(block.querySelectorAll('.board-create__state-row'), function (sr) {
            var sn = sr.querySelector('.board-create__state-name').value.trim();
            var u = sr.querySelector('.board-create__design-url').value.trim();
            var du = sr.querySelector('.board-create__design-dark-url').value.trim();
            if (!sn && !u && !du) return;   // blank row → skip
            rows.push({ screen: sn ? base + cap(sn) : base, kind: kind, url: u, darkUrl: du });
            emitted++;
          });
          if (!emitted) rows.push({ screen: base, kind: kind, url: '', darkUrl: '' });
        });
        return rows;
      }
    };
  }

  function open(options) {
    var inboxMode = !!(options && options.inbox === true);
    if (!inboxMode && globalMutationBlocked()) {
      clipboard.toastError(t('board.integrity.createBlocked'));
      return;
    }
    // Figma projects get the guided Design rows; non-Figma projects never see the
    // section (matches assembleBacklogBody, which omits ## Design unless enabled).
    var figmaEnabled = (((store.get() || {}).setup) || {}).figmaEnabled === true;

    var content = el('div', { class: 'board-modal__body board-modal__body--create' });
    content.appendChild(el('h3', {
      class: 'board-modal__title',
      text: t(inboxMode ? 'board.inbox.createTitle' : 'board.create.title')
    }));

    // PRIMARY / ESSENTIALS zone — Title + Goal, PINNED above the scroll (a sibling of the scrolling
    // form, so it never scrolls). These are the only fields a quick task needs; everything else is
    // optional and lives in collapsible section cards inside the scroll below.
    var essentials = el('div', { class: 'board-create__essentials' });
    content.appendChild(essentials);

    var form = el('div', { class: 'board-modal__form' });

    // Title (required). The deterministic server transaction owns numbering,
    // Unicode-safe slugging and the `# TASK <N> — <title>` first line.
    var titleLabel = el('label', { class: 'board-modal__label' });
    titleLabel.appendChild(el('span', { class: 'board-modal__label-text', text: t('board.create.titleField') }));
    var titleInput = el('input', { type: 'text', class: 'input board-modal__input' });
    titleLabel.appendChild(titleInput);
    essentials.appendChild(titleLabel);
    var titleError = el('p', { class: 'board-modal__field-error', text: t('board.create.titleRequired') });
    titleError.style.display = 'none';
    essentials.appendChild(titleError);

    // Raw-markdown escape hatch toggle — a power-user hatch that REPLACES the structured form with
    // one raw <textarea>. Defined here but MOUNTED IN THE FOOTER (below) so it doesn't compete with
    // the fields. Switching ON seeds the textarea from the structured fields (fields → raw, one way)
    // so power users start from the canonical shape; switching back leaves the fields untouched
    // (raw edits are not parsed back — the rawNote caveat).
    var rawToggle = el('label', { class: 'board-create__rawtoggle' });
    var rawCheck = el('input', { type: 'checkbox', class: 'choice-input' });
    rawToggle.appendChild(rawCheck);
    rawToggle.appendChild(el('span', { text: t('board.create.rawToggle') }));

    // ---- structured fields ----
    var structured = el('div', { class: 'board-create__structured' });

    // Goal (the essential "what") joins the PINNED essentials zone, right under Title.
    var goalInput = el('textarea', { class: 'input board-modal__textarea board-create__goal',
      attrs: { rows: '3', placeholder: t('board.create.goalPlaceholder') } });
    var goalFieldEl = composerField(t('board.create.goalField'), goalInput);
    essentials.appendChild(goalFieldEl);

    // Inputs — data + entry points (collapsible, collapsed by default).
    var dataInput = el('input', { type: 'text', class: 'input board-modal__input',
      attrs: { placeholder: t('board.create.dataPlaceholder'), 'aria-label': t('board.create.dataField') } });
    var entries = composerBulletList(t('board.create.entryAdd'), t('board.create.rowRemove'), t('board.create.entryField'));
    var inputsInner = el('div', { class: 'board-create__field' });
    inputsInner.appendChild(el('span', { class: 'board-create__sublabel', text: t('board.create.dataField') }));
    inputsInner.appendChild(dataInput);
    inputsInner.appendChild(el('span', { class: 'board-create__sublabel', text: t('board.create.entryField') }));
    inputsInner.appendChild(entries.wrap);
    var inputsSec = composerSection(t('board.create.inputsField'), inputsInner);
    structured.appendChild(inputsSec.section);

    // Design/screens (figmaEnabled only) — the biggest section; collapsed by default.
    var design = null, designSec = null;
    if (figmaEnabled) {
      design = composerDesignList({
        screen: t('board.create.designScreen'), url: t('board.create.designUrl'),
        kind: t('board.create.designKind'), darkUrl: t('board.create.designDarkUrl'),
        noMock: t('board.create.noMockTag'), add: t('board.create.designAdd'),
        noMockToggle: t('board.create.noMockToggle'), noMockReason: t('board.create.noMockReason'),
        state: t('board.create.designState'), addState: t('board.create.designAddState'),
        removeState: t('board.create.designRemoveState'),
        remove: t('board.create.rowRemove'), hint: t('board.create.designHint')
      });
      designSec = composerSection(t('board.create.designField'), design.wrap);
      structured.appendChild(designSec.section);
    }

    // Acceptance — automated + manual criteria (collapsible).
    var automated = composerBulletList(t('board.create.bulletAdd'), t('board.create.rowRemove'), t('board.create.automatedField'));
    var manual = composerBulletList(t('board.create.bulletAdd'), t('board.create.rowRemove'), t('board.create.manualField'));
    var accInner = el('div', { class: 'board-create__field' });
    accInner.appendChild(el('span', { class: 'board-create__sublabel', text: t('board.create.automatedField') }));
    accInner.appendChild(automated.wrap);
    accInner.appendChild(el('p', { class: 'board-create__hint', text: t('board.create.automatedHint') }));
    accInner.appendChild(el('span', { class: 'board-create__sublabel', text: t('board.create.manualField') }));
    accInner.appendChild(manual.wrap);
    var accSec = composerSection(t('board.create.acceptanceField'), accInner);
    structured.appendChild(accSec.section);

    // Out of scope (collapsible).
    var outOfScope = composerBulletList(t('board.create.bulletAdd'), t('board.create.rowRemove'), t('board.create.outOfScopeField'));
    var oosSec = composerSection(t('board.create.outOfScopeField'), outOfScope.wrap);
    structured.appendChild(oosSec.section);

    form.appendChild(structured);

    // Live count badges on the collapsed section headers — recompute on any edit (input) or
    // structural change (a bubbled click: add / remove / section toggle). So a collapsed section
    // still shows at a glance whether it holds anything, without expanding it.
    function nonEmpty(arr) { return (arr || []).filter(function (x) { return String(x || '').trim(); }).length; }
    function updateCounts() {
      inputsSec.setCount((dataInput.value.trim() ? 1 : 0) + nonEmpty(entries.get()));
      if (designSec) designSec.setCount((design.get() || []).length);
      accSec.setCount(nonEmpty(automated.get()) + nonEmpty(manual.get()));
      oosSec.setCount(nonEmpty(outOfScope.get()));
    }
    structured.addEventListener('input', updateCounts);
    structured.addEventListener('click', function () { setTimeout(updateCounts, 0); });

    // ---- raw fields (hidden until the toggle flips) ----
    var rawWrap = el('div', { class: 'board-create__raw' });
    rawWrap.style.display = 'none';
    var rawInput = el('textarea', { class: 'input board-modal__textarea',
      attrs: { rows: '18', placeholder: t('board.create.bodyPlaceholder') } });
    rawWrap.appendChild(composerField(t('board.create.bodyField'), rawInput));
    rawWrap.appendChild(el('p', { class: 'board-create__hint', text: t('board.create.rawNote') }));
    form.appendChild(rawWrap);

    function readFields() {
      return {
        figmaEnabled: figmaEnabled,
        goal: goalInput.value,
        data: dataInput.value,
        entries: entries.get(),
        designRows: design ? design.get() : [],
        automated: automated.get(),
        manual: manual.get(),
        outOfScope: outOfScope.get()
      };
    }
    var rawMode = false;
    // The body the submit buttons send: the raw textarea when in raw mode, else
    // the canonical string assembled from the structured fields.
    function currentBody() { return rawMode ? rawInput.value : assembleBacklogBody(readFields()); }

    rawCheck.addEventListener('change', function () {
      rawMode = rawCheck.checked;
      // Goal lives in the pinned essentials zone (outside `structured`), so hide it explicitly in
      // raw mode — the raw body already carries the Goal. Title stays (required in both modes).
      goalFieldEl.style.display = rawMode ? 'none' : '';
      if (rawMode) {
        rawInput.value = assembleBacklogBody(readFields());
        structured.style.display = 'none';
        rawWrap.style.display = '';
        rawInput.focus();
      } else {
        rawWrap.style.display = 'none';
        structured.style.display = '';
      }
    });

    // Split lineage is authoritative only when task-prep Step 5.5 writes `## Origin`
    // for a child task; regen-index derives splitFrom from that source.

    content.appendChild(form);

    function validateTitle() {
      if (!titleInput.value.trim()) {
        titleInput.classList.add('board-modal__input--error');
        titleError.style.display = '';
        titleInput.focus();
        return false;
      }
      titleInput.classList.remove('board-modal__input--error');
      titleError.style.display = 'none';
      return true;
    }

    var actions = el('div', { class: 'board-modal__actions' });
    // The raw-markdown hatch stays left-aligned and quiet. Create is the primary
    // (rightmost) action; Cancel closes.
    actions.appendChild(rawToggle);
    // Keep one key for the lifetime of this form. A double-click, client retry,
    // or lost HTTP response therefore converges on the same durable receipt.
    var createKey = tasksApi.creationKey(inboxMode ? 'setup-inbox' : 'board-create');
    var createBtn = el('button', {
      type: 'button',
      class: 'btn btn--primary',
      text: t(inboxMode ? 'board.inbox.save' : 'board.create.run')
    });
    var submitError = el('p', {
      class: 'board-modal__field-error',
      attrs: { role: 'alert', 'aria-live': 'assertive' }
    });
    submitError.style.display = 'none';
    content.appendChild(submitError);
    // Creation runs the whole publication transaction and is allowed up to
    // ~130s, so the draft-bearing modal must say it is working and must never
    // report a failure only through something that fades.
    var createLabel = createBtn.textContent;
    function showCreateError(message) {
      // Unhide BEFORE writing: a live region that is display:none when its text
      // changes is not announced.
      submitError.style.display = '';
      submitError.textContent = message;
      try { submitError.scrollIntoView({ block: 'nearest' }); } catch (e) {}
    }
    function saveFailedText(err) {
      return t(inboxMode ? 'board.inbox.saveFailed' : 'board.create.failed', {
        detail: boardRequestError(err)
      });
    }
    function endCreateAttempt() {
      createBtn.disabled = false;
      createBtn.textContent = createLabel;
    }
    createBtn.addEventListener('click', function () {
      if (!inboxMode && globalMutationBlocked()) {
        // The block is transient — disabling the button here bricked the only
        // way out of a modal still holding the user's whole draft.
        showCreateError(t('board.integrity.createBlocked'));
        return;
      }
      if (!validateTitle()) return;
      if (!rawMode && design && !design.validate()) return;
      submitError.style.display = 'none';
      createBtn.disabled = true;
      createBtn.textContent = t('board.create.creating');
      var save = inboxMode
        ? tasksApi.saveTaskInbox(titleInput.value, currentBody(), { idempotencyKey: createKey })
        : createBacklogWithIntegrityFence(titleInput.value, currentBody(), { idempotencyKey: createKey });
      save.then(function (resp) {
        closeModal();
        if (inboxMode) {
          clipboard.toast(t('board.inbox.saved'));
          loadTaskInbox();
        } else {
          clipboard.toast(t('board.create.created', { stem: (resp && resp.stem) || '' }));
          store.load();
        }
      }, function (err) {
        endCreateAttempt();
        // Admission conflicts refresh the board, but force-closing this modal
        // would throw away everything the user wrote. Keep it open and explain.
        if (handleTaskMutationConflict(err, { closeModal: false })) {
          showCreateError(saveFailedText(err));
          return;
        }
        showCreateError(saveFailedText(err));
      });
    });
    actions.appendChild(createBtn);
    actions.appendChild(createCloseButton());
    content.appendChild(actions);

    openModal(content);
    // Focus the title field for immediate typing.
    setTimeout(function () { try { titleInput.focus(); } catch (e) {} }, 0);
  }

  return { open: open };
}
