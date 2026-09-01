#!/usr/bin/env node

import assert from 'node:assert/strict'
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import { canonicalHash, canonicalJson } from '../../figma/runtime/canonical-json.mjs'
import { aggregateObservedTokens } from '../../figma/tokens/catalog-aggregator.mjs'
import { normalizeSourceCapture } from '../../figma/tokens/source-normalizer.mjs'
import { sourceBatchSemanticPayload } from '../../figma/tokens/source-contract.mjs'
import { immutablePlan, validObservedCapture } from '../../figma/tests/observed-token-fixtures.mjs'
import '../scripts/i18n/en.js'
import figmaEn from '../scripts/i18n/dictionaries/figma-integration/en.js'
import figmaRu from '../scripts/i18n/dictionaries/figma-integration/ru.js'
import figmaUk from '../scripts/i18n/dictionaries/figma-integration/uk.js'
import { figmaActionError, focusFigmaEnableResult } from '../scripts/panels/figma.js'
import { createHistoryPagination } from '../scripts/figma/history-view.js'

const require = createRequire(import.meta.url)
const REPO = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..')
const generation = require('../server/figma-generation.js')
const syncHistory = require('../server/figma-sync-history.js')
const limits = require('../../figma/runtime/program-limits.cjs')
const hash = (char) => 'sha256:' + char.repeat(64)
const generationId = 'gen-' + '1'.repeat(32)
const instant = '2026-07-23T10:00:00.000Z'
let checks = 0
function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}
async function checkAsync(name, fn) {
  await fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}
function artifact(group, domain, role, logicalPath, suffix) {
  return {
    role, group, domain,
    path: `orchestrator/figma/manifests/artifacts/${generationId}/${group}/${suffix}`,
    logicalPath, hash: hash('a'), schemaVersion: generation.artifactContractVersion(role),
    persistence: 'committed', required: true, size: 10
  }
}
const tokenArtifacts = [
  artifact('tokens', 'tokens', 'observed-token-source-index',
    'orchestrator/figma/tokens/source-index.json', 'source-index.json'),
  artifact('tokens', 'tokens', 'observed-token-catalog',
    'orchestrator/figma/tokens/observed-token-catalog.json', 'catalog.json')
]
const componentArtifacts = [
  artifact('components', 'components', 'design-component-inventory',
    generation.COMPONENT_INVENTORY_LOGICAL_PATH, 'inventory.json')
]
function manifest(artifacts = tokenArtifacts.concat(componentArtifacts)) {
  return {
    schemaVersion: 2,
    generationId,
    accountFingerprint: hash('b'),
    fileKeyFingerprint: hash('c'),
    createdAt: instant,
    syncJobId: 'fsj-' + '2'.repeat(32),
    updatedDomains: ['components', 'tokens'],
    syncGroups: {
      components: { status: 'completed', updated: componentArtifacts.length, unchanged: 0, warnings: 0 },
      tokens: { status: 'completed', updated: tokenArtifacts.length, unchanged: 0, warnings: 0 }
    },
    groups: ['tokens', 'components'],
    domains: [{
      id: 'components', group: 'components', inputFingerprint: hash('d'),
      syncedAt: instant, sourceGenerationId: generationId
    }, {
      id: 'tokens', group: 'tokens', inputFingerprint: hash('d'),
      syncedAt: instant, sourceGenerationId: generationId
    }],
    artifacts,
    counters: {
      updated: tokenArtifacts.length + componentArtifacts.length,
      unchanged: 0,
      warnings: 0
    }
  }
}

check('one manifest admits the exact atomic components+tokens domain set', () => {
  assert.equal(generation.validateManifest(manifest(), generationId), true)
})

check('either missing sibling makes the atomic generation invalid', () => {
  assert.equal(generation.validateManifest(manifest(componentArtifacts), generationId), false)
  assert.equal(generation.validateManifest(manifest(tokenArtifacts), generationId), false)
})

