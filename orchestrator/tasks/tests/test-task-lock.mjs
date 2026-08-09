#!/usr/bin/env node

import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { spawn, spawnSync } from 'node:child_process';
import {
  existsSync, linkSync, lstatSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, symlinkSync,
  unlinkSync, watch, writeFileSync,
} from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const TASKS_DIR = join(HERE, '..');
const CLI = join(TASKS_DIR, 'task-lock.mjs');
const FS_BOUNDARY = join(TASKS_DIR, 'anchored-task-fs.py');
const CANONICAL_PROJECT_ROOT = join(TASKS_DIR, '..', '..');
const OUTCOME_SHAPE = join(TASKS_DIR, '..', 'contracts', 'outcome-shape.json');
const require = createRequire(import.meta.url);
const core = require('../task-state-core.cjs');
const writerLeases = require('../writer-leases.cjs');
const roots = [];
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

function numberOf(stem) { return Number(/^TASK_([1-9][0-9]*)_/.exec(stem)[1]); }
function sourceBlock(stem) {
  return [
    '## Source', '', '- Kind: manual', '- Type: manual', '- Ref: ' + stem,
    '- Fingerprint: sha256:' + createHash('sha256').update('source\0' + stem).digest('hex')
  ].join('\n');
}
function backlogDoc(stem) { return `# TASK ${numberOf(stem)} — Lock fixture\n\n${sourceBlock(stem)}\n\nExercise lock ownership.\n`; }
function todoDoc(stem) {
  return [
    `# TASK ${numberOf(stem)} — Runnable lock fixture`, '',
    sourceBlock(stem), '',
    '## Goal', '', 'Exercise orchestrator lock ownership.', '',
    '## Inputs', '', '- Existing task-state contract.', '',
    '## Acceptance', '', '### Automated', '', '- Run `node orchestrator/tasks/tests/test-task-lock.mjs`.', '',
    '### Manual', '', '- Inspect the lock receipt.', '',
    '## Out of scope', '', '- Product code changes.', '',
  ].join('\n');
}

function makeProject(state = 'backlog', number = 31) {
  const root = mkdtempSync(join(tmpdir(), 'task-lock-'));
  roots.push(root);
  const tasks = join(root, 'orchestrator', 'tasks');
  const locks = join(root, 'orchestrator', '.cache', 'tasks', 'locks');
  const writers = join(root, 'orchestrator', '.cache', 'tasks', 'finalizations', '.writers');
  for (const column of core.COLUMNS) mkdirSync(join(tasks, column), { recursive: true });
  mkdirSync(locks, { recursive: true });
  mkdirSync(writers, { recursive: true });
  const stem = `TASK_${number}_lock_fixture`;
  if (state === 'backlog') writeFileSync(join(tasks, 'backlog', `${stem}.md`), backlogDoc(stem));
  else if (state === 'todo') writeFileSync(join(tasks, 'todo', `${stem}.md`), todoDoc(stem));
  else throw new Error(`unsupported fixture state ${state}`);
  refreshIndex({ root, tasks });
  return { root, tasks, locks, writers, stem, lock: join(locks, `${stem}.json`) };
}

function refreshIndex(project) {
  const result = core.validateTaskState({
    repoRoot: project.root,
    tasksDir: project.tasks,
    outcomeShapePath: OUTCOME_SHAPE,
    checkIndex: false,
    includeRuntime: false,
  });
  assert.equal(result.ok, true, JSON.stringify(result.findings));
  const index = core.deriveIndex(result._model, '2026-07-13T12:00:00.000Z');
  writeFileSync(join(project.tasks, 'INDEX.json'), `${JSON.stringify(index, null, 2)}\n`);
}

function envFor(project) {
  return {
    ...process.env,
    ORCHESTRATOR_PROJECT_ROOT: project.root,
    ORCHESTRATOR_TASKS_DIR: project.tasks,
    ORCHESTRATOR_LOCKS_DIR: project.locks,
    ORCHESTRATOR_WRITER_LEASES_DIR: project.writers,
    ORCHESTRATOR_WRITER_AUTHORITY_ROOT: project.root,
    ORCHESTRATOR_OUTCOME_SHAPE_PATH: OUTCOME_SHAPE,
    TASK_FS_TEST_ROOT: project.root,
  };
}

function run(project, args, env = {}) {
  return spawnSync(process.execPath, [CLI, ...args], {
    cwd: project.root,
    env: { ...envFor(project), ...env },
    encoding: 'utf8',
    timeout: 15000,
  });
}

function runAsync(project, args, env = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [CLI, ...args], {
      cwd: project.root,
      env: { ...envFor(project), ...env },
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    let stdout = '', stderr = '';
    child.stdout.on('data', (chunk) => { stdout += chunk; });
    child.stderr.on('data', (chunk) => { stderr += chunk; });
    child.on('error', reject);
    child.on('close', (status, signal) => resolve({ status, signal, stdout, stderr }));
  });
}

function waitUntil(predicate, message, timeoutMs = 15000) {
  return new Promise((resolve, reject) => {
    const started = Date.now();
    const poll = setInterval(() => {
      let ready = false;
      try { ready = predicate(); }
      catch (error) { clearInterval(poll); reject(error); return; }
      if (ready) { clearInterval(poll); resolve(); return; }
      if (Date.now() - started >= timeoutMs) {
        clearInterval(poll);
        reject(new Error(message));
      }
    }, 5);
  });
}

function runBoundary(project, request, env = {}) {
  const result = spawnSync('python3', [FS_BOUNDARY], {
    input: JSON.stringify({ version: 1, canonicalRoot: CANONICAL_PROJECT_ROOT, fixture: true, ...request }),
    cwd: project.root,
    env: { ...envFor(project), ...env },
    encoding: 'utf8',
    maxBuffer: 4 * 1024 * 1024,
  });
  assert.equal(result.status, 0, result.stderr || result.stdout);
  return JSON.parse(result.stdout);
}

