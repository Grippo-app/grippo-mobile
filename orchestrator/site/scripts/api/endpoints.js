import { dom } from '../dom.js';

var el = dom.el;
var selectAllInFlight = false;
function addParam(params, key, value) { if (value) params.set(key, value); }
function loadUrl(state, cursor) {
  var params = new URLSearchParams();
  addParam(params, 'query', state.query);
  addParam(params, 'area', state.area);
  addParam(params, 'method', state.method);
  addParam(params, 'implementation', state.implementation);
  addParam(params, 'auth', state.auth);
  addParam(params, 'hasTask', state.hasTask);
  addParam(params, 'changeSeverity', state.changeSeverity);
  addParam(params, 'mismatch', state.mismatch);
  addParam(params, 'consumers', state.consumers);
  addParam(params, 'cursor', cursor);
  params.set('limit', '100');
  return '/api/api/endpoints?' + params.toString();
}
function option(value, label) { return el('option', { value: value, text: label }); }
function select(ctx, key, values) {
  var input = el('select', {
    class: 'input api-filter',
    attrs: {
      'aria-label': ctx.t('api.filter.' + key),
      'data-api-focus': 'endpoint-' + key
    }
  });
  input.appendChild(option('', ctx.t('api.filter.any.' + key)));
  values.forEach(function (row) { input.appendChild(option(row[0], row[1])); });
  input.value = ctx.state[key];
  input.addEventListener('change', function () {
    var patch = {}; patch[key] = input.value; ctx.setState(patch, true);
  });
  return input;
}
function sourceFor(row) {
  if (row.latestChange && row.latestChange.id) return 'api:change:' + row.latestChange.id;
  return null;
}
function addPageSources(target, payload) {
  (payload.items || []).forEach(function (row) {
    var sourceId = sourceFor(row);
    if (sourceId) target.add(sourceId);
  });
}
function allMatchingSources(ctx, firstPage) {
  var sourceIds = new Set();
  var cursors = new Set();
  function collect(page) {
    addPageSources(sourceIds, page || {});
    var cursor = page && page.page && page.page.nextCursor;
    if (!cursor) return sourceIds;
    if (cursors.has(cursor)) throw new Error('API endpoint cursor repeated');
    cursors.add(cursor);
    return ctx.get(loadUrl(ctx.state, cursor)).then(collect);
  }
  return Promise.resolve(collect(firstPage));
}
function badge(ctx, value, group) {
  return el('span', {
    class: 'api-badge api-badge--' + String(value || 'unknown').replace(/[^a-z0-9-]/g, ''),
    text: ctx.t('api.' + group + '.' + (value || 'unknown'))
  });
}
function rowElement(ctx, row) {
  var article = el('article', { class: 'api-endpoint-card' });
  var sourceId = sourceFor(row);
  if (sourceId) {
    var check = el('input', {
      type: 'checkbox',
      class: 'choice-input',
      checked: ctx.selected.has(sourceId),
      attrs: { 'aria-label': ctx.t('api.selection.item', { item: row.operationId }) }
    });
    check.addEventListener('change', function () {
      if (!ctx.toggleSource(sourceId, check.checked)) check.checked = false;
    });
    article.appendChild(check);
  } else article.appendChild(el('span', { class: 'api-selection-placeholder' }));
  var open = el('button', {
    type: 'button',
    class: 'api-endpoint-open',
    attrs: { 'data-api-focus': row.operationId }
  });
  var first = el('span', { class: 'api-endpoint-card__route' });
  first.appendChild(el('span', {
    class: 'api-method api-method--' + row.method.toLowerCase(), text: row.method
  }));
  first.appendChild(el('code', { text: row.path }));
  open.appendChild(first);
  open.appendChild(el('span', {
    class: 'api-endpoint-card__summary', text: row.summary || row.operationId
  }));
  open.appendChild(el('span', {
    class: 'api-endpoint-card__meta',
    text: [row.operationId, row.area, row.auth].filter(Boolean).join(' · ')
  }));
  open.addEventListener('click', function () { ctx.openEndpoint(row.operationId, open); });
  article.appendChild(open);
  var badges = el('div', { class: 'api-endpoint-card__badges' });
  badges.appendChild(badge(ctx, row.implementation.state, 'implementation'));
  badges.appendChild(badge(ctx, row.mismatch.severity, 'mismatch'));
  if (row.latestChange) badges.appendChild(badge(ctx, row.latestChange.severity, 'severity'));
  if (row.tasks && row.tasks.open.length) badges.appendChild(el('a', {
    class: 'api-task-chip',
    href: '#board?task=' + encodeURIComponent(row.tasks.open[0].stem),
    text: ctx.t('api.task.open')
  }));
  article.appendChild(badges);
  return article;
}
function renderToolbar(ctx, payload, toolbar) {
  toolbar.replaceChildren();
  var form = el('form', { class: 'api-filters', attrs: { role: 'search' } });
  form.addEventListener('submit', function (event) { event.preventDefault(); });
  var search = el('input', {
    type: 'search', class: 'input api-filter__search', value: ctx.state.query,
    placeholder: ctx.t('api.search.placeholder'),
    attrs: { maxlength: '200', 'aria-label': ctx.t('api.search.placeholder'), 'data-api-focus': 'search' }
  });
  var timer = null;
  search.addEventListener('input', function () {
    clearTimeout(timer);
    timer = setTimeout(function () {
      if (!search.isConnected || !ctx.isCurrent()) return;
      ctx.setState({ query: ctx.boundedQuery(search.value, 200) }, true);
    }, 180);
  });
  form.appendChild(search);
  var facets = payload.facets || {};
  form.appendChild(select(ctx, 'area', (facets.areas || []).map(function (value) { return [value, value]; })));
  form.appendChild(select(ctx, 'method', (facets.methods || []).map(function (value) { return [value, value]; })));
  form.appendChild(select(ctx, 'implementation', [
    ['implemented', ctx.t('api.implementation.implemented')],
    ['partial', ctx.t('api.implementation.partial')],
    ['unknown', ctx.t('api.implementation.unknown')]
  ]));
  form.appendChild(select(ctx, 'auth', (facets.auth || []).map(function (value) {
    return [value, value];
  })));
  form.appendChild(select(ctx, 'changeSeverity', [
    ['breaking', ctx.t('api.severity.breaking')],
    ['potentially-breaking', ctx.t('api.severity.potentially-breaking')],
    ['compatible', ctx.t('api.severity.compatible')],
    ['info', ctx.t('api.severity.info')]
  ]));
  form.appendChild(select(ctx, 'mismatch', [
    ['present', ctx.t('api.mismatch.present')],
    ['none', ctx.t('api.mismatch.none')],
    ['not-checked', ctx.t('api.mismatch.not-checked')]
  ]));
  form.appendChild(select(ctx, 'hasTask', [
    ['yes', ctx.t('api.filter.yes')], ['no', ctx.t('api.filter.no')]
  ]));
  form.appendChild(select(ctx, 'consumers', [
    ['yes', ctx.t('api.filter.yes')],
    ['no', ctx.t('api.filter.no')],
    ['unknown', ctx.t('api.analysis.not-checked')]
  ]));
  toolbar.appendChild(form);
}
function renderSelectionActions(ctx, payload, content) {
  var actions = el('div', { class: 'api-list-actions' });
  var selectAll = el('button', {
    type: 'button',
    class: 'btn btn--ghost btn--small',
    text: ctx.t(selectAllInFlight
      ? 'api.selection.selectingAll'
      : 'api.selection.selectAll'),
    disabled: selectAllInFlight,
    attrs: { 'data-api-focus': 'select-all-endpoints' }
  });
  selectAll.addEventListener('click', function () {
    if (selectAllInFlight) return;
    selectAllInFlight = true;
    selectAll.disabled = true;
    selectAll.textContent = ctx.t('api.selection.selectingAll');
    allMatchingSources(ctx, payload).then(function (sourceIds) {
      if (!ctx.isCurrent()) return;
      var added = ctx.addSources(Array.from(sourceIds));
      ctx.toast('api.selection.selectAllDone', {
        added: added,
        total: ctx.selected.size
      });
    }).catch(function (error) {
      ctx.toastError(error);
    }).finally(function () {
      selectAllInFlight = false;
      ctx.refresh();
    });
  });
  actions.appendChild(selectAll);
  content.appendChild(actions);
}

