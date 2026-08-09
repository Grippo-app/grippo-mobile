import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';

// Task-pipeline lifecycle guards for the app runner: task drift while queued
// behind the writer lease, project drift during and after a run, validation
// idempotency under concurrency, foreign-session evidence rejection, and the
// exact stop/restart/recovery semantics of manual validation state.

const require = createRequire(import.meta.url);
const Ajv2020 = require('ajv/dist/2020.js').default;
const testDirectory = path.dirname(new URL(import.meta.url).pathname);
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'app-run-lifecycle-'));
const orchestrator = path.join(root, 'orchestrator');
const cache = path.join(orchestrator, '.cache');
const tasks = path.join(orchestrator, 'tasks');
const apkRoot = path.join(root, 'androidApp', 'build', 'outputs', 'apk', 'debug');
const contextStem = 'TASK_7_ctx';
const otherStem = 'TASK_8_other';

fs.mkdirSync(apkRoot, { recursive: true });
fs.mkdirSync(path.join(root, 'androidApp', 'src', 'main'), { recursive: true });
for (const directory of [
  path.join(tasks, 'backlog'),
  path.join(tasks, 'pending'),
  path.join(tasks, 'todo'),
  path.join(tasks, 'done'),
]) fs.mkdirSync(directory, { recursive: true });
fs.writeFileSync(path.join(orchestrator, 'project-config.md'), `---
productName: Lifecycle fixture
applicationId: com.example.lifecycle
iosEnabled: false
androidAssembleTask: :androidApp:assembleDebug
---
`);
fs.writeFileSync(path.join(root, 'androidApp', 'src', 'main', 'Fixture.kt'),
  'package com.example.lifecycle\n');
fs.writeFileSync(path.join(apkRoot, 'fixture-debug.apk'), Buffer.from('lifecycle-apk-fixture'));
fs.writeFileSync(path.join(apkRoot, 'output-metadata.json'), JSON.stringify({
  version: 3,
  elements: [{ type: 'SINGLE', filters: [], outputFile: 'fixture-debug.apk' }],
}) + '\n');

const taskText = (stem, extra = '') => [
  '# ' + stem + ' — Lifecycle fixture',
  '',
  '## Acceptance ##',
  '',
  '### Manual ###',
  '',
  '- Verify the launched screen for ' + stem + extra,
  '',
].join('\n');
fs.writeFileSync(path.join(tasks, 'todo', contextStem + '.md'), taskText(contextStem));
fs.writeFileSync(path.join(tasks, 'todo', otherStem + '.md'), taskText(otherStem));
const fingerprint = 'sha256:' + '3'.repeat(64);
const indexRow = (stem) => ({
  stem,
  title: stem,
  state: 'todo',
  createdAt: '2026-07-26T11:00:00.000Z',
  doneAt: null,
  sourceRevision: fingerprint,
  origin: { kind: 'manual', type: 'manual', ref: 'fixture:' + stem, fingerprint },
  dependsOn: [],
  splitFrom: null,
  outcomeStatus: null,
  questionsCount: null,
  round: null,
});
fs.writeFileSync(path.join(tasks, 'INDEX.json'), JSON.stringify({
  version: 2,
  generatedAt: '2026-07-26T12:00:00.000Z',
  backlog: [],
  pending: [],
  todo: [indexRow(contextStem), indexRow(otherStem)],
  done: [],
}, null, 2) + '\n');

