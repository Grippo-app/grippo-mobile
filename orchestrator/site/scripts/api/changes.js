import { dom } from '../dom.js';

var el = dom.el;
function loadUrl(state, cursor) {
  var params = new URLSearchParams({ limit: '100' });
  ['severity', 'kind', 'query', 'operationId', 'modelId', 'hasTask'].forEach(function (key) {
    if (state[key]) params.set(key, state[key]);
  });
  if (cursor) params.set('cursor', cursor);
  return '/api/api/changes?' + params.toString();
}
function option(value, text) { return el('option', { value: value, text: text }); }
function textFilter(ctx, key) {
  var input = el('input', {
    type: 'search',
    class: 'input api-filter',
    value: ctx.state[key],
    placeholder: ctx.t('api.filter.' + key),
    attrs: {
      maxlength: key === 'kind' ? '100' : '200',
      'aria-label': ctx.t('api.filter.' + key),
      'data-api-focus': 'change-' + key
    }
  });
  var timer;
  input.addEventListener('input', function () {
    clearTimeout(timer);
    timer = setTimeout(function () {
      if (!input.isConnected || !ctx.isCurrent()) return;
      var value = ctx.boundedQuery(input.value, key === 'kind' ? 100 : 200);
      if (key === 'kind') value = value.toLowerCase().replace(/[^a-z0-9-]/g, '');
      var patch = {}; patch[key] = value;
      ctx.setState(patch, true);
    }, 180);
  });
  return input;
}
function renderToolbar(ctx, toolbar) {
  toolbar.replaceChildren();
  var form = el('form', { class: 'api-filters', attrs: { role: 'search' } });
  form.addEventListener('submit', function (event) { event.preventDefault(); });
  var search = el('input', {
    type: 'search', class: 'input api-filter__search', value: ctx.state.query,
    placeholder: ctx.t('api.changes.search'),
    attrs: {
      maxlength: '200',
      'aria-label': ctx.t('api.changes.search'),
      'data-api-focus': 'change-search'
    }
  });
  var timer;
  search.addEventListener('input', function () {
    clearTimeout(timer);
    timer = setTimeout(function () {
      if (!search.isConnected || !ctx.isCurrent()) return;
      ctx.setState({ query: ctx.boundedQuery(search.value, 200) }, true);
    }, 180);
  });
  form.appendChild(search);
  var severity = el('select', {
    class: 'input api-filter',
    attrs: {
      'aria-label': ctx.t('api.filter.severity'),
      'data-api-focus': 'change-severity'
    }
  });
  severity.appendChild(option('', ctx.t('api.filter.any.severity')));
  severity.appendChild(option('attention', ctx.t('api.severity.attention')));
  ['breaking', 'potentially-breaking', 'compatible', 'info'].forEach(function (value) {
    severity.appendChild(option(value, ctx.t('api.severity.' + value)));
  });
  severity.value = ctx.state.severity;
  severity.addEventListener('change', function () { ctx.setState({ severity: severity.value }, true); });
  form.appendChild(severity);
  form.appendChild(textFilter(ctx, 'kind'));
  form.appendChild(textFilter(ctx, 'operationId'));
  form.appendChild(textFilter(ctx, 'modelId'));
  var hasTask = el('select', {
    class: 'input api-filter',
    attrs: {
      'aria-label': ctx.t('api.filter.hasTask'),
      'data-api-focus': 'change-has-task'
    }
  });
  hasTask.appendChild(option('', ctx.t('api.filter.any.hasTask')));
  hasTask.appendChild(option('yes', ctx.t('api.filter.yes')));
  hasTask.appendChild(option('no', ctx.t('api.filter.no')));
  hasTask.value = ctx.state.hasTask;
  hasTask.addEventListener('change', function () {
    ctx.setState({ hasTask: hasTask.value }, true);
  });
  form.appendChild(hasTask);
  toolbar.appendChild(form);
}
function impact(ctx, value) {
  var block = el('div', { class: 'api-change-impact' });
  var implementation = value && value.affectedImplementation;
  var implementations = value && value.affectedImplementations || [];
  if (implementations.length > 1) {
    block.appendChild(el('span', {
      text: ctx.t('api.change.implementations', { count: implementations.length })
    }));
  } else {
    block.appendChild(el('span', {
      text: ctx.t('api.change.implementation', {
        state: ctx.t('api.implementation.' + (implementation && implementation.state || 'unknown'))
      })
    }));
  }
  var consumers = value && value.affectedConsumers || [];
  var consumerText = consumers.length ? String(consumers.length) :
    value && value.noKnownConsumersIsConclusive ? ctx.t('api.change.noKnownConsumers') :
      ctx.t('api.change.consumersUnknown');
  block.appendChild(el('span', {
    text: ctx.t('api.change.consumers', { count: consumerText })
  }));
  var models = value && value.affectedModels || [];
  if (models.length) block.appendChild(el('span', {
    text: ctx.t('api.change.models', { count: models.length })
  }));
  if (value && value.consumerAnalysisStatus) block.appendChild(el('span', {
    text: ctx.t('api.change.consumerStatus', {
      status: ctx.t('api.analysis.' + value.consumerAnalysisStatus)
    })
  }));
  if (value && value.truncated) block.appendChild(el('span', {
    class: 'api-context-warning',
    text: ctx.t('api.change.impactTruncated')
  }));
  return block;
}
function changeRow(ctx, row, changeSet) {
  var article = el('article', { class: 'api-change-card' });
  var sourceId = row.sourceId;
  var check = el('input', {
    type: 'checkbox', class: 'choice-input', checked: ctx.selected.has(sourceId),
    attrs: { 'aria-label': ctx.t('api.selection.item', { item: row.id }) }
  });
  check.addEventListener('change', function () {
    if (!ctx.toggleSource(sourceId, check.checked)) check.checked = false;
  });
  article.appendChild(check);
  var body = el('div', { class: 'api-change-card__body' });
  var head = el('div', { class: 'api-change-card__head' });
  head.appendChild(el('span', {
    class: 'api-badge api-badge--' + row.severity,
    text: ctx.t('api.severity.' + row.severity)
  }));
  head.appendChild(el('strong', { text: ctx.changeKindMessage(row.kind) }));
  body.appendChild(head);
  body.appendChild(el('p', { text: row.afterSummary || row.beforeSummary || row.id }));
  if (row.endpointOperationId) {
    var endpoint = el('button', {
      type: 'button', class: 'api-link-button', text: row.endpointOperationId,
      attrs: { 'data-api-focus': row.id }
    });
    endpoint.addEventListener('click', function () {
      ctx.openEndpoint(row.endpointOperationId, endpoint);
    });
    body.appendChild(endpoint);
  } else if (row.operationId) {
    body.appendChild(el('code', { text: row.operationId }));
  }
  body.appendChild(impact(ctx, row.impact));
  if (row.reviewed) {
    body.appendChild(el('span', {
      class: 'api-badge', text: ctx.t('api.change.reviewed')
    }));
  } else if (changeSet && changeSet.reviewRevision) {
    var review = el('button', {
      type: 'button', class: 'btn btn--ghost btn--small',
      text: ctx.t('api.change.markReviewed')
    });
    review.addEventListener('click', function () {
      review.disabled = true;
      ctx.post('/api/api/changes/review', {
        changeId: row.id,
        changeSetId: changeSet.id,
        expectedGenerationId: ctx.meta.committedGenerationId,
        expectedReviewRevision: changeSet.reviewRevision,
        idempotencyKey: ctx.randomKey('api.change.review.')
      }).then(function () {
        return ctx.refresh();
      }).catch(function (error) {
        review.disabled = false;
        ctx.toastError(error);
      });
    });
    body.appendChild(review);
  }
  if (row.tasks && row.tasks.open.length) body.appendChild(el('a', {
    class: 'api-task-chip',
    href: '#board?task=' + encodeURIComponent(row.tasks.open[0].stem),
    text: ctx.t('api.task.open')
  }));
  article.appendChild(body);
  return article;
}

