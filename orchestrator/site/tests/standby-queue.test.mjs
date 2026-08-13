import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import childProcess from 'node:child_process';
import crypto from 'node:crypto';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';

const REPO = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..', '..', '..');
const CLI = path.join(REPO, 'orchestrator/site/scripts/standby-queue.mjs');
const BOUNDARY = path.join(REPO, 'orchestrator/site/scripts/standby-queue-boundary.py');
const RESERVATION = path.join(REPO, 'orchestrator/site/scripts/request-reservation.mjs');
const TASK_LOCK = path.join(REPO, 'orchestrator/tasks/task-lock.mjs');
const OUTCOME_SHAPE = path.join(REPO, 'orchestrator/contracts/outcome-shape.json');
const require = createRequire(import.meta.url);
const taskCore = require('../../tasks/task-state-core.cjs');
const taskSource = require('../../tasks/task-source-contract.cjs');
const writerLeases = require('../../tasks/writer-leases.cjs');
const requestContract = require('../server/requests.js');
const fixtureRoots = [];
const SOURCE_BLOCK = taskSource.render(taskSource.manualForIntent('standby-queue-fixture', 'manual', 'fixture:standby-queue'));

test.afterEach(() => {
  while (fixtureRoots.length) fs.rmSync(fixtureRoots.pop(), { recursive: true, force: true });
});

function fixture({ createLocks = true } = {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'standby-queue-'));
  fixtureRoots.push(root);
  fs.mkdirSync(path.join(root, 'orchestrator/site'), { recursive: true, mode: 0o700 });
  fs.writeFileSync(path.join(root, 'orchestrator/site/server.js'), '// fixture\n', { mode: 0o600 });
  const fx = {
    root,
    cache: path.join(root, 'orchestrator/.cache'),
    tasks: path.join(root, 'orchestrator/tasks'),
    locks: path.join(root, 'orchestrator/.cache/tasks/locks'),
    finalizations: path.join(root, 'orchestrator/.cache/tasks/finalizations'),
    writers: path.join(root, 'orchestrator/.cache/tasks/finalizations/.writers'),
    requests: path.join(root, 'orchestrator/.cache/tasks/requests'),
    reservations: path.join(root, 'orchestrator/.cache/tasks/request-reservations'),
    runs: path.join(root, 'orchestrator/.cache/tasks/runs'),
    worker: path.join(root, 'orchestrator/.cache/tasks/worker'),
    superseded: path.join(root, 'orchestrator/.cache/tasks/superseded'),
  };
  for (const column of taskCore.COLUMNS) fs.mkdirSync(path.join(fx.tasks, column), { recursive: true, mode: 0o700 });
  if (createLocks) fs.mkdirSync(fx.locks, { recursive: true, mode: 0o700 });
  fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_1_demo.md'), '# TASK 1 — Demo\n\n' + SOURCE_BLOCK + '\n\nFixture request.\n', { mode: 0o600 });
  publishFreshIndex(fx);
  fx.sourceRevision = validateFixtureTask(fx).sourceRevision;
  return fx;
}

function validationOptions(fx, extra = {}) {
  return {
    repoRoot: fx.root,
    tasksDir: fx.tasks,
    outcomeShapePath: OUTCOME_SHAPE,
    includeRuntime: false,
    ...extra,
  };
}

function publishFreshIndex(fx) {
  const scanned = taskCore.validateTaskState(validationOptions(fx));
  assert.equal(scanned.ok, true, JSON.stringify(scanned.findings));
  const index = taskCore.deriveIndex(scanned._model, '2026-07-13T00:00:00Z');
  fs.writeFileSync(path.join(fx.tasks, 'INDEX.json'), JSON.stringify(index, null, 2) + '\n', { mode: 0o600 });
  return index;
}

function validateFixtureTask(fx) {
  const result = taskCore.validateTaskState(validationOptions(fx, { stem: 'TASK_1_demo', checkIndex: true }));
  assert.equal(result.ok, true, JSON.stringify(result.findings));
  assert.equal(result.indexStatus, 'fresh');
  return result;
}

function clearFixtureTask(fx) {
  for (const column of taskCore.COLUMNS) {
    const suffix = column === 'pending' ? '.questions.md' : '.md';
    try { fs.unlinkSync(path.join(fx.tasks, column, 'TASK_1_demo' + suffix)); }
    catch (error) { if (!error || error.code !== 'ENOENT') throw error; }
  }
}

function setFixtureState(fx, state) {
  clearFixtureTask(fx);
  if (state === 'backlog' || state === 'pending') {
    fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_1_demo.md'), '# TASK 1 — Demo\n\n' + SOURCE_BLOCK + '\n\nFixture request.\n', { mode: 0o600 });
  }
  if (state === 'pending') {
    fs.writeFileSync(path.join(fx.tasks, 'pending', 'TASK_1_demo.questions.md'), [
      '---', 'forTask: TASK_1_demo', 'createdAt: 2026-07-13T08:00:00Z', 'updatedAt: 2026-07-13T08:01:00Z',
      'round: 1', 'gapCount: 1', 'prevGapCount: 2', '---', '',
      '## Q1 — What should be preserved?', '', '**Type**: text', '', '### Answer', '',
    ].join('\n'), { mode: 0o600 });
  }
  const todo = [
    '# TASK 1 — Demo', '', SOURCE_BLOCK, '', '## Goal', '', 'Implement the requested behavior.', '',
    '## Inputs', '', '- Existing repository contracts.', '',
    '## Acceptance', '', '### Automated', '', '- Run `node test/contract.mjs`.', '',
    '### Manual', '', '- Inspect the result.', '', '## Out of scope', '', '- Unrelated refactors.', '',
  ].join('\n');
  if (state === 'todo') fs.writeFileSync(path.join(fx.tasks, 'todo', 'TASK_1_demo.md'), todo, { mode: 0o600 });
  if (state === 'done') {
    const done = [
      todo.trimEnd(), '', '---', '', '## Outcome', '', '**Status**: completed',
      '**Completed at**: 2026-07-13T09:00:00Z', '**Reviewer**: codex', '**Review iterations**: 1', '',
      '### Build gates', '', '- `node test/contract.mjs` — pass', '',
      '### Runtime verify', '', '- Gate: skipped (fixture)', '- Result: n/a — fixture', '',
      '### Acceptance trace', '', '- `test/contract.mjs` — verified — Passed.', '',
      '### Caveats', '', '- none', '', '### Follow-ups', '', '- none', '',
      '### Files touched', '', '- `src/feature.js` — modified', '',
    ].join('\n');
    fs.writeFileSync(path.join(fx.tasks, 'done', 'TASK_1_demo.md'), done, { mode: 0o600 });
  }
  publishFreshIndex(fx);
  const result = validateFixtureTask(fx);
  assert.equal(result.observedState, state);
  fx.sourceRevision = result.sourceRevision;
}

function setRepairableBrokenDropState(fx) {
  clearFixtureTask(fx);
  fs.writeFileSync(path.join(fx.tasks, 'backlog', 'TASK_1_demo.md'),
    '# TASK 1 —\n\nTemporarily incomplete migration bytes.\n', { mode: 0o600 });
  const result = taskCore.validateAction(validationOptions(fx, {
    stem: 'TASK_1_demo',
    action: 'drop',
    checkIndex: true,
  }));
  assert.equal(result.observedState, 'backlog');
  assert.equal(taskCore.actionAdmission(result).ok, false);
  assert.equal(taskCore.admissionForAction(result, 'TASK_1_demo').ok, true);
  fx.sourceRevision = result.sourceRevision;
}

function writeActiveFinalizationMarker(fx) {
  const phaseNames = ['outcome', 'components', 'tokens', 'ship', 'index', 'arch', 'verify', 'unlock', 'cleanup'];
  const phases = Object.fromEntries(phaseNames.map((phase) => [phase, { state: 'pending', attempts: 0 }]));
  const proof = {
    present: true, ctimeNs: '4000000000', dev: '1', hash: 'sha256:' + '9'.repeat(64), ino: '2',
    kind: 'file', mode: 33152, mtimeNs: '3000000000', size: 128,
  };
  const marker = {
    version: 1, revision: 1, stem: 'TASK_1_demo', transactionId: 'fin-standby-test',
    status: 'incomplete', phase: 'index', createdAt: '2026-07-13T00:00:00.000Z', updatedAt: '2026-07-13T00:01:00.000Z',
    owner: {
      pid: process.pid, processStartId: writerLeases.captureProcessStartId(process.pid), hostname: os.hostname(),
      invocationId: 'standby-finalization-test', startedAt: '2026-07-13T00:01:00.000Z',
    },
    source: {
      originalHash: 'sha256:' + '0'.repeat(64), intendedHash: 'sha256:' + 'a'.repeat(64),
      intendedLogicalHash: 'sha256:' + 'b'.repeat(64), outcomeHash: 'sha256:' + 'c'.repeat(64),
      snapshotHash: 'sha256:' + 'd'.repeat(64), publishFromHash: 'sha256:' + 'e'.repeat(64), lock: proof,
    },
    figma: { enabled: false, configHash: 'sha256:' + 'f'.repeat(64), pipelineRunId: null },
    phases, artifacts: {},
    lastError: { code: 'INDEX_REGEN_FAILED', message: 'fixture failure', at: '2026-07-13T00:01:00.000Z' },
  };
  fs.mkdirSync(fx.finalizations, { recursive: true, mode: 0o700 });
  fs.writeFileSync(path.join(fx.finalizations, 'TASK_1_demo.json'), JSON.stringify(marker, null, 2) + '\n', { mode: 0o600 });
}

