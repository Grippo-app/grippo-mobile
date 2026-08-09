import { store } from './store.js';
import { tasksApi } from './data/tasks-api.js';
import { terminal } from './terminal.js';
import { helpers, wizardSteps } from './data/wizard-steps.js';
import { errorCode } from './data/request-json.js';

// ----------------------------------------------------------------------
// Auto-run — the wizard's "run every remaining step in order" engine.
//
// The per-step ▶ Run button (run-control.js) sends ONE step's prompt into the
// shared, persistent "setup" session and lets the user drive the rest by hand.
// Auto-run chains that: send step N → wait for the turn to finish AND the step's
// ✓ filesystem check to land → send step N+1. It PAUSES (never silently skips)
// the moment a step needs the user, so a failed build can't cascade into the
// dependent steps below it:
//
//   - Claude asks a question (needs_action) → keep watching; the user answers in
//     the terminal and the run continues automatically when that turn ends clean.
//   - the turn ends but ✓ never lands within the grace window → pause "unverified".
//   - the session dies / errors                                → pause "stopped".
//
// Step 12 (end-to-end verify) carries an FS validator — the foundation-integrity
// stub gate (no TODO(...)/NotImplementedError in foundation commonMain). So it is
// kind === 'fs' and gates on the ✓ like every other step: a clean turn alone does
// NOT advance past it; if a stub survives, ✓ never lands and the run pauses
// "unverified". (Steps that genuinely have no validator stay kind === 'manual'
// and pass on turn-completion — see waitForVerify.)
//
// This is a module-level singleton (like terminal.js / run-control.js): it
// outlives the wizard panel's re-renders, so the panel reads getState() and
// subscribes to on('change') to paint the header control — the state lives here,
// not in the DOM. Turn boundaries come from polling /api/session/events for the
// "setup" key (the same endpoint the terminal polls), so detection is prompt and
// race-free (anchored on the session's nextSeq captured at send time), and the
// ✓ check is read from the SSE-backed store (progress.stepStatus[id].done) — the
// same signal the per-step indicator uses.
// ----------------------------------------------------------------------

var KEY = 'setup';
var POLL_MS = 1500;            // session-events poll cadence while a step runs
// After a turn ends cleanly, give the server's file watcher this long to see the
// deliverable land (validators run every ~1.5 s; a big assemble step can finish
// its turn a beat before the last file flushes). The step still pauses as
// "unverified" if ✓ never lands — it never silently advances past a step whose
// artifact the watcher can't see.
var VERIFY_GRACE_MS = 30000;

// status: 'idle' | 'running' | 'awaiting-input' | 'verifying' | 'paused' | 'done'
// reason (only when paused): 'unverified' | 'stopped' | 'busy'
var st = { status: 'idle', stepId: null, reason: null, errorCode: null };
var listeners = [];
var gen = 0;                   // bumped by start()/stop(); stale async work checks it and bails
var pollTimer = null;

function snapshot() { return { status: st.status, stepId: st.stepId, reason: st.reason, errorCode: st.errorCode || null }; }
function emit() { var s = snapshot(); for (var i = 0; i < listeners.length; i++) { try { listeners[i](s); } catch (e) {} } }
function setState(patch) {
  for (var k in patch) if (Object.prototype.hasOwnProperty.call(patch, k)) st[k] = patch[k];
  emit();
}

function on(fn) {
  if (typeof fn !== 'function') return function () {};
  listeners.push(fn);
  return function off() { var i = listeners.indexOf(fn); if (i >= 0) listeners.splice(i, 1); };
}

// ---- store-derived helpers -------------------------------------------------

function snap() { return store.get() || {}; }
function setupForm() { return snap().setup || {}; }

// authProblem = dead keychain token (expired/revoked): loggedIn still reads
// true but every spawned session would 401, so it counts as not-ready.
function cliReady() { var s = snap(); return !!(s.cli && s.cli.installed && s.cli.loggedIn && !s.cli.authProblem); }
function formReady() { return !!setupForm().productName; }

