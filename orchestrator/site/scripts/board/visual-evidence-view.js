import {
  artifactSetReportHashOk,
  finalVisualTrustState
} from './visual-evidence-trust.js';

export function createVisualEvidenceView(dependencies) {
  var el = dependencies.el;
  var t = dependencies.t;
  var pluralTemplate = dependencies.pluralTemplate;
  var evidenceStatusClass = dependencies.evidenceStatusClass;
  var evidenceStatusLabel = dependencies.evidenceStatusLabel;

  function buildEvidenceBadge(status) {
    var cls = evidenceStatusClass(status);
    return el('span', {
      class: 'board-evidence__badge board-evidence__badge--' + cls,
      text: evidenceStatusLabel(status)
    });
  }

  function artifactKindLabel(kind) {
    var key = 'board.figmaEvidence.artifact.' + String(kind || '');
    var label = t(key);
    return label === key ? t('board.figmaEvidence.artifact.unknown') : label;
  }

  function artifactHref(stem, ref, reportHash) {
    return '/api/figma/compare-artifact?stem=' + encodeURIComponent(stem || '') +
      '&id=' + encodeURIComponent((ref && ref.id) || '') +
      '&reportHash=' + encodeURIComponent(reportHash || '');
  }

  // Pane label for the comparison: the left/right panes get the fuller
  // "Figma reference" / "App screenshot" strings the owner asked for; any other
  // kind falls back to the compact artifact label.
  function comparePaneLabel(kind) {
    if (kind === 'figma') return t('board.figmaEvidence.compare.figmaPane');
    if (kind === 'actual') return t('board.figmaEvidence.compare.actualPane');
    return artifactKindLabel(kind);
  }

  function compareArtifactImg(src, title, kindLabel, extraClass) {
    return el('img', {
      class: 'board-evidence__compare-img' + (extraClass ? ' ' + extraClass : ''),
      attrs: { src: src, alt: kindLabel + ' — ' + title, loading: 'lazy', decoding: 'async' }
    });
  }

  // One static side pane (the Figma reference or the app screenshot): a label,
  // the image (degrading to a neutral note on a refused/missing artifact) and an
  // "open full" link so a single image can still be opened large in a new tab.
  function compareStaticPane(modifier, kind, src, title) {
    var col = el('div', { class: 'board-evidence__compare-pane board-evidence__compare-pane--' + modifier });
    col.appendChild(el('div', { class: 'board-evidence__compare-label', text: comparePaneLabel(kind) }));
    var frame = el('div', { class: 'board-evidence__compare-frame' });
    if (src) {
      var fb = el('span', { class: 'board-evidence__compare-fallback', text: t('board.figmaEvidence.artifact.unavailable') });
      fb.hidden = true;
      var img = compareArtifactImg(src, title, artifactKindLabel(kind));
      img.addEventListener('error', function () { img.hidden = true; fb.hidden = false; });
      frame.appendChild(img);
      frame.appendChild(fb);
    } else {
      frame.appendChild(el('span', { class: 'board-evidence__compare-fallback', text: t('board.figmaEvidence.compare.missing') }));
    }
    col.appendChild(frame);
    if (src) {
      col.appendChild(el('a', {
        class: 'board-evidence__compare-open',
        text: t('board.figmaEvidence.compare.openFull'),
        href: src,
        attrs: { target: '_blank', rel: 'noopener noreferrer' }
      }));
    }
    return col;
  }

  // The interactive middle pane. Onion-skin: figma is the base layer, the app
  // screenshot is stacked on top and its opacity is driven by the slider — drag
  // to fade between the design and the build (the literal "layer on layer"). A
  // segmented toggle swaps to the precomputed difference image. A hidden sizer
  // image gives the absolutely-stacked layers their height.
  // `diffIsOverlay`: the "difference" slot actually holds the precomputed onion-skin
  // OVERLAY composite (no per-pixel diff artifact exists) — label the toggle + layer
  // honestly so a blended composite is never read as a per-pixel diff.
  function buildCompareMiddle(figmaSrc, actualSrc, diffSrc, title, diffIsOverlay) {
    var col = el('div', { class: 'board-evidence__compare-pane board-evidence__compare-pane--compare' });
    col.appendChild(el('div', { class: 'board-evidence__compare-label', text: t('board.figmaEvidence.compare.heading') }));

    var canOverlay = !!(figmaSrc && actualSrc);
    var canDiff = !!diffSrc;
    if (!canOverlay && !canDiff) {
      var lone = figmaSrc || actualSrc || diffSrc;
      var frameLone = el('div', { class: 'board-evidence__compare-frame' });
      if (lone) frameLone.appendChild(compareArtifactImg(lone, title, t('board.figmaEvidence.compare.heading')));
      else frameLone.appendChild(el('span', { class: 'board-evidence__compare-fallback', text: t('board.figmaEvidence.compare.missing') }));
      col.appendChild(frameLone);
      return col;
    }

    var stage = el('div', { class: 'board-evidence__compare-frame board-evidence__compare-stage' });
    stage.appendChild(el('img', {
      class: 'board-evidence__compare-sizer',
      attrs: { src: figmaSrc || actualSrc || diffSrc, alt: '', loading: 'lazy', decoding: 'async', 'aria-hidden': 'true' }
    }));
    var baseLayer = figmaSrc ? compareArtifactImg(figmaSrc, title, artifactKindLabel('figma'), 'board-evidence__compare-layer') : null;
    var topLayer = actualSrc ? compareArtifactImg(actualSrc, title, artifactKindLabel('actual'), 'board-evidence__compare-layer') : null;
    var diffLayer = diffSrc ? compareArtifactImg(diffSrc, title, artifactKindLabel(diffIsOverlay ? 'overlay' : 'diff'), 'board-evidence__compare-layer') : null;
    if (baseLayer) stage.appendChild(baseLayer);
    if (topLayer) stage.appendChild(topLayer);
    if (diffLayer) stage.appendChild(diffLayer);
    col.appendChild(stage);

    var controls = el('div', { class: 'board-evidence__compare-controls' });
    var slider = el('label', { class: 'board-evidence__compare-slider' });
    var sliderInput = el('input', {
      type: 'range',
      class: 'range-input',
      attrs: { min: '0', max: '100', value: '50', 'aria-label': t('board.figmaEvidence.compare.opacityAria') }
    });
    slider.appendChild(el('span', { text: t('board.figmaEvidence.compare.opacity') }));
    slider.appendChild(sliderInput);
    function applyOpacity() {
      if (topLayer) topLayer.style.opacity = String((parseInt(sliderInput.value, 10) || 0) / 100);
    }
    sliderInput.addEventListener('input', applyOpacity);

    var modeWrap = el('div', { class: 'board-evidence__compare-modes', attrs: { role: 'group' } });
    var modes = [];
    if (canOverlay) modes.push('overlay');
    if (canDiff) modes.push('diff');
    // Always open on the onion-skin overlay when both layers exist (the headline
    // "layer on layer" comparison); Difference is one click away.
    var active = modes[0];
    var modeBtns = {};
    function setMode(mode) {
      active = mode;
      var overlayOn = mode === 'overlay';
      if (baseLayer) baseLayer.style.display = overlayOn ? '' : 'none';
      if (topLayer) topLayer.style.display = overlayOn ? '' : 'none';
      if (diffLayer) diffLayer.style.display = overlayOn ? 'none' : '';
      slider.hidden = !overlayOn;
      for (var k in modeBtns) if (Object.prototype.hasOwnProperty.call(modeBtns, k)) {
        modeBtns[k].classList.toggle('board-evidence__compare-mode--active', k === mode);
        modeBtns[k].setAttribute('aria-pressed', k === mode ? 'true' : 'false');
      }
    }
    if (modes.length > 1) {
      modes.forEach(function (mode) {
        var btn = el('button', {
          type: 'button',
          class: 'board-evidence__compare-mode',
          text: mode === 'overlay' ? t('board.figmaEvidence.compare.modeOverlay')
            : (diffIsOverlay ? artifactKindLabel('overlay') : t('board.figmaEvidence.compare.modeDiff')),
          attrs: { 'aria-pressed': 'false' }
        });
        btn.addEventListener('click', function () { setMode(mode); });
        modeBtns[mode] = btn;
        modeWrap.appendChild(btn);
      });
      controls.appendChild(modeWrap);
    }
    if (canOverlay) controls.appendChild(slider);
    col.appendChild(controls);

    applyOpacity();
    setMode(active);
    return col;
  }

  // The three-up comparison for one visual check: Figma reference (left), an
  // interactive comparison (middle), and the app screenshot (right). All images
  // come from the vetted /api/figma/compare-artifact endpoint — the browser
  // never sees a file path. Returns the "unavailable" note when the evidence is
  // not a fresh, trusted final check. Degrades
  // gracefully when an individual artifact is absent.
  function buildCompareRow(stem, artifactSet, reportHash, title, opts) {
    opts = opts || {};
    var artifacts = artifactSet && artifactSet.artifacts;
    if (!artifacts || typeof artifacts !== 'object') return null;
    if ((opts.trust && !opts.trust.usable) || !artifactSetReportHashOk(artifactSet, reportHash)) {
      return el('p', { class: 'board-evidence__artifact-unavailable', text: t('board.figmaEvidence.artifact.unavailable') });
    }
    var hash = reportHash || artifactSet.reportHash;
    function hrefFor(kind) {
      var ref = artifacts[kind];
      return (ref && ref.id) ? artifactHref(stem, ref, hash) : null;
    }
    var figmaSrc = hrefFor('figma');
    var actualSrc = hrefFor('actual');
    var diffSrc = hrefFor('diff');
    var overlaySrc = hrefFor('overlay');
    if (!figmaSrc && !actualSrc && !diffSrc && !overlaySrc) return null;
    // The middle "difference" toggle prefers the per-pixel diff, but falls back
    // to the precomputed onion-skin overlay when no diff was produced — the flag
    // makes buildCompareMiddle label that fallback as Overlay, not Difference.
    var middleDiff = diffSrc || overlaySrc;
    var middleIsOverlay = !diffSrc && !!overlaySrc;
    var wrap = el('div', { class: 'board-evidence__compare' });
    wrap.appendChild(compareStaticPane('figma', 'figma', figmaSrc, title));
    wrap.appendChild(buildCompareMiddle(figmaSrc, actualSrc, middleDiff, title, middleIsOverlay));
    wrap.appendChild(compareStaticPane('actual', 'actual', actualSrc, title));
    return wrap;
  }

  function visualTitle(row) {
    var parts = [];
    if (row && row.screen) parts.push(row.screen);
    if (row && row.theme) parts.push(row.theme);
    return parts.join(' · ') || t('board.figmaEvidence.visualUntitled');
  }

  // The masked-ssim-luma-v2 metric is deliberately unclamped (mean SSIM goes negative on
  // anti-correlated content, e.g. a theme-inverted render) and the server
  // forwards any finite number. A DISPLAYED percentage/ratio must still read
  // sanely, so every render site clamps to [0,1] via this helper and keeps the
  // raw value in a title attribute for diagnostics. Display-only — never feed a
  // clamped value back into any comparison logic.
  function clampUnit(v) {
    return Math.max(0, Math.min(1, v));
  }

  function visualMeta(row) {
    var parts = [];
    if (row && typeof row.score === 'number') parts.push('SSIM ' + row.score.toFixed(3));
    if (row && typeof row.coverage === 'number') parts.push(t('board.figmaEvidence.visualCoverage', { n: Math.round(clampUnit(row.coverage) * 100) }));
    if (row && typeof row.worstRegionDeltaE === 'number' && isFinite(row.worstRegionDeltaE)) parts.push('ΔE ' + row.worstRegionDeltaE.toFixed(1));
    if (row && row.colorStatus === 'REVIEW') parts.push(t('board.figmaEvidence.colorReview'));
    if (row && row.pixelStatus) parts.push(t('board.figmaEvidence.rawPixelStatus', { gate: row.pixelGate || 'unknown', status: evidenceStatusLabel(row.pixelStatus) }));
    if (row && row.reason) parts.push(row.reason);
    return parts.join(' · ');
  }

  function visualEffectiveStatus(row) {
    return (row && row.pixelStatus) || (row && row.status);
  }

  // Per-Figma-node breakdown — "which element diverged". The server sends zones
  // worst (lowest SSIM) first, capped; each is a thin status-coloured bar so the
  // weak elements read at a glance. Tucked in a <details>, opened only when the
  // worst zone is a problem, so a clean screen stays uncluttered.
  function buildZonesList(zones) {
    if (!Array.isArray(zones) || !zones.length) return null;
    var worst = zones[0] || {};
    var box = el('details', { class: 'board-evidence__zones' });
    // Open when ANY zone is a problem (not only the worst-SSIM one), so an
    // off-sorted/corrupt status can't hide a flagged element behind a collapsed panel.
    box.open = zones.some(function (z) { return z && evidenceStatusClass(z.status) !== 'pass'; });
    var sum = t('board.figmaEvidence.zones.heading') + ' · ' + zones.length;
    var worstHasSsim = typeof worst.ssim === 'number' && isFinite(worst.ssim);
    // Clamp the displayed worst score; the raw value rides in the title.
    if (worstHasSsim) sum += ' · ' + t('board.figmaEvidence.zones.worst', { score: clampUnit(worst.ssim).toFixed(2) });
    box.appendChild(el('summary', {
      class: 'board-evidence__zones-summary',
      text: sum,
      attrs: worstHasSsim ? { title: 'ssim ' + worst.ssim } : {}
    }));
    for (var i = 0; i < zones.length; i++) {
      var z = zones[i] || {};
      var label = z.name || z.role || z.stableId || '—';
      var r = el('div', { class: 'board-evidence__zone' });
      r.appendChild(el('span', { class: 'board-evidence__zone-name', text: label, attrs: { title: label } }));
      var bar = el('span', { class: 'board-evidence__zone-bar' });
      var fill = el('span', { class: 'board-evidence__zone-fill board-evidence__zone-fill--' + evidenceStatusClass(z.status) });
      var hasSsim = typeof z.ssim === 'number' && isFinite(z.ssim);
      fill.style.width = (Math.max(0, Math.min(1, hasSsim ? z.ssim : 0)) * 100).toFixed(1) + '%';
      bar.appendChild(fill);
      r.appendChild(bar);
      // Clamp the displayed per-zone score; raw stays in the title.
      var scoreText = hasSsim ? clampUnit(z.ssim).toFixed(2) : '—';
      if (typeof z.deltaE === 'number' && isFinite(z.deltaE)) scoreText += ' · ΔE' + z.deltaE.toFixed(1);
      r.appendChild(el('span', {
        class: 'board-evidence__zone-score',
        text: scoreText,
        attrs: hasSsim ? { title: 'ssim ' + z.ssim } : {}
      }));
      box.appendChild(r);
    }
    return box;
  }

  function buildEvidenceVisuals(resp, stem, opts) {
    opts = opts || {};
    if (!opts.trust) opts.trust = finalVisualTrustState(resp);
    var checks = resp && resp.visualChecks;
    var entries = checks && Array.isArray(checks.entries) ? checks.entries : [];
    if (!entries.length) return null;
    // Count problems from the SHOWN (≤48-capped) entries so the summary agrees with the
    // "Problems (N)" group label; the truncation note below accounts for any rows beyond the cap.
    // (Using the server's un-capped checks.problemCount here would contradict the group counts.)
    var problemCount = entries.filter(function (row) { return evidenceStatusClass(visualEffectiveStatus(row)) !== 'pass'; }).length;
    var box = el('details', {
      class: 'board-evidence__visuals'
    });
    if (problemCount) box.open = true;
    var total = checks.total || entries.length;
    var summaryText = t('board.figmaEvidence.visualChecks') + ' · ' +
      pluralTemplate('board.figmaEvidence.visualSummary', total, { shown: entries.length, total: total });
    if (problemCount) summaryText += ' · ' + pluralTemplate('board.figmaEvidence.visualProblems', problemCount);
    // Truncation flagged in the summary line itself (a short marker), not only in
    // the footnote below the list — on a long list the footnote scrolled out of
    // sight and "{shown} of {total}" alone was easy to misread as complete.
    if (checks.truncated) summaryText += ' · ' + t('board.figmaEvidence.visualTruncatedSummary');
    box.appendChild(el('summary', { class: 'board-evidence__visuals-summary', text: summaryText }));
    // Scale affordance: >10 comparison rows get a client-only substring filter on
    // the screen name (visualTitle). Pure display — rows are hidden, never
    // removed; the group counts above keep describing the FULL list.
    var filterables = [];
    var filterInput = null;
    function applyFilter() {
      if (!filterInput) return;
      var q = String(filterInput.value || '').trim().toLowerCase();
      for (var f = 0; f < filterables.length; f++) {
        filterables[f].node.hidden = !!q && filterables[f].key.indexOf(q) < 0;
      }
    }
    if (entries.length > 10) {
      filterInput = el('input', {
        type: 'text',
        class: 'input board-modal__input board-evidence__filter',
        value: opts.filterValue || '',
        attrs: { placeholder: t('board.figmaEvidence.filterPlaceholder'), 'aria-label': t('board.figmaEvidence.filterPlaceholder') }
      });
      filterInput.addEventListener('input', function () {
        if (typeof opts.onFilterInput === 'function') opts.onFilterInput(filterInput.value);
        applyFilter();
      });
      box.appendChild(filterInput);
    }
    // One comparison row: status badge + title + optional similarity %, then the
    // three-up images when they exist AND are trusted (buildCompareRow degrades to
    // null / an "unavailable" note otherwise), then per-element zones. A row with no
    // artifacts (a comparison that bailed early — ASPECT_MISMATCH / MISSING_CAPTURE)
    // still renders its head, so "which screen broke and how" is always visible.
    function renderVisualItem(row) {
      row = row || {};
      var title = visualTitle(row);
      var effectiveStatus = visualEffectiveStatus(row);
      var item = el('div', { class: 'board-evidence__visual board-evidence__visual--' + evidenceStatusClass(effectiveStatus) });
      var head = el('div', { class: 'board-evidence__visual-head' });
      head.appendChild(buildEvidenceBadge(effectiveStatus));
      var copy = el('div', { class: 'board-evidence__visual-copy' });
      copy.appendChild(el('div', { class: 'board-evidence__visual-title', text: title }));
      var meta = visualMeta(row);
      if (meta) copy.appendChild(el('div', { class: 'board-evidence__visual-meta', text: meta }));
      head.appendChild(copy);
      // Done "Figma" view: promote the similarity to a first-class number in the row header.
      // Display clamped to [0,100]% (a negative SSIM read as "similarity -34.2%",
      // i.e. a rendering bug); the raw score stays in the title for diagnostics.
      if (opts.promoteScore && typeof row.score === 'number') {
        head.appendChild(el('div', {
          class: 'board-evidence__visual-score board-evidence__visual-score--' + evidenceStatusClass(effectiveStatus),
          text: t('board.figmaEvidence.similarity', { n: (clampUnit(row.score) * 100).toFixed(1) }),
          attrs: { title: 'SSIM ' + row.score }
        }));
      }
      item.appendChild(head);
      var compare = buildCompareRow(stem, row.artifactSet, checks.reportHash, title, opts);
      if (compare) item.appendChild(compare);
      // Per-element zones only when the evidence is trusted — same gate as the
      // artifacts, so untrusted/stale runs never show potentially-misleading numbers.
      var zonesList = (opts.trust && opts.trust.usable) ? buildZonesList(row.zones) : null;
      if (zonesList) item.appendChild(zonesList);
      if (typeof opts.buildVisualActions === 'function') {
        var rowActions = opts.buildVisualActions(row, resp);
        if (rowActions) item.appendChild(rowActions);
      }
      if (filterInput) filterables.push({ node: item, key: String(title || '').toLowerCase() });
      return item;
    }
    if (opts.groupByStatus) {
      // Split into "Problems" (anything not PASS) and "Matched", each under a labelled
      // subheader — the diagnostic list the owner asked for, problems first.
      var problems = entries.filter(function (r) { return evidenceStatusClass(visualEffectiveStatus(r)) !== 'pass'; });
      var passes = entries.filter(function (r) { return evidenceStatusClass(visualEffectiveStatus(r)) === 'pass'; });
      [{ rows: problems, key: 'board.figmaEvidence.groupProblems', cls: 'problem' },
       { rows: passes, key: 'board.figmaEvidence.groupPassing', cls: 'pass' }].forEach(function (g) {
        if (!g.rows.length) return;
        box.appendChild(el('div', {
          class: 'board-evidence__group-label board-evidence__group-label--' + g.cls,
          text: t(g.key, { n: g.rows.length })
        }));
        g.rows.forEach(function (row) { box.appendChild(renderVisualItem(row)); });
      });
    } else {
      for (var i = 0; i < entries.length; i++) box.appendChild(renderVisualItem(entries[i]));
    }
    applyFilter();
    if (checks.truncated) box.appendChild(el('p', { class: 'board-evidence__truncated', text: t('board.figmaEvidence.visualTruncated') }));
    return box;
  }

  return {
    artifactHref: artifactHref,
    buildEvidenceBadge: buildEvidenceBadge,
    buildEvidenceVisuals: buildEvidenceVisuals,
    visualTitle: visualTitle
  };
}
