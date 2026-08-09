import assert from 'node:assert/strict'
import { cpSync, linkSync, mkdirSync, mkdtempSync, readFileSync, rmSync, unlinkSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import { bytesHash } from '../runtime/canonical-json.mjs'
import { buildTaskObservationReceipt, validateCommittedTaskObservationReceipt } from '../tokens/task-observation-receipt.mjs'
import { buildTaskIngestionIntent, validateTaskIngestionIntent } from '../tokens/task-ingestion-intent.mjs'
import { sourceIdentity, validObservedCapture } from './observed-token-fixtures.mjs'

const root = mkdtempSync(join(tmpdir(), 'token-receipt-'))
const screens = join(root, 'screens')
const taskStem = 'TASK_42_Home'
const taskDir = join(screens, taskStem)
const transactionId = 'fin-0123456789abcdef'
mkdirSync(taskDir, { recursive: true })

function fixtureCapture(overrides = {}) {
  const source = sourceIdentity({
    origin: {
      kind: 'task-screen',
      taskStem,
      screenKey: 'Home',
      variantId: 'light-default-shared'
    },
    ...overrides
  })
  return validObservedCapture({ source })
}

function install(capture = fixtureCapture()) {
  const bytes = Buffer.from(JSON.stringify(capture, null, 2) + '\n')
  writeFileSync(join(taskDir, 'Home.tokens.json'), bytes)
  writeFileSync(join(taskDir, 'index.json'), JSON.stringify({
    schemaVersion: 3,
    taskStem,
    nodes: {
      Home: {
        kind: 'screen',
        url: 'https://www.figma.com/design/fixture/App?node-id=10-20',
        nodeId: '10:20',
        fetchedAt: '2026-07-23T10:00:01.000Z',
        variants: [{
          id: 'light-default-shared',
          theme: 'light',
          locale: 'default',
          platform: 'shared',
          url: 'https://www.figma.com/design/fixture/App?node-id=10-20',
          nodeId: '10:20',
          fetchedAt: '2026-07-23T10:00:01.000Z',
          imageFile: 'Home.png',
          tokensFile: 'Home.tokens.json',
          tokensHash: bytesHash(bytes),
          captureOperationId: capture.captureOperationId,
          captureSequence: capture.captureSequence
        }]
      }
    }
  }, null, 2) + '\n')
  return bytes
}

try {
  install()
  const receipt = buildTaskObservationReceipt({ taskStem, transactionId, screensRoot: screens })
  assert.equal(receipt.manifest.sidecars.length, 1)
  assert.equal(receipt.manifest.sidecars[0].semanticPreflightHash, receipt.sidecars[0].batch.batchSemanticHash)
  assert.equal(receipt.manifestHash, bytesHash(receipt.manifestBytes))

  const committed = join(root, 'receipt')
  mkdirSync(join(committed, 'token-observations'), { recursive: true })
  writeFileSync(join(committed, 'token-observations-manifest.json'), receipt.manifestBytes)
  writeFileSync(join(committed, 'token-observations', 'Home.tokens.json'), receipt.sidecars[0].bytes)
  const validated = validateCommittedTaskObservationReceipt({
    taskStem, transactionId, receiptDirectory: committed, expectedManifestHash: receipt.manifestHash
  })
  assert.equal(validated.manifestHash, receipt.manifestHash)

  const intent = buildTaskIngestionIntent({
    receipt,
    expectedGenerationRevision: 'none',
    receiptManifestPath: `orchestrator/tasks/evidence/figma-ship/${taskStem}/token-observations-manifest.json`
  })
  assert.equal(validateTaskIngestionIntent(intent.intent), intent.intent)
  assert.equal(intent.intent.sources[0].semanticHash, receipt.sidecars[0].batch.batchSemanticHash)

  const hardlink = join(taskDir, 'capture-hardlink')
  linkSync(join(taskDir, 'Home.tokens.json'), hardlink)
  assert.throws(
    () => buildTaskObservationReceipt({ taskStem, transactionId, screensRoot: screens }),
    /TOKEN_TASK_RECEIPT_FILE_UNSAFE/
  )
  unlinkSync(hardlink)

  const duplicate = fixtureCapture()
  const duplicateBytes = Buffer.from(JSON.stringify(duplicate, null, 2) + '\n')
  writeFileSync(join(taskDir, 'Home.dark.tokens.json'), duplicateBytes)
  const index = JSON.parse(readFileSync(join(taskDir, 'index.json'), 'utf8'))
  index.nodes.Home.variants.push({
    ...index.nodes.Home.variants[0],
    id: 'dark-default-shared',
    tokensFile: 'Home.dark.tokens.json',
    tokensHash: bytesHash(duplicateBytes)
  })
  writeFileSync(join(taskDir, 'index.json'), JSON.stringify(index, null, 2) + '\n')
  assert.throws(
    () => buildTaskObservationReceipt({ taskStem, transactionId, screensRoot: screens }),
    /TOKEN_TASK_(?:RECEIPT_SIDECAR_MISMATCH|SOURCE_DUPLICATE)/
  )

  install()
  const tampered = JSON.parse(receipt.manifestBytes.toString('utf8'))
  tampered.totalBytes += 1
  writeFileSync(join(committed, 'token-observations-manifest.json'), JSON.stringify(tampered, null, 2) + '\n')
  assert.throws(
    () => validateCommittedTaskObservationReceipt({ taskStem, transactionId, receiptDirectory: committed }),
    /TOKEN_TASK_RECEIPT_MANIFEST_INVALID/
  )

  writeFileSync(join(committed, 'token-observations-manifest.json'), receipt.manifestBytes)
  const escaped = join(root, 'escaped-receipt')
  cpSync(committed, escaped, { recursive: true })
  rmSync(join(escaped, 'token-observations'), { recursive: true })
  // A symlinked receipt subtree must not be traversed.
  const { symlinkSync } = await import('node:fs')
  symlinkSync(join(committed, 'token-observations'), join(escaped, 'token-observations'))
  assert.throws(
    () => validateCommittedTaskObservationReceipt({ taskStem, transactionId, receiptDirectory: escaped }),
    /TOKEN_TASK_RECEIPT_DIRECTORY_UNSAFE/
  )

  console.log('task observation receipt: 7 checks passed')
} finally {
  rmSync(root, { recursive: true, force: true })
}
