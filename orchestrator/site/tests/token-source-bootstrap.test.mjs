#!/usr/bin/env node

import assert from 'node:assert/strict'
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = join(HERE, '..', '..', '..')
const root = mkdtempSync(join(tmpdir(), 'token-source-bootstrap-'))
const fileKey = 'abcdefgh'
const fileFingerprint = 'sha256:' + 'a'.repeat(64)
const accountFingerprint = 'sha256:' + 'b'.repeat(64)

for (const column of ['backlog', 'pending', 'todo', 'done']) {
  mkdirSync(join(root, 'orchestrator', 'tasks', column), { recursive: true })
}

writeFileSync(join(root, 'orchestrator', 'tasks', 'backlog', 'TASK_1_bootstrap.md'), [
  '# TASK 1',
  '',
  '## Design',
  `- Root — https://www.figma.com/design/${fileKey}/Fixture?node-id=12-34`,
  `- Login — light:https://www.figma.com/design/${fileKey}/Fixture?node-id=56-78 dark:https://www.figma.com/design/${fileKey}/Fixture?node-id=56-79`,
  ''
].join('\n'))
writeFileSync(join(root, 'orchestrator', 'tasks', 'todo', 'TASK_2_other_file.md'), [
  '## Design',
  '- Other — https://www.figma.com/design/ijklmnop/Other?node-id=90-12',
  ''
].join('\n'))
writeFileSync(join(root, 'orchestrator', 'tasks', 'todo', 'TASK_3_broken.md'), [
  '## Design',
  '- missing separator',
  ''
].join('\n'))

process.env.ORCHESTRATOR_PROJECT_ROOT = root

const require = createRequire(import.meta.url)
const bootstrap = require('../server/token-source-bootstrap.js')
const identity = require('../../figma/runtime/token-identity.cjs')
const { createSchemaRegistry } = await import('../../figma/runtime/schema-registry.mjs')
const { captureSemanticError } = await import('../../figma/tokens/source-contract.mjs')

try {
  assert.equal(bootstrap.configuredNodeId(`https://www.figma.com/design/${fileKey}`), '0:1')
  assert.equal(bootstrap.configuredNodeId(
    `https://www.figma.com/design/${fileKey}?node-id=12-34`
  ), '12:34')

  const discovered = bootstrap.discover({
    scope: { fileKeyFingerprint: fileFingerprint, branchKey: 'none' },
    figmaFileKey: fileKey,
    figmaLibraryUrl: `https://www.figma.com/design/${fileKey}?node-id=12-34`
  })
  assert.equal(discovered.sourceCount, 3)
  assert.deepEqual(
    discovered.sources.map((source) => `${source.nodeId}/${source.context.theme}`).sort(),
    ['12:34/unknown', '56:78/light', '56:79/dark']
  )

  const rootSource = discovered.sources.find((source) => source.nodeId === '12:34')
  assert.deepEqual(rootSource.origins.map((origin) => origin.kind), [
    'project-config',
    'task-screen'
  ])
  assert.equal(rootSource.origin.kind, 'project-config')
  assert.equal(rootSource.acceptedSequence, 0)
  assert.equal(rootSource.sourceId, identity.sourceIdFor({
    fileKeyFingerprint: fileFingerprint,
    branchKey: 'none',
    nodeId: rootSource.nodeId,
    context: rootSource.context
  }))

  const discoveredFromRootUrl = bootstrap.discover({
    scope: { fileKeyFingerprint: fileFingerprint, branchKey: 'none' },
    figmaFileKey: fileKey,
    figmaLibraryUrl: `https://www.figma.com/design/${fileKey}?node-id=0-1`
  })
  assert.equal(discoveredFromRootUrl.sourceCount, 3)
  assert.equal(discoveredFromRootUrl.sources.some((source) => source.nodeId === '0:1'), false)

  const captureSource = {
    sourceId: rootSource.sourceId,
    fileKeyFingerprint: fileFingerprint,
    branchKey: 'none',
    nodeId: rootSource.nodeId,
    kind: rootSource.kind,
    context: rootSource.context,
    origin: rootSource.origin
  }
  const capture = {
    schemaVersion: 1,
    providerCapability: 'node-bound-resolved-variables',
    provider: 'figma-mcp',
    captureOperationId: 'tokop_' + '1'.repeat(32),
    captureSequence: 1,
    accountFingerprint,
    connectorRevision: 'figma-mcp-session-v1',
    source: captureSource,
    observations: [{ providerName: 'Primary/600', rawValue: '#112233' }],
    witness: {
      startedAt: '2026-07-24T10:00:00.000Z',
      finishedAt: '2026-07-24T10:00:01.000Z',
      nodeId: rootSource.nodeId,
      operation: 'get_variable_defs',
      sourceCompleteness: 'complete-returned-payload',
      providerEnumerationCompleteness: 'not-available-for-provider',
      providerTruncationSignal: 'unavailable',
      truncated: false,
      issues: [],
      observationCount: 1,
      accountFingerprint,
      connectorRevision: 'figma-mcp-session-v1',
      producerVersion: 'test-v1'
    }
  }
  const schemas = createSchemaRegistry(join(REPO, 'orchestrator', 'figma', 'schemas'))
  assert.equal(schemas.validate('observed-token-source-capture.schema.json')(capture), true)
  assert.equal(captureSemanticError(capture), null)

  console.log('token source bootstrap: 10 checks passed')
} finally {
  rmSync(root, { recursive: true, force: true })
}
