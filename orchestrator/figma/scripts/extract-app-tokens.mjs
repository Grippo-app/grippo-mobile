// extract-app-tokens.mjs — deterministic scanner for implementation-side AppTokens usage.
// It is intentionally a bounded text scanner, not a Kotlin AST. The output is a stable evidence map
// consumed by compare-screen-spec.mjs and by human validators.
//
// Usage:
//   node scripts/extract-app-tokens.mjs [--root <dir>] [--file <path>] [--out <path>]
//
// Env:
//   FIGMA_APP_TOKEN_ROOTS — path-delimited source roots
//   FIGMA_APP_TOKEN_FILES — path-delimited explicit files
//   FIGMA_APP_TOKENS_OUT  — output path

import { existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync, renameSync } from 'node:fs'
import { dirname, join, delimiter } from 'node:path'
import { TextDecoder } from 'node:util'
import { figmaPath, isDirectRun, PROJECT_ROOT } from './_util.mjs'

const SOURCE_EXT = /\.(kt|kts)$/i
const SKIP_DIRS = new Set(['.git', '.gradle', '.idea', 'build', 'node_modules', 'orchestrator'])
const utf8 = new TextDecoder('utf-8', { fatal: true })

function valuesAfter(flag) {
  const out = []
  for (let i = 2; i < process.argv.length; i++) if (process.argv[i] === flag && process.argv[i + 1]) out.push(process.argv[++i])
  return out
}

function lineOf(text, index) {
  let line = 1
  for (let i = 0; i < index; i++) if (text.charCodeAt(i) === 10) line++
  return line
}

function addRef(map, key, ref) {
  if (!map[key]) map[key] = { refs: [] }
  map[key].refs.push(ref)
}

// Single-pass, context-aware trivia masker. Blanks ALL comments (line + nesting block),
// strings (double + triple), and char literals to spaces — preserving newlines and total
// length so lineOf() and every index-based range stay correct. A single pass is required
// for correctness: a chained-regex approach masks comments and strings in SEPARATE passes,
// so it mishandles `/*` inside a string (e.g. a glob "src/*"), a `"` inside a comment, and a
// `'` (char literal) containing a `"`. Walking with explicit state avoids all of those.
// Exported for component-census.mjs's reuse-candidate scan (same body as the
// extract-compose-model.mjs copy — keep the two tokenizers in lock-step when editing).
export function maskKotlinTrivia(text) {
  const s = String(text || '')
  const n = s.length
  const out = new Array(n)
  const blank = (i) => { out[i] = (s[i] === '\n' || s[i] === '\r') ? s[i] : ' ' }
  let i = 0
  while (i < n) {
    const c = s[i], c2 = s[i + 1]
    if (c === '/' && c2 === '/') {                         // line comment
      while (i < n && s[i] !== '\n' && s[i] !== '\r') { out[i] = ' '; i++ }
      continue
    }
    if (c === '/' && c2 === '*') {                         // block comment (nesting)
      let depth = 1
      out[i] = ' '; out[i + 1] = ' '; i += 2
      while (i < n && depth > 0) {
        if (s[i] === '/' && s[i + 1] === '*') { out[i] = ' '; out[i + 1] = ' '; i += 2; depth++ }
        else if (s[i] === '*' && s[i + 1] === '/') { out[i] = ' '; out[i + 1] = ' '; i += 2; depth-- }
        else { blank(i); i++ }
      }
      continue
    }
    if (c === '"' && c2 === '"' && s[i + 2] === '"') {     // triple-quoted (raw) string
      out[i] = ' '; out[i + 1] = ' '; out[i + 2] = ' '; i += 3
      while (i < n) {
        if (s[i] === '"' && s[i + 1] === '"' && s[i + 2] === '"') { out[i] = ' '; out[i + 1] = ' '; out[i + 2] = ' '; i += 3; break }
        blank(i); i++
      }
      continue
    }
    if (c === '"') {                                       // double-quoted string
      out[i] = ' '; i++
      while (i < n) {
        if (s[i] === '\\') { out[i] = ' '; if (i + 1 < n) blank(i + 1); i += 2; continue }
        if (s[i] === '"') { out[i] = ' '; i++; break }
        if (s[i] === '\n' || s[i] === '\r') { out[i] = s[i]; i++; break }   // unterminated: stop at EOL
        out[i] = ' '; i++
      }
      continue
    }
    if (c === "'") {                                       // char literal
      out[i] = ' '; i++
      while (i < n) {
        if (s[i] === '\\') { out[i] = ' '; if (i + 1 < n) blank(i + 1); i += 2; continue }
        if (s[i] === "'") { out[i] = ' '; i++; break }
        if (s[i] === '\n' || s[i] === '\r') { out[i] = s[i]; i++; break }
        out[i] = ' '; i++
      }
      continue
    }
    out[i] = c; i++                                        // code
  }
  return out.join('')
}

