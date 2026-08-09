// Owns card-primary and overflow action orchestration. Typed task actions,
// recovery policy, navigation, modal, and mutation flows remain injected.
export function createBoardTaskActionController(dependencies) {
  function execute(row, action, button, confirmation, options) {
    options = options || {};
    button.disabled = true;
    button.setAttribute('aria-busy', 'true');
    dependencies.executeAction(row.stem, action, confirmation).then(function (response) {
      if (typeof options.onSuccess === 'function') {
        options.onSuccess(response);
        return;
      }
      // The card is the busiest Run affordance, so it must report what actually
      // happened to the request — queued with no drainer, or against a dead CLI
      // token, is not "started". Same verdict the Details modal already shows.
      dependencies.presentEnqueueResult(response, row.stem);
      Promise.resolve(dependencies.reloadStore()).then(function () {
        dependencies.reloadBoard();
      });
    }, function (error) {
      button.disabled = false;
      button.removeAttribute('aria-busy');
      if (error && (error.kind === 'action-stale' || error.kind === 'task-action-stale')) {
        dependencies.toast(dependencies.t('board.action.stale'));
        dependencies.reloadBoard();
        return;
      }
      dependencies.toast(dependencies.t('board.action.failed', {
        detail: dependencies.requestError(error)
      }));
    });
  }

  function run(row, action, button, options) {
    if (dependencies.startupRecoveryBlocksMutation()) {
      button.disabled = true;
      button.setAttribute('title', dependencies.t('board.startupRecovery.actionBlocked'));
      dependencies.toast(dependencies.t('board.startupRecovery.actionBlocked'));
      return;
    }
    if (action.kind === 'run' && dependencies.needsUnpulledScreens(row.stem)) {
      dependencies.confirmScreensBeforeRun(row.stem).then(function (allowed) {
        if (allowed) execute(row, action, button, null, options);
      });
      return;
    }
    if (action.requiresConfirmation) {
      dependencies.confirm({
        title: dependencies.t('board.action.confirmTitle'),
        message: dependencies.t('board.action.confirmBody', { stem: row.stem }),
        confirmLabel: dependencies.t('board.action.confirm'),
        onConfirm: function () { execute(row, action, button, true, options); }
      });
      return;
    }
    execute(row, action, button, null, options);
  }

  function overflow(kind, row, serverAction) {
    if (kind === 'copy-id') {
      dependencies.copyText(row.stem);
      return;
    }
    if (kind === 'source') {
      dependencies.openSource(row.sourceTarget, row);
      return;
    }
    if (dependencies.startupRecoveryBlocksMutation() &&
        (kind === 'edit' || kind === 'reopen')) {
      dependencies.toast(dependencies.t('board.startupRecovery.actionBlocked'));
      return;
    }
    if (kind === 'edit') {
      dependencies.openEdit(row.stem, row);
      return;
    }
    if (kind === 'drop' && serverAction) {
      dependencies.runDrop(row.stem, row.state, serverAction, {
        onCancel: function () { dependencies.restoreOverflowFocus(row.stem); }
      });
      return;
    }
    if (kind === 'reopen' && serverAction) {
      dependencies.runReopen(row.stem, serverAction);
      return;
    }
    if (kind === 'copy-prompt') {
      dependencies.toast(dependencies.t('board.overflow.preparing_prompt'));
      dependencies.loadPrompt(row.stem, serverAction).then(function (response) {
        if (!response || response.manualFallback !== true || typeof response.text !== 'string') {
          dependencies.toast(dependencies.t('board.action.failed', {
            detail: dependencies.requestError('invalid-response')
          }));
          return;
        }
        dependencies.copyText(response.text);
      }, function (error) {
        if (error && (error.kind === 'action-stale' || error.kind === 'task-action-stale')) {
          dependencies.toast(dependencies.t('board.action.stale'));
          dependencies.reloadBoard();
          return;
        }
        dependencies.toast(dependencies.t('board.action.failed', {
          detail: dependencies.requestError(error)
        }));
      });
      return;
    }
    dependencies.toast(dependencies.t('board.action.targetUnavailable'));
  }

  return {
    overflow: overflow,
    run: run
  };
}
