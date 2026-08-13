#!/usr/bin/env node

// Deterministic certification executor (improvement 05, Phase 3): allowlist,
// process-group isolation, sealed reports, immutable started/terminal
// receipts, redaction and fail-closed zero-test/cache/timeout verdicts.
// Runs against a FAKE gradlew fixture — the real product lanes are proven by
// the generated-fixture spike and product CI, not by this template suite.

import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { chmodSync, existsSync, linkSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createRequire } from 'node:module';
import { createHash } from 'node:crypto';

const require = createRequire(import.meta.url);
const receiptContract = require('../task-test-receipt-contract.cjs');
const registry = require('../task-receipt-registry.cjs');
const snapshotContract = require('../content-snapshot.cjs');
const policyContract = require('../task-test-policy-contract.cjs');
const capabilityContract = require('../task-test-capability-contract.cjs');
const executor = await import('../run-test-certification.mjs');
const aggregator = await import('../aggregate-test-certification.mjs');
const resolver = await import('../resolve-test-impact.mjs');
const requestEntrypoint = await import('../run-test-certification-request.mjs');

const roots = [];
const failures = [];
let checks = 0;

async function check(name, fn) {
  checks++;
  try { await fn(); console.log(`PASS ${name}`); }
  catch (error) { failures.push({ name, error }); console.error(`FAIL ${name}\n${error && error.stack || error}`); }
}

const H = (c) => 'sha256:' + c.repeat(64);
const IDENTITY = { taskStem: 'TASK_5_save_note', runId: 'run-exec1', sessionId: 'sess-1', lockStage: 'run' };
const HASHES = { taskInputHash: H('1'), sourceSnapshotHash: H('2'), impactHash: H('3'), policyHash: H('4') };
const TOOLCHAIN = { gradle: '9.1.0', kotlin: '2.3.21', agp: '9.0.1', jdk: '19', os: 'darwin', arch: 'arm64' };

function fixture({ xml, preexistingXml = null, taskLine = '> Task :lib:testAndroidHostTest', exitCode = 0, sleepSeconds = 0, echo = '', noiseLines = 0, stderrNoiseLines = 0 }) {
  const root = mkdtempSync(join(tmpdir(), 'executor-fixture-'));
  roots.push(root);
  const reportDir = join(root, 'lib', 'build', 'test-results', 'testAndroidHostTest');
  mkdirSync(reportDir, { recursive: true });
  if (preexistingXml !== null) writeFileSync(join(reportDir, 'TEST-spike.xml'), preexistingXml);
  const script = [
    '#!/bin/sh',
    `printf '%s\\n' "$@" > "${join(root, '.gradle-args')}"`,
    sleepSeconds > 0 ? `sleep ${sleepSeconds}` : ':',
    echo ? `echo "${echo}"` : ':',
    noiseLines > 0 ? `yes '> noise line from a long configuration phase ................' | head -n ${noiseLines}` : ':',
    xml !== null ? `cat > "${join(reportDir, 'TEST-spike.xml')}" <<'XML'` : ':',
    xml !== null ? xml : ':',
    xml !== null ? 'XML' : ':',
    // Both streams end mid-line before the task announcement arrives. With a
    // shared partial-line buffer the stderr tail is spliced onto the task line
    // and the disposition is lost.
    stderrNoiseLines > 0 ? `printf 'Configure project :lib'` : ':',
    stderrNoiseLines > 0
      ? `{ yes 'w: warning from an interleaved stderr stream ................' | head -n ${stderrNoiseLines}; printf 'w: trailing partial'; } 1>&2`
      : ':',
    `echo "${taskLine}"`,
    'echo "BUILD DONE"',
    `exit ${exitCode}`
  ].join('\n') + '\n';
  writeFileSync(join(root, 'gradlew'), script);
  chmodSync(join(root, 'gradlew'), 0o755);
  return root;
}

const GREEN_XML = '<testsuite name="spike" tests="3" failures="0" errors="0" skipped="0">' +
  '<testcase classname="spike.CoreTest" name="adds"/><testcase classname="spike.CoreTest" name="greets"/>' +
  '<testcase classname="spike.CoreTest" name="bounds"/></testsuite>';

function options(root, overrides = {}) {
  return {
    certificationRoot: join(root, '.cache-cert'),
    productRoot: root,
    taskPaths: [':lib:testAndroidHostTest'],
    allowedTaskPaths: [':lib:testAndroidHostTest', ':lib:hostTests'],
    suite: 'save-note',
    lane: 'host',
    identity: IDENTITY,
    hashes: HASHES,
    toolchain: TOOLCHAIN,
    reportInputs: [{ path: 'lib/build/test-results/testAndroidHostTest', parser: 'junit-xml' }],
    timeoutMs: 60000,
    ...overrides
  };
}

function rejectsCertification(fn, code) {
  return fn().then(
    () => assert.fail(`expected TestCertificationError ${code}`),
    (error) => {
      assert.equal(error.name, 'TestCertificationError', String(error && error.stack || error));
      assert.equal(error.code, code);
    }
  );
}

