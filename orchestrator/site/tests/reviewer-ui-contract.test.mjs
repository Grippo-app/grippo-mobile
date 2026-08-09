import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

import {
  dictionaryFor,
  localeText
} from './i18n-test-helpers.mjs'

const REPO = fileURLToPath(new URL('../../../', import.meta.url))
const SITE = join(REPO, 'orchestrator', 'site')
const panel = readFileSync(join(SITE, 'scripts', 'panels', 'reviewer.js'), 'utf8')
const setup = readFileSync(join(SITE, 'scripts', 'panels', 'setup.js'), 'utf8')
const board = readFileSync(join(SITE, 'scripts', 'panels', 'board.js'), 'utf8')
const registry = readFileSync(join(SITE, 'scripts', 'registry.js'), 'utf8')
const css = readFileSync(join(SITE, 'styles', 'panels.css'), 'utf8')
const en = dictionaryFor('en')
const ru = dictionaryFor('ru')
const uk = dictionaryFor('uk')

function localeKeys(dictionary, prefix) {
  return Object.keys(dictionary)
    .filter((key) => key.startsWith(prefix))
    .sort()
}

test('Reviewer has one canonical visible and internal route contract', () => {
  assert.equal(en['nav.reviewer'], 'Reviewer')
  assert.equal(ru['nav.reviewer'], 'Reviewer')
  assert.equal(en['codex.title'], 'Reviewer')
  assert.equal(ru['codex.title'], 'Reviewer')
  assert.match(registry, /id:\s*'reviewer'/)
  assert.doesNotMatch(registry, /id:\s*'codex'/)
  assert.doesNotMatch(registry, /panels\/codex\.js/)
  assert.doesNotMatch([panel, setup, localeText('en'), localeText('ru')].join('\n'), /Codex Loop/)
  assert.doesNotMatch(
    [localeText('en'), localeText('ru')].join('\n'),
    /codexEnabled:\s*(?:auto|true|false)|auto · Codex|false → internal/
  )
  for (const retiredKey of [
    'codex.radio.auto.label',
    'codex.radio.true.label',
    'codex.radio.false.label',
    'codex.matrixHeading',
    'codex.yamlBanner.pre',
  ]) assert.equal(Object.hasOwn(en, retiredKey), false, retiredKey)
})

