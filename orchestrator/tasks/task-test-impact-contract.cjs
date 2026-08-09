'use strict';

// ---------------------------------------------------------------------------
// Planned/observed test-impact contract (improvement 05, Phase 3).
// Validates the exact artifact shape of test-impact.schema.json in code
// (schema file = documentation + external tooling; this module = runtime
// authority), recomputes the domain-separated impactHash and enforces the
// one-way rules:
//   - model output can only be WIDENED by the resolver, never narrowed;
//   - unknown dependencies force the full suite;
//   - a typed N/A excludes every executable requirement and demands exact
//     allowlisted structural validators;
//   - observed never rewrites planned — both are immutable and hash-bound.
// ---------------------------------------------------------------------------

const crypto = require('crypto');

const IMPACT_DOMAIN = 'test-impact';
const HASH_RE = /^sha256:[0-9a-f]{64}$/;
const STEM_RE = /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/;
const RUN_RE = /^run-[A-Za-z0-9][A-Za-z0-9-]{0,79}$/;
const MODULE_RE = /^:[A-Za-z0-9._-]+(?::[A-Za-z0-9._-]+)*$/;
const ID_RE = /^[a-z][a-z0-9-]{0,79}$/;
const TOP_KEYS = Object.freeze([
  'affectedConsumers', 'affectedModules', 'behaviors', 'capabilityInventoryHash',
  'fullSuiteRequired', 'impactHash', 'moduleGraphHash', 'notApplicableValidators',
  'phase', 'policyHash', 'policyVersion', 'requiredCapabilities', 'requiredSuites',
  'runId', 'selectionReasons', 'sourceSnapshotHash', 'taskInputHash', 'taskStem',
  'testNotApplicable', 'unknownDependencies', 'version'
]);
const BEHAVIOR_KEYS = Object.freeze([
  'acceptanceHash', 'anchor', 'changeKind', 'negativeCases', 'observedTestCases', 'ownerBuilder',
  'ownerModule', 'proposedTestCases', 'requiredLanes', 'testFile', 'testLayer'
]);
const CAPABILITIES = Object.freeze(['base', 'compose-ui', 'coroutines', 'coverage', 'di', 'flow', 'network', 'room', 'screenshot']);

class TestImpactError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'TestImpactError';
    this.code = code;
  }
}

function fail(code, message) { throw new TestImpactError(code, message); }

function sha256(value) {
  return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex');
}

function canonicalJson(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
  return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
}

function impactHashOf(impact) {
  const model = {};
  for (const key of Object.keys(impact)) {
    if (key !== 'impactHash') model[key] = impact[key];
  }
  return sha256(IMPACT_DOMAIN + '\0' + canonicalJson(model));
}

function checkHash(name, value) {
  if (typeof value !== 'string' || !HASH_RE.test(value)) fail('IMPACT_INVALID', name + ' grammar');
}

function checkStringArray(name, value, { max, re, unique = true, allowEmpty = true }) {
  if (!Array.isArray(value)) fail('IMPACT_INVALID', name + ' must be an array');
  if (value.length > max) fail('IMPACT_INVALID', name + ' exceeds ' + max + ' entries');
  if (!allowEmpty && value.length === 0) fail('IMPACT_INVALID', name + ' must not be empty');
  const seen = new Set();
  for (const item of value) {
    if (typeof item !== 'string' || item.length === 0 || item.length > 300) fail('IMPACT_INVALID', name + ' entry grammar');
    if (re && !re.test(item)) fail('IMPACT_INVALID', name + ' entry grammar: ' + item);
    if (unique) {
      if (seen.has(item)) fail('IMPACT_INVALID', name + ' has a duplicate: ' + item);
      seen.add(item);
    }
  }
}

