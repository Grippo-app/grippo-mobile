import { dom } from '../dom.js';
import { clipboard } from '../clipboard.js';

var el = dom.el;

function enumLabel(t, prefix, value) {
  var key = prefix + value;
  var translated = t(key);
  return translated === key ? t('archmap.unknown') : translated;
}

function relationSection(t, titleKey, direction, relation, onSelect, onMore) {
  var section = el('section', {
    class: 'architecture-detail__section',
    attrs: { 'data-relation-direction': direction }
  });
  section.appendChild(el('h4', {
    text: t(titleKey, { count: relation ? relation.total : 0 }),
    attrs: { tabindex: '-1' }
  }));
  if (!relation || !relation.rows.length) {
    section.appendChild(el('p', { class: 'panel-lead', text: t('archmap.none') }));
    return section;
  }
  relation.rows.forEach(function (row) {
    var button = el('button', {
      type: 'button', class: 'architecture-relation',
      text: row.node.name + ' · ' +
        enumLabel(t, 'archmap.relation.', row.edge.kind)
    });
    button.addEventListener('click', function () { onSelect(row.node.id); });
    section.appendChild(button);
  });
  if (relation.nextCursor) {
    var more = el('button', {
      type: 'button', class: 'btn btn--small',
      text: t('archmap.loadMore')
    });
    more.addEventListener('click', function () {
      more.disabled = true;
      onMore(relation.nextCursor);
    });
    section.appendChild(more);
  }
  return section;
}

