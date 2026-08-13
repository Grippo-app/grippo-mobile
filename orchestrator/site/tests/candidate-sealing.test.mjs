#!/usr/bin/env node
// Phase 3 candidate sealing (pipeline improvement 01, §9.5/§21): the receipt
// replaces the old `/tmp` + `git status` set-difference footprint. Every
// operation shape is a first-class entry, control-owned and ignored paths can
// never be deliverables, the receipt binds to the exact candidate tree, and the
// temporary commit carries the fixed local-only identity — never --no-verify,
// never a push.

import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { chmodSync, mkdirSync, mkdtempSync, renameSync, rmSync, symlinkSync, unlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const contract = require('../../tasks/candidate-receipt-contract.cjs')
const repoRoot = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..')

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }
const roots = []
function git(cwd, ...args) {
  return execFileSync('git', args, { cwd, encoding: 'utf8', env: { ...process.env, GIT_TERMINAL_PROMPT: '0' } })
}
// Each scenario runs in a child process so paths.js binds to the fixture root.
function runScenario(body, options = {}) {
  const parent = mkdtempSync(join(tmpdir(), 'candidate-seal-'))
  roots.push(parent)
  const root = join(parent, 'repo с пробелами')
  mkdirSync(root)
  git(root, 'init', '-q', '-b', 'main')
  git(root, 'config', 'user.email', 'fixture@test.invalid')
  git(root, 'config', 'user.name', 'Fixture')
  git(root, 'config', 'commit.gpgsign', 'false')
  for (const dir of ['orchestrator/tasks/todo', 'orchestrator/skills', 'app']) {
    mkdirSync(join(root, dir), { recursive: true })
  }
  writeFileSync(join(root, 'orchestrator', 'tasks', 'todo', 'TASK_7_probe.md'), '# Task 7\n')
  writeFileSync(join(root, 'orchestrator', 'skills', 'install-skills.sh'),
    '#!/usr/bin/env bash\nmkdir -p "$1/.claude/skills/probe"\nprintf installed > "$1/.claude/skills/probe/SKILL.md"\n')
  mkdirSync(join(root, '.claude', 'skills', 'probe'), { recursive: true })
  writeFileSync(join(root, '.claude', 'skills', 'probe', 'SKILL.md'), 'installed')
  writeFileSync(join(root, '.gitignore'), 'app/generated/\n')
  writeFileSync(join(root, 'app', 'keep.txt'), 'base\n')
  writeFileSync(join(root, 'app', 'mode.txt'), 'exec me\n')
  writeFileSync(join(root, 'app', 'gone.txt'), 'delete me\n')
  writeFileSync(join(root, 'app', 'move.txt'), 'content that survives the rename unchanged so git detects it\n')
  git(root, 'add', '.')
  git(root, 'commit', '-q', '-m', 'base')
  const script = `
    process.env.ORCHESTRATOR_PROJECT_ROOT = ${JSON.stringify(root)};
    process.env.ORCHESTRATOR_WORKTREE_HOME = ${JSON.stringify(join(parent, '.orchestrator-worktrees'))};
    ${options.gate === false ? '' : `
    const gateModule = ${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-checkpoints.js'))};
    require.cache[gateModule] = { id: gateModule, filename: gateModule, loaded: true,
      exports: { sealingGate: () => ({ ok: true, checkpoint: { checkpointId: 'cp-fixture' } }) } };
    `}
    const manager = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
    const taskIntegrity = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-integrity.js'))});
    const gitMutations = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/git-mutations.js'))});
    const contract = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/candidate-receipt-contract.cjs'))});
    const { execFileSync } = require('node:child_process');
    const fs = require('node:fs');
    const P = require('node:path');
    const git = (cwd, ...a) => execFileSync('git', a, { cwd, encoding: 'utf8' });
    const ROOT = ${JSON.stringify(root)};
    const provision = () => manager.provision({ stem: 'TASK_7_probe', runId: '1700000000000-r1', requestId: '1700000000000-q1',
      sourceRevision: taskIntegrity.validateAction('run', 'TASK_7_probe', 'fixture').sourceRevision });
    ${body}
  `
  const out = execFileSync(process.execPath, ['-e', script], {
    encoding: 'utf8', cwd: root, maxBuffer: 16 * 1024 * 1024,
  })
  return { root, parent, out: JSON.parse(out) }
}

try {
  check('sealing refuses a candidate without a current completed ship checkpoint', () => {
    const { out } = runScenario(`
      const p = provision();
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'ungated\\n');
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      const record = manager.activeRecordFor('TASK_7_probe').record;
      console.log(JSON.stringify({ code: sealed.code, status: record.status,
        receipt: manager.candidateReceipt(p.worktreeId) }));
    `, { gate: false })
    assert.equal(out.code, 'SEAL_GATE_ABSENT')
    assert.equal(out.status, 'ready', 'a normal ungated child exit remains runnable')
    assert.equal(out.receipt, null)
  })

  check('the receipt carries every operation shape with exact blobs and modes', () => {
    const { out } = runScenario(`
      const p = provision();
      const wt = p.executionRoot;
      fs.writeFileSync(P.join(wt, 'app/keep.txt'), 'candidate\\n');            // modify
      fs.writeFileSync(P.join(wt, 'app/added.txt'), 'new\\n');                 // add
      fs.unlinkSync(P.join(wt, 'app/gone.txt'));                              // delete
      fs.renameSync(P.join(wt, 'app/move.txt'), P.join(wt, 'app/renamed.txt')); // rename
      fs.chmodSync(P.join(wt, 'app/mode.txt'), 0o755);                        // mode
      fs.symlinkSync('keep.txt', P.join(wt, 'app/link.txt'));                 // symlink
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      const receipt = manager.candidateReceipt(p.worktreeId);
      const identity = git(ROOT, 'log', '-1', '--format=%an <%ae>', sealed.candidateCommit).trim();
      const parent = git(ROOT, 'rev-parse', sealed.candidateCommit + '^').trim();
      console.log(JSON.stringify({
        ok: sealed.ok,
        entries: receipt.entries.map((e) => [e.operation, e.path, e.oldMode, e.newMode, e.renameFrom]),
        blobsPresent: receipt.entries.every((e) =>
          (e.operation === 'add' ? e.oldBlob === null && e.newBlob !== null :
           e.operation === 'delete' ? e.newBlob === null && e.oldBlob !== null :
           e.oldBlob !== null && e.newBlob !== null)),
        identity, parentIsBase: parent === p.baseCommit,
        diffHashMatches: receipt.diffHash === contract.diffHashOf(receipt.entries),
        // The receipt has no gates channel: a gate binds to a candidate through
        // the checkpoint execution pin, and a second always-empty mechanism in
        // the receipt only looked like it was pinning something.
        gatesChannel: Object.prototype.hasOwnProperty.call(receipt, 'gates'),
      }));
    `)
    assert.equal(out.ok, true)
    assert.deepEqual(out.entries, [
      ['add', 'app/added.txt', null, '100644', null],
      ['delete', 'app/gone.txt', '100644', null, null],
      ['modify', 'app/keep.txt', '100644', '100644', null],
      ['add', 'app/link.txt', null, '120000', null],
      ['mode', 'app/mode.txt', '100644', '100755', null],
      ['rename', 'app/renamed.txt', '100644', '100644', 'app/move.txt'],
    ])
    assert.equal(out.blobsPresent, true)
    assert.equal(out.identity, 'Orchestrator Candidate <orchestrator@local.invalid>')
    assert.equal(out.parentIsBase, true)
    assert.equal(out.diffHashMatches, true)
    assert.equal(out.gatesChannel, false)
  })

  check('a path dirty before the run is still attributed exactly (the old footprint could not)', () => {
    // The §20 exit criterion. The user's control root carries uncommitted work
    // on the SAME path the task touches; the candidate must still carry that
    // path with its exact base→candidate blobs, and the user's bytes must be
    // untouched.
    const { out, root } = runScenario(`
      // Dirty the control root on a path the task will also change.
      fs.writeFileSync(P.join(ROOT, 'app/keep.txt'), 'user work in progress\\n');
      const p = provision();
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'task work\\n');
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      const receipt = manager.candidateReceipt(p.worktreeId);
      const entry = receipt.entries.find((e) => e.path === 'app/keep.txt');
      const baseBlob = git(ROOT, 'rev-parse', p.baseTree + ':app/keep.txt').trim();
      console.log(JSON.stringify({
        ok: sealed.ok,
        entryOperation: entry && entry.operation,
        oldBlobIsBase: entry && entry.oldBlob === baseBlob,
        candidateContent: git(ROOT, 'show', sealed.candidateCommit + ':app/keep.txt'),
        controlBytes: fs.readFileSync(P.join(ROOT, 'app/keep.txt'), 'utf8'),
      }));
    `)
    assert.equal(out.ok, true)
    assert.equal(out.entryOperation, 'modify', 'the doubly-touched path must not vanish from the footprint')
    assert.equal(out.oldBlobIsBase, true, 'the old side is the sealed base, not the user\'s dirty bytes')
    assert.equal(out.candidateContent, 'task work\n')
    assert.equal(out.controlBytes, 'user work in progress\n', 'the user\'s working tree is never imported or touched')
    void root
  })

  check('control-owned and ignored paths can never become deliverables', () => {
    const { out } = runScenario(`
      const p = provision();
      const wt = p.executionRoot;
      fs.writeFileSync(P.join(wt, 'app/keep.txt'), 'work\\n');
      fs.writeFileSync(P.join(wt, 'orchestrator/tasks/todo/TASK_7_probe.md'), 'tampered\\n');
      const controlOwned = manager.seal({ worktreeId: p.worktreeId });
      git(wt, 'checkout', '--', 'orchestrator');
      fs.mkdirSync(P.join(wt, 'app/generated'), { recursive: true });
      fs.writeFileSync(P.join(wt, 'app/generated/output.bin'), 'derived\\n');
      const withIgnored = manager.seal({ worktreeId: p.worktreeId });
      const receipt = withIgnored.ok ? manager.candidateReceipt(p.worktreeId) : null;
      console.log(JSON.stringify({
        controlOwnedCode: controlOwned.code,
        ignoredSealOk: withIgnored.ok,
        ignoredAbsent: receipt ? receipt.entries.every((e) => e.path.indexOf('app/generated') !== 0) : null,
        statusAfter: git(wt, 'status', '--porcelain'),
      }));
    `)
    assert.equal(out.controlOwnedCode, 'SEAL_CONTROL_OWNED_PATH')
    assert.equal(out.ignoredSealOk, true, 'an ignored file is simply not a deliverable, not a hard failure')
    assert.equal(out.ignoredAbsent, true)
  })

  check('sealing refuses a moved candidate ref and an empty candidate', () => {
    const { out } = runScenario(`
      const empty = provision();
      const emptySeal = manager.seal({ worktreeId: empty.worktreeId });
      fs.writeFileSync(P.join(empty.executionRoot, 'app/keep.txt'), 'work\\n');
      // A child that commits on its own broke the manager protocol. The fixed
      // manager identity is public, so forging it must not manufacture
      // ownership in the absence of the durable candidate receipt.
      git(empty.executionRoot, 'add', 'app/keep.txt');
      git(empty.executionRoot, '-c', 'user.email=orchestrator@local.invalid', '-c', 'user.name=Orchestrator Candidate',
        'commit', '-q', '-m', 'child commit');
      const moved = manager.seal({ worktreeId: empty.worktreeId });
      const statusAfterSeal = manager.discover().records.worktrees.active
        .find((row) => row.record.worktreeId === empty.worktreeId).record.status;
      const released = manager.release(empty.worktreeId);
      console.log(JSON.stringify({ emptyCode: emptySeal.code, movedCode: moved.code,
        statusAfterSeal, releaseCode: released.code, checkoutKept: fs.existsSync(empty.executionRoot) }));
    `)
    assert.equal(out.emptyCode, 'SEAL_EMPTY_CANDIDATE')
    assert.equal(out.movedCode, 'SEAL_REF_MOVED')
    assert.equal(out.statusAfterSeal, 'recovery-required', JSON.stringify(out))
    assert.equal(out.releaseCode, 'RELEASE_REF_FOREIGN')
    assert.equal(out.checkoutKept, true)
  })

  check('the execution pin tracks the live tree and target, so gates cannot carry over', () => {
    const { out } = runScenario(`
      const p = provision();
      // The pin is a discriminated result: an unprovable context must never
      // arrive as a bare null, or the receipt written from it would be exempt
      // from the §6.13 gate for ever.
      const clean = manager.executionPinFor('TASK_7_probe', p.runId).pin;
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'work\\n');
      const dirty = manager.executionPinFor('TASK_7_probe', p.runId).pin;
      // Move the target in the control root: the pin must report the drift.
      fs.writeFileSync(P.join(ROOT, 'app/unrelated.txt'), 'owner work\\n');
      git(ROOT, 'add', 'app/unrelated.txt');
      git(ROOT, 'commit', '-q', '-m', 'owner commit');
      const drifted = manager.executionPinFor('TASK_7_probe', p.runId).pin;
      const absent = manager.executionPinFor('TASK_9_no_generation', '1700000000000-r9');
      const foreignRun = manager.executionPinFor('TASK_7_probe', '1700000000000-foreign');
      const sealAfterDrift = manager.seal({ worktreeId: p.worktreeId });
      console.log(JSON.stringify({
        cleanEqualsBase: clean.executionTree === p.baseTree,
        treeChanged: dirty.executionTree !== clean.executionTree,
        targetDrifted: drifted.targetCommit !== drifted.baseCommit,
        sealCode: sealAfterDrift.code,
        absentOk: absent.ok, absentPin: absent.pin,
        foreignRunOk: foreignRun.ok, foreignRunCode: foreignRun.code,
      }));
    `)
    assert.equal(out.cleanEqualsBase, true)
    assert.equal(out.treeChanged, true, 'any edit changes the pin, invalidating a receipt bound to it')
    assert.equal(out.targetDrifted, true)
    assert.equal(out.sealCode, 'SEAL_TARGET_DRIFTED', 'a moved target refuses sealing instead of shipping stale gates')
    assert.equal(out.absentOk, true, 'a stem with no generation is a PROVEN absence, not a doubt')
    assert.equal(out.absentPin, null)
    assert.equal(out.foreignRunOk, false)
    assert.equal(out.foreignRunCode, 'WORKTREE_RUN_MISMATCH')
  })

  check('execution-pin lookup refuses two valid materialized generations for one stem', () => {
    const { out } = runScenario(`
      const p = provision();
      const recordsDir = P.join(ROOT, 'orchestrator/.cache/tasks/worktrees');
      const name = fs.readdirSync(recordsDir).find((entry) => entry.endsWith('.json'));
      const original = JSON.parse(fs.readFileSync(P.join(recordsDir, name), 'utf8'));
      const duplicate = { ...original, worktreeId: 'wt-' + 'f'.repeat(32), recordHash: 'sha256:' + '0'.repeat(64) };
      const recordContract = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/worktree-record-contract.cjs'))});
      duplicate.recordHash = recordContract.recordHash(duplicate);
      fs.writeFileSync(P.join(recordsDir, duplicate.worktreeId + '.json'), JSON.stringify(duplicate) + '\\n');
      const pin = manager.executionPinFor('TASK_7_probe', p.runId);
      console.log(JSON.stringify({ ok: pin.ok, code: pin.code }));
    `)
    assert.equal(out.ok, false)
    assert.equal(out.code, 'WORKTREE_GENERATION_AMBIGUOUS')
  })

  check('a valid record under another generation filename is rejected before lifecycle CAS', () => {
    const { out } = runScenario(`
      const p = provision();
      const recordFile = P.join(ROOT, 'orchestrator/.cache/tasks/worktrees', p.worktreeId + '.json');
      const original = JSON.parse(fs.readFileSync(recordFile, 'utf8'));
      const substituted = { ...original, worktreeId: 'wt-' + 'e'.repeat(32), recordHash: 'sha256:' + '0'.repeat(64) };
      const recordContract = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/worktree-record-contract.cjs'))});
      substituted.recordHash = recordContract.recordHash(substituted);
      const bytes = Buffer.from(JSON.stringify(substituted) + '\\n');
      fs.writeFileSync(recordFile, bytes);
      const sealed = manager.seal({ worktreeId: substituted.worktreeId });
      console.log(JSON.stringify({ code: sealed.code,
        unchanged: fs.readFileSync(recordFile).equals(bytes) }));
    `)
    assert.equal(out.code, 'SEAL_RECORDS_UNSAFE')
    assert.equal(out.unchanged, true, 'a filename/identity mismatch must be read-only evidence')
  })

  check('candidate receipt bytes must identify the generation named by their canonical file', () => {
    const { out } = runScenario(`
      const p = provision();
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'candidate\\n');
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      const receiptFile = P.join(ROOT, 'orchestrator/.cache/tasks/worktrees/.receipts', p.worktreeId + '.json');
      const receipt = JSON.parse(fs.readFileSync(receiptFile, 'utf8'));
      const substituted = { ...receipt, worktreeId: 'wt-' + 'd'.repeat(32), receiptHash: 'sha256:' + '0'.repeat(64) };
      substituted.receiptHash = contract.receiptHash(substituted);
      fs.writeFileSync(receiptFile, JSON.stringify(substituted) + '\\n');
      console.log(JSON.stringify({ sealed: sealed.ok,
        projected: manager.candidateReceipt(p.worktreeId) }));
    `)
    assert.equal(out.sealed, true)
    assert.equal(out.projected, null)
  })

  check('task and project-config drift invalidate the generation before receipt publication', () => {
    const taskDrift = runScenario(`
      const p = provision();
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'work\\n');
      fs.writeFileSync(P.join(ROOT, 'orchestrator/tasks/todo/TASK_7_probe.md'), '# changed task\\n');
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      console.log(JSON.stringify({ code: sealed.code,
        status: manager.activeRecordFor('TASK_7_probe').record.status,
        receipt: manager.candidateReceipt(p.worktreeId) }));
    `).out
    assert.equal(taskDrift.code, 'SEAL_INPUT_DRIFTED')
    assert.equal(taskDrift.status, 'revalidation-required')
    assert.equal(taskDrift.receipt, null)

    const configDrift = runScenario(`
      const p = provision();
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'work\\n');
      fs.writeFileSync(P.join(ROOT, 'orchestrator/project-config.md'), 'changed\\n');
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      console.log(JSON.stringify({ code: sealed.code,
        status: manager.activeRecordFor('TASK_7_probe').record.status,
        receipt: manager.candidateReceipt(p.worktreeId) }));
    `).out
    assert.equal(configDrift.code, 'SEAL_INPUT_DRIFTED')
    assert.equal(configDrift.status, 'revalidation-required')
    assert.equal(configDrift.receipt, null)

    const postSealDrift = runScenario(`
      const p = provision();
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'work\\n');
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      fs.writeFileSync(P.join(ROOT, 'orchestrator/tasks/todo/TASK_7_probe.md'), '# changed after seal\\n');
      const issue = manager.revalidationIssueFor(manager.activeRecordFor('TASK_7_probe').record);
      console.log(JSON.stringify({ sealed: sealed.ok, code: issue && issue.code,
        status: manager.activeRecordFor('TASK_7_probe').record.status }));
    `).out
    assert.equal(postSealDrift.sealed, true)
    assert.equal(postSealDrift.code, 'GENERATION_SUPERSEDED')
    assert.equal(postSealDrift.status, 'revalidation-required',
      'the integration consumer must invalidate task drift that lands after sealing')
  })

  check('an accepted dependency generation cannot change behind a green ship checkpoint', () => {
    const beforeSeal = runScenario(`
      const sourceRevision = 'sha256:' + '1'.repeat(64);
      let dependencyRevision = 'sha256:' + '2'.repeat(64);
      taskIntegrity.validateAction = () => ({
        action: 'run', observedState: 'todo', sourceRevision, findings: [],
        _model: { metadata: new Map([
          ['TASK_7_probe', { state: 'todo', revision: sourceRevision,
            deps: ['TASK_6_dependency'] }],
          ['TASK_6_dependency', { state: 'done', revision: dependencyRevision,
            deps: [], outcome: { valid: true, status: 'completed' } }],
        ]) },
      });
      const p = provision();
      const before = manager.activeRecordFor('TASK_7_probe').record;
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'work\\n');
      // Models a canonical reopen/edit of an already accepted dependency while
      // this run is active. The ship-gate stub remains green deliberately: the
      // worktree generation itself must carry and re-prove this input pin.
      dependencyRevision = 'sha256:' + '3'.repeat(64);
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      const after = manager.activeRecordFor('TASK_7_probe').record;
      const receipt = manager.candidateReceipt(p.worktreeId);
      console.log(JSON.stringify({ code: sealed.code, status: after.status,
        dependencySnapshotHash: before.dependencySnapshotHash,
        receiptDependencySnapshotHash: receipt && receipt.inputs.dependencySnapshotHash }));
    `).out
    assert.match(beforeSeal.dependencySnapshotHash, /^sha256:[a-f0-9]{64}$/,
      'provisioning must materialize the accepted dependency generation, never a decorative null')
    assert.equal(beforeSeal.code, 'SEAL_INPUT_DRIFTED')
    assert.equal(beforeSeal.status, 'revalidation-required')
    assert.equal(beforeSeal.receiptDependencySnapshotHash, null,
      'a superseded dependency generation must not publish a candidate receipt')

    const afterSeal = runScenario(`
      const sourceRevision = 'sha256:' + '1'.repeat(64);
      let dependencyRevision = 'sha256:' + '2'.repeat(64);
      taskIntegrity.validateAction = () => ({
        action: 'run', observedState: 'todo', sourceRevision, findings: [],
        _model: { metadata: new Map([
          ['TASK_7_probe', { state: 'todo', revision: sourceRevision,
            deps: ['TASK_6_dependency'] }],
          ['TASK_6_dependency', { state: 'done', revision: dependencyRevision,
            deps: [], outcome: { valid: true, status: 'completed' } }],
        ]) },
      });
      const p = provision();
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'work\\n');
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      const receipt = manager.candidateReceipt(p.worktreeId);
      const record = manager.activeRecordFor('TASK_7_probe').record;
      dependencyRevision = 'sha256:' + '3'.repeat(64);
      const issue = manager.revalidationIssueFor(record);
      console.log(JSON.stringify({ sealed: sealed.ok,
        receiptCarriesPin: receipt.inputs.dependencySnapshotHash === record.dependencySnapshotHash,
        code: issue && issue.code,
        status: manager.activeRecordFor('TASK_7_probe').record.status }));
    `).out
    assert.equal(afterSeal.sealed, true)
    assert.equal(afterSeal.receiptCarriesPin, true)
    assert.equal(afterSeal.code, 'GENERATION_SUPERSEDED')
    assert.equal(afterSeal.status, 'revalidation-required',
      'Integrate must consume the same dependency pin after candidate publication')
  })

  check('execution bytes changing during candidate preparation cannot outrun the gate pin', () => {
    const { out } = runScenario(`
      const p = provision();
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'gate-pinned\\n');
      const originalPrepare = gitMutations.prepareCandidate;
      gitMutations.prepareCandidate = (options) => {
        const prepared = originalPrepare(options);
        if (prepared.ok) fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'raced-after-prepare\\n');
        return prepared;
      };
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      console.log(JSON.stringify({ ok: sealed.ok, code: sealed.code,
        status: manager.activeRecordFor('TASK_7_probe').record.status,
        ref: git(ROOT, 'rev-parse', p.candidateRef).trim(), base: p.baseCommit }));
    `)
    assert.equal(out.ok, false)
    assert.equal(out.code, 'SEAL_EXECUTION_DRIFTED')
    assert.equal(out.status, 'revalidation-required')
    assert.equal(out.ref, out.base, 'the receipted ref effect must not publish after execution drift')
  })

  check('pinned inputs changing during the final gate cannot be published', () => {
    const { out } = runScenario(`
      const p = provision();
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'gate-pinned\\n');
      const checkpoints = require(gateModule);
      const originalGate = checkpoints.sealingGate;
      let gateCalls = 0;
      checkpoints.sealingGate = (...args) => {
        const result = originalGate(...args);
        gateCalls += 1;
        if (gateCalls === 2) {
          fs.writeFileSync(P.join(ROOT, 'orchestrator/project-config.md'), 'raced input\\n');
        }
        return result;
      };
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      console.log(JSON.stringify({ ok: sealed.ok, code: sealed.code, gateCalls,
        status: manager.activeRecordFor('TASK_7_probe').record.status,
        ref: git(ROOT, 'rev-parse', p.candidateRef).trim(), base: p.baseCommit }));
    `)
    assert.equal(out.gateCalls, 2, 'the race must land inside the final gate')
    assert.equal(out.ok, false)
    assert.equal(out.code, 'SEAL_INPUT_DRIFTED')
    assert.equal(out.status, 'revalidation-required')
    assert.equal(out.ref, out.base, 'the candidate ref must not publish after its pinned inputs changed')
  })

  check('execution bytes changing during the final gate cannot be published', () => {
    const { out } = runScenario(`
      const p = provision();
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'gate-pinned\\n');
      const checkpoints = require(gateModule);
      const originalGate = checkpoints.sealingGate;
      let gateCalls = 0;
      checkpoints.sealingGate = (...args) => {
        const result = originalGate(...args);
        gateCalls += 1;
        if (gateCalls === 2) {
          fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'raced-during-final-gate\\n');
        }
        return result;
      };
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      console.log(JSON.stringify({ ok: sealed.ok, code: sealed.code, gateCalls,
        status: manager.activeRecordFor('TASK_7_probe').record.status,
        ref: git(ROOT, 'rev-parse', p.candidateRef).trim(), base: p.baseCommit }));
    `)
    assert.equal(out.gateCalls, 2, 'the race must land inside the final gate')
    assert.equal(out.ok, false)
    assert.equal(out.code, 'SEAL_EXECUTION_DRIFTED')
    assert.equal(out.status, 'revalidation-required')
    assert.equal(out.ref, out.base, 'the candidate ref must not publish bytes superseded during the final gate')
  })

  check('a re-seal after a fix cycle replaces the candidate and keeps ALL of the task work', () => {
    // The trap: after the first seal the branch HEAD IS the candidate, so a
    // status-vs-HEAD path set would stage only what changed since it and
    // silently drop the earlier work. The path set is computed against the
    // sealed BASE tree, and the new commit replaces (never chains onto) the
    // previous candidate.
    const { out } = runScenario(`
      const contract2 = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/worktree-record-contract.cjs'))});
      const p = provision();
      const recordFile = P.join(ROOT, 'orchestrator/.cache/tasks/worktrees', p.worktreeId + '.json');
      const backToReady = () => {
        const record = JSON.parse(fs.readFileSync(recordFile, 'utf8'));
        const ready = Object.assign({}, record, { status: 'ready', updatedAt: new Date().toISOString() });
        ready.recordHash = contract2.recordHash(ready);
        fs.writeFileSync(recordFile, JSON.stringify(ready) + '\\n');
      };
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'first pass\\n');
      const first = manager.seal({ worktreeId: p.worktreeId });
      backToReady();
      fs.writeFileSync(P.join(p.executionRoot, 'app/added.txt'), 'second pass\\n');
      const second = manager.seal({ worktreeId: p.worktreeId });
      const receipt = manager.candidateReceipt(p.worktreeId);
      console.log(JSON.stringify({
        firstEntries: first.entries,
        secondEntries: second.entries,
        keepSurvived: git(ROOT, 'show', second.candidateCommit + ':app/keep.txt'),
        addedPresent: git(ROOT, 'show', second.candidateCommit + ':app/added.txt'),
        parentIsBase: git(ROOT, 'rev-parse', second.candidateCommit + '^').trim() === p.baseCommit,
        distinctCommits: first.candidateCommit !== second.candidateCommit,
        receiptCommit: receipt.candidateCommit === second.candidateCommit,
      }));
    `)
    assert.equal(out.firstEntries, 1)
    assert.equal(out.secondEntries, 2, 'the re-seal must carry BOTH passes, not only the newest change')
    assert.equal(out.keepSurvived, 'first pass\n')
    assert.equal(out.addedPresent, 'second pass\n')
    assert.equal(out.parentIsBase, true, 'a re-seal replaces the candidate instead of chaining onto it')
    assert.equal(out.distinctCommits, true)
    assert.equal(out.receiptCommit, true)
  })

  check('a re-seal never overwrites a replacement receipt generation observed after preparation', () => {
    const { out } = runScenario(`
      const contract2 = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/worktree-record-contract.cjs'))});
      const p = provision();
      const recordFile = P.join(ROOT, 'orchestrator/.cache/tasks/worktrees', p.worktreeId + '.json');
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'first pass\\n');
      const first = manager.seal({ worktreeId: p.worktreeId });
      const record = JSON.parse(fs.readFileSync(recordFile, 'utf8'));
      const ready = Object.assign({}, record, { status: 'ready', updatedAt: new Date().toISOString() });
      ready.recordHash = contract2.recordHash(ready);
      fs.writeFileSync(recordFile, JSON.stringify(ready) + '\\n');
      fs.writeFileSync(P.join(p.executionRoot, 'app/added.txt'), 'second pass\\n');
      const receiptFile = P.join(ROOT, 'orchestrator/.cache/tasks/worktrees/.receipts', p.worktreeId + '.json');
      const foreign = Buffer.from('{"foreign":true}\\n');
      const originalPrepare = gitMutations.prepareCandidate;
      gitMutations.prepareCandidate = (options) => {
        const prepared = originalPrepare(options);
        if (prepared.ok) fs.writeFileSync(receiptFile, foreign);
        return prepared;
      };
      const second = manager.seal({ worktreeId: p.worktreeId });
      console.log(JSON.stringify({ first: first.ok, second: second.ok, code: second.code,
        foreignPreserved: fs.readFileSync(receiptFile).equals(foreign) }));
    `)
    assert.equal(out.first, true)
    assert.equal(out.second, false)
    assert.equal(out.code, 'SEAL_RECEIPT_CONFLICT')
    assert.equal(out.foreignPreserved, true)
  })

  check('the receipt contract refuses tampering, control paths and inconsistent entries', () => {
    const base = {
      version: 1, worktreeId: 'wt-' + 'ab'.repeat(16), runId: '1700000000000-r1',
      stem: 'TASK_7_probe',
      candidateRef: 'refs/heads/orchestrator/task/TASK_7-' + 'ab'.repeat(6) + '/r1',
      baseCommit: 'a'.repeat(40), baseTree: 'b'.repeat(40),
      expectedRefCommit: 'a'.repeat(40),
      candidateCommit: 'c'.repeat(40), candidateTree: 'd'.repeat(40),
      entries: [{ path: 'app/x.txt', operation: 'modify', oldMode: '100644', newMode: '100644',
        oldBlob: '1'.repeat(40), newBlob: '2'.repeat(40), renameFrom: null }],
      diffHash: 'sha256:' + '0'.repeat(64),
      inputs: {
        taskSnapshotHash: 'sha256:' + 'a'.repeat(64), projectConfigHash: 'sha256:' + 'b'.repeat(64),
        dependencySnapshotHash: 'sha256:' + 'c'.repeat(64),
        targetRef: 'refs/heads/main', targetCommit: 'a'.repeat(40),
        figmaGenerationHash: null, apiGenerationHash: null,
      },
      sealedAt: '2026-08-10T00:00:00.000Z',
      owner: { hostname: 'mac', pid: 1, processStartId: null, startedAt: '2026-08-10T00:00:00.000Z' },
      receiptHash: 'sha256:' + '0'.repeat(64),
    }
    const seal = (value) => {
      const copy = JSON.parse(JSON.stringify(value))
      copy.diffHash = contract.diffHashOf(copy.entries)
      copy.receiptHash = contract.receiptHash(copy)
      return copy
    }
    const invalidUtf8 = seal({ ...base,
      owner: { ...base.owner, hostname: 'fixture-\uFFFD' } })
    const encoded = Buffer.from(JSON.stringify(invalidUtf8), 'utf8')
    const replacement = Buffer.from('\uFFFD', 'utf8')
    const offset = encoded.indexOf(replacement)
    const rawInvalid = Buffer.concat([encoded.subarray(0, offset), Buffer.from([0xff]),
      encoded.subarray(offset + replacement.length)])
    assert.throws(() => contract.validateBytes(rawInvalid), /UTF-8/)
    contract.validate(seal(base))
    assert.throws(() => {
      const tampered = seal(base)
      tampered.candidateTree = 'e'.repeat(40)
      contract.validate(tampered)
    }, /receiptHash does not match/)
    assert.throws(() => contract.validate(seal({ ...base,
      entries: [{ ...base.entries[0], path: 'orchestrator/tasks/INDEX.json' }] })), /control-owned/)
    assert.throws(() => contract.validate(seal({ ...base,
      entries: [{ ...base.entries[0], newBlob: base.entries[0].oldBlob }] })), /modify must change the blob/)
    assert.throws(() => contract.validate(seal({ ...base,
      inputs: { ...base.inputs, targetCommit: '9'.repeat(40) } })), /moved target invalidates/)
    assert.throws(() => contract.validate(seal({ ...base, entries: [
      { ...base.entries[0], path: 'app/z.txt' }, { ...base.entries[0], path: 'app/a.txt' },
    ] })), /sorted by destination path/)
  })

  check('receipt reads re-prove the physical commit, tree, parent and manifest', () => {
    const { out } = runScenario(`
      const p = provision();
      fs.writeFileSync(P.join(p.executionRoot, 'app/keep.txt'), 'candidate\\n');
      const sealed = manager.seal({ worktreeId: p.worktreeId });
      const paths = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/paths.js'))});
      const receiptFile = P.join(paths.WORKTREE_RECORDS_DIR, '.receipts', p.worktreeId + '.json');
      const receipt = JSON.parse(fs.readFileSync(receiptFile, 'utf8'));
      receipt.candidateTree = receipt.baseTree;
      receipt.receiptHash = contract.receiptHash(receipt);
      fs.writeFileSync(receiptFile, JSON.stringify(receipt) + '\\n');
      console.log(JSON.stringify({ sealed: sealed.ok,
        selfHashValid: (() => { try { contract.validate(receipt); return true; } catch { return false; } })(),
        physicalRead: manager.candidateReceipt(p.worktreeId) }));
    `)
    assert.equal(out.sealed, true)
    assert.equal(out.selfHashValid, true, 'the pure JSON contract cannot inspect Git objects')
    assert.equal(out.physicalRead, null, 'the manager must reject a self-consistent receipt that lies about Git bytes')
  })

  console.log(`candidate-sealing: ${checks} checks passed`)
} finally {
  while (roots.length) rmSync(roots.pop(), { recursive: true, force: true })
  void chmodSync; void renameSync; void symlinkSync; void unlinkSync
}
