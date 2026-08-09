var REFRESH_DEBOUNCE_MS = 150;
var TICK_MS = 30000;

// Owns presentation-only refresh timers. Canonical reads and reload effects
// remain injected by Board; this module only decides when they are needed.
export function createBoardRefreshClock(dependencies) {
  var refreshTimer = null;
  var tickInterval = null;

  function scheduleRefresh() {
    if (refreshTimer != null) dependencies.cancelTimeout(refreshTimer);
    refreshTimer = dependencies.scheduleTimeout(function () {
      refreshTimer = null;
      if (!dependencies.isMounted()) return;
      if (!dependencies.isBoardActive()) return;
      dependencies.reloadBoard({ closeOpenModal: false });
    }, REFRESH_DEBOUNCE_MS);
  }

  function tick() {
    if (!dependencies.isMounted()) return;
    if (!dependencies.isBoardActive()) return;
    var snapshot = dependencies.getSnapshot();
    var inProgress = snapshot && snapshot.progress &&
      Array.isArray(snapshot.progress.inProgress)
      ? snapshot.progress.inProgress
      : [];
    if (inProgress.length === 0 && dependencies.pendingRequests(snapshot).length === 0) return;
    dependencies.render();
  }

  function startClock() {
    if (tickInterval != null) return;
    tickInterval = dependencies.scheduleInterval(tick, TICK_MS);
  }

  return {
    scheduleRefresh: scheduleRefresh,
    startClock: startClock,
    tick: tick
  };
}
