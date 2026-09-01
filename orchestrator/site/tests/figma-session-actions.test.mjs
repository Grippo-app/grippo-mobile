#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createServer } from 'node:http'
import { spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import { chmodSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = mkdtempSync(join(tmpdir(), 'figma-session-actions-'))
const scratch = mkdtempSync(join(tmpdir(), 'figma-session-actions-scratch-'))
const orchestrator = join(root, 'orchestrator')
const tasks = join(orchestrator, 'tasks')
const cache = join(orchestrator, '.cache', 'tasks')
for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasks, column), { recursive: true })
for (const dir of ['locks', 'requests', 'request-reservations', 'runs', 'superseded', 'finalizations', 'creations', 'edits', 'intake']) {
  mkdirSync(join(cache, dir), { recursive: true })
}
if (process.platform !== 'win32') chmodSync(join(cache, 'intake'), 0o700)
if (process.platform !== 'win32') chmodSync(scratch, 0o700)
writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify({
  version: 2,
  generatedAt: '1970-01-01T00:00:00.000Z',
  backlog: [],
  pending: [],
  todo: [],
  done: []
}, null, 2) + '\n')

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_TASKS_DIR = tasks
process.env.ORCHESTRATOR_LOCKS_DIR = join(cache, 'locks')
process.env.ORCHESTRATOR_REQUESTS_DIR = join(cache, 'requests')
process.env.ORCHESTRATOR_REQUEST_RESERVATIONS_DIR = join(cache, 'request-reservations')
process.env.ORCHESTRATOR_RUNS_DIR = join(cache, 'runs')
process.env.ORCHESTRATOR_SUPERSEDED_DIR = join(cache, 'superseded')
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = join(cache, 'finalizations')
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = join(cache, 'creations')
process.env.ORCHESTRATOR_TASK_EDITS_DIR = join(cache, 'edits')
process.env.ORCHESTRATOR_TASK_INTAKE_DIR = join(cache, 'intake')
process.env.ORCHESTRATOR_STATE_FILE = join(orchestrator, '.cache', 'site', '.site-state.json')
process.env.SHALLOW_INTAKE_SCRATCH_DIR = scratch
process.env.RUNNER_DISABLED = '1'

const require = createRequire(import.meta.url)
const figma = require('../server/figma.js')
const finalizations = require('../server/finalizations.js')
const sessions = require('../server/sessions.js')
const actions = require('../server/figma-session-actions.js')

const fileKey = 'AbCdEfGh1234'
const projectConfigFile = join(orchestrator, 'project-config.md')
writeFileSync(projectConfigFile, [
  '---',
  'productName: Fixture',
  'figmaEnabled: true',
  'figmaLibraryUrl: https://www.figma.com/design/' + fileKey + '/Fixture?node-id=12-34',
  '---',
  ''
].join('\n'))

const originals = {
  figmaStatus: figma.status,
  figmaAccount: figma.account,
  sessionAdmission: figma.sessionAdmission,
  status: sessions.status,
  start: sessions.start,
  send: sessions.send
}

