#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import {
  chmodSync, existsSync, linkSync, lstatSync, mkdirSync, mkdtempSync, readFileSync, readdirSync,
  renameSync, rmSync, symlinkSync, unlinkSync, utimesSync, writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const childProcess = require('node:child_process')
const originalSpawnSync = childProcess.spawnSync
const base = mkdtempSync(join(tmpdir(), 'file-guards-bigint-'))
const root = join(base, 'project')
const directory = join(root, 'alpha', 'beta')
mkdirSync(directory, { recursive: true })
process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE = '1'
const fileGuards = require('../server/file-guards.js')
const FILE_GUARDS = fileURLToPath(new URL('../server/file-guards.js', import.meta.url))
const EXACT_KEYS = ['ctimeNs', 'dev', 'ino', 'mode', 'mtimeNs', 'nlink', 'size', 'type']
const DECIMAL = /^(?:0|[1-9][0-9]*)$/

let checks = 0
function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

function exactProof(value, type) {
  assert.deepEqual(Object.keys(value).sort(), [...EXACT_KEYS].sort())
  assert.equal(value.type, type)
  for (const field of ['ctimeNs', 'dev', 'ino', 'mode', 'mtimeNs', 'nlink', 'size']) {
    assert.equal(typeof value[field], 'string', field)
    assert.match(value[field], DECIMAL, field)
  }
}

function withWorkerHook(action, mutateRequest, mutateResponse, operation) {
  let hits = 0
  childProcess.spawnSync = function (command, args, options) {
    let request = null
    try { request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null }
    catch {}
    let nextOptions = options
    if (request && request.action === action) {
      hits++
      if (mutateRequest) mutateRequest(request)
      nextOptions = { ...options, input: JSON.stringify(request) }
    }
    const result = originalSpawnSync.call(childProcess, command, args, nextOptions)
    if (request && request.action === action && mutateResponse && result && typeof result.stdout === 'string') {
      const response = JSON.parse(result.stdout)
      mutateResponse(response)
      return { ...result, stdout: JSON.stringify(response) }
    }
    return result
  }
  try {
    const value = operation()
    assert.ok(hits > 0, `worker hook for ${action} was not reached`)
    return value
  } finally { childProcess.spawnSync = originalSpawnSync }
}

function injectRequestField(action, field, value, operation) {
  return withWorkerHook(action, (request) => { request[field] = value }, null, operation)
}

function foreignBytes() { return Buffer.from('foreign-generation\n').toString('base64') }