function envFor(fx, extra = {}) {
  return {
    ...process.env,
    // Importing a repository module must not drop bytecode into the source
    // tree; the probe below loads the boundary through importlib.
    PYTHONDONTWRITEBYTECODE: '1',
    ORCHESTRATOR_PROJECT_ROOT: fx.root,
    ORCHESTRATOR_CACHE_DIR: fx.cache,
    ORCHESTRATOR_TASKS_DIR: fx.tasks,
    ORCHESTRATOR_LOCKS_DIR: fx.locks,
    ORCHESTRATOR_FINALIZATIONS_DIR: fx.finalizations,
    ORCHESTRATOR_WRITER_LEASES_DIR: fx.writers,
    ORCHESTRATOR_WRITER_AUTHORITY_ROOT: fx.root,
    ORCHESTRATOR_OUTCOME_SHAPE_PATH: OUTCOME_SHAPE,
    TASK_FS_TEST_ROOT: fx.root,
    ORCHESTRATOR_STANDBY_TEST_ROOT: fx.root,
    ...extra,
  };
}

function run(fx, args, extraEnv = {}, input = undefined) {
  const result = childProcess.spawnSync(process.execPath, [CLI, ...args], {
    cwd: fx.root,
    env: envFor(fx, extraEnv),
    encoding: 'utf8',
    maxBuffer: 4 * 1024 * 1024,
    input,
  });
  let json = null;
  try { json = JSON.parse(result.stdout); } catch {}
  return { ...result, json };
}

function requestRecord(fx, patch = {}) {
  return {
    version: 3,
    action: 'prep',
    stem: 'TASK_1_demo',
    expectedState: 'backlog',
    sourceRevision: fx.sourceRevision,
    dedupKey: null,
    dedupReport: null,
    projectRoot: fx.root,
    prompt: 'TOP_SECRET_PROMPT\nsecond line',
    createdAt: '2026-07-13T00:00:00.000Z',
    ...patch,
  };
}

function putRequest(fx, id = '1000000000000-first', patch = {}) {
  const record = requestRecord(fx, patch);
  fs.mkdirSync(fx.requests, { recursive: true, mode: 0o700 });
  const file = path.join(fx.requests, id + '.json');
  fs.writeFileSync(file, JSON.stringify(record), { mode: 0o600 });
  fs.chmodSync(file, 0o600);
  fs.mkdirSync(fx.reservations, { recursive: true, mode: 0o700 });
  const reservation = {
    version: requestContract.REQUEST_RESERVATION_VERSION,
    requestId: id,
    stem: record.stem,
    fingerprint: 'sha256:' + crypto.createHash('sha256').update(JSON.stringify({
      version: record.version,
      projectRoot: record.projectRoot,
      stem: record.stem,
      action: record.action,
      expectedState: record.expectedState,
      sourceRevision: record.sourceRevision,
      dedupKey: record.dedupKey,
      dedupReport: record.dedupReport,
      prompt: record.prompt,
    }), 'utf8').digest('hex'),
    token: crypto.randomBytes(32).toString('hex'),
    createdAt: '2026-07-13T00:00:00.000Z',
  };
  const reservationFile = path.join(fx.reservations, record.stem + '.json');
  fs.writeFileSync(reservationFile, JSON.stringify(reservation, null, 2) + '\n', { mode: 0o600 });
  fs.chmodSync(reservationFile, 0o600);
  return { id, file, reservationFile };
}

function privateOperation(fx) {
  const names = fs.existsSync(fx.runs) ? fs.readdirSync(fx.runs).filter((name) => /^\.standby-.*-sq-[a-f0-9]{48}$/.test(name)) : [];
  assert.equal(names.length, 1);
  return path.join(fx.runs, names[0]);
}

function beginPass(fx, extraEnv = {}) {
  const result = run(fx, ['begin-pass'], extraEnv);
  assert.equal(result.status, 0, result.stdout + result.stderr);
  assert.equal(result.json?.status, 'ready', result.stdout);
  assert.match(result.json.passToken, /^[a-f0-9]{64}$/);
  return result.json;
}

function claimNext(fx, extraEnv = {}) {
  const pass = beginPass(fx, extraEnv);
  return run(fx, ['claim-next', '--pass-token', pass.passToken], extraEnv);
}

function claim(fx, extraEnv = {}) {
  const result = claimNext(fx, extraEnv);
  assert.equal(result.status, 0, result.stdout + result.stderr);
  assert.equal(result.json?.status, 'claimed', result.stdout);
  return result.json;
}

function reserve(fx, handle) {
  const result = run(fx, ['ensure-reservation', '--handle', handle]);
  assert.equal(result.status, 0, result.stdout + result.stderr);
  assert.equal(result.json?.status, 'reserved');
  return result.json.handle;
}

function release(fx, handle) {
  const result = childProcess.spawnSync(process.execPath, [
    RESERVATION, 'release', '--request-id', handle.requestId, '--stem', handle.stem,
    '--fingerprint', handle.fingerprint, '--token', handle.token, '--created-at', handle.createdAt,
  ], { cwd: fx.root, env: envFor(fx), encoding: 'utf8' });
  assert.equal(result.status, 0, result.stdout + result.stderr);
}

function acquireLease(fx, record = requestRecord(fx)) {
  const handle = writerLeases.acquire(fx.writers, {
    kind: 'task-session', stem: record.stem, key: `standby:${record.action}`,
    ownerPid: process.pid, ttlMs: 60 * 60 * 1000,
    sessionId: writerLeases.createSessionId(), rootDir: fx.root,
  });
  return { leaseId: handle.leaseId, token: handle.token, sessionId: handle.record.sessionId };
}

function releaseLease(fx, receipt) {
  writerLeases.release({
    dir: fx.writers, rootDir: fx.root, path: path.join(fx.writers, receipt.leaseId + '.json'),
    leaseId: receipt.leaseId, token: receipt.token,
  });
}

function leaseArgs(receipt) {
  return ['--lease-id', receipt.leaseId, '--lease-token', receipt.token, '--session-id', receipt.sessionId];
}

function runTaskLock(fx, args) {
  const result = childProcess.spawnSync(process.execPath, [TASK_LOCK, ...args], {
    cwd: fx.root,
    env: envFor(fx),
    encoding: 'utf8',
    maxBuffer: 4 * 1024 * 1024,
  });
  let json = null;
  try { json = JSON.parse(result.stdout); } catch {}
  return { ...result, json };
}

function acquireStandbyTaskLock(fx, record, lease, suffix = 'queue') {
  const stage = record.action === 'run' ? 'orchestrator' : 'task-prep';
  const result = runTaskLock(fx, [
    'acquire', '--stem', record.stem, '--stage', stage,
    '--run-id', `run-standby-${suffix.padEnd(8, 'x')}`,
    '--session-id', lease.sessionId,
    '--writer-lease-id', lease.leaseId,
    '--writer-lease-token', lease.token,
    '--owner-kind', 'standby', '--owner-id', `standby:${lease.leaseId}`,
    '--owner-pid', String(process.pid),
  ]);
  assert.equal(result.status, 0, result.stdout + result.stderr);
  assert.equal(result.json?.ok, true, result.stdout);
  return result.json;
}

function releaseTaskLock(fx, receipt) {
  const result = runTaskLock(fx, [
    'release', '--stem', receipt.stem, '--run-id', receipt.runId,
    '--session-id', receipt.sessionId, '--expected-hash', receipt.lockHash,
    '--expected-state', receipt.observedState, '--source-revision', receipt.sourceRevision,
  ]);
  assert.equal(result.status, 0, result.stdout + result.stderr);
  assert.equal(result.json?.released, true, result.stdout);
}

function acquireDirectTaskLock(fx, stem, suffix = 'foreign') {
  const result = runTaskLock(fx, [
    'acquire', '--stem', stem, '--stage', 'orchestrator',
    '--run-id', `run-direct-${suffix.padEnd(8, 'x')}`,
    '--session-id', `ws-direct-${suffix.padEnd(16, 'x')}`,
    '--owner-kind', 'agent', '--owner-id', `agent:${suffix}`, '--owner-pid', String(process.pid),
  ]);
  assert.equal(result.status, 0, result.stdout + result.stderr);
  return result.json;
}

function prepare(fx, queueHandle, receipt, extraEnv = {}) {
  return run(fx, ['prepare-execution', '--handle', queueHandle, ...leaseArgs(receipt)], extraEnv);
}

function disclose(fx, queueHandle, receipt, extraEnv = {}) {
  return run(fx, ['read-prompt', '--handle', queueHandle, ...leaseArgs(receipt)], extraEnv);
}

function supersede(fx, queueHandle, receipt, extraEnv = {}) {
  return run(fx, ['record-superseded', '--handle', queueHandle, ...leaseArgs(receipt)], extraEnv);
}

function prepareAndDisclose(fx, queueHandle, record = requestRecord(fx)) {
  const lease = acquireLease(fx, record);
  const reservation = reserve(fx, queueHandle);
  const prepared = prepare(fx, queueHandle, lease);
  assert.equal(prepared.status, 0, prepared.stdout);
  release(fx, reservation);
  const disclosed = disclose(fx, queueHandle, lease);
  assert.equal(disclosed.status, 0, disclosed.stdout + disclosed.stderr);
  releaseLease(fx, lease);
  return disclosed.stdout;
}

function exactProof(file) {
  const st = fs.lstatSync(file, { bigint: true });
  const type = st.isDirectory() ? 'directory' : st.isFile() ? 'file' : st.isSymbolicLink() ? 'symlink' : 'other';
  return {
    ctimeNs: String(st.ctimeNs), dev: String(st.dev), ino: String(st.ino), mode: String(st.mode),
    mtimeNs: String(st.mtimeNs), nlink: String(st.nlink), size: String(st.size), type,
  };
}

test('module import is inert even with hostile argv', () => {
  const fx = fixture({ createLocks: false });
  const script = `process.argv.push('claim-next'); await import(${JSON.stringify(pathToFileURL(CLI).href)});`;
  const result = childProcess.spawnSync(process.execPath, ['--input-type=module', '-e', script], {
    cwd: fx.root, env: envFor(fx), encoding: 'utf8',
  });
  assert.equal(result.status, 0, result.stderr);
  const pythonImport = `import importlib.util; s=importlib.util.spec_from_file_location('standby_boundary', ${JSON.stringify(BOUNDARY)}); m=importlib.util.module_from_spec(s); s.loader.exec_module(m)`;
  const importedBoundary = childProcess.spawnSync('python3', ['-c', pythonImport], { cwd: fx.root, env: envFor(fx), encoding: 'utf8' });
  assert.equal(importedBoundary.status, 0, importedBoundary.stderr);
  assert.equal(fs.existsSync(fx.cache), false);
});

