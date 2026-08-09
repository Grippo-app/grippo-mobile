import { i18n } from '../i18n.js';

var LABEL_KEYS = Object.freeze({
  syncPlan: Object.freeze({
    targeted: 'figma.syncPlan.targeted',
    'targeted-reactivation': 'figma.syncPlan.targeted-reactivation',
    unknown: 'figma.syncPlan.unknown'
  }),
  group: Object.freeze({
    tokens: 'figma.group.tokens',
    components: 'figma.group.components',
    surfaces: 'figma.group.surfaces',
    drift: 'figma.group.drift',
    unknown: 'figma.group.unknown'
  }),
  warning: Object.freeze({
    'quota-risk': 'figma.warning.quota-risk',
    unknown: 'figma.warning.unknown'
  }),
  syncState: Object.freeze({
    queued: 'figma.syncState.queued',
    running: 'figma.syncState.running',
    completed: 'figma.syncState.completed',
    failed: 'figma.syncState.failed',
    cancelled: 'figma.syncState.cancelled',
    unknown: 'figma.syncState.unknown'
  }),
  groupState: Object.freeze({
    pending: 'figma.groupState.pending',
    running: 'figma.groupState.running',
    completed: 'figma.groupState.completed',
    failed: 'figma.groupState.failed',
    cancelled: 'figma.groupState.cancelled',
    unknown: 'figma.groupState.unknown'
  }),
  historyResult: Object.freeze({
    queued: 'figma.history.result.queued',
    running: 'figma.history.result.running',
    success: 'figma.history.result.success',
    partial: 'figma.history.result.partial',
    failed: 'figma.history.result.failed',
    cancelled: 'figma.history.result.cancelled',
    interrupted: 'figma.history.result.interrupted',
    unknown: 'figma.history.result.unknown'
  }),
  integrationStatus: Object.freeze({
    ready: 'figma.status.ready',
    'needs-attention': 'figma.status.needs-attention',
    syncing: 'figma.status.syncing',
    unavailable: 'figma.status.unavailable',
    unknown: 'figma.status.unknown'
  }),
  testPhase: Object.freeze({
    'checking-connector': 'figma.phase.checking-connector',
    'verifying-account': 'figma.phase.verifying-account',
    'checking-file-access': 'figma.phase.checking-file-access',
    ready: 'figma.phase.ready',
    unknown: 'figma.phase.unknown'
  }),
  testState: Object.freeze({
    running: 'figma.testState.running',
    completed: 'figma.testState.completed',
    failed: 'figma.testState.failed',
    unknown: 'figma.testState.unknown'
  }),
  fileCandidate: Object.freeze({
    verifying: 'figma.fileCandidate.verifying',
    verified: 'figma.fileCandidate.verified',
    failed: 'figma.fileCandidate.failed',
    unknown: 'figma.fileCandidate.unknown'
  }),
  connectorState: Object.freeze({
    connected: 'figma.connectorState.connected',
    'needs-auth': 'figma.connectorState.needs-auth',
    'local-absent': 'figma.connectorState.local-absent',
    misconfigured: 'figma.connectorState.misconfigured',
    'cli-missing': 'figma.connectorState.cli-missing',
    unknown: 'figma.connectorState.unknown'
  }),
  connectorScope: Object.freeze({
    'project-local': 'figma.connectorScope.project-local',
    missing: 'figma.connectorScope.missing',
    unknown: 'figma.connectorScope.unknown'
  }),
  recoveryState: Object.freeze({
    ready: 'figma.recoveryState.ready',
    recovering: 'figma.recoveryState.recovering',
    failed: 'figma.recoveryState.failed',
    unknown: 'figma.recoveryState.unknown'
  })
});

export function figmaEnumText(type, value, params) {
  var keys = LABEL_KEYS[type];
  if (!keys) return i18n.t('figma.value.unknown');
  var key = typeof value === 'string' && Object.prototype.hasOwnProperty.call(keys, value)
    ? keys[value] : keys.unknown;
  return i18n.t(key, params);
}