await check('green run seals started+terminal receipts, parses counts, verifies through the registry', async () => {
  const root = fixture({ xml: GREEN_XML });
  const { receipt, startedReceipt, evaluation, receiptId } = await executor.certifyCommand(options(root));
  assert.equal(evaluation.passed, true, JSON.stringify(evaluation.violations));
  assert.equal(receipt.counts.discovered, 3);
  assert.equal(receipt.counts.passed, 3);
  assert.deepEqual(receipt.discoveredTestIdentities.slice(0, 1), ['spike.CoreTest.adds']);
  assert.equal(receipt.startedReceiptHash, startedReceipt.receiptHash);
  assert.equal(receipt.disposition, 'executed');
  const commandsDir = join(root, '.cache-cert', IDENTITY.taskStem, IDENTITY.runId, 'commands');
  const files = readdirSync(commandsDir).sort();
  assert.equal(files.length, 2, 'immutable started + terminal receipts');
  const loader = (kind, hash) => {
    const file = files.find((name) => name.includes(hash.slice('sha256:'.length)));
    return file ? JSON.parse(readFileSync(join(commandsDir, file), 'utf8')) : null;
  };
  assert.equal(registry.verifyReceiptId(receiptId, loader).verified, true);
  const forged = JSON.parse(readFileSync(join(commandsDir, files[1]), 'utf8'));
  forged.counts.failed = 0;
  forged.exitCode = 0;
  assert.equal(registry.verifyReceiptId(receiptId, () => forged).verified, true, 'sanity: same bytes verify');
  forged.suite = 'another-suite';
  const verdict = registry.verifyReceiptId(receiptId, () => forged);
  assert.equal(verdict.verified, false, 'a re-keyed receipt never verifies');
});

await check('task text can never become shell: grammar and allowlist are fail-closed before spawn', async () => {
  const root = fixture({ xml: GREEN_XML });
  await rejectsCertification(() => executor.certifyCommand(options(root, {
    taskPaths: [':lib:testAndroidHostTest; rm -rf /']
  })), 'ALLOWLIST_VIOLATION');
  await rejectsCertification(() => executor.certifyCommand(options(root, {
    taskPaths: [':lib:assembleRelease']
  })), 'ALLOWLIST_VIOLATION');
  await rejectsCertification(() => executor.certifyCommand(options(root, {
    allowedTaskPaths: []
  })), 'ALLOWLIST_VIOLATION');
});

await check('zero discovered tests and NO-SOURCE never pass when tests are required', async () => {
  const zero = fixture({ xml: GREEN_XML.replace('tests="3"', 'tests="0"').replace(/<testcase[^/]*\/>/g, '') });
  const zeroRun = await executor.certifyCommand(options(zero, { identity: { ...IDENTITY, runId: 'run-zero' } }));
  assert.equal(zeroRun.evaluation.passed, false);
  assert.ok(zeroRun.evaluation.violations.includes('zero-tests-discovered'));

  const noSource = fixture({ xml: null, taskLine: '> Task :lib:testAndroidHostTest NO-SOURCE' });
  const noSourceRun = await executor.certifyCommand(options(noSource, { identity: { ...IDENTITY, runId: 'run-nosrc' } }));
  assert.equal(noSourceRun.evaluation.passed, false);
  assert.ok(noSourceRun.evaluation.violations.includes('no-source-with-required-tests'));
  assert.equal(noSourceRun.receipt.disposition, 'no-source');
});

await check('cache substitution is refused on the direct certification tier', async () => {
  const cached = fixture({ xml: GREEN_XML, taskLine: '> Task :lib:testAndroidHostTest UP-TO-DATE' });
  const run = await executor.certifyCommand(options(cached, { identity: { ...IDENTITY, runId: 'run-cache' } }));
  assert.equal(run.evaluation.passed, false);
  assert.ok(run.evaluation.violations.includes('cache-substituted-direct-tier'));
  const directArgs = readFileSync(join(cached, '.gradle-args'), 'utf8').trim().split('\n');
  assert.ok(directArgs.includes('--rerun-tasks'), 'direct certification forces execution');
  assert.ok(directArgs.includes('--no-build-cache'), 'direct certification disables build-cache substitution');
  const affected = fixture({ xml: GREEN_XML, taskLine: '> Task :lib:testAndroidHostTest UP-TO-DATE' });
  const affectedRun = await executor.certifyCommand(options(affected, {
    identity: { ...IDENTITY, runId: 'run-cache2' }, tier: 'affected-closure'
  }));
  assert.equal(affectedRun.evaluation.passed, true, 'the affected tier may reuse a snapshot-bound cache');
  const affectedArgs = readFileSync(join(affected, '.gradle-args'), 'utf8').trim().split('\n');
  assert.ok(!affectedArgs.includes('--rerun-tasks') && !affectedArgs.includes('--no-build-cache'),
    'cache-permitted affected closure does not inherit direct-only flags');
});

