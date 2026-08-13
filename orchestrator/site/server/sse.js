'use strict';

// ---------------------------------------------------------------------------
// Server-Sent Events: poll deriveState() every POLL_MS, broadcast 'change' to
// all connected clients when its JSON serialization differs. Pure polling is
// way more robust than fs.watch on macOS (recursive flag is flaky for new
// directories that didn't exist at startup). Cost: a few file reads per
// second, negligible.
//
// The poll also advances the Goal-4 activity tracker and runs the Goal-6
// timing reconciler — both only ever ride along with changes that already
// move the state hash, so neither adds SSE churn of its own.
// ---------------------------------------------------------------------------

var state       = require('./state');
var persistence = require('./persistence');
var locksMod    = require('./locks');
var requestsMod = require('./requests');
var timingModel = require('./timing');
var finalizationsMod = require('./finalizations');
var integrationsMod = require('./integrations');
var shallowIntakeMod = require('./shallow-intake');
var editMarkersMod = require('./edit-markers');
var reviewerMod = require('./reviewer');
var tasksLogMod = require('./tasks-log');

var POLL_MS = 1500;
var sseClients = new Set();
var lastStateHash = '';
var lastReviewerStatusRevision = '';
var lastReviewerActivityRevision = '';
var lastIntakeReconcileWarningAt = 0;

// Single-writer serialization for orchestrator/.cache/site/.site-state.json. The setup-form/
// manualStep patch handler (http.js) and the timing reconciler (pollLoop, below)
// each do a read-modify-write on this file; without a shared queue the two RMW
// sequences can interleave across event-loop turns and the later write clobbers
// the earlier (lost update). Both routes funnel through serializeStateWrite so
// their critical sections run strictly one at a time. Lives here (not http.js)
// because http.js already requires this module — the reverse would be a cycle.
var stateWriteQueue = Promise.resolve();
function serializeStateWrite(fn) {
  // Run `fn` after the previous write settles, whatever its outcome, so one
  // failure can't stall the chain. Returns the settled promise for `fn`.
  var run = function () { return fn(); };
  var next = stateWriteQueue.then(run, run);
  stateWriteQueue = next.catch(function () {});
  return next;
}

// An invalid/unreadable INDEX is absence of authority, not an authoritative
// empty board. Preserve both maps byte-for-byte in that state; otherwise the
// normal reconciler would interpret synthetic empty columns as deletion of
// every task and destroy recoverable UI history while the index is being fixed.
function reconcileProgress(persisted, columns, locks, boardStems, locksAvailable, nowIso) {
  if (!columns || !Array.isArray(boardStems) || locksAvailable === false) {
    return {
      timing: { map: persisted.taskTiming || {}, changed: false },
      lifecycle: { map: persisted.taskLifecycle || {}, changed: false }
    };
  }
  return {
    timing: timingModel.reconcile(persisted.taskTiming || {}, locks, columns.done, boardStems),
    lifecycle: timingModel.reconcileLifecycle(persisted.taskLifecycle || {}, columns, locks, nowIso)
  };
}

function broadcast(eventName, payload) {
  var data = JSON.stringify(payload || {});
  var msg = (eventName ? 'event: ' + eventName + '\n' : '') + 'data: ' + data + '\n\n';
  sseClients.forEach(function (res) {
    try { res.write(msg); } catch (e) { sseClients.delete(res); }
  });
}

// The worker rewrites its heartbeat every loop pass (~1/s when idle-online),
// so the precise heartbeatAt would move the hash every tick and fire a 'change'
// SSE every second — making every board re-fetch INDEX.json once a second for
// no reason. Bucket it to 15 s instead: presence transitions (none↔value) and
// crossings still fire promptly so the status pill flips online/offline within
// the client's 90 s window, but a steady stream of beats inside one bucket is
// silent. The full /api/state response still carries the exact heartbeatAt; the
// client derives "last beat X ago" against its own clock.
var HEARTBEAT_BUCKET_MS = 15000;

