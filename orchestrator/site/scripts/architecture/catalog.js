import { dom } from '../dom.js';

var el = dom.el;
var TABS = [
  { id: 'modules', kind: 'module' },
  { id: 'features', kind: 'feature' },
  { id: 'screens', kind: 'screen' },
  { id: 'data', kind: 'data' },
  { id: 'findings', kind: null }
];
var searchTimer = null;
var ownershipTimer = null;

function enumLabel(t, prefix, value) {
  var key = prefix + value;
  var translated = t(key);
  return translated === key ? t('archmap.unknown') : translated;
}

function selectControl(focusKey, label, value, options, onChange) {
  var wrap = el('label', { class: 'architecture-filter' });
  wrap.appendChild(el('span', { class: 'sr-only', text: label }));
  var select = el('select', {
    class: 'input architecture-filter__select',
    attrs: {
      'aria-label': label,
      'data-architecture-control': focusKey
    }
  });
  options.forEach(function (row) {
    select.appendChild(el('option', { value: row.value, text: row.label }));
  });
  select.value = value || '';
  select.addEventListener('change', function () { onChange(select.value); });
  wrap.appendChild(select);
  return wrap;
}

export function tabKind(tab) {
  var row = TABS.find(function (item) { return item.id === tab; });
  return row ? row.kind : 'module';
}

export function renderArchitectureControls(host, state, options) {
  var t = options.t;
  var tabs = el('div', {
    class: 'architecture-tabs',
    attrs: { role: 'tablist', 'aria-label': t('archmap.catalog') }
  });
  var tabButtons = [];
  TABS.forEach(function (tab, index) {
    var button = el('button', {
      type: 'button',
      class: 'architecture-tab' + (state.tab === tab.id ? ' is-active' : ''),
      text: t('archmap.tab.' + tab.id),
      attrs: {
        role: 'tab',
        'aria-selected': state.tab === tab.id ? 'true' : 'false',
        tabindex: state.tab === tab.id ? '0' : '-1',
        'data-architecture-control': 'tab-' + tab.id
      }
    });
    button.addEventListener('click', function () { options.onChange({ tab: tab.id }); });
    button.addEventListener('keydown', function (event) {
      var target = null;
      if (event.key === 'ArrowLeft') target = (index + TABS.length - 1) % TABS.length;
      else if (event.key === 'ArrowRight') target = (index + 1) % TABS.length;
      else if (event.key === 'Home') target = 0;
      else if (event.key === 'End') target = TABS.length - 1;
      if (target === null) return;
      event.preventDefault();
      tabButtons[target].focus();
      options.onChange({ tab: TABS[target].id });
    });
    tabButtons.push(button);
    tabs.appendChild(button);
  });
  host.appendChild(tabs);
  var controls = el('div', { class: 'architecture-controls' });
  var searchWrap = el('label', { class: 'architecture-search' });
  searchWrap.appendChild(el('span', { class: 'sr-only', text: t('archmap.search') }));
  var search = el('input', {
    type: 'search',
    class: 'input',
    value: state.search || '',
    placeholder: t('archmap.searchPlaceholder'),
    attrs: {
      'aria-label': t('archmap.search'),
      'data-architecture-control': 'search'
    }
  });
  search.addEventListener('input', function () {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(function () {
      options.onChange({ search: search.value });
    }, 180);
  });
  searchWrap.appendChild(search);
  if (state.search) {
    var clear = el('button', {
      type: 'button', class: 'architecture-search__clear',
      text: '×', attrs: {
        'aria-label': t('archmap.clearSearch'),
        'data-architecture-control': 'clear-search'
      }
    });
    clear.addEventListener('click', function () {
      clearTimeout(searchTimer);
      options.onChange({ search: '' });
      requestAnimationFrame(function () {
        var replacement = host.querySelector(
          '[data-architecture-control="search"]'
        );
        if (replacement) replacement.focus();
      });
    });
    searchWrap.appendChild(clear);
  }
  controls.appendChild(searchWrap);
  controls.appendChild(selectControl('platform', t('archmap.platform'), state.platform, [
    { value: '', label: t('archmap.platform.all') },
    { value: 'shared', label: t('archmap.platform.shared') },
    { value: 'android', label: t('archmap.platform.android') },
    { value: 'ios', label: t('archmap.platform.ios') },
    { value: 'tooling', label: t('archmap.platform.tooling') },
    { value: 'unknown', label: t('archmap.platform.unknown') }
  ], function (value) { options.onChange({ platform: value }); }));
  controls.appendChild(selectControl('layer', t('archmap.layer'), state.layer, [
    { value: '', label: t('archmap.layer.all') },
    { value: 'ui', label: t('archmap.layer.ui') },
    { value: 'domain', label: t('archmap.layer.domain') },
    { value: 'data', label: t('archmap.layer.data') },
    { value: 'infrastructure', label: t('archmap.layer.infrastructure') },
    { value: 'build', label: t('archmap.layer.build') },
    { value: 'unknown', label: t('archmap.layer.unknown') }
  ], function (value) { options.onChange({ layer: value }); }));
  var ownership = el('input', {
    type: 'text',
    class: 'input architecture-owner-filter',
    value: state.ownership || '',
    placeholder: t('archmap.ownership'),
    attrs: {
      'aria-label': t('archmap.ownership'),
      'data-architecture-control': 'ownership'
    }
  });
  ownership.addEventListener('input', function () {
    clearTimeout(ownershipTimer);
    ownershipTimer = setTimeout(function () {
      options.onChange({ ownership: ownership.value.trim() });
    }, 180);
  });
  controls.appendChild(ownership);
  if (state.tab === 'findings') {
    controls.appendChild(selectControl('severity', t('archmap.severity'), state.severity, [
      { value: '', label: t('archmap.severity.all') },
      { value: 'error', label: t('archmap.severity.error') },
      { value: 'warning', label: t('archmap.severity.warning') },
      { value: 'info', label: t('archmap.severity.info') }
    ], function (value) { options.onChange({ severity: value }); }));
    controls.appendChild(selectControl('finding-type', t('archmap.findingType'), state.findingType, [
      { value: '', label: t('archmap.findingType.all') },
      { value: 'dependency-cycle', label: t('archmap.findingType.dependency-cycle') },
      { value: 'forbidden-dependency', label: t('archmap.findingType.forbidden-dependency') },
      { value: 'orphan-module', label: t('archmap.findingType.orphan-module') },
      { value: 'unused-repository', label: t('archmap.findingType.unused-repository') },
      { value: 'screen-without-owner', label: t('archmap.findingType.screen-without-owner') }
    ], function (value) { options.onChange({ findingType: value }); }));
    controls.appendChild(selectControl('confidence', t('archmap.confidence'), state.confidence, [
      { value: '', label: t('archmap.confidence.all') },
      { value: 'exact', label: t('archmap.confidence.exact') },
      { value: 'derived', label: t('archmap.confidence.derived') },
      { value: 'heuristic', label: t('archmap.confidence.heuristic') }
    ], function (value) { options.onChange({ confidence: value }); }));
  }
  var changed = el('label', {
    class: 'architecture-changed-filter',
    attrs: {
      title: options.changedAvailable ? '' : t('archmap.changedUnavailable')
    }
  });
  var checkbox = el('input', {
    type: 'checkbox',
    class: 'choice-input',
    checked: state.changed === true,
    disabled: !options.changedAvailable,
    attrs: {
      title: options.changedAvailable ? '' : t('archmap.changedUnavailable'),
      'data-architecture-control': 'changed'
    }
  });
  checkbox.addEventListener('change', function () { options.onChange({ changed: checkbox.checked }); });
  changed.appendChild(checkbox);
  changed.appendChild(document.createTextNode(t('archmap.changedLatestTask')));
  controls.appendChild(changed);
  host.appendChild(controls);
}

