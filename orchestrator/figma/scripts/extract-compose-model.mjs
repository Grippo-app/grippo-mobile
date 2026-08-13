// extract-compose-model.mjs — syntax-aware Kotlin/Compose implementation model for Figma comparison.
// It never calls Figma and never compiles the app. Tree-sitter provides parse/error evidence; bounded
// scanners extract stable facts the comparator can consume without trusting comments/strings.
//
// Usage:
//   node scripts/extract-compose-model.mjs --out <model.json> [--file <Screen.kt>] [--root <srcRoot>]
//
// Env:
//   FIGMA_SPEC_IMPL_FILES       — path-delimited explicit Kotlin files
//   FIGMA_SPEC_IMPL_ROOTS       — path-delimited Kotlin source roots
//   FIGMA_IMPL_MODEL_OUT        — output path

import { existsSync, readdirSync, readFileSync, realpathSync, statSync } from 'node:fs'
import { delimiter, join, relative, resolve } from 'node:path'
import { parseCli, PROJECT_ROOT, EXECUTION_ROOT, executionProductInputPath, executionFigmaOutputPath, writeFigmaRuntimeFile } from './_util.mjs'
import { extractAppTokensFromText } from './extract-app-tokens.mjs'
import { sha256Text } from './report-utils.mjs'
import { parseKotlinSource, parserInfo } from './compose-model/parser-tree-sitter.mjs'

const SOURCE_EXT = /\.(kt|kts)$/i
const SKIP_DIRS = new Set(['.git', '.gradle', '.idea', 'build', 'node_modules', 'orchestrator'])
const USAGE = 'usage: node scripts/extract-compose-model.mjs --out <model.json> [--file <Screen.kt>] [--root <srcRoot>]'
const TOP_LEVEL_DECL_RE = /^\s*(?:@\s*(?:[A-Za-z_][\w]*\.)*[A-Za-z_][\w]*(?:\s*\([^)]*\))?\s*)*(?:(?:public|private|protected|internal|expect|actual|final|open|abstract|override|inline|tailrec|operator|infix|external|suspend|const|lateinit)\s+)*(?:fun|val|var|class|interface|object|enum|typealias)\b/
const COMPOSE_BUILTIN_CALLS = new Set([
  'AlertDialog',
  'AnimatedContent',
  'AsyncImage',
  'BadgedBox',
  'BasicText',
  'BasicTextField',
  'Box',
  'Button',
  'Card',
  'Checkbox',
  'CircularProgressIndicator',
  'Column',
  'Crossfade',
  'Divider',
  'DropdownMenu',
  'DropdownMenuItem',
  'ElevatedButton',
  'ElevatedCard',
  'ExposedDropdownMenuBox',
  'FilledIconButton',
  'FilledTonalButton',
  'FilledTonalIconButton',
  'FloatingActionButton',
  'FlowColumn',
  'FlowRow',
  'HorizontalDivider',
  'Icon',
  'IconButton',
  'Image',
  'LazyColumn',
  'LazyHorizontalGrid',
  'LazyHorizontalStaggeredGrid',
  'LazyRow',
  'LazyVerticalGrid',
  'LazyVerticalStaggeredGrid',
  'LinearProgressIndicator',
  'ListItem',
  'ModalBottomSheet',
  'NavigationBar',
  'NavigationBarItem',
  'NavigationRail',
  'NavigationRailItem',
  'OutlinedButton',
  'OutlinedCard',
  'OutlinedTextField',
  'RadioButton',
  'Row',
  'Scaffold',
  'Slider',
  'SmallFloatingActionButton',
  'Spacer',
  'Surface',
  'Switch',
  'Tab',
  'TabRow',
  'Text',
  'TextButton',
  'TextField',
  'VerticalDivider',
])

function issue(severity, issueKind, message, extra = {}) {
  return Object.assign({ severity, issueKind, message }, extra)
}

function projectPath(path) {
  const rel = relative(EXECUTION_ROOT, resolve(path))
  return rel && !rel.startsWith('..') ? rel : resolve(path)
}