await check('disposition evidence survives an output cap overflow', async () => {
  // The captured log is head-bounded, so a long build pushes the Gradle task
  // announcement past the cap. Parsing dispositions from the log would then
  // read a cache substitution as a real execution.
  const cached = fixture({
    xml: GREEN_XML, taskLine: '> Task :lib:testAndroidHostTest FROM-CACHE', noiseLines: 80000
  });
  const run = await executor.certifyCommand(options(cached, { identity: { ...IDENTITY, runId: 'run-cap' } }));
  assert.equal(run.receipt.disposition, 'from-cache', 'the task line is evidence, not log tail luck');
  assert.equal(run.evaluation.passed, false);
  assert.ok(run.evaluation.violations.includes('cache-substituted-direct-tier'));

  // stderr interleaves with stdout: a shared partial-line buffer would splice
  // one stream onto the other and swallow the task announcement.
  const interleaved = fixture({
    xml: GREEN_XML, taskLine: '> Task :lib:testAndroidHostTest UP-TO-DATE', stderrNoiseLines: 20000
  });
  const noisy = await executor.certifyCommand(options(interleaved, { identity: { ...IDENTITY, runId: 'run-interleave' } }));
  assert.equal(noisy.receipt.disposition, 'up-to-date', 'stderr traffic cannot hide the task disposition');
  assert.ok(noisy.evaluation.violations.includes('cache-substituted-direct-tier'));
});

await check('report identities beyond the receipt bound are refused with a typed bound error', async () => {
  const cases = Array.from({ length: 4097 },
    (_, index) => `<testcase classname="spike.BigTest" name="case${index}"/>`).join('');
  const oversized = fixture({ xml: `<testsuite name="spike" tests="4097" failures="0" errors="0" skipped="0">${cases}</testsuite>` });
  await rejectsCertification(() => executor.certifyCommand(options(oversized, {
    identity: { ...IDENTITY, runId: 'run-idbound' }
  })), 'REPORT_BOUNDS');
});

await check('ordinal is grammar-checked before it can address a path', async () => {
  const root = fixture({ xml: GREEN_XML });
  await rejectsCertification(() => executor.certifyCommand(options(root, {
    identity: { ...IDENTITY, runId: 'run-ordinal' }, ordinal: '../../TASK_9_other/run-x/000'
  })), 'IDENTITY_INVALID');
  await rejectsCertification(() => executor.certifyStructuralGate({
    certificationRoot: join(root, '.cache-cert'), productRoot: root, gateId: 'docs-contract-gate',
    identity: { ...IDENTITY, runId: 'run-ordinal' }, hashes: HASHES,
    ordinal: '../../TASK_9_other/run-x/000'
  }), 'IDENTITY_INVALID');
});

await check('timeout kills the whole process group and yields a typed violation, never PASS', async () => {
  const slow = fixture({ xml: GREEN_XML, sleepSeconds: 30 });
  const run = await executor.certifyCommand(options(slow, {
    identity: { ...IDENTITY, runId: 'run-timeout' }, timeoutMs: 800
  }));
  assert.equal(run.receipt.timedOut, true);
  assert.equal(run.evaluation.passed, false);
  assert.ok(run.evaluation.violations.includes('timeout'));
  let alive = true;
  try { process.kill(-run.receipt.processGroup, 0); } catch (error) { alive = false; }
  assert.equal(alive, false, 'no orphan survives the escalation');
});

await check('environment is an allowlist and secrets are redacted before persistence', async () => {
  const canary = 'canary-secret-0451-value';
  const root = fixture({ xml: GREEN_XML, echo: canary });
  const run = await executor.certifyCommand(options(root, {
    identity: { ...IDENTITY, runId: 'run-redact' },
    parentEnv: { ...process.env, MY_API_TOKEN: canary }
  }));
  const log = readFileSync(join(root, '.cache-cert', IDENTITY.taskStem, 'run-redact', 'reports', '000-output.log'), 'utf8');
  assert.ok(!log.includes(canary), 'the canary secret never reaches persisted output');
  assert.ok(log.includes('[REDACTED:MY_API_TOKEN]'));
  const env = executor.sanitizedEnv({ PATH: '/bin', MY_API_TOKEN: canary, HOME: '/h' });
  assert.deepEqual(Object.keys(env).sort(), ['HOME', 'PATH'], 'secret env names never pass the allowlist');
  assert.notEqual(executor.envFingerprintOf(env), executor.envFingerprintOf({ PATH: '/bin' }),
    'fingerprint tracks the allowlisted set without exposing values');
});

await check('symlinked reports are rejected and receipts are no-clobber', async () => {
  const root = fixture({ xml: GREEN_XML });
  const reportDir = join(root, 'lib', 'build', 'test-results', 'testAndroidHostTest');
  writeFileSync(join(root, 'outside.xml'), GREEN_XML);
  symlinkSync(join(root, 'outside.xml'), join(reportDir, 'TEST-link.xml'));
  await rejectsCertification(() => executor.certifyCommand(options(root, {
    identity: { ...IDENTITY, runId: 'run-symlink' }
  })), 'REPORT_UNSAFE');
  rmSync(join(reportDir, 'TEST-link.xml'));
  await executor.certifyCommand(options(root, { identity: { ...IDENTITY, runId: 'run-noclobber' } }));
  await rejectsCertification(() => executor.certifyCommand(options(root, {
    identity: { ...IDENTITY, runId: 'run-noclobber' }
  })), 'NO_CLOBBER');
});

