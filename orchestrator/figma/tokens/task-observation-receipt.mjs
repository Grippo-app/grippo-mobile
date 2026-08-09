import { lstatSync, readFileSync, realpathSync } from 'node:fs'
import { basename, dirname, isAbsolute, join, parse, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { bytesHash, canonicalHash, compareText } from '../runtime/canonical-json.mjs'
import { createSchemaRegistry, schemaError } from '../runtime/schema-registry.mjs'
import { normalizeSourceCapture } from './source-normalizer.mjs'

const HERE = resolve(fileURLToPath(new URL('.', import.meta.url)))
const SCHEMAS_DIR = resolve(HERE, '..', 'schemas')
const HASH_RE = /^sha256:[a-f0-9]{64}$/
const STEM_RE = /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/
const TRANSACTION_RE = /^fin-[A-Za-z0-9._-]{1,160}$/
const SIDECAR_RE = /^[A-Za-z0-9][A-Za-z0-9_.-]{0,159}\.tokens\.json$/
const SIDECAR_MAX = 4 * 1024 * 1024
const TOTAL_MAX = 64 * 1024 * 1024
const registry = createSchemaRegistry(SCHEMAS_DIR)
const validateCaptureSchema = registry.validate('observed-token-source-capture')
const validateReceiptSchema = registry.validate('token-observation-receipt')

function fail(code, detail) {
  const error = new Error(`${code}: ${detail}`)
  error.code = code
  throw error
}

function realDirectoryTree(directory, label) {
  const absolute = resolve(directory)
  const root = parse(absolute).root
  let current = root
  const parts = absolute.slice(root.length).split(/[\\/]+/).filter(Boolean)
  for (let index = 0; index < parts.length; index++) {
    current = join(current, parts[index])
    let stat
    try { stat = lstatSync(current) } catch (error) {
      fail('TOKEN_TASK_RECEIPT_DIRECTORY_UNSAFE', `${label}: ${error.code || error.message}`)
    }
    if (index === 0 && stat.isSymbolicLink() && stat.uid === 0) {
      current = realpathSync(current)
      stat = lstatSync(current)
    }
    if (stat.isSymbolicLink() || !stat.isDirectory()) {
      fail('TOKEN_TASK_RECEIPT_DIRECTORY_UNSAFE', `${label} contains a symlink or non-directory component`)
    }
  }
  return current
}

function regularBytes(root, path, label, maxBytes) {
  const lexicalRoot = resolve(root)
  const lexicalPath = resolve(path)
  const lexicalRelative = relative(lexicalRoot, lexicalPath)
  if (!lexicalRelative || lexicalRelative === '..' || lexicalRelative.startsWith('../') || isAbsolute(lexicalRelative)) {
    fail('TOKEN_TASK_RECEIPT_PATH_ESCAPE', label)
  }
  const safeRoot = realDirectoryTree(lexicalRoot, `${label} root`)
  const safeParent = realDirectoryTree(dirname(lexicalPath), `${label} parent`)
  const contained = relative(safeRoot, safeParent)
  if (contained === '..' || contained.startsWith('../') || isAbsolute(contained)) fail('TOKEN_TASK_RECEIPT_PATH_ESCAPE', label)
  const safePath = join(safeParent, basename(lexicalPath))
  let stat
  try { stat = lstatSync(safePath) } catch (error) {
    fail('TOKEN_TASK_RECEIPT_FILE_MISSING', `${label}: ${error.code || error.message}`)
  }
  if (stat.isSymbolicLink() || !stat.isFile() || stat.nlink !== 1) {
    fail('TOKEN_TASK_RECEIPT_FILE_UNSAFE', `${label} must be a single-link regular file`)
  }
  if (stat.size < 2 || stat.size > maxBytes) fail('TOKEN_TASK_RECEIPT_SIZE_LIMIT', label)
  const bytes = readFileSync(safePath)
  if (bytes.length !== stat.size) fail('TOKEN_TASK_RECEIPT_FILE_CHANGED', label)
  return bytes
}

function parseJson(bytes, label) {
  try { return JSON.parse(bytes.toString('utf8')) }
  catch (error) { fail('TOKEN_TASK_RECEIPT_JSON_INVALID', `${label}: ${error.message}`) }
}

function receiptSemanticPayload(manifest) {
  const { semanticHash, ...payload } = manifest
  return payload
}

function validateManifestSemantics(manifest) {
  const schema = schemaError(validateReceiptSchema, manifest)
  if (schema) fail('TOKEN_TASK_RECEIPT_MANIFEST_INVALID', schema)
  if (canonicalHash(receiptSemanticPayload(manifest)) !== manifest.semanticHash) {
    fail('TOKEN_TASK_RECEIPT_MANIFEST_INVALID', 'semanticHash mismatch')
  }
  const basenames = new Set()
  const sources = new Set()
  let previous = null
  let total = 0
  for (const row of manifest.sidecars) {
    if (basenames.has(row.basename)) fail('TOKEN_TASK_RECEIPT_BASENAME_DUPLICATE', row.basename)
    if (sources.has(row.sourceId)) fail('TOKEN_TASK_SOURCE_DUPLICATE', row.sourceId)
    if (previous !== null && compareText(previous, row.basename) >= 0) {
      fail('TOKEN_TASK_RECEIPT_MANIFEST_INVALID', 'sidecars are not strictly sorted by basename')
    }
    basenames.add(row.basename)
    sources.add(row.sourceId)
    previous = row.basename
    total += row.size
  }
  if (total !== manifest.totalBytes) fail('TOKEN_TASK_RECEIPT_MANIFEST_INVALID', 'totalBytes mismatch')
}

function normalizeSidecar(bytes, expected = null) {
  const capture = parseJson(bytes, expected ? expected.basename : 'token sidecar')
  const schema = schemaError(validateCaptureSchema, capture)
  if (schema) fail('TOKEN_TASK_SIDECAR_INVALID', schema)
  if (expected) {
    if (bytesHash(bytes) !== expected.bytesHash || bytes.length !== expected.size ||
        capture.source.sourceId !== expected.sourceId ||
        capture.captureOperationId !== expected.captureOperationId ||
        capture.captureSequence !== expected.captureSequence) {
      fail('TOKEN_TASK_RECEIPT_SIDECAR_MISMATCH', expected.basename)
    }
  }
  const batch = normalizeSourceCapture(capture, bytes, expected && {
    sourceId: expected.sourceId,
    captureOperationId: expected.captureOperationId,
    captureSequence: expected.captureSequence,
    accountFingerprint: capture.accountFingerprint,
    connectorRevision: capture.connectorRevision
  })
  if (expected && batch.batchSemanticHash !== expected.semanticPreflightHash) {
    fail('TOKEN_TASK_RECEIPT_SIDECAR_MISMATCH', `${expected.basename} semantic preflight hash`)
  }
  return { capture, batch }
}

function variantsFromIndex(index, taskStem) {
  if (!index || index.schemaVersion !== 3 || index.taskStem !== taskStem ||
      !index.nodes || typeof index.nodes !== 'object' || Array.isArray(index.nodes)) {
    fail('TOKEN_TASK_SCREEN_INDEX_INVALID', 'screen index identity/shape mismatch')
  }
  const rows = []
  for (const screenKey of Object.keys(index.nodes).sort(compareText)) {
    const node = index.nodes[screenKey]
    if (!node || !Array.isArray(node.variants) || !node.variants.length) {
      fail('TOKEN_TASK_SCREEN_INDEX_INVALID', `${screenKey} has no variants`)
    }
    for (const variant of node.variants) {
      if (!SIDECAR_RE.test(String(variant.tokensFile || '')) ||
          !HASH_RE.test(String(variant.tokensHash || '')) ||
          !/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(variant.captureOperationId || '')) ||
          !Number.isSafeInteger(variant.captureSequence) || variant.captureSequence < 1) {
        fail('TOKEN_TASK_SCREEN_INDEX_INVALID', `${screenKey} variant token fields are invalid`)
      }
      rows.push({ screenKey, variant })
    }
  }
  return rows
}

