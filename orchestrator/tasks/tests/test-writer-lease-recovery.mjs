#!/usr/bin/env node

import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { existsSync, linkSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import writerLeases from '../writer-leases.cjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const MODULE = join(HERE, '..', 'writer-leases.cjs')
const ROOT = mkdtempSync(join(tmpdir(), 'writer-lease-wal-'))
const CHILD = String.raw`
const fs = require('fs')
const path = require('path')
const writer = require(process.argv[1])
const root = process.argv[2]
const dir = process.argv[3]
const operation = process.argv[4]
const liveChildPid = Number(process.env.WRITER_WAL_LIVE_CHILD_PID || 0)
const pendingChild = operation === 'child-binding'
const handle = writer.acquire(dir, {
  rootDir: root, kind: 'task-session', stem: 'TASK_71_wal_matrix', key: 'direct:wal-matrix',
  sessionId: writer.createSessionId(), ownerPid: process.pid,
  childPid: Number.isInteger(liveChildPid) && liveChildPid > 0 ? liveChildPid : null,
  pendingChild,
  ttlMs: 60000
})
fs.writeSync(1, JSON.stringify({
  dir: handle.dir, rootDir: handle.rootDir, path: handle.path,
  leaseId: handle.leaseId, token: handle.token, record: handle.record,
  before: fs.readFileSync(handle.path, 'base64')
}) + '\n')
if (operation === 'renewal') writer.renew(handle, 120000)
else if (operation === 'release') writer.release(handle)
else if (operation === 'child-binding') writer.updateChildPid(handle, process.pid)
else throw new Error('unknown operation')
`

let checks = 0
function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}
function produceCrash(operation, failpoint, index, extraEnv = {}) {
  const root = join(ROOT, `${String(index).padStart(2, '0')}-${operation}-${failpoint.replaceAll(':', '-')}`)
  const dir = join(root, 'finalizations', '.writers')
  mkdirSync(join(root, 'finalizations'), { recursive: true })
  const child = spawnSync(process.execPath, ['-e', CHILD, MODULE, root, dir, operation], {
    encoding: 'utf8', env: { ...process.env, WRITER_LEASE_FAILPOINT: failpoint, ...extraEnv }
  })
  assert.equal(child.signal, 'SIGKILL', `${failpoint}\n${child.stderr}${child.stdout}`)
  const firstLine = child.stdout.split('\n').find(Boolean)
  assert.ok(firstLine, `crashed ${operation} must publish its exact handle before mutation`)
  const handle = JSON.parse(firstLine)
  const before = Buffer.from(handle.before, 'base64')
  const beforeRecord = JSON.parse(before.toString('utf8'))
  const dirty = writerLeases.scan(dir, root)
  const artifacts = existsSync(dir) ? readdirSync(dir).filter((name) => name.startsWith(`.${handle.leaseId}.`) || name === `.${handle.leaseId}.mutation-lock`) : []
  return { root, dir, handle, before, beforeRecord, dirty, artifacts }
}
function settleCrash(operation, failpoint, index, extraEnv = {}) {
  const crashed = produceCrash(operation, failpoint, index, extraEnv)
  const { root, dir, handle } = crashed
  const recovery = writerLeases.reconcileStaleMutations(dir, root)
  assert.deepEqual(recovery.blocked, [], JSON.stringify(recovery.blocked))
  const clean = writerLeases.scan(dir, root)
  assert.deepEqual(clean.issues, [], JSON.stringify(clean.issues))
  assert.equal(readdirSync(dir).some((name) => name.startsWith(`.${handle.leaseId}.`) || name === `.${handle.leaseId}.mutation-lock`), false)
  return { ...crashed, recovery }
}

