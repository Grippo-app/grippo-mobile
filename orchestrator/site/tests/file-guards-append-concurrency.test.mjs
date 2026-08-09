#!/usr/bin/env node

import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import {
  chmodSync, existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync,
  rmSync, writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const childProcess = require('node:child_process')
const originalSpawnSync = childProcess.spawnSync
const FILE_GUARDS = fileURLToPath(new URL('../server/file-guards.js', import.meta.url))
const guards = require(FILE_GUARDS)
const base = mkdtempSync(join(tmpdir(), 'file-guard-append-concurrency-'))
const root = join(base, 'project')
const directory = join(root, 'runtime')
mkdirSync(directory, { recursive: true, mode: 0o700 })
chmodSync(directory, 0o700)

let checks = 0
function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

try {
  check('multiprocess precheck race commits at most one append against the same remaining-byte budget', () => {
    const target = join(directory, 'bounded.log')
    const initial = Buffer.alloc(96, 0x69)
    writeFileSync(target, initial, { mode: 0o600 })
    chmodSync(target, 0o600)

    const childSource = String.raw`
const cp = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const [guardsPath, root, directory, target, rendezvous, id, payload] = process.argv.slice(1);
const guards = require(guardsPath);
const original = cp.spawnSync;
cp.spawnSync = function(command, args, options) {
  let request = null;
  try { request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null; } catch {}
  if (request && request.action === 'append-bounded') {
    request.testPauseAfterAppendReadMs = 750;
    options = { ...options, input: JSON.stringify(request) };
  }
  return original.call(cp, command, args, options);
};
fs.writeFileSync(path.join(rendezvous, 'ready-' + id), 'ready');
const wait = new Int32Array(new SharedArrayBuffer(4));
while (!fs.existsSync(path.join(rendezvous, 'start'))) Atomics.wait(wait, 0, 0, 2);
const result = guards.appendBoundedRegularFileUnder(root, directory, target, Buffer.from(payload, 'hex'), {
  create: false, mode: 0o600, maxBytes: 128, maxAppendBytes: 32,
});
process.stdout.write(JSON.stringify(result));
`
    const coordinator = String.raw`
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const [guardsPath, root, directory, target, rendezvous, childSourceBase64] = process.argv.slice(1);
const childSource = Buffer.from(childSourceBase64, 'base64').toString('utf8');
fs.mkdirSync(rendezvous, { recursive: true });
function child(id, payload) {
  return spawn(process.execPath, ['-e', childSource, guardsPath, root, directory, target,
    rendezvous, String(id), payload.toString('hex')], {
    env: { ...process.env, ORCHESTRATOR_FILE_GUARD_TEST_MODE: '1' },
    stdio: ['ignore', 'pipe', 'pipe'],
  });
}
function collect(child) {
  return new Promise((resolve, reject) => {
    let stdout = '', stderr = '';
    child.stdout.on('data', (chunk) => { stdout += chunk; });
    child.stderr.on('data', (chunk) => { stderr += chunk; });
    child.on('error', reject);
    child.on('exit', (code, signal) => {
      if (code !== 0) return reject(new Error('child failed: ' + code + '/' + signal + ': ' + stderr + stdout));
      try { resolve(JSON.parse(stdout)); } catch (error) { reject(new Error(stderr + stdout)); }
    });
  });
}
async function main() {
  const payloads = [Buffer.alloc(32, 0x61), Buffer.alloc(32, 0x62)];
  const children = payloads.map((payload, index) => child(index, payload));
  const wait = new Int32Array(new SharedArrayBuffer(4));
  const deadline = Date.now() + 5000;
  while ((!fs.existsSync(path.join(rendezvous, 'ready-0')) ||
      !fs.existsSync(path.join(rendezvous, 'ready-1'))) && Date.now() < deadline) {
    Atomics.wait(wait, 0, 0, 2);
  }
  assert.ok(fs.existsSync(path.join(rendezvous, 'ready-0')) &&
    fs.existsSync(path.join(rendezvous, 'ready-1')), 'children did not reach the barrier');
  fs.writeFileSync(path.join(rendezvous, 'start'), 'start');
  const results = await Promise.all(children.map(collect));
  const successes = results.map((result, index) => ({ result, index }))
    .filter((item) => item.result.ok);
  assert.equal(successes.length, 1, JSON.stringify(results));
  const bytes = fs.readFileSync(target);
  assert.equal(bytes.length, 128);
  assert.deepEqual(bytes.subarray(0, 96), Buffer.alloc(96, 0x69));
  assert.deepEqual(bytes.subarray(96), payloads[successes[0].index]);
  assert.equal(fs.readdirSync(directory).some((name) => name.startsWith('.guard-')), false);
}
main().catch((error) => { console.error(error.stack || error); process.exitCode = 1; });
`
    const rendezvous = join(base, 'rendezvous')
    const result = spawnSync(process.execPath, ['-e', coordinator, FILE_GUARDS,
      root, directory, target, rendezvous, Buffer.from(childSource).toString('base64')], {
      encoding: 'utf8', timeout: 20_000,
      env: { ...process.env, ORCHESTRATOR_FILE_GUARD_TEST_MODE: '1' },
    })
    assert.equal(result.status, 0, result.stderr + result.stdout)
  })

  check('unsupported directory flush fails closed before bounded append mutates the target', () => {
    const isolatedRoot = join(base, 'unsupported-project')
    const isolatedDirectory = join(isolatedRoot, 'runtime')
    const target = join(isolatedDirectory, 'events.log')
    mkdirSync(isolatedDirectory, { recursive: true, mode: 0o700 })
    writeFileSync(target, 'before\n', { mode: 0o600 })
    chmodSync(target, 0o600)
    const script = `
const guards = require(process.argv[1]);
const [root, directory, target] = process.argv.slice(2);
const synced = guards.fsyncDirectoryUnder(root, directory);
const appended = guards.appendBoundedRegularFileUnder(root, directory, target, 'forbidden\\n', {
  create: false, mode: 0o600, maxBytes: 4096, maxAppendBytes: 64,
});
process.stdout.write(JSON.stringify({ synced, appended }));
`
    const result = spawnSync(process.execPath, ['-e', script, FILE_GUARDS,
      isolatedRoot, isolatedDirectory, target], {
      encoding: 'utf8', timeout: 10_000,
      env: {
        ...process.env,
        ORCHESTRATOR_FILE_GUARD_TEST_MODE: '1',
        ORCHESTRATOR_FILE_GUARD_TEST_FORCE_DIRECTORY_FSYNC_UNSUPPORTED: '1',
      },
    })
    assert.equal(result.status, 0, result.stderr + result.stdout)
    const response = JSON.parse(result.stdout)
    assert.equal(response.synced, false)
    assert.equal(response.appended.ok, false)
    assert.equal(response.appended.code, 'wal-publish-unsynced')
    assert.equal(readFileSync(target, 'utf8'), 'before\n')
    assert.equal(readdirSync(isolatedDirectory).some((name) => name.startsWith('.guard-')), true,
      'the unsynced private stage is retained as non-commit evidence')
  })

  check('oversize append never mutates an exact full target', () => {
    const target = join(directory, 'already-full.log')
    writeFileSync(target, Buffer.alloc(128, 0x66), { mode: 0o600 })
    const script = `
const guards = require(process.argv[1]);
const [root, directory, target] = process.argv.slice(2);
process.stdout.write(JSON.stringify(guards.appendBoundedRegularFileUnder(
  root, directory, target, 'x', { create: false, maxBytes: 128, maxAppendBytes: 1 })));
`
    const result = spawnSync(process.execPath, ['-e', script, FILE_GUARDS,
      root, directory, target], { encoding: 'utf8', timeout: 10_000 })
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assert.equal(JSON.parse(result.stdout).ok, false)
    assert.deepEqual(readFileSync(target), Buffer.alloc(128, 0x66))
    assert.equal(readdirSync(directory).some((name) => name.startsWith('.guard-')), false)
  })

  check('expected-generation and expected-missing append preconditions fail without adopting a replacement', () => {
    const target = join(directory, 'expected.log')
    writeFileSync(target, 'owned\n', { mode: 0o600 })
    chmodSync(target, 0o600)
    const proof = guards.statRegularFileUnder(root, directory, target)
    assert.ok(proof)
    rmSync(target)
    writeFileSync(target, 'foreign\n', { mode: 0o600 })
    const stale = guards.appendBoundedRegularFileUnder(root, directory, target, 'forbidden\n', {
      create: false, maxBytes: 4096, maxAppendBytes: 64, expectedProof: proof,
    })
    assert.equal(stale.ok, false)
    assert.equal(stale.code, 'expected-changed')
    assert.equal(readFileSync(target, 'utf8'), 'foreign\n')

    const missing = guards.appendBoundedRegularFileUnder(root, directory, target, 'forbidden\n', {
      create: false, maxBytes: 4096, maxAppendBytes: 64, expectMissing: true,
    })
    assert.equal(missing.ok, false)
    assert.equal(missing.code, 'expected-missing')
    assert.equal(readFileSync(target, 'utf8'), 'foreign\n')
    assert.equal(readdirSync(directory).some((name) => name.startsWith('.guard-')), false)
  })

  check('append WAL supports the one-megabyte event cap without widening public publication', () => {
    const target = join(directory, 'large-events.log')
    const before = Buffer.alloc(600 * 1024, 0x65)
    const line = Buffer.alloc(2048, 0x6c)
    writeFileSync(target, before, { mode: 0o600 })
    chmodSync(target, 0o600)
    const appended = guards.appendBoundedRegularFileUnder(root, directory, target, line, {
      create: false, mode: 0o600, maxBytes: 1024 * 1024, maxAppendBytes: 2048,
    })
    assert.equal(appended.ok, true, JSON.stringify(appended))
    const after = readFileSync(target)
    assert.equal(after.length, before.length + line.length)
    assert.deepEqual(after.subarray(0, before.length), before)
    assert.deepEqual(after.subarray(before.length), line)

    const publicTarget = join(directory, 'oversize-publication.log')
    const publicResult = guards.publishNoClobberRegularFileUnder(
      root, directory, publicTarget, Buffer.alloc(512 * 1024 + 1), {
        maxBytes: 1024 * 1024, mode: 0o600,
      })
    assert.equal(publicResult.ok, false)
    assert.equal(publicResult.code, 'too-large')
    assert.equal(existsSync(publicTarget), false)
    assert.equal(readdirSync(directory).some((name) => name.startsWith('.guard-')), false)
  })

  check('same-token response retry recognizes the terminal receipt and never appends twice', () => {
    const target = join(directory, 'response-loss.log')
    writeFileSync(target, 'before\n', { mode: 0o600 })
    chmodSync(target, 0o600)
    let token = null
    let dropAck = true
    childProcess.spawnSync = function (command, args, options) {
      let request = null
      try { request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null }
      catch {}
      if (request && request.action === 'append-bounded') {
        if (token === null) token = request.publicationToken
        else {
          request.publicationToken = token
          options = { ...options, input: JSON.stringify(request) }
        }
      }
      if (request && request.action === 'append-ack' && dropAck) {
        return { status: 1, signal: null, stdout: '', stderr: 'simulated response loss' }
      }
      return originalSpawnSync.call(childProcess, command, args, options)
    }
    try {
      const first = guards.appendBoundedRegularFileUnder(root, directory, target, 'once\n', {
        create: false, mode: 0o600, maxBytes: 4096, maxAppendBytes: 64,
      })
      assert.equal(first.ok, true, JSON.stringify(first))
      assert.equal(first.cleanupPending, true)
      assert.match(token, /^[a-f0-9]{32}$/)
      assert.equal(readFileSync(target, 'utf8'), 'before\nonce\n')
      assert.equal(readdirSync(directory).some((name) => name.endsWith('.receipt.json')), true)

      dropAck = false
      const retried = guards.appendBoundedRegularFileUnder(root, directory, target, 'once\n', {
        create: false, mode: 0o600, maxBytes: 4096, maxAppendBytes: 64,
      })
      assert.equal(retried.ok, true, JSON.stringify(retried))
      assert.equal(readFileSync(target, 'utf8'), 'before\nonce\n')
      assert.equal(readdirSync(directory).some((name) => name.startsWith('.guard-')), false)
    } finally {
      childProcess.spawnSync = originalSpawnSync
    }
  })

  console.log(`file-guards append concurrency: ${checks} checks passed`)
} finally {
  childProcess.spawnSync = originalSpawnSync
  rmSync(base, { recursive: true, force: true })
}
