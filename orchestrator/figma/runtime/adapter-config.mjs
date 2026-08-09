// Strict reader for the project-owned adapter configuration
// (orchestrator/figma/project-adapters.json). Shared foundation: it validates
// the envelope, uniqueness, capability sections, path containment, glob
// grammar, and cross-adapter authority overlaps — but knows nothing about
// token/component business semantics beyond the capability section shapes the
// schema pins. Absence of the file is the explicit unconfigured state, never
// an error and never a heuristic search.
import { readFileSync } from 'node:fs'
import { isAbsolute, resolve, sep } from 'node:path'
import Ajv from 'ajv'
import { compileGlob } from './glob.mjs'
import { bytesHash, canonicalHash } from './canonical-json.mjs'
import { typedError } from './typed-error.mjs'
import { ADAPTER_ERROR_CODES } from './error-codes.mjs'
import { readContainedSingleLinkFile } from './file-safety.mjs'
import adapterConfigIdentity from './adapter-config-identity.cjs'

const ADAPTER_CONFIG_RELATIVE_PATH = 'orchestrator/figma/project-adapters.json'
const CONFIG_BYTES_MAX = 256 * 1024
const BUILT_IN_ADAPTER_KINDS = Object.freeze({
  'kotlin-compose': Object.freeze({ version: 2, capabilities: Object.freeze(['tokens', 'components']) }),
  'json-tokens': Object.freeze({ version: 2, capabilities: Object.freeze(['tokens']) }),
  'component-manifest': Object.freeze({ version: 2, capabilities: Object.freeze(['components']) })
})
const DEFAULT_SCHEMA_VALIDATE = new Ajv({ allErrors: true, strict: false }).compile(
  JSON.parse(readFileSync(new URL('../schemas/project-adapters.schema.json', import.meta.url), 'utf8'))
)

const isRecord = (value) => !!value && typeof value === 'object' && !Array.isArray(value)

function invalid(detail, path) {
  return typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTER_CONFIG_INVALID, detail, { path })
}

function containedRelativePath(projectRoot, relativePath, label) {
  if (typeof relativePath !== 'string' || !relativePath || relativePath.includes('\\') || isAbsolute(relativePath)) {
    throw invalid(`${label} must be a repository-relative POSIX path`, relativePath)
  }
  const segments = relativePath.split('/')
  if (segments.some((segment) => !segment || segment === '.' || segment === '..')) {
    throw invalid(`${label} must not contain empty or dot segments`, relativePath)
  }
  const absolute = resolve(projectRoot, relativePath)
  const rootPrefix = resolve(projectRoot) + sep
  if (absolute !== resolve(projectRoot) && !absolute.startsWith(rootPrefix)) {
    throw invalid(`${label} escapes the repository root`, relativePath)
  }
  return { relativePath, absolute }
}

function assertNoRootOverlap(roots, label) {
  for (let leftIndex = 0; leftIndex < roots.length; leftIndex++) {
    for (let rightIndex = leftIndex + 1; rightIndex < roots.length; rightIndex++) {
      const left = roots[leftIndex].relativePath + '/'
      const right = roots[rightIndex].relativePath + '/'
      if (left === right || left.startsWith(right) || right.startsWith(left)) {
        throw invalid(`${label} contains overlapping roots ${roots[leftIndex].relativePath} and ${roots[rightIndex].relativePath}`)
      }
    }
  }
}

function pathKey(segments) {
  return segments.join('\u0000')
}

function startsWithPath(path, prefix) {
  return prefix.length <= path.length && prefix.every((segment, index) => path[index] === segment)
}

function pathExcluded(rule, path) {
  if (rule.kind !== 'prefix-map') return false
  return rule.excludeExact.some((candidate) => pathKey(candidate) === pathKey(path)) ||
    rule.excludePrefix.some((candidate) => startsWithPath(path, candidate))
}

function exactPaths(rule) {
  if (rule.kind === 'exact-path') return [rule.providerPath]
  if (rule.kind === 'explicit-table') return rule.entries.map((entry) => entry.providerPath)
  return null
}

