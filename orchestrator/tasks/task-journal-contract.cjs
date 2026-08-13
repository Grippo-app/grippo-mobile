'use strict'

const STEM_RE = /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/
const KINDS = Object.freeze([
  'phase-start', 'phase-end', 'stop', 'retry', 'gate',
  'design-pulled', 'design-pull-failed', 'task-split', 'follow-up', 'note',
])
const PHASES = Object.freeze([
  'lock', 'prep', 'intake', 'preflight', 'screen-preflight', 'planner',
  'builders', 'diff-sanity', 'validators', 'tests', 'assemble-gate',
  'runtime-verify', 'screenshot-gate', 'review', 'security-review', 'ship',
  'design-pull',
  // The owner-started transaction that moves a sealed candidate into the
  // canonical branch. It is a typed phase, not free text, because the History
  // tab is where an operator reconstructs what happened to a task — and until
  // it existed the single most consequential step, the canonical commit, left
  // no trace there at all.
  'integration',
])
const STATUSES = Object.freeze(['ok', 'blocked', 'escalate', 'fail', 'skipped', 'info'])
const COLUMNS = Object.freeze(['backlog', 'pending', 'todo', 'done'])
const META_KEYS = Object.freeze([
  'blockType', 'checkpointId', 'children', 'gate', 'reasonCode', 'reportId',
  'retryPolicy', 'reviewAttempt', 'reviewer', 'round', 'screens', 'selectionReason',
])
const REVIEWERS = Object.freeze(['codex', 'internal-reviewer'])
const SELECTION_REASONS = Object.freeze([
  'codex-available', 'codex-unavailable', 'forced-codex', 'forced-internal',
])
const REASON_CODES = Object.freeze([
  'checkpoint-stale',
  'codex-check-failed',
  'codex-check-malformed',
  'codex-check-output-limit',
  'codex-check-timeout',
  'codex-auth-missing',
  'codex-contract-missing',
  'codex-invocation-failed',
  'codex-not-installed',
  'codex-plugin-broken',
  'codex-plugin-disabled',
  'config-invalid',
  'fallback-used',
  'manual-retry',
  'phase-blocked',
  'phase-failed',
  'require-codex-blocked',
  'review-failed',
  'reviewer-invocation-failed',
  'runtime-validation-failed',
  'source-revision-changed',
])
const RETRY_POLICIES = Object.freeze([
  'retry-phase', 'resume-run', 'restart-from-phase', 'restart-task', 'manual',
])
const REVIEW_REASON_CODES = new Set([
  'codex-check-failed',
  'codex-check-malformed',
  'codex-check-output-limit',
  'codex-check-timeout',
  'codex-auth-missing',
  'codex-contract-missing',
  'codex-invocation-failed',
  'codex-not-installed',
  'codex-plugin-broken',
  'codex-plugin-disabled',
  'config-invalid',
  'fallback-used',
  'require-codex-blocked',
  'review-failed',
  'reviewer-invocation-failed',
])
const EVENT_FIELDS = Object.freeze(['column', 'detail', 'durationMs', 'kind', 'meta', 'phase', 'status', 'stem', 'ts'])
const REQUIRED_FIELDS = Object.freeze(['kind', 'stem', 'ts'])

const KIND_SET = new Set(KINDS)
const PHASE_SET = new Set(PHASES)
const STATUS_SET = new Set(STATUSES)
const COLUMN_SET = new Set(COLUMNS)
const META_KEY_SET = new Set(META_KEYS)
const ISO_RE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3})?Z$/
const MAX_DETAIL_SCALARS = 200
const MAX_META_SCALARS = 500
const MAX_META_ENTRIES = 8
const MAX_EVENT_BYTES = 2048
const MAX_JOURNAL_BYTES = 8 * 1024 * 1024

function exactKeys(value, allowed, required) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false
  const keys = Object.keys(value).sort()
  return keys.every((key) => allowed.includes(key)) && (required || []).every((key) => keys.includes(key))
}

function scalarLength(value) {
  let count = 0
  for (const _ of value) count++
  return count
}

function boundedOneLine(value, maxScalars, maxBytes) {
  return typeof value === 'string' && value.length > 0 && scalarLength(value) <= maxScalars &&
    Buffer.byteLength(value, 'utf8') <= maxBytes && !/[\0\r\n]/.test(value)
}

