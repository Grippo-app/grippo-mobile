'use strict';

// Public Figma sync failures are a deliberately small, localized contract.
// Exact domain/runtime codes remain diagnostics; this module is the only
// owner that projects them into durable browser-facing history codes.

var PUBLIC_CODES = Object.freeze({
  tokenAccessDegraded: 'token-source-access-degraded',
  tokenCaptureIncomplete: 'token-source-capture-incomplete',
  tokenCaptureInconsistent: 'token-source-capture-inconsistent',
  tokenCaptureInvalid: 'token-source-capture-invalid',
  tokenGenerationResyncRequired: 'token-generation-resync-required',
  tokenPublicationLimit: 'token-publication-limit',
  tokenSyncFailed: 'token-sync-failed',
  componentAccessDegraded: 'component-design-access-degraded',
  componentCaptureIncomplete: 'component-design-capture-incomplete',
  componentCaptureInconsistent: 'component-design-capture-inconsistent',
  componentCaptureInvalid: 'component-design-capture-invalid',
  componentGenerationResyncRequired: 'component-generation-resync-required',
  componentSyncFailed: 'component-sync-failed',
  projectAdaptersUnconfigured: 'project-adapters-unconfigured',
  projectAdapterInvalid: 'project-adapter-invalid',
  projectAdapterTimeout: 'project-adapter-timeout',
  tokenComparisonFailed: 'token-comparison-failed',
  componentComparisonFailed: 'component-comparison-failed',
  syncHistoryUnavailable: 'sync-history-unavailable',
  syncRecoveryRequired: 'sync-recovery-required',
  syncTimeout: 'sync-timeout',
  syncSessionUnavailable: 'sync-session-unavailable',
  syncPublicationFailed: 'sync-publication-failed',
  syncCancelled: 'sync-cancelled',
  syncNoValidGroups: 'sync-no-valid-groups',
  jobInterrupted: 'job-interrupted',
  tokenHealthRecoveryRequired: 'token-health-recovery-required',
  unknown: 'unknown'
});

var PUBLIC_SET = Object.freeze(Object.keys(PUBLIC_CODES).reduce(function (out, key) {
  out[PUBLIC_CODES[key]] = true;
  return out;
}, Object.create(null)));

var DOMAIN_TOKEN_CODES = Object.freeze({
  TOKEN_PROVIDER_CAPABILITY_UNSUPPORTED: PUBLIC_CODES.tokenAccessDegraded,
  TOKEN_SOURCE_CAPTURE_INCOMPLETE: PUBLIC_CODES.tokenCaptureIncomplete,
  TOKEN_SOURCE_SET_CHANGED: PUBLIC_CODES.tokenCaptureInconsistent,
  TOKEN_SOURCE_SCOPE_CHANGED: PUBLIC_CODES.tokenCaptureInconsistent,
  TOKEN_SOURCE_SCOPE_MISMATCH: PUBLIC_CODES.tokenCaptureInconsistent,
  TOKEN_SOURCE_SEQUENCE_CONFLICT: PUBLIC_CODES.tokenCaptureInconsistent,
  TOKEN_SOURCE_SEQUENCE_SUPERSEDED: PUBLIC_CODES.tokenCaptureInconsistent,
  TOKEN_SOURCE_EVIDENCE_CONFLICT: PUBLIC_CODES.tokenCaptureInconsistent,
  TOKEN_GENERATION_RESYNC_REQUIRED: PUBLIC_CODES.tokenGenerationResyncRequired,
  TOKEN_GENERATION_ARTIFACT_INVALID: PUBLIC_CODES.tokenGenerationResyncRequired,
  TOKEN_PUBLICATION_BUDGET_EXCEEDED: PUBLIC_CODES.tokenPublicationLimit,
  TOKEN_INDEX_CATALOG_LIMIT_EXCEEDED: PUBLIC_CODES.tokenPublicationLimit,
  TOKEN_SOURCE_SHARD_LIMIT_EXCEEDED: PUBLIC_CODES.tokenPublicationLimit,
  TOKEN_SOURCE_SHARDS_TOTAL_LIMIT_EXCEEDED: PUBLIC_CODES.tokenPublicationLimit,
  TOKEN_SOURCE_CAPTURE_LIMIT_EXCEEDED: PUBLIC_CODES.tokenPublicationLimit,
  TOKEN_OBSERVATION_LIMIT_EXCEEDED: PUBLIC_CODES.tokenPublicationLimit,
  TOKEN_KEY_LIMIT_EXCEEDED: PUBLIC_CODES.tokenPublicationLimit,
  TOKEN_STRING_VALUE_LIMIT_EXCEEDED: PUBLIC_CODES.tokenPublicationLimit,
  TOKEN_SOURCE_CAPTURE_INVALID: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_SOURCE_DUPLICATE_CONFLICT: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_NAME_CANONICALIZATION_COLLISION: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_SOURCE_CONTEXT_INVALID: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_SOURCE_NORMALIZATION_INVALID: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_SOURCE_BUCKET_INVALID: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_SOURCE_BATCH_INVALID: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_SOURCE_ID_INVALID: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_SOURCE_OPERATION_ID_INVALID: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_SOURCE_SEQUENCE_INVALID: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_SENSITIVE_VALUE_REJECTED: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_PUBLICATION_OUTPUT_INVALID: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_RUNNER_ARTIFACT_UNEXPECTED: PUBLIC_CODES.tokenCaptureInvalid,
  TOKEN_RUNNER_ARTIFACT_UNSAFE: PUBLIC_CODES.tokenCaptureInvalid,
  RUN_PLAN_INVALID: PUBLIC_CODES.tokenCaptureInvalid,
  PROJECT_ADAPTER_OUTPUT_INVALID: PUBLIC_CODES.tokenCaptureInvalid,
  PROJECT_ADAPTER_TIMEOUT: PUBLIC_CODES.tokenSyncFailed,
  RUN_PLAN_FAILED: PUBLIC_CODES.tokenSyncFailed
});

