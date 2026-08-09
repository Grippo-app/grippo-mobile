'use strict';

// ---------------------------------------------------------------------------
// Live-status model (Goal 4 — honest live status). Dependency-free CommonJS
// so server.js can require it and the later server split (Goal 7) can move
// this file unchanged.
//
// Two pieces:
//
//   createActivityTracker()
//     In-memory observer. server.js calls tracker.update({locks, requests},
//     Date.now()) once per SSE poll. It diffs the request-id set and the lock
//     set across polls to stamp two wall-clock-FREE signals:
//       lastDrainedAt  — a queued request id disappeared (the /loop claimed +
//                        deleted it). Null until first observed; null again on
//                        server restart (in-memory only).
//       lastActivityAt — the lock OR request set changed at all (add/remove).
//     The FIRST update only records a baseline: locks/requests already present
//     at boot are NOT reported as "just changed" — we can't know when they
//     started moving, so we never fake recent activity.
//
//   computeStatus({locks, locksAvailable, locksErrorCode, requests,
//                  requestsAvailable, requestsErrorCode, lastDrainedAt,
//                  lastActivityAt})
//     Pure presenter. Emits ONLY absolute timestamps + counts. It deliberately
//     emits no relative times and no inferred activity label, because those
//     depend on the current wall clock and would change the SSE change-hash on
//     every 1.5s poll. The client (scripts/status.js) derives "X ago" and the
//     ACTIVE / IN-PROGRESS / IDLE / UNKNOWN label from these timestamps.
// ---------------------------------------------------------------------------

function lockSetKey(locks) {
  // readLocksResult().rows is already sorted by stem, so a plain join is a
  // stable identity for the set — it changes when a lock is added, removed, or
  // its stage/startedAt changes. lastActivityAt is folded in too so the change
  // signal (hence activity.lastActivityAt) keeps advancing during a long
  // no-churn run instead of freezing at lock-creation — the board card already
  // ages from this same per-lock stamp (locks.lastActivityIso). It is
  // 30s-bucketed upstream (locks.js ACTIVITY_BUCKET_MS), so including it adds no
  // SSE churn beyond that existing 30s cadence.
  var parts = [];
  for (var i = 0; i < locks.length; i++) {
    var l = locks[i] || {};
    parts.push(l.stem + '\x00' + l.stage + '\x00' + (l.startedAt || '') + '\x00' + (l.lastActivityAt || ''));
  }
  return parts.join('\x01');
}

function requestIdSet(requests) {
  var set = Object.create(null);
  for (var i = 0; i < requests.length; i++) {
    var id = requests[i] && requests[i].id;
    if (id) set[id] = true;
  }
  return set;
}

function createActivityTracker() {
  var initialized = false;
  var prevReqIds = Object.create(null);
  var prevRequestsKnown = false;
  var prevLockKey = '';
  var prevLocksKnown = false;
  var lastDrainedAt = null;   // ms epoch | null
  var lastActivityAt = null;  // ms epoch | null

  return {
    update: function (snapshot, nowMs) {
      var locks = (snapshot && snapshot.locks) || [];
      var requests = (snapshot && snapshot.requests) || [];
      var locksKnown = !!snapshot && snapshot.locksAvailable === true;
      var requestsKnown = !!snapshot && snapshot.requestsAvailable === true;
      var curReqIds = requestsKnown ? requestIdSet(requests) : prevReqIds;
      var curLockKey = locksKnown ? lockSetKey(locks) : prevLockKey;

      if (!initialized) {
        initialized = true;
        if (requestsKnown) prevReqIds = curReqIds;
        prevRequestsKnown = requestsKnown;
        if (locksKnown) prevLockKey = curLockKey;
        prevLocksKnown = locksKnown;
        return;
      }

      var drained = false; // an id we saw before is gone now
      var added = false;   // a new id appeared
      var k;
      if (requestsKnown && prevRequestsKnown) {
        for (k in prevReqIds) { if (!curReqIds[k]) { drained = true; break; } }
        for (k in curReqIds) { if (!prevReqIds[k]) { added = true; break; } }
      }
      // An observation gap is not a lock add/remove event. After lock-state
      // availability recovers, establish a new baseline before reporting any
      // later change.
      var lockChanged = locksKnown && prevLocksKnown && curLockKey !== prevLockKey;

      if (drained) lastDrainedAt = nowMs;
      if (drained || added || lockChanged) lastActivityAt = nowMs;

      if (requestsKnown) prevReqIds = curReqIds;
      prevRequestsKnown = requestsKnown;
      if (locksKnown) prevLockKey = curLockKey;
      prevLocksKnown = locksKnown;
    },
    snapshot: function () {
      return { lastDrainedAt: lastDrainedAt, lastActivityAt: lastActivityAt };
    }
  };
}

