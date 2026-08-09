// normalize-oracle.test.mjs — pins for the R6 pull-boundary chrome crop: a "9:41" status bar +
// home indicator are cropped from PNG (exact band px at the export scale) and spec (elements
// dropped, bboxDp shifted by −topDp, straddling background CLAMPED, frameSizeDp.h shrunk,
// chromeCrop stamped with the matched signals); a second run is a byte-identical no-op; a
// geometry-only lookalike (no name/text signal) is NOT cropped and warns IOS_CHROME_SUSPECTED;
// a chrome-free spec is untouched with no stamp; --dry-run writes nothing; the dark pair
// normalizes identically; the pure transform scrubs dropped stableIds from v2
// childrenStableIds and the strict predicate's containment/geometry rules hold.
import { createHash } from 'node:crypto'
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync, existsSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { detectChrome, applyChromeCrop, chromeResidue } from '../scripts/lib/oracle-chrome.mjs'
import { bytesHash } from '../runtime/canonical-json.mjs'
import { sourceIdentity, validObservedCapture } from './observed-token-fixtures.mjs'

const { Jimp } = await import('jimp')
const HERE = dirname(fileURLToPath(import.meta.url))
const SCRIPT = join(HERE, '..', 'scripts', 'normalize-oracle.mjs')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }
const sha = (p) => createHash('sha256').update(readFileSync(p)).digest('hex')

// --- pure-lib pins -----------------------------------------------------------

const chromeSpec = (screen = 'Home') => ({
  screen,
  frameSizeDp: { w: 390, h: 891 },
  theme: 'dark',
  elements: [
    { stableId: 'bg', name: 'Background', bboxDp: { x: 0, y: 0, w: 390, h: 891 } },
    { stableId: 'sb', name: 'iOS/Status Bar', bboxDp: { x: 0, y: 0, w: 390, h: 47 } },
    { stableId: 'time', name: 'Time', bboxDp: { x: 20, y: 12, w: 40, h: 20 }, text: '9:41' },
    { stableId: 'title', name: 'Title', bboxDp: { x: 16, y: 100, w: 200, h: 30 }, text: 'Діагностика' },
    { stableId: 'hi', name: 'Home Indicator', bboxDp: { x: 0, y: 857, w: 390, h: 34 } },
  ],
})

check('detectChrome: named status bar + contained "9:41" + home indicator match; labels recorded', () => {
  const { top, bottom, suspects } = detectChrome(chromeSpec())
  assert.ok(top && bottom)
  assert.equal(top.topDp, 47)
  assert.equal(bottom.bottomDp, 34)
  assert.deepEqual([...top.matched].sort(), ['9:41', 'Status Bar'])
  assert.deepEqual(bottom.matched, ['Home Indicator'])
  assert.deepEqual(suspects, [])
})

check('detectChrome: an UN-NAMED full-width strip whose child carries "9:41" still matches (descendant signal)', () => {
  const s = chromeSpec()
  s.elements.find((e) => e.stableId === 'sb').name = 'Frame 123'
  const { top } = detectChrome(s)
  assert.ok(top)
  assert.deepEqual(top.matched, ['9:41'])
})

check('detectChrome: geometry-only lookalike (no name/text signal) → suspect, never a match', () => {
  const s = {
    screen: 'Home', frameSizeDp: { w: 390, h: 891 }, theme: 'dark',
    elements: [{ stableId: 'hdr', name: 'Header', bboxDp: { x: 0, y: 0, w: 390, h: 44 } }],
  }
  const { top, suspects } = detectChrome(s)
  assert.equal(top, null)
  assert.deepEqual(suspects.map((x) => x.name), ['Header'])
})

