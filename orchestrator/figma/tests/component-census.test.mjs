// component-census.test.mjs — CLI pins for scripts/component-census.mjs over
// env-override fixtures (CMP-CENSUS-*): every status family (MAPPED / MISSING
// with code candidates / INCOMPLETE / AMBIGUOUS / UNSUPPORTED / RETIRED /
// SOURCE_STALE), identity-by-node-id (two same-named sets stay two rows),
// instance assignment verification, the v2 bindings upsert, the
// mapping-consult digest recompute stability, and census.schema.json
// conformance. Deterministic: fixed FIGMA_PIPELINE_RUN_ID, temp dirs only.
import assert from 'node:assert/strict'
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import {
  validComponentCapture, COMPONENT_CAPTURE_HASH, validProjectComponentInventory,
  validComponentAnalysisIndex, validComponentRegistry, DESIGN_ID, PROJECT_ID, ADAPTER_ID
} from './component-fixtures.mjs'
import { normalizeCapture } from '../components/capture-normalizer.mjs'
import { projectInventorySemanticHash } from '../components/project-inventory-contract.mjs'
import { compileSchema } from '../scripts/report-utils.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const CENSUS = join(HERE, '..', 'scripts', 'component-census.mjs')
const DESIGN_COMPONENTS_LIB = join(HERE, '..', 'scripts', 'lib', 'design-components.mjs')
const STEM = 'TASK_1_fixture'
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

// ── fixture workspace ──────────────────────────────────────────────────────
const ws = mkdtempSync(join(tmpdir(), 'component-census-'))
const screensRoot = join(ws, 'screens')
const screensDir = join(screensRoot, STEM)
const reportsDir = join(ws, 'reports')
const codeRoot = join(ws, 'code')
const analysisDir = join(ws, 'analysis')
for (const dir of [screensDir, reportsDir, codeRoot, analysisDir]) mkdirSync(dir, { recursive: true })
writeFileSync(join(codeRoot, 'Badge.kt'), 'package fixture\n\n@Composable\nfun Badge() {\n}\n')

// Design inventory: the shared fixture scope + Chip (two active mappings ->
// AMBIGUOUS) + a same-named Dup pair (identity is the node id, not the label).
const capture = validComponentCapture()
capture.entities.push(
  { nodeId: '50:1', pageId: '0:1', kind: 'component', name: 'Chip', idQuality: 'stable', properties: [], variants: [], expectedVariantCount: 0, nestedRefs: [], boundVariables: [] },
  { nodeId: '60:1', pageId: '0:1', kind: 'component', name: 'Dup', idQuality: 'stable', properties: [], variants: [], expectedVariantCount: 0, nestedRefs: [], boundVariables: [] },
  { nodeId: '61:1', pageId: '0:1', kind: 'component', name: 'Dup', idQuality: 'stable', properties: [], variants: [], expectedVariantCount: 0, nestedRefs: [], boundVariables: [] }
)
capture.witness.expectedEntityCount = 7
capture.witness.readEntityCount = 7
const design = normalizeCapture(capture, COMPONENT_CAPTURE_HASH)
const designFile = join(ws, 'design-component-inventory.json')
writeFileSync(designFile, JSON.stringify(design, null, 2))

// Published analysis: AppButton's declared source must be readable on disk for
// the MAPPED verdict (source readability is part of the consult projection).
const project = validProjectComponentInventory()
const buttonSource = 'orchestrator/figma/tests/component-census.test.mjs'
project.components.find((component) => component.projectComponentId === PROJECT_ID.button).source.path = buttonSource
writeFileSync(join(analysisDir, `project-inventory-${ADAPTER_ID}.json`), JSON.stringify(project, null, 2))
const index = validComponentAnalysisIndex([project], projectInventorySemanticHash)
const indexFile = join(ws, 'analysis-index.json')
writeFileSync(indexFile, JSON.stringify(index, null, 2))

// Registry: Button active (MAPPED), Icon retired (RETIRED), Badge unmapped
// (MISSING + code candidate). A two-active-mapping half-state is exercised
// separately below and must fail closed at the strict registry reader.
const registry = validComponentRegistry(design.scopeId)
registry.mappings = registry.mappings.filter((mapping) => mapping.designComponentId !== DESIGN_ID.badge)
const iconMapping = registry.mappings.find((mapping) => mapping.designComponentId === DESIGN_ID.icon)
iconMapping.state = 'retired'
iconMapping.retirement = { reason: 'superseded by a shared icon set', actor: 'owner', at: '2026-07-20T11:30:00.000Z' }
const chipId = design.components.find((component) => component.providerIdentity.nodeId === '50:1').designComponentId
const registryFile = join(ws, 'component-mappings.json')
writeFileSync(registryFile, JSON.stringify(registry, null, 2))

