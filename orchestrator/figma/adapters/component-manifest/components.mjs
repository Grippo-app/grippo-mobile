// component-manifest adapter: extracts a project component inventory from
// explicit JSON component manifests (the non-Compose proof adapter,
// FIGMA_COMPONENTS.md §14/§33.5). The manifest is declarative and exact —
// nothing is inferred from file names, and a malformed manifest is a parse
// failure, never an empty inventory.
//
// Manifest shape (one JSON file, strict):
// {
//   "schemaVersion": 1,
//   "components": [{
//     "name": "Button", "symbol": "ui/button", "visibility": "public",
//     "props": [
//       { "name": "size", "kind": "enum", "values": ["small","large"], "default": "small" },
//       { "name": "disabled", "kind": "boolean", "default": false },
//       { "name": "label", "kind": "text", "required": true },
//       { "name": "icon", "kind": "content", "required": false },
//       { "name": "onClick", "kind": "callback", "required": true }
//     ],
//     "uses": { "components": ["ui/icon"], "tokens": ["<exact projectTokenId>"], "framework": ["react"] },
//     "evidence": { "screenshotTests": ["src/button.test.tsx"] },
//     "deprecated": false
//   }]
// }
import { COMPONENT_LIMITS } from '../../components/limits.mjs'

export const COMPONENT_MANIFEST_EXTRACTOR_VERSION = 'component-manifest-v2'

const NAME_RE = /^[^\u0000-\u001f]{1,300}$/
const SYMBOL_RE = /^[^\u0000-\u001f]{1,320}$/
const PROP_KINDS = new Set(['enum', 'boolean', 'text', 'content', 'callback', 'value'])
const isRecord = (value) => !!value && typeof value === 'object' && !Array.isArray(value)

function manifestError(component, detail) {
  return `${component ? `component ${JSON.stringify(component)}: ` : ''}${detail}`
}

