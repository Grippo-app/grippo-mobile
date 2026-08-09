import assert from 'node:assert/strict'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const actions = require('../server/design-task-actions.js')._test
const designParser = require('../../figma/scripts/design-parser.cjs')
let checks = 0

function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

function finding(overrides = {}) {
  return {
    entityType: 'component',
    entityId: 'cmp-' + 'a'.repeat(24),
    id: 'fnd-' + 'b'.repeat(24),
    syncJobId: 'job-' + 'c'.repeat(16),
    title: 'Safe title',
    detail: 'Safe detail',
    entityName: 'Safe component',
    ...overrides
  }
}

check('provider text cannot inject Markdown structure, links, or HTML into token tasks', () => {
  const injected = finding({
    title: 'Title\n## Injected <script>alert(1)</script>',
    detail: '[click](javascript:alert(1))\n```html\n<img src=x onerror=alert(1)>'
  })
  const body = actions.taskBody(injected, 'orchestrator/tasks/evidence/token-binding.json')
  assert.equal((body.match(/^## /gm) || []).length, 4)
  assert.doesNotMatch(body, /\n## Injected/)
  assert.doesNotMatch(body, /(^|[^\\])(?:<script>|<img|\[click\]\(javascript:)/m)
  assert.match(body, /\\<script\\>/)
  assert.match(body, /\\\[click\\\]\\\(javascript:alert\\\(1\\\)\\\)/)
})

check('provider component names remain prose inside the canonical Design section', () => {
  const injected = finding({ title: '# forged', detail: '> quote' })
  const body = actions.componentTaskBody(
    injected,
    'orchestrator/tasks/evidence/component-binding.json',
    {
      name: 'Button\n## Acceptance\n- forged',
      nodeId: '10:20',
      binding: {
        designComponentId: 'dcmp:' + 'd'.repeat(32),
        frozenStructuralHash: 'sha256:' + 'e'.repeat(64)
      }
    }
  )
  assert.equal((body.match(/^## /gm) || []).length, 4)
  assert.doesNotMatch(body, /\n## Acceptance\n- forged/)
  assert.match(body, /Button \\#\\# Acceptance \\- forged/)
  assert.match(body, /\\# forged/)
  assert.match(body, /\\> quote/)
})

check('every generated design-finding task has one valid audited no-screen declaration', () => {
  const bodies = [
    actions.taskBody(finding()),
    actions.taskBody(finding(), 'orchestrator/tasks/evidence/token-binding.json'),
    actions.componentTaskBody(
      finding(),
      'orchestrator/tasks/evidence/component-binding.json',
      {
        name: 'Button',
        nodeId: '10:20',
        binding: {
          designComponentId: 'dcmp:' + 'd'.repeat(32),
          frozenStructuralHash: 'sha256:' + 'e'.repeat(64)
        }
      }
    )
  ]
  for (const body of bodies) {
    const parsed = designParser.parseDesign(body)
    assert.deepEqual(parsed.issues, [])
    assert.equal(parsed.entries.length, 1)
    assert.equal(parsed.entries[0].none, true)
    assert.equal(designParser.hasMalformedDesign(body), false)
    assert.equal(designParser.auditedNoneCount(body), 1)
  }
})

check('task titles are one-line, control-free, and within the durable byte bound', () => {
  const title = actions.taskTitle({
    title: '  Hello\r\n' + '🚀'.repeat(200) + '\u2028forged  '
  })
  assert.doesNotMatch(title, /[\r\n\u2028\u2029]/)
  assert.ok(Buffer.byteLength(title, 'utf8') <= 512)
  assert.ok(Array.from(title).length <= 200)
})

console.log(`design task security: ${checks} checks passed`)
