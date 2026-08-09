// token-kotlin-adapter.test.mjs — pins for the built-in kotlin-compose token
// extractor (TOK-KT-*): primitive containers, nested contract slots filled by
// per-mode implementations, contract-anchored identity, the value-resolver
// whitelist (Color hex, dp/sp, FontWeight, TextStyle, .copy(alpha), alias
// chains) and its honest unsupported cases, parse-failure isolation,
// determinism, and full schema + semantic-contract conformance.
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { extractTokens, KOTLIN_COMPOSE_EXTRACTOR_VERSION } from '../adapters/kotlin-compose/tokens.mjs'
import { extractTokens as extractJsonTokens } from '../adapters/json-tokens/tokens.mjs'
import { adapterImplementation } from '../runtime/adapter-registry.mjs'
import { projectInventorySemanticError, projectInventorySemanticHash } from '../tokens/project-inventory-contract.mjs'
import { sha256Text } from '../scripts/report-utils.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const FIXTURES = join(HERE, '..', 'adapters', 'kotlin-compose', 'fixtures')
const { default: Ajv } = await import('ajv')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const ADAPTER_ID = 'compose-design-system'
const id = (fq) => `${ADAPTER_ID}:${fq}`

const fixtureFile = (fixtureRelPath, asPath) => {
  const text = readFileSync(join(FIXTURES, fixtureRelPath), 'utf8')
  return { path: asPath, text, hash: sha256Text(text) }
}

const tokenById = (extraction, tokenId) => extraction.tokens.find((token) => token.projectTokenId === tokenId)

const extract = (files, tokensConfig) => extractTokens({ files, tokensConfig, adapterId: ADAPTER_ID })

const flatConfig = () => ({
  roots: ['app/src'], include: ['**/*.kt'], exclude: [],
  modes: ['shared'],
  authorities: { color: { contracts: [], implementations: [], primitiveContainers: ['com.flat.design.AppColorFlat'] } }
})
const flatFiles = () => [fixtureFile('flat/AppColorFlat.kt', 'app/src/AppColorFlat.kt')]

const NESTED_DARK_PATH = 'design-system/src/commonMain/kotlin/DarkAppColors.kt'
const nestedFiles = (darkPath = NESTED_DARK_PATH) => [
  fixtureFile('nested/AppPalette.kt', 'design-system/src/commonMain/kotlin/AppPalette.kt'),
  fixtureFile('nested/AppColor.kt', 'design-system/src/commonMain/kotlin/AppColor.kt'),
  fixtureFile('nested/LightAppColors.kt', 'design-system/src/commonMain/kotlin/LightAppColors.kt'),
  fixtureFile('nested/DarkAppColors.kt', darkPath),
  fixtureFile('nested/AppDimens.kt', 'design-system/src/commonMain/kotlin/AppDimens.kt'),
  fixtureFile('nested/AppTypography.kt', 'design-system/src/commonMain/kotlin/AppTypography.kt')
]
const nestedConfig = () => ({
  roots: ['design-system/src'], include: ['**/*.kt'], exclude: ['**/build/**'],
  modes: ['light', 'dark', 'shared'],
  authorities: {
    color: {
      contracts: ['com.example.design.AppColor'],
      implementations: [
        { mode: 'light', symbols: ['com.example.design.LightAppColors'] },
        { mode: 'dark', symbols: ['com.example.design.DarkAppColors'] }
      ],
      primitiveContainers: ['com.example.design.AppPalette']
    },
    dimension: { contracts: [], implementations: [], primitiveContainers: ['com.example.design.AppDimens'] },
    typography: { contracts: [], implementations: [], primitiveContainers: ['com.example.design.AppType'] }
  }
})

check('TOK-KT: registry serves this extractor under kind kotlin-compose', () => {
  const implementation = adapterImplementation('kotlin-compose')
  assert.equal(implementation.extractorVersion, KOTLIN_COMPOSE_EXTRACTOR_VERSION)
  assert.equal(implementation.extractTokens, extractTokens)
  assert.equal(KOTLIN_COMPOSE_EXTRACTOR_VERSION, 'kotlin-compose-tokens-v1')
})

