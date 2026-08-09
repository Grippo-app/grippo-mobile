import { findTaskInColumns } from './task-summary-projection.js';

// Neutralizes an open card after a summary refresh proves that its column or
// action generation is stale. Reads and UI effects are dependency-injected;
// canonical task state remains owned by the server summary.
export function createBoardOpenCardFreshness(dependencies) {
  function neutralizeIfMoved() {
    var openCard = dependencies.getOpenCard();
    if (!openCard || !dependencies.getActiveModal()) return;
    var columns = dependencies.getColumns() || {};
    var column = columns[openCard.folder] || [];
    var present = null;
    for (var i = 0; i < column.length; i++) {
      if (column[i] && column[i].stem === openCard.stem) {
        present = column[i];
        break;
      }
    }
    var inOriginalColumn = !!present;
    var currentLocation = present
      ? { folder: openCard.folder, item: present }
      : findTaskInColumns(columns, openCard.stem);
    if (!present && currentLocation) present = currentLocation.item;
    var sameGeneration = inOriginalColumn && present &&
      (!openCard.sourceRevision || present.sourceRevision === openCard.sourceRevision) &&
      (!openCard.actionRevision || present.primaryAction &&
        present.primaryAction.actionRevision === openCard.actionRevision);
    if (sameGeneration) return;

    dependencies.abortDetails();
    var activeModal = dependencies.getActiveModal();
    var panel = activeModal.querySelector('.board-modal__content');
    if (!panel || panel.querySelector('.board-modal__moved-notice')) return;
    var staleButtons = panel.querySelectorAll(
      '.board-modal__actions button, .board-figma-slot button, ' +
      '.task-details__overflow button'
    );
    for (var buttonIndex = 0; buttonIndex < staleButtons.length; buttonIndex++) {
      if (!staleButtons[buttonIndex].classList.contains('board-modal__close-btn')) {
        staleButtons[buttonIndex].disabled = true;
      }
    }
    var actions = panel.querySelector('.board-modal__actions');
    if (!actions) return;
    var notice = dependencies.el('div', {
      class: 'banner banner--warn board-modal__moved-notice task-details__stale'
    });
    notice.appendChild(dependencies.el('span', {
      text: present ? dependencies.t('taskDetails.stale') : dependencies.t('board.modalMoved')
    }));
    if (present) {
      var reload = dependencies.el('button', {
        type: 'button', class: 'btn btn--sm', text: dependencies.t('taskDetails.reload')
      });
      reload.addEventListener('click', function () {
        var selected = panel.querySelector(
          '[data-task-details-tab][aria-selected="true"]'
        );
        var section = selected && selected.getAttribute('data-task-details-tab') || 'overview';
        dependencies.openDetails(openCard.stem, section, present);
        dependencies.setOpenCard({
          folder: currentLocation.folder,
          stem: present.stem,
          sourceRevision: present.sourceRevision,
          actionRevision: present.primaryAction &&
            present.primaryAction.actionRevision || null
        });
      });
      notice.appendChild(reload);
    }
    actions.parentNode.insertBefore(notice, actions);
  }

  return { neutralizeIfMoved: neutralizeIfMoved };
}
