import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { execFileSync, spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import assert from 'node:assert/strict'
import { bytesHash } from '../runtime/canonical-json.mjs'
import { sourceIdentity, validObservedCapture } from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const SCRIPT = join(HERE, '..', 'scripts', 'check-screen-cache.mjs')
const designParser = createRequire(import.meta.url)(join(HERE, '..', 'scripts', 'design-parser.cjs'))
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const url = (id) => `https://www.figma.com/design/fileKey?node-id=${encodeURIComponent(String(id).replace(':', '-'))}`
const spec = (theme = 'light', screen = 'Home', nodeId = '1:2') => {
  const frameSizeDp = { w: 100, h: 200 }
  const element = {
    stableId: 'home-title', figmaNodeId: '1:3', name: 'Title',
    bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'],
  }
  return {
    schemaVersion: 2,
    screen,
    frameSizeDp,
    theme,
    source: { fileKey: 'fileKey', nodeId },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp', density: 1, origin: 'frame' },
    themeMetadata: { themeKey: theme },
    nodes: [
      { stableId: 'root', figmaNodeId: nodeId, name: screen, role: 'screen', bboxDp: { x: 0, y: 0, ...frameSizeDp } },
      { ...element },
    ],
    elements: [element],
  }
}

function tokenCaptureBytes(nodeId, theme, variantId) {
  const source = sourceIdentity({
    nodeId,
    context: { theme, locale: 'default', platform: 'shared' },
    origin: {
      kind: 'task-screen',
      taskStem: 'TASK_1_fixture',
      screenKey: 'Home',
      variantId
    }
  })
  const capture = validObservedCapture({
    source,
    captureOperationId: 'tokop_0123456789abcdef',
    captureSequence: 1
  })
  return Buffer.from(JSON.stringify(capture, null, 2) + '\n')
}

function writeCache(root, { nodeId = '1:2', darkOnly = false, indexOnly = false, specTheme = null, specScreen = 'Home', kind = null, omitKind = false, invalidInstances = false, summaryMismatch = false } = {}) {
  const dir = join(root, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const fetchedAt = '2026-01-01T00:00:00Z'
  const variantTheme = darkOnly ? 'dark' : (specTheme || 'light')
  const imageFile = darkOnly ? 'Home.dark.png' : 'Home.png'
  const specFile = darkOnly ? 'Home.dark.spec.json' : 'Home.spec.json'
  const node = darkOnly
    ? { kind: kind || 'screen', darkUrl: url(nodeId), darkNodeId: nodeId, darkFetchedAt: fetchedAt }
    : { kind: kind || 'screen', url: url(nodeId), nodeId, fetchedAt }
  if (omitKind) delete node.kind
  node.variants = [{
    id: `${variantTheme}-default-shared`, theme: variantTheme, locale: 'default', platform: 'shared',
    url: url(nodeId), nodeId, fetchedAt, imageFile, specFile, instancesFile: 'Home.instances.json',
    tokensFile: darkOnly ? 'Home.dark.tokens.json' : 'Home.tokens.json',
    tokensHash: 'sha256:' + '0'.repeat(64),
    captureOperationId: 'tokop_0123456789abcdef',
    captureSequence: 1
  }]
  if (summaryMismatch) node.variants[0].nodeId = '9:9'
  const tokenBytes = tokenCaptureBytes(nodeId, variantTheme, node.variants[0].id)
  node.variants[0].tokensHash = bytesHash(tokenBytes)
  writeFileSync(join(dir, node.variants[0].tokensFile), tokenBytes)
  writeFileSync(join(dir, 'index.json'), JSON.stringify({ schemaVersion: 3, taskStem: 'TASK_1_fixture', nodes: { Home: node } }, null, 2))
  if (indexOnly) return
  if (darkOnly) {
    writeFileSync(join(dir, 'Home.dark.spec.json'), JSON.stringify(spec(specTheme || 'dark', specScreen, nodeId), null, 2))
    writeFileSync(join(dir, 'Home.dark.png'), pngHeader(100, 200))
  } else {
    writeFileSync(join(dir, 'Home.spec.json'), JSON.stringify(spec(specTheme || 'light', specScreen, nodeId), null, 2))
    writeFileSync(join(dir, 'Home.png'), pngHeader(100, 200))
  }
  writeFileSync(join(dir, 'Home.instances.json'), invalidInstances ? '[{"name":"Button","extra":true}]' : '[]')
  writeFileSync(join(dir, 'Home.context.json'), '{}')
}

function run({ design, cache }) {
  const ws = mkdtempSync(join(tmpdir(), 'screen-cache-'))
  try {
    const task = join(ws, 'TASK_1_fixture.md')
    writeFileSync(task, `# Task\n\n## Design\n${design}\n`)
    writeCache(ws, cache)
    return execFileSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SCREEN_CACHE_ROOT: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports'), FIGMA_SCREEN_TASK_FILE: task },
      stdio: 'pipe'
    }).toString()
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
}

function runFail({ design, cache }) {
  const ws = mkdtempSync(join(tmpdir(), 'screen-cache-'))
  try {
    const task = join(ws, 'TASK_1_fixture.md')
    writeFileSync(task, `# Task\n\n## Design\n${design}\n`)
    writeCache(ws, cache)
    return spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SCREEN_CACHE_ROOT: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports'), FIGMA_SCREEN_TASK_FILE: task },
      encoding: 'utf8'
    })
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
}