function isoOrNull(ms) {
  if (typeof ms !== 'number' || !isFinite(ms)) return null;
  try { return new Date(ms).toISOString(); } catch (e) { return null; }
}

// Cap an epoch-ms at 'now'. The browser worker-support freshness check uses the
// same skew defense. lastActivityAt is already capped upstream in locks.js;
// this is the matching cap for the startedAt leg here.
function clampNow(ms) { return Math.min(ms, Date.now()); }

function pickIso(list, field, newest) {
  var best = null;
  var bestT = newest ? -Infinity : Infinity;
  for (var i = 0; i < list.length; i++) {
    var v = list[i] && list[i][field];
    var ms = v ? Date.parse(v) : NaN;
    if (isNaN(ms)) continue;
    if (newest ? ms > bestT : ms < bestT) { bestT = ms; best = v; }
  }
  return best;
}

// Oldest "effective activity" across locks, as an ISO string — the lock that has
// been quiet the LONGEST. Per lock, effective activity = the newer of its
// startedAt and its lastActivityAt (a lock with no activity file falls back to
// startedAt). The header's stale warning ages from this instead of the raw
// start time, which would falsely flag a long-but-live run as stale.
function oldestActivityIso(locks) {
  var bestT = Infinity;
  for (var i = 0; i < locks.length; i++) {
    var eff = effectiveActivityMs(locks[i]);
    if (isFinite(eff) && eff < bestT) bestT = eff;
  }
  return isFinite(bestT) ? new Date(bestT).toISOString() : null;
}

// Per-lock "effective activity" instant (ms): the newer of its startedAt and its
// lastActivityAt, with startedAt capped at 'now' (the matching cap for the
// board's startMs clamp) so a future-dated lock start — NFS / clock skew —
// can't push effective activity forward and suppress staleness signals.
function effectiveActivityMs(l) {
  l = l || {};
  var sTraw = l.startedAt ? Date.parse(l.startedAt) : NaN;
  var sT = isNaN(sTraw) ? NaN : clampNow(sTraw);
  var aT = l.lastActivityAt ? Date.parse(l.lastActivityAt) : NaN;
  return Math.max(isNaN(sT) ? -Infinity : sT, isNaN(aT) ? -Infinity : aT);
}

// Newest "effective activity" across locks — the MOST recently active lock. The
// header's worker pill reads 'busy' from this with a freshness window (a lock
// whose run died hours ago must not pin the pill on busy forever); the oldest
// leg above keeps driving the stale warning.
function newestActivityIso(locks) {
  var bestT = -Infinity;
  for (var i = 0; i < locks.length; i++) {
    var eff = effectiveActivityMs(locks[i]);
    if (isFinite(eff) && eff > bestT) bestT = eff;
  }
  return isFinite(bestT) ? new Date(bestT).toISOString() : null;
}

function publicRequestScanCode(value) {
  switch (value) {
    case 'request-directory-unsafe':
    case 'request-directory-entry-limit':
    case 'request-directory-changed':
    case 'request-directory-identity-changed':
    case 'request-name-invalid':
    case 'request-record-limit':
    case 'request-record-unsafe':
    case 'request-record-invalid':
    case 'request-byte-limit':
      return value;
    default:
      return 'request-scan-unavailable';
  }
}

function computeStatus(input) {
  input = input || {};
  var locks = input.locks || [];
  var requests = input.requests || [];
  var heartbeat = input.heartbeat || null;
  return {
    queue: {
      available: input.requestsAvailable === true,
      errorCode: input.requestsAvailable !== true
        ? publicRequestScanCode(input.requestsErrorCode)
        : null,
      pending: requests.length,
      lastDrainedAt: isoOrNull(input.lastDrainedAt)
    },
    locks: {
      available: input.locksAvailable === true,
      errorCode: input.locksAvailable !== true &&
        (input.locksErrorCode === 'runtime-locks-entry-limit' ||
         input.locksErrorCode === 'runtime-locks-unavailable')
        ? input.locksErrorCode
        : (input.locksAvailable === true ? null : 'runtime-locks-unavailable'),
      count: locks.length,
      newestStartedAt: pickIso(locks, 'startedAt', true),
      oldestStartedAt: pickIso(locks, 'startedAt', false),
      oldestActivityAt: oldestActivityIso(locks),
      newestActivityAt: newestActivityIso(locks)
    },
    activity: {
      lastActivityAt: isoOrNull(input.lastActivityAt)
    },
    // Worker liveness. Absolute heartbeat timestamp only (second-granular, set
    // by the standby /loop). The client turns this into online / busy / offline
    // against its own clock — see scripts/status.js. Null when no drainer has
    // ever written a heartbeat for this project.
    worker: {
      heartbeatAt: heartbeat && typeof heartbeat.at === 'string' ? heartbeat.at : null
    }
  };
}

module.exports = {
  createActivityTracker: createActivityTracker,
  computeStatus: computeStatus
};