export function buildTaskObservationReceipt({ taskStem, transactionId, screensRoot }) {
  if (!STEM_RE.test(String(taskStem || '')) || !TRANSACTION_RE.test(String(transactionId || ''))) {
    fail('TOKEN_TASK_RECEIPT_ARGUMENT_INVALID', 'task stem or finalization transaction id')
  }
  const taskRoot = join(resolve(screensRoot), taskStem)
  const indexBytes = regularBytes(taskRoot, join(taskRoot, 'index.json'), 'screen index', 1024 * 1024)
  const index = parseJson(indexBytes, 'screen index')
  const variants = variantsFromIndex(index, taskStem)
  if (variants.length > 64) fail('TOKEN_TASK_RECEIPT_SIZE_LIMIT', 'more than 64 token sidecars')

  const seenNames = new Set()
  const seenSources = new Set()
  const sidecars = []
  let totalBytes = 0
  for (const { screenKey, variant } of variants) {
    const name = basename(variant.tokensFile)
    if (name !== variant.tokensFile || seenNames.has(name)) fail('TOKEN_TASK_RECEIPT_BASENAME_DUPLICATE', name)
    const bytes = regularBytes(taskRoot, join(taskRoot, name), `${screenKey}/${variant.id} token sidecar`, SIDECAR_MAX)
    const hash = bytesHash(bytes)
    if (hash !== variant.tokensHash) fail('TOKEN_TASK_RECEIPT_SIDECAR_MISMATCH', `${name} bytes hash`)
    const { capture, batch } = normalizeSidecar(bytes)
    if (capture.source.origin.kind !== 'task-screen' ||
        capture.source.origin.taskStem !== taskStem ||
        capture.source.origin.screenKey !== screenKey ||
        capture.source.origin.variantId !== variant.id ||
        capture.source.nodeId !== variant.nodeId ||
        capture.source.context.theme !== variant.theme ||
        capture.source.context.locale !== variant.locale ||
        capture.source.context.platform !== variant.platform ||
        capture.captureOperationId !== variant.captureOperationId ||
        capture.captureSequence !== variant.captureSequence) {
      fail('TOKEN_TASK_RECEIPT_SIDECAR_MISMATCH', `${name} does not match screen index`)
    }
    if (seenSources.has(capture.source.sourceId)) fail('TOKEN_TASK_SOURCE_DUPLICATE', capture.source.sourceId)
    seenNames.add(name)
    seenSources.add(capture.source.sourceId)
    totalBytes += bytes.length
    if (totalBytes > TOTAL_MAX) fail('TOKEN_TASK_RECEIPT_SIZE_LIMIT', 'aggregate sidecar bytes')
    sidecars.push({
      basename: name,
      sourceId: capture.source.sourceId,
      captureOperationId: capture.captureOperationId,
      captureSequence: capture.captureSequence,
      bytesHash: hash,
      semanticPreflightHash: batch.batchSemanticHash,
      size: bytes.length,
      bytes,
      capture,
      batch
    })
  }
  sidecars.sort((a, b) => compareText(a.basename, b.basename))
  const scope = sidecars.length ? {
    fileKeyFingerprint: sidecars[0].capture.source.fileKeyFingerprint,
    branchKey: sidecars[0].capture.source.branchKey
  } : null
  if (!scope || sidecars.some((row) =>
    row.capture.source.fileKeyFingerprint !== scope.fileKeyFingerprint ||
    row.capture.source.branchKey !== scope.branchKey)) {
    fail('TOKEN_TASK_RECEIPT_SCOPE_MISMATCH', 'sidecars span multiple file/branch scopes')
  }
  const manifest = {
    schemaVersion: 1,
    taskStem,
    taskSourceId: `task-token-observations:${canonicalHash({ taskStem, scope })}`,
    originTransactionId: transactionId,
    sidecars: sidecars.map(({ bytes, capture, batch, ...row }) => row),
    totalBytes,
    semanticHash: 'sha256:' + '0'.repeat(64)
  }
  manifest.semanticHash = canonicalHash(receiptSemanticPayload(manifest))
  validateManifestSemantics(manifest)
  const manifestBytes = Buffer.from(JSON.stringify(manifest, null, 2) + '\n')
  return { manifest, manifestBytes, manifestHash: bytesHash(manifestBytes), sidecars, scope }
}