check('current light cache passes', () => {
  const out = run({ design: `- Home — ${url('1:2')}`, cache: {} })
  assert.match(out, /screen-cache: TASK_1_fixture PASS/)
})

check('summary fields must match an exact current variant', () => {
  const r = runFail({ design: `- Home — ${url('1:2')}`, cache: { summaryMismatch: true } })
  assert.equal(r.status, 1, r.stdout + r.stderr)
  assert.match(r.stdout, /INDEX_PRIMARY_SUMMARY_MISMATCH/)
})

check('proposed task markdown can be validated from stdin', () => {
  const ws = mkdtempSync(join(tmpdir(), 'screen-cache-stdin-'))
  try {
    writeCache(ws, {})
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SCREEN_CACHE_ROOT: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports'), FIGMA_SCREEN_TASK_FILE: '-' },
      input: `# Task\n\n## Design\n- Home — ${url('1:2')}\n`,
      encoding: 'utf8'
    })
    assert.equal(r.status, 0, r.stdout + r.stderr)
    assert.match(r.stdout, /screen-cache: TASK_1_fixture PASS/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('invalid UTF-8 task source is a blocker instead of an empty Design success', () => {
  const ws = mkdtempSync(join(tmpdir(), 'screen-cache-invalid-task-'))
  try {
    const task = join(ws, 'TASK_1_fixture.md')
    writeFileSync(task, Buffer.from([0xff, 0xfe, 0xfd]))
    writeCache(ws, {})
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SCREEN_CACHE_ROOT: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports'), FIGMA_SCREEN_TASK_FILE: task },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /TASK_SOURCE_UNAVAILABLE/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('stale node id fails gate', () => {
  const r = runFail({ design: `- Home — ${url('1:2')}`, cache: { nodeId: '9:9' } })
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /NODE_ID_MISMATCH/)
})

check('dark spec on a plain-URL (primary) node passes — primary theme is agnostic (dark-first products)', () => {
  const out = run({ design: `- Home — ${url('1:2')}`, cache: { specTheme: 'dark' } })
  assert.match(out, /screen-cache: TASK_1_fixture PASS/)
})

check('dark-only cache passes with dark artifacts', () => {
  const out = run({ design: `- Home — dark:${url('3:4')}`, cache: { nodeId: '3:4', darkOnly: true } })
  assert.match(out, /screen-cache: TASK_1_fixture PASS/)
})

check('index-only cache is incomplete', () => {
  const r = runFail({ design: `- Home — ${url('1:2')}`, cache: { indexOnly: true } })
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /SPEC_MISSING/)
  assert.match(r.stdout, /PNG_MISSING/)
})

check('malformed instances.json fails gate because census input is not trustworthy', () => {
  const r = runFail({ design: `- Home — ${url('1:2')}`, cache: { invalidInstances: true } })
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /INSTANCES_SCHEMA_INVALID/)
})

check('dark spec with wrong identity/theme fails gate', () => {
  const r = runFail({ design: `- Home — dark:${url('3:4')}`, cache: { nodeId: '3:4', darkOnly: true, specTheme: 'light', specScreen: 'WrongScreen' } })
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /SPEC_SCREEN_MISMATCH/)
  assert.match(r.stdout, /SPEC_THEME_MISMATCH/)
})

check('duplicate screen name in one Design section fails gate (DUPLICATE_SCREEN)', () => {
  const r = runFail({ design: `- Home — ${url('1:2')}\n- Home — ${url('9:9')}`, cache: {} })
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /DUPLICATE_SCREEN/)
})

check('untagged residue beside light:/dark: tags fails gate (DESIGN_VALUE_RESIDUE)', () => {
  // 'darc:' is one character off 'dark:' — the second theme must not be silently dropped.
  const r = runFail({ design: `- Home — light:${url('1:2')} darc:${url('9:9')}`, cache: {} })
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /DESIGN_VALUE_RESIDUE/)
})

check('design [component] but index omits kind → KIND_MISSING_IN_INDEX blocks', () => {
  const r = runFail({ design: `- Home [component] — ${url('1:2')}`, cache: { omitKind: true } })
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /KIND_MISSING_IN_INDEX/)
})

