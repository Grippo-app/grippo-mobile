import { TASK_COLUMNS, countLoadedTasks } from './task-summary-projection.js';

// Owns only the synchronous task-list DOM projection. Task-card actions,
// pagination requests, freshness guards, and canonical state stay Board-owned
// and enter through explicit callbacks/getters.
export function createBoardTaskListView(dependencies) {
  var t = dependencies.t;
  var el = dependencies.el;
  var getSectionElement = dependencies.getSectionElement;
  var getState = dependencies.getState;
  var createCard = dependencies.createCard;
  var globalMutationBlocked = dependencies.globalMutationBlocked;
  var openBacklogComposer = dependencies.openBacklogComposer;
  var loadMoreTasks = dependencies.loadMoreTasks;

  function renderColumn(folder, items) {
    var column = el('section', {
      class: 'board-column',
      attrs: { 'data-folder': folder }
    });
    var header = el('div', { class: 'board-column__header' });
    header.appendChild(el('h3', {
      class: 'board-column__title',
      text: t('board.column.' + folder)
    }));
    if (folder === 'backlog') {
      var addButton = el('button', {
        type: 'button',
        class: 'btn btn--primary board-column__add',
        text: t('board.create.btn')
      });
      if (globalMutationBlocked()) {
        addButton.disabled = true;
        addButton.setAttribute('title', t('board.integrity.createBlocked'));
      }
      addButton.addEventListener('click', openBacklogComposer);
      header.appendChild(addButton);
    }
    column.appendChild(header);

    var body = el('div', { class: 'board-column__body' });
    if (!items || items.length === 0) {
      body.appendChild(el('p', {
        class: 'board-column__empty',
        text: t('board.columnEmpty.' + folder)
      }));
    } else {
      for (var i = 0; i < items.length; i++) {
        body.appendChild(createCard(folder, items[i]));
      }
    }
    column.appendChild(body);
    return column;
  }

  function renderColumns() {
    var state = getState();
    var columns = state.columns || { backlog: [], pending: [], todo: [], done: [] };
    var wrap = el('div', { class: 'board-columns' });
    // The summary service has already reconciled INDEX rows with the validated
    // canonical task model. Rendering never relocates cards from runtime markers.
    TASK_COLUMNS.forEach(function (folder) {
      wrap.appendChild(renderColumn(folder, columns[folder]));
    });
    getSectionElement().appendChild(wrap);
  }

  function renderPagination() {
    var state = getState();
    if (!state.summary || !state.summary.nextCursor) return;
    var loaded = countLoadedTasks(state.summary.columns);
    var nav = el('div', { class: 'board-pagination' });
    nav.appendChild(el('span', {
      class: 'board-pagination__count',
      text: t('board.filter.loaded', { loaded: loaded, total: state.summary.total })
    }));
    var button = el('button', {
      type: 'button',
      class: 'btn board-pagination__more',
      text: state.loadingMore ? t('board.filter.loadingMore')
        : state.paginationError ? t('board.filter.retryMore') : t('board.filter.loadMore'),
      attrs: { 'data-board-filter': 'loadMore' }
    });
    button.disabled = state.loadingMore;
    if (state.loadingMore) button.setAttribute('aria-busy', 'true');
    button.addEventListener('click', loadMoreTasks);
    nav.appendChild(button);
    getSectionElement().appendChild(nav);
  }

  function render() {
    renderColumns();
    renderPagination();
  }

  return { render: render };
}
