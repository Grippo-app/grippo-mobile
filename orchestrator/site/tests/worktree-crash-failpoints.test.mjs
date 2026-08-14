#!/usr/bin/env node
// §21 crash recovery, the PHYSICAL half. The existing provisioning suite
// simulates a crash by hand-writing the state a crash would leave; that proves
// the resume logic reads such a state correctly, but it cannot prove a real
// process death actually produces it. These checks kill the provisioning
// process for real, at every point across the provisioning window, and assert
// the §9.2 trichotomy holds every single time:
//
//   resume-create      — the same generation is completed, same worktreeId
//   recovery-required  — a partial footprint is FLAGGED and never destroyed
//   proven-absent      — nothing was created; a fresh generation is minted
//
// and, in every case: the decision is made from physical evidence, never from
// the age of anything, and a second generation is never silently created for a
// stem that already owns one.

import assert from 'node:assert/strict'
import { execFileSync, spawn } from 'node:child_process'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const repoRoot = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..')
const recordContract = require('../../tasks/worktree-record-contract.cjs')

let checks = 0
function check(name, fn) { return fn().then(() => { checks++; console.log(`ok ${checks} - ${name}`) }) }
const roots = []
function git(cwd, ...args) {
  return execFileSync('git', args, { cwd, encoding: 'utf8', env: { ...process.env, GIT_TERMINAL_PROMPT: '0' } })
}

function fixture() {
  const parent = mkdtempSync(join(tmpdir(), 'wt-crash-'))
  roots.push(parent)
  const root = join(parent, 'control')
  mkdirSync(root)
  git(root, 'init', '-q', '-b', 'main')
  git(root, 'config', 'user.email', 'fixture@test.invalid')
  git(root, 'config', 'user.name', 'Fixture')
  git(root, 'config', 'commit.gpgsign', 'false')
  mkdirSync(join(root, 'orchestrator', 'tasks', 'todo'), { recursive: true })
  mkdirSync(join(root, 'orchestrator', 'skills'), { recursive: true })
  writeFileSync(join(root, 'orchestrator', 'tasks', 'todo', 'TASK_7_probe.md'), '# Task 7\n## Goal\nprobe\n')
  // Provisioning really installs skills into a checkout. A stub that wrote
  // nothing would shorten the window this test is aiming at, and would hide
  // the untracked-artifact handling entirely.
  writeFileSync(join(root, 'orchestrator', 'skills', 'install-skills.sh'),
    '#!/usr/bin/env bash\nmkdir -p "$1/.claude/skills/probe"\nprintf installed > "$1/.claude/skills/probe/SKILL.md"\n')
  mkdirSync(join(root, '.claude', 'skills', 'probe'), { recursive: true })
  writeFileSync(join(root, '.claude', 'skills', 'probe', 'SKILL.md'), 'installed')
  writeFileSync(join(root, '.gitignore'), 'orchestrator/.cache/\n')
  writeFileSync(join(root, 'keep.txt'), 'base\n')
  git(root, 'add', '.')
  git(root, 'commit', '-q', '-m', 'base')
  return { parent, root, home: join(parent, '.worktrees') }
}

function childEnv(fx) {
  return { ...process.env, ORCHESTRATOR_PROJECT_ROOT: fx.root, ORCHESTRATOR_WORKTREE_HOME: fx.home }
}

function raceSameStemProvision(fx) {
  const go = join(fx.parent, 'provision-go')
  function launch(label, suffix) {
    const marker = join(fx.parent, 'provision-ready-' + label)
    const script = `
      const fs = require('node:fs');
      const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
      const taskIntegrity = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-integrity.js'))});
      fs.writeFileSync(${JSON.stringify(marker)}, 'ready');
      const wait = setInterval(() => {
        if (!fs.existsSync(${JSON.stringify(go)})) return;
        clearInterval(wait);
        const out = manager.provision({ stem: 'TASK_7_probe',
          runId: '170000000000${suffix}-r${suffix}', requestId: '170000000000${suffix}-q${suffix}',
          sourceRevision: taskIntegrity.validateAction('run', 'TASK_7_probe', 'fixture').sourceRevision });
        process.stdout.write(JSON.stringify({ ok: out.ok === true, code: out.code || null,
          worktreeId: out.worktreeId || null, runId: out.runId || null }));
      }, 5);
    `
    const child = spawn(process.execPath, ['-e', script], {
      env: childEnv(fx), cwd: fx.root, stdio: ['ignore', 'pipe', 'pipe'],
    })
    let stdout = '', stderr = ''
    child.stdout.on('data', (chunk) => { stdout += chunk })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    return { marker, result: new Promise((resolve) => child.on('close', (code, signal) => {
      resolve({ code, signal, stderr, value: stdout ? JSON.parse(stdout) : null })
    })) }
  }
  const first = launch('first', 1)
  const second = launch('second', 2)
  return new Promise((resolve, reject) => {
    const deadline = Date.now() + 10000
    const timer = setInterval(() => {
      if (existsSync(first.marker) && existsSync(second.marker)) {
        clearInterval(timer)
        writeFileSync(go, 'go')
        Promise.all([first.result, second.result]).then(resolve, reject)
      } else if (Date.now() >= deadline) {
        clearInterval(timer)
        reject(new Error('concurrent provisioners did not reach the shared start boundary'))
      }
    }, 10)
  })
}

