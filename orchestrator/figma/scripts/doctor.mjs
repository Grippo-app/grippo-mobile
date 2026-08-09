// figma:doctor — health check for the sidecar. Never calls Figma. PASS/WARN/FAIL, exit 0/1.
import { ok, warnMsg, failMsg, info, summary, exists, figmaPath, isDirectRun, readConfig, readJson, FIGMA_CACHE_ROOT, PROJECT_ROOT } from './_util.mjs'
import { pathToFileURL } from 'node:url'
import { mkdirSync, writeFileSync, unlinkSync, readFileSync, readdirSync } from 'node:fs'
import { execFileSync } from 'node:child_process'
import { join, sep } from 'node:path'
import { createHash } from 'node:crypto'

const HOOKS_PATH_EXPECTED = 'orchestrator/skills/checks/hooks'

export function unpinnedScriptManifestPaths(
  manifestFiles,
  {
    scriptsRoot = figmaPath('scripts'),
    testInfrastructureRoot = join(PROJECT_ROOT, 'orchestrator', 'tests')
  } = {}
) {
  const requiredPaths = []
  for (const entry of readdirSync(scriptsRoot, { recursive: true })) {
    const relative = String(entry).split(sep).join('/')
    if (!/\.(mjs|cjs)$/.test(relative) || relative.startsWith('_index/')) continue
    requiredPaths.push(`orchestrator/figma/scripts/${relative}`)
  }
  for (const entry of readdirSync(testInfrastructureRoot, { withFileTypes: true })) {
    if (entry.isFile() && entry.name.endsWith('.mjs')) {
      requiredPaths.push(`orchestrator/tests/${entry.name}`)
    }
  }
  return requiredPaths
    .filter((relative) => !Object.hasOwn(manifestFiles, relative))
    .sort()
}

// W2 — a figmaEnabled PRODUCT must actually RUN the mechanical screenshot-gate net, or a bare
// `git mv` (or the auto-commit daemon) lands an uncompared UI task in done/ with nothing to
// catch it (the "vendored product, 183 done tasks, zero enforcement" failure). Two layers must
// be wired: the pre-commit hook (verify-done) via `core.hooksPath`, and the CI workflow.
//
// `strict` controls whether an UNWIRED product is a FAIL or a WARN. It defaults to a WARN in an
// ordinary `figma:doctor` run because bootstrap runs doctor at launch Step 6.5 BEFORE
// `install-skills.sh` wires the hooks at Step 14 — a hard FAIL there would break the bootstrap.
// The post-install verification (`FIGMA_STRICT_WIRING=1 figma:doctor`, launch Step 14) passes
// `strict:true` so a product that finished bootstrapping without a live net fails loudly.
// Independently of this report, the site runner enforces the LOCAL half at RUN TIME
// (sessions.runGateError() — server/sessions.js): while core.hooksPath is unwired in a
// figmaEnabled product, `run` sessions are refused outright, and the run-loop's Step 0
// bootstrap check BLOCKs the same way for site-less runs. PURE (no git/fs) so it is
// unit-testable; the caller gathers the real inputs. Returns
// [{ level: 'ok'|'warn'|'fail', msg }].
export function enforcementWiringFindings({ inGit, hooksPath, strict = true }) {
  const unwired = strict ? 'fail' : 'warn'
  const hint = strict ? '' : ' (advisory in a plain doctor run — enforced strictly by launch Step 14 / `FIGMA_STRICT_WIRING=1`; the site runner + run-loop Step 0 refuse `run` tasks while core.hooksPath is unwired)'
  const out = []
  if (!inGit) {
    out.push({ level: 'warn', msg: 'not a git work-tree (or git unavailable) — cannot verify enforcement wiring (core.hooksPath); after `git init` run orchestrator/skills/install-skills.sh' })
    return out
  }
  out.push(hooksPath === HOOKS_PATH_EXPECTED
    ? { level: 'ok', msg: 'core.hooksPath wired (pre-commit verify-done gate is live)' }
    : { level: unwired, msg: `core.hooksPath is ${hooksPath ? `'${hooksPath}'` : 'UNSET'}, expected '${HOOKS_PATH_EXPECTED}' — the LOCAL screenshot-gate net (pre-commit verify-done) is INACTIVE; a bare \`git mv\` could ship an uncompared UI task. Wire it: \`git config core.hooksPath ${HOOKS_PATH_EXPECTED}\` (or run orchestrator/skills/install-skills.sh)${hint}` })
  return out
}

