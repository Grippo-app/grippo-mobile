import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

const SITE = fileURLToPath(new URL('../', import.meta.url))
const baseCss = readFileSync(join(SITE, 'styles', 'base.css'), 'utf8')
const componentsCss = readFileSync(join(SITE, 'styles', 'components.css'), 'utf8')
const panelsCss = readFileSync(join(SITE, 'styles', 'panels.css'), 'utf8')

function layer(name) {
  const match = baseCss.match(new RegExp(`--layer-${name}:\\s*(\\d+)`))
  assert.ok(match, `--layer-${name} must be declared`)
  return Number(match[1])
}

test('global overlay layers keep transient feedback above every in-page popup', () => {
  assert.ok(layer('status') < layer('popover'))
  assert.ok(layer('popover') < layer('modal'))
  assert.ok(layer('modal') < layer('terminal'))
  assert.ok(layer('terminal') < layer('toast'))

  assert.match(baseCss, /\.status-strip\s*\{[\s\S]*?z-index:\s*var\(--layer-status\)/)
  assert.match(panelsCss, /\.site-status-pop\s*\{[\s\S]*?z-index:\s*var\(--layer-popover\)/)
  assert.match(panelsCss, /\.board-modal\s*\{[\s\S]*?z-index:\s*var\(--layer-modal\)/)
  assert.match(panelsCss, /\.terminal\s*\{[\s\S]*?z-index:\s*var\(--layer-terminal\)/)
  assert.match(componentsCss, /#toast-region\s*\{[\s\S]*?position:\s*fixed[\s\S]*?pointer-events:\s*none[\s\S]*?z-index:\s*var\(--layer-toast\)/)
  assert.match(componentsCss, /\.toast\s*\{[\s\S]*?z-index:\s*var\(--layer-toast\)/)
})

test('mobile status controls stay within the viewport without clipping their popovers', () => {
  const mobile = baseCss.match(/@media \(max-width: 640px\) \{([\s\S]*?)\n\}/)
  assert.ok(mobile, 'the status strip must define a phone breakpoint')
  assert.match(mobile[1], /\.status-strip\s*\{[\s\S]*?max-width:\s*100vw;/)
  assert.match(mobile[1], /\.status-strip \.site-status-label\s*\{\s*display:\s*none;/)
  assert.match(mobile[1], /\.status-strip \.site-status-pop\s*\{[\s\S]*?position:\s*fixed;[\s\S]*?left:\s*8px;[\s\S]*?right:\s*8px;/)
})

class FakeClassList {
  constructor() { this.values = new Set() }
  add(value) { this.values.add(value) }
  remove(value) { this.values.delete(value) }
  contains(value) { return this.values.has(value) }
}

class FakeElement {
  constructor(tagName) {
    this.tagName = tagName.toUpperCase()
    this.attributes = new Map()
    this.children = []
    this.classList = new FakeClassList()
    this.className = ''
    this.parentNode = null
    this.textContent = ''
  }
  appendChild(child) {
    if (child.parentNode) child.parentNode.removeChild(child)
    this.children.push(child)
    child.parentNode = this
    return child
  }
  removeChild(child) {
    const index = this.children.indexOf(child)
    if (index >= 0) this.children.splice(index, 1)
    child.parentNode = null
    return child
  }
  setAttribute(name, value) { this.attributes.set(name, String(value)) }
  getAttribute(name) { return this.attributes.get(name) ?? null }
  get offsetWidth() { return 0 }
  set innerHTML(value) {
    assert.equal(value, '')
    for (const child of this.children) child.parentNode = null
    this.children = []
  }
}

// Mirrors clipboard.js: hold = clamp(length * 70ms, 3000ms, 10000ms), fade 250ms.
const SHORT_HOLD_MS = 3000
const FADE_MS = 250

test('toast runtime uses the live region normally and the native-dialog top layer when needed', async () => {
  const originalDocument = globalThis.document
  const originalSetTimeout = globalThis.setTimeout
  const originalClearTimeout = globalThis.clearTimeout
  const timers = new Map()
  let timerId = 0
  const region = new FakeElement('div')
  const dialog = new FakeElement('dialog')
  const nestedDialog = new FakeElement('dialog')
  let openDialogs = []

  globalThis.document = {
    createElement: (tagName) => new FakeElement(tagName),
    getElementById: (id) => id === 'toast-region' ? region : null,
    querySelectorAll: (selector) => selector === 'dialog[open]' ? openDialogs : [],
  }
  globalThis.setTimeout = (callback, delay) => {
    const id = ++timerId
    timers.set(id, { callback, delay })
    return id
  }
  globalThis.clearTimeout = (id) => { timers.delete(id) }

  function runTimer(delay) {
    const entry = [...timers.entries()].find(([, timer]) => timer.delay === delay)
    assert.ok(entry, `expected a ${delay}ms timer`)
    timers.delete(entry[0])
    entry[1].callback()
  }
  function pendingHold() {
    const holds = [...timers.values()].filter((timer) => timer.delay !== FADE_MS)
    assert.equal(holds.length, 1, 'exactly one hold timer may be pending')
    return holds[0].delay
  }

  try {
    const { clipboard } = await import(`../scripts/clipboard.js?toast-layering=${Date.now()}`)

    clipboard.toast('body toast')
    assert.equal(region.children.length, 1)
    const bodyToast = region.children[0]
    assert.equal(bodyToast.textContent, 'body toast')
    assert.equal(bodyToast.getAttribute('role'), 'status')
    assert.equal(bodyToast.getAttribute('aria-atomic'), 'true')
    assert.equal(bodyToast.classList.contains('toast--visible'), true)

    openDialogs = [dialog, nestedDialog]
    clipboard.toast('dialog toast')
    assert.equal(bodyToast.parentNode, null, 'a rapid replacement removes the old toast from its prior host')
    assert.equal(region.children.length, 0)
    assert.equal(dialog.children.length, 0)
    assert.equal(nestedDialog.children.length, 1, 'the last open dialog owns feedback for a nested modal')
    const dialogToast = nestedDialog.children[0]
    assert.equal(dialogToast.textContent, 'dialog toast')
    assert.equal(dialogToast.getAttribute('role'), 'status')

    runTimer(SHORT_HOLD_MS)
    assert.equal(dialogToast.classList.contains('toast--visible'), false)
    clipboard.toast('latest dialog toast')
    assert.equal(dialogToast.parentNode, null)
    assert.equal(nestedDialog.children.length, 1, 'rapid toasts must not accumulate')
    const latestToast = nestedDialog.children[0]
    assert.equal(latestToast.textContent, 'latest dialog toast')
    assert.equal(timers.size, 2, 'the old fade cleanup and latest hide timer are both pending')

    runTimer(FADE_MS)
    assert.equal(latestToast.parentNode, nestedDialog, 'a stale fade cleanup must not remove the latest toast')
    assert.equal(timers.size, 1)

    clipboard.toast('rapid replacement')
    assert.equal(latestToast.parentNode, null)
    assert.equal(nestedDialog.children.length, 1)
    const replacementToast = nestedDialog.children[0]
    assert.equal(replacementToast.textContent, 'rapid replacement')
    assert.equal(timers.size, 1, 'the superseded hide timer must be canceled')

    runTimer(SHORT_HOLD_MS)
    assert.equal(replacementToast.classList.contains('toast--visible'), false)
    runTimer(FADE_MS)
    assert.equal(replacementToast.parentNode, null)
    assert.equal(dialog.children.length, 0)
    assert.equal(nestedDialog.children.length, 0)

    // A message the user cannot finish reading is a message that was swallowed:
    // the hold scales with length, clamped so nothing parks on screen forever.
    openDialogs = []
    clipboard.toast('x'.repeat(10))
    assert.equal(pendingHold(), SHORT_HOLD_MS, 'short messages keep the readable floor')
    clipboard.toast('x'.repeat(120))
    assert.equal(pendingHold(), 8400, 'a long message holds proportionally longer')
    clipboard.toast('x'.repeat(1000))
    assert.equal(pendingHold(), 10000, 'the hold is capped')

    // The toast must stay click-through: it is centred over the viewport bottom,
    // where sticky action bars and modal footers live.
    assert.match(componentsCss, /\.toast\s*\{[\s\S]*?pointer-events:\s*none/)
    assert.doesNotMatch(componentsCss, /\.toast\.toast--visible\s*\{[^}]*pointer-events:\s*auto/)
    const pending = region.children[0]
    runTimer(10000)
    runTimer(FADE_MS)
    assert.equal(pending.parentNode, null)

    // Failures are assertive and visually distinct from confirmations.
    clipboard.toastError('this failed')
    const failure = region.children[0]
    assert.equal(failure.className, 'toast toast--error')
    assert.equal(failure.getAttribute('role'), 'alert')
    runTimer(SHORT_HOLD_MS)
    runTimer(FADE_MS)
    assert.equal(region.children.length, 0)
  } finally {
    globalThis.document = originalDocument
    globalThis.setTimeout = originalSetTimeout
    globalThis.clearTimeout = originalClearTimeout
  }
})