await check('pre-existing, hardlinked and symlink-ancestor reports fail closed', async () => {
  const stale = fixture({ xml: null, preexistingXml: GREEN_XML });
  await rejectsCertification(() => executor.certifyCommand(options(stale, {
    identity: { ...IDENTITY, runId: 'run-stale' }
  })), 'REPORT_STALE');

  const hardlinked = fixture({ xml: GREEN_XML });
  const hardlinkDir = join(hardlinked, 'lib', 'build', 'test-results', 'testAndroidHostTest');
  writeFileSync(join(hardlinked, 'outside.xml'), GREEN_XML);
  linkSync(join(hardlinked, 'outside.xml'), join(hardlinkDir, 'TEST-hardlink.xml'));
  await rejectsCertification(() => executor.certifyCommand(options(hardlinked, {
    identity: { ...IDENTITY, runId: 'run-hardlink' }
  })), 'REPORT_UNSAFE');

  const ancestor = fixture({ xml: GREEN_XML });
  rmSync(join(ancestor, 'lib'), { recursive: true, force: true });
  mkdirSync(join(ancestor, 'outside'), { recursive: true });
  symlinkSync(join(ancestor, 'outside'), join(ancestor, 'lib'));
  await rejectsCertification(() => executor.certifyCommand(options(ancestor, {
    identity: { ...IDENTITY, runId: 'run-ancestor' }
  })), 'REPORT_UNSAFE');
});

await check('only the product-root wrapper may execute', async () => {
  const root = fixture({ xml: GREEN_XML });
  await rejectsCertification(() => executor.certifyCommand(options(root, {
    identity: { ...IDENTITY, runId: 'run-foreign-wrapper' }, gradlewPath: '/bin/echo'
  })), 'ALLOWLIST_VIOLATION');
  chmodSync(join(root, 'gradlew'), 0o644);
  await rejectsCertification(() => executor.certifyCommand(options(root, {
    identity: { ...IDENTITY, runId: 'run-nonexec-wrapper' }
  })), 'ALLOWLIST_VIOLATION');
});

await check('redaction masks secret-named parent env values before persistence', () => {
  const masked = executor.redactOutput('token is sk-live-abcdef-0451 end',
    { MY_SECRET: 'sk-live-abcdef-0451', SAFE_VALUE: 'visible' });
  assert.equal(masked, 'token is [REDACTED:MY_SECRET] end');
  assert.equal(executor.redactOutput('visible stays', { SAFE_VALUE: 'visible' }), 'visible stays',
    'non-secret names are never masked');
  assert.equal(executor.redactOutput('tiny', { API_KEY: 'abc' }), 'tiny',
    'sub-6-char values never become redaction patterns');
});

function impactPair({ root, runId, observedTestCases, changeKind = 'di-composition-root',
  requiredLanes = ['host'], requiredCapabilities = ['base'], policy = policyContract.loadPolicy() }) {
  const sourceManifest = snapshotContract.captureSnapshot({ root, paths: ['gradlew'] });
  const base = {
    taskStem: IDENTITY.taskStem, runId, taskInputHash: HASHES.taskInputHash,
    sourceSnapshotHash: sourceManifest.snapshotHash, capabilityInventoryHash: H('5'), moduleGraphHash: H('6'),
    behaviors: [{ anchor: 'test:save-note-works', acceptanceHash: H('7'), ownerBuilder: 'feature-builder',
      ownerModule: ':lib', testLayer: requiredLanes[0], testFile: 'lib/Test.kt', changeKind,
      proposedTestCases: ['spike.CoreTest.adds'], observedTestCases: [], requiredLanes, negativeCases: [] }],
    affectedModules: [':lib'], affectedConsumers: [], requiredSuites: ['save-note'],
    requiredCapabilities, fullSuiteRequired: false, testNotApplicable: null
  };
  const planned = resolver.resolveImpact({ policy, proposal: { ...base, phase: 'planned' }, facts: {} }).impact;
  const observedProposal = {
    ...base, phase: 'observed',
    behaviors: base.behaviors.map((behavior) => ({ ...behavior, observedTestCases }))
  };
  const observed = resolver.resolveImpact({ policy, proposal: observedProposal, facts: {} }).impact;
  return { policy, sourceManifest, planned, observed };
}

