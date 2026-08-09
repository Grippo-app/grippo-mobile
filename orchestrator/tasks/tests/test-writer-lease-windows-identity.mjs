#!/usr/bin/env node

import assert from 'node:assert/strict'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const windowsProof = require('../../site/server/windows-runtime-proof.js')
const writerPath = require.resolve('../writer-leases.cjs')
const platformDescriptor = Object.getOwnPropertyDescriptor(process, 'platform')
const originals = {
  captureProcessStartId: windowsProof.captureProcessStartId,
  inspectProcess: windowsProof.inspectProcess,
  processIdentityMatches: windowsProof.processIdentityMatches,
  processIdentityState: windowsProof.processIdentityState,
  processTreeProof: windowsProof.processTreeProof,
}
const PSID_A = 'psid-v1:win32:' + 'a'.repeat(64)
const PSID_B = 'psid-v1:win32:' + 'b'.repeat(64)
const base = mkdtempSync(join(tmpdir(), 'writer-win-identity-'))
const writers = join(base, 'finalizations', '.writers')
let inspectCalls = 0
let captureUnavailable = false
let childUnavailable = false

function state(pid, recorded) {
  if (pid === 404) return 'dead'
  if (pid === 405) return 'unknown'
  if (!windowsProof.PSID_RE.test(String(recorded || ''))) return 'pid-live'
  return recorded === PSID_A ? 'match' : 'reused'
}

try {
  windowsProof.captureProcessStartId = function (pid) {
    if (captureUnavailable) {
      const error = new Error('native proof unavailable')
      error.code = 'WINDOWS_PROCESS_IDENTITY_UNAVAILABLE'
      throw error
    }
    if (childUnavailable && pid === 999) return null
    return PSID_A
  }
  windowsProof.inspectProcess = function (pid) {
    inspectCalls++
    return pid === 404 ? { status: 'dead', processStartId: null } :
      pid === 405 ? { status: 'unknown', processStartId: null } : { status: 'live', processStartId: PSID_A }
  }
  windowsProof.processIdentityState = state
  windowsProof.processIdentityMatches = (pid, recorded) => state(pid, recorded) === 'match'
  windowsProof.processTreeProof = (descendantPid, descendantStartId, ancestorPid, ancestorStartId) => ({
    ok: descendantStartId === PSID_A && ancestorStartId === PSID_A,
    depth: 1, caller: { pid: descendantPid, startId: descendantStartId, pgid: null },
    authority: { pid: ancestorPid, startId: ancestorStartId, pgid: null },
  })
  Object.defineProperty(process, 'platform', { ...platformDescriptor, value: 'win32' })
  delete require.cache[writerPath]
  const writer = require(writerPath)

  process.env.WINDOWS_OWNER_CONTEXT_TEST = JSON.stringify({
    version: 1, transactionId: '1'.repeat(32), revision: 1,
    contentHash: 'sha256:' + '2'.repeat(64), pid: process.pid, processStartId: PSID_A,
    authorityLeaseId: 'wr-windows-owner-context-1234', mode: 'owned-lease',
    publicationKey: 'task:create-backlog', keyHash: 'sha256:' + '3'.repeat(64),
    publicationGuardLeaseId: null,
  })
  const runtimeIntegrity = require('../../site/server/runtime-integrity.js')
  assert.equal(runtimeIntegrity._parseOwnerContext('WINDOWS_OWNER_CONTEXT_TEST', 'creation').processStartId, PSID_A)

  assert.match(PSID_A, writer.PROCESS_START_ID_RE)
  assert.equal(writer.captureProcessStartId(process.pid), PSID_A)
  assert.equal(writer.processIdentityState(process.pid, PSID_A), 'match')
  assert.equal(writer.processIdentityState(process.pid, PSID_B), 'reused')
  assert.equal(writer.processIdentityState(process.pid, null), 'pid-live')
  assert.equal(writer.processIdentityState(404, PSID_A), 'dead')
  assert.equal(writer.processIdentityState(405, PSID_A), 'unknown')
  assert.equal(writer.processIdentityMatches(process.pid, PSID_A), true)
  assert.equal(writer.processIdentityMatches(process.pid, PSID_B), false)
  assert.equal(writer.processTreeProof(999, PSID_A, process.pid, PSID_A).ok, true)
  assert.equal(writer.processTreeProof(999, PSID_B, process.pid, PSID_A).ok, false)
  process.stdout.write('ok 1 - writer lease delegates Windows capture/state/match without PID-only fallback\n')

  captureUnavailable = true
  assert.throws(() => writer.captureProcessStartId(process.pid), (error) =>
    error && error.code === 'WINDOWS_PROCESS_IDENTITY_UNAVAILABLE')
  captureUnavailable = false
  process.stdout.write('ok 2 - Windows capture transport failure remains fail-closed\n')

  const first = writer.acquire(writers, {
    rootDir: base, kind: 'task-session', stem: 'TASK_701_windows_cache', key: 'windows-cache-a',
    ownerPid: process.pid, ttlMs: 60000,
  })
  writer.acquire(writers, {
    rootDir: base, kind: 'task-session', stem: 'TASK_702_windows_cache', key: 'windows-cache-b',
    ownerPid: process.pid, ttlMs: 60000,
  })
  inspectCalls = 0
  const scanned = writer.scan(writers, base)
  assert.equal(scanned.issues.length, 0)
  assert.equal(scanned.active.length, 2)
  assert.equal(inspectCalls, 1, 'one scan must reuse one native generation verdict per PID')
  process.stdout.write('ok 3 - Windows scan caches one exact native verdict per PID\n')

  childUnavailable = true
  assert.throws(() => writer.updateChildPid(first, 999), /process-start identity is unavailable/)
  childUnavailable = false
  process.stdout.write('ok 4 - Windows child binding refuses a missing exact generation before mutation\n')
} finally {
  delete process.env.WINDOWS_OWNER_CONTEXT_TEST
  delete require.cache[writerPath]
  Object.assign(windowsProof, originals)
  Object.defineProperty(process, 'platform', platformDescriptor)
  rmSync(base, { recursive: true, force: true })
}