check('design defaults to [screen] but index says component → KIND_MISMATCH blocks (reverse direction)', () => {
  const r = runFail({ design: `- Home — ${url('1:2')}`, cache: { kind: 'component' } })
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /KIND_MISMATCH/)
})

check('matching [component] kind in design and index passes', () => {
  const out = run({ design: `- Home [component] — ${url('1:2')}`, cache: { kind: 'component' } })
  assert.match(out, /screen-cache: TASK_1_fixture PASS/)
})

check('unrecognized kind tag surfaces through design.issues plumbing', () => {
  const r = runFail({ design: `- Home [sheet] — ${url('1:2')}`, cache: {} })
  assert.match(r.stdout, /UNRECOGNIZED_KIND_TAG/)
})

check('DUPLICATE_SCREEN and DESIGN_VALUE_RESIDUE malform the design; UNRECOGNIZED_KIND_TAG stays warn-grade', () => {
  const dup = `## Design\n- Home — ${url('1:2')}\n- Home — ${url('9:9')}\n`
  const residue = `## Design\n- Home — light:${url('1:2')} darc:${url('9:9')}\n`
  const tag = `## Design\n- Menu [sheet] — ${url('1:2')}\n`
  const clean = `## Design\n- Home — ${url('1:2')}\n`
  assert.equal(designParser.hasMalformedDesign(dup), true)
  assert.equal(designParser.hasMalformedDesign(residue), true)
  assert.equal(designParser.hasMalformedDesign(tag), false)
  assert.equal(designParser.hasMalformedDesign(clean), false)
  // The near-miss tag keeps the alternate shape: full name, kind 'screen', plus the flag.
  const sk = designParser.parseScreenKind('Menu [sheet]')
  assert.deepEqual({ name: sk.name, kind: sk.kind, tag: sk.unrecognizedTag }, { name: 'Menu [sheet]', kind: 'screen', tag: 'sheet' })
})

check('Design extraction ignores fenced and all seven CommonMark HTML-block decoy headings', () => {
  const valid = `- Decoy [screen] — ${url('1:2')}`
  const decoys = [
    ['````markdown', '## Design', valid, '````'],
    ['<script>', '## Design', valid, '</script>'],
    ['<!--', '## Design', valid, '-->'],
    ['<?audit', '## Design', valid, '?>'],
    ['<!AUDIT', '## Design', valid, '>'],
    ['<![CDATA[', '## Design', valid, ']]>'],
    ['<div data-audit="true">', '## Design', valid, ''],
    ['<audit-box data-mode=hidden>', '## Design', valid, ''],
  ]
  for (const decoy of decoys) {
    const parsed = designParser.parseDesign(`${decoy.join('\n')}\n## Design\n- broken bullet\n`)
    assert.equal(parsed.hasPullable, false, JSON.stringify(parsed))
    assert.ok(parsed.issues.some((item) => item.kind === 'UNPARSEABLE_DESIGN_BULLET'), JSON.stringify(parsed))
  }
  const control = designParser.parseDesign(`## Design\n- Home [screen] — ${url('1:2')}\n\n\`\`\`\`markdown\n## Design\n- broken bullet\n\`\`\`\`\n`)
  assert.equal(control.hasPullable, true)
  assert.equal(control.entries[0].screen, 'Home')
  assert.ok(!control.issues.some((item) => item.kind === 'UNPARSEABLE_DESIGN_BULLET'), JSON.stringify(control))
})

check('inline comment continuation rescans a second opener with JS/Python structural parity', () => {
  const hidden = `- Hidden [screen] — ${url('1:2')}`
  const markdown = [
    'lead <!-- first comment',
    '--> <!-- second comment',
    '## Design',
    hidden,
    '-->',
    '## Design',
    '- broken bullet',
    '',
  ].join('\n')
  const parsed = designParser.parseDesign(markdown)
  assert.equal(parsed.hasPullable, false, JSON.stringify(parsed))
  assert.ok(parsed.issues.some((item) => item.kind === 'UNPARSEABLE_DESIGN_BULLET'), JSON.stringify(parsed))

  const terminatorSuffix = [
    'lead <!-- inline comment',
    '-->## Design',
    hidden,
    '## Design',
    '- broken bullet',
    '',
  ].join('\n')
  const suffixParsed = designParser.parseDesign(terminatorSuffix)
  assert.equal(suffixParsed.hasPullable, false, JSON.stringify(suffixParsed))
  assert.ok(suffixParsed.issues.some((item) => item.kind === 'UNPARSEABLE_DESIGN_BULLET'), JSON.stringify(suffixParsed))

  const boundary = join(HERE, '..', '..', 'tasks', 'anchored-task-fs.py')
  for (const sample of [markdown, terminatorSuffix]) {
    const python = spawnSync('python3', ['-c', [
      'import importlib.util, json, sys',
      "spec = importlib.util.spec_from_file_location('anchored_task_fs_comment_probe', sys.argv[1])",
      'module = importlib.util.module_from_spec(spec)',
      'spec.loader.exec_module(module)',
      'print(json.dumps(module.structural_text(sys.argv[2])))',
    ].join('\n'), boundary, sample], { env: { ...process.env, PYTHONDONTWRITEBYTECODE: '1' }, encoding: 'utf8', timeout: 5000 })
    assert.equal(python.error, undefined, String(python.error || ''))
    assert.equal(python.status, 0, python.stderr || python.stdout)
    assert.equal(JSON.parse(python.stdout), designParser.structuralText(sample))
  }
})

