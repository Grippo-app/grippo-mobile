// design-locale.test.mjs — pins for the design-locale detector (R5-1): a uk-text spec against
// uk+en string resources detects uk confidently; digits-only specs carry no signal (candidates
// 0 — textless designs stay unenforced by construction); a formatter-parameterized
// resource ("Оновлено %1$s тому") still matches its rendered instance; ties and zero-match
// votes are NOT confident (callers fail closed); the explicit declaration (FIGMA_DESIGN_LOCALE /
// designLocale) short-circuits detection and an unsupported declared locale is invalid-config,
// never a silent skip.
import { mkdtempSync, mkdirSync, readFileSync, writeFileSync, rmSync } from 'node:fs'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import assert from 'node:assert/strict'
import {
  RTL_LANGUAGES,
  languageOf,
  normalizeText,
  parseStringsXml,
  resourceMatcher,
  localeValueDirs,
  detectDesignLocale,
  deriveResourceRoots,
  readSupportedLocales,
  resolveDesignLocale,
} from '../scripts/lib/design-locale.mjs'

const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

// --- pure helpers -----------------------------------------------------------

check('languageOf handles plain / BCP / underscore / Android-qualifier tags', () => {
  assert.equal(languageOf('uk'), 'uk')
  assert.equal(languageOf('uk-UA'), 'uk')
  assert.equal(languageOf('uk_UA'), 'uk')
  assert.equal(languageOf('uk-rUA'), 'uk')
  assert.equal(languageOf('b+uk+UA'), 'uk')
  assert.equal(languageOf('EN-rUS'), 'en')
  assert.equal(languageOf(''), null)
  assert.equal(languageOf(null), null)
})

check('normalizeText collapses whitespace, strips a trailing ellipsis, lowercases', () => {
  assert.equal(normalizeText('  Діагностика  '), 'діагностика')
  assert.equal(normalizeText('Some\n long\ttext'), 'some long text')
  assert.equal(normalizeText('Truncated label…'), 'truncated label')
  assert.equal(normalizeText('Truncated label...'), 'truncated label')
})

