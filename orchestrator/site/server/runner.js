'use strict';

// ---------------------------------------------------------------------------
// CLI runner — the queue drainer. The host-side counterpart to the standby
// /loop worker: instead of a human keeping a Claude session open, the server
// drains queued requests into INTERACTIVE sessions (server/sessions.js), up to
// MAX_PARALLEL at a time, each in its own task worktree — see below. Each task gets the
// same answerable terminal as the wizard, so the user can watch it and reply
// if Claude asks mid-run.
//
// Flow (the queue itself is unchanged):
//   Typed task action → server prompt builder → orchestrator/.cache/tasks/requests/<id>.json
//   runner.tick() (polled, and called inline on enqueue for instant start):
//     atomically CLAIM the oldest request(s) into a hidden sibling
//     requests/.<id>.claim (no-clobber hard-link + source unlink, so every
//     operation stays pinned to one directory inode and cannot double-run),
//     bind that exact request generation to a provisioned task worktree,
//     then sessions.start("task:<stem>")
//     with the prompt as turn 1. The spawned prompt writes its own
//     .cache/tasks/locks/<stem>.json, so task-summary can project the active run,
//     and the live transcript lands in .cache/tasks/runs/<safeKey>.events.jsonl.
//
// Safety (preserved):
//   - Self-disables if `claude` is not on PATH (→ /loop worker fallback).
//   - Publishes runner liveness and claims work only while the cached CLI
//     contract proves a logged-in, non-revoked session. A present-but-logged-
//     out CLI therefore leaves the durable request for a standby worker or a
//     later successful login instead of consuming it into an auth failure.
//   - Atomic no-clobber claim → safe even if a /loop worker also drains.
//   - projectRoot guard → never runs a request stamped for another project.
//   - MAX_PARALLEL cap (via sessions.taskRunningCount()) → bounded concurrency.
//   - Durable occupancy hold: a board-task writer lease that is ACTIVE but not
//     owned by this live process (an orphan child from an unclean site death,
//     a standby /serve-queue execution, another site process) pauses the drain
//     until it clears — the in-memory cap cannot see those writers.
//     finalizations.beginMutation enforces the same exclusion authoritatively
//     at lease-acquire time; this check only avoids claim→refuse→requeue churn.
//   - Marker ownership: an exact version-1 `.runner-alive` owned by a DIFFERENT live
//     process means another runner drains this root — this one stands down
//     instead of stealing the marker. While our own task children are alive
//     the marker is kept fresh even through a CLI auth flip, so the standby
//     never treats a mid-run runner as absent.
//   - Per-stem dedup → a second click while a stem's session runs is dropped.
//   - Run-gate: while sessions.runGateError(stem) reports the screenshot-gate
//     net unwired for a proven visual task, that queued `run` is left unclaimed.
//     Non-visual tasks keep draining. sessions.start()/send() enforce the same
//     task-scoped gate authoritatively; this skip only avoids a 2s
//     claim→refuse→requeue bounce. Env knobs: RUNNER_DISABLED=1,
//     FIGMA_WIRING_GATE=0 (deliberate gate opt-out for self-managed hooks).
// Children are owned by sessions.js (it spawns, tracks, and kills them).
// ---------------------------------------------------------------------------

var fs    = require('fs');
var path  = require('path');
var cp    = require('child_process');
var TextDecoder = require('util').TextDecoder;
var paths = require('./paths');
var sessions = require('./sessions');
var cli = require('./cli');
var finalizations = require('./finalizations');
var requestsMod = require('./requests');
var locksMod = require('./locks');
var fileGuards = require('./file-guards');
var taskIntegrity = require('./task-integrity');
var shallowIntake = require('./shallow-intake');
var childEnv = require('./child-env').childEnv;
var writerLeases = require('../../tasks/writer-leases.cjs');
var worktreeManager = require('./worktree-manager');

var REQUESTS_DIR = paths.REQUESTS_DIR;
var RUNS_DIR     = paths.RUNS_DIR;
var LOCKS_DIR    = paths.LOCKS_DIR;
var PROJECT_ROOT = paths.PROJECT_ROOT;

// <epochMs>-<rand>, same shape the server generates for request ids.
var ID_RE = /^[0-9]+-[a-z0-9]+$/;
var CLAIM_FILE_MAX_BYTES = 256 * 1024;
var RUNTIME_DIRECTORY_ENTRIES_MAX = 10000;
var RUNTIME_SIDECARS_MAX = 1000;
var RUNTIME_SIDECARS_TOTAL_MAX_BYTES = 8 * 1024 * 1024;
var SESSION_SIDECAR_MAX_BYTES = 64 * 1024;
// REQUEST_PROMPT_MAX is in UTF-16 code units. One unit can encode at most
// three UTF-8 bytes (a supplementary scalar consumes two units), so this is
// the exact byte ceiling corresponding to the existing character contract.
var CLAIM_PROMPT_MAX_BYTES = requestsMod.REQUEST_PROMPT_MAX_BYTES;

// Frozen serial safety. All task sessions share ONE working tree (cwd =
// PROJECT_ROOT), so a second concurrent agent can compile a neighbour task's
// bytes, pick up its files through an aggregate check, and attribute the
// result to the wrong task. Per-task git-worktree isolation (pipeline
// improvement 01, Phases 1-5) removed that failure mode: every run owns a
// separate checkout, seals its own candidate, and publishes only through the
// serialized integration transaction. The cap is therefore raised to the
// plan's CANARY value of 2 — still a source constant with no environment
// override, because a knob that could silently raise it would defeat the
// guarantee. Raising it further (the plan's eventual default of 4) is an
// explicit owner decision after the canary. Mirrors the cap row in
// orchestrator/contracts/orchestrator-loop.md.
var MAX_PARALLEL = 2;
if (process.env.RUNNER_MAX_PARALLEL !== undefined) {
  console.warn('[runner] RUNNER_MAX_PARALLEL is ignored: concurrency is ' +
    'a source constant, currently the canary value 2.');
}

