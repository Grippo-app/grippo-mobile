#!/usr/bin/env node
// Phase 5 root separation (pipeline improvement 01, §12.2). A Figma script now
// answers two different questions with two different roots: "where is the
// product I am analysing" (the execution root — during a run, the isolated
// checkout carrying the candidate) and "where do durable artifacts live" (the
// control root — caches, reports, screen caches, receipts, and the canonical
// branch key). Before this split every script anchored BOTH to its own file
// location, so a run inside a worktree analysed the right code but wrote its
// reports into the checkout — where they died with it — and derived the branch
// key from the temporary candidate branch.

import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const UTIL = join(HERE, '..', 'scripts', '_util.mjs')
const SCRIPTS = join(HERE, '..', 'scripts')

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`PASS ${name}`) }
const roots = []

function scopeOf(env) {
  const script = `import(${JSON.stringify(UTIL)}).then((u) => {
    process.stdout.write(JSON.stringify({
      projectRoot: u.PROJECT_ROOT, executionRoot: u.EXECUTION_ROOT,
      cacheRoot: u.FIGMA_CACHE_ROOT, reports: u.figmaPath('reports'),
      screens: u.figmaScreensRoot(),
    }))
  })`
  const out = execFileSync(process.execPath, ['-e', script], {
    encoding: 'utf8', env: { ...process.env, ...env },
  })
  return JSON.parse(out)
}

try {
  check('with no pins both roots collapse to the installation, exactly as before', () => {
    const scope = scopeOf({ ORCHESTRATOR_PROJECT_ROOT: undefined, ORCHESTRATOR_EXECUTION_ROOT: undefined })
    assert.equal(scope.executionRoot, scope.projectRoot)
    assert.ok(scope.cacheRoot.startsWith(scope.projectRoot + '/'), scope.cacheRoot)
  })

  check('a raw execution-root override is refused without its exact manager proof', () => {
    const parent = mkdtempSync(join(tmpdir(), 'figma-scope-'))
    roots.push(parent)
    const control = join(parent, 'control')
    const execution = join(parent, 'checkout')
    mkdirSync(control); mkdirSync(execution)
    assert.throws(() => scopeOf({
      ORCHESTRATOR_PROJECT_ROOT: control,
      ORCHESTRATOR_EXECUTION_ROOT: execution,
    }), /execution environment|manager-issued|EXECUTION_ENVIRONMENT/i)
  })

  check('a leftover writer identity cannot silently downgrade to control mode', () => {
    assert.throws(() => scopeOf({
      ORCHESTRATOR_WRITER_STEM: 'TASK_7_probe',
    }), /execution environment|manager-issued|EXECUTION_ENVIRONMENT/i)
  })

  check('the execution root defaults to the control root when only the control root is pinned', () => {
    const parent = mkdtempSync(join(tmpdir(), 'figma-scope-'))
    roots.push(parent)
    const control = join(parent, 'control')
    mkdirSync(control)
    const scope = scopeOf({ ORCHESTRATOR_PROJECT_ROOT: control, ORCHESTRATOR_EXECUTION_ROOT: undefined })
    assert.equal(scope.projectRoot, control)
    assert.equal(scope.executionRoot, control)
  })

  check('product source scans read the execution root, not the control root', () => {
    // A source-text contract on the exact defaults, because the alternative is
    // building a whole Compose fixture to observe the same fact indirectly.
    const scans = [
      ['component-census.mjs', [': [EXECUTION_ROOT])', "executionProductInputPath(value, 'FIGMA_CENSUS_CODE_ROOTS')"]],
      ['check-capture-config.mjs', [': [EXECUTION_ROOT])', "executionProductInputPath(value, 'FIGMA_CENSUS_CODE_ROOTS')"]],
      ['check-stub-text.mjs', [': [EXECUTION_ROOT])', "executionProductInputPath(value, 'FIGMA_CENSUS_CODE_ROOTS')"]],
      ['extract-app-tokens.mjs', ['(files.length ? [] : [EXECUTION_ROOT])', 'executionProductInputPath(value']],
      ['lib/design-locale.mjs', ['deriveResourceRoots(rootDirs = [EXECUTION_ROOT])',
        "executionProductInputPath(value, 'FIGMA_STRING_RESOURCE_ROOTS')"]],
    ]
    for (const [file, needles] of scans) {
      const source = readFileSync(join(SCRIPTS, file), 'utf8')
      for (const needle of needles) {
        assert.ok(source.includes(needle),
          `${file} no longer defaults to or confines scans within the execution root`)
      }
    }
    // The gate driver builds and records against the candidate too.
    const driver = readFileSync(join(SCRIPTS, 'run-figma-gates.mjs'), 'utf8')
    assert.ok(driver.includes('cwd: EXECUTION_ROOT'), 'gate steps must run in the execution root')
    assert.ok(driver.includes("join(EXECUTION_ROOT, 'gradlew')"), 'the wrapper must come from the execution root')
    assert.ok(!/cwd: PROJECT_ROOT/.test(driver), 'no gate step may still run in the control root')
  })

  check('control-plane helper CLIs resolve from the installation, never from the project root', () => {
    // A configured project root can be a sandbox or an execution checkout; the
    // helpers themselves are code and must come from where this script lives.
    const source = readFileSync(join(SCRIPTS, 'descope-task.mjs'), 'utf8')
    assert.ok(source.includes("const HELPERS_DIR = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..', 'tasks')"))
    assert.ok(!/join\(PROJECT_ROOT, 'orchestrator', 'tasks', '(writer-lease|transition-task-state|task-lock)\.mjs'\)/.test(source))
  })
} finally {
  for (const root of roots) { try { rmSync(root, { recursive: true, force: true }) } catch (error) {} }
}

console.log(`\nexecution-root-scope: ${checks} checks passed`)
