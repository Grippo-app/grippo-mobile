import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);
const Ajv2020 = require('ajv/dist/2020.js').default;
const testDirectory = path.dirname(fileURLToPath(import.meta.url));
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'app-run-contracts-'));
const orchestrator = path.join(root, 'orchestrator');
fs.mkdirSync(orchestrator, { recursive: true });
fs.writeFileSync(path.join(orchestrator, 'project-config.md'), `---
productName: Sample
applicationId: com.example.sample
iosEnabled: false
androidAssembleTask: :androidApp:assembleDebug
---
`);
process.env.ORCHESTRATOR_PROJECT_ROOT = root;
process.env.ORCHESTRATOR_CACHE_DIR = path.join(orchestrator, '.cache');
process.env.ORCHESTRATOR_APP_RUN_DIR = path.join(orchestrator, '.cache', 'runtime', 'app-run');
process.env.ORCHESTRATOR_APP_RUN_AUTHORITY_ROOT = root;

const runner = require('../server/app-runner.js');
const discovery = require('../server/device-discovery.js');
const appRunConfig = require('../server/app-run-config.js');

try {
  runner.init();
  const ajv = new Ajv2020({
    strict: true,
    strictRequired: false,
    formats: { 'date-time': true },
  });
  const configContract = ajv.compile(JSON.parse(fs.readFileSync(
    path.join(testDirectory, '..', 'contracts', 'app-run', 'config.schema.json'), 'utf8',
  )));
  const jobContract = ajv.compile(JSON.parse(fs.readFileSync(
    path.join(testDirectory, '..', 'contracts', 'app-run', 'job.schema.json'), 'utf8',
  )));
  assert.doesNotThrow(() => ajv.compile(JSON.parse(fs.readFileSync(
    path.join(testDirectory, '..', 'contracts', 'app-run', 'validation-receipt.schema.json'), 'utf8',
  ))));
  assert.equal(configContract(appRunConfig.load().manifest), true, JSON.stringify(configContract.errors));
  assert.equal(discovery.stableHint('android:Pixel_8'),
    discovery.stableHint('android:Pixel_8'));
  assert.equal(discovery.stableHint('android:Pixel_8').includes('Pixel_8'), false);
  const bad = runner.start({
    platform: 'android',
    targetId: 'target-' + 'a'.repeat(32),
    discoveryRevision: 'discovery-' + 'b'.repeat(36),
    variantId: 'debug',
    buildMode: 'rebuild',
    taskStem: null,
    surfaceId: null,
    expectedProjectSourceRevision: 'sha256:' + 'c'.repeat(64),
    confirmationToken: null,
    whenBusy: 'fail',
    idempotencyKey: 'request-123',
    command: 'rm -rf /',
  });
  assert.equal(bad.status, 400);
  assert.equal(bad.error, 'bad-app-run-request');

  const historyStamp = Date.parse('2026-08-05T00:00:00.000Z');
  const historyJobs = Array.from({ length: 25 }, (_, index) => ({
    jobId: 'job-' + (index + 1).toString(16).padStart(36, '0'),
    state: 'failed',
    startedAt: new Date(historyStamp - index * 1000).toISOString(),
    updatedAt: new Date(historyStamp + index * 1000).toISOString(),
    stages: [],
    errorCode: null,
  }));
  historyJobs.forEach((job) => runner._runtime.jobs.set(job.jobId, job));
  const firstHistoryPage = runner.history(20, null);
  assert.equal(firstHistoryPage.ok, true);
  assert.deepEqual(firstHistoryPage.jobs.map((job) => job.jobId),
    historyJobs.slice(0, 20).map((job) => job.jobId));
  const newerHistoryJob = {
    jobId: 'job-' + 'f'.repeat(36),
    state: 'failed',
    startedAt: new Date(historyStamp + 1000).toISOString(),
    updatedAt: new Date(historyStamp + 1000).toISOString(),
    stages: [],
    errorCode: null,
  };
  runner._runtime.jobs.set(newerHistoryJob.jobId, newerHistoryJob);
  const secondHistoryPage = runner.history(20, firstHistoryPage.nextCursor);
  assert.equal(secondHistoryPage.ok, true);
  assert.deepEqual(secondHistoryPage.jobs.map((job) => job.jobId),
    historyJobs.slice(20).map((job) => job.jobId));
  assert.equal(secondHistoryPage.nextCursor, null);
  assert.deepEqual(runner.history(20, 'cursor-20'), {
    ok: false, status: 400, error: 'bad-history-cursor',
  });
  runner._runtime.jobs.clear();

  const png = Buffer.alloc(24);
  Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]).copy(png, 0);
  png.write('IHDR', 12, 'ascii');
  png.writeUInt32BE(1080, 16);
  png.writeUInt32BE(1920, 20);
  assert.deepEqual(runner.pngInfo(png), { width: 1080, height: 1920 });
  png.writeUInt32BE(20000, 16);
  assert.equal(runner.pngInfo(png), null);

  const createPublic = runner.publicJob({
    schemaVersion: 1,
    jobId: 'job-' + 'd'.repeat(36),
    jobRevision: 1,
    action: 'create-device',
    linkedJobId: null,
    platform: 'android',
    profileId: 'profile-' + 'e'.repeat(32),
    runtimeId: 'runtime-' + 'f'.repeat(32),
    creationPreviewHash: 'sha256:' + '1'.repeat(64),
    phase: 'queued',
    state: 'queued',
    progress: 0,
    stages: [
      {
        id: 'detecting', label: 'Detecting environment', status: 'queued',
        startedAt: null, durationMs: null, message: null,
      },
      {
        id: 'creating-device', label: 'Creating device', status: 'queued',
        startedAt: null, durationMs: null, message: null,
      },
    ],
    startedAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    finishedAt: null,
    result: { displayName: 'Fixture device', stableMaterial: 'ios:SECRET-UDID' },
    errorCode: null,
  });
  assert.equal('targetId' in createPublic, false);
  assert.equal('artifactId' in createPublic, false);
  assert.equal(createPublic.profileId, 'profile-' + 'e'.repeat(32));
  assert.equal(createPublic.result.managedDeviceCreated, true);
  assert.equal(createPublic.result.displayName, 'Fixture device');
  assert.match(createPublic.result.targetStableHint, /^hint-[a-f0-9]{32}$/);
  assert.equal(JSON.stringify(createPublic).includes('SECRET-UDID'), false);
  assert.equal(jobContract(createPublic), true, JSON.stringify(jobContract.errors));
  assert.equal(jobContract({ ...createPublic, state: 'running', phase: 'running' }), false);
  assert.equal(jobContract({ ...createPublic, linkedJobId: 'job-' + 'a'.repeat(36) }), false);
  assert.equal(jobContract({
    ...createPublic, state: 'completed', phase: 'completed',
    finishedAt: null,
  }), false);
  assert.equal(jobContract({ ...createPublic, command: 'unexpected' }), false);

  assert.equal(runner.transitionAllowed('run', 'queued', 'detecting'), true);
  assert.equal(runner.transitionAllowed('run', 'building', 'running'), false);
  assert.equal(runner.transitionAllowed('create-device', 'detecting', 'creating-device'), true);
  assert.equal(runner.transitionAllowed('create-device', 'completed', 'queued'), false);

  const internalJob = {
    schemaVersion: 1,
    jobId: 'job-' + '1'.repeat(36),
    jobRevision: 1,
    action: 'run',
    linkedJobId: null,
    platform: 'android',
    targetId: 'target-' + '2'.repeat(32),
    variantId: 'debug',
    buildMode: 'rebuild',
    taskStem: null,
    surfaceId: null,
    requestedProjectSourceRevision: 'sha256:' + '3'.repeat(64),
    runConfigHash: 'sha256:' + '4'.repeat(64),
    phase: 'queued',
    state: 'queued',
    progress: 0,
    finishedAt: null,
    stages: [
      ['detecting', 'Detecting environment'],
      ['waiting-for-project', 'Waiting for project'],
      ['starting-device', 'Starting device'],
      ['building', 'Building'],
      ['resolving-artifact', 'Resolving artifact'],
      ['installing', 'Installing'],
      ['launching', 'Launching'],
    ].map(([id, label]) => ({
      id, label, status: 'queued', startedAt: null, durationMs: null, message: null,
    })),
    processIdentities: [],
  };
  assert.doesNotThrow(() => runner.assertJobInvariant(internalJob));
  assert.throws(() => runner.assertJobInvariant({ ...internalJob, state: 'invented' }), /invariant/);
  assert.throws(() => runner.assertJobInvariant({ ...internalJob, phase: 'invented' }), /invariant/);
  assert.throws(() => runner.assertJobInvariant({
    ...internalJob,
    action: 'install',
    buildMode: 'rebuild',
  }), /invariant/);
  assert.throws(() => runner.assertJobInvariant({
    ...internalJob,
    action: 'restart',
    linkedJobId: null,
  }), /invariant/);
  assert.throws(() => runner.assertJobInvariant({
    ...internalJob,
    stages: internalJob.stages.map((stage, index) => (
      index === 0 ? { ...stage, label: 'Tampered label' } : stage
    )),
  }), /stages/);

  const session = {
    schemaVersion: 1,
    sessionId: 'session-' + '5'.repeat(36),
    sessionRevision: 1,
    jobId: internalJob.jobId,
    state: 'running',
    platform: 'android',
    targetId: internalJob.targetId,
    targetStableHint: 'hint-' + '6'.repeat(32),
    rawTargetIdentifier: 'emulator-5554',
    deviceSummary: 'Pixel 8 · Android 15',
    variantId: 'debug',
    artifactId: 'artifact-' + '7'.repeat(36),
    applicationId: 'com.example.sample',
    requestedProjectSourceRevision: internalJob.requestedProjectSourceRevision,
    appProjectSourceRevision: internalJob.requestedProjectSourceRevision,
    runConfigHash: internalJob.runConfigHash,
    launchedAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    taskStem: null,
    surfaceId: null,
  };
  assert.doesNotThrow(() => runner.assertSessionInvariant(session));
  assert.throws(() => runner.assertSessionInvariant({
    ...session, rawTargetIdentifier: '--transport-id=attacker',
  }), /session invariant/);
} finally {
  runner._resetForTests();
  fs.rmSync(root, { recursive: true, force: true });
}