// Provision in a real child process and SIGKILL it after `delayMs`. Returns
// once the child is gone, whatever it managed to do.
function killDuringProvision(fx, delayMs) {
  const script = `
    const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
    const taskIntegrity = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-integrity.js'))});
    const out = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000001-r1', requestId: '1700000000001-q1',
      sourceRevision: taskIntegrity.validateAction('run', 'TASK_7_probe', 'fixture').sourceRevision });
    process.stdout.write(JSON.stringify({ ok: out.ok === true, worktreeId: out.worktreeId || null }));
  `
  return new Promise((resolve) => {
    const child = spawn(process.execPath, ['-e', script], { env: childEnv(fx), cwd: fx.root, stdio: ['ignore', 'pipe', 'pipe'] })
    let stdout = ''
    child.stdout.on('data', (b) => { stdout += b })
    const timer = setTimeout(() => { try { child.kill('SIGKILL') } catch (error) {} }, delayMs)
    child.on('close', (code, signal) => { clearTimeout(timer); resolve({ code, signal, stdout }) })
  })
}

// Everything a later process can observe about the stem, from a FRESH node so
// nothing is carried in memory.
function inspect(fx) {
  const script = `
    const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
    const taskIntegrity = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-integrity.js'))});
    // The RAW store, not activeRecordFor: the latter reports only materialized
    // generations, and a crashed create is 'create-intent' — invisible there,
    // which is exactly the distinction this test is about.
    const fs_ = require('node:fs'), P_ = require('node:path');
    const recDir = P_.join(${JSON.stringify(fx.root)}, 'orchestrator', '.cache', 'tasks', 'worktrees');
    let raw = [];
    try {
      raw = fs_.readdirSync(recDir).filter((n) => /^wt-[a-f0-9]{32}[.]json$/.test(n)).map((n) =>
        JSON.parse(fs_.readFileSync(P_.join(recDir, n), 'utf8')));
    } catch (error) { raw = []; }
    const before = manager.activeRecordFor('TASK_7_probe');
    const projection = manager.discover();
    // Crash retry is the same admitted queue generation. A different request
    // must never adopt an unfinished create-intent.
    const second = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000001-r1', requestId: '1700000000001-q1',
      sourceRevision: taskIntegrity.validateAction('run', 'TASK_7_probe', 'fixture').sourceRevision });
    const after = manager.activeRecordFor('TASK_7_probe');
    let rawAfter = [];
    try {
      rawAfter = fs_.readdirSync(recDir).filter((n) => /^wt-[a-f0-9]{32}[.]json$/.test(n)).map((n) =>
        JSON.parse(fs_.readFileSync(P_.join(recDir, n), 'utf8')));
    } catch (error) { rawAfter = []; }
    process.stdout.write(JSON.stringify({
      beforeOk: before.ok,
      rawCount: raw.length,
      rawStatus: raw.length ? raw[0].status : null,
      rawId: raw.length ? raw[0].worktreeId : null,
      beforeStatus: before.record ? before.record.status : null,
      beforeId: before.record ? before.record.worktreeId : null,
      blockers: projection.findings.filter((f) => f.severity === 'blocker').map((f) => f.code || f.message),
      secondOk: second.ok === true, secondCode: second.code || null,
      secondId: second.worktreeId || null,
      afterOk: after.ok, afterStatus: after.record ? after.record.status : null,
      afterId: after.record ? after.record.worktreeId : null,
      rawAfterStatuses: rawAfter.map((r) => r.status).sort(),
      rawAfterIds: rawAfter.map((r) => r.worktreeId).sort(),
    }));
  `
  const out = execFileSync(process.execPath, ['-e', script], {
    env: childEnv(fx), cwd: fx.root, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024,
  })
  return JSON.parse(out)
}

function recordCount(fx) {
  const dir = join(fx.root, 'orchestrator', '.cache', 'tasks', 'worktrees')
  try { return readdirSync(dir).filter((n) => /^wt-[a-f0-9]{32}[.]json$/.test(n)).length } catch (error) { return 0 }
}

