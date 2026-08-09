import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { requestJson, errorCode } from '../data/request-json.js';

var el = dom.el;
function t(key, params) { return i18n.t(key, params); }
var TABS = Object.assign(Object.create(null), {
  overview: 1, tokens: 1, components: 1, surfaces: 1
});
var TOKEN_STATUSES = [
  'matched', 'value-drift', 'missing-in-project', 'not-compared', 'unbound',
  'ambiguous-binding', 'authority-conflict', 'context-map-required',
  'source-conflict', 'unsupported', 'not-observed', 'ignored'
];
var TOKEN_CHANGED_SIDES = ['none', 'design', 'project', 'both', 'mapping', 'unknown'];
var TOKEN_KINDS = ['color', 'number', 'string', 'boolean', 'unsupported'];
var TOKEN_SOURCE_STATUSES = ['active', 'retired', 'scope-mismatch'];
var COMPONENT_STATUSES = [
  'matched', 'drifted', 'unmapped', 'ambiguous', 'missing-in-project',
  'missing-in-design', 'design-only', 'ignored', 'unsupported'
];
var COMPONENT_MAPPING_STATES = [
  'active', 'target-out-of-scope', 'incompatible', 'orphaned-project', 'orphaned-design'
];
var ENTITY_PREFIXES = Object.assign(Object.create(null), {
  component: 'cmp', 'project-component': 'cmpp', surface: 'srf', token: 'tok', 'project-token': 'tokp'
});
function allowed(value, values) {
  return values.indexOf(value) >= 0 ? value : '';
}
function utf8Bounded(value, maxBytes) {
  var out = '', used = 0;
  var normalized = String(value || '').normalize('NFC').replace(/[\u0000-\u001f\u007f]/g, '');
  Array.from(normalized).some(function (character) {
    var point = character.codePointAt(0);
    var bytes = point <= 0x7f ? 1 : point <= 0x7ff ? 2 : point <= 0xffff ? 3 : 4;
    if (used + bytes > maxBytes) return true;
    out += character; used += bytes; return false;
  });
  return out;
}

// Translate only a registered closed enum key. Unknown/future values never
// become raw i18n keys in the UI; each caller supplies a domain-specific,
// localized unknown state.
export function localizedEnum(prefix, value, unknownKey, params) {
  var exact = typeof value === 'string' && /^[A-Za-z0-9][A-Za-z0-9_.-]{0,99}$/.test(value)
    ? value : '';
  if (!exact) return t(unknownKey || 'design.unknown', params);
  var key = prefix + exact;
  var translated = t(key, params);
  return translated === key ? t(unknownKey || 'design.unknown', params) : translated;
}

