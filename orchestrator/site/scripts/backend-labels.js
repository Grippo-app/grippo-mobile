import { i18n } from './i18n.js';

var STATES = Object.freeze({
  ready: 1,
  'needs-setup': 1,
  'needs-test': 1,
  'attention-required': 1,
  'source-unavailable': 1,
  'changes-available': 1,
  refreshing: 1
});
var AUTH_STATES = Object.freeze({
  configured: 1,
  'not-required': 1,
  missing: 1,
  invalid: 1,
  unknown: 1
});

export function backendState(value) {
  return typeof value === 'string' && Object.prototype.hasOwnProperty.call(STATES, value)
    ? value : 'unknown';
}

export function backendStateText(value) {
  return i18n.t('backend.state.' + backendState(value));
}

export function backendPillText(value) {
  return i18n.t('backend.pill.' + backendState(value));
}

export function backendAuthText(value) {
  var normalized = typeof value === 'string' && Object.prototype.hasOwnProperty.call(AUTH_STATES, value)
    ? value : 'unknown';
  return i18n.t('backend.auth.' + normalized);
}
