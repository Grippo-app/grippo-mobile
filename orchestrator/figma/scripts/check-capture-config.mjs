// check-capture-config.mjs — the capture-config drift gate plus builder-owned repair mode.
//
// The screenshot `@Config(qualifiers = "w<W>dp-h<H>dp")` is DERIVED data: it must equal the
// oracle's `frameSizeDp` (fidelity-gate §3 — "omitting the qualifier is a gate failure, not a
// stylistic choice"). But nothing MECHANICALLY re-verified it: a test written once (or before
// the design changed) kept its stale/absent qualifier, rendered at Robolectric's ~320×470
// host default, and only surfaced late as an `ASPECT_MISMATCH` after a full Roborazzi run —
// or, within tolerance, silently scored resampling noise.
//
// This gate treats capture config like every other derived-and-verified fact (design-source
// hash, threshold canon, screen-cache identity): it STATICALLY, on every run, matches each
// `## Design` spec's `frameSizeDp` to the `@Test` that captures it — identity-first via the
// `SCREENSHOT_CAPTURE_MANIFEST` (capture basename → nodeId → index.json node, the same H1
// binding the comparator uses; an off-convention capture name stays verified), falling back
// to the `captureRoboImage(".../<Screen>Screenshot.png")` filename shape — and checks the
// test's EFFECTIVE `@Config(qualifiers)` (method-level over class-level, like Robolectric
// resolves it):
//   • no qualifier / no w-h            → CAPTURE_CONFIG_MISSING (BLOCKER)
//   • w or h differ from frameSizeDp   → CAPTURE_CONFIG_DRIFT   (BLOCKER)
//   • wrong/absent LOCALE segment      → the same MISSING/DRIFT class (R5 — see below)
//   • design language underivable      → CAPTURE_LOCALE_UNDERIVABLE (BLOCKER, fail-closed —
//                                        remedy: declare `designLocale` in project-config)
//   • no @Test captures this spec      → CAPTURE_TEST_ABSENT    (WARN — the comparator's
//                                        MISSING_CAPTURE owns the hard block; here it is a hint)
//
// The expected qualifier is `[<locale>[-ldrtl]-]w<W>dp-h<H>dp` (R5): the size comes from the
// spec's `frameSizeDp`, the LOCALE segment from the design language — the committed
// `designLocale` config key when declared, else derived deterministically by
// `lib/design-locale.mjs` (the specs' `elements[].text` votes against each supported locale's
// string resources; an RTL language additionally requires `ldrtl`). Votable design text with
// no decisive winner fails closed (CAPTURE_LOCALE_UNDERIVABLE); a textless spec carries
// no locale signal, so only geometry is enforced and the segment is left untouched.
//
// `--fix` repairs the deterministic parts: it surgically rewrites the `w<W>dp-h<H>dp` portion
// — and, when the design language is resolved and mismatched, the locale segment — of the
// effective qualifier (injecting a method-level `@Config` when the test has none), preserving
// density/unrelated segments and adding the `Config` import when absent. It NEVER touches stub
// data (the only remaining non-derivable part — the builder / check-stub-text own it). The
// orchestrator run loop uses `--gate` and routes blockers to the owner builder; a builder may
// run `--fix` as a targeted repair. Never calls Figma. Advisory→BLOCKER by kind above; exit
// non-zero only in --gate.
//
// Usage: node scripts/check-capture-config.mjs <TASK_STEM> [--gate] [--fix] [--code-root D] [--out <file>]
//   FIGMA_SCREEN_CACHE_ROOT or FIGMA_SPEC_SCREENS_DIR — override screens cache root (must match if both are set)
//   FIGMA_CENSUS_CODE_ROOTS  — path-delimited effective roots (default: the product repo root).
//     Narrowing is useful for fixtures/diagnostics, but final evidence separately compares this
//     witness with a canonical EXECUTION_ROOT discovery and blocks omitted capture/resource scope.
//   FIGMA_DESIGN_LOCALE / FIGMA_SUPPORTED_LOCALES / FIGMA_STRING_RESOURCE_ROOTS — per-run
//     overrides of the design-locale inputs (fixtures/diagnostics only; see lib/design-locale.mjs)
import { readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { delimiter, join } from 'node:path'
import { displayPath, exists, readJson, figmaPath, figmaScreensRoot, isDirectRun, loadBindings, ok, warnMsg, failMsg, info, summary, parseCli, EXECUTION_ROOT, executionProductInputPath, PROJECT_CONFIG_FILE, PROJECT_CONFIG_HASH } from './_util.mjs'
import { assertTaskStem, fileHash, writeReport } from './report-utils.mjs'
import { deriveResourceRoots, languageOf, readSupportedLocales, resolveDesignLocale } from './lib/design-locale.mjs'
import { CAPTURE_CONFIG_DISCOVERY_KEY, captureConfigDiscovery } from './lib/capture-config-discovery.mjs'

const USAGE = 'usage: node scripts/check-capture-config.mjs <TASK_STEM> [--gate] [--fix] [--code-root D] [--out F]'

// Blank line/block comments to spaces (newline-preserving) but KEEP string literals — this
// gate needs the captureRoboImage("...") path content, unlike the full trivia masker.
function maskComments(text) {
  const s = String(text || '')
  const n = s.length
  const out = s.split('')
  let i = 0, inStr = 0 // 0=none 1=" 2="""
  while (i < n) {
    const c = s[i], c2 = s[i + 1]
    if (inStr) {
      if (inStr === 1 && c === '\\') { i += 2; continue }
      if (inStr === 1 && c === '"') inStr = 0
      else if (inStr === 2 && c === '"' && c2 === '"' && s[i + 2] === '"') { inStr = 0; i += 3; continue }
      i++; continue
    }
    if (c === '"' && c2 === '"' && s[i + 2] === '"') { inStr = 2; i += 3; continue }
    if (c === '"') { inStr = 1; i++; continue }
    // char literal 'x' / '\n' / '\'' — skip WITHOUT blanking so an embedded double-quote
    // (`'"'`) does not spuriously start a string and blank the real code that follows.
    if (c === "'") { i++; if (s[i] === '\\') i += 2; else i += 1; if (s[i] === "'") i++; continue }
    if (c === '/' && c2 === '/') { while (i < n && s[i] !== '\n') { out[i] = ' '; i++ } continue }
    if (c === '/' && c2 === '*') { out[i] = out[i + 1] = ' '; i += 2; while (i < n && !(s[i] === '*' && s[i + 1] === '/')) { if (s[i] !== '\n') out[i] = ' '; i++ } if (i < n) { out[i] = out[i + 1] = ' '; i += 2 } continue }
    i++
  }
  return out.join('')
}

const QUALIFIERS_RE = /qualifiers\s*=\s*"([^"]*)"/

