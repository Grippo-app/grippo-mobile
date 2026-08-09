#!/usr/bin/env node

import assert from 'node:assert/strict'
import { chmodSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { spawn } from 'node:child_process'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const root = mkdtempSync(join(tmpdir(), 'shallow-multiprocess-'))
const project = join(root, 'project')
const tasks = join(project, 'orchestrator', 'tasks')
const cache = join(project, 'orchestrator', '.cache', 'tasks')
const intake = join(cache, 'intake')
const scratchRoot = join(root, 'scratch-authority')
const scratch = join(scratchRoot, 'scratch')
const starts = join(intake, 'multiprocess-model.log')
const modulePath = resolve(HERE, '../server/shallow-intake.js')
const liveClients = new Set()
const require = createRequire(import.meta.url)
const taskCore = require('../../tasks/task-state-core.cjs')
const taskSource = require('../../tasks/task-source-contract.cjs')
const SOURCE_BLOCK = taskSource.render(taskSource.manualForIntent('shallow-multiprocess', 'manual', 'fixture:shallow-multiprocess'))

function task(number, title) {
  return `# TASK ${number} — ${title}\n\n${SOURCE_BLOCK}\n\n## Goal\n\nValidate cross-process ownership.\n`
}

for (const column of ['backlog', 'pending', 'todo', 'done']) mkdirSync(join(tasks, column), { recursive: true })
for (const name of ['intake', 'locks', 'requests', 'finalizations', 'creations', 'edits']) {
  mkdirSync(join(cache, name), { recursive: true })
}
mkdirSync(scratch, { recursive: true })
chmodSync(intake, 0o700); chmodSync(scratch, 0o700)

const stems = ['TASK_1_same', 'TASK_2_left', 'TASK_3_right']
stems.forEach((stem, index) => writeFileSync(join(tasks, 'backlog', stem + '.md'), task(index + 1, stem.slice(stem.indexOf('_', 5) + 1))))
const initial = taskCore.validateTaskState({ tasksDir: tasks, repoRoot: project, checkIndex: false, includeRuntime: false })
assert.equal(initial.ok, true, JSON.stringify(initial.findings))
writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify(taskCore.deriveIndex(initial._model, '2026-07-13T00:00:00Z'), null, 2) + '\n')

const fake = join(root, 'fake-model.py')
writeFileSync(fake, `#!/usr/bin/env python3
import json, os, pathlib, sys, time
prompt = sys.stdin.read()
marker = 'BEGIN_UNTRUSTED_CONTEXT_JSON_TO_EOF\\n'
context = json.loads(prompt[prompt.rfind(marker) + len(marker):])
log = pathlib.Path(os.environ['TMPDIR']) / 'multiprocess-model.log'
with log.open('a', encoding='utf-8') as stream:
    stream.write('start ' + context['task']['stem'] + ' ' + str(int(time.time() * 1000)) + '\\n')
    stream.flush()
time.sleep(0.35)
output = {'readiness': 'ready', 'summary': 'A bounded cross-process advisory result.', 'likelyAreas': ['unknown'], 'possibleDuplicates': [], 'missingContext': [], 'riskFlags': []}
with log.open('a', encoding='utf-8') as stream:
    stream.write('end ' + context['task']['stem'] + ' ' + str(int(time.time() * 1000)) + '\\n')
    stream.flush()
print(json.dumps({'type': 'result', 'subtype': 'success', 'is_error': False, 'structured_output': output}, separators=(',', ':')), flush=True)
`)
chmodSync(fake, 0o755)

const childSource = `
const fs = require('node:fs');
const intake = require(${JSON.stringify(modulePath)});
const stem = process.env.TEST_STEM;
const startAt = Number(process.env.TEST_START_AT);
const initOnly = process.env.TEST_INIT_ONLY === '1';
const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
let stopping = false;
process.on('SIGTERM', () => {
  if (stopping) return;
  stopping = true;
  try { intake.killAll(); } catch {}
  setTimeout(() => process.exit(143), 1000);
});
(async () => {
  while (Date.now() < startAt) await wait(5);
  intake.init();
  if (initOnly) {
    process.stdout.write(JSON.stringify({ initialized: true }) + '\\n');
    process.exit(0);
  }
  const scheduled = intake.schedule(stem, 'multiprocess-fixture');
  const deadline = Date.now() + 20000;
  while (Date.now() < deadline) {
    const row = intake.snapshot()[stem];
    if (row && row.status === 'complete' && !fs.existsSync(${JSON.stringify(join(intake, '.worker.json'))})) {
      process.stdout.write(JSON.stringify({ requestId: scheduled.requestId, finalRequestId: row.requestId }) + '\\n');
      process.exit(0);
    }
    await wait(25);
  }
  const row = intake.snapshot()[stem] || null;
  const workerFile = ${JSON.stringify(join(intake, '.worker.json'))};
  let worker = null;
  try { worker = JSON.parse(fs.readFileSync(workerFile, 'utf8')); } catch {}
  let modelLog = null;
  try { modelLog = fs.readFileSync(${JSON.stringify(starts)}, 'utf8'); } catch {}
  throw new Error('multiprocess fixture timed out: ' + JSON.stringify({ stem, row, worker, modelLog }));
})().catch((error) => { console.error(error.stack || error); process.exit(1); });
`

