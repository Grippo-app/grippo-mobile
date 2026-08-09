'use strict'

// Durable, prompt-free phase checkpoint contract shared by the producer CLI
// and the site read/admission path. Validation is deliberately exact: a
// current schemaVersion never accepts extension fields or ambiguous values.

const crypto = require('crypto')
const journal = require('./task-journal-contract.cjs')

// One current checkpoint protocol. Every document binds the machine test-policy
// hash into its identity and fingerprint; every unknown version, missing field,
// or extension field is rejected without conversion or cleanup.
const SCHEMA_VERSION = 1
const MAX_BYTES = 64 * 1024
const MAX_PER_TASK = 30
const MAX_RECEIPTS = 50
const HASH_RE = /^sha256:[a-f0-9]{64}$/
const CHECKPOINT_ID_RE = /^cp-[a-f0-9]{32}$/
const REFERENCE_ID_RE = /^[A-Za-z0-9][A-Za-z0-9._:-]{0,119}$/
const STATES = Object.freeze(['backlog', 'pending', 'todo', 'done'])
const STATUSES = Object.freeze(['completed', 'failed', 'blocked'])
const FAILURE_CODES = Object.freeze([
  'checkpoint-stale', 'config-invalid', 'dependency-blocked', 'figma-required',
  'phase-blocked', 'phase-failed', 'review-failed',
  'runtime-validation-failed', 'source-revision-changed', 'validation-failed',
])
const REASON_CODES = Object.freeze([
  'checkpoint-stale', 'config-invalid', 'dependency-blocked', 'manual-retry',
  'phase-blocked', 'phase-failed', 'review-failed',
  'runtime-validation-failed', 'source-revision-changed', 'validation-failed',
])
const RETRY_KINDS = Object.freeze([
  'retry-phase', 'resume-run', 'restart-from-phase', 'restart-task', 'manual',
])
const FIELDS = Object.freeze([
  'schemaVersion', 'checkpointId', 'stem', 'runId', 'phase', 'attempt',
  'status', 'createdAt', 'taskState', 'taskSourceRevision',
  'projectSourceRevision', 'configHash', 'dependencySnapshotHash',
  'inputFingerprint', 'testPolicyHash', 'outputReceiptIds',
  'priorPhaseReceiptIds', 'failureCode', 'retryPolicy', 'checkpointHash',
])
const POLICY_FIELDS = Object.freeze(['kind', 'safePhase', 'reasonCode'])
// `tests` is exact-retryable ONLY for infrastructure BLOCKED / the single
// diagnostic rerun: the prior FAIL receipt stays in history and the flaky rule
// (fail → pass on retry = BLOCKED) makes a green-by-retry impossible.
const EXACT_RETRY_PHASES = Object.freeze([
  'preflight', 'validators', 'tests', 'assemble-gate', 'runtime-verify',
  'screenshot-gate', 'review', 'security-review', 'design-pull',
])

function canonical(value) {
  if (value === null || typeof value !== 'object') return JSON.stringify(value)
  if (Array.isArray(value)) return '[' + value.map(canonical).join(',') + ']'
  return '{' + Object.keys(value).sort().map((key) =>
    JSON.stringify(key) + ':' + canonical(value[key])).join(',') + '}'
}

function hash(value) {
  return 'sha256:' + crypto.createHash('sha256').update(canonical(value), 'utf8').digest('hex')
}

function exactKeys(value, fields) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === fields.slice().sort().join('\0')
}

function validIso(value) {
  if (typeof value !== 'string' || value.length > 32 ||
      !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3})?Z$/.test(value) ||
      !Number.isFinite(Date.parse(value))) return false
  const parsed = new Date(value)
  const canonicalIso = value.includes('.') ? parsed.toISOString() :
    parsed.toISOString().replace('.000Z', 'Z')
  return canonicalIso === value && parsed.getUTCFullYear() >= 2000 &&
    parsed.getUTCFullYear() <= 9999
}

function validReceiptList(value) {
  return Array.isArray(value) && value.length <= MAX_RECEIPTS &&
    value.every((item) => typeof item === 'string' && REFERENCE_ID_RE.test(item)) &&
    new Set(value).size === value.length
}

function hashInput(value) {
  const copy = {}
  FIELDS.forEach((key) => {
    if (key !== 'checkpointHash') copy[key] = value[key]
  })
  return copy
}

