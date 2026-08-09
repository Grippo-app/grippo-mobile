#!/usr/bin/env node

// Contract tests for the machine test-policy authority (pipeline
// improvement 05, Phase 1): orchestrator/tasks/test-policy.json +
// task-test-policy-contract.cjs + test-capability-inventory.schema.json.
// Runtime contract modules live in the task- namespace because a bare
// `test-*.cjs/.mjs` basename trips the fail-closed test-like filename guard.
// The policy is the ONLY machine-readable source of change kinds, minimum
// evidence, lanes, N/A values, escalation rules and reason codes; consumers
// carry its version/hash, never prose copies.

import assert from 'node:assert/strict';
import { existsSync, linkSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, symlinkSync, unlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const TASKS_DIR = join(HERE, '..');
const require = createRequire(import.meta.url);
const contract = require('../task-test-policy-contract.cjs');
const builderReportContract = require('../task-builder-report-contract.cjs');
const capabilityContract = require('../task-test-capability-contract.cjs');
const foundationContract = require('../task-test-foundation-contract.cjs');
const foundation = await import('../task-test-foundation.mjs');

const failures = [];
let checks = 0;

async function check(name, fn) {
  checks++;
  try {
    await fn();
    console.log(`PASS ${name}`);
  } catch (error) {
    failures.push({ name, error });
    console.error(`FAIL ${name}\n${error && error.stack || error}`);
  }
}

function freshPolicy() {
  return JSON.parse(readFileSync(join(TASKS_DIR, 'test-policy.json'), 'utf8'));
}

function rejects(fn, code) {
  try { fn(); }
  catch (error) {
    assert.equal(error.name, 'TestPolicyError', String(error && error.stack || error));
    assert.equal(error.code, code);
    return;
  }
  assert.fail(`expected TestPolicyError ${code}`);
}

function rejectsBuilderReport(value) {
  assert.throws(
    () => builderReportContract.validateBuilderReport(value, { policy: contract.loadPolicy() }),
    (error) => error && error.name === 'BuilderReportError' && error.code === 'BUILDER_REPORT_INVALID'
  );
}

function walkFileNames(root) {
  const found = [];
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === '.cache') continue;
    const absolute = join(root, entry.name);
    if (entry.isDirectory()) found.push(...walkFileNames(absolute));
    else if (entry.isFile()) found.push(absolute);
  }
  return found;
}

await check('protocol, schema, fixture and test filenames have no generation suffixes', () => {
  const orchestrator = join(TASKS_DIR, '..');
  const offenders = walkFileNames(orchestrator).filter((file) =>
    /(?:^|[-_.])v[0-9]+(?:[-_.]|$)/i.test(file.slice(orchestrator.length + 1)));
  assert.deepEqual(offenders, []);
});

await check('canonical policy loads, hash-verifies and freezes', () => {
  const policy = contract.loadPolicy();
  assert.equal(policy.version, 1);
  assert.equal(policy.domain, 'test-policy');
  assert.equal(contract.policyHashOf(policy), policy.policyHash);
  assert.equal(Object.keys(policy.changeKinds).length, 22);
  assert.ok(Object.isFrozen(policy) && Object.isFrozen(policy.changeKinds));
  assert.deepEqual(policy.lanes,
    ['android-device', 'common', 'host', 'ios-simulator', 'screenshot', 'structural']);
});

