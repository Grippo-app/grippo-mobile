#!/usr/bin/env node
// Phase 4 integration transaction (pipeline improvement 01, §10/§21). The
// candidate produced in an isolated worktree becomes ONE canonical commit on
// the control root's target branch, together with everything the finalizer
// publishes, under a write-ahead log that re-proves every phase from physical
// state. These tests drive the REAL git mutation owner against a real
// repository; only the finalizer child is a stand-in, and it performs exactly
// the physical effects the transaction contracts with it for (task published
// into done/, INDEX regenerated, lock and marker retained until confirm).

import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { chmodSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const taskSource = require('../../tasks/task-source-contract.cjs')
const repoRoot = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..')

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }
const roots = []
function git(cwd, ...args) {
  return execFileSync('git', args, { cwd, encoding: 'utf8', env: { ...process.env, GIT_TERMINAL_PROMPT: '0' } })
}
// A ref that does not exist makes git exit non-zero; that is the answer, not an error.
function refOrEmpty(cwd, ref) {
  try { return git(cwd, 'rev-parse', '-q', '--verify', ref).trim() } catch (error) { return '' }
}

// A faithful finalizer stand-in. It does what the real prepare/confirm halves
// do to the FILESYSTEM — which is all the WAL ever observes — and nothing else.
// A stub that wrote nothing would make every assertion below vacuous, which is
// exactly how a previous phase nearly shipped a broken sealing path.
const STUB = `#!/usr/bin/env node
const fs = require('node:fs'); const P = require('node:path');
const argv = process.argv.slice(2);
const stem = argv[0];
const mode = argv[argv.indexOf('--mode') + 1];
const ROOT = process.env.FINALIZE_PROJECT_ROOT;
const STATE = process.env.FINALIZE_STATE_DIR;
const LOCKS = process.env.FINALIZE_LOCKS_DIR;
const marker = P.join(STATE, stem + '.json');
const PHASES = ['outcome','components','tokens','ship','index','arch','verify','unlock','cleanup'];
if (process.env.STUB_REFUSE === mode) {
  process.stderr.write('stub gate refusal\\n');
  process.exit(2);
}
// kill -9 of the TRANSACTION DRIVER at an exact boundary: the finalizer's
// effects are on disk, the driver never learns the child exited.
const killAfter = (which) => {
  if (process.env.STUB_KILL_DRIVER_AFTER === which) {
    try { process.kill(process.ppid, 'SIGKILL'); } catch (e) {}
    process.exit(0);
  }
};
if (mode === 'prepare') {
  const todo = P.join(ROOT, 'orchestrator/tasks/todo', stem + '.md');
  const done = P.join(ROOT, 'orchestrator/tasks/done', stem + '.md');
  // A production prepare retry revalidates physical state without replaying
  // the already-landed ship effect. This stand-in has no internal validators,
  // so preserve that idempotent shape when the marker and done task exist.
  if (fs.existsSync(marker) && fs.existsSync(done) && !fs.existsSync(todo)) {
    process.stdout.write(JSON.stringify({ stem, prepared: true, reverified: true }) + '\\n');
    process.exit(0);
  }
  fs.mkdirSync(P.dirname(done), { recursive: true });
  const draftFlag = argv.indexOf('--outcome-file');
  const draft = draftFlag >= 0 ? argv[draftFlag + 1] : null;
  let body = fs.readFileSync(todo, 'utf8');
  if (draft && fs.existsSync(draft)) body += fs.readFileSync(draft, 'utf8');
  fs.writeFileSync(done, body);
  fs.unlinkSync(todo);
  fs.writeFileSync(P.join(ROOT, 'orchestrator/tasks/INDEX.json'),
    JSON.stringify({ version: 1, done: [stem], todo: [] }) + '\\n');
  const phases = {};
  for (const p of PHASES) phases[p] = { state: PHASES.indexOf(p) <= PHASES.indexOf('verify') ? 'succeeded' : 'pending', attempts: 1 };
  fs.mkdirSync(STATE, { recursive: true });
  // The marker must satisfy the SAME shape contract the server validates, or
  // the transaction would be testing a shape the real finalizer never writes.
  const lockStat = fs.lstatSync(P.join(LOCKS, stem + '.json'), { bigint: true });
  const h = (text) => 'sha256:' + require('node:crypto').createHash('sha256').update(text).digest('hex');
  const at = new Date().toISOString();
  fs.writeFileSync(marker, JSON.stringify({
    version: 1, revision: 1, stem, transactionId: 'fin-stub0000',
    status: 'incomplete', phase: 'verify', createdAt: at, updatedAt: at, owner: null,
    source: {
      originalHash: h('original'), intendedHash: h('intended'), intendedLogicalHash: h('logical'),
      outcomeHash: h('outcome'), snapshotHash: h('snapshot'), publishFromHash: h('publishFrom'),
      lock: { present: true, ctimeNs: String(lockStat.ctimeNs), dev: String(lockStat.dev),
        hash: h('lock'), ino: String(lockStat.ino), kind: 'file', mode: Number(lockStat.mode),
        mtimeNs: String(lockStat.mtimeNs), size: Number(lockStat.size) }
    },
    figma: { enabled: false, configHash: h('config'), pipelineRunId: null },
    phases, artifacts: {}, lastError: null
  }) + '\\n');
  killAfter('prepare');
  process.stdout.write(JSON.stringify({ stem, prepared: true }) + '\\n');
  process.exit(0);
}
if (mode === 'confirm') {
  const commit = argv[argv.indexOf('--integration-commit') + 1];
  if (!/^[a-f0-9]{40}$/.test(commit || '')) { process.stderr.write('missing commit\\n'); process.exit(1); }
  const lock = P.join(LOCKS, stem + '.json');
  try { fs.unlinkSync(lock); } catch (e) {}
  try { fs.unlinkSync(marker); } catch (e) {}
  // Model an authority-path race after confirm has proved absence but before
  // the integration driver receives the child's successful exit. A symlink is
  // foreign evidence, never proof that the canonical lock name is absent.
  if (process.env.STUB_REPLACE_LOCK_AFTER_CONFIRM === 'symlink') {
    const foreign = P.join(LOCKS, stem + '.foreign-lock');
    fs.writeFileSync(foreign, 'foreign owner\\n');
    fs.symlinkSync(foreign, lock);
  }
  killAfter('confirm');
  process.stdout.write(JSON.stringify({ stem, completed: true }) + '\\n');
  process.exit(0);
}
process.stderr.write('unknown mode\\n');
process.exit(1);
`

