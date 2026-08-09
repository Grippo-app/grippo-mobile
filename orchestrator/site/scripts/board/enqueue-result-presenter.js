export function createEnqueueResultPresenter(dependencies) {
  var t = dependencies.t;
  var getSnapshot = dependencies.getSnapshot;
  var workerOnlineOrBusy = dependencies.workerOnlineOrBusy;
  var cliCannotAuth = dependencies.cliCannotAuth;
  var toast = dependencies.toast;

  function present(response, stem, onRunning) {
    // Typed action responses may carry the snapshot that admitted the request;
    // otherwise use the latest live snapshot available to the caller.
    var snapshot = (response && response.state) || getSnapshot();
    if (snapshot.runnerActive || workerOnlineOrBusy(snapshot)) {
      var cli = snapshot.cli || {};
      var runnerOnly = snapshot.runnerActive && !workerOnlineOrBusy(snapshot);
      // Host CLI authentication matters only when the in-process runner is the
      // sole drainer; an external worker does not depend on that credential.
      if (runnerOnly && cli.installed && cliCannotAuth(cli)) {
        toast(t(cli.authProblem ? 'board.run.queuedAuthDead' : 'board.run.queuedNoAuth'));
      } else {
        var replayed = !!(response && (
          response.deduped || response.idempotentReplay || response.status === 'already-active'
        ));
        toast(t(replayed ? 'board.run.queuedExists' : 'board.run.queued'));
      }
      var session = stem && snapshot.sessions && snapshot.sessions['task:' + stem];
      if (session && session.running && onRunning) onRunning();
    } else {
      toast(t('board.run.queuedNoWorker'));
    }
  }

  return { present: present };
}
