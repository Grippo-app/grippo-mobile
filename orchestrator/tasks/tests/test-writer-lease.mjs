#!/usr/bin/env node

import assert from 'node:assert/strict'
import { spawn, spawnSync } from 'node:child_process'
import { mkdtempSync, mkdirSync, readFileSync, readdirSync, renameSync, rmSync, symlinkSync, unlinkSync, writeFileSync } from 'node:fs'
import { hostname, tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import writerLeases from '../writer-leases.cjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const CLI = join(HERE, '..', 'writer-lease.mjs')
const root = mkdtempSync(join(tmpdir(), 'writer-lease-cli-'))
const state = join(root, 'finalizations')
const writers = join(state, '.writers')
mkdirSync(state, { recursive: true })

let checks = 0
function check(name, fn) {
  fn()
  checks++
  console.log(`PASS ${name}`)
}
function run(args, extraEnv = {}) {
  return spawnSync(process.execPath, [CLI, ...args], {
    encoding: 'utf8',
    env: {
      ...process.env, FINALIZE_STATE_DIR: state, FINALIZE_PROJECT_ROOT: root,
      ORCHESTRATOR_WRITER_DELEGATION_TOKEN: '', ...extraEnv,
    },
  })
}
function acquireArgs() {
  return [
    'acquire', '--kind', 'task-session', '--stem', 'TASK_1_direct_guard',
    '--key', 'direct:run', '--owner-pid', String(process.pid), '--ttl-ms', '60000',
    '--guard-finalization',
  ]
}
function assertGuardRefused(result, code) {
  assert.equal(result.status, 2, result.stderr + result.stdout)
  assert.equal(result.stdout, '', 'a refused guard must not publish JSON success')
  assert.match(result.stderr, new RegExp(code))
  assert.equal(writerLeases.scan(writers).active.length, 0, 'a refused guard must withdraw its lease')
}
function releaseLease(row) {
  const result = run(['release', '--lease-id', row.leaseId, '--token', row.token])
  assert.equal(result.status, 0, result.stderr + result.stdout)
  assert.equal(writerLeases.scan(writers).active.length, 0)
}
function writerSnapshot() {
  try { return readdirSync(writers).sort().map((name) => [name, readFileSync(join(writers, name), 'utf8')]) }
  catch (error) { if (error && error.code === 'ENOENT') return []; throw error }
}
function mutex(overrides = {}) {
  return {
    version: 1,
    pid: process.pid,
    processStartId: writerLeases.captureProcessStartId(process.pid),
    hostname: hostname(),
    invocationId: 'fin-cli-guard-test',
    startedAt: new Date().toISOString(),
    released: false,
    ...overrides,
  }
}

try {
  check('guard publishes first and returns an owned lease when finalization is quiescent', function () {
    const result = run(acquireArgs())
    assert.equal(result.status, 0, result.stderr + result.stdout)
    const row = JSON.parse(result.stdout)
    assert.match(row.leaseId, /^wr-/)
    assert.match(row.sessionId, /^ws-/)
    assert.equal(writerLeases.scan(writers).active.length, 1)
    releaseLease(row)
  })

  check('guarded CLI processes exclude same-task and deterministic publication writers', function () {
    const first = run([
      'acquire', '--guard-finalization', '--kind', 'task-session',
      '--stem', 'TASK_17_cli_owner', '--key', 'direct:run',
      '--owner-pid', String(process.pid), '--ttl-ms', '60000',
    ])
    assert.equal(first.status, 0, first.stderr + first.stdout)
    const owner = JSON.parse(first.stdout)

    let refused = run([
      'acquire', '--guard-finalization', '--kind', 'task-session',
      '--stem', 'TASK_17_cli_owner', '--key', 'standby:run',
      '--owner-pid', String(process.pid), '--ttl-ms', '60000',
    ])
    assert.equal(refused.status, 2, refused.stderr + refused.stdout)
    assert.equal(refused.stdout, '')
    assert.match(refused.stderr, /WORKSPACE_WRITER_ACTIVE/)
    assert.deepEqual(writerLeases.scan(writers).active.map((row) => row.leaseId), [owner.leaseId],
      'losing CLI process must withdraw only its own lease')

    // Frozen serial safety (pipeline improvement 01, Phase 0): board-task
    // writers are mutually exclusive across stems and drainers, so a guarded
    // cross-stem task-session acquire loses the handshake too.
    const parallel = run([
      'acquire', '--guard-finalization', '--kind', 'task-session',
      '--stem', 'TASK_18_cli_parallel', '--key', 'standby:run',
      '--owner-pid', String(process.pid), '--ttl-ms', '60000',
    ])
    assert.equal(parallel.status, 2, parallel.stderr + parallel.stdout)
    assert.equal(parallel.stdout, '')
    assert.match(parallel.stderr, /WORKSPACE_WRITER_ACTIVE/)
    assert.deepEqual(writerLeases.scan(writers).active.map((row) => row.leaseId), [owner.leaseId],
      'a cross-stem board-task writer must withdraw only its own lease')

    refused = run([
      'acquire', '--guard-finalization', '--kind', 'standby-writer',
      '--key', 'task:create-backlog', '--owner-pid', String(process.pid), '--ttl-ms', '60000',
    ])
    assert.equal(refused.status, 2, refused.stderr + refused.stdout)
    assert.equal(refused.stdout, '')
    assert.match(refused.stderr, /WORKSPACE_WRITER_ACTIVE/)
    assert.equal(writerLeases.scan(writers).active.length, 1,
      'a global publisher that loses the post-publication scan must withdraw')

    {
      const released = run(['release', '--lease-id', owner.leaseId, '--token', owner.token])
      assert.equal(released.status, 0, released.stderr + released.stdout)
    }
    const publisher = run([
      'acquire', '--guard-finalization', '--kind', 'standby-writer',
      '--key', 'task:recover-backlog-edits', '--owner-pid', String(process.pid), '--ttl-ms', '60000',
    ])
    assert.equal(publisher.status, 0, publisher.stderr + publisher.stdout)
    const publicationOwner = JSON.parse(publisher.stdout)
    refused = run([
      'acquire', '--guard-finalization', '--kind', 'task-session',
      '--stem', 'TASK_19_after_publisher', '--key', 'standby:prep',
      '--owner-pid', String(process.pid), '--ttl-ms', '60000',
    ])
    assert.equal(refused.status, 2, refused.stderr + refused.stdout)
    assert.equal(refused.stdout, '')
    assert.match(refused.stderr, /WORKSPACE_WRITER_ACTIVE/)
    assert.deepEqual(writerLeases.scan(writers).active.map((row) => row.leaseId), [publicationOwner.leaseId])
    releaseLease(publicationOwner)
  })

  check('nested create publishes a global guard bound to one exact parent receipt', function () {
    const stem = 'TASK_23_nested_parent'
    const sessionId = writerLeases.createSessionId()
    const parent = writerLeases.acquire(writers, {
      kind: 'task-session', stem, key: 'standby:prep', sessionId,
      ownerPid: process.pid, ttlMs: 60000,
    })
    const base = [
      'acquire-publication-guard', '--key', 'task:create-backlog',
      '--owner-pid', String(process.pid), '--parent-stem', stem,
      '--parent-session-id', sessionId, '--parent-lease-id', parent.leaseId,
      '--parent-token', parent.token, '--creation-key-hash', `sha256:${'a'.repeat(64)}`,
    ]

    const wrong = base.slice()
    wrong[wrong.indexOf('--parent-token') + 1] = '0'.repeat(48)
    let result = run(wrong)
    assert.equal(result.status, 2, result.stderr + result.stdout)
    assert.equal(result.stdout, '')
    assert.match(result.stderr, /PUBLICATION_PARENT_AUTHORITY_INVALID/)
    assert.deepEqual(writerLeases.scan(writers).active.map((row) => row.leaseId), [parent.leaseId],
      'a mismatched parent receipt must withdraw only the supplemental guard')

    const detachedOwner = base.slice()
    detachedOwner[detachedOwner.indexOf('--owner-pid') + 1] = '2147483647'
    result = run(detachedOwner)
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.equal(result.stdout, '')
    assert.match(result.stderr, /matching create\/recovery publication scope/)
    assert.deepEqual(writerLeases.scan(writers).active.map((row) => row.leaseId), [parent.leaseId],
      'the supplemental guard must be bound to the invoking creator process')

    const preexistingForeign = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_26_foreign_before_guard', key: 'standby:foreign-before',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, ttlMs: 60000,
    })
    result = run(base)
    assert.equal(result.status, 2, result.stderr + result.stdout)
    assert.equal(result.stdout, '')
    assert.match(result.stderr, /WORKSPACE_WRITER_ACTIVE/)
    assert.deepEqual(new Set(writerLeases.scan(writers).active.map((row) => row.leaseId)),
      new Set([parent.leaseId, preexistingForeign.leaseId]),
      'a nested guard that loses to a pre-existing writer must withdraw only itself')
    writerLeases.release(preexistingForeign)

    result = run(base)
    assert.equal(result.status, 0, result.stderr + result.stdout)
    const guard = JSON.parse(result.stdout)
    assert.deepEqual({
      kind: guard.kind, stem: guard.stem, sessionId: guard.sessionId,
      key: guard.key, expiresAt: guard.expiresAt, parentLeaseId: guard.parentLeaseId,
      parentStem: guard.parentStem, parentSessionId: guard.parentSessionId,
    }, {
      kind: 'lock-writer', stem: null, sessionId,
      key: 'task:create-backlog', expiresAt: null, parentLeaseId: parent.leaseId,
      parentStem: stem, parentSessionId: sessionId,
    })
    let active = writerLeases.scan(writers).active
    assert.equal(active.length, 2)
    assert.ok(active.some((row) => row.leaseId === guard.leaseId && row.key === 'task:create-backlog'))

    const foreign = run([
      'acquire', '--guard-finalization', '--kind', 'task-session',
      '--stem', 'TASK_24_foreign_after_scan', '--key', 'standby:foreign',
      '--owner-pid', String(process.pid), '--ttl-ms', '60000',
    ])
    assert.equal(foreign.status, 2, foreign.stderr + foreign.stdout)
    assert.equal(foreign.stdout, '')
    assert.match(foreign.stderr, /WORKSPACE_WRITER_ACTIVE/)
    assert.deepEqual(new Set(writerLeases.scan(writers).active.map((row) => row.leaseId)),
      new Set([parent.leaseId, guard.leaseId]),
      'a later different-stem writer must see the global guard and withdraw')

    let released = run(['release', '--lease-id', guard.leaseId, '--token', guard.token])
    assert.equal(released.status, 0, released.stderr + released.stdout)
    // Frozen serial safety: the parent is itself a board-task writer, so it
    // must clear before a different-stem task-session acquire can win.
    released = run(['release', '--lease-id', parent.leaseId, '--token', parent.token])
    assert.equal(released.status, 0, released.stderr + released.stdout)
    const after = run([
      'acquire', '--guard-finalization', '--kind', 'task-session',
      '--stem', 'TASK_24_foreign_after_scan', '--key', 'standby:foreign',
      '--owner-pid', String(process.pid), '--ttl-ms', '60000',
    ])
    assert.equal(after.status, 0, after.stderr + after.stdout)
    const afterRow = JSON.parse(after.stdout)
    released = run(['release', '--lease-id', afterRow.leaseId, '--token', afterRow.token])
    assert.equal(released.status, 0, released.stderr + released.stdout)

    const siteSessionId = writerLeases.createSessionId()
    const siteParent = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_25_site_parent', key: 'task:TASK_25_site_parent',
      sessionId: siteSessionId, ownerPid: process.pid, pendingChild: true,
    })
    writerLeases.updateChildPid(siteParent, process.pid)
    assert.match(siteParent.delegationToken, /^[a-f0-9]{48}$/)
    assert.equal(siteParent.record.delegationToken, undefined)
    assert.equal(readFileSync(siteParent.path, 'utf8').includes(siteParent.delegationToken), false,
      'the site capability plaintext must exist only in the returned handle')
    result = run([
      'acquire-publication-guard', '--key', 'task:create-backlog',
      '--owner-pid', String(process.pid), '--parent-stem', 'TASK_25_site_parent',
      '--parent-session-id', siteSessionId, '--creation-key-hash', `sha256:${'b'.repeat(64)}`,
    ])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    assert.equal(result.stdout, '')
    assert.match(result.stderr, /exact parent receipt/)
    assert.deepEqual(writerLeases.scan(writers).active.map((row) => row.leaseId), [siteParent.leaseId],
      'public stem/sessionId alone must fail before a supplemental guard is published')
    result = run([
      'acquire-publication-guard', '--key', 'task:create-backlog',
      '--owner-pid', String(process.pid), '--parent-stem', 'TASK_25_site_parent',
      '--parent-session-id', siteSessionId, '--parent-lease-id', siteParent.leaseId,
      '--creation-key-hash', `sha256:${'b'.repeat(64)}`,
    ], { ORCHESTRATOR_WRITER_DELEGATION_TOKEN: siteParent.delegationToken })
    assert.equal(result.status, 0, result.stderr + result.stdout)
    const siteGuard = JSON.parse(result.stdout)
    assert.equal(siteGuard.parentLeaseId, siteParent.leaseId)
    assert.equal(siteGuard.delegationKind, 'site-process-tree')
    assert.equal(siteGuard.callerPid, process.pid)
    assert.equal(siteGuard.authorityPid, process.pid)
    assert.match(siteGuard.delegationToken, /^[a-f0-9]{48}$/)
    assert.match(siteGuard.callerProcessStartId, writerLeases.PROCESS_START_ID_RE)
    assert.equal(siteGuard.authorityProcessStartId, siteParent.record.childProcessStartId)
    const siteRows = writerLeases.scan(writers).active
    const persistedGuard = siteRows.find((row) => row.leaseId === siteGuard.leaseId)
    assert.equal(persistedGuard.owner.processStartId, siteGuard.callerProcessStartId)
    assert.match(siteParent.record.owner.processStartId, writerLeases.PROCESS_START_ID_RE)
    assert.match(siteParent.record.childProcessStartId, writerLeases.PROCESS_START_ID_RE)
    for (const row of [siteGuard, siteParent]) {
      released = run(['release', '--lease-id', row.leaseId, '--token', row.token])
      assert.equal(released.status, 0, released.stderr + released.stdout)
    }

    const sibling = spawn(process.execPath, ['-e', 'setInterval(() => {}, 1000)'], { stdio: 'ignore' })
    const copiedSessionId = writerLeases.createSessionId()
    const siblingAuthority = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_27_sibling_parent', key: 'task:TASK_27_sibling_parent',
      sessionId: copiedSessionId, ownerPid: process.pid, pendingChild: true,
    })
    writerLeases.updateChildPid(siblingAuthority, sibling.pid)
    try {
      result = run([
        'acquire-publication-guard', '--key', 'task:create-backlog',
        '--owner-pid', String(process.pid), '--parent-stem', 'TASK_27_sibling_parent',
        '--parent-session-id', copiedSessionId, '--parent-lease-id', siblingAuthority.leaseId,
        '--creation-key-hash', `sha256:${'c'.repeat(64)}`,
      ], { ORCHESTRATOR_WRITER_DELEGATION_TOKEN: siblingAuthority.delegationToken })
      assert.equal(result.status, 2, result.stderr + result.stdout)
      assert.equal(result.stdout, '')
      assert.match(result.stderr, /PUBLICATION_PARENT_AUTHORITY_INVALID/)
      assert.deepEqual(writerLeases.scan(writers).active.map((row) => row.leaseId), [siblingAuthority.leaseId],
        'an unrelated sibling with copied stem/sessionId must not retain a publication guard')
    } finally {
      sibling.kill('SIGKILL')
      writerLeases.release(siblingAuthority)
    }
  })

  check('inherited session verification requires one exact active verified lease and never mutates it', function () {
    const sessionId = writerLeases.createSessionId()
    const handle = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_8_inherited', key: 'task:TASK_8_inherited',
      sessionId, ownerPid: process.pid, childPid: process.pid,
    })
    const before = writerSnapshot()
    const result = run([
      'verify-session', '--guard-finalization', '--session-id', sessionId,
      '--stem', 'TASK_8_inherited',
    ])
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assert.deepEqual(JSON.parse(result.stdout), {
      verified: true, sessionId, stem: 'TASK_8_inherited', leaseId: handle.leaseId,
    })
    assert.deepEqual(writerSnapshot(), before, 'verification must be read-only')

    const duplicate = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_8_inherited', key: 'task:TASK_8_inherited',
      sessionId, ownerPid: process.pid, childPid: process.pid,
    })
    const duplicateSnapshot = writerSnapshot()
    const ambiguous = run([
      'verify-session', '--session-id', sessionId, '--stem', 'TASK_8_inherited',
    ])
    assert.equal(ambiguous.status, 2, ambiguous.stderr + ambiguous.stdout)
    assert.equal(ambiguous.stdout, '')
    assert.match(ambiguous.stderr, /expected one active attached site lease, found 2/)
    assert.deepEqual(writerSnapshot(), duplicateSnapshot, 'ambiguous credentials must fail without mutation')
    writerLeases.release(duplicate)
    assert.deepEqual(writerSnapshot(), before)

    for (const args of [
      ['verify-session', '--session-id', writerLeases.createSessionId(), '--stem', 'TASK_8_inherited'],
      ['verify-session', '--session-id', sessionId, '--stem', 'TASK_9_wrong_stem'],
    ]) {
      const refused = run(args)
      assert.equal(refused.status, 2, refused.stderr + refused.stdout)
      assert.equal(refused.stdout, '')
      assert.match(refused.stderr, /expected one active attached site lease, found 0/)
      assert.deepEqual(writerSnapshot(), before, 'a stale/mismatched inherited credential must not mutate leases')
    }
    writerLeases.release(handle)
  })

  check('inherited session verification excludes foreign active writers for the same stem or key', function () {
    const stem = 'TASK_14_exclusive'
    const sessionId = writerLeases.createSessionId()
    const own = writerLeases.acquire(writers, {
      kind: 'task-session', stem, key: `task:${stem}`,
      sessionId, ownerPid: process.pid, childPid: process.pid,
    })

    const sameStem = writerLeases.acquire(writers, {
      kind: 'task-session', stem, key: 'direct:foreign-same-stem',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, childPid: process.pid,
    })
    let before = writerSnapshot()
    let result = run(['verify-session', '--session-id', sessionId, '--stem', stem])
    assert.equal(result.status, 2, result.stderr + result.stdout)
    assert.equal(result.stdout, '')
    assert.match(result.stderr, /another active writer owns TASK_14_exclusive/)
    assert.deepEqual(writerSnapshot(), before, 'same-stem conflict detection must be read-only')
    writerLeases.release(sameStem)

    const sameKey = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_15_foreign_key', key: `task:${stem}`,
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, childPid: process.pid,
    })
    before = writerSnapshot()
    result = run(['verify-session', '--session-id', sessionId, '--stem', stem])
    assert.equal(result.status, 2, result.stderr + result.stdout)
    assert.equal(result.stdout, '')
    assert.match(result.stderr, /another active writer owns TASK_14_exclusive/)
    assert.deepEqual(writerSnapshot(), before, 'same-key conflict detection must be read-only')
    writerLeases.release(sameKey)

    const parallel = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_16_parallel', key: 'task:TASK_16_parallel',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, childPid: process.pid,
    })
    before = writerSnapshot()
    result = run(['verify-session', '--session-id', sessionId, '--stem', stem])
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assert.deepEqual(JSON.parse(result.stdout), { verified: true, sessionId, stem, leaseId: own.leaseId })
    assert.deepEqual(writerSnapshot(), before,
      'verify-session checks OWN authority read-only; admission-time cross-stem exclusion is the acquire guards\' job')
    writerLeases.release(parallel)
    writerLeases.release(own)
  })

  check('inherited session verification rejects unverified ownership and an active finalization guard', function () {
    const sessionId = writerLeases.createSessionId()
    const pending = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_10_unverified', key: 'task:TASK_10_unverified',
      sessionId, ownerPid: process.pid, pendingChild: true,
    })
    let before = writerSnapshot()
    let result = run(['verify-session', '--session-id', sessionId, '--stem', 'TASK_10_unverified'])
    assert.equal(result.status, 2, result.stderr + result.stdout)
    assert.equal(result.stdout, '')
    assert.match(result.stderr, /expected one active attached site lease, found 0/)
    assert.deepEqual(writerSnapshot(), before)
    writerLeases.release(pending)

    const verified = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_11_guarded', key: 'task:TASK_11_guarded',
      sessionId, ownerPid: process.pid, childPid: process.pid,
    })
    writeFileSync(join(state, 'TASK_11_recovery.json'), '{}\n')
    before = writerSnapshot()
    result = run([
      'verify-session', '--guard-finalization', '--session-id', sessionId,
      '--stem', 'TASK_11_guarded',
    ])
    assert.equal(result.status, 2, result.stderr + result.stdout)
    assert.equal(result.stdout, '')
    assert.match(result.stderr, /FINALIZATION_MARKER_ACTIVE/)
    assert.deepEqual(writerSnapshot(), before, 'guard refusal must not withdraw or rewrite a site-owned lease')
    unlinkSync(join(state, 'TASK_11_recovery.json'))
    writerLeases.release(verified)
  })

  check('inherited verification refuses active bounded or unattached direct-shaped leases', function () {
    const sessionId = writerLeases.createSessionId()
    for (const fixture of [
      { stem: 'TASK_12_bounded', ttlMs: 60000, childPid: process.pid },
      { stem: 'TASK_13_unattached' },
    ]) {
      const handle = writerLeases.acquire(writers, {
        kind: 'task-session', stem: fixture.stem, key: `task:${fixture.stem}`,
        sessionId, ownerPid: process.pid, childPid: fixture.childPid, ttlMs: fixture.ttlMs,
      })
      const before = writerSnapshot()
      const result = run(['verify-session', '--session-id', sessionId, '--stem', fixture.stem])
      assert.equal(result.status, 2, result.stderr + result.stdout)
      assert.equal(result.stdout, '')
      assert.match(result.stderr, /expected one active attached site lease, found 0/)
      assert.deepEqual(writerSnapshot(), before, 'refusal must not mutate the direct-shaped lease')
      writerLeases.release(handle)
    }
  })

  check('spawn-to-attach crash window stays active until PID binding is durable', function () {
    const pending = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_3_pending_child', key: 'task:TASK_3_pending_child',
      sessionId: writerLeases.createSessionId(), ownerPid: 2147483647, pendingChild: true,
    })
    let rows = writerLeases.scan(writers).active
    assert.equal(rows.length, 1, 'dead owner plus null child must still block during the spawn/attach gap')
    assert.equal(rows[0].unverified, true)
    assert.equal(rows[0].unverifiedReason, writerLeases.PENDING_CHILD_REASON)
    writerLeases.updateChildPid(pending, process.pid)
    rows = writerLeases.scan(writers).active
    assert.equal(rows.length, 1)
    assert.equal(rows[0].childPid, process.pid)
    assert.equal(rows[0].unverified, false, 'PID binding and pending clearance must land atomically')
    writerLeases.release(pending)

    const retained = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_4_retained_tree', key: 'task:TASK_4_retained_tree',
      sessionId: writerLeases.createSessionId(), ownerPid: 2147483647, pendingChild: true,
    })
    writerLeases.markUnverified(retained, 'fixture non-pending tree proof failure')
    assert.throws(() => writerLeases.updateChildPid(retained, process.pid), /non-pending unverified/)
    writerLeases.release(retained)

    if (process.platform !== 'win32') {
      const reconciled = writerLeases.acquire(writers, {
        kind: 'task-session', stem: 'TASK_5_reaped_tree', key: 'task:TASK_5_reaped_tree',
        sessionId: writerLeases.createSessionId(), ownerPid: 2147483647,
        childPid: 2147483646, pendingChild: true,
      })
      writerLeases.markUnverified(reconciled, 'fixture interrupted cancel with a bound detached PGID')
      const scan = writerLeases.scan(writers)
      assert.ok(scan.stale.some((row) => row.leaseId === reconciled.leaseId), 'proven-dead local owner/leader/PGID should reconcile automatically after restart')
      assert.ok(!scan.active.some((row) => row.leaseId === reconciled.leaseId))
      writerLeases.release(reconciled)
    }
  })

  check('process-start identities reject simulated owner, child, and guard PID reuse', function () {
    if (process.platform !== 'linux' && process.platform !== 'darwin') return
    const replacementId = `psid-v1:${process.platform}:${'0'.repeat(64)}`

    const guard = writerLeases.acquire(writers, {
      kind: 'lock-writer', stem: null, key: 'task:create-backlog',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid,
    })
    let record = JSON.parse(readFileSync(guard.path, 'utf8'))
    assert.notEqual(record.owner.processStartId, replacementId)
    record.owner.processStartId = replacementId
    writeFileSync(guard.path, JSON.stringify(record, null, 2) + '\n')
    let scan = writerLeases.scan(writers)
    assert.ok(scan.stale.some((row) => row.leaseId === guard.leaseId),
      'a live reused guard-owner PID must not resurrect the old guard generation')
    writerLeases.release(guard)

    const child = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_28_reused_child', key: 'task:TASK_28_reused_child',
      sessionId: writerLeases.createSessionId(), ownerPid: 2147483647, childPid: process.pid,
    })
    record = JSON.parse(readFileSync(child.path, 'utf8'))
    record.childProcessStartId = replacementId
    writeFileSync(child.path, JSON.stringify(record, null, 2) + '\n')
    scan = writerLeases.scan(writers)
    assert.ok(scan.stale.some((row) => row.leaseId === child.leaseId),
      'a live reused child PID must not resurrect the old site-child generation')
    writerLeases.release(child)

    const site = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_29_reused_authority', key: 'task:TASK_29_reused_authority',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, childPid: process.pid,
    })
    record = JSON.parse(readFileSync(site.path, 'utf8'))
    record.childProcessStartId = replacementId
    writeFileSync(site.path, JSON.stringify(record, null, 2) + '\n')
    assert.ok(writerLeases.scan(writers).active.some((row) => row.leaseId === site.leaseId),
      'the still-live controller may retain the row for cleanup, but not child authority')
    const refused = run([
      'verify-session', '--session-id', site.record.sessionId, '--stem', site.record.stem,
    ])
    assert.equal(refused.status, 2, refused.stderr + refused.stdout)
    assert.match(refused.stderr, /expected one active attached site lease, found 0/)
    writerLeases.release(site)

    const incomplete = writerLeases.acquire(writers, {
      kind: 'lock-writer', stem: null, key: 'task:recover-backlog-creations',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid,
    })
    record = JSON.parse(readFileSync(incomplete.path, 'utf8'))
    delete record.owner.processStartId
    writeFileSync(incomplete.path, JSON.stringify(record, null, 2) + '\n')
    const incompleteScan = writerLeases.scan(writers)
    assert.ok(incompleteScan.issues.some((row) => row.leaseId === incomplete.leaseId),
      'an incomplete owner shape is rejected by the exact v1 contract')
    rmSync(incomplete.path)
  })

  check('bounded lease scans fail closed on directory-entry and aggregate-byte overflow', function () {
    const junk = []
    for (let index = 0; index < 1025; index++) {
      const file = join(writers, `.scan-limit-${String(index).padStart(4, '0')}`)
      writeFileSync(file, '')
      junk.push(file)
    }
    let scan = writerLeases.scan(writers)
    assert.equal(scan.active.length, 0)
    assert.equal(scan.stale.length, 0)
    assert.equal(scan.issues.length, 1)
    assert.equal(scan.issues[0].code, 'WRITER_LEASE_SCAN_LIMIT')
    for (const file of junk) unlinkSync(file)

    const handles = []
    try {
      for (let index = 0; index < 9; index++) {
        const handle = writerLeases.acquire(writers, {
          kind: 'lock-writer', stem: null, key: `task:scan-bound-${index}`,
          sessionId: writerLeases.createSessionId(), ownerPid: process.pid,
        })
        const record = JSON.parse(readFileSync(handle.path, 'utf8'))
        record.boundedPadding = 'x'.repeat(30 * 1024)
        writeFileSync(handle.path, JSON.stringify(record) + '\n')
        handles.push(handle)
      }
      scan = writerLeases.scan(writers)
      assert.ok(scan.issues.some((issue) => issue.code === 'WRITER_LEASE_SCAN_LIMIT'))
      assert.ok(scan.active.length < handles.length,
        'overflow must stop before returning a complete-looking active/stale conclusion')
    } finally {
      for (const handle of handles) rmSync(handle.path, { force: true })
    }
  })

  check('bounded leases renew ownership-safely and expired generations cannot reopen a race', function () {
    const acquired = run([
      'acquire', '--kind', 'task-session', '--stem', 'TASK_6_renew', '--key', 'direct:run',
      '--owner-pid', String(process.pid), '--ttl-ms', '10000',
    ])
    assert.equal(acquired.status, 0, acquired.stderr + acquired.stdout)
    const row = JSON.parse(acquired.stdout)
    const originalExpiry = Date.parse(row.expiresAt)
    const wrong = run(['renew', '--lease-id', row.leaseId, '--token', '0'.repeat(48), '--ttl-ms', '60000'])
    assert.equal(wrong.status, 1)
    assert.match(wrong.stderr, /ownership changed/)
    assert.equal(writerLeases.scan(writers).active.length, 1, 'wrong-token renewal must not alter/remove the lease')

    const beforeRenew = Date.now()
    const renewed = run(['renew', '--lease-id', row.leaseId, '--token', row.token, '--ttl-ms', String(writerLeases.MAX_TTL_MS * 2)])
    assert.equal(renewed.status, 0, renewed.stderr + renewed.stdout)
    const renewedRow = JSON.parse(renewed.stdout)
    assert.ok(Date.parse(renewedRow.expiresAt) > originalExpiry)
    assert.ok(Date.parse(renewedRow.expiresAt) <= beforeRenew + writerLeases.MAX_TTL_MS + 2000, 'renewal must retain the one-hour crash bound')
    releaseLease(row)

    const expired = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_7_expired', key: 'direct:run',
      sessionId: writerLeases.createSessionId(), ownerPid: 2147483647, ttlMs: 1,
    })
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 10)
    const refused = run(['renew', '--lease-id', expired.leaseId, '--token', expired.token])
    assert.equal(refused.status, 1)
    assert.match(refused.stderr, /already expired/)
    writerLeases.release(expired)
  })

  check('caller-supplied bounded receipt verification is exact, guarded, and read-only', function () {
    const acquired = run([
      'acquire', '--guard-finalization', '--kind', 'task-session',
      '--stem', 'TASK_20_standby_receipt', '--key', 'standby:run',
      '--owner-pid', String(process.pid), '--ttl-ms', '60000',
    ])
    assert.equal(acquired.status, 0, acquired.stderr + acquired.stdout)
    const row = JSON.parse(acquired.stdout)
    const verifyArgs = [
      'verify', '--guard-finalization', '--lease-id', row.leaseId, '--token', row.token,
      '--session-id', row.sessionId, '--stem', 'TASK_20_standby_receipt',
    ]
    let before = writerSnapshot()
    let result = run(verifyArgs)
    assert.equal(result.status, 0, result.stderr + result.stdout)
    assert.deepEqual(JSON.parse(result.stdout), {
      verified: true, leaseId: row.leaseId, sessionId: row.sessionId,
      stem: 'TASK_20_standby_receipt', kind: 'task-session', expiresAt: row.expiresAt,
    })
    assert.deepEqual(writerSnapshot(), before, 'exact receipt verification must not rewrite its caller-owned lease')

    for (const replacement of [
      ['--token', '0'.repeat(48)],
      ['--session-id', writerLeases.createSessionId()],
      ['--stem', 'TASK_21_wrong_receipt_stem'],
    ]) {
      const args = verifyArgs.slice()
      args[args.indexOf(replacement[0]) + 1] = replacement[1]
      result = run(args)
      assert.equal(result.status, 2, result.stderr + result.stdout)
      assert.equal(result.stdout, '')
      assert.match(result.stderr, /expected one exact active bounded task-session lease, found 0/)
      assert.deepEqual(writerSnapshot(), before, 'mismatched credentials must be read-only')
    }

    const conflict = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_20_standby_receipt', key: 'direct:foreign',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, ttlMs: 60000,
    })
    before = writerSnapshot()
    result = run(verifyArgs)
    assert.equal(result.status, 2, result.stderr + result.stdout)
    assert.match(result.stderr, /another active writer owns TASK_20_standby_receipt/)
    assert.deepEqual(writerSnapshot(), before, 'conflict refusal must not mutate either generation')
    writerLeases.release(conflict)

    const marker = join(state, 'TASK_20_receipt_recovery.json')
    writeFileSync(marker, '{}\n')
    before = writerSnapshot()
    result = run(verifyArgs)
    assert.equal(result.status, 2, result.stderr + result.stdout)
    assert.match(result.stderr, /FINALIZATION_MARKER_ACTIVE/)
    assert.deepEqual(writerSnapshot(), before, 'finalization refusal must not release a caller-owned lease')
    unlinkSync(marker)
    releaseLease(row)

    const expired = writerLeases.acquire(writers, {
      kind: 'task-session', stem: 'TASK_22_expired_receipt', key: 'standby:run',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, ttlMs: 1,
    })
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 10)
    result = run([
      'verify', '--lease-id', expired.leaseId, '--token', expired.token,
      '--session-id', expired.record.sessionId, '--stem', expired.record.stem,
    ])
    assert.equal(result.status, 2, result.stderr + result.stdout)
    assert.match(result.stderr, /expected one exact active bounded task-session lease, found 0/)
    writerLeases.release(expired)
  })

  check('any durable non-mutex marker refuses and atomically withdraws the lease', function () {
    writeFileSync(join(state, 'TASK_2_recovery.json'), '{corrupt still blocks\n')
    assertGuardRefused(run(acquireArgs()), 'FINALIZATION_MARKER_ACTIVE')
    unlinkSync(join(state, 'TASK_2_recovery.json'))
  })

  check('a live local or unprovable remote mutex refuses the writer', function () {
    const path = join(state, '.mutex.json')
    writeFileSync(path, JSON.stringify(mutex()) + '\n')
    assertGuardRefused(run(acquireArgs()), 'FINALIZATION_MUTEX_BUSY')
    writeFileSync(path, JSON.stringify(mutex({ hostname: hostname() + '-remote' })) + '\n')
    assertGuardRefused(run(acquireArgs()), 'FINALIZATION_MUTEX_BUSY')
  })

  check('released and provably dead local mutex records do not create false ownership', function () {
    const path = join(state, '.mutex.json')
    writeFileSync(path, JSON.stringify(mutex({ released: true })) + '\n')
    let result = run(acquireArgs())
    assert.equal(result.status, 0, result.stderr + result.stdout)
    releaseLease(JSON.parse(result.stdout))

    writeFileSync(path, JSON.stringify(mutex({ pid: 2147483647, released: false })) + '\n')
    result = run(acquireArgs())
    assert.equal(result.status, 0, result.stderr + result.stdout)
    releaseLease(JSON.parse(result.stdout))

    const currentStart = writerLeases.captureProcessStartId(process.pid)
    const reusedStart = currentStart.slice(0, -1) + (currentStart.endsWith('0') ? '1' : '0')
    writeFileSync(path, JSON.stringify(mutex({ processStartId: reusedStart, released: false })) + '\n')
    result = run(acquireArgs())
    assert.equal(result.status, 0, result.stderr + result.stdout)
    releaseLease(JSON.parse(result.stdout))
  })

  check('unsafe or corrupt mutex state fails closed without JSON success', function () {
    const path = join(state, '.mutex.json')
    writeFileSync(path, '{broken')
    assertGuardRefused(run(acquireArgs()), 'FINALIZATION_MUTEX_UNSAFE')
    unlinkSync(path)
  })

  check('hidden durable CAS state and malformed lookalikes block guarded writers', function () {
    const creations = join(root, 'creations')
    mkdirSync(creations, { recursive: true })
    const valid = '.durable-cas-' + ['a'.repeat(16), 'b'.repeat(16), 'c'.repeat(16)].join('-')
    mkdirSync(join(creations, valid))
    assertGuardRefused(run(acquireArgs()), 'CREATION_MARKER_CAS_RECOVERY_REQUIRED')
    rmSync(join(creations, valid), { recursive: true, force: true })

    writeFileSync(join(creations, '.durable-cas-not-canonical'), '')
    assertGuardRefused(run(acquireArgs()), 'CREATION_MARKER_CAS_NAME_UNSAFE')
    unlinkSync(join(creations, '.durable-cas-not-canonical'))

    const edits = join(root, 'edits')
    mkdirSync(edits, { recursive: true })
    mkdirSync(join(edits, valid))
    assertGuardRefused(run(acquireArgs()), 'EDIT_MARKER_CAS_RECOVERY_REQUIRED')
    rmSync(join(edits, valid), { recursive: true, force: true })
    writeFileSync(join(edits, '.durable-cas-malformed'), '')
    assertGuardRefused(run(acquireArgs()), 'EDIT_MARKER_CAS_NAME_UNSAFE')
    unlinkSync(join(edits, '.durable-cas-malformed'))
  })

  check('authority-root traversal rejects a symlinked ancestor before any redirected write', function () {
    const authority = join(root, 'ancestor-authority')
    const orchestrator = join(authority, 'orchestrator')
    const outside = join(root, 'ancestor-outside')
    mkdirSync(orchestrator, { recursive: true })
    mkdirSync(outside)
    symlinkSync(outside, join(orchestrator, '.cache'))
    const redirected = join(orchestrator, '.cache', 'tasks', 'finalizations', '.writers')
    const scan = writerLeases.scan(redirected, authority)
    assert.equal(scan.active.length, 0)
    assert.equal(scan.stale.length, 0)
    assert.equal(scan.issues[0]?.code, 'WRITER_LEASE_DIR_UNSAFE')
    assert.match(scan.issues[0]?.message || '', /component must be a real directory/)
    assert.throws(() => writerLeases.acquire(redirected, {
      rootDir: authority, kind: 'task-session', stem: 'TASK_40_ancestor',
      key: 'direct:ancestor', sessionId: writerLeases.createSessionId(), ownerPid: process.pid,
    }), /component must be a real directory/)
    assert.deepEqual(readdirSync(outside), [], 'symlink target must remain untouched')
  })

  check('generation CAS never updates or deletes a racing path replacement', function () {
    const resetIsolatedWriters = () => {
      rmSync(writers, { recursive: true, force: true })
      mkdirSync(writers, { recursive: true })
    }
    const replacementFor = (handle, token) => Buffer.from(JSON.stringify({
      ...handle.record, token, key: 'direct:racing-generation', updatedAt: new Date().toISOString(),
    }, null, 2) + '\n')

    const renewRace = writerLeases.acquire(writers, {
      rootDir: root, kind: 'task-session', stem: 'TASK_41_cas_renew', key: 'direct:cas',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, ttlMs: 60000,
    })
    const displacedRenew = join(writers, '.displaced-renew')
    const renewReplacement = replacementFor(renewRace, 'a'.repeat(48))
    renewRace._testHook = (stage) => {
      if (stage !== 'renewal:after-read') return
      renewRace._testHook = null
      renameSync(renewRace.path, displacedRenew)
      writeFileSync(renewRace.path, renewReplacement)
    }
    assert.throws(() => writerLeases.renew(renewRace, 60000), /replacement raced with renewal/)
    assert.deepEqual(readFileSync(renewRace.path), renewReplacement, 'renew must restore, not mutate, the replacement')
    resetIsolatedWriters()

    const publishRace = writerLeases.acquire(writers, {
      rootDir: root, kind: 'task-session', stem: 'TASK_42_cas_publish', key: 'direct:cas',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, ttlMs: 60000,
    })
    const publishReplacement = replacementFor(publishRace, 'b'.repeat(48))
    publishRace._testHook = (stage) => {
      if (stage !== 'renewal:before-publish') return
      publishRace._testHook = null
      writeFileSync(publishRace.path, publishReplacement)
    }
    assert.throws(() => writerLeases.renew(publishRace, 60000), /without clobbering a racing generation/)
    assert.deepEqual(readFileSync(publishRace.path), publishReplacement, 'O_EXCL publication must preserve the racer')
    let scan = writerLeases.scan(writers, root)
    assert.ok(scan.issues.some((issue) => issue.code === 'WRITER_LEASE_RECOVERY_REQUIRED'),
      'an un-restorable detached generation must block scans through a recovery artifact')
    resetIsolatedWriters()

    const afterPublishRace = writerLeases.acquire(writers, {
      rootDir: root, kind: 'task-session', stem: 'TASK_45_cas_after_publish', key: 'direct:cas',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, ttlMs: 60000,
    })
    const displacedPublished = join(writers, '.displaced-published')
    const afterPublishReplacement = replacementFor(afterPublishRace, 'd'.repeat(48))
    afterPublishRace._testHook = (stage) => {
      if (stage !== 'renewal:after-publish') return
      afterPublishRace._testHook = null
      renameSync(afterPublishRace.path, displacedPublished)
      writeFileSync(afterPublishRace.path, afterPublishReplacement)
    }
    assert.throws(() => writerLeases.renew(afterPublishRace, 60000), /generation was replaced before CAS commit/)
    assert.deepEqual(readFileSync(afterPublishRace.path), afterPublishReplacement,
      'post-open replacement must survive and keep the old generation in recovery')
    scan = writerLeases.scan(writers, root)
    assert.ok(scan.issues.some((issue) => issue.code === 'WRITER_LEASE_RECOVERY_REQUIRED'))
    resetIsolatedWriters()

    const releaseRace = writerLeases.acquire(writers, {
      rootDir: root, kind: 'task-session', stem: 'TASK_43_cas_release', key: 'direct:cas',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid,
    })
    const displacedRelease = join(writers, '.displaced-release')
    const releaseReplacement = replacementFor(releaseRace, 'c'.repeat(48))
    releaseRace._testHook = (stage) => {
      if (stage !== 'release:after-read') return
      releaseRace._testHook = null
      renameSync(releaseRace.path, displacedRelease)
      writeFileSync(releaseRace.path, releaseReplacement)
    }
    assert.throws(() => writerLeases.release(releaseRace), /replacement raced with release/)
    assert.deepEqual(readFileSync(releaseRace.path), releaseReplacement, 'release must not unlink a replacement')
    resetIsolatedWriters()

    const serialized = writerLeases.acquire(writers, {
      rootDir: root, kind: 'task-session', stem: 'TASK_44_cas_serialized', key: 'direct:cas',
      sessionId: writerLeases.createSessionId(), ownerPid: process.pid, ttlMs: 60000,
    })
    serialized._testHook = (stage) => {
      if (stage !== 'renewal:after-read') return
      assert.throws(() => writerLeases.release({ ...serialized, _testHook: null }),
        /mutation is already in progress/)
    }
    writerLeases.renew(serialized, 60000)
    serialized._testHook = null
    writerLeases.release(serialized)
    scan = writerLeases.scan(writers, root)
    assert.equal(scan.issues.length, 0)
  })

  check('CLI rejects unknown, duplicate, missing, and invalid flag values', function () {
    for (const args of [
      ['acquire', '--guard-finalization', '--unknown'],
      ['acquire', '--kind'],
      ['acquire', '--kind', 'task-session', '--kind', 'task-session'],
      ['acquire', '--owner-pid', 'zero'],
      ['renew', '--lease-id', 'missing-token'],
      ['verify', '--lease-id', writerLeases.createLeaseId()],
      ['verify-session', '--session-id', writerLeases.createSessionId()],
      ['verify-session', '--session-id', writerLeases.createSessionId(), '--stem', 'TASK_01_leading_zero'],
      ['verify-session', '--session-id', writerLeases.createSessionId(), '--stem', 'TASK_9007199254740992_unsafe'],
      ['acquire-publication-guard', '--key', 'task:create-backlog'],
      ['acquire-publication-guard', '--parent-delegation-token', '0'.repeat(48)],
      ['scan', '--guard-finalization'],
    ]) {
      const result = run(args)
      assert.equal(result.status, 1, `${args.join(' ')}\n${result.stderr}${result.stdout}`)
      assert.equal(result.stdout, '')
    }
  })

  console.log(`\nwriter-lease CLI: ${checks} checks passed`)
} finally {
  rmSync(root, { recursive: true, force: true })
}