// Resolve hoisted AppTokens aliases into synthetic full-path token keys. A Canvas/DrawScope widget
// cannot call the @Composable `AppTokens.*` accessor inside a draw lambda, so it hoists
// `val c = AppTokens.colors.group` and uses `c.leaf` — the full path `colors.group.leaf` then never
// appears verbatim and the exact-match token scan misses it (a false MISSING_COLOR_TOKEN). This
// recovers it, opt-in (compare-screen-spec passes resolveAliases; census/drift do not, so their
// evidence is unchanged).
//
// Strictness guard (the false-POSITIVE direction — a synthetic key must never mask a real
// mismatch): a key is synthesized ONLY from a real `val NAME = AppTokens.kind.prefix` paired with a
// real `NAME.tail` use, so the key is exactly the token the code references (a wrong tail yields a
// different key that no spec asks for). Each alias is scoped to its ENCLOSING BRACE BLOCK and cut at
// the FIRST point NAME is re-bound in ANY form — a second `val`/`var`, a `for (NAME in …)` loop var,
// a `NAME:` typed parameter, OR a lambda parameter (`{ … NAME … -> }`, incl. destructuring). Kotlin
// shadowing via lambda/loop/param binders does NOT use `val`, so cutting only on `val` (the earlier
// version) let a shadowed `NAME.leaf` fabricate a false key; cutting on every binder closes that.
function resolveHoistedAliases(scan, text, file, tokens) {
  // The optional `: Type` slot (W6-4): `val c: AppColor.TextColors = AppTokens.colors.text`
  // is idiomatic Kotlin (and encouraged by some linters) — requiring the annotation to be
  // ABSENT is the same regex-form brittleness as requiring it PRESENT. Soundness is
  // unchanged: a key is still synthesized only
  // from a real `NAME.tail` use after the declaration.
  const declRe = /\bval\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?::\s*[A-Za-z_][A-Za-z0-9_.]*(?:<[^<>]*>)?\??\s*)?=\s*AppTokens\.(colors|dp|typography)((?:\.[A-Za-z_][A-Za-z0-9_]*)*)/g
  let d
  while ((d = declRe.exec(scan))) {
    const name = d[1]
    const prefix = `${d[2]}${d[3] || ''}`                 // "colors" or "colors.group"
    const declEnd = declRe.lastIndex
    const scopeEnd = earliestRebind(scan, name, declEnd, enclosingBlockEnd(scan, declEnd))
    const useRe = new RegExp('\\b' + name + '\\.([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*)', 'g')
    useRe.lastIndex = declEnd
    let u
    while ((u = useRe.exec(scan)) && u.index < scopeEnd) {
      addRef(tokens, `${prefix}.${u[1]}`, { file, line: lineOf(text, u.index), via: 'alias' })
    }
  }
}

