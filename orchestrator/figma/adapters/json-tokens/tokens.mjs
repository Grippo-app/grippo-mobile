// json-tokens adapter: extracts a project token inventory from JSON token
// files (Style-Dictionary-like trees). This is the second built-in adapter
// and the standing proof that the comparator stays provider/language-neutral:
// it shares the exact inventory contract with kotlin-compose and adds zero
// comparator branches.
//
// Supported source shape per configured file: a JSON object tree whose leaves
// are either
//   { "value": "#RRGGBB[AA]" | number | { fontFamily?, fontSize?, ... } }
//   { "value": "{alias.path}" }             — alias to another leaf
// optionally with { "unit": "dp" | ... } beside a numeric value and
//   { "modes": { "<mode>": <value-or-alias> } } instead of "value" for
// mode-dependent leaves. Anything else is an honest unsupported leaf.
// Authority contracts (config authorities.<kind>.contracts) name the dotted
// group paths this adapter treats as that kind's token roots.
export const JSON_TOKENS_EXTRACTOR_VERSION = 'json-tokens-tokens-v1'

const ALIAS_RE = /^\{([A-Za-z0-9_.-]{1,300})\}$/
const HEX_RE = /^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$/
const UNITS = new Set(['dp', 'sp', 'px', 'pt', 'percent', 'em', 'rem', 'unitless'])
const MAX_DEPTH = 16
const ALIAS_DEPTH_MAX = 16

const isRecord = (value) => !!value && typeof value === 'object' && !Array.isArray(value)

function canonicalColor(hex) {
  const body = hex.slice(1).toUpperCase()
  return { kind: 'color', value: '#' + (body.length === 6 ? body + 'FF' : body), colorSpace: 'srgb' }
}

function canonicalOf(kind, rawValue, unit) {
  if (typeof rawValue === 'string' && HEX_RE.test(rawValue)) {
    return kind === 'color' ? { ok: true, value: canonicalColor(rawValue) } : { ok: false, reason: `a color literal under a ${kind} authority` }
  }
  if (typeof rawValue === 'string' && kind === 'string') {
    return rawValue.length <= 16384
      ? { ok: true, value: { kind: 'string', value: rawValue } }
      : { ok: false, reason: 'string value exceeds 16384 characters' }
  }
  if (typeof rawValue === 'boolean') {
    return kind === 'boolean'
      ? { ok: true, value: { kind: 'boolean', value: rawValue } }
      : { ok: false, reason: `a boolean literal under a ${kind} authority` }
  }
  if (typeof rawValue === 'number') {
    if (!Number.isFinite(rawValue) || Object.is(rawValue, -0)) return { ok: false, reason: 'non-finite or negative-zero number' }
    if (kind !== 'dimension') return { ok: false, reason: `a number literal under a ${kind} authority` }
    if (unit !== undefined && !UNITS.has(unit)) return { ok: false, reason: `unknown unit ${JSON.stringify(unit)}` }
    return { ok: true, value: { kind: 'dimension', value: rawValue, unit: unit || 'unitless' } }
  }
  if (isRecord(rawValue) && kind === 'typography') {
    const out = { kind: 'typography' }
    if (rawValue.fontFamily !== undefined) {
      const family = Array.isArray(rawValue.fontFamily) ? rawValue.fontFamily : [rawValue.fontFamily]
      if (!family.length || family.length > 8 || family.some((name) => typeof name !== 'string' || !name || name.length > 120)) {
        return { ok: false, reason: 'fontFamily must be 1..8 bounded strings' }
      }
      out.fontFamily = family
    }
    for (const [field, unitDefault] of [['fontSize', 'sp'], ['lineHeight', 'sp'], ['letterSpacing', 'sp']]) {
      if (rawValue[field] === undefined) continue
      const numeric = rawValue[field]
      if (typeof numeric !== 'number' || !Number.isFinite(numeric)) return { ok: false, reason: `${field} must be a finite number` }
      out[field] = { kind: 'dimension', value: numeric, unit: unitDefault }
    }
    if (rawValue.fontWeight !== undefined) {
      if (typeof rawValue.fontWeight !== 'number' || rawValue.fontWeight < 1 || rawValue.fontWeight > 1000) {
        return { ok: false, reason: 'fontWeight must be a number in 1..1000' }
      }
      out.fontWeight = rawValue.fontWeight
    }
    if (rawValue.textTransform !== undefined) {
      if (!['none', 'uppercase', 'lowercase', 'capitalize'].includes(rawValue.textTransform)) {
        return { ok: false, reason: 'textTransform outside the supported set' }
      }
      out.textTransform = rawValue.textTransform
    }
    if (Object.keys(out).length === 1) return { ok: false, reason: 'typography object carries no supported fields' }
    return { ok: true, value: out }
  }
  return { ok: false, reason: `unsupported leaf value of type ${Array.isArray(rawValue) ? 'array' : typeof rawValue}` }
}

function leafSpec(node) {
  if (!isRecord(node)) return null
  if (node.value !== undefined) return { shared: { raw: node.value, unit: node.unit } }
  if (isRecord(node.modes)) {
    const byMode = {}
    for (const [mode, rawValue] of Object.entries(node.modes)) {
      byMode[mode] = { raw: rawValue, unit: node.unit }
    }
    return byMode
  }
  return null
}