check('TOK-KT: flat primitive container extracts every val as a shared-mode primitive', () => {
  const extraction = extract(flatFiles(), flatConfig())
  assert.deepEqual(extraction.parseFailures, [])
  assert.deepEqual(extraction.tokens.map((token) => token.projectTokenId), [
    id('com.flat.design.AppColorFlat.multiline'),
    id('com.flat.design.AppColorFlat.onPrimary'),
    id('com.flat.design.AppColorFlat.primary'),
    id('com.flat.design.AppColorFlat.surfaceTint'),
    id('com.flat.design.AppColorFlat.underscored')
  ])
  for (const token of extraction.tokens) {
    assert.equal(token.layer, 'primitive')
    assert.deepEqual(Object.keys(token.modes), ['shared'])
  }
  const primary = tokenById(extraction, id('com.flat.design.AppColorFlat.primary'))
  assert.deepEqual(primary.semanticPath, ['AppColorFlat', 'primary'])
  assert.equal(primary.displayName, 'AppColorFlat.primary')
  assert.deepEqual(primary.modes.shared.resolved, { kind: 'color', value: '#6200EEFF', colorSpace: 'srgb' })
})

check('TOK-KT: Color(0xAARRGGBB) reorders to #RRGGBBAA (alpha literal preserved)', () => {
  const extraction = extract(flatFiles(), flatConfig())
  const tint = tokenById(extraction, id('com.flat.design.AppColorFlat.surfaceTint'))
  assert.deepEqual(tint.modes.shared.resolved, { kind: 'color', value: '#11223366', colorSpace: 'srgb' })
})

check('TOK-KT: Kotlin numeric underscores are accepted in hex color literals', () => {
  const extraction = extract(flatFiles(), flatConfig())
  const underscored = tokenById(extraction, id('com.flat.design.AppColorFlat.underscored'))
  assert.deepEqual(underscored.modes.shared.resolved, { kind: 'color', value: '#654321FF', colorSpace: 'srgb' })
})

check('TOK-KT: source carries 1-based line, 0-based column, FQ symbol, and the file hash', () => {
  const files = flatFiles()
  const extraction = extract(files, flatConfig())
  const primary = tokenById(extraction, id('com.flat.design.AppColorFlat.primary'))
  assert.deepEqual(primary.source, {
    path: 'app/src/AppColorFlat.kt', line: 6, column: 4,
    symbol: 'com.flat.design.AppColorFlat.primary', fileHash: files[0].hash
  })
})

check('TOK-KT: a multi-line declaration keeps its line and collapses raw to one line', () => {
  const extraction = extract(flatFiles(), flatConfig())
  const multiline = tokenById(extraction, id('com.flat.design.AppColorFlat.multiline'))
  assert.equal(multiline.source.line, 10)
  assert.equal(multiline.modes.shared.raw.expression, 'Color(0xFF00FF00)')
  assert.deepEqual(multiline.modes.shared.resolved, { kind: 'color', value: '#00FF00FF', colorSpace: 'srgb' })
})

check('TOK-KT: nested project emits the full expected identity set', () => {
  const extraction = extract(nestedFiles(), nestedConfig())
  assert.deepEqual(extraction.parseFailures, [])
  assert.deepEqual(extraction.tokens.map((token) => token.projectTokenId).sort(), [
    'com.example.design.AppColor.background',
    'com.example.design.AppColor.error.container',
    'com.example.design.AppColor.error.content',
    'com.example.design.AppColor.outline',
    'com.example.design.AppColor.primary',
    'com.example.design.AppColor.warning.content',
    'com.example.design.AppDimens.doubled',
    'com.example.design.AppDimens.spaceLg',
    'com.example.design.AppDimens.spaceMd',
    'com.example.design.AppDimens.textMd',
    'com.example.design.AppPalette.Brand.accent',
    'com.example.design.AppPalette.error400',
    'com.example.design.AppPalette.error600',
    'com.example.design.AppPalette.neutral0',
    'com.example.design.AppPalette.neutral900',
    'com.example.design.AppPalette.overlayScrim',
    'com.example.design.AppType.body',
    'com.example.design.AppType.broken',
    'com.example.design.AppType.display',
    'com.example.design.DarkAppColors.darkOnly',
    'com.example.design.LightAppColors.extraGlow'
  ].map(id).sort())
})

