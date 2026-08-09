// component-runner.test.mjs — end-to-end pins for the trusted runner child
// (runtime/run-plan.mjs) component ops, executed exactly the way the server
// spawns it: fixed entrypoint, --plan file inside the stage dir, one JSON
// result line, exit 0 even for typed domain failures. Covers
// normalize-component-capture (incl. visual byte verification) and
// component-compare over a real temp Kotlin design-system fixture, plus the
// scope-fingerprint doctrine: a SOURCE edit is drift on the same mapping,
// only a CONFIG change moves the mapping to target-out-of-scope (CMP-GEN-*).
import assert from 'node:assert/strict'
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync, existsSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { normalizeCapture } from '../components/capture-normalizer.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const RUN_PLAN = join(HERE, '..', 'runtime', 'run-plan.mjs')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const sha = (bytes) => 'sha256:' + createHash('sha256').update(bytes).digest('hex')
const PNG = Buffer.from('89504e470d0a1a0a0000000d494844520000000100000001080600000000000000', 'hex')

function runPlan(stageDir, plan) {
  const planFile = join(stageDir, '.run-plan.json')
  writeFileSync(planFile, JSON.stringify({ ...plan, stageDir }, null, 2))
  const child = spawnSync(process.execPath, [RUN_PLAN, '--plan', planFile], { encoding: 'utf8', env: {} })
  assert.equal(child.status, 0, `runner must exit 0; stderr: ${String(child.stderr).slice(0, 300)}`)
  const lines = child.stdout.split('\n').filter((line) => line.trim())
  return JSON.parse(lines[lines.length - 1])
}

// A minimal single-component design scope: one standalone Chip with a text
// property, bound to one @Composable in the temp Kotlin fixture below.
function chipCapture(pngHash) {
  return {
    schemaVersion: 2,
    provider: 'figma',
    providerIdentity: { fileKeyFingerprint: 'sha256:' + 'a'.repeat(64), branchKey: 'none', libraryOriginPolicy: 'local-authoritative' },
    scope: { kind: 'pages', pageIds: ['0:1'] },
    pages: [{ pageId: '0:1', name: 'Components' }],
    entities: [{
      nodeId: '70:1', pageId: '0:1', kind: 'component', name: 'Chip', idQuality: 'stable',
      properties: [{ propertyId: 'p-label', name: 'label', type: 'text', idQuality: 'stable' }],
      variants: [], expectedVariantCount: 0, nestedRefs: [], boundVariables: []
    }],
    visual: [{ entityNodeId: '70:1', variantNodeId: null, role: 'representative', file: 'visual/chip.png', sha256: pngHash }],
    witness: {
      startedAt: '2026-07-20T10:00:00.000Z', finishedAt: '2026-07-20T10:00:02.000Z',
      providerRevisionBefore: 'rev-1', providerRevisionAfter: 'rev-1',
      consistency: 'proven', completeness: 'complete',
      requestedPageIds: ['0:1'], readPageIds: ['0:1'],
      expectedEntityCount: 1, readEntityCount: 1,
      truncated: false, permissionDegraded: false, limitsHit: []
    }
  }
}

const CHIP_KT = `package com.example.chip

import androidx.compose.runtime.Composable

@Composable
fun Chip(label: String, modifier: Modifier = Modifier) {
}
`

function chipProject(root, { withoutLabel = false, extraExclude = false } = {}) {
  mkdirSync(join(root, 'design-system', 'src'), { recursive: true })
  writeFileSync(join(root, 'design-system', 'src', 'Chip.kt'), withoutLabel ? CHIP_KT.replace('label: String, ', '') : CHIP_KT)
  mkdirSync(join(root, 'orchestrator', 'figma'), { recursive: true })
  writeFileSync(join(root, 'orchestrator', 'figma', 'project-adapters.json'), JSON.stringify({
    schemaVersion: 2,
    adapters: [{
      id: 'chip-ds', kind: 'kotlin-compose', version: 2, enabled: true,
      capabilities: ['components'], platform: 'android-compose', authority: 'handwritten',
      components: {
        roots: ['design-system/src'],
        include: ['**/*.kt'],
        exclude: extraExclude ? ['**/build/**', '**/legacy/**'] : ['**/build/**'],
        visibility: ['public']
      }
    }]
  }, null, 2))
}

const design = normalizeCapture(chipCapture(sha(PNG)), sha(Buffer.from('capture-bytes')))
const CHIP_ID = design.components[0].designComponentId
const COMPARE_ARTIFACTS = [
  'analysis-index.json', 'baseline.json', 'comparison.json', 'mapping-snapshot.json',
  'project-inventory-chip-ds.json', 'suggestions.json', 'task-suggestions.json'
]