check('detectChrome: a "9:41" OUTSIDE the strip does not certify it; small/short/anchored-off strips never match', () => {
  const base = { screen: 'H', frameSizeDp: { w: 390, h: 891 }, theme: 'dark' }
  // 9:41 far below the candidate strip → no containment signal
  const s1 = { ...base, elements: [
    { stableId: 'a', name: 'Frame', bboxDp: { x: 0, y: 0, w: 390, h: 44 } },
    { stableId: 'b', name: 'Body', bboxDp: { x: 10, y: 400, w: 60, h: 20 }, text: '9:41' },
  ] }
  assert.equal(detectChrome(s1).top, null)
  // not full-width / too tall / not top-anchored
  for (const bbox of [{ x: 0, y: 0, w: 200, h: 44 }, { x: 0, y: 0, w: 390, h: 80 }, { x: 0, y: 10, w: 390, h: 44 }]) {
    const s = { ...base, elements: [{ stableId: 'x', name: 'Status Bar', bboxDp: bbox }] }
    assert.equal(detectChrome(s).top, null, JSON.stringify(bbox))
  }
})

check('applyChromeCrop: drops band elements, shifts/clamps the rest, shrinks the frame, stamps', () => {
  const { spec: out, dropped } = applyChromeCrop(chromeSpec(), { topDp: 47, bottomDp: 34, matched: ['9:41', 'Status Bar', 'Home Indicator'], at: 'T' })
  assert.equal(out.frameSizeDp.h, 810)
  assert.deepEqual(out.elements.map((e) => e.stableId).sort(), ['bg', 'title'])
  const bg = out.elements.find((e) => e.stableId === 'bg')
  assert.deepEqual(bg.bboxDp, { x: 0, y: 0, w: 390, h: 810 })          // straddler clamped, never negative
  const title = out.elements.find((e) => e.stableId === 'title')
  assert.equal(title.bboxDp.y, 53)                                      // shifted by −topDp
  assert.deepEqual(out.chromeCrop, { topDp: 47, bottomDp: 34, matched: ['9:41', 'Status Bar', 'Home Indicator'], at: 'T' })
  assert.deepEqual(dropped.sort(), ['Home Indicator', 'Time', 'iOS/Status Bar'])
  assert.equal(chromeResidue(out).length, 0, 'a clean crop leaves no residue')
})

check('applyChromeCrop: v2 nodes[] transformed identically + dropped stableIds scrubbed from childrenStableIds', () => {
  const s = chromeSpec()
  s.nodes = [
    { stableId: 'root', name: 'Frame', bboxDp: { x: 0, y: 0, w: 390, h: 891 }, childrenStableIds: ['sb', 'title', 'hi'] },
    { stableId: 'sb', name: 'iOS/Status Bar', bboxDp: { x: 0, y: 0, w: 390, h: 47 }, parentStableId: 'root' },
    { stableId: 'title', name: 'Title', bboxDp: { x: 16, y: 100, w: 200, h: 30 }, parentStableId: 'root' },
    { stableId: 'hi', name: 'Home Indicator', bboxDp: { x: 0, y: 857, w: 390, h: 34 }, parentStableId: 'root' },
  ]
  const { spec: out } = applyChromeCrop(s, { topDp: 47, bottomDp: 34, matched: [], at: 'T' })
  assert.deepEqual(out.nodes.map((n) => n.stableId).sort(), ['root', 'title'])
  assert.deepEqual(out.nodes.find((n) => n.stableId === 'root').childrenStableIds, ['title'])
  assert.equal(out.nodes.find((n) => n.stableId === 'title').bboxDp.y, 53)
})

check('chromeResidue: surviving "9:41" text or chrome-named layer IN A CHROME BAND POSITION is residue', () => {
  const out = applyChromeCrop(chromeSpec(), { topDp: 47, bottomDp: 34, matched: [], at: 'T' }).spec
  out.elements.push({ stableId: 'ghost', name: 'Clock', bboxDp: { x: 10, y: 5, w: 40, h: 20 }, text: '9:41' })
  assert.ok(chromeResidue(out).some((r) => /9:41/.test(r.reason)))
})

check('chromeResidue: mid-frame "9:41" content and a mid-frame chrome-named mockup layer are EXEMPT (content, not residue)', () => {
  const out = applyChromeCrop(chromeSpec(), { topDp: 47, bottomDp: 34, matched: [], at: 'T' }).spec
  out.elements.push({ stableId: 'alarm', name: 'AlarmTime', bboxDp: { x: 100, y: 400, w: 190, h: 60 }, text: '9:41' })       // alarm-app content
  out.elements.push({ stableId: 'mock', name: 'Home Indicator', bboxDp: { x: 150, y: 400, w: 90, h: 5 } })                   // nested device mockup
  assert.equal(chromeResidue(out).length, 0, JSON.stringify(chromeResidue(out)))
})

