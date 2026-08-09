// sweep-done-drift.mjs — post-ship "drift-auto-stale".
//
// A shipped, green-certified UI task can silently rot: the Figma design MOVES after the task
// reached done/, and nothing marks the certificate stale (the digest's designHash only catches a
// `## Design`-TEXT edit, not a Figma-side move of an unchanged ref). This sweep closes that gap.
//
// For each DONE UI task it diffs the COMMITTED ship-receipt spec baseline (what the code was
// CERTIFIED against — ship-done snapshots it into evidence/figma-ship/<stem>/specs/) against a
// FRESH shadow re-pull, and on drift writes a COMMITTED evidence/figma-ship/<stem>/drift-stale-<stem>.json.
// verify-done reads that marker into a SEPARATE staleness tally (never a violation — staleness is a
// "your design moved" signal, not forgery, so it must not fail the audit / exit non-zero).
//
// GOLDEN INVARIANT: this sweep makes NO Figma call. The shadow `.drift/` specs are re-fetched by a
// spawned figma:screens drift session (the ONLY thing allowed to read Figma); this reads local
// files only. Baseline = committed receipts (NOT the live cache, which a re-pull could re-stale).
//
// Usage: node scripts/sweep-done-drift.mjs [<TASK_STEM>]   (no stem = every done UI task)
//   FIGMA_SHIP_RECEIPTS_DIR — override committed receipts root
//   FIGMA_SCREEN_CACHE_ROOT or FIGMA_SPEC_SCREENS_DIR — override screens cache root (must match if both are set)
//   FIGMA_DONE_DIR          — override the done/ task dir (tests)
//   FIGMA_SWEEP_STAMP       — override the staleAt timestamp (tests; default: now, ISO)
import { existsSync, readdirSync, readFileSync, writeFileSync, renameSync, mkdirSync, rmSync } from 'node:fs'
import { join } from 'node:path'
import { createRequire } from 'node:module'
import { PROJECT_ROOT, figmaPath, figmaScreensRoot, isDirectRun, ok, warnMsg, failMsg, info, summary } from './_util.mjs'
import { runScreenDrift } from './check-screen-drift.mjs'
import { parseFigmaMeta } from './figma-meta.mjs'
import { assertTaskStem, compileSchema } from './report-utils.mjs'

const requireCjs = createRequire(import.meta.url)
const { hasPullableDesign } = requireCjs('./design-parser.cjs')
const shipDriftContract = requireCjs('./ship-drift-contract.cjs')

// Read the committed receipt digest's pipelineRunId (the certification the baseline came from) so a
// drift marker records exactly which ship it invalidated.
function baselineRunId(receiptsDir, stem) {
  const path = join(receiptsDir, `figma-meta-${stem}.txt`)
  let meta
  try { meta = parseFigmaMeta(readFileSync(path, 'utf8')) }
  catch (e) { throw new Error(`${stem}: baseline receipt digest unreadable (${e.message})`) }
  if (!meta || !meta.pipelineRunId) throw new Error(`${stem}: baseline receipt digest has no pipelineRunId`)
  return meta.pipelineRunId
}