var POLL_MS = 2000;

var enabled = false;     // set by init() after the `claude` presence check
var started = false;     // guard against double init

// Liveness marker so a standby /loop worker can detect an active runner and
// stand down (avoids double-execution if both drain the same queue).
var RUNNER_MARKER = path.join(RUNS_DIR, '.runner-alive');
// Recovery grace for an orphaned private claim. Age never grants recovery
// authority: cleanup runs only after this process has published its exact
// runner marker, and a proven live foreign marker returns before cleanup. The
// delay merely avoids immediate churn after a proven owner-generation handoff.
var STALE_CLAIM_MS = 5 * 60 * 1000;
var RETAIN_RUNS = 300;                 // cap on kept session transcript pairs
var tickN = 0;
var gateLogged = false;                // run-gate log deduper (log on transition only)
var queueScanWarningCode = null;        // unavailable scan log deduper
var runnerMarkerFd = null;
var authReadyLogged = null;
var occupancyLoggedCode = null;         // durable-occupancy hold log deduper
var provisionHoldLogged = null;         // worktree-provisioning hold log deduper
var foreignRunnerLogged = null;         // foreign-runner stand-down log deduper
// Start identity of THIS process, stamped into the marker so a later runner
// can distinguish "that pid is still the runner that wrote this" from PID
// reuse. null on platforms without a start-generation primitive.
var RUNNER_PROCESS_START_ID = null;
try { RUNNER_PROCESS_START_ID = writerLeases.captureProcessStartId(process.pid); }
catch (startIdError) { RUNNER_PROCESS_START_ID = null; }

function authReady() {
  var status;
  try { status = cli.status(); } catch (error) { return false; }
  return !!(status && status.installed === true && status.loggedIn === true &&
    !status.authProblem);
}

function withdrawMarker() {
  var ownedMarker = null;
  if (runnerMarkerFd !== null) {
    try { ownedMarker = fs.fstatSync(runnerMarkerFd, { bigint: true }); } catch (statError) {}
    try { fs.closeSync(runnerMarkerFd); } catch (closeError) {}
    runnerMarkerFd = null;
  }
  if (!ownedMarker) return true;
  return fileGuards.unlinkRegularFileIfUnder(
    PROJECT_ROOT, RUNS_DIR, RUNNER_MARKER, 4096,
    function (bounded) {
      return bounded.stat.dev === String(ownedMarker.dev) &&
        bounded.stat.ino === String(ownedMarker.ino);
    }
  );
}

// Foreign-runner probe: only the exact current marker schema plus a proven
// pid/start-generation identity can establish a live owner. Unknown or
// malformed bytes never age into authority to take over; explicit operator
// removal is required when ownership cannot be proven dead.
function exactUtc(value) {
  return typeof value === 'string' &&
    /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(value) &&
    Number.isFinite(Date.parse(value)) && new Date(value).toISOString() === value;
}
function conformingRunnerMarker(value) {
  return value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join(',') === 'at,pid,processStartId,projectRoot,version' &&
    value.version === 1 && exactUtc(value.at) && value.projectRoot === PROJECT_ROOT &&
    Number.isSafeInteger(value.pid) && value.pid > 0 &&
    typeof value.processStartId === 'string' &&
    writerLeases.PROCESS_START_ID_RE.test(value.processStartId);
}
function foreignRunnerOwner() {
  var entry = fileGuards.inspectEntryUnder(PROJECT_ROOT, RUNS_DIR, RUNNER_MARKER);
  if (!entry || entry.status === 'missing') return null;
  if (entry.status !== 'present') return { pid: null, state: 'structural-unsafe' };
  var boundedRead = fileGuards.boundedRegularFileUnder(PROJECT_ROOT, RUNS_DIR, RUNNER_MARKER, 4096);
  if (!boundedRead || boundedRead.stat.nlink !== '1' ||
      (process.platform !== 'win32' && (boundedRead.stat.mode & 0o777) !== 0o600)) {
    return { pid: null, state: 'structural-unsafe' };
  }
  var value = null;
  try { value = JSON.parse(boundedRead.bytes.toString('utf8')); } catch (parseError) {}
  if (!conformingRunnerMarker(value)) return { pid: null, state: 'nonconforming' };
  if (value.pid === process.pid && value.processStartId === RUNNER_PROCESS_START_ID) return null;
  var state = writerLeases.processIdentityState(value.pid, value.processStartId);
  // Exact identity proof (pid + start id both match a live process): a real
  // runner owns the marker — never steal it, whatever the file age.
  if (state === 'match') return { pid: value.pid, state: state };
  if (state === 'dead' || state === 'reused') return null;
  return { pid: value.pid, state: state };
}

