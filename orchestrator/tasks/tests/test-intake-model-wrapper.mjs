#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash, randomBytes } from 'node:crypto'
import { chmodSync, existsSync, mkdtempSync, readFileSync, renameSync, rmSync, statSync, truncateSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { spawn, spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const wrapper = resolve(HERE, '..', 'intake-model-wrapper.py')
const root = mkdtempSync(join(tmpdir(), 'intake-wrapper-test-'))
chmodSync(root, 0o700)
const python = process.env.ORCHESTRATOR_WRITER_PYTHON || 'python3'
let externalEscapeMarker = null

async function runProbe(name, source, options = {}) {
  const startedAt = Date.now()
  const prompt = join(root, name + '.prompt')
  const probe = join(root, name + '.py')
  writeFileSync(prompt, '{}\n', { mode: 0o600 })
  writeFileSync(probe, (process.platform === 'darwin' ? '#!/usr/bin/env python3\n' : '') + source, { mode: 0o700 })
  const token = 'wrapper-test-token-' + name
  const proof = statSync(prompt, { bigint: true })
  const digest = 'sha256:' + createHash('sha256').update(readFileSync(prompt)).digest('hex')
  const nonce = randomBytes(24).toString('hex')
  const timeoutMs = options.timeoutMs || 4000
  const child = spawn(python, [wrapper, '--token', token, '--prompt', prompt,
    '--prompt-sha256', digest,
    '--prompt-dev', String(proof.dev), '--prompt-ino', String(proof.ino), '--prompt-mode', String(proof.mode),
    '--prompt-nlink', String(proof.nlink), '--prompt-size', String(proof.size),
    '--prompt-mtime-ns', String(proof.mtimeNs), '--prompt-ctime-ns', String(proof.ctimeNs),
    '--timeout-ms', String(timeoutMs), '--', ...(options.command || (process.platform === 'darwin' ? [probe] : [python, probe]))], {
    cwd: root,
    detached: true,
    env: { ...process.env, SHALLOW_INTAKE_POSIX_CONTROL_FD: '3', SHALLOW_INTAKE_POSIX_CONTROL_NONCE: nonce },
    stdio: ['pipe', 'pipe', 'pipe', 'pipe']
  })
  let stdout = '', stderr = '', control = '', readyAt = null, drainedAt = null
  child.stdout.setEncoding('utf8'); child.stderr.setEncoding('utf8')
  child.stdout.on('data', (chunk) => { stdout += chunk })
  child.stderr.on('data', (chunk) => { stderr += chunk })
  child.stdio[3].setEncoding('utf8')
  let bindingHandled = false
  child.stdio[3].on('data', (chunk) => {
    control += chunk
    const ready = control.match(new RegExp(`(?:^|\\n)INTAKE_POSIX_MODEL_READY ${nonce} (\\d+)\\n`))
    if (ready && readyAt === null) readyAt = Date.now()
    if (drainedAt === null && new RegExp(`(?:^|\\n)INTAKE_POSIX_MODEL_DRAINED ${nonce} \\d+\\n`).test(control)) {
      drainedAt = Date.now()
    }
    if (ready && !bindingHandled && !child.stdin.destroyed) {
      bindingHandled = true
      if (options.beforeBinding) options.beforeBinding(Number(ready[1]))
      const ack = options.rejectBinding
        ? `BOUND ${token} ${nonce} ${Number(ready[1]) + 1}\n`
        : `BOUND ${token} ${nonce} ${ready[1]}\n`
      child.stdin.end(ack)
    }
  })
  child.stdin.write(`GO ${token}\n`)
  return await new Promise((resolveRun, rejectRun) => {
    const timer = setTimeout(() => {
      try { process.kill(-child.pid, 'SIGKILL') } catch {}
      rejectRun(new Error(`wrapper probe ${name} timed out`))
    }, Math.max(10000, timeoutMs + 6000))
    child.on('error', rejectRun)
    child.on('close', (code, signal) => {
      clearTimeout(timer)
      resolveRun({ code, signal, stdout, stderr, control, nonce, readyAt, drainedAt,
        durationMs: Date.now() - startedAt })
    })
  })
}

try {
  if (!['linux', 'darwin'].includes(process.platform)) {
    console.log('intake model wrapper: skipped on unsupported platform')
    process.exit(0)
  }

  const contract = await runProbe('contract', String.raw`
import ctypes, errno, json, os, signal, sys
result = {}
result['controlNonceVisible'] = 'SHALLOW_INTAKE_POSIX_CONTROL_NONCE' in os.environ
try:
    os.fstat(3)
    result['controlFd'] = 'open'
except OSError as error:
    result['controlFd'] = error.errno
try:
    child = os.fork()
    if child == 0:
        os._exit(0)
    os.waitpid(child, 0)
    result['fork'] = 'allowed'
except OSError as error:
    result['fork'] = error.errno
if sys.platform.startswith('linux'):
    try:
        os.setsid()
        result['setsid'] = 'allowed'
    except OSError as error:
        result['setsid'] = error.errno
    try:
        os.setpgid(0, 0)
        result['setpgid'] = 'allowed'
    except OSError as error:
        result['setpgid'] = error.errno
    libc = ctypes.CDLL(None, use_errno=True)
    death = ctypes.c_int(0)
    result['getPdeath'] = libc.prctl(2, ctypes.byref(death), 0, 0, 0)
    result['pdeath'] = death.value
    ctypes.set_errno(0)
    result['clearPdeath'] = libc.prctl(1, 0, 0, 0, 0)
    result['clearPdeathErrno'] = ctypes.get_errno()
print(json.dumps(result, sort_keys=True), flush=True)
`)
  assert.equal(contract.code, 0, contract.stderr)
  const verdict = JSON.parse(contract.stdout.trim())
  assert.equal(verdict.controlNonceVisible, false, 'the model must not inherit the control nonce')
  assert.equal(verdict.controlFd, 9, 'the model must not inherit the wrapper-only control descriptor')
  assert.equal(verdict.fork, 1, 'fork must fail with EPERM')
  if (process.platform === 'linux') {
    assert.equal(verdict.setsid, 1, 'setsid must fail with EPERM')
    assert.equal(verdict.setpgid, 1, 'setpgid must fail with EPERM')
    assert.equal(verdict.getPdeath, 0)
    assert.equal(verdict.pdeath, 9, 'the direct model must retain SIGKILL PDEATHSIG')
    assert.equal(verdict.clearPdeath, -1)
    assert.equal(verdict.clearPdeathErrno, 1, 'the model cannot clear PDEATHSIG')
  }
  console.log('ok 1 - POSIX model process cannot fork or weaken its containment contract')

  if (process.platform === 'darwin') {
    const execProbeSource = join(root, 'darwin-exec-allowlist.c')
    const execProbeBinary = join(root, 'darwin-exec-allowlist')
    writeFileSync(execProbeSource, String.raw`
#include <errno.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/wait.h>
#include <unistd.h>
extern char **environ;
static int run(int search_path, const char *path, char *const argv[]) {
  pid_t pid = 0;
  posix_spawn_file_actions_t actions;
  if (posix_spawn_file_actions_init(&actions) != 0) return 4000;
  if (posix_spawn_file_actions_addopen(&actions, STDOUT_FILENO, "/dev/null", O_WRONLY, 0) != 0 ||
      posix_spawn_file_actions_addopen(&actions, STDERR_FILENO, "/dev/null", O_WRONLY, 0) != 0) return 4001;
  int rc = search_path
    ? posix_spawnp(&pid, path, &actions, NULL, argv, environ)
    : posix_spawn(&pid, path, &actions, NULL, argv, environ);
  posix_spawn_file_actions_destroy(&actions);
  if (rc != 0) return 1000 + rc;
  int status = 0;
  if (waitpid(pid, &status, 0) < 0) return 2000 + errno;
  return WIFEXITED(status) ? WEXITSTATUS(status) : 3000 + (WIFSIGNALED(status) ? WTERMSIG(status) : 0);
}
int main(void) {
  char *security[] = { "security", "help", NULL };
  char *absolute_security[] = { "/usr/bin/security", "help", NULL };
  char *foreign[] = { "/usr/bin/id", NULL };
  int security_code = run(1, security[0], security);
  int absolute_security_code = run(0, absolute_security[0], absolute_security);
  int foreign_code = run(0, foreign[0], foreign);
  printf("{\"security\":%d,\"absoluteSecurity\":%d,\"foreign\":%d}\n",
         security_code, absolute_security_code, foreign_code);
  return security_code == 0 && absolute_security_code == 0 && foreign_code == 1001 ? 0 : 1;
}
`)
    const compiled = spawnSync(process.env.CC || 'cc', ['-O2', '-Wall', '-Wextra', '-o', execProbeBinary, execProbeSource], {
      encoding: 'utf8', timeout: 30000
    })
    assert.equal(compiled.status, 0, compiled.stderr || compiled.stdout || 'cannot compile Darwin containment probe')
    const execAllowlist = await runProbe('darwin-exec-allowlist', '', {
      command: [execProbeBinary]
    })
    assert.equal(execAllowlist.code, 0, execAllowlist.stderr || execAllowlist.stdout)
    assert.deepEqual(JSON.parse(execAllowlist.stdout.trim()),
      { security: 0, absoluteSecurity: 0, foreign: 1001 },
      'the native-model profile must permit only the fixed Keychain helper and deny foreign executables')
    assert.match(execAllowlist.control,
      new RegExp(`INTAKE_POSIX_MODEL_DRAINED ${execAllowlist.nonce} \\d+\\n`),
      'the wrapper must report drain only after the Keychain-helper process group is empty')
    const escapeProbeSource = join(root, 'darwin-process-group-escape.c')
    const escapeProbeBinary = join(root, 'darwin-process-group-escape')
    writeFileSync(escapeProbeSource, String.raw`
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>
int main(int argc, char **argv) {
  if (argc != 2) return 5;
  int ready[2];
  if (pipe(ready) != 0) return 6;
  pid_t child = fork();
  if (child < 0) return 2;
  if (child > 0) {
    close(ready[1]);
    char byte = 0;
    ssize_t size = read(ready[0], &byte, 1);
    close(ready[0]);
    return size == 1 && byte == 'R' ? 0 : 7;
  }
  close(ready[0]);
  if (setsid() < 0) _exit(3);
  int marker = open(argv[1], O_WRONLY | O_CREAT | O_EXCL, 0600);
  if (marker < 0 || dprintf(marker, "%d\n", getpid()) < 0 || fsync(marker) != 0) _exit(8);
  close(marker);
  if (write(ready[1], "R", 1) != 1) _exit(9);
  close(ready[1]);
  close(STDIN_FILENO);
  close(STDOUT_FILENO);
  close(STDERR_FILENO);
  struct timespec remaining = { .tv_sec = 0, .tv_nsec = 900000000L };
  while (nanosleep(&remaining, &remaining) < 0 && errno == EINTR) {}
  _exit(0);
}
`)
    const escapeCompiled = spawnSync(process.env.CC || 'cc',
      ['-O2', '-Wall', '-Wextra', '-o', escapeProbeBinary, escapeProbeSource],
      { encoding: 'utf8', timeout: 30000 })
    assert.equal(escapeCompiled.status, 0,
      escapeCompiled.stderr || escapeCompiled.stdout || 'cannot compile Darwin PGID escape probe')
    externalEscapeMarker = join(tmpdir(), 'intake-wrapper-escape-' + randomBytes(16).toString('hex') + '.pid')
    const escaped = await runProbe('darwin-process-group-escape', '',
      { command: [escapeProbeBinary, externalEscapeMarker] })
    assert.equal(escaped.code, 0, escaped.stderr)
    const escapedPid = Number(readFileSync(externalEscapeMarker, 'utf8').trim())
    assert.ok(Number.isInteger(escapedPid) && escapedPid > 0,
      'the child must prove successful setsid through its private side channel')
    assert.ok(escaped.readyAt && escaped.drainedAt && escaped.drainedAt - escaped.readyAt >= 750,
      `DRAINED was published before the exact escaped generation exited (${escaped.drainedAt - escaped.readyAt}ms)`)
    assert.throws(() => process.kill(escapedPid, 0), (error) => error && error.code === 'ESRCH',
      'the exact escaped generation must be gone when DRAINED is published')
    assert.match(escaped.control,
      new RegExp(`INTAKE_POSIX_MODEL_DRAINED ${escaped.nonce} \\d+\\n`),
      'global unique-executable inventory must retain a setsid-escaped generation until death')
    rmSync(externalEscapeMarker, { force: true })
    externalEscapeMarker = null

    if (process.env.ORCHESTRATOR_TEST_CLAUDE_KEYCHAIN === '1') {
      const realClaude = await runProbe('darwin-real-claude-keychain', '', {
        timeoutMs: 15000,
        command: [process.env.SHALLOW_INTAKE_CLAUDE || 'claude', '-p', '--safe-mode',
          '--no-session-persistence', '--disable-slash-commands', '--tools', '',
          '--system-prompt', 'Return only the exact text OK.', '--output-format', 'text', '--effort', 'low']
      })
      assert.equal(realClaude.code, 0, realClaude.stderr)
      assert.equal(realClaude.stdout.trim(), 'OK')
      assert.match(realClaude.control,
        new RegExp(`INTAKE_POSIX_MODEL_DRAINED ${realClaude.nonce} \\d+\\n`),
        'the pinned real Claude/Keychain generation must drain completely')
    }

    const selectedExecutable = join(root, 'darwin-selected-model')
    const replacementExecutable = join(root, 'darwin-replacement-model')
    writeFileSync(selectedExecutable, '#!/bin/sh\nprintf "trusted-pinned-model\\n"\n', { mode: 0o700 })
    writeFileSync(replacementExecutable, '#!/bin/sh\nprintf "attacker-replacement-model\\n"\n', { mode: 0o700 })
    const pinned = await runProbe('darwin-executable-pin', '', {
      command: [selectedExecutable],
      // READY is emitted after exact private copy publication while the child
      // is still held behind BOUND. Replacing the selected path here exercises
      // the former stat-to-sandbox-exec race deterministically.
      beforeBinding() { renameSync(replacementExecutable, selectedExecutable) }
    })
    assert.equal(pinned.code, 0, pinned.stderr)
    assert.equal(pinned.stdout, 'trusted-pinned-model\n')
    assert.match(readFileSync(selectedExecutable, 'utf8'), /attacker-replacement-model/)
    assert.equal(existsSync(join(root, '.model-executable')), false,
      'the exact private executable copy must be removed after containment drains')

    const occupiedPin = join(root, '.model-executable')
    writeFileSync(occupiedPin, 'pre-existing private evidence\n', { mode: 0o700 })
    const occupied = await runProbe('darwin-occupied-pin', '', { command: [selectedExecutable] })
    assert.equal(occupied.code, 74, occupied.stderr)
    assert.match(readFileSync(occupiedPin, 'utf8'), /pre-existing private evidence/,
      'a failed O_EXCL pin must never delete the pre-existing generation')
    rmSync(occupiedPin)

    const oversizedExecutable = join(root, 'darwin-oversized-model')
    writeFileSync(oversizedExecutable, '#!/bin/sh\n', { mode: 0o700 })
    truncateSync(oversizedExecutable, 512 * 1024 * 1024 + 1)
    const oversized = await runProbe('darwin-oversized-pin', '', { command: [oversizedExecutable] })
    assert.equal(oversized.code, 74, oversized.stderr)
    assert.doesNotMatch(oversized.control, /INTAKE_POSIX_MODEL_READY/,
      'an oversized executable must fail before any model generation is advertised')
    assert.equal(existsSync(join(root, '.model-executable')), false)
  }
  console.log('ok 2 - Darwin pins the model, limits exec to Keychain, and drains PGID-escaped generations (platform-skipped elsewhere)')

  if (process.platform === 'linux' && ['x64', 'x86_64'].includes(process.arch)) {
    const x32 = await runProbe('x32', String.raw`
import ctypes, os
os.write(1, b'armed\n')
libc = ctypes.CDLL(None, use_errno=True)
libc.syscall(0x40000038, 0, 0, 0, 0, 0)
os.write(1, b'survived\n')
`)
    assert.match(x32.stdout, /^armed\n/)
    assert.doesNotMatch(x32.stdout, /survived/)
    assert.notEqual(x32.code, 0, 'x32 syscall namespace must be killed by the architecture guard')
  }
  console.log('ok 3 - Linux x32 syscall namespace cannot bypass clone filtering')

  for (let index = 0; index < 12; index++) {
    const fast = await runProbe('fast-exit-' + index, `print('fast-${index}', flush=True)\n`)
    assert.equal(fast.code, 0, fast.stderr)
    assert.equal(fast.stdout, `fast-${index}\n`)
    assert.match(fast.control, new RegExp(`INTAKE_POSIX_MODEL_READY ${fast.nonce} \\d+\\n`))
    assert.match(fast.control, new RegExp(`INTAKE_POSIX_MODEL_DRAINED ${fast.nonce} \\d+\\n`))
  }
  console.log('ok 4 - pre-exec gate deterministically binds even immediate-exit model generations')

  const rejectedSideEffect = join(root, 'binding-rejected-side-effect.txt')
  const rejected = await runProbe('binding-rejected', `
from pathlib import Path
import signal, time
Path(${JSON.stringify(rejectedSideEffect)}).write_text('model-executed', encoding='utf-8')
signal.signal(signal.SIGTERM, lambda *_: None)
print('model-started', flush=True)
while True:
    time.sleep(0.1)
`, { rejectBinding: true })
  assert.match(rejected.control, new RegExp(`INTAKE_POSIX_MODEL_READY ${rejected.nonce} \\d+\\n`))
  assert.match(rejected.control, new RegExp(`INTAKE_POSIX_MODEL_DRAINED ${rejected.nonce} \\d+\\n`))
  assert.notEqual(rejected.code, 0, 'a non-matching durable-binding acknowledgement must terminate the model')
  assert.equal(existsSync(rejectedSideEffect), false, 'invalid BOUND must not execute any model side effect')
  assert.equal(rejected.stdout, '', 'invalid BOUND must not execute model stdout')
  console.log('ok 5 - invalid binding drains the gate with zero model side effects')
  console.log('\nintake model wrapper: 5 checks passed')
} finally {
  if (externalEscapeMarker) rmSync(externalEscapeMarker, { force: true })
  rmSync(root, { recursive: true, force: true })
}
