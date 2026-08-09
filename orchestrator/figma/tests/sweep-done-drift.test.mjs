// Fixture self-test for sweep-done-drift.mjs (C1 post-ship drift-auto-stale) — no Figma, no Gradle.
// Verifies: committed receipt spec baseline vs a fresh shadow re-pull → a committed drift-stale
// marker on a Figma move; clean when nothing moved; cleared when drift resolves; hard failure without
// complete current inputs; and the GOLDEN INVARIANT (the sweep makes no Figma call).
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, existsSync, rmSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { buildFigmaMeta } from '../scripts/figma-meta.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const SCRIPT = join(HERE, '..', 'scripts', 'sweep-done-drift.mjs')
const VERIFY = join(HERE, '..', 'scripts', 'verify-done.mjs')
const require = createRequire(import.meta.url)
const shipDriftContract = require('../scripts/ship-drift-contract.cjs')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const el = (id, extra = {}) => ({ stableId: id, figmaNodeId: `fixture:${id}`, name: id, bboxDp: { x: 0, y: 0, w: 10, h: 10 }, ...extra })
const spec = (elements) => ({
  schemaVersion: 2, screen: 'Home', theme: 'light', frameSizeDp: { w: 100, h: 200 },
  source: { fileKey: 'fixture', nodeId: 'fixture:root' }, rootNodeId: 'root',
  coordinateSystem: { units: 'dp', density: 1, origin: 'frame' }, themeMetadata: { themeKey: 'light' },
  nodes: [{ stableId: 'root', figmaNodeId: 'fixture:root', name: 'Home', role: 'screen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } }, ...elements],
  elements,
})
const UI_TASK = '# Task\n\n## Design\n- Home — https://www.figma.com/design/ABC/x?node-id=1-2\n'
const DIGEST = buildFigmaMeta({ taskStem: 'TASK_1_fixture', stage: 'final', overall: 'PASS', pipelineRunId: 'run-9', evidenceReportHash: 'sha256:' + 'a'.repeat(64), screenshotReportHash: 'sha256:' + 'b'.repeat(64), generatedAt: '2026-01-01T00:00:00.000Z', visualChecks: 1, problemCount: 0, designHash: 'sha256:' + 'c'.repeat(64), gatePolicyVersion: 1, tokenObservationManifestHash: 'sha256:' + 'd'.repeat(64), rows: 'Home/primary:PASS' })

function setup({ baseline = true, shadowSpec = null } = {}) {
  const ws = mkdtempSync(join(tmpdir(), 'sweep-drift-'))
  const done = join(ws, 'done'), receipts = join(ws, 'receipts'), screens = join(ws, 'screens')
  mkdirSync(done, { recursive: true })
  writeFileSync(join(done, 'TASK_1_fixture.md'), UI_TASK)
  if (baseline) {
    const specsDir = join(receipts, 'TASK_1_fixture', 'specs')
    mkdirSync(specsDir, { recursive: true })
    writeFileSync(join(specsDir, 'Home.spec.json'), JSON.stringify(spec([el('a'), el('b')])))
    writeFileSync(join(receipts, 'TASK_1_fixture', 'figma-meta-TASK_1_fixture.txt'), DIGEST + '\n')
  }
  const drift = join(screens, 'TASK_1_fixture', '.drift')
  mkdirSync(drift, { recursive: true })
  if (shadowSpec) writeFileSync(join(drift, 'Home.spec.json'), JSON.stringify(shadowSpec))
  return { ws, done, receipts, screens, stale: join(receipts, 'TASK_1_fixture', 'drift-stale-TASK_1_fixture.json') }
}
function runOn(t, extra = {}) {
  return spawnSync('node', [SCRIPT], { env: { ...process.env, FIGMA_DONE_DIR: t.done, FIGMA_SHIP_RECEIPTS_DIR: t.receipts, FIGMA_SCREEN_CACHE_ROOT: t.screens, FIGMA_SWEEP_STAMP: '2026-07-04T00:00:00.000Z', ...extra }, encoding: 'utf8' })
}

try {
  // (1) baseline == fresh shadow → clean → no marker.
  const same = setup({ shadowSpec: spec([el('a'), el('b')]) })
  const r1 = runOn(same)
  check('baseline == fresh shadow → no drift marker (exit 0)', () => {
    assert.equal(r1.status, 0)
    assert.ok(!existsSync(same.stale), 'no drift-stale.json when nothing moved')
  })
  rmSync(same.ws, { recursive: true, force: true })

  // (2) a Figma move (element removed + moved) → committed marker.
  const moved = setup({ shadowSpec: spec([el('a', { bboxDp: { x: 40, y: 40, w: 10, h: 10 } })]) })
  const r2 = runOn(moved)
  check('a Figma move since ship → committed drift-stale marker with the drifted screens', () => {
    assert.equal(r2.status, 0, 'advisory — never fails the process')
    assert.ok(existsSync(moved.stale), 'drift-stale.json written')
    const j = JSON.parse(readFileSync(moved.stale, 'utf8'))
    assert.equal(j.taskStem, 'TASK_1_fixture')
    assert.equal(j.staleAt, '2026-07-04T00:00:00.000Z')
    assert.ok(j.driftedCount >= 1 && j.driftedScreens.length >= 1, 'drifted screens recorded')
    assert.equal(j.baselineRunId, 'run-9', 'marker records the exact baseline pipelineRunId')
  })
  rmSync(moved.ws, { recursive: true, force: true })

  // (2b) drift RESOLVED on a later sweep (shadow now matches) → the stale marker is CLEARED.
  const resolved = setup({ shadowSpec: spec([el('a'), el('b')]) })
  writeFileSync(resolved.stale, JSON.stringify({ version: 1, taskStem: 'TASK_1_fixture', driftedScreens: [{ screen: 'Home', theme: 'primary', changes: ['x'] }] }))
  runOn(resolved)
  check('drift resolved on a later sweep → the stale marker is cleared', () => {
    assert.ok(!existsSync(resolved.stale), 'marker removed when the design no longer drifts')
  })
  rmSync(resolved.ws, { recursive: true, force: true })

  // (3) no committed baseline → incomplete command failure, no marker.
  const noBase = setup({ baseline: false, shadowSpec: spec([el('a')]) })
  const r3 = runOn(noBase)
  check('a task with NO committed spec baseline fails closed (no marker)', () => {
    assert.notEqual(r3.status, 0)
    assert.ok(!existsSync(noBase.stale), 'no marker without a baseline to diff')
    assert.match(r3.stdout + r3.stderr, /skipped 1/)
  })
  rmSync(noBase.ws, { recursive: true, force: true })

  // (3b) PARTIAL re-pull (the drifted screen was NOT_CHECKED, or no comparable
  //      shadow at all): an EXISTING stale marker MUST be KEPT, never cleared — else a re-pull that
  //      skipped the very screen that moved would launder real drift to green (fail-closed hole).
  const partial = setup({ shadowSpec: null })   // .drift exists but empty → Home is NOT_CHECKED
  writeFileSync(partial.stale, JSON.stringify({ version: 1, taskStem: 'TASK_1_fixture', driftedScreens: [{ screen: 'Home', theme: 'primary', changes: ['x'] }] }))
  const rp = runOn(partial)
  check('partial re-pull (NOT_CHECKED) KEEPS the stale marker — never launders real drift to green', () => {
    assert.notEqual(rp.status, 0)
    assert.ok(existsSync(partial.stale), 'marker kept when the drifted screen was not re-fetched')
    assert.match(rp.stdout + rp.stderr, /re-pull incomplete|keeping the existing/i)
  })
  rmSync(partial.ws, { recursive: true, force: true })

  // (4) staleness must NOT be a gate failure — the sweep exits 0 even when it marks drift (2 proved
  //     it above); reaffirm the invariant that a DRIFTED task never raises the exit code.
  const moved2 = setup({ shadowSpec: spec([el('replacement')]) })
  const r4 = runOn(moved2)
  check('a drifted task never raises the exit code (staleness is advisory, not a gate)', () => {
    assert.equal(r4.status, 0)
    assert.ok(existsSync(moved2.stale))
  })
  rmSync(moved2.ws, { recursive: true, force: true })

  // (5) GOLDEN INVARIANT: the sweep source (code, comments stripped) contains NO Figma call.
  check('golden invariant: sweep-done-drift.mjs makes no Figma call', () => {
    const code = readFileSync(SCRIPT, 'utf8').split('\n').filter((l) => !/^\s*\/\//.test(l)).join('\n')
    assert.ok(!/figma\.com|get_screenshot|get_metadata|mcp__figma|fetch\s*\(|https?:\/\//i.test(code), 'no network/Figma call in the sweep code')
  })

  check('writer and both readers share one exact fail-closed drift marker contract', () => {
    const valid = shipDriftContract.marker({
      taskStem: 'TASK_1_fixture',
      staleAt: '2026-07-04T00:00:00.000Z',
      baselineRunId: 'run-9',
      driftedScreens: [{ screen: 'Home', theme: 'primary', changes: ['fill changed'] }],
    })
    assert.equal(shipDriftContract.validMarker(valid, 'TASK_1_fixture'), true)
    assert.equal(shipDriftContract.validMarker({ ...valid, extra: true }, 'TASK_1_fixture'), false)
    assert.equal(shipDriftContract.validMarker({ ...valid, version: 2 }, 'TASK_1_fixture'), false)
    assert.equal(shipDriftContract.validMarker({ ...valid, driftedScreens: [] }, 'TASK_1_fixture'), false)
    const verifySource = readFileSync(VERIFY, 'utf8')
    assert.match(verifySource, /shipDriftContract\.validMarker\(stale, fileStem\)/)
    assert.match(verifySource, /violations\.push\(\[file, 'committed post-ship drift marker is unsafe, unreadable, or invalid'\]\)/)
    assert.doesNotMatch(verifySource, /JSON\.parse\(readFileSync\(stalePath, 'utf8'\)\)\s*\}\s*catch\s*\{\s*continue\s*\}/)
  })
} catch (e) {
  fail++; console.log(`${C.red}FAIL${C.reset} setup threw\n     ${e.message}`)
}

console.log(`\nsweep-done-drift.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