function runWithPostPublishMutation(project, args, mutate) {
  return new Promise((resolve, reject) => {
    const watcher = watch(project.locks);
    const child = spawn(process.execPath, [CLI, ...args], {
      cwd: project.root,
      env: envFor(project),
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    let stdout = '', stderr = '', intercepted = false;
    const timer = setTimeout(() => {
      try { watcher.close(); } catch (_) {}
      clearInterval(poll);
      try { child.kill('SIGKILL'); } catch (_) {}
      reject(new Error('timed out waiting for post-publication race fixture'));
    }, 15000);
    child.stdout.on('data', (chunk) => { stdout += chunk; });
    child.stderr.on('data', (chunk) => { stderr += chunk; });
    child.on('error', reject);
    function intercept() {
      if (intercepted || !existsSync(project.lock)) return;
      intercepted = true;
      clearInterval(poll);
      try {
        process.kill(child.pid, 'SIGSTOP');
        mutate();
        process.kill(child.pid, 'SIGCONT');
      } catch (error) {
        try { process.kill(child.pid, 'SIGCONT'); } catch (_) {}
        reject(error);
      } finally {
        watcher.close();
      }
    }
    const poll = setInterval(intercept, 1);
    watcher.on('change', (_event, filename) => {
      if (filename === `${project.stem}.json`) intercept();
    });
    child.on('close', (status, signal) => {
      clearTimeout(timer);
      clearInterval(poll);
      try { watcher.close(); } catch (_) {}
      if (!intercepted) reject(new Error(`lock publication was not intercepted (status=${status}, stderr=${stderr})`));
      else resolve({ status, signal, stdout, stderr });
    });
  });
}

function parseSuccess(result) {
  assert.equal(result.status, 0, result.stderr || result.stdout);
  return JSON.parse(result.stdout);
}

function parseErrorEnvelope(stderr) {
  const text = String(stderr || '').trim();
  const start = text.lastIndexOf('\n{');
  return JSON.parse(start >= 0 ? text.slice(start + 1) : text);
}

function observations(stderr) {
  return String(stderr || '').split(/\r?\n/).filter((line) => line.startsWith('[task-state] '))
    .map((line) => JSON.parse(line.slice('[task-state] '.length)));
}

function assertObservationShape(event, caller = 'task-prep') {
  assert.equal(event.version, 1);
  assert.equal(event.event, 'task-state-validation');
  assert.equal(event.caller, caller);
  assert.ok(['valid', 'invalid'].includes(event.result));
  assert.equal(typeof event.durationMs, 'number');
  assert.equal(Array.isArray(event.findings), true);
  for (const finding of event.findings) {
    assert.deepEqual(Object.keys(finding).sort(), ['code', 'severity']);
  }
  assert.doesNotMatch(JSON.stringify(event), /Exercise lock ownership|private|answer|prompt/i);
}

function parseFailure(result, status, code) {
  assert.equal(result.status, status, result.stderr || result.stdout);
  const value = parseErrorEnvelope(result.stderr);
  assert.equal(value.ok, false);
  assert.equal(value.code, code);
  return value;
}

function acquireArgs(project, stage = 'task-prep', suffix = 'a') {
  return [
    'acquire', '--stem', project.stem, '--stage', stage,
    '--run-id', `run-lock-fixture-${suffix}`,
    '--session-id', `ws-lock-fixture-session-${suffix.padEnd(16, 'x')}`,
    '--owner-kind', 'agent', '--owner-id', `agent:${suffix}`, '--owner-pid', String(process.pid),
  ];
}

function siteAcquireArgs(project, ownerPid, stage = 'task-prep', suffix = 'site') {
  return [
    'acquire', '--stem', project.stem, '--stage', stage,
    '--run-id', `run-lock-fixture-${suffix}`,
    '--session-id', `ws-lock-fixture-session-${suffix.padEnd(16, 'x')}`,
    '--owner-kind', 'site', '--owner-id', `site:${suffix}`, '--owner-pid', String(ownerPid),
  ];
}

function acquireRecoveryLease(project) {
  return writerLeases.acquire(project.writers, {
    rootDir: project.root,
    kind: 'lock-writer',
    stem: project.stem,
    key: `task-lock-recovery:${project.stem}`,
    ownerPid: process.pid,
    pendingChild: false,
    sessionId: writerLeases.createSessionId(),
  });
}

function recoveryEnv(project, lease) {
  return {
    ORCHESTRATOR_WRITER_SESSION_ID: lease.record.sessionId,
    ORCHESTRATOR_WRITER_STEM: project.stem,
    ORCHESTRATOR_WRITER_LEASE_ID: lease.leaseId,
    ORCHESTRATOR_WRITER_LEASE_TOKEN: lease.token,
  };
}

function acquireStandbyLease(project, key = 'standby:prep') {
  return writerLeases.acquire(project.writers, {
    rootDir: project.root,
    kind: 'task-session',
    stem: project.stem,
    key,
    ownerPid: process.pid,
    ttlMs: 60 * 60 * 1000,
    sessionId: writerLeases.createSessionId(),
  });
}

function standbyAcquireArgs(project, lease, stage = 'task-prep', suffix = 'standby') {
  return [
    'acquire', '--stem', project.stem, '--stage', stage,
    '--run-id', `run-lock-fixture-${suffix}`,
    '--session-id', lease.record.sessionId,
    '--writer-lease-id', lease.leaseId,
    '--writer-lease-token', lease.token,
    '--owner-kind', 'standby', '--owner-id', `standby:${lease.leaseId}`,
    '--owner-pid', String(process.pid),
  ];
}

function releaseArgs(project, acquired, command = 'release') {
  const state = acquired.state || acquired.observedState;
  return [
    command, '--stem', project.stem, '--run-id', acquired.runId,
    '--session-id', acquired.sessionId, '--expected-hash', acquired.lockHash,
    '--expected-state', state, '--source-revision', acquired.sourceRevision,
  ];
}

function currentStateReceipt(project) {
  const verdict = core.validateTaskState({
    repoRoot: project.root, tasksDir: project.tasks, stem: project.stem,
    checkIndex: true, includeRuntime: false, outcomeShapePath: OUTCOME_SHAPE,
  });
  assert.equal(verdict.ok, true, JSON.stringify(verdict.findings));
  return { observedState: verdict.observedState, sourceRevision: verdict.sourceRevision };
}

function runWithReleasePauseMutation(project, args, pauseName, ready, mutate) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [CLI, ...args], {
      cwd: project.root,
      env: { ...envFor(project), [pauseName]: '1200' },
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    let stdout = '', stderr = '', intercepted = false;
    child.stdout.on('data', (chunk) => { stdout += chunk; });
    child.stderr.on('data', (chunk) => { stderr += chunk; });
    child.on('error', reject);
    const poll = setInterval(() => {
      if (intercepted || !ready()) return;
      intercepted = true;
      try { mutate(); }
      catch (error) {
        clearInterval(poll);
        try { child.kill('SIGKILL'); } catch (_) {}
        reject(error);
      }
    }, 5);
    const timer = setTimeout(() => {
      clearInterval(poll);
      try { child.kill('SIGKILL'); } catch (_) {}
      reject(new Error(`timed out waiting for ${pauseName}`));
    }, 15000);
    child.on('close', (status, signal) => {
      clearInterval(poll);
      clearTimeout(timer);
      if (!intercepted) reject(new Error(`${pauseName} was not intercepted (status=${status}, stderr=${stderr})`));
      else resolve({ status, signal, stdout, stderr });
    });
  });
}

function retainedReleasePaths(project) {
  const prefix = `.${project.stem}.json.release-`;
  return readdirSync(project.locks)
    .filter((name) => name.startsWith(prefix))
    .sort()
    .map((name) => join(project.locks, name));
}

function completedReleasePaths(project) {
  const prefix = `.${project.stem}.json.released-`;
  return readdirSync(project.locks)
    .filter((name) => name.startsWith(prefix))
    .sort()
    .map((name) => join(project.locks, name));
}

await check('acquire publishes the exact canonical v1 record and runtime validator accepts it', () => {
  const project = makeProject('backlog', 31);
  const execution = run(project, acquireArgs(project));
  const receipt = parseSuccess(execution);
  assert.equal(receipt.created, true);
  assert.equal(receipt.observedState, 'backlog');
  assert.match(receipt.sourceRevision, /^sha256:[a-f0-9]{64}$/);
  assert.match(receipt.lockHash, /^sha256:[a-f0-9]{64}$/);
  const mode = lstatSync(project.lock).mode & 0o777;
  assert.equal(mode, 0o600);
  const record = JSON.parse(readFileSync(project.lock, 'utf8'));
  assert.deepEqual(Object.keys(record).sort(), ['owner', 'runId', 'sessionId', 'stage', 'startedAt', 'stem', 'version']);
  assert.deepEqual(Object.keys(record.owner).sort(), ['hostname', 'id', 'kind', 'pid', 'processStartId', 'startedAt']);
  assert.match(record.owner.processStartId, /^psid-v1:(?:linux|darwin|win32):[a-f0-9]{64}$/);
  assert.equal(record.owner.startedAt, record.startedAt);
  const verdict = core.validateTaskState({
    repoRoot: project.root, tasksDir: project.tasks, locksDir: project.locks,
    checkIndex: true, includeRuntime: true, stem: project.stem, outcomeShapePath: OUTCOME_SHAPE,
  });
  assert.equal(verdict.ok, true, JSON.stringify(verdict.findings));
  const events = observations(execution.stderr);
  assert.equal(events.length, 4, 'pre/post publication state + action validations must each be observed');
  for (const event of events) assertObservationShape(event);
  assert.deepEqual(events.map((event) => event.action), ['lock-acquire', 'prep', 'lock-acquire', 'prep']);
});

await check('active standby writer admits only its exact private task-lock capability', () => {
  const project = makeProject('backlog', 61);
  const lease = acquireStandbyLease(project);
  parseFailure(run(project, acquireArgs(project, 'task-prep', 'foreign-during-standby')), 4, 'STANDBY_WRITER_CONFLICT');
  assert.throws(() => lstatSync(project.lock), { code: 'ENOENT' });

  const wrongToken = standbyAcquireArgs(project, lease, 'task-prep', 'wrong-token');
  wrongToken[wrongToken.indexOf('--writer-lease-token') + 1] = 'f'.repeat(48);
  parseFailure(run(project, wrongToken), 4, 'STANDBY_WRITER_AUTHORITY_LOST');
  assert.throws(() => lstatSync(project.lock), { code: 'ENOENT' });

  const conflict = writerLeases.acquire(project.writers, {
    rootDir: project.root, kind: 'task-session', stem: project.stem, key: 'direct:foreign',
    ownerPid: process.pid, ttlMs: 60 * 60 * 1000, sessionId: writerLeases.createSessionId(),
  });
  parseFailure(run(project, standbyAcquireArgs(project, lease, 'task-prep', 'conflicting-writer')), 4, 'STANDBY_WRITER_CONFLICT');
  assert.throws(() => lstatSync(project.lock), { code: 'ENOENT' });
  assert.equal(writerLeases.release(conflict), true);

  const execution = run(project, standbyAcquireArgs(project, lease));
  const receipt = parseSuccess(execution);
  assert.equal(receipt.sessionId, lease.record.sessionId);
  assert.equal(execution.stdout.includes(lease.token), false);
  assert.equal(readFileSync(project.lock, 'utf8').includes(lease.token), false);
  parseSuccess(run(project, releaseArgs(project, receipt)));
  assert.equal(writerLeases.release(lease), true);
});

