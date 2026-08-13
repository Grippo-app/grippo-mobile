'use strict';

// ---------------------------------------------------------------------------
// Multi-session manager. Generalizes the wizard's single interactive
// stream-json session (the prior single-session model) to N sessions
// keyed by CONTEXT, so Setup, every Wizard step, and every Board task share ONE
// engine and ONE terminal UI:
//
//   "setup"          — bootstrap + all wizard steps (one persistent, shared
//                       session; context carries across steps as today)
//   "task:<stem>"    — one board task (prep → answers → run share its context)
//
// Each session is a persistent `claude` child over the stream-json
// bidirectional protocol — dependency-free, no PTY:
//
//   claude -p --input-format stream-json --output-format stream-json --verbose
//          --dangerously-skip-permissions
//
// A step (or a user answer) is one stdin user message; stdout is newline-
// delimited JSON events parsed into a compact list. Events are buffered in
// memory (ring buffer) AND appended to .cache/tasks/runs/<safeKey>.events.jsonl, with
// a <safeKey>.session.json status sidecar — so a closed terminal, a page
// reload, or reopening an old task's transcript all work (the user's "return to
// the terminal later" need). All event text is carried as plain strings and
// rendered client-side via textContent, so nothing a session prints can inject.
//
// Concurrency is the caller's concern: runner.js gates task spawns on
// taskRunningCount() before claiming a queued request. `setup` is singleton and
// foreground, so it is never capped here.
// ---------------------------------------------------------------------------

var cp    = require('child_process');
var StringDecoder = require('string_decoder').StringDecoder;
var fs    = require('fs');
var path  = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var locks = require('./locks');
var childEnv = require('./child-env').childEnv;
var gitMod = require('./git');
var finalizationsMod = require('./finalizations');
var projectCfg = require('./project-config');
var taskIntegrityMod = require('./task-integrity');
var taskSourceMod = require('./task-source');
var designParser = require('../../figma/scripts/design-parser.cjs');

var PROJECT_ROOT = paths.PROJECT_ROOT;
var RUNS_DIR     = paths.RUNS_DIR;

// Lock-staleness bound for the maybeAutoClose defer. A turn can end
// mid-pipeline (e.g. right after a Skill-gate report) while the task's lock
// (.cache/tasks/locks/<STEM>.json) is still HELD and fresh — auto-closing then
// would SIGTERM/SIGKILL an in-flight run and strand the task as 'stopped'. So if
// the stem's lock is present AND its last activity is within this window, defer
// the close (like askedThisTurn) and let the existing idle/lock-stale machinery
// handle it. A STALE or MISSING lock still reaps exactly as before — the
// slot-leak guarantee (a dead/stranded run must be reaped) is preserved.
// Same 20-min semantics as board worker-support's STALE_LOCK_MS
// (panels/board.js) and the header's STALE_MS (scripts/status.js); kept in
// lockstep with them.
var STALE_LOCK_MS = 20 * 60 * 1000;

var MAX_EVENTS  = 3000;            // in-memory ring buffer per session
var TEXT_CAP    = 8000;            // per-event text clamp
var BUF_CAP     = TEXT_CAP * 8;    // ceiling on the un-parsed stdout buffer (a newline-less runaway line can't grow it unbounded)
var RUNS_DIRECTORY_ENTRIES_MAX = 10000;
var SESSION_SIDECARS_MAX = 1000;
var SESSION_SIDECAR_MAX_BYTES = 64 * 1024;
var SESSION_SIDECARS_TOTAL_MAX_BYTES = 8 * 1024 * 1024;
var SESSION_EVENTS_MAX_BYTES = 32 * 1024 * 1024;
// v3 adds runId. A sidecar written by an older build has a different field
// set and is rejected outright — the version is a hard constant, never a
// behavioural fork, and a session sidecar is regenerable runtime state.
var SESSION_SIDECAR_VERSION = 3;
var EXECUTION_RUN_ID_RE = /^[0-9]{1,16}-[a-z0-9]{1,32}$/;
// `worktreeId` IS the execution generation identity the plan calls
// `executionGeneration`; a second name for it would be a second source of
// truth. `candidateTree` is deliberately absent: nothing is sealed while a
// session is alive, so the field could only ever be null here.
var SESSION_SIDECAR_FIELDS = ['action', 'awaitingTurn', 'canceled', 'dedupKey', 'dedupReport', 'endedAt', 'exitCode', 'key', 'minSeq', 'nextSeq', 'running', 'runId', 'sessionId', 'startedAt', 'stem', 'version', 'worktreeId', 'executionRoot', 'baseCommit', 'candidateRef'].sort();
var FINISHED_TTL_MS = 5 * 60 * 1000;  // keep finished sessions in memory this long
// An 'awaiting' session (asked the user via needs_action, kept alive so the
// answer continues the SAME run) holds a concurrency slot. If the user never
// answers, free the slot after this idle window so abandoned awaiting sessions
// can't permanently wedge the runner. Generous — the "ждёт ответа" badge shows
// the whole time; answering (send) cancels the timer.
var AWAITING_IDLE_MS = 20 * 60 * 1000;
// Board Prepare is deliberately non-interactive. If the model nevertheless
// emits needs_action, continue the same ownership-fenced child with the fixed
// safe-default policy instead of surfacing a question to the user. Bound the
// fallback so a non-converging model cannot loop forever.
var PREP_AUTO_DEFAULT_LIMIT = 3;
var PREP_AUTO_DEFAULT_PROMPT = [
  'BOARD PREP POLICY: NO QUESTIONS.',
  'Do not ask the user or publish pending questions.',
  'Use repository evidence for decidable gaps; choose the safest reversible conservative default and record it as an `Assumed —` Input.',
  'Keep irreversible, destructive, authorization, breaking-contract, or missing-owner ambiguity out of scope and report a typed actionable blocker or follow-up.',
  'Continue toward a canonical todo promotion whenever a safe runnable scope remains.'
].join(' ');
// How often a lock-deferred session re-checks whether its lock is gone/stale.
// maybeAutoClose defers the close while the stem's lock is held and fresh;
// without a re-check that deferral was PERMANENT (the zombie-session bug: the
// terminal shows the turn done, but the session stays running:true forever,
// pinning a runner slot and deduping every re-run). The recheck loop closes the
// session as soon as the lock is removed, or once its activity goes stale
// (STALE_LOCK_MS), whichever comes first.
var LOCK_RECHECK_MS = 60 * 1000;
// Private send() capability used only by start() for the first turn of a
// server-owned runtime receipt action. Public/warm sends still acquire a
// writer lease, and mutating initial prompts still prove their pre-spawn lease
// through the existing `true` contract.
var RUNTIME_ONLY_INITIAL_PROMPT = Object.freeze({});
// Internal capability for a terminal follow-up that is deliberately spawned
// without workspace mutation authority. The child is additionally restricted
// to read-only built-in tools at the CLI boundary (see start()).
var CONVERSATION_ONLY_INITIAL_PROMPT = Object.freeze({});
var RUNTIME_ONLY_ACTIONS = Object.freeze({
  'figma:whoami': 'whoami',
  'figma:fileaccess': 'file-access',
  'figma:sync-tokens': 'sync-tokens',
  'figma:sync-components': 'sync-components'
});

function runtimeOnlyInitialPromptAllowed(key, meta) {
  return Object.prototype.hasOwnProperty.call(RUNTIME_ONLY_ACTIONS, key) &&
    RUNTIME_ONLY_ACTIONS[key] === meta.action &&
    !meta.resume && typeof meta.beforePrompt !== 'function';
}

// A closed Claude leader does not prove its detached process group is empty.
// Probe aggressively for a short window, then keep a low-frequency unref'd
// reaper alive for as long as the durable writer lease exists.  Test-only
// timing overrides keep the process-tree regressions fast without weakening
// production defaults.
function boundedDuration(name, fallback, min, max) {
  var value = Number(process.env[name]);
  return Number.isFinite(value) ? Math.max(min, Math.min(max, Math.floor(value))) : fallback;
}
var TURN_LEASE_FAST_DEADLINE_MS = boundedDuration('SESSION_LEASE_FAST_DEADLINE_MS', 5000, 10, 60000);
var TURN_LEASE_FAST_PROBE_MS = boundedDuration('SESSION_LEASE_FAST_PROBE_MS', 50, 5, 1000);
var TURN_LEASE_REAPER_MS = boundedDuration('SESSION_LEASE_REAPER_MS', 1000, 20, 60000);
var STDIN_FAILURE_KILL_GRACE_MS = boundedDuration('SESSION_STDIN_KILL_GRACE_MS', 1000, 10, 10000);

// key -> live session object. A finished session lingers here for FINISHED_TTL_MS
// (so the UI sees the final status), then is pruned; its transcript survives on
// disk and is served from there afterwards.
var sessions = Object.create(null);
// Free-text submitted while a turn is busy/closing is accepted here and
// delivered in order at the next safe boundary. This queue is intentionally
// process-local: the transcript records a user event only after stdin accepts
// it, so a server restart can never replay an ambiguously delivered prompt.
var terminalInputQueues = Object.create(null);
var terminalInputDrainTimers = Object.create(null);
var MAX_TERMINAL_INPUTS_PER_SESSION = 20;
var MAX_TERMINAL_INPUT_CHARS_PER_SESSION = 120000;
var turnPublicationLifecycle = null;

function configureTurnPublication(lifecycle) {
  turnPublicationLifecycle = lifecycle &&
    typeof lifecycle.prepareTurn === 'function' && typeof lifecycle.markResult === 'function' &&
    typeof lifecycle.dispatch === 'function' && typeof lifecycle.abort === 'function'
    ? lifecycle : null;
}
function abortTurnPublication(s) {
  if (!s || !turnPublicationLifecycle) return;
  var publicationIds = [s.turnPublicationId, s.readyPublicationId].filter(function (publicationId, index, values) {
    return !!publicationId && values.indexOf(publicationId) === index;
  });
  s.turnPublicationId = null;
  s.readyPublicationId = null;
  publicationIds.forEach(function (publicationId) {
    try { turnPublicationLifecycle.abort(publicationId); } catch (error) {}
  });
}
function dispatchReadyPublication(s) {
  if (!s || !s.readyPublicationId || s.writerLease || !turnPublicationLifecycle) return;
  var publicationId = s.readyPublicationId;
  s.readyPublicationId = null;
  try { turnPublicationLifecycle.dispatch(publicationId); }
  catch (error) { console.error('[sessions] task publication dispatch failed:', error && error.message || error); }
}

// mtime-keyed cache for the disk-discovery half of list(): so a steady-state
// /api/state poll (every ~1.5s) with no on-disk change does zero JSON.parse and
// at most one cheap readdir. Each entry is { mtimeMs, parsed } keyed by sidecar
// filename; a sidecar is re-read+re-parsed only when its mtimeMs advances past
// the cached value, reusing the cached parse otherwise. dirMtimeMs short-circuits
// the readdir's worth of statSync churn: an unchanged RUNS_DIR mtime means no
// sidecar was added/removed, so only already-cached entries can apply.
var sidecarCache = Object.create(null);   // filename -> { mtimeMs, parsed }
var dirMtimeMs   = -1;                      // last-seen RUNS_DIR mtimeMs (-1 = never scanned)

function sameDirectorySnapshot(left, right) {
  return !!left && !!right && left.dev === right.dev && left.ino === right.ino &&
    left.modeExact === right.modeExact && left.sizeExact === right.sizeExact &&
    left.mtimeNs === right.mtimeNs && left.ctimeNs === right.ctimeNs;
}

function boundedRunsNames() {
  var before = fileGuards.realDirectoryUnder(PROJECT_ROOT, RUNS_DIR, { allowMissing: true });
  if (!before) return { ok: false, code: 'runs-directory-unsafe', names: [] };
  if (!before.exists) return { ok: true, names: [], directory: before };
  var listed = fileGuards.boundedDirectoryNamesUnder(PROJECT_ROOT, RUNS_DIR, RUNS_DIRECTORY_ENTRIES_MAX);
  if (!listed.ok) return {
    ok: false,
    code: listed.code === 'directory-entry-limit' ? 'runs-directory-entry-limit' : 'runs-directory-read-failed',
    names: []
  };
  var after = fileGuards.realDirectoryUnder(PROJECT_ROOT, RUNS_DIR);
  if (!after || !sameDirectorySnapshot(before.stat, after.stat)) {
    return { ok: false, code: 'runs-directory-changed', names: [] };
  }
  return { ok: true, names: listed.names, directory: before };
}

function runsDirectoryUnchanged(directory) {
  if (!directory || !directory.exists || !directory.stat) return false;
  var after = fileGuards.realDirectoryUnder(PROJECT_ROOT, RUNS_DIR);
  return !!after && sameDirectorySnapshot(directory.stat, after.stat);
}

function boundedJsonFile(file, maxBytes) {
  var read = fileGuards.boundedRegularFileUnder(PROJECT_ROOT, RUNS_DIR, file, maxBytes);
  if (!read) return null;
  try { return { value: JSON.parse(read.bytes.toString('utf8')), stat: read.stat }; }
  catch (error) { return null; }
}

function ensureRunsDir() {
  return !!fileGuards.realDirectoryUnder(PROJECT_ROOT, RUNS_DIR, { create: true, mode: 0o700 });
}

// Filename-safe basename for a key. Valid keys (http.js validSessionKey) are "setup",
// "task:<stem>", the finite Figma action set (incl. "figma:screens:<stem>")
// and the exact interactive "contract:diff" action; replacing every non-[A-Za-z0-9_.-] run with "_" yields a
// unique, readable name and confines the path to RUNS_DIR (no traversal).
function safeKey(key)        { return String(key).replace(/[^A-Za-z0-9_.-]+/g, '_'); }
function eventsPathFor(key)  { return path.join(RUNS_DIR, safeKey(key) + '.events.jsonl'); }
function sidecarPathFor(key) { return path.join(RUNS_DIR, safeKey(key) + '.session.json'); }

var FIGMA_SESSION_KEYS = {
  'figma:whoami': 1,
  'figma:fileaccess': 1,
  'figma:sync-tokens': 1,
  'figma:sync-components': 1,
  'figma:shipdriftsweep': 1
};

function validSessionKey(key) {
  if (key === 'setup') return true;
  if (typeof key !== 'string') return false;
  if (key.indexOf('task:') === 0) return locks.validTaskStem(key.slice('task:'.length));
  if (key.indexOf('figma:screens:') === 0) return locks.validTaskStem(key.slice('figma:screens:'.length));
  if (key.indexOf('figma:rebundle:') === 0) return locks.validTaskStem(key.slice('figma:rebundle:'.length));
  if (key.indexOf('figma:') === 0) return Object.prototype.hasOwnProperty.call(FIGMA_SESSION_KEYS, key);
  if (key.indexOf('contract:') === 0) return key === 'contract:diff';
  if (key.indexOf('skills:') === 0) return /^[a-z]+$/.test(key.slice('skills:'.length));
  return false;
}