// A real second transaction driver that pauses after the candidate effect but
// before the WAL proof. The control process can then complete the same WAL and
// release this stale writer against the terminal record generation.
const STALE_INTEGRATION_DRIVER = `
const fs = require('node:fs');
const P = require('node:path');
const repo = process.env.INTEGRATION_TEST_REPO;
const stem = process.env.INTEGRATION_TEST_STEM;
const gateModule = P.join(repo, 'orchestrator/site/server/task-checkpoints.js');
require.cache[gateModule] = { id: gateModule, filename: gateModule, loaded: true,
  exports: { sealingGate: () => ({ ok: true, checkpoint: { checkpointId: 'cp-fixture' } }) } };
const gitMutations = require(P.join(repo, 'orchestrator/site/server/git-mutations.js'));
const originalApply = gitMutations.applyCandidate;
gitMutations.applyCandidate = function (options) {
  const result = originalApply(options);
  if (result.ok) {
    fs.writeFileSync(process.env.INTEGRATION_TEST_APPLIED, 'applied\\n');
    const wait = new Int32Array(new SharedArrayBuffer(4));
    while (!fs.existsSync(process.env.INTEGRATION_TEST_GATE)) Atomics.wait(wait, 0, 0, 20);
  }
  return result;
};
const integrations = require(P.join(repo, 'orchestrator/site/server/integrations.js'));
integrations.begin(stem, (result) => {
  fs.writeFileSync(process.env.INTEGRATION_TEST_OUT, JSON.stringify({
    ok: result.ok, code: result.code || null, message: result.message || null
  }) + '\\n');
});
`

// Each scenario runs in a child process so paths.js binds to the fixture root.
// The canonical task document shape every transition validates. Built from the
// task-source renderer itself so the fixture cannot drift from the grammar.
function canonicalTodo(number, slug) {
  const ref = `integration-fixture-${number}`
  const source = taskSource.render(taskSource.manualForIntent(ref, 'manual', ref))
  return `# TASK ${number} — Integration ${slug}\n\n${source}\n\n` +
    '## Goal\n- Exercise the integration transaction end to end.\n\n' +
    '## Inputs\n- Fixture sources under `app/`.\n\n' +
    '## Acceptance\n\n### Automated\n- `integrations.js` publishes exactly one canonical commit.\n\n' +
    '## Out of scope\n- No unrelated fixture files change.\n'
}

function runScenario(body, extraEnv) {
  const parent = mkdtempSync(join(tmpdir(), 'integration-tx-'))
  roots.push(parent)
  const root = join(parent, 'control root')
  mkdirSync(root)
  git(root, 'init', '-q', '-b', 'main')
  git(root, 'config', 'user.email', 'owner@fixture.invalid')
  git(root, 'config', 'user.name', 'Fixture Owner')
  git(root, 'config', 'commit.gpgsign', 'false')
  for (const dir of ['orchestrator/tasks/backlog', 'orchestrator/tasks/pending',
    'orchestrator/tasks/todo', 'orchestrator/tasks/done',
    'orchestrator/skills/checks/hooks', 'app']) {
    mkdirSync(join(root, dir), { recursive: true })
  }
  // Real todo files, not stubs: the transaction now asks the canonical task
  // validator to prove that a dirty current-stem file is one coherent
  // in-lifecycle task, and a fixture with a one-line body would make every
  // dirty-source assertion below pass or fail for the wrong reason.
  writeFileSync(join(root, 'orchestrator', 'tasks', 'todo', 'TASK_7_probe.md'), canonicalTodo(7, 'probe'))
  writeFileSync(join(root, 'orchestrator', 'tasks', 'todo', 'TASK_8_other.md'), canonicalTodo(8, 'other'))
  writeFileSync(join(root, 'orchestrator', 'tasks', 'INDEX.json'), JSON.stringify({ version: 1, done: [], todo: [] }) + '\n')
  // Provisioning really installs skills into every checkout; a stub that wrote
  // nothing would hide the untracked-artifact handling entirely.
  writeFileSync(join(root, 'orchestrator', 'skills', 'install-skills.sh'),
    '#!/usr/bin/env bash\nmkdir -p "$1/.claude/skills/probe"\nprintf installed > "$1/.claude/skills/probe/SKILL.md"\n')
  mkdirSync(join(root, '.claude', 'skills', 'probe'), { recursive: true })
  writeFileSync(join(root, '.claude', 'skills', 'probe', 'SKILL.md'), 'installed')
  writeFileSync(join(root, 'orchestrator', 'skills', 'checks', 'hooks', 'pre-commit'),
    '#!/bin/sh\nexit 0\n')
  chmodSync(join(root, 'orchestrator', 'skills', 'checks', 'hooks', 'pre-commit'), 0o755)
  // Git treats a regular hook without an executable bit as disabled. Keep one
  // in the wired fixture so the integration owner cannot accidentally invent
  // a stricter optional-hook contract than `git commit` itself.
  writeFileSync(join(root, 'orchestrator', 'skills', 'checks', 'hooks', 'commit-msg'),
    '#!/bin/sh\nexit 91\n', { mode: 0o644 })
  // The template ships this: the whole control cache is untracked runtime
  // state. Without it the pipeline cannot work at all — sealing would see
  // control-owned paths and Integrate would see the cache as the owner's work.
  writeFileSync(join(root, '.gitignore'), 'orchestrator/.cache/\n')
  writeFileSync(join(root, 'app', 'keep.txt'), 'base\n')
  writeFileSync(join(root, 'app', 'gone.txt'), 'delete me\n')
  git(root, 'add', '.')
  git(root, 'commit', '-q', '-m', 'base')
  const stub = join(parent, 'stub-finalizer.cjs')
  writeFileSync(stub, STUB)

  const bodies = Array.isArray(body) ? body : [{ code: body }]
  let out = null
  for (const step of bodies) out = runStep(step, root, parent, stub, extraEnv)
  return { root, parent, out }
}

