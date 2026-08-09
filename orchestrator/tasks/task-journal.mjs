#!/usr/bin/env node

// Canonical bounded writer for one task pipeline-journal event.  The Python
// Python CLI constructs the event, while this helper owns the rooted,
// no-symlink/no-hardlink append transaction.  Importing this module is inert.

import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const paths = require('../site/server/paths.js')
const fileGuards = require('../site/server/file-guards.js')
const contract = require('./task-journal-contract.cjs')

export const STEM_RE = contract.STEM_RE
export const KINDS = contract.KINDS
export const PHASES = contract.PHASES
export const STATUSES = contract.STATUSES
export const COLUMNS = contract.COLUMNS
export const META_KEYS = contract.META_KEYS
export const validateEvent = contract.validateEvent

const MAX_STDIN_BYTES = 4096

function readBoundedStdin(maxBytes = MAX_STDIN_BYTES) {
  const chunks = []
  let total = 0
  const buffer = Buffer.allocUnsafe(Math.min(1024, maxBytes + 1))
  while (total <= maxBytes) {
    const count = fs.readSync(0, buffer, 0, Math.min(buffer.length, maxBytes + 1 - total), null)
    if (count === 0) break
    chunks.push(Buffer.from(buffer.subarray(0, count)))
    total += count
  }
  if (total > maxBytes) throw Object.assign(new Error('event input exceeds its byte limit'), { code: 'EVENT_TOO_LARGE' })
  return Buffer.concat(chunks, total)
}

export function appendEvent(event, expectedStem = event && event.stem) {
  const issue = validateEvent(event, expectedStem)
  if (issue) return { version: 1, ok: false, code: 'EVENT_INVALID', message: issue }
  const bytes = Buffer.from(JSON.stringify(event) + '\n', 'utf8')
  if (bytes.length > contract.MAX_EVENT_BYTES) return { version: 1, ok: false, code: 'EVENT_TOO_LARGE', message: 'event exceeds its encoded byte limit' }
  const target = path.join(paths.JOURNAL_DIR, `${expectedStem}.jsonl`)
  let result
  try {
    result = fileGuards.appendBoundedRegularFileUnder(
      paths.PROJECT_ROOT,
      paths.JOURNAL_DIR,
      target,
      bytes,
      { create: true, directoryMode: 0o700, mode: 0o600, maxAppendBytes: contract.MAX_EVENT_BYTES, maxBytes: contract.MAX_JOURNAL_BYTES },
    )
  } catch (error) {
    return { version: 1, ok: false, code: 'JOURNAL_UNAVAILABLE', message: String(error && error.code || 'append failed').slice(0, 120) }
  }
  if (!result || result.ok !== true) {
    return { version: 1, ok: false, code: 'JOURNAL_UNAVAILABLE', message: String(result && result.code || 'append refused').slice(0, 120) }
  }
  return { version: 1, ok: true, stem: expectedStem, bytes: bytes.length }
}

function parseArgs(argv) {
  if (argv[0] !== 'append' || argv.length !== 3 || argv[1] !== '--stem' || !contract.validStem(argv[2])) {
    throw Object.assign(new Error('usage: node task-journal.mjs append --stem TASK_<N>_<slug>'), { exitCode: 2 })
  }
  return argv[2]
}

export function main(argv = process.argv.slice(2)) {
  let stem
  try { stem = parseArgs(argv) }
  catch (error) {
    process.stderr.write(String(error.message).slice(0, 300) + '\n')
    return error.exitCode || 2
  }
  let event
  try {
    const raw = readBoundedStdin()
    const text = new TextDecoder('utf-8', { fatal: true }).decode(raw)
    event = JSON.parse(text)
  } catch (error) {
    process.stdout.write(JSON.stringify({ version: 1, ok: false, code: error.code || 'EVENT_INVALID', message: 'event input is not one bounded UTF-8 JSON object' }) + '\n')
    return 2
  }
  const result = appendEvent(event, stem)
  process.stdout.write(JSON.stringify(result) + '\n')
  return result.ok ? 0 : 1
}

const invoked = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
if (invoked) process.exitCode = main()