function comparePlan(root, eligibleAt) {
  return {
    op: 'component-compare',
    projectRoot: root,
    designInventoryFile: join('inputs', 'design-component-inventory.json'),
    designGenerationId: 'gen-' + 'f'.repeat(32),
    eligibleAt
  }
}
function stageWithDesign() {
  const stage = mkdtempSync(join(tmpdir(), 'component-runner-stage-'))
  mkdirSync(join(stage, 'inputs'), { recursive: true })
  writeFileSync(join(stage, 'inputs', 'design-component-inventory.json'), JSON.stringify(design, null, 2))
  return stage
}
function registryFor(root, scopeFingerprint) {
  writeFileSync(join(root, 'orchestrator', 'figma', 'component-mappings.json'), JSON.stringify({
    schemaVersion: 2, revision: 1, designScopeId: design.scopeId,
    mappings: [{
      mappingId: 'cmap-' + 'a'.repeat(24),
      designComponentId: CHIP_ID,
      expectedKind: 'component',
      implementations: [{
        adapterId: 'chip-ds', platform: 'android-compose', projectScopeFingerprint: scopeFingerprint,
        relation: 'direct', projectComponentIds: ['chip-ds:symbol:com.example.chip.Chip'], required: true
      }],
      propertyMappings: [],
      slotMappings: [{ designSlotId: 'prop:p-label', adapterId: 'chip-ds', projectSlotId: 'param:label', verification: 'static' }],
      state: 'active',
      provenance: { kind: 'user-confirmed', actor: 'owner', at: '2026-07-20T11:00:00.000Z' }
    }],
    dispositions: []
  }, null, 2))
}

check('CMP-GEN-RUNNER: normalize-component-capture publishes the inventory and verifies visual bytes', () => {
  const stage = mkdtempSync(join(tmpdir(), 'component-runner-'))
  try {
    mkdirSync(join(stage, 'visual'), { recursive: true })
    writeFileSync(join(stage, 'visual', 'chip.png'), PNG)
    writeFileSync(join(stage, 'capture.json'), JSON.stringify(chipCapture(sha(PNG))))
    const result = runPlan(stage, { op: 'normalize-component-capture', captureFile: 'capture.json', outFile: 'design-component-inventory.json' })
    assert.equal(result.ok, true, JSON.stringify(result).slice(0, 300))
    assert.equal(result.counts.components, 1)
    assert.equal(result.absenceProofEligible, true)
    assert.deepEqual(result.visualArtifacts, [{ file: 'visual/chip.png', sha256: sha(PNG) }])
    const inventory = JSON.parse(readFileSync(join(stage, 'design-component-inventory.json'), 'utf8'))
    assert.equal(inventory.scopeId, result.scopeId)
    assert.equal(inventory.components[0].designComponentId, CHIP_ID)
  } finally { rmSync(stage, { recursive: true, force: true }) }
})

check('CMP-GEN-RUNNER: a corrupted visual byte is COMPONENT_DESIGN_CAPTURE_INVALID and writes nothing', () => {
  const stage = mkdtempSync(join(tmpdir(), 'component-runner-'))
  try {
    mkdirSync(join(stage, 'visual'), { recursive: true })
    writeFileSync(join(stage, 'visual', 'chip.png'), Buffer.concat([PNG, Buffer.from([0x00])]))
    writeFileSync(join(stage, 'capture.json'), JSON.stringify(chipCapture(sha(PNG))))
    const result = runPlan(stage, { op: 'normalize-component-capture', captureFile: 'capture.json', outFile: 'design-component-inventory.json' })
    assert.equal(result.ok, false)
    assert.equal(result.code, 'COMPONENT_DESIGN_CAPTURE_INVALID')
    assert.match(result.detail, /do not match the declared hash/)
    assert.equal(existsSync(join(stage, 'design-component-inventory.json')), false)
  } finally { rmSync(stage, { recursive: true, force: true }) }
})

check('CMP-GEN-RUNNER: a truncated capture is refused with the typed incomplete code', () => {
  const stage = mkdtempSync(join(tmpdir(), 'component-runner-'))
  try {
    const capture = chipCapture(sha(PNG))
    capture.witness.truncated = true
    mkdirSync(join(stage, 'visual'), { recursive: true })
    writeFileSync(join(stage, 'visual', 'chip.png'), PNG)
    writeFileSync(join(stage, 'capture.json'), JSON.stringify(capture))
    const result = runPlan(stage, { op: 'normalize-component-capture', captureFile: 'capture.json', outFile: 'design-component-inventory.json' })
    assert.equal(result.ok, false)
    assert.equal(result.code, 'COMPONENT_DESIGN_CAPTURE_INCOMPLETE')
  } finally { rmSync(stage, { recursive: true, force: true }) }
})