function touchMarker() {
  var bytes = Buffer.from(JSON.stringify({
    version: 1, at: new Date().toISOString(), pid: process.pid,
    processStartId: RUNNER_PROCESS_START_ID, projectRoot: PROJECT_ROOT
  }) + '\n', 'utf8');
  if (runnerMarkerFd === null) {
    runnerMarkerFd = fileGuards.openAtomicReplaceRegularFile(
      PROJECT_ROOT, RUNS_DIR, RUNNER_MARKER,
      { create: true, mode: 0o600, initialBytes: bytes, maxBytes: 4096 }
    );
    return runnerMarkerFd !== null;
  }
  try {
    var pathStat = fileGuards.statRegularFileUnder(PROJECT_ROOT, RUNS_DIR, RUNNER_MARKER);
    var fdStat = fs.fstatSync(runnerMarkerFd, { bigint: true });
    if (!pathStat || pathStat.dev !== String(fdStat.dev) || pathStat.ino !== String(fdStat.ino)) {
      throw new Error('marker identity changed');
    }
    fs.ftruncateSync(runnerMarkerFd, 0);
    var offset = 0;
    while (offset < bytes.length) {
      var written = fs.writeSync(runnerMarkerFd, bytes, offset, bytes.length - offset, offset);
      if (!written) throw new Error('marker write made no progress');
      offset += written;
    }
    fs.fsyncSync(runnerMarkerFd);
    var afterStat = fileGuards.statRegularFileUnder(PROJECT_ROOT, RUNS_DIR, RUNNER_MARKER);
    if (!afterStat || afterStat.dev !== String(fdStat.dev) || afterStat.ino !== String(fdStat.ino)) {
      throw new Error('marker public identity changed during refresh');
    }
    return true;
  } catch (error) {
    try { fs.closeSync(runnerMarkerFd); } catch (closeError) {}
    runnerMarkerFd = null;
    return false;
  }
}

function probeClaudeOnPath(done) {
  var child;
  try {
    child = cp.spawn('claude', ['--version'], { stdio: 'ignore', env: childEnv() });
  } catch (e) {
    done(false); return;
  }
  var finished = false;
  var timer = setTimeout(function () {
    if (finished) return;
    // Decide now — a probe child that ignores SIGTERM must not leave the runner
    // undecided (init() would never log enabled/disabled and the queue would sit
    // silently). SIGKILL sweeps the stray child after the decision.
    finished = true;
    try { child.kill('SIGTERM'); } catch (e) {}
    setTimeout(function () { try { child.kill('SIGKILL'); } catch (e) {} }, 2000);
    done(false);
  }, 8000);
  child.on('error', function () {
    if (finished) return; finished = true; clearTimeout(timer); done(false);
  });
  child.on('close', function (code) {
    if (finished) return; finished = true; clearTimeout(timer); done(code === 0);
  });
}

function ensureRunsDir() {
  return !!fileGuards.realDirectoryUnder(PROJECT_ROOT, RUNS_DIR, { create: true, mode: 0o700 });
}

// Keep the private site claim in the SAME verified directory as the visible
// request. Node has no openat/linkat API, so a cross-directory hard-link cannot
// be made race-free against ancestor replacement. A hidden sibling preserves
// atomic no-clobber ownership while every link/unlink stays cwd-anchored to one
// checked directory inode.
function claimPathFor(id) { return path.join(REQUESTS_DIR, '.' + id + '.claim'); }

// The id embedded in a `.<id>.claim` filename, or null if it isn't a claim file
// of the expected shape. Used by the sweep to restore an orphaned claim.
function claimIdFromName(name) {
  if (name.charAt(0) !== '.' || !/\.claim$/.test(name)) return null;
  var id = name.slice(1, -'.claim'.length);
  return ID_RE.test(id) ? id : null;
}

function boundedRuntimeNames(directory) {
  var listed = fileGuards.boundedDirectoryNamesUnder(
    PROJECT_ROOT, directory, RUNTIME_DIRECTORY_ENTRIES_MAX
  );
  return listed.ok ? listed : { ok: false, code: listed.code || 'directory-read-failed', names: [] };
}

// Exact execution contract for bytes that have already won the atomic claim.
// No field is coerced: a hand-written or malformed record either matches the current
// server writer exactly or is consumed without executing its prompt.
function claimedRequestIssue(req, expectedProjectRoot) {
  return requestsMod.requestRecordIssue(req, expectedProjectRoot);
}

function discardClaim(id, generation) {
  if (!generation) return false;
  var removed = fileGuards.unlinkRegularFileMatchingResultUnder(
    PROJECT_ROOT, REQUESTS_DIR, claimPathFor(id), CLAIM_FILE_MAX_BYTES,
    { proof: generation.proof, bytes: generation.bytes }
  );
  return !!(removed && removed.ok);
}

// Put a claimed request back into .cache/tasks/requests/ so the next tick
// re-claims and runs it (used by the per-stem dedup and stale-claim sweep).
// A failed transfer deliberately retains the private exact claim generation;
// no cleanup path may unlink it without the same proof.
function requeueClaim(id, generation) {
  if (!generation) return false;
  return fileGuards.transferFileNoClobberSameDirectoryUnder(
    PROJECT_ROOT, REQUESTS_DIR, claimPathFor(id), path.join(REQUESTS_DIR, id + '.json'),
    { proof: generation.proof }
  );
}

// Atomically claim a request out of .cache/tasks/requests/ into a private claim
// file. Returns the parsed request object, or null if it could not be claimed.
function claim(id) {
  var src = path.join(REQUESTS_DIR, id + '.json');
  var claimFile = claimPathFor(id);
  // rename() may replace a stale destination on POSIX. Hard-link publication
  // gives claim and requeue the same no-clobber ownership transfer used by the
  // standby private-claim protocol.
  if (!fileGuards.transferFileNoClobberSameDirectoryUnder(
    PROJECT_ROOT, REQUESTS_DIR, src, claimFile
  )) return null;
  var obj;
  try {
    var bounded = fileGuards.boundedRegularFileUnder(
      PROJECT_ROOT, REQUESTS_DIR, claimFile, CLAIM_FILE_MAX_BYTES
    );
    if (!bounded || bounded.stat.nlink !== '1') throw new Error('claim is unsafe, multiply linked, unstable, or oversized');
    var text = new TextDecoder('utf-8', { fatal: true }).decode(bounded.bytes);
    obj = JSON.parse(text);
  }
  catch (e) {
    // Claimed but unparseable bytes may have owned a valid reservation before
    // corruption. Their stem/fingerprint can no longer authenticate which
    // receipt to withdraw, so retain the hidden claim as recovery evidence.
    // Hidden claims are absent from scanRequests(), hence this cannot hot-loop.
    console.warn('[runner] retained malformed private claim for explicit recovery:', id, '(' + (e && e.message || e) + ')');
    return null;
  }
  return {
    record: obj,
    generation: { proof: bounded.stat, bytes: bounded.bytes }
  };
}