function prepareSeal(fx) {
  const script = `
    const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
    const taskIntegrity = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-integrity.js'))});
    const fs_ = require('node:fs'), P_ = require('node:path');
    const made = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000001-r1', requestId: '1700000000001-q1',
      sourceRevision: taskIntegrity.validateAction('run', 'TASK_7_probe', 'fixture').sourceRevision });
    if (!made.ok) throw new Error(JSON.stringify(made));
    fs_.writeFileSync(P_.join(made.executionRoot, 'keep.txt'), 'candidate\\n');
    process.stdout.write(JSON.stringify(made));
  `
  return JSON.parse(execFileSync(process.execPath, ['-e', script], {
    env: childEnv(fx), cwd: fx.root, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024,
  }))
}

// Kill the real sealing owner immediately after one physical boundary. The
// interception wraps Node's spawn/fs primitive, not manager logic: the Git
// object/ref or receipt is genuinely durable before SIGKILL lands.
function killDuringSeal(fx, made, boundary) {
  const script = `
    const cp = require('node:child_process');
    const guards = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/file-guards.js'))});
    const originalSpawn = cp.spawnSync;
    const originalPublish = guards.publishNoClobberRegularFileUnder;
    const boundary = ${JSON.stringify(boundary)};
    cp.spawnSync = function (file, args, options) {
      const result = originalSpawn.call(this, file, args, options);
      if (file === 'git' && ((boundary === 'candidate-commit' && args[0] === 'commit-tree') ||
          (boundary === 'candidate-ref' && args[0] === 'update-ref' && args[1] === ${JSON.stringify(made.candidateRef)}))) {
        process.kill(process.pid, 'SIGKILL');
      }
      return result;
    };
    guards.publishNoClobberRegularFileUnder = function (root, directory, target) {
      const result = originalPublish.apply(this, arguments);
      if (boundary === 'candidate-receipt' &&
          target === require('node:path').join(${JSON.stringify(fx.root)},
            'orchestrator/.cache/tasks/worktrees/.receipts', ${JSON.stringify(made.worktreeId)} + '.json') &&
          result && result.ok) {
        process.kill(process.pid, 'SIGKILL');
      }
      return result;
    };
    const gateModule = ${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-checkpoints.js'))};
    require.cache[gateModule] = { id: gateModule, filename: gateModule, loaded: true,
      exports: { sealingGate: () => ({ ok: true, checkpoint: { checkpointId: 'cp-fixture' } }) } };
    const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
    manager.seal({ worktreeId: ${JSON.stringify(made.worktreeId)} });
  `
  return new Promise((resolve) => {
    const child = spawn(process.execPath, ['-e', script], {
      env: childEnv(fx), cwd: fx.root, stdio: ['ignore', 'pipe', 'pipe'],
    })
    child.on('close', (code, signal) => resolve({ code, signal }))
  })
}

function recoverSeal(fx, made, gateResult = { ok: true, checkpoint: { checkpointId: 'cp-fixture' } }, releaseBlocked = false) {
  const script = `
    const gateModule = ${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-checkpoints.js'))};
    require.cache[gateModule] = { id: gateModule, filename: gateModule, loaded: true,
      exports: { sealingGate: () => (${JSON.stringify(gateResult)}) } };
    const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
    const supported = typeof manager.recoverInterruptedSeals === 'function';
    const recovery = supported ? manager.recoverInterruptedSeals() : null;
    const current = manager.activeRecordFor('TASK_7_probe');
    const recordFile = require('node:path').join(${JSON.stringify(fx.root)},
      'orchestrator/.cache/tasks/worktrees', ${JSON.stringify(made.worktreeId)} + '.json');
    const rawStatus = JSON.parse(require('node:fs').readFileSync(recordFile, 'utf8')).status;
    const receipt = manager.candidateReceipt(${JSON.stringify(made.worktreeId)});
    let ref = null;
    try { ref = require('node:child_process').execFileSync('git', ['rev-parse', ${JSON.stringify(made.candidateRef)}], { cwd: ${JSON.stringify(fx.root)}, encoding: 'utf8' }).trim(); }
    catch (error) {}
    ${releaseBlocked ? "require('node:fs').writeFileSync(" + JSON.stringify(join(made.executionRoot, 'keep.txt')) + ", 'base\\n')" : ''};
    const released = ${releaseBlocked ? 'manager.release(' + JSON.stringify(made.worktreeId) + ')' : 'null'};
    process.stdout.write(JSON.stringify({ supported, recovery,
      status: current.record && current.record.status,
      rawStatus,
      receiptCommit: receipt && receipt.candidateCommit,
      receiptTree: receipt && receipt.candidateTree,
      ref, released,
    }));
  `
  return JSON.parse(execFileSync(process.execPath, ['-e', script], {
    env: childEnv(fx), cwd: fx.root, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024,
  }))
}

