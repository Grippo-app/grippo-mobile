import { i18n } from './i18n.js';

function t(key, params) {
  return (i18n && typeof i18n.t === 'function') ? i18n.t(key, params) : key;
}

// One label per live figma:* session key (server/sessions.js
// FIGMA_SESSION_KEYS + the stem-scoped prefixes below). No dead kinds: a key
// missing here fails closed to localized unknown copy; it never exposes a raw
// runtime key in the session menu or terminal title.
var FIGMA_KIND_KEYS = {
  whoami: 'figma.sessionKind.whoami',
  fileaccess: 'figma.sessionKind.fileaccess',
  'sync-tokens': 'figma.sessionKind.syncTokens',
  'sync-components': 'figma.sessionKind.syncComponents',
  shipdriftsweep: 'figma.sessionKind.shipdriftsweep'
};

export function figmaSessionKind(key) {
  var s = String(key || '');
  if (s.indexOf('figma:screens:') === 0) return t('figma.sessionKind.screens', { stem: s.slice('figma:screens:'.length) });
  if (s.indexOf('figma:rebundle:') === 0) return t('figma.sessionKind.rebundle', { stem: s.slice('figma:rebundle:'.length) });
  if (s.indexOf('figma:') !== 0) return t('figma.sessionKind.unknown');
  var kind = s.slice('figma:'.length);
  return FIGMA_KIND_KEYS[kind] ? t(FIGMA_KIND_KEYS[kind]) : t('figma.sessionKind.unknown');
}
