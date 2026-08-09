// kotlin-compose adapter: extracts a project component inventory from Kotlin
// Compose design-system sources (FIGMA_COMPONENTS.md §13). Tree-sitter syntax
// facts + bounded in-scope resolution — never Gradle, never execution, never a
// regex fallback over a failed parse. The configured component roots ARE the
// design-system classifier: no @Composable outside them is ever considered,
// and nothing is matched by file name. Anything statically unprovable stays
// out with a limitation instead of becoming a guessed fact.
//
// Pure extractor (REQ-ADAPT-001 contract): no fs/env/network/clock; the same
// { files, previewFiles, screenshotTestFiles, componentsConfig, tokensConfig,
//   adapterId, platform } input serializes to the same bytes.
import { createHash } from 'node:crypto'
import { parseKotlinSource } from '../../scripts/compose-model/parser-tree-sitter.mjs'
import { COMPONENT_LIMITS } from '../../components/limits.mjs'

export const KOTLIN_COMPOSE_COMPONENTS_EXTRACTOR_VERSION = 'kotlin-compose-components-v2'

const REASON_MAX = 300
const TYPE_TEXT_MAX = 500
const LIMITATION_MAX = 200

const collapse = (text) => String(text).replace(/\s+/g, ' ').trim()
const boundReason = (text) => collapse(text).slice(0, REASON_MAX)
const boundLimitation = (text) => String(text).slice(0, LIMITATION_MAX)
const textOf = (source, node) => source.slice(node.startIndex, node.endIndex)

function namedChildOfType(node, type) {
  for (let i = 0; i < node.namedChildCount; i++) {
    const child = node.namedChild(i)
    if (child.type === type) return child
  }
  return null
}

function packageNameOf(root, source) {
  const header = namedChildOfType(root, 'package_header')
  if (!header) return ''
  const qualified = namedChildOfType(header, 'qualified_identifier') || namedChildOfType(header, 'identifier')
  return qualified ? textOf(source, qualified).replace(/\s+/g, '') : ''
}

function importsOf(root, source) {
  const imports = new Map()
  for (let i = 0; i < root.namedChildCount; i++) {
    const child = root.namedChild(i)
    if (child.type !== 'import') continue
    const qualified = namedChildOfType(child, 'qualified_identifier')
    if (!qualified) continue
    const fq = textOf(source, qualified).replace(/\s+/g, '')
    const leaf = fq.split('.').pop()
    if (leaf && leaf !== '*') imports.set(leaf, fq)
  }
  return imports
}

function annotationNamesOf(node, source) {
  const names = []
  const modifiers = namedChildOfType(node, 'modifiers')
  if (!modifiers) return names
  for (let i = 0; i < modifiers.namedChildCount; i++) {
    const child = modifiers.namedChild(i)
    if (child.type !== 'annotation') continue
    const match = /^@?([A-Za-z_][A-Za-z0-9_.]*)/.exec(collapse(textOf(source, child)).replace(/^@/, ''))
    if (match) names.push(match[1].split('.').pop())
  }
  return names
}

function visibilityOf(node, source) {
  const modifiers = namedChildOfType(node, 'modifiers')
  if (!modifiers) return 'public'
  for (let i = 0; i < modifiers.namedChildCount; i++) {
    const child = modifiers.namedChild(i)
    if (child.type === 'visibility_modifier') {
      const text = textOf(source, child)
      if (text === 'internal') return 'internal'
      if (text === 'private' || text === 'protected') return text
    }
  }
  return 'public'
}

function typeContainsComposableAnnotation(typeNode, source) {
  const text = textOf(source, typeNode)
  return /@Composable\b/.test(text)
}

function unwrapType(typeNode) {
  let current = typeNode
  for (let depth = 0; depth < 8 && current; depth++) {
    if (current.type === 'nullable_type' || current.type === 'parenthesized_type') {
      let inner = null
      for (let i = 0; i < current.namedChildCount; i++) {
        const child = current.namedChild(i)
        if (child.type !== 'type_modifiers') { inner = child; break }
      }
      if (!inner) return current
      current = inner
      continue
    }
    return current
  }
  return current
}

