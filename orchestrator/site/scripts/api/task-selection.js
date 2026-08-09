import { dom } from '../dom.js';
import { clipboard } from '../clipboard.js';
import { confirmDialog } from '../ui-dialog.js';

var el = dom.el;
var inFlightMode = null;
var lastResult = null;
var selectionInvalidated = false;

function cancel(ctx, previewId) {
  return ctx.post('/api/api/tasks/cancel', {
    previewId: previewId
  }).then(function () { return true; }, function (error) {
    // Releasing the preview stays best-effort, but a preview left held on the
    // server comes back later as an unrelated "already being applied" refusal
    // with nothing pointing at this action, so name the cause while it is one.
    clipboard.toastError(ctx.errorMessage(error));
    return false;
  });
}

// The preview is a structured report, not a sentence: it renders as grouped rows
// in the site's own dialog. A native confirm() truncated it, ignored the theme,
// and could not indent a package's sources under it.
function previewSummary(ctx, held) {
  var counts = held.counts || {};
  return ctx.t(held.mode === 'hotfix'
    ? 'api.selection.confirmHotfix'
    : 'api.selection.confirmPackage', {
    tasks: counts.tasksCreate || 0,
    sources: counts.sourcesCreate || 0,
    existing: counts.sourcesExisting || 0,
    blocked: counts.blocked || 0,
    skipped: counts.skipped || 0
  });
}

function previewLines(ctx, held) {
  var lines = [];
  function group(text) { lines.push({ text: text, level: 0 }); }
  function child(text) { lines.push({ text: text, level: 1 }); }
  if ((held.actions || []).length) {
    group(ctx.t('api.selection.previewPackages'));
    held.actions.forEach(function (action) {
      child(action.title + ' · ' + ctx.t('api.selection.sources', { count: action.sourceCount }));
      (action.sources || []).slice(0, 4).forEach(function (row) {
        child('↳ ' + (row.title || row.sourceId));
      });
      if ((action.sources || []).length > 4) {
        child('↳ ' + ctx.t('api.selection.moreSources', { count: action.sources.length - 4 }));
      }
    });
  }
  if ((held.existing || []).length) {
    group(ctx.t('api.selection.previewExisting'));
    held.existing.slice(0, 20).forEach(function (row) { child(row.title || row.sourceId); });
  }
  if ((held.blocked || []).length) {
    group(ctx.t('api.selection.previewBlocked'));
    held.blocked.slice(0, 20).forEach(function (row) {
      child(row.item && row.item.title || row.sourceId);
    });
  }
  if ((held.skipped || []).length) {
    group(ctx.t('api.selection.previewSkipped'));
    held.skipped.slice(0, 20).forEach(function (row) {
      child(row.sourceId + ' · ' + ctx.errorMessage(row.reason));
    });
  }
  return lines;
}

function unsuccessfulSources(result) {
  var values = new Set();
  var rows = result && result.result || {};
  (rows.blocked || []).concat(rows.skipped || []).forEach(function (row) {
    if (row && row.sourceId) values.add(row.sourceId);
  });
  (rows.failed || []).forEach(function (row) {
    (row.sourceIds || []).forEach(function (sourceId) {
      values.add(sourceId);
    });
  });
  return values;
}

function preview(ctx, mode) {
  if (inFlightMode || !ctx.selected.size || !ctx.meta ||
      !ctx.meta.committedGenerationId ||
      mode === 'hotfix' && ctx.selected.size !== 1) return;
  var selectedAtStart = Array.from(ctx.selected);
  inFlightMode = mode;
  lastResult = null;
  // The invalidation line describes a selection the user has since rebuilt and
  // is now acting on, so it must not outlive the batch it preceded.
  selectionInvalidated = false;
  apiTaskSelection.render(document.querySelector('.api-batch-host'), ctx);
  ctx.post('/api/api/tasks/preview', {
    expectedGenerationId: ctx.meta.committedGenerationId,
    expectedReportHashes: ctx.meta.reportHashes,
    expectedTaskIndexRevision: ctx.meta.taskIndexRevision,
    mode: mode,
    sourceIds: selectedAtStart
  }).then(function (result) {
    var held = result.preview;
    if (!(held.actions || []).length) {
      return cancel(ctx, held.id).then(function (released) {
        // A release failure already reported itself; replacing it one microtask
        // later with the no-op summary would hide the only copy of that reason.
        if (!released) return null;
        clipboard.toast(ctx.t(
          held.blocked && held.blocked.length
            ? 'api.selection.hotfixRequired'
            : 'api.selection.nothingToCreate'
        ));
        return null;
      });
    }
    return confirmDialog({
      title: ctx.t('api.selection.createPackage'),
      message: previewSummary(ctx, held),
      lines: previewLines(ctx, held),
      confirmLabel: ctx.t('api.selection.createPackage')
    }).then(function (accepted) {
      if (!accepted) return cancel(ctx, held.id).then(function () { return null; });
      return ctx.post('/api/api/tasks/create', {
        previewId: held.id,
        expectedGenerationId: ctx.meta.committedGenerationId,
        expectedItemSetHash: held.itemSetHash,
        expectedReportHashes: ctx.meta.reportHashes,
        expectedTaskIndexRevision: held.taskIndexRevision
      });
    });
  }).then(function (result) {
    if (!result) return;
    var keep = unsuccessfulSources(result);
    selectedAtStart.forEach(function (sourceId) {
      if (!keep.has(sourceId)) ctx.selected.delete(sourceId);
    });
    lastResult = result;
    clipboard.toast(ctx.t('api.selection.result', result.counts));
    return ctx.refresh();
  }).catch(function (error) {
    // A failed multi-task write must leave the same trace a successful one
    // does: nothing was created, every selected source failed, and the box
    // reports which ones through the issues path instead of a toast that goes
    // away. The list stays bounded like every other row set in this box.
    lastResult = {
      counts: {
        tasksCreated: 0,
        tasksExisting: 0,
        sourcesCovered: 0,
        sourcesAlreadyCovered: 0,
        blocked: 0,
        skipped: 0,
        failed: selectedAtStart.length
      },
      result: {
        failed: [{ sourceIds: selectedAtStart.slice(0, 25), error: error }]
      }
    };
    clipboard.toastError(ctx.errorMessage(error));
  }).finally(function () {
    inFlightMode = null;
    apiTaskSelection.render(document.querySelector('.api-batch-host'), ctx);
  });
}