check('inline code spans and escaped openers cannot forge HTML-comment structure (JS/Python parity)', () => {
  const hidden = `- Hidden [screen] — ${url('1:2')}`
  const brokenDesign = ['## Design', '- broken bullet', ''].join('\n')
  const samples = [
    // Every delimiter length is independent; equal maximal runs close it.
    ['same-line 1/2/3 ticks', [
      'lead `<!--` and ``<!--`` and ```<!--```',
      brokenDesign,
    ].join('\n')],
    // Code spans may cross physical lines while the paragraph remains open.
    ...[1, 2, 3].map((length) => {
      const ticks = '`'.repeat(length)
      return [`multiline ${length} ticks`, [
        `lead ${ticks}code`,
        'continued <!--',
        `still code${ticks}`,
        brokenDesign,
      ].join('\n')]
    }),
    ['nested unequal runs', [
      'lead ``outer ` <!-- inner ` outer``',
      brokenDesign,
    ].join('\n')],
    // Neither run has an equal-length closer, so the real comment still opens
    // and hides only the decoy Design section.
    ['unmatched and mismatched runs', [
      'lead `` unmatched ` <!-- real comment',
      '## Design',
      hidden,
      '-->',
      brokenDesign,
    ].join('\n')],
    // Odd backslash parity escapes `<`; two slashes leave it unescaped.
    ['escaped opener', [
      'lead \\<!-- escaped comment opener',
      brokenDesign,
    ].join('\n')],
    ['even-slash real opener', [
      'lead \\\\<!-- real comment opener',
      '## Design',
      hidden,
      '-->',
      brokenDesign,
    ].join('\n')],
    ['astral UTF-16 offsets inside a real comment', [
      'emoji 🧪 <!-- real comment opener',
      '🧪',
      '## Design',
      hidden,
      '-->',
      brokenDesign,
    ].join('\n')],
    ['indented-code opener', [
      '    <!--',
      brokenDesign,
    ].join('\n')],
  ]

  const boundary = join(HERE, '..', '..', 'tasks', 'anchored-task-fs.py')
  for (const [label, sample] of samples) {
    const parsed = designParser.parseDesign(sample)
    assert.equal(parsed.hasPullable, false, `${label}: ${JSON.stringify(parsed)}`)
    assert.ok(parsed.issues.some((item) => item.kind === 'UNPARSEABLE_DESIGN_BULLET'),
      `${label}: ${JSON.stringify(parsed)}`)

    const structural = designParser.structuralText(sample)
    assert.equal(structural.length, sample.length, `${label}: offsets changed`)
    assert.deepEqual(structural.match(/\n/g), sample.match(/\n/g), `${label}: line offsets changed`)
    const python = spawnSync('python3', ['-c', [
      'import importlib.util, json, sys',
      "spec = importlib.util.spec_from_file_location('anchored_task_fs_code_probe', sys.argv[1])",
      'module = importlib.util.module_from_spec(spec)',
      'spec.loader.exec_module(module)',
      'print(json.dumps(module.structural_text(sys.argv[2])))',
    ].join('\n'), boundary, sample], { env: { ...process.env, PYTHONDONTWRITEBYTECODE: '1' }, encoding: 'utf8', timeout: 5000 })
    assert.equal(python.error, undefined, `${label}: ${String(python.error || '')}`)
    assert.equal(python.status, 0, `${label}: ${python.stderr || python.stdout}`)
    assert.equal(JSON.parse(python.stdout), structural, `${label}: JS/Python drift`)
  }
})