check('TOK-KT: contract slots get both modes with alias edges into the palette', () => {
  const extraction = extract(nestedFiles(), nestedConfig())
  const primary = tokenById(extraction, id('com.example.design.AppColor.primary'))
  assert.equal(primary.layer, 'semantic-implementation')
  assert.equal(primary.kind, 'color')
  assert.equal(primary.modes.light.raw.expression, 'AppPalette.error400')
  assert.deepEqual(primary.modes.light.resolved, { kind: 'color', value: '#EF5350FF', colorSpace: 'srgb' })
  assert.deepEqual(primary.modes.dark.resolved, { kind: 'color', value: '#E53935FF', colorSpace: 'srgb' })
  assert.deepEqual(primary.edges, [
    { kind: 'alias', targetProjectTokenId: id('com.example.design.AppPalette.error400'), mode: 'light' },
    { kind: 'alias', targetProjectTokenId: id('com.example.design.AppPalette.error600'), mode: 'dark' }
  ])
})

check('TOK-KT: slot identity and source anchor on the contract declaration, not the implementation', () => {
  const files = nestedFiles()
  const extraction = extract(files, nestedConfig())
  const content = tokenById(extraction, id('com.example.design.AppColor.error.content'))
  assert.ok(content, 'contract-FQ identity expected for the nested group slot')
  assert.deepEqual(content.semanticPath, ['AppColor', 'error', 'content'])
  assert.equal(content.displayName, 'AppColor.error.content')
  assert.equal(content.source.path, 'design-system/src/commonMain/kotlin/AppColor.kt')
  assert.equal(content.source.symbol, 'com.example.design.AppColor.error.content')
  assert.equal(content.source.fileHash, files[1].hash)
})

check('TOK-KT: a slot missing in one theme keeps that mode as unsupported does-not-override', () => {
  const extraction = extract(nestedFiles(), nestedConfig())
  const container = tokenById(extraction, id('com.example.design.AppColor.error.container'))
  assert.equal(container.layer, 'semantic-implementation')
  assert.deepEqual(container.modes.light.resolved, { kind: 'color', value: '#FFEBEEFF', colorSpace: 'srgb' })
  assert.equal(container.modes.dark.raw.expression, '<no override>')
  assert.match(container.modes.dark.unsupported.reason, /does not override/)
  assert.match(container.modes.dark.unsupported.reason, /com\.example\.design\.DarkAppColors/)
})

check('TOK-KT: a slot no implementation provides stays layer semantic-contract with every mode unsupported', () => {
  const extraction = extract(nestedFiles(), nestedConfig())
  const outline = tokenById(extraction, id('com.example.design.AppColor.outline'))
  assert.equal(outline.layer, 'semantic-contract')
  assert.deepEqual(Object.keys(outline.modes).sort(), ['dark', 'light'])
  assert.match(outline.modes.light.unsupported.reason, /does not override/)
  assert.match(outline.modes.dark.unsupported.reason, /does not override/)
  assert.deepEqual(outline.edges, [])
})

check('TOK-KT: duplicate leaf names in different groups stay distinct tokens', () => {
  const extraction = extract(nestedFiles(), nestedConfig())
  const errorContent = tokenById(extraction, id('com.example.design.AppColor.error.content'))
  const warningContent = tokenById(extraction, id('com.example.design.AppColor.warning.content'))
  assert.ok(errorContent && warningContent)
  assert.notDeepEqual(errorContent.semanticPath, warningContent.semanticPath)
  assert.deepEqual(warningContent.modes.light.resolved, { kind: 'color', value: '#FFA000FF', colorSpace: 'srgb' })
  assert.match(warningContent.modes.dark.unsupported.reason, /does not override/)
})

check('TOK-KT: an override without a contract slot becomes its own flagged token', () => {
  const extraction = extract(nestedFiles(), nestedConfig())
  const extra = tokenById(extraction, id('com.example.design.LightAppColors.extraGlow'))
  assert.equal(extra.layer, 'semantic-implementation')
  assert.deepEqual(extra.semanticPath, ['LightAppColors', 'extraGlow'])
  assert.deepEqual(Object.keys(extra.modes), ['light'])
  assert.deepEqual(extra.limitations, ['implementation-slot-without-contract'])
  assert.equal(extra.source.symbol, 'com.example.design.LightAppColors.extraGlow')
})

