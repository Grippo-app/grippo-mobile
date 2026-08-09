import { i18n } from './i18n.js';
import { errorCode } from './data/request-json.js';

function t(key) {
  return i18n && typeof i18n.t === 'function' ? i18n.t(key) : key;
}

export function reviewerErrorMessage(error, context) {
  var code = errorCode(error);
  var key = 'codex.requestError.' + code;
  var translated = t(key);
  if (translated !== key) return translated;
  var commonKey = 'common.requestError.' + code;
  var common = t(commonKey);
  if (common !== commonKey) return common;
  if (context === 'activity') return t('codex.error.activity');
  if (context === 'settings') return t('codex.settings.failed');
  return t('codex.error.status');
}
