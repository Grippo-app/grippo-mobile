var SHA256_REPORT_RE = /^sha256:[0-9a-f]{64}$/i;

export function isValidReportHash(hash) {
  return typeof hash === 'string' && SHA256_REPORT_RE.test(hash);
}

export function artifactSetReportHashOk(artifactSet, reportHash) {
  if (!artifactSet) return false;
  if (!isValidReportHash(reportHash)) return false;
  return !artifactSet.reportHash || artifactSet.reportHash === reportHash;
}

export function reportRunIds(resp) {
  var ids = [];
  var seen = Object.create(null);
  var reports = Array.isArray(resp && resp.reports) ? resp.reports : [];
  for (var i = 0; i < reports.length; i++) {
    var id = reports[i] && reports[i].pipelineRunId;
    if (id && !seen[id]) { seen[id] = true; ids.push(id); }
  }
  if (!ids.length && Array.isArray(resp && resp.runIds)) {
    for (var r = 0; r < resp.runIds.length; r++) {
      var rid = resp.runIds[r];
      if (rid && !seen[rid]) { seen[rid] = true; ids.push(rid); }
    }
  }
  return ids;
}

export function evidenceTrustState(resp, opts) {
  opts = opts || {};
  if (!resp || resp.present === false) return { usable: false, reason: 'missing' };
  if (resp.schemaVersion !== 1) return { usable: false, reason: 'bad-schema' };
  var kind = String(resp.kind || '').toLowerCase();
  if (kind && kind.indexOf('figma') < 0 && kind.indexOf('evidence') < 0) {
    return { usable: false, reason: 'unsupported' };
  }
  if (opts.requiredStage && resp.stage !== opts.requiredStage) {
    return { usable: false, reason: 'wrong-stage' };
  }
  if (resp.stale) return { usable: false, reason: 'stale' };
  if (Array.isArray(resp.hashDriftReports) && resp.hashDriftReports.length) {
    return { usable: false, reason: 'hash-drift' };
  }
  if (Array.isArray(resp.missingRequiredReports) && resp.missingRequiredReports.length && !opts.allowMissingRequired) {
    return { usable: false, reason: 'missing-required' };
  }
  var reports = Array.isArray(resp.reports) ? resp.reports : [];
  for (var i = 0; i < reports.length; i++) {
    if (reports[i] && reports[i].unreadable) return { usable: false, reason: 'unreadable-report' };
  }
  // Defense-in-depth: the server already marks any multi-run summary `stale`
  // (figma-evidence.js readEvidence: stale = runIds.length > 1 || drift), so the
  // stale check above normally returns first. Keep an independent client guard
  // in case that server contract ever changes.
  var ids = reportRunIds(resp);
  if (ids.length > 1) return { usable: false, reason: 'mixed-runs' };
  if (opts.visual) {
    var reportHash = resp && resp.visualChecks && resp.visualChecks.reportHash;
    if (!isValidReportHash(reportHash)) return { usable: false, reason: 'bad-report-hash' };
  }
  return { usable: true, reason: 'ok' };
}

export function finalVisualTrustState(resp) {
  return evidenceTrustState(resp, { visual: true, requiredStage: 'final' });
}

// Missing-required is a completeness/verdict issue, not an integrity issue.
// Done comparisons may display the genuine artifacts that are present, while
// every stage, freshness, run and hash guard remains active.
export function finalVisualDisplayState(resp) {
  return evidenceTrustState(resp, {
    visual: true,
    requiredStage: 'final',
    allowMissingRequired: true
  });
}