var DOMAIN_COMPONENT_CODES = Object.freeze({
  COMPONENT_DESIGN_ACCESS_DEGRADED: PUBLIC_CODES.componentAccessDegraded,
  COMPONENT_DESIGN_CAPTURE_INCOMPLETE: PUBLIC_CODES.componentCaptureIncomplete,
  COMPONENT_DESIGN_ABSENCE_UNPROVEN: PUBLIC_CODES.componentCaptureIncomplete,
  COMPONENT_VARIANT_CAPTURE_INCOMPLETE: PUBLIC_CODES.componentCaptureIncomplete,
  COMPONENT_DESIGN_SCOPE_CHANGED: PUBLIC_CODES.componentCaptureInconsistent,
  COMPONENT_DESIGN_CAPTURE_INVALID: PUBLIC_CODES.componentCaptureInvalid,
  COMPONENT_GENERATION_RESYNC_REQUIRED: PUBLIC_CODES.componentGenerationResyncRequired,
  COMPONENT_GENERATION_ARTIFACT_INVALID: PUBLIC_CODES.componentGenerationResyncRequired,
  RUN_PLAN_INVALID: PUBLIC_CODES.componentCaptureInvalid,
  PROJECT_ADAPTER_OUTPUT_INVALID: PUBLIC_CODES.componentCaptureInvalid,
  PROJECT_ADAPTER_TIMEOUT: PUBLIC_CODES.componentSyncFailed,
  RUN_PLAN_FAILED: PUBLIC_CODES.componentSyncFailed
});

var GENERAL_CODES = Object.freeze({
  'sync-history-unavailable': PUBLIC_CODES.syncHistoryUnavailable,
  'sync-history-runtime-failed': PUBLIC_CODES.syncHistoryUnavailable,
  'sync-history-index-invalid': PUBLIC_CODES.syncHistoryUnavailable,
  'sync-history-record-invalid': PUBLIC_CODES.syncHistoryUnavailable,
  'figma-sync-recovering': PUBLIC_CODES.syncRecoveryRequired,
  'figma-sync-recovery-failed': PUBLIC_CODES.syncRecoveryRequired,
  'sync-action-timeout': PUBLIC_CODES.syncTimeout,
  'sync-session-start-refused': PUBLIC_CODES.syncSessionUnavailable,
  'invalid-session-key': PUBLIC_CODES.syncSessionUnavailable,
  'writer-termination-pending': PUBLIC_CODES.syncSessionUnavailable,
  'conversation-only-contract-invalid': PUBLIC_CODES.syncSessionUnavailable,
  'runtime-only-contract-invalid': PUBLIC_CODES.syncSessionUnavailable,
  'session-runtime-unsafe': PUBLIC_CODES.syncSessionUnavailable,
  'initial-prompt-refused': PUBLIC_CODES.syncSessionUnavailable,
  'sync-cancelled': PUBLIC_CODES.syncCancelled,
  'sync-no-valid-groups': PUBLIC_CODES.syncNoValidGroups,
  'job-interrupted': PUBLIC_CODES.jobInterrupted,
  'TOKEN_HEALTH_RECOVERY_REQUIRED': PUBLIC_CODES.tokenHealthRecoveryRequired,
  'component-token-publication-failed': PUBLIC_CODES.syncPublicationFailed,
  'component-token-atomic-domain-set-incomplete': PUBLIC_CODES.syncPublicationFailed,
  'generation-publication-failed': PUBLIC_CODES.syncPublicationFailed
});

