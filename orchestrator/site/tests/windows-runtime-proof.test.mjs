#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const windowsProof = require('../server/windows-runtime-proof.js')
const PSID_A = 'psid-v1:win32:' + 'a'.repeat(64)
const PSID_B = 'psid-v1:win32:' + 'b'.repeat(64)
const FILE = '/tmp/windows-proof-owner.json'
const FILE_PROOF = { dev: '10', ino: '20', type: 'file', nlink: '1' }

let checks = 0
function check(name, fn) {
  fn()
  checks++
  process.stdout.write(`ok ${checks} - ${name}\n`)
}

function sorted(value) {
  if (Array.isArray(value)) return value.map(sorted)
  if (!value || typeof value !== 'object') return value
  return Object.fromEntries(Object.keys(value).sort().map((key) => [key, sorted(value[key])]))
}
function result(value, overrides = {}) {
  return { status: 0, stdout: JSON.stringify(sorted(value)) + '\n', stderr: '', ...overrides }
}
function processResult(pid, status, id = null, reason = status === 'live' ? 'ok' : status === 'dead' ? 'not-found' : 'open-failed') {
  return { pid, processStartId: id, reason, status, version: 1 }
}
function pathResult(status, reason, dev = null, ino = null, pathType = null) {
  return { dev, ino, pathType, reason, status, version: 1 }
}
function ancestryResult(descendantPid, ancestorPid, status, depth = null, reason = status === 'match' ? 'ok' : 'ancestor-generation') {
  return { ancestorPid, depth, descendantPid, reason, status, version: 1 }
}
function adapter(runHelper) { return windowsProof.create({ platform: 'win32', runHelper }) }

check('exact live process generation supports capture, match, and PID-reuse detection', () => {
  const calls = []
  const runtime = adapter((args) => {
    calls.push(args)
    return result(processResult(41, 'live', PSID_A))
  })
  assert.equal(runtime.captureProcessStartId(41), PSID_A)
  assert.equal(runtime.processIdentityState(41, PSID_A), 'match')
  assert.equal(runtime.processIdentityState(41, PSID_B), 'reused')
  assert.equal(runtime.processIdentityMatches(41, PSID_A), true)
  assert.deepEqual(calls, [['process', '41'], ['process', '41'], ['process', '41'], ['process', '41']])
})

check('dead, unknown, pid-live, invalid PID, and unsupported platform remain distinct', () => {
  const dead = adapter(() => result(processResult(52, 'dead')))
  assert.equal(dead.captureProcessStartId(52), null)
  assert.equal(dead.processIdentityState(52, PSID_A), 'dead')
  assert.equal(dead.processIdentityState(0, PSID_A), 'dead')
  const live = adapter(() => result(processResult(52, 'live', PSID_A)))
  assert.equal(live.processIdentityState(52, null), 'pid-live')
  const unknown = adapter(() => result(processResult(52, 'unknown')))
  assert.equal(unknown.processIdentityState(52, PSID_A), 'unknown')
  assert.throws(() => unknown.captureProcessStartId(52), (error) => error.code === 'WINDOWS_PROCESS_IDENTITY_UNAVAILABLE')
  const unsupported = windowsProof.create({ platform: 'darwin', runHelper: () => { throw new Error('must not run') } })
  assert.equal(unsupported.processIdentityState(52, PSID_A), 'unsupported')
})

check('malformed, non-canonical, overlong, and pid-confused helper output is fail-closed', () => {
  const variants = [
    result({ ...processResult(61, 'live', PSID_A), extra: true }),
    result(processResult(61, 'live', 'psid-v1:win32:nope')),
    result(processResult(62, 'live', PSID_A)),
    { status: 0, stdout: ' ' + JSON.stringify(processResult(61, 'live', PSID_A)) + '\n' },
    { status: 0, stdout: 'x'.repeat(16 * 1024 + 1) },
    { status: 1, stdout: '' },
    { status: null, stdout: '', error: new Error('transport') },
  ]
  for (const invalid of variants) {
    const runtime = adapter(() => invalid)
    assert.equal(runtime.processIdentityState(61, PSID_A), 'unknown')
    assert.throws(() => runtime.captureProcessStartId(61), (error) => error.code === 'WINDOWS_PROCESS_IDENTITY_UNAVAILABLE')
  }
})

