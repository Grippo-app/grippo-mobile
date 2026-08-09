// Fixture self-test for descope-task.mjs (W3-5/W3-6 — the sanctioned de-scope path) plus the
// figmaNodeRetired provenance skip. No Figma, no Gradle.
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, existsSync, rmSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import assert from 'node:assert/strict'
import { bytesHash } from '../runtime/canonical-json.mjs'
import { sourceIdentity, validObservedCapture } from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const SCRIPT = join(HERE, '..', 'scripts', 'descope-task.mjs')
const PROJECT_ROOT = fileURLToPath(new globalThis.URL('../../..', import.meta.url))
const REGEN = join(PROJECT_ROOT, 'orchestrator', 'tasks', 'regen-index.py')
const requireCjs = createRequire(import.meta.url)
const designParser = requireCjs(join(HERE, '..', 'scripts', 'design-parser.cjs'))
const taskState = requireCjs(join(PROJECT_ROOT, 'orchestrator', 'tasks', 'task-state-core.cjs'))
const taskSource = requireCjs(join(PROJECT_ROOT, 'orchestrator', 'tasks', 'task-source-contract.cjs'))
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const URL = 'https://www.figma.com/design/fileKey?node-id=1-2'
const STEM = 'TASK_5_component_badge'

function setup({ bodyExtra = '' } = {}) {
  const ws = mkdtempSync(join(tmpdir(), 'descope-'))
  const tasks = join(ws, 'orchestrator', 'tasks')
  const todo = join(tasks, 'todo')
  const screens = join(ws, 'screens')
  const screensDir = join(screens, STEM)
  for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasks, column), { recursive: true })
  mkdirSync(join(ws, 'orchestrator', '.cache', 'tasks', 'finalizations'), { recursive: true })
  mkdirSync(screensDir, { recursive: true })
  const fetchedAt = '2026-01-01T00:00:00Z'
  const tokenCapture = validObservedCapture({
    source: sourceIdentity({
      nodeId: '1:2',
      kind: 'component',
      origin: { kind: 'task-component', taskStem: STEM, designComponentId: 'figma-component:fixture:1:2' },
    }),
  })
  const tokenBytes = Buffer.from(JSON.stringify(tokenCapture, null, 2) + '\n')
  writeFileSync(join(screensDir, 'Badge.primary.tokens.json'), tokenBytes)
  writeFileSync(join(screensDir, 'index.json'), JSON.stringify({
    schemaVersion: 3, taskStem: STEM, nodes: { Badge: {
      kind: 'component', nodeId: '1:2', url: URL, fetchedAt,
      variants: [{
        id: 'light-default-shared', theme: 'light', locale: 'default', platform: 'shared',
        url: URL, nodeId: '1:2', fetchedAt, imageFile: 'Badge.png',
        tokensFile: 'Badge.primary.tokens.json',
        tokensHash: bytesHash(tokenBytes),
        captureOperationId: tokenCapture.captureOperationId,
        captureSequence: tokenCapture.captureSequence,
      }]
    } }
  }))
  const source = taskSource.render(taskSource.manualForIntent('descope-fixture', 'manual', 'descope-fixture'))
  writeFileSync(join(todo, `${STEM}.md`), [
    '# TASK 5 — Component badge', '', source, '',
    '## Goal', '', 'Update the Badge component safely.', '',
    '## Inputs', '', '- Existing component contracts.', bodyExtra, '',
    '## Design',
    `- Badge [component] — ${URL}`,
    '- Aux — none (author reason)',
    '', '## Acceptance', '', '### Automated', '',
    '- Run `node test/badge-contract.mjs`.', '',
    '## Out of scope', '', '- Unrelated component work.', '',
  ].join('\n'))
  writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify({
    version: 2, generatedAt: '1970-01-01T00:00:00.000Z', backlog: [], pending: [], todo: [], done: [],
  }, null, 2) + '\n')
  const regenerated = spawnSync('python3', [REGEN], {
    cwd: ws,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: ws, ORCHESTRATOR_TASKS_DIR: tasks },
    encoding: 'utf8',
  })
  assert.equal(regenerated.status, 0, regenerated.stderr + regenerated.stdout)
  return { ws, tasks, todo, screensDir, taskFile: join(todo, `${STEM}.md`) }
}

