// component-census.mjs — the demand-side coverage check for a task's screens.
// Matches each screen's component instances (.cache/figma/screens/<stem>/*.instances.json,
// written by the figma:screens session) against the published Design Component
// Inventory and the project-owned Component Mapping Registry, and classifies
// every distinct OWNING DESIGN IDENTITY (component-set node id, or the main
// component node id for a standalone component — display names stay labels):
//   MAPPED       — design identity present, one active mapping, every required
//                  implementation resolves in the published project analysis
//                  with a readable source file
//   INCOMPLETE   — active mapping exists but an implementation cannot be
//                  verified (no published analysis, adapter scope moved,
//                  target absent, source unreadable, external-only)
//   MISSING      — design identity present, no active mapping (the
//                  reuse-or-create pick; code candidates surfaced)
//   AMBIGUOUS    — the registry resolves this identity to more than one active
//                  mapping (a corrupt/duplicated registry needs human review)
//   UNSUPPORTED  — the identity is captured but unsupported by the inventory
//   RETIRED      — the only mapping for the identity is retired
//   SOURCE_STALE — the screens cache references an identity absent from the
//                  current inventory (stale pull or moved scope)
// Demand-side only: it never re-scans the project (the published analysis is
// the supply truth) and never calls Figma. Consumers: task-prep Step 5.5,
// orchestrator Step 1b, figma-spec-validator.
//
// Usage: node scripts/component-census.mjs <TASK_STEM>
//          [--screens-dir <dir>] [--code-root <dir>] [--out <file>]   (fixture overrides)
import { displayPath, exists, readJson, figmaPath, figmaScreensRoot, isDirectRun, ok, warnMsg, failMsg, info, summary, PROJECT_ROOT, parseCli, loadBindings, EXECUTION_ROOT, executionProductInputPath, EXECUTION_SCOPE, writeFigmaRuntimeFile } from './_util.mjs'
import { accessSync, constants, readdirSync, readFileSync, realpathSync, statSync } from 'node:fs'
import { basename, delimiter, isAbsolute, join, resolve } from 'node:path'
import { assertTaskStem, fileHash, sha256Text, writeReport } from './report-utils.mjs'
import { maskKotlinTrivia } from './extract-app-tokens.mjs'
import { loadDesignComponentInventory, loadComponentMappings, loadPublishedComponentAnalysis } from './lib/design-components.mjs'
import { compareText } from '../runtime/canonical-json.mjs'

const USAGE = 'usage: node scripts/component-census.mjs <TASK_STEM> [--screens-dir D] [--code-root D] [--out F] [fixture options]'

// ── Mapping-consult digest ───────────────────────────────────────────────────
// The census verdicts depend ONLY on the design/mapping/analysis rows its
// consulted identities actually touched — never on whole-file hashes (an
// unrelated mapping edit must not retro-stale a shipped census). The report
// pins a digest over each consulted design identity's projection: inventory
// presence, mapping state, and per-implementation resolvability including
// whether the declared source file is currently readable. evidence-bundle
// recomputes this digest against the LIVE truth — a change to a CONSULTED
// identity still blocks the final gate; an unrelated edit no longer does.
export const MAPPING_CONSULT_KEY = 'virtual:component-mappings-consulted'

function sourceReadableState(sourcePath) {
  const declared = String(sourcePath || '').trim()
  if (!declared) return 'absent'
  const path = isAbsolute(declared) ? resolve(declared) : resolve(EXECUTION_ROOT, declared)
  let stat
  try { stat = statSync(path) } catch { return 'missing' }
  if (!stat.isFile()) return 'not-file'
  try { accessSync(path, constants.R_OK) } catch { return 'unreadable' }
  return 'readable'
}