function heartbeatBucket(s) {
  var at = s && s.status && s.status.worker && s.status.worker.heartbeatAt;
  var ms = at ? Date.parse(at) : NaN;
  return isNaN(ms) ? 'none' : Math.floor(ms / HEARTBEAT_BUCKET_MS);
}

function stateHash(snapshot, reviewerRevision) {
  // Exclude wall-clock fields from the change-detection hash so polling
  // doesn't fire a spurious 'change' SSE on every tick. Of the status block's
  // other timestamps, lastDrainedAt and lock startedAt are strictly event-driven
  // (they only move when their event fires, which already moves another part of
  // the hash) so they need no special handling. lock lastActivityAt is NOT purely
  // event-driven: it is a clock-derived 30s bucket of the run/journal file mtimes
  // (locks.js ACTIVITY_BUCKET_MS), so it can advance mid-run with no lock add/
  // remove — but that advance is deliberately quantized to a 30s cadence (the same
  // churn-control rationale as heartbeatBucket below) and an in-progress run is
  // exactly when a periodic repaint is wanted, so it is left inlined. heartbeatAt
  // is the one HIGH-frequency field (≈1/s); it is bucketed below instead of inlined.
  // Accepts a precomputed deriveState() snapshot so pollLoop derives state once
  // per tick (it also needs boardStems for timing pruning); deletes below mutate
  // it, so callers must not reuse the object after hashing.
  var s = snapshot || state.deriveState();
  if (s && s.progress) delete s.progress.boardLastFetchedAt;
  var hb = heartbeatBucket(s);
  if (s && s.status && s.status.worker) delete s.status.worker.heartbeatAt;
  return JSON.stringify(s)
    + '|tasks:' + state.tasksIndexMtime()
    + '|locks:' + locksMod.locksDirMtime()
    + '|requests:' + requestsMod.requestsDirMtime()
    + '|finalizations:' + finalizationsMod.dirMtime()
    + '|integrations:' + integrationsMod.dirMtime()
    + '|shallow-intake:' + shallowIntakeMod.dirMtime()
    + '|backlog-edits:' + editMarkersMod.dirMtime()
    + '|task-journal:' + tasksLogMod.revision()
    + '|reviewer:' + (reviewerRevision || reviewerMod.revision())
    + '|hb:' + hb;
}

