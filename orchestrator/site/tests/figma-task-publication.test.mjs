#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdirSync, mkdtempSync, readdirSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'

const root = mkdtempSync(join(tmpdir(), 'figma-task-publication-'))
const orchestrator = join(root, 'orchestrator')
const cache = join(orchestrator, '.cache')
mkdirSync(join(cache, 'figma', 'screens', 'TASK_1_fixture'), { recursive: true })
mkdirSync(join(cache, 'tasks', 'finalizations'), { recursive: true })
mkdirSync(join(orchestrator, 'tasks', 'todo'), { recursive: true })
writeFileSync(join(orchestrator, 'project-config.md'), [
  '---',
  'productName: Fixture',
  'figmaLibraryUrl: https://www.figma.com/design/AbCdEfGh1234',
  'figmaEnabled: true',
  '---',
  '',
  '# Fixture',
  ''
].join('\n'))

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_CACHE_DIR = cache
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = join(cache, 'tasks', 'finalizations')

const require = createRequire(import.meta.url)
const generation = require('../server/figma-generation.js')
const figma = require('../server/figma.js')
const publication = require('../server/figma-task-publication.js')
const screenTokenPlans = require('../server/screen-token-plans.js')
const tokenIdentity = require('../../figma/runtime/token-identity.cjs')
const originalAccount = figma.account
figma.account = () => ({ email: 'fixture@example.test', handle: 'fixture' })

const stem = 'TASK_1_fixture'
const screenDir = join(cache, 'figma', 'screens', stem)
const reportDir = join(cache, 'figma', 'reports')
const reportFile = join(reportDir, `screen-cache-${stem}.json`)
const iso = (second) => `2026-01-01T00:00:${String(second).padStart(2, '0')}.000Z`
const write = (file, value) => {
  mkdirSync(dirname(file), { recursive: true })
  writeFileSync(file, Buffer.isBuffer(value) ? value : JSON.stringify(value, null, 2) + '\n')
}

writeFileSync(join(orchestrator, 'tasks', 'todo', `${stem}.md`), [
  '# Fixture',
  '',
  '## Design',
  '- Home — https://www.figma.com/design/AbCdEfGh1234/Fixture?node-id=1-2',
  ''
].join('\n'))
write(join(screenDir, 'Home.spec.json'), { screen: 'Home' })
write(join(screenDir, 'Home.instances.json'), [])
write(join(screenDir, 'Home.context.json'), { node: 'Home' })
write(join(screenDir, 'Home.png'), Buffer.from('fixture-png'))
write(join(screenDir, 'bindings.json'), { schemaVersion: 1, stem, screens: [], components: [] })

function installTokenPlan() {
  const prepared = screenTokenPlans.prepare(stem)
  const record = prepared.plan.records[0]
  const capture = {
    schemaVersion: 1,
    providerCapability: 'node-bound-resolved-variables',
    provider: 'figma-mcp',
    captureOperationId: record.captureOperationId,
    captureSequence: record.captureSequence,
    accountFingerprint: prepared.plan.accountFingerprint,
    connectorRevision: prepared.plan.connectorRevision,
    source: record.source,
    observations: [],
    witness: {
      startedAt: iso(0),
      finishedAt: iso(1),
      nodeId: record.source.nodeId,
      operation: 'get_variable_defs',
      sourceCompleteness: 'complete-returned-payload',
      providerEnumerationCompleteness: 'not-available-for-provider',
      providerTruncationSignal: 'unavailable',
      truncated: false,
      issues: [],
      observationCount: 0,
      accountFingerprint: prepared.plan.accountFingerprint,
      connectorRevision: prepared.plan.connectorRevision,
      producerVersion: 'fixture-v1'
    }
  }
  const tokenBytes = Buffer.from(JSON.stringify(capture, null, 2) + '\n')
  write(join(screenDir, record.tokensFile), tokenBytes)
  write(join(screenDir, 'index.json'), {
    schemaVersion: 3,
    taskStem: stem,
    nodes: {
      Home: {
        kind: 'screen',
        url: 'https://www.figma.com/design/AbCdEfGh1234?node-id=1-2',
        nodeId: '1:2',
        fetchedAt: iso(1),
        variants: [{
          id: record.variantId,
          theme: record.source.context.theme,
          locale: record.source.context.locale,
          platform: record.source.context.platform,
          url: 'https://www.figma.com/design/AbCdEfGh1234?node-id=1-2',
          nodeId: '1:2',
          fetchedAt: iso(1),
          imageFile: 'Home.png',
          tokensFile: record.tokensFile,
          tokensHash: generation.sha(tokenBytes),
          captureOperationId: record.captureOperationId,
          captureSequence: record.captureSequence
        }]
      }
    }
  })
  return prepared
}

