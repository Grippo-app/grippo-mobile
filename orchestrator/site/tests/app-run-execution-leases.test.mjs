#!/usr/bin/env node
// Phase 5 resource policy (pipeline improvement 01, §15/§16). A worktree
// isolates files; it does NOT isolate a device. Two task-bound app runs may
// therefore build in parallel, but they must never boot, install, launch or
// capture on the same device — nor install the same application id — at the
// same time. The exclusion is expressed as leases whose whole identity lives in
// the key, so conflicts are exact-resource conflicts and nothing wider.

import assert from 'node:assert/strict'
import { mkdtempSync, readFileSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const writerLeases = require('../../tasks/writer-leases.cjs')
const HERE = dirname(fileURLToPath(import.meta.url))
const APP_RUNNER = join(HERE, '..', 'server', 'app-runner.js')

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }
const roots = []

function leaseDir() {
  const root = mkdtempSync(join(tmpdir(), 'app-run-leases-'))
  roots.push(root)
  return { dir: join(root, '.writers'), root }
}
function acquire(scope, kind, key) {
  return writerLeases.acquire(scope.dir, {
    kind, key, sessionId: writerLeases.createSessionId(), ownerPid: process.pid, rootDir: scope.root,
  })
}
function activeKeys(scope) {
  return writerLeases.scan(scope.dir, scope.root).active.map((row) => row.key).sort()
}