function walkTree(tree, filePath, fileHash, collect, failures) {
  const visit = (node, path, depth) => {
    if (depth > MAX_DEPTH) {
      failures.push({ path: filePath, reason: `token tree deeper than ${MAX_DEPTH} at ${path.join('.')}` })
      return
    }
    if (!isRecord(node)) return
    const spec = leafSpec(node)
    if (spec) {
      collect({ path, spec, filePath, fileHash })
      return
    }
    for (const key of Object.keys(node).sort()) visit(node[key], [...path, key], depth + 1)
  }
  visit(tree, [], 0)
}

// files: [{ path, text, hash }]; tokensConfig: the adapter's tokens section;
// adapterId: namespace for projectTokenIds.
export function extractTokens({ files, tokensConfig, adapterId }) {
  const parseFailures = []
  const limitations = []
  const leaves = new Map()
  for (const file of files) {
    let tree
    try { tree = JSON.parse(file.text) } catch (error) {
      parseFailures.push({ path: file.path, reason: `invalid JSON: ${String(error.message).slice(0, 200)}` })
      continue
    }
    if (!isRecord(tree)) {
      parseFailures.push({ path: file.path, reason: 'token file root must be an object' })
      continue
    }
    walkTree(tree, file.path, file.hash, (leaf) => {
      const dotted = leaf.path.join('.')
      if (!dotted) return
      if (leaves.has(dotted)) {
        parseFailures.push({ path: leaf.filePath, reason: `duplicate token path ${dotted} (also in ${leaves.get(dotted).filePath})` })
        return
      }
      leaves.set(dotted, leaf)
    }, parseFailures)
  }

  const authorities = tokensConfig.authorities
  const kindOfPath = (dotted) => {
    for (const [kind, authority] of Object.entries(authorities)) {
      for (const contract of authority.contracts) {
        if (dotted === contract || dotted.startsWith(contract + '.')) return kind
      }
    }
    return null
  }

  const declaredModes = new Set(tokensConfig.modes)

  const resolveAlias = (dotted, mode, visited) => {
    const leaf = leaves.get(dotted)
    if (!leaf) return { ok: false, reason: `alias target ${dotted} does not exist` }
    if (visited.has(dotted)) return { ok: false, reason: 'alias cycle detected' }
    visited.add(dotted)
    if (visited.size > ALIAS_DEPTH_MAX) return { ok: false, reason: 'alias chain exceeds depth limit' }
    const entry = leaf.spec[mode] || leaf.spec.shared
    if (!entry) return { ok: false, reason: `alias target ${dotted} has no value for mode ${mode}` }
    const aliasMatch = typeof entry.raw === 'string' ? ALIAS_RE.exec(entry.raw) : null
    if (aliasMatch) return resolveAlias(aliasMatch[1], mode, visited)
    const kind = kindOfPath(dotted)
    if (!kind) return { ok: false, reason: `alias target ${dotted} is outside every configured authority` }
    return { ...canonicalOf(kind, entry.raw, entry.unit), targetDotted: dotted }
  }

  const tokens = []
  for (const dotted of [...leaves.keys()].sort()) {
    const kind = kindOfPath(dotted)
    if (!kind) continue
    const leaf = leaves.get(dotted)
    const semanticPath = dotted.split('.')
    const modes = {}
    const edges = []
    const itemLimitations = []
    for (const [mode, entry] of Object.entries(leaf.spec)) {
      if (!declaredModes.has(mode)) {
        itemLimitations.push(`mode ${mode} is not declared in tokens.modes`)
        continue
      }
      const aliasMatch = typeof entry.raw === 'string' ? ALIAS_RE.exec(entry.raw) : null
      const rawExpression = aliasMatch ? entry.raw : JSON.stringify(entry.raw).slice(0, 500)
      const modeEntry = { raw: { expression: rawExpression } }
      if (aliasMatch) {
        const resolved = resolveAlias(aliasMatch[1], mode, new Set([dotted]))
        if (resolved.ok) {
          modeEntry.resolved = resolved.value
          const edge = { kind: 'alias', targetProjectTokenId: `${adapterId}:${resolved.targetDotted}` }
          if (mode !== 'shared') edge.mode = mode
          if (!edges.some((existing) => existing.targetProjectTokenId === edge.targetProjectTokenId && existing.mode === edge.mode)) {
            edges.push(edge)
          }
        } else {
          modeEntry.unsupported = { reason: resolved.reason.slice(0, 300) }
        }
      } else {
        const canonical = canonicalOf(kind, entry.raw, entry.unit)
        if (canonical.ok) modeEntry.resolved = canonical.value
        else modeEntry.unsupported = { reason: canonical.reason.slice(0, 300) }
      }
      modes[mode] = modeEntry
    }
    if (!Object.keys(modes).length) {
      parseFailures.push({ path: leaf.filePath, reason: `token ${dotted} declares only undeclared modes` })
      continue
    }
    tokens.push({
      projectTokenId: `${adapterId}:${dotted}`,
      kind,
      layer: 'semantic-contract',
      semanticPath,
      displayName: dotted,
      modes,
      source: { path: leaf.filePath, symbol: dotted, fileHash: leaf.fileHash },
      edges,
      limitations: itemLimitations.slice(0, 32)
    })
  }
  // Alias edges may point at leaves outside every authority; those targets
  // are not inventory rows, so drop such edges honestly (the resolved value
  // still stands on its own).
  const ids = new Set(tokens.map((token) => token.projectTokenId))
  for (const token of tokens) {
    const kept = token.edges.filter((edge) => ids.has(edge.targetProjectTokenId))
    if (kept.length !== token.edges.length) {
      token.limitations = [...token.limitations, 'alias-target-outside-authorities'].slice(0, 32)
      token.edges = kept
    }
  }
  return { tokens, parseFailures, limitations }
}
