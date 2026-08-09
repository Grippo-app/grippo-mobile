#!/usr/bin/env node

import assert from 'node:assert/strict'
import { createServer } from 'node:http'
import { chmodSync, existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, statSync, unlinkSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import { createRequire } from 'node:module'

const root = mkdtempSync(join(tmpdir(), 'backlog-http-'))
const scratch = mkdtempSync(join(tmpdir(), 'backlog-http-scratch-'))
const tasks = join(root, 'tasks')
const cache = join(root, 'cache')
for (const dir of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasks, dir), { recursive: true })
for (const dir of ['creations', 'edits', 'inbox', 'intake', 'locks', 'requests', 'runs', 'superseded', 'finalizations']) mkdirSync(join(cache, dir), { recursive: true })
if (process.platform !== 'win32') chmodSync(join(cache, 'intake'), 0o700)
writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify({ version: 2, generatedAt: '1970-01-01T00:00:00Z', backlog: [], pending: [], todo: [], done: [] }, null, 2) + '\n')

const createScript = join(root, 'fake-create.py')
writeFileSync(createScript, `import datetime,hashlib,json,os,pathlib,sys
print('READY', flush=True)
p=json.load(sys.stdin); tasks=pathlib.Path(os.environ['CREATE_BACKLOG_TASKS_DIR'])
stem='TASK_1_http_created'; source=p['source']; source_block='## Source\\n\\n- Kind: '+source['kind']+'\\n- Type: '+source['type']+'\\n- Ref: '+source['ref']+'\\n- Fingerprint: '+source['fingerprint']; data=('# TASK 1 — '+p['title']+'\\n\\n'+source_block+'\\n\\n'+p['body'].rstrip('\\n')+'\\n').encode()
(tasks/'backlog'/f'{stem}.md').write_bytes(data); task_file=tasks/'backlog'/f'{stem}.md'
created_at=datetime.datetime.fromtimestamp(task_file.stat().st_mtime,datetime.timezone.utc).replace(microsecond=0).isoformat().replace('+00:00','Z')
content_hash='sha256:'+hashlib.sha256(data).hexdigest(); rows='[["backlog","tasks/backlog/'+stem+'.md","'+content_hash+'"]]'; revision='sha256:'+hashlib.sha256(('task-state-revision-v1\\x00backlog\\x00'+rows).encode()).hexdigest()
idx=json.loads((tasks/'INDEX.json').read_text()); idx['backlog']=[{'stem':stem,'title':p['title'],'state':'backlog','sourceRevision':revision,'createdAt':created_at,'doneAt':None,'origin':source,'dependsOn':[],'splitFrom':None,'outcomeStatus':None,'questionsCount':None,'round':None}]
(tasks/'INDEX.json').write_text(json.dumps(idx,indent=2)+'\\n')
print(json.dumps({'ok':True,'created':True,'deduped':False,'effect':'created','stem':stem,'number':1,'column':'backlog','task':idx['backlog'][0],'sourceHash':'sha256:'+hashlib.sha256(data).hexdigest(),'transactionId':'0'*32,'replayed':False}), flush=True)
`)
const editScript = join(root, 'fake-edit.py')
writeFileSync(editScript, `import datetime,hashlib,json,os,pathlib
print('READY', flush=True)
p=json.load(__import__('sys').stdin); f=pathlib.Path(os.environ['CREATE_BACKLOG_TASKS_DIR'])/'backlog'/(p['stem']+'.md')
old=f.read_bytes(); data=p['markdown'].replace('\\r\\n','\\n').replace('\\r','\\n').encode(); data += b'' if data.endswith(b'\\n') else b'\\n'; f.write_bytes(data)
idx=json.loads((pathlib.Path(os.environ['CREATE_BACKLOG_TASKS_DIR'])/'INDEX.json').read_text()); task=next(x for x in idx['backlog'] if x['stem']==p['stem']); task['createdAt']=datetime.datetime.fromtimestamp(f.stat().st_mtime,datetime.timezone.utc).replace(microsecond=0).isoformat().replace('+00:00','Z'); content_hash='sha256:'+hashlib.sha256(data).hexdigest(); rows='[["backlog","tasks/backlog/'+p['stem']+'.md","'+content_hash+'"]]'; task['sourceRevision']='sha256:'+hashlib.sha256(('task-state-revision-v1\\x00backlog\\x00'+rows).encode()).hexdigest(); task['title']=data.decode().splitlines()[0].split(' — ',1)[1]; (pathlib.Path(os.environ['CREATE_BACKLOG_TASKS_DIR'])/'INDEX.json').write_text(json.dumps(idx,indent=2)+'\\n')
print(json.dumps({'ok':True,'changed':old!=data,'stem':p['stem'],'column':'backlog','previousSourceHash':p['expectedSourceHash'],'sourceHash':'sha256:'+hashlib.sha256(data).hexdigest(),'transactionId':'1'*32,'recovered':False,'task':task}), flush=True)
`)
const earlyScript = join(root, 'fake-early.py')
writeFileSync(earlyScript, `print('READY', flush=True)\n`)
const invalidScript = join(root, 'fake-invalid.py')
writeFileSync(invalidScript, `import json,sys\nprint('READY',flush=True)\njson.load(sys.stdin)\nprint(json.dumps({'ok':True,'stem':'TASK_2_invalid'}),flush=True)\n`)
const fakeClaude = join(root, 'slow-claude.mjs')
const slowModelStarted = join(root, 'slow-model-started')
const slowModelCompleted = join(root, 'slow-model-completed')
const slowModelRelease = join(root, 'slow-model-release')
writeFileSync(fakeClaude, `#!/usr/bin/env node
import { existsSync, writeFileSync } from 'node:fs'
writeFileSync(${JSON.stringify(slowModelStarted)}, 'started\\n')
process.stdin.resume()
const deadline = Date.now() + 30000
function waitForRelease() {
  if (existsSync(${JSON.stringify(slowModelRelease)})) {
    writeFileSync(${JSON.stringify(slowModelCompleted)}, 'completed\\n')
    process.stdout.write(JSON.stringify({type:'result',subtype:'success',structured_output:{readiness:'ready',summary:'HTTP preview.',likelyAreas:['unknown'],possibleDuplicates:[],missingContext:[],riskFlags:[]}})+'\\n')
    return
  }
  if (Date.now() >= deadline) process.exit(70)
  setTimeout(waitForRelease, 20)
}
waitForRelease()
`)
chmodSync(fakeClaude, 0o755)