function runStep(step, root, parent, stub, extraEnv) {
  const script = `
    process.env.ORCHESTRATOR_PROJECT_ROOT = ${JSON.stringify(root)};
    process.env.ORCHESTRATOR_WORKTREE_HOME = ${JSON.stringify(join(parent, '.orchestrator-worktrees'))};
    process.env.FINALIZE_TASK_SCRIPT = ${JSON.stringify(stub)};
    const gateModule = ${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-checkpoints.js'))};
    require.cache[gateModule] = { id: gateModule, filename: gateModule, loaded: true,
      exports: { sealingGate: () => ({ ok: true, checkpoint: { checkpointId: 'cp-fixture' } }) } };
    const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
    const gitMutations = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/git-mutations.js'))});
    const integrations = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/integrations.js'))});
    const taskIntegrity = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-integrity.js'))});
    const paths = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/paths.js'))});
    const { execFileSync } = require('node:child_process');
    const fs = require('node:fs');
    const P = require('node:path');
    const git = (cwd, ...a) => execFileSync('git', a, { cwd, encoding: 'utf8' });
    const ROOT = ${JSON.stringify(root)};
    const STEM = 'TASK_7_probe';
    // The lock the finalizer requires: written by the run at Step 0.5.
    const writeLock = (runId = '1700000000000-r1') => {
      fs.mkdirSync(paths.LOCKS_DIR, { recursive: true });
      const startedAt = '2025-01-01T00:00:00.000Z';
      fs.writeFileSync(P.join(paths.LOCKS_DIR, STEM + '.json'), JSON.stringify({
        version: 1, stem: STEM, stage: 'orchestrator', runId,
        sessionId: 'ws-fixture-session-000000', startedAt,
        owner: { kind: 'agent', id: 'agent:fixture', pid: process.pid,
          processStartId: null, hostname: 'fixture.invalid', startedAt }
      }) + '\\n');
    };
    // A real run writes its Outcome into the control cache under its OWN
    // generation id; a draft with no generation in its name could be published
    // for a candidate it never described.
    const writeDraft = (worktreeId) => {
      fs.mkdirSync(paths.FINALIZATIONS_DIR, { recursive: true });
      fs.writeFileSync(P.join(paths.FINALIZATIONS_DIR, STEM + '.' + worktreeId + '.draft.md'),
        '\\n## Outcome\\n');
    };
    // Provision, do product work in the checkout, seal the candidate.
    const sealed = (mutate) => {
      const provisioned = manager.provision({ stem: STEM, runId: '1700000000000-r1', requestId: '1700000000000-q1',
        sourceRevision: taskIntegrity.validateAction('run', STEM, 'fixture').sourceRevision });
      if (!provisioned.ok) throw new Error('provision failed: ' + JSON.stringify(provisioned));
      const wt = provisioned.executionRoot;
      mutate(wt);
      // The run leaves its Outcome before the candidate is sealed, named by the
      // generation it executed in.
      writeDraft(provisioned.worktreeId);
      const result = manager.seal({ worktreeId: provisioned.worktreeId });
      if (!result.ok) throw new Error('seal failed: ' + JSON.stringify(result));
      return { provisioned, result, wt };
    };
    const productWork = (wt) => {
      fs.writeFileSync(P.join(wt, 'app', 'keep.txt'), 'candidate\\n');
      fs.writeFileSync(P.join(wt, 'app', 'added.txt'), 'new file\\n');
      fs.unlinkSync(P.join(wt, 'app', 'gone.txt'));
    };
    const done = (value) => { process.stdout.write(JSON.stringify(value)); process.exit(0); };
    ${step.code}
  `
  let stdout
  try {
    stdout = execFileSync(process.execPath, ['-e', script], {
      encoding: 'utf8', cwd: root, maxBuffer: 16 * 1024 * 1024,
      env: { ...process.env, ...(extraEnv || {}), ...(step.env || {}) },
    })
  } catch (error) {
    // A step declared as dying IS the crash under test; anything else is a
    // genuine failure and must surface with its output.
    if (!step.mayDie) throw error
    return { killed: true, signal: error.signal || null, status: error.status }
  }
  return stdout ? JSON.parse(stdout) : { killed: false }
}

