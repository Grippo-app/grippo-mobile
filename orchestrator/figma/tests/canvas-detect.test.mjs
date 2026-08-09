// Fixture self-test for lib/canvas-detect.mjs (the Canvas/DrawScope classifier) and the
// resolveAliases path of extract-app-tokens.mjs. No Figma, no Gradle. Adversarial per-construct
// probes: the false-POSITIVE direction (a synthetic key masking a real mismatch) is what must not
// regress, so most alias cases assert a key is ABSENT. The Kotlin shadow forms (lambda/loop/
// destructuring/param) are the regression pins for the resolver's scope-termination.
import { classifyWidgetSource } from '../scripts/lib/canvas-detect.mjs'
import { extractAppTokensFromText } from '../scripts/extract-app-tokens.mjs'
import assert from 'node:assert/strict'

const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

const tokensOf = (src) => extractAppTokensFromText(src, 'W.kt', { resolveAliases: true }).tokens
const has = (src, key) => Object.prototype.hasOwnProperty.call(tokensOf(src), key)

// ---- classifyWidgetSource ---------------------------------------------------
check('classify: Canvas( is canvas', () => {
  assert.equal(classifyWidgetSource('fun W(){ Canvas(Modifier) { drawRect() } }').canvas, true)
})
check('classify: Modifier.drawBehind is canvas', () => {
  assert.equal(classifyWidgetSource('Box(Modifier.drawBehind {\n drawLine() })').canvas, true)
})
check('classify: DrawScope receiver is canvas', () => {
  assert.equal(classifyWidgetSource('private fun DrawScope.axis() {}').canvas, true)
})
check('classify: plain declarative is NOT canvas', () => {
  assert.equal(classifyWidgetSource('fun W(){ Text("hi"); Box(Modifier.padding(8.dp)) }').canvas, false)
})
check('classify: Canvas in a comment is NOT canvas (masker)', () => {
  assert.equal(classifyWidgetSource('fun W(){ /* uses Canvas() somewhere */ Text("x") }').canvas, false)
})
check('classify: Canvas in a string is NOT canvas (masker)', () => {
  assert.equal(classifyWidgetSource('val label = "Canvas(size)"').canvas, false)
})

// ---- alias resolution: the primary fix (recovers the hoisted token) ---------
check('alias: hoisted val + c.leaf synthesizes the full token key', () => {
  const src = 'fun W(){ val c = AppTokens.colors.group\n Canvas(Modifier){ drawPath(color = c.line) } }'
  assert.equal(has(src, 'colors.group.line'), true)
})
check('alias: OFF by default (opt-in only) — census/drift consumers unchanged', () => {
  const t = extractAppTokensFromText('val c = AppTokens.colors.group\n val x = c.line', 'W.kt').tokens
  assert.equal(Object.prototype.hasOwnProperty.call(t, 'colors.group.line'), false)
  assert.equal(Object.prototype.hasOwnProperty.call(t, 'colors.group'), true) // the hoist RHS still scanned
})
check('alias: kind-only hoist (val c = AppTokens.colors) resolves c.group.leaf', () => {
  assert.equal(has('val c = AppTokens.colors\n val x = c.group.leaf', 'colors.group.leaf'), true)
})
check('alias: dp/typography kinds resolve too', () => {
  assert.equal(has('val d = AppTokens.dp.card\n val x = d.pad', 'dp.card.pad'), true)
  assert.equal(has('val t = AppTokens.typography.body\n val x = t.size', 'typography.body.size'), true)
})
check('alias: a legit use BEFORE a later shadow still resolves', () => {
  const src = 'fun W(){ val c = AppTokens.colors.g\n val top = c.top\n xs.forEach { c -> c.line } }'
  assert.equal(has(src, 'colors.g.top'), true)   // before the shadow
  assert.equal(has(src, 'colors.g.line'), false) // the shadowed use is cut
})

// ---- alias resolution: false-POSITIVE guards (the strictness pins) ----------
check('guard: alias in a comment does not resolve (masker)', () => {
  assert.equal(has('// val c = AppTokens.colors.group\n val x = c.line', 'colors.group.line'), false)
})
check('guard: cross-function same-name alias does not leak across blocks', () => {
  const src = [
    'fun A(){ val c = AppTokens.colors.groupA\n drawPath(color = c.lineA) }',
    'fun B(){ val c = AppTokens.colors.groupB\n drawPath(color = c.lineB) }',
  ].join('\n')
  assert.equal(has(src, 'colors.groupA.lineA'), true)
  assert.equal(has(src, 'colors.groupB.lineB'), true)
  assert.equal(has(src, 'colors.groupA.lineB'), false) // no cross-scope leak
  assert.equal(has(src, 'colors.groupB.lineA'), false)
})
check('guard: nested val re-declaration cuts the outer alias scope', () => {
  const src = 'fun A(){\n val c = AppTokens.colors.g\n run {\n val c = listOf(1)\n val b = c.size\n } }'
  assert.equal(has(src, 'colors.g.size'), false)
})
// The shadow forms that DON'T use `val` — the regression pins for the review finding (a lambda /
// loop / destructuring / fn param that re-binds the alias name must cut the scope, or a shadowed
// `.leaf` fabricates a false token key that masks a real mismatch).
check('guard: lambda-param shadow does NOT synthesize a key (forEach { c -> c.leaf })', () => {
  assert.equal(has('fun W(){ val c = AppTokens.colors.g\n pts.forEach { c -> drawCircle(c.line) } }', 'colors.g.line'), false)
})
check('guard: for-loop var shadow does NOT synthesize a key', () => {
  assert.equal(has('fun W(){ val c = AppTokens.colors.g\n for (c in xs) { use(c.line) } }', 'colors.g.line'), false)
})
check('guard: DESTRUCTURING for-loop shadow does NOT synthesize a key (for ((c,x) in …))', () => {
  assert.equal(has('fun W(){ val c = AppTokens.colors.g\n for ((c, x) in pts) { drawCircle(c.line) } }', 'colors.g.line'), false)
})
check('guard: destructuring-param shadow does NOT synthesize a key', () => {
  assert.equal(has('fun W(){ val c = AppTokens.colors.g\n xs.map { (c, x) -> c.line } }', 'colors.g.line'), false)
})
check('guard: nested-fun param shadow does NOT synthesize a key', () => {
  assert.equal(has('fun W(){ val c = AppTokens.colors.g\n fun draw(c: Seg){ paint(c.line) } }', 'colors.g.line'), false)
})
check('guard: substring name does not false-match (col vs column)', () => {
  assert.equal(has('val col = AppTokens.colors.g\n val w = column.width', 'colors.g.width'), false)
})
check('guard: alias used only outside its block is not resolved', () => {
  assert.equal(has('fun A(){ val c = AppTokens.colors.g }\n val stray = c.foo', 'colors.g.foo'), false)
})

console.log(`\n${fail ? C.red : C.green}canvas-detect.test: ${pass} passed, ${fail} failed${C.reset}`)
process.exit(fail ? 1 : 0)