function validateImpact(impact, { policy }) {
  if (!impact || typeof impact !== 'object' || Array.isArray(impact)) fail('IMPACT_INVALID', 'impact must be an object');
  if (!policy || typeof policy !== 'object') fail('IMPACT_INVALID', 'a validated policy is required');
  const keys = Object.keys(impact).sort();
  if (keys.length !== TOP_KEYS.length || keys.some((key, i) => key !== TOP_KEYS[i])) {
    fail('IMPACT_INVALID', 'top-level keys must be exactly ' + TOP_KEYS.join(','));
  }
  if (impact.version !== 1) fail('IMPACT_INVALID', 'unsupported version');
  if (impact.policyVersion !== policy.version) fail('IMPACT_INVALID', 'policyVersion mismatch');
  if (impact.policyHash !== policy.policyHash) fail('POLICY_MISMATCH', 'impact is bound to a different policy hash');
  if (!STEM_RE.test(String(impact.taskStem))) fail('IMPACT_INVALID', 'taskStem grammar');
  if (!RUN_RE.test(String(impact.runId))) fail('IMPACT_INVALID', 'runId grammar');
  if (impact.phase !== 'planned' && impact.phase !== 'observed') fail('IMPACT_INVALID', 'phase must be planned|observed');
  for (const name of ['taskInputHash', 'sourceSnapshotHash', 'capabilityInventoryHash', 'moduleGraphHash']) {
    checkHash(name, impact[name]);
  }
  if (!Array.isArray(impact.behaviors) || impact.behaviors.length > 200) fail('IMPACT_INVALID', 'behaviors bounds');
  const anchorRe = new RegExp(policy.anchorGrammar);
  const seenAnchors = new Set();
  for (const behavior of impact.behaviors) {
    if (!behavior || typeof behavior !== 'object' || Array.isArray(behavior)) fail('IMPACT_INVALID', 'behavior must be an object');
    const bKeys = Object.keys(behavior).sort();
    if (bKeys.length !== BEHAVIOR_KEYS.length || bKeys.some((key, i) => key !== BEHAVIOR_KEYS[i])) {
      fail('IMPACT_INVALID', 'behavior keys must be exactly ' + BEHAVIOR_KEYS.join(','));
    }
    if (!anchorRe.test(behavior.anchor)) fail('IMPACT_INVALID', 'anchor grammar: ' + behavior.anchor);
    if (seenAnchors.has(behavior.anchor)) fail('IMPACT_INVALID', 'duplicate anchor: ' + behavior.anchor);
    seenAnchors.add(behavior.anchor);
    checkHash('acceptanceHash', behavior.acceptanceHash);
    if (!Object.prototype.hasOwnProperty.call(policy.changeKinds, behavior.changeKind)) {
      fail('IMPACT_INVALID', 'unknown behavior changeKind: ' + behavior.changeKind);
    }
    if (!ID_RE.test(String(behavior.ownerBuilder))) fail('IMPACT_INVALID', 'ownerBuilder grammar');
    if (!MODULE_RE.test(String(behavior.ownerModule))) fail('IMPACT_INVALID', 'ownerModule grammar');
    if (!policy.lanes.includes(behavior.testLayer)) fail('IMPACT_INVALID', 'unknown testLayer: ' + behavior.testLayer);
    if (typeof behavior.testFile !== 'string' || behavior.testFile.length === 0 || behavior.testFile.length > 400) {
      fail('IMPACT_INVALID', 'testFile grammar');
    }
    checkStringArray('proposedTestCases', behavior.proposedTestCases, { max: 64 });
    checkStringArray('observedTestCases', behavior.observedTestCases, { max: 256 });
    if (impact.phase === 'planned' && behavior.proposedTestCases.length === 0) {
      fail('IMPACT_INVALID', 'planned behavior requires proposed test identities');
    }
    if (impact.phase === 'planned' && behavior.observedTestCases.length > 0) {
      fail('IMPACT_INVALID', 'planned impact must not carry observed identities');
    }
    if (impact.phase === 'observed' && behavior.observedTestCases.length === 0) {
      fail('IMPACT_INVALID', 'observed behavior requires discovered test identities');
    }
    checkStringArray('requiredLanes', behavior.requiredLanes, { max: 6, allowEmpty: false });
    for (const lane of behavior.requiredLanes) {
      if (!policy.lanes.includes(lane)) fail('IMPACT_INVALID', 'unknown required lane: ' + lane);
    }
    checkStringArray('negativeCases', behavior.negativeCases, { max: 64 });
  }
  checkStringArray('affectedModules', impact.affectedModules, { max: 2000, re: MODULE_RE });
  checkStringArray('affectedConsumers', impact.affectedConsumers, { max: 2000, re: MODULE_RE });
  checkStringArray('requiredSuites', impact.requiredSuites, { max: 64, re: ID_RE });
  if (typeof impact.fullSuiteRequired !== 'boolean') fail('IMPACT_INVALID', 'fullSuiteRequired must be boolean');
  checkStringArray('requiredCapabilities', impact.requiredCapabilities, { max: 16 });
  for (const capability of impact.requiredCapabilities) {
    if (!CAPABILITIES.includes(capability)) fail('IMPACT_INVALID', 'unknown capability: ' + capability);
  }
  checkStringArray('unknownDependencies', impact.unknownDependencies, { max: 200, unique: false });
  if (impact.unknownDependencies.length > 0 && impact.fullSuiteRequired !== true) {
    fail('IMPACT_INVALID', 'unknown dependencies force fullSuiteRequired');
  }
  checkStringArray('selectionReasons', impact.selectionReasons, { max: 64, re: ID_RE, allowEmpty: false });
  for (const reason of impact.selectionReasons) {
    if (!policy.selectionReasonCodes.includes(reason)) fail('IMPACT_INVALID', 'unknown selection reason: ' + reason);
  }
  checkStringArray('notApplicableValidators', impact.notApplicableValidators, { max: 16, re: ID_RE });
  if (impact.testNotApplicable !== null) {
    if (!policy.testNotApplicable.includes(impact.testNotApplicable)) {
      fail('IMPACT_INVALID', 'testNotApplicable outside the allowlist: ' + impact.testNotApplicable);
    }
    if (impact.behaviors.length > 0 || impact.requiredSuites.length > 0 ||
        impact.requiredCapabilities.length > 0 || impact.fullSuiteRequired) {
      fail('IMPACT_INVALID', 'a typed N/A excludes executable requirements');
    }
    if (impact.notApplicableValidators.length === 0) fail('IMPACT_INVALID', 'a typed N/A requires structural validators');
    for (const gateId of impact.notApplicableValidators) {
      if (!policy.notApplicableValidators.includes(gateId)) fail('IMPACT_INVALID', 'gate outside the allowlist: ' + gateId);
    }
  } else if (impact.notApplicableValidators.length > 0) {
    fail('IMPACT_INVALID', 'executable impact must not carry N/A validators');
  }
  checkHash('impactHash', impact.impactHash);
  if (impactHashOf(impact) !== impact.impactHash) fail('HASH_MISMATCH', 'impactHash does not match content');
  return Object.freeze(JSON.parse(JSON.stringify(impact)));
}