test('boundary subprocess is isolated from Python runtime environment injection', () => {
  const fx = fixture();
  const injectedEnv = envFor(fx, { PYTHONPLATLIBDIR: '__standby_invalid__' });
  const control = childProcess.spawnSync('python3', ['-c', 'pass'], { env: injectedEnv, encoding: 'utf8' });
  assert.notEqual(control.status, 0, 'control must prove the injected runtime setting is effective');

  putRequest(fx);
  const claimed = claimNext(fx, { PYTHONPLATLIBDIR: '__standby_invalid__' });
  assert.equal(claimed.status, 0, claimed.stdout + claimed.stderr);
  assert.equal(claimed.json?.status, 'claimed');
});

test('begin-pass publishes an exact private atomic heartbeat', () => {
  const fx = fixture();
  const result = run(fx, ['begin-pass']);
  assert.equal(result.status, 0, result.stdout);
  assert.equal(result.json?.status, 'ready');
  assert.match(result.json?.passToken, /^[a-f0-9]{64}$/);
  const file = path.join(fx.worker, 'heartbeat.json');
  const raw = fs.readFileSync(file, 'utf8');
  const value = JSON.parse(raw);
  assert.deepEqual(Object.keys(value).sort(), ['at', 'passTokenHash', 'projectRoot', 'state', 'version']);
  assert.equal(value.version, 1);
  assert.equal(value.projectRoot, fx.root);
  assert.equal(value.state, 'ready');
  assert.equal(value.passTokenHash, 'sha256:' + crypto.createHash('sha256').update(result.json.passToken).digest('hex'));
  assert.equal(raw.includes(result.json.passToken), false);
  if (process.platform !== 'win32') assert.equal(fs.lstatSync(file).mode & 0o777, 0o600);
});

test('fresh exact runner marker stands down without heartbeat', () => {
  const fx = fixture();
  fs.mkdirSync(fx.runs, { recursive: true, mode: 0o700 });
  fs.writeFileSync(path.join(fx.runs, '.runner-alive'), JSON.stringify({
    version: 1, at: new Date().toISOString(), pid: process.pid,
    processStartId: writerLeases.captureProcessStartId(process.pid), projectRoot: fx.root,
  }) + '\n', { mode: 0o600 });
  const result = run(fx, ['begin-pass']);
  assert.deepEqual(result.json, { status: 'runner-active' });
  assert.equal(fs.existsSync(path.join(fx.worker, 'heartbeat.json')), false);
});

test('non-conforming marker content never ages into takeover authority', () => {
  const fx = fixture();
  fs.mkdirSync(fx.runs, { recursive: true, mode: 0o700 });
  const marker = path.join(fx.runs, '.runner-alive');
  // A pre-processStartId marker: non-conforming CONTENT, not a structural fault.
  const legacy = JSON.stringify({ at: new Date().toISOString(), pid: 123, projectRoot: fx.root }) + '\n';
  fs.writeFileSync(marker, legacy, { mode: 0o600 });
  const fresh = run(fx, ['begin-pass']);
  assert.deepEqual(fresh.json, { status: 'runner-unknown' });
  assert.equal(fs.existsSync(path.join(fx.worker, 'heartbeat.json')), false);
  const aged = new Date(Date.now() - 120000);
  fs.utimesSync(marker, aged, aged);
  const stale = run(fx, ['begin-pass']);
  assert.deepEqual(stale.json, { status: 'runner-unknown' });
  assert.equal(fs.existsSync(path.join(fx.worker, 'heartbeat.json')), false);
  assert.equal(fs.existsSync(marker), true, 'the standby never deletes markers');
});

test('unsafe or malformed runner marker is fail-closed and writes no heartbeat', () => {
  const fx = fixture();
  fs.mkdirSync(fx.runs, { recursive: true, mode: 0o700 });
  fs.writeFileSync(path.join(fx.runs, '.runner-alive'), '{bad', { mode: 0o600 });
  const result = run(fx, ['begin-pass']);
  assert.deepEqual(result.json, { status: 'runner-unknown' });
  assert.equal(fs.existsSync(path.join(fx.worker, 'heartbeat.json')), false);
});

test('exact marker with a proven dead owner permits heartbeat without deleting marker', () => {
  const fx = fixture();
  fs.mkdirSync(fx.runs, { recursive: true, mode: 0o700 });
  const marker = path.join(fx.runs, '.runner-alive');
  fs.writeFileSync(marker, JSON.stringify({
    version: 1, at: '2020-01-01T00:00:00.000Z', pid: 2147483647,
    processStartId: writerLeases.captureProcessStartId(process.pid), projectRoot: fx.root,
  }) + '\n', { mode: 0o600 });
  assert.equal(run(fx, ['begin-pass']).json?.status, 'ready');
  assert.equal(fs.existsSync(marker), true);
});

test('claim-next requires the exact one-shot token and replay cannot move a later request', () => {
  const fx = fixture();
  const pass = beginPass(fx);
  const first = run(fx, ['claim-next', '--pass-token', pass.passToken]);
  assert.deepEqual(first.json, { status: 'empty' });
  const queued = putRequest(fx);
  const replay = run(fx, ['claim-next', '--pass-token', pass.passToken]);
  assert.deepEqual(replay.json, { status: 'error', code: 'pass-token-invalid' });
  assert.equal(fs.existsSync(queued.file), true);
  assert.equal(fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length, 0);
});

test('a newer begin-pass generation invalidates every older pass token', () => {
  const fx = fixture();
  const queued = putRequest(fx);
  const older = beginPass(fx);
  const current = beginPass(fx);
  const stale = run(fx, ['claim-next', '--pass-token', older.passToken]);
  assert.deepEqual(stale.json, { status: 'error', code: 'pass-token-invalid' });
  assert.equal(fs.existsSync(queued.file), true);
  const claimed = run(fx, ['claim-next', '--pass-token', current.passToken]);
  assert.equal(claimed.json?.status, 'claimed', claimed.stdout);
});

test('expired pass token fails closed before queue authority', () => {
  const fx = fixture();
  const queued = putRequest(fx);
  const pass = beginPass(fx);
  const heartbeat = path.join(fx.worker, 'heartbeat.json');
  const value = JSON.parse(fs.readFileSync(heartbeat, 'utf8'));
  value.at = '2020-01-01T00:00:00.000Z';
  fs.writeFileSync(heartbeat, JSON.stringify(value) + '\n', { mode: 0o600 });
  fs.chmodSync(heartbeat, 0o600);
  const expired = run(fx, ['claim-next', '--pass-token', pass.passToken]);
  assert.deepEqual(expired.json, { status: 'error', code: 'pass-token-expired' });
  assert.equal(fs.existsSync(queued.file), true);
});

test('pass-token heartbeat must remain canonical, private, regular, and single-linked', { skip: process.platform === 'win32' }, () => {
  for (const mutation of ['noncanonical', 'public-mode', 'hardlinked', 'symlink']) {
    const fx = fixture();
    const queued = putRequest(fx);
    const pass = beginPass(fx);
    const heartbeat = path.join(fx.worker, 'heartbeat.json');
    const raw = fs.readFileSync(heartbeat);
    if (mutation === 'noncanonical') {
      fs.writeFileSync(heartbeat, JSON.stringify(JSON.parse(raw.toString('utf8')), null, 2) + '\n', { mode: 0o600 });
    } else if (mutation === 'public-mode') {
      fs.chmodSync(heartbeat, 0o644);
    } else if (mutation === 'hardlinked') {
      fs.linkSync(heartbeat, path.join(fx.root, 'heartbeat-alias.json'));
    } else {
      const outside = path.join(fx.root, 'foreign-heartbeat.json');
      fs.writeFileSync(outside, raw, { mode: 0o600 });
      fs.unlinkSync(heartbeat);
      fs.symlinkSync(outside, heartbeat);
    }
    const rejected = run(fx, ['claim-next', '--pass-token', pass.passToken]);
    assert.deepEqual(rejected.json, { status: 'error', code: 'pass-token-invalid' }, mutation);
    assert.equal(fs.existsSync(queued.file), true, mutation);
    assert.equal(fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length, 0, mutation);
  }
});

test('claim-next rechecks exact runner exclusion after begin-pass and before queue movement', () => {
  for (const [marker, expected] of [
    [{ version: 1, at: new Date().toISOString(), pid: process.pid,
      processStartId: writerLeases.captureProcessStartId(process.pid), projectRoot: null }, 'runner-active'],
    ['{bad', 'runner-unknown'],
  ]) {
    const fx = fixture();
    const queued = putRequest(fx);
    const pass = beginPass(fx);
    const markerFile = path.join(fx.runs, '.runner-alive');
    const bytes = typeof marker === 'string' ? marker : JSON.stringify({ ...marker, projectRoot: fx.root }) + '\n';
    fs.writeFileSync(markerFile, bytes, { mode: 0o600 });
    fs.chmodSync(markerFile, 0o600);
    const result = run(fx, ['claim-next', '--pass-token', pass.passToken]);
    assert.deepEqual(result.json, { status: expected });
    assert.equal(fs.existsSync(queued.file), true, expected);
    assert.equal(fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length, 0, expected);
  }
});