check('vertical controls cannot terminate HTML blocks, paragraphs, or forge thematic breaks', () => {
  const hidden = `- Hidden [screen] — ${url('1:2')}`
  const broken = ['## Design', '- broken bullet', ''].join('\n')
  const samples = [
    ['VT does not close type-6 HTML', [
      '<div>',
      '\v',
      '## Design',
      hidden,
      '',
      broken,
    ].join('\n')],
    ['FF does not close type-7 HTML', [
      '<audit-box>',
      '\f',
      '## Design',
      hidden,
      '',
      broken,
    ].join('\n')],
    ['VT does not close a paragraph before type-7 HTML', [
      'paragraph remains open',
      '\v',
      '<audit-box>',
      broken,
    ].join('\n')],
    ['control-separated markers are not thematic', [
      'paragraph remains open',
      '-\v-\v-',
      '<audit-box>',
      broken,
    ].join('\n')],
  ]
  const boundary = join(HERE, '..', '..', 'tasks', 'anchored-task-fs.py')
  for (const [label, sample] of samples) {
    const parsed = designParser.parseDesign(sample)
    assert.equal(parsed.hasPullable, false, `${label}: ${JSON.stringify(parsed)}`)
    assert.ok(parsed.issues.some((item) => item.kind === 'UNPARSEABLE_DESIGN_BULLET'),
      `${label}: ${JSON.stringify(parsed)}`)
    const python = spawnSync('python3', ['-c', [
      'import importlib.util, json, sys',
      "spec = importlib.util.spec_from_file_location('anchored_task_fs_control_probe', sys.argv[1])",
      'module = importlib.util.module_from_spec(spec)',
      'spec.loader.exec_module(module)',
      'print(json.dumps(module.structural_text(sys.argv[2])))',
    ].join('\n'), boundary, sample], { env: { ...process.env, PYTHONDONTWRITEBYTECODE: '1' }, encoding: 'utf8', timeout: 5000 })
    assert.equal(python.error, undefined, `${label}: ${String(python.error || '')}`)
    assert.equal(python.status, 0, `${label}: ${python.stderr || python.stdout}`)
    assert.equal(JSON.parse(python.stdout), designParser.structuralText(sample), `${label}: JS/Python drift`)
  }
})

check('CommonMark fence indentation cannot hide or prematurely expose Design headings', () => {
  for (const opener of ['    ````markdown', '\t````markdown']) {
    const parsed = designParser.parseDesign(`${opener}\n## Design\n- broken bullet\n`)
    assert.ok(parsed.issues.some((item) => item.kind === 'UNPARSEABLE_DESIGN_BULLET'),
      `${JSON.stringify(opener)} incorrectly opened a fence: ${JSON.stringify(parsed)}`)
  }

  const overindentedClose = designParser.parseDesign([
    '````markdown',
    '    ````',
    '## Design',
    `- Hidden — ${url('1:2')}`,
    '````',
    '## Design',
    '- broken bullet',
    '',
  ].join('\n'))
  assert.equal(overindentedClose.hasPullable, false, JSON.stringify(overindentedClose))
  assert.ok(overindentedClose.issues.some((item) => item.kind === 'UNPARSEABLE_DESIGN_BULLET'),
    JSON.stringify(overindentedClose))
})

check('ATX closing-hash parsing stays bounded on a long heading without closing hashes', () => {
  const parserPath = join(HERE, '..', 'scripts', 'design-parser.cjs')
  const probe = spawnSync(process.execPath, ['-e', [
    `const parser = require(${JSON.stringify(parserPath)});`,
    "const parsed = parser.parseAtxHeadingLine('## ' + ' '.repeat(256 * 1024) + 'x');",
    "if (!parsed || parsed.level !== 2 || parsed.name !== 'x') process.exit(1);",
  ].join('\n')], { encoding: 'utf8', timeout: 5000 })
  assert.equal(probe.error, undefined, String(probe.error || ''))
  assert.equal(probe.status, 0, probe.stderr || probe.stdout)
})

check('W2-5: a PULLABLE screen name outside [A-Za-z0-9_] blocks at prep (RISKY_SCREEN_NAME); none-bullets are exempt', () => {
  // Parens in the frame name ('Screen (Content)') made the derived capture filename
  // unproducible by a Kotlin test — the production class surfaced 30 minutes later as
  // MISSING_CAPTURE; now it blocks at the cache gate with a rename message.
  const r = runFail({ design: `- Speed Test (Verified) — ${url('1:2')}`, cache: {} })
  assert.notEqual(r.status, 0, 'gate mode must EXIT non-zero on a risky pullable name')
  assert.match(r.stdout, /RISKY_SCREEN_NAME/)
  // W2-5 message substance: the block is only actionable because it says WHAT to do —
  // pin the rename instruction so a reworded message can't silently drop the remedy.
  assert.match(r.stdout, /rename the Figma frame.*PascalCase/)
  // Warn-grade: not in MALFORMED_DESIGN_KINDS — the ship backstops judge only the whitelist.
  assert.equal(designParser.hasMalformedDesign(`## Design\n- Speed Test (Verified) — ${url('1:2')}\n`), false)
  // A `none` bullet never derives a capture from its name — no issue, any name legal.
  const noneParsed = designParser.parseDesignSources([`## Design\n- Speed Test (Verified) — none (design deleted)\n`])
  assert.ok(!noneParsed.issues.some((i) => i.kind === 'RISKY_SCREEN_NAME'))
  // A clean PascalCase pullable name stays issue-free.
  const cleanParsed = designParser.parseDesignSources([`## Design\n- SpeedTestVerified — ${url('1:2')}\n`])
  assert.ok(!cleanParsed.issues.some((i) => i.kind === 'RISKY_SCREEN_NAME'))
})