export const apiChanges = {
  load: function (ctx) { return ctx.get(loadUrl(ctx.state)); },
  render: function (ctx, payload, toolbar, content) {
    renderToolbar(ctx, toolbar);
    content.replaceChildren();
    if (payload.changeSet) {
      var summary = payload.changeSet.summary || {};
      content.appendChild(el('p', {
        class: 'api-change-summary',
        text: ctx.t('api.changes.summary', {
          breaking: summary.breaking || 0,
          potential: summary.potentiallyBreaking || 0,
          compatible: summary.compatible || 0
        })
      }));
    }
    var list = el('div', { class: 'api-change-list' });
    (payload.items || []).forEach(function (row) {
      list.appendChild(changeRow(ctx, row, payload.changeSet));
    });
    if (!(payload.items || []).length) list.appendChild(el('p', {
      class: 'api-state', text: ctx.t('api.changes.empty')
    }));
    content.appendChild(list);
    if (payload.limitations && payload.limitations.length) {
      content.appendChild(el('p', {
        class: 'api-limitations',
        text: ctx.t('api.limitations', { count: payload.limitations.length })
      }));
    }
    if (payload.page && payload.page.nextCursor) {
      var loaded = payload.page.returned;
      var cursor = payload.page.nextCursor;
      var pager = el('div', { class: 'api-pager' });
      var note = el('p', {
        class: 'field-help',
        text: ctx.t('api.page.more', { shown: loaded, total: payload.page.total })
      });
      var more = el('button', {
        type: 'button', class: 'btn btn--ghost', text: ctx.t('api.page.loadMore')
      });
      more.addEventListener('click', function () {
        more.disabled = true;
        ctx.get(loadUrl(ctx.state, cursor)).then(function (next) {
          if (!ctx.isCurrent()) return;
          ctx.adopt(next);
          (next.items || []).forEach(function (row) {
            list.appendChild(changeRow(ctx, row, next.changeSet));
          });
          loaded += next.page && next.page.returned || 0;
          cursor = next.page && next.page.nextCursor;
          note.textContent = ctx.t('api.page.more', {
            shown: loaded, total: next.page && next.page.total || loaded
          });
          if (!cursor) pager.remove();
          else more.disabled = false;
        }).catch(function (error) {
          more.disabled = false;
          ctx.toastError(error);
        });
      });
      pager.appendChild(note);
      pager.appendChild(more);
      content.appendChild(pager);
    }
  }
};