await check('standby task-lock capability is bound to the action-compatible lease key', () => {
  const project = makeProject('backlog', 62);
  const lease = acquireStandbyLease(project, 'standby:run');
  parseFailure(run(project, standbyAcquireArgs(project, lease, 'task-prep', 'wrong-action')), 4, 'STANDBY_WRITER_AUTHORITY_LOST');
  assert.throws(() => lstatSync(project.lock), { code: 'ENOENT' });
  assert.equal(writerLeases.release(lease), true);
});

await check('standby lease appearing after lock publication rolls back the exact foreign generation', async () => {
  const project = makeProject('backlog', 63);
  const pending = runAsync(project, acquireArgs(project, 'task-prep', 'standby-race'), {
    TASK_LOCK_TEST_PAUSE_BEFORE_WRITER_RECHECK_MS: '1500',
  });
  await waitUntil(() => existsSync(project.lock), 'task lock was not published before standby race');
  const lease = acquireStandbyLease(project);
  const result = await pending;
  parseFailure(result, 4, 'STANDBY_WRITER_CONFLICT');
  assert.throws(() => lstatSync(project.lock), { code: 'ENOENT' });
  assert.equal(writerLeases.release(lease), true);
});

await check('conflicting writer appearing after exact standby lock publication rolls it back', async () => {
  const project = makeProject('backlog', 64);
  const lease = acquireStandbyLease(project);
  const pending = runAsync(project, standbyAcquireArgs(project, lease, 'task-prep', 'standby-conflict-race'), {
    TASK_LOCK_TEST_PAUSE_BEFORE_WRITER_RECHECK_MS: '1500',
  });
  await waitUntil(() => existsSync(project.lock), 'standby task lock was not published before writer conflict race');
  const conflict = writerLeases.acquire(project.writers, {
    rootDir: project.root, kind: 'task-session', stem: project.stem, key: 'direct:late-foreign',
    ownerPid: process.pid, ttlMs: 60 * 60 * 1000, sessionId: writerLeases.createSessionId(),
  });
  const result = await pending;
  parseFailure(result, 4, 'STANDBY_WRITER_CONFLICT');
  assert.throws(() => lstatSync(project.lock), { code: 'ENOENT' });
  assert.equal(writerLeases.release(conflict), true);
  assert.equal(writerLeases.release(lease), true);
});

await check('orchestrator stage is admitted only for a canonical todo task', () => {
  const project = makeProject('todo', 32);
  const receipt = parseSuccess(run(project, acquireArgs(project, 'orchestrator')));
  assert.equal(receipt.stage, 'orchestrator');
  assert.equal(receipt.observedState, 'todo');
  const released = run(project, releaseArgs(project, receipt));
  assert.equal(parseSuccess(released).state, 'todo');
  const events = observations(released.stderr);
  assert.equal(events.length, 3);
  for (const event of events) assertObservationShape(event, 'runner');
});

await check('orchestrator lock acquisition uses run admission and blocks unresolved dependencies', () => {
  const project = makeProject('todo', 44);
  const current = readFileSync(join(project.tasks, 'todo', `${project.stem}.md`), 'utf8');
  writeFileSync(join(project.tasks, 'todo', `${project.stem}.md`), current.replace(
    '\n## Acceptance\n',
    '\n## Depends on (optional)\n\n- TASK_45_missing_dependency\n\n## Acceptance\n',
  ));
  refreshIndex(project);
  const failure = parseFailure(run(project, acquireArgs(project, 'orchestrator')), 1, 'TASK_ACTION_NOT_ADMITTED');
  assert.equal(failure.details.action, 'run');
  assert.equal(failure.details.findings.some((finding) => finding.code === 'RUN_DEPENDENCY_UNSATISFIED'), true);
  assert.throws(() => lstatSync(project.lock), { code: 'ENOENT' });
});

await check('stage/state mismatch fails before publishing a lock', () => {
  const project = makeProject('backlog', 33);
  parseFailure(run(project, acquireArgs(project, 'orchestrator')), 1, 'LOCK_STAGE_STATE_MISMATCH');
  assert.throws(() => lstatSync(project.lock), { code: 'ENOENT' });
});

await check('stale INDEX fails lock admission without changing runtime state', () => {
  const project = makeProject('backlog', 34);
  writeFileSync(join(project.tasks, 'INDEX.json'), JSON.stringify({ version: 2, generatedAt: '2026-07-13T12:00:00.000Z', backlog: [], pending: [], todo: [], done: [] }));
  parseFailure(run(project, acquireArgs(project)), 1, 'TASK_STATE_INVALID');
  assert.throws(() => lstatSync(project.lock), { code: 'ENOENT' });
});

await check('post-publication source drift releases only the exact owned lock and exits transiently', async () => {
  if (process.platform === 'win32') return;
  const project = makeProject('todo', 46);
  // Give the parent watcher a comfortable scheduling window between the
  // no-clobber link and the post-publication scan without adding a test hook to
  // production code.
  for (let i = 0; i < 1000; i++) {
    const stem = `TASK_${1000 + i}_filler_${i}`;
    writeFileSync(join(project.tasks, 'backlog', `${stem}.md`), backlogDoc(stem));
  }
  refreshIndex(project);
  const result = await runWithPostPublishMutation(project, acquireArgs(project, 'orchestrator', 'post-race'), () => {
    const task = join(project.tasks, 'todo', `${project.stem}.md`);
    writeFileSync(task, `${readFileSync(task, 'utf8').trimEnd()}\n\nPost-publication source mutation.\n`);
    refreshIndex(project);
  });
  parseFailure(result, 4, 'TASK_STATE_CHANGED');
  assert.throws(() => lstatSync(project.lock), { code: 'ENOENT' });
});

await check('same run/session acquisition is idempotent and preserves exact bytes', () => {
  const project = makeProject('backlog', 35);
  const args = acquireArgs(project);
  const first = parseSuccess(run(project, args));
  const bytes = readFileSync(project.lock);
  parseFailure(run(project, args), 1, 'LOCK_ALREADY_OWNED');
  const second = parseSuccess(run(project, [...args, '--expected-hash', first.lockHash]));
  assert.equal(second.created, false);
  assert.equal(second.lockHash, first.lockHash);
  assert.deepEqual(readFileSync(project.lock), bytes);
});

await check('idempotent acquire binds the owner process generation, not only its numeric pid', () => {
  const project = makeProject('backlog', 57);
  const args = acquireArgs(project, 'task-prep', 'pid-generation');
  parseSuccess(run(project, args));
  const record = JSON.parse(readFileSync(project.lock, 'utf8'));
  record.owner.processStartId = `psid-v1:${process.platform === 'darwin' ? 'darwin' : 'linux'}:${'0'.repeat(64)}`;
  const foreignBytes = Buffer.from(`${JSON.stringify(record, null, 2)}\n`);
  writeFileSync(project.lock, foreignBytes, { mode: 0o600 });
  const foreignHash = `sha256:${createHash('sha256').update(foreignBytes).digest('hex')}`;
  parseFailure(run(project, [...args, '--expected-hash', foreignHash]), 1, 'LOCK_ALREADY_OWNED');
  assert.deepEqual(readFileSync(project.lock), foreignBytes);
});

await check('different owner cannot overwrite an existing canonical or malformed lock', () => {
  const project = makeProject('backlog', 36);
  parseSuccess(run(project, acquireArgs(project, 'task-prep', 'first')));
  const firstBytes = readFileSync(project.lock);
  parseFailure(run(project, acquireArgs(project, 'task-prep', 'second')), 1, 'LOCK_ALREADY_OWNED');
  assert.deepEqual(readFileSync(project.lock), firstBytes);

  unlinkSync(project.lock);
  const malformed = Buffer.from('{"stage":"task-prep","startedAt":"2026-07-13T12:00:00Z"}\n');
  writeFileSync(project.lock, malformed);
  parseFailure(run(project, acquireArgs(project, 'task-prep', 'third')), 3, 'LOCK_INVALID');
  assert.deepEqual(readFileSync(project.lock), malformed);
});

