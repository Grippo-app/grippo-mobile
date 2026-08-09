export function createBoardPaginationController(dependencies) {
  function loadMore() {
    var state = dependencies.getState();
    if (!state.summary || !state.summary.nextCursor || state.loadingMore) return;
    var cursor = state.summary.nextCursor;
    var revision = state.summary.revision;
    var generation = dependencies.getLoadGeneration();
    var filters = Object.assign({}, dependencies.getFilters(), { cursor: cursor });
    state.loadingMore = true;
    state.paginationError = null;
    dependencies.render();
    dependencies.loadSummary(filters).then(function (page) {
      if (generation !== dependencies.getLoadGeneration() ||
          !state.summary || state.summary.revision !== revision) return;
      if (page.revision !== revision) {
        dependencies.reload();
        return;
      }
      state.summary = dependencies.mergeSummaryPage(state.summary, page);
      state.columns = state.summary.columns;
      state.loadingMore = false;
      dependencies.render();
    }, function (error) {
      if (generation !== dependencies.getLoadGeneration()) return;
      state.loadingMore = false;
      state.paginationError = error || { kind: 'fetch-failed' };
      dependencies.render();
    });
  }

  return { loadMore: loadMore };
}
