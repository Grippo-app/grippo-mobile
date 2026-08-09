import { bytesHash, canonicalHash, canonicalJson, compareText } from '../runtime/canonical-json.mjs'
import { TOKEN_IDENTITY_QUALITY, TOKEN_PROVIDER_CAPABILITY } from '../runtime/provider-capabilities.mjs'
import limits from '../runtime/program-limits.cjs'
import {
  canonicalContext,
  canonicalProviderName,
  captureSemanticError,
  observedTokenKeyFor,
  sourceBatchSemanticError,
  sourceBatchSemanticPayload
} from './source-contract.mjs'

const COLOR_RE = /^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/
const CREDENTIAL_PATTERNS = Object.freeze([
  /\bfigd_[A-Za-z0-9_-]{6,}\b/,
  /\bgithub_pat_[A-Za-z0-9_]{20,}\b/,
  /\bghp_[A-Za-z0-9]{20,}\b/,
  /\bsk-(?:proj-)?[A-Za-z0-9_-]{20,}\b/,
  /\bxox[baprs]-[A-Za-z0-9-]{10,}\b/,
  /\bAKIA[0-9A-Z]{16}\b/,
  /\bBearer\s+[A-Za-z0-9._~+/-]{12,}\b/i,
  /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/,
  /(?:^|[?&;\s])(?:access_token|api_key|client_secret|password|token)\s*[:=]\s*[^\s&;"']{8,}/i
])

function credentialLikeString(value) {
  return CREDENTIAL_PATTERNS.some((pattern) => pattern.test(value))
}

function containsCredentialLikeString(value, seen = new Set()) {
  if (typeof value === 'string') return credentialLikeString(value)
  if (!value || typeof value !== 'object') return false
  if (seen.has(value)) return false
  seen.add(value)
  const children = Array.isArray(value) ? value : Object.values(value)
  return children.some((child) => containsCredentialLikeString(child, seen))
}

function sensitiveValueError() {
  return Object.assign(
    new Error('TOKEN_SENSITIVE_VALUE_REJECTED'),
    { code: 'TOKEN_SENSITIVE_VALUE_REJECTED' }
  )
}

function normalizeColor(text) {
  if (!COLOR_RE.test(text)) return null
  const body = text.slice(1).toUpperCase()
  return { space: 'srgb', hex: '#' + (body.length === 6 ? body + 'FF' : body) }
}

function normalizeValue(rawValue, providerType) {
  if (containsCredentialLikeString(rawValue)) throw sensitiveValueError()
  if (typeof rawValue === 'number') {
    if (!Number.isFinite(rawValue) || Object.is(rawValue, -0)) throw new Error('TOKEN_VALUE_INVALID_NUMBER')
    if (providerType && providerType !== 'FLOAT' && providerType !== 'UNKNOWN') {
      return { valueKind: 'unsupported', value: { providerType, primitive: 'number', evidence: rawValue }, limitation: 'provider-type-value-mismatch' }
    }
    return { valueKind: 'number', value: rawValue }
  }
  if (typeof rawValue === 'boolean') {
    if (providerType && providerType !== 'BOOLEAN' && providerType !== 'UNKNOWN') {
      return { valueKind: 'unsupported', value: { providerType, primitive: 'boolean', evidence: rawValue }, limitation: 'provider-type-value-mismatch' }
    }
    return { valueKind: 'boolean', value: rawValue }
  }
  if (typeof rawValue === 'string') {
    if (Buffer.byteLength(rawValue, 'utf8') > limits.tokenStringValueBytesMax) throw new Error('TOKEN_STRING_VALUE_LIMIT_EXCEEDED')
    const color = normalizeColor(rawValue)
    if (providerType === 'COLOR') {
      return color
        ? { valueKind: 'color', value: color }
        : { valueKind: 'unsupported', value: { providerType, primitive: 'string', evidence: rawValue }, limitation: 'invalid-provider-color' }
    }
    if (!providerType || providerType === 'UNKNOWN') {
      if (color) return { valueKind: 'color', value: color }
    }
    if (providerType === 'FLOAT' || providerType === 'BOOLEAN') {
      return { valueKind: 'unsupported', value: { providerType, primitive: 'string', evidence: rawValue }, limitation: 'provider-type-value-mismatch' }
    }
    return { valueKind: 'string', value: rawValue }
  }
  return {
    valueKind: 'unsupported',
    value: { providerType: providerType || 'UNKNOWN', primitive: Array.isArray(rawValue) ? 'array' : 'object', canonical: canonicalJson(rawValue) },
    limitation: 'structured-provider-value-unsupported'
  }
}

export function normalizeSourceCapture(capture, captureBytes, immutablePlan = null) {
  const error = captureSemanticError(capture, immutablePlan)
  if (error) throw new Error(`TOKEN_SOURCE_CAPTURE_INVALID: ${error}`)
  const exactBytes = Buffer.isBuffer(captureBytes)
    ? captureBytes
    : Buffer.from(typeof captureBytes === 'string' ? captureBytes : JSON.stringify(capture), 'utf8')
  if (exactBytes.length > limits.tokenLogicalCaptureBytesMax) throw new Error('TOKEN_SOURCE_CAPTURE_LIMIT_EXCEEDED')

  const byName = new Map()
  for (const raw of capture.observations) {
    const providerName = canonicalProviderName(raw.providerName)
    const normalized = {
      observedTokenKey: observedTokenKeyFor({ ...capture.source, providerName }),
      providerName,
      identityQuality: TOKEN_IDENTITY_QUALITY,
      ...normalizeValue(raw.rawValue, raw.providerType)
    }
    const prior = byName.get(providerName)
    if (prior) {
      if (canonicalJson(prior) !== canonicalJson(normalized)) {
        throw new Error(`TOKEN_SOURCE_DUPLICATE_CONFLICT: ${JSON.stringify(providerName)}`)
      }
      continue
    }
    byName.set(providerName, normalized)
  }
  const observations = [...byName.values()].sort((a, b) => compareText(a.providerName, b.providerName))
  const batch = {
    schemaVersion: 1,
    sourceId: capture.source.sourceId,
    provider: 'figma-mcp',
    providerCapability: TOKEN_PROVIDER_CAPABILITY,
    fileKeyFingerprint: capture.source.fileKeyFingerprint,
    branchKey: capture.source.branchKey.normalize('NFC'),
    nodeId: capture.source.nodeId,
    kind: capture.source.kind,
    context: canonicalContext(capture.source.context),
    origins: immutablePlan && Array.isArray(immutablePlan.origins)
      ? immutablePlan.origins
      : [capture.source.origin],
    identityQuality: TOKEN_IDENTITY_QUALITY,
    captureOperationId: capture.captureOperationId,
    captureSequence: capture.captureSequence,
    batchSemanticHash: 'sha256:' + '0'.repeat(64),
    captureEvidenceHash: bytesHash(exactBytes),
    observations,
    witness: {
      sourceCompleteness: 'complete-returned-payload',
      providerEnumerationCompleteness: 'not-available-for-provider',
      providerTruncationSignal: capture.witness.providerTruncationSignal,
      observationCount: observations.length,
      ...(capture.witness.providerRevision ? { providerRevision: capture.witness.providerRevision } : {})
    }
  }
  batch.batchSemanticHash = canonicalHash(sourceBatchSemanticPayload(batch))
  const semantic = sourceBatchSemanticError(batch)
  if (semantic) throw new Error(`TOKEN_SOURCE_NORMALIZATION_INVALID: ${semantic}`)
  return batch
}