// Set of stems with a lock file present under .cache/tasks/locks/ right now. cleanup() must
// not prune a sidecar whose stem still holds a lock: the dead-run recovery modal
// reads that transcript for the stop reason / last message, and losing it would
// degrade the recovery badge (stopped → stalled). Read once per cleanup pass.
function lockedStems() {
  var set = Object.create(null);
  var listed = boundedRuntimeNames(LOCKS_DIR);
  if (!listed.ok) return null;
  var names = listed.names;
  for (var i = 0; i < names.length; i++) {
    if (!names[i].endsWith('.json')) continue;
    set[names[i].slice(0, -'.json'.length)] = true;
  }
  return set;
}

function cleanupClaims() {
  var listed = boundedRuntimeNames(REQUESTS_DIR);
  if (!listed.ok) return;
  var names = listed.names;
  var now = Date.now();
  for (var i = 0; i < names.length; i++) {
    var nm = names[i];
    if (/\.claim$/.test(nm)) {
      try {
        var staleFile = path.join(REQUESTS_DIR, nm);
        var claimStat = fileGuards.statRegularFileUnder(PROJECT_ROOT, REQUESTS_DIR, staleFile);
        if (claimStat && now - claimStat.mtimeMs > STALE_CLAIM_MS) {
          var rid = claimIdFromName(nm);
          // Requeue only while the exact reservation is still active. Missing
          // reservation can mean the final handoff succeeded and the process
          // crashed before claim unlink; guessing "not run" would duplicate a
          // mutating prompt. Missing-reservation and unsafe claims therefore
          // remain private for explicit root recovery.
          var staleRead = rid ? fileGuards.boundedRegularFileUnder(
            PROJECT_ROOT, REQUESTS_DIR, staleFile, CLAIM_FILE_MAX_BYTES
          ) : null;
          var staleRecord = null;
          if (staleRead) {
            try {
              staleRecord = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(staleRead.bytes));
            } catch (parseError) {}
            if (requestsMod.requestRecordIssue(staleRecord, PROJECT_ROOT)) staleRecord = null;
          }
          var staleReservation = staleRecord ? requestsMod.inspectRequestReservation(staleRecord.stem) : null;
          var exactReservation = staleReservation && staleReservation.status === 'active' &&
            staleReservation.record.requestId === rid &&
            staleReservation.record.fingerprint === requestsMod.requestFingerprint(staleRecord);
          var staleGeneration = staleRead ? { proof: staleRead.stat, bytes: staleRead.bytes } : null;
          if (rid && exactReservation && requeueClaim(rid, staleGeneration)) {
            console.warn('[runner] restored stale claim to requests:', nm);
          } else {
            console.warn('[runner] retained stale private claim for explicit recovery:', nm);
          }
        }
      } catch (e) {}
    }
  }
}

// Bound disk + per-poll cost: restore stale same-directory private claims and
// prune the oldest session transcript pairs beyond RETAIN_RUNS — never live
// ones, locked stems, or "setup". Unknown cross-directory claims in RUNS_DIR are
// deliberately retained for explicit recovery; guessing their handoff state
// would reintroduce the duplicate-execution ambiguity this protocol removes.
function cleanup() {
  cleanupClaims();
  var listed = boundedRuntimeNames(RUNS_DIR);
  if (!listed.ok) return;
  var sidecars = listed.names.filter(function (name) { return name.endsWith('.session.json'); })
    .map(function (name) { return name.slice(0, -'.session.json'.length); });
  if (sidecars.length > RUNTIME_SIDECARS_MAX) return;
  if (sidecars.length <= RETAIN_RUNS) return;
  var locked = lockedStems();
  if (!locked) return;
  var metas = sidecars.map(function (base) {
    var sidecarStat = fileGuards.statRegularFileUnder(
      PROJECT_ROOT, RUNS_DIR, path.join(RUNS_DIR, base + '.session.json')
    );
    var m = sidecarStat ? sidecarStat.mtimeMs : 0;
    return { base: base, mtime: m };
  });
  metas.sort(function (a, b) { return a.mtime - b.mtime; });   // oldest first
  var toDelete = metas.length - RETAIN_RUNS;
  var inspectedBytes = 0;
  for (var j = 0; j < metas.length && toDelete > 0; j++) {
    if (metas[j].base === 'setup') continue;                  // never prune the setup transcript
    var side;
    try {
      var sideRead = fileGuards.boundedRegularFileUnder(
        PROJECT_ROOT, RUNS_DIR,
        path.join(RUNS_DIR, metas[j].base + '.session.json'), SESSION_SIDECAR_MAX_BYTES
      );
      if (!sideRead) continue;
      inspectedBytes += sideRead.bytes.length;
      if (inspectedBytes > RUNTIME_SIDECARS_TOTAL_MAX_BYTES) return;
      side = JSON.parse(sideRead.bytes.toString('utf8'));
    } catch (e) { continue; }
    if (side && side.running) continue;                       // never prune a live run
    // Keep the transcript of any stem that still holds a lock — the dead-run
    // recovery modal needs it for the stop reason / last message.
    if (side && side.stem && locked[side.stem]) continue;
    var removedEvents = fileGuards.unlinkRegularFileUnder(
      PROJECT_ROOT, RUNS_DIR, path.join(RUNS_DIR, metas[j].base + '.events.jsonl'), { allowMissing: true }
    );
    if (!removedEvents) continue;
    var removedSidecar = fileGuards.unlinkRegularFileUnder(
      PROJECT_ROOT, RUNS_DIR, path.join(RUNS_DIR, metas[j].base + '.session.json'), { allowMissing: true }
    );
    if (removedSidecar) toDelete--;
  }
}