function validate(value, expectedStem) {
  if (!exactKeys(value, FIELDS)) return 'checkpoint fields are invalid'
  if (value.schemaVersion !== SCHEMA_VERSION) return 'checkpoint schemaVersion is unsupported'
  if (!CHECKPOINT_ID_RE.test(String(value.checkpointId || ''))) return 'checkpoint id is invalid'
  if (!journal.validStem(value.stem) || expectedStem && value.stem !== expectedStem) return 'checkpoint stem is invalid'
  if (!REFERENCE_ID_RE.test(String(value.runId || ''))) return 'checkpoint run id is invalid'
  if (!journal.PHASES.includes(value.phase)) return 'checkpoint phase is invalid'
  if (!Number.isSafeInteger(value.attempt) || value.attempt < 1 || value.attempt > 99) return 'checkpoint attempt is invalid'
  if (!STATUSES.includes(value.status)) return 'checkpoint status is invalid'
  if (!validIso(value.createdAt)) return 'checkpoint timestamp is invalid'
  if (!STATES.includes(value.taskState)) return 'checkpoint task state is invalid'
  for (const key of ['taskSourceRevision', 'projectSourceRevision', 'configHash',
    'dependencySnapshotHash', 'inputFingerprint', 'testPolicyHash', 'checkpointHash']) {
    if (!HASH_RE.test(String(value[key] || ''))) return 'checkpoint ' + key + ' is invalid'
  }
  if (!validReceiptList(value.outputReceiptIds) ||
      !validReceiptList(value.priorPhaseReceiptIds)) return 'checkpoint receipt ids are invalid'
  if (value.failureCode !== null && !FAILURE_CODES.includes(value.failureCode)) return 'checkpoint failure code is invalid'
  if (value.status === 'failed' && value.failureCode === null) return 'failed checkpoint requires failureCode'
  if (value.status === 'completed' && value.failureCode !== null) return 'completed checkpoint forbids failureCode'
  if (!exactKeys(value.retryPolicy, POLICY_FIELDS) ||
      !RETRY_KINDS.includes(value.retryPolicy.kind) ||
      value.retryPolicy.safePhase !== null && !journal.PHASES.includes(value.retryPolicy.safePhase) ||
      value.retryPolicy.reasonCode !== null && !REASON_CODES.includes(value.retryPolicy.reasonCode)) {
    return 'checkpoint retry policy is invalid'
  }
  if (['retry-phase', 'resume-run', 'restart-from-phase'].includes(value.retryPolicy.kind) &&
      value.retryPolicy.safePhase === null) return 'retry checkpoint requires safePhase'
  if (value.retryPolicy.kind === 'restart-task' && value.retryPolicy.safePhase !== null) {
    return 'restart-task checkpoint forbids safePhase'
  }
  if (value.retryPolicy.kind === 'retry-phase' &&
      !['failed', 'blocked'].includes(value.status)) return 'retry-phase requires failed or blocked status'
  if (value.retryPolicy.kind === 'retry-phase' &&
      (!EXACT_RETRY_PHASES.includes(value.phase) ||
        value.retryPolicy.safePhase !== value.phase)) {
    return 'phase is not eligible for exact retry'
  }
  if (value.retryPolicy.kind === 'resume-run' &&
      (value.phase !== 'planner' || value.status !== 'completed' ||
        value.retryPolicy.safePhase !== 'planner')) {
    return 'resume-run requires a complete planner checkpoint'
  }
  if (value.retryPolicy.kind === 'resume-run' && value.priorPhaseReceiptIds.length < 1) {
    return 'resume-run requires a verified planner input receipt'
  }
  if (value.retryPolicy.kind === 'restart-from-phase') {
    const phaseIndex = journal.PHASES.indexOf(value.phase)
    const safeIndex = journal.PHASES.indexOf(value.retryPolicy.safePhase)
    if (safeIndex < 0 || safeIndex > phaseIndex ||
        ['lock', 'prep', 'intake', 'ship'].includes(value.retryPolicy.safePhase)) {
      return 'restart-from-phase safePhase is invalid'
    }
    if (value.priorPhaseReceiptIds.length < 1) {
      return 'restart-from-phase requires verified prior phase receipts'
    }
    if (safeIndex > journal.PHASES.indexOf('tests') &&
        !value.priorPhaseReceiptIds.some((id) => id.startsWith('test-summary:'))) {
      return 'restart past the tests phase requires the current PASS test-summary receipt'
    }
  }
  if (value.checkpointHash !== hash(hashInput(value))) return 'checkpoint hash does not match content'
  if (Buffer.byteLength(canonical(value), 'utf8') > MAX_BYTES) return 'checkpoint exceeds its byte limit'
  return null
}

function seal(value) {
  const copy = Object.assign({}, value, {
    outputReceiptIds: (value.outputReceiptIds || []).slice(),
    priorPhaseReceiptIds: (value.priorPhaseReceiptIds || []).slice(),
    retryPolicy: Object.assign({}, value.retryPolicy),
  })
  copy.checkpointHash = hash(hashInput(copy))
  const issue = validate(copy, copy.stem)
  if (issue) throw new Error(issue)
  return copy
}

function publicProjection(value, freshness) {
  if (validate(value, value && value.stem)) return null
  return {
    schemaVersion: value.schemaVersion,
    checkpointId: value.checkpointId,
    checkpointHash: value.checkpointHash,
    phase: value.phase,
    attempt: value.attempt,
    status: value.status,
    createdAt: value.createdAt,
    failureCode: value.failureCode,
    retryPolicy: Object.assign({}, value.retryPolicy),
    reusableReceiptCount: value.outputReceiptIds.length + value.priorPhaseReceiptIds.length,
    freshness: freshness || { current: null, reasonCode: 'freshness-not-checked' },
  }
}

module.exports = Object.freeze({
  SCHEMA_VERSION,
  MAX_BYTES,
  MAX_PER_TASK,
  MAX_RECEIPTS,
  HASH_RE,
  CHECKPOINT_ID_RE,
  REFERENCE_ID_RE,
  STATES,
  STATUSES,
  FAILURE_CODES,
  REASON_CODES,
  RETRY_KINDS,
  EXACT_RETRY_PHASES,
  canonical,
  hash,
  validate,
  seal,
  publicProjection,
})
