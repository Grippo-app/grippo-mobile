#!/usr/bin/env node

import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const ownerGuard = require('../server/shallow-owner-guard.js')

const ROOT = '/authority'
const FILE = '/authority/runtime/owner.json'
const LOCAL_SITE = Object.freeze({ pid: 101, processStartId: 'site:start:current' })
const PRIVATE_MODE = 0o100600

let checks = 0
function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

function owner(overrides = {}) {
  return {
    role: 'owner', version: 1, hostname: 'local', pid: 201,
    processStartId: 'site:start:old', token: 'owner-token',
    ...overrides,
  }
}

function worker(overrides = {}) {
  return {
    role: 'worker', version: 1, hostname: 'local', pid: 201,
    processStartId: 'site:start:old', token: 'worker-token',
    childPid: 301, childProcessStartId: 'child:start:old',
    modelPid: 401, modelProcessStartId: 'model:start:old',
    goState: 'sent',
    ...overrides,
  }
}

function pair(pid, processStartId) {
  if (pid === null && processStartId === null) return null
  return { pid, processStartId }
}

function makeHarness(extra = {}) {
  let serial = 100
  let current = null
  let observedOverride = null
  let reconcileResult = {
    ok: true, exists: true, scanned: 0, transactions: 0,
    reconciled: 0, pending: 0, codes: {},
  }
  let unlinkScript = null
  let reconcileHook = null
  const processStates = new Map()
  const calls = []
  let privacyState = 'private'
  let containment = 'drained'

  function canonical(value) {
    return Buffer.from(`${JSON.stringify(value)}\n`, 'utf8')
  }

  function statFor(bytes, overrides = {}) {
    serial++
    return {
      dev: '10', ino: String(serial), mode: PRIVATE_MODE,
      modeExact: String(PRIVATE_MODE), nlink: '1', size: bytes.length,
      sizeExact: String(bytes.length), mtimeNs: String(serial * 1000),
      ctimeNs: String(serial * 1000 + 1), type: 'file',
      isFile() { return this.type === 'file' },
      isDirectory() { return this.type === 'directory' },
      isSymbolicLink() { return this.type === 'symlink' },
      ...overrides,
    }
  }

  function setRaw(bytes, statOverrides = {}) {
    bytes = Buffer.isBuffer(bytes) ? Buffer.from(bytes) : Buffer.from(bytes, 'utf8')
    current = { bytes, stat: statFor(bytes, statOverrides) }
    return current
  }

  function setRecord(record, statOverrides = {}) {
    return setRaw(canonical(record), statOverrides)
  }

  function observedFrom(stat) {
    const proof = observedOverride || stat
    return {
      dev: BigInt(proof.dev), ino: BigInt(proof.ino), mode: BigInt(proof.modeExact),
      nlink: BigInt(proof.nlink), size: BigInt(proof.sizeExact),
      mtimeNs: BigInt(proof.mtimeNs), ctimeNs: BigInt(proof.ctimeNs),
      uid: BigInt(proof.uid === undefined ? 501 : proof.uid),
      isFile: () => proof.type === 'file',
      isDirectory: () => proof.type === 'directory',
      isSymbolicLink: () => proof.type === 'symlink',
    }
  }

  function sameProof(left, right) {
    return ['dev', 'ino', 'modeExact', 'nlink', 'sizeExact', 'mtimeNs', 'ctimeNs', 'type']
      .every((field) => String(left[field]) === String(right[field]))
  }

  const fileGuards = {
    reconcileGuardTransactionsUnder(root, directory, options) {
      calls.push(['reconcile', root, directory, options])
      if (reconcileHook) reconcileHook()
      return { ...reconcileResult }
    },
    boundedRegularFileUnder(root, directory, file, maxBytes) {
      calls.push(['read', root, directory, file, maxBytes])
      return current ? { bytes: Buffer.from(current.bytes), stat: { ...current.stat } } : null
    },
    inspectEntryUnder(root, directory, file) {
      calls.push(['inspect-entry', root, directory, file])
      return current ? { status: 'present', stat: current.stat } : { status: 'missing' }
    },
    unlinkRegularFileMatchingResultUnder(root, directory, file, maxBytes, expected) {
      calls.push(['unlink', root, directory, file, maxBytes, expected])
      if (unlinkScript) return unlinkScript({ expected, current, setRecord, setRaw })
      if (!current || !current.bytes.equals(expected.bytes) || !sameProof(current.stat, expected.proof)) {
        return {
          ok: false, code: 'target-proof-mismatch', outcome: 'not-committed',
          committed: false, alreadyMissing: false, uncertain: false,
        }
      }
      current = null
      return {
        ok: true, code: 'deleted', outcome: 'deleted', committed: true,
        alreadyMissing: false, uncertain: false,
      }
    },
  }

  const platform = extra.platform || 'linux'
  const config = {
    fileGuards, root: ROOT, maxBytes: 16384, hostname: 'local', platform,
    siteIdentity: () => LOCAL_SITE,
    currentUid: platform === 'win32' ? undefined : 501,
    lstatSync(file, options) {
      calls.push(['lstat', file, options])
      if (!current) {
        const error = new Error('missing')
        error.code = 'ENOENT'
        throw error
      }
      return observedFrom(current.stat)
    },
    privatePathState(file, proof) {
      calls.push(['private-path-state', file, proof])
      return privacyState
    },
    validateOwner: (record) => record && record.role === 'owner',
    validateWorker: (record) => record && record.role === 'worker',
    processIdentityState(pid, startId) {
      calls.push(['process-state', pid, startId])
      return processStates.get(`${pid}\0${startId}`) || 'unknown'
    },
    workerBindings(record) {
      return {
        child: pair(record.childPid, record.childProcessStartId),
        model: pair(record.modelPid, record.modelProcessStartId),
      }
    },
    proveNoSpawnPossible(record) {
      return record.goState === 'not-sent' && record.childPid === null &&
        record.childProcessStartId === null && record.modelPid === null &&
        record.modelProcessStartId === null
    },
    workerContainmentState(record) {
      calls.push(['containment-state', record.token])
      return containment
    },
    ...extra.config,
  }
  if (platform === 'win32') delete config.currentUid

  const guard = ownerGuard.create(config)
  return {
    guard, calls, processStates,
    get current() { return current },
    setRecord, setRaw,
    setObservedOverride(value) { observedOverride = value },
    setReconcileResult(value) { reconcileResult = value },
    setReconcileHook(value) { reconcileHook = value },
    setUnlinkScript(value) { unlinkScript = value },
    setPrivacyState(value) { privacyState = value },
    setContainment(value) { containment = value },
  }
}