await check('builder report has one strict current protocol and unversioned fixtures', () => {
  const policy = contract.loadPolicy();
  const fixtures = join(TASKS_DIR, '..', 'contracts', 'fixtures');
  const schema = JSON.parse(readFileSync(join(TASKS_DIR, '..', 'contracts', 'builder-report.schema.json'), 'utf8'));
  const valid = JSON.parse(readFileSync(join(fixtures, 'builder-report.valid.json'), 'utf8'));
  const invalid = JSON.parse(readFileSync(join(fixtures, 'builder-report.invalid.json'), 'utf8'));
  assert.equal(schema.properties.schemaVersion.const, 1);
  assert.equal(schema.additionalProperties, false);
  assert.deepEqual(Object.keys(schema.properties).sort(), [...builderReportContract.TOP_FIELDS].sort());
  assert.equal(builderReportContract.validateBuilderReport(valid, { policy }).schemaVersion, 1);
  rejectsBuilderReport(invalid);
  const cli = join(TASKS_DIR, 'task-builder-report-contract.cjs');
  const accepted = spawnSync(process.execPath, [cli], { input: JSON.stringify(valid), encoding: 'utf8' });
  assert.equal(accepted.status, 0, accepted.stderr);
  assert.equal(JSON.parse(accepted.stdout).schemaVersion, 1);
  const refused = spawnSync(process.execPath, [cli], { input: JSON.stringify(invalid), encoding: 'utf8' });
  assert.equal(refused.status, 1);

  const mutate = (fn) => {
    const copy = JSON.parse(JSON.stringify(valid));
    fn(copy);
    rejectsBuilderReport(copy);
  };
  mutate((value) => { value.schemaVersion = 2; });
  mutate((value) => { value.compatibility = 'old-reader'; });
  mutate((value) => { delete value.test_cases; });
  mutate((value) => { value.test_cases[0].lane = 'jvm-desktop'; });
  mutate((value) => { value.test_cases[0].file = '../outside.kt'; });
  mutate((value) => { value.test_cases[0].file = 'unreported/Test.kt'; });
  mutate((value) => { value.status = 'failed'; });
  assert.throws(() => builderReportContract.parseBuilderReport('{broken', { policy }),
    (error) => error && error.code === 'BUILDER_REPORT_INVALID');
});

await check('anchor and reason-code grammars are anchored and behave', () => {
  const policy = contract.loadPolicy();
  const anchor = new RegExp(policy.anchorGrammar);
  assert.ok(anchor.test('test:save-note-failure-keeps-cache'));
  assert.ok(anchor.test('test:a1'));
  for (const bad of ['test:', 'test:Save-Note', 'test:save_note', 'anchor:save', ' test:save', 'test:save ']) {
    assert.ok(!anchor.test(bad), bad);
  }
  const reason = new RegExp(policy.reasonCodeGrammar);
  assert.ok(reason.test('policy-minimum-widened'));
  assert.ok(!reason.test('Policy Minimum'));
  for (const code of policy.selectionReasonCodes) assert.ok(reason.test(code), code);
});

await check('policy semantics: bugfix regression evidence, docs-only N/A, lane closure', () => {
  const policy = contract.loadPolicy();
  assert.deepEqual(contract.minimumEvidenceFor('bugfix'), ['regression-test']);
  // `fail-before-pass-after` stays a declared class but no kind may demand it
  // while no producer can run a red test against an exact baseline; demanding
  // it would block every task of that kind instead of certifying it.
  assert.ok(policy.evidenceClasses.includes('fail-before-pass-after'));
  for (const [kindId, kind] of Object.entries(policy.changeKinds)) {
    assert.ok(!kind.minimumEvidence.includes('fail-before-pass-after'),
      `${kindId} demands red/green evidence that has no producer yet`);
  }
  assert.deepEqual(contract.minimumEvidenceFor('documentation-only'), []);
  for (const [kindId, kind] of Object.entries(policy.changeKinds)) {
    if (kindId !== 'documentation-only') {
      assert.ok(kind.minimumEvidence.length > 0, `${kindId} must demand evidence`);
    }
    for (const lane of kind.defaultLanes) assert.ok(policy.lanes.includes(lane), `${kindId}:${lane}`);
  }
  assert.deepEqual(contract.minimumEvidenceFor('room-schema'),
    ['behavioral-test', 'platform-fidelity-test']);
  assert.deepEqual(policy.changeKinds['compose-ui'].defaultLanes, ['host', 'ios-simulator'],
    'Compose UI lanes are the proven pair: Robolectric host entry + iOS simulator; ' +
    'CMP 1.10.3 cannot configure withDeviceTest on a Compose module');
  assert.match(policy.changeKinds['compose-ui'].notes, /uiTest tree/);
  assert.match(policy.changeKinds['compose-ui'].notes, /Robolectric/);
  assert.ok(contract.isAllowedNotApplicable('documentation-only'));
  assert.ok(!contract.isAllowedNotApplicable('simple-change'));
  assert.ok(!contract.isAllowedNotApplicable('builder-decided'));
  rejects(() => contract.minimumEvidenceFor('invented-kind'), 'UNKNOWN_CHANGE_KIND');
});

