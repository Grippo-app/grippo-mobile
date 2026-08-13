import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readdirSync, readFileSync } from 'node:fs'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const REPO = fileURLToPath(new URL('../../../', import.meta.url))
const SITE = join(REPO, 'orchestrator', 'site')
const SCRIPTS = join(SITE, 'scripts')
const componentsCss = readFileSync(join(SITE, 'styles', 'components.css'), 'utf8')
const panelsCss = readFileSync(join(SITE, 'styles', 'panels.css'), 'utf8')
const baseCss = readFileSync(join(SITE, 'styles', 'base.css'), 'utf8')

function scriptFiles(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const file = join(directory, entry.name)
    return entry.isDirectory() ? scriptFiles(file) : (entry.name.endsWith('.js') ? [file] : [])
  })
}

function matchingParen(source, open) {
  let depth = 0
  let quote = null
  let escaped = false
  let lineComment = false
  let blockComment = false
  for (let index = open; index < source.length; index += 1) {
    const char = source[index]
    const next = source[index + 1]
    if (lineComment) {
      if (char === '\n') lineComment = false
      continue
    }
    if (blockComment) {
      if (char === '*' && next === '/') {
        blockComment = false
        index += 1
      }
      continue
    }
    if (quote) {
      if (escaped) escaped = false
      else if (char === '\\') escaped = true
      else if (char === quote) quote = null
      continue
    }
    if (char === '/' && next === '/') {
      lineComment = true
      index += 1
      continue
    }
    if (char === '/' && next === '*') {
      blockComment = true
      index += 1
      continue
    }
    if (char === "'" || char === '"' || char === '`') {
      quote = char
      continue
    }
    if (char === '(') depth += 1
    if (char === ')' && --depth === 0) return index
  }
  return -1
}