function exactInstant(value) {
  if (typeof value !== 'string' || value.length < 20 || value.length > 40) return false;
  var parsed = Date.parse(value);
  return Number.isFinite(parsed) && new Date(parsed).toISOString() === value;
}

function nullableBoundedString(value, max) {
  return value === null || (typeof value === 'string' && value.length > 0 && value.length <= max && value.indexOf('\0') < 0);
}

function sessionSidecarIssue(value, filename) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return 'sidecar must be an object';
  var keys = Object.keys(value).sort();
  if (keys.length !== SESSION_SIDECAR_FIELDS.length || keys.some(function (key, index) { return key !== SESSION_SIDECAR_FIELDS[index]; })) {
    return 'sidecar fields must exactly match the version-' + SESSION_SIDECAR_VERSION + ' contract';
  }
  if (value.version !== SESSION_SIDECAR_VERSION) return 'sidecar version must be ' + SESSION_SIDECAR_VERSION;
  if (!validSessionKey(value.key)) return 'sidecar key is invalid';
  if (filename && filename !== safeKey(value.key) + '.session.json') return 'sidecar filename does not match its key';
  var expectedStem = value.key.indexOf('task:') === 0
    ? value.key.slice('task:'.length) : null;
  if (value.stem !== expectedStem && !(expectedStem === null && nullableBoundedString(value.stem, 120))) {
    return 'sidecar stem does not match its key';
  }
  if (!nullableBoundedString(value.action, 80) || !nullableBoundedString(value.dedupKey, 240) ||
      !nullableBoundedString(value.dedupReport, 80) ||
      (value.dedupReport !== null && !/^sha256:[a-f0-9]{64}$/.test(value.dedupReport)) ||
      (value.dedupReport !== null && value.dedupKey === null)) return 'sidecar action or dedup metadata is invalid';
  if (typeof value.running !== 'boolean' || typeof value.awaitingTurn !== 'boolean' || typeof value.canceled !== 'boolean') {
    return 'sidecar flags must be booleans';
  }
  if (!exactInstant(value.startedAt) || !(value.endedAt === null || exactInstant(value.endedAt)) ||
      (value.endedAt !== null && Date.parse(value.endedAt) < Date.parse(value.startedAt))) return 'sidecar timestamps are invalid';
  if (value.running && value.endedAt !== null) return 'running sidecar cannot have endedAt';
  if (!value.running && value.endedAt === null) return 'finished sidecar requires endedAt';
  if (value.awaitingTurn && !value.running) return 'awaitingTurn requires a running session';
  if (!(value.exitCode === null || (Number.isSafeInteger(value.exitCode) && value.exitCode >= 0 && value.exitCode <= 255))) {
    return 'sidecar exitCode is invalid';
  }
  if (!nullableBoundedString(value.sessionId, 240) ||
      !Number.isSafeInteger(value.nextSeq) || value.nextSeq < 0 ||
      !Number.isSafeInteger(value.minSeq) || value.minSeq < 0 || value.minSeq > value.nextSeq) {
    return 'sidecar session/sequence metadata is invalid';
  }
  // Execution binding (pipeline improvement 01, Phase 2): only a task `run`
  // session may carry a worktree execution context, and it carries ALL four
  // fields or none.
  var executionFields = [value.worktreeId, value.runId, value.executionRoot, value.baseCommit, value.candidateRef];
  var boundCount = executionFields.filter(function (field) { return field !== null; }).length;
  if (boundCount !== 0 && boundCount !== 5) return 'sidecar execution binding must be complete or absent';
  // A task `run` ALWAYS executes inside a worktree, so an unbound run sidecar
  // is exactly the shared-root escape this phase deleted — never valid.
  if (boundCount === 0 && value.key.indexOf('task:') === 0 && value.action === 'run') {
    return 'a task run sidecar requires its execution binding';
  }
  if (boundCount === 5) {
    if (!(value.key.indexOf('task:') === 0 && value.action === 'run')) {
      return 'sidecar execution binding is only legal for task run sessions';
    }
    if (!/^wt-[a-f0-9]{32}$/.test(String(value.worktreeId)) ||
        !EXECUTION_RUN_ID_RE.test(String(value.runId)) ||
        typeof value.executionRoot !== 'string' || value.executionRoot[0] !== '/' ||
        value.executionRoot.length > 4096 ||
        !/^[a-f0-9]{40}$/.test(String(value.baseCommit)) ||
        !nullableBoundedString(value.candidateRef, 240)) {
      return 'sidecar execution binding is invalid';
    }
  }
  return null;
}

// Drop finished sessions whose TTL elapsed — bounds memory over a long server
// life without timers. Their transcript + sidecar remain on disk.
function prune() {
  var now = Date.now();
  var keys = Object.keys(sessions);
  for (var i = 0; i < keys.length; i++) {
    var s = sessions[keys[i]];
    // A closed UI session can still be the in-memory authority that owns a
    // durable writer lease while its background PGID reaper runs. Pruning that
    // object would hide the termination state and make the reaper's lifecycle
    // impossible to observe/reconcile through the session registry.
    if (s && !s.running && s.endedAt && !s.writerLease && !s.writerLeaseSettling && !s.writerLeaseReaperTimer) {
      var t = Date.parse(s.endedAt);
      if (isFinite(t) && now - t > FINISHED_TTL_MS) delete sessions[keys[i]];
    }
  }
}

function writeSidecar(s) {
  var body = {
    version: SESSION_SIDECAR_VERSION,
    key: s.key, stem: s.stem || null, action: s.action || null,
    dedupKey: s.dedupKey || null, dedupReport: s.dedupReport || null,
    running: s.running, awaitingTurn: s.awaitingTurn,
    startedAt: s.startedAt, endedAt: s.endedAt || null,
    exitCode: ((s.exitCode === null || s.exitCode === undefined) ? null : s.exitCode),
    canceled: !!s.canceled, sessionId: s.sessionId || null,
    worktreeId: s.executionContext ? s.executionContext.worktreeId : null,
    runId: s.executionContext ? (s.executionContext.runId || null) : null,
    executionRoot: s.executionContext ? s.executionContext.executionRoot : null,
    baseCommit: s.executionContext ? s.executionContext.baseCommit : null,
    candidateRef: s.executionContext ? s.executionContext.candidateRef : null,
    // nextSeq lets statusOf() answer for a non-live session without reading the
    // whole .events.jsonl; minSeq is the lowest seq still available on disk
    // after ring-buffer pruning (the .events.jsonl is trimmed to MAX_EVENTS on
    // read), so the client doesn't ask for seqs that were dropped.
    nextSeq: s.seq, minSeq: (s.events.length ? s.events[0].seq : 0)
  };
  var bytes = Buffer.from(JSON.stringify(body, null, 2) + '\n', 'utf8');
  if (bytes.length > SESSION_SIDECAR_MAX_BYTES) return false;
  return fileGuards.atomicReplaceRegularFile(
    PROJECT_ROOT, RUNS_DIR, sidecarPathFor(s.key), bytes,
    { create: true, mode: 0o600, maxBytes: SESSION_SIDECAR_MAX_BYTES }
  );
}

