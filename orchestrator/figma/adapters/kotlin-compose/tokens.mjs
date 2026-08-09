// kotlin-compose adapter: extracts a project token inventory from Kotlin
// Compose design-system sources (FIGMA_TOKENS.md §12). Tree-sitter syntax
// facts + a bounded whitelist resolver — never Gradle, never execution, never
// a regex fallback over a failed parse. Anything outside the whitelist stays
// `unsupported` with a reason; a guessed value is a contract violation.
//
// Pure extractor (REQ-ADAPT-001 contract): no fs/env/network/clock; the same
// { files, tokensConfig, adapterId } input serializes to the same bytes —
// files are re-sorted by path, every emitted collection is sorted, and every
// string is bounded.
import { parseKotlinSource } from '../../scripts/compose-model/parser-tree-sitter.mjs'
import { TOKEN_LIMITS } from '../../tokens/limits.mjs'

export const KOTLIN_COMPOSE_EXTRACTOR_VERSION = 'kotlin-compose-tokens-v1'

const REASON_MAX = 300
const EXPRESSION_MAX = 500
const LIMITATION_MAX = 200
const EDGE_DETAIL_MAX = 200
const SEMANTIC_SEGMENT_MAX = 120
const SEMANTIC_PATH_MAX = 16
const DISPLAY_NAME_MAX = 300
const ADAPTER_LIMITATIONS_MAX = 64
// Mirrors project-token-inventory.schema.json projectTokenId; identity that
// cannot be represented is dropped with a limitation, never truncated.
const PROJECT_TOKEN_ID_RE = /^[a-z0-9][a-z0-9-]{0,63}:[A-Za-z0-9_.$/#[\]-]{1,335}$/
const AUTHORITY_KINDS = new Set(['color', 'dimension', 'typography', 'string', 'boolean'])
const FONT_WEIGHTS = Object.freeze({
  Thin: 100, ExtraLight: 200, Light: 300, Normal: 400, Medium: 500,
  SemiBold: 600, Bold: 700, ExtraBold: 800, Black: 900
})
const DIMENSION_UNITS = Object.freeze({ dp: 'dp', sp: 'sp' })

const collapse = (text) => String(text).replace(/\s+/g, ' ').trim()
const boundReason = (text) => collapse(text).slice(0, REASON_MAX)
const boundLimitation = (text) => String(text).slice(0, LIMITATION_MAX)
const boundExpression = (text) => {
  const collapsed = collapse(text).slice(0, EXPRESSION_MAX)
  return collapsed || '<empty>'
}
const stringCompare = (a, b) => (a < b ? -1 : a > b ? 1 : 0)
const fail = (reason) => ({ ok: false, reason: boundReason(reason) })

const textOf = (source, node) => source.slice(node.startIndex, node.endIndex)

function namedChildOfType(node, type) {
  for (let i = 0; i < node.namedChildCount; i++) {
    const child = node.namedChild(i)
    if (child.type === type) return child
  }
  return null
}

function hasAnonymousChild(node, type) {
  for (let i = 0; i < node.childCount; i++) {
    const child = node.child(i)
    if (!child.isNamed && child.type === type) return true
  }
  return false
}

function packageNameOf(root, source) {
  const header = namedChildOfType(root, 'package_header')
  if (!header) return ''
  for (let i = 0; i < header.namedChildCount; i++) {
    const child = header.namedChild(i)
    if (child.type === 'qualified_identifier' || child.type === 'identifier') {
      return textOf(source, child).replace(/\s+/g, '')
    }
  }
  return ''
}

function containerNameOf(node, source) {
  const identifier = namedChildOfType(node, 'identifier')
  if (identifier) return textOf(source, identifier)
  return node.type === 'companion_object' ? 'Companion' : null
}

// Syntax facts of one property_declaration. The initializer is the named
// child after the property-level '=' token; getter/delegate value sources are
// recorded but never computed (§12.3).
function propertyFacts(node, source) {
  let variableDeclaration = null
  let initializer = null
  let hasGetter = false
  let hasDelegate = false
  let isOverride = false
  let seenAssign = false
  for (let i = 0; i < node.childCount; i++) {
    const child = node.child(i)
    if (!child.isNamed) {
      if (child.type === '=') seenAssign = true
      continue
    }
    if (child.type === 'modifiers') {
      for (let j = 0; j < child.namedChildCount; j++) {
        if (textOf(source, child.namedChild(j)) === 'override') isOverride = true
      }
    } else if (child.type === 'variable_declaration' && !variableDeclaration) {
      variableDeclaration = child
    } else if (child.type === 'getter') {
      hasGetter = true
    } else if (child.type === 'setter') {
      continue
    } else if (child.type === 'property_delegate') {
      hasDelegate = true
    } else if (seenAssign && !initializer) {
      initializer = child
    }
  }
  if (!variableDeclaration) return null
  let name = null
  let typeText = null
  for (let i = 0; i < variableDeclaration.namedChildCount; i++) {
    const child = variableDeclaration.namedChild(i)
    if (child.type === 'identifier' && name === null) name = textOf(source, child)
    else if (child.type === 'user_type' || child.type === 'nullable_type') typeText = collapse(textOf(source, child))
  }
  if (!name) return null
  return { name, typeText, isOverride, initializer, hasGetter, hasDelegate }
}

// Deterministic tie-break for duplicate FQ declarations (e.g. KMP source
// sets): the lexicographically smallest (path, line, column) wins so input
// file order can never change the output.
function preferDeclaration(existing, candidate) {
  if (existing.path !== candidate.path) return existing.path < candidate.path ? existing : candidate
  if (existing.line !== candidate.line) return existing.line < candidate.line ? existing : candidate
  return existing.column <= candidate.column ? existing : candidate
}

function indexFile(file, parsed, index, adapterLimitations) {
  const source = file.text
  const pkg = packageNameOf(parsed.root, source)
  const fqOf = (segments) => (pkg ? pkg + '.' : '') + segments.join('.')

  const recordContainer = (segments, kind, node) => {
    const fq = fqOf(segments)
    const record = {
      fq, kind, name: segments[segments.length - 1],
      path: file.path, line: node.startPosition.row + 1, column: node.startPosition.column
    }
    const existing = index.containersByFq.get(fq)
    index.containersByFq.set(fq, existing ? preferDeclaration(existing, record) : record)
  }

  const recordVal = (segments, facts, node) => {
    const fq = fqOf(segments)
    const containerFq = fqOf(segments.slice(0, -1))
    const record = {
      fq, name: facts.name, containerFq,
      path: file.path, fileHash: file.hash, text: source,
      line: node.startPosition.row + 1, column: node.startPosition.column,
      isOverride: facts.isOverride, typeText: facts.typeText,
      initializer: facts.initializer, hasGetter: facts.hasGetter, hasDelegate: facts.hasDelegate,
      propertyNode: node
    }
    const existing = index.valsByFq.get(fq)
    if (existing) {
      adapterLimitations.add(boundLimitation(`duplicate-declaration:${fq}`))
    }
    const chosen = existing ? preferDeclaration(existing, record) : record
    index.valsByFq.set(fq, chosen)
    let byName = index.valsByContainer.get(containerFq)
    if (!byName) {
      byName = new Map()
      index.valsByContainer.set(containerFq, byName)
    }
    byName.set(facts.name, chosen)
  }

  const walkBody = (bodyNode, chain) => {
    if (chain.length > TOKEN_LIMITS.symbolGraphDepthMax) {
      adapterLimitations.add('limit:symbol-graph-depth')
      return
    }
    for (let i = 0; i < bodyNode.namedChildCount; i++) {
      const child = bodyNode.namedChild(i)
      if (child.type === 'object_declaration' || child.type === 'class_declaration' || child.type === 'companion_object') {
        const name = containerNameOf(child, source)
        if (!name) continue
        const kind = child.type === 'class_declaration'
          ? (hasAnonymousChild(child, 'interface') ? 'interface' : 'class')
          : 'object'
        const nextChain = [...chain, name]
        recordContainer(nextChain, kind, child)
        const body = namedChildOfType(child, 'class_body')
        if (body) walkBody(body, nextChain)
      } else if (child.type === 'property_declaration') {
        const facts = propertyFacts(child, source)
        if (!facts) continue
        if (facts.initializer && facts.initializer.type === 'object_literal') {
          // `override val group = object : Contract.Group { ... }` exposes a
          // nested value group: the val is a container segment, not a value.
          const nextChain = [...chain, facts.name]
          recordContainer(nextChain, 'val-group', child)
          const body = namedChildOfType(facts.initializer, 'class_body')
          if (body) walkBody(body, nextChain)
        } else {
          recordVal([...chain, facts.name], facts, child)
        }
      }
    }
  }
  walkBody(parsed.root, [])
}

const isAbstractVal = (decl) => !decl.initializer && !decl.hasGetter && !decl.hasDelegate

function parseNumericLiteral(text) {
  const cleaned = String(text).replace(/_/g, '').replace(/[fFL]$/, '')
  if (!cleaned) return null
  const value = Number(cleaned)
  return Number.isFinite(value) ? value : null
}

function flattenNavigation(node, source) {
  if (node.type === 'identifier') return [textOf(source, node)]
  if (node.type !== 'navigation_expression' || node.namedChildCount !== 2) return null
  const left = flattenNavigation(node.namedChild(0), source)
  const right = node.namedChild(1)
  if (!left || right.type !== 'identifier') return null
  return [...left, textOf(source, right)]
}

const argumentIsNamed = (argNode) => hasAnonymousChild(argNode, '=')

function argumentParts(argNode, source) {
  if (!argumentIsNamed(argNode)) return null
  if (argNode.namedChildCount < 2) return null
  const nameNode = argNode.namedChild(0)
  if (nameNode.type !== 'identifier') return null
  return { name: textOf(source, nameNode), valueNode: argNode.namedChild(argNode.namedChildCount - 1) }
}

function resolveColorCall(args, source) {
  if (args.length !== 1 || argumentIsNamed(args[0])) {
    return fail('Color() requires exactly one positional 6- or 8-digit hex literal argument')
  }
  const literal = args[0].namedChildCount === 1 ? args[0].namedChild(0) : null
  if (!literal || (literal.type !== 'number_literal' && literal.type !== 'integer_literal')) {
    return fail('Color() argument is not a 6- or 8-digit hex literal')
  }
  const digits = /^0[xX]([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$/.exec(textOf(source, literal).replace(/_/g, ''))
  if (!digits) return fail('Color() argument is not a 6- or 8-digit hex literal')
  const body = digits[1].toUpperCase()
  // Kotlin writes AARRGGBB; the canonical value is #RRGGBBAA.
  const value = body.length === 8 ? '#' + body.slice(2) + body.slice(0, 2) : '#' + body + 'FF'
  return { ok: true, value: { kind: 'color', value, colorSpace: 'srgb' } }
}

function fontWeightOf(valueNode, source) {
  const chain = flattenNavigation(valueNode, source)
  if (!chain || chain.length !== 2 || chain[0] !== 'FontWeight') return null
  if (Object.prototype.hasOwnProperty.call(FONT_WEIGHTS, chain[1])) return FONT_WEIGHTS[chain[1]]
  const numbered = /^W([1-9]00)$/.exec(chain[1])
  return numbered ? Number(numbered[1]) : null
}

function resolveCopy(receiverNode, args, source, context, visited, depth) {
  const parts = args.length === 1 ? argumentParts(args[0], source) : null
  if (!parts || parts.name !== 'alpha') {
    return fail('.copy() supports only a single named literal alpha argument')
  }
  if (parts.valueNode.type !== 'float_literal' && parts.valueNode.type !== 'number_literal') {
    return fail('.copy(alpha) requires a numeric literal in 0..1')
  }
  const alpha = parseNumericLiteral(textOf(source, parts.valueNode))
  if (alpha === null || alpha < 0 || alpha > 1) {
    return fail('.copy(alpha) requires a numeric literal in 0..1')
  }
  const receiver = resolveExpression(receiverNode, source, context, new Set(visited), depth + 1)
  if (!receiver.ok) return fail(`.copy() receiver: ${receiver.reason}`)
  if (receiver.value.kind !== 'color') {
    return fail(`.copy(alpha) receiver resolved to ${receiver.value.kind}, expected color`)
  }
  const alphaHex = Math.round(alpha * 255).toString(16).toUpperCase().padStart(2, '0')
  const result = {
    ok: true,
    value: { kind: 'color', value: '#' + receiver.value.value.slice(1, 7) + alphaHex, colorSpace: 'srgb' }
  }
  if (receiver.refFq) result.transform = { targetFq: receiver.refFq, detail: `copy(alpha=${alpha})` }
  return result
}

function resolveTextStyle(args, source, context, visited, depth) {
  const fields = {}
  const tokenLimitations = []
  for (const arg of args) {
    const parts = argumentParts(arg, source)
    if (!parts) return fail('TextStyle positional arguments are not supported')
    if (parts.name === 'fontSize' || parts.name === 'lineHeight' || parts.name === 'letterSpacing') {
      const resolved = resolveExpression(parts.valueNode, source, context, new Set(visited), depth + 1)
      if (!resolved.ok) return fail(`TextStyle argument ${parts.name}: ${resolved.reason}`)
      if (resolved.value.kind !== 'dimension') {
        return fail(`TextStyle argument ${parts.name} resolved to ${resolved.value.kind}, expected dimension`)
      }
      fields[parts.name] = resolved.value
    } else if (parts.name === 'fontWeight') {
      const weight = fontWeightOf(parts.valueNode, source)
      if (weight === null) return fail('TextStyle argument fontWeight must be a known FontWeight constant')
      fields.fontWeight = weight
    } else {
      tokenLimitations.push(boundLimitation(`typography-field-unsupported:${parts.name}`))
    }
  }
  const value = { kind: 'typography' }
  if (fields.fontSize !== undefined) value.fontSize = fields.fontSize
  if (fields.fontWeight !== undefined) value.fontWeight = fields.fontWeight
  if (fields.lineHeight !== undefined) value.lineHeight = fields.lineHeight
  if (fields.letterSpacing !== undefined) value.letterSpacing = fields.letterSpacing
  return { ok: true, value, tokenLimitations }
}

function resolveReference(referenceText, context, visited, depth) {
  let target = context.valsByFq.get(referenceText) || null
  if (!target) {
    const suffix = '.' + referenceText
    const matches = context.resolvableValFqs.filter((fq) => fq.endsWith(suffix))
    if (matches.length === 0) return fail(`unresolved reference ${referenceText}`)
    if (matches.length > 1) return fail(`ambiguous reference ${referenceText} (${matches.length} candidates); short-name fallback is forbidden`)
    target = context.valsByFq.get(matches[0])
  }
  if (visited.has(target.fq)) return fail('alias cycle')
  const nextVisited = new Set(visited)
  nextVisited.add(target.fq)
  const inner = resolveDeclarationValue(target, context, nextVisited, depth + 1)
  if (!inner.ok) return fail(`reference ${referenceText}: ${inner.reason}`)
  // The edge targets the direct hop only; the value comes from the full chain.
  return { ok: true, value: inner.value, refFq: target.fq }
}

function resolveCall(node, source, context, visited, depth) {
  if (namedChildOfType(node, 'annotated_lambda')) {
    return fail('function call with a lambda argument is not statically resolvable')
  }
  const callee = node.namedChild(0)
  const argsNode = namedChildOfType(node, 'value_arguments')
  const args = []
  if (argsNode) {
    for (let i = 0; i < argsNode.namedChildCount; i++) {
      const child = argsNode.namedChild(i)
      if (child.type === 'value_argument') args.push(child)
    }
  }
  if (callee.type === 'identifier') {
    const name = textOf(source, callee)
    if (name === 'Color') return resolveColorCall(args, source)
    if (name === 'TextStyle') return resolveTextStyle(args, source, context, visited, depth)
    return fail(`function call ${name}(...) is not statically resolvable`)
  }
  if (callee.type === 'navigation_expression' && callee.namedChildCount === 2 &&
      callee.namedChild(1).type === 'identifier' && textOf(source, callee.namedChild(1)) === 'copy') {
    return resolveCopy(callee.namedChild(0), args, source, context, visited, depth)
  }
  return fail(`function call ${collapse(textOf(source, node)).slice(0, 80)} is not statically resolvable`)
}

function resolveExpression(node, source, context, visited, depth) {
  if (depth > TOKEN_LIMITS.symbolGraphDepthMax) {
    return fail(`alias chain exceeds depth limit ${TOKEN_LIMITS.symbolGraphDepthMax}`)
  }
  const type = node.type
  if (type === 'string_literal') {
    const raw = textOf(source, node)
    if (!raw.startsWith('"') || !raw.endsWith('"') || raw.startsWith('"""')) {
      return fail('only ordinary non-interpolated Kotlin string literals are supported')
    }
    try {
      const value = JSON.parse(raw)
      if (typeof value !== 'string' || value.length > 16384) {
        return fail('string literal is not a bounded string')
      }
      return { ok: true, value: { kind: 'string', value } }
    } catch (error) {
      return fail('interpolated or non-JSON-compatible Kotlin string literal is not statically resolvable')
    }
  }
  if (type === 'call_expression') return resolveCall(node, source, context, visited, depth)
  if (type === 'navigation_expression') {
    const first = node.namedChild(0)
    const last = node.namedChildCount === 2 ? node.namedChild(1) : null
    if ((first.type === 'number_literal' || first.type === 'float_literal') && last && last.type === 'identifier') {
      const member = textOf(source, last)
      const unit = DIMENSION_UNITS[member]
      if (!unit) return fail(`numeric member .${member} is not a supported unit (dp/sp only)`)
      const value = parseNumericLiteral(textOf(source, first))
      if (value === null) return fail(`numeric literal ${collapse(textOf(source, first)).slice(0, 40)} is not resolvable`)
      return { ok: true, value: { kind: 'dimension', value, unit } }
    }
    const chain = flattenNavigation(node, source)
    if (!chain) return fail(`expression ${collapse(textOf(source, node)).slice(0, 80)} is not statically resolvable`)
    if (chain.length === 2 && chain[0] === 'FontWeight') {
      return fail('FontWeight constants are only supported inside TextStyle')
    }
    if (chain.length === 2 && chain[0] === 'Color') {
      return fail(`named Compose color ${chain.join('.')} is not whitelisted`)
    }
    return resolveReference(chain.join('.'), context, visited, depth)
  }
  if (type === 'identifier') {
    const identifier = textOf(source, node)
    if (identifier === 'true' || identifier === 'false') {
      return { ok: true, value: { kind: 'boolean', value: identifier === 'true' } }
    }
    return resolveReference(identifier, context, visited, depth)
  }
  if (type === 'if_expression' || type === 'when_expression') {
    return fail('conditional expression is not statically resolvable')
  }
  if (type === 'binary_expression' || type === 'range_expression' || type === 'prefix_expression') {
    return fail('arithmetic or binary expression is not statically resolvable')
  }
  return fail(`unsupported expression kind ${type}`)
}

function resolveDeclarationValue(decl, context, visited, depth) {
  if (!decl.initializer) {
    if (decl.hasDelegate) return fail('property delegate (by ...) is not statically resolvable')
    if (decl.hasGetter) return fail('property getter is not statically resolvable')
    return fail(`declaration ${decl.fq} has no initializer`)
  }
  return resolveExpression(decl.initializer, decl.text, context, visited, depth)
}

const edgeCompare = (a, b) =>
  stringCompare(a.kind, b.kind) ||
  stringCompare(a.targetProjectTokenId, b.targetProjectTokenId) ||
  stringCompare(a.mode || '', b.mode || '')

// files: [{ path, text, hash }]; tokensConfig: the adapter's tokens section
// (roots/include/exclude/modes/authorities); adapterId namespaces every
// projectTokenId. See the module header for the purity/determinism contract.
export function extractTokens({ files, tokensConfig, adapterId }) {
  const parseFailures = []
  const adapterLimitations = new Set()
  const index = { valsByFq: new Map(), valsByContainer: new Map(), containersByFq: new Map() }

  const orderedFiles = [...files].sort((a, b) => stringCompare(a.path, b.path))
  // Parsed trees must stay referenced while declaration nodes are in use.
  const retainedTrees = []
  for (const file of orderedFiles) {
    const parsed = parseKotlinSource(file.text)
    retainedTrees.push(parsed)
    if (parsed.hasError) {
      const first = parsed.parseErrors[0]
      const where = first ? `line ${first.start.row + 1}:${first.start.column}` : 'unknown location'
      const near = first && first.text ? ` near ${JSON.stringify(collapse(first.text).slice(0, 60))}` : ''
      parseFailures.push({ path: file.path, reason: boundReason(`kotlin parse error at ${where}${near}`) })
      continue
    }
    indexFile(file, parsed, index, adapterLimitations)
  }

  const context = {
    valsByFq: index.valsByFq,
    // Reference candidates are declarations that carry a value source;
    // abstract contract slots can never be a resolution target.
    resolvableValFqs: [...index.valsByFq.keys()].filter((fq) => !isAbstractVal(index.valsByFq.get(fq))).sort()
  }

  const declaredModes = Array.isArray(tokensConfig.modes) ? tokensConfig.modes : []
  const declaredModesSet = new Set(declaredModes)
  const authorities = tokensConfig.authorities && typeof tokensConfig.authorities === 'object'
    ? tokensConfig.authorities
    : {}

  const plans = []
  const emittedByFoldedId = new Map()
  const declFqToTokenId = new Map()
  const consumedDeclFqs = new Set()

  const addPlan = (plan) => {
    const pathOk = plan.semanticPath.length >= 1 &&
      plan.semanticPath.length <= SEMANTIC_PATH_MAX &&
      plan.semanticPath.every((segment) => segment && segment.length <= SEMANTIC_SEGMENT_MAX) &&
      plan.semanticPath.join('.').length <= DISPLAY_NAME_MAX
    if (!pathOk || !PROJECT_TOKEN_ID_RE.test(plan.id)) {
      adapterLimitations.add(boundLimitation(`token-identity-out-of-bounds:${plan.id.slice(0, 150)}`))
      return false
    }
    const folded = plan.id.toLowerCase()
    if (emittedByFoldedId.has(folded)) {
      adapterLimitations.add(boundLimitation(`duplicate-token-identity:${plan.id.slice(0, 150)}`))
      return false
    }
    emittedByFoldedId.set(folded, plan.id)
    plans.push(plan)
    return true
  }

  const valFqsUnder = (containerFq) =>
    [...index.valsByFq.keys()].filter((fq) => fq.startsWith(containerFq + '.')).sort()

  const collectContractSlots = (contractFq) => {
    const nestedInterfaces = new Map()
    const sortedContainerFqs = [...index.containersByFq.keys()].sort()
    for (const fq of sortedContainerFqs) {
      if (!fq.startsWith(contractFq + '.')) continue
      const container = index.containersByFq.get(fq)
      if (container.kind !== 'interface') continue
      const list = nestedInterfaces.get(container.name) || []
      list.push(fq)
      nestedInterfaces.set(container.name, list)
    }
    const slots = []
    const usedInterfaces = new Set()
    const walkGroup = (groupFq, relativePath, visitedInterfaces) => {
      if (relativePath.length + 2 > SEMANTIC_PATH_MAX || visitedInterfaces.size > TOKEN_LIMITS.symbolGraphDepthMax) {
        adapterLimitations.add('limit:symbol-graph-depth')
        return
      }
      const direct = index.valsByContainer.get(groupFq)
      if (!direct) return
      for (const name of [...direct.keys()].sort()) {
        const decl = direct.get(name)
        if (!isAbstractVal(decl)) continue
        const typeSimple = decl.typeText
          ? decl.typeText.replace(/<.*$/, '').replace(/\?$/, '').split('.').pop().trim()
          : null
        const interfaceFqs = typeSimple ? nestedInterfaces.get(typeSimple) : null
        if (interfaceFqs && interfaceFqs.length === 1) {
          if (visitedInterfaces.has(interfaceFqs[0])) {
            adapterLimitations.add(boundLimitation(`contract-group-cycle:${groupFq}.${name}`))
            continue
          }
          usedInterfaces.add(interfaceFqs[0])
          walkGroup(interfaceFqs[0], [...relativePath, name], new Set([...visitedInterfaces, interfaceFqs[0]]))
        } else if (interfaceFqs && interfaceFqs.length > 1) {
          adapterLimitations.add(boundLimitation(`contract-group-ambiguous:${groupFq}.${name}`))
        } else {
          slots.push({ path: [...relativePath, name], decl })
        }
      }
    }
    walkGroup(contractFq, [], new Set())
    for (const [, list] of nestedInterfaces) {
      // Interfaces never reached through a val group keep their declared
      // container path so their slots are not silently lost.
      for (const fq of list) {
        if (usedInterfaces.has(fq)) continue
        walkGroup(fq, fq.slice(contractFq.length + 1).split('.'), new Set([fq]))
      }
    }
    const byPath = new Map()
    for (const slot of slots) {
      const key = slot.path.join('.')
      if (!byPath.has(key)) byPath.set(key, slot)
    }
    return [...byPath.keys()].sort().map((key) => byPath.get(key))
  }

  const planContract = (kind, contractFq, implementationEntries) => {
    if (!index.containersByFq.has(contractFq)) {
      adapterLimitations.add(boundLimitation(`authority-symbol-not-found:${contractFq}`))
      return
    }
    if (!implementationEntries.length) {
      adapterLimitations.add(boundLimitation(`contract-without-implementations:${contractFq}`))
      return
    }
    const contractSimple = contractFq.split('.').pop()
    for (const slot of collectContractSlots(contractFq)) {
      const slotIdentityFq = contractFq + '.' + slot.path.join('.')
      const id = `${adapterId}:${slotIdentityFq}`
      const modePlans = implementationEntries.map((entry) => {
        for (const symbol of entry.symbols) {
          const decl = index.valsByFq.get(`${symbol}.${slot.path.join('.')}`)
          if (decl && !isAbstractVal(decl)) return { mode: entry.mode, decl }
        }
        return {
          mode: entry.mode,
          decl: null,
          absenceReason: `implementation ${entry.symbols.join(', ')} does not override this slot`
        }
      })
      const provided = modePlans.some((modePlan) => modePlan.decl)
      const ok = addPlan({
        id, kind,
        layer: provided ? 'semantic-implementation' : 'semantic-contract',
        semanticPath: [contractSimple, ...slot.path],
        // Identity and source are anchored on the contract slot declaration:
        // implementations moving between files must not change this row.
        source: {
          path: slot.decl.path, line: slot.decl.line, column: slot.decl.column,
          symbol: slotIdentityFq, fileHash: slot.decl.fileHash
        },
        modePlans,
        extraLimitations: []
      })
      if (!ok) continue
      for (const modePlan of modePlans) {
        if (!modePlan.decl) continue
        consumedDeclFqs.add(modePlan.decl.fq)
        if (!declFqToTokenId.has(modePlan.decl.fq)) declFqToTokenId.set(modePlan.decl.fq, id)
      }
    }
  }

  const planPrimitives = (kind, containerFq) => {
    if (!declaredModesSet.has('shared')) {
      adapterLimitations.add(boundLimitation(`primitive-container-requires-shared-mode:${containerFq}`))
      return
    }
    const valFqs = valFqsUnder(containerFq)
    if (!index.containersByFq.has(containerFq) && !valFqs.length) {
      adapterLimitations.add(boundLimitation(`authority-symbol-not-found:${containerFq}`))
      return
    }
    const containerSimple = containerFq.split('.').pop()
    for (const fq of valFqs) {
      const decl = index.valsByFq.get(fq)
      const id = `${adapterId}:${fq}`
      const ok = addPlan({
        id, kind, layer: 'primitive',
        semanticPath: [containerSimple, ...fq.slice(containerFq.length + 1).split('.')],
        source: { path: decl.path, line: decl.line, column: decl.column, symbol: fq, fileHash: decl.fileHash },
        modePlans: [{ mode: 'shared', decl }],
        extraLimitations: []
      })
      if (ok && !declFqToTokenId.has(fq)) declFqToTokenId.set(fq, id)
    }
  }

  const planImplementationOnly = (kind, entry, symbol) => {
    const valFqs = valFqsUnder(symbol)
    if (!index.containersByFq.has(symbol) && !valFqs.length) {
      adapterLimitations.add(boundLimitation(`authority-symbol-not-found:${symbol}`))
      return
    }
    const symbolSimple = symbol.split('.').pop()
    for (const fq of valFqs) {
      const decl = index.valsByFq.get(fq)
      if (consumedDeclFqs.has(fq) || !decl.isOverride) continue
      const id = `${adapterId}:${fq}`
      const ok = addPlan({
        id, kind, layer: 'semantic-implementation',
        semanticPath: [symbolSimple, ...fq.slice(symbol.length + 1).split('.')],
        source: { path: decl.path, line: decl.line, column: decl.column, symbol: fq, fileHash: decl.fileHash },
        modePlans: [{ mode: entry.mode, decl }],
        extraLimitations: ['implementation-slot-without-contract']
      })
      if (ok && !declFqToTokenId.has(fq)) declFqToTokenId.set(fq, id)
    }
  }

  for (const kind of Object.keys(authorities).sort()) {
    if (!AUTHORITY_KINDS.has(kind)) {
      adapterLimitations.add(boundLimitation(`authority-kind-unknown:${kind}`))
      continue
    }
    const authority = authorities[kind] || {}
    const implementationEntries = (authority.implementations || []).filter((entry) => {
      if (declaredModesSet.has(entry.mode)) return true
      adapterLimitations.add(boundLimitation(`implementation-mode-not-declared:${entry.mode}`))
      return false
    })
    for (const contractFq of authority.contracts || []) planContract(kind, contractFq, implementationEntries)
    for (const containerFq of authority.primitiveContainers || []) planPrimitives(kind, containerFq)
    for (const entry of implementationEntries) {
      for (const symbol of entry.symbols) planImplementationOnly(kind, entry, symbol)
    }
  }

  plans.sort((a, b) => stringCompare(a.id, b.id))
  if (plans.length > TOKEN_LIMITS.projectTokensMax) {
    adapterLimitations.add('limit:project-tokens-max')
    plans.length = TOKEN_LIMITS.projectTokensMax
  }
  const emittedIds = new Set(plans.map((plan) => plan.id))

  const tokens = []
  for (const plan of plans) {
    const modes = {}
    const edges = []
    const tokenLimitations = new Set(plan.extraLimitations)
    for (const modePlan of [...plan.modePlans].sort((a, b) => stringCompare(a.mode, b.mode))) {
      if (modes[modePlan.mode] !== undefined) continue
      if (!modePlan.decl) {
        modes[modePlan.mode] = {
          raw: { expression: '<no override>' },
          unsupported: { reason: boundReason(modePlan.absenceReason) }
        }
        continue
      }
      const decl = modePlan.decl
      const rawExpression = decl.initializer
        ? boundExpression(textOf(decl.text, decl.initializer))
        : boundExpression(textOf(decl.text, decl.propertyNode))
      const resolution = resolveDeclarationValue(decl, context, new Set([decl.fq]), 0)
      if (!resolution.ok) {
        modes[modePlan.mode] = { raw: { expression: rawExpression }, unsupported: { reason: resolution.reason } }
        continue
      }
      if (resolution.value.kind !== plan.kind) {
        modes[modePlan.mode] = {
          raw: { expression: rawExpression },
          unsupported: { reason: boundReason(`value kind ${resolution.value.kind} under a ${plan.kind} authority`) }
        }
        continue
      }
      modes[modePlan.mode] = { raw: { expression: rawExpression }, resolved: resolution.value }
      for (const limitation of resolution.tokenLimitations || []) tokenLimitations.add(boundLimitation(limitation))
      if (resolution.refFq) {
        const targetId = declFqToTokenId.get(resolution.refFq)
        if (targetId && emittedIds.has(targetId) && targetId !== plan.id) {
          const edge = { kind: 'alias', targetProjectTokenId: targetId }
          if (modePlan.mode !== 'shared') edge.mode = modePlan.mode
          edges.push(edge)
        }
      }
      if (resolution.transform) {
        const targetId = declFqToTokenId.get(resolution.transform.targetFq)
        if (targetId && emittedIds.has(targetId) && targetId !== plan.id) {
          const edge = { kind: 'transform', targetProjectTokenId: targetId }
          if (modePlan.mode !== 'shared') edge.mode = modePlan.mode
          edge.detail = resolution.transform.detail.slice(0, EDGE_DETAIL_MAX)
          edges.push(edge)
        }
      }
    }
    if (!Object.keys(modes).length) continue
    edges.sort(edgeCompare)
    const seenEdges = new Set()
    const uniqueEdges = []
    for (const edge of edges) {
      const key = JSON.stringify([edge.kind, edge.targetProjectTokenId, edge.mode || '', edge.detail || ''])
      if (seenEdges.has(key)) continue
      seenEdges.add(key)
      uniqueEdges.push(edge)
    }
    if (uniqueEdges.length > TOKEN_LIMITS.aliasFanOutMax) {
      adapterLimitations.add('limit:alias-fan-out')
      uniqueEdges.length = TOKEN_LIMITS.aliasFanOutMax
    }
    tokens.push({
      projectTokenId: plan.id,
      kind: plan.kind,
      layer: plan.layer,
      semanticPath: plan.semanticPath,
      displayName: plan.semanticPath.join('.'),
      modes,
      source: plan.source,
      edges: uniqueEdges,
      limitations: [...tokenLimitations].sort().slice(0, 32)
    })
  }

  parseFailures.sort((a, b) => stringCompare(a.path, b.path) || stringCompare(a.reason, b.reason))
  const limitations = [...adapterLimitations].sort().slice(0, ADAPTER_LIMITATIONS_MAX)
  // retainedTrees kept alive until here on purpose (declaration nodes borrow
  // from their trees).
  void retainedTrees.length
  return { tokens, parseFailures, limitations }
}