test('claim-next refuses a NEW claim while any board-task writer lease is active (frozen serial safety)', () => {
  const fx = fixture();
  const queued = putRequest(fx);
  // A live board-task writer — the durable shape of a site runner session, a
  // runner-orphaned child, or another standby execution. Owner is this test
  // process, so the lease scans as ACTIVE.
  const busy = writerLeases.acquire(fx.writers, {
    kind: 'task-session', stem: 'TASK_7_running_elsewhere', key: 'task:TASK_7_running_elsewhere',
    sessionId: writerLeases.createSessionId(), ownerPid: process.pid, rootDir: fx.root,
  });
  const blocked = run(fx, ['claim-next', '--pass-token', beginPass(fx).passToken]);
  assert.deepEqual(blocked.json, { status: 'blocked', code: 'task-writer-active' });
  assert.equal(fs.existsSync(queued.file), true, 'no request may move while a board-task writer is live');
  assert.equal(fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length, 0);
  // Control-plane writers do not occupy the board-task slot.
  writerLeases.release(busy);
  const controlPlane = writerLeases.acquire(fx.writers, {
    kind: 'workspace-session', stem: null, key: 'figma:screens:TASK_7_running_elsewhere',
    sessionId: writerLeases.createSessionId(), ownerPid: process.pid, rootDir: fx.root,
  });
  const claimed = run(fx, ['claim-next', '--pass-token', beginPass(fx).passToken]);
  assert.equal(claimed.json?.status, 'claimed', JSON.stringify(claimed.json));
  writerLeases.release(controlPlane);
});

test('symlinked cache ancestor cannot receive heartbeat or queue authority', { skip: process.platform === 'win32' }, () => {
  const fx = fixture({ createLocks: false });
  const outside = path.join(fx.root, 'foreign-cache');
  fs.mkdirSync(outside, { mode: 0o700 });
  fs.mkdirSync(path.join(fx.root, 'orchestrator'), { recursive: true });
  fs.symlinkSync(outside, fx.cache);
  const result = run(fx, ['begin-pass']);
  assert.equal(result.json?.code, 'queue-authority-unsafe');
  assert.equal(fs.readdirSync(outside).length, 0);
});

test('claim-next selects exact oldest and exposes only prompt-free projection', () => {
  const fx = fixture();
  putRequest(fx, '2000000000000-second', { prompt: 'SECOND_SECRET', stem: 'TASK_2_second' });
  putRequest(fx, '1000000000000-first');
  const result = claim(fx);
  assert.equal(result.request.id, '1000000000000-first');
  assert.equal(JSON.stringify(result).includes('TOP_SECRET_PROMPT'), false);
  assert.equal(JSON.stringify(result).includes('dedup'), false);
  assert.equal(fs.existsSync(path.join(fx.requests, '1000000000000-first.json')), false);
  assert.equal(fs.existsSync(path.join(fx.requests, '2000000000000-second.json')), true);
});

test('status and reservation projection never disclose prompt or dedup fields', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const status = run(fx, ['status', '--handle', claimed.handle]);
  const reserved = run(fx, ['ensure-reservation', '--handle', claimed.handle]);
  assert.equal(status.status, 0, status.stdout + status.stderr);
  assert.equal(reserved.status, 0, reserved.stdout + reserved.stderr);
  assert.equal((status.stdout + reserved.stdout).includes('TOP_SECRET_PROMPT'), false);
  assert.equal((status.stdout + reserved.stdout).includes('dedupKey'), false);
});

test('restore requires exact active reservation and restores exact inode no-clobber', () => {
  const fx = fixture();
  const original = putRequest(fx);
  const originalIno = fs.lstatSync(original.file).ino;
  const claimed = claim(fx);
  const reservationBytes = fs.readFileSync(original.reservationFile);
  fs.unlinkSync(original.reservationFile);
  assert.equal(run(fx, ['restore', '--handle', claimed.handle]).json?.code, 'exact-reservation-required');
  fs.writeFileSync(original.reservationFile, reservationBytes, { mode: 0o600 });
  reserve(fx, claimed.handle);
  const restored = run(fx, ['restore', '--handle', claimed.handle]);
  assert.deepEqual(restored.json, { status: 'restored', id: original.id });
  assert.equal(fs.lstatSync(original.file).ino, originalIno);
  assert.equal(fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length, 0);
});

test('restore collision retains same-bytes foreign inode and private claim', () => {
  const fx = fixture();
  const original = putRequest(fx);
  const bytes = fs.readFileSync(original.file);
  const claimed = claim(fx);
  reserve(fx, claimed.handle);
  fs.writeFileSync(original.file, bytes, { mode: 0o600 });
  const foreignIno = fs.lstatSync(original.file).ino;
  const result = run(fx, ['restore', '--handle', claimed.handle]);
  assert.deepEqual(result.json, { status: 'blocked', code: 'restore-collision' });
  assert.equal(fs.lstatSync(original.file).ino, foreignIno);
  assert.equal(fs.existsSync(path.join(privateOperation(fx), 'request.claim')), true);
});

test('prepare-execution requires reservation and creates a durable prompt-free phase', () => {
  const fx = fixture();
  const original = putRequest(fx);
  const claimed = claim(fx);
  const lease = acquireLease(fx);
  const reservationBytes = fs.readFileSync(original.reservationFile);
  fs.unlinkSync(original.reservationFile);
  assert.equal(prepare(fx, claimed.handle, lease).json?.code, 'exact-reservation-required');
  fs.writeFileSync(original.reservationFile, reservationBytes, { mode: 0o600 });
  reserve(fx, claimed.handle);
  const result = prepare(fx, claimed.handle, lease);
  assert.equal(result.json?.status, 'execution-prepared', result.stdout + result.stderr);
  assert.equal(result.stdout.includes('TOP_SECRET_PROMPT'), false);
  const receipt = JSON.parse(fs.readFileSync(path.join(privateOperation(fx), 'execution.json'), 'utf8'));
  assert.equal(receipt.fence.leaseId, lease.leaseId);
  assert.equal(receipt.fence.sourceRevision, fx.sourceRevision);
  assert.equal(receipt.fence.indexStatus, 'fresh');
  assert.match(receipt.fence.snapshotHash, /^sha256:[a-f0-9]{64}$/);
  assert.equal(JSON.stringify(receipt).includes(lease.token), false);
});

test('prepared but undisclosed execution can restore only with its exact reservation', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const lease = acquireLease(fx);
  reserve(fx, claimed.handle);
  assert.equal(prepare(fx, claimed.handle, lease).json?.status, 'execution-prepared');
  const activeRestore = run(fx, ['restore', '--handle', claimed.handle]);
  assert.deepEqual(activeRestore.json, { status: 'error', code: 'writer-lease-release-unproven' });
  assert.equal(fs.existsSync(path.join(privateOperation(fx), 'request.claim')), true);
  assert.equal(fs.existsSync(path.join(fx.requests, claimed.request.id + '.json')), false);
  releaseLease(fx, lease);
  const restored = run(fx, ['restore', '--handle', claimed.handle]);
  assert.equal(restored.status, 0, restored.stdout + restored.stderr);
  assert.equal(restored.json?.status, 'restored');
  assert.equal(fs.existsSync(path.join(fx.requests, claimed.request.id + '.json')), true);
  assert.equal(fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length, 0);
});

test('prompt remains sealed until reservation withdrawal', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const lease = acquireLease(fx);
  reserve(fx, claimed.handle);
  prepare(fx, claimed.handle, lease);
  const result = disclose(fx, claimed.handle, lease);
  assert.equal(result.json?.code, 'reservation-still-active');
  assert.equal(result.stdout.includes('TOP_SECRET_PROMPT'), false);
});

test('disclosure receipt is durable before one-shot exact prompt output', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  assert.equal(prepareAndDisclose(fx, claimed.handle), requestRecord(fx).prompt);
  const operation = privateOperation(fx);
  const executionReceipt = JSON.parse(fs.readFileSync(path.join(operation, 'execution.json'), 'utf8'));
  const disclosureReceipt = JSON.parse(fs.readFileSync(path.join(operation, 'disclosed.json'), 'utf8'));
  for (const field of ['action', 'stem', 'expectedState', 'sourceRevision', 'indexStatus', 'leaseId', 'sessionId']) {
    assert.equal(disclosureReceipt.fence[field], executionReceipt.fence[field], field);
  }
  assert.match(disclosureReceipt.fence.snapshotHash, /^sha256:[a-f0-9]{64}$/);
  assert.equal(JSON.stringify(disclosureReceipt).includes('TOP_SECRET_PROMPT'), false);
  assert.equal(Object.hasOwn(disclosureReceipt.fence, 'token'), false);
  const replay = run(fx, ['read-prompt', '--handle', claimed.handle, ...leaseArgs(acquireLease(fx))]);
  assert.equal(replay.json?.code, 'prompt-already-disclosed');
  assert.equal(replay.stdout.includes('TOP_SECRET_PROMPT'), false);
});

test('all canonical queue actions share the complete execution-to-consume fence contract', () => {
  // Worktree isolation Phase 2: `run` executes only inside the site runner's
  // provisioned worktree, so the standby's execution-to-consume contract now
  // covers the four control-plane actions; the run is refused outright.
  const cases = [
    ['prep', 'backlog'],
    ['answers', 'pending'],
    ['drop', 'backlog'],
    ['reopen', 'done'],
  ];
  assert.deepEqual([...requestContract.REQUEST_ACTIONS].sort(),
    cases.map(([action]) => action).concat('run').sort());
  const refusedRun = fixture();
  setFixtureState(refusedRun, 'todo');
  putRequest(refusedRun, '1000000000000-first', { action: 'run', expectedState: 'todo' });
  assert.deepEqual(claimNext(refusedRun).json, { status: 'blocked', code: 'run-requires-site-runner' });
  for (const [action, state] of cases) {
    const fx = fixture();
    setFixtureState(fx, state);
    const record = requestRecord(fx, { action, expectedState: state });
    putRequest(fx, '1000000000000-first', { action, expectedState: state });
    const claimed = claim(fx);
    assert.equal(prepareAndDisclose(fx, claimed.handle, record), record.prompt, action);
    const consumed = run(fx, ['consume', '--handle', claimed.handle, '--kind', 'executed']);
    assert.deepEqual(consumed.json, { status: 'consumed', id: '1000000000000-first', kind: 'executed' }, action + ': ' + consumed.stdout);
  }

  const fx = fixture();
  putRequest(fx, '1000000000000-first', { action: 'finalize', expectedState: 'todo' });
  const rejected = claimNext(fx);
  assert.deepEqual(rejected.json, { status: 'retained', code: 'contract-invalid' });
  assert.equal(fs.existsSync(path.join(privateOperation(fx), 'execution.json')), false);
  assert.equal(rejected.stdout.includes('TOP_SECRET_PROMPT'), false);
});

