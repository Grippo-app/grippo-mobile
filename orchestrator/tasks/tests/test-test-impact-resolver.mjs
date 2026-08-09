#!/usr/bin/env node

// Deterministic impact resolver + impact contract (improvement 05, Phase 3):
// the policy table is applied AFTER model output and can only widen; observed
// never narrows planned; unknown dependencies force the full suite.

import assert from 'node:assert/strict';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const policyContract = require('../task-test-policy-contract.cjs');
const impactContract = require('../task-test-impact-contract.cjs');
const { resolveImpact } = await import('../resolve-test-impact.mjs');

const policy = policyContract.loadPolicy();
const failures = [];
let checks = 0;

async function check(name, fn) {
  checks++;
  try { await fn(); console.log(`PASS ${name}`); }
  catch (error) { failures.push({ name, error }); console.error(`FAIL ${name}\n${error && error.stack || error}`); }
}

const H = (c) => 'sha256:' + c.repeat(64);
function proposal(overrides = {}) {
  return {
    taskStem: 'TASK_5_save_note',
    runId: 'run-abc123',
    phase: 'planned',
    taskInputHash: H('1'),
    sourceSnapshotHash: H('2'),
    capabilityInventoryHash: H('3'),
    moduleGraphHash: H('4'),
    behaviors: [{
      changeKind: 'compose-ui',
      anchor: 'test:save-note-button-disables',
      acceptanceHash: H('5'),
      ownerBuilder: 'screen-builder',
      ownerModule: ':ui-screen-features:save-note',
      testLayer: 'host',
      testFile: 'src/uiTest/kotlin/SaveNoteScenario.kt',
      proposedTestCases: ['cui.SaveNoteTest.disablesWhileSaving'],
      requiredLanes: ['host'],
      negativeCases: ['second tap while saving is a no-op']
    }],
    affectedModules: [':ui-screen-features:save-note'],
    affectedConsumers: [],
    requiredSuites: ['save-note'],
    requiredCapabilities: ['compose-ui'],
    ...overrides
  };
}

function rejects(fn, name, code) {
  try { fn(); }
  catch (error) {
    assert.equal(error.name, name, String(error && error.stack || error));
    assert.equal(error.code, code);
    return;
  }
  assert.fail(`expected ${name} ${code}`);
}

await check('policy minimums widen the model output and record why', () => {
  const { impact, widened } = resolveImpact({ policy, proposal: proposal(), facts: {} });
  assert.equal(widened, true);
  assert.deepEqual(impact.behaviors[0].requiredLanes, ['host', 'ios-simulator'],
    'compose-ui lanes come from the policy, not from the narrower model suggestion');
  assert.ok(impact.selectionReasons.includes('policy-minimum-widened'));
  assert.ok(impact.selectionReasons.includes('planner-proposed'));
  assert.equal(impact.policyHash, policy.policyHash);
  assert.equal(impact.behaviors[0].changeKind, 'compose-ui', 'normalized evidence keeps its policy change kind');

  // The audit trail is the point: padding the proposal with duplicates must
  // not buy silence about the policy having to widen it.
  const padded = resolveImpact({
    policy,
    proposal: proposal({ behaviors: [{ ...proposal().behaviors[0], requiredLanes: ['host', 'host'] }] }),
    facts: {}
  });
  assert.equal(padded.widened, true, 'a duplicate-padded proposal is still widened by the policy');
  assert.ok(padded.impact.selectionReasons.includes('policy-minimum-widened'));
  assert.deepEqual(padded.impact.behaviors[0].requiredLanes, ['host', 'ios-simulator']);

  const paddedConsumers = resolveImpact({
    policy,
    proposal: proposal({ affectedConsumers: [':shared', ':shared'] }),
    facts: { reverseClosure: { ':ui-screen-features:save-note': [':androidApp'] } }
  });
  assert.ok(paddedConsumers.impact.selectionReasons.includes('affected-consumer-closure'));
});

await check('resolver output is deterministic byte-for-byte', () => {
  const first = resolveImpact({ policy, proposal: proposal(), facts: {} });
  const second = resolveImpact({ policy, proposal: proposal(), facts: {} });
  assert.equal(first.impact.impactHash, second.impact.impactHash);
  assert.equal(impactContract.canonicalJson(first.impact), impactContract.canonicalJson(second.impact));
});

await check('reverse consumer closure and escalated surfaces widen fail-closed', () => {
  const { impact } = resolveImpact({
    policy,
    proposal: proposal(),
    facts: {
      reverseClosure: { ':ui-screen-features:save-note': [':shared', ':androidApp'] },
      escalatedSurfaces: ['version-catalog']
    }
  });
  assert.deepEqual(impact.affectedConsumers, [':androidApp', ':shared']);
  assert.equal(impact.fullSuiteRequired, true);
  assert.ok(impact.selectionReasons.includes('affected-consumer-closure'));
  assert.ok(impact.selectionReasons.includes('full-suite-escalation'));
  rejects(() => resolveImpact({ policy, proposal: proposal(), facts: { escalatedSurfaces: ['made-up-surface'] } }),
    'TestImpactResolveError', 'FACTS_INVALID');
});

await check('unknown dependencies force the full suite', () => {
  const { impact } = resolveImpact({
    policy, proposal: proposal(), facts: { unknownDependencies: ['dynamic reflection call site'] }
  });
  assert.equal(impact.fullSuiteRequired, true);
  assert.ok(impact.selectionReasons.includes('unknown-dependency-widened'));
});

