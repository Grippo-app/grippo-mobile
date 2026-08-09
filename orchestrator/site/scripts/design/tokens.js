import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { clipboard } from '../clipboard.js';
import { designFilters } from './filters.js';
import { designAnalysisBanner } from './analysis-banner.js';

var el = dom.el;
function t(key, params) { return i18n.t(key, params); }
var KNOWN_BLOCKERS = Object.assign(Object.create(null), {
  PROJECT_TOKEN_INVENTORY_INCOMPLETE: 1,
  TOKEN_SOURCE_HEALTH_UNAVAILABLE: 1,
  TOKEN_SOURCE_REFRESH_FAILED: 1
});

function modeSummary(values) {
  var modes = Object.keys(values || {});
  if (!modes.length) return '';
  return modes.map(function (mode) { return mode + ': ' + values[mode]; }).join(' · ');
}
function statusPill(status) {
  return el('span', {
    class: 'design-status design-token-status design-token-status--' + designFilters.cssToken(status),
    text: designFilters.tokenStatusText(status), attrs: { role: 'cell' }
  });
}
function changedSideBadge(side) {
  return el('span', {
    class: 'design-changed-side design-changed-side--' + designFilters.cssToken(side),
    text: designFilters.changedSideText(side), attrs: { role: 'cell' }
  });
}
function classificationLabel(value) {
  if (value === 'unclassified') return t('design.projectOnly.unclassified');
  if (value === 'project-only-intentional') return t('design.projectOnly.intentional');
  if (value === 'superseded') return t('design.projectOnly.superseded');
  return t('design.unknown');
}
function selectionCell(row, context) {
  var cell = el('div', { class: 'design-table__select', attrs: { role: 'cell' } });
  if (row.findingId && !row.openTask) {
    var check = el('input', {
      type: 'checkbox', class: 'choice-input', checked: context.selected.has(row.findingId),
      attrs: {
        'aria-label': t('design.selectFinding', { name: row.name }),
        'data-design-focus': 'token-finding:' + row.findingId
      }
    });
    check.addEventListener('change', function () { context.toggleFinding(row.findingId, check.checked); });
    cell.appendChild(check);
  } else cell.appendChild(el('span', { class: 'design-select-spacer' }));
  return cell;
}
function taskCell(row) {
  var cell = el('div', { class: 'design-table__task', attrs: { role: 'cell' } });
  if (row.openTask) cell.appendChild(el('a', {
    class: 'btn btn--ghost btn--small',
    href: '#board?task=' + encodeURIComponent(row.openTask.stem), text: t('design.openTask')
  }));
  return cell;
}
function appendRows(table, rows, context) {
  rows.forEach(function (row) {
    var line = el('div', { class: 'design-table__row design-table__row--token', attrs: { role: 'row' } });
    line.appendChild(selectionCell(row, context));
    var main = el('div', { class: 'design-table__main', attrs: { role: 'rowheader' } });
    var open = el('button', {
      type: 'button', class: 'design-token-open',
      attrs: {
        'aria-label': t('design.tokens.openDetail', { name: row.name }),
        'data-design-focus': 'token:' + row.id
      }
    });
    open.appendChild(el('strong', { text: row.name }));
    var designValues = modeSummary(row.figmaValues);
    open.appendChild(el('span', {
      text: designFilters.tokenKindText(row.kind) + (designValues ? ' · ' + designValues : '')
    }));
    if (row.statusDetail) open.appendChild(el('span', { text: row.statusDetail }));
    open.addEventListener('click', function () { context.openEntity('token', row.id, open); });
    main.appendChild(open);
    line.appendChild(main);
    line.appendChild(statusPill(row.status));
    line.appendChild(changedSideBadge(row.changedSide));
    var codeValues = modeSummary(row.codeValues);
    line.appendChild(el('span', {
      class: 'design-code-value', attrs: { role: 'cell' },
      text: codeValues || t('design.none')
    }));
    line.appendChild(taskCell(row));
    table.appendChild(line);
  });
}
function appendProjectRows(table, rows, context) {
  rows.forEach(function (row) {
    var line = el('div', { class: 'design-table__row design-table__row--project-token', attrs: { role: 'row' } });
    line.appendChild(selectionCell(row, context));
    var main = el('div', { class: 'design-table__main', attrs: { role: 'rowheader' } });
    var open = el('button', {
      type: 'button', class: 'design-token-open',
      attrs: {
        'aria-label': t('design.tokens.openDetail', { name: row.name }),
        'data-design-focus': 'project-token:' + row.id
      }
    });
    open.appendChild(el('strong', { text: row.name }));
    open.appendChild(el('span', {
      text: [designFilters.tokenKindText(row.kind), row.layer, row.adapterId]
        .filter(Boolean).join(' · ')
    }));
    if (row.sourcePath) open.appendChild(el('span', {
      text: row.sourcePath + (row.sourceSymbol ? ' · ' + row.sourceSymbol : '')
    }));
    open.addEventListener('click', function () { context.openEntity('project-token', row.id, open); });
    main.appendChild(open);
    line.appendChild(main);
    line.appendChild(el('span', {
      class: 'design-status design-project-classification design-project-classification--' + designFilters.cssToken(row.classification),
      text: classificationLabel(row.classification), attrs: { role: 'cell' }
    }));
    line.appendChild(taskCell(row));
    table.appendChild(line);
  });
}
function addPager(root, table, data, context, endpoint, append) {
  if (!data.nextCursor) return;
  var button = el('button', { type: 'button', class: 'btn btn--ghost design-load-more', text: t('design.loadMore') });
  // The caption IS the affordance: writing the failure into it leaves a button
  // labelled with an error sentence and no visible way to retry. Keep the
  // caption, report beside it. The live region is mounted up front so the
  // assistive-technology announcement fires on the text change.
  var failure = el('p', {
    class: 'design-state design-state--error', attrs: { role: 'alert' }, hidden: true
  });
  button.addEventListener('click', function () {
    button.disabled = true; button.textContent = t('design.loadingMore');
    failure.textContent = ''; failure.hidden = true;
    var query = designFilters.query(context.state, data.generationRevision);
    designFilters.request(endpoint + '?' + query + '&cursor=' + encodeURIComponent(data.nextCursor)).then(function (next) {
      if (!context.isCurrent()) return;
      append(table, next.items, context);
      button.remove();
      failure.remove();
      addPager(root, table, next, context, endpoint, append);
    }).catch(function (error) {
      button.disabled = false; button.textContent = t('design.loadMore');
      failure.hidden = false;
      failure.textContent = designFilters.errorMessage(error);
    });
  });
  root.appendChild(button);
  root.appendChild(failure);
}
function scopeToggle(context, projectOnlyTotal) {
  var toggle = el('div', {
    class: 'design-view-toggle design-token-scope',
    attrs: { role: 'group', 'aria-label': t('design.tokens.scopeAria') }
  });
  var projectLabel = t('design.projectOnly.title') +
    (Number.isFinite(projectOnlyTotal) ? ' (' + projectOnlyTotal + ')' : '');
  [
    ['', t('design.tokens.scope.design')],
    ['sources', t('design.tokens.scope.sources')],
    ['project-only', projectLabel]
  ].forEach(function (option) {
    var active = (context.state.scope || '') === option[0];
    var button = el('button', {
      type: 'button', class: 'btn btn--small' + (active ? '' : ' btn--ghost'),
      text: option[1], attrs: { 'aria-pressed': active ? 'true' : 'false' }
    });
    button.addEventListener('click', function () {
      if (!active) context.setState({ scope: option[0] }, true);
    });
    toggle.appendChild(button);
  });
  return toggle;
}
function sourceStatePill(state) {
  return el('span', {
    class: 'design-status design-token-source-state design-token-source-state--' + designFilters.cssToken(state),
    text: designFilters.localizedEnum(
      'design.tokenSource.state.', state, 'design.tokenSource.state.unknown'
    )
  });
}
function sourceContextText(context) {
  return ['theme', 'locale', 'platform', 'state'].filter(function (key) {
    return context && context[key];
  }).map(function (key) { return key + ': ' + context[key]; }).join(' · ');
}
function originLabel(origin) {
  if (!origin) return t('design.unknown');
  if (origin.kind === 'task-screen') {
    return t('design.tokenSource.origin.taskScreen', {
      task: origin.taskStem, screen: origin.screenKey, variant: origin.variantId
    });
  }
  if (origin.kind === 'task-component') {
    return t('design.tokenSource.origin.taskComponent', {
      task: origin.taskStem, component: origin.designComponentId
    });
  }
  return t('design.tokenSource.origin.componentInventory', {
    scope: origin.componentScopeId, node: origin.captureRootNodeId
  });
}
function originLink(origin, source) {
  var label = originLabel(origin);
  if (origin && (origin.kind === 'task-screen' || origin.kind === 'task-component')) {
    return el('a', {
      href: '#board?task=' + encodeURIComponent(origin.taskStem),
      text: label
    });
  }
  if (source.deepLink) return el('a', {
    href: source.deepLink, target: '_blank', rel: 'noopener noreferrer', text: label
  });
  return el('span', { text: label });
}
function mutationId() {
  var bytes = new Uint8Array(16);
  if (window.crypto && window.crypto.getRandomValues) window.crypto.getRandomValues(bytes);
  else {
    var seed = String(Date.now()) + String(Math.random());
    for (var i = 0; i < bytes.length; i++) bytes[i] = seed.charCodeAt(i % seed.length) & 255;
  }
  return 'tsm_' + Array.from(bytes).map(function (value) {
    return value.toString(16).padStart(2, '0');
  }).join('');
}
function sourceMutationDialog(source, data, action, origin, trigger) {
  return new Promise(function (resolve) {
    // True only while the mutate POST is unsettled. Escape during that window
    // would resolve the dialog with null, so mutateSource skips the toast and
    // the refresh and the list keeps showing the source it just retired.
    var mutating = false;
    // The confirmed request is built once, so a retry after a lost response
    // replays the SAME mutationId and bytes. A freshly generated id makes the
    // server treat the replay as a new mutation, which then fails the CAS
    // check the committed first attempt already invalidated.
    var pendingRequest = null;
    var dialog = el('dialog', {
      class: 'design-confirm design-source-confirm',
      attrs: { 'aria-labelledby': 'design-source-confirm-title' }
    });
    dialog.appendChild(el('h3', {
      id: 'design-source-confirm-title',
      text: t('design.tokenSource.confirm.' + action + '.title')
    }));
    dialog.appendChild(el('p', {
      text: t('design.tokenSource.confirm.summary', {
        origins: source.activeOrigins.length,
        tokens: source.affectedTokenCount,
        exclusive: source.exclusivelyAffectedTokenCount
      })
    }));
    var provenance = action === 'detach-origin' ? [origin] :
      source.activeOrigins.length ? source.activeOrigins : source.retainedOrigins;
    var list = el('ul', { class: 'design-source-confirm__origins' });
    provenance.forEach(function (row) {
      list.appendChild(el('li', { text: originLabel(row) }));
    });
    dialog.appendChild(list);
    var reasonLabel = el('label', { class: 'design-source-confirm__reason' });
    reasonLabel.appendChild(el('span', { text: t('design.tokenSource.reason') }));
    var reason = el('textarea', {
      class: 'input', attrs: { maxlength: '500', rows: '3', required: 'required' }
    });
    reasonLabel.appendChild(reason);
    dialog.appendChild(reasonLabel);
    var status = el('p', {
      class: 'design-source-confirm__status',
      attrs: { 'aria-live': 'polite' }
    });
    dialog.appendChild(status);
    var actions = el('div', { class: 'design-dialog-actions' });
    var cancel = el('button', {
      type: 'button', class: 'btn btn--ghost', text: t('design.cancel')
    });
    var confirm = el('button', {
      type: 'button', class: 'btn', text: t('design.tokenSource.confirm.' + action + '.action'),
      disabled: true
    });
    reason.addEventListener('input', function () {
      confirm.disabled = !reason.value.trim();
      // An edited reason is different request bytes. Replaying them under the
      // prepared mutationId is an idempotency conflict, so drop the draft and
      // let the next confirm mint a fresh id.
      pendingRequest = null;
    });
    function close(value) {
      if (dialog.open && dialog.close) dialog.close(); else dialog.removeAttribute('open');
      dialog.remove();
      resolve(value);
      if (trigger && trigger.isConnected && trigger.focus) {
        setTimeout(function () { trigger.focus(); }, 0);
      }
    }
    cancel.addEventListener('click', function () { close(null); });
    dialog.addEventListener('cancel', function (event) {
      event.preventDefault();
      if (mutating) return;
      close(null);
    });
    confirm.addEventListener('click', function () {
      if (!pendingRequest) {
        pendingRequest = {
          mutationId: mutationId(),
          action: action,
          sourceId: source.sourceId,
          expectedGenerationRevision: data.generationRevision,
          expectedSourceIndexHash: data.sourceIndexHash,
          expectedSourceIndexRevision: data.sourceIndexRevision,
          confirmedAffectedTokenCount: source.affectedTokenCount,
          reason: reason.value.trim()
        };
        if (action === 'detach-origin') {
          pendingRequest.origin = origin;
          pendingRequest.confirmedOriginCount = source.activeOrigins.length;
        } else if (action === 'retire-source') {
          pendingRequest.confirmedOriginCount = source.activeOrigins.length;
          pendingRequest.detachOrigins = true;
        }
      }
      cancel.disabled = true;
      confirm.disabled = true;
      reason.disabled = true;
      status.textContent = t('design.tokenSource.mutating');
      mutating = true;
      designFilters.post('/api/design/token-sources/mutate', pendingRequest).then(function (result) {
        mutating = false;
        close(result);
      }).catch(function (error) {
        mutating = false;
        cancel.disabled = false;
        confirm.disabled = false;
        reason.disabled = false;
        status.textContent = designFilters.errorMessage(error);
        reason.focus();
      });
    });
    actions.appendChild(cancel);
    actions.appendChild(confirm);
    dialog.appendChild(actions);
    document.body.appendChild(dialog);
    if (dialog.showModal) dialog.showModal(); else dialog.setAttribute('open', '');
    setTimeout(function () { reason.focus(); }, 0);
  });
}
function mutateSource(source, data, action, origin, trigger, context) {
  sourceMutationDialog(source, data, action, origin, trigger).then(function (result) {
    if (!result) return;
    // 202 is not always "recapture started": a retire/detach that committed but
    // could not close its health record answers 202 with healthRecoveryRequired,
    // and announcing a recapture that never began hid it completely.
    if (result.healthRecoveryRequired === true) {
      clipboard.toastError(t('design.tokenSource.healthRecoveryRequired'));
    } else {
      clipboard.toast(result.status === 202 && action === 'reactivate-source'
        ? t('design.tokenSource.reactivationStarted')
        : t('design.tokenSource.mutationCommitted'));
    }
    if (context.refresh) context.refresh();
  });
}
function sourceOrigins(block, source, data, context) {
  var origins = source.activeOrigins.length ? source.activeOrigins : source.retainedOrigins;
  var list = el('ul', { class: 'design-token-source__origins' });
  origins.forEach(function (origin) {
    var item = el('li');
    item.appendChild(originLink(origin, source));
    if (source.actions.detachOrigin && source.activeOrigins.length) {
      var detach = el('button', {
        type: 'button', class: 'btn btn--ghost btn--small',
        text: t('design.tokenSource.detachOrigin')
      });
      detach.addEventListener('click', function () {
        mutateSource(source, data, 'detach-origin', origin, detach, context);
      });
      item.appendChild(detach);
    }
    list.appendChild(item);
  });
  if (!origins.length) list.appendChild(el('li', { text: t('design.none') }));
  block.appendChild(list);
}
function sourceHistory(source) {
  if (!source.history.length) return null;
  var details = el('details', { class: 'design-token-source__history' });
  details.appendChild(el('summary', {
    text: t('design.tokenSource.history', { count: source.history.length })
  }));
  var list = el('ul');
  source.history.forEach(function (row) {
    var details = [
      designFilters.localizedEnum(
        'design.tokenSource.action.', row.action, 'design.tokenSource.action.unknown'
      ),
      designFilters.localizedEnum(
        'design.tokenSource.mutationState.', row.state, 'design.tokenSource.mutationState.unknown'
      ),
      row.finishedAt || row.preparedAt,
      row.reason
    ];
    if (row.errorCode) details.push(designFilters.errorMessage(row.errorCode));
    list.appendChild(el('li', {
      text: details.join(' · ')
    }));
  });
  details.appendChild(list);
  return details;
}
function appendSourceRows(root, rows, data, context) {
  rows.forEach(function (source) {
    var card = el('article', { class: 'design-token-source' });
    var head = el('div', { class: 'design-token-source__head' });
    var title = el('div');
    title.appendChild(el('strong', { text: source.nodeId }));
    title.appendChild(el('code', { text: source.sourceId }));
    head.appendChild(title);
    head.appendChild(sourceStatePill(source.state));
    card.appendChild(head);
    card.appendChild(el('p', {
      class: 'design-token-source__context',
      text: sourceContextText(source.context)
    }));
    card.appendChild(el('h4', { text: t('design.tokenSource.currentAccepted') }));
    var facts = el('dl', { class: 'design-token-source__facts' });
    [
      [t('design.tokenSource.observations'), source.acceptedBatch.observationCount],
      [t('design.tokenSource.affectedTokens'), source.affectedTokenCount],
      [t('design.tokenSource.captureSequence'), source.acceptedBatch.captureSequence],
      [t('design.tokenSource.batchHash'), source.acceptedBatch.semanticHash]
    ].forEach(function (row) {
      facts.appendChild(el('div', {}, [
        el('dt', { text: row[0] }), el('dd', { text: String(row[1]) })
      ]));
    });
    card.appendChild(facts);
    if (source.acceptedBatch.becameEmpty) card.appendChild(el('p', {
      class: 'design-token-source__warning', attrs: { role: 'note' },
      text: t('design.tokenSource.becameEmpty', {
        previous: source.acceptedBatch.previousObservationCount
      })
    }));
    var health = el('div', { class: 'design-token-source__health' });
    health.appendChild(el('h4', { text: t('design.tokenSource.latestAttempt') }));
    if (!data.sourceHealthAvailable) {
      health.appendChild(el('p', { text: t('design.tokenSource.healthUnavailable') }));
    } else if (!source.health) {
      health.appendChild(el('p', { text: t('design.tokenSource.neverChecked') }));
    } else {
      health.appendChild(el('p', {
        text: source.health.latestSuccess
          ? t('design.tokenSource.lastSuccess', {
            at: source.health.latestSuccess.at,
            sequence: source.health.latestSuccess.captureSequence
          })
          : t('design.tokenSource.noSuccessfulCheck')
      }));
      health.appendChild(el('p', {
        text: source.health.latestFailure
          ? t('design.tokenSource.latestFailure', {
            at: source.health.latestFailure.at,
            code: designFilters.localizedEnum(
              'design.tokenSource.failure.',
              source.health.latestFailure.code,
              'design.tokenSource.failure.unknown'
            )
          })
          : t('design.tokenSource.noFailure')
      }));
    }
    card.appendChild(health);
    if (source.actions.reason) card.appendChild(el('p', {
      class: 'design-token-source__action-reason',
      text: designFilters.localizedEnum(
        'design.tokenSource.actionReason.',
        source.actions.reason,
        'design.tokenSource.actionReason.unknown'
      )
    }));
    card.appendChild(el('h4', { text: t('design.tokenSource.origins') }));
    sourceOrigins(card, source, data, context);
    var buttons = el('div', { class: 'design-token-source__actions' });
    if (source.deepLink) buttons.appendChild(el('a', {
      class: 'btn btn--ghost btn--small', href: source.deepLink,
      target: '_blank', rel: 'noopener noreferrer', text: t('design.tokenSource.openNode')
    }));
    if (source.actions.retry) buttons.appendChild(el('a', {
      class: 'btn btn--ghost btn--small', href: '#figma?sync=tokens',
      text: t('design.tokenSource.retry')
    }));
    if (source.actions.retire) {
      var retire = el('button', {
        type: 'button', class: 'btn btn--ghost btn--small',
        text: t('design.tokenSource.retire')
      });
      retire.addEventListener('click', function () {
        mutateSource(source, data, 'retire-source', null, retire, context);
      });
      buttons.appendChild(retire);
    }
    if (source.actions.reactivate) {
      var reactivate = el('button', {
        type: 'button', class: 'btn btn--small',
        text: t('design.tokenSource.reactivate')
      });
      reactivate.addEventListener('click', function () {
        mutateSource(source, data, 'reactivate-source', null, reactivate, context);
      });
      buttons.appendChild(reactivate);
    }
    card.appendChild(buttons);
    var history = sourceHistory(source);
    if (history) card.appendChild(history);
    root.appendChild(card);
  });
}
function addSourcePager(root, list, data, context) {
  if (!data.nextCursor) return;
  var button = el('button', {
    type: 'button', class: 'btn btn--ghost design-load-more', text: t('design.loadMore')
  });
  // Same rule as the table pager: the failure never overwrites the caption, or
  // the only control that can retry the page stops reading as a control.
  var failure = el('p', {
    class: 'design-state design-state--error', attrs: { role: 'alert' }, hidden: true
  });
  button.addEventListener('click', function () {
    button.disabled = true;
    button.textContent = t('design.loadingMore');
    failure.textContent = '';
    failure.hidden = true;
    var query = designFilters.query(context.state, data.generationRevision);
    designFilters.request('/api/design/token-sources?' + query + '&cursor=' +
      encodeURIComponent(data.nextCursor)).then(function (next) {
      if (!context.isCurrent()) return;
      appendSourceRows(list, next.rows, next, context);
      button.remove();
      failure.remove();
      addSourcePager(root, list, next, context);
    }).catch(function (error) {
      button.disabled = false;
      button.textContent = t('design.loadMore');
      failure.hidden = false;
      failure.textContent = designFilters.errorMessage(error);
    });
  });
  root.appendChild(button);
  root.appendChild(failure);
}
function renderSources(root, context, data) {
  root.replaceChildren();
  root.appendChild(scopeToggle(context));
  root.appendChild(el('div', {
    class: 'design-token-source-banner', attrs: { role: 'note' }
  }, [
    el('strong', { text: t('design.tokenSource.bannerTitle') }),
    el('p', { text: t('design.tokenSource.bannerBody') })
  ]));
  var current = data.currentAccepted || { sourceCount: 0, active: 0, retired: 0 };
  var latest = data.latestAttempt;
  root.appendChild(el('div', { class: 'design-token-source-summary' }, [
    el('strong', { text: t('design.tokenSource.currentAccepted') }),
    el('span', {
      text: t('design.tokenSource.currentSummary', {
        sources: current.sourceCount,
        active: current.active,
        retired: current.retired
      })
    }),
    el('strong', { text: t('design.tokenSource.latestAttempt') }),
    el('span', {
      text: !data.sourceHealthAvailable
        ? t('design.tokenSource.healthUnavailable')
        : latest
          ? t('design.tokenSource.latestAttemptSummary', {
            action: latest.action,
            outcome: latest.outcome,
            finishedAt: latest.finishedAt,
            sources: latest.sourceCount
          })
          : t('design.tokenSource.neverChecked')
    })
  ]));
  designFilters.appendLimitations(root, data.limitations);
  if (!data.rows.length) {
    root.appendChild(el('p', { class: 'design-state', text: t('design.tokenSource.empty') }));
    return;
  }
  var list = el('div', {
    class: 'design-token-sources',
    attrs: { 'aria-label': t('design.tokenSource.listAria') }
  });
  appendSourceRows(list, data.rows, data, context);
  root.appendChild(list);
  addSourcePager(root, list, data, context);
}
function coverageStrip(coverage) {
  var wrap = el('div', { class: 'design-coverage' });
  if (!coverage) {
    wrap.appendChild(el('strong', { text: t('design.tokenCoverage.unknown') }));
    return wrap;
  }
  wrap.appendChild(el('strong', { text: coverage.percent === null
    ? t('design.tokenCoverage.empty', { denominator: coverage.denominator })
    : t('design.tokenCoverage.summary', {
      matched: coverage.matched, denominator: coverage.denominator, percent: coverage.percent
    }) }));
  wrap.appendChild(el('span', { text: t('design.tokenCoverage.breakdown', {
    valueDrift: coverage.valueDrift, unbound: coverage.unbound,
    missingInProject: coverage.missingInProject,
    unsupported: coverage.excludedUnsupported,
    conflicting: coverage.excludedConflicting,
    context: coverage.excludedContext,
    notObserved: coverage.notObserved
  }) }));
  wrap.appendChild(el('span', {
    class: 'design-coverage__aside', text: t('design.tokenCoverage.projectOnly', { count: coverage.projectOnly })
  }));
  if (coverage.partial) wrap.appendChild(el('span', {
    class: 'design-coverage__partial', text: t('design.tokenCoverage.partial')
  }));
  return wrap;
}
function blockersBlock(analysis) {
  var blockers = analysis && Array.isArray(analysis.blockers) ? analysis.blockers : [];
  if (!blockers.length) return null;
  var block = el('div', { class: 'design-token-blockers', attrs: { role: 'note' } });
  block.appendChild(el('strong', { text: t('design.tokenBlockers.title', { count: blockers.length }) }));
  var list = el('ul');
  blockers.slice(0, 32).forEach(function (blocker) {
    var code = blocker && blocker.code;
    var label = code && KNOWN_BLOCKERS[code]
      ? t('design.tokenBlocker.' + code)
      : t('design.tokenBlocker.generic');
    list.appendChild(el('li', { text: label }));
  });
  block.appendChild(list);
  return block;
}
function tableHead(projectScope) {
  var cells = [
    el('span', { class: 'design-table__select', attrs: { role: 'columnheader', 'aria-label': t('design.field.selection') } }),
    el('span', { class: 'design-table__main', attrs: { role: 'columnheader' }, text: t('design.field.token') })
  ];
  if (projectScope) {
    cells.push(el('span', { class: 'design-table__head-status', attrs: { role: 'columnheader' }, text: t('design.field.classification') }));
  } else {
    cells.push(el('span', { class: 'design-table__head-status', attrs: { role: 'columnheader' }, text: t('design.field.status') }));
    cells.push(el('span', { class: 'design-table__head-status', attrs: { role: 'columnheader' }, text: t('design.field.changedSide') }));
    cells.push(el('span', { class: 'design-code-value', attrs: { role: 'columnheader' }, text: t('design.field.codeValue') }));
  }
  cells.push(el('span', { class: 'design-table__task', attrs: { role: 'columnheader' }, text: t('design.field.task') }));
  return el('div', {
    class: 'design-table__row design-table__row--head' + (projectScope ? ' design-table__row--project-token' : ' design-table__row--token'),
    attrs: { role: 'row' }
  }, cells);
}
function render(root, context) {
  var projectScope = context.state.scope === 'project-only';
  var sourcesScope = context.state.scope === 'sources';
  var endpoint = sourcesScope ? '/api/design/token-sources'
    : projectScope ? '/api/design/tokens/project-only' : '/api/design/tokens';
  return designFilters.request(endpoint + '?' + designFilters.query(context.state)).then(function (data) {
    if (!context.isCurrent()) return;
    context.adopt(data);
    if (sourcesScope) {
      if (!context.shouldRender(data)) return;
      renderSources(root, context, data);
      return;
    }
    if (typeof context.adoptTokenAnalysis === 'function') context.adoptTokenAnalysis(data.analysis);
    if (!context.shouldRender(data)) return;
    root.replaceChildren();
    root.appendChild(designAnalysisBanner.element(
      'tokens', data.analysis, context.comparisonState(data.comparison),
      context.startTokenComparison
    ));
    root.appendChild(scopeToggle(context, projectScope ? data.total : data.projectOnlyTotal));
    if (!projectScope) root.appendChild(coverageStrip(data.coverage));
    var blockers = blockersBlock(data.analysis);
    if (blockers) root.appendChild(blockers);
    designFilters.appendLimitations(root, data.limitations);
    if (!data.items.length) {
      root.appendChild(el('p', {
        class: 'design-state',
        text: t(projectScope ? 'design.empty.projectOnly' : 'design.empty.tokens')
      }));
      return;
    }
    var table = el('div', {
      class: 'design-table design-table--tokens',
      attrs: {
        role: 'table', 'aria-label': t('design.tab.tokens'),
        'aria-colcount': projectScope ? '4' : '6'
      }
    });
    table.appendChild(tableHead(projectScope));
    var append = projectScope ? appendProjectRows : appendRows;
    append(table, data.items, context);
    root.appendChild(table);
    addPager(root, table, data, context, endpoint, append);
  });
}

export const designTokens = { render: render };
