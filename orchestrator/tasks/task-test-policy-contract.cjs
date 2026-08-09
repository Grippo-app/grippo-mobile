'use strict';

// ---------------------------------------------------------------------------
// Machine test-policy authority (pipeline improvement 05, Phase 1).
//
// `orchestrator/tasks/test-policy.json` is the ONLY machine-readable source
// of change kinds, minimum evidence classes, lane/structural-gate ids,
// `test-not-applicable` values, escalation rules and bounded reason codes.
// This contract validates the exact byte shape, recomputes the domain-
// separated `policyHash` (`test-policy\0` + canonical JSON without the
// hash field) and hands consumers a frozen read-only lookup. Skills, prompts
// and Site never copy these enums — they carry the version/hash pointer.
// ---------------------------------------------------------------------------

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const POLICY_VERSION = 1;
const POLICY_DOMAIN = 'test-policy';
const POLICY_PATH = path.join(__dirname, 'test-policy.json');
const HASH_RE = /^sha256:[0-9a-f]{64}$/;
const ID_RE = /^[a-z][a-z0-9-]{0,79}$/;

const TOP_KEYS = Object.freeze([
  'anchorGrammar', 'changeKinds', 'domain', 'escalationRules', 'evidenceClasses',
  'executionTiers', 'flakyPolicy', 'lanes', 'notApplicableForbidden',
  'notApplicableValidators', 'policyHash', 'reasonCodeGrammar',
  'selectionReasonCodes', 'structuralGateIds', 'testNotApplicable', 'version'
]);
const CHANGE_KIND_KEYS = Object.freeze(['defaultLanes', 'minimumEvidence', 'notes']);
const FLAKY_KEYS = Object.freeze(['failThenPassVerdict', 'maxDiagnosticRetries']);

class TestPolicyError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'TestPolicyError';
    this.code = code;
  }
}

function fail(code, message) { throw new TestPolicyError(code, message); }

function sha256(value) {
  return 'sha256:' + crypto.createHash('sha256').update(value).digest('hex');
}

function canonicalJson(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value);
  if (Array.isArray(value)) return '[' + value.map(canonicalJson).join(',') + ']';
  return '{' + Object.keys(value).sort().map((key) => JSON.stringify(key) + ':' + canonicalJson(value[key])).join(',') + '}';
}

function checkIdList(name, value, { allowEmpty = false } = {}) {
  if (!Array.isArray(value)) fail('POLICY_INVALID', name + ' must be an array');
  if (!allowEmpty && value.length === 0) fail('POLICY_INVALID', name + ' must not be empty');
  for (const id of value) {
    if (typeof id !== 'string' || !ID_RE.test(id)) fail('POLICY_INVALID', name + ' has a malformed id: ' + JSON.stringify(id));
  }
  const sorted = [...value].sort();
  for (let i = 0; i < value.length; i++) {
    if (value[i] !== sorted[i]) fail('POLICY_INVALID', name + ' must be strictly sorted');
    if (i > 0 && value[i] === value[i - 1]) fail('POLICY_INVALID', name + ' has a duplicate: ' + value[i]);
  }
  return value;
}

function policyHashOf(policy) {
  const model = {};
  for (const key of Object.keys(policy)) {
    if (key !== 'policyHash') model[key] = policy[key];
  }
  return sha256(POLICY_DOMAIN + '\0' + canonicalJson(model));
}

