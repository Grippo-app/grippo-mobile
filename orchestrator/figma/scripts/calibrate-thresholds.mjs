#!/usr/bin/env node
// calibrate-thresholds.mjs (--labels <file> [--reports <dir>] | --corpus <dir>) [--out <ledger.json>]
//
// OFFLINE analysis tool (H3) — derives the screenshot gate's thresholds from a labeled
// corpus. It NEVER changes gate behavior: it reads already-written `screenshot-*.json`
// reports, joins each per-screen result with a labels file (three classes:
// pass = ship it, minor = cosmetic drift, fail = must block; schema-gated by
// token-schemas/calibration-labels.schema.json), sweeps candidate thresholds, and emits
// a ledger + recommendations. `--corpus <dir>` is sugar for a curated corpus layout
// (<dir>/labels.json + <dir>/reports/ — generate-calibration-mutations.mjs writes it).
//
// Objective: a fidelity gate must never ship a broken screen, so among the thresholds
// that catch EVERY labeled should-flag case (zero false-negatives) it recommends the
// fewest-false-positives split — as the MEDIAN of the equally-good run (margin on both
// sides), at full precision — and prints the full sweep so a human can trade off when
// no clean split exists. Boundaries reported: the blocking floor (fail vs rest → config
// `major`), the worst-zone floor (per-node zones only), and the PASS floor
// (pass vs minor+fail → config `pass`).
//
// labels file shape: { "labels": [ { "screen": "Home", "theme": "primary"|null, "expect": "pass"|"minor"|"fail" }, ... ] }
//   theme omitted/null matches any theme for that screen; optional expectBand/expectIssue
//   pin exact comparator behaviour (used by the template's synthetic corpus).

import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { figmaPath, parseCli } from './_util.mjs'
import { compileSchema, schemaIssues } from './report-utils.mjs'

const cli = parseCli({
  allowedFlags: ['--labels', '--reports', '--out', '--corpus'],
  valueFlags: ['--labels', '--reports', '--out', '--corpus'],
  usage: 'usage: node scripts/calibrate-thresholds.mjs (--labels <file> [--reports <dir>] | --corpus <dir>) [--out <ledger.json>]',
})
// --corpus <dir> is path sugar for a generated/curated corpus layout
// (generate-calibration-mutations.mjs writes it): <dir>/labels.json + <dir>/reports/.
const corpusDir = cli.value('--corpus')
const labelsPath = cli.value('--labels') || (corpusDir ? join(corpusDir, 'labels.json') : null)
if (!labelsPath || !existsSync(labelsPath)) { console.error(`ERROR: --labels file not found: ${labelsPath}`); process.exit(1) }
const reportsDir = cli.value('--reports') || (corpusDir ? join(corpusDir, 'reports') : figmaPath('reports'))
const outPath = cli.value('--out') || null

let labelsDoc
try { labelsDoc = JSON.parse(readFileSync(labelsPath, 'utf8')) } catch (e) { console.error(`ERROR: labels unreadable: ${e.message}`); process.exit(1) }
// Schema gate (calibration-labels.schema.json): a hand-labeled file with a
// typo'd expect class must fail loudly here, not skew the sweep silently.
const labelsSchema = await compileSchema(figmaPath('token-schemas', 'calibration-labels.schema.json'), { gate: true })
{
  const labelIssues = schemaIssues(labelsSchema, labelsDoc)
  if (labelIssues.length) { console.error(`ERROR: labels file violates calibration-labels.schema.json: ${labelIssues.map((i) => `${i.path}: ${i.message}`).join('; ')}`); process.exit(1) }
}
const labels = labelsDoc.labels

function expectFor(screen, theme) {
  // Prefer a theme-specific label, else a theme-agnostic one for the screen.
  // Returns the three-class ground truth: 'pass' | 'minor' | 'fail'.
  let agnostic = null
  for (const l of labels) {
    if (!l || l.screen !== screen) continue
    if (l.theme && theme && l.theme !== theme) continue
    if (l.theme && theme && l.theme === theme) return l.expect
    if (!l.theme) agnostic = l.expect
  }
  return agnostic
}

// Collect (score, zoneMinSsim, expect) for every labeled per-screen result across all reports.
const samples = []
const reportFiles = existsSync(reportsDir) ? readdirSync(reportsDir).filter((f) => /^screenshot-.*\.json$/.test(f)) : []
for (const f of reportFiles) {
  let doc
  try { doc = JSON.parse(readFileSync(join(reportsDir, f), 'utf8')) } catch { continue }
  const results = Array.isArray(doc && doc.results) ? doc.results : []
  for (const r of results) {
    if (typeof r.score !== 'number') continue
    const expect = expectFor(r.screen, r.theme || r.themeKey || null)
    if (!expect) continue
    let zoneMin = null
    if (Array.isArray(r.zones)) {
      for (const z of r.zones) if (typeof z.ssim === 'number') zoneMin = zoneMin == null ? z.ssim : Math.min(zoneMin, z.ssim)
    }
    samples.push({ screen: r.screen, theme: r.theme || r.themeKey || null, score: r.score, zoneMin, expect, report: f })
  }
}

if (!samples.length) { console.error(`ERROR: no labeled results found in ${reportsDir} (re-capture the corpus first — H0)`); process.exit(2) }

