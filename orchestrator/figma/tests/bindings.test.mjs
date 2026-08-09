// bindings.test.mjs — pins for the task bindings manifest (schemaVersion 2): the schema
// accepts the documented shape and rejects unknown keys / missing screenName / the retired
// v1 shape; loadBindings is strict (absence → null; any present foreign/malformed file
// throws; duplicate designComponentId/setNodeId forbidden while setName stays a repeatable label);
// bindingsManifestEntries derives capture-manifest entries only for screens that carry BOTH
// captureBasename and nodeId AND whose file exists; and compare-screen-spec consumes the
// bindings screen-map end-to-end (a bound implFile is used with ZERO --screen-map
// plumbing — proven by the gate naming exactly that file when it is absent).
// The census-side upsert of components[] rows is pinned in component-census.test.mjs.
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { loadBindings, bindingsManifestEntries } from '../scripts/_util.mjs'
import { compileSchema } from '../scripts/report-utils.mjs'
import { bytesHash } from '../runtime/canonical-json.mjs'
import { sourceIdentity, validObservedCapture } from './observed-token-fixtures.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const fetchedAt = '2026-01-01T00:00:00.000Z'
function indexNode(screen, nodeId, kind = 'screen', extra = {}) {
  const url = `https://www.figma.com/design/FileKey?node-id=${nodeId.replace(':', '-')}`
  return { kind, url, nodeId, fetchedAt, variants: [{ id: 'primary', theme: 'light', locale: 'default', platform: 'shared', url, nodeId, fetchedAt, imageFile: `${screen}.png`, instancesFile: `${screen}.instances.json` }], ...extra }
}

function writeScreenIndex(directory, nodes) {
  let ordinal = 0
  for (const [screenKey, node] of Object.entries(nodes)) {
    for (const variant of node.variants) {
      ordinal += 1
      const capture = validObservedCapture({
        source: sourceIdentity({
          nodeId: variant.nodeId,
          context: { theme: variant.theme, locale: variant.locale, platform: variant.platform },
          origin: { kind: 'task-screen', taskStem: 'TASK_1_fixture', screenKey, variantId: variant.id },
        }),
        captureOperationId: `tokop_${String(ordinal).padStart(16, '0')}`,
      })
      const tokenBytes = Buffer.from(JSON.stringify(capture, null, 2) + '\n')
      Object.assign(variant, {
        tokensFile: `${screenKey}.${variant.id}.tokens.json`,
        tokensHash: bytesHash(tokenBytes),
        captureOperationId: capture.captureOperationId,
        captureSequence: capture.captureSequence,
      })
      writeFileSync(join(directory, variant.tokensFile), tokenBytes)
    }
  }
  writeFileSync(join(directory, 'index.json'), JSON.stringify({ schemaVersion: 3, taskStem: 'TASK_1_fixture', nodes }))
}

const DESIGN_ID = 'figma-component:' + 'a'.repeat(16) + ':none:3:4'
const TWIN_ID = 'figma-component:' + 'a'.repeat(16) + ':none:9:9'
const componentRow = (overrides = {}) => ({
  designComponentId: DESIGN_ID,
  setNodeId: '3:4',
  setName: 'Banners',
  mappingId: 'cmap-' + 'a'.repeat(24),
  implementations: [{ adapterId: 'compose-ds', platform: 'android-compose', projectComponentId: 'compose-ds:symbol:com.x.Banner', sourcePath: 'ds/Banner.kt' }],
  ...overrides,
})

const GOOD = {
  schemaVersion: 2,
  stem: 'TASK_1_fixture',   // must match the loadBindings('TASK_1_fixture') key — a mismatch is fail-closed (see the cross-task test)
  screens: [
    { nodeId: '1:2', screenName: 'Home', implFile: 'ui/HomeScreen.kt', composable: 'HomeScreen', captureBasename: 'HomeScreenshot.png', qualifiers: 'w390dp-h844dp', kind: 'screen' },
    { screenName: 'Menu', kind: 'dialog' },   // partially filled is legal — override layer
  ],
  components: [componentRow()],
}

const validate = await compileSchema(join(HERE, '..', 'token-schemas', 'bindings.schema.json'), { gate: true })
check('schema: the documented v2 shape validates; partial or foreign records are rejected', () => {
  assert.equal(validate(GOOD), true, JSON.stringify(validate.errors))
  assert.equal(validate({ ...GOOD, extra: 1 }), false, 'unknown top-level key must fail')
  assert.equal(validate({ ...GOOD, screens: [{ nodeId: '1:2' }] }), false, 'screenName is required')
  assert.equal(validate({ ...GOOD, schemaVersion: 1 }), false, 'the retired v1 shape must fail — only schemaVersion 2 exists')
  for (const required of ['designComponentId', 'setNodeId', 'setName', 'mappingId', 'implementations']) {
    const partial = componentRow()
    delete partial[required]
    assert.equal(validate({ ...GOOD, components: [partial] }), false, `component binding requires ${required}`)
  }
  assert.equal(validate({ ...GOOD, components: [componentRow({ designComponentId: 'ds.banner' })] }), false,
    'a name-derived id must fail the designComponentId pattern')
  assert.equal(validate({ ...GOOD, components: [componentRow({ implementations: [] })] }), false,
    'implementations must be non-empty')
  assert.equal(validate({
    ...GOOD,
    components: [componentRow({ implementations: [{ adapterId: 'compose-ds', platform: 'android-compose' }] })],
  }), false, 'an implementation requires projectComponentId')
})