function rulesOverlap(left, right) {
  if (left.tokenKind !== right.tokenKind) return false
  const leftExact = exactPaths(left)
  const rightExact = exactPaths(right)
  if (leftExact && rightExact) {
    const rightKeys = new Set(rightExact.map(pathKey))
    return leftExact.some((path) => rightKeys.has(pathKey(path)))
  }
  if (leftExact || rightExact) {
    const exact = leftExact || rightExact
    const prefix = leftExact ? right : left
    return exact.some((path) => startsWithPath(path, prefix.providerPrefix) && !pathExcluded(prefix, path))
  }
  const intersectionPrefix = left.providerPrefix.length >= right.providerPrefix.length
    ? left.providerPrefix
    : right.providerPrefix
  if (!startsWithPath(intersectionPrefix, left.providerPrefix) ||
      !startsWithPath(intersectionPrefix, right.providerPrefix)) return false
  const leftCovers = left.excludePrefix.some((excluded) => startsWithPath(intersectionPrefix, excluded))
  const rightCovers = right.excludePrefix.some((excluded) => startsWithPath(intersectionPrefix, excluded))
  return !leftCovers && !rightCovers
}

function contextsOverlap(left, right) {
  const keys = new Set([...Object.keys(left.when), ...Object.keys(right.when)])
  for (const key of keys) {
    if (Object.hasOwn(left.when, key) && Object.hasOwn(right.when, key) && left.when[key] !== right.when[key]) return false
  }
  return true
}

