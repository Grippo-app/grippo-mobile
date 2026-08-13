import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

import { i18n } from '../scripts/i18n.js'
import dictionaries, { dictionaryDomains } from '../scripts/i18n/dictionaries/index.js'
import { mergeLocaleDictionaries } from '../scripts/i18n/dictionaries/merge.js'

const SITE = fileURLToPath(new URL('../', import.meta.url))
const localeDir = join(SITE, 'scripts', 'i18n')
const entrySources = Object.fromEntries(['en', 'ru', 'uk'].map((locale) => [
  locale,
  readFileSync(join(localeDir, locale + '.js'), 'utf8'),
]))

function placeholders(value) {
  return (String(value).match(/\{[^{}]+\}/g) || []).sort()
}

test('Ukrainian is wired through the client toggle and persisted language allowlist', () => {
  const app = readFileSync(join(SITE, 'scripts', 'app.js'), 'utf8')
  const index = readFileSync(join(SITE, 'index.html'), 'utf8')
  const persistence = readFileSync(join(SITE, 'server', 'persistence.js'), 'utf8')

  assert.deepEqual(i18n.SUPPORTED, ['en', 'ru', 'uk'])
  assert.match(app, /import '\.\/i18n\/uk\.js'/)
  assert.match(index, /data-lang-set="uk"[^>]*>UA<\/button>/)
  assert.match(persistence, /UI_LANGS = \['en', 'ru', 'uk'\]/)
  assert.match(entrySources.uk, /i18n\.register\('uk', dictionaries\.uk\)/)
})

test('EN, RU, and UK dictionaries have exact key and placeholder parity', () => {
  const enKeys = Object.keys(dictionaries.en).sort()
  assert.equal(enKeys.length, 3634)
  for (const locale of ['ru', 'uk']) {
    assert.deepEqual(Object.keys(dictionaries[locale]).sort(), enKeys, locale + ' keys')
    for (const key of enKeys) {
      assert.deepEqual(placeholders(dictionaries[locale][key]), placeholders(dictionaries.en[key]), locale + ':' + key)
    }
  }
})

test('API filter defaults lead with their facet name in every locale', () => {
  const facets = [
    'area',
    'method',
    'implementation',
    'auth',
    'changeSeverity',
    'mismatch',
    'hasTask',
    'consumers',
    'severity',
  ]
  for (const locale of ['en', 'ru', 'uk']) {
    for (const facet of facets) {
      const label = dictionaries[locale]['api.filter.' + facet]
      const defaultOption = dictionaries[locale]['api.filter.any.' + facet]
      assert.ok(
        defaultOption.startsWith(label + ':'),
        locale + ':' + facet + ' must remain identifiable when the select text is clipped',
      )
    }
  }
})

test('API bulk-selection controls use complete, action-oriented copy', () => {
  for (const locale of ['en', 'ru', 'uk']) {
    for (const key of [
      'api.selection.selectAll',
      'api.selection.selectingAll',
      'api.selection.selectAllDone',
      'api.selection.actionsAria',
    ]) {
      assert.ok(dictionaries[locale][key].length > 8, locale + ':' + key)
    }
    assert.equal(Object.hasOwn(dictionaries[locale], 'api.selection.limit'), false)
    assert.equal(
      dictionaries[locale]['api.selection.confirmPackage'].includes('{expanded}'),
      false,
      locale + ': package confirmation must not imply automatic expansion',
    )
  }
  assert.equal(dictionaries.en['api.selection.createPackage'], 'Review and create tasks')
  assert.equal(dictionaries.ru['api.selection.createPackage'], 'Проверить и создать задачи')
  assert.equal(dictionaries.uk['api.selection.createPackage'], 'Перевірити та створити завдання')
  assert.equal(
    dictionaries.en['api.selection.scopeHint'],
    'Only the selected API issues are included. The preview creates one task per API group.',
  )
  assert.equal(
    dictionaries.ru['api.selection.scopeHint'],
    'В задачи попадут только выбранные проблемы API. В предпросмотре создаётся одна задача на каждую группу API.',
  )
  assert.equal(
    dictionaries.uk['api.selection.scopeHint'],
    'До завдань потраплять лише вибрані проблеми API. У перегляді створюється одне завдання на кожну групу API.',
  )
})

test('every domain owns the same immutable key set in EN, RU, and UK', () => {
  assert.deepEqual(
    dictionaryDomains.map(([domain]) => domain),
    [
      'core',
      'design',
      'architecture',
      'setup',
      'wizard',
      'reviewer',
      'board',
      'figma-integration',
      'backend-integration',
      'live-status',
      'figma-status',
      'api',
      'app-run',
      'task-details',
    ]
  )
  for (const [domain, translations] of dictionaryDomains) {
    assert.equal(Object.isFrozen(translations), true, `${domain}: locale bundle`)
    const enKeys = Object.keys(translations.en).sort()
    assert.ok(enKeys.length > 0, domain)
    for (const locale of ['en', 'ru', 'uk']) {
      const source = readFileSync(
        join(localeDir, 'dictionaries', domain, locale + '.js'),
        'utf8'
      )
      const sourceKeys = [...source.matchAll(/^\s*'([A-Za-z0-9._-]+)'\s*:/gm)]
        .map((match) => match[1])
      assert.equal(
        new Set(sourceKeys).size,
        sourceKeys.length,
        `${domain}:${locale}: duplicate source key`
      )
      assert.equal(Object.isFrozen(translations[locale]), true, `${domain}:${locale}`)
      const runtimeKeys = Object.keys(translations[locale]).sort()
      assert.deepEqual(sourceKeys.sort(), runtimeKeys, `${domain}:${locale}: source/runtime keys`)
      assert.deepEqual(runtimeKeys, enKeys, `${domain}:${locale}: domain keys`)
    }
  }
})

test('domain merge rejects missing dictionaries, duplicate domains, and duplicate keys', () => {
  const one = Object.freeze({ en: Object.freeze({ 'sample.one': 'one' }) })
  const two = Object.freeze({ en: Object.freeze({ 'sample.one': 'two' }) })
  assert.throws(
    () => mergeLocaleDictionaries('en', [['one', one], ['two', two]]),
    /duplicate en locale key: sample\.one/
  )
  assert.throws(
    () => mergeLocaleDictionaries('en', [['one', one], ['one', one]]),
    /invalid or duplicate locale domain: one/
  )
  assert.throws(
    () => mergeLocaleDictionaries('ru', [['one', one]]),
    /missing ru dictionary for domain one/
  )
  assert.throws(
    () => mergeLocaleDictionaries('en', [['one', { en: {} }]]),
    /empty en dictionary for domain one/
  )
  assert.throws(
    () => mergeLocaleDictionaries('en', [['one', { en: { key: 1 } }]]),
    /invalid en locale entry in domain one: key/
  )
  assert.throws(
    () => mergeLocaleDictionaries('de', [['one', one]]),
    /unsupported locale: de/
  )
})

test('Ukrainian dictionary is complete and free of generation artifacts', () => {
  assert.equal(dictionaries.uk['chrome.lang'], 'Мова')
  assert.equal(dictionaries.uk['nav.setup'], 'Налаштування')
  assert.equal(dictionaries.uk['nav.board'], 'Дошка')
  assert.equal(dictionaries.uk['common.required'], 'Обов’язково.')

  for (const [key, value] of Object.entries(dictionaries.uk)) {
    assert.doesNotMatch(value, /ZXQ(?:TK|SEPARATOR)|[ыэъё]/i, key)
  }
})
