export function createVisualEvidenceStatus(dependencies) {
  var t = dependencies.t;

  function liteKey(evidence) {
    if (!evidence) return '';
    return [
      evidence.overall || '',
      evidence.stage || '',
      evidence.pipelineRunId || '',
      evidence.generatedAt || '',
      evidence.screenshotReportHash || '',
      evidence.stale ? 'stale' : 'fresh',
      evidence.missingRequiredCount || 0,
      evidence.blockingCount || 0,
      evidence.warningCount || 0,
      evidence.issueCount || 0,
      evidence.artifactCount || 0,
      evidence.screenshotCount || 0
    ].join('\u0001');
  }

  function className(status) {
    var normalized = String(status || '').toUpperCase();
    if (normalized === 'PASS') return 'pass';
    if (normalized === 'WARN' || normalized === 'WARNING' || normalized === 'MINOR' ||
        normalized === 'MAJOR' || normalized === 'SKIPPED') return 'warn';
    if (normalized === 'INCOMPLETE' || normalized === 'REVIEW_REQUIRED') return 'incomplete';
    if (normalized === 'BLOCKER' || normalized === 'FAIL' || normalized === 'ERROR') return 'blocker';
    if (normalized === 'MISSING' || normalized === 'MISSING_ORACLE' ||
        normalized === 'MISSING_CAPTURE') return 'missing';
    // Per-screen capture/compare failures mean the comparison could not finish
    // for that screen. Keep them visibly incomplete while label() still prefers
    // each status's exact localized name over the class fallback.
    if (normalized === 'ASPECT_MISMATCH' || normalized === 'STALE_CAPTURE' ||
        normalized === 'DUPLICATE_CAPTURE' || normalized === 'UNREPRESENTABLE_OVERLAY' ||
        normalized === 'LOW_CONTENT_ORACLE' || normalized === 'CAPTURE_LOCALE_MISMATCH' ||
        normalized === 'NO_INDEXED_SCREENS') return 'incomplete';
    return 'unknown';
  }

  function label(status) {
    var raw = String(status || 'unknown').toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');
    var rawKey = 'board.figmaEvidence.status.' + raw;
    var rawLabel = t(rawKey);
    if (rawLabel !== rawKey) return rawLabel;
    var fallbackClass = className(status);
    var fallbackKey = 'board.figmaEvidence.status.' + fallbackClass;
    var fallbackLabel = t(fallbackKey);
    return fallbackLabel === fallbackKey
      ? t('board.figmaEvidence.status.unknown')
      : fallbackLabel;
  }

  return {
    liteKey: liteKey,
    className: className,
    label: label
  };
}
