import assert from 'node:assert/strict'
import {
  cpSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  readdirSync,
  realpathSync,
  renameSync,
  rmSync,
  writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'

const HERE = dirname(new URL(import.meta.url).pathname)
const ROOT = resolve(HERE, '..', '..', '..')
// macOS exposes /var through /private/var. Use the canonical path so the
// production authority-root checks exercise the fixture without a false
// symlink escape.
const FIXTURE = realpathSync(mkdtempSync(join(tmpdir(), 'architecture-')))
const require = createRequire(import.meta.url)

function write(relative, text) {
  const file = join(FIXTURE, relative)
  mkdirSync(dirname(file), { recursive: true })
  writeFileSync(file, text)
}

function generate(...args) {
  const result = spawnSync('python3', [
    join(ROOT, 'orchestrator', 'tasks', 'regen-arch.py'),
    ...args,
  ], { cwd: FIXTURE, encoding: 'utf8' })
  assert.equal(result.status, 0, result.stderr)
  return result
}

try {
  write('settings.gradle.kts', [
    'include(":shared")',
    'include(":ui-screen-features:screen-api")',
    'include(":ui-screen-features:home")',
    'include(":data-features:feature-api")',
    'include(":data-features:note")',
    '',
  ].join('\n'))
  write('orchestrator/project-config.md', [
    '---',
    'apiClassName: "DemoApi"',
    'featuresWithRootComponentSuffix: []',
    '---',
    '',
  ].join('\n'))
  for (const module of [
    'shared',
    'ui-screen-features/screen-api',
    'ui-screen-features/home',
    'data-features/feature-api',
    'data-features/note',
  ]) {
    write(`${module}/build.gradle.kts`, 'plugins { kotlin("multiplatform") }\n')
  }
  write('ui-screen-features/screen-api/src/commonMain/kotlin/demo/HomeRouter.kt', [
    'package demo',
    'sealed interface HomeRouter {',
    '  @Serializable data object Feed : HomeRouter',
    '}',
    '',
  ].join('\n'))
  write('ui-screen-features/home/src/commonMain/kotlin/demo/HomeComponent.kt',
    'package demo\nclass HomeComponent\nclass SecondaryComponent\n')
  write('data-features/feature-api/src/commonMain/kotlin/demo/Features.kt',
    'package demo\ninterface NoteFeature\n')
  write('data-features/note/src/commonMain/kotlin/demo/NoteRepository.kt',
    'package demo\nclass NoteRepository\n')
  mkdirSync(join(FIXTURE, 'orchestrator', 'tasks', 'backlog'), { recursive: true })
  mkdirSync(join(FIXTURE, 'orchestrator', 'tasks', 'pending'), { recursive: true })
  mkdirSync(join(FIXTURE, 'orchestrator', 'tasks', 'todo'), { recursive: true })
  mkdirSync(join(FIXTURE, 'orchestrator', 'tasks', 'done'), { recursive: true })

  // The production job resolves its executable inside the mutable project.
  cpSync(join(ROOT, 'orchestrator', 'tasks', 'regen-arch.py'),
    join(FIXTURE, 'orchestrator', 'tasks', 'regen-arch.py'))
  cpSync(join(ROOT, 'orchestrator', 'tasks', 'architecture_analysis.py'),
    join(FIXTURE, 'orchestrator', 'tasks', 'architecture_analysis.py'))
  mkdirSync(join(FIXTURE, 'orchestrator', 'contracts'), { recursive: true })
  cpSync(join(ROOT, 'orchestrator', 'contracts', 'architecture-map.schema.json'),
    join(FIXTURE, 'orchestrator', 'contracts', 'architecture-map.schema.json'))
  cpSync(join(ROOT, 'orchestrator', 'contracts', 'architecture-rules.schema.json'),
    join(FIXTURE, 'orchestrator', 'contracts', 'architecture-rules.schema.json'))

  generate()
  const regenIndex = spawnSync('python3', [
    join(ROOT, 'orchestrator', 'tasks', 'regen-index.py'),
  ], { cwd: FIXTURE, encoding: 'utf8' })
  assert.equal(regenIndex.status, 0, regenIndex.stderr)

  process.env.ORCHESTRATOR_PROJECT_ROOT = FIXTURE
  process.env.ORCHESTRATOR_CACHE_DIR = join(FIXTURE, 'orchestrator', '.cache')
  process.env.ORCHESTRATOR_TASKS_DIR = join(FIXTURE, 'orchestrator', 'tasks')
  process.env.ORCHESTRATOR_FINALIZATIONS_DIR =
    join(FIXTURE, 'orchestrator', '.cache', 'tasks', 'finalizations')

  const contract = require('../server/architecture-contract.js')
  const Ajv2020 = require('ajv/dist/2020.js').default
  const arch = require('../server/arch.js')
  const taskSource = require('../server/task-source.js')
  const taskActions = require('../server/architecture-task-actions.js')
  const generation = require('../server/architecture-generation.js')
  const designRelations = require('../server/design-relations.js')

  const bytes = readFileSync(join(FIXTURE, 'orchestrator', '.arch-map.json'))
  const parsed = contract.parse(bytes)
  const ajv = new Ajv2020({ allErrors: true, strict: true })
  const mapSchema = JSON.parse(readFileSync(
    join(ROOT, 'orchestrator', 'contracts', 'architecture-map.schema.json'),
    'utf8',
  ))
  const rulesSchema = JSON.parse(readFileSync(
    join(ROOT, 'orchestrator', 'contracts', 'architecture-rules.schema.json'),
    'utf8',
  ))
  const validateMapSchema = ajv.compile(mapSchema)
  assert.equal(validateMapSchema(JSON.parse(bytes.toString('utf8'))), true,
    JSON.stringify(validateMapSchema.errors))
  const validateRulesSchema = ajv.compile(rulesSchema)
  assert.equal(validateRulesSchema(JSON.parse(readFileSync(
    join(ROOT, 'orchestrator', 'architecture-rules.json'),
    'utf8',
  ))), true, JSON.stringify(validateRulesSchema.errors))
  assert.equal(parsed.map.structuralHash, contract.structuralHash(parsed.map))
  assert.equal(parsed.map.schemaVersion, 2)
  assert.throws(() => contract.parse(Buffer.from([0xff, 0xfe, 0xfd])),
    /UTF-8|JSON/i)
  assert.throws(() => contract.parse(Buffer.alloc(contract.MAX_MAP_BYTES + 1, 0x20)),
    /too-large/i)

  const tampered = structuredClone(parsed.map)
  tampered.nodes[0].name = 'Tampered'
  assert.throws(() => contract.validateV2(tampered), /structural hash/i)

  const impossibleTimestamp = structuredClone(parsed.map)
  impossibleTimestamp.generatedAt = '2026-02-30T25:61:61Z'
  assert.throws(() => contract.validateV2(impossibleTimestamp), /envelope/i)

  const zeroYearTimestamp = structuredClone(parsed.map)
  zeroYearTimestamp.generatedAt = '0000-01-01T00:00:00Z'
  assert.equal(validateMapSchema(zeroYearTimestamp), false)
  assert.throws(() => contract.validateV2(zeroYearTimestamp), /envelope/i)

  const nonNormalizedPath = structuredClone(parsed.map)
  nonNormalizedPath.nodes[0].path = '.'
  nonNormalizedPath.structuralHash = contract.structuralHash(nonNormalizedPath)
  assert.equal(validateMapSchema(nonNormalizedPath), false)
  assert.throws(() => contract.validateV2(nonNormalizedPath), /node/i)

  const dottedCapability = structuredClone(parsed.map)
  dottedCapability.analysis.capabilities =
    [...dottedCapability.analysis.capabilities, 'scanner.experimental'].sort()
  dottedCapability.structuralHash = contract.structuralHash(dottedCapability)
  assert.equal(validateMapSchema(dottedCapability), true,
    JSON.stringify(validateMapSchema.errors))
  assert.doesNotThrow(() => contract.validateV2(dottedCapability))

  const inconsistentAnalysis = structuredClone(parsed.map)
  inconsistentAnalysis.analysis.status = 'partial'
  assert.throws(() => contract.validateV2(inconsistentAnalysis), /analysis/i)

  const falseUnknownDatabase = structuredClone(parsed.map)
  falseUnknownDatabase.summary.databaseEntities = null
  assert.throws(() => contract.validateV2(falseUnknownDatabase), /summary/i)

  const dangling = structuredClone(parsed.map)
  dangling.edges.push({
    id: 'edge:depends-on/ffffffffffffffffffffffffffffffff',
    from: dangling.nodes[0].id,
    to: 'module:missing',
    kind: 'depends-on',
    evidence: {
      sourcePath: 'settings.gradle.kts',
      line: 1,
      analyzer: 'test-v1',
      confidence: 'exact',
    },
  })
  dangling.edges.sort((a, b) => a.id.localeCompare(b.id))
  dangling.structuralHash = contract.structuralHash(dangling)
  assert.throws(() => contract.validateV2(dangling), /edge/i)

  const invalidPair = structuredClone(parsed.map)
  invalidPair.edges[0].kind = 'owns'
  invalidPair.structuralHash = contract.structuralHash(invalidPair)
  assert.throws(() => contract.validateV2(invalidPair), /edge/i)

  const duplicate = structuredClone(parsed.map)
  duplicate.nodes.push(structuredClone(duplicate.nodes[0]))
  duplicate.nodes.sort((a, b) => a.id.localeCompare(b.id))
  duplicate.structuralHash = contract.structuralHash(duplicate)
  assert.throws(() => contract.validateV2(duplicate), /sorted with unique ids/i)

  const wrongMetadata = structuredClone(parsed.map)
  wrongMetadata.nodes[0].metadata.unexpected = true
  wrongMetadata.structuralHash = contract.structuralHash(wrongMetadata)
  assert.throws(() => contract.validateV2(wrongMetadata), /node/i)

  const badFingerprint = structuredClone(parsed.map)
  badFingerprint.findings[0].fingerprint = `sha256:${'0'.repeat(64)}`
  badFingerprint.structuralHash = contract.structuralHash(badFingerprint)
  assert.throws(() => contract.validateV2(badFingerprint), /fingerprint/i)

  const unknownEvidenceReason = structuredClone(parsed.map)
  unknownEvidenceReason.findings[0].evidence[0].reasonCode = 'future-legacy-reason'
  unknownEvidenceReason.structuralHash = contract.structuralHash(unknownEvidenceReason)
  assert.equal(validateMapSchema(unknownEvidenceReason), false)
  assert.throws(() => contract.validateV2(unknownEvidenceReason), /evidence/i)

  const unsupportedMap = {
    version: 1,
    generatedAt: '2026-01-01T00:00:00Z',
    moduleCount: 1,
    summary: {},
    modules: { ':shared': { dir: 'shared' } },
    screens: [],
    dialogs: { subtypes: [], modules: [] },
    dataFeatures: [],
    api: { class: null, file: null, sections: [], methods: [] },
    database: { version: null, file: null, entities: [] },
  }
  assert.throws(() => contract.parse(Buffer.from(JSON.stringify(unsupportedMap))),
    /version/i)

  const overview = arch.overview()
  assert.equal(overview.present, true)
  assert.equal(overview.summary.modules, 5)
  assert.equal(overview.freshness.currentRevision, overview.generatedAtRevision)
  assert.equal(overview.unownedScreensTotal, overview.unownedScreens.length)
  assert.equal(overview.unownedScreensTruncated, false)
  const mapFile = join(FIXTURE, 'orchestrator', '.arch-map.json')
  const savedMapFile = join(FIXTURE, 'orchestrator', '.arch-map.saved.json')
  renameSync(mapFile, savedMapFile)
  arch.invalidate()
  const missingOverview = arch.overview()
  assert.equal(missingOverview.present, false)
  assert.equal(missingOverview.canGenerate, true)
  assert.match(missingOverview.freshness.currentRevision, /^sha256:[a-f0-9]{64}$/)
  renameSync(savedMapFile, mapFile)
  arch.invalidate()

  const partialMap = structuredClone(parsed.map)
  partialMap.analysis.status = 'partial'
  partialMap.analysis.limitations = ['analysis-coverage-partial']
  writeFileSync(mapFile, JSON.stringify(partialMap, null, 2) + '\n')
  arch.invalidate()
  const partialOverview = arch.overview()
  assert.equal(partialOverview.analysis.status, 'partial')
  assert.equal(partialOverview.freshness.status, 'fresh')
  writeFileSync(mapFile, bytes)
  arch.invalidate()
  const unicodeMap = structuredClone(parsed.map)
  unicodeMap.nodes[0].name = 'Cafe\u0301'
  unicodeMap.structuralHash = contract.structuralHash(unicodeMap)
  writeFileSync(mapFile, JSON.stringify(unicodeMap, null, 2) + '\n')
  arch.invalidate()
  assert.equal(arch.nodes({ search: 'Café' }).total, 1)
  writeFileSync(mapFile, bytes)
  arch.invalidate()
  const sourceFile = join(FIXTURE,
    'ui-screen-features', 'home', 'src', 'commonMain', 'kotlin', 'demo', 'HomeComponent.kt')
  const sourceBeforeStale = readFileSync(sourceFile, 'utf8')
  writeFileSync(sourceFile, sourceBeforeStale + '// architecture revision drift\n')
  arch.invalidate()
  const staleOverview = arch.overview()
  assert.equal(staleOverview.freshness.status, 'stale')
  assert.equal(staleOverview.freshness.reason, 'source-revision-drift')
  writeFileSync(sourceFile, sourceBeforeStale)
  arch.invalidate()
  assert.equal(arch.overview().freshness.status,
    parsed.map.analysis.status === 'partial' ? 'stale' : 'fresh')

  const modules = arch.nodes({ kind: 'module', limit: 1 })
  assert.equal(modules.rows.length, 1)
  assert.ok(modules.nextCursor)
  const nextModules = arch.nodes({ kind: 'module', limit: 1, cursor: modules.nextCursor })
  assert.equal(nextModules.rows.length, 1)
  assert.notEqual(nextModules.rows[0].id, modules.rows[0].id)
  assert.throws(() => arch.nodes({ kind: 'module', limit: 1, cursor: `${modules.nextCursor}x` }),
    /cursor invalid/i)

  const screens = arch.nodes({ kind: 'screen', search: 'Feed' })
  assert.equal(screens.total, 1)
  const designLink = designRelations.surfaceRelation({
    name: screens.rows[0].name,
    feature: null,
    route: null,
  }, designRelations.snapshot())
  assert.equal(designLink.available, true)
  assert.ok(designLink.feature)
  assert.ok(designLink.module)
  assert.ok(designLink.codeSources.length > 0)
  const componentSurfaceLink = designRelations.surfaceRelation({
    name: 'Secondary dialog',
    feature: null,
    route: null,
  }, designRelations.snapshot())
  assert.equal(componentSurfaceLink.available, true)
  assert.equal(componentSurfaceLink.feature, designLink.feature)
  assert.equal(componentSurfaceLink.module, designLink.module)
  assert.ok(componentSurfaceLink.codeSources.some(path => path.endsWith('HomeComponent.kt')))
  const detail = arch.nodeDetail(screens.rows[0].id, { limit: 100 })
  assert.equal(detail.present, true)
  assert.ok(detail.incoming.rows.some(row => row.edge.kind === 'owns'))
  assert.ok(detail.outgoing.rows.some(row => row.edge.kind === 'renders'))
  assert.equal(detail.findingsTruncated, false)
  assert.equal(detail.findingsTotal, detail.findings.length)
  assert.equal(detail.linkedTasksTotal, detail.linkedTasks.length)
  assert.equal(detail.linkedTasksTruncated, false)
  const firstOutgoing = arch.nodeDetail(screens.rows[0].id, { limit: 1 })
  assert.equal(firstOutgoing.outgoing.rows.length, 1)
  assert.ok(firstOutgoing.outgoing.nextCursor)
  const secondOutgoing = arch.nodeDetail(screens.rows[0].id, {
    limit: 1,
    outgoingCursor: firstOutgoing.outgoing.nextCursor,
  })
  assert.equal(secondOutgoing.outgoing.rows.length, 1)
  assert.notEqual(secondOutgoing.outgoing.rows[0].edge.id,
    firstOutgoing.outgoing.rows[0].edge.id)

  const graph = arch.graph({ kind: 'screen' })
  assert.equal(graph.tooLarge, false)
  assert.equal(graph.nodeCount, 1)
  const moduleGraph = arch.graph({ kind: 'module' })
  assert.ok(moduleGraph.nodes.some(node => node.findingSeverity))

  const findings = arch.findings({ severity: 'warning' })
  assert.ok(findings.total > 0)
  assert.equal(findings.analysisStatus, parsed.map.analysis.status)
  const typedFindings = arch.findings({
    type: findings.rows[0].type,
    confidence: findings.rows[0].confidence,
  })
  assert.ok(typedFindings.rows.length > 0)
  assert.ok(typedFindings.rows.every(row => row.type === findings.rows[0].type))
  assert.ok(typedFindings.rows.every(row => row.confidence === findings.rows[0].confidence))

  const latestDiff = arch.readDiff('latest')
  assert.equal(latestDiff.present, true)
  assert.equal(latestDiff.diff.schemaVersion, 2)
  assert.equal(latestDiff.diff.baselineCreated, true)
  assert.equal(latestDiff.diff.truncated, false)
  assert.ok(Object.values(latestDiff.diff.changeTotals).every(total => total === 0))
  assert.equal(latestDiff.diff.currentHash, parsed.map.structuralHash)
  const validRawDiff = structuredClone(latestDiff.diff)
  delete validRawDiff.followedByChanges
  assert.equal(arch._test.validDiff(validRawDiff), true)
  const invalidDiff = structuredClone(validRawDiff)
  invalidDiff.changes.nodesAdded = ['module:z', 'module:a']
  assert.equal(arch._test.validDiff(invalidDiff), false)
  const invalidTotals = structuredClone(validRawDiff)
  invalidTotals.changeTotals.nodesAdded = 1
  assert.equal(arch._test.validDiff(invalidTotals), false)
  assert.throws(() => arch.nodes({ changed: true }), error =>
    error && error.code === 'architecture-diff-unavailable')
  assert.throws(() => arch.findings({ changed: true }), error =>
    error && error.code === 'architecture-diff-unavailable')
  const taskDiffFile = join(FIXTURE, 'orchestrator', '.cache', 'architecture',
    'latest-task-diff.json')
  writeFileSync(taskDiffFile, '{}\n')
  assert.equal(arch.readDiff('task').error, 'architecture-diff-invalid')
  assert.throws(() => arch.nodes({ changed: true }), error =>
    error && error.code === 'architecture-diff-invalid')
  rmSync(taskDiffFile)

  const taskState = taskSource.scanOpen()
  assert.equal(taskState.ok, true)
  assert.equal(taskState.historyOk, true)
  const finding = parsed.map.findings[0]
  const preview = taskActions.preview({
    findingId: finding.id,
    fingerprint: finding.fingerprint,
    expectedStructuralHash: parsed.map.structuralHash,
    expectedTaskIndexRevision: taskState.revision,
  })
  assert.equal(preview.ok, true)
  assert.equal(preview.preview.action, 'create')

  const created = await taskActions.create({
    previewId: preview.preview.id,
    expectedStructuralHash: preview.structuralHash,
    expectedTaskIndexRevision: preview.taskIndexRevision,
  })
  assert.equal(created.ok, true, JSON.stringify(created))
  assert.equal(created.result, 'created')
  const afterCreate = taskSource.scanOpen()
  const linked = afterCreate.allByRef[finding.id]
  assert.equal(linked.length, 1)
  assert.equal(linked[0].source.kind, 'manual')
  assert.equal(linked[0].source.type, 'architecture-finding')
  assert.equal(linked[0].source.fingerprint, finding.fingerprint)
  const duplicatePreview = taskActions.preview({
    findingId: finding.id,
    fingerprint: finding.fingerprint,
    expectedStructuralHash: parsed.map.structuralHash,
    expectedTaskIndexRevision: afterCreate.revision,
  })
  assert.equal(duplicatePreview.ok, true)
  assert.equal(duplicatePreview.preview.action, 'existing')
  assert.equal(duplicatePreview.preview.existingTask.stem, linked[0].stem)
  await new Promise(resolvePromise => setTimeout(resolvePromise, 5))
  const replayedDuplicatePreview = taskActions.preview({
    findingId: finding.id,
    fingerprint: finding.fingerprint,
    expectedStructuralHash: parsed.map.structuralHash,
    expectedTaskIndexRevision: afterCreate.revision,
  })
  assert.equal(replayedDuplicatePreview.preview.id, duplicatePreview.preview.id)
  assert.ok(Date.parse(replayedDuplicatePreview.preview.expiresAt) >
    Date.parse(duplicatePreview.preview.expiresAt))
  const duplicateCreate = await taskActions.create({
    previewId: duplicatePreview.preview.id,
    expectedStructuralHash: duplicatePreview.structuralHash,
    expectedTaskIndexRevision: duplicatePreview.taskIndexRevision,
  })
  assert.equal(duplicateCreate.ok, true)
  assert.equal(duplicateCreate.result, 'existing')
  assert.equal(taskSource.scanOpen().allByRef[finding.id].length, 1)
  const linkedDetail = arch.nodeDetail(finding.affectedNodeIds[0], { limit: 100 })
  assert.equal(linkedDetail.linkedTasksTotal, 1)
  assert.equal(linkedDetail.linkedTasksTruncated, false)

  const pagedTaskDiff = structuredClone(validRawDiff)
  pagedTaskDiff.baselineCreated = false
  pagedTaskDiff.previousHash = pagedTaskDiff.currentHash
  pagedTaskDiff.previousRevision = pagedTaskDiff.currentRevision
  pagedTaskDiff.trigger = 'task-finalization'
  pagedTaskDiff.triggerId = 'architecture-page'
  pagedTaskDiff.taskStem = 'TASK_1_architecture'
  pagedTaskDiff.changes.nodesAdded = parsed.map.nodes
    .filter(node => node.kind === 'module').slice(0, 2).map(node => node.id).sort()
  pagedTaskDiff.changeTotals.nodesAdded = pagedTaskDiff.changes.nodesAdded.length
  assert.equal(arch._test.validDiff(pagedTaskDiff), true)
  writeFileSync(taskDiffFile, JSON.stringify(pagedTaskDiff, null, 2) + '\n')
  const changedPage = arch.nodes({ changed: true, limit: 1 })
  assert.equal(changedPage.rows.length, 1)
  assert.ok(changedPage.nextCursor)
  pagedTaskDiff.id = 'diff:cursor-revision'
  writeFileSync(taskDiffFile, JSON.stringify(pagedTaskDiff, null, 2) + '\n')
  assert.throws(() => arch.nodes({ changed: true, limit: 1,
    cursor: changedPage.nextCursor }), error =>
    error && error.code === 'architecture-cursor-stale')

  const truncatedTaskDiff = structuredClone(validRawDiff)
  truncatedTaskDiff.baselineCreated = false
  truncatedTaskDiff.previousHash = truncatedTaskDiff.currentHash
  truncatedTaskDiff.previousRevision = truncatedTaskDiff.currentRevision
  truncatedTaskDiff.trigger = 'task-finalization'
  truncatedTaskDiff.triggerId = 'architecture-test'
  truncatedTaskDiff.taskStem = 'TASK_1_architecture'
  truncatedTaskDiff.changeTotals.nodesAdded = 1
  truncatedTaskDiff.truncated = true
  assert.equal(arch._test.validDiff(truncatedTaskDiff), true)
  writeFileSync(taskDiffFile, JSON.stringify(truncatedTaskDiff, null, 2) + '\n')
  assert.throws(() => arch.nodes({ changed: true }), error =>
    error && error.code === 'architecture-diff-truncated')
  assert.throws(() => arch.findings({ changed: true }), error =>
    error && error.code === 'architecture-diff-truncated')

  const stalePreview = taskActions.preview({
    findingId: finding.id,
    fingerprint: `sha256:${'0'.repeat(64)}`,
    expectedStructuralHash: parsed.map.structuralHash,
    expectedTaskIndexRevision: afterCreate.revision,
  })
  assert.equal(stalePreview.ok, false)
  assert.equal(stalePreview.error, 'architecture-finding-stale')

  const generationStart = generation.start({
    expectedSourceRevision: overview.generatedAtRevision,
    reason: 'manual',
  })
  assert.equal(generationStart.ok, true, JSON.stringify(generationStart))
  assert.equal(generationStart.status, 202)
  const generationReplay = generation.start({
    expectedSourceRevision: overview.generatedAtRevision,
    reason: 'manual',
  })
  assert.equal(generationReplay.ok, true)
  assert.equal(generationReplay.replayed, true)
  assert.equal(generationReplay.job.id, generationStart.job.id)
  const generationConflict = generation.start({
    expectedSourceRevision: null,
    reason: 'manual',
  })
  assert.equal(generationConflict.ok, false)
  assert.equal(generationConflict.error, 'architecture-source-conflict')
  let generatedJob = generationStart.job
  const deadline = Date.now() + 15_000
  while (generatedJob && ['queued', 'running'].includes(generatedJob.state) &&
      Date.now() < deadline) {
    await new Promise(resolvePromise => setTimeout(resolvePromise, 50))
    generatedJob = generation.get(generatedJob.id)
  }
  assert.equal(generatedJob.state, 'succeeded', JSON.stringify(generatedJob))
  assert.match(generatedJob.structuralHash, /^sha256:[a-f0-9]{64}$/)
  assert.equal(arch.readValidated().map.structuralHash, generatedJob.structuralHash)
  const futureInstant = '2999-01-01T00:00:00.000Z'
  assert.equal(generation._test.terminalInstant(futureInstant), futureInstant)
  const jobDirectory = join(FIXTURE, 'orchestrator', '.cache', 'architecture', 'jobs')
  for (let index = 0; index < 105; index++) {
    const reportId = `archjob-${index.toString(16).padStart(32, '0')}`
    const startedAt = new Date(Date.parse('2026-01-01T00:00:00.000Z') + index).toISOString()
    writeFileSync(join(jobDirectory, `${reportId}.json`), JSON.stringify({
      schemaVersion: 1,
      id: reportId,
      state: 'failed',
      phase: 'failed',
      reason: 'manual',
      expectedSourceRevision: parsed.map.generatedAtRevision,
      startedAt,
      finishedAt: startedAt,
      structuralHash: null,
      generatedAtRevision: null,
      error: { code: 'architecture-generator-failed' },
    }, null, 2) + '\n')
  }
  assert.equal(generation._test.pruneReports(), true)
  assert.ok(readdirSync(jobDirectory)
    .filter(name => /^archjob-[a-f0-9]{32}\.json$/.test(name)).length <= 100)

  const httpSource = readFileSync(join(ROOT, 'orchestrator', 'site', 'server', 'http.js'), 'utf8')
  for (const route of [
    '/api/architecture/overview',
    '/api/architecture/nodes',
    '/api/architecture/findings',
    '/api/architecture/diff',
    '/api/architecture/generate',
    '/api/architecture/tasks/preview',
    '/api/architecture/tasks/create',
  ]) {
    assert.match(httpSource, new RegExp(route.replaceAll('/', '\\/')))
  }
  assert.doesNotMatch(httpSource, /\/api\/arch-map/)

  console.log('architecture.test.mjs: OK')
} finally {
  try {
    const generation = require('../server/architecture-generation.js')
    generation.killAll()
  } catch {}
  if (process.env.KEEP_ARCHITECTURE_FIXTURE !== '1') {
    rmSync(FIXTURE, { recursive: true, force: true })
  } else {
    console.error(`architecture fixture retained at ${FIXTURE}`)
  }
}
