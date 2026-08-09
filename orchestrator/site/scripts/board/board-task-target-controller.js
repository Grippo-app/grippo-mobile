import { findTaskInColumns } from './task-summary-projection.js';

var TASK_STEM_PATTERN = /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/;
var TASK_SECTIONS = Object.freeze([
  'overview',
  'activity',
  'artifacts',
  'advanced',
  'questions',
  'dependencies',
  'validation'
]);

export function createBoardTaskTargetController(dependencies) {
  var pending = null;

  function request(stem, section, target) {
    if (typeof stem !== 'string' || !TASK_STEM_PATTERN.test(stem)) return false;
    pending = { stem: stem, section: section, target: target };
    return true;
  }

  function openRequested(clearIfMissing) {
    if (!pending) return null;
    var columns = dependencies.getColumns();
    if (!columns || dependencies.getCurrentPanel() !== 'board') return null;
    var requested = pending;
    var match = findTaskInColumns(columns, requested.stem);
    if (!match) {
      if (clearIfMissing) pending = null;
      return false;
    }
    pending = null;
    dependencies.openTask(
      match.folder,
      requested.stem,
      match.item,
      requested.section || null,
      requested.target
    );
    return true;
  }

  function consumeDeepLink() {
    var raw = String(dependencies.getHash() || '').replace(/^#/, '');
    if (raw.indexOf('board?') !== 0) return;
    var params;
    try { params = new URLSearchParams(raw.slice('board?'.length)); }
    catch (_) { return; }
    var stem = params.get('task');
    var tab = params.get('tab');
    var artifactId = params.get('artifact');
    var checkpointId = params.get('checkpoint');
    if (!stem || !TASK_STEM_PATTERN.test(stem)) return;
    if (TASK_SECTIONS.indexOf(tab) < 0) tab = 'overview';
    if (artifactId && (artifactId.length > 240 || /[\0\r\n]/.test(artifactId))) artifactId = null;
    if (checkpointId && !/^cp-[a-f0-9]{32}$/.test(checkpointId)) checkpointId = null;
    if (artifactId) tab = 'artifacts';
    if (checkpointId) tab = 'advanced';
    request(stem, tab, {
      artifactId: artifactId || null,
      checkpointId: checkpointId || null
    });
    try { dependencies.replaceHash('#board'); } catch (_) {}
  }

  return {
    consumeDeepLink: consumeDeepLink,
    openRequested: openRequested,
    request: request
  };
}
