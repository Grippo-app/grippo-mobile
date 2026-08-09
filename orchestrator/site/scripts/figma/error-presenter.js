var KNOWN = {
  'connector-missing': 'connect',
  'auth-required': 'reconnect',
  'connector-conflict': null,
  'connector-unavailable': 'test',
  'connection-test-timeout': 'test',
  'connection-test-start-failed': 'test',
  'connection-test-busy': 'test',
  'integration-failed': null,
  'account-stale': 'test',
  'file-missing': 'selectFile',
  'file-invalid': 'selectFile',
  'access-unverified': 'test',
  'file-not-found': 'selectAnotherFile',
  'access-denied': 'changeAccount',
  'quota-risk': 'changeAccount',
  'sync-recovery-failed': null
};

export function presentFigmaError(reasonCode) {
  var code = Object.prototype.hasOwnProperty.call(KNOWN, reasonCode) ? reasonCode : 'unknown';
  var action = KNOWN[code] || null;
  return {
    code: code,
    titleKey: 'figma.error.' + code + '.title',
    bodyKey: 'figma.error.' + code + '.body',
    actionKey: action ? 'figma.action.' + action : null,
    action: action
  };
}
