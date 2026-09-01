import assert from 'node:assert/strict'
import { test } from 'node:test'
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

import { localeText } from './i18n-test-helpers.mjs'
import backendEn from '../scripts/i18n/dictionaries/backend-integration/en.js'
import backendRu from '../scripts/i18n/dictionaries/backend-integration/ru.js'
import backendUk from '../scripts/i18n/dictionaries/backend-integration/uk.js'
import {
  dialogProbeOutcome,
  normalizeSourceUrl,
  previewFailureNeedsErrorBanner,
  resolutionApplyPlan,
  sourceCredentialMustReset,
  sourceInputProfile
} from '../scripts/panels/backend.js'

const REPO = fileURLToPath(new URL('../../../', import.meta.url))
const backendFile = join(REPO, 'orchestrator', 'site', 'scripts', 'panels', 'backend.js')
const statusFile = join(REPO, 'orchestrator', 'site', 'scripts', 'contract-status.js')
const backend = readFileSync(backendFile, 'utf8')
const status = readFileSync(statusFile, 'utf8')
const css = readFileSync(join(REPO, 'orchestrator', 'site', 'styles', 'panels.css'), 'utf8')
const baseCss = readFileSync(join(REPO, 'orchestrator', 'site', 'styles', 'base.css'), 'utf8')
const componentsCss = readFileSync(join(REPO, 'orchestrator', 'site', 'styles', 'components.css'), 'utf8')

function filesUnder(directory, extension) {
  const out = []
  for (const name of readdirSync(directory)) {
    const file = join(directory, name)
    if (statSync(file).isDirectory()) out.push(...filesUnder(file, extension))
    else if (file.endsWith(extension)) out.push(file)
  }
  return out
}