function collectFiles(root, out = []) {
  if (!existsSync(root)) return out
  let entries = []
  try { entries = readdirSync(root, { withFileTypes: true }) } catch { return out }
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

function pathKind(path) {
  try {
    const st = statSync(path)
    return st.isFile() ? 'file' : st.isDirectory() ? 'dir' : 'other'
  } catch {
    return 'missing'
  }
}

function lineOf(text, index) {
  let line = 1
  for (let i = 0; i < index; i++) if (text.charCodeAt(i) === 10) line++
  return line
}

// Single-pass, context-aware trivia masker. Blanks ALL comments (line + nesting block),
// strings (double + triple), and char literals to spaces — preserving newlines and total
// length so lineOf() and every index-based range stay correct. A single pass is required
// for correctness: a chained-regex approach masks comments and strings in SEPARATE passes,
// so it mishandles `/*` inside a string (e.g. a glob "src/*"), a `"` inside a comment, and a
// `'` (char literal) containing a `"`. Walking with explicit state avoids all of those.
function maskKotlinTrivia(text) {
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

function splitTopLevel(raw) {
  const out = []
  let start = 0, depth = 0
  const s = String(raw || '')
  const closing = ')]}>'
  for (let i = 0; i < s.length; i++) {
    const ch = s[i]
    if ('([{<'.includes(ch)) depth++
    else if (closing.includes(ch)) depth = Math.max(0, depth - 1)
    else if (ch === ',' && depth === 0) { out.push(s.slice(start, i).trim()); start = i + 1 }
  }
  const tail = s.slice(start).trim()
  if (tail) out.push(tail)
  return out
}

function parseParams(raw) {
  return splitTopLevel(raw).map((part) => {
    const m = part.match(/^(?:\w+\s+)*(?<name>[A-Za-z_][A-Za-z0-9_]*)\s*:\s*(?<type>[^=]+?)(?:\s*=\s*(?<def>[\s\S]+))?$/)
    if (!m || !m.groups) return { raw: part, unresolved: true }
    return {
      name: m.groups.name,
      type: m.groups.type.trim(),
      defaultValue: m.groups.def ? m.groups.def.trim() : null,
      raw: part,
    }
  })
}

function packageName(scan) {
  const m = scan.match(/^\s*package\s+([\w.]+)/m)
  return m ? m[1] : null
}

function findMatching(text, openIndex, openChar, closeChar) {
  let depth = 0
  for (let i = openIndex; i < text.length; i++) {
    const ch = text[i]
    if (ch === openChar) depth++
    else if (ch === closeChar) {
      depth--
      if (depth === 0) return i
    }
  }
  return -1
}

function braceDepthAt(scan, index) {
  let depth = 0
  for (let i = 0; i < index; i++) {
    const ch = scan[i]
    if (ch === '{') depth++
    else if (ch === '}') depth = Math.max(0, depth - 1)
  }
  return depth
}

function findExpressionEnd(scan, start) {
  let depth = 0
  for (let i = start; i < scan.length; i++) {
    const ch = scan[i]
    if ('({['.includes(ch)) depth++
    else if (')}]'.includes(ch)) depth = Math.max(0, depth - 1)
    else if (ch === '\n' && depth === 0) {
      const tail = scan.slice(i + 1, i + 160)
      if (TOP_LEVEL_DECL_RE.test(tail)) return i
    }
  }
  return scan.length
}

function findLocalExpressionEnd(scan, start, limit) {
  let depth = 0
  for (let i = start; i < limit; i++) {
    const ch = scan[i]
    if ('({['.includes(ch)) depth++
    else if (')}]'.includes(ch)) {
      if (depth === 0 && ch === '}') return i
      depth = Math.max(0, depth - 1)
    } else if ((ch === '\n' || ch === ';') && depth === 0) {
      return i
    }
  }
  return limit
}

function bodyRange(scan, afterParams) {
  for (let i = afterParams + 1; i < scan.length; i++) {
    const ch = scan[i]
    if (ch === '{') {
      const end = findMatching(scan, i, '{', '}')
      return { start: i + 1, end: end >= 0 ? end : scan.length }
    }
    if (ch === '=') {
      return { start: i + 1, end: findExpressionEnd(scan, i + 1) }
    }
    if (ch === '\n' && TOP_LEVEL_DECL_RE.test(scan.slice(i + 1, i + 160))) break
  }
  return { start: afterParams + 1, end: afterParams + 1 }
}

function findComposables(source, file) {
  const scan = maskKotlinTrivia(source)
  const out = []
  const pkg = packageName(scan)
  const seen = new Set()
  const composable = /@\s*(?:[A-Za-z_][\w]*\.)*Composable(?:\s*\([^)]*\))?/g
  let m
	  while ((m = composable.exec(scan))) {
	    if (braceDepthAt(scan, m.index) !== 0) continue
	    const tailStart = composable.lastIndex
	    const tail = scan.slice(tailStart)
	    const fn = /^(?:\s*@\s*(?:[A-Za-z_][\w]*\.)*[A-Za-z_][\w]*(?:\s*\([^)]*\))?)*\s*(?:(?:public|private|protected|internal|expect|actual|final|open|abstract|override|inline|tailrec|operator|infix|external|suspend)\s+)*fun\s+(?:<[^>(){}]*>\s*)?(?:(?:[A-Za-z_][\w]*(?:\.[A-Za-z_][\w]*)*)\s*\.\s*)?([A-Z][A-Za-z0-9_]*)\s*\(/.exec(tail)
    if (!fn) continue
    const openIndex = tailStart + fn[0].lastIndexOf('(')
    if (seen.has(openIndex)) continue
    seen.add(openIndex)
    const closeIndex = findMatching(scan, openIndex, '(', ')')
    if (closeIndex < 0) continue
    const name = fn[1]
    const range = bodyRange(scan, closeIndex)
    out.push({
      name,
	      fqName: pkg ? `${pkg}.${name}` : name,
	      file: projectPath(file),
	      package: pkg,
	      visibility: /\bprivate\b/.test(fn[0]) ? 'private' : /\bprotected\b/.test(fn[0]) ? 'protected' : /\binternal\b/.test(fn[0]) ? 'internal' : 'public',
	      line: lineOf(source, m.index),
      params: parseParams(scan.slice(openIndex + 1, closeIndex)),
      bodyStart: range.start,
      bodyEnd: range.end,
    })
  }
  return out
}

function nestedDeclarationRanges(scan, start, end) {
  const ranges = []
  const decl = /\b(fun|class|interface|object|enum)\s+([A-Za-z_][A-Za-z0-9_]*)/g
  decl.lastIndex = start
  let m
  while ((m = decl.exec(scan)) && m.index < end) {
    let bodyStart = scan.indexOf('{', decl.lastIndex)
    const parenStart = m[1] === 'fun' ? scan.indexOf('(', decl.lastIndex) : -1
    let parenEnd = -1
    if (parenStart >= 0 && parenStart < end) {
      parenEnd = findMatching(scan, parenStart, '(', ')')
      if (parenEnd >= 0) bodyStart = scan.indexOf('{', parenEnd + 1)
    }
    if (m[1] === 'fun' && parenEnd >= 0) {
      const eq = scan.indexOf('=', parenEnd + 1)
      if (eq >= 0 && eq < end && (bodyStart < 0 || eq < bodyStart)) {
        const exprEnd = findLocalExpressionEnd(scan, eq + 1, end)
        ranges.push({
          kind: 'fun',
          name: m[2],
          start: m.index,
          end: exprEnd,
          bodyStart: eq + 1,
          bodyEnd: exprEnd,
        })
        decl.lastIndex = Math.max(decl.lastIndex, exprEnd)
        continue
      }
    }
    if (bodyStart < 0 || bodyStart >= end) continue
    const bodyEnd = findMatching(scan, bodyStart, '{', '}')
    ranges.push({
      kind: m[1],
      name: m[2],
      start: m.index,
      end: bodyEnd >= 0 ? Math.min(bodyEnd + 1, end) : end,
      bodyStart: bodyStart + 1,
      bodyEnd: bodyEnd >= 0 ? Math.min(bodyEnd, end) : end,
    })
    decl.lastIndex = Math.max(decl.lastIndex, bodyEnd >= 0 ? bodyEnd + 1 : end)
  }
  const assignedLambda = /\b(?:val|var)\s+([A-Za-z_][A-Za-z0-9_]*)[^=\n{};]{0,200}=\s*\{/g
  assignedLambda.lastIndex = start
  while ((m = assignedLambda.exec(scan)) && m.index < end) {
    const bodyStart = scan.indexOf('{', m.index)
    if (bodyStart < 0 || bodyStart >= end) continue
    const bodyEnd = findMatching(scan, bodyStart, '{', '}')
    ranges.push({
      kind: 'lambda',
      name: m[1],
      start: m.index,
      end: bodyEnd >= 0 ? Math.min(bodyEnd + 1, end) : end,
      bodyStart: bodyStart + 1,
      bodyEnd: bodyEnd >= 0 ? Math.min(bodyEnd, end) : end,
    })
    assignedLambda.lastIndex = Math.max(assignedLambda.lastIndex, bodyEnd >= 0 ? bodyEnd + 1 : end)
  }
  return ranges.sort((a, b) => a.start - b.start)
}

function inRanges(index, ranges) {
  return ranges.some((range) => index >= range.start && index < range.end)
}

function escapeRegex(text) {
  return String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function hasInvocation(scan, name, start, end, excludedRanges) {
  const re = new RegExp(`\\b${escapeRegex(name)}\\s*(?=\\(|\\{)`, 'g')
  re.lastIndex = Math.max(0, start)
  let m
  while ((m = re.exec(scan)) && m.index < end) {
    if (!inRanges(m.index, excludedRanges)) return true
  }
  return false
}

function hasReference(scan, name, start, end, excludedRanges) {
  const re = new RegExp(`\\b${escapeRegex(name)}\\b|::\\s*${escapeRegex(name)}\\b`, 'g')
  re.lastIndex = Math.max(0, start)
  let m
  while ((m = re.exec(scan)) && m.index < end) {
    if (!inRanges(m.index, excludedRanges)) return true
  }
  return false
}

// R2 arg-extractor. Slice a call's DIRECT parenthesized arguments (NOT its trailing `{ … }`
// lambda body, so nested child calls' args are not misattributed to this element) from the
// trivia-masked scan, then extract the structured facts the spec-compare gate binds per element:
// inline `N.dp` literals (incl. multi-arg `padding(16.dp, 8.dp)` and `Arrangement.spacedBy(8.dp)`),
// `AppTokens.*`/`AppColor.*` token refs, and raw `Color(0x…)` literals. The scan is already
// tree-sitter-trivia-masked (strings/comments → spaces), so no string/comment noise leaks in.
function sliceCallParenArgs(scan, calleeIndex, callee) {
  let i = calleeIndex + callee.length
  while (i < scan.length && /\s/.test(scan[i])) i++
  if (scan[i] !== '(') return ''   // trailing-lambda-only call (e.g. `Column { … }`) has no direct paren args
  const start = i + 1
  let depth = 1
  const cap = Math.min(scan.length, start + 4000)   // bound pathological arg spans
  for (i = start; i < cap; i++) {
    const c = scan[i]
    if (c === '(') depth++
    else if (c === ')') { depth--; if (depth === 0) return scan.slice(start, i) }
  }
  return scan.slice(start, cap)
}
const DP_ARG_RE = /(\d+(?:\.\d+)?)\s*\.\s*dp\b/g
const TOKEN_ARG_RE = /\bAppTokens\.[A-Za-z0-9_.]+|\bAppColor\.[A-Za-z0-9_]+/g
// Kept in lockstep with extract-app-tokens' colorRe: a SINGLE-arg Color(0xAARRGGBB) literal with
// the closing `)` anchored (so a multi-arg Color(...) or a chained expression does NOT over-match —
// which would let the R2 hint key on a colour the file-scoped evidence never records), and an
// optional Kotlin unsigned/Long suffix (u/U/L) before the paren so Color(0xFFFF0000L) is caught.
const COLOR_ARG_RE = /\bColor\s*\(\s*0x([0-9A-Fa-f]{8})[uUlL]*\s*\)/g
function extractCallArgs(argText) {
  if (!argText) return null
  const dp = new Set(), tokens = new Set(), colors = new Set()
  let m
  DP_ARG_RE.lastIndex = 0
  while ((m = DP_ARG_RE.exec(argText))) { const n = Number(m[1]); if (Number.isFinite(n) && n >= 0) dp.add(n) }
  TOKEN_ARG_RE.lastIndex = 0
  while ((m = TOKEN_ARG_RE.exec(argText))) tokens.add(m[0])
  COLOR_ARG_RE.lastIndex = 0
  // Compose Color(0xAARRGGBB) → #RRGGBBAA (the resolved Figma spec's colour convention). Alpha is
  // PRESERVED and reordered to the trailing position so a TRANSLUCENT literal (a scrim Color(0x80…))
  // is NOT conflated with the SOLID fill token it shares RGB with — dropping alpha was a proven
  // false-match. m[1] is AARRGGBB → RRGGBB (slice 2) + AA (slice 0,2).
  while ((m = COLOR_ARG_RE.exec(argText))) colors.add(`#${(m[1].slice(2) + m[1].slice(0, 2)).toUpperCase()}`)
  if (!dp.size && !tokens.size && !colors.size) return null
  const out = {}
  if (dp.size) out.dp = [...dp].sort((a, b) => a - b)
  if (tokens.size) out.tokens = [...tokens].sort()
  if (colors.size) out.colors = [...colors].sort()
  return out
}

function pushPascalCalls(out, seen, scan, source, file, start, end, excludedRanges, localTargets = new Set(), pkg = null, owner = null) {
  const re = /\b([A-Z][A-Za-z0-9_]*)\s*(?=\(|\{)/g
  re.lastIndex = Math.max(0, start)
  let m
  while ((m = re.exec(scan)) && m.index < end) {
    if (inRanges(m.index, excludedRanges)) continue
    const prefix = scan.slice(Math.max(0, m.index - 32), m.index)
    const prev = prefix.match(/\S(?=\s*$)/)
    if (prev && prev[0] === '@') continue
    if (/\b(fun|class|interface|object|enum|typealias|constructor)\s+$/.test(prefix)) continue
    const key = `${m.index}:${m[1]}`
    if (seen.has(key)) continue
    seen.add(key)
	    const call = {
		      callee: m[1],
		      file: projectPath(file),
		      package: pkg,
		      owner,
		      line: lineOf(source, m.index),
      raw: scan.slice(m.index, Math.min(scan.length, m.index + 240)).replace(/\s+/g, ' ').trim(),
    }
    if (localTargets.has(m[1])) call.localTarget = true
    const args = extractCallArgs(sliceCallParenArgs(scan, m.index, m[1]))   // R2: structured direct-arg facts for element binding
    if (args) call.args = args
    out.push(call)
  }
}

function invokedDeclarationCalls(out, seen, scan, source, file, declaration, pkg = null, visited = new Set(), owner = null) {
  const key = `${declaration.start}:${declaration.name}`
  if (visited.has(key)) return
  visited.add(key)
	  const nested = nestedDeclarationRanges(scan, declaration.bodyStart || 0, declaration.bodyEnd || 0)
	  const localTargets = new Set(nested.filter((n) => n.kind === 'fun').map((n) => n.name))
		  pushPascalCalls(out, seen, scan, source, file, declaration.bodyStart || 0, declaration.bodyEnd || 0, nested, localTargets, pkg, owner)
	  for (const inner of nested) {
	    if (inner.kind !== 'fun' && inner.kind !== 'lambda') continue
	    if (hasInvocation(scan, inner.name, declaration.bodyStart || 0, declaration.bodyEnd || 0, nested) ||
	      (inner.kind === 'lambda' && hasReference(scan, inner.name, declaration.bodyStart || 0, declaration.bodyEnd || 0, nested))) {
		      invokedDeclarationCalls(out, seen, scan, source, file, inner, pkg, visited, owner)
	    }
	  }
}

function findCalls(source, file, ranges = []) {
	  const scan = maskKotlinTrivia(source)
	  const pkg = packageName(scan)
	  const out = []
  const seen = new Set()
  for (const range of ranges) {
	    const nested = nestedDeclarationRanges(scan, range.bodyStart || 0, range.bodyEnd || 0)
	    const localTargets = new Set(nested.filter((n) => n.kind === 'fun').map((n) => n.name))
		    pushPascalCalls(out, seen, scan, source, file, range.bodyStart || 0, range.bodyEnd || 0, nested, localTargets, pkg, range.name)
	    for (const declaration of nested) {
	      if (declaration.kind !== 'fun' && declaration.kind !== 'lambda') continue
	      if (hasInvocation(scan, declaration.name, range.bodyStart || 0, range.bodyEnd || 0, nested) ||
	        (declaration.kind === 'lambda' && hasReference(scan, declaration.name, range.bodyStart || 0, range.bodyEnd || 0, nested))) {
		        invokedDeclarationCalls(out, seen, scan, source, file, declaration, pkg, new Set(), range.name)
	      }
	    }
  }
  // W2-3 owner-closure (ONE level, same file): a component invoked inside a private/internal
  // top-level sub-composable carried only that sub-composable as `owner`, so owner-scoped
  // matching in the spec gate could never bind it to the screen (false MISSING_COMPONENT_CALL
  // whenever a screen factors content into DeliveryChannelsCard-style helpers). For each
  // top-level composable P that another top-level composable Q of the SAME file invokes,
  // re-attribute P's calls to Q as closure entries (`ownerVia: P`). One level is deliberate:
  // every production shape is screen → sub-composable; deeper chains keep their direct owner
  // (and can still bind by implementation FILE — the spec gate's other W2-3 leg).
  const rangeNames = new Set(ranges.map((r) => r.name))
  const invokersOf = new Map()
  for (const call of out) {
    if (call.owner && call.callee !== call.owner && rangeNames.has(call.callee)) {
      const set = invokersOf.get(call.callee) || new Set()
      set.add(call.owner)
      invokersOf.set(call.callee, set)
    }
  }
  const closures = []
  for (const call of out) {
    if (!call.owner || call.ownerVia) continue
    const invokers = invokersOf.get(call.owner)
    if (!invokers) continue
    for (const q of invokers) {
      if (q === call.owner) continue
      const key = `closure:${call.line}:${call.callee}:${q}`
      if (seen.has(key)) continue
      seen.add(key)
      closures.push({ ...call, owner: q, ownerVia: call.owner })
    }
  }
  out.push(...closures)
  return out.sort((a, b) => a.line - b.line || a.callee.localeCompare(b.callee) || a.raw.localeCompare(b.raw))
}

function fileKey(path) {
  try { return realpathSync(path) } catch { return resolve(path) }
}

function uniqueFiles(paths) {
  const out = new Map()
  for (const path of paths) {
    if (!SOURCE_EXT.test(path)) continue
    const key = fileKey(path)
    if (!out.has(key)) out.set(key, path)
  }
  return [...out.values()].sort()
}

function tokenPaths(evidence) {
  return Object.keys((evidence && evidence.tokens) || {}).sort()
}

function callResolvedByComposable(call, composables) {
  const matches = composables.filter((c) => c.name === call.callee)
  if (!matches.length) return false
  if (matches.some((c) => c.file === call.file)) return true
  return matches.some((c) => c.package && c.package === call.package && c.visibility !== 'private')
}

function atomicWrite(path, data) {
  writeFigmaRuntimeFile(path, JSON.stringify(data, null, 2) + '\n')
}

async function main() {
  let cli
  try {
    cli = parseCli({
      allowedFlags: ['--out', '--file', '--root'],
      valueFlags: ['--out', '--file', '--root'],
      usage: USAGE,
    })
  } catch (e) {
    console.error(e.message)
    process.exit(1)
  }

  const roots = [
    ...cli.valuesFor('--root'),
    ...(process.env.FIGMA_SPEC_IMPL_ROOTS ? process.env.FIGMA_SPEC_IMPL_ROOTS.split(delimiter).filter(Boolean)
      .map((value) => executionProductInputPath(value, 'FIGMA_SPEC_IMPL_ROOTS')) : []),
  ]
  const files = [
    ...cli.valuesFor('--file'),
    ...(process.env.FIGMA_SPEC_IMPL_FILES ? process.env.FIGMA_SPEC_IMPL_FILES.split(delimiter).filter(Boolean)
      .map((value) => executionProductInputPath(value, 'FIGMA_SPEC_IMPL_FILES')) : []),
  ]
  const out = cli.value('--out') || (process.env.FIGMA_IMPL_MODEL_OUT
    ? executionFigmaOutputPath(process.env.FIGMA_IMPL_MODEL_OUT, 'FIGMA_IMPL_MODEL_OUT') : '')
  if (!out) {
    console.error(USAGE)
    process.exit(1)
  }

  const issues = []
  for (const root of roots) {
    if (pathKind(root) !== 'dir') issues.push(issue('BLOCKER', 'IMPLEMENTATION_ROOT_MISSING', `implementation root is not readable: ${root}`, { file: projectPath(root) }))
  }
  const allFiles = uniqueFiles([...files, ...roots.flatMap((r) => collectFiles(r))])
  const inputHashes = {}
  const modelFiles = []
  const composables = []
  const calls = []
  const tokens = { paths: [], refs: {} }
  const unresolvedRefs = []
  if (!allFiles.length) issues.push(issue('BLOCKER', 'NO_KOTLIN_INPUT', 'pass --file/--root or FIGMA_SPEC_IMPL_FILES/ROOTS'))

  for (const file of allFiles) {
    const relFile = projectPath(file)
    if (!existsSync(file)) {
      issues.push(issue('BLOCKER', 'IMPLEMENTATION_FILE_MISSING', `implementation file is not readable: ${file}`, { file: relFile }))
      continue
    }
    let source = ''
    try { source = readFileSync(file, 'utf8') } catch (e) {
      issues.push(issue('BLOCKER', 'IMPLEMENTATION_FILE_UNREADABLE', e.message, { file: relFile }))
      continue
    }
	    const sourceHash = sha256Text(source)
	    inputHashes[relFile] = sourceHash
    const parsed = parseKotlinSource(source)
    const evidence = extractAppTokensFromText(source, relFile)
    for (const p of tokenPaths(evidence)) {
      if (!tokens.refs[p]) tokens.refs[p] = []
      tokens.refs[p].push(...((evidence.tokens[p] && evidence.tokens[p].refs) || []))
    }
    const fileComposables = findComposables(source, file)
    const publicComposables = fileComposables.map(({ bodyStart, bodyEnd, ...c }) => c)
    const fileCalls = findCalls(source, file, fileComposables)
    composables.push(...publicComposables)
    calls.push(...fileCalls)
    if (parsed.parseErrors.length) {
      unresolvedRefs.push({ file: relFile, kind: 'PARSE_ERROR', count: parsed.parseErrors.length })
      issues.push(issue('WARN', 'KOTLIN_PARSE_ERROR', `${relFile} contains ${parsed.parseErrors.length} parse error node(s)`, { file: relFile }))
    }
    modelFiles.push({
      path: relFile,
	      hash: sourceHash,
	      sourceHash,
      package: packageName(maskKotlinTrivia(source)),
      parseStatus: parsed.parseErrors.length ? 'PARSE_ERROR' : 'OK',
      parseErrors: parsed.parseErrors,
      composables: publicComposables.map((c) => c.name),
      calls: fileCalls.map((c) => c.callee),
    })
	  }

	  for (const call of calls) {
	    if (call.localTarget || COMPOSE_BUILTIN_CALLS.has(call.callee) || callResolvedByComposable(call, composables)) continue
    unresolvedRefs.push({ file: call.file, line: call.line, kind: 'UNRESOLVED_COMPONENT_CALL', callee: call.callee })
  }
  if (unresolvedRefs.some((r) => r.kind === 'UNRESOLVED_COMPONENT_CALL')) {
    issues.push(issue('WARN', 'UNRESOLVED_COMPONENT_CALLS', 'implementation model contains PascalCase calls not resolved by Compose builtins or the declared source files', {
      count: unresolvedRefs.filter((r) => r.kind === 'UNRESOLVED_COMPONENT_CALL').length,
    }))
  }

  tokens.paths = Object.keys(tokens.refs).sort()
  const hasBlocker = issues.some((i) => i.severity === 'BLOCKER' || i.severity === 'ERROR')
  const model = {
    schemaVersion: 1,
    parser: parserInfo(),
    inputs: {
      files: files.map(projectPath),
      roots: roots.map(projectPath),
    },
    inputHashes,
    overall: hasBlocker ? 'BLOCKER' : issues.length ? 'WARN' : 'PASS',
    issues,
    files: modelFiles,
    composables,
    calls,
    tokens,
    unresolvedRefs,
  }
  atomicWrite(out, model)
  console.log(`extract-compose-model: ${model.overall} ${modelFiles.length} file(s), ${composables.length} composable(s), ${calls.length} call(s) -> ${out}`)
  process.exit(hasBlocker ? 1 : 0)
}

main().catch((e) => {
  console.error(`FATAL: ${e.message}`)
  process.exit(1)
})