process.env.ORCHESTRATOR_PROJECT_ROOT = root
process.env.ORCHESTRATOR_TASKS_DIR = tasks
process.env.ORCHESTRATOR_TASK_CREATIONS_DIR = join(cache, 'creations')
process.env.ORCHESTRATOR_TASK_EDITS_DIR = join(cache, 'edits')
process.env.ORCHESTRATOR_TASK_INBOX_DIR = join(cache, 'inbox')
process.env.ORCHESTRATOR_TASK_INBOX_AUTHORITY_ROOT = root
process.env.ORCHESTRATOR_TASK_INTAKE_DIR = join(cache, 'intake')
process.env.ORCHESTRATOR_LOCKS_DIR = join(cache, 'locks')
process.env.ORCHESTRATOR_REQUESTS_DIR = join(cache, 'requests')
process.env.ORCHESTRATOR_RUNS_DIR = join(cache, 'runs')
process.env.ORCHESTRATOR_SUPERSEDED_DIR = join(cache, 'superseded')
process.env.ORCHESTRATOR_FINALIZATIONS_DIR = join(cache, 'finalizations')
process.env.ORCHESTRATOR_STATE_FILE = join(cache, 'site', 'state.json')
process.env.CREATE_BACKLOG_SCRIPT = createScript
process.env.EDIT_BACKLOG_SCRIPT = editScript
process.env.SHALLOW_INTAKE_CLAUDE = fakeClaude
// Both model-side and controller-side deadlines exceed the HTTP watchdog, so
// a broken implementation which waits for model termination cannot pass when
// the blocked model times out on its own.
process.env.SHALLOW_INTAKE_TIMEOUT_MS = '30000'
process.env.SHALLOW_INTAKE_SCRATCH_DIR = scratch
process.env.RUNNER_DISABLED = '1'