await check('runtime validator rejects incomplete and malformed lock records', () => {
  const project = makeProject('backlog', 43);
  writeFileSync(project.lock, '{"stage":"task-prep","startedAt":"2026-07-13T12:00:00Z"}\n');
  let verdict = core.validateTaskState({
    repoRoot: project.root, tasksDir: project.tasks, locksDir: project.locks,
    checkIndex: true, includeRuntime: true, stem: project.stem, outcomeShapePath: OUTCOME_SHAPE,
  });
  assert.equal(verdict.ok, false, JSON.stringify(verdict.findings));
  assert.equal(verdict.findings.some((finding) => finding.code === 'LOCK_INVALID'), true);

  writeFileSync(project.lock, JSON.stringify({
    version: 1,
    stem: project.stem,
    stage: 'task-prep',
    runId: 'run-malformed-owner',
    sessionId: 'ws-malformed-owner-xxxxxxxx',
    startedAt: '2026-07-13T12:00:00.000Z',
    owner: {},
  }));
  verdict = core.validateTaskState({
    repoRoot: project.root, tasksDir: project.tasks, locksDir: project.locks,
    checkIndex: true, includeRuntime: true, stem: project.stem, outcomeShapePath: OUTCOME_SHAPE,
  });
  assert.equal(verdict.ok, false);
  assert.equal(verdict.findings.some((finding) => finding.code === 'LOCK_INVALID'), true);
});

await check('verify binds stage, run, session and exact bytes', () => {
  const project = makeProject('backlog', 37);
  const acquired = parseSuccess(run(project, acquireArgs(project)));
  const verified = parseSuccess(run(project, [
    'verify', '--stem', project.stem, '--stage', acquired.stage,
    '--run-id', acquired.runId, '--session-id', acquired.sessionId,
    '--expected-hash', acquired.lockHash,
  ]));
  assert.equal(verified.lockHash, acquired.lockHash);
  parseFailure(run(project, [
    'verify', '--stem', project.stem, '--stage', acquired.stage,
    '--run-id', acquired.runId, '--session-id', 'ws-another-session-xxxxxxxx',
    '--expected-hash', acquired.lockHash,
  ]), 1, 'LOCK_IDENTITY_MISMATCH');
});

await check('release requires the exact CAS tuple and never deletes a replacement', () => {
  const project = makeProject('backlog', 38);
  const acquired = parseSuccess(run(project, acquireArgs(project)));
  parseFailure(run(project, [
    'release', '--stem', project.stem, '--run-id', acquired.runId,
    '--session-id', acquired.sessionId, '--expected-hash', `sha256:${'0'.repeat(64)}`,
  ]), 1, 'LOCK_IDENTITY_MISMATCH');
  assert.equal(lstatSync(project.lock).isFile(), true);

  const replacement = JSON.parse(readFileSync(project.lock, 'utf8'));
  replacement.runId = 'run-external-replacement';
  replacement.sessionId = 'ws-external-replacement-xxxxxxxx';
  writeFileSync(project.lock, `${JSON.stringify(replacement, null, 2)}\n`);
  parseFailure(run(project, [
    'release', '--stem', project.stem, '--run-id', acquired.runId,
    '--session-id', acquired.sessionId, '--expected-hash', acquired.lockHash,
  ]), 1, 'LOCK_IDENTITY_MISMATCH');
  assert.equal(JSON.parse(readFileSync(project.lock, 'utf8')).runId, 'run-external-replacement');
});

await check('exact CAS release removes only the owned generation', () => {
  const project = makeProject('backlog', 39);
  const acquired = parseSuccess(run(project, acquireArgs(project)));
  const execution = run(project, releaseArgs(project, acquired));
  const released = parseSuccess(execution);
  assert.equal(released.released, true);
  assert.equal(released.state, acquired.observedState);
  assert.equal(released.sourceRevision, acquired.sourceRevision);
  assert.match(released.snapshotHash, /^sha256:[a-f0-9]{64}$/);
  assert.throws(() => lstatSync(project.lock), { code: 'ENOENT' });
  assert.equal(readdirSync(project.locks).some((name) => name.includes('.release-')), false);
  const completed = completedReleasePaths(project);
  assert.equal(completed.length, 1);
  const manifest = JSON.parse(readFileSync(join(completed[0], 'manifest.json'), 'utf8'));
  for (const field of ['dev', 'ino', 'mtimeNs', 'ctimeNs']) assert.match(manifest.sourceProof[field], /^(?:0|[1-9][0-9]*)$/);
  const events = observations(execution.stderr);
  assert.equal(events.length, 3, 'release must expose every pre/detach/post-detach verdict');
  for (const event of events) {
    assertObservationShape(event);
    assert.equal(event.action, 'lock-release');
    assert.equal(event.expectedState, acquired.observedState);
    assert.equal(event.result, 'valid');
  }
});

await check('release requires a fresh final state receipt and preserves the lock on source or INDEX drift', () => {
  const project = makeProject('backlog', 69);
  const acquired = parseSuccess(run(project, acquireArgs(project, 'task-prep', 'release-fence')));
  parseFailure(run(project, [
    'release', '--stem', project.stem, '--run-id', acquired.runId,
    '--session-id', acquired.sessionId, '--expected-hash', acquired.lockHash,
  ]), 2, 'LOCK_RELEASE_POSTCONDITION_REQUIRED');
  assert.equal(existsSync(project.lock), true);

  const task = join(project.tasks, 'backlog', `${project.stem}.md`);
  writeFileSync(task, `${readFileSync(task, 'utf8').trimEnd()}\n\nFresh clarification.\n`);
  parseFailure(run(project, releaseArgs(project, acquired)), 4, 'LOCK_RELEASE_POSTCONDITION_FAILED');
  assert.equal(existsSync(project.lock), true);
  assert.deepEqual(retainedReleasePaths(project), []);

  refreshIndex(project);
  const fresh = { ...acquired, ...currentStateReceipt(project) };
  const released = parseSuccess(run(project, releaseArgs(project, fresh)));
  assert.equal(released.sourceRevision, fresh.sourceRevision);
  assert.equal(existsSync(project.lock), false);
});

await check('release revalidates after prepare and keeps a recoverable pre-detach owner on drift', async () => {
  const project = makeProject('backlog', 70);
  const acquired = parseSuccess(run(project, acquireArgs(project, 'task-prep', 'prepare-fence')));
  const task = join(project.tasks, 'backlog', `${project.stem}.md`);
  const result = await runWithReleasePauseMutation(
    project, releaseArgs(project, acquired), 'TASK_LOCK_TEST_PAUSE_AFTER_RELEASE_PREPARE_MS',
    () => retainedReleasePaths(project).some((entry) => existsSync(join(entry, 'manifest.json'))),
    () => {
      writeFileSync(task, `${readFileSync(task, 'utf8').trimEnd()}\n\nPrepared-window mutation.\n`);
      refreshIndex(project);
    },
  );
  parseFailure(result, 4, 'LOCK_RELEASE_POSTCONDITION_FAILED');
  assert.equal(existsSync(project.lock), true);
  assert.equal(retainedReleasePaths(project).length, 1);

  const fresh = { ...acquired, ...currentStateReceipt(project) };
  const recovered = parseSuccess(run(project, releaseArgs(project, fresh, 'recover-release')));
  assert.equal(recovered.mode, 'pre-detach');
  assert.equal(existsSync(project.lock), false);
  assert.deepEqual(retainedReleasePaths(project), []);
});

await check('release revalidates while the detached generation still blocks every new owner', async () => {
  const project = makeProject('backlog', 71);
  const acquired = parseSuccess(run(project, acquireArgs(project, 'task-prep', 'detach-fence')));
  const task = join(project.tasks, 'backlog', `${project.stem}.md`);
  const result = await runWithReleasePauseMutation(
    project, releaseArgs(project, acquired), 'TASK_LOCK_TEST_PAUSE_AFTER_RELEASE_DETACH_MS',
    () => !existsSync(project.lock) && retainedReleasePaths(project).some((entry) => existsSync(join(entry, 'candidate'))),
    () => {
      writeFileSync(task, `${readFileSync(task, 'utf8').trimEnd()}\n\nDetached-window mutation.\n`);
      refreshIndex(project);
    },
  );
  parseFailure(result, 4, 'LOCK_RELEASE_POSTCONDITION_FAILED');
  assert.equal(existsSync(project.lock), false);
  assert.equal(retainedReleasePaths(project).length, 1);
  parseFailure(run(project, acquireArgs(project, 'task-prep', 'blocked-during-recovery')), 1, 'LOCK_RELEASE_RECOVERY_REQUIRED');

  const fresh = { ...acquired, ...currentStateReceipt(project) };
  const recovered = parseSuccess(run(project, releaseArgs(project, fresh, 'recover-release')));
  assert.equal(recovered.mode, 'detached');
  assert.equal(existsSync(project.lock), false);
  assert.deepEqual(retainedReleasePaths(project), []);
});

