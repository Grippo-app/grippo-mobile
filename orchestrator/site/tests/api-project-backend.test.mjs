import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import {
  existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, symlinkSync, writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'
import { spawn, spawnSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'

import { classifyChanges } from '../../api-contract/scripts/change-classifier.mjs'
import { _test as analyzerTest } from '../../api-contract/scripts/analyze-project.mjs'
import { createApiRefreshCoordinator, createApiRenderGeneration } from '../scripts/panels/api.js'

const REPO = fileURLToPath(new URL('../../../', import.meta.url))
const API = join(REPO, 'orchestrator', 'api-contract')
const apiPanelSource = readFileSync(join(REPO, 'orchestrator', 'site', 'scripts', 'panels', 'api.js'), 'utf8')
const require = createRequire(import.meta.url)
const projectInputs = require(join(REPO, 'orchestrator', 'site', 'server', 'api-project-inputs.js'))
const apiRelations = require(join(REPO, 'orchestrator', 'site', 'server', 'api-relations.js'))
const apiCatalog = require(join(REPO, 'orchestrator', 'site', 'server', 'api-catalog.js'))
const apiChanges = require(join(REPO, 'orchestrator', 'site', 'server', 'api-changes.js'))
const apiChangeReviews = require(join(REPO, 'orchestrator', 'site', 'server', 'api-change-reviews.js'))
const apiTasks = require(join(REPO, 'orchestrator', 'site', 'server', 'api-task-actions.js'))
const apiMock = require(join(REPO, 'orchestrator', 'site', 'server', 'api-mock.js'))
const contractHistory = require(join(REPO, 'orchestrator', 'site', 'server', 'contract-history.js'))
const architectureContract = require(join(
  REPO, 'orchestrator', 'site', 'server', 'architecture-contract.js',
))
const Ajv = createRequire(join(API, 'package.json'))('ajv')
const mockWorkers = []
const mockRoots = []

after(() => {
  for (const worker of mockWorkers) {
    if (worker.exitCode === null) worker.kill('SIGTERM')
  }
  for (const root of mockRoots) rmSync(root, { recursive: true, force: true })
})

const HASH_A = 'sha256:' + 'a'.repeat(64)
const HASH_B = 'sha256:' + 'b'.repeat(64)

test('unchanged background refresh keeps mounted API pagination handlers current', () => {
  const generation = createApiRenderGeneration()
  const initial = generation.begin(false)
  assert.equal(generation.isLatest(initial), true)
  assert.equal(generation.commit(initial), true)
  assert.equal(generation.isCurrent(initial), true)

  const unchangedBackground = generation.begin(true)
  assert.equal(generation.isLatest(unchangedBackground), true)
  assert.equal(generation.isCurrent(initial), true,
    'preserving the DOM must preserve the generation owned by its event handlers')

  const changedBackground = generation.begin(true)
  assert.equal(generation.commit(changedBackground), true)
  assert.equal(generation.isCurrent(initial), false)
  assert.equal(generation.isCurrent(changedBackground), true)

  generation.begin(false)
  assert.equal(generation.isCurrent(changedBackground), false,
    'clearing the DOM must invalidate its handlers immediately')
})

test('API coalesces SSE refreshes behind the foreground render and bounds every read', () => {
  const coordinator = createApiRefreshCoordinator()
  const foreground = {}
  coordinator.begin(foreground)
  assert.equal(coordinator.queue(), false)
  assert.equal(coordinator.queue(), false)
  assert.equal(coordinator.settle({}), false)
  assert.equal(coordinator.settle(foreground, true), true,
    'one queued refresh must run after the foreground content commits')
  assert.equal(coordinator.settle(foreground, true), false)
  assert.equal(coordinator.queue(), true)

  const failed = {}
  coordinator.begin(failed)
  assert.equal(coordinator.queue(), false)
  assert.equal(coordinator.settle(failed, false), false,
    'a queued SSE refresh must not hide a foreground error behind another loading state')
  assert.equal(coordinator.queue(), true,
    'settling a failed request must release the coordinator for an explicit retry')

  const stale = {}
  const latest = {}
  coordinator.begin(stale)
  assert.equal(coordinator.queue(), false)
  coordinator.begin(latest)
  assert.equal(coordinator.settle(stale, true), false)
  assert.equal(coordinator.settle(latest, true), true,
    'a superseded render must not consume the queued refresh')
  coordinator.reset()
  assert.equal(coordinator.queue(), true)

  assert.match(apiPanelSource, /requestJson\(url, \{ cache: 'no-store', timeoutMs: 15000,/)
  assert.match(apiPanelSource, /if \(!refreshCoordinator\.queue\(\)\) return;\s*render\(\{ background: true \}\)/)
  // A background refresh failure is reported by the persistent in-panel notice —
  // pausing without a visible way back would silently freeze the panel.
  assert.match(apiPanelSource,
    /if \(background && renderedTab === tab\)[\s\S]*?automaticRefreshPaused = true;\s*showRefreshNotice\(error\);/)
  assert.match(apiPanelSource, /if \(automaticRefreshPaused\) return;/)
  assert.match(apiPanelSource, /refreshCoordinator\.settle\(request, succeeded\)/)
})

test('Backend history cursor remains stable when a newer job is inserted between pages', () => {
  const startedAt = Date.parse('2026-08-05T00:00:00.000Z')
  const rows = Array.from({ length: 25 }, (_, index) => ({
    jobId: 'job-' + (index + 1).toString(16).padStart(32, '0'),
    startedAt: new Date(startedAt - index * 1000).toISOString(),
  }))
  const first = contractHistory._pageRowsForTests(rows, null, 20)
  assert.equal(first.ok, true)
  assert.deepEqual(first.rows.map((row) => row.jobId), rows.slice(0, 20).map((row) => row.jobId))
  assert.equal(typeof first.nextCursor, 'string')

  const newer = {
    jobId: 'job-' + 'f'.repeat(32),
    startedAt: new Date(startedAt + 1000).toISOString(),
  }
  const second = contractHistory._pageRowsForTests([newer, ...rows], first.nextCursor, 20)
  assert.equal(second.ok, true)
  assert.deepEqual(second.rows.map((row) => row.jobId), rows.slice(20).map((row) => row.jobId))
  assert.equal(second.nextCursor, null)
  assert.deepEqual(contractHistory._pageRowsForTests(
    rows, Buffer.from('20').toString('base64url'), 20
  ), { ok: false, error: 'bad-cursor' })
})

function field(overrides = {}) {
  return {
    name: 'value',
    jsonName: 'value',
    type: 'string',
    required: true,
    nullable_declared: false,
    ...overrides,
  }
}

function schema(modelField) {
  return { type: 'object', fields: Array.isArray(modelField) ? modelField : [modelField] }
}

function endpoint(overrides = {}) {
  return {
    operationId: 'getWidget',
    method: 'GET',
    path: '/widgets/{id}',
    area: 'widgets',
    summary: 'Get widget',
    auth: 'none',
    deprecated: false,
    request: {
      pathParams: [{ name: 'id', type: 'string', required: true }],
      query: [],
      body: { schemaRef: 'WidgetRequest', contentType: 'application/json' },
    },
    response: { 200: { schemaRef: 'WidgetResponse', array: false } },
    errors: ['404'],
    examples: { request: false, response: false },
    ...overrides,
  }
}

function classify(beforeFields, afterFields, endpointBefore = endpoint(), endpointAfter = endpoint()) {
  return classifyChanges({
    previousInventory: { endpoints: [endpointBefore] },
    nextInventory: { endpoints: [endpointAfter] },
    previousAreas: {
      widgets: {
        schemas: {
          WidgetRequest: schema(beforeFields.request),
          WidgetResponse: schema(beforeFields.response),
        },
      },
    },
    nextAreas: {
      widgets: {
        schemas: {
          WidgetRequest: schema(afterFields.request),
          WidgetResponse: schema(afterFields.response),
        },
      },
    },
    previousHash: HASH_A,
    currentHash: HASH_B,
    environmentId: 'local',
    generatedAt: '2026-01-01T00:00:00.000Z',
  })
}

test('semantic classifier applies request and response compatibility in the correct direction', () => {
  const base = { request: field(), response: field() }

  let report = classify(base, {
    request: field({ enum: ['a', 'b'] }),
    response: field({ enum: ['a', 'b'] }),
  })
  assert.equal(report.changes.find((row) => row.kind === 'request-enum-narrowed').severity, 'breaking')
  assert.equal(report.changes.find((row) => row.kind === 'response-enum-narrowed').severity, 'compatible')

  report = classify({
    request: field({ enum: ['a'] }),
    response: field({ enum: ['a'] }),
  }, {
    request: field({ enum: ['a', 'b'] }),
    response: field({ enum: ['a', 'b'] }),
  })
  assert.equal(report.changes.find((row) => row.kind === 'request-enum-widened').severity, 'compatible')
  assert.equal(
    report.changes.find((row) => row.kind === 'response-enum-widened').severity,
    'potentially-breaking',
  )

  report = classify({
    request: field({ nullable_declared: true }),
    response: field({ nullable_declared: false }),
  }, {
    request: field({ nullable_declared: false }),
    response: field({ nullable_declared: true }),
  })
  assert.equal(report.changes.filter((row) => row.severity === 'breaking').length, 2)

  report = classify({
    request: field({ type: 'number' }),
    response: field({ type: 'number' }),
  }, {
    request: field({ type: 'integer' }),
    response: field({ type: 'integer' }),
  })
  assert.equal(report.changes.find((row) => row.kind === 'request-field-type-changed').severity, 'breaking')
  assert.equal(report.changes.find((row) => row.kind === 'response-field-type-compatible').severity, 'compatible')
})

test('semantic classifier covers field addition, removal, requiredness, and nullability directions', () => {
  const requiredAdded = field({
    name: 'requiredAdded', jsonName: 'requiredAdded', required: true,
  })
  const optionalAdded = field({
    name: 'optionalAdded', jsonName: 'optionalAdded', required: false,
  })
  let report = classify(
    { request: [field()], response: [field()] },
    {
      request: [field(), requiredAdded],
      response: [field(), requiredAdded, optionalAdded],
    },
  )
  assert.equal(report.changes.some((row) =>
    row.kind === 'request-required-field-added' && row.severity === 'breaking'), true)
  assert.equal(report.changes.some((row) =>
    row.kind === 'response-required-field-added' &&
    row.severity === 'potentially-breaking'), true)
  assert.equal(report.changes.some((row) =>
    row.kind === 'response-optional-field-added' && row.severity === 'compatible'), true)

  report = classify(
    { request: [field()], response: [field()] },
    { request: [], response: [] },
  )
  assert.equal(report.changes.some((row) =>
    row.kind === 'request-field-removed' &&
    row.severity === 'potentially-breaking'), true)
  assert.equal(report.changes.some((row) =>
    row.kind === 'response-field-removed' && row.severity === 'breaking'), true)

  report = classify(
    {
      request: field({ required: false, nullable_declared: true }),
      response: field({ required: true, nullable_declared: false }),
    },
    {
      request: field({ required: true, nullable_declared: false }),
      response: field({ required: false, nullable_declared: true }),
    },
  )
  for (const kind of [
    'request-field-became-required',
    'request-nullability-narrowed',
    'response-field-became-optional',
    'response-nullability-widened',
  ]) {
    assert.equal(report.changes.some((row) =>
      row.kind === kind && row.severity === 'breaking'), true, kind)
  }
})

test('semantic classifier is stable, sees schema-only changes, and ignores all-null constraint upgrades', () => {
  const base = { request: field(), response: field() }
  const constrained = {
    request: field({ constraints: { minimum: 1 } }),
    response: field(),
  }
  const first = classify(base, constrained)
  const second = classify(base, constrained)
  assert.deepEqual(first, second)
  assert.match(first.changeSetId, /^changes-[a-f0-9]{24}$/)
  assert.equal(first.changes.some((row) => row.kind === 'request-constraints-narrowed' &&
    row.severity === 'potentially-breaking'), true)
  const exclusive = classify(base, {
    request: field({ constraints: { minimum: 0, exclusiveMinimum: 0 } }),
    response: field(),
  })
  assert.equal(exclusive.changes.some((row) =>
    row.kind === 'request-constraints-narrowed'), true)

  const nullConstraints = {
    request: field({ constraints: { minimum: null, maximum: null, pattern: null } }),
    response: field({ constraints: {} }),
  }
  assert.equal(classify(base, nullConstraints).changes.length, 0)
})

test('semantic classifier covers route, auth, required parameter, status, and endpoint removal rules', () => {
  const same = { request: field(), response: field() }
  let report = classify(same, same, endpoint(), endpoint({ path: '/widgets/{widgetId}' }))
  assert.equal(report.changes.some((row) => row.kind === 'endpoint-route-changed' &&
    row.severity === 'breaking'), true)
  assert.equal(report.changes.some((row) => row.kind === 'documentation-changed'), false)

  report = classifyChanges({
    previousInventory: { endpoints: [endpoint()] },
    nextInventory: { endpoints: [
      endpoint({ path: '/widgets/{widgetId}' }),
      endpoint({ operationId: 'getWidgetLegacy' }),
    ] },
    previousAreas: { widgets: {
      schemas: {
        WidgetRequest: schema(same.request),
        WidgetResponse: schema(same.response),
      },
    } },
    nextAreas: { widgets: {
      schemas: {
        WidgetRequest: schema(same.request),
        WidgetResponse: schema(same.response),
      },
    } },
    previousHash: HASH_A,
    currentHash: HASH_B,
    environmentId: 'local',
    generatedAt: '2026-01-01T00:00:00.000Z',
  })
  assert.equal(report.changes.some((row) =>
    row.kind === 'endpoint-route-changed-with-compatible-alias' &&
    row.severity === 'compatible'), true)

  report = classifyChanges({
    previousInventory: { endpoints: [endpoint()] },
    nextInventory: { endpoints: [
      endpoint({ path: '/widgets/{widgetId}', auth: 'bearer' }),
      endpoint({ operationId: 'getWidgetLegacy', auth: 'bearer' }),
    ] },
    previousAreas: { widgets: {
      schemas: {
        WidgetRequest: schema(same.request),
        WidgetResponse: schema(same.response),
      },
    } },
    nextAreas: { widgets: {
      schemas: {
        WidgetRequest: schema(same.request),
        WidgetResponse: schema(same.response),
      },
    } },
    previousHash: HASH_A,
    currentHash: HASH_B,
    environmentId: 'local',
    generatedAt: '2026-01-01T00:00:00.000Z',
  })
  assert.equal(report.changes.some((row) => row.kind === 'endpoint-route-changed'), true)
  assert.equal(report.changes.some((row) => row.kind === 'auth-requirement-changed'), true)

  report = classify(same, same, endpoint(), endpoint({ auth: 'bearer' }))
  assert.equal(report.changes.some((row) => row.kind === 'auth-requirement-changed' &&
    row.severity === 'breaking'), true)

  report = classify(same, same, endpoint(), endpoint({
    request: {
      ...endpoint().request,
      query: [{ name: 'mode', type: 'string', required: true }],
    },
  }))
  assert.equal(report.changes.some((row) => row.kind === 'request-required-parameter-added' &&
    row.severity === 'breaking'), true)

  report = classify(same, same, endpoint(), endpoint({
    request: {
      ...endpoint().request,
      query: [{ name: 'mode', type: 'string', required: false }],
      body: { schemaRef: 'WidgetRequest', contentType: 'application/problem+json' },
    },
    response: { 200: { schemaRef: 'WidgetResponse', array: true } },
  }))
  assert.equal(report.changes.some((row) =>
    row.kind === 'request-optional-parameter-added' && row.severity === 'compatible'), true)
  assert.equal(report.changes.some((row) =>
    row.kind === 'request-body-content-type-changed' &&
    row.severity === 'potentially-breaking'), true)
  assert.equal(report.changes.some((row) =>
    row.kind === 'response-shape-changed' && row.severity === 'breaking'), true)

  report = classify(same, same, endpoint({
    request: { ...endpoint().request, body: null },
  }), endpoint())
  assert.equal(report.changes.some((row) =>
    row.kind === 'request-body-added' && row.severity === 'breaking'), true)

  report = classify({
    request: field({
      name: 'items', jsonName: 'items', type: 'array', itemsRef: 'Widget',
    }),
    response: field(),
  }, {
    request: field({
      name: 'items', jsonName: 'items', type: 'array', itemsRef: 'Replacement',
    }),
    response: field(),
  })
  assert.equal(report.changes.some((row) =>
    row.kind === 'request-array-item-reference-changed' &&
    row.severity === 'breaking'), true)

  report = classify(same, same, endpoint(), endpoint({ response: {} }))
  assert.equal(report.changes.some((row) => row.kind === 'status-code-set-changed' &&
    row.severity === 'potentially-breaking'), true)

  report = classify({
    request: field({ format: 'email' }),
    response: field({ type: 'string' }),
  }, {
    request: field({ format: 'uuid' }),
    response: field({ type: 'integer' }),
  })
  assert.equal(report.changes.some((row) => row.kind === 'request-format-changed' &&
    row.severity === 'potentially-breaking'), true)
  assert.equal(report.changes.some((row) => row.kind === 'response-field-type-changed' &&
    row.severity === 'breaking'), true)

  report = classifyChanges({
    previousInventory: { endpoints: [endpoint()] },
    nextInventory: { endpoints: [endpoint({ operationId: 'getWidgetV2' })] },
    previousAreas: {
      widgets: { schemas: {
        WidgetRequest: schema(same.request),
        WidgetResponse: schema(same.response),
      } },
    },
    nextAreas: {
      widgets: { schemas: {
        WidgetRequest: schema(same.request),
        WidgetResponse: schema(same.response),
      } },
    },
    previousHash: HASH_A,
    currentHash: HASH_B,
    environmentId: 'local',
    generatedAt: '2026-01-01T00:00:00.000Z',
  })
  assert.equal(report.changes.some((row) => row.kind === 'operation-id-changed' &&
    row.severity === 'potentially-breaking'), true)
  assert.equal(report.changes.some((row) => row.kind === 'documentation-changed'), false)

  report = classify(
    { request: field(), response: field() },
    { request: field(), response: field() },
    endpoint(),
    endpoint({ summary: 'Updated prose', examples: { request: true, response: true } }),
  )
  assert.equal(report.changes.length, 1)
  assert.equal(report.changes[0].kind, 'documentation-changed')
  assert.equal(report.changes[0].severity, 'compatible')

  report = classify(
    {
      request: field({ enum: [{ alpha: 1, beta: 2 }] }),
      response: field(),
    },
    {
      request: field({ enum: [{ beta: 2, alpha: 1 }] }),
      response: field(),
    },
  )
  assert.equal(report.changes.length, 0)

  report = classifyChanges({
    previousInventory: { endpoints: [endpoint()] },
    nextInventory: { endpoints: [] },
    previousAreas: {},
    nextAreas: {},
    previousHash: HASH_A,
    currentHash: HASH_B,
    environmentId: 'local',
    generatedAt: '2026-01-01T00:00:00.000Z',
  })
  assert.equal(report.changes[0].kind, 'endpoint-removed')
  assert.equal(report.changes[0].severity, 'breaking')
})

test('semantic classifier traverses nested request and response model graphs', () => {
  const before = {
    WidgetRequest: schema(field()),
    WidgetResponse: schema(field({
      name: 'child', jsonName: 'child', type: 'ref:Child',
    })),
    Child: schema(field({ type: 'string' })),
  }
  const after = structuredClone(before)
  after.Child = schema(field({ type: 'integer' }))
  const report = classifyChanges({
    previousInventory: { endpoints: [endpoint()] },
    nextInventory: { endpoints: [endpoint()] },
    previousAreas: { widgets: { schemas: before } },
    nextAreas: { widgets: { schemas: after } },
    previousHash: HASH_A,
    currentHash: HASH_B,
    environmentId: 'local',
    generatedAt: '2026-01-01T00:00:00.000Z',
  })
  assert.equal(report.changes.some((row) =>
    row.modelId === 'Child' &&
    row.kind === 'response-field-type-changed' &&
    row.severity === 'breaking'), true)

  const swapped = classifyChanges({
    previousInventory: { endpoints: [endpoint()] },
    nextInventory: { endpoints: [endpoint({
      response: { 200: { schemaRef: 'ReplacementResponse', array: false } },
    })] },
    previousAreas: { widgets: { schemas: before } },
    nextAreas: { widgets: { schemas: {
      ...after,
      ReplacementResponse: schema(field()),
    } } },
    previousHash: HASH_A,
    currentHash: HASH_B,
    environmentId: 'local',
    generatedAt: '2026-01-01T00:00:00.000Z',
  })
  assert.deepEqual(
    swapped.changes.filter((row) => row.kind.includes('model') ||
      row.kind.includes('schema')).map((row) => row.kind),
    ['response-schema-reference-changed'],
  )
})

test('semantic classifier declares recursive model graph truncation', () => {
  const rootFields = Array.from({ length: 10001 }, (_, index) => field({
    name: 'child' + index,
    jsonName: 'child' + index,
    type: 'ref:Child' + index,
  }))
  const responseEndpoint = endpoint({
    response: { 200: { schemaRef: 'Root', array: false } },
  })
  const areas = { widgets: { schemas: {
    WidgetRequest: schema(field()),
    Root: { type: 'object', fields: rootFields },
  } } }
  const report = classifyChanges({
    previousInventory: { endpoints: [responseEndpoint] },
    nextInventory: { endpoints: [responseEndpoint] },
    previousAreas: areas,
    nextAreas: areas,
    previousHash: HASH_A,
    currentHash: HASH_B,
    environmentId: 'local',
    generatedAt: '2026-01-01T00:00:00.000Z',
  })
  assert.equal(report.changes.length, 0)
  assert.deepEqual(report.limitations, ['model-graph-cap'])
})

test('project analyzer distinguishes exact, derived, heuristic, ambiguous, and missing mappings', () => {
  const target = endpoint()
  const exactRecord = {
    path: 'src/widget-client.ts',
    text: ['export', 'async function getWidget() { return client.get("/widgets/{id}") }'].join(' ') + '\n',
  }
  let records = [exactRecord]
  let routes = records.flatMap(analyzerTest.routeCandidates)
  let mapping = analyzerTest.implementationFor(target, records, routes)
  assert.equal(mapping.state, 'implemented')
  assert.equal(mapping.confidence, 'exact')
  assert.equal(mapping.file, exactRecord.path)
  assert.equal(mapping.symbol, 'getWidget')
  const consumers = analyzerTest.consumersFor(mapping, [
    exactRecord,
    {
      path: 'src/commented-consumer.ts',
      text: '// getWidget()\n/* client.getWidget() */\n',
    },
    {
      path: 'src/widget-screen.ts',
      text: 'function loadWidget() { return getWidget() }\n',
    },
  ], { present: false, map: null })
  assert.deepEqual(consumers.map((row) => row.file), ['src/widget-screen.ts'])
  const indexedConsumers = analyzerTest.consumerEvidenceIndex([
    exactRecord,
    {
      path: 'src/commented-consumer.ts',
      text: '// getWidget()\n/* client.getWidget() */\n',
    },
    {
      path: 'src/widget-screen.ts',
      text: 'function loadWidget() { return getWidget() }\n',
    },
  ], [mapping], { present: false, map: null })
  assert.equal(indexedConsumers.truncated, false)
  assert.deepEqual(
    indexedConsumers.byOperation.getWidget.map((row) => row.file),
    ['src/widget-screen.ts'],
  )
  const indexed = analyzerTest.evidenceIndexes(records, [target])
  indexed.routes = analyzerTest.exactRouteIndex(routes).index
  assert.equal(analyzerTest.implementationFor(target, records, routes, indexed).state, 'implemented')
  assert.equal(analyzerTest.maskComments(
    'const url = "http://localhost/widgets"; // client.get("/ignored")\n',
  ).includes('http://localhost/widgets'), true)

  records = [{
    path: 'src/comment-only.ts',
    text: '// client.get("/widgets/{id}")\n/* function getWidget() {} */\n',
  }]
  routes = records.flatMap(analyzerTest.routeCandidates)
  assert.equal(routes.length, 0)
  mapping = analyzerTest.implementationFor(target, records, routes)
  assert.equal(mapping.state, 'missing')

  records = [{
    path: 'src/widget-operation.ts',
    text: ['export', 'const operation = "getWidget"'].join(' ') + '\n',
  }]
  mapping = analyzerTest.implementationFor(target, records, [])
  assert.equal(mapping.state, 'partial')
  assert.equal(mapping.confidence, 'derived')

  assert.equal(analyzerTest.isTestSource('src/commonTest/api/WidgetApiTest.kt'), true)
  assert.equal(analyzerTest.isTestSource('src/jvmTest/api/WidgetApi.kt'), true)
  assert.equal(analyzerTest.isTestSource('src/iosX64Test/api/WidgetApi.swift'), true)
  assert.equal(analyzerTest.isTestSource('src/testFixtures/api/widget.ts'), true)
  assert.equal(analyzerTest.isTestSource('src/latest/api/widget.ts'), false)
  assert.equal(analyzerTest.isTestSource('src/main/api/WidgetApi.kt'), false)

  records = [{
    path: 'src/widget-path.ts',
    text: 'export const route = "/widgets/{id}"\n',
  }]
  mapping = analyzerTest.implementationFor(target, records, [])
  assert.equal(mapping.state, 'partial')
  assert.equal(mapping.confidence, 'heuristic')

  records = [
    exactRecord,
    {
      path: 'src/other-widget-client.ts',
      text: 'export function another() { return fetch("/widgets/{id}") }\n',
    },
  ]
  routes = records.flatMap(analyzerTest.routeCandidates)
  mapping = analyzerTest.implementationFor(target, records, routes)
  assert.equal(mapping.state, 'partial')
  assert.equal(mapping.candidates.length, 2)

  mapping = analyzerTest.implementationFor(target, [{
    path: 'src/unrelated.ts', text: 'export const value = 1\n',
  }], [])
  assert.equal(mapping.state, 'missing')
  assert.equal(mapping.file, null)
})

test('new report schemas accept bounded producer-shaped values and reject extra fields', () => {
  const ajv = new Ajv({
    allErrors: true,
    strict: false,
    formats: { 'date-time': true },
  })
  const schemas = Object.fromEntries([
    'change-set', 'implementation-map', 'consumer-map', 'generation-manifest',
    'inventory', 'area',
  ].map((name) => [
    name,
    JSON.parse(readFileSync(join(API, 'contract-schemas', name + '.schema.json'), 'utf8')),
  ]))
  assert.equal(Object.hasOwn(schemas['change-set'].properties, 'legacyDelta'), false)
  for (const required of ['jobId', 'sourceFingerprint', 'committedGenerationId']) {
    assert.equal(schemas['change-set'].required.includes(required), true)
  }

  const change = classify(
    { request: field(), response: field() },
    { request: field({ required: false }), response: field() },
  )
  assert.equal(ajv.compile(schemas['change-set'])(change), false)
  const committedChange = {
    ...change,
    changeSetId: 'changes-' + 'd'.repeat(24),
    jobId: 'job-' + 'e'.repeat(32),
    sourceFingerprint: HASH_B,
    committedGenerationId: 'gen-20260101T000000Z-abcdef123456',
  }
  assert.equal(ajv.compile(schemas['change-set'])(committedChange), true)
  assert.equal(apiRelations._test.validChangeSet(committedChange), true)
  assert.equal(apiRelations._test.validChangeSet({
    ...committedChange,
    summary: { ...committedChange.summary, total: committedChange.summary.total + 1 },
  }), false)
  const manifestArtifact = (role, schemaVersion, persistence, required) => ({
    role,
    path: persistence === 'runtime'
      ? `orchestrator/.cache/api-contract/reports/${role}.json`
      : `orchestrator/api-contract/manifests/generation-artifacts/gen-20260101T000000Z-abcdef123456/${role}.json`,
    schemaVersion,
    size: 2,
    hash: HASH_A,
    persistence,
    required,
  })
  const manifest = {
    schemaVersion: 1,
    generationId: 'gen-20260101T000000Z-abcdef123456',
    environmentId: 'local',
    sourceKind: 'openapi',
    sourceFingerprint: HASH_A,
    previousHash: null,
    currentHash: HASH_B,
    state: 'committed',
    createdAt: '2026-01-01T00:00:00.000Z',
    committedAt: '2026-01-01T00:00:01.000Z',
    artifacts: [
      manifestArtifact('inventory', 1, 'committed', true),
      manifestArtifact('normalized-spec', 1, 'committed', true),
      manifestArtifact('change-report', 2, 'runtime', false),
    ],
  }
  const validateManifest = ajv.compile(schemas['generation-manifest'])
  assert.equal(validateManifest(manifest), true, JSON.stringify(validateManifest.errors))
  assert.equal(validateManifest({
    ...manifest,
    artifacts: manifest.artifacts.map((row) =>
      row.role === 'normalized-spec' ? { ...row, schemaVersion: 2 } : row),
  }), false)
  const inventory = {
    schemaVersion: 1,
    source: {
      kind: 'openapi',
      openApiUrl: null,
      openApiVersion: '3.1.0',
      title: 'Widgets',
      specHash: HASH_A,
      fetchedAt: '2026-01-01T00:00:00.000Z',
      postmanImportedAt: null,
    },
    stats: { endpoints: 1, areas: 1, schemas: 2 },
    areas: { widgets: ['getWidget'] },
    endpoints: [endpoint()],
  }
  assert.equal(ajv.compile(schemas.inventory)(inventory), true)
  assert.equal(apiRelations._test.validInventory(inventory), true)
  assert.equal(apiRelations._test.validInventory({ ...inventory, unexpected: true }), false)
  assert.equal(apiRelations._test.validInventory({
    ...inventory,
    endpoints: [endpoint({
      request: {
        ...endpoint().request,
        query: [
          { name: 'mode', type: 'string', required: false },
          { name: 'mode', type: 'string', required: true },
        ],
      },
    })],
  }), false)
  assert.equal(apiRelations._test.validInventory({
    ...inventory,
    endpoints: [endpoint({
      response: { 404: { schemaRef: null, array: false } },
    })],
  }), false)
  const constructorInventory = {
    ...inventory,
    areas: { constructor: ['getWidget'] },
    endpoints: [endpoint({ area: 'constructor' })],
  }
  assert.equal(ajv.compile(schemas.inventory)(constructorInventory), true)
  assert.equal(apiRelations._test.validInventory(constructorInventory), true)
  const area = {
    schemaVersion: 1,
    area: 'widgets',
    schemas: {
      Widget: {
        fields: [field()],
      },
    },
  }
  const validateArea = ajv.compile(schemas.area)
  assert.equal(validateArea(area), true, JSON.stringify(validateArea.errors))
  assert.equal(apiRelations.validArea(area, 'widgets'), true)
  const spacedFieldArea = {
    ...area,
    schemas: {
      Widget: {
        fields: [field({
          name: 'display name',
          jsonName: 'display name',
          format: 'custom\nformat',
        })],
      },
    },
  }
  assert.equal(validateArea(spacedFieldArea), true, JSON.stringify(validateArea.errors))
  assert.equal(apiRelations.validArea(spacedFieldArea, 'widgets'), true)
  assert.equal(apiRelations.validArea({ ...area, unexpected: true }, 'widgets'), false)
  assert.equal(apiRelations.validArea({
    ...area,
    schemas: {
      Widget: {
        fields: [field({ type: 'ref:Missing' })],
      },
    },
  }, 'widgets'), false)
  assert.equal(validateArea({
    ...area,
    schemas: {
      Widget: {
        fields: [field({ name: 'x'.repeat(201) })],
      },
    },
  }), false)

  const common = {
    schemaVersion: 1,
    analyzerVersion: 'api-project-analyzer-v1',
    committedGenerationId: 'gen-20260101T000000Z-abcdef123456',
    contractHash: HASH_A,
    environmentId: 'local',
    projectCodeRevision: HASH_B,
    generatedAt: '2026-01-01T00:00:00.000Z',
  }
  const implementation = {
    ...common,
    analysisStatus: 'complete',
    coverage: {
      total: 1, implemented: 1, missing: 0, partial: 0, unknown: 0, analyzedFiles: 1,
    },
    receipt: {
      fileCount: 1, totalBytes: 10, directoryCount: 2,
      files: [{ path: 'src/client.ts', size: 10, hash: HASH_A }],
    },
    operations: [{
      operationId: 'getWidget', state: 'implemented', file: 'src/client.ts',
      symbol: 'getWidget', confidence: 'exact', evidence: ['literal fetch request'],
    }],
    unresolved: [],
    limitations: [],
  }
  const consumer = {
    ...common,
    architectureStructuralHash: null,
    analysisStatus: 'partial',
    operations: [{
      operationId: 'getWidget',
      analysisStatus: 'partial',
      consumers: [{
        id: 'consumer-' + 'c'.repeat(24),
        architectureId: null,
        file: 'src/use-widget.ts',
        symbol: 'loadWidget',
        kind: 'source-reference',
      }],
    }],
    limitations: ['static-consumer-analysis-not-conclusive'],
  }
  const validateImplementation = ajv.compile(schemas['implementation-map'])
  assert.equal(validateImplementation(implementation), true)
  assert.equal(ajv.compile(schemas['consumer-map'])(consumer), true)
  assert.equal(apiRelations._test.validImplementation(implementation, inventory), true)
  assert.equal(apiRelations._test.validConsumers(consumer, inventory), true)
  assert.equal(validateImplementation({ ...implementation, unexpected: true }), false)
  assert.equal(apiRelations._test.validImplementation({
    ...implementation, unexpected: true,
  }, inventory), false)
  assert.equal(apiRelations._test.validImplementation({
    ...implementation, schemaVersion: 2,
  }, inventory), false)
  const inconsistent = structuredClone(implementation)
  inconsistent.coverage.implemented = 0
  inconsistent.coverage.missing = 1
  assert.equal(apiRelations._test.validImplementation(inconsistent, inventory), false)
  const poisonedConsumer = structuredClone(consumer)
  poisonedConsumer.operations[0].consumers[0].file = '../outside.ts'
  assert.equal(apiRelations._test.validConsumers(poisonedConsumer, inventory), false)
  assert.equal(apiRelations._test.validConsumers({
    ...consumer,
    analysisStatus: 'complete',
  }, inventory), false)

  const drift = {
    schemaVersion: 1,
    checkedAt: common.generatedAt,
    specHash: HASH_A,
    committedGenerationId: common.committedGenerationId,
    contractHash: common.contractHash,
    environmentId: common.environmentId,
    projectCodeRevision: common.projectCodeRevision,
    analyzerVersion: common.analyzerVersion,
    limitations: [],
    summary: { errors: 1, warnings: 0, infos: 0 },
    findings: [{
      severity: 'ERROR',
      kind: 'dto-field-unknown',
      area: 'widgets',
      schemaRef: 'WidgetResponse',
      operationId: null,
      field: 'value',
      dtoFile: 'src/widget-client.ts',
      message: 'DTO field does not exist in the current server schema',
      suggestion: null,
    }],
  }
  assert.equal(apiRelations._test.validDrift(drift), true)
  assert.equal(apiRelations._test.validDrift({
    ...drift,
    findings: [{ ...drift.findings[0], dtoFile: '../outside.ts' }],
  }), false)
  assert.equal(apiRelations._test.validDrift({
    ...drift,
    summary: { errors: 0, warnings: 1, infos: 0 },
  }), false)
})

test('canonical project input receipt is deterministic, VCS-neutral, and fail-closed on symlinks', () => {
  const root = mkdtempSync(join(tmpdir(), 'api-project-inputs-'))
  try {
    mkdirSync(join(root, 'src'), { recursive: true })
    mkdirSync(join(root, '.git'), { recursive: true })
    mkdirSync(join(root, 'orchestrator'), { recursive: true })
    writeFileSync(join(root, 'src', 'client.ts'), 'export ' + 'const getWidget = () => fetch("/widgets/1")\n')
    writeFileSync(join(root, 'src', 'client.mjs'), 'const getModernWidget = () => fetch("/widgets/2")\n')
    writeFileSync(join(root, 'src', 'client.cjs'), 'exports.getCommonWidget = () => fetch("/widgets/3")\n')
    writeFileSync(join(root, '.git', 'ignored.ts'), 'not part of the receipt\n')
    writeFileSync(join(root, 'orchestrator', 'project-config.md'), '---\nbackendContractEnabled: true\n---\n')

    const first = projectInputs.collect(root, { includeText: false })
    const second = projectInputs.collect(root, { includeText: false })
    assert.equal(first.ok, true)
    assert.equal(first.projectCodeRevision, second.projectCodeRevision)
    assert.deepEqual(first.receipt.files.map((row) => row.path), [
      'orchestrator/project-config.md',
      'src/client.cjs',
      'src/client.mjs',
      'src/client.ts',
    ])

    writeFileSync(join(root, 'src', 'client.ts'), 'export ' + 'const getWidget = () => fetch("/widgets/2")\n')
    const changed = projectInputs.collect(root, { includeText: false })
    assert.notEqual(changed.projectCodeRevision, first.projectCodeRevision)

    symlinkSync(join(root, 'src', 'client.ts'), join(root, 'src', 'linked.ts'))
    const unsafe = projectInputs.collect(root, { includeText: false })
    assert.equal(unsafe.ok, false)
    assert.equal(unsafe.error, 'analyzer-source-symlink')
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

test('sidecar runtime reports honor the same isolated cache root as Site consumers', () => {
  const root = mkdtempSync(join(tmpdir(), 'api-runtime-cache-'))
  try {
    const cache = join(root, 'orchestrator', '.isolated-cache')
    mkdirSync(cache, { recursive: true })
    const utilFile = join(API, 'scripts', '_util.mjs')
    const script = [
      "import { contractPath } from '" + new URL('file://' + utilFile).href + "'",
      "process.stdout.write(contractPath('reports', 'drift.json'))",
    ].join('\n')
    const result = spawnSync(process.execPath, ['--input-type=module', '-e', script], {
      cwd: root,
      env: {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: root,
        ORCHESTRATOR_CACHE_DIR: cache,
      },
      encoding: 'utf8',
    })
    assert.equal(result.status, 0, result.stderr)
    assert.equal(result.stdout, join(cache, 'api-contract', 'reports', 'drift.json'))
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

test('durable change-set artifacts are byte-immutable and exact replays are idempotent', () => {
  const root = mkdtempSync(join(tmpdir(), 'api-change-immutable-'))
  try {
    mkdirSync(join(root, 'orchestrator'), { recursive: true })
    const script = `
const path = require('node:path');
const history = require(path.join(process.argv[1], 'orchestrator/site/server/contract-history.js'));
const first = {
  schemaVersion: 2,
  changeSetId: 'changes-${'a'.repeat(24)}',
  changes: []
};
const file = history.writeChangeSet(first);
const replay = history.writeChangeSet(first);
let conflict = null;
try {
  history.writeChangeSet({ ...first, changes: [{ id: 'different' }] });
} catch (error) {
  conflict = error && error.message;
}
process.stdout.write(JSON.stringify({ file, replay, conflict }));
`
    const result = spawnSync(process.execPath, ['-e', script, REPO], {
      cwd: root,
      env: {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: root,
        ORCHESTRATOR_CACHE_DIR: join(root, 'orchestrator', '.cache'),
      },
      encoding: 'utf8',
    })
    assert.equal(result.status, 0, result.stderr)
    const output = JSON.parse(result.stdout)
    assert.equal(output.replay, output.file)
    assert.equal(output.conflict, 'change-set-immutable-conflict')
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

test('catalog/query/batch helpers enforce closed filters, cursors, redaction, and batch bounds', () => {
  assert.equal(apiCatalog._test.endpointFilters({ limit: '501' }).error, 'bad-api-query')
  assert.equal(apiCatalog._test.endpointFilters({ unexpected: '1' }).error, 'bad-api-query')
  const boundedList = apiCatalog._test.boundedListResponse(
    { limitations: [], schemaVersion: 1 },
    Array.from({ length: 500 }, (_, index) => ({
      index, value: 'x'.repeat(10_000),
    })),
    0,
    500,
    { generationId: 'gen-20260101T000000Z-abcdef123456', filterHash: HASH_A },
  )
  assert.ok(Buffer.byteLength(JSON.stringify(boundedList), 'utf8') <= apiCatalog.RESPONSE_MAX)
  assert.equal(boundedList.page.responseTruncated, true)
  assert.ok(boundedList.page.nextCursor)
  assert.equal(boundedList.limitations.includes('api-response-size-cap'), true)
  const boundedDetail = apiCatalog._test.boundDetailResponse({
    contract: {
      schemaIds: Array.from({ length: 15000 }, (_, index) =>
        'Schema' + index + 'x'.repeat(190)),
      schemas: {},
      schemasTruncated: false,
    },
    changes: [],
    mismatches: [],
    limitations: [],
    examples: { request: null, responses: {} },
    consumers: { analysisStatus: 'partial', items: [] },
  })
  assert.ok(Buffer.byteLength(JSON.stringify(boundedDetail), 'utf8') <= 2 * 1024 * 1024)
  assert.equal(boundedDetail.limitations.includes('api-detail-size-cap'), true)
  assert.equal(apiChanges._test.parse({ modelId: 'Widget' }).modelId, 'Widget')
  assert.equal(apiChanges._test.parse({ severity: 'attention' }).severity, 'attention')
  const reviewState = {
    schemaVersion: 1,
    reviews: [{
      changeSetId: 'changes-' + 'a'.repeat(24),
      changeId: 'chg-' + 'b'.repeat(24),
      reviewedAt: '2026-01-01T00:00:00.000Z',
      idempotencyKey: 'api.change.review.test',
    }],
  }
  assert.equal(apiChangeReviews._test.valid(reviewState), true)
  assert.match(apiChangeReviews._test.revision(reviewState), /^sha256:[a-f0-9]{64}$/)
  assert.equal(apiChangeReviews._test.valid({
    ...reviewState,
    reviews: reviewState.reviews.concat(reviewState.reviews),
  }), false)
  assert.equal(apiChangeReviews._test.valid({
    ...reviewState,
    reviews: reviewState.reviews.concat([{
      ...reviewState.reviews[0],
      changeSetId: 'changes-' + 'c'.repeat(24),
      changeId: 'chg-' + 'd'.repeat(24),
    }]),
  }), false)
  assert.equal(apiChanges._test.parse({ modelId: '\u0000' }).error, 'bad-api-changes-query')
  assert.equal(apiTasks._test.validSourceId('api:change:chg-' + 'a'.repeat(24)), true)
  assert.equal(apiTasks._test.validSourceId('api:change:../../secret'), false)
  assert.equal(apiTasks._test.validReportHashes({
    implementation: HASH_A, consumers: null, drift: HASH_B, changes: null,
  }), true)
  assert.equal(apiTasks._test.validReportHashes({
    implementation: HASH_A, consumers: null, drift: HASH_B, changes: null, extra: null,
  }), false)
  assert.match(apiRelations.sourceId('missing', 'получить/виджет'), /^api:missing:missing-[a-f0-9]{24}$/)
  const mismatchFinding = {
    kind: 'dto-field-unknown',
    operationId: null,
    area: 'widgets',
    schemaRef: 'WidgetResponse',
    field: 'legacy',
    dtoFile: 'src/widget-client.ts',
  }
  assert.equal(
    apiRelations.mismatchId(mismatchFinding, 0, HASH_A),
    apiRelations.mismatchId(mismatchFinding, 0, HASH_B),
  )
  assert.notEqual(
    apiRelations.mismatchId(mismatchFinding, 0),
    apiRelations.mismatchId(mismatchFinding, 1),
  )

  const secret = apiCatalog._test.sanitizeExample({
    password: 'visible',
    accessToken: 'also-visible',
    nested: { authorization: 'Bearer top-secret', clientSecret: 'hidden', safe: 'ok' },
  }, '', { nodes: 0 }, 0)
  assert.equal(secret.password, '[redacted]')
  assert.equal(secret.accessToken, '[redacted]')
  assert.equal(secret.nested.authorization, '[redacted]')
  assert.equal(secret.nested.clientSecret, '[redacted]')
  assert.equal(secret.nested.safe, 'ok')
  const unicodeBudget = { nodes: 0 }
  const unicode = apiCatalog._test.sanitizeExample('😀'.repeat(1001), '', unicodeBudget, 0)
  assert.equal(Array.from(unicode).length, 1000)
  assert.equal(unicode.endsWith('\ud83d'), false)
  assert.equal(unicodeBudget.truncated, true)
  assert.throws(
    () => apiMock._test.safeFixtureValue('😀'.repeat(1001), '', { nodes: 0 }, 0),
    (error) => error && error.code === 'api-mock-fixture-cap',
  )
  assert.throws(
    () => apiMock._test.generatedValue(
      'Missing', {}, Object.create(null), 0, { nodes: 0, bytes: 0 },
    ),
    (error) => error && error.code === 'api-mock-area-unavailable',
  )
  assert.deepEqual(apiCatalog._test.mediaExample({ example: null }), {
    present: true, value: null,
  })
  assert.deepEqual(apiCatalog._test.mediaExample({}), {
    present: false, value: null,
  })
  const exampleBudget = { nodes: 0, truncated: false }
  apiCatalog._test.sanitizeExample(Array.from({ length: 21 }, () => 'value'), '', exampleBudget, 0)
  assert.equal(exampleBudget.truncated, true)
  const recursiveExamples = apiCatalog._test.generatedExamples(endpoint(), {
    schemas: {
      WidgetRequest: { fields: [] },
      WidgetResponse: {
        fields: [{
          name: 'self', jsonName: 'self', type: 'ref:WidgetResponse', required: true,
        }],
      },
    },
  }, {
    requestPresent: false,
    responses: {},
  })
  assert.equal(recursiveExamples.truncated, true)
  assert.equal(recursiveExamples.truncatedResponses['200'], true)
  const exhaustedGraphBudget = { visits: 199999 }
  const graphResult = apiCatalog._test.modelReachable(
    ['Root'],
    {
      Root: {
        fields: [{ type: 'ref:Child' }],
      },
      Child: { fields: [] },
    },
    'Child',
    exhaustedGraphBudget,
  )
  assert.deepEqual(graphResult, { found: false, truncated: true })
  assert.deepEqual(
    apiCatalog._test.modelReachable(
      ['constructor'],
      JSON.parse('{"constructor":{"fields":[]}}'),
      'constructor',
    ),
    { found: true, truncated: false },
  )
  assert.equal(apiCatalog._test.endpointRootsAvailable(
    endpoint(),
    { WidgetRequest: { fields: [] }, WidgetResponse: { fields: [] } },
  ), true)
  assert.equal(apiCatalog._test.endpointRootsAvailable(
    endpoint(),
    { WidgetRequest: { fields: [] } },
  ), false)
  assert.equal(apiCatalog._test.endpointRootsAvailable(
    {
      request: { body: { schemaRef: 'constructor' } },
      response: {},
    },
    JSON.parse('{"constructor":{"fields":[]}}'),
  ), true)
  const operationRows = apiCatalog._test.operationRowIndex([
    { operationId: 'constructor' },
  ])
  assert.equal(Object.getPrototypeOf(operationRows), null)
  assert.equal(operationRows.constructor.operationId, 'constructor')
  assert.equal(operationRows.toString, undefined)
  const endpointRow = {
    operationId: 'getNested',
    models: { requestIds: ['Root'], responseIds: [] },
    implementation: { state: 'implemented' },
    consumers: { analysisStatus: 'complete' },
    _consumerRows: [{ id: 'consumer-1', architectureId: null, file: null, symbol: null }],
  }
  const modelRows = apiCatalog._test.modelRowIndex(
    [endpointRow],
    apiCatalog._test.operationRowIndex([endpointRow]),
    { Nested: ['getNested'] },
  )
  assert.equal(modelRows.Nested[0], endpointRow)
  const nestedImpact = apiChanges._test.impact({ modelId: 'Nested' }, {
    byOperation: apiCatalog._test.operationRowIndex([endpointRow]),
    byModel: modelRows,
  })
  assert.equal(nestedImpact.affectedImplementations[0].state, 'implemented')
  assert.equal(nestedImpact.affectedConsumers[0].id, 'consumer-1')
  assert.equal(nestedImpact.noKnownConsumersIsConclusive, false)
  const publicSchema = apiCatalog._test.publicSchema({
    fields: [{
      name: 'token',
      jsonName: 'token',
      type: 'string',
      required: false,
      example: 'real-token',
      enum_observed: ['real-token'],
      enum: ['contract-value'],
    }],
  })
  assert.equal(Object.hasOwn(publicSchema.fields[0], 'example'), false)
  assert.equal(Object.hasOwn(publicSchema.fields[0], 'enum_observed'), false)
  assert.deepEqual(publicSchema.fields[0].enum, ['contract-value'])
  const architectureA = architectureContract._test.sha(
    architectureContract._test.canonical({ modules: { b: 2, a: 1 } }),
  )
  const architectureB = architectureContract._test.sha(
    architectureContract._test.canonical({ modules: { a: 1, b: 2 } }),
  )
  assert.equal(architectureA, architectureB)
  assert.notEqual(architectureA, architectureContract._test.sha(
    architectureContract._test.canonical({ modules: { a: 1, b: 3 } }),
  ))
  assert.equal(apiRelations._test.environmentMismatch(null, 'local'), true)
  assert.equal(apiRelations._test.environmentMismatch({ id: 'local' }, 'local'), false)
  assert.deepEqual(apiRelations._test.reportLimitations({
    limitations: ['consumer-analysis-partial', '', 'x'.repeat(101), 42],
  }), ['consumer-analysis-partial'])
  const backendAction = readFileSync(
    join(API, 'scripts', 'backend-action.mjs'), 'utf8',
  )
  assert.match(backendAction, /'ORCHESTRATOR_CACHE_DIR'/)
  assert.equal(apiMock._test.publicState({
    state: 'running',
    serverId: 'mock-' + 'f'.repeat(24),
    stateRevision: HASH_A,
    port: 43210,
    committedGenerationId: 'gen-20260101T000000Z-abcdef123456',
    contractHash: HASH_A,
    environmentId: 'local',
    startedAt: '2026-01-01T00:00:00.000Z',
    stoppedAt: null,
    stopReason: null,
  }, { ok: true, empty: true }).staleContract, true)
})

test('fixed mock worker binds loopback, serves deterministic fixtures, and logs no bodies or headers', async () => {
  const root = mkdtempSync(join(tmpdir(), 'api-mock-worker-'))
  mockRoots.push(root)
  const serverId = 'mock-' + 'a'.repeat(24)
  const identity = 'b'.repeat(64)
  const directory = join(root, 'instances', serverId)
  const fixtureFile = join(directory, 'fixture.json')
  const logFile = join(directory, 'requests.jsonl')
  const readyFile = join(directory, 'ready.json')
  mkdirSync(directory, { recursive: true })
  const fixtureBytes = JSON.stringify({
    schemaVersion: 1,
    serverId,
    committedGenerationId: 'gen-20260101T000000Z-abcdef123456',
    contractHash: HASH_A,
    environmentId: 'local',
    generatedAt: '2026-01-01T00:00:00.000Z',
    routes: [{
      operationId: 'getWidget',
      method: 'GET',
      path: '/widgets/{id}',
      status: 200,
      contentType: 'application/json',
      generated: true,
      body: { id: '00000000-0000-0000-0000-000000000000' },
    }],
  }) + '\n'
  writeFileSync(fixtureFile, fixtureBytes)
  const fixtureHash = 'sha256:' + createHash('sha256').update(fixtureBytes).digest('hex')
  const worker = spawn(process.execPath, [
    join(REPO, 'orchestrator', 'site', 'server', 'api-mock-worker.js'),
    '--server-id', serverId,
    '--port', '0',
    '--fixture', fixtureFile,
    '--fixture-hash', fixtureHash,
    '--log', logFile,
    '--ready', readyFile,
    '--identity', identity,
    '--root', root,
  ], { stdio: ['ignore', 'pipe', 'pipe'] })
  mockWorkers.push(worker)
  let stderr = ''
  worker.stderr.on('data', (chunk) => { stderr += chunk })

  const ready = await new Promise((resolve, reject) => {
    let stdout = ''
    const timer = setTimeout(() => reject(new Error('mock worker timeout: ' + stderr)), 5000)
    worker.stdout.on('data', (chunk) => {
      stdout += chunk
      const newline = stdout.indexOf('\n')
      if (newline < 0) return
      clearTimeout(timer)
      resolve(JSON.parse(stdout.slice(0, newline)))
    })
    worker.once('exit', (code) => {
      clearTimeout(timer)
      reject(new Error(`mock worker exited ${code}: ${stderr}`))
    })
  })
  assert.equal(ready.ready, true)
  assert.ok(ready.port >= 1024 && ready.port <= 65535)
  assert.equal(JSON.parse(readFileSync(readyFile, 'utf8')).identity, identity)

  const response = await fetch(`http://127.0.0.1:${ready.port}/widgets/123?token=secret`, {
    headers: { authorization: 'Bearer secret-never-log' },
  })
  assert.equal(response.status, 200)
  assert.equal(response.headers.get('x-content-type-options'), 'nosniff')
  assert.deepEqual(await response.json(), {
    id: '00000000-0000-0000-0000-000000000000',
  })
  const oversized = await fetch(`http://127.0.0.1:${ready.port}/widgets/123`, {
    method: 'POST',
    body: 'x'.repeat(1024 * 1024 + 1),
  })
  assert.equal(oversized.status, 413)
  assert.equal(oversized.headers.get('x-content-type-options'), 'nosniff')
  assert.deepEqual(await oversized.json(), { error: 'mock-request-body-too-large' })
  for (let attempt = 0; attempt < 40 && !existsSync(logFile); attempt++) {
    await new Promise((resolve) => setTimeout(resolve, 25))
  }
  const log = readFileSync(logFile, 'utf8')
  assert.equal(log.includes('"path":"/widgets/123"'), true)
  assert.equal(log.includes('token'), false)
  assert.equal(log.includes('authorization'), false)
  assert.equal(log.includes('secret-never-log'), false)
  const shutdown = await fetch(
    `http://127.0.0.1:${ready.port}/__orchestrator_mock/${serverId}/${identity}`,
    { method: 'POST' },
  )
  assert.equal(shutdown.status, 202)
  assert.deepEqual(await shutdown.json(), {
    serverId,
    identity,
    pid: worker.pid,
    stopping: true,
  })
  await new Promise((resolve, reject) => {
    if (worker.exitCode !== null) { resolve(); return }
    const timer = setTimeout(() => reject(new Error('mock worker did not stop')), 3000)
    worker.once('exit', () => {
      clearTimeout(timer)
      resolve()
    })
  })
})

test('mock lifecycle keeps one generation-pinned instance, durable index, logs, and verified stop', () => {
  const root = mkdtempSync(join(tmpdir(), 'api-mock-lifecycle-'))
  try {
    const contractDir = join(root, 'orchestrator', 'api-contract')
    const candidate = join(root, 'candidate')
    mkdirSync(join(contractDir, 'manifests'), { recursive: true })
    mkdirSync(join(candidate, 'areas'), { recursive: true })
    writeFileSync(join(root, 'orchestrator', 'project-config.md'),
      '---\nbackendContractEnabled: true\n---\n')
    writeFileSync(join(contractDir, 'environments.json'), JSON.stringify({
      schemaVersion: 1,
      environments: [{
        id: 'local',
        label: 'Local',
        sourceKind: 'openapi',
        sourceUrl: 'http://127.0.0.1:8080/openapi.json',
        postmanEnrichmentUrl: null,
        authRef: null,
      }],
      defaultEnvironmentId: 'local',
    }, null, 2) + '\n')
    const inventory = {
      schemaVersion: 1,
      source: {
        kind: 'openapi',
        openApiUrl: 'http://127.0.0.1:8080/openapi.json',
        openApiVersion: '3.1.0',
        title: 'Fixture',
        specHash: HASH_A,
        fetchedAt: '2026-01-01T00:00:00.000Z',
        postmanImportedAt: null,
      },
      stats: { endpoints: 1, areas: 1, schemas: 1 },
      areas: { widgets: ['getWidget'] },
      endpoints: [endpoint({
        request: { pathParams: [], query: [], body: null },
        response: { 200: { schemaRef: 'WidgetResponse', array: false } },
      })],
    }
    writeFileSync(join(candidate, 'inventory.json'), JSON.stringify(inventory, null, 2) + '\n')
    writeFileSync(join(candidate, 'areas', 'widgets.json'), JSON.stringify({
      schemaVersion: 1,
      area: 'widgets',
      schemas: {
        WidgetResponse: {
          fields: [
            field({ format: 'uuid' }),
            field({
              name: 'accessToken',
              jsonName: 'accessToken',
              enum: ['production-secret-must-not-escape'],
            }),
            field({
              name: 'profile',
              jsonName: 'profile',
              enum: [{
                password: 'nested-production-secret',
                displayName: 'deterministic',
              }],
            }),
          ],
        },
      },
    }, null, 2) + '\n')
    writeFileSync(join(candidate, 'openapi.json'), JSON.stringify({
      openapi: '3.1.0',
      info: { title: 'Fixture', version: '1' },
      paths: {},
    }, null, 2) + '\n')

    const script = `
const fs = require('fs');
const path = require('path');
const repo = process.argv[1];
const root = process.env.ORCHESTRATOR_PROJECT_ROOT;
const generation = require(path.join(repo, 'orchestrator/site/server/contract-generation.js'));
const relations = require(path.join(repo, 'orchestrator/site/server/api-relations.js'));
const apiMock = require(path.join(repo, 'orchestrator/site/server/api-mock.js'));
(async () => {
  const published = generation.publish({
    generationId: generation.createGenerationId(),
    environmentId: 'local',
    sourceKind: 'openapi',
    sourceFingerprint: '${HASH_A}',
    expectedSnapshotHash: null,
    inventoryFile: path.join(root, 'candidate/inventory.json'),
    areasDir: path.join(root, 'candidate/areas'),
    specFile: path.join(root, 'candidate/openapi.json'),
    createdAt: '2026-01-01T00:00:00.000Z'
  });
  if (!published.ok) throw new Error(JSON.stringify(published));
  const pinned = relations.snapshot({ freshInputs: true });
  const areaFile = pinned.current.artifacts['area:widgets'];
  const originalArea = fs.readFileSync(areaFile);
  let tamperError = null;
  try {
    fs.writeFileSync(areaFile, Buffer.from(originalArea.toString('utf8') + ' '));
    apiMock._test.buildFixture(pinned, 'mock-' + 'f'.repeat(24));
  } catch (error) {
    tamperError = error && error.code || error && error.message || 'unknown';
  } finally {
    fs.writeFileSync(areaFile, originalArea);
  }
  const request = {
    expectedGenerationId: published.generationId,
    contractHash: published.currentHash,
    environmentId: 'local',
    portMode: 'auto',
    port: null,
    idempotencyKey: 'api.mock.lifecycle.start'
  };
  const started = await apiMock.start(request);
  if (!started.ok) throw new Error(JSON.stringify(started));
  const response = await fetch(started.mock.url + '/widgets/123');
  const body = await response.json();
  const conflict = await apiMock.start({ ...request, idempotencyKey: 'api.mock.lifecycle.other' });
  const logs = apiMock.logs({ serverId: started.mock.serverId, limit: '100' });
  process.stdout.write(JSON.stringify({
    started, responseStatus: response.status, body, conflict, logs, tamperError
  }));
})().catch((error) => {
  try { apiMock.killAll(); } catch {}
  process.stderr.write(String(error && error.stack || error));
  process.exitCode = 1;
});
`
    const result = spawnSync(process.execPath, ['-e', script, REPO], {
      cwd: root,
      env: {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: root,
        ORCHESTRATOR_CACHE_DIR: join(root, 'orchestrator', '.cache'),
      },
      encoding: 'utf8',
      timeout: 15000,
      maxBuffer: 4 * 1024 * 1024,
    })
    assert.equal(result.status, 0, result.stderr + result.stdout)
    const output = JSON.parse(result.stdout)
    assert.equal(output.started.mock.url.startsWith('http://127.0.0.1:'), true)
    assert.equal(output.tamperError, 'api-mock-area-unavailable')
    assert.equal(output.responseStatus, 200)
    assert.equal(output.body.value, '00000000-0000-0000-0000-000000000000')
    assert.equal(output.body.accessToken, '[redacted]')
    assert.deepEqual(output.body.profile, {
      displayName: 'deterministic',
      password: '[redacted]',
    })
    assert.equal(output.started.mock.canStop, true)
    assert.equal(output.conflict.error, 'api-mock-already-running')
    assert.equal(output.logs.items.length, 1)
    assert.equal(output.logs.committedGenerationId, output.started.mock.committedGenerationId)
    assert.equal(output.logs.contractHash, output.started.mock.contractHash)
    assert.equal(output.logs.environmentId, 'local')
    const recoverScript = `
const path = require('path');
const fs = require('fs');
const repo = process.argv[1];
const root = process.env.ORCHESTRATOR_PROJECT_ROOT;
const apiMock = require(path.join(repo, 'orchestrator/site/server/api-mock.js'));
(async () => {
  const recovered = await apiMock.status();
  if (!recovered.ok || recovered.mock.state !== 'running') {
    throw new Error(JSON.stringify(recovered));
  }
  const stopped = await apiMock.stop({
    serverId: recovered.mock.serverId,
    expectedStateRevision: recovered.mock.stateRevision,
    idempotencyKey: 'api.mock.lifecycle.stop'
  });
  const final = await apiMock.status();
  const logFile = path.join(
    process.env.ORCHESTRATOR_CACHE_DIR,
    'api-contract', 'mock', 'instances', recovered.mock.serverId, 'requests.jsonl'
  );
  const poison = path.join(root, 'poisoned-mock-log.jsonl');
  fs.writeFileSync(poison, '{}\\n');
  fs.unlinkSync(logFile);
  fs.linkSync(poison, logFile);
  const poisonedLog = apiMock.logs({ serverId: recovered.mock.serverId });
  process.stdout.write(JSON.stringify({ recovered, stopped, final, poisonedLog }));
})().catch(async (error) => {
  try {
    const current = await apiMock.status();
    if (current && current.ok && current.mock && current.mock.state === 'running') {
      await apiMock.stop({
        serverId: current.mock.serverId,
        expectedStateRevision: current.mock.stateRevision,
        idempotencyKey: 'api.mock.lifecycle.cleanup'
      });
    }
  } catch {}
  process.stderr.write(String(error && error.stack || error));
  process.exitCode = 1;
});
`
    const recoveredResult = spawnSync(process.execPath, ['-e', recoverScript, REPO], {
      cwd: root,
      env: {
        ...process.env,
        ORCHESTRATOR_PROJECT_ROOT: root,
        ORCHESTRATOR_CACHE_DIR: join(root, 'orchestrator', '.cache'),
      },
      encoding: 'utf8',
      timeout: 15000,
      maxBuffer: 4 * 1024 * 1024,
    })
    assert.equal(recoveredResult.status, 0, recoveredResult.stderr + recoveredResult.stdout)
    const recovered = JSON.parse(recoveredResult.stdout)
    assert.equal(recovered.recovered.mock.serverId, output.started.mock.serverId)
    assert.equal(recovered.recovered.mock.state, 'running')
    assert.equal(recovered.stopped.mock.state, 'stopped')
    assert.equal(recovered.stopped.mock.canStop, false)
    assert.equal(recovered.final.mock.state, 'stopped')
    assert.equal(recovered.poisonedLog.error, 'api-mock-log-invalid')
    const index = JSON.parse(readFileSync(
      join(root, 'orchestrator', '.cache', 'api-contract', 'mock', 'index.json'),
      'utf8',
    ))
    assert.equal(index.instances.length, 1)
    assert.equal(index.instances[0].state, 'stopped')
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

test('Project to API frontend uses stable tabs and modular, text-only rendering contracts', () => {
  const panel = readFileSync(join(REPO, 'orchestrator', 'site', 'scripts', 'panels', 'api.js'), 'utf8')
  assert.match(panel, /TABS = \['overview', 'endpoints', 'changes', 'diagnostics'\]/)
  assert.match(panel, /generationKey !== next/)
  assert.match(panel, /hasOwnProperty\.call\(patch, 'entity'\)/)
  assert.match(panel, /var renderedTab = null/)
  assert.match(
    panel,
    /var background = options\.background === true && renderedTab === tab/,
  )
  assert.match(panel, /if \(!background\) \{\s*renderedTab = null;/)
  assert.match(
    panel,
    /!options\.force && background && renderSignature\[tab\] === signature/,
  )
  assert.match(panel, /renderer\.render\([\s\S]*renderedTab = tab;/)
  assert.match(
    panel,
    /var tabChanged = state\.tab !== previousTab;[\s\S]{0,160}render\(\{ background: !tabChanged, force: true \}\)/,
  )
  assert.match(
    panel,
    /if \(!renderGeneration\.commit\(version\)\) return;[\s\S]{0,320}var interaction = background \? captureInteraction\(\) : null;[\s\S]{0,160}renderer\.render/,
  )
  assert.match(panel, /if \(draftChanged\) replacement\.dispatchEvent\(new Event\('input'/)
  assert.doesNotMatch(panel, /selected\.size\s*>=/)
  assert.match(panel, /\.catch\(function \(error\) \{[\s\S]*renderedTab = null;/)
  for (const kind of [
    'request-body-added',
    'request-body-removed',
    'request-array-item-reference-changed',
    'response-array-item-reference-changed',
  ]) {
    assert.match(panel, new RegExp("'" + kind + "'"))
  }
  const directory = join(REPO, 'orchestrator', 'site', 'scripts', 'api')
  for (const name of [
    'overview.js', 'endpoints.js', 'changes.js', 'endpoint-detail.js',
    'task-selection.js', 'diagnostics.js', 'mock.js',
  ]) {
    const source = readFileSync(join(directory, name), 'utf8')
    assert.equal(source.includes('innerHTML'), false, name)
  }
  const endpoints = readFileSync(join(directory, 'endpoints.js'), 'utf8')
  assert.match(endpoints, /api\.page\.loadMore/)
  assert.match(endpoints, /api\.selection\.selectAll/)
  assert.match(endpoints, /ctx\.get\(loadUrl\(ctx\.state, cursor\)\)\.then\(collect\)/)
  assert.match(endpoints, /ctx\.addSources\(Array\.from\(sourceIds\)\)/)
  const endpointDetail = readFileSync(join(directory, 'endpoint-detail.js'), 'utf8')
  assert.match(endpointDetail, /api:mismatch|finding\.sourceId/)
  assert.match(endpointDetail, /change\.sourceId/)
  assert.match(endpointDetail, /examples\.request/)
  assert.match(endpointDetail, /api\.detail\.implementation/)
  assert.match(endpointDetail, /api\.detail\.changes/)
  assert.match(endpointDetail, /ctx\.driftFindingMessage/)
  assert.equal(endpointDetail.includes('text: finding.message'), false)
  assert.match(endpointDetail, /#archmap/)
  assert.match(endpointDetail, /#board\?task=/)
  assert.match(readFileSync(join(directory, 'overview.js'), 'utf8'), /api\.priority\.tasks/)
  const taskSelection = readFileSync(join(directory, 'task-selection.js'), 'utf8')
  assert.match(taskSelection, /#board\?task=/)
  assert.match(taskSelection, /preview\(ctx, 'package'\)/)
  assert.match(taskSelection, /preview\(ctx, 'hotfix'\)/)
  assert.match(taskSelection, /mode: mode/)
  assert.match(taskSelection, /held\.actions/)
  assert.match(taskSelection, /api-batch-bar__actions/)
  const http = readFileSync(
    join(REPO, 'orchestrator', 'site', 'server', 'http.js'), 'utf8',
  )
  const apiTaskActionsSource = readFileSync(
    join(
      REPO,
      'orchestrator',
      'site',
      'server',
      'api-task-actions.js',
    ),
    'utf8',
  )
  assert.equal(http.includes('contractTaskActionsMod'), false)
  assert.match(
    apiTaskActionsSource,
    /!current\.tasks \|\| !current\.tasks\.ok/,
  )
  assert.doesNotMatch(apiTaskActionsSource, /localeCompare/)
  assert.match(panel, /api\.limitation\./)
  assert.match(readFileSync(join(directory, 'diagnostics.js'), 'utf8'), /ctx\.limitationMessage/)
  assert.match(readFileSync(join(directory, 'diagnostics.js'), 'utf8'), /finding\.sourceId/)
  assert.match(readFileSync(join(directory, 'mock.js'), 'utf8'), /disabled: mismatch/)
  assert.match(
    readFileSync(join(REPO, 'orchestrator', 'site', 'server', 'api-changes.js'), 'utf8'),
    /filters\.severity === 'attention'[\s\S]{0,100}row\.reviewed/,
  )
})