const renewalStages = [
  ['renewal:after-candidate-stage', 'old'],
  ['renewal:after-manifest-stage', 'old'],
  ['renewal:before-lock-publish', 'old'],
  ['renewal:after-lock-publish', 'old'],
  ['renewal:after-read', 'old'],
  ['renewal:before-detach', 'old'],
  ['renewal:after-detach', 'old'],
  ['renewal:before-publish', 'old'],
  ['renewal:after-publish', 'new'],
  ['renewal:before-publish-receipt', 'new'],
  ['renewal:after-receipt-stage', 'new'],
  ['renewal:after-publish-receipt', 'new'],
  ['renewal:before-recovery-unlink', 'new'],
  ['renewal:after-recovery-unlink', 'new'],
  ['renewal:after-receipt-unlink', 'new'],
  ['renewal:before-lock-unlink', 'new'],
  ['renewal:after-lock-unlink', 'new'],
]
const releaseStages = [
  ['release:before-unlink', 'old'],
  ['release:after-manifest-stage', 'old'],
  ['release:before-lock-publish', 'old'],
  ['release:after-lock-publish', 'old'],
  ['release:after-read', 'old'],
  ['release:before-detach', 'old'],
  ['release:after-detach', 'old'],
  ['release:before-publish-receipt', 'old'],
  ['release:after-receipt-stage', 'old'],
  ['release:after-publish-receipt', 'absent'],
  ['release:before-recovery-unlink', 'absent'],
  ['release:after-recovery-unlink', 'absent'],
  ['release:after-receipt-unlink', 'absent'],
  ['release:before-lock-unlink', 'absent'],
  ['release:after-lock-unlink', 'absent'],
]
// A dead child-binding transaction always rolls forward to the exact bound
// generation.  Rolling back to the old pending row would create a lease whose
// missing child identity can never be proven stale automatically.
const childBindingStages = renewalStages.map(([stage]) => [stage.replace(/^renewal:/, 'child binding:'), 'new'])