// Find the @Config nearest to (below) endIdx within [startIdx, endIdx), tolerating a
// MULTI-LINE annotation (its args span until parens balance). Returns
// { configLineIdx, endLineIdx, qualifiers, qualLineIdx } or null. qualLineIdx is the line
// carrying `qualifiers = "..."` (for a surgical patch that works on single- or multi-line).
function findConfigAbove(lines, endIdx, startIdx) {
  for (let i = endIdx - 1; i >= startIdx; i--) {
    if (!/@Config\b/.test(lines[i])) continue
    let depth = 0, started = false, endLine = i, qualLineIdx = -1, args = ''
    for (let j = i; j < lines.length; j++) {
      for (const ch of lines[j]) { if (ch === '(') { depth++; started = true } else if (ch === ')') depth-- }
      args += lines[j] + '\n'
      if (qualLineIdx < 0 && QUALIFIERS_RE.test(lines[j])) qualLineIdx = j
      endLine = j
      if (started && depth <= 0) break
    }
    return { configLineIdx: i, endLineIdx: endLine, qualifiers: (QUALIFIERS_RE.exec(args) || [])[1] ?? null, qualLineIdx }
  }
  return null
}

// The line after the nearest enclosing `{` / preceding `}` above funIdx — the top of this
// function's own annotation region, so a method-@Config search never bleeds into a sibling.
function annotationRegionStart(lines, funIdx) {
  for (let i = funIdx - 1; i >= 0; i--) {
    const t = lines[i].trim()
    if (t.endsWith('}') || t.endsWith('{')) return i + 1
  }
  return 0
}

function qualifiersWH(q) {
  let w = null, h = null
  for (const seg of String(q || '').split('-').filter(Boolean)) {
    let m
    if ((m = /^w(\d+)dp$/.exec(seg))) w = Number(m[1])
    else if ((m = /^h(\d+)dp$/.exec(seg))) h = Number(m[1])
  }
  return { w, h }
}

// R5 — leading-locale split of a qualifier segment list, per the Android config-qualifier
// grammar: optional mcc/mnc segments come FIRST, then the locale — a plain language
// (`uk`, optionally + `rUA` region) or the BCP form (`b+uk+UA`), then everything else.
// `car` (UI-mode) is the one 2-3-letter lowercase qualifier keyword that could sit in the
// locale position without being a locale — excluded defensively. Returns
// { mcc, language, region, rest } (language/region null when no locale segment).
function splitLeadingLocale(segs) {
  let i = 0
  while (i < segs.length && /^m[cn]c\d+$/.test(segs[i])) i++
  let language = null, region = null, j = i
  if (i < segs.length) {
    if (/^b\+/i.test(segs[i])) { language = languageOf(segs[i]); j = i + 1 }
    else if (/^[a-z]{2,3}$/.test(segs[i]) && segs[i] !== 'car') {
      language = segs[i]
      j = i + 1
      if (j < segs.length && /^r[A-Z]{2}$/.test(segs[j])) { region = segs[j]; j++ }
    }
  }
  return { mcc: segs.slice(0, i), language, region, rest: segs.slice(j) }
}