await check('aggregator derives PASS from every sealed terminal receipt and seals no-clobber', async () => {
  const root = fixture({ xml: GREEN_XML });
  const evidence = impactPair({ root, runId: 'run-seal', observedTestCases: ['spike.CoreTest.adds'] });
  const run = await executor.certifyCommand(options(root, {
    identity: { ...IDENTITY, runId: 'run-seal' },
    hashes: { ...HASHES, sourceSnapshotHash: evidence.sourceManifest.snapshotHash,
      impactHash: evidence.observed.impactHash, policyHash: evidence.policy.policyHash }
  }));
  const sealed = aggregator.aggregateAndSeal({
    certificationRoot: join(root, '.cache-cert'), productRoot: root,
    identity: { ...IDENTITY, runId: 'run-seal' }, taskInputHash: HASHES.taskInputHash,
    sourceManifest: evidence.sourceManifest, policy: evidence.policy,
    plannedImpact: evidence.planned, observedImpact: evidence.observed
  });
  assert.equal(sealed.summary.verdict, 'PASS');
  assert.deepEqual(sealed.summary.commandReceiptHashes, [run.receipt.receiptHash]);
  assert.match(sealed.receiptId, /^test-summary:sha256:[0-9a-f]{64}$/);
  const runRoot = join(root, '.cache-cert', IDENTITY.taskStem, 'run-seal');
  const onDisk = JSON.parse(readFileSync(join(runRoot, 'summary.json'), 'utf8'));
  assert.equal(onDisk.summaryHash, sealed.summary.summaryHash);
  const loader = (kind, hash) => {
    const fixed = { 'test-summary': 'summary.json', 'test-policy': 'policy.json',
      'source-snapshot': 'source-snapshot.json', 'test-impact-planned': 'planned-impact.json',
      'test-impact-observed': 'observed-impact.json' };
    if (fixed[kind]) return JSON.parse(readFileSync(join(runRoot, fixed[kind]), 'utf8'));
    const family = kind === 'test-command' ? 'commands' : 'structural';
    const file = readdirSync(join(runRoot, family)).find((name) => name.endsWith('-' + hash.slice(7) + '.json'));
    return file ? JSON.parse(readFileSync(join(runRoot, family, file), 'utf8')) : null;
  };
  assert.equal(registry.verifyReceiptId(sealed.receiptId, loader).verified, true,
    'downstream registry reconstructs the entire summary evidence graph');
  await assert.rejects(() => Promise.resolve().then(() => aggregator.aggregateAndSeal({
      certificationRoot: join(root, '.cache-cert'), productRoot: root,
      identity: { ...IDENTITY, runId: 'run-seal' }, taskInputHash: HASHES.taskInputHash,
      sourceManifest: evidence.sourceManifest, policy: evidence.policy,
      plannedImpact: evidence.planned, observedImpact: evidence.observed
    })), (error) => error && error.code === 'NO_CLOBBER');
});

await check('a caller cannot submit a forged PASS summary or cite a missing receipt', () => {
  assert.equal(executor.sealSummary, undefined, 'the arbitrary summary sealing API does not exist');
  const root = fixture({ xml: GREEN_XML });
  const evidence = impactPair({ root, runId: 'run-forged', observedTestCases: ['spike.CoreTest.adds'] });
  mkdirSync(join(root, '.cache-cert', IDENTITY.taskStem, 'run-forged'), { recursive: true });
  const result = aggregator.aggregateAndSeal({
    certificationRoot: join(root, '.cache-cert'), productRoot: root,
    identity: { ...IDENTITY, runId: 'run-forged' }, taskInputHash: HASHES.taskInputHash,
    sourceManifest: evidence.sourceManifest, policy: evidence.policy,
    plannedImpact: evidence.planned, observedImpact: evidence.observed
  });
  assert.equal(result.summary.verdict, 'BLOCKED');
  assert.deepEqual(result.summary.commandReceiptHashes, []);
  assert.ok(!result.summary.commandReceiptHashes.includes(H('a')));
});

await check('aggregator re-hashes sealed reports transitively before deriving a verdict', async () => {
  const root = fixture({ xml: GREEN_XML });
  const evidence = impactPair({ root, runId: 'run-report-tamper', observedTestCases: ['spike.CoreTest.adds'] });
  await executor.certifyCommand(options(root, {
    identity: { ...IDENTITY, runId: 'run-report-tamper' },
    hashes: { ...HASHES, sourceSnapshotHash: evidence.sourceManifest.snapshotHash,
      impactHash: evidence.observed.impactHash, policyHash: evidence.policy.policyHash }
  }));
  const sealedDir = join(root, '.cache-cert', IDENTITY.taskStem, 'run-report-tamper', 'reports', '000-xml');
  const report = join(sealedDir, readdirSync(sealedDir)[0]);
  writeFileSync(report, readFileSync(report, 'utf8').replace('adds', 'evil'));
  assert.throws(() => aggregator.aggregateAndSeal({
    certificationRoot: join(root, '.cache-cert'), productRoot: root,
    identity: { ...IDENTITY, runId: 'run-report-tamper' }, taskInputHash: HASHES.taskInputHash,
    sourceManifest: evidence.sourceManifest, policy: evidence.policy,
    plannedImpact: evidence.planned, observedImpact: evidence.observed
  }), (error) => error && error.code === 'ARTIFACT_HASH_MISMATCH');
});

