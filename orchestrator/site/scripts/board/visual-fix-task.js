export function createVisualFixTask(dependencies) {
  var artifactHref = dependencies.artifactHref;
  var visualTitle = dependencies.visualTitle;

  // A real, pullable Figma node URL inside a `## Design` bullet. This is the
  // shared predicate for both Pull Screens visibility and fix-task Design copy.
  var FIGMA_NODE_URL_RE = /https:\/\/(?:www\.)?figma\.com\/(?:design|file)\/[A-Za-z0-9]+(?:\/[^?\s#]*)?\?[^ \t\r\n#]*node-id=([0-9]+[:-][0-9]+)/i;

  function safeInline(value, limit) {
    var s = String(value == null ? '' : value).replace(/\s+/g, ' ').trim();
    if (limit && s.length > limit) return s.substring(0, limit - 1) + '…';
    return s;
  }

  function slugPart(value) {
    return String(value || 'default')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '') || 'default';
  }

  function visualFixKey(stem, row) {
    return slugPart(stem) + ':' + slugPart(row && row.screen) + ':' + slugPart(row && row.theme);
  }

  function visualFixMarker(stem, row, reportHash) {
    return '<!-- figma-visual-fix key=' + visualFixKey(stem, row) +
      ' stem=' + stem + ' report=' + String(reportHash || '') + ' -->';
  }

  // The `## Design` section body, '' when absent. An empty section whose
  // header abuts the next `##` must not over-capture that following section.
  function designSectionOf(body) {
    var s = String(body || '');
    var h = s.match(/(?:^|\n)##[ \t]+Design[ \t]*\r?\n/);
    if (!h) return '';
    var rest = s.slice(h.index + h[0].length);
    var end = rest.search(/(?:^|\r?\n)##[ \t]/);
    return end === -1 ? rest : rest.slice(0, end);
  }

  // Split at the first supported bullet separator and test only the value.
  // A `none` value and the scaffold placeholder are never pullable.
  function isPullableDesignLine(line) {
    var s = String(line || '').trim();
    if (s.charAt(0) !== '-' || s.indexOf('<') !== -1) return false;
    var rest = s.replace(/^-\s*/, '');
    var idx = rest.indexOf(' — ');
    var sepLen = 3;
    if (idx < 0) { idx = rest.indexOf(' - '); sepLen = 3; }
    if (idx < 0) { idx = rest.indexOf(' -- '); sepLen = 4; }
    if (idx < 0) return false;
    var value = rest.slice(idx + sepLen).trim();
    if (/^none\b/i.test(value)) return false;
    return FIGMA_NODE_URL_RE.test(value);
  }

  function bodyHasPullableDesign(body) {
    if (!body) return false;
    var section = designSectionOf(body);
    return !!section && section.split('\n').some(isPullableDesignLine);
  }

  function pullableBulletCount(body) {
    if (!body) return 0;
    var section = designSectionOf(body);
    return section ? section.split('\n').filter(isPullableDesignLine).length : 0;
  }

  function extractDesignSectionBullets(markdown) {
    var section = designSectionOf(markdown);
    if (!section) return [];
    var out = [];
    var lines = section.split(/\r?\n/);
    for (var i = 0; i < lines.length; i++) {
      var s = lines[i].trim();
      if (s.charAt(0) === '-') out.push(s);
    }
    return out;
  }

  function isFigmaDesignBullet(line) {
    return isPullableDesignLine(line);
  }

  function designBulletMatchesVisual(line, row) {
    var hay = slugPart(line);
    var screen = slugPart(row && row.screen);
    var theme = slugPart(row && row.theme);
    return (!screen || hay.indexOf(screen) >= 0) && (!theme || hay.indexOf(theme) >= 0);
  }

  function relevantFigmaDesignBullets(lines, row) {
    var figmaLines = lines.filter(isFigmaDesignBullet);
    var matching = figmaLines.filter(function (line) { return designBulletMatchesVisual(line, row); });
    return matching.length ? matching : figmaLines;
  }

  function hasFigmaDesignBullet(lines) {
    return lines.some(isFigmaDesignBullet);
  }

  function reportHashFromBundle(resp) {
    if (resp && resp.bundle && resp.bundle.hash) return resp.bundle.hash;
    var reports = Array.isArray(resp && resp.reports) ? resp.reports : [];
    for (var i = 0; i < reports.length; i++) {
      if (reports[i] && reports[i].name === 'evidence' && reports[i].hash) return reports[i].hash;
    }
    return '';
  }

  function buildVisualFixTaskBody(parentMarkdown, resp, row, stem) {
    var reportHash = resp && resp.visualChecks && resp.visualChecks.reportHash;
    var designBullets = relevantFigmaDesignBullets(extractDesignSectionBullets(parentMarkdown), row);
    var title = visualTitle(row);
    var diff = row && row.artifactSet && row.artifactSet.artifacts && row.artifactSet.artifacts.diff
      ? artifactHref(stem, row.artifactSet.artifacts.diff, reportHash)
      : '';
    var overlay = row && row.artifactSet && row.artifactSet.artifacts && row.artifactSet.artifacts.overlay
      ? artifactHref(stem, row.artifactSet.artifacts.overlay, reportHash)
      : '';
    var body = [
      visualFixMarker(stem, row, reportHash),
      '',
      '## Goal',
      '',
      'Fix the app UI so ' + safeInline(title, 120) + ' matches the Figma reference.',
      '',
      '## What failed',
      '',
      '- Screen: ' + safeInline((row && row.screen) || 'unknown', 120),
      '- Theme: ' + safeInline((row && row.theme) || 'default', 80),
      '- Result: ' + safeInline((row && row.status) || 'UNKNOWN', 40),
      '- What to inspect: Difference and Overlay from the parent Figma check if retained artifacts are still available.',
      diff ? '- Difference: ' + diff : null,
      overlay ? '- Overlay: ' + overlay : null,
      '',
      '## Design',
      '',
      designBullets.join('\n'),
      '',
      '## Technical evidence',
      '',
      '- Parent task: ' + stem,
      '- Evidence source: orchestrator/.cache/figma/reports/evidence-' + stem + '.json',
      '- Screenshot report: orchestrator/.cache/figma/reports/screenshot-' + stem + '.json',
      '- Pipeline run: ' + safeInline(resp && resp.pipelineRunId, 120),
      '- Report hash: ' + safeInline(reportHash, 90),
      '- Visual check: ' + safeInline(title, 120) + ', status ' + safeInline(row && row.status, 40) +
        ', score ' + safeInline(row && row.score, 20) + ', coverage ' + safeInline(row && row.coverage, 20) +
        ', reason ' + safeInline(row && row.reason, 180),
      '- Durable digest: ' + safeInline(title, 120) + ', ' + safeInline(row && row.status, 40) +
        ', score ' + safeInline(row && row.score, 20) + ', coverage ' + safeInline(row && row.coverage, 20) +
        ', reason ' + safeInline(row && row.reason, 180) + ', pipeline run ' + safeInline(resp && resp.pipelineRunId, 120) +
        ', evidence hash ' + safeInline(reportHashFromBundle(resp), 90) +
        ', screenshot hash ' + safeInline(reportHash, 90) + '.',
      '',
      'Do not rely on parent artifact links or cache paths staying available. They are useful inspection hints only unless artifacts are snapshotted durably.',
      '',
      '## Acceptance',
      '',
      '### Automated',
      '',
      '- `orchestrator/figma/scripts/check-screen-cache.mjs` passes after pulling the copied `## Design` screens for this new fix task.',
      '- `orchestrator/figma/scripts/compare-screenshots.mjs` passes for the copied `## Design` screens.',
      '- `orchestrator/figma/scripts/evidence-bundle.mjs` reports no `BLOCKER`, `INCOMPLETE`, or `REVIEW_REQUIRED` rows for the affected screens.',
      '',
      '### Manual',
      '',
      '- Reviewer confirms the app state matches the intended Figma oracle for the affected screen/theme rows.',
      '',
      '## Out of scope',
      '',
      '- Do not change Figma source files.',
      '- Do not relax screenshot thresholds unless evidence proves calibration is the issue.',
      '- Do not rewrite unrelated screens.'
    ].filter(function (line) { return line != null; }).join('\n');
    return { body: body, designBullets: designBullets };
  }

  return {
    buildTaskBody: buildVisualFixTaskBody,
    hasFigmaDesignBullet: hasFigmaDesignBullet,
    hasPullableDesign: bodyHasPullableDesign,
    inlineTitle: function (row, limit) { return safeInline(visualTitle(row), limit); },
    key: visualFixKey,
    pullableBulletCount: pullableBulletCount
  };
}