function state(harness, identity, value) {
  harness.processStates.set(`${identity.pid}\0${identity.processStartId}`, value)
}

check('release reconciles first and deletes only its exact local site generation', () => {
  const h = makeHarness()
  h.setRecord(owner({ ...LOCAL_SITE, token: 'mine' }))
  const result = h.guard.release(FILE, 'mine')
  assert.deepEqual(result, {
    ok: true, action: 'release', code: 'deleted', outcome: 'deleted',
    committed: true, alreadyMissing: false, uncertain: false, recordKind: 'owner',
  })
  assert.equal(h.current, null)
  assert.deepEqual(h.calls.slice(0, 3).map((call) => call[0]), ['reconcile', 'read', 'lstat'])
  assert.equal(h.calls.filter((call) => call[0] === 'unlink').length, 1)
})

check('release never treats a foreign token, pid, start generation, or host as success', () => {
  const cases = [
    owner({ ...LOCAL_SITE, token: 'foreign-token' }),
    owner({ pid: LOCAL_SITE.pid + 1, processStartId: LOCAL_SITE.processStartId, token: 'mine' }),
    owner({ pid: LOCAL_SITE.pid, processStartId: 'site:start:replacement', token: 'mine' }),
    owner({ ...LOCAL_SITE, hostname: 'remote', token: 'mine' }),
  ]
  for (const record of cases) {
    const h = makeHarness()
    h.setRecord(record)
    const result = h.guard.release(FILE, 'mine')
    assert.equal(result.ok, false)
    assert.equal(result.code, 'owner-generation-foreign')
    assert.deepEqual(h.current.bytes, Buffer.from(`${JSON.stringify(record)}\n`))
    assert.equal(h.calls.some((call) => call[0] === 'unlink'), false)
  }
})