await check('release crash after detach is explicitly recoverable and recovery is idempotent', () => {
  const project = makeProject('backlog', 51);
  const acquired = parseSuccess(run(project, acquireArgs(project, 'task-prep', 'crash-recovery')));
  const ownedBytes = readFileSync(project.lock);
  const crashed = run(project, releaseArgs(project, acquired), {
    TASK_LOCK_TEST_CRASH_AFTER_RELEASE_DETACH: '1',
  });
  assert.equal(crashed.status, 87, crashed.stderr || crashed.stdout);
  assert.equal(existsSync(project.lock), false);
  const retained = retainedReleasePaths(project);
  assert.equal(retained.length, 1);
  assert.deepEqual(readFileSync(join(retained[0], 'candidate')), ownedBytes);

  parseFailure(run(project, releaseArgs(project, {
    ...acquired, lockHash: `sha256:${'0'.repeat(64)}`,
  }, 'recover-release')), 1, 'LOCK_IDENTITY_MISMATCH');
  assert.deepEqual(readFileSync(join(retained[0], 'candidate')), ownedBytes);
  parseFailure(run(project, acquireArgs(project, 'task-prep', 'blocked-by-crash')), 1, 'LOCK_RELEASE_RECOVERY_REQUIRED');
  const recovered = parseSuccess(run(project, releaseArgs(project, acquired, 'recover-release')));
  assert.equal(recovered.recovered, true);
  assert.equal(recovered.alreadyRecovered, false);
  assert.equal(recovered.mode, 'detached');
  assert.equal(existsSync(project.lock), false);
  assert.deepEqual(retainedReleasePaths(project), []);

  const repeated = parseSuccess(run(project, releaseArgs(project, acquired, 'recover-release')));
  assert.equal(repeated.recovered, false);
  assert.equal(repeated.alreadyRecovered, true);
  assert.deepEqual(retainedReleasePaths(project), []);
  assert.equal(parseSuccess(run(project, acquireArgs(project, 'task-prep', 'after-recovery'))).created, true);
});

await check('recover-release keeps active ownership until its own post-detach verdict is fresh', async () => {
  const project = makeProject('backlog', 72);
  const acquired = parseSuccess(run(project, acquireArgs(project, 'task-prep', 'recover-fence')));
  const prepared = run(project, releaseArgs(project, acquired), {
    TASK_LOCK_TEST_CRASH_AFTER_RELEASE_PREPARE: '1',
  });
  assert.equal(prepared.status, 85, prepared.stderr || prepared.stdout);
  assert.equal(existsSync(project.lock), true);
  assert.equal(retainedReleasePaths(project).length, 1);

  const task = join(project.tasks, 'backlog', `${project.stem}.md`);
  const raced = await runWithReleasePauseMutation(
    project, releaseArgs(project, acquired, 'recover-release'),
    'TASK_LOCK_TEST_PAUSE_AFTER_RECOVERY_DETACH_MS',
    () => !existsSync(project.lock) && retainedReleasePaths(project).some((entry) => existsSync(join(entry, 'candidate'))),
    () => {
      writeFileSync(task, `${readFileSync(task, 'utf8').trimEnd()}\n\nRecovery-window mutation.\n`);
      refreshIndex(project);
    },
  );
  parseFailure(raced, 4, 'LOCK_RELEASE_POSTCONDITION_FAILED');
  assert.equal(existsSync(project.lock), false);
  assert.equal(retainedReleasePaths(project).length, 1);
  parseFailure(run(project, acquireArgs(project, 'task-prep', 'recover-race-blocked')), 1, 'LOCK_RELEASE_RECOVERY_REQUIRED');

  const fresh = { ...acquired, ...currentStateReceipt(project) };
  const recovered = parseSuccess(run(project, releaseArgs(project, fresh, 'recover-release')));
  assert.equal(recovered.mode, 'detached');
  assert.equal(recovered.sourceRevision, fresh.sourceRevision);
  assert.deepEqual(retainedReleasePaths(project), []);
});

await check('release recovery covers current empty, prepared, and committed crash states', () => {
  for (const [number, hook, status, expectedEntries, mode] of [
    [58, 'TASK_LOCK_TEST_CRASH_AFTER_RELEASE_MKDIR', 84, [], 'pre-detach'],
    [59, 'TASK_LOCK_TEST_CRASH_AFTER_RELEASE_PREPARE', 85, ['manifest.json'], 'pre-detach'],
  ]) {
    const project = makeProject('backlog', number);
    const acquired = parseSuccess(run(project, acquireArgs(project, 'task-prep', `crash-${number}`)));
    const crashed = run(project, releaseArgs(project, acquired), { [hook]: '1' });
    assert.equal(crashed.status, status, crashed.stderr || crashed.stdout);
    const active = retainedReleasePaths(project);
    assert.equal(active.length, 1);
    assert.deepEqual(readdirSync(active[0]).sort(), expectedEntries);
    const recovered = parseSuccess(run(project, releaseArgs(project, acquired, 'recover-release')));
    assert.equal(recovered.mode, mode);
    assert.equal(existsSync(project.lock), false);
    assert.deepEqual(retainedReleasePaths(project), []);
  }

  const committed = makeProject('backlog', 60);
  const committedLock = parseSuccess(run(committed, acquireArgs(committed, 'task-prep', 'crash-commit')));
  const crashAfterCommit = run(committed, releaseArgs(committed, committedLock), {
    TASK_LOCK_TEST_CRASH_AFTER_RELEASE_COMMIT: '1',
  });
  assert.equal(crashAfterCommit.status, 88, crashAfterCommit.stderr || crashAfterCommit.stdout);
  assert.equal(existsSync(committed.lock), false);
  assert.deepEqual(retainedReleasePaths(committed), []);
  const repeated = parseSuccess(run(committed, releaseArgs(committed, committedLock, 'recover-release')));
  assert.equal(repeated.alreadyRecovered, true);
  assert.equal(repeated.mode, 'detached');
});

await check('recover-release captures a retained hard-link duplicate and leaves no canonical lock', () => {
  const project = makeProject('backlog', 52);
  const acquired = parseSuccess(run(project, acquireArgs(project, 'task-prep', 'duplicate-recovery')));
  const ownedBytes = readFileSync(project.lock);
  const crashed = run(project, releaseArgs(project, acquired), {
    TASK_LOCK_TEST_CRASH_AFTER_RELEASE_DETACH: '1',
  });
  assert.equal(crashed.status, 87, crashed.stderr || crashed.stdout);
  const retained = retainedReleasePaths(project);
  assert.equal(retained.length, 1);
  linkSync(join(retained[0], 'candidate'), project.lock);

  const recovered = parseSuccess(run(project, releaseArgs(project, acquired, 'recover-release')));
  assert.equal(recovered.mode, 'duplicate');
  assert.equal(existsSync(project.lock), false);
  assert.deepEqual(retainedReleasePaths(project), []);
  const repeated = parseSuccess(run(project, releaseArgs(project, acquired, 'recover-release')));
  assert.equal(repeated.recovered, false);
  assert.equal(repeated.alreadyRecovered, true);
  assert.equal(repeated.mode, 'detached');
  assert.equal(existsSync(project.lock), false);
});

await check('completed receipt captures only an exact hard-link reappearance and rejects an independent copy', () => {
  const exact = makeProject('backlog', 62);
  const acquired = parseSuccess(run(exact, acquireArgs(exact, 'task-prep', 'completed-reappeared')));
  const ownedBytes = readFileSync(exact.lock);
  parseSuccess(run(exact, releaseArgs(exact, acquired)));
  const completed = completedReleasePaths(exact);
  assert.equal(completed.length, 1);
  linkSync(join(completed[0], 'candidate'), exact.lock);
  const captured = parseSuccess(run(exact, releaseArgs(exact, acquired, 'recover-release')));
  assert.equal(captured.mode, 'reappeared-captured');
  assert.equal(existsSync(exact.lock), false);

  const foreign = makeProject('backlog', 63);
  const foreignOwned = parseSuccess(run(foreign, acquireArgs(foreign, 'task-prep', 'completed-foreign')));
  const foreignBytes = readFileSync(foreign.lock);
  parseSuccess(run(foreign, releaseArgs(foreign, foreignOwned)));
  writeFileSync(foreign.lock, foreignBytes, { mode: 0o600 });
  parseFailure(run(foreign, releaseArgs(foreign, foreignOwned, 'recover-release')), 1, 'LOCK_RELEASE_FOREIGN_CANONICAL');
  assert.deepEqual(readFileSync(foreign.lock), foreignBytes);
});

