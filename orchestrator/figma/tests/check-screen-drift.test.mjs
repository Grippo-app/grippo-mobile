// check-screen-drift.test.mjs — pins for the local baseline↔shadow spec comparator:
// identical specs are CLEAN (PASS); a frame/element/text/fill change is DRIFTED (WARN) with a
// readable change list; a screen with no shadow spec is an incomplete BLOCKER; a sub-tolerance
// bbox nudge is not flagged; checked drift stays advisory (exit 0 even on drift).
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'

const HERE = dirname(fileURLToPath(import.meta.url))
const SCRIPT = join(HERE, '..', 'scripts', 'check-screen-drift.mjs')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const el = (id, over = {}) => ({ stableId: id, figmaNodeId: `fixture:${id}`, name: id, bboxDp: { x: 0, y: 0, w: 100, h: 20 }, fills: ['#FFFFFF'], ...over })
const spec = (inputEls, over = {}) => {
  const elements = inputEls.map((element) => ({ ...element }))
  const base = {
    schemaVersion: 2, screen: 'Home', frameSizeDp: { w: 390, h: 844 }, theme: 'dark',
    source: { fileKey: 'fixture', nodeId: 'fixture:root' }, rootNodeId: 'root',
    coordinateSystem: { units: 'dp', density: 1, origin: 'frame' }, themeMetadata: { themeKey: 'dark' },
    nodes: [{ stableId: 'root', figmaNodeId: 'fixture:root', name: 'Home', role: 'screen', bboxDp: { x: 0, y: 0, w: 390, h: 844 } }, ...elements.map((element) => ({ ...element }))],
    elements,
  }
  const merged = { ...base, ...over }
  if (over.frameSizeDp) merged.nodes = [{ ...merged.nodes[0], bboxDp: { x: 0, y: 0, ...over.frameSizeDp } }, ...merged.nodes.slice(1)]
  if (over.chromeCrop) merged.chromeCrop = over.chromeCrop
  return merged
}

function run({ base, shadow }) {
  const ws = mkdtempSync(join(tmpdir(), 'screen-drift-'))
  try {
    const sdir = join(ws, 'screens', 'TASK_1_fixture'), drift = join(sdir, '.drift'), reports = join(ws, 'reports')
    mkdirSync(drift, { recursive: true }); mkdirSync(reports, { recursive: true })
    for (const [name, s] of Object.entries(base)) writeFileSync(join(sdir, name), JSON.stringify(s))
    for (const [name, s] of Object.entries(shadow || {})) writeFileSync(join(drift, name), JSON.stringify(s))
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture'], { env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: reports }, encoding: 'utf8' })
    return { status: r.status, report: JSON.parse(readFileSync(join(reports, 'screen-drift-TASK_1_fixture.json'), 'utf8')) }
  } finally { rmSync(ws, { recursive: true, force: true }) }
}
const rowOf = (rep, screen) => rep.screens.find((s) => s.screen === screen)

