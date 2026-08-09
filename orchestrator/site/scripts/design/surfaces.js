import { dom } from '../dom.js';
import { i18n } from '../i18n.js';
import { designFilters } from './filters.js';

var el = dom.el;
function t(key, params) { return i18n.t(key, params); }
function themeLabel(value) {
  return value === 'light' || value === 'dark' ? t('design.theme.' + value) : t('design.unknown');
}
function previewImage(src, alt) {
  var image = el('img', { src: src, loading: 'lazy', alt: alt });
  image.addEventListener('error', function () {
    image.replaceWith(el('div', {
      class: 'design-card__placeholder', text: t('design.imageUnavailable')
    }));
  }, { once: true });
  return image;
}
function appendCards(list, rows, context, revision) {
  rows.forEach(function (row) {
    var card = el('li', { class: 'design-surface-card' });
    if (row.findingId && !row.openTask) {
      var select = el('input', {
        type: 'checkbox', class: 'choice-input design-card__select',
        checked: context.selected.has(row.findingId),
        attrs: {
          'aria-label': t('design.selectFinding', { name: row.name }),
          'data-design-focus': 'surface-finding:' + row.findingId
        }
      });
      select.addEventListener('change', function () { context.toggleFinding(row.findingId, select.checked); });
      card.appendChild(select);
    }
    var open = el('button', {
      type: 'button', class: 'design-surface-card__open',
      attrs: {
        'aria-label': t('design.surfaces.openDetail', { name: row.name }),
        'data-design-focus': 'surface:' + row.id
      }
    });
    var variant = row.thumbnail || row.variants && row.variants.find(function (item) {
      return item.captured && item.imageUrl;
    });
    if (variant) open.appendChild(previewImage(
      variant.imageUrl + '&expectedGenerationRevision=' + encodeURIComponent(revision),
      t('design.surfacePreviewAlt', { name: row.name, theme: themeLabel(variant.theme) })
    ));
    else open.appendChild(el('div', { class: 'design-card__placeholder', text: t('design.notCaptured') }));
    var copy = el('span', { class: 'design-surface-card__copy' });
    copy.appendChild(el('strong', { text: row.name }));
    copy.appendChild(el('span', {
      text: [
        designFilters.localizedEnum('design.surface.', row.type, 'design.surface.unknown'),
        row.feature || row.module
      ].filter(Boolean).join(' · ')
    }));
    copy.appendChild(el('span', {
      text: (row.themes || []).map(themeLabel).join(', ') + ' · ' + (row.locales || []).join(', ')
    }));
    open.appendChild(copy);
    open.addEventListener('click', function () { context.openEntity('surface', row.id, open); });
    card.appendChild(open);
    var footer = el('div', { class: 'design-card__footer' });
    footer.appendChild(el('span', {
      class: 'design-status design-status--' + row.status,
      text: designFilters.statusText(row.status)
    }));
    if (row.openTask) footer.appendChild(el('a', {
      href: '#board?task=' + encodeURIComponent(row.openTask.stem),
      text: t('design.openTask')
    }));
    card.appendChild(footer);
    list.appendChild(card);
  });
}
function addPager(root, list, data, context) {
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
    designFilters.request('/api/design/surfaces?' + query + '&cursor=' + encodeURIComponent(data.nextCursor)).then(function (next) {
      if (!context.isCurrent()) return;
      appendCards(list, next.items, context, next.generationRevision);
      button.remove();
      failure.remove();
      addPager(root, list, next, context);
    }).catch(function (error) {
      button.disabled = false; button.textContent = t('design.loadMore');
      failure.hidden = false;
      failure.textContent = designFilters.errorMessage(error);
    });
  });
  root.appendChild(button);
  root.appendChild(failure);
}
function render(root, context) {
  return designFilters.request('/api/design/surfaces?' + designFilters.query(context.state)).then(function (data) {
    if (!context.isCurrent()) return;
    context.adopt(data);
    if (!context.shouldRender(data)) return;
    root.replaceChildren();
    var toggle = el('div', { class: 'design-view-toggle', attrs: { role: 'group', 'aria-label': t('design.viewAria') } });
    ['gallery', 'list'].forEach(function (view) {
      var button = el('button', {
        type: 'button', class: 'btn btn--small' + (context.state.view === view ? '' : ' btn--ghost'),
        text: t('design.view.' + view), attrs: { 'aria-pressed': context.state.view === view ? 'true' : 'false' }
      });
      button.addEventListener('click', function () { context.setState({ view: view }, true); });
      toggle.appendChild(button);
    });
    root.appendChild(toggle);
    designFilters.appendLimitations(root, data.limitations);
    if (!data.items.length) {
      root.appendChild(el('p', { class: 'design-state', text: t('design.empty.surfaces') })); return;
    }
    var list = el('ul', { class: context.state.view === 'list' ? 'design-surface-list' : 'design-surface-gallery' });
    appendCards(list, data.items, context, data.generationRevision);
    root.appendChild(list);
    addPager(root, list, data, context);
  });
}

export const designSurfaces = { render: render };
