'use strict';

// Single current contract for committed post-ship drift markers. The sweep
// writes this shape; verify-done and the Site read exactly this shape. Unknown
// fields and malformed current markers are integrity failures, never absence.

const taskSource = require('../../tasks/task-source-contract.cjs');

const MAX_BYTES = 1024 * 1024;
const MAX_ROWS = 10000;
const MAX_CHANGES = 30;
const MAX_CHANGE_LENGTH = 500;
const ISO_RE = /^\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:\.\d{3})?Z$/;

function exactKeys(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}

function validScreen(value) {
  return typeof value === 'string' && value.length > 0 && value.length <= 200 &&
    value !== '.' && value !== '..' && value.indexOf('/') < 0 && value.indexOf('\\') < 0 &&
    !/[\x00-\x1f\x7f]/.test(value);
}

function validMarker(value, expectedStem) {
  if (!exactKeys(value, ['version', 'taskStem', 'staleAt', 'baselineRunId', 'driftedCount', 'driftedScreens']) ||
      value.version !== 1 || !taskSource.safeTaskStem(value.taskStem) ||
      expectedStem !== undefined && value.taskStem !== expectedStem ||
      !ISO_RE.test(value.staleAt) || typeof value.baselineRunId !== 'string' ||
      value.baselineRunId.length < 1 || value.baselineRunId.length > 4096 ||
      /[\x00-\x1f\x7f]/.test(value.baselineRunId) ||
      !Number.isSafeInteger(value.driftedCount) || value.driftedCount < 1 ||
      !Array.isArray(value.driftedScreens) || value.driftedScreens.length !== value.driftedCount ||
      value.driftedScreens.length > MAX_ROWS) return false;
  return value.driftedScreens.every(function (row) {
    return exactKeys(row, ['screen', 'theme', 'changes']) && validScreen(row.screen) &&
      (row.theme === 'primary' || row.theme === 'dark') && Array.isArray(row.changes) &&
      row.changes.length > 0 && row.changes.length <= MAX_CHANGES &&
      row.changes.every(function (change) {
        return typeof change === 'string' && change.length > 0 &&
          change.length <= MAX_CHANGE_LENGTH && !/[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]/.test(change);
      });
  });
}

function marker(input) {
  var value = {
    version: 1,
    taskStem: input.taskStem,
    staleAt: input.staleAt,
    baselineRunId: input.baselineRunId,
    driftedCount: Array.isArray(input.driftedScreens) ? input.driftedScreens.length : 0,
    driftedScreens: Array.isArray(input.driftedScreens) ? input.driftedScreens.map(function (row) {
      return {
        screen: row.screen,
        theme: row.theme,
        changes: Array.isArray(row.changes) ? row.changes.slice(0, MAX_CHANGES).map(function (change) {
          return String(change).slice(0, MAX_CHANGE_LENGTH);
        }) : []
      };
    }) : []
  };
  if (!validMarker(value, input.taskStem)) throw new Error('ship-drift-marker-invalid');
  return value;
}

module.exports = {
  MAX_BYTES: MAX_BYTES,
  validMarker: validMarker,
  marker: marker
};