check('replacement between exact read and delete is preserved and reported non-committed', () => {
  const h = makeHarness()
  const replacement = owner({ ...LOCAL_SITE, token: 'replacement' })
  h.setRecord(owner({ ...LOCAL_SITE, token: 'mine' }))
  h.setUnlinkScript(({ setRecord }) => {
    setRecord(replacement)
    return {
      ok: false, code: 'target-proof-mismatch', outcome: 'not-committed',
      committed: false, alreadyMissing: false, uncertain: false,
    }
  })
  const result = h.guard.release(FILE, 'mine')
  assert.equal(result.ok, false)
  assert.equal(result.code, 'target-proof-mismatch')
  assert.equal(result.committed, false)
  assert.deepEqual(JSON.parse(h.current.bytes), replacement)
})

check('response-loss WAL is reconciled before retry and a new generation is never released as the old one', () => {
  const h = makeHarness()
  const replacement = owner({ ...LOCAL_SITE, token: 'replacement' })
  let oldWal = false
  h.setRecord(owner({ ...LOCAL_SITE, token: 'mine' }))
  // Model response loss after the exact old target was detached.
  h.setUnlinkScript(() => {
    oldWal = true
    h.setRecord(replacement)
    return {
      ok: false, code: 'worker-envelope-invalid', outcome: 'not-committed',
      committed: false, alreadyMissing: false, uncertain: true,
    }
  })
  const first = h.guard.release(FILE, 'mine')
  assert.equal(first.uncertain, true)
  assert.equal(oldWal, true)
  const unlinksBefore = h.calls.filter((call) => call[0] === 'unlink').length
  h.setReconcileHook(() => { oldWal = false })
  const retry = h.guard.release(FILE, 'mine')
  assert.equal(retry.ok, false)
  assert.equal(retry.code, 'owner-generation-foreign')
  assert.equal(h.calls.filter((call) => call[0] === 'unlink').length, unlinksBefore)
  assert.equal(oldWal, false)
  assert.deepEqual(JSON.parse(h.current.bytes), replacement)
})

check('delete settlement preserves committed, alreadyMissing, and uncertain evidence exactly', () => {
  const cases = [
    {
      input: { ok: true, code: 'deleted', outcome: 'deleted', committed: true, alreadyMissing: false, uncertain: false },
      expected: { ok: true, committed: true, alreadyMissing: false, uncertain: false },
    },
    {
      input: { ok: true, code: 'already-missing', outcome: 'already-missing', committed: false, alreadyMissing: true, uncertain: false },
      expected: { ok: true, committed: false, alreadyMissing: true, uncertain: false },
    },
    {
      input: { ok: false, code: 'delete-commit-unproven', outcome: 'not-committed', committed: false, alreadyMissing: true, uncertain: true },
      expected: { ok: false, committed: false, alreadyMissing: true, uncertain: true },
    },
  ]
  for (const item of cases) {
    const h = makeHarness()
    h.setRecord(owner({ ...LOCAL_SITE, token: 'mine' }))
    h.setUnlinkScript(() => ({ ...item.input }))
    const result = h.guard.release(FILE, 'mine')
    for (const [key, value] of Object.entries(item.expected)) assert.equal(result[key], value)
    assert.equal(result.code, item.input.code)
    assert.equal(result.outcome, item.input.outcome)
  }
})

check('stale owner recovery accepts only exact dead or reused site generations', () => {
  for (const processState of ['dead', 'reused']) {
    const h = makeHarness()
    const record = owner()
    h.setRecord(record)
    state(h, record, processState)
    const result = h.guard.recover(FILE)
    assert.equal(result.ok, true)
    assert.equal(result.committed, true)
  }
  for (const processState of ['match', 'pid-live', 'unknown', 'unsupported', 'invalid']) {
    const h = makeHarness()
    const record = owner()
    h.setRecord(record)
    state(h, record, processState)
    const result = h.guard.recover(FILE)
    assert.equal(result.ok, false)
    assert.equal(result.code, processState === 'match' || processState === 'pid-live'
      ? 'owner-site-active' : 'owner-site-unverified')
    assert.equal(h.current !== null, true)
    assert.equal(h.calls.some((call) => call[0] === 'unlink'), false)
  }
})

check('remote stale records are retained without probing a meaningless local PID', () => {
  const h = makeHarness()
  h.setRecord(owner({ hostname: 'remote' }))
  const result = h.guard.recover(FILE)
  assert.equal(result.code, 'owner-host-foreign')
  assert.equal(h.calls.some((call) => call[0] === 'process-state'), false)
  assert.equal(h.calls.some((call) => call[0] === 'unlink'), false)
})