check('old token roles and unknown schema versions fail closed', () => {
  const old = artifact('tokens', 'tokens', 'design-token-' + 'inventory',
    'orchestrator/figma/tokens/' + 'design-token-' + 'inventory.json', 'old.json')
  assert.equal(generation.validateManifest(manifest([old].concat(componentArtifacts)), generationId), false)
  assert.equal(generation.validateManifest({ ...manifest(), schemaVersion: 1 }, generationId), false)
})

check('published composite limits are the normative 195/200/64 MiB ceilings', () => {
  assert.equal(limits.compositePublicationArtifactsMax, 195)
  assert.equal(limits.manifestArtifactsMax, 200)
  assert.equal(limits.phaseBytesMax, 64 * 1024 * 1024)
})

check('a completed turn remains active until its runtime session exits', () => {
  const sessions = require('../server/sessions.js')
  assert.equal(sessions.settled({ running: true, awaitingTurn: false, askedThisTurn: false }), false)
  assert.equal(sessions.settled({ running: true, awaitingTurn: false, askedThisTurn: false, closing: true }), false)
  assert.equal(sessions.settled({ running: false, awaitingTurn: false, askedThisTurn: false }), true)
  assert.equal(sessions.settled(null), true)
})

check('Figma request failures preserve their typed localized reason', () => {
  assert.equal(figmaActionError({ kind: 'figma-session-active' }),
    'A task or Figma session is still active. Wait for it to finish, then try again.')
  assert.equal(figmaActionError({ kind: 'plan-stale' }),
    'The synchronization plan is stale because its inputs changed. Recalculate it and try again.')
  assert.equal(figmaActionError({ kind: 'access-denied' }),
    'Change the account or select a file it can access.')
  assert.equal(figmaActionError({ kind: 'unregistered-code' }),
    'Orchestrator could not classify the returned error safely. Reload the current state; if it persists, review synchronization history and diagnostics.')
})

await checkAsync('Figma history pagination appends every cursor page, blocks duplicates, and retries in place', async () => {
  const calls = []
  const snapshots = []
  let failSecondPage = true
  const pagination = createHistoryPagination(async (cursor) => {
    calls.push(cursor)
    if (cursor === 'cursor-2' && failSecondPage) {
      failSecondPage = false
      throw new Error('fixture-page-failure')
    }
    return cursor === null
      ? { items: [{ id: 'first' }], nextCursor: 'cursor-2' }
      : { items: [{ id: 'first' }, { id: 'second' }], nextCursor: null }
  }, (snapshot) => snapshots.push(snapshot))

  await pagination.open()
  assert.deepEqual(calls, [null])
  assert.deepEqual(snapshots.at(-1), {
    items: [{ id: 'first' }], nextCursor: 'cursor-2', loading: false, error: null
  })

  const failed = pagination.loadMore()
  assert.equal(pagination.loadMore(), null)
  await failed
  assert.equal(snapshots.at(-1).items.length, 1)
  assert.equal(snapshots.at(-1).nextCursor, 'cursor-2')
  assert.equal(snapshots.at(-1).loading, false)
  assert.equal(snapshots.at(-1).error.message, 'fixture-page-failure')

  await pagination.retry()
  assert.deepEqual(calls, [null, 'cursor-2', 'cursor-2'])
  assert.deepEqual(snapshots.at(-1), {
    items: [{ id: 'first' }, { id: 'second' }], nextCursor: null,
    loading: false, error: null
  })
})

await checkAsync('Figma history invalid-cursor recovery reloads page one instead of retrying a dead cursor', async () => {
  const calls = []
  let initialLoads = 0
  const pagination = createHistoryPagination(async (cursor) => {
    calls.push(cursor)
    if (cursor === null) {
      initialLoads++
      return { items: [{ id: 'first-' + initialLoads }], nextCursor: 'cursor-2' }
    }
    const error = new Error('stale cursor')
    error.code = 'bad-cursor'
    throw error
  }, () => {})

  await pagination.open()
  await pagination.loadMore()
  await pagination.retry()
  assert.deepEqual(calls, [null, 'cursor-2', null])
})