process.env.ORCHESTRATOR_PROJECT_ROOT = root;
process.env.ORCHESTRATOR_TASKS_DIR = tasks;
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
let buildHold = null;
function holdBuilds() {
  let release;
  const promise = new Promise((resolve) => { release = resolve; });
  buildHold = promise;
  return () => { buildHold = null; release(); };
}
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
    if (spec.executable === tools.apkanalyzer) return result('com.example.lifecycle\n');
    return result('');
  },
  async run(spec) {
    invocations.push({ mode: 'async', executable: spec.executable, argv: spec.argv.slice() });
    const args = spec.argv.join(' ');
    if (spec.executable === tools.gradlew && buildHold) await buildHold;
    if (args.includes('resolve-activity')) return result('com.example.lifecycle/.MainActivity\n');
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

const appRunPaths = require('../server/paths.js');
const runner = require('../server/app-runner.js');
const storage = require('../server/app-run-storage.js');
const validation = require('../server/app-run-validation.js');
const finalizations = require('../server/finalizations.js');

async function waitFor(predicate, label) {
  for (let attempt = 0; attempt < 400; attempt++) {
    const value = predicate();
    if (value) return value;
    await new Promise((resolve) => setTimeout(resolve, 10));
  }
  const status = runner.status();
  throw new Error('timed out waiting for ' + label + '\n' + JSON.stringify(status, null, 2));
}

function startBody(taskStem, targets, patch = {}) {
  const android = targets.platforms[0];
  return {
    platform: 'android',
    targetId: android.devices[0].id,
    discoveryRevision: targets.discoveryRevision,
    variantId: 'debug',
    buildMode: 'rebuild',
    taskStem,
    surfaceId: null,
    expectedProjectSourceRevision: targets.projectSourceRevision,
    confirmationToken: null,
    whenBusy: 'queue',
    idempotencyKey: 'lifecycle-' + Math.random().toString(16).slice(2),
    ...patch,
  };
}

try {
  runner.init({ commandRunner: fakeRunner, androidTools: tools });

  // --- Task context drift while the run waits for the writer lease. ---
  const taskWriter = finalizations.beginMutation({
    kind: 'task-session',
    stem: 'TASK_90_busy',
    sessionId: finalizations.createWriterSessionId(),
    key: 'task:TASK_90_busy',
    pendingChild: false,
  });
  assert.equal(taskWriter.ok, true);
  let targets = runner.targets('android', true).targets;
  const queuedStart = runner.start(startBody(contextStem, targets, {
    idempotencyKey: 'lifecycle-queued-task-drift',
  }));
  assert.equal(queuedStart.status, 202);
  assert.equal(queuedStart.job.taskStem, contextStem,
    'the queued job must carry the exact board task context');
  await waitFor(() => {
    const current = runner.status().job;
    return current && current.jobId === queuedStart.job.jobId &&
      current.state === 'waiting-for-project' ? current : null;
  }, 'queued admission behind the task writer');
  fs.appendFileSync(path.join(tasks, 'todo', contextStem + '.md'),
    '- A check added while the run was queued\n');
  finalizations.endMutation(taskWriter.handle);
  const taskDrift = await waitFor(() => {
    const current = runner.status().job;
    return current && current.jobId === queuedStart.job.jobId &&
      current.state === 'failed' ? current : null;
  }, 'task drift rejection after admission');
  assert.equal(taskDrift.errorCode, 'task-context-changed',
    'a task edited while the run was queued must fail the exact typed way');
  assert.equal(runner.status().session, null,
    'a task-drift failure must not leave a running session behind');

  // --- Project source drift while the build is running. ---
  const releaseBuild = holdBuilds();
  targets = runner.targets('android', true).targets;
  const buildingStart = runner.start(startBody(contextStem, targets, {
    idempotencyKey: 'lifecycle-source-drift-build',
  }));
  assert.equal(buildingStart.status, 202);
  await waitFor(() => invocations.some((row) =>
    row.executable === tools.gradlew), 'gradle admission');
  fs.appendFileSync(path.join(root, 'androidApp', 'src', 'main', 'Fixture.kt'),
    '// edited during the build\n');
  releaseBuild();
  const buildDrift = await waitFor(() => {
    const current = runner.status().job;
    return current && current.jobId === buildingStart.job.jobId &&
      current.state === 'failed' ? current : null;
  }, 'in-build source drift rejection');
  assert.equal(buildDrift.errorCode, 'source-changed',
    'sources edited during the build must fail the run, not ship a stale binary');

  // --- A clean run bound to the task context. ---
  targets = runner.targets('android', true).targets;
  const cleanStart = runner.start(startBody(contextStem, targets, {
    idempotencyKey: 'lifecycle-clean-run',
  }));
  assert.equal(cleanStart.status, 202);
  const runningA = await waitFor(() => {
    const current = runner.status();
    return current.session && current.session.jobId === cleanStart.job.jobId &&
      current.session.state === 'running' ? current : null;
  }, 'clean task-bound run');
  assert.equal(runningA.session.taskStem, contextStem);
  assert.equal(runningA.session.sourceState, 'current');
  const startKeyConflict = runner.start(startBody(contextStem, targets, {
    idempotencyKey: 'lifecycle-clean-run',
    buildMode: 'if-needed',
  }));
  assert.equal(startKeyConflict.status, 409);
  assert.equal(startKeyConflict.error, 'idempotency-conflict',
    'one start idempotency key must never accept a different payload');

  // --- Project source drift after launch is surfaced on the session. ---
  fs.appendFileSync(path.join(root, 'androidApp', 'src', 'main', 'Fixture.kt'),
    '// edited after launch\n');
  await new Promise((resolve) => setTimeout(resolve, 1100));
  assert.equal(runner.status().session.sourceState, 'changed',
    'a session must report project drift after launch instead of staying silently current');

  // --- Evidence and validation bound to the session. ---
  const sessionA = runner.status().session;
  const shot = await runner.screenshot({
    sessionId: sessionA.sessionId,
    expectedSessionRevision: sessionA.sessionRevision,
    taskStem: contextStem,
    surfaceId: null,
    idempotencyKey: 'lifecycle-screenshot-a',
  });
  assert.equal(shot.status, 201);
  assert.equal(shot.screenshot.taskStem, contextStem,
    'screenshot evidence must inherit the exact session task context');

  const eligible = runner.validationGet({ taskStem: contextStem, sessionId: sessionA.sessionId });
  assert.equal(eligible.ok, true);
  assert.equal(eligible.eligibleSession.sessionId, sessionA.sessionId);
  assert.equal(eligible.latestReceipt, null);
  const saveBody = {
    taskStem: contextStem,
    expectedTaskSourceRevision: eligible.taskSourceRevision,
    sessionId: sessionA.sessionId,
    expectedSessionRevision: eligible.eligibleSession.sessionRevision,
    validationRevision: eligible.validationRevision,
    items: eligible.items.map((item, index) => ({
      itemId: item.itemId,
      result: 'pass',
      note: null,
      screenshotIds: index === 0 ? [shot.screenshot.screenshotId] : [],
    })),
    acknowledgeStaleTask: false,
    idempotencyKey: 'lifecycle-validation-save',
  };

  // Concurrent identical saves must resolve to exactly one receipt.
  const [firstSave, replaySave] = await Promise.all([
    runner.validationSave(saveBody),
    runner.validationSave(saveBody),
  ]);
  assert.equal(firstSave.status, 201);
  assert.equal(replaySave.status, 201);
  assert.equal(firstSave.receipt.receiptId, replaySave.receipt.receiptId,
    'idempotent replays must return the already-persisted receipt');
  assert.equal(validation.history(contextStem, 10).length, 1,
    'concurrent identical saves must persist exactly one receipt');
  assert.equal(firstSave.journalRecorded, true);
  assert.equal(firstSave.receipt.sessionId, sessionA.sessionId);
  assert.equal(firstSave.receipt.runJobId, sessionA.jobId);
  assert.equal(firstSave.receipt.artifactId, sessionA.artifactId);
  assert.equal(firstSave.receipt.appProjectSourceRevision, sessionA.appProjectSourceRevision);
  assert.equal(firstSave.receipt.staleSource, true,
    'a receipt saved after project drift must record that staleness honestly');

  const receiptContract = new Ajv2020({
    strict: true,
    strictRequired: false,
    formats: { 'date-time': true },
  }).compile(JSON.parse(fs.readFileSync(
    path.join(testDirectory, '..', 'contracts', 'app-run', 'validation-receipt.schema.json'), 'utf8',
  )));
  assert.equal(receiptContract(firstSave.receipt), true,
    JSON.stringify(receiptContract.errors));

  const conflicting = await runner.validationSave({
    ...saveBody,
    items: saveBody.items.map((item) => ({ ...item, result: 'fail' })),
  });
  assert.equal(conflicting.status, 409);
  assert.equal(conflicting.error, 'idempotency-conflict',
    'one idempotency key must never publish two different validation payloads');

  // --- Stop must not leave any validation-eligible session behind. ---
  const stopped = await runner.stop({
    sessionId: sessionA.sessionId,
    expectedSessionRevision: runner.status().session.sessionRevision,
    idempotencyKey: 'lifecycle-stop-a',
  });
  assert.equal(stopped.ok, true);
  assert.equal(runner.status().actions.canStop, false);
  assert.equal(runner.status().actions.canScreenshot, false);
  const afterStop = runner.validationGet({ taskStem: contextStem, sessionId: sessionA.sessionId });
  assert.equal(afterStop.eligibleSession, null,
    'a stopped session must never be offered for validation');
  assert.equal(afterStop.latestReceipt.receiptId, firstSave.receipt.receiptId,
    'stopping the app must not erase already-saved validation evidence');
  const savedAfterStop = await runner.validationSave({
    ...saveBody,
    expectedSessionRevision: stopped.session.sessionRevision,
    idempotencyKey: 'lifecycle-validation-after-stop',
  });
  assert.equal(savedAfterStop.status, 409);
  assert.equal(savedAfterStop.error, 'session-not-running',
    'validation must be refused once the session is stopped');

  // --- A different session/task must not be able to use foreign evidence. ---
  targets = runner.targets('android', true).targets;
  const otherStart = runner.start(startBody(otherStem, targets, {
    buildMode: 'if-needed',
    idempotencyKey: 'lifecycle-other-task-run',
  }));
  assert.equal(otherStart.status, 202);
  const runningB = await waitFor(() => {
    const current = runner.status();
    return current.session && current.session.jobId === otherStart.job.jobId &&
      current.session.state === 'running' ? current : null;
  }, 'second task-bound run');
  assert.equal(runningB.session.taskStem, otherStem);
  const otherEligible = runner.validationGet({
    taskStem: otherStem, sessionId: runningB.session.sessionId,
  });
  assert.equal(otherEligible.ok, true);
  const foreignEvidence = await runner.validationSave({
    taskStem: otherStem,
    expectedTaskSourceRevision: otherEligible.taskSourceRevision,
    sessionId: runningB.session.sessionId,
    expectedSessionRevision: otherEligible.eligibleSession.sessionRevision,
    validationRevision: otherEligible.validationRevision,
    items: otherEligible.items.map((item, index) => ({
      itemId: item.itemId,
      result: 'pass',
      note: null,
      screenshotIds: index === 0 ? [shot.screenshot.screenshotId] : [],
    })),
    acknowledgeStaleTask: false,
    idempotencyKey: 'lifecycle-foreign-evidence',
  });
  assert.equal(foreignEvidence.status, 400);
  assert.equal(foreignEvidence.error, 'screenshot-not-owned',
    'another session/task must never consume foreign screenshot evidence');

  // --- Server restart: sessions are lost, receipts survive with honest
  // staleness, and no false validated/running state remains. ---
  runner._resetForTests();
  runner.init({ commandRunner: fakeRunner, androidTools: tools });
  assert.equal(runner._runtime.sessions.get(runningB.session.sessionId).state, 'lost',
    'a running session must be marked lost across a server restart');
  assert.equal(runner.status().actions.canStop, false);
  const afterRestart = runner.validationGet({ taskStem: contextStem, sessionId: null });
  assert.equal(afterRestart.ok, true);
  assert.equal(afterRestart.eligibleSession, null);
  assert.equal(afterRestart.latestReceipt.receiptId, firstSave.receipt.receiptId,
    'validation receipts must survive a server restart');
  assert.equal(afterRestart.latestReceipt.staleTask, false);
  const lostSave = await runner.validationSave({
    ...saveBody,
    expectedSessionRevision:
      runner._runtime.sessions.get(sessionA.sessionId).sessionRevision,
    idempotencyKey: 'lifecycle-validation-after-restart',
  });
  assert.equal(lostSave.status, 409);
  assert.equal(lostSave.error, 'session-not-running',
    'an unconfirmed pre-restart session must never validate after recovery');

  fs.appendFileSync(path.join(tasks, 'todo', contextStem + '.md'),
    '- A check added after the receipt was saved\n');
  const staleAfterRestart = runner.validationGet({ taskStem: contextStem, sessionId: null });
  assert.equal(staleAfterRestart.latestReceipt.receiptId, firstSave.receipt.receiptId);
  assert.equal(staleAfterRestart.latestReceipt.staleTask, true,
    'a surviving receipt must project as stale once the task changes');

  // --- Corrupt receipts block validation reads fail-closed. ---
  const corruptId = storage.randomId('receipt');
  storage.writeJson(appRunPaths.APP_RUN_HISTORY_DIR, corruptId, {
    schemaVersion: 1, receiptId: corruptId, truncated: true,
  }, 128 * 1024);
  const corruptGet = runner.validationGet({ taskStem: contextStem, sessionId: null });
  assert.equal(corruptGet.status, 409);
  assert.equal(corruptGet.error, 'runtime-recovery-required',
    'corrupt receipt storage must fail closed, never read as an empty history');
  assert.equal((await runner.validationSave({
    ...saveBody,
    idempotencyKey: 'lifecycle-validation-corrupt-blocked',
  })).error, 'runtime-recovery-required');
  storage.remove(appRunPaths.APP_RUN_HISTORY_DIR, corruptId, '.json', 128 * 1024);
} finally {
  buildHold = null;
  runner.killAll();
  runner._resetForTests();
  fs.rmSync(root, { recursive: true, force: true });
}
