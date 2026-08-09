import assert from 'node:assert/strict'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const generation = require('../server/figma-generation.js')
const hash = (char) => 'sha256:' + char.repeat(64)
const generationId = 'gen-' + '1'.repeat(32)
const syncedAt = '2026-07-23T10:00:00.000Z'

function artifact(role, logicalPath, schemaVersion = 1) {
  return {
    role,
    group: 'tokens',
    domain: 'tokens',
    path: `orchestrator/figma/manifests/artifacts/${generationId}/tokens/${logicalPath.split('/').at(-1)}`,
    logicalPath,
    hash: hash('a'),
    schemaVersion,
    persistence: 'committed',
    required: true,
    size: 10
  }
}
function manifest(artifacts) {
  return {
    schemaVersion: 2,
    generationId,
    accountFingerprint: hash('b'),
    fileKeyFingerprint: hash('c'),
    createdAt: syncedAt,
    syncJobId: 'fsj-' + '2'.repeat(32),
    updatedDomains: ['tokens'],
    syncGroups: {
      tokens: { status: 'completed', updated: artifacts.length, unchanged: 0, warnings: 0 }
    },
    groups: ['tokens'],
    domains: [{
      id: 'tokens',
      group: 'tokens',
      inputFingerprint: hash('d'),
      syncedAt,
      sourceGenerationId: generationId
    }],
    artifacts,
    counters: { updated: artifacts.length, unchanged: 0, warnings: 0 }
  }
}

const emptyDomain = [
  artifact('observed-token-source-index', 'orchestrator/figma/tokens/source-index.json'),
  artifact('observed-token-catalog', 'orchestrator/figma/tokens/observed-token-catalog.json')
]

assert.equal(generation.validateManifest(manifest(emptyDomain), generationId), true)
console.log('PASS strict-empty observed token domain is a valid generation')

const withShard = emptyDomain.concat([
  artifact('observed-token-source-shard:042', 'orchestrator/figma/tokens/sources/042.json')
])
assert.equal(generation.validateManifest(manifest(withShard), generationId), true)
console.log('PASS deterministic source shard role/path is admitted')

const wrongShard = emptyDomain.concat([
  artifact('observed-token-source-shard:042', 'orchestrator/figma/tokens/sources/041.json')
])
assert.equal(generation.validateManifest(manifest(wrongShard), generationId), false)
console.log('PASS source shard role/path mismatch is rejected')

assert.equal(generation.validateManifest(manifest([
  artifact('design-token-inventory', 'orchestrator/figma/tokens/design-token-inventory.json')
]), generationId), false)
console.log('PASS incompatible design-token-inventory role is rejected')

assert.equal(generation.validateManifest(manifest([
  artifact('observed-token-source-index', 'orchestrator/figma/tokens/source-index.json')
]), generationId), false)
console.log('PASS token domain cannot publish without the observed catalog')

assert.equal(generation.ARTIFACTS_MAX, 200)
assert.equal(generation.artifactContractVersion('token-comparison'), 2)
assert.equal(generation.artifactContractVersion('token-mapping-snapshot'), 2)
assert.equal(generation.artifactContractVersion('observed-token-catalog'), 1)
assert.equal(generation.artifactContractVersion('design-component-inventory'), 2)
assert.equal(generation.artifactContractVersion('component-comparison'), 2)
assert.equal(generation.artifactContractVersion('component-visual-evidence:' + 'a'.repeat(32)), 1)
console.log('PASS shared artifact ceiling and cutover schema versions are exact')

console.log('observed token generation: 6 checks passed')
