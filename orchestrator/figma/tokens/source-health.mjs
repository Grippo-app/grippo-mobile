// ESM facade over the one CommonJS state-machine owner used by the site
// runtime. Keeping this surface lets deterministic runner/tests import named
// functions without maintaining a second implementation.
import core from '../runtime/token-source-health-core.cjs'

export const materializeSourceHealth = core.materializeSourceHealth
export const reserveSourceSequence = core.reserveSourceSequence
export const sourceHealthSemanticError = core.sourceHealthSemanticError
export const sourceHealthSnapshotHash = core.sourceHealthSnapshotHash
export const sourceFreshness = core.sourceFreshness
