#!/usr/bin/env node

import assert from 'node:assert/strict'
import { chmodSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { spawn, spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const modulePath = resolve(HERE, '../server/shallow-intake.js')
const writerPath = resolve(HERE, '../../tasks/writer-leases.cjs')
const require = createRequire(import.meta.url)
const writerLeases = require('../../tasks/writer-leases.cjs')

function waitFor(fn, timeout = 10000) {
  const deadline = Date.now() + timeout
  return new Promise((resolveWait, rejectWait) => {
    const poll = () => {
      try {
        const value = fn()
        if (value) return resolveWait(value)
      } catch {}
      if (Date.now() >= deadline) return rejectWait(new Error('timed out waiting for Darwin orphan fixture'))
      setTimeout(poll, 25)
    }
    poll()
  })
}

function runNode(source, env, timeout = 20000) {
  const child = spawn(process.execPath, ['-e', source, modulePath, writerPath], {
    env, stdio: ['ignore', 'pipe', 'pipe']
  })
  let stdout = '', stderr = ''
  child.stdout.setEncoding('utf8'); child.stderr.setEncoding('utf8')
  child.stdout.on('data', (chunk) => { stdout += chunk })
  child.stderr.on('data', (chunk) => { stderr += chunk })
  return new Promise((resolveRun, rejectRun) => {
    const timer = setTimeout(() => {
      try { child.kill('SIGKILL') } catch {}
      rejectRun(new Error(`Darwin orphan subprocess timed out: ${stderr}${stdout}`))
    }, timeout)
    child.on('error', rejectRun)
    child.on('close', (code, signal) => {
      clearTimeout(timer)
      resolveRun({ code, signal, stdout, stderr })
    })
  })
}

if (process.platform !== 'darwin') {
  console.log('shallow-intake Darwin orphan recovery: skipped on non-Darwin host')
  process.exit(0)
}

const root = mkdtempSync(join(tmpdir(), 'shallow-darwin-orphan-'))
const tasks = join(root, 'tasks')
const cache = join(root, 'cache')
const intakeDir = join(cache, 'intake')
const scratchAuthorityRoot = mkdtempSync(join(tmpdir(), 'shallow-darwin-scratch-'))
const scratchDir = join(scratchAuthorityRoot, 'scratch')
const workerFile = join(intakeDir, '.worker.json')
const marker = join(intakeDir, 'escaped.pid')
let site = null
let worker = null
let escapedPid = null

try {
  for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasks, column), { recursive: true })
  for (const name of ['intake', 'locks', 'requests', 'finalizations', 'creations', 'edits']) {
    mkdirSync(join(cache, name), { recursive: true })
  }
  chmodSync(intakeDir, 0o700)
  const stem = 'TASK_1_darwin_orphan'
  const taskSource = require('../../tasks/task-source-contract.cjs')
  const taskCore = require('../../tasks/task-state-core.cjs')
  const sourceBlock = taskSource.render(taskSource.manualForIntent('darwin-orphan', 'manual', 'fixture:darwin-orphan'))
  writeFileSync(join(tasks, 'backlog', stem + '.md'), '# TASK 1 — Darwin orphan\n\n' + sourceBlock + '\n\n## Goal\nExercise exact stale model recovery.\n')
  const initial = taskCore.validateTaskState({ tasksDir: tasks, repoRoot: root, checkIndex: false, includeRuntime: false })
  assert.equal(initial.ok, true, JSON.stringify(initial.findings))
  writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify(taskCore.deriveIndex(initial._model, '2026-07-13T00:00:00Z'), null, 2) + '\n')
  const source = join(root, 'darwin-orphan-model.c')
  const fake = join(root, 'darwin-orphan-model')
  writeFileSync(source, String.raw`
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
int main(void) {
  int ready[2];
  if (pipe(ready) != 0) return 2;
  pid_t child = fork();
  if (child < 0) return 3;
  if (child > 0) {
    close(ready[1]);
    char byte = 0;
    ssize_t size = read(ready[0], &byte, 1);
    close(ready[0]);
    return size == 1 && byte == 'R' ? 0 : 4;
  }
  close(ready[0]);
  if (setsid() < 0) _exit(5);
  const char *tmp = getenv("TMPDIR");
  char marker[PATH_MAX];
  if (!tmp || snprintf(marker, sizeof(marker), "%s/escaped.pid", tmp) <= 0) _exit(6);
  int fd = open(marker, O_WRONLY | O_CREAT | O_EXCL, 0600);
  if (fd < 0 || dprintf(fd, "%d\n", getpid()) < 0 || fsync(fd) != 0) _exit(7);
  close(fd);
  signal(SIGTERM, SIG_IGN);
  if (write(ready[1], "R", 1) != 1) _exit(8);
  close(ready[1]);
  close(STDIN_FILENO); close(STDOUT_FILENO); close(STDERR_FILENO);
  for (;;) pause();
}
`)
  const compiled = spawnSync(process.env.CC || 'cc', ['-O2', '-Wall', '-Wextra', '-o', fake, source], {
    encoding: 'utf8', timeout: 30000
  })
  assert.equal(compiled.status, 0, compiled.stderr || compiled.stdout || 'cannot compile Darwin orphan fixture')
  chmodSync(fake, 0o700)
  const env = {
    ...process.env,
    ORCHESTRATOR_PROJECT_ROOT: root,
    ORCHESTRATOR_TASKS_DIR: tasks,
    ORCHESTRATOR_TASK_INTAKE_DIR: intakeDir,
    ORCHESTRATOR_LOCKS_DIR: join(cache, 'locks'),
    ORCHESTRATOR_REQUESTS_DIR: join(cache, 'requests'),
    ORCHESTRATOR_FINALIZATIONS_DIR: join(cache, 'finalizations'),
    ORCHESTRATOR_TASK_CREATIONS_DIR: join(cache, 'creations'),
    ORCHESTRATOR_TASK_EDITS_DIR: join(cache, 'edits'),
    SHALLOW_INTAKE_SCRATCH_DIR: scratchDir,
    SHALLOW_INTAKE_SCRATCH_ROOT: scratchAuthorityRoot,
    SHALLOW_INTAKE_TIMEOUT_MS: '600000',
    SHALLOW_INTAKE_CONCURRENCY: '1',
    SHALLOW_INTAKE_CLAUDE: fake,
    TMPDIR: intakeDir
  }

  const siteSource = `
const intake = require(process.argv[1]);
intake.init();
intake.schedule(${JSON.stringify(stem)}, 'darwin-double-crash');
setInterval(() => {}, 1000);
`
  site = spawn(process.execPath, ['-e', siteSource, modulePath], {
    env, detached: false, stdio: ['ignore', 'ignore', 'ignore']
  })
  worker = await waitFor(() => {
    if (!existsSync(workerFile) || !existsSync(marker)) return null
    const value = JSON.parse(readFileSync(workerFile, 'utf8'))
    return value.pid === site.pid && value.childPid && value.modelPid && value.modelProcessStartId && value
  }, 15000)
  escapedPid = Number(readFileSync(marker, 'utf8').trim())
  assert.ok(Number.isInteger(escapedPid) && escapedPid > 0 && escapedPid !== worker.modelPid,
    'the native fixture must publish its setsid-escaped child generation')
  await waitFor(() => !writerLeases.processIdentityMatches(worker.modelPid, worker.modelProcessStartId), 5000)
  assert.equal(existsSync(join(scratchDir, worker.requestId, '.model-executable')), true,
    'the live Darwin generation must use the private pinned executable')

  // Kill the wrapper only after its direct model exited and the escaped pinned
  // child left the original PGID. Without an authenticated DRAINED receipt the
  // site and startup recovery must retain every durable ownership artifact.
  process.kill(worker.childPid, 'SIGKILL')
  await waitFor(() => {
    try { process.kill(worker.childPid, 0); return false }
    catch (error) { return error && error.code === 'ESRCH' }
  }, 5000)
  const eventsFile = join(intakeDir, 'events.jsonl')
  const terminalEvents = () => existsSync(eventsFile)
    ? readFileSync(eventsFile, 'utf8').trim().split('\n').filter(Boolean).map((line) => JSON.parse(line))
      .filter((event) => event.stem === stem && event.requestId === worker.requestId)
    : []
  await waitFor(() => terminalEvents().some((event) =>
    event.event === 'shallow-intake-stale' && event.reasonCode === 'containment-drain-unverified'), 5000)
  const retainedChecking = JSON.parse(readFileSync(join(intakeDir, stem + '.json'), 'utf8'))
  assert.equal(retainedChecking.status, 'checking',
    'a live site must not publish a terminal result after losing the Darwin DRAINED receipt')
  assert.equal(terminalEvents().some((event) =>
    event.event === 'shallow-intake-failed' || event.event === 'shallow-intake-completed'), false,
  'a wrapper crash with an escaped generation must not emit a terminal advisory event')
  process.kill(site.pid, 'SIGKILL')
  await waitFor(() => site.exitCode !== null || site.signalCode !== null, 5000)
  assert.doesNotThrow(() => process.kill(escapedPid, 0),
    'the fixture must prove the escaped generation survived the wrapper/site crash')

  // Keep startup recovery deterministic: the task is no longer intake-eligible,
  // so init must drain/recover the stale generation without launching a new one.
  rmSync(join(tasks, 'backlog', stem + '.md'))
  writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify({
    version: 2, generatedAt: '2026-07-13T00:00:01.000Z', backlog: [], pending: [], todo: [], done: []
  }, null, 2) + '\n')

  const recovery = await runNode(`
const fs = require('node:fs');
const intake = require(process.argv[1]);
const worker = process.env.ORCHESTRATOR_TASK_INTAKE_DIR + '/.worker.json';
intake.init();
const recovered = !fs.existsSync(worker);
for (let index = 0; index < 4; index++) intake._reconcileScratchRoot('intake-' + 'f'.repeat(32), true);
console.log(JSON.stringify({ recovered }));
`, env)
  assert.equal(recovery.code, 0, recovery.stderr + recovery.stdout)
  assert.deepEqual(JSON.parse(recovery.stdout.trim()), { recovered: false })
  assert.equal(existsSync(workerFile), true)
  assert.equal(existsSync(join(scratchDir, worker.requestId)), true,
    'crash recovery must retain the pinned path without authenticated DRAINED')
  assert.doesNotThrow(() => process.kill(escapedPid, 0))
  process.kill(escapedPid, 'SIGKILL')
  await waitFor(() => {
    try { process.kill(escapedPid, 0); return false }
    catch (error) { return error && error.code === 'ESRCH' }
  }, 5000)
  escapedPid = null
  const afterDeath = await runNode(`
const fs = require('node:fs');
const intake = require(process.argv[1]);
const worker = process.env.ORCHESTRATOR_TASK_INTAKE_DIR + '/.worker.json';
intake.init();
console.log(JSON.stringify({ recovered: !fs.existsSync(worker) }));
`, env)
  assert.equal(afterDeath.code, 0, afterDeath.stderr + afterDeath.stdout)
  assert.deepEqual(JSON.parse(afterDeath.stdout.trim()), { recovered: false },
    'direct observation of later process death cannot forge the lost wrapper receipt')
  assert.equal(existsSync(join(scratchDir, worker.requestId)), true)
  console.log('ok 1 - a Darwin wrapper crash after setsid escape retains ownership without authenticated DRAINED')

  const adversarial = await runNode(`
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const intake = require(process.argv[1]);
const writer = require(process.argv[2]);
const file = process.env.ORCHESTRATOR_TASK_INTAKE_DIR + '/.worker.json';
const start = writer.captureProcessStartId(process.pid);
const record = {
  version: 1, pid: 111111, processStartId: start, hostname: os.hostname(),
  token: 'intake-' + 'a'.repeat(32), createdAt: '2026-07-13T00:00:00.000Z',
  stem: 'TASK_1_darwin_orphan', sourceHash: 'sha256:' + 'b'.repeat(64),
  requestId: 'intake-' + 'c'.repeat(32), attempt: 1,
  childPid: 111112, childProcessStartId: start,
  modelPid: 111113, modelProcessStartId: start,
  spawnState: 'started', updatedAt: '2026-07-13T00:00:00.000Z'
};
const bytes = Buffer.from(JSON.stringify(record) + '\\n');
const publish = () => { fs.writeFileSync(file, bytes, { mode: 0o600 }); fs.chmodSync(file, 0o600); };
const proofFn = () => ({ ok: true, caller: { pgid: record.childPid } });
publish();
const replacementSignals = [];
const replaced = intake._reapDarwinOrphanedModel(file, {
  platform: 'darwin', stateFn(pid) { return pid === record.modelPid ? 'match' : 'dead'; }, proofFn,
  signalFn(pgid, signal) { replacementSignals.push([pgid, signal]); }, sleepFn() {},
  beforeSignal() {
    const next = file + '.replacement';
    fs.writeFileSync(next, bytes, { mode: 0o600 }); fs.chmodSync(next, 0o600); fs.renameSync(next, file);
  }
});
publish();
let modelChecks = 0;
const reuseSignals = [];
const reused = intake._reapDarwinOrphanedModel(file, {
  platform: 'darwin',
  stateFn(pid) {
    if (pid !== record.modelPid) return 'dead';
    modelChecks++;
    return modelChecks < 3 ? 'match' : 'reused';
  },
  proofFn, signalFn(pgid, signal) { reuseSignals.push([pgid, signal]); }, sleepFn() {}
});
publish();
fs.chmodSync(process.env.ORCHESTRATOR_TASK_INTAKE_DIR, 0o777);
const privacySignals = [];
const nonPrivate = intake._reapDarwinOrphanedModel(file, {
  platform: 'darwin', stateFn(pid) { return pid === record.modelPid ? 'match' : 'dead'; }, proofFn,
  signalFn(pgid, signal) { privacySignals.push([pgid, signal]); }, sleepFn() {}
});
fs.chmodSync(process.env.ORCHESTRATOR_TASK_INTAKE_DIR, 0o700);
console.log(JSON.stringify({ replaced, replacementSignals, reused, reuseSignals, nonPrivate, privacySignals }));
`, env)
  assert.equal(adversarial.code, 0, adversarial.stderr + adversarial.stdout)
  const blocked = JSON.parse(adversarial.stdout.trim())
  assert.equal(blocked.replaced.ok, false)
  assert.equal(blocked.reused.ok, false)
  assert.equal(blocked.nonPrivate.ok, false)
  assert.deepEqual(blocked.replacementSignals, [])
  assert.deepEqual(blocked.reuseSignals, [])
  assert.deepEqual(blocked.privacySignals, [])
  console.log('ok 2 - non-private authority, replaced owner inode, and last-moment PID reuse receive no Darwin signal')
  console.log('shallow-intake Darwin orphan recovery: 2 checks passed')
} finally {
  if (site && site.exitCode === null && site.signalCode === null) try { site.kill('SIGKILL') } catch {}
  if (worker) {
    try {
      if (writerLeases.processIdentityMatches(worker.childPid, worker.childProcessStartId)) {
        process.kill(worker.childPid, 'SIGTERM')
      }
    } catch {}
    try {
      if (writerLeases.processIdentityMatches(worker.modelPid, worker.modelProcessStartId)) {
        process.kill(worker.modelPid, 'SIGKILL')
      }
    } catch {}
  }
  if (escapedPid) try { process.kill(escapedPid, 'SIGKILL') } catch {}
  rmSync(root, { recursive: true, force: true })
  rmSync(scratchAuthorityRoot, { recursive: true, force: true })
}
