import assert from 'node:assert/strict'
import {
  chmodSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync,
  statSync, writeFileSync,
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'
import { test } from 'node:test'

const root = mkdtempSync(join(tmpdir(), 'task-inbox-'))
const inbox = join(root, 'cache', 'inbox')
mkdirSync(join(root, 'orchestrator', 'tasks'), { recursive: true })

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_TASK_INBOX_DIR = inbox
process.env.ORCHESTRATOR_TASK_INBOX_AUTHORITY_ROOT = root

const require = createRequire(import.meta.url)
const backlogCreate = require('../server/backlog-create.js')
const originalCreate = backlogCreate.create
let createCalls = 0
backlogCreate.create = async (input) => {
  createCalls++
  assert.match(input.idempotencyKey, /^setup-inbox:INBOX_[a-f0-9]{40}$/)
  return {
    ok: true,
    created: true,
    replayed: false,
    stem: 'TASK_1_saved_before_setup',
    sourceHash: 'sha256:' + 'a'.repeat(64),
  }
}
const taskInbox = require('../server/task-inbox.js')

const payload = {
  title: 'Saved before Setup',
  body: '## Goal\nKeep this task durable without launching AI.',
  idempotencyKey: 'setup-inbox-test-key-0001',
}

test('pre-Setup inbox saves private durable records and replays the same intent', () => {
  const created = taskInbox.save(payload)
  assert.equal(created.created, true)
  assert.equal(created.published, false)
  assert.match(created.entry.id, /^INBOX_[a-f0-9]{40}$/)

  const files = readdirSync(inbox).filter((name) => /^INBOX_.*\.json$/.test(name))
  assert.equal(files.length, 1)
  if (process.platform !== 'win32') {
    assert.equal(statSync(inbox).mode & 0o777, 0o700)
    assert.equal(statSync(join(inbox, files[0])).mode & 0o777, 0o600)
  }
  const stored = JSON.parse(readFileSync(join(inbox, files[0]), 'utf8'))
  assert.equal(stored.title, payload.title)
  assert.equal(stored.body, payload.body)
  assert.equal(Object.hasOwn(stored, 'idempotencyKey'), false)

  const replay = taskInbox.save(payload)
  assert.equal(replay.created, false)
  assert.equal(replay.entry.id, created.entry.id)
  assert.deepEqual(taskInbox.list().entries.map((entry) => entry.id), [created.entry.id])
})

test('the inbox rejects changed payloads under the same idempotency key', () => {
  assert.throws(() => taskInbox.save({ ...payload, title: 'Changed title' }),
    (error) => error && error.code === 'TASK_INBOX_IDEMPOTENCY_CONFLICT' && error.httpStatus === 409)
  assert.throws(() => taskInbox.save({ ...payload, extra: true }),
    (error) => error && error.code === 'bad-task-inbox-request' && error.httpStatus === 400)
})

test('publication is deterministic, removes the item from pending list, and survives response replay', async () => {
  const id = taskInbox.list().entries[0].id
  const published = await taskInbox.publish(id)
  assert.equal(published.published, true)
  assert.equal(published.stem, 'TASK_1_saved_before_setup')
  assert.equal(published.inboxRetained, false)
  assert.equal(createCalls, 1)
  assert.deepEqual(taskInbox.list().entries, [])

  const replay = await taskInbox.publish(id)
  assert.equal(replay.published, true)
  assert.equal(replay.replayed, true)
  assert.equal(replay.stem, 'TASK_1_saved_before_setup')
  assert.equal(createCalls, 1)
})

test('foreign public entries fail closed instead of entering the inbox projection', () => {
  writeFileSync(join(inbox, 'foreign.json'), '{}\n')
  if (process.platform !== 'win32') chmodSync(join(inbox, 'foreign.json'), 0o600)
  assert.throws(() => taskInbox.list(),
    (error) => error && error.code === 'task-inbox-entry-invalid')
})

test.after(() => {
  backlogCreate.create = originalCreate
  rmSync(root, { recursive: true, force: true })
})
