export function createFigmaScreensView(dependencies) {
  var el = dependencies.el;
  var t = dependencies.t;
  var relativeTime = dependencies.relativeTime;
  var createTextNode = dependencies.createTextNode;

  function clearBody(bodyEl) {
    while (bodyEl.firstChild) bodyEl.removeChild(bodyEl.firstChild);
  }

  // The compact spec line for one pulled screen. Sparse specs omit absent
  // fields instead of exposing null-like placeholders.
  function screenSpecLine(node) {
    var parts = [];
    var fs = node.frameSizeDp;
    if (fs && typeof fs.w === 'number' && typeof fs.h === 'number') parts.push(fs.w + '×' + fs.h);
    if (node.theme) parts.push(String(node.theme));
    if (typeof node.elementCount === 'number') parts.push(t('board.screens.elements', { n: node.elementCount }));
    if (typeof node.instanceCount === 'number') parts.push(t('board.screens.instances', { n: node.instanceCount }));
    return parts.join(' · ');
  }

  function buildScreenCard(stem, node) {
    var card = el('div', { class: 'board-screens__card' });
    var name = String(node.screen || '');
    var dark = node.darkTheme || null;
    var prefersDark = !node.hasPng && dark && dark.hasPng;
    var displayNode = prefersDark ? Object.assign({}, node, dark, { instanceCount: node.instanceCount }) : node;
    var theme = prefersDark ? 'dark' : '';
    var url = (prefersDark && dark && dark.url) ? dark.url : (node.url || '');

    var imgSrc = '/api/figma/screen-image?stem=' + encodeURIComponent(stem) + '&screen=' + encodeURIComponent(name) +
      (theme ? '&theme=' + encodeURIComponent(theme) : '');
    var img = el('img', {
      class: 'board-screens__img',
      attrs: { loading: 'lazy', src: imgSrc, alt: name }
    });
    var shotFallback = el('span', {
      class: 'board-screens__fallback',
      text: t('board.screens.previewUnavailable')
    });
    img.addEventListener('load', function () { shotFallback.hidden = true; });
    img.addEventListener('error', function () {
      img.hidden = true;
      shotFallback.hidden = false;
    });
    shotFallback.hidden = true;
    if (url) {
      var link = el('a', {
        class: 'board-screens__shot',
        href: url,
        attrs: { target: '_blank', rel: 'noopener noreferrer', title: t('board.screens.openFigma') }
      });
      link.appendChild(img);
      link.appendChild(shotFallback);
      card.appendChild(link);
    } else {
      var frame = el('span', { class: 'board-screens__shot' });
      frame.appendChild(img);
      frame.appendChild(shotFallback);
      card.appendChild(frame);
    }

    var meta = el('div', { class: 'board-screens__meta' });
    meta.appendChild(el('span', { class: 'board-screens__name', text: name }));
    if (url) {
      meta.appendChild(el('a', {
        class: 'board-screens__figma-link',
        href: url,
        text: t('board.screens.openFigma'),
        attrs: { target: '_blank', rel: 'noopener noreferrer' }
      }));
    }
    if (displayNode.fetchedAt) {
      meta.appendChild(el('span', {
        class: 'board-screens__pulled',
        text: t('board.screens.pulledAgo', { ago: relativeTime(displayNode.fetchedAt) })
      }));
    }
    var spec = screenSpecLine(displayNode);
    if (spec) meta.appendChild(el('span', { class: 'board-screens__spec', text: spec }));
    card.appendChild(meta);
    return card;
  }

  function buildCensusTally(census) {
    var row = el('div', { class: 'board-screens__census' });
    row.appendChild(el('span', { class: 'board-screens__census-label', text: t('board.screens.census') }));
    function cell(labelKey, n, warn) {
      var c = el('span', { class: 'board-screens__census-cell' + (warn && n > 0 ? ' board-screens__census-cell--warn' : '') });
      c.appendChild(createTextNode(t(labelKey, { n: typeof n === 'number' ? n : 0 })));
      return c;
    }
    row.appendChild(cell('board.screens.censusMapped', census.mapped, false));
    row.appendChild(cell('board.screens.censusMissing', census.missing, true));
    row.appendChild(cell('board.screens.censusIncomplete', census.incomplete, false));
    row.appendChild(cell('board.screens.censusAmbiguous', census.ambiguous, true));
    return row;
  }

  function buildSection() {
    var section = el('div', { class: 'board-screens', attrs: { tabindex: '0' } });
    var body = el('div', { class: 'board-screens__body' });
    body.appendChild(el('p', { class: 'panel-lead', text: t('board.screens.loading') }));
    section.appendChild(body);
    return { section: section, body: body };
  }

  function renderResponse(bodyEl, stem, resp) {
    clearBody(bodyEl);
    var nodes = (resp && Array.isArray(resp.nodes)) ? resp.nodes : [];
    if (!resp || resp.present === false || nodes.length === 0) {
      bodyEl.appendChild(el('p', { class: 'board-screens__empty', text: t('board.screens.empty') }));
      return;
    }
    var grid = el('div', { class: 'board-screens__grid' });
    var cards = [];
    for (var i = 0; i < nodes.length; i++) {
      var cardEl = buildScreenCard(stem, nodes[i]);
      cards.push({ node: cardEl, key: String((nodes[i] && nodes[i].screen) || '').toLowerCase() });
      grid.appendChild(cardEl);
    }
    if (nodes.length > 10) {
      var filterInput = el('input', {
        type: 'text',
        class: 'input board-modal__input board-screens__filter',
        attrs: { placeholder: t('board.screens.filterPlaceholder'), 'aria-label': t('board.screens.filterPlaceholder') }
      });
      filterInput.addEventListener('input', function () {
        var q = String(filterInput.value || '').trim().toLowerCase();
        for (var j = 0; j < cards.length; j++) {
          cards[j].node.hidden = !!q && cards[j].key.indexOf(q) < 0;
        }
      });
      bodyEl.appendChild(filterInput);
    }
    bodyEl.appendChild(grid);
    if (resp.census) bodyEl.appendChild(buildCensusTally(resp.census));
  }

  function renderError(bodyEl, detail) {
    clearBody(bodyEl);
    bodyEl.appendChild(el('p', {
      class: 'board-screens__empty',
      text: t('board.screens.error', { detail: detail })
    }));
  }

  return {
    buildSection: buildSection,
    renderResponse: renderResponse,
    renderError: renderError
  };
}