// Semantic validation of a schema-valid config document. schemaValidate is an
// injected ajv validate function so this module stays free of I/O policy.
export function validateAdapterConfig(document, { projectRoot, schemaValidate }) {
  const validate = typeof schemaValidate === 'function' ? schemaValidate : DEFAULT_SCHEMA_VALIDATE
  if (!validate(document)) {
      const first = (validate.errors || [])[0]
      throw invalid(`schema: ${(first && (first.instancePath || '/') + ' ' + first.message) || 'invalid document'}`)
  }
  if (!isRecord(document) || document.schemaVersion !== 2) {
    throw invalid('schemaVersion must be exactly 2; author the current adapter contract')
  }

  const ids = new Set()
  const adapters = document.adapters.map((adapter, index) => {
    const where = `adapters[${index}]`
    if (ids.has(adapter.id)) throw invalid(`duplicate adapter id ${JSON.stringify(adapter.id)}`, where)
    ids.add(adapter.id)

    const builtIn = BUILT_IN_ADAPTER_KINDS[adapter.kind]
    if (!builtIn) throw invalid(`unknown adapter kind ${JSON.stringify(adapter.kind)}`, where)
    if (adapter.version !== builtIn.version) {
      throw invalid(`adapter kind ${adapter.kind} supports version ${builtIn.version} only`, where)
    }
    for (const capability of adapter.capabilities) {
      if (!builtIn.capabilities.includes(capability)) {
        throw invalid(`adapter kind ${adapter.kind} does not ship capability ${JSON.stringify(capability)}`, where)
      }
      if (!isRecord(adapter[capability])) {
        throw invalid(`declared capability ${JSON.stringify(capability)} has no ${capability} section`, where)
      }
    }
    for (const key of Object.keys(adapter)) {
      if (['id', 'kind', 'version', 'enabled', 'capabilities', 'platform', 'authority'].includes(key)) continue
      if (!adapter.capabilities.includes(key)) {
        throw invalid(`section ${JSON.stringify(key)} belongs to an undeclared capability`, where)
      }
    }

    let tokenRoots = []
    if (adapter.capabilities.includes('tokens')) {
      const tokens = adapter.tokens
      tokenRoots = tokens.roots.map((root) => containedRelativePath(projectRoot, root, `${where}.tokens.roots`))
      assertNoRootOverlap(tokenRoots, `${where}.tokens.roots`)
      for (const pattern of [...tokens.include, ...(tokens.exclude || [])]) {
        try { compileGlob(pattern) } catch (error) {
          throw invalid(`glob ${JSON.stringify(pattern)}: ${error.message}`, where)
        }
      }
      if (new Set(tokens.modes).size !== tokens.modes.length) throw invalid('tokens.modes must be unique', where)
      for (let left = 0; left < tokens.contextMap.length; left++) {
        const rule = tokens.contextMap[left]
        if (!tokens.modes.includes(rule.projectMode)) {
          throw invalid(`contextMap projectMode ${JSON.stringify(rule.projectMode)} is outside tokens.modes`, where)
        }
        for (let right = left + 1; right < tokens.contextMap.length; right++) {
          if (contextsOverlap(rule, tokens.contextMap[right])) {
            throw invalid(`contextMap rules ${left} and ${right} overlap; first-match is forbidden`, where)
          }
        }
      }
      const ruleIds = new Set()
      for (let left = 0; left < tokens.bindingRules.length; left++) {
        const rule = tokens.bindingRules[left]
        if (ruleIds.has(rule.ruleId)) throw invalid(`duplicate binding rule id ${JSON.stringify(rule.ruleId)}`, where)
        ruleIds.add(rule.ruleId)
        if (rule.kind === 'explicit-table') {
          const providerPaths = new Set()
          for (const entry of rule.entries) {
            const key = pathKey(entry.providerPath)
            if (providerPaths.has(key)) throw invalid(`binding rule ${rule.ruleId} repeats providerPath`, where)
            providerPaths.add(key)
          }
        }
        for (let right = left + 1; right < tokens.bindingRules.length; right++) {
          if (rulesOverlap(rule, tokens.bindingRules[right])) {
            throw invalid(`binding rules ${rule.ruleId} and ${tokens.bindingRules[right].ruleId} overlap; priority/order is forbidden`, where)
          }
        }
      }
      for (const [kind, authority] of Object.entries(tokens.authorities)) {
        const symbols = new Set()
        for (const symbol of authority.contracts) {
          if (symbols.has(symbol)) throw invalid(`authority ${kind} lists duplicate contract ${JSON.stringify(symbol)}`, where)
          symbols.add(symbol)
        }
        for (const implementation of authority.implementations || []) {
          if (!tokens.modes.includes(implementation.mode)) {
            throw invalid(`authority ${kind} binds implementation mode ${JSON.stringify(implementation.mode)} outside tokens.modes`, where)
          }
          for (const symbol of implementation.symbols) {
            if (symbols.has(symbol)) throw invalid(`authority ${kind} symbol ${JSON.stringify(symbol)} is claimed twice`, where)
            symbols.add(symbol)
          }
        }
        for (const symbol of authority.primitiveContainers || []) {
          if (symbols.has(symbol)) throw invalid(`authority ${kind} symbol ${JSON.stringify(symbol)} is claimed twice`, where)
          symbols.add(symbol)
        }
        const modes = new Set()
        for (const implementation of authority.implementations || []) {
          if (modes.has(implementation.mode)) throw invalid(`authority ${kind} binds mode ${JSON.stringify(implementation.mode)} twice`, where)
          modes.add(implementation.mode)
        }
      }
    }

    let componentRoots = []
    if (adapter.capabilities.includes('components')) {
      const components = adapter.components
      componentRoots = components.roots.map((root) => containedRelativePath(projectRoot, root, `${where}.components.roots`))
      assertNoRootOverlap(componentRoots, `${where}.components.roots`)
      for (const pattern of [...components.include, ...(components.exclude || [])]) {
        try { compileGlob(pattern) } catch (error) {
          throw invalid(`glob ${JSON.stringify(pattern)}: ${error.message}`, where)
        }
      }
      const previewRoots = (components.previewRoots || []).map((root) =>
        containedRelativePath(projectRoot, root, `${where}.components.previewRoots`))
      const screenshotRoots = (components.screenshotTestRoots || []).map((root) =>
        containedRelativePath(projectRoot, root, `${where}.components.screenshotTestRoots`))
      assertNoRootOverlap(previewRoots, `${where}.components.previewRoots`)
      assertNoRootOverlap(screenshotRoots, `${where}.components.screenshotTestRoots`)
    }
    return { adapter, tokenRoots, componentRoots }
  })

  // Overlapping authoritative roots between enabled adapters of the SAME
  // capability are an explicit conflict: two adapters may never claim the same
  // source subtree for one capability without an ownership rule, and v1 ships
  // no such rule (first-match-wins is forbidden). Cross-capability overlap is
  // the declared non-conflicting rule: token and component extraction read the
  // same tree for different concerns and never contend for one entity.
  const enabled = adapters.filter((entry) => entry.adapter.enabled)
  for (const capability of ['tokens', 'components']) {
    const rootsKey = capability === 'tokens' ? 'tokenRoots' : 'componentRoots'
    for (let a = 0; a < enabled.length; a++) {
      for (let b = a + 1; b < enabled.length; b++) {
        for (const rootA of enabled[a][rootsKey]) {
          for (const rootB of enabled[b][rootsKey]) {
            const left = rootA.relativePath + '/'
            const right = rootB.relativePath + '/'
            if (left === right || left.startsWith(right) || right.startsWith(left)) {
              throw invalid(
                `enabled adapters ${enabled[a].adapter.id} and ${enabled[b].adapter.id} claim overlapping ${capability} roots ` +
                `${rootA.relativePath} and ${rootB.relativePath} without an ownership rule`
              )
            }
          }
        }
      }
    }
  }

  return {
    schemaVersion: 2,
    adapters: adapters.map((entry) => entry.adapter),
    enabledTokenAdapters: enabled.filter((entry) => entry.adapter.capabilities.includes('tokens')).map((entry) => entry.adapter),
    enabledComponentAdapters: enabled.filter((entry) => entry.adapter.capabilities.includes('components')).map((entry) => entry.adapter),
    configHash: canonicalHash(document),
    tokenConfigHash: adapterConfigIdentity.capabilityHash(document, 'tokens'),
    componentConfigHash: adapterConfigIdentity.capabilityHash(document, 'components')
  }
}