try {
  check('the new classes are part of the closed kind registry', () => {
    const source = readFileSync(join(HERE, '..', '..', 'tasks', 'writer-leases.cjs'), 'utf8')
    // No 'integration-writer': the transaction serializes on its WAL record,
    // and a registered kind nobody acquires is a promise the code never keeps.
    for (const kind of ['execution-writer', 'resource-writer']) {
      assert.ok(new RegExp(`'${kind}': 1`).test(source), `${kind} is not registered`)
    }
    // A kind outside the registry is refused, so the classes cannot be smuggled.
    const scope = leaseDir()
    assert.throws(() => acquire(scope, 'device-writer', 'device:android:x'), /kind/)
  })

  check('two isolated executions are compatible; the same one is not', () => {
    const scope = leaseDir()
    const first = acquire(scope, 'execution-writer', 'execution:wt-' + 'a'.repeat(32))
    const second = acquire(scope, 'execution-writer', 'execution:wt-' + 'b'.repeat(32))
    assert.deepEqual(activeKeys(scope), [
      'execution:wt-' + 'a'.repeat(32), 'execution:wt-' + 'b'.repeat(32),
    ].sort())
    // The lease system itself never rejects a second row; exclusion is decided
    // by the conflict rules, which compare the exact key. Same key, same tree.
    const rows = writerLeases.scan(scope.dir, scope.root).active
    const sameKey = rows.filter((row) => row.key === first.record.key)
    assert.equal(sameKey.length, 1, 'one execution key names exactly one live execution')
    writerLeases.release(first); writerLeases.release(second)
  })

  check('a device and a bundle are separate exact resources', () => {
    const scope = leaseDir()
    const device = acquire(scope, 'resource-writer', 'device:android:hint-1')
    const bundle = acquire(scope, 'resource-writer', 'bundle:android:com.example.app')
    const other = acquire(scope, 'resource-writer', 'device:android:hint-2')
    assert.deepEqual(activeKeys(scope), [
      'bundle:android:com.example.app', 'device:android:hint-1', 'device:android:hint-2',
    ])
    // A device key and a bundle key never alias each other.
    assert.notEqual(device.record.key, bundle.record.key)
    writerLeases.release(device); writerLeases.release(bundle); writerLeases.release(other)
  })

  check('app-run asks for exclusion proportional to its isolation', () => {
    const source = readFileSync(APP_RUNNER, 'utf8')
    // A control-root job still demands sole-writer: it builds the shared tree.
    assert.match(source, /kind: 'runtime-build', key: 'app-run:runtime-build', requireSoleWriter: true/)
    // A task-bound job names its worktree, its device and its bundle instead.
    assert.match(source, /kind: 'execution-writer', key: 'execution:' \+ job\.worktreeId/)
    assert.match(source, /key: 'device:' \+ job\.platform \+ ':' \+ job\.targetStableHint/)
    assert.match(source, /key: 'bundle:' \+ job\.platform \+ ':' \+ job\.applicationId/)
    // Partial acquisition is never left standing.
    assert.match(source, /releaseLeases\(handles\);\s*\n\s*return admission;/)
  })

  check('a task-bound job builds in its own checkout, not the control root', () => {
    const source = readFileSync(APP_RUNNER, 'utf8')
    // §26: the mode is decided ONCE and never re-derived with a `||` fallback,
    // which would silently turn a half-bound job into a control-root build.
    assert.match(source, /function jobExecutionBinding\(taskStem\)/)
    assert.match(source, /function jobExecutionBindingCurrent\(job\)/)
    assert.match(source, /if \(!jobExecutionBindingCurrent\(job\)\) \{\s*releaseLeases\(lease\.handles\);/)
    assert.match(source, /function jobProductRoot\(job\)/)
    assert.match(source, /executionRoot: jobProductRoot\(job\)/)
    assert.match(source, /worktreeId: execution\.binding \? execution\.binding\.worktreeId : null/)
    assert.match(source, /candidateTree: execution\.candidateTree/)
    assert.match(source, /worktreeManager\.executionPinFor\(taskStem, binding\.runId\)/)
    assert.doesNotMatch(source, /\|\| paths\.PROJECT_ROOT/,
      'no call site may resolve a root by falling back to the control root')
    // The private job contract carries the binding, so an artifact can never be
    // attributed to the wrong tree.
    for (const field of ['worktreeId', 'executionRoot', 'executionRunId', 'candidateTree', 'applicationId']) {
      assert.ok(new RegExp(`'${field}'`).test(source), `${field} missing from the job contract`)
    }
    const android = readFileSync(join(HERE, '..', 'server', 'android-runner.js'), 'utf8')
    assert.match(android, /cwd: context\.executionRoot\b/)
    assert.doesNotMatch(android, /context\.executionRoot \|\| paths\.PROJECT_ROOT/)
    // A context with no proven root refuses with a typed code instead of
    // crashing inside path.join or quietly reading the shared tree.
    assert.match(android, /function assertProductRoot\(context\)/)
    const ios = readFileSync(join(HERE, '..', 'server', 'ios-runner.js'), 'utf8')
    assert.doesNotMatch(ios, /context\.executionRoot \|\| paths\.PROJECT_ROOT/)
    assert.match(ios, /function assertProductRoot\(context\)/)
    assert.match(android, /executable: context\.gradlew \|\| context\.tools\.gradlew/)
  })
  check('build caches are task-local and no control secret follows a job', () => {
    const source = readFileSync(APP_RUNNER, 'utf8')
    // §14: two checkouts sharing a run config must not share one DerivedData
    // tree, or their products overwrite each other.
    assert.match(source, /function derivedDataRoot\(runConfigHash, worktreeId\)/)
    assert.match(source, /scope \+ '-' \+ String\(worktreeId\)\.slice\(-12\)/)
    const ios = readFileSync(join(HERE, '..', 'server', 'ios-runner.js'), 'utf8')
    assert.match(ios, /context\.worktreeId \? scope \+ '-' \+ String\(context\.worktreeId\)\.slice\(-12\) : scope/)
    // §13: provisioning materialises exactly the manager's own skill/contract
    // copies into a checkout — no credential file, no local.properties, and no
    // recursive copy of anything under the control root.
    const manager = readFileSync(join(HERE, '..', 'server', 'worktree-manager.js'), 'utf8')
    assert.ok(!/local\.properties/.test(manager), 'a checkout never receives local.properties')
    assert.ok(!/\.secrets/.test(manager), 'a checkout never receives control secrets')
  })

  check('every consumer of a task-bound build follows the tree that was built', () => {
    // Re-rooting the BUILD without its readers is the exact trap this phase's
    // review found: gradle ran in the checkout while the APK locator, the Xcode
    // project and the preflight still named the control root.
    const android = readFileSync(join(HERE, '..', 'server', 'android-runner.js'), 'utf8')
    assert.match(android, /var outputRoot = path\.join\(context\.executionRoot,/)
    const ios = readFileSync(join(HERE, '..', 'server', 'ios-runner.js'), 'utf8')
    assert.match(ios, /'-project', path\.join\(context\.executionRoot, variant\.project\)/)
    assert.match(ios, /cwd: context\.executionRoot,/)
    const source = readFileSync(APP_RUNNER, 'utf8')
    // The preflight judges against the same build root the run will use.
    assert.match(source, /binding \? binding\.executionRoot : paths\.PROJECT_ROOT/)
    // A typed process may run inside a manager-owned checkout, which lives
    // beside the control root — otherwise no task-bound build can spawn at all.
    const proc = readFileSync(join(HERE, '..', 'server', 'app-run-process.js'), 'utf8')
    assert.match(proc, /\[paths\.PROJECT_ROOT, paths\.WORKTREE_HOME\]/)
  })

  check('stored jobs have no schema compatibility path, and every lease is renewed', () => {
    const source = readFileSync(APP_RUNNER, 'utf8')
    assert.doesNotMatch(source, /migrateStoredJob|LEGACY_RUN_JOB_KEYS|pre-upgrade/i)
    // Renewing only the build lease would let the device exclusion expire
    // under a long build while the job is still installing on that device.
    assert.match(source, /function beginRenewal\(handles, controller, leaseState\)/)
    assert.match(source, /list\.forEach\(function \(handle\) \{ writerLeases\.renew\(handle, LEASE_TTL_MS\); \}\)/)
    const leases = readFileSync(join(HERE, '..', '..', 'tasks', 'writer-leases.cjs'), 'utf8')
    // ONE definition of the group-leader set: it was written out three times.
    assert.match(leases, /function groupLeaderKind\(kind\)/)
    assert.match(leases, /kind === 'runtime-build' \|\| kind === 'execution-writer'/)
    assert.equal((leases.match(/groupLeaderKind\(/g) || []).length, 4, 'one definition, three call sites')
    // The same lesson, learned again: "which writers are globally exclusive"
    // was written out THREE times — the site arbiter, the guarded CLI acquire
    // and standby — and the copies had already diverged on
    // figma:ship-drift-artifacts, so the CLI admitted a writer the site
    // refuses. One definition, and no file may carry a private copy.
    assert.match(leases, /function deterministicPublisherLease\(row\)/)
    assert.match(leases, /row\.key === 'figma:ship-drift-artifacts'/)
    for (const consumer of [
      join(HERE, '..', 'server', 'finalizations.js'),
      join(HERE, '..', '..', 'tasks', 'writer-lease.mjs'),
      join(HERE, '..', 'scripts', 'standby-queue.mjs'),
    ]) {
      const text = readFileSync(consumer, 'utf8')
      assert.match(text, /writerLeases\.deterministicPublisherLease\(/,
        consumer + ' must ask the shared definition')
      assert.doesNotMatch(text, /^function deterministic\w*(Publication|Publisher)Lease/m,
        consumer + ' must not keep a private copy')
    }
  })

} finally {
  for (const root of roots) { try { rmSync(root, { recursive: true, force: true }) } catch (error) {} }
}

console.log(`\napp-run execution leases: ${checks} checks passed`)
