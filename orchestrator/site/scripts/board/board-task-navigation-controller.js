import { findTaskInColumns } from './task-summary-projection.js';

// Owns Board target interpretation. Navigation effects remain injected so this
// module cannot acquire terminal, router, modal, or task mutation authority.
export function createBoardTaskNavigationController(dependencies) {
  function openTarget(target, row) {
    if (!target || typeof target !== 'object') {
      dependencies.openCard(row.state, row.stem, row);
      return;
    }
    if (target.type === 'terminal' && target.key) {
      dependencies.openTerminal(target.key);
      return;
    }
    if (target.type === 'panel' && target.panel) {
      if (!dependencies.openPanel({
        panel: target.panel,
        entityId: target.entityId || null
      })) dependencies.targetUnavailable();
      return;
    }
    if (target.type === 'task' && target.stem) {
      var found = findTaskInColumns(dependencies.getColumns(), target.stem);
      if (found) {
        dependencies.openCard(
          found.folder,
          found.item.stem,
          found.item,
          target.section || null
        );
      } else {
        dependencies.targetUnavailable();
      }
      return;
    }
    dependencies.openCard(row.state, row.stem, row);
  }

  function openSource(target, row) {
    if (!target || target.availability !== 'available') {
      dependencies.targetUnavailable();
      return;
    }
    if (target.panel === 'board') {
      openTarget({ type: 'task', stem: target.entityId }, row);
      return;
    }
    if (!dependencies.openPanel(target)) dependencies.targetUnavailable();
  }

  return {
    openSource: openSource,
    openTarget: openTarget
  };
}