// Locale facts of a qualifier string: the leading language (absent = the Robolectric default
// en-rUS) + whether ldrtl is present anywhere.
function qualifiersLocale(q) {
  const segs = String(q || '').split('-').filter(Boolean)
  const { language } = splitLeadingLocale(segs)
  return { language, ldrtl: segs.includes('ldrtl') }
}

// Rewrite the size portion only, preserving locale/ld*/density segments and their order.
// With a localePlan ({language, rtl}) the LOCALE portion is owned too (R5): the leading
// locale (plain or b+ form) is replaced by the plan's language (an existing region survives
// only when the language already matched), ld* segments are re-derived (ldrtl iff RTL), and
// the locale lands right after any mcc/mnc segments — Android qualifier grammar.
function rewriteQualifiers(q, W, H, localePlan = null) {
  let segs = String(q || '').split('-').filter(Boolean)
  const localeSegs = []
  if (localePlan) {
    const lead = splitLeadingLocale(segs)
    segs = lead.rest.filter((s) => s !== 'ldrtl' && s !== 'ldltr')
    localeSegs.push(...lead.mcc, localePlan.language)
    if (lead.region && lead.language === localePlan.language) localeSegs.push(lead.region)
    if (localePlan.rtl) localeSegs.push('ldrtl')
  }
  const pre = [], density = []
  for (const seg of segs) {
    if (/^[wh]\d+dp$/.test(seg)) continue                                   // replace the size
    if (/dpi$/.test(seg)) density.push(seg)                                 // density goes AFTER size
    else pre.push(seg)                                                      // locale / ldrtl / smallest-width etc keep order, BEFORE size
  }
  return [...localeSegs, ...pre, `w${W}dp`, `h${H}dp`, ...density].join('-')
}

// Parse capture-config facts. ANCHORS ON captureRoboImage (masked keeps string contents +
// line numbers, so a commented-out capture is already blanked and ignored) rather than on a
// fragile `@Test` annotation cluster — a multi-line annotation above the `fun` (e.g. a
// wrapped @Config) must remain visible to the parser. For each
// capture we find the enclosing `fun`, then its effective @Config (method-level over
// class-level, each multi-line-tolerant). Returns
// [{ name, funLineIdx, indent, capture:{screen,theme}, methodConfig, classConfig }].
function parseTests(masked, raw) {
  const lines = masked.split('\n')
  const rawLines = raw.split('\n')
  let classDeclIdx = lines.findIndex((l) => /\b(?:class|object)\s+[A-Za-z_]/.test(l))
  if (classDeclIdx < 0) classDeclIdx = lines.length
  const classConfig = findConfigAbove(lines, classDeclIdx, 0)

  const tests = []
  const unowned = []
  // Grab the quoted path GREEDILY (linear — a lazy `[^"]*?` before `([A-Za-z0-9_]+)Screenshot`
  // backtracks quadratically on a long unterminated string), then parse the screen off the path.
  const capRe = /captureRoboImage\s*\(\s*"([^"]*)"/g
  const nameRe = /([A-Za-z0-9_]+)Screenshot(\.dark)?\.png$/
  for (let i = 0; i < lines.length; i++) {
    capRe.lastIndex = 0
    for (let m; (m = capRe.exec(lines[i])) != null;) {
      // Identity-first: keep the capture BASENAME even when the `<Screen>Screenshot.png`
      // name shape does not match — a nodeId-bound manifest entry (W2-2 join in main) can
      // still map it to a spec, so an off-convention name retains @Config verification.
      const basename = m[1].split('/').pop().split('\\').pop()
      const pm = nameRe.exec(m[1])
      if (!pm && !basename) continue
      const screen = pm ? pm[1] : null, theme = pm ? (pm[2] ? 'dark' : 'primary') : null
      // Enclosing TEST method: the nearest `fun` above whose annotation region carries @Test.
      // A NAMED local `fun helper()` inside the test body has no @Test in its region, so it is
      // skipped (else --fix would inject a @Config above the local fun and leave the real
      // drift unhealed). A capture with no enclosing @Test is invalid evidence.
      let funLineIdx = -1, name = null
      for (let j = i; j >= 0; j--) {
        const fm = /(?:^|\s)fun\s+([A-Za-z_]\w*)\s*\(/.exec(lines[j])
        if (!fm) continue
        if (lines.slice(annotationRegionStart(lines, j), j).some((l) => /@Test\b/.test(l))) { funLineIdx = j; name = fm[1]; break }
      }
      if (funLineIdx < 0) { unowned.push({ basename, line: i + 1 }); continue }
      const methodConfig = findConfigAbove(lines, funLineIdx, annotationRegionStart(lines, funLineIdx))
      const indent = (rawLines[funLineIdx].match(/^(\s*)/) || ['', ''])[1]
      tests.push({ name, funLineIdx, indent, capture: { screen, theme, basename }, methodConfig, classConfig })
    }
  }
  return { tests, unowned, classConfig }
}

