#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import {
  chmodSync, existsSync, linkSync, mkdirSync, mkdtempSync, readFileSync,
  readdirSync, renameSync, rmSync, unlinkSync, writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const childProcess = require('node:child_process')
const originalSpawnSync = childProcess.spawnSync
process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE = '1'
const guards = require('../server/file-guards.js')

const base = mkdtempSync(join(tmpdir(), 'file-guard-publication-'))
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

function hookRequest(action, mutate, operation) {
  let hits = 0
  childProcess.spawnSync = function (command, args, options) {
    let request = null
    try { request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null }
    catch {}
    if (request && request.action === action) {
      hits++
      mutate(request)
      options = { ...options, input: JSON.stringify(request) }
    }
    return originalSpawnSync.call(childProcess, command, args, options)
  }
  try {
    const result = operation()
    assert.ok(hits > 0, `${action} worker hook was not reached`)
    return result
  } finally { childProcess.spawnSync = originalSpawnSync }
}

function inject(action, field, value, operation) {
  return hookRequest(action, (request) => { request[field] = value }, operation)
}

function guardEntries() {
  return readdirSync(directory).filter((name) => name.startsWith('.guard-')).sort()
}

function reconcile(maxTransactions = 100) {
  return guards.reconcileGuardTransactionsUnder(root, directory, {
    maxEntries: 10_000, maxTransactions,
  })
}

function sha(bytes) {
  return `sha256:${createHash('sha256').update(bytes).digest('hex')}`
}

function privateFile(file, bytes) {
  writeFileSync(file, bytes, { mode: 0o600 })
  chmodSync(file, 0o600)
}

function crashPublish(file, bytes, point) {
  const result = inject('publish-no-clobber', 'testCrashAt', point, () =>
    guards.publishNoClobberRegularFileUnder(root, directory, file, bytes, {
      maxBytes: 4096, mode: 0o600,
    }))
  assert.equal(result.ok, false, point)
}

function crashCas(file, proof, before, after, point) {
  const result = inject('compare-and-swap', 'testCrashAt', point, () =>
    guards.compareAndSwapRegularFileUnder(root, directory, file, 4096,
      { bytes: before, proof }, after, { mode: 0o600 }))
  assert.equal(result.ok, false, point)
}

function crashAppend(file, bytes, point) {
  const result = inject('append-bounded', 'testCrashAt', point, () =>
    guards.appendBoundedRegularFileUnder(root, directory, file, bytes, {
      create: false, maxBytes: 4096, maxAppendBytes: 256, mode: 0o600,
    }))
  assert.equal(result.ok, false, point)
}

