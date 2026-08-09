#!/usr/bin/env node

// Narrow CLI for the prompt-driven /serve-queue worker.  Exit 2 means an edit
// marker (or unsafe marker state) blocks the requested task; stdout is always
// one JSON object so callers do not scrape prose.

import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import contract from './edit-marker-contract.cjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const PROJECT_ROOT = resolve(process.env.EDIT_BACKLOG_PROJECT_ROOT || join(HERE, '..', '..'))
const DIR = resolve(process.env.EDIT_BACKLOG_EDITS_DIR || join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'edits'))
const args = process.argv.slice(2)
const command = args.shift()

function emit(value) { process.stdout.write(JSON.stringify(value) + '\n') }
function die(value, status = 2) { emit(value); process.exit(status) }

if (command === 'scan' && args.length === 0) {
  const result = contract.scan(DIR)
  emit(result)
  process.exit(result.issues.length ? 2 : 0)
}
if (command === 'guard' && args.length === 2 && args[0] === '--stem' && contract.validStem(args[1])) {
  const issue = contract.blockingIssue(DIR, args[1])
  if (issue) die({ ok: false, blocked: true, issue })
  emit({ ok: true, blocked: false, stem: args[1] })
  process.exit(0)
}
die({ ok: false, error: 'usage: edit-marker.mjs scan | guard --stem TASK_N_name' })