function readState() {
  var hash = (location.hash || '#design').replace(/^#/, '');
  var raw = hash === 'design' ? '' : hash.indexOf('design?') === 0 ? hash.slice('design?'.length) : '';
  var params = new URLSearchParams(raw);
  var tab = params.get('tab') || 'overview';
  if (!Object.prototype.hasOwnProperty.call(TABS, tab)) tab = 'overview';
  var rawScope = (tab === 'tokens' || tab === 'components') ? params.get('scope') || '' : '';
  var scope = rawScope === 'project-only' || tab === 'tokens' && rawScope === 'sources' ? rawScope : '';
  var typeValues = tab === 'tokens' ? TOKEN_KINDS :
    tab === 'surfaces' ? ['screen', 'dialog', 'overlay'] : [];
  var statusValues = tab === 'tokens' && scope === 'sources' ? TOKEN_SOURCE_STATUSES :
    tab === 'tokens' ? TOKEN_STATUSES :
    tab === 'components' ? COMPONENT_STATUSES : ['healthy', 'drifted', 'missing', 'unknown'];
  var locale = params.get('locale') || '';
  if (!/^[A-Za-z0-9][A-Za-z0-9_-]{0,39}$/.test(locale)) locale = '';
  var platform = tab === 'components'
    ? (/^[a-z][a-z0-9-]{0,59}$/.test(params.get('platform') || '') ? params.get('platform') : '')
    : allowed(params.get('platform') || '', ['shared', 'android', 'ios']);
  var entityType = allowed(params.get('entityType') || '', ['component', 'project-component', 'surface', 'token', 'project-token']);
  var entity = params.get('entity') || '';
  if (!entityType || !new RegExp('^' + ENTITY_PREFIXES[entityType] + '-[a-f0-9]{24}$').test(entity)) {
    entityType = ''; entity = '';
  }
  return {
    tab: tab, q: utf8Bounded(params.get('q') || '', 200),
    status: allowed(params.get('status') || '', statusValues),
    theme: allowed(params.get('theme') || '', ['light', 'dark']),
    platform: platform,
    locale: locale, type: allowed(params.get('type') || '', typeValues),
    changedSide: tab === 'tokens' || tab === 'components'
      ? allowed(params.get('changedSide') || '', TOKEN_CHANGED_SIDES) : '',
    mappingState: tab === 'components' ? allowed(params.get('mappingState') || '', COMPONENT_MAPPING_STATES) : '',
    scope: scope,
    changed: params.get('changed') === 'true' ? 'true' : '',
    hasTask: params.get('hasTask') === 'true' ? 'true' : '',
    view: params.get('view') === 'list' ? 'list' : 'gallery',
    entity: entity, entityType: entityType
  };
}
function writeState(state, replace) {
  var params = new URLSearchParams();
  ['tab', 'q', 'status', 'theme', 'platform', 'locale', 'type', 'changedSide', 'mappingState', 'scope', 'changed', 'hasTask', 'view', 'entity', 'entityType'].forEach(function (key) {
    if (state[key] && !(key === 'tab' && state[key] === 'overview') && !(key === 'view' && state[key] === 'gallery')) {
      params.set(key, state[key]);
    }
  });
  var target = '#design' + (params.toString() ? '?' + params.toString() : '');
  try {
    if (replace) history.replaceState(null, '', target); else history.pushState(null, '', target);
  } catch (error) { location.hash = target.slice(1); }
}
function option(value, key) { return el('option', { value: value, text: t(key) }); }
function select(label, value, values, onChange) {
  var wrap = el('label', { class: 'design-filter' });
  wrap.appendChild(el('span', { class: 'design-filter__label', text: label }));
  var input = el('select', { class: 'input' });
  values.forEach(function (row) { input.appendChild(option(row[0], row[1])); });
  input.value = value;
  input.addEventListener('change', function () { onChange(input.value); });
  wrap.appendChild(input);
  return wrap;
}
function textFilter(label, value, placeholder, onChange) {
  var wrap = el('label', { class: 'design-filter' });
  wrap.appendChild(el('span', { class: 'design-filter__label', text: label }));
  var input = el('input', {
    type: 'text', class: 'input', value: value, placeholder: placeholder,
    attrs: { maxlength: '40', pattern: '[A-Za-z0-9][A-Za-z0-9_-]{0,39}' }
  });
  var timer = null;
  input.addEventListener('input', function () {
    clearTimeout(timer); timer = setTimeout(function () { onChange(input.value.trim()); }, 180);
  });
  wrap.appendChild(input);
  return wrap;
}
function element(state, tab, onChange) {
  var form = el('form', { class: 'design-filters', attrs: { role: 'search' } });
  form.addEventListener('submit', function (event) { event.preventDefault(); });
  var search = el('input', {
    type: 'search', class: 'input design-filter__search', value: state.q,
    placeholder: t('design.search'),
    attrs: { 'aria-label': t('design.searchAria'), maxlength: '200' }
  });
  var timer = null;
  search.addEventListener('input', function () {
    clearTimeout(timer); timer = setTimeout(function () {
      onChange({ q: utf8Bounded(search.value, 200) });
    }, 180);
  });
  form.appendChild(search);
  var projectOnly = (tab === 'tokens' || tab === 'components') && state.scope === 'project-only';
  var tokenSources = tab === 'tokens' && state.scope === 'sources';
  if (tab === 'tokens' && !tokenSources) form.appendChild(select(t('design.filter.type'), state.type, [
    ['', 'design.filter.any']
  ].concat(TOKEN_KINDS.map(function (kind) {
    return [kind, 'design.tokenKind.' + kind];
  })), function (value) { onChange({ type: value }); }));
  if (tab === 'surfaces') form.appendChild(select(t('design.filter.type'), state.type, [
    ['', 'design.filter.any'], ['screen', 'design.surface.screen'],
    ['dialog', 'design.surface.dialog'], ['overlay', 'design.surface.overlay']
  ], function (value) { onChange({ type: value }); }));
  if (tokenSources) {
    form.appendChild(select(t('design.filter.status'), state.status, [
      ['', 'design.filter.any']
    ].concat(TOKEN_SOURCE_STATUSES.map(function (status) {
      return [status, 'design.tokenSource.state.' + status];
    })), function (value) { onChange({ status: value }); }));
  } else if (tab === 'tokens' && !projectOnly) {
    form.appendChild(select(t('design.filter.status'), state.status, [
      ['', 'design.filter.any']
    ].concat(TOKEN_STATUSES.map(function (status) {
      return [status, 'design.tokenStatus.' + status];
    })), function (value) { onChange({ status: value }); }));
    form.appendChild(select(t('design.filter.changedSide'), state.changedSide, [
      ['', 'design.filter.any']
    ].concat(TOKEN_CHANGED_SIDES.map(function (side) {
      return [side, 'design.changedSide.' + side];
    })), function (value) { onChange({ changedSide: value }); }));
  }
  if (tab === 'components' && !projectOnly) {
    form.appendChild(select(t('design.filter.status'), state.status, [
      ['', 'design.filter.any']
    ].concat(COMPONENT_STATUSES.map(function (status) {
      return [status, 'design.componentStatus.' + status];
    })), function (value) { onChange({ status: value }); }));
    form.appendChild(select(t('design.filter.changedSide'), state.changedSide, [
      ['', 'design.filter.any']
    ].concat(TOKEN_CHANGED_SIDES.map(function (side) {
      return [side, 'design.changedSide.' + side];
    })), function (value) { onChange({ changedSide: value }); }));
    form.appendChild(select(t('design.filter.mappingState'), state.mappingState, [
      ['', 'design.filter.any']
    ].concat(COMPONENT_MAPPING_STATES.map(function (mappingState) {
      return [mappingState, 'design.mappingState.' + mappingState];
    })), function (value) { onChange({ mappingState: value }); }));
  }
  if (tab === 'components') form.appendChild(textFilter(
    t('design.filter.platform'), state.platform, t('design.platformPlaceholder'),
    function (value) {
      if (!value || /^[a-z][a-z0-9-]{0,59}$/.test(value)) {
        onChange({ platform: value });
      }
    }
  ));
  if (tab === 'surfaces') form.appendChild(select(t('design.filter.status'), state.status, [
    ['', 'design.filter.any'], ['healthy', 'design.status.healthy'],
    ['drifted', 'design.status.drifted'], ['missing', 'design.status.missing'],
    ['unknown', 'design.status.unknown']
  ], function (value) { onChange({ status: value }); }));
  if (tab === 'surfaces') form.appendChild(select(t('design.filter.theme'), state.theme, [
    ['', 'design.filter.any'], ['light', 'design.theme.light'], ['dark', 'design.theme.dark']
  ], function (value) { onChange({ theme: value }); }));
  if (tab === 'surfaces') form.appendChild(select(t('design.filter.platform'), state.platform, [
    ['', 'design.filter.any'], ['shared', 'design.platform.shared'],
    ['android', 'design.platform.android'], ['ios', 'design.platform.ios']
  ], function (value) { onChange({ platform: value }); }));
  if (tab === 'surfaces') form.appendChild(textFilter(
    t('design.filter.locale'), state.locale, t('design.localePlaceholder'),
    function (value) {
      if (!value || /^[A-Za-z0-9][A-Za-z0-9_-]{0,39}$/.test(value)) {
        onChange({ locale: value });
      }
    }
  ));
  if (!tokenSources) form.appendChild(select(t('design.filter.task'), state.hasTask, [
    ['', 'design.filter.any'], ['true', 'design.filter.hasTask']
  ], function (value) { onChange({ hasTask: value }); }));
  if (!projectOnly && !tokenSources) {
    var changed = el('label', { class: 'design-filter design-filter--check' });
    var checkbox = el('input', { type: 'checkbox', class: 'choice-input', checked: state.changed === 'true' });
    checkbox.addEventListener('change', function () { onChange({ changed: checkbox.checked ? 'true' : '' }); });
    changed.appendChild(checkbox);
    changed.appendChild(document.createTextNode(t('design.filter.changed')));
    form.appendChild(changed);
  }
  return form;
}
function query(state, revision) {
  var params = new URLSearchParams();
  var boundedQuery = utf8Bounded(state.q, 200);
  if (boundedQuery) params.set('query', boundedQuery);
  var keys = state.tab === 'tokens' && state.scope === 'sources' ? [] : ['hasTask'];
  if (state.tab === 'tokens') {
    if (state.scope === 'sources') keys.push('status');
    else keys.push('type');
    // The project-only endpoints reject status/changed/changedSide outright.
    if (state.scope !== 'project-only' && state.scope !== 'sources') keys.push('status', 'changed', 'changedSide');
  } else if (state.tab === 'components') {
    keys.push('platform');
    if (state.scope !== 'project-only') keys.push('status', 'changed', 'changedSide', 'mappingState');
  } else {
    keys.push('type', 'status', 'changed');
    if (state.tab === 'surfaces') keys.push('theme', 'platform', 'locale');
  }
  keys.forEach(function (key) {
    if (state[key]) params.set(key, state[key]);
  });
  if (revision) params.set('expectedGenerationRevision', revision);
  return params.toString();
}
export function errorMessage(value) {
  var code = errorCode(value);
  var known = {
    'fetch-failed': 1,
    'invalid-response': 1,
    'http-error': 1,
    'not-found': 1,
    'internal': 1,
    'design-generation-conflict': 1,
    'design-generation-artifact-invalid': 1,
    'design-generation-mode-invalid': 1,
    'design-unavailable': 1,
    'design-cursor-invalid': 1,
    'bad-design-query': 1,
    'bad-design-overview-query': 1,
    'bad-design-detail-query': 1,
    'bad-design-comparison-request': 1,
    'design-comparison-unavailable': 1,
    'design-comparison-contract-invalid': 1,
    'bad-design-image-request': 1,
    'bad-design-image-revision': 1,
    'component-not-found': 1,
    'surface-not-found': 1,
    'surface-image-not-found': 1,
    'component-image-not-found': 1,
    'task-index-unavailable': 1,
    'task-origin-index-partial': 1,
    'design-task-preview-capacity': 1,
    'design-task-preview-expired': 1,
    'design-task-preview-in-flight': 1,
    'finding-set-conflict': 1,
    'finding-stale': 1,
    'task-conflict': 1,
    'design-item-too-large': 1,
    'design-history-entity-limit': 1,
    'bad-json': 1,
    'bad-token-id': 1,
    'token-mapping-read-failed': 1,
    'bad-token-mapping-request': 1,
    'TOKEN_MAPPING_REVISION_CONFLICT': 1,
    'TOKEN_DESIGN_GENERATION_CONFLICT': 1,
    'TOKEN_PROJECT_INVENTORY_CONFLICT': 1,
    'TOKEN_MAPPING_OPERATION_CONFLICT': 1,
    'TOKEN_MAPPING_INVALID': 1,
    'TOKEN_FINDING_STALE': 1,
    'TOKEN_MAPPING_SCOPE_CHANGED': 1,
    'TOKEN_BINDING_TARGET_INCOMPATIBLE': 1,
    'PROJECT_TOKEN_INVENTORY_INCOMPLETE': 1,
    'TOKEN_MAPPING_TARGET_MISSING': 1,
    'TOKEN_MAPPING_CONFLICT': 1,
    'token-mapping-write-failed': 1,
    'TOKEN_MAPPING_PUBLICATION_RECOVERY_REQUIRED': 1,
    'token-comparison-required': 1,
    'token-mapping-writer-busy': 1,
    'token-compare-running': 1,
    'PROJECT_ADAPTERS_UNCONFIGURED': 1,
    'PROJECT_ADAPTER_CONFIG_INVALID': 1,
    'token-not-found': 1,
    'bad-component-mapping-request': 1,
    'bad-component-id': 1,
    'bad-surface-id': 1,
    'component-mapping-read-failed': 1,
    'COMPONENT_MAPPING_REVISION_CONFLICT': 1,
    'COMPONENT_DESIGN_GENERATION_CONFLICT': 1,
    'COMPONENT_PROJECT_INVENTORY_CONFLICT': 1,
    'COMPONENT_MAPPING_OPERATION_CONFLICT': 1,
    'COMPONENT_MAPPING_INVALID': 1,
    'COMPONENT_FINDING_STALE': 1,
    'component-comparison-required': 1,
    'COMPONENT_DESIGN_SCOPE_CHANGED': 1,
    'COMPONENT_DESIGN_SOURCE_NOT_SYNCED': 1,
    'COMPONENT_GENERATION_RESYNC_REQUIRED': 1,
    'COMPONENT_MAPPING_CONFLICT': 1,
    'COMPONENT_MAPPING_TARGET_MISSING': 1,
    'COMPONENT_PROJECT_INVENTORY_INCOMPLETE': 1,
    'COMPONENT_PROPERTY_UNSUPPORTED': 1,
    'component-mapping-writer-busy': 1,
    'component-mapping-write-failed': 1,
    'COMPONENT_MAPPING_PUBLICATION_RECOVERY_REQUIRED': 1,
    'component-compare-running': 1,
    'design-generation-invalid': 1,
    'bad-token-source-query': 1,
    'bad-token-source-cursor': 1,
    'TOKEN_GENERATION_RESYNC_REQUIRED': 1,
    'token-source-management-recovering': 1,
    'bad-token-source-mutation': 1,
    'token-source-idempotency-conflict': 1,
    'token-source-cas-conflict': 1,
    'token-source-not-found': 1,
    'token-source-scope-mismatch': 1,
    'token-source-confirmation-stale': 1,
    'token-source-origin-not-active': 1,
    'token-source-already-retired': 1,
    'token-source-not-retired': 1,
    'TOKEN_SOURCE_MUTATION_FAILED': 1,
    'TOKEN_SOURCE_MUTATION_DIRECTORY_UNSAFE': 1,
    'TOKEN_SOURCE_MUTATION_RECORD_INVALID': 1,
    'TOKEN_SOURCE_MUTATION_RECORD_WRITE_FAILED': 1,
    'TOKEN_SOURCE_MUTATION_RECORD_CONFLICT': 1,
    'TOKEN_SOURCE_MUTATION_ENTRY_INVALID': 1,
    'TOKEN_SOURCE_MUTATION_NO_EFFECT': 1,
    'TOKEN_SOURCE_HEALTH_RECOVERY_REQUIRED': 1,
    'TOKEN_SOURCE_CAS_CONFLICT': 1,
    'TOKEN_SOURCE_NOT_FOUND': 1,
    'TOKEN_SOURCE_REACTIVATION_FAILED': 1,
    'TOKEN_SOURCE_REACTIVATION_START_FAILED': 1,
    'bad-design-task-preview-request': 1,
    'bad-design-task-create-request': 1,
    'bad-design-task-cancel-request': 1,
    'duplicate-finding-id': 1,
    'task-create-invalid-result': 1,
    'task-create-failed': 1,
    'design-task-create-failed': 1,
    'token-binding-context-unavailable': 1,
    'token-binding-evidence-unavailable': 1,
    'token-binding-invalid': 1,
    'token-binding-row-unavailable': 1,
    'token-binding-write-failed': 1,
    'component-binding-adapter-unresolved': 1,
    'component-binding-context-unavailable': 1,
    'component-binding-invalid': 1,
    'component-binding-platform-unresolved': 1,
    'component-binding-relation-unsupported': 1,
    'component-binding-row-unavailable': 1,
    'component-binding-spec-unavailable': 1,
    'component-binding-symbol-unresolved': 1,
    'component-binding-write-failed': 1
  };
  return Object.prototype.hasOwnProperty.call(known, code)
    ? t('design.error.' + code) : t('design.error.generic');
}
function request(url, options) {
  return requestJson(url, Object.assign({ cache: 'no-store', headers: { Accept: 'application/json' } }, options || {})).catch(function (requestError) {
    var code = errorCode(requestError);
    var error = new Error(errorMessage(code));
    error.code = code;
    throw error;
  });
}
function post(url, body) {
  var headers = { Accept: 'application/json', 'content-type': 'application/json' };
  if (window.__ORCHESTRATOR_CSRF__) headers['x-orchestrator-csrf'] = window.__ORCHESTRATOR_CSRF__;
  return request(url, { method: 'POST', headers: headers, body: JSON.stringify(body || {}) });
}
function statusText(status) {
  return ['healthy', 'drifted', 'missing', 'unknown'].indexOf(status) >= 0
    ? t('design.status.' + status) : t('design.status.unknown');
}
function tokenStatusText(status) {
  return TOKEN_STATUSES.indexOf(status) >= 0
    ? t('design.tokenStatus.' + status) : t('design.unknown');
}
function changedSideText(side) {
  return TOKEN_CHANGED_SIDES.indexOf(side) >= 0
    ? t('design.changedSide.' + side) : t('design.changedSide.unknown');
}
function tokenKindText(kind) {
  return TOKEN_KINDS.indexOf(kind) >= 0
    ? t('design.tokenKind.' + kind) : t('design.unknown');
}
function componentStatusText(status) {
  return COMPONENT_STATUSES.indexOf(status) >= 0
    ? t('design.componentStatus.' + status) : t('design.unknown');
}
function componentKindText(kind) {
  return ['component-set', 'component', 'unsupported'].indexOf(kind) >= 0
    ? t('design.componentKind.' + kind) : t('design.unknown');
}
// Stable CSS modifier for enum-ish server strings; unknown values collapse
// to a harmless suffix instead of injecting arbitrary class characters.
function cssToken(value) {
  var out = String(value || '').toLowerCase().replace(/[^a-z0-9-]/g, '');
  return out || 'unknown';
}
function appendLimitations(root, limitations) {
  var values = Array.isArray(limitations) ? limitations.filter(function (value, index, list) {
    return typeof value === 'string' && list.indexOf(value) === index;
  }) : [];
  if (!values.length) return;
  var details = el('details', { class: 'design-limitations design-limitations--inline' });
  details.appendChild(el('summary', { text: t('design.limitations', { count: values.length }) }));
  var list = el('ul');
  values.forEach(function (value) {
    list.appendChild(el('li', {
      text: localizedEnum('design.limitation.', value, 'design.limitation.unknown')
    }));
  });
  details.appendChild(list);
  root.appendChild(details);
}

export const designFilters = {
  readState: readState, writeState: writeState, element: element,
  query: query, request: request, post: post, statusText: statusText,
  errorMessage: errorMessage,
  tokenStatusText: tokenStatusText, changedSideText: changedSideText,
  tokenKindText: tokenKindText, componentStatusText: componentStatusText,
  componentKindText: componentKindText, cssToken: cssToken,
  appendLimitations: appendLimitations, localizedEnum: localizedEnum
};
