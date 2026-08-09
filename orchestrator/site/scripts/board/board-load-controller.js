import { TASK_COLUMNS } from './task-summary-projection.js';

export function createBoardLoadController(dependencies) {
  var generation = 0;

  function unavailableIntegrity(error) {
    return {
      version: 1,
      ok: false,
      scope: 'all',
      indexStatus: 'unavailable',
      affectedStems: [],
      findings: [{
        code: 'TASK_STATE_UNAVAILABLE',
        severity: 'blocker',
        stem: null,
        paths: [],
        message: dependencies.requestError(error),
        recovery: null
      }]
    };
  }

  function loadedTaskStems(columns) {
    return TASK_COLUMNS.reduce(function (stems, column) {
      return stems.concat((columns[column] || []).map(function (row) { return row.stem; }));
    }, []);
  }

  function load(options) {
    if (!dependencies.isMounted()) return;
    options = options || { closeOpenModal: true };
    if (options.closeOpenModal) dependencies.closeModal();
    var loadGeneration = ++generation;
    var state = dependencies.getState();
    var initial = state.columns == null;
    state.loading = initial;
    state.loadingMore = false;
    state.error = null;
    if (initial) dependencies.render();
    dependencies.loadResults(dependencies.getFilters()).then(function (results) {
      if (loadGeneration !== generation) return;
      var summaryResult = results[0];
      var integrityResult = results[1];
      state.integrity = integrityResult.ok
        ? integrityResult.value
        : unavailableIntegrity(integrityResult.error);
      if (summaryResult.ok) {
        state.summary = summaryResult.value;
        state.columns = summaryResult.value.columns;
        dependencies.reconcileMenus(loadedTaskStems(state.columns));
        state.error = null;
        state.paginationError = null;
      } else {
        state.error = summaryResult.error;
      }
      state.loading = false;
      dependencies.render();
      dependencies.afterLoad();
    });
  }

  return {
    generation: function () { return generation; },
    load: load
  };
}