function validatePolicy(policy) {
  if (!policy || typeof policy !== 'object' || Array.isArray(policy)) fail('POLICY_INVALID', 'policy must be an object');
  const keys = Object.keys(policy).sort();
  if (keys.length !== TOP_KEYS.length || keys.some((key, i) => key !== TOP_KEYS[i])) {
    fail('POLICY_INVALID', 'top-level keys must be exactly ' + TOP_KEYS.join(','));
  }
  if (policy.version !== POLICY_VERSION) fail('POLICY_INVALID', 'unsupported version: ' + policy.version);
  if (policy.domain !== POLICY_DOMAIN) fail('POLICY_INVALID', 'unsupported domain: ' + policy.domain);
  for (const grammarKey of ['anchorGrammar', 'reasonCodeGrammar']) {
    if (typeof policy[grammarKey] !== 'string') fail('POLICY_INVALID', grammarKey + ' must be a string');
    try { void new RegExp(policy[grammarKey]); }
    catch (error) { fail('POLICY_INVALID', grammarKey + ' is not a valid pattern'); }
    if (!policy[grammarKey].startsWith('^') || !policy[grammarKey].endsWith('$')) {
      fail('POLICY_INVALID', grammarKey + ' must be fully anchored');
    }
  }
  const lanes = checkIdList('lanes', policy.lanes);
  const evidenceClasses = checkIdList('evidenceClasses', policy.evidenceClasses);
  checkIdList('executionTiers', policy.executionTiers);
  const structuralGateIds = checkIdList('structuralGateIds', policy.structuralGateIds);
  const notApplicable = checkIdList('testNotApplicable', policy.testNotApplicable);
  const forbidden = checkIdList('notApplicableForbidden', policy.notApplicableForbidden);
  const notApplicableValidators = checkIdList('notApplicableValidators', policy.notApplicableValidators);
  checkIdList('escalationRules', policy.escalationRules);
  checkIdList('selectionReasonCodes', policy.selectionReasonCodes);
  for (const value of notApplicable) {
    if (forbidden.includes(value)) fail('POLICY_INVALID', 'N/A value is also forbidden: ' + value);
  }
  for (const gateId of notApplicableValidators) {
    if (!structuralGateIds.includes(gateId)) fail('POLICY_INVALID', 'notApplicableValidators references unknown gate: ' + gateId);
  }
  if (!policy.changeKinds || typeof policy.changeKinds !== 'object' || Array.isArray(policy.changeKinds)) {
    fail('POLICY_INVALID', 'changeKinds must be an object');
  }
  const kindIds = Object.keys(policy.changeKinds);
  if (kindIds.length === 0) fail('POLICY_INVALID', 'changeKinds must not be empty');
  const sortedKinds = [...kindIds].sort();
  for (let i = 0; i < kindIds.length; i++) {
    if (kindIds[i] !== sortedKinds[i]) fail('POLICY_INVALID', 'changeKinds must be sorted by id');
  }
  for (const kindId of kindIds) {
    if (!ID_RE.test(kindId)) fail('POLICY_INVALID', 'malformed change kind id: ' + kindId);
    const kind = policy.changeKinds[kindId];
    if (!kind || typeof kind !== 'object' || Array.isArray(kind)) fail('POLICY_INVALID', kindId + ' must be an object');
    const kindKeys = Object.keys(kind).sort();
    if (kindKeys.length !== CHANGE_KIND_KEYS.length || kindKeys.some((key, i) => key !== CHANGE_KIND_KEYS[i])) {
      fail('POLICY_INVALID', kindId + ' keys must be exactly ' + CHANGE_KIND_KEYS.join(','));
    }
    checkIdList(kindId + '.minimumEvidence', kind.minimumEvidence, { allowEmpty: kindId === 'documentation-only' });
    for (const evidence of kind.minimumEvidence) {
      if (!evidenceClasses.includes(evidence)) fail('POLICY_INVALID', kindId + ' references unknown evidence class: ' + evidence);
    }
    checkIdList(kindId + '.defaultLanes', kind.defaultLanes, { allowEmpty: true });
    for (const lane of kind.defaultLanes) {
      if (!lanes.includes(lane)) fail('POLICY_INVALID', kindId + ' references unknown lane: ' + lane);
    }
    if (typeof kind.notes !== 'string' || kind.notes.length === 0 || kind.notes.length > 400) {
      fail('POLICY_INVALID', kindId + '.notes must be a bounded non-empty string');
    }
  }
  const flaky = policy.flakyPolicy;
  if (!flaky || typeof flaky !== 'object' || Array.isArray(flaky)) fail('POLICY_INVALID', 'flakyPolicy must be an object');
  const flakyKeys = Object.keys(flaky).sort();
  if (flakyKeys.length !== FLAKY_KEYS.length || flakyKeys.some((key, i) => key !== FLAKY_KEYS[i])) {
    fail('POLICY_INVALID', 'flakyPolicy keys must be exactly ' + FLAKY_KEYS.join(','));
  }
  if (flaky.maxDiagnosticRetries !== 1) fail('POLICY_INVALID', 'maxDiagnosticRetries is frozen at 1');
  if (flaky.failThenPassVerdict !== 'BLOCKED') fail('POLICY_INVALID', 'failThenPassVerdict is frozen at BLOCKED');
  if (typeof policy.policyHash !== 'string' || !HASH_RE.test(policy.policyHash)) fail('POLICY_INVALID', 'policyHash grammar');
  if (policyHashOf(policy) !== policy.policyHash) fail('HASH_MISMATCH', 'policyHash does not match policy content');
  return deepFreeze(JSON.parse(JSON.stringify(policy)));
}

function deepFreeze(value) {
  if (value && typeof value === 'object') {
    for (const key of Object.keys(value)) deepFreeze(value[key]);
    Object.freeze(value);
  }
  return value;
}

let cachedPolicy = null;

function loadPolicy(options = {}) {
  const policyPath = options.path || POLICY_PATH;
  if (!options.path && cachedPolicy) return cachedPolicy;
  let raw;
  try { raw = fs.readFileSync(policyPath, 'utf8'); }
  catch (error) { fail('POLICY_UNREADABLE', 'cannot read ' + policyPath + ' (' + error.code + ')'); }
  let parsed;
  try { parsed = JSON.parse(raw); }
  catch (error) { fail('POLICY_INVALID', 'policy is not valid JSON'); }
  const policy = validatePolicy(parsed);
  if (!options.path) cachedPolicy = policy;
  return policy;
}

function minimumEvidenceFor(changeKind, policy = loadPolicy()) {
  const kind = policy.changeKinds[changeKind];
  if (!kind) fail('UNKNOWN_CHANGE_KIND', 'not a policy change kind: ' + changeKind);
  return kind.minimumEvidence;
}

function isAllowedNotApplicable(value, policy = loadPolicy()) {
  return policy.testNotApplicable.includes(value);
}

module.exports = {
  POLICY_VERSION,
  POLICY_DOMAIN,
  POLICY_PATH,
  TestPolicyError,
  canonicalJson,
  policyHashOf,
  validatePolicy,
  loadPolicy,
  minimumEvidenceFor,
  isAllowedNotApplicable
};