function prepareRelease(fx) {
  const script = `
    const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
    const taskIntegrity = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-integrity.js'))});
    const made = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000001-r1', requestId: '1700000000001-q1',
      sourceRevision: taskIntegrity.validateAction('run', 'TASK_7_probe', 'fixture').sourceRevision });
    if (!made.ok) throw new Error(JSON.stringify(made));
    process.stdout.write(JSON.stringify(made));
  `
  return JSON.parse(execFileSync(process.execPath, ['-e', script], {
    env: childEnv(fx), cwd: fx.root, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024,
  }))
}

function renumberRecordedVolume(fx, made) {
  const recordFile = join(fx.root, 'orchestrator', '.cache', 'tasks', 'worktrees', made.worktreeId + '.json')
  const record = JSON.parse(readFileSync(recordFile, 'utf8'))
  const remountedDev = String(BigInt(record.executionRoot.dev) + 1n)
  record.controlRoot.dev = remountedDev
  record.gitCommonDirIdentity.dev = remountedDev
  record.executionRoot.dev = remountedDev
  record.controlProjectId = recordContract.digest({
    path: record.gitCommonDirIdentity.path,
    dev: record.gitCommonDirIdentity.dev,
    ino: record.gitCommonDirIdentity.ino,
  })
  record.recordHash = recordContract.recordHash(record)
  writeFileSync(recordFile, JSON.stringify(record) + '\n')
}

function killDuringRelease(fx, made, boundary) {
  const script = `
    const cp = require('node:child_process');
    const guards = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/file-guards.js'))});
    const originalSpawn = cp.spawnSync;
    const originalCas = guards.compareAndSwapRegularFileUnder;
    const boundary = ${JSON.stringify(boundary)};
    cp.spawnSync = function (file, args, options) {
      const result = originalSpawn.call(this, file, args, options);
      if (file === 'git' && ((boundary === 'checkout-removed' && args[0] === 'worktree' && args[1] === 'remove') ||
          (boundary === 'ref-transaction' && args[0] === 'update-ref' && args[1] === '--stdin'))) {
        process.kill(process.pid, 'SIGKILL');
      }
      return result;
    };
    guards.compareAndSwapRegularFileUnder = function (root, dir, target, maxBytes, expected, replacement) {
      const result = originalCas.apply(this, arguments);
      let value = null;
      try { value = JSON.parse(Buffer.from(replacement).toString('utf8')); } catch (error) {}
      if (value && value.worktreeId === ${JSON.stringify(made.worktreeId)} &&
          ((boundary === 'release-claim' && value.status === 'releasing') ||
           (boundary === 'released-record' && value.status === 'released'))) {
        process.kill(process.pid, 'SIGKILL');
      }
      return result;
    };
    const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
    manager.release(${JSON.stringify(made.worktreeId)});
  `
  return new Promise((resolve) => {
    const child = spawn(process.execPath, ['-e', script], {
      env: childEnv(fx), cwd: fx.root, stdio: ['ignore', 'pipe', 'pipe'],
    })
    let stderr = ''
    child.stderr.on('data', (chunk) => { stderr += chunk })
    child.on('close', (code, signal) => resolve({ code, signal, stderr }))
  })
}

function recoverRelease(fx, made) {
  const script = `
    const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
    const fs_ = require('node:fs'), P_ = require('node:path'), cp_ = require('node:child_process');
    const recovery = manager.recoverInterruptedReleases();
    const again = manager.recoverInterruptedReleases();
    const recordFile = P_.join(${JSON.stringify(fx.root)},
      'orchestrator/.cache/tasks/worktrees', ${JSON.stringify(made.worktreeId)} + '.json');
    const record = JSON.parse(fs_.readFileSync(recordFile, 'utf8'));
    const readRef = (ref) => { try { return cp_.execFileSync('git', ['rev-parse', '-q', '--verify', ref],
      { cwd: ${JSON.stringify(fx.root)}, encoding: 'utf8' }).trim(); } catch (error) { return null; } };
    const markerRef = 'refs/orchestrator/releases/' + ${JSON.stringify(made.worktreeId)};
    process.stdout.write(JSON.stringify({ recovery, again, status: record.status,
      checkoutExists: fs_.existsSync(${JSON.stringify(made.executionRoot)}),
      candidate: readRef(${JSON.stringify(made.candidateRef)}), marker: readRef(markerRef) }));
  `
  return JSON.parse(execFileSync(process.execPath, ['-e', script], {
    env: childEnv(fx), cwd: fx.root, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024,
  }))
}