await check('every tamper and shape drift is rejected fail-closed', () => {
  const tamper = (mutate, code) => {
    const copy = freshPolicy();
    mutate(copy);
    rejects(() => contract.validatePolicy(copy), code);
  };
  tamper((p) => { p.extra = true; }, 'POLICY_INVALID');
  tamper((p) => { delete p.escalationRules; }, 'POLICY_INVALID');
  tamper((p) => { p.version = 2; }, 'POLICY_INVALID');
  tamper((p) => { p.domain = 'test-policy-unknown'; }, 'POLICY_INVALID');
  tamper((p) => { p.lanes = [...p.lanes].reverse(); }, 'POLICY_INVALID');
  tamper((p) => { p.lanes = [...p.lanes, p.lanes[0]].sort(); }, 'POLICY_INVALID');
  tamper((p) => { p.testNotApplicable = [...p.testNotApplicable, 'simple-change'].sort(); }, 'POLICY_INVALID');
  tamper((p) => { p.notApplicableValidators = ['unknown-gate']; }, 'POLICY_INVALID');
  tamper((p) => { p.changeKinds.bugfix.minimumEvidence = ['invented-class']; }, 'POLICY_INVALID');
  tamper((p) => { p.changeKinds.bugfix.defaultLanes = ['jvm-desktop']; }, 'POLICY_INVALID');
  tamper((p) => { p.changeKinds.bugfix.minimumEvidence = []; }, 'POLICY_INVALID');
  tamper((p) => { p.changeKinds.mapper.extra = 1; }, 'POLICY_INVALID');
  tamper((p) => { p.anchorGrammar = 'test:[a-z]+'; }, 'POLICY_INVALID');
  tamper((p) => { p.flakyPolicy.maxDiagnosticRetries = 3; }, 'POLICY_INVALID');
  tamper((p) => { p.flakyPolicy.failThenPassVerdict = 'PASS'; }, 'POLICY_INVALID');
  tamper((p) => { p.changeKinds.bugfix.notes = 'weakened'; p.policyHash = p.policyHash; }, 'HASH_MISMATCH');
  tamper((p) => { p.policyHash = 'sha256:' + 'a'.repeat(64); }, 'HASH_MISMATCH');
});

await check('capability inventory schema is strict and pins the frozen lane names', () => {
  const schema = JSON.parse(readFileSync(join(TASKS_DIR, 'test-capability-inventory.schema.json'), 'utf8'));
  assert.equal(schema.properties.version.const, 1);
  assert.equal(schema.properties.domain.const, 'test-capability-inventory');
  assert.equal(schema.additionalProperties, false);
  const moduleSchema = schema.properties.modules.items;
  assert.equal(moduleSchema.additionalProperties, false);
  assert.deepEqual(Object.keys(moduleSchema.properties.lanes.properties),
    ['host', 'ios-simulator', 'android-device', 'screenshot']);
  assert.equal(moduleSchema.properties.lanes.additionalProperties, false);
  assert.equal(schema.$defs.lane.additionalProperties, false);
  assert.deepEqual(moduleSchema.properties.capabilities.items.enum,
    ['base', 'compose-ui', 'coroutines', 'coverage', 'di', 'flow', 'network', 'room', 'screenshot']);
  // Frozen Phase-1 spike decisions live in the schema description so every
  // consumer reads one pinned vocabulary.
  for (const pinned of ['testAndroidHostTest', 'androidHostTest', 'hostTest',
    'iosSimulatorArm64Test', 'connectedAndroidDeviceTest', 'androidDeviceTest',
    'deviceTest', 'withDeviceTest', 'withHostTest',
    'recordRoborazziAndroidHostTest', 'verifyRoborazziAndroidHostTest']) {
    assert.ok(schema.description.includes(pinned), pinned);
  }
  assert.ok(schema.description.includes('false-green'),
    'the provider-based androidDeviceCheck trap stays documented');
  assert.ok(schema.description.includes('does NOT include `commonTest`'),
    'the standalone deviceTest compilation topology stays documented');
});