// Parameters with their default expressions: `parameter` nodes are siblings of
// their default expression inside function_value_parameters (the default
// follows an anonymous '=' until the next ',').
function parametersOf(fn, source) {
  const container = namedChildOfType(fn, 'function_value_parameters')
  if (!container) return []
  const rows = []
  let current = null
  let seenAssign = false
  for (let i = 0; i < container.childCount; i++) {
    const child = container.child(i)
    if (!child.isNamed) {
      if (child.type === '=') seenAssign = true
      if (child.type === ',' || child.type === ')') seenAssign = false
      continue
    }
    if (child.type === 'parameter') {
      const nameNode = namedChildOfType(child, 'identifier')
      let typeNode = null
      for (let j = 0; j < child.namedChildCount; j++) {
        const inner = child.namedChild(j)
        if (inner.type !== 'identifier') { typeNode = inner; break }
      }
      current = {
        name: nameNode ? textOf(source, nameNode) : null,
        typeNode,
        typeText: typeNode ? collapse(textOf(source, typeNode)).slice(0, TYPE_TEXT_MAX) : '',
        defaultNode: null
      }
      if (current.name) rows.push(current)
      seenAssign = false
    } else if (seenAssign && current && !current.defaultNode) {
      current.defaultNode = child
    }
  }
  return rows
}

function flattenNavigation(node, source) {
  if (node.type === 'identifier') return [textOf(source, node)]
  if (node.type !== 'navigation_expression' || node.namedChildCount !== 2) return null
  const left = flattenNavigation(node.namedChild(0), source)
  const right = node.namedChild(1)
  if (!left || right.type !== 'identifier') return null
  return [...left, textOf(source, right)]
}

// Collect call-expression callee simple names and navigation chains inside a
// node (bounded walk).
function collectBodyFacts(bodyNode, source, facts, depth) {
  if (!bodyNode || depth > 64) return
  for (let i = 0; i < bodyNode.namedChildCount; i++) {
    const child = bodyNode.namedChild(i)
    if (child.type === 'call_expression') {
      const callee = child.namedChild(0)
      if (callee) {
        if (callee.type === 'identifier') {
          facts.calls.push(textOf(source, callee))
        } else if (callee.type === 'call_expression') {
          const inner = callee.namedChild(0)
          if (inner && inner.type === 'identifier') facts.calls.push(textOf(source, inner))
        } else if (callee.type === 'navigation_expression') {
          const chain = flattenNavigation(callee, source)
          if (chain) facts.navigations.push(chain)
        }
      }
    } else if (child.type === 'navigation_expression') {
      const chain = flattenNavigation(child, source)
      if (chain) { facts.navigations.push(chain); continue }
    }
    collectBodyFacts(child, source, facts, depth + 1)
  }
}

// True when the function body is a single call to `calleeName` (bounded
// wrapper evidence — the relation stays a mapping-review decision).
function singleDelegationTarget(bodyNode, source) {
  if (!bodyNode) return null
  const block = namedChildOfType(bodyNode, 'block') || bodyNode
  const statements = []
  for (let i = 0; i < block.namedChildCount; i++) statements.push(block.namedChild(i))
  if (statements.length !== 1 || statements[0].type !== 'call_expression') return null
  let callee = statements[0].namedChild(0)
  if (callee && callee.type === 'call_expression') callee = callee.namedChild(0)
  return callee && callee.type === 'identifier' ? textOf(source, callee) : null
}

function enumValuesOf(classNode, source) {
  const body = namedChildOfType(classNode, 'enum_class_body')
  if (!body) return null
  const values = []
  for (let i = 0; i < body.namedChildCount; i++) {
    const entry = body.namedChild(i)
    if (entry.type !== 'enum_entry') continue
    const identifier = namedChildOfType(entry, 'identifier')
    if (identifier) values.push(textOf(source, identifier))
  }
  return values.length ? values : null
}