// Sweep: for a metric value v, the gate FAILS a screen when value < threshold. Pick the
// threshold that catches every labeled should-flag case (0 false-negatives) with the fewest
// false-positives. `shouldFlag` picks the boundary being calibrated: the default (expect ===
// 'fail') sweeps the blocking floor; (expect !== 'pass') sweeps the PASS/MINOR boundary.
function sweep(metricFn, shouldFlag = (expect) => expect === 'fail') {
  const rows = samples.map((s) => ({ v: metricFn(s), expect: s.expect })).filter((r) => typeof r.v === 'number')
  if (!rows.length) return null
  const cands = [...new Set(rows.map((r) => r.v))].sort((a, b) => a - b)
  // candidate thresholds: midpoints + the exact values (a screen fails when v < threshold)
  const thresholds = []
  for (let i = 0; i < cands.length; i++) { thresholds.push(cands[i]); if (i + 1 < cands.length) thresholds.push((cands[i] + cands[i + 1]) / 2) }
  let best = null
  const sweepRows = []
  for (const t of thresholds) {
    let fp = 0, fn = 0, tp = 0, tn = 0
    for (const r of rows) {
      const flaggedFail = r.v < t
      if (shouldFlag(r.expect)) { if (flaggedFail) tp++; else fn++ }
      else { if (flaggedFail) fp++; else tn++ }
    }
    // FULL precision — a rounded value can land on the wrong side of a boundary
    // score (0.98128 rounded to 0.9813 flags the very pass case the printed
    // fp=0 was computed on). Display formatting is the printer's job.
    const row = { threshold: t, tp, fp, fn, tn }
    sweepRows.push(row)
    const better = !best
      || (fn < best.fn)
      || (fn === best.fn && fp < best.fp)
    if (better) best = row
  }
  // Among all thresholds sharing the best confusion, recommend the MEDIAN of the
  // run — margin on BOTH sides — instead of the old highest-tie-break, which by
  // construction landed exactly ON the minimum passing score (zero margin).
  if (best) {
    const run = sweepRows.filter((r) => r.fn === best.fn && r.fp === best.fp).sort((a, b) => a.threshold - b.threshold)
    best = run[Math.floor((run.length - 1) / 2)]
  }
  return { best, sweep: sweepRows, n: rows.length }
}

const globalSweep = sweep((s) => s.score)
const zoneSweep = sweep((s) => s.zoneMin)
// Three-class boundary: where PASS ends (minor + fail should both flag). In the
// band function this edge is T_PASS (config `pass`) — NOT `minor`, which is the
// MINOR/MAJOR edge.
const passSweep = sweep((s) => s.score, (expect) => expect !== 'pass')

const ledger = {
  schemaVersion: 1,
  kind: 'screenshot-threshold-calibration',
  reportsDir,
  labelsPath,
  sampleCount: samples.length,
  failCount: samples.filter((s) => s.expect === 'fail').length,
  minorCount: samples.filter((s) => s.expect === 'minor').length,
  passCount: samples.filter((s) => s.expect === 'pass').length,
  global: globalSweep ? { recommendedMajor: globalSweep.best.threshold, best: globalSweep.best, sweep: globalSweep.sweep } : null,
  zone: zoneSweep ? { recommendedZoneBlocker: zoneSweep.best.threshold, best: zoneSweep.best, sweep: zoneSweep.sweep } : null,
  zoneNote: zoneSweep ? null : 'no per-node zone samples — r.zones exists only when a resolved spec projects zones; grid-cell floors surface only via zoneFloorHit on FAILING screens, which this sweep cannot use (no pass-side values)',
  passBoundary: passSweep ? { recommendedPass: passSweep.best.threshold, best: passSweep.best, sweep: passSweep.sweep } : null,
}

const fmt = (t) => Number(t.toPrecision(6))
console.log(`calibrate-thresholds: ${samples.length} labeled sample(s) (${ledger.failCount} fail / ${ledger.minorCount} minor / ${ledger.passCount} pass) from ${reportFiles.length} report(s)`)
if (globalSweep) console.log(`  global mean SSIM → recommend SCREENSHOT_MAJOR_THRESHOLD=${fmt(globalSweep.best.threshold)} (fp=${globalSweep.best.fp} fn=${globalSweep.best.fn}; ledger carries full precision)`)
if (zoneSweep) console.log(`  worst-zone SSIM  → recommend SCREENSHOT_ZONE_BLOCKER_THRESHOLD=${fmt(zoneSweep.best.threshold)} (fp=${zoneSweep.best.fp} fn=${zoneSweep.best.fn})`)
else console.log('  worst-zone SSIM  → no zone samples (spec-projected zones absent; grid-cell floors are visible only on failing screens) — zone recommendation skipped')
if (passSweep) console.log(`  PASS floor       → recommend SCREENSHOT_PASS_THRESHOLD=${fmt(passSweep.best.threshold)} (config \`pass\`; fp=${passSweep.best.fp} fn=${passSweep.best.fn})`)
if (globalSweep && globalSweep.best.fn > 0) console.log(`  ⚠ no clean split for the global mean — ${globalSweep.best.fn} labeled fail(s) score above the threshold; inspect the sweep`)
if (outPath) { writeFileSync(outPath, JSON.stringify(ledger, null, 2) + '\n'); console.log(`  ledger → ${outPath}`) }
process.exit(0)