await check('capability inventory runtime validates hashes and owns the exact task allowlist', () => {
  const inventory = {
    version: 1,
    domain: 'test-capability-inventory',
    generatedBy: ':testCapabilityInventory',
    modules: [{
      path: ':core', capabilities: ['base', 'room'],
      lanes: {
        host: { taskPath: ':core:testAndroidHostTest', sourceSet: 'androidHostTest', compilation: 'hostTest' },
        'android-device': { taskPath: ':core:connectedAndroidDeviceTest', sourceSet: 'androidDeviceTest', compilation: 'deviceTest' }
      }
    }],
    inventoryHash: 'sha256:' + '0'.repeat(64)
  };
  inventory.inventoryHash = capabilityContract.inventoryHashOf(inventory);
  assert.deepEqual(capabilityContract.allowedTaskPaths(inventory),
    [':core:connectedAndroidDeviceTest', ':core:testAndroidHostTest']);
  const tampered = JSON.parse(JSON.stringify(inventory));
  tampered.modules[0].lanes.host.taskPath = ':core:assemble';
  assert.throws(() => capabilityContract.validateInventory(tampered),
    (error) => error && error.name === 'TestCapabilityError' && error.code === 'HASH_MISMATCH');
  const unbased = JSON.parse(JSON.stringify(inventory));
  unbased.modules[0].capabilities = ['room'];
  unbased.inventoryHash = capabilityContract.inventoryHashOf(unbased);
  assert.throws(() => capabilityContract.validateInventory(unbased),
    (error) => error && error.code === 'INVENTORY_INVALID');
});

await check('policy consumers get the version/hash pointer, not prose copies', () => {
  const policy = contract.loadPolicy();
  // The machine policy file itself is the only place these enum tables live
  // under orchestrator/tasks/: the contract exposes lookups instead of copies.
  assert.equal(typeof contract.minimumEvidenceFor, 'function');
  assert.equal(typeof contract.isAllowedNotApplicable, 'function');
  assert.match(policy.policyHash, /^sha256:[0-9a-f]{64}$/);
  // Domain separation: the policy hash is never a plain content hash.
  const plain = contract.canonicalJson({ ...policy, policyHash: undefined });
  assert.notEqual(policy.policyHash, 'sha256:' + plain);
});

// ---------------------------------------------------------------------------
// Foundation doctor + bootstrap coordinator (Phase 2 primitives).
// ---------------------------------------------------------------------------

const roots = [];
function tempRoot(prefix) {
  const root = mkdtempSync(join(tmpdir(), prefix));
  roots.push(root);
  return root;
}

const CATALOG_OK = [
  'agp = "9.0.1"', 'kotlin = "2.3.21"',
  'kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }',
  'kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }',
  'turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }',
  'ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }',
  'koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin" }',
  'androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }',
  'androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidx-test-runner" }',
  'androidx-test-core = { module = "androidx.test:core", version.ref = "androidx-test-core" }',
  'compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest", version.ref = "compose-ui-test-manifest" }'
].join('\n') + '\n';

const CONVENTION_FILES = [
  'KmpTestConventionPlugin.kt', 'CoroutinesTestConventionPlugin.kt', 'FlowTestConventionPlugin.kt',
  'NetworkTestConventionPlugin.kt', 'DiTestConventionPlugin.kt', 'RoomTestConventionPlugin.kt',
  'ComposeUiTestConventionPlugin.kt', 'TestCapabilityEntryTask.kt'
];
const REGISTRATIONS = [
  'kmp.test.convention', 'coroutines.test.convention', 'flow.test.convention',
  'network.test.convention', 'di.test.convention', 'room.test.convention', 'compose.ui.test.convention'
].map((id) => `register("${id}") { id = "${id}" }`).join('\n') + '\n';
const ROOT_BUILD_OK = [
  'allHostTests', 'allIosSimulatorTests', 'allAndroidDeviceTests',
  'allScreenshotTests', 'allConfiguredTests', 'testCapabilityInventory'
].map((name) => `tasks.register("${name}") {}`).join('\n') + '\n';

