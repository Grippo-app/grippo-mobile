// enforcement-net.test.mjs — pins for the two MECHANICAL layers of the W2 enforcement net
// (the run-gate in the site server + the pre-commit verify-done hook). The pure verdict logic
// (enforcementWiringFindings) is pinned by cli-contracts.test.mjs; these pins guard the layers
// that actually stop an uncompared UI task, so a future edit cannot silently disarm them:
//
// Run-gate (server/sessions.js runGateError + server/git.js enforcementWiring):
//   (1) unwired + figmaEnabled → refusal names 'figma-net-unwired' AND the exact wiring command;
//   (2) start(action:'run') is refused with NO session registered (returns before any spawn);
//   (3) the warm-idle send(action:'run') reuse path is refused too (returns false);
//   (4) FIGMA_WIRING_GATE=0 escape hatch disables the gate;
//   (5) figmaEnabled:false / null config (template pre-bootstrap) → gate off;
//   (6) wired → gate off;
//   (7) ADVERSARIAL — action:'prep' is NOT gated (over-gating prep would freeze a product's
//       task-prep flow; the gate must stay scoped to `run`).
//
// Pre-commit hook (skills/checks/hooks/pre-commit, exercised via a hermetic scratch git repo
// with a stub verify-done.mjs so only the HOOK's plumbing is under test):
//   (8) a production script change regenerates the production manifest;
//   (9) a production-script rename still regenerates the manifest;
//   (10) a pinned central runner change regenerates the production manifest;
//   (11) an extracted suite module change regenerates the production manifest;
//   (12) both manifest generators are themselves guarded triggers;
//   (13) a Figma test change does not regenerate the production manifest;
//   (14) mixed staged/unstaged manifest sources fail closed for script and skill manifests;
//   (15) verify-done green → commit passes; (16) verify-done red → commit BLOCKED
//   (the bare-`mv` net); (17) `git commit --no-verify` stays the deliberate bypass.
//
// Manifest reverse closure:
//   (18) production scripts and top-level test-infrastructure modules must all be pinned.
import { mkdtempSync, mkdirSync, writeFileSync, rmSync, unlinkSync, chmodSync, readFileSync, existsSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { tmpdir } from 'node:os'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import { createRequire } from 'node:module'
import assert from 'node:assert/strict'
import { unpinnedScriptManifestPaths } from '../scripts/doctor.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const ORCH = join(HERE, '..', '..')                    // orchestrator/
const C = { reset: '\x1b[0m', red: '\x1b[31m', green: '\x1b[32m' }
let pass = 0, fail = 0
const check = (name, fn) => { try { fn(); pass++; console.log(`${C.green}PASS${C.reset} ${name}`) } catch (e) { fail++; console.log(`${C.red}FAIL${C.reset} ${name}\n     ${e.message}`) } }

// ─── Section A — run-gate ────────────────────────────────────────────────────
// The server modules are CommonJS; stub the centralized git-owned policy
// through the shared require cache BEFORE sessions.js captures it, then drive
// runGateError/start/send against controlled wiring/config states. sessions.js
// no longer reads enforcementWiring() itself, so stubbing only that retired
// dependency boundary would accidentally exercise the operator's real repo.
const require2 = createRequire(import.meta.url)
// Mirror the production bootstrap order in a private project root. sessions.js
// deliberately fails closed while the finalization authority is absent; the
// real site creates that authority through finalizations.init() before it
// accepts requests. Keeping it under a fixture also prevents this mechanical
// gate test from reading or writing the operator's live runtime state.
const SESSION_PROJECT = mkdtempSync(join(tmpdir(), 'enforcement-net-session-'))
mkdirSync(join(SESSION_PROJECT, 'orchestrator'), { recursive: true })
const savedProjectRoot = process.env.ORCHESTRATOR_PROJECT_ROOT
process.env.ORCHESTRATOR_PROJECT_ROOT = SESSION_PROJECT
const GATE_STEM = 'TASK_999998_gatetest'
const GATE_KEY = 'task:' + GATE_STEM
const gitPath = require2.resolve(join(ORCH, 'site', 'server', 'git.js'))
const cfgPath = require2.resolve(join(ORCH, 'site', 'server', 'project-config.js'))
const taskSourcePath = require2.resolve(join(ORCH, 'site', 'server', 'task-source.js'))
require2(gitPath); require2(cfgPath); require2(taskSourcePath)
const EXPECTED = 'orchestrator/skills/checks/hooks'
let wiring = { inGit: true, hooksPath: '', expected: EXPECTED, wired: false }
let cfg = { figmaEnabled: true }
require2.cache[gitPath].exports.enforcementWiring = () => wiring
require2.cache[gitPath].exports.enforcementNetIssue = () => {
  if (process.env.FIGMA_WIRING_GATE === '0' || !cfg || cfg.figmaEnabled !== true || wiring.wired) return null
  const state = wiring.inGit
    ? `core.hooksPath is ${wiring.hooksPath ? `'${wiring.hooksPath}'` : 'UNSET'}, expected '${wiring.expected}'`
    : 'not a git work-tree (or git unavailable)'
  return 'figma-net-unwired: ' + state + ' — the LOCAL screenshot-gate net (pre-commit verify-done) ' +
    'is INACTIVE, so an uncompared UI task could ship to done/. Wire it: ' +
    '`git config core.hooksPath ' + wiring.expected + '` (or run orchestrator/skills/install-skills.sh). ' +
    'Deliberate opt-out (self-managed hooks): FIGMA_WIRING_GATE=0.'
}
require2.cache[cfgPath].exports.parseConfigForm = () => cfg
// The gate is intentionally scoped to tasks with visual evidence. Supply one
// exact canonical todo row without constructing a mutable task corpus: this
// mechanical test owns the gate decision, while task-source parsing has its
// own contract suite.
require2.cache[taskSourcePath].exports = {
  safeTaskStem: (stem) => stem === GATE_STEM,
  readIndex: () => ({ rows: [{ column: 'todo', row: { stem: GATE_STEM } }] }),
  readTask: () => ({
    text: '## Design\n\n- Home — https://www.figma.com/design/fixture?node-id=1-2\n',
  }),
}
const finalizations = require2(join(ORCH, 'site', 'server', 'finalizations.js'))
finalizations.init()
const sessions = require2(join(ORCH, 'site', 'server', 'sessions.js'))
if (savedProjectRoot == null) delete process.env.ORCHESTRATOR_PROJECT_ROOT
else process.env.ORCHESTRATOR_PROJECT_ROOT = savedProjectRoot
const RUNS = join(SESSION_PROJECT, 'orchestrator', '.cache', 'tasks', 'runs')
// Pre-clean sidecars a previous (crashed) run may have left — sessions.list()
// surfaces finished sidecars from disk, so stale ones would poison asserts.
const reapSidecars = () => {
  for (const suffix of ['.events.jsonl', '.session.json']) {
    const p = join(RUNS, 'task_' + GATE_STEM + suffix)
    try { if (existsSync(p)) unlinkSync(p) } catch { /* best-effort */ }
  }
}
reapSidecars()
// A live-in-memory session for the stem (disk sidecars come back running:false).
const liveGateSession = () => {
  const all = sessions.list()
  return Object.keys(all).some((k) => k.includes(GATE_STEM) && all[k] && all[k].running)
}

check('run-gate: unwired figmaEnabled product → refusal names the error AND the wiring command', () => {
  const err = sessions.runGateError()
  assert.ok(err, 'expected a refusal, got null')
  assert.match(err, /figma-net-unwired/)
  assert.match(err, /git config core\.hooksPath orchestrator\/skills\/checks\/hooks/)
  assert.match(err, /install-skills\.sh/)
})

check('run-gate: a visual start(action:run) is refused with NO session registered (no spawn)', () => {
  const st = sessions.start(GATE_KEY, { action: 'run', prompt: 'must not spawn' })
  assert.equal(st.running, false)
  assert.match(String(st.error || ''), /figma-net-unwired/)
  assert.ok(!liveGateSession(), 'refused run must not register a live session')
})

check('run-gate: warm-idle send(action:run) reuse path is refused (false)', () => {
  assert.equal(sessions.send(GATE_KEY, 'x', { action: 'run' }), false)
})

check('run-gate: FIGMA_WIRING_GATE=0 escape hatch disables the gate', () => {
  process.env.FIGMA_WIRING_GATE = '0'
  try { assert.equal(sessions.runGateError(), null) } finally { delete process.env.FIGMA_WIRING_GATE }
})

check('run-gate: figmaEnabled:false and null config (template pre-bootstrap) → gate off', () => {
  const saved = cfg
  try {
    cfg = { figmaEnabled: false }
    assert.equal(sessions.runGateError(), null)
    cfg = null
    assert.equal(sessions.runGateError(), null)
  } finally { cfg = saved }
})

check('run-gate: wired → gate off', () => {
  const saved = wiring
  try {
    wiring = { inGit: true, hooksPath: EXPECTED, expected: EXPECTED, wired: true }
    assert.equal(sessions.runGateError(), null)
  } finally { wiring = saved }
})

check('run-gate: action:prep is NOT gated (gate stays scoped to run)', () => {
  // Put a fake `claude` (a short sleep) first on PATH so the ungated prep can
  // spawn hermetically. It must get PAST the gate: the session starts for real
  // while the SAME wiring state refuses `run` — the strongest possible pin
  // that the gate is scoped to the action, not the key.
  const fakeBin = mkdtempSync(join(tmpdir(), 'enforcement-net-bin-'))
  writeFileSync(join(fakeBin, 'claude'), '#!/bin/sh\nsleep 2\n')
  chmodSync(join(fakeBin, 'claude'), 0o755)
  const savedPath = process.env.PATH
  process.env.PATH = fakeBin + ':' + savedPath
  let st
  try { st = sessions.start(GATE_KEY, { action: 'prep', prompt: '' }) } finally {
    process.env.PATH = savedPath
  }
  try {
    assert.ok(!/figma-net-unwired/.test(String(st.error || '')), 'prep must not hit the run-gate')
    assert.equal(st.running, true, 'prep must actually start while the gate refuses run')
  } finally {
    sessions.cancel(GATE_KEY)
    rmSync(fakeBin, { recursive: true, force: true })
  }
})

// The prep probe wrote transcript sidecars into the live .cache — reap them.
sessions.killAll()
reapSidecars()

// ─── Section B — script-manifest reverse closure ─────────────────────────────
check('manifest: production scripts and top-level test infrastructure fail closed when unpinned', () => {
  const ws = mkdtempSync(join(tmpdir(), 'enforcement-manifest-'))
  const scriptsRoot = join(ws, 'scripts')
  const testInfrastructureRoot = join(ws, 'tests')
  try {
    mkdirSync(join(scriptsRoot, '_index'), { recursive: true })
    mkdirSync(join(scriptsRoot, 'nested'), { recursive: true })
    mkdirSync(join(testInfrastructureRoot, 'nested'), { recursive: true })
    writeFileSync(join(scriptsRoot, 'root.mjs'), '// production root\n')
    writeFileSync(join(scriptsRoot, 'nested', 'child.cjs'), '// production child\n')
    writeFileSync(join(scriptsRoot, '_index', 'generated.mjs'), '// excluded index\n')
    writeFileSync(join(testInfrastructureRoot, 'runner.mjs'), '// test infrastructure\n')
    writeFileSync(
      join(testInfrastructureRoot, 'nested', 'fixture.mjs'),
      '// excluded nested fixture\n'
    )

    const expected = [
      'orchestrator/figma/scripts/nested/child.cjs',
      'orchestrator/figma/scripts/root.mjs',
      'orchestrator/tests/runner.mjs'
    ]
    assert.deepEqual(
      unpinnedScriptManifestPaths({}, { scriptsRoot, testInfrastructureRoot }),
      expected
    )
    assert.deepEqual(
      unpinnedScriptManifestPaths(
        Object.fromEntries(expected.map((relative) => [relative, 'sha256'])),
        { scriptsRoot, testInfrastructureRoot }
      ),
      []
    )
  } finally {
    rmSync(ws, { recursive: true, force: true })
  }
})

// ─── Section C — pre-commit hook plumbing ────────────────────────────────────
// Hermetic scratch git repo wired exactly like a product (core.hooksPath →
// tracked hooks dir, REAL hook bytes) with a stub verify-done.mjs driven by a
// .gate-state marker — pins that the hook blocks on exit!=0 and passes on 0.
const HOOK_SRC = join(ORCH, 'skills', 'checks', 'hooks', 'pre-commit')
function hookFixture() {
  const ws = mkdtempSync(join(tmpdir(), 'enforcement-net-'))
  const env = { ...process.env, GIT_CONFIG_NOSYSTEM: '1', GIT_CONFIG_GLOBAL: join(ws, '.no-global-gitconfig') }
  const git = (...args) => spawnSync('git', args, { cwd: ws, encoding: 'utf8', env })
  git('init', '-q')
  git('config', 'user.email', 'test@example.com')
  git('config', 'user.name', 'enforcement-net-test')
  git('config', 'commit.gpgsign', 'false')
  git('config', 'core.hooksPath', 'orchestrator/skills/checks/hooks')
  mkdirSync(join(ws, 'orchestrator', 'skills', 'checks', 'hooks'), { recursive: true })
  const hookDst = join(ws, 'orchestrator', 'skills', 'checks', 'hooks', 'pre-commit')
  writeFileSync(hookDst, readFileSync(HOOK_SRC))
  chmodSync(hookDst, 0o755)
  mkdirSync(join(ws, 'orchestrator', 'figma', 'scripts'), { recursive: true })
  writeFileSync(join(ws, 'orchestrator', 'figma', 'scripts', 'verify-done.mjs'), [
    "import { readFileSync } from 'node:fs'",
    "let state = 'RED'",
    "try { state = readFileSync('.gate-state', 'utf8').trim() } catch {}",
    "if (state === 'GREEN') { console.log('verify-done stub: green'); process.exit(0) }",
    "console.error('  \\u2717 TASK_1_stub.md: stub violation (red gate)')",
    'process.exit(2)'
  ].join('\n'))
  const indexDir = join(ws, 'orchestrator', 'figma', 'scripts', '_index')
  const manifest = join(indexDir, 'script-manifest.json')
  const regenMarker = join(ws, '.manifest-regenerated')
  const generator = join(indexDir, '_generate_script_manifest.py')
  mkdirSync(indexDir, { recursive: true })
  writeFileSync(generator, [
    'from pathlib import Path',
    "Path('.manifest-regenerated').write_text('yes\\n')",
    "Path('orchestrator/figma/scripts/_index/script-manifest.json').write_text(" +
      "'{\\\"version\\\":1,\\\"count\\\":0,\\\"files\\\":{}}\\\\n')",
  ].join('\n'))
  writeFileSync(manifest, '{"version":1,"count":0,"files":{}}\n')
  const skillIndexDir = join(ws, 'orchestrator', 'skills', '_index')
  const installManifest = join(skillIndexDir, 'install-manifest.json')
  const installRegenMarker = join(ws, '.install-manifest-regenerated')
  const installGenerator = join(skillIndexDir, '_generate_install_manifest.py')
  mkdirSync(skillIndexDir, { recursive: true })
  writeFileSync(installGenerator, [
    'from pathlib import Path',
    "Path('.install-manifest-regenerated').write_text('yes\\n')",
    "Path('orchestrator/skills/_index/install-manifest.json').write_text(" +
      "'{\\\"version\\\":1,\\\"count\\\":0,\\\"skills\\\":[]}\\\\n')",
  ].join('\n'))
  writeFileSync(installManifest, '{"version":1,"count":0,"skills":[]}\n')
  // Seed without executing the hook. Real repos commit the pipeline and its
  // generator before later production/test changes exercise the boundary.
  git('add', '-A')
  git('commit', '-q', '--no-verify', '-m', 'seed')
  let n = 0
  return {
    ws,
    git,
    manifest,
    regenMarker,
    generator,
    installGenerator,
    installRegenMarker,
    commitFile(state, relative, source) {
      writeFileSync(join(ws, '.gate-state'), state)
      const target = join(ws, ...relative.split('/'))
      mkdirSync(dirname(target), { recursive: true })
      writeFileSync(target, source)
      git('add', relative)
      return git('commit', '-q', '-m', 'path-contract-' + (++n))
    },
    commitPartiallyStagedFile(state, relative, stagedSource, unstagedSource) {
      writeFileSync(join(ws, '.gate-state'), state)
      const target = join(ws, ...relative.split('/'))
      mkdirSync(dirname(target), { recursive: true })
      writeFileSync(target, stagedSource)
      git('add', relative)
      writeFileSync(target, unstagedSource)
      return git('commit', '-q', '-m', 'partial-path-contract-' + (++n))
    },
    renameFile(state, from, to) {
      writeFileSync(join(ws, '.gate-state'), state)
      mkdirSync(dirname(join(ws, ...to.split('/'))), { recursive: true })
      const moved = git('mv', from, to)
      assert.equal(moved.status, 0, 'stderr: ' + moved.stderr)
      return git('commit', '-q', '-m', 'rename-path-contract-' + (++n))
    },
    commit(state, extraArgs = []) {
      writeFileSync(join(ws, '.gate-state'), state)
      writeFileSync(join(ws, 'file' + (++n) + '.txt'), state + '\n')
      git('add', 'file' + n + '.txt')
      return git('commit', '-q', ...extraArgs, '-m', 'c' + n)
    }
  }
}

{
  const fx = hookFixture()
  try {
    check('hook: production script change regenerates the production manifest', () => {
      try { unlinkSync(fx.regenMarker) } catch {}
      const r = fx.commitFile(
        'GREEN',
        'orchestrator/figma/scripts/path-contract.mjs',
        '// production path contract fixture\n'
      )
      assert.equal(r.status, 0, 'stderr: ' + r.stderr)
      assert.equal(existsSync(fx.regenMarker), true)
    })
    check('hook: production script rename regenerates the production manifest', () => {
      const seeded = fx.commitFile(
        'GREEN',
        'orchestrator/figma/scripts/rename-source.mjs',
        '// production rename source fixture\n'
      )
      assert.equal(seeded.status, 0, 'stderr: ' + seeded.stderr)
      try { unlinkSync(fx.regenMarker) } catch {}
      const r = fx.renameFile(
        'GREEN',
        'orchestrator/figma/scripts/rename-source.mjs',
        'orchestrator/figma/tests/rename-source.mjs'
      )
      assert.equal(r.status, 0, 'stderr: ' + r.stderr)
      assert.equal(existsSync(fx.regenMarker), true)
    })
    check('hook: pinned central runner change regenerates the production manifest', () => {
      try { unlinkSync(fx.regenMarker) } catch {}
      const r = fx.commitFile(
        'GREEN',
        'orchestrator/tests/run-suite.mjs',
        '// central runner path contract fixture\n'
      )
      assert.equal(r.status, 0, 'stderr: ' + r.stderr)
      assert.equal(existsSync(fx.regenMarker), true)
    })
    check('hook: extracted suite module change regenerates the production manifest', () => {
      try { unlinkSync(fx.regenMarker) } catch {}
      const r = fx.commitFile(
        'GREEN',
        'orchestrator/tests/suite-discovery.mjs',
        '// extracted suite discovery path contract fixture\n'
      )
      assert.equal(r.status, 0, 'stderr: ' + r.stderr)
      assert.equal(existsSync(fx.regenMarker), true)
    })
    check('hook: script-manifest generator change regenerates the production manifest', () => {
      try { unlinkSync(fx.regenMarker) } catch {}
      const r = fx.commitFile(
        'GREEN',
        'orchestrator/figma/scripts/_index/_generate_script_manifest.py',
        readFileSync(fx.generator, 'utf8') + '\n# reviewed generator change\n'
      )
      assert.equal(r.status, 0, 'stderr: ' + r.stderr)
      assert.equal(existsSync(fx.regenMarker), true)
    })
    check('hook: install-manifest generator change regenerates the skill manifest', () => {
      try { unlinkSync(fx.installRegenMarker) } catch {}
      const r = fx.commitFile(
        'GREEN',
        'orchestrator/skills/_index/_generate_install_manifest.py',
        readFileSync(fx.installGenerator, 'utf8') + '\n# reviewed generator change\n'
      )
      assert.equal(r.status, 0, 'stderr: ' + r.stderr)
      assert.equal(existsSync(fx.installRegenMarker), true)
    })
    check('hook: Figma test change does not regenerate the production manifest', () => {
      try { unlinkSync(fx.regenMarker) } catch {}
      const before = readFileSync(fx.manifest, 'utf8')
      const r = fx.commitFile(
        'GREEN',
        'orchestrator/figma/tests/path-contract.test.mjs',
        '// staged Figma test fixture\n'
      )
      assert.equal(r.status, 0, 'stderr: ' + r.stderr)
      assert.equal(existsSync(fx.regenMarker), false)
      assert.equal(readFileSync(fx.manifest, 'utf8'), before)
    })
    check('hook: green verify-done → commit passes', () => {
      const r = fx.commit('GREEN')
      assert.equal(r.status, 0, 'stderr: ' + r.stderr)
    })
    check('hook: red verify-done → commit BLOCKED (the bare-`mv` net)', () => {
      const r = fx.commit('RED')
      assert.notEqual(r.status, 0, 'a red gate must block the commit')
      assert.match(String(r.stderr), /BLOCKED/)
    })
    check('hook: --no-verify stays the documented deliberate bypass', () => {
      const r = fx.commit('RED', ['--no-verify'])
      assert.equal(r.status, 0, 'stderr: ' + r.stderr)
    })
  } finally {
    rmSync(fx.ws, { recursive: true, force: true })
  }
}

{
  const fx = hookFixture()
  try {
    check('hook: partially staged production source fails closed before manifest generation', () => {
      try { unlinkSync(fx.regenMarker) } catch {}
      const r = fx.commitPartiallyStagedFile(
        'GREEN',
        'orchestrator/figma/scripts/partial-stage.mjs',
        '// staged production revision 1\n',
        '// unstaged production revision 2\n'
      )
      assert.notEqual(r.status, 0, 'mixed source revisions must block the commit')
      assert.match(String(r.stdout) + String(r.stderr), /mixed staged\/unstaged source revisions/)
      assert.equal(existsSync(fx.regenMarker), false)
    })
  } finally {
    rmSync(fx.ws, { recursive: true, force: true })
  }
}

{
  const fx = hookFixture()
  try {
    check('hook: partially staged skill source fails closed before manifest generation', () => {
      const r = fx.commitPartiallyStagedFile(
        'GREEN',
        'orchestrator/skills/fixture/SKILL.md',
        '---\nname: fixture\n---\n',
        '---\nname: changed-after-stage\n---\n'
      )
      assert.notEqual(r.status, 0, 'mixed skill revisions must block the commit')
      assert.match(String(r.stdout) + String(r.stderr), /mixed staged\/unstaged source revisions/)
    })
  } finally {
    rmSync(fx.ws, { recursive: true, force: true })
  }
}

console.log(`\nenforcement-net.test: ${pass} pass, ${fail} fail`)
// A prep probe's spawn-error event may still be in flight — let it settle
// before the process result is decided.
setTimeout(() => {
  rmSync(SESSION_PROJECT, { recursive: true, force: true })
  process.exit(fail === 0 ? 0 : 1)
}, 250)
