#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const source = require('../task-source-contract.cjs')
const apiWorkPackage = require('../api-work-package-contract.cjs')

let checks = 0
function check(name, fn) {
  fn()
  checks++
  process.stdout.write(`ok ${checks} - ${name}\n`)
}

function documentFor(value, tail = '## Goal\n\nShip it.') {
  return '# TASK 1 — Source contract\n\n' + source.render(value) + (tail ? '\n\n' + tail : '') + '\n'
}

check('every registered Kind/Type pair round-trips through the exact block', () => {
  for (const [kind, types] of Object.entries(source.SOURCE_TYPES)) {
    for (const type of types) {
      const metadata = type === 'api-work-package'
        ? apiWorkPackage.create('area:users', [
          'api:missing:getUser',
          'api:missing:listUsers',
        ])
        : null
      const ref = kind === 'follow-up'
        ? 'TASK_9_parent'
        : metadata
          ? 'api:package:' + metadata.packageId
          : kind + ':' + type
      const value = { kind, type, ref, fingerprint: source.sha256(kind + '\0' + type + '\0' + ref) }
      const tail = metadata
        ? apiWorkPackage.render(metadata) + '\n\n## Goal\n\nShip it.'
        : '## Goal\n\nShip it.'
      const parsed = source.parse(documentFor(value, tail))
      assert.equal(parsed.present, true)
      assert.equal(parsed.valid, true)
      assert.deepEqual(parsed.source, value)
      assert.equal(parsed.block, source.render(value))
      if (metadata) assert.deepEqual(parsed.package.value, metadata)
    }
  }
})

check('API package aliases are mandatory, canonical and bound to Source.ref', () => {
  const metadata = apiWorkPackage.create('area:billing', [
    'api:change:chg-' + 'a'.repeat(24),
    'api:mismatch:mismatch-' + 'b'.repeat(24),
  ])
  const value = {
    kind: 'api',
    type: 'api-work-package',
    ref: 'api:package:' + metadata.packageId,
    fingerprint: source.sha256('package-source'),
  }
  const valid = documentFor(
    value,
    apiWorkPackage.render(metadata) + '\n\n## Goal\n\nShip it.',
  )
  assert.equal(source.parse(valid).valid, true)
  assert.equal(
    source.parse(documentFor(value)).error,
    'task-source-api-package-invalid',
  )

  const other = apiWorkPackage.create('area:orders', metadata.sourceIds)
  assert.equal(
    source.parse(valid.replace(
      apiWorkPackage.render(metadata),
      apiWorkPackage.render(other),
    )).error,
    'task-source-api-package-invalid',
  )
  assert.equal(
    source.parse(valid + '\n' + apiWorkPackage.render(metadata) + '\n').error,
    'task-source-api-package-invalid',
  )
  assert.equal(
    source.parse(valid.replace(
      apiWorkPackage.render(metadata) + '\n\n## Goal',
      '## Goal\n\nShip it.\n\n' + apiWorkPackage.render(metadata) +
        '\n\n## Notes',
    )).error,
    'task-source-api-package-invalid',
  )
})

check('placement, field order, duplicate headings and extra prose fail closed', () => {
  const value = source.manualForIntent('intent-stable', 'manual', 'intent-stable')
  assert.equal(source.parse('# TASK 1 — Bad\n\n## Goal\n\nX\n\n' + source.render(value) + '\n').valid, false)
  assert.equal(source.parse(documentFor(value).replace('- Kind: manual\n- Type:', '- Type: manual\n- Kind:')).valid, false)
  assert.equal(source.parse(documentFor(value).replace('- Ref:', 'Unexpected prose\n- Ref:')).valid, false)
  const duplicate = documentFor(value) + '\n' + source.render(value) + '\n'
  assert.equal(source.parse(duplicate).error, 'task-source-duplicate')
  assert.equal(source.parse('\uFEFF' + documentFor(value)).error, 'task-source-malformed')
  assert.equal(source.parse(documentFor(value).replace(/\n/g, '\r\n')).error, 'task-source-malformed')
})

