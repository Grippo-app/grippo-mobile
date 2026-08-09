import { canonicalHash } from '../runtime/canonical-json.mjs'

export function publishableBaseline(candidate, report, eligibleAt) {
  if (!candidate || candidate.eligible !== true || !report || report.complete !== true) return null
  const baseline = {
    schemaVersion: 2,
    scope: candidate.scope,
    source: {
      comparisonSemanticHash: report.semanticHash,
      observedCatalogHash: report.inputs.observedCatalogHash,
      analysisIndexHash: report.inputs.analysisIndexHash,
      bindingSnapshotHash: report.inputs.bindingSnapshotHash,
      mappingRevision: report.inputs.mappingRevision,
      eligibleAt
    },
    entries: candidate.entries,
    semanticHash: 'sha256:' + '0'.repeat(64)
  }
  const { semanticHash, ...payload } = baseline
  baseline.semanticHash = canonicalHash(payload)
  return baseline
}