check('Figma history cursor remains stable when a newer sync is inserted between pages', () => {
  const rows = Array.from({ length: 25 }, (_, index) => ({
    id: 'fsj-' + (index + 1).toString(16).padStart(32, '0'),
    startedAt: new Date(Date.parse(instant) - index * 1000).toISOString(),
  }))
  const first = syncHistory._pageRowsForTests(rows, null, 20)
  assert.equal(first.ok, true)
  assert.deepEqual(first.rows.map((row) => row.id), rows.slice(0, 20).map((row) => row.id))
  assert.equal(typeof first.nextCursor, 'string')

  const newer = {
    id: 'fsj-' + 'f'.repeat(32),
    startedAt: new Date(Date.parse(instant) + 1000).toISOString(),
  }
  const second = syncHistory._pageRowsForTests([newer, ...rows], first.nextCursor, 20)
  assert.equal(second.ok, true)
  assert.deepEqual(second.rows.map((row) => row.id), rows.slice(20).map((row) => row.id))
  assert.equal(second.nextCursor, null)
  assert.deepEqual(syncHistory._pageRowsForTests(
    rows, Buffer.from('20').toString('base64url'), 20
  ), { ok: false, error: 'bad-cursor' })
})

check('component orchestration requires both domains before publication', () => {
  const sync = readFileSync(join(REPO, 'orchestrator', 'site', 'server', 'figma-sync.js'), 'utf8')
  const jobs = readFileSync(join(REPO, 'orchestrator', 'site', 'server', 'figma-token-jobs.js'), 'utf8')
  assert.match(sync, /component-token-atomic-domain-set-incomplete/)
  assert.match(sync, /\['components', 'tokens'\]/)
  assert.match(jobs, /component-token-plan\.json/)
  assert.match(jobs, /COMPONENT_TOKEN_REFERENCE_UNCOVERED/)
  assert.match(jobs, /compositePublicationArtifactsMax/)
})

check('Figma sync start timestamps the job before constructing it', () => {
  const source = readFileSync(join(REPO, 'orchestrator', 'site', 'server', 'figma-sync.js'), 'utf8')
  const startAt = source.indexOf('function start(request, internal)')
  const cancelAt = source.indexOf('function cancel(request)', startAt)
  assert.ok(startAt >= 0 && cancelAt > startAt)
  const startSource = source.slice(startAt, cancelAt)
  const declarationAt = startSource.indexOf('var startedAt = now();')
  const jobFieldAt = startSource.indexOf('startedAt: startedAt')
  assert.ok(declarationAt >= 0 && jobFieldAt > declarationAt)
})