function validStem(value) {
  if (typeof value !== 'string' || value.length > 120 || !STEM_RE.test(value)) return false
  const numeric = value.slice(5, value.indexOf('_', 5))
  const id = Number(numeric)
  return Number.isSafeInteger(id) && id > 0 && String(id) === numeric
}

function validIso(value) {
  if (typeof value !== 'string' || !ISO_RE.test(value) || !Number.isFinite(Date.parse(value))) return false
  const date = new Date(value)
  const canonical = value.includes('.') ? date.toISOString() : date.toISOString().replace('.000Z', 'Z')
  const year = date.getUTCFullYear()
  return canonical === value && year >= 2000 && year <= 9999
}

function canonicalPositiveDecimal(value, maximum) {
  return typeof value === 'string' && /^[1-9][0-9]*$/.test(value) &&
    Number.isSafeInteger(Number(value)) && Number(value) <= maximum
}

function referenceId(value) {
  return typeof value === 'string' && /^[A-Za-z0-9][A-Za-z0-9._:-]{0,119}$/.test(value)
}

function validateTypedMeta(value) {
  if (value.phase === 'review') {
    if (value.kind === 'phase-start' && value.status !== 'info') {
      return 'review phase-start status must be info'
    }
    if (value.kind === 'phase-end' && !['ok', 'fail'].includes(value.status)) {
      return 'review phase-end status must be ok or fail'
    }
    if (value.kind === 'stop' && !['blocked', 'escalate', 'fail'].includes(value.status)) {
      return 'review stop status must be blocked, escalate, or fail'
    }
    if (value.kind === 'gate' &&
        !['info', 'blocked', 'escalate', 'fail'].includes(value.status)) {
      return 'review gate status is invalid'
    }
  }
  const meta = value.meta
  if (!meta) {
    return value.phase === 'review' &&
      (value.kind === 'phase-start' || value.kind === 'phase-end' || value.kind === 'stop' ||
       (value.kind === 'gate' && value.status !== 'info'))
      ? 'review lifecycle event requires reviewer and reviewAttempt'
      : null
  }
  const hasReviewMeta = ['reviewer', 'reviewAttempt', 'selectionReason'].some((key) =>
    Object.prototype.hasOwnProperty.call(meta, key))
  if (hasReviewMeta || (value.phase === 'review' && meta.reasonCode !== undefined)) {
    if (value.phase !== 'review' || !['phase-start', 'phase-end', 'stop', 'gate'].includes(value.kind)) {
      return 'review meta is not allowed for this event'
    }
  }
  const hasReviewer = Object.prototype.hasOwnProperty.call(meta, 'reviewer')
  const hasReviewAttempt = Object.prototype.hasOwnProperty.call(meta, 'reviewAttempt')
  if (hasReviewer !== hasReviewAttempt) return 'event reviewer and reviewAttempt must be recorded together'
  if (value.phase === 'review' &&
      (value.kind === 'phase-start' || value.kind === 'phase-end' || value.kind === 'stop' ||
       (value.kind === 'gate' && value.status !== 'info')) &&
      !hasReviewer) {
    return 'review lifecycle event requires reviewer and reviewAttempt'
  }
  if (meta.reviewer !== undefined && !REVIEWERS.includes(meta.reviewer)) return 'event reviewer is invalid'
  if (meta.reviewAttempt !== undefined && !canonicalPositiveDecimal(meta.reviewAttempt, 99)) {
    return 'event reviewAttempt is invalid'
  }
  if (meta.selectionReason !== undefined) {
    if (!hasReviewer || value.kind !== 'phase-start' || value.phase !== 'review' ||
        !SELECTION_REASONS.includes(meta.selectionReason)) {
      return 'event selectionReason is invalid'
    }
    const reviewerReasons = meta.reviewer === 'codex'
      ? ['codex-available', 'forced-codex']
      : ['codex-unavailable', 'forced-internal']
    if (!reviewerReasons.includes(meta.selectionReason)) {
      return 'event reviewer and selectionReason are inconsistent'
    }
  }
  if (value.kind === 'phase-start' && hasReviewer && meta.selectionReason === undefined) {
    return 'review phase-start requires selectionReason'
  }
  if (meta.reasonCode !== undefined && !REASON_CODES.includes(meta.reasonCode)) return 'event reasonCode is invalid'
  if (REVIEW_REASON_CODES.has(meta.reasonCode) &&
      (value.phase !== 'review' || !['phase-start', 'phase-end', 'stop', 'gate'].includes(value.kind))) {
    return 'review reasonCode is not allowed for this event'
  }
  if (meta.reasonCode === 'fallback-used' &&
      (value.kind !== 'phase-start' || meta.reviewer !== 'internal-reviewer' ||
       meta.selectionReason !== 'codex-unavailable')) {
    return 'event fallback-used metadata is inconsistent'
  }
  if (meta.reasonCode === 'require-codex-blocked' &&
      (!hasReviewer || meta.reviewer !== 'codex' ||
       !['phase-end', 'stop', 'gate'].includes(value.kind))) {
    return 'event require-codex-blocked metadata is inconsistent'
  }
  if (['review-failed', 'reviewer-invocation-failed'].includes(meta.reasonCode) &&
      (!hasReviewer || !['phase-end', 'stop'].includes(value.kind))) {
    return 'event review failure metadata is inconsistent'
  }
  if (meta.checkpointId !== undefined) {
    if (!['phase-start', 'phase-end', 'stop', 'retry', 'gate', 'note'].includes(value.kind) ||
        !referenceId(meta.checkpointId)) return 'event checkpointId is invalid'
  }
  if (meta.reportId !== undefined) {
    if (!['phase-end', 'gate', 'note'].includes(value.kind) || !referenceId(meta.reportId)) {
      return 'event reportId is invalid'
    }
  }
  if (meta.retryPolicy !== undefined) {
    if (value.kind !== 'retry' || !RETRY_POLICIES.includes(meta.retryPolicy)) return 'event retryPolicy is invalid'
  }
  return null
}

