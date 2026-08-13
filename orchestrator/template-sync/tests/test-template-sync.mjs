import assert from 'node:assert/strict'
import { copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, renameSync, rmSync, symlinkSync, unlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

import { templateIntegrityFindings } from '../check-integrity.mjs'

const here = dirname(fileURLToPath(import.meta.url))
const root = join(here, '..', '..', '..')
const sourceGenerator = join(here, '..', '_generate_template_manifest.py')
const sourceSync = join(here, '..', 'sync-from-template.sh')
const sourceTrackedGate = join(here, '..', 'check-tracked-runtime-artifacts.mjs')
const sourceExclusionGate = join(here, '..', 'check-template-exclusion-contract.mjs')

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    encoding: 'utf8',
    env: { ...process.env, ...(options.env || {}) },
    maxBuffer: 64 * 1024 * 1024,
  })
  if (result.error) throw result.error
  return result
}

function output(result) {
  return `${result.stdout || ''}${result.stderr || ''}`
}

function expectStatus(result, status, label) {
  assert.equal(result.status, status, `${label}\n${output(result)}`)
}

const pureMissing = templateIntegrityFindings({
  manifest: { files: { 'orchestrator/owned.txt': 'abc' }, stampedAt: '2026-01-01T00:00:00Z' },
  current: {},
  templateFiles: null,
  nowMs: Date.parse('2026-01-02T00:00:00Z'),
})
assert.ok(pureMissing.some((finding) => finding.msg.includes('MISSING')), 'pure verdict must report a missing stamped file')

