// run-plan.mjs — the single fixed entrypoint the server-owned trusted runner
// executes as a child process (shell:false, sanitized env, timeout,
// process-group kill). The server writes an exact plan file into its own
// staging directory and passes its path as the only argument; no browser
// value ever reaches this argv. Every output is written atomically inside
// the staging directory named by the plan; the process prints one bounded
// JSON result line on stdout and exits 0 (even for typed domain failures —
// the caller reads `ok`/`code`, a non-zero exit means the runner itself
// broke).
//
// Ops:
//   normalize-token-captures    — fixed capture shards -> source shards/index/catalog
//   ingest-token-receipt        — committed task sidecars + active source shards ->
//                                 rebased source shards/index/catalog
//   token-compare               — token adapter extraction + mapping snapshot +
//                                 comparison (+ baseline) into the staging directory
//   normalize-component-capture — staged capture.json (+ visual/ PNGs) ->
//                                 design-component-inventory.json
//   component-compare           — component adapter extraction + mapping snapshot +
//                                 comparison + suggestions + task proposals
//                                 (+ baseline) into the staging directory
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, isAbsolute, join, resolve, sep } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { bytesHash, canonicalHash, canonicalJson } from './canonical-json.mjs'
import { readContainedSingleLinkFile } from './file-safety.mjs'
import { isTypedError } from './typed-error.mjs'
import { TOKEN_ERROR_CODES } from '../tokens/error-codes.mjs'
import { COMPONENT_ERROR_CODES } from '../components/error-codes.mjs'
import limits from './program-limits.cjs'

const PLAN_BYTES_MAX = limits.stageManifestBytesMax
const INPUT_BYTES_MAX = limits.artifactBytesMax
const VISUAL_BYTES_MAX = 2 * 1024 * 1024

function fail(code, detail) {
  process.stdout.write(JSON.stringify({ ok: false, code, detail: String(detail || '').slice(0, 500) }) + '\n')
  process.exit(0)
}

function containedFile(baseDir, file, label) {
  const absolute = resolve(baseDir, file)
  if (absolute !== baseDir && !absolute.startsWith(baseDir + sep)) {
    throw new Error(`${label} escapes its directory`)
  }
  return absolute
}

function readBounded(file, max, root = dirname(file)) {
  return readContainedSingleLinkFile({ root, file, maxBytes: max })
}

const PLAN_KEYS = Object.freeze({
  'normalize-token-captures': Object.freeze({
    required: ['op', 'stageDir', 'captureShardFiles', 'expectedCapturePlanFiles', 'outDir', 'scope', 'revision'],
    optional: ['previousCatalogFile', 'previousIndexFile', 'existingSourceShardFiles']
  }),
  'ingest-token-receipt': Object.freeze({
    required: ['op', 'stageDir', 'captureFiles', 'existingSourceShardFiles', 'intentSources', 'outDir', 'scope', 'revision'],
    optional: ['previousCatalogFile', 'previousIndexFile', 'retireSourceIds', 'detachOrigin']
  }),
  'token-compare': Object.freeze({
    required: ['op', 'stageDir', 'projectRoot', 'observedCatalogFile', 'sourceIndexFile', 'sourceFreshness', 'eligibleAt'],
    optional: ['previousBaselineFile']
  }),
  'normalize-component-capture': Object.freeze({
    required: ['op', 'stageDir', 'captureFile', 'outFile'], optional: []
  }),
  'component-compare': Object.freeze({
    required: ['op', 'stageDir', 'projectRoot', 'designInventoryFile', 'designGenerationId', 'eligibleAt'],
    optional: ['previousBaselineFile', 'tokenComparisonFile', 'tokenBindingSnapshotFile']
  })
})

function validPlanShape(plan) {
  if (!plan || typeof plan !== 'object' || Array.isArray(plan) || typeof plan.op !== 'string') return false
  const contract = PLAN_KEYS[plan.op]
  if (!contract) return false
  const allowed = new Set([...contract.required, ...contract.optional])
  return contract.required.every((key) => Object.hasOwn(plan, key)) &&
    Object.keys(plan).every((key) => allowed.has(key))
}

function parseBoundedJson(file, max, code, label, root = dirname(file)) {
  let bytes
  try { bytes = readBounded(file, max, root) } catch (error) {
    fail(code, `${label} is unreadable (${error && error.code || 'read-failed'})`)
  }
  try { return JSON.parse(bytes.toString('utf8')) } catch (error) {
    fail(code, `${label} is not valid JSON`)
  }
}

function validDetachOriginSelector(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false
  const keys = Object.keys(value).sort().join('\0')
  if (value.kind === 'component-inventory') {
    return (keys === ['componentScopeId', 'kind'].sort().join('\0') ||
      keys === ['captureRootNodeId', 'componentScopeId', 'kind'].sort().join('\0')) &&
      typeof value.componentScopeId === 'string' && value.componentScopeId.length >= 12 &&
      value.componentScopeId.length <= 256 &&
      (value.captureRootNodeId === undefined ||
        /^[0-9]+:[0-9]+$/.test(String(value.captureRootNodeId || '')))
  }
  if (value.kind === 'task-screen') {
    return keys === ['kind', 'screenKey', 'taskStem', 'variantId'].sort().join('\0') &&
      /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/.test(String(value.taskStem || '')) &&
      typeof value.screenKey === 'string' && value.screenKey.length >= 1 && value.screenKey.length <= 128 &&
      typeof value.variantId === 'string' && value.variantId.length >= 1 && value.variantId.length <= 128
  }
  if (value.kind === 'task-component') {
    return keys === ['designComponentId', 'kind', 'taskStem'].sort().join('\0') &&
      /^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$/.test(String(value.taskStem || '')) &&
      typeof value.designComponentId === 'string' &&
      value.designComponentId.length >= 12 && value.designComponentId.length <= 256
  }
  return false
}

function originMatchesSelector(origin, selector) {
  return Object.keys(selector).every((key) => origin[key] === selector[key])
}

function writeFresh(file, text) {
  mkdirSync(dirname(file), { recursive: true })
  // Stage directories are private and unpublished. O_EXCL is the important
  // invariant here: a pre-existing symlink, hardlink, stale output, or racing
  // writer must be rejected instead of replaced or followed.
  writeFileSync(file, text, { flag: 'wx', mode: 0o600 })
}