check('first sync bootstraps project adapters, then schedules zero-read local comparison', () => {
  const sync = require('../server/figma-sync.js')
  const source = readFileSync(join(REPO, 'orchestrator', 'site', 'server', 'figma-sync.js'), 'utf8')
  assert.match(source, /projectAdaptersBootstrap\.ensure\(\)/)
  assert.match(source, /bootstrapTokenSourceSnapshot\(tokenSources, context\)/)
  assert.match(source, /prepared\.sourceCount === 0[\s\S]*TOKEN_SOURCE_CAPTURE_INCOMPLETE/)
  assert.doesNotMatch(source, /prepared\.sourceCount === 0 \? Promise\.resolve/)
  assert.match(source, /requestDriftComparison\('post-' \+ job\.scope \+ '-sync'/)
  assert.equal(sync._test.stageWarningCount([
    'Token snapshot published: 2 artifacts.',
    'Component inventory unchanged.',
  ]), 0)
  assert.equal(sync._test.stageWarningCount([
    'Token comparison skipped because source health requires recovery.',
  ]), 1)
})

check('component plan durably reserves unique prospective operations before provider intake and fixes all 128 buckets', () => {
  const sync = readFileSync(join(REPO, 'orchestrator', 'site', 'server', 'figma-sync.js'), 'utf8')
  const prompt = readFileSync(join(REPO, 'orchestrator', 'site', 'scripts', 'figma-actions.js'), 'utf8')
  assert.match(sync, /newSourceReservations/)
  assert.match(sync, /prospectiveCount: selected\.scope === 'components' \? 128 : 0/)
  assert.match(sync, /Array\.from\(\{ length: 128 \}/)
  assert.match(prompt, /every one of the 128 listed/)
  assert.match(prompt, /Never reuse one reservation for two sources/)
  assert.match(prompt, /Failure of either domain publishes neither domain/)
})

check('origin detachment removes only component ownership from a shared source', () => {
  const capture = validObservedCapture()
  capture.source.kind = 'component'
  capture.source.origin = {
    kind: 'component-inventory',
    componentScopeId: 'component-scope:' + '1'.repeat(64),
    captureRootNodeId: capture.source.nodeId
  }
  const batch = normalizeSourceCapture(capture, Buffer.from(JSON.stringify(capture)), immutablePlan(capture))
  batch.origins.unshift({
    kind: 'task-screen',
    taskStem: 'TASK_1_fixture',
    screenKey: 'Main',
    variantId: 'light-default-shared'
  })
  batch.origins.sort((left, right) => canonicalJson(left).localeCompare(canonicalJson(right)))
  batch.batchSemanticHash = canonicalHash(sourceBatchSemanticPayload(batch))
  const observed = aggregateObservedTokens({
    scope: { fileKeyFingerprint: batch.fileKeyFingerprint, branchKey: batch.branchKey },
    batches: [batch],
    revision: 1
  })
  assert.equal(observed.index.sources[0].origins.length, 2)
})

check('stage input fingerprint and pointer-last rails remain explicit', () => {
  const sync = readFileSync(join(REPO, 'orchestrator', 'site', 'server', 'figma-sync.js'), 'utf8')
  assert.match(sync, /inputFingerprint/)
  assert.match(sync, /generation-postcondition-failed/)
  assert.match(sync, /atomic\(generation\.POINTER_FILE/)
})

check('drift fingerprint roots fail closed on invalid adapter config instead of collapsing to empty', () => {
  const sync = require('../server/figma-sync.js')
  const document = {
    schemaVersion: 2,
    adapters: [{
      id: 'tokens', kind: 'json-tokens', version: 2, enabled: true,
      capabilities: ['tokens'], platform: 'web', authority: 'handwritten',
      tokens: {
        roots: ['design/tokens'], include: ['**/*.json'], exclude: [],
        modes: ['shared'], authorities: { color: { contracts: ['FixtureTokens'] } },
        contextMap: [], bindingRules: []
      }
    }, {
      id: 'components', kind: 'component-manifest', version: 2, enabled: true,
      capabilities: ['components'], platform: 'shared', authority: 'generated',
      components: {
        roots: ['design/components'], include: ['**/*.json'], exclude: [],
        visibility: ['public'], previewRoots: ['design/previews'],
        screenshotTestRoots: ['design/screenshots']
      }
    }]
  }
  assert.deepEqual(sync._test.adapterRootsFromDocument(document), [
    'design/components', 'design/previews', 'design/screenshots', 'design/tokens'
  ])
  assert.deepEqual(sync._test.adapterRootsFromDocument(document, 'tokens'), ['design/tokens'])
  assert.deepEqual(sync._test.adapterRootsFromDocument(document, 'components'), [
    'design/components', 'design/previews', 'design/screenshots'
  ])
  assert.throws(() => sync._test.adapterRootsFromDocument({ schemaVersion: 2, adapters: [] }),
    /figma-adapter-config-invalid/)
  assert.throws(() => sync._test.adapterRootsFromDocument({ ...document, extra: true }),
    /figma-adapter-config-invalid/)
})

check('restart recovery recognizes a committed token semantic no-op without inventing a generation', () => {
  const sync = require('../server/figma-sync.js')
  const jobId = 'fsj-' + '7'.repeat(32)
  const accountFingerprint = hash('b')
  const fileKeyFingerprint = hash('c')
  const startedAt = '2026-07-23T10:00:00.000Z'
  const finishedAt = '2026-07-23T10:00:01.000Z'
  const recovered = sync._test.semanticNoOpCommittedRecovery({
    id: jobId,
    startedAt,
    accountFingerprint,
    fileKeyFingerprint,
    planGroups: ['tokens']
  }, {
    manifest: { generationId, accountFingerprint, fileKeyFingerprint }
  }, hash('d'), {
    index: {
      sourceIndexRevision: hash('d'),
      jobSummaries: [{
        jobId,
        action: 'refresh-known-token-sources',
        startedAt,
        finishedAt,
        outcome: 'no-op',
        sourceCount: 3
      }]
    }
  })
  assert.deepEqual(recovered, {
    generationId,
    updatedGroups: ['tokens'],
    groups: {
      tokens: { status: 'completed', updated: 0, unchanged: 3, warnings: 0 }
    },
    partial: false,
    finishedAt
  })
  assert.equal(sync._test.semanticNoOpCommittedRecovery({
    id: jobId,
    startedAt,
    accountFingerprint,
    fileKeyFingerprint,
    planGroups: ['tokens']
  }, {
    manifest: { generationId, accountFingerprint, fileKeyFingerprint }
  }, hash('d'), {
    index: {
      sourceIndexRevision: hash('e'),
      jobSummaries: []
    }
  }), null)
})

check('disconnected Figma blocks provider refresh but not compatible offline compare', () => {
  const figma = require('../server/figma.js')
  const config = require('../server/project-config-update.js')
  const history = require('../server/figma-sync-history.js')
  const sync = require('../server/figma-sync.js')
  const taskPublication = require('../server/figma-task-publication.js')
  const testJobs = require('../server/figma-test-job.js')
  const sessions = require('../server/sessions.js')
  const integration = require('../server/figma-integration.js')
  const overrides = [
    [figma, 'status', () => ({
      state: 'unknown',
      local: { present: false },
      global: { present: false },
      checkedAt: instant
    })],
    [figma, 'account', () => null],
    [config, 'read', () => ({
      ok: true, figmaFileKey: null, figmaFieldState: 'missing',
      figmaLibraryUrl: null, revision: null
    })],
    [history, 'latestSuccessful', () => null],
    [history, 'latest', () => null],
    [sync, 'latestTerminal', () => null],
    [sync, 'active', () => null],
    [sync, 'busy', () => false],
    [sync, 'recoveryState', () => 'ready'],
    [sync, 'comparisonSourceAvailable', () => true],
    [taskPublication, 'busy', () => false],
    [taskPublication, 'recoveryState', () => 'ready'],
    [testJobs, 'currentJob', () => null],
    [testJobs, 'lastJob', () => null],
    [testJobs, 'busy', () => false],
    [sessions, 'list', () => ({})]
  ]
  const originals = overrides.map(([owner, key]) => [owner, key, owner[key]])
  overrides.forEach(([owner, key, value]) => { owner[key] = value })
  try {
    const model = integration.get({
      ok: true,
      mode: 'generation',
      manifest: { artifacts: [{ role: 'observed-token-catalog' }] }
    })
    assert.equal(model.actions.canSync, false)
    assert.equal(model.syncGate.reasonCode, 'connector-unavailable')
    assert.equal(model.actions.canCompare, true)
    assert.deepEqual(model.compareGate, { state: 'ready', reasonCode: null })
  } finally {
    originals.forEach(([owner, key, value]) => { owner[key] = value })
  }
})

check('retention rejects an unreadable manifest instead of deleting its unknown lineage', () => {
  const sync = require('../server/figma-sync.js')
  const valid = manifest()
  assert.deepEqual(sync._test.sourceGenerationIdsForRetention(valid, generationId), [generationId])
  assert.throws(() => sync._test.sourceGenerationIdsForRetention({
    ...valid,
    domains: valid.domains.map((domain) => ({ ...domain, sourceGenerationId: 'gen-' + 'f'.repeat(32) })),
    artifacts: valid.artifacts
  }, generationId), /generation-retention-manifest-invalid/)
  assert.throws(() => sync._test.sourceGenerationIdsForRetention({
    ...valid,
    schemaVersion: 1
  }, generationId), /generation-retention-manifest-invalid/)
})

check('Figma clear UX keeps destructive confirmation and typed errors inside its modal', () => {
  const panel = readFileSync(join(REPO, 'orchestrator', 'site', 'scripts', 'panels', 'figma.js'), 'utf8')
  const view = readFileSync(join(REPO, 'orchestrator', 'site', 'scripts', 'figma', 'integration-view.js'), 'utf8')
  const api = readFileSync(join(REPO, 'orchestrator', 'site', 'scripts', 'data', 'tasks-api.js'), 'utf8')
  const css = readFileSync(join(REPO, 'orchestrator', 'site', 'styles', 'panels.css'), 'utf8')
  assert.match(view, /figma\.action\.clearIntegration/)
  assert.match(view, /actions\.canClearIntegration/)
  assert.match(panel, /figma\.clear\.title/)
  assert.match(panel, /figma\.clear\.body/)
  assert.match(panel, /status\.setAttribute\('role', 'alert'\)/)
  assert.match(panel, /current\.context && current\.context\.generationId/)
  assert.match(panel, /clearDialog\._figmaResetKey = idempotencyKey\('figma-reset'\)/)
  assert.match(panel, /figma\.clear\.error\./)
  assert.match(panel, /dialog\.setAttribute\('aria-busy', 'true'\)/)
  assert.match(panel, /confirm\.textContent = t\('figma\.clear\.progressButton'\)/)
  assert.match(panel, /status\.textContent = t\('figma\.clear\.progress'\)/)
  assert.match(panel, /dialog\._prepare = function \(\)/)
  for (const dictionary of [figmaEn, figmaRu, figmaUk]) {
    assert.ok(dictionary['figma.clear.progressButton'])
    assert.ok(dictionary['figma.clear.progress'].length > 60)
  }
  assert.match(css, /\.integration-clear-progress\s*\{[\s\S]*?background:\s*var\(--info-soft\)/)
  assert.match(api, /\/api\/figma\/integration\/reset/)
  assert.doesNotMatch(panel, /window\.confirm\(t\('figma\.clear/)
})

check('Figma feature transition announces its terminal state and transfers keyboard focus', () => {
  const view = readFileSync(join(REPO, 'orchestrator', 'site', 'scripts', 'figma', 'integration-view.js'), 'utf8')
  const panel = readFileSync(join(REPO, 'orchestrator', 'site', 'scripts', 'panels', 'figma.js'), 'utf8')
  const header = readFileSync(join(REPO, 'orchestrator', 'site', 'scripts', 'figma-status.js'), 'utf8')
  assert.match(view, /'aria-live': 'polite'/)
  assert.match(view, /'aria-atomic': 'true'/)
  assert.match(view, /role: 'status'/)
  assert.match(view, /stateTitle\.focus\(\)/)
  assert.match(view, /focusPrimary/)
  assert.match(view, /class: 'panel-title', text: t\('figma\.title'\), attrs: \{ tabindex: '-1' \}/)
  assert.match(panel, /focusFigmaEnableResult/)
  assert.match(header, /setAttribute\('aria-label', els\.label\.textContent\)/)
  assert.doesNotMatch(header, /setAttribute\('aria-label', t\('figma\.toggle'\)\)/)

  const calls = []
  const views = {
    feature: { focusAction: () => calls.push('feature') },
    integration: { focusPrimary: () => calls.push('integration') }
  }
  assert.equal(focusFigmaEnableResult({ state: 'restart-required' }, null, views), 'feature')
  assert.deepEqual(calls, ['feature'])
  calls.length = 0
  assert.equal(focusFigmaEnableResult({ state: 'enabled' }, { status: 'ready' }, views), 'integration')
  assert.deepEqual(calls, ['integration'])
})

console.log(`figma integration contracts: ${checks} checks passed`)