export function validateCommittedTaskObservationReceipt({
  taskStem, transactionId, receiptDirectory, expectedManifestHash = null
}) {
  const root = resolve(receiptDirectory)
  const manifestPath = join(root, 'token-observations-manifest.json')
  const manifestBytes = regularBytes(root, manifestPath, 'token observation manifest', 1024 * 1024)
  const manifestHash = bytesHash(manifestBytes)
  if (expectedManifestHash && manifestHash !== expectedManifestHash) {
    fail('TOKEN_TASK_RECEIPT_MANIFEST_MISMATCH', 'manifest bytes hash differs from expected hash')
  }
  const manifest = parseJson(manifestBytes, 'token observation manifest')
  validateManifestSemantics(manifest)
  if (manifest.taskStem !== taskStem || manifest.originTransactionId !== transactionId) {
    fail('TOKEN_TASK_RECEIPT_MANIFEST_MISMATCH', 'task or transaction identity mismatch')
  }
  const sidecarRoot = join(root, 'token-observations')
  const sidecars = manifest.sidecars.map((row) => {
    const bytes = regularBytes(sidecarRoot, join(sidecarRoot, row.basename), row.basename, SIDECAR_MAX)
    const { capture, batch } = normalizeSidecar(bytes, row)
    return { ...row, bytes, capture, batch }
  })
  const scope = {
    fileKeyFingerprint: sidecars[0].capture.source.fileKeyFingerprint,
    branchKey: sidecars[0].capture.source.branchKey
  }
  if (sidecars.some((row) =>
    row.capture.source.fileKeyFingerprint !== scope.fileKeyFingerprint ||
    row.capture.source.branchKey !== scope.branchKey)) {
    fail('TOKEN_TASK_RECEIPT_SCOPE_MISMATCH', 'committed sidecars span multiple scopes')
  }
  return { manifest, manifestBytes, manifestHash, sidecars, scope }
}