try {
  let index = 0
  check('public process identity state exposes exact generation outcomes and conservative PID liveness', () => {
    const startId = writerLeases.captureProcessStartId(process.pid)
    if (process.platform === 'linux' || process.platform === 'darwin') {
      assert.match(startId, writerLeases.PROCESS_START_ID_RE)
      assert.equal(writerLeases.processIdentityState(process.pid, startId), 'match')
      const last = startId.at(-1)
      const reused = startId.slice(0, -1) + (last === '0' ? '1' : '0')
      assert.equal(writerLeases.processIdentityState(process.pid, reused), 'reused')
      assert.equal(writerLeases.processIdentityState(process.pid, null), 'pid-live')
      assert.equal(writerLeases.processIdentityState(2147483647, startId), 'dead')
    } else {
      assert.equal(writerLeases.processIdentityState(process.pid, 'psid-v1:linux:' + '0'.repeat(64)), 'unsupported')
      assert.equal(writerLeases.processIdentityState(process.pid, null), 'pid-live')
    }
    assert.equal(writerLeases.processIdentityState(2147483648, startId), 'dead')
    assert.equal(writerLeases.processIdentityState(Number.MAX_SAFE_INTEGER, startId), 'dead')
    assert.equal(writerLeases.processIdentityMatches(process.pid, startId), true)
  })
  for (const [stage, expected] of renewalStages) {
    check(`renewal crash ${stage} deterministically settles ${expected}`, () => {
      const result = settleCrash('renewal', stage, ++index)
      assert.equal(existsSync(result.handle.path), true)
      const after = readFileSync(result.handle.path)
      if (expected === 'old') assert.deepEqual(after, result.before)
      else {
        const record = JSON.parse(after.toString('utf8'))
        assert.equal(record.token, result.beforeRecord.token)
        assert.notEqual(record.updatedAt, result.beforeRecord.updatedAt)
        assert.ok(Date.parse(record.expiresAt) > Date.parse(result.beforeRecord.expiresAt))
      }
      if (stage !== 'renewal:after-lock-unlink') assert.ok(result.dirty.issues.length > 0)
    })
  }
  for (const [stage, expected] of releaseStages) {
    check(`release crash ${stage} deterministically settles ${expected}`, () => {
      const result = settleCrash('release', stage, ++index)
      assert.equal(existsSync(result.handle.path), expected !== 'absent')
      if (expected === 'old') assert.deepEqual(readFileSync(result.handle.path), result.before)
      if (stage !== 'release:before-unlink' && stage !== 'release:after-lock-unlink') assert.ok(result.dirty.issues.length > 0)
    })
  }
  for (const [stage, expected] of childBindingStages) {
    check(`child binding crash ${stage} deterministically settles ${expected}`, () => {
      const result = settleCrash('child-binding', stage, ++index)
      assert.equal(existsSync(result.handle.path), true)
      const after = readFileSync(result.handle.path)
      if (expected === 'old') assert.deepEqual(after, result.before)
      else {
        const record = JSON.parse(after.toString('utf8'))
        assert.equal(record.unverified, false)
        assert.equal(record.childPid, result.beforeRecord.owner.pid)
        assert.match(record.childProcessStartId, writerLeases.PROCESS_START_ID_RE)
      }
      if (stage !== 'child binding:after-lock-unlink') assert.ok(result.dirty.issues.length > 0)
    })
  }

  check('a dead unpublished pending acquisition is aborted without creating an immortal spawn-gap lease', () => {
    const root = join(ROOT, `${String(++index).padStart(2, '0')}-acquire-after-candidate-stage`)
    const dir = join(root, 'finalizations', '.writers')
    mkdirSync(join(root, 'finalizations'), { recursive: true })
    const child = spawnSync(process.execPath, ['-e', CHILD, MODULE, root, dir, 'child-binding'], {
      encoding: 'utf8', env: { ...process.env, WRITER_LEASE_FAILPOINT: 'acquire:after-candidate-stage' }
    })
    assert.equal(child.signal, 'SIGKILL', child.stderr + child.stdout)
    const candidateName = readdirSync(dir).find((name) => name.endsWith('.lease-candidate'))
    assert.ok(candidateName)
    const pending = JSON.parse(readFileSync(join(dir, candidateName), 'utf8'))
    assert.equal(pending.childPid, null)
    assert.equal(pending.unverifiedReason, writerLeases.PENDING_CHILD_REASON)
    const recovery = writerLeases.reconcileStaleMutations(dir, root)
    assert.deepEqual(recovery.blocked, [], JSON.stringify(recovery.blocked))
    assert.equal(recovery.reconciled.length, 1)
    assert.equal(recovery.reconciled[0].state, 'orphan-candidate-aborted')
    assert.equal(existsSync(join(dir, `${pending.leaseId}.json`)), false)
    assert.deepEqual(readdirSync(dir), [])
    assert.equal(writerLeases.scan(dir, root).issues.length, 0)
  })

  check('orphan child binding consolidates known candidate aliases before synthesizing its WAL manifest', () => {
    const result = produceCrash('child-binding', 'child binding:after-candidate-stage', ++index)
    const candidateName = readdirSync(result.dir).find((name) => name.endsWith('.lease-candidate'))
    assert.ok(candidateName)
    const captureName = candidateName.replace(/\.lease-candidate$/, '.candidate.mutation-capture')
    linkSync(join(result.dir, candidateName), join(result.dir, captureName))
    const recovery = writerLeases.reconcileStaleMutations(result.dir, result.root)
    assert.deepEqual(recovery.blocked, [], JSON.stringify(recovery.blocked))
    assert.equal(recovery.reconciled.length, 1)
    const bound = JSON.parse(readFileSync(result.handle.path, 'utf8'))
    assert.equal(bound.unverified, false)
    assert.equal(bound.childPid, result.beforeRecord.owner.pid)
    assert.equal(writerLeases.scan(result.dir, result.root).issues.length, 0)
    assert.equal(readdirSync(result.dir).some((name) => name.startsWith(`.${result.handle.leaseId}.`) ||
      name === `.${result.handle.leaseId}.mutation-lock`), false)
  })

  function mutationLock(result) {
    const name = readdirSync(result.dir).find((entry) => entry === `.${result.handle.leaseId}.mutation-lock`)
    assert.ok(name, 'fixture must retain the canonical mutation manifest')
    return join(result.dir, name)
  }
  function rewriteManifest(result, mutate) {
    const file = mutationLock(result)
    const value = JSON.parse(readFileSync(file, 'utf8'))
    mutate(value)
    writeFileSync(file, JSON.stringify(value, null, 2) + '\n')
    return value
  }

  check('an exact live mutation owner blocks recovery, while PID-generation reuse permits deterministic settlement', () => {
    const result = produceCrash('renewal', 'renewal:after-lock-publish', ++index)
    const currentStart = writerLeases.captureProcessStartId(process.pid)
    rewriteManifest(result, (manifest) => {
      manifest.mutationOwner = { pid: process.pid, hostname: result.beforeRecord.owner.hostname, processStartId: currentStart }
    })
    let recovery = writerLeases.reconcileStaleMutations(result.dir, result.root)
    assert.equal(recovery.reconciled.length, 0)
    assert.equal(recovery.blocked.length, 1)
    assert.match(recovery.blocked[0].message, /may still be alive/)
    rewriteManifest(result, (manifest) => {
      manifest.mutationOwner.processStartId = currentStart.slice(0, -1) + (currentStart.endsWith('0') ? '1' : '0')
    })
    recovery = writerLeases.reconcileStaleMutations(result.dir, result.root)
    assert.deepEqual(recovery.blocked, [])
    assert.equal(writerLeases.scan(result.dir, result.root).issues.length, 0)
  })

  check('a remote mutation generation remains fail-closed and preserves every WAL artifact', () => {
    const result = produceCrash('renewal', 'renewal:after-lock-publish', ++index)
    rewriteManifest(result, (manifest) => { manifest.mutationOwner.hostname = 'remote.invalid.example' })
    const beforeNames = readdirSync(result.dir).sort()
    const recovery = writerLeases.reconcileStaleMutations(result.dir, result.root)
    assert.equal(recovery.reconciled.length, 0)
    assert.equal(recovery.blocked.length, 1)
    assert.match(recovery.blocked[0].message, /may still be alive/)
    assert.deepEqual(readdirSync(result.dir).sort(), beforeNames)
  })

  check('release recovery requires the exact bound child generation to be gone', () => {
    const result = produceCrash('release', 'release:after-detach', ++index, {
      WRITER_WAL_LIVE_CHILD_PID: String(process.pid)
    })
    const recovery = writerLeases.reconcileStaleMutations(result.dir, result.root)
    assert.equal(recovery.reconciled.length, 0)
    assert.equal(recovery.blocked.length, 1)
    assert.match(recovery.blocked[0].message, /child process generation may still be alive/)
    assert.equal(existsSync(result.handle.path), false, 'blocked recovery must not invent a release decision')
  })

  check('handle reconciliation authenticates the exact token before touching artifacts', () => {
    const result = produceCrash('renewal', 'renewal:after-lock-publish', ++index)
    const names = readdirSync(result.dir).sort()
    assert.throws(() => writerLeases.reconcile({ ...result.handle, token: 'f'.repeat(48) }), /token does not authenticate/)
    assert.deepEqual(readdirSync(result.dir).sort(), names)
    assert.deepEqual(writerLeases.reconcileStaleMutations(result.dir, result.root).blocked, [])
  })

  check('multiple operation generations for one lease are preserved and block reconciliation', () => {
    const result = produceCrash('renewal', 'renewal:after-lock-publish', ++index)
    const candidate = readdirSync(result.dir).find((name) => name.endsWith('.lease-candidate'))
    assert.ok(candidate)
    const foreign = `.${result.handle.leaseId}.${'b'.repeat(32)}.lease-candidate`
    writeFileSync(join(result.dir, foreign), readFileSync(join(result.dir, candidate)))
    let recovery = writerLeases.reconcileStaleMutations(result.dir, result.root)
    assert.equal(recovery.reconciled.length, 0)
    assert.equal(recovery.blocked.length, 1)
    assert.match(recovery.blocked[0].message, /unexpected or malformed artifact/)
    assert.equal(existsSync(join(result.dir, foreign)), true)
    rmSync(join(result.dir, foreign))
    recovery = writerLeases.reconcileStaleMutations(result.dir, result.root)
    assert.deepEqual(recovery.blocked, [])
  })

  check('a foreign canonical generation is never overwritten during detach recovery', () => {
    const result = produceCrash('renewal', 'renewal:after-detach', ++index)
    const foreign = { ...result.beforeRecord, token: 'e'.repeat(48), updatedAt: new Date().toISOString() }
    const foreignBytes = Buffer.from(JSON.stringify(foreign, null, 2) + '\n')
    writeFileSync(result.handle.path, foreignBytes)
    let recovery = writerLeases.reconcileStaleMutations(result.dir, result.root)
    assert.equal(recovery.reconciled.length, 0)
    assert.equal(recovery.blocked.length, 1)
    assert.match(recovery.blocked[0].message, /foreign generation/)
    assert.deepEqual(readFileSync(result.handle.path), foreignBytes)
    rmSync(result.handle.path)
    recovery = writerLeases.reconcileStaleMutations(result.dir, result.root)
    assert.deepEqual(recovery.blocked, [])
    assert.deepEqual(readFileSync(result.handle.path), result.before)
  })

  check('proof fields above Number.MAX_SAFE_INTEGER are compared as exact decimal strings', () => {
    const result = produceCrash('renewal', 'renewal:after-lock-publish', ++index)
    rewriteManifest(result, (manifest) => { manifest.oldProof.ino = '9007199254740993' })
    const recovery = writerLeases.reconcileStaleMutations(result.dir, result.root)
    assert.equal(recovery.reconciled.length, 0)
    assert.equal(recovery.blocked.length, 1)
    assert.match(recovery.blocked[0].message, /foreign generation|old generation/)
  })

  check('malformed and foreign mutation artifacts remain fail-closed without leaking their bytes', () => {
    const root = join(ROOT, 'adversarial-artifacts')
    const dir = join(root, 'finalizations', '.writers')
    mkdirSync(dir, { recursive: true })
    const leaseId = writerLeases.createLeaseId()
    const malformed = `.${leaseId}.not-an-operation.foreign`
    const secret = 'DO-NOT-EXPOSE-MUTATION-SECRET'
    writeFileSync(join(dir, malformed), secret)
    const scan = writerLeases.scan(dir, root)
    assert.ok(scan.issues.some((row) => row.code === 'WRITER_LEASE_ARTIFACT_UNSAFE'))
    assert.equal(JSON.stringify(scan).includes(secret), false)
    const recovery = writerLeases.reconcileStaleMutations(dir, root)
    assert.equal(recovery.reconciled.length, 0)
    assert.equal(recovery.blocked.length, 1)
    assert.equal(readFileSync(join(dir, malformed), 'utf8'), secret)
  })

  check('manifest paths cannot authenticate a self-consistent directory outside the pinned writer root', () => {
    const result = produceCrash('renewal', 'renewal:after-lock-publish', ++index)
    rewriteManifest(result, (manifest) => {
      const outside = join(result.root, 'outside')
      manifest.candidateName = join(outside, `.${manifest.leaseId}.${manifest.operationId}.lease-candidate`)
      manifest.recoveryName = join(outside, `.${manifest.leaseId}.${manifest.operationId}.lease-recovery`)
      manifest.publishedName = join(outside, `.${manifest.leaseId}.${manifest.operationId}.mutation-published`)
    })
    const recovery = writerLeases.reconcileStaleMutations(result.dir, result.root)
    assert.equal(recovery.reconciled.length, 0)
    assert.equal(recovery.blocked.length, 1)
    assert.match(recovery.blocked[0].message, /manifest is missing or malformed/)
  })

  console.log(`writer-lease recovery WAL: ${checks} checks passed`)
} finally {
  rmSync(ROOT, { recursive: true, force: true })
}
