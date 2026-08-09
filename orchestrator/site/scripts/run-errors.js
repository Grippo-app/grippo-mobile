import { i18n } from './i18n.js';
import { errorCode } from './data/request-json.js';

function t(key) {
  return i18n && typeof i18n.t === 'function' ? i18n.t(key) : key;
}

export function runErrorMessage(error) {
  var code = errorCode(error);
  var key = 'run.error.' + code;
  var message = t(key);
  if (message !== key) return message;
  var commonKey = 'common.requestError.' + code;
  var common = t(commonKey);
  return common === commonKey ? t('run.error.unknown') : common;
}
