import { dom } from '../dom.js';

var el = dom.el;

function metric(t, key, value) {
  var card = el('div', { class: 'archmap-stat architecture-metric' });
  card.appendChild(el('span', {
    class: 'archmap-stat__value',
    text: value === null || value === undefined ? t('archmap.unknown') : String(value)
  }));
  card.appendChild(el('span', { class: 'archmap-stat__label', text: t(key) }));
  return card;
}

export function renderArchitectureOverview(host, data, options) {
  var t = options.t;
  var header = el('div', { class: 'architecture-header' });
  var title = el('div', { class: 'architecture-header__title' });
  title.appendChild(el('h2', { class: 'panel-title', text: t('archmap.title') }));
  title.appendChild(el('p', { class: 'panel-lead', text: t('archmap.lead') }));
  header.appendChild(title);
  var actions = el('div', { class: 'architecture-header__actions' });
  if (data && data.freshness) {
    actions.appendChild(el('span', {
      class: 'architecture-freshness architecture-freshness--' + data.freshness.status,
      text: t('archmap.freshness.' + data.freshness.status)
    }));
  }
  if (data && data.canGenerate) {
    if (!data.present || data.freshness.status === 'stale') {
      var generate = el('button', {
        type: 'button', class: 'btn btn--primary',
        text: t(data.present ? 'archmap.refreshMap' : 'archmap.generateMap'),
        attrs: { 'data-architecture-control': 'generate-map' }
      });
      generate.addEventListener('click', function () {
        options.onGenerate(data.present ? 'stale' : 'missing');
      });
      actions.appendChild(generate);
    }
    if (data.present) {
      var refresh = el('button', {
        type: 'button', class: 'btn',
        text: t('archmap.refresh'),
        attrs: { 'data-architecture-control': 'refresh-map' }
      });
      refresh.addEventListener('click', function () { options.onGenerate('manual'); });
      actions.appendChild(refresh);
    }
  }
  if (data && data.present) {
    var view = el('div', {
      class: 'architecture-view-switch',
      attrs: { role: 'group', 'aria-label': t('archmap.view') }
    });
    (options.allowGraph ? ['list', 'graph'] : ['list']).forEach(function (mode) {
      var button = el('button', {
        type: 'button',
        class: 'btn btn--small' + (options.view === mode ? ' is-active' : ''),
        text: t('archmap.view.' + mode),
        attrs: {
          'aria-pressed': options.view === mode ? 'true' : 'false',
          'data-architecture-control': 'view-' + mode
        }
      });
      button.addEventListener('click', function () { options.onView(mode); });
      view.appendChild(button);
    });
    actions.appendChild(view);
  }
  header.appendChild(actions);
  host.appendChild(header);

  if (!data || !data.present) return;
  var summary = data.summary || {};
  var metrics = el('div', { class: 'archmap-summary architecture-summary' });
  [
    ['archmap.stat.modules', summary.modules],
    ['archmap.stat.features', summary.features],
    ['archmap.stat.screens', summary.screens],
    ['archmap.stat.dataSources', summary.dataSources],
    ['archmap.stat.databaseEntities', summary.databaseEntities],
    ['archmap.stat.findings', summary.findingsBySeverity
      ? summary.findingsBySeverity.error + summary.findingsBySeverity.warning + summary.findingsBySeverity.info
      : null]
  ].forEach(function (row) { metrics.appendChild(metric(t, row[0], row[1])); });
  host.appendChild(metrics);
  var meta = el('p', {
    class: 'archmap-freshness',
    text: [
      data.freshness && data.freshness.generatedAt,
      data.generatedAtRevision ? t('archmap.revision', { revision: data.generatedAtRevision.slice(-12) }) : null
    ].filter(Boolean).join(' · ')
  });
  host.appendChild(meta);
  if (data.analysis && data.analysis.status === 'partial') {
    host.appendChild(el('div', {
      class: 'banner banner--warn',
      text: t('archmap.analysisPartial')
    }));
  } else if (summary.findingsBySeverity &&
      summary.findingsBySeverity.error + summary.findingsBySeverity.warning + summary.findingsBySeverity.info === 0) {
    host.appendChild(el('div', {
      class: 'banner banner--success',
      text: t('archmap.analysisClean')
    }));
  }
  if (data.analysis && data.analysis.limitations && data.analysis.limitations.length) {
    var limitations = el('details', { class: 'architecture-limitations' });
    limitations.appendChild(el('summary', {
      text: t('archmap.limitations', { count: data.analysis.limitations.length })
    }));
    var list = el('ul');
    data.analysis.limitations.forEach(function (item) {
      list.appendChild(el('li', { text: t('archmap.limitation.' + item) === 'archmap.limitation.' + item
        ? item : t('archmap.limitation.' + item) }));
    });
    limitations.appendChild(list);
    host.appendChild(limitations);
  }
  if (data.topFindings && data.topFindings.length) {
    var findings = el('section', { class: 'architecture-overview-list' });
    findings.appendChild(el('h3', { text: t('archmap.topFindings') }));
    data.topFindings.forEach(function (finding) {
      var button = el('button', {
        type: 'button',
        class: 'architecture-overview-row architecture-overview-row--' + finding.severity,
        attrs: { 'data-architecture-control': 'overview-finding-' + finding.id }
      });
      button.appendChild(el('span', {
        class: 'architecture-severity',
        text: t('archmap.severity.' + finding.severity)
      }));
      button.appendChild(el('span', { text: finding.title }));
      button.addEventListener('click', function () {
        if (finding.affectedNodeIds.length) options.onSelect(finding.affectedNodeIds[0]);
      });
      findings.appendChild(button);
    });
    host.appendChild(findings);
  }
  if (data.unownedScreens && data.unownedScreens.length) {
    var unowned = el('section', { class: 'architecture-overview-list' });
    unowned.appendChild(el('h3', { text: t('archmap.unownedScreens') }));
    data.unownedScreens.forEach(function (node) {
      var button = el('button', {
        type: 'button', class: 'architecture-overview-row', text: node.name,
        attrs: { 'data-architecture-control': 'overview-node-' + node.id }
      });
      button.addEventListener('click', function () { options.onSelect(node.id); });
      unowned.appendChild(button);
    });
    if (data.unownedScreensTruncated) {
      unowned.appendChild(el('p', {
        class: 'panel-lead',
        text: t('archmap.unownedScreensTruncated', {
          count: data.unownedScreens.length,
          total: data.unownedScreensTotal
        })
      }));
    }
    host.appendChild(unowned);
  }
}
