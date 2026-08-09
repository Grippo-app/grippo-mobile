#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const root = mkdtempSync(join(tmpdir(), 'figma-screen-read-errors-'))
const cache = join(root, 'orchestrator', '.cache')
process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_CACHE_DIR = cache

const require = createRequire(import.meta.url)
const generation = require('../server/figma-generation.js')
const screens = require('../server/figma-screens.js')
const taskSource = require('../server/task-source.js')
const stem = 'TASK_1_screen_fixture'
const indexLogicalPath = `orchestrator/.cache/figma/screens/${stem}/index.json`
const indexRole = generation.surfaceIndexRole(stem)
const originalReadEntry = generation.readEntry
let checks = 0

function check(name, fn) {
  fn()
  checks++
  console.log(`ok ${checks} - ${name}`)
}

function entry(group, logicalPath, role, value) {
  return {
    group,
    logicalPath,
    role,
    bytes: value === null ? null : Buffer.from(
      typeof value === 'string' ? value : JSON.stringify(value),
      'utf8',
    ),
  }
}

function active(artifacts) {
  return { ok: true, mode: 'generation', manifest: { artifacts } }
}

function validIndex() {
  const url = 'https://www.figma.com/design/AbCdEfGh1234?node-id=1-2'
  const fetchedAt = '2026-07-24T00:00:00.000Z'
  return {
    schemaVersion: 3,
    taskStem: stem,
    nodes: {
      Home: {
        kind: 'screen',
        url,
        nodeId: '1:2',
        fetchedAt,
        variants: [{
          id: 'primary',
          theme: 'light',
          locale: 'default',
          platform: 'shared',
          url,
          nodeId: '1:2',
          fetchedAt,
          imageFile: 'Home.png',
          tokensFile: 'Home.tokens.json',
          tokensHash: 'sha256:' + 'a'.repeat(64),
          captureOperationId: 'tokop_' + 'b'.repeat(16),
          captureSequence: 1,
        }],
      },
    },
  }
}

try {
  generation.readEntry = (artifact) => artifact && artifact.bytes || null

  check('the canonical task reader rejects invalid identity and malformed UTF-8', () => {
    const directory = join(root, 'orchestrator', 'tasks', 'todo')
    mkdirSync(directory, { recursive: true })
    assert.equal(taskSource.readTask('../todo', stem), null)
    assert.equal(taskSource.readTask('todo', '../bad'), null)
    writeFileSync(join(directory, `${stem}.md`), Buffer.from([0x23, 0x20, 0xff, 0x0a]))
    assert.equal(taskSource.readTask('todo', stem), null)
    writeFileSync(join(directory, `${stem}.md`), '# Valid task\n')
    assert.equal(taskSource.readTask('todo', stem).text, '# Valid task\n')
  })

  check('an absent screen domain remains an honest empty state', () => {
    assert.deepEqual(screens.screensIndex(stem, active([])), { present: false })
  })

  check('invalid task identity is classified instead of looking empty', () => {
    assert.deepEqual(screens.screensIndex('../bad', active([])), {
      present: false,
      error: 'bad-stem',
    })
  })

  check('an invalid committed Design generation is classified', () => {
    assert.deepEqual(screens.screensIndex(stem, { ok: false, error: 'private-detail' }), {
      present: false,
      error: 'design-generation-invalid',
    })
  })

  check('malformed and wrong-version screen indexes never become empty success', () => {
    const malformed = entry('surfaces', indexLogicalPath, indexRole, '{broken')
    assert.deepEqual(screens.screensIndex(stem, active([malformed])), {
      present: false,
      error: 'screen-cache-invalid',
    })
    const wrongVersion = entry('surfaces', indexLogicalPath, indexRole, {
      ...validIndex(),
      schemaVersion: 2,
    })
    assert.deepEqual(screens.screensIndex(stem, active([wrongVersion])), {
      present: false,
      error: 'screen-cache-invalid',
    })
  })

  check('a canonical screen index remains readable', () => {
    const result = screens.screensIndex(
      stem,
      active([entry('surfaces', indexLogicalPath, indexRole, validIndex())]),
    )
    assert.equal(result.present, true)
    assert.equal(result.nodes.length, 1)
    assert.equal(result.nodes[0].screen, 'Home')
  })

  check('catalog aggregation carries corrupt screen state as a limitation', () => {
    const malformed = entry('surfaces', indexLogicalPath, indexRole, '{broken')
    const result = screens.screensAll(active([malformed]))
    assert.equal(result.present, false)
    assert.deepEqual(result.limitations, ['screen-cache-invalid'])
  })

  check('a malformed committed screen-drift report is explicit unknown freshness', () => {
    const logicalPath = `orchestrator/.cache/figma/reports/screen-drift-${stem}.json`
    const artifact = entry('drift', logicalPath, generation.surfaceDriftRole(stem), '{broken')
    assert.deepEqual(screens.screenDrift(stem, active([artifact])), {
      present: false,
      error: 'screen-drift-invalid',
      limitations: ['surface-drift-invalid'],
    })
  })

  check('post-ship drift distinguishes missing, corrupt, and valid markers', () => {
    const directory = join(root, 'orchestrator', 'tasks', 'evidence', 'figma-ship', stem)
    const file = join(directory, `drift-stale-${stem}.json`)
    assert.deepEqual(screens.shipDrift(stem), { present: false })
    mkdirSync(directory, { recursive: true })
    writeFileSync(file, '{broken')
    assert.deepEqual(screens.shipDrift(stem), {
      present: false,
      error: 'ship-drift-invalid',
    })
    writeFileSync(file, JSON.stringify({
      version: 1,
      taskStem: stem,
      staleAt: '2026-07-24T00:00:00.000Z',
      baselineRunId: 'run-1',
      driftedCount: 1,
      driftedScreens: [{ screen: 'Home', theme: 'primary', changes: ['color'] }],
    }))
    const result = screens.shipDrift(stem)
    assert.equal(result.present, true)
    assert.equal(result.driftedCount, 1)
    assert.equal(result.driftedScreens[0].screen, 'Home')
  })

  console.log(`figma-screen-read-errors: ${checks} checks passed`)
} finally {
  generation.readEntry = originalReadEntry
  rmSync(root, { recursive: true, force: true })
}