function productFixture({ complete }) {
  const root = tempRoot('foundation-doctor-');
  writeFileSync(join(root, 'settings.gradle.kts'),
    'plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0" }\nrootProject.name = "fixture"\n');
  mkdirSync(join(root, 'gradle'), { recursive: true });
  mkdirSync(join(root, 'build-logic', 'convention', 'src', 'main', 'kotlin'), { recursive: true });
  if (!complete) {
    writeFileSync(join(root, 'gradle', 'libs.versions.toml'), 'agp = "9.0.1"\nkotlin = "2.3.21"\n');
    writeFileSync(join(root, 'build-logic', 'convention', 'build.gradle.kts'), 'gradlePlugin {}\n');
    writeFileSync(join(root, 'build.gradle.kts'), '// no aggregates yet\n');
    return root;
  }
  writeFileSync(join(root, 'gradle', 'libs.versions.toml'), CATALOG_OK);
  writeFileSync(join(root, 'build-logic', 'convention', 'build.gradle.kts'), REGISTRATIONS);
  writeFileSync(join(root, 'build.gradle.kts'), ROOT_BUILD_OK);
  for (const name of CONVENTION_FILES) {
    const body = name === 'KmpTestConventionPlugin.kt' ? 'withHostTest { }\n' : '// capability\n';
    writeFileSync(join(root, 'build-logic', 'convention', 'src', 'main', 'kotlin', name), body);
  }
  return root;
}

function rejectsFoundation(fn, code) {
  try { fn(); }
  catch (error) {
    assert.equal(error.name, 'TestFoundationError', String(error && error.stack || error));
    assert.equal(error.code, code);
    return;
  }
  assert.fail(`expected TestFoundationError ${code}`);
}

await check('doctor returns READY only for the complete lean foundation', () => {
  const ready = foundation.doctor({ productRoot: productFixture({ complete: true }) });
  assert.equal(ready.state, 'READY');
  assert.match(ready.doctorInventoryHash, /^sha256:[0-9a-f]{64}$/);
  assert.equal(ready.inventory.singleHostOwner, true);
  const absent = foundation.doctor({ productRoot: productFixture({ complete: false }) });
  assert.equal(absent.state, 'ABSENT_CAN_INSTALL');
  assert.notEqual(absent.doctorInventoryHash, ready.doctorInventoryHash);
});

await check('doctor typed failures: partial, conflicting, unsupported, toolchain, not-a-product', () => {
  const partial = productFixture({ complete: true });
  rmSync(join(partial, 'build-logic', 'convention', 'src', 'main', 'kotlin', 'FlowTestConventionPlugin.kt'));
  assert.equal(foundation.doctor({ productRoot: partial }).state, 'PARTIAL_CORRUPT');

  const secondOwner = productFixture({ complete: true });
  writeFileSync(join(secondOwner, 'build-logic', 'convention', 'src', 'main', 'kotlin', 'RoomTestConventionPlugin.kt'),
    'withHostTest { }\n');
  assert.equal(foundation.doctor({ productRoot: secondOwner }).state, 'PARTIAL_CORRUPT',
    'a second withHostTest owner is corruption, not READY');

  const conflicting = productFixture({ complete: true });
  writeFileSync(join(conflicting, 'gradle', 'libs.versions.toml'),
    CATALOG_OK + 'kotest = { module = "io.kotest:kotest-runner-junit5", version = "6.0.0" }\n');
  assert.equal(foundation.doctor({ productRoot: conflicting }).state, 'CONFLICTING_STACK');

  const unsupported = productFixture({ complete: true });
  writeFileSync(join(unsupported, 'gradle', 'libs.versions.toml'), CATALOG_OK.replace('agp = "9.0.1"', 'agp = "8.13.0"'));
  assert.equal(foundation.doctor({ productRoot: unsupported }).state, 'UNSUPPORTED_VERSION');

  const noToolchain = productFixture({ complete: true });
  writeFileSync(join(noToolchain, 'settings.gradle.kts'), 'rootProject.name = "fixture"\n');
  assert.equal(foundation.doctor({ productRoot: noToolchain }).state, 'TOOLCHAIN_UNAVAILABLE');

  rejectsFoundation(() => foundation.doctor({ productRoot: tempRoot('not-a-product-') }), 'NOT_A_PRODUCT');
});