function stepStatusOf(stepId) {
  var p = snap().progress || {};
  var ss = p.stepStatus || {};
  return ss[stepId] || null;
}
function stepDone(stepId) { var s = stepStatusOf(stepId); return !!(s && s.done); }
function stepKind(stepId) { var s = stepStatusOf(stepId); return (s && s.kind) || 'fs'; }

// Prompt text for a step — the shared helpers.stepPrompt renderer (tokens
// substituted, unattended-run footer appended), so auto-run sends exactly
// what the wizard panel displays and copies.
function promptFor(step) {
  return helpers.stepPrompt(step, setupForm());
}

// Ordered list of steps still to run: not auto-skipped, not already done.
function remainingSteps() {
  var setup = setupForm();
  var out = [];
  for (var i = 0; i < wizardSteps.length; i++) {
    var step = wizardSteps[i];
    if (helpers.isAutoSkipped(step, setup)) continue;
    if (stepDone(step.id)) continue;
    out.push(step);
  }
  return out;
}

// ---- the run loop ----------------------------------------------------------

// Send one step's prompt into the setup session, resolving to the baseline seq
// (nextSeq AFTER our user message) so the turn-watcher reads only THIS turn's
// events. A live session takes the prompt via send; otherwise start() spawns and
// sends in one shot. start() drops a prompt sent to an already-running session,
// so the live/dead split (mirroring run-control's defaultRun) is load-bearing.
function sendStep(step) {
  var prompt = promptFor(step);
  var sess = snap().sessions && snap().sessions[KEY];
  var live = !!(sess && sess.running);
  var p = live ? tasksApi.sessionSend(KEY, prompt) : tasksApi.sessionStart(KEY, prompt);
  return Promise.resolve(p).then(function (resp) {
    // A live session mid-turn rejects the send ({sent:false}); don't watch a
    // turn that isn't ours.
    if (resp && resp.sent === false) {
      var busyError = new Error('session-busy');
      busyError.code = 'session-busy';
      throw busyError;
    }
    var status = resp && resp.status;
    return (status && typeof status.nextSeq === 'number') ? status.nextSeq : 0;
  });
}

// Watch the setup session from seq `base` until a turn ends WITHOUT asking. A
// turn ends on each `result` event; if a needs_action preceded it, the run is
// waiting on the user — surface the terminal and keep watching the answer's
// turn. Resolves when a clean turn ends; rejects if the session dies first.
// setup never auto-closes (sessions.js exempts it), so running===false here is a
// genuine crash/stop, not a normal turn end.
function watchTurn(base, myGen) {
  return new Promise(function (resolve, reject) {
    var since = base;
    var askedSegment = false;
    function tick() {
      if (myGen !== gen) return;
      tasksApi.sessionEvents(KEY, since).then(function (r) {
        if (myGen !== gen) return;
        var evs = (r && r.events) || [];
        for (var i = 0; i < evs.length; i++) {
          var kind = evs[i].kind;
          if (kind === 'needs_action') askedSegment = true;
          else if (kind === 'result') {
            if (askedSegment) {
              // Turn ended by asking; wait for the user's terminal answer and
              // keep watching the next turn.
              askedSegment = false;
              setState({ status: 'awaiting-input' });
              terminal.open(KEY);
            } else {
              resolve();
              return;
            }
          }
        }
        var status = r && r.status;
        if (status && typeof status.nextSeq === 'number') since = status.nextSeq;
        if (status && status.running === false) { reject(new Error('session-ended')); return; }
        // The user has begun answering (awaitingTurn flipped back on) → restore
        // the "running" label from "awaiting-input".
        if (st.status === 'awaiting-input' && status && status.awaitingTurn) setState({ status: 'running' });
        pollTimer = setTimeout(tick, POLL_MS);
      }, function () {
        if (myGen !== gen) return;
        pollTimer = setTimeout(tick, POLL_MS);   // transient fetch error → retry
      });
    }
    tick();
  });
}

