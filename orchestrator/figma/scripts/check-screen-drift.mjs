// check-screen-drift.mjs — the LOCAL, deterministic half of screen-drift detection.
//
// "Did the Figma design of a screen we already pulled change since we pulled it?" A shipped
// screen can silently drift in Figma after `done`, and nothing detected it (ORACLE_PULL_STALE
// is an age proxy, not a content signal). Detecting real drift needs a Figma read — which,
// per the golden invariant, ONLY a spawned figma:screens session may do. So the flow splits:
//   1. a lightweight drift-pull session re-fetches each screen's spec/metadata into a SHADOW
//      dir  .cache/figma/screens/<stem>/.drift/<Screen>.spec.json  (structure only, NO images —
//      cheap) WITHOUT touching the live cache;
//   2. THIS script compares the live spec (the baseline the code was built against) against the
//      shadow spec, per screen, and writes a drift report — no Figma, fully testable.
//
// Drift = a structural change: frameSizeDp, an element added/removed (by stableId||figmaNodeId),
// moved/resized (bboxDp beyond BBOX_TOL), or a changed fill / textStyle / text / cornerRadius /
// stroke / padding. A screen with no shadow spec is NOT_CHECKED (the drift-pull has not run for
// it). Advisory (WARN on drift): drift is a signal to create an actualize task, not a gate block.
// The per-screen change list feeds that task's body. Never calls Figma; reads local files only.
//
// Usage: node scripts/check-screen-drift.mjs <TASK_STEM> [--out <path>]
//   FIGMA_SCREEN_CACHE_ROOT or FIGMA_SPEC_SCREENS_DIR — override screens cache root (must match if both are set)
import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { displayPath, exists, readJson, figmaPath, figmaScreensRoot, isDirectRun, ok, warnMsg, failMsg, info, summary, parseCli } from './_util.mjs'
import { assertTaskStem, compileSchema, fileHash, schemaIssues, writeReport } from './report-utils.mjs'
import { applyChromeCrop, detectChrome } from './lib/oracle-chrome.mjs'

const BBOX_TOL = 1   // dp — a re-pull of an unchanged node reproduces its bbox; >1dp move is real drift
const near = (a, b) => Math.abs((a || 0) - (b || 0)) <= BBOX_TOL
function canon(v) {
  if (Array.isArray(v)) return `[${v.map(canon).join(',')}]`
  if (v && typeof v === 'object') return `{${Object.keys(v).sort().map((key) => `${JSON.stringify(key)}:${canon(v[key])}`).join(',')}}`
  return JSON.stringify(v ?? null)
}
const elId = (e) => String((e && (e.stableId || e.figmaNodeId)) || '')

function currentSpecError(validate, spec, label) {
  const invalid = schemaIssues(validate, spec, `${label}:`)
  if (invalid.length) return `${invalid[0].path} ${invalid[0].message}`
  const identities = new Set()
  for (const [index, element] of spec.elements.entries()) {
    const identity = elId(element)
    if (identities.has(identity)) return `elements[${index}] duplicates comparison identity ${JSON.stringify(identity)}`
    identities.add(identity)
  }
  return null
}

function bboxChanged(a, b) {
  if (!a || !b) return !!a !== !!b
  return !(near(a.x, b.x) && near(a.y, b.y) && near(a.w, b.w) && near(a.h, b.h))
}

// Compare two specs of the same screen; return a list of human-readable change strings ([] = clean).
function diffSpec(base, fresh) {
  const changes = []
  const bf = base.frameSizeDp || {}, ff = fresh.frameSizeDp || {}
  if (!(near(bf.w, ff.w) && near(bf.h, ff.h))) changes.push(`frame ${bf.w}×${bf.h} → ${ff.w}×${ff.h}dp`)
  if (String(base.theme || '') !== String(fresh.theme || '')) changes.push(`theme ${base.theme} → ${fresh.theme}`)
  const baseEls = new Map((Array.isArray(base.elements) ? base.elements : []).filter(elId).map((e) => [elId(e), e]))
  const freshEls = new Map((Array.isArray(fresh.elements) ? fresh.elements : []).filter(elId).map((e) => [elId(e), e]))
  for (const [id, e] of baseEls) if (!freshEls.has(id)) changes.push(`removed '${e.name || id}'`)
  for (const [id, e] of freshEls) if (!baseEls.has(id)) changes.push(`added '${e.name || id}'`)
  for (const [id, a] of baseEls) {
    const b = freshEls.get(id)
    if (!b) continue
    const label = a.name || id
    if (bboxChanged(a.bboxDp, b.bboxDp)) changes.push(`moved/resized '${label}'`)
    if (canon(a.fills) !== canon(b.fills)) changes.push(`fill changed '${label}'`)
    if (canon(a.textStyle) !== canon(b.textStyle)) changes.push(`text style changed '${label}'`)
    if (String(a.text ?? '') !== String(b.text ?? '')) changes.push(`text changed '${label}': ${JSON.stringify(a.text ?? null)} → ${JSON.stringify(b.text ?? null)}`)
    if ((a.cornerRadiusDp ?? null) !== (b.cornerRadiusDp ?? null)) changes.push(`corner radius changed '${label}'`)
    if (canon(a.strokes) !== canon(b.strokes)) changes.push(`stroke changed '${label}'`)
    if (canon(a.paddingDp) !== canon(b.paddingDp)) changes.push(`padding changed '${label}'`)
  }
  return changes
}