try {
  const clean = run({ base: { 'Home.spec.json': spec([el('a'), el('b')]) }, shadow: { 'Home.spec.json': spec([el('a'), el('b')]) } })
  check('identical baseline and shadow → CLEAN, overall PASS, exit 0', () => {
    assert.equal(clean.status, 0)
    assert.equal(clean.report.overall, 'PASS')
    assert.equal(rowOf(clean.report, 'Home').status, 'CLEAN')
  })

  const drift = run({
    base: { 'Home.spec.json': spec([el('title', { text: 'Speed' }), el('gone')]) },
    shadow: { 'Home.spec.json': spec([el('title', { bboxDp: { x: 0, y: 40, w: 100, h: 20 }, text: 'Velocity' }), el('newcard')], { frameSizeDp: { w: 390, h: 900 } }) },
  })
  check('frame/element/text/move change → DRIFTED, WARN, readable changes, still exit 0', () => {
    assert.equal(drift.status, 0)   // advisory
    assert.equal(drift.report.overall, 'WARN')
    const row = rowOf(drift.report, 'Home')
    assert.equal(row.status, 'DRIFTED')
    const joined = row.changes.join(' | ')
    assert.match(joined, /frame 390×844 → 390×900dp/)
    assert.match(joined, /removed 'gone'/)
    assert.match(joined, /added 'newcard'/)
    assert.match(joined, /moved\/resized 'title'/)
    assert.match(joined, /text changed 'title'/)
    assert.ok(drift.report.issues.some((i) => i.issueKind === 'SCREEN_DRIFT'))
  })

  const noShadow = run({ base: { 'Home.spec.json': spec([el('a')]) }, shadow: {} })
  check('no shadow spec → NOT_CHECKED, overall BLOCKER, non-zero exit', () => {
    assert.equal(noShadow.report.overall, 'BLOCKER')
    assert.equal(rowOf(noShadow.report, 'Home').status, 'NOT_CHECKED')
    assert.notEqual(noShadow.status, 0)
  })

  const jitter = run({
    base: { 'Home.spec.json': spec([el('a', { bboxDp: { x: 10, y: 10, w: 100, h: 20 } })]) },
    shadow: { 'Home.spec.json': spec([el('a', { bboxDp: { x: 10.4, y: 9.6, w: 100, h: 20 } })]) },
  })
  check('sub-tolerance (<=1dp) bbox jitter is not flagged as drift', () => {
    assert.equal(jitter.report.overall, 'PASS')
    assert.equal(rowOf(jitter.report, 'Home').status, 'CLEAN')
  })

  // R6 — like-for-like diffing: a chromeCrop-STAMPED baseline vs a RAW chromed shadow re-pull
  // must not read as phantom drift; the shadow is normalized IN MEMORY with the same predicate.
  const rawChromedSpec = (els = [], over = {}, sbH = 47) => spec([
    { stableId: 'sb', name: 'iOS/Status Bar', bboxDp: { x: 0, y: 0, w: 390, h: sbH } },
    { stableId: 'time', name: 'Time', bboxDp: { x: 20, y: 12, w: 40, h: 20 }, text: '9:41' },
    ...els,
    { stableId: 'hi', name: 'Home Indicator', bboxDp: { x: 0, y: 857, w: 390, h: 34 } },
  ], { frameSizeDp: { w: 390, h: 891 }, ...over })
  const normalizedBase = (els = []) => spec(els, {
    frameSizeDp: { w: 390, h: 810 },
    chromeCrop: { topDp: 47, bottomDp: 34, matched: ['9:41', 'Status Bar', 'Home Indicator'], at: 'T' },
  })
  const title = (y) => el('title', { bboxDp: { x: 16, y, w: 200, h: 30 }, text: 'Speed' })

  const r6Clean = run({
    base: { 'Home.spec.json': normalizedBase([title(53)]) },              // post-crop: y 100−47
    shadow: { 'Home.spec.json': rawChromedSpec([title(100)]) },          // raw re-pull, same design
  })
  check('R6: stamped baseline vs raw chromed shadow → CLEAN (shadow normalized in memory, no phantom drift)', () => {
    assert.equal(r6Clean.report.overall, 'PASS')
    const row = rowOf(r6Clean.report, 'Home')
    assert.equal(row.status, 'CLEAN')
    assert.equal(row.shadowNormalized, true)
  })

  const r6Real = run({
    base: { 'Home.spec.json': normalizedBase([title(53)]) },
    shadow: { 'Home.spec.json': rawChromedSpec([title(160)]) },          // REAL move: 100 → 160 in raw dp
  })
  check('R6: real drift under the chrome still surfaces (only the true change, not the crop)', () => {
    assert.equal(r6Real.report.overall, 'WARN')
    const row = rowOf(r6Real.report, 'Home')
    assert.equal(row.status, 'DRIFTED')
    const joined = row.changes.join(' | ')
    assert.match(joined, /moved\/resized 'title'/)
    assert.ok(!/frame /.test(joined), `no phantom frame change: ${joined}`)
    assert.ok(!/added 'iOS\/Status Bar'/.test(joined), `no phantom chrome adds: ${joined}`)
  })

  const r6Unstamped = run({
    base: { 'Home.spec.json': rawChromedSpec([title(100)]) },            // unstamped raw receipt baseline
    shadow: { 'Home.spec.json': rawChromedSpec([title(100)]) },
  })
  check('R6: unstamped raw baseline keeps its raw-vs-raw diff (no in-memory normalization) → CLEAN', () => {
    assert.equal(r6Unstamped.report.overall, 'PASS')
    const row = rowOf(r6Unstamped.report, 'Home')
    assert.equal(row.status, 'CLEAN')
    assert.ok(!row.shadowNormalized)
  })

  // Chrome band grew in Figma (47→50dp): every normalized coordinate shifts, which would read
  // as a cause-blind whole-screen move — the FIRST change line must name the band change.
  const r6Band = run({
    base: { 'Home.spec.json': normalizedBase([title(53)]) },             // stamped at top 47
    shadow: { 'Home.spec.json': rawChromedSpec([title(100)], {}, 50) },
  })
  check('R6: a chrome-band-only change names its cause first (band 47→50dp), not a mystery whole-screen move', () => {
    const row = rowOf(r6Band.report, 'Home')
    assert.equal(row.status, 'DRIFTED')
    assert.match(row.changes[0], /device-chrome band changed in Figma: top 47→50dp/)
  })
} catch (e) {
  fail++; console.log(`${C.red}FAIL${C.reset} check-screen-drift setup threw\n     ${e.stack || e.message}`)
}

console.log(`\ncheck-screen-drift.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
