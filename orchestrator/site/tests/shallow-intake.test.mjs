#!/usr/bin/env node

import assert from 'node:assert/strict'
import { chmodSync, existsSync, linkSync, lstatSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, renameSync, rmSync, statSync, symlinkSync, writeFileSync } from 'node:fs'
import { hostname, tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { createRequire } from 'node:module'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))

const root = mkdtempSync(join(tmpdir(), 'shallow-intake-'))
const tasks = join(root, 'tasks')
const cache = join(root, 'cache')
const intakeDir = join(cache, 'intake')
const locksDir = join(cache, 'locks')
const requestsDir = join(cache, 'requests')
const finalizationsDir = join(cache, 'finalizations')
const creationsDir = join(cache, 'creations')
const editsDir = join(cache, 'edits')
const scratchAuthorityRoot = mkdtempSync(join(tmpdir(), 'shallow-intake-scratch-'))
const scratchDir = join(scratchAuthorityRoot, 'scratch')
for (const dir of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasks, dir), { recursive: true })
mkdirSync(intakeDir, { recursive: true })
chmodSync(intakeDir, 0o700)
mkdirSync(locksDir, { recursive: true })
mkdirSync(requestsDir, { recursive: true })
mkdirSync(finalizationsDir, { recursive: true })
mkdirSync(creationsDir, { recursive: true })
mkdirSync(editsDir, { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_TASKS_DIR = tasks
process.env.ORCHESTRATOR_TASK_INTAKE_DIR = intakeDir
process.env.ORCHESTRATOR_LOCKS_DIR = locksDir
process.env.ORCHESTRATOR_REQUESTS_DIR = requestsDir
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = finalizationsDir
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = creationsDir
process.env.ORCHESTRATOR_TASK_EDITS_DIR = editsDir
process.env.SHALLOW_INTAKE_SCRATCH_DIR = scratchDir
process.env.SHALLOW_INTAKE_SCRATCH_ROOT = scratchAuthorityRoot
process.env.SHALLOW_INTAKE_TIMEOUT_MS = '1000'
process.env.SHALLOW_INTAKE_CONCURRENCY = '1'
process.env.TMPDIR = intakeDir

const fake = join(root, 'fake-claude.mjs')
writeFileSync(fake, `#!/usr/bin/env node
import { readFileSync, writeFileSync, appendFileSync, statSync } from 'node:fs'
import { resolve } from 'node:path'
import { spawn } from 'node:child_process'
const prompt = readFileSync(0, 'utf8')
const intake = process.env.TMPDIR
appendFileSync(resolve(intake, 'fake-starts.log'), 'start\\n')
writeFileSync(resolve(intake, 'captured-args.json'), JSON.stringify(process.argv.slice(2)))
writeFileSync(resolve(intake, 'captured-prompt.txt'), prompt)
writeFileSync(resolve(intake, 'captured-cwd.txt'), process.cwd())
writeFileSync(resolve(intake, 'captured-cwd-mode.txt'), String(statSync('.').mode))
const marker = 'BEGIN_UNTRUSTED_CONTEXT_JSON_TO_EOF\\n'
const context = JSON.parse(prompt.slice(prompt.lastIndexOf(marker) + marker.length))
if (context.task.text.includes('SCHEMA_TRANSPORT_FAILURE')) {
  process.stderr.write('Error: --json-schema is not a valid JSON Schema: unsupported fixture dialect\\n')
  process.exitCode = 1
}
else if (context.task.text.includes('KEYCHAIN_SANDBOX_FAILURE')) {
  process.stderr.write("EPERM: operation not permitted, posix_spawn 'security'\\n")
  process.exitCode = 1
}
else if (context.task.text.includes('ABRUPT_WRAPPER')) {
  writeFileSync(resolve(intake, 'abrupt-model.pid'), String(process.pid) + '\\n')
  process.on('SIGTERM', () => {})
  setInterval(() => {}, 1000)
}
else if (context.task.text.includes('SLOW_MODEL')) { process.on('SIGTERM', () => {}); setTimeout(emit, 5000) }
else if (context.task.text.includes('ESCAPED_DESCENDANT')) {
  let emitted = false
  const finish = () => { if (!emitted) { emitted = true; emit() } }
  const denied = () => {
    appendFileSync(resolve(intake, 'fork-denied.log'), context.task.stem + '\\n')
    finish()
  }
  try {
    const descendant = spawn(process.execPath, ['-e', 'process.on("SIGTERM",()=>{});setInterval(()=>{},1000)'], {
      detached: true, stdio: 'ignore'
    })
    descendant.once('spawn', () => {
      appendFileSync(resolve(intake, 'escaped-pids.log'), String(descendant.pid) + '\\n')
      descendant.unref()
      finish()
    })
    descendant.once('error', denied)
  } catch {
    denied()
  }
}
else emit()
function emit() {
  if (context.task.text.includes('MALFORMED_MODEL')) { process.stdout.write('not json\\n'); return }
  const duplicate = context.task.text.includes('DUPLICATE_MODEL') && context.activeCandidates[0]
  const output = {
    readiness: duplicate ? 'possible-duplicate' : 'ready',
    summary: 'A bounded advisory summary of the requested backlog outcome.',
    likelyAreas: ['unknown'],
    possibleDuplicates: duplicate ? [{ stem: context.activeCandidates[0].stem, reason: 'Duplicate candidate is named in both tasks.', evidence: [
      { sourceStem: context.task.stem, quote: 'Duplicate candidate' },
      { sourceStem: context.activeCandidates[0].stem, quote: context.activeCandidates[0].title }
    ] }] : [],
    missingContext: [],
    riskFlags: []
  }
  process.stdout.write(JSON.stringify({ type: 'result', subtype: 'success', is_error: false, structured_output: output }) + '\\n')
}
`)
chmodSync(fake, 0o755)
process.env.SHALLOW_INTAKE_CLAUDE = fake

const require = createRequire(import.meta.url)
const intake = require('../server/shallow-intake.js')
const fileGuards = require('../server/file-guards.js')
const writerLeases = require('../../tasks/writer-leases.cjs')
const taskCore = require('../../tasks/task-state-core.cjs')
const taskSource = require('../../tasks/task-source-contract.cjs')
const SOURCE_BLOCK = taskSource.render(taskSource.manualForIntent('shallow-intake-fixture', 'manual', 'fixture:shallow-intake'))

let checks = 0
async function check(name, fn) {
  await fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}
async function waitFor(fn, timeout = 5000) {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    const value = fn()
    if (value) return value
    await new Promise((resolve) => setTimeout(resolve, 25))
  }
  assert.fail('timed out waiting for fixture state')
}
function taskText(n, title, body = '') {
  return [
    `# TASK ${n} — ${title}`, '', SOURCE_BLOCK, '',
    '## Goal', '', body || title, '',
    '## Inputs', '', '- Existing repository contracts.', '',
    '## Acceptance', '', '### Automated', '', '- Run `node orchestrator/site/tests/shallow-intake.test.mjs`.', '',
    '### Manual', '', '- Inspect the bounded result.', '',
    '## Out of scope', '', '- Unrelated behavior.', ''
  ].join('\n')
}
function writeCanonicalIndex(tasksDir, repoRoot, generatedAt = '2026-01-01T00:00:00Z') {
  const taskRuntime = join(repoRoot, 'orchestrator', '.cache', 'tasks')
  const snapshot = taskCore.validateTaskState({
    tasksDir, repoRoot, checkIndex: false, includeRuntime: false,
    taskCreationsDir: join(taskRuntime, 'creations'), taskCreationsAuthorityRoot: repoRoot,
    taskEditsDir: join(taskRuntime, 'edits'), taskEditsAuthorityRoot: repoRoot
  })
  assert.equal(snapshot.ok, true, JSON.stringify(snapshot.findings))
  writeFileSync(join(tasksDir, 'INDEX.json'), JSON.stringify(taskCore.deriveIndex(snapshot._model, generatedAt), null, 2) + '\n')
}
function addTask(stem, title, text) {
  writeFileSync(join(tasks, 'backlog', stem + '.md'), text)
  writeIndex()
}
function writeIndex() {
  writeCanonicalIndex(tasks, root)
}
function record(stem) { return JSON.parse(readFileSync(join(intakeDir, stem + '.json'), 'utf8')) }
function taskBytes(stem) { return readFileSync(join(tasks, 'backlog', stem + '.md')) }
function writePrivateJson(file, value) {
  writeFileSync(file, JSON.stringify(value) + '\n', { mode: 0o600 })
  chmodSync(file, 0o600)
}
function writePrivateBytes(file, value) {
  writeFileSync(file, value, { mode: 0o600 })
  chmodSync(file, 0o600)
}
function guardCrash(action, point, authorityRoot, directory, target) {
  const guardsPath = resolve(HERE, '../server/file-guards.js')
  const script = String.raw`
const cp = require('node:child_process');
const original = cp.spawnSync;
const guards = require(process.argv[1]);
const [action, point, root, directory, target] = process.argv.slice(2);
let hits = 0;
cp.spawnSync = function (command, args, options) {
  let request = null;
  try { request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null; } catch {}
  if (request && request.action === action) {
    hits++;
    request.testCrashAt = point;
    options = { ...options, input: JSON.stringify(request) };
  }
  return original.call(cp, command, args, options);
};
const result = action === 'quarantine-unlink'
  ? guards.unlinkRegularFileUnder(root, directory, target, { allowMissing: true })
  : guards.removeEmptyDirectoryUnder(root, directory, target);
console.log(JSON.stringify({ hits, result, action, argv: process.argv.slice(1) }));
`
  const probe = spawnSync(process.execPath, ['-e', script, guardsPath, action, point, authorityRoot, directory, target], {
    encoding: 'utf8', timeout: 10000, env: { ...process.env, ORCHESTRATOR_FILE_GUARD_TEST_MODE: '1' }
  })
  assert.equal(probe.status, 0, probe.stderr + probe.stdout)
  const result = JSON.parse(probe.stdout.trim())
  assert.ok(result.hits > 0, `guard failpoint for ${action} was not reached: ${JSON.stringify(result)}`)
  return result.result
}

try {
  writeIndex()
  intake.init()

  await check('valid model output publishes a source-bound complete advisory without mutating task or INDEX', async () => {
    const stem = 'TASK_1_profile'
    addTask(stem, 'Profile', taskText(1, 'Profile', 'Add a profile page.'))
    const beforeTask = taskBytes(stem)
    const beforeIndex = readFileSync(join(tasks, 'INDEX.json'))
    const queued = intake.schedule(stem, 'test')
    assert.equal(queued.status, 'queued')
    const complete = await waitFor(() => { const r = record(stem); return r.status === 'complete' && r })
    assert.equal(complete.readiness, 'ready')
    assert.deepEqual(taskBytes(stem), beforeTask)
    assert.deepEqual(readFileSync(join(tasks, 'INDEX.json')), beforeIndex)
    const projected = intake.snapshot()
    assert.equal(projected[stem].sourceHash, complete.sourceHash)
  })

  await check('an authoritative runner claim shadows but does not invalidate its source-bound advisory', async () => {
    const stem = 'TASK_1_profile'
    const claim = join(requestsDir, '.1786651007116-intakeshadow.claim')
    writePrivateJson(claim, {
      version: 3,
      action: 'prep',
      stem,
      expectedState: 'backlog',
      sourceRevision: 'sha256:' + 'a'.repeat(64),
      dedupKey: null,
      dedupReport: null,
      projectRoot: root,
      prompt: 'Prepare the task.',
      createdAt: '2026-08-13T19:55:21.214Z'
    })
    assert.equal(intake.snapshot()[stem], undefined,
      'an authoritative claim must hide the advisory from UI projection')
    assert.equal(intake.scanIntegrity(stem).findings.some((finding) =>
      finding.code === 'INTAKE_RESULT_INVALID'), false,
      'the runner claim window must not make its own execution fence fail')
    rmSync(claim)
    assert.ok(intake.snapshot()[stem], 'the advisory becomes visible again if handoff is refused')
  })

  await check('model process is tool-less, safe-mode, schema-bound, scratch-cwd, and prompt context is bounded', async () => {
    const args = JSON.parse(readFileSync(join(intakeDir, 'captured-args.json'), 'utf8'))
    assert.ok(args.includes('--safe-mode'))
    assert.ok(args.includes('--no-session-persistence'))
    assert.ok(args.includes('--disable-slash-commands'))
    assert.equal(args[args.indexOf('--tools') + 1], '')
    assert.ok(args.includes('--json-schema'))
    const cliSchema = JSON.parse(args[args.indexOf('--json-schema') + 1])
    assert.equal(cliSchema.$schema, undefined,
      'Claude CLI receives a dialect-neutral transport clone')
    assert.match(cliSchema.$id, /shallow-intake-model-output-v1/)
    assert.ok(cliSchema.$defs && cliSchema.$defs.evidence,
      'transport adaptation must retain refs and all validation bounds')
    assert.ok(args.includes('--system-prompt'))
    const prompt = readFileSync(join(intakeDir, 'captured-prompt.txt'), 'utf8')
    assert.match(prompt, /UNTRUSTED DATA/)
    assert.match(prompt, /NOT task-prep/)
    assert.doesNotMatch(prompt, /\/Users\/|Projects\/Pet/)
    const cwd = readFileSync(join(intakeDir, 'captured-cwd.txt'), 'utf8')
    assert.ok(!cwd.startsWith(resolve(process.cwd())))
    assert.equal(Number(readFileSync(join(intakeDir, 'captured-cwd-mode.txt'), 'utf8')) & 0o077, 0,
      'the model scratch generation must be private')
    await waitFor(() => !existsSync(cwd), 10000)
  })

  await check('scratch recovery is WAL-backed, bounded, and preserves foreign or unrecognized generations', async () => {
    const scratchRoot = scratchDir
    const requestDir = (digit) => join(scratchRoot, `intake-${digit.repeat(32)}`)

    const ordinary = requestDir('3')
    mkdirSync(ordinary, { mode: 0o700 })
    writePrivateBytes(join(ordinary, 'prompt.txt'), 'ordinary orphan prompt')
    writePrivateBytes(join(ordinary, '.model-executable'), '#!/bin/sh\nexit 0\n')
    chmodSync(join(ordinary, '.model-executable'), 0o700)

    const innerCrash = requestDir('4')
    mkdirSync(innerCrash, { mode: 0o700 })
    writePrivateBytes(join(innerCrash, 'prompt.txt'), 'detached orphan prompt')
    assert.equal(guardCrash('quarantine-unlink', 'guard:after-detach',
      scratchAuthorityRoot, innerCrash, join(innerCrash, 'prompt.txt')), false)
    assert.equal(existsSync(join(innerCrash, 'prompt.txt')), false)
    assert.ok(readdirSync(innerCrash).some((name) => name.startsWith('.guard-')))

    const outerCrash = requestDir('5')
    mkdirSync(outerCrash, { mode: 0o700 })
    assert.equal(guardCrash('remove-empty-directory', 'guard:after-detach',
      scratchAuthorityRoot, scratchRoot, outerCrash), false)
    assert.equal(existsSync(outerCrash), false)
    assert.ok(readdirSync(scratchRoot).some((name) => name.startsWith('.guard-')))

    const outsideDir = join(intakeDir, 'scratch-outside-symlink-target')
    mkdirSync(outsideDir, { mode: 0o700 })
    writePrivateBytes(join(outsideDir, 'sentinel.txt'), 'outside symlink sentinel')
    const symlinked = requestDir('6')
    symlinkSync(outsideDir, symlinked)

    const outsideHardlink = join(intakeDir, 'scratch-outside-hardlink.txt')
    writePrivateBytes(outsideHardlink, 'outside hardlink sentinel')
    const hardlinked = requestDir('7')
    mkdirSync(hardlinked, { mode: 0o700 })
    linkSync(outsideHardlink, join(hardlinked, 'prompt.txt'))

    const unknown = requestDir('8')
    mkdirSync(unknown, { mode: 0o700 })
    writePrivateBytes(join(unknown, 'keep.txt'), 'unknown evidence')
    const corruptEvidence = join(scratchRoot, '.guard-foreign-evidence')
    writePrivateBytes(corruptEvidence, 'foreign guard evidence')

    const stem = 'TASK_28_scratch_recovery'
    addTask(stem, 'Scratch recovery', taskText(28, 'Scratch recovery', 'Recover only owned scratch artifacts.'))
    intake.schedule(stem, 'scratch-recovery')
    await waitFor(() => record(stem).status === 'complete')
    await waitFor(() => !existsSync(ordinary) && !existsSync(innerCrash), 10000)
    assert.equal(existsSync(outerCrash), false)
    assert.equal(lstatSync(symlinked).isSymbolicLink(), true)
    assert.equal(readFileSync(join(outsideDir, 'sentinel.txt'), 'utf8'), 'outside symlink sentinel')
    assert.equal(existsSync(join(hardlinked, 'prompt.txt')), true)
    assert.equal(readFileSync(outsideHardlink, 'utf8'), 'outside hardlink sentinel')
    assert.equal(readFileSync(join(unknown, 'keep.txt'), 'utf8'), 'unknown evidence')
    assert.equal(readFileSync(corruptEvidence, 'utf8'), 'foreign guard evidence')
    assert.deepEqual(readdirSync(scratchRoot).filter((name) => name.startsWith('.guard-')), ['.guard-foreign-evidence'])
    rmSync(corruptEvidence)
  })

  await check('scratch scan caps and poisoned old entries cannot wedge an unrelated fresh advisory', async () => {
    const scratchRoot = scratchDir
    const capEntries = []
    for (let index = 0; index < 513; index++) {
      const entry = join(scratchRoot, `.foreign-cap-${String(index).padStart(3, '0')}`)
      writePrivateBytes(entry, 'retained')
      capEntries.push(entry)
    }
    const staleRequests = []
    for (let index = 0; index < 20; index++) {
      const entry = join(scratchRoot, `intake-${(5000 + index).toString(16).padStart(32, '0')}`)
      mkdirSync(entry, { mode: 0o700 })
      staleRequests.push(entry)
    }
    const stem = 'TASK_29_scratch_cap'
    addTask(stem, 'Scratch cap', taskText(29, 'Scratch cap', 'A hostile cleanup inventory must not block fresh work.'))
    intake.schedule(stem, 'scratch-cap')
    await waitFor(() => record(stem).status === 'complete', 10000)
    assert.equal(capEntries.every((entry) => existsSync(entry)), true, 'over-cap evidence must not be partially mutated')
    for (let pass = 0; pass < 30 && staleRequests.some((entry) => existsSync(entry)); pass++) {
      intake._reconcileScratchRoot('intake-' + 'f'.repeat(32), true)
    }
    assert.equal(staleRequests.some((entry) => existsSync(entry)), false,
      'anchored paging must make bounded cleanup progress even above the one-shot listing cap')
    assert.equal(capEntries.every((entry) => existsSync(entry)), true, 'paging must retain foreign entries')
    capEntries.forEach((entry) => rmSync(entry))
  })

  await check('the current request scratch is recreated from safe stale state and rejects unsafe stale content before spawn', async () => {
    const scratchRoot = scratchDir
    const safeStem = 'TASK_30_current_scratch'
    addTask(safeStem, 'Current scratch', taskText(30, 'Current scratch', 'Replace a stale prompt before launch.'))
    const safeRequest = intake.schedule(safeStem, 'current-scratch')
    const safeScratch = join(scratchRoot, safeRequest.requestId)
    mkdirSync(safeScratch, { mode: 0o700 })
    writePrivateBytes(join(safeScratch, 'prompt.txt'), 'stale prompt must never reach the model')
    await waitFor(() => record(safeStem).status === 'complete', 10000)
    assert.doesNotMatch(readFileSync(join(intakeDir, 'captured-prompt.txt'), 'utf8'), /stale prompt must never reach the model/)
    await waitFor(() => !existsSync(safeScratch), 10000)

    const startsBefore = readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').length
    const unsafeStem = 'TASK_31_current_scratch_unsafe'
    addTask(unsafeStem, 'Unsafe current scratch', taskText(31, 'Unsafe current scratch', 'Do not reuse unknown current content.'))
    const unsafeRequest = intake.schedule(unsafeStem, 'current-scratch-unsafe')
    const unsafeScratch = join(scratchRoot, unsafeRequest.requestId)
    mkdirSync(unsafeScratch, { mode: 0o700 })
    writePrivateBytes(join(unsafeScratch, 'foreign.txt'), 'retain foreign current generation')
    const failed = await waitFor(() => { const value = record(unsafeStem); return value.status === 'failed' && value }, 10000)
    assert.equal(failed.errorCode, 'INTAKE_SCRATCH_UNSAFE')
    assert.equal(readFileSync(join(unsafeScratch, 'foreign.txt'), 'utf8'), 'retain foreign current generation')
    assert.equal(readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').length, startsBefore)
  })

  await check('an unsafe scratch configuration stays latched and can never enqueue or spawn', () => {
    const unsafeRoot = join(root, 'unsafe-project')
    const unsafeTasks = join(unsafeRoot, 'orchestrator', 'tasks')
    const unsafeCache = join(unsafeRoot, 'orchestrator', '.cache', 'tasks')
    for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(unsafeTasks, column), { recursive: true })
    for (const dir of ['intake', 'locks', 'requests', 'finalizations', 'creations', 'edits']) mkdirSync(join(unsafeCache, dir), { recursive: true })
    chmodSync(join(unsafeCache, 'intake'), 0o700)
    const stem = 'TASK_1_unsafe_scratch'
    writeFileSync(join(unsafeTasks, 'backlog', stem + '.md'), taskText(1, 'Unsafe scratch', 'Do not spawn inside the repository.'))
    writeCanonicalIndex(unsafeTasks, unsafeRoot, '2026-07-12T00:00:00Z')
    const modulePath = resolve(HERE, '../server/shallow-intake.js')
    const probe = spawnSync(process.execPath, ['-e', `
      const intake = require(${JSON.stringify(modulePath)});
      const result = {};
      try { intake.init(); } catch (error) { result.init = error.code; }
      try { intake.schedule(${JSON.stringify(stem)}, 'unsafe-probe'); } catch (error) { result.schedule = error.code; }
      setTimeout(() => { result.snapshot = intake.snapshot()[${JSON.stringify(stem)}] || null; console.log(JSON.stringify(result)); }, 50);
    `], {
      cwd: unsafeRoot,
      encoding: 'utf8',
      env: {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: unsafeRoot,
        ORCHESTRATOR_TASKS_DIR: unsafeTasks,
        ORCHESTRATOR_TASK_INTAKE_DIR: join(unsafeCache, 'intake'),
        ORCHESTRATOR_LOCKS_DIR: join(unsafeCache, 'locks'),
        ORCHESTRATOR_REQUESTS_DIR: join(unsafeCache, 'requests'),
        ORCHESTRATOR_FINALIZATIONS_DIR: join(unsafeCache, 'finalizations'),
        ORCHESTRATOR_TASK_CREATIONS_DIR: join(unsafeCache, 'creations'),
        ORCHESTRATOR_TASK_EDITS_DIR: join(unsafeCache, 'edits'),
        SHALLOW_INTAKE_SCRATCH_DIR: join(unsafeRoot, 'scratch'),
        SHALLOW_INTAKE_SCRATCH_ROOT: unsafeRoot
      }
    })
    assert.equal(probe.status, 0, probe.stderr)
    const result = JSON.parse(probe.stdout.trim())
    assert.deepEqual(result, { init: 'INTAKE_SCRATCH_UNSAFE', schedule: 'INTAKE_SCRATCH_UNSAFE', snapshot: null })
  })

  await check('a pre-existing non-private scratch root is rejected before prompt publication', () => {
    if (process.platform === 'win32') return
    const project = join(root, 'scratch-privacy-project')
    const projectTasks = join(project, 'orchestrator', 'tasks')
    const taskCache = join(project, 'orchestrator', '.cache', 'tasks')
    const externalScratchRoot = join(root, 'scratch-privacy-authority')
    const externalScratch = join(externalScratchRoot, 'shared-scratch')
    for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(projectTasks, column), { recursive: true })
    for (const name of ['intake', 'locks', 'requests', 'finalizations', 'creations', 'edits']) {
      mkdirSync(join(taskCache, name), { recursive: true })
    }
    chmodSync(join(taskCache, 'intake'), 0o700)
    mkdirSync(externalScratch, { recursive: true })
    chmodSync(externalScratch, 0o777)
    const stem = 'TASK_1_non_private_scratch'
    writeFileSync(join(projectTasks, 'backlog', stem + '.md'), taskText(1, 'Non-private scratch', 'Never publish a prompt into a shared directory.'))
    writeCanonicalIndex(projectTasks, project, '2026-07-12T00:00:00Z')
    const modulePath = resolve(HERE, '../server/shallow-intake.js')
    const probe = spawnSync(process.execPath, ['-e', `
      const intake = require(${JSON.stringify(modulePath)});
      const result = {};
      try { intake.init(); } catch (error) { result.init = error.code; }
      try { intake.schedule(${JSON.stringify(stem)}, 'privacy-probe'); } catch (error) { result.schedule = error.code; }
      result.promptFiles = require('node:fs').readdirSync(${JSON.stringify(externalScratch)});
      console.log(JSON.stringify(result));
    `], {
      cwd: project,
      encoding: 'utf8',
      env: {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: project,
        ORCHESTRATOR_TASKS_DIR: projectTasks,
        ORCHESTRATOR_TASK_INTAKE_DIR: join(taskCache, 'intake'),
        ORCHESTRATOR_LOCKS_DIR: join(taskCache, 'locks'),
        ORCHESTRATOR_REQUESTS_DIR: join(taskCache, 'requests'),
        ORCHESTRATOR_FINALIZATIONS_DIR: join(taskCache, 'finalizations'),
        ORCHESTRATOR_TASK_CREATIONS_DIR: join(taskCache, 'creations'),
        ORCHESTRATOR_TASK_EDITS_DIR: join(taskCache, 'edits'),
        SHALLOW_INTAKE_SCRATCH_DIR: externalScratch,
        SHALLOW_INTAKE_SCRATCH_ROOT: externalScratchRoot
      }
    })
    assert.equal(probe.status, 0, probe.stderr)
    assert.deepEqual(JSON.parse(probe.stdout.trim()), {
      init: 'INTAKE_SCRATCH_UNSAFE', schedule: 'INTAKE_SCRATCH_UNSAFE', promptFiles: []
    })
  })

  await check('non-private intake and stem-lock directories are rejected as owner authorities', () => {
    if (process.platform === 'win32') return
    const modulePath = resolve(HERE, '../server/shallow-intake.js')
    for (const variant of ['intake', 'stem-locks']) {
      const project = join(root, 'runtime-privacy-' + variant)
      const projectTasks = join(project, 'orchestrator', 'tasks')
      const taskCache = join(project, 'orchestrator', '.cache', 'tasks')
      const runtime = join(taskCache, 'intake')
      const stemLocks = join(runtime, '.stem-locks')
      const externalScratchRoot = join(root, 'runtime-privacy-scratch-' + variant)
      const externalScratch = join(externalScratchRoot, 'scratch')
      for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(projectTasks, column), { recursive: true })
      for (const name of ['locks', 'requests', 'finalizations', 'creations', 'edits']) mkdirSync(join(taskCache, name), { recursive: true })
      mkdirSync(stemLocks, { recursive: true })
      mkdirSync(externalScratch, { recursive: true })
      chmodSync(runtime, variant === 'intake' ? 0o777 : 0o700)
      chmodSync(stemLocks, variant === 'stem-locks' ? 0o777 : 0o700)
      chmodSync(externalScratch, 0o700)
      const stem = `TASK_1_non_private_${variant.replace('-', '_')}`
      writeFileSync(join(projectTasks, 'backlog', stem + '.md'), taskText(1, 'Non-private runtime', 'Do not trust shared owner directories.'))
      writeCanonicalIndex(projectTasks, project, '2026-07-12T00:00:00Z')
      const probe = spawnSync(process.execPath, ['-e', `
        const intake = require(${JSON.stringify(modulePath)});
        const result = {};
        try { intake.init(); } catch (error) { result.init = error.code; }
        try { intake.schedule(${JSON.stringify(stem)}, 'runtime-privacy'); } catch (error) { result.schedule = error.code; }
        result.recordExists = require('node:fs').existsSync(${JSON.stringify(join(runtime, stem + '.json'))});
        console.log(JSON.stringify(result));
      `], {
        cwd: project,
        encoding: 'utf8',
        env: {
          ...process.env,
          ORCHESTRATOR_PROJECT_ROOT: project,
          ORCHESTRATOR_TASKS_DIR: projectTasks,
          ORCHESTRATOR_TASK_INTAKE_DIR: runtime,
          ORCHESTRATOR_LOCKS_DIR: join(taskCache, 'locks'),
          ORCHESTRATOR_REQUESTS_DIR: join(taskCache, 'requests'),
          ORCHESTRATOR_FINALIZATIONS_DIR: join(taskCache, 'finalizations'),
          ORCHESTRATOR_TASK_CREATIONS_DIR: join(taskCache, 'creations'),
          ORCHESTRATOR_TASK_EDITS_DIR: join(taskCache, 'edits'),
          SHALLOW_INTAKE_SCRATCH_DIR: externalScratch,
          SHALLOW_INTAKE_SCRATCH_ROOT: externalScratchRoot
        }
      })
      assert.equal(probe.status, 0, probe.stderr)
      assert.deepEqual(JSON.parse(probe.stdout.trim()), {
        init: 'INTAKE_DIR_UNSAFE', schedule: 'INTAKE_DIR_UNSAFE', recordExists: false
      })
    }
  })

  await check('same-user read-only exposure is hardened to private mode on startup', () => {
    if (process.platform === 'win32') return
    const project = join(root, 'runtime-privacy-self-heal')
    const projectTasks = join(project, 'orchestrator', 'tasks')
    const taskCache = join(project, 'orchestrator', '.cache', 'tasks')
    const runtime = join(taskCache, 'intake')
    const stemLocks = join(runtime, '.stem-locks')
    const externalScratch = join(root, 'runtime-privacy-self-heal-scratch')
    for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(projectTasks, column), { recursive: true })
    mkdirSync(stemLocks, { recursive: true })
    mkdirSync(externalScratch, { recursive: true })
    chmodSync(runtime, 0o755)
    chmodSync(stemLocks, 0o755)
    chmodSync(externalScratch, 0o755)
    const modulePath = resolve(HERE, '../server/shallow-intake.js')
    const probe = spawnSync(process.execPath, ['-e', `
      const intake = require(${JSON.stringify(modulePath)});
      intake.prepareRuntime();
    `], {
      cwd: project,
      encoding: 'utf8',
      env: {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: project,
        ORCHESTRATOR_TASKS_DIR: projectTasks,
        ORCHESTRATOR_TASK_INTAKE_DIR: runtime,
        SHALLOW_INTAKE_SCRATCH_DIR: externalScratch,
        SHALLOW_INTAKE_SCRATCH_ROOT: root,
      }
    })
    assert.equal(probe.status, 0, probe.stderr)
    assert.equal(statSync(runtime).mode & 0o777, 0o700)
    assert.equal(statSync(stemLocks).mode & 0o777, 0o700)
    assert.equal(statSync(externalScratch).mode & 0o777, 0o700)
  })

  await check('a symlinked intake ancestor is rejected before any external cache write', () => {
    const project = join(root, 'unsafe-intake-project')
    const projectTasks = join(project, 'orchestrator', 'tasks')
    const taskCache = join(project, 'orchestrator', '.cache', 'tasks')
    const external = join(root, 'unsafe-intake-external')
    const scratchRoot = join(root, 'unsafe-intake-scratch-root')
    for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(projectTasks, column), { recursive: true })
    for (const name of ['locks', 'requests', 'finalizations', 'creations', 'edits']) mkdirSync(join(taskCache, name), { recursive: true })
    mkdirSync(external, { recursive: true })
    mkdirSync(scratchRoot, { recursive: true })
    symlinkSync(external, join(taskCache, 'intake-link'))
    const unsafeIntake = join(taskCache, 'intake-link', 'records')
    const unsafeStem = 'TASK_1_unsafe_intake'
    writeFileSync(join(projectTasks, 'backlog', unsafeStem + '.md'), taskText(1, 'Unsafe intake', 'Do not write through an ancestor symlink.'))
    writeCanonicalIndex(projectTasks, project, '2026-07-12T00:00:00Z')
    const modulePath = resolve(HERE, '../server/shallow-intake.js')
    const probe = spawnSync(process.execPath, ['-e', `
      const intake = require(${JSON.stringify(modulePath)});
      const result = {};
      try { intake.init(); } catch (error) { result.init = error.code; }
      try { intake.schedule(${JSON.stringify(unsafeStem)}, 'unsafe-ancestor'); } catch (error) { result.schedule = error.code; }
      result.snapshot = intake.snapshot();
      console.log(JSON.stringify(result));
    `], {
      cwd: project,
      encoding: 'utf8',
      env: {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: project,
        ORCHESTRATOR_TASKS_DIR: projectTasks,
        ORCHESTRATOR_TASK_INTAKE_DIR: unsafeIntake,
        ORCHESTRATOR_LOCKS_DIR: join(taskCache, 'locks'),
        ORCHESTRATOR_REQUESTS_DIR: join(taskCache, 'requests'),
        ORCHESTRATOR_FINALIZATIONS_DIR: join(taskCache, 'finalizations'),
        ORCHESTRATOR_TASK_CREATIONS_DIR: join(taskCache, 'creations'),
        ORCHESTRATOR_TASK_EDITS_DIR: join(taskCache, 'edits'),
        SHALLOW_INTAKE_SCRATCH_DIR: join(scratchRoot, 'scratch'),
        SHALLOW_INTAKE_SCRATCH_ROOT: scratchRoot
      }
    })
    assert.equal(probe.status, 0, probe.stderr)
    assert.deepEqual(JSON.parse(probe.stdout.trim()), { init: 'INTAKE_DIR_UNSAFE', schedule: 'INTAKE_DIR_UNSAFE', snapshot: {} })
    assert.deepEqual(readdirSync(external), [], 'the symlink target must remain untouched')
  })

  await check('global runtime enumeration fails closed at its ceiling while scoped integrity remains target-bounded', () => {
    const childProcess = require('node:child_process')
    const originalSpawnSync = childProcess.spawnSync
    let boundedRequest = false
    childProcess.spawnSync = function (command, args, options) {
      let request = null
      try { request = options && typeof options.input === 'string' ? JSON.parse(options.input) : null } catch {}
      if (request && request.action === 'directory-names' && options.cwd === root) {
        boundedRequest = request.maxEntries === 10000
        return { status: 0, stdout: JSON.stringify({ ok: false, code: 'directory-entry-limit', names: [] }), stderr: '' }
      }
      return originalSpawnSync.call(this, command, args, options)
    }
    try {
      assert.deepEqual(Object.keys(intake.snapshot()), [])
      assert.equal(boundedRequest, true, 'intake must delegate a fixed 10k entry budget to its anchored worker')
      const global = intake.scanIntegrity()
      assert.equal(global.truncated, true)
      assert.equal(global.findings.some((finding) => finding.code === 'INTAKE_RUNTIME_SCAN_LIMIT'), true)
      const scoped = intake.scanIntegrity('TASK_1_profile')
      assert.equal(scoped.truncated, false,
        'one-stem admission must inspect deterministic target evidence without a global name scan')
      assert.equal(scoped.findings.some((finding) =>
        finding.code === 'INTAKE_RUNTIME_SCAN_LIMIT' || finding.code === 'INTAKE_LOCK_SCAN_LIMIT'), false)
    } finally {
      childProcess.spawnSync = originalSpawnSync
    }
  })

  await check('same stem and source are deduplicated to one model start', async () => {
    const stem = 'TASK_2_dedup'
    addTask(stem, 'Dedup', taskText(2, 'Dedup', 'One idea.'))
    const before = existsSync(join(intakeDir, 'fake-starts.log')) ? readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').length : 0
    const first = intake.schedule(stem, 'first')
    const second = intake.schedule(stem, 'second')
    assert.equal(second.requestId, first.requestId)
    await waitFor(() => record(stem).status === 'complete')
    const after = readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').length
    assert.equal(after - before, 1)
  })

  await check('worker attach and model bind recover only their exact durable CAS response loss', async () => {
    const workerFile = join(intakeDir, '.worker.json')
    await waitFor(() => !existsSync(workerFile), 10000)
    const stem = 'TASK_330_worker_cas_response_loss'
    addTask(stem, 'Worker CAS response loss', taskText(330, 'Worker CAS response loss',
      'Both exact worker identity transitions must survive a lost commit response.'))
    const before = existsSync(join(intakeDir, 'fake-starts.log'))
      ? readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').filter(Boolean).length : 0
    const originalCas = fileGuards.compareAndSwapRegularFileUnder
    let committedResponsesLost = 0
    try {
      fileGuards.compareAndSwapRegularFileUnder = function (...args) {
        const result = originalCas.apply(fileGuards, args)
        if (result?.ok && committedResponsesLost < 2) {
          committedResponsesLost++
          return { ok: false, code: 'guard-chain-raced' }
        }
        return result
      }
      intake.schedule(stem, 'worker-cas-response-loss')
      await waitFor(() => record(stem).status === 'complete', 10000)
      await waitFor(() => !existsSync(workerFile), 10000)
    } finally {
      fileGuards.compareAndSwapRegularFileUnder = originalCas
    }
    const after = readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').filter(Boolean).length
    assert.equal(committedResponsesLost, 2, 'both attach and model-bind commit responses must be exercised')
    assert.equal(after - before, 1, 'exact self-recognition must release the model once, never replay it')
  })

  await check('worker model bind never adopts a failed CAS without its exact public bytes', async () => {
    const workerFile = join(intakeDir, '.worker.json')
    await waitFor(() => !existsSync(workerFile), 10000)
    const stem = 'TASK_331_worker_cas_no_commit'
    addTask(stem, 'Worker CAS no commit', taskText(331, 'Worker CAS no commit',
      'A failed model-bind CAS without exact public bytes must remain behind the gate.'))
    const before = readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').filter(Boolean).length
    const originalCas = fileGuards.compareAndSwapRegularFileUnder
    let calls = 0
    try {
      fileGuards.compareAndSwapRegularFileUnder = function (...args) {
        calls++
        if (calls === 2) return { ok: false, code: 'guard-chain-raced' }
        return originalCas.apply(fileGuards, args)
      }
      intake.schedule(stem, 'worker-cas-no-commit')
      await waitFor(() => record(stem).status === 'failed', 10000)
      await waitFor(() => !existsSync(workerFile), 10000)
    } finally {
      fileGuards.compareAndSwapRegularFileUnder = originalCas
    }
    const after = readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').filter(Boolean).length
    assert.equal(calls, 2, 'the negative fixture must fail the model-bind CAS after a real attach')
    assert.equal(after, before, 'a no-commit bind failure must execute zero model bytes')
  })

  await check('worker model bind rejects a generation changed between its durability proofs', async () => {
    const workerFile = join(intakeDir, '.worker.json')
    await waitFor(() => !existsSync(workerFile), 10000)
    const stem = 'TASK_332_worker_cas_reproof_race'
    addTask(stem, 'Worker CAS reproof race', taskText(332, 'Worker CAS reproof race',
      'A changed public generation between exact reads must remain behind the gate.'))
    const before = readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').filter(Boolean).length
    const originalCas = fileGuards.compareAndSwapRegularFileUnder
    const originalFsyncDirectory = fileGuards.fsyncDirectoryUnder
    let calls = 0, rollbackBytes = null, replaceDuringReproof = false
    try {
      fileGuards.compareAndSwapRegularFileUnder = function (...args) {
        calls++
        if (calls !== 2) return originalCas.apply(fileGuards, args)
        rollbackBytes = Buffer.from(args[4].bytes)
        const committed = originalCas.apply(fileGuards, args)
        assert.equal(committed?.ok, true, 'the race fixture requires a real bound-record commit')
        replaceDuringReproof = true
        return { ok: false, code: 'guard-chain-raced' }
      }
      fileGuards.fsyncDirectoryUnder = function (...args) {
        const result = originalFsyncDirectory.apply(fileGuards, args)
        if (replaceDuringReproof && args[1] === intakeDir) {
          replaceDuringReproof = false
          writePrivateBytes(workerFile, rollbackBytes)
        }
        return result
      }
      intake.schedule(stem, 'worker-cas-reproof-race')
      await waitFor(() => record(stem).status === 'failed', 10000)
      await waitFor(() => !existsSync(workerFile), 10000)
    } finally {
      fileGuards.compareAndSwapRegularFileUnder = originalCas
      fileGuards.fsyncDirectoryUnder = originalFsyncDirectory
    }
    const after = readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').filter(Boolean).length
    assert.equal(calls, 2, 'the race fixture must reach model binding after a real attach')
    assert.equal(replaceDuringReproof, false, 'the public generation must change inside the recognition proof window')
    assert.equal(after, before, 'a changed reproof generation must execute zero model bytes')
  })

  await check('stem-lock release removes only the exact JS file-guard generation', () => {
    const stem = 'TASK_33_exact_owner_release'
    const lockFile = join(intakeDir, '.stem-locks', stem + '.json')
    const result = intake._withStemLock(stem, () => {
      const owned = JSON.parse(readFileSync(lockFile, 'utf8'))
      assert.equal(owned.pid, process.pid)
      assert.match(owned.token, /^intake-[a-f0-9]{32}$/)
      return 42
    })
    assert.equal(result, 42)
    assert.equal(existsSync(lockFile), false)
  })

  await check('owner acquisition retries only an exists-to-missing handoff', () => {
    const originalPublish = fileGuards.publishNoClobberRegularFileUnder
    let calls = 0
    try {
      fileGuards.publishNoClobberRegularFileUnder = function (...args) {
        calls++
        if (calls === 1) return { ok: false, code: 'exists' }
        return originalPublish.apply(fileGuards, args)
      }
      assert.equal(intake._withStemLock('TASK_331_owner_handoff', () => 7), 7)
      assert.equal(calls, 2, 'exact missing after exists must retry one bounded publication attempt')

      calls = 0
      fileGuards.publishNoClobberRegularFileUnder = function () {
        calls++
        return { ok: false, code: 'directory-unsafe' }
      }
      assert.throws(() => intake._withStemLock('TASK_332_owner_failure', () => 8),
        (error) => error?.code === 'INTAKE_OWNER_INVALID')
      assert.equal(calls, 1, 'missing after a non-exists publication failure must remain fail-closed')
    } finally {
      fileGuards.publishNoClobberRegularFileUnder = originalPublish
    }
  })

  await check('malformed output becomes a non-blocking failed preview', async () => {
    const stem = 'TASK_3_malformed'
    const bytes = taskText(3, 'Malformed', 'MALFORMED_MODEL must remain task data.')
    addTask(stem, 'Malformed', bytes)
    intake.schedule(stem, 'test')
    const failed = await waitFor(() => { const r = record(stem); return r.status === 'failed' && r })
    assert.equal(failed.retryable, false)
    assert.match(failed.errorCode, /INVALID_ENVELOPE|INVALID_JSON/)
    assert.deepEqual(taskBytes(stem), Buffer.from(bytes))
  })

  await check('timeout kills the advisory child and leaves the backlog task usable', async () => {
    const stem = 'TASK_4_timeout'
    const bytes = taskText(4, 'Timeout', 'SLOW_MODEL must time out.')
    addTask(stem, 'Timeout', bytes)
    const workerFile = join(intakeDir, '.worker.json')
    // The previous failed result is published before its crash-safe scratch and
    // owner cleanup finishes. Separate that global-slot latency from the model
    // timeout itself so this assertion measures only an admitted generation.
    await waitFor(() => !existsSync(workerFile), 10000)
    const queued = intake.schedule(stem, 'test')
    await waitFor(() => {
      if (!existsSync(workerFile)) return null
      const worker = JSON.parse(readFileSync(workerFile, 'utf8'))
      return worker.requestId === queued.requestId && worker.modelPid && worker
    }, 10000)
    const failed = await waitFor(() => { const r = record(stem); return r.status === 'failed' && r }, 4000)
    assert.equal(failed.errorCode, 'MODEL_TIMEOUT')
    assert.equal(failed.retryable, true)
    assert.deepEqual(taskBytes(stem), Buffer.from(bytes))
  })

  await check('the POSIX model cannot create a process generation outside containment', async () => {
    if (process.platform === 'win32') return
    const stem = 'TASK_27_escaped_descendant'
    addTask(stem, 'Escaped descendant', taskText(27, 'Escaped descendant', 'ESCAPED_DESCENDANT must stay inside containment.'))
    intake.schedule(stem, 'containment-proof')
    const workerFile = join(intakeDir, '.worker.json')
    await waitFor(() => {
      if (!existsSync(workerFile)) return null
      const value = JSON.parse(readFileSync(workerFile, 'utf8'))
      return value.childPid && value
    })
    await waitFor(() => record(stem).status === 'complete', 10000)
    const scratch = readFileSync(join(intakeDir, 'captured-cwd.txt'), 'utf8')
    await waitFor(() => !existsSync(workerFile), 10000)
    await waitFor(() => !existsSync(scratch), 10000)
    assert.equal(readFileSync(join(intakeDir, 'fork-denied.log'), 'utf8').trim().split('\n').includes(stem), true,
      'the test must observe a kernel/sandbox fork denial rather than pass without exercising it')
    const escapedLog = join(intakeDir, 'escaped-pids.log')
    if (existsSync(escapedLog)) {
      const escapedPid = Number(readFileSync(escapedLog, 'utf8').trim().split('\n').at(-1))
      assert.ok(Number.isInteger(escapedPid) && escapedPid > 0)
      await waitFor(() => {
        try { process.kill(escapedPid, 0); return false }
        catch (error) { return error && error.code === 'ESRCH' }
      }, 10000)
    }
  })

  await check('Linux exact model binding kills the direct generation if its wrapper dies abruptly', async () => {
    // Darwin deliberately retains this generation without authenticated
    // DRAINED; the native fork+setsid crash contract is exercised in the
    // dedicated shallow-intake-darwin-orphan integration test.
    if (process.platform !== 'linux') return
    const stem = 'TASK_329_abrupt_wrapper'
    const marker = join(intakeDir, 'abrupt-model.pid')
    rmSync(marker, { force: true })
    addTask(stem, 'Abrupt wrapper', taskText(329, 'Abrupt wrapper', 'ABRUPT_WRAPPER must not leave a model orphan.'))
    const queued = intake.schedule(stem, 'wrapper-sigkill-proof')
    const workerFile = join(intakeDir, '.worker.json')
    const worker = await waitFor(() => {
      if (!existsSync(workerFile)) return null
      const value = JSON.parse(readFileSync(workerFile, 'utf8'))
      return value.requestId === queued.requestId && value.childPid && value.modelPid && value.modelProcessStartId && value
    })
    const modelPid = await waitFor(() => {
      if (!existsSync(marker)) return null
      const value = Number(readFileSync(marker, 'utf8').trim())
      return Number.isInteger(value) && value > 0 ? value : null
    })
    assert.notEqual(modelPid, worker.childPid)
    assert.equal(worker.modelPid, modelPid, 'the durable worker must bind the exact direct model reported by the wrapper-only channel')
    process.kill(worker.childPid, 'SIGKILL')
    await waitFor(() => {
      try { process.kill(modelPid, 0); return false }
      catch (error) { return error && error.code === 'ESRCH' }
    }, 10000)
    await waitFor(() => !existsSync(workerFile), 10000)
    await waitFor(() => !existsSync(join(scratchDir, queued.requestId)), 10000)
    const failed = await waitFor(() => { const value = record(stem); return value.status === 'failed' && value }, 10000)
    assert.equal(failed.errorCode, 'MODEL_PROCESS_FAILED')
  })

  await check('an unavailable model CLI remains an advisory failure and never touches the task', async () => {
    const stem = 'TASK_20_missing_model_cli'
    const bytes = taskText(20, 'Missing model CLI', 'Creation must remain usable without Claude installed.')
    addTask(stem, 'Missing model CLI', bytes)
    const previous = process.env.SHALLOW_INTAKE_CLAUDE
    process.env.SHALLOW_INTAKE_CLAUDE = join(root, 'definitely-missing-claude')
    try {
      const queued = intake.schedule(stem, 'missing-cli')
      assert.equal(queued.status, 'queued')
      const failed = await waitFor(() => { const r = record(stem); return r.status === 'failed' && r })
      assert.equal(failed.errorCode, 'CLI_UNAVAILABLE')
      assert.equal(failed.retryable, false)
      assert.deepEqual(taskBytes(stem), Buffer.from(bytes))
    } finally { process.env.SHALLOW_INTAKE_CLAUDE = previous }
  })

  await check('the macOS Keychain sandbox incompatibility is precise, non-retryable, and does not persist stderr', async () => {
    const stem = 'TASK_34_keychain_sandbox_incompatibility'
    const bytes = taskText(34, 'Keychain sandbox incompatibility',
      'KEYCHAIN_SANDBOX_FAILURE must remain an advisory transport failure.')
    addTask(stem, 'Keychain sandbox incompatibility', bytes)
    const queued = intake.schedule(stem, 'keychain-sandbox-fixture')
    assert.equal(queued.status, 'queued')
    const failed = await waitFor(() => { const r = record(stem); return r.status === 'failed' && r })
    assert.equal(failed.errorCode, 'MODEL_KEYCHAIN_SANDBOX_INCOMPATIBLE')
    assert.equal(failed.retryable, false)
    assert.deepEqual(taskBytes(stem), Buffer.from(bytes))
    const events = readFileSync(join(intakeDir, 'events.jsonl'), 'utf8').trim().split('\n').map(JSON.parse)
    const event = events.slice().reverse().find((row) => row.event === 'shallow-intake-failed' && row.stem === stem)
    assert.equal(event.errorCode, 'MODEL_KEYCHAIN_SANDBOX_INCOMPATIBLE')
    assert.equal(Object.keys(event).some((key) => /stderr|message|detail/i.test(key)), false,
      'provider stderr must stay memory-only and out of durable/public diagnostics')
  })

  await check('a CLI schema-dialect rejection is precise, non-retryable, and does not persist stderr', async () => {
    const stem = 'TASK_35_schema_transport_incompatibility'
    const bytes = taskText(35, 'Schema transport incompatibility',
      'SCHEMA_TRANSPORT_FAILURE must remain an advisory transport failure.')
    addTask(stem, 'Schema transport incompatibility', bytes)
    intake.schedule(stem, 'schema-transport-fixture')
    const failed = await waitFor(() => { const r = record(stem); return r.status === 'failed' && r })
    assert.equal(failed.errorCode, 'MODEL_SCHEMA_TRANSPORT_INCOMPATIBLE')
    assert.equal(failed.retryable, false)
    assert.deepEqual(taskBytes(stem), Buffer.from(bytes))
    const events = readFileSync(join(intakeDir, 'events.jsonl'), 'utf8').trim().split('\n').map(JSON.parse)
    const event = events.slice().reverse().find((row) => row.event === 'shallow-intake-failed' && row.stem === stem)
    assert.equal(event.errorCode, 'MODEL_SCHEMA_TRANSPORT_INCOMPATIBLE')
    assert.equal(Object.keys(event).some((key) => /stderr|message|detail/i.test(key)), false)
  })

  await check('edit during a running preview fences the late generation and publishes only the new hash', async () => {
    const stem = 'TASK_5_edit'
    addTask(stem, 'Edit', taskText(5, 'Edit', 'SLOW_MODEL old source.'))
    const old = intake.schedule(stem, 'test')
    await waitFor(() => record(stem).status === 'checking')
    writeFileSync(join(tasks, 'backlog', stem + '.md'), taskText(5, 'Edit', 'New source after edit.'))
    intake.reconcile()
    const current = await waitFor(() => { const r = record(stem); return r.status === 'complete' && r.requestId !== old.requestId && r })
    assert.notEqual(current.sourceHash, old.sourceHash)
    assert.equal(intake.snapshot()[stem].requestId, current.requestId)
  })

  await check('pending sidecar supersedes a running preview although backlog body still exists', async () => {
    const stem = 'TASK_6_pending'
    addTask(stem, 'Pending', taskText(6, 'Pending', 'SLOW_MODEL pending race.'))
    intake.schedule(stem, 'test')
    await waitFor(() => record(stem).status === 'checking')
    writeFileSync(join(tasks, 'pending', stem + '.questions.md'), [
      '---', 'forTask: ' + stem, 'createdAt: 2026-07-13T08:00:00Z', 'updatedAt: 2026-07-13T08:01:00Z',
      'round: 1', 'gapCount: 1', 'prevGapCount: 2', '---', '',
      '## Q1 — Confirm the boundary?', '', '**Type**: text', '', '### Answer', ''
    ].join('\n'))
    intake.reconcile()
    await waitFor(() => record(stem).status === 'superseded')
    assert.equal(intake.snapshot()[stem], undefined)
  })

  await check('moving a backlog task to another column supersedes the running generation', async () => {
    const stem = 'TASK_21_moved_during_intake'
    addTask(stem, 'Moved during intake', taskText(21, 'Moved during intake', 'SLOW_MODEL column move race.'))
    intake.schedule(stem, 'test')
    await waitFor(() => record(stem).status === 'checking')
    renameSync(join(tasks, 'backlog', stem + '.md'), join(tasks, 'todo', stem + '.md'))
    intake.reconcile()
    const superseded = await waitFor(() => { const r = record(stem); return r.status === 'superseded' && r })
    assert.equal(superseded.reasonCode, 'todo')
    assert.equal(intake.snapshot()[stem], undefined)
  })

  await check('task-prep supersession fences its generation and same-source eligibility restoration reruns intake', async () => {
    const stem = 'TASK_7_prep'
    addTask(stem, 'Prep', taskText(7, 'Prep', 'SLOW_MODEL prep race.'))
    intake.schedule(stem, 'test')
    await waitFor(() => record(stem).status === 'checking')
    intake.supersede(stem, 'authoritative-prep')
    const superseded = await waitFor(() => { const value = record(stem); return value.status === 'superseded' && value })
    assert.equal(intake.snapshot()[stem], undefined)
    intake.reconcile()
    const restored = await waitFor(() => {
      const value = record(stem)
      return value.requestId !== superseded.requestId && value.status === 'failed' && value
    }, 10000)
    assert.equal(restored.sourceHash, superseded.sourceHash)
    assert.equal(restored.errorCode, 'MODEL_TIMEOUT')
  })

  await check('dismiss is source-bound and an edit schedules a fresh preview', async () => {
    const stem = 'TASK_8_dismiss'
    addTask(stem, 'Dismiss', taskText(8, 'Dismiss', 'Dismiss this preview.'))
    intake.schedule(stem, 'test')
    const complete = await waitFor(() => { const r = record(stem); return r.status === 'complete' && r })
    intake.dismiss(stem, complete.sourceHash)
    assert.equal(intake.snapshot()[stem], undefined)
    assert.throws(() => intake.retry(stem, 'sha256:' + '0'.repeat(64)), (e) => e.code === 'source-changed')
    writeFileSync(join(tasks, 'backlog', stem + '.md'), taskText(8, 'Dismiss', 'Edited source enables a new preview.'))
    intake.reconcile()
    const refreshed = await waitFor(() => { const r = record(stem); return r.status === 'complete' && r.sourceHash !== complete.sourceHash && r })
    assert.ok(refreshed)
  })

  await check('prepare failures are durable failed previews rather than eternal queued records', async () => {
    const largeStem = 'TASK_9_large_context'
    addTask(largeStem, 'Large context', taskText(9, 'Large context', 'x'.repeat(40000)))
    intake.schedule(largeStem, 'test')
    const large = await waitFor(() => { const r = record(largeStem); return r.status === 'failed' && r })
    assert.equal(large.errorCode, 'CONTEXT_TOO_LARGE')
    assert.equal(large.retryable, false)

    const indexStem = 'TASK_10_bad_index'
    addTask(indexStem, 'Bad index', taskText(10, 'Bad index', 'Visible failure.'))
    writeFileSync(join(tasks, 'INDEX.json'), '{broken\n')
    intake.schedule(indexStem, 'test')
    const invalid = await waitFor(() => { const r = record(indexStem); return r.status === 'failed' && r })
    assert.match(invalid.errorCode, /INTAKE_JSON_INVALID|INDEX_INVALID/)
    assert.equal(invalid.retryable, true)
    writeIndex()
  })

  await check('active task writer lease closes the claimed-request race for intake and edit source reads', async () => {
    const stem = 'TASK_11_writer_lease'
    addTask(stem, 'Writer lease', taskText(11, 'Writer lease', 'Authoritative prep owns this stem.'))
    const leases = require('../../tasks/writer-leases.cjs')
    const handle = leases.acquire(join(finalizationsDir, '.writers'), { kind: 'task-session', stem, key: 'task:prep:' + stem })
    try {
      const source = intake.sourceState(stem)
      assert.equal(source.eligible, false)
      assert.equal(source.reason, 'task-prep')
      assert.throws(() => intake.schedule(stem, 'test'), (error) => error.code === 'INTAKE_NOT_ELIGIBLE')
    } finally { leases.release(handle) }
  })

  await check('candidate artifact drift filters a stale-INDEX duplicate link and keeps the advisory visible', async () => {
    const candidate = 'TASK_12_duplicate_candidate'
    addTask(candidate, 'Duplicate candidate', taskText(12, 'Duplicate candidate', 'Existing duplicate candidate.'))
    const stem = 'TASK_13_duplicate_target'
    addTask(stem, 'Duplicate target', taskText(13, 'Duplicate target', 'DUPLICATE_MODEL Duplicate candidate requested.'))
    intake.schedule(stem, 'test')
    const complete = await waitFor(() => { const r = record(stem); return r.status === 'complete' && r })
    assert.equal(complete.possibleDuplicates.length, 1)
    assert.equal(complete.possibleDuplicates[0].stem, candidate)
    // Deliberately leave INDEX stale: the target must stop linking as soon as
    // its expected active artifact disappears.
    rmSync(join(tasks, 'backlog', candidate + '.md'))
    const projected = intake.snapshot()[stem]
    assert.ok(projected)
    assert.deepEqual(projected.possibleDuplicates, [])
    assert.equal(projected.readiness, 'ready')
  })

  await check('schema-valid JSON with non-current INDEX metadata is rejected before advisory context', async () => {
    const stem = 'TASK_100_noncurrent_index'
    addTask(stem, 'Non-current index', taskText(100, 'Non-current index', 'Strict INDEX rows only.'))
    const indexFile = join(tasks, 'INDEX.json')
    const index = JSON.parse(readFileSync(indexFile, 'utf8'))
    index.backlog.find((entry) => entry.stem === stem).origin = null
    writeFileSync(indexFile, JSON.stringify(index, null, 2) + '\n')
    intake.schedule(stem, 'test')
    const invalid = await waitFor(() => { const r = record(stem); return r.status === 'failed' && r })
    assert.equal(invalid.errorCode, 'INDEX_INVALID')
    assert.equal(invalid.retryable, true)
    rmSync(join(tasks, 'backlog', stem + '.md'))
    writeIndex()
  })

  await check('candidate preselection uses the target Goal as well as its title', async () => {
    for (let i = 0; i < 40; i++) {
      const stem = `TASK_${200 + i}_unrelated_title_distractor_${i}`
      addTask(stem, `Unrelated title distractor ${i}`, taskText(200 + i, `Unrelated title distractor ${i}`, 'Noise only.'))
    }
    const candidate = 'TASK_250_different_heading'
    addTask(candidate, 'Different heading', taskText(250, 'Different heading', 'Unique semantic goal phrase.'))
    const target = 'TASK_251_unrelated_title'
    addTask(target, 'Unrelated title', taskText(251, 'Unrelated title', 'DUPLICATE_MODEL Duplicate candidate. Unique semantic goal phrase.'))
    intake.schedule(target, 'goal-ranking')
    await waitFor(() => record(target).status === 'complete')
    const prompt = readFileSync(join(intakeDir, 'captured-prompt.txt'), 'utf8')
    const marker = 'BEGIN_UNTRUSTED_CONTEXT_JSON_TO_EOF\n'
    const context = JSON.parse(prompt.slice(prompt.lastIndexOf(marker) + marker.length))
    assert.ok(context.activeCandidates.some((row) => row.stem === candidate))
  })

  await check('manual retry advances the bounded attempt counter', async () => {
    const stem = 'TASK_14_retry_attempt'
    addTask(stem, 'Retry attempt', taskText(14, 'Retry attempt', 'MALFORMED_MODEL retry counter.'))
    intake.schedule(stem, 'test')
    const first = await waitFor(() => { const r = record(stem); return r.status === 'failed' && r })
    assert.equal(first.attempt, 1)
    intake.retry(stem, first.sourceHash)
    const second = await waitFor(() => { const r = record(stem); return r.status === 'failed' && r.attempt === 2 && r })
    assert.equal(second.attempt, 2)
  })

  await check('a wrapper-bound/model-unbound generation needs exact wrapper and containment drain proof', () => {
    if (!['linux', 'darwin'].includes(process.platform)) return
    const exactStart = writerLeases.captureProcessStartId(process.pid)
    const prebind = {
      version: 1, pid: process.pid, processStartId: exactStart, hostname: hostname(),
      token: 'prebind-worker-token-1234', createdAt: '2026-01-01T00:00:00.000Z',
      updatedAt: '2026-01-01T00:00:00.000Z', stem: 'TASK_900_prebind',
      sourceHash: 'sha256:' + '9'.repeat(64), requestId: 'intake-' + '9'.repeat(32), attempt: 1,
      childPid: process.pid, childProcessStartId: exactStart,
      modelPid: null, modelProcessStartId: null, spawnState: 'started'
    }
    assert.equal(intake.validWorker(prebind), true)
    assert.equal(intake.validWorker({ ...prebind, modelPid: process.pid }), false,
      'a half-bound model identity is not a worker record')
    assert.equal(intake.validWorker({ ...prebind, modelPid: process.pid, modelProcessStartId: exactStart }), false,
      'the wrapper generation cannot impersonate the direct model generation')
    assert.equal(intake._workerRecordTreeGone(prebind), false,
      'a live wrapper remains fail-closed even though the model pair is null')

    const deadPid = 2147483647
    const deadStart = 'psid-v1:' + process.platform + ':' + 'e'.repeat(64)
    const drainedPrebind = {
      ...prebind,
      pid: deadPid,
      processStartId: deadStart,
      childPid: deadPid,
      childProcessStartId: deadStart,
    }
    assert.equal(intake.validWorker(drainedPrebind), true)
    assert.equal(intake._workerRecordTreeGone(drainedPrebind, () => 'dead'), true,
      'the pre-exec gate plus exact wrapper death and empty PGID closes partial-binding recovery automatically')
    assert.equal(intake._workerRecordTreeGone(drainedPrebind, () => 'unknown'), false)
  })

  await check('Windows wrapper environment keeps required system resolution variables but no arbitrary parent keys', () => {
    const saved = Object.fromEntries(['SHALLOW_INTAKE_TEST_WINDOWS_JOB', 'SystemRoot', 'WINDIR', 'COMSPEC', 'PATHEXT',
      'SHALLOW_INTAKE_PRIVATE_SENTINEL'].map((key) => [key, process.env[key]]))
    try {
      process.env.SHALLOW_INTAKE_TEST_WINDOWS_JOB = '1'
      process.env.SystemRoot = 'C:\\Windows'
      process.env.WINDIR = 'C:\\Windows'
      process.env.COMSPEC = 'C:\\Windows\\System32\\cmd.exe'
      process.env.PATHEXT = '.COM;.EXE;.BAT;.CMD'
      process.env.SHALLOW_INTAKE_PRIVATE_SENTINEL = 'must-not-cross'
      const env = intake._modelEnv()
      assert.deepEqual({
        SystemRoot: env.SystemRoot, WINDIR: env.WINDIR,
        COMSPEC: env.COMSPEC, PATHEXT: env.PATHEXT,
      }, {
        SystemRoot: 'C:\\Windows', WINDIR: 'C:\\Windows',
        COMSPEC: 'C:\\Windows\\System32\\cmd.exe', PATHEXT: '.COM;.EXE;.BAT;.CMD',
      })
      assert.equal(Object.hasOwn(env, 'SHALLOW_INTAKE_PRIVATE_SENTINEL'), false)
    } finally {
      for (const [key, value] of Object.entries(saved)) {
        if (value === undefined) delete process.env[key]
        else process.env[key] = value
      }
    }
  })

  await check('integrated owner recovery is exact-generation bound and pre-GO workers recover automatically', () => {
    if (!['linux', 'darwin'].includes(process.platform)) return
    const stemLocks = join(intakeDir, '.stem-locks')
    mkdirSync(stemLocks, { recursive: true })
    const deadStart = 'psid-v1:' + process.platform + ':' + 'd'.repeat(64)
    const staleLock = join(stemLocks, 'TASK_15_reused_lock.json')
    writePrivateJson(staleLock, {
      version: 1, pid: process.pid, processStartId: deadStart, hostname: hostname(),
      token: 'reused-lock-token-1234', createdAt: '2000-01-01T00:00:00.000Z'
    })
    assert.equal(intake._recoverStaleOwner(staleLock), true)
    assert.equal(existsSync(staleLock), false)

    const worker = join(intakeDir, '.worker.json')
    writePrivateJson(worker, {
      version: 1, pid: 2147483647, processStartId: deadStart, hostname: hostname(),
      token: 'pre-go-worker-token-1234', createdAt: '2000-01-01T00:00:00.000Z',
      updatedAt: '2000-01-01T00:00:00.000Z', stem: 'TASK_15_old',
      sourceHash: 'sha256:' + 'b'.repeat(64), requestId: 'intake-' + '1'.repeat(32), attempt: 1,
      childPid: null, childProcessStartId: null, modelPid: null, modelProcessStartId: null,
      spawnState: 'not-started'
    })
    assert.equal(intake._recoverStaleOwner(worker), true,
      'the durable pre-GO state proves that no model could have started')
    assert.equal(existsSync(worker), false)
  })

  await check('stem-scoped integrity exposes deterministic file-guard WAL evidence', () => {
    const guards = require('../server/file-guards.js')
    const stem = 'TASK_320_owner_wal_preflight'
    const target = join(intakeDir, '.stem-locks', stem + '.json')
    const evidence = guards.guardTransactionEvidenceForTarget(join(intakeDir, '.stem-locks'), target)
    const manifest = evidence.entries.find((entry) => entry.kind === 'delete' && entry.role === 'manifest')
    writePrivateBytes(manifest.path, '{}\n')
    const scoped = intake.scanIntegrity(stem)
    assert.equal(scoped.findings.some((finding) => finding.code === 'INTAKE_GUARD_TRANSACTION_EVIDENCE' &&
      finding.paths.includes(manifest.path)), true)
    assert.equal(scoped.snapshotInputs.some((input) =>
      input.kind === 'guard-transaction-evidence' && input.path === manifest.path), true)
    rmSync(manifest.path)
  })

  await check('integrated owner guard retains remote, linked, symlinked, malformed, and out-of-authority state', () => {
    if (!['linux', 'darwin'].includes(process.platform)) return
    const stemLocks = join(intakeDir, '.stem-locks')
    const deadStart = 'psid-v1:' + process.platform + ':' + 'e'.repeat(64)
    const record = { version: 1, pid: 2147483647, processStartId: deadStart, hostname: hostname(),
      token: 'unsafe-lock-token-1234', createdAt: '2000-01-01T00:00:00.000Z' }
    const remote = join(stemLocks, 'TASK_15_remote_lock.json')
    writePrivateJson(remote, { ...record, hostname: 'remote.example.invalid' })
    assert.equal(intake._recoverStaleOwner(remote), false)

    const backing = join(stemLocks, 'backing-owner.json')
    const linked = join(stemLocks, 'TASK_15_linked_lock.json')
    writePrivateJson(backing, record)
    linkSync(backing, linked)
    assert.equal(intake._recoverStaleOwner(linked), false)

    const symlinked = join(stemLocks, 'TASK_15_symlinked_lock.json')
    symlinkSync(backing, symlinked)
    assert.equal(intake._recoverStaleOwner(symlinked), false)

    const malformed = join(stemLocks, 'TASK_15_malformed_lock.json')
    writePrivateBytes(malformed, '{"version":1,"version":1}\n')
    assert.equal(intake._recoverStaleOwner(malformed), false)

    const outside = join(root, 'outside-intake', '.stem-locks')
    mkdirSync(outside, { recursive: true })
    const outsideOwner = join(outside, 'TASK_15_outside_lock.json')
    writePrivateJson(outsideOwner, record)
    assert.equal(intake._recoverStaleOwner(outsideOwner), false)
    for (const file of [remote, linked, symlinked, backing, malformed]) rmSync(file, { force: true })
    rmSync(join(root, 'outside-intake'), { recursive: true, force: true })
  })

  await check('runtime integrity exposes private-mode violations and unknown runtime entries globally', () => {
    const invalidScope = intake.scanIntegrity('../outside')
    assert.deepEqual(invalidScope.findings.map((finding) => finding.code), ['INTAKE_SCOPE_INVALID'])
    assert.deepEqual(invalidScope.snapshotInputs, [])
    assert.deepEqual(invalidScope.findings[0].paths, [])

    const stem = 'TASK_1_profile'
    const result = join(intakeDir, stem + '.json')
    chmodSync(result, 0o644)
    const unsafe = intake.scanIntegrity(stem)
    assert.equal(unsafe.findings.some((finding) => finding.code === 'INTAKE_RESULT_UNSAFE'), true)
    chmodSync(result, 0o600)

    const validComplete = JSON.parse(readFileSync(result, 'utf8'))
    writePrivateJson(result, { ...validComplete, summary: '' })
    const invalidComplete = intake.scanIntegrity(stem)
    assert.equal(invalidComplete.findings.some((finding) => finding.code === 'INTAKE_RESULT_INVALID'), true,
      'integrity must apply the full model-result contract, not only top-level shape checks')
    writePrivateJson(result, validComplete)

    const retained = join(intakeDir, '.stem-locks', '.unknown-owner-' + 'a'.repeat(32))
    writePrivateBytes(retained, 'retained evidence\n')
    const scoped = intake.scanIntegrity(stem)
    assert.equal(scoped.findings.some((finding) => finding.paths.includes(retained)), false,
      'unknown runtime state is not mutation authority for an unrelated scoped target')
    const global = intake.scanIntegrity()
    assert.equal(global.findings.some((finding) => finding.code === 'INTAKE_LOCK_ENTRY_UNRECOGNIZED' &&
      finding.paths.includes(retained)), true)
    assert.equal(global.findings.some((finding) => finding.code === 'INTAKE_RUNTIME_ENTRY_UNRECOGNIZED' &&
      finding.paths.includes(scratchDir)), false, 'the exact configured nested scratch root is audited, not treated as foreign state')
    assert.equal(global.statuses.some((status) => status.kind === 'scratch-root' && status.state === 'private'), true)
    rmSync(retained)
  })

  await check('corrupt or partially-written advisory records never poison state and reconcile to a bounded failure', async () => {
    const malformedStem = 'TASK_16_corrupt_shape'
    addTask(malformedStem, 'Corrupt shape', taskText(16, 'Corrupt shape', 'Advisory cache is non-authoritative.'))
    intake.schedule(malformedStem, 'test')
    const complete = await waitFor(() => { const r = record(malformedStem); return r.status === 'complete' && r })
    const malformed = { ...complete, possibleDuplicates: {} }
    writeFileSync(join(intakeDir, malformedStem + '.json'), JSON.stringify(malformed) + '\n')
    assert.doesNotThrow(() => intake.snapshot())
    assert.equal(intake.snapshot()[malformedStem], undefined)
    intake.reconcile()
    const shapeFailure = record(malformedStem)
    assert.equal(shapeFailure.status, 'failed')
    assert.equal(shapeFailure.errorCode, 'INTAKE_CACHE_INVALID')
    assert.equal(shapeFailure.retryable, true)

    const nestedStem = 'TASK_18_corrupt_nested'
    addTask(nestedStem, 'Corrupt nested', taskText(18, 'Corrupt nested', 'Nested advisory fields must be exact.'))
    intake.schedule(nestedStem, 'test')
    const nestedComplete = await waitFor(() => { const r = record(nestedStem); return r.status === 'complete' && r })
    writeFileSync(join(intakeDir, nestedStem + '.json'), JSON.stringify({ ...nestedComplete, likelyAreas: 'ui' }) + '\n')
    assert.equal(intake.snapshot()[nestedStem], undefined)
    intake.reconcile()
    const nestedFailure = record(nestedStem)
    assert.equal(nestedFailure.status, 'failed')
    assert.equal(nestedFailure.errorCode, 'INTAKE_CACHE_INVALID')

    const partialStem = 'TASK_17_partial_record'
    const partialBytes = taskText(17, 'Partial record', 'A torn advisory write must stay isolated.')
    addTask(partialStem, 'Partial record', partialBytes)
    writePrivateBytes(join(intakeDir, partialStem + '.json'), '{"version":1')
    assert.doesNotThrow(() => intake.snapshot())
    assert.equal(intake.snapshot()[partialStem], undefined)
    intake.reconcile()
    const partialFailure = record(partialStem)
    assert.equal(partialFailure.status, 'failed')
    assert.equal(partialFailure.errorCode, 'INTAKE_CACHE_INVALID')
    assert.deepEqual(taskBytes(partialStem), Buffer.from(partialBytes))

    const metadataStem = 'TASK_22_corrupt_metadata'
    const metadataBytes = taskText(22, 'Corrupt metadata', 'Attempt and timestamp bounds are part of the cache contract.')
    addTask(metadataStem, 'Corrupt metadata', metadataBytes)
    const source = intake.sourceState(metadataStem)
    const badAttempt = {
      version: 1, stem: metadataStem, sourceHash: source.sourceHash, createdAt: '2026-07-12T00:00:00.000Z',
      status: 'failed', requestId: 'intake-corrupt-metadata', attempt: 3,
      errorCode: 'MODEL_PROCESS_FAILED', retryable: true
    }
    writePrivateJson(join(intakeDir, metadataStem + '.json'), badAttempt)
    assert.equal(intake.snapshot()[metadataStem], undefined)
    intake.reconcile()
    const boundedAttempt = record(metadataStem)
    assert.equal(boundedAttempt.status, 'failed')
    assert.equal(boundedAttempt.attempt, 1)
    assert.equal(boundedAttempt.errorCode, 'INTAKE_CACHE_INVALID')

    writeFileSync(join(intakeDir, metadataStem + '.json'), JSON.stringify({ ...boundedAttempt, createdAt: 'July 12, 2026' }) + '\n')
    assert.equal(intake.snapshot()[metadataStem], undefined)
    intake.reconcile()
    const canonicalTimestamp = record(metadataStem)
    assert.match(canonicalTimestamp.createdAt, /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/)
    assert.equal(canonicalTimestamp.errorCode, 'INTAKE_CACHE_INVALID')
    assert.deepEqual(taskBytes(metadataStem), Buffer.from(metadataBytes))
  })

  await check('a replaced global owner generation retains scratch and is never released through the old handle', async () => {
    if (!['linux', 'darwin'].includes(process.platform)) return
    const workerFile = join(intakeDir, '.worker.json')
    await waitFor(() => !existsSync(workerFile), 10000)
    const stem = 'TASK_32_foreign_worker_generation'
    addTask(stem, 'Foreign worker generation', taskText(32, 'Foreign worker generation', 'SLOW_MODEL retain scratch after owner replacement.'))
    const queued = intake.schedule(stem, 'foreign-worker-generation')
    const scratch = join(scratchDir, queued.requestId)
    const owned = await waitFor(() => {
      if (!existsSync(workerFile)) return null
      const value = JSON.parse(readFileSync(workerFile, 'utf8'))
      return value.childPid && value
    })
    const foreignToken = 'foreign-worker-token-1234'
    writePrivateJson(workerFile, { ...owned, token: foreignToken })
    await new Promise((resolveWait) => setTimeout(resolveWait, 1800))
    const fenced = record(stem)
    assert.equal(fenced.status, 'checking', 'the displaced worker must not publish its timeout result')
    assert.equal(fenced.requestId, queued.requestId)
    assert.equal(JSON.parse(readFileSync(workerFile, 'utf8')).token, foreignToken,
      'the old handle must not unlink a replacement owner generation')
    assert.equal(existsSync(scratch), true, 'scratch must remain when cleanup ownership changed')
    rmSync(workerFile)
    rmSync(scratch, { recursive: true, force: true })
  })

  await check('a valid creator-sized task persists a bounded context failure instead of losing intake state', async () => {
    const stem = 'TASK_19_storage_limit'
    const prefix = taskText(19, 'Storage limit', '')
    const text = prefix + 'x'.repeat(66 * 1024 - Buffer.byteLength(prefix))
    assert.ok(Buffer.byteLength(text) > 64 * 1024)
    assert.ok(Buffer.byteLength(text) <= 64 * 1024 + 4096)
    addTask(stem, 'Storage limit', text)
    const queued = intake.schedule(stem, 'test')
    assert.equal(queued.status, 'queued')
    const failed = await waitFor(() => { const r = record(stem); return r.status === 'failed' && r })
    assert.equal(failed.errorCode, 'CONTEXT_TOO_LARGE')
    assert.equal(failed.retryable, false)
    assert.equal(intake.snapshot()[stem].status, 'failed')
    assert.deepEqual(taskBytes(stem), Buffer.from(text))
  })

  await check('Windows Job control releases only after authenticated DRAINED and retains an unproved tree', async () => {
    const windowsWrapper = join(root, 'fake-intake-windows-job.mjs')
    writeFileSync(windowsWrapper, `#!/usr/bin/env node
import { readFileSync, unlinkSync, writeSync } from 'node:fs'
import { spawn } from 'node:child_process'
const argv = process.argv.slice(2)
const separator = argv.indexOf('--')
const value = (flag) => argv[argv.indexOf(flag) + 1]
const token = value('--token')
const promptPath = value('--prompt')
const command = argv.slice(separator + 1)
const nonce = process.env.SHALLOW_INTAKE_JOB_NONCE
const control = (line) => writeSync(3, line + '\\n')
let buffer = '', launched = false, bound = false, rejected = false, terminal = false
let child = null, prompt = null, noDrain = false
const rejectUnbound = () => {
  if (bound || rejected || terminal) return
  rejected = true
  if (!child) { terminal = true; control('INTAKE_WINDOWS_JOB_DRAINED ' + nonce); process.exit(72) }
  try { child.kill('SIGKILL') } catch {}
}
process.stdin.setEncoding('utf8')
process.stdin.on('data', (chunk) => {
  buffer += chunk
  let newline
  while ((newline = buffer.indexOf('\\n')) >= 0) {
    const line = buffer.slice(0, newline); buffer = buffer.slice(newline + 1)
    if (!launched) {
      if (line !== 'GO ' + token) process.exit(72)
      launched = true
      prompt = readFileSync(promptPath)
      noDrain = prompt.toString('utf8').includes('WINDOWS_NO_DRAIN')
      child = spawn(command[0], command.slice(1), { stdio: ['pipe', 'inherit', 'inherit'] })
      control('INTAKE_WINDOWS_JOB_READY ' + nonce + ' ' + child.pid)
      child.on('close', (code) => {
        if (terminal) return
        terminal = true
        if (rejected) {
          control('INTAKE_WINDOWS_JOB_DRAINED ' + nonce)
          process.exit(72)
        }
        if (noDrain) {
          control('INTAKE_WINDOWS_JOB_DRAINED ' + nonce + ' trailing-garbage')
          process.exit(code || 0)
        }
        control('INTAKE_WINDOWS_JOB_DRAINED ' + nonce)
        process.exit(code || 0)
      })
    } else if (!bound) {
      if (line !== 'BOUND ' + token + ' ' + nonce + ' ' + child.pid) { rejectUnbound(); continue }
      bound = true
      try { unlinkSync(promptPath) } catch {}
      if (prompt.toString('utf8').includes('WINDOWS_NOISY_STDERR')) process.stderr.write('x'.repeat(8192))
      child.stdin.end(prompt)
    } else if (line === 'TERMINATE' && child) {
      try { child.kill('SIGKILL') } catch {}
    }
  }
})
process.stdin.on('end', () => {
  if (!bound) rejectUnbound()
  else if (child) try { child.kill('SIGKILL') } catch {}
})
`)
    chmodSync(windowsWrapper, 0o755)

    const invalidToken = 'invalid-bound-token-1234'
    const invalidNonce = 'a'.repeat(48)
    const invalidPrompt = join(root, 'invalid-bound-prompt.txt')
    const invalidMarker = join(root, 'invalid-bound-model-started')
    const invalidModel = join(root, 'invalid-bound-model.mjs')
    writeFileSync(invalidPrompt, 'must remain unread by the model\n')
    writeFileSync(invalidModel, `
import { writeFileSync } from 'node:fs'
const marker = process.argv[2]
process.stdin.once('data', () => writeFileSync(marker, 'started\\n'))
setInterval(() => {}, 1000)
`)
    const invalidBound = spawnSync(process.execPath, [windowsWrapper,
      '--token', invalidToken, '--prompt', invalidPrompt, '--', process.execPath, invalidModel, invalidMarker], {
      input: 'GO ' + invalidToken + '\nBOUND ' + invalidToken + ' ' + invalidNonce + ' 1\n',
      stdio: ['pipe', 'pipe', 'pipe', 'pipe'], encoding: 'utf8', timeout: 5000,
      env: { ...process.env, SHALLOW_INTAKE_JOB_NONCE: invalidNonce, SHALLOW_INTAKE_JOB_CONTROL_FD: '3' },
    })
    assert.equal(invalidBound.status, 72, String(invalidBound.stderr || invalidBound.stdout || 'invalid BOUND did not fail'))
    assert.equal(existsSync(invalidMarker), false, 'an invalid BOUND must not release model side effects')
    assert.equal(existsSync(invalidPrompt), true, 'an invalid BOUND must not consume the prompt')
    const invalidControl = String(invalidBound.output[3] || '').trim().split('\n')
    assert.match(invalidControl[0], new RegExp('^INTAKE_WINDOWS_JOB_READY ' + invalidNonce + ' [1-9][0-9]*$'))
    assert.equal(invalidControl.at(-1), 'INTAKE_WINDOWS_JOB_DRAINED ' + invalidNonce)

    process.env.SHALLOW_INTAKE_TEST_WINDOWS_JOB = '1'
    process.env.SHALLOW_INTAKE_WINDOWS_JOB_WRAPPER = windowsWrapper
    process.env.SHALLOW_INTAKE_WINDOWS_JOB_PYTHON = process.execPath

    const workerFile = join(intakeDir, '.worker.json')
    const drainedStem = 'TASK_23_windows_drained'
    addTask(drainedStem, 'Windows drained', taskText(23, 'Windows drained', 'WINDOWS_NOISY_STDERR authenticated drain proof.'))
    intake.schedule(drainedStem, 'windows-job-test')
    await waitFor(() => record(drainedStem).status === 'complete')
    await waitFor(() => !existsSync(workerFile))
    assert.equal(record(drainedStem).status, 'complete', 'a fast model exit after exact BOUND must still publish')

    const startsBefore = readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').length
    const retainedStem = 'TASK_24_windows_unverified'
    addTask(retainedStem, 'Windows unverified', taskText(24, 'Windows unverified', 'WINDOWS_NO_DRAIN leader exit is not proof.'))
    intake.schedule(retainedStem, 'windows-job-test')
    await waitFor(() => record(retainedStem).status === 'complete')
    await waitFor(() => existsSync(workerFile))
    const startsAfterRetained = readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').length
    assert.equal(startsAfterRetained, startsBefore + 1)

    const blockedStem = 'TASK_25_windows_blocked'
    addTask(blockedStem, 'Windows blocked', taskText(25, 'Windows blocked', 'Must wait for durable tree proof.'))
    intake.schedule(blockedStem, 'windows-job-test')
    await new Promise((resolveWait) => setTimeout(resolveWait, 350))
    assert.equal(record(blockedStem).status, 'queued')
    assert.equal(readFileSync(join(intakeDir, 'fake-starts.log'), 'utf8').trim().split('\n').length, startsAfterRetained)
    assert.equal(existsSync(workerFile), true)

    delete process.env.SHALLOW_INTAKE_TEST_WINDOWS_JOB
    delete process.env.SHALLOW_INTAKE_WINDOWS_JOB_WRAPPER
    delete process.env.SHALLOW_INTAKE_WINDOWS_JOB_PYTHON
  })

  console.log(`shallow-intake: ${checks} checks passed`)
} finally {
  intake.killAll()
  await new Promise((resolve) => setTimeout(resolve, 1100))
  rmSync(root, { recursive: true, force: true })
  rmSync(scratchAuthorityRoot, { recursive: true, force: true })
}
