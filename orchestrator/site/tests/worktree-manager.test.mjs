#!/usr/bin/env node
// Read-only worktree manager discovery (pipeline improvement 01, Phase 1):
// repository identity keyed on the git common dir, §18 environment prechecks
// as typed findings, owned/foreign/unsafe inventory classification with FULL
// record binding (filesystem identity + repository identity + lifecycle
// status + candidate branch + .git pointer), and the no-mutation doctrine —
// discovery must never delete, prune or repair anything.

import assert from 'node:assert/strict'
import { execFileSync, spawnSync } from 'node:child_process'
import { chmodSync, linkSync, lstatSync, mkdirSync, mkdtempSync, readdirSync, realpathSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const manager = require('../server/worktree-manager.js')
const contract = require('../../tasks/worktree-record-contract.cjs')
const repoRoot = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..')
const CLI = join(repoRoot, 'orchestrator', 'tasks', 'task-worktree.mjs')

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }

const roots = []
function scratch(prefix) {
  const dir = mkdtempSync(join(tmpdir(), prefix))
  roots.push(dir)
  return dir
}
function git(cwd, ...args) {
  return execFileSync('git', args, { cwd, encoding: 'utf8', env: { ...process.env, GIT_TERMINAL_PROMPT: '0' } })
}
function fixtureRepo(name = 'worktree-mgr-') {
  const parent = scratch(name)
  const root = join(parent, 'repo с пробелами')
  mkdirSync(root)
  git(root, 'init', '-q', '-b', 'main')
  git(root, 'config', 'user.email', 'fixture@test.invalid')
  git(root, 'config', 'user.name', 'Fixture')
  git(root, 'config', 'commit.gpgsign', 'false')
  writeFileSync(join(root, 'файл.txt'), 'content\n')
  git(root, 'add', '.')
  git(root, 'commit', '-q', '-m', 'init')
  return { parent, root }
}
function identityOf(target) {
  const real = realpathSync.native(target)
  const stat = lstatSync(real, { bigint: true })
  return { path: real.normalize('NFC'), dev: String(stat.dev), ino: String(stat.ino) }
}
function envFor(fixture) {
  return {
    ...process.env,
    ORCHESTRATOR_PROJECT_ROOT: fixture.root,
    ORCHESTRATOR_WORKTREE_HOME: join(fixture.parent, '.orchestrator-worktrees'),
  }
}
function discoverIn(fixture) {
  const result = spawnSync(process.execPath, [CLI, 'discover'], {
    encoding: 'utf8', env: envFor(fixture), maxBuffer: 8 * 1024 * 1024,
  })
  assert.notEqual(result.status, null, result.stderr)
  let projection = null
  try { projection = JSON.parse(result.stdout) } catch { assert.fail('discover printed no JSON: ' + result.stderr) }
  return { status: result.status, projection }
}
function findingCodes(projection) {
  return projection.findings.map((finding) => finding.code)
}
const CANDIDATE_REF = 'refs/heads/orchestrator/task/TASK_7-' + 'ab'.repeat(6) + '/r1'
const EMPTY_DEPENDENCY_SNAPSHOT_HASH =
  'sha256:c213f0364a9d65c84f208816f8b1f4a841747584ee2af680272093612b4297ad'