// Redact secret-bearing tokens from any event text before it persists to the
// transcript (.events.jsonl) or streams over SSE — so a token a tool's printed
// command or the agent echoes never lands on disk / on the wire. Patterns are
// kept specific (named headers, known provider PAT prefixes with a long suffix,
// explicit token= / --token forms) so ordinary prose is untouched. Applied to
// every event in pushEvent (cheap on already-bounded text).
function redactSecrets(str) {
  return String(str)
    .replace(/(X-Figma-Token:\s*)\S+/gi, '$1<redacted>')
    .replace(/(Authorization:\s*Bearer\s+)\S+/gi, '$1<redacted>')
    .replace(/figd_[A-Za-z0-9_-]{6,}/g, 'figd_<redacted>')
    .replace(/github_pat_[A-Za-z0-9_]{20,}/g, 'github_pat_<redacted>')   // GitHub fine-grained PAT (before ghp_, which it does not match)
    .replace(/ghp_[A-Za-z0-9]{20,}/g, 'ghp_<redacted>')                  // GitHub classic PAT
    .replace(/sk-[A-Za-z0-9_-]{20,}/g, 'sk-<redacted>')                  // OpenAI key (incl. sk-proj-…); long suffix avoids prose hits
    .replace(/xox[baprs]-[A-Za-z0-9-]{10,}/g, 'xox<redacted>')           // Slack bot/app/user/refresh tokens
    .replace(/((?:access_)?token=)[^\s&"']+/gi, '$1<redacted>')          // ?token=… / ?access_token=… query forms
    .replace(/(--token[ =])\S+/gi, '$1<redacted>');                      // --token <val> / --token=<val> flag forms
}

function pushEvent(s, kind, text, extra) {
  var safe = redactSecrets((text === null || text === undefined) ? '' : String(text));
  var ev = { seq: s.seq++, kind: kind, text: safe.slice(0, TEXT_CAP) };
  if (extra) { for (var k in extra) if (Object.prototype.hasOwnProperty.call(extra, k)) { var xv = extra[k]; ev[k] = (typeof xv === 'string') ? xv.slice(0, 200) : xv; } }
  s.events.push(ev);
  if (s.events.length > MAX_EVENTS) s.events.splice(0, s.events.length - MAX_EVENTS);
  if (s.logFd != null) {
    try {
      var line = Buffer.from(JSON.stringify(ev) + '\n', 'utf8');
      if (!Number.isSafeInteger(s.logBytes) || s.logBytes < 0 ||
          s.logBytes + line.length > SESSION_EVENTS_MAX_BYTES) {
        s.logTruncated = true;
        try { fs.closeSync(s.logFd); } catch (closeAtLimitError) {}
        s.logFd = null;
      } else {
        var offset = 0;
        while (offset < line.length) {
          var written = fs.writeSync(s.logFd, line, offset, line.length - offset, s.logBytes + offset);
          if (!written) throw new Error('session event write made no progress');
          offset += written;
        }
        s.logBytes += line.length;
      }
    } catch (e) { try { fs.closeSync(s.logFd); } catch (e2) {} s.logFd = null; }   // stop writing to a dead fd
  }
}

// Bind a follow-up turn to the exact generation and a fresh canonical task
// snapshot observed when its question/result was emitted. This catches manual
// edits after that boundary without rejecting edits the same Claude turn
// legitimately made before it paused.
function captureTaskTurnFence(s) {
  if (!s || s.key.indexOf('task:') !== 0) return null;
  var stem = s.stem || s.key.slice('task:'.length);
  if (!stem || typeof s.action !== 'string' || typeof s.expectedState !== 'string' ||
      typeof s.sourceRevision !== 'string') {
    return { ok: false, generation: s.turnGeneration, reason: 'task-answer-snapshot-unavailable' };
  }
  var result;
  try { result = taskIntegrityMod.validateAction(s.action, stem, 'runner'); }
  catch (error) {
    return { ok: false, generation: s.turnGeneration, reason: 'task-answer-integrity-unavailable' };
  }
  if (!result || !taskIntegrityMod.admissionForAction(result, stem).ok ||
      result.observedState !== s.expectedState ||
      typeof result.sourceRevision !== 'string') {
    return { ok: false, generation: s.turnGeneration, reason: 'task-answer-integrity-invalid' };
  }
  return {
    ok: true,
    generation: s.turnGeneration,
    action: s.action,
    expectedState: s.expectedState,
    admittedSourceRevision: s.sourceRevision,
    observedState: result.observedState,
    sourceRevision: result.sourceRevision
  };
}

// A free-form continuation is available only for the live task child that
// already owns the canonical lock. Capture that ownership boundary alongside
// the post-result task snapshot; the same facts are checked again immediately
// before delivery, so neither a lock handoff nor a task edit can race stdin.
function captureTaskIdleContinuationFence(s) {
  if (!s || s.key.indexOf('task:') !== 0) return null;
  if (!s.writerLease) {
    return { ok: false, generation: s.turnGeneration, reason: 'task-writer-lease-unavailable' };
  }
  var stem = s.stem || s.key.slice('task:'.length);
  var ownership;
  try { ownership = locks.lockOwnedBySession(stem, s.writerSessionId); }
  catch (error) {
    return { ok: false, generation: s.turnGeneration, reason: 'task-lock-owner-check-unavailable' };
  }
  if (!ownership || ownership.owned !== true) {
    return {
      ok: false,
      generation: s.turnGeneration,
      reason: 'task-lock-owner-mismatch',
      lockReason: ownership && ownership.reason || null
    };
  }
  return captureTaskTurnFence(s);
}

function markNeedsAction(s, text) {
  // Server-owned runtime receipt actions (Figma whoami/file-access/capture)
  // have no interactive answer rail. Authentication or another needs_action
  // response must terminate the probe so its owner can publish a typed failure;
  // retaining it like a task question would leave "Figma: check" running for
  // the full abandoned-question timeout.
  if (s.runtimeOnly) {
    s.awaitingTurn = false;
    s.askedThisTurn = false;
    pushEvent(s, 'needs_action', text);
    maybeAutoClose(s);
    return;
  }
  if (s.noQuestions) {
    s.askedThisTurn = true;
    s.answerFence = captureTaskTurnFence(s);
    s.prepAutoDefaultCount = (Number.isSafeInteger(s.prepAutoDefaultCount) ? s.prepAutoDefaultCount : 0) + 1;
    if (!s.answerFence || !s.answerFence.ok || s.prepAutoDefaultCount > PREP_AUTO_DEFAULT_LIMIT ||
        !enqueueTerminalInput(s.key, PREP_AUTO_DEFAULT_PROMPT)) {
      s.askedThisTurn = false;
      s.autoDefaultPending = false;
      pushEvent(s, 'error', '', {
        code: s.prepAutoDefaultCount > PREP_AUTO_DEFAULT_LIMIT
          ? 'prep-no-questions-convergence-failed'
          : 'prep-no-questions-continuation-refused'
      });
      return;
    }
    // Keep the internal question fence only long enough to deliver the fixed
    // continuation. Public state masks it, and the model's question text is
    // never persisted or streamed as a needs_action event.
    s.autoDefaultPending = true;
    scheduleTerminalInputDrain(s.key, 0);
    return;
  }
  s.askedThisTurn = true;
  s.answerFence = captureTaskTurnFence(s);
  pushEvent(s, 'needs_action', text);
  // Some Claude protocol generations emit needs_action just after result. The
  // result-side grace keeps this task child's lifetime lease intact; arm the
  // abandoned-question timeout here as well so late ordering cannot create an
  // immortal paused session.
  if (s.turnResultSeen && s.key !== 'setup') scheduleAwaitingIdleClose(s);
}

function summarizeTool(c) {
  try {
    var inp = c.input || {};
    if (inp.file_path) return String(inp.file_path);
    if (inp.command) return String(inp.command).slice(0, 120);
    if (inp.description) return String(inp.description).slice(0, 120);
    if (inp.prompt) return String(inp.prompt).slice(0, 120);
  } catch (e) {}
  return '';
}

function handleRaw(s, o) {
  if (o.session_id && !s.sessionId) s.sessionId = o.session_id;
  var t = o.type;
  if (t === 'system' && o.subtype === 'init') {
    pushEvent(s, 'system', '', { code: 'session-ready' });
  } else if (t === 'assistant' && o.message && Array.isArray(o.message.content)) {
    for (var i = 0; i < o.message.content.length; i++) {
      var c = o.message.content[i];
      if (c.type === 'text' && c.text && c.text.trim()) pushEvent(s, 'assistant', c.text);
      else if (c.type === 'tool_use') pushEvent(s, 'tool', summarizeTool(c), { tool: c.name || 'tool' });
    }
  } else if (t === 'system' && o.subtype === 'post_turn_summary') {
    // A post_turn_summary carrying needs_action means Claude is asking — pin the
    // session so the turn's pending auto-close aborts (maybeAutoClose re-checks
    // askedThisTurn after a grace window). post_turn_summary is OPTIONAL — most
    // turns never emit one, so auto-close must NOT depend on having seen it.
    if (o.needs_action) markNeedsAction(s, o.needs_action);
  } else if (t === 'system' && o.needs_action) {
    // Defensive: a needs_action emitted as a standalone system event (not wrapped
    // in post_turn_summary) must still pin the session so it isn't auto-closed.
    markNeedsAction(s, o.needs_action);
  } else if (t === 'result') {
    s.awaitingTurn = false;
    s.turnResultSeen = true;
    if (s.turnPublicationId && turnPublicationLifecycle) {
      var publicationId = s.turnPublicationId;
      var publication;
      s.turnPublicationId = null;
      try {
        publication = turnPublicationLifecycle.markResult(publicationId,
          o.is_error !== true && o.subtype !== 'error');
      } catch (publicationError) {
        publication = {
          ready: false,
          error: typeof turnPublicationLifecycle.publicationErrorCode === 'function'
            ? turnPublicationLifecycle.publicationErrorCode(publicationError, 'task-publication-validation-failed')
            : 'task-publication-validation-failed'
        };
      }
      if (publication && publication.ready) s.readyPublicationId = publicationId;
      if (publication && publication.error) {
        pushEvent(s, 'error', '', { code: 'design-publication-failed', reason: publication.error });
      }
    }
    // A task may finish a turn without emitting needs_action while deliberately
    // retaining its canonical lock (for example, a BLOCKED escalation). The
    // terminal documents that state as "paused" and offers a continuation
    // input. Freeze the exact post-turn task/INDEX snapshot now so a later
    // manual continuation cannot silently adopt edits made after the user saw
    // the result. sendOrResume revalidates it again immediately before stdin.
    s.idleContinuationFence = captureTaskIdleContinuationFence(s);
    // A task lease is bound to the exact spawned Claude child and its immutable
    // environment capability, so it lives for that child generation—not for a
    // single prompt. Releasing here and reacquiring on a warm answer/queued
    // continuation would give the live child a stale lease ID/delegation token.
    // Generic/setup sessions do not expose task delegation and use per-turn
    // leases.
    if (!s.canceled && !s.writerTerminationPending) {
      // write() may have returned while its completion callback (including a
      // possible EPIPE) is still pending. A racing result cannot withdraw the
      // lease until stdin confirms that this turn was actually accepted.
      if (s.stdinWritePending) s.resultAwaitingStdinSettlement = true;
      else if (s.key.indexOf('task:') !== 0) releaseTurnLease(s);
    }
    dispatchReadyPublication(s);
    pushEvent(s, 'result', typeof o.result === 'string' ? o.result : '');
    writeSidecar(s);
    // A task turn that completed → behave like `claude -p` and end the session.
    // maybeAutoClose waits a short grace before acting, so a needs_action that
    // arrives just after `result` (protocol ordering) still aborts the close.
    // "setup" persists across wizard steps and is exempt.
    if (s.key !== 'setup') {
      maybeAutoClose(s);
      // If the turn ended by asking the user (askedThisTurn), maybeAutoClose did
      // NOT close — the session stays alive so the answer can continue the same
      // run. Arm an idle timer so an ABANDONED awaiting session still frees its
      // slot instead of pinning a runner slot until restart.
      if (s.askedThisTurn) scheduleAwaitingIdleClose(s);
    }
    // A free-text submission may have arrived while this turn was in flight.
    // Drain only after the result fence above is frozen; the zero-delay timer
    // keeps prompt delivery out of the stdout parser's current call stack.
    scheduleTerminalInputDrain(s.key, 0);
  } else if (t === 'rate_limit_event' && o.rate_limit_info) {
    if (o.rate_limit_info.status && o.rate_limit_info.status !== 'allowed') {
      pushEvent(s, 'rate', '', { code: 'session-rate-limited' });
    }
  }
}

function onStdout(s, d) {
  // Decode through a per-session StringDecoder so a multibyte UTF-8 char split
  // across two stdout chunks is reassembled, not turned into replacement chars
  // (matters for non-ASCII output — e.g. Cyrillic assistant text). Falls back to
  // toString() defensively if a session predates the decoder field.
  s.buf += s.decoder ? s.decoder.write(d) : d.toString();
  // A newline-less runaway line would grow s.buf without bound (only a '\n' trims
  // it below). Cap it: keep the tail and note the truncation once per episode (the
  // flag resets when a line finally completes), so one bad line can't OOM the server.
  if (s.buf.indexOf('\n') < 0 && s.buf.length > BUF_CAP) {
    s.buf = s.buf.slice(-BUF_CAP);
    if (!s.bufTruncated) {
      s.bufTruncated = true;
      pushEvent(s, 'error', '', { code: 'session-output-truncated' });
    }
  }
  var i;
  while ((i = s.buf.indexOf('\n')) >= 0) {
    var ln = s.buf.slice(0, i);
    s.buf = s.buf.slice(i + 1);
    s.bufTruncated = false;   // a line completed — the runaway (if any) resolved; re-arm the one-shot notice
    if (!ln.trim()) continue;
    var o;
    try { o = JSON.parse(ln); } catch (e) { continue; }   // truncated/malformed JSON — skip (complete lines only reach here)
    handleRaw(s, o);
  }
}

// ---------------------------------------------------------------------------
// Run-gate: a proven visual task in a figmaEnabled product whose local
// screenshot-gate net (pre-commit verify-done via core.hooksPath) is unwired
// must not start. Non-visual tasks do not depend on this net and are
// deliberately not held. The finalizer remains the authoritative full visual
// check; this early gate is defense in depth against a hand-moved uncompared UI
// task. The server never mutates git configuration.
//
// Gated at this layer (not only runner.tick) because every site-driven task
// execution converges here: the runner's start at tick(), its warm-idle
// send() reuse, and the direct POST /api/session/start path.
//
// figmaEnabled is read from orchestrator/project-config.md (the product flag
// doctor.mjs gates on) — null/placeholder config (template pre-bootstrap) or
// figmaEnabled:false → gate off. Escape hatch for products that deliberately
// manage their own hooks (install-skills.sh --no-hooks): FIGMA_WIRING_GATE=0 —
// the operator then owns chaining verify-done into their own pre-commit.
// Returns null when runs are allowed, else the human-readable refusal.
function screenshotGateApplies(stem, dependencies) {
  // A missing stem means "report project-level wiring health" (the Skills
  // status pill), not an executable task decision.
  if (!stem) return true;
  dependencies = dependencies || {};
  var source = dependencies.taskSource || taskSourceMod;
  var parser = dependencies.designParser || designParser;
  if (!source || typeof source.safeTaskStem !== 'function' ||
      !source.safeTaskStem(stem)) return true;

  // Task-state admission remains the authority for an unavailable/changing
  // task. This classifier is defense in depth for screenshot applicability,
  // so an observational miss must not turn a non-visual Run into a false
  // global block; the fresh admission fence and finalizer still fail closed.
  var index;
  try { index = source.readIndex(); } catch (error) { return false; }
  if (!index || !Array.isArray(index.rows)) return false;
  var entry = index.rows.find(function (item) {
    return item && item.row && item.row.stem === stem;
  });
  if (!entry || entry.column !== 'todo') return false;
  var task;
  try { task = source.readTask(entry.column, stem); } catch (error2) { return false; }
  if (!task || typeof task.text !== 'string') return false;

  try {
    if (parser.hasPullableDesign(task.text)) return true;
    var undeclaredUi = parser.uiTaskWithoutDesign(task.text, {
      stem: stem,
      // The finalizer supplies the full component inventory. At run start the
      // strong in-task signals are enough to avoid globally holding unrelated
      // backend/data work; completion repeats the authoritative full check.
      inventory: []
    });
    return !!(undeclaredUi && undeclaredUi.level === 'block');
  } catch (error3) {
    return false;
  }
}

function runGateError(stem) {
  // The net policy itself lives in git.js, because the canonical commit asks
  // the same question and the two answers must never disagree. Only the
  // stem scoping is this caller's: a non-visual Run is not blocked by it.
  var issue = gitMod.enforcementNetIssue();
  if (!issue) return null;
  if (stem && !screenshotGateApplies(stem)) return null;
  return issue + ' Then Run again.';
}

function writerKindFor(key, stem) {
  return stem && key.indexOf('task:') === 0
    ? 'task-session' : 'workspace-session';
}
function writerLeaseKeyFor(key) {
  if (key === 'figma:shipdriftsweep') return 'figma:ship-drift-artifacts';
  return key;
}
function writerLeaseRequiresSoleWriter(key) {
  return key === 'figma:shipdriftsweep';
}
function releaseTurnLease(s) {
  if (!s || !s.writerLease) return true;
  var handle = s.writerLease;
  s.writerLease = null;
  var released = finalizationsMod.endMutation(handle);
  if (!released) s.writerLease = handle;
  return released;
}
// Pipeline improvement 01, Phase 3: when a worktree-bound run child exits,
// seal whatever it produced into a candidate (§9.5). Sealing is the ONLY way
// a receipt exists, and it must not depend on the child remembering to ask:
// the manager owns the tree, so the manager records what is in it. Refusals
// are logged and leave the generation exactly as it was — the run can be
// re-driven, and nothing is ever discarded.
function sealExecutionCandidate(s) {
  if (!s || !s.executionContext || s.action !== 'run' || s.canceled || s.candidateSealScheduled) return;
  s.candidateSealScheduled = true;
  var worktreeId = s.executionContext.worktreeId;
  setImmediate(function () {
    var sealed;
    try { sealed = require('./worktree-manager').seal({ worktreeId: worktreeId }); }
    catch (error) { sealed = { ok: false, code: 'SEAL_UNAVAILABLE', message: error && error.message }; }
    if (sealed && sealed.ok) {
      console.log('[sessions] sealed candidate for ' + (s.stem || worktreeId) + ': ' +
        sealed.entries + ' path(s), commit ' + String(sealed.candidateCommit).slice(0, 12));
    } else if (sealed && sealed.code === 'SEAL_EMPTY_CANDIDATE') {
      console.log('[sessions] no product changes to seal for ' + (s.stem || worktreeId) + '.');
    } else {
      console.warn('[sessions] candidate sealing refused for ' + (s.stem || worktreeId) + ': ' +
        (sealed && sealed.code) + (sealed && sealed.message ? ' — ' + sealed.message : ''));
    }
  });
}

function signalSessionTree(child, signal) {
  if (!child || !child.pid) return false;
  if (process.platform !== 'win32') {
    try { process.kill(-child.pid, signal); return true; }
    catch (e) { try { child.kill(signal); return true; } catch (e2) { return false; } }
  }
  try { child.kill(signal); return true; } catch (e3) { return false; }
}
function sessionTreeGone(child) {
  if (!child || !child.pid) return true;
  if (process.platform === 'win32') return false;
  try { process.kill(-child.pid, 0); return false; }
  catch (e) { return !!(e && e.code === 'ESRCH'); }
}
function retainTurnLease(s, reason) {
  if (!s || !s.writerLease) return;
  finalizationsMod.retainMutation(s.writerLease, reason);
}
function settlePromptDelivery(s, delivered, error) {
  if (!s || typeof s.promptSettlement !== 'function') return;
  var callback = s.promptSettlement;
  s.promptSettlement = null;
  try { callback(delivered === true, error || null); }
  catch (callbackError) { console.error('[sessions] prompt settlement callback failed:', callbackError && callbackError.message || callbackError); }
}
function handleStdinFailure(s, error) {
  if (!s || s.stdinFailureHandled) return;
  abortTurnPublication(s);
  settlePromptDelivery(s, false, error);
  s.stdinFailureHandled = true;
  s.writerTerminationPending = true;
  s.stdinWritePending = false;
  s.resultAwaitingStdinSettlement = false;
  s.closing = true;
  s.awaitingTurn = false;
  s.askedThisTurn = false;
  if (s.writerLease) retainTurnLease(s, 'Claude session stdin failed; process-tree death is pending proof');
  if (sessions[s.key] === s) {
    pushEvent(s, 'error', '', { code: 'session-input-failed' });
    writeSidecar(s);
  }
  var child = s.child;
  if (!child || !child.pid) {
    // No PID means no spawned process tree exists. This is the only stdin
    // failure path where immediate withdrawal is a proof, not an assumption.
    releaseTurnLease(s);
    return;
  }
  signalSessionTree(child, 'SIGTERM');
  // Bind escalation to the exact old child, not sessions[key]: a replacement
  // session must never cancel the cleanup of the writer that actually failed.
  if (s._stdinKillTimer == null) {
    s._stdinKillTimer = setTimeout(function () {
      s._stdinKillTimer = null;
      signalSessionTree(child, 'SIGKILL');
    }, STDIN_FAILURE_KILL_GRACE_MS);
    if (typeof s._stdinKillTimer.unref === 'function') s._stdinKillTimer.unref();
  }
}
function settleTurnLeaseAfterClose(s) {
  if (!s || !s.writerLease || s.writerLeaseSettling) return;
  s.writerLeaseSettling = true;
  if (process.platform === 'win32') {
    retainTurnLease(s, 'Claude session parent closed on Windows without a Job/process-tree drain proof');
    return;
  }
  var deadline = Date.now() + TURN_LEASE_FAST_DEADLINE_MS;
  var retainedLiveTree = false;
  function schedule(delay) {
    var timer = setTimeout(probe, delay);
    s.writerLeaseReaperTimer = timer;
    if (typeof timer.unref === 'function') timer.unref();
  }
  function probe() {
    s.writerLeaseReaperTimer = null;
    if (!s.writerLease) { s.writerLeaseSettling = false; return; }
    if (sessionTreeGone(s.child)) {
      if (s._stdinKillTimer != null) { clearTimeout(s._stdinKillTimer); s._stdinKillTimer = null; }
      if (releaseTurnLease(s)) {
        s.writerLeaseSettling = false;
        // Parent exit/close alone is not a tree-death proof: a detached child
        // may have closed inherited stdio and keep writing the candidate. Seal
        // only after the exact process group is gone and its writer lease has
        // been withdrawn successfully.
        sealExecutionCandidate(s);
        dispatchReadyPublication(s);
        return;
      }
      // A failed unlink/ownership check is not terminal. Retain fail-closed and
      // retry at the same low frequency until release is actually proven.
      retainTurnLease(s, 'writer process tree was gone, but lease release could not be proven');
      schedule(TURN_LEASE_REAPER_MS);
      return;
    }
    signalSessionTree(s.child, 'SIGKILL');
    if (Date.now() >= deadline) {
      if (!retainedLiveTree) {
        retainedLiveTree = true;
        retainTurnLease(s, 'Claude session process group remained alive after SIGKILL verification; background reaper is active');
      }
      schedule(TURN_LEASE_REAPER_MS);
      return;
    }
    schedule(TURN_LEASE_FAST_PROBE_MS);
  }
  probe();
}
function retireUnattachedLease(child, handle) {
  var holder = { child: child, writerLease: handle, writerLeaseSettling: false };
  if (!child) { releaseTurnLease(holder); return; }
  // spawn() reports ENOENT and similar failures asynchronously. This helper
  // returns before the normal session handlers are installed, so consume that
  // ChildProcess error here; with no PID there is provably no process tree and
  // the just-published lease can be withdrawn immediately.
  child.once('error', function () {});
  if (!child.pid) { releaseTurnLease(holder); return; }
  finalizationsMod.retainMutation(handle, 'prompted Claude child could not be attached to its lease; process-tree death is pending proof');
  signalSessionTree(child, 'SIGTERM');
  child.once('close', function () { settleTurnLeaseAfterClose(holder); });
  var timer = setTimeout(function () { signalSessionTree(child, 'SIGKILL'); }, 1000);
  if (typeof timer.unref === 'function') timer.unref();
}

// Start (or restart) the session for `key`. meta =
// { stem?, action?, prompt?, beforePrompt?, runtimeOnly?, conversationOnly? }.
// A non-empty meta.prompt is sent as the first turn. Reusing a still-running key
// is a no-op (returns its status) — the caller should open the terminal instead.
function start(key, meta) {
  meta = meta || {};
  prune();
  if (!validSessionKey(key)) return { running: false, error: 'invalid-session-key' };
  var conversationOnly = meta.conversationOnly === true;
  if (!conversationOnly && finalizationsMod.mutationBlocked(null)) {
    return { running: false, error: 'finalization-active' };
  }
  var finalizationStem = meta.stem || (key.indexOf('task:') === 0 ? key.slice('task:'.length) : null);
  if (!conversationOnly && finalizationStem && finalizationsMod.mutationBlocked(finalizationStem)) {
    return { running: false, error: 'finalization-active' };
  }
  // Run-gate: refuse to spawn a `run` before any session state is touched. The
  // spawn-failure shape ({running:false, error}) is what every caller already
  // handles (runner restores the claim; /api/session/start returns the status).
  if (meta.action === 'run' && key.indexOf('task:') === 0) {
    var gateErr = runGateError(finalizationStem);
    if (gateErr) return { running: false, error: 'figma-net-unwired' };
  }
  var existing = sessions[key];
  if (existing && existing.running && !existing.closing) return statusOf(key);
  if (existing && existing.childAlive) {
    return { running: false, error: 'writer-termination-pending' };
  }
  // A canceled/failed predecessor can remain in memory while its detached
  // process group is being reaped. Its durable unverified lease is the proof
  // that a writer may still exist; never overlap a replacement session for the
  // same key during that window.
  if (existing && existing.writerLease) {
    return { running: false, error: 'writer-termination-pending' };
  }
  var initialPrompt = meta.prompt && String(meta.prompt).trim() ? String(meta.prompt) : '';
  if (conversationOnly &&
      (!initialPrompt || meta.runtimeOnly === true || typeof meta.beforePrompt === 'function' ||
       (key.indexOf('task:') !== 0 && key.indexOf('figma:') !== 0))) {
    return { running: false, error: 'conversation-only-contract-invalid' };
  }
  if (meta.runtimeOnly === true &&
      (!initialPrompt || !runtimeOnlyInitialPromptAllowed(key, meta))) {
    return { running: false, error: 'runtime-only-contract-invalid' };
  }
  if (!ensureRunsDir()) {
    return { running: false, error: 'session-runtime-unsafe' };
  }

  // Pipeline improvement 01, Phase 2 — THE cutover assert: a task `run` turn
  // NEVER executes in the shared control root. The runner provisions a
  // manager-verified opaque generation before start(); anything else (HTTP
  // callers, stale flows) is refused fail-closed and the queued request
  // stays recoverable. Non-run actions must NOT carry a context.
  var executionContext = null;
  var opaqueExecutionContext = meta && meta.executionContext;
  // Applies to fresh starts AND full-authority resumes alike: any mutating
  // run child needs a verified worktree (§9.3 allows resume only under exact
  // identity, which only the runner can supply).
  var isTaskRunStart = key.indexOf('task:') === 0 && meta.action === 'run' &&
    !conversationOnly && meta.runtimeOnly !== true;
  if (isTaskRunStart && !opaqueExecutionContext) {
    return { running: false, error: 'execution-context-required' };
  }
  if (opaqueExecutionContext && !isTaskRunStart) {
    return { running: false, error: 'execution-context-not-allowed' };
  }
  if (opaqueExecutionContext) {
    if (typeof opaqueExecutionContext !== 'object' || Array.isArray(opaqueExecutionContext) ||
        Object.keys(opaqueExecutionContext).sort().join('\0') !== 'runId\0worktreeId' ||
        typeof opaqueExecutionContext.worktreeId !== 'string' ||
        typeof opaqueExecutionContext.runId !== 'string' ||
        !EXECUTION_RUN_ID_RE.test(opaqueExecutionContext.runId)) {
      return { running: false, error: 'execution-context-invalid' };
    }
    var resolvedExecution = require('./worktree-manager').sessionExecutionContext({
      worktreeId: opaqueExecutionContext.worktreeId,
      runId: opaqueExecutionContext.runId,
      stem: finalizationStem,
      sourceRevision: meta.sourceRevision
    });
    if (!resolvedExecution || !resolvedExecution.ok || !resolvedExecution.context) {
      return { running: false, error: 'execution-context-invalid' };
    }
    executionContext = resolvedExecution.context;
  }

  var writerSessionId = finalizationsMod.createWriterSessionId();
  var initialWriterLease = null;
  if (initialPrompt && meta.runtimeOnly !== true && !conversationOnly) {
    // Publish the writer lease BEFORE the mandatory finalization re-check. A
    // finalizer that wins its mutex after this publication sees the lease and
    // refuses; if it won first, beginMutation withdraws the lease and no Claude
    // process is spawned.
    var leaseStart = finalizationsMod.beginMutation({
      kind: writerKindFor(key, finalizationStem), stem: finalizationStem,
      sessionId: writerSessionId, key: writerLeaseKeyFor(key),
      requireSoleWriter: writerLeaseRequiresSoleWriter(key)
    });
    if (!leaseStart.ok) return {
      running: false,
      error: leaseStart.error === 'finalization-active'
        ? 'finalization-active' : 'workspace-writer-lease-refused'
    };
    initialWriterLease = leaseStart.handle;
  }

  // Resume mode: continue a PRIOR (now-ended) conversation with full context via
  // `claude --resume <sessionId>` instead of a fresh process. Triggered by the
  // terminal's free-text send to an ended session (sendOrResume), so "the CLI
  // finished" is never a dead end. We reuse the prior startedAt + seq counter and
  // APPEND to the transcript, so the client's restart-detection (keyed on
  // startedAt) does NOT wipe the rendered scrollback — the continued turn just
  // streams on. A fresh "Run again" (no resume) still rotates startedAt + truncates.
  var resumeId = (meta.resume && typeof meta.resume === 'string') ? meta.resume : null;
  var preservePrior = !!resumeId || (conversationOnly && meta.preserveTranscript === true);
  var prior = preservePrior ? statusOf(key) : null;
  var startSeq  = (preservePrior && prior && typeof prior.nextSeq === 'number') ? prior.nextSeq : 0;
  var startedAt = (preservePrior && prior && prior.startedAt) ? prior.startedAt : new Date().toISOString();
  var preload   = preservePrior ? readPersistedEvents(key, 0) : [];

  // Establish the transcript descriptor before a child is spawned. Fresh runs
  // atomically replace the prior path; resume runs copy the bounded, stable old
  // bytes into a new inode first. Subsequent writes use this descriptor, so a
  // later ancestor-path swap cannot redirect model output.
  var logFd = fileGuards.openAtomicReplaceRegularFile(
    PROJECT_ROOT, RUNS_DIR, eventsPathFor(key), {
      create: true,
      mode: 0o600,
      // Preserve until spawn+lease attach succeeds. Fresh generations truncate
      // the descriptor only after that point, so a spawn failure cannot erase
      // the prior transcript.
      preserveExisting: true,
      maxExistingBytes: SESSION_EVENTS_MAX_BYTES,
      maxBytes: SESSION_EVENTS_MAX_BYTES
    }
  );
  if (logFd === null) {
    if (initialWriterLease) finalizationsMod.endMutation(initialWriterLease);
    return { running: false, error: 'session-runtime-unsafe' };
  }
  var logBytes;
  try {
    logBytes = fs.fstatSync(logFd).size;
    if (!Number.isSafeInteger(logBytes) || logBytes < 0 || logBytes > SESSION_EVENTS_MAX_BYTES) {
      throw new Error('transcript size is invalid');
    }
  } catch (logStatError) {
    try { fs.closeSync(logFd); } catch (closeLogStatError) {}
    if (initialWriterLease) finalizationsMod.endMutation(initialWriterLease);
    return { running: false, error: 'session-runtime-unsafe' };
  }

  var args = ['-p', '--input-format', 'stream-json', '--output-format', 'stream-json', '--verbose'];
  if (conversationOnly) {
    // A stale/ended task or typed Figma action has no live ownership proof.
    // Resume its conversation with read-only repo access, no project hooks,
    // plugins, skills or MCP tools. This makes the terminal responsive without
    // turning persisted session metadata into workspace mutation authority.
    args = args.concat(['--safe-mode', '--tools', 'Read,Grep,Glob', '--permission-mode', 'dontAsk',
      '--disable-slash-commands']);
  } else {
    args.push('--dangerously-skip-permissions');
  }
  if (resumeId) args = ['--resume', resumeId].concat(args);
  var child;
	  try {
	    var sessionEnv = childEnv();
	    // Never inherit a stale parent capability into a new generation. The
	    // pending site lease returns one plaintext delegation token only in its
	    // in-memory handle; the durable row stores its hash. Copy that exact pair
	    // solely into the child being attached to this lease.
	    delete sessionEnv.ORCHESTRATOR_WRITER_SESSION_ID;
	    delete sessionEnv.ORCHESTRATOR_WRITER_STEM;
	    delete sessionEnv.ORCHESTRATOR_WRITER_LEASE_ID;
	    delete sessionEnv.ORCHESTRATOR_WRITER_DELEGATION_TOKEN;
	    delete sessionEnv.ORCHESTRATOR_TASK_PREP_NO_QUESTIONS;
	    if (!conversationOnly) {
	      sessionEnv.ORCHESTRATOR_WRITER_SESSION_ID = writerSessionId;
	      if (finalizationStem) sessionEnv.ORCHESTRATOR_WRITER_STEM = finalizationStem;
	      if (meta.noQuestions === true) sessionEnv.ORCHESTRATOR_TASK_PREP_NO_QUESTIONS = '1';
	    }
	    if (initialWriterLease) {
	      if (typeof initialWriterLease.leaseId !== 'string' ||
	          typeof initialWriterLease.delegationToken !== 'string' || !initialWriterLease.delegationToken) {
	        throw new Error('writer delegation capability is unavailable');
	      }
	      sessionEnv.ORCHESTRATOR_WRITER_LEASE_ID = initialWriterLease.leaseId;
	      sessionEnv.ORCHESTRATOR_WRITER_DELEGATION_TOKEN = initialWriterLease.delegationToken;
	    }
	    if (executionContext) {
	      // cwd = the isolated worktree; ONE env var re-anchors every
	      // control-plane helper (locks, leases, journal, checkpoints, task
	      // corpus) at the control root — the worktree's own script copies then
	      // write control state, never a second control plane inside the
	      // checkout. The manifest/snapshot pins are read-only inputs.
	      sessionEnv.ORCHESTRATOR_PROJECT_ROOT = PROJECT_ROOT;
	      // Helpers invoked from the worktree resolve their roots from their OWN
	      // location (the checkout copy) unless their exact knob is set. Pin every
	      // control-plane root explicitly so lock/lease/journal/checkpoint/task
	      // state always lands in the control plane, never in a second one inside
	      // the checkout.
	      sessionEnv.ORCHESTRATOR_TASKS_DIR = paths.TASKS_DIR;
	      sessionEnv.ORCHESTRATOR_CACHE_DIR = paths.RUNTIME_CACHE_DIR;
	      sessionEnv.ORCHESTRATOR_LOCKS_DIR = paths.LOCKS_DIR;
	      sessionEnv.ORCHESTRATOR_TRANSITIONS_DIR = paths.TRANSITIONS_DIR;
	      sessionEnv.ORCHESTRATOR_JOURNAL_DIR = paths.JOURNAL_DIR;
	      sessionEnv.ORCHESTRATOR_CHECKPOINTS_DIR = paths.CHECKPOINTS_DIR;
	      sessionEnv.ORCHESTRATOR_FINALIZATIONS_DIR = paths.FINALIZATIONS_DIR;
	      sessionEnv.ORCHESTRATOR_WRITER_LEASES_DIR = paths.WRITER_LEASES_DIR;
	      sessionEnv.ORCHESTRATOR_WRITER_AUTHORITY_ROOT = paths.WRITER_AUTHORITY_ROOT;
	      sessionEnv.ORCHESTRATOR_TASK_CREATIONS_DIR = paths.TASK_CREATIONS_DIR;
	      sessionEnv.ORCHESTRATOR_TASK_CREATIONS_AUTHORITY_ROOT = paths.TASK_CREATIONS_AUTHORITY_ROOT;
	      sessionEnv.ORCHESTRATOR_TASK_EDITS_DIR = paths.TASK_EDITS_DIR;
	      sessionEnv.ORCHESTRATOR_TASK_EDITS_AUTHORITY_ROOT = paths.TASK_EDITS_AUTHORITY_ROOT;
	      sessionEnv.ORCHESTRATOR_TASK_INTAKE_DIR = paths.TASK_INTAKE_DIR;
	      sessionEnv.ORCHESTRATOR_TASK_ACTION_RECEIPTS_DIR = paths.TASK_ACTION_RECEIPTS_DIR;
	      sessionEnv.ORCHESTRATOR_TEST_CERTIFICATION_DIR = paths.TEST_CERTIFICATION_DIR;
	      sessionEnv.ORCHESTRATOR_RUNS_DIR = paths.RUNS_DIR;
	      sessionEnv.ORCHESTRATOR_EXECUTION_MANIFEST = executionContext.manifestFile;
	      sessionEnv.ORCHESTRATOR_TASK_SNAPSHOT_FILE = executionContext.taskSnapshotFile;
	      sessionEnv.ORCHESTRATOR_TASK_SNAPSHOT_HASH = executionContext.taskSnapshotHash;
	      sessionEnv.ORCHESTRATOR_WORKTREE_ID = executionContext.worktreeId;
	      // The task lock is part of this exact worktree generation. Without the
	      // manager-issued run id the child would mint an unrelated lock id, and
	      // finalization could never prove that the lock owns this candidate.
	      sessionEnv.ORCHESTRATOR_RUN_ID = executionContext.runId;
	      sessionEnv.ORCHESTRATOR_EXECUTION_ROOT = executionContext.executionRoot;
	    }
	    child = cp.spawn('claude', args,
	      { cwd: executionContext ? executionContext.executionRoot : PROJECT_ROOT,
	        stdio: ['pipe', 'pipe', 'pipe'], env: sessionEnv, detached: process.platform !== 'win32' });
	  } catch (e) {
    try { fs.closeSync(logFd); } catch (closeLogError) {}
    if (initialWriterLease) finalizationsMod.endMutation(initialWriterLease);
    return { running: false, error: 'session-spawn-failed' };
  }
  if (initialWriterLease) {
    var attached = finalizationsMod.attachMutationChild(initialWriterLease, child.pid);
    if (!attached.ok) {
      try { fs.closeSync(logFd); } catch (closeLogError2) {}
      retireUnattachedLease(child, initialWriterLease);
      return { running: false, error: 'workspace-writer-lease-attach-failed' };
    }
  }
  if (!preservePrior) {
    try {
      fs.ftruncateSync(logFd, 0);
      fs.fsyncSync(logFd);
      logBytes = 0;
    } catch (truncateError) {
      try { fs.closeSync(logFd); } catch (closeTruncateError) {}
      if (initialWriterLease) retireUnattachedLease(child, initialWriterLease);
      else {
        signalSessionTree(child, 'SIGTERM');
        var truncateKill = setTimeout(function () { signalSessionTree(child, 'SIGKILL'); }, 1000);
        if (typeof truncateKill.unref === 'function') truncateKill.unref();
      }
      return { running: false, error: 'session-runtime-unsafe' };
    }
  }

  var s = {
    key: key, stem: meta.stem || null, action: meta.action || null,
    executionContext: executionContext,
    dedupKey: meta.dedupKey || null, dedupReport: meta.dedupReport || null,
    expectedState: meta.expectedState || null, sourceRevision: meta.sourceRevision || null,
    child: child, sessionId: null, running: true, awaitingTurn: false, canceled: false,
    runtimeOnly: meta.runtimeOnly === true,
    askedThisTurn: false,   // set when Claude emits needs_action; gates auto-close
    answerFence: null, idleContinuationFence: null, turnGeneration: 0,
    closing: false,         // auto-close committed (stdin ended) — released from the DEDUP before the child's exit lands
    childAlive: true,       // cleared ONLY on child exit/error — the CAPACITY gate (a closing child stays alive ~6s+ and still edits the tree)
    _killTimer: null,       // setTimeout handle from maybeAutoClose fallback; cleared in cancel/exit
    idleTimer: null,        // awaiting-idle auto-close handle; cleared on answer (send) / cancel / exit
    writerTerminationPending: false, writerLeaseSettling: false, writerLeaseReaperTimer: null,
    candidateSealScheduled: false,
    stdinFailureHandled: false, stdinWritePending: false, resultAwaitingStdinSettlement: false,
    turnResultSeen: false,
    turnPublicationId: null, readyPublicationId: null,
    promptSettlement: null,
    _stdinKillTimer: null,
    decoder: new StringDecoder('utf8'),   // reassembles multibyte UTF-8 split across stdout chunks
    startedAt: startedAt, endedAt: null, exitCode: null,
    events: preload, seq: startSeq, buf: '', bufTruncated: false,
    logFd: logFd, logBytes: logBytes, logTruncated: false,
    writerSessionId: writerSessionId, writerLease: initialWriterLease,
    conversationOnly: conversationOnly,
    noQuestions: meta.noQuestions === true,
    prepAutoDefaultCount: 0,
    autoDefaultPending: false
  };
  if (existing && existing.logFd != null) { try { fs.closeSync(existing.logFd); } catch (e) {} existing.logFd = null; }
  sessions[key] = s;
  pushEvent(s, 'system', '', { code: resumeId ? 'session-resuming' : 'session-starting' });
  writeSidecar(s);

  // Every handler is bound to THIS session: if a newer session replaces
  // sessions[key], a late event from this (now-dead) child is ignored — so an
  // old child's exit can never flip the new session's state off.
  child.stdout.on('data', function (d) { if (sessions[key] === s) onStdout(s, d); });
  child.stderr.on('data', function (d) {
    if (sessions[key] !== s) return;
    var str = d.toString().trim();
    if (str) pushEvent(s, 'stderr', '', { code: 'session-stderr-output' });
  });
  child.on('exit', function (code, signal) {
    settlePromptDelivery(s, false, new Error('Claude session exited before prompt delivery settled'));
    if (!s.turnResultSeen) abortTurnPublication(s);
    s.childAlive = false;     // child is truly gone now → frees its capacity slot (set here only; the cap counts a live-but-closing child)
    if (s.writerLease && (!s.turnResultSeen || s.awaitingTurn || s.stdinWritePending || s.writerTerminationPending)) {
      retainTurnLease(s, 'Claude session parent exited before its mutating turn produced a result; process-tree death is pending proof');
    }
    if (sessions[key] !== s) return;
    if (s.canceled) return;   // cancel() already set endedAt, closed logFd, and wrote the sidecar — don't duplicate
    s.closing = true;
    if (s._killTimer != null) { clearTimeout(s._killTimer); s._killTimer = null; }
    if (s.idleTimer != null) { clearTimeout(s.idleTimer); s.idleTimer = null; }
    if (s.buf && s.buf.trim()) {
      var finalBuf = s.buf; s.buf = '';
      var lines = finalBuf.split('\n');
      for (var li = 0; li < lines.length; li++) {
        if (!lines[li].trim()) continue;
        var fo; try { fo = JSON.parse(lines[li]); } catch (e) { continue; }
        handleRaw(s, fo);
      }
    }
    s.running = false; s.awaitingTurn = false; s.askedThisTurn = false;
    s.endedAt = new Date().toISOString();
    s.exitCode = ((code === null || code === undefined) ? null : code);
    pushEvent(s, 'exit', '', {
      code: 'session-ended',
      exitCode: ((code === null || code === undefined) ? null : code),
      signal: signal || null
    });
    if (s.logFd != null) { try { fs.closeSync(s.logFd); } catch (e) {} s.logFd = null; }
    writeSidecar(s);
  });
  child.on('error', function (e) {
    settlePromptDelivery(s, false, e);
    abortTurnPublication(s);
    s.childAlive = false;     // spawn/IO error → no live child → frees its capacity slot
    if (!s.child || !s.child.pid) releaseTurnLease(s);
    else {
      s.writerTerminationPending = true;
      retainTurnLease(s, 'Claude session errored with a live child; process-tree death is pending proof');
      signalSessionTree(s.child, 'SIGKILL');
    }
    if (sessions[key] !== s) return;
    if (s._killTimer != null) { clearTimeout(s._killTimer); s._killTimer = null; }
    if (s.idleTimer != null) { clearTimeout(s.idleTimer); s.idleTimer = null; }
    s.running = false; s.awaitingTurn = false; s.askedThisTurn = false; s.endedAt = new Date().toISOString();
    pushEvent(s, 'error', '', { code: 'session-process-failed' });
    if (s.logFd != null) { try { fs.closeSync(s.logFd); } catch (e2) {} s.logFd = null; }
    writeSidecar(s);
  });
  // `exit` proves only the Claude parent died. `close` also waits for inherited
  // stdio, then the POSIX process-group probe proves descendants are gone. On
  // Windows there is no equivalent for these interactive sessions, so an
  // abnormal turn is retained durably fail-closed instead of guessed safe.
  child.on('close', function () {
    settleTurnLeaseAfterClose(s);
    scheduleTerminalInputDrain(s.key, 0);
  });
  // Writable failures (notably an asynchronous EPIPE after write() returned)
  // are emitted on child.stdin, not on ChildProcess. Without this listener Node
  // treats them as uncaught errors and exits. The handler retains ownership,
  // terminates the exact process group and lets close + group proof settle it.
  child.stdin.on('error', function (error) { handleStdinFailure(s, error); });

  var initialTurnMeta = typeof meta.beforePrompt === 'function' ? {
    action: meta.action,
    executionContext: opaqueExecutionContext,
    noQuestions: meta.noQuestions === true,
    dedupKey: meta.dedupKey,
    dedupReport: meta.dedupReport,
    expectedState: meta.expectedState,
    sourceRevision: meta.sourceRevision,
    beforePrompt: meta.beforePrompt,
    onPromptSettled: meta.onPromptSettled
  } : null;
  var initialPromptLease = conversationOnly ? CONVERSATION_ONLY_INITIAL_PROMPT :
    (meta.runtimeOnly === true ? RUNTIME_ONLY_INITIAL_PROMPT : true);
  if (initialPrompt && !send(key, initialPrompt, initialTurnMeta, initialPromptLease)) {
    cancel(key);
    return { running: false, error: 'initial-prompt-refused' };
  }
  return statusOf(key);
}

// Send a step prompt OR a user answer into the live session as one turn.
// meta (optional) re-stamps the session's action/dedup fields when the runner
// reuses a warm (running-but-idle) session for a NEW queued action — so the
// board's action-scoped consumers see the turn that is actually executing.
function send(key, text, meta, initialPromptLease) {
  var s = sessions[key];
  if (!s || !s.running || !s.child || !s.child.stdin || !s.child.stdin.writable) return false;
  if (s.awaitingTurn) return false;   // one turn at a time — don't collide prompts
  if (s.stdinWritePending || s.resultAwaitingStdinSettlement) return false;
  var leaseAlreadyHeld = initialPromptLease === true;
  var runtimeOnlyInitialPrompt = initialPromptLease === RUNTIME_ONLY_INITIAL_PROMPT;
  var conversationOnlyTurn = s.conversationOnly === true ||
    initialPromptLease === CONVERSATION_ONLY_INITIAL_PROMPT;
  // The fourth argument is an internal start() contract, not a generic way to
  // bypass writer ownership. Reject every unknown value; exported send() calls
  // omit it and therefore retain the normal acquire-and-attach path.
  if (initialPromptLease !== undefined && !leaseAlreadyHeld && !runtimeOnlyInitialPrompt &&
      initialPromptLease !== CONVERSATION_ONLY_INITIAL_PROMPT) return false;
  if (conversationOnlyTurn && !s.conversationOnly) return false;
  // Run-gate for the warm-idle reuse path: the runner delivers a queued `run`
  // into an already-running task session without calling start(), so the gate
  // must fire here too. false → the runner requeues the claim, same as busy.
  if (meta && meta.action === 'run' && runGateError(meta.stem || s.stem)) return false;
  // Pipeline improvement 01, Phase 2 — the cutover assert applies to warm
  // delivery too: an idle session may take a queued `run` only when it is
  // ALREADY executing inside the exact worktree this run was provisioned for.
  // A run must never reach a control-root child, and a worktree-bound child
  // must never be handed control-plane work (its cwd/binding are wrong).
  if (meta && meta.action === 'run') {
    var deliveredOpaque = meta.executionContext || null;
    if (!s.executionContext || !deliveredOpaque || typeof deliveredOpaque !== 'object' ||
        Array.isArray(deliveredOpaque) ||
        Object.keys(deliveredOpaque).sort().join('\0') !== 'runId\0worktreeId') return false;
    var delivered = require('./worktree-manager').sessionExecutionContext({
      worktreeId: deliveredOpaque.worktreeId, runId: deliveredOpaque.runId,
      stem: meta.stem || s.stem, sourceRevision: meta.sourceRevision || s.sourceRevision
    });
    if (!delivered || !delivered.ok || !delivered.context ||
        s.executionContext.worktreeId !== delivered.context.worktreeId ||
        s.executionContext.runId !== delivered.context.runId ||
        s.executionContext.executionRoot !== delivered.context.executionRoot ||
        s.executionContext.manifestFile !== delivered.context.manifestFile ||
        s.executionContext.taskSnapshotHash !== delivered.context.taskSnapshotHash) return false;
  } else if (meta && typeof meta.action === 'string' && s.executionContext) {
    return false;
  }
  var reusedTaskLease = !leaseAlreadyHeld && !runtimeOnlyInitialPrompt && !conversationOnlyTurn &&
    s.key.indexOf('task:') === 0 && !!s.writerLease;
  var acquiredThisCall = false;
  if (!leaseAlreadyHeld && !runtimeOnlyInitialPrompt && !conversationOnlyTurn && !reusedTaskLease) {
    if (s.writerLease) return false;
    var leaseStart = finalizationsMod.beginMutation({
      kind: writerKindFor(s.key, s.stem), stem: s.stem,
      sessionId: s.writerSessionId, key: writerLeaseKeyFor(s.key),
      requireSoleWriter: writerLeaseRequiresSoleWriter(s.key)
    });
    if (!leaseStart.ok) return false;
    s.writerLease = leaseStart.handle;
    acquiredThisCall = true;
    var attached = finalizationsMod.attachMutationChild(s.writerLease, s.child.pid);
    if (!attached.ok) { releaseTurnLease(s); return false; }
  } else if (leaseAlreadyHeld && !s.writerLease) {
    return false;
  } else if ((runtimeOnlyInitialPrompt || conversationOnlyTurn) && s.writerLease) {
    // runtimeOnly is valid only for a genuinely lease-free initial generation.
    return false;
  }
  // Queue handoff hook: runner supplies a synchronous, read-only final fence
  // followed by exact per-stem reservation withdrawal. It runs only after the
  // writer lease is durable+attached and strictly before the user event or any
  // stdin byte. Refusal/throw withdraws this turn's lease and guarantees no
  // prompt was handed to Claude.
  if (meta && typeof meta.beforePrompt === 'function') {
    var handoffAccepted = false;
    try {
      handoffAccepted = meta.beforePrompt({
        key: s.key, stem: s.stem, action: meta.action || s.action,
        writerLease: s.writerLease
      }) === true;
    } catch (handoffError) { handoffAccepted = false; }
    if (!handoffAccepted) {
      // An initial prompt owns a just-created pre-spawn lease and a generic
      // per-turn prompt owns a lease acquired above; both must withdraw on a
      // pre-delivery refusal. A warm task continuation reuses its child-lifetime
      // lease, which predates this request and must remain exact for retries.
      if (leaseAlreadyHeld || acquiredThisCall) releaseTurnLease(s);
      return false;
    }
  }
  if (meta) {
    if (typeof meta.action === 'string') s.action = meta.action;
    if (typeof meta.noQuestions === 'boolean') s.noQuestions = meta.noQuestions;
    if (typeof meta.dedupKey === 'string') s.dedupKey = meta.dedupKey;
    if (typeof meta.dedupReport === 'string') s.dedupReport = meta.dedupReport;
    if (typeof meta.expectedState === 'string') s.expectedState = meta.expectedState;
    if (typeof meta.sourceRevision === 'string') s.sourceRevision = meta.sourceRevision;
  }
  if (turnPublicationLifecycle && !conversationOnlyTurn) {
    try {
      s.turnPublicationId = turnPublicationLifecycle.prepareTurn({
        key: s.key, stem: s.stem, action: s.action,
        screenTokenPlanId: meta && meta.screenTokenPlanId || null
      });
    } catch (publicationPrepareError) {
      if (leaseAlreadyHeld || acquiredThisCall) releaseTurnLease(s);
      return false;
    }
  }
  s.turnGeneration = (Number.isSafeInteger(s.turnGeneration) ? s.turnGeneration : 0) + 1;
  s.turnResultSeen = false;
  s.answerFence = null;
  s.idleContinuationFence = null;
  var msg = { type: 'user', message: { role: 'user', content: [{ type: 'text', text: String(text) }] } };
  try {
    s.promptSettlement = meta && typeof meta.onPromptSettled === 'function' ? meta.onPromptSettled : null;
    s.stdinWritePending = true;
    s.child.stdin.write(JSON.stringify(msg) + '\n', function (error) {
      if (error) { settlePromptDelivery(s, false, error); handleStdinFailure(s, error); return; }
      settlePromptDelivery(s, true, null);
      s.stdinWritePending = false;
      if (s.resultAwaitingStdinSettlement) {
        s.resultAwaitingStdinSettlement = false;
        if (!s.canceled && !s.writerTerminationPending && s.key.indexOf('task:') !== 0) releaseTurnLease(s);
        dispatchReadyPublication(s);
      }
    });
    // A custom/failed Writable may invoke its callback synchronously. Do not
    // overwrite the termination state that handler just established.
    if (s.stdinFailureHandled) return false;
    s.awaitingTurn = true;
    s.askedThisTurn = false;         // new turn — wait for this turn's needs_action, not a prior one
    s.autoDefaultPending = false;
    if (s.idleTimer != null) { clearTimeout(s.idleTimer); s.idleTimer = null; }   // answered — cancel the abandoned-awaiting close
    pushEvent(s, 'user', String(text));
    writeSidecar(s);
    return true;
  } catch (e) { abortTurnPublication(s); settlePromptDelivery(s, false, e); handleStdinFailure(s, e); return false; }
}

function terminalInputCount(key) {
  var queue = terminalInputQueues[key];
  return queue ? queue.length : 0;
}

function enqueueTerminalInput(key, text) {
  var queue = terminalInputQueues[key] || [];
  var chars = queue.reduce(function (sum, item) { return sum + item.length; }, 0);
  if (queue.length >= MAX_TERMINAL_INPUTS_PER_SESSION ||
      chars + text.length > MAX_TERMINAL_INPUT_CHARS_PER_SESSION) return false;
  queue.push(text);
  terminalInputQueues[key] = queue;
  return true;
}

function closeForConversationOnly(s) {
  if (!s || !s.running || s.closing) return;
  // A fresh task lock can mean the canonical pipeline is still performing
  // phase work after a result event. Never kill that owner merely to accelerate
  // a chat follow-up; the queued input waits for its normal safe close.
  if (s.key.indexOf('task:') === 0 && hasFreshLock(s)) {
    scheduleLockRecheck(s);
    return;
  }
  s.closing = true;
  if (s.idleTimer != null) { clearTimeout(s.idleTimer); s.idleTimer = null; }
  writeSidecar(s);
  try { if (s.child && s.child.stdin && s.child.stdin.writable) s.child.stdin.end(); } catch (e) {}
  if (s._killTimer == null) {
    s._killTimer = setTimeout(function () {
      if (sessions[s.key] !== s || !s.running || !s.child) return;
      signalSessionTree(s.child, 'SIGTERM');
      var force = setTimeout(function () {
        if (sessions[s.key] === s && s.running && s.child) signalSessionTree(s.child, 'SIGKILL');
      }, 2000);
      if (typeof force.unref === 'function') force.unref();
    }, 4000);
    if (typeof s._killTimer.unref === 'function') s._killTimer.unref();
  }
}

function startConversationOnly(key, text) {
  var prior = statusOf(key);
  var st = start(key, {
    stem: prior.stem || (key.indexOf('task:') === 0 ? key.slice('task:'.length) : null),
    // This continuation has no mutation authority and no execution binding.
    // Retaining a finished task's `run` action here makes the persisted v3
    // sidecar self-contradictory: every task run action requires the complete
    // worktree binding, while conversation-only continuations deliberately
    // carry none. Clear the action at the authority boundary instead of
    // misrepresenting a read-only follow-up as another task run.
    action: null,
    prompt: text,
    resume: prior.sessionId || null,
    preserveTranscript: true,
    conversationOnly: true
  });
  return {
    sent: !!(st && st.running && !st.error),
    resumed: !!prior.sessionId,
    status: st,
    error: st && st.error || null,
    // All transient predecessor states were handled before start(). A failure
    // here is an infrastructure/configuration refusal, so report it instead of
    // retaining an in-memory queue that can never make progress on its own.
    queueable: false
  };
}

// Try one free-text delivery without accepting it into the pending queue. A
// caller can distinguish a transient busy/closing boundary (`queueable`) from a
// live task/Figma generation that must first finish and reopen read-only
// (`transition`).
function deliverTerminalInput(key, text) {
  var s = sessions[key];
  if (s && s.running && !s.closing) {
    if (s.awaitingTurn || s.stdinWritePending || s.resultAwaitingStdinSettlement) {
      return { sent: false, resumed: false, busy: true, queueable: true };
    }
    if (s.conversationOnly) {
      var conversationSent = send(key, text);
      return { sent: conversationSent, resumed: false, queueable: !conversationSent };
    }
    if (key.indexOf('figma:') === 0) {
      return { sent: false, resumed: false, transition: true, queueable: true };
    }
    if (key.indexOf('task:') === 0) {
      if (!s.writerLease || (!s.askedThisTurn && !s.turnResultSeen)) {
        return { sent: false, resumed: false, transition: true, queueable: true };
      }
      var writerLease = s.writerLease;
      var answeringQuestion = !!s.askedThisTurn;
      var answerFence = answeringQuestion ? s.answerFence : s.idleContinuationFence;
      if (!answerFence || !answerFence.ok || answerFence.generation !== s.turnGeneration ||
          answerFence.action !== s.action || answerFence.expectedState !== s.expectedState ||
          answerFence.admittedSourceRevision !== s.sourceRevision) {
        return {
          sent: false, resumed: false, transition: true, queueable: true,
          error: answerFence && answerFence.reason || 'task-answer-snapshot-unavailable',
          lockReason: answerFence && answerFence.lockReason || null
        };
      }
      var ownership = null;
      var refusal = null;
      var sent = send(key, text, {
        beforePrompt: function () {
          var samePromptBoundary = answeringQuestion
            ? (s.askedThisTurn && s.answerFence === answerFence)
            : (!s.askedThisTurn && s.turnResultSeen && s.idleContinuationFence === answerFence);
          if (!samePromptBoundary || s.writerLease !== writerLease ||
              s.turnGeneration !== answerFence.generation) {
            refusal = 'task-answer-not-requested';
            return false;
          }
          ownership = locks.lockOwnedBySession(s.stem || key.slice('task:'.length), s.writerSessionId);
          if (ownership.owned !== true) {
            refusal = 'task-lock-owner-mismatch';
            return false;
          }
          var current;
          try { current = taskIntegrityMod.validateAction(s.action, s.stem || key.slice('task:'.length), 'runner'); }
          catch (error) {
            refusal = 'task-integrity-unavailable';
            return false;
          }
          if (!current || !taskIntegrityMod.admissionForAction(current, s.stem || key.slice('task:'.length)).ok ||
              current.observedState !== answerFence.observedState ||
              current.observedState !== s.expectedState) {
            refusal = 'task-answer-integrity-invalid';
            return false;
          }
          if (current.sourceRevision !== answerFence.sourceRevision) {
            refusal = 'task-answer-snapshot-changed';
            return false;
          }
          return true;
        }
      });
      return {
        sent: sent, resumed: false, transition: !sent, queueable: !sent,
        error: !sent ? (refusal || 'task-turn-refused') : null,
        lockReason: ownership && ownership.reason || null
      };
    }
    var genericSent = send(key, text);
    return { sent: genericSent, resumed: false, queueable: !genericSent };
  }
  if (s && (s.childAlive || s.writerLease || s.writerLeaseSettling)) {
    return { sent: false, resumed: false, busy: true, queueable: true };
  }
  if (key.indexOf('task:') === 0 || key.indexOf('figma:') === 0) {
    return startConversationOnly(key, text);
  }
  var prior = statusOf(key);   // memory-or-sidecar — recovers sessionId/stem/action after prune
  var st = start(key, {
    stem: prior.stem || null,
    action: prior.action || null,
    prompt: text,
    resume: prior.sessionId || null
  });
  return {
    sent: !!(st && st.running && !st.error), resumed: !!prior.sessionId, status: st,
    error: st && st.error || null, queueable: false
  };
}

function scheduleTerminalInputDrain(key, delay) {
  if (!terminalInputCount(key) || terminalInputDrainTimers[key]) return;
  var timer = setTimeout(function () {
    delete terminalInputDrainTimers[key];
    var queue = terminalInputQueues[key];
    if (!queue || !queue.length) return;
    var result = deliverTerminalInput(key, queue[0]);
    if (result.sent) {
      queue.shift();
      if (!queue.length) delete terminalInputQueues[key];
      scheduleTerminalInputDrain(key, 0);
      return;
    }
    if (result.transition) closeForConversationOnly(sessions[key]);
    scheduleTerminalInputDrain(key, 500);
  }, Math.max(0, delay || 0));
  terminalInputDrainTimers[key] = timer;
  if (typeof timer.unref === 'function') timer.unref();
}

// Accept terminal free text in every visible session state. Safe live task
// answers continue in the exact canonical child. Mid-turn input is queued in
// order. Ended/unsafe task and Figma generations resume the same transcript in
// a CLI process restricted to read-only tools, so sidecar metadata never grants
// workspace mutation authority.
function sendOrResume(key, text) {
  text = String(text || '');
  // Once a context has pending input, preserve strict submission order: a later
  // message must never jump directly into stdin ahead of the earlier queue.
  if (terminalInputCount(key)) {
    if (!enqueueTerminalInput(key, text)) {
      return { sent: false, resumed: false, error: 'terminal-input-queue-full' };
    }
    scheduleTerminalInputDrain(key, 0);
    return { sent: true, resumed: false, queued: true, busy: true, status: statusOf(key) };
  }
  var delivered = deliverTerminalInput(key, text);
  if (delivered.sent) return delivered;
  if (!delivered.queueable) return delivered;
  if (!enqueueTerminalInput(key, text)) {
    return { sent: false, resumed: false, error: 'terminal-input-queue-full' };
  }
  if (delivered.transition) closeForConversationOnly(sessions[key]);
  scheduleTerminalInputDrain(key, 0);
  return {
    sent: true, resumed: false, queued: true, busy: !!delivered.busy,
    status: statusOf(key)
  };
}

// Typed Task Details continuation. Unlike terminal free text, this capability
// never starts/resumes a process and never queues across a generation change:
// the exact live session and its paused-turn revision must still own the
// canonical writer fence at the delivery boundary.
function continueLive(key, text, expectedSessionId, expectedRevision) {
  text = String(text || '');
  var current = statusOf(key);
  if (!text.trim() || text.length > 8000 || /[\0\r]/.test(text)) {
    return { sent: false, error: 'task-answer-invalid', status: current };
  }
  if (!current.running || !current.askedThisTurn || current.inputReady !== true ||
      !expectedSessionId || current.sessionId !== expectedSessionId ||
      !expectedRevision || current.revision !== expectedRevision) {
    return { sent: false, error: 'recovery-required', status: current };
  }
  var delivered = deliverTerminalInput(key, text);
  if (!delivered.sent) {
    return {
      sent: false,
      error: delivered.error || 'recovery-required',
      lockReason: delivered.lockReason || null,
      status: statusOf(key)
    };
  }
  return { sent: true, error: null, status: statusOf(key) };
}

// A task whose lock is held and fresh is still in flight (e.g. the
// orchestrator ended a turn mid-pipeline right after a Skill-gate report) — do
// NOT auto-close it, or the run is killed and the task strands as 'stopped'.
// Returns true only when there is a fresh lock to defer on; a MISSING or STALE
// lock returns false so the caller reaps exactly as before (slot-leak preserved).
//
// A current task session always carries its validated stem explicitly.
// The stem is validated via locks.lockPathFor (start/end-anchored LOCK_STEM_RE);
// an invalid stem yields a null path and is treated as no-lock (false) — no
// filesystem path is ever built from an unvalidated key.
function hasFreshLock(s) {
  var stem = s.stem;
  if (!stem) return false;
  var lp = locks.lockPathFor(stem);   // null unless stem matches LOCK_STEM_RE
  if (!lp) return false;
  try { if (!fs.statSync(lp).isFile()) return false; } catch (e) { return false; }   // lock missing → reap as today
  var iso = locks.lastActivityIso(stem);
  if (!iso) return false;             // no activity file → not provably fresh → reap as today
  var t = Date.parse(iso);
  if (!isFinite(t)) return false;
  return (Date.now() - t) < STALE_LOCK_MS;   // fresh → defer; stale → reap as today
}

// A task session that finished a turn WITHOUT asking the user anything is done —
// behave like `claude -p`: end stdin so the process exits and frees its slot. If
// it DID ask (needs_action this turn), keep it alive so the user can answer in
// the terminal. "setup" is exempt (it persists across wizard steps). Best-effort
// with a fallback kill so a process that ignores stdin EOF can't pin a slot.
function maybeAutoClose(s) {
  if (s.key === 'setup' || s.askedThisTurn || !s.running || s.closing) return;
  // A fresh, held lock means the task is still mid-pipeline (the turn ended
  // between phases). Defer the close — keep the session alive so the user can
  // continue it in the terminal — but ARM A RECHECK: without it the deferral is
  // permanent (nothing else ever calls maybeAutoClose again) and the session
  // zombies at running:true after the turn already finished. The recheck loop
  // closes as soon as the lock is removed or its activity goes stale.
  if (hasFreshLock(s)) { scheduleLockRecheck(s); return; }
  // Grace window: a needs_action can arrive just after `result` in some protocol
  // orderings. Wait briefly, then re-check — if the session was replaced, ended,
  // started a new turn, asked for input, or is already closing, abort.
  setTimeout(function () {
    if (sessions[s.key] !== s || !s.running || s.awaitingTurn || s.askedThisTurn || s.closing) return;
    // Re-check the lock here too (mirrors the askedThisTurn re-check above): the
    // next pipeline phase may have grabbed/refreshed the lock during this grace
    // window. A fresh lock now means the task is in flight — defer with the same
    // recheck arm as the sync check above (a bare return here was the second,
    // independent path into the permanent-zombie state).
    if (hasFreshLock(s)) { scheduleLockRecheck(s); return; }
    // Committed to close: release the cap slot and stop deduping against this
    // (now winding-down) session immediately, instead of waiting up to ~4s for
    // the child's exit — so a fresh Run for the same stem isn't dropped as a dup.
    s.closing = true;
    try { if (s.child && s.child.stdin && s.child.stdin.writable) s.child.stdin.end(); } catch (e) {}
    // Fallback kill so a process that ignores stdin EOF can't pin a slot forever.
    // Registered unconditionally after the try/catch so a throw in stdin.end() can't skip it.
    s._killTimer = setTimeout(function () {
      if (sessions[s.key] === s && s.running && s.child) {
        try { s.child.kill('SIGTERM'); } catch (e) {}
        // SIGKILL escalation: a `claude` child that ignores SIGTERM would orphan,
        // burn the plan, and pin a MAX_PARALLEL slot. Re-check the same guard so a
        // child that already exited (slot freed, session replaced) isn't signaled.
        setTimeout(function () {
          if (sessions[s.key] === s && s.running && s.child) { try { s.child.kill('SIGKILL'); } catch (e) {} }
        }, 2000);
      }
    }, 4000);
  }, 800);
}

// Arm or refresh the lock-deferred recheck. A
// session whose close was deferred on a held+fresh lock re-runs maybeAutoClose
// every LOCK_RECHECK_MS until the lock is removed or its activity goes stale
// (hasFreshLock then returns false and the normal close path reaps), so the
// deferral is a bounded pause, not a permanent zombie. Reuses s.idleTimer —
// send() (a new turn), cancel(), exit and error all clear it already, and it
// never coexists with scheduleAwaitingIdleClose (that path requires
// askedThisTurn, which maybeAutoClose bails on before reaching the lock check).
function scheduleLockRecheck(s) {
  if (s.idleTimer != null) { clearTimeout(s.idleTimer); s.idleTimer = null; }
  s.idleTimer = setTimeout(function () {
    s.idleTimer = null;
    if (sessions[s.key] !== s || !s.running || s.awaitingTurn || s.askedThisTurn || s.closing) return;
    maybeAutoClose(s);   // lock still fresh → re-arms via the defer branch; gone/stale → closes
  }, LOCK_RECHECK_MS);
}

// Arm/refresh an idle auto-close for an 'awaiting' session (one that asked the
// user via needs_action and was kept alive by maybeAutoClose's early return). If
// the user never answers within AWAITING_IDLE_MS, close it like a normal
// auto-close so the slot frees — an abandoned awaiting session must not pin a
// runner slot until restart. send() (the answer), cancel(), and exit/error all
// clear s.idleTimer, so this only fires on genuine abandonment.
function scheduleAwaitingIdleClose(s) {
  if (s.idleTimer != null) { clearTimeout(s.idleTimer); s.idleTimer = null; }
  s.idleTimer = setTimeout(function () {
    s.idleTimer = null;
    // Still the same session, still paused-and-waiting (alive, not mid-turn,
    // asked, not already closing)? Then it was abandoned — close it (mirrors
    // maybeAutoClose's commit path).
    if (sessions[s.key] !== s || !s.running || s.awaitingTurn || s.closing || !s.askedThisTurn) return;
    s.closing = true;
    try { if (s.child && s.child.stdin && s.child.stdin.writable) s.child.stdin.end(); } catch (e) {}
    s._killTimer = setTimeout(function () {
      if (sessions[s.key] === s && s.running && s.child) {
        try { s.child.kill('SIGTERM'); } catch (e) {}
        // SIGKILL escalation: a `claude` child that ignores SIGTERM would orphan,
        // burn the plan, and pin a MAX_PARALLEL slot. Re-check the same guard so a
        // child that already exited (slot freed, session replaced) isn't signaled.
        setTimeout(function () {
          if (sessions[s.key] === s && s.running && s.child) { try { s.child.kill('SIGKILL'); } catch (e) {} }
        }, 2000);
      }
    }, 4000);
  }, AWAITING_IDLE_MS);
}

function cancel(key) {
  var s = sessions[key];
  if (!s) return false;
  settlePromptDelivery(s, false, new Error('Claude session canceled before prompt delivery settled'));
  abortTurnPublication(s);
  s.canceled = true;
  s.writerTerminationPending = true;
  retainTurnLease(s, 'Claude session was canceled; process-tree death is pending proof');
  s.closing = true;
  if (s._killTimer != null) { clearTimeout(s._killTimer); s._killTimer = null; }
  if (s.idleTimer != null) { clearTimeout(s.idleTimer); s.idleTimer = null; }
  if (s.child) {
    signalSessionTree(s.child, 'SIGTERM');
    // SIGKILL fallback so a child that ignores SIGTERM can't orphan and keep
    // burning the plan. The guard checks only that the session is still current
    // (sessions[s.key]===s) and its child handle is present — NOT s.running (set
    // false just below), so a cancel mid-turn still escalates.
    setTimeout(function () {
      if (sessions[s.key] === s && s.child) signalSessionTree(s.child, 'SIGKILL');
    }, 2000);
  } else releaseTurnLease(s);
  // A canceled live child retains the writer lease until its exit/error event.
  // SIGTERM is only a request; releasing here would let a finalizer overlap the
  // up-to-2s SIGKILL grace (or an unproven descendant).
  s.running = false; s.awaitingTurn = false; s.askedThisTurn = false;
  s.endedAt = s.endedAt || new Date().toISOString();
  if (s.logFd != null) { try { fs.closeSync(s.logFd); } catch (e) {} s.logFd = null; }
  writeSidecar(s);
  return true;
}

// Best-effort: kill every live child (server shutdown) so Ctrl+C doesn't orphan
// `claude` processes that keep consuming the plan in the background.
function killAll() {
  var keys = Object.keys(sessions);
  var kids = [];
  for (var i = 0; i < keys.length; i++) {
    var s = sessions[keys[i]];
    if (s && s.child) {
      kids.push(s.child);
      s.writerTerminationPending = true;
      retainTurnLease(s, 'site shutdown began while a mutating Claude turn was active; process-tree death is pending proof');
      signalSessionTree(s.child, 'SIGTERM');
    }
  }
  // SIGKILL escalation, mirroring cancel() / auto-close: a `claude` that ignores
  // SIGTERM would be reparented to init on our exit and keep burning the plan.
  // server.js delays process.exit past this 1s sweep so it actually fires.
  if (kids.length) {
    setTimeout(function () {
      for (var j = 0; j < kids.length; j++) signalSessionTree(kids[j], 'SIGKILL');
    }, 1000);
  }
}

function readPersistedEvents(key, since) {
  var read = fileGuards.boundedRegularFileUnder(
    PROJECT_ROOT, RUNS_DIR, eventsPathFor(key), SESSION_EVENTS_MAX_BYTES
  );
  if (!read) return [];
  var raw = read.bytes.toString('utf8');
  var lines = raw.split('\n');
  var out = [];
  for (var i = 0; i < lines.length; i++) {
    if (!lines[i].trim()) continue;
    var ev; try { ev = JSON.parse(lines[i]); } catch (e) { continue; }
    if (ev && typeof ev.seq === 'number' && ev.seq >= since) out.push(ev);
  }
  if (out.length > MAX_EVENTS) out = out.slice(out.length - MAX_EVENTS);
  return out;
}

function readPersistedStatus(key) {
  var read = boundedJsonFile(sidecarPathFor(key), SESSION_SIDECAR_MAX_BYTES);
  return read && !sessionSidecarIssue(read.value, path.basename(sidecarPathFor(key))) ? read.value : null;
}

function eventsSince(key, since) {
  since = Math.trunc(Number(since) || 0); if (since < 0) since = 0;
  var s = sessions[key];
  if (s) {
    var out = [];
    for (var i = 0; i < s.events.length; i++) if (s.events[i].seq >= since) out.push(s.events[i]);
    return out;
  }
  return readPersistedEvents(key, since);   // finished / old → serve from disk
}

// Prompt-free public readiness bit for the terminal. This is presentation only,
// never delivery authority: sendOrResume repeats the lock/task/lease checks at
// the stdin boundary. It prevents the browser from advertising an input that
// the already-frozen task continuation fence can never accept.
function taskInputReady(s) {
  if (!s || s.key.indexOf('task:') !== 0) return null;
  if (s.conversationOnly) {
    return !!(s.running && !s.closing && !s.awaitingTurn && !s.stdinWritePending &&
      !s.resultAwaitingStdinSettlement);
  }
  if (!s.running || s.closing || s.awaitingTurn || s.autoDefaultPending || s.stdinWritePending ||
      s.resultAwaitingStdinSettlement || !s.writerLease) return false;
  var fence = s.askedThisTurn ? s.answerFence : (s.turnResultSeen ? s.idleContinuationFence : null);
  return !!(fence && fence.ok && fence.generation === s.turnGeneration &&
    fence.action === s.action && fence.expectedState === s.expectedState &&
    fence.admittedSourceRevision === s.sourceRevision);
}

function sessionRevision(s) {
  if (!s) return null;
  var seed = [
    s.sessionId || '',
    s.startedAt || '',
    s.running === true ? 'running' : 'stopped',
    s.closing === true ? 'closing' : 'open',
    s.askedThisTurn === true && !s.autoDefaultPending ? 'asked' : 'not-asked',
    Number.isSafeInteger(s.turnGeneration) ? String(s.turnGeneration) : '',
    s.action || '',
    s.expectedState || '',
    s.sourceRevision || ''
  ].join('\0');
  return 'sha256:' + crypto.createHash('sha256').update(seed, 'utf8').digest('hex');
}

function statusOf(key) {
  var s = sessions[key];
  if (s) {
    var minSeq = s.events.length ? s.events[0].seq : 0;   // 0 when empty, matching writeSidecar()
    // `closing` rides along (as in list()) so the terminal's status line can tell
    // "winding down after the turn finished" from "idle, ready for input".
    return { running: s.running, sessionId: s.sessionId, revision: sessionRevision(s),
      awaitingTurn: s.awaitingTurn,
      askedThisTurn: !!s.askedThisTurn && !s.autoDefaultPending,
      closing: !!s.closing,
      inputReady: taskInputReady(s),
      queuedInputCount: terminalInputCount(key),
      nextSeq: s.seq, minSeq: minSeq, startedAt: s.startedAt, endedAt: s.endedAt,
      exitCode: s.exitCode, canceled: !!s.canceled, stem: s.stem, action: s.action,
      dedupKey: s.dedupKey || null, dedupReport: s.dedupReport || null };
  }
  var side = readPersistedStatus(key);
  if (side) {
    return { running: false, sessionId: side.sessionId || null, revision: null,
      awaitingTurn: false, closing: false,
      inputReady: key.indexOf('task:') === 0 ? false : null,
      queuedInputCount: terminalInputCount(key),
      nextSeq: (typeof side.nextSeq === 'number' ? side.nextSeq : 0),
      minSeq: (typeof side.minSeq === 'number' ? side.minSeq : 0),
      startedAt: side.startedAt || null, endedAt: side.endedAt || null,
      exitCode: ((side.exitCode === null || side.exitCode === undefined) ? null : side.exitCode), canceled: !!side.canceled,
      stem: side.stem || null, action: side.action || null,
      dedupKey: side.dedupKey || null, dedupReport: side.dedupReport || null };
  }
  return { running: false, sessionId: null, revision: null, awaitingTurn: false, closing: false,
    inputReady: key.indexOf('task:') === 0 ? false : null, queuedInputCount: terminalInputCount(key), nextSeq: 0, minSeq: 0,
    startedAt: null, endedAt: null, exitCode: null, canceled: false, stem: null, action: null,
    dedupKey: null, dedupReport: null };
}

// A completed turn is not yet a completed runtime session. The child may still
// be closing (or retaining a writer lock), so callers that are about to publish
// a terminal operation state must wait until `running` is false.
function settled(status) {
  return !status || status.running === false;
}

// Snapshot of LIVE (+ recently finished) sessions for /api/state, keyed by key.
// The client run-control reads store.sessions[key] to flip Run ↔ Terminal.
function list() {
  prune();
  var out = {};
  var keys = Object.keys(sessions);
  for (var i = 0; i < keys.length; i++) {
    var s = sessions[keys[i]];
    // askedThisTurn — the session emitted a needs_action and is alive, waiting for
    // the user's answer (a paused-for-input state the board paints as "awaiting").
    // Only meaningful on a live session; a finished session resets it on exit.
    out[s.key] = { running: s.running, sessionId: s.sessionId,
      revision: sessionRevision(s), awaitingTurn: s.awaitingTurn,
      askedThisTurn: !!s.askedThisTurn && !s.autoDefaultPending,
      closing: !!s.closing, inputReady: taskInputReady(s),
      queuedInputCount: terminalInputCount(s.key),
      startedAt: s.startedAt, endedAt: s.endedAt, exitCode: s.exitCode, canceled: !!s.canceled,
      stem: s.stem, action: s.action, dedupKey: s.dedupKey || null, dedupReport: s.dedupReport || null };
  }
  // Discover persisted sessions no longer in memory (after the FINISHED_TTL_MS
  // prune), so the user can still reopen a finished task's terminal. Mirrors the
  // statusOf() disk fallback; reads .key straight from the sidecar (no reverse
  // safeKey()), and never overrides an in-memory entry (that's the source of truth).
  //
  // mtime-keyed so a steady-state poll re-parses nothing: if RUNS_DIR's own mtime
  // is unchanged no sidecar was added/removed, so we skip the readdir and walk the
  // cache directly; otherwise we readdir but still re-read only sidecars whose
  // mtimeMs advanced past the cached value, reusing the cached parse for the rest.
  var directory = fileGuards.realDirectoryUnder(PROJECT_ROOT, RUNS_DIR, { allowMissing: true });
  var dirMtime = directory && directory.exists ? directory.stat.mtimeMs : -1;
  if (!directory) {
    sidecarCache = Object.create(null);
    dirMtimeMs = -1;
    return out;
  }
  if (dirMtime >= 0 && dirMtime === dirMtimeMs) {
    // Directory unchanged → no sidecar added/removed; only cached entries apply.
    for (var ck in sidecarCache) {
      if (!Object.prototype.hasOwnProperty.call(sidecarCache, ck)) continue;
      addSidecarEntry(out, sidecarCache[ck].parsed, ck);
    }
  } else {
    var listed = boundedRunsNames();
    if (!listed.ok) {
      sidecarCache = Object.create(null);
      dirMtimeMs = -1;
      return out;
    }
    var names = listed.names.filter(function (name) { return name.endsWith('.session.json'); });
    if (names.length > SESSION_SIDECARS_MAX) {
      sidecarCache = Object.create(null);
      dirMtimeMs = -1;
      return out;
    }
    var fresh = Object.create(null);   // rebuild the cache so removed sidecars drop out
    var totalBytes = 0;
    for (var j = 0; j < names.length; j++) {
      var p = path.join(RUNS_DIR, names[j]);
      var bounded = fileGuards.boundedRegularFileUnder(PROJECT_ROOT, RUNS_DIR, p, SESSION_SIDECAR_MAX_BYTES);
      if (!bounded) continue;
      totalBytes += bounded.bytes.length;
      if (totalBytes > SESSION_SIDECARS_TOTAL_MAX_BYTES) {
        sidecarCache = Object.create(null);
        dirMtimeMs = -1;
        return out;
      }
      var mtimeMs = bounded.stat.mtimeMs;
      var cached = sidecarCache[names[j]];
      var side;
      if (cached && cached.mtimeMs === mtimeMs && cached.ctimeMs === bounded.stat.ctimeMs &&
          cached.dev === bounded.stat.dev && cached.ino === bounded.stat.ino && cached.size === bounded.stat.size) {
        side = cached.parsed;   // unchanged since last scan — reuse the parse
      } else {
        try { side = JSON.parse(bounded.bytes.toString('utf8')); } catch (e) { continue; }
      }
      fresh[names[j]] = {
        mtimeMs: mtimeMs, ctimeMs: bounded.stat.ctimeMs, dev: bounded.stat.dev,
        ino: bounded.stat.ino, size: bounded.stat.size, parsed: side
      };
      addSidecarEntry(out, side, names[j]);
    }
    if (listed.directory && listed.directory.exists && !runsDirectoryUnchanged(listed.directory)) {
      sidecarCache = Object.create(null);
      dirMtimeMs = -1;
      return out;
    }
    sidecarCache = fresh;
    dirMtimeMs = dirMtime;
  }
  return out;
}

// Fold a sidecar's parsed status into the list() snapshot, never overriding an
// in-memory entry (the live session is the source of truth). Shared between the
// cache-hit and cache-miss paths of list().
function addSidecarEntry(out, side, filename) {
  if (sessionSidecarIssue(side, filename)) return;
  var key = side && side.key;
  if (validSessionKey(key) && !Object.prototype.hasOwnProperty.call(out, key)) {
    out[key] = { running: false, sessionId: side.sessionId || null, revision: null,
      awaitingTurn: false, askedThisTurn: false, closing: false, startedAt: side.startedAt || null,
      queuedInputCount: terminalInputCount(key),
      endedAt: side.endedAt || null, exitCode: ((side.exitCode === null || side.exitCode === undefined) ? null : side.exitCode),
      canceled: !!side.canceled, stem: side.stem || null, action: side.action || null,
      dedupKey: side.dedupKey || null, dedupReport: side.dedupReport || null };
  }
}

// Running task sessions (excludes "setup") — the runner's concurrency gate.
function taskRunningCount() {
  var n = 0, keys = Object.keys(sessions);
  for (var i = 0; i < keys.length; i++) {
    var s = sessions[keys[i]];
    // Exclude 'setup' (persistent) and the 'figma:*' / 'contract:*' export
    // sessions — none of them is a board task, so none should consume the
    // runner's task concurrency budget.
    // Gate on childAlive, NOT !closing: a closing session's `claude` child stays
    // alive (and keeps editing the shared tree) up to ~6s+ after `closing` is set,
    // so freeing its slot early lets the runner exceed MAX_PARALLEL. childAlive is
    // cleared only when the child has actually exited.
    if (s && s.childAlive && s.key !== 'setup' && s.key.indexOf('figma:') !== 0 && s.key.indexOf('contract:') !== 0) n++;
  }
  return n;
}

// The running session key for a task stem, or null — the runner's dedup check.
function runningKeyForStem(stem) {
  var info = runningInfoForStem(stem);
  return info ? info.key : null;
}

// Like runningKeyForStem, but also says whether that session is BUSY (mid-turn,
// or paused on a needs_action question) vs merely warm-and-idle (its turn
// finished; the close may be lock-deferred). The enqueue dedup and the runner
// treat only a BUSY session as "in flight": an idle-warm one accepts the new
// prompt as its next turn (send), so a lock-deferred session never wedges Run.
// Does any session for this stem STILL hold its board-task writer lease?
// `cancel()` deliberately withdraws nothing — SIGTERM is a request, and the
// lease is retained until process-tree death is proven — so a caller that
// cancels a warm session and then reads the lease store in the same turn always
// sees the lease. Anything that must wait for the lease to clear polls this
// instead of assuming the cancel took effect.
function taskWriterLeaseHeldForStem(stem) {
  if (!stem) return false;
  var keys = Object.keys(sessions);
  for (var i = 0; i < keys.length; i++) {
    var s = sessions[keys[i]];
    if (s && s.writerLease && s.stem === stem &&
        s.key.indexOf('figma:') !== 0 && s.key.indexOf('contract:') !== 0) return true;
  }
  return false;
}
function runningInfoForStem(stem) {
  if (!stem) return null;
  var keys = Object.keys(sessions);
  for (var i = 0; i < keys.length; i++) {
    var s = sessions[keys[i]];
    // Exclude the figma:*/contract:* families (a figma:screens:<stem> pull/drift session also carries
    // s.stem): they are NOT board tasks, so the runner's per-stem dedup + warm-session delivery must
    // never match them — mirrors taskRunningCount's exclusion. Else a screen pull/drift for a stem
    // collides with the real pipeline run for the same stem (silent-drop of the run, or the run
    // prompt delivered into the Figma pull session).
    if (s && s.running && !s.closing && s.stem === stem && s.key.indexOf('figma:') !== 0 && s.key.indexOf('contract:') !== 0) {
      // A read-only terminal continuation shares the task key but cannot accept
      // a canonical mutating queue prompt. Hold the request until that short
      // conversation turn auto-closes instead of consuming it into a no-tools child.
      return {
        key: s.key,
        busy: !!(s.conversationOnly || s.awaitingTurn || s.askedThisTurn),
        action: s.action || null,
        noQuestions: s.noQuestions === true
      };
    }
  }
  return null;
}

// On boot, any persisted session sidecar still marked running is an orphan from
// a prior server process (the child died with it) — flip it to interrupted so
// the UI doesn't show a phantom running session.
function reconcile() {
  var listed = boundedRunsNames();
  if (!listed.ok) return;
  var names = listed.names.filter(function (name) { return name.endsWith('.session.json'); });
  if (names.length > SESSION_SIDECARS_MAX) return;
  var rows = [];
  var totalBytes = 0;
  for (var i = 0; i < names.length; i++) {
    var p = path.join(RUNS_DIR, names[i]);
    var read = fileGuards.boundedRegularFileUnder(PROJECT_ROOT, RUNS_DIR, p, SESSION_SIDECAR_MAX_BYTES);
    if (!read) return;
    totalBytes += read.bytes.length;
    if (totalBytes > SESSION_SIDECARS_TOTAL_MAX_BYTES) return;
    var side;
    try { side = JSON.parse(read.bytes.toString('utf8')); }
    catch (e) { return; }
    if (!side || sessionSidecarIssue(side, names[i])) return;
    rows.push({ path: p, side: side });
  }
  if (listed.directory && listed.directory.exists && !runsDirectoryUnchanged(listed.directory)) return;
  for (var j = 0; j < rows.length; j++) {
    var p = rows[j].path;
    var side = rows[j].side;
    if (side && side.running) {
      side.running = false; side.awaitingTurn = false;
      side.endedAt = side.endedAt || new Date().toISOString();
      var bytes = Buffer.from(JSON.stringify(side, null, 2) + '\n', 'utf8');
      if (bytes.length > SESSION_SIDECAR_MAX_BYTES || !fileGuards.atomicReplaceRegularFile(
        PROJECT_ROOT, RUNS_DIR, p, bytes,
        { create: false, mode: 0o600, maxBytes: SESSION_SIDECAR_MAX_BYTES }
      )) return;
    }
  }
}

function integrityHash(bytes) { return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex'); }
function integrityFinding(code, stem, file, message) {
  return { code: code, severity: 'error', stem: stem || null, paths: file ? [file] : [], message: message, recovery: 'Inspect and recover the exact session sidecar through its owning runtime; do not delete runtime state by age.' };
}
function scanIntegrity(scope) {
  var stem = typeof scope === 'string' ? scope : scope && scope.stem || null;
  var out = { version: 1, owner: 'sessions', statuses: [], findings: [], snapshotInputs: [], truncated: false };
  var listed;
  if (stem) {
    var runsDirectory = fileGuards.realDirectoryUnder(PROJECT_ROOT, RUNS_DIR, { allowMissing: true });
    if (!runsDirectory) {
      out.findings.push(integrityFinding('SESSION_SIDECAR_DIRECTORY_UNSAFE', stem, RUNS_DIR, 'Session runtime directory cannot be inspected safely.'));
      return out;
    }
    if (!runsDirectory.exists) return out;
    var exactName = safeKey('task:' + stem) + '.session.json';
    var exactEntry = fileGuards.inspectEntryUnder(PROJECT_ROOT, RUNS_DIR, path.join(RUNS_DIR, exactName));
    if (exactEntry.status === 'missing') return out;
    if (exactEntry.status !== 'present') {
      out.findings.push(integrityFinding('SESSION_SIDECAR_DIRECTORY_UNSAFE', stem, RUNS_DIR, 'Session runtime directory cannot be inspected safely.'));
      return out;
    }
    listed = { ok: true, names: [exactName] };
  } else listed = boundedRunsNames();
  if (!listed.ok) {
    out.findings.push(integrityFinding(listed.code === 'runs-directory-entry-limit' ? 'SESSION_SIDECAR_SCAN_LIMIT' : 'SESSION_SIDECAR_DIRECTORY_UNSAFE', null, RUNS_DIR,
      listed.code === 'runs-directory-entry-limit' ? 'Session runtime directory exceeds its bounded scan limit.' : 'Session runtime directory cannot be read safely.'));
    out.truncated = listed.code === 'runs-directory-entry-limit';
    return out;
  }
  var names = listed.names.filter(function (name) { return name.endsWith('.session.json'); }).sort();
  if (names.length > SESSION_SIDECARS_MAX) {
    out.findings.push(integrityFinding('SESSION_SIDECAR_SCAN_LIMIT', null, RUNS_DIR, 'Session sidecar count exceeds its bounded scan limit.'));
    out.truncated = true; return out;
  }
  var total = 0;
  names.forEach(function (name) {
    var file = path.join(RUNS_DIR, name);
    var read = fileGuards.boundedRegularFileUnder(PROJECT_ROOT, RUNS_DIR, file, SESSION_SIDECAR_MAX_BYTES);
    if (!read) {
      out.findings.push(integrityFinding('SESSION_SIDECAR_UNSAFE', null, file, 'Session sidecar is unsafe, unstable, or oversized.'));
      return;
    }
    total += read.bytes.length;
    if (total > SESSION_SIDECARS_TOTAL_MAX_BYTES) { out.truncated = true; return; }
    var value;
    try { value = JSON.parse(read.bytes.toString('utf8')); }
    catch (error) { value = null; }
    var invalid = sessionSidecarIssue(value, name);
    var rowStem = value && typeof value.stem === 'string' ? value.stem : null;
    out.snapshotInputs.push({ owner: 'sessions', kind: 'sidecar', path: file, hash: integrityHash(read.bytes), size: read.bytes.length });
    if (invalid) out.findings.push(integrityFinding('SESSION_SIDECAR_INVALID', rowStem, file, invalid));
    else out.statuses.push({ owner: 'sessions', kind: 'session', stem: value.stem, key: value.key,
      state: value.running ? (value.awaitingTurn ? 'awaiting' : 'running') : (value.canceled ? 'canceled' : 'finished'),
      startedAt: value.startedAt, updatedAt: value.endedAt || value.startedAt, contentHash: integrityHash(read.bytes) });
  });
  if (total > SESSION_SIDECARS_TOTAL_MAX_BYTES) {
    out.findings.push(integrityFinding('SESSION_SIDECAR_SCAN_LIMIT', null, RUNS_DIR, 'Session sidecar bytes exceed the bounded aggregate limit.'));
  }
  return out;
}

function init() { if (ensureRunsDir()) reconcile(); }

module.exports = {
  init: init,
  start: start,
  send: send,
  sendOrResume: sendOrResume,
  continueLive: continueLive,
  boundedRunsNames: boundedRunsNames,
  cancel: cancel,
  killAll: killAll,
  eventsSince: eventsSince,
  status: statusOf,
  settled: settled,
  list: list,
  taskRunningCount: taskRunningCount,
  runningKeyForStem: runningKeyForStem,
  runningInfoForStem: runningInfoForStem,
  taskWriterLeaseHeldForStem: taskWriterLeaseHeldForStem,
  runGateError: runGateError,
  screenshotGateApplies: screenshotGateApplies,
  validSessionKey: validSessionKey,
  writerLeaseKeyFor: writerLeaseKeyFor,
  writerLeaseRequiresSoleWriter: writerLeaseRequiresSoleWriter,
  scanIntegrity: scanIntegrity,
  configureTurnPublication: configureTurnPublication
};
