import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { designFilters } from './filters.js';
import { designAnalysisBanner } from './analysis-banner.js';

var el = dom.el;
function t(key, params) { return i18n.t(key, params); }
var KNOWN_BLOCKERS = Object.assign(Object.create(null), {
  COMPONENT_DESIGN_ABSENCE_UNPROVEN: 1, COMPONENT_DESIGN_SCOPE_CHANGED: 1
});

function statusPill(status) {
  return el('span', {
    class: 'design-status design-component-status design-component-status--' + designFilters.cssToken(status),
    text: designFilters.componentStatusText(status), attrs: { role: 'cell' }
  });
}
function changedSideBadge(side) {
  return el('span', {
    class: 'design-changed-side design-changed-side--' + designFilters.cssToken(side),
    text: designFilters.changedSideText(side), attrs: { role: 'cell' }
  });
}
function classificationLabel(value) {
  return ['unclassified', 'intentionally-project-only', 'external', 'deprecated',
    'superseded', 'deferred', 'ignored'].indexOf(value) >= 0
    ? t('design.projectComponent.' + value) : t('design.unknown');
}
function visualLabel(value) {
  return ['not-run', 'not-applicable', 'insufficient-evidence', 'matched', 'review-required', 'drifted'].indexOf(value) >= 0
    ? t('design.componentVisual.' + value) : t('design.unknown');
}
function dimensionsSummary(dimensions) {
  if (!dimensions) return '';
  return [
    t('design.components.dimensions.properties', {
      mapped: dimensions.properties.mapped, total: dimensions.properties.designTotal
    }),
    t('design.components.dimensions.variants', {
      expressible: dimensions.variants.expressible, total: dimensions.variants.designTotal
    }),
    t('design.components.dimensions.slots', {
      mapped: dimensions.slots.mapped, total: dimensions.slots.designTotal
    }),
    visualLabel(dimensions.visual)
  ].join(' · ');
}
function selectionCell(row, context) {
  var cell = el('div', { class: 'design-table__select', attrs: { role: 'cell' } });
  if (row.findingId && !row.openTask) {
    var check = el('input', {
      type: 'checkbox', class: 'choice-input', checked: context.selected.has(row.findingId),
      attrs: {
        'aria-label': t('design.selectFinding', { name: row.name }),
        'data-design-focus': 'component-finding:' + row.findingId
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
    var line = el('div', { class: 'design-table__row design-table__row--component', attrs: { role: 'row' } });
    line.appendChild(selectionCell(row, context));
    var main = el('div', { class: 'design-table__main', attrs: { role: 'rowheader' } });
    var open = el('button', {
      type: 'button', class: 'design-token-open',
      attrs: {
        'aria-label': t('design.components.openDetail', { name: row.name }),
        'data-design-focus': 'component:' + row.id
      }
    });
    open.appendChild(el('strong', { text: row.name }));
    open.appendChild(el('span', {
      text: designFilters.componentKindText(row.kind) +
        (row.implementationSummary ? ' · ' + row.implementationSummary : '')
    }));
    var dimensions = dimensionsSummary(row.dimensions);
    if (dimensions) open.appendChild(el('span', { text: dimensions }));
    if (row.statusDetail) open.appendChild(el('span', { text: row.statusDetail }));
    open.addEventListener('click', function () { context.openEntity('component', row.id, open); });
    main.appendChild(open);
    line.appendChild(main);
    line.appendChild(statusPill(row.status));
    line.appendChild(changedSideBadge(row.changedSide));
    line.appendChild(taskCell(row));
    table.appendChild(line);
  });
}
function appendProjectRows(table, rows, context) {
  rows.forEach(function (row) {
    var line = el('div', { class: 'design-table__row design-table__row--project-component', attrs: { role: 'row' } });
    line.appendChild(selectionCell(row, context));
    var main = el('div', { class: 'design-table__main', attrs: { role: 'rowheader' } });
    var open = el('button', {
      type: 'button', class: 'design-token-open',
      attrs: {
        'aria-label': t('design.components.openDetail', { name: row.name }),
        'data-design-focus': 'project-component:' + row.id
      }
    });
    open.appendChild(el('strong', { text: row.name }));
    open.appendChild(el('span', {
      text: [row.kind, row.adapterId, row.platform].filter(Boolean).join(' · ')
    }));
    if (row.sourcePath) open.appendChild(el('span', {
      text: row.sourcePath + (row.sourceSymbol ? ' · ' + row.sourceSymbol : '')
    }));
    open.addEventListener('click', function () { context.openEntity('project-component', row.id, open); });
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
    class: 'design-view-toggle design-component-scope',
    attrs: { role: 'group', 'aria-label': t('design.components.scopeAria') }
  });
  var projectLabel = t('design.projectOnly.title') +
    (Number.isFinite(projectOnlyTotal) ? ' (' + projectOnlyTotal + ')' : '');
  [['', t('design.components.scope.design')], ['project-only', projectLabel]].forEach(function (option) {
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
function coverageStrip(coverage) {
  var wrap = el('div', { class: 'design-coverage' });
  if (!coverage) {
    wrap.appendChild(el('strong', { text: t('design.componentCoverage.unknown') }));
    return wrap;
  }
  wrap.appendChild(el('strong', { text: coverage.percent === null
    ? t('design.componentCoverage.empty', { denominator: coverage.denominator })
    : t('design.componentCoverage.summary', {
      matched: coverage.matched, denominator: coverage.denominator, percent: coverage.percent
    }) }));
  wrap.appendChild(el('span', { text: t('design.componentCoverage.breakdown', {
    drifted: coverage.drifted, unmapped: coverage.unmapped, ambiguous: coverage.ambiguous,
    missingInProject: coverage.missingInProject, missingInDesign: coverage.missingInDesign,
    designOnly: coverage.designOnly, unsupported: coverage.unsupported
  }) }));
  // Ignored and project-only counts live OUTSIDE the coverage denominator and
  // are reported separately on purpose: they must never dilute design coverage.
  wrap.appendChild(el('span', {
    class: 'design-coverage__aside', text: t('design.componentCoverage.ignored', { count: coverage.ignored })
  }));
  wrap.appendChild(el('span', {
    class: 'design-coverage__aside', text: t('design.componentCoverage.projectOnly', { count: coverage.projectOnly })
  }));
  if (coverage.partial) wrap.appendChild(el('span', {
    class: 'design-coverage__partial', text: t('design.componentCoverage.partial')
  }));
  return wrap;
}
function blockersBlock(analysis, context) {
  var blockers = analysis && Array.isArray(analysis.blockers) ? analysis.blockers : [];
  if (!blockers.length) return null;
  var block = el('div', { class: 'design-component-blockers', attrs: { role: 'note' } });
  block.appendChild(el('strong', { text: t('design.componentBlockers.title', { count: blockers.length }) }));
  var list = el('ul');
  blockers.slice(0, 32).forEach(function (blocker) {
    var code = blocker && blocker.code;
    var label = code && KNOWN_BLOCKERS[code]
      ? t('design.componentBlocker.' + code)
      : t('design.componentBlocker.generic');
    list.appendChild(el('li', { text: label }));
  });
  block.appendChild(list);
  // The scope-changed blocker has exactly one sanctioned remedy: the explicit
  // onboard-fresh mapping operation, drafted into the review basket.
  if (blockers.some(function (blocker) { return blocker && blocker.code === 'COMPONENT_DESIGN_SCOPE_CHANGED'; }) &&
      typeof context.addComponentOperation === 'function') {
    var onboard = el('button', {
      type: 'button', class: 'btn btn--small', text: t('design.componentMapping.onboardFresh')
    });
    onboard.addEventListener('click', function () {
      context.addComponentOperation({ op: 'onboard-fresh' }, t('design.componentMapping.onboardFresh'));
    });
    block.appendChild(onboard);
  }
  return block;
}
function tableHead(projectScope) {
  var cells = [
    el('span', { class: 'design-table__select', attrs: { role: 'columnheader', 'aria-label': t('design.field.selection') } }),
    el('span', { class: 'design-table__main', attrs: { role: 'columnheader' }, text: t('design.field.component') })
  ];
  if (projectScope) {
    cells.push(el('span', { class: 'design-table__head-status', attrs: { role: 'columnheader' }, text: t('design.field.classification') }));
  } else {
    cells.push(el('span', { class: 'design-table__head-status', attrs: { role: 'columnheader' }, text: t('design.field.status') }));
    cells.push(el('span', { class: 'design-table__head-status', attrs: { role: 'columnheader' }, text: t('design.field.changedSide') }));
  }
  cells.push(el('span', { class: 'design-table__task', attrs: { role: 'columnheader' }, text: t('design.field.task') }));
  return el('div', {
    class: 'design-table__row design-table__row--head' + (projectScope ? ' design-table__row--project-component' : ' design-table__row--component'),
    attrs: { role: 'row' }
  }, cells);
}
function render(root, context) {
  var projectScope = context.state.scope === 'project-only';
  var endpoint = projectScope ? '/api/design/components/project-only' : '/api/design/components';
  return designFilters.request(endpoint + '?' + designFilters.query(context.state)).then(function (data) {
    if (!context.isCurrent()) return;
    context.adopt(data);
    if (typeof context.adoptComponentAnalysis === 'function') context.adoptComponentAnalysis(data.analysis);
    if (!context.shouldRender(data)) return;
    root.replaceChildren();
    root.appendChild(designAnalysisBanner.element(
      'components', data.analysis, context.comparisonState(data.comparison),
      context.startComponentComparison
    ));
    root.appendChild(scopeToggle(context, projectScope ? data.total : data.projectOnlyTotal));
    if (!projectScope) root.appendChild(coverageStrip(data.coverage));
    var blockers = blockersBlock(data.analysis, context);
    if (blockers) root.appendChild(blockers);
    designFilters.appendLimitations(root, data.limitations);
    if (!data.items.length) {
      root.appendChild(el('p', {
        class: 'design-state',
        text: t(projectScope ? 'design.empty.projectOnlyComponents' : 'design.empty.components')
      }));
      return;
    }
    var table = el('div', {
      class: 'design-table design-table--components',
      attrs: {
        role: 'table', 'aria-label': t('design.tab.components'),
        'aria-colcount': projectScope ? '4' : '5'
      }
    });
    table.appendChild(tableHead(projectScope));
    var append = projectScope ? appendProjectRows : appendRows;
    append(table, data.items, context);
    root.appendChild(table);
    addPager(root, table, data, context, endpoint, append);
  });
}

export const designComponents = { render: render };