export const apiEndpoints = {
  load: function (ctx) { return ctx.get(loadUrl(ctx.state)); },
  render: function (ctx, payload, toolbar, content) {
    renderToolbar(ctx, payload, toolbar);
    content.replaceChildren();
    if (payload.empty) {
      var empty = el('div', { class: 'banner banner--info' });
      empty.appendChild(document.createTextNode(ctx.t('api.empty.pre') + ' '));
      empty.appendChild(el('a', { href: '#backend', text: ctx.t('api.empty.link') }));
      empty.appendChild(document.createTextNode('.'));
      content.appendChild(empty);
      return;
    }
    renderSelectionActions(ctx, payload, content);
    var list = el('div', { class: 'api-endpoint-list' });
    (payload.items || []).forEach(function (row) { list.appendChild(rowElement(ctx, row)); });
    if (!(payload.items || []).length) list.appendChild(el('p', {
      class: 'api-state', text: ctx.t('api.noMatches')
    }));
    content.appendChild(list);
    if (payload.page && payload.page.nextCursor) {
      var loaded = payload.page.returned;
      var pager = el('div', { class: 'api-pager' });
      var note = el('p', {
        class: 'field-help', text: ctx.t('api.page.more', {
          shown: loaded, total: payload.page.total
        })
      });
      var more = el('button', {
        type: 'button', class: 'btn btn--ghost', text: ctx.t('api.page.loadMore')
      });
      var cursor = payload.page.nextCursor;
      more.addEventListener('click', function () {
        more.disabled = true;
        ctx.get(loadUrl(ctx.state, cursor)).then(function (next) {
          if (!ctx.isCurrent()) return;
          ctx.adopt(next);
          (next.items || []).forEach(function (row) { list.appendChild(rowElement(ctx, row)); });
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
    if (payload.limitations && payload.limitations.length) {
      content.appendChild(el('p', {
        class: 'api-limitations',
        text: ctx.t('api.limitations', { count: payload.limitations.length })
      }));
    }
  }
};
