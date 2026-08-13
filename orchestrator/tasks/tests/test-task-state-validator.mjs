#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { spawnSync } from 'node:child_process'
import {
  lstatSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync,
  symlinkSync, unlinkSync, writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, isAbsolute, join, relative } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import {
  inspectOutcomeFigmaMeta,
  installOutcomeDraft,
  logicalTaskText,
  outcomeAppendixStart as sharedOutcomeAppendixStart,
  outcomeShapeError,
  parseFigmaEnabledConfig,
} from '../../figma/scripts/outcome-shape.mjs'
import { outcomeAppendixStatus as figmaOutcomeAppendixStatus } from '../../figma/scripts/_util.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const TASKS_DIR = join(HERE, '..')
const CLI = join(TASKS_DIR, 'validate-task-state.mjs')
const OUTCOME_SHAPE = join(TASKS_DIR, '..', 'contracts', 'outcome-shape.json')
const RUNTIME_INTEGRITY = join(TASKS_DIR, '..', 'site', 'server', 'runtime-integrity.js')
const CANONICAL_PROJECT_ROOT = join(TASKS_DIR, '..', '..')
const core = createRequire(import.meta.url)('../task-state-core.cjs')
const taskSource = createRequire(import.meta.url)('../task-source-contract.cjs')
const apiWorkPackage = createRequire(import.meta.url)('../api-work-package-contract.cjs')
const durableCas = createRequire(import.meta.url)('../durable-cas-contract.cjs')
const roots = []
let checks = 0
const failures = []

function check(name, fn) {
  checks++
  try {
    fn()
    console.log(`PASS ${name}`)
  } catch (error) {
    failures.push({ name, error })
    console.error(`FAIL ${name}\n${error && error.stack || error}`)
  }
}

function emptyIndex() {
  return {
    version: 2,
    generatedAt: '1970-01-01T00:00:00Z',
    backlog: [], pending: [], todo: [], done: [],
  }
}

function makeProject(prefix = 'task-state-validator-') {
  const root = mkdtempSync(join(tmpdir(), prefix))
  roots.push(root)
  const tasks = join(root, 'orchestrator', 'tasks')
  for (const column of core.COLUMNS) mkdirSync(join(tasks, column), { recursive: true })
  writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify(emptyIndex(), null, 2) + '\n')
  return { root, tasks }
}

function numberOf(stem) {
  const match = /^TASK_([1-9][0-9]*)_/.exec(stem)
  assert.ok(match, `test fixture stem is invalid: ${stem}`)
  return Number(match[1])
}

function backlogDoc(stem, title = 'Backlog task') {
  return `# TASK ${numberOf(stem)} — ${title}\n\n${taskSource.render(manualSource(stem))}\n\nInitial request.\n`
}

function withSource(document, provenance) {
  const parsed = taskSource.parse(document)
  assert.equal(parsed.valid, true)
  return document.slice(0, parsed.start) + taskSource.render(provenance) + document.slice(parsed.end)
}

function withApiPackage(document, metadata) {
  const parsed = taskSource.parse(document)
  assert.equal(parsed.valid, true)
  const provenance = {
    kind: 'api',
    type: 'api-work-package',
    ref: 'api:package:' + metadata.packageId,
    fingerprint: taskSource.sha256('api-package\0' + metadata.packageId),
  }
  return document.slice(0, parsed.start) +
    taskSource.render(provenance) + '\n\n' +
    apiWorkPackage.render(metadata) +
    document.slice(parsed.end)
}

function manualSource(stem) {
  return {
    kind: 'manual', type: 'manual', ref: stem,
    fingerprint: taskSource.sha256('source\0' + stem)
  }
}

function pendingDoc(stem, { type = 'text', options = '' } = {}) {
  return [
    '---',
    `forTask: ${stem}`,
    'createdAt: 2026-07-13T08:00:00Z',
    'updatedAt: 2026-07-13T08:01:00Z',
    'round: 1',
    'gapCount: 1',
    'prevGapCount: 2',
    '---',
    '',
    '## Q1 — What should the implementation preserve?',
    '',
    `**Type**: ${type}`,
    ...(options ? [`**Options**: ${options}`] : []),
    '',
    '### Answer',
    '',
  ].join('\n')
}

function todoDoc(stem, { dependency = null, automated = 'Run `node test/contract.mjs`.', title = 'Runnable task' } = {}) {
  return [
    `# TASK ${numberOf(stem)} — ${title}`,
    '',
    taskSource.render(manualSource(stem)),
    '',
    '## Goal',
    '',
    'Implement the requested behavior.',
    '',
    '## Inputs',
    '',
    '- Existing repository contracts.',
    ...(dependency ? ['', '## Depends on (optional)', '', `- ${dependency}`] : []),
    '',
    '## Acceptance',
    '',
    '### Automated',
    '',
    `- ${automated}`,
    '',
    '### Manual',
    '',
    '- Inspect the rendered result.',
    '',
    '## Out of scope',
    '',
    '- Unrelated refactors.',
    '',
  ].join('\n')
}

function doneDoc(stem, { status = 'completed', completedAt = '2026-07-13T09:00:00Z', acceptance = '`test/contract.mjs` — verified — Passed.', file = '`src/feature.js` — modified', dependency = null } = {}) {
  return [
    todoDoc(stem, { title: 'Completed task', dependency }).trimEnd(),
    '',
    '---',
    '',
    '## Outcome',
    '',
    `**Status**: ${status}`,
    `**Completed at**: ${completedAt}`,
    '**Reviewer**: codex',
    '**Review iterations**: 1',
    '',
    '### Build gates',
    '',
    '- `node test/contract.mjs` — pass',
    '',
    '### Runtime verify',
    '',
    '- Gate: skipped (no runtime-observable change)',
    '- Result: n/a — no runtime change',
    '',
    '### Acceptance trace',
    '',
    `- ${acceptance}`,
    '',
    '### Caveats',
    '',
    '- none',
    '',
    '### Follow-ups',
    '',
    '- none',
    '',
    '### Files touched',
    '',
    `- ${file}`,
    '',
  ].join('\n')
}

function artifactPath(project, column, stem) {
  return join(project.tasks, column, stem + (column === 'pending' ? '.questions.md' : '.md'))
}

function writeArtifact(project, column, stem, contents) {
  writeFileSync(artifactPath(project, column, stem), contents)
}

function clearStem(project, stem) {
  for (const column of core.COLUMNS) {
    try { unlinkSync(artifactPath(project, column, stem)) }
    catch (error) { if (!error || error.code !== 'ENOENT') throw error }
  }
}

function setState(project, state, stem) {
  clearStem(project, stem)
  if (state === 'backlog' || state === 'pending') writeArtifact(project, 'backlog', stem, backlogDoc(stem))
  if (state === 'pending') writeArtifact(project, 'pending', stem, pendingDoc(stem))
  if (state === 'todo') writeArtifact(project, 'todo', stem, todoDoc(stem))
  if (state === 'done') writeArtifact(project, 'done', stem, doneDoc(stem))
}

function validate(project, options = {}) {
  return core.validateTaskState({
    repoRoot: project.root,
    tasksDir: project.tasks,
    outcomeShapePath: OUTCOME_SHAPE,
    includeRuntime: false,
    ...options,
  })
}

function codes(result) {
  return result.findings.map((item) => item.code)
}

function assertCode(result, code) {
  assert.ok(codes(result).includes(code), `expected ${code}; got ${codes(result).join(', ') || 'no findings'}`)
}

function assertValidState(project, stem, state) {
  const result = validate(project, { stem, expect: state })
  assert.equal(result.ok, true, JSON.stringify(result.findings, null, 2))
  assert.equal(result.observedState, state)
  assert.equal(result.expectedState, state)
  assert.match(result.snapshotHash, /^sha256:[a-f0-9]{64}$/)
  assert.match(result.sourceRevision, /^sha256:[a-f0-9]{64}$/)
  return result
}

function cliEnv(project) {
  return {
    ...process.env,
    ORCHESTRATOR_PROJECT_ROOT: project.root,
    ORCHESTRATOR_TASKS_DIR: project.tasks,
    ORCHESTRATOR_OUTCOME_SHAPE_PATH: OUTCOME_SHAPE,
  }
}

function runCli(project, args, input = undefined, env = {}) {
  const childEnv = { ...cliEnv(project), ...env }
  for (const [name, value] of Object.entries(childEnv)) {
    if (value === null || value === undefined) delete childEnv[name]
  }
  return spawnSync(process.execPath, [CLI, ...args], {
    cwd: project.root,
    env: childEnv,
    input,
    encoding: 'utf8',
    maxBuffer: 4 * 1024 * 1024,
  })
}

function parseCliJson(result) {
  assert.notEqual(result.stdout, '', `CLI emitted no JSON; stderr: ${result.stderr}`)
  return JSON.parse(result.stdout)
}

function parseObservation(result) {
  const line = result.stderr.split('\n').find((entry) => entry.startsWith('[task-state] '))
  assert.ok(line, `CLI emitted no observation; stderr: ${result.stderr}`)
  return JSON.parse(line.slice('[task-state] '.length))
}

function publishFreshIndex(project, generatedAt = '2026-07-13T10:00:00Z') {
  const result = validate(project)
  assert.equal(result.ok, true, JSON.stringify(result.findings, null, 2))
  const index = core.deriveIndex(result._model, generatedAt)
  writeFileSync(join(project.tasks, 'INDEX.json'), JSON.stringify(index, null, 2) + '\n')
  return index
}

function hashFile(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex')
}

function treeSnapshot(root) {
  const rows = []
  function visit(absolute) {
    const stat = lstatSync(absolute)
    const rel = relative(root, absolute).split('\\').join('/') || '.'
    const row = { path: rel, mode: stat.mode, size: stat.size, mtimeMs: stat.mtimeMs }
    if (stat.isFile()) row.hash = hashFile(absolute)
    else if (stat.isDirectory()) row.children = readdirSync(absolute).sort()
    else row.kind = 'special'
    rows.push(row)
    if (stat.isDirectory()) for (const name of readdirSync(absolute).sort()) visit(join(absolute, name))
  }
  visit(root)
  return rows
}

function stableEnvelope(value) {
  const copy = structuredClone(value)
  if (copy.stats) delete copy.stats.durationMs
  return copy
}

function durableCasName(digit = '1') {
  assert.match(digit, /^[0-9a-f]$/)
  return durableCas.PREFIX + [digit.repeat(16), digit.repeat(16), digit.repeat(16)].join('-')
}

function fileProof(file) {
  const bytes = readFileSync(file)
  const stat = lstatSync(file, { bigint: true })
  return {
    dev: String(stat.dev),
    ino: String(stat.ino),
    mode: Number(stat.mode & 0o7777n),
    size: Number(stat.size),
    mtimeNs: String(stat.mtimeNs),
    ctimeNs: String(stat.ctimeNs),
    hash: core.sha256(bytes),
  }
}

function ownedLockGeneration(file) {
  const bytes = readFileSync(file)
  const stat = lstatSync(file, { bigint: true })
  const proof = {
    dev: String(stat.dev), ino: String(stat.ino), kind: 'file', mode: Number(stat.mode),
    size: Number(stat.size), mtimeNs: String(stat.mtimeNs), hash: core.sha256(bytes),
  }
  const hash = core.lockGenerationHash(proof)
  assert.match(hash, /^sha256:[a-f0-9]{64}$/)
  return { proof, hash }
}

function writeCanonicalLock(project, stem, stage = 'orchestrator') {
  const locks = join(project.root, 'orchestrator', '.cache', 'tasks', 'locks')
  mkdirSync(locks, { recursive: true })
  const startedAt = new Date().toISOString()
  const record = {
    version: 1, stem, stage, runId: `run-finalize-${numberOf(stem)}`,
    sessionId: 'ws-' + 'a'.repeat(32), startedAt,
    owner: {
      id: 'task-state-validator', kind: 'direct', pid: process.pid,
      hostname: 'validator.fixture', processStartId: null, startedAt,
    },
  }
  assert.equal(core.canonicalLockV1(record, stem), true)
  const file = join(locks, stem + '.json')
  writeFileSync(file, JSON.stringify(record, null, 2) + '\n')
  return { locks, file, record }
}

function createDurableCas(parent, targetName, {
  digit = '1', candidate = Buffer.from('candidate bytes\n'), source = null,
  expectedFile = null, owner = 'task-state-validator', manifest = true,
} = {}) {
  const operation = join(parent, durableCasName(digit))
  mkdirSync(operation, { recursive: true })
  if (candidate !== null) writeFileSync(join(operation, 'candidate'), candidate)
  if (source !== null) writeFileSync(join(operation, 'source'), source)
  if (manifest) {
    const proofFile = source !== null ? join(operation, 'source') : expectedFile || join(parent, targetName)
    const proof = fileProof(proofFile)
    const value = {
      version: 1,
      targetName,
      owner,
      expectedProof: proof,
      candidateHash: core.sha256(candidate),
      maxBytes: Math.max(1024, proof.size, candidate.length),
    }
    writeFileSync(join(operation, 'manifest.json'), durableCas.canonicalManifest(value))
  }
  return operation
}