function buildComponent(row, adapterId, filePath, fileHash, resolveComponent) {
  const problem = (detail) => { throw new Error(manifestError(row && row.name, detail)) }
  if (!isRecord(row)) problem('component row must be an object')
  if (typeof row.name !== 'string' || !NAME_RE.test(row.name)) problem('name must be a bounded string')
  if (typeof row.symbol !== 'string' || !SYMBOL_RE.test(row.symbol)) problem('symbol must be a bounded string')
  if (row.visibility !== 'public' && row.visibility !== 'internal') problem('visibility must be public or internal')

  const apiParameters = []
  const variantProperties = []
  const slots = []
  const props = row.props === undefined ? [] : row.props
  if (!Array.isArray(props) || props.length > COMPONENT_LIMITS.parametersPerComponentMax) problem('props must be a bounded array')
  const seenProps = new Set()
  for (const prop of props) {
    if (!isRecord(prop) || typeof prop.name !== 'string' || !NAME_RE.test(prop.name)) problem('prop name must be a bounded string')
    if (seenProps.has(prop.name)) problem(`prop ${JSON.stringify(prop.name)} declared twice`)
    seenProps.add(prop.name)
    if (!PROP_KINDS.has(prop.kind)) problem(`prop ${JSON.stringify(prop.name)} kind must be one of enum/boolean/text/content/callback/value`)
    const required = prop.required === true
    const hasDefault = prop.default !== undefined
    if (required && hasDefault) problem(`prop ${JSON.stringify(prop.name)} cannot be required and defaulted at once`)
    const propertyId = `param:${prop.name}`
    if (prop.kind === 'enum') {
      if (!Array.isArray(prop.values) || !prop.values.length ||
          prop.values.length > COMPONENT_LIMITS.projectVariantValuesPerPropertyMax ||
          prop.values.some((value) => typeof value !== 'string' || !value || value.length > 200)) {
        problem(`prop ${JSON.stringify(prop.name)} enum requires bounded string values`)
      }
      if (new Set(prop.values).size !== prop.values.length) problem(`prop ${JSON.stringify(prop.name)} lists duplicate values`)
      if (hasDefault && (typeof prop.default !== 'string' || !prop.values.includes(prop.default))) {
        problem(`prop ${JSON.stringify(prop.name)} default is outside its values`)
      }
      variantProperties.push({
        projectPropertyId: propertyId,
        name: prop.name,
        source: 'enum',
        values: prop.values.map((value) => ({ value })),
        ...(hasDefault ? { defaultValue: prop.default } : {}),
        defaultKnown: hasDefault
      })
    } else if (prop.kind === 'boolean') {
      if (hasDefault && typeof prop.default !== 'boolean') problem(`prop ${JSON.stringify(prop.name)} boolean default must be boolean`)
      variantProperties.push({
        projectPropertyId: propertyId,
        name: prop.name,
        source: 'boolean',
        values: [{ value: 'false' }, { value: 'true' }],
        ...(hasDefault ? { defaultValue: String(prop.default) } : {}),
        defaultKnown: hasDefault
      })
    } else if (prop.kind === 'text' || prop.kind === 'content' || prop.kind === 'callback') {
      slots.push({ slotId: propertyId, kind: prop.kind, name: prop.name, required })
    }
    apiParameters.push({
      name: prop.name,
      kind: prop.kind === 'enum' || prop.kind === 'boolean' || prop.kind === 'value' ? 'value'
        : prop.kind === 'content' ? 'content-lambda'
        : prop.kind,
      typeText: prop.kind,
      required,
      hasDefault,
      ...(hasDefault ? { defaultText: String(prop.default).slice(0, 500) } : {})
    })
  }

  const dependencies = []
  const uses = row.uses === undefined ? {} : row.uses
  if (!isRecord(uses)) problem('uses must be an object')
  for (const symbol of uses.components || []) {
    if (typeof symbol !== 'string' || !SYMBOL_RE.test(symbol)) problem('uses.components entries must be bounded strings')
    const target = resolveComponent(symbol)
    dependencies.push({
      kind: 'component',
      ...(target ? { targetProjectComponentId: target } : {}),
      symbol: symbol.slice(0, 320)
    })
  }
  for (const projectTokenId of uses.tokens || []) {
    if (typeof projectTokenId !== 'string' || projectTokenId.length < 3 || projectTokenId.length > 400) {
      problem('uses.tokens entries must be exact projectTokenId strings')
    }
    dependencies.push({ kind: 'token', projectTokenId })
  }
  for (const symbol of uses.framework || []) {
    if (typeof symbol !== 'string' || !SYMBOL_RE.test(symbol)) problem('uses.framework entries must be bounded strings')
    dependencies.push({ kind: 'framework', symbol: symbol.slice(0, 320) })
  }
  if (dependencies.length > COMPONENT_LIMITS.dependencyEdgesPerComponentMax) problem('dependency edges exceed the limit')

  const evidence = { previews: [], screenshotTests: [] }
  if (row.evidence !== undefined) {
    if (!isRecord(row.evidence)) problem('evidence must be an object')
    for (const path of row.evidence.screenshotTests || []) {
      if (typeof path !== 'string' || !path || path.length > 300) problem('evidence.screenshotTests entries must be bounded paths')
      evidence.screenshotTests.push({ path })
    }
  }

  return {
    projectComponentId: `${adapterId}:symbol:${row.symbol}`,
    name: row.name,
    fqName: row.symbol.slice(0, 320),
    kind: 'manifest-component',
    visibility: row.visibility,
    source: { path: filePath, line: 1, symbol: row.symbol.slice(0, 320), fileHash },
    api: { parameters: apiParameters },
    variantProperties,
    combinationsKnown: 'all',
    slots,
    dependencies,
    evidence,
    ...(row.deprecated === true ? { deprecated: true } : {})
  }
}

// options: { files, componentsConfig, adapterId, platform }
export function extractComponents(options) {
  const { adapterId } = options
  const files = options.files.slice().sort((a, b) => (a.path < b.path ? -1 : a.path > b.path ? 1 : 0))
  const parseFailures = []
  const limitations = new Set()
  const rowsByFile = []
  const symbolToId = new Map()

  for (const file of files) {
    let manifest
    try { manifest = JSON.parse(file.text) } catch (error) {
      parseFailures.push({ path: file.path, reason: `manifest is not valid JSON: ${error.message}`.slice(0, 300) })
      continue
    }
    if (!isRecord(manifest) || manifest.schemaVersion !== 2 || !Array.isArray(manifest.components)) {
      parseFailures.push({ path: file.path, reason: 'manifest must be { schemaVersion: 2, components: [...] }' })
      continue
    }
    rowsByFile.push({ file, manifest })
    for (const row of manifest.components) {
      if (isRecord(row) && typeof row.symbol === 'string') {
        symbolToId.set(row.symbol, `${adapterId}:symbol:${row.symbol}`)
      }
    }
  }

  const components = []
  for (const { file, manifest } of rowsByFile) {
    for (const row of manifest.components) {
      try {
        components.push(buildComponent(row, adapterId, file.path, file.hash,
          (symbol) => symbolToId.get(symbol) || null))
      } catch (error) {
        parseFailures.push({ path: file.path, reason: String(error.message).slice(0, 300) })
      }
    }
  }

  components.sort((a, b) => (a.projectComponentId < b.projectComponentId ? -1 : a.projectComponentId > b.projectComponentId ? 1 : 0))
  parseFailures.sort((a, b) => (a.path < b.path ? -1 : a.path > b.path ? 1 : 0))
  return { components, parseFailures, limitations: [...limitations].sort().slice(0, 64) }
}
