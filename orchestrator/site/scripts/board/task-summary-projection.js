export const TASK_COLUMNS = Object.freeze(['backlog', 'pending', 'todo', 'done']);

// Keep the server-backed Task Summary's canonical column order while projecting
// its read model. Pagination freshness, mutations, and UI state remain Board-owned.

export function findTaskInColumns(columns, stem) {
  var summaryColumns = columns || {};
  for (var i = 0; i < TASK_COLUMNS.length; i++) {
    var column = TASK_COLUMNS[i];
    var rows = Array.isArray(summaryColumns[column]) ? summaryColumns[column] : [];
    for (var j = 0; j < rows.length; j++) {
      if (rows[j] && rows[j].stem === stem) return { folder: column, item: rows[j] };
    }
  }
  return null;
}

export function mergeTaskSummaryPage(current, page) {
  var columns = { backlog: [], pending: [], todo: [], done: [] };
  TASK_COLUMNS.forEach(function (column) {
    columns[column] = (current.columns[column] || []).concat(page.columns[column] || []);
  });
  return Object.assign({}, page, { columns: columns });
}

export function countLoadedTasks(columns) {
  return TASK_COLUMNS.reduce(function (count, column) {
    return count + (columns[column] || []).length;
  }, 0);
}