function sealedValuesOf(classNode, source) {
  const modifiers = namedChildOfType(classNode, 'modifiers')
  if (!modifiers || !/\bsealed\b/.test(textOf(source, modifiers))) return null
  const body = namedChildOfType(classNode, 'class_body')
  if (!body) return null
  const values = []
  for (let i = 0; i < body.namedChildCount; i++) {
    const child = body.namedChild(i)
    if (child.type !== 'object_declaration' && child.type !== 'class_declaration') continue
    const identifier = namedChildOfType(child, 'identifier')
    if (identifier) values.push(textOf(source, identifier))
  }
  return values.length ? values : null
}

function classifyParameter(parameter, source) {
  const unwrapped = parameter.typeNode ? unwrapType(parameter.typeNode) : null
  if (!unwrapped) return { kind: 'value' }
  if (unwrapped.type === 'function_type') {
    return typeContainsComposableAnnotation(parameter.typeNode, source)
      ? { kind: 'content-lambda' }
      : { kind: 'callback' }
  }
  if (unwrapped.type === 'user_type') {
    const simple = collapse(textOf(source, unwrapped)).replace(/<.*$/, '').split('.').pop().replace(/\?$/, '')
    if (simple === 'Modifier') return { kind: 'modifier' }
    if (simple === 'String') return { kind: 'text' }
    if (simple === 'Boolean') return { kind: 'boolean-variant' }
    return { kind: 'user-type', simple }
  }
  return { kind: 'value' }
}

function defaultValueTextOf(parameter, source) {
  if (!parameter.defaultNode) return null
  return collapse(textOf(source, parameter.defaultNode)).slice(0, 500)
}

function overloadDiscriminatorOf(parameters) {
  const signature = parameters.map((parameter) => `${parameter.name}:${parameter.typeText}`).join(',')
  return createHash('sha256').update(signature, 'utf8').digest('hex').slice(0, 8)
}

// Resolve a body token reference chain against configured token authorities.
// Exact identity only: the chain root must resolve (import or same package)
// to a configured authority symbol; the projectTokenId mirrors the token
// extractor's `${adapterId}:${fq}` construction. Unresolvable references stay
// out — evidence is never invented.
function tokenAuthorityIndex(tokensConfig) {
  const roots = new Map()
  if (!tokensConfig || !tokensConfig.authorities) return roots
  for (const authority of Object.values(tokensConfig.authorities)) {
    const symbols = [
      ...(authority.contracts || []),
      ...(authority.primitiveContainers || []),
      ...((authority.implementations || []).flatMap((implementation) => implementation.symbols))
    ]
    for (const fq of symbols) roots.set(fq.split('.').pop(), fq)
  }
  return roots
}

