// Baseline publication rules for the component comparison. The comparator
// emits a baseline candidate only for structurally clean runs; this module
// finalizes it at the mutation boundary (injected clock, REQ-CODE-004) or
// explains why the previous baseline must be carried forward unchanged.
import { typedError } from '../runtime/typed-error.mjs'
import { COMPONENT_ERROR_CODES } from './error-codes.mjs'

const INSTANT_RE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/

// candidate: comparator output (baselineCandidate). now: ISO instant supplied
// by the caller. previousBaseline: the currently published baseline or null.
// Returns the exact baseline artifact to publish with this comparison.
export function publishableBaseline(candidate, previousBaseline, now) {
  if (!INSTANT_RE.test(String(now || ''))) {
    throw typedError(COMPONENT_ERROR_CODES.COMPONENT_COMPARISON_BASELINE_INELIGIBLE, 'baseline publication requires an exact ISO instant')
  }
  if (candidate) {
    return {
      ...candidate,
      source: { ...candidate.source, eligibleAt: now }
    }
  }
  if (previousBaseline) return previousBaseline
  return null
}
