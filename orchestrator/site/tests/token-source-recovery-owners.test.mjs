#!/usr/bin/env node

import assert from 'node:assert/strict'
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const root = mkdtempSync(join(tmpdir(), 'token-source-recovery-owners-'))
process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_CACHE_DIR = join(root, 'orchestrator', '.cache')
process.env.RUNNER_DISABLED = '1'

const require = createRequire(import.meta.url)
const identity = require('../../figma/runtime/token-identity.cjs')
const sync = require('../server/figma-sync.js')
const ingestion = require('../server/token-source-ingestion.js')
const screenPlans = require('../server/screen-token-plans.js')

const hash = (char) => 'sha256:' + char.repeat(64)
const startedAt = '2026-07-23T12:00:00.000Z'
const scope = { fileKeyFingerprint: hash('1'), branchKey: 'branch:test' }
const tokenJobId = 'fsj-' + '1'.repeat(32)
const componentJobId = 'fsj-' + '2'.repeat(32)
const generations = join(root, 'orchestrator', '.cache', 'figma', 'generations')
const tokenRoot = join(generations, tokenJobId, 'tokens')
const componentRoot = join(generations, componentJobId, 'components')

function writeJson(file, value) {
  mkdirSync(join(file, '..'), { recursive: true })
  writeFileSync(file, JSON.stringify(value, null, 2) + '\n', { mode: 0o600 })
}

const source = {
  fileKeyFingerprint: scope.fileKeyFingerprint,
  branchKey: scope.branchKey,
  nodeId: '12:34',
  kind: 'screen',
  context: { theme: 'light', locale: 'default', platform: 'shared' },
  origin: {
    kind: 'task-screen',
    taskStem: 'TASK_1_fixture',
    screenKey: 'Home',
    variantId: 'light-default-shared'
  }
}
source.sourceId = identity.sourceIdFor(source)
const operationId = 'tokop_' + '1'.repeat(32)
const bucket = identity.sourceBucket(source.sourceId)
const record = {
  captureOperationId: operationId,
  captureSequence: 7,
  accountFingerprint: hash('2'),
  connectorRevision: 'figma-mcp-session-v1',
  semanticPreflightHash: identity.hash({
    captureOperationId: operationId,
    captureSequence: 7,
    sourceId: source.sourceId
  }),
  source,
  origins: [source.origin]
}
const relativePlan = `capture-plan/${String(bucket).padStart(3, '0')}.json`
const relativeIntake = `capture-intake/${String(bucket).padStart(3, '0')}.json`
const tokenIndex = {
  schemaVersion: 1,
  sourceIndexHash: hash('3'),
  revision: 4,
  scope,
  sourceCount: 1,
  planShardFiles: [relativePlan],
  captureShardFiles: [relativeIntake]
}
const tokenShard = {
  schemaVersion: 1,
  bucket,
  scope,
  sourceIndexHash: tokenIndex.sourceIndexHash,
  records: [record]
}

const componentSource = {
  ...source,
  nodeId: '55:89',
  context: { theme: 'dark', locale: 'default', platform: 'shared' }
}
componentSource.sourceId = identity.sourceIdFor(componentSource)
const componentOperationId = 'tokop_' + '2'.repeat(32)
const prospective = Array.from({ length: 128 }, (_, index) => ({
  captureOperationId: 'tokop_' + (index + 256).toString(16).padStart(32, '0'),
  captureSequence: 1
}))
const componentPlan = {
  schemaVersion: 1,
  sourceIndexHash: hash('3'),
  revision: 4,
  scope,
  accountFingerprint: hash('2'),
  connectorRevision: 'figma-mcp-session-v1',
  componentSourceScopeId: 'component-scope:' + '4'.repeat(64),
  newSourceReservations: prospective,
  knownSources: [{
    sourceId: componentSource.sourceId,
    captureOperationId: componentOperationId,
    captureSequence: 9,
    componentOwned: true,
    origins: [{
      kind: 'component-inventory',
      componentScopeId: 'component-scope:' + '4'.repeat(64),
      captureRootNodeId: componentSource.nodeId
    }]
  }],
  captureShardFiles: Array.from({ length: 128 }, (_, index) =>
    `component-token-intake/${String(index).padStart(3, '0')}.json`)
}

try {
  writeJson(join(tokenRoot, 'capture-plan.json'), tokenIndex)
  writeJson(join(tokenRoot, relativePlan), tokenShard)
  writeJson(join(componentRoot, 'component-token-plan.json'), componentPlan)
  const history = {
    [tokenJobId]: { startedAt, planGroups: ['tokens'] },
    [componentJobId]: { startedAt, planGroups: ['components'] }
  }

  const recovered = sync._test.syncStageRecoveryReservations(history)
  assert.deepEqual(recovered.reservations, [{
    sourceId: source.sourceId,
    captureOperationId: operationId,
    captureSequence: 7,
    at: startedAt
  }, {
    sourceId: componentSource.sourceId,
    captureOperationId: componentOperationId,
    captureSequence: 9,
    at: startedAt
  }])
  assert.equal(recovered.prospectiveReservations.length, 128)
  assert.deepEqual(recovered.prospectiveReservations[0], {
    captureOperationId: prospective[0].captureOperationId,
    captureSequence: 1,
    reservedAt: startedAt,
    ownerId: componentJobId
  })

  writeJson(join(tokenRoot, relativePlan), {
    ...tokenShard,
    records: [{ ...record, source: { ...source, nodeId: '99:99' } }]
  })
  assert.throws(() => sync._test.syncStageRecoveryReservations(history),
    /TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE/)
  writeJson(join(tokenRoot, relativePlan), tokenShard)

  writeJson(join(componentRoot, 'component-token-plan.json'), {
    ...componentPlan,
    captureShardFiles: componentPlan.captureShardFiles.map((value, index) =>
      index === 64 ? 'component-token-intake/063.json' : value)
  })
  assert.throws(() => sync._test.syncStageRecoveryReservations(history),
    /TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE/)

  const screenPlanId = 'tokplan_' + '3'.repeat(32)
  const screenPlan = {
    schemaVersion: 1,
    planId: screenPlanId,
    taskStem: 'TASK_1_fixture',
    designSourceHash: hash('4'),
    sourceIndexHash: tokenIndex.sourceIndexHash,
    accountFingerprint: hash('2'),
    connectorRevision: 'figma-mcp-session-v1',
    scope,
    createdAt: startedAt,
    records: [{
      screenKey: 'Home',
      variantId: 'light-default-shared',
      tokensFile: 'Home.tokens.json',
      captureOperationId: operationId,
      captureSequence: 7,
      semanticPreflightHash: record.semanticPreflightHash,
      source
    }]
  }
  writeJson(join(root, 'orchestrator', '.cache', 'figma', 'screen-token-plans',
    screenPlanId + '.json'), screenPlan)
  assert.deepEqual(screenPlans.recoveryReservations(), [{
    sourceId: source.sourceId,
    captureOperationId: operationId,
    captureSequence: 7,
    at: startedAt
  }])

  const orphanReceipt = join(root, 'orchestrator', 'tasks', 'evidence', 'figma-ship',
    'TASK_9_orphan', 'token-observations-manifest.json')
  writeJson(orphanReceipt, { schemaVersion: 1 })
  await assert.rejects(() => ingestion.recoveryReservations(),
    /TOKEN_SOURCE_RESERVATION_OWNER_SCAN_INCOMPLETE/)

  console.log('token source recovery owners: sync, screen, and receipt owner scans passed')
} finally {
  rmSync(root, { recursive: true, force: true })
}