export function createNodeDrawer(options) {
  var t = options.t;
  var backdrop = el('div', {
    class: 'architecture-drawer-backdrop',
    attrs: { role: 'presentation' }
  });
  var drawer = el('aside', {
    class: 'architecture-drawer',
    attrs: {
      role: 'dialog', 'aria-modal': 'true',
      'aria-label': t('archmap.nodeDetails'),
      tabindex: '-1'
    }
  });
  backdrop.appendChild(drawer);
  var previousFocus = null;
  var previousFocusKey = null;
  var currentId = null;
  var currentData = null;
  var entityRequestGeneration = 0;
  var relationRequestGeneration = { incoming: 0, outgoing: 0 };
  function navigate(id) {
    if (typeof options.onNavigate === 'function') options.onNavigate(id);
    else open(id);
  }
  function close() {
    entityRequestGeneration++;
    relationRequestGeneration.incoming++;
    relationRequestGeneration.outgoing++;
    currentId = null;
    currentData = null;
    backdrop.remove();
    document.body.classList.remove('architecture-drawer-open');
    document.removeEventListener('keydown', onKey);
    options.onClose();
    var returnTarget = previousFocus && previousFocus.isConnected
      ? previousFocus : null;
    if (!returnTarget && previousFocusKey) {
      var candidates = document.querySelectorAll('[data-architecture-control]');
      for (var index = 0; index < candidates.length; index++) {
        if (candidates[index].getAttribute('data-architecture-control') === previousFocusKey) {
          returnTarget = candidates[index];
          break;
        }
      }
    }
    if (!returnTarget) returnTarget = document.querySelector('[data-target="archmap"]');
    if (returnTarget && typeof returnTarget.focus === 'function') returnTarget.focus();
  }
  function onKey(event) {
    if (event.key === 'Escape') { close(); return; }
    if (event.key !== 'Tab') return;
    var focusable = Array.from(drawer.querySelectorAll(
      'button:not([disabled]),a[href],input:not([disabled]),select:not([disabled]),[tabindex]:not([tabindex="-1"])'
    ));
    if (!focusable.length) {
      event.preventDefault();
      drawer.focus();
      return;
    }
    var first = focusable[0], last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }
  backdrop.addEventListener('mousedown', function (event) {
    if (event.target === backdrop) close();
  });
  function renderLoading() {
    drawer.replaceChildren();
    drawer.appendChild(el('p', { class: 'panel-lead', text: t('archmap.loadingDetails') }));
  }
  function renderError(data) {
    drawer.replaceChildren();
    var closeButton = el('button', {
      type: 'button', class: 'architecture-drawer__close',
      text: '×', attrs: { 'aria-label': t('archmap.close') }
    });
    closeButton.addEventListener('click', close);
    drawer.appendChild(closeButton);
    drawer.appendChild(el('div', {
      class: 'banner banner--warn',
      text: data && data.removedInLatestDiff
        ? t('archmap.entityMissingInDiff') : t('archmap.entityMissing')
    }));
    if (data && data.removedInLatestDiff && options.onShowDiff) {
      var diff = el('button', {
        type: 'button', class: 'btn', text: t('archmap.viewLatestDiff')
      });
      diff.addEventListener('click', function () {
        close();
        options.onShowDiff();
      });
      drawer.appendChild(diff);
    }
    closeButton.focus();
  }
  function renderRequestError(retry) {
    drawer.replaceChildren();
    var closeButton = el('button', {
      type: 'button', class: 'architecture-drawer__close',
      text: '×', attrs: { 'aria-label': t('archmap.close') }
    });
    closeButton.addEventListener('click', close);
    drawer.appendChild(closeButton);
    drawer.appendChild(el('div', {
      class: 'banner banner--warn',
      text: t('archmap.detailsUnavailable')
    }));
    var retryButton = el('button', {
      type: 'button', class: 'btn', text: t('archmap.retry')
    });
    retryButton.addEventListener('click', retry);
    drawer.appendChild(retryButton);
    retryButton.focus();
  }
  function render(data) {
    if (!data || !data.present) { renderError(data); return; }
    drawer.replaceChildren();
    var closeButton = el('button', {
      type: 'button', class: 'architecture-drawer__close',
      text: '×', attrs: { 'aria-label': t('archmap.close') }
    });
    closeButton.addEventListener('click', close);
    drawer.appendChild(closeButton);
    drawer.appendChild(el('span', { class: 'architecture-detail__kind', text: t('archmap.kind.' + data.node.kind) }));
    drawer.appendChild(el('h3', { text: data.node.name }));
    drawer.appendChild(el('code', { class: 'architecture-detail__id', text: data.node.id }));
    var badges = el('div', { class: 'archmap-chips architecture-detail__badges' });
    badges.appendChild(el('span', {
      class: 'archmap-chip',
      text: enumLabel(t, 'archmap.platform.', data.node.platform)
    }));
    badges.appendChild(el('span', {
      class: 'archmap-chip',
      text: enumLabel(t, 'archmap.layer.', data.node.layer)
    }));
    drawer.appendChild(badges);
    if (data.node.path) {
      var pathButton = el('button', {
        type: 'button', class: 'archmap-path', text: data.node.path,
        attrs: { title: t('archmap.copyPath') }
      });
      pathButton.addEventListener('click', function () { clipboard.copy(data.node.path); });
      drawer.appendChild(pathButton);
    }
    drawer.appendChild(relationSection(t, 'archmap.usedBy', 'incoming', data.incoming, navigate,
      function (cursor) { loadRelations('incoming', cursor); }));
    drawer.appendChild(relationSection(t, 'archmap.uses', 'outgoing', data.outgoing, navigate,
      function (cursor) { loadRelations('outgoing', cursor); }));
    var relatedRows = (data.incoming.rows || []).concat(data.outgoing.rows || []);
    var related = [];
    relatedRows.forEach(function (row) {
      if (['screen', 'feature', 'api', 'component'].indexOf(row.node.kind) < 0 ||
          related.some(function (item) { return item.id === row.node.id; })) return;
      related.push(row.node);
    });
    if (related.length) {
      var relatedSection = el('section', { class: 'architecture-detail__section' });
      relatedSection.appendChild(el('h4', { text: t('archmap.relatedEntities') }));
      related.forEach(function (node) {
        var button = el('button', {
          type: 'button', class: 'architecture-relation',
          text: t('archmap.kind.' + node.kind) + ' · ' + node.name
        });
        button.addEventListener('click', function () { navigate(node.id); });
        relatedSection.appendChild(button);
      });
      drawer.appendChild(relatedSection);
    }
    if (data.findings && data.findings.length) {
      var findings = el('section', { class: 'architecture-detail__section' });
      findings.appendChild(el('h4', {
        text: t('archmap.relatedFindingsCount', {
          count: data.findingsTotal == null ? data.findings.length : data.findingsTotal
        })
      }));
      data.findings.forEach(function (finding) {
        findings.appendChild(el('p', {
          class: 'architecture-detail__finding',
          text: t('archmap.severity.' + finding.severity) + ' · ' + finding.title
        }));
      });
      if (data.findingsTruncated) {
        findings.appendChild(el('p', {
          class: 'panel-lead',
          text: t('archmap.relatedFindingsTruncated', {
            count: data.findings.length,
            total: data.findingsTotal
          })
        }));
      }
      drawer.appendChild(findings);
    }
    if (data.linkedTasks && data.linkedTasks.length) {
      var tasks = el('section', { class: 'architecture-detail__section' });
      tasks.appendChild(el('h4', {
        text: t('archmap.linkedTasksCount', {
          count: data.linkedTasksTotal == null ? data.linkedTasks.length : data.linkedTasksTotal
        })
      }));
      data.linkedTasks.forEach(function (task) {
        var button = el('button', {
          type: 'button', class: 'architecture-relation',
          text: task.title + ' · ' +
            enumLabel(t, 'board.column.', task.column)
        });
        button.addEventListener('click', function () {
          close();
          options.onOpenTask(task.stem);
        });
        tasks.appendChild(button);
      });
      if (data.linkedTasksTruncated) {
        tasks.appendChild(el('p', {
          class: 'panel-lead',
          text: t('archmap.linkedTasksTruncated', {
            count: data.linkedTasks.length,
            total: data.linkedTasksTotal
          })
        }));
      }
      drawer.appendChild(tasks);
    }
  }
  function renderRelationRequestError(direction, cursor) {
    render(currentData);
    var section = drawer.querySelector(
      '[data-relation-direction="' + direction + '"]'
    );
    if (!section) return;
    var staleMore = section.querySelector('button.btn');
    if (staleMore) staleMore.disabled = true;
    section.appendChild(el('div', {
      class: 'banner banner--warn',
      text: t('archmap.relationsUnavailable')
    }));
    var retryButton = el('button', {
      type: 'button', class: 'btn btn--small', text: t('archmap.retry')
    });
    retryButton.addEventListener('click', function () {
      loadRelations(direction, cursor);
    });
    section.appendChild(retryButton);
    retryButton.focus();
  }
  function loadRelations(direction, cursor) {
    var id = currentId;
    var entityGeneration = entityRequestGeneration;
    var relationGeneration = ++relationRequestGeneration[direction];
    var params = { limit: 100 };
    params[direction + 'Cursor'] = cursor;
    options.load(id, params).then(function (data) {
      if (currentId !== id || entityGeneration !== entityRequestGeneration ||
          relationGeneration !== relationRequestGeneration[direction] ||
          !currentData) return;
      if (!data || typeof data !== 'object') {
        renderRelationRequestError(direction, cursor);
        return;
      }
      if (!data.present) {
        currentData = data;
        render(data);
        return;
      }
      if (data.structuralHash !== currentData.structuralHash) {
        open(id);
        return;
      }
      var merged = currentData[direction].rows.concat(data[direction].rows);
      currentData[direction] = Object.assign({}, data[direction], { rows: merged });
      render(currentData);
      var heading = drawer.querySelector(
        '[data-relation-direction="' + direction + '"] h4'
      );
      if (heading) heading.focus();
    }, function () {
      if (currentId !== id || entityGeneration !== entityRequestGeneration ||
          relationGeneration !== relationRequestGeneration[direction]) return;
      renderRelationRequestError(direction, cursor);
    });
  }
  function open(id) {
    var generation = ++entityRequestGeneration;
    relationRequestGeneration.incoming++;
    relationRequestGeneration.outgoing++;
    currentId = id;
    currentData = null;
    if (!backdrop.isConnected) {
      previousFocus = document.activeElement;
      previousFocusKey = previousFocus && previousFocus.getAttribute
        ? previousFocus.getAttribute('data-architecture-control') : null;
      document.body.appendChild(backdrop);
      document.body.classList.add('architecture-drawer-open');
      document.addEventListener('keydown', onKey);
    }
    renderLoading();
    drawer.focus();
    options.load(id).then(function (data) {
      if (currentId === id && generation === entityRequestGeneration) {
        if (!data || typeof data !== 'object') {
          renderRequestError(function () { open(id); });
          return;
        }
        currentData = data;
        render(data);
        var closeButton = drawer.querySelector('.architecture-drawer__close');
        if (closeButton) closeButton.focus();
      }
    }, function () {
      if (currentId === id && generation === entityRequestGeneration) {
        renderRequestError(function () { open(id); });
      }
    });
  }
  return { open: open, close: close };
}
