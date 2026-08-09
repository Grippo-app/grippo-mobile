#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const figma = require('../server/figma.js')
const { classify, classifyOutput, classifyScope, applyLocalScope, identityConnectorReady, installSnapshot, accountTimestampCurrent, normalizeAccountRecord, sessionAdmissionFor } = figma._test

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }

const cases = [
  ['✔ Connected', 'connected'],
  ['✓ Connected', 'connected'],
  ['! Needs authentication', 'needs-auth'],
  ['Connected · tools fetch failed', 'unknown'],
  ['! Connected · tools fetch failed', 'unknown'],
  ['Disconnected', 'unknown'],
  ['Not connected', 'unknown'],
  ['Connection failed', 'unknown'],
]

for (const [status, expected] of cases) {
  check(`${JSON.stringify(status)} -> ${expected}`, () => {
    assert.equal(classify(status), expected)
  })
}

check('failed tool discovery cannot make the local connector green', () => {
  const result = classifyOutput('figma: https://mcp.figma.com/mcp (HTTP) - ! Connected · tools fetch failed\n', '', 0)
  assert.equal(result.state, 'unknown')
  assert.equal(result.local.status, 'unknown')
})

check('the exact healthy row remains connected', () => {
  const result = classifyOutput('figma: https://mcp.figma.com/mcp (HTTP) - ✔ Connected\n', '', 0)
  assert.equal(result.state, 'connected')
  assert.equal(result.local.status, 'connected')
})

check('an auth-expired row remains needs-auth', () => {
  const result = classifyOutput('figma: https://mcp.figma.com/mcp (HTTP) - ! Needs authentication\n', '', 0)
  assert.equal(result.state, 'needs-auth')
  assert.equal(result.local.status, 'needs-auth')
})

check('mcp get accepts only the exact Local config scope', () => {
  assert.equal(classifyScope('figma:\n  Scope: Local config (private to you in this project)\n'), 'local')
  assert.equal(classifyScope('figma:\n  Scope: User config\n'), 'nonlocal')
  assert.equal(classifyScope('figma:\n  Scope: Project config\n'), 'nonlocal')
  assert.equal(classifyScope('figma:\n  Scope: something future\n'), 'unknown')
})

check('a non-local figma row cannot become project-local readiness', () => {
  const listed = classifyOutput('figma: https://mcp.figma.com/mcp (HTTP) - ✔ Connected\n', '', 0)
  const result = applyLocalScope(listed, 'nonlocal')
  assert.equal(result.state, 'local-absent')
  assert.equal(result.local.present, false)
  assert.equal(result.global.present, true)
  assert.equal(result.global.name, 'figma')
})

check('an unverifiable scope fails closed', () => {
  const listed = classifyOutput('figma: https://mcp.figma.com/mcp (HTTP) - ✔ Connected\n', '', 0)
  const result = applyLocalScope(listed, 'unknown')
  assert.equal(result.state, 'unknown')
  assert.equal(result.local.status, 'unknown')
})

check('a global connector conflict ends the verified identity episode', () => {
  const clean = () => applyLocalScope(classifyOutput('figma: https://mcp.figma.com/mcp (HTTP) - Connected\n', '', 0), 'local')
  const conflict = applyLocalScope(classifyOutput([
    'figma: https://mcp.figma.com/mcp (HTTP) - Connected',
    'claude.ai Figma: https://mcp.figma.com/mcp (HTTP) - Connected',
  ].join('\n'), '', 0), 'local')
  assert.equal(identityConnectorReady(clean()), true)
  assert.equal(identityConnectorReady(conflict), false)
  installSnapshot(clean())
  const firstNonce = figma.status().verificationNonce
  assert.match(firstNonce, /^[0-9a-f]{32}$/)
  installSnapshot(conflict)
  assert.equal(figma.status().verificationNonce, null)
  assert.equal(figma.status().connectedSince, null)
  installSnapshot(clean())
  assert.match(figma.status().verificationNonce, /^[0-9a-f]{32}$/)
  assert.notEqual(figma.status().verificationNonce, firstNonce)
})

check('a whoami snapshot from a previous connector episode is stale', () => {
  assert.equal(accountTimestampCurrent('2026-07-14T09:00:00Z', '2026-07-14T10:00:00Z', Date.parse('2026-07-14T10:01:00Z')), false)
})

check('a whoami snapshot written in the current episode is accepted', () => {
  assert.equal(accountTimestampCurrent('2026-07-14T10:00:01Z', '2026-07-14T10:00:00.500Z', Date.parse('2026-07-14T10:01:00Z')), true)
})

check('a loose non-ISO whoami timestamp is rejected', () => {
  assert.equal(accountTimestampCurrent('July 14, 2026 10:00:01 UTC', '2026-07-14T10:00:00Z', Date.parse('2026-07-14T10:01:00Z')), false)
})

check('account identity is nonblank and bound to the current verification nonce', () => {
  const episode = '2026-07-14T10:00:00Z'
  const now = Date.parse('2026-07-14T10:01:00Z')
  assert.equal(normalizeAccountRecord({ handle: '   ', email: '', checkedAt: '2026-07-14T10:00:01Z', verificationNonce: 'current' }, episode, 'current', now), null)
  assert.equal(normalizeAccountRecord({ handle: 'A', checkedAt: '2026-07-14T10:00:01Z', verificationNonce: 'old' }, episode, 'current', now), null)
  assert.equal(normalizeAccountRecord({ handle: 'A', email: '', tier: '', seat: '', checkedAt: '2026-07-14T10:00:01Z', verificationNonce: 'current', extra: true }, episode, 'current', now), null)
  assert.deepEqual(normalizeAccountRecord({ handle: ' A ', email: ' a@example.test ', tier: '', seat: '', checkedAt: '2026-07-14T10:00:01Z', verificationNonce: 'current' }, episode, 'current', now), {
    handle: 'A', email: 'a@example.test', tier: null, seat: null, checkedAt: '2026-07-14T10:00:01Z'
  })
})

check('Figma MCP session admission is fail-closed but keeps local rebundle available', () => {
  const connected = { state: 'connected', local: { present: true }, global: { present: false } }
  assert.equal(sessionAdmissionFor('figma:whoami', connected, null), null)
  assert.equal(sessionAdmissionFor('figma:derive', connected, { handle: 'A' }), null)
  assert.equal(sessionAdmissionFor('figma:rebundle:TASK_1_x', { state: 'unknown' }, null), null)
  assert.equal(sessionAdmissionFor('figma:derive', connected, null).error, 'figma-account-unverified')
  assert.equal(sessionAdmissionFor('figma:derive', { ...connected, global: { present: true } }, { handle: 'A' }).error, 'figma-connector-conflict')
  assert.equal(sessionAdmissionFor('figma:whoami', { state: 'needs-auth', local: { present: true }, global: { present: false } }, null).error, 'figma-connector-not-ready')
})

console.log(`figma-status-classification: ${checks} checks passed`)
