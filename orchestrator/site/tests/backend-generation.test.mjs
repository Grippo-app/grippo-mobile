import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, unlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const REPO = fileURLToPath(new URL('../../../', import.meta.url))
const root = mkdtempSync(join(tmpdir(), 'backend-generation-'))
const contractDir = join(root, 'orchestrator', 'api-contract')
const candidate = join(root, 'candidate')
mkdirSync(join(contractDir, 'manifests'), { recursive: true })
mkdirSync(join(candidate, 'areas'), { recursive: true })
writeFileSync(join(contractDir, 'environments.json'), JSON.stringify({ schemaVersion: 1,
  environments: [{ id: 'local', label: 'Local', sourceKind: 'openapi', sourceUrl: 'http://127.0.0.1:8080/openapi.json', postmanEnrichmentUrl: null, authRef: null }],
  defaultEnvironmentId: 'local' }, null, 2) + '\n')
process.env.ORCHESTRATOR_PROJECT_ROOT = root

const inventory = { schemaVersion: 1, source: { kind: 'openapi', title: 'Fixture', fetchedAt: new Date().toISOString() },
  areas: { notes: { endpointCount: 1 } }, endpoints: [{ method: 'GET', path: '/notes' }], stats: { endpoints: 1, areas: 1 } }
writeFileSync(join(candidate, 'inventory.json'), JSON.stringify(inventory, null, 2) + '\n')
writeFileSync(join(candidate, 'areas', 'notes.json'), JSON.stringify({ schemaVersion: 1, area: 'notes', schemas: {} }, null, 2) + '\n')
writeFileSync(join(candidate, 'openapi.json'), JSON.stringify({ openapi: '3.1.0', info: { title: 'Fixture', version: '1' }, paths: { '/notes': {} } }, null, 2) + '\n')

const require = createRequire(import.meta.url)
const generation = require(join(REPO, 'orchestrator', 'site', 'server', 'contract-generation.js'))
const backendIntegration = require(join(REPO, 'orchestrator', 'site', 'server', 'backend-integration.js'))
const apiReportState = require(join(REPO, 'orchestrator', 'site', 'server', 'api-report-state.js'))
const apiContract = require(join(REPO, 'orchestrator', 'site', 'server', 'api-contract.js'))

after(() => rmSync(root, { recursive: true, force: true }))

function publish(expectedSnapshotHash, overrides = {}) {
  return generation.publish({ generationId: generation.createGenerationId(), environmentId: 'dev', sourceKind: 'openapi',
    sourceFingerprint: 'sha256:' + '1'.repeat(64), expectedSnapshotHash,
    inventoryFile: join(candidate, 'inventory.json'), areasDir: join(candidate, 'areas'),
    specFile: join(candidate, 'openapi.json'), createdAt: new Date().toISOString(), ...overrides })
}

test('root snapshot files are never a source before the first generation', () => {
  const retiredInventory = join(contractDir, 'manifests', 'endpoint-inventory.json')
  writeFileSync(retiredInventory, JSON.stringify(inventory, null, 2) + '\n')
  const current = generation.current()
  assert.equal(current.ok, true)
  assert.equal(current.mode, 'none')
  const integration = backendIntegration.get()
  assert.equal(integration.snapshot.present, false)
  unlinkSync(retiredInventory)
  const empty = generation.current()
  assert.equal(empty.ok, true)
  assert.equal(empty.mode, 'none')
})

test('empty interrupted generation skeleton is recovered and never treated as contract evidence', () => {
  const generationId = 'gen-20260101T010101Z-abcdef123456'
  const directory = join(contractDir, 'manifests', 'generation-artifacts', generationId)
  mkdirSync(join(directory, 'areas'), { recursive: true })
  const current = generation.current()
  assert.equal(current.ok, true)
  assert.equal(current.mode, 'none')
  assert.equal(existsSync(directory), false)
})

test('failed publication removes its owned generation instead of leaving pointer-blocking residue', () => {
  const generationId = generation.createGenerationId()
  const invalidInventory = join(candidate, 'inventory-invalid-first.json')
  writeFileSync(invalidInventory, JSON.stringify({ ...inventory, stats: { endpoints: 1, areas: 2 } }) + '\n')
  const result = generation.publish({ generationId, environmentId: 'dev', sourceKind: 'openapi',
    sourceFingerprint: 'sha256:' + '1'.repeat(64), expectedSnapshotHash: null,
    inventoryFile: invalidInventory, areasDir: join(candidate, 'areas'), specFile: join(candidate, 'openapi.json') })
  assert.equal(result.ok, false)
  assert.equal(result.error, 'generation-area-set-invalid')
  assert.equal(result.abandonedGenerationId, undefined)
  assert.equal(existsSync(join(contractDir, 'manifests', 'generation-artifacts', generationId)), false)
  assert.equal(existsSync(join(contractDir, 'manifests', 'generations', generationId + '.json')), false)
  assert.equal(generation.current().mode, 'none')
})