function run(t, args) {
  return spawnSync('node', [SCRIPT, STEM, ...args], {
    env: {
      ...process.env,
      FIGMA_TASKS_ROOT: t.tasks,
      FIGMA_DESCOPE_PROJECT_ROOT: t.ws,
      FIGMA_SCREEN_CACHE_ROOT: join(t.ws, 'screens'),
      ORCHESTRATOR_PROJECT_ROOT: t.ws,
      ORCHESTRATOR_TASKS_DIR: t.tasks,
      ORCHESTRATOR_FINALIZATIONS_DIR: join(t.ws, 'orchestrator', '.cache', 'tasks', 'finalizations'),
      ORCHESTRATOR_WRITER_LEASES_DIR: join(t.ws, 'orchestrator', '.cache', 'tasks', 'finalizations', '.writers'),
      ORCHESTRATOR_TRANSITIONS_DIR: join(t.ws, 'orchestrator', '.cache', 'tasks', 'transitions'),
    },
    encoding: 'utf8',
  })
}
function taskStateEvents(stderr) {
  return String(stderr || '').split(/\r?\n/).filter((line) => line.startsWith('[task-state] '))
    .map((line) => JSON.parse(line.slice('[task-state] '.length)))
}

try {
  // (1) Dry-run: plan printed, NOTHING written.
  const t1 = setup()
  const dry = run(t1, ['--reason', 'design deleted upstream'])
  check('dry-run prints the plan and writes nothing', () => {
    assert.equal(dry.status, 0, dry.stdout + dry.stderr)
    assert.match(dry.stdout, /DRY-RUN/)
    const events = taskStateEvents(dry.stderr)
    assert.equal(events.length, 1)
    assert.equal(events[0].caller, 'manual')
    assert.equal(events[0].scope, STEM)
    assert.equal(events[0].action, 'descope')
    assert.equal(JSON.stringify(events[0]).includes('design deleted upstream'), false)
    assert.match(readFileSync(t1.taskFile, 'utf8'), /— https:\/\//, 'bullet must be untouched on dry-run')
    assert.ok(existsSync(t1.screensDir), 'cache must survive a dry-run')
  })

  // (2) --yes full path: audited-none bullets, confined cache removal, committed receipt.
  const applied = run(t1, ['--reason', 'design deleted (upstream pivot)', '--yes'])
  check('--yes rewrites bullets to audited none, keeps author none-reasons, clears cache, writes receipt', () => {
    assert.equal(applied.status, 0, applied.stdout + applied.stderr)
    const body = readFileSync(t1.taskFile, 'utf8')
    assert.match(body, /- Badge \[component\] — none \(design deleted upstream pivot\)/)
    assert.match(body, /- Aux — none \(author reason\)/, 'an existing none keeps its author reason')
    assert.equal(designParser.hasMalformedDesign(body), false)
    assert.equal(designParser.hasPullableDesign(body), false)
    assert.ok(!existsSync(t1.screensDir), 'screens cache removed')
    const receipt = JSON.parse(readFileSync(join(t1.tasks, 'evidence', 'descope', `${STEM}.json`), 'utf8'))
    assert.equal(receipt.stem, STEM)
    assert.equal(receipt.screensCacheRemoved, true)
    assert.equal(receipt.designBulletsBefore.length, 2)
    assert.match(receipt.sourceRevisionBefore, /^sha256:[a-f0-9]{64}$/)
    assert.match(receipt.sourceRevisionAfter, /^sha256:[a-f0-9]{64}$/)
    assert.notEqual(receipt.sourceRevisionBefore, receipt.sourceRevisionAfter)
    const valid = taskState.validateTaskState({ tasksDir: t1.tasks, repoRoot: t1.ws, stem: STEM, expect: 'todo', checkIndex: true, includeRuntime: false })
    assert.equal(valid.ok, true, JSON.stringify(valid.findings))
    // W3-5 contract shape: the receipt records WHO performed the sanctioned act.
    assert.ok(typeof receipt.by === 'string' && receipt.by.length > 0, 'receipt must carry a non-empty by field')
  })

  // (3) Residual body citation: bullets land, cache KEPT (backstops stay armed), exit 2.
  const t3 = setup({ bodyExtra: `See the mock at ${URL} for the old shape.` })
  const partial = run(t3, ['--reason', 'design deleted', '--yes'])
  check('a residual body citation exits 2, keeps the cache, and records it in the receipt', () => {
    assert.equal(partial.status, 2, partial.stdout + partial.stderr)
    assert.match(readFileSync(t3.taskFile, 'utf8'), /- Badge \[component\] — none \(design deleted\)/, 'bullets still land (progress persists)')
    assert.ok(existsSync(t3.screensDir), 'cache must be KEPT while citations remain')
    const receipt = JSON.parse(readFileSync(join(t3.tasks, 'evidence', 'descope', `${STEM}.json`), 'utf8'))
    assert.equal(receipt.residualCitations.length, 1)
  })

  // (4) Retirement moved to the Mapping Review CAS op (`retire-mapping`): the CLI flag is GONE
  //     and must be refused as unknown, while the provenance backstop's release semantics
  //     (a figmaNodeRetired provenance row no longer holds the component) stay pinned at the
  //     parser level over the exact row shape componentProvenanceEntries() emits.
  const t4 = setup()
  const retiredFlag = run(t4, ['--reason', 'design deleted', '--yes', '--retire-component', 'Badge'])
  check('--retire-component is refused as an unknown flag (retirement = Mapping Review retire-mapping)', () => {
    assert.notEqual(retiredFlag.status, 0)
    assert.match(retiredFlag.stdout + retiredFlag.stderr, /unknown flag|usage/i)
    assert.match(readFileSync(t4.taskFile, 'utf8'), /- Badge \[component\] — https:\/\//, 'refused invocation must not edit the task')
  })
  check('figmaNodeRetired provenance rows release the backstop; a live anchor stays held', () => {
    const liveRows = [{ component: 'Badge', source: 'ds/Badge.kt', figmaNodeId: '1:2' }]
    const heldBefore = designParser.uiTaskWithoutDesign('# T\n\n### Files touched\n\n- `ds/Badge.kt` — edited\n', { stem: STEM, inventory: liveRows })
    assert.ok(heldBefore && heldBefore.level === 'block', 'live anchor must hard-block')
    const retiredRows = [{ component: 'Badge', source: 'ds/Badge.kt', figmaNodeId: '1:2', figmaNodeRetired: { reason: 'design deleted', at: '2026-01-01T00:00:00Z', by: 'owner' } }]
    const heldAfter = designParser.uiTaskWithoutDesign('# T\n\n### Files touched\n\n- `ds/Badge.kt` — edited\n', { stem: STEM, inventory: retiredRows })
    assert.equal(heldAfter, null, `retired anchor must release the backstop, got ${JSON.stringify(heldAfter)}`)
  })

  // (4b) R2-3 wedge guard: a task that opted into `- gate: strict` must still be
  // de-scope-able — the gate line passes through VERBATIM (rewriting it would malform the
  // section and abort; and a de-scoped task has no rows for strict routing to act on).
  const tGate = setup({})
  writeFileSync(tGate.taskFile, readFileSync(tGate.taskFile, 'utf8').replace('## Inputs', '- gate: strict\n\n## Inputs'))
  const gateIndex = spawnSync('python3', [REGEN], {
    cwd: tGate.ws,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: tGate.ws, ORCHESTRATOR_TASKS_DIR: tGate.tasks },
    encoding: 'utf8',
  })
  assert.equal(gateIndex.status, 0, gateIndex.stderr + gateIndex.stdout)
  const gateRun = run(tGate, ['--reason', 'design deleted', '--yes'])
  check('a `- gate: strict` task de-scopes cleanly; the gate line survives verbatim', () => {
    assert.equal(gateRun.status, 0, gateRun.stdout + gateRun.stderr)
    const body = readFileSync(tGate.taskFile, 'utf8')
    assert.match(body, /^- gate: strict$/m, 'gate line preserved')
    assert.match(body, /- Badge \[component\] — none \(design deleted\)/)
    assert.equal(designParser.hasMalformedDesign(body), false)
    assert.equal(designParser.hasPullableDesign(body), false)
  })

  // (5) W3-5 acceptance: a stem typo is refused by containment — nothing written, nothing removed.
  const t5 = setup()
  const runStem = (t, stem, args) => spawnSync('node', [SCRIPT, stem, ...args], {
    env: {
      ...process.env, FIGMA_TASKS_ROOT: t.tasks, FIGMA_DESCOPE_PROJECT_ROOT: t.ws,
      FIGMA_SCREEN_CACHE_ROOT: join(t.ws, 'screens'),
      ORCHESTRATOR_PROJECT_ROOT: t.ws, ORCHESTRATOR_TASKS_DIR: t.tasks,
      ORCHESTRATOR_FINALIZATIONS_DIR: join(t.ws, 'orchestrator', '.cache', 'tasks', 'finalizations'),
    },
    encoding: 'utf8',
  })
  check('a traversal-shaped stem is refused before any lookup (STEM_RE containment)', () => {
    const r = runStem(t5, '../evil', ['--reason', 'x', '--yes'])
    assert.notEqual(r.status, 0)
    assert.ok(existsSync(t5.screensDir), 'screens cache untouched')
    assert.ok(!existsSync(join(t5.tasks, 'evidence', 'descope')), 'no receipt dir created')
  })
  check('a valid-charset but NONEXISTENT stem is refused with a no-task-file error (typo catch)', () => {
    const r = runStem(t5, 'TASK_5_component_bagde', ['--reason', 'x', '--yes'])
    assert.notEqual(r.status, 0)
    assert.match(r.stdout + r.stderr, /task file|no task/i)
    assert.ok(existsSync(t5.screensDir), 'screens cache untouched')
    assert.ok(!existsSync(join(t5.tasks, 'evidence', 'descope')), 'no receipt written for a typo\'d stem')
  })
} catch (e) {
  fail++; console.log(`${C.red}FAIL${C.reset} descope setup threw\n     ${e.stack || e.message}`)
}

console.log(`\ndescope-task.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
