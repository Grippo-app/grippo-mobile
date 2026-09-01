// Canonical Kotlin request-call parser used by project analysis, drift, and
// endpoint suggestions. It is deliberately lexical: comments, documentation,
// string contents, nested calls, and concatenated path expressions must never
// be mistaken for top-level request(method = ..., path = ...) evidence.

const HTTP_METHOD_RE = /^(?:(?:[A-Za-z_][A-Za-z0-9_]*\s*\.\s*)*HttpMethod\s*\.\s*)?(Get|Post|Put|Patch|Delete)$/i

function isIdentifierStart(char) {
  return /[A-Za-z_]/.test(char || '')
}

function isIdentifierPart(char) {
  return /[A-Za-z0-9_]/.test(char || '')
}

function isEscaped(value, index) {
  let slashes = 0
  for (let cursor = index - 1; cursor >= 0 && value[cursor] === '\\'; cursor--) slashes++
  return slashes % 2 === 1
}

function skipLineComment(source, index) {
  index += 2
  while (index < source.length && source[index] !== '\n' && source[index] !== '\r') index++
  return index
}

function skipBlockComment(source, index) {
  let depth = 1
  index += 2
  while (index < source.length && depth > 0) {
    if (source[index] === '/' && source[index + 1] === '*') {
      depth++
      index += 2
    } else if (source[index] === '*' && source[index + 1] === '/') {
      depth--
      index += 2
    } else index++
  }
  return depth === 0 ? index : -1
}

function skipChar(source, index) {
  index++
  while (index < source.length) {
    if (source[index] === '\\') index += 2
    else if (source[index] === "'") return index + 1
    else if (source[index] === '\n' || source[index] === '\r') return -1
    else index++
  }
  return -1
}

function skipTrivia(source, index, end = source.length) {
  while (index < end) {
    if (/\s/.test(source[index])) index++
    else if (source[index] === '/' && source[index + 1] === '/') index = skipLineComment(source, index)
    else if (source[index] === '/' && source[index + 1] === '*') {
      index = skipBlockComment(source, index)
      if (index < 0) return -1
    } else break
  }
  return index
}

function scanBalanced(source, index, open, close) {
  let depth = 1
  index++
  while (index < source.length && depth > 0) {
    if (source[index] === '/' && source[index + 1] === '/') {
      index = skipLineComment(source, index)
    } else if (source[index] === '/' && source[index + 1] === '*') {
      index = skipBlockComment(source, index)
      if (index < 0) return -1
    } else if (source[index] === '"') {
      const scanned = scanString(source, index)
      if (!scanned) return -1
      index = scanned.end
    } else if (source[index] === "'") {
      index = skipChar(source, index)
      if (index < 0) return -1
    } else if (source[index] === open) {
      depth++
      index++
    } else if (source[index] === close) {
      depth--
      index++
    } else index++
  }
  return depth === 0 ? index : -1
}

function scanTypeArguments(source, index) {
  let depth = 1
  index++
  while (index < source.length && depth > 0) {
    if (source[index] === '/' && source[index + 1] === '/') index = skipLineComment(source, index)
    else if (source[index] === '/' && source[index + 1] === '*') {
      index = skipBlockComment(source, index)
      if (index < 0) return -1
    } else if (source[index] === '"') {
      const scanned = scanString(source, index)
      if (!scanned) return -1
      index = scanned.end
    } else if (source[index] === "'") {
      index = skipChar(source, index)
      if (index < 0) return -1
    } else if (source[index] === '<') { depth++; index++ }
    else if (source[index] === '>') { depth--; index++ }
    else index++
  }
  return depth === 0 ? index : -1
}

function scanInterpolation(source, index) {
  return scanBalanced(source, index, '{', '}')
}

function scanString(source, index) {
  const triple = source.slice(index, index + 3) === '"""'
  const contentStart = index + (triple ? 3 : 1)
  index = contentStart
  while (index < source.length) {
    if (source[index] === '$' && source[index + 1] === '{') {
      const next = scanInterpolation(source, index + 1)
      if (next < 0) return null
      index = next
      continue
    }
    if (triple) {
      if (source.slice(index, index + 3) === '"""') {
        return { start: contentStart, endContent: index, end: index + 3, raw: true }
      }
      index++
    } else if (source[index] === '\\') {
      index += 2
    } else if (source[index] === '"') {
      return { start: contentStart, endContent: index, end: index + 1, raw: false }
    } else if (source[index] === '\n' || source[index] === '\r') return null
    else index++
  }
  return null
}

