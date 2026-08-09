// check-stub-text.mjs — mechanical text-CONTENT parity (advisory): the design's visible
// strings must exist in the code.
//
// The pixel gate is deliberately lenient on text STRUCTURE (font-AA ≠ Figma) and
// spec-compare checks text STYLE (size/weight/case) — so "right style, wrong words"
// (an invented stub value like "312 Mbps" where the design shows "128 Mbps") was covered
// by nothing mechanical. This closes that hole from the spec side: every element `text`
// the pull session recorded (spec.schema.json, VERBATIM contract) is searched across the
// product's Kotlin + resource XML sources — screenshot stubs mirror the oracle content
// per the fidelity gate's §2 content-parity rule, so a design string that appears NOWHERE
// in code cannot render and means the stub (or the copy) drifted.
//
// Honest limits, hence ADVISORY (WARN, exit 0): runtime-composed strings ("%s км",
// number formatters) legitimately split design text across code; a spec without any
// `text` fields (pulled before the text contract) yields PASS with a visible note, never
// a false alarm. Search is whitespace-normalized + case-insensitive (textStyle.case can
// legally re-case the glyphs).
//
// Usage: node scripts/check-stub-text.mjs <TASK_STEM> [--code-root D]
//   FIGMA_SCREEN_CACHE_ROOT or FIGMA_SPEC_SCREENS_DIR — override screens cache root (must match if both are set)
//   FIGMA_CENSUS_CODE_ROOTS  — path-delimited code roots (default: the product repo root)
import { readdirSync, readFileSync } from 'node:fs'
import { delimiter, join } from 'node:path'
import { displayPath, exists, readJson, figmaPath, figmaScreensRoot, isDirectRun, ok, warnMsg, failMsg, info, summary, PROJECT_ROOT } from './_util.mjs'
import { assertTaskStem, fileHash, writeReport } from './report-utils.mjs'

function arg(flag) {
  const i = process.argv.indexOf(flag)
  return i > -1 && process.argv[i + 1] ? process.argv[i + 1] : undefined
}

const SCAN_SKIP_DIRS = new Set(['.git', '.gradle', '.idea', 'build', 'node_modules', 'orchestrator'])
const norm = (s) => String(s || '').replace(/\s+/g, ' ').trim().toLowerCase()

function collectSourceFiles(root, out) {
  let entries = []
  try { entries = readdirSync(root, { withFileTypes: true }) } catch { return out }
  for (const d of entries) {
    if (d.isDirectory()) {
      if (SCAN_SKIP_DIRS.has(d.name)) continue   // test sources INCLUDED on purpose — the stub IS a test source
      collectSourceFiles(join(root, d.name), out)
    } else if (/\.(kt|xml)$/.test(d.name)) out.push(join(root, d.name))
  }
  return out
}