check('R2-3: `- gate: strict` is recognized (gateOverride, no screen entry); any other gate value malforms the design', () => {
  const ok = designParser.parseDesignSources([`## Design\n- Home — ${url('1:2')}\n- gate: strict\n`])
  assert.equal(ok.gateOverride, 'strict')
  assert.equal(ok.entries.length, 1, 'the gate bullet is not a screen entry')
  assert.ok(!ok.issues.length, `gate: strict must be issue-free, got ${JSON.stringify(ok.issues)}`)
  assert.equal(designParser.hasMalformedDesign(`## Design\n- Home — ${url('1:2')}\n- gate: strict\n`), false)
  // The weakening direction has no grammar: any other value is a MALFORMED design (blocks at prep).
  const weak = designParser.parseDesignSources([`## Design\n- Home — ${url('1:2')}\n- gate: advisory\n`])
  assert.equal(weak.gateOverride, null)
  const residue = weak.issues.find((i) => i.kind === 'DESIGN_VALUE_RESIDUE')
  assert.ok(residue, 'gate: advisory must raise DESIGN_VALUE_RESIDUE')
  assert.match(residue.message, /tighten-only/)
  assert.equal(designParser.hasMalformedDesign(`## Design\n- Home — ${url('1:2')}\n- gate: advisory\n`), true)
})

check('R2-3 guard: a SCREEN whose name starts with `Gate:` keeps its normal parse (separator present)', () => {
  // The gate interception lives on the NO-separator branch only — `- Gate: Confirm — none (…)`
  // is a legitimate audited-none bullet and must neither be dropped nor malform the design.
  const parsed = designParser.parseDesignSources([`## Design\n- Gate: Confirm — none (no mock exists)\n`])
  assert.equal(parsed.entries.length, 1)
  assert.equal(parsed.entries[0].screen, 'Gate: Confirm')
  assert.equal(parsed.entries[0].none, true)
  assert.equal(parsed.gateOverride, null)
  assert.ok(!parsed.issues.some((i) => i.kind === 'DESIGN_VALUE_RESIDUE'), JSON.stringify(parsed.issues))
  assert.equal(designParser.hasMalformedDesign(`## Design\n- Gate: Confirm — none (no mock exists)\n`), false)
})

check('CommonMark H2 indentation and closing hashes share one Design-section boundary', () => {
  for (let indent = 0; indent <= 3; indent++) {
    const pad = ' '.repeat(indent)
    const parsed = designParser.parseDesign([
      `${pad}## Design ##`,
      `- Home — ${url('1:2')}`,
      `${pad}## Inputs ##`,
      '- broken bullet outside Design',
      '',
    ].join('\n'))
    assert.equal(parsed.entries.length, 1, `indent ${indent}`)
    assert.equal(parsed.entries[0].screen, 'Home', `indent ${indent}`)
    assert.equal(parsed.issues.length, 0, `indent ${indent}: ${JSON.stringify(parsed.issues)}`)
    assert.equal(designParser.hasDesignSection(`${pad}## Design ##\n- broken bullet\n`), true, `presence indent ${indent}`)
  }

  const code = designParser.parseDesign([
    '    ## Design ##',
    `- Home — ${url('1:2')}`,
    '',
  ].join('\n'))
  assert.equal(code.entries.length, 0, 'four-space indented code must not open Design')
  assert.equal(code.issues.length, 0, 'content outside a structural Design section is not a malformed Design entry')
  assert.equal(designParser.hasDesignSection('    ## Design ##\n- broken bullet\n'), false)
  assert.equal(designParser.hasDesignSection('\t## Design ##\n- broken bullet\n'), false)
  assert.equal(designParser.hasDesignSection('````markdown\n## Design\n- broken bullet\n````\n'), false)
})