// A FULLY-BOUND record: real repository identity, real candidate branch. The
// negative checks below perturb exactly one binding each.
function writeRecord(fixture, patch) {
  const recordsDir = join(fixture.root, 'orchestrator', '.cache', 'tasks', 'worktrees')
  mkdirSync(recordsDir, { recursive: true })
  const commonDir = identityOf(join(fixture.root, '.git'))
  const head = git(fixture.root, 'rev-parse', 'HEAD').trim()
  const tree = git(fixture.root, 'rev-parse', 'HEAD^{tree}').trim()
  const record = {
    version: 1, worktreeId: 'wt-' + 'ab'.repeat(16), runId: '1700000000000-r1',
    requestId: '1700000000000-q1', stem: 'TASK_7_managed_probe', status: 'ready',
    controlProjectId: contract.digest({ path: commonDir.path, dev: commonDir.dev, ino: commonDir.ino }),
    gitCommonDirIdentity: commonDir,
    controlRoot: identityOf(fixture.root),
    executionRoot: null,
    targetRef: 'refs/heads/main',
    candidateRef: CANDIDATE_REF,
    baseCommit: head, baseTree: tree,
    taskState: 'todo',
    taskSourceRevision: 'sha256:' + 'b'.repeat(64),
    taskSnapshotHash: 'sha256:' + 'c'.repeat(64),
    projectConfigHash: 'sha256:' + 'd'.repeat(64),
    dependencySnapshotHash: EMPTY_DEPENDENCY_SNAPSHOT_HASH,
    figmaGenerationHash: null, apiGenerationHash: null,
    capabilities: [],
    owner: { hostname: 'fixture', pid: process.pid, processStartId: null, startedAt: '2026-08-09T10:00:00.000Z' },
    createdAt: '2026-08-09T10:00:00.000Z', updatedAt: '2026-08-09T10:00:01.000Z',
    recordHash: 'sha256:' + 'e'.repeat(64),
    ...patch,
  }
  record.recordHash = contract.recordHash(record)
  contract.validate(record)
  const file = join(recordsDir, record.worktreeId + '.json')
  writeFileSync(file, JSON.stringify(record) + '\n', { mode: 0o600 })
  return { record, file }
}
// A managed checkout fixture: worktree inside the home, on the candidate ref.
function managedFixture(fixture, { branch = CANDIDATE_REF.slice('refs/heads/'.length), dirName = 'wt-managed' } = {}) {
  const home = join(fixture.parent, '.orchestrator-worktrees')
  mkdirSync(home, { recursive: true })
  const checkout = join(home, dirName)
  git(fixture.root, 'worktree', 'add', '-q', checkout, '-b', branch)
  return checkout
}

