import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const REPO = fileURLToPath(new URL('../../../', import.meta.url))
const SITE = join(REPO, 'orchestrator', 'site')
const setup = readFileSync(join(SITE, 'scripts', 'panels', 'setup.js'), 'utf8')
const wizard = readFileSync(join(SITE, 'scripts', 'panels', 'wizard.js'), 'utf8')
const css = readFileSync(join(SITE, 'styles', 'panels.css'), 'utf8')

test('fresh Setup starts with one focused onboarding action', () => {
  assert.ok(setup.includes('function isFreshSetup(setup, setupDone)'))
  assert.ok(setup.includes('!setup.productName && !setup.orgName && !setup.backendHost'))
  assert.ok(setup.includes('setupIntroDismissed = true'))
  assert.ok(setup.includes("class: 'first-run-card first-run-card--setup'"))
  assert.ok(setup.includes("text: t('setup.intro.action')"))
  assert.ok(setup.includes("'aria-label': t('setup.intro.stepsLabel')"))
})

test('Launch Wizard has first-run surfaces before and after Setup', () => {
  assert.ok(wizard.includes("class: 'first-run-card first-run-card--wizard-gate'"))
  assert.ok(wizard.includes("text: t('wizard.gate.action')"))
  assert.ok(wizard.includes("class: 'first-run-card first-run-card--wizard'"))
  assert.ok(wizard.includes('!hasWizardProgress(state) && !wizardIntroDismissed'))
  assert.ok(wizard.includes("String(id) !== '12'"))
  assert.ok(wizard.includes('wizardIntroDismissed = true'))
  assert.ok(wizard.includes("text: t('wizard.intro.action')"))
})

test('shared first-run surface mirrors Backend and stays mobile-safe', () => {
  assert.match(css, /\.first-run-card\s*\{[\s\S]*?grid-template-columns:/)
  assert.match(css, /\.first-run-card__steps\s*\{[\s\S]*?border-left:/)
  assert.match(css, /@media \(max-width: 760px\)[\s\S]*?\.first-run-card\s*\{[^}]*grid-template-columns:\s*1fr;/)
  assert.match(css, /\.first-run-card__action\s*\{[^}]*width:\s*100%;/)
})

test('Setup form exposes numbered, described and responsive sections', () => {
  assert.ok(setup.includes("class: 'setup-card-head'"))
  assert.ok(setup.includes("class: 'setup-card-num'"))
  assert.ok(setup.includes("'setup.cardProjectDescription'"))
  assert.match(css, /\.setup-card\s*\{[\s\S]*?grid-template-columns:\s*repeat\(2,/)
  assert.match(css, /@media \(max-width: 680px\)[\s\S]*?\.setup-card\s*\{[^}]*grid-template-columns:\s*1fr;/)
})

test('Launch Wizard groups the pipeline into readable phases', () => {
  assert.ok(wizard.includes("{ id: 'foundation', through: 5"))
  assert.ok(wizard.includes("{ id: 'finish',     through: Infinity"))
  assert.ok(wizard.includes("class: 'wizard-group-head'"))
  assert.ok(wizard.includes("class: 'wizard-group-steps'"))
  assert.ok(wizard.includes("t('wizard.group.count', { count: count })"))
  assert.match(css, /\.wizard-group-steps\s*\{[^}]*display:\s*flex;/)
})

test('Wizard copy stays a quiet fallback beside Run', () => {
  assert.ok(wizard.includes("text: runAvailable ? '⧉' : t('wizard.btnCopyPrompt')"))
  assert.ok(wizard.includes("keepLabel: runAvailable"))
  assert.ok(wizard.includes("'aria-label': t('wizard.btnCopyPrompt')"))
  assert.match(css, /\.step-copy-btn\s*\{[\s\S]*?background:\s*transparent;[\s\S]*?border:\s*1px solid transparent;/)
  assert.match(css, /\.step-copy-btn--icon\s*\{[^}]*width:\s*28px;/)
})

test('Setup bootstrap copy follows the same fallback hierarchy', () => {
  assert.ok(setup.includes("class: 'step-copy-btn' + (lastCliReady ? ' step-copy-btn--icon' : '')"))
  assert.ok(setup.includes("text: lastCliReady ? '⧉' : t('setup.copyPromptBtn')"))
  assert.ok(setup.includes("{ keepLabel: lastCliReady }"))
  assert.ok(setup.includes("'aria-label': t('setup.copyPromptAria')"))
})
