// Deterministic, explainable component mapping candidates. Suggestions are
// derived evidence in an immutable artifact — they never mutate the registry
// and no name/API/screenshot similarity ever creates a confirmed mapping.
// Ambiguity is a first-class outcome: two candidates in the strong band make
// the row ambiguous instead of picking sort order. Candidate generation is
// index-backed; the containment scan runs only when no anchored name matched
// and its pool is inside a declared bound, recording a limitation otherwise.
import { COMPONENT_LIMITS } from './limits.mjs'
import { compareText } from '../runtime/canonical-json.mjs'

const NAME_POOL_MAX = 2000
const BANDS = ['strong', 'moderate', 'weak']

const fold = (value) => String(value).toLowerCase().replace(/[^a-z0-9]+/g, '')

function pushTo(map, key, value) {
  let bucket = map.get(key)
  if (!bucket) { bucket = []; map.set(key, bucket) }
  bucket.push(value)
}

// Build once per comparison over every validated project inventory.
export function buildSuggestionIndex(projectInventories) {
  const entries = []
  const byFoldedName = new Map()
  for (const inventory of projectInventories) {
    for (const component of inventory.components) {
      const folded = fold(component.name)
      const entry = { adapterId: inventory.adapterId, platform: inventory.platform, component, folded }
      entries.push(entry)
      if (folded) pushTo(byFoldedName, folded, entry)
    }
  }
  entries.sort((a, b) => compareText(a.component.projectComponentId, b.component.projectComponentId))
  return { entries, byFoldedName }
}

// Structural compatibility of one design component against one project
// component: which design variant properties are expressible and which design
// slots have a plausible project slot. Pure evidence — never authority.
function apiCompatibility(designComponent, projectComponent) {
  const matched = []
  const unmatched = []
  const projectByFold = new Map()
  for (const property of projectComponent.variantProperties) {
    projectByFold.set(fold(property.name), property)
  }
  for (const property of designComponent.properties) {
    if (property.type !== 'variant' && property.type !== 'boolean') continue
    const hit = projectByFold.get(fold(property.name))
    if (!hit) { unmatched.push(`${property.propertyId} (${property.name}): no same-named project property`); continue }
    if (property.type === 'boolean') {
      if (hit.source === 'boolean') matched.push(`${property.propertyId} ≙ ${hit.projectPropertyId} (boolean)`)
      else unmatched.push(`${property.propertyId} (${property.name}): project property is not boolean`)
      continue
    }
    const projectValues = new Set(hit.values.map((row) => fold(row.value)))
    const missing = property.options.filter((option) => !projectValues.has(fold(option)))
    if (missing.length === 0) matched.push(`${property.propertyId} ≙ ${hit.projectPropertyId} (all ${property.options.length} values expressible)`)
    else unmatched.push(`${property.propertyId} (${property.name}): values not expressible: ${missing.slice(0, 4).join(', ')}`)
  }
  const designSlots = designComponent.semanticSlots.filter((slot) => slot.kind === 'text-property' || slot.kind === 'instance-swap')
  const projectSlotFolds = new Set(projectComponent.slots.map((slot) => fold(slot.name)))
  let slotHits = 0
  for (const slot of designSlots) {
    if (projectSlotFolds.has(fold(slot.name))) slotHits++
  }
  const considered = matched.length + unmatched.length
  return {
    matched,
    unmatched,
    score: considered === 0 ? null : matched.length / considered,
    slotScore: designSlots.length === 0 ? null : slotHits / designSlots.length
  }
}

// Candidates for one design component. index: buildSuggestionIndex output.
// options: { exclusiveClaims: Map<projectComponentId, mappingId>,
//            sharedClaims: Map<projectComponentId, mappingId[]> }
// Returns { candidates, ambiguous, limitations }.
export function suggestCandidates(designComponent, index, options) {
  const exclusiveClaims = options.exclusiveClaims || new Map()
  const sharedClaims = options.sharedClaims || new Map()
  const designFold = fold(designComponent.name)
  const limitations = []
  const seen = new Set()
  const hits = []

  const addHit = (entry, nameSignal) => {
    if (seen.has(entry.component.projectComponentId)) return
    if (exclusiveClaims.has(entry.component.projectComponentId)) return
    seen.add(entry.component.projectComponentId)
    hits.push({ entry, nameSignal })
  }

  if (designFold) {
    for (const entry of index.byFoldedName.get(designFold) || []) {
      addHit(entry, { kind: 'qualified-name', detail: `normalized names are identical (${designComponent.name} ≙ ${entry.component.name})` })
    }
    // Anchored suffix: AppButton implements Button.
    for (const entry of index.entries) {
      if (seen.has(entry.component.projectComponentId)) continue
      if (entry.folded && entry.folded !== designFold && entry.folded.endsWith(designFold)) {
        addHit(entry, { kind: 'qualified-name', detail: `project name ends with the design name (${entry.component.name})` })
      }
    }
    if (!hits.length) {
      if (index.entries.length > NAME_POOL_MAX) {
        limitations.push('name-similarity-skipped-large-inventory')
      } else {
        for (const entry of index.entries) {
          if (!entry.folded) continue
          if (entry.folded.includes(designFold) || designFold.includes(entry.folded)) {
            addHit(entry, { kind: 'display-name', detail: 'normalized names contain each other' })
          }
        }
      }
    }
  }

  const candidates = hits.map(({ entry, nameSignal }) => {
    const compatibility = apiCompatibility(designComponent, entry.component)
    const signals = [nameSignal]
    if (compatibility.score !== null) {
      signals.push({
        kind: 'api-compatibility',
        detail: `${compatibility.matched.length}/${compatibility.matched.length + compatibility.unmatched.length} design variant/boolean properties expressible`
      })
    }
    if (compatibility.slotScore !== null) {
      signals.push({ kind: 'slot-compatibility', detail: `${Math.round(compatibility.slotScore * 100)}% of design content slots have a same-named project slot` })
    }
    const strongApi = compatibility.score !== null && compatibility.score >= 0.5
    const band = nameSignal.kind === 'qualified-name' && (strongApi || compatibility.score === null)
      ? 'strong'
      : nameSignal.kind === 'qualified-name' || strongApi ? 'moderate' : 'weak'
    const conflicts = []
    const shared = sharedClaims.get(entry.component.projectComponentId)
    if (shared && shared.length) {
      conflicts.push(`already implements ${shared.length} design famil${shared.length === 1 ? 'y' : 'ies'} via shared-implementation (${shared.slice(0, 3).join(', ')})`)
    }
    return {
      projectComponentId: entry.component.projectComponentId,
      adapterId: entry.adapterId,
      platform: entry.platform,
      band,
      signals: signals.slice(0, 8),
      matchedProperties: compatibility.matched.slice(0, 40),
      unmatchedProperties: compatibility.unmatched.slice(0, 40),
      conflicts,
      sourcePath: entry.component.source.path,
      sourceSymbol: entry.component.source.symbol,
      autoConfirmForbidden: true
    }
  })
  candidates.sort((a, b) =>
    BANDS.indexOf(a.band) - BANDS.indexOf(b.band) ||
    compareText(a.projectComponentId, b.projectComponentId))
  const bounded = candidates.slice(0, COMPONENT_LIMITS.suggestionsPerComponentMax)
  const ambiguous = candidates.filter((candidate) => candidate.band === 'strong').length > 1
  return { candidates: bounded, ambiguous, limitations }
}