await check('a foreign host-test enabler is never reported as safe to install into', () => {
  // Both halves of the single-owner audit: the scan must reach a package
  // sub-directory, and "nothing installed yet" must not out-rank a foreign
  // enabler — installing on top of one creates the second withHostTest owner
  // AGP fails on.
  const nested = productFixture({ complete: true });
  mkdirSync(join(nested, 'build-logic', 'convention', 'src', 'main', 'kotlin', 'com', 'acme'), { recursive: true });
  writeFileSync(join(nested, 'build-logic', 'convention', 'src', 'main', 'kotlin', 'com', 'acme', 'HostTestHelper.kt'),
    'withHostTest { }\n');
  const nestedDoctor = foundation.doctor({ productRoot: nested });
  assert.equal(nestedDoctor.inventory.singleHostOwner, false, 'a sub-directory enabler is still an owner');
  assert.equal(nestedDoctor.state, 'PARTIAL_CORRUPT');

  const bare = productFixture({ complete: false });
  mkdirSync(join(bare, 'build-logic', 'convention', 'src', 'main', 'kotlin'), { recursive: true });
  writeFileSync(join(bare, 'build-logic', 'convention', 'src', 'main', 'kotlin', 'AndroidLibraryConventionPlugin.kt'),
    'withHostTest { }\n');
  assert.equal(foundation.doctor({ productRoot: bare }).state, 'PARTIAL_CORRUPT',
    'an absent foundation with a foreign enabler is corruption, not ABSENT_CAN_INSTALL');
});

await check('frozen vocabulary pins: policy constants, doctor states, marker protocol', () => {
  assert.equal(contract.POLICY_VERSION, 1);
  assert.equal(contract.POLICY_DOMAIN, 'test-policy');
  assert.equal(contract.loadPolicy({ path: contract.POLICY_PATH }).version, 1,
    'POLICY_PATH is the canonical machine authority location');
  assert.deepEqual(foundationContract.DOCTOR_STATES,
    ['READY', 'ABSENT_CAN_INSTALL', 'PARTIAL_CORRUPT', 'CONFLICTING_STACK',
      'UNSUPPORTED_VERSION', 'TOOLCHAIN_UNAVAILABLE']);
  assert.deepEqual(foundationContract.MARKER_PHASES,
    ['claimed', 'child-created', 'child-promoted', 'ready'],
    'bounded phases are frozen and strictly ordered');
  assert.equal(foundationContract.INTENT_DOMAIN, 'test-foundation-intent');
  assert.equal(foundationContract.INVENTORY_DOMAIN, 'test-foundation-inventory');
  assert.deepEqual([...foundationContract.MARKER_KEYS].sort(), [...foundationContract.MARKER_KEYS],
    'marker keys are the exact sorted contract');
  assert.ok(foundationContract.MARKER_KEYS.includes('ownerSessionId') &&
    foundationContract.MARKER_KEYS.includes('markerHash'));
});

await check('foundation intent hash is domain-separated and deterministic', () => {
  const policyHash = contract.loadPolicy().policyHash;
  const inventoryHash = 'sha256:' + 'a'.repeat(64);
  const intent = foundationContract.foundationIntentHash(policyHash, inventoryHash);
  assert.equal(intent, foundationContract.foundationIntentHash(policyHash, inventoryHash));
  assert.notEqual(intent, foundationContract.foundationIntentHash(policyHash, 'sha256:' + 'b'.repeat(64)));
  assert.equal(foundationContract.markerPathComponent(intent).length, 64);
  rejectsFoundation(() => foundationContract.markerPathComponent('sha256:зло'), 'INTENT_INVALID');
});

