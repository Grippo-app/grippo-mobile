export function createBoardLoadResults(dependencies) {
  function load(filters) {
    var summaryLoad = dependencies.loadSummary(filters).then(function (summary) {
      return { ok: true, value: summary };
    }, function (error) {
      return { ok: false, error: error || { kind: 'fetch-failed', detail: 'unknown' } };
    });
    var integrityLoad = dependencies.loadIntegrity().then(function (integrity) {
      return { ok: true, value: integrity };
    }, function (error) {
      return { ok: false, error: error };
    });
    return Promise.all([summaryLoad, integrityLoad]);
  }

  return { load: load };
}