// After a clean turn, wait for the step's ✓ to land within the grace window.
// Resolves true when verified, false on timeout (→ caller pauses "unverified").
function waitForVerify(stepId, myGen) {
  if (stepDone(stepId)) return Promise.resolve(true);
  // Manual-kind steps (no FS validator) have
  // no ✓ to wait for; a clean turn is their only signal. Record the same "done"
  // the user's checkbox would (store.saveManualStep) — both to drop the step
  // from the remaining set (else runNext would re-send it forever, since its
  // done flag never flips on disk) and to tick its box so the wizard progress
  // reflects the completed run. A persistence failure rejects → runNext pauses.
  if (stepKind(stepId) === 'manual') {
    return store.saveManualStep(stepId, true).then(function () { return true; });
  }
  setState({ status: 'verifying' });
  return new Promise(function (resolve) {
    var waited = 0;
    function tick() {
      if (myGen !== gen) { resolve(false); return; }
      if (stepDone(stepId)) { resolve(true); return; }
      waited += POLL_MS;
      if (waited >= VERIFY_GRACE_MS) { resolve(false); return; }
      pollTimer = setTimeout(tick, POLL_MS);
    }
    tick();
  });
}

function runNext(myGen) {
  if (myGen !== gen) return;
  var remaining = remainingSteps();
  if (remaining.length === 0) { setState({ status: 'done', stepId: null, reason: null, errorCode: null }); return; }
  var step = remaining[0];
  setState({ status: 'running', stepId: step.id, reason: null, errorCode: null });
  sendStep(step).then(function (base) {
    if (myGen !== gen) return;
    return watchTurn(base, myGen).then(function () {
      if (myGen !== gen) return;
      return waitForVerify(step.id, myGen).then(function (ok) {
        if (myGen !== gen) return;
        if (!ok) { setState({ status: 'paused', stepId: step.id, reason: 'unverified', errorCode: null }); terminal.open(KEY); return; }
        runNext(myGen);
      });
    });
  }).catch(function (e) {
    if (myGen !== gen) return;
    // 'busy' is sendStep's mid-turn rejection — the session is WORKING on another
    // turn, the exact opposite of "stopped". Pause with its own reason so the
    // wizard header says "busy — finish/answer the current turn", not the inverse.
    var busy = errorCode(e) === 'session-busy';
    // Every other typed refusal — finalization-active, writer-termination-pending,
    // session-runtime-unsafe, a transport failure — used to read as "the session
    // stopped. Resume to continue", which is both wrong and unactionable: Resume
    // hits the same refusal forever. Carry the code so the wizard can name it.
    setState({
      status: 'paused', stepId: step.id, reason: busy ? 'busy' : 'stopped',
      errorCode: busy || errorCode(e) === 'unknown' ? null : errorCode(e)
    });
    terminal.open(KEY);
  });
}

// ---- public control --------------------------------------------------------

function isActive() { return st.status === 'running' || st.status === 'awaiting-input' || st.status === 'verifying'; }

// Start (or resume) the sequence. A no-op while already active. Resuming from a
// pause recomputes the remaining steps, so a step the user marked done in the
// meantime is skipped and the run picks up at the next unfinished one. Opens the
// terminal so the user watches the sequence stream live.
function start() {
  if (!cliReady() || !formReady()) return false;
  if (isActive()) return false;
  if (remainingSteps().length === 0) { setState({ status: 'done', stepId: null, reason: null, errorCode: null }); return false; }
  gen++;
  if (pollTimer) { clearTimeout(pollTimer); pollTimer = null; }
  terminal.open(KEY);
  runNext(gen);
  return true;
}

// Halt auto-advancing. Does NOT kill the setup session — its warm context stays
// available so the user can finish by hand or Resume. Just stops the watcher.
function stop() {
  gen++;
  if (pollTimer) { clearTimeout(pollTimer); pollTimer = null; }
  setState({ status: 'idle', stepId: null, reason: null, errorCode: null });
}

export const autoRun = {
  start: start,
  stop: stop,
  on: on,
  getState: snapshot,
  isActive: isActive,
  remainingCount: function () { return remainingSteps().length; }
};
