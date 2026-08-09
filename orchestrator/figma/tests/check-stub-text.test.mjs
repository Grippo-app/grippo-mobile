// check-stub-text.test.mjs — pins for the mechanical text-content parity check:
// a design string present in code passes; an invented-stub divergence (design says
// "128 Mbps", code stubs "312 Mbps") is a named WARN; whitespace/case differences do
// not false-alarm; a pre-text-contract spec (no `text` fields) is a PASS note, never
// a false alarm; the check stays advisory (exit 0 even with findings).
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'

const HERE = dirname(fileURLToPath(import.meta.url))
const SCRIPT = join(HERE, '..', 'scripts', 'check-stub-text.mjs')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

function run({ specs, kotlin }) {
  const ws = mkdtempSync(join(tmpdir(), 'stub-text-'))
  try {
    const sdir = join(ws, 'screens', 'TASK_1_fixture'), code = join(ws, 'code'), reports = join(ws, 'reports')
    mkdirSync(sdir, { recursive: true }); mkdirSync(code, { recursive: true }); mkdirSync(reports, { recursive: true })
    for (const [name, spec] of Object.entries(specs)) writeFileSync(join(sdir, name), JSON.stringify(spec))
    if (kotlin != null) writeFileSync(join(code, 'ScreenshotTest.kt'), kotlin)
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--code-root', code], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: reports },
      encoding: 'utf8',
    })
    const report = JSON.parse(readFileSync(join(reports, 'stub-text-TASK_1_fixture.json'), 'utf8'))
    return { status: r.status, stdout: r.stdout + r.stderr, report }
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
}

const el = (text, id) => ({ stableId: id, name: id, bboxDp: { x: 0, y: 0, w: 100, h: 20 }, text })
const spec = (elements) => ({ screen: 'Home', frameSizeDp: { w: 390, h: 844 }, theme: 'dark', elements })

try {
  const good = run({
    specs: { 'Home.spec.json': spec([el('128 Mbps', 'speed-value'), el('Download', 'speed-label')]) },
    kotlin: 'package t\nval stub = SpeedState(value = "128  MBPS", label = "Download")\n',
  })
  check('design strings found in code (whitespace + case normalized) → PASS', () => {
    assert.equal(good.status, 0)
    assert.equal(good.report.overall, 'PASS')
    assert.deepEqual(good.report.missing, [])
  })

  const drift = run({
    specs: { 'Home.spec.json': spec([el('128 Mbps', 'speed-value'), el('Download', 'speed-label')]) },
    kotlin: 'package t\nval stub = SpeedState(value = "312 Mbps", label = "Download")\n',
  })
  check('invented stub value → named TEXT_NOT_IN_CODE WARN, still exit 0 (advisory)', () => {
    assert.equal(drift.status, 0)
    assert.equal(drift.report.overall, 'WARN')
    const hit = drift.report.issues.find((i) => i.issueKind === 'TEXT_NOT_IN_CODE')
    assert.ok(hit && hit.text === '128 Mbps')
    assert.deepEqual(drift.report.missing.map((m) => m.text), ['128 Mbps'])
  })

  const textless = run({
    specs: { 'Home.spec.json': spec([{ stableId: 'x', name: 'x', bboxDp: { x: 0, y: 0, w: 9, h: 9 } }]) },
    kotlin: 'package t\n',
  })
  check('textless spec → PASS note, zero issues', () => {
    assert.equal(textless.status, 0)
    assert.equal(textless.report.overall, 'PASS')
    assert.equal(textless.report.issues.length, 0)
    assert.equal(textless.report.inputs.textCount, 0)
  })

  const single = run({
    specs: { 'Home.spec.json': spec([el('+', 'plus-icon'), el('OK', 'ok-btn')]) },
    kotlin: 'package t\nval s = "OK"\n',
  })
  check('single-glyph strings are skipped (no signal), 2+ chars are checked', () => {
    assert.equal(single.report.overall, 'PASS')
    assert.equal(single.report.inputs.textCount, 1)
  })

  // v2 spec carrying `text` on nodes[] (not elements[]) is still read — an invented stub flags.
  // Keep the fixture schema-valid: v2 requires source/root metadata and a non-empty comparable
  // elements[] projection, even when text lives only on semantic nodes[].
  const v2 = run({
    specs: { 'Home.spec.json': {
      schemaVersion: 2,
      screen: 'Home',
      frameSizeDp: { w: 390, h: 844 },
      theme: 'dark',
      source: { fileKey: 'file', nodeId: '1:1' },
      rootNodeId: '1:1',
      coordinateSystem: { units: 'dp' },
      themeMetadata: { themeKey: 'dark' },
      elements: [{ stableId: 'projection', name: 'projection', bboxDp: { x: 0, y: 0, w: 9, h: 9 } }],
      nodes: [{ stableId: 'v', name: 'v', bboxDp: { x: 0, y: 0, w: 9, h: 9 }, text: '128 Mbps' }],
    } },
    kotlin: 'package t\nval stub = SpeedState(value = "312 Mbps")\n',
  })
  check('v2 node text (elements[] projection has no text) is scanned → invented stub still flagged', () => {
    assert.equal(v2.report.inputs.textCount, 1)
    assert.ok(v2.report.issues.some((i) => i.issueKind === 'TEXT_NOT_IN_CODE' && i.text === '128 Mbps'))
  })
} catch (e) {
  fail++; console.log(`${C.red}FAIL${C.reset} check-stub-text setup threw\n     ${e.stack || e.message}`)
}

console.log(`\ncheck-stub-text.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
