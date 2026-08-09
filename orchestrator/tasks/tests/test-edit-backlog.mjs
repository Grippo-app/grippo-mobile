#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash, randomBytes } from 'node:crypto'
import { spawn, spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'

const repo = fileURLToPath(new URL('../../..', import.meta.url))
const script = join(repo, 'orchestrator', 'tasks', 'edit-backlog.py')
const createScript = join(repo, 'orchestrator', 'tasks', 'create-backlog.py')
const regen = join(repo, 'orchestrator', 'tasks', 'regen-index.py')
const markerCli = join(repo, 'orchestrator', 'tasks', 'edit-marker.mjs')
const require = createRequire(import.meta.url)
const markerContract = require('../edit-marker-contract.cjs')
const durableCas = markerContract.durableCas
const writerLeases = require('../writer-leases.cjs')
const taskSource = require('../task-source-contract.cjs')
const TASK_MAX_BYTES = 64 * 1024 + 4096
let checks = 0
const roots = []
const activeChildren = new Set()
let cleanupStarted = false
let cleanupFailed = false

function trackedSpawn(...args) {
  const child = spawn(...args)
  activeChildren.add(child)
  const forget = () => { activeChildren.delete(child) }
  child.once('close', forget)
  child.once('error', forget)
  return child
}

function cleanupTestRoots() {
  if (cleanupStarted) return []
  cleanupStarted = true

  for (const child of activeChildren) {
    if (child.exitCode === null && child.signalCode === null) {
      try { child.kill('SIGKILL') } catch {}
    }
  }

  const failures = []
  for (const root of new Set(roots)) {
    try {
      rmSync(root, { recursive: true, force: true, maxRetries: 3, retryDelay: 20 })
    } catch (error) {
      failures.push({ root, error })
    }
  }
  return failures
}

function reportCleanupFailures(failures) {
  for (const { root, error } of failures) {
    cleanupFailed = true
    console.error(`cleanup failed for test root ${root}: ${error && error.message || error}`)
  }
  if (cleanupFailed && !process.exitCode) process.exitCode = 1
}

process.once('exit', () => { reportCleanupFailures(cleanupTestRoots()) })
const cleanupSignals = process.platform === 'win32'
  ? ['SIGINT', 'SIGTERM', 'SIGBREAK']
  : ['SIGHUP', 'SIGINT', 'SIGTERM']
for (const signal of cleanupSignals) {
  process.once(signal, () => {
    reportCleanupFailures(cleanupTestRoots())
    try { process.kill(process.pid, signal) } catch { process.exit(1) }
  })
}

function hash(bytes) { return 'sha256:' + createHash('sha256').update(bytes).digest('hex') }
function sourceBlock(number) {
  const ref = `fixture-task-${number}`
  return taskSource.render(taskSource.manualForIntent(ref, 'manual', ref))
}
function taskMarkdown(number, title, body = '') {
  const suffix = String(body || '').replace(/^\n+|\n+$/g, '')
  return `# TASK ${number} — ${title}\n\n${sourceBlock(number)}${suffix ? `\n\n${suffix}` : ''}\n`
}
function taskBytes(number, title, body = '') { return Buffer.from(taskMarkdown(number, title, body)) }
function sorted(value) {
  if (Array.isArray(value)) return value.map(sorted)
  if (value && typeof value === 'object') {
    const out = {}
    for (const key of Object.keys(value).sort()) out[key] = sorted(value[key])
    return out
  }
  return value
}
function canonical(value) { return JSON.stringify(sorted(value), null, 2) + '\n' }
function makeProject() {
  const root = mkdtempSync(join(tmpdir(), 'edit-backlog-'))
  roots.push(root)
  const tasks = join(root, 'orchestrator', 'tasks')
  const cache = join(root, 'orchestrator', '.cache', 'tasks')
  for (const col of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasks, col), { recursive: true })
  mkdirSync(cache, { recursive: true })
  writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify({ version: 2, generatedAt: '1970-01-01T00:00:00Z', backlog: [], pending: [], todo: [], done: [] }, null, 2) + '\n')
  return { root, tasks, cache }
}
function env(p, extra = {}) {
  return {
    ...process.env,
    CREATE_BACKLOG_PROJECT_ROOT: p.root,
    CREATE_BACKLOG_TASKS_DIR: p.tasks,
    CREATE_BACKLOG_CACHE_DIR: p.cache,
    EDIT_BACKLOG_EDITS_DIR: join(p.cache, 'edits'),
    EDIT_BACKLOG_FINALIZATIONS_DIR: join(p.cache, 'finalizations'),
    EDIT_BACKLOG_NODE: process.execPath,
    EDIT_BACKLOG_TEST_ALLOW_UNLEASED: '1',
    ...extra
  }
}
function regenIndex(p) {
  const r = spawnSync('python3', [regen], { cwd: p.root, env: env(p), encoding: 'utf8' })
  assert.equal(r.status, 0, r.stderr)
}
function run(p, payload, extra = {}) {
  const r = spawnSync('python3', [script], { cwd: p.root, env: env(p, extra), input: JSON.stringify(payload), encoding: 'utf8' })
  const lines = r.stdout.trim().split(/\r?\n/)
  assert.equal(lines.shift(), 'READY')
  return { status: r.status, json: JSON.parse(lines.join('\n')), stderr: r.stderr }
}
function taskStateObservations(stderr) {
  return String(stderr || '').split(/\r?\n/).filter(line => line.startsWith('[task-state] '))
    .map(line => JSON.parse(line.slice('[task-state] '.length)))
}
function crash(p, payload, point) {
  const r = spawnSync('python3', [script], {
    cwd: p.root,
    env: env(p, { EDIT_BACKLOG_FAILPOINT: point }),
    input: JSON.stringify(payload),
    encoding: 'utf8'
  })
  assert.equal(r.status, 87, `failpoint ${point}: ${r.stderr}`)
  assert.equal(r.stdout.trim(), 'READY')
}
function recover(p, extra = {}) {
  const r = spawnSync('python3', [script, '--recover-all'], { cwd: p.root, env: env(p, extra), encoding: 'utf8' })
  const lines = r.stdout.trim().split(/\r?\n/)
  assert.equal(lines.shift(), 'READY')
  return { status: r.status, json: JSON.parse(lines.join('\n')) }
}
function writerGuard(p, extra = {}) {
  const finalizations = join(p.cache, 'finalizations')
  mkdirSync(finalizations, { recursive: true })
  const result = spawnSync(process.execPath, [
    join(repo, 'orchestrator', 'tasks', 'writer-lease.mjs'),
    'acquire', '--kind', 'standby-writer', '--key', 'test:anchored-marker-guard',
    '--guard-finalization'
  ], {
    cwd: p.root,
    env: {
      ...process.env,
      FINALIZE_PROJECT_ROOT: p.root,
      FINALIZE_STATE_DIR: finalizations,
      FINALIZE_CREATIONS_DIR: join(p.cache, 'creations'),
      FINALIZE_EDITS_DIR: join(p.cache, 'edits'),
      ...extra
    },
    encoding: 'utf8', maxBuffer: 2 * 1024 * 1024
  })
  if (result.status === 0) {
    const receipt = JSON.parse(result.stdout)
    spawnSync(process.execPath, [
      join(repo, 'orchestrator', 'tasks', 'writer-lease.mjs'), 'release',
      '--lease-id', receipt.leaseId, '--token', receipt.token
    ], {
      cwd: p.root,
      env: {
        ...process.env,
        FINALIZE_PROJECT_ROOT: p.root,
        FINALIZE_STATE_DIR: finalizations
      }, encoding: 'utf8'
    })
  }
  return result
}
async function runAsync(p, payload) {
  return await new Promise((resolve) => {
    const child = trackedSpawn('python3', [script], { cwd: p.root, env: env(p), stdio: ['pipe', 'pipe', 'pipe'] })
    let out = ''
    child.stdout.on('data', (c) => { out += c })
    child.on('close', (status) => resolve({ status, json: JSON.parse(out.trim().split(/\r?\n/).slice(1).join('\n')) }))
    child.stdin.end(JSON.stringify(payload))
  })
}
async function runAuthenticated(p, payload, options = {}) {
  const recovery = options.recovery === true
  const finalizations = join(p.cache, 'finalizations')
  const writers = join(finalizations, '.writers')
  const ownId = 'wr-' + randomBytes(20).toString('hex')
  const childEnv = env(p, {
    EDIT_BACKLOG_TEST_ALLOW_UNLEASED: '',
    EDIT_BACKLOG_OWN_WRITER_LEASE_ID: ownId,
    EDIT_BACKLOG_FINALIZATIONS_DIR: finalizations,
    EDIT_BACKLOG_NODE: process.execPath
  })
  return await new Promise((resolve, reject) => {
    const wrapper = [
      '-c',
      'import os,sys; os.read(3,1); os.execv(sys.executable,[sys.executable,sys.argv[1],*sys.argv[2:]])',
      script, ...(recovery ? ['--recover-all'] : []),
    ]
    const child = trackedSpawn('python3', wrapper, {
      cwd: p.root, env: childEnv, stdio: ['pipe', 'pipe', 'pipe', 'pipe']
    })
    let stdout = '', stderr = ''
    child.stdout.on('data', (chunk) => { stdout += chunk })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    const handles = []
    try {
      handles.push(writerLeases.acquire(writers, {
        leaseId: ownId,
        kind: 'workspace-session',
        stem: recovery ? null : payload.stem,
        key: recovery ? 'task:recover-backlog-edits' : `task:edit-backlog:${payload.stem}`,
        ownerPid: process.pid,
        childPid: child.pid
      }))
      if (options.reusedChildPid && (process.platform === 'linux' || process.platform === 'darwin')) {
        const row = JSON.parse(readFileSync(handles[0].path, 'utf8'))
        row.childProcessStartId = `psid-v1:${process.platform}:${'0'.repeat(64)}`
        writeFileSync(handles[0].path, JSON.stringify(row, null, 2) + '\n')
      }
      if (options.foreign) {
        handles.push(writerLeases.acquire(writers, {
          kind: 'task-session', stem: payload.stem, key: `task:${payload.stem}`,
          ownerPid: process.pid, childPid: process.pid
        }))
      }
      child.stdio[3].end('1')
      child.stdin.end(recovery ? '' : JSON.stringify(payload))
    } catch (error) {
      try { child.kill('SIGKILL') } catch {}
      reject(error)
      return
    }
    child.on('error', reject)
    child.on('close', (status) => {
      for (const handle of handles.reverse()) {
        try { writerLeases.release(handle) } catch {}
      }
      const lines = stdout.trim().split(/\r?\n/)
      try {
        assert.equal(lines.shift(), 'READY', stderr)
        resolve({ status, json: JSON.parse(lines.join('\n')), stderr })
      } catch (error) { reject(error) }
    })
  })
}
async function check(name, fn) { await fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  await check('recover-all readiness blocks pre-EOF work, accepts empty EOF, and rejects control bytes', async () => {
    const delayed = makeProject()
    const child = trackedSpawn('python3', [script, '--recover-all'], {
      cwd: delayed.root,
      env: env(delayed, { EDIT_BACKLOG_TEST_ALLOW_UNLEASED: '' }),
      stdio: ['pipe', 'pipe', 'pipe'],
    })
    let stdout = '', stderr = ''
    child.stdout.on('data', chunk => { stdout += chunk })
    child.stderr.on('data', chunk => { stderr += chunk })
    const closed = new Promise((resolve, reject) => {
      child.once('error', reject)
      child.once('close', status => resolve(status))
    })
    await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('timed out waiting for recovery READY')), 5000)
      const inspect = () => {
        if (stdout.includes('READY\n')) {
          clearTimeout(timeout)
          resolve()
        } else setTimeout(inspect, 5)
      }
      inspect()
    })
    await new Promise(resolve => setTimeout(resolve, 250))
    assert.equal(child.exitCode, null, 'recovery must remain fenced while controller stdin is open')
    assert.equal(existsSync(join(delayed.cache, 'edits')), false, 'recovery mutated marker state before readiness')
    assert.equal(existsSync(join(delayed.cache, 'finalizations')), false, 'recovery scanned or mutated writer state before readiness')
    child.stdin.end()
    assert.equal(await closed, 1)
    const delayedLines = stdout.trim().split(/\r?\n/)
    assert.equal(delayedLines.shift(), 'READY', stderr)
    assert.equal(JSON.parse(delayedLines.join('\n')).error.code, 'EDIT_WRITER_LEASE_MISSING')

    const ready = makeProject()
    const allowed = recover(ready)
    assert.equal(allowed.status, 0)
    assert.equal(allowed.json.ok, true)
    assert.equal(allowed.json.mode, 'recover-all')

    const malformed = makeProject()
    const before = readFileSync(join(malformed.tasks, 'INDEX.json'))
    const rejected = spawnSync('python3', [script, '--recover-all'], {
      cwd: malformed.root,
      env: env(malformed),
      input: 'not-an-empty-readiness-message',
      encoding: 'utf8',
    })
    const rejectedLines = rejected.stdout.trim().split(/\r?\n/)
    assert.equal(rejectedLines.shift(), 'READY')
    assert.equal(rejected.status, 2)
    assert.equal(JSON.parse(rejectedLines.join('\n')).error.code, 'RECOVERY_READINESS_INVALID')
    assert.deepEqual(readFileSync(join(malformed.tasks, 'INDEX.json')), before)
    assert.equal(existsSync(join(malformed.cache, 'edits')), false)
    assert.equal(existsSync(join(malformed.cache, 'finalizations')), false)
  })

  await check('shared durable CAS contract fixes names, artifacts, canonical manifests, and operation phases', () => {
    const name = '.durable-cas-' + 'a'.repeat(16) + '-' + 'b'.repeat(16) + '-' + 'c'.repeat(16)
    assert.equal(durableCas.isName(name), true)
    assert.equal(durableCas.classifyName(name), 'recovery-required')
    assert.equal(durableCas.classifyName('.durable-cas-nearly-valid'), 'unsafe')
    assert.equal(durableCas.classifyName('TASK_1_safe.md'), null)
    assert.equal(durableCas.classifyArtifactName('.candidate-partial-' + 'd'.repeat(16)), 'candidate-partial')
    assert.equal(durableCas.classifyArtifactName('.candidate-partial-' + 'D'.repeat(16)), null)
    const manifest = {
      version: 1,
      targetName: 'TASK_1_safe.md',
      owner: 'edit:0123456789abcdef0123456789abcdef',
      expectedProof: {
        dev: '1', ino: '2', mode: 420, size: 4, mtimeNs: '5', ctimeNs: '6',
        hash: 'sha256:' + '1'.repeat(64)
      },
      candidateHash: 'sha256:' + '2'.repeat(64),
      maxBytes: 1024
    }
    const manifestBytes = durableCas.canonicalManifest(manifest)
    const preEpoch = JSON.parse(JSON.stringify(manifest))
    preEpoch.expectedProof.mtimeNs = '-5'
    assert.equal(durableCas.validateManifest(preEpoch), preEpoch)
    const negativeInode = JSON.parse(JSON.stringify(manifest))
    negativeInode.expectedProof.ino = '-1'
    assert.throws(() => durableCas.validateManifest(negativeInode), /source proof/)
    const negativeZeroTime = JSON.parse(JSON.stringify(manifest))
    negativeZeroTime.expectedProof.mtimeNs = '-0'
    assert.throws(() => durableCas.validateManifest(negativeZeroTime), /source proof/)
    assert.deepEqual(durableCas.validateOperationSnapshot([
      { name: 'manifest.json', kind: 'file', size: manifestBytes.length, bytes: manifestBytes },
      { name: 'candidate', kind: 'file', size: 8 },
      { name: 'source', kind: 'file', size: 4 }
    ]).phase, 'detached')
    assert.throws(() => durableCas.validateOperationSnapshot([
      { name: 'source', kind: 'file', size: 4 }
    ]), /manifest boundary/)
    const noncanonical = Buffer.from(JSON.stringify(manifest, null, 2) + '\n')
    assert.throws(() => durableCas.validateOperationSnapshot([
      { name: 'manifest.json', kind: 'file', size: noncanonical.length, bytes: noncanonical }
    ]), /not canonical/)

    const p = makeProject()
    const source = join(p.tasks, 'backlog', 'TASK_1_wire.md')
    writeFileSync(source, '# TASK 1 — Wire proof\n')
    const probe = spawnSync('python3', ['-c', [
      'import base64,json,os,runpy,sys',
      'lib=runpy.run_path(sys.argv[1],run_name="cas_wire_probe")',
      'data=open(sys.argv[2],"rb").read()',
      'proof=lib["_proof_from_stat"](os.stat(sys.argv[2]),data)',
      'value={"version":1,"targetName":"TASK_1_wire.md","owner":"edit:wire","expectedProof":proof,"candidateHash":lib["sha256"](data),"maxBytes":65536}',
      'raw=lib["_cas_manifest_bytes"](value)',
      'print(json.dumps({"manifest":value,"bytes":base64.b64encode(raw).decode("ascii")}))'
    ].join(';'), createScript, source], { cwd: p.root, env: env(p), encoding: 'utf8' })
    assert.equal(probe.status, 0, probe.stderr)
    const actual = JSON.parse(probe.stdout)
    for (const field of ['dev', 'ino', 'mtimeNs', 'ctimeNs']) {
      assert.match(actual.manifest.expectedProof[field], /^(?:0|[1-9][0-9]{0,19})$/)
    }
    assert.ok(BigInt(actual.manifest.expectedProof.mtimeNs) > BigInt(Number.MAX_SAFE_INTEGER))
    durableCas.validateManifest(actual.manifest)
    assert.deepEqual(durableCas.canonicalManifest(actual.manifest), Buffer.from(actual.bytes, 'base64'))
  })

  await check('compare-and-swap edit publishes exact bytes and a fresh canonical INDEX', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_1_edit_me', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(1, 'Old title', '## Goal\nOld.')
    writeFileSync(file, old); regenIndex(p)
    const markdown = taskMarkdown(1, 'New title', '## Goal\nNew.')
    const result = run(p, { version: 1, stem, expectedSourceHash: hash(old), markdown })
    assert.equal(result.status, 0)
    assert.equal(result.json.changed, true)
    assert.equal(readFileSync(file, 'utf8'), markdown)
    const idx = JSON.parse(readFileSync(join(p.tasks, 'INDEX.json'), 'utf8'))
    assert.equal(idx.backlog[0].title, 'New title')
    const events = taskStateObservations(result.stderr)
    assert.equal(events.length, 12,
      'four edit validations plus one six-scan publish and one two-scan check must all be observed')
    assert.equal(events.filter(event => event.action === 'index-publish').length, 6)
    assert.equal(events.filter(event => event.action === 'index-check').length, 2)
    assert.equal(events.filter(event => !String(event.action || '').startsWith('index-')).length, 4)
    assert.equal(events.filter(event => event.action === 'edit').length, 4)
    for (const event of events) {
      assert.equal(event.event, 'task-state-validation')
      assert.equal(event.caller, 'server')
      assert.ok(['valid', 'invalid'].includes(event.result))
      assert.equal(event.findings.every(item => Object.keys(item).sort().join(',') === 'code,severity'), true)
    }
    assert.doesNotMatch(JSON.stringify(events), /New title|Old title|## Goal|\/tmp\//)
    const checkResult = spawnSync('python3', [regen, '--check'], { cwd: p.root, env: env(p), encoding: 'utf8' })
    assert.equal(checkResult.status, 0, checkResult.stderr)
  })

  await check('stale hash and changed task column fail without touching bytes', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_2_stale', file = join(p.tasks, 'backlog', stem + '.md')
    const bytes = taskBytes(2, 'Stable'); writeFileSync(file, bytes); regenIndex(p)
    const stale = run(p, { version: 1, stem, expectedSourceHash: 'sha256:' + '0'.repeat(64), markdown: taskMarkdown(2, 'Changed') })
    assert.equal(stale.status, 2); assert.equal(stale.json.error.code, 'SOURCE_CHANGED'); assert.deepEqual(readFileSync(file), bytes)
    writeFileSync(join(p.tasks, 'pending', stem + '.questions.md'), '---\nround: 1\n---\n')
    const pending = run(p, { version: 1, stem, expectedSourceHash: hash(bytes), markdown: taskMarkdown(2, 'Changed') })
    assert.equal(pending.status, 1); assert.equal(pending.json.error.code, 'TASK_NOT_IDLE_BACKLOG'); assert.deepEqual(readFileSync(file), bytes)
  })

  await check('heading cannot change the task number', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_3_heading', file = join(p.tasks, 'backlog', stem + '.md')
    const bytes = taskBytes(3, 'Stable'); writeFileSync(file, bytes); regenIndex(p)
    const result = run(p, { version: 1, stem, expectedSourceHash: hash(bytes), markdown: taskMarkdown(4, 'Wrong identity') })
    assert.equal(result.status, 2); assert.equal(result.json.error.code, 'MARKDOWN_HEADING_INVALID'); assert.deepEqual(readFileSync(file), bytes)
  })

  await check('only the canonical heading crosses the destructive edit boundary', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_6_canonical_heading', file = join(p.tasks, 'backlog', stem + '.md')
    const bytes = taskBytes(6, 'Stable'); writeFileSync(file, bytes); regenIndex(p)
    const invalid = [
      '#TASK 6 — Compact',
      '#  TASK 6 — Extra hash spacing',
      '#\tTASK 6 — Tab after hash',
      '# TASK\t6 — Tab before number',
      '# TASK 6— Missing separator spaces',
      '# TASK 6 - ASCII hyphen',
      '# TASK 06 — Leading zero',
      '# TASK 6 — Trailing space ',
      '# TASK 6 — '
    ]
    const marker = join(p.cache, 'edits', stem + '.json')
    for (const heading of invalid) {
      const result = run(p, { version: 1, stem, expectedSourceHash: hash(bytes), markdown: heading + '\n' })
      assert.equal(result.status, 2, heading)
      assert.equal(result.json.error.code, 'MARKDOWN_HEADING_INVALID', heading)
      assert.deepEqual(readFileSync(file), bytes, heading)
      assert.equal(existsSync(marker), false, heading)
    }

    const accepted = taskMarkdown(6, 'Canonical title', '## Goal\nStill canonical.')
    const result = run(p, { version: 1, stem, expectedSourceHash: hash(bytes), markdown: accepted })
    assert.equal(result.status, 0)
    assert.equal(readFileSync(file, 'utf8'), accepted)
    const index = JSON.parse(readFileSync(join(p.tasks, 'INDEX.json'), 'utf8'))
    assert.equal(index.backlog[0].title, 'Canonical title')
    const verified = spawnSync('python3', [regen, '--check'], { cwd: p.root, env: env(p), encoding: 'utf8' })
    assert.equal(verified.status, 0, verified.stderr)
  })

  await check('canonical proposal validation rejects an invalid live-state body before task mutation', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_7_proposal_gate', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(7, 'Stable', 'Unstructured backlog request.')
    writeFileSync(file, old); regenIndex(p)
    const indexBefore = readFileSync(join(p.tasks, 'INDEX.json'))
    const invalid = [
      taskMarkdown(7, 'Invalid edit').trimEnd(),
      '',
      '---',
      '',
      '## Outcome',
      '',
      '**Status**: completed',
      '',
    ].join('\n')
    const result = run(p, {
      version: 1, stem, expectedSourceHash: hash(old), markdown: invalid,
    })
    assert.equal(result.status, 1)
    assert.equal(result.json.error.code, 'TASK_STATE_EDIT_PROPOSAL_INVALID')
    assert.deepEqual(readFileSync(file), old)
    assert.deepEqual(readFileSync(join(p.tasks, 'INDEX.json')), indexBefore)
    const scan = markerContract.scan(join(p.cache, 'edits'))
    assert.equal(scan.issues.length, 0, JSON.stringify(scan))
    assert.equal(scan.incomplete.length, 0)
    assert.equal(scan.completed.length, 1)
    assert.equal(scan.completed[0].effect, 'aborted')
    assert.equal(scan.completed[0].lastError.code, 'TASK_STATE_EDIT_PROPOSAL_INVALID')
  })

  await check('INDEX failure leaves a durable forward-recovery marker and recover-all finishes it', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_4_rollback', file = join(p.tasks, 'backlog', stem + '.md')
    const bytes = taskBytes(4, 'Original'); writeFileSync(file, bytes); regenIndex(p)
    const badRegen = join(p.root, 'bad-regen.py'); writeFileSync(badRegen, 'raise SystemExit(9)\n')
    const changed = taskMarkdown(4, 'Changed')
    const result = run(p, { version: 1, stem, expectedSourceHash: hash(bytes), markdown: changed }, { CREATE_BACKLOG_REGEN_INDEX: badRegen })
    assert.equal(result.status, 1); assert.equal(result.json.error.code, 'INDEX_REGEN_FAILED'); assert.equal(result.json.recoverable, true)
    assert.equal(readFileSync(file, 'utf8'), changed)
    const scan = markerContract.scan(join(p.cache, 'edits'))
    assert.equal(scan.issues.length, 0, JSON.stringify(scan)); assert.equal(scan.incomplete.length, 1)
    const repaired = recover(p)
    assert.equal(repaired.status, 0); assert.equal(repaired.json.recoveredCount, 1)
    assert.equal(markerContract.scan(join(p.cache, 'edits')).incomplete.length, 0)
    const checkResult = spawnSync('python3', [regen, '--check'], { cwd: p.root, env: env(p), encoding: 'utf8' })
    assert.equal(checkResult.status, 0, checkResult.stderr)
  })

  await check('two concurrent edits with one source hash have one winner and no clobber', async () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_5_race', file = join(p.tasks, 'backlog', stem + '.md')
    const bytes = taskBytes(5, 'Original'); writeFileSync(file, bytes); regenIndex(p)
    const base = { version: 1, stem, expectedSourceHash: hash(bytes) }
    const [a, b] = await Promise.all([
      runAsync(p, { ...base, markdown: taskMarkdown(5, 'Winner A') }),
      runAsync(p, { ...base, markdown: taskMarkdown(5, 'Winner B') })
    ])
    assert.equal([a, b].filter((x) => x.status === 0).length, 1)
    assert.equal([a, b].filter((x) => x.json.error && x.json.error.code === 'SOURCE_CHANGED').length, 1)
    assert.match(readFileSync(file, 'utf8'), /^# TASK 5 — Winner [AB]\n\n## Source\n/)
  })

  await check('every pre-completion crash boundary recovers forward to exact Markdown and INDEX', () => {
    for (const [i, point] of ['after-marker', 'after-cas-manifest', 'after-detach', 'after-file', 'after-index'].entries()) {
      const p = makeProject(); roots.push(p.root)
      const number = 10 + i, stem = `TASK_${number}_crash_${i}`, file = join(p.tasks, 'backlog', stem + '.md')
      const old = taskBytes(number, 'Before')
      const markdown = taskMarkdown(number, `After ${point}`, 'Recovered.')
      writeFileSync(file, old); regenIndex(p)
      crash(p, { version: 1, stem, expectedSourceHash: hash(old), markdown }, point)
      const before = markerContract.scan(join(p.cache, 'edits'))
      assert.equal(before.issues.length, 0, JSON.stringify(before)); assert.equal(before.incomplete.length, 1)
      const result = recover(p)
      assert.equal(result.status, 0); assert.equal(result.json.recoveredCount, 1)
      assert.equal(readFileSync(file, 'utf8'), markdown)
      assert.equal(markerContract.scan(join(p.cache, 'edits')).incomplete.length, 0)
      assert.equal(spawnSync('python3', [regen, '--check'], { cwd: p.root, env: env(p), encoding: 'utf8' }).status, 0)
    }
  })

  await check('crash after completed receipt is an already-complete replay, not a second edit', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_20_completed_crash', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(20, 'Before'); writeFileSync(file, old); regenIndex(p)
    const markdown = taskMarkdown(20, 'After')
    crash(p, { version: 1, stem, expectedSourceHash: hash(old), markdown }, 'after-complete')
    const first = readFileSync(file)
    const result = recover(p)
    assert.equal(result.status, 0); assert.equal(result.json.recoveredCount, 0); assert.equal(result.json.alreadyCompleted, 1)
    assert.deepEqual(readFileSync(file), first)
  })

  await check('an incomplete edit globally blocks a new edit until recover-all completes it', () => {
    const p = makeProject(); roots.push(p.root)
    const stemA = 'TASK_30_first', stemB = 'TASK_31_second'
    const fileA = join(p.tasks, 'backlog', stemA + '.md'), fileB = join(p.tasks, 'backlog', stemB + '.md')
    const oldA = taskBytes(30, 'First'), oldB = taskBytes(31, 'Second')
    writeFileSync(fileA, oldA); writeFileSync(fileB, oldB); regenIndex(p)
    crash(p, { version: 1, stem: stemA, expectedSourceHash: hash(oldA), markdown: taskMarkdown(30, 'First edited') }, 'after-marker')
    const prepGuard = spawnSync(process.execPath, [markerCli, 'guard', '--stem', stemB], {
      cwd: p.root, env: env(p, { EDIT_BACKLOG_EDITS_DIR: join(p.cache, 'edits') }), encoding: 'utf8'
    })
    assert.equal(prepGuard.status, 2); assert.equal(JSON.parse(prepGuard.stdout).issue.stem, stemA)
    const blocked = run(p, { version: 1, stem: stemB, expectedSourceHash: hash(oldB), markdown: taskMarkdown(31, 'Second edited') })
    assert.equal(blocked.status, 1); assert.equal(blocked.json.error.code, 'EDIT_RECOVERY_REQUIRED'); assert.deepEqual(readFileSync(fileB), oldB)
    assert.equal(recover(p).status, 0)
    const accepted = run(p, { version: 1, stem: stemB, expectedSourceHash: hash(oldB), markdown: taskMarkdown(31, 'Second edited') })
    assert.equal(accepted.status, 0)
  })

  await check('recovery refuses a target that matches neither side of the durable CAS', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_40_diverged', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(40, 'Before'); writeFileSync(file, old); regenIndex(p)
    crash(p, { version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(40, 'Intended') }, 'after-marker')
    const foreign = taskMarkdown(40, 'Foreign writer'); writeFileSync(file, foreign)
    const result = recover(p)
    assert.equal(result.status, 1); assert.equal(result.json.error.code, 'EDIT_TARGET_DIVERGED')
    assert.equal(readFileSync(file, 'utf8'), foreign)
    assert.equal(markerContract.scan(join(p.cache, 'edits')).incomplete.length, 1)
  })

  await check('canonical marker validation fails closed in Python and the task-prep guard CLI', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_50_corrupt', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(50, 'Before'); writeFileSync(file, old); regenIndex(p)
    const first = run(p, { version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(50, 'Edited') })
    assert.equal(first.status, 0)
    const markerFile = join(p.cache, 'edits', stem + '.json')
    writeFileSync(markerFile, readFileSync(markerFile, 'utf8').replace(/^\{\n/, '{ \n'))
    const current = readFileSync(file)
    const blocked = run(p, { version: 1, stem, expectedSourceHash: hash(current), markdown: taskMarkdown(50, 'Again') })
    assert.equal(blocked.status, 1); assert.equal(blocked.json.error.code, 'EDIT_RECOVERY_SCAN_FAILED'); assert.deepEqual(readFileSync(file), current)
    const guard = spawnSync(process.execPath, [markerCli, 'guard', '--stem', stem], {
      cwd: p.root, env: env(p, { EDIT_BACKLOG_EDITS_DIR: join(p.cache, 'edits') }), encoding: 'utf8'
    })
    assert.equal(guard.status, 2); assert.equal(JSON.parse(guard.stdout).issue.code, 'EDIT_MARKER_INVALID')
  })

  await check('Python and JS both reject microsecond-reversed marker timestamps', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_51_timestamp_parity', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(51, 'Before'); writeFileSync(file, old); regenIndex(p)
    assert.equal(run(p, { version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(51, 'Edited') }).status, 0)
    const markerFile = join(p.cache, 'edits', stem + '.json')
    const marker = JSON.parse(readFileSync(markerFile, 'utf8'))
    marker.createdAt = '2026-01-01T00:00:00.000999Z'
    marker.updatedAt = '2026-01-01T00:00:00.000001Z'
    writeFileSync(markerFile, canonical(marker))
    const scan = markerContract.scan(join(p.cache, 'edits'))
    assert.equal(scan.issues.length, 1); assert.match(scan.issues[0].message, /timestamps are out of order/)
    const current = readFileSync(file)
    const python = run(p, { version: 1, stem, expectedSourceHash: hash(current), markdown: taskMarkdown(51, 'Again') })
    assert.equal(python.status, 1); assert.equal(python.json.error.code, 'EDIT_RECOVERY_SCAN_FAILED'); assert.deepEqual(readFileSync(file), current)
  })

  await check('Python and JS both reject unpaired Unicode surrogates in marker strings', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_52_unicode_scalar', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(52, 'Before'); writeFileSync(file, old); regenIndex(p)
    assert.equal(run(p, { version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(52, 'Edited') }).status, 0)
    const markerFile = join(p.cache, 'edits', stem + '.json')
    const marker = JSON.parse(readFileSync(markerFile, 'utf8'))
    marker.effect = 'aborted'; marker.sourceHash = null
    marker.lastError = { code: 'BROKEN\uD800', message: 'invalid scalar', at: marker.updatedAt }
    writeFileSync(markerFile, canonical(marker))
    const scan = markerContract.scan(join(p.cache, 'edits'))
    assert.equal(scan.issues.length, 1); assert.match(scan.issues[0].message, /lastError is invalid/)
    const python = recover(p)
    assert.equal(python.status, 1); assert.equal(python.json.error.code, 'EDIT_RECOVERY_SCAN_FAILED')
  })

  await check('Python and JS reject the same non-canonical recovery heading before mutation', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_53_heading_parity', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(53, 'Before'); writeFileSync(file, old); regenIndex(p)
    crash(p, { version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(53, 'Intended') }, 'after-marker')
    const markerFile = join(p.cache, 'edits', stem + '.json')
    const marker = JSON.parse(readFileSync(markerFile, 'utf8'))
    const permissive = Buffer.from('#TASK 53 — Guard drift\n')
    marker.recoveryMarkdownBase64 = permissive.toString('base64')
    marker.requestedSourceHash = hash(permissive)
    writeFileSync(markerFile, canonical(marker))
    const scan = markerContract.scan(join(p.cache, 'edits'))
    assert.equal(scan.issues.length, 1)
    assert.match(scan.issues[0].message, /recovery heading is invalid/)
    const python = recover(p)
    assert.equal(python.status, 1)
    assert.equal(python.json.error.code, 'EDIT_RECOVERY_SCAN_FAILED')
    assert.deepEqual(readFileSync(file), old)
  })

  await check('symlink marker files and marker directories are rejected without touching the task', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_60_symlink', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(60, 'Stable'); writeFileSync(file, old); regenIndex(p)
    const edits = join(p.cache, 'edits'); mkdirSync(edits, { recursive: true })
    const outside = join(p.root, 'outside.json'); writeFileSync(outside, '{}\n'); symlinkSync(outside, join(edits, stem + '.json'))
    const result = run(p, { version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(60, 'Changed') })
    assert.equal(result.status, 1); assert.equal(result.json.error.code, 'EDIT_RECOVERY_SCAN_FAILED'); assert.deepEqual(readFileSync(file), old)

    const q = makeProject(); roots.push(q.root)
    const qStem = 'TASK_61_dir_symlink', qFile = join(q.tasks, 'backlog', qStem + '.md')
    const qOld = taskBytes(61, 'Stable'); writeFileSync(qFile, qOld); regenIndex(q)
    const outsideDir = join(q.root, 'outside-edits'); mkdirSync(outsideDir); symlinkSync(outsideDir, join(q.cache, 'edits'))
    const qResult = run(q, { version: 1, stem: qStem, expectedSourceHash: hash(qOld), markdown: taskMarkdown(61, 'Changed') })
    assert.equal(qResult.status, 1); assert.equal(qResult.json.error.code, 'EDIT_MARKER_DIR_UNSAFE'); assert.deepEqual(readFileSync(qFile), qOld)
  })

  await check('a symlinked cache ancestor is rejected before edit runtime creates through it', () => {
    if (process.platform === 'win32') return
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_62_ancestor_symlink', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(62, 'Stable'); writeFileSync(file, old); regenIndex(p)
    rmSync(join(p.root, 'orchestrator', '.cache'), { recursive: true })
    const outside = join(p.root, 'outside-cache'); mkdirSync(outside)
    symlinkSync(outside, join(p.root, 'orchestrator', '.cache'))
    const result = run(p, { version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(62, 'Changed') })
    assert.equal(result.status, 1)
    assert.equal(result.json.error.code, 'UNSAFE_DIRECTORY')
    assert.deepEqual(readFileSync(file), old)
    assert.deepEqual(readdirSync(outside), [], 'edit runtime must not create marker/cache state through the symlink')
  })

  await check('production edit requires an attached authenticated writer lease', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_70_lease_required', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(70, 'Stable'); writeFileSync(file, old); regenIndex(p)
    const result = run(p, { version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(70, 'Changed') }, { EDIT_BACKLOG_TEST_ALLOW_UNLEASED: '' })
    assert.equal(result.status, 1); assert.equal(result.json.error.code, 'EDIT_WRITER_LEASE_MISSING'); assert.deepEqual(readFileSync(file), old)
    const scan = markerContract.scan(join(p.cache, 'edits'))
    assert.equal(scan.incomplete.length, 0); assert.equal(scan.completed.length, 0,
      'unauthenticated helpers must not be allowed to publish even an aborted marker')
  })

  await check('authenticated edit succeeds with only its own lease and rejects a concurrent task writer', async () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_80_writer_handshake', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(80, 'Stable'); writeFileSync(file, old); regenIndex(p)
    const blocked = await runAuthenticated(p, { version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(80, 'Blocked') }, { foreign: true })
    assert.equal(blocked.status, 1); assert.equal(blocked.json.error.code, 'WORKSPACE_WRITER_ACTIVE'); assert.deepEqual(readFileSync(file), old)
    if (process.platform === 'linux' || process.platform === 'darwin') {
      const reused = await runAuthenticated(p, {
        version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(80, 'Reused PID')
      }, { reusedChildPid: true })
      assert.equal(reused.status, 1)
      assert.equal(reused.json.error.code, 'EDIT_WRITER_LEASE_INVALID')
      assert.deepEqual(readFileSync(file), old,
        'the same numeric child PID with another start identity must not authorize an edit')
    }
    const acceptedMarkdown = taskMarkdown(80, 'Accepted')
    const accepted = await runAuthenticated(p, { version: 1, stem, expectedSourceHash: hash(old), markdown: acceptedMarkdown })
    assert.equal(accepted.status, 0); assert.equal(accepted.json.changed, true); assert.equal(readFileSync(file, 'utf8'), acceptedMarkdown)
  })

  await check('authenticated recover-all uses the same writer exclusion handshake', async () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_90_recovery_lease', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(90, 'Stable'); writeFileSync(file, old); regenIndex(p)
    const recoveredMarkdown = taskMarkdown(90, 'Recovered')
    crash(p, { version: 1, stem, expectedSourceHash: hash(old), markdown: recoveredMarkdown }, 'after-file')
    const blocked = await runAuthenticated(p, { stem }, { recovery: true, foreign: true })
    assert.equal(blocked.status, 1, JSON.stringify(blocked)); assert.equal(blocked.json.error.code, 'WORKSPACE_WRITER_ACTIVE')
    const recovered = await runAuthenticated(p, { stem }, { recovery: true })
    assert.equal(recovered.status, 0); assert.equal(recovered.json.recoveredCount, 1); assert.equal(readFileSync(file, 'utf8'), recoveredMarkdown)
  })

  await check('no-op edits produce an exact unchanged receipt and large UTF-8-safe edits remain recoverable', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_100_noop', file = join(p.tasks, 'backlog', stem + '.md')
    const bytes = taskBytes(100, 'Same'); writeFileSync(file, bytes); regenIndex(p)
    const same = run(p, { version: 1, stem, expectedSourceHash: hash(bytes), markdown: bytes.toString('utf8') })
    assert.equal(same.status, 0); assert.equal(same.json.changed, false)
    let scan = markerContract.scan(join(p.cache, 'edits'))
    assert.equal(scan.issues.length, 0); assert.equal(scan.completed[0].effect, 'unchanged')

    const large = taskMarkdown(100, 'Large', 'я'.repeat(30_000))
    const current = readFileSync(file)
    const changed = run(p, { version: 1, stem, expectedSourceHash: hash(current), markdown: large })
    assert.equal(changed.status, 0); assert.equal(readFileSync(file, 'utf8'), large)
    scan = markerContract.scan(join(p.cache, 'edits'))
    assert.equal(scan.issues.length, 0, JSON.stringify(scan)); assert.equal(scan.completed[0].effect, 'changed')
  })

  await check('canonical trailing newline is included in the exact edit byte limit', async () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_105_size_boundary', file = join(p.tasks, 'backlog', stem + '.md')
    const original = taskBytes(105, 'Original'); writeFileSync(file, original); regenIndex(p)
    const heading = `# TASK 105 — Boundary\n\n${sourceBlock(105)}\n\n`
    const overflow = heading + 'x'.repeat(TASK_MAX_BYTES - Buffer.byteLength(heading))
    assert.equal(Buffer.byteLength(overflow), TASK_MAX_BYTES)
    const rejected = await runAsync(p, { version: 1, stem, expectedSourceHash: hash(original), markdown: overflow })
    assert.equal(rejected.status, 2)
    assert.equal(rejected.json.error.code, 'MARKDOWN_INVALID')
    assert.deepEqual(readFileSync(file), original)
    assert.equal(existsSync(join(p.cache, 'edits', stem + '.json')), false)

    const exact = heading + 'x'.repeat(TASK_MAX_BYTES - 1 - Buffer.byteLength(heading))
    assert.equal(Buffer.byteLength(exact), TASK_MAX_BYTES - 1)
    const accepted = await runAsync(p, { version: 1, stem, expectedSourceHash: hash(original), markdown: exact })
    assert.equal(accepted.status, 0)
    assert.equal(readFileSync(file).length, TASK_MAX_BYTES)
    assert.equal(readFileSync(file, 'utf8'), exact + '\n')
  })

  await check('a symlink backlog body is rejected and its external target is never overwritten', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_110_task_symlink', file = join(p.tasks, 'backlog', stem + '.md')
    const outside = join(p.root, 'external-task.md'), bytes = Buffer.from('# TASK 110 — External\n')
    writeFileSync(outside, bytes); symlinkSync(outside, file)
    const result = run(p, { version: 1, stem, expectedSourceHash: hash(bytes), markdown: '# TASK 110 — Changed\n' })
    assert.equal(result.status, 1); assert.equal(result.json.error.code, 'TASK_STATE_UNSAFE'); assert.deepEqual(readFileSync(outside), bytes)
  })

  await check('canonical workspace cannot enable the fixture-only unleased edit bypass', () => {
    const probe = spawnSync('python3', ['-c', [
      'import runpy,sys',
      'lib=runpy.run_path(sys.argv[1],run_name="fixture_probe")',
      'print(int(lib["fixture_mode_enabled"]("EDIT_BACKLOG_TEST_ALLOW_UNLEASED")))'
    ].join(';'), script], {
      cwd: repo,
      env: {
        ...process.env,
        CREATE_BACKLOG_PROJECT_ROOT: repo,
        CREATE_BACKLOG_AUTHORITY_ROOT: repo,
        EDIT_BACKLOG_TEST_ALLOW_UNLEASED: '1',
        PYTHONDONTWRITEBYTECODE: '1'
      }, encoding: 'utf8'
    })
    assert.equal(probe.status, 0, probe.stderr)
    assert.equal(probe.stdout.trim(), '0')
  })

  await check('source CAS rejects a pre-detach swap and restores the foreign generation exactly', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_120_source_swap', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(120, 'Original'), foreign = taskBytes(120, 'Foreign')
    writeFileSync(file, old); regenIndex(p)
    const result = run(p, {
      version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(120, 'Intended')
    }, {
      EDIT_BACKLOG_TEST_SWAP_SOURCE_BEFORE_DETACH: '1',
      EDIT_BACKLOG_TEST_SWAP_SOURCE_BEFORE_DETACH_BASE64: foreign.toString('base64')
    })
    assert.equal(result.status, 1)
    assert.equal(result.json.error.code, 'CAS_SOURCE_CHANGED')
    assert.deepEqual(readFileSync(file), foreign)
    assert.equal(readdirSync(join(p.tasks, 'backlog')).some(name => name.startsWith('.durable-cas-')), false,
      'a restored pre-detach foreign generation must not leave recovery debris')
  })

  await check('source CAS never clobbers a foreign post-detach claimant and recovery stays fail-closed', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_121_target_claim', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(121, 'Original'), foreign = taskBytes(121, 'Foreign claimant')
    writeFileSync(file, old); regenIndex(p)
    const result = run(p, {
      version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(121, 'Intended')
    }, {
      EDIT_BACKLOG_TEST_CLAIM_TARGET_AFTER_DETACH: '1',
      EDIT_BACKLOG_TEST_CLAIM_TARGET_AFTER_DETACH_BASE64: foreign.toString('base64')
    })
    assert.equal(result.status, 1)
    assert.equal(result.json.error.code, 'CAS_TARGET_CONFLICT')
    assert.deepEqual(readFileSync(file), foreign)
    assert.ok(readdirSync(join(p.tasks, 'backlog')).some(name => name.startsWith('.durable-cas-')),
      'the exact detached source must remain quarantined while a foreign canonical name exists')
    const recovery = recover(p)
    assert.equal(recovery.status, 1)
    assert.equal(recovery.json.error.code, 'CAS_TARGET_CONFLICT')
    assert.deepEqual(readFileSync(file), foreign)
  })

  await check('edit marker CAS preserves a last-moment foreign marker generation', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_122_marker_swap', file = join(p.tasks, 'backlog', stem + '.md')
    const old = taskBytes(122, 'Original'); writeFileSync(file, old); regenIndex(p)
    assert.equal(run(p, { version: 1, stem, expectedSourceHash: hash(old), markdown: taskMarkdown(122, 'First') }).status, 0)
    const markerFile = join(p.cache, 'edits', stem + '.json')
    const foreign = JSON.parse(readFileSync(markerFile, 'utf8'))
    foreign.transactionId = 'e'.repeat(32)
    const foreignBytes = canonical(foreign)
    const live = readFileSync(file)
    const result = run(p, {
      version: 1, stem, expectedSourceHash: hash(live), markdown: taskMarkdown(122, 'Second')
    }, {
      EDIT_BACKLOG_TEST_SWAP_MARKER_BEFORE_DETACH: '1',
      EDIT_BACKLOG_TEST_SWAP_MARKER_BEFORE_DETACH_BASE64: Buffer.from(foreignBytes).toString('base64')
    })
    assert.equal(result.status, 1)
    assert.equal(result.json.error.code, 'CAS_SOURCE_CHANGED')
    assert.equal(readFileSync(markerFile, 'utf8'), foreignBytes)
    assert.deepEqual(readFileSync(file), live)
  })

  await check('duplicate edit transaction ids and impossible completed receipts fail with Python/JS parity', () => {
    const p = makeProject(); roots.push(p.root)
    const stems = ['TASK_130_duplicate_a', 'TASK_131_duplicate_b']
    for (const stem of stems) {
      const file = join(p.tasks, 'backlog', stem + '.md')
      const original = Buffer.from(`# ${stem.replace(/_/g, ' ')} — placeholder\n`)
      const number = stem.match(/^TASK_(\d+)_/)[1]
      const bytes = taskBytes(number, 'Original')
      writeFileSync(file, bytes)
    }
    regenIndex(p)
    for (const stem of stems) {
      const file = join(p.tasks, 'backlog', stem + '.md'), bytes = readFileSync(file)
      const number = stem.match(/^TASK_(\d+)_/)[1]
      assert.equal(run(p, { version: 1, stem, expectedSourceHash: hash(bytes), markdown: taskMarkdown(number, 'Edited') }).status, 0)
    }
    const markerA = join(p.cache, 'edits', stems[0] + '.json')
    const markerB = join(p.cache, 'edits', stems[1] + '.json')
    const a = JSON.parse(readFileSync(markerA, 'utf8')), b = JSON.parse(readFileSync(markerB, 'utf8'))
    b.transactionId = a.transactionId
    writeFileSync(markerB, canonical(b))
    assert.ok(markerContract.scan(join(p.cache, 'edits')).issues.some(issue => /not unique/.test(issue.message)))
    assert.equal(recover(p).json.error.code, 'EDIT_RECOVERY_SCAN_FAILED')

    const q = makeProject(); roots.push(q.root)
    const stem = 'TASK_132_impossible', file = join(q.tasks, 'backlog', stem + '.md')
    const bytes = taskBytes(132, 'Original'); writeFileSync(file, bytes); regenIndex(q)
    assert.equal(run(q, { version: 1, stem, expectedSourceHash: hash(bytes), markdown: taskMarkdown(132, 'Edited') }).status, 0)
    const markerFile = join(q.cache, 'edits', stem + '.json')
    const impossible = JSON.parse(readFileSync(markerFile, 'utf8'))
    impossible.sourceHash = impossible.expectedSourceHash
    writeFileSync(markerFile, canonical(impossible))
    assert.ok(markerContract.scan(join(q.cache, 'edits')).issues.length > 0)
    assert.equal(recover(q).json.error.code, 'EDIT_RECOVERY_SCAN_FAILED')
  })

  await check('edit mutation rejects an over-bound marker directory without a partial scan', () => {
    const p = makeProject(); roots.push(p.root)
    const stem = 'TASK_140_bounded', file = join(p.tasks, 'backlog', stem + '.md')
    const bytes = taskBytes(140, 'Stable'); writeFileSync(file, bytes); regenIndex(p)
    const edits = join(p.cache, 'edits'); mkdirSync(edits, { recursive: true })
    for (let i = 0; i <= 10_000; i++) writeFileSync(join(edits, `.bounded-${i}`), '')
    const result = run(p, {
      version: 1, stem, expectedSourceHash: hash(bytes), markdown: taskMarkdown(140, 'Changed')
    })
    assert.equal(result.status, 1)
    assert.equal(result.json.error.code, 'CAS_RECOVERY_SCAN_FAILED')
    assert.match(result.json.error.message, /10000-entry limit/)
    assert.deepEqual(readFileSync(file), bytes)
    const guarded = writerGuard(p)
    assert.equal(guarded.status, 2, guarded.stderr + guarded.stdout)
    assert.match(guarded.stderr, /publication guard refused acquisition/)
  })

  await check('anchored marker contract rejects a symlink hidden in a directory ancestor', () => {
    const p = makeProject(); roots.push(p.root)
    const cacheRoot = join(p.root, 'orchestrator', '.cache')
    const outside = join(p.root, 'outside-cache')
    rmSync(cacheRoot, { recursive: true, force: true })
    mkdirSync(join(outside, 'tasks', 'edits'), { recursive: true })
    symlinkSync(outside, cacheRoot, 'dir')
    const result = markerContract.scan(join(cacheRoot, 'tasks', 'edits'), p.root)
    assert.deepEqual(result.incomplete, [])
    assert.deepEqual(result.completed, [])
    assert.ok(result.issues.some(issue => issue.code === 'EDIT_MARKER_DIR_UNSAFE'))
  })

  await check('explicit marker authority is strict and never widens to an environment root', () => {
    const p = makeProject(); roots.push(p.root)
    const edits = join(p.cache, 'edits'); mkdirSync(edits, { recursive: true })
    const unrelated = join(p.root, 'unrelated-authority'); mkdirSync(unrelated)
    const previous = process.env.EDIT_BACKLOG_AUTHORITY_ROOT
    process.env.EDIT_BACKLOG_AUTHORITY_ROOT = p.root
    try {
      const result = markerContract.scan(edits, unrelated)
      assert.deepEqual(result.incomplete, [])
      assert.deepEqual(result.completed, [])
      assert.ok(result.issues.some(issue => issue.code === 'EDIT_MARKER_DIR_UNSAFE'))
    } finally {
      if (previous === undefined) delete process.env.EDIT_BACKLOG_AUTHORITY_ROOT
      else process.env.EDIT_BACKLOG_AUTHORITY_ROOT = previous
    }
  })

  await check('anchored missing-directory decision rejects appearance and leaves no fixture state', () => {
    const p = makeProject(); roots.push(p.root)
    const edits = join(p.cache, 'edits')
    assert.equal(existsSync(edits), false)
    process.env.EDIT_MARKER_SCAN_TEST_ROOT = p.root
    process.env.EDIT_MARKER_SCAN_TEST_CREATE_MISSING = '1'
    try {
      const result = markerContract.scan(edits, p.root)
      assert.ok(result.issues.some(issue => issue.code === 'EDIT_MARKER_DIR_UNSAFE'))
      assert.equal(existsSync(edits), false, 'fixture hook must restore the exact missing component')
    } finally {
      delete process.env.EDIT_MARKER_SCAN_TEST_CREATE_MISSING
      delete process.env.EDIT_MARKER_SCAN_TEST_ROOT
    }
  })

  await check('writer-lease consumer rejects swap-back, symlink, FIFO, and oversized marker snapshots', () => {
    const swapped = makeProject(); roots.push(swapped.root)
    const stem = 'TASK_150_swapback', file = join(swapped.tasks, 'backlog', stem + '.md')
    const bytes = taskBytes(150, 'Stable'); writeFileSync(file, bytes); regenIndex(swapped)
    crash(swapped, {
      version: 1, stem, expectedSourceHash: hash(bytes), markdown: taskMarkdown(150, 'Intended')
    }, 'after-marker')
    const foreign = join(swapped.root, 'empty-foreign-edits'); mkdirSync(foreign)
    const swapResult = writerGuard(swapped, {
      EDIT_MARKER_SCAN_TEST_ROOT: swapped.root,
      EDIT_MARKER_SCAN_TEST_SWAP_WITH: foreign
    })
    assert.equal(swapResult.status, 2, swapResult.stderr + swapResult.stdout)
    assert.match(swapResult.stderr, /publication guard refused acquisition/)
    assert.ok(existsSync(join(swapped.cache, 'edits', stem + '.json')),
      'the original anchored marker directory must be restored')
    assert.ok(existsSync(foreign), 'the foreign fixture directory must be restored')

    const unsafeCases = [
      ['symlink', (p, marker) => {
        const outside = join(p.root, 'outside-marker.json'); writeFileSync(outside, '{}\n'); symlinkSync(outside, marker)
      }],
      ['fifo', (p, marker) => {
        const made = spawnSync('mkfifo', [marker], { encoding: 'utf8' })
        assert.equal(made.status, 0, made.stderr)
      }],
      ['oversized', (_p, marker) => writeFileSync(marker, Buffer.alloc(markerContract.MAX_BYTES + 1, 0x61))]
    ]
    for (const [label, prepare] of unsafeCases) {
      const p = makeProject(); roots.push(p.root)
      const edits = join(p.cache, 'edits'); mkdirSync(edits, { recursive: true })
      const marker = join(edits, `TASK_${151 + unsafeCases.findIndex(row => row[0] === label)}_${label}.json`)
      prepare(p, marker)
      const result = writerGuard(p)
      assert.equal(result.status, 2, `${label}: ${result.stderr}${result.stdout}`)
      assert.match(result.stderr, /publication guard refused acquisition/, label)
    }
  })

} finally {
  reportCleanupFailures(cleanupTestRoots())
}
if (cleanupFailed) process.exit(1)
console.log(`edit-backlog: ${checks} checks passed`)
