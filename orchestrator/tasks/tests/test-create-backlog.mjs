#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import {
  existsSync, linkSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync,
  statSync, symlinkSync, unlinkSync, writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawn, spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'

const HERE = dirname(fileURLToPath(import.meta.url))
const TASKS_DIR = join(HERE, '..')
const SCRIPT = join(TASKS_DIR, 'create-backlog.py')
const REGEN = join(TASKS_DIR, 'regen-index.py')
const VALIDATOR = join(TASKS_DIR, 'validate-task-state.mjs')
const WRITER_CLI = join(TASKS_DIR, 'writer-lease.mjs')
const markerContract = createRequire(import.meta.url)('../creation-marker-contract.cjs')
const writerLeases = createRequire(import.meta.url)('../writer-leases.cjs')
const taskSource = createRequire(import.meta.url)('../task-source-contract.cjs')
const apiWorkPackage = createRequire(import.meta.url)('../api-work-package-contract.cjs')
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

function emptyIndex() {
  return {
    version: 2,
    generatedAt: '1970-01-01T00:00:00Z',
    backlog: [], pending: [], todo: [], done: [],
  }
}

function makeProject() {
  const root = mkdtempSync(join(tmpdir(), 'create-backlog-test-'))
  roots.push(root)
  const tasks = join(root, 'orchestrator', 'tasks')
  for (const column of ['backlog', 'pending', 'todo', 'done']) {
    mkdirSync(join(tasks, column), { recursive: true })
  }
  writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify(emptyIndex(), null, 2) + '\n')
  return { root, tasks, cache: join(root, 'orchestrator', '.cache', 'tasks') }
}

function envFor(project, extra = {}) {
  return {
    ...process.env,
    PYTHONDONTWRITEBYTECODE: '1',
    CREATE_BACKLOG_PROJECT_ROOT: project.root,
    CREATE_BACKLOG_TASKS_DIR: project.tasks,
    CREATE_BACKLOG_CACHE_DIR: project.cache,
    CREATE_BACKLOG_REGEN_INDEX: REGEN,
    CREATE_BACKLOG_MUTEX_TIMEOUT_MS: '10000',
    CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '1',
    ...extra,
  }
}

function basePayload(suffix = 'one') {
  const key = 'test.create.key.' + suffix
  const intentId = 'intent-' + createHash('sha256').update(key, 'ascii').digest('hex')
  return {
    version: 1,
    title: 'Create profile ' + suffix,
    body: '## Goal\n\nCreate the profile.\n',
    key,
    source: taskSource.manualForIntent(intentId, 'manual', intentId),
  }
}

function parseOutput(stdout, { allowMissingResult = false } = {}) {
  const lines = String(stdout).trimEnd().split(/\r?\n/)
  assert.equal(lines[0], 'READY', 'CLI must publish READY before reading stdin')
  if (allowMissingResult && lines.length === 1) return null
  assert.equal(lines.length, 2, 'CLI must emit exactly one JSON result after READY')
  return JSON.parse(lines[1])
}

function taskStateObservations(stderr) {
  return String(stderr || '').split(/\r?\n/).filter(line => line.startsWith('[task-state] '))
    .map(line => JSON.parse(line.slice('[task-state] '.length)))
}

function run(project, payload, extraEnv = {}) {
  const result = spawnSync('python3', [SCRIPT], {
    cwd: project.root,
    env: envFor(project, extraEnv),
    input: JSON.stringify(payload),
    encoding: 'utf8',
    maxBuffer: 2 * 1024 * 1024,
  })
  return { ...result, json: parseOutput(result.stdout) }
}

function runRaw(project, raw, extraEnv = {}) {
  const result = spawnSync('python3', [SCRIPT], {
    cwd: project.root,
    env: envFor(project, extraEnv),
    input: raw,
    encoding: 'utf8',
    maxBuffer: 2 * 1024 * 1024,
  })
  return { ...result, json: parseOutput(result.stdout) }
}

function recoverAll(project, extraEnv = {}) {
  const result = spawnSync('python3', [SCRIPT, '--recover-all'], {
    cwd: project.root,
    env: envFor(project, extraEnv),
    input: '',
    encoding: 'utf8',
    maxBuffer: 2 * 1024 * 1024,
  })
  return { ...result, json: parseOutput(result.stdout) }
}

function runAsync(project, payload, extraEnv = {}) {
  return new Promise((resolve, reject) => {
    const child = trackedSpawn('python3', [SCRIPT], {
      cwd: project.root,
      env: envFor(project, extraEnv),
      stdio: ['pipe', 'pipe', 'pipe'],
    })
    let stdout = '', stderr = ''
    child.stdout.on('data', chunk => { stdout += chunk })
    child.stderr.on('data', chunk => { stderr += chunk })
    child.on('error', reject)
    child.on('close', (code, signal) => {
      try { resolve({ status: code, signal, stdout, stderr, json: parseOutput(stdout) }) }
      catch (error) { reject(error) }
    })
    child.stdin.end(JSON.stringify(payload))
  })
}

async function waitForPath(path, timeoutMs = 10000) {
  const deadline = Date.now() + timeoutMs
  while (!existsSync(path)) {
    if (Date.now() >= deadline) throw new Error(`timed out waiting for ${path}`)
    await new Promise(resolve => setTimeout(resolve, 10))
  }
}

function runAuthenticated(project, payload, { foreign = false, recovery = false } = {}) {
  return new Promise((resolve, reject) => {
    const writers = join(project.cache, 'finalizations', '.writers')
    const own = writerLeases.acquire(writers, {
      rootDir: project.root,
      kind: 'workspace-session', stem: null,
      key: recovery ? 'task:recover-backlog-creations' : 'task:create-backlog',
      ownerPid: process.pid, pendingChild: true
    })
    const handles = [own]
    if (foreign) handles.push(writerLeases.acquire(writers, {
      rootDir: project.root,
      kind: 'task-session', stem: 'TASK_999_foreign', key: 'task:TASK_999_foreign', childPid: process.pid
    }))
    const child = trackedSpawn('python3', [SCRIPT].concat(recovery ? ['--recover-all'] : []), {
      cwd: project.root,
      env: envFor(project, {
        CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '',
        CREATE_BACKLOG_FINALIZATIONS_DIR: join(project.cache, 'finalizations'),
        CREATE_BACKLOG_OWN_WRITER_LEASE_ID: own.leaseId,
        CREATE_BACKLOG_NODE: process.execPath
      }),
      stdio: ['pipe', 'pipe', 'pipe']
    })
    writerLeases.updateChildPid(own, child.pid)
    let stdout = '', stderr = ''
    child.stdout.on('data', (chunk) => { stdout += chunk })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    child.on('error', reject)
    child.on('close', (status) => {
      for (const handle of handles.reverse()) { try { writerLeases.release(handle) } catch {} }
      try { resolve({ status, stderr, json: parseOutput(stdout) }) } catch (error) { reject(error) }
    })
    child.stdin.end(recovery ? '' : JSON.stringify(payload))
  })
}

function crash(project, payload, failpoint) {
  const result = spawnSync('python3', [SCRIPT], {
    cwd: project.root,
    env: envFor(project, { CREATE_BACKLOG_FAILPOINT: failpoint }),
    input: JSON.stringify(payload),
    encoding: 'utf8',
    maxBuffer: 2 * 1024 * 1024,
  })
  assert.equal(result.status, 86, `${failpoint} must simulate abrupt process death`)
  assert.equal(parseOutput(result.stdout, { allowMissingResult: true }), null)
  return result
}

function markerFor(project, key) {
  const digest = createHash('sha256').update(key, 'ascii').digest('hex')
  return JSON.parse(readFileSync(join(project.cache, 'creations', digest + '.json'), 'utf8'))
}

function markerFileFor(project, key) {
  const digest = createHash('sha256').update(key, 'ascii').digest('hex')
  return join(project.cache, 'creations', digest + '.json')
}

function renderedTask(payload, marker) {
  const lines = [`# TASK ${marker.number} — ${payload.title}`]
  const source = marker.intent && marker.intent.source
  if (source) lines.push('', '## Source', '',
    `- Kind: ${source.kind}`, `- Type: ${source.type}`, `- Ref: ${source.ref}`,
    `- Fingerprint: ${source.fingerprint}`)
  const body = payload.body.replace(/\n+$/, '')
  if (body) lines.push('', body)
  if (payload.originStem) lines.push('', '## Origin', `- split from ${payload.originStem}`)
  return Buffer.from(lines.join('\n').replace(/\n+$/, '') + '\n')
}

function exactFileProof(path) {
  const bytes = readFileSync(path)
  const st = statSync(path, { bigint: true })
  return {
    dev: String(st.dev), ino: String(st.ino), mode: Number(st.mode & 0o7777n),
    size: Number(st.size), mtimeNs: String(st.mtimeNs), ctimeNs: String(st.ctimeNs),
    hash: 'sha256:' + createHash('sha256').update(bytes).digest('hex'),
  }
}

function sortedTree(value) {
  if (Array.isArray(value)) return value.map(sortedTree)
  if (value && typeof value === 'object') {
    const out = {}
    for (const key of Object.keys(value).sort()) out[key] = sortedTree(value[key])
    return out
  }
  return value
}

function markerText(value) {
  return JSON.stringify(sortedTree(value), null, 2) + '\n'
}