function proof(second) {
  const required = ['index.json', 'Home.spec.json', 'Home.instances.json', 'Home.context.json', 'Home.png', 'Home.tokens.json']
  const inputHashes = {}
  for (const name of required) {
    const bytes = require('fs').readFileSync(join(screenDir, name))
    inputHashes[join(screenDir, name)] = generation.sha(bytes)
  }
  return {
    schemaVersion: 1, gatePolicyVersion: 1, taskStem: stem, pipelineRunId: 'fixture-run',
    mode: 'gate', inputs: {}, inputHashes, overall: 'PASS', blockingCount: 0, warningCount: 0,
    issues: [], reportPath: `orchestrator/.cache/figma/reports/screen-cache-${stem}.json`,
    reportRelPath: `orchestrator/.cache/figma/reports/screen-cache-${stem}.json`,
    generatedAt: iso(second), screens: []
  }
}

try {
  const originalCurrent = generation.current
  const originalReadEntry = generation.readEntry
  const scope = {
    fileKeyFingerprint: 'sha256:' + 'a'.repeat(64),
    branchKey: 'branch:fixture'
  }
  const source = {
    fileKeyFingerprint: scope.fileKeyFingerprint,
    branchKey: scope.branchKey,
    nodeId: '1:2',
    context: { theme: 'light', locale: 'default', platform: 'shared' }
  }
  const sourceId = tokenIdentity.sourceIdFor(source)
  const sourceIndexHash = 'sha256:' + 'b'.repeat(64)
  generation.current = () => ({
    ok: true,
    mode: 'generation',
    manifest: {
      fileKeyFingerprint: scope.fileKeyFingerprint,
      artifacts: [{ role: 'observed-token-source-index' }]
    }
  })
  generation.readEntry = () => Buffer.from(JSON.stringify({
    schemaVersion: 1,
    semanticHash: sourceIndexHash,
    scope,
    sources: [{ sourceId, acceptedBatch: { captureSequence: 7 } }]
  }))
  const sourceState = screenTokenPlans._test.currentSourceState(scope, [sourceId])
  assert.equal(sourceState.sourceIndexHash, sourceIndexHash)
  assert.equal(sourceState.accepted[sourceId], 7)
  generation.current = originalCurrent
  generation.readEntry = originalReadEntry

  const longStem = 'TASK_2_' + 'x'.repeat(113)
  assert.ok(longStem.length <= 120)
  assert.ok(generation.surfaceIndexRole(longStem).length <= 128)
  assert.ok(generation.surfaceDriftRole(longStem).length <= 128)
  assert.equal(generation.requiredRole({
    role: generation.surfaceIndexRole(longStem), group: 'surfaces', required: true,
    persistence: 'committed', logicalPath: `orchestrator/.cache/figma/screens/${longStem}/index.json`
  }), true)

  const stalePlan = installTokenPlan()
  write(reportFile, proof(2))
  const stale = publication.prepareTurn({
    key: `figma:screens:${stem}`, stem, action: 'screen-pull', screenTokenPlanId: stalePlan.planId
  })
  const staleResult = publication.markResult(stale, true)
  assert.equal(staleResult.ready, false)
  assert.equal(staleResult.error, 'task-publication-completion-proof-missing')

  const readyPlan = installTokenPlan()
  write(reportFile, proof(2))
  const ready = publication.prepareTurn({
    key: `figma:screens:${stem}`, stem, action: 'screen-pull', screenTokenPlanId: readyPlan.planId
  })
  write(reportFile, proof(3))
  assert.deepEqual(publication.markResult(ready, true), { ready: true })

  // A turn that did not produce a result remains pending. Startup must discard
  // it rather than guessing that partially-written files are publishable.
  publication.prepareTurn({ key: `figma:screens:${stem}`, stem, action: 'screen-drift' })

  let request = null
  await publication.init({
    publishDomains(value) {
      request = value
      assert.equal(value.completed[0].domain, `surface:${stem.toLowerCase()}`)
      assert.equal(value.completed[0].group, 'surfaces')
      assert.equal(value.verifyInputs(), true)
      const configFile = join(orchestrator, 'project-config.md')
      const configBytes = require('fs').readFileSync(configFile)
      writeFileSync(configFile, configBytes.toString('utf8').replace('AbCdEfGh1234', 'ChangedFile1234'))
      assert.equal(value.verifyInputs(), false)
      writeFileSync(configFile, configBytes)
      assert.equal(value.verifyInputs(), true)
      assert.ok(value.completed[0].stage.artifacts.every((artifact) => artifact.persistence === 'committed' && artifact.required))
      return Promise.resolve('gen-' + '1'.repeat(32))
    }
  })

  const deadline = Date.now() + 3000
  while (!request && Date.now() < deadline) await new Promise((resolve) => setTimeout(resolve, 10))
  assert.ok(request, 'ready publication was recovered and dispatched')
  while (readdirSync(join(cache, 'figma', 'task-publications')).length && Date.now() < deadline) {
    await new Promise((resolve) => setTimeout(resolve, 10))
  }
  assert.deepEqual(readdirSync(join(cache, 'figma', 'task-publications')), [])

  mkdirSync(join(screenDir, 'unexpected'), { recursive: true })
  assert.throws(() => publication.collect('screen-pull', stem), /task-surface-entry-invalid/)
  rmSync(join(screenDir, 'unexpected'), { recursive: true })

  console.log('figma-task-publication: durable proof, recovery and confinement checks passed')
} finally {
  figma.account = originalAccount
  rmSync(root, { recursive: true, force: true })
}