let checks = 0
async function check(name, fn) {
  await fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

let server = null
try {
  figma.status = () => ({ verificationNonce: '0123456789abcdef0123456789abcdef' })
  figma.account = () => ({ email: 'fixture@example.test', handle: 'fixture' })
  writeFileSync(join(tasks, 'todo', 'TASK_1_fixture.md'), [
    '# Fixture',
    '',
    '## Design',
    `- Home — https://www.figma.com/design/${fileKey}/Fixture?node-id=12-34`,
    ''
  ].join('\n'))

  await check('resolver binds every active action id to its exact key', async () => {
    const pairs = [
      ['figma:whoami', 'whoami'],
      ['figma:screens:TASK_1_fixture', 'screen-pull'],
      ['figma:screens:TASK_1_fixture', 'screen-drift'],
      ['figma:rebundle:TASK_1_fixture', 'rebundle'],
      ['figma:shipdriftsweep', 'ship-drift-sweep']
    ]
    const resolved = []
    for (const [key, action] of pairs) {
      const result = await actions.resolveAction(key, action)
      assert.equal(result.ok, true, `${key} / ${action}`)
      assert.equal(result.action, action)
      assert.ok(result.prompt.length > 100)
      resolved.push(result)
    }

    const pull = resolved[1]
    const drift = resolved[2]
    assert.notEqual(pull.prompt, drift.prompt)
    assert.match(pull.prompt, /Pull the per-screen Figma design cache/)
    assert.match(pull.prompt, /screen-token-plans\/tokplan_/)
    assert.match(drift.prompt, /has DRIFTED/)

    const mismatch = await actions.resolveAction('figma:shipdriftsweep', 'screen-pull')
    assert.equal(mismatch.ok, false)
    assert.equal(mismatch.error, 'figma-action-key-mismatch')

    // The retired component-drift session key never resolves again — component
    // comparison is a local deterministic runner, not a browser session.
    const retiredComponentDrift = await actions.resolveAction('figma:componentdrift', 'component-drift')
    assert.equal(retiredComponentDrift.ok, false, 'figma:componentdrift must not resolve')

    const whoami = resolved[0]
    assert.match(whoami.prompt, /0123456789abcdef0123456789abcdef/)

    // Retired token session keys never resolve again: the token capture is
    // the internal sync-tokens scope and the comparison is a local runner.
    for (const [key, action] of [['figma:variables', 'variables'], ['figma:styles', 'styles'], ['figma:tokendrift', 'token-drift']]) {
      const gone = await actions.resolveAction(key, action)
      assert.equal(gone.ok, false, `${key} must not resolve`)
    }
  })

  await check('runtime enablement cannot outrun startup initialization', () => {
    const gate = require('../server/figma-feature-gate.js')
    const disabled = {
      ok: true, figmaEnabledState: 'selected', figmaEnabled: false,
      hasFigmaEnabledField: true, revision: 'sha256:' + '1'.repeat(64),
    }
    const enabled = {
      ok: true, figmaEnabledState: 'selected', figmaEnabled: true,
      hasFigmaEnabledField: true, revision: 'sha256:' + '2'.repeat(64),
    }
    const restartRequired = gate._test.evaluate(disabled, enabled)
    assert.deepEqual(restartRequired, {
      enabled: false, status: 503, error: 'figma-restart-required',
    })
    assert.deepEqual(gate._test.publicState(restartRequired, enabled), {
      state: 'restart-required',
      reasonCode: 'figma-restart-required',
      configRevision: enabled.revision,
      canEnable: false,
    })
    assert.deepEqual(gate._test.evaluate(enabled, disabled), {
      enabled: false, status: 409, error: 'figma-disabled',
    })
  })

  await check('file-scoped sync actions fail closed when the canonical file key is invalid', async () => {
    const result = await actions.resolveServerAction('figma:sync-tokens', 'sync-tokens', {
      figmaFileKey: 'not a figma key',
      jobId: 'fsj-' + '1'.repeat(32),
      inputFingerprint: 'sha256:' + '2'.repeat(64),
      fileKeyFingerprint: 'sha256:' + '3'.repeat(64),
      stagePath: 'orchestrator/.cache/figma/generations/fsj-' + '1'.repeat(32) + '/tokens',
      capturePlanPath: 'orchestrator/.cache/figma/generations/fsj-' + '1'.repeat(32) + '/tokens/capture-plan.json'
    })
    assert.equal(result.ok, false)
    assert.equal(result.error, 'figma-file-key-invalid')
  })

  await check('browser cannot invoke internal probe or sync actions and server context is exact', async () => {
    for (const [key, action] of [
      ['figma:fileaccess', 'file-access'],
      ['figma:sync-tokens', 'sync-tokens'],
      ['figma:sync-components', 'sync-components'],
      ['figma:sync-drift', 'sync-drift']
    ]) {
      const publicResult = await actions.resolveAction(key, action)
      assert.equal(publicResult.ok, false)
      assert.equal(publicResult.error, 'figma-internal-action-forbidden')
    }
    const malformed = await actions.resolveServerAction('figma:sync-tokens', 'sync-tokens', {
      figmaFileKey: fileKey,
      jobId: 'fsj-' + '1'.repeat(32),
      inputFingerprint: 'sha256:' + '2'.repeat(64),
      fileKeyFingerprint: 'sha256:' + '3'.repeat(64),
      stagePath: '../../outside',
      capturePlanPath: '../../outside/capture-plan.json'
    })
    assert.equal(malformed.ok, false)
    assert.equal(malformed.error, 'figma-server-context-invalid')
    const valid = await actions.resolveServerAction('figma:sync-tokens', 'sync-tokens', {
      figmaFileKey: fileKey,
      jobId: 'fsj-' + '1'.repeat(32),
      inputFingerprint: 'sha256:' + '2'.repeat(64),
      fileKeyFingerprint: 'sha256:' + '3'.repeat(64),
      stagePath: 'orchestrator/.cache/figma/generations/fsj-' + '1'.repeat(32) + '/tokens',
      capturePlanPath: 'orchestrator/.cache/figma/generations/fsj-' + '1'.repeat(32) + '/tokens/capture-plan.json'
    })
    assert.equal(valid.ok, true)
    assert.match(valid.prompt, /server-owned staging directory/)
    assert.match(valid.prompt, /capture-plan\.json/)
    assert.match(valid.prompt, /Do not write artifacts\.json/i)
    assert.doesNotMatch(valid.prompt, /undefined/)

    // The comparison scope is a local deterministic runner now — no session
    // action may resolve for it under any context.
    const localDrift = await actions.resolveServerAction('figma:sync-drift', 'sync-drift', {
      jobId: 'fsj-' + '6'.repeat(32),
      inputFingerprint: 'sha256:' + '7'.repeat(64),
      fileKeyFingerprint: 'sha256:' + '8'.repeat(64),
      stagePath: 'orchestrator/.cache/figma/generations/fsj-' + '6'.repeat(32) + '/drift'
    })
    assert.equal(localDrift.ok, false)
    assert.equal(localDrift.error, 'figma-server-context-invalid')

    const accessNonce = '4'.repeat(32)
    const fileAccess = await actions.resolveServerAction('figma:fileaccess', 'file-access', {
      figmaFileKey: fileKey,
      accessNonce,
      accountFingerprint: 'sha256:' + '5'.repeat(64),
      receiptPath: 'orchestrator/.cache/figma/integration/file-access-' + accessNonce + '.json'
    })
    assert.equal(fileAccess.ok, true)
    assert.match(fileAccess.prompt, /fileName is optional display metadata, not access evidence/)
    assert.match(fileAccess.prompt, /otherwise write an empty string/)
    assert.match(fileAccess.prompt, /Never infer it from a page\/frame name/)
    assert.match(fileAccess.prompt, /UTC YYYY-MM-DDTHH:mm:ssZ or YYYY-MM-DDTHH:mm:ss\.sssZ/)
    assert.match(fileAccess.prompt, /Do not use a Figma REST API or token fallback/)
  })

  await check('ship drift sweep owns a globally exclusive writer lease in both acquisition orders', async () => {
    const screenKey = 'figma:screens:TASK_1_fixture'
    assert.equal(sessions.writerLeaseKeyFor('figma:shipdriftsweep'), 'figma:ship-drift-artifacts')
    assert.equal(sessions.writerLeaseRequiresSoleWriter('figma:shipdriftsweep'), true)

    const screen = finalizations.beginMutation({
      kind: 'workspace-session', stem: 'TASK_1_fixture',
      sessionId: finalizations.createWriterSessionId(), key: sessions.writerLeaseKeyFor(screenKey)
    })
    assert.equal(screen.ok, true)
    try {
      const sweepAfterScreen = finalizations.beginMutation({
        kind: 'workspace-session', stem: null,
        sessionId: finalizations.createWriterSessionId(), key: sessions.writerLeaseKeyFor('figma:shipdriftsweep'),
        requireSoleWriter: sessions.writerLeaseRequiresSoleWriter('figma:shipdriftsweep')
      })
      assert.equal(sweepAfterScreen.ok, false)
    } finally {
      assert.equal(finalizations.endMutation(screen.handle), true)
    }

    const sweep = finalizations.beginMutation({
      kind: 'workspace-session', stem: null,
      sessionId: finalizations.createWriterSessionId(), key: sessions.writerLeaseKeyFor('figma:shipdriftsweep'),
      requireSoleWriter: sessions.writerLeaseRequiresSoleWriter('figma:shipdriftsweep')
    })
    assert.equal(sweep.ok, true)
    try {
      const screenAfterSweep = finalizations.beginMutation({
        kind: 'workspace-session', stem: 'TASK_1_fixture',
        sessionId: finalizations.createWriterSessionId(), key: sessions.writerLeaseKeyFor(screenKey)
      })
      assert.equal(screenAfterSweep.ok, false)
    } finally {
      assert.equal(finalizations.endMutation(sweep.handle), true)
    }
  })

  await check('Figma terminal exposes the same free-text rail before revealing the overlay', async () => {
    const [{ terminal }, { dom }, { tasksApi }] = await Promise.all([
      import('../scripts/terminal.js'), import('../scripts/dom.js'), import('../scripts/data/tasks-api.js'),
    ])
    const originalEl = dom.el
    const originalEvents = tasksApi.sessionEvents
    const originalDocument = globalThis.document
    const originalWindow = globalThis.window
    const changes = []
    function node(tag, attrs) {
      const children = []
      const value = {
        tag, className: '', textContent: '', value: '', children, parentNode: null,
        appendChild(child) { children.push(child); child.parentNode = this; return child },
        removeChild(child) { const at = children.indexOf(child); if (at >= 0) children.splice(at, 1) },
        addEventListener() {}, removeEventListener() {}, setAttribute() {}, removeAttribute() {},
        querySelectorAll() { return [] }, focus() {},
      }
      Object.defineProperty(value, 'firstChild', { get() { return children[0] || null } })
      for (const property of ['hidden', 'disabled']) {
        let current = false
        Object.defineProperty(value, property, {
          get() { return current },
          set(next) { current = next; changes.push({ className: value.className, property, value: next }) },
        })
      }
      for (const [key, entry] of Object.entries(attrs || {})) {
        if (key === 'class') value.className = entry
        else if (key === 'text') value.textContent = entry
        else if (key !== 'attrs') value[key] = entry
      }
      return value
    }
    try {
      const body = node('body')
      globalThis.document = {
        body, activeElement: null,
        addEventListener() {}, removeEventListener() {},
      }
      globalThis.window = { addEventListener() {}, removeEventListener() {} }
      dom.el = node
      tasksApi.sessionEvents = () => new Promise(() => {})
      terminal.open('figma:file:fixture')
      const inputReady = changes.findIndex((row) => row.className.includes('terminal__input') && row.property === 'disabled' && row.value === false)
      const sendReady = changes.findIndex((row) => row.className === 'btn btn--primary' && row.property === 'disabled' && row.value === false)
      const reveal = changes.findIndex((row) => row.className === 'terminal' && row.property === 'hidden' && row.value === false)
      assert.ok(inputReady >= 0 && sendReady >= 0 && reveal > inputReady && reveal > sendReady)
      terminal.close()
    } finally {
      dom.el = originalEl
      tasksApi.sessionEvents = originalEvents
      globalThis.document = originalDocument
      globalThis.window = originalWindow
    }
  })

  await check('Figma terminal clears a foreign draft and enables input before revealing the switched context', async () => {
    class FakeElement {
      constructor(tagName, onReveal) {
        this.tagName = String(tagName).toUpperCase()
        this.children = []
        this.parentNode = null
        this.attributes = new Map()
        this.listeners = Object.create(null)
        this.className = ''
        this.textContent = ''
        this.value = ''
        this.disabled = false
        this._hidden = false
        this.onReveal = onReveal
      }
      get firstChild() { return this.children[0] || null }
      get hidden() { return this._hidden }
      set hidden(value) {
        this._hidden = !!value
        if (!this._hidden && this.className === 'terminal') this.onReveal(this)
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
      addEventListener(name, handler) { this.listeners[name] = handler }
      setAttribute(name, value) { this.attributes.set(name, String(value)) }
      removeAttribute(name) { this.attributes.delete(name) }
      querySelectorAll() { return [] }
      focus() { fakeDocument.activeElement = this }
    }
    const findClass = (root, className) => {
      if (root.className === className) return root
      for (const child of root.children || []) {
        const found = findClass(child, className)
        if (found) return found
      }
      return null
    }

    const originalDocument = globalThis.document
    const originalWindow = globalThis.window
    const revealStates = []
    const onReveal = (overlay) => {
      const input = findClass(overlay, 'input terminal__input')
      revealStates.push({ value: input && input.value, hidden: input && input.hidden, disabled: input && input.disabled })
    }
    const fakeDocument = {
      body: null,
      activeElement: null,
      createElement: (tagName) => new FakeElement(tagName, onReveal),
      createTextNode: (text) => Object.assign(new FakeElement('#text', onReveal), { textContent: text }),
      addEventListener() {},
      removeEventListener() {},
    }
    fakeDocument.body = fakeDocument.createElement('body')
    fakeDocument.activeElement = fakeDocument.createElement('button')
    globalThis.document = fakeDocument
    globalThis.window = { addEventListener() {}, removeEventListener() {} }

    const { tasksApi } = await import('../scripts/data/tasks-api.js')
    const originalSessionEvents = tasksApi.sessionEvents
    let terminalUi = null
    try {
      tasksApi.sessionEvents = () => new Promise(() => {})
      ;({ terminal: terminalUi } = await import('../scripts/terminal.js?figma-session-switch-test'))

      terminalUi.open('figma:whoami')
      const overlay = findClass(fakeDocument.body, 'terminal')
      const input = findClass(overlay, 'input terminal__input')
      const transcript = findClass(overlay, 'terminal__body')
      input.value = 'draft for the first Figma context'
      transcript.appendChild(fakeDocument.createElement('div'))

      terminalUi.open('figma:screens:TASK_2_second')

      assert.equal(input.value, '')
      assert.equal(transcript.children.length, 0)
      assert.deepEqual(revealStates.at(-1), { value: '', hidden: false, disabled: false })
    } finally {
      if (terminalUi) terminalUi.close()
      tasksApi.sessionEvents = originalSessionEvents
      if (originalDocument === undefined) delete globalThis.document
      else globalThis.document = originalDocument
      if (originalWindow === undefined) delete globalThis.window
      else globalThis.window = originalWindow
    }
  })

  figma.sessionAdmission = () => null
  const statuses = Object.create(null)
  const starts = []
  const sends = []
  sessions.status = (key) => statuses[key] || null
  sessions.start = (key, meta) => {
    starts.push({ key, meta })
    statuses[key] = { key, running: true, awaitingTurn: true, askedThisTurn: false }
    return statuses[key]
  }
  sessions.send = (key, prompt, meta) => {
    sends.push({ key, prompt, meta })
    statuses[key] = { key, running: true, awaitingTurn: true, askedThisTurn: false }
    return true
  }

  const sync = require('../server/figma-sync.js')
  const testJobs = require('../server/figma-test-job.js')
  await sync.init({ notify: () => {}, testActive: testJobs.busy })
  testJobs.init({ notify: () => {}, syncActive: sync.busy })
  const httpMod = require('../server/http.js')
  server = createServer(httpMod.handle)
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
  const base = `http://127.0.0.1:${server.address().port}`
  const state = await (await fetch(base + '/api/state')).json()
  const headers = {
    'content-type': 'application/json',
    'x-orchestrator-csrf': state.csrfToken,
    origin: base
  }
  const post = (body) => fetch(base + '/api/session/start', { method: 'POST', headers, body: JSON.stringify(body) })
  const send = (body) => fetch(base + '/api/session/send', { method: 'POST', headers, body: JSON.stringify(body) })

  await check('figma-disabled state never reads a leftover account receipt', async () => {
    const enabledConfig = readFileSync(projectConfigFile, 'utf8')
    const enabledAccount = figma.account
    let accountReads = 0
    try {
      writeFileSync(projectConfigFile, enabledConfig.replace('figmaEnabled: true', 'figmaEnabled: false'))
      figma.account = () => { accountReads++; return { email: 'must-not-read@example.test' } }
      const disabled = await (await fetch(base + '/api/state')).json()
      assert.equal(disabled.figma.state, 'disabled')
      assert.equal(disabled.figma.account, null)
      assert.equal(disabled.figmaIntegration, null)
      assert.deepEqual(disabled.figmaFeature, {
        state: 'disabled',
        reasonCode: 'figma-disabled',
        configRevision: disabled.figmaFeature.configRevision,
        canEnable: true
      })
      assert.match(disabled.figmaFeature.configRevision, /^sha256:[a-f0-9]{64}$/)
      const integration = await (await fetch(base + '/api/figma/integration')).json()
      assert.equal(integration.error, 'figma-disabled')
      const startDisabled = await post({ key: 'figma:whoami', figmaAction: 'whoami' })
      assert.equal(startDisabled.status, 409)
      assert.equal((await startDisabled.json()).error, 'figma-disabled')
      assert.equal(accountReads, 0)

      const withoutCsrf = await fetch(base + '/api/figma/enable', {
        method: 'POST',
        headers: { 'content-type': 'application/json', origin: base },
        body: JSON.stringify({ expectedConfigRevision: disabled.figmaFeature.configRevision })
      })
      assert.equal(withoutCsrf.status, 403)
      assert.equal((await withoutCsrf.json()).error, 'bad-csrf')
      const enabled = await fetch(base + '/api/figma/enable', {
        method: 'POST', headers,
        body: JSON.stringify({ expectedConfigRevision: disabled.figmaFeature.configRevision })
      })
      assert.equal(enabled.status, 200)
      const enabledBody = await enabled.json()
      assert.equal(enabledBody.ok, true)
      assert.equal(enabledBody.feature.state, 'enabled')
      assert.match(readFileSync(projectConfigFile, 'utf8'), /figmaEnabled: true/)
    } finally {
      writeFileSync(projectConfigFile, enabledConfig)
      figma.account = enabledAccount
    }
  })

  await check('a non-canonical figma gate is unavailable, never silently disabled', async () => {
    const enabledConfig = readFileSync(projectConfigFile, 'utf8')
    try {
      writeFileSync(projectConfigFile, enabledConfig.replace('figmaEnabled: true', 'figmaEnabled: TRUE'))
      const invalid = await (await fetch(base + '/api/state')).json()
      assert.equal(invalid.figma.state, 'unavailable')
      assert.equal(invalid.figma.configError, 'figma-config-invalid')
      assert.equal(invalid.figmaFeature.state, 'invalid')
      assert.equal(invalid.figmaFeature.canEnable, false)
      const integration = await fetch(base + '/api/figma/integration')
      assert.equal(integration.status, 503)
      assert.equal((await integration.json()).error, 'figma-config-invalid')
      const startInvalid = await post({ key: 'figma:whoami', figmaAction: 'whoami' })
      assert.equal(startInvalid.status, 503)
      assert.equal((await startInvalid.json()).error, 'figma-config-invalid')
    } finally {
      writeFileSync(projectConfigFile, enabledConfig)
    }
  })

  await check('new Figma endpoints inherit CSRF, origin, JSON and public-redaction guards', async () => {
    let response = await fetch(base + '/api/figma/sync/plan', {
      method: 'POST', headers: { 'content-type': 'application/json', origin: base }, body: '{}'
    })
    assert.equal(response.status, 403)
    assert.equal((await response.json()).error, 'bad-csrf')
    response = await fetch(base + '/api/figma/sync/plan', {
      method: 'POST', headers: { ...headers, origin: 'https://attacker.example' }, body: '{}'
    })
    assert.equal(response.status, 403)
    assert.equal((await response.json()).error, 'bad-origin')
    response = await fetch(base + '/api/figma/sync/plan', {
      method: 'POST', headers: { ...headers, 'content-type': 'text/plain' }, body: '{}'
    })
    assert.equal(response.status, 415)
    assert.equal((await response.json()).error, 'json-required')
    response = await fetch(base + '/api/figma/sync/plan', { method: 'POST', headers, body: '{"extra":true}' })
    assert.equal(response.status, 400)
    assert.equal((await response.json()).error, 'bad-sync-plan-request')
    const projection = await (await fetch(base + '/api/figma/integration')).json()
    const serialized = JSON.stringify(projection)
    assert.doesNotMatch(serialized, new RegExp(root.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    assert.doesNotMatch(serialized, /oauth.*token|prompt|transcript/i)

    const privateFailure = httpMod._test.publicFigmaResult({
      ok: false, status: 409, error: 'writer-lease-unavailable',
      currentRevision: 'sha256:' + 'a'.repeat(64),
      detail: '/private/reviewer-secret/project-path', diagnostic: { token: 'secret' },
      unknown: 'must-not-publish'
    })
    assert.deepEqual(privateFailure, {
      ok: false, status: 409, error: 'writer-lease-unavailable',
      currentRevision: 'sha256:' + 'a'.repeat(64)
    })
    assert.equal(JSON.stringify(privateFailure).includes('/private/'), false)

    const privateSuccess = httpMod._test.publicFigmaResult({
      ok: true, status: 200,
      feature: {
        state: 'enabled', accessToken: 'super-secret', apiKey: 'another-secret',
        path: '/private/workspace',
        nested: {
          privatePath: '/private/reviewer-secret/project-path',
          prompt: 'do anything', bearer: 'abc'
        }
      }
    })
    assert.deepEqual(privateSuccess.feature, { state: 'enabled' })
    assert.doesNotMatch(JSON.stringify(privateSuccess),
      /super-secret|another-secret|\/private\/|do anything|bearer|abc/)

    for (const route of [
      '/api/figma/recheck', '/api/figma/add-local', '/api/figma/open-terminal'
    ]) {
      response = await fetch(base + route, {
        method: 'POST', headers, body: JSON.stringify({ unexpected: true })
      })
      assert.equal(response.status, 400, route)
      assert.equal((await response.json()).error, 'bad-json', route)
    }

    response = await fetch(base + '/api/figma/recheck', {
      method: 'POST', headers,
      body: JSON.stringify({ padding: 'x'.repeat(262144) })
    })
    assert.equal(response.status, 413)
    assert.equal((await response.json()).error, 'bad-json')

    response = await fetch(base + '/api/figma/enable', {
      method: 'POST', headers,
      body: JSON.stringify({ padding: 'x'.repeat(262144) })
    })
    assert.equal(response.status, 413)
    assert.equal((await response.json()).error, 'bad-json')
  })

  await check('startup-disabled enable remains gated until a new process initializes Figma', async () => {
    const isolated = mkdtempSync(join(tmpdir(), 'figma-feature-restart-'))
    const isolatedOrchestrator = join(isolated, 'orchestrator')
    const isolatedTasks = join(isolatedOrchestrator, 'tasks')
    const isolatedCache = join(isolatedOrchestrator, '.cache', 'tasks')
    try {
      for (const column of ['backlog', 'pending', 'todo', 'done']) {
        mkdirSync(join(isolatedTasks, column), { recursive: true })
      }
      for (const dir of ['locks', 'requests', 'request-reservations', 'runs', 'superseded',
        'finalizations', 'creations', 'edits', 'intake']) {
        mkdirSync(join(isolatedCache, dir), { recursive: true })
      }
      writeFileSync(join(isolatedTasks, 'INDEX.json'), JSON.stringify({
        version: 2, generatedAt: '1970-01-01T00:00:00.000Z',
        backlog: [], pending: [], todo: [], done: []
      }) + '\n')
      writeFileSync(join(isolatedOrchestrator, 'project-config.md'), [
        '---', 'productName: Restart fixture', 'figmaEnabled: false',
        'figmaLibraryUrl: <figma-library-url>', '---', ''
      ].join('\n'))
      const childEnv = {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: isolated,
        ORCHESTRATOR_TASKS_DIR: isolatedTasks,
        ORCHESTRATOR_LOCKS_DIR: join(isolatedCache, 'locks'),
        ORCHESTRATOR_REQUESTS_DIR: join(isolatedCache, 'requests'),
        ORCHESTRATOR_REQUEST_RESERVATIONS_DIR: join(isolatedCache, 'request-reservations'),
        ORCHESTRATOR_RUNS_DIR: join(isolatedCache, 'runs'),
        ORCHESTRATOR_SUPERSEDED_DIR: join(isolatedCache, 'superseded'),
        ORCHESTRATOR_FINALIZATIONS_DIR: join(isolatedCache, 'finalizations'),
        ORCHESTRATOR_TASK_CREATIONS_DIR: join(isolatedCache, 'creations'),
        ORCHESTRATOR_TASK_EDITS_DIR: join(isolatedCache, 'edits'),
        ORCHESTRATOR_TASK_INTAKE_DIR: join(isolatedCache, 'intake'),
        ORCHESTRATOR_STATE_FILE: join(isolatedOrchestrator, '.cache', 'site', '.site-state.json'),
        SHALLOW_INTAKE_SCRATCH_DIR: scratch,
        RUNNER_DISABLED: '1'
      }
      const childScript = `
        const { createServer } = require('node:http');
        const http = require(${JSON.stringify(join(dirname(fileURLToPath(import.meta.url)), '..', 'server', 'http.js'))});
        const server = createServer(http.handle);
        server.listen(0, '127.0.0.1', async () => {
          try {
            const base = 'http://127.0.0.1:' + server.address().port;
            const before = await (await fetch(base + '/api/state')).json();
            if (process.env.CHILD_MODE === 'enable') {
              const headers = { 'content-type': 'application/json', origin: base,
                'x-orchestrator-csrf': before.csrfToken };
              const enabledResponse = await fetch(base + '/api/figma/enable', { method: 'POST', headers,
                body: JSON.stringify({ expectedConfigRevision: before.figmaFeature.configRevision }) });
              const enabled = await enabledResponse.json();
              const blockedResponse = await fetch(base + '/api/figma/integration');
              const blocked = await blockedResponse.json();
              const after = await (await fetch(base + '/api/state')).json();
              console.log(JSON.stringify({ before: before.figmaFeature, enabledStatus: enabledResponse.status,
                enabled, blockedStatus: blockedResponse.status, blocked, after: after.figmaFeature }));
            } else {
              const integration = await fetch(base + '/api/figma/integration');
              console.log(JSON.stringify({ feature: before.figmaFeature, integrationStatus: integration.status }));
            }
          } catch (error) { console.error(error && error.stack || error); process.exitCode = 1; }
          finally { server.close(); }
        });
      `
      const runChild = (mode) => {
        const result = spawnSync(process.execPath, ['-e', childScript], {
          env: { ...childEnv, CHILD_MODE: mode }, encoding: 'utf8', timeout: 20000
        })
        assert.equal(result.status, 0, result.stderr + result.stdout)
        const line = result.stdout.trim().split('\n').filter(Boolean).at(-1)
        return JSON.parse(line)
      }
      const first = runChild('enable')
      assert.equal(first.before.state, 'disabled')
      assert.equal(first.enabledStatus, 200)
      assert.equal(first.enabled.feature.state, 'restart-required')
      assert.equal(first.after.state, 'restart-required')
      assert.equal(first.blockedStatus, 503)
      assert.equal(first.blocked.error, 'figma-restart-required')
      const restarted = runChild('inspect')
      assert.equal(restarted.feature.state, 'enabled')
      assert.equal(restarted.integrationStatus, 200)

      const ownerScript = `
        const marks = [];
        function replace(modulePath, name, marker, promise) {
          const owner = require(modulePath);
          owner[name] = function () {
            marks.push(marker);
            return promise ? Promise.resolve() : undefined;
          };
        }
        replace(${JSON.stringify(join(dirname(fileURLToPath(import.meta.url)), '..', 'server', 'figma-task-publication.js'))}, 'beginRecovery', 'publication-begin', false);
        replace(${JSON.stringify(join(dirname(fileURLToPath(import.meta.url)), '..', 'server', 'figma-task-publication.js'))}, 'init', 'publication-init', true);
        replace(${JSON.stringify(join(dirname(fileURLToPath(import.meta.url)), '..', 'server', 'figma-test-job.js'))}, 'init', 'test-init', false);
        replace(${JSON.stringify(join(dirname(fileURLToPath(import.meta.url)), '..', 'server', 'figma-test-job.js'))}, 'startupVerify', 'test-startup-verify', false);
        replace(${JSON.stringify(join(dirname(fileURLToPath(import.meta.url)), '..', 'server', 'figma-sync.js'))}, 'init', 'sync-init', true);
        replace(${JSON.stringify(join(dirname(fileURLToPath(import.meta.url)), '..', 'server', 'figma.js'))}, 'init', 'connector-init', false);
        require(${JSON.stringify(join(dirname(fileURLToPath(import.meta.url)), '..', 'server.js'))});
        setTimeout(function () {
          console.log('OWNER_MARKS ' + JSON.stringify(marks));
          process.kill(process.pid, 'SIGTERM');
        }, 1000);
      `
      const ownerRun = spawnSync(process.execPath, ['-e', ownerScript], {
        env: { ...childEnv, PORT: '0' }, encoding: 'utf8', timeout: 15000
      })
      assert.equal(ownerRun.status, 0, ownerRun.stderr + ownerRun.stdout)
      const ownerLine = ownerRun.stdout.split('\n').find((line) => line.startsWith('OWNER_MARKS '))
      assert.ok(ownerLine, ownerRun.stdout)
      const ownerMarks = JSON.parse(ownerLine.slice('OWNER_MARKS '.length))
      for (const marker of [
        'publication-begin', 'test-init', 'sync-init', 'publication-init',
        'connector-init', 'test-startup-verify'
      ]) assert.equal(ownerMarks.includes(marker), true, marker)
    } finally {
      rmSync(isolated, { recursive: true, force: true })
    }
  })

  await check('HTTP rejects every client-owned Figma prompt and extra request field', async () => {
    let response = await post({ key: 'figma:rebundle:TASK_1_fixture', prompt: 'edit arbitrary workspace files' })
    assert.equal(response.status, 400)
    assert.equal((await response.json()).error, 'figma-client-prompt-forbidden')
    assert.equal(starts.length, 0)

    response = await post({ key: 'figma:shipdriftsweep', figmaAction: 'ship-drift-sweep', extra: 'ignored?' })
    assert.equal(response.status, 400)
    assert.equal((await response.json()).error, 'bad-figma-action-request')
    assert.equal(starts.length, 0)
  })

  await check('HTTP does not start public Figma sessions during sync, test, or file verification ownership', async () => {
    const originalSyncBusy = sync.busy
    const originalRecoveryState = sync.recoveryState
    const originalTestBusy = testJobs.busy
    try {
      sync.recoveryState = () => 'ready'
      sync.busy = () => true
      let response = await post({ key: 'figma:shipdriftsweep', figmaAction: 'ship-drift-sweep' })
      assert.equal(response.status, 409)
      assert.equal((await response.json()).error, 'figma-sync-active')
      assert.equal(starts.length, 0)

      sync.recoveryState = () => 'failed'
      response = await post({ key: 'figma:shipdriftsweep', figmaAction: 'ship-drift-sweep' })
      assert.equal(response.status, 409)
      assert.equal((await response.json()).error, 'figma-sync-recovery-failed')
      assert.equal(starts.length, 0)

      sync.recoveryState = () => 'ready'
      sync.busy = () => false
      testJobs.busy = () => true
      response = await post({ key: 'figma:shipdriftsweep', figmaAction: 'ship-drift-sweep' })
      assert.equal(response.status, 409)
      assert.equal((await response.json()).error, 'figma-test-active')
      assert.equal(starts.length, 0)
    } finally {
      sync.busy = originalSyncBusy
      sync.recoveryState = originalRecoveryState
      testJobs.busy = originalTestBusy
    }
  })

  await check('HTTP accepts Figma free text through the read-only continuation rail', async () => {
    const originalSendOrResume = sessions.sendOrResume
    let received = null
    try {
      sessions.sendOrResume = (key, text) => {
        received = { key, text }
        return { sent: true, queued: true, resumed: false }
      }
      const response = await send({ key: 'figma:shipdriftsweep', text: 'explain the current Figma result' })
      assert.equal(response.status, 200)
      const body = await response.json()
      assert.equal(body.sent, true)
      assert.equal(body.queued, true)
      assert.deepEqual(received, { key: 'figma:shipdriftsweep', text: 'explain the current Figma result' })
    } finally {
      sessions.sendOrResume = originalSendOrResume
    }
  })

  await check('HTTP starts only the canonical key/action prompt', async () => {
    let response = await post({ key: 'figma:shipdriftsweep', figmaAction: 'screen-pull' })
    assert.equal(response.status, 400)
    assert.equal((await response.json()).error, 'figma-action-key-mismatch')
    assert.equal(starts.length, 0)

    response = await post({ key: 'figma:screens', figmaAction: 'screen-pull' })
    assert.equal(response.status, 400)
    assert.equal((await response.json()).error, 'bad-key')
    assert.equal(starts.length, 0)

    response = await post({ key: 'figma:shipdriftsweep', figmaAction: 'ship-drift-sweep' })
    assert.equal(response.status, 200)
    assert.equal(starts.length, 1)
    assert.equal(starts[0].key, 'figma:shipdriftsweep')
    assert.equal(starts[0].meta.action, 'ship-drift-sweep')
    assert.match(starts[0].meta.prompt, /Sweep every SHIPPED \(done\/\) UI task for post-ship Figma DRIFT/)
    assert.doesNotMatch(starts[0].meta.prompt, /arbitrary workspace files/)
  })

  await check('HTTP sends a canonical action into an idle live session but never overwrites a question', async () => {
    const key = 'figma:screens:TASK_1_fixture'
    statuses[key] = { key, running: true, awaitingTurn: false, askedThisTurn: false }
    let response = await post({ key, figmaAction: 'screen-drift' })
    assert.equal(response.status, 200)
    assert.equal(sends.length, 1)
    assert.match(sends[0].prompt, /has DRIFTED/)
    assert.equal(sends[0].meta.action, 'screen-drift')

    statuses[key] = { key, running: true, awaitingTurn: false, askedThisTurn: true }
    response = await post({ key, figmaAction: 'screen-pull' })
    assert.equal(response.status, 409)
    assert.equal((await response.json()).error, 'session-busy')
    assert.equal(sends.length, 1)
  })

  await check('full Figma reset is CSRF-protected, idempotent, and preserves project-owned inputs', async () => {
    Object.keys(statuses).forEach((key) => delete statuses[key])
    const figmaDir = join(orchestrator, 'figma')
    const manifests = join(figmaDir, 'manifests')
    const normalized = join(figmaDir, 'tokens', 'normalized')
    const adapters = join(figmaDir, 'project-adapters.json')
    const sourceFile = join(root, 'src', 'keep.txt')
    const generated = [
      join(figmaDir, '.account.json'), join(figmaDir, '.rest-token'), join(figmaDir, '.env'),
      join(figmaDir, 'token-mappings.json'), join(figmaDir, 'component-mappings.json'),
      join(figmaDir, 'tokens', 'source-index.json'), join(figmaDir, 'tokens', 'observed-token-catalog.json'),
      join(figmaDir, 'tokens', 'sources', '000.json'), join(normalized, 'generated.json'),
      join(figmaDir, 'components', 'design-component-inventory.json'),
      join(figmaDir, 'components', 'visual', 'fixture.png'),
      join(manifests, 'generations', 'invalid.json'),
      join(orchestrator, '.cache', 'figma', 'runtime', 'fixture.json')
    ]
    for (const file of generated) {
      mkdirSync(dirname(file), { recursive: true })
      writeFileSync(file, file.endsWith('.png') ? Buffer.from([1, 2, 3]) : '{}\n')
    }
    mkdirSync(manifests, { recursive: true }); writeFileSync(join(manifests, '.gitkeep'), '')
    mkdirSync(normalized, { recursive: true }); writeFileSync(join(normalized, '.gitkeep'), '')
    writeFileSync(adapters, '{"schemaVersion":1}\n')
    mkdirSync(join(root, 'src'), { recursive: true }); writeFileSync(sourceFile, 'keep\n')
    const persistence = require('../server/persistence.js')
    const persisted = persistence.readPersisted()
    persisted.setupForm.figmaLibraryUrl = 'https://www.figma.com/design/' + fileKey
    persistence.writePersisted(persisted)

    const commands = []
    figma._test.setRemoveExecFile((_command, args, _options, callback) => {
      commands.push(args)
      queueMicrotask(() => callback(null, '', ''))
    })
    try {
      const before = (await (await fetch(base + '/api/figma/integration')).json()).integration
      const request = {
        expectedConfigRevision: before.configRevision,
        expectedGenerationId: before.context.generationId,
        idempotencyKey: 'figma-reset:fixture-0001'
      }
      let response = await fetch(base + '/api/figma/integration/reset', {
        method: 'POST', headers: { 'content-type': 'application/json', origin: base }, body: JSON.stringify(request)
      })
      assert.equal(response.status, 403)
      assert.equal((await response.json()).error, 'bad-csrf')

      response = await fetch(base + '/api/figma/integration/reset', {
        method: 'POST', headers,
        body: JSON.stringify({ padding: 'x'.repeat(262144) })
      })
      assert.equal(response.status, 413)
      assert.equal((await response.json()).error, 'bad-json')

      response = await fetch(base + '/api/figma/integration/reset', {
        method: 'POST', headers, body: JSON.stringify(request)
      })
      assert.equal(response.status, 200)
      const result = await response.json()
      assert.equal(result.ok, true)
      assert.deepEqual(commands, [
        ['mcp', 'logout', 'figma'],
        ['mcp', 'remove', '--scope', 'local', 'figma']
      ])
      for (const file of generated) assert.equal(existsSync(file), false, file)
      assert.equal(existsSync(join(manifests, '.gitkeep')), true)
      assert.equal(existsSync(join(normalized, '.gitkeep')), true)
      assert.equal(readFileSync(adapters, 'utf8'), '{"schemaVersion":1}\n')
      assert.equal(readFileSync(sourceFile, 'utf8'), 'keep\n')
      assert.equal(existsSync(join(tasks, 'todo', 'TASK_1_fixture.md')), true)
      assert.match(readFileSync(projectConfigFile, 'utf8'), /figmaLibraryUrl: <figma-library-url>/)
      assert.equal(Object.hasOwn(persistence.readPersisted().setupForm, 'figmaLibraryUrl'), false)
      assert.equal(result.integration.context.resetAvailable, false)

      response = await fetch(base + '/api/figma/integration/reset', {
        method: 'POST', headers, body: JSON.stringify(request)
      })
      assert.equal(response.status, 200)
      assert.equal((await response.json()).ok, true)
      assert.equal(commands.length, 2)

      figma._test.installSnapshot(figma._test.classifyOutput('', '', 0))
      await new Promise((resolve, reject) => figma.removeLocalServer((error) => error ? reject(error) : resolve()))
      assert.deepEqual(commands.slice(2), [
        ['mcp', 'logout', 'figma'],
        ['mcp', 'remove', '--scope', 'local', 'figma']
      ])

      commands.length = 0
      figma._test.setRemoveExecFile((_command, args, _options, callback) => {
        commands.push(args)
        queueMicrotask(() => args[1] === 'logout'
          ? callback(new Error('already logged out'), '', 'No OAuth tokens found')
          : callback(null, '', ''))
      })
      await new Promise((resolve, reject) => figma.removeLocalServer((error) => error ? reject(error) : resolve()))
      assert.deepEqual(commands, [
        ['mcp', 'logout', 'figma'],
        ['mcp', 'remove', '--scope', 'local', 'figma']
      ])

      commands.length = 0
      figma._test.setRemoveExecFile((_command, args, _options, callback) => {
        commands.push(args)
        queueMicrotask(() => callback(new Error('cli unavailable'), '', 'fatal'))
      })
      await assert.rejects(
        new Promise((resolve, reject) => figma.removeLocalServer((error) => error ? reject(error) : resolve())),
        /integration-failed/
      )
      assert.deepEqual(commands, [['mcp', 'logout', 'figma']])
    } finally {
      figma._test.setRemoveExecFile(null)
    }
  })

  console.log(`figma-session-actions: ${checks} checks passed`)
} finally {
  if (server) await new Promise((resolve) => server.close(resolve))
  figma.status = originals.figmaStatus
  figma.account = originals.figmaAccount
  figma.sessionAdmission = originals.sessionAdmission
  sessions.status = originals.status
  sessions.start = originals.start
  sessions.send = originals.send
  rmSync(root, { recursive: true, force: true })
  rmSync(scratch, { recursive: true, force: true })
}