test('generation pointer is last, fail-closed and keeps the prior generation on conflict', () => {
  const first = publish(null)
  assert.equal(first.ok, true)
  const current = generation.current()
  assert.equal(current.ok, true)
  assert.equal(current.mode, 'generation')
  assert.equal(current.manifest.generationId, first.generationId)
  assert.equal(current.snapshotHash, first.currentHash)
  assert.equal(backendIntegration.get().snapshot.environmentMismatch, true)
  const pointerBefore = readFileSync(generation.POINTER_FILE)

  const conflict = publish(null)
  assert.equal(conflict.ok, false)
  assert.equal(conflict.error, 'write-conflict')
  assert.deepEqual(readFileSync(generation.POINTER_FILE), pointerBefore)

  const badAreasInventory = { ...inventory, stats: { endpoints: 1, areas: 2 } }
  writeFileSync(join(candidate, 'inventory-bad.json'), JSON.stringify(badAreasInventory) + '\n')
  const bad = publish(first.currentHash, { inventoryFile: join(candidate, 'inventory-bad.json') })
  assert.equal(bad.ok, false)
  assert.equal(generation.current().manifest.generationId, first.generationId)
  assert.deepEqual(readFileSync(generation.POINTER_FILE), pointerBefore)

  const wrongSetInventory = { ...inventory, areas: { other: [] } }
  writeFileSync(join(candidate, 'inventory-wrong-set.json'), JSON.stringify(wrongSetInventory) + '\n')
  const wrongSet = publish(first.currentHash, { inventoryFile: join(candidate, 'inventory-wrong-set.json') })
  assert.equal(wrongSet.ok, false)
  assert.equal(wrongSet.error, 'generation-area-set-invalid')
  assert.deepEqual(readFileSync(generation.POINTER_FILE), pointerBefore)
})

test('manifest role policy is fail-closed even when an attacker recomputes the pointer hash', () => {
  const current = generation.current()
  const manifestFile = join(contractDir, 'manifests', 'generations', current.manifest.generationId + '.json')
  const manifestBytes = readFileSync(manifestFile)
  const pointerBytes = readFileSync(generation.POINTER_FILE)
  const changed = JSON.parse(manifestBytes)
  changed.artifacts.find((row) => row.role === 'inventory').required = false
  const changedBytes = Buffer.from(JSON.stringify(changed, null, 2) + '\n')
  writeFileSync(manifestFile, changedBytes)
  const pointer = JSON.parse(pointerBytes)
  pointer.manifestHash = generation.sha(changedBytes)
  writeFileSync(generation.POINTER_FILE, JSON.stringify(pointer, null, 2) + '\n')
  const invalid = generation.current()
  assert.equal(invalid.ok, false)
  assert.equal(invalid.error, 'generation-contract-invalid')
  writeFileSync(manifestFile, manifestBytes)
  writeFileSync(generation.POINTER_FILE, pointerBytes)
  assert.equal(generation.current().ok, true)
})

test('artifact schema versions are exact and never treated as forward-compatible', () => {
  const current = generation.current()
  const manifestFile = join(contractDir, 'manifests', 'generations', current.manifest.generationId + '.json')
  const manifestBytes = readFileSync(manifestFile)
  const pointerBytes = readFileSync(generation.POINTER_FILE)
  const changed = JSON.parse(manifestBytes)
  changed.artifacts.find((row) => row.role === 'inventory').schemaVersion = 2
  const changedBytes = Buffer.from(JSON.stringify(changed, null, 2) + '\n')
  writeFileSync(manifestFile, changedBytes)
  const pointer = JSON.parse(pointerBytes)
  pointer.manifestHash = generation.sha(changedBytes)
  writeFileSync(generation.POINTER_FILE, JSON.stringify(pointer, null, 2) + '\n')
  assert.deepEqual(generation.current(), { ok: false, error: 'generation-contract-invalid' })
  writeFileSync(manifestFile, manifestBytes)
  writeFileSync(generation.POINTER_FILE, pointerBytes)
  assert.equal(generation.current().ok, true)
})