function splitTopLevelArguments(source, start, end) {
  const ranges = []
  let segmentStart = start
  let index = start
  const closers = { '(': ')', '[': ']', '{': '}' }
  while (index < end) {
    if (source[index] === '/' && source[index + 1] === '/') {
      index = skipLineComment(source, index)
    } else if (source[index] === '/' && source[index + 1] === '*') {
      index = skipBlockComment(source, index)
      if (index < 0) return null
    } else if (source[index] === '"') {
      const scanned = scanString(source, index)
      if (!scanned || scanned.end > end) return null
      index = scanned.end
    } else if (source[index] === "'") {
      index = skipChar(source, index)
      if (index < 0 || index > end) return null
    } else if (closers[source[index]]) {
      index = scanBalanced(source, index, source[index], closers[source[index]])
      if (index < 0 || index > end) return null
    } else if (source[index] === ',') {
      ranges.push([segmentStart, index])
      segmentStart = ++index
    } else index++
  }
  ranges.push([segmentStart, end])
  return ranges
}

function namedArgument(source, start, end) {
  let index = skipTrivia(source, start, end)
  if (index < 0 || !isIdentifierStart(source[index])) return null
  const nameStart = index++
  while (index < end && isIdentifierPart(source[index])) index++
  const name = source.slice(nameStart, index)
  index = skipTrivia(source, index, end)
  if (index < 0 || source[index] !== '=') return null
  const valueStart = skipTrivia(source, index + 1, end)
  if (valueStart < 0) return null
  let valueEnd = end
  while (valueEnd > valueStart && /\s/.test(source[valueEnd - 1])) valueEnd--
  return { name, valueStart, valueEnd }
}

function parsePathValue(source, start, end) {
  if (source[start] !== '"') return null
  const scanned = scanString(source, start)
  if (!scanned || scanned.end > end) return null
  const tail = skipTrivia(source, scanned.end, end)
  if (tail !== end) return null
  return {
    value: source.slice(scanned.start, scanned.endContent),
    raw: scanned.raw === true
  }
}

function normalizeInterpolations(value, raw) {
  let out = ''
  for (let index = 0; index < value.length; index++) {
    const char = value[index]
    if (char !== '$' || !raw && isEscaped(value, index)) {
      out += char
      continue
    }
    if (value[index + 1] === '{') {
      const close = scanInterpolation(value, index + 1)
      const expression = close > 0 ? value.slice(index + 2, close - 1).trim() : ''
      if (close > 0 && expression && expression.length <= 500) {
        const literalDollar = /^(?:'\$'|"\$")$/.test(expression)
        out += literalDollar ? '$' : '{}'
        index = close - 1
        continue
      }
    } else {
      const identifier = /^[A-Za-z_][A-Za-z0-9_]*/.exec(value.slice(index + 1))
      if (identifier) {
        out += '{}'
        index += identifier[0].length
        continue
      }
    }
    out += char
  }
  return raw ? out : out.replace(/\\\$/g, '$')
}

function literalRouteAccepted(value) {
  const route = String(value || '')
  if (!route || route !== route.trim() || /[\r\n\t]/.test(route)) return false
  // Relative routes are valid in common wrappers. A leading interpolation is
  // ambiguous (usually a base URL), so it is not exact route evidence unless
  // the path is rooted before that interpolation.
  return /^(?:\/|https?:\/\/)/i.test(route) || route[0] !== '$'
}

