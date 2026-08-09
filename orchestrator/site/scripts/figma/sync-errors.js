import { i18n } from '../i18n.js';

export function syncFailureText(errorCode) {
  var key = 'figma.syncFailure.' + String(errorCode || 'unknown');
  var translated = i18n.t(key);
  return translated === key ? i18n.t('figma.syncFailure.unknown') : translated;
}
