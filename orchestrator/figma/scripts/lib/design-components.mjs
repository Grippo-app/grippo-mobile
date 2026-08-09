// lib/design-components.mjs — the ONE script-side reader of the published
// design-side component truth and the project-owned component mapping
// registry. Resolution uses the shared exact generation artifact reader:
// generation pointer walk for published artifacts, direct strict read for the
// project-owned registry. No fallback: a missing pointer or role is the empty
// state; anything malformed throws COMPONENT_GENERATION_RESYNC_REQUIRED /
// COMPONENT_MAPPING_INVALID, never a substitute file.
//
// Tests pass fixture paths explicitly through the exported options object.
// Production callers never inspect process environment variables for truth.
import { createHash } from 'node:crypto'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import Ajv from 'ajv'
import { PROJECT_ROOT } from '../_util.mjs'
import { readGenerationArtifact } from './generation-artifacts.mjs'
import { inventorySemanticError } from '../../components/design-inventory-contract.mjs'
import { mappingRegistrySemanticError, emptyMappingRegistry } from '../../components/mapping-contract.mjs'
import {
  projectInventorySemanticError,
  analysisIndexSemanticError
} from '../../components/project-inventory-contract.mjs'

const sha256 = (bytes) => 'sha256:' + createHash('sha256').update(bytes).digest('hex')
const resync = (detail) => new Error(`COMPONENT_GENERATION_RESYNC_REQUIRED: ${detail}`)
const mappingInvalid = (detail) => new Error(`COMPONENT_MAPPING_INVALID: ${detail}`)
const ajv = new Ajv({ allErrors: true, strict: false })
const schema = (name) => JSON.parse(readFileSync(new URL(`../../schemas/${name}`, import.meta.url), 'utf8'))
const validateDesignInventory = ajv.compile(schema('design-component-inventory.schema.json'))
const validateMappings = ajv.compile(schema('component-mappings.schema.json'))
const validateAnalysisIndex = ajv.compile(schema('project-component-analysis-index.schema.json'))
const validateProjectInventory = ajv.compile(schema('project-component-inventory.schema.json'))

function parseInventory(bytes, origin) {
  let inventory
  try { inventory = JSON.parse(bytes.toString('utf8')) } catch (error) { throw resync(`${origin} is not valid JSON (${error.message})`) }
  if (!validateDesignInventory(inventory)) throw resync(`${origin} does not satisfy the exact design-component-inventory schemaVersion 2 contract`)
  const semantic = inventorySemanticError(inventory)
  if (semantic) throw resync(`${origin} violates the design-component-inventory semantic contract (${semantic})`)
  return inventory
}

export function loadDesignComponentInventory(options = {}) {
  const fixture = options.fixtureInventoryFile
  if (fixture) {
    if (!existsSync(fixture)) return { present: false }
    const bytes = readFileSync(fixture)
    return { present: true, inventory: parseInventory(bytes, `fixture ${fixture}`), generationId: null, artifactHash: sha256(bytes) }
  }
  const hit = readGenerationArtifact('design-component-inventory', resync)
  if (!hit.present) return { present: false }
  return { present: true, inventory: parseInventory(hit.bytes, 'generation artifact design-component-inventory'), generationId: hit.generationId, artifactHash: hit.artifactHash }
}

// The project-owned component mapping registry. Absent file = the exact
// revision-0 empty registry bound to `scopeId` (a predefined state); a
// malformed file blocks, never reads as empty.
export function loadComponentMappings(scopeId, options = {}) {
  const fixture = options.fixtureMappingsFile
  const path = fixture || join(PROJECT_ROOT, 'orchestrator', 'figma', 'component-mappings.json')
  if (!existsSync(path)) {
    return {
      present: false,
      registry: emptyMappingRegistry(String(scopeId || ''))
    }
  }
  const bytes = readFileSync(path)
  let registry
  try { registry = JSON.parse(bytes.toString('utf8')) } catch (error) { throw mappingInvalid(`component-mappings.json is not valid JSON (${error.message})`) }
  if (!validateMappings(registry)) throw mappingInvalid('component-mappings.json does not satisfy the exact schemaVersion 2 registry contract')
  const semantic = mappingRegistrySemanticError(registry)
  if (semantic) throw mappingInvalid(semantic)
  return { present: true, registry, registryHash: sha256(bytes) }
}

