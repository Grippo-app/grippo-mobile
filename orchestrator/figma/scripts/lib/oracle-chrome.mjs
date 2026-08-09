// oracle-chrome.mjs — detect + strip iOS device chrome from a pulled oracle spec (R6).
//
// Why this exists: Figma oracles are often iOS exports WITH device chrome — the "9:41"
// status bar (~47dp) and the home indicator (~34dp). The Roborazzi capture renders the
// composable only, so every oracle pixel sits ~47px below its capture counterpart: the
// per-zone comparison matches each zone against a shifted region (element SSIM ≈ 0), the
// ±2px shift search cannot absorb it, and the comparator's SYMMETRIC statusBarDp/navBarDp
// bands cannot fix a one-sided offset. The comparison contract is app CONTENT vs design
// CONTENT — device chrome is not content — so the oracle is normalized ONCE where it enters
// the system (the pull boundary, normalize-oracle.mjs) and every downstream consumer (spec
// gate bboxDp, zone grid, comparator, site three-up) sees one consistent chrome-free oracle.
//
// Doctrine (REVIEW_ROUTING R6): detection is a STRICT deterministic predicate over committed
// data — geometry AND a name/text signal, never geometry alone ("9:41" is Apple's canonical
// marketing time, which never appears as real app copy). Ambiguity keeps the pixels and
// WARNS (IOS_CHROME_SUSPECTED — the owner renames the Figma layer or accepts), never
// silently crops content. Never fabricate chrome onto the capture; never absorb the offset
// into thresholds.
//
// Pure library (canvas-detect pattern): no CLI, no I/O — it transforms spec OBJECTS; the
// normalize-oracle CLI owns the PNG crop and the atomic writes, check-screen-cache reuses
// the same predicate to verify a stamped spec stayed consistent.

// Strict predicate constants (REVIEW_ROUTING R6-1 — do not widen):
const TOP_ANCHOR_DP = 2          // top-anchored: bboxDp.y ≤ 2
const FULL_WIDTH_FRACTION = 0.95 // full-width: ≥ 95% of frameSizeDp.w
const TOP_MAX_HEIGHT_DP = 50     // iOS status bar ≈ 44–47dp
const BOTTOM_MAX_HEIGHT_DP = 40  // home indicator ≈ 34dp
const TOP_NAME_RE = /^(ios[\/\s_-]*)?status\s*bar/i
const BOTTOM_NAME_RE = /home\s*indicator/i
const MARKETING_TIME = '9:41'    // never real app copy — Apple's canonical marketing time
const EPS = 0.5                  // dp tolerance for anchor/containment float noise

const bbox = (el) => (el && el.bboxDp && typeof el.bboxDp === 'object' ? el.bboxDp : null)
const nameOf = (el) => `${el && el.name ? el.name : ''}`
const nameSignal = (el, re) => re.test(nameOf(el)) || re.test(String((el && el.componentSetName) || ''))
const timeSignal = (el) => typeof el?.text === 'string' && el.text.trim() === MARKETING_TIME
const within = (inner, outer) =>
  inner.x >= outer.x - EPS && inner.y >= outer.y - EPS &&
  inner.x + inner.w <= outer.x + outer.w + EPS && inner.y + inner.h <= outer.y + outer.h + EPS

// detectChrome(spec) -> { top, bottom, suspects }
//   top:    { topDp, matched: [...], elements: [names] } | null — the crop band is the matched
//           strip's own bottom edge (max y+h over matched elements)
//   bottom: { bottomDp, matched: ['Home Indicator'], elements: [names] } | null
//   suspects: [{ name, bboxDp }] — TOP strips matching the GEOMETRY but carrying no name/text
//           signal; NOT cropped (fail-closed) — the caller warns IOS_CHROME_SUSPECTED.
export function detectChrome(spec) {
  const fs = spec && spec.frameSizeDp
  const elements = spec && Array.isArray(spec.elements) ? spec.elements : []
  if (!fs || !(fs.w > 0) || !(fs.h > 0)) return { top: null, bottom: null, suspects: [] }
  const topGeometry = (b) => b.y <= TOP_ANCHOR_DP && b.w >= FULL_WIDTH_FRACTION * fs.w && b.h <= TOP_MAX_HEIGHT_DP
  const bottomGeometry = (b) => b.y + b.h >= fs.h - EPS && b.w >= FULL_WIDTH_FRACTION * fs.w && b.h <= BOTTOM_MAX_HEIGHT_DP

  const topMatches = []
  const bottomMatches = []
  const geometryOnly = []
  for (const el of elements) {
    const b = bbox(el)
    if (!b) continue
    if (topGeometry(b)) {
      const byName = nameSignal(el, TOP_NAME_RE)
      const byTime = timeSignal(el) || elements.some((d) => d !== el && timeSignal(d) && bbox(d) && within(bbox(d), b))
      if (byName || byTime) topMatches.push({ el, b, labels: [...(byTime ? [MARKETING_TIME] : []), ...(byName ? ['Status Bar'] : [])] })
      else geometryOnly.push(el)
    }
    if (bottomGeometry(b) && nameSignal(el, BOTTOM_NAME_RE)) bottomMatches.push({ el, b })
  }

  const top = topMatches.length
    ? {
        topDp: Math.max(...topMatches.map((m) => m.b.y + m.b.h)),
        matched: [...new Set(topMatches.flatMap((m) => m.labels))],
        elements: topMatches.map((m) => nameOf(m.el)),
      }
    : null
  const bottom = bottomMatches.length
    ? {
        bottomDp: fs.h - Math.min(...bottomMatches.map((m) => m.b.y)),
        matched: ['Home Indicator'],
        elements: bottomMatches.map((m) => nameOf(m.el)),
      }
    : null
  // A geometry-lookalike already inside a MATCHED top band is cropped with the band — only a
  // strip that would survive the crop is worth the owner's attention.
  const suspects = geometryOnly
    .filter((el) => !top || bbox(el).y + bbox(el).h > top.topDp + EPS)
    .map((el) => ({ name: nameOf(el), bboxDp: bbox(el) }))
  return { top, bottom, suspects }
}

