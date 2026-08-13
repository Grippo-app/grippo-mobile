#!/usr/bin/env node
// Narrow worktree CLI for standby and recovery flows (pipeline improvement
// 01). `discover` prints the manager's full classification projection and
// `inspect` one record's — both read-only. `release` is the EXPLICIT operator
// recovery surface: it removes one manager-owned generation (its checkout and
// candidate ref) so a task whose provisioning failed, or whose sealed inputs
// moved on, can be run again. It never touches foreign state, never forces
// anything, and refuses whenever ownership or cleanliness is unproven.
// Provisioning itself is a server operation — this CLI never creates.

import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const manager = require('../site/server/worktree-manager.js')
const worktreeContract = require('./worktree-record-contract.cjs')

function die(message, code = 2) {
  process.stderr.write(String(message) + '\n')
  process.exit(code)
}

const [command, ...args] = process.argv.slice(2)

if (command === 'discover') {
  if (args.length) die('discover takes no arguments')
  const projection = manager.discover()
  process.stdout.write(JSON.stringify(projection) + '\n')
  // Blockers make discovery exit non-zero so scripted callers fail closed
  // without parsing; the projection still carries the full detail.
  process.exit(projection.findings.some((finding) => finding.severity === 'blocker') ? 3 : 0)
} else if (command === 'inspect') {
  if (args.length !== 2 || args[0] !== '--worktree-id') die('usage: task-worktree.mjs inspect --worktree-id <wt-id>')
  const worktreeId = args[1]
  if (!worktreeContract.WORKTREE_ID_RE.test(String(worktreeId || ''))) die('[WORKTREE_ID_INVALID] not a canonical worktree id')
  const projection = manager.discover()
  const record = projection.records.worktrees.active.find((item) => item.record.worktreeId === worktreeId)
  if (!record) die('[WORKTREE_RECORD_NOT_FOUND] no active record for ' + worktreeId, 4)
  const checkout = projection.worktrees.find((row) => row.record && row.record.worktreeId === worktreeId) || null
  process.stdout.write(JSON.stringify({
    version: 1,
    record: record.record,
    checkout: checkout ? {
      path: checkout.path, branch: checkout.branch, head: checkout.head,
      classification: checkout.classification
    } : null,
    findings: projection.findings.filter((finding) => finding.message.indexOf(worktreeId) >= 0)
  }) + '\n')
  process.exit(0)
} else if (command === 'release') {
  if (args.length !== 2 || args[0] !== '--worktree-id') die('usage: task-worktree.mjs release --worktree-id <wt-id>')
  const worktreeId = args[1]
  if (!worktreeContract.WORKTREE_ID_RE.test(String(worktreeId || ''))) die('[WORKTREE_ID_INVALID] not a canonical worktree id')
  const result = manager.release(worktreeId)
  if (!result.ok) die('[' + (result.code || 'RELEASE_FAILED') + '] ' + (result.message || ''), 5)
  process.stdout.write(JSON.stringify({ version: 1, released: worktreeId }) + '\n')
  process.exit(0)
} else if (command === 'metrics') {
  // The §20 Phase-6 figures the owner needs before raising the concurrency cap:
  // provisioning latency, disk headroom, revalidation rate, integration
  // recovery rate, and the generation/cleanup counts. Read-only.
  if (args.length) die('metrics takes no arguments')
  const value = manager.metrics()
  process.stdout.write(JSON.stringify(value) + '\n')
  process.exit(value.unavailable ? 3 : 0)
} else if (command === 'integrate') {
  // The headless equivalent of the Board's Integrate/Resume integration: it
  // drives the SAME write-ahead log, so starting here and finishing there (or
  // the other way round) is one transaction, not two. `--preview` is read-only.
  if (args.length < 2 || args[0] !== '--stem') {
    die('usage: task-worktree.mjs integrate --stem <STEM> [--preview]')
  }
  const stem = args[1]
  const previewOnly = args.length === 3 && args[2] === '--preview'
  if (args.length > 3 || (args.length === 3 && !previewOnly)) {
    die('usage: task-worktree.mjs integrate --stem <STEM> [--preview]')
  }
  const integrations = require('../site/server/integrations.js')
  if (previewOnly) {
    const projection = integrations.preview(stem)
    process.stdout.write(JSON.stringify(projection) + '\n')
    process.exit(projection.ok && projection.state !== 'blocked' ? 0 : 6)
  }
  const existing = integrations.readOne(stem)
  const driver = existing.ok ? integrations.resume : integrations.begin
  driver(stem, (result) => {
    process.stdout.write(JSON.stringify(result) + '\n')
    if (!result.ok) {
      process.stderr.write('[' + (result.code || 'INTEGRATION_FAILED') + '] ' + (result.message || '') + '\n')
      process.exit(result.code === 'INTEGRATION_RECOVERY_REQUIRED' ? 7 : 6)
    }
    process.exit(0)
  })
} else {
  die('usage: task-worktree.mjs discover | inspect --worktree-id <wt-id> | release --worktree-id <wt-id> | integrate --stem <STEM> [--preview] | metrics')
}