// Core, reusable drift computation: diff EVERY `<Screen>.spec.json` under `baseDir` against the
// same-named spec under `shadowDir`. NO Figma, NO report I/O, NO process.exit — a pure function the
// CLI (baseDir = live cache, shadowDir = live `.drift/`) AND the post-ship sweep (baseDir =
// COMMITTED ship receipts, shadowDir = a fresh re-pull's `.drift/`) both call. Throws only on an
// unreadable BASE spec (a corrupt baseline is a hard error for the caller to route). `quiet`
// suppresses the per-screen console lines for the sweep (which iterates many tasks).
export function runScreenDrift(baseDir, shadowDir, { quiet = false, validateSpec } = {}) {
  if (typeof validateSpec !== 'function') throw new Error('current spec-v2 validator is required')
  const baseFiles = readdirSync(baseDir).filter((f) => f.endsWith('.spec.json')).sort()
  const inputHashes = {}
  const screens = []
  const issues = []
  let drifted = 0, checked = 0, notChecked = 0
  for (const f of baseFiles) {
    const basePath = join(baseDir, f)
    const shadowPath = join(shadowDir, f)
    const screen = f.replace(/\.dark\.spec\.json$/, '').replace(/\.spec\.json$/, '')
    const theme = /\.dark\.spec\.json$/.test(f) ? 'dark' : 'primary'
    inputHashes[basePath] = fileHash(basePath)
    if (!exists(shadowPath)) {
      screens.push({ screen, theme, status: 'NOT_CHECKED', changes: [] })
      issues.push({ severity: 'ERROR', issueKind: 'SCREEN_DRIFT_NOT_CHECKED', message: `${screen} [${theme}] has no fresh shadow spec`, screen, theme })
      notChecked++
      continue
    }
    inputHashes[shadowPath] = fileHash(shadowPath)
    let base, fresh
    try { base = readJson(basePath) } catch (e) { throw new Error(`${f} unreadable (${e.message})`) }
    try { fresh = readJson(shadowPath) } catch (e) { throw new Error(`shadow ${f} unreadable (${e.message})`) }
    const baseInvalid = currentSpecError(validateSpec, base, `baseline ${f}`)
    if (baseInvalid) throw new Error(`baseline ${f} violates current spec-v2 contract (${baseInvalid})`)
    const freshInvalid = currentSpecError(validateSpec, fresh, `shadow ${f}`)
    if (freshInvalid) throw new Error(`shadow ${f} violates current spec-v2 contract (${freshInvalid})`)
    if (base.screen !== fresh.screen) throw new Error(`${f} screen identity changed from ${JSON.stringify(base.screen)} to ${JSON.stringify(fresh.screen)}`)
    // R6 — like-for-like diffing: a chromeCrop-STAMPED baseline was normalized at pull time,
    // while the shadow re-pull is raw (structure-only re-fetch, no normalize step). Diffing
    // them directly reads as permanent phantom drift (frame h shrunk, every element "moved"
    // by topDp, chrome "added"). Normalize the shadow IN MEMORY with the SAME strict predicate
    // — per FILE, so an unstamped raw baseline keeps its raw-vs-raw diff.
    // Detection that finds nothing on the shadow (chrome removed/renamed in Figma) diffs
    // as-is: that IS a structural design change the owner should see.
    let shadowNormalized = false
    let chromeBandNote = null
    if (base.chromeCrop && !fresh.chromeCrop) {
      const { top, bottom } = detectChrome(fresh)
      if (top || bottom) {
        const appliedTop = top ? top.topDp : 0
        const appliedBottom = bottom ? bottom.bottomDp : 0
        fresh = applyChromeCrop(fresh, {
          topDp: appliedTop,
          bottomDp: appliedBottom,
          matched: [...new Set([...(top ? top.matched : []), ...(bottom ? bottom.matched : [])])],
          at: 'in-memory',
        }).spec
        shadowNormalized = true
        // A band-size change (chrome grew/shrank in Figma) shifts EVERY normalized coordinate,
        // so the per-element diffs below would read as a cause-blind whole-screen move. Name
        // the cause first: chrome is not content — the element shifts stem from the band.
        const baseTop = Number(base.chromeCrop.topDp) || 0
        const baseBottom = Number(base.chromeCrop.bottomDp) || 0
        if (Math.abs(appliedTop - baseTop) > BBOX_TOL || Math.abs(appliedBottom - baseBottom) > BBOX_TOL) {
          chromeBandNote = `device-chrome band changed in Figma: top ${baseTop}→${appliedTop}dp, bottom ${baseBottom}→${appliedBottom}dp (chrome is not content — the frame/element shifts below stem from the band change)`
        }
      }
    }
    const changes = diffSpec(base, fresh)
    if (chromeBandNote && changes.length) changes.unshift(chromeBandNote)
    checked++
    if (changes.length) {
      drifted++
      screens.push({ screen, theme, status: 'DRIFTED', changes: changes.slice(0, 30), ...(shadowNormalized ? { shadowNormalized } : {}) })
      issues.push({ severity: 'WARN', issueKind: 'SCREEN_DRIFT', message: `${screen} [${theme}] drifted in Figma since pull: ${changes.slice(0, 6).join('; ')}${changes.length > 6 ? ` (+${changes.length - 6} more)` : ''}`, screen, theme, changeCount: changes.length })
      if (!quiet) warnMsg(`SCREEN_DRIFT  ${screen} [${theme}] — ${changes.length} change(s): ${changes.slice(0, 4).join('; ')}`)
    } else {
      screens.push({ screen, theme, status: 'CLEAN', changes: [], ...(shadowNormalized ? { shadowNormalized } : {}) })
      if (!quiet) ok(`${screen} [${theme}] — no drift${shadowNormalized ? ' (shadow chrome-normalized in memory for a like-for-like diff)' : ''}`)
    }
  }
  return { baseFiles, screens, issues, inputHashes, drifted, checked, notChecked, overall: notChecked ? 'BLOCKER' : drifted ? 'WARN' : 'PASS' }
}