check('Windows ancestry is bound to both exact generations and one canonical helper envelope', () => {
  const calls = []
  const runtime = adapter((args) => {
    calls.push(args)
    return result(ancestryResult(71, 70, 'match', 1))
  })
  assert.deepEqual(runtime.processTreeProof(71, PSID_A, 70, PSID_B), {
    ok: true, depth: 1,
    caller: { pid: 71, startId: PSID_A, pgid: null },
    authority: { pid: 70, startId: PSID_B, pgid: null },
  })
  assert.deepEqual(calls, [['ancestry', '71', PSID_A, '70', PSID_B]])
  const mismatch = adapter(() => result(ancestryResult(71, 70, 'mismatch')))
  assert.deepEqual(mismatch.processTreeProof(71, PSID_A, 70, PSID_B), { ok: false, reason: 'ancestor-generation' })
  const malformed = adapter(() => result({ ...ancestryResult(71, 70, 'match', 1), extra: true }))
  assert.equal(malformed.processTreeProof(71, PSID_A, 70, PSID_B).ok, false)
  assert.equal(runtime.processTreeProof(71, null, 70, PSID_B).ok, false)
})

check('private DACL proof is bound to exact file-guard volume, inode, and type', () => {
  const runtime = adapter(() => result(pathResult('private', 'ok', '10', '20', 'file')))
  assert.equal(runtime.privatePathState(FILE, FILE_PROOF), 'private')
  assert.equal(runtime.privatePathState(FILE, { ...FILE_PROOF, ino: '21' }), 'changed')
  assert.equal(runtime.privatePathState(FILE, { ...FILE_PROOF, type: 'directory' }), 'changed')
  assert.equal(runtime.privatePathState('relative.json', FILE_PROOF), 'unknown')
  assert.equal(runtime.privatePathState(FILE, { dev: '10', ino: '20', type: 'symlink' }), 'unknown')
})

check('unsafe, missing, unknown, changed, and malformed path verdicts never become private', () => {
  const cases = [
    [pathResult('unsafe', 'ace-principal', '10', '20', 'file'), 'unsafe'],
    [pathResult('unsafe', 'reparse', '10', '20', 'file'), 'unsafe'],
    [pathResult('missing', 'missing'), 'missing'],
    [pathResult('unknown', 'open-failed'), 'unknown'],
    [pathResult('unknown', 'set-dacl', '10', '20', 'file'), 'unknown'],
    [pathResult('private', 'ok', '10', '99', 'file'), 'changed'],
    [{ ...pathResult('private', 'ok', '10', '20', 'file'), extra: 1 }, 'unknown'],
    [pathResult('private', 'ace-principal', '10', '20', 'file'), 'unknown'],
  ]
  for (const [value, expected] of cases) {
    assert.equal(adapter(() => result(value)).privatePathState(FILE, FILE_PROOF), expected)
  }
})

check('DACL hardening uses its separate native operation and requires a rebound proof', () => {
  let command = null
  const runtime = adapter((args) => {
    command = args
    return result(pathResult('private', 'ok', '10', '20', 'file'))
  })
  assert.equal(runtime.hardenPrivatePath(FILE, FILE_PROOF), 'private')
  assert.deepEqual(command, ['harden-path', FILE, '10', '20', 'file'])
})

function recoveryFixture({ record, processRows, privacy = pathResult('private', 'ok', '10', '20', 'file'), unlink = true,
  journal = { ok: true }, bytes = Buffer.from(JSON.stringify(record) + '\n') }) {
  const calls = []
  const runtime = adapter((args) => {
    calls.push(args)
    if (args[0] === 'private-path') return result(privacy)
    const pid = Number(args[1])
    return result(processRows[pid] || processResult(pid, 'unknown'))
  })
  let deletes = 0
  const guards = {
    reconcileGuardTransactionsUnder(root, directory, options) {
      assert.equal(root, '/authority')
      assert.equal(directory, '/authority/locks')
      assert.deepEqual(options, { maxEntries: 512, maxTransactions: 512 })
      return journal
    },
    boundedRegularFileUnder(root, directory, file, maxBytes) {
      assert.equal(root, '/authority')
      assert.equal(directory, '/authority/locks')
      assert.equal(file, FILE)
      assert.equal(maxBytes, 4096)
      return { bytes, stat: FILE_PROOF }
    },
    unlinkRegularFileMatchingUnder(root, directory, file, maxBytes, expected) {
      deletes++
      assert.deepEqual(expected, { bytes, proof: FILE_PROOF })
      return unlink
    },
  }
  const verdict = runtime.recoverStaleOwner({
    fileGuards: guards, root: '/authority', directory: '/authority/locks', file: FILE, maxBytes: 4096,
    hostname: 'local', validateOwner: (value) => value.kind === 'owner', validateWorker: (value) => value.kind === 'worker',
  })
  return { calls, deletes, verdict }
}

