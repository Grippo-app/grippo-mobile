// normalize-oracle.mjs — deterministic iOS device-chrome crop at the PULL boundary (R6).
//
// The Figma oracle is often an iOS export WITH device chrome (the "9:41" status bar, the home
// indicator) while the Roborazzi capture renders the composable only — a one-sided ~47px shift
// no comparator-side band or ±2px search can absorb. The comparison contract is app CONTENT vs
// design CONTENT, so chrome is stripped ONCE, where the oracle enters the system: this script
// runs as a MANDATORY step of the figma:screens pull session (after the cache write, BEFORE the
// check-screen-cache gate), so the spec gate's bboxDp, the zone grid, the comparator and the
// site three-up all see one consistent chrome-free oracle.
//
// Detection is the STRICT predicate in lib/oracle-chrome.mjs (geometry AND a name/"9:41"
// signal). Fail-closed: a geometry-only lookalike is NOT cropped — it is reported as a
// warn-grade IOS_CHROME_SUSPECTED naming the element, and the owner decides (rename the layer
// in Figma, or accept the pixels). Idempotent: a normalized spec carries the auditable
// `chromeCrop` stamp and is skipped — a second run is a byte no-op. Writes are atomic
// (tmp + rename, PNG and spec pairwise); a pair whose PNG is missing is skipped whole, never
// half-normalized. `--dry-run` prints the plan without writing (descope pattern). Never calls
// Figma. check-screen-cache re-verifies a stamped spec with the SAME predicate
// (CHROME_CROP_RESIDUE / CHROME_CROP_BAD_SHIFT / CHROME_CROP_ASPECT), so the crop is
// verifiable, not trusted.
//
// Drift flows need NO step here: check-screen-drift/sweep-done-drift normalize the raw shadow
// re-pull IN MEMORY (same lib predicate) whenever the baseline is stamped, so a normalized
// baseline never phantom-drifts against a raw shadow. An unstamped raw baseline keeps
// its raw-vs-raw diff.
//
// Usage: node scripts/normalize-oracle.mjs <TASK_STEM> [--dry-run]
//   FIGMA_SCREEN_CACHE_ROOT or FIGMA_SPEC_SCREENS_DIR — override screens cache root

