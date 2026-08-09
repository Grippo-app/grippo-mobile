#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { test } from 'node:test'

const require = createRequire(import.meta.url)
const sessions = require('../server/sessions.js')

function sourceFor(text, options = {}) {
  return {
    safeTaskStem: () => true,
    readIndex: () => options.indexUnavailable
      ? null
      : { rows: [{ column: options.column || 'todo', row: { stem: 'TASK_1_gate' } }] },
    readTask: () => options.taskUnavailable ? null : { text },
  }
}

const parser = {
  hasPullableDesign: (text) => text === 'pullable',
  uiTaskWithoutDesign: (text) => text === 'undeclared-ui'
    ? { level: 'block' }
    : text === 'possible-ui' ? { level: 'warn' } : null,
}

test('screenshot run gate applies only to proven visual todo tasks', () => {
  assert.equal(sessions.screenshotGateApplies('TASK_1_gate', {
    taskSource: sourceFor('backend-only'), designParser: parser,
  }), false)
  assert.equal(sessions.screenshotGateApplies('TASK_1_gate', {
    taskSource: sourceFor('pullable'), designParser: parser,
  }), true)
  assert.equal(sessions.screenshotGateApplies('TASK_1_gate', {
    taskSource: sourceFor('undeclared-ui'), designParser: parser,
  }), true)
  assert.equal(sessions.screenshotGateApplies('TASK_1_gate', {
    taskSource: sourceFor('possible-ui'), designParser: parser,
  }), false)
})

test('screenshot classifier does not create a second availability gate', () => {
  assert.equal(sessions.screenshotGateApplies('TASK_1_gate', {
    taskSource: sourceFor('pullable', { column: 'backlog' }), designParser: parser,
  }), false)
  assert.equal(sessions.screenshotGateApplies('TASK_1_gate', {
    taskSource: sourceFor('pullable', { indexUnavailable: true }), designParser: parser,
  }), false)
  assert.equal(sessions.screenshotGateApplies('TASK_1_gate', {
    taskSource: sourceFor('pullable', { taskUnavailable: true }), designParser: parser,
  }), false)
})