function controlCalls(file) {
  const source = readFileSync(file, 'utf8')
  const calls = []
  const matcher = /\bel\(['"](input|select|textarea)['"]\s*,/g
  for (const match of source.matchAll(matcher)) {
    const open = source.indexOf('(', match.index)
    const close = matchingParen(source, open)
    assert.ok(close > open, `${relative(REPO, file)} has an unterminated control constructor`)
    const snippet = source.slice(match.index, close + 1)
    const className = (snippet.match(/\bclass:\s*['"]([^'"]*)['"]/) || [])[1] || ''
    const type = (snippet.match(/\btype:\s*['"]([^'"]*)['"]/) || [])[1] || ''
    calls.push({
      file,
      line: source.slice(0, match.index).split('\n').length,
      tag: match[1],
      type,
      dynamicChoice: /\btype:\s*cfg\.type\b/.test(snippet),
      classes: className.split(/\s+/).filter(Boolean),
    })
  }
  return calls
}

test('every visible native control explicitly opts into the matching component', () => {
  const calls = scriptFiles(SCRIPTS).flatMap(controlCalls)
  assert.ok(calls.length >= 50, 'control scanner must cover the complete current UI')

  const fieldTypes = new Set([
    'text', 'search', 'email', 'password', 'url', 'tel', 'number',
    'date', 'time', 'datetime-local', 'month', 'week',
  ])
  for (const call of calls) {
    let expected
    if (call.tag === 'select' || call.tag === 'textarea' || fieldTypes.has(call.type)) expected = 'input'
    else if (call.type === 'checkbox' || call.type === 'radio' || call.dynamicChoice) expected = 'choice-input'
    else if (call.type === 'range') expected = 'range-input'
    else assert.fail(`${relative(REPO, call.file)}:${call.line} has an unclassified ${call.tag}/${call.type || 'missing-type'} control`)
    assert.ok(call.classes.includes(expected),
      `${relative(REPO, call.file)}:${call.line} must use .${expected}`)
  }
})

test('raw DOM control construction is limited to the invisible clipboard fallback', () => {
  const raw = []
  for (const file of scriptFiles(SCRIPTS)) {
    const source = readFileSync(file, 'utf8')
    const withoutComments = source.replace(/\/\*[\s\S]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '')
    assert.doesNotMatch(withoutComments, /<\s*(?:input|select|textarea)\b/i,
      `${relative(SCRIPTS, file)} must not string-author a native control`)
    for (const match of source.matchAll(/document\.createElement\(['"](input|select|textarea)['"]\)/g)) {
      raw.push(`${relative(SCRIPTS, file)}:${match[1]}`)
    }
  }
  assert.deepEqual(raw, ['clipboard.js:textarea'])
})

test('static HTML cannot bypass the explicit control components', () => {
  const html = readFileSync(join(SITE, 'index.html'), 'utf8')
  for (const match of html.matchAll(/<(input|select|textarea)\b([^>]*)>/gi)) {
    const classes = ((match[2].match(/\bclass=["']([^"']*)["']/i) || [])[1] || '').split(/\s+/)
    assert.ok(classes.some((name) => ['input', 'choice-input', 'range-input'].includes(name)),
      `index.html ${match[1]} must opt into an explicit control component`)
  }
})

test('the component stylesheet has no native-tag fallback or duplicated panel field skin', () => {
  assert.match(componentsCss, /\.input\s*\{/)
  assert.match(componentsCss, /\.choice-input\s*\{/)
  assert.match(componentsCss, /\.range-input\s*\{/)
  assert.doesNotMatch(componentsCss, /:where\([^)]*(?:input|select|textarea)/)
  assert.doesNotMatch(componentsCss, /input:not\(|input\[type=/)

  for (const removedDuplicate of [
    '.form-field input[type="text"]',
    '.board-modal__input:hover',
    '.board-modal__textarea:focus',
    '.figma-file-input:focus',
    '.design-filter__control:focus-visible',
  ]) assert.equal(panelsCss.includes(removedDuplicate), false, removedDuplicate)
  assert.equal(panelsCss.includes('.figma-file-input'), false)
  assert.equal(readFileSync(join(SITE, 'scripts', 'panels', 'api.js'), 'utf8').includes('figma-file-input'), false)
})

test('site CSS cannot reintroduce a raw native-control skin', () => {
  const css = [baseCss, componentsCss, panelsCss].join('\n').replace(/\/\*[\s\S]*?\*\//g, '')
  for (const chunk of css.split('{').slice(0, -1)) {
    const selector = chunk.slice(chunk.lastIndexOf('}') + 1).trim()
    if (!selector || selector.startsWith('@')) continue
    const withoutQualifiedComponents = selector.replace(
      /\b(?:input|select|textarea)\.(?:input|choice-input|range-input)\b/gi, '')
    assert.doesNotMatch(withoutQualifiedComponents,
      /(^|[\s>+~,(])(input|select|textarea)(?=[\s\[:>+~),.#]|$)/i,
      `raw control selector: ${selector}`)
  }
})

function luminance(hex) {
  const channels = hex.slice(1).match(/.{2}/g).map((part) => parseInt(part, 16) / 255)
    .map((value) => value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4)
  return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]
}

function contrast(first, second) {
  const high = Math.max(luminance(first), luminance(second))
  const low = Math.min(luminance(first), luminance(second))
  return (high + 0.05) / (low + 0.05)
}

function colorToken(block, name) {
  const match = block.match(new RegExp(`${name}:\\s*(#[0-9a-f]{6})`, 'i'))
  assert.ok(match, `${name} must be an explicit six-digit color`)
  return match[1]
}

test('input placeholders retain WCAG AA text contrast in both themes', () => {
  const darkStart = baseCss.indexOf('@media (prefers-color-scheme: dark)')
  const themes = [baseCss.slice(0, darkStart), baseCss.slice(darkStart)]
  for (const theme of themes) {
    assert.ok(contrast(colorToken(theme, '--control-placeholder'), colorToken(theme, '--control-bg')) >= 4.5)
  }
})

test('compact and repeatable fields have explicit accessible names', () => {
  const cli = readFileSync(join(SITE, 'scripts', 'cli-status.js'), 'utf8')
  assert.match(cli, /cli-login__input[\s\S]{0,180}'aria-label':\s*t\('cli\.loginCodePlaceholder'\)/)
  const composer = readFileSync(join(SITE, 'scripts', 'board', 'backlog-composer.js'), 'utf8')
  assert.match(composer, /function composerBulletList\(addLabel, removeAria, inputAria\)/)
  assert.match(composer, /attrs:\s*\{\s*'aria-label': inputAria\s*\}/)
  assert.match(composer, /placeholder: labels\.screen, 'aria-label': labels\.screen/)
  assert.match(composer, /placeholder: labels\.url, 'aria-label': labels\.url/)
  assert.match(composer, /placeholder: t\('board\.create\.dataPlaceholder'\), 'aria-label': t\('board\.create\.dataField'\)/)
})

test('CLI login form owns the full popover width in every locale', () => {
  const cli = readFileSync(join(SITE, 'scripts', 'cli-status.js'), 'utf8')
  assert.match(cli,
    /row\('cli\.loginLabel', \[buildLoginBox\(loginJob\)\], \{ stacked: true \}\)/)
  assert.match(panelsCss,
    /\.site-status-row--stacked\s*\{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)/s)
  assert.match(panelsCss,
    /\.site-status-row--stacked \.site-status-row-value\s*\{[^}]*align-items:\s*stretch/s)
  assert.match(panelsCss,
    /\.site-status-row--stacked \.cli-login\s*\{[^}]*max-width:\s*none/s)
  const mobileStatusPopover = [...baseCss.matchAll(/\.status-strip \.site-status-pop\s*\{([^}]*)\}/gs)].at(-1)[1]
  assert.match(mobileStatusPopover, /max-height:\s*calc\(100dvh - 50px\)/)
  assert.match(mobileStatusPopover, /overflow-y:\s*auto/)
})

test('live refreshes preserve editable control drafts and focus', () => {
  const design = readFileSync(join(SITE, 'scripts', 'panels', 'design.js'), 'utf8')
  assert.match(design, /render\(\{ preserveToolbar: !tabChanged && !scopeChanged \}\)/)
  assert.match(design, /options\.preserveToolbar === true/)
  assert.match(design, /if \(!preserveToolbar\) toolbar\.replaceChildren\(\)/)

  const cli = readFileSync(join(SITE, 'scripts', 'cli-status.js'), 'utf8')
  assert.match(cli, /var loginDraft = ''/)
  assert.match(cli, /loginDraft = input\.value/)
  assert.match(cli, /restoreLoginFocus[\s\S]{0,900}replacement\.setSelectionRange\(selectionStart, selectionEnd\)/)

  const api = readFileSync(join(SITE, 'scripts', 'panels', 'api.js'), 'utf8')
  assert.match(api, /render\(\{ background: true, force: true \}\)/)
  assert.match(api, /function captureInteraction\(\)/)
  assert.match(api, /draft: editable \? active\.value : null/)
  assert.match(api, /function restoreInteraction\(snapshot\)/)
  assert.match(api, /replacement\.setSelectionRange\(snapshot\.selectionStart, snapshot\.selectionEnd\)/)

  const board = readFileSync(join(SITE, 'scripts', 'panels', 'board.js'), 'utf8')
  const visualEvidenceView = readFileSync(
    join(SITE, 'scripts', 'board', 'visual-evidence-view.js'), 'utf8')
  const visualEvidenceSummaryView = readFileSync(
    join(SITE, 'scripts', 'board', 'visual-evidence-summary-view.js'), 'utf8')
  assert.match(visualEvidenceView, /value: opts\.filterValue \|\| ''/)
  assert.match(visualEvidenceSummaryView,
    /filterValue: opts\.filterValue,\s*onFilterInput: opts\.onFilterInput/)
  assert.match(board, /onFilterInput: function \(value\) \{ filterValue = value; \}/)
  assert.match(board, /restoreFilter[\s\S]{0,1500}replacement\.setSelectionRange/)
})
