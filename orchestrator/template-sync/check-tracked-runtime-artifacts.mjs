#!/usr/bin/env node

import { spawnSync } from 'node:child_process'
import { resolve } from 'node:path'

const root = resolve(process.argv[2] || process.cwd())
const listed = spawnSync('git', ['-C', root, 'ls-files', '-z'], {
  encoding: 'buffer',
  maxBuffer: 64 * 1024 * 1024,
})

if (listed.error || listed.status !== 0) {
  console.error('TRACKED_RUNTIME_CHECK_UNAVAILABLE: git ls-files failed for ' + root)
  process.exit(2)
}

function forbidden(path) {
  const parts = path.split('/')
  return parts.includes('node_modules') || parts.includes('.env') ||
    path === 'orchestrator/.cache' || path.startsWith('orchestrator/.cache/') ||
    path === 'orchestrator/figma/.account.json' ||
    path === 'orchestrator/api-contract/.secrets' || path.startsWith('orchestrator/api-contract/.secrets/')
}

const paths = listed.stdout.toString('utf8').split('\0').filter(Boolean)
const violations = paths.filter(forbidden).sort()
for (const path of violations) console.error('TRACKED_RUNTIME_ARTIFACT: ' + path)
if (violations.length) {
  console.error('Remove generated/runtime/secret artifacts from the Git index; ignore rules alone are not sufficient.')
  process.exit(1)
}

console.log('OK: Git tracks no forbidden runtime, dependency, cache, account, or secret artifacts.')
