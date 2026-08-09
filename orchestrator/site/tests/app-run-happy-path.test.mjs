import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'app-run-happy-'));
const orchestrator = path.join(root, 'orchestrator');
const cache = path.join(orchestrator, '.cache');
const apkRoot = path.join(root, 'androidApp', 'build', 'outputs', 'apk', 'debug');
fs.mkdirSync(apkRoot, { recursive: true });
fs.mkdirSync(path.join(root, 'androidApp', 'src', 'main'), { recursive: true });
fs.mkdirSync(orchestrator, { recursive: true });
fs.writeFileSync(path.join(orchestrator, 'project-config.md'), `---
productName: Runner fixture
applicationId: com.example.runner
iosEnabled: false
androidAssembleTask: :androidApp:assembleDebug
---
`);
fs.writeFileSync(path.join(root, 'androidApp', 'src', 'main', 'Fixture.kt'), 'package com.example.runner\n');
fs.writeFileSync(path.join(apkRoot, 'fixture-debug.apk'), Buffer.from('verified-apk-fixture'));
fs.writeFileSync(path.join(apkRoot, 'output-metadata.json'), JSON.stringify({
  version: 3,
  elements: [{ type: 'SINGLE', filters: [], outputFile: 'fixture-debug.apk' }],
}) + '\n');

process.env.ORCHESTRATOR_PROJECT_ROOT = root;
process.env.ORCHESTRATOR_CACHE_DIR = cache;
process.env.ORCHESTRATOR_APP_RUN_DIR = path.join(cache, 'runtime', 'app-run');
process.env.ORCHESTRATOR_APP_RUN_AUTHORITY_ROOT = root;
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = path.join(cache, 'tasks', 'finalizations');
process.env.ORCHESTRATOR_WRITER_LEASES_DIR = path.join(cache, 'tasks', 'finalizations', '.writers');
process.env.ORCHESTRATOR_WRITER_AUTHORITY_ROOT = root;
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = path.join(cache, 'tasks', 'creations');
process.env.ORCHESTRATOR_TASK_EDITS_DIR = path.join(cache, 'tasks', 'edits');

const tools = {
  sdkRoot: null,
  adb: '/fixture/bin/adb',
  emulator: '/fixture/bin/emulator',
  avdmanager: null,
  sdkmanager: null,
  apkanalyzer: '/fixture/bin/apkanalyzer',
  aapt: null,
  gradlew: '/fixture/project/gradlew',
};
const invocations = [];
let emulatorStarted = false;
let releaseBuild;
const buildGate = new Promise((resolve) => { releaseBuild = resolve; });
function result(stdout = '', stderr = '', ok = true) {
  return { ok, status: ok ? 0 : 1, signal: null, timedOut: false, stdout, stderr, errorCode: null };
}
const fakeRunner = {
  runSync(spec) {
    invocations.push({ mode: 'sync', executable: spec.executable, argv: spec.argv.slice() });
    const args = spec.argv.join(' ');
    if (spec.executable === tools.adb && args === 'devices -l') {
      return result(emulatorStarted
        ? 'List of devices attached\nemulator-5554 device product:sdk model:Pixel_8 transport_id:1\n'
        : 'List of devices attached\n');
    }
    if (spec.executable === tools.adb && args === 'devices') {
      return result(emulatorStarted
        ? 'List of devices attached\nemulator-5554\tdevice\n'
        : 'List of devices attached\n');
    }
    if (spec.executable === tools.emulator && args === '-list-avds') return result('Pixel_8_API_35\n');
    if (spec.executable === tools.adb && args.includes('emu avd name')) return result('Pixel_8_API_35\nOK\n');
    if (spec.executable === tools.adb && args.includes('sys.boot_completed')) return result('1\n');
    if (spec.executable === tools.adb && args.includes('ro.build.version.release')) return result('15\n');
    if (spec.executable === tools.adb && args.includes('ro.product.cpu.abi')) return result('x86_64\n');
    if (spec.executable === tools.apkanalyzer) return result('com.example.runner\n');
    return result('');
  },
  async run(spec) {
    invocations.push({
      mode: 'async',
      executable: spec.executable,
      argv: spec.argv.slice(),
      buildFence: typeof spec.beforeSpawn === 'function' && typeof spec.onSpawn === 'function',
    });
    const args = spec.argv.join(' ');
    if (spec.executable === tools.gradlew) await buildGate;
    if (typeof spec.onLine === 'function') {
      spec.onLine('stdout', 'resolved device emulator-5554 safely');
    }
    if (args.includes('resolve-activity')) return result('com.example.runner/.MainActivity\n');
    if (args.includes('pidof')) return result('4242\n');
    return result('Success\n');
  },
  async runBinary(spec) {
    invocations.push({ mode: 'binary', executable: spec.executable, argv: spec.argv.slice() });
    const png = Buffer.alloc(24);
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]).copy(png);
    png.write('IHDR', 12, 'ascii');
    png.writeUInt32BE(1080, 16);
    png.writeUInt32BE(1920, 20);
    return { ok: true, status: 0, bytes: png, stderr: '', errorCode: null };
  },
  startDetached() {
    emulatorStarted = true;
    return {
      pid: 4242,
      processStartId: 'psid-v1:' + process.platform + ':' + 'a'.repeat(64),
    };
  },
  terminateIdentity() {
    emulatorStarted = false;
    return true;
  },
};