await check('coordinator marker: singleton claim, bounded owner-only phases, immutable child', () => {
  const cacheRoot = tempRoot('foundation-marker-');
  const intentHash = foundationContract.foundationIntentHash(
    contract.loadPolicy().policyHash, 'sha256:' + 'c'.repeat(64));
  const sessionId = 'test-session-1';

  const first = foundation.claimFoundation({ cacheRoot, intentHash, sessionId });
  assert.equal(first.claimed, true);
  assert.equal(first.marker.phase, 'claimed');
  const second = foundation.claimFoundation({ cacheRoot, intentHash, sessionId: 'rival-session' });
  assert.equal(second.claimed, false);
  assert.equal(second.code, 'FOUNDATION_IN_PROGRESS');
  assert.equal(second.marker.ownerSessionId, sessionId, 'the loser sees the exact owner, never adopts');

  rejectsFoundation(() => foundation.advanceFoundation({
    cacheRoot, intentHash, sessionId: 'rival-session', phase: 'child-created', childStem: 'TASK_9_evil'
  }), 'NOT_OWNER');
  rejectsFoundation(() => foundation.advanceFoundation({
    cacheRoot, intentHash, sessionId, phase: 'child-promoted', childStem: 'TASK_9_skip'
  }), 'TRANSITION_INVALID');

  const created = foundation.advanceFoundation({
    cacheRoot, intentHash, sessionId, phase: 'child-created', childStem: 'TASK_9_test_foundation'
  });
  assert.equal(created.marker.childStem, 'TASK_9_test_foundation');
  rejectsFoundation(() => foundation.advanceFoundation({
    cacheRoot, intentHash, sessionId, phase: 'child-promoted', childStem: 'TASK_10_other'
  }), 'TRANSITION_INVALID');
  const promoted = foundation.advanceFoundation({ cacheRoot, intentHash, sessionId, phase: 'child-promoted' });
  assert.equal(promoted.marker.phase, 'child-promoted');
  const ready = foundation.advanceFoundation({ cacheRoot, intentHash, sessionId, phase: 'ready' });
  assert.equal(ready.marker.phase, 'ready');
  rejectsFoundation(() => foundation.advanceFoundation({
    cacheRoot, intentHash, sessionId, phase: 'ready'
  }), 'TRANSITION_INVALID');

  const inspected = foundation.inspectFoundation({ cacheRoot, intentHash });
  assert.equal(inspected.present, true);
  assert.equal(inspected.marker.phase, 'ready');
  assert.equal(inspected.ownerAlive, true, 'this test process is the live owner');
});

await check('a crashed advance leaves no staging file that can wedge the intent', () => {
  const cacheRoot = tempRoot('foundation-staging-');
  const intentHash = foundationContract.foundationIntentHash(
    contract.loadPolicy().policyHash, 'sha256:' + 'e'.repeat(64));
  const sessionId = 'staging-session';
  assert.equal(foundation.claimFoundation({ cacheRoot, intentHash, sessionId }).claimed, true);

  // Exactly what a SIGKILL between staging and rename leaves behind. There is
  // no recovery verb by design, so a stale own-format temp must not become a
  // permanent EEXIST on every later advance.
  const dir = join(cacheRoot, 'tasks', 'test-foundation');
  const staging = join(dir, foundationContract.markerPathComponent(intentHash) + '.json.next');
  writeFileSync(staging, '{"partial":true}\n', { mode: 0o600 });
  const advanced = foundation.advanceFoundation({
    cacheRoot, intentHash, sessionId, phase: 'child-created', childStem: 'TASK_11_test_foundation'
  });
  assert.equal(advanced.marker.phase, 'child-created');
  assert.equal(existsSync(staging), false, 'the staging path is consumed, never accumulated');

  // A foreign-shaped leftover is still fail-closed: only our own exact
  // 0600 single-link regular file may be discarded.
  symlinkSync(join(cacheRoot, 'elsewhere.json'), staging);
  rejectsFoundation(() => foundation.advanceFoundation({
    cacheRoot, intentHash, sessionId, phase: 'child-promoted'
  }), 'UNSAFE_PATH');
  // unlink, not rm: the symlink dangles (elsewhere.json never exists), and on
  // darwin rmSync stats through the link, sees ENOENT and silently no-ops,
  // leaving the link in place. unlink removes the link itself.
  unlinkSync(staging);
});