function resultSummary(ctx, result) {
  return ctx.t('api.selection.result', result.counts || {});
}

function resultLinks(host, result) {
  var rows = result.result || {};
  (rows.created || []).concat(rows.existing || [])
    .slice(0, 25)
    .forEach(function (row) {
      var task = row.task || {};
      var stem = row.stem || task.stem;
      if (!stem) return;
      host.appendChild(el('a', {
        class: 'api-task-chip',
        href: '#board?task=' + encodeURIComponent(stem),
        text: task.title || stem
      }));
    });
}

function resultIssues(ctx, result) {
  var rows = result.result || {};
  var issues = [];
  (rows.blocked || []).forEach(function (row) {
    issues.push(
      (row.item && row.item.title || row.sourceId) +
      ' · ' + ctx.t('api.selection.hotfixRequired')
    );
  });
  (rows.skipped || []).forEach(function (row) {
    issues.push(row.sourceId + ' · ' + ctx.errorMessage(row.reason));
  });
  (rows.failed || []).forEach(function (row) {
    issues.push((row.id || (row.sourceIds || []).join(', ')) +
      ' · ' + ctx.errorMessage(row.error));
  });
  return issues;
}

export const apiTaskSelection = {
  // The panel clears the selection whenever the contract or the analysis
  // reports change underneath it. That can land during a background refresh,
  // with the user scrolling or on another tab, so the loss is recorded here
  // and stays until dismissed instead of living only in a toast.
  invalidate: function () { selectionInvalidated = true; },
  render: function (host, ctx) {
    if (!host) return;
    host.replaceChildren();
    if (lastResult || selectionInvalidated) {
      var resultBox = el('div', {
        class: 'api-batch-result',
        attrs: { role: 'status', 'aria-live': 'polite' }
      });
      if (selectionInvalidated) {
        resultBox.appendChild(el('p', {
          class: 'field-help',
          text: ctx.t('api.selection.invalidated')
        }));
      }
      if (lastResult) {
        resultBox.appendChild(el('strong', {
          text: resultSummary(ctx, lastResult)
        }));
        var links = el('div', { class: 'api-batch-result__links' });
        resultLinks(links, lastResult);
        resultBox.appendChild(links);
        var issueRows = resultIssues(ctx, lastResult);
        if (issueRows.length) {
          var failures = el('ul', { class: 'api-batch-result__failures' });
          issueRows.slice(0, 25).forEach(function (text) {
            failures.appendChild(el('li', { text: text }));
          });
          resultBox.appendChild(failures);
        }
      }
      var dismiss = el('button', {
        type: 'button',
        class: 'btn btn--ghost btn--small',
        text: ctx.t('api.selection.dismiss')
      });
      dismiss.addEventListener('click', function () {
        lastResult = null;
        selectionInvalidated = false;
        apiTaskSelection.render(host, ctx);
      });
      resultBox.appendChild(dismiss);
      host.appendChild(resultBox);
      if (!ctx.selected.size) return;
    }
    if (!ctx.selected.size) return;

    var bar = el('div', {
      class: 'api-batch-bar',
      attrs: { role: 'region', 'aria-label': ctx.t('api.selection.actionsAria') }
    });
    var copy = el('div', {
      class: 'api-batch-bar__copy',
      attrs: { 'aria-live': 'polite' }
    });
    copy.appendChild(el('strong', {
      text: ctx.t('api.selection.count', { count: ctx.selected.size })
    }));
    copy.appendChild(el('span', {
      class: 'field-help',
      text: ctx.t('api.selection.scopeHint')
    }));
    bar.appendChild(copy);
    var actions = el('div', { class: 'api-batch-bar__actions' });

    var clear = el('button', {
      type: 'button',
      class: 'btn btn--ghost btn--small',
      text: ctx.t('api.selection.clear'),
      disabled: !!inFlightMode
    });
    clear.addEventListener('click', function () {
      ctx.selected.clear();
      apiTaskSelection.render(host, ctx);
      ctx.refresh();
    });
    actions.appendChild(clear);

    if (ctx.selected.size === 1) {
      var hotfix = el('button', {
        type: 'button',
        class: 'btn btn--ghost btn--small',
        text: inFlightMode === 'hotfix'
          ? ctx.t('api.selection.working')
          : ctx.t('api.selection.hotfix'),
        disabled: !!inFlightMode
      });
      hotfix.addEventListener('click', function () {
        preview(ctx, 'hotfix');
      });
      actions.appendChild(hotfix);
    }

    var create = el('button', {
      type: 'button',
      class: 'btn btn--small',
      text: inFlightMode === 'package'
        ? ctx.t('api.selection.working')
        : ctx.t('api.selection.createPackage'),
      disabled: !!inFlightMode
    });
    create.addEventListener('click', function () {
      preview(ctx, 'package');
    });
    actions.appendChild(create);
    bar.appendChild(actions);
    host.appendChild(bar);
  }
};