try {
  const ws = mkdtempSync(join(tmpdir(), 'bindings-'))
  const sdir = join(ws, 'screens', 'TASK_1_fixture')
  mkdirSync(sdir, { recursive: true })
  process.env.FIGMA_SPEC_SCREENS_DIR = join(ws, 'screens')

  check('loadBindings: absent optional file → null', () => {
    assert.equal(loadBindings('TASK_1_fixture'), null)
  })
  writeFileSync(join(sdir, 'bindings.json'), JSON.stringify(GOOD))
  check('loadBindings: valid file → normalized screens/components', () => {
    const b = loadBindings('TASK_1_fixture')
    assert.equal(b.screens.length, 2)
    assert.equal(b.components.length, 1)
    assert.equal(b.screens[0].captureBasename, 'HomeScreenshot.png')
    assert.equal(b.components[0].implementations[0].projectComponentId, 'compose-ds:symbol:com.x.Banner')
  })
  check('loadBindings: foreign schemaVersion or garbage throws (no silent recovery)', () => {
    writeFileSync(join(sdir, 'bindings.json'), JSON.stringify({ ...GOOD, schemaVersion: 1 }))
    assert.throws(() => loadBindings('TASK_1_fixture'), /current schemaVersion 2 contract/)
    writeFileSync(join(sdir, 'bindings.json'), '{not-json')
    assert.throws(() => loadBindings('TASK_1_fixture'), /not valid JSON/)
    writeFileSync(join(sdir, 'bindings.json'), JSON.stringify(GOOD))   // restore
  })
  check('loadBindings: a bindings file whose stem names a DIFFERENT task throws (no cross-task replay)', () => {
    writeFileSync(join(sdir, 'bindings.json'), JSON.stringify({ ...GOOD, stem: 'TASK_99_other' }))
    assert.throws(() => loadBindings('TASK_1_fixture'), /match the task stem/)
    writeFileSync(join(sdir, 'bindings.json'), JSON.stringify(GOOD))   // restore
  })
  check('loadBindings: duplicate durable ids throw; a repeated setName label is legal', () => {
    writeFileSync(join(sdir, 'bindings.json'), JSON.stringify({
      ...GOOD,
      components: [componentRow(), componentRow({ setName: 'OtherSet' })],
    }))
    assert.throws(() => loadBindings('TASK_1_fixture'), /duplicate designComponentId/)
    writeFileSync(join(sdir, 'bindings.json'), JSON.stringify({
      ...GOOD,
      components: [componentRow(), componentRow({
        designComponentId: TWIN_ID,
        mappingId: 'cmap-' + 'b'.repeat(24)
      })],
    }))
    assert.throws(() => loadBindings('TASK_1_fixture'), /duplicate setNodeId/)
    // Two sets sharing one display name are two rows — identity is the design id.
    writeFileSync(join(sdir, 'bindings.json'), JSON.stringify({
      ...GOOD,
      components: [componentRow(), componentRow({ designComponentId: TWIN_ID, setNodeId: '9:9', mappingId: 'cmap-' + 'b'.repeat(24) })],
    }))
    const twins = loadBindings('TASK_1_fixture')
    assert.equal(twins.components.length, 2)
    assert.equal(twins.components[0].setName, twins.components[1].setName)
    writeFileSync(join(sdir, 'bindings.json'), JSON.stringify(GOOD))
  })

  const robo = join(ws, 'robo')
  mkdirSync(robo, { recursive: true })
  writeFileSync(join(robo, 'HomeScreenshot.png'), 'png')
  check('bindingsManifestEntries: entries only for bound screens whose capture file EXISTS; partial entries skipped', () => {
    const b = loadBindings('TASK_1_fixture')
    const { entries, boundScreens } = bindingsManifestEntries(b, [robo], (p) => readFileSync && p === join(robo, 'HomeScreenshot.png'))
    assert.equal(entries.length, 1)
    assert.deepEqual(entries[0], { captureName: 'HomeScreenshot.png', path: join(robo, 'HomeScreenshot.png'), nodeId: '1:2', primaryState: true })
    assert.deepEqual([...boundScreens], ['Home'], 'Menu (no captureBasename/nodeId) never binds')
    const none = bindingsManifestEntries(null, [robo], () => true)
    assert.deepEqual(none, { entries: [], boundScreens: new Set() }, 'absent optional bindings → empty')
  })

  // End-to-end zero-plumbing proof: compare-screen-spec picks the implFile from bindings with
  // NO --screen-map/env — decisive because the gate then names EXACTLY that file as missing.
  const SPEC_FIXTURE = join(HERE, 'spec', 'spec-valid.json')
  const specData = JSON.parse(readFileSync(SPEC_FIXTURE, 'utf8'))
  writeFileSync(join(sdir, `${specData.screen}.spec.json`), JSON.stringify(specData))
  writeFileSync(join(sdir, 'bindings.json'), JSON.stringify({
    schemaVersion: 2, stem: 'TASK_1_fixture',
    screens: [{ screenName: specData.screen, implFile: join(ws, 'nonexistent', 'BoundScreen.kt') }],
    components: [],
  }))
  const reports = join(ws, 'reports')
  mkdirSync(reports, { recursive: true })
  const r = spawnSync('node', [join(HERE, '..', 'scripts', 'compare-screen-spec.mjs'), 'TASK_1_fixture', '--advisory'], {
    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(ws, 'screens'), FIGMA_REPORTS_DIR: reports, FIGMA_PIPELINE_RUN_ID: 'bindings-test' },
    encoding: 'utf8',
  })
  check('W2-6 e2e: compare-screen-spec consumes the bindings implFile with ZERO --screen-map plumbing', () => {
    assert.match(r.stdout + r.stderr, /BoundScreen\.kt/, `the bound implFile must enter the evidence scope:\n${(r.stdout + r.stderr).slice(0, 1200)}`)
  })

  rmSync(ws, { recursive: true, force: true })
  delete process.env.FIGMA_SPEC_SCREENS_DIR

  // Multi-screen: an explicit bindings implFile remains visible while every unbound screen
  // receives the mapping-required blocker; no filename inference mutates the declaration.
  const mw = mkdtempSync(join(tmpdir(), 'bindings-multi-'))
  const msdir = join(mw, 'screens', 'TASK_1_fixture')
  const mreports = join(mw, 'reports')
  mkdirSync(msdir, { recursive: true }); mkdirSync(mreports, { recursive: true })
  writeScreenIndex(msdir, { Alpha: indexNode('Alpha', '1:1'), Beta: indexNode('Beta', '2:2') })
  const mspec = (screen, nodeId) => ({
    schemaVersion: 2, screen, frameSizeDp: { w: 100, h: 200 }, theme: 'light',
    source: { fileKey: 'FileKey', nodeId }, rootNodeId: `${screen}-root`,
    coordinateSystem: { units: 'dp', density: 1, origin: 'frame' },
    themeMetadata: { themeKey: 'light' },
    nodes: [
      { stableId: `${screen}-root`, figmaNodeId: nodeId, name: screen, role: 'screen', bboxDp: { x: 0, y: 0, w: 100, h: 200 } },
      { stableId: `${screen}-t`, figmaNodeId: `${nodeId}:1`, name: 'T', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] },
    ],
    elements: [{ stableId: `${screen}-t`, figmaNodeId: `${nodeId}:1`, name: 'T', bboxDp: { x: 0, y: 0, w: 10, h: 10 }, fills: ['#FFFFFF'] }],
  })
  writeFileSync(join(msdir, 'Alpha.spec.json'), JSON.stringify(mspec('Alpha', '1:1')))
  writeFileSync(join(msdir, 'Beta.spec.json'), JSON.stringify(mspec('Beta', '2:2')))
  // Alpha is bound to an odd filename; Beta is deliberately left unbound.
  writeFileSync(join(msdir, 'bindings.json'), JSON.stringify({
    schemaVersion: 2, stem: 'TASK_1_fixture',
    screens: [{ screenName: 'Alpha', implFile: join(mw, 'nowhere', 'AlphaWidgetXYZ.kt') }],
    components: [],
  }))
  const mr = spawnSync('node', [join(HERE, '..', 'scripts', 'compare-screen-spec.mjs'), 'TASK_1_fixture', '--advisory'], {
    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(mw, 'screens'), FIGMA_REPORTS_DIR: mreports, FIGMA_PIPELINE_RUN_ID: 'bindings-multi' },
    encoding: 'utf8',
  })
  check('multi-screen keeps the explicit binding and blocks the unbound screen without inference', () => {
    assert.match(mr.stdout + mr.stderr, /AlphaWidgetXYZ\.kt/, `Alpha's bindings implFile must remain explicit:\n${(mr.stdout + mr.stderr).slice(0, 1000)}`)
    assert.match(mr.stdout + mr.stderr, /screen Beta requires an explicit screen-to-implementation mapping/)
  })
  rmSync(mw, { recursive: true, force: true })
} catch (e) {
  fail++; console.log(`${C.red}FAIL${C.reset} bindings setup threw\n     ${e.stack || e.message}`)
}

console.log(`\nbindings.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
