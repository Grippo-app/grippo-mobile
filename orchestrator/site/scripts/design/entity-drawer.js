import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { designFilters, errorMessage } from './filters.js';
import { clipboard } from '../clipboard.js';
import { appRunControl } from '../app-run-control.js';

var el = dom.el;
function t(key, params) { return i18n.t(key, params); }
function themeLabel(value) {
  return value === 'light' || value === 'dark' ? t('design.theme.' + value) : t('design.unknown');
}
function platformLabel(value) {
  return ['shared', 'android', 'ios'].indexOf(value) >= 0 ? t('design.platform.' + value) : t('design.unknown');
}
function statusLabel(value) {
  return ['healthy', 'drifted', 'missing', 'unknown'].indexOf(value) >= 0
    ? t('design.status.' + value) : t('design.unknown');
}
function surfaceTypeLabel(value) {
  return ['screen', 'dialog', 'overlay'].indexOf(value) >= 0
    ? t('design.surface.' + value) : t('design.unknown');
}
var MAPPING_STATES = ['active', 'review-required', 'orphaned-design', 'orphaned-project', 'target-out-of-scope', 'incompatible', 'retired'];
var RELATIONS = ['one-to-one', 'alias'];
var COMPONENT_RELATIONS = ['direct', 'wrapper', 'composite', 'shared-implementation', 'external'];
var SUGGESTION_TIERS = ['exact-path', 'prefix-path', 'leaf-name', 'alias-graph', 'name-similarity', 'value-only'];
function mappingStateLabel(value) {
  return MAPPING_STATES.indexOf(value) >= 0
    ? t('design.mappingState.' + value) : t('design.unknown');
}
function relationLabel(value) {
  return RELATIONS.indexOf(value) >= 0
    ? t('design.mapping.relation.' + value) : t('design.unknown');
}
function componentRelationLabel(value) {
  return COMPONENT_RELATIONS.indexOf(value) >= 0
    ? t('design.componentRelation.' + value) : t('design.unknown');
}
function componentSeverityLabel(value) {
  return ['info', 'review', 'additive', 'behavioral', 'breaking', 'blocking'].indexOf(value) >= 0
    ? t('design.componentSeverity.' + value) : t('design.unknown');
}
function componentBandLabel(value) {
  return ['strong', 'moderate', 'weak'].indexOf(value) >= 0
    ? t('design.componentSuggestion.band.' + value) : t('design.unknown');
}
function componentSignalLabel(value) {
  return ['qualified-name', 'display-name', 'api-compatibility', 'slot-compatibility'].indexOf(value) >= 0
    ? t('design.componentSuggestion.signal.' + value) : t('design.unknown');
}
function componentClassificationLabel(value) {
  return ['unclassified', 'intentionally-project-only', 'external', 'deprecated',
    'superseded', 'deferred', 'ignored'].indexOf(value) >= 0
    ? t('design.projectComponent.' + value) : t('design.unknown');
}
function componentVisualLabel(value) {
  return ['not-run', 'not-applicable', 'insufficient-evidence', 'matched', 'review-required', 'drifted'].indexOf(value) >= 0
    ? t('design.componentVisual.' + value) : t('design.unknown');
}
function suggestionTierLabel(value) {
  return SUGGESTION_TIERS.indexOf(value) >= 0
    ? t('design.suggestion.' + value) : t('design.unknown');
}
var dialog = null;
var body = null;
var title = null;
var closeButton = null;
var opener = null;
var onClose = null;
var currentKey = '';
// Registered by panels/design.js: the mapping-ops baskets live in panel
// state, so a background poll can rebuild the list without dropping drafted
// ops. Tokens and components each have their own basket + apply endpoint.
var tokenHooks = null;
var componentHooks = null;