try {
  check('target evidence naming exposes deterministic WALs and classifies only scoped entries', () => {
    const file = join(directory, 'evidence-target.json')
    const evidence = guards.guardTransactionEvidenceForTarget(directory, file)
    const key = createHash('sha256').update('evidence-target.json', 'utf8').digest('hex')
    assert.equal(evidence.version, 1)
    assert.equal(evidence.target, file)
    assert.equal(evidence.directory, directory)
    assert.equal(evidence.name, 'evidence-target.json')
    assert.equal(evidence.entries.length, 12)
    assert.ok(evidence.entries.every((entry) => entry.path === join(directory, entry.name)))
    assert.ok(evidence.entries.some((entry) =>
      entry.kind === 'delete' && entry.role === 'decision-stage' &&
      entry.name === `.guard-txn-${key}.decision.json.stage`))
    const receiptName = `.guard-publish-${key}.receipt.json`
    assert.deepEqual(guards.classifyGuardTransactionEvidenceForTarget(directory, file, receiptName), {
      kind: 'publication', role: 'receipt', name: receiptName, path: join(directory, receiptName),
    })
    const atomicName = `.evidence-target.json-123-${'a'.repeat(24)}.tmp`
    assert.deepEqual(guards.classifyGuardTransactionEvidenceForTarget(directory, file, atomicName), {
      kind: 'atomic', role: 'stage', name: atomicName, path: join(directory, atomicName),
    })
    assert.equal(guards.classifyGuardTransactionEvidenceForTarget(
      directory, file, '.guard-publish-foreign.receipt.json'), null)
    assert.equal(guards.guardTransactionEvidenceForTarget(
      directory, join(directory, 'nested', 'evidence-target.json')), null)
  })

  check('structured matching unlink proves an exact commit and preserves replacement owners', () => {
    const direct = join(directory, 'unlink-result-direct.json')
    privateFile(direct, 'direct-owner\n')
    const directProof = guards.statRegularFileUnder(root, directory, direct)
    const directResult = guards.unlinkRegularFileMatchingResultUnder(
      root, directory, direct, 4096, { bytes: Buffer.from('direct-owner\n'), proof: directProof })
    assert.deepEqual(directResult, {
      ok: true, code: 'deleted', outcome: 'deleted', committed: true,
      alreadyMissing: false, uncertain: false,
    })
    assert.equal(existsSync(direct), false)

    const crashed = join(directory, 'unlink-result-recovered.json')
    privateFile(crashed, 'old-owner\n')
    const crashedProof = guards.statRegularFileUnder(root, directory, crashed)
    const interrupted = inject('quarantine-unlink-matching', 'testCrashAt',
      'guard:after-detach', () => guards.unlinkRegularFileMatchingResultUnder(
        root, directory, crashed, 4096,
        { bytes: Buffer.from('old-owner\n'), proof: crashedProof }))
    assert.equal(interrupted.ok, false)
    assert.equal(existsSync(crashed), false)
    privateFile(crashed, 'replacement-owner\n')
    const recovered = guards.unlinkRegularFileMatchingResultUnder(
      root, directory, crashed, 4096,
      { bytes: Buffer.from('old-owner\n'), proof: crashedProof })
    assert.equal(recovered.ok, true)
    assert.equal(recovered.committed, true)
    assert.equal(recovered.code, 'deleted')
    assert.equal(readFileSync(crashed, 'utf8'), 'replacement-owner\n')
    unlinkSync(crashed)

    const replaced = join(directory, 'unlink-result-replaced.json')
    privateFile(replaced, 'stale-owner\n')
    const replacedProof = guards.statRegularFileUnder(root, directory, replaced)
    unlinkSync(replaced)
    privateFile(replaced, 'foreign-owner\n')
    const replacementResult = guards.unlinkRegularFileMatchingResultUnder(
      root, directory, replaced, 4096,
      { bytes: Buffer.from('stale-owner\n'), proof: replacedProof })
    assert.equal(replacementResult.ok, false)
    assert.equal(replacementResult.committed, false)
    assert.equal(replacementResult.code, 'target-proof-mismatch')
    assert.equal(readFileSync(replaced, 'utf8'), 'foreign-owner\n')
    unlinkSync(replaced)

    const unproven = join(directory, 'unlink-result-unproven.json')
    privateFile(unproven, 'unproven-owner\n')
    const unprovenProof = guards.statRegularFileUnder(root, directory, unproven)
    unlinkSync(unproven)
    const missingResult = guards.unlinkRegularFileMatchingResultUnder(
      root, directory, unproven, 4096,
      { bytes: Buffer.from('unproven-owner\n'), proof: unprovenProof })
    assert.deepEqual(missingResult, {
      ok: false, code: 'delete-commit-unproven', outcome: 'not-committed', committed: false,
      alreadyMissing: true, uncertain: true,
    })
    assert.equal(guards.unlinkRegularFileMatchingUnder(
      root, directory, unproven, 4096,
      { bytes: Buffer.from('unproven-owner\n'), proof: unprovenProof }), true)
    assert.deepEqual(guardEntries(), [])
  })

  check('structured atomic replace self-recognizes its exact uncertain commit and rejects a foreign generation', () => {
    const recognizedFile = join(directory, 'atomic-self-recognized.json')
    const recognized = inject('atomic-replace', 'testForceAtomicUncertainAfterVerification', true, () =>
      guards.atomicReplaceRegularFileResult(root, directory, recognizedFile, 'recognized\n', {
        maxBytes: 4096, mode: 0o600,
      }))
    assert.equal(recognized.ok, true)
    assert.equal(recognized.code, 'published-self-recognized')
    assert.equal(recognized.selfRecognized, true)
    assert.equal(recognized.uncertain, false)
    assert.equal(readFileSync(recognizedFile, 'utf8'), 'recognized\n')

    const foreignFile = join(directory, 'atomic-foreign.json')
    const foreign = inject('atomic-replace', 'testReplaceAfterPublishBytes',
      Buffer.from('foreign-atomic\n').toString('base64'), () =>
        guards.atomicReplaceRegularFileResult(root, directory, foreignFile, 'owned-atomic\n', {
          maxBytes: 4096, mode: 0o600,
        }))
    assert.equal(foreign.ok, false)
    assert.equal(foreign.code, 'published-unverified')
    assert.equal(foreign.uncertain, true)
    assert.equal(foreign.selfRecognized, false)
    assert.equal(readFileSync(foreignFile, 'utf8'), 'foreign-atomic\n')
  })

  check('exact CAS accepts bytes or SHA-256 with a full proof and rejects stale authority', () => {
    const bytesFile = join(directory, 'cas-bytes.json')
    privateFile(bytesFile, 'old-bytes\n')
    const bytesProof = guards.statRegularFileUnder(root, directory, bytesFile)
    const bytesResult = guards.compareAndSwapRegularFileUnder(root, directory, bytesFile, 4096,
      { bytes: Buffer.from('old-bytes\n'), proof: bytesProof }, 'new-bytes\n', { mode: 0o600 })
    assert.equal(bytesResult.ok, true)
    assert.equal(bytesResult.stat.nlink, '1')
    assert.equal(readFileSync(bytesFile, 'utf8'), 'new-bytes\n')

    const hashFile = join(directory, 'cas-hash.json')
    privateFile(hashFile, 'old-hash\n')
    const hashProof = guards.statRegularFileUnder(root, directory, hashFile)
    const hashResult = guards.compareAndSwapRegularFileUnder(root, directory, hashFile, 4096,
      { sha256: sha(Buffer.from('old-hash\n')), proof: hashProof }, 'new-hash\n', { mode: 0o600 })
    assert.equal(hashResult.ok, true)
    assert.equal(readFileSync(hashFile, 'utf8'), 'new-hash\n')

    const stale = join(directory, 'cas-stale.json')
    privateFile(stale, 'old-generation\n')
    const staleProof = guards.statRegularFileUnder(root, directory, stale)
    unlinkSync(stale)
    privateFile(stale, 'foreign-generation\n')
    const staleResult = guards.compareAndSwapRegularFileUnder(root, directory, stale, 4096,
      { bytes: Buffer.from('foreign-generation\n'), proof: staleProof }, 'forbidden\n', { mode: 0o600 })
    assert.equal(staleResult.ok, false)
    assert.equal(readFileSync(stale, 'utf8'), 'foreign-generation\n')

    const mismatchProof = guards.statRegularFileUnder(root, directory, stale)
    const mismatchResult = guards.compareAndSwapRegularFileUnder(root, directory, stale, 4096,
      { bytes: Buffer.from('not-the-file\n'), proof: mismatchProof }, 'forbidden\n', { mode: 0o600 })
    assert.equal(mismatchResult.ok, false)
    assert.equal(readFileSync(stale, 'utf8'), 'foreign-generation\n')
    assert.equal(guards.compareAndSwapRegularFileUnder(root, directory, stale, 4096,
      { bytes: Buffer.from('foreign-generation\n') }, 'forbidden\n').code, 'invalid-proof')
    assert.deepEqual(guardEntries(), [])
  })

  check('no-clobber publication forward-recovers every durable WAL and identity boundary', () => {
    const points = [
      'guard:after-publish-manifest-stage', 'guard:after-publish-manifest',
      'guard:after-publish-data-create', 'guard:after-publish-data-fsync',
      'guard:before-publish-target-link', 'guard:after-publish-target-link',
      'guard:after-publish-link-stage', 'guard:after-publish-link-record',
      'guard:before-publish-stage-remove', 'guard:after-publish-stage-remove',
      'guard:after-publish-receipt-stage', 'guard:after-publish-receipt',
      'guard:after-publish-link-unlink', 'guard:after-publish-manifest-unlink',
      'guard:after-publish-receipt-unlink',
    ]
    for (const [index, point] of points.entries()) {
      const file = join(directory, `publish-crash-${index}.json`)
      const bytes = `published-${index}\n`
      crashPublish(file, bytes, point)
      const summary = reconcile()
      assert.equal(summary.ok, true, `${point}: ${JSON.stringify(summary)}`)
      assert.equal(readFileSync(file, 'utf8'), bytes, point)
      assert.deepEqual(guardEntries(), [], point)
    }
  })

  check('exact CAS forward-recovers every capture, detach, link, cleanup, and terminal boundary', () => {
    const points = [
      'guard:after-publish-manifest-stage', 'guard:after-publish-manifest',
      'guard:after-publish-data-create', 'guard:after-publish-data-fsync',
      'guard:after-cas-old-link', 'guard:before-cas-old-detach',
      'guard:after-cas-old-detach', 'guard:before-publish-target-link',
      'guard:after-publish-target-link', 'guard:after-publish-link-stage',
      'guard:after-publish-link-record', 'guard:before-publish-stage-remove',
      'guard:after-publish-stage-remove', 'guard:before-cas-old-capture-remove',
      'guard:after-cas-old-capture-remove', 'guard:after-publish-receipt-stage',
      'guard:after-publish-receipt', 'guard:after-publish-link-unlink',
      'guard:after-publish-manifest-unlink', 'guard:after-publish-receipt-unlink',
    ]
    for (const [index, point] of points.entries()) {
      const file = join(directory, `cas-crash-${index}.json`)
      const before = Buffer.from(`cas-old-${index}\n`)
      const after = Buffer.from(`cas-new-${index}\n`)
      privateFile(file, before)
      const proof = guards.statRegularFileUnder(root, directory, file)
      crashCas(file, proof, before, after, point)
      const summary = reconcile()
      assert.equal(summary.ok, true, `${point}: ${JSON.stringify(summary)}`)
      assert.equal(readFileSync(file, 'utf8'), after.toString(), point)
      assert.deepEqual(guardEntries(), [], point)
    }
  })

  check('bounded append inherits durable CAS recovery without duplicating or partially writing its payload', () => {
    const points = [
      'guard:after-publish-manifest', 'guard:after-publish-data-fsync',
      'guard:after-cas-old-link', 'guard:after-cas-old-detach',
      'guard:after-publish-target-link', 'guard:after-publish-link-record',
      'guard:after-publish-stage-remove', 'guard:after-cas-old-capture-remove',
      'guard:after-publish-receipt', 'guard:after-publish-manifest-unlink',
    ]
    for (const [index, point] of points.entries()) {
      const file = join(directory, `append-crash-${index}.log`)
      const before = `append-before-${index}\n`
      const appended = `append-owned-${index}\n`
      privateFile(file, before)
      crashAppend(file, appended, point)
      const summary = reconcile()
      assert.equal(summary.ok, true, `${point}: ${JSON.stringify(summary)}`)
      assert.equal(readFileSync(file, 'utf8'), before + appended, point)
      assert.deepEqual(guardEntries(), [], point)
    }
  })

  check('ordinary same-request retry recognizes a prior publish or CAS intent after a crash', () => {
    const publish = join(directory, 'publish-retry.json')
    crashPublish(publish, 'retry-publish\n', 'guard:after-publish-data-create')
    const retriedPublish = guards.publishNoClobberRegularFileUnder(
      root, directory, publish, 'retry-publish\n', { maxBytes: 4096, mode: 0o600 })
    assert.equal(retriedPublish.ok, true)

    const cas = join(directory, 'cas-retry.json')
    privateFile(cas, 'retry-old\n')
    const proof = guards.statRegularFileUnder(root, directory, cas)
    crashCas(cas, proof, Buffer.from('retry-old\n'), Buffer.from('retry-new\n'),
      'guard:after-cas-old-detach')
    const retriedCas = guards.compareAndSwapRegularFileUnder(root, directory, cas, 4096,
      { bytes: Buffer.from('retry-old\n'), proof }, 'retry-new\n', { mode: 0o600 })
    assert.equal(retriedCas.ok, true)
    assert.equal(readFileSync(cas, 'utf8'), 'retry-new\n')
    assert.deepEqual(guardEntries(), [])
  })

  check('foreign public generations win before or after publish link, including byte-identical replacements', () => {
    const foreign = Buffer.from('foreign-owner\n').toString('base64')
    for (const [name, hook] of [
      ['publish-foreign-before.json', 'testReplaceBeforePublishLinkBytes'],
      ['publish-foreign-after.json', 'testReplaceAfterPublishBytes'],
    ]) {
      const file = join(directory, name)
      const result = inject('publish-no-clobber', hook, foreign, () =>
        guards.publishNoClobberRegularFileUnder(root, directory, file, 'owned-owner\n', {
          maxBytes: 4096, mode: 0o600,
        }))
      assert.equal(result.ok, false)
      assert.equal(readFileSync(file, 'utf8'), 'foreign-owner\n')
      assert.deepEqual(guardEntries(), [])
    }

    const identical = join(directory, 'publish-identical-foreign.json')
    const desired = Buffer.from('identical-owner\n')
    const identicalResult = inject('publish-no-clobber', 'testReplaceBeforePublishLinkBytes',
      desired.toString('base64'), () => guards.publishNoClobberRegularFileUnder(
        root, directory, identical, desired, { maxBytes: 4096, mode: 0o600 }))
    assert.equal(identicalResult.ok, false)
    assert.equal(readFileSync(identical, 'utf8'), desired.toString())
    assert.deepEqual(guardEntries(), [])
  })

  check('the first durable target-keyed publication intent wins over a competing payload', () => {
    const file = join(directory, 'publish-competing.json')
    crashPublish(file, 'first-intent\n', 'guard:after-publish-manifest')
    const second = guards.publishNoClobberRegularFileUnder(
      root, directory, file, 'second-intent\n', { maxBytes: 4096, mode: 0o600 })
    assert.equal(second.ok, false)
    assert.equal(second.code, 'exists')
    assert.equal(readFileSync(file, 'utf8'), 'first-intent\n')
    assert.ok(guardEntries().some((name) => name.endsWith('.receipt.json')),
      'a non-creator helper retains terminal proof until creator/reconciler settlement')
    assert.equal(reconcile().ok, true)
    assert.deepEqual(guardEntries(), [])
  })

  check('CAS preserves a foreign public owner and retained old evidence, then forward-commits when the name clears', () => {
    for (const [index, hook] of [
      'testReplaceBeforeCasDetachBytes', 'testReplaceAfterPublishBytes',
    ].entries()) {
      const file = join(directory, `cas-foreign-${index}.json`)
      privateFile(file, `owned-old-${index}\n`)
      const proof = guards.statRegularFileUnder(root, directory, file)
      const result = inject('compare-and-swap', hook,
        Buffer.from(`foreign-public-${index}\n`).toString('base64'), () =>
          guards.compareAndSwapRegularFileUnder(root, directory, file, 4096,
            { bytes: Buffer.from(`owned-old-${index}\n`), proof },
            `owned-new-${index}\n`, { mode: 0o600 }))
      assert.equal(result.ok, false)
      assert.equal(readFileSync(file, 'utf8'), `foreign-public-${index}\n`)
      assert.ok(guardEntries().some((name) => name.startsWith('.guard-cas-old-')))
      const blocked = reconcile()
      assert.equal(blocked.ok, false)
      assert.equal(blocked.codes['cas-foreign-public'], 1)
      assert.equal(readFileSync(file, 'utf8'), `foreign-public-${index}\n`)
      unlinkSync(file)
      const recovered = reconcile()
      assert.equal(recovered.ok, true, JSON.stringify(recovered))
      assert.equal(readFileSync(file, 'utf8'), `owned-new-${index}\n`)
      assert.deepEqual(guardEntries(), [])
    }
  })

  check('reconciliation counts a publish data stage and CAS old capture as their WAL groups', () => {
    const publish = join(directory, 'reconcile-publish.json')
    crashPublish(publish, 'reconcile-publish\n', 'guard:after-publish-data-fsync')

    const cas = join(directory, 'reconcile-cas.json')
    privateFile(cas, 'reconcile-old\n')
    const proof = guards.statRegularFileUnder(root, directory, cas)
    crashCas(cas, proof, Buffer.from('reconcile-old\n'), Buffer.from('reconcile-new\n'),
      'guard:after-cas-old-link')

    const orphanId = 'f'.repeat(32)
    privateFile(join(directory, `.guard-publish-data-${orphanId}`), 'orphan-stage\n')
    privateFile(join(directory, `.guard-cas-old-${orphanId}`), 'orphan-old\n')

    const capped = reconcile(2)
    assert.equal(capped.ok, false)
    assert.equal(capped.code, 'scan-transaction-cap-exceeded')
    assert.equal(capped.transactions, 3)
    assert.equal(existsSync(publish), false)
    assert.equal(readFileSync(cas, 'utf8'), 'reconcile-old\n')

    const recovered = reconcile(3)
    assert.equal(recovered.ok, false)
    assert.equal(recovered.transactions, 3)
    assert.equal(recovered.reconciled, 2)
    assert.equal(recovered.pending, 1)
    assert.equal(recovered.codes['guard-capture-orphan'], 1)
    assert.equal(readFileSync(publish, 'utf8'), 'reconcile-publish\n')
    assert.equal(readFileSync(cas, 'utf8'), 'reconcile-new\n')
    unlinkSync(join(directory, `.guard-publish-data-${orphanId}`))
    unlinkSync(join(directory, `.guard-cas-old-${orphanId}`))
    assert.deepEqual(guardEntries(), [])
  })

  check('anchored directory pages make bounded deletion progress beyond 512 entries', () => {
    const paged = join(root, 'paged-cleanup')
    mkdirSync(paged, { mode: 0o700 })
    for (let index = 0; index < 1205; index++) {
      privateFile(join(paged, `scratch-${String(index).padStart(4, '0')}`), 'x')
    }
    let cursor = null
    let removed = 0
    for (let pageNumber = 0; pageNumber < 20; pageNumber++) {
      const page = guards.boundedDirectoryPageUnder(root, paged, {
        pageSize: 127, maxScanEntries: 2000, cursor,
      })
      assert.equal(page.ok, true, JSON.stringify(page))
      assert.ok(page.names.length <= 127)
      for (const name of page.names) unlinkSync(join(paged, name))
      removed += page.names.length
      cursor = page.nextCursor
      if (page.done) break
    }
    assert.equal(removed, 1205)
    assert.deepEqual(readdirSync(paged), [])
    assert.equal(cursor, null)

    for (const name of ['one', 'two', 'three']) privateFile(join(paged, name), name)
    const capped = guards.boundedDirectoryPageUnder(root, paged, {
      pageSize: 2, maxScanEntries: 2, cursor: null,
    })
    assert.equal(capped.ok, false)
    assert.equal(capped.code, 'scan-entry-cap-exceeded')
    assert.deepEqual(capped.names, [])
    const malformed = guards.boundedDirectoryPageUnder(root, paged, {
      pageSize: 2, maxScanEntries: 10, cursor: 'not-a-valid-cursor',
    })
    assert.equal(malformed.ok, false)
    assert.equal(malformed.code, 'cursor-invalid')

    const raced = inject('directory-page', 'testMutateDirectoryPageName', 'page-race', () =>
      guards.boundedDirectoryPageUnder(root, paged, {
        pageSize: 2, maxScanEntries: 10, cursor: null,
      }))
    assert.equal(raced.ok, false)
    assert.equal(raced.code, 'directory-changed')

    const first = guards.boundedDirectoryPageUnder(root, paged, {
      pageSize: 1, maxScanEntries: 10, cursor: null,
    })
    assert.equal(first.ok, true)
    assert.equal(first.done, false)
    const displaced = join(root, 'paged-cleanup-displaced')
    renameSync(paged, displaced)
    mkdirSync(paged, { mode: 0o700 })
    privateFile(join(paged, 'replacement'), 'replacement')
    const wrongDirectory = guards.boundedDirectoryPageUnder(root, paged, {
      pageSize: 1, maxScanEntries: 10, cursor: first.nextCursor,
    })
    assert.equal(wrongDirectory.ok, false)
    assert.equal(wrongDirectory.code, 'cursor-directory-mismatch')
    assert.equal(readFileSync(join(paged, 'replacement'), 'utf8'), 'replacement')
  })

  check('hardlink and publication-size boundaries fail closed without changing either generation', () => {
    const file = join(directory, 'cas-hardlink.json')
    const alias = join(directory, 'cas-hardlink-alias.json')
    privateFile(file, 'hardlinked-old\n')
    const proof = guards.statRegularFileUnder(root, directory, file)
    linkSync(file, alias)
    const result = guards.compareAndSwapRegularFileUnder(root, directory, file, 4096,
      { bytes: Buffer.from('hardlinked-old\n'), proof }, 'forbidden\n', { mode: 0o600 })
    assert.equal(result.ok, false)
    assert.equal(readFileSync(file, 'utf8'), 'hardlinked-old\n')
    assert.equal(readFileSync(alias, 'utf8'), 'hardlinked-old\n')

    const oversized = join(directory, 'publish-oversized.json')
    const tooLarge = Buffer.alloc(512 * 1024 + 1, 0x61)
    const oversizedResult = guards.publishNoClobberRegularFileUnder(
      root, directory, oversized, tooLarge, { maxBytes: tooLarge.length, mode: 0o600 })
    assert.equal(oversizedResult.ok, false)
    assert.equal(oversizedResult.code, 'too-large')
    assert.equal(existsSync(oversized), false)
    assert.deepEqual(guardEntries(), [])
  })

  console.log(`file-guards-publication-recovery: ${checks} checks passed`)
} finally {
  childProcess.spawnSync = originalSpawnSync
  rmSync(base, { recursive: true, force: true })
}