check('W2-5: DESIGN_VALUE_RESIDUE messages quote the offending value and name the accepted `none` shapes', () => {
  // The none-residue message is the operator's ONLY spec of the audited-none grammar at the
  // point of failure — pin the quoted value + both accepted shapes so a reword can't
  // degrade it back to an uninformative "malformed" (the W2-5 production trap).
  const noneRes = designParser.parseDesignSources(['## Design\n- Home — none (broken (reason))\n'])
  const noneIssue = noneRes.issues.find((i) => i.kind === 'DESIGN_VALUE_RESIDUE')
  assert.ok(noneIssue, 'malformed none-value must raise DESIGN_VALUE_RESIDUE')
  assert.match(noneIssue.message, /none \(broken \(reason\)\)/)          // quotes the actual value
  assert.match(noneIssue.message, /`none`, or `none \(<reason>\)`/)      // names both accepted shapes
  const tagRes = designParser.parseDesignSources([`## Design\n- Home — light:${url('1:2')} darc:${url('9:9')}\n`])
  const tagIssue = tagRes.issues.find((i) => i.kind === 'DESIGN_VALUE_RESIDUE')
  assert.ok(tagIssue, 'untagged residue beside theme tags must raise DESIGN_VALUE_RESIDUE')
  assert.match(tagIssue.message, /every token must be a tagged theme URL/)
})

check('a `none` value with residue is malformed (W1 bypass closed); exact `none`/`none (<reason>)` stay legal', () => {
  // `none <url>`: the smuggled URL would be subtracted from bodyCitesFigmaNode's scan with the
  // rest of ## Design — the task would self-classify non-UI and ship uncompared. Fail closed.
  assert.equal(designParser.hasMalformedDesign(`## Design\n- Home — none ${url('1:2')}\n`), true)
  assert.equal(designParser.hasMalformedDesign('## Design\n- Home — none see the old mock\n'), true)
  assert.equal(designParser.hasMalformedDesign(`## Design\n- Home — none (why) ${url('1:2')}\n`), true)
  assert.equal(designParser.hasMalformedDesign('## Design\n- Home — none\n'), false)
  assert.equal(designParser.hasMalformedDesign('## Design\n- Home — none (no mock exists)\n'), false)
  // A URL inside the audited parenthetical reason stays a legal, audited opt-out.
  assert.equal(designParser.hasMalformedDesign(`## Design\n- Home — none (mock at ${url('1:2')} TBD)\n`), false)
})

// --- R6-2: a chromeCrop-stamped spec is VERIFIED, not trusted -------------------------------

// Minimal real PNG header (signature + IHDR) — enough for the gate's header-only size reader.
function pngHeader(w, h) {
  const b = Buffer.alloc(24)
  b.write('\x89PNG\r\n\x1a\n', 0, 'latin1')
  b.writeUInt32BE(13, 8)
  b.write('IHDR', 12, 'latin1')
  b.writeUInt32BE(w, 16)
  b.writeUInt32BE(h, 20)
  return b
}
const stampedSpec = (mutate = (s) => s) => {
  const current = spec('light', 'Home', '1:2')
  current.frameSizeDp = { w: 100, h: 160 }
  current.nodes[0].bboxDp = { x: 0, y: 0, w: 100, h: 160 }
  current.chromeCrop = { topDp: 30, bottomDp: 10, matched: ['9:41'], at: '2026-07-12T00:00:00Z' }
  current.elements = [{ stableId: 't', figmaNodeId: '1:3', name: 'Title', bboxDp: { x: 0, y: 10, w: 50, h: 20 } }]
  current.nodes = [current.nodes[0], { ...current.elements[0] }]
  return mutate(current)
}
function runStamped(spec, png = pngHeader(300, 480) /* aspect 0.625 == 100/160 */, kind = 'screen') {
  const ws = mkdtempSync(join(tmpdir(), 'screen-cache-r6-'))
  try {
    const task = join(ws, 'TASK_1_fixture.md')
    const kindTag = kind === 'screen' ? '' : ` [${kind}]`
    writeFileSync(task, `# Task\n\n## Design\n- Home${kindTag} — ${url('1:2')}\n`)
    const dir = join(ws, 'screens', 'TASK_1_fixture')
    mkdirSync(dir, { recursive: true })
    const fetchedAt = '2026-01-01T00:00:00Z'
    const node = {
      kind, url: url('1:2'), nodeId: '1:2', fetchedAt,
      variants: [{
        id: 'light-default-shared', theme: 'light', locale: 'default', platform: 'shared',
        url: url('1:2'), nodeId: '1:2', fetchedAt, imageFile: 'Home.png',
        specFile: 'Home.spec.json', instancesFile: 'Home.instances.json',
        tokensFile: 'Home.tokens.json',
        tokensHash: 'sha256:' + '0'.repeat(64),
        captureOperationId: 'tokop_0123456789abcdef',
        captureSequence: 1
      }]
    }
    const tokenBytes = tokenCaptureBytes('1:2', 'light', 'light-default-shared')
    node.variants[0].tokensHash = bytesHash(tokenBytes)
    writeFileSync(join(dir, 'Home.tokens.json'), tokenBytes)
    writeFileSync(join(dir, 'index.json'), JSON.stringify({ schemaVersion: 3, taskStem: 'TASK_1_fixture', nodes: { Home: node } }, null, 2))
    writeFileSync(join(dir, 'Home.spec.json'), JSON.stringify(spec, null, 2))
    writeFileSync(join(dir, 'Home.png'), png)
    writeFileSync(join(dir, 'Home.instances.json'), '[]')
    writeFileSync(join(dir, 'Home.context.json'), '{}')
    return spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SCREEN_CACHE_ROOT: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports'), FIGMA_SCREEN_TASK_FILE: task },
      encoding: 'utf8'
    })
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
}