const owner = { kind: 'owner', hostname: 'local', pid: 101, processStartId: PSID_A }
const worker = { kind: 'worker', hostname: 'local', pid: 101, processStartId: PSID_A,
  childPid: 202, childProcessStartId: PSID_B }

check('matching-WAL recovery removes only a canonical proven-dead owner generation', () => {
  const dead = recoveryFixture({ record: owner, processRows: { 101: processResult(101, 'dead') } })
  assert.deepEqual(dead.verdict, { code: 'removed', removed: true })
  assert.equal(dead.deletes, 1)
  const raced = recoveryFixture({ record: owner, processRows: { 101: processResult(101, 'dead') }, unlink: false })
  assert.deepEqual(raced.verdict, { code: 'changed', removed: false })
  assert.equal(raced.deletes, 1)
})

check('live, unknown, remote, unsafe-DACL, and invalid records are retained without mutation', () => {
  const active = recoveryFixture({ record: owner, processRows: { 101: processResult(101, 'live', PSID_A) } })
  assert.equal(active.verdict.code, 'active')
  assert.equal(active.deletes, 0)
  const unknown = recoveryFixture({ record: owner, processRows: { 101: processResult(101, 'unknown') } })
  assert.equal(unknown.verdict.code, 'owner-unknown')
  assert.equal(unknown.deletes, 0)
  const remote = recoveryFixture({ record: { ...owner, hostname: 'remote' }, processRows: {} })
  assert.equal(remote.verdict.code, 'remote')
  assert.equal(remote.deletes, 0)
  const unsafe = recoveryFixture({ record: owner, processRows: {},
    privacy: pathResult('unsafe', 'ace-principal', '10', '20', 'file') })
  assert.equal(unsafe.verdict.code, 'privacy-unsafe')
  assert.equal(unsafe.deletes, 0)
  const malformed = recoveryFixture({ record: owner, processRows: {}, bytes: Buffer.from('{bad}\n') })
  assert.equal(malformed.verdict.code, 'invalid-record')
  assert.equal(malformed.deletes, 0)
  const journalUnsafe = recoveryFixture({ record: owner, processRows: {}, journal: { ok: false } })
  assert.equal(journalUnsafe.verdict.code, 'journal-unsafe')
  assert.equal(journalUnsafe.deletes, 0)
})

check('worker recovery requires both site owner and bound Job wrapper generations to be gone', () => {
  const childLive = recoveryFixture({ record: worker, processRows: {
    101: processResult(101, 'dead'), 202: processResult(202, 'live', PSID_B),
  } })
  assert.equal(childLive.verdict.code, 'child-active')
  assert.equal(childLive.deletes, 0)
  const childUnknown = recoveryFixture({ record: worker, processRows: {
    101: processResult(101, 'dead'), 202: processResult(202, 'unknown'),
  } })
  assert.equal(childUnknown.verdict.code, 'child-unknown')
  assert.equal(childUnknown.deletes, 0)
  const drainedByClose = recoveryFixture({ record: worker, processRows: {
    101: processResult(101, 'dead'), 202: processResult(202, 'dead'),
  } })
  assert.equal(drainedByClose.verdict.code, 'removed')
  assert.equal(drainedByClose.deletes, 1)
})

check('spawn-before-binding worker is recoverable only after the site owner generation is gone', () => {
  const pending = { ...worker, childPid: null, childProcessStartId: null }
  const dead = recoveryFixture({ record: pending, processRows: { 101: processResult(101, 'dead') } })
  assert.equal(dead.verdict.code, 'removed')
  const live = recoveryFixture({ record: pending, processRows: { 101: processResult(101, 'live', PSID_A) } })
  assert.equal(live.verdict.code, 'active')
})

process.stdout.write(`windows runtime proof checks: ${checks}\n`)