// Screens: instances aggregated by owning node id; one stale id; one bad
// variant assignment sample on the mapped Button.
writeFileSync(join(screensDir, 'Home.instances.json'), JSON.stringify([
  { name: 'Button/Primary', componentSetName: 'Button', figmaNodeId: '10:1', variantProps: { Size: 'Huge', Ghost: 'on' } },
  { name: 'Button/Secondary', componentSetName: 'Button', figmaNodeId: '10:1', variantProps: { Size: 'Small' } },
  { name: 'Badge', componentSetName: 'Badge', figmaNodeId: '30:1', nodeUrl: 'https://www.figma.com/design/k/f?node-id=30-1' },
  { name: 'Icon', componentSetName: 'Icon', figmaNodeId: '20:1' },
  { name: 'Chip', componentSetName: 'Chip', figmaNodeId: '50:1' },
  { name: 'RemoteCard', componentSetName: 'RemoteCard', figmaNodeId: '40:1' },
  { name: 'Ghost', componentSetName: 'Ghost', figmaNodeId: '99:9' },
  { name: 'Dup A', componentSetName: 'Dup', figmaNodeId: '60:1' },
  { name: 'Dup B', componentSetName: 'Dup', figmaNodeId: '61:1' }
], null, 2))

const bindingsPath = join(screensDir, 'bindings.json')
writeFileSync(bindingsPath, JSON.stringify({ schemaVersion: 2, stem: STEM, screens: [{ screenName: 'Home', nodeId: '1:1' }] }, null, 2) + '\n')

const OUT = join(reportsDir, `census-${STEM}.json`)
const baseEnv = {
  ...process.env,
  FIGMA_REPORTS_DIR: reportsDir,
  FIGMA_CACHE_ROOT: join(ws, 'cache'),
  FIGMA_SPEC_SCREENS_DIR: screensRoot,
  FIGMA_PIPELINE_RUN_ID: 'component-census-fixture'
}
const baseFixtures = {
  inventory: designFile,
  mappings: registryFile,
  analysisIndex: indexFile,
  analysisDirectory: analysisDir
}
const runCensus = (fixtures = baseFixtures) => spawnSync(process.execPath,
  [CENSUS, STEM, '--screens-dir', screensRoot, '--code-root', codeRoot, '--out', OUT,
    '--fixture-component-inventory', fixtures.inventory,
    '--fixture-component-mappings', fixtures.mappings,
    '--fixture-component-analysis-index', fixtures.analysisIndex,
    '--fixture-component-analysis-dir', fixtures.analysisDirectory],
  { env: baseEnv, encoding: 'utf8' })

const result = runCensus()
const report = result.status === 0 ? JSON.parse(readFileSync(OUT, 'utf8')) : null
const rowOf = (rows, setNodeId) => rows.find((row) => row.setNodeId === setNodeId)
// compileSchema resolves the report-envelope $ref next to the schema file.
const validateCensus = await compileSchema(join(HERE, '..', 'token-schemas', 'census.schema.json'), { gate: true })