check('TOK-KT: moving an implementation file changes only impl-only sources, never identity', () => {
  const before = extract(nestedFiles(), nestedConfig())
  const after = extract(nestedFiles('moved/theme/DarkAppColors.kt'), nestedConfig())
  assert.deepEqual(
    after.tokens.map((token) => token.projectTokenId),
    before.tokens.map((token) => token.projectTokenId)
  )
  for (const token of before.tokens) {
    const moved = tokenById(after, token.projectTokenId)
    if (token.projectTokenId === id('com.example.design.DarkAppColors.darkOnly')) {
      assert.equal(moved.source.path, 'moved/theme/DarkAppColors.kt')
      assert.deepEqual({ ...moved.source, path: token.source.path }, token.source)
      continue
    }
    assert.deepEqual(moved, token)
  }
})

check('TOK-KT: alias chains resolve through intermediates with the edge on the direct hop', () => {
  const config = {
    roots: ['app/src'], include: ['**/*.kt'], exclude: [], modes: ['shared'],
    authorities: { color: { contracts: [], implementations: [], primitiveContainers: ['com.cycle.design.CyclePalette'] } }
  }
  const extraction = extract([fixtureFile('cycle/CyclePalette.kt', 'app/src/CyclePalette.kt')], config)
  const top = tokenById(extraction, id('com.cycle.design.CyclePalette.chainTop'))
  assert.deepEqual(top.modes.shared.resolved, { kind: 'color', value: '#123456FF', colorSpace: 'srgb' })
  assert.deepEqual(top.edges, [
    { kind: 'alias', targetProjectTokenId: id('com.cycle.design.CyclePalette.chainMid') }
  ])
})

check('TOK-KT: an alias cycle is unsupported, never a guessed or looping value', () => {
  const config = {
    roots: ['app/src'], include: ['**/*.kt'], exclude: [], modes: ['shared'],
    authorities: { color: { contracts: [], implementations: [], primitiveContainers: ['com.cycle.design.CyclePalette'] } }
  }
  const extraction = extract([fixtureFile('cycle/CyclePalette.kt', 'app/src/CyclePalette.kt')], config)
  for (const name of ['a', 'b']) {
    const token = tokenById(extraction, id(`com.cycle.design.CyclePalette.${name}`))
    assert.equal(token.modes.shared.resolved, undefined)
    assert.match(token.modes.shared.unsupported.reason, /alias cycle/)
    assert.deepEqual(token.edges, [])
  }
})

check('TOK-KT: an ambiguous suffix reference is unsupported, never first-match', () => {
  const config = {
    roots: ['app/src'], include: ['**/*.kt'], exclude: [], modes: ['shared'],
    authorities: {
      color: {
        contracts: [], implementations: [],
        primitiveContainers: ['com.amb.one.AppPalette', 'com.amb.two.AppPalette', 'com.amb.app.Consumer']
      }
    }
  }
  const extraction = extract([
    fixtureFile('ambiguous/OnePalette.kt', 'app/src/one/AppPalette.kt'),
    fixtureFile('ambiguous/TwoPalette.kt', 'app/src/two/AppPalette.kt'),
    fixtureFile('ambiguous/Consumer.kt', 'app/src/Consumer.kt')
  ], config)
  const pick = tokenById(extraction, id('com.amb.app.Consumer.pick'))
  assert.equal(pick.modes.shared.resolved, undefined)
  assert.match(pick.modes.shared.unsupported.reason, /ambiguous reference AppPalette\.brand/)
  assert.deepEqual(pick.edges, [])
})

check('TOK-KT: .copy(alpha = 0.4f) on a palette ref rewrites alpha to 0x66 with a transform edge', () => {
  const extraction = extract(nestedFiles(), nestedConfig())
  const scrim = tokenById(extraction, id('com.example.design.AppPalette.overlayScrim'))
  assert.equal(scrim.modes.shared.raw.expression, 'neutral900.copy(alpha = 0.4f)')
  assert.deepEqual(scrim.modes.shared.resolved, { kind: 'color', value: '#11111166', colorSpace: 'srgb' })
  assert.deepEqual(scrim.edges, [{
    kind: 'transform',
    targetProjectTokenId: id('com.example.design.AppPalette.neutral900'),
    detail: 'copy(alpha=0.4)'
  }])
})