try {
  check('repository identity keys on the shared git common dir across linked worktrees', () => {
    const fixture = fixtureRepo()
    const linked = join(fixture.parent, 'linked копия')
    git(fixture.root, 'worktree', 'add', '-q', linked, '-b', 'side-branch')
    const fromMain = manager.repositoryIdentity(fixture.root)
    const fromLinked = manager.repositoryIdentity(linked)
    assert.equal(fromMain.ok, true)
    assert.equal(fromLinked.ok, true)
    assert.equal(fromMain.controlProjectId, fromLinked.controlProjectId,
      'one repository must resolve to one controlProjectId from every checkout')
    assert.equal(manager.repositoryIdentity(fixture.parent).ok, false, 'a plain directory is not a work tree')
  })

  check('environment prechecks are clean on a plain full repo', () => {
    const fixture = fixtureRepo()
    const { findings } = manager.environmentPrechecks(fixture.root)
    assert.deepEqual(findings, [])
  })

  check('submodule predicate keys on .gitmodules or gitlinks, never submodule.* config', () => {
    const fixture = fixtureRepo()
    git(fixture.root, 'config', 'submodule.active', '.')
    assert.deepEqual(manager.environmentPrechecks(fixture.root).findings, [])
    writeFileSync(join(fixture.root, '.gitmodules'), '[submodule "x"]\n\tpath = x\n\turl = ./x\n')
    assert.ok(manager.environmentPrechecks(fixture.root).findings
      .some((finding) => finding.code === 'SUBMODULES_UNSUPPORTED'))
  })

  check('gitlink index entries trip the submodule blocker without .gitmodules', () => {
    const fixture = fixtureRepo()
    git(fixture.root, 'update-index', '--add', '--cacheinfo', `160000,${'0'.repeat(38)}42,nested-repo`)
    assert.ok(manager.environmentPrechecks(fixture.root).findings
      .some((finding) => finding.code === 'SUBMODULES_UNSUPPORTED'))
  })

  check('sparse, shallow, merge-state and detached HEAD are typed blockers', () => {
    const sparse = fixtureRepo()
    git(sparse.root, 'config', 'core.sparseCheckout', 'true')
    assert.ok(manager.environmentPrechecks(sparse.root).findings
      .some((finding) => finding.code === 'SPARSE_CHECKOUT_UNSUPPORTED'))

    const merging = fixtureRepo()
    writeFileSync(join(merging.root, '.git', 'MERGE_HEAD'), git(merging.root, 'rev-parse', 'HEAD'))
    assert.ok(manager.environmentPrechecks(merging.root).findings
      .some((finding) => finding.code === 'GIT_MERGE_IN_PROGRESS'))

    const detached = fixtureRepo()
    git(detached.root, 'checkout', '-q', '--detach', 'HEAD')
    assert.ok(manager.environmentPrechecks(detached.root).findings
      .some((finding) => finding.code === 'DETACHED_HEAD_UNSUPPORTED'))

    const shallowSource = fixtureRepo()
    const shallowClone = join(shallowSource.parent, 'shallow-clone')
    execFileSync('git', ['clone', '-q', '--depth', '1',
      'file://' + shallowSource.root, shallowClone], { encoding: 'utf8' })
    assert.ok(manager.environmentPrechecks(shallowClone).findings
      .some((finding) => finding.code === 'SHALLOW_REPOSITORY_UNSUPPORTED'))
  })

  check('content filters are detected from EVERY attributes source via check-attr', () => {
    // check-attr resolves the complete attribute stack (tree files,
    // info/attributes, core.attributesFile, global/system), so each source is
    // probed with a tracked path the pattern actually matches.
    function withTrackedBin(fixture, relative = 'payload.bin') {
      writeFileSync(join(fixture.root, relative), 'bytes')
      git(fixture.root, 'add', relative)
      git(fixture.root, 'commit', '-q', '-m', 'payload')
    }
    const top = fixtureRepo()
    withTrackedBin(top)
    writeFileSync(join(top.root, '.gitattributes'), '*.bin filter=lfs diff=lfs merge=lfs -text\n')
    assert.ok(manager.environmentPrechecks(top.root).findings
      .some((finding) => finding.code === 'CONTENT_FILTERS_UNSUPPORTED'))

    const nested = fixtureRepo()
    mkdirSync(join(nested.root, 'sub'))
    writeFileSync(join(nested.root, 'sub', '.gitattributes'), '*.bin filter=lfs -text\n')
    writeFileSync(join(nested.root, 'sub', 'payload.bin'), 'bytes')
    git(nested.root, 'add', 'sub')
    git(nested.root, 'commit', '-q', '-m', 'attrs')
    assert.ok(manager.environmentPrechecks(nested.root).findings
      .some((finding) => finding.code === 'CONTENT_FILTERS_UNSUPPORTED'),
      'per-directory .gitattributes must be honored')

    const info = fixtureRepo()
    withTrackedBin(info)
    mkdirSync(join(info.root, '.git', 'info'), { recursive: true })
    writeFileSync(join(info.root, '.git', 'info', 'attributes'), '*.bin filter=lfs -text\n')
    assert.ok(manager.environmentPrechecks(info.root).findings
      .some((finding) => finding.code === 'CONTENT_FILTERS_UNSUPPORTED'),
      '$GIT_DIR/info/attributes must be honored')

    // core.attributesFile is honored by git and must be caught too.
    const viaConfig = fixtureRepo()
    withTrackedBin(viaConfig)
    const attrFile = join(viaConfig.parent, 'custom-attributes')
    writeFileSync(attrFile, '*.bin filter=lfs -text\n')
    git(viaConfig.root, 'config', 'core.attributesFile', attrFile)
    assert.ok(manager.environmentPrechecks(viaConfig.root).findings
      .some((finding) => finding.code === 'CONTENT_FILTERS_UNSUPPORTED'),
      'core.attributesFile filters must be honored')

    // A config-only filter driver with no attribute referencing it, and a
    // filter pattern matching NO tracked path, affect nothing at checkout
    // time and are deliberately not findings.
    const configOnly = fixtureRepo()
    git(configOnly.root, 'config', 'filter.lfs.process', 'git-lfs filter-process')
    assert.deepEqual(manager.environmentPrechecks(configOnly.root).findings, [])
    const unmatched = fixtureRepo()
    writeFileSync(join(unmatched.root, '.gitattributes'), '*.bin filter=lfs -text\n')
    assert.deepEqual(manager.environmentPrechecks(unmatched.root).findings, [])
  })

  check('case-folded duplicate tracked paths are a typed blocker (APFS folds silently)', () => {
    const fixture = fixtureRepo()
    const blob = git(fixture.root, 'hash-object', '-w', join(fixture.root, 'файл.txt')).trim()
    git(fixture.root, 'update-index', '--add', '--cacheinfo', `100644,${blob},Collide.txt`)
    git(fixture.root, 'update-index', '--add', '--cacheinfo', `100644,${blob},collide.TXT`)
    assert.ok(manager.environmentPrechecks(fixture.root).findings
      .some((finding) => finding.code === 'CASE_COLLIDING_PATHS_UNSUPPORTED'))
  })

  check('discovery classifies control, foreign and fully-bound managed checkouts', () => {
    const fixture = fixtureRepo()
    const foreign = join(fixture.parent, 'разработчик own tree')
    git(fixture.root, 'worktree', 'add', '-q', foreign, '-b', 'dev-branch')
    const checkout = managedFixture(fixture)
    writeRecord(fixture, { executionRoot: identityOf(checkout) })

    const { status, projection } = discoverIn(fixture)
    assert.equal(status, 0, JSON.stringify(projection.findings))
    const byPath = new Map(projection.worktrees.map((row) => [row.path.split('/').pop(), row]))
    assert.equal(byPath.get('repo с пробелами').classification, 'control')
    assert.equal(byPath.get('разработчик own tree').classification, 'foreign')
    assert.equal(byPath.get('wt-managed').classification, 'managed')
    assert.equal(byPath.get('wt-managed').record.stem, 'TASK_7_managed_probe')
    assert.deepEqual(projection.findings, [])
  })

  check('a record from a DIFFERENT repository never grants managed classification', () => {
    const fixture = fixtureRepo()
    const checkout = managedFixture(fixture)
    // Perturb exactly the repository binding: same inode, foreign repo id.
    writeRecord(fixture, {
      executionRoot: identityOf(checkout),
      controlProjectId: 'sha256:' + 'a'.repeat(64),
      gitCommonDirIdentity: { path: '/some/other/repo/.git', dev: '999', ino: '999' },
    })
    const { status, projection } = discoverIn(fixture)
    assert.equal(status, 3, 'binding mismatch must fail closed')
    const row = projection.worktrees.find((entry) => entry.path.endsWith('wt-managed'))
    assert.equal(row.classification, 'unsafe')
    assert.ok(findingCodes(projection).includes('WORKTREE_RECORD_BINDING_MISMATCH'))
    assert.ok(!findingCodes(projection).includes('WORKTREE_RECORD_ORPHANED'),
      'a claimed record is reported as a binding problem, not an orphan')
  })

  check('a checkout off its candidate branch or under a terminal record is unsafe', () => {
    const offBranch = fixtureRepo()
    const rogueCheckout = managedFixture(offBranch, { branch: 'orchestrator/task/TASK_7-' + 'ab'.repeat(6) + '/other' })
    writeRecord(offBranch, { executionRoot: identityOf(rogueCheckout) })
    const offResult = discoverIn(offBranch)
    assert.equal(offResult.status, 3)
    assert.ok(findingCodes(offResult.projection).includes('WORKTREE_RECORD_BINDING_MISMATCH'))

    // A terminal record never claims a checkout; a live tree inside the home
    // without a materialized claim is a squatter finding, and an inode-reused
    // terminal record must not wedge trees OUTSIDE the home at all.
    const terminal = fixtureRepo()
    const terminalCheckout = managedFixture(terminal)
    writeRecord(terminal, { executionRoot: identityOf(terminalCheckout), status: 'released' })
    const terminalResult = discoverIn(terminal)
    assert.equal(terminalResult.status, 3)
    const row = terminalResult.projection.worktrees.find((entry) => entry.path.endsWith('wt-managed'))
    assert.equal(row.classification, 'unsafe')
    assert.ok(findingCodes(terminalResult.projection).includes('WORKTREE_HOME_SQUATTER'))
    assert.ok(!findingCodes(terminalResult.projection).includes('WORKTREE_RECORD_BINDING_MISMATCH'))

    const reuse = fixtureRepo()
    const outside = join(reuse.parent, 'developer tree outside home')
    git(reuse.root, 'worktree', 'add', '-q', outside, '-b', 'dev-outside')
    writeRecord(reuse, { executionRoot: identityOf(outside), status: 'released' })
    const reuseResult = discoverIn(reuse)
    const outsideRow = reuseResult.projection.worktrees.find((entry) => entry.path.endsWith('developer tree outside home'))
    assert.equal(outsideRow.classification, 'foreign',
      'a terminal record reusing a foreign inode must not wedge the foreign tree')
    assert.ok(!findingCodes(reuseResult.projection).includes('WORKTREE_RECORD_BINDING_MISMATCH'))
  })

  check('a tampered .git pointer makes a bound checkout unsafe (foreign repo AND sibling redirect)', () => {
    const fixture = fixtureRepo()
    const checkout = managedFixture(fixture)
    writeRecord(fixture, { executionRoot: identityOf(checkout) })
    const other = fixtureRepo('worktree-mgr-other-')
    writeFileSync(join(checkout, '.git'), 'gitdir: ' + join(other.root, '.git') + '\n')
    const { status, projection } = discoverIn(fixture)
    assert.equal(status, 3)
    assert.ok(findingCodes(projection).includes('WORKTREE_RECORD_BINDING_MISMATCH'))

    // Same-repo redirect: point the managed checkout at a SIBLING worktree's
    // admin dir — the common dir stays identical, only the toplevel differs.
    const sibling = fixtureRepo('worktree-mgr-sibling-')
    const siblingManaged = managedFixture(sibling)
    writeRecord(sibling, { executionRoot: identityOf(siblingManaged) })
    const decoy = join(sibling.parent, 'decoy tree')
    git(sibling.root, 'worktree', 'add', '-q', decoy, '-b', 'decoy-branch')
    const decoyAdmin = readdirSync(join(sibling.root, '.git', 'worktrees'))
      .map((name) => join(sibling.root, '.git', 'worktrees', name))
      .find((admin) => {
        try { return realpathSync.native(join(admin, '..', '..', '..')) && true } catch { return false }
      })
    const admins = readdirSync(join(sibling.root, '.git', 'worktrees'))
    const decoyName = admins.find((name) => name.startsWith('decoy'))
    assert.ok(decoyName, admins.join(','))
    writeFileSync(join(siblingManaged, '.git'),
      'gitdir: ' + join(sibling.root, '.git', 'worktrees', decoyName) + '\n')
    const siblingResult = discoverIn(sibling)
    assert.equal(siblingResult.status, 3, JSON.stringify(siblingResult.projection.findings))
    assert.ok(findingCodes(siblingResult.projection).includes('WORKTREE_RECORD_BINDING_MISMATCH'))
    void decoyAdmin
  })

  check('a managed record outside the worktree home is unsafe, never repaired', () => {
    const fixture = fixtureRepo()
    const rogue = join(fixture.parent, 'rogue-checkout')
    git(fixture.root, 'worktree', 'add', '-q', rogue, '-b', CANDIDATE_REF.slice('refs/heads/'.length))
    writeRecord(fixture, { executionRoot: identityOf(rogue) })
    const { status, projection } = discoverIn(fixture)
    assert.equal(status, 3, 'blockers must fail the discovery exit')
    const row = projection.worktrees.find((entry) => entry.path.endsWith('rogue-checkout'))
    assert.equal(row.classification, 'unsafe')
    assert.ok(findingCodes(projection).includes('MANAGED_WORKTREE_OUTSIDE_HOME'))
    assert.ok(lstatSync(rogue).isDirectory(), 'discovery must not delete anything')
  })

  check('orphaned, recovery-required and namespace-squatting states are typed findings', () => {
    const fixture = fixtureRepo()
    writeRecord(fixture, {
      executionRoot: { path: join(fixture.parent, '.orchestrator-worktrees', 'gone').normalize('NFC'), dev: '1', ino: '424242' },
    })
    const orphanResult = discoverIn(fixture)
    assert.equal(orphanResult.status, 3)
    assert.ok(findingCodes(orphanResult.projection).includes('WORKTREE_RECORD_ORPHANED'))

    const recovery = fixtureRepo()
    writeRecord(recovery, { status: 'recovery-required', executionRoot: null })
    const recoveryResult = discoverIn(recovery)
    assert.equal(recoveryResult.status, 3)
    assert.ok(findingCodes(recoveryResult.projection).includes('WORKTREE_RECOVERY_REQUIRED'))

    const squatting = fixtureRepo()
    const squat = join(squatting.parent, 'squat tree')
    git(squatting.root, 'worktree', 'add', '-q', squat, '-b', 'orchestrator/task/TASK_9-' + 'cd'.repeat(6) + '/sq')
    const squatResult = discoverIn(squatting)
    assert.equal(squatResult.status, 3)
    assert.ok(findingCodes(squatResult.projection).includes('MANAGER_NAMESPACE_BRANCH_FOREIGN'))
    const row = squatResult.projection.worktrees.find((entry) => entry.path.endsWith('squat tree'))
    assert.equal(row.classification, 'foreign', 'squatters are reported but never touched')
  })

  check('invalid, symlinked, hardlinked and oversized record files are blockers and retained', () => {
    const fixture = fixtureRepo()
    const recordsDir = join(fixture.root, 'orchestrator', '.cache', 'tasks', 'worktrees')
    mkdirSync(recordsDir, { recursive: true })
    const malformed = join(recordsDir, 'wt-' + 'ff'.repeat(16) + '.json')
    writeFileSync(malformed, '{"broken": true}\n', { mode: 0o600 })
    const target = join(fixture.parent, 'outside-target.json')
    writeFileSync(target, '{}\n')
    symlinkSync(target, join(recordsDir, 'wt-' + 'ee'.repeat(16) + '.json'))
    const linked = join(recordsDir, 'wt-' + 'dd'.repeat(16) + '.json')
    writeFileSync(linked, '{}\n', { mode: 0o600 })
    linkSync(linked, join(fixture.parent, 'second-name.json'))
    const oversized = join(recordsDir, 'wt-' + 'cc'.repeat(16) + '.json')
    writeFileSync(oversized, '{"pad":"' + 'x'.repeat(contract.MAX_BYTES) + '"}\n', { mode: 0o600 })

    const { status, projection } = discoverIn(fixture)
    assert.equal(status, 3, 'an unreliable record store must fail closed')
    const invalidCodes = projection.records.worktrees.invalid.map((entry) => entry.code).sort()
    // symlink, hardlink and oversize all die inside fileGuards' no-follow
    // nlink=1 bounded read — one honest UNREADABLE code each.
    assert.deepEqual(invalidCodes, [
      'WORKTREE_RECORD_INVALID', 'WORKTREE_RECORD_UNREADABLE',
      'WORKTREE_RECORD_UNREADABLE', 'WORKTREE_RECORD_UNREADABLE',
    ])
    assert.ok(findingCodes(projection).includes('WORKTREE_RECORDS_INVALID'))
    assert.ok(lstatSync(malformed).isFile(), 'invalid records must be retained for explicit recovery')
    assert.ok(lstatSync(linked).isFile(), 'hardlinked records must be retained for explicit recovery')
  })

  check('prunable FOREIGN worktree entries stay untouched through discovery', () => {
    const fixture = fixtureRepo()
    const doomed = join(fixture.parent, 'doomed tree')
    git(fixture.root, 'worktree', 'add', '-q', doomed, '-b', 'doomed-branch')
    rmSync(doomed, { recursive: true, force: true })
    const before = git(fixture.root, 'worktree', 'list', '--porcelain')
    assert.ok(before.includes('prunable'), 'fixture must present a prunable entry')
    const adminBefore = readdirSync(join(fixture.root, '.git', 'worktrees')).sort()

    const { projection } = discoverIn(fixture)
    const row = projection.worktrees.find((entry) => entry.path.endsWith('doomed tree'))
    assert.ok(row, 'prunable entry must stay visible in the inventory')
    assert.equal(row.classification, 'foreign')
    assert.ok(row.prunable, 'prunable reason must be carried verbatim')

    const after = git(fixture.root, 'worktree', 'list', '--porcelain')
    assert.equal(after, before, 'discovery must not prune or mutate the worktree list')
    assert.deepEqual(readdirSync(join(fixture.root, '.git', 'worktrees')).sort(), adminBefore,
      'discovery must not touch .git/worktrees admin state')
  })

  check('worktree list parser handles spaces, Unicode, lock reasons and bare markers', () => {
    const entries = manager.parseWorktreeList([
      'worktree /tmp/пробел в пути', 'HEAD ' + 'a'.repeat(40), 'branch refs/heads/main', '',
      'worktree /tmp/second', 'HEAD ' + 'b'.repeat(40), 'detached', 'locked уходит в отпуск', '',
      'worktree /tmp/third', 'bare', 'prunable gitdir file points nowhere', '',
    ].join('\0') + '\0')
    assert.equal(entries.length, 3)
    assert.deepEqual(entries[0], { path: '/tmp/пробел в пути', head: 'a'.repeat(40), branch: 'refs/heads/main', bare: false, detached: false, locked: null, prunable: null })
    assert.equal(entries[1].locked, 'уходит в отпуск')
    assert.equal(entries[1].detached, true)
    assert.equal(entries[2].bare, true)
    assert.equal(entries[2].prunable, 'gitdir file points nowhere')
  })

  check('CLI inspect projects one record with its checkout and typed not-found', () => {
    const fixture = fixtureRepo()
    const checkout = managedFixture(fixture)
    const { record } = writeRecord(fixture, { executionRoot: identityOf(checkout) })
    const found = spawnSync(process.execPath, [CLI, 'inspect', '--worktree-id', record.worktreeId],
      { encoding: 'utf8', env: envFor(fixture) })
    assert.equal(found.status, 0, found.stderr)
    const projection = JSON.parse(found.stdout)
    assert.equal(projection.record.worktreeId, record.worktreeId)
    assert.equal(projection.checkout.classification, 'managed')
    const missing = spawnSync(process.execPath, [CLI, 'inspect', '--worktree-id', 'wt-' + '00'.repeat(16)],
      { encoding: 'utf8', env: envFor(fixture) })
    assert.equal(missing.status, 4)
    assert.match(missing.stderr, /WORKTREE_RECORD_NOT_FOUND/)
  })

  check('a control root below the repo toplevel and duplicate claims are typed findings', () => {
    const fixture = fixtureRepo()
    mkdirSync(join(fixture.root, 'sub'))
    const subResult = spawnSync(process.execPath, [CLI, 'discover'], {
      encoding: 'utf8',
      env: { ...envFor(fixture), ORCHESTRATOR_PROJECT_ROOT: join(fixture.root, 'sub') },
      maxBuffer: 8 * 1024 * 1024,
    })
    const subProjection = JSON.parse(subResult.stdout)
    assert.equal(subResult.status, 3)
    assert.ok(subProjection.findings.some((finding) => finding.code === 'CONTROL_ROOT_NOT_IN_INVENTORY'))

    const dup = fixtureRepo()
    const checkout = managedFixture(dup)
    writeRecord(dup, { executionRoot: identityOf(checkout) })
    writeRecord(dup, { worktreeId: 'wt-' + 'ba'.repeat(16), executionRoot: identityOf(checkout) })
    const dupResult = discoverIn(dup)
    assert.equal(dupResult.status, 3)
    assert.ok(findingCodes(dupResult.projection).includes('WORKTREE_RECORD_DUPLICATE_CLAIM'))
    const row = dupResult.projection.worktrees.find((entry) => entry.path.endsWith('wt-managed'))
    assert.equal(row.classification, 'unsafe', 'ambiguous ownership is never awarded')
    assert.ok(!findingCodes(dupResult.projection).includes('WORKTREE_RECORD_ORPHANED'),
      'doubly-claiming records are a duplicate-claim problem, not orphans')
  })

  check('manager constants are the published contract surface', () => {
    assert.equal(manager.DEFAULT_MIN_DISK_BYTES, 2 * 1024 * 1024 * 1024)
    assert.equal(manager.MANAGER_NAMESPACE_PREFIX, 'refs/heads/orchestrator/task/')
    assert.ok(contract.CANDIDATE_REF_RE.test(CANDIDATE_REF))
  })

  console.log(`worktree-manager: ${checks} checks passed`)
} finally {
  while (roots.length) {
    const dir = roots.pop()
    try { chmodSync(dir, 0o700) } catch {}
    rmSync(dir, { recursive: true, force: true })
  }
}
