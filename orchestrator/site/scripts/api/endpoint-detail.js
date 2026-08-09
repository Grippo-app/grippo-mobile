import { dom } from '../dom.js';
import { clipboard } from '../clipboard.js';

var el = dom.el;
var dialog = null;
var body = null;
var title = null;
var closeButton = null;
var opener = null;
var onClose = null;
var requestVersion = 0;

// Route ownership matches the router's: only the part before "?" picks the
// panel, so "#api?tab=endpoints" is still the API panel and "" is not.
function onApiRoute() {
  var raw = (location.hash || '').replace(/^#/, '');
  var query = raw.indexOf('?');
  return (query === -1 ? raw : raw.slice(0, query)) === 'api';
}
function ensure() {
  if (dialog) return dialog;
  dialog = el('dialog', {
    class: 'api-drawer',
    attrs: { 'aria-labelledby': 'api-drawer-title' }
  });
  var shell = el('div', { class: 'api-drawer__shell' });
  var head = el('header', { class: 'api-drawer__head' });
  title = el('h3', { id: 'api-drawer-title', class: 'api-drawer__title' });
  closeButton = el('button', {
    type: 'button', class: 'btn btn--ghost', text: '×',
    attrs: { 'aria-label': 'Close' }
  });
  closeButton.addEventListener('click', function () { dialog.close(); });
  head.appendChild(title);
  head.appendChild(closeButton);
  body = el('div', { class: 'api-drawer__body' });
  shell.appendChild(head);
  shell.appendChild(body);
  dialog.appendChild(shell);
  dialog.addEventListener('close', function () {
    requestVersion++;
    if (typeof onClose === 'function') onClose();
    if (opener && opener.isConnected) {
      try { opener.focus({ preventScroll: true }); } catch (error) { opener.focus(); }
    }
    opener = null; onClose = null;
  });
  dialog.addEventListener('click', function (event) {
    if (event.target === dialog) dialog.close();
  });
  // The drawer links out to #board and #archmap. Following one hides the API
  // panel with this modal still open inside it: invisible, still blocking the
  // document, and recoverable only by Back or reload. Leaving the API route
  // closes it first.
  window.addEventListener('hashchange', function () {
    if (!dialog.open || onApiRoute()) return;
    // The panel's close callback rewrites the URL back to #api; the route has
    // already moved on, so it must not run for this close.
    onClose = null;
    dialog.close();
  });
  return dialog;
}
function codeBlock(value) {
  return el('pre', { class: 'api-json', text: JSON.stringify(value, null, 2) });
}
function fieldValue(value) {
  if (typeof value === 'string') return value;
  var encoded;
  try { encoded = JSON.stringify(value); } catch (error) { encoded = String(value); }
  return String(encoded === undefined ? value : encoded).slice(0, 200);
}
function fields(ctx, label, rows) {
  var section = el('details', { class: 'api-schema-group' });
  section.appendChild(el('summary', { text: label }));
  if (!rows || !rows.length) {
    section.appendChild(el('p', { class: 'field-help', text: ctx.t('api.detail.none') }));
    return section;
  }
  var table = el('table', { class: 'api-schema-table' });
  var thead = el('thead');
  var header = el('tr');
  [
    ctx.t('api.field.name'),
    ctx.t('api.field.type'),
    ctx.t('api.field.flags'),
    ctx.t('api.field.values')
  ].forEach(function (value) {
    header.appendChild(el('th', { text: value, attrs: { scope: 'col' } }));
  });
  thead.appendChild(header);
  var tbody = el('tbody');
  rows.forEach(function (row) {
    var tr = el('tr');
    tr.appendChild(el('th', {
      text: row.jsonName || row.name || '—', attrs: { scope: 'row' }
    }));
    tr.appendChild(el('td', {
      text: (row.type || 'unknown') + (row.format ? ' · ' + row.format : '')
    }));
    tr.appendChild(el('td', {
      text: [
        row.required ? ctx.t('api.field.required') : '',
        row.nullable_declared ? ctx.t('api.field.nullable') : '',
        row.nullable_observed ? ctx.t('api.field.observedNull') : ''
      ].filter(Boolean).join(' · ')
    }));
    tr.appendChild(el('td', {
      text: [
        Array.isArray(row.enum) && row.enum.length
          ? ctx.t('api.field.enumValues', {
            values: row.enum.slice(0, 20).map(fieldValue).join(', ')
          }) : '',
        Array.isArray(row.enumObserved) && row.enumObserved.length
          ? ctx.t('api.field.observedEnum', {
            values: row.enumObserved.map(fieldValue).join(', ')
          }) : ''
      ].filter(Boolean).join(' · ')
    }));
    tbody.appendChild(tr);
  });
  table.appendChild(thead);
  table.appendChild(tbody);
  section.appendChild(table);
  return section;
}
function exampleActions(ctx, value, filename) {
  var actions = el('div', { class: 'api-example-actions' });
  var copy = el('button', {
    type: 'button', class: 'btn btn--ghost btn--small', text: ctx.t('api.mock.copy')
  });
  copy.addEventListener('click', function () { clipboard.copy(JSON.stringify(value, null, 2)); });
  var download = el('button', {
    type: 'button', class: 'btn btn--ghost btn--small', text: ctx.t('api.mock.download')
  });
  download.addEventListener('click', function () {
    var blob = new Blob([JSON.stringify(value, null, 2) + '\n'], { type: 'application/json' });
    var url = URL.createObjectURL(blob);
    var link = document.createElement('a');
    link.href = url; link.download = filename; link.click();
    setTimeout(function () { URL.revokeObjectURL(url); }, 0);
  });
  actions.appendChild(copy);
  actions.appendChild(download);
  return actions;
}
function taskLink(ctx, task, state) {
  return el('a', {
    class: 'api-task-chip',
    href: '#board?task=' + encodeURIComponent(task.stem),
    text: (task.title || task.stem) + (state ? ' · ' + ctx.t('api.task.' + state) : '')
  });
}
function examples(ctx, payload) {
  var section = el('section', { class: 'api-detail-section' });
  section.appendChild(el('h4', { text: ctx.t('api.mock.title') }));
  section.appendChild(el('p', { class: 'field-help', text: ctx.t('api.mock.generatedHelp') }));
  var request = payload.examples && payload.examples.request;
  var responses = payload.examples && payload.examples.responses || {};
  var statuses = Object.keys(responses);
  if (!request && !statuses.length) {
    section.appendChild(el('p', { text: ctx.t('api.mock.unavailable') }));
    return section;
  }
  function renderExample(host, row, filename) {
    host.replaceChildren();
    if (!row || row.truncated) {
      host.appendChild(el('p', { text: ctx.t('api.mock.truncated') }));
      return;
    }
    host.appendChild(el('span', {
      class: 'api-badge', text: row.generated ? ctx.t('api.mock.generated') : ctx.t('api.mock.explicit')
    }));
    host.appendChild(codeBlock(row.value));
    host.appendChild(exampleActions(ctx, row.value, filename));
  }
  var safeName = payload.endpoint.operationId.replace(/[^A-Za-z0-9_.-]/g, '_');
  if (request) {
    section.appendChild(el('h5', { text: ctx.t('api.detail.requestExample') }));
    var requestOutput = el('div');
    renderExample(requestOutput, request, safeName + '-request.json');
    section.appendChild(requestOutput);
  }
  if (statuses.length) {
    section.appendChild(el('h5', { text: ctx.t('api.detail.responseExamples') }));
    var select = el('select', {
      class: 'input api-example-select',
      attrs: { 'aria-label': ctx.t('api.mock.responseCode') }
    });
    statuses.forEach(function (status) {
      var row = responses[status];
      select.appendChild(el('option', {
        value: status,
        text: status + ' · ' + row.contentType
      }));
    });
    var output = el('div');
    function renderResponse() {
      renderExample(output, responses[select.value], safeName + '-' + select.value + '.json');
    }
    select.addEventListener('change', renderResponse);
    section.appendChild(select);
    section.appendChild(output);
    renderResponse();
  }
  return section;
}
function render(ctx, payload) {
  body.replaceChildren();
  var endpoint = payload.endpoint;
  title.textContent = endpoint.method + ' ' + endpoint.path;
  var meta = el('div', { class: 'api-detail-meta' });
  meta.appendChild(el('code', { text: endpoint.operationId }));
  meta.appendChild(el('span', { text: endpoint.area }));
  meta.appendChild(el('span', {
    class: 'api-badge api-badge--' + endpoint.implementation.state,
    text: ctx.t('api.implementation.' + endpoint.implementation.state)
  }));
  body.appendChild(meta);

  var summary = el('section', { class: 'api-detail-section' });
  summary.appendChild(el('h4', { text: ctx.t('api.detail.summary') }));
  summary.appendChild(el('p', {
    text: endpoint.summary || endpoint.operationId
  }));
  summary.appendChild(el('p', {
    text: ctx.t('api.detail.auth', { auth: endpoint.auth || 'none' })
  }));
  if (endpoint.deprecated) summary.appendChild(el('p', {
    class: 'api-context-warning', text: ctx.t('api.detail.deprecated')
  }));
  if (endpoint.latestChange) summary.appendChild(el('p', {
    text: ctx.t('api.detail.latestChange', {
      kind: ctx.changeKindMessage(endpoint.latestChange.kind),
      severity: ctx.t('api.severity.' + endpoint.latestChange.severity)
    })
  }));
  body.appendChild(summary);

  var contract = payload.contract || {};
  var request = contract.request || {};
  var contractSection = el('section', { class: 'api-detail-section' });
  contractSection.appendChild(el('h4', { text: ctx.t('api.detail.contract') }));
  contractSection.appendChild(fields(ctx, ctx.t('api.detail.pathParams'), request.pathParams));
  contractSection.appendChild(fields(ctx, ctx.t('api.detail.query'), request.query));
  if (request.body) contractSection.appendChild(el('p', {
    text: ctx.t('api.detail.requestBody', {
      model: request.body.schemaRef || '—',
      contentType: request.body.contentType || 'application/json'
    })
  }));
  var responseRows = Object.keys(contract.responses || {}).sort().map(function (status) {
    var response = contract.responses[status] || {};
    return status + ' · ' + (response.schemaRef || ctx.t('api.detail.noSchema')) +
      (response.array ? '[]' : '');
  });
  contractSection.appendChild(el('p', {
    text: ctx.t('api.detail.responses', {
      list: responseRows.join(', ') || ctx.t('api.detail.none')
    })
  }));
  if ((contract.errors || []).length) contractSection.appendChild(el('p', {
    text: ctx.t('api.detail.errors', { list: contract.errors.join(', ') })
  }));
  body.appendChild(contractSection);

  body.appendChild(examples(ctx, payload));

  var implementation = payload.implementation || endpoint.implementation || {};
  var implementationSection = el('section', { class: 'api-detail-section' });
  implementationSection.appendChild(el('h4', {
    text: ctx.t('api.detail.implementation')
  }));
  implementationSection.appendChild(el('p', {
    text: ctx.t('api.detail.implementationState', {
      state: ctx.t('api.implementation.' + (implementation.state || 'unknown'))
    })
  }));
  if (implementation.file) implementationSection.appendChild(el('p', {
    text: ctx.t('api.detail.file', { value: implementation.file })
  }));
  if (implementation.symbol) implementationSection.appendChild(el('p', {
    text: ctx.t('api.detail.symbol', { value: implementation.symbol })
  }));
  if (implementation.confidence) implementationSection.appendChild(el('p', {
    text: ctx.t('api.detail.confidence', {
      value: ctx.confidenceMessage(implementation.confidence)
    })
  }));
  body.appendChild(implementationSection);

  var consumersSection = el('section', { class: 'api-detail-section' });
  consumersSection.appendChild(el('h4', { text: ctx.t('api.detail.consumersTitle') }));
  var consumers = payload.consumers || { items: [], analysisStatus: 'not-checked' };
  consumersSection.appendChild(el('p', {
    text: ctx.t('api.detail.consumers', {
      count: consumers.items.length,
      status: ctx.t('api.analysis.' + consumers.analysisStatus)
    })
  }));
  (consumers.items || []).slice(0, 100).forEach(function (consumer) {
    var line = el('div', { class: 'api-consumer-line' });
    if (consumer.architectureId) line.appendChild(el('a', {
      href: '#archmap', text: consumer.architectureId
    }));
    var source = [consumer.file, consumer.symbol].filter(Boolean).join(' · ');
    if (source) line.appendChild(el('code', {
      text: (consumer.architectureId ? ' · ' : '') + source
    }));
    consumersSection.appendChild(line);
  });
  body.appendChild(consumersSection);

  var changeSection = el('section', { class: 'api-detail-section' });
  changeSection.appendChild(el('h4', { text: ctx.t('api.detail.changes') }));
  var changes = payload.changes || [];
  if (!changes.length) changeSection.appendChild(el('p', {
    class: 'field-help', text: ctx.t('api.detail.noChanges')
  }));
  changes.forEach(function (change) {
    var article = el('article', { class: 'api-detail-finding' });
    if (change.sourceId) {
      var changeCheck = el('input', {
        type: 'checkbox',
        class: 'choice-input',
        checked: ctx.selected.has(change.sourceId),
        attrs: { 'aria-label': ctx.t('api.selection.item', { item: change.id }) }
      });
      changeCheck.addEventListener('change', function () {
        if (!ctx.toggleSource(change.sourceId, changeCheck.checked)) changeCheck.checked = false;
      });
      article.appendChild(changeCheck);
    }
    article.appendChild(el('span', {
      class: 'api-badge api-badge--' + change.severity,
      text: ctx.t('api.severity.' + change.severity)
    }));
    article.appendChild(el('strong', { text: ctx.changeKindMessage(change.kind) }));
    article.appendChild(el('p', {
      text: change.afterSummary || change.beforeSummary || change.id
    }));
    if ((change.evidence || []).length) article.appendChild(el('p', {
      class: 'field-help', text: change.evidence.join(' · ')
    }));
    changeSection.appendChild(article);
  });
  body.appendChild(changeSection);

  var mismatchSection = el('section', { class: 'api-detail-section' });
  mismatchSection.appendChild(el('h4', { text: ctx.t('api.detail.mismatches') }));
  var mismatches = Array.isArray(payload.mismatches) ? payload.mismatches : null;
  if (!mismatches) mismatchSection.appendChild(el('p', {
    class: 'field-help', text: ctx.t('api.analysis.not-checked')
  }));
  else if (!mismatches.length) mismatchSection.appendChild(el('p', {
    class: 'field-help', text: ctx.t('api.detail.noMismatches')
  }));
  (mismatches || []).forEach(function (finding) {
    if (!finding || !finding.sourceId) return;
    var article = el('article', { class: 'api-detail-finding' });
    var check = el('input', {
      type: 'checkbox',
      class: 'choice-input',
      checked: ctx.selected.has(finding.sourceId),
      attrs: { 'aria-label': ctx.t('api.selection.item', { item: finding.id }) }
    });
    check.addEventListener('change', function () {
      if (!ctx.toggleSource(finding.sourceId, check.checked)) check.checked = false;
    });
    article.appendChild(check);
    article.appendChild(el('span', {
      class: 'api-badge api-badge--' + finding.severity,
      text: ctx.driftSeverity(finding.severity)
    }));
    article.appendChild(el('strong', {
      text: ctx.driftFindingMessage(finding.kind)
    }));
    if (finding.suggestion) article.appendChild(el('p', {
      class: 'field-help', text: ctx.driftFindingSuggestion(finding.kind)
    }));
    if (finding.file) article.appendChild(el('code', { text: finding.file }));
    mismatchSection.appendChild(article);
  });
  body.appendChild(mismatchSection);

  var schemas = contract.schemas || {};
  var schemaIds = Object.keys(schemas);
  var models = el('section', { class: 'api-detail-section' });
  models.appendChild(el('h4', { text: ctx.t('api.detail.models') }));
  schemaIds.forEach(function (id) {
    var schema = schemas[id] || {};
    models.appendChild(fields(ctx, id, schema.fields));
  });
  if (contract.schemasTruncated) models.appendChild(el('p', {
    class: 'api-limitations', text: ctx.t('api.detail.schemasTruncated')
  }));
  body.appendChild(models);

  var taskSection = el('section', { class: 'api-detail-section' });
  taskSection.appendChild(el('h4', { text: ctx.t('api.detail.tasks') }));
  var tasks = payload.tasks || { open: [], resolved: [] };
  if (!(tasks.open || []).length && !(tasks.resolved || []).length) {
    taskSection.appendChild(el('p', {
      class: 'field-help', text: ctx.t('api.detail.noTasks')
    }));
  }
  (tasks.open || []).forEach(function (task) {
    taskSection.appendChild(taskLink(ctx, task, 'open'));
  });
  (tasks.resolved || []).forEach(function (task) {
    taskSection.appendChild(taskLink(ctx, task, 'resolved'));
  });
  body.appendChild(taskSection);
  if (payload.limitations && payload.limitations.length) {
    var limitationSection = el('section', {
      class: 'api-detail-section api-limitations'
    });
    limitationSection.appendChild(el('h4', {
      text: ctx.t('api.diagnostics.limitations')
    }));
    var limitationList = el('ul');
    payload.limitations.map(function (value) {
      return ctx.limitationMessage(value);
    }).filter(function (value, index, rows) {
      return rows.indexOf(value) === index;
    }).forEach(function (value) {
      limitationList.appendChild(el('li', { text: value }));
    });
    limitationSection.appendChild(limitationList);
    body.appendChild(limitationSection);
  }
}

export const apiEndpointDetail = {
  element: ensure,
  open: function (ctx, operationId, trigger, closed) {
    ensure();
    requestVersion++;
    var version = requestVersion;
    opener = trigger || document.activeElement;
    onClose = closed;
    title.textContent = operationId;
    closeButton.setAttribute('aria-label', ctx.t('api.close'));
    body.replaceChildren(el('p', { class: 'api-state', text: ctx.t('api.detail.loading') }));
    if (!dialog.open) dialog.showModal();
    closeButton.focus();
    var meta = ctx.meta;
    var query = meta && meta.committedGenerationId
      ? '?expectedGenerationId=' + encodeURIComponent(meta.committedGenerationId) : '';
    ctx.get('/api/api/endpoints/' + encodeURIComponent(operationId) + query)
      .then(function (payload) {
        if (version !== requestVersion || !dialog.open) return;
        ctx.adopt(payload);
        render(ctx, payload);
      }).catch(function (error) {
        if (version !== requestVersion || !dialog.open) return;
        body.replaceChildren(el('p', {
          class: 'agent-cards-status agent-cards-status--error',
          text: ctx.errorMessage(error)
        }));
      });
  },
  close: function () { if (dialog && dialog.open) dialog.close(); }
};
