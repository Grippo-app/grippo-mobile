export function createBoardReadinessPolicy(dependencies) {
  var t = dependencies.t;
  var getSnapshot = dependencies.getSnapshot;
  var getFreshIntegrity = dependencies.getFreshIntegrity;

  // This policy controls browser affordances only. The server remains the
  // authority for task-state validation and mutation admission.
  function taskIntegrity() {
    // Prefer the latest dedicated integrity response over the shorter-lived
    // application snapshot used for SSE rendering.
    var fresh = getFreshIntegrity();
    if (fresh && typeof fresh === 'object') return fresh;
    var snapshot = getSnapshot();
    return snapshot && snapshot.taskIntegrity && typeof snapshot.taskIntegrity === 'object'
      ? snapshot.taskIntegrity
      : { ok: true, indexStatus: 'unchecked', findings: [], affectedStems: [] };
  }

  function startupRecoveryState() {
    var snapshot = getSnapshot();
    return snapshot && snapshot.startupRecovery && typeof snapshot.startupRecovery === 'object'
      ? snapshot.startupRecovery
      : { status: 'pending', reasonCode: null, findingCount: 0 };
  }

  function startupRecoveryBlocksMutation() {
    return startupRecoveryState().status !== 'ready';
  }

  function startupRecoveryReason(code) {
    var known = {
      'startup-recovery-unconfigured': 1,
      'startup-recovery-outcome-invalid': 1,
      'startup-recovery-failed': 1,
      'startup-integrity-unavailable': 1,
      'startup-integrity-blocked': 1,
      'task-index-not-fresh': 1,
      'runner-start-failed': 1
    };
    var safe = Object.prototype.hasOwnProperty.call(known, code)
      ? code : 'startup-recovery-failed';
    return t('board.startupRecovery.reason.' + safe);
  }

  function globalMutationBlocked() {
    var snapshot = getSnapshot();
    var publicationIssues = snapshot && snapshot.progress &&
      Array.isArray(snapshot.progress.publicationRecoveryIssues)
      ? snapshot.progress.publicationRecoveryIssues : [];
    // Canonical create/edit recovery proceeds automatically; every other
    // publication issue keeps client-side mutation controls fail-closed.
    return startupRecoveryBlocksMutation() || publicationIssues.some(function (issue) {
      return issue && issue.code !== 'CREATION_INCOMPLETE' && issue.code !== 'EDIT_INCOMPLETE';
    });
  }

  function integrityBlocksStem(stem) {
    var integrity = taskIntegrity();
    if (startupRecoveryBlocksMutation()) return true;
    if (integrity.ok) return false;
    var findings = Array.isArray(integrity.findings) ? integrity.findings : [];
    for (var i = 0; i < findings.length; i++) {
      var item = findings[i];
      if (!item || (item.severity !== 'blocker' && item.severity !== 'error')) continue;
      if (item.stem === stem) return true;
      if (item.details && Array.isArray(item.details.stems) &&
          item.details.stems.indexOf(stem) >= 0) return true;
    }
    return Array.isArray(integrity.affectedStems) && integrity.affectedStems.indexOf(stem) >= 0;
  }

  return {
    taskIntegrity: taskIntegrity,
    startupRecoveryState: startupRecoveryState,
    startupRecoveryBlocksMutation: startupRecoveryBlocksMutation,
    startupRecoveryReason: startupRecoveryReason,
    globalMutationBlocked: globalMutationBlocked,
    integrityBlocksStem: integrityBlocksStem
  };
}