await check('recover-release preserves a byte-identical foreign canonical inode and the retained generation', () => {
  const project = makeProject('backlog', 53);
  const acquired = parseSuccess(run(project, acquireArgs(project, 'task-prep', 'foreign-recovery')));
  const ownedBytes = readFileSync(project.lock);
  const crashed = run(project, releaseArgs(project, acquired), {
    TASK_LOCK_TEST_CRASH_AFTER_RELEASE_DETACH: '1',
  });
  assert.equal(crashed.status, 87, crashed.stderr || crashed.stdout);
  const retained = retainedReleasePaths(project);
  assert.equal(retained.length, 1);
  const candidate = join(retained[0], 'candidate');
  const candidateStat = lstatSync(candidate);
  writeFileSync(project.lock, ownedBytes, { mode: 0o600 });
  assert.notEqual(lstatSync(project.lock).ino, candidateStat.ino);

  parseFailure(run(project, releaseArgs(project, acquired, 'recover-release')), 1, 'LOCK_RELEASE_FOREIGN_CANONICAL');
  assert.deepEqual(readFileSync(project.lock), ownedBytes);
  assert.deepEqual(readFileSync(candidate), ownedBytes);
  assert.deepEqual(retainedReleasePaths(project), retained);
});

await check('recover-release rejects ambiguous or malformed retained schemas without deleting data', () => {
  const ambiguous = makeProject('backlog', 54);
  const ambiguousOwned = parseSuccess(run(ambiguous, acquireArgs(ambiguous, 'task-prep', 'ambiguous-recovery')));
  assert.equal(run(ambiguous, releaseArgs(ambiguous, ambiguousOwned), {
    TASK_LOCK_TEST_CRASH_AFTER_RELEASE_DETACH: '1',
  }).status, 87);
  const original = retainedReleasePaths(ambiguous)[0];
  const second = join(ambiguous.locks, `.${ambiguous.stem}.json.release-${'b'.repeat(36)}`);
  mkdirSync(second, { mode: 0o700 });
  linkSync(join(original, 'candidate'), join(second, 'candidate'));
  parseFailure(run(ambiguous, releaseArgs(ambiguous, ambiguousOwned, 'recover-release')), 1, 'LOCK_RELEASE_RECOVERY_AMBIGUOUS');
  assert.equal(existsSync(join(original, 'candidate')), true);
  assert.equal(existsSync(join(second, 'candidate')), true);

  const malformed = makeProject('backlog', 55);
  const malformedOwned = parseSuccess(run(malformed, acquireArgs(malformed, 'task-prep', 'malformed-recovery')));
  assert.equal(run(malformed, releaseArgs(malformed, malformedOwned), {
    TASK_LOCK_TEST_CRASH_AFTER_RELEASE_DETACH: '1',
  }).status, 87);
  const malformedPath = retainedReleasePaths(malformed)[0];
  writeFileSync(join(malformedPath, 'unexpected'), 'preserve me\n');
  parseFailure(run(malformed, releaseArgs(malformed, malformedOwned, 'recover-release')), 1, 'LOCK_RELEASE_RECOVERY_INVALID');
  assert.equal(existsSync(join(malformedPath, 'candidate')), true);
  assert.equal(readFileSync(join(malformedPath, 'unexpected'), 'utf8'), 'preserve me\n');

  const malformedName = makeProject('backlog', 56);
  const malformedDir = join(malformedName.locks, `.${malformedName.stem}.json.release-not-a-valid-id`);
  mkdirSync(malformedDir, { mode: 0o700 });
  parseFailure(run(malformedName, [
    'recover-release', '--stem', malformedName.stem,
    '--run-id', 'run-malformed-recovery',
    '--session-id', 'ws-malformed-recovery-xxxxxxxx',
    '--expected-hash', `sha256:${'0'.repeat(64)}`,
    '--expected-state', currentStateReceipt(malformedName).observedState,
    '--source-revision', currentStateReceipt(malformedName).sourceRevision,
  ]), 1, 'LOCK_RELEASE_RECOVERY_INVALID');
  parseFailure(run(malformedName, acquireArgs(malformedName, 'task-prep', 'malformed-name')), 1, 'LOCK_RELEASE_RECOVERY_INVALID');
  assert.equal(lstatSync(malformedDir).isDirectory(), true);
});

await check('symlink and oversized lock paths fail closed and remain untouched', () => {
  const symlinkProject = makeProject('backlog', 40);
  symlinkSync('/dev/null', symlinkProject.lock);
  parseFailure(run(symlinkProject, acquireArgs(symlinkProject)), 3, 'LOCK_UNSAFE');
  assert.equal(lstatSync(symlinkProject.lock).isSymbolicLink(), true);

  const largeProject = makeProject('backlog', 41);
  writeFileSync(largeProject.lock, Buffer.alloc(32 * 1024 + 1, 0x61));
  parseFailure(run(largeProject, acquireArgs(largeProject)), 3, 'LOCK_TOO_LARGE');
  assert.equal(lstatSync(largeProject.lock).size, 32 * 1024 + 1);
  let verdict = core.validateTaskState({
    repoRoot: largeProject.root, tasksDir: largeProject.tasks, locksDir: largeProject.locks,
    checkIndex: true, includeRuntime: true, stem: largeProject.stem, outcomeShapePath: OUTCOME_SHAPE,
  });
  assert.equal(verdict.ok, false);
  assert.equal(verdict.findings.some((finding) => finding.code === 'LOCK_UNSAFE'), true);

  const ancestorProject = makeProject('backlog', 47);
  rmSync(ancestorProject.locks, { recursive: true, force: true });
  const external = join(ancestorProject.root, 'external-locks');
  mkdirSync(external);
  symlinkSync(external, ancestorProject.locks);
  parseFailure(run(ancestorProject, acquireArgs(ancestorProject)), 3, 'LOCK_DIRECTORY_UNSAFE');
  assert.throws(() => lstatSync(join(external, `${ancestorProject.stem}.json`)), { code: 'ENOENT' });
  verdict = core.validateTaskState({
    repoRoot: ancestorProject.root, tasksDir: ancestorProject.tasks, locksDir: ancestorProject.locks,
    checkIndex: true, includeRuntime: true, stem: ancestorProject.stem, outcomeShapePath: OUTCOME_SHAPE,
  });
  assert.equal(verdict.ok, false);
  assert.equal(verdict.findings.some((finding) => finding.code === 'LOCK_DIRECTORY_UNSAFE'), true);

  const readProject = makeProject('backlog', 50);
  const owned = parseSuccess(run(readProject, acquireArgs(readProject, 'task-prep', 'ancestor-read')));
  const ownedBytes = readFileSync(readProject.lock);
  rmSync(readProject.locks, { recursive: true, force: true });
  const externalRead = join(readProject.root, 'external-read-locks');
  mkdirSync(externalRead);
  writeFileSync(join(externalRead, `${readProject.stem}.json`), ownedBytes);
  symlinkSync(externalRead, readProject.locks);
  const exactIdentity = [
    '--stem', readProject.stem, '--stage', owned.stage,
    '--run-id', owned.runId, '--session-id', owned.sessionId,
    '--expected-hash', owned.lockHash,
  ];
  parseFailure(run(readProject, ['verify', ...exactIdentity]), 3, 'LOCK_DIRECTORY_UNSAFE');
  parseFailure(run(readProject, [
    'release', '--stem', readProject.stem, '--run-id', owned.runId,
    '--session-id', owned.sessionId, '--expected-hash', owned.lockHash,
  ]), 3, 'LOCK_DIRECTORY_UNSAFE');
  assert.deepEqual(readFileSync(join(externalRead, `${readProject.stem}.json`)), ownedBytes);

  const recoveryProject = makeProject('backlog', 49);
  mkdirSync(join(recoveryProject.locks, `.${recoveryProject.stem}.json.release-${'a'.repeat(36)}`));
  verdict = core.validateTaskState({
    repoRoot: recoveryProject.root, tasksDir: recoveryProject.tasks, locksDir: recoveryProject.locks,
    checkIndex: true, includeRuntime: true, stem: recoveryProject.stem, outcomeShapePath: OUTCOME_SHAPE,
  });
  assert.equal(verdict.ok, false);
  assert.equal(verdict.findings.some((finding) => finding.code === 'LOCK_RELEASE_RECOVERY_REQUIRED'), true);
  parseFailure(run(recoveryProject, acquireArgs(recoveryProject, 'task-prep', 'retained-release')), 1, 'LOCK_RELEASE_RECOVERY_REQUIRED');
  assert.equal(existsSync(recoveryProject.lock), false, 'retained release ownership must block a new canonical generation');
});