await check('typed N/A carries exact validators and excludes executable work', () => {
  const { impact } = resolveImpact({
    policy,
    proposal: proposal({ behaviors: [], requiredSuites: [], requiredCapabilities: [], testNotApplicable: 'documentation-only' }),
    facts: {}
  });
  assert.equal(impact.testNotApplicable, 'documentation-only');
  assert.deepEqual(impact.notApplicableValidators, policy.notApplicableValidators);
  rejects(() => resolveImpact({
    policy, proposal: proposal({ testNotApplicable: 'documentation-only' }), facts: {}
  }), 'TestImpactResolveError', 'PROPOSAL_INVALID');
  rejects(() => resolveImpact({
    policy, proposal: proposal({ behaviors: [], requiredSuites: [], testNotApplicable: 'wiring-only' }), facts: {}
  }), 'TestImpactResolveError', 'PROPOSAL_INVALID');
});

await check('impact contract rejects tampering, foreign policy and planned/observed confusion', () => {
  const { impact } = resolveImpact({ policy, proposal: proposal(), facts: {} });
  const copy = JSON.parse(JSON.stringify(impact));
  copy.requiredSuites = [];
  rejects(() => impactContract.validateImpact(copy, { policy }), 'TestImpactError', 'HASH_MISMATCH');
  const foreign = JSON.parse(JSON.stringify(impact));
  foreign.policyHash = H('9');
  rejects(() => impactContract.validateImpact(foreign, { policy }), 'TestImpactError', 'POLICY_MISMATCH');
  const confused = JSON.parse(JSON.stringify(impact));
  confused.behaviors[0].observedTestCases = ['cui.SaveNoteTest.disablesWhileSaving'];
  confused.impactHash = impactContract.impactHashOf(confused);
  rejects(() => impactContract.validateImpact(confused, { policy }), 'TestImpactError', 'IMPACT_INVALID');
});

await check('observed impact may only keep or widen the plan', () => {
  const planned = resolveImpact({ policy, proposal: proposal(), facts: {} }).impact;
  const observedSame = resolveImpact({
    policy, proposal: proposal({ phase: 'observed', behaviors: [{
      ...proposal().behaviors[0], observedTestCases: ['cui.SaveNoteTest.disablesWhileSaving']
    }] }), facts: {}
  }).impact;
  assert.deepEqual(impactContract.checkWidening(planned, observedSame, { policy }), { widened: false });

  const observedWider = resolveImpact({
    policy,
    proposal: proposal({ phase: 'observed', requiredSuites: ['save-note', 'notes-list'], behaviors: [{
      ...proposal().behaviors[0], observedTestCases: ['cui.SaveNoteTest.disablesWhileSaving']
    }] }),
    facts: {}
  }).impact;
  assert.deepEqual(impactContract.checkWidening(planned, observedWider, { policy }), { widened: true });

  const observedNarrow = resolveImpact({
    policy, proposal: proposal({ phase: 'observed', requiredSuites: [], behaviors: [{
      ...proposal().behaviors[0], observedTestCases: ['cui.SaveNoteTest.disablesWhileSaving']
    }] }), facts: {}
  }).impact;
  rejects(() => impactContract.checkWidening(planned, observedNarrow, { policy }), 'TestImpactError', 'IMPACT_NARROWED');

  const observedDrift = resolveImpact({
    policy, proposal: proposal({ phase: 'observed', taskInputHash: H('7'), behaviors: [{
      ...proposal().behaviors[0], observedTestCases: ['cui.SaveNoteTest.disablesWhileSaving']
    }] }), facts: {}
  }).impact;
  rejects(() => impactContract.checkWidening(planned, observedDrift, { policy }), 'TestImpactError', 'WIDENING_INVALID');

  const observedKindDrift = resolveImpact({
    policy, proposal: proposal({ phase: 'observed', behaviors: [{
      ...proposal().behaviors[0], changeKind: 'bugfix', requiredLanes: ['host', 'ios-simulator'],
      observedTestCases: ['cui.SaveNoteTest.disablesWhileSaving']
    }] }), facts: {}
  }).impact;
  rejects(() => impactContract.checkWidening(planned, observedKindDrift, { policy }),
    'TestImpactError', 'IMPACT_NARROWED');

  const plannedWithConsumer = resolveImpact({
    policy, proposal: proposal({ affectedConsumers: [':shared'] }), facts: {}
  }).impact;
  const observedWithoutConsumer = resolveImpact({
    policy, proposal: proposal({ phase: 'observed', affectedConsumers: [], behaviors: [{
      ...proposal().behaviors[0], observedTestCases: ['cui.SaveNoteTest.disablesWhileSaving']
    }] }), facts: {}
  }).impact;
  rejects(() => impactContract.checkWidening(plannedWithConsumer, observedWithoutConsumer, { policy }),
    'TestImpactError', 'IMPACT_NARROWED');
});

await check('planned and observed behavior identities are mandatory for their phase', () => {
  rejects(() => resolveImpact({
    policy,
    proposal: proposal({ behaviors: [{ ...proposal().behaviors[0], proposedTestCases: [] }] }),
    facts: {}
  }), 'TestImpactError', 'IMPACT_INVALID');
  rejects(() => resolveImpact({
    policy,
    proposal: proposal({ phase: 'observed', behaviors: [{
      ...proposal().behaviors[0], observedTestCases: []
    }] }),
    facts: {}
  }), 'TestImpactError', 'IMPACT_INVALID');
});

if (failures.length > 0) {
  console.error(`test-impact-resolver: ${failures.length}/${checks} checks failed`);
  process.exit(1);
}
console.log(`test-impact-resolver: ${checks} checks passed`);