check('CMP-GEN-RUNNER: component-compare walks empty-registry -> matched -> source-drift -> config-out-of-scope', () => {
  const root = mkdtempSync(join(tmpdir(), 'component-runner-project-'))
  const stage = mkdtempSync(join(tmpdir(), 'component-runner-stage-'))
  try {
    chipProject(root)
    const comparePass = (name, eligibleAt) => {
      const passStage = join(stage, name)
      mkdirSync(join(passStage, 'inputs'), { recursive: true })
      writeFileSync(join(passStage, 'inputs', 'design-component-inventory.json'), JSON.stringify(design, null, 2))
      return { result: runPlan(passStage, comparePlan(root, eligibleAt)), passStage }
    }

    // 1. No registry: the exact revision-0 empty registry, full artifact set.
    const firstPass = comparePass('pass-1', '2026-07-20T12:00:00.000Z')
    const first = firstPass.result
    assert.equal(first.ok, true, JSON.stringify(first).slice(0, 400))
    assert.deepEqual(first.artifacts.slice().sort(), COMPARE_ARTIFACTS)
    assert.equal(first.registryPresent, false)
    assert.equal(first.mappingRevision, 0)
    assert.equal(first.coverage.unmapped, 1)
    const snapshot = JSON.parse(readFileSync(join(firstPass.passStage, 'mapping-snapshot.json'), 'utf8'))
    assert.equal(snapshot.revision, 0)
    assert.deepEqual(snapshot.mappings, [])
    const index = JSON.parse(readFileSync(join(firstPass.passStage, 'analysis-index.json'), 'utf8'))
    assert.equal(index.complete, true)
    const scopeFingerprint = index.adapters[0].scopeFingerprint

    // 2. Hand-authored registry pinned to the scanned scope: matched.
    registryFor(root, scopeFingerprint)
    const secondPass = comparePass('pass-2', '2026-07-20T12:30:00.000Z')
    const second = secondPass.result
    assert.equal(second.ok, true, JSON.stringify(second).slice(0, 400))
    assert.equal(second.mappingRevision, 1)
    assert.equal(second.registryPresent, true)
    const matchedRow = JSON.parse(readFileSync(join(secondPass.passStage, 'comparison.json'), 'utf8')).rows.find((row) => row.designComponentId === CHIP_ID)
    assert.equal(matchedRow.status, 'matched')
    assert.equal(matchedRow.mappingState, 'active')
    const baseline = JSON.parse(readFileSync(join(secondPass.passStage, 'baseline.json'), 'utf8'))
    assert.equal(baseline.entries.length, 1)
    assert.equal(baseline.source.eligibleAt, '2026-07-20T12:30:00.000Z')

    // 3. §11.10 doctrine: a SOURCE edit inside the scope is drift on the SAME
    //    applied mapping — the scope fingerprint must not move.
    chipProject(root, { withoutLabel: true })
    const thirdPass = comparePass('pass-3', '2026-07-20T13:00:00.000Z')
    const third = thirdPass.result
    assert.equal(third.ok, true, JSON.stringify(third).slice(0, 400))
    const driftedRow = JSON.parse(readFileSync(join(thirdPass.passStage, 'comparison.json'), 'utf8')).rows.find((row) => row.designComponentId === CHIP_ID)
    assert.equal(driftedRow.status, 'drifted')
    assert.equal(driftedRow.mappingState, 'active', 'a content edit must never silently retire the mapping')
    assert.ok(driftedRow.findings.some((finding) => finding.family === 'slot-removed' && finding.severity === 'breaking'))
    const thirdIndex = JSON.parse(readFileSync(join(thirdPass.passStage, 'analysis-index.json'), 'utf8'))
    assert.equal(thirdIndex.adapters[0].scopeFingerprint, scopeFingerprint, 'source content is not part of the scope identity')

    // 4. A CONFIG change moves the scope fingerprint -> target-out-of-scope.
    chipProject(root, { extraExclude: true })
    const fourthPass = comparePass('pass-4', '2026-07-20T13:30:00.000Z')
    const fourth = fourthPass.result
    assert.equal(fourth.ok, true, JSON.stringify(fourth).slice(0, 400))
    const fourthIndex = JSON.parse(readFileSync(join(fourthPass.passStage, 'analysis-index.json'), 'utf8'))
    assert.notEqual(fourthIndex.adapters[0].scopeFingerprint, scopeFingerprint)
    const outOfScopeRow = JSON.parse(readFileSync(join(fourthPass.passStage, 'comparison.json'), 'utf8')).rows.find((row) => row.designComponentId === CHIP_ID)
    assert.equal(outOfScopeRow.status, 'unmapped')
    assert.equal(outOfScopeRow.mappingState, 'target-out-of-scope')
  } finally {
    rmSync(root, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('CMP-GEN-RUNNER: a malformed mapping registry blocks the comparison with a typed code', () => {
  const root = mkdtempSync(join(tmpdir(), 'component-runner-project-'))
  const stage = stageWithDesign()
  try {
    chipProject(root)
    writeFileSync(join(root, 'orchestrator', 'figma', 'component-mappings.json'), '{ broken json')
    const result = runPlan(stage, comparePlan(root, '2026-07-20T12:00:00.000Z'))
    assert.equal(result.ok, false)
    assert.equal(result.code, 'COMPONENT_MAPPING_INVALID')
  } finally {
    rmSync(root, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('CMP-GEN-RUNNER: a design inventory violating its contract is COMPONENT_GENERATION_RESYNC_REQUIRED', () => {
  const root = mkdtempSync(join(tmpdir(), 'component-runner-project-'))
  const stage = mkdtempSync(join(tmpdir(), 'component-runner-stage-'))
  try {
    chipProject(root)
    mkdirSync(join(stage, 'inputs'), { recursive: true })
    writeFileSync(join(stage, 'inputs', 'design-component-inventory.json'), JSON.stringify({ schemaVersion: 2 }))
    const result = runPlan(stage, comparePlan(root, '2026-07-20T12:00:00.000Z'))
    assert.equal(result.ok, false)
    assert.equal(result.code, 'COMPONENT_GENERATION_RESYNC_REQUIRED')
  } finally {
    rmSync(root, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('CMP-GEN-RUNNER: malformed or unpaired token causality inputs fail closed', () => {
  const root = mkdtempSync(join(tmpdir(), 'component-runner-project-'))
  const stage = stageWithDesign()
  try {
    chipProject(root)
    writeFileSync(join(stage, 'inputs', 'token-comparison.json'), JSON.stringify({ schemaVersion: 2 }))
    writeFileSync(join(stage, 'inputs', 'token-bindings.json'), JSON.stringify({ schemaVersion: 1 }))
    const malformedPlan = comparePlan(root, '2026-07-20T12:00:00.000Z')
    malformedPlan.tokenComparisonFile = join('inputs', 'token-comparison.json')
    malformedPlan.tokenBindingSnapshotFile = join('inputs', 'token-bindings.json')
    const malformed = runPlan(stage, malformedPlan)
    assert.equal(malformed.ok, false)
    assert.equal(malformed.code, 'COMPONENT_GENERATION_RESYNC_REQUIRED')

    const unpairedPlan = comparePlan(root, '2026-07-20T12:00:00.000Z')
    unpairedPlan.tokenComparisonFile = join('inputs', 'token-comparison.json')
    const unpaired = runPlan(stage, unpairedPlan)
    assert.equal(unpaired.ok, false)
    assert.equal(unpaired.code, 'COMPONENT_GENERATION_RESYNC_REQUIRED')
  } finally {
    rmSync(root, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('CMP-GEN-RUNNER: an unconfigured project is a typed skip, never a heuristic scan', () => {
  const root = mkdtempSync(join(tmpdir(), 'component-runner-project-'))
  const stage = stageWithDesign()
  try {
    const result = runPlan(stage, comparePlan(root, '2026-07-20T12:00:00.000Z'))
    assert.equal(result.ok, false)
    assert.equal(result.code, 'PROJECT_ADAPTERS_UNCONFIGURED')
  } finally {
    rmSync(root, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

check('CMP-GEN-RUNNER: a design inventory file escaping the stage directory is refused', () => {
  const root = mkdtempSync(join(tmpdir(), 'component-runner-project-'))
  const stage = mkdtempSync(join(tmpdir(), 'component-runner-stage-'))
  try {
    chipProject(root)
    const plan = comparePlan(root, '2026-07-20T12:00:00.000Z')
    plan.designInventoryFile = '../outside.json'
    const result = runPlan(stage, plan)
    assert.equal(result.ok, false)
  } finally {
    rmSync(root, { recursive: true, force: true })
    rmSync(stage, { recursive: true, force: true })
  }
})

console.log(`\ncomponent-runner.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
