'use strict';

// Process-local startup recovery barrier.
//
// Durable owner modules remain the actual write authority. This barrier adds
// one orchestration rule: the in-process task runner may start only after the
// canonical startup recovery attempt settled and a fresh task/runtime
// integrity scan is green. Read-only HTTP, diagnostics and exact recovery
// endpoints remain available while the barrier is pending or blocked.

var VERSION = 1;
var PUBLIC_REASON_CODES = new Set([
  'startup-recovery-unconfigured',
  'startup-recovery-outcome-invalid',
  'startup-recovery-failed',
  'startup-integrity-unavailable',
  'startup-integrity-blocked',
  'task-index-not-fresh',
  'runner-start-failed'
]);

function iso(clock) { return new Date(clock()).toISOString(); }
function publicReasonCode(value, fallback) {
  var code = String(value || '').toLowerCase().replace(/_/g, '-');
  return PUBLIC_REASON_CODES.has(code) ? code : fallback;
}

function createBarrier(options) {
  options = options || {};
  var clock = typeof options.clock === 'function' ? options.clock : Date.now;
  var verify = typeof options.verify === 'function' ? options.verify : function () {
    return { ok: false, reasonCode: 'startup-recovery-unconfigured', findingCount: 0 };
  };
  var startRunner = typeof options.startRunner === 'function' ? options.startRunner : function () {
    throw new Error('startup recovery runner callback is unavailable');
  };
  var startedAt = iso(clock);
  var state = {
    version: VERSION, status: 'pending', attempts: 0,
    startedAt: startedAt, updatedAt: startedAt, readyAt: null,
    reasonCode: null, findingCount: 0
  };
  var runnerStarted = false;
  var lastSequence = 0;

  function snapshot() { return Object.assign({}, state); }
  function setBlocked(code, findingCount, fallback) {
    state.status = 'blocked';
    state.updatedAt = iso(clock);
    state.readyAt = null;
    state.reasonCode = publicReasonCode(code, fallback || 'startup-recovery-failed');
    state.findingCount = Number.isSafeInteger(findingCount) && findingCount >= 0 ? findingCount : 0;
  }
  function settle(outcome) {
    if (state.status === 'ready') return snapshot();
    if (!outcome || outcome.version !== 1 || !Number.isSafeInteger(outcome.sequence) || outcome.sequence <= 0 ||
        typeof outcome.ok !== 'boolean') {
      setBlocked('startup-recovery-outcome-invalid', 0);
      return snapshot();
    }
    // A delayed callback from an older retry must never overwrite a newer
    // verdict. Duplicate delivery of the same settled attempt is idempotent.
    if (outcome.sequence <= lastSequence) return snapshot();
    lastSequence = outcome.sequence;
    state.attempts++;
    state.updatedAt = iso(clock);
    if (!outcome.ok) {
      setBlocked(outcome.code, 0);
      return snapshot();
    }
    var verdict;
    try { verdict = verify(); }
    catch (error) {
      setBlocked('startup-integrity-unavailable', 0);
      return snapshot();
    }
    if (!verdict || verdict.ok !== true) {
      setBlocked(verdict && verdict.reasonCode,
        verdict && verdict.findingCount, 'startup-integrity-blocked');
      return snapshot();
    }
    try {
      if (!runnerStarted) startRunner();
      runnerStarted = true;
    } catch (error) {
      setBlocked('runner-start-failed', 0);
      return snapshot();
    }
    state.status = 'ready';
    state.updatedAt = iso(clock);
    state.readyAt = state.updatedAt;
    state.reasonCode = null;
    state.findingCount = 0;
    return snapshot();
  }
  return { settle: settle, snapshot: snapshot };
}

var active = null;
function configure(options) {
  if (active) throw new Error('startup recovery barrier is already configured');
  active = createBarrier(options);
  return active;
}
function snapshot() {
  if (active) return active.snapshot();
  return { version: VERSION, status: 'pending', attempts: 0, startedAt: null,
    updatedAt: null, readyAt: null, reasonCode: null, findingCount: 0 };
}
function settle(outcome) {
  if (!active) throw new Error('startup recovery barrier is not configured');
  return active.settle(outcome);
}
module.exports = {
  VERSION: VERSION,
  configure: configure,
  settle: settle,
  snapshot: snapshot,
  publicReasonCode: publicReasonCode,
  _createBarrier: createBarrier
};
