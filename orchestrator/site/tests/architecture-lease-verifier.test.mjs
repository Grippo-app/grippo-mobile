import assert from 'node:assert/strict'
import { mkdtempSync, realpathSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const ROOT = resolve(dirname(new URL(import.meta.url).pathname), '..', '..', '..')
const writerLeases = require('../../tasks/writer-leases.cjs')
const verifier = require('../../tasks/architecture-lease-verify.cjs')
const fixture = realpathSync(mkdtempSync(join(tmpdir(), 'architecture-lease-')))
const directory = join(fixture, 'finalizations', '.writers')
let handle

try {
  handle = writerLeases.acquire(directory, {
    kind: 'architecture-generate',
    key: 'architecture-generate',
    ownerPid: process.pid,
    childPid: process.pid,
    ttlMs: 60_000,
    rootDir: fixture,
  })
  const environment = {
    ...process.env,
    ORCHESTRATOR_ARCHITECTURE_LEASE_ID: handle.leaseId,
    ORCHESTRATOR_ARCHITECTURE_LEASE_TOKEN: handle.token,
    ORCHESTRATOR_ARCHITECTURE_WRITER_DIR: directory,
    ORCHESTRATOR_ARCHITECTURE_WRITER_AUTHORITY: fixture,
  }
  const proof = verifier.verify(environment, process.pid, Date.now())
  assert.equal(proof.verified, true)
  assert.equal(proof.leaseId, handle.leaseId)

  assert.throws(() => verifier.verify({
    ...environment,
    ORCHESTRATOR_ARCHITECTURE_LEASE_TOKEN: '0'.repeat(48),
  }, process.pid, Date.now()), /ownership was lost/i)

  const cli = spawnSync(process.execPath, [
    join(ROOT, 'orchestrator', 'tasks', 'architecture-lease-verify.cjs'),
  ], { env: environment, encoding: 'utf8' })
  assert.equal(cli.status, 0, cli.stderr)
  assert.equal(JSON.parse(cli.stdout).verified, true)

  const wrongChild = spawnSync(process.execPath, [
    join(ROOT, 'orchestrator', 'tasks', 'architecture-lease-verify.cjs'),
  ], {
    env: {
      ...environment,
      ORCHESTRATOR_ARCHITECTURE_LEASE_TOKEN: 'f'.repeat(48),
    },
    encoding: 'utf8',
  })
  assert.equal(wrongChild.status, 2)
  assert.doesNotMatch(wrongChild.stderr, new RegExp(handle.token))

  console.log('architecture-lease-verifier.test.mjs: OK')
} finally {
  if (handle) {
    try { writerLeases.release(handle) } catch {}
  }
  rmSync(fixture, { recursive: true, force: true })
}
