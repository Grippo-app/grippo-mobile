export function createFigmaTaskReadModel(dependencies) {
  var getSnapshot = dependencies.getSnapshot;

  // Read on every call: screensCache is replaced by live snapshot updates, so a
  // panel-local cache would make task gates and evidence presentation stale.
  function entry(stem) {
    if (!stem) return null;
    var current = (getSnapshot().screensCache || {})[stem];
    return current || null;
  }

  function needsUnpulledScreens(stem) {
    var current = entry(stem);
    return !!(current && current.needed && !current.pulled);
  }

  function blockingDesignIssues(stem) {
    var current = entry(stem);
    var issues = current && current.designIssues;
    if (!issues || typeof issues !== 'object') return null;
    return issues.captureBlocked === true ? issues : null;
  }

  function evidence(stem) {
    var current = entry(stem);
    return current && current.evidence ? current.evidence : null;
  }

  return {
    entry: entry,
    needsUnpulledScreens: needsUnpulledScreens,
    blockingDesignIssues: blockingDesignIssues,
    evidence: evidence
  };
}