// Resolve one consulted set/main node id against the live truth into a
// deterministic projection. Shared by the census (verdicts) and
// evidence-bundle (recompute) so the digest has exactly one meaning.
function projectConsultedIdentity(setId, truth) {
  const inventory = truth.inventory
  const component = inventory
    ? inventory.components.find((row) => row.providerIdentity.nodeId === setId ||
        row.variants.some((variant) => variant.nodeId === setId))
    : null
  const unsupported = !component && inventory
    ? inventory.unsupportedComponents.find((row) => row.providerIdentity.nodeId === setId)
    : null
  const designComponentId = component ? component.designComponentId : unsupported ? unsupported.designComponentId : null
  const mappings = designComponentId
    ? truth.registry.mappings.filter((mapping) => mapping.designComponentId === designComponentId)
    : []
  const active = mappings.filter((mapping) => mapping.state === 'active')
  const projection = {
    setId,
    designComponentId,
    designPresent: !!component,
    designUnsupported: !!unsupported,
    scopeMatches: !designComponentId || !truth.registry.designScopeId || !inventory ||
      truth.registry.designScopeId === inventory.scopeId,
    activeMappings: active.map((mapping) => ({
      mappingId: mapping.mappingId,
      implementations: mapping.implementations.map((implementation) => {
        const analysisRow = truth.analysis.present
          ? truth.analysis.index.adapters.find((row) => row.adapterId === implementation.adapterId)
          : null
        const inventoryForAdapter = truth.analysis.present ? truth.analysis.inventories.get(implementation.adapterId) : null
        const targets = implementation.projectComponentIds.map((projectComponentId) => {
          const projectComponent = inventoryForAdapter
            ? inventoryForAdapter.components.find((row) => row.projectComponentId === projectComponentId)
            : null
          return {
            projectComponentId,
            present: !!projectComponent,
            sourcePath: projectComponent ? projectComponent.source.path : null,
            sourceReadable: projectComponent ? sourceReadableState(projectComponent.source.path) === 'readable' : false
          }
        })
        return {
          adapterId: implementation.adapterId,
          platform: implementation.platform,
          relation: implementation.relation,
          required: implementation.required,
          analyzed: !!analysisRow,
          scopeCurrent: !!analysisRow && analysisRow.scopeFingerprint === implementation.projectScopeFingerprint,
          targets
        }
      })
    })),
    retiredMappingIds: mappings.filter((mapping) => mapping.state === 'retired').map((mapping) => mapping.mappingId).sort()
  }
  return projection
}

export function computeMappingConsultDigest(setIds, truth) {
  const projections = [...new Set(setIds.map(String))].sort()
    .map((setId) => projectConsultedIdentity(setId, truth))
  return sha256Text(JSON.stringify({ version: 2, identities: projections }))
}

