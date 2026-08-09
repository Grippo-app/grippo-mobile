import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'

import { sessionsMenu } from '../scripts/sessions-menu.js'

const SITE = fileURLToPath(new URL('../', import.meta.url))
const source = readFileSync(join(SITE, 'scripts', 'sessions-menu.js'), 'utf8')
const css = readFileSync(join(SITE, 'styles', 'panels.css'), 'utf8')

test('session history includes persisted contexts that are no longer on the board', () => {
  const sessions = sessionsMenu._test.sessionsFromSnapshot({
    progress: { boardStems: ['TASK_1_current'] },
    sessions: {
      'task:TASK_1_current': { stem: 'TASK_1_current', running: true },
      'task:TASK_0_old': { stem: 'TASK_0_old', running: false, endedAt: '2026-07-22T10:00:00.000Z' },
      'figma:whoami': { running: false, endedAt: '2026-07-21T10:00:00.000Z' },
    },
  })

  assert.deepEqual(
    sessions.map((session) => session.key).sort(),
    ['figma:whoami', 'task:TASK_0_old', 'task:TASK_1_current'],
  )
})

test('Sessions header entry is permanent and its complete history scrolls', () => {
  assert.doesNotMatch(source, /els\.pill\.hidden\s*=/)
  assert.doesNotMatch(source, /class:\s*'site-status-pill sessions-pill'[\s\S]{0,80}hidden:\s*true/)
  assert.doesNotMatch(source, /MAX_VISIBLE|\.slice\(0,\s*MAX_VISIBLE\)/)
  assert.match(source, /class:\s*'sessions-list'/)
  assert.match(css, /\.sessions-list\s*\{[\s\S]*?max-height:[\s\S]*?overflow-y:\s*auto;/)
})