test('a valid generation never reads root snapshot artifacts', () => {
  const retiredInventory = join(contractDir, 'manifests', 'endpoint-inventory.json')
  writeFileSync(retiredInventory, JSON.stringify(inventory, null, 2) + '\n')
  assert.equal(generation.current().ok, true)
  unlinkSync(retiredInventory)

  mkdirSync(join(contractDir, 'spec'), { recursive: true })
  const retiredSpec = join(contractDir, 'spec', 'openapi.json')
  writeFileSync(retiredSpec, '{}\n')
  assert.equal(generation.current().ok, true)
  unlinkSync(retiredSpec)

  mkdirSync(join(contractDir, 'manifests', 'areas'), { recursive: true })
  const retiredArea = join(contractDir, 'manifests', 'areas', 'notes.json')
  writeFileSync(retiredArea, '{}\n')
  assert.equal(generation.current().ok, true)
  unlinkSync(retiredArea)
  assert.equal(generation.current().ok, true)
})

test('missing pointer with generation evidence is fail-closed and never downgrades', () => {
  const pointerBytes = readFileSync(generation.POINTER_FILE)
  unlinkSync(generation.POINTER_FILE)
  const missing = generation.current()
  assert.equal(missing.ok, false)
  assert.equal(missing.error, 'generation-pointer-missing')
  writeFileSync(generation.POINTER_FILE, pointerBytes)
  assert.equal(generation.current().ok, true)
})

test('contract:paths resolves only validated generation artifacts under the selected project root', () => {
  const resolver = join(REPO, 'orchestrator', 'api-contract', 'scripts', 'resolve-current.mjs')
  const resolved = spawnSync(process.execPath, [resolver], {
    cwd: root,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: root },
    encoding: 'utf8',
  })
  assert.equal(resolved.status, 0, resolved.stderr + resolved.stdout)
  const output = JSON.parse(resolved.stdout)
  assert.equal(output.present, true)
  assert.equal(output.mode, 'generation')
  assert.match(output.inventory, /^orchestrator\/api-contract\/manifests\/generation-artifacts\//)
  assert.match(output.areasDir, /^orchestrator\/api-contract\/manifests\/generation-artifacts\//)
  assert.equal(existsSync(join(root, output.inventory)), true)
  assert.equal(existsSync(join(root, output.areasDir, 'notes.json')), true)
  assert.equal(existsSync(join(contractDir, 'manifests', 'endpoint-inventory.json')), false)
})

test('contract:paths reports no snapshot when only retired root artifacts exist', () => {
  const retiredRoot = mkdtempSync(join(tmpdir(), 'backend-retired-resolver-'))
  try {
    const retiredContract = join(retiredRoot, 'orchestrator', 'api-contract')
    mkdirSync(join(retiredContract, 'manifests', 'areas'), { recursive: true })
    writeFileSync(join(retiredContract, 'manifests', 'endpoint-inventory.json'), JSON.stringify(inventory, null, 2) + '\n')
    writeFileSync(join(retiredContract, 'manifests', 'areas', 'notes.json'), JSON.stringify({ schemaVersion: 1, area: 'notes', schemas: {} }) + '\n')
    const resolver = join(REPO, 'orchestrator', 'api-contract', 'scripts', 'resolve-current.mjs')
    const resolved = spawnSync(process.execPath, [resolver], {
      cwd: retiredRoot,
      env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: retiredRoot },
      encoding: 'utf8',
    })
    assert.equal(resolved.status, 0, resolved.stderr + resolved.stdout)
    const output = JSON.parse(resolved.stdout)
    assert.equal(output.present, false)
    assert.equal(output.mode, 'none')
    assert.equal(output.inventory, null)

    unlinkSync(join(retiredContract, 'manifests', 'endpoint-inventory.json'))
    const incomplete = spawnSync(process.execPath, [resolver], {
      cwd: retiredRoot,
      env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: retiredRoot },
      encoding: 'utf8',
    })
    assert.equal(incomplete.status, 0)
    assert.deepEqual(JSON.parse(incomplete.stdout), {
      schemaVersion: 1, present: false, mode: 'none', snapshotHash: null,
      inventory: null, areasDir: null, spec: null
    })
  } finally {
    rmSync(retiredRoot, { recursive: true, force: true })
  }
})

test('contract:paths rejects the internal staging override', () => {
  const resolver = join(REPO, 'orchestrator', 'api-contract', 'scripts', 'resolve-current.mjs')
  const result = spawnSync(process.execPath, [resolver], {
    cwd: root,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: root, ORCHESTRATOR_API_CONTRACT_DATA_DIR: join(root, 'staging') },
    encoding: 'utf8',
  })
  assert.equal(result.status, 2)
  assert.match(result.stderr, /refuses the internal staging data override/)
})