await check('policy-only evidence cannot disappear into a false-green aggregate', async () => {
  // A bugfix certifies on its regression evidence: the policy may not demand a
  // red/green pair while nothing can produce one (that would block the kind
  // outright, not certify it). The guard below stays wired for the day the
  // producer lands — a kind that demands the class still cannot reach PASS.
  const bugfixRoot = fixture({ xml: GREEN_XML });
  const bugfixEvidence = impactPair({ root: bugfixRoot, runId: 'run-bugfix-gap',
    observedTestCases: ['spike.CoreTest.adds'], changeKind: 'bugfix', requiredLanes: ['common'] });
  await executor.certifyCommand(options(bugfixRoot, {
    identity: { ...IDENTITY, runId: 'run-bugfix-gap' }, lane: 'common',
    hashes: { ...HASHES, sourceSnapshotHash: bugfixEvidence.sourceManifest.snapshotHash,
      impactHash: bugfixEvidence.observed.impactHash, policyHash: bugfixEvidence.policy.policyHash }
  }));
  const bugfix = aggregator.aggregateAndSeal({
    certificationRoot: join(bugfixRoot, '.cache-cert'), productRoot: bugfixRoot,
    identity: { ...IDENTITY, runId: 'run-bugfix-gap' }, taskInputHash: HASHES.taskInputHash,
    sourceManifest: bugfixEvidence.sourceManifest, policy: bugfixEvidence.policy,
    plannedImpact: bugfixEvidence.planned, observedImpact: bugfixEvidence.observed
  });
  assert.equal(bugfix.summary.verdict, 'PASS', 'bugfix certifies on its regression evidence');
  assert.deepEqual(bugfix.summary.failBeforePassAfter, []);

  const redGreenPolicy = JSON.parse(JSON.stringify(bugfixEvidence.policy));
  redGreenPolicy.changeKinds.bugfix.minimumEvidence = ['fail-before-pass-after', 'regression-test'];
  redGreenPolicy.policyHash = policyContract.policyHashOf(redGreenPolicy);
  const demanded = impactPair({ root: bugfixRoot, runId: 'run-bugfix-demand', policy: redGreenPolicy,
    observedTestCases: ['spike.CoreTest.adds'], changeKind: 'bugfix', requiredLanes: ['common'] });
  await executor.certifyCommand(options(bugfixRoot, {
    identity: { ...IDENTITY, runId: 'run-bugfix-demand' }, lane: 'common',
    hashes: { ...HASHES, sourceSnapshotHash: demanded.sourceManifest.snapshotHash,
      impactHash: demanded.observed.impactHash, policyHash: demanded.policy.policyHash }
  }));
  const demandedResult = aggregator.aggregateAndSeal({
    certificationRoot: join(bugfixRoot, '.cache-cert'), productRoot: bugfixRoot,
    identity: { ...IDENTITY, runId: 'run-bugfix-demand' }, taskInputHash: HASHES.taskInputHash,
    sourceManifest: demanded.sourceManifest, policy: demanded.policy,
    plannedImpact: demanded.planned, observedImpact: demanded.observed
  });
  assert.equal(demandedResult.summary.verdict, 'BLOCKED',
    'a kind demanding red/green still blocks until a producer seals the pair');

  const coverageRoot = fixture({ xml: GREEN_XML });
  const coverageEvidence = impactPair({ root: coverageRoot, runId: 'run-coverage-gap',
    observedTestCases: ['spike.CoreTest.adds'], requiredCapabilities: ['coverage'] });
  await executor.certifyCommand(options(coverageRoot, {
    identity: { ...IDENTITY, runId: 'run-coverage-gap' },
    hashes: { ...HASHES, sourceSnapshotHash: coverageEvidence.sourceManifest.snapshotHash,
      impactHash: coverageEvidence.observed.impactHash, policyHash: coverageEvidence.policy.policyHash }
  }));
  const coverage = aggregator.aggregateAndSeal({
    certificationRoot: join(coverageRoot, '.cache-cert'), productRoot: coverageRoot,
    identity: { ...IDENTITY, runId: 'run-coverage-gap' }, taskInputHash: HASHES.taskInputHash,
    sourceManifest: coverageEvidence.sourceManifest, policy: coverageEvidence.policy,
    plannedImpact: coverageEvidence.planned, observedImpact: coverageEvidence.observed
  });
  assert.equal(coverage.summary.verdict, 'BLOCKED', 'coverage capability needs a sealed Kover report');
  assert.equal(coverage.summary.coverage, null);
});

