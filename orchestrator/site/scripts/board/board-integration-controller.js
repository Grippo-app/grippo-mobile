// Owns the Integrate modal: the read-only preview of what the transaction
// would publish, the exact blocking paths, and the single confirm that drives
// the server-side write-ahead log. Every verdict here is the server's — the
// browser never decides whether an integration is admissible, and it never
// offers to stash, commit or revert the owner's own changes.
export function createBoardIntegrationController(dependencies) {
  function list() {
    var snapshot = dependencies.getSnapshot();
    return snapshot && snapshot.progress && Array.isArray(snapshot.progress.integrations)
      ? snapshot.progress.integrations : [];
  }

  function find(stem) {
    var records = list();
    for (var i = 0; i < records.length; i++) {
      if (records[i] && records[i].stem === stem) return records[i];
    }
    return null;
  }

  function blockerLine(blocker) {
    var line = dependencies.el('li', { class: 'board-integration__blocker' });
    line.appendChild(dependencies.el('span', { text: blocker.message || blocker.code || '' }));
    if (Array.isArray(blocker.paths) && blocker.paths.length) {
      var paths = dependencies.el('ul', { class: 'board-integration__paths' });
      blocker.paths.forEach(function (entry) {
        paths.appendChild(dependencies.el('li', {}, [dependencies.el('code', { text: entry })]));
      });
      line.appendChild(paths);
    }
    return line;
  }

  // Renders the escape hatch for a state the transaction cannot leave. `kind`
  // is 'release' (drop a generation nothing will ever integrate) or 'abandon'
  // (end a transaction that stopped in an unresolvable state). Neither action
  // repairs, reverts or deletes evidence — they record a human's decision.
  function offerEscape(button, message, kind, stem, integrationId) {
    if (kind === 'abandon' && !integrationId) return;
    button.hidden = false;
    button.textContent = dependencies.t('board.integration.' + kind);
    button.addEventListener('click', function () {
      button.disabled = true;
      message.textContent = '';
      var call = kind === 'abandon'
        ? dependencies.abandon(stem, integrationId)
        : dependencies.release(stem);
      call.then(function () {
        dependencies.modal.close();
        dependencies.toast(dependencies.t('board.integration.' + kind + 'Done'));
        dependencies.reloadStore();
      }, function (error) {
        message.textContent = dependencies.t('board.integration.' + kind + 'Failed', {
          detail: dependencies.requestError(error)
        });
        button.disabled = false;
      });
    });
  }

  function open(stem) {
    // The server preview distinguishes live WAL state from completed history.
    // Keep that verdict for the confirm click: the raw progress list retains
    // completed records, so mere record presence cannot select /resume for a
    // reopened task's new sealed generation.
    var resumeRequested = false;
    var content = dependencies.el('div', { class: 'board-modal__body' });
    content.appendChild(dependencies.el('h3', {
      class: 'board-modal__title', text: dependencies.t('board.integration.title')
    }));
    content.appendChild(dependencies.el('code', { class: 'board-modal__stem', text: stem + '.md' }));
    var body = dependencies.el('div', { class: 'board-integration__body' });
    content.appendChild(body);
    var message = dependencies.el('p', { class: 'board-integration__message' });
    content.appendChild(message);
    var actions = dependencies.el('div', { class: 'board-modal__actions' });
    var confirm = dependencies.el('button', { type: 'button', class: 'btn btn--primary' });
    confirm.disabled = true;
    actions.appendChild(confirm);
    // The two exits from a state the transaction cannot leave by itself. Both
    // are hidden until the server's own verdict says they apply, and both are
    // destructive-sounding on purpose: neither repairs anything.
    var escape = dependencies.el('button', { type: 'button', class: 'btn btn--danger' });
    escape.hidden = true;
    actions.appendChild(escape);
    actions.appendChild(dependencies.modal.createCloseButton());
    content.appendChild(actions);
    dependencies.modal.open(content);
    dependencies.setOpenStem(stem);

    dependencies.preview(stem).then(function (preview) {
      body.replaceChildren();
      var resuming = preview.state === 'in-flight';
      resumeRequested = resuming;
      confirm.textContent = dependencies.t(resuming ? 'board.integration.resume' : 'board.integration.confirm');
      if (preview.state === 'revalidation-required') {
        body.appendChild(dependencies.el('p', {
          class: 'banner banner--warn', text: dependencies.t('board.integration.revalidate')
        }));
        offerEscape(escape, message, 'release', stem, null);
      } else if (preview.state === 'recovery-required') {
        body.appendChild(dependencies.el('p', {
          class: 'banner banner--danger', text: dependencies.t('board.integration.recovery')
        }));
        offerEscape(escape, message, 'abandon', stem,
          preview.integration && preview.integration.integrationId);
      } else if (preview.state === 'blocked') {
        body.appendChild(dependencies.el('p', {
          class: 'banner banner--warn', text: dependencies.t('board.integration.blocked')
        }));
        var blockers = dependencies.el('ul', { class: 'board-integration__blockers' });
        (preview.blockers || []).forEach(function (entry) { blockers.appendChild(blockerLine(entry)); });
        body.appendChild(blockers);
        // A generation that will never be integrable — its candidate is not
        // sealed, or its target moved — has no other way out of the queue.
        if ((preview.blockers || []).some(function (entry) {
          return entry && (entry.code === 'candidate-not-sealed' || entry.code === 'target-drifted');
        })) offerEscape(escape, message, 'release', stem, null);
      } else {
        body.appendChild(dependencies.el('p', {
          class: 'banner banner--info', text: dependencies.t('board.integration.explain')
        }));
        confirm.disabled = false;
      }
      if (preview.integration) {
        body.appendChild(dependencies.el('p', {
          text: dependencies.t('board.integration.phase', { phase: preview.integration.phase || '—' })
        }));
        if (preview.integration.status === 'active') confirm.disabled = false;
      }
      if (preview.candidate) {
        body.appendChild(dependencies.el('p', {
          text: dependencies.t('board.integration.diff', {
            count: String(preview.candidate.entries.length),
            base: String(preview.candidate.baseCommit || '').slice(0, 12)
          })
        }));
        var files = dependencies.el('ul', { class: 'board-integration__files' });
        preview.candidate.entries.slice(0, 200).forEach(function (entry) {
          files.appendChild(dependencies.el('li', {}, [
            dependencies.el('span', { class: 'board-integration__op', text: entry.operation }),
            dependencies.el('code', { text: entry.path })
          ]));
        });
        body.appendChild(files);
        body.appendChild(dependencies.el('p', {
          class: 'board-integration__note', text: dependencies.t('board.integration.dirtyExcluded')
        }));
      }
    }, function (previewError) {
      body.replaceChildren();
      message.textContent = dependencies.requestError(previewError);
    });

    confirm.addEventListener('click', function () {
      var resuming = resumeRequested;
      confirm.disabled = true;
      confirm.textContent = dependencies.t('board.integration.running');
      message.textContent = '';
      dependencies.run(stem, resuming).then(function (response) {
        dependencies.modal.close();
        dependencies.toast(dependencies.t('board.integration.started', {
          commit: String((response && response.commit) || '').slice(0, 12) || '—'
        }));
        dependencies.reloadStore();
      }, function (runError) {
        message.textContent = dependencies.t('board.integration.failed', {
          detail: dependencies.requestError(runError)
        });
        confirm.disabled = false;
        confirm.textContent = dependencies.t(resuming ? 'board.integration.resume' : 'board.integration.confirm');
      });
    });
  }

  function refreshOpen() {
    var stem = dependencies.getOpenStem();
    if (!dependencies.hasActiveModal() || !stem) return;
    dependencies.modal.close();
    open(stem);
  }

  return { list: list, find: find, open: open, refreshOpen: refreshOpen };
}