// Number of task sessions running right now — the concurrency gate. Delegates
// to sessions.js (the authority), excluding the persistent "setup" session.
function runningCount() { return sessions.taskRunningCount(); }

// The running session key for a stem, or null — used by the enqueue dedup.
function runIdForStem(stem) { return sessions.runningKeyForStem(stem); }

// {key, busy} for the stem's running session, or null — busy-aware variant so
// the enqueue dedup can admit a request whose stem session is warm but idle
// (tick() then delivers it into that session as the next turn).
function runInfoForStem(stem) {
  var info = sessions.runningInfoForStem(stem);
  if (!info) return null;
  var snapshot = sessions.list();
  var live = snapshot && snapshot[info.key];
  return { key: info.key, busy: info.busy, action: live && live.action || null };
}

function runIdForDedupKey(action, dedupKey) {
  if (!action || !dedupKey) return null;
  var all = sessions.list();
  var keys = Object.keys(all);
  for (var i = 0; i < keys.length; i++) {
    var s = all[keys[i]];
    if (s && s.running && !s.closing && s.action === action && s.dedupKey === dedupKey) return keys[i];
  }
  return null;
}

function boundedFenceFindings(result) {
  var rows = Array.isArray(result && result.findings) ? result.findings.slice(0, 30) : [];
  return rows.map(function (item) {
    return {
      code: typeof item.code === 'string' ? item.code : 'UNKNOWN',
      severity: typeof item.severity === 'string' ? item.severity : 'error',
      paths: Array.isArray(item.paths) ? item.paths.slice(0, 20) : []
    };
  });
}

function persistSuperseded(id, req, reason, result) {
  // A tombstone can outlive a failed reservation withdrawal. Reuse only its
  // immutable timestamp on replay; writeSupersededFile still requires every
  // canonical byte to match, so a changed verdict/conflict remains fail-closed.
  var existing = requestsMod.readSupersededFile(id);
  var tombstone = {
    version: 1,
    status: 'superseded',
    requestId: id,
    action: req.action,
    stem: req.stem,
    reason: reason,
    expectedState: req.expectedState,
    observedState: result && result.observedState || null,
    expectedSourceRevision: req.sourceRevision,
    observedSourceRevision: result && result.sourceRevision || null,
    admittedAt: req.createdAt,
    supersededAt: existing && existing.supersededAt || new Date().toISOString(),
    snapshotHash: result && result.snapshotHash || null,
    findings: boundedFenceFindings(result)
  };
  return requestsMod.writeSupersededFile(id, tombstone);
}

// Pure/read-only action+state+revision+INDEX fence. It is used once after claim
// and again synchronously under the attached writer lease immediately before
// reservation withdrawal/prompt handoff.
function inspectClaimForExecution(req) {
  var lockPresence = locksMod.lockPresence(req && req.stem);
  if (!lockPresence.validStem || lockPresence.present) {
    // A lock that appeared after enqueue belongs to another canonical task
    // generation (or is unsafe/unreadable). This is transient ownership, not a
    // stale-request verdict: retain reservation+claim and retry only after the
    // exact owner/recovery flow clears it.
    return {
      ok: false,
      retry: true,
      error: !lockPresence.validStem ? 'task-lock-stem-invalid' : 'task-lock-present'
    };
  }
  var result;
  try { result = taskIntegrity.validateAction(req.action, req.stem, 'runner'); }
  catch (error) {
    return { ok: false, retry: true, error: String(error && error.message || error).slice(0, 500) };
  }
  var reason = null;
  if (result.observedState !== req.expectedState) reason = 'state-changed';
  else if (result.sourceRevision !== req.sourceRevision) reason = 'source-revision-changed';
  else if (!taskIntegrity.admissionForAction(result, req.stem).ok) {
    reason = 'task-integrity-invalid';
  }
  if (!reason) return { ok: true, result: result };
  return { ok: false, retry: false, reason: reason, result: result };
}

// Initial post-claim fence adds the durable prompt-free terminal tombstone.
// Final pre-prompt fencing uses inspectClaimForExecution() and persists only
// after sessions.send/start has refused the handoff, so no prompt can race it.
function fenceClaimForExecution(id, req) {
  var inspected = inspectClaimForExecution(req);
  if (inspected.ok || inspected.retry) return inspected;
  var reason = inspected.reason;
  var result = inspected.result;
  var persisted = persistSuperseded(id, req, reason, result);
  return { ok: false, retry: !persisted, persisted: persisted, reason: reason, result: result };
}

function restoreClaimForRetry(id, req, generation) {
  var reservation = requestsMod.inspectRequestReservation(req.stem);
  if (reservation.status !== 'active' || reservation.record.requestId !== id ||
      reservation.record.fingerprint !== requestsMod.requestFingerprint(req)) {
    // Never recreate a withdrawn handoff receipt. Missing/different ownership
    // after a release attempt is an at-most-once ambiguity: automatic retry
    // could duplicate a prompt another drainer already accepted.
    console.error('[runner] exact request reservation is no longer active; claim kept private:', id, reservation.code || reservation.status);
    return false;
  }
  if (!requeueClaim(id, generation)) {
    console.error('[runner] could not restore claim no-clobber; claim kept private:', id);
    return false;
  }
  return true;
}

