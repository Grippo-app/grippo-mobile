import assert from 'node:assert/strict';
import { after, test } from 'node:test';
import { createServer, request } from 'node:http';
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const REPO = fileURLToPath(new URL('../../../', import.meta.url));
const root = mkdtempSync(join(tmpdir(), 'app-run-http-'));
const orchestrator = join(root, 'orchestrator');
const tasks = join(orchestrator, 'tasks');
const cache = join(orchestrator, '.cache');
for (const directory of [
  join(tasks, 'backlog'),
  join(tasks, 'pending'),
  join(tasks, 'todo'),
  join(tasks, 'done'),
  join(cache, 'site'),
]) mkdirSync(directory, { recursive: true });
writeFileSync(join(orchestrator, 'project-config.md'), `---
productName: HTTP fixture
applicationId: com.example.http
iosEnabled: false
androidAssembleTask: :androidApp:assembleDebug
---
`);
writeFileSync(join(tasks, 'INDEX.json'), JSON.stringify({
  version: 2,
  generatedAt: new Date().toISOString(),
  backlog: [],
  pending: [],
  todo: [],
  done: [],
}, null, 2) + '\n');

const reserve = createServer();
await new Promise((resolve) => reserve.listen(0, '127.0.0.1', resolve));
const port = reserve.address().port;
await new Promise((resolve) => reserve.close(resolve));

const child = spawn(process.execPath, [join(REPO, 'orchestrator', 'site', 'server.js')], {
  cwd: root,
  env: {
    ...process.env,
    PATH: '/usr/bin:/bin',
    ANDROID_HOME: '',
    ANDROID_SDK_ROOT: '',
    ORCHESTRATOR_PROJECT_ROOT: root,
    ORCHESTRATOR_CACHE_DIR: cache,
    PORT: String(port),
    RUNNER_DISABLED: '1',
  },
  stdio: ['ignore', 'pipe', 'pipe'],
});
let stdout = '';
let stderr = '';
child.stdout.on('data', (chunk) => { stdout += chunk; });
child.stderr.on('data', (chunk) => { stderr += chunk; });
await new Promise((resolve, reject) => {
  const timeout = setTimeout(() => reject(new Error(`server timeout\n${stdout}\n${stderr}`)), 10000);
  const poll = () => {
    if (stdout.includes('Press Ctrl+C to stop.')) {
      clearTimeout(timeout);
      resolve();
      return;
    }
    if (child.exitCode !== null) {
      clearTimeout(timeout);
      reject(new Error(`server exited ${child.exitCode}\n${stdout}\n${stderr}`));
      return;
    }
    setTimeout(poll, 25);
  };
  poll();
});

const base = `http://127.0.0.1:${port}`;
after(async () => {
  if (child.exitCode === null) child.kill('SIGTERM');
  await Promise.race([
    new Promise((resolve) => child.once('close', resolve)),
    new Promise((resolve) => setTimeout(resolve, 3000)),
  ]);
  rmSync(root, { recursive: true, force: true });
});

async function json(url, options) {
  const response = await fetch(base + url, options);
  const body = await response.json().catch(() => ({}));
  return { response, body };
}

async function rawStatus(host) {
  return new Promise((resolve, reject) => {
    const pending = request({
      hostname: '127.0.0.1',
      port,
      path: '/api/app-run/status',
      method: 'GET',
      headers: { host },
    }, (response) => {
      response.resume();
      response.on('end', () => resolve(response.statusCode));
    });
    pending.on('error', reject);
    pending.end();
  });
}