check('TOK-KT: dp and sp literals resolve as dimensions under a dimension authority', () => {
  const extraction = extract(nestedFiles(), nestedConfig())
  assert.deepEqual(
    tokenById(extraction, id('com.example.design.AppDimens.spaceMd')).modes.shared.resolved,
    { kind: 'dimension', value: 16, unit: 'dp' }
  )
  assert.deepEqual(
    tokenById(extraction, id('com.example.design.AppDimens.spaceLg')).modes.shared.resolved,
    { kind: 'dimension', value: 24.5, unit: 'dp' }
  )
  assert.deepEqual(
    tokenById(extraction, id('com.example.design.AppDimens.textMd')).modes.shared.resolved,
    { kind: 'dimension', value: 20, unit: 'sp' }
  )
})

check('TOK-KT: dimension arithmetic stays unsupported', () => {
  const extraction = extract(nestedFiles(), nestedConfig())
  const doubled = tokenById(extraction, id('com.example.design.AppDimens.doubled'))
  assert.equal(doubled.modes.shared.resolved, undefined)
  assert.match(doubled.modes.shared.unsupported.reason, /arithmetic/)
})

check('TOK-KT: TextStyle with supported named args resolves to a full typography value', () => {
  const extraction = extract(nestedFiles(), nestedConfig())
  const body = tokenById(extraction, id('com.example.design.AppType.body'))
  assert.deepEqual(body.modes.shared.resolved, {
    kind: 'typography',
    fontSize: { kind: 'dimension', value: 16, unit: 'sp' },
    fontWeight: 500,
    lineHeight: { kind: 'dimension', value: 24, unit: 'sp' },
    letterSpacing: { kind: 'dimension', value: 0.5, unit: 'sp' }
  })
  assert.deepEqual(body.limitations, [])
  assert.equal(body.modes.shared.raw.expression.includes('\n'), false)
})

check('TOK-KT: a fontFamily arg is omitted with a typography-field limitation; W-weights map', () => {
  const extraction = extract(nestedFiles(), nestedConfig())
  const display = tokenById(extraction, id('com.example.design.AppType.display'))
  assert.deepEqual(display.modes.shared.resolved, {
    kind: 'typography',
    fontSize: { kind: 'dimension', value: 32, unit: 'sp' },
    fontWeight: 700
  })
  assert.deepEqual(display.limitations, ['typography-field-unsupported:fontFamily'])
})

check('TOK-KT: a positional TextStyle argument makes the whole value unsupported', () => {
  const extraction = extract(nestedFiles(), nestedConfig())
  const broken = tokenById(extraction, id('com.example.design.AppType.broken'))
  assert.equal(broken.modes.shared.resolved, undefined)
  assert.match(broken.modes.shared.unsupported.reason, /positional/)
})

check('TOK-KT: non-whitelisted expressions each stay unsupported with a specific reason', () => {
  const config = {
    roots: ['app/src'], include: ['**/*.kt'], exclude: [], modes: ['shared'],
    authorities: { color: { contracts: [], implementations: [], primitiveContainers: ['com.unsupported.design.WeirdColors'] } }
  }
  const extraction = extract([fixtureFile('unsupported/WeirdColors.kt', 'app/src/WeirdColors.kt')], config)
  const reasonOf = (name) => {
    const token = tokenById(extraction, id(`com.unsupported.design.WeirdColors.${name}`))
    assert.equal(token.modes.shared.resolved, undefined, `${name} must not resolve`)
    return token.modes.shared.unsupported.reason
  }
  assert.match(reasonOf('conditional'), /conditional expression/)
  assert.match(reasonOf('computed'), /function call computeColor/)
  assert.match(reasonOf('named'), /named Compose color Color\.Red/)
  assert.match(reasonOf('lazyOne'), /delegate/)
  assert.match(reasonOf('viaGetter'), /getter/)
  assert.match(reasonOf('math'), /arithmetic/)
})

