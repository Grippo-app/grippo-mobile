import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { designFilters } from './filters.js';
import { syncFailureText } from '../figma/sync-errors.js';

var el = dom.el;
function t(key, params) { return i18n.t(key, params); }
function metricValue(metric, suffix) {
  if (!metric || metric.value === null || metric.value === undefined) {
    if (metric && metric.state === 'not-checked') return t('design.notChecked');
    if (metric && metric.state === 'unsupported') return t('design.unsupported');
    return t('design.unknown');
  }
  return String(metric.value) + (suffix || '');
}
function section(title) {
  var block = el('section', { class: 'design-overview-section' });
  block.appendChild(el('h3', { text: title }));
  return block;
}
function findingTitle(item) {
  return item && item.kind
    ? designFilters.localizedEnum('design.finding.', item.kind, 'design.finding.unknown', {
      name: item.entityName || item.title || item.entityId
    })
    : item && item.title || t('design.finding.unknown');
}
function findingDetail(item) {
  if (!item) return t('design.unknown');
  if (item.entityType === 'token') return designFilters.tokenStatusText(item.status);
  if (item.entityType === 'project-token' || item.entityType === 'project-component') return t('design.projectOnly.title');
  if (item.entityType === 'component') return designFilters.componentStatusText(item.status);
  if (item.entityType === 'surface') return t('design.surfaceDrift.DRIFTED');
  return designFilters.statusText(item.status);
}
function timestamp(value) {
  var parsed = Date.parse(value);
  return Number.isFinite(parsed) ? new Date(parsed).toLocaleString() : String(value || '');
}
function render(root, context) {
  return designFilters.request('/api/design/overview').then(function (data) {
    if (!context.isCurrent()) return;
    context.adopt(data);
    if (!context.shouldRender(data)) return;
    root.replaceChildren();
    if (data.readiness.state !== 'ready' || data.freshness.state !== 'fresh') {
      var bannerState = data.readiness.state !== 'ready' ? data.readiness.state : data.freshness.state;
      var banner = el('div', {
        class: 'design-readiness design-readiness--' + designFilters.cssToken(bannerState)
      });
      banner.appendChild(el('strong', {
        text: data.readiness.state !== 'ready'
          ? designFilters.localizedEnum('design.readiness.', data.readiness.state, 'design.readiness.unknown')
          : designFilters.localizedEnum('design.freshness.', data.freshness.state, 'design.freshness.unknown')
      }));
      var actionKey = data.readiness.state !== 'ready' ? 'design.openFigma' :
        data.freshness.state === 'incompatible-source' ? 'design.reviewSource' : 'design.openFigma';
      banner.appendChild(el('a', { class: 'btn', href: '#figma', text: t(actionKey) }));
      root.appendChild(banner);
    } else if (data.readiness.actions.reviewTarget) {
      var ready = el('div', { class: 'design-readiness design-readiness--ready' });
      ready.appendChild(el('strong', { text: t('design.readiness.ready') }));
      ready.appendChild(el('a', {
        class: 'btn',
        href: data.readiness.actions.reviewTarget,
        text: t(data.readiness.actions.reviewTarget.indexOf('surfaces') >= 0
          ? 'design.reviewSurfaces' : 'design.reviewComponents')
      }));
      root.appendChild(ready);
    }
    var metrics = el('div', { class: 'design-metrics' });
    [
      ['figmaSyncFreshness', 'design.metric.freshness', ''],
      ['tokenCoverage', 'design.metric.coverage', '%'],
      ['componentCoverage', 'design.metric.componentCoverage', '%'],
      ['driftedSurfaces', 'design.metric.driftedSurfaces', ''],
      ['openTasks', 'design.metric.openTasks', '']
    ].forEach(function (row) {
      var card = el('div', { class: 'design-metric' });
      card.appendChild(el('span', { class: 'design-metric__label', text: t(row[1]) }));
      card.appendChild(el('strong', { class: 'design-metric__value', text: metricValue(data.metrics[row[0]], row[2]) }));
      if (row[0] === 'tokenCoverage' || row[0] === 'componentCoverage') {
        // The comparator's coverage object rides along as metric.detail:
        // surface the exact matched/denominator pair, not just the percent.
        var coverage = data.metrics[row[0]] && data.metrics[row[0]].detail;
        if (coverage && Number.isFinite(coverage.denominator)) {
          card.appendChild(el('span', {
            class: 'design-metric__sub',
            text: t('design.metric.coverageDetail', { matched: coverage.matched, denominator: coverage.denominator })
          }));
        }
      }
      metrics.appendChild(card);
    });
    root.appendChild(metrics);

    var grid = el('div', { class: 'design-overview-grid' });
    var attention = section(t('design.attention'));
    if (!data.attentionItems.length) attention.appendChild(el('p', { class: 'design-state', text: t('design.attentionEmpty') }));
    data.attentionItems.slice(0, 10).forEach(function (item) {
      var button = el('button', { type: 'button', class: 'design-attention-item' });
      button.appendChild(el('strong', { text: findingTitle(item) }));
      button.appendChild(el('span', { text: findingDetail(item) }));
      button.addEventListener('click', function () {
        context.openEntity(item.entityType, item.entityId, button);
      });
      attention.appendChild(button);
    });
    grid.appendChild(attention);

    var recent = section(t('design.recent'));
    if (!data.recentChanges.length) recent.appendChild(el('p', { class: 'design-state', text: t('design.historyStarts') }));
    data.recentChanges.slice(0, 10).forEach(function (item) {
      var row = el('button', {
        type: 'button', class: 'design-recent-item',
        disabled: item.kind === 'removed',
        text: (item.entity && item.entity.name || item.id) + ' · ' +
          designFilters.localizedEnum('design.change.', item.kind, 'design.change.unknown')
      });
      if (item.entity && item.kind !== 'removed') row.addEventListener('click', function () {
        context.openEntity(item.entity.entityType, item.id, row);
      });
      recent.appendChild(row);
    });
    grid.appendChild(recent);

    var tasks = section(t('design.openTasks'));
    if (!data.openTasks.length) tasks.appendChild(el('p', { class: 'design-state', text: t('design.openTasksEmpty') }));
    data.openTasks.slice(0, 10).forEach(function (item) {
      tasks.appendChild(el('a', { class: 'design-task-link', href: '#board?task=' + encodeURIComponent(item.stem), text: item.title }));
    });
    grid.appendChild(tasks);

    var sync = section(t('design.lastSync'));
    sync.appendChild(el('p', {
      text: data.lastSync
        ? designFilters.localizedEnum(
          'figma.history.result.', data.lastSync.result, 'figma.history.result.unknown'
        ) + ' · ' + timestamp(data.lastSync.finishedAt || data.lastSync.startedAt)
        : t('design.neverSynced')
    }));
    if (data.lastSync && data.lastSync.errorCode) {
      sync.appendChild(el('p', {
        class: 'design-state',
        text: syncFailureText(data.lastSync.errorCode)
      }));
    }
    if (data.analysisLimitations.length) {
      var details = el('details', { class: 'design-limitations' });
      details.appendChild(el('summary', { text: t('design.limitations', { count: data.analysisLimitations.length }) }));
      var list = el('ul');
      data.analysisLimitations.forEach(function (item) {
        list.appendChild(el('li', {
          text: designFilters.localizedEnum('design.limitation.', item, 'design.limitation.unknown')
        }));
      });
      details.appendChild(list); sync.appendChild(details);
    }
    grid.appendChild(sync);
    root.appendChild(grid);
  });
}

export const designOverview = { render: render };