// Earliest index in [from, end) where `name` is RE-BOUND (so the alias no longer refers to the
// AppTokens group past that point), or `end` if never. Covers: another `val`/`var name`; a
// `for (name in …)` loop var; a `name :` typed binder (fn/lambda param or property); and a lambda
// parameter — `name` appearing between a `{` and its following `->` (single, multi, or destructured
// params). Conservative by design: it may over-cut (e.g. a `when` branch `name -> …`), which only
// costs recall (a legit use goes uncredited → stays a BLOCKER), never soundness.
function earliestRebind(scan, name, from, end) {
  const n = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  let min = end
  const binder = new RegExp(
    '\\b(?:val|var)\\s+' + n + '\\b' +                 // a second val/var NAME
    '|\\bfor\\s*\\(\\s*(?:val\\s+)?' + n + '\\b' +      // for (NAME in …) / for (val NAME in …)
    '|\\bfor\\s*\\(\\s*\\([^)]*\\b' + n + '\\b' +       // for ((a, NAME) in …) destructuring header
    '|\\b' + n + '\\s*:',                               // NAME: typed param/property
    'g')
  binder.lastIndex = from
  const b = binder.exec(scan)
  if (b && b.index < end) min = Math.min(min, b.index)
  const arrow = /->/g
  arrow.lastIndex = from
  const word = new RegExp('\\b' + n + '\\b')
  let a
  while ((a = arrow.exec(scan)) && a.index < end) {
    let open = scan.lastIndexOf('{', a.index)
    if (open < from) open = from
    if (word.test(scan.slice(open, a.index))) { min = Math.min(min, open + 1); break }
  }
  return min
}

// Index of the `}` that closes the block enclosing `from` (net brace depth first goes negative), or
// end-of-text. Runs on masked source, so braces inside strings/comments are already blanked.
function enclosingBlockEnd(scan, from) {
  let depth = 0
  for (let i = from; i < scan.length; i++) {
    const c = scan[i]
    if (c === '{') depth++
    else if (c === '}') { if (depth === 0) return i; depth-- }
  }
  return scan.length
}