try {
  check('CMP-CENSUS: the census exits 0 (information, not a gate) with overall INCOMPLETE', () => {
    assert.equal(result.status, 0, result.stderr.slice(0, 400))
    assert.equal(report.overall, 'INCOMPLETE')
    assert.equal(report.pipelineRunId, 'component-census-fixture')
    assert.equal(report.taskStem, STEM)
  })

  check('CMP-CENSUS-MAPPED: an active fully-resolvable mapping reads MAPPED with its implementations', () => {
    const row = rowOf(report.components, '10:1')
    assert.equal(row.status, 'MAPPED')
    assert.equal(row.designComponentId, DESIGN_ID.button)
    assert.equal(row.mappingId, 'cmap-' + '1'.repeat(24))
    assert.deepEqual(row.implementations, [{
      adapterId: ADAPTER_ID,
      platform: 'android-compose',
      projectComponentId: PROJECT_ID.button,
      sourcePath: buttonSource
    }])
    assert.equal(row.instances, 2)
    assert.deepEqual(row.screens, ['Home'])
  })

  check('CMP-CENSUS-MISSING: an unmapped identity reads MISSING with exact-name code candidates', () => {
    const row = rowOf(report.components, '30:1')
    assert.equal(row.status, 'MISSING')
    assert.deepEqual(row.codeCandidates, [{ name: 'Badge', file: 'Badge.kt', line: 4 }])
    assert.equal(row.sourceNodeUrl, 'https://www.figma.com/design/k/f?node-id=30-1')
    assert.deepEqual(report.missing.includes('30:1'), true)
    assert.deepEqual(report.reuseCandidates, ['30:1'])
    assert.ok(report.issues.some((issue) => issue.issueKind === 'COMPONENT_CODE_CANDIDATE'))
  })

  check('CMP-CENSUS-RETIRED: screens still using a retired mapping read RETIRED', () => {
    const row = rowOf(report.components, '20:1')
    assert.equal(row.status, 'RETIRED')
    assert.deepEqual(row.retiredMappingIds, ['cmap-' + '2'.repeat(24)])
    assert.ok(report.retired.includes('20:1'))
  })

  check('CMP-CENSUS-CORRUPT: two active mappings on one identity fail closed before projection', () => {
    const corrupt = JSON.parse(JSON.stringify(registry))
    for (const digit of ['8', '9']) corrupt.mappings.push({
      mappingId: 'cmap-' + digit.repeat(24), designComponentId: chipId, expectedKind: 'component',
      implementations: [{ adapterId: ADAPTER_ID, platform: 'android-compose',
        projectScopeFingerprint: project.scopeFingerprint, relation: 'direct',
        projectComponentIds: [PROJECT_ID.spare], required: true }],
      propertyMappings: [], slotMappings: [], state: 'active',
      provenance: { kind: 'user-confirmed', actor: 'owner', at: '2026-07-20T11:00:00.000Z' }
    })
    const corruptFile = join(ws, 'component-mappings-corrupt.json')
    writeFileSync(corruptFile, JSON.stringify(corrupt, null, 2))
    const run = runCensus({ ...baseFixtures, mappings: corruptFile })
    assert.equal(run.status, 1)
    assert.match(run.stdout + run.stderr, /COMPONENT_MAPPING_INVALID.*two active mappings/)
  })

  check('CMP-CENSUS-UNSUPPORTED: an inventory-unsupported identity reads UNSUPPORTED', () => {
    assert.equal(rowOf(report.components, '40:1').status, 'UNSUPPORTED')
    assert.ok(report.unsupported.includes('40:1'))
  })

  check('CMP-CENSUS-STALE: an identity unknown to the inventory reads SOURCE_STALE with a null id', () => {
    const row = rowOf(report.components, '99:9')
    assert.equal(row.status, 'SOURCE_STALE')
    assert.equal(row.designComponentId, null)
    assert.ok(report.sourceStale.includes('99:9'))
  })

  check('CMP-CENSUS-IDENTITY: two same-named sets stay two rows keyed by node id', () => {
    const rows = report.components.filter((row) => row.setName === 'Dup')
    assert.deepEqual(rows.map((row) => row.setNodeId), ['60:1', '61:1'])
    assert.notEqual(rows[0].designComponentId, rows[1].designComponentId)
  })

  check('CMP-CENSUS-ASSIGN: undeclared instance variant values are an advisory review issue', () => {
    const issue = report.issues.find((item) => item.issueKind === 'COMPONENT_INSTANCE_ASSIGNMENT_INVALID')
    assert.ok(issue)
    assert.match(issue.message, /Ghost \(unknown property\)/)
    assert.match(issue.message, /Size=Huge \(outside declared options\)/)
    assert.equal(rowOf(report.components, '10:1').status, 'MAPPED', 'advisory issues never rewrite the status')
  })

  check('CMP-CENSUS-BINDINGS: MAPPED rows upsert v2 component bindings keyed by designComponentId', () => {
    const bindings = JSON.parse(readFileSync(bindingsPath, 'utf8'))
    assert.equal(bindings.schemaVersion, 2)
    assert.deepEqual(bindings.screens, [{ screenName: 'Home', nodeId: '1:1' }], 'builder-owned screen rows stay untouched')
    assert.deepEqual(bindings.components, [{
      designComponentId: DESIGN_ID.button,
      setNodeId: '10:1',
      setName: 'Button',
      mappingId: 'cmap-' + '1'.repeat(24),
      implementations: [{
        adapterId: ADAPTER_ID,
        platform: 'android-compose',
        projectComponentId: PROJECT_ID.button,
        sourcePath: buttonSource
      }]
    }])
  })

  check('CMP-CENSUS-CONSULT: the consult digest is stable across reruns and pins every consulted identity', () => {
    assert.deepEqual(report.mappingConsult.setIds, ['10:1', '20:1', '30:1', '40:1', '50:1', '60:1', '61:1', '99:9'])
    assert.equal(report.inputHashes['virtual:component-mappings-consulted'], report.mappingConsult.digest)
    const again = runCensus()
    assert.equal(again.status, 0)
    const secondReport = JSON.parse(readFileSync(OUT, 'utf8'))
    assert.equal(secondReport.mappingConsult.digest, report.mappingConsult.digest)
    assert.equal(JSON.stringify(secondReport.components), JSON.stringify(report.components))
  })

  check('CMP-CENSUS-CONSULT: computeMappingConsultDigest recomputes the identical digest from the live truth', () => {
    const recompute = spawnSync(process.execPath, ['--input-type=module', '-e', `
import { computeMappingConsultDigest } from ${JSON.stringify(CENSUS)}
import { loadDesignComponentInventory, loadComponentMappings, loadPublishedComponentAnalysis } from ${JSON.stringify(DESIGN_COMPONENTS_LIB)}
const options = { fixtureInventoryFile: ${JSON.stringify(designFile)}, fixtureMappingsFile: ${JSON.stringify(registryFile)}, fixtureAnalysisIndexFile: ${JSON.stringify(indexFile)}, fixtureAnalysisDirectory: ${JSON.stringify(analysisDir)} }
const design = loadDesignComponentInventory(options)
const mappings = loadComponentMappings(design.present ? design.inventory.scopeId : '', options)
const analysis = loadPublishedComponentAnalysis(options)
const truth = { inventory: design.present ? design.inventory : null, registry: mappings.registry, analysis }
process.stdout.write(computeMappingConsultDigest(${JSON.stringify(report.mappingConsult.setIds)}, truth))
`], { env: baseEnv, encoding: 'utf8' })
    assert.equal(recompute.status, 0, recompute.stderr.slice(0, 300))
    assert.equal(recompute.stdout.trim(), report.mappingConsult.digest)
  })

  check('CMP-CENSUS-SCHEMA: the report validates against token-schemas/census.schema.json', () => {
    const ok = validateCensus(report)
    assert.equal(ok, true, JSON.stringify((validateCensus.errors || []).slice(0, 5)))
  })

  check('CMP-CENSUS-INCOMPLETE: an absent published analysis turns MAPPED into INCOMPLETE', () => {
    const run = runCensus({ ...baseFixtures, analysisIndex: join(ws, 'absent-analysis.json') })
    assert.equal(run.status, 0, run.stderr.slice(0, 300))
    const incompleteReport = JSON.parse(readFileSync(OUT, 'utf8'))
    const row = rowOf(incompleteReport.components, '10:1')
    assert.equal(row.status, 'INCOMPLETE')
    assert.match(row.detail, /no published analysis/)
  })

  check('CMP-CENSUS-INCOMPLETE: a mapping pinned to an old adapter scope turns MAPPED into INCOMPLETE', () => {
    const moved = JSON.parse(JSON.stringify(registry))
    moved.mappings.find((mapping) => mapping.designComponentId === DESIGN_ID.button)
      .implementations[0].projectScopeFingerprint = 'sha256:' + '0'.repeat(64)
    const movedFile = join(ws, 'moved-mappings.json')
    writeFileSync(movedFile, JSON.stringify(moved))
    const run = runCensus({ ...baseFixtures, mappings: movedFile })
    assert.equal(run.status, 0, run.stderr.slice(0, 300))
    const movedReport = JSON.parse(readFileSync(OUT, 'utf8'))
    const row = rowOf(movedReport.components, '10:1')
    assert.equal(row.status, 'INCOMPLETE')
    assert.match(row.detail, /adapter scope moved/)
  })

  check('CMP-CENSUS-STALE: without any design inventory every identity reads SOURCE_STALE', () => {
    const run = runCensus({ ...baseFixtures, inventory: join(ws, 'absent-inventory.json') })
    assert.equal(run.status, 0, run.stderr.slice(0, 300))
    const staleReport = JSON.parse(readFileSync(OUT, 'utf8'))
    assert.ok(staleReport.components.every((row) => row.status === 'SOURCE_STALE'))
  })
} finally {
  rmSync(ws, { recursive: true, force: true })
}

console.log(`\ncomponent-census.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
