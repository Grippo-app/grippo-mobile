#!/usr/bin/env node

import assert from 'node:assert/strict'
import childProcess from 'node:child_process'
import { createRequire } from 'node:module'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, renameSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const require = createRequire(import.meta.url)
process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE = '1'
const fileGuards = require('../server/file-guards.js')
const root = mkdtempSync(join(tmpdir(), 'writer-lease-primitives-'))
const parent = join(root, 'orchestrator', '.cache', 'tasks', 'finalizations')
const writers = join(parent, '.writers')
mkdirSync(writers, { recursive: true })
let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  const directoryProof = fileGuards.writerLeaseDirectoryProofUnder(root, writers)
  check('writer lease directory proof retains exact bigint decimal fields', () => {
    assert.ok(directoryProof)
    for (const field of ['dev', 'ino', 'mode', 'nlink', 'size', 'mtimeNs', 'ctimeNs']) {
      assert.match(directoryProof[field], /^\d+$/)
    }
    assert.equal(directoryProof.type, 'directory')
  })

  const source = join(writers, '.wr-' + 'a'.repeat(20) + '.' + '1'.repeat(32) + '.mutation-stage')
  const target = join(writers, '.wr-' + 'a'.repeat(20) + '.mutation-lock')
  const bytes = Buffer.from('{"exact":"stage"}\n')
  let staged
  check('fsync stage, exact read, and no-clobber transfer preserve one proven inode', () => {
    staged = fileGuards.writerLeaseStageUnder(root, writers, source, bytes, {
      directoryProof, maxBytes: 4096, mode: 0o600
    })
    assert.equal(staged.ok, true)
    assert.equal(staged.proof.nlink, '1')
    const read = fileGuards.writerLeaseReadUnder(root, writers, source, 4096, directoryProof)
    assert.deepEqual(read.bytes, bytes)
    assert.deepEqual(read.proof, staged.proof)
    const moved = fileGuards.writerLeaseLinkUnder(root, writers, source, target, {
      directoryProof, sourceProof: staged.proof, removeSource: true, maxBytes: 4096,
      capture: join(writers, '.wr-' + 'a'.repeat(20) + '.' + '1'.repeat(32) + '.manifest.mutation-cleanup')
    })
    assert.equal(moved.ok, true)
    assert.equal(existsSync(source), false)
    assert.equal(readFileSync(target, 'utf8'), bytes.toString('utf8'))
    assert.equal(moved.proof.dev, staged.proof.dev)
    assert.equal(moved.proof.ino, staged.proof.ino)
    assert.equal(moved.proof.nlink, '1')
  })

  check('wrong bigint proof and no-clobber target preserve both generations', () => {
    const wrong = { ...fileGuards.writerLeaseReadUnder(root, writers, target, 4096, directoryProof).proof,
      ino: '9007199254740993' }
    const refused = fileGuards.writerLeaseLinkUnder(root, writers, target, join(writers, '.refused'), {
      directoryProof, sourceProof: wrong, removeSource: false, maxBytes: 4096
    })
    assert.equal(refused.ok, false)
    const foreign = join(writers, '.foreign')
    writeFileSync(foreign, 'foreign\n')
    const live = fileGuards.writerLeaseReadUnder(root, writers, target, 4096, directoryProof)
    const collision = fileGuards.writerLeaseLinkUnder(root, writers, target, foreign, {
      directoryProof, sourceProof: live.proof, removeSource: false, maxBytes: 4096
    })
    assert.equal(collision.ok, false)
    assert.equal(readFileSync(foreign, 'utf8'), 'foreign\n')
    rmSync(foreign)
  })

  check('root and every ancestor component use exact decimal bigint proofs', () => {
    const file = join(writers, '.writer-chain-proof-fixture')
    const prepared = fileGuards.writerLeaseStageUnder(root, writers, file, Buffer.from('chain-proof\n'), {
      directoryProof, maxBytes: 4096, mode: 0o600
    })
    assert.equal(prepared.ok, true)
    const originalSpawnSync = childProcess.spawnSync
    let inspected = false
    childProcess.spawnSync = function (command, args, options) {
      const request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null
      if (request && request.action === 'writer-read') {
        inspected = true
        for (const field of ['dev', 'ino', 'mode']) assert.match(request.writerRootProof[field], /^\d+$/)
        assert.equal(request.writerComponentProofs.length > 1, true)
        for (const proof of request.writerComponentProofs) {
          assert.ok(proof)
          for (const field of ['dev', 'ino', 'mode']) assert.match(proof[field], /^\d+$/)
        }
        request.writerComponentProofs.at(-1).ino =
          (BigInt(request.writerComponentProofs.at(-1).ino) + 9007199254740992n).toString()
        options = { ...options, input: JSON.stringify(request) }
      }
      return originalSpawnSync.call(childProcess, command, args, options)
    }
    try {
      assert.equal(fileGuards.writerLeaseReadUnder(root, writers, file, 4096, directoryProof), null)
      assert.equal(inspected, true)
      assert.equal(readFileSync(file, 'utf8'), 'chain-proof\n')
    } finally {
      childProcess.spawnSync = originalSpawnSync
      rmSync(file, { force: true })
    }
  })

  check('exact deletion uses a deterministic quarantine and never accepts a changed proof', () => {
    const live = fileGuards.writerLeaseReadUnder(root, writers, target, 4096, directoryProof)
    const quarantine = join(writers, '.wr-' + 'a'.repeat(20) + '.' + '1'.repeat(32) + '.lock.mutation-cleanup')
    const denied = fileGuards.writerLeaseDeleteUnder(root, writers, target, quarantine, {
      directoryProof, proof: { ...live.proof, size: (BigInt(live.proof.size) + 1n).toString() }, maxBytes: 4096,
      capture: join(writers, '.wr-' + 'a'.repeat(20) + '.' + '1'.repeat(32) + '.lock.mutation-capture')
    })
    assert.equal(denied.ok, false)
    const deleted = fileGuards.writerLeaseDeleteUnder(root, writers, target, quarantine, {
      directoryProof, proof: live.proof, maxBytes: 4096,
      capture: join(writers, '.wr-' + 'a'.repeat(20) + '.' + '1'.repeat(32) + '.lock.mutation-capture')
    })
    assert.equal(deleted.ok, true)
    assert.equal(existsSync(target), false)
    assert.equal(existsSync(quarantine), false)
  })

  check('capture boundary preserves a foreign public replacement instead of unlinking it', () => {
    const racingSource = join(writers, '.wr-' + 'c'.repeat(20) + '.' + '3'.repeat(32) + '.mutation-stage')
    const racingTarget = join(writers, '.wr-' + 'c'.repeat(20) + '.mutation-lock')
    const capture = join(writers, '.wr-' + 'c'.repeat(20) + '.' + '3'.repeat(32) + '.manifest.mutation-cleanup')
    const original = Buffer.from('owned-generation\n')
    const foreign = Buffer.from('foreign-generation\n')
    const prepared = fileGuards.writerLeaseStageUnder(root, writers, racingSource, original, {
      directoryProof, maxBytes: 4096, mode: 0o600
    })
    try {
      const moved = fileGuards.writerLeaseLinkUnder(root, writers, racingSource, racingTarget, {
        directoryProof, sourceProof: prepared.proof, removeSource: true, capture, maxBytes: 4096,
        testForeignBytes: foreign.toString('base64')
      })
      assert.equal(moved.ok, false)
      assert.equal(moved.code, 'source-replaced')
      assert.deepEqual(readFileSync(racingSource), foreign)
      assert.deepEqual(readFileSync(racingTarget), original)
      assert.deepEqual(readFileSync(capture), original)
    } finally {
      rmSync(racingSource, { force: true }); rmSync(racingTarget, { force: true }); rmSync(capture, { force: true })
    }
  })

  check('replacement immediately before capture is restored no-clobber to its public name', () => {
    const source = join(writers, '.wr-' + 'd'.repeat(20) + '.' + '4'.repeat(32) + '.mutation-stage')
    const target = join(writers, '.wr-' + 'd'.repeat(20) + '.mutation-lock')
    const capture = join(writers, '.wr-' + 'd'.repeat(20) + '.' + '4'.repeat(32) + '.manifest.mutation-capture')
    const original = Buffer.from('original-before-capture\n')
    const foreign = Buffer.from('foreign-before-capture\n')
    const prepared = fileGuards.writerLeaseStageUnder(root, writers, source, original, {
      directoryProof, maxBytes: 4096, mode: 0o600
    })
    const moved = fileGuards.writerLeaseLinkUnder(root, writers, source, target, {
      directoryProof, sourceProof: prepared.proof, removeSource: true, capture, maxBytes: 4096,
      testReplaceBeforeCaptureBytes: foreign.toString('base64')
    })
    assert.equal(moved.ok, false)
    assert.equal(moved.code, 'source-replaced')
    assert.deepEqual(readFileSync(source), foreign)
    assert.deepEqual(readFileSync(capture), foreign)
    assert.deepEqual(readFileSync(target), original)
    rmSync(source, { force: true }); rmSync(capture, { force: true }); rmSync(target, { force: true })

    const deleting = join(writers, '.wr-' + 'e'.repeat(20) + '.mutation-lock')
    const quarantine = join(writers, '.wr-' + 'e'.repeat(20) + '.' + '5'.repeat(32) + '.lock.mutation-cleanup')
    const deleteCapture = join(writers, '.wr-' + 'e'.repeat(20) + '.' + '5'.repeat(32) + '.lock.mutation-capture')
    const deletePrepared = fileGuards.writerLeaseStageUnder(root, writers, deleting, original, {
      directoryProof, maxBytes: 4096, mode: 0o600
    })
    const deleted = fileGuards.writerLeaseDeleteUnder(root, writers, deleting, quarantine, {
      directoryProof, proof: deletePrepared.proof, capture: deleteCapture, maxBytes: 4096,
      testReplaceBeforeCaptureBytes: foreign.toString('base64')
    })
    assert.equal(deleted.ok, false)
    assert.equal(deleted.code, 'source-replaced')
    assert.deepEqual(readFileSync(deleting), foreign)
    assert.deepEqual(readFileSync(deleteCapture), foreign)
    assert.deepEqual(readFileSync(quarantine), original)
    rmSync(deleting, { force: true }); rmSync(deleteCapture, { force: true }); rmSync(quarantine, { force: true })
  })

  check('ancestor replacement cannot redirect stage bytes into a foreign writer directory', () => {
    const displaced = join(root, 'finalizations-displaced')
    const outside = join(root, 'outside')
    mkdirSync(join(outside, '.writers'), { recursive: true })
    const originalSpawnSync = childProcess.spawnSync
    let swapped = false
    childProcess.spawnSync = function (command, args, options) {
      const request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null
      if (!swapped && request && request.action === 'writer-stage') {
        swapped = true
        renameSync(parent, displaced)
        symlinkSync(outside, parent, 'dir')
      }
      return originalSpawnSync.call(childProcess, command, args, options)
    }
    try {
      const result = fileGuards.writerLeaseStageUnder(root, writers, join(writers, '.wr-' + 'b'.repeat(20) + '.' + '2'.repeat(32) + '.mutation-stage'), bytes, {
        directoryProof, maxBytes: 4096, mode: 0o600
      })
      assert.equal(result.ok, false)
      assert.equal(swapped, true)
      assert.deepEqual(readdirSync(join(outside, '.writers')), [])
    } finally {
      childProcess.spawnSync = originalSpawnSync
      rmSync(parent, { recursive: true, force: true })
      renameSync(displaced, parent)
    }
  })

  console.log(`writer-lease-primitives: ${checks} checks passed`)
} finally {
  delete process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE
  rmSync(root, { recursive: true, force: true })
}