async function main() {
  if (!isDirectRun(import.meta.url)) return
  const arg = (process.argv[2] || '').trim()
  if ((arg && arg.startsWith('--')) || process.argv.length > 3) { failMsg('usage: node scripts/sweep-done-drift.mjs [<TASK_STEM>]'); process.exit(1) }
  const onlyStem = arg
  if (onlyStem) try { assertTaskStem(onlyStem) }
  catch { failMsg('usage: node scripts/sweep-done-drift.mjs [<TASK_STEM>]'); process.exit(1) }
  const stamp = process.env.FIGMA_SWEEP_STAMP || new Date().toISOString()
  if (!Number.isFinite(Date.parse(stamp))) { failMsg(`FIGMA_SWEEP_STAMP must be an ISO timestamp, got ${JSON.stringify(stamp)}`); process.exit(1) }
  const doneDir = process.env.FIGMA_DONE_DIR || join(PROJECT_ROOT, 'orchestrator', 'tasks', 'done')
  const receiptsRoot = process.env.FIGMA_SHIP_RECEIPTS_DIR || join(PROJECT_ROOT, 'orchestrator', 'tasks', 'evidence', 'figma-ship')
  const screensRoot = figmaScreensRoot()
  if (!existsSync(doneDir)) {
    if (onlyStem) { failMsg(`done/ directory missing; cannot sweep requested ${onlyStem}`); process.exit(1) }
    info('sweep-done-drift: no done/ dir — nothing to sweep'); process.exit(0)
  }
  const validateSpec = await compileSchema(figmaPath('token-schemas', 'spec.schema.json'), { gate: true })

  const files = readdirSync(doneDir).filter((f) => f.endsWith('.md')).sort()
  let swept = 0, drifted = 0, skipped = 0, cleared = 0
  let matchedStem = !onlyStem
  for (const file of files) {
    const stem = file.replace(/\.md$/, '')
    try { assertTaskStem(stem) }
    catch { failMsg(`${file}: non-canonical task filename`); skipped++; continue }
    if (onlyStem && stem !== onlyStem) continue
    matchedStem = true
    let md = ''
    try { md = readFileSync(join(doneDir, file), 'utf8') }
    catch (e) { failMsg(`${stem}: done task unreadable (${e.message})`); skipped++; continue }
    if (!hasPullableDesign(md)) continue   // non-UI or no pullable `## Design` — nothing to compare

    const baseDir = join(receiptsRoot, stem, 'specs')
    const shadowDir = join(screensRoot, stem, '.drift')
    const stalePath = join(receiptsRoot, stem, `drift-stale-${stem}.json`)
    if (!existsSync(baseDir)) { failMsg(`${stem}: committed spec baseline missing`); skipped++; continue }
    if (!existsSync(shadowDir)) { failMsg(`${stem}: fresh drift shadow missing`); skipped++; continue }

    let res
    try { res = runScreenDrift(baseDir, shadowDir, { quiet: true, validateSpec }) }
    catch (e) { failMsg(`${stem}: drift inputs invalid (${e.message})`); skipped++; continue }
    swept++
    const driftedScreens = res.screens.filter((s) => s.status === 'DRIFTED')
    if (driftedScreens.length) {
      drifted++
      mkdirSync(join(receiptsRoot, stem), { recursive: true })
      // Atomic write into the COMMITTED tree (tmp + rename): the external commit-only watcher
      // (DEV-NOTES.md) can `git add` mid-write, and verify-done JSON.parses this marker — a
      // half-written drift-stale-<stem>.json would throw there. rename is atomic on one FS.
      const staleTmp = stalePath + '.tmp'
      const marker = shipDriftContract.marker({
        taskStem: stem,
        staleAt: stamp,
        baselineRunId: baselineRunId(join(receiptsRoot, stem), stem),
        driftedScreens: driftedScreens.map((s) => ({ screen: s.screen, theme: s.theme, changes: s.changes })),
      })
      writeFileSync(staleTmp, JSON.stringify(marker, null, 2) + '\n')
      renameSync(staleTmp, stalePath)
      warnMsg(`STALE  ${stem} — ${driftedScreens.length} screen(s) drifted since ship: ${driftedScreens.map((s) => `${s.screen}[${s.theme}]`).join(', ')}`)
    } else if (res.notChecked > 0 || res.checked === 0) {
      // PARTIAL re-pull: drift can be neither confirmed nor cleared. NEVER clear an existing stale marker here
      // — a re-pull that skipped the very screen that moved would otherwise LAUNDER real drift to
      // green (a fail-closed hole the review caught). Leave any marker untouched; report the gap.
      failMsg(`${stem} — re-pull incomplete (${res.notChecked} screen(s) not re-fetched); ${existsSync(stalePath) ? 'keeping the existing stale marker' : 'no verdict'}`)
      skipped++
    } else if (existsSync(stalePath)) {
      // Fully re-verified CLEAN (every baseline screen re-fetched + compared, none drifted) → the
      // drift resolved (re-actualized + re-shipped, or Figma reverted). Only NOW clear the marker.
      rmSync(stalePath, { force: true })
      cleared++
      ok(`${stem} — no drift (cleared stale marker)`)
    } else {
      ok(`${stem} — no drift`)
    }
  }
  if (!matchedStem) { failMsg(`requested done task ${onlyStem} does not exist`); skipped++ }
  info(`sweep-done-drift: swept ${swept}, drifted ${drifted}, cleared ${cleared}, skipped ${skipped}`)
  process.exit(summary('figma:sweep-done-drift')) // detected drift is advisory; incomplete/invalid sweep inputs fail the command
}
main().catch((e) => { failMsg(e?.message || String(e)); process.exit(summary('figma:sweep-done-drift')) })
