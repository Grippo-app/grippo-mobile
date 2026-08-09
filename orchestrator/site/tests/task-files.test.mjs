import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { linkSync, mkdirSync, mkdtempSync, rmSync, symlinkSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { after, test } from 'node:test'

const root = mkdtempSync(join(tmpdir(), 'task-files-'))
const tasks = join(root, 'orchestrator', 'tasks')
for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasks, column), { recursive: true })
process.env.ORCHESTRATOR_PROJECT_ROOT = root

const require = createRequire(import.meta.url)
const taskFiles = require('../server/task-files.js')

after(() => rmSync(root, { recursive: true, force: true }))

test('task file reader serves only bounded canonical direct children from the project root', () => {
  const stem = 'TASK_1_fixture'
  const markdown = '# TASK 1 — Fixture\n'
  writeFileSync(join(tasks, 'todo', stem + '.md'), markdown)
  assert.equal(taskFiles.read('todo', stem).bytes.toString('utf8'), markdown)
  assert.throws(() => taskFiles.read('unknown', stem), (error) => error.code === 'task-file-column-invalid' && error.httpStatus === 400)
  assert.throws(() => taskFiles.read('todo', '../escape'), (error) => error.code === 'task-file-stem-invalid' && error.httpStatus === 400)
  for (const invalidStem of ['TASK_0_zero', 'TASK_01_leading_zero', 'TASK_9007199254740992_unsafe']) {
    assert.throws(() => taskFiles.read('todo', invalidStem), (error) => error.code === 'task-file-stem-invalid' && error.httpStatus === 400)
  }
  assert.throws(() => taskFiles.read('pending', stem), (error) => error.code === 'task-file-not-found' && error.httpStatus === 404)
})

test('task file reader rejects symlinks, hardlinks and oversized task bodies', () => {
  const outside = join(root, 'outside.md')
  writeFileSync(outside, '# outside\n')
  symlinkSync(outside, join(tasks, 'backlog', 'TASK_2_symlink.md'))
  linkSync(outside, join(tasks, 'done', 'TASK_3_hardlink.md'))
  writeFileSync(join(tasks, 'todo', 'TASK_4_large.md'), Buffer.alloc(taskFiles.MAX_TASK_BYTES + 1, 97))
  for (const [column, stem] of [['backlog', 'TASK_2_symlink'], ['done', 'TASK_3_hardlink'], ['todo', 'TASK_4_large']]) {
    assert.throws(() => taskFiles.read(column, stem), (error) => error.code === 'task-file-not-found' && error.httpStatus === 404)
  }
})
