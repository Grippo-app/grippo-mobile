import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

import { dictionaryFor } from './i18n-test-helpers.mjs'

const site = join(dirname(fileURLToPath(import.meta.url)), '..')
const read = relative => readFileSync(join(site, relative), 'utf8')
const panel = read('scripts/panels/archmap.js')
const api = read('scripts/data/architecture-api.js')
const overview = read('scripts/architecture/overview.js')
const catalog = read('scripts/architecture/catalog.js')
const findings = read('scripts/architecture/findings.js')
const detail = read('scripts/architecture/node-detail.js')
const diff = read('scripts/architecture/diff.js')
const graph = read('scripts/architecture/graph.js')
const http = read('server/http.js')
const server = read('server/arch.js')
const css = read('styles/panels.css')
const locales = ['en', 'ru', 'uk'].map(dictionaryFor)

for (const module of [
  './architecture/overview.js',
  './architecture/catalog.js',
  './architecture/findings.js',
  './architecture/node-detail.js',
  './architecture/diff.js',
  './architecture/graph.js',
]) {
  assert.match(panel, new RegExp(module.replaceAll('.', '\\.').replaceAll('/', '\\/')))
}
assert.match(panel, /new URLSearchParams\(raw\)/)
for (const key of [
  'tab', 'view', 'search', 'platform', 'layer', 'ownership', 'severity',
  'findingType', 'confidence', 'changed', 'node',
]) {
  assert.match(panel, new RegExp(`(?:params\\.get\\('${key}'\\)|params\\.set\\('${key}'|state\\.${key})`))
}
assert.match(panel, /scrollY/)
assert.match(panel, /graphViewport/)
assert.match(panel, /contentRequestGeneration/)
assert.match(panel, /generation !== contentRequestGeneration/)
assert.match(panel, /jobPollGeneration/)
assert.match(panel, /generation !== jobPollGeneration/)
assert.match(panel, /beginJobPolling/)
assert.match(panel, /Date\.parse\(job\.startedAt\) > Date\.parse\(activeJob\.startedAt\)/)
assert.match(panel, /pendingFindingTasks/)
assert.match(panel, /allowGraph/)
assert.match(panel, /activeJob/)
assert.match(panel, /siteEvents\.on\('architecture-job'/)
assert.match(panel, /siteEvents\.on\('open',[\s\S]{0,160}fetchAll\(\)/)
assert.match(panel, /architecture-changed/)
assert.match(panel, /architecture-job/)
assert.match(panel, /captureInteraction/)
assert.match(panel, /restoreInteraction/)
assert.match(panel, /setSelectionRange\(snapshot\.start, snapshot\.end\)/)
assert.match(panel, /router\.current\(\) !== 'archmap'/)

assert.match(overview, /summary\.modules/)
assert.match(overview, /summary\.features/)
assert.match(overview, /summary\.screens/)
assert.match(overview, /summary\.dataSources/)
assert.match(overview, /summary\.databaseEntities/)
assert.match(overview, /findingsBySeverity/)
assert.match(overview, /analysis\.status === 'partial'/)
assert.match(overview, /topFindings/)
assert.match(overview, /unownedScreens/)
assert.doesNotMatch(overview, /copyPath/)

assert.match(catalog, /type: 'search'/)
assert.match(catalog, /clearSearch/)
assert.match(catalog, /findingType/)
assert.match(catalog, /confidence/)
assert.match(catalog, /changedLatestTask/)
assert.match(catalog, /changedAvailable/)
assert.match(catalog, /event\.key === 'ArrowRight'/)
assert.match(catalog, /data-architecture-control/)
assert.match(catalog, /clearTimeout\(searchTimer\)/)
assert.match(catalog, /replacement\.focus\(\)/)
assert.match(findings, /finding\.evidence/)
assert.match(findings, /finding\.confidence/)
assert.match(findings, /finding\.linkedTask/)
assert.match(findings, /taskCreationEnabled/)
assert.match(findings, /evidenceReason\(t, row\.reasonCode\)/)
assert.doesNotMatch(findings, /['"] — ['"] \+ row\.reasonCode/)

assert.match(detail, /aria-modal/)
assert.match(detail, /event\.key === 'Escape'/)
assert.match(detail, /event\.key !== 'Tab'/)
assert.match(detail, /previousFocus/)
assert.match(detail, /previousFocusKey/)
assert.match(detail, /previousFocus\.isConnected/)
assert.match(detail, /data-target="archmap"/)
assert.match(detail, /entityRequestGeneration/)
assert.match(detail, /relationRequestGeneration/)
assert.match(detail, /if \(!data \|\| !data\.present\)/)
assert.match(detail, /data\.structuralHash !== currentData\.structuralHash/)
assert.match(detail, /detailsUnavailable/)
assert.match(detail, /relationsUnavailable/)
assert.match(detail, /renderRelationRequestError/)
assert.match(detail, /typeof data !== 'object'/)
assert.match(detail, /params\[direction \+ 'Cursor'\]/)
assert.match(detail, /removedInLatestDiff/)
assert.match(detail, /linkedTasks/)
assert.match(detail, /relatedEntities/)
assert.match(detail, /onNavigate/)
assert.match(detail, /findingsTruncated/)
assert.match(detail, /linkedTasksTruncated/)
assert.match(detail, /archmap\.relation\./)
assert.doesNotMatch(detail, /row\.node\.name \+ ' · ' \+ row\.edge\.kind/)
assert.match(diff, /followedByChanges/)
assert.match(diff, /changeTotals/)
assert.match(diff, /diff\.truncated/)

assert.match(graph, /archmap\.graph\.zoomIn/)
assert.match(graph, /archmap\.graph\.zoomOut/)
assert.match(graph, /event\.key === 'ArrowLeft'/)
assert.match(graph, /role: 'region'/)
assert.match(graph, /architecture-graph__node--finding/)
assert.match(graph, /architecture-graph-fallback/)
assert.match(graph, /archmap\.graph\.textRelations/)
assert.match(graph, /archmap\.relation\./)
assert.match(graph, /pointercancel/)
assert.match(graph, /lostpointercapture/)
assert.match(server, /selected\.length > 150 \|\| edges\.length > 500/)
assert.match(server, /findingSeverity/)

assert.match(api, /\/api\/architecture\/overview/)
assert.match(api, /\/api\/architecture\/nodes/)
assert.match(api, /\/api\/architecture\/findings/)
assert.match(api, /\/api\/architecture\/graph/)
assert.match(api, /\/api\/architecture\/generate/)
assert.match(api, /\/api\/architecture\/tasks\/preview/)
assert.doesNotMatch(api, /\bcommand\b|\bshell\b/)
assert.match(http, /1024 \* 1024/)
assert.match(http, /architecture-response-too-large/)

assert.match(css, /@media \(max-width: 700px\)/)
assert.match(css, /\.architecture-drawer \{ width: 100vw; \}/)
assert.match(css, /\.architecture-graph__node--finding/)
assert.match(css, /\.architecture-overview-row--error/)

const requiredLocaleKeys = [
  'archmap.findingType.dependency-cycle',
  'archmap.findingType.forbidden-dependency',
  'archmap.findingType.orphan-module',
  'archmap.findingType.unused-repository',
  'archmap.findingType.screen-without-owner',
  'archmap.confidence.exact',
  'archmap.confidence.derived',
  'archmap.confidence.heuristic',
  'archmap.entityMissingInDiff',
  'archmap.findingMeta',
  'archmap.platform.unknown',
  'archmap.layer.unknown',
  'archmap.graph.textFallback',
  'archmap.graph.textRelations',
  'archmap.relation.depends-on',
  'archmap.relation.navigates-to',
  'archmap.evidenceReason.forbidden-dependency',
  'archmap.evidenceReason.module-dependency-cycle',
  'archmap.evidenceReason.module-has-no-incoming-relation',
  'archmap.evidenceReason.repository-has-no-proven-consumer',
  'archmap.evidenceReason.screen-has-no-owner',
  'archmap.evidenceReason.unknown',
]
for (const locale of locales) {
  for (const key of requiredLocaleKeys) {
    assert.equal(typeof locale[key], 'string', key)
  }
}

console.log('architecture-ui-contract.test.mjs: OK')