test('repair-admitted malformed Drop stays executable and is never falsely superseded by standby', () => {
  const executable = fixture();
  setRepairableBrokenDropState(executable);
  const executableRecord = requestRecord(executable, { action: 'drop', expectedState: 'backlog' });
  putRequest(executable, '1000000000000-first', { action: 'drop', expectedState: 'backlog' });
  const executableClaim = claim(executable);
  assert.equal(prepareAndDisclose(executable, executableClaim.handle, executableRecord), executableRecord.prompt);
  assert.deepEqual(run(executable, [
    'consume', '--handle', executableClaim.handle, '--kind', 'executed',
  ]).json, {
    status: 'consumed', id: '1000000000000-first', kind: 'executed',
  });

  const retained = fixture();
  setRepairableBrokenDropState(retained);
  putRequest(retained, '1000000000000-first', { action: 'drop', expectedState: 'backlog' });
  const retainedClaim = claim(retained);
  const lease = acquireLease(retained, requestRecord(retained, { action: 'drop', expectedState: 'backlog' }));
  reserve(retained, retainedClaim.handle);
  const verdict = supersede(retained, retainedClaim.handle, lease);
  assert.deepEqual(verdict.json, { status: 'error', code: 'supersession-not-proven' });
  assert.equal(fs.existsSync(path.join(retained.superseded, retainedClaim.request.id + '.json')), false);
});

test('executed consume requires disclosure and removes only exact claim after receipt', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const lease = acquireLease(fx);
  const reservation = reserve(fx, claimed.handle);
  prepare(fx, claimed.handle, lease);
  release(fx, reservation);
  assert.equal(run(fx, ['consume', '--handle', claimed.handle, '--kind', 'executed']).json?.code, 'disclosure-receipt-required');
  assert.equal(disclose(fx, claimed.handle, lease).status, 0);
  const activeLeaseConsume = run(fx, ['consume', '--handle', claimed.handle, '--kind', 'executed']);
  assert.deepEqual(activeLeaseConsume.json, { status: 'error', code: 'writer-lease-release-unproven' });
  assert.equal(fs.existsSync(path.join(privateOperation(fx), 'request.claim')), true);
  releaseLease(fx, lease);
  fs.writeFileSync(path.join(fx.writers, 'foreign-evidence'), 'x\n', { mode: 0o600 });
  const unsafeScanConsume = run(fx, ['consume', '--handle', claimed.handle, '--kind', 'executed']);
  assert.deepEqual(unsafeScanConsume.json, { status: 'error', code: 'writer-lease-release-unproven' });
  assert.equal(fs.existsSync(path.join(privateOperation(fx), 'request.claim')), true);
  fs.unlinkSync(path.join(fx.writers, 'foreign-evidence'));
  const consumed = run(fx, ['consume', '--handle', claimed.handle, '--kind', 'executed']);
  assert.deepEqual(consumed.json, { status: 'consumed', id: '1000000000000-first', kind: 'executed' });
  assert.equal(fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length, 0);
});

test('executed prep consume requires canonical task-lock release settlement', () => {
  const fx = fixture();
  const record = requestRecord(fx);
  putRequest(fx);
  const claimed = claim(fx);
  const lease = acquireLease(fx, record);
  const reservation = reserve(fx, claimed.handle);
  assert.equal(prepare(fx, claimed.handle, lease).json?.status, 'execution-prepared');
  release(fx, reservation);
  assert.equal(disclose(fx, claimed.handle, lease).status, 0);
  const taskLock = acquireStandbyTaskLock(fx, record, lease, 'prep-retained');
  releaseLease(fx, lease);
  const retained = run(fx, ['consume', '--handle', claimed.handle, '--kind', 'executed']);
  assert.deepEqual(retained.json, { status: 'error', code: 'task-lock-absence-unproven' });
  assert.equal(fs.existsSync(path.join(privateOperation(fx), 'request.claim')), true);
  releaseTaskLock(fx, taskLock);
  assert.equal(run(fx, ['consume', '--handle', claimed.handle, '--kind', 'executed']).json?.status, 'consumed');
});

test('run requests are invisible to the standby: never claimed, never blocking younger work', () => {
  // The oldest-first rule skips `run` in BOTH mirrors (the JS peek and the
  // python boundary), so an older run must not wedge younger non-run work —
  // and a run-only queue is honestly blocked for the site runner.
  const fairness = fixture();
  setFixtureState(fairness, 'todo');
  const olderRun = putRequest(fairness, '1000000000000-first', { action: 'run', expectedState: 'todo' });
  const newerDrop = putRequest(fairness, '2000000000000-second', { action: 'drop', expectedState: 'todo' });
  const claimed = run(fairness, ['claim-next', '--pass-token', beginPass(fairness).passToken]);
  assert.equal(claimed.json?.status, 'claimed', JSON.stringify(claimed.json));
  assert.equal(claimed.json?.request?.action, 'drop', 'younger non-run work drains past the older run');
  assert.equal(fs.existsSync(olderRun.file), true, 'the run request stays queued untouched');
  assert.equal(fs.existsSync(newerDrop.file), false, 'the claimed request moved out of the public queue');

  const runOnly = fixture();
  setFixtureState(runOnly, 'todo');
  const only = putRequest(runOnly, '1000000000000-first', { action: 'run', expectedState: 'todo' });
  const blocked = run(runOnly, ['claim-next', '--pass-token', beginPass(runOnly).passToken]);
  assert.deepEqual(blocked.json, { status: 'blocked', code: 'run-requires-site-runner' });
  assert.equal(fs.existsSync(only.file), true, 'a run-only queue is left entirely to the site runner');
  assert.equal(fs.readdirSync(runOnly.runs).filter((name) => name.startsWith('.standby-')).length, 0);
});

test('executed consume requires a proven-absent task lock for every standby action', () => {
  // Worktree isolation Phase 2 deleted the standby `run` path and with it the
  // orchestrator-stage retained-lock tolerance: any standby execution that
  // leaves a task lock behind cannot consume its request.
  const clean = fixture();
  setFixtureState(clean, 'pending');
  const cleanRecord = requestRecord(clean, { action: 'answers', expectedState: 'pending' });
  putRequest(clean, '1000000000000-first', { action: 'answers', expectedState: 'pending' });
  const cleanClaim = claim(clean);
  assert.equal(prepareAndDisclose(clean, cleanClaim.handle, cleanRecord), cleanRecord.prompt);
  assert.deepEqual(run(clean, ['consume', '--handle', cleanClaim.handle, '--kind', 'executed']).json,
    { status: 'consumed', id: '1000000000000-first', kind: 'executed' });

  const retained = fixture();
  setFixtureState(retained, 'pending');
  const retainedRecord = requestRecord(retained, { action: 'answers', expectedState: 'pending' });
  putRequest(retained, '1000000000000-first', { action: 'answers', expectedState: 'pending' });
  const retainedClaim = claim(retained);
  const lease = acquireLease(retained, retainedRecord);
  const reservation = reserve(retained, retainedClaim.handle);
  assert.equal(prepare(retained, retainedClaim.handle, lease).json?.status, 'execution-prepared');
  release(retained, reservation);
  assert.equal(disclose(retained, retainedClaim.handle, lease).status, 0);
  const taskLock = acquireStandbyTaskLock(retained, retainedRecord, lease, 'retained');
  releaseLease(retained, lease);
  assert.deepEqual(run(retained, ['consume', '--handle', retainedClaim.handle, '--kind', 'executed']).json,
    { status: 'error', code: 'task-lock-absence-unproven' });
  releaseTaskLock(retained, taskLock);
});

test('mechanical execution fence rejects absent and changed exact task state', () => {
  const mutateCases = [
    ['absent-task', (fx) => fs.unlinkSync(path.join(fx.tasks, 'backlog', 'TASK_1_demo.md'))],
    ['changed-source', (fx) => {
      fs.appendFileSync(path.join(fx.tasks, 'backlog', 'TASK_1_demo.md'), '\nChanged after queue admission.\n');
      publishFreshIndex(fx);
    }],
  ];
  for (const [label, mutate] of mutateCases) {
    const fx = fixture();
    putRequest(fx);
    const claimed = claim(fx);
    const lease = acquireLease(fx);
    reserve(fx, claimed.handle);
    mutate(fx);
    const rejected = prepare(fx, claimed.handle, lease);
    assert.deepEqual(rejected.json, { status: 'error', code: 'task-state-fence-rejected' }, label + ': ' + rejected.stdout);
    assert.equal(fs.existsSync(path.join(privateOperation(fx), 'execution.json')), false, label);
    assert.equal(rejected.stdout.includes('TOP_SECRET_PROMPT'), false, label);
  }
});

test('mechanical execution fence treats stale or malformed INDEX as advisory for an unchanged task', () => {
  for (const [label, mutate, expectedIndexStatus] of [
    ['stale-index', (fx) => {
      const indexPath = path.join(fx.tasks, 'INDEX.json');
      const index = JSON.parse(fs.readFileSync(indexPath, 'utf8'));
      index.backlog[0].title = 'Stale derived title';
      fs.writeFileSync(indexPath, JSON.stringify(index, null, 2) + '\n', { mode: 0o600 });
    }, 'stale'],
    ['malformed-index', (fx) => fs.writeFileSync(path.join(fx.tasks, 'INDEX.json'), '{bad\n', { mode: 0o600 }), 'invalid'],
  ]) {
    const fx = fixture();
    putRequest(fx);
    const claimed = claim(fx);
    const lease = acquireLease(fx);
    reserve(fx, claimed.handle);
    mutate(fx);
    const prepared = prepare(fx, claimed.handle, lease);
    assert.equal(prepared.json?.status, 'execution-prepared', label + ': ' + prepared.stdout);
    const receipt = JSON.parse(fs.readFileSync(path.join(privateOperation(fx), 'execution.json'), 'utf8'));
    assert.equal(receipt.fence.indexStatus, expectedIndexStatus, label);
  }
});

