#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'

const here = dirname(fileURLToPath(import.meta.url))
const tasksDir = join(here, '..', '..', 'tasks')
const cli = join(tasksDir, 'writer-lease.mjs')
const require = createRequire(import.meta.url)
const creationContract = require('../../tasks/creation-marker-contract.cjs')
const taskSource = require('../../tasks/task-source-contract.cjs')
const editContract = require('../../tasks/edit-marker-contract.cjs')
const writerLeases = require('../../tasks/writer-leases.cjs')
const root = mkdtempSync(join(tmpdir(), 'publication-guard-'))
const finalizations = join(root, 'finalizations')
const creations = join(root, 'creations')
const edits = join(root, 'edits')
for (const dir of [finalizations, creations, edits]) mkdirSync(dir, { recursive: true })
const env = { ...process.env, FINALIZE_STATE_DIR: finalizations, FINALIZE_CREATIONS_DIR: creations, FINALIZE_EDITS_DIR: edits }
const hash = (bytes) => 'sha256:' + createHash('sha256').update(bytes).digest('hex')
const at = '2026-07-12T00:00:00.000000Z'
const sorted = (value) => Array.isArray(value) ? value.map(sorted) : value && typeof value === 'object'
  ? Object.fromEntries(Object.keys(value).sort().map((key) => [key, sorted(value[key])])) : value
const canonical = (value) => JSON.stringify(sorted(value), null, 2) + '\n'
let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }
function acquire(stem) {
  return spawnSync(process.execPath, [cli, 'acquire', '--guard-finalization', '--kind', 'task-session', '--stem', stem, '--key', 'standby:prep'], {
    cwd: root, env, encoding: 'utf8'
  })
}

try {
  check('publication guard admits and releases a clean writer lease', () => {
    const result = acquire('TASK_1_clean')
    assert.equal(result.status, 0, result.stderr)
    const receipt = JSON.parse(result.stdout)
    const released = spawnSync(process.execPath, [cli, 'release', '--lease-id', receipt.leaseId, '--token', receipt.token], { cwd: root, env, encoding: 'utf8' })
    assert.equal(released.status, 0, released.stderr)
  })

  check('post-lease guard rejects an incomplete deterministic creation and withdraws its lease', () => {
    const intent = {
      version: 1, title: 'Creation', body: '', originStem: null, dedupKey: null, dedupReport: null,
      source: taskSource.manualForIntent('publication-guard', 'manual', 'fixture:publication-guard')
    }
    const keyHash = hash(Buffer.from('publication-guard-key', 'ascii'))
    const marker = {
      version: 2, transactionId: '1'.repeat(32), keyHash, payloadHash: creationContract.digest(intent), intent,
      status: 'incomplete', phase: 'claimed', effect: null, number: null, slug: null, stem: null, sourceHash: null,
      column: null, createdAt: at, updatedAt: at, revision: 1, lastError: null, targetProof: null
    }
    writeFileSync(join(creations, keyHash.slice(7) + '.json'), JSON.stringify(marker) + '\n')
    const result = acquire('TASK_2_creation_blocked')
    assert.equal(result.status, 2)
    assert.match(result.stderr, /CREATION_INCOMPLETE/)
    assert.equal(writerLeases.scan(join(finalizations, '.writers')).active.length, 0)
    rmSync(join(creations, keyHash.slice(7) + '.json'))
  })

  check('post-lease guard rejects an incomplete deterministic edit and withdraws its lease', () => {
    const stem = 'TASK_3_edit_blocked'
    const bytes = Buffer.from('# TASK 3 — Edit blocked\n')
    const marker = {
      version: 1, transactionId: '2'.repeat(32), stem, expectedSourceHash: hash(bytes), requestedSourceHash: hash(bytes),
      recoveryMarkdownBase64: bytes.toString('base64'), status: 'incomplete', phase: 'claimed', effect: null,
      sourceHash: null, createdAt: at, updatedAt: at, revision: 1, lastError: null
    }
    assert.equal(editContract.validateRecord(marker, stem), null)
    writeFileSync(join(edits, stem + '.json'), canonical(marker))
    const result = acquire(stem)
    assert.equal(result.status, 2)
    assert.match(result.stderr, /EDIT_INCOMPLETE/)
    assert.equal(writerLeases.scan(join(finalizations, '.writers')).active.length, 0)
  })

  console.log(`publication-guard: ${checks} checks passed`)
} finally {
  rmSync(root, { recursive: true, force: true })
}