export function extractAppTokensFromText(text, file = '<memory>', opts = {}) {
  const scan = maskKotlinTrivia(text)
  const tokens = {}
  const raw = { colors: {}, dp: {}, sp: {}, fontWeights: {} }
  const tokenRe = /\bAppTokens\.(colors|dp|typography)\.([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)(?:\s*\(\s*\))?/g
  let m
  while ((m = tokenRe.exec(scan))) {
    const key = `${m[1]}.${m[2]}`
    addRef(tokens, key, { file, line: lineOf(text, m.index) })
  }
  if (opts.resolveAliases) resolveHoistedAliases(scan, text, file, tokens)
  const colorRe = /\bColor\s*\(\s*0x([0-9A-Fa-f]{8})[uUlL]*\s*\)/g   // optional Kotlin u/L suffix (Color(0xFFFF0000L))
  while ((m = colorRe.exec(scan))) {
    const argb = m[1].toUpperCase()
    const ref = { file, line: lineOf(text, m.index), literal: `Color(0x${m[1]})` }
    addRef(raw.colors, `#${argb.slice(2)}`, ref)
    // Translucent literal: ALSO store the 8-digit key in spec order (#RRGGBBAA — Figma/CSS
    // export order, AARRGGBB→RRGGBBAA) so an 8-digit spec resolvedValue can match evidence
    // (compare-screen-spec hasRawColor treats full alpha as equal to the 6-digit key).
    if (argb.slice(0, 2) !== 'FF') addRef(raw.colors, `#${argb.slice(2)}${argb.slice(0, 2)}`, ref)
  }
  const dpRe = /(^|[^\w.])(-?\d+(?:\.\d+)?)\.dp\b/g
  while ((m = dpRe.exec(scan))) addRef(raw.dp, String(Number(m[2])), { file, line: lineOf(text, m.index), literal: `${m[2]}.dp` })
  const spRe = /(^|[^\w.])(-?\d+(?:\.\d+)?)\.sp\b/g
  while ((m = spRe.exec(scan))) addRef(raw.sp, String(Number(m[2])), { file, line: lineOf(text, m.index), literal: `${m[2]}.sp` })
  const weightRe = /\bFontWeight\.(W[1-9]00|Thin|ExtraLight|Light|Normal|Medium|SemiBold|Bold|ExtraBold|Black)\b/g
  while ((m = weightRe.exec(scan))) addRef(raw.fontWeights, m[1], { file, line: lineOf(text, m.index) })
  return { tokens, raw }
}

function mergeEvidence(into, part) {
  for (const [key, value] of Object.entries(part.tokens || {})) {
    if (!into.tokens[key]) into.tokens[key] = { refs: [] }
    into.tokens[key].refs.push(...(value.refs || []))
  }
  for (const kind of Object.keys(into.raw)) {
    for (const [key, value] of Object.entries((part.raw && part.raw[kind]) || {})) {
      if (!into.raw[kind][key]) into.raw[kind][key] = { refs: [] }
      into.raw[kind][key].refs.push(...value.refs)
    }
  }
}

function collectFiles(root, out = []) {
  if (!existsSync(root)) return out
  const entries = readdirSync(root, { withFileTypes: true })
  for (const entry of entries) {
    const p = join(root, entry.name)
    if (entry.isDirectory()) {
      if (!SKIP_DIRS.has(entry.name)) collectFiles(p, out)
    } else if (entry.isFile() && SOURCE_EXT.test(entry.name)) {
      out.push(p)
    }
  }
  return out
}

export function extractAppTokens({ roots = [], files = [], resolveAliases = false } = {}) {
  const evidence = { schemaVersion: 1, generatedAt: new Date().toISOString(), roots, files: [], tokens: {}, raw: { colors: {}, dp: {}, sp: {}, fontWeights: {} } }
  const allFiles = [...new Set([...files, ...roots.flatMap((r) => collectFiles(r))])]
  for (const file of allFiles.sort()) {
    if (!SOURCE_EXT.test(file)) continue
    if (!existsSync(file)) throw new Error('implementation source is missing')
    let text
    try { text = utf8.decode(readFileSync(file)) } catch {
      throw new Error('implementation source is unreadable or not valid UTF-8')
    }
    evidence.files.push(file)
    mergeEvidence(evidence, extractAppTokensFromText(text, file, { resolveAliases }))
  }
  return evidence
}

function atomicWrite(path, data) {
  mkdirSync(dirname(path), { recursive: true })
  const tmp = path + '.tmp'
  writeFileSync(tmp, JSON.stringify(data, null, 2) + '\n')
  renameSync(tmp, path)
}

if (isDirectRun(import.meta.url)) {
  const roots = [
    ...valuesAfter('--root'),
    ...(process.env.FIGMA_APP_TOKEN_ROOTS ? process.env.FIGMA_APP_TOKEN_ROOTS.split(delimiter).filter(Boolean) : []),
  ]
  const files = [
    ...valuesAfter('--file'),
    ...(process.env.FIGMA_APP_TOKEN_FILES ? process.env.FIGMA_APP_TOKEN_FILES.split(delimiter).filter(Boolean) : []),
  ]
  const outFlag = valuesAfter('--out')[0]
  const out = outFlag || process.env.FIGMA_APP_TOKENS_OUT || figmaPath('reports', 'app-tokens.json')
  const scanRoots = roots.length ? roots : (files.length ? [] : [PROJECT_ROOT])
  // W6-4: a pre-extracted evidence file consumed via FIGMA_APP_TOKENS bypasses the spec
  // gate's own alias resolution (loadEvidence resolves only when IT extracts), so a canvas
  // hoisted alias silently false-BLOCKed on that input path. `--resolve-aliases` bakes the
  // synthetic alias keys into the output — the spec-fidelity gate's documented pre-extract
  // invocation must pass it.
  const resolveAliases = process.argv.includes('--resolve-aliases')
  const evidence = extractAppTokens({ roots: scanRoots, files, resolveAliases })
  atomicWrite(out, evidence)
  console.log(`extract-app-tokens: ${Object.keys(evidence.tokens).length} token path(s), ${evidence.files.length} file(s)${resolveAliases ? ', aliases resolved' : ''} -> ${out}`)
}
