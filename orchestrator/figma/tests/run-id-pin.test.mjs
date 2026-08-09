// Fixture self-test for the file-pinned pipeline run id (_util.mjs pipelineRunId/runIdPinPath)
// — no Figma, no Gradle. Verifies the W1-1 contract: two separate processes with no env agree
// on one id via the pin; env wins and re-pins; a malformed pin fails closed;
// evidence-clean's full clean removes the pin while --bundle-only keeps it.
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, existsSync, symlinkSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { artifactSegment } from '../scripts/_util.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const UTIL = join(HERE, '..', 'scripts', '_util.mjs')
const CLEAN = join(HERE, '..', 'scripts', 'evidence-clean.mjs')
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

// pipelineRunId in a CHILD process (the whole point: ids must survive process boundaries).
function runIdProcess(reportsDir, stem, env = {}) {
  return spawnSync('node', ['--input-type=module', '-e',
    `import { pipelineRunId } from ${JSON.stringify(UTIL)}; process.stdout.write(pipelineRunId(${JSON.stringify(stem)}))`],
    { env: { ...process.env, FIGMA_REPORTS_DIR: reportsDir, FIGMA_PIPELINE_RUN_ID: '', ...env }, encoding: 'utf8' })
}

function runIdInChild(reportsDir, stem, env = {}) {
  const r = runIdProcess(reportsDir, stem, env)
  assert.equal(r.status, 0, `child failed: ${r.stderr}`)
  return r.stdout.trim()
}

function setup(stem = 'TASK_PIN') {
  const cacheRoot = mkdtempSync(join(tmpdir(), 'run-id-pin-'))
  const reportsDir = join(cacheRoot, 'reports')
  mkdirSync(reportsDir, { recursive: true })
  return { cacheRoot, reportsDir, pin: join(reportsDir, `.run-id-${artifactSegment(stem)}`) }
}

// (1) Two separate processes, no env → SAME id; the pin file holds it.
check('two env-less processes agree on one pinned id', () => {
  const t = setup()
  const a = runIdInChild(t.reportsDir, 'TASK_PIN')
  const b = runIdInChild(t.reportsDir, 'TASK_PIN')
  assert.equal(a, b, `ids diverged: ${a} vs ${b}`)
  assert.ok(existsSync(t.pin), 'pin file missing')
  assert.equal(readFileSync(t.pin, 'utf8').trim(), a)
})

// (2) Env wins over an existing different pin and re-pins the file.
check('env id wins and re-pins', () => {
  const t = setup()
  writeFileSync(t.pin, 'oldid-TASK_PIN\n')
  const got = runIdInChild(t.reportsDir, 'TASK_PIN', { FIGMA_PIPELINE_RUN_ID: 'envid-123' })
  assert.equal(got, 'envid-123')
  assert.equal(readFileSync(t.pin, 'utf8').trim(), 'envid-123', 'pin not rewritten to env id')
})

// (3) A malformed durable pin is an explicit recovery error.
check('malformed pin fails closed without replacing durable evidence', () => {
  const t = setup()
  writeFileSync(t.pin, 'bad id with spaces !!\n')
  const result = runIdProcess(t.reportsDir, 'TASK_PIN')
  assert.notEqual(result.status, 0)
  assert.match(result.stderr, /pinned pipeline run id must match/)
  assert.equal(readFileSync(t.pin, 'utf8').trim(), 'bad id with spaces !!')
})

// (4) Per-stem isolation: different stems pin different ids.
check('pins are per-stem', () => {
  const t = setup()
  const a = runIdInChild(t.reportsDir, 'TASK_A')
  const b = runIdInChild(t.reportsDir, 'TASK_B')
  assert.notEqual(a, b, 'different stems shared one id')
})

// (5) evidence-clean: full clean removes the pin; --bundle-only keeps it.
check('evidence-clean full clean removes the pin, --bundle-only keeps it', () => {
  const stem = 'TASK_1_pin'
  const t = setup(stem)
  runIdInChild(t.reportsDir, stem)
  assert.ok(existsSync(t.pin))
  const env = { ...process.env, FIGMA_REPORTS_DIR: t.reportsDir, FIGMA_CACHE_ROOT: t.cacheRoot, FIGMA_PIPELINE_RUN_ID: '' }
  const keep = spawnSync('node', [CLEAN, stem, '--bundle-only'], { env, encoding: 'utf8' })
  assert.equal(keep.status, 0, keep.stderr)
  assert.ok(existsSync(t.pin), '--bundle-only must keep the pin (re-bundle stays on the same run)')
  const full = spawnSync('node', [CLEAN, stem], { env, encoding: 'utf8' })
  assert.equal(full.status, 0, full.stderr)
  assert.ok(!existsSync(t.pin), 'full clean must remove the pin')
})

const LONG_STEM = 'TASK_250_zadacha_na_uluchshenii_flou_diagnostiki_i_telemetrii_terminaldiagnosticscreen'

