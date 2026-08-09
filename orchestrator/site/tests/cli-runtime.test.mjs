#!/usr/bin/env node

import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import { lstatSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const root = mkdtempSync(join(tmpdir(), 'cli-runtime-'))
const runs = join(root, 'orchestrator', '.cache', 'tasks', 'runs')
mkdirSync(runs, { recursive: true })
process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_RUNS_DIR = runs

const childProcess = require('node:child_process')
const originalSpawn = childProcess.spawn
const spawned = []

function fakeChild(command, args, options) {
  const child = new EventEmitter()
  child.command = command
  child.args = args
  child.options = options
  child.stdout = new EventEmitter()
  child.stderr = new EventEmitter()
  child.stdin = new EventEmitter()
  child.stdin.writable = true
  child.stdinWrites = []
  child.stdin.write = (value) => { child.stdinWrites.push(value); return true }
  child.kill = () => true
  spawned.push(child)
  if (command === 'claude' && args[0] === '--version') {
    queueMicrotask(() => child.emit('close', 1))
  }
  return child
}

childProcess.spawn = fakeChild
let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }

try {
  const cli = require('../server/cli.js')

  check('safe runtime directory admits install and publishes a private regular log', () => {
    const job = cli.install()
    assert.equal(job.running, true, job.error)
    const child = spawned.find((item) => item.command === 'npm')
    assert.ok(child)
    assert.deepEqual(child.options.stdio, ['ignore', 'pipe', 'pipe'])
    child.stdout.emit('data', Buffer.from('install output\n'))
    child.emit('exit', 0)
    const log = join(runs, '.cli-install.log')
    assert.equal(lstatSync(log).isFile(), true)
    if (process.platform !== 'win32') assert.equal(lstatSync(log).mode & 0o777, 0o600)
    assert.equal(cli.readJobLog('install'), 'install output\n')
    assert.equal(cli.jobs().install.exitCode, 0)
  })

  check('interactive login log stays descriptor-backed and relays one bounded code', () => {
    const status = cli.login()
    assert.equal(status.running, true, status.error)
    const child = spawned.filter((item) => item.command === 'claude' && item.args[0] === 'auth').at(-1)
    assert.ok(child)
    child.stdout.emit('data', Buffer.from('Open https://example.test/oauth/authorize?x=1\n'))
    assert.equal(cli.jobs().login.url, 'https://example.test/oauth/authorize?x=1')
    assert.equal(cli.loginSubmitCode('valid-code'), true)
    assert.deepEqual(child.stdinWrites, ['valid-code\n'])
    child.emit('exit', 0)
    assert.match(cli.readJobLog('login'), /oauth\/authorize/)
    assert.equal(cli.jobs().login, null)
  })

  check('job-log projection reads only the bounded 64 KiB tail', () => {
    const prefix = 'prefix-that-must-drop\n'
    const tail = 'z'.repeat(64 * 1024)
    writeFileSync(join(runs, '.cli-install.log'), prefix + tail)
    assert.equal(cli.readJobLog('install'), tail)
  })

  console.log(`cli-runtime: ${checks} checks passed`)
} finally {
  childProcess.spawn = originalSpawn
  rmSync(root, { recursive: true, force: true })
}