// Load + validate from disk. Returns { state: 'unconfigured' } |
// { state: 'configured', config, configHash, bytesHash } | throws TypedError.
export function loadAdapterConfig({ projectRoot, schemaValidate, readBytes }) {
  const reader = readBytes || ((absolute) => {
    try {
      return readContainedSingleLinkFile({
        root: projectRoot, file: absolute, maxBytes: CONFIG_BYTES_MAX, allowMissing: true
      })
    } catch (error) {
      throw invalid(`config is not a safe single-link regular file: ${error && error.code || 'unsafe-file'}`)
    }
  })
  const absolute = resolve(projectRoot, ADAPTER_CONFIG_RELATIVE_PATH)
  const bytes = reader(absolute)
  if (bytes === null) return { state: 'unconfigured' }
  if (!Buffer.isBuffer(bytes)) throw invalid('config reader must return bytes or null')
  if (bytes.length > CONFIG_BYTES_MAX) throw invalid(`config exceeds ${CONFIG_BYTES_MAX} bytes`)
  let document
  try { document = JSON.parse(bytes.toString('utf8')) } catch (error) {
    throw invalid(`config is not valid JSON: ${error.message}`)
  }
  const validated = validateAdapterConfig(document, { projectRoot, schemaValidate })
  return {
    state: 'configured',
    config: validated,
    configHash: validated.configHash,
    fileHash: bytesHash(bytes),
    tokenConfigHash: validated.tokenConfigHash,
    componentConfigHash: validated.componentConfigHash,
    // These are hashes of the canonical capability-local bytes. They are
    // intentionally insensitive to the other capability and to JSON trivia.
    tokenConfigFileHash: validated.tokenConfigHash,
    componentConfigFileHash: validated.componentConfigHash
  }
}