test('prompt disclosure repeats the exact task fence after reservation withdrawal', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const lease = acquireLease(fx);
  const reservation = reserve(fx, claimed.handle);
  assert.equal(prepare(fx, claimed.handle, lease).json?.status, 'execution-prepared');
  release(fx, reservation);
  fs.appendFileSync(path.join(fx.tasks, 'backlog', 'TASK_1_demo.md'), '\nChanged after execution receipt.\n');
  publishFreshIndex(fx);
  const rejected = disclose(fx, claimed.handle, lease);
  assert.deepEqual(rejected.json, { status: 'error', code: 'task-state-fence-rejected' }, rejected.stdout);
  assert.equal(fs.existsSync(path.join(privateOperation(fx), 'disclosed.json')), false);
  assert.equal(rejected.stdout.includes('TOP_SECRET_PROMPT'), false);
});

test('prompt disclosure remains available when only derived INDEX becomes invalid', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const lease = acquireLease(fx);
  const reservation = reserve(fx, claimed.handle);
  assert.equal(prepare(fx, claimed.handle, lease).json?.status, 'execution-prepared');
  release(fx, reservation);
  fs.writeFileSync(path.join(fx.tasks, 'INDEX.json'), '[]\n', { mode: 0o600 });
  const disclosed = disclose(fx, claimed.handle, lease);
  assert.equal(disclosed.status, 0, disclosed.stdout + disclosed.stderr);
  assert.equal(disclosed.stdout, requestRecord(fx).prompt);
  const receipt = JSON.parse(fs.readFileSync(path.join(privateOperation(fx), 'disclosed.json'), 'utf8'));
  assert.equal(receipt.fence.indexStatus, 'invalid');
});

test('exact writer lease generation and conflict checks gate execution and disclosure', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const lease = acquireLease(fx);
  const reservation = reserve(fx, claimed.handle);
  const wrong = { ...lease, token: 'f'.repeat(64) };
  assert.deepEqual(prepare(fx, claimed.handle, wrong).json, { status: 'error', code: 'writer-lease-authority-lost' });

  const foreign = acquireLease(fx);
  assert.deepEqual(prepare(fx, claimed.handle, lease).json, { status: 'error', code: 'writer-lease-conflict' });
  releaseLease(fx, foreign);
  assert.equal(prepare(fx, claimed.handle, lease).json?.status, 'execution-prepared');
  release(fx, reservation);
  releaseLease(fx, lease);
  const rejected = disclose(fx, claimed.handle, lease);
  assert.deepEqual(rejected.json, { status: 'error', code: 'writer-lease-authority-lost' });
  assert.equal(fs.existsSync(path.join(privateOperation(fx), 'disclosed.json')), false);
  assert.equal(rejected.stdout.includes('TOP_SECRET_PROMPT'), false);
});

test('any canonical task-lock entry blocks execution before a receipt or prompt exists', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const lease = acquireLease(fx);
  reserve(fx, claimed.handle);
  fs.writeFileSync(path.join(fx.locks, 'TASK_1_demo.json'), '{}\n', { mode: 0o600 });
  const result = prepare(fx, claimed.handle, lease);
  assert.deepEqual(result.json, { status: 'error', code: 'task-lock-absence-unproven' });
  assert.equal(fs.existsSync(path.join(privateOperation(fx), 'execution.json')), false);
  assert.equal(result.stdout.includes('TOP_SECRET_PROMPT'), false);
});

test('healthy or caller-forged supersession is rejected without a tombstone', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const lease = acquireLease(fx);
  reserve(fx, claimed.handle);
  const forged = run(fx, ['record-superseded', '--handle', claimed.handle, '--reason', 'task-integrity-invalid'], {},
    JSON.stringify({ observedState: null, sourceRevision: null, snapshotHash: null, findings: [] }));
  assert.equal(forged.status, 64);
  const healthy = supersede(fx, claimed.handle, lease);
  assert.deepEqual(healthy.json, { status: 'error', code: 'supersession-not-proven' });
  assert.equal(fs.existsSync(path.join(fx.superseded, claimed.request.id + '.json')), false);
  assert.equal((forged.stdout + healthy.stdout).includes('TOP_SECRET_PROMPT'), false);
});

test('superseded reason and verdict come only from the canonical task authority', () => {
  const cases = [
    ['state-changed', (fx) => setFixtureState(fx, 'todo')],
    ['source-revision-changed', (fx) => {
      fs.appendFileSync(path.join(fx.tasks, 'backlog', 'TASK_1_demo.md'), '\nChanged after admission.\n');
      publishFreshIndex(fx);
    }],
  ];
  for (const [expectedReason, mutate] of cases) {
    const fx = fixture();
    putRequest(fx);
    const claimed = claim(fx);
    const lease = acquireLease(fx);
    const reservation = reserve(fx, claimed.handle);
    mutate(fx);
    const written = supersede(fx, claimed.handle, lease);
    assert.deepEqual(written.json, { status: 'superseded-recorded', id: claimed.request.id, reason: expectedReason },
      expectedReason + ': ' + written.stdout);
    const tombstone = JSON.parse(fs.readFileSync(path.join(fx.superseded, claimed.request.id + '.json'), 'utf8'));
    assert.equal(tombstone.reason, expectedReason);
    assert.equal(tombstone.expectedSourceRevision, claimed.request.sourceRevision);
    assert.match(tombstone.snapshotHash, /^sha256:[a-f0-9]{64}$/);
    assert.equal(JSON.stringify(tombstone).includes('TOP_SECRET_PROMPT'), false);
    release(fx, reservation);
    const activeLeaseConsume = run(fx, ['consume', '--handle', claimed.handle, '--kind', 'superseded']);
    assert.deepEqual(activeLeaseConsume.json, { status: 'error', code: 'writer-lease-release-unproven' }, expectedReason);
    assert.equal(fs.existsSync(path.join(privateOperation(fx), 'request.claim')), true, expectedReason);
    releaseLease(fx, lease);
    const consumed = run(fx, ['consume', '--handle', claimed.handle, '--kind', 'superseded']);
    assert.equal(written.status, 0, written.stdout + written.stderr);
    assert.equal(consumed.json?.status, 'consumed', expectedReason + ': ' + consumed.stdout);
  }
});

test('derived INDEX corruption alone cannot supersede an unchanged task', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const lease = acquireLease(fx);
  reserve(fx, claimed.handle);
  fs.writeFileSync(path.join(fx.tasks, 'INDEX.json'), '[]\n', { mode: 0o600 });
  const result = supersede(fx, claimed.handle, lease);
  assert.deepEqual(result.json, { status: 'error', code: 'supersession-not-proven' });
  assert.equal(fs.existsSync(path.join(fx.superseded, claimed.request.id + '.json')), false);
});

test('finalization supersession requires one mechanically proven active same-stem marker', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const reservation = reserve(fx, claimed.handle);
  const unproven = run(fx, ['record-finalization-superseded', '--handle', claimed.handle]);
  assert.deepEqual(unproven.json, { status: 'error', code: 'finalization-supersession-unproven' });
  assert.equal(fs.existsSync(path.join(fx.superseded, claimed.request.id + '.json')), false);

  writeActiveFinalizationMarker(fx);
  const inspectScript = `const t=require(${JSON.stringify(path.join(REPO, 'orchestrator/site/server/task-integrity.js'))});` +
    `const r=t.validateAction('prep','TASK_1_demo','standby-finalization-test');` +
    `process.stdout.write(JSON.stringify({runtimeStatus:r.runtimeStatus,findings:r.findings}));`;
  const inspected = childProcess.spawnSync(process.execPath, ['-e', inspectScript], {
    cwd: fx.root, env: envFor(fx), encoding: 'utf8', maxBuffer: 4 * 1024 * 1024,
  });
  assert.equal(inspected.status, 0, inspected.stdout + inspected.stderr);
  const integrity = JSON.parse(inspected.stdout);
  assert.equal(integrity.runtimeStatus.filter((row) => row.owner === 'finalizations' && row.kind === 'marker' &&
    row.stem === 'TASK_1_demo' && row.state === 'running').length, 1, JSON.stringify(integrity, null, 2));
  const written = run(fx, ['record-finalization-superseded', '--handle', claimed.handle]);
  assert.deepEqual(written.json, { status: 'superseded-recorded', id: claimed.request.id, reason: 'finalization-active' }, written.stdout);
  const tombstone = JSON.parse(fs.readFileSync(path.join(fx.superseded, claimed.request.id + '.json'), 'utf8'));
  assert.equal(tombstone.reason, 'finalization-active');
  assert.match(tombstone.snapshotHash, /^sha256:[a-f0-9]{64}$/);
  assert.equal(JSON.stringify(tombstone).includes('TOP_SECRET_PROMPT'), false);
  release(fx, reservation);
  assert.equal(run(fx, ['consume', '--handle', claimed.handle, '--kind', 'superseded']).json?.status, 'consumed');
});

test('durable supersession receipt is idempotent after crash even when live authority disappears', () => {
  for (const finalization of [false, true]) {
    const fx = fixture();
    putRequest(fx);
    const claimed = claim(fx);
    const reservation = reserve(fx, claimed.handle);
    let lease = null;
    let args;
    if (finalization) {
      writeActiveFinalizationMarker(fx);
      args = ['record-finalization-superseded', '--handle', claimed.handle];
    } else {
      lease = acquireLease(fx);
      fs.appendFileSync(path.join(fx.tasks, 'backlog', 'TASK_1_demo.md'), '\nChanged before supersession.\n');
      publishFreshIndex(fx);
      args = ['record-superseded', '--handle', claimed.handle, ...leaseArgs(lease)];
    }
    const crashed = run(fx, args, { ORCHESTRATOR_STANDBY_TEST_HOOK: 'superseded-after-publication' });
    assert.equal(crashed.status, 86, (finalization ? 'finalization' : 'revision') + ': ' + crashed.stdout + crashed.stderr);
    const tombstone = JSON.parse(fs.readFileSync(path.join(fx.superseded, claimed.request.id + '.json'), 'utf8'));
    if (finalization) fs.unlinkSync(path.join(fx.finalizations, 'TASK_1_demo.json'));
    else releaseLease(fx, lease);
    const replay = run(fx, args);
    assert.deepEqual(replay.json, { status: 'superseded-recorded', id: claimed.request.id, reason: tombstone.reason }, replay.stdout);
    release(fx, reservation);
    assert.equal(run(fx, ['consume', '--handle', claimed.handle, '--kind', 'superseded']).json?.status, 'consumed');
  }
});