function pollLoop() {
  try {
    // Reconcile source edits, column moves and crashed preview workers before
    // deriving the snapshot, so stale hashes never reach a client tick.
    try { shallowIntakeMod.reconcile(); }
    catch (intakeError) {
      if (Date.now() - lastIntakeReconcileWarningAt > 30000) {
        lastIntakeReconcileWarningAt = Date.now();
        console.warn('[site] advisory shallow-intake reconcile failed; authoritative SSE continues:', intakeError && intakeError.message || intakeError);
      }
    }
    // Advance the activity tracker first so its drain/activity stamps are
    // reflected in this tick's hash (a drain changes the request set too, so
    // this never fires a 'change' on its own — it just keeps stamps fresh).
    var lockRead = locksMod.readLocksResult();
    var indexRead = state.readIndexSnapshot();
    var pollLocks = lockRead.rows;
    var requestRead = requestsMod.scanRequests();
    var pollRequests = requestRead.rows;
    state.activityTracker.update({
      locks: pollLocks,
      locksAvailable: lockRead.available,
      requestsAvailable: requestRead.ok,
      requests: pollRequests
    }, Date.now());
    // Derive the snapshot once: it carries boardStems (for timing pruning) and
    // feeds the change-detection hash below, avoiding a second deriveState().
    var snapshot = state.deriveState({
      lockRead: lockRead,
      indexRead: indexRead,
      requestRead: requestRead
    });
    var boardStems = snapshot.progress && snapshot.progress.boardStems;
    // One exact INDEX generation per tick, shared by the public snapshot and
    // both reconcilers: the duration calc takes done[], while the lifecycle
    // spine takes all four columns.
    var columns = indexRead.columns;
    var nowIso = new Date().toISOString();
    // Goal 6: stamp lock startedAt + compute durations as stems land in done[],
    // and prune entries for stems no longer on the board. Lifecycle: stamp the
    // current column per stem + append on a column change (catches manual git
    // mv). Persist ONLY when a map actually changed — which coincides with a
    // lock/done/column change that already moves the state hash, so this adds no
    // SSE churn of its own. The read-modify-write is routed through the shared
    // single-writer queue so it can't clobber a concurrent setup-form/manualStep
    // patch (http.js). taskLifecycle rides the snapshot stringify (state.js), so
    // a column move fires a 'change' SSE without a new hash term here.
    var persistedProbe = persistence.readPersisted();
    var probe = reconcileProgress(
      persistedProbe,
      columns,
      pollLocks,
      boardStems,
      lockRead.available,
      nowIso
    );
    var probeTiming = probe.timing;
    var probeLife = probe.lifecycle;
    // Overlay the freshly-reconciled maps onto THIS tick's broadcast snapshot
    // BEFORE it is hashed/sent. deriveState() above read the PRE-reconcile
    // persisted maps, so without this the 'change' SSE that moves a card to done
    // would ship the stale taskTiming and the computed duration would only appear
    // one tick (~POLL_MS) later. reconcile()/reconcileLifecycle() always return the
    // full current map (not a delta), so this is the same map the queued write
    // persists. The persist itself stays on the single-writer queue below — this
    // only fixes the read-side latency, it does not write anything.
    if (snapshot.progress) {
      snapshot.progress.taskTiming = probeTiming.map;
      snapshot.progress.taskLifecycle = probeLife.map;
    }
    if (probeTiming.changed || probeLife.changed) {
      serializeStateWrite(function () {
        // Re-read + re-reconcile inside the critical section so a setup write
        // that landed first is carried forward; replace ONLY the maps that
        // actually changed (guard each independently, never the rest of the file).
        var fresh = persistence.readPersisted();
        var current = reconcileProgress(
          fresh,
          columns,
          pollLocks,
          boardStems,
          lockRead.available,
          nowIso
        );
        var timing = current.timing;
        var life = current.lifecycle;
        if (!timing.changed && !life.changed) return;
        if (timing.changed) fresh.taskTiming = timing.map;
        if (life.changed) fresh.taskLifecycle = life.map;
        persistence.writePersisted(fresh);
      });
    }
    var reviewerStatusRevision = reviewerMod.statusRevision();
    var reviewerActivityRevision = reviewerMod.activityRevision();
    var h = stateHash(snapshot, reviewerStatusRevision);
    if (reviewerStatusRevision !== lastReviewerStatusRevision) {
      lastReviewerStatusRevision = reviewerStatusRevision;
      broadcast('reviewer-status', { t: Date.now(), revision: reviewerStatusRevision });
    }
    if (reviewerActivityRevision !== lastReviewerActivityRevision) {
      lastReviewerActivityRevision = reviewerActivityRevision;
      broadcast('reviewer-activity', { t: Date.now(), revision: reviewerActivityRevision });
    }
    if (h !== lastStateHash) {
      lastStateHash = h;
      broadcast('change', { t: Date.now() });
    }
  } catch (e) {
    console.warn('[site] poll failed:', e && e.message);
  }
}

// Keep SSE connections from being killed by intermediaries.
function ssePingLoop() {
  sseClients.forEach(function (res) {
    try { res.write(': ping\n\n'); } catch (e) {}
  });
}

// Seed the hash so the first /api/events client doesn't get a no-op change.
function seedHash() {
  try {
    lastReviewerStatusRevision = reviewerMod.statusRevision();
    lastReviewerActivityRevision = reviewerMod.activityRevision();
    lastStateHash = stateHash(null, lastReviewerStatusRevision);
  } catch (e) {}
}

module.exports = {
  POLL_MS: POLL_MS,
  sseClients: sseClients,
  pollLoop: pollLoop,
  ssePingLoop: ssePingLoop,
  seedHash: seedHash,
  serializeStateWrite: serializeStateWrite,
  reconcileProgress: reconcileProgress,
  broadcast: broadcast
};