// Terminal consumption order is tombstone -> exact reservation release ->
// claim unlink. If withdrawal cannot be proven, keep/requeue the claim; never
// discard the only recovery evidence and never execute its prompt.
function consumeTerminalClaim(id, req, handle, generation) {
  if (requestsMod.releaseRequestReservation(handle)) {
    if (!discardClaim(id, generation)) console.error('[runner] terminal claim unlink failed; retained private without reservation:', id);
    return true;
  }
  var after = requestsMod.inspectRequestReservation(req.stem);
  if (after.status === 'active' && after.record.requestId === id &&
      after.record.fingerprint === requestsMod.requestFingerprint(req)) {
    restoreClaimForRetry(id, req, generation);
  } else {
    console.error('[runner] reservation withdrawal unverified; terminal claim retained private:', id);
  }
  return false;
}

function settleRefusedHandoff(id, req, handle, handoff, generation) {
  if (handoff.fence && !handoff.fence.ok && !handoff.fence.retry) {
    var persisted = persistSuperseded(id, req, handoff.fence.reason, handoff.fence.result);
    if (persisted) consumeTerminalClaim(id, req, handle, generation);
    else restoreClaimForRetry(id, req, generation);
    return;
  }
  // Covers transient fence failure, pre-hook session refusal and the rare
  // synchronous stdin failure. Requeue is permitted only while the exact
  // reservation remains active; a completed/ambiguous withdrawal keeps the
  // claim private so automatic retry cannot duplicate a handed-off prompt.
  restoreClaimForRetry(id, req, generation);
}

function supersedeIntakeAtExecution(req) {
  if (req.action !== 'prep' && req.action !== 'answers' && req.action !== 'drop') return;
  try { shallowIntake.supersede(req.stem, 'authoritative-' + req.action); }
  catch (error) { console.warn('[runner] shallow intake supersession failed for', req.stem + ':', error && error.message); }
}

