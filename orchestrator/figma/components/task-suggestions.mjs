// Deterministic task proposals derived from one component comparison report.
// Plans only — creating a task stays an explicit user action through the task
// creation transaction. Dedup keys are built from stable IDs, domain,
// platform, and finding family; display text never participates
// (REQ-ID-004). A proposal suppressed by a proven underlying-token-change
// never appears.
import { createHash } from 'node:crypto'
import { COMPONENT_LIMITS } from './limits.mjs'
import { compareText } from '../runtime/canonical-json.mjs'

const sha = (value) => 'sha256:' + createHash('sha256').update(value, 'utf8').digest('hex')
const proposalIdFor = (dedupKey) => 'cmpt-' + createHash('sha256').update(dedupKey, 'utf8').digest('hex').slice(0, 24)

function dedupKeyFor(parts) {
  return sha(parts.join('\0'))
}

function proposal(intent, severity, title, detail, identity, extra) {
  const dedupKey = dedupKeyFor([identity.id, 'component', identity.platform || 'none', intent])
  return {
    proposalId: proposalIdFor(dedupKey),
    intent,
    dedupKey,
    severity,
    title: title.slice(0, 200),
    detail: detail.slice(0, 1000),
    ...(extra || {})
  }
}

// report: validated component comparison. Returns the task-suggestions
// artifact body (schemaVersion + comparisonSemanticHash + proposals).
export function suggestComponentTasks(report) {
  const proposals = []
  for (const row of report.rows) {
    const suppressed = (row.findings || []).some((finding) => finding.suppressesTask === true)
    if (row.status === 'unmapped' && !row.suggestionsAmbiguous) {
      proposals.push(proposal('implement', 'review',
        `Implement design component ${row.displayName}`,
        `Design component ${row.designComponentId} has no confirmed project implementation. Review suggestions first; implementing creates the mapping through finalization.`,
        { id: row.designComponentId },
        { designComponentId: row.designComponentId, findingId: row.findingId }))
      continue
    }
    if (row.status === 'ambiguous') {
      proposals.push(proposal('reconcile-mapping', 'review',
        `Review mapping for ${row.displayName}`,
        row.statusDetail || `Design component ${row.designComponentId} needs an explicit mapping decision.`,
        { id: row.designComponentId },
        { designComponentId: row.designComponentId, ...(row.mappingId ? { mappingId: row.mappingId } : {}), findingId: row.findingId }))
      continue
    }
    if (row.status === 'missing-in-design') {
      proposals.push(proposal('remap', 'review',
        `Resolve deleted design component for mapping ${row.mappingId}`,
        `The mapped design component ${row.designComponentId} is proven absent. Choose: keep project-only, remap to a new design id, retire the mapping, or restore the design side. Code is never deleted automatically.`,
        { id: row.designComponentId },
        { designComponentId: row.designComponentId, mappingId: row.mappingId, findingId: row.findingId }))
      continue
    }
    if (row.status !== 'drifted' && row.dimensions &&
        (row.dimensions.visual === 'review-required' || row.dimensions.visual === 'drifted') && !suppressed) {
      proposals.push(proposal('update-visual', 'review',
        `Review visual contract for ${row.displayName}`,
        `Component ${row.designComponentId} has visual status ${row.dimensions.visual}; semantic/API status remains ${row.status}.`,
        { id: row.designComponentId },
        { designComponentId: row.designComponentId, ...(row.mappingId ? { mappingId: row.mappingId } : {}), findingId: row.findingId, findingFamilies: ['visual-evidence-drift'] }))
      continue
    }
    if (row.status !== 'drifted' || suppressed) continue
    const families = [...new Set((row.findings || []).map((finding) => finding.family))].sort()
    const missingPlatforms = (row.platforms || []).filter((platform) => platform.state === 'missing' && platform.required)
    if (row.status === 'drifted' && missingPlatforms.length === (row.platforms || []).length && missingPlatforms.length) {
      proposals.push(proposal('implement', 'breaking',
        `Implement missing platforms for ${row.displayName}`,
        `Every required mapped implementation is absent from a complete scan.`,
        { id: row.designComponentId },
        { designComponentId: row.designComponentId, mappingId: row.mappingId, findingId: row.findingId, findingFamilies: families.slice(0, 16) }))
      continue
    }
    for (const platform of missingPlatforms) {
      proposals.push(proposal('add-platform', 'breaking',
        `Add ${platform.platform} implementation for ${row.displayName}`,
        `The required ${platform.platform} implementation of ${row.designComponentId} is absent from a complete scan.`,
        { id: row.designComponentId, platform: platform.platform },
        { designComponentId: row.designComponentId, mappingId: row.mappingId, adapterId: platform.adapterId, platform: platform.platform, findingId: row.findingId, findingFamilies: ['platform-implementation-missing'] }))
    }
    const visualOnly = families.every((family) => ['visual-evidence-drift', 'design-renamed', 'design-moved', 'mapping-policy-changed', 'identity-quality-degraded', 'test-coverage-missing', 'default-unknown', 'project-api-added', 'token-binding-changed', 'unsupported-property'].includes(family))
    if (!missingPlatforms.length) {
      const severityRank = ['info', 'review', 'additive', 'behavioral', 'breaking', 'blocking']
      const worst = (row.findings || []).reduce((acc, finding) =>
        severityRank.indexOf(finding.severity) > severityRank.indexOf(acc) ? finding.severity : acc, 'review')
      proposals.push(proposal(visualOnly ? 'update-visual' : 'update-api', worst === 'info' ? 'review' : worst,
        `Update ${row.displayName} to match its design contract`,
        `Component ${row.designComponentId} drifted: ${families.slice(0, 8).join(', ')}.`,
        { id: row.designComponentId },
        { designComponentId: row.designComponentId, ...(row.mappingId ? { mappingId: row.mappingId } : {}), findingId: row.findingId, findingFamilies: families.slice(0, 16) }))
    }
  }
  for (const entry of report.projectOnly) {
    if (entry.classification !== 'unclassified') continue
    proposals.push(proposal('classify-project-only', 'info',
      `Classify project-only component ${entry.displayName}`,
      `Project component ${entry.projectComponentId} has no design relation. Map it, mark it intentionally local/external, or create a design-side task.`,
      { id: entry.projectComponentId, platform: entry.platform },
      { projectComponentId: entry.projectComponentId, adapterId: entry.adapterId, platform: entry.platform, findingId: entry.findingId }))
  }

  proposals.sort((a, b) => compareText(a.dedupKey, b.dedupKey))
  const bounded = proposals.slice(0, COMPONENT_LIMITS.taskProposalsMax)
  return {
    schemaVersion: 2,
    comparisonSemanticHash: report.semanticHash,
    proposals: bounded
  }
}