await check('prepared/completed release lookalikes fail closed and completed history is bounded', () => {
  const project = makeProject('backlog', 64);
  for (let index = 0; index < 257; index++) {
    mkdirSync(join(project.locks, `.${project.stem}.json.released-${index.toString(16).padStart(36, '0')}`), { mode: 0o700 });
  }
  parseFailure(run(project, acquireArgs(project, 'task-prep', 'receipt-limit')), 1, 'LOCK_RELEASE_RECEIPT_LIMIT');
  let verdict = core.validateTaskState({
    repoRoot: project.root, tasksDir: project.tasks, locksDir: project.locks,
    checkIndex: true, includeRuntime: true, stem: project.stem, outcomeShapePath: OUTCOME_SHAPE,
  });
  assert.equal(verdict.findings.some((finding) => finding.code === 'LOCK_RELEASE_RECEIPT_LIMIT'), true);

  const malformed = makeProject('backlog', 65);
  mkdirSync(join(malformed.locks, `.${malformed.stem}.json.released-not-canonical`), { mode: 0o700 });
  parseFailure(run(malformed, acquireArgs(malformed, 'task-prep', 'receipt-malformed')), 1, 'LOCK_RELEASE_RECEIPT_LIMIT');
  verdict = core.validateTaskState({
    repoRoot: malformed.root, tasksDir: malformed.tasks, locksDir: malformed.locks,
    checkIndex: true, includeRuntime: true, stem: malformed.stem, outcomeShapePath: OUTCOME_SHAPE,
  });
  assert.equal(verdict.findings.some((finding) => finding.code === 'LOCK_RELEASE_RECEIPT_INVALID'), true);

  const prepared = makeProject('backlog', 67);
  const preparedPath = join(prepared.locks, `.${prepared.stem}.json.release-not-canonical`);
  mkdirSync(preparedPath, { mode: 0o700 });
  parseFailure(run(prepared, acquireArgs(prepared, 'task-prep', 'prepared-malformed')), 1, 'LOCK_RELEASE_RECOVERY_INVALID');
  verdict = core.validateTaskState({
    repoRoot: prepared.root, tasksDir: prepared.tasks, locksDir: prepared.locks,
    checkIndex: true, includeRuntime: true, stem: prepared.stem, outcomeShapePath: OUTCOME_SHAPE,
  });
  assert.equal(verdict.findings.some((finding) => finding.code === 'LOCK_RELEASE_RECOVERY_INVALID'), true);
  assert.equal(lstatSync(preparedPath).isDirectory(), true, 'malformed recovery ownership must be preserved');
});

await check('anchored lock admission rejects swap-back ancestry without touching either generation', () => {
  const project = makeProject('backlog', 66);
  const foreign = join(project.root, 'foreign-locks');
  mkdirSync(foreign, { mode: 0o700 });
  const swapEnv = {
    TASK_FS_TEST_SWAP_PATH: project.locks,
    TASK_FS_TEST_SWAP_WITH: foreign,
    TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY: '1',
  };
  parseFailure(run(project, acquireArgs(project, 'task-prep', 'swap-back'), swapEnv), 4, 'LOCK_MUTEX_OWNERSHIP_LOST');
  assert.equal(existsSync(project.lock), false);
  assert.deepEqual(readdirSync(foreign), []);
});

await check('publication cleanup retains a replaced temporary and exact proofs reject non-canonical fields', () => {
  const project = makeProject('backlog', 68);
  const target = join(project.locks, 'publication-target');
  const replacement = join(project.locks, 'foreign-replacement');
  const existing = Buffer.from('existing canonical generation\n');
  const foreign = Buffer.from('foreign temporary generation\n');
  const candidate = Buffer.from('owned publication candidate\n');
  writeFileSync(target, existing);
  writeFileSync(replacement, foreign);
  const raced = runBoundary(project, {
    action: 'publish', path: target, authorityRoot: project.root,
    rawBase64: candidate.toString('base64'), maxBytes: 1024, createParents: false,
  }, { TASK_FS_TEST_REPLACE_PUBLISH_TEMP_WITH: replacement });
  assert.equal(raced.ok, false);
  assert.equal(raced.error.code, 'TEMP_CLEANUP_AMBIGUOUS');
  assert.deepEqual(readFileSync(target), existing, 'an existing destination must never be replaced');
  const retained = readdirSync(project.locks).filter((name) => name.includes('.publish-'));
  assert.equal(retained.length, 2, 'both ambiguous temporary generations must be retained');
  const retainedBytes = retained.map((name) => readFileSync(join(project.locks, name)).toString('hex')).sort();
  assert.deepEqual(retainedBytes, [candidate.toString('hex'), foreign.toString('hex')].sort());

  const destination = join(project.locks, 'must-not-appear');
  const baseProof = { dev: '1', ino: '2', mode: 0o100600, size: existing.length, mtimeNs: '3', ctimeNs: '4' };
  for (const expectedSource of [
    { ...baseProof, extra: 'forbidden' },
    { ...baseProof, dev: '-1' },
    { ...baseProof, ino: '01' },
    { ...baseProof, mode: -1 },
  ]) {
    const invalid = runBoundary(project, {
      action: 'rename-exact', sourcePath: target, targetPath: destination,
      authorityRoot: project.root, expectedSource,
    });
    assert.equal(invalid.ok, false);
    assert.equal(invalid.error.code, 'ARGUMENT_INVALID');
    assert.deepEqual(readFileSync(target), existing);
    assert.equal(existsSync(destination), false);
  }

  const source = join(project.locks, 'rename-source');
  const injected = join(project.locks, 'rename-foreign');
  writeFileSync(source, candidate);
  writeFileSync(injected, foreign);
  const inspected = runBoundary(project, {
    action: 'read', path: source, authorityRoot: project.root, maxBytes: 1024,
  });
  assert.equal(inspected.ok, true);
  const { kind: _kind, ...expectedSource } = inspected.result.stat;
  const noClobber = runBoundary(project, {
    action: 'rename-exact', sourcePath: source, targetPath: destination,
    authorityRoot: project.root, expectedSource,
  }, { TASK_FS_TEST_RENAME_TARGET_WITH: injected });
  assert.equal(noClobber.ok, false);
  assert.equal(noClobber.error.code, 'EEXIST');
  assert.deepEqual(readFileSync(source), candidate, 'source must survive a losing no-clobber publication');
  assert.deepEqual(readFileSync(destination), foreign, 'foreign destination must be preserved byte-for-byte');
  assert.equal(existsSync(injected), false, 'fixture foreign generation must now be the destination inode');
});

await check('verify is never an identity-free inspect alias', () => {
  const project = makeProject('backlog', 48);
  parseSuccess(run(project, acquireArgs(project)));
  parseFailure(run(project, ['verify', '--stem', project.stem]), 2, 'INVOCATION_INVALID');
  const inspected = parseSuccess(run(project, ['inspect', '--stem', project.stem]));
  assert.equal(inspected.stem, project.stem);
});

await check('dead Site owner recovery is two-phase, hash-bound, writer-fenced, and exact', async () => {
  const project = makeProject('todo', 70);
  const owner = spawn(process.execPath, ['-e', 'setInterval(() => {}, 1000)'], { stdio: 'ignore' });
  let lease = null;
  try {
    await waitUntil(() => {
      try { return writerLeases.PROCESS_START_ID_RE.test(writerLeases.captureProcessStartId(owner.pid)); }
      catch (_) { return false; }
    }, 'fixture owner process identity was not observable');
    const acquired = parseSuccess(run(project, siteAcquireArgs(project, owner.pid, 'orchestrator', 'dead-owner')));
    owner.kill('SIGTERM');
    await new Promise((resolve) => owner.once('close', resolve));

    const inspected = parseSuccess(run(project, ['owner-status', '--stem', project.stem]));
    assert.equal(inspected.operation, 'owner-status');
    assert.equal(inspected.recoverable, true);
    assert.ok(['dead', 'reused'].includes(inspected.ownerState));
    assert.equal(inspected.lockHash, acquired.lockHash);

    const authorityMissing = parseFailure(run(project, [
      'recover-owner', '--stem', project.stem, '--expected-hash', inspected.lockHash,
    ]), 1, 'LOCK_OWNER_RECOVERY_REFUSED');
    assert.equal(authorityMissing.details.reason, 'recovery-authority-missing');
    assert.equal(existsSync(project.lock), true);

    lease = acquireRecoveryLease(project);
    parseFailure(run(project, [
      'recover-owner', '--stem', project.stem, '--expected-hash', `sha256:${'0'.repeat(64)}`,
    ], recoveryEnv(project, lease)), 1, 'LOCK_IDENTITY_MISMATCH');
    assert.equal(existsSync(project.lock), true);

    const recovered = parseSuccess(run(project, [
      'recover-owner', '--stem', project.stem, '--expected-hash', inspected.lockHash,
    ], recoveryEnv(project, lease)));
    assert.equal(recovered.operation, 'recover-owner');
    assert.equal(recovered.released, true);
    assert.equal(recovered.state, 'todo');
    // The healthy branch of the INDEX verdict: the site rejects any value outside
    // fresh|stale|invalid, so both branches must be pinned.
    assert.equal(recovered.indexStatus, 'fresh');
    assert.equal(existsSync(project.lock), false);
    assert.equal(completedReleasePaths(project).length, 1);
  } finally {
    if (owner.exitCode === null && owner.signalCode === null) owner.kill('SIGKILL');
    if (lease) assert.equal(writerLeases.release(lease), true);
  }
});