function runClient(stem, startAt, initOnly = false) {
  const child = spawn(process.execPath, ['-e', childSource], {
    cwd: project,
    env: {
      ...process.env,
      TEST_STEM: stem,
      TEST_START_AT: String(startAt),
      TEST_INIT_ONLY: initOnly ? '1' : '0',
      ORCHESTRATOR_PROJECT_ROOT: project,
      ORCHESTRATOR_TASKS_DIR: tasks,
      ORCHESTRATOR_TASK_INTAKE_DIR: intake,
      ORCHESTRATOR_LOCKS_DIR: join(cache, 'locks'),
      ORCHESTRATOR_REQUESTS_DIR: join(cache, 'requests'),
      ORCHESTRATOR_FINALIZATIONS_DIR: join(cache, 'finalizations'),
      ORCHESTRATOR_TASK_CREATIONS_DIR: join(cache, 'creations'),
      ORCHESTRATOR_TASK_EDITS_DIR: join(cache, 'edits'),
      SHALLOW_INTAKE_SCRATCH_DIR: scratch,
      SHALLOW_INTAKE_SCRATCH_ROOT: scratchRoot,
      SHALLOW_INTAKE_CLAUDE: fake,
      SHALLOW_INTAKE_TIMEOUT_MS: '5000',
      TMPDIR: intake
    },
    stdio: ['ignore', 'pipe', 'pipe']
  })
  liveClients.add(child)
  let stdout = '', stderr = ''
  child.stdout.setEncoding('utf8'); child.stderr.setEncoding('utf8')
  child.stdout.on('data', (chunk) => { stdout += chunk })
  child.stderr.on('data', (chunk) => { stderr += chunk })
  return new Promise((resolveRun, rejectRun) => {
    child.on('error', (error) => {
      liveClients.delete(child)
      rejectRun(error)
    })
    child.on('close', (code) => {
      liveClients.delete(child)
      if (code !== 0) {
        rejectRun(new Error(stderr || `client exited ${code}`))
        return
      }
      try { resolveRun(JSON.parse(stdout.trim())) }
      catch (error) { rejectRun(new Error(`client returned invalid JSON: ${error.message}`)) }
    })
  })
}

const wait = (ms) => new Promise((resolveWait) => setTimeout(resolveWait, ms))

async function stopLiveClients() {
  for (const child of liveClients) {
    try { child.kill('SIGTERM') } catch {}
  }
  let deadline = Date.now() + 2500
  while (liveClients.size && Date.now() < deadline) await wait(25)
  for (const child of liveClients) {
    try { child.kill('SIGKILL') } catch {}
  }
  deadline = Date.now() + 2500
  while (liveClients.size && Date.now() < deadline) await wait(25)
  if (liveClients.size) throw new Error(`multiprocess fixture retained ${liveClients.size} client process(es)`)
}

function modelEvents() {
  if (!existsSync(starts)) return []
  return readFileSync(starts, 'utf8').trim().split('\n').filter(Boolean).map((line) => {
    const [kind, stem, time] = line.split(' ')
    return { kind, stem, time: Number(time) }
  })
}

try {
  const stemLocks = join(intake, '.stem-locks')
  for (let round = 0; round < 5; round++) {
    rmSync(stemLocks, { recursive: true, force: true })
    assert.equal(existsSync(stemLocks), false, 'the init race fixture must not pre-create the stem-lock directory')
    const initAt = Date.now() + 300
    const initialized = await Promise.all([
      runClient('', initAt, true), runClient('', initAt, true)
    ])
    assert.deepEqual(initialized, [{ initialized: true }, { initialized: true }])
    assert.equal(statSync(stemLocks).mode & 0o777, 0o700)
  }
  console.log('ok 1 - simultaneous initializers repeatedly admit only the exact private stem-lock directory')

  let startAt = Date.now() + 500
  const same = await Promise.all([runClient(stems[0], startAt), runClient(stems[0], startAt)])
  assert.equal(same[0].requestId, same[1].requestId)
  assert.equal(same[0].finalRequestId, same[0].requestId)
  assert.deepEqual(modelEvents().map((event) => event.kind), ['start', 'end'])
  console.log('ok 2 - two site processes deduplicate one stem to one model generation')

  writeFileSync(starts, '')
  startAt = Date.now() + 500
  await Promise.all([runClient(stems[1], startAt), runClient(stems[2], startAt)])
  const events = modelEvents()
  assert.equal(events.filter((event) => event.kind === 'start').length, 2)
  assert.equal(events.filter((event) => event.kind === 'end').length, 2)
  assert.deepEqual(events.map((event) => event.kind), ['start', 'end', 'start', 'end'],
    'the global worker must serialize different stems across site processes')
  assert.ok(events[1].time <= events[2].time)
  console.log('ok 3 - one durable global worker serializes different stems across site processes')
  console.log('\nshallow-intake multiprocess: 3 checks passed')
} finally {
  await stopLiveClients()
  rmSync(root, { recursive: true, force: true })
}
