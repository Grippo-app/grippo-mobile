import { canonicalHash, bytesHash, compareText } from '../runtime/canonical-json.mjs'
import { createSchemaRegistry, schemaError } from '../runtime/schema-registry.mjs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = resolve(dirname(fileURLToPath(import.meta.url)))
const registry = createSchemaRegistry(resolve(HERE, '..', 'schemas'))
const validateIntentSchema = registry.validate('token-source-ingestion-intent')

function fail(detail) {
  const error = new Error(`TOKEN_SOURCE_INGESTION_INTENT_INVALID: ${detail}`)
  error.code = 'TOKEN_SOURCE_INGESTION_INTENT_INVALID'
  throw error
}

export function validateTaskIngestionIntent(intent) {
  const schema = schemaError(validateIntentSchema, intent)
  if (schema) fail(schema)
  const sources = intent.sources
  const seen = new Set()
  let previous = null
  for (const row of sources) {
    if (seen.has(row.sourceId)) fail(`duplicate sourceId ${row.sourceId}`)
    if (previous !== null && compareText(previous, row.sourceId) >= 0) fail('sources are not strictly sorted')
    seen.add(row.sourceId)
    previous = row.sourceId
  }
  return intent
}

export function buildTaskIngestionIntent({
  receipt, expectedGenerationRevision, receiptManifestPath
}) {
  if (!receipt || !receipt.manifest || !receipt.manifestHash || !receipt.scope) {
    fail('validated task observation receipt is required')
  }
  const sources = receipt.sidecars.map((row) => ({
    sourceId: row.capture.source.sourceId,
    captureOperationId: row.capture.captureOperationId,
    captureSequence: row.capture.captureSequence,
    semanticHash: row.batch.batchSemanticHash
  })).sort((a, b) => compareText(a.sourceId, b.sourceId))
  const identity = canonicalHash({
    originTransactionId: receipt.manifest.originTransactionId,
    taskStem: receipt.manifest.taskStem,
    receiptManifestHash: receipt.manifestHash,
    sources
  }).slice('sha256:'.length)
  const intent = {
    schemaVersion: 1,
    intentId: `tokintent_${identity}`,
    originTransactionId: receipt.manifest.originTransactionId,
    taskStem: receipt.manifest.taskStem,
    receiptManifestHash: receipt.manifestHash,
    receiptManifestPath,
    expectedGenerationRevision: String(expectedGenerationRevision || 'none'),
    scope: receipt.scope,
    sources,
    state: 'committed'
  }
  validateTaskIngestionIntent(intent)
  const bytes = Buffer.from(JSON.stringify(intent, null, 2) + '\n')
  return { intent, bytes, hash: bytesHash(bytes) }
}