function tick() {
  if (!enabled) return;
  // Binary presence is not execution readiness. In particular, a logged-out
  // or revoked CLI must not publish the marker that makes the standby /loop
  // worker stand down, and must not claim an at-most-once request merely to
  // fail authentication after prompt delivery.
  if (!authReady()) {
    // Serial-safety handoff: while one of OUR children still owns a board-task
    // writer lease, the single execution slot is genuinely occupied, so the
    // marker must keep standing the standby worker down. Withdrawing it on a
    // mid-run auth flip would hand the queue to a second shared-root writer
    // while the child keeps editing the tree. Non-writer children (skills:*
    // installs, read-only terminal turns) hold no board-task lease and must
    // NOT block a legal standby takeover, so the durable lease — not the raw
    // session count — is the deciding evidence.
    if (runningCount() > 0 && finalizations.ownTaskSessionWriterActive()) {
      // Refresh only a marker we still own (or an ownerless slot): with the
      // fd lost, the unconditional create path would atomically replace a
      // FOREIGN live runner's marker — the exclusion probe applies here too.
      if (runnerMarkerFd !== null || !foreignRunnerOwner()) touchMarker();
      if (authReadyLogged !== false) {
        authReadyLogged = false;
        console.warn('[runner] Claude CLI is not authenticated — holding the runner marker while a task child is still running.');
      }
      return;
    }
    withdrawMarker();
    if (authReadyLogged !== false) {
      authReadyLogged = false;
      console.warn('[runner] Claude CLI is not authenticated — durable task requests remain queued.');
    }
    return;
  }
  if (authReadyLogged === false) {
    console.log('[runner] Claude CLI authentication is ready — queue drain resumed.');
  }
  authReadyLogged = true;
  if (runnerMarkerFd === null) {
    // Runner-vs-runner exclusion: never steal a marker whose owner may still
    // be alive (a second site process on the same project root would other-
    // wise drain the same queue with its own in-memory cap). Stand down and
    // re-probe next tick; a provably dead owner falls through to takeover.
    var foreignRunner = foreignRunnerOwner();
    if (foreignRunner) {
      var foreignKey = String(foreignRunner.pid) + ':' + foreignRunner.state;
      if (foreignRunnerLogged !== foreignKey) {
        foreignRunnerLogged = foreignKey;
        console.warn('[runner] another runner owns the liveness marker (' +
          (foreignRunner.pid === null ? 'unprovable' : 'pid ' + foreignRunner.pid + ', ' + foreignRunner.state) +
          '); this runner stands down.');
      }
      return;
    }
  }
  if (foreignRunnerLogged !== null) {
    foreignRunnerLogged = null;
    console.log('[runner] liveness marker is ours to own — queue drain resumed.');
  }
  if (!touchMarker()) {                       // refresh liveness every pass (busy or idle)
    console.error('[runner] runtime directory became unsafe; queue drain paused.');
    return;
  }
  if ((tickN++ % 30) === 0) cleanup();        // ~once a minute at POLL_MS=2s
  var capacity = MAX_PARALLEL - runningCount();
  if (capacity <= 0) return;
  // Durable occupancy hold. Unscoped it answers ONE question: is the writer
  // lease store provable at all? An unprovable store must never admit a
  // writer, so that holds the whole drain. Per-TASK occupancy is checked at
  // claim time below, because a live board-task writer for another stem is no
  // longer a reason to hold this one: the two runs own disjoint checkouts.
  var occupancy = finalizations.foreignTaskSessionWriterIssue();
  if (occupancy) {
    if (occupancyLoggedCode !== occupancy.code) {
      occupancyLoggedCode = occupancy.code;
      console.warn('[runner] queue drain held — ' + occupancy.code + ': ' + occupancy.message);
    }
    return;
  }
  if (occupancyLoggedCode !== null) {
    occupancyLoggedCode = null;
    console.log('[runner] board-task writer cleared — queue drain resumed.');
  }
  var queueScan = requestsMod.scanRequests();
  if (!queueScan.ok) {
    if (queueScanWarningCode !== queueScan.code) {
      queueScanWarningCode = queueScan.code;
      console.error('[runner] queue scan unavailable; queue drain paused:', queueScan.code);
    }
    return;
  }
  if (queueScanWarningCode !== null) {
    queueScanWarningCode = null;
    console.warn('[runner] queue scan recovered; queue drain resumed.');
  }
  var ids = queueScan.rows.map(function (row) { return row.id; });
  // Run-gate (see header): hold only visual `run` requests whose enforcement
  // net is unwired. The same strict queue snapshot drives both claiming and
  // task-scoped gating.
  var gateSkip = null;
  if (ids.length) {
    var gateErr = null;
    var queued = queueScan.rows;
    for (var g = 0; g < queued.length; g++) {
      if (queued[g].action !== 'run') continue;
      var requestGateError = sessions.runGateError(queued[g].stem);
      if (!requestGateError) continue;
      if (!gateSkip) gateSkip = Object.create(null);
      gateSkip[queued[g].id] = true;
      if (!gateErr) gateErr = requestGateError;
    }
    if (!!gateErr !== gateLogged) {
      gateLogged = !!gateErr;
      console.warn(gateErr
        ? '[runner] run-gate ACTIVE — queued `run` requests held: ' + gateErr
        : '[runner] run-gate cleared — draining queued `run` requests.');
    }
  }
  for (var i = 0; i < ids.length && capacity > 0; i++) {
    const id = ids[i];
    if (gateSkip && gateSkip[id]) continue;   // held in queue until the net is wired
    const claimed = claim(id);
    if (!claimed) continue;                   // lost the race, or malformed bytes retained privately
    const req = claimed.record;
    const claimGeneration = claimed.generation;
    var invalidClaim = claimedRequestIssue(req, PROJECT_ROOT);
    if (invalidClaim) {
      console.warn('[runner] retained invalid private claim for explicit recovery:', id, '(' + invalidClaim + ')');
      continue;
    }
    const stem = req.stem;
    // Per-task occupancy: a board-task writer for THIS stem that this process
    // does not own is an orphan child, a standby execution, or another site
    // process holding the same task. beginMutation would refuse the spawn
    // anyway; holding here avoids a claim→refuse→requeue bounce.
    var stemOccupancy = stem ? finalizations.foreignTaskSessionWriterIssue(stem) : null;
    if (stemOccupancy) {
      console.warn('[runner] ' + stem + ' held — ' + stemOccupancy.code + ': ' + stemOccupancy.message);
      requeueClaim(id, claimGeneration); // best effort; failure deliberately leaves private
      continue;
    }
    // Require the exact HTTP reservation before any retry/terminal decision.
    // It remains durable through
    // every queue→claim move until the final under-lease handoff.
    var reservation = requestsMod.ensureRequestReservation(id, req);
    if (!reservation.ok) {
      console.warn('[runner] exact per-stem reservation unavailable; request retained:', id, reservation.code);
      requeueClaim(id, claimGeneration); // best effort; failure deliberately leaves private
      continue;
    }
    const reservationHandle = reservation.handle;
    // A durable finalization marker supersedes every queued AI action for this
    // stem. Consume the stale request now; retaining it would re-run an already
    // shipped task immediately after marker cleanup.
    if (stem && finalizations.hasMarker(stem)) {
      console.warn('[runner] dropping stale queued action for task under finalization:', stem, req.action || 'unknown');
      if (persistSuperseded(id, req, 'finalization-active', null)) consumeTerminalClaim(id, req, reservationHandle, claimGeneration);
      else restoreClaimForRetry(id, req, claimGeneration);
      continue;
    }
    // A finalizer owns the global publication mutex but may still be in the
    // short pre-marker preparation window. Requeue (do not drop) unrelated or
    // not-yet-marked work until that deterministic transaction publishes its
    // marker or completes.
    if (finalizations.mutationBlocked(stem)) {
      restoreClaimForRetry(id, req, claimGeneration);
      continue;
    }
    var executionFence = fenceClaimForExecution(id, req);
    if (!executionFence.ok) {
      if (executionFence.retry) {
        console.warn('[runner] task-state fence unavailable; request retained:', stem, executionFence.error || executionFence.reason || 'unknown');
        restoreClaimForRetry(id, req, claimGeneration);
      } else {
        console.warn('[runner] superseded stale queued action:', stem, req.action, executionFence.reason);
        consumeTerminalClaim(id, req, reservationHandle, claimGeneration);
      }
      continue;
    }
    // Pipeline improvement 01, Phase 2 — mandatory execution isolation: a
    // `run` claim provisions (or resumes) its manager-verified worktree
    // BEFORE any spawn or warm-session delivery. A refused provision keeps
    // the durable request queued — there is no shared-root fallback.
    let executionContext = null;
    if (req.action === 'run') {
      var provisioned = worktreeManager.provision({
        stem: stem, runId: id, requestId: id, sourceRevision: req.sourceRevision
      });
      if (!provisioned.ok) {
        if (provisionHoldLogged !== provisioned.code) {
          provisionHoldLogged = provisioned.code;
          console.warn('[runner] run held — worktree provisioning refused: ' +
            provisioned.code + (provisioned.message ? ' — ' + provisioned.message : ''));
        }
        restoreClaimForRetry(id, req, claimGeneration);
        continue;
      }
      if (provisionHoldLogged !== null) {
        provisionHoldLogged = null;
        console.log('[runner] worktree provisioning recovered — run admission resumed.');
      }
      executionContext = { worktreeId: provisioned.worktreeId, runId: provisioned.runId };
    }
    // A session already running for this stem: if it's BUSY (mid-turn or paused
    // on a question), requeue — a second click must not double-run or hijack the
    // pending answer. If it's warm-but-IDLE (its turn finished; the close may be
    // lock-deferred), deliver the prompt INTO it as the next turn — same context
    // continuation the session-per-stem design intends — so an idle/zombie
    // session never bounces the request forever. send() re-stamps action/dedup
    // and returns false if the session raced busy/away — then requeue as before.
    const handoff = { called: false, fence: null, released: false, settled: null };
    const beforePrompt = function () {
      handoff.called = true;
      handoff.fence = inspectClaimForExecution(req);
      if (!handoff.fence.ok) return false;
      handoff.released = requestsMod.releaseRequestReservation(reservationHandle);
      return handoff.released;
    };
    const onPromptSettled = function (delivered, error) {
      if (handoff.settled !== null) return;
      handoff.settled = delivered === true;
      if (handoff.settled) {
        supersedeIntakeAtExecution(req);
        if (!discardClaim(id, claimGeneration)) console.error('[runner] delivered prompt claim retained private:', id);
      } else {
        console.error('[runner] prompt delivery remained ambiguous; claim retained private:', id,
          error && error.message || 'stdin settlement failed');
      }
    };
    var liveInfo = stem ? sessions.runningInfoForStem(stem) : null;
    if (liveInfo) {
      // The Board Prepare policy is also carried by the spawned child's
      // process environment. Never reuse a child across that policy boundary:
      // a non-prep child lacks the transition fence, while a prep child must
      // not leak its stricter environment into another action.
      var noQuestionsCompatible = liveInfo.noQuestions === (req.action === 'prep');
      if (!liveInfo.busy && noQuestionsCompatible) {
        if (sessions.send(liveInfo.key, req.prompt, {
          action: req.action,
          executionContext: executionContext,
          noQuestions: req.action === 'prep',
          dedupKey: req.dedupKey,
          dedupReport: req.dedupReport,
          expectedState: req.expectedState,
          sourceRevision: req.sourceRevision,
          beforePrompt: beforePrompt,
          onPromptSettled: onPromptSettled
        })) {
          if (!handoff.called || !handoff.released) {
            console.error('[runner] session accepted prompt without an exact reservation handoff:', id);
            sessions.cancel(liveInfo.key);
            settleRefusedHandoff(id, req, reservationHandle, handoff, claimGeneration);
            continue;
          }
          capacity--;   // conservative: the reused session already held a slot, but never over-admit
          continue;
        }
      }
      settleRefusedHandoff(id, req, reservationHandle, handoff, claimGeneration);
      continue;
    }
    var key = 'task:' + stem;
    var st = sessions.start(key, {
      stem: stem,
      action: req.action,
      executionContext: executionContext,
      noQuestions: req.action === 'prep',
      dedupKey: req.dedupKey,
      dedupReport: req.dedupReport,
      expectedState: req.expectedState,
      sourceRevision: req.sourceRevision,
      prompt: req.prompt,
      beforePrompt: beforePrompt,
      onPromptSettled: onPromptSettled
    });
    if (st && st.running && handoff.called && handoff.released) {
      capacity--;
    } else {
      if (st && st.running) sessions.cancel(key);
      settleRefusedHandoff(id, req, reservationHandle, handoff, claimGeneration);
      console.warn('[runner] could not start session', key, st && st.error);
    }
  }
}