import { readdirSync, readFileSync, renameSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { exists, figmaScreensRoot, info, isDirectRun, ok, parseCli, summary, warnMsg, failMsg, displayPath } from './_util.mjs'
import { assertTaskStem, writeReport } from './report-utils.mjs'
import { applyChromeCrop, detectChrome } from './lib/oracle-chrome.mjs'

const USAGE = 'usage: node scripts/normalize-oracle.mjs <TASK_STEM> [--dry-run]'

async function loadJimp() {
  try {
    const { Jimp } = await import('jimp')
    return Jimp
  } catch {
    console.error('ERROR: jimp not installed. Run `npm install` in orchestrator/figma/ first.')
    process.exit(1)
  }
}

function atomicWrite(path, data) {
  const tmp = `${path}.tmp-normalize`
  writeFileSync(tmp, data)
  renameSync(tmp, path)
}

async function main() {
  let cli
  try {
    cli = parseCli({ allowedFlags: ['--dry-run'], valueFlags: [], booleanFlags: ['--dry-run'], usage: USAGE })
  } catch (e) {
    console.error(e.message)
    process.exit(1)
  }
  let stem
  try { stem = assertTaskStem(cli.positional[0] || '') }
  catch { failMsg(USAGE); process.exit(1) }
  if (cli.positional.length !== 1) { failMsg(USAGE); process.exit(1) }
  const dryRun = cli.has('--dry-run')

  const screensDir = join(figmaScreensRoot(), stem)
  if (!exists(screensDir)) { failMsg(`screens cache missing: ${screensDir} — pull it first (figma:screens session)`); process.exit(1) }

  const specFiles = readdirSync(screensDir).filter((f) => f.endsWith('.spec.json')).sort()
  if (!specFiles.length) { failMsg(`no *.spec.json under ${screensDir}`); process.exit(1) }
  let indexNodes = {}
  try {
    const index = JSON.parse(readFileSync(join(screensDir, 'index.json'), 'utf8'))
    if (!index || !index.nodes || typeof index.nodes !== 'object' || Array.isArray(index.nodes)) throw new Error('nodes must be an object')
    indexNodes = index.nodes
  } catch (error) {
    failMsg(`invalid or missing screen-cache index.json: ${error.message}`)
    process.exit(1)
  }

  const issues = []
  const normalized = []
  const stamped = []
  const untouched = []
  let Jimp = null

  for (const f of specFiles) {
    const specPath = join(screensDir, f)
    let spec
    try { spec = JSON.parse(readFileSync(specPath, 'utf8')) } catch (e) {
      warnMsg(`SPEC_UNREADABLE  ${f} (${e.message}) — the cache gate owns blocking this; skipped`)
      issues.push({ severity: 'WARN', issueKind: 'SPEC_UNREADABLE', message: `${f} unreadable: ${e.message}`, file: displayPath(specPath) })
      continue
    }
    if (spec.chromeCrop) { stamped.push(f); continue }   // idempotency: already normalized — byte no-op
    if (!spec.frameSizeDp || !(spec.frameSizeDp.h > 0) || !(spec.frameSizeDp.w > 0)) { untouched.push(f); continue }
    const screen = String(spec.screen || f.replace(/\.dark\.spec\.json$/, '').replace(/\.spec\.json$/, ''))
    const kind = String(indexNodes[screen]?.kind || 'screen').toLowerCase()
    if (kind !== 'screen') { untouched.push(f); continue }   // dialogs/components may legitimately contain similarly named rows

    const { top, bottom, suspects } = detectChrome(spec)
    for (const s of suspects) {
      const overlapNote = top ? ' (its slice overlapping the separately MATCHED status-bar band above is cropped with that band; the rest stays)' : ''
      const msg = `${f}: element '${s.name}' matches the top-chrome GEOMETRY (y=${s.bboxDp.y}, ${s.bboxDp.w}×${s.bboxDp.h}dp) but carries no status-bar name and no "9:41" text — NOT treated as chrome (fail-closed)${overlapNote}. Owner decides: rename the layer in Figma so the predicate matches (then re-pull), or accept the pixels as content.`
      warnMsg(`IOS_CHROME_SUSPECTED  ${msg}`)
      issues.push({ severity: 'WARN', issueKind: 'IOS_CHROME_SUSPECTED', message: msg, screen: spec.screen, file: displayPath(specPath) })
    }
    if (!top && !bottom) { untouched.push(f); continue }

    const pngPath = specPath.replace(/\.spec\.json$/, '.png')
    if (!exists(pngPath)) {
      const msg = `${f} matches device chrome but its oracle PNG is missing (${displayPath(pngPath)}) — pair skipped whole (never half-normalized); the cache gate owns the missing PNG`
      warnMsg(`CHROME_PNG_MISSING  ${msg}`)
      issues.push({ severity: 'WARN', issueKind: 'CHROME_PNG_MISSING', message: msg, screen: spec.screen, file: displayPath(pngPath) })
      continue
    }

    Jimp = Jimp || await loadJimp()
    let png
    try { png = await Jimp.read(pngPath) } catch (e) {
      const msg = `${f}: oracle PNG unreadable (${e.message}) — pair skipped`
      warnMsg(`CHROME_PNG_UNREADABLE  ${msg}`)
      issues.push({ severity: 'WARN', issueKind: 'CHROME_PNG_UNREADABLE', message: msg, screen: spec.screen, file: displayPath(pngPath) })
      continue
    }
    // Pair-consistency pre-check (torn-state net): the pull writes PNG and spec from the SAME
    // frame, so before cropping, the PNG aspect must match the PRE-crop spec aspect within a
    // tight rounding bound. A torn previous run (crash between the PNG write and the spec
    // write) leaves a CROPPED PNG beside a still-chromed spec — re-deriving the export scale
    // from that PNG would DOUBLE-CROP real content and then stamp the corruption permanent.
    // An inconsistent pair is REFUSED, never guessed at; the remedy is a fresh re-pull.
    const expectedAspect = spec.frameSizeDp.w / spec.frameSizeDp.h
    const pngAspect = png.bitmap.width / png.bitmap.height
    if (Math.abs(pngAspect - expectedAspect) / expectedAspect > 0.02) {
      const msg = `${f}: oracle PNG aspect ${pngAspect.toFixed(4)} (${png.bitmap.width}×${png.bitmap.height}px) does not match the PRE-crop spec aspect ${expectedAspect.toFixed(4)} (${spec.frameSizeDp.w}×${spec.frameSizeDp.h}dp) — inconsistent pair (a torn previous run, or a wrong-scale export); crop REFUSED (a scale re-derived from this PNG would corrupt content). Re-pull this screen (figma:screens session) so both files are rewritten fresh.`
      warnMsg(`CHROME_PAIR_INCONSISTENT  ${msg}`)
      issues.push({ severity: 'WARN', issueKind: 'CHROME_PAIR_INCONSISTENT', message: msg, screen: spec.screen, file: displayPath(pngPath) })
      continue
    }
    const topDp = top ? top.topDp : 0
    const bottomDp = bottom ? bottom.bottomDp : 0
    // dp → px via the oracle's own export scale (the PNG height over the PRE-crop frame dp).
    const scale = png.bitmap.height / spec.frameSizeDp.h
    const topPx = Math.round(topDp * scale)
    const bottomPx = Math.round(bottomDp * scale)
    const newPxH = png.bitmap.height - topPx - bottomPx
    if (newPxH <= 0) {
      const msg = `${f}: computed crop (${topPx}+${bottomPx}px of ${png.bitmap.height}px) would consume the whole image — corrupt geometry, pair skipped`
      warnMsg(`CHROME_CROP_DEGENERATE  ${msg}`)
      issues.push({ severity: 'WARN', issueKind: 'CHROME_CROP_DEGENERATE', message: msg, screen: spec.screen, file: displayPath(pngPath) })
      continue
    }
    const matched = [...new Set([...(top ? top.matched : []), ...(bottom ? bottom.matched : [])])]
    const plan = `${f}: top ${topDp}dp (${topPx}px)${top ? ` [${top.elements.join(', ')}]` : ''} + bottom ${bottomDp}dp (${bottomPx}px)${bottom ? ` [${bottom.elements.join(', ')}]` : ''} — matched: ${matched.join(', ')}`
    if (dryRun) { info(`DRY-RUN would crop ${plan}`); normalized.push({ file: f, topDp, bottomDp, matched, dryRun: true }); continue }

    const { spec: cropped, dropped } = applyChromeCrop(spec, { topDp, bottomDp, matched, at: new Date().toISOString() })
    png.crop({ x: 0, y: topPx, w: png.bitmap.width, h: newPxH })
    atomicWrite(pngPath, await png.getBuffer('image/png'))
    atomicWrite(specPath, JSON.stringify(cropped, null, 2) + '\n')
    ok(`cropped ${plan}; dropped ${dropped.length} chrome element(s), frame h ${spec.frameSizeDp.h} → ${cropped.frameSizeDp.h}dp`)
    normalized.push({ file: f, topDp, bottomDp, matched, dropped })
  }

  if (stamped.length) info(`already normalized (chromeCrop stamp, untouched): ${stamped.join(', ')}`)
  if (untouched.length) info(`chrome-free (untouched, no stamp): ${untouched.join(', ')}`)

  if (!dryRun) {
    const { reportPath } = writeReport({
      name: 'normalize-oracle',
      taskStem: stem,
      mode: 'advisory',
      inputs: { screensDir: displayPath(screensDir), specs: specFiles.length },
      inputHashes: {},
      overall: issues.length ? 'WARN' : 'PASS',
      issues,
      extra: { version: 1, normalized, stamped, untouched },
    })
    info(`report: ${reportPath}`)
  }
  summary('figma:normalize-oracle')
  // Warn-grade only: normalization is a transform, not a gate — check-screen-cache and the
  // comparator own the blocking; a suspected-but-uncropped oracle surfaces there with the
  // IOS_CHROME_SUSPECTED hint on its ASPECT_MISMATCH bail.
  process.exit(0)
}

if (isDirectRun(import.meta.url)) {
  main().catch((e) => {
    console.error(`FATAL: ${e.message}`)
    process.exit(1)
  })
}