test('contract:suggest accepts only the validated current generation and canonical report paths', () => {
  const suggest = join(REPO, 'orchestrator', 'api-contract', 'scripts', 'suggest-endpoint-tasks.mjs')
  const rejected = spawnSync(process.execPath, [suggest], {
    cwd: root,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: root, CONTRACT_SUGGEST_INVENTORY: join(candidate, 'inventory.json') },
    encoding: 'utf8',
  })
  assert.equal(rejected.status, 1)
  assert.match(rejected.stdout, /unsupported contract:suggest environment override/)

  const planned = spawnSync(process.execPath, [suggest], {
    cwd: root,
    env: { ...process.env, ORCHESTRATOR_PROJECT_ROOT: root },
    encoding: 'utf8',
  })
  assert.equal(planned.status, 0, planned.stderr + planned.stdout)
  const report = JSON.parse(readFileSync(join(root, 'orchestrator', '.cache', 'api-contract', 'reports', 'suggested-endpoints.json'), 'utf8'))
  assert.deepEqual(report.summary, { notImplemented: 1, drift: 0, implemented: 0, total: 1 })
  assert.equal(report.suggestions[0].operationId, 'GET /notes')
  assert.equal(report.suggestions[0].method, 'GET')
  assert.equal(report.suggestions[0].path, '/notes')
})

test('required artifact tampering invalidates the pointer and root files remain irrelevant', () => {
  const current = generation.current()
  const inventoryFile = current.artifacts.inventory
  const original = readFileSync(inventoryFile)
  writeFileSync(inventoryFile, Buffer.concat([original, Buffer.from(' ')]))
  const invalid = generation.current()
  assert.equal(invalid.ok, false)
  assert.equal(invalid.error, 'generation-artifact-invalid')
  writeFileSync(inventoryFile, original)

  const retiredInventory = join(contractDir, 'manifests', 'endpoint-inventory.json')
  writeFileSync(retiredInventory, JSON.stringify({ retired: true }) + '\n')
  const shadowed = generation.current()
  assert.equal(shadowed.ok, true)
  unlinkSync(retiredInventory)
  assert.equal(generation.current().ok, true)
})

test('corrupt optional comparison reports are invalid, never successful empty state', () => {
  const reports = join(root, 'orchestrator', '.cache', 'api-contract', 'reports')
  mkdirSync(reports, { recursive: true })
  const drift = join(reports, 'drift.json')
  const coverage = join(reports, 'suggested-endpoints.json')
  writeFileSync(drift, '{broken')
  writeFileSync(coverage, '[]\n')
  assert.deepEqual(apiReportState.readDrift(), {
    present: false, invalid: true, error: 'contract-drift-invalid'
  })
  assert.deepEqual(apiReportState.readCoverage(), {
    present: false, invalid: true, error: 'contract-coverage-invalid'
  })
  assert.deepEqual(apiContract.status().drift, {
    present: false, invalid: true, error: 'contract-drift-invalid'
  })
  assert.deepEqual(apiContract.status().coverage, {
    present: false, invalid: true, error: 'contract-coverage-invalid'
  })
  unlinkSync(drift)
  unlinkSync(coverage)
  assert.deepEqual(apiReportState.readDrift(), { present: false })
  assert.deepEqual(apiReportState.readCoverage(), { present: false })
  assert.deepEqual(apiContract.status().drift, { present: false })
  assert.deepEqual(apiContract.status().coverage, { present: false })
})

test('optional runtime reports may expire without invalidating committed artifacts', () => {
  const reports = join(root, 'orchestrator', '.cache', 'api-contract', 'reports')
  mkdirSync(reports, { recursive: true })
  const change = join(reports, 'change-job-' + 'b'.repeat(32) + '.json')
  const refresh = join(reports, 'refresh-job-' + 'b'.repeat(32) + '.json')
  writeFileSync(change, '{}\n')
  writeFileSync(refresh, '{}\n')
  const before = generation.current()
  const next = publish(before.snapshotHash, { changeReportFile: change, refreshReportFile: refresh })
  assert.equal(next.ok, true)
  unlinkSync(change); unlinkSync(refresh)
  const afterRetention = generation.current()
  assert.equal(afterRetention.ok, true)
  assert.equal(afterRetention.manifest.generationId, next.generationId)
})

test('explicit generation reset is CAS-bound and removes every committed generation', () => {
  const before = generation.current()
  assert.equal(before.ok, true)
  assert.equal(before.mode, 'generation')
  assert.deepEqual(generation.clearAll('sha256:' + '0'.repeat(64)), { ok: false, error: 'write-conflict' })
  assert.equal(generation.current().snapshotHash, before.snapshotHash)
  assert.deepEqual(generation.clearAll(before.snapshotHash), { ok: true })
  assert.equal(generation.current().mode, 'none')
  assert.equal(existsSync(generation.POINTER_FILE), false)
})