export function accountSnapshotIssue(value) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return 'not a JSON object'
  const text = (field) => typeof value[field] === 'string' ? value[field].trim() : ''
  if (!text('handle') && !text('email')) return 'handle/email are blank'
  const checkedAt = text('checkedAt')
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,3})?(?:Z|[+-]\d{2}:\d{2})$/.test(checkedAt) || !Number.isFinite(Date.parse(checkedAt))) {
    return 'checkedAt is not a timezone-bearing ISO 8601 timestamp'
  }
  if (!/^[a-f0-9]{32}$/.test(text('verificationNonce'))) return 'verificationNonce is missing or invalid'
  return null
}

;(async function main() {
  if (!isDirectRun(import.meta.url)) return

  // Toolchain — the engines.node floor is the source of truth (the scripts use modern
  // syntax; a below-floor node fails deep inside a run, not here). Read it from package.json
  // rather than hardcoding so doctor never drifts from the declared engine.
  const major = Number(process.versions.node.split('.')[0])
  let engineFloor = null
  try {
    const declared = readJson(figmaPath('package.json'))?.engines?.node
    const match = /^>=(\d+)$/.exec(String(declared || ''))
    if (!match) throw new Error('engines.node must use the exact >=<major> contract')
    engineFloor = Number(match[1])
    ok('package.json present and valid')
  } catch (error) {
    failMsg(`package.json missing or invalid (${error.message})`)
  }
  if (engineFloor !== null) {
    major >= engineFloor
      ? ok(`Node ${process.versions.node} (>=${engineFloor})`)
      : failMsg(`Node ${process.versions.node} below engines.node floor >=${engineFloor} — the scripts use modern syntax; install Node ${engineFloor}+ (e.g. \`nvm install ${engineFloor}\`)`)
  }

  // Runtime deps — the fresh-clone-with-no-node_modules failure. These are resolved lazily
  // deep in the pipeline (compare-screenshots does `await import('jimp')`; the schema gate
  // does `await import('ajv')`), so a missing install dead-ends a run far from the cause.
  // Doctor names the exact remedy the run-loop npm-ci preflight also guards. FAIL hard.
  try {
    const { Jimp } = await import('jimp')
    if (typeof Jimp?.read === 'function') ok('jimp importable (screenshot comparator)')
    else failMsg('jimp imported but Jimp.read missing — reinstall the root workspace with `npm ci`')
  } catch (e) {
    failMsg(`jimp unavailable (${e.message}) — the screenshot comparator hard-depends on it; run root \`npm ci\``)
  }
  try {
    const { default: Ajv } = await import('ajv')
    if (typeof Ajv === 'function') ok('ajv importable (schema validation gate)')
    else failMsg('ajv imported but not constructable — reinstall the root workspace with `npm ci`')
  } catch (e) {
    failMsg(`ajv unavailable (${e.message}) — the schema validation gate hard-depends on it; run root \`npm ci\``)
  }

  // Consolidated cache root must be creatable + writable — the pipeline writes reports/,
  // screens/, artifacts/ there (FIGMA_CACHE_ROOT override honored). A missing-parent or
  // read-only cache dir fails EVERY run. Probe by creating + removing a temp file; leave no junk.
  try {
    mkdirSync(FIGMA_CACHE_ROOT, { recursive: true })
    const probe = join(FIGMA_CACHE_ROOT, `.doctor-write-probe-${process.pid}`)
    writeFileSync(probe, 'ok')
    unlinkSync(probe)
    ok(`cache root writable (${FIGMA_CACHE_ROOT})`)
  } catch (e) {
    failMsg(`cache root not writable (${FIGMA_CACHE_ROOT}): ${e.message} — the pipeline writes reports/screens/artifacts there; fix the path or its permissions (or set FIGMA_CACHE_ROOT to a writable dir)`)
  }

  // Committed structure (reports/ now lives under orchestrator/.cache/figma/reports — runtime-created, not committed)
  for (const d of ['scripts', 'tokens', 'manifests', 'token-schemas', 'skill']) {
    exists(figmaPath(d)) ? ok(`${d}/ present`) : failMsg(`${d}/ missing`)
  }

  // Script-hash manifest (E3) — the gate SCRIPTS are the enforcement; nothing else detects a
  // silent edit/corruption of design-parser.cjs / ship-done.mjs / verify-done.mjs / etc. Assert
  // every pin in scripts/_index/script-manifest.json exists + matches on disk, AND that no
  // .mjs/.cjs under scripts/ (excluding only generated _index/) or top-level .mjs test
  // infrastructure module is UNPINNED ("added trusted code, forgot to pin it"). Committed
  // template files always exist at bootstrap Step 6.5, so a hard FAIL here has no ordering
  // hazard (unlike the enforcement-wiring check).
  {
    const manifestPath = figmaPath('scripts', '_index', 'script-manifest.json')
    const manifest = exists(manifestPath) ? readJson(manifestPath) : null
    if (!manifest || !manifest.files || typeof manifest.files !== 'object') {
      failMsg('scripts/_index/script-manifest.json missing or malformed — regenerate: python3 orchestrator/figma/scripts/_index/_generate_script_manifest.py')
    } else {
      const shaOf = (p) => createHash('sha256').update(readFileSync(p)).digest('hex')
      let pinned = 0, drift = 0
      for (const [rel, want] of Object.entries(manifest.files)) {
        const abs = join(PROJECT_ROOT, rel)
        if (!exists(abs)) { failMsg(`script-manifest pins ${rel} but the file is MISSING — restore it, or regenerate the manifest if the removal was intentional`); drift++; continue }
        if (shaOf(abs) !== want) { failMsg(`script-manifest sha256 MISMATCH for ${rel} — the pinned gate script was edited/corrupted; if intentional, regenerate: python3 orchestrator/figma/scripts/_index/_generate_script_manifest.py`); drift++ }
        else pinned++
      }
      const unpinnedPaths = unpinnedScriptManifestPaths(manifest.files)
      for (const relative of unpinnedPaths) {
        failMsg(`${relative} is NOT pinned in script-manifest.json — regenerate: python3 orchestrator/figma/scripts/_index/_generate_script_manifest.py`)
      }
      if (drift === 0 && unpinnedPaths.length === 0) {
        ok(`script-manifest.json: ${pinned} trusted file(s) pinned + content-verified (no drift, no unpinned)`)
      }
    }
  }

  // Config gate
  const enabled = readConfig('figmaEnabled')
  if (enabled === undefined) warnMsg('figmaEnabled not found in project-config.md')
  else if (enabled === 'true') ok('figmaEnabled: true')
  else info(`figmaEnabled: ${enabled} (Figma pipeline is opt-in; not enabled — nothing to run)`)

  const lib = readConfig('figmaLibraryUrl')
  if (!lib || lib.startsWith('<')) warnMsg('figmaLibraryUrl not set (placeholder) — bind a library before pull/derive')
  else ok('figmaLibraryUrl set')

  for (const relative of ['tests', 'tests/calibration', 'tests/census', 'tests/spec']) {
    exists(figmaPath(...relative.split('/')))
      ? ok(`${relative}/ present`)
      : failMsg(`${relative}/ missing (required Figma test/fixture directory)`)
  }

  // Screen-cache + census contract (the spec-gate's input side).
  if (exists(figmaPath('scripts', 'component-census.mjs'))) ok('component-census.mjs present (screen census)')
  else if (enabled === 'true') failMsg('component-census.mjs missing (required when figmaEnabled: true — task-prep Step 5.5 / orchestrator Step 1b run it)')
  else warnMsg('component-census.mjs missing')
  if (exists(figmaPath('token-schemas', 'census.schema.json'))) ok('census.schema.json present (census contract)')
  else if (enabled === 'true') failMsg('census.schema.json missing (required when figmaEnabled: true — the census report contract)')
  else warnMsg('census.schema.json missing')
  if (exists(figmaPath('token-schemas', 'spec.schema.json'))) ok('spec.schema.json present (spec validator contract)')
  else if (enabled === 'true') failMsg('spec.schema.json missing (required when figmaEnabled: true — check-spec.mjs needs it)')
  else warnMsg('spec.schema.json missing')
  // instances.schema.json validates the per-screen instances-list shape, including mandatory owning component-set identity.
  if (exists(figmaPath('token-schemas', 'instances.schema.json'))) ok('instances.schema.json present (instances-list contract)')
  else warnMsg('instances.schema.json missing (instances-list contract — soft)')
  // Pipeline HARD ajv-gated schemas (the token and component comparison pipelines validate their published inventories and the shared adapter config); presence-check them so a missing file fails cleanly here instead of crashing at readJson downstream.
  for (const schema of ['observed-token-source-index.schema.json', 'observed-token-catalog.schema.json',
    'token-binding-snapshot.schema.json', 'token-comparison.schema.json']) {
    if (exists(figmaPath('schemas', schema))) ok(`${schema} present`)
    else if (enabled === 'true') failMsg(`${schema} missing (required when figmaEnabled: true)`)
    else warnMsg(`${schema} missing`)
  }
  if (exists(figmaPath('schemas', 'design-component-inventory.schema.json'))) ok('design-component-inventory.schema.json present (component contract)')
  else if (enabled === 'true') failMsg('design-component-inventory.schema.json missing (required when figmaEnabled: true — the published component inventory is validated against it)')
  else warnMsg('design-component-inventory.schema.json missing')
  if (exists(figmaPath('schemas', 'component-mappings.schema.json'))) ok('component-mappings.schema.json present (component mapping contract)')
  else if (enabled === 'true') failMsg('component-mappings.schema.json missing (required when figmaEnabled: true — the CAS mapping registry is validated against it)')
  else warnMsg('component-mappings.schema.json missing')
  if (exists(figmaPath('schemas', 'project-adapters.schema.json'))) ok('project-adapters.schema.json present (adapter contract)')
  else if (enabled === 'true') failMsg('project-adapters.schema.json missing (required when figmaEnabled: true — the comparison pipelines validate adapter config against it)')
  else warnMsg('project-adapters.schema.json missing')

  // Evidence-bundle gate schemas — the FINAL gate (evidence-bundle.mjs) compiles the
  // report-envelope schema plus one per report family (mirror of its REPORT_SCHEMAS map).
  // A missing OR corrupt (non-parseable) schema dead-ends the gate at compileSchema. Check
  // each exists AND JSON.parse-clean here so it fails cleanly at preflight, not at final.
  const GATE_SCHEMAS = [
    'report-envelope.schema.json',   // evidence-bundle: envelopeSchema
    'screen-cache-report.schema.json',
    'check-spec-report.schema.json',
    'capture-config-report.schema.json',
    'census.schema.json',
    'spec-report.schema.json',       // evidence-bundle REPORT_SCHEMAS.spec — was missing from this preflight mirror
    'spec-compare-report.schema.json',
    'screenshot-compare-report.schema.json',
    'screenshot-thresholds.schema.json',   // evidence-bundle: validates the committed thresholds config feeding its canon
  ]
  for (const s of GATE_SCHEMAS) {
    const p = figmaPath('token-schemas', s)
    if (!exists(p)) failMsg(`${s} missing (evidence-bundle final gate compiles it — restore token-schemas/${s})`)
    else {
      try { readJson(p); ok(`${s} present + parseable (final gate schema)`) }
      catch (e) { failMsg(`${s} unparseable (${e.message}) — the final gate dead-ends compiling it; fix token-schemas/${s}`) }
    }
  }

  // The thresholds CONFIG INSTANCE, not just its schema file: compare-screenshots
  // and evidence-bundle both hard-fail at import when it is absent/invalid, so a
  // broken config must red HERE at preflight, not dead-end the first task run.
  // Probed in a child process because the loader's contract is process.exit(1) —
  // the probe exercises the REAL loader (one validation source, nothing replicated).
  {
    const probe = `import(${JSON.stringify(pathToFileURL(figmaPath('scripts', '_util.mjs')).href)}).then((m) => m.loadScreenshotThresholds())`
    try {
      execFileSync('node', ['--input-type=module', '-e', probe], { stdio: 'pipe', encoding: 'utf8' })
      ok('screenshot-thresholds.json present + loader-valid (the single strictness source)')
    } catch (e) {
      failMsg(`screenshot-thresholds.json fails the loader — every compare/evidence run will die at import: ${String(e.stderr || e.message).trim().split('\n')[0]}`)
    }
  }

  // Spec validator + screenshot compare scripts
  if (exists(figmaPath('scripts', 'check-spec.mjs'))) ok('check-spec.mjs present (figma:check-spec)')
  else if (enabled === 'true') failMsg('check-spec.mjs missing (required when figmaEnabled: true)')
  else warnMsg('check-spec.mjs missing')
  if (exists(figmaPath('scripts', 'compare-screenshots.mjs'))) ok('compare-screenshots.mjs present (figma:compare-screenshots)')
  else if (enabled === 'true') failMsg('compare-screenshots.mjs missing (required when figmaEnabled: true)')
  else warnMsg('compare-screenshots.mjs missing')

  try {
    const { parseKotlinSource, parserInfo } = await import('./compose-model/parser-tree-sitter.mjs')
    const parsed = parseKotlinSource('package doctor\n@Composable\nfun DoctorScreen() { Text("ok") }\n')
    if (parsed.hasError || parsed.parseErrors.length) failMsg('tree-sitter Kotlin parser failed on trivial Compose source')
    else ok(`${parserInfo().engine} available (implementation model parser)`)
  } catch (e) {
    failMsg(`tree-sitter Kotlin parser unavailable: ${e.message}`)
  }

  // Auth snapshot shape only — never print identity fields. Runtime additionally
  // binds the nonce/timestamp to the current connector episode before enabling pulls.
  const accountPath = figmaPath('.account.json')
  if (!exists(accountPath)) {
    warnMsg('no account snapshot yet — run whoami / bind via the site Figma tab')
  } else {
    try {
      const issue = accountSnapshotIssue(readJson(accountPath))
      issue
        ? warnMsg(`account snapshot is invalid (${issue}) — the site will keep Figma actions blocked and re-run whoami`)
        : ok('account snapshot shape valid (.account.json); runtime still verifies the current connector episode')
    } catch (e) {
      warnMsg(`account snapshot unreadable (${e.message}) — the site will keep Figma actions blocked and re-run whoami`)
    }
  }

  // Enforcement wiring (W2) — only for a figmaEnabled PRODUCT (the template + non-figma
  // projects pay nothing, so figma:verify stays green here). Gathers the real git/fs inputs and
  // delegates the verdict to the pure enforcementWiringFindings().
  if (enabled === 'true') {
    let inGit = false
    try { execFileSync('git', ['rev-parse', '--is-inside-work-tree'], { cwd: PROJECT_ROOT, stdio: 'pipe' }); inGit = true } catch { /* not a git work-tree */ }
    let hooksPath = ''
    if (inGit) { try { hooksPath = execFileSync('git', ['config', '--get', 'core.hooksPath'], { cwd: PROJECT_ROOT, stdio: ['pipe', 'pipe', 'ignore'] }).toString().trim() } catch { /* unset → '' */ } }
    // Default to WARN (bootstrap runs doctor at Step 6.5 before install-skills wires hooks at
    // Step 14); `--strict` / FIGMA_STRICT_WIRING=1 (the Step-14 post-install verification) FAILs.
    const strict = process.env.FIGMA_STRICT_WIRING === '1' || process.argv.includes('--strict')
    for (const f of enforcementWiringFindings({ inGit, hooksPath, strict })) {
      f.level === 'ok' ? ok(f.msg) : f.level === 'warn' ? warnMsg(f.msg) : failMsg(f.msg)
    }
  }

  process.exit(summary('figma:doctor'))
})().catch((e) => { failMsg(e?.message || String(e)); process.exit(summary('figma:doctor')) })
