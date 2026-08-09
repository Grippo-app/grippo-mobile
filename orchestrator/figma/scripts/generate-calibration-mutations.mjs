#!/usr/bin/env node
// generate-calibration-mutations.mjs — synthetic calibration pairs with ground
// truth BY CONSTRUCTION.
//
// Reads a recipes file (default: tests/calibration/recipes.json —
// a seed screen layout + a mutation list), renders each case's oracle PNG from
// the seed and its capture PNG from the mutated seed, and writes a
// calibration-labels.schema.json-shaped labels.json next to them:
//
//   <out>/
//     pairs/<Case>.png            oracle (Figma-side stand-in)
//     pairs/<Case>Screenshot.png  capture (render-side stand-in)
//     labels.json                 { labels: [{screen, expect, expectBand, …}] }
//
// Consumers: tests/calibration-verdict.test.mjs (drives compare-screenshots
// over every pair and pins the bands — the template's threshold regression pin)
// and calibrate-thresholds.mjs --corpus (sweeps candidate thresholds over the
// same reports). No Figma is ever called; jimp is the sidecar's existing devDep.
//
// Usage: node scripts/generate-calibration-mutations.mjs --out <dir> [--recipes <file>]

import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { parseCli } from './_util.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const cli = parseCli({
  allowedFlags: ['--out', '--recipes'],
  valueFlags: ['--out', '--recipes'],
  usage: 'usage: node scripts/generate-calibration-mutations.mjs --out <dir> [--recipes <file>]',
})
const outDir = cli.value('--out')
if (!outDir) { console.error('ERROR: --out <dir> is required'); process.exit(1) }
const recipesPath = cli.value('--recipes') || join(HERE, '..', 'tests', 'calibration', 'recipes.json')

let recipes
try { recipes = JSON.parse(readFileSync(recipesPath, 'utf8')) } catch (e) {
  console.error(`ERROR: recipes unreadable (${recipesPath}): ${e.message}`); process.exit(1)
}
const seed = recipes.seed
const mutations = Array.isArray(recipes.mutations) ? recipes.mutations : []
if (!seed || !Array.isArray(seed.elements) || !mutations.length) {
  console.error(`ERROR: recipes must carry { seed: { w, h, bg, elements[] }, mutations[] } (${recipesPath})`); process.exit(1)
}

let Jimp
try { ({ Jimp } = await import('jimp')) } catch {
  console.error('ERROR: jimp not installed — run root `npm ci` first'); process.exit(1)
}

const clamp = (v) => Math.max(0, Math.min(255, Math.round(v)))

function render({ w, h, bg, elements }, { tweak = false } = {}) {
  const img = new Jimp({ width: w, height: h, color: ((bg[0] << 24) | (bg[1] << 16) | (bg[2] << 8) | 0xff) >>> 0 })
  const data = img.bitmap.data
  const fill = ([x0, y0, x1, y1], [r, g, b], stripes = 0) => {
    for (let y = Math.max(0, y0 | 0); y < Math.min(h, y1 | 0); y++) {
      // stripes: N-px rows of the element colour alternating with N-px rows of
      // background — high-frequency content that makes sub-cell misalignment
      // visible to SSIM (coarse solid rects forgive small shifts).
      if (stripes > 0 && Math.floor((y - y0) / stripes) % 2 === 1) continue
      for (let x = Math.max(0, x0 | 0); x < Math.min(w, x1 | 0); x++)
        data.set([r, g, b, 255], (y * w + x) << 2)
    }
  }
  for (const el of elements) fill(el.rect, el.color, el.stripes || 0)
  // Byte-distinct-but-visually-nil tweak for identity captures: the real gate
  // rejects a byte-identical capture as CAPTURE_IS_ORACLE_COPY (a copied oracle
  // certifies nothing), exactly like a real renderer's output would differ.
  if (tweak) data[2] ^= 1
  return img
}

function mutate(seedSpec, m) {
  const s = { ...seedSpec, elements: seedSpec.elements.map((e) => ({ ...e, rect: [...e.rect], color: [...e.color] })) }
  switch (m.op) {
    case 'none':
      return s
    case 'shift':
      for (const e of s.elements) e.rect = [e.rect[0] + m.dx, e.rect[1] + m.dy, e.rect[2] + m.dx, e.rect[3] + m.dy]
      return s
    case 'remove':
      s.elements = s.elements.filter((e) => e.name !== m.element)
      return s
    case 'move': {
      const el = s.elements.find((e) => e.name === m.element)
      if (!el) { console.error(`ERROR: mutation ${m.name} names unknown element ${m.element}`); process.exit(1) }
      el.rect = [el.rect[0] + m.dx, el.rect[1] + m.dy, el.rect[2] + m.dx, el.rect[3] + m.dy]
      return s
    }
    case 'dim': {
      const el = s.elements.find((e) => e.name === m.element)
      if (!el) { console.error(`ERROR: mutation ${m.name} names unknown element ${m.element}`); process.exit(1) }
      el.color = el.color.map((c) => clamp(c * m.factor))
      return s
    }
    case 'tint': {
      const el = s.elements.find((e) => e.name === m.element)
      if (!el) { console.error(`ERROR: mutation ${m.name} names unknown element ${m.element}`); process.exit(1) }
      el.color = el.color.map((c, i) => clamp(c + (m.delta[i] || 0)))
      return s
    }
    case 'extra':
      s.elements = [...s.elements, { name: `extra-${m.name}`, rect: m.rect, color: m.color }]
      return s
    case 'blur':
      // Pixel-space post-op (applied after render below) — the DIFFUSE
      // whole-frame degradation that must be decided by the severity BANDS
      // (global mean SSIM), not by any zone floor: the band-weakening pin.
      return s
    default:
      console.error(`ERROR: unknown mutation op ${JSON.stringify(m.op)} (${m.name})`); process.exit(1)
  }
}

const pairsDir = join(outDir, 'pairs')
mkdirSync(pairsDir, { recursive: true })
const labels = []
for (const m of mutations) {
  if (!m.name || !/^[A-Za-z][A-Za-z0-9]*$/.test(m.name)) {
    console.error(`ERROR: mutation name must be a bare CamelCase identifier (it becomes the screen name), got ${JSON.stringify(m.name)}`); process.exit(1)
  }
  const oracle = render(seed)
  const capture = render(mutate(seed, m), { tweak: m.op === 'none' })
  if (m.op === 'blur') for (let i = 0; i < (m.passes || 1); i++) capture.blur(m.radius)
  await oracle.write(join(pairsDir, `${m.name}.png`))
  await capture.write(join(pairsDir, `${m.name}Screenshot.png`))
  const label = { screen: m.name, theme: null, expect: m.expect, expectBand: m.expectBand, note: m.note || '', source: `synthetic:${m.op}` }
  if (m.expectIssue) label.expectIssue = m.expectIssue
  labels.push(label)
}
writeFileSync(join(outDir, 'labels.json'), JSON.stringify({ labels }, null, 2) + '\n')
console.log(`generated ${labels.length} calibration pair(s) -> ${pairsDir} (+ labels.json)`)