function finishClose() {
  currentKey = '';
  if (onClose) onClose();
  onClose = null;
  if (opener && opener.focus) setTimeout(function () { opener.focus(); }, 0);
  opener = null;
}
function requestClose() {
  if (dialog && typeof dialog.close === 'function') dialog.close();
  else if (dialog) {
    dialog.removeAttribute('open');
    finishClose();
  }
}
function build() {
  dialog = el('dialog', { class: 'design-drawer', attrs: { 'aria-labelledby': 'design-drawer-title' } });
  var head = el('div', { class: 'design-drawer__head' });
  title = el('h3', { id: 'design-drawer-title', text: t('design.details') });
  closeButton = el('button', { type: 'button', class: 'btn btn--ghost btn--small', text: t('design.close') });
  closeButton.addEventListener('click', requestClose);
  head.appendChild(title); head.appendChild(closeButton); dialog.appendChild(head);
  body = el('div', { class: 'design-drawer__body' }); dialog.appendChild(body);
  dialog.addEventListener('cancel', function (event) {
    event.preventDefault(); requestClose();
  });
  dialog.addEventListener('close', finishClose);
  return dialog;
}
function definition(label, value) {
  var wrap = el('div', { class: 'design-definition' });
  wrap.appendChild(el('dt', { text: label }));
  wrap.appendChild(el('dd', { text: value === null || value === undefined || value === '' ? t('design.unknown') : String(value) }));
  return wrap;
}
function section(label, value) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: label }));
  if (Array.isArray(value)) {
    if (!value.length) block.appendChild(el('p', { text: t('design.none') }));
    else {
      var list = el('ul');
      value.slice(0, 100).forEach(function (item) {
        list.appendChild(el('li', { text: typeof item === 'string' ? item : JSON.stringify(item) }));
      });
      block.appendChild(list);
    }
  } else if (value && typeof value === 'object') {
    var dl = el('dl', { class: 'design-definitions' });
    Object.keys(value).slice(0, 100).forEach(function (key) {
      var item = value[key];
      dl.appendChild(definition(key, typeof item === 'object' ? JSON.stringify(item) : item));
    });
    block.appendChild(dl);
  } else block.appendChild(el('p', {
    text: value === null || value === undefined || value === '' ? t('design.none') : String(value)
  }));
  return block;
}
function definitionsSection(label, rows) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: label }));
  var dl = el('dl', { class: 'design-definitions' });
  rows.forEach(function (row) {
    if (row[1] !== null && row[1] !== undefined && row[1] !== '') {
      dl.appendChild(definition(row[0], row[1]));
    }
  });
  if (dl.childNodes.length) block.appendChild(dl);
  else block.appendChild(el('p', { text: t('design.none') }));
  return block;
}
function listSection(label, items, formatter) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: label }));
  if (!Array.isArray(items) || !items.length) {
    block.appendChild(el('p', { text: t('design.none') })); return block;
  }
  var list = el('ul');
  items.slice(0, 100).forEach(function (item) {
    list.appendChild(el('li', { text: formatter(item) }));
  });
  block.appendChild(list);
  return block;
}
function tasksSection(tasks, taskHistory) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.tasks') }));
  if (taskHistory && !taskHistory.complete) {
    block.appendChild(el('p', {
      class: 'design-state design-state--error',
      text: designFilters.localizedEnum(
        'design.limitation.',
        taskHistory.reason || 'task-history-index-partial',
        'design.limitation.unknown'
      )
    }));
  }
  if (!Array.isArray(tasks) || !tasks.length) {
    block.appendChild(el('p', { text: t('design.none') })); return block;
  }
  var list = el('ul');
  tasks.slice(0, 100).forEach(function (task) {
    var link = el('a', {
      href: '#board?task=' + encodeURIComponent(task.stem),
      text: (task.title || task.stem) + ' · ' +
        designFilters.localizedEnum('board.column.', task.column, 'board.column.unknown')
    });
    list.appendChild(el('li', {}, [link]));
  });
  block.appendChild(list);
  return block;
}
function copyButton(value) {
  var button = el('button', { type: 'button', class: 'btn btn--ghost btn--small', text: t('common.copy') });
  button.addEventListener('click', function () { clipboard.copy(String(value), button); });
  return button;
}
function sourcesSection(item) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.sources') }));
  var sources = item.sources || {};
  if (sources.figma && (sources.figma.url || sources.figma.nodeId || sources.figma.name)) {
    var figma = el('div', { class: 'design-source-row' });
    figma.appendChild(el('strong', { text: t('design.source.figma') }));
    if (sources.figma.url) figma.appendChild(el('a', {
      href: sources.figma.url, target: '_blank', rel: 'noopener noreferrer',
      text: sources.figma.name || sources.figma.nodeId || t('design.openSource')
    }));
    else figma.appendChild(el('span', { text: sources.figma.name || sources.figma.nodeId }));
    if (sources.figma.nodeId) figma.appendChild(copyButton(sources.figma.nodeId));
    block.appendChild(figma);
  }
  if (sources.code && (sources.code.path || sources.code.symbol)) {
    var code = el('div', { class: 'design-source-row' });
    code.appendChild(el('strong', { text: t('design.source.code') }));
    code.appendChild(el('span', { text: [sources.code.symbol, sources.code.path].filter(Boolean).join(' · ') }));
    if (sources.code.path) code.appendChild(copyButton(sources.code.path));
    block.appendChild(code);
  }
  if (block.childNodes.length === 1) block.appendChild(el('p', { text: t('design.none') }));
  return block;
}
function historySection(item) {
  var history = item.history || {};
  if (!history.available) {
    return section(t('design.detail.history'),
      history.reason
        ? designFilters.localizedEnum('design.limitation.', history.reason, 'design.limitation.unknown')
        : t('design.none'));
  }
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.history') }));
  if (!history.items || !history.items.length) {
    block.appendChild(el('p', { text: t('design.none') }));
    return block;
  }
  var list = el('div', { class: 'design-history-list' });
  history.items.slice(0, 100).forEach(function (change) {
    var details = el('details');
    details.appendChild(el('summary', {
      text: designFilters.localizedEnum(
        'design.change.', change.kind, 'design.change.unknown'
      ) + (change.timestamp ? ' · ' + change.timestamp : '')
    }));
    var definitions = el('dl', { class: 'design-definitions' });
    [
      [t('design.history.sourceFingerprint'), change.sourceFingerprint],
      [t('design.history.artifactHash'), change.artifactHash],
      [t('design.history.syncJob'), change.syncJobId],
      [t('design.history.previous'), change.previousHash],
      [t('design.history.current'), change.currentHash]
    ].forEach(function (row) {
      if (row[1]) definitions.appendChild(definition(row[0], row[1]));
    });
    details.appendChild(definitions);
    list.appendChild(details);
  });
  block.appendChild(list);
  return block;
}
function surfaceVariantsSection(item) {
  return listSection(t('design.detail.variants'), item.variants || [], function (variant) {
    return [
      themeLabel(variant.theme), variant.locale || t('design.unknown'),
      platformLabel(variant.platform),
      variant.captured ? t('design.captured') : t('design.notCaptured')
    ].join(' · ');
  });
}
function surfaceDriftSection(item) {
  if (!Array.isArray(item.drift)) return section(t('design.detail.drift'), t('design.notChecked'));
  return listSection(t('design.detail.drift'), item.drift, function (drift) {
    var state = ['DRIFTED', 'CLEAN', 'NOT_CHECKED'].indexOf(drift.status) >= 0
      ? t('design.surfaceDrift.' + drift.status) : t('design.notChecked');
    var changes = Array.isArray(drift.changes) && drift.changes.length ? ' · ' + drift.changes.join(', ') : '';
    return themeLabel(drift.theme === 'primary' ? 'light' : drift.theme) + ' · ' + state + changes;
  });
}
function comparisonSection(item, revision) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.comparisons') }));
  var comparisons = item.comparisons || [];
  if (!comparisons.length) {
    block.appendChild(el('p', { text: t('design.noCompatibleComparison') })); return block;
  }
  var select = el('select', { class: 'input', attrs: { 'aria-label': t('design.comparisonAria') } });
  comparisons.forEach(function (comparison, index) {
    var left = (item.variants || []).find(function (variant) { return variant.id === comparison.left; });
    var right = (item.variants || []).find(function (variant) { return variant.id === comparison.right; });
    select.appendChild(el('option', {
      value: String(index),
      text: designFilters.localizedEnum(
        'design.comparison.', comparison.mode, 'design.comparison.unknown'
      ) + ': ' +
        [left && (themeLabel(left.theme) + '/' + left.locale + '/' + platformLabel(left.platform)),
          right && (themeLabel(right.theme) + '/' + right.locale + '/' + platformLabel(right.platform))]
          .filter(Boolean).join(' ↔ ')
    }));
  });
  block.appendChild(select);
  var stage = el('div', { class: 'design-comparison-stage' });
  var range = el('input', {
    type: 'range', min: '0', max: '100', value: '50',
    class: 'range-input',
    attrs: { 'aria-label': t('design.comparisonPosition'), 'aria-valuetext': '50%' }
  });
  var output = el('output', { text: '50%' });
  function renderComparison() {
    var comparison = comparisons[Number(select.value) || 0];
    var left = (item.variants || []).find(function (variant) { return variant.id === comparison.left; });
    var right = (item.variants || []).find(function (variant) { return variant.id === comparison.right; });
    stage.replaceChildren();
    if (!left || !right || !left.imageUrl || !right.imageUrl) {
      stage.appendChild(el('p', { text: t('design.notCaptured') })); return;
    }
    var leftUrl = left.imageUrl + '&expectedGenerationRevision=' + encodeURIComponent(revision);
    var rightUrl = right.imageUrl + '&expectedGenerationRevision=' + encodeURIComponent(revision);
    var failed = false;
    function imageFailed() {
      if (failed) return;
      failed = true;
      stage.replaceChildren(el('p', {
        class: 'design-state design-state--error', text: t('design.imageUnavailable')
      }));
    }
    var leftImage = el('img', {
      class: 'design-comparison-stage__base', src: leftUrl, loading: 'lazy',
      alt: t('design.comparisonAlt', {
        name: item.name, variant: themeLabel(left.theme) + '/' + left.locale + '/' + platformLabel(left.platform)
      })
    });
    leftImage.addEventListener('error', imageFailed, { once: true });
    stage.appendChild(leftImage);
    var reveal = el('div', { class: 'design-comparison-stage__reveal' });
    var rightImage = el('img', {
      src: rightUrl, loading: 'lazy',
      alt: t('design.comparisonAlt', {
        name: item.name, variant: themeLabel(right.theme) + '/' + right.locale + '/' + platformLabel(right.platform)
      })
    });
    rightImage.addEventListener('error', imageFailed, { once: true });
    reveal.appendChild(rightImage);
    stage.appendChild(reveal);
    function update() {
      reveal.style.clipPath = 'inset(0 ' + (100 - Number(range.value)) + '% 0 0)';
      output.textContent = range.value + '%';
      range.setAttribute('aria-valuetext', range.value + '%');
    }
    range.oninput = update; update();
  }
  select.addEventListener('change', renderComparison);
  block.appendChild(stage);
  var controls = el('label', { class: 'design-comparison-controls' });
  controls.appendChild(el('span', { text: t('design.comparisonPosition') }));
  controls.appendChild(range); controls.appendChild(output); block.appendChild(controls);
  renderComparison();
  return block;
}
// ---------------------------------------------------------------------------
// Component detail sections (entityType 'component' | 'project-component') —
// projections of the component comparison report + design inventory extras.
// ---------------------------------------------------------------------------
function canReviewComponentMappings(data) {
  return !!(componentHooks && typeof componentHooks.addOperation === 'function' &&
    data && data.analysis && typeof data.analysis.comparisonSemanticHash === 'string' &&
    data.analysis.comparisonSemanticHash);
}
function componentSummarySection(item) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.summary') }));
  var badges = el('div', { class: 'design-token-badges' });
  badges.appendChild(tokenBadge(
    'design-status design-component-status design-component-status--' + designFilters.cssToken(item.status),
    designFilters.componentStatusText(item.status)
  ));
  badges.appendChild(tokenBadge(
    'design-changed-side design-changed-side--' + designFilters.cssToken(item.changedSide),
    designFilters.changedSideText(item.changedSide)
  ));
  badges.appendChild(tokenBadge('design-status', designFilters.componentKindText(item.kind)));
  block.appendChild(badges);
  if (item.statusDetail) block.appendChild(el('p', { text: item.statusDetail }));
  if (item.change) {
    block.appendChild(el('p', {
      class: 'design-token-change',
      text: t('design.tokenChange.confidence.' + (item.change.confidence === 'exact' ? 'exact' : 'none'))
    }));
  }
  if (item.dimensions) {
    block.appendChild(el('p', {
      class: 'design-component-dimensions',
      text: [
        t('design.components.dimensions.properties', {
          mapped: item.dimensions.properties.mapped, total: item.dimensions.properties.designTotal
        }),
        t('design.components.dimensions.variants', {
          expressible: item.dimensions.variants.expressible, total: item.dimensions.variants.designTotal
        }),
        t('design.components.dimensions.slots', {
          mapped: item.dimensions.slots.mapped, total: item.dimensions.slots.designTotal
        }),
        componentVisualLabel(item.dimensions.visual)
      ].join(' · ')
    }));
  }
  return block;
}
function componentIdentitySection(item) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.identity') }));
  var dl = el('dl', { class: 'design-definitions' });
  [
    [t('design.field.designComponentId'), item.designComponentId],
    [t('design.field.projectComponentId'), item.projectComponentId],
    [t('design.field.nodeId'), item.nodeId || (item.design && item.design.nodeId)],
    [t('design.field.mappingId'), item.mappingId],
    [t('design.field.dispositionId'), item.dispositionId],
    [t('design.field.adapter'), item.adapterId]
  ].forEach(function (row) {
    if (row[1]) dl.appendChild(codeDefinition(row[0], row[1]));
  });
  if (item.mappingState) dl.appendChild(definition(t('design.field.mappingState'), mappingStateLabel(item.mappingState)));
  if (item.design && item.design.page && item.design.page.name) {
    dl.appendChild(definition(t('design.field.page'), item.design.page.name));
  }
  if (dl.childNodes.length) block.appendChild(dl);
  else block.appendChild(el('p', { text: t('design.none') }));
  return block;
}
function componentImplementationsSection(item) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.implementations') }));
  var platforms = Array.isArray(item.platforms) ? item.platforms : [];
  if (!platforms.length) {
    block.appendChild(el('p', { text: t('design.none') }));
    return block;
  }
  platforms.slice(0, 8).forEach(function (platform) {
    var row = el('div', { class: 'design-source-row' });
    row.appendChild(el('strong', {
      text: [platform.platform, componentRelationLabel(platform.relation),
        t('design.componentPlatformState.' + (['matched', 'drifted', 'missing', 'out-of-scope', 'external', 'unknown']
          .indexOf(platform.state) >= 0 ? platform.state : 'unknown'))].join(' · ')
    }));
    (platform.projectRefs || []).forEach(function (ref) {
      var line = el('span', {
        text: [ref.sourceSymbol, ref.sourcePath].filter(Boolean).join(' · ') || ref.projectComponentId
      });
      row.appendChild(line);
      if (ref.sourcePath) row.appendChild(copyButton(ref.sourcePath));
    });
    block.appendChild(row);
  });
  return block;
}
function componentPropertiesSection(design) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.properties') }));
  var properties = design && Array.isArray(design.properties) ? design.properties : [];
  var unsupported = design && Array.isArray(design.unsupportedProperties) ? design.unsupportedProperties : [];
  if (!properties.length && !unsupported.length) {
    block.appendChild(el('p', { text: t('design.none') }));
    return block;
  }
  var list = el('ul');
  properties.forEach(function (property) {
    list.appendChild(el('li', {
      text: property.name + ' (' + property.type + ')' +
        (property.options && property.options.length ? ': ' + property.options.join(', ') : '') +
        (property.defaultKnown && property.defaultValue !== null
          ? ' · ' + t('design.components.defaultValue', { value: String(property.defaultValue) }) : '')
    }));
  });
  unsupported.forEach(function (property) {
    list.appendChild(el('li', {
      class: 'design-component-unsupported-property',
      text: property.name + ' — ' + t('design.components.unsupportedProperty', { reason: property.reason })
    }));
  });
  block.appendChild(list);
  return block;
}
function componentVariantsSection(item, revision) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.variants') }));
  var variants = item.design && item.design.variants ? item.design.variants : { items: [], total: 0, nextOffset: null };
  if (!variants.total) {
    block.appendChild(el('p', { text: t('design.noVariants') }));
    return block;
  }
  block.appendChild(el('p', { class: 'field-help', text: t('design.components.variantTotal', { total: variants.total }) }));
  var list = el('ul', { class: 'design-component-variants' });
  function appendVariants(items) {
    (items || []).forEach(function (variant) {
      var assignments = Object.keys(variant.assignments || {}).map(function (key) {
        return key + '=' + variant.assignments[key];
      }).join(', ');
      list.appendChild(el('li', {
        text: (assignments || variant.name || variant.variantId) +
          (variant.isDefault ? ' · ' + t('design.components.defaultVariant') : '')
      }));
    });
  }
  appendVariants(variants.items);
  block.appendChild(list);
  function addMore(nextOffset) {
    if (nextOffset === null || nextOffset === undefined) return;
    var more = el('button', { type: 'button', class: 'btn btn--ghost btn--small', text: t('design.loadMoreVariants') });
    // The caption IS the affordance: writing the failure into it leaves a button
    // labelled with an error sentence and no visible way to retry. Keep the
    // caption, report beside it. The live region is mounted up front so the
    // assistive-technology announcement fires on the text change.
    var failure = el('p', {
      class: 'design-state design-state--error', attrs: { role: 'alert' }, hidden: true
    });
    more.addEventListener('click', function () {
      more.disabled = true; more.textContent = t('design.loadingMore');
      failure.textContent = ''; failure.hidden = true;
      designFilters.request('/api/design/components/' + encodeURIComponent(item.id) +
        '?expectedGenerationRevision=' + encodeURIComponent(revision) +
        '&variantOffset=' + encodeURIComponent(nextOffset) + '&variantLimit=50').then(function (data) {
        var next = data.component && data.component.design && data.component.design.variants;
        appendVariants(next && next.items);
        more.remove();
        failure.remove();
        addMore(next ? next.nextOffset : null);
      }).catch(function (error) {
        more.disabled = false; more.textContent = t('design.loadMoreVariants');
        failure.hidden = false;
        failure.textContent = errorMessage(error);
      });
    });
    block.appendChild(more);
    block.appendChild(failure);
  }
  addMore(variants.nextOffset);
  return block;
}
function componentVisualEvidenceSection(item) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.visualEvidence') }));
  var visual = item.design && item.design.visualEvidence;
  var entries = visual && Array.isArray(visual.entries) ? visual.entries : [];
  if (!entries.length) {
    block.appendChild(el('p', { text: t('design.notCaptured') }));
    return block;
  }
  block.appendChild(el('p', {
    class: 'field-help',
    text: t('design.components.visualCoverage.' + (['none', 'partial', 'representative'].indexOf(visual.coverage) >= 0
      ? visual.coverage : 'none'))
  }));
  var grid = el('div', { class: 'design-component-evidence' });
  entries.forEach(function (entry) {
    var cell = el('figure', { class: 'design-component-evidence__item' });
    if (entry.imageUrl) {
      var image = el('img', {
        src: entry.imageUrl, loading: 'lazy',
        alt: t('design.componentPreviewAlt', { name: item.name })
      });
      image.addEventListener('error', function () {
        image.replaceWith(el('div', { class: 'design-card__placeholder', text: t('design.imageUnavailable') }));
      }, { once: true });
      cell.appendChild(image);
    } else {
      cell.appendChild(el('div', { class: 'design-card__placeholder', text: t('design.notCaptured') }));
    }
    cell.appendChild(el('figcaption', {
      text: [entry.role, entry.variantId].filter(Boolean).join(' · ')
    }));
    grid.appendChild(cell);
  });
  block.appendChild(grid);
  return block;
}
function componentFindingsSection(item) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.findings') }));
  var findings = Array.isArray(item.findings) ? item.findings : [];
  if (!findings.length) {
    block.appendChild(el('p', { text: t('design.none') }));
    return block;
  }
  var list = el('ul');
  findings.slice(0, 64).forEach(function (finding) {
    var line = el('li');
    line.appendChild(el('span', {
      class: 'design-status design-component-severity design-component-severity--' + designFilters.cssToken(finding.severity),
      text: componentSeverityLabel(finding.severity)
    }));
    line.appendChild(el('span', {
      text: ' ' + [finding.family, finding.detail].filter(Boolean).join(' · ')
    }));
    list.appendChild(line);
  });
  block.appendChild(list);
  return block;
}
function confirmComponentMappingForm(item, candidate) {
  var wrap = el('div', { class: 'design-mapping-form', hidden: true });
  var status = inlineStatus();
  var relationField = el('label', { class: 'design-filter' });
  relationField.appendChild(el('span', { class: 'design-filter__label', text: t('design.mapping.relationLabel') }));
  var relationSelect = el('select', { class: 'input', attrs: { 'aria-label': t('design.mapping.relationLabel') } });
  ['direct', 'wrapper', 'shared-implementation'].forEach(function (relation) {
    relationSelect.appendChild(el('option', { value: relation, text: componentRelationLabel(relation) }));
  });
  relationSelect.value = 'direct';
  relationField.appendChild(relationSelect);
  wrap.appendChild(relationField);
  var requiredField = el('label', { class: 'design-filter design-filter--check' });
  var requiredCheck = el('input', { type: 'checkbox', class: 'choice-input', checked: true });
  requiredField.appendChild(requiredCheck);
  requiredField.appendChild(document.createTextNode(t('design.componentMapping.required')));
  wrap.appendChild(requiredField);
  wrap.appendChild(el('p', { class: 'design-mapping-form__policy', text: t('design.componentMapping.confirmNote') }));
  var actions = el('div', { class: 'design-dialog-actions' });
  var submit = el('button', { type: 'button', class: 'btn btn--small', text: t('design.mapping.addToBasket') });
  submit.addEventListener('click', function () {
    var operation = {
      op: 'upsert-mapping',
      designComponentId: item.designComponentId,
      implementations: [{
        adapterId: candidate.adapterId,
        relation: relationSelect.value,
        projectComponentIds: [candidate.projectComponentId],
        required: requiredCheck.checked
      }]
    };
    if (item.mappingId) operation.mappingId = item.mappingId;
    var result = componentHooks.addOperation(operation, t('design.mapping.confirm') + ': ' + (item.name || item.designComponentId));
    status.textContent = result && result.ok ? t('design.mapping.opAdded') : (result && result.reason || '');
    if (result && result.ok) wrap.hidden = true;
  });
  var cancel = el('button', { type: 'button', class: 'btn btn--ghost btn--small', text: t('design.cancel') });
  cancel.addEventListener('click', function () { wrap.hidden = true; });
  actions.appendChild(submit);
  actions.appendChild(cancel);
  wrap.appendChild(actions);
  wrap.appendChild(status);
  return wrap;
}
function componentSuggestionsSection(item, data) {
  var block = el('section', { class: 'design-detail-section design-token-suggestions' });
  block.appendChild(el('h4', { text: t('design.detail.suggestions') }));
  var suggestions = Array.isArray(item.suggestions) ? item.suggestions : [];
  if (!suggestions.length) {
    block.appendChild(el('p', { text: t('design.suggestion.empty') }));
    return block;
  }
  var allowConfirm = canReviewComponentMappings(data);
  suggestions.slice(0, 5).forEach(function (candidate) {
    var card = el('div', { class: 'design-suggestion' });
    var head = el('div', { class: 'design-suggestion__head' });
    head.appendChild(el('code', { text: candidate.projectComponentId }));
    head.appendChild(copyButton(candidate.projectComponentId));
    head.appendChild(el('span', {
      class: 'design-suggestion__confidence',
      text: t('design.componentSuggestion.bandLabel', { band: componentBandLabel(candidate.band) })
    }));
    card.appendChild(head);
    var meta = [];
    if (candidate.adapterId) meta.push(t('design.suggestion.adapter', { adapter: candidate.adapterId }));
    if (candidate.platform) meta.push(candidate.platform);
    if (candidate.sourcePath) meta.push(candidate.sourcePath);
    if (meta.length) card.appendChild(el('span', { class: 'design-suggestion__meta', text: meta.join(' · ') }));
    var signals = Array.isArray(candidate.signals) ? candidate.signals : [];
    if (signals.length) {
      var list = el('ul', { class: 'design-suggestion__signals' });
      signals.forEach(function (signal) {
        list.appendChild(el('li', {
          text: [componentSignalLabel(signal.kind), signal.detail].filter(Boolean).join(' · ')
        }));
      });
      card.appendChild(list);
    }
    card.appendChild(el('span', {
      class: 'design-suggestion__meta',
      text: t('design.componentSuggestion.propertyCounts', {
        matched: (candidate.matchedProperties || []).length,
        unmatched: (candidate.unmatchedProperties || []).length
      })
    }));
    if (candidate.conflicts && candidate.conflicts.length) {
      card.appendChild(el('p', {
        class: 'design-suggestion__note',
        text: t('design.componentSuggestion.conflicts', { detail: candidate.conflicts.join('; ') })
      }));
    }
    if (allowConfirm) {
      var form = confirmComponentMappingForm(item, candidate);
      var confirm = el('button', { type: 'button', class: 'btn btn--small', text: t('design.mapping.confirm') });
      confirm.addEventListener('click', function () { form.hidden = !form.hidden; });
      card.appendChild(confirm);
      card.appendChild(form);
    } else {
      card.appendChild(el('p', {
        class: 'design-suggestion__note', text: t('design.mapping.confirmRequiresComparison')
      }));
    }
    block.appendChild(card);
  });
  return block;
}
function componentActionsSection(item, data) {
  // Review actions only; there are deliberately NO destructive code-delete
  // actions anywhere in this drawer.
  if (!componentHooks) return null;
  var block = el('section', { class: 'design-detail-section design-token-actions' });
  block.appendChild(el('h4', { text: t('design.detail.actions') }));
  var any = false;
  var allowReview = canReviewComponentMappings(data);
  if (item.mappingId) {
    any = true;
    reasonAction(block, t('design.mapping.retire'), t('design.mapping.addToBasket'), function (reason) {
      return componentHooks.addOperation({
        op: 'retire-mapping', mappingId: item.mappingId, reason: reason
      }, t('design.mapping.retire') + ': ' + (item.name || item.designComponentId));
    });
    var renderField = el('label', { class: 'design-filter' });
    renderField.appendChild(el('span', { class: 'design-filter__label', text: t('design.componentMapping.renderClass') }));
    var renderSelect = el('select', { class: 'input', attrs: { 'aria-label': t('design.componentMapping.renderClass') } });
    [['', 'design.componentMapping.renderClassNone'], ['canvas', 'design.componentMapping.renderClassCanvas'],
      ['glass', 'design.componentMapping.renderClassGlass']].forEach(function (option) {
      renderSelect.appendChild(el('option', { value: option[0], text: t(option[1]) }));
    });
    renderField.appendChild(renderSelect);
    block.appendChild(renderField);
    reasonAction(block, t('design.componentMapping.setRenderClass'), t('design.mapping.addToBasket'), function (reason) {
      return componentHooks.addOperation({
        op: 'set-render-class', mappingId: item.mappingId,
        renderClass: renderSelect.value || null, reason: reason
      }, t('design.componentMapping.setRenderClass') + ': ' + (item.name || item.designComponentId));
    });
  }
  if (item.dispositionId) {
    any = true;
    var remove = el('button', { type: 'button', class: 'btn btn--small', text: t('design.componentMapping.removeDisposition') });
    remove.addEventListener('click', function () {
      componentHooks.addOperation({
        op: 'remove-disposition', dispositionId: item.dispositionId
      }, t('design.componentMapping.removeDisposition') + ': ' + (item.name || item.designComponentId));
    });
    block.appendChild(remove);
  }
  if ((item.status === 'design-only' || item.status === 'unmapped') && item.designComponentId) {
    if (allowReview) {
      any = true;
      reasonAction(block, t('design.componentMapping.keepDesignOnly'), t('design.mapping.addToBasket'), function (reason) {
        return componentHooks.addOperation({
          op: 'add-disposition', side: 'design',
          designComponentId: item.designComponentId,
          kind: 'intentionally-design-only', reason: reason
        }, t('design.componentMapping.keepDesignOnly') + ': ' + (item.name || item.designComponentId));
      });
    } else {
      any = true;
      block.appendChild(el('p', {
        class: 'design-suggestion__note', text: t('design.mapping.confirmRequiresComparison')
      }));
    }
  }
  return any ? block : null;
}
function renderComponent(data) {
  var item = data.component;
  if (componentHooks && typeof componentHooks.adoptAnalysis === 'function') componentHooks.adoptAnalysis(data.analysis);
  title.textContent = item.name || t('design.details');
  var sections = [
    componentSummarySection(item),
    componentIdentitySection(item),
    componentImplementationsSection(item),
    componentPropertiesSection(item.design),
    componentVariantsSection(item, data.generationRevision),
    listSection(t('design.detail.slots'), item.design && item.design.slots || [], function (slot) {
      return [slot.name, slot.kind].filter(Boolean).join(' · ');
    }),
    listSection(t('design.detail.dependencies'), item.design && item.design.dependencies || [], function (dependency) {
      return [dependency.layerName || dependency.targetNodeId || dependency.targetDesignComponentId,
        dependency.resolved ? null : t('design.components.dependencyUnresolved')].filter(Boolean).join(' · ');
    }),
    listSection(t('design.detail.tokenRefs'), item.design && item.design.tokenRefs || [], function (ref) {
      return [ref.providerName || ref.observedTokenKey, ref.contextKey, ref.field].filter(Boolean).join(' · ');
    }),
    componentVisualEvidenceSection(item),
    componentFindingsSection(item),
    componentSuggestionsSection(item, data),
    // Row limitations are comparator-owned free-form codes; show them verbatim
    // instead of pretending a localized catalog entry exists for each.
    listSection(t('design.detail.limitations'), item.limitations || [], String),
    listSection(t('design.detail.usage'), item.usage || [], function (usage) {
      return usage.name + ' · ' + surfaceTypeLabel(usage.type) + (usage.route ? ' · ' + usage.route : '');
    })
  ];
  var actions = componentActionsSection(item, data);
  if (actions) sections.push(actions);
  sections.push(historySection(item));
  sections.push(tasksSection(item.tasks, item.taskHistory));
  body.replaceChildren.apply(body, sections);
}
function projectComponentActionsSection(item, data, suggestionsBlock) {
  var block = el('section', { class: 'design-detail-section design-token-actions' });
  block.appendChild(el('h4', { text: t('design.detail.actions') }));
  var mapButton = el('button', { type: 'button', class: 'btn btn--small', text: t('design.projectOnly.mapToFigma') });
  mapButton.addEventListener('click', function () {
    if (suggestionsBlock && suggestionsBlock.scrollIntoView) suggestionsBlock.scrollIntoView({ block: 'start' });
  });
  block.appendChild(mapButton);
  if (componentHooks && item.projectComponentId && item.adapterId) {
    if (canReviewComponentMappings(data)) {
      reasonAction(block, t('design.projectOnly.keepLocal'), t('design.mapping.addToBasket'), function (reason) {
        return componentHooks.addOperation({
          op: 'add-disposition', side: 'project',
          projectComponentId: item.projectComponentId, adapterId: item.adapterId,
          kind: 'intentionally-project-only', reason: reason
        }, t('design.projectOnly.keepLocal') + ': ' + (item.name || item.projectComponentId));
      });
    } else {
      block.appendChild(el('p', {
        class: 'design-suggestion__note', text: t('design.mapping.confirmRequiresComparison')
      }));
    }
  }
  if (componentHooks && item.dispositionId) {
    var remove = el('button', { type: 'button', class: 'btn btn--small', text: t('design.componentMapping.removeDisposition') });
    remove.addEventListener('click', function () {
      componentHooks.addOperation({
        op: 'remove-disposition', dispositionId: item.dispositionId
      }, t('design.componentMapping.removeDisposition') + ': ' + (item.name || item.projectComponentId));
    });
    block.appendChild(remove);
  }
  return block;
}
function renderProjectComponent(data) {
  var item = data.component;
  if (componentHooks && typeof componentHooks.adoptAnalysis === 'function') componentHooks.adoptAnalysis(data.analysis);
  title.textContent = item.name || t('design.details');
  var suggestionsBlock = componentSuggestionsSection(item, data);
  body.replaceChildren(
    definitionsSection(t('design.detail.summary'), [
      [t('design.field.type'), item.kind],
      [t('design.field.adapter'), item.adapterId],
      [t('design.filter.platform'), item.platform],
      [t('design.projectOnly.classify'), componentClassificationLabel(item.classification)]
    ]),
    componentIdentitySection(item),
    sourcesSection({ sources: { code: { symbol: item.sourceSymbol, path: item.sourcePath } } }),
    suggestionsBlock,
    projectComponentActionsSection(item, data, suggestionsBlock),
    tasksSection(item.tasks, item.taskHistory)
  );
}
function renderSurface(data) {
  var item = data.surface;
  title.textContent = item.name;
  var actions = el('div', { class: 'design-detail-actions' });
  var actionStatus = el('span', { class: 'design-detail-actions__status', attrs: { 'aria-live': 'polite' } });
  if (item.preview && item.preview.available) {
    var preview = el('button', { type: 'button', class: 'btn', text: t('design.previewInApp') });
    preview.addEventListener('click', function () {
      appRunControl.open({ surfaceId: item.id });
      actionStatus.className = 'design-detail-actions__status';
      actionStatus.textContent = t('design.appRunnerOpened');
    });
    actions.appendChild(preview);
  }
  if (item.preview && item.preview.reason) {
    actions.appendChild(el('span', {
      class: 'design-state',
      text: designFilters.localizedEnum(
        'design.previewReason.', item.preview.reason, 'design.previewReason.unknown'
      )
    }));
  }
  actions.appendChild(actionStatus);
  var surfaceSources = {
    sources: {
      figma: {
        name: item.name,
        nodeId: item.figmaSource && item.figmaSource.nodeId,
        url: item.figmaSource && item.figmaSource.url
      },
      code: {
        symbol: null,
        path: item.relations && item.relations.codeSources && item.relations.codeSources[0]
      }
    }
  };
  body.replaceChildren(
    actions,
    definitionsSection(t('design.detail.summary'), [
      [t('design.field.type'), surfaceTypeLabel(item.type)],
      [t('design.field.status'), statusLabel(item.status)],
      [t('design.field.route'), item.route]
    ]),
    sourcesSection(surfaceSources),
    surfaceVariantsSection(item),
    comparisonSection(item, data.generationRevision),
    listSection(t('design.detail.notCaptured'), item.notCaptured || [], function (variant) {
      return [themeLabel(variant.theme), variant.locale || t('design.unknown'),
        platformLabel(variant.platform)].join(' · ');
    }),
    definitionsSection(t('design.detail.ownership'), [
      [t('design.field.module'), item.relations && item.relations.module],
      [t('design.field.feature'), item.relations && item.relations.feature],
      [t('design.field.route'), item.relations && item.relations.route],
      [t('design.field.codeSources'), item.relations && (item.relations.codeSources || []).join(', ')]
    ]),
    listSection(t('design.detail.usage'), item.usedComponents || [], function (usage) {
      return usage.name + (usage.nodeId ? ' · ' + usage.nodeId : '');
    }),
    section(t('design.detail.evidence'), item.evidence && item.evidence.available
      ? t('design.available') : t('design.notCaptured')),
    surfaceDriftSection(item),
    historySection(item),
    tasksSection(item.tasks, item.taskHistory)
  );
}
// ---------------------------------------------------------------------------
// Token detail sections (entityType 'token' | 'project-token').
// ---------------------------------------------------------------------------
function canReviewMappings(data) {
  return !!(tokenHooks && typeof tokenHooks.addOperation === 'function' &&
    data && data.analysis && typeof data.analysis.comparisonSemanticHash === 'string' &&
    data.analysis.comparisonSemanticHash);
}
function codeDefinition(label, value) {
  var wrap = el('div', { class: 'design-definition' });
  wrap.appendChild(el('dt', { text: label }));
  var dd = el('dd', { class: 'design-definition__code' });
  dd.appendChild(el('code', { text: String(value) }));
  dd.appendChild(copyButton(value));
  wrap.appendChild(dd);
  return wrap;
}
function tokenIdentitySection(item) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.identity') }));
  var dl = el('dl', { class: 'design-definitions' });
  [
    [t('design.field.observedTokenKey'), item.observedTokenKey],
    [t('design.field.mode'), item.contextKey],
    [t('design.field.projectTokenId'), item.projectTokenId],
    [t('design.field.mappingId'), item.mappingId],
    [t('design.field.adapter'), item.adapterId],
    [t('design.field.dispositionId'), item.dispositionId]
  ].forEach(function (row) {
    if (row[1]) dl.appendChild(codeDefinition(row[0], row[1]));
  });
  if (item.mappingState) dl.appendChild(definition(t('design.field.mappingState'), mappingStateLabel(item.mappingState)));
  if (item.relation) dl.appendChild(definition(t('design.mapping.relationLabel'), relationLabel(item.relation)));
  if (dl.childNodes.length) block.appendChild(dl);
  else block.appendChild(el('p', { text: t('design.none') }));
  return block;
}
function tokenBadge(className, text) {
  return el('span', { class: className, text: text });
}
function tokenSummarySection(item) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.summary') }));
  var badges = el('div', { class: 'design-token-badges' });
  badges.appendChild(tokenBadge(
    'design-status design-token-status design-token-status--' + designFilters.cssToken(item.status),
    designFilters.tokenStatusText(item.status)
  ));
  badges.appendChild(tokenBadge(
    'design-changed-side design-changed-side--' + designFilters.cssToken(item.changedSide),
    designFilters.changedSideText(item.changedSide)
  ));
  badges.appendChild(tokenBadge('design-status', designFilters.tokenKindText(item.kind)));
  block.appendChild(badges);
  if (item.statusDetail) block.appendChild(el('p', { text: item.statusDetail }));
  if (item.change) {
    block.appendChild(el('p', {
      class: 'design-token-change',
      text: t('design.tokenChange.confidence.' + (item.change.confidence === 'exact' ? 'exact' : 'none'))
    }));
  }
  return block;
}
function modeValuesSection(item) {
  var block = el('section', { class: 'design-detail-section' });
  block.appendChild(el('h4', { text: t('design.detail.modeValues') }));
  var results = Array.isArray(item.modeResults) ? item.modeResults : [];
  if (!results.length) {
    var figmaModes = Object.keys(item.figmaValues || {});
    var codeModes = Object.keys(item.codeValues || {});
    if (!figmaModes.length && !codeModes.length) {
      block.appendChild(el('p', { text: t('design.none') }));
      return block;
    }
    var dl = el('dl', { class: 'design-definitions' });
    figmaModes.forEach(function (mode) {
      dl.appendChild(definition(t('design.modeTable.designMode') + ' ' + mode, item.figmaValues[mode]));
    });
    codeModes.forEach(function (mode) {
      dl.appendChild(definition(t('design.modeTable.projectMode') + ' ' + mode, item.codeValues[mode]));
    });
    block.appendChild(dl);
    return block;
  }
  var table = el('div', { class: 'design-mode-table', attrs: { role: 'table', 'aria-label': t('design.detail.modeValues') } });
  table.appendChild(el('div', { class: 'design-mode-table__row design-mode-table__row--head', attrs: { role: 'row' } }, [
    el('span', { attrs: { role: 'columnheader' }, text: t('design.modeTable.designMode') }),
    el('span', { attrs: { role: 'columnheader' }, text: t('design.modeTable.designValue') }),
    el('span', { attrs: { role: 'columnheader' }, text: t('design.modeTable.projectMode') }),
    el('span', { attrs: { role: 'columnheader' }, text: t('design.modeTable.projectValue') }),
    el('span', { attrs: { role: 'columnheader' }, text: t('design.modeTable.result') })
  ]));
  results.slice(0, 100).forEach(function (mode) {
    table.appendChild(el('div', { class: 'design-mode-table__row', attrs: { role: 'row' } }, [
      el('span', { attrs: { role: 'rowheader' }, text: mode.designContextKey || t('design.unknown') }),
      el('span', { attrs: { role: 'cell' }, text: mode.designValue === null || mode.designValue === undefined ? t('design.none') : String(mode.designValue) }),
      el('span', { attrs: { role: 'cell' }, text: mode.projectMode || t('design.none') }),
      el('span', { attrs: { role: 'cell' }, text: mode.projectValue === null || mode.projectValue === undefined ? t('design.none') : String(mode.projectValue) }),
      el('span', {
        class: 'design-status design-mode-result design-mode-result--' + designFilters.cssToken(mode.result),
        attrs: { role: 'cell' }, text: String(mode.result || t('design.unknown'))
      })
    ]));
  });
  block.appendChild(table);
  return block;
}
function projectRefsSection(item) {
  // Plain-text paths and symbols only — no file:// links by design.
  return listSection(t('design.detail.projectRefs'), item.projectRefs || [], function (ref) {
    return [
      ref.projectTokenId,
      ref.sourcePath ? ref.sourcePath + (ref.sourceSymbol ? ' · ' + ref.sourceSymbol : '') : ref.sourceSymbol,
      ref.present ? null : t('design.projectRef.absent')
    ].filter(Boolean).join(' · ');
  });
}
function inlineStatus() {
  return el('p', { class: 'design-inline-status', attrs: { role: 'status' } });
}
function reasonForm(submitLabel, onSubmit) {
  var wrap = el('div', { class: 'design-reason-form', hidden: true });
  var label = el('label', { class: 'design-filter' });
  label.appendChild(el('span', { class: 'design-filter__label', text: t('design.mapping.reason') }));
  var input = el('input', {
    type: 'text', class: 'input',
    attrs: { maxlength: '500', 'aria-label': t('design.mapping.reason') }
  });
  label.appendChild(input);
  wrap.appendChild(label);
  var status = inlineStatus();
  var actions = el('div', { class: 'design-dialog-actions' });
  var submit = el('button', { type: 'button', class: 'btn btn--small', text: submitLabel });
  submit.addEventListener('click', function () {
    var reason = input.value.trim();
    if (!reason) {
      status.textContent = t('design.mapping.reasonRequired');
      input.focus();
      return;
    }
    var result = onSubmit(reason);
    status.textContent = result && result.ok ? t('design.mapping.opAdded') : (result && result.reason || '');
    if (result && result.ok) {
      input.value = '';
      wrap.hidden = true;
    }
  });
  var cancel = el('button', { type: 'button', class: 'btn btn--ghost btn--small', text: t('design.cancel') });
  cancel.addEventListener('click', function () { wrap.hidden = true; });
  actions.appendChild(submit);
  actions.appendChild(cancel);
  wrap.appendChild(actions);
  wrap.appendChild(status);
  return { element: wrap, input: input };
}
function reasonAction(container, buttonLabel, submitLabel, onSubmit) {
  var button = el('button', { type: 'button', class: 'btn btn--small', text: buttonLabel });
  var form = reasonForm(submitLabel, onSubmit);
  button.addEventListener('click', function () {
    form.element.hidden = !form.element.hidden;
    if (!form.element.hidden) setTimeout(function () { form.input.focus(); }, 0);
  });
  container.appendChild(button);
  container.appendChild(form.element);
}
function confirmMappingForm(item, suggestion) {
  var wrap = el('div', { class: 'design-mapping-form', hidden: true });
  var status = inlineStatus();
  var relationField = el('label', { class: 'design-filter' });
  relationField.appendChild(el('span', { class: 'design-filter__label', text: t('design.mapping.relationLabel') }));
  var relationSelect = el('select', { class: 'input', attrs: { 'aria-label': t('design.mapping.relationLabel') } });
  RELATIONS.forEach(function (relation) {
    relationSelect.appendChild(el('option', { value: relation, text: relationLabel(relation) }));
  });
  relationSelect.value = 'one-to-one';
  relationField.appendChild(relationSelect);
  wrap.appendChild(relationField);

  wrap.appendChild(el('p', {
    class: 'design-mapping-form__label',
    text: item.contextKey || t('design.unknown')
  }));
  wrap.appendChild(el('p', { class: 'design-mapping-form__policy', text: t('design.mapping.policyExact') }));
  var actions = el('div', { class: 'design-dialog-actions' });
  var submit = el('button', { type: 'button', class: 'btn btn--small', text: t('design.mapping.addToBasket') });
  submit.addEventListener('click', function () {
    var operation = {
      op: 'upsert-mapping',
      observedTokenKey: item.observedTokenKey,
      contextSelector: item.context || {},
      adapterId: suggestion.adapterId,
      projectTokenIds: [suggestion.projectTokenId],
      relation: relationSelect.value
    };
    if (item.mappingId) operation.mappingId = item.mappingId;
    var result = tokenHooks.addOperation(operation, t('design.mapping.confirm') + ': ' + (item.name || item.observedTokenKey));
    status.textContent = result && result.ok ? t('design.mapping.opAdded') : (result && result.reason || '');
    if (result && result.ok) wrap.hidden = true;
  });
  var cancel = el('button', { type: 'button', class: 'btn btn--ghost btn--small', text: t('design.cancel') });
  cancel.addEventListener('click', function () { wrap.hidden = true; });
  actions.appendChild(submit);
  actions.appendChild(cancel);
  wrap.appendChild(actions);
  wrap.appendChild(status);
  return wrap;
}
function suggestionsSection(item, type, data) {
  var block = el('section', { class: 'design-detail-section design-token-suggestions' });
  block.appendChild(el('h4', { text: t('design.detail.suggestions') }));
  var suggestions = Array.isArray(item.suggestions) ? item.suggestions : [];
  if (!suggestions.length) {
    block.appendChild(el('p', { text: t('design.suggestion.empty') }));
    return block;
  }
  var allowConfirm = type === 'token' && canReviewMappings(data);
  suggestions.slice(0, 5).forEach(function (suggestion) {
    var card = el('div', { class: 'design-suggestion' });
    var head = el('div', { class: 'design-suggestion__head' });
    head.appendChild(el('code', { text: suggestion.projectTokenId }));
    head.appendChild(copyButton(suggestion.projectTokenId));
    head.appendChild(el('span', {
      class: 'design-suggestion__confidence',
      text: t('design.suggestion.confidence', { tier: suggestionTierLabel(suggestion.confidence) })
    }));
    card.appendChild(head);
    var meta = [];
    if (suggestion.adapterId) meta.push(t('design.suggestion.adapter', { adapter: suggestion.adapterId }));
    if (suggestion.competitors) meta.push(t('design.suggestion.competitors', { count: suggestion.competitors }));
    if (meta.length) card.appendChild(el('span', { class: 'design-suggestion__meta', text: meta.join(' · ') }));
    var signals = Array.isArray(suggestion.signals) ? suggestion.signals : [];
    if (signals.length) {
      var list = el('ul', { class: 'design-suggestion__signals' });
      signals.forEach(function (signal) {
        list.appendChild(el('li', {
          text: [suggestionTierLabel(signal.signal), signal.detail].filter(Boolean).join(' · ')
        }));
      });
      card.appendChild(list);
    }
    if (type === 'token') {
      if (allowConfirm) {
        var form = confirmMappingForm(item, suggestion);
        var confirm = el('button', { type: 'button', class: 'btn btn--small', text: t('design.mapping.confirm') });
        confirm.addEventListener('click', function () { form.hidden = !form.hidden; });
        card.appendChild(confirm);
        card.appendChild(form);
      } else {
        card.appendChild(el('p', {
          class: 'design-suggestion__note', text: t('design.mapping.confirmRequiresComparison')
        }));
      }
    }
    block.appendChild(card);
  });
  return block;
}
function tokenActionsSection(item, data) {
  // Review actions only; there are deliberately NO destructive code-delete
  // actions anywhere in this drawer.
  if (!tokenHooks) return null;
  var block = el('section', { class: 'design-detail-section design-token-actions' });
  block.appendChild(el('h4', { text: t('design.detail.actions') }));
  var any = false;
  if (item.mappingId) {
    any = true;
    reasonAction(block, t('design.mapping.retire'), t('design.mapping.addToBasket'), function (reason) {
      return tokenHooks.addOperation({
        op: 'retire-mapping', mappingId: item.mappingId, reason: reason
      }, t('design.mapping.retire') + ': ' + (item.name || item.observedTokenKey));
    });
  }
  if (!item.mappingId && !item.dispositionId && item.observedTokenKey) {
    if (canReviewMappings(data)) {
      any = true;
      reasonAction(block, t('design.mapping.keepDesignOnly'), t('design.mapping.addToBasket'), function (reason) {
        return tokenHooks.addOperation({
          op: 'add-disposition', side: 'observed',
          observedTokenKey: item.observedTokenKey,
          kind: 'observed-only-intentional', reason: reason
        }, t('design.mapping.keepDesignOnly') + ': ' + (item.name || item.observedTokenKey));
      });
    } else {
      any = true;
      block.appendChild(el('p', {
        class: 'design-suggestion__note', text: t('design.mapping.confirmRequiresComparison')
      }));
    }
  }
  return any ? block : null;
}
function renderToken(data) {
  var item = data.token;
  if (tokenHooks && typeof tokenHooks.adoptAnalysis === 'function') tokenHooks.adoptAnalysis(data.analysis);
  title.textContent = item.name || t('design.details');
  var sections = [
    tokenSummarySection(item),
    tokenIdentitySection(item),
    modeValuesSection(item),
    listSection(t('design.detail.lifecycle'), item.lifecycle || [], function (finding) {
      return [finding.kind, finding.detail].filter(Boolean).join(' · ');
    }),
    // Row limitations are comparator-owned free-form codes; show them verbatim
    // instead of pretending a localized catalog entry exists for each.
    listSection(t('design.detail.limitations'), item.limitations || [], String),
    suggestionsSection(item, 'token', data),
    projectRefsSection(item)
  ];
  var actions = tokenActionsSection(item, data);
  if (actions) sections.push(actions);
  sections.push(tasksSection(item.tasks));
  body.replaceChildren.apply(body, sections);
}
function projectTokenActionsSection(item, data, suggestionsBlock) {
  var block = el('section', { class: 'design-detail-section design-token-actions' });
  block.appendChild(el('h4', { text: t('design.detail.actions') }));
  var mapButton = el('button', { type: 'button', class: 'btn btn--small', text: t('design.projectOnly.mapToFigma') });
  mapButton.addEventListener('click', function () {
    if (suggestionsBlock && suggestionsBlock.scrollIntoView) suggestionsBlock.scrollIntoView({ block: 'start' });
  });
  block.appendChild(mapButton);
  if (tokenHooks && item.projectTokenId && item.adapterId) {
    if (canReviewMappings(data)) {
      reasonAction(block, t('design.projectOnly.keepLocal'), t('design.mapping.addToBasket'), function (reason) {
        return tokenHooks.addOperation({
          op: 'add-disposition', side: 'project',
          projectTokenId: item.projectTokenId, adapterId: item.adapterId,
          kind: 'project-only-intentional', reason: reason
        }, t('design.projectOnly.keepLocal') + ': ' + (item.name || item.projectTokenId));
      });
    } else {
      block.appendChild(el('p', {
        class: 'design-suggestion__note', text: t('design.mapping.confirmRequiresComparison')
      }));
    }
  }
  return block;
}
function renderProjectToken(data) {
  var item = data.token;
  if (tokenHooks && typeof tokenHooks.adoptAnalysis === 'function') tokenHooks.adoptAnalysis(data.analysis);
  title.textContent = item.name || t('design.details');
  var classification = item.classification === 'unclassified' ? t('design.projectOnly.unclassified') :
    item.classification === 'project-only-intentional' ? t('design.projectOnly.intentional') :
      item.classification === 'superseded' ? t('design.projectOnly.superseded') :
        String(item.classification || t('design.unknown'));
  var suggestionsBlock = suggestionsSection(item, 'project-token', data);
  body.replaceChildren(
    definitionsSection(t('design.detail.summary'), [
      [t('design.field.type'), designFilters.tokenKindText(item.kind)],
      [t('design.field.layer'), item.layer],
      [t('design.projectOnly.classify'), classification]
    ]),
    tokenIdentitySection(item),
    sourcesSection({ sources: { code: { symbol: item.sourceSymbol, path: item.sourcePath } } }),
    suggestionsBlock,
    projectTokenActionsSection(item, data, suggestionsBlock),
    tasksSection(item.tasks)
  );
}
function open(type, id, revision, trigger, closed) {
  if (!dialog) build();
  var key = type + ':' + id + ':' + revision;
  var same = dialog.open && currentKey === key;
  currentKey = key;
  if (!same) opener = trigger || document.activeElement;
  onClose = closed || onClose;
  title.textContent = t('design.details');
  body.replaceChildren(el('p', { class: 'design-state', text: t('design.loading') }));
  if (!dialog.open) {
    if (dialog.showModal) dialog.showModal(); else dialog.setAttribute('open', '');
  }
  var endpoint = type === 'component' || type === 'project-component' ? '/api/design/components/'
    : type === 'token' || type === 'project-token' ? '/api/design/tokens/'
      : '/api/design/surfaces/';
  designFilters.request(endpoint + encodeURIComponent(id) + '?expectedGenerationRevision=' + encodeURIComponent(revision || '')).then(function (data) {
    if (currentKey !== key) return;
    if (type === 'component') renderComponent(data);
    else if (type === 'project-component') renderProjectComponent(data);
    else if (type === 'token') renderToken(data);
    else if (type === 'project-token') renderProjectToken(data);
    else renderSurface(data);
  }).catch(function (error) {
    if (currentKey !== key) return;
    var message = el('p', {
      class: 'design-state design-state--error',
      text: errorMessage(error)
    });
    var reload = el('button', {
      type: 'button', class: 'btn btn--small', text: t('design.reloadDetail')
    });
    reload.addEventListener('click', function () {
      reload.disabled = true;
      designFilters.request('/api/design/overview').then(function (overview) {
        open(type, id, overview.generationRevision, opener, closed);
      }).catch(function (reloadError) {
        reload.disabled = false;
        message.textContent = errorMessage(reloadError);
      });
    });
    body.replaceChildren(message, reload);
  });
}

function refresh() {
  if (closeButton) closeButton.textContent = t('design.close');
}
function close(silent) {
  if (!dialog || !dialog.open) return;
  if (silent) {
    onClose = null;
    opener = null;
  }
  requestClose();
}

export const designEntityDrawer = {
  element: function () { return dialog || build(); },
  open: open,
  close: close,
  refresh: refresh,
  setTokenHooks: function (hooks) {
    tokenHooks = hooks && typeof hooks === 'object' ? hooks : null;
  },
  setComponentHooks: function (hooks) {
    componentHooks = hooks && typeof hooks === 'object' ? hooks : null;
  }
};
