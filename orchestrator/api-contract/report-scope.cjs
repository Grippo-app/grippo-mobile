'use strict';

// One path contract for API runtime reports. Control-plane refresh/analyse jobs
// keep their singleton projection, while a task execution writes beneath the
// exact manager-owned worktree/run generation so concurrent candidates cannot
// replace each other's evidence.

var path = require('path');
var worktreeContract = require('../tasks/worktree-record-contract.cjs');

function directory(cacheRoot, execution) {
  var root = path.resolve(cacheRoot);
  if (execution === null || execution === undefined) {
    return path.join(root, 'reports');
  }
  if (!execution || typeof execution !== 'object' ||
      !worktreeContract.WORKTREE_ID_RE.test(String(execution.worktreeId || '')) ||
      !worktreeContract.RUN_ID_RE.test(String(execution.runId || ''))) {
    throw new Error('API_REPORT_EXECUTION_SCOPE_INVALID');
  }
  return path.join(root, 'reports', 'executions', execution.worktreeId, execution.runId);
}

module.exports = { directory: directory };