function concurrentSeal(fx, made) {
  const gateDir = join(fx.parent, 'seal-gate')
  mkdirSync(gateDir)
  const go = join(gateDir, 'go')
  function launch(label) {
    const marker = join(gateDir, label)
    const script = `
      const fs_ = require('node:fs');
      const guards = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/file-guards.js'))});
      const original = guards.compareAndSwapRegularFileUnder;
      guards.compareAndSwapRegularFileUnder = function (root, dir, target, maxBytes, expected, replacement, options) {
        let value = null;
        try { value = JSON.parse(Buffer.from(replacement).toString('utf8')); } catch (error) {}
        if (value && value.worktreeId === ${JSON.stringify(made.worktreeId)} && value.status === 'sealing') {
          fs_.writeFileSync(${JSON.stringify(marker)}, 'ready');
          const sleeper = new Int32Array(new SharedArrayBuffer(4));
          const deadline = Date.now() + 10000;
          while (!fs_.existsSync(${JSON.stringify(go)}) && Date.now() < deadline) Atomics.wait(sleeper, 0, 0, 10);
        }
        return original.apply(this, arguments);
      };
      const gateModule = ${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-checkpoints.js'))};
      require.cache[gateModule] = { id: gateModule, filename: gateModule, loaded: true,
        exports: { sealingGate: () => ({ ok: true, checkpoint: { checkpointId: 'cp-fixture' } }) } };
      const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
      const result = manager.seal({ worktreeId: ${JSON.stringify(made.worktreeId)} });
      process.stdout.write(JSON.stringify(result));
    `
    const child = spawn(process.execPath, ['-e', script], {
      env: childEnv(fx), cwd: fx.root, stdio: ['ignore', 'pipe', 'pipe'],
    })
    let stdout = '', stderr = ''
    child.stdout.on('data', (chunk) => { stdout += chunk })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    return { marker, result: new Promise((resolve) => child.on('close', (code, signal) => {
      resolve({ code, signal, stderr, value: stdout ? JSON.parse(stdout) : null })
    })) }
  }
  const first = launch('first')
  const second = launch('second')
  return new Promise((resolve, reject) => {
    const deadline = Date.now() + 10000
    const timer = setInterval(() => {
      if (existsSync(first.marker) && existsSync(second.marker)) {
        clearInterval(timer)
        writeFileSync(go, 'go')
        Promise.all([first.result, second.result]).then(resolve, reject)
      } else if (Date.now() >= deadline) {
        clearInterval(timer)
        reject(new Error('concurrent sealers did not reach the shared record CAS'))
      }
    }, 10)
  })
}

function concurrentRelease(fx, made) {
  const gateDir = join(fx.parent, 'release-gate')
  mkdirSync(gateDir)
  const go = join(gateDir, 'go')
  function launch(label) {
    const marker = join(gateDir, label)
    const script = `
      const fs_ = require('node:fs');
      const guards = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/file-guards.js'))});
      const original = guards.compareAndSwapRegularFileUnder;
      guards.compareAndSwapRegularFileUnder = function (root, dir, target, maxBytes, expected, replacement) {
        let value = null;
        try { value = JSON.parse(Buffer.from(replacement).toString('utf8')); } catch (error) {}
        if (value && value.worktreeId === ${JSON.stringify(made.worktreeId)} && value.status === 'releasing') {
          fs_.writeFileSync(${JSON.stringify(marker)}, 'ready');
          const sleeper = new Int32Array(new SharedArrayBuffer(4));
          const deadline = Date.now() + 10000;
          while (!fs_.existsSync(${JSON.stringify(go)}) && Date.now() < deadline) Atomics.wait(sleeper, 0, 0, 10);
        }
        return original.apply(this, arguments);
      };
      const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
      const result = manager.release(${JSON.stringify(made.worktreeId)});
      process.stdout.write(JSON.stringify(result));
    `
    const child = spawn(process.execPath, ['-e', script], {
      env: childEnv(fx), cwd: fx.root, stdio: ['ignore', 'pipe', 'pipe'],
    })
    let stdout = '', stderr = ''
    child.stdout.on('data', (chunk) => { stdout += chunk })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    return { marker, result: new Promise((resolve) => child.on('close', (code, signal) => {
      resolve({ code, signal, stderr, value: stdout ? JSON.parse(stdout) : null })
    })) }
  }
  const first = launch('first')
  const second = launch('second')
  return new Promise((resolve, reject) => {
    const deadline = Date.now() + 10000
    const timer = setInterval(() => {
      if (existsSync(first.marker) && existsSync(second.marker)) {
        clearInterval(timer)
        writeFileSync(go, 'go')
        Promise.all([first.result, second.result]).then(resolve, reject)
      } else if (Date.now() >= deadline) {
        clearInterval(timer)
        reject(new Error('concurrent releasers did not reach the shared record CAS'))
      }
    }, 10)
  })
}