function canonical(value) {
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']'
  if (value && typeof value === 'object') return '{' + Object.keys(value).sort().map(key => JSON.stringify(key) + ':' + canonical(value[key])).join(',') + '}'
  return JSON.stringify(value)
}

function taskFiles(project) {
  return readdirSync(join(project.tasks, 'backlog')).filter(name => /^TASK_.*\.md$/.test(name)).sort()
}

function readIndex(project) {
  return JSON.parse(readFileSync(join(project.tasks, 'INDEX.json'), 'utf8'))
}

function regenIndex(project) {
  const result = spawnSync('python3', [REGEN], {
    cwd: project.root, env: envFor(project), encoding: 'utf8',
  })
  assert.equal(result.status, 0, result.stderr || result.stdout)
}

function pendingDoc(stem) {
  return [
    '---', `forTask: ${stem}`, 'createdAt: 2026-07-13T08:00:00Z',
    'updatedAt: 2026-07-13T08:01:00Z', 'round: 1', 'gapCount: 1',
    'prevGapCount: 2', '---', '', '## Q1 — What must be preserved?', '',
    '**Type**: text', '', '### Answer', '',
  ].join('\n')
}

function sourceBlock(ref) {
  return taskSource.render(taskSource.manualForIntent(ref, 'manual', ref))
}

function todoDoc(number, title = 'todo') {
  return `# TASK ${number} — ${title}\n\n${sourceBlock(`fixture-task-${number}`)}\n\n## Goal\n\nImplement safely.\n\n## Inputs\n\n- Existing contracts.\n\n## Acceptance\n\n### Automated\n\n- Run \`node test/contract.mjs\`.\n\n## Out of scope\n\n- Unrelated work.\n`
}

function doneDoc(number) {
  return todoDoc(number, 'done').trimEnd() + `\n\n---\n\n## Outcome\n\n**Status**: completed\n**Completed at**: 2026-07-13T09:00:00Z\n**Reviewer**: codex\n**Review iterations**: 1\n\n### Build gates\n\n- \`node test/contract.mjs\` — pass\n\n### Runtime verify\n\n- Gate: skipped (no runtime-observable change)\n- Result: n/a — no runtime change\n\n### Acceptance trace\n\n- \`test/contract.mjs\` — verified — Passed.\n\n### Caveats\n\n- none\n\n### Follow-ups\n\n- none\n\n### Files touched\n\n- \`src/feature.js\` — modified\n`
}

let passed = 0
let failed = 0
async function check(name, fn) {
  try {
    await fn()
    passed++
    console.log('  ok:', name)
  } catch (error) {
    failed++
    console.error('  FAIL:', name)
    console.error(error && error.stack || error)
  }
}

await check('run_regen replaces malformed child telemetry and never forwards arbitrary stderr', () => {
  const p = makeProject()
  const secret = 'CREATE-REGEN-SECRET-2d93e00a'
  const fake = join(p.root, 'malformed-regen.py')
  writeFileSync(fake, [
    'import sys',
    `sys.stderr.write(${JSON.stringify(secret + '\n')})`,
    `sys.stderr.write(${JSON.stringify('[task-state] ' + '['.repeat(1200) + '0' + ']'.repeat(1200) + '\n')})`,
    `sys.stderr.write(${JSON.stringify('[task-state] {"version":1,"event":"task-state-validation","secret":"' + secret + '"}\n')})`,
    ''
  ].join('\n'))
  const program = [
    'import runpy, sys',
    'module = runpy.run_path(sys.argv[1], run_name="create_backlog_observation_test")',
    'assert module["run_regen"](False) == ("", "")'
  ].join('; ')
  const result = spawnSync('python3', ['-c', program, SCRIPT], {
    cwd: p.root,
    env: envFor(p, { CREATE_BACKLOG_REGEN_INDEX: fake, PYTHONDONTWRITEBYTECODE: '1' }),
    encoding: 'utf8'
  })
  assert.equal(result.status, 0, result.stdout + result.stderr)
  const events = taskStateObservations(result.stderr)
  assert.equal(events.length, 6)
  assert.deepEqual(events.map(event => event.phase), ['pre', 'pre', 'pre', 'post', 'post', 'post'])
  for (const event of events) {
    assert.equal(event.caller, 'server')
    assert.equal(event.scope, 'all')
    assert.equal(event.action, 'index-publish')
    assert.equal(event.result, 'invalid')
    assert.deepEqual(event.findings, [{ code: 'INDEX_OBSERVATION_INVALID', severity: 'blocker' }])
  }
  assert.doesNotMatch(result.stdout + result.stderr, new RegExp(secret))
})

