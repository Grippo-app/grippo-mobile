// Owns the read-only loading and failure shell for Task Details. Loading,
// mutation, retry, and secondary-action effects remain caller callbacks.
export function createBoardTaskDetailsShell(dependencies) {
  function loading(stem, item) {
    var content = dependencies.el('div', {
      class: 'board-modal__body task-details task-details--loading'
    });
    content.appendChild(dependencies.el('h3', {
      class: 'board-modal__title',
      text: item && item.title || stem
    }));
    content.appendChild(dependencies.el('code', {
      class: 'board-modal__stem', text: stem
    }));
    content.appendChild(dependencies.el('p', {
      class: 'task-details__loading',
      text: dependencies.t('taskDetails.loading')
    }));
    var actions = dependencies.el('div', { class: 'board-modal__actions' });
    actions.appendChild(dependencies.createCloseButton());
    content.appendChild(actions);
    return content;
  }

  function sectionForTarget(target) {
    var section = target && target.section;
    if (section === 'questions' || section === 'validation') return 'action';
    if (section === 'dependencies') return 'overview';
    if (section === 'action') return 'action';
    if (section === 'artifacts') return 'artifacts';
    if (section === 'advanced') return 'advanced';
    return 'overview';
  }

  function showLoadError(options) {
    options = options || {};
    if (dependencies.getActiveModal() !== options.modalToken ||
        !options.loading || !options.loading.isConnected) return;
    var paragraph = options.loading.querySelector('.task-details__loading');
    if (!paragraph) return;
    paragraph.className = 'banner banner--warn';
    paragraph.textContent = dependencies.requestError(options.error);
    paragraph.setAttribute('role', 'alert');
    var retry = dependencies.el('button', {
      type: 'button',
      class: 'btn btn--sm',
      text: dependencies.t('taskDetails.reload')
    });
    retry.addEventListener('click', function () {
      if (typeof options.onRetry === 'function') options.onRetry();
    });
    paragraph.insertAdjacentElement('afterend', retry);
    var secondaryAction = typeof options.getSecondaryAction === 'function'
      ? options.getSecondaryAction()
      : options.secondaryAction;
    if (secondaryAction) {
      var secondary = dependencies.el('button', {
        type: 'button',
        class: secondaryAction.className || 'btn btn--danger btn--sm',
        text: secondaryAction.label || ''
      });
      secondary.addEventListener('click', function () {
        if (typeof secondaryAction.onClick === 'function') {
          secondaryAction.onClick();
        }
      });
      retry.insertAdjacentElement('afterend', secondary);
    }
    retry.focus();
  }

  return {
    loading: loading,
    sectionForTarget: sectionForTarget,
    showLoadError: showLoadError
  };
}
