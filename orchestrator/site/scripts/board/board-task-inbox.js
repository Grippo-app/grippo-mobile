// Owns the transient pre-Setup Inbox projection, request generation, publish
// lifecycle, and DOM rendering. Durable Inbox authority remains server-side.
export function createBoardTaskInbox(dependencies) {
  var generation = 0;
  var entries = [];
  var loaded = false;
  var loading = false;
  var error = null;

  function rerenderIfActive() {
    if (dependencies.getSectionElement() && dependencies.isBoardActive()) {
      dependencies.rerender();
    }
  }

  function load() {
    var loadGeneration = ++generation;
    loading = true;
    error = null;
    return dependencies.loadEntries().then(function (result) {
      if (loadGeneration !== generation) return;
      entries = result.entries;
      loaded = true;
      loading = false;
      rerenderIfActive();
    }, function (loadError) {
      if (loadGeneration !== generation) return;
      loaded = true;
      loading = false;
      error = loadError;
      rerenderIfActive();
    });
  }

  function publish(entry, button) {
    if (!entry || !entry.id || !dependencies.canPublish()) return;
    button.disabled = true;
    dependencies.publishEntry(entry.id).then(function (result) {
      dependencies.toast(dependencies.t('board.inbox.published', {
        stem: result && result.stem || ''
      }));
      load();
      dependencies.reloadStore();
    }, function (publishError) {
      button.disabled = false;
      dependencies.toast(dependencies.t('board.inbox.publishFailed', {
        detail: dependencies.requestError(publishError)
      }));
    });
  }

  function render(complete) {
    var currentEntries = entries || [];
    if (complete && loaded && !error && currentEntries.length === 0) return;
    var panel = dependencies.el('section', { class: 'board-inbox' });
    var head = dependencies.el('div', { class: 'board-inbox__head' });
    var copy = dependencies.el('div', { class: 'board-inbox__copy' });
    copy.appendChild(dependencies.el('h3', {
      class: 'board-inbox__title', text: dependencies.t('board.inbox.title')
    }));
    copy.appendChild(dependencies.el('p', {
      class: 'board-inbox__description',
      text: dependencies.t(complete
        ? 'board.inbox.readyDescription'
        : 'board.inbox.setupDescription')
    }));
    head.appendChild(copy);
    if (!complete) {
      var saveButton = dependencies.el('button', {
        type: 'button', class: 'btn btn--primary', text: dependencies.t('board.inbox.create')
      });
      saveButton.addEventListener('click', dependencies.openComposer);
      head.appendChild(saveButton);
    }
    panel.appendChild(head);
    if (loading && !loaded) {
      panel.appendChild(dependencies.el('p', {
        class: 'board-inbox__status', text: dependencies.t('board.inbox.loading')
      }));
    } else if (error) {
      var errorRow = dependencies.el('div', {
        class: 'board-inbox__status board-inbox__status--error'
      });
      errorRow.appendChild(dependencies.createTextNode(
        dependencies.t('board.inbox.loadFailed') + ' '));
      var retry = dependencies.el('button', {
        type: 'button', class: 'btn btn--small', text: dependencies.t('common.retry')
      });
      retry.addEventListener('click', load);
      errorRow.appendChild(retry);
      panel.appendChild(errorRow);
    } else if (currentEntries.length === 0) {
      panel.appendChild(dependencies.el('p', {
        class: 'board-inbox__status', text: dependencies.t('board.inbox.empty')
      }));
    } else {
      var list = dependencies.el('ul', { class: 'board-inbox__list' });
      currentEntries.forEach(function (entry) {
        var item = dependencies.el('li', { class: 'board-inbox__item' });
        var meta = dependencies.el('div', { class: 'board-inbox__item-copy' });
        meta.appendChild(dependencies.el('strong', {
          class: 'board-inbox__item-title', text: entry.title
        }));
        meta.appendChild(dependencies.el('span', {
          class: 'board-inbox__item-date',
          text: dependencies.formatTimestamp(entry.createdAt)
        }));
        item.appendChild(meta);
        if (complete) {
          var publishButton = dependencies.el('button', {
            type: 'button', class: 'btn btn--primary',
            text: dependencies.t('board.inbox.publish')
          });
          publishButton.addEventListener('click', function () {
            publish(entry, publishButton);
          });
          item.appendChild(publishButton);
        } else {
          item.appendChild(dependencies.el('span', {
            class: 'board-inbox__waiting', text: dependencies.t('board.inbox.waitingSetup')
          }));
        }
        list.appendChild(item);
      });
      panel.appendChild(list);
    }
    dependencies.getSectionElement().appendChild(panel);
  }

  return {
    load: load,
    render: render
  };
}