await check('READY/JSON contract, exact creation, durable replay, and payload conflict', () => {
  const p = makeProject()
  const payload = basePayload('idempotent')
  const first = run(p, payload)
  assert.equal(first.status, 0)
  assert.equal(first.json.ok, true)
  assert.equal(first.json.created, true)
  assert.match(first.json.stem, /^TASK_1_[a-z0-9_]+$/)
  const bytes = readFileSync(join(p.tasks, 'backlog', first.json.stem + '.md'))
  assert.equal('sha256:' + createHash('sha256').update(bytes).digest('hex'), first.json.sourceHash)
  assert.match(bytes.toString('utf8'), /^# TASK 1 — .+\n\n## Source\n\n- Kind: manual\n- Type: manual\n- Ref: intent-[a-f0-9]{64}\n- Fingerprint: sha256:[a-f0-9]{64}\n\n## Goal/m)
  const idx = readIndex(p)
  assert.equal(idx.version, 2)
  assert.ok(Date.parse(idx.generatedAt) > 0, 'first task publication must replace the bootstrap INDEX sentinel')
  assert.deepEqual(idx.backlog.map(x => x.stem), [first.json.stem])
  assert.equal(idx.backlog[0].title, payload.title)
  assert.equal(idx.backlog[0].origin.kind, 'manual')
  assert.equal(Object.hasOwn(idx.backlog[0].origin, 'inferredLegacy'), false)

  const replay = run(p, payload)
  assert.equal(replay.status, 0)
  assert.equal(replay.json.stem, first.json.stem)
  assert.equal(replay.json.replayed, true)
  assert.equal(replay.json.deduped, true)
  assert.deepEqual(taskFiles(p), [first.json.stem + '.md'])
  assert.equal(markerFor(p, payload.key).intent, null, 'completed receipt must shed the recovery body')

  const conflict = run(p, { ...payload, title: 'Different title' })
  assert.equal(conflict.status, 2)
  assert.equal(conflict.json.error.code, 'IDEMPOTENCY_CONFLICT')
  assert.deepEqual(taskFiles(p), [first.json.stem + '.md'])
})

await check('API work-package provenance publishes through the canonical Python and JS registries', () => {
  const p = makeProject()
  const metadata = apiWorkPackage.create('area:widgets', [
    'api:change:chg-' + 'a'.repeat(24),
    'api:mismatch:mismatch-' + 'b'.repeat(24),
  ])
  const source = {
    kind: 'api',
    type: 'api-work-package',
    ref: 'api:package:' + metadata.packageId,
    fingerprint: 'sha256:' + 'c'.repeat(64),
  }
  const payload = {
    ...basePayload('api-work-package'),
    title: 'API work package',
    body: apiWorkPackage.render(metadata) + '\n\n## Goal\n\nDeliver the package.\n',
    source,
  }
  const result = run(p, payload)
  assert.equal(result.status, 0, result.stderr)
  const markdown = readFileSync(
    join(p.tasks, 'backlog', result.json.stem + '.md'), 'utf8',
  )
  assert.deepEqual(taskSource.parse(markdown).source, source)
  assert.deepEqual(apiWorkPackage.parse(markdown).value, metadata)
  assert.equal(readIndex(p).backlog[0].origin.type, 'api-work-package')
})

await check('recover-all readiness blocks pre-EOF work, accepts empty EOF, and rejects control bytes', async () => {
  const delayed = makeProject()
  const child = trackedSpawn('python3', [SCRIPT, '--recover-all'], {
    cwd: delayed.root,
    env: envFor(delayed, { CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '' }),
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
  assert.equal(existsSync(join(delayed.cache, 'creations')), false, 'recovery mutated marker state before readiness')
  assert.equal(existsSync(join(delayed.cache, 'finalizations')), false, 'recovery scanned or mutated writer state before readiness')
  child.stdin.end()
  assert.equal(await closed, 1)
  assert.equal(parseOutput(stdout).error.code, 'CREATE_WRITER_LEASE_MISSING', stderr)

  const ready = makeProject()
  const allowed = recoverAll(ready)
  assert.equal(allowed.status, 0)
  assert.equal(allowed.json.ok, true)
  assert.equal(allowed.json.mode, 'recover-all')

  const malformed = makeProject()
  const before = readFileSync(join(malformed.tasks, 'INDEX.json'))
  const rejected = spawnSync('python3', [SCRIPT, '--recover-all'], {
    cwd: malformed.root,
    env: envFor(malformed),
    input: 'not-an-empty-readiness-message',
    encoding: 'utf8',
    maxBuffer: 2 * 1024 * 1024,
  })
  assert.equal(rejected.status, 2)
  assert.equal(parseOutput(rejected.stdout).error.code, 'RECOVERY_READINESS_INVALID')
  assert.deepEqual(readFileSync(join(malformed.tasks, 'INDEX.json')), before)
  assert.equal(existsSync(join(malformed.cache, 'creations')), false)
  assert.equal(existsSync(join(malformed.cache, 'finalizations')), false)
})

await check('global allocation scans files, pending sidecars, INDEX, and permanent sentinels', () => {
  const p = makeProject()
  writeFileSync(join(p.tasks, 'backlog', 'TASK_2_backlog.md'), `# TASK 2 — backlog\n\n${sourceBlock('fixture-task-2')}\n`)
  writeFileSync(join(p.tasks, 'backlog', 'TASK_7_pending.md'), `# TASK 7 — pending\n\n${sourceBlock('fixture-task-7')}\n`)
  writeFileSync(join(p.tasks, 'pending', 'TASK_7_pending.questions.md'), pendingDoc('TASK_7_pending'))
  writeFileSync(join(p.tasks, 'todo', 'TASK_11_todo.md'), todoDoc(11))
  writeFileSync(join(p.tasks, 'done', 'TASK_13_done.md'), doneDoc(13))
  regenIndex(p)
  mkdirSync(join(p.cache, '.taskno'), { recursive: true })
  writeFileSync(join(p.cache, '.taskno', '25.lock'), '') // malformed reservation remains fail-closed

  const result = run(p, basePayload('global-number'))
  assert.equal(result.status, 0)
  assert.equal(result.json.number, 26)
  assert.equal(existsSync(join(p.cache, '.taskno', '26.lock')), true)
  assert.equal(readIndex(p).backlog.some(x => x.stem === result.json.stem), true)
  const events = taskStateObservations(result.stderr)
  assert.equal(events.length, 24,
    'eight lifecycle validations plus two six-scan publishes and two two-scan checks must all be observed')
  assert.equal(events.filter(event => event.action === 'index-publish').length, 12)
  assert.equal(events.filter(event => event.action === 'index-check').length, 4)
  assert.equal(events.filter(event => !String(event.action || '').startsWith('index-')).length, 8)
  assert.equal(events.filter(event => event.action === 'create').length, 8)
  for (const event of events) {
    assert.equal(event.event, 'task-state-validation')
    assert.equal(event.caller, 'server')
    assert.ok(['valid', 'invalid'].includes(event.result))
    assert.equal(event.findings.every(item => Object.keys(item).sort().join(',') === 'code,severity'), true)
  }
  assert.doesNotMatch(JSON.stringify(events), /Create profile global-number|Create the profile|test\.create\.key|task-lock-/)
})

await check('parallel distinct requests serialize across processes and publish a fresh combined INDEX', async () => {
  const p = makeProject()
  const [a, b] = await Promise.all([
    runAsync(p, basePayload('parallel-a')),
    runAsync(p, basePayload('parallel-b')),
  ])
  assert.equal(a.status, 0)
  assert.equal(b.status, 0)
  assert.notEqual(a.json.stem, b.json.stem)
  assert.deepEqual(new Set([a.json.number, b.json.number]), new Set([1, 2]))
  assert.deepEqual(new Set(readIndex(p).backlog.map(x => x.stem)), new Set([a.json.stem, b.json.stem]))
})

await check('parallel identical requests converge on one receipt and one task', async () => {
  const p = makeProject()
  const payload = basePayload('parallel-same')
  const [a, b] = await Promise.all([runAsync(p, payload), runAsync(p, payload)])
  assert.equal(a.status, 0)
  assert.equal(b.status, 0)
  assert.equal(a.json.stem, b.json.stem)
  assert.equal([a.json.created, b.json.created].filter(Boolean).length, 1)
  assert.equal([a.json.replayed, b.json.replayed].filter(Boolean).length, 1)
  assert.deepEqual(taskFiles(p), [a.json.stem + '.md'])
})

await check('RU/UA transliteration, NFC/NFD stability, and emoji fallback stay canonical ASCII', () => {
  const p = makeProject()
  const titles = [
    ['Добавить профиль', 'dobavit_profil'],
    ['Змінити екран', 'zminiti_ekran'],
    ['Йога їжак ґанок ёлка єнот', 'yoga_yizhak_ganok_yolka_yenot'],
    ['Café', 'cafe'],
    ['Cafe\u0301', 'cafe'],
    ['🚀', 'u1f680'],
  ]
  titles.forEach(([title, expected], i) => {
    const out = run(p, { ...basePayload(`unicode-${i}`), title, body: '' })
    assert.equal(out.status, 0)
    assert.equal(out.json.stem.replace(/^TASK_\d+_/, ''), expected)
    assert.match(out.json.stem, /^TASK_\d+_[A-Za-z0-9_]+$/)
    assert.ok(out.json.stem.length <= 120)
    assert.ok(!out.json.stem.endsWith('_untitled'))
  })
})

await check('request validation enforces UTF-8 byte/character limits and field contracts', async () => {
  const p = makeProject()
  const cases = [
    [{ ...basePayload('empty'), title: '   ' }, 'TITLE_EMPTY'],
    [{ ...basePayload('newline'), title: 'bad\ntitle' }, 'INVALID_REQUEST'],
    [{ ...basePayload('unicode-newline'), title: 'bad\u2028title' }, 'INVALID_REQUEST'],
    [{ ...basePayload('bidi-title'), title: 'bad\u202etitle' }, 'INVALID_REQUEST'],
    [{ ...basePayload('title-chars'), title: 'a'.repeat(201) }, 'TITLE_TOO_LARGE'],
    [{ ...basePayload('title-bytes'), title: '🚀'.repeat(130) }, 'TITLE_TOO_LARGE'],
    [{ ...basePayload('body-chars'), body: 'a'.repeat(65537) }, 'BODY_TOO_LARGE'],
    [{ ...basePayload('body-bytes'), body: 'я'.repeat(33000) }, 'BODY_TOO_LARGE'],
    [{ ...basePayload('bad-key'), key: '../bad' }, 'KEY_INVALID'],
    [{ ...basePayload('short-key'), key: 'short.key' }, 'KEY_INVALID'],
    [{ ...basePayload('origin'), originStem: 'TASK_1_плохой' }, 'ORIGIN_INVALID'],
    [{ ...basePayload('leading-zero-origin'), originStem: 'TASK_01_parent' }, 'ORIGIN_INVALID'],
    [{ ...basePayload('unsafe-origin-number'), originStem: 'TASK_9007199254740992_unsafe' }, 'ORIGIN_INVALID'],
    [(() => {
      const value = basePayload('leading-zero-source')
      value.source = { ...value.source, kind: 'follow-up', type: 'outcome-follow-up', ref: 'TASK_01_parent' }
      return value
    })(), 'SOURCE_INVALID'],
    [{ ...basePayload('report'), dedupReport: 'sha256:bad' }, 'DEDUP_REPORT_INVALID'],
    [{ ...basePayload('unknown'), surprise: true }, 'INVALID_REQUEST'],
    [(() => { const value = basePayload('missing-source'); delete value.source; return value })(), 'SOURCE_INVALID'],
    [(() => { const value = basePayload('missing-version'); delete value.version; return value })(), 'INVALID_REQUEST'],
  ]
  for (const [payload, code] of cases) {
    const result = await runAsync(p, payload)
    assert.equal(result.status, 2, code)
    assert.equal(result.json.error.code, code)
  }
  const malformed = runRaw(p, '{bad json')
  assert.equal(malformed.status, 2)
  assert.equal(malformed.json.error.code, 'INVALID_JSON')
  const floatVersion = runRaw(p, '{"version":1.0,"title":"Float","body":"","key":"float.version.key"}')
  assert.equal(floatVersion.status, 2)
  assert.equal(floatVersion.json.error.code, 'INVALID_REQUEST')
  assert.deepEqual(taskFiles(p), [])
})

await check('invalid rendered backlog proposal is rejected before marker, number, or task publication', () => {
  const p = makeProject()
  const payload = {
    ...basePayload('invalid-proposal'),
    body: '## Goal\n\nCapture an idea.\n\n## Outcome\n\nThis trailer is illegal in backlog.\n',
  }
  const indexBefore = readFileSync(join(p.tasks, 'INDEX.json'))
  const rejected = run(p, payload)
  assert.equal(rejected.status, 1, rejected.stderr + JSON.stringify(rejected.json))
  assert.equal(rejected.json.error.code, 'TASK_STATE_CREATE_PROPOSAL_INVALID')
  assert.ok(rejected.json.taskState.findingCodes.includes('OUTCOME_FORBIDDEN_IN_LIVE_STATE'))
  assert.equal(existsSync(markerFileFor(p, payload.key)), false,
    'invalid candidate must not publish an idempotency/recovery marker')
  assert.deepEqual(taskFiles(p), [])
  assert.deepEqual(readFileSync(join(p.tasks, 'INDEX.json')), indexBefore)
  const reservations = existsSync(join(p.cache, '.taskno'))
    ? readdirSync(join(p.cache, '.taskno')).filter(name => name.endsWith('.lock')) : []
  assert.deepEqual(reservations, [], 'invalid candidate must not reserve a permanent task number')

  const corrected = run(p, { ...payload, body: '## Goal\n\nCapture an idea safely.\n' })
  assert.equal(corrected.status, 0, corrected.stderr + JSON.stringify(corrected.json))
  assert.equal(corrected.json.number, 1,
    'the same idempotency key must remain usable after a pre-publication rejection')
})

await check('the exact maximum body publishes a valid storage-sized task above the AI context budget', async () => {
  const p = makeProject()
  const payload = { ...basePayload('storage-boundary'), body: 'x'.repeat(64 * 1024) }
  // Use the same streaming transport as the production controller. On macOS,
  // Node spawnSync(input) can retain EOF for a pipe-sized accepted payload even
  // though the CLI itself is draining stdin correctly.
  const result = await runAsync(p, payload)
  assert.equal(result.status, 0)
  const bytes = readFileSync(join(p.tasks, 'backlog', result.json.stem + '.md'))
  assert.ok(bytes.length > 64 * 1024)
  assert.ok(bytes.length <= 64 * 1024 + 4096)
  assert.equal(result.json.sourceHash, 'sha256:' + createHash('sha256').update(bytes).digest('hex'))
})

await check('origin is rendered once and verified through canonical INDEX splitFrom', () => {
  const p = makeProject()
  const base = basePayload('origin-positive')
  const intentId = 'intent-' + createHash('sha256').update(base.key, 'ascii').digest('hex')
  const payload = { ...base, originStem: 'TASK_99_parent_task', source: taskSource.followUp('TASK_99_parent_task', 'task-split', intentId) }
  const result = run(p, payload)
  assert.equal(result.status, 0)
  const text = readFileSync(join(p.tasks, 'backlog', result.json.stem + '.md'), 'utf8')
  assert.match(text, /\n## Origin\n- split from TASK_99_parent_task\n$/)
  assert.equal(readIndex(p).backlog[0].splitFrom, 'TASK_99_parent_task')
})

await check('every durable failpoint resumes the same transaction without duplicating a task or staging debris', () => {
  for (const point of [
    'after-marker', 'after-number', 'mid-task-write', 'after-task-candidate',
    'after-task-link', 'after-task-proof', 'after-task-cleanup',
    'after-file', 'after-index', 'after-complete',
  ]) {
    const p = makeProject()
    const payload = basePayload('crash-' + point)
    crash(p, payload, point)
    const before = markerFor(p, payload.key)
    const recovered = run(p, payload)
    assert.equal(recovered.status, 0, point)
    assert.equal(recovered.json.transactionId, before.transactionId, point)
    if (before.stem) assert.equal(recovered.json.stem, before.stem, point)
    assert.deepEqual(taskFiles(p), [recovered.json.stem + '.md'], point)
    assert.deepEqual(readIndex(p).backlog.map(x => x.stem), [recovered.json.stem], point)
    assert.equal(markerFor(p, payload.key).status, 'completed', point)
    assert.deepEqual(readdirSync(join(p.cache, 'creations')).filter(name => name.startsWith('.create-')), [],
      `${point}: exact recovery must remove every owned partial/candidate`)
  }
})

await check('creation staging cleanup requires exact transaction content ownership', () => {
  const p = makeProject()
  const payload = basePayload('foreign-stage-content')
  crash(p, payload, 'mid-task-write')
  const marker = markerFor(p, payload.key)
  const stage = join(p.cache, 'creations', `.create-${marker.transactionId}.partial`)
  const foreign = Buffer.from('not a prefix of the durable task intent\n')
  writeFileSync(stage, foreign)

  const refused = run(p, payload)
  assert.equal(refused.status, 1)
  assert.equal(refused.json.error.code, 'CREATION_STAGING_CONTENT_INVALID')
  assert.deepEqual(readFileSync(stage), foreign,
    'mismatched staging bytes must be diagnosed and preserved, never deleted as owned')
  assert.deepEqual(taskFiles(p), [])

  unlinkSync(stage)
  const recovered = run(p, payload)
  assert.equal(recovered.status, 0, recovered.stderr + JSON.stringify(recovered.json))
  assert.deepEqual(readdirSync(join(p.cache, 'creations')).filter(name => name.startsWith('.create-')), [])
})

await check('orphaned creation staging is diagnosed and preserved before recovery mutation', () => {
  const p = makeProject()
  const dir = join(p.cache, 'creations')
  mkdirSync(dir, { recursive: true })
  const stage = join(dir, `.create-${'f'.repeat(32)}.candidate`)
  const foreign = Buffer.from('# TASK 1 — Unowned\n')
  writeFileSync(stage, foreign)
  const indexBefore = readFileSync(join(p.tasks, 'INDEX.json'))

  const refused = recoverAll(p)
  assert.equal(refused.status, 1)
  assert.equal(refused.json.error.code, 'CREATION_STAGING_ORPHANED')
  assert.deepEqual(readFileSync(stage), foreign)
  assert.deepEqual(readFileSync(join(p.tasks, 'INDEX.json')), indexBefore)
  assert.deepEqual(taskFiles(p), [])
})

await check('byte-identical foreign targets are never adopted without exact inode lineage', () => {
  const preexisting = makeProject()
  const preexistingPayload = basePayload('same-bytes-preexisting')
  crash(preexisting, preexistingPayload, 'after-number')
  const preexistingMarker = markerFor(preexisting, preexistingPayload.key)
  const preexistingTarget = join(preexisting.tasks, 'backlog', preexistingMarker.stem + '.md')
  const expected = renderedTask(preexistingPayload, preexistingMarker)
  writeFileSync(preexistingTarget, expected)

  const refusedPreexisting = run(preexisting, preexistingPayload)
  assert.equal(refusedPreexisting.status, 1)
  assert.equal(refusedPreexisting.json.error.code, 'TASK_TARGET_OWNERSHIP_UNPROVEN')
  assert.deepEqual(readFileSync(preexistingTarget), expected,
    'a byte-identical preexisting target must be preserved, not adopted or overwritten')
  assert.equal(markerFor(preexisting, preexistingPayload.key).effect, null)

  unlinkSync(preexistingTarget)
  const recoveredPreexisting = run(preexisting, preexistingPayload)
  assert.equal(recoveredPreexisting.status, 0,
    recoveredPreexisting.stderr + JSON.stringify(recoveredPreexisting.json))

  const raced = makeProject()
  const racedPayload = basePayload('same-bytes-race')
  crash(raced, racedPayload, 'after-task-candidate')
  const racedMarker = markerFor(raced, racedPayload.key)
  const candidate = join(raced.cache, 'creations', `.create-${racedMarker.transactionId}.candidate`)
  const racedTarget = join(raced.tasks, 'backlog', racedMarker.stem + '.md')
  writeFileSync(racedTarget, readFileSync(candidate))

  const refusedRace = run(raced, racedPayload)
  assert.equal(refusedRace.status, 1)
  assert.equal(refusedRace.json.error.code, 'TASK_TARGET_OWNERSHIP_UNPROVEN')
  assert.ok(existsSync(candidate), 'the retained owned candidate must survive a foreign target race')
  assert.deepEqual(readFileSync(racedTarget), readFileSync(candidate))
  assert.equal(markerFor(raced, racedPayload.key).targetProof, null)

  unlinkSync(racedTarget)
  const recoveredRace = run(raced, racedPayload)
  assert.equal(recoveredRace.status, 0, recoveredRace.stderr + JSON.stringify(recoveredRace.json))
  assert.equal(markerFor(raced, racedPayload.key).targetProof.hash,
    markerFor(raced, racedPayload.key).sourceHash)
  assert.equal(existsSync(candidate), false)
})

await check('durable target proof rejects a byte-identical post-publication generation swap', () => {
  const p = makeProject()
  const payload = basePayload('same-bytes-post-publication')
  crash(p, payload, 'after-file')
  const marker = markerFor(p, payload.key)
  const target = join(p.tasks, 'backlog', marker.stem + '.md')
  const bytes = readFileSync(target)
  const originalProof = marker.targetProof
  linkSync(target, join(p.root, 'held-original-target'))
  unlinkSync(target)
  writeFileSync(target, bytes, { mode: 0o600 })
  assert.notEqual(exactFileProof(target).ino, originalProof.ino,
    'fixture must replace the target with another inode generation')

  const refused = run(p, payload)
  assert.equal(refused.status, 1)
  assert.equal(refused.json.error.code, 'TASK_TARGET_GENERATION_CHANGED')
  assert.deepEqual(readFileSync(target), bytes)
  assert.deepEqual(markerFor(p, payload.key).targetProof, originalProof,
    'failed recovery must retain the original durable ownership proof')
})

await check('a crashed reservation blocks foreign creation until exact recovery and keeps numbering unique', () => {
  const p = makeProject()
  const held = basePayload('held-number')
  crash(p, held, 'after-number')
  const heldMarker = markerFor(p, held.key)
  assert.equal(heldMarker.number, 1)
  const later = basePayload('later-number')
  const refused = run(p, later)
  assert.equal(refused.status, 1)
  assert.equal(refused.json.error.code, 'CREATION_RECOVERY_REQUIRED')
  assert.equal(existsSync(markerFileFor(p, later.key)), false,
    'refused foreign work must not publish another recovery marker')
  const recovered = run(p, held)
  assert.equal(recovered.status, 0)
  assert.equal(recovered.json.number, 1)
  const other = run(p, later)
  assert.equal(other.status, 0)
  assert.equal(other.json.number, 2)
  assert.deepEqual(new Set(readIndex(p).backlog.map(x => x.stem)), new Set([other.json.stem, recovered.json.stem]))
})

await check('malformed INDEX fails closed before numbering and the same key resumes after repair', () => {
  const p = makeProject()
  writeFileSync(join(p.tasks, 'INDEX.json'), '{malformed\n')
  const payload = basePayload('malformed-index')
  const failed = run(p, payload)
  assert.equal(failed.status, 1)
  assert.equal(failed.json.error.code, 'TASK_STATE_CREATE_PRECONDITION_FAILED')
  assert.deepEqual(failed.json.taskState.findingCodes, ['INDEX_JSON_INVALID'])
  assert.equal(failed.json.recoverable, true)
  assert.deepEqual(taskFiles(p), [])
  writeFileSync(join(p.tasks, 'INDEX.json'), JSON.stringify(emptyIndex(), null, 2) + '\n')
  const recovered = run(p, payload)
  assert.equal(recovered.status, 0)
  assert.equal(recovered.json.transactionId, markerFor(p, payload.key).transactionId)
})

await check('index execution failure retains file+marker and canonical retry recovers the same stem', () => {
  const p = makeProject()
  const broken = join(p.root, 'broken-regen.py')
  writeFileSync(broken, 'raise RuntimeError("fixture index failure")\n')
  const payload = basePayload('index-failure')
  const failed = run(p, payload, { CREATE_BACKLOG_REGEN_INDEX: broken })
  assert.equal(failed.status, 1)
  assert.equal(failed.json.error.code, 'INDEX_REGEN_FAILED')
  assert.equal(failed.json.recoverable, true)
  const saved = markerFor(p, payload.key)
  assert.equal(saved.phase, 'regenerating-index')
  assert.equal(taskFiles(p).length, 1)
  assert.deepEqual(readIndex(p).backlog, [])
  // Startup recovery has no original idempotency key/body.  It reconstructs
  // the canonical intent from the incomplete marker under the same mutex.
  const recovery = recoverAll(p)
  assert.equal(recovery.status, 0, recovery.stderr + JSON.stringify(recovery.json))
  assert.equal(recovery.json.mode, 'recover-all')
  assert.equal(recovery.json.recoveredCount, 1)
  const recovered = run(p, payload)
  assert.equal(recovered.status, 0)
  assert.equal(recovered.json.stem, saved.stem)
  assert.deepEqual(readIndex(p).backlog.map(x => x.stem), [saved.stem])
})

await check('recover-all validates a canonical multi-marker crash fixture before resuming every intent', () => {
  const p = makeProject()
  const a = basePayload('startup-a')
  const b = basePayload('startup-b')
  crash(p, a, 'after-number')
  // Normal admission intentionally refuses a second transaction while `a`
  // owns recovery.  Seed the legitimate multi-crash state from an isolated
  // canonical transaction whose number 1 is already occupied, as could be
  // retained across a controller restart.
  const fixture = makeProject()
  writeFileSync(join(fixture.tasks, 'backlog', 'TASK_1_fixture_sentinel.md'), `# TASK 1 — Fixture sentinel\n\n${sourceBlock('fixture-task-1')}\n`)
  regenIndex(fixture)
  crash(fixture, b, 'after-task-candidate')
  const bMarker = markerFor(fixture, b.key)
  assert.equal(bMarker.number, 2)
  mkdirSync(join(p.cache, 'creations'), { recursive: true })
  const fixtureCandidate = join(fixture.cache, 'creations',
    `.create-${bMarker.transactionId}.candidate`)
  const copiedCandidate = join(p.cache, 'creations', `.create-${bMarker.transactionId}.candidate`)
  writeFileSync(copiedCandidate, readFileSync(fixtureCandidate), { mode: 0o600 })
  writeFileSync(markerFileFor(p, b.key), markerText(bMarker))
  assert.equal(taskFiles(p).length, 0)
  const recovery = recoverAll(p)
  assert.equal(recovery.status, 0, recovery.stderr + JSON.stringify(recovery.json))
  assert.equal(recovery.json.scanned, 2)
  assert.equal(recovery.json.recoveredCount, 2)
  assert.equal(recovery.json.alreadyCompleted, 0)
  assert.deepEqual(new Set(recovery.json.recovered.map(x => x.stem)), new Set([markerFor(p, a.key).stem, markerFor(p, b.key).stem]))
  assert.equal(taskFiles(p).length, 2)
  assert.equal(readIndex(p).backlog.length, 2)
  assert.equal(markerFor(p, a.key).intent, null)
  assert.equal(markerFor(p, b.key).intent, null)
  assert.equal(existsSync(copiedCandidate), false)
})

await check('recover-all fails closed on a corrupt marker before touching valid incomplete work', () => {
  const p = makeProject()
  const payload = basePayload('recovery-corrupt')
  crash(p, payload, 'after-number')
  const corruptDir = join(p.cache, 'creations')
  writeFileSync(join(corruptDir, '0'.repeat(64) + '.json'), '{broken\n')
  const recovery = recoverAll(p)
  assert.equal(recovery.status, 1)
  assert.equal(recovery.json.error.code, 'RECOVERY_SCAN_FAILED')
  assert.deepEqual(taskFiles(p), [])
  assert.equal(markerFor(p, payload.key).status, 'incomplete')
})

await check('recovery rejects crafted non-canonical Unicode intent exactly like the site guard', () => {
  for (const [suffix, title] of [['line-separator', 'bad\u2028title'], ['surrogate', 'bad\uD800title']]) {
    const p = makeProject()
    const payload = basePayload('unicode-marker-' + suffix)
    crash(p, payload, 'after-marker')
    const marker = markerFor(p, payload.key)
    marker.intent.title = title
    marker.payloadHash = 'sha256:' + createHash('sha256').update(Buffer.from(canonical(marker.intent), 'utf8')).digest('hex')
    const digest = createHash('sha256').update(payload.key, 'ascii').digest('hex')
    writeFileSync(join(p.cache, 'creations', digest + '.json'), JSON.stringify(marker) + '\n')
    const recovery = recoverAll(p)
    assert.equal(recovery.status, 1)
    assert.equal(recovery.json.error.code, 'RECOVERY_SCAN_FAILED')
    assert.deepEqual(taskFiles(p), [])
  }
})

await check('Python recovery and the JS guard reject identical non-canonical creation timestamps', () => {
  for (const [suffix, timestamp] of [
    ['impossible-date', '2026-02-31T00:00:00.000000Z'],
    ['naive', '2026-01-01T00:00:00'],
    ['offset', '2026-01-01T00:00:00+00:00'],
    ['reversed', '2000-01-01T00:00:00.000000Z']
  ]) {
    const p = makeProject()
    const payload = basePayload('timestamp-' + suffix)
    crash(p, payload, 'after-marker')
    const marker = markerFor(p, payload.key)
    if (suffix === 'reversed') marker.updatedAt = timestamp
    else marker.createdAt = marker.updatedAt = timestamp
    const digest = createHash('sha256').update(payload.key, 'ascii').digest('hex')
    const file = join(p.cache, 'creations', digest + '.json')
    writeFileSync(file, JSON.stringify(marker) + '\n')
    assert.throws(() => markerContract.validate(marker, digest + '.json'), /timestamp/)
    const recovery = recoverAll(p)
    assert.equal(recovery.status, 1)
    assert.equal(recovery.json.error.code, 'RECOVERY_SCAN_FAILED')
    assert.deepEqual(taskFiles(p), [])
  }
})

await check('domain dedup binds a new idempotency receipt to the existing active task', () => {
  const p = makeProject()
  const stem = 'TASK_1_visual_fix'
  const body = '# TASK 1 — Existing\n\n' + sourceBlock('fixture-visual-fix') +
    '\n\n<!-- figma-visual-fix key=visual.fix.1 report=sha256:' + 'a'.repeat(64) + ' -->\n'
  writeFileSync(join(p.tasks, 'backlog', stem + '.md'), body)
  const idx = emptyIndex()
  // Deliberately stale: domain dedup must canonically rebuild/verify INDEX
  // before completing the durable receipt and returning success.
  idx.backlog = []
  writeFileSync(join(p.tasks, 'INDEX.json'), JSON.stringify(idx, null, 2) + '\n')
  const payload = {
    ...basePayload('domain-dedup'),
    dedupKey: 'visual.fix.1',
    dedupReport: 'sha256:' + 'a'.repeat(64),
  }
  const result = run(p, payload)
  assert.equal(result.status, 0)
  assert.equal(result.json.effect, 'domain-dedup')
  assert.equal(result.json.stem, stem)
  assert.equal(result.json.created, false)
  assert.deepEqual(taskFiles(p), [stem + '.md'])
  assert.deepEqual(readIndex(p).backlog.map(x => x.stem), [stem])
})

await check('task and index symlinks plus special task artifacts fail closed', () => {
  if (process.platform === 'win32') return

  const p1 = makeProject()
  const payload1 = basePayload('symlink-target')
  crash(p1, payload1, 'after-number')
  const m1 = markerFor(p1, payload1.key)
  const outside = join(p1.root, 'outside.md')
  writeFileSync(outside, 'foreign\n')
  symlinkSync(outside, join(p1.tasks, 'backlog', m1.stem + '.md'))
  const targetResult = run(p1, payload1)
  assert.equal(targetResult.status, 1)
  assert.equal(targetResult.json.error.code, 'UNSAFE_TASK_ARTIFACT')
  assert.equal(readFileSync(outside, 'utf8'), 'foreign\n')

  const p2 = makeProject()
  const realIndex = join(p2.root, 'foreign-index.json')
  writeFileSync(realIndex, JSON.stringify(emptyIndex()))
  unlinkSync(join(p2.tasks, 'INDEX.json'))
  symlinkSync(realIndex, join(p2.tasks, 'INDEX.json'))
  const indexResult = run(p2, basePayload('symlink-index'))
  assert.equal(indexResult.status, 1)
  assert.equal(indexResult.json.error.code, 'TASK_STATE_CREATE_PRECONDITION_FAILED')

  const p3 = makeProject()
  mkdirSync(join(p3.tasks, 'backlog', 'TASK_1_special.md'))
  const specialResult = run(p3, basePayload('special-file'))
  assert.equal(specialResult.status, 1)
  assert.equal(specialResult.json.error.code, 'TASK_STATE_CREATE_PRECONDITION_FAILED')
})

await check('a symlinked cache ancestor is rejected before any redirected write', () => {
  if (process.platform === 'win32') return
  const p = makeProject()
  const outside = join(p.root, 'outside-cache')
  mkdirSync(outside)
  symlinkSync(outside, join(p.root, 'orchestrator', '.cache'))
  const result = run(p, basePayload('ancestor-symlink'))
  assert.equal(result.status, 1)
  assert.equal(result.json.error.code, 'UNSAFE_DIRECTORY')
  assert.match(result.json.error.message, /component must be real/)
  assert.deepEqual(readdirSync(outside), [], 'cache preparation must not create through the symlink target')
})

await check('no-clobber recovery preserves a foreign target byte-for-byte', () => {
  const p = makeProject()
  const payload = basePayload('no-clobber')
  crash(p, payload, 'after-number')
  const marker = markerFor(p, payload.key)
  const target = join(p.tasks, 'backlog', marker.stem + '.md')
  const foreign = Buffer.from('FOREIGN BYTES\n')
  writeFileSync(target, foreign)
  const result = run(p, payload)
  assert.equal(result.status, 1)
  assert.equal(result.json.error.code, 'TASK_TARGET_CONFLICT')
  assert.deepEqual(readFileSync(target), foreign)
  assert.deepEqual(readIndex(p).backlog, [])
})

await check('production creation requires one attached sole workspace writer lease', async () => {
  const missingProject = makeProject()
  const missing = run(missingProject, basePayload('missing-lease'), { CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '' })
  assert.equal(missing.status, 1)
  assert.equal(missing.json.error.code, 'CREATE_WRITER_LEASE_MISSING')
  assert.deepEqual(taskFiles(missingProject), [])

  const acceptedProject = makeProject()
  const accepted = await runAuthenticated(acceptedProject, basePayload('attached-lease'))
  assert.equal(accepted.status, 0, accepted.stderr)
  assert.equal(accepted.json.created, true)
  assert.equal(taskFiles(acceptedProject).length, 1)

  const blockedProject = makeProject()
  const blocked = await runAuthenticated(blockedProject, basePayload('foreign-writer'), { foreign: true })
  assert.equal(blocked.status, 1)
  assert.equal(blocked.json.error.code, 'WORKSPACE_WRITER_ACTIVE')
  assert.deepEqual(taskFiles(blockedProject), [])
})

await check('authorized task-prep may create a split child through its sole guarded parent lease', () => {
  const p = makeProject()
  const writers = join(p.cache, 'finalizations', '.writers')
  const parentStem = 'TASK_99_parent_task'
  const parentSessionId = writerLeases.createSessionId()
  const authority = writerLeases.acquire(writers, {
    rootDir: p.root,
    kind: 'task-session', stem: parentStem, key: 'direct:prep',
    sessionId: parentSessionId, ownerPid: process.pid, ttlMs: 60_000,
  })
  try {
    const result = run(p, basePayload('nested-split-child'), {
      CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '',
      CREATE_BACKLOG_FINALIZATIONS_DIR: join(p.cache, 'finalizations'),
      CREATE_BACKLOG_PARENT_STEM: parentStem,
      CREATE_BACKLOG_PARENT_WRITER_LEASE_ID: authority.leaseId,
      CREATE_BACKLOG_PARENT_WRITER_LEASE_TOKEN: authority.token,
      CREATE_BACKLOG_PARENT_WRITER_SESSION_ID: parentSessionId,
      CREATE_BACKLOG_NODE: process.execPath,
    })
    assert.equal(result.status, 0, JSON.stringify(result.json))
    assert.equal(result.json.created, true)
    assert.equal(readIndex(p).backlog.some((row) => row.stem === result.json.stem), true)
  } finally {
    writerLeases.release(authority)
  }

  const site = makeProject()
  const siteWriters = join(site.cache, 'finalizations', '.writers')
  const siteStem = 'TASK_100_site_parent'
  const siteSessionId = writerLeases.createSessionId()
  const siteAuthority = writerLeases.acquire(siteWriters, {
    rootDir: site.root,
    kind: 'task-session', stem: siteStem, key: `task:${siteStem}`,
    sessionId: siteSessionId, ownerPid: process.pid, pendingChild: true,
  })
  writerLeases.updateChildPid(siteAuthority, process.pid)
  try {
    const result = run(site, basePayload('nested-site-child'), {
      CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '',
      CREATE_BACKLOG_FINALIZATIONS_DIR: join(site.cache, 'finalizations'),
      CREATE_BACKLOG_PARENT_STEM: siteStem,
      ORCHESTRATOR_WRITER_SESSION_ID: siteSessionId,
      ORCHESTRATOR_WRITER_LEASE_ID: siteAuthority.leaseId,
      ORCHESTRATOR_WRITER_DELEGATION_TOKEN: siteAuthority.delegationToken,
      CREATE_BACKLOG_NODE: process.execPath,
    })
    assert.equal(result.status, 0, JSON.stringify(result.json))
    assert.equal(result.json.created, true)
    assert.equal(readIndex(site).backlog.some((row) => row.stem === result.json.stem), true)
  } finally {
    writerLeases.release(siteAuthority)
  }

  const siblingProject = makeProject()
  const siblingWriters = join(siblingProject.cache, 'finalizations', '.writers')
  const siblingStem = 'TASK_104_copied_site_parent'
  const copiedSessionId = writerLeases.createSessionId()
  const sibling = trackedSpawn(process.execPath, ['-e', 'setInterval(() => {}, 1000)'], { stdio: 'ignore' })
  const siblingAuthority = writerLeases.acquire(siblingWriters, {
    rootDir: siblingProject.root,
    kind: 'task-session', stem: siblingStem, key: `task:${siblingStem}`,
    sessionId: copiedSessionId, ownerPid: process.pid, pendingChild: true,
  })
  writerLeases.updateChildPid(siblingAuthority, sibling.pid)
  try {
    const noReceipt = run(siblingProject, basePayload('copied-site-no-receipt'), {
      CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '',
      CREATE_BACKLOG_FINALIZATIONS_DIR: join(siblingProject.cache, 'finalizations'),
      CREATE_BACKLOG_PARENT_STEM: siblingStem,
      ORCHESTRATOR_WRITER_SESSION_ID: copiedSessionId,
      CREATE_BACKLOG_NODE: process.execPath,
    })
    assert.equal(noReceipt.status, 1)
    assert.equal(noReceipt.json.error.code, 'CREATE_PARENT_WRITER_INVALID',
      'copied public stem/sessionId without the child receipt must fail before guard acquisition')

    const refused = run(siblingProject, basePayload('copied-site-sibling'), {
      CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '',
      CREATE_BACKLOG_FINALIZATIONS_DIR: join(siblingProject.cache, 'finalizations'),
      CREATE_BACKLOG_PARENT_STEM: siblingStem,
      ORCHESTRATOR_WRITER_SESSION_ID: copiedSessionId,
      ORCHESTRATOR_WRITER_LEASE_ID: siblingAuthority.leaseId,
      ORCHESTRATOR_WRITER_DELEGATION_TOKEN: siblingAuthority.delegationToken,
      CREATE_BACKLOG_NODE: process.execPath,
    })
    assert.equal(refused.status, 1)
    assert.equal(refused.json.error.code, 'CREATE_PUBLICATION_GUARD_REFUSED')
    assert.deepEqual(taskFiles(siblingProject), [],
      'an unrelated sibling with copied site metadata must not publish a task')
    assert.deepEqual(writerLeases.scan(siblingWriters).active.map((row) => row.leaseId),
      [siblingAuthority.leaseId], 'the refused sibling must withdraw only its attempted guard')
  } finally {
    sibling.kill('SIGKILL')
    writerLeases.release(siblingAuthority)
  }
})

await check('nested create global guard closes the post-scan foreign-writer race', async () => {
  const p = makeProject()
  const writers = join(p.cache, 'finalizations', '.writers')
  const parentStem = 'TASK_101_race_parent'
  const parentSessionId = writerLeases.createSessionId()
  const parent = writerLeases.acquire(writers, {
    rootDir: p.root,
    kind: 'task-session', stem: parentStem, key: 'standby:prep',
    sessionId: parentSessionId, ownerPid: process.pid, ttlMs: 60_000,
  })
  const signal = join(p.root, 'validator-entered.signal')
  const release = join(p.root, 'validator-release.signal')
  const blockingValidator = join(p.root, 'blocking-validator.mjs')
  writeFileSync(blockingValidator, [
    "import { existsSync, writeFileSync } from 'node:fs'",
    "import { spawnSync } from 'node:child_process'",
    "writeFileSync(process.env.TEST_VALIDATOR_SIGNAL, 'entered\\n')",
    "const deadline = Date.now() + 15000",
    "while (!existsSync(process.env.TEST_VALIDATOR_RELEASE)) {",
    "  if (Date.now() >= deadline) process.exit(98)",
    "  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 10)",
    "}",
    "const child = spawnSync(process.execPath, [process.env.REAL_TASK_STATE_VALIDATOR, ...process.argv.slice(2)], { env: process.env, stdio: 'inherit' })",
    "if (child.error) { console.error(child.error.message); process.exit(97) }",
    "process.exit(child.status === null ? 96 : child.status)",
    "",
  ].join('\n'), { mode: 0o600 })

  let createPromise
  let postGuardWriter = null
  try {
    createPromise = runAsync(p, basePayload('nested-race-child'), {
      CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '',
      CREATE_BACKLOG_FINALIZATIONS_DIR: join(p.cache, 'finalizations'),
      CREATE_BACKLOG_PARENT_STEM: parentStem,
      CREATE_BACKLOG_PARENT_WRITER_LEASE_ID: parent.leaseId,
      CREATE_BACKLOG_PARENT_WRITER_LEASE_TOKEN: parent.token,
      CREATE_BACKLOG_PARENT_WRITER_SESSION_ID: parentSessionId,
      CREATE_BACKLOG_NODE: process.execPath,
      CREATE_BACKLOG_TASK_VALIDATOR: blockingValidator,
      REAL_TASK_STATE_VALIDATOR: VALIDATOR,
      TEST_VALIDATOR_SIGNAL: signal,
      TEST_VALIDATOR_RELEASE: release,
    })
    await waitForPath(signal)

    const active = writerLeases.scan(writers).active
    const guard = active.find((row) => row.kind === 'lock-writer' && row.key === 'task:create-backlog')
    assert.ok(guard, 'nested create must retain a visible global publication guard while blocked after writer scan')
    assert.equal(guard.sessionId, parentSessionId)
    assert.ok(active.some((row) => row.leaseId === parent.leaseId))

    const writerEnv = {
      ...process.env,
      FINALIZE_PROJECT_ROOT: p.root,
      FINALIZE_STATE_DIR: join(p.cache, 'finalizations'),
      FINALIZE_CREATIONS_DIR: join(p.cache, 'creations'),
      FINALIZE_EDITS_DIR: join(p.cache, 'edits'),
    }
    // The probe uses kind standby-writer so the nested publication guard is
    // the ONLY possible conflict: a task-session probe would now also be
    // refused by the frozen-serial board-task exclusion against the parent
    // lease, muddying what this check proves about the guard itself.
    const foreignArgs = [
      WRITER_CLI, 'acquire', '--guard-finalization', '--kind', 'standby-writer',
      '--key', 'standby:foreign-racer',
      '--owner-pid', String(process.pid), '--ttl-ms', '60000',
    ]
    const refused = spawnSync(process.execPath, foreignArgs, { env: writerEnv, encoding: 'utf8' })
    assert.equal(refused.status, 2, refused.stderr + refused.stdout)
    assert.equal(refused.stdout, '')
    assert.match(refused.stderr, /WORKSPACE_WRITER_ACTIVE/)
    assert.deepEqual(new Set(writerLeases.scan(writers).active.map((row) => row.leaseId)),
      new Set([parent.leaseId, guard.leaseId]), 'foreign racer must withdraw only its own lease')

    writeFileSync(release, 'release\n')
    const created = await createPromise
    createPromise = null
    assert.equal(created.status, 0, created.stderr + JSON.stringify(created.json))
    assert.equal(created.json.created, true)
    assert.deepEqual(writerLeases.scan(writers).active.map((row) => row.leaseId), [parent.leaseId],
      'nested publication guard must be ownership-safely released after completion')

    const accepted = spawnSync(process.execPath, foreignArgs, { env: writerEnv, encoding: 'utf8' })
    assert.equal(accepted.status, 0, accepted.stderr + accepted.stdout)
    postGuardWriter = JSON.parse(accepted.stdout)
  } finally {
    if (!existsSync(release)) writeFileSync(release, 'cleanup\n')
    if (createPromise) {
      try { await createPromise } catch {}
    }
    if (postGuardWriter) {
      try {
        writerLeases.release({
          dir: writers, path: join(writers, postGuardWriter.leaseId + '.json'),
          leaseId: postGuardWriter.leaseId, token: postGuardWriter.token,
        })
      } catch {}
    }
    try { writerLeases.release(parent) } catch {}
  }
})

await check('nested guard crash blocks until exact stale-lease reconciliation, then recover-all completes', () => {
  const p = makeProject()
  const writers = join(p.cache, 'finalizations', '.writers')
  const parentStem = 'TASK_103_recovery_parent'
  const parentSessionId = writerLeases.createSessionId()
  const parent = writerLeases.acquire(writers, {
    rootDir: p.root,
    kind: 'task-session', stem: parentStem, key: 'direct:prep',
    sessionId: parentSessionId, ownerPid: process.pid, ttlMs: 60_000,
  })
  const payload = basePayload('nested-guard-recovery')
  const authorityEnv = {
    CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '',
    CREATE_BACKLOG_FINALIZATIONS_DIR: join(p.cache, 'finalizations'),
    CREATE_BACKLOG_PARENT_STEM: parentStem,
    CREATE_BACKLOG_PARENT_WRITER_LEASE_ID: parent.leaseId,
    CREATE_BACKLOG_PARENT_WRITER_LEASE_TOKEN: parent.token,
    CREATE_BACKLOG_PARENT_WRITER_SESSION_ID: parentSessionId,
    CREATE_BACKLOG_NODE: process.execPath,
  }
  try {
    const crashed = spawnSync('python3', [SCRIPT], {
      cwd: p.root,
      env: envFor(p, { ...authorityEnv, CREATE_BACKLOG_FAILPOINT: 'after-number' }),
      input: JSON.stringify(payload), encoding: 'utf8', maxBuffer: 2 * 1024 * 1024,
    })
    assert.equal(crashed.status, 86, crashed.stderr + crashed.stdout)
    assert.equal(parseOutput(crashed.stdout, { allowMissingResult: true }), null)
    const afterCrash = writerLeases.scan(writers)
    assert.deepEqual(afterCrash.active.map((row) => row.leaseId), [parent.leaseId])
    assert.ok(afterCrash.stale.some((row) => row.kind === 'lock-writer' && row.key === 'task:create-backlog'),
      'a crashed helper guard must become automatically stale when its exact owner PID is gone')

    const conflicting = run(p, { ...payload, body: payload.body + '\nconflict\n' }, authorityEnv)
    assert.equal(conflicting.status, 2)
    assert.equal(conflicting.json.error.code, 'IDEMPOTENCY_CONFLICT',
      'guard admission by key must still bind recovery to the marker payload before mutation')

    const staleGuard = writerLeases.scan(writers).stale.find(row =>
      row.kind === 'lock-writer' && row.key === 'task:create-backlog')
    assert.ok(staleGuard, 'the exact dead guard generation must remain available for reconciliation')

    const reconcileCrash = spawnSync('python3', [SCRIPT, '--recover-all'], {
      cwd: p.root,
      env: envFor(p, { ...authorityEnv, CREATE_BACKLOG_FAILPOINT: 'after-stale-guard-reconcile' }),
      input: '', encoding: 'utf8', maxBuffer: 2 * 1024 * 1024,
    })
    assert.equal(reconcileCrash.status, 86, reconcileCrash.stderr + reconcileCrash.stdout)
    assert.equal(parseOutput(reconcileCrash.stdout, { allowMissingResult: true }), null)
    const afterReconcileCrash = writerLeases.scan(writers)
    assert.equal(afterReconcileCrash.stale.some(row => row.leaseId === staleGuard.leaseId), false,
      'the exact old guard release must be durable before the crash hook')
    assert.ok(afterReconcileCrash.stale.some(row => row.key === 'task:recover-backlog-creations'),
      'the interrupted recovery guard itself must remain as the next exact recovery input')

    const recovered = recoverAll(p, authorityEnv)
    assert.equal(recovered.status, 0, recovered.stderr + JSON.stringify(recovered.json))
    assert.equal(recovered.json.recoveredCount, 1)
    assert.equal(markerFor(p, payload.key).status, 'completed')
    assert.deepEqual(writerLeases.scan(writers).active.map((row) => row.leaseId), [parent.leaseId])
  } finally {
    writerLeases.release(parent)
  }
})

await check('recover-all never reconciles a foreign-session stale guard or any live guard', async () => {
  const delegationHash = 'sha256:' + 'a'.repeat(64)

  const staleProject = makeProject()
  const stalePayload = basePayload('foreign-stale-guard')
  crash(staleProject, stalePayload, 'after-number')
  const staleWriters = join(staleProject.cache, 'finalizations', '.writers')
  const staleParentStem = 'TASK_104_stale_parent'
  const staleParentSession = writerLeases.createSessionId()
  const staleParent = writerLeases.acquire(staleWriters, {
    rootDir: staleProject.root, kind: 'task-session', stem: staleParentStem,
    key: 'direct:prep', sessionId: staleParentSession, ownerPid: process.pid, ttlMs: 60_000,
  })
  const sleeper = trackedSpawn(process.execPath, ['-e', 'setInterval(function(){}, 1000)'])
  const foreignStale = writerLeases.acquire(staleWriters, {
    rootDir: staleProject.root, kind: 'lock-writer', stem: null,
    key: 'task:create-backlog', sessionId: writerLeases.createSessionId(),
    ownerPid: sleeper.pid, delegationHash,
  })
  sleeper.kill('SIGKILL')
  await new Promise(resolve => sleeper.once('close', resolve))
  const staleEnv = {
    CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '',
    CREATE_BACKLOG_FINALIZATIONS_DIR: join(staleProject.cache, 'finalizations'),
    CREATE_BACKLOG_PARENT_STEM: staleParentStem,
    CREATE_BACKLOG_PARENT_WRITER_LEASE_ID: staleParent.leaseId,
    CREATE_BACKLOG_PARENT_WRITER_LEASE_TOKEN: staleParent.token,
    CREATE_BACKLOG_PARENT_WRITER_SESSION_ID: staleParentSession,
    CREATE_BACKLOG_NODE: process.execPath,
  }
  try {
    assert.ok(writerLeases.scan(staleWriters).stale.some(row => row.leaseId === foreignStale.leaseId))
    const refused = recoverAll(staleProject, staleEnv)
    assert.equal(refused.status, 1)
    assert.equal(refused.json.error.code, 'TASK_STATE_CREATE_PRECONDITION_FAILED')
    assert.ok(existsSync(foreignStale.path), 'foreign-session stale guard must survive automatic recovery')
    writerLeases.release(foreignStale)
    assert.equal(recoverAll(staleProject, staleEnv).status, 0)
  } finally {
    try { writerLeases.release(foreignStale) } catch {}
    try { writerLeases.release(staleParent) } catch {}
  }

  const liveProject = makeProject()
  const livePayload = basePayload('foreign-live-guard')
  crash(liveProject, livePayload, 'after-number')
  const liveWriters = join(liveProject.cache, 'finalizations', '.writers')
  const liveParentStem = 'TASK_105_live_parent'
  const liveParentSession = writerLeases.createSessionId()
  const liveParent = writerLeases.acquire(liveWriters, {
    rootDir: liveProject.root, kind: 'task-session', stem: liveParentStem,
    key: 'direct:prep', sessionId: liveParentSession, ownerPid: process.pid, ttlMs: 60_000,
  })
  const foreignLive = writerLeases.acquire(liveWriters, {
    rootDir: liveProject.root, kind: 'lock-writer', stem: null,
    key: 'task:create-backlog', sessionId: liveParentSession,
    ownerPid: process.pid, delegationHash,
  })
  const liveEnv = {
    CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '',
    CREATE_BACKLOG_FINALIZATIONS_DIR: join(liveProject.cache, 'finalizations'),
    CREATE_BACKLOG_PARENT_STEM: liveParentStem,
    CREATE_BACKLOG_PARENT_WRITER_LEASE_ID: liveParent.leaseId,
    CREATE_BACKLOG_PARENT_WRITER_LEASE_TOKEN: liveParent.token,
    CREATE_BACKLOG_PARENT_WRITER_SESSION_ID: liveParentSession,
    CREATE_BACKLOG_NODE: process.execPath,
  }
  try {
    const refused = recoverAll(liveProject, liveEnv)
    assert.equal(refused.status, 1)
    assert.equal(refused.json.error.code, 'CREATE_PUBLICATION_GUARD_REFUSED')
    assert.ok(existsSync(foreignLive.path), 'a live guard must never be reconciled')
    writerLeases.release(foreignLive)
    assert.equal(recoverAll(liveProject, liveEnv).status, 0)
  } finally {
    try { writerLeases.release(foreignLive) } catch {}
    try { writerLeases.release(liveParent) } catch {}
  }
})

await check('canonical workspace cannot enable the fixture-only unleased writer bypass', () => {
  const probe = spawnSync('python3', ['-c', [
    'import runpy,sys',
    'lib=runpy.run_path(sys.argv[1],run_name="fixture_probe")',
    'print(int(lib["_fixture_mode_enabled"]("CREATE_BACKLOG_TEST_ALLOW_UNLEASED")))'
  ].join(';'), SCRIPT], {
    cwd: dirname(TASKS_DIR),
    env: {
      ...process.env,
      CREATE_BACKLOG_PROJECT_ROOT: join(TASKS_DIR, '..', '..'),
      CREATE_BACKLOG_AUTHORITY_ROOT: join(TASKS_DIR, '..', '..'),
      CREATE_BACKLOG_TEST_ALLOW_UNLEASED: '1',
      PYTHONDONTWRITEBYTECODE: '1'
    }, encoding: 'utf8'
  })
  assert.equal(probe.status, 0, probe.stderr)
  assert.equal(probe.stdout.trim(), '0')
})

await check('marker CAS preserves a last-moment foreign generation byte-for-byte', () => {
  const p = makeProject()
  const payload = basePayload('marker-generation-race')
  crash(p, payload, 'after-marker')
  const marker = markerFor(p, payload.key)
  const foreign = { ...marker, transactionId: 'f'.repeat(32) }
  const foreignBytes = markerText(foreign)
  const raced = run(p, payload, {
    CREATE_BACKLOG_TEST_SWAP_MARKER_BEFORE_DETACH: '1',
    CREATE_BACKLOG_TEST_SWAP_MARKER_BEFORE_DETACH_BASE64: Buffer.from(foreignBytes).toString('base64')
  })
  assert.equal(raced.status, 1)
  assert.equal(raced.json.error.code, 'CAS_SOURCE_CHANGED')
  assert.equal(readFileSync(markerFileFor(p, payload.key), 'utf8'), foreignBytes)
  assert.doesNotThrow(() => markerContract.validate(
    foreign, markerFileFor(p, payload.key).split('/').pop()))
})

await check('duplicate transaction ids and impossible completed lattices fail in Python and JS', () => {
  const p = makeProject()
  const a = basePayload('duplicate-tx-a'), b = basePayload('duplicate-tx-b')
  assert.equal(run(p, a).status, 0)
  assert.equal(run(p, b).status, 0)
  const first = markerFor(p, a.key)
  const second = markerFor(p, b.key)
  second.transactionId = first.transactionId
  writeFileSync(markerFileFor(p, b.key), markerText(second))
  const entries = [a, b].map(payload => ({
    filename: markerFileFor(p, payload.key).split('/').pop(),
    value: markerFor(p, payload.key)
  }))
  assert.throws(() => markerContract.validateSet(entries), /transaction ids are not unique/)
  const blocked = recoverAll(p)
  assert.equal(blocked.status, 1)
  assert.equal(blocked.json.error.code, 'RECOVERY_SCAN_FAILED')

  const q = makeProject(), payload = basePayload('impossible-lattice')
  assert.equal(run(q, payload).status, 0)
  const impossible = markerFor(q, payload.key)
  impossible.column = 'pending'
  writeFileSync(markerFileFor(q, payload.key), markerText(impossible))
  assert.throws(() => markerContract.validate(
    impossible, markerFileFor(q, payload.key).split('/').pop()), /impossible result lattice/)
  assert.equal(recoverAll(q).json.error.code, 'RECOVERY_SCAN_FAILED')

  const r = makeProject(), versionPayload = basePayload('boolean-marker-version')
  assert.equal(run(r, versionPayload).status, 0)
  const invalidVersion = markerFor(r, versionPayload.key)
  invalidVersion.version = true
  writeFileSync(markerFileFor(r, versionPayload.key), markerText(invalidVersion))
  assert.throws(() => markerContract.validate(
    invalidVersion, markerFileFor(r, versionPayload.key).split('/').pop()), /current contract/)
  assert.equal(recoverAll(r).json.error.code, 'RECOVERY_SCAN_FAILED')
})

await check('malformed durable CAS artifacts fail closed before creation effects', () => {
  const p = makeProject()
  const casDir = join(p.cache, 'creations', '.durable-cas-not-canonical')
  mkdirSync(casDir, { recursive: true })
  const before = readFileSync(join(p.tasks, 'INDEX.json'))
  const result = recoverAll(p)
  assert.equal(result.status, 1)
  assert.equal(result.json.error.code, 'CAS_RECOVERY_SCAN_FAILED')
  assert.deepEqual(readFileSync(join(p.tasks, 'INDEX.json')), before)
  assert.deepEqual(taskFiles(p), [])
})

await check('creation recovery rejects an over-bound directory without using a partial scan', () => {
  const p = makeProject()
  const dir = join(p.cache, 'creations')
  mkdirSync(dir, { recursive: true })
  for (let i = 0; i <= 10_000; i++) writeFileSync(join(dir, `.bounded-${i}`), '')
  const before = readFileSync(join(p.tasks, 'INDEX.json'))
  const result = recoverAll(p)
  assert.equal(result.status, 1)
  assert.equal(result.json.error.code, 'CAS_RECOVERY_SCAN_FAILED')
  assert.match(result.json.error.message, /10000-entry limit/)
  assert.deepEqual(readFileSync(join(p.tasks, 'INDEX.json')), before)
})

reportCleanupFailures(cleanupTestRoots())

if (failed) {
  console.error(`create-backlog: ${failed} failed, ${passed} passed`)
  process.exit(1)
}
if (cleanupFailed) process.exit(1)
console.log(`create-backlog: ${passed} checks passed`)
