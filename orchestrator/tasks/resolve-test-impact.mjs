#!/usr/bin/env node

// ---------------------------------------------------------------------------
// Deterministic test-impact resolver (improvement 05, Phase 3).
//
// The model/planner PROPOSES a behavior map; this resolver applies the
// machine policy table after that output and may only widen:
//   - per-behavior lanes are unioned with the change kind's defaultLanes;
//   - reverse consumer closure is unioned into affectedConsumers;
//   - any escalated surface or unknown dependency forces the full suite;
//   - a narrower model suggestion never survives — the widening is recorded
//     in selectionReasons, which IS the deterministic check of the model's
//     test-layer selection.
// Output is an immutable, hash-bound artifact validated by
// task-test-impact-contract.cjs. No filesystem, no clock, no randomness —
// identity comes entirely from the inputs.
// ---------------------------------------------------------------------------

import { createRequire } from 'node:module';
import path from 'node:path';
import process from 'node:process';
import fs from 'node:fs';

const require = createRequire(import.meta.url);
const impactContract = require('./task-test-impact-contract.cjs');
const policyContract = require('./task-test-policy-contract.cjs');

const HERE = path.dirname(new URL(import.meta.url).pathname);

class TestImpactResolveError extends Error {
  constructor(code, message) {
    super(code + ': ' + message);
    this.name = 'TestImpactResolveError';
    this.code = code;
  }
}

function fail(code, message) { throw new TestImpactResolveError(code, message); }

function sortedUnique(values) {
  return [...new Set(values)].sort();
}

export function resolveImpact({ policy, proposal, facts }) {
  const activePolicy = policy || policyContract.loadPolicy();
  if (!proposal || typeof proposal !== 'object' || Array.isArray(proposal)) fail('PROPOSAL_INVALID', 'proposal must be an object');
  const {
    taskStem, runId, phase, taskInputHash, sourceSnapshotHash,
    capabilityInventoryHash, moduleGraphHash, behaviors = [],
    affectedModules = [], affectedConsumers = [], requiredSuites = [],
    requiredCapabilities = [], testNotApplicable = null
  } = proposal;
  const { escalatedSurfaces = [], reverseClosure = {}, unknownDependencies = [] } = facts || {};

  for (const surface of escalatedSurfaces) {
    if (!activePolicy.escalationRules.includes(surface)) fail('FACTS_INVALID', 'unknown escalation surface: ' + surface);
  }

  const reasons = new Set(['planner-proposed']);
  let widened = false;

  const resolvedBehaviors = behaviors.map((behavior) => {
    if (!behavior || typeof behavior !== 'object') fail('PROPOSAL_INVALID', 'behavior must be an object');
    const kind = activePolicy.changeKinds[behavior.changeKind];
    if (!kind) fail('PROPOSAL_INVALID', 'unknown change kind: ' + behavior.changeKind);
    // Compare against the deduplicated proposal: a padded duplicate must not
    // hide the fact that the policy minimum had to widen the lane set.
    const proposedLanes = sortedUnique(behavior.requiredLanes || []);
    const lanes = sortedUnique([...proposedLanes, ...kind.defaultLanes]);
    if (lanes.length > proposedLanes.length) {
      widened = true;
      reasons.add('policy-minimum-widened');
    }
    return {
      anchor: behavior.anchor,
      acceptanceHash: behavior.acceptanceHash,
      changeKind: behavior.changeKind,
      ownerBuilder: behavior.ownerBuilder,
      ownerModule: behavior.ownerModule,
      testLayer: behavior.testLayer,
      testFile: behavior.testFile,
      proposedTestCases: [...(behavior.proposedTestCases || [])],
      observedTestCases: phase === 'observed' ? [...(behavior.observedTestCases || [])] : [],
      requiredLanes: lanes,
      negativeCases: [...(behavior.negativeCases || [])]
    };
  });

  const closureConsumers = [];
  for (const module of affectedModules) {
    for (const consumer of reverseClosure[module] || []) closureConsumers.push(consumer);
  }
  const proposedConsumers = sortedUnique(affectedConsumers);
  const resolvedConsumers = sortedUnique([...proposedConsumers, ...closureConsumers]);
  if (resolvedConsumers.length > proposedConsumers.length) {
    widened = true;
    reasons.add('affected-consumer-closure');
  }

  let fullSuiteRequired = proposal.fullSuiteRequired === true;
  if (escalatedSurfaces.length > 0) {
    if (!fullSuiteRequired) widened = true;
    fullSuiteRequired = true;
    reasons.add('full-suite-escalation');
  }
  if (unknownDependencies.length > 0) {
    if (!fullSuiteRequired) widened = true;
    fullSuiteRequired = true;
    reasons.add('unknown-dependency-widened');
  }

  let notApplicableValidators = [];
  if (testNotApplicable !== null) {
    if (!activePolicy.testNotApplicable.includes(testNotApplicable)) {
      fail('PROPOSAL_INVALID', 'testNotApplicable outside the allowlist: ' + testNotApplicable);
    }
    if (resolvedBehaviors.length > 0 || requiredSuites.length > 0 || fullSuiteRequired) {
      fail('PROPOSAL_INVALID', 'a typed N/A cannot coexist with executable requirements');
    }
    notApplicableValidators = [...activePolicy.notApplicableValidators];
  }

  const impact = {
    version: 1,
    policyVersion: activePolicy.version,
    policyHash: activePolicy.policyHash,
    taskStem,
    runId,
    phase,
    taskInputHash,
    sourceSnapshotHash,
    capabilityInventoryHash,
    moduleGraphHash,
    behaviors: resolvedBehaviors,
    affectedModules: sortedUnique(affectedModules),
    affectedConsumers: resolvedConsumers,
    requiredSuites: sortedUnique(requiredSuites),
    fullSuiteRequired,
    requiredCapabilities: sortedUnique(requiredCapabilities),
    testNotApplicable,
    notApplicableValidators,
    unknownDependencies: [...unknownDependencies],
    selectionReasons: sortedUnique([...reasons]),
    impactHash: 'sha256:' + '0'.repeat(64)
  };
  impact.impactHash = impactContract.impactHashOf(impact);
  const validated = impactContract.validateImpact(impact, { policy: activePolicy });
  return { impact: validated, widened };
}

function cliMain(argv) {
  const [inputPath, outputPath] = argv;
  if (!inputPath) fail('CLI_INVALID', 'usage: resolve-test-impact.mjs <input.json> [output.json]');
  const input = JSON.parse(fs.readFileSync(inputPath, 'utf8'));
  const { impact, widened } = resolveImpact(input);
  const serialized = JSON.stringify(impact, null, 2) + '\n';
  if (outputPath) fs.writeFileSync(outputPath, serialized, { flag: 'wx' });
  else process.stdout.write(serialized);
  process.stderr.write('widened: ' + widened + '\n');
  return 0;
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.join(HERE, 'resolve-test-impact.mjs')) {
  try { process.exit(cliMain(process.argv.slice(2))); }
  catch (error) {
    process.stderr.write(String(error && error.message || error) + '\n');
    process.exit(1);
  }
}