check('TOK-KT: a resolved value of the wrong kind is rejected by the owning authority', () => {
  const config = {
    roots: ['app/src'], include: ['**/*.kt'], exclude: [], modes: ['shared'],
    authorities: { color: { contracts: [], implementations: [], primitiveContainers: ['com.unsupported.design.WeirdColors'] } }
  }
  const extraction = extract([fixtureFile('unsupported/WeirdColors.kt', 'app/src/WeirdColors.kt')], config)
  const token = tokenById(extraction, id('com.unsupported.design.WeirdColors.dimensionish'))
  assert.equal(token.modes.shared.resolved, undefined)
  assert.equal(token.modes.shared.unsupported.reason, 'value kind dimension under a color authority')
})

check('TOK-KT: a parse-error file lands in parseFailures without poisoning other files', () => {
  const config = {
    roots: ['app/src'], include: ['**/*.kt'], exclude: [], modes: ['shared'],
    authorities: { color: { contracts: [], implementations: [], primitiveContainers: ['com.broken.design.GoodPalette'] } }
  }
  const extraction = extract([
    fixtureFile('broken/Broken.kt', 'app/src/Broken.kt'),
    fixtureFile('broken/Good.kt', 'app/src/Good.kt')
  ], config)
  assert.equal(extraction.parseFailures.length, 1)
  assert.equal(extraction.parseFailures[0].path, 'app/src/Broken.kt')
  assert.match(extraction.parseFailures[0].reason, /parse error/)
  assert.ok(extraction.parseFailures[0].reason.length <= 300)
  const fine = tokenById(extraction, id('com.broken.design.GoodPalette.fine'))
  assert.deepEqual(fine.modes.shared.resolved, { kind: 'color', value: '#224466FF', colorSpace: 'srgb' })
})

check('TOK-KT: a primitive container without a declared shared mode is skipped honestly', () => {
  const config = nestedConfig()
  config.modes = ['light', 'dark']
  const extraction = extract(nestedFiles(), config)
  assert.equal(extraction.tokens.some((token) => token.projectTokenId.includes('AppPalette')), false)
  assert.ok(extraction.limitations.some((item) =>
    item.startsWith('primitive-container-requires-shared-mode:com.example.design.AppPalette')))
})

check('TOK-KT: a contract with an empty implementations list is skipped with a limitation', () => {
  const config = nestedConfig()
  config.authorities.color.implementations = []
  const extraction = extract(nestedFiles(), config)
  assert.equal(extraction.tokens.some((token) => token.projectTokenId.includes('AppColor.')), false)
  assert.ok(extraction.limitations.includes('contract-without-implementations:com.example.design.AppColor'))
})

check('TOK-KT: string and boolean authorities resolve only exact Kotlin literals', () => {
  const text = [
    'package com.example.design',
    '',
    'object AppStrings {',
    '  val title = "Welcome"',
    '}',
    '',
    'object AppFlags {',
    '  val enabled = true',
    '}'
  ].join('\n')
  const extraction = extract([{
    path: 'design-system/src/AppPrimitives.kt',
    text,
    hash: sha256Text(text)
  }], {
    roots: ['design-system/src'],
    include: ['**/*.kt'],
    exclude: [],
    modes: ['shared'],
    authorities: {
      string: { contracts: [], implementations: [], primitiveContainers: ['com.example.design.AppStrings'] },
      boolean: { contracts: [], implementations: [], primitiveContainers: ['com.example.design.AppFlags'] }
    }
  })
  assert.deepEqual(extraction.parseFailures, [])
  assert.deepEqual(
    tokenById(extraction, id('com.example.design.AppStrings.title')).modes.shared.resolved,
    { kind: 'string', value: 'Welcome' }
  )
  assert.deepEqual(
    tokenById(extraction, id('com.example.design.AppFlags.enabled')).modes.shared.resolved,
    { kind: 'boolean', value: true }
  )
})