;(function main() {
  if (!isDirectRun(import.meta.url)) return

  let cli
  try {
    cli = parseCli({ allowedFlags: ['--gate', '--fix', '--code-root', '--out'], valueFlags: ['--code-root', '--out'], booleanFlags: ['--gate', '--fix'], usage: USAGE })
  } catch (e) {
    console.error(e.message)
    process.exit(1)
  }
  let stem
  try { stem = assertTaskStem(cli.positional[0] || '') } catch { failMsg(USAGE); process.exit(1) }
  if (cli.positional.length !== 1) { failMsg(USAGE); process.exit(1) }
  const gate = cli.has('--gate')
  const doFix = cli.has('--fix')

  const screensRoot = figmaScreensRoot()
  const screensDir = join(screensRoot, stem)
  if (!exists(screensDir)) { failMsg(`screens cache missing: ${screensDir} — pull it first (figma:screens session)`); process.exit(1) }
  const codeRootArg = cli.value('--code-root')
  const codeRoots = codeRootArg
    ? [codeRootArg]
    : (process.env.FIGMA_CENSUS_CODE_ROOTS
      ? process.env.FIGMA_CENSUS_CODE_ROOTS.split(delimiter).filter(Boolean)
        .map((value) => executionProductInputPath(value, 'FIGMA_CENSUS_CODE_ROOTS'))
      : [EXECUTION_ROOT])

  // 1. Desired geometry per (screen, theme) from the specs' frameSizeDp.
  const specFiles = readdirSync(screensDir).filter((f) => f.endsWith('.spec.json')).sort()
  if (!specFiles.length) { failMsg(`no *.spec.json under ${screensDir}`); process.exit(1) }
  const want = new Map()   // `${screen}|${theme}` -> { screen, theme, w, h, specFile }
  const inputHashes = {}
  const specsForLocale = []   // R5 — full parsed specs; their elements[].text votes for the design language
  for (const f of specFiles) {
    const p = join(screensDir, f)
    let spec
    try { spec = readJson(p) } catch (e) { failMsg(`${f} unreadable (${e.message})`); process.exit(1) }
    inputHashes[p] = fileHash(p)
    specsForLocale.push(spec)
    const fs = spec && spec.frameSizeDp
    if (!fs || !(fs.w > 0) || !(fs.h > 0)) continue
    const theme = /\.dark\.spec\.json$/.test(f) ? 'dark' : 'primary'
    const screen = f.replace(/\.dark\.spec\.json$/, '').replace(/\.spec\.json$/, '')
    want.set(`${screen}|${theme}`, { screen, theme, w: Math.round(fs.w), h: Math.round(fs.h), specFile: f })
  }

  // 1b. W2-2 identity join — when the validator/driver supplied a capture manifest, bind a
  // test's capture to its spec by manifest basename → nodeId → current index variant. A capture
  // legitimately named off-convention (`<Screen>DefaultScreenshot.png`) but nodeId-bound in
  // the manifest keeps its @Config verified here instead of silently skipping (fail-open) and
  // WARNing CAPTURE_TEST_ABSENT on the spec side every run.
  const manifestPath = process.env.SCREENSHOT_CAPTURE_MANIFEST || ''
  const manifestKeyByBasename = new Map()   // capture basename -> { screen, theme }
  let indexNodes = null
  const indexPath = join(screensDir, 'index.json')
  if (exists(indexPath)) inputHashes[indexPath] = fileHash(indexPath)
  if (exists(indexPath)) {
    let idx
    try { idx = readJson(indexPath) }
    catch (error) { failMsg(`index.json unreadable (${error.message})`); process.exit(1) }
    if (!idx || idx.schemaVersion !== 3 || idx.taskStem !== stem || !idx.nodes || typeof idx.nodes !== 'object' || Array.isArray(idx.nodes)) {
      failMsg('index.json must satisfy the current schemaVersion 3 contract and match the task stem')
      process.exit(1)
    }
    indexNodes = idx.nodes
  }
  if (manifestPath && !exists(manifestPath)) {
    failMsg(`explicit capture manifest missing: ${manifestPath}`)
    process.exit(1)
  }
  if (manifestPath) {
    inputHashes[manifestPath] = fileHash(manifestPath)
    let mf
    try {
      mf = readJson(manifestPath)
    } catch (error) { failMsg(`capture manifest unreadable (${error.message})`); process.exit(1) }
    if (!mf || typeof mf !== 'object' || Array.isArray(mf) || !Array.isArray(mf.captures)) {
      failMsg('capture manifest must use the current object-with-captures contract')
      process.exit(1)
    }
    if (indexNodes) for (const entry of mf.captures) {
      const nodeId = entry && entry.nodeId != null ? String(entry.nodeId) : ''
      const base = String((entry && entry.captureName) || '')
      if (!nodeId || !base) continue
      for (const [screen, node] of Object.entries(indexNodes)) {
        const variants = node && Array.isArray(node.variants) ? node.variants : []
        const variant = variants.find((candidate) => String(candidate.nodeId || '') === nodeId && (!entry.variantId || entry.variantId === candidate.id))
        if (!variant) continue
        manifestKeyByBasename.set(base, { screen, theme: String(variant.theme).toLowerCase() === 'dark' ? 'dark' : 'primary' })
        break
      }
    }
  }
  // The canonical driver creates its capture manifest only after recording, but bindings.json
  // already carries the same basename→node identity before the render. Consume it directly so
  // an off-convention bound test cannot skip the static @Config/locale gate on the first run.
  const bindingsPath = join(screensDir, 'bindings.json')
  const bindings = loadBindings(stem)
  if (bindings && exists(bindingsPath)) inputHashes[bindingsPath] = fileHash(bindingsPath)
  if (bindings && indexNodes) {
    for (const bound of bindings.screens || []) {
      if (!bound.captureBasename || !bound.nodeId || !bound.screenName) continue
      const node = indexNodes[bound.screenName]
      if (!node || typeof node !== 'object') continue
      const nodeId = String(bound.nodeId)
      const variant = (Array.isArray(node.variants) ? node.variants : []).find((candidate) => String(candidate.nodeId || '') === nodeId)
      const theme = variant ? (String(variant.theme).toLowerCase() === 'dark' ? 'dark' : 'primary') : null
      if (theme) manifestKeyByBasename.set(String(bound.captureBasename), { screen: bound.screenName, theme })
    }
  }

  // 2. Index every @Test capture across the product's test sources. The virtual discovery
  // digest also pins files/resources that do not exist yet, closing add-after-report freshness.
  const issues = []
  const fixed = []
  let supportedLocales = []
  let localeConfigUnavailable = false
  try {
    supportedLocales = readSupportedLocales()
  } catch {
    localeConfigUnavailable = true
    const message = 'supported locale configuration is missing, invalid, unreadable, unsafe, or not valid UTF-8'
    issues.push({ severity: 'BLOCKER', issueKind: 'CAPTURE_LOCALE_SOURCE_UNAVAILABLE', message })
    warnMsg(`CAPTURE_LOCALE_SOURCE_UNAVAILABLE  ${message}`)
  }
  const discovery = captureConfigDiscovery({ codeRoots, screensDir, supportedLocales })
  const files = discovery.files
  inputHashes[CAPTURE_CONFIG_DISCOVERY_KEY] = discovery.digest
  const fileEdits = new Map()   // absPath -> { raw, edits:[{lineIdx, kind, ...}] }
  const seen = new Set()        // `${screen}|${theme}` that a @Test captures
  for (const error of discovery.errors) {
    const file = displayPath(error.path)
    const message = `capture-config discovery could not read ${file} (${error.code}) — a partial code-root scan cannot verify every capture test/resource`
    issues.push({ severity: 'BLOCKER', issueKind: 'CAPTURE_DISCOVERY_UNREADABLE', message, file })
    warnMsg(`CAPTURE_DISCOVERY_UNREADABLE  ${file} (${error.code})`)
  }

  // 1c (R5) — resolve the design LANGUAGE; it is part of the derived qualifier. Deterministic
  // sources only: the committed `designLocale` config key (FIGMA_DESIGN_LOCALE = fixture
  // override) short-circuits; otherwise the spec texts vote against each supported locale's
  // string resources (lib/design-locale.mjs). Fail-closed doctrine: votable design text with
  // no decisive winner — or a declared locale outside supportedLocales — blocks with a NAMED
  // remedy, never guesses. A textless spec (zero votable candidates) carries no locale
  // signal: geometry stays enforced, the locale segment is left untouched (pre-R5 behavior).
  const locale = localeConfigUnavailable
    ? { language: null, reason: 'source-unavailable' }
    : resolveDesignLocale({ specs: specsForLocale, resourceRoots: deriveResourceRoots(codeRoots), supportedLocales })
  const localePlan = locale.language ? { language: locale.language, rtl: locale.rtl } : null
  const localeEnvOverrides = ['FIGMA_DESIGN_LOCALE', 'FIGMA_SUPPORTED_LOCALES', 'FIGMA_STRING_RESOURCE_ROOTS'].filter((k) => process.env[k])
  if (exists(PROJECT_CONFIG_FILE)) {
    inputHashes[PROJECT_CONFIG_FILE] = PROJECT_CONFIG_HASH || fileHash(PROJECT_CONFIG_FILE)
  }
  for (const f of locale.resourceFiles || []) {
    if (exists(f)) inputHashes[f] = fileHash(f)
  }
  if (!localeConfigUnavailable && !localePlan && (locale.reason === 'invalid-config' || locale.reason === 'not-confident' || locale.reason === 'source-unavailable')) {
    const msg = locale.reason === 'invalid-config'
      ? `designLocale '${locale.locale}' is not one of supportedLocales [${supportedLocales.join(', ')}] — fix orchestrator/project-config.md (the declared design language must be a supported locale)`
      : locale.reason === 'source-unavailable'
        ? 'one or more locale resource files are unreadable, unsafe, or not valid UTF-8 — repair the resource input before deriving the capture locale'
      : `design language underivable: ${locale.candidates} votable spec string(s) match no single supported locale decisively (scores: ${JSON.stringify(locale.scores)}) — add \`designLocale: <locale>\` to orchestrator/project-config.md, or enrich the design's text layers so the pulled spec carries the app's real copy`
    const kind = locale.reason === 'source-unavailable' ? 'CAPTURE_LOCALE_SOURCE_UNAVAILABLE' : 'CAPTURE_LOCALE_UNDERIVABLE'
    issues.push({ severity: 'BLOCKER', issueKind: kind, message: msg })
    warnMsg(`${kind}  ${msg}`)
  } else if (localePlan) {
    info(`design locale: ${localePlan.language}${localePlan.rtl ? ' (RTL)' : ''} [${locale.source}]`)
  }

  for (const file of files) {
    let raw
    try { raw = readFileSync(file, 'utf8') } catch { continue }
    if (!raw.includes('captureRoboImage')) continue
    inputHashes[file] = fileHash(file)
    const masked = maskComments(raw)
    const { tests, unowned } = parseTests(masked, raw)
    for (const capture of unowned) {
      const message = `${displayPath(file)}:${capture.line} captures ${capture.basename} outside an @Test method — capture evidence must have one exact test/config owner`
      issues.push({ severity: 'BLOCKER', issueKind: 'CAPTURE_NOT_IN_TEST', message, file: displayPath(file) })
      warnMsg(`CAPTURE_NOT_IN_TEST  ${message}`)
    }
    // Group captures by their enclosing @Test fun: ALL captures in one fun share ONE effective
    // @Config, so the fun gets at most ONE edit (stacking two @Config lines would be a
    // non-repeatable-annotation compile error). Two captures in one fun that need DIFFERENT
    // sizes is a genuine mixed-geometry error to REPORT, not to silently duplicate-annotate.
    const byFun = new Map()
    for (const test of tests) { const g = byFun.get(test.funLineIdx) || []; g.push(test); byFun.set(test.funLineIdx, g) }
    let fileMatchedSpec = false
    const fileUnmatched = []
    for (const group of byFun.values()) {
      const rep = group[0]
      const wanted = []
      for (const test of group) {
        // Identity join first (manifest basename → nodeId → index screen), filename shape second.
        const mapped = manifestKeyByBasename.get(test.capture.basename) || null
        const screen = mapped ? mapped.screen : test.capture.screen
        const theme = mapped ? mapped.theme : test.capture.theme
        if (!screen) { fileUnmatched.push({ basename: test.capture.basename, test: test.name }); continue }
        const key = `${screen}|${theme}`
        seen.add(key)
        const w = want.get(key)
        if (w) { wanted.push({ w, cap: { screen, theme } }); fileMatchedSpec = true }
        else fileUnmatched.push({ basename: test.capture.basename, test: test.name })
      }
      if (!wanted.length) continue   // no capture in this fun matches a spec bullet
      const sizes = new Set(wanted.map((x) => `${x.w.w}x${x.w.h}`))
      if (sizes.size > 1) {
        const desc = wanted.map((x) => `${x.cap.screen} ${x.w.w}×${x.w.h}`).join(', ')
        issues.push({ severity: 'BLOCKER', issueKind: 'CAPTURE_CONFIG_CONFLICT', message: `test '${rep.name}' captures screens of different frameSizeDp (${desc}) under ONE @Config — split into separate @Tests, one geometry each (fidelity-gate §3)`, test: rep.name, file: displayPath(file) })
        warnMsg(`CAPTURE_CONFIG_CONFLICT  '${rep.name}' — ${desc}`)
        continue
      }
      const w = wanted[0].w
      const effective = rep.methodConfig || rep.classConfig
      const { w: qw, h: qh } = qualifiersWH(effective && effective.qualifiers)
      const level = rep.methodConfig ? 'method' : (rep.classConfig ? 'class' : 'none')
      const sizeOk = qw === w.w && qh === w.h
      // R5 — the locale segment is derived data too: enforced only under a resolved localePlan
      // (the effective language of an ABSENT segment is the Robolectric default 'en'). Layout
      // direction is owned in BOTH directions: an RTL design language requires ldrtl, and a
      // stray ldrtl on an LTR design language is a mismatch too (a mirrored render diverges
      // structurally with no named cause otherwise).
      const effLoc = qualifiersLocale(effective && effective.qualifiers)
      const localeOk = !localePlan || ((effLoc.language || 'en') === localePlan.language && effLoc.ldrtl === localePlan.rtl)
      if (sizeOk && localeOk) continue   // in sync
      const expected = `${localeOk ? '' : `${localePlan.language}${localePlan.rtl ? '-ldrtl' : ''}-`}w${w.w}dp-h${w.h}dp`
      const drift = (qw != null || qh != null)
      const kind = drift ? 'CAPTURE_CONFIG_DRIFT' : 'CAPTURE_CONFIG_MISSING'
      const localeSource = locale.source === 'config' ? 'declared via designLocale' : 'derived from spec texts × string resources'
      const localeNote = localeOk ? '' : (
        (effLoc.language || 'en') !== localePlan.language
          ? ` (capture locale ${effLoc.language || 'en, the Robolectric default'} ≠ design language ${localePlan.language}${localePlan.rtl ? ' [RTL — ldrtl required]' : ''}, ${localeSource}; fidelity-gate §2/§3: the capture must speak the design's language)`
          : localePlan.rtl
            ? ` (design language ${localePlan.language} is RTL, ${localeSource} — the qualifier must carry ldrtl; fidelity-gate §3)`
            : ` (stray ldrtl: the design language ${localePlan.language} is LTR, ${localeSource} — a mirrored render diverges structurally; fidelity-gate §3)`
      )
      const msg = drift
        ? sizeOk
          ? `${w.screen} [${w.theme}] test '${rep.name}' @Config qualifiers ${JSON.stringify((effective && effective.qualifiers) || '')} has the right geometry but the wrong locale segment${localeNote} — set ${expected}`
          : `${w.screen} [${w.theme}] test '${rep.name}' @Config qualifiers ${JSON.stringify((effective && effective.qualifiers) || '')} → w${qw}dp-h${qh}dp, but the oracle frameSizeDp is ${w.w}×${w.h} — set ${expected} (drift renders resampling noise / ASPECT_MISMATCH)${localeNote}`
        : `${w.screen} [${w.theme}] test '${rep.name}' has ${level === 'none' ? 'no @Config' : `a @Config with no w/h qualifier (${level}-level)`} — renders at the ~320×470 host default; add ${expected} (fidelity-gate §3)${localeNote}`
      if (doFix) {
        const target = rewriteQualifiers(effective && effective.qualifiers, w.w, w.h, localeOk ? null : localePlan)
        const ent = fileEdits.get(file) || { raw, edits: [] }
        // Prefer editing an existing METHOD-level @Config; else inject ONE method-level @Config
        // above the fun (NEVER edit class-level — it is shared across sibling tests). The patch
        // targets qualLineIdx (the `qualifiers = "…"` line) so a multi-line @Config is handled;
        // when the method @Config has no qualifiers arg, inject into its `@Config(`.
        if (rep.methodConfig) ent.edits.push({ kind: 'set-qualifiers', funLineIdx: rep.funLineIdx, configLineIdx: rep.methodConfig.configLineIdx, qualLineIdx: rep.methodConfig.qualLineIdx, target })
        else ent.edits.push({ kind: 'inject-config', funLineIdx: rep.funLineIdx, indent: rep.indent, target })
        fileEdits.set(file, ent)
        fixed.push({ screen: w.screen, theme: w.theme, test: rep.name, expected, file: displayPath(file) })
      } else {
        issues.push({ severity: 'BLOCKER', issueKind: kind, message: msg, screen: w.screen, theme: w.theme, test: rep.name, expected, file: displayPath(file) })
        warnMsg(`${kind}  ${w.screen} [${w.theme}] '${rep.name}' — want ${expected}`)
      }
    }
    // A capture that matched NO spec, inside a file that DOES capture this task's specs, is a
    // likely naming slip for THIS task — surface it instead of silently skipping. Console
    // note only, not a report issue: unrelated tasks' tests legitimately live in the same
    // scanned tree and must not spam the report.
    if (fileMatchedSpec && fileUnmatched.length) {
      const dedup = [...new Map(fileUnmatched.map((u) => [`${u.test}|${u.basename}`, u])).values()]
      for (const u of dedup) info(`TEST_UNMATCHED  '${u.test}' captures ${u.basename} — matches no spec of this task (manifest + filename both missed); if it belongs to this task, fix the capture name or bind it in the manifest`)
    }
  }

  // 3. Apply fixes. set-qualifiers is IN-PLACE (no line-count change); inject-config inserts a
  // line. Applying bottom-up (by funLineIdx desc) keeps every not-yet-applied index valid.
  for (const [file, ent] of fileEdits) {
    let lines = ent.raw.split('\n')
    // Ensure the Config import exists (needed when we inject the first method-level @Config).
    if (ent.edits.some((e) => e.kind === 'inject-config') && !/import\s+org\.robolectric\.annotation\.Config\b/.test(ent.raw)) {
      let lastImport = -1
      for (let i = 0; i < lines.length; i++) if (/^\s*import\s+/.test(lines[i])) lastImport = i
      // Anchor after the last import; with NO imports, after the `package` line (or at the very top).
      // Skipping the insert on a no-import source emitted `@Config(...)` with an unresolved `Config`
      // reference — non-compilable Kotlin the re-gate still PASSED (it only compares qualifier text).
      let insertAt
      if (lastImport >= 0) insertAt = lastImport + 1
      else { const pkg = lines.findIndex((l) => /^\s*package\s+/.test(l)); insertAt = pkg >= 0 ? pkg + 1 : 0 }
      lines.splice(insertAt, 0, 'import org.robolectric.annotation.Config')
      for (const e of ent.edits) for (const k of ['funLineIdx', 'configLineIdx', 'qualLineIdx']) if (e[k] != null && e[k] >= insertAt) e[k]++
    }
    const ops = ent.edits.slice().sort((a, b) => b.funLineIdx - a.funLineIdx)
    for (const e of ops) {
      if (e.kind === 'set-qualifiers') {
        if (e.qualLineIdx != null && e.qualLineIdx >= 0 && QUALIFIERS_RE.test(lines[e.qualLineIdx])) {
          lines[e.qualLineIdx] = lines[e.qualLineIdx].replace(QUALIFIERS_RE, `qualifiers = "${e.target}"`)   // single- OR multi-line @Config
        } else {
          // method @Config exists but carries no qualifiers arg — inject one right after `@Config(`.
          const li = e.configLineIdx
          lines[li] = lines[li].replace(/@Config\s*\(/, `@Config(qualifiers = "${e.target}", `)
        }
      } else {
        lines.splice(e.funLineIdx, 0, `${e.indent}@Config(qualifiers = "${e.target}")`)
      }
    }
    writeFileSync(file, lines.join('\n'))
    ok(`patched ${displayPath(file)}`)
  }

  // 4. A spec bullet no @Test captures → WARN (the comparator's MISSING_CAPTURE owns the block).
  for (const [key, w] of want) {
    if (seen.has(key)) continue
    issues.push({ severity: 'WARN', issueKind: 'CAPTURE_TEST_ABSENT', message: `${w.screen} [${w.theme}] (frameSizeDp ${w.w}×${w.h}) has no @Test calling captureRoboImage(".../${w.screen}Screenshot${w.theme === 'dark' ? '.dark' : ''}.png") — cannot verify its @Config; the screenshot gate will block it as MISSING_CAPTURE`, screen: w.screen, theme: w.theme })
    warnMsg(`CAPTURE_TEST_ABSENT  ${w.screen} [${w.theme}] — no capturing @Test found`)
  }

  const hasBlocker = issues.some((i) => i.severity === 'BLOCKER')
  const overall = hasBlocker ? 'BLOCKER' : issues.length ? 'WARN' : 'PASS'
  const OUT = cli.value('--out') || ''
  const { reportPath } = writeReport({
    name: 'capture-config',
    taskStem: stem,
    mode: gate ? 'gate' : 'advisory',
    inputs: {
      screensDir: displayPath(screensDir),
      codeRoots: codeRoots.map((r) => displayPath(r)),
      specs: want.size,
      testFilesScanned: files.length,
      fixed: fixed.length,
      captureDiscovery: { version: discovery.version, roots: discovery.roots, screensDir: discovery.screensDir, digest: discovery.digest },
    },
    inputHashes,
    overall,
    issues,
    extra: { version: 1, fixed, designLocale: localePlan ? { language: localePlan.language, rtl: localePlan.rtl, source: locale.source } : { language: null, reason: locale.reason || 'no-signal' }, designLocaleEnvOverrides: localeEnvOverrides },
    outPath: OUT,
  })
  info(`report: ${reportPath}`)
  info(`specs with frameSizeDp: ${want.size}, drift/missing: ${issues.filter((i) => i.severity === 'BLOCKER').length}, auto-fixed: ${fixed.length}, absent tests: ${issues.filter((i) => i.issueKind === 'CAPTURE_TEST_ABSENT').length}`)
  if (fixed.length) for (const f of fixed) ok(`fixed ${f.screen} [${f.theme}] '${f.test}' → ${f.expected}`)
  summary('figma:check-capture-config')
  // In --gate mode an unresolved BLOCKER fails the process; --fix resolves size drift in place,
  // so a --fix run that healed everything exits 0.
  process.exit(gate && hasBlocker ? 2 : 0)
})()