await check('junit parser reads failures and identities exactly', () => {
  const parsed = executor.parseJUnitXml('<testsuite tests="2" failures="1" errors="0" skipped="0">' +
    '<testcase classname="a.B" name="ok"/><testcase classname="a.B" name="bad"><failure/></testcase></testsuite>');
  assert.deepEqual(parsed, { tests: 2, failures: 1, errors: 0, skipped: 0, identities: ['a.B.ok', 'a.B.bad'] });
  // Attribute names are matched on a boundary: a vendor attribute that merely
  // ends with the wanted name must not be read as the count itself.
  const decorated = executor.parseJUnitXml('<testsuite data_failures="0" tests="2" failures="1" errors="0" skipped="0">' +
    '<testcase classname="a.B" name="ok"/><testcase classname="a.B" name="bad"><failure/></testcase></testsuite>');
  assert.equal(decorated.failures, 1, 'data_failures is not failures');
  const evalFail = receiptContract.evaluateCommandReceipt;
  assert.equal(typeof evalFail, 'function');
  assert.throws(() => executor.parseJUnitXml('<testsuite tests="3" failures="0" errors="0" skipped="0">' +
    '<testcase classname="a.B" name="only"/></testsuite>'),
  (error) => error && error.code === 'REPORT_INVALID');
  assert.throws(() => executor.parseJUnitXml('<testsuite tests="1" failures="1" errors="1" skipped="0">' +
    '<testcase classname="a.B" name="bad"/></testsuite>'),
  (error) => error && error.code === 'REPORT_INVALID');
});

await check('all declared lane report adapters dispatch explicitly and unknown adapters fail closed', () => {
  for (const parser of ['junit-xml', 'kotlin-native-xml', 'android-connected-xml', 'roborazzi-report']) {
    assert.equal(executor.parseReport(GREEN_XML, parser).tests, 3, parser);
  }
  assert.throws(() => executor.parseReport(GREEN_XML, 'implicit-xml'),
    (error) => error && error.code === 'REPORT_INPUT_INVALID');
});

await check('structural receipt producer selects a closed gate command and seals both stages', async () => {
  const root = fixture({ xml: GREEN_XML });
  mkdirSync(join(root, 'orchestrator', 'skills', 'checks'), { recursive: true });
  writeFileSync(join(root, 'orchestrator', 'skills', 'checks', 'check_links.py'),
    '#!/usr/bin/env python3\nprint("links-ok")\n');
  const result = await executor.certifyStructuralGate({
    certificationRoot: join(root, '.cache-cert'), productRoot: root,
    gateId: 'link-integrity-gate', identity: { ...IDENTITY, runId: 'run-struct' }, hashes: HASHES
  });
  assert.equal(result.passed, true);
  assert.equal(result.receipt.gateId, 'link-integrity-gate');
  const receipts = readdirSync(join(root, '.cache-cert', IDENTITY.taskStem, 'run-struct', 'structural'));
  assert.equal(receipts.length, 2);
  await rejectsCertification(() => executor.certifyStructuralGate({
    certificationRoot: join(root, '.cache-cert'), productRoot: root,
    gateId: 'caller-supplied-command', identity: { ...IDENTITY, runId: 'run-struct-bad' }, hashes: HASHES
  }), 'STRUCTURAL_GATE_INVALID');
});

await check('typed N/A is derived only after every policy structural producer passes', async () => {
  const root = fixture({ xml: GREEN_XML });
  mkdirSync(join(root, 'orchestrator', 'skills', 'checks'), { recursive: true });
  mkdirSync(join(root, 'orchestrator', 'tasks'), { recursive: true });
  for (const name of ['check_links.py', 'check_self_contained_content.py']) {
    writeFileSync(join(root, 'orchestrator', 'skills', 'checks', name), '#!/usr/bin/env python3\nprint("ok")\n');
  }
  writeFileSync(join(root, 'orchestrator', 'tasks', 'validate-task-state.mjs'), 'process.exit(0);\n');
  const policy = policyContract.loadPolicy();
  const sourceManifest = snapshotContract.captureSnapshot({ root, paths: ['gradlew'] });
  const proposal = {
    taskStem: IDENTITY.taskStem, runId: 'run-typed-na', taskInputHash: HASHES.taskInputHash,
    sourceSnapshotHash: sourceManifest.snapshotHash, capabilityInventoryHash: H('5'), moduleGraphHash: H('6'),
    behaviors: [], affectedModules: [], affectedConsumers: [], requiredSuites: [], requiredCapabilities: [],
    fullSuiteRequired: false, testNotApplicable: 'documentation-only'
  };
  const planned = resolver.resolveImpact({ policy, proposal: { ...proposal, phase: 'planned' }, facts: {} }).impact;
  const observed = resolver.resolveImpact({ policy, proposal: { ...proposal, phase: 'observed' }, facts: {} }).impact;
  const identity = { ...IDENTITY, runId: 'run-typed-na' };
  const hashes = { ...HASHES, sourceSnapshotHash: sourceManifest.snapshotHash, policyHash: policy.policyHash };
  for (let index = 0; index < observed.notApplicableValidators.length; index++) {
    await executor.certifyStructuralGate({
      certificationRoot: join(root, '.cache-cert'), productRoot: root,
      gateId: observed.notApplicableValidators[index], identity, hashes, ordinal: String(index).padStart(3, '0')
    });
  }
  const result = aggregator.aggregateAndSeal({
    certificationRoot: join(root, '.cache-cert'), productRoot: root, identity,
    taskInputHash: HASHES.taskInputHash, sourceManifest, policy, plannedImpact: planned, observedImpact: observed
  });
  assert.equal(result.summary.verdict, 'SKIPPED');
  assert.equal(result.summary.structuralReceiptHashes.length, 3);
  assert.deepEqual(result.summary.commandReceiptHashes, []);
});

