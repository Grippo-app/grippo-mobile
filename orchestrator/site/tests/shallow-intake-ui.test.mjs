#!/usr/bin/env node

import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

import { dictionaryFor } from './i18n-test-helpers.mjs'

const site = join(dirname(fileURLToPath(import.meta.url)), '..')
const overview = readFileSync(join(site, 'scripts', 'board', 'task-overview.js'), 'utf8')
const taskCard = readFileSync(join(site, 'scripts', 'board', 'task-card.js'), 'utf8')
const taskSummary = readFileSync(join(site, 'server', 'task-summary.js'), 'utf8')
const panels = readFileSync(join(site, 'styles', 'panels.css'), 'utf8')
const en = dictionaryFor('en')
const ru = dictionaryFor('ru')
const uk = dictionaryFor('uk')

assert.doesNotMatch(taskSummary, /kind: 'intake'/,
  'the automatic-check is advisory-only: it must not surface a card-face signal — it lives solely in the collapsed details section')
assert.match(taskCard, /\(row\.compactSignals \|\| \[\]\)\.slice\(0, 2\)/,
  'the card consumes only the bounded compact signal projection')
assert.doesNotMatch(taskCard, /board-card__intake-(summary|areas|findings|finding)/,
  'the board card must not embed preview prose, chips, or findings')

const sectionStart = overview.indexOf('function intakeWork(')
const sectionEnd = overview.indexOf('export function taskOverview(', sectionStart)
const section = overview.slice(sectionStart, sectionEnd)
assert.match(section, /el\('details', \{[\s\S]*?class: 'task-details__current-work board-intake'/,
  'full automatic-check details must be collapsed out of the task body by default')
assert.match(section, /el\('summary', \{ class: 'board-intake__head' \}\)/,
  'the collapsed automatic-check section must have a clear summary')
assert.match(section, /class: 'board-intake__actions'/,
  'preview-only controls belong inside the collapsed automatic-check section')
assert.match(section,
  /MODEL_KEYCHAIN_SANDBOX_INCOMPATIBLE:\s*'board\.intake\.failureKeychainSandbox'/,
  'the known macOS Keychain containment failure must have a specific explanation')
assert.match(section,
  /MODEL_SCHEMA_TRANSPORT_INCOMPATIBLE:\s*'board\.intake\.failureSchemaTransport'/,
  'the known Claude schema transport failure must have a specific explanation')
assert.match(section, /'board\.intake\.failureDetail'/,
  'unknown model failures must retain the bounded generic fallback')
assert.doesNotMatch(section, /\{\s*code:\s*intake\.errorCode/,
  'the preview must never interpolate an internal failure code into user-facing copy')

for (const [language, source] of [['English', en], ['Russian', ru]]) {
  assert.ok(source['board.intake.failureKeychainSandbox'],
    `${language} must explain the Keychain containment failure`)
  assert.ok(source['board.intake.failureSchemaTransport'],
    `${language} must explain the schema transport failure`)
  assert.ok(source['board.intake.failureOutputInvalid'],
    `${language} must explain strict output-schema failures`)
  assert.ok(source['board.action.prepare'],
    `${language} must label the server-resolved preparation action`)
}

function localeValue(source, key) {
  return source[key] || ''
}

for (const [language, source, advisoryPattern] of [
  ['English', en, /never blocks.*requires a response/i],
  ['Russian', ru, /ничего не блокирует.*не требует ответа/i],
  ['Ukrainian', uk, /нічого не блокує.*не потребує відповіді/i]
]) {
  const earlyCopy = [
    'board.create.bodyPlaceholder',
    'board.create.automatedHint',
    'board.intake.readiness.needs-context',
    'board.intake.advisory',
    'board.intake.missing'
  ].map(function (key) { return localeValue(source, key) }).join(' ')
  assert.doesNotMatch(earlyCopy,
    /Clarify and prepare|ask clarifying questions|open questions|Уточнить и подготовить|Есть вопросы к описанию|Уточнити та підготувати|Є питання до опису/i,
    `${language} must not present advisory intake findings as a question gate`)
  assert.match(localeValue(source, 'board.intake.advisory'), advisoryPattern,
    `${language} must say that the intake preview neither blocks nor requires a response`)
}

assert.doesNotMatch(panels,
  /\.board-intake__status--needs-context,[\s\S]{0,160}var\(--banner-warn-fg\)/,
  'needs-context is an advisory Prepare input and must not use blocker warning color')

assert.match(panels, /grid-template-columns:\s*repeat\(4, minmax\(0, 1fr\)\)/,
  'desktop board tracks must be allowed to shrink below content min-width')
assert.match(panels, /\.board-column\s*\{[\s\S]*?min-width:\s*0;/,
  'board columns must not inherit a content-driven minimum width')
assert.match(panels, /\.board-card\s*\{[\s\S]*?width:\s*100%;[\s\S]*?min-width:\s*0;[\s\S]*?overflow:\s*hidden;/,
  'cards must stay inside their grid track even with long generated content')

console.log('shallow-intake UI: checks passed')