await check('coordinator marker storage is fail-closed against symlinks and tampering', () => {
  const cacheRoot = tempRoot('foundation-unsafe-');
  const intentHash = foundationContract.foundationIntentHash(
    contract.loadPolicy().policyHash, 'sha256:' + 'd'.repeat(64));
  const dir = join(cacheRoot, 'tasks', 'test-foundation');
  mkdirSync(dir, { recursive: true });
  const component = foundationContract.markerPathComponent(intentHash) + '.json';
  symlinkSync(join(cacheRoot, 'elsewhere.json'), join(dir, component));
  rejectsFoundation(() => foundation.inspectFoundation({ cacheRoot, intentHash }), 'UNSAFE_PATH');
  // unlink, not rm: the dangling symlink survives rmSync on darwin (stat
  // through the link → ENOENT → silent no-op), which then wedges the claim
  // below with EEXIST + ELOOP — the exact fail-closed behavior under test,
  // fired at the wrong moment.
  unlinkSync(join(dir, component));

  const claimed = foundation.claimFoundation({ cacheRoot, intentHash, sessionId: 'tamper-session' });
  assert.equal(claimed.claimed, true);
  const onDisk = JSON.parse(readFileSync(join(dir, component), 'utf8'));
  onDisk.phase = 'ready';
  writeFileSync(join(dir, component), JSON.stringify(onDisk, null, 2) + '\n');
  rejectsFoundation(() => foundation.inspectFoundation({ cacheRoot, intentHash }), 'MARKER_INVALID',
  );
  const shapeValid = JSON.parse(readFileSync(join(dir, component), 'utf8'));
  shapeValid.phase = 'claimed';
  shapeValid.ownerPid = shapeValid.ownerPid + 1;
  writeFileSync(join(dir, component), JSON.stringify(shapeValid, null, 2) + '\n');
  rejectsFoundation(() => foundation.inspectFoundation({ cacheRoot, intentHash }), 'HASH_MISMATCH');

  const hardlinkRoot = tempRoot('foundation-hardlink-');
  const hardlinkIntent = foundationContract.foundationIntentHash(
    contract.loadPolicy().policyHash, 'sha256:' + 'e'.repeat(64));
  foundation.claimFoundation({ cacheRoot: hardlinkRoot, intentHash: hardlinkIntent, sessionId: 'hardlink-session' });
  const hardlinkDir = join(hardlinkRoot, 'tasks', 'test-foundation');
  const hardlinkMarker = join(hardlinkDir, foundationContract.markerPathComponent(hardlinkIntent) + '.json');
  linkSync(hardlinkMarker, join(hardlinkDir, 'second-link.json'));
  rejectsFoundation(() => foundation.inspectFoundation({ cacheRoot: hardlinkRoot, intentHash: hardlinkIntent }), 'UNSAFE_PATH');

  const ancestorRoot = tempRoot('foundation-ancestor-');
  const outside = tempRoot('foundation-outside-');
  mkdirSync(join(ancestorRoot, 'tasks'), { recursive: true });
  mkdirSync(join(outside, 'test-foundation'), { recursive: true });
  symlinkSync(join(outside, 'test-foundation'), join(ancestorRoot, 'tasks', 'test-foundation'));
  rejectsFoundation(() => foundation.claimFoundation({
    cacheRoot: ancestorRoot, intentHash: hardlinkIntent, sessionId: 'ancestor-session'
  }), 'UNSAFE_PATH');
});

for (const root of roots) rmSync(root, { recursive: true, force: true });

if (failures.length > 0) {
  console.error(`test-policy-contract: ${failures.length}/${checks} checks failed`);
  process.exit(1);
}
console.log(`test-policy-contract: ${checks} checks passed`);