const require = createRequire(import.meta.url)
const httpMod = require('../server/http.js')
const shallow = require('../server/shallow-intake.js')
// server.js initializes the advisory controller as startup work independent
// of any backlog request. Complete that same phase explicitly in this focused
// HTTP fixture instead of charging controller initialization to one request.
shallow.init()
if (process.platform !== 'win32') {
  assert.equal(statSync(join(cache, 'intake')).mode & 0o777, 0o700)
  assert.equal(statSync(join(cache, 'intake', '.stem-locks')).mode & 0o777, 0o700)
}
const server = createServer(httpMod.handle)
await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
const base = `http://127.0.0.1:${server.address().port}`
let checks = 0
async function check(name, fn) { await fn(); checks++; console.log(`ok ${checks} - ${name}`) }
function headers(csrf) { return { 'content-type': 'application/json', 'x-orchestrator-csrf': csrf, origin: base } }
async function waitForFile(file, timeoutMs = 5000) {
  const deadline = Date.now() + timeoutMs
  while (!existsSync(file) && Date.now() < deadline) await new Promise((resolve) => setTimeout(resolve, 20))
  assert.equal(existsSync(file), true, `timed out waiting for ${file}`)
}
async function waitForIntakeStatus(stem, status, timeoutMs = 5000) {
  const deadline = Date.now() + timeoutMs
  let current = null
  while (Date.now() < deadline) {
    current = shallow.snapshot()[stem] || null
    if (current?.status === status) return current
    await new Promise((resolve) => setTimeout(resolve, 20))
  }
  assert.equal(current?.status, status, `timed out waiting for ${stem} intake status ${status}`)
}