check('R6-2: a stamped-clean spec (consistent crop, matching PNG aspect) passes', () => {
  const r = runStamped(stampedSpec())
  assert.equal(r.status, 0, r.stdout)
  assert.match(r.stdout, /screen-cache: TASK_1_fixture PASS/)
})

check('R6-2: a chromeCrop stamp on a non-screen node blocks as stale', () => {
  const r = runStamped(stampedSpec(), pngHeader(300, 480), 'component')
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /CHROME_CROP_KIND_MISMATCH/)
})

check('R6-2: a corrupt oracle PNG blocks instead of skipping the aspect check', () => {
  const r = runStamped(stampedSpec(), Buffer.from('not-a-png'))
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /PNG_UNREADABLE/)
})

check('R6-2: a stamped spec with a surviving "9:41" element blocks (CHROME_CROP_RESIDUE)', () => {
  const r = runStamped(stampedSpec((s) => {
    s.elements.push({ stableId: 'ghost', name: 'Clock', bboxDp: { x: 5, y: 5, w: 20, h: 10 }, text: '9:41' })
    return s
  }))
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /CHROME_CROP_RESIDUE/)
  assert.match(r.stdout, /normalize-oracle/)
})

check('R6-2: a stamped spec with a negative-y bbox blocks (CHROME_CROP_BAD_SHIFT)', () => {
  const r = runStamped(stampedSpec((s) => {
    s.elements[0].bboxDp.y = -5
    return s
  }))
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /CHROME_CROP_BAD_SHIFT/)
})

check('R6-2: a stamped spec whose PNG was NOT cropped in step blocks (CHROME_CROP_ASPECT)', () => {
  // PNG aspect 300/600 = 0.5 vs post-crop frame 100/160 = 0.625 → 20% divergence > tolerance.
  const r = runStamped(stampedSpec(), pngHeader(300, 600))
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /CHROME_CROP_ASPECT/)
})

check('R6-2: the aspect bound is TIGHT (2%) — a realistic missed-crop skew (~8%) blocks too', () => {
  // The comparator's loose 0.15 tolerance would be blind here: 300/520 = 0.577 vs 0.625 → 7.7%.
  const r = runStamped(stampedSpec(), pngHeader(300, 520))
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /CHROME_CROP_ASPECT/)
})

check('R6-2: residue remedy is honest — re-pull/rename, never the no-op re-run of a stamped spec', () => {
  const r = runStamped(stampedSpec((s) => {
    s.elements.push({ stableId: 'ghost', name: 'Clock', bboxDp: { x: 5, y: 5, w: 20, h: 10 }, text: '9:41' })
    return s
  }))
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /re-pull the screens/)
  assert.match(r.stdout, /rename it in Figma/)
})

check('R6-2: mid-frame "9:41" CONTENT on a stamped spec is exempt from residue (band-gated)', () => {
  const r = runStamped(stampedSpec((s) => {
    s.elements.push({ stableId: 'alarm', name: 'AlarmTime', bboxDp: { x: 10, y: 70, w: 60, h: 30 }, text: '9:41' })
    return s
  }))
  assert.equal(r.status, 0, r.stdout)
  assert.ok(!/CHROME_CROP_RESIDUE/.test(r.stdout))
})

check('R6-2 control: an UN-stamped spec with a "9:41" element is not judged by the crop invariants', () => {
  const r = runStamped(stampedSpec((s) => {
    delete s.chromeCrop
    s.elements.push({ stableId: 'time', name: 'Time', bboxDp: { x: 5, y: 5, w: 20, h: 10 }, text: '9:41' })
    return s
  }))
  assert.equal(r.status, 0, r.stdout)
  assert.ok(!/CHROME_CROP_/.test(r.stdout))
})

check('R6-2 torn-state: an UN-stamped spec whose PNG has another aspect blocks', () => {
  const spec = stampedSpec((s) => { delete s.chromeCrop; return s })
  const r = runStamped(spec, pngHeader(300, 520))
  assert.notEqual(r.status, 0)
  assert.match(r.stdout, /SPEC_PNG_ASPECT_MISMATCH/)
  assert.match(r.stdout, /re-pull the screens/)
})

console.log(`\ncheck-screen-cache.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