try {
  check('module import is inert and the public API remains stable', () => {
    const script = `
const assert = require('node:assert/strict');
const fs = require('node:fs');
const beforeCwd = process.cwd();
const before = fs.readdirSync(process.argv[1]).sort();
const guards = require(process.argv[2]);
assert.equal(process.cwd(), beforeCwd);
assert.deepEqual(fs.readdirSync(process.argv[1]).sort(), before);
for (const name of ['guardTransactionEvidenceForTarget','classifyGuardTransactionEvidenceForTarget',
  'boundedRegularFileUnder','statRegularFileUnder','atomicReplaceRegularFile','atomicReplaceRegularFileResult',
  'appendBoundedRegularFileUnder','publishNoClobberRegularFileUnder','compareAndSwapRegularFileUnder','unlinkRegularFileUnder',
  'unlinkRegularFileMatchingUnder','unlinkRegularFileMatchingResultUnder','unlinkRegularFileIfUnder','fsyncRegularFileUnder','fsyncDirectoryUnder',
  'boundedDirectoryNamesUnder','boundedDirectoryPageUnder','reconcileGuardTransactionsUnder','tailRegularFileUnder','transferFileNoClobberSameDirectoryUnder',
  'removeEmptyDirectoryUnder']) assert.equal(typeof guards[name], 'function', name);
`
    const result = originalSpawnSync(process.execPath, ['-e', script, root, FILE_GUARDS], { encoding: 'utf8' })
    assert.equal(result.status, 0, result.stderr + result.stdout)
  })

  check('root, every ancestor, directory, and file proofs are canonical exact decimal envelopes', () => {
    const file = join(directory, 'proof.json')
    writeFileSync(file, 'proof\n')
    const stat = withWorkerHook('stat-file', (request) => {
      exactProof(request.expected, 'directory')
      assert.equal(request.directoryParts.length, 2)
      assert.equal(request.componentProofs.length, 2)
      for (const proof of request.componentProofs) exactProof(proof, 'directory')
    }, (response) => {
      exactProof(response.directoryStat, 'directory')
      exactProof(response.stat, 'file')
    }, () => fileGuards.statRegularFileUnder(root, directory, file))
    assert.ok(stat)
    for (const field of ['dev', 'ino', 'nlink', 'mtimeNs', 'ctimeNs']) {
      assert.equal(typeof stat[field], 'string', field)
      assert.match(stat[field], DECIMAL, field)
    }
    assert.equal(typeof stat.mode, 'number')
    assert.equal(typeof stat.size, 'number')
    assert.equal(stat.sizeExact, String(Buffer.byteLength('proof\n')))
    assert.equal(stat.nlinkNumber, 1)
  })

  check('directory creation reports the exact EEXIST race without adopting the appeared generation', () => {
    const raced = join(root, 'reported-create-race')
    const failure = {}
    const result = withWorkerHook('directory-proof', (request) => {
      if (request.createDirectory === true && request.directoryParts.at(-1) === 'reported-create-race') {
        mkdirSync(raced)
        chmodSync(raced, 0o777)
      }
    }, null, () => fileGuards.realDirectoryUnder(root, raced, { create: true, mode: 0o700, failure }))
    assert.equal(result, null)
    assert.deepEqual(failure, { code: 'guard-component-raced' })
    assert.equal(lstatSync(raced).mode & 0o777, 0o777,
      'the create primitive must not adopt or mutate a generation which appeared after absence proof')
  })

  check('root and ancestor nanosecond proofs authorize by exact value, never Number coercion', () => {
    const rootTarget = join(directory, 'root-proof-fail.json')
    const rootResult = withWorkerHook('atomic-replace', (request) => {
      request.expected.ctimeNs = (BigInt(request.expected.ctimeNs) + 1n).toString()
    }, null, () => fileGuards.atomicReplaceRegularFile(root, directory, rootTarget, 'blocked\n', { maxBytes: 1024 }))
    assert.equal(rootResult, false)
    assert.equal(existsSync(rootTarget), false)

    const ancestorTarget = join(directory, 'ancestor-proof-fail.json')
    const ancestorResult = withWorkerHook('atomic-replace', (request) => {
      request.componentProofs[0].mtimeNs = (BigInt(request.componentProofs[0].mtimeNs) + 1n).toString()
    }, null, () => fileGuards.atomicReplaceRegularFile(root, directory, ancestorTarget, 'blocked\n', { maxBytes: 1024 }))
    assert.equal(ancestorResult, false)
    assert.equal(existsSync(ancestorTarget), false)
  })

  check('high exact identity values hydrate losslessly while numeric companions are explicitly derived', () => {
    const file = join(directory, 'high-stat.json')
    writeFileSync(file, 'high\n')
    const stat = withWorkerHook('stat-file', null, (response) => {
      response.stat.dev = '9007199254740993'
      response.stat.ino = '9007199254740995'
      response.stat.nlink = '9007199254740997'
      response.stat.mtimeNs = '9007199254740993000000'
      response.stat.ctimeNs = '9007199254740995000000'
    }, () => fileGuards.statRegularFileUnder(root, directory, file))
    assert.equal(stat.dev, '9007199254740993')
    assert.equal(stat.ino, '9007199254740995')
    assert.equal(stat.nlink, '9007199254740997')
    assert.equal(stat.mtimeNs, '9007199254740993000000')
    assert.equal(stat.ctimeNs, '9007199254740995000000')
    assert.equal(stat.nlinkNumber, Infinity)
    assert.equal(stat.mtimeMs, Infinity)
    assert.equal(stat.ctimeMs, Infinity)
  })

  check('malformed or extended worker stat envelopes can never become a false green', () => {
    const file = join(directory, 'malformed-stat.json')
    writeFileSync(file, 'malformed\n')
    const numeric = withWorkerHook('stat-file', null, (response) => {
      response.stat.dev = 9007199254740992
    }, () => fileGuards.statRegularFileUnder(root, directory, file))
    assert.equal(numeric, null)
    const extended = withWorkerHook('stat-file', null, (response) => {
      response.stat.untrusted = '1'
    }, () => fileGuards.statRegularFileUnder(root, directory, file))
    assert.equal(extended, null)
    const malformedDirectory = withWorkerHook('stat-file', null, (response) => {
      response.directoryStat.ctimeNs = Number(response.directoryStat.ctimeNs)
    }, () => fileGuards.statRegularFileUnder(root, directory, file))
    assert.equal(malformedDirectory, null)
  })

  check('site directory consumers fail closed on exact snapshot drift and rooted enumeration races', () => {
    const consumerRoot = join(base, 'consumer-project')
    const runs = join(consumerRoot, 'runtime', 'runs')
    const locks = join(consumerRoot, 'runtime', 'locks')
    const finalizations = join(consumerRoot, 'runtime', 'finalizations')
    for (const value of [runs, locks, finalizations]) mkdirSync(value, { recursive: true })
    const script = String.raw`
const assert = require('node:assert/strict');
const cp = require('node:child_process');
const path = require('node:path');
const root = process.argv[1], server = process.argv[2];
process.env.ORCHESTRATOR_PROJECT_ROOT = root;
process.env.ORCHESTRATOR_RUNS_DIR = path.join(root, 'runtime', 'runs');
process.env.ORCHESTRATOR_LOCKS_DIR = path.join(root, 'runtime', 'locks');
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = path.join(root, 'runtime', 'finalizations');
process.env.ORCHESTRATOR_WRITER_AUTHORITY_ROOT = root;
const fileGuards = require(path.join(server, 'file-guards.js'));
const original = cp.spawnSync;
let pair = 0;
let directoryMutation = null;
cp.spawnSync = function(command, args, options) {
  const request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null;
  let nextOptions = options;
  if (request && request.action === 'directory-names' && directoryMutation) {
    request.testMutateDirectoryName = directoryMutation;
    nextOptions = { ...options, input: JSON.stringify(request) };
  }
  const result = original.call(cp, command, args, nextOptions);
  if (!request || request.action !== 'directory-proof' || !result || typeof result.stdout !== 'string') return result;
  const response = JSON.parse(result.stdout);
  const second = pair++ % 2 === 1;
  response.stat.size = second ? '9007199254740993000001' : '9007199254740993000000';
  response.stat.mtimeNs = second ? '9007199254740995000001' : '9007199254740995000000';
  response.stat.ctimeNs = second ? '9007199254740997000001' : '9007199254740997000000';
  return { ...result, stdout: JSON.stringify(response) };
};
const sessions = require(path.join(server, 'sessions.js'));
pair = 0;
const listed = sessions.boundedRunsNames();
assert.equal(listed.ok, false);
assert.equal(listed.code, 'runs-directory-changed');
assert.ok(pair >= 2);
const locksModule = require(path.join(server, 'locks.js'));
directoryMutation = 'locks-enumeration-race';
assert.deepEqual(locksModule.readLocksResult(), {
  available: false,
  errorCode: 'runtime-locks-unavailable',
  rows: [],
});
const finalizationsModule = require(path.join(server, 'finalizations.js'));
directoryMutation = 'finalizations-enumeration-race';
const finalRows = finalizationsModule.list();
assert.equal(finalRows.length, 1);
assert.match(JSON.stringify(finalRows[0]), /cannot be enumerated safely/);
cp.spawnSync = original;
`
    const server = fileURLToPath(new URL('../server', import.meta.url))
    const result = originalSpawnSync(process.execPath, ['-e', script, consumerRoot, server], {
      encoding: 'utf8', timeout: 20_000,
    })
    assert.equal(result.status, 0, result.stderr + result.stdout)
  })

  check('basic guarded read, write, enumerate, fsync, move, and delete operations preserve exact contracts', () => {
    const state = join(directory, 'state.log')
    assert.equal(fileGuards.atomicReplaceRegularFile(root, directory, state, 'one\n', { maxBytes: 1024 }), true)
    assert.equal(fileGuards.appendBoundedRegularFileUnder(root, directory, state, 'two\n', {
      maxBytes: 1024, maxAppendBytes: 64,
    }).ok, true)
    assert.equal(fileGuards.boundedRegularFileUnder(root, directory, state, 1024).bytes.toString(), 'one\ntwo\n')
    assert.equal(fileGuards.tailRegularFileUnder(root, directory, state, 4).toString(), 'two\n')
    assert.equal(fileGuards.inspectEntryUnder(root, directory, state).status, 'present')
    const absentDirectory = join(root, 'absent-inspect-parent')
    assert.equal(fileGuards.inspectEntryUnder(
      root, absentDirectory, join(absentDirectory, 'missing.json'),
    ).status, 'missing')
    assert.equal(existsSync(absentDirectory), false, 'read-only inspection must not create a missing parent')
    assert.deepEqual(fileGuards.boundedDirectoryNamesUnder(root, absentDirectory, 100), {
      ok: true, exists: false, names: [],
    })
    assert.equal(existsSync(absentDirectory), false, 'read-only enumeration must not create a missing directory')
    assert.equal(fileGuards.fsyncRegularFileUnder(root, directory, state), true)
    assert.equal(fileGuards.fsyncDirectoryUnder(root, directory), true)
    assert.equal(fileGuards.boundedDirectoryNamesUnder(root, directory, 100).names.includes('state.log'), true)

    const published = join(directory, 'published.json')
    assert.equal(fileGuards.publishNoClobberRegularFileUnder(root, directory, published, 'published\n', {
      maxBytes: 1024,
    }).ok, true)
    assert.equal(fileGuards.publishNoClobberRegularFileUnder(root, directory, published, 'clobber\n', {
      maxBytes: 1024,
    }).code, 'exists')

    const source = join(directory, 'source.json')
    const moved = join(directory, 'moved.json')
    writeFileSync(source, 'move\n')
    assert.equal(fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, moved), true)
    assert.equal(existsSync(source), false)
    assert.equal(readFileSync(moved, 'utf8'), 'move\n')
    assert.equal(fileGuards.unlinkRegularFileUnder(root, directory, moved), true)

    const conditional = join(directory, 'conditional.json')
    writeFileSync(conditional, 'owned\n')
    assert.equal(fileGuards.unlinkRegularFileIfUnder(root, directory, conditional, 64, () => false), false)
    assert.equal(readFileSync(conditional, 'utf8'), 'owned\n')
    assert.equal(fileGuards.unlinkRegularFileIfUnder(root, directory, conditional, 64,
      (value) => value.bytes.toString() === 'owned\n'), true)
    assert.equal(existsSync(conditional), false)
  })

  check('symlink, hardlink, special-file, and oversize inputs fail closed', () => {
    const outside = join(base, 'outside-secret')
    const symlink = join(directory, 'link.json')
    writeFileSync(outside, 'outside\n')
    symlinkSync(outside, symlink)
    assert.equal(fileGuards.boundedRegularFileUnder(root, directory, symlink, 1024), null)
    assert.equal(fileGuards.statRegularFileUnder(root, directory, symlink), null)
    assert.equal(fileGuards.tailRegularFileUnder(root, directory, symlink, 16), null)
    assert.equal(fileGuards.fsyncRegularFileUnder(root, directory, symlink), false)
    assert.equal(fileGuards.unlinkRegularFileUnder(root, directory, symlink), false)
    assert.equal(readFileSync(outside, 'utf8'), 'outside\n')

    const hard = join(directory, 'hard.json')
    const alias = join(directory, 'hard-alias.json')
    writeFileSync(hard, 'hard\n')
    linkSync(hard, alias)
    assert.equal(fileGuards.boundedRegularFileUnder(root, directory, hard, 1024), null)
    assert.equal(fileGuards.statRegularFileUnder(root, directory, hard), null)
    assert.equal(fileGuards.tailRegularFileUnder(root, directory, hard, 16), null)
    assert.equal(fileGuards.appendBoundedRegularFileUnder(root, directory, hard, 'x', {
      maxBytes: 1024, maxAppendBytes: 16,
    }).ok, false)
    assert.equal(fileGuards.fsyncRegularFileUnder(root, directory, hard), false)
    assert.equal(fileGuards.unlinkRegularFileUnder(root, directory, hard), false)
    assert.equal(readFileSync(alias, 'utf8'), 'hard\n')

    const oversized = join(directory, 'oversized.json')
    writeFileSync(oversized, '0123456789')
    assert.equal(fileGuards.boundedRegularFileUnder(root, directory, oversized, 4), null)

    if (process.platform !== 'win32') {
      const fifo = join(directory, 'pipe')
      const made = originalSpawnSync('mkfifo', [fifo], { encoding: 'utf8' })
      if (made.status === 0) {
        assert.equal(fileGuards.boundedRegularFileUnder(root, directory, fifo, 16), null)
        assert.equal(fileGuards.statRegularFileUnder(root, directory, fifo), null)
        assert.equal(fileGuards.unlinkRegularFileUnder(root, directory, fifo), false)
      }
    }
  })

  check('every descriptor-based operation rejects a final-name replacement and preserves the foreign generation', () => {
    const cases = [
      ['bounded-read', 'read-race.json', (file) => fileGuards.boundedRegularFileUnder(root, directory, file, 1024), null],
      ['stat-file', 'stat-race.json', (file) => fileGuards.statRegularFileUnder(root, directory, file), null],
      ['tail', 'tail-race.json', (file) => fileGuards.tailRegularFileUnder(root, directory, file, 16), null],
      ['inspect-entry', 'inspect-race.json', (file) => fileGuards.inspectEntryUnder(root, directory, file), 'unsafe'],
      ['fsync-file', 'fsync-race.json', (file) => fileGuards.fsyncRegularFileUnder(root, directory, file), false],
      ['append-bounded', 'append-race.json', (file) => fileGuards.appendBoundedRegularFileUnder(
        root, directory, file, 'append\n', { maxBytes: 1024, maxAppendBytes: 64 }), false],
    ]
    for (const [action, name, operation, expected] of cases) {
      const file = join(directory, name)
      writeFileSync(file, 'owned-generation\n')
      const result = injectRequestField(action, 'testReplaceAfterOpenBytes', foreignBytes(), () => operation(file))
      if (action === 'inspect-entry') assert.equal(result.status, expected)
      else if (action === 'append-bounded') assert.equal(result.ok, expected)
      else assert.equal(result, expected)
      assert.equal(readFileSync(file, 'utf8'), 'foreign-generation\n', action)
    }
  })

  check('atomic and no-clobber publication never report success over a raced final replacement', () => {
    const atomic = join(directory, 'atomic-race.json')
    const atomicResult = injectRequestField('atomic-replace', 'testReplaceAfterPublishBytes', foreignBytes(), () =>
      fileGuards.atomicReplaceRegularFile(root, directory, atomic, 'owned\n', { maxBytes: 1024 }))
    assert.equal(atomicResult, false)
    assert.equal(readFileSync(atomic, 'utf8'), 'foreign-generation\n')

    const publish = join(directory, 'publish-race.json')
    const publishResult = injectRequestField('publish-no-clobber', 'testReplaceAfterPublishBytes', foreignBytes(), () =>
      fileGuards.publishNoClobberRegularFileUnder(root, directory, publish, 'owned\n', { maxBytes: 1024 }))
    assert.equal(publishResult.ok, false)
    assert.equal(readFileSync(publish, 'utf8'), 'foreign-generation\n')
  })

  check('enumeration rejects a directory mutation observed between its exact pre/post proofs', () => {
    const result = injectRequestField('directory-names', 'testMutateDirectoryName', 'enumeration-race', () =>
      fileGuards.boundedDirectoryNamesUnder(root, directory, 1000))
    assert.equal(result.ok, false)
    assert.deepEqual(result.names, [])
    assert.equal(existsSync(join(directory, 'enumeration-race')), true)
  })

  check('quarantine delete and conditional read never unlink a replacement generation', () => {
    const direct = join(directory, 'unlink-capture-race.json')
    writeFileSync(direct, 'owned\n')
    const removed = injectRequestField('quarantine-unlink', 'testReplaceBeforeCaptureBytes', foreignBytes(), () =>
      fileGuards.unlinkRegularFileUnder(root, directory, direct))
    assert.equal(removed, false)
    assert.equal(readFileSync(direct, 'utf8'), 'foreign-generation\n')
    fileGuards.unlinkRegularFileUnder(root, directory, direct)
    assert.equal(readFileSync(direct, 'utf8'), 'foreign-generation\n')

    const conditional = join(directory, 'conditional-capture-race.json')
    writeFileSync(conditional, 'owned\n')
    const conditionalRemoved = injectRequestField('quarantine-detach-read', 'testReplaceBeforeCaptureBytes', foreignBytes(), () =>
      fileGuards.unlinkRegularFileIfUnder(root, directory, conditional, 1024, () => true))
    assert.equal(conditionalRemoved, false)
    assert.equal(readFileSync(conditional, 'utf8'), 'foreign-generation\n')
    fileGuards.unlinkRegularFileIfUnder(root, directory, conditional, 1024, () => true)
    assert.equal(readFileSync(conditional, 'utf8'), 'foreign-generation\n')
  })

  check('matching unlink validates exact bounded bytes or SHA-256 inside the worker and is idempotent', () => {
    const bytesFile = join(directory, 'matching-bytes.json')
    writeFileSync(bytesFile, 'match-me\n')
    assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
      root, directory, bytesFile, 1024, 'not-a-match\n'), false)
    assert.equal(readFileSync(bytesFile, 'utf8'), 'match-me\n')
    const proof = fileGuards.statRegularFileUnder(root, directory, bytesFile)
    assert.ok(proof)
    assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
      root, directory, bytesFile, 1024, { bytes: Buffer.from('match-me\n'), proof }), true)
    assert.equal(existsSync(bytesFile), false)
    assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
      root, directory, bytesFile, 1024, 'match-me\n'), true, 'already missing is an idempotent success')

    const hashFile = join(directory, 'matching-hash.json')
    const hashBytes = Buffer.from('hash-match\n')
    writeFileSync(hashFile, hashBytes)
    const hash = `sha256:${createHash('sha256').update(hashBytes).digest('hex')}`
    assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
      root, directory, hashFile, 1024, { sha256: hash }), true)
    assert.equal(existsSync(hashFile), false)

    const staleFile = join(directory, 'matching-stale-proof.json')
    writeFileSync(staleFile, 'old\n')
    const staleProof = fileGuards.statRegularFileUnder(root, directory, staleFile)
    unlinkSync(staleFile)
    writeFileSync(staleFile, 'new\n')
    assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
      root, directory, staleFile, 1024, { bytes: 'new\n', proof: staleProof }), false)
    assert.equal(readFileSync(staleFile, 'utf8'), 'new\n')
    assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
  })

  check('matching unlink records delete intent before detach and recovers every durable crash boundary', () => {
    const points = [
      'guard:after-manifest-stage', 'guard:after-manifest', 'guard:after-detach',
      'guard:after-remove', 'guard:after-receipt-stage', 'guard:after-receipt',
      'guard:after-receipt-unlink', 'guard:after-manifest-unlink',
    ]
    for (let index = 0; index < points.length; index++) {
      const file = join(directory, `matching-crash-${index}.json`)
      writeFileSync(file, `matching-owned-${index}\n`)
      const first = injectRequestField('quarantine-unlink-matching', 'testCrashAt', points[index], () =>
        fileGuards.unlinkRegularFileMatchingUnder(
          root, directory, file, 1024, `matching-owned-${index}\n`))
      assert.equal(first, false, points[index])
      assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
        root, directory, file, 1024, `matching-owned-${index}\n`), true, points[index])
      assert.equal(existsSync(file), false, points[index])
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [], points[index])
    }
  })

  check('matching unlink cannot wedge a new public owner after a crash-detach and never deletes a mismatch', () => {
    const file = join(directory, 'matching-new-owner.json')
    writeFileSync(file, 'old-owner\n')
    assert.equal(injectRequestField('quarantine-unlink-matching', 'testCrashAt', 'guard:after-detach', () =>
      fileGuards.unlinkRegularFileMatchingUnder(root, directory, file, 1024, 'old-owner\n')), false)
    assert.equal(existsSync(file), false)
    writeFileSync(file, 'new-owner\n', { flag: 'wx' })
    assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
      root, directory, file, 1024, 'new-owner\n'), true)
    assert.equal(existsSync(file), false)
    assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])

    const mismatch = join(directory, 'matching-new-owner-mismatch.json')
    writeFileSync(mismatch, 'old-owner\n')
    assert.equal(injectRequestField('quarantine-unlink-matching', 'testCrashAt', 'guard:after-detach', () =>
      fileGuards.unlinkRegularFileMatchingUnder(root, directory, mismatch, 1024, 'old-owner\n')), false)
    writeFileSync(mismatch, 'foreign-owner\n', { flag: 'wx' })
    assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
      root, directory, mismatch, 1024, 'different-owner\n'), false)
    assert.equal(readFileSync(mismatch, 'utf8'), 'foreign-owner\n')
    assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
    assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
      root, directory, mismatch, 1024, 'foreign-owner\n'), true)

    const raced = join(directory, 'matching-public-race.json')
    writeFileSync(raced, 'authorized-owner\n')
    assert.equal(injectRequestField('quarantine-unlink-matching', 'testReplaceBeforeCaptureBytes', foreignBytes(), () =>
      fileGuards.unlinkRegularFileMatchingUnder(root, directory, raced, 1024, 'authorized-owner\n')), false)
    assert.equal(readFileSync(raced, 'utf8'), 'foreign-generation\n')
    assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
      root, directory, raced, 1024, 'authorized-owner\n'), false)
    assert.equal(readFileSync(raced, 'utf8'), 'foreign-generation\n')
    assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
  })

  check('WAL records require private mode and exact lone-versus-publication hardlink topology', () => {
    {
      const file = join(directory, 'wal-final-hardlink.json')
      const alias = join(directory, 'wal-final-hardlink-alias')
      writeFileSync(file, 'hardlink-topology\n')
      assert.equal(injectRequestField('quarantine-unlink-matching', 'testCrashAt', 'guard:after-manifest', () =>
        fileGuards.unlinkRegularFileMatchingUnder(root, directory, file, 1024, 'hardlink-topology\n')), false)
      const manifest = readdirSync(directory).find((name) => /^\.guard-txn-[a-f0-9]{64}\.json$/.test(name))
      assert.ok(manifest)
      linkSync(join(directory, manifest), alias)
      assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
        root, directory, file, 1024, 'hardlink-topology\n'), false)
      assert.equal(readFileSync(file, 'utf8'), 'hardlink-topology\n')
      unlinkSync(alias)
      assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
        root, directory, file, 1024, 'hardlink-topology\n'), true)
    }

    {
      const file = join(directory, 'wal-stage-hardlink.json')
      const alias = join(directory, 'wal-stage-hardlink-alias')
      writeFileSync(file, 'stage-topology\n')
      assert.equal(injectRequestField('quarantine-unlink-matching', 'testCrashAt',
        'guard:after-manifest-stage', () => fileGuards.unlinkRegularFileMatchingUnder(
          root, directory, file, 1024, 'stage-topology\n')), false)
      const stage = readdirSync(directory).find((name) => /^\.guard-txn-[a-f0-9]{64}\.json\.stage$/.test(name))
      assert.ok(stage)
      linkSync(join(directory, stage), alias)
      assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
        root, directory, file, 1024, 'stage-topology\n'), false)
      assert.equal(readFileSync(file, 'utf8'), 'stage-topology\n')
      unlinkSync(alias)
      assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
        root, directory, file, 1024, 'stage-topology\n'), true)
    }

    if (process.platform !== 'win32') {
      const file = join(directory, 'wal-public-mode.json')
      writeFileSync(file, 'private-only\n')
      assert.equal(injectRequestField('quarantine-unlink-matching', 'testCrashAt', 'guard:after-manifest', () =>
        fileGuards.unlinkRegularFileMatchingUnder(root, directory, file, 1024, 'private-only\n')), false)
      const manifest = readdirSync(directory).find((name) => /^\.guard-txn-[a-f0-9]{64}\.json$/.test(name))
      assert.ok(manifest)
      chmodSync(join(directory, manifest), 0o644)
      assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
        root, directory, file, 1024, 'private-only\n'), false)
      assert.equal(readFileSync(file, 'utf8'), 'private-only\n')
      chmodSync(join(directory, manifest), 0o600)
      assert.equal(fileGuards.unlinkRegularFileMatchingUnder(
        root, directory, file, 1024, 'private-only\n'), true)
    }
    assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
  })

  check('no-clobber transfer preserves a replaced source and never deletes its durable owned target', () => {
    const source = join(directory, 'transfer-race-source.json')
    const target = join(directory, 'transfer-race-target.json')
    writeFileSync(source, 'owned\n')
    const moved = injectRequestField('transfer-no-clobber', 'testReplaceBeforeCaptureBytes', foreignBytes(), () =>
      fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target))
    assert.equal(moved, false)
    assert.equal(readFileSync(source, 'utf8'), 'foreign-generation\n')
    assert.equal(readFileSync(target, 'utf8'), 'owned\n')
  })

  check('no-clobber transfer WAL forward-recovers every durable link, detach, cleanup, and receipt boundary', () => {
    const points = [
      'guard:after-transfer-manifest-stage', 'guard:after-transfer-manifest',
      'guard:before-transfer-target-link', 'guard:after-transfer-target-link',
      'guard:after-transfer-link-stage', 'guard:after-transfer-link-record',
      'guard:before-transfer-source-detach', 'guard:after-transfer-source-detach',
      'guard:before-transfer-capture-remove', 'guard:after-transfer-capture-remove',
      'guard:after-transfer-receipt-stage', 'guard:after-transfer-receipt',
      'guard:after-transfer-link-unlink', 'guard:after-transfer-receipt-unlink',
      'guard:after-transfer-manifest-unlink',
    ]
    for (let index = 0; index < points.length; index++) {
      const source = join(directory, `transfer-crash-source-${index}.json`)
      const target = join(directory, `transfer-crash-target-${index}.json`)
      writeFileSync(source, `owned-${index}\n`)
      const first = injectRequestField('transfer-no-clobber', 'testCrashAt', points[index], () =>
        fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target))
      assert.equal(first, false, points[index])
      fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target)
      assert.equal(existsSync(source), false, points[index])
      assert.equal(readFileSync(target, 'utf8'), `owned-${index}\n`, points[index])
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [], points[index])
    }
  })

  check('transfer recovery preserves foreign source, target, and capture generations without a false green', () => {
    {
      const source = join(directory, 'transfer-foreign-source.json')
      const target = join(directory, 'transfer-foreign-source-target.json')
      writeFileSync(source, 'owned-source\n')
      assert.equal(injectRequestField('transfer-no-clobber', 'testCrashAt',
        'guard:after-transfer-target-link', () =>
          fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target)), false)
      unlinkSync(source)
      writeFileSync(source, 'foreign-source\n')
      assert.equal(fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target), false)
      assert.equal(readFileSync(source, 'utf8'), 'foreign-source\n')
      assert.equal(readFileSync(target, 'utf8'), 'owned-source\n')
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
    }

    {
      const source = join(directory, 'transfer-foreign-target.json')
      const target = join(directory, 'transfer-foreign-target-destination.json')
      writeFileSync(source, 'owned-before-target-race\n')
      assert.equal(injectRequestField('transfer-no-clobber', 'testCrashAt',
        'guard:after-transfer-target-link', () =>
          fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target)), false)
      unlinkSync(target)
      writeFileSync(target, 'foreign-target\n')
      assert.equal(fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target), false)
      assert.equal(readFileSync(source, 'utf8'), 'owned-before-target-race\n')
      assert.equal(readFileSync(target, 'utf8'), 'foreign-target\n')
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
    }

    {
      const source = join(directory, 'transfer-detached-target-race.json')
      const target = join(directory, 'transfer-detached-target-race-destination.json')
      writeFileSync(source, 'owned-detached\n')
      assert.equal(injectRequestField('transfer-no-clobber', 'testCrashAt',
        'guard:after-transfer-source-detach', () =>
          fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target)), false)
      unlinkSync(target)
      writeFileSync(target, 'foreign-after-detach\n')
      assert.equal(fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target), false)
      assert.equal(readFileSync(source, 'utf8'), 'owned-detached\n')
      assert.equal(readFileSync(target, 'utf8'), 'foreign-after-detach\n')
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
    }

    {
      const source = join(directory, 'transfer-detached-source-race.json')
      const target = join(directory, 'transfer-detached-source-race-destination.json')
      writeFileSync(source, 'owned-before-source-race\n')
      assert.equal(injectRequestField('transfer-no-clobber', 'testCrashAt',
        'guard:after-transfer-source-detach', () =>
          fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target)), false)
      writeFileSync(source, 'foreign-after-detach\n')
      assert.equal(fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target), false)
      assert.equal(readFileSync(source, 'utf8'), 'foreign-after-detach\n')
      assert.equal(readFileSync(target, 'utf8'), 'owned-before-source-race\n')
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
    }

    {
      const source = join(directory, 'transfer-foreign-capture.json')
      const target = join(directory, 'transfer-foreign-capture-destination.json')
      writeFileSync(source, 'owned-before-capture-race\n')
      assert.equal(injectRequestField('transfer-no-clobber', 'testCrashAt',
        'guard:after-transfer-source-detach', () =>
          fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target)), false)
      const capture = readdirSync(directory).find((name) => name.startsWith('.guard-transfer-capture-'))
      assert.ok(capture)
      unlinkSync(join(directory, capture))
      writeFileSync(join(directory, capture), 'foreign-capture\n')
      assert.equal(fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target), false)
      assert.equal(readFileSync(source, 'utf8'), 'foreign-capture\n')
      assert.equal(readFileSync(target, 'utf8'), 'owned-before-capture-race\n')
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
    }

    for (const [index, point] of [
      'guard:after-transfer-rollback-link',
      'guard:after-transfer-rollback-capture-remove',
    ].entries()) {
      const source = join(directory, `transfer-rollback-crash-source-${index}.json`)
      const target = join(directory, `transfer-rollback-crash-target-${index}.json`)
      writeFileSync(source, `rollback-owned-${index}\n`)
      assert.equal(injectRequestField('transfer-no-clobber', 'testCrashAt',
        'guard:after-transfer-source-detach', () =>
          fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target)), false)
      unlinkSync(target)
      writeFileSync(target, `rollback-foreign-${index}\n`)
      assert.equal(injectRequestField('transfer-no-clobber', 'testCrashAt', point, () =>
        fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, target)), false)
      assert.equal(fileGuards.transferFileNoClobberSameDirectoryUnder(
        root, directory, source, target), false)
      assert.equal(readFileSync(source, 'utf8'), `rollback-owned-${index}\n`)
      assert.equal(readFileSync(target, 'utf8'), `rollback-foreign-${index}\n`)
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
    }
  })

  check('a source-keyed transfer WAL rejects a competing target until the original intent reconciles', () => {
    const source = join(directory, 'transfer-target-binding-source.json')
    const originalTarget = join(directory, 'transfer-target-binding-original.json')
    const competingTarget = join(directory, 'transfer-target-binding-competing.json')
    writeFileSync(source, 'target-bound\n')
    assert.equal(injectRequestField('transfer-no-clobber', 'testCrashAt',
      'guard:after-transfer-manifest', () =>
        fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, originalTarget)), false)
    assert.equal(fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, competingTarget), false)
    assert.equal(existsSync(competingTarget), false)
    assert.equal(fileGuards.transferFileNoClobberSameDirectoryUnder(root, directory, source, originalTarget), true)
    assert.equal(readFileSync(originalTarget, 'utf8'), 'target-bound\n')
    assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
  })

  check('bounded guard reconciliation recovers mixed valid transactions and preserves corrupt or foreign evidence', () => {
    const removedDirectory = join(directory, 'reconcile-rmdir-target')
    mkdirSync(removedDirectory)
    assert.equal(injectRequestField('remove-empty-directory', 'testCrashAt', 'guard:after-remove', () =>
      fileGuards.removeEmptyDirectoryUnder(root, directory, removedDirectory)), false)
    assert.equal(existsSync(removedDirectory), false)

    const deletedFile = join(directory, 'reconcile-delete-target.json')
    writeFileSync(deletedFile, 'reconcile-delete\n')
    assert.equal(injectRequestField('quarantine-unlink-matching', 'testCrashAt', 'guard:after-detach', () =>
      fileGuards.unlinkRegularFileMatchingUnder(
        root, directory, deletedFile, 1024, 'reconcile-delete\n')), false)

    const transferSource = join(directory, 'reconcile-transfer-source.json')
    const transferTarget = join(directory, 'reconcile-transfer-target.json')
    writeFileSync(transferSource, 'reconcile-transfer\n')
    assert.equal(injectRequestField('transfer-no-clobber', 'testCrashAt',
      'guard:after-transfer-source-detach', () =>
        fileGuards.transferFileNoClobberSameDirectoryUnder(
          root, directory, transferSource, transferTarget)), false)

    const foreignPublic = join(directory, 'reconcile-foreign-public.json')
    writeFileSync(foreignPublic, 'reconcile-old-owner\n')
    assert.equal(injectRequestField('quarantine-unlink-matching', 'testCrashAt', 'guard:after-detach', () =>
      fileGuards.unlinkRegularFileMatchingUnder(
        root, directory, foreignPublic, 1024, 'reconcile-old-owner\n')), false)
    writeFileSync(foreignPublic, 'reconcile-foreign-owner\n', { flag: 'wx' })

    const corrupt = `.guard-txn-${'f'.repeat(64)}.json`
    const orphanCapture = `.guard-capture-${'e'.repeat(32)}`
    const unknown = '.guard-foreign-evidence'
    writeFileSync(join(directory, corrupt), '{not-json}\n', { mode: 0o600 })
    writeFileSync(join(directory, orphanCapture), 'orphan-capture\n', { mode: 0o600 })
    writeFileSync(join(directory, unknown), 'foreign-evidence\n', { mode: 0o600 })

    const summary = fileGuards.reconcileGuardTransactionsUnder(root, directory, {
      maxEntries: 10_000, maxTransactions: 100,
    })
    assert.equal(summary.ok, false)
    assert.equal(summary.code, 'guard-reconcile-incomplete')
    assert.equal(summary.reconciled, 4)
    assert.equal(summary.pending, 3)
    assert.equal(summary.transactions, 7)
    assert.equal(existsSync(deletedFile), false)
    assert.equal(existsSync(transferSource), false)
    assert.equal(readFileSync(transferTarget, 'utf8'), 'reconcile-transfer\n')
    assert.equal(readFileSync(foreignPublic, 'utf8'), 'reconcile-foreign-owner\n')
    assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')).sort(),
      [corrupt, orphanCapture, unknown].sort())
    assert.equal(JSON.stringify(summary).includes('reconcile-delete-target'), false,
      'summary exposes counts/codes, never private names or record contents')

    for (const name of [corrupt, orphanCapture, unknown]) unlinkSync(join(directory, name))
    const clean = fileGuards.reconcileGuardTransactionsUnder(root, directory, {
      maxEntries: 10_000, maxTransactions: 100,
    })
    assert.deepEqual(clean.codes, {})
    assert.equal(clean.ok, true)
    assert.equal(clean.transactions, 0)
  })

  check('guard reconciliation enforces entry and transaction caps before performing any mutation', () => {
    const first = join(directory, 'reconcile-cap-first.json')
    const second = join(directory, 'reconcile-cap-second.json')
    writeFileSync(first, 'cap-first\n')
    writeFileSync(second, 'cap-second\n')
    for (const [file, bytes] of [[first, 'cap-first\n'], [second, 'cap-second\n']]) {
      assert.equal(injectRequestField('quarantine-unlink-matching', 'testCrashAt', 'guard:after-manifest', () =>
        fileGuards.unlinkRegularFileMatchingUnder(root, directory, file, 1024, bytes)), false)
    }
    const entryCapped = fileGuards.reconcileGuardTransactionsUnder(root, directory, {
      maxEntries: 0, maxTransactions: 100,
    })
    assert.equal(entryCapped.ok, false)
    assert.equal(entryCapped.code, 'scan-entry-cap-exceeded')
    assert.equal(entryCapped.reconciled, 0)
    assert.equal(readFileSync(first, 'utf8'), 'cap-first\n')
    assert.equal(readFileSync(second, 'utf8'), 'cap-second\n')

    const transactionCapped = fileGuards.reconcileGuardTransactionsUnder(root, directory, {
      maxEntries: 10_000, maxTransactions: 1,
    })
    assert.equal(transactionCapped.ok, false)
    assert.equal(transactionCapped.code, 'scan-transaction-cap-exceeded')
    assert.equal(transactionCapped.reconciled, 0)
    assert.equal(readFileSync(first, 'utf8'), 'cap-first\n')
    assert.equal(readFileSync(second, 'utf8'), 'cap-second\n')

    const recovered = fileGuards.reconcileGuardTransactionsUnder(root, directory, {
      maxEntries: 10_000, maxTransactions: 2,
    })
    assert.equal(recovered.ok, true)
    assert.equal(recovered.reconciled, 2)
    assert.equal(existsSync(first), false)
    assert.equal(existsSync(second), false)
    assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])

    const missing = fileGuards.reconcileGuardTransactionsUnder(root, join(root, 'missing-reconcile-dir'), {
      maxEntries: 10, maxTransactions: 10,
    })
    assert.deepEqual(missing, { ok: true, exists: false, scanned: 0,
      transactions: 0, reconciled: 0, pending: 0, codes: {} })
  })

  check('ancestor swap-and-swap-back and missing-component appearance both fail before mutation', () => {
    const alpha = join(root, 'alpha')
    const displaced = join(root, 'alpha-swap-displaced')
    const target = join(directory, 'swap-back.json')
    const swapped = withWorkerHook('atomic-replace', () => {
      renameSync(alpha, displaced)
      mkdirSync(alpha)
      rmSync(alpha, { recursive: true, force: true })
      renameSync(displaced, alpha)
      const forced = new Date(Date.now() - 60_000)
      utimesSync(root, forced, forced)
    }, null, () => fileGuards.atomicReplaceRegularFile(root, directory, target, 'blocked\n', { maxBytes: 1024 }))
    assert.equal(swapped, false)
    assert.equal(existsSync(target), false)

    const appearedDirectory = join(root, 'appeared', 'nested')
    const appearedTarget = join(appearedDirectory, 'must-not-write.json')
    const appeared = withWorkerHook('atomic-replace', () => {
      mkdirSync(appearedDirectory, { recursive: true })
    }, null, () => fileGuards.atomicReplaceRegularFile(
      root, appearedDirectory, appearedTarget, 'blocked\n', { create: true, maxBytes: 1024 }))
    assert.equal(appeared, false)
    assert.equal(existsSync(appearedTarget), false)
  })

  check('removeEmptyDirectoryUnder removes only the exact empty generation and rejects stale/nonempty proofs', () => {
    const empty = join(directory, 'empty-owned')
    mkdirSync(empty)
    const snapshot = fileGuards.realDirectoryUnder(root, empty)
    assert.ok(snapshot?.stat)
    assert.equal(fileGuards.removeEmptyDirectoryUnder(root, directory, empty, snapshot.stat), true)
    assert.equal(existsSync(empty), false)

    const nonempty = join(directory, 'not-empty')
    mkdirSync(nonempty)
    writeFileSync(join(nonempty, 'keep'), 'keep\n')
    assert.equal(fileGuards.removeEmptyDirectoryUnder(root, directory, nonempty), false)
    assert.equal(readFileSync(join(nonempty, 'keep'), 'utf8'), 'keep\n')

    const replaced = join(directory, 'empty-replaced')
    const old = join(directory, 'empty-replaced-old')
    mkdirSync(replaced)
    const stale = fileGuards.realDirectoryUnder(root, replaced).stat
    renameSync(replaced, old)
    mkdirSync(replaced)
    assert.equal(fileGuards.removeEmptyDirectoryUnder(root, directory, replaced, stale), false)
    assert.equal(existsSync(replaced), true)
    assert.equal(existsSync(old), true)
  })

  check('empty-directory removal preserves a replacement and rejects ancestor swap-back', () => {
    const replaced = join(directory, 'rmdir-capture-race')
    mkdirSync(replaced)
    const result = injectRequestField('remove-empty-directory', 'testReplaceBeforeCapture', true, () =>
      fileGuards.removeEmptyDirectoryUnder(root, directory, replaced))
    assert.equal(result, false)
    assert.equal(lstatSync(replaced).isDirectory(), true)
    fileGuards.removeEmptyDirectoryUnder(root, directory, replaced)
    assert.equal(lstatSync(replaced).isDirectory(), true)

    const alpha = join(root, 'alpha')
    const displaced = join(root, 'alpha-rmdir-displaced')
    const guarded = join(directory, 'rmdir-swap-back')
    mkdirSync(guarded)
    const swapResult = withWorkerHook('remove-empty-directory', () => {
      renameSync(alpha, displaced)
      mkdirSync(alpha)
      rmSync(alpha, { recursive: true, force: true })
      renameSync(displaced, alpha)
      const forced = new Date(Date.now() - 120_000)
      utimesSync(root, forced, forced)
    }, null, () => fileGuards.removeEmptyDirectoryUnder(root, directory, guarded))
    assert.equal(swapResult, false)
    assert.equal(existsSync(guarded), true)
  })

  check('direct unlink WAL recovers every durable crash boundary without a hidden capture', () => {
    const points = [
      'guard:after-manifest-stage', 'guard:after-manifest', 'guard:after-detach',
      'guard:after-remove', 'guard:after-receipt-stage', 'guard:after-receipt',
      'guard:after-receipt-unlink', 'guard:after-manifest-unlink',
    ]
    for (let index = 0; index < points.length; index++) {
      const file = join(directory, `unlink-crash-${index}.json`)
      writeFileSync(file, 'owned\n')
      const first = injectRequestField('quarantine-unlink', 'testCrashAt', points[index], () =>
        fileGuards.unlinkRegularFileUnder(root, directory, file, { allowMissing: true }))
      assert.equal(first, false, points[index])
      fileGuards.unlinkRegularFileUnder(root, directory, file, { allowMissing: true })
      assert.equal(existsSync(file), false, points[index])
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [], points[index])
    }
  })

  check('conditional unlink WAL recovers detach, durable decision, outcome, and cleanup crashes', () => {
    const detachPoints = ['guard:after-manifest-stage', 'guard:after-manifest', 'guard:after-detach']
    for (let index = 0; index < detachPoints.length; index++) {
      const file = join(directory, `conditional-detach-crash-${index}.json`)
      writeFileSync(file, 'owned\n')
      const first = injectRequestField('quarantine-detach-read', 'testCrashAt', detachPoints[index], () =>
        fileGuards.unlinkRegularFileIfUnder(root, directory, file, 1024, () => true))
      assert.equal(first, false, detachPoints[index])
      assert.equal(fileGuards.unlinkRegularFileIfUnder(root, directory, file, 1024,
        (value) => value.bytes.toString() === 'owned\n'), true, detachPoints[index])
      assert.equal(existsSync(file), false)
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
    }

    const finalizePoints = [
      'guard:after-decision-stage', 'guard:after-decision', 'guard:after-remove',
      'guard:after-receipt-stage', 'guard:after-receipt', 'guard:after-receipt-unlink',
      'guard:after-manifest-unlink',
    ]
    for (let index = 0; index < finalizePoints.length; index++) {
      const file = join(directory, `conditional-finalize-crash-${index}.json`)
      writeFileSync(file, 'owned\n')
      const first = injectRequestField('quarantine-finalize', 'testCrashAt', finalizePoints[index], () =>
        fileGuards.unlinkRegularFileIfUnder(root, directory, file, 1024, () => true))
      assert.equal(first, false, finalizePoints[index])
      fileGuards.unlinkRegularFileUnder(root, directory, file, { allowMissing: true })
      assert.equal(existsSync(file), false, finalizePoints[index])
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [], finalizePoints[index])
    }

    const restorePoints = [
      'guard:after-decision-stage', 'guard:after-decision', 'guard:after-restore-link',
      'guard:after-restore-unlink', 'guard:after-restore', 'guard:after-receipt-stage',
      'guard:after-receipt', 'guard:after-receipt-unlink', 'guard:after-manifest-unlink',
    ]
    for (let index = 0; index < restorePoints.length; index++) {
      const file = join(directory, `conditional-restore-crash-${index}.json`)
      writeFileSync(file, 'restore-owned\n')
      const first = injectRequestField('quarantine-finalize', 'testCrashAt', restorePoints[index], () =>
        fileGuards.unlinkRegularFileIfUnder(root, directory, file, 1024, () => false))
      assert.equal(first, false, restorePoints[index])
      assert.equal(fileGuards.unlinkRegularFileIfUnder(root, directory, file, 1024, () => false), false)
      assert.equal(readFileSync(file, 'utf8'), 'restore-owned\n', restorePoints[index])
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [], restorePoints[index])
      assert.equal(fileGuards.unlinkRegularFileUnder(root, directory, file), true)
    }
  })

  check('a parent crash between conditional detach and finalize restores exact bytes on ordinary recovery', () => {
    const file = join(directory, 'conditional-parent-crash.json')
    writeFileSync(file, 'owned-before-parent-crash\n')
    const script = `
const guards = require(process.argv[1]);
const [root, directory, file] = process.argv.slice(2);
guards.unlinkRegularFileIfUnder(root, directory, file, 4096, () => {
  process.kill(process.pid, 'SIGKILL');
  return true;
});
`
    const child = originalSpawnSync(process.execPath, ['-e', script, FILE_GUARDS, root, directory, file], {
      encoding: 'utf8', env: { ...process.env, ORCHESTRATOR_FILE_GUARD_TEST_MODE: '1' }, timeout: 10_000,
    })
    assert.equal(child.signal, 'SIGKILL', child.stderr + child.stdout)
    assert.equal(existsSync(file), false, 'the crashed parent left the exact generation privately detached')
    assert.equal(fileGuards.unlinkRegularFileUnder(root, directory, file, { allowMissing: true }), false)
    assert.equal(readFileSync(file, 'utf8'), 'owned-before-parent-crash\n')
    assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [])
    assert.equal(fileGuards.unlinkRegularFileIfUnder(root, directory, file, 4096, () => true), true)
  })

  check('empty-directory WAL forward-recovers every detach/remove/receipt crash boundary', () => {
    const points = [
      'guard:after-manifest-stage', 'guard:after-manifest', 'guard:after-detach',
      'guard:after-remove', 'guard:after-receipt-stage', 'guard:after-receipt',
      'guard:after-receipt-unlink', 'guard:after-manifest-unlink',
    ]
    for (let index = 0; index < points.length; index++) {
      const target = join(directory, `rmdir-crash-${index}`)
      mkdirSync(target)
      const first = injectRequestField('remove-empty-directory', 'testCrashAt', points[index], () =>
        fileGuards.removeEmptyDirectoryUnder(root, directory, target))
      assert.equal(first, false, points[index])
      fileGuards.removeEmptyDirectoryUnder(root, directory, target)
      assert.equal(existsSync(target), false, points[index])
      assert.deepEqual(readdirSync(directory).filter((name) => name.startsWith('.guard-')), [], points[index])
    }
  })

  check('empty-directory WAL never deletes a same-inode generation whose exact pre-detach proof changed', () => {
    const target = join(directory, 'rmdir-same-inode-race')
    mkdirSync(target)
    const before = lstatSync(target, { bigint: true })
    const removed = injectRequestField('remove-empty-directory', 'testRenameTargetAwayAndBack', true, () =>
      fileGuards.removeEmptyDirectoryUnder(root, directory, target))
    assert.equal(removed, false)
    const after = lstatSync(target, { bigint: true })
    assert.equal(after.dev, before.dev)
    assert.equal(after.ino, before.ino)
    assert.notEqual(after.ctimeNs, before.ctimeNs)
    assert.equal(readdirSync(directory).filter((name) => name.startsWith('.guard-txn-')).length, 1,
      'stale exact proof remains durable evidence instead of authorizing deletion')
  })

  console.log(`file-guards bigint boundary: ${checks} checks passed`)
} finally {
  childProcess.spawnSync = originalSpawnSync
  delete process.env.ORCHESTRATOR_FILE_GUARD_TEST_MODE
  rmSync(base, { recursive: true, force: true })
}