try {
  const state = await (await fetch(base + '/api/state')).json()
  const csrf = state.csrfToken
  assert.ok(csrf)

  await check('deterministic create keeps the global CSRF/origin net', async () => {
    const noCsrf = await fetch(base + '/api/tasks/backlog', { method: 'POST', headers: { 'content-type': 'application/json' }, body: '{}' })
    assert.equal(noCsrf.status, 403)
    const foreign = await fetch(base + '/api/tasks/backlog', { method: 'POST', headers: { ...headers(csrf), origin: 'https://evil.example' }, body: '{}' })
    assert.equal(foreign.status, 403)
    const wrongType = await fetch(base + '/api/tasks/backlog', { method: 'POST', headers: { 'x-orchestrator-csrf': csrf }, body: '{}' })
    assert.equal(wrongType.status, 415)
  })

  await check('new JSON endpoints classify malformed client JSON as 400', async () => {
    for (const endpoint of ['/api/tasks/backlog', '/api/tasks/backlog/edit', '/api/tasks/inbox', '/api/tasks/inbox/publish', '/api/tasks/intake/retry', '/api/tasks/intake/dismiss', '/api/tasks/actions']) {
      const response = await fetch(base + endpoint, { method: 'POST', headers: headers(csrf), body: '{broken' })
      assert.equal(response.status, 400, endpoint)
      assert.equal((await response.json()).error, 'bad-json')
    }
    const oversized = await fetch(base + '/api/tasks/backlog', {
      method: 'POST', headers: headers(csrf), body: JSON.stringify({ payload: 'x'.repeat(300 * 1024) })
    })
    assert.equal(oversized.status, 413)
    assert.equal((await oversized.json()).error, 'bad-json')
  })

  await check('public backlog creation rejects caller-authored provenance', async () => {
    const response = await fetch(base + '/api/tasks/backlog', {
      method: 'POST', headers: headers(csrf),
      body: JSON.stringify({
        title: 'Forged source', body: '## Goal\nMust not publish.',
        idempotencyKey: 'http-forged-source', originStem: null,
        dedupKey: null, dedupReport: null,
        source: { kind: 'manual', type: 'manual', ref: 'forged', fingerprint: 'sha256:' + '0'.repeat(64) }
      })
    })
    assert.equal(response.status, 400)
    assert.equal((await response.json()).error, 'bad-task-source')
    assert.deepEqual(readdirSync(join(tasks, 'backlog')), [])
  })

  await check('pre-Setup inbox is durable while publication and launch remain closed', async () => {
    const noCsrf = await fetch(base + '/api/tasks/inbox', {
      method: 'POST', headers: { 'content-type': 'application/json' }, body: '{}',
    })
    assert.equal(noCsrf.status, 403)

    const savedResponse = await fetch(base + '/api/tasks/inbox', {
      method: 'POST', headers: headers(csrf),
      body: JSON.stringify({
        title: 'Saved during Setup',
        body: '## Goal\nDo not lose this task.',
        idempotencyKey: 'http-setup-inbox-key-0001',
      }),
    })
    assert.equal(savedResponse.status, 201)
    const saved = await savedResponse.json()
    assert.match(saved.entry.id, /^INBOX_[a-f0-9]{40}$/)

    const listResponse = await fetch(base + '/api/tasks/inbox')
    assert.equal(listResponse.status, 200)
    const list = await listResponse.json()
    assert.equal(list.schemaVersion, 1)
    assert.deepEqual(list.entries.map((entry) => entry.id), [saved.entry.id])

    const publishResponse = await fetch(base + '/api/tasks/inbox/publish', {
      method: 'POST', headers: headers(csrf), body: JSON.stringify({ id: saved.entry.id }),
    })
    assert.equal(publishResponse.status, 409)
    assert.equal((await publishResponse.json()).error, 'setup-incomplete')
    assert.deepEqual(readdirSync(join(tasks, 'backlog')), [])
  })

  await check('prompt-bearing request queue routes are not public HTTP APIs', async () => {
    for (const pathname of ['/api/requests', '/api/requests/clear']) {
      const response = await fetch(base + pathname, {
        method: 'POST', headers: headers(csrf),
        body: JSON.stringify({ action: 'prep', stem: 'TASK_1_forbidden', prompt: 'forbidden' }),
      })
      assert.equal(response.status, 404, pathname)
    }
    assert.deepEqual(readdirSync(join(cache, 'requests')).filter((name) => name.endsWith('.json')), [])
  })

  let created
  await check('HTTP creation structurally completes before the slow advisory model and exposes queued/checking state', async () => {
    const response = await fetch(base + '/api/tasks/backlog', {
      method: 'POST', headers: headers(csrf),
      body: JSON.stringify({ title: 'HTTP created', body: '## Goal\nCreated without waiting for AI.', idempotencyKey: 'http-create-key-0001', originStem: null, dedupKey: null, dedupReport: null }),
      // A generous harness watchdog catches a wedged HTTP path. Whether the
      // response waited for AI is proved independently by the completion
      // marker below, not by machine-dependent millisecond timing.
      signal: AbortSignal.timeout(15000)
    })
    assert.equal(response.status, 201)
    created = await response.json()
    assert.equal(created.stem, 'TASK_1_http_created')
    assert.ok(['queued', 'checking'].includes(created.intake.status))
    assert.equal(existsSync(slowModelCompleted), false,
      'the HTTP response must be complete while the advisory model release gate is closed')
    await waitForFile(slowModelStarted)
    assert.equal(existsSync(slowModelCompleted), false,
      'the exercised advisory model must remain blocked after the HTTP response')
    writeFileSync(slowModelRelease, 'release\n')
    await waitForFile(slowModelCompleted)
    await waitForIntakeStatus(created.stem, 'complete')
    unlinkSync(slowModelRelease)
    assert.match(readFileSync(join(tasks, 'backlog', created.stem + '.md'), 'utf8'), /Created without waiting for AI/)
  })

  let source
  await check('backlog source GET returns exact markdown plus compare-and-swap hash', async () => {
    const response = await fetch(base + '/api/tasks/backlog?stem=' + created.stem)
    assert.equal(response.status, 200)
    source = await response.json()
    assert.equal(source.sourceHash, created.sourceHash)
    assert.match(source.markdown, /^# TASK 1/)
  })

  await check('bounded task-file GET serves the project-root body and rejects invalid paths', async () => {
    const response = await fetch(base + '/api/tasks/file?column=backlog&stem=' + encodeURIComponent(created.stem))
    assert.equal(response.status, 200)
    assert.match(response.headers.get('content-type') || '', /^text\/markdown; charset=utf-8$/)
    assert.equal(response.headers.get('cache-control'), 'no-store')
    assert.equal(response.headers.get('x-content-type-options'), 'nosniff')
    assert.equal(await response.text(), source.markdown)

    const invalidColumn = await fetch(base + '/api/tasks/file?column=unknown&stem=' + encodeURIComponent(created.stem))
    assert.equal(invalidColumn.status, 400)
    assert.equal((await invalidColumn.json()).error, 'task-file-column-invalid')

    const traversal = await fetch(base + '/api/tasks/file?column=backlog&stem=' + encodeURIComponent('../escape'))
    assert.equal(traversal.status, 400)
    assert.equal((await traversal.json()).error, 'task-file-stem-invalid')

    const missing = await fetch(base + '/api/tasks/file?column=done&stem=' + encodeURIComponent(created.stem))
    assert.equal(missing.status, 404)
    assert.equal((await missing.json()).error, 'task-file-not-found')
  })

  await check('task summary endpoints expose the current typed read model', async () => {
    const listResponse = await fetch(base + '/api/tasks/summary?limit=1&sort=number')
    assert.equal(listResponse.status, 200)
    const list = await listResponse.json()
    assert.equal(list.schemaVersion, 1)
    assert.equal(list.indexSchemaVersion, 2)
    assert.equal(list.total, 1)
    assert.equal(list.columns.backlog[0].stem, created.stem)
    assert.equal(list.columns.backlog[0].origin.kind, 'manual')
    assert.equal(Object.hasOwn(list.columns.backlog[0].origin, 'inferredLegacy'), false)

    const invalidFilter = await fetch(base + '/api/tasks/summary?dependency=unknown')
    assert.equal(invalidFilter.status, 400)
    assert.equal((await invalidFilter.json()).error, 'bad-task-summary-filter')

    const itemResponse = await fetch(base + '/api/tasks/' + created.stem + '/summary')
    assert.equal(itemResponse.status, 200)
    const item = await itemResponse.json()
    assert.equal(item.schemaVersion, 1)
    assert.equal(item.task.stem, created.stem)
    assert.match(item.task.primaryAction.actionRevision, /^sha256:[a-f0-9]{64}$/)

    const detailsResponse = await fetch(base + '/api/tasks/' + created.stem + '/details')
    assert.equal(detailsResponse.status, 200)
    const details = await detailsResponse.json()
    assert.equal(details.schemaVersion, 1)
    assert.equal(details.identity.stem, created.stem)
    assert.equal(details.state.column, 'backlog')
    assert.equal(details.primaryAction.kind, item.task.primaryAction.kind)
    assert.deepEqual(Object.keys(details).sort(), [
      'activitySummary', 'advancedAvailable', 'appValidation', 'artifactSummary',
      'blockers', 'currentWork', 'dependencies', 'designIssues', 'identity',
      'lastActivity', 'limitations', 'origin', 'outcome', 'partial',
      'primaryAction', 'recovery', 'requirement', 'retryRecovery', 'revision',
      'schemaVersion', 'secondaryActions', 'state',
    ])
    assert.equal((await fetch(base + '/api/tasks/' + created.stem +
      '/details?unexpected=true')).status, 400)
    assert.equal((await fetch(base + '/api/tasks/' + created.stem +
      '/activity?limit=201')).status, 400)
    assert.equal((await fetch(base + '/api/tasks/' + created.stem +
      '/advanced?sections=raw,raw')).status, 400)
  })

  await check('typed task action endpoint rejects malformed and stale client intents without queueing', async () => {
    const malformed = await fetch(base + '/api/tasks/actions', {
      method: 'POST', headers: headers(csrf), body: JSON.stringify({ stem: created.stem, prompt: 'client controlled' })
    })
    assert.equal(malformed.status, 400)
    assert.equal((await malformed.json()).error, 'bad-task-action-request')

    const summary = await (await fetch(base + '/api/tasks/' + created.stem + '/summary')).json()
    const action = summary.task.primaryAction
    const legacy = await fetch(base + '/api/tasks/actions', {
      method: 'POST', headers: headers(csrf),
      body: JSON.stringify({
        stem: created.stem,
        actionId: 'act-' + '0'.repeat(24),
        actionRevision: action.actionRevision,
        kind: action.kind,
        expectedState: action.expectedState,
        expectedSourceRevision: action.expectedSourceRevision,
        checkpointId: action.checkpointId || null,
        confirmation: null
      })
    })
    assert.equal(legacy.status, 400)
    assert.equal((await legacy.json()).error, 'bad-task-action-request')
    const stale = await fetch(base + '/api/tasks/actions', {
      method: 'POST', headers: headers(csrf),
      body: JSON.stringify({
        stem: created.stem,
        actionId: 'act-' + '0'.repeat(24),
        actionRevision: action.actionRevision,
        action: 'prepare',
        expectedState: action.expectedState,
        expectedSourceRevision: action.expectedSourceRevision,
        checkpointId: action.checkpointId || null,
        confirmation: null,
        confirmationToken: null,
        answers: null,
        questionRound: null,
        expectedQuestionsRevision: null,
        liveSessionId: null,
        expectedSessionRevision: null,
        idempotencyKey: 'task-details-http-stale-0001'
      })
    })
    assert.equal(stale.status, 409)
    assert.equal((await stale.json()).error, 'action-stale')
    assert.deepEqual(readdirSync(join(cache, 'requests')).filter((name) => name.endsWith('.json')), [])
  })

  await check('edit is CSRF-protected, source-bound, and schedules a new advisory generation', async () => {
    const stale = await fetch(base + '/api/tasks/backlog/edit', { method: 'POST', headers: headers(csrf), body: JSON.stringify({ stem: created.stem, expectedSourceHash: 'sha256:' + '0'.repeat(64), markdown: source.markdown }) })
    assert.equal(stale.status, 409)
    const markdown = source.markdown.replace('Created without waiting for AI.', 'Edited without waiting for AI.')
    const edited = await fetch(base + '/api/tasks/backlog/edit', { method: 'POST', headers: headers(csrf), body: JSON.stringify({ stem: created.stem, expectedSourceHash: source.sourceHash, markdown }) })
    assert.equal(edited.status, 200)
    const result = await edited.json()
    assert.equal(result.changed, true)
    assert.notEqual(result.sourceHash, source.sourceHash)
    assert.ok(['queued', 'checking'].includes(result.intake.status))
  })

  await check('retry/dismiss require the current source hash and remain advisory', async () => {
    const current = await (await fetch(base + '/api/tasks/backlog?stem=' + created.stem)).json()
    const staleRetry = await fetch(base + '/api/tasks/intake/retry', { method: 'POST', headers: headers(csrf), body: JSON.stringify({ stem: created.stem, expectedSourceHash: source.sourceHash }) })
    assert.equal(staleRetry.status, 409)
    const dismissed = await fetch(base + '/api/tasks/intake/dismiss', { method: 'POST', headers: headers(csrf), body: JSON.stringify({ stem: created.stem, expectedSourceHash: current.sourceHash }) })
    assert.equal(dismissed.status, 200)
    const after = await (await fetch(base + '/api/state')).json()
    assert.equal(after.progress.shallowIntake[created.stem], undefined)
    assert.ok(readFileSync(join(tasks, 'backlog', created.stem + '.md'), 'utf8').includes('Edited without waiting for AI.'))
  })

  await check('queued task-prep keeps advisory preview until actual runner execution', async () => {
    const setup = await fetch(base + '/api/state-patch', {
      method: 'POST',
      headers: headers(csrf),
      body: JSON.stringify({
        manualSteps: {
          'setup:requirementsVerified': true,
          'setup:yamlPasted': true,
          'setup:agentsInstalled': true,
        },
      }),
    })
    assert.equal(setup.status, 200)
    assert.equal((await setup.json()).progress.setupDone, true)
    const current = await (await fetch(base + '/api/tasks/backlog?stem=' + created.stem)).json()
    await fetch(base + '/api/tasks/intake/retry', { method: 'POST', headers: headers(csrf), body: JSON.stringify({ stem: created.stem, expectedSourceHash: current.sourceHash }) })
    const summary = await (await fetch(base + '/api/tasks/' + created.stem + '/summary')).json()
    const action = summary.task.primaryAction
    const request = {
      stem: created.stem,
      actionId: action.id,
      actionRevision: action.actionRevision,
      action: action.kind,
      expectedState: action.expectedState,
      expectedSourceRevision: action.expectedSourceRevision,
      checkpointId: null,
      confirmation: null,
      confirmationToken: null,
      answers: null,
      questionRound: null,
      expectedQuestionsRevision: null,
      liveSessionId: null,
      expectedSessionRevision: null,
      idempotencyKey: 'task-details-http-prep-0001',
    }
    const prep = await fetch(base + '/api/tasks/actions', {
      method: 'POST', headers: headers(csrf), body: JSON.stringify(request),
    })
    const accepted = await prep.json()
    assert.equal(prep.status, 200, JSON.stringify({ action, accepted }))
    assert.deepEqual(Object.keys(accepted).sort(), [
      'action', 'idempotentReplay', 'requestId', 'resultingActionRevision',
      'schemaVersion', 'sessionId', 'status', 'taskSummaryRevision',
    ])
    assert.equal(accepted.action, 'prepare')
    assert.ok(['accepted', 'already-active'].includes(accepted.status))
    assert.equal(accepted.idempotentReplay, false)
    assert.equal(Object.hasOwn(accepted, 'prompt'), false)
    const replay = await fetch(base + '/api/tasks/actions', {
      method: 'POST', headers: headers(csrf), body: JSON.stringify(request),
    })
    assert.equal(replay.status, 200)
    assert.equal((await replay.json()).idempotentReplay, true)
    const after = await (await fetch(base + '/api/state')).json()
    assert.ok(after.progress.shallowIntake[created.stem])
    assert.ok(['queued', 'checking'].includes(after.progress.shallowIntake[created.stem].status))
  })

  await check('spawn/stdio failures and malformed helper success settle fail-closed without retaining writer leases', async () => {
    const writerLeases = require('../../tasks/writer-leases.cjs')
    const finalizations = require('../server/finalizations.js')
    const writers = join(cache, 'finalizations', '.writers')
    const originalPython = process.env.CREATE_BACKLOG_PYTHON
    const originalScript = process.env.CREATE_BACKLOG_SCRIPT
    const originalAttach = finalizations.attachMutationChild
    let attachCalls = 0
    try {
      finalizations.attachMutationChild = function (...args) {
        attachCalls++
        return originalAttach.apply(finalizations, args)
      }
      process.env.CREATE_BACKLOG_PYTHON = join(root, 'missing-python')
      let response = await fetch(base + '/api/tasks/backlog', { method: 'POST', headers: headers(csrf), body: JSON.stringify({ title: 'Missing runtime', body: '', idempotencyKey: 'http-create-key-missing-runtime', originStem: null, dedupKey: null, dedupReport: null }) })
      assert.equal(response.status, 500)
      assert.equal((await response.json()).error, 'create-runtime-unavailable')
      assert.equal(attachCalls, 0, 'a spawn with no PID must never enter child attachment')
      assert.equal(writerLeases.scan(writers).active.length, 0)

      process.env.CREATE_BACKLOG_PYTHON = 'python3'
      process.env.CREATE_BACKLOG_SCRIPT = earlyScript
      response = await fetch(base + '/api/tasks/backlog', { method: 'POST', headers: headers(csrf), body: JSON.stringify({ title: 'Early close', body: 'x'.repeat(64 * 1024), idempotencyKey: 'http-create-key-early-close', originStem: null, dedupKey: null, dedupReport: null }) })
      const earlyBody = await response.json()
      assert.ok([500, 503].includes(response.status), JSON.stringify(earlyBody))
      if (response.status === 503) assert.equal(earlyBody.error, 'writer-lease-update-failed')
      assert.equal(writerLeases.scan(writers).active.length, 0)

      process.env.CREATE_BACKLOG_SCRIPT = invalidScript
      response = await fetch(base + '/api/tasks/backlog', { method: 'POST', headers: headers(csrf), body: JSON.stringify({ title: 'Invalid protocol', body: '', idempotencyKey: 'http-create-key-invalid-protocol', originStem: null, dedupKey: null, dedupReport: null }) })
      assert.equal(response.status, 500)
      assert.equal((await response.json()).error, 'create-protocol-invalid')
      assert.equal(writerLeases.scan(writers).active.length, 0)

      const beforeForcedAttach = attachCalls
      finalizations.attachMutationChild = function () {
        attachCalls++
        return { ok: false, error: 'writer-lease-update-failed', detail: 'forced attach failure fixture' }
      }
      process.env.CREATE_BACKLOG_SCRIPT = earlyScript
      response = await fetch(base + '/api/tasks/backlog', { method: 'POST', headers: headers(csrf), body: JSON.stringify({ title: 'Attach failure', body: '', idempotencyKey: 'http-create-key-attach-failure', originStem: null, dedupKey: null, dedupReport: null }) })
      assert.equal(response.status, 503)
      assert.equal((await response.json()).error, 'writer-lease-update-failed')
      assert.equal(attachCalls, beforeForcedAttach + 1)
      await new Promise((resolve) => setTimeout(resolve, 50))
      assert.equal(writerLeases.scan(writers).active.length, 0)
      assert.equal((await fetch(base + '/api/state')).status, 200, 'server remains single-settled after child close/error')
    } finally {
      finalizations.attachMutationChild = originalAttach
      if (originalPython === undefined) delete process.env.CREATE_BACKLOG_PYTHON; else process.env.CREATE_BACKLOG_PYTHON = originalPython
      process.env.CREATE_BACKLOG_SCRIPT = originalScript
    }
  })

  await check('unsafe edit recovery state is visible in sanitized board state', async () => {
    const marker = join(cache, 'edits', 'TASK_99_corrupt.json')
    writeFileSync(marker, '{broken\n')
    try {
      const response = await fetch(base + '/api/state')
      assert.equal(response.status, 200)
      const raw = await response.text()
      const snapshot = JSON.parse(raw)
      assert.ok(snapshot.progress.publicationRecoveryIssues.some((issue) => issue.kind === 'edit' && issue.code === 'EDIT_MARKER_INVALID'))
      assert.doesNotMatch(raw, /recoveryMarkdownBase64/)
    } finally { unlinkSync(marker) }
  })

  console.log(`backlog-http: ${checks} checks passed`)
} finally {
  // Release any model still waiting because an assertion/HTTP watchdog failed,
  // then terminate the controller's exact owned process generations.
  try { writeFileSync(slowModelRelease, 'release\n') } catch {}
  shallow.killAll()
  await new Promise((resolve) => setTimeout(resolve, 1100))
  await new Promise((resolve) => server.close(resolve))
  rmSync(root, { recursive: true, force: true })
  rmSync(scratch, { recursive: true, force: true })
}
