// Owns the presentation-only Figma Screens pull and pre-Run choice. Session,
// modal, connectivity, and task classification authorities remain injected.
export function createBoardFigmaScreensController(dependencies) {
  function sessionKey(stem) {
    return 'figma:screens:' + stem;
  }

  function isPullReady() {
    return dependencies.isConnected();
  }

  function triggerPull(stem) {
    if (!isPullReady()) {
      dependencies.toast(dependencies.t('board.screensWarn.pullUnavailable'));
      return;
    }
    var key = sessionKey(stem);
    var action = dependencies.sessionAction(key, dependencies.screenPullAction);
    Promise.resolve(action).then(function (response) {
      if (response && response.sent === false) {
        dependencies.toast(dependencies.t('run.busy'));
      }
      dependencies.openTerminal(key);
    }, function (error) {
      dependencies.toast(dependencies.t('board.screensWarn.pullFailed', {
        detail: dependencies.actionError(error)
      }));
    });
  }

  function confirmBeforeRun(stem) {
    return new Promise(function (resolve) {
      if (!dependencies.needsUnpulledScreens(stem)) {
        resolve(true);
        return;
      }
      var settled = false;
      function settle(value) {
        if (settled) return;
        settled = true;
        resolve(value);
      }

      var content = dependencies.el('div', {
        class: 'board-modal__body board-modal__confirm'
      });
      content.appendChild(dependencies.el('h3', {
        class: 'board-modal__title',
        text: dependencies.t('board.screensWarn.title')
      }));
      content.appendChild(dependencies.el('p', {
        class: 'board-modal__confirm-message',
        text: dependencies.t('board.screensWarn.body')
      }));

      var actions = dependencies.el('div', { class: 'board-modal__actions' });
      var pull = dependencies.el('button', {
        type: 'button',
        class: 'btn btn--primary',
        text: dependencies.t('board.screensWarn.pull')
      });
      pull.disabled = !isPullReady();
      if (pull.disabled) {
        pull.setAttribute('title', dependencies.t('board.screensWarn.pullUnavailable'));
      }
      pull.addEventListener('click', function () {
        dependencies.setCancelHandler(null);
        dependencies.closeModal();
        triggerPull(stem);
        settle(false);
      });

      var runAnyway = dependencies.el('button', {
        type: 'button',
        class: 'btn',
        text: dependencies.t('board.screensWarn.runAnyway')
      });
      runAnyway.addEventListener('click', function () {
        dependencies.setCancelHandler(null);
        dependencies.closeModal();
        settle(true);
      });

      var cancel = dependencies.el('button', {
        type: 'button',
        class: 'btn',
        text: dependencies.t('board.confirm.cancel')
      });
      cancel.addEventListener('click', dependencies.closeModal);
      actions.appendChild(pull);
      actions.appendChild(runAnyway);
      actions.appendChild(cancel);
      content.appendChild(actions);

      dependencies.openModal(content);
      dependencies.setCancelHandler(function () { settle(false); });
      dependencies.schedule(function () {
        try { pull.focus(); } catch (error) {}
      }, 0);
    });
  }

  return {
    confirmBeforeRun: confirmBeforeRun,
    isPullReady: isPullReady,
    sessionKey: sessionKey,
    triggerPull: triggerPull
  };
}