check('fully bound worker requires site, child, model, and containment death proofs', () => {
  const record = worker()
  const safeCases = [
    ['dead', 'dead', 'dead'],
    ['reused', 'reused', 'reused'],
    ['dead', 'reused', 'dead'],
  ]
  for (const [siteState, childState, modelState] of safeCases) {
    const h = makeHarness()
    h.setRecord(record)
    state(h, record, siteState)
    state(h, { pid: record.childPid, processStartId: record.childProcessStartId }, childState)
    state(h, { pid: record.modelPid, processStartId: record.modelProcessStartId }, modelState)
    const result = h.guard.recover(FILE)
    assert.equal(result.ok, true)
    assert.equal(result.recordKind, 'worker')
  }

  const unsafeCases = [
    ['match', 'dead', 'dead', 'owner-site-active'],
    ['dead', 'match', 'dead', 'worker-child-active'],
    ['dead', 'unknown', 'dead', 'worker-child-unverified'],
    ['dead', 'dead', 'match', 'worker-model-active'],
    ['dead', 'dead', 'unknown', 'worker-model-unverified'],
  ]
  for (const [siteState, childState, modelState, code] of unsafeCases) {
    const h = makeHarness()
    h.setRecord(record)
    state(h, record, siteState)
    state(h, { pid: record.childPid, processStartId: record.childProcessStartId }, childState)
    state(h, { pid: record.modelPid, processStartId: record.modelProcessStartId }, modelState)
    const result = h.guard.recover(FILE)
    assert.equal(result.ok, false)
    assert.equal(result.code, code)
    assert.equal(h.calls.some((call) => call[0] === 'unlink'), false)
  }

  for (const containmentState of ['active', 'unknown', 'unverified']) {
    const h = makeHarness()
    h.setRecord(record)
    state(h, record, 'dead')
    state(h, { pid: record.childPid, processStartId: record.childProcessStartId }, 'dead')
    state(h, { pid: record.modelPid, processStartId: record.modelProcessStartId }, 'dead')
    h.setContainment(containmentState)
    const result = h.guard.recover(FILE)
    assert.equal(result.ok, false)
    assert.equal(result.code, containmentState === 'active'
      ? 'worker-containment-active' : 'worker-containment-unverified')
  }
})

check('worker pre-bind ambiguity is fail-closed except for an explicit GO-ordering proof', () => {
  const cases = [
    [worker({ childPid: null, childProcessStartId: null, modelPid: null, modelProcessStartId: null }), false],
    [worker({ childPid: 301, childProcessStartId: 'child:start:old', modelPid: null, modelProcessStartId: null }), false],
    [worker({ childPid: null, childProcessStartId: null, modelPid: 401, modelProcessStartId: 'model:start:old' }), false],
    [worker({ childPid: null, childProcessStartId: null, modelPid: null, modelProcessStartId: null, goState: 'not-sent' }), true],
  ]
  for (const [record, recoverable] of cases) {
    const h = makeHarness()
    h.setRecord(record)
    state(h, record, 'dead')
    const result = h.guard.recover(FILE)
    assert.equal(result.ok, recoverable)
    if (!recoverable) {
      assert.equal(result.code, 'worker-prebind-ambiguous')
      assert.equal(h.calls.some((call) => call[0] === 'unlink'), false)
    }
  }

  const h = makeHarness({ config: { proveNoSpawnPossible: () => false } })
  const record = worker({
    childPid: null, childProcessStartId: null, modelPid: null,
    modelProcessStartId: null, goState: 'not-sent',
  })
  h.setRecord(record)
  state(h, record, 'dead')
  assert.equal(h.guard.recover(FILE).code, 'worker-prebind-ambiguous')

  const partial = worker({
    childPid: 301, childProcessStartId: 'child:start:old',
    modelPid: null, modelProcessStartId: null,
  })
  const recovered = makeHarness({
    config: { proveUnboundModelCannotExecute: () => true },
  })
  recovered.setRecord(partial)
  state(recovered, partial, 'dead')
  state(recovered, { pid: partial.childPid, processStartId: partial.childProcessStartId }, 'dead')
  recovered.setContainment('drained')
  assert.equal(recovered.guard.recover(FILE).committed, true,
    'a pre-exec-gated null model pair is recoverable only after exact child and containment drain proofs')

  const unproved = makeHarness({
    config: { proveUnboundModelCannotExecute: () => true },
  })
  unproved.setRecord(partial)
  state(unproved, partial, 'dead')
  state(unproved, { pid: partial.childPid, processStartId: partial.childProcessStartId }, 'dead')
  unproved.setContainment('unverified')
  assert.equal(unproved.guard.recover(FILE).code, 'worker-containment-unverified')
})