;(async function main() {
  if (!isDirectRun(import.meta.url)) return

  const usage = 'usage: node scripts/check-screen-drift.mjs <TASK_STEM> [--out F]'
  let cli
  try { cli = parseCli({ allowedFlags: ['--out'], valueFlags: ['--out'], usage }) }
  catch (e) { failMsg(e.message); process.exit(1) }
  let stem
  try { stem = assertTaskStem(cli.positional[0] || '') }
  catch { failMsg(usage); process.exit(1) }
  if (cli.positional.length !== 1) { failMsg(usage); process.exit(1) }
  const screensRoot = figmaScreensRoot()
  const screensDir = join(screensRoot, stem)
  if (!exists(screensDir)) { failMsg(`screens cache missing: ${screensDir} — pull it first (figma:screens session)`); process.exit(1) }
  const shadowDir = join(screensDir, '.drift')

  const baseFiles = readdirSync(screensDir).filter((f) => f.endsWith('.spec.json')).sort()
  if (!baseFiles.length) { failMsg(`no *.spec.json under ${screensDir}`); process.exit(1) }

  let validateSpec
  try { validateSpec = await compileSchema(figmaPath('token-schemas', 'spec.schema.json'), { gate: true }) }
  catch (e) { failMsg(`current spec-v2 schema unavailable (${e.message})`); process.exit(1) }
  let res
  try { res = runScreenDrift(screensDir, shadowDir, { validateSpec }) } catch (e) { failMsg(e.message); process.exit(1) }

  const { reportPath } = writeReport({
    name: 'screen-drift',
    taskStem: stem,
    mode: 'advisory',
    inputs: { screensDir: displayPath(screensDir), shadowDir: displayPath(shadowDir), baseSpecs: res.baseFiles.length, checked: res.checked, drifted: res.drifted, notChecked: res.notChecked },
    inputHashes: res.inputHashes,
    overall: res.overall,
    issues: res.issues,
    extra: { version: 1, screens: res.screens },
    outPath: cli.value('--out') || '',
  })
  info(`report: ${reportPath}`)
  info(`screens: ${res.baseFiles.length}, checked: ${res.checked}, drifted: ${res.drifted}, not-yet-checked: ${res.notChecked}`)
  if (res.notChecked) failMsg(`${res.notChecked} screen(s) were not freshly checked — the drift run is incomplete`)
  process.exit(summary('figma:check-screen-drift')) // checked drift is advisory; incomplete evidence is an execution failure
})().catch((e) => { failMsg(e?.message || String(e)); process.exit(summary('figma:check-screen-drift')) })
