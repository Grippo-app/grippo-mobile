// Canonical JSON serialization + content hashing shared by every token/component
// domain producer and consumer. One owner for determinism: object keys are
// serialized sorted, arrays keep author order (order is semantic in every
// contract that uses them), and only JSON-safe primitives are accepted —
// anything else is a programming error, never coerced.
import { createHash } from 'node:crypto'

// Locale/ICU-independent ordering for every semantic array. JavaScript's
// relational string comparison is defined over UTF-16 code units and does
// not consult the host locale; localeCompare() does and is therefore not a
// valid canonicalization primitive.
export function compareText(left, right) {
  const a = String(left)
  const b = String(right)
  return a < b ? -1 : a > b ? 1 : 0
}

export function canonicalJson(value) {
  return serialize(value, 0)
}

function serialize(value, depth) {
  if (depth > 64) throw new Error('canonical-json depth limit exceeded')
  if (value === null) return 'null'
  const type = typeof value
  if (type === 'string') return JSON.stringify(value)
  if (type === 'boolean') return value ? 'true' : 'false'
  if (type === 'number') {
    if (!Number.isFinite(value)) throw new Error('canonical-json rejects non-finite numbers')
    if (Object.is(value, -0)) throw new Error('canonical-json rejects negative zero')
    return JSON.stringify(value)
  }
  if (Array.isArray(value)) {
    return '[' + value.map((item) => serialize(item, depth + 1)).join(',') + ']'
  }
  if (type === 'object') {
    const keys = Object.keys(value).sort(compareText)
    const parts = []
    for (const key of keys) {
      const entry = value[key]
      if (entry === undefined) continue
      parts.push(JSON.stringify(key) + ':' + serialize(entry, depth + 1))
    }
    return '{' + parts.join(',') + '}'
  }
  throw new Error(`canonical-json rejects value of type ${type}`)
}

export function canonicalHash(value) {
  return 'sha256:' + createHash('sha256').update(canonicalJson(value), 'utf8').digest('hex')
}

export function bytesHash(bytes) {
  return 'sha256:' + createHash('sha256').update(bytes).digest('hex')
}
