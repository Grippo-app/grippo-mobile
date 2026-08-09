import assert from 'node:assert/strict';

import dictionaries from '../scripts/i18n/dictionaries/index.js';

const localeNames = Object.freeze(['en', 'ru', 'uk']);

export function dictionaryFor(locale) {
  assert(localeNames.includes(locale), `unsupported test locale: ${String(locale)}`);
  return dictionaries[locale];
}

export function localeText(locale) {
  return Object.values(dictionaryFor(locale)).join('\n');
}
