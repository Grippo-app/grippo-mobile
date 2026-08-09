import { dom } from '../dom.js';

var el = dom.el;
function taskHref(task) {
  return task && task.stem ? '#board?task=' + encodeURIComponent(task.stem) : '#board';
}
function metricCard(ctx, key, value, action) {
  var card = el('button', {
    type: 'button',
    class: 'api-metric api-metric--interactive'
  });
  card.appendChild(el('span', { class: 'api-metric__label', text: ctx.t('api.metric.' + key) }));
  var amount = value && value.value !== null ? String(value.value) : '—';
  if (value && value.denominator !== null) amount += ' / ' + value.denominator;
  card.appendChild(el('strong', { class: 'api-metric__value', text: amount }));
  card.appendChild(el('span', {
    class: 'api-metric__state',
    text: ctx.t('api.analysis.' + (value && value.analysisStatus || 'not-checked'))
  }));
  card.addEventListener('click', function () { ctx.setState(action, false); });
  return card;
}
function priorityList(ctx, titleKey, rows, kind) {
  var section = el('section', { class: 'api-priority' });
  section.appendChild(el('h3', { class: 'panel-section-title', text: ctx.t(titleKey) }));
  if (!rows.length) {
    section.appendChild(el('p', { class: 'field-help', text: ctx.t('api.priority.none') }));
    return section;
  }
  var list = el('ul', { class: 'api-priority-list' });
  rows.forEach(function (row) {
    var item = el('li', { class: 'api-priority-list__item' });
    var summary = kind === 'mismatch'
      ? ctx.driftFindingMessage(row.kind)
      : row.summary || row.title || [row.method, row.path].filter(Boolean).join(' ') ||
      row.operationId || row.modelId || row.id;
    var openOperationId = kind === 'mismatch' || kind === 'change'
      ? row.endpointOperationId : row.operationId;
    if (openOperationId && kind !== 'task') {
      var button = el('button', { type: 'button', class: 'api-link-button', text: summary });
      button.addEventListener('click', function () {
        ctx.setState({ tab: 'endpoints', entity: openOperationId }, false);
      });
      item.appendChild(button);
    } else if (kind === 'task' && row.stem) {
      item.appendChild(el('a', {
        class: 'api-link-button', href: taskHref(row), text: summary || row.stem
      }));
    } else item.appendChild(document.createTextNode(summary || '—'));
    if (row.severity) item.appendChild(el('span', {
      class: 'api-badge api-badge--' + row.severity,
      text: ctx.t('api.severity.' + row.severity)
    }));
    if (row.task) item.appendChild(el('a', {
      class: 'api-task-chip', href: taskHref(row.task),
      text: row.task.title || row.task.stem || ctx.t('api.task.open')
    }));
    list.appendChild(item);
  });
  section.appendChild(list);
  return section;
}

export const apiOverview = {
  load: function (ctx) { return ctx.get('/api/api/overview'); },
  render: function (ctx, payload, toolbar, content) {
    toolbar.replaceChildren();
    content.replaceChildren();
    if (payload.empty) {
      var empty = el('div', { class: 'banner banner--info' });
      empty.appendChild(document.createTextNode(ctx.t('api.empty.pre') + ' '));
      empty.appendChild(el('a', { href: '#backend', text: ctx.t('api.empty.link') }));
      empty.appendChild(document.createTextNode('.'));
      content.appendChild(empty);
      return;
    }
    var context = payload.context || {};
    var strip = el('div', { class: 'api-context-strip' });
    strip.appendChild(el('strong', {
      text: context.activeEnvironment && context.activeEnvironment.label ||
        context.snapshotEnvironmentId || '—'
    }));
    strip.appendChild(el('span', {
      class: 'api-badge api-badge--' + (context.freshness && context.freshness.state || 'unknown'),
      text: ctx.t('api.freshness.' + (context.freshness && context.freshness.state || 'unknown'))
    }));
    if (context.environmentMismatch) {
      var warning = el('span', {
        class: 'api-context-warning', text: ctx.t('api.environmentMismatch')
      });
      strip.appendChild(warning);
    }
    var change = el('a', { class: 'btn btn--ghost btn--small', href: '#backend', text: ctx.t('api.changeBackend') });
    strip.appendChild(change);
    content.appendChild(strip);
    var metrics = el('div', { class: 'api-metrics' });
    var clearEndpointFilters = {
      query: '', area: '', method: '', auth: '', hasTask: '',
      changeSeverity: '', mismatch: '', consumers: ''
    };
    [
      ['implemented', Object.assign({}, clearEndpointFilters, {
        tab: 'endpoints', implementation: 'implemented'
      })],
      ['missing', Object.assign({}, clearEndpointFilters, {
        tab: 'endpoints', implementation: 'missing'
      })],
      ['drifted', Object.assign({}, clearEndpointFilters, {
        tab: 'diagnostics', implementation: '', mismatch: '', query: ''
      })],
      ['breakingChanges', {
        tab: 'changes', severity: 'attention', query: '', kind: '',
        operationId: '', modelId: '', hasTask: ''
      }]
    ].forEach(function (row) {
      metrics.appendChild(metricCard(ctx, row[0], payload.metrics && payload.metrics[row[0]], row[1]));
    });
    content.appendChild(metrics);
    var action = payload.primaryAction || {};
    var primary = el('a', {
      class: 'btn api-primary-action',
      href: action.href || '#api?tab=endpoints',
      text: ctx.t('api.action.' + (action.kind || 'view-endpoints'))
    });
    content.appendChild(primary);
    var priorities = payload.priorities || {};
    var grid = el('div', { class: 'api-priority-grid' });
    grid.appendChild(priorityList(ctx, 'api.priority.breaking', priorities.breakingChanges || [], 'change'));
    grid.appendChild(priorityList(ctx, 'api.priority.missing', priorities.missingEndpoints || [], 'endpoint'));
    grid.appendChild(priorityList(ctx, 'api.priority.mismatch', priorities.observedMismatches || [], 'mismatch'));
    grid.appendChild(priorityList(ctx, 'api.priority.recent', priorities.recentlyChanged || [], 'change'));
    grid.appendChild(priorityList(ctx, 'api.priority.tasks', priorities.openTasks || [], 'task'));
    content.appendChild(grid);
    if (payload.limitations && payload.limitations.length) {
      content.appendChild(el('p', {
        class: 'api-limitations',
        text: ctx.t('api.limitations', { count: payload.limitations.length })
      }));
    }
  }
};