export function renderNodeCatalog(host, response, options) {
  var t = options.t;
  if (!response || !response.rows || !response.rows.length) {
    host.appendChild(el('div', { class: 'architecture-empty', text: t('archmap.noResults') }));
    return;
  }
  var list = el('div', {
    class: 'architecture-list',
    attrs: { role: 'list', 'aria-label': t('archmap.catalog') }
  });
  response.rows.forEach(function (node) {
    var row = el('button', {
      type: 'button',
      class: 'architecture-row',
      attrs: {
        role: 'listitem',
        'data-architecture-control': 'catalog-node-' + node.id
      }
    });
    var identity = el('span', { class: 'architecture-row__identity' });
    identity.appendChild(el('strong', { text: node.name }));
    identity.appendChild(el('code', { text: node.id }));
    row.appendChild(identity);
    var meta = el('span', { class: 'architecture-row__meta' });
    meta.appendChild(el('span', { class: 'archmap-chip', text: t('archmap.kind.' + node.kind) }));
    meta.appendChild(el('span', {
      class: 'archmap-chip',
      text: enumLabel(t, 'archmap.platform.', node.platform)
    }));
    meta.appendChild(el('span', {
      class: 'archmap-chip',
      text: enumLabel(t, 'archmap.layer.', node.layer)
    }));
    if (node.owner) meta.appendChild(el('span', {
      class: 'archmap-chip', text: t('archmap.ownerValue', { owner: node.owner.name })
    }));
    row.appendChild(meta);
    if (node.path) row.appendChild(el('span', {
      class: 'architecture-row__path', text: node.path
    }));
    row.addEventListener('click', function () { options.onSelect(node.id); });
    list.appendChild(row);
  });
  host.appendChild(list);
  if (response.nextCursor) {
    var more = el('button', {
      type: 'button', class: 'btn architecture-load-more',
      text: t('archmap.loadMore')
    });
    more.addEventListener('click', function () { options.onMore(response.nextCursor); });
    host.appendChild(more);
  }
}
