#!/usr/bin/env node

// Contract fixtures are executable evidence, not decoration. Every expectation
// below is DERIVED from the contract document itself — edit a contract and the
// fixtures must follow, or this test fails. The `valid` fixture must satisfy
// its contract; the `invalid` fixture must violate it, so a fixture pair can
// never quietly degrade into two copies of the same passing document.
// builder-report has its own schema + parser and is covered by
// test-test-policy-contract.mjs; these three contracts are prose-only.

import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const CONTRACTS = join(HERE, '..', '..', 'contracts');
const FIXTURES = join(CONTRACTS, 'fixtures');

const failures = [];
let checks = 0;
function check(name, fn) {
  checks++;
  try { fn(); console.log(`PASS ${name}`); }
  catch (error) { failures.push({ name, error }); console.error(`FAIL ${name}\n${error && error.stack || error}`); }
}

const read = (...parts) => readFileSync(join(...parts), 'utf8');
const headingsOf = (markdown) => markdown.split('\n')
  .filter((line) => /^#{1,4} /.test(line))
  .map((line) => line.trimEnd());

// A contract heading may carry a placeholder (`# Implementation plan —
// TASK_<N>_<title>`); only the literal prefix can be matched against a
// concrete fixture.
const literalPrefix = (heading) => heading.split('<')[0].trimEnd();

// `## Required headings, in order` — the numbered, backticked list every
// prose contract uses.
function requiredHeadings(contractFile) {
  const source = read(CONTRACTS, contractFile);
  const section = source.split('## Required headings, in order')[1];
  assert.ok(section, `${contractFile} must declare its frozen heading order`);
  const found = [...section.split('\n## ')[0].matchAll(/^\s*\d+\.\s+`(#{1,4} [^`]+)`/gm)].map((m) => m[1]);
  assert.ok(found.length > 0, `${contractFile} heading list is unreadable`);
  return found;
}

function indexOfHeadings(fixtureHeadings, required) {
  return required.map((heading) => {
    const prefix = literalPrefix(heading);
    return fixtureHeadings.findIndex((line) => line === heading || line.startsWith(prefix));
  });
}

function assertOrderedHeadings(fixture, required, label) {
  const positions = indexOfHeadings(headingsOf(fixture), required);
  required.forEach((heading, index) => {
    assert.notEqual(positions[index], -1, `${label}: missing required heading ${heading}`);
  });
  for (let index = 1; index < positions.length; index++) {
    assert.ok(positions[index] > positions[index - 1],
      `${label}: ${required[index]} must follow ${required[index - 1]}`);
  }
}

function violatesHeadings(fixture, required) {
  const positions = indexOfHeadings(headingsOf(fixture), required);
  if (positions.some((position) => position === -1)) return true;
  return positions.some((position, index) => index > 0 && position <= positions[index - 1]);
}

check('planner-output fixtures follow the contract heading order', () => {
  const required = requiredHeadings('planner-output.md');
  assert.equal(required.length, 12, 'the planner contract declares exactly 12 required headings');
  assertOrderedHeadings(read(FIXTURES, 'planner-output.valid.md'), required, 'planner-output.valid.md');
  assert.ok(violatesHeadings(read(FIXTURES, 'planner-output.invalid.md'), required),
    'planner-output.invalid.md must break the frozen heading order');
});

check('planner-output fixtures carry the frozen Test contract sub-headings', () => {
  const source = read(CONTRACTS, 'planner-output.md');
  const line = source.split('\n').find((text) => /`## Test contract`/.test(text) && /### /.test(text));
  assert.ok(line, 'the contract must name the Test contract sub-heading order');
  // The same sub-heading may be named twice on that line (once in the order,
  // once in its own rule), so the order is the de-duplicated first mention.
  const subHeadings = [...new Set([...line.matchAll(/`(### [^`]+)`/g)].map((m) => m[1]))];
  assert.equal(subHeadings.length, 6, 'six frozen sub-headings');
  assertOrderedHeadings(read(FIXTURES, 'planner-output.valid.md'), subHeadings, 'planner-output.valid.md');

  // `### Manual` is the single optional heading and sits between `### Automated`
  // and `## Test contract`.
  const headings = headingsOf(read(FIXTURES, 'planner-output.valid.md'));
  const at = (needle) => headings.findIndex((text) => text.startsWith(needle));
  assert.ok(at('### Manual') > at('### Automated') && at('### Manual') < at('## Test contract'),
    'the optional Manual heading keeps its declared position');
});

check('execution-plan fixtures follow the contract heading order and pin the gate sequence', () => {
  const required = requiredHeadings('execution-plan.md');
  assert.equal(required.length, 7, 'the execution-plan contract declares exactly 7 required headings');
  const valid = read(FIXTURES, 'execution-plan.valid.md');
  assertOrderedHeadings(valid, required, 'execution-plan.valid.md');
  assert.ok(violatesHeadings(read(FIXTURES, 'execution-plan.invalid.md'), required),
    'execution-plan.invalid.md must break the frozen heading order');

  // The ordering line is verbatim contract text, so the fixture cannot drift
  // into a different gate order.
  const contractSource = read(CONTRACTS, 'execution-plan.md');
  const sequence = /Step 4 \(compile\)[^\n]*/.exec(contractSource);
  assert.ok(sequence, 'the contract must spell out the gate ordering');
  const steps = [...sequence[0].matchAll(/\d+(?:\.\d+)?[a-z]?\s*\(/g)].map((m) => m[0].replace(/\s*\($/, ''));
  assert.ok(steps.length >= 6, 'the gate sequence names every step');
  const gateSection = valid.split('## Gate sequence')[1];
  assert.ok(gateSection, 'the valid fixture carries a Gate sequence section');
  let cursor = -1;
  for (const step of steps) {
    const next = gateSection.indexOf(step, cursor + 1);
    assert.notEqual(next, -1, `execution-plan.valid.md omits gate step ${step}`);
    assert.ok(next > cursor, `execution-plan.valid.md reorders gate step ${step}`);
    cursor = next;
  }
});

check('validator-finding fixtures satisfy every pin the contract declares', () => {
  const source = read(CONTRACTS, 'validator-finding.md');
  const requiredLine = /Required keys:\s*`([^`]+)`/.exec(source);
  assert.ok(requiredLine, 'the contract must list its required keys');
  const requiredKeys = requiredLine[1].split(',').map((key) => key.trim()).filter(Boolean);
  const enumOf = (field) => {
    const match = new RegExp('`' + field + '` ∈ `([^`]+)`').exec(source);
    assert.ok(match, `the contract must pin the ${field} enum`);
    return match[1].split('|').map((value) => value.trim());
  };
  const statuses = enumOf('status');
  const severities = enumOf('severity');

  const valid = JSON.parse(read(FIXTURES, 'validator-finding.valid.json'));
  for (const key of requiredKeys) {
    assert.ok(Object.hasOwn(valid, key), `validator-finding.valid.json is missing required key ${key}`);
  }
  assert.ok(statuses.includes(valid.status), 'valid fixture status is in the frozen enum');
  assert.ok(severities.includes(valid.severity), 'valid fixture severity is in the frozen enum');
  assert.equal(valid.dedup_key, `${valid.file}::${valid.rule_id}`,
    'dedup_key is exactly <file>::<rule_id>');
  if (valid.status === 'skipped') assert.notEqual(valid.skip_reason, null, 'a skipped finding needs a reason');

  // The invalid fixture must break at least one pin — otherwise the pair is
  // two copies of the same passing document.
  const invalid = JSON.parse(read(FIXTURES, 'validator-finding.invalid.json'));
  const broken = requiredKeys.some((key) => !Object.hasOwn(invalid, key)) ||
    !statuses.includes(invalid.status) ||
    !severities.includes(invalid.severity) ||
    invalid.dedup_key !== `${invalid.file}::${invalid.rule_id}` ||
    (invalid.status === 'skipped' && invalid.skip_reason === null);
  assert.ok(broken, 'validator-finding.invalid.json must violate a declared pin');
});

if (failures.length > 0) {
  console.error(`contract-fixtures: ${failures.length}/${checks} checks failed`);
  process.exit(1);
}
console.log(`contract-fixtures: ${checks} checks passed`);