check('long canonical stem keeps full logical run id while pin/artifact segments stay bounded', () => {
  const t = setup(LONG_STEM)
  const runId = runIdInChild(t.reportsDir, LONG_STEM)
  assert.equal(LONG_STEM.length, 86)
  assert.ok(runId.length > 80, `fixture must cover a long auto run id, got ${runId.length}`)
  assert.equal(artifactSegment(LONG_STEM).length, 80)
  assert.equal(artifactSegment(runId).length, 80)
  assert.equal(readFileSync(t.pin, 'utf8').trim(), runId, 'pin must retain the full logical run id')
})

check('valid dotted run id is preserved logically and mapped safely for artifact paths', () => {
  const t = setup()
  const runId = runIdInChild(t.reportsDir, 'TASK_PIN', { FIGMA_PIPELINE_RUN_ID: 'owner.review-1' })
  assert.equal(runId, 'owner.review-1')
  assert.match(artifactSegment(runId), /^owner-review-1-[a-f0-9]{64}$/)
  assert.notEqual(artifactSegment(runId), artifactSegment('owner-review-1'))
})

check('lossy and truncated logical ids cannot alias one artifact directory', () => {
  const shared = `run-${'a'.repeat(76)}`
  const first = artifactSegment(`${shared}-one`)
  const second = artifactSegment(`${shared}-two`)
  assert.equal(first.length, 80)
  assert.equal(second.length, 80)
  assert.notEqual(first, second)
  assert.notEqual(artifactSegment('owner.review-1'), artifactSegment('owner-review-1'))
  assert.match(artifactSegment('CON'), /^CON-[a-f0-9]{64}$/, 'Windows device names must not remain raw path segments')
  assert.throws(() => artifactSegment(''), /artifact identity must be non-empty/)
})

check('invalid env run id fails closed without poisoning the pin', () => {
  const t = setup()
  const r = runIdProcess(t.reportsDir, 'TASK_PIN', { FIGMA_PIPELINE_RUN_ID: '../escape' })
  assert.notEqual(r.status, 0)
  assert.match(r.stderr, /FIGMA_PIPELINE_RUN_ID must match/)
  assert.equal(existsSync(t.pin), false)
})

check('evidence-clean removes the bounded pin and artifact tree for a long stem', () => {
  const t = setup(LONG_STEM)
  const runId = runIdInChild(t.reportsDir, LONG_STEM)
  const artifactRoot = join(t.cacheRoot, 'artifacts', 'screenshot')
  const artifactDir = join(artifactRoot, artifactSegment(LONG_STEM), artifactSegment(runId))
  mkdirSync(artifactDir, { recursive: true })
  writeFileSync(join(artifactDir, 'probe.txt'), 'probe\n')
  const env = {
    ...process.env,
    FIGMA_REPORTS_DIR: t.reportsDir,
    FIGMA_CACHE_ROOT: t.cacheRoot,
    FIGMA_COMPARE_ARTIFACTS_DIR: artifactRoot,
    FIGMA_PIPELINE_RUN_ID: '',
  }
  const cleaned = spawnSync('node', [CLEAN, LONG_STEM, '--artifacts'], { env, encoding: 'utf8' })
  assert.equal(cleaned.status, 0, cleaned.stderr)
  assert.equal(existsSync(t.pin), false, 'full clean must remove the bounded long-stem pin')
  assert.equal(existsSync(join(artifactRoot, artifactSegment(LONG_STEM))), false, 'artifact cleanup must target the bounded stem directory')
})

check('evidence-clean refuses a symlink inside the artifact tree and preserves its target', () => {
  const stem = 'TASK_251_artifact_symlink'
  const t = setup(stem)
  const artifactRoot = join(t.cacheRoot, 'artifacts', 'screenshot')
  const artifactDir = join(artifactRoot, artifactSegment(stem))
  const external = join(t.cacheRoot, 'external-artifact.txt')
  mkdirSync(artifactDir, { recursive: true })
  writeFileSync(external, 'external artifact\n')
  symlinkSync(external, join(artifactDir, 'linked.txt'))
  const env = {
    ...process.env,
    FIGMA_REPORTS_DIR: t.reportsDir,
    FIGMA_CACHE_ROOT: t.cacheRoot,
    FIGMA_COMPARE_ARTIFACTS_DIR: artifactRoot,
    FIGMA_PIPELINE_RUN_ID: '',
  }
  const cleaned = spawnSync('node', [CLEAN, stem, '--artifacts'], { env, encoding: 'utf8' })
  assert.notEqual(cleaned.status, 0)
  assert.match(cleaned.stderr, /symlinks and special files are not cleanup authority/)
  assert.equal(readFileSync(external, 'utf8'), 'external artifact\n')
  assert.equal(existsSync(join(artifactDir, 'linked.txt')), true)
})

console.log(`\nrun-id-pin.test: ${pass} pass, ${fail} fail`)
process.exit(fail === 0 ? 0 : 1)