check('TOK-JSON: string, boolean, and project-only typography values remain representable', () => {
  const document = {
    AppStrings: { title: { value: 'Welcome' } },
    AppFlags: { enabled: { value: true } },
    AppType: {
      body: {
        value: { fontFamily: 'Inter', fontSize: 16, fontWeight: 400, lineHeight: 24 }
      }
    }
  }
  const text = JSON.stringify(document)
  const extraction = extractJsonTokens({
    files: [{ path: 'design/tokens/primitives.json', text, hash: sha256Text(text) }],
    adapterId: 'json-design-system',
    tokensConfig: {
      modes: ['shared'],
      authorities: {
        string: { contracts: ['AppStrings'] },
        boolean: { contracts: ['AppFlags'] },
        typography: { contracts: ['AppType'] }
      }
    }
  })
  assert.deepEqual(extraction.parseFailures, [])
  const byId = new Map(extraction.tokens.map((token) => [token.projectTokenId, token]))
  assert.deepEqual(byId.get('json-design-system:AppStrings.title').modes.shared.resolved,
    { kind: 'string', value: 'Welcome' })
  assert.deepEqual(byId.get('json-design-system:AppFlags.enabled').modes.shared.resolved,
    { kind: 'boolean', value: true })
  assert.deepEqual(byId.get('json-design-system:AppType.body').modes.shared.resolved, {
    kind: 'typography',
    fontFamily: ['Inter'],
    fontSize: { kind: 'dimension', value: 16, unit: 'sp' },
    fontWeight: 400,
    lineHeight: { kind: 'dimension', value: 24, unit: 'sp' }
  })
})

check('TOK-KT-DETERMINISM: reversed input file order serializes byte-identically', () => {
  const forward = extract(nestedFiles(), nestedConfig())
  const reversed = extract([...nestedFiles()].reverse(), nestedConfig())
  assert.equal(JSON.stringify(forward.tokens), JSON.stringify(reversed.tokens))
  assert.equal(JSON.stringify(forward.parseFailures), JSON.stringify(reversed.parseFailures))
  assert.equal(JSON.stringify(forward.limitations), JSON.stringify(reversed.limitations))
})

check('TOK-KT-CONTRACT: the nested extraction passes the inventory schema and semantic contract', () => {
  const schema = JSON.parse(readFileSync(join(HERE, '..', 'schemas', 'project-token-inventory.schema.json'), 'utf8'))
  const validate = new Ajv({ allErrors: true, strict: false }).compile(schema)
  const config = nestedConfig()
  const files = nestedFiles()
  const extraction = extract(files, config)
  const failedPaths = new Set(extraction.parseFailures.map((failure) => failure.path))
  const limitsHit = extraction.limitations.filter((item) => item.startsWith('limit:'))
  let unsupportedModes = 0
  let aliasEdges = 0
  for (const token of extraction.tokens) {
    for (const entry of Object.values(token.modes)) if (entry.unsupported !== undefined) unsupportedModes++
    for (const edge of token.edges) if (edge.kind === 'alias') aliasEdges++
  }
  const witness = {
    rootsConfigured: config.roots.length,
    rootsResolved: config.roots.length,
    rootsMissing: [],
    filesMatched: files.length,
    filesParsed: files.length - failedPaths.size,
    parseFailures: extraction.parseFailures,
    limitsHit,
    complete: failedPaths.size === 0 && limitsHit.length === 0
  }
  const inventory = {
    schemaVersion: 2,
    adapterId: ADAPTER_ID,
    adapterKind: 'kotlin-compose',
    adapterVersion: 2,
    scopeFingerprint: 'sha256:' + 'b'.repeat(64),
    configHash: 'sha256:' + 'd'.repeat(64),
    modes: config.modes,
    tokens: extraction.tokens,
    witness,
    counts: { tokens: extraction.tokens.length, unsupportedModes, aliasEdges }
  }
  const schemaOk = validate(inventory)
  if (!schemaOk) {
    const first = validate.errors[0]
    assert.fail(`schema rejected: ${first.instancePath || '/'} ${first.message}`)
  }
  assert.equal(projectInventorySemanticError(inventory), null)
  const lineEndingOnly = structuredClone(inventory)
  for (const token of lineEndingOnly.tokens) token.source.fileHash = 'sha256:' + 'e'.repeat(64)
  assert.equal(projectInventorySemanticHash(lineEndingOnly), projectInventorySemanticHash(inventory),
    'exact source byte hashes are evidence, not semantic drift')
  assert.equal(unsupportedModes, 6)
  assert.equal(aliasEdges, 6)
})

console.log(`\ntoken-kotlin-adapter.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
