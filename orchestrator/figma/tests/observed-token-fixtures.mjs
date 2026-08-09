import { canonicalHash } from '../runtime/canonical-json.mjs'
import { sourceIdFor } from '../tokens/source-contract.mjs'

export const FILE_FINGERPRINT = 'sha256:' + 'a'.repeat(64)
const ACCOUNT_FINGERPRINT = 'sha256:' + 'b'.repeat(64)

export function sourceIdentity(overrides = {}) {
  const source = {
    fileKeyFingerprint: FILE_FINGERPRINT,
    branchKey: 'none',
    nodeId: '10:20',
    kind: 'screen',
    context: { theme: 'light', locale: 'default', platform: 'shared' },
    origin: {
      kind: 'task-screen',
      taskStem: 'TASK_42_Home',
      screenKey: 'Home',
      variantId: 'light-default-shared'
    },
    ...overrides
  }
  source.sourceId = sourceIdFor(source)
  return source
}

export function validObservedCapture(overrides = {}) {
  const source = overrides.source || sourceIdentity()
  const observations = overrides.observations || [
    { providerName: 'color/content/primary', rawValue: '#336699', providerType: 'COLOR' },
    { providerName: 'spacing/md', rawValue: 16, providerType: 'FLOAT' },
    { providerName: 'content/title', rawValue: 'Welcome', providerType: 'STRING' },
    { providerName: 'feature/enabled', rawValue: true, providerType: 'BOOLEAN' }
  ]
  const witness = {
    startedAt: '2026-07-23T10:00:00.000Z',
    finishedAt: '2026-07-23T10:00:01.000Z',
    nodeId: source.nodeId,
    operation: 'get_variable_defs',
    sourceCompleteness: 'complete-returned-payload',
    providerEnumerationCompleteness: 'not-available-for-provider',
    providerTruncationSignal: 'unavailable',
    truncated: false,
    issues: [],
    observationCount: observations.length,
    accountFingerprint: ACCOUNT_FINGERPRINT,
    connectorRevision: 'figma-mcp-v1',
    producerVersion: 'fixture-v1',
    ...(overrides.witness || {})
  }
  const capture = {
    schemaVersion: 1,
    providerCapability: 'node-bound-resolved-variables',
    provider: 'figma-mcp',
    captureOperationId: 'tokop_0123456789abcdef',
    captureSequence: 1,
    accountFingerprint: ACCOUNT_FINGERPRINT,
    connectorRevision: 'figma-mcp-v1',
    source,
    observations,
    witness,
    ...overrides,
    witness
  }
  capture.source = source
  capture.observations = observations
  capture.witness = { ...capture.witness, observationCount: observations.length }
  return capture
}

export function immutablePlan(capture) {
  return {
    captureOperationId: capture.captureOperationId,
    captureSequence: capture.captureSequence,
    accountFingerprint: capture.accountFingerprint,
    connectorRevision: capture.connectorRevision,
    sourceId: capture.source.sourceId,
    preflightHash: canonicalHash({
      captureOperationId: capture.captureOperationId,
      captureSequence: capture.captureSequence,
      sourceId: capture.source.sourceId
    })
  }
}
