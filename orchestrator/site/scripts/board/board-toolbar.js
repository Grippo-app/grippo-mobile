export function createBoardToolbar(dependencies) {
  var t = dependencies.t;
  var el = dependencies.el;
  var getLanguage = dependencies.getLanguage;
  var taskListStore = dependencies.taskListStore;
  var getSectionElement = dependencies.getSectionElement;
  var schedule = dependencies.schedule;
  var cancelSchedule = dependencies.cancelSchedule;
  var refresh = dependencies.refresh;

  // The Board refreshes after every relevant SSE change. Keep one toolbar DOM
  // node mounted while its language is current: detaching a native <select>
  // closes the browser-owned popup even if an equivalent node is appended in
  // the same render pass.
  var filterTimer = null;
  var toolbarEl = null;
  var toolbarLang = null;

  function currentLanguage() {
    return getLanguage();
  }

  function updateTaskFilter(key, value, immediate) {
    if (!taskListStore.set(key, value)) return;
    if (filterTimer != null) cancelSchedule(filterTimer);
    filterTimer = schedule(function () {
      filterTimer = null;
      refresh();
    }, immediate ? 0 : 220);
  }

  function filterSelect(key, labelKey, choices, selected) {
    var label = el('label', { class: 'board-toolbar__field' });
    label.appendChild(el('span', { class: 'board-toolbar__label', text: t(labelKey) }));
    var select = el('select', {
      class: 'input board-toolbar__select',
      attrs: { 'data-board-filter': key, 'aria-label': t(labelKey) }
    });
    choices.forEach(function (choice) {
      select.appendChild(el('option', {
        value: choice.value,
        text: t(choice.key),
        selected: choice.value === selected
      }));
    });
    select.addEventListener('change', function () {
      updateTaskFilter(key, select.value, true);
    });
    label.appendChild(select);
    return label;
  }

  function render() {
    var sectionEl = getSectionElement();
    var lang = currentLanguage();
    if (toolbarEl && toolbarLang === lang) {
      if (toolbarEl.parentNode !== sectionEl) sectionEl.appendChild(toolbarEl);
      return;
    }
    var filters = taskListStore.get();
    var bar = el('div', {
      class: 'board-toolbar',
      attrs: { 'aria-label': t('board.filter.label') }
    });
    var searchLabel = el('label', {
      class: 'board-toolbar__field board-toolbar__field--search'
    });
    searchLabel.appendChild(el('span', {
      class: 'board-toolbar__label', text: t('board.filter.search')
    }));
    var search = el('input', {
      type: 'search',
      class: 'input board-toolbar__search',
      value: filters.search,
      attrs: {
        'data-board-filter': 'search',
        placeholder: t('board.filter.searchPlaceholder')
      }
    });
    search.addEventListener('input', function () {
      updateTaskFilter('search', search.value, false);
    });
    searchLabel.appendChild(search);
    bar.appendChild(searchLabel);
    bar.appendChild(filterSelect('column', 'board.filter.state', [
      { value: '', key: 'board.filter.allStates' },
      { value: 'backlog', key: 'board.column.backlog' },
      { value: 'pending', key: 'board.column.pending' },
      { value: 'todo', key: 'board.column.todo' },
      { value: 'done', key: 'board.column.done' }
    ], filters.column));
    bar.appendChild(filterSelect('origin', 'board.filter.origin', [
      { value: '', key: 'board.filter.allOrigins' },
      { value: 'manual', key: 'board.origin.manual' },
      { value: 'figma', key: 'board.origin.figma' },
      { value: 'api', key: 'board.origin.api' },
      { value: 'follow-up', key: 'board.origin.follow_up' }
    ], filters.origin));
    bar.appendChild(filterSelect('blocker', 'board.filter.blocker', [
      { value: '', key: 'board.filter.allBlockers' },
      { value: 'blocked', key: 'board.filter.blocked' },
      { value: 'unblocked', key: 'board.filter.unblocked' }
    ], filters.blocker));
    bar.appendChild(filterSelect('dependency', 'board.filter.dependency', [
      { value: '', key: 'board.filter.allDependencies' },
      { value: 'blocked', key: 'board.filter.dependenciesBlocked' },
      { value: 'satisfied', key: 'board.filter.dependenciesSatisfied' },
      { value: 'none', key: 'board.filter.dependenciesNone' }
    ], filters.dependency));
    bar.appendChild(filterSelect('context', 'board.filter.context', [
      { value: '', key: 'board.filter.allContexts' },
      { value: 'figma', key: 'board.origin.figma' },
      { value: 'api', key: 'board.origin.api' },
      { value: 'follow-up', key: 'board.origin.follow_up' }
    ], filters.context));
    bar.appendChild(filterSelect('sort', 'board.filter.sort', [
      { value: 'board', key: 'board.filter.sortBoard' },
      { value: 'recent', key: 'board.filter.sortRecent' },
      { value: 'number', key: 'board.filter.sortNumber' }
    ], filters.sort));
    toolbarEl = bar;
    toolbarLang = lang;
    sectionEl.appendChild(toolbarEl);
  }

  function element() {
    return toolbarEl;
  }

  function preservedElement() {
    var sectionEl = getSectionElement();
    return toolbarEl && toolbarLang === currentLanguage() &&
      toolbarEl.parentNode === sectionEl ? toolbarEl : null;
  }

  return {
    render: render,
    element: element,
    preservedElement: preservedElement
  };
}