await check('canonical request CLI is active and rejects any caller-supplied summary surface', () => {
  const root = fixture({ xml: GREEN_XML });
  writeFileSync(join(root, 'request.json'), JSON.stringify({ version: 1, summary: { verdict: 'PASS' } }));
  const cli = spawnSync(process.execPath,
    [new URL('../run-test-certification-request.mjs', import.meta.url).pathname, '--request', 'request.json'],
    { cwd: root, encoding: 'utf8' });
  assert.equal(cli.status, 1);
  assert.match(cli.stderr, /REQUEST_INVALID/);
  assert.ok(!cli.stderr.includes('library for the deterministic pipeline'));
});

await check('canonical request CLI recomputes taskInputHash from the current todo bytes', () => {
  const root = fixture({ xml: GREEN_XML });
  const stem = 'TASK_5_save_note';
  const runId = 'run-stale-input';
  const sessionId = 'ws-fixture-session-000000000000';
  mkdirSync(join(root, 'orchestrator', 'tasks', 'todo'), { recursive: true });
  mkdirSync(join(root, 'orchestrator', '.cache', 'tasks', 'locks'), { recursive: true });
  writeFileSync(join(root, 'orchestrator', 'tasks', 'todo', stem + '.md'),
    '# TASK 5 — Save note\n\n## Goal\n- Save the current note.\n');
  const startedAt = '2026-08-02T10:00:00.000Z';
  const lock = {
    version: 1, stem, stage: 'orchestrator', runId, sessionId, startedAt,
    owner: { kind: 'direct', id: 'fixture:stale-input', pid: process.pid,
      processStartId: null, hostname: 'fixture-host', startedAt }
  };
  const lockBytes = Buffer.from(JSON.stringify(lock, null, 2) + '\n');
  writeFileSync(join(root, 'orchestrator', '.cache', 'tasks', 'locks', stem + '.json'), lockBytes);
  const lockHash = 'sha256:' + createHash('sha256').update(lockBytes).digest('hex');
  const request = {
    version: 1,
    identity: { taskStem: stem, runId, sessionId, lockStage: 'orchestrator', lockHash },
    taskInputHash: H('1'),
    sourceManifestPath: 'missing/source.json',
    plannedImpactPath: 'missing/planned.json',
    observedImpactPath: 'missing/observed.json',
    capabilityInventoryPath: 'missing/inventory.json',
    commands: [], structuralGateIds: [],
    toolchain: TOOLCHAIN,
    executionRootKind: 'shared-serial'
  };
  writeFileSync(join(root, 'request-stale.json'), JSON.stringify(request, null, 2) + '\n');
  const cli = spawnSync(process.execPath,
    [new URL('../run-test-certification-request.mjs', import.meta.url).pathname,
      '--request', 'request-stale.json'],
    { cwd: root, encoding: 'utf8' });
  assert.equal(cli.status, 1, cli.stderr + cli.stdout);
  assert.match(cli.stderr, /TASK_INPUT_STALE/);
  assert.equal(existsSync(join(root, 'orchestrator', '.cache', 'tasks', 'test-certification')), false);
});

await check('canonical request plan admits only sealed structural-only bootstrap execution without commands', () => {
  const policy = policyContract.loadPolicy();
  const inventory = {
    version: 1,
    domain: 'test-capability-inventory',
    generatedBy: ':testCapabilityInventory',
    modules: [],
    inventoryHash: H('0'),
  };
  inventory.inventoryHash = capabilityContract.inventoryHashOf(inventory);
  const structuralObserved = {
    capabilityInventoryHash: inventory.inventoryHash,
    behaviors: [{ requiredLanes: ['structural'] }],
    requiredSuites: [],
    fullSuiteRequired: false,
    testNotApplicable: null,
  };
  const allowed = requestEntrypoint.validateExecutionPlan({
    commands: [],
    structuralGateIds: ['bootstrap-foundation-fixture'],
  }, structuralObserved, inventory, policy);
  assert.ok(allowed.includes(':allConfiguredTests'));

  assert.throws(() => requestEntrypoint.validateExecutionPlan({
    commands: [],
    structuralGateIds: [],
  }, {
    ...structuralObserved,
    behaviors: [{ requiredLanes: ['host'] }],
    requiredSuites: ['host-suite'],
  }, inventory, policy), (error) => error && error.code === 'PLAN_INCOMPLETE');
});

for (const root of roots) rmSync(root, { recursive: true, force: true });

if (failures.length > 0) {
  console.error(`test-executor: ${failures.length}/${checks} checks failed`);
  process.exit(1);
}
console.log(`test-executor: ${checks} checks passed`);
