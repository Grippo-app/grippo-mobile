// Owns the bounded finalization projection and recovery modal. Marker
// validation, recovery admission, and mutations remain server-authoritative.
export function createBoardFinalizationController(dependencies) {
  function list() {
    var snapshot = dependencies.getSnapshot();
    return snapshot && snapshot.progress && Array.isArray(snapshot.progress.finalizations)
      ? snapshot.progress.finalizations : [];
  }

  function find(stem) {
    var finalizations = list();
    for (var i = 0; i < finalizations.length; i++) {
      if (finalizations[i] && finalizations[i].stem === stem) return finalizations[i];
    }
    return null;
  }

  function open(finalization) {
    var content = dependencies.el('div', { class: 'board-modal__body' });
    content.appendChild(dependencies.el('h3', {
      class: 'board-modal__title', text: dependencies.t('board.finalization.title')
    }));
    content.appendChild(dependencies.el('code', {
      class: 'board-modal__stem', text: (finalization.stem || '') + '.md'
    }));
    content.appendChild(dependencies.el('p', {
      class: 'banner ' + (finalization.status === 'corrupt'
        ? 'banner--danger' : 'banner--warn'),
      text: finalization.status === 'corrupt'
        ? dependencies.t('board.finalization.corrupt')
        : dependencies.t('board.finalization.explain')
    }));

    var details = dependencies.el('div', { class: 'board-finalization__details' });
    details.appendChild(dependencies.el('p', {
      text: dependencies.t('board.finalization.phase', {
        phase: finalization.phase || '—'
      })
    }));
    details.appendChild(dependencies.el('p', {
      text: dependencies.t('board.finalization.column', {
        column: finalization.observedColumn || '—'
      })
    }));
    if (finalization.errorCode) {
      details.appendChild(dependencies.el('p', {
        class: 'board-finalization__error',
        text: dependencies.requestError({ kind: finalization.errorCode })
      }));
    }
    content.appendChild(details);

    var fallback = dependencies.el('div', { class: 'board-finalization__fallback' });
    var safeCliStem = /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/.test(finalization.stem || '') &&
      finalization.status !== 'corrupt' && finalization.recoverable === true;
    if (safeCliStem) {
      fallback.appendChild(dependencies.el('p', {
        text: dependencies.t('board.finalization.cli')
      }));
      fallback.appendChild(dependencies.el('code', {
        text: 'node orchestrator/tasks/finalize-task.mjs ' + finalization.stem
      }));
    } else {
      fallback.appendChild(dependencies.el('p', {
        text: dependencies.t('board.finalization.inspect')
      }));
    }
    content.appendChild(fallback);

    var message = dependencies.el('p', { class: 'board-finalization__message' });
    content.appendChild(message);
    var actions = dependencies.el('div', { class: 'board-modal__actions' });
    var resume = dependencies.el('button', {
      type: 'button',
      class: 'btn btn--primary',
      text: finalization.recoveryRunning
        ? dependencies.t('board.finalization.resuming')
        : dependencies.t('board.finalization.resume')
    });
    resume.disabled = finalization.status === 'corrupt' ||
      finalization.recoveryRunning || !finalization.recoverable;
    resume.addEventListener('click', function () {
      resume.disabled = true;
      resume.textContent = dependencies.t('board.finalization.resuming');
      message.textContent = '';
      dependencies.resume(finalization).then(function () {
        dependencies.modal.close();
        dependencies.toast(dependencies.t('board.finalization.started'));
        dependencies.reloadStore();
      }, function (resumeError) {
        message.textContent = dependencies.requestError(resumeError);
        resume.disabled = false;
        resume.textContent = dependencies.t('board.finalization.resume');
      });
    });
    actions.appendChild(resume);
    actions.appendChild(dependencies.modal.createCloseButton());
    content.appendChild(actions);
    dependencies.modal.open(content);
    // modal.open closes any previous modal first, so assign the live identity
    // only after it has completed its reset.
    dependencies.setOpenStem(finalization.stem || null);
  }

  function refreshOpen() {
    var stem = dependencies.getOpenStem();
    if (!dependencies.hasActiveModal() || !stem) return;
    var fresh = find(stem);
    dependencies.modal.close();
    if (fresh) open(fresh);
  }

  return {
    list: list,
    open: open,
    refreshOpen: refreshOpen
  };
}