var COMPARISON_CODES = Object.freeze({
  PROJECT_ADAPTERS_UNCONFIGURED: PUBLIC_CODES.projectAdaptersUnconfigured,
  PROJECT_ADAPTER_CONFIG_INVALID: PUBLIC_CODES.projectAdapterInvalid,
  PROJECT_ADAPTER_UNKNOWN: PUBLIC_CODES.projectAdapterInvalid,
  PROJECT_ADAPTER_PROTOCOL_UNSUPPORTED: PUBLIC_CODES.projectAdapterInvalid,
  PROJECT_ADAPTER_RUNTIME_UNAVAILABLE: PUBLIC_CODES.projectAdapterInvalid,
  PROJECT_ADAPTER_INPUT_OUTSIDE_ROOT: PUBLIC_CODES.projectAdapterInvalid,
  PROJECT_ADAPTER_OUTPUT_INVALID: PUBLIC_CODES.projectAdapterInvalid,
  ADAPTER_INPUT_SNAPSHOT_LIMIT: PUBLIC_CODES.projectAdapterInvalid,
  ADAPTER_INPUT_SNAPSHOT_UNSAFE: PUBLIC_CODES.projectAdapterInvalid,
  PROJECT_ADAPTER_TIMEOUT: PUBLIC_CODES.projectAdapterTimeout,
  TOKEN_GENERATION_RESYNC_REQUIRED: PUBLIC_CODES.tokenGenerationResyncRequired,
  COMPONENT_GENERATION_RESYNC_REQUIRED: PUBLIC_CODES.componentGenerationResyncRequired
});

function exactCandidate(group, error) {
  if (!error) return '';
  var typed = group === 'tokens' ? error.tokenCode :
    group === 'components' ? error.componentCode : null;
  if (typeof typed === 'string' && typed) return typed;
  if (typeof error.code === 'string' && error.code) return error.code;
  var message = typeof error.message === 'string' ? error.message : '';
  if (Object.prototype.hasOwnProperty.call(GENERAL_CODES, message) ||
      Object.prototype.hasOwnProperty.call(DOMAIN_TOKEN_CODES, message) ||
      Object.prototype.hasOwnProperty.call(DOMAIN_COMPONENT_CODES, message)) return message;
  return '';
}

function classify(group, error) {
  var candidate = exactCandidate(group, error);
  if (Object.prototype.hasOwnProperty.call(PUBLIC_SET, candidate)) return candidate;
  if (Object.prototype.hasOwnProperty.call(GENERAL_CODES, candidate)) return GENERAL_CODES[candidate];
  if (group === 'tokens' && Object.prototype.hasOwnProperty.call(DOMAIN_TOKEN_CODES, candidate)) return DOMAIN_TOKEN_CODES[candidate];
  if (group === 'components' && Object.prototype.hasOwnProperty.call(DOMAIN_COMPONENT_CODES, candidate)) return DOMAIN_COMPONENT_CODES[candidate];
  return group === 'tokens' ? PUBLIC_CODES.tokenSyncFailed :
    group === 'components' ? PUBLIC_CODES.componentSyncFailed : PUBLIC_CODES.unknown;
}

function classifyComparison(domain, code) {
  var exact = typeof code === 'string' ? code : '';
  if (Object.prototype.hasOwnProperty.call(COMPARISON_CODES, exact)) return COMPARISON_CODES[exact];
  return domain === 'tokens' ? PUBLIC_CODES.tokenComparisonFailed :
    domain === 'components' ? PUBLIC_CODES.componentComparisonFailed : PUBLIC_CODES.unknown;
}

function publicCode(value, fallback) {
  if (typeof value === 'string' && Object.prototype.hasOwnProperty.call(PUBLIC_SET, value)) return value;
  return typeof fallback === 'string' && Object.prototype.hasOwnProperty.call(PUBLIC_SET, fallback)
    ? fallback : PUBLIC_CODES.unknown;
}

function tokenHealthCode(publicFailure) {
  if (publicFailure === PUBLIC_CODES.syncCancelled) return 'TOKEN_CAPTURE_CANCELLED';
  if (publicFailure === PUBLIC_CODES.tokenCaptureIncomplete) return 'TOKEN_SOURCE_CAPTURE_INCOMPLETE';
  if (publicFailure === PUBLIC_CODES.tokenGenerationResyncRequired) return 'TOKEN_GENERATION_RESYNC_REQUIRED';
  if (publicFailure === PUBLIC_CODES.tokenHealthRecoveryRequired) return 'TOKEN_HEALTH_RECOVERY_REQUIRED';
  return 'TOKEN_CAPTURE_FAILED';
}

module.exports = {
  PUBLIC_CODES: PUBLIC_CODES,
  classify: classify,
  classifyComparison: classifyComparison,
  publicCode: publicCode,
  tokenHealthCode: tokenHealthCode
};