check('parseStringsXml reads values incl. entities, CDATA and the quoted form', () => {
  const xml = `<?xml version="1.0"?><resources>
    <string name="a">Plain value</string>
    <string name="b">Tom &amp; Jerry &lt;tag&gt;</string>
    <string name="c"><![CDATA[Raw <b>bold</b>]]></string>
    <string name="d">"  spaced  "</string>
    <string name="e">Don\\'t stop</string>
  </resources>`
  const values = parseStringsXml(xml)
  assert.deepEqual(values[0], 'Plain value')
  assert.equal(values[1], 'Tom & Jerry <tag>')
  assert.equal(values[2], 'Raw <b>bold</b>')
  assert.equal(values[3], '  spaced  ')
  assert.match(values[4], /Don't stop/)
})

check('resourceMatcher: plain value → exact; formatter value → anchored wildcard', () => {
  assert.deepEqual(resourceMatcher('Діагностика'), { exact: 'діагностика' })
  const m = resourceMatcher('Оновлено %1$s тому')
  assert.ok(m.pattern instanceof RegExp)
  assert.ok(m.pattern.test(normalizeText('Оновлено 15 хв тому')))
  assert.ok(!m.pattern.test(normalizeText('Updated 15 min ago')))
  assert.equal(resourceMatcher('   '), null)
})

check('resourceMatcher: formatter-only / letterless values do NOT vote (match-anything guard)', () => {
  // '%1$s' compiled to /^[\s\S]+?$/ — one passthrough string in the base locale matched EVERY
  // candidate and flipped the vote to a CONFIDENT wrong language. Literal content between
  // wildcards must satisfy the votable bar (≥ 3 chars, ≥ 1 letter).
  for (const v of ['%1$s', '%d', '%s', '%d%%', '%1$s:', '%1$s — %2$s', '%.1f']) {
    assert.equal(resourceMatcher(v), null, `'${v}' must not produce a matcher`)
  }
  assert.ok(resourceMatcher('Оновлено %1$s тому').pattern instanceof RegExp, 'real literal content still votes')
})

check('parseStringsXml: self-closing <string/> does not swallow the next value; comments are dead', () => {
  const xml = `<resources>
    <string name="empty"/>
    <string name="hello">Привіт світ</string>
    <!-- <string name="old">commented translation</string> -->
    <string name="tail">Кінець</string>
  </resources>`
  const values = parseStringsXml(xml)
  assert.deepEqual(values, ['Привіт світ', 'Кінець'])
})

check('localeValueDirs: base locale owns values/; regions map to -r and b+ forms; never values-night', () => {
  assert.deepEqual(localeValueDirs('en', true), ['values', 'values-en'])
  assert.deepEqual(localeValueDirs('uk', false), ['values-uk'])
  assert.deepEqual(localeValueDirs('pt-BR', false), ['values-pt', 'values-pt-rBR', 'values-b+pt+BR'])
  assert.deepEqual(localeValueDirs('uk-rUA', false), ['values-uk', 'values-uk-rUA', 'values-b+uk+UA'])
})

check('RTL_LANGUAGES carries the enforced qualifier set', () => {
  for (const l of ['ar', 'he', 'fa', 'ur']) assert.ok(RTL_LANGUAGES.has(l))
  assert.ok(!RTL_LANGUAGES.has('uk'))
})

// --- FS fixture: uk + en resources ------------------------------------------

const root = mkdtempSync(join(tmpdir(), 'design-locale-'))
const res = join(root, 'composeResources')
mkdirSync(join(res, 'values'), { recursive: true })
mkdirSync(join(res, 'values-uk'), { recursive: true })
writeFileSync(join(res, 'values', 'strings.xml'), `<resources>
  <string name="diagnostics">Diagnostics</string>
  <string name="terminals">Terminals</string>
  <string name="updated_ago">Updated %1$s ago</string>
</resources>`)
writeFileSync(join(res, 'values-uk', 'strings.xml'), `<resources>
  <string name="diagnostics">Діагностика</string>
  <string name="terminals">Термінали</string>
  <string name="updated_ago">Оновлено %1$s тому</string>
</resources>`)

const spec = (texts) => ({ screen: 'Home', frameSizeDp: { w: 412, h: 915 }, theme: 'dark', elements: texts.map((t, i) => ({ stableId: `e${i}`, name: `t${i}`, bboxDp: { x: 0, y: 0, w: 10, h: 10 }, text: t })) })
const ARGS = { resourceRoots: [res], supportedLocales: ['en', 'uk'] }

try {
  check('uk-text spec → { locale: uk, confident: true } with per-locale scores', () => {
    const r = detectDesignLocale({ specs: [spec(['Діагностика', 'Термінали'])], ...ARGS })
    assert.equal(r.locale, 'uk')
    assert.equal(r.confident, true)
    assert.equal(r.scores.uk, 2)
    assert.equal(r.scores.en, 0)
    assert.equal(r.candidates, 2)
    assert.deepEqual(r.files.sort(), [join(res, 'values', 'strings.xml'), join(res, 'values-uk', 'strings.xml')].sort(), 'consulted strings.xml are reported for the hash net')
  })

  check('a formatter-only passthrough string in the base locale cannot flip a uk vote to en', () => {
    writeFileSync(join(res, 'values', 'strings.xml'), `<resources>
      <string name="raw">%1$s</string>
      <string name="diagnostics">Diagnostics</string>
      <string name="terminals">Terminals</string>
      <string name="updated_ago">Updated %1$s ago</string>
    </resources>`)
    try {
      const r = detectDesignLocale({ specs: [spec(['Діагностика', 'Термінали', 'Оновлено 15 хв тому'])], ...ARGS })
      assert.equal(r.locale, 'uk')
      assert.equal(r.confident, true)
      assert.equal(r.scores.en, 0, 'the %1$s passthrough must not match everything')
    } finally {
      writeFileSync(join(res, 'values', 'strings.xml'), `<resources>
  <string name="diagnostics">Diagnostics</string>
  <string name="terminals">Terminals</string>
  <string name="updated_ago">Updated %1$s ago</string>
</resources>`)
    }
  })

  check('en-text spec → confident en (base values/ dir is read for the first supported locale)', () => {
    const r = detectDesignLocale({ specs: [spec(['Diagnostics', 'Terminals'])], ...ARGS })
    assert.equal(r.locale, 'en')
    assert.equal(r.confident, true)
  })

  check('formatter-parameterized resource matches a rendered instance', () => {
    const r = detectDesignLocale({ specs: [spec(['Оновлено 15 хв тому', 'Діагностика'])], ...ARGS })
    assert.equal(r.locale, 'uk')
    assert.equal(r.confident, true)
    assert.equal(r.scores.uk, 2)
  })

  check('digits/punctuation-only spec → candidates 0, not confident (no signal)', () => {
    const r = detectDesignLocale({ specs: [spec(['128', '99,9 %', '—', 'ok'])], ...ARGS })
    assert.equal(r.candidates, 0)
    assert.equal(r.confident, false)
    assert.equal(r.locale, null)
  })

  check('votable text matching NO locale → not confident, locale null (zero-match vote)', () => {
    const r = detectDesignLocale({ specs: [spec(['Completely invented copy', 'Another stub line'])], ...ARGS })
    assert.equal(r.candidates, 2)
    assert.equal(r.confident, false)
    assert.equal(r.locale, null)
  })

  check('a single matched string is decisive but NOT confident (< 2 votes)', () => {
    const r = detectDesignLocale({ specs: [spec(['Діагностика', 'Invented dynamic value'])], ...ARGS })
    assert.equal(r.locale, 'uk')
    assert.equal(r.confident, false)
  })

  check('tie (same string in both locales) → not confident', () => {
    writeFileSync(join(res, 'values', 'strings2.xml'), '<resources/>') // noise: only strings.xml is read
    const tieRoot = mkdtempSync(join(tmpdir(), 'design-locale-tie-'))
    for (const dir of ['values', 'values-uk']) {
      mkdirSync(join(tieRoot, dir), { recursive: true })
      writeFileSync(join(tieRoot, dir, 'strings.xml'), '<resources><string name="ok">OK button</string><string name="go">Go now</string></resources>')
    }
    const r = detectDesignLocale({ specs: [spec(['OK button', 'Go now'])], resourceRoots: [tieRoot], supportedLocales: ['en', 'uk'] })
    assert.equal(r.confident, false)
    assert.equal(r.locale, null)
    rmSync(tieRoot, { recursive: true, force: true })
  })

  check('resolveDesignLocale: confident detection → language + rtl:false + source detected', () => {
    const r = resolveDesignLocale({ specs: [spec(['Діагностика', 'Термінали'])], ...ARGS })
    assert.deepEqual({ language: r.language, rtl: r.rtl, source: r.source }, { language: 'uk', rtl: false, source: 'detected' })
  })

  check('resolveDesignLocale: undecided vote → reason not-confident (caller fails closed)', () => {
    const r = resolveDesignLocale({ specs: [spec(['Completely invented copy', 'Another stub line'])], ...ARGS })
    assert.equal(r.language, null)
    assert.equal(r.reason, 'not-confident')
  })

  check('resolveDesignLocale: textless spec → reason no-signal (nothing to enforce)', () => {
    const r = resolveDesignLocale({ specs: [spec([])], ...ARGS })
    assert.equal(r.language, null)
    assert.equal(r.reason, 'no-signal')
  })

  check('FIGMA_DESIGN_LOCALE declaration short-circuits an unconfident detection', () => {
    process.env.FIGMA_DESIGN_LOCALE = 'uk'
    try {
      const r = resolveDesignLocale({ specs: [spec(['Completely invented copy'])], ...ARGS })
      assert.deepEqual({ language: r.language, source: r.source }, { language: 'uk', source: 'config' })
    } finally { delete process.env.FIGMA_DESIGN_LOCALE }
  })

  check('declared locale outside supportedLocales → invalid-config (a typo cannot disarm the check)', () => {
    process.env.FIGMA_DESIGN_LOCALE = 'de'
    try {
      const r = resolveDesignLocale({ specs: [spec(['Діагностика', 'Термінали'])], ...ARGS })
      assert.equal(r.language, null)
      assert.equal(r.reason, 'invalid-config')
      assert.equal(r.locale, 'de')
    } finally { delete process.env.FIGMA_DESIGN_LOCALE }
  })

  check('declared RTL locale → rtl: true (the qualifier must carry ldrtl)', () => {
    process.env.FIGMA_DESIGN_LOCALE = 'ar'
    try {
      const r = resolveDesignLocale({ specs: [], resourceRoots: [res], supportedLocales: ['en', 'ar'] })
      assert.deepEqual({ language: r.language, rtl: r.rtl }, { language: 'ar', rtl: true })
    } finally { delete process.env.FIGMA_DESIGN_LOCALE }
  })

  check('FIGMA_SUPPORTED_LOCALES / FIGMA_STRING_RESOURCE_ROOTS env overrides (fixture path)', () => {
    process.env.FIGMA_SUPPORTED_LOCALES = 'en, uk'
    process.env.FIGMA_STRING_RESOURCE_ROOTS = res
    try {
      assert.deepEqual(readSupportedLocales(), ['en', 'uk'])
      assert.deepEqual(deriveResourceRoots(), [res])
    } finally {
      delete process.env.FIGMA_SUPPORTED_LOCALES
      delete process.env.FIGMA_STRING_RESOURCE_ROOTS
    }
  })

  check('deriveResourceRoots walk finds composeResources and src/main/res, skips build trees', () => {
    const repo = mkdtempSync(join(tmpdir(), 'design-locale-repo-'))
    mkdirSync(join(repo, 'featureA', 'src', 'commonMain', 'composeResources', 'values'), { recursive: true })
    mkdirSync(join(repo, 'app', 'src', 'main', 'res', 'values'), { recursive: true })
    mkdirSync(join(repo, 'build', 'src', 'main', 'res'), { recursive: true })          // skipped: build/
    mkdirSync(join(repo, 'featureB', 'src', 'test', 'res'), { recursive: true })       // not src/main/res
    const roots = deriveResourceRoots([repo]).sort()
    assert.deepEqual(roots, [join(repo, 'app', 'src', 'main', 'res'), join(repo, 'featureA', 'src', 'commonMain', 'composeResources')].sort())
    rmSync(repo, { recursive: true, force: true })
  })

  check('missing project config never falls back to an invented en locale list', () => {
    const missingConfigRoot = mkdtempSync(join(tmpdir(), 'design-locale-missing-config-'))
    try {
      assert.throws(() => readSupportedLocales(missingConfigRoot), /configuration is missing/)
    } finally {
      rmSync(missingConfigRoot, { recursive: true, force: true })
    }
  })

  check('malformed supportedLocales never falls back to en', () => {
    const invalidConfigRoot = mkdtempSync(join(tmpdir(), 'design-locale-invalid-config-'))
    try {
      mkdirSync(join(invalidConfigRoot, 'orchestrator'), { recursive: true })
      writeFileSync(join(invalidConfigRoot, 'orchestrator', 'project-config.md'), 'supportedLocales: definitely-not-a-list\n')
      assert.throws(() => readSupportedLocales(invalidConfigRoot), /configuration is missing, invalid/)
    } finally {
      rmSync(invalidConfigRoot, { recursive: true, force: true })
    }
  })

  check('invalid UTF-8 locale resources make locale resolution unavailable instead of changing the vote', () => {
    const file = join(res, 'values', 'strings.xml')
    const original = readFileSync(file)
    try {
      writeFileSync(file, Buffer.from([0xff, 0xfe, 0xfd]))
      const r = resolveDesignLocale({ specs: [spec(['Діагностика', 'Термінали'])], ...ARGS })
      assert.equal(r.language, null)
      assert.equal(r.reason, 'source-unavailable')
    } finally {
      writeFileSync(file, original)
    }
  })
} finally {
  rmSync(root, { recursive: true, force: true })
}

console.log(`\ndesign-locale.test: ${pass} passed, ${fail} failed`)
process.exit(fail ? 1 : 0)