// --- CLI pins ----------------------------------------------------------------

const SCALE = 3   // export scale: dp → px (png 1170×2673 for a 390×891 frame)
async function png(path, wDp, hDp) {
  const img = new Jimp({ width: wDp * SCALE, height: hDp * SCALE, color: 0x101010ff })
  writeFileSync(path, await img.getBuffer('image/png'))
}

async function ws({ dark = false, spec = chromeSpec(), kind = 'screen' } = {}) {
  const dir = mkdtempSync(join(tmpdir(), 'norm-oracle-'))
  const sdir = join(dir, 'screens', 'TASK_1_fixture')
  const reports = join(dir, 'reports')
  mkdirSync(sdir, { recursive: true }); mkdirSync(reports, { recursive: true })
  const fetchedAt = '2026-01-01T00:00:00.000Z'
  const url = (nodeId) => `https://www.figma.com/design/FileKey?node-id=${nodeId.replace(':', '-')}`
  const primary = { id: 'primary', theme: 'light', locale: 'default', platform: 'shared', url: url('1:2'), nodeId: '1:2', fetchedAt, imageFile: 'Home.png', specFile: 'Home.spec.json' }
  const node = { kind, url: primary.url, nodeId: primary.nodeId, fetchedAt, variants: [primary] }
  if (dark) {
    const variant = { id: 'dark', theme: 'dark', locale: 'default', platform: 'shared', url: url('1:3'), nodeId: '1:3', fetchedAt, imageFile: 'Home.dark.png', specFile: 'Home.dark.spec.json' }
    Object.assign(node, { darkUrl: variant.url, darkNodeId: variant.nodeId, darkFetchedAt: fetchedAt })
    node.variants.push(variant)
  }
  for (const [ordinal, variant] of node.variants.entries()) {
    const capture = validObservedCapture({
      source: sourceIdentity({
        nodeId: variant.nodeId,
        context: { theme: variant.theme, locale: variant.locale, platform: variant.platform },
        origin: { kind: 'task-screen', taskStem: 'TASK_1_fixture', screenKey: 'Home', variantId: variant.id },
      }),
      captureOperationId: `tokop_${String(ordinal + 1).padStart(16, '0')}`,
      captureSequence: 1,
    })
    const tokenBytes = Buffer.from(JSON.stringify(capture, null, 2) + '\n')
    variant.tokensFile = `Home.${variant.id}.tokens.json`
    variant.tokensHash = bytesHash(tokenBytes)
    variant.captureOperationId = capture.captureOperationId
    variant.captureSequence = capture.captureSequence
    writeFileSync(join(sdir, variant.tokensFile), tokenBytes)
  }
  writeFileSync(join(sdir, 'index.json'), JSON.stringify({ schemaVersion: 3, taskStem: 'TASK_1_fixture', nodes: { Home: node } }, null, 2) + '\n')
  writeFileSync(join(sdir, 'Home.spec.json'), JSON.stringify(spec, null, 2) + '\n')
  await png(join(sdir, 'Home.png'), spec.frameSizeDp.w, spec.frameSizeDp.h)
  if (dark) {
    writeFileSync(join(sdir, 'Home.dark.spec.json'), JSON.stringify({ ...spec, theme: 'dark' }, null, 2) + '\n')
    await png(join(sdir, 'Home.dark.png'), spec.frameSizeDp.w, spec.frameSizeDp.h)
  }
  return { dir, sdir, reports }
}
function run(w, args = []) {
  const r = spawnSync('node', [SCRIPT, 'TASK_1_fixture', ...args], {
    env: { ...process.env, FIGMA_SPEC_SCREENS_DIR: join(w.dir, 'screens'), FIGMA_REPORTS_DIR: w.reports },
    encoding: 'utf8',
  })
  return { status: r.status, out: r.stdout + r.stderr }
}
const report = (w) => JSON.parse(readFileSync(join(w.reports, 'normalize-oracle-TASK_1_fixture.json'), 'utf8'))
const pngDims = (p) => { const b = readFileSync(p); return { w: b.readUInt32BE(16), h: b.readUInt32BE(20) } }