// Observed may only keep or widen the planned requirements. Narrowing of any
// axis invalidates the plan and returns the exact shrunken facts.
function checkWidening(planned, observed, { policy }) {
  const before = validateImpact(planned, { policy });
  const after = validateImpact(observed, { policy });
  if (before.phase !== 'planned' || after.phase !== 'observed') fail('WIDENING_INVALID', 'expected planned then observed');
  if (before.taskStem !== after.taskStem || before.runId !== after.runId) fail('WIDENING_INVALID', 'identity mismatch');
  if (before.taskInputHash !== after.taskInputHash) fail('WIDENING_INVALID', 'task input changed between passes');
  const shrunk = [];
  const missingSuites = before.requiredSuites.filter((suite) => !after.requiredSuites.includes(suite));
  if (missingSuites.length > 0) shrunk.push('requiredSuites:' + missingSuites.join('+'));
  const missingModules = before.affectedModules.filter((module) => !after.affectedModules.includes(module));
  if (missingModules.length > 0) shrunk.push('affectedModules:' + missingModules.join('+'));
  const missingConsumers = before.affectedConsumers.filter((module) => !after.affectedConsumers.includes(module));
  if (missingConsumers.length > 0) shrunk.push('affectedConsumers:' + missingConsumers.join('+'));
  const missingCapabilities = before.requiredCapabilities.filter((capability) => !after.requiredCapabilities.includes(capability));
  if (missingCapabilities.length > 0) shrunk.push('requiredCapabilities:' + missingCapabilities.join('+'));
  const missingUnknownDependencies = before.unknownDependencies.filter((dependency) =>
    !after.unknownDependencies.includes(dependency));
  if (missingUnknownDependencies.length > 0) {
    shrunk.push('unknownDependencies:' + missingUnknownDependencies.join('+'));
  }
  if (before.fullSuiteRequired && !after.fullSuiteRequired) shrunk.push('fullSuiteRequired');
  if (before.testNotApplicable !== after.testNotApplicable) shrunk.push('testNotApplicable');
  for (const behavior of before.behaviors) {
    const counterpart = after.behaviors.find((candidate) => candidate.anchor === behavior.anchor);
    if (!counterpart) { shrunk.push('behavior:' + behavior.anchor); continue; }
    if (counterpart.changeKind !== behavior.changeKind) shrunk.push('changeKind:' + behavior.anchor);
    const missingLanes = behavior.requiredLanes.filter((lane) => !counterpart.requiredLanes.includes(lane));
    if (missingLanes.length > 0) shrunk.push('lanes:' + behavior.anchor + ':' + missingLanes.join('+'));
  }
  if (shrunk.length > 0) fail('IMPACT_NARROWED', 'observed impact narrowed the plan: ' + shrunk.join(', '));
  const widened =
    after.requiredSuites.length > before.requiredSuites.length ||
    after.affectedModules.length > before.affectedModules.length ||
    after.affectedConsumers.length > before.affectedConsumers.length ||
    after.requiredCapabilities.length > before.requiredCapabilities.length ||
    after.unknownDependencies.length > before.unknownDependencies.length ||
    after.behaviors.length > before.behaviors.length ||
    after.behaviors.some((behavior) => {
      const prior = before.behaviors.find((candidate) => candidate.anchor === behavior.anchor);
      return prior && behavior.requiredLanes.length > prior.requiredLanes.length;
    }) ||
    (!before.fullSuiteRequired && after.fullSuiteRequired);
  return { widened };
}

module.exports = {
  IMPACT_DOMAIN,
  TestImpactError,
  canonicalJson,
  impactHashOf,
  validateImpact,
  checkWidening
};