const scratch = mkdtempSync(join(tmpdir(), 'orchestrator-template-sync-'))
const product = join(scratch, 'product')
try {
  const trackedFixture = join(scratch, 'tracked-fixture')
  mkdirSync(trackedFixture)
  expectStatus(run('git', ['init', '-q', trackedFixture]), 0, 'tracked-runtime fixture init must succeed')
  const trackedPaths = [
    'vendor/node_modules/pkg/index.js',
    'orchestrator/.cache/tasks/runs/receipt.json',
    'orchestrator/figma/.account.json',
    'orchestrator/figma/.env',
    'orchestrator/api-contract/.env',
    'orchestrator/api-contract/.secrets/backend.env',
  ]
  for (const rel of ['orchestrator/README.md', ...trackedPaths]) {
    const file = join(trackedFixture, rel)
    mkdirSync(dirname(file), { recursive: true })
    writeFileSync(file, 'fixture\n')
  }
  expectStatus(run('git', ['-C', trackedFixture, 'add', '-f', '.']), 0, 'tracked-runtime fixture add must succeed')
  const trackedFailure = run('node', [sourceTrackedGate, trackedFixture])
  expectStatus(trackedFailure, 1, 'tracked runtime artifacts must fail closed')
  for (const rel of trackedPaths) assert.match(output(trackedFailure), new RegExp(rel.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  expectStatus(run('git', ['-C', trackedFixture, 'rm', '-q', '--cached', ...trackedPaths]), 0,
    'tracked-runtime fixture cleanup must succeed')
  expectStatus(run('node', [sourceTrackedGate, trackedFixture]), 0,
    'ordinary tracked source must pass runtime-artifact hygiene')

  expectStatus(run('node', [sourceExclusionGate, root]), 0,
    'source manifest exclusions and ignore documentation must agree')

  const exclusionFixture = join(scratch, 'exclusion-fixture')
  const fixtureGenerator = join(exclusionFixture, 'orchestrator', 'template-sync', '_generate_template_manifest.py')
  const fixtureLaunch = join(exclusionFixture, 'orchestrator', 'launch.md')
  const fixtureIgnore = join(exclusionFixture, '.gitignore')
  for (const [source, destination] of [
    [sourceGenerator, fixtureGenerator],
    [join(root, 'orchestrator', 'launch.md'), fixtureLaunch],
    [join(root, '.gitignore'), fixtureIgnore],
  ]) {
    mkdirSync(dirname(destination), { recursive: true })
    copyFileSync(source, destination)
  }
  const ignoreBytes = readFileSync(fixtureIgnore)
  writeFileSync(fixtureIgnore, readFileSync(fixtureIgnore, 'utf8').replace(
    '# END ORCHESTRATOR RUNTIME IGNORE CONTRACT',
    'orchestrator/.root-only-runtime/\n# END ORCHESTRATOR RUNTIME IGNORE CONTRACT',
  ))
  let exclusionFailure = run('node', [sourceExclusionGate, exclusionFixture])
  expectStatus(exclusionFailure, 1, 'root-only runtime ignore must fail lockstep')
  assert.match(output(exclusionFailure), /root \.gitignore runtime pattern is absent from launch Step 2\.5/)
  writeFileSync(fixtureIgnore, ignoreBytes)

  const launchBytes = readFileSync(fixtureLaunch)
  writeFileSync(fixtureLaunch, readFileSync(fixtureLaunch, 'utf8').replace(
    '# END ORCHESTRATOR RUNTIME IGNORE CONTRACT',
    'orchestrator/.launch-only-runtime/\n# END ORCHESTRATOR RUNTIME IGNORE CONTRACT',
  ))
  exclusionFailure = run('node', [sourceExclusionGate, exclusionFixture])
  expectStatus(exclusionFailure, 1, 'launch-only runtime ignore must fail lockstep')
  assert.match(output(exclusionFailure), /launch Step 2\.5 runtime pattern is absent from root \.gitignore/)
  writeFileSync(fixtureLaunch, launchBytes)
  expectStatus(run('node', [sourceExclusionGate, exclusionFixture]), 0,
    'exclusion gate must recover after exact fixture restoration')

  mkdirSync(product)
  const sourceScan = run('python3', [sourceGenerator, '--print', '--root', root])
  expectStatus(sourceScan, 0, 'source manifest scan must succeed')
  const sourceManifest = JSON.parse(sourceScan.stdout)
  const ownedFiles = Object.keys(sourceManifest.files || {})
  assert.ok(ownedFiles.length > 500, 'consumer test must exercise the complete template-owned tree')
  assert.ok(ownedFiles.includes('orchestrator/figma/tokens/baseline.mjs'),
    'committed Figma token logic must remain template-owned')
  assert.ok(ownedFiles.includes('orchestrator/figma/manifests/.gitkeep'),
    'the committed Figma manifests placeholder must remain template-owned')
  for (const rel of ownedFiles) {
    const destination = join(product, rel)
    mkdirSync(dirname(destination), { recursive: true })
    copyFileSync(join(root, rel), destination)
  }

  const configRel = join('orchestrator', 'project-config.md')
  const productConfig = join(product, configRel)
  copyFileSync(join(root, configRel), productConfig)
  const configured = readFileSync(productConfig, 'utf8').replace(/^productName: (?!SampleApp$).*$/m, 'productName: SampleApp')
  assert.notEqual(configured, readFileSync(productConfig, 'utf8'), 'fixture product identity must replace the source productName (template placeholder or bound product value)')
  writeFileSync(productConfig, configured)

  const productGenerator = join(product, 'orchestrator', 'template-sync', '_generate_template_manifest.py')
  const productChecker = join(product, 'orchestrator', 'template-sync', 'check-integrity.mjs')
  expectStatus(run('python3', [productGenerator, '--root', product]), 0, 'fresh consumer stamp must succeed')
  expectStatus(run('node', [productChecker]), 0, 'fresh stamped consumer must be clean')

  // Product-owned config and task changes stay outside the template integrity surface.
  writeFileSync(productConfig, `${configured.trimEnd()}\n# product-owned fixture change\n`)
  const backlog = join(product, 'orchestrator', 'tasks', 'backlog')
  mkdirSync(backlog, { recursive: true })
  writeFileSync(join(backlog, 'TASK_1_fixture.md'), '# TASK 1 — Fixture\n')
  const projectAdapters = join(product, 'orchestrator', 'figma', 'project-adapters.json')
  writeFileSync(projectAdapters, '{"schemaVersion":2,"adapters":[]}\n')
  expectStatus(run('node', [productChecker], { env: { TEMPLATE_ROOT: root } }), 0,
    'configured consumer and its generated Figma adapter must stay clean and match the live template')

  if (process.platform !== 'win32') {
    const rogueLink = join(product, 'orchestrator', 'rogue-link')
    symlinkSync('project-config.md', rogueLink)
    const linkedCheck = run('node', [productChecker])
    expectStatus(linkedCheck, 1, 'extra symlink must be a drift finding')
    assert.match(output(linkedCheck), /extra.*orchestrator\/rogue-link/i)
    const linkedStamp = run('python3', [productGenerator, '--print', '--root', product])
    assert.notEqual(linkedStamp.status, 0, 'generator must not stamp around an unsafe symlink')
    assert.match(output(linkedStamp), /unsafe symlink file.*orchestrator\/rogue-link/i)
    unlinkSync(rogueLink)

    const fifo = join(product, 'orchestrator', 'rogue-fifo')
    const fifoCreate = run('mkfifo', [fifo])
    if (fifoCreate.status === 0) {
      const fifoCheck = run('node', [productChecker])
      expectStatus(fifoCheck, 1, 'extra special file must be a drift finding without being read')
      assert.match(output(fifoCheck), /extra.*orchestrator\/rogue-fifo/i)
      const fifoStamp = run('python3', [productGenerator, '--print', '--root', product])
      assert.notEqual(fifoStamp.status, 0, 'generator must not stamp around an unsafe special file')
      assert.match(output(fifoStamp), /unsafe special file.*orchestrator\/rogue-fifo/i)
      unlinkSync(fifo)
    }
  }

  const ownedReadme = join(product, 'orchestrator', 'README.md')
  unlinkSync(ownedReadme)
  const missingCheck = run('node', [productChecker])
  expectStatus(missingCheck, 1, 'missing template-owned file must be reported')
  assert.match(output(missingCheck), /MISSING.*orchestrator\/README\.md/)

  // The live checkout may itself be a configured product (bound identity in
  // project-config.md); the sync directionality guard rightly refuses such a
  // tree as a SOURCE. Build a template-identity source fixture from the same
  // owned files so the sync mechanics are provable in both checkouts.
  const templateSource = join(scratch, 'template-source')
  mkdirSync(templateSource)
  for (const rel of ownedFiles) {
    const destination = join(templateSource, rel)
    mkdirSync(dirname(destination), { recursive: true })
    copyFileSync(join(root, rel), destination)
  }
  const sourceConfig = join(templateSource, configRel)
  copyFileSync(join(root, configRel), sourceConfig)
  writeFileSync(sourceConfig, readFileSync(sourceConfig, 'utf8').replace(/^productName: .*$/m, 'productName: <Product>'))

  const dryRun = run('bash', [sourceSync, templateSource, product])
  expectStatus(dryRun, 0, 'sync preview must stay read-only and succeed')
  assert.match(output(dryRun), /to copy\s+: 1 file/)
  assert.match(output(dryRun), /<- orchestrator\/README\.md/)
  assert.match(output(dryRun), /DRY-RUN \(nothing written\)/)
  assert.equal(existsSync(ownedReadme), false, 'dry-run must not restore the missing file')

  const apply = run('bash', [sourceSync, templateSource, product, '--apply'])
  expectStatus(apply, 0, 'explicit sync apply must restore the reviewed file')
  assert.equal(readFileSync(ownedReadme, 'utf8'), readFileSync(join(templateSource, 'orchestrator', 'README.md'), 'utf8'))
  expectStatus(run('node', [productChecker], { env: { TEMPLATE_ROOT: templateSource } }), 0,
    'applied consumer must be re-stamped from and match the live template')

  const extraFile = join(product, 'orchestrator', 'rogue-extra.txt')
  writeFileSync(extraFile, 'not shipped\n')
  const extraCheck = run('node', [productChecker])
  expectStatus(extraCheck, 1, 'extra regular file must be reported')
  assert.match(output(extraCheck), /extra.*orchestrator\/rogue-extra\.txt/i)
  unlinkSync(extraFile)

  if (process.platform !== 'win32') {
    const orchestrator = join(product, 'orchestrator')
    const realOrchestrator = join(product, 'orchestrator-real')
    renameSync(orchestrator, realOrchestrator)
    symlinkSync('orchestrator-real', orchestrator, 'dir')
    const rootStamp = run('python3', [join(realOrchestrator, 'template-sync', '_generate_template_manifest.py'), '--print', '--root', product])
    assert.notEqual(rootStamp.status, 0, 'generator must reject a symlinked orchestrator root')
    assert.match(output(rootStamp), /unsafe symlink orchestrator root/)
    const rootCheck = run('node', [join(orchestrator, 'template-sync', 'check-integrity.mjs')])
    expectStatus(rootCheck, 2, 'checker must reject a symlinked orchestrator root without walking it')
    assert.match(output(rootCheck), /orchestrator root is not a real directory/)
    unlinkSync(orchestrator)
    renameSync(realOrchestrator, orchestrator)
  }
} finally {
  rmSync(scratch, { recursive: true, force: true })
}

console.log('ok - template consumer stamp, drift detection, safe sync, and unsafe-entry fencing')