// --- Reuse-candidate scan (the code side of MISSING) ---------------------------------------
// MISSING only means "no confirmed mapping" — the composable may still exist in
// code, unmapped; auto-splitting then breeds a near-duplicate component. Scan
// the product's Kotlin sources for top-level non-private @Composable functions
// and surface EXACT normalized-name matches on the MISSING row so task-prep can
// ask "reuse <file> or create?" instead of splitting blind. Conservative on
// purpose: exact normalized equality only, no fuzzy matching.
const SCAN_SKIP_DIRS = new Set(['.git', '.gradle', '.idea', 'build', 'node_modules', 'orchestrator'])
const isTestDir = (name) => name === 'test' || name === 'tests' || /Test$/.test(name)
const normComponentName = (s) => String(s || '').toLowerCase().replace(/[^a-z0-9]/g, '')
const FUN_DECL_RE = /(?:^|\s)fun\s+([A-Z][A-Za-z0-9_]*)\s*\(/

function collectKotlinFiles(root, out, visited = new Set()) {
  let physicalRoot
  try { physicalRoot = realpathSync(root) } catch { physicalRoot = resolve(root) }
  if (visited.has(physicalRoot)) return out
  visited.add(physicalRoot)
  let entries = []
  try { entries = readdirSync(root, { withFileTypes: true }) } catch { return out }
  for (const d of entries) {
    if (SCAN_SKIP_DIRS.has(d.name) || isTestDir(d.name)) continue
    const path = join(root, d.name)
    let isDirectory = d.isDirectory()
    let isFile = d.isFile()
    if (d.isSymbolicLink()) {
      if (EXECUTION_SCOPE) continue
      try { const target = statSync(path); isDirectory = target.isDirectory(); isFile = target.isFile() } catch { continue }
    }
    if (isDirectory) collectKotlinFiles(path, out, visited)
    else if (isFile && /\.kt$/.test(d.name)) out.push(path)
  }
  return out
}

function scanComposableIndex(roots, { warn = warnMsg } = {}) {
  const index = new Map()   // normalized component name -> [{ name, file, line }]
  const seenFiles = new Set()
  for (const root of roots) {
    if (!exists(root)) { warn(`code-candidate scan root missing: ${root} — reuse candidates unavailable from it`); continue }
    if (isTestDir(basename(resolve(root)))) { warn(`code-candidate scan root is a test source set: ${root} — ignored`); continue }
    for (const file of collectKotlinFiles(root, [])) {
      let resolvedFile
      try { resolvedFile = realpathSync(file) } catch { resolvedFile = resolve(file) }
      if (seenFiles.has(resolvedFile)) continue
      seenFiles.add(resolvedFile)
      let text
      try { text = readFileSync(file, 'utf8') } catch { continue }
      if (!text.includes('@Composable')) continue
      // maskKotlinTrivia blanks comments/strings (newline-preserving), so an `@Composable`
      // inside a comment or string literal never counts, and line numbers stay correct.
      const lines = maskKotlinTrivia(text).split('\n')
      for (let i = 0; i < lines.length; i++) {
        const m = FUN_DECL_RE.exec(lines[i])
        if (!m) continue
        let start = Math.max(0, i - 4)
        for (let j = i - 1; j >= start; j--) { if (FUN_DECL_RE.test(lines[j])) { start = j + 1; break } }
        const win = lines.slice(start, i + 1).join('\n')
        if (!win.includes('@Composable') || /\bprivate\b/.test(win)) continue
        const key = normComponentName(m[1])
        const rows = index.get(key) || []
        rows.push({ name: m[1], file: displayPath(file), line: i + 1 })
        index.set(key, rows)
      }
    }
  }
  return index
}

;(async function main() {
  if (!isDirectRun(import.meta.url)) return

  let cli
  try {
    const valueFlags = ['--screens-dir', '--code-root', '--out', '--fixture-component-inventory',
      '--fixture-component-mappings', '--fixture-component-analysis-index', '--fixture-component-analysis-dir']
    cli = parseCli({ allowedFlags: valueFlags, valueFlags, usage: USAGE })
  } catch (e) {
    console.error(e.message)
    process.exit(1)
  }
  let stem
  try { stem = assertTaskStem(cli.positional[0] || '') } catch { failMsg(USAGE); process.exit(1) }
  if (cli.positional.length !== 1) { failMsg(USAGE); process.exit(1) }

  const screensRoot = cli.value('--screens-dir') || figmaScreensRoot()
  const screensDir = join(screensRoot, stem)
  if (!exists(screensDir)) { failMsg(`screens cache missing: ${screensDir} — pull it via the task card's "Pull Figma screens" button (figma:screens session)`); process.exit(1) }

  // Truth inputs: published design inventory, project-owned mapping registry,
  // last published project analysis. Absence is a state; malformed throws.
  const fixtureOptions = {
    fixtureInventoryFile: cli.value('--fixture-component-inventory') || undefined,
    fixtureMappingsFile: cli.value('--fixture-component-mappings') || undefined,
    fixtureAnalysisIndexFile: cli.value('--fixture-component-analysis-index') || undefined,
    fixtureAnalysisDirectory: cli.value('--fixture-component-analysis-dir') || undefined
  }
  let design
  try { design = loadDesignComponentInventory(fixtureOptions) } catch (e) { failMsg(e.message); process.exit(1) }
  let mappingsRead
  try { mappingsRead = loadComponentMappings(design.present ? design.inventory.scopeId : '', fixtureOptions) } catch (e) { failMsg(e.message); process.exit(1) }
  let analysis
  try { analysis = loadPublishedComponentAnalysis(fixtureOptions) } catch (e) { failMsg(e.message); process.exit(1) }
  const truth = {
    inventory: design.present ? design.inventory : null,
    registry: mappingsRead.registry,
    analysis
  }
  if (!design.present) info('no design component inventory is synced — identities will read SOURCE_STALE until a components sync publishes one')
  if (!analysis.present) info('no published project component analysis — mapped implementations cannot be verified (INCOMPLETE)')

  // Kotlin roots for the reuse-candidate scan: --code-root (fixtures/tests) >
  // FIGMA_CENSUS_CODE_ROOTS (path-delimited) > the product repo root.
  const codeRootArg = cli.value('--code-root')
  const codeRoots = codeRootArg
    ? [codeRootArg]
    : (process.env.FIGMA_CENSUS_CODE_ROOTS
      ? process.env.FIGMA_CENSUS_CODE_ROOTS.split(delimiter).filter(Boolean)
        .map((value) => executionProductInputPath(value, 'FIGMA_CENSUS_CODE_ROOTS'))
      : [EXECUTION_ROOT])
  let composableIndex = null   // built lazily on the first MISSING row

  // Aggregate instances per OWNING DESIGN IDENTITY across all screens. Two
  // sets sharing one display name stay two rows — names are labels.
  const bySetId = new Map()   // setId -> { setId, names:Set, screens:Set, instances:n, sample, samples:[] }
  const screens = {}
  const files = readdirSync(screensDir).filter((f) => f.endsWith('.instances.json')).sort()
  if (files.length === 0) { failMsg(`no *.instances.json under ${screensDir} — the figma:screens session has not written instance lists`); process.exit(1) }

  for (const f of files) {
    const screen = f.replace(/\.instances\.json$/, '')
    let list
    try { list = readJson(join(screensDir, f)) } catch (e) { failMsg(`${f} unreadable (${e.message})`); process.exit(1) }
    if (!Array.isArray(list)) { failMsg(`${f} is not a JSON array of instances`); process.exit(1) }
    screens[screen] = []
    for (const [instanceIndex, inst] of list.entries()) {
      if (!inst || typeof inst !== 'object' || Array.isArray(inst) ||
          typeof inst.name !== 'string' || !inst.name || typeof inst.componentSetName !== 'string' || !inst.componentSetName ||
          typeof inst.figmaNodeId !== 'string' || !/^[0-9]+:[0-9]+$/.test(inst.figmaNodeId)) {
        failMsg(`${f}[${instanceIndex}] must carry name, componentSetName, and the owning component-set figmaNodeId`)
        process.exit(1)
      }
      const setId = String(inst.figmaNodeId)
      screens[screen].push(setId)
      const slot = bySetId.get(setId) || { setId, names: new Set(), screens: new Set(), instances: 0, sample: inst, samples: [] }
      slot.names.add(String(inst.componentSetName).trim())
      slot.screens.add(screen)
      slot.instances++
      if (slot.samples.length < 64) slot.samples.push(inst)
      bySetId.set(setId, slot)
    }
  }

  const report = { version: 2, screens, components: [], missing: [], incomplete: [], ambiguous: [], unsupported: [], retired: [], sourceStale: [], reuseCandidates: [] }
  const issues = []

  for (const slot of [...bySetId.values()].sort((a, b) => compareText(a.setId, b.setId))) {
    const projection = projectConsultedIdentity(slot.setId, truth)
    const setName = [...slot.names].sort()[0]
    const sourceNodeUrl = slot.sample && slot.sample.nodeUrl ? String(slot.sample.nodeUrl) : null
    const row = {
      setNodeId: slot.setId,
      setName,
      designComponentId: projection.designComponentId,
      screens: [...slot.screens].sort(),
      instances: slot.instances,
      sourceNodeUrl
    }
    const designComponent = truth.inventory && projection.designPresent
      ? truth.inventory.components.find((candidate) => candidate.designComponentId === projection.designComponentId)
      : null

    if (!projection.designComponentId) {
      row.status = 'SOURCE_STALE'
      row.detail = truth.inventory
        ? 'this identity is absent from the current design component inventory — re-pull the screens or re-sync components'
        : 'no design component inventory is synced'
      report.sourceStale.push(slot.setId)
      issues.push({ severity: 'REVIEW_REQUIRED', issueKind: 'COMPONENT_SOURCE_STALE', message: `${setName} (${slot.setId}): ${row.detail}`, component: setName, setNodeId: slot.setId, sourceNodeUrl })
      warnMsg(`SOURCE_STALE ${setName} (${slot.setId}) — ${row.detail}`)
    } else if (projection.designUnsupported) {
      row.status = 'UNSUPPORTED'
      report.unsupported.push(slot.setId)
      issues.push({ severity: 'REVIEW_REQUIRED', issueKind: 'COMPONENT_UNSUPPORTED', message: `${setName} is captured but unsupported by the design inventory`, component: setName, setNodeId: slot.setId, designComponentId: projection.designComponentId })
      warnMsg(`UNSUPPORTED ${setName} — captured but unsupported by the design inventory`)
    } else if (!projection.scopeMatches) {
      row.status = 'INCOMPLETE'
      row.detail = 'the mapping registry is bound to another design scope; run onboarding before trusting mappings'
      report.incomplete.push(slot.setId)
      issues.push({ severity: 'REVIEW_REQUIRED', issueKind: 'COMPONENT_SCOPE_CHANGED', message: `${setName}: ${row.detail}`, component: setName, setNodeId: slot.setId })
      warnMsg(`INCOMPLETE  ${setName} — ${row.detail}`)
    } else if (projection.activeMappings.length > 1) {
      row.status = 'AMBIGUOUS'
      row.candidates = projection.activeMappings.map((mapping) => mapping.mappingId).sort()
      report.ambiguous.push(slot.setId)
      issues.push({ severity: 'REVIEW_REQUIRED', issueKind: 'COMPONENT_AMBIGUOUS', message: `${setName} resolves to ${projection.activeMappings.length} active mappings (${row.candidates.join(', ')}) — the registry needs review`, component: setName, setNodeId: slot.setId, candidates: row.candidates })
      warnMsg(`AMBIGUOUS   ${setName} — ${projection.activeMappings.length} active mappings claim one design identity: ${row.candidates.join(', ')}`)
    } else if (projection.activeMappings.length === 1) {
      const mapping = projection.activeMappings[0]
      row.mappingId = mapping.mappingId
      const verifiable = mapping.implementations.filter((implementation) => implementation.relation !== 'external')
      const resolved = verifiable.filter((implementation) =>
        implementation.analyzed && implementation.scopeCurrent &&
        implementation.targets.length > 0 &&
        implementation.targets.every((target) => target.present && target.sourceReadable))
      if (verifiable.length && resolved.length === verifiable.length) {
        row.status = 'MAPPED'
        row.implementations = resolved.map((implementation) => ({
          adapterId: implementation.adapterId,
          platform: implementation.platform,
          projectComponentId: implementation.targets[0].projectComponentId,
          sourcePath: implementation.targets[0].sourcePath
        }))
        ok(`MAPPED      ${setName} → ${row.implementations.map((implementation) => implementation.projectComponentId).join(', ')}`)
      } else {
        row.status = 'INCOMPLETE'
        const reasons = []
        if (!verifiable.length) reasons.push('external-only implementation')
        for (const implementation of verifiable) {
          if (!implementation.analyzed) reasons.push(`${implementation.adapterId}: no published analysis`)
          else if (!implementation.scopeCurrent) reasons.push(`${implementation.adapterId}: adapter scope moved`)
          else if (!implementation.targets.every((target) => target.present)) reasons.push(`${implementation.adapterId}: implementation absent from analysis`)
          else if (!implementation.targets.every((target) => target.sourceReadable)) reasons.push(`${implementation.adapterId}: source unreadable`)
        }
        row.detail = reasons.slice(0, 4).join('; ') || 'implementation cannot be verified'
        report.incomplete.push(slot.setId)
        issues.push({ severity: 'REVIEW_REQUIRED', issueKind: 'COMPONENT_INCOMPLETE', message: `${setName}: ${row.detail}`, component: setName, setNodeId: slot.setId, mappingId: mapping.mappingId })
        warnMsg(`INCOMPLETE  ${setName} — ${row.detail}`)
      }
    } else if (projection.retiredMappingIds.length) {
      row.status = 'RETIRED'
      row.retiredMappingIds = projection.retiredMappingIds
      report.retired.push(slot.setId)
      issues.push({ severity: 'REVIEW_REQUIRED', issueKind: 'COMPONENT_RETIRED', message: `${setName} is used on screens but its mapping is retired (${projection.retiredMappingIds.join(', ')})`, component: setName, setNodeId: slot.setId })
      warnMsg(`RETIRED     ${setName} — screens still use a retired mapping (${projection.retiredMappingIds.join(', ')})`)
    } else {
      row.status = 'MISSING'
      report.missing.push(slot.setId)
      if (composableIndex === null) composableIndex = scanComposableIndex(codeRoots)
      const codeCands = composableIndex.get(normComponentName(setName)) || []
      if (codeCands.length) {
        row.codeCandidates = codeCands.slice(0, 3).map(({ name, file, line }) => ({ name, file, line }))
        report.reuseCandidates.push(slot.setId)
        issues.push({ severity: 'REVIEW_REQUIRED', issueKind: 'COMPONENT_CODE_CANDIDATE', message: `${setName} has no confirmed mapping but ${codeCands.length} unmapped @Composable name-match(es) exist in code — reuse-or-create is a human pick, not an auto-split`, component: setName, setNodeId: slot.setId, codeCandidates: row.codeCandidates, sourceNodeUrl })
        warnMsg(`MISSING?    ${setName} — unmapped code candidate(s): ${row.codeCandidates.map((c) => `${c.name} (${c.file}:${c.line})`).join(', ')} — ask reuse-vs-create before splitting`)
      }
      issues.push({ severity: 'REVIEW_REQUIRED', issueKind: 'COMPONENT_MISSING', message: `${setName} has no confirmed mapping`, component: setName, setNodeId: slot.setId, designComponentId: projection.designComponentId, sourceNodeUrl })
      warnMsg(`MISSING     ${setName} (${slot.instances}× on ${row.screens.join(', ')}) — no confirmed mapping${sourceNodeUrl ? ` [${sourceNodeUrl}]` : ''}`)
    }

    // Instance assignment verification (§19.5): each sampled variantProps axis
    // must name a declared variant property and one of its options. Advisory —
    // a mismatch is a review issue, never an invented status.
    if (designComponent && designComponent.kind === 'component-set') {
      const propertiesByName = new Map(designComponent.properties
        .filter((property) => property.type === 'variant')
        .map((property) => [property.name, property]))
      const badAssignments = new Set()
      for (const sampleInstance of slot.samples) {
        const variantProps = sampleInstance && typeof sampleInstance.variantProps === 'object' && !Array.isArray(sampleInstance.variantProps)
          ? sampleInstance.variantProps : null
        if (!variantProps) continue
        for (const [axis, value] of Object.entries(variantProps)) {
          const property = propertiesByName.get(axis)
          if (!property) { badAssignments.add(`${axis} (unknown property)`); continue }
          if (!property.options.includes(String(value))) badAssignments.add(`${axis}=${value} (outside declared options)`)
        }
      }
      if (badAssignments.size) {
        const detail = [...badAssignments].sort().slice(0, 6).join(', ')
        issues.push({ severity: 'REVIEW_REQUIRED', issueKind: 'COMPONENT_INSTANCE_ASSIGNMENT_INVALID', message: `${setName}: instance assignments not declared by the design component: ${detail}`, component: setName, setNodeId: slot.setId })
        warnMsg(`ASSIGNMENTS ${setName} — instance uses undeclared variant values: ${detail}`)
      }
    }
    report.components.push(row)
  }

  const OUT = cli.value('--out') || figmaPath('reports', `census-${stem}.json`)
  const hasWarnIssue = issues.some((i) => i && i.severity === 'WARN')
  const overall = (report.missing.length || report.incomplete.length || report.ambiguous.length ||
    report.sourceStale.length || report.retired.length || report.unsupported.length)
    ? 'INCOMPLETE' : hasWarnIssue ? 'WARN' : 'PASS'
  const instanceHashes = Object.fromEntries(files.map((f) => {
    const p = join(screensDir, f)
    return [p, fileHash(p)]
  }))
  const consultedSetIds = [...bySetId.keys()].sort()
  report.mappingConsult = {
    version: 2,
    setIds: consultedSetIds,
    digest: computeMappingConsultDigest(consultedSetIds, truth)
  }
  const inputHashes = Object.assign({}, instanceHashes)
  inputHashes[MAPPING_CONSULT_KEY] = report.mappingConsult.digest

  // Upsert the census's identity↔implementation resolution into the task
  // bindings manifest (screens/<stem>/bindings.json) when the pull seeded it.
  // IDENTITY is designComponentId (with the set node id as its provider
  // anchor); display names are labels, so same-named sets stay separate rows.
  // Bindings are a derived cache of the registry truth — MAPPED rows overwrite
  // their prior row; builder-owned screen fields are never touched.
  const bindingsPath = join(screensDir, 'bindings.json')
  if (exists(bindingsPath)) {
    let bindings
    try { bindings = loadBindings(stem) }
    catch (e) { failMsg(e.message); process.exit(1) }
    const raw = JSON.parse(readFileSync(bindingsPath, 'utf8'))
    const byDesignId = new Map((bindings.components || []).map((component) => [component.designComponentId, component]))
    let changed = false
    for (const row of report.components) {
      if (!row || row.status !== 'MAPPED' || !row.designComponentId) continue
      const entry = {
        designComponentId: row.designComponentId,
        setNodeId: row.setNodeId,
        setName: row.setName,
        mappingId: row.mappingId,
        implementations: row.implementations.map((implementation) => ({
          adapterId: implementation.adapterId,
          platform: implementation.platform,
          projectComponentId: implementation.projectComponentId,
          ...(implementation.sourcePath ? { sourcePath: implementation.sourcePath } : {})
        }))
      }
      const prev = byDesignId.get(entry.designComponentId)
      if (!prev || JSON.stringify(prev) !== JSON.stringify(entry)) { byDesignId.set(entry.designComponentId, entry); changed = true }
    }
    if (changed) {
      raw.components = [...byDesignId.values()].sort((a, b) => compareText(a.designComponentId, b.designComponentId))
      writeFigmaRuntimeFile(bindingsPath, JSON.stringify(raw, null, 2) + '\n')
      info(`bindings: ${raw.components.length} component binding(s) upserted -> ${displayPath(bindingsPath)}`)
    }
  }

  const { reportPath } = writeReport({
    name: 'census',
    taskStem: stem,
    mode: 'advisory',
    inputs: {
      screensDir: displayPath(screensDir),
      designGenerationId: design.present ? design.generationId : null,
      mappingRevision: truth.registry.revision,
      analysisPresent: analysis.present,
      codeRoots: composableIndex === null ? null : codeRoots.map((r) => displayPath(r))
    },
    inputHashes,
    overall,
    issues,
    extra: report,
    outPath: OUT,
  })
  info(`report: ${OUT}`)
  info(`split inputs — missing: ${report.missing.length}, incomplete: ${report.incomplete.length}, ambiguous: ${report.ambiguous.length}, source-stale: ${report.sourceStale.length}, retired: ${report.retired.length}, unsupported: ${report.unsupported.length}, reuse-candidates: ${report.reuseCandidates.length}`)
  if (report.reuseCandidates.length) info(`  MISSING with unmapped code candidates (reuse-or-create pick): ${report.reuseCandidates.join(', ')}`)

  // The census is information, not a gate: unresolved identities are the
  // CALLER's branch (task-prep splits / asks), so they exit 0. Only unreadable
  // inputs (handled above) are script failures.
  info(`report envelope: ${reportPath}`)
  summary('figma:census')
  process.exit(0)
})().catch((error) => {
  failMsg(error && error.message ? error.message : String(error))
  process.exit(1)
})
