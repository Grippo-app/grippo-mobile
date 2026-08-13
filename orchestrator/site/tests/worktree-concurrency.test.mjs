#!/usr/bin/env node
// Phase 6 exit criterion (pipeline improvement 01, §20): two disjoint tasks
// execute in parallel while integrations stay serialized and reproducible.
// These checks drive the REAL manager, the REAL git mutation owner and the
// REAL lease system against a real repository — the parallelism is physical,
// not asserted from source text.

import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const repoRoot = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..')

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }
const roots = []
function git(cwd, ...args) {
  return execFileSync('git', args, { cwd, encoding: 'utf8', env: { ...process.env, GIT_TERMINAL_PROMPT: '0' } })
}

// One fixture repository with TWO todo tasks, so a scenario can provision both.
function runScenario(body) {
  const parent = mkdtempSync(join(tmpdir(), 'worktree-concurrency-'))
  roots.push(parent)
  const root = join(parent, 'control')
  mkdirSync(root)
  git(root, 'init', '-q', '-b', 'main')
  git(root, 'config', 'user.email', 'owner@fixture.invalid')
  git(root, 'config', 'user.name', 'Fixture Owner')
  git(root, 'config', 'commit.gpgsign', 'false')
  for (const dir of ['orchestrator/tasks/todo', 'orchestrator/tasks/done', 'orchestrator/skills', 'app']) {
    mkdirSync(join(root, dir), { recursive: true })
  }
  writeFileSync(join(root, '.gitignore'), 'orchestrator/.cache/\n')
  writeFileSync(join(root, 'orchestrator', 'tasks', 'todo', 'TASK_7_first.md'), '# Task 7 first\n')
  writeFileSync(join(root, 'orchestrator', 'tasks', 'todo', 'TASK_8_second.md'), '# Task 8 second\n')
  writeFileSync(join(root, 'orchestrator', 'skills', 'install-skills.sh'),
    '#!/usr/bin/env bash\nmkdir -p "$1/.claude/skills/probe"\nprintf installed > "$1/.claude/skills/probe/SKILL.md"\n')
  // Real template checkouts already track the installed skill surface. The
  // installer rewrites the same bytes; release must never delete those tracked
  // paths merely to make `git worktree remove` succeed.
  mkdirSync(join(root, '.claude', 'skills', 'probe'), { recursive: true })
  writeFileSync(join(root, '.claude', 'skills', 'probe', 'SKILL.md'), 'installed')
  writeFileSync(join(root, 'app', 'keep.txt'), 'base\n')
  git(root, 'add', '.')
  git(root, 'commit', '-q', '-m', 'base')

  const script = `
    process.env.ORCHESTRATOR_PROJECT_ROOT = ${JSON.stringify(root)};
    process.env.ORCHESTRATOR_WORKTREE_HOME = ${JSON.stringify(join(parent, '.orchestrator-worktrees'))};
    const gateModule = ${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-checkpoints.js'))};
    require.cache[gateModule] = { id: gateModule, filename: gateModule, loaded: true,
      exports: { sealingGate: () => ({ ok: true, checkpoint: { checkpointId: 'cp-fixture' } }) } };
    const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
    const taskIntegrity = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-integrity.js'))});
    const integrations = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/integrations.js'))});
    const finalizations = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/finalizations.js'))});
    const leases = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/writer-leases.cjs'))});
    const fs = require('node:fs');
    const P = require('node:path');
    const { execFileSync } = require('node:child_process');
    const git = (cwd, ...a) => execFileSync('git', a, { cwd, encoding: 'utf8' });
    const ROOT = ${JSON.stringify(root)};
    const provision = (stem, n) => manager.provision({
      stem, runId: '170000000000' + n + '-r' + n, requestId: '170000000000' + n + '-q' + n,
      sourceRevision: taskIntegrity.validateAction('run', stem, 'fixture').sourceRevision,
    });
    const done = (value) => { process.stdout.write(JSON.stringify(value)); process.exit(0); };
    ${body}
  `
  const out = execFileSync(process.execPath, ['-e', script], {
    encoding: 'utf8', cwd: root, maxBuffer: 16 * 1024 * 1024,
  })
  return { root, parent, out: JSON.parse(out) }
}