try {
  await check('two processes racing one stem can materialize only one execution generation', async () => {
    const fx = fixture()
    const results = await raceSameStemProvision(fx)
    assert.ok(results.every((row) => row.code === 0),
      results.map((row) => row.stderr).join('\n'))
    const values = results.map((row) => row.value)
    const recordDir = join(fx.root, 'orchestrator', '.cache', 'tasks', 'worktrees')
    const records = readdirSync(recordDir).filter((name) => name.endsWith('.json'))
      .map((name) => JSON.parse(readFileSync(join(recordDir, name), 'utf8')))
      .filter((record) => record.stem === 'TASK_7_probe' && record.status !== 'released')
    assert.equal(records.length, 1, JSON.stringify(values))
    assert.equal(new Set(values.filter((value) => value.ok).map((value) => value.worktreeId)).size <= 1, true)
    assert.equal(git(fx.root, 'worktree', 'list', '--porcelain').split('\nworktree ').length - 1, 1,
      'exactly one linked execution checkout may be materialized')
  })

  await check('a real process death anywhere in the provisioning window lands in exactly one classified outcome', async () => {
    // The window is walked in small steps so the kill lands before the intent,
    // between the intent and `git worktree add`, mid-checkout, during the skill
    // install, and after the record is ready. Any of the three outcomes is
    // correct — an UNCLASSIFIED state is not.
    // The exact source-generation fence now brackets task-state validation
    // both before the intent and before physical creation. Walk a deliberately
    // broad window so slower cold machines still cross publication and Git
    // boundaries instead of killing only during module/admission startup.
    const delays = [100, 300, 450, 550, 650, 750, 850, 950, 1100, 1300, 1600]
    const seen = new Set()
    for (const delayMs of delays) {
      const fx = fixture()
      const killed = await killDuringProvision(fx, delayMs)
      const state = inspect(fx)

      assert.equal(state.beforeOk, true,
        `delay ${delayMs}ms: the ownership store must stay readable after a kill`)
      assert.deepEqual(state.blockers, [],
        `delay ${delayMs}ms: a crashed provisioning must leave no unclassifiable state, got ${JSON.stringify(state.blockers)}`)

      if (state.rawCount === 0) {
        // Proven absent: nothing was published, so a fresh generation is minted.
        assert.equal(state.secondOk, true, `delay ${delayMs}ms: a clean absence must provision`)
        seen.add('proven-absent')
      } else if (state.secondOk) {
        // Resume-create or an already-ready generation: the SAME generation is
        // completed. A second worktreeId here would mean two checkouts for one
        // stem, which is the whole failure this phase exists to prevent.
        assert.equal(state.secondId, state.rawId,
          `delay ${delayMs}ms: resume must complete the same generation, not mint a second`)
        seen.add(state.rawStatus === 'ready' ? 'ready-resume' : 'resume-create')
      } else {
        // Recovery: flagged, never destroyed, and the record still points at
        // whatever physically exists.
        assert.equal(state.secondCode, 'PROVISION_RECOVERY_REQUIRED',
          `delay ${delayMs}ms: an unresumable footprint must be recovery-required, got ${state.secondCode}`)
        // The record is FLAGGED on disk, and it is no longer a materialized
        // generation — so nothing downstream will bind a run to it.
        assert.ok(state.rawAfterStatuses.includes('recovery-required'),
          `delay ${delayMs}ms: the crashed generation must be flagged, got ${JSON.stringify(state.rawAfterStatuses)}`)
        assert.ok(state.rawAfterIds.includes(state.rawId),
          `delay ${delayMs}ms: the record keeps its identity, it is never replaced`)
        assert.equal(state.afterId, null,
          `delay ${delayMs}ms: a recovery-required generation is not a materialized one`)
        seen.add('recovery-required')
      }

      // Never two generations for one stem, whatever happened.
      assert.ok(recordCount(fx) <= 2,
        `delay ${delayMs}ms: at most the crashed generation and its successor may exist`)
      assert.ok(killed.signal === 'SIGKILL' || killed.code === 0,
        `delay ${delayMs}ms: the child either finished or was killed outright`)
    }
    // The walk must actually have crossed a boundary, or it proves nothing.
    assert.ok(seen.size >= 2,
      `the delay walk must exercise more than one outcome, saw ${JSON.stringify([...seen])}`)
  })

  await check('the classification never depends on how old the evidence is', async () => {
    // §9.2 forbids age-based cleanup. The same crashed state must classify
    // identically whether it happened a moment ago or long ago, so the record
    // and its footprint are back-dated by a year and re-read.
    const fx = fixture()
    await killDuringProvision(fx, 12)
    const first = inspect(fx)

    const dir = join(fx.root, 'orchestrator', '.cache', 'tasks', 'worktrees')
    const names = (() => { try { return readdirSync(dir).filter((n) => /^wt-[a-f0-9]{32}[.]json$/.test(n)) } catch (error) { return [] } })()
    if (!names.length) { assert.equal(first.beforeId, null); return }

    // A second fixture, killed at the same point, then aged.
    const aged = fixture()
    await killDuringProvision(aged, 12)
    const agedDir = join(aged.root, 'orchestrator', '.cache', 'tasks', 'worktrees')
    let agedNames = []
    try { agedNames = readdirSync(agedDir).filter((n) => /^wt-[a-f0-9]{32}[.]json$/.test(n)) } catch (error) { agedNames = [] }
    if (!agedNames.length) return   // the kill landed before anything was published
    for (const name of agedNames) {
      const file = join(agedDir, name)
      const record = JSON.parse(readFileSync(file, 'utf8'))
      const old = '2020-01-01T00:00:00.000Z'
      record.createdAt = old
      record.updatedAt = old
      record.owner.startedAt = old
      // The hash covers the record, so it is recomputed rather than forged.
      const contract = require('../../tasks/worktree-record-contract.cjs')
      record.recordHash = contract.recordHash(record)
      writeFileSync(file, JSON.stringify(record) + '\n')
    }
    const agedState = inspect(aged)
    assert.equal(agedState.secondOk, first.secondOk,
      'a year-old crash must classify exactly like a fresh one')
    assert.equal(agedState.secondCode, first.secondCode)
    assert.deepEqual(agedState.blockers, first.blockers)
  })

  await check('real deaths across candidate commit, ref and receipt publication resume the exact seal', async () => {
    for (const boundary of ['candidate-commit', 'candidate-ref', 'candidate-receipt']) {
      const fx = fixture()
      const made = prepareSeal(fx)
      const killed = await killDuringSeal(fx, made, boundary)
      assert.equal(killed.signal, 'SIGKILL', boundary + ': the sealing owner must die at the requested boundary')
      const recovered = recoverSeal(fx, made)
      assert.equal(recovered.supported, true, boundary + ': sealing needs an explicit recovery owner')
      assert.equal(recovered.recovery.ok, true, boundary + ': ' + JSON.stringify(recovered))
      assert.equal(recovered.status, 'ready-for-integration', boundary + ': the generation must leave sealing')
      assert.match(recovered.receiptCommit || '', /^[a-f0-9]{40}$/)
      assert.equal(recovered.ref, recovered.receiptCommit, boundary + ': ref and receipt must name one commit')
      assert.equal(git(fx.root, 'rev-parse', recovered.ref + '^{tree}').trim(), recovered.receiptTree,
        boundary + ': the receipt tree must be the physical commit tree')
    }
  })

  await check('a killed seal whose gate can no longer be proved becomes explicitly releasable', async () => {
    const fx = fixture()
    const made = prepareSeal(fx)
    const killed = await killDuringSeal(fx, made, 'candidate-receipt')
    assert.equal(killed.signal, 'SIGKILL')
    const recovered = recoverSeal(fx, made,
      { ok: false, code: 'SEAL_GATE_ABSENT', message: 'the ship checkpoint is absent' }, true)
    assert.equal(recovered.recovery.ok, false)
    assert.equal(recovered.recovery.blocked[0].code, 'SEAL_GATE_ABSENT')
    assert.equal(recovered.rawStatus, 'recovery-required',
      'a recovery refusal must not leave a dead sealing owner behind')
    assert.equal(recovered.released.ok, true,
      'explicit recovery evidence must remain safely releasable: ' + JSON.stringify(recovered.released))
  })

  await check('two sealers racing from the same ready record publish one coherent candidate', async () => {
    const fx = fixture()
    const made = prepareSeal(fx)
    const outcomes = await concurrentSeal(fx, made)
    assert.equal(outcomes.filter((row) => row.value && row.value.ok).length, 1, JSON.stringify(outcomes))
    assert.equal(outcomes.filter((row) => row.value && row.value.code === 'SEAL_RECORD_CONFLICT').length, 1,
      JSON.stringify(outcomes))
    const current = recoverSeal(fx, made)
    assert.equal(current.status, 'ready-for-integration')
    assert.equal(current.recovery.ok, true)
    assert.equal(current.ref, current.receiptCommit)
    assert.equal(git(fx.root, 'rev-parse', current.ref + '^{tree}').trim(), current.receiptTree)
  })

  await check('real deaths across every release boundary replay to one exact released state', async () => {
    for (const boundary of ['release-claim', 'checkout-removed', 'ref-transaction', 'released-record']) {
      const fx = fixture()
      const made = prepareRelease(fx)
      const base = git(fx.root, 'rev-parse', 'HEAD').trim()
      const killed = await killDuringRelease(fx, made, boundary)
      assert.equal(killed.signal, 'SIGKILL', boundary + ': ' + killed.stderr)
      const recovered = recoverRelease(fx, made)
      assert.equal(recovered.recovery.ok, true, boundary + ': ' + JSON.stringify(recovered))
      assert.equal(recovered.again.ok, true, boundary + ': replay must be idempotent')
      assert.deepEqual(recovered.again.recovered, [])
      assert.equal(recovered.status, 'released')
      assert.equal(recovered.checkoutExists, false)
      assert.equal(recovered.candidate, null)
      assert.equal(recovered.marker, base, 'the durable marker pins the exact deleted ref commit')
    }
  })

  await check('release recovery preserves same-commit ref ABA and a replacement path', async () => {
    const refFx = fixture()
    const refMade = prepareRelease(refFx)
    const refBase = git(refFx.root, 'rev-parse', 'HEAD').trim()
    assert.equal((await killDuringRelease(refFx, refMade, 'ref-transaction')).signal, 'SIGKILL')
    git(refFx.root, 'update-ref', refMade.candidateRef, refBase)
    const refRecovery = recoverRelease(refFx, refMade)
    assert.equal(refRecovery.recovery.ok, false)
    assert.equal(refRecovery.recovery.blocked[0].code, 'RELEASE_REF_REAPPEARED')
    assert.equal(refRecovery.candidate, refBase, 'a recreated same-commit ref is foreign and survives')
    assert.equal(refRecovery.marker, refBase)

    const pathFx = fixture()
    const pathMade = prepareRelease(pathFx)
    assert.equal((await killDuringRelease(pathFx, pathMade, 'checkout-removed')).signal, 'SIGKILL')
    mkdirSync(pathMade.executionRoot, { recursive: true })
    writeFileSync(join(pathMade.executionRoot, 'foreign.txt'), 'must survive\n')
    const pathRecovery = recoverRelease(pathFx, pathMade)
    assert.equal(pathRecovery.recovery.ok, false)
    assert.equal(pathRecovery.recovery.blocked[0].code, 'RELEASE_PATH_REPLACED')
    assert.equal(readFileSync(join(pathMade.executionRoot, 'foreign.txt'), 'utf8'), 'must survive\n')
    assert.equal(pathRecovery.candidate, git(pathFx.root, 'rev-parse', 'HEAD').trim(),
      'the candidate ref is untouched when the recorded path was replaced')
  })

  await check('release accepts only a coherent filesystem-device remount with the full Git binding', async () => {
    const fx = fixture()
    const made = prepareRelease(fx)
    renumberRecordedVolume(fx, made)
    const script = `
      const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
      process.stdout.write(JSON.stringify(manager.release(${JSON.stringify(made.worktreeId)})));
    `
    const released = JSON.parse(execFileSync(process.execPath, ['-e', script], {
      env: childEnv(fx), cwd: fx.root, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024,
    }))
    assert.equal(released.ok, true, JSON.stringify(released))
    assert.equal(existsSync(made.executionRoot), false)
    assert.throws(() => git(fx.root, 'rev-parse', '-q', '--verify', made.candidateRef))
    const record = JSON.parse(readFileSync(join(fx.root, 'orchestrator', '.cache', 'tasks',
      'worktrees', made.worktreeId + '.json'), 'utf8'))
    assert.equal(record.status, 'released')
  })

  await check('two release owners racing on one generation perform cleanup once', async () => {
    const fx = fixture()
    const made = prepareRelease(fx)
    const outcomes = await concurrentRelease(fx, made)
    assert.equal(outcomes.filter((row) => row.value && row.value.ok).length, 1, JSON.stringify(outcomes))
    assert.equal(outcomes.filter((row) => row.value && row.value.code === 'RELEASE_RECORD_CONFLICT').length, 1,
      JSON.stringify(outcomes))
    const current = recoverRelease(fx, made)
    assert.equal(current.status, 'released')
    assert.equal(current.checkoutExists, false)
    assert.equal(current.candidate, null)
    assert.match(current.marker || '', /^[a-f0-9]{40}$/)
  })

  console.log(`\nworktree-crash-failpoints: ${checks} checks passed`)
} finally {
  for (const root of roots) { try { rmSync(root, { recursive: true, force: true }) } catch (error) {} }
}
