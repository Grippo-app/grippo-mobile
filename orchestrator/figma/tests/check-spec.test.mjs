import { mkdtempSync, mkdirSync, copyFileSync, rmSync, writeFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { execFileSync, spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'

const HERE = dirname(fileURLToPath(import.meta.url))
const SCRIPT = join(HERE, '..', 'scripts', 'check-spec.mjs')
const VALID = join(HERE, 'spec', 'spec-valid.json')
const INVALID = join(HERE, 'spec', 'spec-invalid.json')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

function workspace(specFile) {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  copyFileSync(specFile, join(dir, 'HomeScreen.spec.json'))
  return ws
}

function currentSpec(input) {
  const elements = (input.elements || []).map((element, index) => ({
    figmaNodeId: element.figmaNodeId || `1:${index + 2}`,
    ...element,
  }))
  return {
    schemaVersion: 2,
    ...input,
    source: { fileKey: 'fixture', nodeId: '1:1' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp', density: 1, origin: 'frame' },
    themeMetadata: { themeKey: input.theme },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:1', name: input.screen, role: 'screen', bboxDp: { x: 0, y: 0, ...input.frameSizeDp } },
      ...elements.map((element) => ({ ...element })),
    ],
    elements,
  }
}

check('valid spec passes gate and writes report', () => {
  const ws = workspace(VALID)
  try {
    const out = execFileSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      stdio: 'pipe'
    }).toString()
    assert.match(out, /check-spec: TASK_1_fixture PASS/)
    assert.match(out, /Report:/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('invalid spec fails gate', () => {
  const ws = workspace(INVALID)
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /BLOCKER/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('non-v2 spec is rejected in advisory output', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  copyFileSync(VALID, join(dir, 'HomeScreen.spec.json'))
  const unsupported = {
    screen: 'Home',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    elements: [{ name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] }]
  }
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(unsupported, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--advisory'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.equal(r.status, 0)
    assert.match(r.stdout, /MISSING_STABLE_ID|SCHEMA_INVALID/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('dark spec in a non-.dark (primary) file PASSES — primary is theme-agnostic (dark-first products)', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const darkPrimary = currentSpec({
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'dark',   // dark frame written to a plain-URL (non-.dark) file — the pull contract permits this
    elements: [{ stableId: 'title', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] }]
  })
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(darkPrimary, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], { env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') }, encoding: 'utf8' })
    assert.equal(r.status, 0, r.stdout)
    assert.doesNotMatch(r.stdout, /SPEC_THEME_MISMATCH/)
  } finally { rmSync(ws, { recursive: true, force: true }) }
})

check('light spec in a .dark file STILL fails — an explicit dark file pins dark', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const lightInDark = currentSpec({ screen: 'HomeScreen', frameSizeDp: { w: 100, h: 200 }, theme: 'light', elements: [{ stableId: 'title', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] }] })
  writeFileSync(join(dir, 'HomeScreen.dark.spec.json'), JSON.stringify(lightInDark, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], { env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') }, encoding: 'utf8' })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /SPEC_THEME_MISMATCH/)
  } finally { rmSync(ws, { recursive: true, force: true }) }
})

check('filename screen/theme mismatch fails gate', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const wrong = currentSpec({
    screen: 'WrongScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    elements: [{ stableId: 'title', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] }]
  })
  writeFileSync(join(dir, 'HomeScreen.dark.spec.json'), JSON.stringify(wrong, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /SPEC_SCREEN_MISMATCH/)
    assert.match(r.stdout, /SPEC_THEME_MISMATCH/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('v2 semantic spec with projection passes gate', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const v2 = {
    schemaVersion: 2,
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    source: { fileKey: 'file', nodeId: '1:2' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp', density: 1, origin: 'frame' },
    themeMetadata: { themeKey: 'primary' },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', role: 'screen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } },
      { stableId: 'title', figmaNodeId: '1:3', name: 'Title', role: 'unclassified', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] }
    ],
    elements: [{ figmaNodeId: '1:3', name: 'Title', role: 'UNCLASSIFIED', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] }]
  }
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(v2, null, 2))
  try {
    const out = execFileSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      stdio: 'pipe'
    }).toString()
    assert.match(out, /check-spec: TASK_1_fixture PASS/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('v2 nodes-only spec fails instead of comparing zero elements', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const v2 = {
    schemaVersion: 2,
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    source: { fileKey: 'file', nodeId: '1:2' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp' },
    themeMetadata: { themeKey: 'primary' },
    nodes: [{ stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } }],
    elements: []
  }
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(v2, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /V2_PROJECTION_MISSING|SCHEMA_INVALID/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('v2 projection field mismatch fails gate', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const v2 = {
    schemaVersion: 2,
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    source: { fileKey: 'file', nodeId: '1:2' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp' },
    themeMetadata: { themeKey: 'primary' },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } },
      { stableId: 'title', figmaNodeId: '1:3', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'], componentSetName: 'PrimaryButton', componentSetNodeId: '1:30' }
    ],
    elements: [{ stableId: 'title', figmaNodeId: '1:3', name: 'Title', bboxDp: { x: 5, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'], componentSetName: 'PrimaryButton', componentSetNodeId: '1:31' }]
  }
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(v2, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /V2_PROJECTION_FIELD_MISMATCH/)
    assert.match(r.stdout, /componentSetNodeId/, 'the durable component identity participates in node→element projection integrity')
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('duplicate figmaNodeId identities fail gate', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const v2 = {
    schemaVersion: 2,
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    source: { fileKey: 'file', nodeId: '1:2' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp' },
    themeMetadata: { themeKey: 'primary' },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } },
      { stableId: 'title-a', figmaNodeId: '1:3', name: 'Title A', bboxDp: { x: 0, y: 0, w: 10, h: 10 } },
      { stableId: 'title-b', figmaNodeId: '1:3', name: 'Title B', bboxDp: { x: 0, y: 20, w: 10, h: 10 } }
    ],
    elements: [{ stableId: 'title-a', figmaNodeId: '1:3', name: 'Title A', bboxDp: { x: 0, y: 0, w: 10, h: 10 } }]
  }
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(v2, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /DUPLICATE_NODE_FIGMA_ID/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('placeholder words inside legit identifiers do not block the gate', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  // 'todo-list-item' (pipeline's own kebab-case stableId convention), a designer layer
  // named 'Todo card', and a variant value 'Unknown' were all false BLOCKERs under the
  // old \b word-boundary sweep — they must pass; only full-string placeholders block.
  const spec = currentSpec({
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    elements: [
      { stableId: 'todo-list-item', name: 'Todo card', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] },
      { stableId: 'state-chip', name: 'State chip', bboxDp: { x: 0, y: 20, w: 10, h: 10 }, componentSetName: 'Chip', componentSetNodeId: '1:30', variantProps: { State: 'Unknown filter' } }
    ]
  })
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(spec, null, 2))
  try {
    const out = execFileSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      stdio: 'pipe'
    }).toString()
    assert.match(out, /check-spec: TASK_1_fixture PASS/)
    assert.doesNotMatch(out, /PLACEHOLDER/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('full-string placeholder words still fail gate', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const spec = currentSpec({
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    elements: [{ stableId: 'title', name: ' TBD ', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] }]
  })
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(spec, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /PLACEHOLDER/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('angle-bracket placeholders fail gate', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const spec = currentSpec({
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    elements: [{ stableId: 'title', name: '<Product>', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] }]
  })
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(spec, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /PLACEHOLDER/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('non-v2 empty projection fails gate', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const spec = { screen: 'HomeScreen', frameSizeDp: { w: 100, h: 200 }, theme: 'light', elements: [] }
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(spec, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /NO_COMPARABLE_ELEMENTS|SCHEMA_INVALID/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('v2 light/dark themeMetadata mismatch fails gate', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const v2 = {
    schemaVersion: 2,
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'dark',
    source: { fileKey: 'file', nodeId: '1:2' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp' },
    themeMetadata: { themeKey: 'light' },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } },
      { stableId: 'title', figmaNodeId: '1:3', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 } }
    ],
    elements: [{ stableId: 'title', figmaNodeId: '1:3', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 } }]
  }
  writeFileSync(join(dir, 'HomeScreen.dark.spec.json'), JSON.stringify(v2, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /THEME_METADATA_MISMATCH/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('v2 projection cannot drop comparable fields from semantic node', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const v2 = {
    schemaVersion: 2,
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    source: { fileKey: 'file', nodeId: '1:2' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp' },
    themeMetadata: { themeKey: 'primary' },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } },
      { stableId: 'title', figmaNodeId: '1:3', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] }
    ],
    elements: [{ stableId: 'title', figmaNodeId: '1:3', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 } }]
  }
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(v2, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /V2_PROJECTION_FIELD_MISSING/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('v2 projection stableId and figmaNodeId must point to the same node', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const v2 = {
    schemaVersion: 2,
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    source: { fileKey: 'file', nodeId: '1:2' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp' },
    themeMetadata: { themeKey: 'primary' },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } },
      { stableId: 'title-a', figmaNodeId: '1:3', name: 'Title A', bboxDp: { x: 0, y: 0, w: 10, h: 10 } },
      { stableId: 'title-b', figmaNodeId: '1:4', name: 'Title B', bboxDp: { x: 0, y: 20, w: 10, h: 10 } }
    ],
    elements: [{ stableId: 'title-a', figmaNodeId: '1:4', name: 'Title A', bboxDp: { x: 0, y: 0, w: 10, h: 10 } }]
  }
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(v2, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /V2_PROJECTION_ID_CONFLICT/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('v2 projection comparison identity duplicates fail gate', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const v2 = {
    schemaVersion: 2,
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    source: { fileKey: 'file', nodeId: '1:2' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp' },
    themeMetadata: { themeKey: 'primary' },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } },
      { stableId: 'same', figmaNodeId: '1:3', name: 'Title A', bboxDp: { x: 0, y: 0, w: 10, h: 10 } },
      { stableId: 'other', figmaNodeId: 'same', name: 'Title B', bboxDp: { x: 0, y: 20, w: 10, h: 10 } }
    ],
    elements: [
      { stableId: 'same', figmaNodeId: '1:3', name: 'Title A', bboxDp: { x: 0, y: 0, w: 10, h: 10 } },
      { figmaNodeId: 'same', name: 'Title B', bboxDp: { x: 0, y: 20, w: 10, h: 10 } }
    ]
  }
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(v2, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /DUPLICATE_ELEMENT_COMPARISON_ID/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('v2 projection cannot omit nodes with comparable data', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const v2 = {
    schemaVersion: 2,
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    source: { fileKey: 'file', nodeId: '1:2' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp' },
    themeMetadata: { themeKey: 'primary' },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } },
      { stableId: 'title', figmaNodeId: '1:3', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] },
      { stableId: 'cta', figmaNodeId: '1:4', name: 'CTA', bboxDp: { x: 0, y: 20, w: 20, h: 10 }, componentSetName: 'PrimaryButton', componentSetNodeId: '1:30' }
    ],
    elements: [
      { stableId: 'title', figmaNodeId: '1:3', name: 'Title', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] }
    ]
  }
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(v2, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /V2_PROJECTION_NODE_MISSING/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('v2 projection cannot omit bbox-only visible/layout nodes', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const v2 = {
    schemaVersion: 2,
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    source: { fileKey: 'file', nodeId: '1:2' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp' },
    themeMetadata: { themeKey: 'primary' },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } },
      { stableId: 'layout-row', figmaNodeId: '1:5', name: 'LayoutRow', bboxDp: { x: 0, y: 40, w: 80, h: 12 } }
    ],
    elements: []
  }
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(v2, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /V2_PROJECTION_NODE_MISSING/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

check('v2 projection cannot omit root node with comparable visual fields', () => {
  const ws = mkdtempSync(join(tmpdir(), 'check-spec-'))
  const dir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(dir, { recursive: true })
  const v2 = {
    schemaVersion: 2,
    screen: 'HomeScreen',
    frameSizeDp: { w: 100, h: 200 },
    theme: 'light',
    source: { fileKey: 'file', nodeId: '1:2' },
    rootNodeId: 'root',
    coordinateSystem: { units: 'dp' },
    themeMetadata: { themeKey: 'primary' },
    nodes: [
      { stableId: 'root', figmaNodeId: '1:2', name: 'HomeScreen', bboxDp: { x: 0, y: 0, w: 100, h: 200 }, fills: ['#101010'] },
      { stableId: 'title', figmaNodeId: '1:3', name: 'Title', bboxDp: { x: 0, y: 10, w: 80, h: 12 } }
    ],
    elements: [
      { stableId: 'title', figmaNodeId: '1:3', name: 'Title', bboxDp: { x: 0, y: 10, w: 80, h: 12 } }
    ]
  }
  writeFileSync(join(dir, 'HomeScreen.spec.json'), JSON.stringify(v2, null, 2))
  try {
    const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', '--gate'], {
      env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: join(ws, 'reports') },
      encoding: 'utf8'
    })
    assert.notEqual(r.status, 0)
    assert.match(r.stdout, /V2_PROJECTION_NODE_MISSING/)
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

console.log(`\ncheck-spec.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