function validateEvent(value, expectedStem) {
  if (!exactKeys(value, EVENT_FIELDS, REQUIRED_FIELDS)) return 'event fields do not match the canonical schema'
  if (!validStem(value.stem) || (expectedStem && value.stem !== expectedStem)) return 'event stem is invalid'
  if (!validIso(value.ts)) return 'event timestamp is invalid'
  if (!KIND_SET.has(value.kind)) return 'event kind is invalid'
  if (value.phase !== undefined && !PHASE_SET.has(value.phase)) return 'event phase is invalid'
  if (value.status !== undefined && !STATUS_SET.has(value.status)) return 'event status is invalid'
  if (value.column !== undefined && !COLUMN_SET.has(value.column)) return 'event column is invalid'
  if (value.durationMs !== undefined && (!Number.isSafeInteger(value.durationMs) || value.durationMs < 0 || value.durationMs > 86_400_000)) {
    return 'event durationMs is invalid'
  }
  if (value.detail !== undefined && !boundedOneLine(value.detail, MAX_DETAIL_SCALARS, 800)) return 'event detail is invalid'
  if (value.meta !== undefined) {
    if (!exactKeys(value.meta, META_KEYS) || Object.keys(value.meta).length < 1 || Object.keys(value.meta).length > MAX_META_ENTRIES) {
      return 'event meta fields are invalid'
    }
    for (const [key, item] of Object.entries(value.meta)) {
      if (!META_KEY_SET.has(key) || !boundedOneLine(item, MAX_META_SCALARS, 1000)) return 'event meta value is invalid'
    }
  }
  const typedMetaIssue = validateTypedMeta(value)
  if (typedMetaIssue) return typedMetaIssue
  return null
}

function publicEvent(value, expectedStem) {
  if (validateEvent(value, expectedStem)) return null
  const out = { ts: value.ts, stem: value.stem, kind: value.kind }
  for (const key of ['phase', 'status', 'column', 'durationMs', 'detail']) {
    if (value[key] !== undefined) out[key] = value[key]
  }
  if (value.meta !== undefined) {
    out.meta = {}
    for (const key of Object.keys(value.meta).sort()) out.meta[key] = value.meta[key]
  }
  return out
}

module.exports = Object.freeze({
  STEM_RE,
  KINDS,
  PHASES,
  STATUSES,
  COLUMNS,
  META_KEYS,
  REVIEWERS,
  SELECTION_REASONS,
  REASON_CODES,
  RETRY_POLICIES,
  MAX_META_ENTRIES,
  MAX_EVENT_BYTES,
  MAX_JOURNAL_BYTES,
  validStem,
  validateEvent,
  publicEvent,
})
