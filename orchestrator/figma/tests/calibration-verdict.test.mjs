// calibration-verdict.test.mjs — the template's threshold REGRESSION PIN.
//
// Generates the synthetic calibration corpus (generate-calibration-mutations.mjs
// from tests/calibration/recipes.json — ground truth by construction), runs
// the real compare-screenshots.mjs over every pair under the COMMITTED
// screenshot-thresholds.json, and asserts each case reproduces its labeled
// expectBand. A metric change or a threshold edit that silently shifts any
// verdict fails here — in figma:verify — instead of shipping. Then pins the
// corpus ↔ calibrate-thresholds contract: the calibrator must ingest the same
// labels + report via --corpus and find a clean fail split.
//
// This pins the TEMPLATE'S OWN committed numbers (doctrine: fidelity-gate §7.1);
// per-project routing (screenshotPixelGate) and per-project re-tuning are
// untouched. 0 quota, no Figma.
// Run: node orchestrator/figma/tests/calibration-verdict.test.mjs
import { copyFileSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { execFileSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { bytesHash } from '../runtime/canonical-json.mjs'
import { sourceIdentity, validObservedCapture } from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const GEN = join(HERE, '..', 'scripts', 'generate-calibration-mutations.mjs')
const COMPARE = join(HERE, '..', 'scripts', 'compare-screenshots.mjs')
const CALIBRATE = join(HERE, '..', 'scripts', 'calibrate-thresholds.mjs')

const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const ws = mkdtempSync(join(tmpdir(), 'calib-verdict-'))
try {
  // 1. Generate the corpus.
  const corpus = join(ws, 'corpus')
  execFileSync('node', [GEN, '--out', corpus], { stdio: 'pipe' })
  const labels = JSON.parse(readFileSync(join(corpus, 'labels.json'), 'utf8')).labels
  check('generator emits the corpus + schema-shaped labels', () => {
    assert.ok(labels.length >= 5, `expected >=5 cases, got ${labels.length}`)
    for (const l of labels) assert.ok(l.screen && l.expect && l.expectBand, JSON.stringify(l))
  })

  // 2. One compare workspace carrying EVERY pair (one report, one row per case).
  const sdir = join(ws, 'screens', 'TASK_1_fixture'), rdir = join(ws, 'robo'), reportDir = join(ws, 'reports'), cacheRoot = join(ws, 'figma-cache')
  mkdirSync(sdir, { recursive: true }); mkdirSync(rdir, { recursive: true }); mkdirSync(reportDir, { recursive: true })
  const nodes = {}
  const captures = []
  const fetchedAt = new Date().toISOString()
  for (let i = 0; i < labels.length; i++) {
    const l = labels[i]
    const nodeId = `1:${i + 1}`
    const url = `https://www.figma.com/design/calibration?node-id=1-${i + 1}`
    const variantId = 'primary-default-shared'
    const capture = validObservedCapture({
      source: sourceIdentity({
        nodeId,
        context: { theme: 'primary', locale: 'default', platform: 'shared' },
        origin: { kind: 'task-screen', taskStem: 'TASK_1_fixture', screenKey: l.screen, variantId },
      }),
      captureOperationId: `tokop_${(i + 1).toString(16).padStart(16, '0')}`,
      captureSequence: 1,
    })
    const tokenBytes = Buffer.from(JSON.stringify(capture, null, 2) + '\n')
    const tokensFile = `calibration-${i + 1}.tokens.json`
    writeFileSync(join(sdir, tokensFile), tokenBytes)
    nodes[l.screen] = {
      kind: 'screen', url, nodeId, fetchedAt,
      variants: [{
        id: variantId, theme: 'primary', locale: 'default', platform: 'shared',
        url, nodeId, fetchedAt, imageFile: `${l.screen}.png`,
        tokensFile,
        tokensHash: bytesHash(tokenBytes),
        captureOperationId: capture.captureOperationId,
        captureSequence: capture.captureSequence,
      }],
    }
    copyFileSync(join(corpus, 'pairs', `${l.screen}.png`), join(sdir, `${l.screen}.png`))
    copyFileSync(join(corpus, 'pairs', `${l.screen}Screenshot.png`), join(rdir, `${l.screen}Screenshot.png`))
    captures.push({
      captureName: `${l.screen}Screenshot.png`,
      path: join(rdir, `${l.screen}Screenshot.png`),
      nodeId,
      variantId,
      primaryState: true,
    })
  }
  writeFileSync(join(sdir, 'index.json'), JSON.stringify({ schemaVersion: 3, taskStem: 'TASK_1_fixture', nodes }))
  const captureManifest = join(ws, 'capture-manifest.json')
  writeFileSync(captureManifest, JSON.stringify({ captures }))
  // Strip ALL ambient SCREENSHOT_* overrides: the pin's whole claim is "under
  // the COMMITTED screenshot-thresholds.json", and calibration work is exactly
  // where a maintainer's shell carries leftover SCREENSHOT_* exports — an
  // inherited knob would silently re-point the pin (false red after a
  // legitimate config change, or false green over a real regression).
  const cleanEnv = Object.fromEntries(Object.entries(process.env).filter(([k]) => !k.startsWith('SCREENSHOT_')))
  const env = {
    ...cleanEnv,
    FIGMA_CACHE_ROOT: cacheRoot,
    FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'),
    FIGMA_REPORTS_DIR: reportDir,
    FIGMA_PIPELINE_RUN_ID: 'calibration-verdict-test',
    ROBORAZZI_OUTPUT_DIR: rdir,
    SCREENSHOT_CAPTURE_MANIFEST: captureManifest,
    SCREENSHOT_PIXEL_GATE: 'strict',   // config-independent: pin the strict routing
  }
  execFileSync('node', [COMPARE, 'TASK_1_fixture'], { env, stdio: 'pipe' })
  const report = JSON.parse(readFileSync(join(reportDir, 'screenshot-TASK_1_fixture.json'), 'utf8'))
  const byScreen = new Map(report.results.map((r) => [r.screen, r]))

  // 3. The pin: every labeled band reproduces under the committed thresholds.
  for (const l of labels) {
    check(`corpus pin: ${l.screen} → ${l.expectBand} (${l.note})`, () => {
      const row = byScreen.get(l.screen)
      assert.ok(row, `no report row for ${l.screen}`)
      assert.equal(row.status, l.expectBand,
        `status=${row.status} score=${row.score} zoneFloorHit=${JSON.stringify(row.zoneFloorHit || null)}`)
      if (l.expectIssue) {
        const hit = (report.issues || []).some((i) => i.issueKind === l.expectIssue
          && (i.screen === l.screen || String(i.message || '').includes(l.screen)))
        assert.ok(hit, `expected a ${l.expectIssue} issue for ${l.screen}; issues: ${JSON.stringify((report.issues || []).map((i) => [i.issueKind, i.screen || '']))}`)
      }
    })
  }

  // 4. Corpus ↔ calibrator contract: --corpus ingests labels + reports and the
  //    fail class splits cleanly (fn=0) on this ground-truth corpus.
  const corpusReports = join(corpus, 'reports')
  mkdirSync(corpusReports, { recursive: true })
  copyFileSync(join(reportDir, 'screenshot-TASK_1_fixture.json'), join(corpusReports, 'screenshot-corpus.json'))
  const ledgerPath = join(ws, 'ledger.json')
  const calOut = execFileSync('node', [CALIBRATE, '--corpus', corpus, '--out', ledgerPath], { stdio: 'pipe' }).toString()
  check('calibrate-thresholds --corpus: ingests the corpus and splits the fail class cleanly', () => {
    const ledger = JSON.parse(readFileSync(ledgerPath, 'utf8'))
    assert.equal(ledger.sampleCount, labels.length, calOut)
    assert.equal(ledger.failCount, labels.filter((l) => l.expect === 'fail').length, calOut)
    assert.ok(ledger.global && ledger.global.best, calOut)
    assert.equal(ledger.global.best.fn, 0, `global sweep left false-negatives: ${JSON.stringify(ledger.global.best)}`)
  })
  check('calibrate-thresholds --corpus: reports the PASS-floor boundary (three-class labels)', () => {
    const ledger = JSON.parse(readFileSync(ledgerPath, 'utf8'))
    assert.ok(ledger.passBoundary && typeof ledger.passBoundary.recommendedPass === 'number', JSON.stringify(ledger.passBoundary))
    // The recommendation must sit strictly BETWEEN the classes it separates
    // (median-of-best-run — margin on both sides, no zero-margin boundary pick).
    assert.equal(ledger.passBoundary.best.fn, 0, JSON.stringify(ledger.passBoundary.best))
    assert.equal(ledger.passBoundary.best.fp, 0, JSON.stringify(ledger.passBoundary.best))
  })
} catch (e) {
  fail++; console.log(`${C.red}FAIL${C.reset} calibration-verdict setup threw\n     ${e.stdout ? e.stdout.toString() : ''}${e.stderr ? e.stderr.toString() : ''}${e.message}`)
} finally {
  rmSync(ws, { recursive: true, force: true })
}

console.log(`\ncalibration-verdict.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
