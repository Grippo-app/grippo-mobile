// Fixture self-test for token/report schema compile closure.
// Ensures report schemas with local $ref dependencies are actually loadable by
// the shared compile helper used by gate scripts.
import { mkdtempSync, readdirSync, readFileSync, rmSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { execFileSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { assertTaskStem, compileSchema } from '../scripts/report-utils.mjs'
import { loadScreenshotThresholds } from '../scripts/_util.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const SCHEMAS = join(HERE, '..', 'token-schemas')
const SEED = join(HERE, '..', 'scripts', 'seed-evidence-fixture.mjs')
const GATE_POLICY_VERSION = loadScreenshotThresholds().version
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

try {
  check('report writers accept only canonical safe-integer task stems', () => {
    assert.equal(assertTaskStem('TASK_1_fixture'), 'TASK_1_fixture')
    for (const stem of ['stem', 'TASK_01_leading_zero', 'TASK_9007199254740992_unsafe']) {
      assert.throws(() => assertTaskStem(stem), /canonical safe-integer task stem/)
    }
  })

  const files = readdirSync(SCHEMAS).filter((f) => f.endsWith('.schema.json')).sort()
  for (const file of files) {
    const validate = await compileSchema(join(SCHEMAS, file), { gate: true })
    check(`${file} compiles`, () => assert.equal(typeof validate, 'function'))
  }

  const screenSpec = await compileSchema(join(SCHEMAS, 'spec.schema.json'), { gate: true })
  const screenSpecFixture = JSON.parse(readFileSync(join(HERE, 'spec', 'spec-valid.json'), 'utf8'))
  check('screen spec requires durable componentSetNodeId alongside every componentSetName display label', () => {
    assert.equal(screenSpec(screenSpecFixture), true, JSON.stringify(screenSpec.errors || []))
    const nameOnly = structuredClone(screenSpecFixture)
    Object.assign(nameOnly.nodes[1], { componentSetName: 'PrimaryButton' })
    Object.assign(nameOnly.elements[0], { componentSetName: 'PrimaryButton' })
    assert.equal(screenSpec(nameOnly), false, 'display name alone must never become component authority')
    const idOnly = structuredClone(screenSpecFixture)
    Object.assign(idOnly.nodes[1], { componentSetNodeId: '1:30' })
    Object.assign(idOnly.elements[0], { componentSetNodeId: '1:30' })
    assert.equal(screenSpec(idOnly), false, 'the durable id must retain its display evidence for diagnostics')
    const exact = structuredClone(screenSpecFixture)
    Object.assign(exact.nodes[1], { componentSetName: 'PrimaryButton', componentSetNodeId: '1:30' })
    Object.assign(exact.elements[0], { componentSetName: 'PrimaryButton', componentSetNodeId: '1:30' })
    assert.equal(screenSpec(exact), true, JSON.stringify(screenSpec.errors || []))
    const malformedId = structuredClone(exact)
    malformedId.nodes[1].componentSetNodeId = 'PrimaryButton'
    malformedId.elements[0].componentSetNodeId = 'PrimaryButton'
    assert.equal(screenSpec(malformedId), false, 'name-shaped values cannot occupy the stable set-id field')
  })

  const envelope = await compileSchema(join(SCHEMAS, 'report-envelope.schema.json'), { gate: true })
  check('report-envelope accepts transport reports', () => assert.equal(envelope({
    schemaVersion: 1,
    gatePolicyVersion: GATE_POLICY_VERSION,
    taskStem: 'TASK_1_fixture',
    pipelineRunId: 'schema-test-run',
    mode: 'transport',
    inputs: {},
    inputHashes: {},
    overall: 'PASS',
    blockingCount: 0,
    warningCount: 0,
    issues: [],
    reportPath: 'reports/evidence-TASK_1_fixture.json',
  }), true, JSON.stringify(envelope.errors || [])))

  const generationManifest = await compileSchema(join(SCHEMAS, 'generation-manifest.schema.json'), { gate: true })
  const generationId = `gen-${'1'.repeat(32)}`
  const generationHash = (char) => `sha256:${char.repeat(64)}`
  const tokenIndexArtifact = {
    role: 'observed-token-source-index',
    group: 'tokens',
    domain: 'tokens',
    path: `orchestrator/figma/manifests/artifacts/${generationId}/tokens/source-index.json`,
    logicalPath: 'orchestrator/figma/tokens/source-index.json',
    hash: generationHash('a'),
    schemaVersion: 1,
    persistence: 'committed',
    required: true,
    size: 10,
  }
  const tokenCatalogArtifact = {
    ...tokenIndexArtifact,
    role: 'observed-token-catalog',
    path: `orchestrator/figma/manifests/artifacts/${generationId}/tokens/observed-token-catalog.json`,
    logicalPath: 'orchestrator/figma/tokens/observed-token-catalog.json',
    hash: generationHash('9'),
  }
  const tokenArtifacts = [tokenIndexArtifact, tokenCatalogArtifact]
  const componentArtifact = {
    ...tokenIndexArtifact,
    role: 'design-component-inventory',
    group: 'components',
    domain: 'components',
    path: `orchestrator/figma/manifests/artifacts/${generationId}/components/design-component-inventory.json`,
    logicalPath: 'orchestrator/figma/components/design-component-inventory.json',
    hash: generationHash('e'),
  }
  const generationFixture = {
    schemaVersion: 2,
    generationId,
    accountFingerprint: generationHash('c'),
    fileKeyFingerprint: generationHash('d'),
    createdAt: '2026-01-01T00:00:00.000Z',
    syncJobId: `fsj-${'2'.repeat(32)}`,
    updatedDomains: ['tokens'],
    syncGroups: { tokens: { status: 'completed', updated: 2, unchanged: 0, warnings: 0 } },
    groups: ['tokens'],
    domains: [{
      id: 'tokens', group: 'tokens', inputFingerprint: generationHash('b'),
      syncedAt: '2026-01-01T00:00:00.000Z', sourceGenerationId: generationId,
    }],
    artifacts: tokenArtifacts,
    counters: { updated: 2, unchanged: 0, warnings: 0 },
  }
  check('generation manifest accepts a declared artifact group', () => assert.equal(
    generationManifest(generationFixture), true, JSON.stringify(generationManifest.errors || [])))
  check('generation manifest rejects artifacts from undeclared groups', () => assert.equal(generationManifest({
    ...generationFixture,
    artifacts: tokenArtifacts.concat([componentArtifact]),
  }), false))
  check('generation manifest accepts complete domain provenance', () => assert.equal(
    generationManifest(generationFixture), true, JSON.stringify(generationManifest.errors || [])))
  check('generation manifest rejects incomplete or malformed domain provenance', () => {
    assert.equal(generationManifest({
      ...generationFixture,
      updatedDomains: undefined,
    }), false)
    assert.equal(generationManifest({
      ...generationFixture,
      updatedDomains: ['components'],
      syncGroups: { components: { status: 'completed', updated: 1, unchanged: 0, warnings: 0 } },
    }), false)
    assert.equal(generationManifest({
      ...generationFixture,
      syncGroups: { tokens: { status: 'failed', updated: 1, unchanged: 0, warnings: 0 } },
    }), false)
    assert.equal(generationManifest({
      ...generationFixture,
      syncGroups: { tokens: { status: 'running', updated: 1, unchanged: 0, warnings: 0 } },
    }), false)
    assert.equal(generationManifest({
      ...generationFixture,
      groups: ['tokens', 'components'],
      domains: generationFixture.domains.concat([{
        id: 'components', group: 'components', inputFingerprint: generationHash('f'),
        syncedAt: '2026-01-01T00:00:00.000Z', sourceGenerationId: generationId,
      }]),
      artifacts: tokenArtifacts.concat([componentArtifact]),
      counters: { updated: 1, unchanged: 1, warnings: 0 },
      syncGroups: { tokens: { status: 'completed', updated: 1, unchanged: 0, warnings: 0 } },
    }), true, JSON.stringify(generationManifest.errors || []))
  })

  // The token comparison pipeline publishes its whole domain under 'token-drift':
  // project-token-analysis-index + token-mapping-snapshot + token-comparison, all committed under
  // the token-comparison report dir. This pins the schema's token-drift domain constraints.
  const tokenComparisonDir = 'orchestrator/.cache/figma/reports/token-comparison/'
  const tokenDriftArtifact = (role, file, hashChar) => ({
    role,
    group: 'drift',
    domain: 'token-drift',
    path: `orchestrator/figma/manifests/artifacts/${generationId}/drift/${file}`,
    logicalPath: `${tokenComparisonDir}${file}`,
    hash: generationHash(hashChar),
    schemaVersion: 1,
    persistence: 'committed',
    required: true,
    size: 10,
  })
  const tokenDriftFixture = {
    ...generationFixture,
    groups: ['tokens', 'drift'],
    updatedDomains: ['tokens', 'token-drift'],
    syncGroups: {
      tokens: { status: 'completed', updated: 2, unchanged: 0, warnings: 0 },
      drift: { status: 'completed', updated: 4, unchanged: 0, warnings: 0 },
    },
    domains: generationFixture.domains.concat([{
      id: 'token-drift', group: 'drift', inputFingerprint: generationHash('f'),
      syncedAt: '2026-01-01T00:00:00.000Z', sourceGenerationId: generationId,
    }]),
    artifacts: [
      ...tokenArtifacts,
      tokenDriftArtifact('project-token-analysis-index', 'analysis-index.json', '1'),
      tokenDriftArtifact('token-binding-snapshot', 'binding-snapshot.json', '4'),
      tokenDriftArtifact('token-mapping-snapshot', 'mapping-snapshot.json', '2'),
      tokenDriftArtifact('token-comparison', 'comparison.json', '3'),
    ],
    counters: { updated: 6, unchanged: 0, warnings: 0 },
  }
  check('generation manifest accepts the token-drift domain (analysis-index + mapping-snapshot + comparison)', () => assert.equal(
    generationManifest(tokenDriftFixture), true, JSON.stringify(generationManifest.errors || [])))
  check('generation manifest rejects a token-drift artifact outside the token-comparison report dir', () => assert.equal(generationManifest({
    ...tokenDriftFixture,
    artifacts: tokenDriftFixture.artifacts.map((artifact) => artifact.role === 'token-comparison'
      ? { ...artifact, logicalPath: 'orchestrator/.cache/figma/reports/drift.json' }
      : artifact),
  }), false))

  const specCompare = await compileSchema(join(SCHEMAS, 'spec-compare-report.schema.json'), { gate: true })
  check('spec-compare report schema validates the current contract fields', () => assert.equal(specCompare({
    schemaVersion: 1,
    gatePolicyVersion: GATE_POLICY_VERSION,
    taskStem: 'TASK_1_fixture',
    pipelineRunId: 'schema-test-run',
    mode: 'gate',
    inputs: {},
    inputHashes: {},
    overall: 'PASS',
    blockingCount: 0,
    warningCount: 0,
    issues: [],
    reportPath: 'reports/spec-compare-TASK_1_fixture.json',
    engineVersion: 'spec-compare-v1',
    unresolvedRefs: [],
    implementationModel: null,
    comparisons: [],
    widgetClasses: {},
    implementation: { files: [], screenMap: {}, tokenCount: 0, tokenValueCount: 0, rawColorCount: 0, rawDpCount: 0 },
  }), true, JSON.stringify(specCompare.errors || [])))

  const screenshot = await compileSchema(join(SCHEMAS, 'screenshot-compare-report.schema.json'), { gate: true })
  const H = (c) => `sha256:${c.repeat(64)}`
  const artifact = (kind, hashChar) => ({
    id: `001-home-primary-${kind}`,
    kind,
    path: `artifacts/screenshot/TASK_1_fixture/schema-test-run/001-home-primary/${kind}.png`,
    hash: H(hashChar),
    bytes: 10,
    width: 120,
    height: 240,
    mime: 'image/png',
  })
  const manifest = { id: '001-home-primary-manifest', kind: 'manifest', path: 'artifacts/screenshot/TASK_1_fixture/schema-test-run/001-home-primary/manifest.json', hash: H('e'), bytes: 10, mime: 'application/json' }
  const artifacts = {
    figma: artifact('figma', 'a'),
    actual: artifact('actual', 'b'),
    diff: artifact('diff', 'c'),
    overlay: artifact('overlay', 'd'),
  }
  const artifactSet = {
    schemaVersion: 1,
    id: '001-home-primary',
    manifest,
    artifacts,
  }
  check('screenshot report schema validates semantic/themeKey fields', () => assert.equal(screenshot({
    schemaVersion: 1,
    gatePolicyVersion: GATE_POLICY_VERSION,
    taskStem: 'TASK_1_fixture',
    pipelineRunId: 'schema-test-run',
    mode: 'advisory',
    inputs: {},
    inputHashes: {},
    overall: 'PASS',
    blockingCount: 0,
    warningCount: 0,
    issues: [],
    reportPath: 'reports/screenshot-TASK_1_fixture.json',
    metric: 'masked-ssim-luma-v2',
    artifactSet: {
      schemaVersion: 1,
      kind: 'screenshot-compare-artifact-set',
      root: 'artifacts/screenshot/TASK_1_fixture/schema-test-run',
      entries: Object.values(artifacts),
    },
    artifactSets: [artifactSet],
    semantic: { enabled: true, status: 'ADVISORY_UNCALIBRATED', metric: 'semantic-masked-ssim-v1', promoted: false, zones: [], findings: [] },
    results: [{
      screen: 'Home',
      theme: 'primary',
      themeKey: 'primary',
      status: 'PASS',
      score: 1,
      artifactSet,
    }],
  }), true, JSON.stringify(screenshot.errors || [])))

  const seedRoot = mkdtempSync(join(tmpdir(), 'figma-evidence-seed-schema-'))
  try {
    const stem = 'TASK_999999_schema_seed'
    execFileSync('node', [SEED, stem, 'schema-seed-run'], {
      env: { ...process.env, FIGMA_CACHE_ROOT: seedRoot },
      stdio: 'pipe',
    })
    const reportSchemas = {
      'screen-cache': 'screen-cache-report.schema.json',
      'check-spec': 'check-spec-report.schema.json',
      'capture-config': 'capture-config-report.schema.json',
      census: 'census.schema.json',
      spec: 'spec-report.schema.json',
      'spec-compare': 'spec-compare-report.schema.json',
      screenshot: 'screenshot-compare-report.schema.json',
    }
    for (const [name, schemaFile] of Object.entries(reportSchemas)) {
      const validate = await compileSchema(join(SCHEMAS, schemaFile), { gate: true })
      const report = JSON.parse(readFileSync(join(seedRoot, 'reports', `${name}-${stem}.json`), 'utf8'))
      check(`seed-evidence ${name} fixture matches its report schema`, () => assert.equal(validate(report), true, JSON.stringify(validate.errors || [])))
    }
    const seededEvidence = JSON.parse(readFileSync(join(seedRoot, 'reports', `evidence-${stem}.json`), 'utf8'))
    check('seed-evidence final bundle uses the canonical seven-report set', () => assert.deepEqual(seededEvidence.requiredReports, [
      'screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot',
    ]))
  } finally {
    rmSync(seedRoot, { recursive: true, force: true })
  }
} catch (e) {
  fail++; console.log(`${C.red}FAIL${C.reset} schema compile setup threw\n     ${e.message}`)
}

console.log(`\nschema-contract.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