test('Reviewer panel exposes operational status, canonical settings, activity, diagnostics, and recovery', () => {
  for (const key of [
    'codex.summary.review',
    'codex.summary.mode',
    'codex.summary.activeReviewer',
    'codex.summary.codex',
    'codex.summary.fallback',
    'codex.summary.lastReview',
    'codex.summary.queue',
    'codex.lastReview.title',
    'codex.activity.pending',
    'codex.activity.failed',
  ]) assert.ok(panel.includes(key), key)

  assert.ok(panel.includes("role: 'radiogroup'"))
  assert.ok(panel.includes("type: 'radio'"))
  assert.ok(panel.includes("attrs: { 'aria-live': 'polite' }"))
  assert.ok(panel.includes('dirtyMode || status.config.mode'))
  assert.ok(panel.includes('dirtyRevision'))
  assert.ok(panel.includes('dirtyRevision !== status.config.revision'))
  assert.ok(panel.includes('reconcileDirtySettings()'))
  assert.ok(panel.includes('saving || settingsConflict || !dirtyMode'))
  assert.ok(panel.includes('status.config.revision'))
  assert.ok(panel.includes('reviewerApi.save(requested, requestedRevision, settingsRequest.key)'))
  assert.ok(panel.includes("reviewerApi.idempotencyKey('reviewer-settings')"))
  assert.ok(panel.includes('return reviewerApi.status().then(function (value)'))
  assert.ok(panel.includes("t('codex.settings.reload')"))
  assert.ok(panel.includes("t('codex.action.diagnostics')"))
  assert.ok(panel.includes("status.review.fallbackPolicy === 'internal-when-not-detected'"))
  assert.ok(panel.includes("status.review.activeReviewer === 'internal-reviewer'"))
  assert.ok(panel.includes("status.review.activeReviewerBasis === 'active-review'"))
  assert.ok(panel.includes('response.revision !== requestRevision'))
  assert.ok(panel.includes('activity[kind] !== slot'))
  assert.ok(panel.includes("error.code === 'stale-activity-cursor'"))
  assert.ok(panel.includes('installedLabel(status.codex.installed)'))
  assert.ok(panel.includes("data-reviewer-focus"))
  assert.ok(panel.includes('diagnosticsOpen = details.open'))
  assert.ok(panel.includes('applyStatus(response.reviewer)'))
  assert.match(panel, /el\('details', \{ class: 'reviewer-diagnostics', open: diagnosticsOpen \}\)/)
  assert.match(panel, /if \(status\.codex\.availability !== 'available'\)[\s\S]+recoveryCommands\(status\.codex\.reasonCode\)/)
  for (const command of [
    'INSTALL_PLUGIN',
    'ENABLE_PLUGIN',
    'UPDATE_PLUGIN',
    'INSTALL_RELOAD',
    'INSTALL_SETUP',
    'CODEX_LOGIN',
  ]) {
    assert.ok(panel.includes(command), command)
  }
  assert.ok(panel.includes('recoveryCommands(status.codex.reasonCode)'))
  assert.ok(panel.includes('board.openTask(row.taskStem)'))
  assert.ok(panel.includes('terminal.open(row.sessionKey)'))
  assert.match(board,
    /openTask: function \(stem\) \{\s*if \(!boardTaskTargetController\.request\(stem, 'overview', null\)\) return;\s*router\.go\('board'\);\s*if \(!state\.columns \|\| boardTaskTargetController\.openRequested\(false\) === false\) \{\s*boardLoadController\.load\(\{ closeOpenModal: true \}\);\s*\}/)
})

test('Setup and Reviewer share one endpoint and no persisted Reviewer shadow', () => {
  assert.ok(setup.includes('reviewerApi.save('))
  assert.ok(setup.includes('var revision = currentConfig.revision ||'))
  assert.ok(setup.includes('delete persisted.codexEnabled'))
  assert.ok(setup.includes('return store.load().catch(function () {})'))
  assert.ok(setup.includes('canonicalReviewerValue()'))
  assert.ok(setup.includes('reviewerValueForForm()'))
  assert.ok(setup.includes("config.state === 'missing' && config.revision === null"))
  assert.ok(setup.includes('reviewerConfig.canUpdate === true'))
  assert.ok(setup.includes("t('setup.reviewerBootstrapDefault')"))
  assert.ok(setup.includes("'setup.reviewerAddMissing'"))
  assert.doesNotMatch(setup, /mirror the change|manually update|ручн\w+.*project-config/i)
})

test('Reviewer layout and status remain accessible and one-column on mobile', () => {
  for (const selector of [
    '.reviewer-badge--operational',
    '.reviewer-badge--attention-required',
    '.reviewer-badge--blocked',
    '.reviewer-activity__status--pending',
    '.reviewer-activity__status--failed',
    '.reviewer-activity__status--passed',
  ]) assert.ok(css.includes(selector), selector)
  assert.match(css, /@media \(max-width: 700px\)[\s\S]+\.reviewer-activity-grid[\s\S]+grid-template-columns: 1fr/)
  assert.match(css, /@media \(max-width: 520px\)[\s\S]+\.reviewer-activity__row[\s\S]+flex-direction: column/)
  assert.ok(panel.includes('text: overallLabel(status.overall)'))
  assert.ok(panel.includes('text: rowStatus(row, kind)'))
})

test('EN, RU, and UA Reviewer dictionaries remain in exact parity with user-facing mode labels', () => {
  assert.deepEqual(localeKeys(en, 'codex.'), localeKeys(ru, 'codex.'))
  assert.deepEqual(localeKeys(en, 'codex.'), localeKeys(uk, 'codex.'))
  for (const label of ['Automatic', 'Require Codex', 'Internal review only']) {
    assert.ok(Object.values(en).includes(label), label)
    assert.ok(Object.values(ru).includes(label), label)
    assert.ok(Object.values(uk).includes(label), label)
  }
})