// options: { files, previewFiles, screenshotTestFiles, componentsConfig,
//            tokensConfig, adapterId, platform }
export function extractComponents(options) {
  const { componentsConfig, tokensConfig, adapterId } = options
  const files = options.files.slice().sort((a, b) => (a.path < b.path ? -1 : a.path > b.path ? 1 : 0))
  const previewFiles = (options.previewFiles || []).slice().sort((a, b) => (a.path < b.path ? -1 : a.path > b.path ? 1 : 0))
  const screenshotTestFiles = (options.screenshotTestFiles || []).slice().sort((a, b) => (a.path < b.path ? -1 : a.path > b.path ? 1 : 0))
  const allowedVisibility = new Set(componentsConfig.visibility)
  const tokenRoots = tokenAuthorityIndex(tokensConfig)
  const parseFailures = []
  const limitations = new Set()

  // ── pass 1: parse + index declarations ──────────────────────────────────
  const parsedFiles = []
  const enumsByFq = new Map()
  const candidates = []
  for (const file of files) {
    const parsed = parseKotlinSource(file.text)
    if (parsed.hasError) {
      const first = parsed.parseErrors[0]
      parseFailures.push({
        path: file.path,
        reason: boundReason(`parse error at ${first ? first.start.row + 1 : '?'}:${first ? first.start.column : '?'}`)
      })
      continue
    }
    const source = file.text
    const pkg = packageNameOf(parsed.root, source)
    const imports = importsOf(parsed.root, source)
    parsedFiles.push({ file, parsed, pkg, imports })
    for (let i = 0; i < parsed.root.namedChildCount; i++) {
      const child = parsed.root.namedChild(i)
      if (child.type === 'class_declaration') {
        const identifier = namedChildOfType(child, 'identifier')
        if (!identifier) continue
        const name = textOf(source, identifier)
        const fq = (pkg ? pkg + '.' : '') + name
        const enumValues = enumValuesOf(child, source)
        const sealedValues = enumValues ? null : sealedValuesOf(child, source)
        if (enumValues) enumsByFq.set(fq, { source: 'enum', values: enumValues, name })
        else if (sealedValues) enumsByFq.set(fq, { source: 'sealed', values: sealedValues, name })
      } else if (child.type === 'function_declaration') {
        const annotations = annotationNamesOf(child, source)
        if (!annotations.includes('Composable')) continue
        if (annotations.includes('Preview')) continue
        const identifier = namedChildOfType(child, 'identifier')
        if (!identifier) continue
        const visibility = visibilityOf(child, source)
        if (visibility === 'private' || visibility === 'protected') continue
        if (!allowedVisibility.has(visibility)) continue
        candidates.push({ fileEntry: parsedFiles[parsedFiles.length - 1], node: child, name: textOf(source, identifier), annotations, visibility })
      }
    }
  }

  // ── pass 2: overload discrimination ─────────────────────────────────────
  const byFq = new Map()
  for (const candidate of candidates) {
    const fq = (candidate.fileEntry.pkg ? candidate.fileEntry.pkg + '.' : '') + candidate.name
    candidate.fqName = fq
    let bucket = byFq.get(fq)
    if (!bucket) { bucket = []; byFq.set(fq, bucket) }
    bucket.push(candidate)
  }
  for (const bucket of byFq.values()) {
    if (bucket.length === 1) continue
    for (const candidate of bucket) {
      candidate.overloadDiscriminator = overloadDiscriminatorOf(parametersOf(candidate.node, candidate.fileEntry.file.text))
    }
    const discriminators = new Set(bucket.map((candidate) => candidate.overloadDiscriminator))
    if (discriminators.size !== bucket.length) {
      limitations.add(boundLimitation(`duplicate-declaration:${bucket[0].fqName}`))
    }
  }
  const componentIdOf = (candidate) =>
    `${adapterId}:symbol:${candidate.fqName}${candidate.overloadDiscriminator ? `#${candidate.overloadDiscriminator}` : ''}`
  const idsBySimpleName = new Map()
  for (const candidate of candidates) {
    let bucket = idsBySimpleName.get(candidate.name)
    if (!bucket) { bucket = []; idsBySimpleName.set(candidate.name, bucket) }
    bucket.push(componentIdOf(candidate))
  }

  // ── pass 3: build component rows ────────────────────────────────────────
  const components = []
  for (const candidate of candidates) {
    const { fileEntry, node } = candidate
    const source = fileEntry.file.text
    const parameters = parametersOf(node, source)
    if (parameters.length > COMPONENT_LIMITS.parametersPerComponentMax) {
      limitations.add(boundLimitation(`limit:parameters:${candidate.fqName}`))
      continue
    }
    const apiParameters = []
    const variantProperties = []
    const slots = []
    for (const parameter of parameters) {
      const classified = classifyParameter(parameter, source)
      const hasDefault = !!parameter.defaultNode
      const defaultText = defaultValueTextOf(parameter, source)
      let kind = 'value'
      if (classified.kind === 'modifier') kind = 'modifier'
      else if (classified.kind === 'callback') kind = 'callback'
      else if (classified.kind === 'content-lambda') kind = 'content-lambda'
      else if (classified.kind === 'text') kind = 'text'
      else if (classified.kind === 'user-type' && /State$|Model$/.test(classified.simple)) kind = 'state'

      const propertyId = `param:${parameter.name}`
      if (classified.kind === 'boolean-variant') {
        const defaultValue = defaultText === 'true' || defaultText === 'false' ? defaultText : undefined
        variantProperties.push({
          projectPropertyId: propertyId,
          name: parameter.name,
          source: 'boolean',
          values: [{ value: 'false' }, { value: 'true' }],
          ...(defaultValue !== undefined ? { defaultValue } : {}),
          defaultKnown: defaultValue !== undefined
        })
      } else if (classified.kind === 'user-type') {
        const resolvedFq = fileEntry.imports.get(classified.simple) ||
          (fileEntry.pkg ? fileEntry.pkg + '.' : '') + classified.simple
        const declaration = enumsByFq.get(resolvedFq)
        if (declaration) {
          let defaultValue
          if (defaultText) {
            const leaf = defaultText.split('.').pop()
            if (declaration.values.includes(leaf)) defaultValue = leaf
          }
          variantProperties.push({
            projectPropertyId: propertyId,
            name: parameter.name,
            source: declaration.source,
            typeSymbol: resolvedFq.slice(0, 320),
            values: declaration.values.slice(0, COMPONENT_LIMITS.projectVariantValuesPerPropertyMax).map((value) => ({ value })),
            ...(defaultValue !== undefined ? { defaultValue } : {}),
            defaultKnown: defaultValue !== undefined
          })
        }
      } else if (kind === 'text' || kind === 'callback' || kind === 'content-lambda') {
        slots.push({
          slotId: propertyId,
          kind: kind === 'text' ? 'text' : kind === 'content-lambda' ? 'content' : 'callback',
          name: parameter.name,
          required: !hasDefault
        })
      }
      apiParameters.push({
        name: parameter.name,
        kind,
        typeText: parameter.typeText || '<untyped>',
        required: !hasDefault,
        hasDefault,
        ...(defaultText !== null ? { defaultText } : {})
      })
    }

    // Body facts: nested in-scope component calls + token references.
    const bodyNode = namedChildOfType(node, 'function_body')
    const bodyFacts = { calls: [], navigations: [] }
    if (bodyNode) collectBodyFacts(bodyNode, source, bodyFacts, 0)
    const dependencies = []
    const dependencySeen = new Set()
    for (const call of bodyFacts.calls) {
      if (call === candidate.name) continue
      const targets = idsBySimpleName.get(call)
      if (!targets) continue
      const key = `component ${call}`
      if (dependencySeen.has(key)) continue
      dependencySeen.add(key)
      if (targets.length === 1) {
        dependencies.push({ kind: 'component', targetProjectComponentId: targets[0], symbol: call.slice(0, 320) })
      } else {
        dependencies.push({ kind: 'component', symbol: call.slice(0, 320) })
        limitations.add(boundLimitation(`ambiguous-nested-call:${candidate.fqName}:${call}`))
      }
    }
    for (const chain of bodyFacts.navigations) {
      const rootFq = tokenRoots.get(chain[0])
      if (!rootFq || chain.length < 2) continue
      const fqPath = [rootFq, ...chain.slice(1)].join('.')
      const projectTokenId = `${adapterId}:${fqPath}`.slice(0, 400)
      const key = `token ${projectTokenId}`
      if (dependencySeen.has(key)) continue
      dependencySeen.add(key)
      dependencies.push({
        kind: 'token',
        projectTokenId,
        path: fileEntry.file.path,
        line: node.startPosition.row + 1
      })
    }
    if (dependencies.length > COMPONENT_LIMITS.dependencyEdgesPerComponentMax) {
      limitations.add(boundLimitation(`limit:dependency-edges:${candidate.fqName}`))
      dependencies.length = COMPONENT_LIMITS.dependencyEdgesPerComponentMax
    }

    const wrapperTarget = bodyNode ? singleDelegationTarget(bodyNode, source) : null
    let wrapperOf
    if (wrapperTarget && wrapperTarget !== candidate.name) {
      const targets = idsBySimpleName.get(wrapperTarget)
      if (targets && targets.length === 1) wrapperOf = targets[0]
    }

    const component = {
      projectComponentId: componentIdOf(candidate),
      name: candidate.name,
      fqName: candidate.fqName,
      kind: 'function-component',
      visibility: candidate.visibility,
      ...(candidate.overloadDiscriminator ? { overloadDiscriminator: candidate.overloadDiscriminator } : {}),
      source: {
        path: fileEntry.file.path,
        line: node.startPosition.row + 1,
        symbol: candidate.fqName.slice(0, 320),
        fileHash: fileEntry.file.hash
      },
      api: { parameters: apiParameters },
      variantProperties,
      combinationsKnown: 'all',
      slots,
      dependencies,
      evidence: { previews: [], screenshotTests: [] },
      ...(candidate.annotations.includes('Deprecated') ? { deprecated: true } : {}),
      ...(wrapperOf ? { wrapperOf } : {})
    }
    components.push(component)
  }

  // ── pass 4: render/test evidence ────────────────────────────────────────
  const componentsBySimpleName = new Map()
  for (const component of components) {
    let bucket = componentsBySimpleName.get(component.name)
    if (!bucket) { bucket = []; componentsBySimpleName.set(component.name, bucket) }
    bucket.push(component)
  }
  const evidencePass = (entries, record) => {
    for (const file of entries) {
      const parsed = parseKotlinSource(file.text)
      if (parsed.hasError) {
        limitations.add(boundLimitation(`evidence-parse-error:${file.path}`))
        continue
      }
      record(file, parsed)
    }
  }
  evidencePass([...files, ...previewFiles], (file, parsed) => {
    const source = file.text
    for (let i = 0; i < parsed.root.namedChildCount; i++) {
      const child = parsed.root.namedChild(i)
      if (child.type !== 'function_declaration') continue
      const annotations = annotationNamesOf(child, source)
      if (!annotations.includes('Preview')) continue
      const identifier = namedChildOfType(child, 'identifier')
      if (!identifier) continue
      const previewName = textOf(source, identifier)
      const facts = { calls: [], navigations: [] }
      collectBodyFacts(namedChildOfType(child, 'function_body'), source, facts, 0)
      const called = new Set(facts.calls)
      for (const [simpleName, componentRows] of componentsBySimpleName) {
        if (!called.has(simpleName)) continue
        for (const component of componentRows) {
          if (component.evidence.previews.length >= 64) continue
          component.evidence.previews.push({
            symbol: previewName.slice(0, 320),
            path: file.path,
            line: child.startPosition.row + 1
          })
        }
      }
    }
  })
  evidencePass(screenshotTestFiles, (file, parsed) => {
    const source = file.text
    const facts = { calls: [], navigations: [] }
    collectBodyFacts(parsed.root, source, facts, 0)
    const called = new Set(facts.calls)
    let className
    for (let i = 0; i < parsed.root.namedChildCount; i++) {
      const child = parsed.root.namedChild(i)
      if (child.type === 'class_declaration') {
        const identifier = namedChildOfType(child, 'identifier')
        if (identifier) { className = textOf(source, identifier); break }
      }
    }
    for (const [simpleName, componentRows] of componentsBySimpleName) {
      if (!called.has(simpleName)) continue
      for (const component of componentRows) {
        if (component.evidence.screenshotTests.length >= 64) continue
        component.evidence.screenshotTests.push({
          path: file.path,
          ...(className ? { className: className.slice(0, 320) } : {})
        })
      }
    }
  })

  components.sort((a, b) => (a.projectComponentId < b.projectComponentId ? -1 : a.projectComponentId > b.projectComponentId ? 1 : 0))
  parseFailures.sort((a, b) => (a.path < b.path ? -1 : a.path > b.path ? 1 : 0))
  return {
    components,
    parseFailures,
    limitations: [...limitations].sort().slice(0, 64)
  }
}