try {
  check('the happy path publishes exactly one canonical commit under the owner identity', () => {
    const { root, out } = runScenario(`
      writeLock();
      const s = sealed(productWork);
      integrations.begin(STEM, (result) => {
        const record = integrations.readOne(STEM);
        done({
          result: { ok: result.ok, completed: result.completed, commit: result.commit, code: result.code,
            message: result.message, blockers: result.blockers },
          status: record.ok ? record.record.status : null,
          phasesProven: record.ok ? Object.keys(record.record.phases)
            .filter((k) => record.record.phases[k].provenAt !== null).length : 0,
          worktreeStatus: (() => {
            const rows = manager.discover().records.worktrees.active
              .filter((row) => row.record.stem === STEM);
            return rows.map((row) => row.record.status);
          })(),
          candidateRef: s.provisioned.candidateRef,
        });
      });
    `)
    assert.equal(out.result.ok, true, JSON.stringify(out.result))
    assert.equal(out.result.completed, true)
    assert.match(out.result.commit, /^[a-f0-9]{40}$/)
    assert.equal(out.status, 'completed')
    assert.equal(out.phasesProven, 9)
    // §9.6: the generation is released and its branch deleted after Integrate.
    assert.deepEqual(out.worktreeStatus, ['released'])
    assert.equal(refOrEmpty(root, out.candidateRef), '', 'the candidate branch must be gone')

    // Exactly one new commit, on the exact base, with the owner's identity.
    const log = git(root, 'log', '--format=%H %P%n%an <%ae>%n%s', '-n', '1').trim().split('\n')
    assert.equal(log[0].trim().split(' ').length, 2, 'the canonical commit has exactly one parent')
    assert.equal(log[1], 'Fixture Owner <owner@fixture.invalid>')
    assert.equal(log[2], 'TASK_7: probe')
    const body = git(root, 'log', '--format=%B', '-n', '1')
    for (const trailer of ['Task-Stem: TASK_7_probe', 'Run-Id: 1700000000000-r1',
      'Candidate-Diff: sha256:', 'Integration-Id: ig-']) {
      assert.ok(body.includes(trailer), 'missing trailer ' + trailer)
    }
    assert.ok(!/claude/i.test(body), 'canonical history must not mention the tooling')
    // Product candidate AND finalizer artifacts, in one commit.
    // --no-renames: the transaction accounts for PATHS, and rename detection
    // would collapse the todo->done publication into one entry.
    const namesOnly = git(root, 'show', '--name-status', '--no-renames', '--format=', 'HEAD').trim().split('\n').sort()
    assert.deepEqual(namesOnly, [
      'A\tapp/added.txt',
      'A\torchestrator/tasks/done/TASK_7_probe.md',
      'D\tapp/gone.txt',
      'D\torchestrator/tasks/todo/TASK_7_probe.md',
      'M\tapp/keep.txt',
      'M\torchestrator/tasks/INDEX.json',
    ].sort())
    // The working tree agrees with the commit; nothing was left staged.
    assert.equal(git(root, 'status', '--porcelain').trim(), '')
    assert.equal(readFileSync(join(root, 'app', 'keep.txt'), 'utf8'), 'candidate\n')
    // The Outcome draft reached the published task.
    assert.ok(readFileSync(join(root, 'orchestrator', 'tasks', 'done', 'TASK_7_probe.md'), 'utf8')
      .includes('## Outcome'))
  })

  check('completed cleanup never follows a raced finalizations-directory symlink', () => {
    const { out } = runScenario(`
      writeLock();
      const s = sealed(productWork);
      const draftName = STEM + '.' + s.provisioned.worktreeId + '.draft.md';
      const externalDir = P.join(P.dirname(ROOT), 'foreign-finalizations');
      const externalDraft = P.join(externalDir, draftName);
      const parkedDir = paths.FINALIZATIONS_DIR + '.parked';
      fs.mkdirSync(externalDir, { recursive: true });
      fs.writeFileSync(externalDraft, 'foreign draft must survive\\n');
      const release = manager.release;
      manager.release = function (worktreeId) {
        const result = release(worktreeId);
        if (result.ok) {
          fs.renameSync(paths.FINALIZATIONS_DIR, parkedDir);
          fs.symlinkSync(externalDir, paths.FINALIZATIONS_DIR, 'dir');
        }
        return result;
      };
      integrations.begin(STEM, (result) => done({
        ok: result.ok, code: result.code || null,
        externalSurvived: fs.existsSync(externalDraft),
        ownedDraftSurvived: fs.existsSync(P.join(parkedDir, draftName)),
      }));
    `)
    assert.equal(out.ok, true, JSON.stringify(out))
    assert.equal(out.externalSurvived, true,
      'best-effort draft cleanup must never delete through a replaced ancestor')
    assert.equal(out.ownedDraftSurvived, true,
      'an unprovable owned draft is retained instead of following the replacement')
  })

  check('a wired pre-commit net ignores an absent or non-executable optional commit-msg hook', () => {
    const { root, out } = runScenario(`
      writeLock();
      sealed(productWork);
      git(ROOT, 'config', 'core.hooksPath', 'orchestrator/skills/checks/hooks');
      integrations.begin(STEM, (result) => done({ ok: result.ok, code: result.code,
        message: result.message, commit: result.commit || null }));
    `)
    assert.equal(out.ok, true, JSON.stringify(out))
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '2')
  })

  check('prepare-commit-msg, commit-msg and post-commit hooks run in canonical commit order', () => {
    const { parent, out } = runScenario(`
      writeLock();
      sealed(productWork);
      const hookDir = P.join(P.dirname(ROOT), 'canonical-hooks');
      const trace = P.join(P.dirname(ROOT), 'canonical-hook-trace.txt');
      fs.mkdirSync(hookDir, { recursive: true });
      const hook = (name, body) => {
        const file = P.join(hookDir, name);
        fs.writeFileSync(file, '#!/bin/sh\\n' + body + '\\n');
        fs.chmodSync(file, 0o755);
      };
      hook('pre-commit', 'printf "pre-commit\\n" >> "$HOOK_TRACE"');
      const prepareHook = P.join(hookDir, 'prepare-commit-msg');
      fs.writeFileSync(prepareHook, '#!/usr/bin/env node\\n' +
        'const fs=require("node:fs");' +
        'fs.appendFileSync(process.env.HOOK_TRACE,"prepare-commit-msg:"+process.argv[3]+"\\\\n");' +
        'fs.appendFileSync(process.argv[2],"\\\\nPrepared-Hook: yes\\\\n");\\n');
      fs.chmodSync(prepareHook, 0o755);
      hook('commit-msg', 'printf "commit-msg\\n" >> "$HOOK_TRACE"; grep -q "Prepared-Hook: yes" "$1"');
      hook('post-commit', 'printf "post-commit\\n" >> "$HOOK_TRACE"');
      git(ROOT, 'config', 'core.hooksPath', hookDir);
      process.env.HOOK_TRACE = trace;
      integrations.begin(STEM, (result) => done({ ok: result.ok, code: result.code,
        message: result.message, trace: fs.existsSync(trace) ? fs.readFileSync(trace, 'utf8') : '' }));
    `)
    assert.equal(out.ok, true, JSON.stringify(out))
    assert.equal(out.trace, 'pre-commit\nprepare-commit-msg:message\ncommit-msg\npost-commit\n')
    assert.ok(git(join(parent, 'control root'), 'log', '-1', '--format=%B').includes('Prepared-Hook: yes'))
  })

  check('a hook cannot replace pinned transaction bytes on an expected path', () => {
    const { root, out } = runScenario(`
      writeLock();
      sealed(productWork);
      const hookDir = P.join(P.dirname(ROOT), 'mutating-hooks');
      fs.mkdirSync(hookDir, { recursive: true });
      const hook = P.join(hookDir, 'commit-msg');
      fs.writeFileSync(hook, '#!/bin/sh\\nprintf "forged by hook\\n" >> orchestrator/tasks/done/' + STEM + '.md\\ngit add orchestrator/tasks/done/' + STEM + '.md\\n');
      fs.chmodSync(hook, 0o755);
      git(ROOT, 'config', 'core.hooksPath', hookDir);
      integrations.begin(STEM, (result) => done({ ok: result.ok, code: result.code,
        message: result.message }));
    `)
    assert.equal(out.ok, false, JSON.stringify(out))
    assert.equal(out.code, 'INTEGRATION_PREPARED_ARTIFACT_DRIFT')
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '1',
      'same-path hook mutation must be refused before canonical publication')
  })

  check('a second Integrate on a completed transaction never creates a second commit', () => {
    const { root, out } = runScenario(`
      writeLock();
      sealed(productWork);
      integrations.begin(STEM, (first) => {
        integrations.begin(STEM, (second) => {
          done({ first: { ok: first.ok, commit: first.commit },
            second: { ok: second.ok, code: second.code } });
        });
      });
    `)
    assert.equal(out.first.ok, true)
    assert.equal(out.second.ok, false)
    assert.equal(out.second.code, 'INTEGRATION_ALREADY_COMPLETED')
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '2')
  })

  check('an interrupted publication adopts the physical commit instead of publishing a second', () => {
    // The exact crash §10.3 phase 7 exists for: the ref moved, the record did
    // not. Resume must recognise the commit as its own and continue.
    const { root, out } = runScenario(`
      writeLock();
      sealed(productWork);
      integrations.begin(STEM, (first) => {
        const file = P.join(paths.INTEGRATIONS_DIR, STEM + '.json');
        const record = JSON.parse(fs.readFileSync(file, 'utf8'));
        const published = record.commitPin.publishedCommit;
        // Rewind the WAL to "commit-publishing intent recorded, nothing proven".
        record.status = 'active';
        record.phase = 'commit-publishing';
        record.commitPin.publishedCommit = null;
        for (const phase of ['commit-publishing', 'commit-published', 'finalizer-confirming', 'completed']) {
          record.phases[phase].provenAt = null;
        }
        for (const phase of ['commit-published', 'finalizer-confirming', 'completed']) {
          record.phases[phase].intentAt = null;
        }
        const contract = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/integration-record-contract.cjs'))});
        record.recordHash = contract.recordHash(record);
        fs.writeFileSync(file, JSON.stringify(record) + '\\n');
        integrations.resume(STEM, (second) => {
          done({ published, second: { ok: second.ok, commit: second.commit, code: second.code, message: second.message } });
        });
      });
    `)
    assert.equal(out.second.ok, true, JSON.stringify(out.second))
    assert.equal(out.second.commit, out.published, 'the resume must adopt the existing commit')
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '2', 'no second commit may exist')
  })

  check('a dirty path that the candidate also touches blocks with its exact path', () => {
    const { out } = runScenario(`
      writeLock();
      sealed(productWork);
      fs.writeFileSync(P.join(ROOT, 'app', 'keep.txt'), 'the owner was editing this\\n');
      integrations.begin(STEM, (result) => {
        done({ ok: result.ok, code: result.code, blockers: result.blockers });
      });
    `)
    assert.equal(out.ok, false)
    assert.equal(out.code, 'INTEGRATION_BLOCKED')
    const dirty = out.blockers.find((entry) => entry.code === 'dirty-control-root')
    assert.ok(dirty, JSON.stringify(out.blockers))
    assert.deepEqual(dirty.paths, ['app/keep.txt'])
  })

  check('task-source drift supersedes the gate, and another dirty stem is still reported exactly', () => {
    const { out } = runScenario(`
      writeLock();
      sealed(productWork);
      fs.appendFileSync(P.join(ROOT, 'orchestrator/tasks/todo/TASK_7_probe.md'), 'answered\\n');
      const own = integrations.preconditions(STEM);
      const ownStatus = manager.activeRecordFor(STEM).record.status;
      fs.appendFileSync(P.join(ROOT, 'orchestrator/tasks/todo/TASK_8_other.md'), 'foreign edit\\n');
      const foreign = integrations.preconditions(STEM);
      done({
        own: { ok: own.ok, allowed: own.context.dirtyAllowed, blockers: own.blockers.map((b) => b.code), status: ownStatus },
        foreign: { ok: foreign.ok, blockers: foreign.blockers.filter((b) => b.code === 'dirty-control-root') },
      });
    `)
    assert.equal(out.own.ok, false, 'the canonical task is a sealed gate input, not a post-seal free edit')
    assert.ok(out.own.blockers.includes('target-drifted'), JSON.stringify(out.own.blockers))
    assert.equal(out.own.status, 'revalidation-required')
    assert.deepEqual(out.own.allowed, ['orchestrator/tasks/todo/TASK_7_probe.md'])
    assert.equal(out.foreign.ok, false)
    assert.deepEqual(out.foreign.blockers[0].paths, ['orchestrator/tasks/todo/TASK_8_other.md'])
  })

  check('the canonical commit refuses an unwired net and an unproven task source', () => {
    // Two gates the transaction never had. The screenshot-gate net IS a
    // pre-commit hook, so publishing while it is unwired ships an uncompared UI
    // task to done/ under a net everything else believes is active. And the
    // current stem's own todo file was admitted dirty on the strength of its
    // PATH alone, so arbitrary bytes would have been published as its source.
    // Two separate processes because the wiring probe is TTL-cached: a flip
    // inside one process would be answered from the cache, proving nothing.
    const unwired = runScenario(`
      writeLock();
      sealed(productWork);
      fs.writeFileSync(P.join(ROOT, 'orchestrator/project-config.md'),
        '---\\nproductName: Fixture\\nfigmaEnabled: true\\n---\\n');
      const codes = integrations.preconditions(STEM).blockers.map((b) => b.code);
      fs.writeFileSync(P.join(ROOT, 'orchestrator/tasks/todo', STEM + '.md'), 'garbage\\n');
      const unproven = integrations.preconditions(STEM).blockers.map((b) => b.code);
      done({ codes, unproven });
    `).out
    assert.ok(unwired.codes.includes('hooks-unwired'), JSON.stringify(unwired.codes))
    assert.ok(unwired.unproven.includes('task-source-unproven'), JSON.stringify(unwired.unproven))

    const wired = runScenario(`
      writeLock();
      sealed(productWork);
      fs.writeFileSync(P.join(ROOT, 'orchestrator/project-config.md'),
        '---\\nproductName: Fixture\\nfigmaEnabled: true\\n---\\n');
      git(ROOT, 'config', 'core.hooksPath', 'orchestrator/skills/checks/hooks');
      done({ codes: integrations.preconditions(STEM).blockers.map((b) => b.code) });
    `).out
    assert.ok(!wired.codes.includes('hooks-unwired'),
      'wiring the net clears the blocker: ' + JSON.stringify(wired.codes))
  })

  check('foreign staged changes and a moved target both block before any mutation', () => {
    const { out } = runScenario(`
      writeLock();
      sealed(productWork);
      fs.writeFileSync(P.join(ROOT, 'app', 'foreign.txt'), 'staged by someone else\\n');
      git(ROOT, 'add', 'app/foreign.txt');
      const staged = integrations.preconditions(STEM).blockers.map((b) => b.code);
      git(ROOT, 'reset', '-q');
      fs.unlinkSync(P.join(ROOT, 'app', 'foreign.txt'));
      // Move the target: the sweep-commit case the WAL refuses by construction.
      fs.writeFileSync(P.join(ROOT, 'app', 'sweep.txt'), 'target moved\\n');
      git(ROOT, 'add', '.'); git(ROOT, 'commit', '-q', '-m', 'sweep');
      const moved = integrations.preconditions(STEM).blockers.map((b) => b.code);
      done({ staged, moved });
    `)
    assert.ok(out.staged.includes('index-not-clean'), JSON.stringify(out.staged))
    assert.ok(out.moved.includes('target-drifted'), JSON.stringify(out.moved))
  })

  check('a missing git identity blocks Integrate and is never invented', () => {
    const { out } = runScenario(`
      writeLock();
      sealed(productWork);
      // An empty configured value is how an unconfigured identity presents to
      // the mutation owner: it strips every GIT_* variable by design, so a
      // global config cannot be masked with the environment here.
      git(ROOT, 'config', 'user.email', '');
      done({ identity: gitMutations.configuredIdentity(),
        blockers: integrations.preconditions(STEM).blockers.map((b) => b.code) });
    `)
    assert.equal(out.identity, null)
    assert.ok(out.blockers.includes('git-identity-missing'), JSON.stringify(out.blockers))
  })

  check('a refused prepare keeps the transaction resumable and publishes no commit', () => {
    const { root, out } = runScenario(`
      writeLock();
      sealed(productWork);
      integrations.begin(STEM, (result) => {
        const record = integrations.readOne(STEM);
        done({ ok: result.ok, code: result.code,
          status: record.ok ? record.record.status : null,
          phase: record.ok ? record.record.phase : null,
          applied: record.ok ? record.record.phases['product-applied'].provenAt !== null : false });
      });
    `, { STUB_REFUSE: 'prepare' })
    assert.equal(out.ok, false)
    assert.equal(out.code, 'INTEGRATION_PREPARE_FAILED')
    // The product diff IS applied (that is what prepare needs) but nothing is
    // committed and the record stays active for a later resume.
    assert.equal(out.applied, true)
    assert.equal(out.status, 'active')
    assert.equal(out.phase, 'finalizer-preparing')
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '1', 'no canonical commit may exist')
  })

  check('the commit message subject is bounded and deterministic', () => {
    const integrations = require('../server/integrations.js')
    const record = {
      stem: 'TASK_12_a_very_long_title_that_keeps_going_and_going_far_past_the_subject_bound_for_sure',
      runId: '1700000000000-r1', integrationId: 'ig-' + 'a'.repeat(32),
      candidate: { diffHash: 'sha256:' + 'b'.repeat(64) },
    }
    const message = integrations.commitMessage(record)
    const subject = message.split('\n')[0]
    assert.ok(Buffer.byteLength(subject, 'utf8') <= 72, subject)
    assert.ok(subject.startsWith('TASK_12: a very long title'))
    assert.equal(integrations.commitMessage(record), message)
    assert.ok(message.includes('Integration-Id: ig-' + 'a'.repeat(32)))
  })

  check('an active transaction for another stem holds the repository-wide mutex', () => {
    const { out } = runScenario(`
      writeLock();
      sealed(productWork);
      const contract = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/integration-record-contract.cjs'))});
      const clean = integrations.preconditions(STEM).ok;
      // A foreign active record: same shape, different stem.
      const now = new Date().toISOString();
      const phases = {};
      for (const name of contract.PHASES) phases[name] = { intentAt: null, provenAt: null };
      phases.prepared = { intentAt: now, provenAt: now };
      const base = git(ROOT, 'rev-parse', 'HEAD').trim();
      const baseTree = git(ROOT, 'rev-parse', 'HEAD^{tree}').trim();
      const foreign = {
        version: 1, integrationId: 'ig-' + 'c'.repeat(32), stem: 'TASK_8_other',
        runId: '1700000000000-r9', worktreeId: 'wt-' + 'd'.repeat(32),
        phase: 'prepared', status: 'active',
        candidate: { commit: base, tree: baseTree, diffHash: 'sha256:' + '1'.repeat(64),
          receiptHash: 'sha256:' + '2'.repeat(64) },
        target: { ref: 'refs/heads/main', baseCommit: base, baseTree: baseTree },
        controlSnapshot: { headCommit: base, dirtyAllowedPaths: [] },
        commitPin: { stagedTreeHash: null, messageHash: null, expectedParent: null, publishedCommit: null },
        finalizerPrepared: null, phases: phases,
        owner: { hostname: 'fixture', pid: 4242, processStartId: null, startedAt: now },
        createdAt: now, updatedAt: now, recordHash: 'sha256:' + '0'.repeat(64)
      };
      foreign.recordHash = contract.recordHash(foreign);
      fs.mkdirSync(paths.INTEGRATIONS_DIR, { recursive: true });
      fs.writeFileSync(P.join(paths.INTEGRATIONS_DIR, 'TASK_8_other.json'), JSON.stringify(foreign) + '\\n');
      const blocked = integrations.preconditions(STEM);
      done({ clean, blocked: blocked.blockers.map((b) => b.code), active: integrations.activeIssue() });
    `)
    assert.equal(out.clean, true)
    assert.ok(out.blocked.includes('integration-busy'), JSON.stringify(out.blocked))
    assert.equal(out.active.active, true)
    assert.equal(out.active.stem, 'TASK_8_other')
  })

  check('WAL publication is cross-process atomic and a dead guard is reclaimed by identity, not age', () => {
    const { out } = runScenario(`
      const guardPath = ${JSON.stringify(join(repoRoot, 'orchestrator/site/server/integration-publication-guard.js'))};
      const guard = require(guardPath);
      const childCode = 'const g=require(' + JSON.stringify(guardPath) + ');' +
        'const h=g.acquire();process.stdout.write(JSON.stringify({ok:h.ok,code:h.code||null}));';
      const held = guard.acquire();
      if (!held.ok) throw new Error('first guard acquisition failed: ' + JSON.stringify(held));
      const projected = integrations.activeIssue();
      const blocked = JSON.parse(execFileSync(process.execPath, ['-e', childCode], {
        cwd: ROOT, encoding: 'utf8', env: process.env,
      }));
      if (!guard.release(held)) throw new Error('owned guard release failed');
      const orphan = JSON.parse(execFileSync(process.execPath, ['-e', childCode], {
        cwd: ROOT, encoding: 'utf8', env: process.env,
      }));
      const recovered = guard.acquire();
      const recoveredOk = recovered.ok;
      if (recovered.ok && !guard.release(recovered)) throw new Error('recovered guard release failed');
      done({ held: held.ok, projected, blocked, orphan, recoveredOk,
        residue: fs.existsSync(guard.FILE) });
    `)
    assert.equal(out.held, true)
    assert.deepEqual(out.projected, { active: true, reason: 'integration-publication-active', stem: null })
    assert.deepEqual(out.blocked, { ok: false, code: 'INTEGRATION_PUBLICATION_BUSY' })
    assert.deepEqual(out.orphan, { ok: true, code: null })
    assert.equal(out.recoveredOk, true)
    assert.equal(out.residue, false)
  })

  // §21 crash matrix, physically faithful: the transaction driver is killed
  // with SIGKILL at an exact boundary, and a SECOND process resumes against
  // whatever the first one really left on disk. No record is hand-edited.
  const SETUP = `
    writeLock();
    sealed(productWork);
    integrations.begin(STEM, (result) => done({ ok: result.ok, code: result.code, message: result.message,
      commit: result.commit || null }));
  `
  const RESUME = `
    integrations.resume(STEM, (result) => {
      const after = integrations.readOne(STEM);
      done({ ok: result.ok, code: result.code, message: result.message, commit: result.commit || null,
        status: after.ok ? after.record.status : null });
    });
  `

  check('resume rejects a replacement receipt even when it names the same candidate tree', () => {
    const { root, out } = runScenario([
      { code: SETUP, env: { STUB_REFUSE: 'prepare' } },
      { code: `
        const wal = integrations.readOne(STEM).record;
        const receiptFile = P.join(paths.WORKTREE_RECORDS_DIR, '.receipts', wal.worktreeId + '.json');
        const receipt = JSON.parse(fs.readFileSync(receiptFile, 'utf8'));
        receipt.candidateCommit = git(ROOT, 'commit-tree', receipt.candidateTree,
          '-p', receipt.baseCommit, '-m', 'foreign receipt replacement').trim();
        const candidateContract = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/candidate-receipt-contract.cjs'))});
        receipt.receiptHash = candidateContract.receiptHash(receipt);
        fs.writeFileSync(receiptFile, JSON.stringify(receipt) + '\\n');
        integrations.resume(STEM, (result) => {
          const after = integrations.readOne(STEM);
          done({ ok: result.ok, code: result.code, message: result.message,
            status: after.ok ? after.record.status : null });
        });
      ` },
    ])
    assert.equal(out.ok, false, JSON.stringify(out))
    assert.equal(out.code, 'INTEGRATION_RECOVERY_REQUIRED')
    assert.equal(out.status, 'recovery-required')
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '1')
  })

  check('preconditions bind the canonical task lock to the sealed candidate run', () => {
    const { root, out } = runScenario(`
      writeLock('1700000000000-foreign');
      sealed(productWork);
      const verdict = integrations.preconditions(STEM);
      done({ ok: verdict.ok, blockers: verdict.blockers.map((entry) => entry.code) });
    `)
    assert.equal(out.ok, false, JSON.stringify(out))
    assert.ok(out.blockers.includes('task-lock-owner-mismatch'), JSON.stringify(out.blockers))
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '1', 'no product effect may precede lock ownership proof')
  })

  check('kill -9 after the finalizer prepared resumes into exactly one commit', () => {
    const { root, out } = runScenario([
      { code: SETUP, env: { STUB_KILL_DRIVER_AFTER: 'prepare' }, mayDie: true },
      { code: RESUME },
    ])
    assert.equal(out.ok, true, JSON.stringify(out))
    assert.equal(out.status, 'completed')
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '2', 'exactly one canonical commit')
    assert.equal(git(root, 'status', '--porcelain').trim(), '')
    assert.equal(git(root, 'show', '--name-only', '--format=', 'HEAD').includes('orchestrator/tasks/done/TASK_7_probe.md'), true)
  })

  check('kill -9 after the finalizer confirmed resumes without a second commit', () => {
    const { root, out } = runScenario([
      { code: SETUP, env: { STUB_KILL_DRIVER_AFTER: 'confirm' }, mayDie: true },
      { code: RESUME },
    ])
    assert.equal(out.ok, true, JSON.stringify(out))
    assert.equal(out.status, 'completed')
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '2')
    assert.equal(git(root, 'status', '--porcelain').trim(), '')
  })

  check('an unsafe task-lock entry appearing after confirm is never accepted as proven absence', () => {
    const { root, out } = runScenario([
      { code: SETUP, env: { STUB_REPLACE_LOCK_AFTER_CONFIRM: 'symlink' } },
    ])
    const wal = JSON.parse(readFileSync(join(root,
      'orchestrator/.cache/tasks/integrations/TASK_7_probe.json'), 'utf8'))
    assert.equal(out.ok, false, JSON.stringify(out))
    assert.equal(out.code, 'INTEGRATION_RECOVERY_REQUIRED')
    assert.equal(wal.status, 'recovery-required')
    assert.equal(readFileSync(join(root,
      'orchestrator/.cache/tasks/locks/TASK_7_probe.json'), 'utf8'), 'foreign owner\n')
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '2', 'the canonical commit remains singular')
  })

  check('a stale same-stem driver cannot regress a completed integration WAL', () => {
    const { root, out } = runScenario([
      { code: `
        writeLock();
        sealed(productWork);
        const raceRoot = P.join(paths.INTEGRATIONS_DIR, '.same-stem-race');
        fs.mkdirSync(raceRoot, { recursive: true });
        const applied = P.join(raceRoot, 'applied');
        const gate = P.join(raceRoot, 'gate');
        const output = P.join(raceRoot, 'out.json');
        const child = require('node:child_process').spawn(process.execPath,
          ['-e', ${JSON.stringify(STALE_INTEGRATION_DRIVER)}], {
            cwd: ROOT, detached: true, stdio: 'ignore',
            env: { ...process.env,
              ORCHESTRATOR_PROJECT_ROOT: ROOT,
              INTEGRATION_TEST_REPO: ${JSON.stringify(repoRoot)},
              INTEGRATION_TEST_STEM: STEM,
              INTEGRATION_TEST_APPLIED: applied,
              INTEGRATION_TEST_GATE: gate,
              INTEGRATION_TEST_OUT: output,
            },
          });
        child.unref();
        const wait = new Int32Array(new SharedArrayBuffer(4));
        for (let i = 0; i < 500 && !fs.existsSync(applied); i++) Atomics.wait(wait, 0, 0, 20);
        if (!fs.existsSync(applied)) throw new Error('stale integration driver did not reach the apply boundary');
        done({ started: true });
      ` },
      { code: `
        const raceRoot = P.join(paths.INTEGRATIONS_DIR, '.same-stem-race');
        const gate = P.join(raceRoot, 'gate');
        const output = P.join(raceRoot, 'out.json');
        integrations.begin(STEM, (result) => {
          const beforeRelease = integrations.readOne(STEM);
          fs.writeFileSync(gate, 'release\\n');
          const wait = new Int32Array(new SharedArrayBuffer(4));
          for (let i = 0; i < 500 && !fs.existsSync(output); i++) Atomics.wait(wait, 0, 0, 20);
          if (!fs.existsSync(output)) throw new Error('stale integration driver did not settle');
          const stale = JSON.parse(fs.readFileSync(output, 'utf8'));
          const afterRelease = integrations.readOne(STEM);
          done({ current: { ok: result.ok, code: result.code || null }, stale,
            beforeStatus: beforeRelease.ok ? beforeRelease.record.status : beforeRelease.code,
            afterStatus: afterRelease.ok ? afterRelease.record.status : afterRelease.code });
        });
      ` },
    ])
    assert.equal(out.current.ok, true, JSON.stringify(out))
    assert.equal(out.beforeStatus, 'completed')
    assert.equal(out.stale.ok, false, JSON.stringify(out))
    assert.equal(out.afterStatus, 'completed', 'the stale writer must not replace the terminal WAL generation')
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '2', 'exactly one canonical commit')
  })

  check('a driver killed before it recorded the published commit adopts that commit', () => {
    // The narrowest window in the whole transaction: the ref moved, the record
    // did not. Reconstructed by clearing exactly the proof the crashed process
    // never got to write, against the real post-commit filesystem.
    const { root, out } = runScenario([
      { code: SETUP },
      { code: `
        const contract = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/integration-record-contract.cjs'))});
        const file = P.join(paths.INTEGRATIONS_DIR, STEM + '.json');
        const record = JSON.parse(fs.readFileSync(file, 'utf8'));
        const published = record.commitPin.publishedCommit;
        record.status = 'active';
        record.commitPin.publishedCommit = null;
        for (const phase of ['commit-publishing', 'commit-published', 'finalizer-confirming', 'completed']) {
          record.phases[phase].provenAt = null;
        }
        for (const phase of ['commit-published', 'finalizer-confirming', 'completed']) {
          record.phases[phase].intentAt = null;
        }
        record.phase = 'commit-publishing';
        record.recordHash = contract.recordHash(record);
        fs.writeFileSync(file, JSON.stringify(record) + '\\n');
        integrations.resume(STEM, (result) => {
          const after = integrations.readOne(STEM);
          done({ published, ok: result.ok, code: result.code, message: result.message,
            commit: result.commit || null, status: after.ok ? after.record.status : null });
        });
      ` },
    ])
    assert.equal(out.ok, true, JSON.stringify(out))
    assert.equal(out.commit, out.published, 'the existing commit must be adopted, never re-published')
    assert.equal(out.status, 'completed')
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '2')
  })

  check('a gate refusal leaves the transaction resumable and a later Integrate completes it', () => {
    const { root, out } = runScenario([
      { code: SETUP, env: { STUB_REFUSE: 'prepare' } },
      { code: RESUME },
    ])
    assert.equal(out.ok, true, JSON.stringify(out))
    assert.equal(out.status, 'completed')
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '2')
  })

  check('a foreign commit with the same parent and tree is never adopted as ours', () => {
    // The background sweep in this repository commits whatever is staged. Such
    // a commit has the transaction's exact parent AND exact tree, so parent and
    // tree alone cannot prove ownership — only the pinned message can.
    const { root, out } = runScenario([
      { code: SETUP },
      { code: `
        const contract = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/integration-record-contract.cjs'))});
        const file = P.join(paths.INTEGRATIONS_DIR, STEM + '.json');
        const record = JSON.parse(fs.readFileSync(file, 'utf8'));
        const ours = record.commitPin.publishedCommit;
        const tree = git(ROOT, 'rev-parse', ours + '^{tree}').trim();
        // Rewind to "commit-publishing intent recorded, nothing proven"...
        record.status = 'active';
        record.commitPin.publishedCommit = null;
        for (const phase of ['commit-publishing', 'commit-published', 'finalizer-confirming', 'completed']) {
          record.phases[phase].provenAt = null;
        }
        for (const phase of ['commit-published', 'finalizer-confirming', 'completed']) {
          record.phases[phase].intentAt = null;
        }
        record.phase = 'commit-publishing';
        record.recordHash = contract.recordHash(record);
        fs.writeFileSync(file, JSON.stringify(record) + '\\n');
        // ...and let a THIRD PARTY publish the same content under its own message.
        const foreign = git(ROOT, 'commit-tree', tree, '-p', record.target.baseCommit,
          '-m', 'chore: automatic sweep').trim();
        git(ROOT, 'update-ref', 'refs/heads/main', foreign, ours);
        const adopted = integrations.publishedCommitMatching(record);
        integrations.resume(STEM, (result) => {
          done({ ours, foreign, adopted, ok: result.ok, code: result.code, message: result.message });
        });
      ` },
    ])
    assert.equal(out.adopted, null, 'a foreign commit must never be adopted')
    assert.equal(out.ok, false, JSON.stringify(out))
    // The target now carries someone else's commit, which is exactly the
    // unprovable state the transaction must refuse rather than absorb.
    assert.equal(git(root, 'rev-parse', 'HEAD').trim(), out.foreign)
  })

  check('a foreign control artifact written during prepare blocks with its exact path', () => {
    const { root, out } = runScenario([
      { code: `
        writeLock();
        sealed(productWork);
        // The owner edits an UNRELATED task while the transaction runs.
        fs.writeFileSync(P.join(ROOT, 'orchestrator/tasks/todo/TASK_8_other.md'), '# Task 8 edited\\n');
        integrations.begin(STEM, (result) => done({ ok: result.ok, code: result.code,
          message: result.message, blockers: result.blockers || [] }));
      ` },
    ])
    assert.equal(out.ok, false, JSON.stringify(out))
    assert.equal(out.code, 'INTEGRATION_BLOCKED')
    const dirty = out.blockers.find((entry) => entry.code === 'dirty-control-root')
    assert.ok(dirty, JSON.stringify(out.blockers))
    assert.ok(dirty.paths.includes('orchestrator/tasks/todo/TASK_8_other.md'), JSON.stringify(dirty.paths))
    assert.equal(git(root, 'rev-list', '--count', 'HEAD').trim(), '1', 'nothing may be committed')
  })

} finally {
  for (const root of roots) { try { rmSync(root, { recursive: true, force: true }) } catch (error) {} }
}

console.log(`\nintegration-transaction: ${checks} checks passed`)