try {
  check('two disjoint tasks each get their own checkout and candidate branch', () => {
    const { root, out } = runScenario(`
      const first = provision('TASK_7_first', 1);
      const second = provision('TASK_8_second', 2);
      done({
        first: { ok: first.ok, root: first.executionRoot, ref: first.candidateRef, id: first.worktreeId },
        second: { ok: second.ok, root: second.executionRoot, ref: second.candidateRef, id: second.worktreeId },
      });
    `)
    assert.equal(out.first.ok, true, JSON.stringify(out.first))
    assert.equal(out.second.ok, true, JSON.stringify(out.second))
    // Physically distinct trees, ids and branches — the whole point of the phase.
    assert.notEqual(out.first.root, out.second.root)
    assert.notEqual(out.first.id, out.second.id)
    assert.notEqual(out.first.ref, out.second.ref)
    // Both are live linked worktrees of the SAME repository.
    const listed = git(root, 'worktree', 'list', '--porcelain')
    assert.ok(listed.includes(out.first.root), 'first checkout must be a linked worktree')
    assert.ok(listed.includes(out.second.root), 'second checkout must be a linked worktree')
  })

  check('work in one checkout is invisible to the other', () => {
    const { out } = runScenario(`
      const first = provision('TASK_7_first', 1);
      const second = provision('TASK_8_second', 2);
      fs.writeFileSync(P.join(first.executionRoot, 'app', 'keep.txt'), 'first edit\\n');
      fs.writeFileSync(P.join(second.executionRoot, 'app', 'only-second.txt'), 'second only\\n');
      done({
        firstSeesOwn: fs.readFileSync(P.join(first.executionRoot, 'app', 'keep.txt'), 'utf8'),
        secondSeesBase: fs.readFileSync(P.join(second.executionRoot, 'app', 'keep.txt'), 'utf8'),
        firstSeesSecondFile: fs.existsSync(P.join(first.executionRoot, 'app', 'only-second.txt')),
        controlUntouched: fs.readFileSync(P.join(ROOT, 'app', 'keep.txt'), 'utf8'),
      });
    `)
    assert.equal(out.firstSeesOwn, 'first edit\n')
    assert.equal(out.secondSeesBase, 'base\n', 'the second checkout must still hold the base bytes')
    assert.equal(out.firstSeesSecondFile, false)
    assert.equal(out.controlUntouched, 'base\n', 'the control root is never written by a run')
  })

  check('board-task writers coexist per stem while integrations stay repository-wide', () => {
    const { out } = runScenario(`
      const writers = P.join(ROOT, 'orchestrator/.cache/tasks/finalizations/.writers');
      // Two different stems: both admitted.
      const a = finalizations.beginMutation({ kind: 'task-session', stem: 'TASK_7_first',
        sessionId: finalizations.createWriterSessionId(), key: 'task:TASK_7_first' });
      const b = finalizations.beginMutation({ kind: 'task-session', stem: 'TASK_8_second',
        sessionId: finalizations.createWriterSessionId(), key: 'task:TASK_8_second' });
      // The same stem twice: refused.
      const dup = finalizations.beginMutation({ kind: 'task-session', stem: 'TASK_7_first',
        sessionId: finalizations.createWriterSessionId(), key: 'task:TASK_7_first' });
      const activeAfter = leases.scan(writers, ROOT).active.length;
      if (a.ok) finalizations.endMutation(a.handle);
      if (b.ok) finalizations.endMutation(b.handle);
      done({ a: a.ok, b: b.ok, dup: dup.ok, dupError: dup.error, activeAfter });
    `)
    assert.equal(out.a, true)
    assert.equal(out.b, true, 'a second stem must be admitted alongside the first')
    assert.equal(out.dup, false, 'one task stays single-writer')
    assert.equal(out.activeAfter, 2, 'the refused acquisition withdraws its own lease')
  })

  check('a second integration is refused while one is mid-flight', () => {
    const { out } = runScenario(`
      const contract = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/integration-record-contract.cjs'))});
      const base = git(ROOT, 'rev-parse', 'HEAD').trim();
      const baseTree = git(ROOT, 'rev-parse', 'HEAD^{tree}').trim();
      const at = new Date().toISOString();
      const phases = {};
      for (const name of contract.PHASES) phases[name] = { intentAt: null, provenAt: null };
      phases.prepared = { intentAt: at, provenAt: at };
      const record = {
        version: 1, integrationId: 'ig-' + 'c'.repeat(32), stem: 'TASK_7_first',
        runId: '1700000000001-r1', worktreeId: 'wt-' + 'd'.repeat(32),
        phase: 'prepared', status: 'active',
        candidate: { commit: base, tree: baseTree, diffHash: 'sha256:' + '1'.repeat(64),
          receiptHash: 'sha256:' + '2'.repeat(64) },
        target: { ref: 'refs/heads/main', baseCommit: base, baseTree },
        controlSnapshot: { headCommit: base, dirtyAllowedPaths: [] },
        commitPin: { stagedTreeHash: null, messageHash: null, expectedParent: null, publishedCommit: null },
        finalizerPrepared: null, phases,
        owner: { hostname: 'fixture', pid: process.pid, processStartId: null, startedAt: at },
        createdAt: at, updatedAt: at, recordHash: 'sha256:' + '0'.repeat(64),
      };
      record.recordHash = contract.recordHash(record);
      const dir = P.join(ROOT, 'orchestrator/.cache/tasks/integrations');
      fs.mkdirSync(dir, { recursive: true });
      fs.writeFileSync(P.join(dir, 'TASK_7_first.json'), JSON.stringify(record) + '\\n');
      // The OTHER task may still run, but it may not integrate.
      const otherBlocked = integrations.preconditions('TASK_8_second').blockers.map((b) => b.code);
      const mutation = finalizations.mutationBlocked('TASK_8_second');
      done({ otherBlocked, mutation, active: integrations.activeIssue() });
    `)
    assert.ok(out.otherBlocked.includes('integration-busy'), JSON.stringify(out.otherBlocked))
    assert.equal(out.active.active, true)
    assert.equal(out.active.stem, 'TASK_7_first')
    // A live integration blocks every board mutation: publication is global.
    assert.equal(out.mutation, true)
  })

  check('the §20 metrics report the state the owner needs before raising the cap', () => {
    const { out } = runScenario(`
      provision('TASK_7_first', 1);
      provision('TASK_8_second', 2);
      done(manager.metrics());
    `)
    assert.equal(out.unavailable, null)
    assert.equal(out.generations.total, 2)
    assert.equal(out.generations.materialized, 2)
    assert.equal(out.generations.revalidationRequired, 0)
    assert.equal(out.rates.revalidation, 0)
    // Provisioning latency is measured from the mutation owner's own receipts.
    assert.equal(out.provisioning.measured, 2)
    assert.ok(Number.isFinite(out.provisioning.medianMs) && out.provisioning.medianMs >= 0)
    assert.ok(Number.isFinite(out.disk.freeBytes) && out.disk.freeBytes > 0)
    assert.equal(out.disk.belowFloor, false)
  })

  check('only a key-provable isolated lease is exempt from the publication gate', () => {
    // The second review round broke the first cut of this rule. `task-session`
    // is ALSO the lease kind for prep/answers/drop/reopen, and §9.1 gives a
    // worktree only to `run` — those actions execute with cwd = the control
    // root and rewrite orchestrator/tasks/**. Exempting a board-task lease
    // because its stem differs therefore waves a real control writer through,
    // and the transaction ends up half-applied with an active WAL. The lease
    // carries no execution binding, so the ONLY provable exemption is the class
    // whose own key names a checkout or a device.
    const { out } = runScenario(`
      const DIR = P.join(ROOT, 'orchestrator/.cache/tasks/finalizations/.writers');
      const acquire = (spec) => leases.acquire(DIR, Object.assign({
        sessionId: leases.createSessionId(), ownerPid: process.pid, rootDir: ROOT }, spec));
      const quiet = finalizations.controlWriterIssue();
      // Isolated by key: an app-run's build, device and bundle holders. None of
      // them can reach the control tree, so a publication they overlap must not
      // be refused — that would be the blanket serialization §16 forbids.
      const build = acquire({ kind: 'execution-writer', key: 'execution:wt-0000000000000000000000000000000f' });
      const device = acquire({ kind: 'resource-writer', key: 'device:android:emulator-5554' });
      const bundle = acquire({ kind: 'resource-writer', key: 'bundle:android:com.example.app' });
      const isolatedOnly = finalizations.controlWriterIssue();
      // A board-task writer for ANOTHER stem is NOT exempt: it may be a prep.
      const foreign = acquire({ kind: 'task-session', stem: 'TASK_8_second', key: 'task:TASK_8_second' });
      const withForeignTask = finalizations.controlWriterIssue();
      leases.release(foreign);
      const control = acquire({ kind: 'workspace-session', key: 'figma:screens:TASK_9_other' });
      const withControl = finalizations.controlWriterIssue();
      leases.release(control); leases.release(bundle); leases.release(device); leases.release(build);
      done({ quiet, isolatedOnly,
        withForeignTask: withForeignTask && withForeignTask.code,
        withControl: withControl && withControl.code });
    `)
    assert.equal(out.quiet, null, 'a quiet workspace has no control writer')
    assert.equal(out.isolatedOnly, null, 'an app-run build, device and bundle are not control writers')
    assert.equal(out.withForeignTask, 'WORKSPACE_WRITER_ACTIVE',
      'a board-task writer for another stem may be a control-root prep and must block')
    assert.equal(out.withControl, 'WORKSPACE_WRITER_ACTIVE', 'a workspace session still blocks publication')
  })

  check('the generation cannot be released while an app-run builds inside it', () => {
    // §16: "execution A is incompatible with cleanup A". The build output is
    // gitignored, so `git worktree remove` would NOT refuse — it would delete
    // the checkout underneath a live build. Proven at the one place that
    // deletes, so the integration's release phase and the operator's explicit
    // release are both covered.
    const { out } = runScenario(`
      const DIR = P.join(ROOT, 'orchestrator/.cache/tasks/finalizations/.writers');
      const made = provision('TASK_7_first', 1);
      const held = leases.acquire(DIR, { kind: 'execution-writer',
        key: leases.executionLeaseKeyFor(made.worktreeId),
        sessionId: leases.createSessionId(), ownerPid: process.pid, rootDir: ROOT });
      const refused = manager.release(made.worktreeId);
      const stillThere = fs.existsSync(made.executionRoot);
      leases.release(held);
      const allowed = manager.release(made.worktreeId);
      done({ provisioned: made.ok === true, refused: refused.code, stillThere,
        allowed: allowed.ok === true, gone: !fs.existsSync(made.executionRoot) });
    `)
    assert.equal(out.provisioned, true, 'the fixture generation was materialized')
    assert.equal(out.refused, 'RELEASE_EXECUTION_BUSY', 'cleanup is refused while the build lease is held')
    assert.equal(out.stillThere, true, 'the checkout survives the refused cleanup')
    assert.equal(out.allowed, true, 'the same release succeeds once the build lease clears')
    assert.equal(out.gone, true, 'and the checkout is actually removed')
  })

  check('release preserves every unproven byte below the installed-skill prefix', () => {
    const { out } = runScenario(`
      const made = provision('TASK_7_first', 1);
      const foreign = P.join(made.executionRoot, '.claude/skills/probe/foreign-note.txt');
      fs.writeFileSync(foreign, 'must survive\\n');
      const released = manager.release(made.worktreeId);
      done({ code: released.code || null, checkoutKept: fs.existsSync(made.executionRoot),
        foreignKept: fs.existsSync(foreign),
        foreignBytes: fs.existsSync(foreign) ? fs.readFileSync(foreign, 'utf8') : null });
    `)
    assert.equal(out.code, 'MUTATION_REMOVE_FAILED',
      'an untracked byte must make unforced worktree removal refuse')
    assert.equal(out.checkoutKept, true)
    assert.equal(out.foreignKept, true, 'release must not recursively delete an unproven byte')
    assert.equal(out.foreignBytes, 'must survive\n')
  })

  check('release refuses an unprovable candidate receipt before any physical cleanup', () => {
    const { out } = runScenario(`
      const made = provision('TASK_7_first', 1);
      const receiptDir = P.join(ROOT, 'orchestrator/.cache/tasks/worktrees/.receipts');
      fs.mkdirSync(receiptDir, { recursive: true });
      fs.writeFileSync(P.join(receiptDir, made.worktreeId + '.json'), '{not-json\\n');
      const refused = manager.release(made.worktreeId);
      let ref = null;
      try { ref = git(ROOT, 'rev-parse', '-q', '--verify', made.candidateRef).trim(); } catch (error) {}
      done({ code: refused.code, checkoutKept: fs.existsSync(made.executionRoot), ref });
    `)
    assert.equal(out.code, 'RELEASE_RECEIPT_UNSAFE')
    assert.equal(out.checkoutKept, true)
    assert.match(out.ref || '', /^[a-f0-9]{40}$/)
  })

  check('discovery and the stem release surface agree on recovery ownership', () => {
    const { out } = runScenario(`
      const made = provision('TASK_7_first', 1);
      const foreign = P.join(made.executionRoot, 'foreign.txt');
      fs.writeFileSync(foreign, 'hold\\n');
      const first = manager.release(made.worktreeId);
      const projection = manager.discover();
      const row = projection.worktrees.find((candidate) => candidate.path === made.executionRoot);
      fs.unlinkSync(foreign);
      const throughStem = manager.releaseFor('TASK_7_first');
      done({ firstCode: first.code, classification: row && row.classification,
        findings: projection.findings.map((finding) => finding.code),
        throughStemOk: throughStem.ok === true,
        checkoutGone: !fs.existsSync(made.executionRoot) });
    `)
    assert.equal(out.firstCode, 'MUTATION_REMOVE_FAILED')
    assert.equal(out.classification, 'managed',
      'the same recovery record that authorizes release must own its exact checkout in discovery')
    assert.ok(!out.findings.includes('WORKTREE_HOME_SQUATTER'))
    assert.ok(!out.findings.includes('MANAGER_NAMESPACE_BRANCH_FOREIGN'))
    assert.equal(out.throughStemOk, true,
      'the board stem surface must reach the recovery generation that direct release created')
    assert.equal(out.checkoutGone, true)
  })

  check('release closes the lease race after its durable lifecycle claim', () => {
    const { out } = runScenario(`
      const made = provision('TASK_7_first', 1);
      const writerDir = P.join(ROOT, 'orchestrator/.cache/tasks/finalizations/.writers');
      const originalScan = leases.scan;
      let scans = 0, build = null;
      leases.scan = function () {
        scans += 1;
        if (scans === 2) {
          build = leases.acquire(writerDir, { kind: 'execution-writer',
            key: 'execution:' + made.worktreeId, sessionId: leases.createSessionId(),
            ownerPid: process.pid, rootDir: ROOT });
        }
        return originalScan.apply(this, arguments);
      };
      const refused = manager.release(made.worktreeId);
      leases.scan = originalScan;
      const recordAfterRefusal = manager.activeRecordFor('TASK_7_first');
      const checkoutAfterRefusal = fs.existsSync(made.executionRoot);
      leases.release(build);
      const retried = manager.release(made.worktreeId);
      done({ refusedCode: refused.code, scans,
        restoredStatus: recordAfterRefusal.record && recordAfterRefusal.record.status,
        checkoutAfterRefusal, retriedOk: retried.ok });
    `)
    assert.equal(out.refusedCode, 'RELEASE_EXECUTION_BUSY')
    assert.ok(out.scans >= 2, 'release must scan on both sides of the claim')
    assert.equal(out.restoredStatus, 'ready', 'a pre-effect lease race restores the exact prior state')
    assert.equal(out.checkoutAfterRefusal, true)
    assert.equal(out.retriedOk, true)
  })

  check('a stranded generation and an unresolvable transaction each have an exit', () => {
    // Both states used to be terminal from inside the product. A run that ended
    // without Integrate leaves a generation provision() refuses for ever, so the
    // queued request bounces every poll; and one recovery-required record makes
    // activeIssue() report the repository busy, freezing every board mutation.
    // Neither had an HTTP or a board surface — only a shell.
    const { out } = runScenario(`
      const contract = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/integration-record-contract.cjs'))});
      // A run that was started and then abandoned: the generation is materialized
      // and 'ready', which is exactly what provision() refuses for ever after.
      const made = provision('TASK_7_first', 1);
      const strandedBefore = manager.activeRecordFor('TASK_7_first').record.status;
      const released = manager.releaseFor('TASK_7_first');
      const strandedAfter = manager.activeRecordFor('TASK_7_first').record;

      const second = provision('TASK_8_second', 2);
      const base = git(ROOT, 'rev-parse', 'HEAD').trim();
      const baseTree = git(ROOT, 'rev-parse', 'HEAD^{tree}').trim();
      const at = new Date().toISOString();
      const phases = {};
      for (const name of contract.PHASES) phases[name] = { intentAt: null, provenAt: null };
      phases.prepared = { intentAt: at, provenAt: at };
      const wal = {
        version: 1, integrationId: 'ig-' + 'e'.repeat(32), stem: 'TASK_8_second',
        runId: '1700000000002-r2', worktreeId: second.worktreeId,
        phase: 'prepared', status: 'recovery-required',
        candidate: { commit: base, tree: baseTree, diffHash: 'sha256:' + '1'.repeat(64),
          receiptHash: 'sha256:' + '2'.repeat(64) },
        target: { ref: 'refs/heads/main', baseCommit: base, baseTree },
        controlSnapshot: { headCommit: base, dirtyAllowedPaths: [] },
        commitPin: { stagedTreeHash: null, messageHash: null, expectedParent: null, publishedCommit: null },
        finalizerPrepared: null, phases,
        owner: { hostname: 'fixture', pid: process.pid, processStartId: null, startedAt: at },
        createdAt: at, updatedAt: at, recordHash: 'sha256:' + '0'.repeat(64),
      };
      wal.recordHash = contract.recordHash(wal);
      const dir = P.join(ROOT, 'orchestrator/.cache/tasks/integrations');
      fs.mkdirSync(dir, { recursive: true });
      fs.writeFileSync(P.join(dir, 'TASK_8_second.json'), JSON.stringify(wal) + '\\n');

      const busyBefore = integrations.activeIssue().active;
      const wrongId = integrations.abandon('TASK_8_second', 'ig-' + '0'.repeat(32));
      const heldByWal = manager.releaseFor('TASK_8_second');
      const abandoned = integrations.abandon('TASK_8_second', wal.integrationId);
      const busyAfter = integrations.activeIssue().active;
      done({
        strandedBefore, released: released.ok === true,
        strandedGone: strandedAfter === null,
        checkoutGone: !fs.existsSync(made.executionRoot),
        busyBefore, wrongIdCode: wrongId.code, heldByWalCode: heldByWal.code,
        abandonedOk: abandoned.ok === true, busyAfter
      });
    `)
    assert.equal(out.strandedBefore, 'ready')
    assert.equal(out.released, true, 'the board can release a generation nothing will ever integrate')
    assert.equal(out.strandedGone, true, 'and the stem stops owning it, so the next run provisions afresh')
    assert.equal(out.checkoutGone, true)
    assert.equal(out.busyBefore, true, 'a recovery-required record holds the repository-wide mutex')
    assert.equal(out.wrongIdCode, 'INTEGRATION_ID_MISMATCH',
      'abandoning needs the exact id, so it cannot be pressed on a record nobody read')
    assert.equal(out.heldByWalCode, 'RELEASE_INTEGRATION_ACTIVE',
      'a generation inside a transaction is never released underneath it')
    assert.equal(out.abandonedOk, true)
    assert.equal(out.busyAfter, false, 'and the board is no longer frozen by it')
  })

  check('a superseded generation is RECORDED as such, not just reported', () => {
    // Detecting drift and only returning a blocker left the record saying
    // 'ready-for-integration', so the board kept offering Integrate for a
    // candidate that can never be integrated and its revalidation branch was
    // unreachable. The same check also re-proves the Figma and API generation
    // pins, which were written at provisioning and never compared to anything.
    const { out } = runScenario(`
      const made = provision('TASK_7_first', 1);
      fs.writeFileSync(P.join(made.executionRoot, 'app/keep.txt'), 'candidate\\n');
      manager.seal({ worktreeId: made.worktreeId });
      const sealedStatus = manager.activeRecordFor('TASK_7_first').record.status;
      const readyPreview = integrations.preview('TASK_7_first').state;

      // The owner commits on the target while the candidate sits sealed.
      fs.writeFileSync(P.join(ROOT, 'app/unrelated.txt'), 'owner work\\n');
      git(ROOT, 'add', 'app/unrelated.txt');
      git(ROOT, 'commit', '-q', '-m', 'owner commit');
      const driftedPreview = integrations.preview('TASK_7_first').state;
      const driftedStatus = manager.activeRecordFor('TASK_7_first').record.status;

      // A second generation, superseded by a Figma regeneration instead.
      const other = provision('TASK_8_second', 2);
      fs.writeFileSync(P.join(other.executionRoot, 'app/keep.txt'), 'other\\n');
      manager.seal({ worktreeId: other.worktreeId });
      const figmaManifest = P.join(ROOT, 'orchestrator/figma/manifests');
      fs.mkdirSync(figmaManifest, { recursive: true });
      fs.writeFileSync(P.join(figmaManifest, 'current-generation.json'), '{\"generation\":2}');
      const pinPreview = integrations.preview('TASK_8_second').state;
      const pinStatus = manager.activeRecordFor('TASK_8_second').record.status;
      done({ sealedStatus, readyPreview, driftedPreview, driftedStatus, pinPreview, pinStatus });
    `)
    assert.equal(out.sealedStatus, 'ready-for-integration')
    // This fixture has no task lock or Outcome draft, so a freshly sealed
    // candidate is 'blocked' for those ordinary reasons. That is exactly the
    // state the defect hid in: superseded looked identical to merely blocked.
    assert.equal(out.readyPreview, 'blocked')
    assert.equal(out.driftedPreview, 'revalidation-required',
      'the board must see a superseded generation as superseded, not merely blocked')
    assert.equal(out.driftedStatus, 'revalidation-required',
      'and the RECORD must carry it, so every later reader agrees')
    assert.equal(out.pinPreview, 'revalidation-required',
      'a regenerated Figma generation supersedes a candidate pinned against the old one')
    assert.equal(out.pinStatus, 'revalidation-required')
  })

  check('release cannot overwrite a sealer that wins the lifecycle record CAS', () => {
    const { out } = runScenario(`
      const made = provision('TASK_7_first', 1);
      fs.writeFileSync(P.join(made.executionRoot, 'app/keep.txt'), 'candidate\\n');
      const originalScan = leases.scan;
      let raced = false;
      leases.scan = function () {
        if (!raced) {
          raced = true;
          const sealed = manager.seal({ worktreeId: made.worktreeId });
          if (!sealed.ok) throw new Error(JSON.stringify(sealed));
        }
        return originalScan.apply(this, arguments);
      };
      const released = manager.release(made.worktreeId);
      const recordFile = P.join(ROOT, 'orchestrator/.cache/tasks/worktrees', made.worktreeId + '.json');
      const record = JSON.parse(fs.readFileSync(recordFile, 'utf8'));
      const receipt = manager.candidateReceipt(made.worktreeId);
      const ref = git(ROOT, 'rev-parse', made.candidateRef).trim();
      done({ releaseCode: released.code || null, status: record.status,
        receiptCommit: receipt && receipt.candidateCommit, ref });
    `)
    assert.equal(out.releaseCode, 'RELEASE_RECORD_CONFLICT',
      'the stale releaser must lose the record CAS before judging candidate ownership')
    assert.equal(out.status, 'ready-for-integration',
      'a stale releaser must not overwrite the successful seal')
    assert.equal(out.ref, out.receiptCommit,
      'the winning receipt and physical candidate ref stay coherent')
  })

  check('the home chain refuses a redirected component and cleans up after itself', () => {
    // §7.1. lstat alone proves the component we NAMED is a directory; it does
    // not prove the path we walked is the path we think we walked, because a
    // symlinked component resolves elsewhere and every lstat below it still
    // passes. And step 3 of the same section — removing the manager's own
    // directory once it is empty — was never implemented, so every repository
    // left one behind for ever.
    const { out, parent } = runScenario(`
      const home = process.env.ORCHESTRATOR_WORKTREE_HOME;
      const made = provision('TASK_7_first', 1);
      const repoDir = P.dirname(made.executionRoot);
      const released = manager.release(made.worktreeId);
      done({
        provisioned: made.ok === true,
        released: released.ok === true,
        repoDirGone: !fs.existsSync(repoDir),
        homeKept: fs.existsSync(home),
      });
    `)
    assert.equal(out.provisioned, true)
    assert.equal(out.released, true)
    assert.equal(out.repoDirGone, true, 'the emptied manager directory is removed, ownership-safely')
    assert.equal(out.homeKept, true, 'the worktree home itself is never removed')

    // A redirected component: the repository directory is replaced by a symlink
    // pointing somewhere else. Every lstat on the NAMED path still succeeds.
    const redirected = runScenario(`
      const home = process.env.ORCHESTRATOR_WORKTREE_HOME;
      const first = provision('TASK_7_first', 1);
      const repoDir = P.dirname(first.executionRoot);
      manager.release(first.worktreeId);
      const elsewhere = P.join(home, 'elsewhere');
      fs.mkdirSync(elsewhere, { recursive: true });
      fs.symlinkSync(elsewhere, repoDir);
      const second = provision('TASK_8_second', 2);
      done({ ok: second.ok === true, code: second.code || null });
    `).out
    assert.equal(redirected.ok, false, 'a redirected home component must refuse provisioning')
    assert.equal(redirected.code, 'PROVISION_HOME_UNSAFE')
    void parent
  })

  check('the concurrency cap is the canary value and the environment cannot raise it', () => {
    const probe = execFileSync(process.execPath,
      ['-e', 'console.log(require(process.argv[1]).MAX_PARALLEL)',
        join(repoRoot, 'orchestrator', 'site', 'server', 'runner.js')],
      { env: { ...process.env, RUNNER_MAX_PARALLEL: '16' }, encoding: 'utf8' })
    assert.equal(probe.trim(), '2')
  })
} finally {
  for (const root of roots) { try { rmSync(root, { recursive: true, force: true }) } catch (error) {} }
}

console.log(`\nworktree-concurrency: ${checks} checks passed`)