await check('dead-owner recovery survives an unpublished workspace INDEX', async () => {
  // The crash that strands a lock is the same crash that leaves INDEX.json
  // unpublished, and republishing it needs a transition the held lock refuses.
  // Gating recovery on INDEX freshness therefore made the recovery path
  // unreachable in exactly the situation it exists for.
  const project = makeProject('todo', 74);
  const owner = spawn(process.execPath, ['-e', 'setInterval(() => {}, 1000)'], { stdio: 'ignore' });
  let lease = null;
  try {
    await waitUntil(() => {
      try { return writerLeases.PROCESS_START_ID_RE.test(writerLeases.captureProcessStartId(owner.pid)); }
      catch (_) { return false; }
    }, 'fixture owner process identity was not observable');
    const acquired = parseSuccess(run(project, siteAcquireArgs(project, owner.pid, 'orchestrator', 'stale-index')));

    // The run held the lock on a healthy workspace; the INDEX drifts afterwards.
    // Keep the canonical schema and empty only the columns.
    const indexFile = join(project.tasks, 'INDEX.json');
    const published = JSON.parse(readFileSync(indexFile, 'utf8'));
    for (const column of ['backlog', 'pending', 'todo', 'done']) {
      if (Array.isArray(published[column])) published[column] = [];
    }
    writeFileSync(indexFile, `${JSON.stringify(published, null, 2)}\n`);
    const stale = core.validateTaskState({
      repoRoot: project.root, tasksDir: project.tasks, outcomeShapePath: OUTCOME_SHAPE,
      stem: project.stem, checkIndex: true, includeRuntime: false,
    });
    assert.equal(stale.ok, false, 'fixture must reproduce a stale INDEX');
    assert.equal(stale.indexStatus, 'stale');
    assert.deepEqual(stale.findings.filter((item) =>
      (item.severity === 'error' || item.severity === 'blocker') &&
      !String(item.code || '').startsWith('INDEX_')).map((item) => item.code), [],
      'INDEX staleness must be the only blocker in this fixture');

    owner.kill('SIGTERM');
    await new Promise((resolve) => owner.once('close', resolve));

    const inspected = parseSuccess(run(project, ['owner-status', '--stem', project.stem]));
    assert.equal(inspected.recoverable, true);
    assert.equal(inspected.lockHash, acquired.lockHash);

    lease = acquireRecoveryLease(project);
    const recovered = parseSuccess(run(project, [
      'recover-owner', '--stem', project.stem, '--expected-hash', inspected.lockHash,
    ], recoveryEnv(project, lease)));
    assert.equal(recovered.released, true);
    assert.equal(recovered.state, 'todo');
    // The detach publishes no INDEX, so it must report that it is still stale
    // rather than letting the board look canonical.
    assert.equal(recovered.indexStatus, 'stale');
    assert.equal(existsSync(project.lock), false);
  } finally {
    if (owner.exitCode === null && owner.signalCode === null) owner.kill('SIGKILL');
    if (lease) assert.equal(writerLeases.release(lease), true);
  }
});

await check('owner recovery preserves live, foreign-kind, and writer-active locks', async () => {
  const live = makeProject('todo', 71);
  const liveLock = parseSuccess(run(live, siteAcquireArgs(live, process.pid, 'orchestrator', 'live-owner')));
  let status = parseSuccess(run(live, ['owner-status', '--stem', live.stem]));
  assert.equal(status.recoverable, false);
  assert.equal(status.reason, 'owner-active');
  const liveLease = acquireRecoveryLease(live);
  try {
    const refused = parseFailure(run(live, [
      'recover-owner', '--stem', live.stem, '--expected-hash', liveLock.lockHash,
    ], recoveryEnv(live, liveLease)), 1, 'LOCK_OWNER_RECOVERY_REFUSED');
    assert.equal(refused.details.reason, 'owner-active');
    assert.equal(existsSync(live.lock), true);
  } finally {
    assert.equal(writerLeases.release(liveLease), true);
  }

  const direct = makeProject('todo', 72);
  parseSuccess(run(direct, acquireArgs(direct, 'orchestrator', 'direct-owner')));
  status = parseSuccess(run(direct, ['owner-status', '--stem', direct.stem]));
  assert.equal(status.recoverable, false);
  assert.equal(status.reason, 'owner-kind-unsupported');
  assert.equal(existsSync(direct.lock), true);

  const writer = makeProject('todo', 73);
  const owner = spawn(process.execPath, ['-e', 'setInterval(() => {}, 1000)'], { stdio: 'ignore' });
  let conflict = null;
  try {
    await waitUntil(() => {
      try { return writerLeases.PROCESS_START_ID_RE.test(writerLeases.captureProcessStartId(owner.pid)); }
      catch (_) { return false; }
    }, 'writer-conflict owner identity was not observable');
    parseSuccess(run(writer, siteAcquireArgs(writer, owner.pid, 'orchestrator', 'writer-conflict')));
    owner.kill('SIGTERM');
    await new Promise((resolve) => owner.once('close', resolve));
    conflict = writerLeases.acquire(writer.writers, {
      rootDir: writer.root, kind: 'task-session', stem: writer.stem,
      key: `task:${writer.stem}`, ownerPid: process.pid, pendingChild: false,
      sessionId: writerLeases.createSessionId(),
    });
    status = parseSuccess(run(writer, ['owner-status', '--stem', writer.stem]));
    assert.equal(status.recoverable, false);
    assert.equal(status.reason, 'writer-active');
    assert.equal(existsSync(writer.lock), true);
  } finally {
    if (owner.exitCode === null && owner.signalCode === null) owner.kill('SIGKILL');
    if (conflict) assert.equal(writerLeases.release(conflict), true);
  }
});

await check('concurrent no-clobber acquisition has exactly one winner', async () => {
  const project = makeProject('backlog', 42);
  const [a, b] = await Promise.all([
    runAsync(project, acquireArgs(project, 'task-prep', 'concurrent-a')),
    runAsync(project, acquireArgs(project, 'task-prep', 'concurrent-b')),
  ]);
  assert.deepEqual([a.status, b.status].sort(), [0, 1], `${a.stderr}\n${b.stderr}`);
  const record = JSON.parse(readFileSync(project.lock, 'utf8'));
  assert.ok(['run-lock-fixture-concurrent-a', 'run-lock-fixture-concurrent-b'].includes(record.runId));
  const loser = a.status === 1 ? a : b;
  assert.equal(parseErrorEnvelope(loser.stderr).code, 'LOCK_ALREADY_OWNED');
});

await check('concurrent identical releases serialize and converge to one completed receipt', async () => {
  const project = makeProject('backlog', 69);
  const acquired = parseSuccess(run(project, acquireArgs(project, 'task-prep', 'concurrent-release')));
  const args = releaseArgs(project, acquired);
  const firstPromise = runAsync(project, args, { TASK_LOCK_TEST_PAUSE_AFTER_RELEASE_PREPARE_MS: '900' });
  await waitUntil(() => retainedReleasePaths(project).length === 1,
    'first release did not publish its prepared receipt');
  const secondPromise = runAsync(project, args);
  const [first, second] = await Promise.all([firstPromise, secondPromise]);
  const firstValue = parseSuccess(first);
  const secondValue = parseSuccess(second);
  assert.equal(firstValue.released, true);
  assert.equal(secondValue.alreadyRecovered, true);
  assert.equal(retainedReleasePaths(project).length, 0);
  assert.equal(completedReleasePaths(project).length, 1);
  assert.equal(existsSync(project.lock), false);
  const reacquired = parseSuccess(run(project, acquireArgs(project, 'task-prep', 'after-concurrent-release')));
  assert.equal(reacquired.created, true);
});

for (const root of roots) {
  try { rmSync(root, { recursive: true, force: true }); } catch (_) {}
}

if (failures.length) {
  console.error(`task-lock: ${failures.length}/${checks} checks failed`);
  process.exitCode = 1;
} else {
  console.log(`task-lock: ${checks} checks passed`);
}