// Best-effort: the children belong to sessions.js (sessions.killAll kills them
// on shutdown). Here we just drop the liveness marker so a /loop worker knows
// the runner is gone.
function killAll() {
  withdrawMarker();
}

function init() {
  if (started) return;
  started = true;
  if (process.env.RUNNER_DISABLED === '1') {
    console.log('[runner] disabled via RUNNER_DISABLED=1 — using /loop worker fallback.');
    return;
  }
  if (!writerLeases.PROCESS_START_ID_RE.test(String(RUNNER_PROCESS_START_ID || ''))) {
    console.error('[runner] process-start identity is unavailable; runner remains dormant until this host can prove exact process generations.');
    return;
  }
  probeClaudeOnPath(function (found) {
    if (!found) {
      console.log('[runner] `claude` CLI not found on PATH — runner dormant; use the /loop worker.');
      return;
    }
    if (!ensureRunsDir()) {
      console.error('[runner] runs directory is unsafe; runner remains dormant.');
      return;
    }
    enabled = true;
    // tick() owns both the auth-readiness decision and marker publication, so
    // there is no startup window in which a logged-out CLI looks attached. It
    // also owns cleanup: startup must prove runner-marker exclusion before an
    // aged private claim can be restored.
    tick();
    setInterval(tick, POLL_MS);
    console.log('[runner] enabled — authenticated CLI sessions drain queued tasks ' +
      'up to MAX_PARALLEL=' + MAX_PARALLEL + ', each in its own task worktree.');
  });
}

module.exports = {
  init: init,
  tick: tick,
  killAll: killAll,
  runningCount: runningCount,
  runInfoForStem: runInfoForStem,
  runIdForDedupKey: runIdForDedupKey,
  claimedRequestIssue: claimedRequestIssue,
  inspectClaimForExecution: inspectClaimForExecution,
  fenceClaimForExecution: fenceClaimForExecution,
  ensureRunsDir: ensureRunsDir,
  touchMarker: touchMarker,
  boundedRuntimeNames: boundedRuntimeNames,
  isEnabled: function () { return enabled; },
  isReady: function () {
    return enabled && runnerMarkerFd !== null && authReady();
  },
  MAX_PARALLEL: MAX_PARALLEL,
  RUNS_DIR: RUNS_DIR
};