;(function main() {
  if (!isDirectRun(import.meta.url)) return

  let stem
  try { stem = assertTaskStem(process.argv[2] || '') } catch { failMsg('usage: node scripts/check-stub-text.mjs <TASK_STEM> [--code-root D] [--out F]'); process.exit(1) }

  const screensRoot = figmaScreensRoot()
  const screensDir = join(screensRoot, stem)
  if (!exists(screensDir)) { failMsg(`screens cache missing: ${screensDir} — pull it first (figma:screens session)`); process.exit(1) }

  const codeRootArg = arg('--code-root')
  const codeRoots = codeRootArg
    ? [codeRootArg]
    : (process.env.FIGMA_CENSUS_CODE_ROOTS ? process.env.FIGMA_CENSUS_CODE_ROOTS.split(delimiter).filter(Boolean) : [PROJECT_ROOT])

  // 1. Collect every recorded design text across the stem's specs (both themes).
  const specFiles = readdirSync(screensDir).filter((f) => f.endsWith('.spec.json')).sort()
  if (!specFiles.length) { failMsg(`no *.spec.json under ${screensDir}`); process.exit(1) }
  const texts = new Map()   // normalized -> { text, screens:Set, stableIds:Set }
  const inputHashes = {}
  let specsWithText = 0
  for (const f of specFiles) {
    const p = join(screensDir, f)
    let spec
    try { spec = readJson(p) } catch (e) { failMsg(`${f} unreadable (${e.message})`); process.exit(1) }
    inputHashes[p] = fileHash(p)
    let sawText = false
    // Read text from elements[] AND nodes[] (schemaVersion:2 carries `text` on nodes): a v2
    // spec whose elements[] projection dropped `text` would otherwise silently contribute zero
    // strings. Dedup by normalized value below, so an element+node carrying the same text is fine.
    const carriers = [
      ...(Array.isArray(spec.elements) ? spec.elements : []),
      ...(Array.isArray(spec.nodes) ? spec.nodes : []),
    ]
    for (const e of carriers) {
      if (!e || typeof e.text !== 'string') continue
      const n = norm(e.text)
      if (n.length < 2) continue   // single glyphs ("+", "×") match everything — no signal
      sawText = true
      const slot = texts.get(n) || { text: e.text.replace(/\s+/g, ' ').trim(), screens: new Set(), stableIds: new Set() }
      slot.screens.add(String(spec.screen || f))
      if (e.stableId) slot.stableIds.add(e.stableId)
      texts.set(n, slot)
    }
    if (sawText) specsWithText++
  }

  const issues = []
  const missing = []
  let searchedFiles = 0
  if (!texts.size) {
    // Pre-text-contract pull: nothing to check is a visible NOTE, never a false alarm.
    info(`spec(s) carry no element text content (pulled before the text contract) — nothing to verify; re-pull to enable content parity`)
  } else {
    // 2. One normalized haystack per source file; every needle must appear in SOME file.
    const files = codeRoots.flatMap((r) => (exists(r) ? collectSourceFiles(r, []) : []))
    searchedFiles = files.length
    const pending = new Map(texts)
    for (const file of files) {
      if (!pending.size) break
      let hay
      try { hay = norm(readFileSync(file, 'utf8')) } catch { continue }
      for (const [n] of pending) { if (hay.includes(n)) pending.delete(n) }
    }
    for (const [n, slot] of pending) {
      missing.push({ text: slot.text, screens: [...slot.screens].sort(), stableIds: [...slot.stableIds].sort() })
    }
    missing.sort((a, b) => a.text.localeCompare(b.text))
    for (const m of missing.slice(0, 20)) {
      issues.push({ severity: 'WARN', issueKind: 'TEXT_NOT_IN_CODE', message: `design text "${m.text}" (${m.screens.join(', ')}) appears nowhere in the code — the stub/copy drifted from the design, or the string is runtime-composed`, text: m.text, screens: m.screens, stableIds: m.stableIds })
      warnMsg(`TEXT_NOT_IN_CODE  "${m.text}" — ${m.screens.join(', ')}`)
    }
    if (missing.length > 20) issues.push({ severity: 'WARN', issueKind: 'TEXT_NOT_IN_CODE_TRUNCATED', message: `${missing.length - 20} more design strings not found in code (report bounded at 20 issues)`, droppedCount: missing.length - 20 })
    if (!missing.length) ok(`all ${texts.size} design string(s) found in code (${searchedFiles} source files searched)`)
  }

  const overall = issues.length ? 'WARN' : 'PASS'
  // outPath only when --out was given: writeReport's own default honors FIGMA_REPORTS_DIR.
  const OUT = arg('--out') || ''
  const { reportPath } = writeReport({
    name: 'stub-text',
    taskStem: stem,
    mode: 'advisory',
    inputs: { screensDir: displayPath(screensDir), codeRoots: codeRoots.map((r) => displayPath(r)), specFiles: specFiles.length, specsWithText, textCount: texts.size, searchedFiles },
    inputHashes,
    overall,
    issues,
    extra: { version: 1, textCount: texts.size, missing: missing.slice(0, 50) },
    outPath: OUT,
  })
  info(`report: ${reportPath}`)
  info(`design strings: ${texts.size}, missing in code: ${missing.length}`)
  summary('figma:check-stub-text')
  // Advisory by design (see header): drift surfaces as WARN in the report + task summary;
  // promotion to a blocker is an owner decision once the text contract has soaked.
  process.exit(0)
})()
