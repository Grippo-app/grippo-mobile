#!/usr/bin/env node

// Exact Site request-reservation helper for the private standby claim path.
// `ensure` validates claimed bytes with the same server contract and adopts or
// creates only their requestId+fingerprint receipt. `release` requires the full
// returned handle. `inspect` is strictly read-only and redacts the token unless
// an operator explicitly requests it for root-owned recovery.

import path from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const requests = require('../server/requests.js')
const paths = require('../server/paths.js')
const fileGuards = require('../server/file-guards.js')

function fail(message, code = 1) {
  process.stderr.write(String(message) + '\n')
  process.exitCode = code
}

function parse(argv) {
  const command = argv[0] || ''
  const values = Object.create(null)
  const flags = new Set()
  for (let i = 1; i < argv.length; i++) {
    const item = argv[i]
    if (!item.startsWith('--')) throw new Error('unexpected positional argument: ' + item)
    const name = item.slice(2)
    if (name === 'include-token') {
      if (flags.has(name)) throw new Error('duplicate flag --' + name)
      flags.add(name)
      continue
    }
    if (Object.hasOwn(values, name)) throw new Error('duplicate flag --' + name)
    if (i + 1 >= argv.length || argv[i + 1].startsWith('--')) throw new Error('missing value for --' + name)
    values[name] = argv[++i]
  }
  return { command, values, flags }
}

function exactKeys(parsed, allowedValues, allowedFlags = []) {
  const allowed = new Set(allowedValues)
  for (const key of Object.keys(parsed.values)) if (!allowed.has(key)) throw new Error('unsupported flag --' + key)
  const flagSet = new Set(allowedFlags)
  for (const key of parsed.flags) if (!flagSet.has(key)) throw new Error('unsupported flag --' + key)
}

function claimInsideRuns(file) {
  const runs = path.resolve(paths.RUNS_DIR)
  const resolved = path.resolve(file)
  const rel = path.relative(runs, resolved)
  if (!rel || rel === '..' || rel.startsWith('..' + path.sep) || path.isAbsolute(rel)) return null
  const base = path.basename(resolved)
  if (base !== 'request.claim' && !/^\.[0-9]+-[a-z0-9]+\.claim$/.test(base)) return null
  const runsChain = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, runs)
  const parentChain = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, path.dirname(resolved))
  if (!runsChain || !runsChain.exists || !parentChain || !parentChain.exists) return null
  return resolved
}

function print(value) { process.stdout.write(JSON.stringify(value) + '\n') }

let parsed
try { parsed = parse(process.argv.slice(2)) }
catch (error) { fail(error.message); process.exit() }

try {
  if (parsed.command === 'ensure') {
    exactKeys(parsed, ['request-id', 'claim'])
    const id = parsed.values['request-id']
    const claim = claimInsideRuns(parsed.values.claim || '')
    if (!requests.REQUEST_ID_RE.test(String(id || '')) || !claim) throw new Error('ensure requires canonical --request-id and a --claim inside RUNS_DIR')
    const record = requests.readRequestRecordFile(claim, paths.PROJECT_ROOT)
    if (!record) throw new Error('claimed request is unsafe or violates the exact v2 contract')
    const result = requests.ensureRequestReservation(id, record)
    if (!result.ok) {
      fail(result.code + (result.detail ? ': ' + result.detail : ''), 2)
    } else {
      print({ ok: true, acquired: result.acquired, handle: result.handle })
    }
  } else if (parsed.command === 'release') {
    exactKeys(parsed, ['request-id', 'stem', 'fingerprint', 'token', 'created-at'])
    const handle = {
      version: requests.REQUEST_RESERVATION_VERSION,
      requestId: parsed.values['request-id'],
      stem: parsed.values.stem,
      fingerprint: parsed.values.fingerprint,
      token: parsed.values.token,
      createdAt: parsed.values['created-at']
    }
    if (!requests.releaseRequestReservation(handle)) fail('exact request reservation release refused', 2)
    else print({ ok: true, released: true, requestId: handle.requestId, stem: handle.stem })
  } else if (parsed.command === 'inspect') {
    exactKeys(parsed, ['stem'], ['include-token'])
    const result = requests.inspectRequestReservation(parsed.values.stem)
    if (result.status === 'unsafe') {
      print(result)
      process.exitCode = 2
    } else if (result.status === 'active' && !parsed.flags.has('include-token')) {
      const publicRecord = { ...result.record }
      delete publicRecord.token
      print({ status: 'active', record: publicRecord })
    } else print(result)
  } else {
    throw new Error('usage: request-reservation.mjs ensure|release|inspect [flags]')
  }
} catch (error) {
  fail(error && error.message || error)
}
