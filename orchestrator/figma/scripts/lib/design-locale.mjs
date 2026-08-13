// design-locale.mjs — derive the DESIGN language from committed data: spec texts × string resources.
//
// Why this exists: the capture must speak the oracle's language (fidelity-gate §2/§3 — the
// @Config locale segment), but nothing DERIVED that language: a wrapper hardcoding `en` against
// a Ukrainian oracle blocked pixel-perfect screens as unexplained low-SSIM text zones. The
// language IS derivable from committed data alone: the pulled specs carry the design's verbatim
// texts (`elements[].text`) and the app carries per-locale string resources
// (`values*/strings.xml` under composeResources / src/main/res). Each candidate spec string
// votes for every supported locale whose resources contain it; the winner must be decisive.
//
// Doctrine (REVIEW_ROUTING R5): deterministic derivation from COMMITTED data only — no ML, no
// guessing. This lib never guesses: an undecided vote returns `confident: false` and the CALLERS
// fail closed with a named remedy (declare `designLocale` in project-config). The explicit
// declaration always wins over detection.
//
// Pure library (canvas-detect pattern): no CLI, no writes. It reads only the string-resource
// files the caller points it at (plus project-config for the two config readers).

import { existsSync, readFileSync, readdirSync, realpathSync, statSync } from 'node:fs'
import { delimiter, join, resolve, sep } from 'node:path'
import { TextDecoder } from 'node:util'
import { PROJECT_ROOT, readConfig, EXECUTION_ROOT, EXECUTION_SCOPE, executionProductInputPath } from '../_util.mjs'

// Languages whose derived qualifier must also carry `ldrtl` (fidelity-gate §3).
export const RTL_LANGUAGES = new Set(['ar', 'he', 'fa', 'ur'])
const utf8 = new TextDecoder('utf-8', { fatal: true })
const readUtf8 = (file) => utf8.decode(readFileSync(file))

// Primary language subtag of a locale tag in any of the shapes this pipeline meets:
// 'uk', 'uk-UA', 'uk_UA', Android qualifier 'uk-rUA', BCP-in-qualifier 'b+uk+UA'.
export function languageOf(tag) {
  const t = String(tag ?? '').trim()
  if (!t) return null
  if (/^b\+/i.test(t)) return (t.split('+')[1] || '').toLowerCase() || null
  const first = t.replace(/_/g, '-').split('-')[0].toLowerCase()
  return first || null
}

// Shared normalization for BOTH sides of the vote (spec text and resource value): collapse
// whitespace, strip a trailing ellipsis (a truncated design label still matches its resource),
// lowercase (a design `case: upper` transform must not defeat the match).
export function normalizeText(s) {
  return String(s ?? '')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/(?:\.{3}|…)$/u, '')
    .trim()
    .toLowerCase()
}

// A string is votable only when it plausibly comes from a string resource: digits/punctuation-only
// content and micro-labels (< 3 chars) are dynamic data — they do not vote.
const votable = (t) => t.length >= 3 && /\p{L}/u.test(t)

