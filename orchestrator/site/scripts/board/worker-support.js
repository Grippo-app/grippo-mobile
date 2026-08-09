import { dom } from '../dom.js';

var el = dom.el;

export function createBoardWorkerSupport(dependencies) {
  var t = dependencies.t;
  var pluralLabel = dependencies.pluralLabel;
  var parseIso = dependencies.parseIso;
  var clampNow = dependencies.clampNow;
  var staleLockMs = dependencies.staleLockMs;
  var copyButton = dependencies.copyButton;
  var boardModal = dependencies.boardModal;
  var store = dependencies.store;

  // Heartbeat freshness window — mirrors scripts/status.js. A heartbeat newer
  // than this means the standby /loop is cycling (online); older + no lock
  // means no drainer is attached.
  var WORKER_ONLINE_MS = 90 * 1000;
  // Tolerance for benign clock skew between the worker and this page (they may
  // run on different clocks — VM vs host). A heartbeat dated further than this
  // INTO THE FUTURE is treated as not-fresh, so a worker that stamps a far
  // future time and then dies can't appear "online" forever.
  var WORKER_SKEW_MS = 10 * 1000;

  // POSIX single-quote a string for safe shell paste. Wrap in '...'; any
  // embedded single quote becomes '\'' (close, escaped quote, reopen).
  function shellQuote(s) {
    return "'" + String(s).replace(/'/g, "'\\''") + "'";
  }

  function pendingRequests(storeState) {
    return (storeState && storeState.progress && Array.isArray(storeState.progress.requests))
      ? storeState.progress.requests
      : [];
  }

  // True when the standby /loop is either heart-beating (online) or holding a
  // task lock (busy). Busy counts as attached: a queued request will be picked
  // up when the current run finishes, so we must NOT nag in that case.
  function workerOnlineOrBusy(storeState) {
    var status = storeState && storeState.status;
    if (!status) return false;
    // A held lock counts as a plausible "busy" drainer signal only while it is
    // fresh. An old, unobservable lock (older than staleLockMs — the same
    // threshold the board greys it out at) must not suppress
    // the runner warning in Board status, or queued requests can appear healthy.
    // Real runs are minutes, well under the threshold, so a genuine worker run
    // (whose heartbeat goes stale mid-run) is still covered.
    if (((status.locks && status.locks.count) || 0) > 0) {
      var lockMs = parseIso(status.locks && status.locks.newestStartedAt);
      // Cap at 'now' before the freshness test: an uncapped future-dated lock
      // (NFS / clock skew) would read negative age and falsely count as "busy"
      // forever, suppressing the runner warning in Board status. Same clamp as the card.
      if (lockMs && Date.now() - clampNow(lockMs.getTime()) < staleLockMs) return true;   // busy (fresh lock)
    }
    var d = parseIso(status.worker && status.worker.heartbeatAt);
    if (!d) return false;
    var age = Date.now() - d.getTime();                                   // online,
    return age > -WORKER_SKEW_MS && age < WORKER_ONLINE_MS;               // skew-guarded
  }

  // True when the queue drainer is attached right now: the in-process CLI runner
  // (active, and the CLI logged in so it can actually execute), OR a /loop worker
  // heart-beating within the freshness window. The board uses this to extend
  // patience on an unobservable-but-live run before calling its lock stale.
  // This intentionally excludes the fresh-lock "busy" leg above: another stem's
  // held lock must not make every lock self-justify as live.
  // True when the host CLI cannot authenticate: explicitly logged out, OR
  // signed-in with a dead token (cli.authProblem — expired/revoked keychain
  // credentials; loggedIn still reads true but every session 401s). A missing
  // cli block stays optimistic (unknown ≠ broken), matching the
  // `loggedIn === false` tri-state.
  function cliCannotAuth(cli) {
    return !!cli && (cli.loggedIn === false || !!cli.authProblem);
  }

  function drainerAttached(storeState) {
    if (storeState && storeState.runnerActive &&
        !cliCannotAuth(storeState.cli)) return true;
    var status = storeState && storeState.status;
    var d = parseIso(status && status.worker && status.worker.heartbeatAt);
    if (!d) return false;
    var age = Date.now() - d.getTime();
    return age > -WORKER_SKEW_MS && age < WORKER_ONLINE_MS;
  }

  // Show the runner warning in Board status when requests are queued but no
  // drainer is attached. With the heartbeat this is a positive signal (no
  // recent beat + no lock), so the warning appears immediately.
  // The runnerActive short-circuit is suppressed when the CLI is logged out or
  // its token is dead: in either case the runner cannot actually execute queued
  // requests even though its process is still alive, so Board status must surface the warning.
  function workerLooksOffline(storeState) {
    if (storeState && storeState.startupRecovery && storeState.startupRecovery.status !== 'ready') return false;
    if (storeState && storeState.runnerActive &&
        !cliCannotAuth(storeState.cli)) return false;
    if (!pendingRequests(storeState).length) return false;
    return !workerOnlineOrBusy(storeState);
  }

  // Builds the help-modal body: how to start the standby /loop worker. These are
  // Claude slash commands typed inside a Claude session, not terminal commands.
  function buildWorkerHelpContent(storeState) {
    var reqs = pendingRequests(storeState);
    var box = el('div', { class: 'board-worker-help' });

    box.appendChild(el('h2', { class: 'board-worker-help__title board-modal__title', text: t('board.workerOffline.title') }));
    box.appendChild(el('p', { class: 'board-worker-help__lead', text: pluralLabel('board.workerOffline.waiting', reqs.length) }));
    box.appendChild(el('p', { class: 'board-worker-help__inside', text: t('board.workerOffline.insideClaude') }));

    var projectRoot = (storeState && typeof storeState.projectRoot === 'string')
      ? storeState.projectRoot
      : '';

    // step(labelKey, cmd?) — cmd null renders a text-only step (no code/copy).
    function step(labelKey, cmd) {
      var wrap = el('div', { class: 'board-worker-help__step' });
      wrap.appendChild(el('span', { class: 'board-worker-help__step-label', text: t(labelKey) }));
      if (cmd) {
        var row = el('div', { class: 'board-worker-help__cmd' });
        row.appendChild(el('code', { class: 'board-worker-help__code', text: cmd }));
        row.appendChild(copyButton(t('board.workerOffline.copy'), function () { return cmd; }));
        wrap.appendChild(row);
      }
      return wrap;
    }

    // Step 1 shows the project folder (single-quoted so it survives a paste with
    // spaces/metacharacters). Step 2 is a session toggle (no command). Step 3 is
    // the slash command typed in the Claude chat.
    if (projectRoot) box.appendChild(step('board.workerOffline.step1', shellQuote(projectRoot)));
    box.appendChild(step('board.workerOffline.step2', null));
    box.appendChild(step('board.workerOffline.step3', '/loop /serve-queue'));

    box.appendChild(el('p', { class: 'board-worker-help__note', text: t('board.workerOffline.note') }));

    // Honest alternative to the /loop worker: the in-process runner drains the
    // queue WITHOUT a worker — but only when the Claude CLI is installed, logged
    // in, AND the server was (re)started with it on PATH. Surface which of those
    // isn't met so the user knows the second way out, not just the worker.
    var cli = (storeState && storeState.cli) || {};
    var cliNoteKey = !cli.installed ? 'board.workerOffline.cliNotInstalled'
                   : cli.loggedIn === false ? 'board.workerOffline.cliNotLoggedIn'
                   : cli.authProblem ? 'board.workerOffline.cliAuthDead'
                   : 'board.workerOffline.cliRestart';
    box.appendChild(el('p', { class: 'board-worker-help__note', text: t(cliNoteKey) }));

    var actions = el('div', { class: 'board-worker-help__actions' });
    var done = el('button', { type: 'button', class: 'btn btn--primary', text: t('board.workerOffline.gotit') });
    done.addEventListener('click', boardModal.close);
    actions.appendChild(done);
    box.appendChild(actions);

    return box;
  }

  function openWorkerHelpModal() {
    boardModal.open(buildWorkerHelpContent(store.get()));
  }

  return {
    pendingRequests: pendingRequests,
    workerOnlineOrBusy: workerOnlineOrBusy,
    cliCannotAuth: cliCannotAuth,
    drainerAttached: drainerAttached,
    workerLooksOffline: workerLooksOffline,
    openHelp: openWorkerHelpModal
  };
}