const round4 = (v) => Math.round(v * 10000) / 10000

// applyChromeCrop(spec, { topDp, bottomDp, matched, at }) -> { spec, dropped }
// Pure transform of a spec object (input untouched): drops every element/node fully inside a
// cropped band, shifts remaining bboxDp.y by −topDp, CLAMPS straddlers (an edge-to-edge
// background legitimately spans the whole frame — it survives, trimmed to the new frame, so
// the post-crop gate's no-negative-y invariant holds by construction), shrinks frameSizeDp.h,
// scrubs dropped stableIds from v2 childrenStableIds, and stamps the auditable `chromeCrop`.
export function applyChromeCrop(spec, { topDp = 0, bottomDp = 0, matched = [], at }) {
  const out = structuredClone(spec)
  const oldH = out.frameSizeDp.h
  const newH = round4(oldH - topDp - bottomDp)
  const dropped = []
  const droppedIds = new Set()
  const transform = (arr) => {
    if (!Array.isArray(arr)) return arr
    const kept = []
    for (const item of arr) {
      const b = bbox(item)
      if (!b) { kept.push(item); continue }
      const fullyTop = b.y + b.h <= topDp + EPS
      const fullyBottom = b.y >= oldH - bottomDp - EPS
      if (fullyTop || fullyBottom) {
        dropped.push(nameOf(item) || item.stableId || '(unnamed)')
        if (item.stableId) droppedIds.add(item.stableId)
        continue
      }
      let y = b.y - topDp
      let h = b.h
      if (y < 0) { h = round4(h + y); y = 0 }
      if (y + h > newH) h = round4(newH - y)
      if (h <= 0) {
        dropped.push(nameOf(item) || item.stableId || '(unnamed)')
        if (item.stableId) droppedIds.add(item.stableId)
        continue
      }
      item.bboxDp = { ...b, y: round4(y), h }
      kept.push(item)
    }
    return kept
  }
  out.elements = transform(out.elements)
  if (Array.isArray(out.nodes)) {
    out.nodes = transform(out.nodes)
    for (const node of out.nodes) {
      if (Array.isArray(node.childrenStableIds)) node.childrenStableIds = node.childrenStableIds.filter((id) => !droppedIds.has(id))
    }
  }
  out.frameSizeDp = { ...out.frameSizeDp, h: newH }
  out.chromeCrop = { topDp: round4(topDp), bottomDp: round4(bottomDp), matched, at }
  return { spec: out, dropped }
}

// chromeResidue(spec) — what a STAMPED spec may no longer contain (check-screen-cache gate):
// any element/node still matching the FULL chrome predicate, plus a chrome-named layer or a
// surviving "9:41" text WITHIN a plausible chrome band position (top-anchored within the top
// 50dp, or bottom-anchored within the bottom 40dp of the post-crop frame). The band gate keeps
// legitimate CONTENT out of residue — a mid-screen alarm-app "9:41", or a nested device-mockup
// layer named "Home Indicator", is content, not an inconsistent crop; only chrome signals in
// chrome POSITIONS mean the stamped crop must be redone (remedy: re-pull — normalize-oracle
// re-runs on the fresh pair; a stamped spec itself is skipped by design).
export function chromeResidue(spec) {
  const residue = []
  const { top, bottom, suspects } = detectChrome(spec)
  if (top) residue.push(...top.elements.map((name) => ({ name, reason: 'top chrome predicate still matches' })))
  if (bottom) residue.push(...bottom.elements.map((name) => ({ name, reason: 'bottom chrome predicate still matches' })))
  void suspects // geometry-only strips are the pull-time WARN's job, not residue
  const frameH = spec && spec.frameSizeDp && spec.frameSizeDp.h > 0 ? spec.frameSizeDp.h : null
  const inChromeBand = (el) => {
    const b = bbox(el)
    if (!b || frameH == null) return true   // no geometry to prove it is content → stay strict
    return b.y <= TOP_MAX_HEIGHT_DP || b.y + b.h >= frameH - BOTTOM_MAX_HEIGHT_DP
  }
  const lists = [spec && spec.elements, spec && spec.nodes].filter(Array.isArray)
  for (const arr of lists) {
    for (const el of arr) {
      if (!inChromeBand(el)) continue
      if (timeSignal(el)) residue.push({ name: nameOf(el) || '(unnamed)', reason: `text "${MARKETING_TIME}" survived the crop in a chrome band position` })
      else if (nameSignal(el, TOP_NAME_RE) || nameSignal(el, BOTTOM_NAME_RE)) residue.push({ name: nameOf(el), reason: 'chrome-named layer survived the crop in a chrome band position' })
    }
  }
  const seen = new Set()
  return residue.filter((r) => { const k = `${r.name}|${r.reason}`; if (seen.has(k)) return false; seen.add(k); return true })
}
