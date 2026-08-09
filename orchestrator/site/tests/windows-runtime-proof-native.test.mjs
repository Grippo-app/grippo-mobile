#!/usr/bin/env node

import assert from 'node:assert/strict'
import childProcess from 'node:child_process'
import { createRequire } from 'node:module'
import { mkdirSync, mkdtempSync, rmSync, statSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

if (process.platform !== 'win32') {
  // This host cannot exercise Win32 process/DACL syscalls, but the test still
  // owns a real platform boundary: importing the production adapter off
  // Windows must remain inert and every native authority query must fail
  // closed.  The adjacent platform-neutral suite exercises the full Win32
  // helper protocol through its injected native seam.
  const offWindowsRequire = createRequire(import.meta.url)
  const runtime = offWindowsRequire('../server/windows-runtime-proof.js')
  assert.deepEqual(runtime.inspectProcess(process.pid), { status: 'unsupported', processStartId: null })
  assert.throws(
    () => runtime.captureProcessStartId(process.pid),
    (error) => error && error.code === 'WINDOWS_PROCESS_IDENTITY_UNAVAILABLE',
  )
  assert.equal(runtime.processIdentityState(process.pid, null), 'unsupported')
  assert.equal(runtime.processIdentityMatches(process.pid, null), false)
  assert.deepEqual(
    runtime.processTreeProof(process.pid, null, process.pid, null),
    { ok: false, reason: 'exact Windows ancestry identity is missing' },
  )
  assert.equal(runtime.privatePathState(import.meta.filename, { dev: '1', ino: '1', type: 'file' }), 'unknown')
  assert.equal(runtime.hardenPrivatePath(import.meta.filename, { dev: '1', ino: '1', type: 'file' }), 'unknown')
  assert.deepEqual(runtime.recoverStaleOwner({}), { code: 'invalid-request', removed: false })
  process.stdout.write('ok 1 - non-Windows host fails closed at every native Windows authority boundary\n')
  process.exit(0)
}

const require = createRequire(import.meta.url)
const runtime = require('../server/windows-runtime-proof.js')
const base = mkdtempSync(join(tmpdir(), 'windows-runtime-proof-native-'))
const directory = join(base, 'private')
const file = join(directory, 'record.json')

function proof(target, type) {
  const value = statSync(target, { bigint: true })
  return { dev: String(value.dev), ino: String(value.ino), type }
}

try {
  mkdirSync(directory)
  writeFileSync(file, '{}\n')

  const processStartId = runtime.captureProcessStartId(process.pid)
  assert.match(processStartId, /^psid-v1:win32:[a-f0-9]{64}$/)
  assert.equal(runtime.processIdentityState(process.pid, processStartId), 'match')

  const child = childProcess.spawn(process.execPath, ['-e', 'setTimeout(() => {}, 30000)'], {
    stdio: 'ignore', windowsHide: true,
  })
  try {
    const childStartId = runtime.captureProcessStartId(child.pid)
    const ancestry = runtime.processTreeProof(child.pid, childStartId, process.pid, processStartId)
    assert.equal(ancestry.ok, true)
    assert.equal(ancestry.depth, 1)
  } finally {
    child.kill()
  }

  assert.equal(runtime.hardenPrivatePath(directory, proof(directory, 'directory')), 'private')
  assert.equal(runtime.privatePathState(directory, proof(directory, 'directory')), 'private')
  assert.equal(runtime.hardenPrivatePath(file, proof(file, 'file')), 'private')
  assert.equal(runtime.privatePathState(file, proof(file, 'file')), 'private')

  const systemRoot = process.env.SystemRoot || process.env.WINDIR
  assert.ok(systemRoot, 'Windows system root is required')
  const icacls = join(systemRoot, 'System32', 'icacls.exe')
  const loosened = childProcess.spawnSync(icacls, [file, '/grant', '*S-1-1-0:(R)'], {
    encoding: 'utf8', windowsHide: true,
  })
  assert.equal(loosened.status, 0, String(loosened.stderr || loosened.stdout || 'icacls failed'))
  assert.equal(runtime.privatePathState(file, proof(file, 'file')), 'unsafe')
  assert.equal(runtime.hardenPrivatePath(file, proof(file, 'file')), 'private')

  process.stdout.write('ok 1 - native Windows process identity, ancestry, and protected DACL proofs\n')
} finally {
  rmSync(base, { recursive: true, force: true })
}