export function normalizeRoute(value) {
  let route = String(value || '').trim()
  try {
    if (/^https?:\/\//i.test(route)) route = new URL(route).pathname
  } catch {}
  route = route.replace(/%7B/gi, '{').replace(/%7D/gi, '}').split(/[?#]/)[0]
    .replace(/(^|\/):[A-Za-z_][A-Za-z0-9_]*/g, '$1{}')
    .replace(/\{[^}/]+\}/g, '{}')
  if (!route.startsWith('/')) route = '/' + route
  return route.length > 1 ? route.replace(/\/$/, '') : route
}

export function normalizeKotlinRoute(value, options) {
  return normalizeRoute(normalizeInterpolations(
    String(value || ''), !!(options && options.raw)
  ))
}

export function parseKotlinRequestCandidates(text) {
  const source = String(text || '')
  const out = []
  let index = 0
  while (index < source.length) {
    if (source[index] === '/' && source[index + 1] === '/') {
      index = skipLineComment(source, index)
      continue
    }
    if (source[index] === '/' && source[index + 1] === '*') {
      index = skipBlockComment(source, index)
      if (index < 0) break
      continue
    }
    if (source[index] === '"') {
      const scanned = scanString(source, index)
      index = scanned ? scanned.end : source.length
      continue
    }
    if (source[index] === "'") {
      const next = skipChar(source, index)
      index = next < 0 ? source.length : next
      continue
    }
    if (!isIdentifierStart(source[index])) {
      index++
      continue
    }
    const wordStart = index++
    while (index < source.length && isIdentifierPart(source[index])) index++
    if (source.slice(wordStart, index) !== 'request') continue
    let open = skipTrivia(source, index)
    if (source[open] === '<') {
      open = scanTypeArguments(source, open)
      if (open < 0) continue
      open = skipTrivia(source, open)
    }
    if (open < 0 || source[open] !== '(') continue
    const after = scanBalanced(source, open, '(', ')')
    if (after < 0) continue
    const ranges = splitTopLevelArguments(source, open + 1, after - 1)
    if (!ranges) continue
    let method = null
    let path = null
    for (const range of ranges) {
      const arg = namedArgument(source, range[0], range[1])
      if (!arg) continue
      if (arg.name === 'method') {
        const compact = maskKotlinNonCode(source.slice(arg.valueStart, arg.valueEnd)).trim()
        const match = HTTP_METHOD_RE.exec(compact)
        if (match) method = match[1].toUpperCase()
      } else if (arg.name === 'path') {
        path = parsePathValue(source, arg.valueStart, arg.valueEnd)
      }
    }
    if (method && path !== null && literalRouteAccepted(path.value)) {
      out.push({
        method,
        path: normalizeKotlinRoute(path.value, { raw: path.raw }),
        index: wordStart
      })
    }
  }
  return out
}

export function parseKotlinRequestEndpoints(text) {
  return parseKotlinRequestCandidates(text).map(({ method, path }) => ({ method, path }))
}

function directReceiver(source, wordStart) {
  let index = wordStart - 1
  while (index >= 0 && /\s/.test(source[index])) index--
  if (source[index] !== '.') return { bare: true, name: null }
  index--
  while (index >= 0 && /\s/.test(source[index])) index--
  if (source[index] === '?') {
    index--
    while (index >= 0 && /\s/.test(source[index])) index--
  } else if (source[index] === '!' && source[index - 1] === '!') {
    index -= 2
    while (index >= 0 && /\s/.test(source[index])) index--
  }
  const end = index + 1
  while (index >= 0 && isIdentifierPart(source[index])) index--
  if (end === index + 1) return { bare: false, name: null }
  return { bare: false, name: source.slice(index + 1, end) }
}

// Kotlin/Ktor direct HTTP forms preserved alongside request(...):
//   client/httpClient.get("/route"). Bare get(...) is deliberately excluded:
// without a resolved import it is indistinguishable from a project helper.
// Unknown receivers (cache.get, store.put, etc.) are not HTTP evidence.
export function parseKotlinDirectHttpCandidates(text) {
  const source = String(text || '')
  const out = []
  let index = 0
  while (index < source.length) {
    if (source[index] === '/' && source[index + 1] === '/') {
      index = skipLineComment(source, index)
      continue
    }
    if (source[index] === '/' && source[index + 1] === '*') {
      index = skipBlockComment(source, index)
      if (index < 0) break
      continue
    }
    if (source[index] === '"') {
      const scanned = scanString(source, index)
      index = scanned ? scanned.end : source.length
      continue
    }
    if (source[index] === "'") {
      const next = skipChar(source, index)
      index = next < 0 ? source.length : next
      continue
    }
    if (!isIdentifierStart(source[index])) {
      index++
      continue
    }
    const wordStart = index++
    while (index < source.length && isIdentifierPart(source[index])) index++
    const methodMatch = /^(get|post|put|patch|delete)$/i.exec(source.slice(wordStart, index))
    if (!methodMatch) continue
    const receiver = directReceiver(source, wordStart)
    if (receiver.bare || !/^(?:client|http|httpClient|apiClient|ktorClient)$/i.test(receiver.name || '')) {
      continue
    }
    let open = skipTrivia(source, index)
    if (source[open] === '<') {
      open = scanTypeArguments(source, open)
      if (open < 0) continue
      open = skipTrivia(source, open)
    }
    if (open < 0 || source[open] !== '(') continue
    const after = scanBalanced(source, open, '(', ')')
    if (after < 0) continue
    const ranges = splitTopLevelArguments(source, open + 1, after - 1)
    if (!ranges || !ranges.length) continue
    let path = null
    let ambiguousPath = false
    for (const range of ranges) {
      const named = namedArgument(source, range[0], range[1])
      if (!named || !['path', 'url', 'uri', 'urlString'].includes(named.name)) continue
      const candidatePath = parsePathValue(source, named.valueStart, named.valueEnd)
      if (!candidatePath || path !== null) { ambiguousPath = true; break }
      path = candidatePath
    }
    if (ambiguousPath) continue
    if (path === null) {
      const first = ranges[0]
      const named = namedArgument(source, first[0], first[1])
      if (named) continue
      const start = skipTrivia(source, first[0], first[1])
      if (start >= 0) path = parsePathValue(source, start, first[1])
    }
    if (path !== null && literalRouteAccepted(path.value)) {
      out.push({
        method: methodMatch[1].toUpperCase(),
        path: normalizeKotlinRoute(path.value, { raw: path.raw }),
        index: wordStart,
      })
    }
  }
  return out
}

// One client-call truth for every API consumer. Keep server annotations out of
// this union: they are implementation evidence for the project analyzer, but
// they must not make a selected mobile API client look covered in diff/suggest.
export function parseKotlinClientCandidates(text) {
  return parseKotlinRequestCandidates(text)
    .map((candidate) => ({ ...candidate, kind: 'request' }))
    .concat(parseKotlinDirectHttpCandidates(text)
      .map((candidate) => ({ ...candidate, kind: 'direct' })))
    .sort((left, right) => left.index - right.index)
}

export function parseKotlinClientEndpoints(text) {
  return parseKotlinClientCandidates(text)
    .map(({ method, path }) => ({ method, path }))
}

export function maskKotlinNonCode(text) {
  const source = String(text || '')
  const out = source.split('')
  const mask = (start, end) => {
    for (let cursor = start; cursor < end; cursor++) {
      if (out[cursor] !== '\n' && out[cursor] !== '\r') out[cursor] = ' '
    }
  }
  let index = 0
  while (index < source.length) {
    let end = index
    if (source[index] === '/' && source[index + 1] === '/') end = skipLineComment(source, index)
    else if (source[index] === '/' && source[index + 1] === '*') {
      end = skipBlockComment(source, index)
      if (end < 0) end = source.length
    } else if (source[index] === '"') {
      const scanned = scanString(source, index)
      end = scanned ? scanned.end : source.length
    } else if (source[index] === "'") {
      end = skipChar(source, index)
      if (end < 0) end = source.length
    }
    if (end > index) {
      mask(index, end)
      index = end
    } else index++
  }
  return out.join('')
}

// Evidence indexes need string literals (routes/operation ids) but must not see
// comment contents. This variant keeps strings and exact offsets while using
// Kotlin's nested block-comment rules.
export function maskKotlinComments(text) {
  const source = String(text || '')
  const out = source.split('')
  const mask = (start, end) => {
    for (let cursor = start; cursor < end; cursor++) {
      if (out[cursor] !== '\n' && out[cursor] !== '\r') out[cursor] = ' '
    }
  }
  let index = 0
  while (index < source.length) {
    if (source[index] === '/' && source[index + 1] === '/') {
      const end = skipLineComment(source, index)
      mask(index, end)
      index = end
    } else if (source[index] === '/' && source[index + 1] === '*') {
      let end = skipBlockComment(source, index)
      if (end < 0) end = source.length
      mask(index, end)
      index = end
    } else if (source[index] === '"') {
      const scanned = scanString(source, index)
      index = scanned ? scanned.end : source.length
    } else if (source[index] === "'") {
      const end = skipChar(source, index)
      index = end < 0 ? source.length : end
    } else index++
  }
  return out.join('')
}

export function parseKotlinAnnotationCandidates(text) {
  const source = String(text || '')
  const out = []
  let index = 0
  while (index < source.length) {
    if (source[index] === '/' && source[index + 1] === '/') {
      index = skipLineComment(source, index)
      continue
    }
    if (source[index] === '/' && source[index + 1] === '*') {
      index = skipBlockComment(source, index)
      if (index < 0) break
      continue
    }
    if (source[index] === '"') {
      const scanned = scanString(source, index)
      index = scanned ? scanned.end : source.length
      continue
    }
    if (source[index] === "'") {
      const next = skipChar(source, index)
      index = next < 0 ? source.length : next
      continue
    }
    if (source[index] !== '@') {
      index++
      continue
    }
    const annotationIndex = index++
    if (!isIdentifierStart(source[index])) continue
    let rawName = ''
    while (isIdentifierStart(source[index])) {
      const nameStart = index++
      while (index < source.length && isIdentifierPart(source[index])) index++
      rawName = source.slice(nameStart, index)
      const dot = skipTrivia(source, index)
      if (source[dot] !== '.') break
      index = skipTrivia(source, dot + 1)
    }
    const match = /^(Get|Post|Put|Patch|Delete)(?:Mapping)?$/i.exec(rawName)
    if (!match) continue
    const open = skipTrivia(source, index)
    if (open < 0 || source[open] !== '(') continue
    const after = scanBalanced(source, open, '(', ')')
    if (after < 0) continue
    const ranges = splitTopLevelArguments(source, open + 1, after - 1)
    if (!ranges || !ranges.length) continue
    let path = null
    for (const range of ranges) {
      const named = namedArgument(source, range[0], range[1])
      if (named && (named.name === 'value' || named.name === 'path')) {
        path = parsePathValue(source, named.valueStart, named.valueEnd)
      } else if (!named && path === null) {
        const valueStart = skipTrivia(source, range[0], range[1])
        path = valueStart < 0 ? null : parsePathValue(source, valueStart, range[1])
      }
      if (path !== null) break
    }
    if (path !== null && literalRouteAccepted(path.value)) {
      out.push({
        method: match[1].toUpperCase(),
        path: normalizeKotlinRoute(path.value, { raw: path.raw }),
        index: annotationIndex
      })
    }
  }
  return out
}

export function sameRouteShape(left, right) {
  return normalizeRoute(left) === normalizeRoute(right)
}

export function isTestSourcePath(sourcePath) {
  const parts = String(sourcePath || '').split('/').filter(Boolean)
  const file = parts[parts.length - 1] || ''
  return parts.slice(0, -1).some((part) =>
    /^(?:test|tests|__tests__|testFixtures)$/i.test(part) || /Test(?:s)?$/.test(part)) ||
    /(?:^|[._-])(?:test|spec)\.[^.]+$/i.test(file) ||
    /(?:Test|Tests)\.(?:kt|kts|swift|java|cs)$/i.test(file)
}

export function selectKotlinApiRecord(records, apiClassName) {
  if (!apiClassName) return { ok: true, record: null }
  const suffix = '/' + apiClassName + '.kt'
  const candidates = (Array.isArray(records) ? records : []).filter((record) =>
    record && typeof record.path === 'string' && record.path.endsWith(suffix) &&
    record.path.split('/').includes('data-services') && !isTestSourcePath(record.path))
  if (candidates.length > 1) {
    return { ok: false, error: 'api-client-file-ambiguous', paths: candidates.map((row) => row.path).sort() }
  }
  return { ok: true, record: candidates[0] || null }
}