const paths = require('../server/paths.js');
const runner = require('../server/app-runner.js');
const androidRunner = require('../server/android-runner.js');
const storage = require('../server/app-run-storage.js');
const writerLeases = require('../../tasks/writer-leases.cjs');
const finalizations = require('../server/finalizations.js');

async function waitFor(predicate, label) {
  for (let attempt = 0; attempt < 200; attempt++) {
    const value = predicate();
    if (value) return value;
    await new Promise((resolve) => setTimeout(resolve, 10));
  }
  const status = runner.status();
  const logs = status.job ? runner.logs({ jobId: status.job.jobId, sessionId: null, cursor: null, limit: 100 }) : null;
  throw new Error('timed out waiting for ' + label + '\n' + JSON.stringify({ status, logs }, null, 2));
}

try {
  runner.init({ commandRunner: fakeRunner, androidTools: tools });
  const targetResponse = runner.targets('android', true);
  assert.equal(targetResponse.ok, true);
  const targets = targetResponse.targets;
  const android = targets.platforms[0];
  assert.equal(android.availability, 'available');
  assert.equal(android.devices.length, 1);
  assert.equal(JSON.stringify(targets).includes('emulator-5554'), false);

  const startBody = {
    platform: 'android',
    targetId: android.devices[0].id,
    discoveryRevision: targets.discoveryRevision,
    variantId: 'debug',
    buildMode: 'rebuild',
    taskStem: null,
    surfaceId: null,
    expectedProjectSourceRevision: targets.projectSourceRevision,
    confirmationToken: null,
    whenBusy: 'queue',
    idempotencyKey: 'happy-path-start',
  };
  const started = runner.start(startBody);
  assert.equal(started.status, 202);
  assert.equal(runner.start(startBody).job.jobId, started.job.jobId);
  await waitFor(() => invocations.some((row) => row.executable === tools.gradlew), 'build admission');
  const activeRuntimeLease = writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT).active;
  assert.equal(activeRuntimeLease.length, 1);
  assert.equal(activeRuntimeLease[0].kind, 'runtime-build');
  assert.equal(invocations.find((row) => row.executable === tools.gradlew).buildFence, true,
    'the real build invocation must publish and bind its process generation to the runtime lease');
  const overlappingTaskWriter = finalizations.beginMutation({
    kind: 'task-session',
    stem: 'TASK_90_overlap',
    sessionId: finalizations.createWriterSessionId(),
    key: 'task:TASK_90_overlap',
    pendingChild: false,
  });
  assert.equal(overlappingTaskWriter.ok, false,
    'a published runtime-build lease must reject every later task writer');
  releaseBuild();

  const running = await waitFor(() => {
    const status = runner.status();
    return status.session && status.session.state === 'running' ? status : null;
  }, 'running session');
  assert.equal(running.job.state, 'running');
  assert.equal(running.job.result.launched, true);
  assert.equal(running.job.result.pidObserved, true);
  assert.equal(running.session.applicationId, 'com.example.runner');
  assert.deepEqual(
    runner._runtime.jobs.get(running.job.jobId).processIdentities,
    [{
      kind: 'emulator',
      pid: 4242,
      processStartId: 'psid-v1:' + process.platform + ':' + 'a'.repeat(64),
    }],
    'the exact detached emulator identity must be durably bound to its private job record',
  );
  assert.equal(JSON.stringify(running).includes('emulator-5554'), false);
  assert.equal(JSON.stringify(runner.logs({
    jobId: running.job.jobId, sessionId: null, cursor: null, limit: 100,
  })).includes('emulator-5554'), false);
  assert.equal(writerLeases.scan(paths.WRITER_LEASES_DIR, paths.WRITER_AUTHORITY_ROOT).active.length, 0,
    'the runtime-build lease must be released after launch');
  const secondWhileRunning = runner.start({
    ...startBody,
    idempotencyKey: 'second-while-running',
  });
  assert.equal(secondWhileRunning.error, 'app-run-active',
    'the first release must keep exactly one active app target');

  assert.throws(() => androidRunner.resolveArtifact({
    tools: { apkanalyzer: tools.apkanalyzer, aapt: null },
    commandRunner: {
      runSync() { return result('com.example.substituted\n'); },
    },
    applicationId: 'com.example.runner',
    targetArchitecture: 'x86_64',
  }, {
    module: 'androidApp',
    id: 'debug',
  }), (error) => error && error.code === 'artifact-invalid',
  'an APK with a substituted application id must never be installed');
  const canonicalOutputMetadata = fs.readFileSync(
    path.join(apkRoot, 'output-metadata.json'), 'utf8',
  );
  fs.writeFileSync(path.join(apkRoot, 'output-metadata.json'), JSON.stringify({
    version: 3,
    elements: [{ type: 'SINGLE', filters: {}, outputFile: 'fixture-debug.apk' }],
  }) + '\n');
  assert.throws(() => androidRunner.resolveArtifact({
    tools: { apkanalyzer: tools.apkanalyzer, aapt: null },
    commandRunner: {
      runSync() { return result('com.example.runner\n'); },
    },
    applicationId: 'com.example.runner',
    targetArchitecture: 'x86_64',
  }, {
    module: 'androidApp',
    id: 'debug',
  }), /invalid element/,
  'malformed AGP filter metadata must not be reinterpreted as a universal APK');
  fs.writeFileSync(path.join(apkRoot, 'output-metadata.json'), canonicalOutputMetadata);

  const screenshotBody = {
    sessionId: running.session.sessionId,
    expectedSessionRevision: running.session.sessionRevision,
    taskStem: null,
    surfaceId: null,
    idempotencyKey: 'happy-path-screenshot',
  };
  assert.deepEqual(await runner.screenshot({
    ...screenshotBody,
    expectedSessionRevision: 0,
    idempotencyKey: 'bad-screenshot-revision',
  }), {
    ok: false,
    status: 400,
    error: 'bad-screenshot-request',
  });
  const firstShot = await runner.screenshot(screenshotBody);
  const duplicateShot = await runner.screenshot(screenshotBody);
  assert.equal(firstShot.status, 201);
  assert.equal(duplicateShot.screenshot.screenshotId, firstShot.screenshot.screenshotId);
  assert.equal(runner.screenshotFile(firstShot.screenshot.screenshotId).bytes.length, 24);
  const screenshotMetadata = storage.readJson(
    paths.APP_RUN_SCREENSHOTS_DIR, firstShot.screenshot.screenshotId, 128 * 1024,
  );
  storage.writeJson(paths.APP_RUN_SCREENSHOTS_DIR, screenshotMetadata.screenshotId, {
    ...screenshotMetadata,
    width: screenshotMetadata.width + 1,
  }, 128 * 1024);
  assert.equal(runner.screenshotFile(firstShot.screenshot.screenshotId), null,
    'screenshot metadata dimensions must match the PNG IHDR exactly');
  assert.deepEqual(runner.screenshotRead(firstShot.screenshot.screenshotId), {
    ok: false,
    status: 409,
    error: 'runtime-recovery-required',
  }, 'published screenshot corruption must never be disguised as a missing screenshot');
  assert.ok(runner.status().integrity.issues.some((issue) =>
    issue.code === 'screenshot-recovery-invalid' &&
      issue.recordId === firstShot.screenshot.screenshotId),
  'observed screenshot corruption must latch runtime recovery');
  storage.writeJson(
    paths.APP_RUN_SCREENSHOTS_DIR, screenshotMetadata.screenshotId, screenshotMetadata, 128 * 1024,
  );
  runner._runtime.integrityIssues = [];
  for (let index = 1; index <= 49; index++) {
    const cloneId = storage.randomId('shot');
    storage.writeFileAtomic(
      storage.fileFor(paths.APP_RUN_SCREENSHOTS_DIR, cloneId, '.png'),
      runner.screenshotFile(firstShot.screenshot.screenshotId).bytes,
      25 * 1024 * 1024,
    );
    storage.writeJson(paths.APP_RUN_SCREENSHOTS_DIR, cloneId, {
      ...screenshotMetadata,
      screenshotId: cloneId,
      createdAt: new Date(Date.parse(screenshotMetadata.createdAt) + index * 1000).toISOString(),
    }, 128 * 1024);
  }
  assert.deepEqual(await runner.screenshot({
    ...screenshotBody,
    idempotencyKey: 'screenshot-session-capacity',
  }), {
    ok: false,
    status: 409,
    error: 'screenshot-capacity',
  }, 'a session must stop at its explicit screenshot evidence cap');
  for (let index = 50; index <= 51; index++) {
    const cloneId = storage.randomId('shot');
    storage.writeFileAtomic(
      storage.fileFor(paths.APP_RUN_SCREENSHOTS_DIR, cloneId, '.png'),
      runner.screenshotFile(firstShot.screenshot.screenshotId).bytes,
      25 * 1024 * 1024,
    );
    storage.writeJson(paths.APP_RUN_SCREENSHOTS_DIR, cloneId, {
      ...screenshotMetadata,
      screenshotId: cloneId,
      sessionId: 'session-' + String(index).padStart(36, '0'),
      createdAt: new Date(Date.parse(screenshotMetadata.createdAt) + index * 1000).toISOString(),
    }, 128 * 1024);
  }
  const evidenceReceiptId = storage.randomId('receipt');
  storage.writeJson(paths.APP_RUN_HISTORY_DIR, evidenceReceiptId, {
    schemaVersion: 1,
    receiptId: evidenceReceiptId,
    taskStem: 'TASK_1_fixture',
    taskSourceRevision: 'sha256:' + '1'.repeat(64),
    runJobId: running.job.jobId,
    sessionId: running.session.sessionId,
    platform: 'android',
    deviceSummary: running.session.deviceSummary,
    artifactId: running.session.artifactId,
    appProjectSourceRevision: running.session.appProjectSourceRevision,
    checklist: [{
      itemId: 'manual-' + '2'.repeat(24),
      result: 'pass',
      note: null,
      screenshotIds: [firstShot.screenshot.screenshotId],
    }],
    overall: 'passed',
    staleSource: false,
    staleTask: false,
    createdAt: new Date().toISOString(),
  }, 128 * 1024);

  const stopBody = {
    sessionId: running.session.sessionId,
    expectedSessionRevision: running.session.sessionRevision,
    idempotencyKey: 'happy-path-stop',
  };
  const stopped = await runner.stop(stopBody);
  const duplicateStop = await runner.stop(stopBody);
  assert.equal(stopped.ok, true);
  assert.equal(stopped.session.state, 'stopped');
  assert.equal(duplicateStop.session.state, 'stopped');
  assert.equal(runner.status().job.state, 'completed');

  const lateSwapTargets = runner.targets('android', true).targets;
  const buildCountBeforeLateSwap = invocations.filter(
    (row) => row.executable === tools.gradlew,
  ).length;
  const lateSwapStart = runner.start({
    ...startBody,
    targetId: lateSwapTargets.platforms[0].devices[0].id,
    discoveryRevision: lateSwapTargets.discoveryRevision,
    expectedProjectSourceRevision: lateSwapTargets.projectSourceRevision,
    buildMode: 'last-build',
    idempotencyKey: 'last-build-late-artifact-swap',
  });
  assert.equal(lateSwapStart.status, 202);
  const originalApk = fs.readFileSync(path.join(apkRoot, 'fixture-debug.apk'));
  fs.writeFileSync(path.join(apkRoot, 'fixture-debug.apk'), 'late-swapped-apk');
  const lateSwapFailure = await waitFor(() => {
    const current = runner.status().job;
    return current && current.jobId === lateSwapStart.job.jobId &&
      current.state === 'failed' ? current : null;
  }, 'late last-build artifact swap rejection');
  assert.equal(lateSwapFailure.errorCode, 'artifact-hash-mismatch');
  assert.equal(invocations.filter((row) => row.executable === tools.gradlew).length,
    buildCountBeforeLateSwap,
    'Install last build must fail closed instead of silently rebuilding after a late artifact swap');
  assert.equal(runner.status().integrity.ok, false,
    'a late artifact hash mismatch must latch runtime recovery');
  fs.writeFileSync(path.join(apkRoot, 'fixture-debug.apk'), originalApk);
  runner._runtime.integrityIssues = [];

  const ifNeededTargets = runner.targets('android', true).targets;
  const buildCountBeforeIfNeededTamper = invocations.filter(
    (row) => row.executable === tools.gradlew,
  ).length;
  fs.writeFileSync(path.join(apkRoot, 'fixture-debug.apk'), 'preflight-swapped-apk');
  const ifNeededTamper = runner.start({
    ...startBody,
    targetId: ifNeededTargets.platforms[0].devices[0].id,
    discoveryRevision: ifNeededTargets.discoveryRevision,
    expectedProjectSourceRevision: ifNeededTargets.projectSourceRevision,
    buildMode: 'if-needed',
    idempotencyKey: 'if-needed-artifact-tamper',
  });
  assert.equal(ifNeededTamper.status, 202);
  const ifNeededTamperFailure = await waitFor(() => {
    const current = runner.status().job;
    return current && current.jobId === ifNeededTamper.job.jobId &&
      current.state === 'failed' ? current : null;
  }, 'if-needed stored artifact tamper rejection');
  assert.equal(ifNeededTamperFailure.errorCode, 'artifact-hash-mismatch');
  assert.equal(invocations.filter((row) => row.executable === tools.gradlew).length,
    buildCountBeforeIfNeededTamper,
    'If needed must not disguise artifact corruption as an ordinary rebuild');
  assert.equal(runner.status().integrity.ok, false);
  fs.writeFileSync(path.join(apkRoot, 'fixture-debug.apk'), originalApk);
  runner._runtime.integrityIssues = [];

  const corruptManifestRecord = storage.readJson(
    paths.APP_RUN_ARTIFACTS_DIR, running.job.artifactId,
  );
  storage.writeJson(paths.APP_RUN_ARTIFACTS_DIR, corruptManifestRecord.artifactId, {
    ...corruptManifestRecord,
    unexpectedField: true,
  });
  const corruptManifestTargets = runner.targets('android', true).targets;
  const corruptManifestStart = runner.start({
    ...startBody,
    targetId: corruptManifestTargets.platforms[0].devices[0].id,
    discoveryRevision: corruptManifestTargets.discoveryRevision,
    expectedProjectSourceRevision: corruptManifestTargets.projectSourceRevision,
    buildMode: 'last-build',
    idempotencyKey: 'corrupt-artifact-manifest-preflight',
  });
  assert.equal(corruptManifestStart.status, 409);
  assert.equal(corruptManifestStart.error, 'artifact-invalid',
    'a corrupt stored manifest must remain a typed recovery failure, never internal');
  assert.equal(runner.status().integrity.ok, false);
  storage.writeJson(
    paths.APP_RUN_ARTIFACTS_DIR, corruptManifestRecord.artifactId, corruptManifestRecord,
  );
  runner._runtime.integrityIssues = [];

  const appSourceFile = path.join(root, 'androidApp', 'src', 'main', 'Fixture.kt');
  const originalAppSource = fs.readFileSync(appSourceFile, 'utf8');
  fs.writeFileSync(appSourceFile, originalAppSource + '// changed after build\n');
  const staleArtifactTargets = runner.targets('android', true).targets;
  const staleArtifactBody = {
    ...startBody,
    targetId: staleArtifactTargets.platforms[0].devices[0].id,
    discoveryRevision: staleArtifactTargets.discoveryRevision,
    expectedProjectSourceRevision: staleArtifactTargets.projectSourceRevision,
    buildMode: 'last-build',
    idempotencyKey: 'stale-artifact-preview',
  };
  const staleArtifactPreview = runner.start(staleArtifactBody);
  assert.equal(staleArtifactPreview.error, 'artifact-stale');
  assert.equal(staleArtifactPreview.confirmationRequired, true);
  const artifactRecord = storage.readJson(
    paths.APP_RUN_ARTIFACTS_DIR, running.job.artifactId,
  );
  const originalArtifactRecord = structuredClone(artifactRecord);
  artifactRecord.appProjectSourceRevision = 'sha256:' + 'e'.repeat(64);
  artifactRecord.buildInputFingerprint = artifactRecord.appProjectSourceRevision;
  storage.writeJson(paths.APP_RUN_ARTIFACTS_DIR, artifactRecord.artifactId, artifactRecord);
  const substitutedArtifactConfirmation = runner.start({
    ...staleArtifactBody,
    confirmationToken: staleArtifactPreview.confirmation.token,
    idempotencyKey: 'stale-artifact-confirmation',
  });
  assert.equal(substitutedArtifactConfirmation.error, 'confirmation-invalid',
    'an older-build token must be bound to the exact artifact source revision');
  storage.writeJson(
    paths.APP_RUN_ARTIFACTS_DIR, originalArtifactRecord.artifactId, originalArtifactRecord,
  );
  fs.writeFileSync(appSourceFile, originalAppSource);
  runner.targets('android', true);

  const taskWriter = finalizations.beginMutation({
    kind: 'task-session',
    stem: 'TASK_91_busy',
    sessionId: finalizations.createWriterSessionId(),
    key: 'task:TASK_91_busy',
    pendingChild: false,
  });
  assert.equal(taskWriter.ok, true);
  try {
    const refreshed = runner.targets('android', true).targets;
    const refreshedAndroid = refreshed.platforms[0];
    const busyBase = {
      ...startBody,
      targetId: refreshedAndroid.devices[0].id,
      discoveryRevision: refreshed.discoveryRevision,
      expectedProjectSourceRevision: refreshed.projectSourceRevision,
      buildMode: 'if-needed',
    };
    const failedBusy = runner.start({
      ...busyBase,
      whenBusy: 'fail',
      idempotencyKey: 'busy-fail-start',
    });
    assert.equal(failedBusy.error, 'project-busy');
    const queued = runner.start({
      ...busyBase,
      whenBusy: 'queue',
      idempotencyKey: 'busy-queued-start',
    });
    assert.equal(queued.status, 202);
    const waiting = await waitFor(() => {
      const status = runner.status();
      return status.job && status.job.jobId === queued.job.jobId &&
        status.job.state === 'waiting-for-project' ? status.job : null;
    }, 'queued project admission');
    const cancelled = await runner.cancel({
      jobId: waiting.jobId,
      expectedStateRevision: waiting.jobRevision,
      idempotencyKey: 'busy-cancel',
    });
    assert.equal(cancelled.job.state, 'cancelled');
    await waitFor(() => !runner._runtime.controllers.has(waiting.jobId), 'cancelled runner cleanup');
  } finally {
    finalizations.endMutation(taskWriter.handle);
  }

  const reusedTargets = runner.targets('android', true).targets;
  const reusedBuildCount = invocations.filter((row) => row.executable === tools.gradlew).length;
  const reusedStart = runner.start({
    ...startBody,
    targetId: reusedTargets.platforms[0].devices[0].id,
    discoveryRevision: reusedTargets.discoveryRevision,
    expectedProjectSourceRevision: reusedTargets.projectSourceRevision,
    buildMode: 'if-needed',
    idempotencyKey: 'verified-artifact-reuse',
  });
  assert.equal(reusedStart.status, 202);
  const reusedRunning = await waitFor(() => {
    const current = runner.status();
    return current.session && current.session.jobId === reusedStart.job.jobId &&
      current.session.state === 'running' ? current : null;
  }, 'verified artifact reuse');
  assert.equal(invocations.filter((row) => row.executable === tools.gradlew).length, reusedBuildCount,
    'if-needed must not rebuild a source/config/toolchain-compatible artifact');
  assert.equal(reusedRunning.job.stages.find((stage) => stage.id === 'building').status, 'skipped');
  assert.equal(reusedRunning.job.stages.find((stage) => stage.id === 'resolving-artifact').status, 'success');

  const restartTargets = runner.targets('android', true).targets;
  const staleRestart = await runner.restart({
    sessionId: reusedRunning.session.sessionId,
    expectedSessionRevision: reusedRunning.session.sessionRevision,
    buildMode: 'if-needed',
    discoveryRevision: restartTargets.discoveryRevision,
    expectedProjectSourceRevision: 'sha256:' + '0'.repeat(64),
    confirmationToken: null,
    idempotencyKey: 'restart-stale-source-preflight',
  });
  assert.equal(staleRestart.error, 'source-changed');
  assert.equal(runner.status().session.sessionId, reusedRunning.session.sessionId);
  assert.equal(runner.status().session.state, 'running',
    'restart preflight failure must not stop the current app');

  const restarted = await runner.restart({
    sessionId: reusedRunning.session.sessionId,
    expectedSessionRevision: reusedRunning.session.sessionRevision,
    buildMode: 'if-needed',
    discoveryRevision: restartTargets.discoveryRevision,
    expectedProjectSourceRevision: restartTargets.projectSourceRevision,
    confirmationToken: null,
    idempotencyKey: 'verified-artifact-restart',
  });
  assert.equal(restarted.status, 202);
  const restartedRunning = await waitFor(() => {
    const current = runner.status();
    return current.session && current.session.jobId === restarted.job.jobId &&
      current.session.state === 'running' ? current : null;
  }, 'preflighted app restart');
  assert.equal(restartedRunning.job.linkedJobId, reusedStart.job.jobId);
  const restartedStopped = await runner.stop({
    sessionId: restartedRunning.session.sessionId,
    expectedSessionRevision: restartedRunning.session.sessionRevision,
    idempotencyKey: 'verified-artifact-restart-stop',
  });
  assert.equal(restartedStopped.ok, true);

  const runningJobRecord = storage.readJson(paths.APP_RUN_JOBS_DIR, restarted.job.jobId);
  const runningSessionRecord = storage.readJson(
    paths.APP_RUN_SESSIONS_DIR, restartedRunning.session.sessionId,
  );
  runningJobRecord.state = 'running';
  runningJobRecord.phase = 'running';
  runningJobRecord.finishedAt = null;
  runningJobRecord.errorCode = null;
  runningJobRecord.result = {
    launched: true,
    rebuilt: false,
    pidObserved: true,
  };
  runningJobRecord.jobRevision++;
  runningJobRecord.updatedAt = new Date().toISOString();
  runningSessionRecord.state = 'running';
  runningSessionRecord.sessionRevision++;
  runningSessionRecord.updatedAt = new Date().toISOString();
  storage.writeJson(paths.APP_RUN_JOBS_DIR, runningJobRecord.jobId, runningJobRecord);
  storage.writeJson(paths.APP_RUN_SESSIONS_DIR, runningSessionRecord.sessionId, runningSessionRecord);
  runner._resetForTests();
  runner.init({ commandRunner: fakeRunner, androidTools: tools });
  assert.equal(runner._runtime.jobs.get(runningJobRecord.jobId).state, 'interrupted',
    'an unrediscovered running job must not survive restart as an immortal running record');
  assert.equal(runner._runtime.jobs.get(runningJobRecord.jobId).errorCode, 'process-interrupted');
  assert.equal(runner._runtime.sessions.get(runningSessionRecord.sessionId).state, 'lost');

  const index = JSON.parse(fs.readFileSync(paths.APP_RUN_INDEX_FILE, 'utf8'));
  assert.equal(index.schemaVersion, 1);
  assert.ok(index.jobIds.includes(started.job.jobId));
  assert.ok(invocations.some((row) => row.argv.includes('install')));
  assert.ok(invocations.some((row) => row.argv.includes('force-stop')));

  const orphanScreenshotId = storage.randomId('shot');
  const orphanScreenshotFile = storage.fileFor(
    paths.APP_RUN_SCREENSHOTS_DIR, orphanScreenshotId, '.png',
  );
  storage.writeFileAtomic(
    orphanScreenshotFile,
    runner.screenshotFile(firstShot.screenshot.screenshotId).bytes,
    25 * 1024 * 1024,
  );
  const persisted = storage.readJson(paths.APP_RUN_JOBS_DIR, started.job.jobId);
  persisted.state = 'building';
  persisted.phase = 'building';
  persisted.finishedAt = null;
  persisted.result = null;
  persisted.errorCode = null;
  persisted.artifactId = null;
  persisted.sessionId = null;
  persisted.appProjectSourceRevision = null;
  persisted.progress = 41;
  persisted.stages.forEach((stage) => {
    if (['detecting', 'waiting-for-project', 'starting-device'].includes(stage.id)) {
      stage.status = stage.id === 'waiting-for-project' ? 'skipped' : 'success';
    } else if (stage.id === 'building') {
      stage.status = 'running';
      stage.startedAt = new Date().toISOString();
      stage.durationMs = null;
    } else {
      stage.status = 'queued';
      stage.startedAt = null;
      stage.durationMs = null;
      stage.message = null;
    }
  });
  persisted.jobRevision++;
  persisted.updatedAt = new Date().toISOString();
  storage.writeJson(paths.APP_RUN_JOBS_DIR, persisted.jobId, persisted);
  runner._resetForTests();
  runner.init({ commandRunner: fakeRunner, androidTools: tools });
  assert.equal(fs.existsSync(orphanScreenshotFile), false,
    'recovery must remove a screenshot PNG that has no committed metadata');
  assert.ok(runner.screenshotFile(firstShot.screenshot.screenshotId),
    'screenshot evidence referenced by a retained validation receipt must survive retention');
  const corruptScreenshotId = storage.randomId('shot');
  const originalScreenshot = runner.screenshotFile(firstShot.screenshot.screenshotId);
  storage.writeFileAtomic(
    storage.fileFor(paths.APP_RUN_SCREENSHOTS_DIR, corruptScreenshotId, '.png'),
    Buffer.concat([originalScreenshot.bytes, Buffer.from('tampered')]),
    25 * 1024 * 1024,
  );
  storage.writeJson(paths.APP_RUN_SCREENSHOTS_DIR, corruptScreenshotId, {
    ...originalScreenshot.metadata,
    screenshotId: corruptScreenshotId,
  }, 128 * 1024);
  assert.equal(runner.screenshotFile(corruptScreenshotId), null);
  assert.equal(fs.existsSync(storage.fileFor(
    paths.APP_RUN_SCREENSHOTS_DIR, corruptScreenshotId, '.json',
  )), true, 'published invalid screenshot metadata must be preserved as recovery evidence');
  assert.equal(fs.existsSync(storage.fileFor(
    paths.APP_RUN_SCREENSHOTS_DIR, corruptScreenshotId, '.png',
  )), true, 'published invalid screenshot bytes must be preserved as recovery evidence');
  assert.ok(runner.status().integrity.issues.some((issue) =>
    issue.code === 'screenshot-recovery-invalid' && issue.recordId === corruptScreenshotId));
  storage.remove(paths.APP_RUN_SCREENSHOTS_DIR, corruptScreenshotId, '.json', 128 * 1024);
  storage.remove(paths.APP_RUN_SCREENSHOTS_DIR, corruptScreenshotId, '.png', 25 * 1024 * 1024);
  runner._runtime.integrityIssues = [];
  const recovered = runner.status().job;
  assert.equal(recovered.jobId, persisted.jobId);
  assert.equal(recovered.state, 'interrupted');
  assert.equal(recovered.errorCode, 'process-interrupted');

  const corrupted = storage.readJson(paths.APP_RUN_JOBS_DIR, persisted.jobId);
  corrupted.unexpectedField = true;
  storage.writeJson(paths.APP_RUN_JOBS_DIR, corrupted.jobId, corrupted);
  runner._resetForTests();
  runner.init({ commandRunner: fakeRunner, androidTools: tools });
  const rejectedRecovery = runner.status();
  assert.equal(rejectedRecovery.integrity.ok, false);
  assert.ok(rejectedRecovery.integrity.issues.some((issue) =>
    issue.code === 'job-recovery-invalid' && issue.recordId === corrupted.jobId));
  assert.equal(rejectedRecovery.actions.canRestart, false);
  assert.equal(rejectedRecovery.actions.canScreenshot, false);
  assert.equal((await runner.restart({
    sessionId: 'session-' + 'a'.repeat(36),
    expectedSessionRevision: 1,
    buildMode: 'rebuild',
    discoveryRevision: 'discovery-' + 'b'.repeat(36),
    expectedProjectSourceRevision: 'sha256:' + 'c'.repeat(64),
    confirmationToken: null,
    idempotencyKey: 'recovery-restart-denied',
  })).error, 'runtime-recovery-required');
  assert.equal((await runner.screenshot({
    sessionId: 'session-' + 'a'.repeat(36),
    expectedSessionRevision: 1,
    taskStem: null,
    surfaceId: null,
    idempotencyKey: 'recovery-screenshot-denied',
  })).error, 'runtime-recovery-required');
  assert.equal((await runner.validationSave({
    idempotencyKey: 'recovery-validation-denied',
  })).error, 'runtime-recovery-required');

  fs.mkdirSync(path.dirname(paths.STATE_FILE), { recursive: true });
  fs.writeFileSync(paths.STATE_FILE, '{}\n');
  const corruptPreferenceState = runner.status();
  assert.deepEqual(corruptPreferenceState, {
    ok: false,
    status: 409,
    error: 'runtime-recovery-required',
  }, 'corrupt shared persistence must not be presented as empty app-run preferences');
} finally {
  releaseBuild();
  runner.killAll();
  runner._resetForTests();
  fs.rmSync(root, { recursive: true, force: true });
}