try {
  // 1. Full chrome fixture: PNG cropped by exactly the band px, spec transformed + stamped.
  const w1 = await ws({ dark: true })
  const r1 = run(w1)
  check('CLI: crops PNG by exactly topPx/bottomPx at the export scale (both themes)', () => {
    assert.equal(r1.status, 0)
    for (const name of ['Home.png', 'Home.dark.png']) {
      const d = pngDims(join(w1.sdir, name))
      assert.deepEqual(d, { w: 390 * SCALE, h: (891 - 47 - 34) * SCALE }, name)
    }
  })
  check('CLI: spec elements shifted/dropped, frame shrunk, chromeCrop stamped (both themes)', () => {
    for (const name of ['Home.spec.json', 'Home.dark.spec.json']) {
      const s = JSON.parse(readFileSync(join(w1.sdir, name), 'utf8'))
      assert.equal(s.frameSizeDp.h, 810, name)
      assert.deepEqual(s.elements.map((e) => e.stableId).sort(), ['bg', 'title'], name)
      assert.equal(s.elements.find((e) => e.stableId === 'title').bboxDp.y, 53, name)
      assert.deepEqual([...s.chromeCrop.matched].sort(), ['9:41', 'Home Indicator', 'Status Bar'], name)
      assert.equal(typeof s.chromeCrop.at, 'string')
    }
    assert.equal(report(w1).normalized.length, 2)
  })
  check('CLI: second run is a byte-identical no-op (stamp short-circuits)', () => {
    const files = ['Home.spec.json', 'Home.png', 'Home.dark.spec.json', 'Home.dark.png'].map((n) => join(w1.sdir, n))
    const before = files.map(sha)
    const r = run(w1)
    assert.equal(r.status, 0)
    assert.deepEqual(files.map(sha), before)
    assert.equal(report(w1).normalized.length, 0)
    assert.equal(report(w1).stamped.length, 2)
  })
  rmSync(w1.dir, { recursive: true, force: true })

  // 2. Geometry-only lookalike: untouched + IOS_CHROME_SUSPECTED warn.
  const suspectSpec = {
    screen: 'Home', frameSizeDp: { w: 390, h: 891 }, theme: 'dark',
    elements: [
      { stableId: 'hdr', name: 'Header', bboxDp: { x: 0, y: 0, w: 390, h: 44 } },
      { stableId: 'title', name: 'Title', bboxDp: { x: 16, y: 100, w: 200, h: 30 }, text: 'Hello' },
    ],
  }
  const w2 = await ws({ spec: suspectSpec })
  const before2 = [join(w2.sdir, 'Home.spec.json'), join(w2.sdir, 'Home.png')].map(sha)
  const r2 = run(w2)
  check('CLI: geometry-only lookalike is NOT cropped (fail-closed) + IOS_CHROME_SUSPECTED names it', () => {
    assert.equal(r2.status, 0)
    assert.deepEqual([join(w2.sdir, 'Home.spec.json'), join(w2.sdir, 'Home.png')].map(sha), before2)
    const hit = report(w2).issues.find((i) => i.issueKind === 'IOS_CHROME_SUSPECTED')
    assert.ok(hit && hit.severity === 'WARN')
    assert.match(hit.message, /'Header'/)
    assert.match(hit.message, /rename the layer/i)
  })
  rmSync(w2.dir, { recursive: true, force: true })

  // 3. Chrome-free spec: untouched, no stamp, clean report.
  const cleanSpec = { screen: 'Home', frameSizeDp: { w: 390, h: 891 }, theme: 'dark', elements: [{ stableId: 'title', name: 'Title', bboxDp: { x: 16, y: 100, w: 200, h: 30 } }] }
  const w3 = await ws({ spec: cleanSpec })
  const before3 = sha(join(w3.sdir, 'Home.spec.json'))
  const r3 = run(w3)
  check('CLI: chrome-free spec untouched — no stamp, no issues', () => {
    assert.equal(r3.status, 0)
    assert.equal(sha(join(w3.sdir, 'Home.spec.json')), before3)
    assert.ok(!JSON.parse(readFileSync(join(w3.sdir, 'Home.spec.json'), 'utf8')).chromeCrop)
    assert.equal(report(w3).issues.length, 0)
    assert.deepEqual(report(w3).untouched, ['Home.spec.json'])
  })
  rmSync(w3.dir, { recursive: true, force: true })

  // 3b. Device chrome belongs only to a full screen. A dialog/component can legitimately
  // contain a row named "Status Bar" or the text "9:41"; normalization must not destroy it.
  const w3b = await ws({ kind: 'dialog' })
  const before3b = [join(w3b.sdir, 'Home.spec.json'), join(w3b.sdir, 'Home.png')].map(sha)
  const r3b = run(w3b)
  check('CLI: non-screen oracle is never device-chrome-cropped', () => {
    assert.equal(r3b.status, 0)
    assert.deepEqual([join(w3b.sdir, 'Home.spec.json'), join(w3b.sdir, 'Home.png')].map(sha), before3b)
    assert.ok(!JSON.parse(readFileSync(join(w3b.sdir, 'Home.spec.json'), 'utf8')).chromeCrop)
    assert.deepEqual(report(w3b).untouched, ['Home.spec.json'])
  })
  rmSync(w3b.dir, { recursive: true, force: true })

  // 4. --dry-run prints the plan, writes nothing (no report either).
  const w4 = await ws()
  const before4 = [join(w4.sdir, 'Home.spec.json'), join(w4.sdir, 'Home.png')].map(sha)
  const r4 = run(w4, ['--dry-run'])
  check('CLI: --dry-run prints the crop plan and writes NOTHING', () => {
    assert.equal(r4.status, 0)
    assert.match(r4.out, /DRY-RUN would crop/)
    assert.match(r4.out, /top 47dp \(141px\)/)
    assert.deepEqual([join(w4.sdir, 'Home.spec.json'), join(w4.sdir, 'Home.png')].map(sha), before4)
    assert.ok(!existsSync(join(w4.reports, 'normalize-oracle-TASK_1_fixture.json')))
  })
  rmSync(w4.dir, { recursive: true, force: true })

  // 5. Torn-state net: a cropped PNG beside a still-chromed spec (crash between the two
  //    writes) must be REFUSED, never re-cropped — a scale re-derived from the already-cropped
  //    PNG would destroy real content and stamp the corruption permanent.
  const w5 = await ws()
  run(w5)                                                          // normal crop: PNG now 2430px
  const tornSpec = chromeSpec()                                    // regress the spec to its pre-crop (chromed) state
  writeFileSync(join(w5.sdir, 'Home.spec.json'), JSON.stringify(tornSpec, null, 2) + '\n')
  const tornPng = sha(join(w5.sdir, 'Home.png'))
  const r5 = run(w5)
  check('CLI: torn pair (cropped PNG + chromed spec) → CHROME_PAIR_INCONSISTENT, crop REFUSED, no double-crop', () => {
    assert.equal(r5.status, 0)
    const hit = report(w5).issues.find((i) => i.issueKind === 'CHROME_PAIR_INCONSISTENT')
    assert.ok(hit && hit.severity === 'WARN', JSON.stringify(report(w5).issues))
    assert.match(hit.message, /Re-pull/)
    assert.equal(sha(join(w5.sdir, 'Home.png')), tornPng, 'PNG must NOT be cropped again')
    assert.ok(!JSON.parse(readFileSync(join(w5.sdir, 'Home.spec.json'), 'utf8')).chromeCrop, 'spec must NOT be stamped')
    assert.deepEqual(pngDims(join(w5.sdir, 'Home.png')), { w: 390 * SCALE, h: 810 * SCALE }, 'dimensions unchanged from the single legitimate crop')
  })
  rmSync(w5.dir, { recursive: true, force: true })
} catch (e) {
  fail++; console.log(`${C.red}FAIL${C.reset} normalize-oracle setup threw\n     ${e.stack || e.message}`)
}

console.log(`\nnormalize-oracle.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