test('app-run HTTP surface is local, typed, bounded, and path-opaque', async () => {
  const state = await json('/api/state');
  assert.equal(state.response.status, 200);
  const csrf = state.body.csrfToken;
  assert.match(csrf, /^[a-f0-9]{48}$/);

  const status = await json('/api/app-run/status');
  assert.equal(status.response.status, 200);
  assert.equal(status.body.ok, true);
  assert.equal(Array.isArray(status.body.availability), true);
  assert.equal(JSON.stringify(status.body).includes(root), false);
  assert.equal(await rawStatus('attacker.example'), 403);
  assert.equal(await rawStatus(`localhost:${port},attacker.example`), 403);

  assert.equal((await json('/api/app-run/status?unknown=1')).response.status, 400);
  assert.equal((await json('/api/app-run/targets?refresh=yes')).response.status, 400);
  assert.equal((await json('/api/app-run/logs?jobId=x&sessionId=y')).response.status, 400);
  assert.equal((await json(`/api/app-run/logs?jobId=job-${'a'.repeat(36)}`)).response.status, 404);
  assert.equal((await json(`/api/app-run/logs?sessionId=session-${'b'.repeat(36)}`)).response.status, 404);
  assert.equal((await json('/api/app-run/history?limit=101')).response.status, 400);
  assert.equal((await json('/api/app-run/history?cursor=../../secret')).response.status, 400);
  assert.equal((await json(
    '/api/app-run/validation?taskStem=TASK_9007199254740992_unsafe',
  )).response.status, 400);
  assert.equal((await json('/api/app-run/screenshots/%2e%2e%2fsecret')).response.status, 404);

  const typedBody = {
    platform: 'android',
    targetId: 'target-' + 'a'.repeat(32),
    discoveryRevision: 'discovery-' + 'b'.repeat(36),
    variantId: 'debug',
    buildMode: 'rebuild',
    taskStem: null,
    surfaceId: null,
    expectedProjectSourceRevision: 'sha256:' + 'c'.repeat(64),
    confirmationToken: null,
    whenBusy: 'fail',
    idempotencyKey: 'http-security-start',
    executable: '/bin/sh',
  };
  let response = await json('/api/app-run/start', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(typedBody),
  });
  assert.equal(response.response.status, 403);
  assert.equal(response.body.error, 'bad-csrf');

  response = await json('/api/app-run/start', {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'x-orchestrator-csrf': csrf,
      origin: 'https://attacker.example',
    },
    body: JSON.stringify(typedBody),
  });
  assert.equal(response.response.status, 403);
  assert.equal(response.body.error, 'bad-origin');

  response = await json('/api/app-run/start', {
    method: 'POST',
    headers: {
      'content-type': 'text/plain',
      'x-orchestrator-csrf': csrf,
      origin: base,
    },
    body: JSON.stringify(typedBody),
  });
  assert.equal(response.response.status, 415);
  assert.equal(response.body.error, 'json-required');

  response = await json('/api/app-run/start', {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'x-orchestrator-csrf': csrf,
      origin: base,
    },
    body: JSON.stringify(typedBody),
  });
  assert.equal(response.response.status, 400);
  assert.equal(response.body.error, 'bad-app-run-request');

  response = await json('/api/app-run/start', {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'x-orchestrator-csrf': csrf,
      origin: base,
    },
    body: JSON.stringify({ padding: 'x'.repeat(300000) }),
  });
  assert.equal(response.response.status, 413);
  assert.equal(response.body.error, 'bad-json');

  assert.equal((await fetch(base + '/orchestrator/.cache/runtime/app-run/index.json')).status, 403);
  assert.equal((await fetch(base + '/.cache/runtime/app-run/index.json')).status, 403);
  assert.equal((await fetch(base + '/figma/.account.json')).status, 403);
  assert.equal((await fetch(base + '/figma/%2eaccount.json')).status, 403);
  assert.equal((await fetch(base + '/figma%5c.account.json')).status, 403);
});

test('app-run schemas have one feature-owned canonical path and no legacy aliases', async () => {
  const schemaPaths = [
    {
      current: '/site/contracts/app-run/config.schema.json',
      retiredPaths: ['/app-run.schema.json', '/schemas/app-run.schema.json'],
    },
    {
      current: '/site/contracts/app-run/job.schema.json',
      retiredPaths: ['/app-run-job.schema.json', '/schemas/app-run-job.schema.json'],
    },
    {
      current: '/site/contracts/app-run/validation-receipt.schema.json',
      retiredPaths: ['/app-run-validation.schema.json', '/schemas/app-run-validation.schema.json'],
    },
  ];

  for (const entry of schemaPaths) {
    const current = await fetch(base + entry.current);
    assert.equal(current.status, 200);
    await current.json();
    for (const retiredPath of entry.retiredPaths) {
      assert.equal((await fetch(base + retiredPath)).status, 404, retiredPath);
    }
  }
});

test('the board summary consumes app validation receipts through the production reader', async () => {
  const history = join(cache, 'runtime', 'app-run', 'history');
  mkdirSync(history, { recursive: true, mode: 0o700 });
  const corruptId = 'receipt-' + 'c'.repeat(36);
  writeFileSync(join(history, corruptId + '.json'),
    JSON.stringify({ schemaVersion: 1, receiptId: corruptId }) + '\n', { mode: 0o600 });

  const corrupted = await json('/api/tasks/summary');
  assert.equal(corrupted.response.status, 200);
  assert.equal(corrupted.body.partial, true,
    'a corrupt receipt store must surface as an explicitly partial summary');
  assert.ok(corrupted.body.limitations.includes('app-validation-receipts-invalid'),
    'receipt corruption must be named, never silently treated as no receipts');

  rmSync(join(history, corruptId + '.json'));
  const validId = 'receipt-' + 'd'.repeat(36);
  writeFileSync(join(history, validId + '.json'), JSON.stringify({
    schemaVersion: 1,
    receiptId: validId,
    taskStem: 'TASK_1_http_fixture',
    taskSourceRevision: 'sha256:' + '7'.repeat(64),
    runJobId: 'job-' + '7'.repeat(36),
    sessionId: 'session-' + '7'.repeat(36),
    platform: 'android',
    deviceSummary: 'Pixel 8 · Android 15',
    artifactId: 'artifact-' + '7'.repeat(36),
    appProjectSourceRevision: 'sha256:' + '8'.repeat(64),
    checklist: [{
      itemId: 'manual-' + '9'.repeat(24),
      result: 'pass',
      note: null,
      screenshotIds: [],
    }],
    overall: 'passed',
    staleSource: false,
    staleTask: false,
    createdAt: new Date().toISOString(),
  }, null, 2) + '\n', { mode: 0o600 });

  const healthy = await json('/api/tasks/summary');
  assert.equal(healthy.response.status, 200);
  assert.equal(healthy.body.limitations.includes('app-validation-receipts-invalid'), false,
    'replacing the corrupt receipt must clear the limitation through the stat-keyed reader');
});
