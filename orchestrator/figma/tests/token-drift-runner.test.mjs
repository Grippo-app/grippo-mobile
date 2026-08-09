import assert from 'node:assert/strict'
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { aggregateObservedTokens } from '../tokens/catalog-aggregator.mjs'
import { normalizeSourceCapture } from '../tokens/source-normalizer.mjs'
import { immutablePlan, validObservedCapture } from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const RUNNER = join(HERE, '..', 'runtime', 'run-plan.mjs')
let passed = 0

function check(name, fn) {
  try {
    fn()
    passed++
    console.log(`PASS ${name}`)
  } catch (error) {
    console.error(`FAIL ${name}\n  ${error.stack || error.message}`)
    process.exitCode = 1
  }
}

function writeJson(file, value) {
  mkdirSync(dirname(file), { recursive: true })
  writeFileSync(file, JSON.stringify(value, null, 2) + '\n')
}

function runPlan(stageDir, plan) {
  const file = join(stageDir, 'plan.json')
  writeJson(file, { ...plan, stageDir })
  const child = spawnSync(process.execPath, [RUNNER, '--plan', file], {
    encoding: 'utf8',
    env: {}
  })
  const lines = child.stdout.split('\n').filter((line) => line.trim())
  assert.ok(lines.length, child.stderr)
  return JSON.parse(lines.at(-1))
}

function projectAdapter() {
  return {
    schemaVersion: 2,
    adapters: [{
      id: 'fixture-json',
      kind: 'json-tokens',
      version: 2,
      enabled: true,
      capabilities: ['tokens'],
      platform: 'shared',
      authority: 'handwritten',
      tokens: {
        roots: ['design/tokens'],
        include: ['**/*.json'],
        exclude: [],
        modes: ['shared'],
        authorities: {
          color: { contracts: ['AppColor'] },
          dimension: { contracts: ['AppDimension'] }
        },
        contextMap: [{
          when: { theme: 'light', locale: 'default', platform: 'shared' },
          projectMode: 'shared'
        }],
        bindingRules: [{
          ruleId: 'app-colors',
          kind: 'prefix-map',
          tokenKind: 'color',
          providerPrefix: ['color'],
          projectPrefix: ['AppColor'],
          caseTransform: 'preserve',
          excludeExact: [],
          excludePrefix: []
        }]
      }
    }]
  }
}

function observedDomain() {
  const capture = validObservedCapture()
  const bytes = Buffer.from(JSON.stringify(capture))
  const batch = normalizeSourceCapture(capture, bytes, immutablePlan(capture))
  return aggregateObservedTokens({
    scope: {
      fileKeyFingerprint: capture.source.fileKeyFingerprint,
      branchKey: capture.source.branchKey
    },
    batches: [batch],
    revision: 1
  })
}