// <string name="…">value</string> values out of an Android/Compose strings.xml. Regex-based on
// purpose — the sidecar carries no XML dependency and resource files are machine-written.
// Comments are stripped FIRST (a commented-out translation must not cast a vote), and
// the open-tag pattern rejects the self-closing `<string name="…"/>` form (with `[^>]*` alone
// its trailing `/` was consumed and the lazy capture swallowed the NEXT string element).
export function parseStringsXml(xml) {
  const out = []
  const re = /<string\b(?:[^>]*[^/>])?>([\s\S]*?)<\/string>/g
  const src = String(xml ?? '').replace(/<!--[\s\S]*?-->/g, '')
  for (let m; (m = re.exec(src)) != null;) {
    let v = m[1]
    const cdata = /^\s*<!\[CDATA\[([\s\S]*?)\]\]>\s*$/.exec(v)
    if (cdata) v = cdata[1]
    v = v
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&quot;/g, '"')
      .replace(/&apos;/g, "'")
      .replace(/&#(\d+);/g, (_, d) => String.fromCodePoint(Number(d)))
      .replace(/&amp;/g, '&')
      .replace(/\\n|\\t/g, ' ')
      .replace(/\\'/g, "'")
      .replace(/\\"/g, '"')
    v = v.replace(/^"([\s\S]*)"$/, '$1') // Android quoted-string form (preserved literal spaces)
    out.push(v)
  }
  return out
}

// Android position formatters (%1$s, %d, %.1f, %%) become wildcards so a rendered
// "Оновлено 15 хв тому" still matches the resource "Оновлено %1$s тому".
const FORMATTER_RE = /%(?:\d+\$)?(?:\.\d+)?[a-z]|%%/g
const escapeRe = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

// Compile one resource value into a matcher: exact normalized string, or an anchored
// wildcard regex when it carries formatters. Returns null for a value that normalizes away —
// AND for a formatter value whose literal remainder is not votable ("%1$s", "%d%%", "%s:"
// compile to (near-)match-anything patterns; one such passthrough string in a base locale
// matched EVERY candidate and flipped the vote to a CONFIDENT wrong language). The literal
// content between wildcards must satisfy the same bar as a votable candidate: ≥ 3 chars with
// at least one letter.
export function resourceMatcher(value) {
  const norm = normalizeText(value)
  if (!norm) return null
  FORMATTER_RE.lastIndex = 0
  if (!FORMATTER_RE.test(norm)) return { exact: norm }
  FORMATTER_RE.lastIndex = 0
  const parts = norm.split(FORMATTER_RE)
  const literal = parts.join('').replace(/\s+/g, ' ').trim()
  if (!(literal.length >= 3 && /\p{L}/u.test(literal))) return null
  return { pattern: new RegExp(`^${parts.map(escapeRe).join('[\\s\\S]+?')}$`) }
}

// The `values*` dirs a locale's strings may live in. The BASE locale (first supportedLocales
// entry) also owns the unsuffixed `values/`; every locale additionally gets its explicit
// `values-<lang>[-r<REGION>]` / `values-b+<lang>+<REGION>` forms. Matching against these
// DERIVED names (instead of classifying arbitrary values-* suffixes) keeps qualifiers like
// `values-night` / `values-w600dp` from ever being read as locales.
export function localeValueDirs(locale, isBase) {
  const lang = languageOf(locale)
  if (!lang) return []
  const segs = String(locale).replace(/_/g, '-').split('-')
  let region = null
  if (segs[1]) {
    if (/^r[A-Za-z]{2}$/.test(segs[1])) region = segs[1].slice(1).toUpperCase()
    else if (/^[A-Za-z]{2}$/.test(segs[1])) region = segs[1].toUpperCase()
  }
  const dirs = []
  if (isBase) dirs.push('values')
  dirs.push(`values-${lang}`)
  if (region) dirs.push(`values-${lang}-r${region}`, `values-b+${lang}+${region}`)
  return dirs
}

function localeMatchers(resourceRoots, locale, isBase) {
  const exact = new Set()
  const patterns = []
  const files = []
  const errors = []
  for (const root of resourceRoots) {
    for (const dir of localeValueDirs(locale, isBase)) {
      const file = join(root, dir, 'strings.xml')
      if (!existsSync(file)) continue
      let xml = ''
      try { xml = readUtf8(file) } catch {
        errors.push({ code: 'locale-resource-unavailable' })
        continue
      }
      files.push(file)
      for (const value of parseStringsXml(xml)) {
        const m = resourceMatcher(value)
        if (!m) continue
        if (m.exact) exact.add(m.exact)
        else patterns.push(m.pattern)
      }
    }
  }
  return { exact, patterns, files, errors }
}

// detectDesignLocale({ specs, resourceRoots, supportedLocales })
//   -> { locale, confident, scores, candidates, files }
// Score = DISTINCT candidate spec strings matched per locale. `confident` iff the winner
// matched ≥ 2 strings AND strictly beats every other locale — a tie or a zero-match vote is
// not a detection (the caller fails closed). `candidates: 0` means the design carries no
// votable text at all (a genuinely textless design) —
// there is nothing to enforce, which is distinct from an undecided vote. `files` lists every
// strings.xml the vote consulted, so a gate can hash the decision's inputs.
export function detectDesignLocale({ specs = [], resourceRoots = [], supportedLocales = [] } = {}) {
  const candidates = new Set()
  for (const spec of specs) {
    const elements = spec && Array.isArray(spec.elements) ? spec.elements : []
    for (const el of elements) {
      if (!el || typeof el.text !== 'string') continue
      const t = normalizeText(el.text)
      if (votable(t)) candidates.add(t)
    }
  }
  const scores = {}
  const files = []
  const errors = []
  const locales = [...new Set(supportedLocales.filter(Boolean).map((l) => String(l).trim()))]
  if (!candidates.size || !locales.length) return { locale: null, confident: false, scores, candidates: candidates.size, files }
  locales.forEach((locale, i) => {
    const m = localeMatchers(resourceRoots, locale, i === 0)
    files.push(...m.files)
    errors.push(...m.errors)
    let n = 0
    for (const c of candidates) if (m.exact.has(c) || m.patterns.some((re) => re.test(c))) n++
    scores[locale] = n
  })
  let winner = null
  let best = -1
  let tied = false
  for (const [locale, n] of Object.entries(scores)) {
    if (n > best) { winner = locale; best = n; tied = false }
    else if (n === best) tied = true
  }
  const decisive = !tied && best > 0
  return { locale: decisive ? winner : null, confident: decisive && best >= 2, scores, candidates: candidates.size, files, errors }
}

// String-resource roots of the product repo: every `composeResources/` dir plus every
// `src/main/res/` dir, skipping VCS/build/tooling trees. FIGMA_STRING_RESOURCE_ROOTS
// (path-delimited) overrides the walk — fixtures only.
const ROOT_SCAN_SKIP = new Set(['.git', '.gradle', '.idea', '.cache', 'build', 'node_modules', 'orchestrator'])
export function deriveResourceRoots(rootDirs = [EXECUTION_ROOT]) {
  const env = process.env.FIGMA_STRING_RESOURCE_ROOTS
  const physical = (path) => { try { return realpathSync(path) } catch { return resolve(path) } }
  const uniquePaths = (paths) => {
    const byPhysical = new Map()
    for (const path of paths.filter(Boolean).map((value) => resolve(value))) {
      const identity = physical(path)
      if (!byPhysical.has(identity)) byPhysical.set(identity, path)
    }
    return [...byPhysical.values()]
  }
  if (env) return uniquePaths(env.split(delimiter)
    .map((value) => executionProductInputPath(value, 'FIGMA_STRING_RESOURCE_ROOTS')))
  const out = []
  const visited = new Set()
  const walk = (dir, depth) => {
    if (depth > 8) return
    const realDir = physical(dir)
    if (visited.has(realDir)) return
    visited.add(realDir)
    let entries = []
    try { entries = readdirSync(dir, { withFileTypes: true }) } catch { return }
    for (const d of entries) {
      if (ROOT_SCAN_SKIP.has(d.name)) continue
      const p = join(dir, d.name)
      let isDirectory = d.isDirectory()
      if (d.isSymbolicLink()) {
        if (EXECUTION_SCOPE) continue
        try { isDirectory = statSync(p).isDirectory() } catch { isDirectory = false }
      }
      if (!isDirectory) continue
      if (d.name === 'composeResources') { out.push(resolve(p)); continue }
      if (d.name === 'res' && dir.endsWith(`${sep}src${sep}main`)) { out.push(resolve(p)); continue }
      walk(p, depth + 1)
    }
  }
  for (const r of rootDirs) walk(r, 0)
  return uniquePaths(out)
}

// The committed `supportedLocales` list (block or inline-flow YAML) from project-config.
// FIGMA_SUPPORTED_LOCALES (comma-separated) overrides — fixtures only. A readable
// config with no key uses the contractual `en` default; a missing, unreadable,
// or invalid-UTF-8 config is not equivalent to that state and fails closed.
export function readSupportedLocales(projectRoot = EXECUTION_ROOT) {
  const env = process.env.FIGMA_SUPPORTED_LOCALES
  const validate = (values) => {
    const locales = values.map((s) => String(s).trim()).filter(Boolean)
    if (!locales.length || locales.some((locale) => !/^[A-Za-z]{2,3}(?:[-_][A-Za-z0-9]{2,8})*$/.test(locale)) ||
        new Set(locales).size !== locales.length) {
      throw new Error('supported locale configuration is invalid')
    }
    return locales
  }
  if (env) return validate(env.split(','))
  try {
    const md = readUtf8(join(projectRoot, 'orchestrator', 'project-config.md'))
    const m = /(?:^|\n)supportedLocales:([^\n]*)\n?((?:[ \t]+-[^\n]*\n?)*)/.exec(md)
    if (!m) return ['en']
    const inline = m[1].trim()
    if (inline) {
      if (!/^\[[^\[\]]+\]$/.test(inline)) throw new Error('supported locale configuration is invalid')
      return validate(inline.slice(1, -1).split(','))
    }
    const items = [...(m[2] || '').matchAll(/^[ \t]+-[ \t]*(.+?)[ \t]*$/gm)].map((x) => x[1])
    return validate(items)
  } catch {
    throw new Error('supported locale configuration is missing, invalid, unreadable, or not valid UTF-8')
  }
}

// The explicit declaration that short-circuits detection: FIGMA_DESIGN_LOCALE env
// (fixtures/diagnostics, mirrors the FIGMA_TOKEN_MODE precedent) → the committed
// `designLocale` config key. A `<placeholder>` value reads as absent (readConfig → null).
function declaredDesignLocale() {
  const env = (process.env.FIGMA_DESIGN_LOCALE || '').trim()
  if (env) return env
  const v = readConfig('designLocale')
  return v ? String(v).trim() : null
}

// resolveDesignLocale({ specs, resourceRoots, supportedLocales }) — the one call both
// consumers (check-capture-config, compare-screenshots) make. Outcomes:
//   { language, rtl, source: 'config'|'detected', locale, … }   — resolved, enforce it
//   { language: null, reason: 'invalid-config', locale, supported } — fail closed (declared
//       locale is not a supported locale; a typo must not silently disarm the check)
//   { language: null, reason: 'not-confident', scores, candidates }  — fail closed (votable
//       text exists but no locale wins decisively; remedy: declare `designLocale`)
//   { language: null, reason: 'no-signal', candidates: 0 }       — nothing to enforce (the
//       design carries no votable text)
export function resolveDesignLocale({ specs = [], resourceRoots = [], supportedLocales = null } = {}) {
  const supported = supportedLocales || readSupportedLocales()
  const declared = declaredDesignLocale()
  if (declared) {
    if (supported.includes(declared)) {
      const language = languageOf(declared)
      return { language, rtl: RTL_LANGUAGES.has(language), source: 'config', locale: declared }
    }
    return { language: null, reason: 'invalid-config', locale: declared, supported }
  }
  const det = detectDesignLocale({ specs, resourceRoots, supportedLocales: supported })
  if (det.errors && det.errors.length) {
    return { language: null, reason: 'source-unavailable', scores: det.scores, candidates: det.candidates, resourceFiles: det.files }
  }
  if (det.confident) {
    const language = languageOf(det.locale)
    return { language, rtl: RTL_LANGUAGES.has(language), source: 'detected', locale: det.locale, scores: det.scores, candidates: det.candidates, resourceFiles: det.files }
  }
  if (!det.candidates) return { language: null, reason: 'no-signal', candidates: 0, scores: det.scores, resourceFiles: det.files }
  return { language: null, reason: 'not-confident', scores: det.scores, candidates: det.candidates, resourceFiles: det.files }
}
