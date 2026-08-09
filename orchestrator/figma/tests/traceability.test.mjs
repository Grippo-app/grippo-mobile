#!/usr/bin/env node
// Machine-reviewable pins for REQ-TRACE-001..005 and REQ-CONC-001. This test
// deliberately validates references into behavioral suites rather than
// accepting prose-only rule rows.
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { dirname, join, resolve, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = resolve(HERE, '..', '..', '..')
const TRACE = join(REPO, 'orchestrator', 'figma', 'traceability')
const readJson = (name) => JSON.parse(readFileSync(join(TRACE, name), 'utf8'))
const underRepo = (relative) => {
  assert.equal(typeof relative, 'string')
  assert.equal(relative.includes('\\'), false)
  const absolute = resolve(REPO, relative)
  assert.ok(absolute.startsWith(REPO + sep), `${relative} escapes repository`)
  assert.ok(existsSync(absolute), `${relative} does not exist`)
  return absolute
}

const registry = readJson('business-rules.json')
assert.equal(registry.schemaVersion, 1)
assert.ok(Array.isArray(registry.rules) && registry.rules.length >= 8)
const requiredRuleFields = [
  'ruleId', 'domain', 'businessQuestion', 'authority', 'preconditions', 'exactInputsRevisions',
  'decisionTable', 'outputStatusFinding', 'severity', 'allowedNextActions', 'forbiddenActions',
  'typedErrors', 'auditFields', 'positiveTestIds', 'negativeTestIds', 'stalePartialTestId',
  'concurrencyTestId', 'uiCopyActionReference', 'taskFinalizationEffect', 'codeRefs', 'schemaRefs'
]
const ruleIds = new Set()
for (const rule of registry.rules) {
  assert.deepEqual(Object.keys(rule).sort(), requiredRuleFields.slice().sort(), `${rule.ruleId} exact fields`)
  assert.match(rule.ruleId, /^[A-Z][A-Z0-9-]{2,79}$/)
  assert.equal(ruleIds.has(rule.ruleId), false, `duplicate ${rule.ruleId}`)
  ruleIds.add(rule.ruleId)
  for (const key of ['preconditions', 'exactInputsRevisions', 'decisionTable', 'outputStatusFinding',
    'allowedNextActions', 'forbiddenActions', 'typedErrors', 'auditFields', 'positiveTestIds',
    'negativeTestIds', 'codeRefs', 'schemaRefs']) {
    assert.ok(Array.isArray(rule[key]) && rule[key].length > 0, `${rule.ruleId}.${key} must be non-empty`)
  }
  for (const ref of [...rule.codeRefs, ...rule.schemaRefs]) underRepo(ref)
  for (const decision of rule.decisionTable) {
    assert.deepEqual(Object.keys(decision).sort(), ['testContains', 'testRef', 'then', 'when'].sort())
    assert.ok(decision.when && decision.then && decision.testContains)
    const source = readFileSync(underRepo(decision.testRef), 'utf8')
    assert.ok(source.includes(decision.testContains),
      `${rule.ruleId} decision test marker ${JSON.stringify(decision.testContains)} missing in ${decision.testRef}`)
  }
}

// Every normative REQ id is owned exactly once and points to executable or
// inspectable evidence. This prevents broad prose rules from silently leaving
// an individual requirement unimplemented.
const requirementTrace = readJson('requirements.json')
assert.deepEqual(Object.keys(requirementTrace).sort(), ['groups', 'schemaVersion'])
assert.equal(requirementTrace.schemaVersion, 1)
const tracedIds = new Set()
for (const group of requirementTrace.groups) {
  assert.deepEqual(Object.keys(group).sort(), ['codeRefs', 'owner', 'requirements', 'schemaRefs', 'testRefs', 'uiRefs'].sort())
  assert.match(group.owner, /^[a-z][a-z0-9-]+$/)
  assert.ok(group.requirements.length > 0, `${group.owner} has no requirements`)
  assert.ok(group.codeRefs.length > 0 && group.testRefs.length > 0 && group.schemaRefs.length > 0,
    `${group.owner} must cite code, tests, and a machine contract`)
  for (const id of group.requirements) {
    assert.match(id, /^REQ-[A-Z]+-[0-9]+$/)
    assert.equal(tracedIds.has(id), false, `${id} is owned more than once`)
    tracedIds.add(id)
  }
  for (const ref of [...group.codeRefs, ...group.testRefs, ...group.schemaRefs, ...group.uiRefs]) underRepo(ref)
}
assert.equal(tracedIds.size, 137, 'machine-readable requirement registry must stay exact and complete')

const decisions = readJson('decisions.json')
assert.equal(decisions.schemaVersion, 1)
const decisionIds = new Set()
for (const row of decisions.decisions) {
  assert.deepEqual(Object.keys(row).sort(), ['changeMechanism', 'choice', 'decisionId', 'evidence', 'owner', 'question', 'ruleIds', 'tests'].sort())
  assert.match(row.decisionId, /^DEC-[A-Z0-9-]+$/)
  assert.equal(decisionIds.has(row.decisionId), false)
  decisionIds.add(row.decisionId)
  assert.ok(row.ruleIds.every((ruleId) => ruleIds.has(ruleId)), `${row.decisionId} cites an unknown rule`)
  assert.ok(row.evidence.length && row.tests.length && row.choice && row.changeMechanism)
}

const races = readJson('mutation-race-matrix.json')
assert.equal(races.schemaVersion, 1)
assert.equal(new Set(races.flows).size, races.flows.length)
const order = new Map(races.flows.map((flow, index) => [flow, index]))
const expectedPairs = races.flows.length * (races.flows.length + 1) / 2
assert.equal(races.pairs.length, expectedPairs, 'every unordered mutating-flow pair must have a race rule')
const pairKeys = new Set()
for (const pair of races.pairs) {
  assert.deepEqual(Object.keys(pair).sort(), ['left', 'right', 'rule', 'testRef'].sort())
  assert.ok(order.has(pair.left) && order.has(pair.right))
  assert.ok(order.get(pair.left) <= order.get(pair.right), `${pair.left}/${pair.right} is not canonical`)
  const key = pair.left + '\0' + pair.right
  assert.equal(pairKeys.has(key), false, `duplicate race pair ${pair.left}/${pair.right}`)
  pairKeys.add(key)
  assert.ok(pair.rule.length >= 20)
  underRepo(pair.testRef)
}

console.log(`traceability.test: ${tracedIds.size} requirements, ${registry.rules.length} rules, ${decisions.decisions.length} decisions, ${races.pairs.length} race pairs verified`)