// Committed provenance entries for the done-gate backstop
// (design-parser.cjs uiTaskWithoutDesign): one row per mapped implementation
// target, carrying the SIMPLE component name, its repo-relative source path,
// and the owning design node id. A retired mapping keeps its rows with a
// figmaNodeRetired record so the backstop can note — but no longer hold —
// the anchor. Truth = active design inventory + mapping registry + the last
// published project analysis; absence of any of them is the empty list, a
// malformed one throws.
export function componentProvenanceEntries(options = {}) {
  const design = loadDesignComponentInventory(options)
  if (!design.present) return []
  const mappings = loadComponentMappings(design.inventory.scopeId, options)
  const analysis = loadPublishedComponentAnalysis(options)
  const designById = new Map(design.inventory.components.map((component) => [component.designComponentId, component]))
  const entries = []
  for (const mapping of mappings.registry.mappings) {
    const designRow = designById.get(mapping.designComponentId)
    if (!designRow) continue
    const retired = mapping.state === 'retired' && mapping.retirement
      ? { reason: String(mapping.retirement.reason || '').slice(0, 500), at: mapping.retirement.at || null, by: mapping.retirement.actor || null }
      : null
    for (const implementation of mapping.implementations) {
      if (implementation.relation === 'external') continue
      const inventoryForAdapter = analysis.present ? analysis.inventories.get(implementation.adapterId) : null
      for (const projectComponentId of implementation.projectComponentIds) {
        const projectComponent = inventoryForAdapter
          ? inventoryForAdapter.components.find((row) => row.projectComponentId === projectComponentId)
          : null
        const symbol = String(projectComponentId).split(':symbol:')[1] || ''
        const simpleName = symbol.split('#')[0].split('.').pop() || symbol
        const entry = {
          component: simpleName,
          source: projectComponent ? projectComponent.source.path : null,
          figmaNodeId: designRow.providerIdentity.nodeId
        }
        if (retired) entry.figmaNodeRetired = retired
        entries.push(entry)
      }
    }
  }
  return entries
}

// Owner render-class policy per owning design node id: the screenshot gate's
// routing source. Reads the ACTIVE mapping's visualPolicy (canvas|glass; a
// null/absent policy is strict). Design inventory absence = empty map.
export function renderClassByNodeId(options = {}) {
  const design = loadDesignComponentInventory(options)
  if (!design.present) return new Map()
  const mappings = loadComponentMappings(design.inventory.scopeId, options)
  const nodeByDesignId = new Map(design.inventory.components.map((component) =>
    [component.designComponentId, component.providerIdentity.nodeId]))
  const out = new Map()
  for (const mapping of mappings.registry.mappings) {
    if (mapping.state !== 'active' || !mapping.visualPolicy) continue
    const renderClass = mapping.visualPolicy.renderClass
    if (renderClass !== 'canvas' && renderClass !== 'glass') continue
    const nodeId = nodeByDesignId.get(mapping.designComponentId)
    if (nodeId) out.set(String(nodeId), renderClass)
  }
  return out
}

// The last published project component analysis: index + per-adapter
// inventories, all hash-bound to the active generation. { present: false }
// when no analysis was ever published.
export function loadPublishedComponentAnalysis(options = {}) {
  const fixtureIndex = options.fixtureAnalysisIndexFile
  let index
  let generationId = null
  const inventories = new Map()
  if (fixtureIndex) {
    if (!existsSync(fixtureIndex)) return { present: false }
    try { index = JSON.parse(readFileSync(fixtureIndex, 'utf8')) }
    catch (error) { throw resync(`component analysis index fixture is not valid JSON (${error.message})`) }
    const directory = options.fixtureAnalysisDirectory || ''
    if (!validateAnalysisIndex(index)) throw resync('component analysis index fixture does not satisfy the exact schemaVersion 2 contract')
    for (const row of index.adapters) {
      const file = directory ? join(directory, `project-inventory-${row.adapterId}.json`) : ''
      if (!file || !existsSync(file)) throw resync(`component analysis fixture cites adapter ${row.adapterId} but its inventory is absent`)
      let inventory
      try { inventory = JSON.parse(readFileSync(file, 'utf8')) }
      catch (error) { throw resync(`project component inventory fixture ${row.adapterId} is not valid JSON (${error.message})`) }
      inventories.set(row.adapterId, inventory)
    }
  } else {
    const indexHit = readGenerationArtifact('project-component-analysis-index', resync)
    if (!indexHit.present) return { present: false }
    generationId = indexHit.generationId
    try { index = JSON.parse(indexHit.bytes.toString('utf8')) }
    catch (error) { throw resync(`component analysis index is not valid JSON (${error.message})`) }
    if (!validateAnalysisIndex(index)) throw resync('component analysis index does not satisfy the exact schemaVersion 2 contract')
    for (const row of index.adapters) {
      const hit = readGenerationArtifact(`project-component-inventory:${row.adapterId}`, resync)
      if (!hit.present) throw resync(`published analysis cites adapter ${row.adapterId} but its inventory artifact is absent`)
      let inventory
      try { inventory = JSON.parse(hit.bytes.toString('utf8')) }
      catch (error) { throw resync(`project component inventory ${row.adapterId} is not valid JSON (${error.message})`) }
      inventories.set(row.adapterId, inventory)
    }
  }
  for (const [adapterId, inventory] of inventories) {
    if (!validateProjectInventory(inventory)) throw resync(`project component inventory ${adapterId} does not satisfy the exact schemaVersion 2 contract`)
    const semantic = projectInventorySemanticError(inventory)
    if (semantic) throw resync(`project component inventory ${adapterId} violates its semantic contract (${semantic})`)
  }
  const indexSemantic = analysisIndexSemanticError(index, inventories, {
    enabledAdapterIds: index.adapters.map((row) => row.adapterId), configHash: index.configHash
  })
  if (indexSemantic) throw resync(`component analysis index violates its semantic contract (${indexSemantic})`)
  return { present: true, index, inventories, generationId }
}