check('unsafe privacy, unstable proof, non-canonical JSON, and ambiguous validators fail closed', () => {
  const scenarios = [
    (h) => h.setRecord(owner({ ...LOCAL_SITE, token: 'mine' }), { mode: 0o100644, modeExact: String(0o100644) }),
    (h) => h.setRecord(owner({ ...LOCAL_SITE, token: 'mine' }), { nlink: '2' }),
    (h) => { h.setRecord(owner({ ...LOCAL_SITE, token: 'mine' })); h.setObservedOverride({ ...h.current.stat, uid: 777 }) },
    (h) => { h.setRecord(owner({ ...LOCAL_SITE, token: 'mine' })); h.setObservedOverride({ ...h.current.stat, ino: '9999' }) },
    (h) => h.setRaw(` ${JSON.stringify(owner({ ...LOCAL_SITE, token: 'mine' }))}\n`),
    (h) => h.setRaw('{broken json\n'),
  ]
  for (const arrange of scenarios) {
    const h = makeHarness()
    arrange(h)
    const result = h.guard.release(FILE, 'mine')
    assert.equal(result.ok, false)
    assert.equal(h.calls.some((call) => call[0] === 'unlink'), false)
  }

  const h = makeHarness({
    config: {
      validateOwner: () => true,
      validateWorker: () => true,
    },
  })
  h.setRecord(owner({ ...LOCAL_SITE, token: 'mine' }))
  assert.equal(h.guard.release(FILE, 'mine').code, 'owner-record-invalid')
})

check('Windows requires the injected exact private-path proof', () => {
  const h = makeHarness({ platform: 'win32' })
  h.setRecord(owner({ ...LOCAL_SITE, token: 'mine' }))
  h.setPrivacyState('unsafe')
  assert.equal(h.guard.release(FILE, 'mine').code, 'owner-privacy-unsafe')
  assert.equal(h.calls.some((call) => call[0] === 'unlink'), false)
  h.setPrivacyState('private')
  assert.equal(h.guard.release(FILE, 'mine').committed, true)

  assert.throws(() => ownerGuard.create({
    fileGuards: {}, root: ROOT, maxBytes: 10, hostname: 'local',
  }), /exact file-guard operations/)
})

check('reconcile failure, missing target, and paths outside the authority stay non-committed', () => {
  const h = makeHarness()
  h.setRecord(owner({ ...LOCAL_SITE, token: 'mine' }))
  h.setReconcileResult({ ok: false, code: 'directory-entry-limit' })
  const failed = h.guard.release(FILE, 'mine')
  assert.equal(failed.code, 'owner-guard-reconcile-failed')
  assert.equal(failed.uncertain, true)
  assert.equal(h.calls.some((call) => call[0] === 'read'), false)

  const missingHarness = makeHarness()
  const missing = missingHarness.guard.release(FILE, 'mine')
  assert.equal(missing.ok, false)
  assert.equal(missing.alreadyMissing, true)
  assert.equal(missing.uncertain, true)

  for (const unsafe of ['relative.json', '/authority-foreign/owner.json', '/authority']) {
    const pathHarness = makeHarness()
    const result = pathHarness.guard.release(unsafe, 'mine')
    assert.equal(result.code, 'owner-path-unsafe')
    assert.equal(pathHarness.calls.length, 0)
  }
})

check('module has no process signalling or direct filesystem mutation primitive', () => {
  const source = readFileSync(new URL('../server/shallow-owner-guard.js', import.meta.url), 'utf8')
  assert.equal(/process\.kill|killSync|unlinkSync|renameSync|rmSync|writeFileSync|chmodSync/.test(source), false)
  assert.match(source, /unlinkRegularFileMatchingResultUnder/)
  assert.match(source, /reconcileGuardTransactionsUnder/)
})

console.log(`shallow owner guard tests passed (${checks} checks)`)
