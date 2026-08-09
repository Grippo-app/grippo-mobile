// Bounded glob matching over repository-relative POSIX paths. Supports
// literal segments, `*` (within a segment), `?`, `[...]` classes, and `**`
// (any number of whole segments). No brace expansion, no extglobs, no
// negation — the adapter-config schema rejects anything outside this
// grammar, and compileGlob re-rejects it so schema and matcher cannot drift.
const SEGMENT_SPECIALS = /[.+^${}()|\\]/g

function segmentToRegExp(segment) {
  let out = ''
  for (let i = 0; i < segment.length; i++) {
    const ch = segment[i]
    if (ch === '*') { out += '[^/]*'; continue }
    if (ch === '?') { out += '[^/]'; continue }
    if (ch === '[') {
      const close = segment.indexOf(']', i + 1)
      if (close < 0) throw new Error(`glob segment ${JSON.stringify(segment)} has an unterminated character class`)
      const body = segment.slice(i + 1, close)
      if (!body || /[\\\[]/.test(body)) throw new Error(`glob segment ${JSON.stringify(segment)} has an invalid character class`)
      out += '[' + body.replace(/\^/g, '\\^') + ']'
      i = close
      continue
    }
    if (ch === ']') throw new Error(`glob segment ${JSON.stringify(segment)} has an unmatched class terminator`)
    out += ch.replace(SEGMENT_SPECIALS, '\\$&')
  }
  return out
}

export function compileGlob(pattern) {
  const value = String(pattern || '')
  if (!value || value.length > 200 || value.startsWith('/') || value.includes('\\')) {
    throw new Error(`glob pattern ${JSON.stringify(pattern)} is out of contract`)
  }
  const segments = value.split('/')
  if (segments.some((segment) => segment === '' || segment === '.' || segment === '..')) {
    throw new Error(`glob pattern ${JSON.stringify(pattern)} contains empty or dot segments`)
  }
  const parts = segments.map((segment) => {
    if (segment === '**') return '(?:[^/]+/)*[^/]+'
    if (segment.includes('**')) throw new Error(`glob pattern ${JSON.stringify(pattern)} mixes ** into a segment`)
    return segmentToRegExp(segment)
  })
  // `**` as a non-final segment means zero or more whole segments; splice the
  // trailing separator handling accordingly.
  let source = '^'
  for (let i = 0; i < segments.length; i++) {
    const last = i === segments.length - 1
    if (segments[i] === '**') {
      source += last ? '(?:[^/]+(?:/[^/]+)*)' : '(?:[^/]+/)*'
      continue
    }
    source += parts[i] + (last ? '' : '/')
  }
  source += '$'
  const re = new RegExp(source)
  return (candidate) => re.test(candidate)
}

export function compileGlobSet(patterns) {
  const matchers = (patterns || []).map(compileGlob)
  return (candidate) => matchers.some((matcher) => matcher(candidate))
}
