import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const generation = require('../../../site/server/figma-generation.js')

// Exact pointer -> manifest -> artifact walk shared by all script-side
// readers. Missing roles are explicit empty state; malformed generations are
// typed by the caller and never fall back to loose files.
export function readGenerationArtifact(role, fail) {
  const active = generation.current()
  if (!active.ok) throw fail(`active generation is invalid (${active.error}${active.role ? `: ${active.role}` : ''})`)
  if (active.mode === 'none') return { present: false }
  const row = active.manifest.artifacts.find((entry) => entry.role === role)
  if (!row) return { present: false, generationId: active.manifest.generationId, manifest: active.manifest }
  const bytes = generation.readEntry(row)
  if (!bytes) throw fail(`${role} artifact is unavailable after active-generation validation`)
  return {
    present: true,
    bytes,
    generationId: active.manifest.generationId,
    artifactHash: row.hash,
    manifest: active.manifest
  }
}