test('Backend frontend exposes the typed responsibility split and accessibility fences', () => {
  for (const route of ['/api/backend/integration', '/api/backend/test', '/api/backend/refresh',
    '/api/backend/environments', '/api/backend/credential', '/api/backend/integration/reset']) assert.ok(backend.includes(route), route)
  assert.equal((backend.match(/requestJson\('\/api\/backend\/integration', \{ cache: 'no-store', timeoutMs: 10000 \}\)/g) || []).length, 2)
  assert.equal(backend.includes('/api/session/start'), false)
  assert.equal(backend.includes('/api/session/send'), false)
  assert.ok(backend.includes("href: '#api'"))
  assert.ok(backend.includes("class: 'u-visually-hidden'"))
  assert.ok(backend.includes("attrs: { for: id }"))
  assert.ok(backend.includes("attrs: { 'aria-live': 'polite' }"))
  assert.match(backend, /var value = secret\.value; secret\.value = '';/)
  assert.equal(/type:\s*['"]text['"][^\n]+credential/i.test(backend), false)
  assert.equal(backend.includes('Postman-only bootstrap is deliberately reachable only from Advanced'), false)
  assert.ok(backend.includes('sourceInputProfile(sourceUrl.value, failureCode)'))
  assert.ok(backend.includes("el('details', { class: 'card backend-card backend-advanced' })"))
  assert.ok(backend.includes("t('backend.preview.environment'"))
  assert.ok(backend.includes("t('backend.value.stale'"))
  assert.equal(backend.includes('migrateSource'), false)
  assert.ok(backend.includes("button('backend.action.addFirstEnvironment'"))
  assert.equal(backend.includes("button('backend.action.addSource'"), false)
  assert.ok(backend.includes("'btn btn--primary backend-setup-action'"))
  assert.ok(backend.includes("'aria-labelledby': titleId"))
  assert.ok(backend.includes("'aria-label': t('backend.setup.stepsLabel')"))
  assert.ok(backend.includes("model.sourceMode === 'missing'"))
  assert.ok(backend.includes("backend.source.environmentPlaceholder"))
  assert.ok(backend.includes("backend.setup.stepRefresh"))
  assert.ok(backend.includes("model.diagnostics.lastError"))
  assert.ok(backend.includes("refresh.sourceFingerprint === preview.sourceFingerprint"))
  assert.ok(status.includes("value === 'attention-required'"))
  assert.ok(status.includes("value === 'needs-test'"))
  assert.ok(backend.includes('npm run contract:doctor · npm run contract:probe'))
  assert.ok(backend.includes("t('backend.result.failed'"))
  assert.ok(backend.includes("t('backend.result.partial'"))
  assert.ok(backend.includes("data-backend-focus"))
  assert.ok(backend.includes("button('backend.action.clearIntegration'"))
  assert.ok(backend.includes("openDialog('backend.clear.title'"))
  assert.ok(backend.includes('expectedEnvironmentRevision: model.environmentRevision'))
  assert.ok(backend.includes('expectedSnapshotHash: model.snapshot && model.snapshot.hash || null'))
  assert.ok(backend.includes('expectedStateRevision: model.selectionRevision'))
  assert.ok(backend.includes("var resetKey = idempotencyKey('backend-reset')"))
  assert.ok(backend.includes("dialog.querySelectorAll('button')"))
  assert.ok(backend.includes("dialog.addEventListener('cancel'"))
  const clearDialog = backend.slice(backend.indexOf('function clearIntegration()'), backend.indexOf('function buildHeader()'))
  assert.ok(clearDialog.includes("dialog.setAttribute('aria-busy', 'true')"))
  assert.ok(clearDialog.includes("confirm.textContent = t('backend.clear.progressButton')"))
  assert.ok(clearDialog.includes("status.textContent = t('backend.clear.progress')"))
  assert.ok(clearDialog.indexOf('status.hidden = false') < clearDialog.indexOf("postJson('/api/backend/integration/reset'"))
  assert.ok(clearDialog.includes("dialog.removeAttribute('aria-busy')"))
  for (const dictionary of [backendEn, backendRu, backendUk]) {
    assert.ok(dictionary['backend.clear.body'].length > 80)
    assert.ok(dictionary['backend.action.clearIntegration'])
    assert.ok(dictionary['backend.clear.confirm'])
    assert.ok(dictionary['backend.clear.progressButton'])
    assert.ok(dictionary['backend.clear.progress'].length > 60)
    assert.ok(dictionary['backend.error.snapshot-invalid'].includes(dictionary['backend.action.clearIntegration']))
  }
  assert.match(css, /\.backend-clear-confirm\s*\{[^}]*color:\s*var\(--danger\)/)
  assert.match(css, /\.integration-clear-progress\s*\{[\s\S]*?background:\s*var\(--info-soft\)/)
  assert.match(css, /\.integration-clear-progress::before\s*\{[\s\S]*?animation:\s*integration-clear-spin/)
  assert.match(css, /@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.integration-clear-progress::before\s*\{\s*animation:\s*none;/)
  assert.ok(css.includes('@media (max-width: 760px)'))
  assert.ok(css.includes('.u-visually-hidden'))
  assert.match(css, /\.backend-advanced:not\(\[open\]\)\s*\{\s*gap:\s*0;/)
})

test('Backend dialogs use the explicit shared input component in both color schemes', () => {
  assert.match(baseCss, /:root\s*\{[\s\S]*?color-scheme:\s*light;/)
  assert.match(baseCss, /@media \(prefers-color-scheme: dark\)[\s\S]*?color-scheme:\s*dark;/)
  assert.match(baseCss, /@media \(prefers-color-scheme: dark\)[\s\S]*?--control-bg:\s*(?!white\b|#fff(?:fff)?\b)[^;]+;/i)
  for (const token of ['--control-bg', '--control-disabled-bg', '--control-placeholder']) {
    assert.ok(baseCss.includes(token), token)
  }
  assert.match(componentsCss, /\.input\s*\{[\s\S]*?background:\s*var\(--control-bg\)/)
  assert.match(componentsCss, /\.input::placeholder\s*\{[\s\S]*?var\(--control-placeholder\)/)
  assert.match(componentsCss, /\.choice-input\s*\{[\s\S]*?accent-color:\s*var\(--accent\)/)
  assert.match(componentsCss, /\.input:-webkit-autofill[\s\S]*?var\(--control-bg\)/)
  assert.match(componentsCss, /\.input:-webkit-autofill:focus\s*\{[\s\S]*?var\(--focus-ring\)/)

  assert.match(backend, /el\('select', \{ class: 'input', required: true \}\)/)
  assert.match(backend, /el\('input', \{ type: 'url', class: 'input'/)
  assert.match(backend, /el\('input', \{ type: 'checkbox', class: 'choice-input'/)
  const sourceEditor = backend.slice(backend.indexOf('function sourceEditor'), backend.indexOf('function deleteEnvironment'))
  assert.match(sourceEditor, /type: 'password',[^\n]+autocomplete: 'new-password'/)
  assert.doesNotMatch(sourceEditor, /type: 'password',[^\n]+required: true/)
  assert.equal(sourceEditor.includes("field('backend.source.kind'"), false)
  assert.equal(sourceEditor.includes('backend.source.authKind'), false)
  assert.ok(sourceEditor.includes("t('backend.source.detected.' + detectedSourceKind)"))
  assert.ok(sourceEditor.includes("t('backend.source.credentialOptional.' + detectedAuthKind)"))
  assert.ok(sourceEditor.includes('authWrap.hidden = postman'))
  assert.ok(sourceEditor.includes("postJson('/api/backend/credential'"))
  assert.match(sourceEditor, /var value = rebinding \? carriedValue : credential\.value;\s*if \(!value\)/)
  assert.ok(sourceEditor.includes("credential.value = ''"))
  assert.ok(sourceEditor.includes("transientCredential = ''"))
  assert.ok(sourceEditor.includes('result.credentialReset && transientCredential'))
  assert.ok(sourceEditor.includes('allowMissing'))
  assert.ok(backend.includes("apiKey ? 'backend.credential.labelApiKey'"))
  assert.ok(backend.includes('function resolutionContent(preview)'))
  assert.ok(backend.includes('preview.environmentRevision !== model.environmentRevision'))
  assert.ok(backend.includes("button('backend.resolution.apply'"))
  assert.ok(backend.includes('expectedEnvironmentRevision: response.revision'))
  assert.ok(backend.includes('disabled: !model.actions.canTest'))
  assert.ok(backend.includes("pick.addEventListener('click', function () { applyResolution(resolution, candidate); })"))
  assert.ok(backend.includes("placeholder: 'https://api.example.com/docs'"))
  assert.ok(backend.includes('KEEP IN SYNC with postmanUrlInfo'))
  assert.ok(backend.includes('export function normalizeSourceUrl(value)'))
  assert.ok(backend.includes("parsed.search = ''; parsed.hash = '';"))
  assert.ok(backend.includes('var normalizedSourceUrl = normalizeSourceField()'))
  assert.ok(backend.includes('sourceUrl: normalizedSourceUrl'))
  assert.ok(backend.includes("t('backend.resolution.pmatRejected')"))
  assert.ok(backend.includes("resolution.detectedKind === 'postman'"))
  assert.ok(backend.includes("t('backend.resolution.postmanPicker')"))
  assert.ok(backend.includes("postJson('/api/backend/credential', { environmentId: current.id, operation: 'delete'"))
  assert.ok(backend.indexOf("operation: 'delete'") < backend.indexOf('upsertAndContinue(deleted.revision, true)'))
  // The resolution flow must hand the credential dialog a continuation, or storing
  // the secret leaves the source change applied and never re-tests it.
  assert.match(backend, /credentialDialog\(environment\.id, expectedAuthRevision, \{\s*applied: true,\s*onStored:/)
  assert.match(css, /\.backend-dialog-field > \.input \{ width: 100%;/)
  assert.match(css, /\.backend-dialog \[hidden\] \{ display: none; \}/)
  assert.match(css, /\.backend-checkbox \.choice-input \{ flex: 0 0 auto;/)
})

test('a source editor that already wrote the environment stops offering "Cancel"', () => {
  const sourceEditor = backend.slice(backend.indexOf('function sourceEditor'), backend.indexOf('function deleteEnvironment'))

  // Both dismiss controls (dialog head and action bar) carry the marker class the
  // relabel queries, so neither keeps saying "Cancel" after the write landed.
  assert.match(backend, /var close = button\('common\.cancel', 'btn btn--ghost backend-dialog-dismiss'/)
  assert.match(sourceEditor, /button\('common\.cancel', 'btn btn--ghost backend-dialog-dismiss'/)
  assert.match(sourceEditor, /dismissals\[index\]\.textContent = t\('backend\.action\.close'\)/)
  // The relabel is bound to the environment write itself, not to a later leg.
  assert.match(sourceEditor, /postJson\('\/api\/backend\/environments', \{[\s\S]*?\}\)\.then\(function \(written\) \{ markEnvironmentSaved\(\); return written; \}\)/)
  // Revealed first, then written: a live region whose text changes while it is
  // hidden is not announced.
  assert.match(sourceEditor,
    /savedNotice\.hidden = false;\s*savedNotice\.textContent = t\('backend\.source\.savedNotice'\)/)
  // The notice lives in the action bar, which never scrolls out of view.
  assert.match(sourceEditor, /class: 'backend-dialog-actions' \}, \[\s*savedNotice,/)
  assert.match(css, /\.backend-dialog-saved \{ flex: 1 0 100%;/)
  // Every verdict that keeps the dialog open must be brought into view.
  assert.match(sourceEditor, /status\.scrollIntoView\(\{ block: 'nearest' \}\)/)
})

test('Backend actions expose immediate progress and make preview publication explicit', () => {
  const load = backend.slice(backend.indexOf('function load()'), backend.indexOf('function activeEnvironment()'))
  assert.ok(load.indexOf('render();') < load.indexOf("requestJson('/api/backend/integration'"))
  const busy = backend.slice(backend.indexOf('function setBusy'), backend.indexOf('function selectEnvironment'))
  assert.ok(busy.indexOf('pendingAction = actionKind') < busy.indexOf('render()'))
  assert.ok(busy.indexOf('render()') < busy.indexOf('Promise.resolve().then(action)'))
  assert.ok(backend.includes("running.push({ reportType: pendingAction, progress: [] })"))
  assert.ok(backend.includes("actionNodes.push(button(activeLabel, 'btn btn--primary', null, true))"))
  assert.ok(backend.includes("'backend.action.import'"))
  assert.ok(backend.includes("t('backend.preview.applyHint')"))
  assert.ok(backend.includes("'btn btn--primary', runRefresh"))
  assert.match(css, /\.backend-header-copy\s*\{[^}]*max-width:\s*520px;/)
  assert.match(css, /\.backend-header-actions\s*\{[^}]*flex-wrap:\s*nowrap;/)
  assert.match(css, /\.backend-job strong::before\s*\{[\s\S]*?animation:\s*integration-clear-spin/)
  assert.equal(css.includes('.backend-tabs'), false)
  assert.equal(css.includes('.api-source-badge'), false)
})

test('Backend reloads automatically when the local Site server reconnects', () => {
  const events = backend.slice(backend.indexOf('function connectEvents()'), backend.indexOf('export const backend'))
  assert.ok(events.includes("siteEvents.on('open'"))
  assert.ok(events.includes('if (loadError && !model) load();'))
})

test('Site multiplexes live updates over one EventSource connection', async () => {
  const clientFiles = filesUnder(join(REPO, 'orchestrator', 'site', 'scripts'), '.js')
  const owners = clientFiles.filter(file => readFileSync(file, 'utf8').includes("new EventSource('/api/events')"))
  assert.deepEqual(owners, [join(REPO, 'orchestrator', 'site', 'scripts', 'event-stream.js')])

  const previousEventSource = globalThis.EventSource
  const instances = []
  class FakeEventSource {
    constructor(url) {
      this.url = url
      this.readyState = 1
      this.listeners = Object.create(null)
      instances.push(this)
    }
    addEventListener(name, handler) {
      if (!this.listeners[name]) this.listeners[name] = []
      this.listeners[name].push(handler)
    }
    removeEventListener(name, handler) {
      this.listeners[name] = (this.listeners[name] || []).filter(listener => listener !== handler)
    }
    emit(name, data) {
      for (const handler of this.listeners[name] || []) handler({ data })
    }
  }
  globalThis.EventSource = FakeEventSource
  try {
    const { siteEvents } = await import(`../scripts/event-stream.js?test=${Date.now()}`)
    let backendEvents = 0
    let appRunEvents = 0
    const offBackend = siteEvents.on('backend-job', () => { backendEvents++ })
    siteEvents.on('app-run-status', () => { appRunEvents++ })
    assert.equal(instances.length, 1)
    assert.equal(instances[0].url, '/api/events')
    assert.equal(siteEvents.connection().readyState, 1)
    instances[0].emit('backend-job', '{}')
    instances[0].emit('app-run-status', '{}')
    assert.equal(backendEvents, 1)
    assert.equal(appRunEvents, 1)
    offBackend()
    instances[0].emit('backend-job', '{}')
    assert.equal(backendEvents, 1)
  } finally {
    if (previousEventSource === undefined) delete globalThis.EventSource
    else globalThis.EventSource = previousEventSource
  }
})

test('source popup infers Postman API keys immediately and bearer auth only from an unauthenticated probe', () => {
  const teamUrl = 'https://plantin-team.postman.co/workspace/PlantIn/request/24467165-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'
  assert.deepEqual(sourceInputProfile(teamUrl), {
    sourceKind: 'postman', authenticationRequired: true, authKind: 'x-api-key'
  })
  assert.deepEqual(sourceInputProfile('https://www.postman.com/team/workspace'), {
    sourceKind: 'postman', authenticationRequired: true, authKind: 'x-api-key'
  })
  assert.deepEqual(sourceInputProfile('https://api.example.com/docs'), {
    sourceKind: 'openapi', authenticationRequired: false, authKind: 'bearer'
  })
  assert.deepEqual(sourceInputProfile('https://plantin-team.postman.co.attacker.example/workspace'), {
    sourceKind: 'openapi', authenticationRequired: false, authKind: 'bearer'
  })
  assert.deepEqual(sourceInputProfile('https://api.example.com/docs', 'auth-rejected'), {
    sourceKind: 'openapi', authenticationRequired: true, authKind: 'bearer'
  })
  assert.equal(normalizeSourceUrl(teamUrl + '?tab=overview#request').value, teamUrl)
  assert.ok(backend.includes("button('backend.action.test', 'btn btn--primary', null)"))
  assert.ok(backend.includes("waitForJobResult(started.job.jobId"))
  assert.ok(backend.includes("applyAutomaticProfile('auth-rejected')"))
  assert.ok(backend.includes("model.authentication.state === 'missing' || replacementCode"))
  assert.ok(backend.includes('current = activeEnvironment()'))
  assert.ok(backend.includes('renderDialogResolution(outcome.resolution, environment)'))
  assert.ok(backend.includes('applyDialogResolution(resolution, candidate)'))
  const terminalProbeSuccess = backend.slice(backend.indexOf("if (outcome.state === 'resolution'"),
    backend.indexOf('setEditorBusy(false); return job;'))
  assert.match(terminalProbeSuccess, /else \{\s*transientCredential = '';\s*dialog\.close\(\);/)
  assert.ok(backend.includes('newCredentialSlotChecked = true'))
  assert.ok(backend.includes('staleCredentialSlot'))
  assert.ok(backend.includes('model.authentication.dormant'))
  assert.ok(backend.includes("status.setAttribute('role', error ? 'alert' : 'status')"))
  assert.ok(backend.includes("status.setAttribute('aria-live', error ? 'assertive' : 'polite')"))
  const persist = backend.slice(backend.indexOf('function persistEnvironment'), backend.indexOf('function selectAndReload'))
  const dormant = persist.indexOf('var dormantEnvironment = Object.assign({}, environment, { authRef: null })')
  const removeOldSecret = persist.indexOf("operation: 'delete'")
  const enableNewAuth = persist.indexOf("writeEnvironment(environment, dormantResult.revision, 'upsert')")
  assert.ok(dormant >= 0 && dormant < removeOldSecret && removeOldSecret < enableNewAuth)
  assert.ok(backend.includes("class: 'backend-dialog-form backend-dialog-form--scroll'"))
  assert.ok(backend.includes("class: 'backend-dialog-scroll'"))
  assert.ok(backend.includes("dialog.classList.add('backend-dialog--source')"))
  assert.match(css, /\.backend-dialog-form--scroll\s*\{[\s\S]*?flex:\s*1 1 auto;[\s\S]*?grid-template-rows:\s*minmax\(0, 1fr\) auto;/)
  assert.match(css, /\.backend-dialog-scroll\s*\{[\s\S]*?overflow:\s*auto;/)
  assert.match(css, /\.backend-dialog--source \.backend-dialog-body\s*\{\s*height:\s*min\(86vh, 760px\);\s*\}/)

  const currentSource = { sourceKind: 'openapi', sourceUrl: 'https://api.example.com/openapi.json', authKind: 'bearer' }
  assert.equal(sourceCredentialMustReset(currentSource, { state: 'configured', dormant: false }, currentSource), false)
  assert.equal(sourceCredentialMustReset(currentSource, { state: 'configured', dormant: false }, {
    ...currentSource, sourceUrl: 'https://other.example.com/openapi.json'
  }), true)
  assert.equal(sourceCredentialMustReset(currentSource, { state: 'not-required', dormant: true }, {
    ...currentSource, authKind: 'x-api-key'
  }), true)
  assert.equal(sourceCredentialMustReset(currentSource, { state: 'missing', dormant: false }, {
    ...currentSource, sourceKind: 'postman'
  }), false)

  const postmanEnvironment = { id: 'dev', sourceKind: 'postman', authRef: 'dev' }
  const invalidPmak = dialogProbeOutcome({
    error: { code: 'auth-invalid' },
    result: { resolution: { detectedKind: 'postman', reason: 'postman-link' } }
  }, postmanEnvironment)
  assert.equal(invalidPmak.state, 'replace-credential')
  assert.equal(invalidPmak.errorCode, 'auth-invalid')

  assert.equal(dialogProbeOutcome({
    error: { code: 'source-unreachable' },
    result: { resolution: { detectedKind: 'postman', reason: 'postman-link' } }
  }, postmanEnvironment).state, 'failed')

  const picker = dialogProbeOutcome({
    error: { code: 'invalid-postman' },
    result: { resolution: { detectedKind: 'postman', candidates: [{ uid: '12345678-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee' }] } }
  }, postmanEnvironment)
  assert.equal(picker.state, 'resolution')

  assert.equal(dialogProbeOutcome({
    error: { code: 'auth-rejected' }, result: {}
  }, { id: 'dev', sourceKind: 'openapi', authRef: null }).state, 'require-credential')

  const protectedCandidate = dialogProbeOutcome({
    error: { code: 'source-content-type' },
    result: { resolution: { reason: 'auth-required' } }
  }, { id: 'dev', sourceKind: 'openapi', authRef: 'dev' })
  assert.equal(protectedCandidate.state, 'replace-credential')
  assert.equal(protectedCandidate.errorCode, 'auth-rejected')

  for (const errorCode of ['auth-missing', 'auth-invalid', 'auth-rejected']) {
    const configured = dialogProbeOutcome({ error: { code: errorCode }, result: {} },
      { id: 'dev', sourceKind: 'openapi', authRef: 'dev' })
    assert.equal(configured.state, 'replace-credential', errorCode)
    assert.equal(configured.errorCode, errorCode)
  }
  for (const errorCode of ['auth-missing', 'auth-rejected']) {
    assert.equal(dialogProbeOutcome({ error: { code: errorCode }, result: {} },
      { id: 'dev', sourceKind: 'openapi', authRef: null }).state, 'require-credential', errorCode)
  }
  assert.equal(dialogProbeOutcome({ error: { code: 'source-too-large' }, result: {} },
    { id: 'dev', sourceKind: 'openapi', authRef: null }).state, 'failed')
  assert.equal(dialogProbeOutcome({ error: null, result: {} },
    { id: 'dev', sourceKind: 'openapi', authRef: null }).state, 'success')
})

test('actionable fresh resolution replaces the contradictory failed-preview error banner', () => {
  const environment = { id: 'dev', sourceKind: 'postman', authRef: 'dev' }
  const base = { state: 'failed', error: { code: 'source-content-type' }, environmentRevision: 'revision-2' }

  assert.equal(previewFailureNeedsErrorBanner({
    ...base,
    resolution: { detectedKind: 'postman', candidates: [{ uid: '12345678-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee' }] }
  }, environment, 'revision-2'), false)
  assert.equal(previewFailureNeedsErrorBanner({
    ...base,
    resolution: { reason: 'auth-required' }
  }, environment, 'revision-2'), false)
  assert.equal(previewFailureNeedsErrorBanner({
    ...base,
    resolution: { state: 'unrecognized', reason: 'unrecognized' }
  }, environment, 'revision-2'), true)
  assert.equal(previewFailureNeedsErrorBanner({
    ...base,
    environmentRevision: 'revision-1',
    resolution: { detectedKind: 'postman', candidates: [{ uid: '12345678-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee' }] }
  }, environment, 'revision-2'), true)
})

test('Postman resolution Apply plans preserve E16 guidance and stop empty E15/E17 loops', () => {
  const current = { id: 'dev', label: 'Dev', sourceKind: 'openapi', sourceUrl: 'https://www.postman.com/',
    postmanEnrichmentUrl: 'https://dev.example/postman.json', authRef: null, authKind: 'bearer' }
  const detected = { detectedKind: 'postman', reason: 'postman-link', method: 'postman-link' }
  const missing = resolutionApplyPlan(current, { state: 'missing' }, detected, null)
  assert.deepEqual(missing.environment, { id: 'dev', label: 'Dev', sourceKind: 'postman',
    sourceUrl: 'https://www.postman.com/', postmanEnrichmentUrl: null, authRef: 'dev', authKind: 'x-api-key' })
  assert.equal(missing.deleteCredential, false)
  assert.equal(missing.openCredential, true)

  const configuredBearer = resolutionApplyPlan({ ...current, authRef: 'dev' }, { state: 'configured' }, detected, null)
  assert.equal(configuredBearer.deleteCredential, true)
  assert.equal(configuredBearer.openCredential, true)
  assert.equal(resolutionApplyPlan({ ...current, authRef: 'dev' }, { state: 'invalid' }, detected, null).deleteCredential, true)
  assert.equal(resolutionApplyPlan({ ...current, authRef: 'dev' }, { state: 'missing', dormant: true }, detected, null).deleteCredential, true)

  const postmanCurrent = { ...configuredBearer.environment, sourceUrl: 'https://www.postman.com/' }
  assert.equal(resolutionApplyPlan(postmanCurrent, { state: 'configured' }, detected, null), null)
  const picked = resolutionApplyPlan(postmanCurrent, { state: 'configured' }, detected,
    { uid: '12345678-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee', title: 'Chosen' })
  assert.equal(picked.environment.sourceUrl,
    'https://api.getpostman.com/collections/12345678-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee')
  assert.equal(picked.deleteCredential, false)
  assert.equal(picked.openCredential, false)
  const dormantPick = resolutionApplyPlan(postmanCurrent, { state: 'configured', dormant: true }, detected,
    { uid: '12345678-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee' })
  assert.equal(dormantPick.deleteCredential, true)
  assert.equal(dormantPick.openCredential, true)
  const offPostmanPick = resolutionApplyPlan(postmanCurrent, { state: 'configured' }, detected,
    { url: 'https://api.example.com/collection.json' })
  assert.equal(offPostmanPick.deleteCredential, true)
  assert.equal(offPostmanPick.openCredential, true)

  const apiUrl = 'https://api.getpostman.com/collections/12345678-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'
  const alreadyApplied = { ...postmanCurrent, sourceUrl: apiUrl }
  assert.equal(resolutionApplyPlan(alreadyApplied, { state: 'configured' },
    { detectedKind: 'postman', resolvedUrl: apiUrl }, { url: apiUrl }), null)
  assert.equal(resolutionApplyPlan(alreadyApplied, { state: 'invalid' },
    { detectedKind: 'postman', resolvedUrl: apiUrl }, { url: apiUrl }).openCredential, true)
})

test('header status uses source/environment/auth/contract language rather than gate/drift labels', () => {
  for (const key of ['backend.field.source', 'backend.field.environment', 'backend.field.authentication', 'backend.field.contract']) {
    assert.ok(status.includes(key), key)
  }
  assert.equal(status.includes("backend.pop.gateLabel"), false)
  assert.equal(status.includes("backend.pop.drift"), false)
  assert.equal(status.includes("backend.snapshot.label"), false)
})

test('EN/RU/UA typed Backend keys stay in parity and frontend modules parse', () => {
  const backendIntegration = { en: backendEn, ru: backendRu, uk: backendUk }
  function backendKeys(locale) {
    return Object.keys(backendIntegration[locale]).sort()
  }
  assert.deepEqual(backendKeys('en'), backendKeys('ru'))
  assert.deepEqual(backendKeys('en'), backendKeys('uk'))
  for (const locale of ['en', 'ru', 'uk']) {
    assert.doesNotMatch(backendIntegration[locale]['backend.resolution.postmanPicker'], /neither action|действий нет|дій немає/i)
  }
  assert.match(backendEn['backend.error.auth-rejected'], /source returned 401\/403/i)
  assert.match(backendRu['backend.error.auth-rejected'], /Источник вернул 401\/403/)
  assert.match(backendUk['backend.error.auth-rejected'], /Джерело повернуло 401\/403/)
  assert.match(backendEn['backend.credential.help'], /issued by the API[\s\S]*without the “Bearer ” prefix/)
  assert.match(backendRu['backend.credential.help'], /выдаёт подключаемый API[\s\S]*без префикса «Bearer »/)
  assert.match(backendUk['backend.credential.help'], /видає API[\s\S]*без префікса «Bearer »/)
  for (const dictionary of [backendEn, backendRu, backendUk]) {
    assert.match(dictionary['backend.credential.helpApiKey'], /Settings[\s\S]*Account settings[\s\S]*API keys[\s\S]*Generate API Key/)
    assert.match(dictionary['backend.credential.helpApiKey'], /PMAK-[\s\S]*PMAT/)
    assert.match(dictionary['backend.error.auth-rejected'], /401\/403/)
    assert.ok(dictionary['backend.source.detected.postman'])
    assert.ok(dictionary['backend.source.credentialOptional.x-api-key'])
    assert.ok(dictionary['backend.preview.applyHint'])
    assert.equal(dictionary['backend.source.kind'], undefined)
    assert.equal(dictionary['backend.source.authKind'], undefined)
  }
  assert.ok(backendUk['backend.job.testing'].endsWith('…'))
  for (const file of [backendFile, statusFile]) {
    const checked = spawnSync(process.execPath, ['--check', file], { encoding: 'utf8' })
    assert.equal(checked.status, 0, checked.stderr)
  }
})

test('template surfaces exclude product Backend config and secret stores', () => {
  const projectConfig = readFileSync(join(REPO, 'orchestrator', 'project-config.md'), 'utf8')
  if (/^productName: <Product>$/m.test(projectConfig)) {
    assert.equal(existsSync(join(REPO, 'orchestrator', 'api-contract', 'environments.json')), false)
  }
  const generatorPath = join(REPO, 'orchestrator', 'template-sync', '_generate_template_manifest.py')
  const generator = readFileSync(generatorPath, 'utf8')
  assert.ok(generator.includes('"orchestrator/api-contract/environments.json"'))
  assert.ok(generator.includes('"orchestrator/api-contract/.secrets/"'))
  const generated = spawnSync('python3', [generatorPath, '--print'], { cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 })
  assert.equal(generated.status, 0, generated.stderr)
  assert.ok(JSON.parse(generated.stdout).files['orchestrator/api-contract/scripts/resolve-current.mjs'])
  const staticServer = readFileSync(join(REPO, 'orchestrator', 'site', 'server', 'static.js'), 'utf8')
  assert.ok(staticServer.includes("seg.charAt(0) === '.'"))
})

test('current-generation consumers never prescribe retired root snapshot paths', () => {
  const surfaces = [
    ...filesUnder(join(REPO, 'orchestrator', 'skills'), '.md'),
    ...filesUnder(join(REPO, 'orchestrator', 'site', 'scripts'), '.js'),
    join(REPO, 'orchestrator', 'launch.md'),
    join(REPO, 'orchestrator', 'project-config.md'),
  ]
  const forbidden = /(?:orchestrator\/api-contract\/)?manifests\/endpoint-inventory\.json|manifests\/areas\/<area>\.json/
  for (const file of surfaces) assert.doesNotMatch(readFileSync(file, 'utf8'), forbidden, file)

  const packageJson = JSON.parse(readFileSync(join(REPO, 'orchestrator', 'api-contract', 'package.json'), 'utf8'))
  assert.equal(packageJson.scripts['contract:paths'], 'node scripts/resolve-current.mjs')
  const skill = readFileSync(join(REPO, 'orchestrator', 'skills', 'backend-contract-client', 'SKILL.md'), 'utf8')
  assert.ok(skill.includes('npm run --silent contract:paths'))
})

test('retired interactive contract session keys and dead contract state fields are absent', () => {
  const sessions = readFileSync(join(REPO, 'orchestrator', 'site', 'server', 'sessions.js'), 'utf8')
  assert.ok(sessions.includes("return key === 'contract:diff'"))
  assert.equal(sessions.includes("/^[a-z]+$/.test(key.slice('contract:'.length))"), false)

  assert.equal(existsSync(join(REPO, 'orchestrator', 'site', 'server', 'api-contract.js')), false)
  const state = readFileSync(join(REPO, 'orchestrator', 'site', 'server', 'state.js'), 'utf8')
  assert.doesNotMatch(state, /contractMod|contract:\s*contractMod\.status/)

  for (const sample of ['openapi.sample.json', 'postman.sample.json']) {
    const text = readFileSync(join(REPO, 'orchestrator', 'api-contract', 'scripts', 'examples', sample), 'utf8')
    assert.equal(text.includes('contract:pull -- --file'), false)
    assert.equal(text.includes('contract:postman -- --file'), false)
  }
})

test('auto contract mode never downgrades endpoint work to task-authored shapes', () => {
  const consumers = [
    join(REPO, 'orchestrator', 'project-config.md'),
    join(REPO, 'orchestrator', 'launch.md'),
    join(REPO, 'orchestrator', 'site', 'scripts', 'panels', 'setup.js'),
    join(REPO, 'orchestrator', 'skills', 'backend-contract-client', 'SKILL.md'),
    join(REPO, 'orchestrator', 'skills', 'backend-contract-client', 'references', 'overview.md'),
    join(REPO, 'orchestrator', 'skills', 'backend-contract-client', 'references', 'endpoint-inventory.md'),
    join(REPO, 'orchestrator', 'skills', 'data-layer', 'references', 'cookbook-endpoint.md'),
    join(REPO, 'orchestrator', 'skills', 'task-orchestrator', 'references', 'context-finder.md'),
  ]
  const forbidden = /fall(?:s)? back to (?:the )?(?:manual contract|manual flow|manual check|task text)|manual greenfield flow|фолбэк на ручной контракт/i
  for (const file of consumers) assert.doesNotMatch(readFileSync(file, 'utf8'), forbidden, file)
  for (const locale of ['en', 'ru']) {
    assert.doesNotMatch(localeText(locale), forbidden, `locale:${locale}`)
  }
  assert.match(readFileSync(join(REPO, 'orchestrator', 'skills', 'data-layer', 'SKILL.md'), 'utf8'),
    /gate `auto` or `true`[\s\S]{0,160}snapshot or endpoint is missing[\s\S]{0,80}`BLOCKED`/)
})

test('Project to API uses generation-bound endpoints and stable workspace tabs', () => {
  const panel = readFileSync(join(REPO, 'orchestrator', 'site', 'scripts', 'panels', 'api.js'), 'utf8')
  assert.match(panel, /TABS = \['overview', 'endpoints', 'changes', 'diagnostics'\]/)
  assert.ok(panel.includes("from '../api/overview.js'"))
  assert.ok(panel.includes("from '../api/endpoints.js'"))
  assert.ok(panel.includes('payloadKey(payload)'))
  assert.ok(panel.includes('generationKey !== next'))
  const readme = readFileSync(join(REPO, 'orchestrator', 'site', 'README.md'), 'utf8')
  assert.equal(readme.includes('legacy snapshot when\n  no generation pointer exists'), false)
})

test('coverage planning cannot bypass the selected generation through fixture environment paths', () => {
  const suggest = readFileSync(join(REPO, 'orchestrator', 'api-contract', 'scripts', 'suggest-endpoint-tasks.mjs'), 'utf8')
  assert.equal(suggest.includes('process.env.CONTRACT_SUGGEST_INVENTORY ||'), false)
  assert.equal(suggest.includes('process.env.CONTRACT_SUGGEST_OUT ||'), false)
  assert.ok(suggest.includes('unsupported contract:suggest environment override'))
})