check('token drift runner publishes observed binding/comparison artifacts', () => {
  const project = mkdtempSync(join(tmpdir(), 'token-drift-project-'))
  const stage = mkdtempSync(join(tmpdir(), 'token-drift-stage-'))
  try {
    writeJson(join(project, 'orchestrator', 'figma', 'project-adapters.json'), projectAdapter())
    writeJson(join(project, 'design', 'tokens', 'tokens.json'), {
      AppColor: { content: { primary: { value: '#336699' } } },
      AppDimension: { md: { value: 16, unit: 'dp' } }
    })
    const domain = observedDomain()
    writeJson(join(stage, 'inputs', 'catalog.json'), domain.catalog)
    writeJson(join(stage, 'inputs', 'source-index.json'), domain.index)
    const result = runPlan(stage, {
      op: 'token-compare',
      projectRoot: project,
      observedCatalogFile: 'inputs/catalog.json',
      sourceIndexFile: 'inputs/source-index.json',
      sourceFreshness: 'current',
      eligibleAt: '2026-07-23T12:00:00.000Z'
    })
    assert.equal(result.ok, true, JSON.stringify(result))
    assert.deepEqual(result.artifacts.slice().sort(), [
      'analysis-index.json',
      'baseline.json',
      'binding-snapshot.json',
      'comparison.json',
      'mapping-snapshot.json',
      'project-inventory-fixture-json.json'
    ])
    const binding = JSON.parse(readFileSync(join(stage, 'binding-snapshot.json'), 'utf8'))
    assert.equal(binding.bindings.length, 1)
    assert.equal(binding.bindings[0].projectTokenId, 'fixture-json:AppColor.content.primary')
    const comparison = JSON.parse(readFileSync(join(stage, 'comparison.json'), 'utf8'))
    const color = comparison.observedRows.find((row) => row.providerName === 'color/content/primary')
    assert.equal(color.bindingStatus, 'auto-bound')
    assert.equal(color.valueStatus, 'matched')
    assert.equal(comparison.inputs.sourceIndexHash, domain.index.semanticHash)
    const baseline = JSON.parse(readFileSync(join(stage, 'baseline.json'), 'utf8'))
    assert.equal(baseline.source.comparisonSemanticHash, comparison.semanticHash)
    assert.equal(baseline.entries.length, 1)
  } finally {
    rmSync(project, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('unconfigured adapter state fails with PROJECT_ADAPTERS_UNCONFIGURED and no heuristic scan', () => {
  const project = mkdtempSync(join(tmpdir(), 'token-drift-unconfigured-project-'))
  const stage = mkdtempSync(join(tmpdir(), 'token-drift-unconfigured-stage-'))
  try {
    const domain = observedDomain()
    writeJson(join(stage, 'inputs', 'catalog.json'), domain.catalog)
    writeJson(join(stage, 'inputs', 'source-index.json'), domain.index)
    const result = runPlan(stage, {
      op: 'token-compare',
      projectRoot: project,
      observedCatalogFile: 'inputs/catalog.json',
      sourceIndexFile: 'inputs/source-index.json',
      sourceFreshness: 'current',
      eligibleAt: '2026-07-23T12:00:00.000Z'
    })
    assert.equal(result.ok, false)
    assert.equal(result.code, 'PROJECT_ADAPTERS_UNCONFIGURED')
  } finally {
    rmSync(project, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('unknown Source Health emits a read-only comparison and no baseline', () => {
  const project = mkdtempSync(join(tmpdir(), 'token-drift-unknown-health-project-'))
  const stage = mkdtempSync(join(tmpdir(), 'token-drift-unknown-health-stage-'))
  try {
    writeJson(join(project, 'orchestrator', 'figma', 'project-adapters.json'), projectAdapter())
    writeJson(join(project, 'design', 'tokens', 'tokens.json'), {
      AppColor: { content: { primary: { value: '#336699' } } }
    })
    const domain = observedDomain()
    writeJson(join(stage, 'inputs', 'catalog.json'), domain.catalog)
    writeJson(join(stage, 'inputs', 'source-index.json'), domain.index)
    const result = runPlan(stage, {
      op: 'token-compare',
      projectRoot: project,
      observedCatalogFile: 'inputs/catalog.json',
      sourceIndexFile: 'inputs/source-index.json',
      sourceFreshness: 'unknown',
      eligibleAt: '2026-07-23T12:00:00.000Z'
    })
    assert.equal(result.ok, true, JSON.stringify(result))
    assert.equal(result.baselinePublished, false)
    assert.equal(result.artifacts.includes('baseline.json'), false)
    const comparison = JSON.parse(readFileSync(join(stage, 'comparison.json'), 'utf8'))
    assert.equal(comparison.inputs.sourceFreshness, 'unknown')
    assert.equal(comparison.complete, false)
    assert.ok(comparison.blockers.some((row) => row.code === 'TOKEN_SOURCE_HEALTH_UNAVAILABLE'))
  } finally {
    rmSync(project, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('retired designInventoryFile plans fail the exact runner contract', () => {
  const stage = mkdtempSync(join(tmpdir(), 'token-drift-stage-'))
  try {
    const result = runPlan(stage, {
      op: 'token-compare',
      projectRoot: stage,
      designInventoryFile: 'inputs/retired.json',
      designGenerationId: 'gen-' + 'a'.repeat(32),
      eligibleAt: '2026-07-23T12:00:00.000Z'
    })
    assert.equal(result.ok, false)
    assert.equal(result.code, 'RUN_PLAN_INVALID')
  } finally {
    rmSync(stage, { recursive: true, force: true })
  }
})

if (!process.exitCode) console.log(`token drift runner: ${passed} checks passed`)
