#!/usr/bin/env node

// Canonical producer entry point. The caller supplies phase facts only; this
// helper owns checkpoint identity, task/project/config/dependency revisions,
// hashing, bounded publication and the fail-closed retention cap.

import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const checkpoints = require('../site/server/task-checkpoints.js')
const source = require('./task-source-contract.cjs')

const MAX_STDIN_BYTES = 32 * 1024

function readInput() {
  const chunks = []
  let total = 0
  const buffer = Buffer.alloc(4096)
  while (total <= MAX_STDIN_BYTES) {
    const count = fs.readSync(0, buffer, 0, Math.min(buffer.length, MAX_STDIN_BYTES + 1 - total), null)
    if (!count) break
    chunks.push(Buffer.from(buffer.subarray(0, count)))
    total += count
  }
  if (total > MAX_STDIN_BYTES) throw new Error('checkpoint input exceeds its byte limit')
  return JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(Buffer.concat(chunks, total)))
}

export function main(argv = process.argv.slice(2)) {
  if (argv.length !== 3 || argv[0] !== 'create' || argv[1] !== '--stem' ||
      !source.safeTaskStem(argv[2])) {
    process.stderr.write('usage: node task-checkpoint.mjs create --stem TASK_<N>_<slug>\n')
    return 2
  }
  let input
  try { input = readInput() }
  catch (error) {
    process.stdout.write(JSON.stringify({ schemaVersion: 1, ok: false, error: 'checkpoint-producer-input-invalid' }) + '\n')
    return 2
  }
  const result = checkpoints.create(argv[2], input)
  process.stdout.write(JSON.stringify(result) + '\n')
  return result.ok ? 0 : 1
}

const invoked = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
if (invoked) process.exitCode = main()