try {
  check('task ids use one canonical safe-integer decimal spelling', () => {
    assert.equal(core.safeIntegerId('TASK_1_ok'), 1)
    assert.equal(core.safeIntegerId('TASK_01_leading_zero'), null)
    assert.equal(core.safeIntegerId('TASK_9007199254740991_max'), Number.MAX_SAFE_INTEGER)
    assert.equal(core.safeIntegerId('TASK_9007199254740992_unsafe'), null)
  })

  check('shared figmaEnabled parser admits one canonical physical-line credential', () => {
    assert.equal(parseFigmaEnabledConfig('---\r\nfigmaEnabled:\ttrue  \r\n---\r\n'), true)
    assert.equal(parseFigmaEnabledConfig('figmaEnabled: false\n'), false)
    for (const malformed of [
      'figmaEnabled:\ntrue\n',
      'figmaEnabled: TRUE\n',
      'figmaEnabled:true\n',
      'figmaEnabled: true\nfigmaEnabled: false\n',
      ' figmaEnabled: true\n',
      'figmaEnabled : true\n',
      'figmaEnabled:\vtrue\n',
    ]) assert.throws(() => parseFigmaEnabledConfig(malformed), /canonical physical-line/)
  })

  check('canonical absent, backlog, pending, todo, and done states validate', () => {
    const project = makeProject()
    setState(project, 'backlog', 'TASK_1_backlog')
    setState(project, 'pending', 'TASK_2_pending')
    setState(project, 'todo', 'TASK_3_todo')
    setState(project, 'done', 'TASK_4_done')

    assertValidState(project, 'TASK_5_absent', 'absent')
    assertValidState(project, 'TASK_1_backlog', 'backlog')
    assertValidState(project, 'TASK_2_pending', 'pending')
    assertValidState(project, 'TASK_3_todo', 'todo')
    assertValidState(project, 'TASK_4_done', 'done')

    const all = validate(project)
    assert.equal(all.ok, true, JSON.stringify(all.findings, null, 2))
    assert.equal(all.scope, 'all')
    assert.equal(all.stats.tasks, 4)
    assert.equal(all.stats.files, 5, 'pending is a two-file logical state')
  })

  check('single-stem validation reads only the transitive dependency closure with deterministic budget stats', () => {
    const project = makeProject()
    const target = 'TASK_101_budget_target'
    const middle = 'TASK_102_budget_middle'
    const accepted = 'TASK_103_budget_done'
    const unrelated = 'TASK_104_unrelated_body'
    writeArtifact(project, 'todo', target, todoDoc(target, { dependency: middle }))
    writeArtifact(project, 'todo', middle, todoDoc(middle, { dependency: accepted }))
    writeArtifact(project, 'done', accepted, doneDoc(accepted))
    writeArtifact(project, 'backlog', unrelated, backlogDoc(unrelated))
    for (let index = 0; index < 256; index++) {
      const stem = `TASK_${200 + index}_inventory_only_${index}`
      writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    }
    publishFreshIndex(project)

    // The unrelated row is now stale and its body malformed. Scoped INDEX
    // equivalence must ignore that body while global validation still sees it.
    writeArtifact(project, 'backlog', unrelated, 'not a canonical task document\n')
    const first = validate(project, { stem: target, checkIndex: true })
    assert.equal(first.ok, true, JSON.stringify(first.findings, null, 2))
    assert.equal(first.indexStatus, 'fresh')
    assert.equal(first.stats.scanMode, 'stem-closure')
    assert.equal(first.stats.tasks, 260, 'all canonical names remain globally inventoried')
    assert.equal(first.stats.taskRelatedEntries, 260)
    assert.equal(first.stats.files, 3)
    assert.equal(first.stats.taskBodyReads, 3)
    assert.deepEqual(Array.from(first._model.loadedStems).sort(), [accepted, middle, target].sort())
    assert.equal(first._model.snapshotInputs.some((item) => item.kind === 'file' && item.path.endsWith(`/${unrelated}.md`)), false)
    assert.equal(first._model.snapshotInputs.some((item) => item.kind === 'file' && item.path.endsWith('/INDEX.json')), true)

    const cli = runCli(project, ['--stem', target, '--check-index', '--json'])
    assert.equal(cli.status, 0, cli.stderr + cli.stdout)
    const cliEnvelope = parseCliJson(cli)
    assert.equal(cliEnvelope.stats.scanMode, 'stem-closure')
    assert.equal(cliEnvelope.stats.taskBodyReads, 3,
      'the production CLI must retain the same bounded closure read budget')

    writeArtifact(project, 'backlog', unrelated, 'different unrelated malformed bytes\n')
    const second = validate(project, { stem: target, checkIndex: true })
    assert.equal(second.ok, true, JSON.stringify(second.findings, null, 2))
    assert.equal(second.snapshotHash, first.snapshotHash,
      'unread unrelated body bytes must not perturb a scoped verdict fence')
    assert.deepEqual({ ...second.stats, durationMs: 0 }, { ...first.stats, durationMs: 0 })

    const global = validate(project, { checkIndex: true })
    assert.equal(global.ok, false)
    assert.equal(global.stats.scanMode, 'full')
    assert.equal(global.stats.taskBodyReads, 260)
    assertCode(global, 'TASK_HEADING_NONCANONICAL')
    assertCode(global, 'INDEX_STALE')
  })

  check('anchored scoped run closure shares CommonMark heading and fence semantics with admission', () => {
    const action = (project, stem) => core.validateAction({
      action: 'run', stem, repoRoot: project.root, tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false, checkIndex: true,
    })
    const assertClosure = (result, expected, context) => {
      assert.equal(result.ok, true, `${context}: ${JSON.stringify(result.findings, null, 2)}`)
      assert.equal(result.indexStatus, 'fresh', context)
      assert.equal(result.stats.scanMode, 'stem-closure', context)
      assert.deepEqual(Array.from(result._model.loadedStems).sort(), expected.slice().sort(), context)
      assert.equal(result.stats.taskBodyReads, expected.length, context)
    }

    // Existing done artifacts make an under-read observable: the JS validator
    // must never discover a valid edge outside the Python boundary's frozen
    // closure. All four legal ATX indentation widths and closing hashes route
    // through the same accepted-dependency admission.
    for (let indent = 0; indent <= 3; indent++) {
      const project = makeProject()
      const target = `TASK_${500 + indent * 2}_commonmark_closure_${indent}`
      const dependency = `TASK_${501 + indent * 2}_commonmark_done_${indent}`
      writeArtifact(project, 'todo', target, todoDoc(target, { dependency }).replace(
        '## Depends on (optional)', `${' '.repeat(indent)}## Depends on (optional) ##`,
      ))
      writeArtifact(project, 'done', dependency, doneDoc(dependency))
      publishFreshIndex(project)
      assertClosure(action(project, target), [target, dependency], `ATX indent ${indent}`)
    }

    // Four spaces and a leading tab are indented code, not top-level H2s.
    // The otherwise valid done artifact stays outside the bounded closure.
    for (const [index, prefix] of ['    ', '\t'].entries()) {
      const project = makeProject()
      const target = `TASK_${510 + index * 2}_indented_heading_${index}`
      const decoy = `TASK_${511 + index * 2}_indented_heading_done_${index}`
      writeArtifact(project, 'todo', target, todoDoc(target, { dependency: decoy }).replace(
        '## Depends on (optional)', `${prefix}## Depends on (optional) ##`,
      ))
      writeArtifact(project, 'done', decoy, doneDoc(decoy))
      publishFreshIndex(project)
      assertClosure(action(project, target), [target], `non-heading prefix ${JSON.stringify(prefix)}`)
    }

    // Legal fences (zero through three spaces) hide dependency-shaped decoys
    // from both closure discovery and final admission.
    for (const [delimiterIndex, delimiter] of ['````', '~~~~'].entries()) {
      for (let indent = 0; indent <= 3; indent++) {
        const project = makeProject()
        const base = 520 + delimiterIndex * 20 + indent * 3
        const target = `TASK_${base}_fenced_closure_${delimiterIndex}_${indent}`
        const hidden = `TASK_${base + 1}_fenced_hidden_${delimiterIndex}_${indent}`
        const visible = `TASK_${base + 2}_fenced_visible_${delimiterIndex}_${indent}`
        const fence = ' '.repeat(indent) + delimiter
        const body = todoDoc(target, { dependency: visible }).replace(
          '\n## Depends on (optional)', [
            '', `${fence}markdown`, '## Depends on (optional) ##', `- ${hidden}`, fence,
            '', '## Depends on (optional)',
          ].join('\n'),
        )
        writeArtifact(project, 'todo', target, body)
        writeArtifact(project, 'done', hidden, doneDoc(hidden))
        writeArtifact(project, 'done', visible, doneDoc(visible))
        publishFreshIndex(project)
        assertClosure(action(project, target), [target, visible],
          `${delimiter[0] === '`' ? 'backtick' : 'tilde'} fence indent ${indent}`)
      }
    }

    // Unlike backtick fences, tilde fences may contain backticks in their
    // info string. Such a legal fence still masks its dependency decoy.
    const tildeProject = makeProject()
    const tildeTarget = 'TASK_560_tilde_info_fence'
    const tildeHidden = 'TASK_561_tilde_info_hidden'
    const tildeBody = todoDoc(tildeTarget).replace(
      '\n## Acceptance', [
        '', '~~~audit`allowed', '## Depends on (optional) ##', `- ${tildeHidden}`, '~~~',
        '', '## Acceptance',
      ].join('\n'),
    )
    writeArtifact(tildeProject, 'todo', tildeTarget, tildeBody)
    writeArtifact(tildeProject, 'done', tildeHidden, doneDoc(tildeHidden))
    publishFreshIndex(tildeProject)
    assertClosure(action(tildeProject, tildeTarget), [tildeTarget], 'tilde fence info backtick')

    // Four-space/tab delimiters and a backtick opener whose info string
    // contains a backtick are not fences. The following real dependency must
    // therefore be included before the snapshot is handed to JS admission.
    for (const [index, opener] of [
      '    ````markdown', '\t````markdown', '```audit`invalid',
    ].entries()) {
      const project = makeProject()
      const target = `TASK_${540 + index * 2}_invalid_fence_${index}`
      const dependency = `TASK_${541 + index * 2}_invalid_fence_done_${index}`
      const body = todoDoc(target, { dependency }).replace(
        '\n## Depends on (optional)', `\n${opener}\n## Depends on (optional)`,
      )
      writeArtifact(project, 'todo', target, body)
      writeArtifact(project, 'done', dependency, doneDoc(dependency))
      publishFreshIndex(project)
      assertClosure(action(project, target), [target, dependency], `invalid fence ${JSON.stringify(opener)}`)
    }

    // An overindented delimiter cannot close a real fence. The hidden edge
    // remains masked until the valid closer, while the later visible edge is
    // the only dependency admitted into the scoped closure.
    const project = makeProject()
    const target = 'TASK_550_overindented_closer'
    const hidden = 'TASK_551_overindented_hidden'
    const visible = 'TASK_552_overindented_visible'
    const body = todoDoc(target, { dependency: visible }).replace(
      '\n## Depends on (optional)', [
        '', '````markdown', '    ````', '## Depends on (optional) ##', `- ${hidden}`, '````',
        '', '## Depends on (optional)',
      ].join('\n'),
    )
    writeArtifact(project, 'todo', target, body)
    writeArtifact(project, 'done', hidden, doneDoc(hidden))
    writeArtifact(project, 'done', visible, doneDoc(visible))
    publishFreshIndex(project)
    assertClosure(action(project, target), [target, visible], 'overindented fence closer')
  })

  check('anchored ATX closing-hash scan stays bounded on a long heading without closing hashes', () => {
    const boundary = join(TASKS_DIR, 'anchored-task-fs.py')
    const probe = spawnSync('python3', ['-c', [
      'import importlib.util, sys',
      "spec = importlib.util.spec_from_file_location('anchored_task_fs_probe', sys.argv[1])",
      'module = importlib.util.module_from_spec(spec)',
      'spec.loader.exec_module(module)',
      "assert module.parse_atx_heading_line('## ' + (' ' * (256 * 1024)) + 'x') == (2, 'x')",
    ].join('\n'), boundary], { env: { ...process.env, PYTHONDONTWRITEBYTECODE: '1' }, encoding: 'utf8', timeout: 5000 })
    assert.equal(probe.error, undefined, String(probe.error || ''))
    assert.equal(probe.status, 0, probe.stderr || probe.stdout)
  })

  check('Outcome fields stay physical-line-bound and missing-field scans remain linear', () => {
    const shape = JSON.parse(readFileSync(OUTCOME_SHAPE, 'utf8'))
    const stem = 'TASK_570_outcome_field_linearity'
    const splitStatus = doneDoc(stem).replace('**Status**: completed', '**Status**\n: completed')
    const splitCore = core.parseOutcome(splitStatus, shape)
    assert.equal(splitCore.valid, false)
    assert.ok(splitCore.errors.includes('missing-field:Status'), JSON.stringify(splitCore.errors))
    assert.match(outcomeShapeError(splitStatus, { shape }), /exactly one `\*\*Status\*\*:` field; found 0/)
    assert.equal(figmaOutcomeAppendixStatus(splitStatus), '')

    const horizontalPadding = doneDoc(stem).replace(
      '**Status**: completed',
      '   **Status**\t:\tcompleted\t',
    )
    assert.equal(core.parseOutcome(horizontalPadding, shape).valid, true)
    assert.equal(outcomeShapeError(horizontalPadding, { shape }), null)
    assert.equal(figmaOutcomeAppendixStatus(horizontalPadding), 'completed')

    const corePath = join(TASKS_DIR, 'task-state-core.cjs')
    const figmaOutcomePath = join(TASKS_DIR, '..', 'figma', 'scripts', 'outcome-shape.mjs')
    const probe = spawnSync(process.execPath, ['-e', [
      "const fs = require('node:fs')",
      "const { pathToFileURL } = require('node:url')",
      `const core = require(${JSON.stringify(corePath)})`,
      `const shape = JSON.parse(fs.readFileSync(${JSON.stringify(OUTCOME_SHAPE)}, 'utf8'))`,
      "const sections = shape.headings.map((heading) => '### ' + heading + '\\n- none\\n').join('\\n')",
      "const text = '---\\n\\n## Outcome\\n' + '\\n'.repeat(100000) + sections",
      ';(async () => {',
      `  const figma = await import(pathToFileURL(${JSON.stringify(figmaOutcomePath)}).href)`,
      '  const parsed = core.parseOutcome(text, shape)',
      "  if (parsed.valid || !parsed.errors.includes('missing-field:Status')) throw new Error('core missing-field verdict changed')",
      '  const error = figma.outcomeShapeError(text, { shape })',
      "  if (!/exactly one `\\*\\*Status\\*\\*:` field; found 0/.test(String(error))) throw new Error('Figma missing-field verdict changed: ' + error)",
      '})().catch((error) => { console.error(error && error.stack || error); process.exitCode = 1 })',
    ].join('\n')], { encoding: 'utf8', timeout: 3000 })
    assert.equal(probe.error, undefined, String(probe.error || ''))
    assert.equal(probe.status, 0, probe.stderr || probe.stdout)
  })

  check('Figma Outcome helpers share the canonical structural anchor without trimming authored content', () => {
    const shape = JSON.parse(readFileSync(OUTCOME_SHAPE, 'utf8'))
    const stem = 'TASK_571_outcome_structural_parity'
    const canonical = doneDoc(stem)
    const anchor = '\n---\n\n## Outcome'
    const withFencedDecoy = canonical.replace(anchor, [
      '', '````markdown', '---', '## Outcome', '### Execution log', '- Figma meta: fenced-forgery', '````', anchor,
    ].join('\n'))
    assert.equal(core.parseOutcome(withFencedDecoy, shape).valid, true)
    assert.equal(outcomeShapeError(withFencedDecoy, { shape }), null)
    assert.equal(sharedOutcomeAppendixStart(withFencedDecoy), withFencedDecoy.lastIndexOf('\n---\n') + 1)

    const unanchored = canonical.replace(anchor, '\n---\n\nprose between anchor and heading\n\n## Outcome')
    assert.equal(core.parseOutcome(unanchored, shape).valid, false)
    assert.match(outcomeShapeError(unanchored, { shape }), /anchored directly/)
    assert.equal(sharedOutcomeAppendixStart(unanchored), -1)

    const authoredTail = todoDoc(stem).trimEnd() + [
      '', '', '````markdown', '---', '## Outcome', '### Execution log', '- Figma meta: inert-example', '````', '',
      'Authored suffix that must survive Outcome installation.', '',
    ].join('\n')
    const draft = canonical.slice(canonical.lastIndexOf('\n---\n') + 1)
    const installed = installOutcomeDraft(authoredTail, draft)
    assert.ok(installed.startsWith(authoredTail.trimEnd() + '\n\n---\n'), 'base task body was truncated')
    assert.match(installed, /Authored suffix that must survive Outcome installation\./)
    assert.equal(outcomeShapeError(installed, { shape }), null)
    assert.equal(core.parseOutcome(installed, shape).valid, true)

    const draftWithFencedExample = draft.replace('### Caveats\n\n- none', [
      '### Caveats', '', '- The audit includes this inert example:', '', '````markdown', '---',
      '## Outcome', '````', '', '- none',
    ].join('\n'))
    const installedWithFencedExample = installOutcomeDraft(todoDoc(stem), draftWithFencedExample)
    assert.match(installedWithFencedExample, /````markdown\n---\n## Outcome\n````/)
    assert.match(outcomeShapeError(installedWithFencedExample, { shape }), /caveats/)
    assert.equal(core.parseOutcome(installedWithFencedExample, shape).valid, false)

    const withRealDigest = canonical.replace('### Files touched', [
      '### Execution log', '', '- Figma meta: canonical-digest', '', '### Files touched',
    ].join('\n')) + [
      '', '````markdown', '---', '## Outcome', '### Execution log', '- Figma meta: fenced-forgery', '````', '',
    ].join('\n')
    const inspected = inspectOutcomeFigmaMeta(withRealDigest)
    assert.equal(inspected.hasOutcome, true)
    assert.deepEqual(inspected.executionLines.map((row) => row.line.trim()), ['- Figma meta: canonical-digest'])
    const logical = logicalTaskText(withRealDigest)
    assert.doesNotMatch(logical, /Figma meta: canonical-digest/)
    assert.match(logical, /Figma meta: fenced-forgery/)

    const crlf = '\uFEFF' + canonical.replace(/\n/g, '\r\n')
    assert.equal(sharedOutcomeAppendixStart(crlf), -1)
    assert.match(outcomeShapeError(crlf, { shape }), /canonical UTF-8.*LF line endings/)
    const crOnly = '\uFEFF' + withRealDigest.replace(/\n/g, '\r')
    assert.equal(sharedOutcomeAppendixStart(crOnly), -1)
    assert.equal(inspectOutcomeFigmaMeta(crOnly).hasOutcome, false)
  })

  check('scoped INDEX equivalence catches target drift and global identity collisions without unrelated body reads', () => {
    const project = makeProject()
    const target = 'TASK_300_index_target'
    const unrelated = 'TASK_301_index_unrelated'
    writeArtifact(project, 'todo', target, todoDoc(target))
    writeArtifact(project, 'backlog', unrelated, backlogDoc(unrelated))
    publishFreshIndex(project)

    writeArtifact(project, 'todo', target, todoDoc(target, { title: 'Changed target row' }))
    const stale = validate(project, { stem: target, checkIndex: true })
    assert.equal(stale.ok, false)
    assertCode(stale, 'INDEX_STALE')
    assert.equal(stale.stats.taskBodyReads, 1)
    assert.equal(stale._model.snapshotInputs.some((item) => item.kind === 'file' && item.path.endsWith(`/${unrelated}.md`)), false)

    publishFreshIndex(project)
    const indexPath = join(project.tasks, 'INDEX.json')
    const index = JSON.parse(readFileSync(indexPath, 'utf8'))
    index.todo = index.todo.filter((row) => row.stem !== target)
    writeFileSync(indexPath, JSON.stringify(index, null, 2) + '\n')
    const missing = validate(project, { stem: target, checkIndex: true })
    assert.equal(missing.ok, false)
    assertCode(missing, 'INDEX_STALE')

    publishFreshIndex(project)
    const collision = 'TASK_300_global_collision'
    writeArtifact(project, 'backlog', collision, backlogDoc(collision))
    const blocked = validate(project, { stem: target, checkIndex: true })
    assert.equal(blocked.ok, false)
    assertCode(blocked, 'TASK_NUMBER_CONFLICT')
    assert.equal(blocked.stats.taskBodyReads, 1,
      'the colliding filename must block globally without reading its unrelated body')
  })

  check('proposal validation follows dependencies from the proposed bytes, not stale on-disk content', () => {
    const project = makeProject()
    const target = 'TASK_302_proposal_closure'
    const dependency = 'TASK_303_proposal_dependency'
    const unrelated = 'TASK_304_proposal_unrelated'
    writeArtifact(project, 'backlog', target, backlogDoc(target))
    writeArtifact(project, 'done', dependency, doneDoc(dependency))
    writeArtifact(project, 'backlog', unrelated, backlogDoc(unrelated))
    const proposal = Buffer.from([
      backlogDoc(target, 'Proposal with dependency').trimEnd(),
      '', '## Depends on (optional)', '', `- ${dependency}`, '',
    ].join('\n'))
    const result = validate(project, {
      stem: target,
      proposal: { stem: target, state: 'backlog', bytes: proposal },
    })
    assert.equal(result.ok, true, JSON.stringify(result.findings, null, 2))
    assert.deepEqual(Array.from(result._model.loadedStems).sort(), [dependency, target].sort())
    assert.equal(result.stats.taskBodyReads, 2)
    assert.equal(result._model.snapshotInputs.some((item) => item.kind === 'file' && item.path.endsWith(`/${unrelated}.md`)), false)
  })

  check('lifecycle proposals preserve the exact Source provenance envelope', () => {
    const project = makeProject()
    const stem = 'TASK_305_source_immutable'
    const original = taskSource.manualForIntent('intent-source-original', 'manual', 'intent-source-original')
    const changed = taskSource.manualForIntent('intent-source-changed', 'manual', 'intent-source-changed')
    writeArtifact(project, 'backlog', stem, withSource(backlogDoc(stem), original))

    const preserved = validate(project, {
      stem,
      proposal: {
        stem, fromState: 'backlog', state: 'todo',
        bytes: Buffer.from(withSource(todoDoc(stem), original)),
      },
    })
    assert.equal(preserved.findings.some((item) => item.code === 'TASK_SOURCE_IMMUTABLE'), false)
    assert.equal(preserved.ok, true, JSON.stringify(preserved.findings, null, 2))

    const mutated = validate(project, {
      stem,
      proposal: {
        stem, fromState: 'backlog', state: 'todo',
        bytes: Buffer.from(withSource(todoDoc(stem), changed)),
      },
    })
    assertCode(mutated, 'TASK_SOURCE_IMMUTABLE')
    assert.equal(mutated.ok, false)
  })

  check('lifecycle proposals preserve API work-package aliases', () => {
    const project = makeProject()
    const stem = 'TASK_306_api_package_immutable'
    const metadata = apiWorkPackage.create('area:users', [
      'api:missing:getUser',
      'api:missing:listUsers',
    ])
    const changed = apiWorkPackage.create('area:users', [
      'api:missing:getUser',
      'api:missing:updateUser',
    ])
    writeArtifact(
      project,
      'backlog',
      stem,
      withApiPackage(backlogDoc(stem), metadata),
    )

    const preserved = validate(project, {
      stem,
      proposal: {
        stem,
        fromState: 'backlog',
        state: 'todo',
        bytes: Buffer.from(withApiPackage(todoDoc(stem), metadata)),
      },
    })
    assert.equal(
      preserved.findings.some((item) =>
        item.code === 'TASK_SOURCE_IMMUTABLE'),
      false,
    )
    assert.equal(preserved.ok, true, JSON.stringify(preserved.findings, null, 2))

    const replaced = withApiPackage(todoDoc(stem), changed)
    const mismatchedRef = replaced.replace(
      'api:package:' + changed.packageId,
      'api:package:' + metadata.packageId,
    )
    const mutated = validate(project, {
      stem,
      proposal: {
        stem,
        fromState: 'backlog',
        state: 'todo',
        bytes: Buffer.from(mismatchedRef),
      },
    })
    assertCode(mutated, 'TASK_SOURCE_IMMUTABLE')
    assert.equal(mutated.ok, false)

    const dropped = validate(project, {
      stem,
      proposal: {
        stem,
        fromState: 'backlog',
        state: 'todo',
        bytes: Buffer.from(
          withApiPackage(todoDoc(stem), metadata)
            .replace('\n\n' + apiWorkPackage.render(metadata), ''),
        ),
      },
    })
    assertCode(dropped, 'TASK_SOURCE_IMMUTABLE')
    assert.equal(dropped.ok, false)
  })

  check('pending accepts each canonical question type and an empty Answer body', () => {
    for (const [index, type, options] of [
      [6, 'text', ''],
      [7, 'choice', 'one, two'],
      [8, 'multiselect', 'one, two, three'],
    ]) {
      const project = makeProject()
      const stem = `TASK_${index}_${type}`
      writeArtifact(project, 'backlog', stem, backlogDoc(stem))
      writeArtifact(project, 'pending', stem, pendingDoc(stem, { type, options }))
      assertValidState(project, stem, 'pending')
    }
  })

  check('malformed heading and unsupported task filename fail closed', () => {
    const project = makeProject()
    writeArtifact(project, 'backlog', 'TASK_10_heading', '# TASK 99 — Wrong number\n')
    writeFileSync(join(project.tasks, 'backlog', 'TASK_0_unsupported.md'), '# TASK 0 — Unsupported\n')
    writeFileSync(join(project.tasks, 'backlog', 'task_11_case_shadow.md'), '# TASK 11 — Case shadow\n')
    writeFileSync(join(project.tasks, 'backlog', 'ＴＡＳＫ_12_width_shadow.md'), '# TASK 12 — Width shadow\n')
    const result = validate(project)
    assert.equal(result.ok, false)
    assertCode(result, 'TASK_HEADING_NUMBER_MISMATCH')
    const unsupported = result.findings.filter((item) => item.code === 'TASK_FILENAME_UNSUPPORTED')
    assert.equal(unsupported.length, 3)
    assert.equal(unsupported.some((item) => item.paths.some((entry) => entry.includes('task_11_case_shadow'))), true)
    assert.equal(unsupported.some((item) => item.paths.some((entry) => entry.includes('ＴＡＳＫ_12_width_shadow'))), true)
  })

  check('task identity is a real CommonMark H1 with horizontal canonical separators', () => {
    for (let indent = 0; indent <= 3; indent++) {
      const parsed = core.parseHeading(`${' '.repeat(indent)}# TASK 10 — Canonical identity ###\n`)
      assert.equal(parsed.issue, undefined, `legal ATX indent ${indent}`)
      assert.equal(parsed.number, 10)
      assert.equal(parsed.title, 'Canonical identity')
    }
    for (const text of [
      '    # TASK 10 — Indented code\n',
      '\t# TASK 10 — Tab-indented code\n',
      '# TASK 10\v—\vControl whitespace\n',
      '# TASK 10\f—\fControl whitespace\n',
    ]) assert.ok(core.parseHeading(text).issue, JSON.stringify(text))
  })

  check('task-like files at the task root are bounded global blockers', () => {
    const project = makeProject()
    const stem = 'TASK_63_root_scope'
    setState(project, 'backlog', stem)
    const baseline = validate(project)
    assert.equal(baseline.ok, true, JSON.stringify(baseline.findings, null, 2))

    const stray = join(project.tasks, 'TASK_99_hidden.md')
    const widthAlias = join(project.tasks, 'ＴＡＳＫ_63_root_scope.md')
    writeFileSync(stray, '# TASK 99 — Hidden root task\n')
    writeFileSync(widthAlias, '# TASK 63 — Width alias\n')
    const result = core.validateAction({
      repoRoot: project.root,
      tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE,
      includeRuntime: false,
      action: 'prep',
      stem,
    })
    assert.equal(result.ok, false)
    const findings = result.findings.filter((item) => item.code === 'TASK_FILE_OUTSIDE_COLUMN')
    assert.equal(findings.length, 2)
    assert.equal(findings.every((item) => item.stem === null), true)
    assert.equal(result._model.snapshotInputs.some((item) => item.path.endsWith('/TASK_99_hidden.md')), true)
    assert.equal(result._model.snapshotInputs.some((item) => item.path.endsWith('/ＴＡＳＫ_63_root_scope.md')), true,
      'NFKC task aliases at the task root must be read and fenced during a global scan')
    assert.notEqual(result.snapshotHash, baseline.snapshotHash)
  })

  check('task-like example suffixes inside lifecycle columns are never silently ignored', () => {
    const project = makeProject()
    for (const [index, column] of core.COLUMNS.entries()) {
      writeFileSync(join(project.tasks, column, `TASK_${80 + index}_hidden.md.example`), 'hidden task-like bytes\n')
    }
    const result = validate(project)
    assert.equal(result.ok, false)
    const findings = result.findings.filter((item) => item.code === 'TASK_FILENAME_UNSUPPORTED')
    assert.equal(findings.length, 4)
    for (const column of core.COLUMNS) assert.equal(findings.some((item) => item.paths.some((entry) => entry.includes('/' + column + '/'))), true)
    assert.equal(result._model.snapshotInputs.filter((item) => item.path.endsWith('.md.example')).length, 4)
  })

  check('scoped action admission cannot hide a Unicode or case-shadow task filename', () => {
    const project = makeProject()
    const stem = 'TASK_13_scoped_shadow'
    setState(project, 'backlog', stem)
    publishFreshIndex(project)
    writeFileSync(join(project.tasks, 'backlog', 'ＴＡＳＫ_13_scoped_shadow.md'), '# TASK 13 — Width shadow\n')
    const result = core.validateAction({
      repoRoot: project.root,
      tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE,
      includeRuntime: false,
      checkIndex: true,
      action: 'prep',
      stem,
    })
    assert.equal(result.ok, false)
    assertCode(result, 'TASK_FILENAME_UNSUPPORTED')
    const finding = result.findings.find((item) => item.code === 'TASK_FILENAME_UNSUPPORTED')
    assert.equal(finding.stem, null, 'an uncanonical visual alias must block every scoped action')
  })

  check('task-column temporaries use a strict allow-list and participate in the snapshot', () => {
    const project = makeProject()
    const allowed = join(project.tasks, 'backlog', '.create-' + 'a'.repeat(32) + '.tmp')
    writeFileSync(allowed, backlogDoc('TASK_9_staged'))
    const transitionTemps = core.COLUMNS.map((column, index) => {
      const file = join(project.tasks, column, '.transition-' + String(index + 1).repeat(36) + '.tmp')
      writeFileSync(file, 'bounded transition candidate for ' + column + '\n')
      return file
    })
    const first = validate(project)
    assert.equal(first.ok, true, JSON.stringify(first.findings, null, 2))
    for (const file of transitionTemps) {
      const relative = file.slice(project.root.length + 1)
      assert.equal(first._model.snapshotInputs.some((item) => item.path === relative), true,
        'every column transition temp must contribute exact bytes to the snapshot')
    }

    writeFileSync(allowed, backlogDoc('TASK_9_staged', 'Changed staged bytes'))
    const second = validate(project)
    assert.equal(second.ok, true, JSON.stringify(second.findings, null, 2))
    assert.notEqual(second.snapshotHash, first.snapshotHash,
      'allow-listed temporary bytes must be fenced by snapshotHash')

    writeFileSync(join(project.tasks, 'todo', '.edit-not-a-transaction.tmp'), 'foreign')
    const invalid = validate(project)
    assert.equal(invalid.ok, false)
    assertCode(invalid, 'TASK_TEMP_FILENAME_UNSUPPORTED')
  })

  check('corpus limits include temporary and unsupported task-related files', () => {
    const countProject = makeProject()
    writeFileSync(join(countProject.tasks, 'backlog', '.create-' + 'a'.repeat(32) + '.tmp'), 'one\n')
    writeFileSync(join(countProject.tasks, 'todo', 'TASK_0_unsupported.md'), 'two\n')
    assert.throws(() => validate(countProject, { maxFiles: 1 }), (error) =>
      error instanceof core.ContractError && /file-count limit/.test(error.message))

    const bytesProject = makeProject()
    writeFileSync(join(bytesProject.tasks, 'backlog', '.edit-' + 'b'.repeat(32) + '.tmp'), '123456789')
    assert.throws(() => validate(bytesProject, { maxCorpusBytes: 8 }), (error) =>
      error instanceof core.ContractError && /bounded read limit/.test(error.message))
  })

  check('orphan and malformed pending pairs report every structural failure', () => {
    const orphanProject = makeProject()
    writeArtifact(orphanProject, 'pending', 'TASK_11_orphan', pendingDoc('TASK_11_orphan'))
    const orphan = validate(orphanProject, { stem: 'TASK_11_orphan' })
    assert.equal(orphan.observedState, 'corrupt')
    assert.equal(orphan.ok, false)
    assertCode(orphan, 'PENDING_SOURCE_MISSING')
    assertCode(orphan, 'TASK_PRESENT_IN_MULTIPLE_STATES')

    const project = makeProject()
    const stem = 'TASK_12_bad_pending'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    writeArtifact(project, 'pending', stem, [
      '---',
      'forTask: TASK_999_wrong',
      'createdAt: not-a-date',
      'updatedAt: 2026-07-13T08:01:00Z',
      'round: 0',
      'gapCount: -1',
      '---',
      '## Q2 — Bad options',
      '**Type**: choice',
      '**Options**: only-one',
      '### Answer',
      '## Q1 — Bad type and answer',
      '**Type**: binary',
      '',
    ].join('\n'))
    const result = validate(project, { stem })
    assert.equal(result.ok, false)
    for (const code of [
      'PENDING_FOR_TASK_MISMATCH', 'PENDING_COUNTER_INVALID',
      'PENDING_QUESTION_ORDER_INVALID', 'PENDING_QUESTION_TYPE_INVALID',
      'PENDING_QUESTION_OPTIONS_INVALID', 'PENDING_ANSWER_SECTION_INVALID',
    ]) assertCode(result, code)

    writeArtifact(project, 'pending', stem, [
      '---',
      `forTask: ${stem}`,
      `forTask: ${stem}`,
      'createdAt: 2026-07-13T08:00:00Z',
      'updatedAt: 2026-07-13T08:01:00Z',
      'round: 1',
      'gapCount: 0',
      '---',
      '',
    ].join('\n'))
    const missingQuestions = validate(project, { stem })
    assertCode(missingQuestions, 'PENDING_FRONTMATTER_INVALID')
    assertCode(missingQuestions, 'PENDING_QUESTION_ID_INVALID')

    writeArtifact(project, 'pending', stem, '\uFEFF' + pendingDoc(stem).replace(/\n/g, '\r\n'))
    const noncanonicalText = validate(project, { stem })
    assertCode(noncanonicalText, 'PENDING_FRONTMATTER_INVALID')

    writeArtifact(project, 'pending', stem, [
      '---',
      `forTask: ${stem}`,
      'createdAt: 2026-07-13T08:00:00Z',
      'updatedAt: 2026-07-13T08:01:00Z',
      'round: 1',
      'gapCount: 2',
      '---',
      '## Q1 — Text with options and duplicate Answer',
      '**Type**: text',
      '**Options**: forbidden, here',
      '### Answer',
      '### Answer',
      '## Q1 — Duplicate id and normalized options',
      '**Type**: multiselect',
      '**Options**: Alpha, alpha',
      '### Answer',
      '',
    ].join('\n'))
    const duplicates = validate(project, { stem })
    assertCode(duplicates, 'PENDING_QUESTION_ID_INVALID')
    assertCode(duplicates, 'PENDING_QUESTION_ORDER_INVALID')
    assertCode(duplicates, 'PENDING_QUESTION_OPTIONS_INVALID')
    assertCode(duplicates, 'PENDING_ANSWER_SECTION_INVALID')
  })

  check('a malformed in-body Questions section is task data that needs repair', () => {
    const project = makeProject()
    const stem = 'TASK_140_todo_questions_invalid'
    const canonical = [
      todoDoc(stem).trimEnd(),
      '',
      '## Questions',
      '',
      '### Q1 — Which strategy?',
      '',
      '**Type**: choice',
      '**Options**: safe, fast',
      '',
      '#### Answer',
      '',
      '',
    ].join('\n')
    writeArtifact(project, 'todo', stem, canonical)
    assert.equal(validate(project, { stem }).ok, true, 'a canonical section must stay clean')

    for (const [label, mutate] of [
      ['missing_answer', (text) => text.replace('#### Answer\n', '')],
      ['duplicate_section', (text) => text + '\n## Questions\n\n### Q2 — Second\n\n**Type**: text\n\n#### Answer\n\n'],
      ['unknown_heading', (text) => text.replace('### Q1 —', '### Question 1 —')],
      ['single_option', (text) => text.replace('**Options**: safe, fast', '**Options**: safe')],
      ['text_with_options', (text) => text.replace('**Type**: choice', '**Type**: text')],
    ]) {
      writeArtifact(project, 'todo', stem, mutate(canonical))
      const result = validate(project, { stem })
      assertCode(result, 'TODO_QUESTIONS_INVALID')
      const reported = result.findings.find((item) => item.code === 'TODO_QUESTIONS_INVALID')
      assert.equal(reported.severity, 'error', label + ' must fail closed')
      assert.equal(result.ok, false, label + ' must block canonical admission')
      assert.equal(core.actionAdmission(result).ok, false, label + ' must not be action-admissible')
      assert.equal(core.dropAdmission(result, stem).ok, true, label + ' must remain explicitly droppable')
    }

    // A task body without the reserved section stays entirely unaffected.
    writeArtifact(project, 'todo', stem, todoDoc(stem))
    assert.equal(validate(project, { stem }).ok, true)
  })

  check('pending rejects unrecognized question headings and unsafe numeric counters', () => {
    const project = makeProject()
    const stem = 'TASK_18_pending_residue'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    writeArtifact(project, 'pending', stem, pendingDoc(stem)
      .replace('round: 1', `round: ${'9'.repeat(400)}`)
      .replace('### Answer\n', '### Answer\n\n## Q2: malformed and otherwise invisible\n'))
    const result = validate(project, { stem })
    assert.equal(result.ok, false)
    assertCode(result, 'PENDING_COUNTER_INVALID')
    assertCode(result, 'PENDING_QUESTION_HEADING_INVALID')
  })

  check('pending rejects calendar-normalized timestamps', () => {
    const project = makeProject()
    const stem = 'TASK_19_pending_date'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    writeArtifact(project, 'pending', stem, pendingDoc(stem)
      .replace('createdAt: 2026-07-13T08:00:00Z', 'createdAt: 2026-02-31T08:00:00Z'))
    const result = validate(project, { stem })
    assert.equal(result.ok, false)
    assertCode(result, 'PENDING_COUNTER_INVALID')
  })

  check('pending counters cannot undercount rendered gaps or move backward in time', () => {
    const project = makeProject()
    const stem = 'TASK_23_pending_counter_order'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    writeArtifact(project, 'pending', stem, pendingDoc(stem)
      .replace('updatedAt: 2026-07-13T08:01:00Z', 'updatedAt: 2026-07-13T07:59:00Z')
      .replace('gapCount: 1', 'gapCount: 0'))
    const result = validate(project, { stem })
    assert.equal(result.ok, false)
    assertCode(result, 'PENDING_COUNTER_INVALID')
  })

  check('pending and todo structural tokens cannot span physical lines', () => {
    const pendingCases = [
      ['split_type', (text) => text.replace('**Type**: text', '**Type**:\ntext'), 'PENDING_QUESTION_TYPE_INVALID'],
      ['split_options', (text) => text.replace('**Options**: alpha, beta', '**Options**:\nalpha, beta'), 'PENDING_QUESTION_OPTIONS_INVALID'],
      ['split_answer', (text) => text.replace('### Answer', '###\nAnswer'), 'PENDING_ANSWER_SECTION_INVALID'],
      ['split_question', (text) => text.replace('## Q1 —', '##\nQ1 —'), 'PENDING_QUESTION_HEADING_INVALID'],
    ]
    for (let index = 0; index < pendingCases.length; index++) {
      const [suffix, mutate, code] = pendingCases[index]
      const project = makeProject()
      const stem = `TASK_${572 + index}_${suffix}`
      writeArtifact(project, 'backlog', stem, backlogDoc(stem))
      writeArtifact(project, 'pending', stem, mutate(pendingDoc(stem, {
        type: suffix === 'split_options' ? 'choice' : 'text',
        options: suffix === 'split_options' ? 'alpha, beta' : '',
      })))
      const result = validate(project, { stem })
      assert.equal(result.ok, false, `${suffix} unexpectedly passed`)
      assertCode(result, code)
    }

    for (const [number, heading] of [[576, 'Automated'], [577, 'Manual']]) {
      const project = makeProject()
      const stem = `TASK_${number}_split_${heading.toLowerCase()}`
      writeArtifact(project, 'todo', stem, todoDoc(stem).replace(`### ${heading}`, `###\n${heading}`))
      const result = validate(project, { stem })
      assert.equal(result.ok, false, `split ${heading} unexpectedly passed`)
      assertCode(result, 'TODO_ACCEPTANCE_INVALID')
    }
  })

  check('malformed todo rejects missing sections, weak automation, and live Outcome', () => {
    const project = makeProject()
    const stem = 'TASK_13_bad_todo'
    writeArtifact(project, 'todo', stem, [
      '# TASK 13 — Bad todo',
      '',
      '## Goal',
      '',
      'A goal.',
      '',
      '## Inputs',
      '',
      '## Acceptance',
      '',
      '### Automated',
      '',
      '- It works.',
      '',
      '## Outcome',
      '',
      'This does not belong in todo.',
      '',
    ].join('\n'))
    const result = validate(project, { stem })
    assert.equal(result.ok, false)
    assertCode(result, 'TODO_REQUIRED_SECTION_EMPTY')
    assertCode(result, 'TODO_REQUIRED_SECTION_MISSING')
    assertCode(result, 'TODO_AUTOMATION_ANCHOR_MISSING')
    assertCode(result, 'OUTCOME_FORBIDDEN_IN_LIVE_STATE')
  })

  check('todo Design structure is validated before run admission', () => {
    const validProject = makeProject()
    const validStem = 'TASK_75_valid_design'
    writeArtifact(validProject, 'todo', validStem, todoDoc(validStem).replace(
      '\n## Acceptance\n',
      '\n## Design\n\n- Home [screen] — https://www.figma.com/design/ABC/Home?node-id=1-2\n\n## Acceptance\n',
    ))
    const admitted = core.validateAction({
      action: 'run', stem: validStem, repoRoot: validProject.root, tasksDir: validProject.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(admitted.ok, true, JSON.stringify(admitted.findings, null, 2))

    for (const [number, body] of [
      [76, '- broken bullet'],
      [77, '- Menu [sheet] — https://www.figma.com/design/ABC/Menu?node-id=1-2'],
      [78, ''],
    ]) {
      const project = makeProject()
      const stem = `TASK_${number}_invalid_design`
      writeArtifact(project, 'todo', stem, todoDoc(stem).replace(
        '\n## Acceptance\n', `\n## Design\n\n${body}\n\n## Acceptance\n`,
      ))
      const result = core.validateAction({
        action: 'run', stem, repoRoot: project.root, tasksDir: project.tasks,
        outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
      })
      assert.equal(result.ok, false)
      assertCode(result, 'TODO_DESIGN_INVALID')
    }
  })

  check('CommonMark H2 indentation and closing hashes cannot hide Design, Outcome, or dependencies', () => {
    for (let indent = 0; indent <= 3; indent++) {
      const project = makeProject()
      const stem = `TASK_${430 + indent}_commonmark_h2_${indent}`
      const pad = ' '.repeat(indent)
      const structuralSections = [
        `${pad}## Depends on (optional) ##`,
        '',
        '- TASK_999_missing_commonmark_dependency',
        '',
        `${pad}## Design ##`,
        '',
        '- broken bullet',
        '',
      ].join('\n')
      const body = todoDoc(stem)
        .replace('\n## Acceptance\n', `\n${structuralSections}## Acceptance\n`)
        .trimEnd() + `\n\n---\n\n${pad}## Outcome ##\n\n**Status**: completed\n`
      writeArtifact(project, 'todo', stem, body)
      const result = core.validateAction({
        action: 'run', stem, repoRoot: project.root, tasksDir: project.tasks,
        outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
      })
      assert.equal(result.ok, false, `indent ${indent} escaped admission: ${JSON.stringify(result.findings)}`)
      for (const code of [
        'TODO_DESIGN_INVALID', 'OUTCOME_FORBIDDEN_IN_LIVE_STATE',
        'DEPENDENCY_UNRESOLVED', 'RUN_DEPENDENCY_UNSATISFIED',
      ]) assertCode(result, code)
      assert.deepEqual(result._model.metadata.get(stem).deps, ['TASK_999_missing_commonmark_dependency'])
    }

    const codeProject = makeProject()
    const codeStem = 'TASK_434_commonmark_h2_code'
    const indentedCode = [
      '    ## Depends on (optional) ##',
      '- TASK_999_code_only_dependency',
      '',
      '    ## Design ##',
      '- broken bullet',
      '',
    ].join('\n')
    const codeBody = todoDoc(codeStem)
      .replace('\n## Acceptance\n', `\n${indentedCode}## Acceptance\n`)
      .trimEnd() + '\n\n---\n\n    ## Outcome ##\n\n**Status**: completed\n'
    writeArtifact(codeProject, 'todo', codeStem, codeBody)
    const codeResult = core.validateAction({
      action: 'run', stem: codeStem, repoRoot: codeProject.root, tasksDir: codeProject.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(codeResult.ok, true, `four-space indented code became structural: ${JSON.stringify(codeResult.findings)}`)
    assert.deepEqual(codeResult._model.metadata.get(codeStem).deps, [])
  })

  check('only structural top-level bullets can become dependency or Outcome credentials', () => {
    for (let indent = 0; indent <= 3; indent++) {
      const dependency = `TASK_${590 + indent}_top_level_dependency`
      const parsed = core.parseDependencies([
        '# TASK 589 — Bullet policy',
        '',
        '## Depends on (optional)',
        '',
        `${' '.repeat(indent)}- ${dependency}`,
        '',
      ].join('\n'))
      assert.deepEqual(parsed.deps, [dependency], `legal bullet indent ${indent}`)
      assert.deepEqual(parsed.invalid, [])
    }

    for (const [label, prefix] of [['indented code', '    '], ['tab code', '\t'], ['vertical tab', '\v']]) {
      const dependency = 'TASK_599_hidden_dependency'
      const parsed = core.parseDependencies([
        '# TASK 598 — Bullet policy',
        '',
        '## Depends on (optional)',
        '',
        `${prefix}- ${dependency}`,
        '',
      ].join('\n'))
      assert.deepEqual(parsed.deps, [], label)
    }

    const controlDependency = core.parseDependencies([
      '# TASK 595 — Control separator', '', '## Depends on (optional)', '',
      '- TASK_599_hidden_dependency\v—\vnote', '',
    ].join('\n'))
    assert.deepEqual(controlDependency.deps, [])
    assert.equal(controlDependency.invalid.length, 1)
    const controlLineage = core.parseLineage([
      '# TASK 595 — Control separator', '', '## Origin', '',
      '- split\vfrom TASK_599_hidden_dependency', '',
    ].join('\n'))
    assert.deepEqual(controlLineage.parents, [])
    assert.equal(controlLineage.invalid.length, 1)

    const shape = JSON.parse(readFileSync(OUTCOME_SHAPE, 'utf8'))
    const codeBullet = doneDoc('TASK_597_code_outcome').replace(
      '- `test/contract.mjs` — verified — Passed.',
      '    - `test/contract.mjs` — verified — Passed.',
    )
    assert.equal(core.parseOutcome(codeBullet, shape).valid, false)
    assert.notEqual(outcomeShapeError(codeBullet, { shape }), null)

    const mixedNone = doneDoc('TASK_596_mixed_none').replace(
      '- `test/contract.mjs` — verified — Passed.',
      '- none\n- `test/contract.mjs` — verified — Passed.',
    )
    assert.equal(core.parseOutcome(mixedNone, shape).valid, false)
    assert.match(outcomeShapeError(mixedNone, { shape }), /cannot combine `none`/)

    const unsafeFollowUp = doneDoc('TASK_595_unsafe_follow_up').replace(
      '### Follow-ups\n\n- none',
      '### Follow-ups\n\n- `TASK_9007199254740992_unsafe` — backlog',
    )
    assert.equal(core.parseOutcome(unsafeFollowUp, shape).valid, false)
    assert.match(outcomeShapeError(unsafeFollowUp, { shape }), /invalid follow-up bullet/)

    const controlFile = doneDoc('TASK_594_control_file', {
      file: '`src/feature.js`\v—\vmodified — forged controls.',
    })
    assert.equal(core.parseOutcome(controlFile, shape).valid, false)
  })

  check('done Outcome H2 uses the same CommonMark indentation and closing-hash contract', () => {
    const shape = JSON.parse(readFileSync(OUTCOME_SHAPE, 'utf8'))
    for (let indent = 0; indent <= 3; indent++) {
      const project = makeProject()
      const stem = `TASK_${435 + indent}_commonmark_done_${indent}`
      const body = doneDoc(stem).replace('## Outcome', `${' '.repeat(indent)}## Outcome ##`)
      writeArtifact(project, 'done', stem, body)
      const result = validate(project, { stem })
      assert.equal(result.ok, true, `indent ${indent} rejected: ${JSON.stringify(result.findings)}`)
      assert.equal(core.parseOutcome(body, shape).valid, true)
      assert.equal(outcomeShapeError(body, { shape }), null)
      assert.ok(sharedOutcomeAppendixStart(body) >= 0)
      assert.equal(figmaOutcomeAppendixStatus(body), 'completed')
    }

    const codeProject = makeProject()
    const codeStem = 'TASK_439_commonmark_done_code'
    const codeBody = doneDoc(codeStem).replace('## Outcome', '    ## Outcome ##')
    writeArtifact(codeProject, 'done', codeStem, codeBody)
    const codeResult = validate(codeProject, { stem: codeStem })
    assert.equal(codeResult.ok, false)
    assertCode(codeResult, 'DONE_OUTCOME_INVALID')
    assert.notEqual(outcomeShapeError(codeBody, { shape }), null)
    assert.equal(sharedOutcomeAppendixStart(codeBody), -1)
    assert.equal(figmaOutcomeAppendixStatus(codeBody), '')

    const statusStem = 'TASK_569_commonmark_status_fences'
    const baseStatusBody = doneDoc(statusStem)
    const realOutcome = '\n---\n\n## Outcome'
    for (let indent = 0; indent <= 3; indent++) {
      const fence = ' '.repeat(indent) + '````'
      const fencedDecoy = baseStatusBody.replace(realOutcome,
        `\n${fence}markdown\n## Outcome ##\n${fence}\n${realOutcome}`)
      assert.equal(figmaOutcomeAppendixStatus(fencedDecoy), 'completed', `status fence indent ${indent}`)
    }
    for (const opener of ['    ````markdown', '\t````markdown', '```audit`invalid']) {
      const exposedDecoy = baseStatusBody.replace(realOutcome,
        `\n${opener}\n## Outcome ##\n\`\`\`\`\n${realOutcome}`)
      assert.equal(figmaOutcomeAppendixStatus(exposedDecoy), '', `status invalid fence ${JSON.stringify(opener)}`)
    }
    const retainedFence = baseStatusBody.replace(realOutcome,
      `\n\`\`\`\`markdown\n    \`\`\`\`\n## Outcome ##\n\`\`\`\`\n${realOutcome}`)
    assert.equal(figmaOutcomeAppendixStatus(retainedFence), 'completed', 'overindented status closer')
  })

  check('todo Design admission ignores fenced and all CommonMark HTML-block decoys', () => {
    const validBullet = '- Decoy [screen] — https://www.figma.com/design/ABC/Decoy?node-id=1-2'
    const decoys = [
      ['fenced', ['````markdown', '## Design', validBullet, '````']],
      ['script', ['<script>', '## Design', validBullet, '</script>']],
      ['comment', ['<!--', '## Design', validBullet, '-->']],
      ['processing_instruction', ['<?audit', '## Design', validBullet, '?>']],
      ['declaration', ['<!AUDIT', '## Design', validBullet, '>']],
      ['cdata', ['<![CDATA[', '## Design', validBullet, ']]>']],
      ['block_tag', ['<div data-audit="true">', '## Design', validBullet, '']],
      ['complete_custom_tag', ['<audit-box data-mode=hidden>', '## Design', validBullet, '']],
    ]
    for (const [index, [kind, decoy]] of decoys.entries()) {
      const project = makeProject()
      const number = 420 + index
      const stem = `TASK_${number}_design_${kind}`
      const malformedRealDesign = ['## Design', '', '- broken bullet', ''].join('\n')
      writeArtifact(project, 'todo', stem, todoDoc(stem).replace(
        '\n## Acceptance\n', `\n${decoy.join('\n')}\n${malformedRealDesign}\n## Acceptance\n`,
      ))
      const result = core.validateAction({
        action: 'run', stem, repoRoot: project.root, tasksDir: project.tasks,
        outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
      })
      assert.equal(result.ok, false, `${kind} decoy hid malformed real Design: ${JSON.stringify(result.findings)}`)
      assertCode(result, 'TODO_DESIGN_INVALID')
    }

    const control = makeProject()
    const controlStem = 'TASK_428_design_real_first'
    const fencedAfter = ['````markdown', '## Design', '- broken bullet', '````'].join('\n')
    writeArtifact(control, 'todo', controlStem, todoDoc(controlStem).replace(
      '\n## Acceptance\n',
      `\n## Design\n\n- Home [screen] — https://www.figma.com/design/ABC/Home?node-id=1-2\n\n${fencedAfter}\n\n## Acceptance\n`,
    ))
    const admitted = core.validateAction({
      action: 'run', stem: controlStem, repoRoot: control.root, tasksDir: control.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(admitted.ok, true, JSON.stringify(admitted.findings, null, 2))
  })

  check('shorter or prose-suffixed code fences cannot expose structural headings', () => {
    const project = makeProject()
    const stem = 'TASK_64_fenced_structure'
    writeArtifact(project, 'todo', stem, [
      '# TASK 64 — Fenced structure',
      '',
      '````markdown',
      '```',
      '## Goal',
      '',
      'Hidden goal.',
      '',
      '## Inputs',
      '',
      '- Hidden input.',
      '',
      '## Acceptance',
      '',
      '### Automated',
      '',
      '- Run `node test/hidden.mjs`.',
      '',
      '### Manual',
      '',
      '- Inspect.',
      '',
      '## Out of scope',
      '',
      '- Hidden scope.',
      '',
      '```` trailing prose is not a closing fence',
      '',
    ].join('\n'))
    const result = validate(project, { stem })
    assert.equal(result.ok, false)
    assertCode(result, 'TODO_REQUIRED_SECTION_MISSING')
  })

  check('four-space or tab fence openers and overindented closers cannot hide real Design', () => {
    for (const [index, opener] of ['    ````markdown', '\t````markdown'].entries()) {
      const project = makeProject()
      const stem = `TASK_${440 + index}_invalid_fence_opener`
      writeArtifact(project, 'todo', stem, todoDoc(stem).replace(
        '\n## Acceptance\n', `\n${opener}\n## Design\n\n- broken bullet\n\n## Acceptance\n`,
      ))
      const result = core.validateAction({
        action: 'run', stem, repoRoot: project.root, tasksDir: project.tasks,
        outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
      })
      assert.equal(result.ok, false, `${JSON.stringify(opener)} hid Design: ${JSON.stringify(result.findings)}`)
      assertCode(result, 'TODO_DESIGN_INVALID')
    }

    const project = makeProject()
    const stem = 'TASK_442_overindented_fence_close'
    const fenced = [
      '````markdown',
      '    ````',
      '## Design',
      '- Hidden [screen] — https://www.figma.com/design/ABC/Hidden?node-id=1-2',
      '````',
      '## Design',
      '- broken bullet',
      '',
    ].join('\n')
    writeArtifact(project, 'todo', stem, todoDoc(stem).replace(
      '\n## Acceptance\n', `\n${fenced}## Acceptance\n`,
    ))
    const result = core.validateAction({
      action: 'run', stem, repoRoot: project.root, tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(result.ok, false, `overindented close exposed the decoy: ${JSON.stringify(result.findings)}`)
    assertCode(result, 'TODO_DESIGN_INVALID')
  })

  check('HTML comments cannot satisfy todo, pending, done, or dependency structure', () => {
    const todoProject = makeProject()
    const todoStem = 'TASK_65_commented_todo'
    writeArtifact(todoProject, 'todo', todoStem, [
      '# TASK 65 — Commented todo',
      '',
      '<!--',
      todoDoc(todoStem).split('\n').slice(1).join('\n'),
      '-->',
      '',
    ].join('\n'))
    const todoResult = validate(todoProject, { stem: todoStem })
    assert.equal(todoResult.ok, false)
    assertCode(todoResult, 'TODO_REQUIRED_SECTION_MISSING')

    const inlineProject = makeProject()
    const inlineStem = 'TASK_578_inline_comment_suffix'
    writeArtifact(inlineProject, 'todo', inlineStem, [
      '# TASK 578 — Inline comment suffix',
      '',
      'paragraph <!--',
      '-->## Goal',
      'Forged goal.',
      'paragraph <!--',
      '-->## Inputs',
      '- Forged input.',
      'paragraph <!--',
      '-->## Out of scope',
      '- Forged exclusion.',
      'paragraph <!--',
      '-->## Acceptance',
      '- Run `node test/contract.mjs`.',
      '',
    ].join('\n'))
    const inlineResult = validate(inlineProject, { stem: inlineStem })
    assert.equal(inlineResult.ok, false, 'an inline-comment terminator suffix became block structure')
    assertCode(inlineResult, 'TODO_REQUIRED_SECTION_MISSING')

    const pendingProject = makeProject()
    const pendingStem = 'TASK_66_commented_pending'
    writeArtifact(pendingProject, 'backlog', pendingStem, backlogDoc(pendingStem))
    writeArtifact(pendingProject, 'pending', pendingStem,
      pendingDoc(pendingStem).replace('## Q1', '<!-- ## Q1').replace('### Answer\n', '### Answer -->\n'))
    const pendingResult = validate(pendingProject, { stem: pendingStem })
    assert.equal(pendingResult.ok, false)
    assertCode(pendingResult, 'PENDING_QUESTION_ID_INVALID')

    const doneProject = makeProject()
    const doneStem = 'TASK_67_commented_done'
    const outcomeStart = doneDoc(doneStem).indexOf('\n---\n\n## Outcome')
    const doneText = doneDoc(doneStem)
    writeArtifact(doneProject, 'done', doneStem,
      doneText.slice(0, outcomeStart) + '\n<!--' + doneText.slice(outcomeStart) + '\n-->\n')
    const doneResult = validate(doneProject, { stem: doneStem })
    assert.equal(doneResult.ok, false)
    assertCode(doneResult, 'DONE_OUTCOME_INVALID')

    const dependencyProject = makeProject()
    const dependencyStem = 'TASK_68_commented_dependency'
    const hiddenDependency = 'TASK_69_hidden_dependency'
    writeArtifact(dependencyProject, 'todo', dependencyStem, todoDoc(dependencyStem)
      .replace('## Acceptance', `<!--\n## Depends on (optional)\n\n- ${hiddenDependency}\n-->\n\n## Acceptance`))
    const dependencyResult = validate(dependencyProject, { stem: dependencyStem })
    assert.equal(dependencyResult.ok, true, JSON.stringify(dependencyResult.findings, null, 2))
    assert.deepEqual(Array.from(dependencyResult._model.loadedStems), [dependencyStem],
      'commented dependency edges must not expand the scoped read closure')
  })

  check('code and lowercase declaration lookalikes cannot hide real dependencies', () => {
    const cases = [
      {
        stem: 'TASK_583_indented_comment_code',
        dependency: 'TASK_584_indented_dependency',
        prefix: '    <!--\n',
        suffix: '    -->',
      },
      {
        stem: 'TASK_585_lowercase_declaration',
        dependency: 'TASK_586_lowercase_dependency',
        prefix: '<!foo\n',
        suffix: '>',
      },
    ]
    for (const item of cases) {
      const project = makeProject()
      const inserted = [
        item.prefix + '## Depends on (optional)',
        `- ${item.dependency}`,
        item.suffix,
        '',
      ].join('\n')
      writeArtifact(project, 'todo', item.stem, todoDoc(item.stem).replace(
        '\n## Acceptance\n', `\n${inserted}## Acceptance\n`,
      ))
      const result = validate(project, { stem: item.stem })
      assert.deepEqual(result._model.metadata.get(item.stem).deps, [item.dependency])
      const admission = core.validateAction({
        action: 'run', stem: item.stem, repoRoot: project.root, tasksDir: project.tasks,
        outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
      })
      assert.equal(admission.ok, false)
      assertCode(admission, 'RUN_DEPENDENCY_UNSATISFIED')
    }
  })

  check('inline code spans and escaped comment openers preserve dependency closure', () => {
    const cases = [
      {
        stem: 'TASK_587_inline_code_dependency',
        dependency: 'TASK_588_inline_code_target',
        insertion: [
          'literal `<!--` and ``<!--`` and ```<!--```',
          '## Depends on (optional)',
          '- TASK_588_inline_code_target',
        ],
      },
      {
        stem: 'TASK_589_multiline_code_dependency',
        dependency: 'TASK_590_multiline_code_target',
        insertion: [
          'literal ``code span',
          'continued <!--',
          'closed here``',
          '## Depends on (optional)',
          '- TASK_590_multiline_code_target',
        ],
      },
      {
        stem: 'TASK_591_nested_code_dependency',
        dependency: 'TASK_592_nested_code_target',
        insertion: [
          'literal ``outer ` <!-- inner ` outer``',
          '## Depends on (optional)',
          '- TASK_592_nested_code_target',
        ],
      },
      {
        stem: 'TASK_593_escaped_comment_dependency',
        dependency: 'TASK_594_escaped_comment_target',
        insertion: [
          'literal \\<!-- escaped opener',
          '## Depends on (optional)',
          '- TASK_594_escaped_comment_target',
        ],
      },
      {
        stem: 'TASK_595_unmatched_code_dependency',
        dependency: 'TASK_596_unmatched_code_target',
        insertion: [
          'literal `` unmatched ` <!-- real comment',
          '## Depends on (optional)',
          '- TASK_999_forged_dependency',
          '-->',
          '## Depends on (optional)',
          '- TASK_596_unmatched_code_target',
        ],
      },
      {
        stem: 'TASK_597_even_slash_dependency',
        dependency: 'TASK_598_even_slash_target',
        insertion: [
          'literal \\\\<!-- real comment',
          '## Depends on (optional)',
          '- TASK_999_forged_dependency',
          '-->',
          '## Depends on (optional)',
          '- TASK_598_even_slash_target',
        ],
      },
    ]

    for (const item of cases) {
      const project = makeProject()
      writeArtifact(project, 'todo', item.stem, todoDoc(item.stem).replace(
        '## Acceptance', `${item.insertion.join('\n')}\n\n## Acceptance`,
      ))
      const result = validate(project, { stem: item.stem })
      assert.deepEqual(result._model.metadata.get(item.stem).deps, [item.dependency],
        `${item.stem}: JS/Python dependency closure drifted`)
      assert.ok(!result._model.loadedStems.has('TASK_999_forged_dependency'),
        `${item.stem}: commented dependency expanded the closure`)
      const admission = core.validateAction({
        action: 'run', stem: item.stem, repoRoot: project.root, tasksDir: project.tasks,
        outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
      })
      assert.equal(admission.ok, false, `${item.stem}: missing dependency was admitted`)
      assertCode(admission, 'RUN_DEPENDENCY_UNSATISFIED')
    }
  })

  check('only top-level horizontal-space bullets create dependency edges', () => {
    const cases = [
      ['TASK_599_three_space_dependency', 'TASK_600_three_space_target', '   ', 'edge'],
      ['TASK_601_four_space_dependency', 'TASK_602_four_space_target', '    ', 'ignored-code'],
      ['TASK_603_tab_dependency', 'TASK_604_tab_target', '\t', 'ignored-code'],
      ['TASK_605_vt_dependency', 'TASK_606_vt_target', '\v', 'invalid-residue'],
      ['TASK_607_ff_dependency', 'TASK_608_ff_target', '\f', 'invalid-residue'],
    ]
    for (const [stem, dependency, prefix, policy] of cases) {
      const project = makeProject()
      const section = ['## Depends on (optional)', `${prefix}- ${dependency}`, '', '## Acceptance'].join('\n')
      writeArtifact(project, 'todo', stem, todoDoc(stem).replace('## Acceptance', section))
      const result = validate(project, { stem })
      assert.deepEqual(result._model.metadata.get(stem).deps, policy === 'edge' ? [dependency] : [],
        `${stem}: non-canonical bullet policy drifted`)
      const admission = core.validateAction({
        action: 'run', stem, repoRoot: project.root, tasksDir: project.tasks,
        outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
      })
      if (policy === 'edge') {
        assert.equal(admission.ok, false)
        assertCode(admission, 'RUN_DEPENDENCY_UNSATISFIED')
      } else if (policy === 'invalid-residue') {
        assert.equal(admission.ok, false)
        assertCode(admission, 'DEPENDENCY_SYNTAX_INVALID')
      } else {
        assert.equal(admission.ok, true, JSON.stringify(admission.findings, null, 2))
      }
    }
  })

  check('dependency headings and note delimiters accept horizontal whitespace only', () => {
    const acceptedProject = makeProject()
    const acceptedStem = 'TASK_609_horizontal_dependency_tokens'
    const acceptedDependency = 'TASK_610_horizontal_dependency_target'
    writeArtifact(acceptedProject, 'todo', acceptedStem, todoDoc(acceptedStem).replace(
      '## Acceptance', [
        '## Depends on\t(optional)',
        `- \`${acceptedDependency}\`\t—\thorizontal note`,
        '',
        '## Acceptance',
      ].join('\n'),
    ))
    const accepted = validate(acceptedProject, { stem: acceptedStem })
    assert.deepEqual(accepted._model.metadata.get(acceptedStem).deps, [acceptedDependency])
    const acceptedAdmission = core.validateAction({
      action: 'run', stem: acceptedStem, repoRoot: acceptedProject.root, tasksDir: acceptedProject.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(acceptedAdmission.ok, false)
    assertCode(acceptedAdmission, 'RUN_DEPENDENCY_UNSATISFIED')

    const headingProject = makeProject()
    const headingStem = 'TASK_611_vertical_dependency_heading'
    writeArtifact(headingProject, 'todo', headingStem, todoDoc(headingStem).replace(
      '## Acceptance', [
        '## Depends on\v(optional)',
        '- TASK_612_vertical_heading_target',
        '',
        '## Acceptance',
      ].join('\n'),
    ))
    const heading = validate(headingProject, { stem: headingStem })
    assert.deepEqual(heading._model.metadata.get(headingStem).deps, [],
      'a vertical-control heading expanded the Python closure')

    const separatorProject = makeProject()
    const separatorStem = 'TASK_613_vertical_dependency_separator'
    writeArtifact(separatorProject, 'todo', separatorStem, todoDoc(separatorStem).replace(
      '## Acceptance', [
        '## Depends on (optional)',
        '- TASK_614_vertical_separator_target\v—\vforged note delimiter',
        '',
        '## Acceptance',
      ].join('\n'),
    ))
    const separator = validate(separatorProject, { stem: separatorStem })
    assert.deepEqual(separator._model.metadata.get(separatorStem).deps, [],
      'a vertical-control separator expanded the Python closure')
    const separatorAdmission = core.validateAction({
      action: 'run', stem: separatorStem, repoRoot: separatorProject.root, tasksDir: separatorProject.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(separatorAdmission.ok, false)
    assertCode(separatorAdmission, 'DEPENDENCY_SYNTAX_INVALID')
  })

  check('all seven CommonMark raw HTML block types hide structural Markdown until their exact terminator', () => {
    const hiddenStructure = [
      '## Goal',
      'Implement hidden behavior.',
      '## Inputs',
      '- Existing contracts.',
      '## Depends on (optional)',
      '- TASK_999_hidden_html_dependency',
      '## Acceptance',
      '### Automated',
      '- Run `node test/contract.mjs`.',
      '### Manual',
      '- Inspect output.',
      '## Out of scope',
      '- Unrelated work.',
    ].join('\n')
    const starts = [
      ['script', '<script>'],
      ['comment', '<!--'],
      ['processing-instruction', '<?audit'],
      ['declaration', '<!AUDIT'],
      ['cdata', '<![CDATA['],
      ['block-tag', '<div data-audit="true">'],
      ['complete-custom-tag', '<audit-box data-mode=hidden>'],
    ]
    for (const [index, [kind, start]] of starts.entries()) {
      const project = makeProject()
      const stem = `TASK_${410 + index}_html_${kind.replaceAll('-', '_')}`
      writeArtifact(project, 'todo', stem, `# TASK ${410 + index} — Hidden in ${kind}\n\n${start}\n${hiddenStructure}\n`)
      const result = validate(project, { stem })
      assert.equal(result.ok, false, `${kind} exposed raw-HTML headings: ${JSON.stringify(result.findings)}`)
      assertCode(result, 'TODO_REQUIRED_SECTION_MISSING')
      assert.deepEqual(Array.from(result._model.loadedStems), [stem],
        `${kind} expanded the anchored dependency closure through raw HTML`)
    }

    const terminatedProject = makeProject()
    const terminatedStem = 'TASK_417_html_terminators'
    const canonicalTodo = todoDoc(terminatedStem)
    const canonicalSource = taskSource.parse(canonicalTodo)
    assert.equal(canonicalSource.valid, true)
    const visibleBody = canonicalTodo.slice(canonicalSource.end).replace(/^\n+/, '')
    writeArtifact(terminatedProject, 'todo', terminatedStem, [
      '# TASK 417 — Exact HTML termination',
      '',
      canonicalSource.block,
      '',
      '<script>',
      '## Hidden script heading',
      '</script>',
      '<div>',
      '## Hidden block heading',
      '',
      visibleBody,
    ].join('\n'))
    const terminated = validate(terminatedProject, { stem: terminatedStem })
    assert.equal(terminated.ok, true, JSON.stringify(terminated.findings, null, 2))
  })

  check('todo acceptance rejects prose residue and duplicate structured subsections', () => {
    const project = makeProject()
    const residueStem = 'TASK_20_prose_residue'
    writeArtifact(project, 'todo', residueStem, todoDoc(residueStem)
      .replace('### Automated\n\n', '')
      .replace('### Manual\n\n- Inspect the rendered result.\n\n', '')
      .replace('- Run `node test/contract.mjs`.', 'This prose must not disappear.\n- Run `node test/contract.mjs`.'))
    const residue = validate(project, { stem: residueStem })
    assert.equal(residue.ok, false)
    assertCode(residue, 'TODO_ACCEPTANCE_INVALID')

    const duplicateStem = 'TASK_21_duplicate_acceptance'
    writeArtifact(project, 'todo', duplicateStem, todoDoc(duplicateStem)
      .replace('### Manual', '### Automated\n\n- Run `node test/second.mjs`.\n\n### Manual'))
    const duplicate = validate(project, { stem: duplicateStem })
    assert.equal(duplicate.ok, false)
    assertCode(duplicate, 'TODO_ACCEPTANCE_INVALID')
  })

  check('malformed done rejects timestamp, acceptance verdict, and unsafe file path', () => {
    const project = makeProject()
    const stem = 'TASK_14_bad_done'
    writeArtifact(project, 'done', stem, doneDoc(stem, {
      completedAt: 'not-a-date',
      acceptance: '`test/contract.mjs` — guessed — Unsupported.',
      file: '`../outside.txt` — modified — Unsafe.',
    }))
    const result = validate(project, { stem })
    assert.equal(result.ok, false)
    for (const code of [
      'DONE_OUTCOME_INVALID', 'DONE_COMPLETED_AT_INVALID',
      'DONE_ACCEPTANCE_TRACE_INVALID', 'DONE_FILES_TOUCHED_INVALID',
    ]) assertCode(result, code)

    writeArtifact(project, 'done', stem, doneDoc(stem, {
      file: '`C:\\outside.txt` — modified — Windows absolute path.',
    }))
    const windowsAbsolute = validate(project, { stem })
    assert.equal(windowsAbsolute.ok, false, 'Windows absolute paths are not repository-relative')
    assertCode(windowsAbsolute, 'DONE_FILES_TOUCHED_INVALID')
  })

  check('done acceptance verdict parsing ignores em-dashes inside the quoted source bullet', () => {
    const project = makeProject()
    const stem = 'TASK_62_quoted_acceptance_dashes'
    writeArtifact(project, 'done', stem, doneDoc(stem, {
      acceptance: '`render states — light — dark` — verified — Exact variants passed.',
    }))
    const result = validate(project, { stem })
    assert.equal(result.ok, true, JSON.stringify(result.findings, null, 2))
    assert.equal(result._model.metadata.get(stem).outcome.acceptance[0].verdict, 'verified')
  })

  check('done Outcome list sections reject prose that cannot be indexed', () => {
    const project = makeProject()
    const stem = 'TASK_15_done_prose'
    const malformed = doneDoc(stem)
      .replace('- `test/contract.mjs` — verified — Passed.', '`test/contract.mjs` — verified — prose is not a bullet.')
      .replace('- `src/feature.js` — modified', '`src/feature.js` — modified')
    writeArtifact(project, 'done', stem, malformed)
    const result = validate(project, { stem })
    assert.equal(result.ok, false)
    assertCode(result, 'DONE_OUTCOME_INVALID')
    assertCode(result, 'DONE_ACCEPTANCE_TRACE_INVALID')
    assertCode(result, 'DONE_FILES_TOUCHED_INVALID')
    assert.match(outcomeShapeError(malformed), /acceptance-trace contains non-bullet/)
  })

  check('done Outcome must be the single appendix anchored directly after the final separator', () => {
    const project = makeProject()
    const stem = 'TASK_19_outcome_anchor'
    const unanchored = doneDoc(stem).replace('\n---\n\n## Outcome\n', '\n---\n\nUntrusted text before the trailer.\n\n## Outcome\n')
    writeArtifact(project, 'done', stem, unanchored)
    let result = validate(project, { stem })
    assert.equal(result.ok, false)
    assertCode(result, 'DONE_OUTCOME_INVALID')

    writeArtifact(project, 'done', stem, doneDoc(stem) + '\n## Outcome\n\nsecond trailer\n')
    result = validate(project, { stem })
    assert.equal(result.ok, false)
    assertCode(result, 'DONE_OUTCOME_INVALID')

    assert.throws(() => core.validateTaskState({
      repoRoot: project.root,
      tasksDir: project.tasks,
      outcomeShapePath: join(project.root, 'missing-outcome-shape.json'),
    }), (error) => error instanceof core.ContractError && error.exitCode === 3)
  })

  check('numeric identity, exact stem, and case-alias collisions block the corpus', () => {
    const numberProject = makeProject()
    setState(numberProject, 'backlog', 'TASK_20_alpha')
    setState(numberProject, 'backlog', 'TASK_20_beta')
    setState(numberProject, 'backlog', 'TASK_23_unrelated')
    const numberResult = validate(numberProject)
    assert.equal(numberResult.ok, false)
    assertCode(numberResult, 'TASK_NUMBER_CONFLICT')
    const unrelatedNumber = validate(numberProject, { stem: 'TASK_23_unrelated' })
    assert.equal(core.actionAdmission(unrelatedNumber).ok, true,
      'a collision between two exact stems must not block a third task')
    assert.equal(unrelatedNumber.findings.some((item) => item.code === 'TASK_NUMBER_CONFLICT'), false)

    const stemProject = makeProject()
    writeArtifact(stemProject, 'backlog', 'TASK_21_same', backlogDoc('TASK_21_same'))
    writeArtifact(stemProject, 'todo', 'TASK_21_same', todoDoc('TASK_21_same'))
    const stemResult = validate(stemProject, { stem: 'TASK_21_same' })
    assert.equal(stemResult.observedState, 'corrupt')
    assertCode(stemResult, 'TASK_PRESENT_IN_MULTIPLE_STATES')

    const aliasProject = makeProject()
    writeArtifact(aliasProject, 'backlog', 'TASK_22_Case', backlogDoc('TASK_22_Case'))
    // Keep aliases in different directories so this remains testable on the
    // default case-insensitive macOS filesystem as well as on Linux.
    writeArtifact(aliasProject, 'todo', 'TASK_22_case', todoDoc('TASK_22_case'))
    writeArtifact(aliasProject, 'backlog', 'TASK_24_unrelated', backlogDoc('TASK_24_unrelated'))
    const aliasResult = validate(aliasProject)
    assert.equal(aliasResult.ok, false)
    assertCode(aliasResult, 'TASK_STEM_ALIAS_COLLISION')
    assertCode(aliasResult, 'TASK_NUMBER_CONFLICT')
    const unrelatedAlias = validate(aliasProject, { stem: 'TASK_24_unrelated' })
    assert.equal(core.actionAdmission(unrelatedAlias).ok, true,
      'a case/Unicode alias collision must remain scoped to its exact stems')
    assert.equal(unrelatedAlias.findings.some((item) =>
      item.code === 'TASK_STEM_ALIAS_COLLISION' || item.code === 'TASK_NUMBER_CONFLICT'), false)
  })

  check('dependency is a warning globally but a blocker at run admission', () => {
    const project = makeProject()
    const stem = 'TASK_30_runner'
    const dependency = 'TASK_31_dependency'
    writeArtifact(project, 'todo', stem, todoDoc(stem, { dependency }))

    const global = validate(project, { stem })
    assert.equal(global.ok, true, JSON.stringify(global.findings, null, 2))
    assertCode(global, 'DEPENDENCY_UNRESOLVED')
    const blocked = core.validateAction({
      action: 'run', stem, repoRoot: project.root, tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(blocked.ok, false)
    assertCode(blocked, 'RUN_DEPENDENCY_UNSATISFIED')
    const finalizeBlocked = core.validateAction({
      action: 'finalize', stem, repoRoot: project.root, tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(finalizeBlocked.ok, false)
    assertCode(finalizeBlocked, 'RUN_DEPENDENCY_UNSATISFIED')

    writeArtifact(project, 'done', dependency, doneDoc(dependency, { status: 'completed-with-caveats' }))
    const admitted = core.validateAction({
      action: 'run', stem, repoRoot: project.root, tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(admitted.ok, true, JSON.stringify(admitted.findings, null, 2))
    assert.ok(!codes(admitted).includes('RUN_DEPENDENCY_UNSATISFIED'))
    const finalizeAdmitted = core.validateAction({
      action: 'finalize', stem, repoRoot: project.root, tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(finalizeAdmitted.ok, true, JSON.stringify(finalizeAdmitted.findings, null, 2))
    assert.ok(!codes(finalizeAdmitted).includes('RUN_DEPENDENCY_UNSATISFIED'))

    writeArtifact(project, 'done', dependency, doneDoc(dependency).replace('# TASK 31 —', '# TASK 999 —'))
    const structurallyInvalidDependency = core.validateAction({
      action: 'run', stem, repoRoot: project.root, tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(structurallyInvalidDependency.ok, false)
    assertCode(structurallyInvalidDependency, 'RUN_DEPENDENCY_UNSATISFIED')
    const structurallyInvalidFinalize = core.validateAction({
      action: 'finalize', stem, repoRoot: project.root, tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(structurallyInvalidFinalize.ok, false)
    assertCode(structurallyInvalidFinalize, 'RUN_DEPENDENCY_UNSATISFIED')
  })

  check('non-bullet dependency residue is an error instead of disappearing', () => {
    const project = makeProject()
    const stem = 'TASK_35_dependency_syntax'
    const dependency = 'TASK_36_dependency'
    writeArtifact(project, 'todo', stem, todoDoc(stem, { dependency }).replace(`- ${dependency}`, dependency))
    const result = validate(project, { stem })
    assert.equal(result.ok, false)
    assertCode(result, 'DEPENDENCY_SYNTAX_INVALID')
  })

  check('thematic separators cannot truncate dependency or lineage parsing', () => {
    const project = makeProject()
    const stem = 'TASK_579_separator_dependency'
    const dependency = 'TASK_580_hidden_dependency'
    writeArtifact(project, 'todo', stem, todoDoc(stem, { dependency })
      .replace(`- ${dependency}`, `---\n- ${dependency}`))
    const result = validate(project, { stem })
    assert.equal(result.ok, false)
    assertCode(result, 'DEPENDENCY_SYNTAX_INVALID')
    assertCode(result, 'DEPENDENCY_UNRESOLVED')
    assert.deepEqual(result._model.metadata.get(stem).deps, [dependency])
    const admission = core.validateAction({
      action: 'run', stem, repoRoot: project.root, tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(admission.ok, false)
    assertCode(admission, 'RUN_DEPENDENCY_UNSATISFIED')

    const lineageProject = makeProject()
    const child = 'TASK_581_separator_lineage'
    const parent = 'TASK_582_visible_parent'
    writeArtifact(lineageProject, 'todo', child, todoDoc(child).replace(
      '\n## Acceptance\n', `\n## Origin\n\n---\n- split from ${parent}\n\n## Acceptance\n`,
    ))
    const lineage = validate(lineageProject, { stem: child })
    assert.equal(lineage.ok, false)
    assertCode(lineage, 'LINEAGE_INVALID')
    assert.deepEqual(lineage._model.metadata.get(child).lineage, [parent])
  })

  check('one dependency bullet cannot smuggle a second ASCII or Unicode task reference in its note', () => {
    for (const [index, secondReference] of [
      [37, 'TASK_39_second'],
      [40, 'ＴＡＳＫ_42_width_second'],
    ]) {
      const project = makeProject()
      const stem = `TASK_${index}_ambiguous_dependency`
      const dependency = `TASK_${index + 1}_first`
      writeArtifact(project, 'todo', stem, todoDoc(stem, { dependency })
        .replace(`- ${dependency}`, `- ${dependency} — also ${secondReference}`))
      const result = validate(project, { stem })
      assert.equal(result.ok, false)
      assertCode(result, 'DEPENDENCY_SYNTAX_INVALID')
      assert.deepEqual(Array.from(result._model.loadedStems), [stem],
        'an ambiguous bullet must produce no executable dependency edge')
    }
  })

  check('self, duplicate, and cyclic dependencies are diagnosed', () => {
    const duplicateProject = makeProject()
    const self = 'TASK_32_self'
    writeArtifact(duplicateProject, 'todo', self, todoDoc(self, { dependency: self }).replace(`- ${self}`, `- ${self}\n- ${self}`))
    const duplicate = validate(duplicateProject, { stem: self })
    assertCode(duplicate, 'DEPENDENCY_SELF')
    assertCode(duplicate, 'DEPENDENCY_DUPLICATE')

    const cycleProject = makeProject()
    const first = 'TASK_33_first'
    const second = 'TASK_34_second'
    writeArtifact(cycleProject, 'todo', first, todoDoc(first, { dependency: second }))
    writeArtifact(cycleProject, 'todo', second, todoDoc(second, { dependency: first }))
    const cycle = validate(cycleProject)
    assert.equal(cycle.ok, false)
    assertCode(cycle, 'DEPENDENCY_CYCLE')
    for (const stem of [first, second]) {
      const scoped = validate(cycleProject, { stem })
      assert.equal(scoped.ok, false, `${stem} must inherit the whole-cycle blocker`)
      assertCode(scoped, 'DEPENDENCY_CYCLE')
    }
  })

  check('INDEX freshness ignores generatedAt but rejects structural drift', () => {
    const project = makeProject()
    setState(project, 'backlog', 'TASK_40_indexed')

    const stale = validate(project, { checkIndex: true })
    assert.equal(stale.ok, false)
    assert.equal(stale.indexStatus, 'stale')
    assertCode(stale, 'INDEX_STALE')

    const published = publishFreshIndex(project)
    let fresh = validate(project, { checkIndex: true })
    assert.equal(fresh.ok, true, JSON.stringify(fresh.findings, null, 2))
    assert.equal(fresh.indexStatus, 'fresh')
    const firstSnapshotHash = fresh.snapshotHash

    published.generatedAt = '2099-12-31T23:59:59Z'
    writeFileSync(join(project.tasks, 'INDEX.json'), JSON.stringify(published, null, 2) + '\n')
    fresh = validate(project, { checkIndex: true })
    assert.equal(fresh.ok, true, JSON.stringify(fresh.findings, null, 2))
    assert.equal(fresh.indexStatus, 'fresh')
    assert.notEqual(fresh.snapshotHash, firstSnapshotHash,
      'snapshotHash must cover INDEX bytes even when volatile generatedAt is structurally ignored')

    published.backlog[0].title = 'Drifted title'
    writeFileSync(join(project.tasks, 'INDEX.json'), JSON.stringify(published, null, 2) + '\n')
    const drifted = validate(project, { checkIndex: true })
    assert.equal(drifted.ok, false)
    assert.equal(drifted.indexStatus, 'stale')
    assertCode(drifted, 'INDEX_STALE')
  })

  check('INDEX still requires a canonical generatedAt instant', () => {
    const project = makeProject()
    const stem = 'TASK_22_bad_index_time'
    writeArtifact(project, 'backlog', stem, backlogDoc(stem))
    publishFreshIndex(project)
    const published = JSON.parse(readFileSync(join(project.tasks, 'INDEX.json'), 'utf8'))
    published.generatedAt = '2026-02-31T12:00:00Z'
    writeFileSync(join(project.tasks, 'INDEX.json'), JSON.stringify(published, null, 2) + '\n')
    const result = validate(project, { stem, checkIndex: true })
    assert.equal(result.ok, false)
    assertCode(result, 'INDEX_SCHEMA_INVALID')
  })

  check('task root cannot traverse an in-project symlink ancestor', () => {
    const project = makeProject()
    const outside = makeProject('task-state-outside-')
    rmSync(join(project.root, 'orchestrator'), { recursive: true, force: true })
    symlinkSync(join(outside.root, 'orchestrator'), join(project.root, 'orchestrator'), 'dir')
    const result = core.validateTaskState({
      repoRoot: project.root,
      tasksDir: join(project.root, 'orchestrator', 'tasks'),
      outcomeShapePath: OUTCOME_SHAPE,
      includeRuntime: false,
    })
    assert.equal(result.ok, false)
    assertCode(result, 'TASK_ROOT_UNSAFE')
  })

  check('task column symlinks are rejected without enumerating their targets', () => {
    const project = makeProject()
    const outside = makeProject('task-column-outside-')
    writeArtifact(outside, 'backlog', 'TASK_24_outside_secret', backlogDoc('TASK_24_outside_secret'))
    rmSync(join(project.tasks, 'backlog'), { recursive: true, force: true })
    symlinkSync(join(outside.tasks, 'backlog'), join(project.tasks, 'backlog'), 'dir')
    const result = validate(project)
    assert.equal(result.ok, false)
    assertCode(result, 'TASK_COLUMN_UNSAFE')
    assert.equal(result._model.artifacts.has('TASK_24_outside_secret'), false)
  })

  check('single-stem name inventory does not read an unrelated symlink target while global validation rejects it', () => {
    if (process.platform === 'win32') return
    const project = makeProject()
    const target = 'TASK_25_safe_target'
    const shadow = 'TASK_26_symlink_shadow'
    setState(project, 'backlog', target)
    const outside = join(project.root, 'outside-task.md')
    writeFileSync(outside, backlogDoc(shadow))
    symlinkSync(outside, artifactPath(project, 'backlog', shadow))
    const scoped = validate(project, { stem: target })
    assert.equal(scoped.ok, true, JSON.stringify(scoped.findings, null, 2))
    assert.equal(scoped.stats.taskBodyReads, 1)
    assert.equal(scoped._model.snapshotInputs.some((item) => item.path.endsWith(`/${shadow}.md`)), false)
    const global = validate(project)
    assert.equal(global.ok, false)
    assertCode(global, 'TASK_ARTIFACT_UNSAFE')
  })

  check('configured repository root itself cannot be a symlink', () => {
    if (process.platform === 'win32') return
    const project = makeProject()
    const link = join(dirname(project.root), `task-root-link-${process.pid}-${Date.now()}`)
    roots.push(link)
    symlinkSync(project.root, link, 'dir')
    const result = core.validateTaskState({
      repoRoot: link,
      tasksDir: join(link, 'orchestrator', 'tasks'),
      outcomeShapePath: OUTCOME_SHAPE,
      includeRuntime: false,
    })
    assert.equal(result.ok, false)
    assertCode(result, 'TASK_ROOT_UNSAFE')
    assert.equal(result._model.artifacts.size, 0)
  })

  check('runtime lock scans fail closed at a bounded entry ceiling', () => {
    const project = makeProject()
    const locks = join(project.root, 'orchestrator', '.cache', 'tasks', 'locks')
    mkdirSync(locks, { recursive: true })
    writeFileSync(join(locks, 'TASK_30_first.json'), '{}\n')
    writeFileSync(join(locks, 'TASK_31_second.json'), '{}\n')
    const result = validate(project, { includeRuntime: true, locksDir: locks, maxRuntimeFiles: 1 })
    assert.equal(result.ok, false)
    assertCode(result, 'LOCK_DIRECTORY_TOO_LARGE')
    assert.doesNotMatch(codes(result).join(','), /LOCK_INVALID/,
      'once the bounded directory verdict is red, partial entry parsing must not imply completeness')
  })

  check('single-stem runtime inspection reads only matching lock records while retaining global lock-name integrity', () => {
    const project = makeProject()
    const target = 'TASK_32_runtime_target'
    const unrelated = 'TASK_33_runtime_unrelated'
    setState(project, 'todo', target)
    setState(project, 'todo', unrelated)
    const locks = join(project.root, 'orchestrator', '.cache', 'tasks', 'locks')
    mkdirSync(locks, { recursive: true })
    const unrelatedLock = join(locks, unrelated + '.json')
    writeFileSync(unrelatedLock, '{}\n')

    const scoped = validate(project, { stem: target, includeRuntime: true, locksDir: locks })
    assert.equal(scoped.ok, true, JSON.stringify(scoped.findings, null, 2))
    assert.equal(scoped._model.snapshotInputs.some((item) => item.kind === 'file' && item.path.endsWith(`/${unrelated}.json`)), false)
    assert.equal(scoped._model.snapshotInputs.some((item) => item.kind === 'directory' && item.path.endsWith('/locks')), true)

    const global = validate(project, { includeRuntime: true, locksDir: locks })
    assert.equal(global.ok, false)
    assertCode(global, 'LOCK_INVALID')
    assert.equal(global._model.snapshotInputs.some((item) => item.kind === 'file' && item.path.endsWith(`/${unrelated}.json`)), true)

    writeFileSync(join(locks, 'not-a-task.json'), '{}\n')
    const invalidName = validate(project, { stem: target, includeRuntime: true, locksDir: locks })
    assert.equal(invalidName.ok, false)
    const finding = invalidName.findings.find((item) => item.code === 'LOCK_INVALID')
    assert.equal(finding.stem, null, 'invalid lock identity remains a global name-inventory blocker')
  })

  check('runtime lock-age findings are deterministic for their hashed threshold classification', () => {
    const project = makeProject()
    const stem = 'TASK_79_runtime_clock'
    setState(project, 'todo', stem)
    const lock = writeCanonicalLock(project, stem)
    const startedAt = '2026-07-13T00:00:00.000Z'
    lock.record.startedAt = startedAt
    lock.record.owner.startedAt = startedAt
    writeFileSync(lock.file, JSON.stringify(lock.record, null, 2) + '\n')
    const threshold = Date.parse(startedAt) + 6 * 60 * 60 * 1000
    const options = { stem, includeRuntime: true, locksDir: lock.locks, nowMs: threshold }
    const first = validate(project, options)
    const repeated = validate(project, options)
    assert.equal(first.snapshotHash, repeated.snapshotHash)
    assert.deepEqual(first.findings, repeated.findings)
    assert.equal(codes(first).includes('LOCK_LIVENESS_UNPROVEN'), false)
    const aged = validate(project, { ...options, nowMs: threshold + 1 })
    assertCode(aged, 'LOCK_LIVENESS_UNPROVEN')
    assert.notEqual(aged.snapshotHash, first.snapshotHash,
      'a clock change that changes the verdict must change the snapshot hash')
    assert.equal(aged._model.snapshotInputs.some((item) =>
      item.kind === 'lock-age-classification' && item.old === true), true)
  })

  check('finalize admits done recovery only for one active marker bound to the retained orchestrator-lock generation', () => {
    const project = makeProject()
    const stem = 'TASK_33_finalize_recovery'
    setState(project, 'todo', stem)
    const fresh = core.validateAction({
      action: 'finalize', stem, repoRoot: project.root, tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: false,
    })
    assert.equal(fresh.ok, true, JSON.stringify(fresh.findings, null, 2))
    assert.equal(fresh.action, 'finalize')

    const reopenedDependency = 'TASK_330_reopened_dependency'
    clearStem(project, stem)
    writeArtifact(project, 'done', stem, doneDoc(stem, { dependency: reopenedDependency }))
    writeArtifact(project, 'todo', reopenedDependency, todoDoc(reopenedDependency))
    const lock = writeCanonicalLock(project, stem)
    const generation = ownedLockGeneration(lock.file)
    assert.equal(core.lockGenerationHash({ ...generation.proof, ctimeNs: '9999999999' }), generation.hash,
      'ctime-only hard-link proof changes must not invent a new owned-file generation')
    const foreignGeneration = core.lockGenerationHash({
      ...generation.proof, ino: String(BigInt(generation.proof.ino) + 1n),
    })
    assert.notEqual(foreignGeneration, generation.hash,
      'same lock bytes in a replacement inode must never satisfy recovery ownership')

    const markerStatus = (lockGenerationHash, state = 'running') => ({
      owner: 'finalizations', kind: 'marker', state, stem, phase: 'verify', revision: 7,
      contentHash: 'sha256:' + 'c'.repeat(64), lockGenerationHash,
    })
    const envelope = (statuses) => ({
      version: 1, scope: stem, ok: true, statuses, findings: [], truncated: false,
      snapshotInputs: statuses.length ? [{
        owner: 'finalizations', kind: 'marker',
        path: `orchestrator/.cache/tasks/finalizations/${stem}.json`,
        hash: 'sha256:' + 'd'.repeat(64), size: 512,
      }] : [],
      stats: { statuses: statuses.length, findings: 0, snapshotInputs: statuses.length ? 1 : 0 },
    })
    const options = {
      action: 'finalize', stem, repoRoot: project.root, tasksDir: project.tasks,
      outcomeShapePath: OUTCOME_SHAPE, includeRuntime: true, locksDir: lock.locks,
    }

    const admitted = core.validateAction({
      ...options, runtimeInspector: () => envelope([markerStatus(generation.hash)]),
    })
    assert.equal(admitted.ok, true, JSON.stringify(admitted.findings, null, 2))
    assertCode(admitted, 'DEPENDENCY_UNRESOLVED')
    assert.equal(codes(admitted).includes('RUN_DEPENDENCY_UNSATISFIED'), false,
      'done recovery must not be stranded when a dependency is later reopened')
    assert.equal(codes(admitted).includes('LOCK_STAGE_STATE_MISMATCH'), false)
    assert.equal(admitted.runtimeStatus[0].lockGenerationHash, generation.hash)
    assert.equal(admitted._model.runtimeLocks.get(stem).generationHash, generation.hash)
    assert.equal(admitted._model.snapshotInputs.some((item) =>
      item.kind === 'lock-generation' && item.hash === generation.hash), true)

    const arbitraryDone = core.validateAction({
      ...options, runtimeInspector: () => envelope([]),
    })
    assert.equal(arbitraryDone.ok, false)
    assertCode(arbitraryDone, 'FINALIZE_RECOVERY_MARKER_UNPROVEN')
    assertCode(arbitraryDone, 'LOCK_STAGE_STATE_MISMATCH')

    const replacedLock = core.validateAction({
      ...options, runtimeInspector: () => envelope([markerStatus(foreignGeneration)]),
    })
    assert.equal(replacedLock.ok, false)
    assertCode(replacedLock, 'FINALIZE_RECOVERY_LOCK_GENERATION_MISMATCH')
    assertCode(replacedLock, 'LOCK_STAGE_STATE_MISMATCH')

    const inactiveMarker = core.validateAction({
      ...options, runtimeInspector: () => envelope([markerStatus(generation.hash, 'recovery-required')]),
    })
    assert.equal(inactiveMarker.ok, false)
    assertCode(inactiveMarker, 'FINALIZE_RECOVERY_MARKER_UNPROVEN')

    unlinkSync(lock.file)
    const missingLock = core.validateAction({
      ...options, runtimeInspector: () => envelope([markerStatus(generation.hash)]),
    })
    assert.equal(missingLock.ok, false)
    assertCode(missingLock, 'FINALIZE_RECOVERY_LOCK_UNPROVEN')
  })

  check('runtime composite seam validates, merges, hashes, and fails closed without schema re-parsing', () => {
    const project = makeProject()
    const stem = 'TASK_34_runtime_composite'
    setState(project, 'todo', stem)
    const envelope = (hash, overrides = {}) => ({
      version: 1,
      scope: stem,
      ok: true,
      statuses: [{ owner: 'requests', kind: 'reservation', state: 'active', stem, revision: 1 }],
      findings: [],
      snapshotInputs: [{ owner: 'requests', kind: 'reservation', path: `runtime/${stem}.json`, hash, size: 42 }],
      truncated: false,
      stats: { statuses: 1, findings: 0, snapshotInputs: 1 },
      ...overrides,
    })
    const first = validate(project, {
      stem, includeRuntime: true,
      runtimeInspector: ({ stem: requested, roots: runtimeRoots }) => {
        assert.equal(requested, stem)
        assert.deepEqual(Object.keys(runtimeRoots).sort(), [
          'projectRoot', 'tasksDir', 'locksDir', 'requestsDir', 'requestReservationsDir',
          'runsDir', 'finalizationsDir', 'writerLeasesDir', 'taskCreationsDir',
          'taskEditsDir', 'taskIntakeDir', 'transitionsDir', 'journalDir',
          'writerAuthorityRoot', 'taskCreationsAuthorityRoot', 'taskEditsAuthorityRoot',
        ].sort())
        for (const value of Object.values(runtimeRoots)) {
          const rel = relative(project.root, value)
          assert.equal(rel === '..' || rel.startsWith('../'), false,
            `runtime root escaped the validation fixture: ${value}`)
        }
        return envelope('sha256:' + 'a'.repeat(64))
      },
    })
    assert.equal(first.ok, true, JSON.stringify(first.findings, null, 2))
    assert.equal(first.runtimeStats.inspected, true)
    assert.equal(first.runtimeStats.statuses, 1)
    assert.equal(first.runtimeStatus[0].owner, 'requests')
    assert.equal(first._model.snapshotInputs.some((item) => item.kind === 'runtime-composite' && item.owner === 'requests'), true)

    const second = validate(project, {
      stem, includeRuntime: true,
      runtimeInspector: () => envelope('sha256:' + 'b'.repeat(64)),
    })
    assert.notEqual(second.snapshotHash, first.snapshotHash,
      'owner-provided runtime hashes must fence the canonical verdict snapshot')

    const statusA = { owner: 'a-owner', kind: 'record', state: 'active', stem }
    const statusZ = { owner: 'z-owner', kind: 'record', state: 'active', stem }
    const snapshotA = { owner: 'a-owner', kind: 'record', path: 'runtime/a.json', hash: 'sha256:' + '1'.repeat(64) }
    const snapshotZ = { owner: 'z-owner', kind: 'record', path: 'runtime/z.json', hash: 'sha256:' + '2'.repeat(64) }
    const withOrder = (statuses, snapshotInputs) => envelope('sha256:' + 'd'.repeat(64), {
      statuses, snapshotInputs, stats: { statuses: 2, findings: 0, snapshotInputs: 2 },
    })
    const ordered = validate(project, {
      stem, includeRuntime: true,
      runtimeInspector: () => withOrder([statusA, statusZ], [snapshotA, snapshotZ]),
    })
    const reversed = validate(project, {
      stem, includeRuntime: true,
      runtimeInspector: () => withOrder([statusZ, statusA], [snapshotZ, snapshotA]),
    })
    assert.equal(reversed.snapshotHash, ordered.snapshotHash,
      'transport array order must not make an otherwise identical runtime snapshot nondeterministic')
    assert.deepEqual(reversed.runtimeStatus, ordered.runtimeStatus)

    const truncated = validate(project, {
      stem, includeRuntime: true,
      runtimeInspector: () => envelope('sha256:' + 'c'.repeat(64), {
        ok: false, statuses: [], snapshotInputs: [], truncated: true,
        stats: { statuses: 0, findings: 0, snapshotInputs: 0 },
      }),
    })
    assert.equal(truncated.ok, false)
    assertCode(truncated, 'RUNTIME_COMPOSITE_TRUNCATED')

    const unavailable = validate(project, {
      stem, includeRuntime: true,
      runtimeInspector: () => { throw new Error('owner unavailable') },
    })
    assert.equal(unavailable.ok, false)
    assertCode(unavailable, 'RUNTIME_INSPECTOR_UNAVAILABLE')

    const unidentifiedSnapshot = validate(project, {
      stem, includeRuntime: true,
      runtimeInspector: () => envelope('sha256:' + 'e'.repeat(64), {
        snapshotInputs: [{ owner: 'requests', kind: 'reservation', path: '', hash: 'sha256:' + 'e'.repeat(64) }],
      }),
    })
    assertCode(unidentifiedSnapshot, 'RUNTIME_INSPECTOR_UNAVAILABLE')
  })

  check('durable CAS task publication is owner-red, fully hashed, and green only after exact recovery cleanup', () => {
    const project = makeProject()
    const stem = 'TASK_35_durable_cas_task'
    setState(project, 'backlog', stem)
    const candidate = Buffer.from(backlogDoc(stem, 'Conditionally replaced task'))
    const operation = createDurableCas(join(project.tasks, 'backlog'), `${stem}.md`, {
      digit: '1', candidate, expectedFile: artifactPath(project, 'backlog', stem),
    })

    const active = validate(project, { stem, includeRuntime: false })
    assert.equal(active.ok, false)
    assertCode(active, 'DURABLE_CAS_RECOVERY_REQUIRED')
    assert.equal(active.runtimeStats.inspected, false,
      'durable publication ownership must be visible even when the optional runtime composite is disabled')
    for (const name of ['manifest.json', 'candidate']) {
      assert.equal(active._model.snapshotInputs.some((item) => item.kind === 'file' && item.path.endsWith(`/${name}`)), true,
        `${name} bytes must contribute to the canonical snapshot`)
    }
    const activeHash = active.snapshotHash

    writeFileSync(join(operation, 'candidate'), Buffer.from('tampered candidate generation\n'))
    const changed = validate(project, { stem, includeRuntime: false })
    assert.equal(changed.ok, false)
    assertCode(changed, 'DURABLE_CAS_OPERATION_INVALID')
    assert.notEqual(changed.snapshotHash, activeHash,
      'changing a private candidate generation must change the verdict fence')

    rmSync(operation, { recursive: true })
    const recovered = validate(project, { stem, includeRuntime: false })
    assert.equal(recovered.ok, true, JSON.stringify(recovered.findings, null, 2))
  })

  check('scoped admission cannot hide unknown, malformed, or symlinked durable CAS ownership', () => {
    const project = makeProject()
    const stem = 'TASK_36_durable_cas_scope'
    setState(project, 'todo', stem)
    const backlog = join(project.tasks, 'backlog')

    const unknown = createDurableCas(backlog, 'unused.md', { digit: '2', candidate: null, manifest: false })
    const unknownResult = validate(project, { stem, includeRuntime: false })
    assert.equal(unknownResult.ok, false)
    const unknownFinding = unknownResult.findings.find((item) => item.code === 'DURABLE_CAS_RECOVERY_REQUIRED')
    assert.equal(unknownFinding.stem, null, 'an unattributable operation must remain a global scoped blocker')
    rmSync(unknown, { recursive: true })

    const lookalike = join(backlog, durableCas.PREFIX + 'not-canonical')
    writeFileSync(lookalike, 'do not trust this path\n')
    const malformed = validate(project, { stem, includeRuntime: false })
    assert.equal(malformed.ok, false)
    assertCode(malformed, 'DURABLE_CAS_NAME_UNSAFE')
    writeFileSync(lookalike, 'changed but same byte!\n')
    const malformedChanged = validate(project, { stem, includeRuntime: false })
    assert.notEqual(malformedChanged.snapshotHash, malformed.snapshotHash,
      'even an opaque malformed CAS lookalike must contribute its bounded bytes')
    rmSync(lookalike)

    const outside = join(project.root, 'outside-durable-cas')
    mkdirSync(outside)
    writeFileSync(join(outside, 'secret'), 'must not be followed\n')
    const linked = join(backlog, durableCasName('3'))
    symlinkSync(outside, linked)
    const unsafe = validate(project, { stem, includeRuntime: false })
    assert.equal(unsafe.ok, false)
    assertCode(unsafe, 'DURABLE_CAS_OPERATION_UNSAFE')
    assert.equal(unsafe._model.snapshotInputs.some((item) => String(item.path).includes('/secret')), false,
      'a canonical-name symlink must never be traversed')
  })

  check('creation and edit CAS roots gate actions independently of the optional runtime composite', () => {
    const project = makeProject()
    const stem = 'TASK_37_durable_cas_runtime_roots'
    setState(project, 'backlog', stem)
    const taskRuntime = join(project.root, 'orchestrator', '.cache', 'tasks')
    const creations = join(taskRuntime, 'creations')
    const edits = join(taskRuntime, 'edits')
    const creationOperation = createDurableCas(creations, 'unused.json', { digit: '4', candidate: null, manifest: false })
    const editOperation = createDurableCas(edits, 'unused.json', { digit: '5', candidate: null, manifest: false })

    const blocked = validate(project, { stem, includeRuntime: false })
    assert.equal(blocked.ok, false)
    assert.equal(blocked.runtimeStats.inspected, false)
    assert.equal(blocked.findings.filter((item) => item.code === 'DURABLE_CAS_RECOVERY_REQUIRED').length, 2)
    for (const operation of [creationOperation, editOperation]) {
      assert.equal(blocked._model.snapshotInputs.some((item) => item.kind === 'durable-cas-operation' && item.path.includes(operation.split('/').pop())), true)
    }

    rmSync(creationOperation, { recursive: true })
    rmSync(editOperation, { recursive: true })
    writeFileSync(join(creations, 'ordinary-completed-marker.json'), '{}\n')
    writeFileSync(join(edits, 'ordinary-completed-marker.json'), '{}\n')
    const clear = validate(project, { stem, includeRuntime: false })
    assert.equal(clear.ok, true, JSON.stringify(clear.findings, null, 2))
  })

  check('durable CAS artifact lattice, detached source hash, no-follow, and operation bound fail closed', () => {
    const project = makeProject()
    const stem = 'TASK_38_durable_cas_contract'
    setState(project, 'backlog', stem)
    const backlog = join(project.tasks, 'backlog')
    const candidate = Buffer.from(backlogDoc(stem, 'Candidate'))
    const source = Buffer.from(backlogDoc(stem, 'Detached source'))
    let operation = createDurableCas(backlog, `${stem}.md`, { digit: '6', candidate, source })

    const detached = validate(project, { stem, includeRuntime: false })
    assert.equal(detached.ok, false)
    assertCode(detached, 'DURABLE_CAS_RECOVERY_REQUIRED')
    assert.equal(codes(detached).includes('DURABLE_CAS_OPERATION_INVALID'), false,
      'a canonical detached snapshot should be recovery-required, not structurally invalid')
    for (const name of ['manifest.json', 'candidate', 'source']) {
      assert.equal(detached._model.snapshotInputs.some((item) => item.kind === 'file' && item.path.endsWith(`/${name}`)), true)
    }

    const before = detached.snapshotHash
    writeFileSync(join(operation, 'source'), 'foreign detached source\n')
    const changedSource = validate(project, { stem, includeRuntime: false })
    assertCode(changedSource, 'DURABLE_CAS_OPERATION_INVALID')
    assert.notEqual(changedSource.snapshotHash, before)

    rmSync(join(operation, 'source'))
    const outside = join(project.root, 'outside-source')
    writeFileSync(outside, 'outside bytes\n')
    symlinkSync(outside, join(operation, 'source'))
    const linkedSource = validate(project, { stem, includeRuntime: false })
    assertCode(linkedSource, 'DURABLE_CAS_OPERATION_INVALID')
    assert.equal(linkedSource._model.snapshotInputs.some((item) => item.kind === 'file' && item.path === 'outside-source'), false)

    rmSync(operation, { recursive: true })
    operation = createDurableCas(backlog, 'unused.md', { digit: '7', candidate: null, manifest: false })
    for (let index = 0; index < 9; index++) writeFileSync(join(operation, `unexpected-${index}`), String(index))
    const overBound = validate(project, { stem, includeRuntime: false })
    assert.equal(overBound.ok, false)
    assertCode(overBound, 'DURABLE_CAS_OPERATION_INVALID')
    assertCode(overBound, 'DURABLE_CAS_RECOVERY_REQUIRED')
    const operationSnapshot = overBound._model.snapshotInputs.find((item) => item.kind === 'durable-cas-operation')
    assert.equal(operationSnapshot.names.length, 9)
  })

  check('CLI runtime composition derives every default root from the isolated validation project', () => {
    const project = makeProject()
    const stem = 'TASK_35_runtime_root_isolation'
    setState(project, 'todo', stem)
    const runs = join(project.root, 'orchestrator', '.cache', 'tasks', 'runs')
    mkdirSync(runs, { recursive: true })
    writeFileSync(join(runs, `task_${stem}.session.json`), '{}\n')
    const unsetIndividualRoots = {
      ORCHESTRATOR_CACHE_DIR: null,
      ORCHESTRATOR_LOCKS_DIR: null,
      ORCHESTRATOR_REQUESTS_DIR: null,
      ORCHESTRATOR_REQUEST_RESERVATIONS_DIR: null,
      ORCHESTRATOR_RUNS_DIR: null,
      ORCHESTRATOR_FINALIZATIONS_DIR: null,
      ORCHESTRATOR_WRITER_LEASES_DIR: null,
      ORCHESTRATOR_TASK_CREATIONS_DIR: null,
      ORCHESTRATOR_TASK_EDITS_DIR: null,
      ORCHESTRATOR_TASK_INTAKE_DIR: null,
      ORCHESTRATOR_TRANSITIONS_DIR: null,
      ORCHESTRATOR_JOURNAL_DIR: null,
      ORCHESTRATOR_WRITER_AUTHORITY_ROOT: null,
      ORCHESTRATOR_TASK_CREATIONS_AUTHORITY_ROOT: null,
      ORCHESTRATOR_TASK_EDITS_AUTHORITY_ROOT: null,
    }
    const cli = runCli(project, ['--stem', stem, '--json'], undefined, unsetIndividualRoots)
    assert.equal(cli.status, 1, cli.stderr + cli.stdout)
    const envelope = parseCliJson(cli)
    assertCode(envelope, 'SESSION_SIDECAR_INVALID')
    assert.equal(codes(envelope).some((code) => code.endsWith('_DIRECTORY_UNSAFE')), false,
      'a PROJECT_ROOT-only fixture must not inspect canonical runtime directories')
    assert.equal(envelope.runtimeStats.snapshotInputs, 3,
      'runtime snapshot must cover writer leases, the invalid sidecar, and the normalized runtime verdict/context fence')
    for (const item of envelope.findings) for (const itemPath of item.paths) {
      const absolutePath = isAbsolute(itemPath) ? itemPath : join(project.root, itemPath)
      const rel = relative(project.root, absolutePath)
      assert.equal(rel === '..' || rel.startsWith('../'), false, `runtime finding escaped fixture: ${itemPath}`)
      assert.equal(absolutePath.startsWith(CANONICAL_PROJECT_ROOT + '/orchestrator/.cache/'), false)
    }

    const probe = [
      `const runtime = require(${JSON.stringify(RUNTIME_INTEGRITY)});`,
      'const roots = runtime.loadedRoots();',
      `let omittedRootsRejected = false; try { runtime.scanIntegrity({ stem: ${JSON.stringify(stem)} }); } catch (_) { omittedRootsRejected = true; }`,
      `let mismatchedRootsRejected = false; try { runtime.scanIntegrity({ stem: ${JSON.stringify(stem)}, roots: { ...roots, runsDir: roots.runsDir + '-foreign' } }); } catch (_) { mismatchedRootsRejected = true; }`,
      `const result = runtime.scanIntegrity({ stem: ${JSON.stringify(stem)}, roots });`,
      'process.stdout.write(JSON.stringify({ roots, snapshotInputs: result.snapshotInputs, omittedRootsRejected, mismatchedRootsRejected }));',
    ].join('\n')
    const probeEnv = { ...cliEnv(project), ...unsetIndividualRoots }
    for (const [name, value] of Object.entries(probeEnv)) if (value === null || value === undefined) delete probeEnv[name]
    const inspected = spawnSync(process.execPath, ['-e', probe], {
      cwd: project.root, env: probeEnv, encoding: 'utf8', maxBuffer: 4 * 1024 * 1024,
    })
    assert.equal(inspected.status, 0, inspected.stderr + inspected.stdout)
    const observed = JSON.parse(inspected.stdout)
    assert.equal(observed.omittedRootsRejected, true, 'runtime composite must require the exact root envelope')
    assert.equal(observed.mismatchedRootsRejected, true, 'runtime composite must reject a single mismatched runtime root')
    for (const root of Object.values(observed.roots)) {
      const rel = relative(project.root, root)
      assert.equal(rel === '..' || rel.startsWith('../'), false, `loaded runtime root escaped fixture: ${root}`)
    }
    assert.equal(observed.snapshotInputs.length, 3)
    assert.deepEqual(observed.snapshotInputs.map((item) => `${item.owner}/${item.kind}`).sort(),
      ['composite/verdict', 'finalizations/writer-lease-directory', 'sessions/sidecar'])
    for (const input of observed.snapshotInputs) {
      const rel = relative(project.root, isAbsolute(input.path) ? input.path : join(project.root, input.path))
      assert.equal(rel === '..' || rel.startsWith('../'), false, `runtime snapshot escaped fixture: ${input.path}`)
    }
  })

  check('a missing INDEX is a durable integrity finding, not an endless transient race', () => {
    const project = makeProject()
    setState(project, 'backlog', 'TASK_41_missing_index')
    unlinkSync(join(project.tasks, 'INDEX.json'))
    const result = validate(project, { checkIndex: true })
    assert.equal(result.ok, false)
    assert.equal(result.indexStatus, 'invalid')
    assertCode(result, 'INDEX_UNSAFE')
  })

  check('snapshotHash covers the outcome contract that contributes to the verdict', () => {
    const project = makeProject()
    const stem = 'TASK_42_contract_hash'
    setState(project, 'done', stem)
    const contractPath = join(project.root, 'outcome-shape.json')
    const original = JSON.parse(readFileSync(OUTCOME_SHAPE, 'utf8'))
    writeFileSync(contractPath, JSON.stringify(original))
    const before = validate(project, { stem, outcomeShapePath: contractPath })
    assert.equal(before.ok, true, JSON.stringify(before.findings, null, 2))

    writeFileSync(contractPath, JSON.stringify({ ...original, reviewerValid: ['internal-reviewer'] }))
    const after = validate(project, { stem, outcomeShapePath: contractPath })
    assert.equal(after.ok, false)
    assertCode(after, 'DONE_OUTCOME_INVALID')
    assert.notEqual(after.snapshotHash, before.snapshotHash,
      'a contract change that changes the verdict must change the snapshot hash')
  })

  check('Outcome acceptance verdicts come only from the versioned contract', () => {
    const project = makeProject()
    const stem = 'TASK_43_contract_verdict'
    writeArtifact(project, 'done', stem, doneDoc(stem, {
      acceptance: '`test/contract.mjs` — certified — Passed.',
    }))
    const contractPath = join(project.root, 'outcome-shape.json')
    const original = JSON.parse(readFileSync(OUTCOME_SHAPE, 'utf8'))
    writeFileSync(contractPath, JSON.stringify({ ...original, acceptanceVerdicts: ['certified'] }))

    const result = validate(project, { stem, outcomeShapePath: contractPath })
    assert.equal(result.ok, true, JSON.stringify(result.findings, null, 2))
  })

  check('CLI accepts exactly the documented transition allow-list in pre and post phases', () => {
    const expected = [
      'absent:backlog', 'backlog:absent', 'backlog:pending', 'backlog:todo',
      'corrupt:absent', 'done:absent', 'done:todo',
      'pending:absent', 'pending:pending', 'pending:todo',
      'todo:absent', 'todo:done',
    ]
    assert.deepEqual(Array.from(core.ALLOWED_TRANSITIONS).sort(), expected)

    const project = makeProject()
    const stateStems = {
      absent: 'TASK_50_absent',
      backlog: 'TASK_51_backlog',
      pending: 'TASK_52_pending',
      todo: 'TASK_53_todo',
      done: 'TASK_54_done',
      corrupt: 'TASK_59_corrupt',
    }
    for (const [state, stem] of Object.entries(stateStems)) setState(project, state, stem)
    writeArtifact(project, 'backlog', stateStems.corrupt, backlogDoc(stateStems.corrupt))
    writeArtifact(project, 'todo', stateStems.corrupt, todoDoc(stateStems.corrupt))
    for (const transition of expected) {
      const [from, to] = transition.split(':')
      for (const [phase, state] of [['pre', from], ['post', to]]) {
        const result = runCli(project, [
          '--stem', stateStems[state], '--transition', transition, '--phase', phase, '--json',
        ])
        if (transition === 'corrupt:absent' && phase === 'pre') {
          assert.equal(result.status, 1, `${transition} ${phase}: ${result.stderr}${result.stdout}`)
          const envelope = parseCliJson(result)
          assert.equal(envelope.observedState, 'corrupt')
          assert.equal(envelope.expectedState, 'corrupt')
          assertCode(envelope, 'TASK_PRESENT_IN_MULTIPLE_STATES')
          continue
        }
        assert.equal(result.status, 0, `${transition} ${phase}: ${result.stderr}${result.stdout}`)
        const envelope = parseCliJson(result)
        assert.equal(envelope.ok, true)
        assert.equal(envelope.observedState, state)
        assert.equal(envelope.expectedState, state)
        assert.equal(envelope.transition, transition)
        assert.equal(envelope.phase, phase)
      }
    }
  })

  check('CLI action mode is the exact enqueue/execution admission authority', () => {
    const project = makeProject()
    const stems = {
      backlog: 'TASK_55_action_backlog',
      pending: 'TASK_56_action_pending',
      todo: 'TASK_57_action_todo',
      done: 'TASK_58_action_done',
    }
    for (const [state, stem] of Object.entries(stems)) setState(project, state, stem)
    publishFreshIndex(project)

    for (const [action, state] of [
      ['prep', 'backlog'], ['answers', 'pending'], ['run', 'todo'],
      ['drop', 'backlog'], ['drop', 'pending'], ['drop', 'todo'], ['drop', 'done'],
      ['reopen', 'done'],
    ]) {
      const result = runCli(project, [
        '--stem', stems[state], '--action', action, '--check-index', '--json', '--caller', 'standby',
      ])
      assert.equal(result.status, 0, `${action}/${state}: ${result.stderr}${result.stdout}`)
      const envelope = parseCliJson(result)
      assert.equal(envelope.ok, true)
      assert.equal(envelope.action, action)
      assert.equal(envelope.observedState, state)
      assert.equal(envelope.indexStatus, 'fresh')
      assert.match(envelope.sourceRevision, /^sha256:[a-f0-9]{64}$/)
    }

    const staleAction = runCli(project, [
      '--stem', stems.backlog, '--action', 'run', '--check-index', '--json',
    ])
    assert.equal(staleAction.status, 1, staleAction.stderr + staleAction.stdout)
    assertCode(parseCliJson(staleAction), 'REQUEST_ACTION_STATE_MISMATCH')

    for (const args of [
      ['--stem', stems.todo, '--action', 'unknown', '--json'],
      ['--stem', stems.todo, '--action', 'run', '--expect', 'todo', '--json'],
      ['--action', 'run', '--json'],
    ]) {
      const invalid = runCli(project, args)
      assert.equal(invalid.status, 2, `${args.join(' ')}: ${invalid.stderr}${invalid.stdout}`)
    }
  })

  check('CLI validates same-state proposal bytes read-only before publication', () => {
    const project = makeProject()
    const stem = 'TASK_59_proposal'
    setState(project, 'backlog', stem)
    publishFreshIndex(project)
    const before = treeSnapshot(project.root)
    const current = validate(project, { stem, expect: 'backlog' })

    const proposed = backlogDoc(stem, 'Edited proposal').replace('Initial request.', 'Still a valid backlog request.')
    const accepted = runCli(project, [
      '--stem', stem, '--expect', 'backlog', '--proposal', '-',
      '--proposal-state', 'backlog', '--json', '--caller', 'server',
    ], proposed)
    assert.equal(accepted.status, 0, accepted.stderr + accepted.stdout)
    const acceptedEnvelope = parseCliJson(accepted)
    assert.equal(acceptedEnvelope.ok, true)
    assert.equal(acceptedEnvelope.proposalState, 'backlog')
    assert.notEqual(acceptedEnvelope.sourceRevision, current.sourceRevision,
      'proposal verdict must identify the proposed bytes, not the on-disk source')
    assert.deepEqual(treeSnapshot(project.root), before, 'accepted proposal validation mutated the project')

    const rejected = runCli(project, [
      '--stem', stem, '--expect', 'backlog', '--proposal', '-',
      '--proposal-state', 'backlog', '--json',
    ], backlogDoc(stem, 'Invalid live outcome').replace('Initial request.', '## Outcome\n\n**Status**: completed'))
    assert.equal(rejected.status, 1, rejected.stderr + rejected.stdout)
    assertCode(parseCliJson(rejected), 'OUTCOME_FORBIDDEN_IN_LIVE_STATE')
    assert.deepEqual(treeSnapshot(project.root), before, 'rejected proposal validation mutated the project')

    const oversized = runCli(project, [
      '--stem', stem, '--expect', 'backlog', '--proposal', '-',
      '--proposal-state', 'backlog', '--json',
    ], Buffer.alloc(8 * 1024 * 1024 + 1, 0x61))
    assert.equal(oversized.status, 2, oversized.stderr + oversized.stdout)
    assertCode(parseCliJson(oversized), 'INVOCATION_INVALID')
    assert.match(parseCliJson(oversized).findings[0].message, /exceeds 8388608 bytes/)
    assert.deepEqual(treeSnapshot(project.root), before, 'oversized proposal validation mutated the project')

    for (const args of [
      ['--stem', stem, '--proposal', '-', '--json'],
      ['--stem', stem, '--proposal-state', 'backlog', '--json'],
      ['--stem', stem, '--proposal', '-', '--proposal-state', 'todo', '--json'],
      ['--stem', stem, '--proposal', '-', '--proposal-state', 'backlog', '--check-index', '--json'],
      ['--stem', stem, '--proposal', '-', '--proposal-state', 'backlog', '--action', 'prep', '--json'],
    ]) {
      const invalid = runCli(project, args, proposed)
      assert.equal(invalid.status, 2, `${args.join(' ')}: ${invalid.stderr}${invalid.stdout}`)
    }
    assert.deepEqual(treeSnapshot(project.root), before, 'invalid proposal invocation mutated the project')
  })

  check('CLI validates exact absent-to-backlog proposal bytes without creating durable state', () => {
    const project = makeProject()
    const stem = 'TASK_68_virtual_create'
    const before = treeSnapshot(project.root)
    const accepted = runCli(project, [
      '--stem', stem, '--expect', 'backlog', '--proposal', '-',
      '--proposal-state', 'backlog', '--proposal-from-state', 'absent',
      '--json', '--caller', 'server',
    ], backlogDoc(stem, 'Virtual create').replace('Initial request.', '## Goal\n\nCapture an idea.'))
    assert.equal(accepted.status, 0, accepted.stderr + accepted.stdout)
    const acceptedEnvelope = parseCliJson(accepted)
    assert.equal(acceptedEnvelope.observedState, 'backlog')
    assert.equal(acceptedEnvelope.proposalState, 'backlog')
    assert.equal(acceptedEnvelope.proposalFromState, 'absent')
    assert.deepEqual(treeSnapshot(project.root), before,
      'accepted absent proposal validation mutated the project')

    const rejected = runCli(project, [
      '--stem', stem, '--proposal', '-', '--proposal-state', 'backlog',
      '--proposal-from-state', 'absent', '--json',
    ], backlogDoc(stem, 'Broken create').replace('Initial request.', '## Outcome\n\nMust never enter backlog.'))
    assert.equal(rejected.status, 1, rejected.stderr + rejected.stdout)
    assertCode(parseCliJson(rejected), 'OUTCOME_FORBIDDEN_IN_LIVE_STATE')
    assert.deepEqual(treeSnapshot(project.root), before,
      'rejected absent proposal validation mutated the project')

    for (const args of [
      ['--stem', stem, '--proposal', '-', '--proposal-state', 'todo', '--proposal-from-state', 'absent', '--json'],
      ['--stem', stem, '--proposal-from-state', 'absent', '--json'],
    ]) {
      const invalid = runCli(project, args, '# TASK 68 — Invalid proposal mode\n')
      assert.equal(invalid.status, 2, invalid.stderr + invalid.stdout)
      assertCode(parseCliJson(invalid), 'INVOCATION_INVALID')
    }
    assert.deepEqual(treeSnapshot(project.root), before,
      'invalid absent proposal invocation mutated the project')
  })

  check('proposal path reads are ancestor-anchored and reject a restored directory swap with exit 4', () => {
    const project = makeProject()
    const stem = 'TASK_67_proposal_ancestor'
    setState(project, 'backlog', stem)
    const proposals = join(project.root, 'proposals')
    const foreign = join(project.root, 'foreign-proposals')
    mkdirSync(proposals)
    mkdirSync(foreign)
    const proposalName = 'candidate.md'
    const proposal = '# TASK 67 — Anchored proposal\n\nCandidate bytes.\n'
    writeFileSync(join(proposals, proposalName), proposal)
    writeFileSync(join(foreign, proposalName), '# TASK 67 — Foreign proposal\n\nMust not be read.\n')
    const before = {
      proposals: readdirSync(proposals).sort(), foreign: readdirSync(foreign).sort(),
      proposalHash: hashFile(join(proposals, proposalName)), foreignHash: hashFile(join(foreign, proposalName)),
    }
    const raced = runCli(project, [
      '--stem', stem, '--proposal', join(proposals, proposalName), '--proposal-state', 'backlog', '--json',
    ], undefined, {
      TASK_FS_TEST_ROOT: project.root,
      TASK_FS_TEST_SWAP_PATH: proposals,
      TASK_FS_TEST_SWAP_WITH: foreign,
      TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY: '1',
    })
    assert.equal(raced.status, 4, raced.stderr + raced.stdout)
    assertCode(parseCliJson(raced), 'SNAPSHOT_RACE')
    assert.deepEqual({
      proposals: readdirSync(proposals).sort(), foreign: readdirSync(foreign).sort(),
      proposalHash: hashFile(join(proposals, proposalName)), foreignHash: hashFile(join(foreign, proposalName)),
    }, before, 'proposal race fixture must restore and preserve both generations')
  })

  check('pure cross-state proposal validates the exact todo-to-done candidate without filesystem mutation', () => {
    const project = makeProject()
    const stem = 'TASK_64_virtual_done'
    setState(project, 'todo', stem)
    const before = treeSnapshot(project.root)
    const accepted = validate(project, {
      stem,
      transition: 'todo:done',
      phase: 'post',
      proposal: { stem, fromState: 'todo', state: 'done', bytes: Buffer.from(doneDoc(stem, {
        acceptance: '`render states — light — dark` — verified — Exact variants passed.',
      })) },
    })
    assert.equal(accepted.ok, true, JSON.stringify(accepted.findings, null, 2))
    assert.equal(accepted.observedState, 'done')
    assert.deepEqual(treeSnapshot(project.root), before)

    const invalidBytes = Buffer.from(doneDoc(stem, {
      completedAt: 'yesterday',
      file: '`../../outside` — modified — Unsafe.',
    }).replace('**Review iterations**: 1', '**Review iterations**: -99'))
    const rejected = validate(project, {
      stem,
      transition: 'todo:done',
      phase: 'post',
      proposal: { stem, fromState: 'todo', state: 'done', bytes: invalidBytes },
    })
    assert.equal(rejected.ok, false)
    assertCode(rejected, 'DONE_COMPLETED_AT_INVALID')
    assertCode(rejected, 'DONE_OUTCOME_INVALID')
    assertCode(rejected, 'DONE_FILES_TOUCHED_INVALID')
    assert.deepEqual(treeSnapshot(project.root), before)
  })

  check('CLI rejects forbidden transitions and malformed invocations with exit 2', () => {
    const project = makeProject()
    setState(project, 'backlog', 'TASK_60_backlog')
    const allowed = new Set(core.ALLOWED_TRANSITIONS)
    const forbidden = core.STATES.flatMap((from) => core.STATES.map((to) => `${from}:${to}`))
      .filter((transition) => !allowed.has(transition))
    assert.equal(forbidden.length, core.STATES.length ** 2 - allowed.size,
      'every state pair outside the explicit transition allow-list stays forbidden')
    for (const transition of forbidden) {
      const result = runCli(project, [
        '--stem', 'TASK_60_backlog', '--transition', transition, '--phase', 'pre', '--json',
      ])
      assert.equal(result.status, 2, `${transition}: ${result.stderr}${result.stdout}`)
      const envelope = parseCliJson(result)
      assert.equal(envelope.ok, false)
      assertCode(envelope, 'INVOCATION_INVALID')
    }

    for (const args of [
      ['--stem', 'TASK_60_backlog', '--transition', 'backlog:todo', '--json'],
      ['--stem', 'not-a-stem', '--json'],
      ['--expect', 'todo', '--json'],
      ['--all', '--stem', 'TASK_60_backlog', '--json'],
      ['--unknown'],
    ]) {
      const result = runCli(project, args)
      assert.equal(result.status, 2, `${args.join(' ')}: ${result.stderr}${result.stdout}`)
    }

    const jsonFailure = runCli(project, ['--unknown', '--json'])
    assert.equal(jsonFailure.status, 2, jsonFailure.stderr + jsonFailure.stdout)
    assertCode(parseCliJson(jsonFailure), 'INVOCATION_INVALID')
    assert.deepEqual(parseObservation(jsonFailure).findings, [{ code: 'INVOCATION_INVALID', severity: 'blocker' }])

    const quietFailure = runCli(project, ['--unknown', '--quiet'])
    assert.equal(quietFailure.status, 2, quietFailure.stderr + quietFailure.stdout)
    assert.equal(quietFailure.stdout, '')
    assert.equal(quietFailure.stderr, '')

    const quietWins = runCli(project, ['--unknown', '--json', '--quiet'])
    assert.equal(quietWins.status, 2, quietWins.stderr + quietWins.stdout)
    assert.equal(quietWins.stdout, '')
    assert.equal(quietWins.stderr, '')
  })

  check('CLI returns exit 1 and a stable envelope for a contract violation', () => {
    const project = makeProject()
    setState(project, 'backlog', 'TASK_61_actual')
    const result = runCli(project, ['--stem', 'TASK_61_actual', '--expect', 'todo', '--json'])
    assert.equal(result.status, 1, result.stderr + result.stdout)
    const envelope = parseCliJson(result)
    assert.equal(envelope.ok, false)
    assert.equal(envelope.observedState, 'backlog')
    assertCode(envelope, 'TASK_STATE_MISMATCH')
  })

  check('fixture-only atomic replacement during safe read fails closed with exit 4', () => {
    const project = makeProject()
    const stem = 'TASK_62_atomic_race'
    setState(project, 'todo', stem)
    const target = artifactPath(project, 'todo', stem)
    const replacement = join(project.tasks, 'todo', '.transition-' + 'a'.repeat(36) + '.tmp')
    writeFileSync(replacement, todoDoc(stem, { title: 'Atomic replacement generation' }))

    const raced = runCli(project, ['--stem', stem, '--expect', 'todo', '--json'], undefined, {
      TASK_STATE_TEST_ATOMIC_REPLACE_TARGET: target,
      TASK_STATE_TEST_ATOMIC_REPLACE_SOURCE: replacement,
    })
    assert.equal(raced.status, 4, raced.stderr + raced.stdout)
    const envelope = parseCliJson(raced)
    assert.equal(envelope.ok, false)
    assertCode(envelope, 'SNAPSHOT_RACE')
    assert.equal(envelope.findings[0].message, 'Task-state inputs changed during validation; retry from a fresh snapshot.')
    assert.equal(JSON.stringify(envelope).includes(target), false, 'public race envelopes must not expose internal paths')
    assert.equal(JSON.stringify(envelope).includes(replacement), false, 'public race envelopes must not expose fixture paths')

    assert.throws(() => core.validateTaskState({
      repoRoot: join(TASKS_DIR, '..', '..'),
      tasksDir: TASKS_DIR,
      outcomeShapePath: OUTCOME_SHAPE,
      stem: 'TASK_999_fixture_hook_denied',
      testReadHook() {},
    }), (error) => error instanceof core.ContractError && /isolated temporary fixtures/.test(error.message),
    'the deterministic race seam must be impossible on the canonical task root')

    const canonicalTarget = join(TASKS_DIR, 'README.md')
    const canonicalSource = join(TASKS_DIR, 'regen-arch.py')
    const canonicalBefore = { target: hashFile(canonicalTarget), source: hashFile(canonicalSource) }
    const denied = spawnSync(process.execPath, [CLI, '--stem', 'TASK_999_fixture_hook_denied', '--json'], {
      cwd: CANONICAL_PROJECT_ROOT,
      env: {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: CANONICAL_PROJECT_ROOT,
        ORCHESTRATOR_TASKS_DIR: TASKS_DIR,
        ORCHESTRATOR_OUTCOME_SHAPE_PATH: OUTCOME_SHAPE,
        TASK_STATE_TEST_ATOMIC_REPLACE_TARGET: canonicalTarget,
        TASK_STATE_TEST_ATOMIC_REPLACE_SOURCE: canonicalSource,
      },
      encoding: 'utf8', maxBuffer: 4 * 1024 * 1024,
    })
    assert.equal(denied.status, 2, denied.stderr + denied.stdout)
    const deniedEnvelope = parseCliJson(denied)
    assertCode(deniedEnvelope, 'INVOCATION_INVALID')
    assert.match(deniedEnvelope.findings[0].message, /isolated temporary project/)
    assert.deepEqual({ target: hashFile(canonicalTarget), source: hashFile(canonicalSource) }, canonicalBefore,
      'the CLI must reject the canonical root before constructing a destructive rename callback')
  })

  check('required contract absence is exit 3 while frozen task, INDEX, or contract disappearance is exit 4', () => {
    const missing = makeProject()
    setState(missing, 'backlog', 'TASK_68_missing_contract')
    const missingCli = runCli(missing, ['--stem', 'TASK_68_missing_contract', '--json'], undefined, {
      ORCHESTRATOR_OUTCOME_SHAPE_PATH: join(missing.root, 'absent-outcome-shape.json'),
    })
    assert.equal(missingCli.status, 3, missingCli.stderr + missingCli.stdout)
    assertCode(parseCliJson(missingCli), 'CONTRACT_UNREADABLE')

    const cases = [
      {
        stem: 'TASK_69_contract_disappears',
        target(project) {
          const contract = join(project.root, 'outcome-shape.json')
          writeFileSync(contract, readFileSync(OUTCOME_SHAPE))
          return { path: contract, options: { outcomeShapePath: contract } }
        },
      },
      {
        stem: 'TASK_70_task_disappears',
        target(project, stem) { return { path: artifactPath(project, 'backlog', stem), options: {} } },
      },
      {
        stem: 'TASK_71_index_disappears',
        target(project) {
          publishFreshIndex(project)
          return { path: join(project.tasks, 'INDEX.json'), options: { checkIndex: true } }
        },
      },
    ]
    for (const item of cases) {
      const project = makeProject()
      setState(project, 'backlog', item.stem)
      const selected = item.target(project, item.stem)
      let fired = false
      assert.throws(() => validate(project, {
        stem: item.stem,
        ...selected.options,
        testReadHook({ absolutePath, phase }) {
          if (!fired && phase === 'before-path-revalidation' && absolutePath === selected.path) {
            fired = true
            unlinkSync(selected.path)
          }
        },
      }), (error) => error instanceof core.SnapshotRaceError)
      assert.equal(fired, true, `disappearance hook did not fire for ${item.path}`)
    }
  })

  check('anchored scan survives safe swap-back, rejects live swaps, and keeps fixture hooks out of the canonical root', () => {
    const project = makeProject()
    const stem = 'TASK_63_anchored_swap'
    setState(project, 'backlog', stem)
    const target = join(project.tasks, 'backlog')
    const foreign = join(project.root, 'foreign-backlog')
    mkdirSync(foreign)
    writeFileSync(join(foreign, `${stem}.md`), 'foreign malformed bytes\n')
    const namesBefore = readdirSync(target).sort()
    const foreignBefore = readdirSync(foreign).sort()
    const saved = Object.fromEntries(['TASK_FS_TEST_ROOT', 'TASK_FS_TEST_SWAP_PATH', 'TASK_FS_TEST_SWAP_WITH',
      'TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY'].map((name) => [name, process.env[name]]))
    try {
      process.env.TASK_FS_TEST_ROOT = project.root
      process.env.TASK_FS_TEST_SWAP_PATH = target
      process.env.TASK_FS_TEST_SWAP_WITH = foreign
      process.env.TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY = '1'
      assert.throws(() => validate(project, { stem }), (error) => error instanceof core.SnapshotRaceError,
        'even a restored path must not erase evidence that its exact directory generation changed')
      assert.deepEqual(readdirSync(target).sort(), namesBefore)
      assert.deepEqual(readdirSync(foreign).sort(), foreignBefore)

      delete process.env.TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY
      assert.throws(() => validate(project, { stem }), (error) => error instanceof core.SnapshotRaceError)
      assert.deepEqual(readdirSync(target).sort(), namesBefore)
      assert.deepEqual(readdirSync(foreign).sort(), foreignBefore)
    } finally {
      for (const [name, value] of Object.entries(saved)) {
        if (value === undefined) delete process.env[name]
        else process.env[name] = value
      }
    }

    const backlog = join(TASKS_DIR, 'backlog'), todo = join(TASKS_DIR, 'todo')
    const before = { backlog: lstatSync(backlog).ino, todo: lstatSync(todo).ino }
    const denied = spawnSync('python3', [join(TASKS_DIR, 'anchored-task-fs.py')], {
      input: JSON.stringify({ version: 1, action: 'list', path: backlog, authorityRoot: CANONICAL_PROJECT_ROOT,
        maxEntries: 10_001, canonicalRoot: CANONICAL_PROJECT_ROOT, fixture: true }),
      env: { ...process.env, TASK_FS_TEST_ROOT: CANONICAL_PROJECT_ROOT,
        TASK_FS_TEST_SWAP_PATH: backlog, TASK_FS_TEST_SWAP_WITH: todo,
        TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY: '1' },
      encoding: 'utf8', maxBuffer: 4 * 1024 * 1024,
    })
    assert.equal(denied.status, 0, denied.stderr)
    const deniedEnvelope = JSON.parse(denied.stdout)
    assert.equal(deniedEnvelope.ok, false)
    assert.equal(deniedEnvelope.error.code, 'TEST_HOOK_INVALID')
    assert.deepEqual({ backlog: lstatSync(backlog).ino, todo: lstatSync(todo).ino }, before)
  })

  check('exact stat proofs distinguish dev/ino values that collide as JavaScript Numbers', () => {
    const common = { mode: 0o100600, size: 8, mtimeNs: '1783948400000000000', ctimeNs: '1783948400000000001' }
    const left = { ...common, dev: '9007199254740992', ino: '9007199254740992' }
    const right = { ...common, dev: '9007199254740993', ino: '9007199254740993' }
    assert.equal(Number(left.dev), Number(right.dev), 'fixture must collide under IEEE-754 Number')
    assert.equal(Number(left.ino), Number(right.ino), 'fixture must collide under IEEE-754 Number')
    assert.equal(core.sameExactStatProof(left, right), false)
    assert.equal(core.sameExactStatProof(left, { ...left }), true)
  })

  check('slow validation observation is configurable and cannot alter verdict or snapshot hash', () => {
    const project = makeProject()
    const stem = 'TASK_65_slow_observation'
    setState(project, 'backlog', stem)
    const low = runCli(project, ['--stem', stem, '--json', '--caller', 'test-suite'], undefined, {
      TASK_STATE_SLOW_MS: '0',
    })
    const high = runCli(project, ['--stem', stem, '--json', '--caller', 'test-suite'], undefined, {
      TASK_STATE_SLOW_MS: '60000',
    })
    assert.equal(low.status, 0, low.stderr + low.stdout)
    assert.equal(high.status, 0, high.stderr + high.stdout)
    const lowObservation = parseObservation(low)
    const highObservation = parseObservation(high)
    assert.equal(lowObservation.version, 1)
    assert.equal(lowObservation.event, 'task-state-validation')
    assert.equal(lowObservation.result, 'valid')
    assert.equal(lowObservation.slow, true)
    assert.equal(lowObservation.slowThresholdMs, 0)
    assert.equal(highObservation.slow, false)
    assert.equal(highObservation.slowThresholdMs, 60000)
    assert.deepEqual(core.projectObservation(lowObservation, {
      caller: 'test-suite', scope: stem,
    }), lowObservation, 'a parent can rebuild the exact public observation schema')
    assert.equal(core.projectObservation({ ...lowObservation, leakedPrompt: 'secret task prose' }), null,
      'unknown fields cannot cross the observation relay boundary')
    assert.equal(core.projectObservation({
      ...lowObservation, findings: [{ code: 'BAD CODE /tmp/secret', severity: 'blocker' }],
    }), null, 'finding text and paths cannot be smuggled through finding codes')
    const constraints = { caller: 'test-suite', scope: stem, action: null, transition: null }
    assert.deepEqual(core.projectObservationStream(
      `[task-state] ${JSON.stringify(lowObservation)}\nignored child secret\n`, constraints,
      { expectedCount: 1, syntheticPhases: ['pre'] }), [lowObservation],
    'arbitrary child stderr is discarded while the exact event is reprojected')
    const rejectedStream = core.projectObservationStream(
      `[task-state] ${JSON.stringify({ ...lowObservation, leakedPrompt: 'secret' })}\n`, constraints,
      { expectedCount: 1, syntheticPhases: ['pre'], fallbackCode: 'OBSERVATION_INVALID' })
    assert.equal(rejectedStream.length, 1)
    assert.deepEqual(rejectedStream[0].findings, [{ code: 'OBSERVATION_INVALID', severity: 'blocker' }])
    assert.equal(JSON.stringify(rejectedStream).includes('secret'), false)
    assert.deepEqual(stableEnvelope(parseCliJson(low)), stableEnvelope(parseCliJson(high)),
      'performance observation belongs only to stderr telemetry')
  })

  check('architecture freshness is an explicit global derived-state report, separate from task integrity', () => {
    const project = makeProject()
    const stem = 'TASK_66_arch_scope'
    setState(project, 'backlog', stem)

    const absent = runCli(project, ['--all', '--check-arch', '--json'])
    assert.equal(absent.status, 0, absent.stderr + absent.stdout)
    const absentEnvelope = parseCliJson(absent)
    assert.equal(absentEnvelope.ok, true)
    assert.equal(absentEnvelope.overallOk, true)
    assert.equal(absentEnvelope.derivedState.arch.status, 'absent')
    const directAbsent = core.checkArchitectureState({ repoRoot: project.root })
    assert.equal(directAbsent.status, 'absent')
    assert.equal(directAbsent.ok, true)

    const invalidMachineScript = join(project.root, 'invalid-arch-contract.cjs')
    writeFileSync(invalidMachineScript, [
      "'use strict';",
      "process.stdout.write(JSON.stringify({version:2,status:'absent',fresh:true,path:'orchestrator/.arch-map.json',actualHash:null,expectedHash:null,actualRevision:null,expectedRevision:null,reason:'pre-bootstrap',extra:true}));",
    ].join('\n'))
    assert.throws(() => core.checkArchitectureState({
      repoRoot: project.root, python: process.execPath, scriptPath: invalidMachineScript,
    }), core.ContractError, 'the reusable API must reject an extended/malformed machine envelope')

    const scoped = runCli(project, ['--stem', stem, '--check-arch', '--json'])
    assert.equal(scoped.status, 2, scoped.stderr + scoped.stdout)
    const scopedEnvelope = parseCliJson(scoped)
    assertCode(scopedEnvelope, 'INVOCATION_INVALID')
    assert.match(scopedEnvelope.findings[0].message, /--check-arch is available only with global --all validation/)

    writeFileSync(join(project.root, 'settings.gradle.kts'), 'include(":shared")\n')
    const stale = runCli(project, ['--all', '--check-arch', '--json'])
    assert.equal(stale.status, 1, stale.stderr + stale.stdout)
    const staleEnvelope = parseCliJson(stale)
    assert.equal(staleEnvelope.ok, true, 'task-column integrity remains an independent verdict')
    assert.equal(staleEnvelope.derivedOk, false)
    assert.equal(staleEnvelope.overallOk, false)
    assert.equal(staleEnvelope.derivedState.arch.status, 'stale')
    assert.equal(codes(staleEnvelope).includes('ARCH_MAP_STALE'), false,
      'architecture drift must not masquerade as a task-column finding')
    assert.equal(staleEnvelope.derivedState.arch.findings[0].code, 'ARCH_MAP_STALE')

    if (process.platform !== 'win32') {
      const externalMap = join(project.root, 'external-arch-map.json')
      const mapPath = join(project.root, 'orchestrator', '.arch-map.json')
      writeFileSync(externalMap, '{"external":"must-not-be-read"}\n')
      const externalHash = hashFile(externalMap)
      symlinkSync(externalMap, mapPath)
      const unsafeMap = runCli(project, ['--all', '--check-arch', '--json'])
      assert.equal(unsafeMap.status, 1, unsafeMap.stderr + unsafeMap.stdout)
      const unsafeEnvelope = parseCliJson(unsafeMap)
      assert.equal(unsafeEnvelope.derivedState.arch.status, 'stale')
      assert.equal(unsafeEnvelope.derivedState.arch.actualHash, null,
        'the no-follow checker must not hash a symlink target')
      assert.equal(unsafeEnvelope.derivedState.arch.findings[0].details.reason, 'unreadable-or-invalid')
      assert.equal(hashFile(externalMap), externalHash)
      rmSync(mapPath)
    }

    const regen = spawnSync('python3', [join(TASKS_DIR, 'regen-arch.py')], {
      cwd: project.root,
      env: { ...process.env, PYTHONDONTWRITEBYTECODE: '1' },
      encoding: 'utf8',
      maxBuffer: 4 * 1024 * 1024,
    })
    assert.equal(regen.status, 0, regen.stderr + regen.stdout)
    const fresh = runCli(project, ['--all', '--check-arch', '--json'])
    assert.equal(fresh.status, 0, fresh.stderr + fresh.stdout)
    const freshEnvelope = parseCliJson(fresh)
    assert.equal(freshEnvelope.derivedState.arch.status, 'fresh')
    assert.equal(freshEnvelope.derivedOk, true)
    assert.equal(freshEnvelope.overallOk, true)
    assert.equal(freshEnvelope.snapshotHash, staleEnvelope.snapshotHash,
      'derived architecture state must remain outside the canonical task snapshot hash')
  })

  check('validation is deterministic and strictly read-only', () => {
    const project = makeProject()
    setState(project, 'backlog', 'TASK_70_read_only')
    setState(project, 'pending', 'TASK_71_pending')
    setState(project, 'todo', 'TASK_72_todo')
    setState(project, 'done', 'TASK_73_done')
    publishFreshIndex(project)

    const before = treeSnapshot(project.root)
    const first = runCli(project, ['--all', '--check-index', '--json', '--caller', 'test-suite'])
    const middle = treeSnapshot(project.root)
    const second = runCli(project, ['--all', '--check-index', '--json', '--caller', 'test-suite'])
    const after = treeSnapshot(project.root)
    assert.equal(first.status, 0, first.stderr + first.stdout)
    assert.equal(second.status, 0, second.stderr + second.stdout)
    assert.deepEqual(middle, before, 'first validation changed the project tree')
    assert.deepEqual(after, before, 'second validation changed the project tree')
    assert.deepEqual(stableEnvelope(parseCliJson(second)), stableEnvelope(parseCliJson(first)))

    const quiet = runCli(project, ['--all', '--check-index', '--quiet'])
    assert.equal(quiet.status, 0, quiet.stderr + quiet.stdout)
    assert.equal(quiet.stdout, '')
    assert.equal(quiet.stderr, '')
    assert.deepEqual(treeSnapshot(project.root), before, 'quiet validation changed the project tree')

    const invalidProject = makeProject()
    writeArtifact(invalidProject, 'todo', 'TASK_74_invalid', '# TASK 74 — Invalid\n')
    const invalidBefore = treeSnapshot(invalidProject.root)
    const invalidFirst = runCli(invalidProject, ['--all', '--json', '--caller', 'test-suite'])
    const invalidSecond = runCli(invalidProject, ['--all', '--json', '--caller', 'test-suite'])
    assert.equal(invalidFirst.status, 1, invalidFirst.stderr + invalidFirst.stdout)
    assert.equal(invalidSecond.status, 1, invalidSecond.stderr + invalidSecond.stdout)
    assert.deepEqual(stableEnvelope(parseCliJson(invalidSecond)), stableEnvelope(parseCliJson(invalidFirst)),
      'invalid findings must be deterministically ordered and serialized')
    assert.deepEqual(treeSnapshot(invalidProject.root), invalidBefore, 'invalid validation changed the project tree')
  })

  if (failures.length) {
    console.error(`${failures.length} of ${checks} task-state validator checks failed.`)
    process.exitCode = 1
  } else {
    console.log(`All task-state validator tests passed (${checks} checks).`)
  }
} finally {
  for (const root of roots.reverse()) rmSync(root, { recursive: true, force: true, maxRetries: 3, retryDelay: 20 })
}
