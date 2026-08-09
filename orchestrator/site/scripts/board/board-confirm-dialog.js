// Owns the generic two-action confirmation DOM and dismissal lifecycle. The
// caller retains every domain action through the injected option callbacks.
export function createBoardConfirmDialog(dependencies) {
  function open(options) {
    options = options || {};
    var content = dependencies.el('div', {
      class: 'board-modal__body board-modal__confirm'
    });
    content.appendChild(dependencies.el('h3', {
      class: 'board-modal__title', text: options.title || ''
    }));
    content.appendChild(dependencies.el('p', {
      class: 'board-modal__confirm-message' +
        (options.danger ? ' banner banner--warn' : ''),
      text: options.message || ''
    }));
    var actions = dependencies.el('div', { class: 'board-modal__actions' });
    var confirm = dependencies.el('button', {
      type: 'button',
      class: 'btn ' + (options.danger ? 'btn--danger' : 'btn--primary'),
      text: options.confirmLabel || dependencies.t('board.confirm.confirm')
    });
    confirm.addEventListener('click', function () {
      dependencies.setCancelHandler(null);
      dependencies.closeModal();
      if (typeof options.onConfirm === 'function') options.onConfirm();
    });
    var cancel = dependencies.el('button', {
      type: 'button', class: 'btn', text: dependencies.t('board.confirm.cancel')
    });
    cancel.addEventListener('click', dependencies.closeModal);
    actions.appendChild(confirm);
    actions.appendChild(cancel);
    content.appendChild(actions);
    dependencies.openModal(content);
    // openModal closes the previous modal first; install this handler only
    // after that reset so it belongs to the new confirmation.
    if (typeof options.onCancel === 'function') {
      dependencies.setCancelHandler(options.onCancel);
    }
    dependencies.schedule(function () {
      try { (options.danger ? cancel : confirm).focus(); } catch (error) {}
    }, 0);
  }

  return { open: open };
}