test('malformed claimed JSON is retained privately and never leaked', () => {
  const fx = fixture();
  fs.mkdirSync(fx.requests, { recursive: true, mode: 0o700 });
  fs.writeFileSync(path.join(fx.requests, '1000000000000-first.json'), '{"prompt":"LEAK_ME"', { mode: 0o600 });
  const result = claimNext(fx);
  assert.deepEqual(result.json, { status: 'retained', code: 'contract-invalid' });
  assert.equal(result.stdout.includes('LEAK_ME'), false);
  assert.equal(fs.existsSync(path.join(privateOperation(fx), 'invalid.json')), true);
});

test('invalid-claim receipt recovers every publication crash point', () => {
  for (const label of ['invalid-temp-durable', 'invalid-published', 'invalid-durable']) {
    const fx = fixture();
    const queued = putRequest(fx);
    fs.writeFileSync(queued.file, '{"prompt":"INVALID_RECEIPT_SECRET",', { mode: 0o600 });
    fs.chmodSync(queued.file, 0o600);
    const crashed = claimNext(fx, { ORCHESTRATOR_STANDBY_TEST_HOOK: label });
    assert.equal(crashed.status, 2, label + ': ' + crashed.stdout);
    const recovered = claimNext(fx);
    assert.equal(recovered.json?.status, 'retained', label + ': ' + recovered.stdout);
    assert.equal(recovered.stdout.includes('INVALID_RECEIPT_SECRET'), false, label);
    assert.equal(fs.existsSync(queued.file), false, label);
    assert.equal(fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length, 1, label);
  }
});

test('duplicate-key and non-standard JSON are rejected before contract authority', () => {
  const direct = childProcess.spawnSync('python3', [BOUNDARY], { input: '{"version":1,"version":1}', encoding: 'utf8' });
  assert.equal(direct.status, 2);
  assert.equal(JSON.parse(direct.stdout).code, 'input-json-invalid');
  const fx = fixture();
  fs.mkdirSync(fx.requests, { recursive: true, mode: 0o700 });
  const valid = JSON.stringify(requestRecord(fx));
  const duplicate = valid.replace('"prompt":"TOP_SECRET_PROMPT\\nsecond line"', '"prompt":"NEVER_EXECUTE","prompt":"TOP_SECRET_PROMPT\\nsecond line"');
  fs.writeFileSync(path.join(fx.requests, '1000000000000-first.json'), duplicate, { mode: 0o600 });
  const result = claimNext(fx);
  assert.deepEqual(result.json, { status: 'retained', code: 'contract-invalid' });
  assert.equal(result.stdout.includes('TOP_SECRET_PROMPT'), false);
});

test('boundary versions are exact JSON integers, never booleans or floats', () => {
  const fx = fixture();
  fs.mkdirSync(fx.requests, { recursive: true, mode: 0o700 });
  fs.mkdirSync(fx.runs, { recursive: true, mode: 0o700 });
  const base = {
    action: 'claim', projectRoot: fx.root,
    requestsRelative: 'orchestrator/.cache/tasks/requests', runsRelative: 'orchestrator/.cache/tasks/runs',
    rootProof: exactProof(fx.root), requestsProof: exactProof(fx.requests), runsProof: exactProof(fx.runs),
    candidate: null, nonce: null,
  };
  for (const version of [true, 1.0]) {
    const wireVersion = version === 1 ? '1.0' : JSON.stringify(version);
    const input = JSON.stringify({ version: '__VERSION__', ...base }).replace('"__VERSION__"', wireVersion);
    const result = childProcess.spawnSync('python3', [BOUNDARY], {
      cwd: fx.root, env: envFor(fx), input, encoding: 'utf8',
    });
    assert.equal(result.status, 2, wireVersion);
    assert.equal(JSON.parse(result.stdout).code, 'input-contract-invalid', wireVersion);
  }

  putRequest(fx);
  assert.equal(claimNext(fx, { ORCHESTRATOR_STANDBY_TEST_HOOK: 'intent-durable' }).status, 2);
  const intent = path.join(privateOperation(fx), 'intent.json');
  const corrupted = fs.readFileSync(intent, 'utf8').replace('"version":1', '"version":true');
  fs.writeFileSync(intent, corrupted, { mode: 0o600 });
  fs.chmodSync(intent, 0o600);
  const receipt = claimNext(fx);
  assert.equal(receipt.status, 2);
  assert.equal(receipt.json?.code, 'receipt-contract-invalid');
});

test('oversized canonical request is claimed as retained evidence', () => {
  const fx = fixture();
  fs.mkdirSync(fx.requests, { recursive: true, mode: 0o700 });
  fs.writeFileSync(path.join(fx.requests, '1000000000000-first.json'), Buffer.alloc(256 * 1024 + 1, 65), { mode: 0o600 });
  const result = claimNext(fx);
  assert.equal(result.json?.status, 'retained');
  assert.equal(fs.existsSync(path.join(privateOperation(fx), 'request.claim')), true);
});

test('canonical symlink is moved without following and retained', { skip: process.platform === 'win32' }, () => {
  const fx = fixture();
  fs.mkdirSync(fx.requests, { recursive: true, mode: 0o700 });
  const secret = path.join(fx.root, 'secret');
  fs.writeFileSync(secret, 'DO_NOT_READ', { mode: 0o600 });
  fs.symlinkSync(secret, path.join(fx.requests, '1000000000000-first.json'));
  const result = claimNext(fx);
  assert.equal(result.json?.status, 'retained');
  assert.equal(result.stdout.includes('DO_NOT_READ'), false);
  assert.equal(fs.lstatSync(path.join(privateOperation(fx), 'request.claim')).isSymbolicLink(), true);
});

test('hardlinked canonical request loses execution authority and is retained', () => {
  const fx = fixture();
  const { file } = putRequest(fx);
  fs.linkSync(file, path.join(fx.root, 'second-link'));
  const result = claimNext(fx);
  assert.equal(result.json?.status, 'retained');
  assert.equal(fs.existsSync(path.join(fx.root, 'second-link')), true);
});

test('special canonical entry is moved and retained without read', { skip: process.platform === 'win32' }, () => {
  const fx = fixture();
  fs.mkdirSync(fx.requests, { recursive: true, mode: 0o700 });
  const fifo = path.join(fx.requests, '1000000000000-first.json');
  assert.equal(childProcess.spawnSync('mkfifo', [fifo]).status, 0);
  const result = claimNext(fx);
  assert.equal(result.json?.status, 'retained');
  assert.equal(fs.lstatSync(path.join(privateOperation(fx), 'request.claim')).isFIFO(), true);
});

test('same-bytes claim replacement is rejected by inode lineage', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const claimFile = path.join(privateOperation(fx), 'request.claim');
  const bytes = fs.readFileSync(claimFile);
  fs.unlinkSync(claimFile);
  fs.writeFileSync(claimFile, bytes, { mode: 0o600 });
  const result = run(fx, ['status', '--handle', claimed.handle]);
  assert.equal(result.status, 2);
  assert.equal(result.stdout.includes('TOP_SECRET_PROMPT'), false);
  assert.match(result.json?.code || '', /claim/);
});

test('claim disappearance is fail-closed and never recreated by age', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  fs.unlinkSync(path.join(privateOperation(fx), 'request.claim'));
  const result = run(fx, ['status', '--handle', claimed.handle]);
  assert.equal(result.status, 2);
  assert.equal(fs.existsSync(path.join(fx.requests, claimed.request.id + '.json')), false);
});

test('ancestor swap-away and swap-back cannot redirect handle authority', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const away = fx.runs + '-away';
  fs.renameSync(fx.runs, away);
  fs.mkdirSync(fx.runs, { mode: 0o700 });
  const during = run(fx, ['status', '--handle', claimed.handle]);
  assert.equal(during.status, 2);
  assert.equal(fs.readdirSync(fx.runs).length, 0);
  fs.rmdirSync(fx.runs);
  fs.renameSync(away, fx.runs);
  assert.equal(run(fx, ['status', '--handle', claimed.handle]).json?.status, 'claimed');
});

test('foreign standby evidence is retained and blocks new claims', () => {
  const fx = fixture();
  putRequest(fx);
  fs.mkdirSync(path.join(fx.runs, '.standby-1000000000000-first-foreign'), { recursive: true, mode: 0o700 });
  const result = claimNext(fx);
  assert.equal(result.json?.code, 'foreign-standby-evidence');
  assert.equal(fs.existsSync(path.join(fx.requests, '1000000000000-first.json')), true);
});

test('foreign entries inside an owned operation are retained fail-closed', () => {
  const fx = fixture();
  putRequest(fx);
  const claimed = claim(fx);
  const foreign = path.join(privateOperation(fx), 'foreign.evidence');
  fs.writeFileSync(foreign, 'FOREIGN', { mode: 0o600 });
  const result = run(fx, ['status', '--handle', claimed.handle]);
  assert.equal(result.json?.code, 'foreign-op-evidence');
  assert.equal(fs.readFileSync(foreign, 'utf8'), 'FOREIGN');
});

test('multiple drainers yield one offer and one exact private owner', () => {
  const fx = fixture();
  putRequest(fx);
  const pass = beginPass(fx);
  const spawn = () => childProcess.spawn(process.execPath, [CLI, 'claim-next', '--pass-token', pass.passToken], {
    cwd: fx.root, env: envFor(fx), stdio: ['ignore', 'pipe', 'pipe'],
  });
  const children = [spawn(), spawn()];
  const outputs = children.map((child) => new Promise((resolve) => {
    let stdout = '';
    child.stdout.on('data', (chunk) => { stdout += chunk; });
    child.on('close', (status) => resolve({ status, json: JSON.parse(stdout) }));
  }));
  return Promise.all(outputs).then((rows) => {
    assert.equal(rows.filter((row) => row.json.status === 'claimed').length, 1);
    assert.equal(rows.filter((row) => row.json.status === 'error' &&
      ['pass-token-invalid', 'pass-token-consume-failed'].includes(row.json.code)).length, 1);
    assert.equal(fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length, 1);
  });
});

