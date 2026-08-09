// Shared grammar for the `- Figma meta:` digest bullet — the machine-emitted proof that a
// FINAL Figma↔app comparison actually ran for a UI task. Single source of truth for the
// node side: AUTHORED by evidence-bundle.mjs (`--stage final`), CONSUMED by ship-done.mjs
// (pre-`mv` interlock) and verify-done.mjs (headless `done/` auditor). The digest is
// code-emitted, so a skipped done-gate or a hand-`mv` leaves no digest, which the
// interlock and auditor both catch.
//
// NODE-SIDE ONLY: the digest is authored and consumed entirely by these three sidecar scripts.
// The site board does not parse it, so there is no cross-runtime parser mirror to
// keep in lock-step. (The `KNOWN_OVERALL` set below still mirrors
// board.js `evidenceStatusClass`'s OVERALL-field vocabulary — see the KNOWN_OVERALL note below.)

import { assertTaskStem } from './report-utils.mjs'
import { outcomeSectionLines } from './outcome-shape.mjs'

const FIGMA_META_SCHEMA = 'figma-comparison-v2'
const SHA256_REPORT_RE = /^sha256:[0-9a-f]{64}$/i
// The OVERALL-field vocabulary a digest may carry — a strict SUBSET of board.js
// evidenceStatusClass()'s status set (which also classifies per-SCREEN bail statuses like
// ASPECT_MISMATCH/MISSING_CAPTURE that never appear as a bundle `overall`). Not a full mirror.
const KNOWN_OVERALL = new Set([
  'PASS', 'WARN', 'WARNING', 'MINOR', 'MAJOR', 'SKIPPED',
  'INCOMPLETE', 'REVIEW_REQUIRED', 'BLOCKER', 'FAIL', 'ERROR', 'MISSING',
])

// Build the canonical digest bullet (returns the full `- Figma meta: …` line, ready to
// write into a file or paste verbatim into the appendix). `stale` is always `false`:
// the digest is only emitted by a fresh `--stage final` bundle, so a stale digest cannot
// legitimately exist. `designHash` binds the certification to the exact `## Design`
// section, and `gatePolicyVersion` names the strictness policy that certified it.
export function buildFigmaMeta({ taskStem, stage, overall, pipelineRunId, evidenceReportHash, screenshotReportHash, generatedAt, visualChecks, problemCount, designHash, gatePolicyVersion, tokenObservationManifestHash, rows }) {
  const safeTaskStem = assertTaskStem(taskStem)
  const fields = [
    `schema=${FIGMA_META_SCHEMA}`,
    `taskStem=${safeTaskStem}`,
    `stage=${stage}`,
    `overall=${overall}`,
    `pipelineRunId=${pipelineRunId}`,
    `evidenceReportHash=${evidenceReportHash}`,
    `screenshotReportHash=${screenshotReportHash}`,
    `generatedAt=${generatedAt}`,
    `stale=false`,
    `visualChecks=${visualChecks}`,
    `problemCount=${problemCount}`,
  ]
  if (!SHA256_REPORT_RE.test(String(designHash || ''))) throw new Error('designHash must be a sha256 digest')
  if (!SHA256_REPORT_RE.test(String(tokenObservationManifestHash || ''))) throw new Error('tokenObservationManifestHash must be a sha256 digest')
  if (!Number.isSafeInteger(Number(gatePolicyVersion)) || Number(gatePolicyVersion) < 1) throw new Error('gatePolicyVersion must be a positive integer')
  // Binding fields go BEFORE rows= — rows stays the trailing field (its cells are the only
  // free-form content; keeping it last preserves the existing truncation-safety reasoning).
  fields.push(`designHash=${designHash}`)
  fields.push(`gatePolicyVersion=${gatePolicyVersion}`)
  fields.push(`tokenObservationManifestHash=${tokenObservationManifestHash}`)
  fields.push(`rows=${rows}`)
  return `- Figma meta: ${fields.join('; ')}`
}

// Parse + validate a digest line (with or without the leading `- ` bullet marker — the
// execution-log stores bullets without it). Returns the field object, or null if the line
// is not a structurally valid final digest. Consumed by ship-done.mjs + verify-done.mjs.
export function parseFigmaMeta(line) {
  let text = String(line || '').trim()
  if (text.startsWith('- ')) text = text.slice(2).trim()
  if (text.indexOf('Figma meta:') !== 0) return null
  const raw = text.substring('Figma meta:'.length).trim()
  const out = {}
  for (const part of raw.split(';')) {
    const s = part.trim()
    if (!s) continue
    const eq = s.indexOf('=')
    if (eq <= 0) continue
    out[s.substring(0, eq).trim()] = s.substring(eq + 1).trim()
  }
  if (out.schema !== FIGMA_META_SCHEMA) return null
  // taskStem binds the digest to ITS task file, so a valid PASS digest copied from another
  // done task (a hand-`mv` forge) can be caught by ship-done/verify-done comparing it to the stem.
  try { assertTaskStem(out.taskStem || '') } catch { return null }
  if (out.stage !== 'final') return null
  if (!out.overall || !KNOWN_OVERALL.has(String(out.overall).toUpperCase())) return null
  if (!out.pipelineRunId) return null
  if (!SHA256_REPORT_RE.test(out.evidenceReportHash || '')) return null
  if (!SHA256_REPORT_RE.test(out.screenshotReportHash || '')) return null
  if (out.generatedAt && Number.isNaN(Date.parse(out.generatedAt))) return null
  if (out.stale !== 'false') return null
  if (out.visualChecks && !/^\d+$/.test(out.visualChecks)) return null
  if (out.problemCount && !/^\d+$/.test(out.problemCount)) return null
  if (!SHA256_REPORT_RE.test(out.designHash || '')) return null
  if (!SHA256_REPORT_RE.test(out.tokenObservationManifestHash || '')) return null
  if (!/^[1-9]\d*$/.test(out.gatePolicyVersion || '')) return null
  return out
}

// A digest's overall is "shippable" iff it cleared the gate: PASS, or a WARN-family verdict
// (a reviewed WARN recorded in ### Caveats is acceptable per the Step-6b contract). A
// BLOCKER/INCOMPLETE/REVIEW_REQUIRED digest must never accompany a `done/` move.
export function isShippableOverall(overall) {
  const s = String(overall || '').toUpperCase()
  // A final bundle's overall is only ever PASS / WARN / BLOCKER / INCOMPLETE /
  // REVIEW_REQUIRED (overallOf + hasBlocker). Shippable = PASS or a reviewed WARN.
  return s === 'PASS' || s === 'WARN' || s === 'WARNING'
}

// True iff the appendix's `### Caveats` section carries at least one REAL bullet (not `- none`).
// A reviewed-WARN ship requires an actual recorded caveat — `### Caveats\n- none` is not one.
export function caveatsHaveContent(md) {
  const lines = outcomeSectionLines(md, 'Caveats')
  if (!lines) return false
  return lines.some((line) => {
    const t = line.trim()
    if (t.charAt(0) !== '-') return false
    const body = t.replace(/^-+\s*/, '').trim().toLowerCase()
    return body && body !== 'none'
  })
}