async function main() {
  const [flag, planPath] = process.argv.slice(2)
  if (process.argv.length !== 4 || flag !== '--plan' || !planPath || !isAbsolute(planPath)) {
    fail('RUN_PLAN_USAGE', 'usage: run-plan.mjs --plan <absolute-plan-file>')
  }
  let plan
  try {
    plan = JSON.parse(readBounded(planPath, PLAN_BYTES_MAX).toString('utf8'))
  } catch (error) {
    fail('RUN_PLAN_INVALID', error.message)
  }
  if (!validPlanShape(plan)) fail('RUN_PLAN_INVALID', 'plan does not match the exact operation contract')
  const stageDir = plan.stageDir
  if (typeof stageDir !== 'string' || !isAbsolute(stageDir)) fail('RUN_PLAN_INVALID', 'stageDir must be absolute')
  if (resolve(dirname(planPath)) !== resolve(stageDir)) fail('RUN_PLAN_INVALID', 'the plan file must live inside its own stage directory')

  // URL.pathname is not a filesystem path: it keeps percent encoding and on
  // Windows prefixes drive paths with a slash. The trusted runner must locate
  // its schemas and domain modules identically on every supported platform.
  const here = dirname(fileURLToPath(import.meta.url))
  const schemasDir = join(here, '..', 'schemas')
  const { createSchemaRegistry } = await import('./schema-registry.mjs')
  const schemas = createSchemaRegistry(schemasDir)
  const compile = (name) => schemas.validate(name)

  try {
    if (plan.op === 'ingest-token-receipt') {
      const exactScope = plan.scope && typeof plan.scope === 'object' && !Array.isArray(plan.scope) &&
        Object.keys(plan.scope).sort().join('\0') === ['branchKey', 'fileKeyFingerprint'].sort().join('\0') &&
        /^sha256:[a-f0-9]{64}$/.test(String(plan.scope.fileKeyFingerprint || '')) &&
        typeof plan.scope.branchKey === 'string' && !!plan.scope.branchKey
      if (!exactScope || !Number.isSafeInteger(plan.revision) || plan.revision < 0 ||
          !Array.isArray(plan.captureFiles) || plan.captureFiles.length > 128 ||
          plan.captureFiles.some((file) => typeof file !== 'string') ||
          !Array.isArray(plan.existingSourceShardFiles) || plan.existingSourceShardFiles.length > 128 ||
          plan.existingSourceShardFiles.some((file) => typeof file !== 'string') ||
          !Array.isArray(plan.intentSources) || plan.intentSources.length !== plan.captureFiles.length ||
          plan.retireSourceIds !== undefined && (!Array.isArray(plan.retireSourceIds) ||
            plan.retireSourceIds.length > 4096 ||
            plan.retireSourceIds.some((sourceId) => !/^otsrc:sha256:[a-f0-9]{64}$/.test(String(sourceId || '')))) ||
          plan.detachOrigin !== undefined && !validDetachOriginSelector(plan.detachOrigin)) {
        fail('RUN_PLAN_INVALID', 'task token ingestion plan is invalid')
      }
      if (!plan.captureFiles.length && !(plan.retireSourceIds && plan.retireSourceIds.length) && !plan.detachOrigin) {
        fail('RUN_PLAN_INVALID', 'token ingestion has neither captures nor retirements')
      }
      const { normalizeSourceCapture } = await import(pathToFileURL(join(here, '..', 'tokens', 'source-normalizer.mjs')).href)
      const { aggregateObservedTokens } = await import(pathToFileURL(join(here, '..', 'tokens', 'catalog-aggregator.mjs')).href)
      const { sourceBatchSemanticError, sourceBatchSemanticPayload } = await import(pathToFileURL(join(here, '..', 'tokens', 'source-contract.mjs')).href)
      const validateCapture = compile('observed-token-source-capture.schema.json')
      const validateSourceShard = compile('observed-token-source-shard.schema.json')
      const validateIndex = compile('observed-token-source-index.schema.json')
      const validateCatalog = compile('observed-token-catalog.schema.json')
      const expected = new Map()
      for (const row of plan.intentSources) {
        if (!row || typeof row !== 'object' || Array.isArray(row) ||
            Object.keys(row).sort().join('\0') !== ['captureOperationId', 'captureSequence', 'semanticHash', 'sourceId'].sort().join('\0') ||
            !/^otsrc:sha256:[a-f0-9]{64}$/.test(String(row.sourceId || '')) ||
            !/^tokop_[A-Za-z0-9_-]{16,96}$/.test(String(row.captureOperationId || '')) ||
            !Number.isSafeInteger(row.captureSequence) || row.captureSequence < 1 ||
            !/^sha256:[a-f0-9]{64}$/.test(String(row.semanticHash || '')) || expected.has(row.sourceId)) {
          fail('RUN_PLAN_INVALID', 'intentSources contains an invalid or duplicate source')
        }
        expected.set(row.sourceId, row)
      }
      const incoming = []
      const seenIncoming = new Set()
      for (const relative of plan.captureFiles) {
        const file = containedFile(resolve(stageDir), relative, 'captureFiles')
        let bytes
        try { bytes = readBounded(file, INPUT_BYTES_MAX, resolve(stageDir)) }
        catch (error) { fail('TOKEN_TASK_RECEIPT_INVALID', `capture sidecar unreadable (${error.code || error.message})`) }
        let capture
        try { capture = JSON.parse(bytes.toString('utf8')) }
        catch { fail('TOKEN_TASK_RECEIPT_INVALID', 'capture sidecar is not valid JSON') }
        if (!validateCapture(capture)) {
          const first = validateCapture.errors[0]
          fail('TOKEN_TASK_RECEIPT_INVALID', `capture schema: ${(first.instancePath || '/') + ' ' + first.message}`)
        }
        const held = expected.get(capture.source.sourceId)
        if (!held || seenIncoming.has(capture.source.sourceId)) {
          fail('TOKEN_TASK_RECEIPT_INVALID', 'capture source set differs from committed intent')
        }
        const batch = normalizeSourceCapture(capture, bytes, {
          sourceId: held.sourceId,
          captureOperationId: held.captureOperationId,
          captureSequence: held.captureSequence,
          accountFingerprint: capture.accountFingerprint,
          connectorRevision: capture.connectorRevision
        })
        if (batch.batchSemanticHash !== held.semanticHash) {
          fail('TOKEN_TASK_RECEIPT_INVALID', 'capture semantic hash differs from committed intent')
        }
        seenIncoming.add(batch.sourceId)
        incoming.push(batch)
      }
      if (seenIncoming.size !== expected.size) fail('TOKEN_TASK_RECEIPT_INVALID', 'committed intent source is missing')

      const existing = []
      const existingBySource = new Map()
      const shardBuckets = new Set()
      for (const relative of plan.existingSourceShardFiles) {
        const file = containedFile(resolve(stageDir), relative, 'existingSourceShardFiles')
        const shard = parseBoundedJson(file, INPUT_BYTES_MAX,
          'TOKEN_GENERATION_RESYNC_REQUIRED', 'active observed-token source shard', resolve(stageDir))
        if (!validateSourceShard(shard) || shardBuckets.has(shard.bucket)) {
          fail('TOKEN_GENERATION_RESYNC_REQUIRED', 'active source shard schema/bucket invalid')
        }
        shardBuckets.add(shard.bucket)
        for (const batch of shard.sources) {
          const semantic = sourceBatchSemanticError(batch)
          if (semantic || existingBySource.has(batch.sourceId)) {
            fail('TOKEN_GENERATION_RESYNC_REQUIRED', semantic || 'active source is duplicated across shards')
          }
          existingBySource.set(batch.sourceId, batch)
          existing.push(batch)
        }
      }
      const acceptedSources = []
      const supersededSources = []
      for (const batch of incoming) {
        const prior = existingBySource.get(batch.sourceId)
        if (prior && prior.captureSequence > batch.captureSequence) {
          if (!prior.origins.some((origin) => canonicalJson(origin) === canonicalJson(batch.origins[0]))) {
            fail('TOKEN_SOURCE_ORIGIN_REBOUND', `newer source no longer carries receipt origin ${batch.sourceId}`)
          }
          supersededSources.push(batch.sourceId)
        }
        else acceptedSources.push(batch.sourceId)
      }

      let previousCatalog = null
      if (plan.previousCatalogFile) {
        previousCatalog = parseBoundedJson(
          containedFile(resolve(stageDir), String(plan.previousCatalogFile), 'previousCatalogFile'),
          INPUT_BYTES_MAX, 'TOKEN_GENERATION_RESYNC_REQUIRED', 'previous observed token catalog', resolve(stageDir))
        if (!validateCatalog(previousCatalog)) fail('TOKEN_GENERATION_RESYNC_REQUIRED', 'previous catalog schema-invalid')
      }
      let previousIndex = null
      if (plan.previousIndexFile) {
        previousIndex = parseBoundedJson(
          containedFile(resolve(stageDir), String(plan.previousIndexFile), 'previousIndexFile'),
          INPUT_BYTES_MAX, 'TOKEN_GENERATION_RESYNC_REQUIRED', 'previous observed token source index', resolve(stageDir))
        if (!validateIndex(previousIndex)) fail('TOKEN_GENERATION_RESYNC_REQUIRED', 'previous source index schema-invalid')
      }
      const previousBySource = new Map()
      if (previousIndex) {
        for (const source of previousIndex.sources) {
          const batch = existingBySource.get(source.sourceId)
          if (!batch || source.acceptedBatch.batchSemanticHash !== batch.batchSemanticHash ||
              source.acceptedBatch.captureSequence !== batch.captureSequence ||
              canonicalJson(source.origins) !== canonicalJson(batch.origins)) {
            fail('TOKEN_GENERATION_RESYNC_REQUIRED', `source index/shard mismatch ${source.sourceId}`)
          }
          previousBySource.set(source.sourceId, source)
        }
        if (previousBySource.size !== existingBySource.size) {
          fail('TOKEN_GENERATION_RESYNC_REQUIRED', 'source index/shard source sets differ')
        }
      } else if (existing.length) {
        fail('TOKEN_GENERATION_RESYNC_REQUIRED', 'source shards require their exact source index')
      }
      for (let index = 0; index < incoming.length; index++) {
        const batch = incoming[index]
        const priorBatch = existingBySource.get(batch.sourceId)
        const priorSource = previousBySource.get(batch.sourceId)
        if (!priorBatch || !priorSource) continue
        if (priorSource.lifecycle === 'retired' &&
            batch.captureSequence <= priorBatch.captureSequence) {
          fail('TOKEN_SOURCE_SEQUENCE_SUPERSEDED',
            `retired source requires a newer successful recapture ${batch.sourceId}`)
        }
        if (priorSource.lifecycle !== 'active' ||
            batch.captureSequence < priorBatch.captureSequence) continue
        const originsByKey = new Map([...priorBatch.origins, ...batch.origins]
          .map((origin) => [canonicalJson(origin), origin]))
        const merged = {
          ...batch,
          origins: [...originsByKey.entries()]
            .sort(([left], [right]) => left.localeCompare(right))
            .map(([, origin]) => origin),
          batchSemanticHash: 'sha256:' + '0'.repeat(64)
        }
        merged.batchSemanticHash = canonicalHash(sourceBatchSemanticPayload(merged))
        if (batch.captureSequence === priorBatch.captureSequence &&
            (merged.captureOperationId !== priorBatch.captureOperationId ||
              merged.captureEvidenceHash !== priorBatch.captureEvidenceHash ||
              merged.batchSemanticHash !== priorBatch.batchSemanticHash)) {
          fail('TOKEN_SOURCE_SEQUENCE_CONFLICT',
            `same source sequence carries different bytes ${batch.sourceId}`)
        }
        incoming[index] = merged
      }
      const retired = new Set(plan.retireSourceIds || [])
      const incomingBySource = new Map(incoming.map((batch) => [batch.sourceId, batch]))
      for (const sourceId of retired) {
        if (!existingBySource.has(sourceId) || incomingBySource.has(sourceId)) {
          fail('TOKEN_SOURCE_MUTATION_CONFLICT', `retirement target is absent or concurrently recaptured ${sourceId}`)
        }
      }
      const retainedExisting = []
      const retainedRetired = []
      for (const batch of existing) {
        if (incomingBySource.has(batch.sourceId)) continue
        const prior = previousBySource.get(batch.sourceId)
        if (!prior) fail('TOKEN_GENERATION_RESYNC_REQUIRED', `source index entry missing ${batch.sourceId}`)
        if (prior.lifecycle === 'retired') {
          retainedRetired.push({
            sourceId: batch.sourceId,
            nodeId: batch.nodeId,
            kind: batch.kind,
            context: batch.context,
            origins: batch.origins,
            batch
          })
          continue
        }
        if (retired.has(batch.sourceId)) {
          retainedRetired.push({
            sourceId: batch.sourceId,
            nodeId: batch.nodeId,
            kind: batch.kind,
            context: batch.context,
            origins: batch.origins,
            batch
          })
          continue
        }
        if (!plan.detachOrigin) {
          retainedExisting.push(batch)
          continue
        }
        const origins = batch.origins.filter((origin) =>
          !originMatchesSelector(origin, plan.detachOrigin))
        if (origins.length === batch.origins.length) {
          retainedExisting.push(batch)
          continue
        }
        if (!origins.length) {
          // A retired source retains its last accepted immutable batch and
          // historical provenance. Lifecycle, not physical deletion, removes
          // it from active aggregation.
          retainedRetired.push({
            sourceId: batch.sourceId,
            nodeId: batch.nodeId,
            kind: batch.kind,
            context: batch.context,
            origins: batch.origins,
            batch
          })
          continue
        }
        const detached = { ...batch, origins, batchSemanticHash: 'sha256:' + '0'.repeat(64) }
        detached.batchSemanticHash = canonicalHash(sourceBatchSemanticPayload(detached))
        retainedExisting.push(detached)
      }
      const result = aggregateObservedTokens({
        scope: plan.scope,
        batches: retainedExisting.concat(incoming),
        revision: plan.revision,
        previousCatalog,
        previousIndex,
        retiredSources: retainedRetired
      })
      const healthEvidence = incoming
        .map((batch) => ({
          sourceId: batch.sourceId,
          captureOperationId: batch.captureOperationId,
          captureSequence: batch.captureSequence,
          captureEvidenceHash: batch.captureEvidenceHash
        }))
        .sort((left, right) => left.sourceId.localeCompare(right.sourceId))
      const noOp = !!previousIndex &&
        canonicalJson(previousIndex.scope) === canonicalJson(result.index.scope) &&
        canonicalJson(previousIndex.sources) === canonicalJson(result.index.sources)
      if (noOp) {
        process.stdout.write(JSON.stringify({
          ok: true, op: plan.op, noOp: true, artifacts: [], acceptedSources,
          supersededSources, sourceIndexHash: previousIndex.semanticHash,
          healthEvidence,
          counts: previousCatalog ? previousCatalog.counts : result.catalog.counts,
          totalBytes: 0
        }) + '\n')
        return
      }
      const outDir = containedFile(resolve(stageDir), String(plan.outDir || 'publication'), 'outDir')
      const artifacts = []
      let totalBytes = 0
      const emit = (name, role, value, validate) => {
        if (!validate(value)) {
          const first = validate.errors[0]
          fail('TOKEN_PUBLICATION_OUTPUT_INVALID', `${role}: ${(first.instancePath || '/') + ' ' + first.message}`)
        }
        const text = JSON.stringify(value, null, 2) + '\n'
        totalBytes += Buffer.byteLength(text)
        writeFresh(containedFile(outDir, name, 'publication artifact'), text)
        artifacts.push({ file: join(String(plan.outDir), name), role, bytes: Buffer.byteLength(text) })
      }
      for (const { role, shard } of result.shards) {
        emit(`source-shard-${String(shard.bucket).padStart(3, '0')}.json`, role, shard, validateSourceShard)
      }
      emit('source-index.json', 'observed-token-source-index', result.index, validateIndex)
      emit('observed-token-catalog.json', 'observed-token-catalog', result.catalog, validateCatalog)
      if (artifacts.length > limits.manifestArtifactsMax || totalBytes > limits.phaseBytesMax) {
        fail('TOKEN_PUBLICATION_BUDGET_EXCEEDED', 'token publication output exceeds artifact/byte budget')
      }
      process.stdout.write(JSON.stringify({
        ok: true, op: plan.op, noOp: false, artifacts, acceptedSources, supersededSources,
        counts: result.catalog.counts, sourceIndexHash: result.index.semanticHash,
        catalogHash: result.catalog.semanticHash, healthEvidence, totalBytes
      }) + '\n')
      return
    }

    if (plan.op === 'normalize-token-captures') {
      if (!Array.isArray(plan.captureShardFiles) || plan.captureShardFiles.length > 128 ||
          plan.captureShardFiles.some((file) => typeof file !== 'string')) {
        fail('RUN_PLAN_INVALID', 'captureShardFiles must be a bounded exact file list')
      }
      if (!Array.isArray(plan.expectedCapturePlanFiles) || plan.expectedCapturePlanFiles.length > 128 ||
          plan.expectedCapturePlanFiles.some((file) => typeof file !== 'string')) {
        fail('RUN_PLAN_INVALID', 'expectedCapturePlanFiles must be a bounded exact file list')
      }
      if (plan.existingSourceShardFiles !== undefined &&
          (!Array.isArray(plan.existingSourceShardFiles) || plan.existingSourceShardFiles.length > 128 ||
            plan.existingSourceShardFiles.some((file) => typeof file !== 'string'))) {
        fail('RUN_PLAN_INVALID', 'existingSourceShardFiles must be a bounded exact file list')
      }
      if (!plan.scope || typeof plan.scope !== 'object' || Array.isArray(plan.scope) ||
          Object.keys(plan.scope).sort().join('\0') !== ['branchKey', 'fileKeyFingerprint'].sort().join('\0') ||
          !/^sha256:[a-f0-9]{64}$/.test(String(plan.scope.fileKeyFingerprint || '')) ||
          typeof plan.scope.branchKey !== 'string' || !plan.scope.branchKey ||
          !Number.isSafeInteger(plan.revision) || plan.revision < 0) {
        fail('RUN_PLAN_INVALID', 'scope/revision does not match the token publication contract')
      }
      const { normalizeSourceCapture } = await import(pathToFileURL(join(here, '..', 'tokens', 'source-normalizer.mjs')).href)
      const { aggregateObservedTokens } = await import(pathToFileURL(join(here, '..', 'tokens', 'catalog-aggregator.mjs')).href)
      const { sourceBucket } = await import(pathToFileURL(join(here, '..', 'tokens', 'source-contract.mjs')).href)
      const validateCaptureShard = compile('observed-token-capture-shard.schema.json')
      const validateRefreshPlanShard = compile('token-source-refresh-plan-shard.schema.json')
      const validateSourceShard = compile('observed-token-source-shard.schema.json')
      const validateIndex = compile('observed-token-source-index.schema.json')
      const validateCatalog = compile('observed-token-catalog.schema.json')
      const batches = []
      const seenBuckets = new Set()
      const expectedBySource = new Map()
      const expectedPlanBuckets = new Set()
      for (const relative of plan.expectedCapturePlanFiles) {
        const expectedFile = containedFile(resolve(stageDir), relative, 'expectedCapturePlanFiles')
        const planShard = parseBoundedJson(expectedFile, INPUT_BYTES_MAX,
          'RUN_PLAN_INVALID', 'token refresh plan shard', resolve(stageDir))
        if (!validateRefreshPlanShard(planShard) || expectedPlanBuckets.has(planShard.bucket) ||
            canonicalJson(planShard.scope) !== canonicalJson(plan.scope)) {
          fail('RUN_PLAN_INVALID', 'refresh plan shard schema/bucket/scope invalid')
        }
        expectedPlanBuckets.add(planShard.bucket)
        for (const record of planShard.records) {
          if (record.semanticPreflightHash !== canonicalHash({
            captureOperationId: record.captureOperationId,
            captureSequence: record.captureSequence,
            sourceId: record.source.sourceId
          })) {
            fail('RUN_PLAN_INVALID', 'refresh plan semantic preflight hash mismatch')
          }
          const expected = {
            sourceId: record.source.sourceId,
            captureOperationId: record.captureOperationId,
            captureSequence: record.captureSequence,
            accountFingerprint: record.accountFingerprint,
            connectorRevision: record.connectorRevision,
            origins: record.origins || [record.source.origin]
          }
          if (expectedBySource.has(expected.sourceId) || sourceBucket(expected.sourceId) !== planShard.bucket) {
            fail('RUN_PLAN_INVALID', 'refresh plan contains a duplicate or misbucketed source')
          }
          expectedBySource.set(expected.sourceId, expected)
        }
      }
      const seenSources = new Set()
      for (const relative of plan.captureShardFiles) {
        const captureFile = containedFile(resolve(stageDir), relative, 'captureShardFiles')
        const shard = parseBoundedJson(captureFile, INPUT_BYTES_MAX,
          'TOKEN_SOURCE_CAPTURE_INVALID', 'token capture shard', resolve(stageDir))
        if (!validateCaptureShard(shard)) {
          const first = validateCaptureShard.errors[0]
          fail('TOKEN_SOURCE_CAPTURE_INVALID', `schema: ${(first.instancePath || '/') + ' ' + first.message}`)
        }
        if (seenBuckets.has(shard.bucket)) fail('TOKEN_SOURCE_CAPTURE_INVALID', `duplicate capture bucket ${shard.bucket}`)
        seenBuckets.add(shard.bucket)
        for (const record of shard.captures) {
          const expected = expectedBySource.get(record.capture.source.sourceId)
          if (!expected || seenSources.has(record.capture.source.sourceId)) {
            fail('TOKEN_SOURCE_SET_CHANGED', 'capture source set differs from immutable plan')
          }
          seenSources.add(record.capture.source.sourceId)
          const logicalBytes = Buffer.from(canonicalJson(record.capture), 'utf8')
          if (record.captureOperationId !== record.capture.captureOperationId ||
              record.captureSequence !== record.capture.captureSequence ||
              record.captureBytesHash !== bytesHash(logicalBytes) ||
              record.semanticPreflightHash !== canonicalHash({
                captureOperationId: record.captureOperationId,
                captureSequence: record.captureSequence,
                sourceId: record.capture.source.sourceId
              }) ||
              sourceBucket(record.capture.source.sourceId) !== shard.bucket) {
            fail('TOKEN_SOURCE_CAPTURE_INVALID', 'capture shard record identity/hash/bucket mismatch')
          }
          batches.push(normalizeSourceCapture(record.capture, logicalBytes, {
            captureOperationId: expected.captureOperationId,
            captureSequence: expected.captureSequence,
            accountFingerprint: expected.accountFingerprint,
            connectorRevision: expected.connectorRevision,
            sourceId: expected.sourceId,
            origins: expected.origins
          }))
        }
      }
      if (seenSources.size !== expectedBySource.size) {
        fail('TOKEN_SOURCE_CAPTURE_INCOMPLETE', 'one or more planned sources have no complete capture')
      }
      let previousCatalog = null
      if (plan.previousCatalogFile) {
        const previousFile = containedFile(resolve(stageDir), String(plan.previousCatalogFile), 'previousCatalogFile')
        previousCatalog = parseBoundedJson(previousFile, INPUT_BYTES_MAX,
          'TOKEN_GENERATION_RESYNC_REQUIRED', 'previous observed token catalog', resolve(stageDir))
        if (!validateCatalog(previousCatalog)) fail('TOKEN_GENERATION_RESYNC_REQUIRED', 'previous catalog schema-invalid')
      }
      let previousIndex = null
      if (plan.previousIndexFile) {
        previousIndex = parseBoundedJson(
          containedFile(resolve(stageDir), String(plan.previousIndexFile), 'previousIndexFile'),
          INPUT_BYTES_MAX, 'TOKEN_GENERATION_RESYNC_REQUIRED', 'previous observed token source index', resolve(stageDir))
        if (!validateIndex(previousIndex)) fail('TOKEN_GENERATION_RESYNC_REQUIRED', 'previous source index schema-invalid')
      }
      const retainedRetired = []
      if ((plan.existingSourceShardFiles || []).length) {
        if (!previousIndex) fail('TOKEN_GENERATION_RESYNC_REQUIRED', 'source shards require their exact source index')
        const previousBySource = new Map(previousIndex.sources.map((source) => [source.sourceId, source]))
        const priorBatches = new Map()
        const priorBuckets = new Set()
        for (const relative of plan.existingSourceShardFiles) {
          const shard = parseBoundedJson(
            containedFile(resolve(stageDir), relative, 'existingSourceShardFiles'),
            INPUT_BYTES_MAX, 'TOKEN_GENERATION_RESYNC_REQUIRED', 'previous observed token source shard', resolve(stageDir))
          if (!validateSourceShard(shard) || priorBuckets.has(shard.bucket)) {
            fail('TOKEN_GENERATION_RESYNC_REQUIRED', 'previous source shard schema/bucket invalid')
          }
          priorBuckets.add(shard.bucket)
          for (const batch of shard.sources) {
            const source = previousBySource.get(batch.sourceId)
            if (!source || priorBatches.has(batch.sourceId) ||
                source.acceptedBatch.batchSemanticHash !== batch.batchSemanticHash ||
                source.acceptedBatch.captureSequence !== batch.captureSequence ||
                source.acceptedBatch.observationCount !== batch.observations.length) {
              fail('TOKEN_GENERATION_RESYNC_REQUIRED', `previous source index/shard mismatch ${batch.sourceId}`)
            }
            priorBatches.set(batch.sourceId, batch)
            if (source.lifecycle === 'retired' && !seenSources.has(batch.sourceId)) {
              retainedRetired.push({
                sourceId: batch.sourceId,
                nodeId: batch.nodeId,
                kind: batch.kind,
                context: batch.context,
                origins: batch.origins,
                batch
              })
            }
          }
        }
        if (priorBatches.size !== previousBySource.size) {
          fail('TOKEN_GENERATION_RESYNC_REQUIRED', 'previous source index/shard source sets differ')
        }
      } else if (previousIndex && previousIndex.sources.length) {
        fail('TOKEN_GENERATION_RESYNC_REQUIRED', 'previous source index requires its source shards')
      }
      const result = aggregateObservedTokens({
        scope: plan.scope,
        batches,
        revision: plan.revision,
        previousCatalog,
        previousIndex,
        retiredSources: retainedRetired
      })
      const healthEvidence = batches
        .map((batch) => ({
          sourceId: batch.sourceId,
          captureOperationId: batch.captureOperationId,
          captureSequence: batch.captureSequence,
          captureEvidenceHash: batch.captureEvidenceHash
        }))
        .sort((left, right) => left.sourceId.localeCompare(right.sourceId))
      if (!!previousIndex !== !!previousCatalog) {
        fail('TOKEN_GENERATION_RESYNC_REQUIRED',
          'previous token source index and observed catalog must be supplied together')
      }
      const semanticSourceRows = (index) => index.sources.map((source) => ({
        sourceId: source.sourceId,
        ordinal: source.ordinal,
        lifecycle: source.lifecycle,
        nodeId: source.nodeId,
        kind: source.kind,
        context: source.context,
        origins: source.origins,
        acceptedBatch: {
          batchSemanticHash: source.acceptedBatch.batchSemanticHash,
          observationCount: source.acceptedBatch.observationCount,
          ...(source.acceptedBatch.previousObservationCount === undefined
            ? {}
            : { previousObservationCount: source.acceptedBatch.previousObservationCount })
        }
      }))
      const noOp = !!previousIndex &&
        canonicalJson(previousIndex.scope) === canonicalJson(result.index.scope) &&
        canonicalJson(semanticSourceRows(previousIndex)) === canonicalJson(semanticSourceRows(result.index))
      if (noOp) {
        process.stdout.write(JSON.stringify({
          ok: true,
          op: plan.op,
          noOp: true,
          artifacts: [],
          counts: previousCatalog.counts,
          sourceIndexHash: previousIndex.semanticHash,
          healthEvidence,
          totalBytes: 0
        }) + '\n')
        return
      }
      const outDir = containedFile(resolve(stageDir), String(plan.outDir || 'publication'), 'outDir')
      const artifacts = []
      let totalBytes = 0
      const emit = (name, role, value, validate) => {
        if (!validate(value)) {
          const first = validate.errors[0]
          fail('TOKEN_PUBLICATION_OUTPUT_INVALID', `${role}: ${(first.instancePath || '/') + ' ' + first.message}`)
        }
        const text = JSON.stringify(value, null, 2) + '\n'
        totalBytes += Buffer.byteLength(text)
        writeFresh(containedFile(outDir, name, 'publication artifact'), text)
        artifacts.push({ file: join(String(plan.outDir), name), role, bytes: Buffer.byteLength(text) })
      }
      for (const { role, shard } of result.shards) {
        emit(`source-shard-${String(shard.bucket).padStart(3, '0')}.json`, role, shard, validateSourceShard)
      }
      emit('source-index.json', 'observed-token-source-index', result.index, validateIndex)
      emit('observed-token-catalog.json', 'observed-token-catalog', result.catalog, validateCatalog)
      if (artifacts.length > limits.manifestArtifactsMax || totalBytes > limits.phaseBytesMax) {
        fail('TOKEN_PUBLICATION_BUDGET_EXCEEDED', 'token publication output exceeds artifact/byte budget')
      }
      process.stdout.write(JSON.stringify({
        ok: true,
        op: plan.op,
        artifacts,
        counts: result.catalog.counts,
        sourceIndexHash: result.index.semanticHash,
        catalogHash: result.catalog.semanticHash,
        healthEvidence,
        totalBytes
      }) + '\n')
      return
    }

    if (plan.op === 'token-compare') {
      const projectRoot = plan.projectRoot
      if (typeof projectRoot !== 'string' || !isAbsolute(projectRoot)) fail('RUN_PLAN_INVALID', 'projectRoot must be absolute')
      const { loadAdapterConfig } = await import(pathToFileURL(join(here, 'adapter-config.mjs')).href)
      const { extractProjectTokens } = await import(pathToFileURL(join(here, 'token-extraction.mjs')).href)
      const { bindObservedTokens, bindingSnapshotSemanticError } = await import(pathToFileURL(join(here, '..', 'tokens', 'binder.mjs')).href)
      const { compareTokens } = await import(pathToFileURL(join(here, '..', 'tokens', 'comparator.mjs')).href)
      const { publishableBaseline } = await import(pathToFileURL(join(here, '..', 'tokens', 'baseline.mjs')).href)
      const { mappingRegistrySemanticError, emptyMappingRegistry } = await import(pathToFileURL(join(here, '..', 'tokens', 'mapping-contract.mjs')).href)
      const { sourceIndexSemanticError, catalogSemanticError } = await import(pathToFileURL(join(here, '..', 'tokens', 'source-contract.mjs')).href)

      const validateCatalog = compile('observed-token-catalog.schema.json')
      const validateSourceIndex = compile('observed-token-source-index.schema.json')
      const validateMappings = compile('token-mappings.schema.json')
      const validateBindings = compile('token-binding-snapshot.schema.json')
      const validateComparison = compile('token-comparison.schema.json')
      const validateBaseline = compile('token-baseline.schema.json')
      const validateProjectInventory = compile('project-token-inventory.schema.json')
      const validateIndex = compile('project-token-analysis-index.schema.json')

      // Observed catalog and its exact source index are byte-verified by the
      // server before handoff and independently revalidated in the child.
      const catalogFile = containedFile(resolve(stageDir), String(plan.observedCatalogFile || ''), 'observedCatalogFile')
      const catalog = parseBoundedJson(catalogFile, INPUT_BYTES_MAX,
        TOKEN_ERROR_CODES.TOKEN_GENERATION_RESYNC_REQUIRED, 'active observed token catalog', resolve(stageDir))
      const sourceIndexFile = containedFile(resolve(stageDir), String(plan.sourceIndexFile || ''), 'sourceIndexFile')
      const sourceIndex = parseBoundedJson(sourceIndexFile, INPUT_BYTES_MAX,
        TOKEN_ERROR_CODES.TOKEN_GENERATION_RESYNC_REQUIRED, 'active observed token source index', resolve(stageDir))
      if (!validateCatalog(catalog) || !validateSourceIndex(sourceIndex)) {
        fail(TOKEN_ERROR_CODES.TOKEN_GENERATION_RESYNC_REQUIRED, 'active observed token domain does not satisfy the current contract')
      }
      const sourceIndexError = sourceIndexSemanticError(sourceIndex)
      if (sourceIndexError) fail(TOKEN_ERROR_CODES.TOKEN_GENERATION_RESYNC_REQUIRED, sourceIndexError)
      const catalogError = catalogSemanticError(catalog, sourceIndex)
      if (catalogError) fail(TOKEN_ERROR_CODES.TOKEN_GENERATION_RESYNC_REQUIRED, catalogError)
      if (catalog.scope.fileKeyFingerprint !== sourceIndex.scope.fileKeyFingerprint ||
          catalog.scope.branchKey !== sourceIndex.scope.branchKey) {
        fail(TOKEN_ERROR_CODES.TOKEN_GENERATION_RESYNC_REQUIRED, 'observed token catalog and source index scopes differ')
      }
      if (!['current', 'stale', 'unknown'].includes(plan.sourceFreshness)) {
        fail('RUN_PLAN_INVALID', 'sourceFreshness must be current, stale, or unknown')
      }

      // Adapter config: read from the repository (project-owned file).
      const configState = loadAdapterConfig({ projectRoot, schemaValidate: compile('project-adapters.schema.json') })
      if (configState.state === 'unconfigured') {
        fail('PROJECT_ADAPTERS_UNCONFIGURED', 'orchestrator/figma/project-adapters.json is absent')
      }

      // Mapping registry: project-owned; an absent file reads as the exact
      // revision-0 empty registry (predefined state), a malformed file blocks.
      const mappingAbsolute = resolve(projectRoot, 'orchestrator', 'figma', 'token-mappings.json')
      let registry
      let registryPresent = true
      try {
        registry = JSON.parse(readBounded(mappingAbsolute, INPUT_BYTES_MAX, projectRoot).toString('utf8'))
      } catch (error) {
        if (error && error.code === 'ENOENT') {
          registryPresent = false
          registry = emptyMappingRegistry(catalog.scope)
        } else {
          fail(TOKEN_ERROR_CODES.TOKEN_MAPPING_INVALID, `token-mappings.json unreadable: ${error.code || 'unsafe-file'}`)
        }
      }
      if (registryPresent) {
        if (!validateMappings(registry)) {
          const first = validateMappings.errors[0]
          fail(TOKEN_ERROR_CODES.TOKEN_MAPPING_INVALID, `schema: ${(first.instancePath || '/') + ' ' + first.message}`)
        }
        const registryError = mappingRegistrySemanticError(registry)
        if (registryError) fail(TOKEN_ERROR_CODES.TOKEN_MAPPING_INVALID, registryError)
      }
      if (registry.scope.fileKeyFingerprint !== catalog.scope.fileKeyFingerprint ||
          registry.scope.branchKey !== catalog.scope.branchKey) {
        fail(TOKEN_ERROR_CODES.TOKEN_MAPPING_SCOPE_CHANGED, 'token mapping registry scope differs from the observed token domain')
      }

      // Previous baseline (optional; exact bytes handed over by the server).
      let previousBaseline = null
      if (plan.previousBaselineFile) {
        const baselineFile = containedFile(resolve(stageDir), String(plan.previousBaselineFile), 'previousBaselineFile')
        previousBaseline = parseBoundedJson(baselineFile, INPUT_BYTES_MAX,
          TOKEN_ERROR_CODES.TOKEN_COMPARISON_BASELINE_INELIGIBLE, 'previous token baseline', resolve(stageDir))
        if (!validateBaseline(previousBaseline)) {
          fail(TOKEN_ERROR_CODES.TOKEN_COMPARISON_BASELINE_INELIGIBLE, 'previous baseline does not satisfy the current contract')
        }
      }

      const extraction = extractProjectTokens({
        projectRoot,
        config: configState.config,
        configHash: configState.tokenConfigHash,
        schemaValidators: { projectInventory: validateProjectInventory, analysisIndex: validateIndex }
      })

      const analysisIndexHash = canonicalHash(extraction.index)
      const bindingSnapshot = bindObservedTokens({
        catalog,
        projectInventories: extraction.inventories,
        adapterConfig: {
          ...configState.config,
          tokenConfigHash: configState.tokenConfigHash
        },
        mappingRegistry: registry,
        projectAnalysisHash: analysisIndexHash
      })
      if (!validateBindings(bindingSnapshot)) {
        const first = validateBindings.errors[0]
        fail('TOKEN_BINDING_OUTPUT_INVALID', `${(first.instancePath || '/') + ' ' + first.message}`)
      }
      const bindingError = bindingSnapshotSemanticError(bindingSnapshot)
      if (bindingError) fail('TOKEN_BINDING_OUTPUT_INVALID', bindingError)

      const { report, baselineCandidate } = compareTokens({
        observedCatalog: catalog,
        sourceIndex,
        projectInventories: extraction.inventories,
        analysisIndex: extraction.index,
        bindingSnapshot,
        mappingRegistry: registry,
        baseline: previousBaseline,
        context: {
          analysisIndexHash,
          adapterConfigHash: configState.tokenConfigHash,
          baselineHash: previousBaseline ? previousBaseline.semanticHash : 'none',
          sourceFreshness: plan.sourceFreshness
        }
      })
      if (!validateComparison(report)) {
        const first = validateComparison.errors[0]
        fail('TOKEN_COMPARATOR_OUTPUT_INVALID', `${(first.instancePath || '/') + ' ' + first.message}`)
      }
      const baseline = publishableBaseline(baselineCandidate, report, String(plan.eligibleAt || ''))
      if (baseline && !validateBaseline(baseline)) {
        fail('TOKEN_COMPARATOR_OUTPUT_INVALID', 'baseline candidate schema-invalid')
      }

      const artifacts = []
      const stage = resolve(stageDir)
      const emit = (name, value) => {
      writeFresh(containedFile(stage, name, 'artifact'), JSON.stringify(value, null, 2) + '\n')
        artifacts.push(name)
      }
      emit('analysis-index.json', extraction.index)
      for (const inventory of extraction.inventories) emit(`project-inventory-${inventory.adapterId}.json`, inventory)
      emit('binding-snapshot.json', bindingSnapshot)
      emit('mapping-snapshot.json', registry)
      emit('comparison.json', report)
      if (baseline) emit('baseline.json', baseline)

      process.stdout.write(JSON.stringify({
        ok: true,
        op: plan.op,
        artifacts,
        adapterIds: extraction.inventories.map((inventory) => inventory.adapterId),
        snapshotFingerprints: extraction.fingerprintsByAdapterId,
        configHash: configState.tokenConfigHash,
        configFileHash: configState.tokenConfigFileHash,
        mappingRevision: registry.revision,
        registryPresent,
        coverage: report.coverage,
        blockers: report.blockers || [],
        semanticHash: report.semanticHash,
        baselinePublished: !!baseline
      }) + '\n')
      return
    }

    if (plan.op === 'normalize-component-capture') {
      const { normalizeCapture } = await import(pathToFileURL(join(here, '..', 'components', 'capture-normalizer.mjs')).href)
      const validateCapture = compile('design-component-capture.schema.json')
      const validateInventory = compile('design-component-inventory.schema.json')
      const captureFile = containedFile(resolve(stageDir), String(plan.captureFile || 'capture.json'), 'captureFile')
      let captureBytes
      try { captureBytes = readBounded(captureFile, INPUT_BYTES_MAX, resolve(stageDir)) } catch (error) {
        fail(COMPONENT_ERROR_CODES.COMPONENT_DESIGN_CAPTURE_INVALID, `staged capture unreadable: ${error.code || error.message}`)
      }
      let capture
      try { capture = JSON.parse(captureBytes.toString('utf8')) } catch (error) {
        fail(COMPONENT_ERROR_CODES.COMPONENT_DESIGN_CAPTURE_INVALID, `staged capture is not JSON: ${error.message}`)
      }
      if (!validateCapture(capture)) {
        const first = validateCapture.errors[0]
        fail(COMPONENT_ERROR_CODES.COMPONENT_DESIGN_CAPTURE_INVALID, `schema: ${(first.instancePath || '/') + ' ' + first.message}`)
      }
      if (capture.visual.length > limits.componentVisualArtifactsMax) {
        fail(COMPONENT_ERROR_CODES.COMPONENT_DESIGN_CAPTURE_INVALID,
          `visual evidence exceeds ${limits.componentVisualArtifactsMax} artifacts`)
      }
      // Visual evidence bytes must exist in the stage and match their declared
      // hashes before the inventory may cite them.
      const visualArtifacts = []
      for (const entry of capture.visual) {
        const visualFile = containedFile(resolve(stageDir), entry.file, 'visual entry')
        let visualBytes
        try { visualBytes = readBounded(visualFile, VISUAL_BYTES_MAX, resolve(stageDir)) } catch (error) {
          fail(COMPONENT_ERROR_CODES.COMPONENT_DESIGN_CAPTURE_INVALID, `visual evidence ${entry.file} unreadable: ${error.code || error.message}`)
        }
        if (bytesHash(visualBytes) !== entry.sha256) {
          fail(COMPONENT_ERROR_CODES.COMPONENT_DESIGN_CAPTURE_INVALID, `visual evidence ${entry.file} bytes do not match the declared hash`)
        }
        visualArtifacts.push({ file: entry.file, sha256: entry.sha256 })
      }
      const inventory = normalizeCapture(capture, bytesHash(captureBytes))
      if (!validateInventory(inventory)) {
        const first = validateInventory.errors[0]
        fail(COMPONENT_ERROR_CODES.COMPONENT_DESIGN_CAPTURE_INVALID, `normalized inventory schema-invalid: ${(first.instancePath || '/') + ' ' + first.message}`)
      }
      const outFile = containedFile(resolve(stageDir), String(plan.outFile || 'design-component-inventory.json'), 'outFile')
      writeFresh(outFile, JSON.stringify(inventory, null, 2) + '\n')
      process.stdout.write(JSON.stringify({
        ok: true,
        op: plan.op,
        counts: inventory.counts,
        scopeId: inventory.scopeId,
        absenceProofEligible: inventory.capture.absenceProofEligible,
        visualArtifacts
      }) + '\n')
      return
    }

    if (plan.op === 'component-compare') {
      const projectRoot = plan.projectRoot
      if (typeof projectRoot !== 'string' || !isAbsolute(projectRoot)) fail('RUN_PLAN_INVALID', 'projectRoot must be absolute')
      const { loadAdapterConfig } = await import(pathToFileURL(join(here, 'adapter-config.mjs')).href)
      const { extractProjectComponents } = await import(pathToFileURL(join(here, 'component-extraction.mjs')).href)
      const { compareComponents } = await import(pathToFileURL(join(here, '..', 'components', 'comparator.mjs')).href)
      const { publishableBaseline } = await import(pathToFileURL(join(here, '..', 'components', 'baseline.mjs')).href)
      const { suggestComponentTasks } = await import(pathToFileURL(join(here, '..', 'components', 'task-suggestions.mjs')).href)
      const { mappingRegistrySemanticError, emptyMappingRegistry, COMPONENT_MAPPINGS_RELATIVE_PATH } =
        await import(pathToFileURL(join(here, '..', 'components', 'mapping-contract.mjs')).href)
      const { inventorySemanticError } = await import(pathToFileURL(join(here, '..', 'components', 'design-inventory-contract.mjs')).href)

      const validateDesign = compile('design-component-inventory.schema.json')
      const validateMappings = compile('component-mappings.schema.json')
      const validateComparison = compile('component-comparison.schema.json')
      const validateBaseline = compile('component-baseline.schema.json')
      const validateProjectInventory = compile('project-component-inventory.schema.json')
      const validateIndex = compile('project-component-analysis-index.schema.json')
      const validateSuggestions = compile('component-mapping-suggestions.schema.json')
      const validateTaskSuggestions = compile('component-task-suggestions.schema.json')

      // Design inventory: exact bytes handed over by the server from the
      // active generation (hash-verified there); re-validated here.
      const designFile = containedFile(resolve(stageDir), String(plan.designInventoryFile || ''), 'designInventoryFile')
      const design = parseBoundedJson(designFile, INPUT_BYTES_MAX,
        COMPONENT_ERROR_CODES.COMPONENT_GENERATION_RESYNC_REQUIRED, 'active component inventory', resolve(stageDir))
      if (!validateDesign(design)) {
        fail(COMPONENT_ERROR_CODES.COMPONENT_GENERATION_RESYNC_REQUIRED, 'active design component inventory does not satisfy the current contract')
      }
      const designError = inventorySemanticError(design)
      if (designError) fail(COMPONENT_ERROR_CODES.COMPONENT_GENERATION_RESYNC_REQUIRED, designError)

      const configState = loadAdapterConfig({ projectRoot, schemaValidate: compile('project-adapters.schema.json') })
      if (configState.state === 'unconfigured') {
        fail('PROJECT_ADAPTERS_UNCONFIGURED', 'orchestrator/figma/project-adapters.json is absent')
      }

      // Mapping registry: project-owned; an absent file reads as the exact
      // revision-0 empty registry (predefined state), a malformed file blocks.
      const mappingAbsolute = resolve(projectRoot, ...COMPONENT_MAPPINGS_RELATIVE_PATH.split('/'))
      let registry
      let registryPresent = true
      try {
        registry = JSON.parse(readBounded(mappingAbsolute, INPUT_BYTES_MAX, projectRoot).toString('utf8'))
      } catch (error) {
        if (error && error.code === 'ENOENT') {
          registryPresent = false
          registry = emptyMappingRegistry(design.scopeId)
        } else {
          fail(COMPONENT_ERROR_CODES.COMPONENT_MAPPING_INVALID, `component-mappings.json unreadable: ${error.code || 'unsafe-file'}`)
        }
      }
      if (registryPresent) {
        if (!validateMappings(registry)) {
          const first = validateMappings.errors[0]
          fail(COMPONENT_ERROR_CODES.COMPONENT_MAPPING_INVALID, `schema: ${(first.instancePath || '/') + ' ' + first.message}`)
        }
        const registryError = mappingRegistrySemanticError(registry)
        if (registryError) fail(COMPONENT_ERROR_CODES.COMPONENT_MAPPING_INVALID, registryError)
      }

      // Previous baseline (optional; exact bytes handed over by the server).
      let previousBaseline = null
      if (plan.previousBaselineFile) {
        const baselineFile = containedFile(resolve(stageDir), String(plan.previousBaselineFile), 'previousBaselineFile')
        previousBaseline = parseBoundedJson(baselineFile, INPUT_BYTES_MAX,
          COMPONENT_ERROR_CODES.COMPONENT_COMPARISON_BASELINE_INELIGIBLE, 'previous component baseline', resolve(stageDir))
        if (!validateBaseline(previousBaseline)) {
          fail(COMPONENT_ERROR_CODES.COMPONENT_COMPARISON_BASELINE_INELIGIBLE, 'previous baseline does not satisfy the current contract')
        }
      }

      // Token snapshot (optional; exact bytes from the active generation).
      // The comparator uses it only for the §17.12 causality proof and the
      // token-binding coverage axis.
      let tokenReport = null
      let tokenBindings = null
      if (!!plan.tokenComparisonFile !== !!plan.tokenBindingSnapshotFile) {
        fail(COMPONENT_ERROR_CODES.COMPONENT_GENERATION_RESYNC_REQUIRED,
          'token comparison and effective binding snapshots must be supplied as one exact pair')
      }
      const validateTokenComparison = compile('token-comparison.schema.json')
      const validateTokenBindings = compile('token-binding-snapshot.schema.json')
      if (plan.tokenComparisonFile) {
        const file = containedFile(resolve(stageDir), String(plan.tokenComparisonFile), 'tokenComparisonFile')
        tokenReport = parseBoundedJson(file, INPUT_BYTES_MAX,
          COMPONENT_ERROR_CODES.COMPONENT_GENERATION_RESYNC_REQUIRED, 'token comparison snapshot', resolve(stageDir))
        if (!validateTokenComparison(tokenReport)) {
          fail(COMPONENT_ERROR_CODES.COMPONENT_GENERATION_RESYNC_REQUIRED,
            'token comparison snapshot does not satisfy the current contract')
        }
      }
      if (plan.tokenBindingSnapshotFile) {
        const file = containedFile(resolve(stageDir), String(plan.tokenBindingSnapshotFile), 'tokenBindingSnapshotFile')
        tokenBindings = parseBoundedJson(file, INPUT_BYTES_MAX,
          COMPONENT_ERROR_CODES.COMPONENT_GENERATION_RESYNC_REQUIRED, 'effective token binding snapshot', resolve(stageDir))
        if (!validateTokenBindings(tokenBindings)) {
          fail(COMPONENT_ERROR_CODES.COMPONENT_GENERATION_RESYNC_REQUIRED,
            'effective token binding snapshot does not satisfy the current contract')
        }
      }
      if (tokenReport && tokenReport.inputs.bindingSnapshotHash !== tokenBindings.semanticHash) {
        fail(COMPONENT_ERROR_CODES.COMPONENT_GENERATION_RESYNC_REQUIRED,
          'token comparison and effective binding snapshots do not share one exact semantic hash')
      }

      const extraction = extractProjectComponents({
        projectRoot,
        config: configState.config,
        configHash: configState.componentConfigHash,
        schemaValidators: { projectInventory: validateProjectInventory, analysisIndex: validateIndex }
      })

      const { report, suggestions, baselineCandidate } = compareComponents({
        designInventory: design,
        projectInventories: extraction.inventories,
        analysisIndex: extraction.index,
        mappingRegistry: registry,
        baseline: previousBaseline,
        tokenSnapshot: { report: tokenReport, bindingSnapshot: tokenBindings },
        context: {
          designGenerationId: String(plan.designGenerationId || ''),
          adapterConfigHash: configState.componentConfigHash,
          adapterConfigFileHash: configState.componentConfigFileHash
        }
      })
      if (!validateComparison(report)) {
        const first = validateComparison.errors[0]
        fail('COMPONENT_COMPARATOR_OUTPUT_INVALID', `${(first.instancePath || '/') + ' ' + first.message}`)
      }
      if (!validateSuggestions(suggestions)) {
        fail('COMPONENT_COMPARATOR_OUTPUT_INVALID', 'suggestions artifact schema-invalid')
      }
      const taskSuggestions = suggestComponentTasks(report)
      if (!validateTaskSuggestions(taskSuggestions)) {
        fail('COMPONENT_COMPARATOR_OUTPUT_INVALID', 'task suggestions artifact schema-invalid')
      }
      const baseline = publishableBaseline(baselineCandidate, previousBaseline, String(plan.eligibleAt || ''))
      if (baseline && !validateBaseline(baseline)) {
        fail('COMPONENT_COMPARATOR_OUTPUT_INVALID', 'baseline candidate schema-invalid')
      }

      const artifacts = []
      const stage = resolve(stageDir)
      const emit = (name, value) => {
        writeFresh(containedFile(stage, name, 'artifact'), JSON.stringify(value, null, 2) + '\n')
        artifacts.push(name)
      }
      emit('analysis-index.json', extraction.index)
      for (const inventory of extraction.inventories) emit(`project-inventory-${inventory.adapterId}.json`, inventory)
      emit('mapping-snapshot.json', registry)
      emit('comparison.json', report)
      emit('suggestions.json', suggestions)
      emit('task-suggestions.json', taskSuggestions)
      if (baseline) emit('baseline.json', baseline)

      process.stdout.write(JSON.stringify({
        ok: true,
        op: plan.op,
        artifacts,
        adapterIds: extraction.inventories.map((inventory) => inventory.adapterId),
        snapshotFingerprints: extraction.fingerprintsByAdapterId,
        configHash: configState.componentConfigHash,
        configFileHash: configState.componentConfigFileHash,
        mappingRevision: registry.revision,
        registryPresent,
        coverage: report.coverage,
        blockers: report.blockers || [],
        semanticHash: report.semanticHash,
        baselinePublished: !!baseline
      }) + '\n')
      return
    }

    fail('RUN_PLAN_INVALID', `unknown op ${JSON.stringify(plan.op)}`)
  } catch (error) {
    if (isTypedError(error)) fail(error.code, error.detail || error.message)
    fail('RUN_PLAN_FAILED', error && error.code || 'runtime failure')
  }
}

main().catch((error) => {
  fail('RUN_PLAN_FAILED', error && error.code || 'runtime failure')
})