test('claim WAL recovers every pre-offer crash boundary without requeue', () => {
  const labels = ['op-created', 'intent-temp-durable', 'intent-published', 'intent-durable', 'claim-renamed', 'claim-dirs-durable', 'claimed-temp-durable', 'claimed-published', 'claimed-durable'];
  for (const label of labels) {
    const fx = fixture();
    putRequest(fx);
    const crashed = claimNext(fx, { ORCHESTRATOR_STANDBY_TEST_HOOK: label });
    assert.equal(crashed.status, 2, label + ': ' + crashed.stdout);
    let recovered = claimNext(fx);
    if (recovered.json?.status === 'recovered') recovered = claimNext(fx);
    assert.equal(recovered.json?.status, 'claimed', label + ': ' + recovered.stdout);
    assert.equal(fs.existsSync(path.join(fx.requests, '1000000000000-first.json')), false, label);
  }
});

test('cancel winning after intent cleans only the empty loser WAL and retries', () => {
  const fx = fixture();
  const queued = putRequest(fx);
  const crashed = claimNext(fx, { ORCHESTRATOR_STANDBY_TEST_HOOK: 'intent-durable' });
  assert.equal(crashed.status, 2);
  const cancelEvidence = path.join(fx.requests, '.1000000000000-first-1-' + 'a'.repeat(16) + '.cancel');
  fs.renameSync(queued.file, cancelEvidence);
  const recovered = claimNext(fx);
  assert.equal(['retry', 'recovered'].includes(recovered.json?.status), true, recovered.stdout);
  assert.equal(fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length, 0);
  assert.equal(fs.existsSync(cancelEvidence), true);
});

test('offer receipt crashes are conservative and never re-disclose projection ownership', () => {
  for (const label of ['offered-temp-durable', 'offered-published', 'offered-durable']) {
    const fx = fixture();
    putRequest(fx);
    const crashed = claimNext(fx, { ORCHESTRATOR_STANDBY_TEST_HOOK: label });
    assert.equal(crashed.status, 2, label);
    const retry = claimNext(fx);
    assert.deepEqual(retry.json, { status: 'blocked', code: 'active-offer' }, label);
    assert.equal(retry.stdout.includes('TOP_SECRET_PROMPT'), false, label);
  }
});

test('execution and disclosure receipts recover every publication crash point', () => {
  for (const label of ['execution-temp-durable', 'execution-published', 'execution-durable']) {
    const fx = fixture();
    putRequest(fx);
    const claimed = claim(fx);
    const lease = acquireLease(fx);
    reserve(fx, claimed.handle);
    assert.equal(prepare(fx, claimed.handle, lease, { ORCHESTRATOR_STANDBY_TEST_HOOK: label }).status, 2, label);
    assert.equal(prepare(fx, claimed.handle, lease).json?.status, 'execution-prepared', label);
  }
  for (const label of ['disclosed-temp-durable', 'disclosed-published', 'disclosed-durable']) {
    const fx = fixture();
    putRequest(fx);
    const claimed = claim(fx);
    const lease = acquireLease(fx);
    const reservation = reserve(fx, claimed.handle);
    prepare(fx, claimed.handle, lease);
    release(fx, reservation);
    assert.equal(disclose(fx, claimed.handle, lease, { ORCHESTRATOR_STANDBY_TEST_HOOK: label }).status, 2, label);
    const retry = disclose(fx, claimed.handle, lease);
    assert.equal(retry.json?.code, 'prompt-already-disclosed', label);
    assert.equal(retry.stdout.includes('TOP_SECRET_PROMPT'), false, label);
  }
});

test('restore WAL recovers every receipt and rename crash boundary', () => {
  const labels = [
    'restore-temp-durable', 'restore-published', 'restore-durable', 'restore-renamed', 'restore-dirs-durable',
    'restored-temp-durable', 'restored-published', 'restored-durable',
    'cleanup-claimed', 'cleanup-offered', 'cleanup-restore', 'cleanup-intent', 'cleanup-restored', 'cleanup-durable',
  ];
  for (const label of labels) {
    const fx = fixture();
    putRequest(fx);
    const claimed = claim(fx);
    reserve(fx, claimed.handle);
    const crashed = run(fx, ['restore', '--handle', claimed.handle], { ORCHESTRATOR_STANDBY_TEST_HOOK: label });
    assert.equal(crashed.status, 2, label + ': ' + crashed.stdout);
    const recovery = claimNext(fx);
    const publicRequest = fs.existsSync(path.join(fx.requests, claimed.request.id + '.json'));
    const privateOperations = fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length;
    if (label === 'cleanup-durable') {
      assert.equal(recovery.json?.status, 'claimed', label + ': ' + recovery.stdout);
      assert.equal(publicRequest, false, label);
      assert.equal(privateOperations, 1, label);
    } else {
      assert.equal(publicRequest, true, label + ': ' + recovery.stdout);
      assert.equal(privateOperations, 0, label + ': ' + recovery.stdout);
    }
  }
});

test('consume receipt precedes detach and crash recovery never restores executed work', () => {
  const labels = [
    'consume-temp-durable', 'consume-published', 'consume-durable',
    'consume-detached', 'consume-detached-durable',
    'detached-temp-durable', 'detached-published', 'detached-durable',
    'consume-unlinked', 'consume-unlink-durable',
    'consumed-temp-durable', 'consumed-published', 'consumed-durable',
    ...['claimed', 'offered', 'invalid', 'execution', 'disclosed', 'restore', 'restored', 'consume', 'detached', 'intent', 'consumed'].map((name) => 'cleanup-' + name),
    'cleanup-durable',
  ];
  for (const label of labels) {
    const fx = fixture();
    putRequest(fx);
    const claimed = claim(fx);
    prepareAndDisclose(fx, claimed.handle);
    const crashed = run(fx, ['consume', '--handle', claimed.handle, '--kind', 'executed'], { ORCHESTRATOR_STANDBY_TEST_HOOK: label });
    assert.equal(crashed.status, 2, label + ': ' + crashed.stdout);
    const recovery = claimNext(fx);
    assert.equal(fs.existsSync(path.join(fx.requests, claimed.request.id + '.json')), false, label);
    assert.equal(fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length, 0, label + ': ' + recovery.stdout);
  }
});

test('strict CLI rejects unknown, duplicate, malformed, and oversized arguments without writes', () => {
  const fx = fixture({ createLocks: false });
  const cases = [
    [], ['claim-next'], ['claim-next', '--pass-token', 'f'.repeat(63)], ['claim-next', '--x', '1'], ['status', '--handle', 'x'],
    ['status', '--handle', 'a'.repeat(64), '--handle', 'a'.repeat(64)],
    ['prepare-execution', '--handle', 'a'.repeat(64)],
    ['read-prompt', '--handle', 'a'.repeat(64), '--lease-id', 'x', '--lease-token', 'f'.repeat(64), '--session-id', 'x'],
    ['consume', '--handle', 'a'.repeat(64), '--kind', 'other'],
    ['claim-next', 'x'.repeat(9000)],
  ];
  for (const args of cases) assert.equal(run(fx, args).status, 64, args.join(' '));
  assert.equal(fs.existsSync(fx.cache), false);
});

test('syntactically valid unauthenticated claim cannot create queue authority directories', () => {
  const fx = fixture({ createLocks: false });
  const result = run(fx, ['claim-next', '--pass-token', 'f'.repeat(64)]);
  assert.deepEqual(result.json, { status: 'error', code: 'queue-authority-unsafe' });
  assert.equal(fs.existsSync(fx.cache), false);
});

test('boundary keeps authority proofs as lossless decimals beyond 2^53', () => {
  const fx = fixture();
  fs.mkdirSync(fx.requests, { recursive: true, mode: 0o700 });
  fs.mkdirSync(fx.runs, { recursive: true, mode: 0o700 });
  const rootProof = exactProof(fx.root);
  rootProof.dev = '9007199254740993';
  const input = {
    version: 1, action: 'claim', projectRoot: fx.root,
    requestsRelative: 'orchestrator/.cache/tasks/requests', runsRelative: 'orchestrator/.cache/tasks/runs',
    rootProof, requestsProof: exactProof(fx.requests), runsProof: exactProof(fx.runs), candidate: null, nonce: null,
  };
  const result = childProcess.spawnSync('python3', [BOUNDARY], { cwd: fx.root, env: envFor(fx), input: JSON.stringify(input), encoding: 'utf8' });
  assert.equal(result.status, 2);
  assert.equal(JSON.parse(result.stdout).code, 'root-proof-changed');
});

test('noncanonical queue names are inert and never deleted', () => {
  const fx = fixture();
  fs.mkdirSync(fx.requests, { recursive: true, mode: 0o700 });
  const foreign = path.join(fx.requests, 'not-a-request.json');
  fs.writeFileSync(foreign, 'FOREIGN', { mode: 0o600 });
  assert.deepEqual(claimNext(fx).json, { status: 'empty' });
  assert.equal(fs.readFileSync(foreign, 'utf8'), 'FOREIGN');
});

test('directory entry ceilings fail closed before sorting or claiming', () => {
  const fx = fixture();
  fs.mkdirSync(fx.requests, { recursive: true, mode: 0o700 });
  for (let index = 0; index <= 10_000; index++) fs.writeFileSync(path.join(fx.requests, 'foreign-' + String(index).padStart(5, '0')), 'x', { mode: 0o600 });
  const result = claimNext(fx);
  assert.equal(result.json?.code, 'request-scan-incomplete');
  assert.equal(fs.readdirSync(fx.requests).length, 10_001);
  assert.equal(fs.readdirSync(fx.runs).filter((name) => name.startsWith('.standby-')).length, 0);
});