check('CommonMark-hidden lookalikes do not satisfy the mandatory provenance contract', () => {
  const fenced = '# TASK 1 — Fenced example\n\n```md\n## Source\n```\n'
  const commented = '# TASK 1 — Commented example\n\n<!--\n## Source\n-->\n'
  assert.deepEqual(source.parse(fenced), { present: false, valid: false, error: 'task-source-missing' })
  assert.deepEqual(source.parse(commented), { present: false, valid: false, error: 'task-source-missing' })
})

check('unsafe refs, credentials and mismatched follow-up refs are rejected', () => {
  const fp = source.sha256('unsafe')
  for (const ref of ['/tmp/private', 'C:\\private', 'https://user:pass@example.test/node', 'node?api_key=secret']) {
    assert.equal(source.validate({ kind: 'manual', type: 'manual', ref, fingerprint: fp }), null)
  }
  assert.equal(source.validate({ kind: 'follow-up', type: 'task-split', ref: 'not-a-task', fingerprint: fp }), null)
  assert.equal(source.validate({
    kind: 'follow-up', type: 'task-split', ref: 'TASK_1_' + 'a'.repeat(114), fingerprint: fp,
  }), null)
  assert.equal(source.validate({
    kind: 'follow-up', type: 'task-split', ref: 'TASK_9007199254740992_unsafe', fingerprint: fp,
  }), null)
})

check('missing Source is rejected without provenance inference', () => {
  assert.deepEqual(source.parse('# TASK 1 — Missing\n\n## Goal\n\nShip it.\n'), {
    present: false, valid: false, error: 'task-source-missing'
  })
  assert.equal(Object.hasOwn(source, 'legacy'), false)
  assert.equal(Object.hasOwn(source, 'project'), false)
})

check('body injection rejects only a real Source heading and keeps fenced examples', () => {
  const value = source.manualForIntent('intent-inject', 'manual', 'intent-inject')
  assert.match(source.injectBody('## Goal\n\nDone.', value), /^## Source\n/)
  assert.throws(() => source.injectBody('## Source\n\n- example', value), /Source section/)
  assert.match(source.injectBody('```md\n## Source\n```', value), /```md\n## Source/)
})

check('CJS and Python source registries stay in exact parity', async () => {
  const { readFileSync } = await import('node:fs')
  const { join, dirname } = await import('node:path')
  const { fileURLToPath } = await import('node:url')
  const here = dirname(fileURLToPath(import.meta.url))
  const py = readFileSync(join(here, '..', 'create-backlog.py'), 'utf8')
  const block = /SOURCE_TYPES = \{([\s\S]*?)\}\n/.exec(py)
  assert.ok(block, 'python registry block present')
  for (const [kind, types] of Object.entries(source.SOURCE_TYPES)) {
    const row = new RegExp('"' + kind + '":\\s*\\{([^}]*)\\}').exec(block[1])
    assert.ok(row, kind)
    const pyTypes = [...row[1].matchAll(/"([a-z0-9-]+)"/g)].map((m) => m[1]).sort()
    assert.deepEqual(pyTypes, [...types].sort(), 'registry parity for kind ' + kind)
  }
  assert.ok(source.SOURCE_TYPES['follow-up'].includes('test-foundation-prerequisite'),
    'the foundation prerequisite Source type is registered — no third registry exists')
})

check('the test-foundation prerequisite factory is deterministic and intent-bound', () => {
  const intent = 'sha256:' + 'ab'.repeat(32)
  const first = source.testFoundationPrerequisite('TASK_9_parent', intent)
  const second = source.testFoundationPrerequisite('TASK_9_parent', intent)
  assert.deepEqual(first, second, 'same parent + same intent → byte-identical provenance')
  assert.equal(first.kind, 'follow-up')
  assert.equal(first.type, 'test-foundation-prerequisite')
  assert.equal(first.ref, 'TASK_9_parent')
  assert.notEqual(first.fingerprint,
    source.testFoundationPrerequisite('TASK_9_parent', 'sha256:' + 'cd'.repeat(32)).fingerprint,
    'a different foundation intent yields a different fingerprint')
  assert.deepEqual(first,
    source.followUp('TASK_9_parent', 'test-foundation-prerequisite', 'ab'.repeat(32)),
    'one fingerprint formula — the special factory never invents a second hash scheme')
  assert.throws(() => source.testFoundationPrerequisite('TASK_9_parent', 'sha256:short'), /intent hash/)
})

process.stdout.write(`task-source-contract: ${checks} checks passed\n`)
