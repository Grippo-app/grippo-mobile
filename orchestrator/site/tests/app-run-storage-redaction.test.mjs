import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'app-run-storage-'));
const cache = path.join(root, 'orchestrator', '.cache');
fs.mkdirSync(cache, { recursive: true });
process.env.ORCHESTRATOR_PROJECT_ROOT = root;
process.env.ORCHESTRATOR_CACHE_DIR = cache;
process.env.ORCHESTRATOR_APP_RUN_DIR = path.join(cache, 'runtime', 'app-run');
process.env.ORCHESTRATOR_APP_RUN_AUTHORITY_ROOT = root;

const paths = require('../server/paths.js');
const storage = require('../server/app-run-storage.js');
const processLayer = require('../server/app-run-process.js');
const redaction = require('../server/app-run-redaction.js');
const artifacts = require('../server/app-run-artifacts.js');
const validation = require('../server/app-run-validation.js');
const ios = require('../server/ios-runner.js');
const android = require('../server/android-runner.js');
const writerLeases = require('../../tasks/writer-leases.cjs');

try {
  assert.equal(storage.AUTHORITY_ROOT, root);
  assert.equal(storage.MAX_JSON, 256 * 1024);
  assert.equal(storage.MAX_SCREENSHOT_ENTRIES, 12000);
  fs.chmodSync(path.join(root, 'orchestrator'), 0o755);
  const crashTemp = path.join(
    paths.APP_RUN_DIR,
    `.index.json-4242-${'a'.repeat(24)}.tmp`,
  );
  fs.mkdirSync(paths.APP_RUN_DIR, { recursive: true });
  fs.writeFileSync(crashTemp, 'incomplete', { mode: 0o600 });
  storage.init();
  assert.equal(fs.existsSync(crashTemp), false);
  assert.equal(fs.statSync(path.join(root, 'orchestrator')).mode & 0o777, 0o755,
    'app-run storage must not rewrite permissions of shared ancestor directories');
  assert.equal(fs.statSync(paths.APP_RUN_DIR).mode & 0o777, 0o700);
  const id = storage.randomId('job');
  storage.writeJson(paths.APP_RUN_JOBS_DIR, id, { ok: true });
  assert.deepEqual(storage.readJson(paths.APP_RUN_JOBS_DIR, id), { ok: true });
  assert.ok(storage.list(paths.APP_RUN_JOBS_DIR, 'job').includes(id));
  assert.ok(paths.APP_RUN_INDEX_FILE.endsWith(path.join('app-run', 'index.json')));
  const toolCreatedCapture = path.join(
    paths.APP_RUN_SCREENSHOTS_DIR,
    `.capture-${'b'.repeat(24)}.png`,
  );
  fs.writeFileSync(toolCreatedCapture, 'tool-output', { mode: 0o644 });
  fs.chmodSync(toolCreatedCapture, 0o644);
  assert.equal(storage.hardenTemporary(toolCreatedCapture, 1024), true);
  if (process.platform !== 'win32') {
    assert.equal(fs.statSync(toolCreatedCapture).mode & 0o777, 0o600);
  }
  assert.equal(storage.removeTemporary(toolCreatedCapture, 1024), true);

  const linked = storage.fileFor(paths.APP_RUN_JOBS_DIR, storage.randomId('job'), '.json');
  fs.symlinkSync('/tmp/nowhere', linked);
  assert.throws(() => storage.writeFileAtomic(linked, Buffer.from('{}'), 100), /unsafe/);
  fs.unlinkSync(linked);
  const hardlinked = storage.fileFor(paths.APP_RUN_JOBS_DIR, storage.randomId('job'), '.json');
  fs.linkSync(storage.fileFor(paths.APP_RUN_JOBS_DIR, id, '.json'), hardlinked);
  assert.throws(() => storage.writeFileAtomic(hardlinked, Buffer.from('{}'), 100), /unsafe/);
  fs.unlinkSync(hardlinked);
  const originalJobsDirectory = `${paths.APP_RUN_JOBS_DIR}-held`;
  const foreignJobsDirectory = path.join(root, 'foreign-jobs');
  fs.mkdirSync(foreignJobsDirectory);
  fs.renameSync(paths.APP_RUN_JOBS_DIR, originalJobsDirectory);
  fs.symlinkSync(foreignJobsDirectory, paths.APP_RUN_JOBS_DIR);
  fs.writeFileSync(path.join(foreignJobsDirectory, `${id}.json`), '{"outside":true}\n');
  assert.throws(() => storage.readJson(paths.APP_RUN_JOBS_DIR, id), /unsafe|symlink/,
    'runtime reads must reject a swapped storage-directory ancestor');
  fs.unlinkSync(paths.APP_RUN_JOBS_DIR);
  fs.renameSync(originalJobsDirectory, paths.APP_RUN_JOBS_DIR);

  assert.doesNotMatch(redaction.redact(`Authorization: Bearer abc token=my-secret ${os.homedir()}/project`), /abc|my-secret/);
  assert.doesNotMatch(redaction.redact(
    'Authorization: Basic dXNlcjpwYXNz\naccess_token=access-value refresh-token=\"refresh value\"',
  ), /dXNlcjpwYXNz|access-value|refresh value/);
  assert.doesNotMatch(redaction.redact(
    'GITHUB_TOKEN=github-value AWS_SECRET_ACCESS_KEY:aws-value --client-secret cli-value ' +
    'https://user:pass@example.test/path',
  ), /github-value|aws-value|cli-value|user:pass/);
  assert.match(redaction.redact(`${os.homedir()}/project`), /\$HOME/);
  assert.equal(redaction.line('emulator-5554 12345678-1234-1234-1234-123456789ABC'),
    '[DEVICE] [DEVICE]');
  assert.equal(processLayer.minimalEnv({ SAFE_FLAG: '1', bad: '2' }).SAFE_FLAG, undefined);
  assert.equal(processLayer.minimalEnv({ LANG: 'C', SAFE_FLAG: '1' }).LANG, 'C');
  assert.equal(processLayer.minimalEnv({ SAFE_FLAG: '1', bad: '2' }).bad, undefined);
  assert.equal(processLayer.terminateIdentity({ pid: 1, processStartId: 'stale' }), false);
  const echo = processLayer.resolveExecutable('echo', ['/bin/echo', '/usr/bin/echo']);
  assert.ok(echo);
  const pathOne = path.join(root, 'path-one');
  const pathTwo = path.join(root, 'path-two');
  fs.mkdirSync(pathOne);
  fs.mkdirSync(pathTwo);
  for (const directory of [pathOne, pathTwo]) {
    const executable = path.join(directory, 'ambiguous-tool');
    fs.writeFileSync(executable, '#!/bin/sh\nexit 0\n', { mode: 0o755 });
    fs.chmodSync(executable, 0o755);
  }
  assert.equal(processLayer.resolveExecutable(
    'ambiguous-tool', [], [pathOne, pathTwo].join(path.delimiter),
  ), null, 'distinct PATH executables must fail closed as ambiguous');
  assert.equal(processLayer.resolveExecutable(
    'ambiguous-tool', [path.join(pathOne, 'ambiguous-tool')],
    [pathOne, pathTwo].join(path.delimiter),
  ), fs.realpathSync(path.join(pathOne, 'ambiguous-tool')),
  'a server-owned canonical executable must take precedence over PATH ambiguity');
  const sdkOne = path.join(root, 'sdk-one');
  const sdkTwo = path.join(root, 'sdk-two');
  fs.mkdirSync(sdkOne);
  fs.mkdirSync(sdkTwo);
  const previousSdkRoot = process.env.ANDROID_SDK_ROOT;
  const previousAndroidHome = process.env.ANDROID_HOME;
  try {
    process.env.ANDROID_SDK_ROOT = sdkOne;
    process.env.ANDROID_HOME = sdkTwo;
    assert.equal(android.sdkRoots().error, 'android-sdk-ambiguous');
    process.env.ANDROID_HOME = sdkOne;
    assert.deepEqual(android.sdkRoots(), {
      roots: [fs.realpathSync(sdkOne)],
      error: null,
    });
    delete process.env.ANDROID_SDK_ROOT;
    delete process.env.ANDROID_HOME;
    fs.writeFileSync(path.join(root, 'local.properties'), `sdk.dir=${sdkTwo}\n`);
    assert.deepEqual(android.sdkRoots(), {
      roots: [fs.realpathSync(sdkTwo)],
      error: null,
    }, 'the canonical project SDK configuration must precede standard layouts and PATH');
    fs.unlinkSync(path.join(root, 'local.properties'));
  } finally {
    if (previousSdkRoot === undefined) delete process.env.ANDROID_SDK_ROOT;
    else process.env.ANDROID_SDK_ROOT = previousSdkRoot;
    if (previousAndroidHome === undefined) delete process.env.ANDROID_HOME;
    else process.env.ANDROID_HOME = previousAndroidHome;
  }
  const echoResult = processLayer.runSync({
    executable: echo,
    argv: ['literal;$(never-executed)'],
    cwd: root,
    timeoutMs: 5000,
  });
  assert.equal(echoResult.ok, true);
  assert.match(echoResult.stdout, /literal;\$\(never-executed\)/);
  assert.throws(() => processLayer.runSync({
    executable: echo,
    argv: [],
    cwd: path.dirname(root),
    timeoutMs: 5000,
  }), /outside the project root/);
  const nodeExecutable = processLayer.resolveExecutable(process.execPath, [process.execPath]);
  const binaryInput = Buffer.from([0, 255, 1, 128]);
  const binaryInputResult = processLayer.runSync({
    executable: nodeExecutable,
    argv: ['-e', "process.stdin.on('data', function (bytes) { process.stdout.write(bytes.toString('hex')) })"],
    cwd: root,
    timeoutMs: 5000,
    input: binaryInput,
  });
  assert.equal(binaryInputResult.ok, true);
  assert.equal(binaryInputResult.stdout, binaryInput.toString('hex'),
    'typed synchronous process input must preserve binary plist bytes exactly');
  const abortController = new AbortController();
  const longProcess = processLayer.run({
    executable: nodeExecutable,
    argv: ['-e', 'setInterval(function () {}, 1000)'],
    cwd: root,
    timeoutMs: 5000,
    signal: abortController.signal,
  });
  abortController.abort();
  const abortedProcess = await longProcess;
  assert.equal(abortedProcess.ok, false);
  assert.equal(abortedProcess.errorCode, 'cancelled');
  const streamedLines = [];
  const splitSecret = await processLayer.run({
    executable: nodeExecutable,
    argv: ['-e', [
      "process.stdout.write('token=split')",
      "setTimeout(function () { process.stdout.write('-secret\\n') }, 20)",
    ].join(';')],
    cwd: root,
    timeoutMs: 5000,
    onLine(source, line) {
      streamedLines.push({ source, line });
    },
  });
  assert.equal(splitSecret.ok, true);
  assert.equal(splitSecret.stdout, 'token=split-secret\n',
    'bounded raw command output must remain available only to private typed parsers');
  assert.deepEqual(streamedLines, [{ source: 'stdout', line: 'token=[REDACTED]' }],
    'stream redaction must operate on logical lines across process chunk boundaries');
  assert.equal(redaction.line('safe\u0001control'), 'safe\uFFFDcontrol');
  assert.ok(Buffer.byteLength(redaction.line('😀'.repeat(4096)), 'utf8') <= 4096,
    'the durable log-line limit is measured in UTF-8 bytes');
  const leaseDirectory = path.join(root, '.writer-leases');
  fs.mkdirSync(leaseDirectory, { mode: 0o700 });
  const buildLease = writerLeases.acquire(leaseDirectory, {
    rootDir: root,
    kind: 'runtime-build',
    sessionId: writerLeases.createSessionId(),
    key: 'app-run:test-build-fence',
    pendingChild: false,
    ttlMs: 120000,
  });
  const fenceEvents = [];
  const fencedProcess = await processLayer.run({
    executable: nodeExecutable,
    argv: ['-e', 'setTimeout(function () {}, 200)'],
    cwd: root,
    timeoutMs: 5000,
    beforeSpawn() {
      fenceEvents.push('pending');
      writerLeases.markUnverified(buildLease, writerLeases.PENDING_CHILD_REASON);
    },
    onSpawn(identity) {
      fenceEvents.push('bound');
      writerLeases.updateChildPid(buildLease, identity.pid);
    },
  });
  assert.equal(fencedProcess.ok, true);
  assert.deepEqual(fenceEvents, ['pending', 'bound']);
  assert.doesNotThrow(() => writerLeases.renew(buildLease, 120000),
    'a bound runtime build lease must remain renewable');
  assert.equal(writerLeases.release(buildLease), true);
  let failedFenceSpawned = false;
  const rejectedFence = await processLayer.run({
    executable: nodeExecutable,
    argv: ['-e', 'setInterval(function () {}, 1000)'],
    cwd: root,
    timeoutMs: 5000,
    beforeSpawn() {},
    onSpawn() {
      failedFenceSpawned = true;
      throw new Error('lease binding rejected');
    },
  });
  assert.equal(failedFenceSpawned, true);
  assert.equal(rejectedFence.ok, false,
    'a process whose lease binding fails must be terminated before publication can continue');
  const detachedIdentity = processLayer.startDetached({
    executable: nodeExecutable,
    argv: ['-e', 'setInterval(function () {}, 1000)'],
    cwd: root,
  });
  assert.match(detachedIdentity.processStartId, writerLeases.PROCESS_START_ID_RE,
    'detached children must never be published without an exact process generation');
  assert.equal(processLayer.terminateIdentity({
    pid: detachedIdentity.pid,
    processStartId: null,
  }), false, 'a reconstructed PID-only identity must never gain termination authority');
  assert.equal(processLayer.terminateIdentity(detachedIdentity), true,
    'the exact in-process ownership handle must terminate its own detached child');

  assert.match(artifacts.toolchainFingerprint('android', {}), /^sha256:[a-f0-9]{64}$/);
  const apkRoot = path.join(root, 'androidApp', 'build', 'outputs', 'apk', 'debug');
  const otherRoot = path.join(root, 'androidApp', 'build', 'outputs', 'apk', 'release');
  fs.mkdirSync(apkRoot, { recursive: true });
  fs.mkdirSync(otherRoot, { recursive: true });
  const apk = path.join(apkRoot, 'fixture.apk');
  fs.writeFileSync(apk, Buffer.from('artifact-one'));
  const artifactHash = 'sha256:' + (await import('node:crypto')).createHash('sha256')
    .update(fs.readFileSync(apk)).digest('hex');
  const sourceHash = 'sha256:' + '1'.repeat(64);
  const runConfigHash = 'sha256:' + '2'.repeat(64);
  const manifest = artifacts.create({
    platform: 'android',
    variantId: 'debug',
    sourceRevision: sourceHash,
    runConfigHash,
    artifact: {
      path: apk,
      hash: artifactHash,
      size: fs.statSync(apk).size,
      applicationId: 'com.example.fixture',
      targetArchitectures: ['x86_64'],
    },
    tools: {},
  });
  const expected = {
    platform: 'android',
    variantId: 'debug',
    applicationId: 'com.example.fixture',
    runConfigHash,
    allowedBuildRoot: apkRoot,
    toolchainFingerprint: artifacts.toolchainFingerprint('android', {}),
    targetArchitecture: 'x86_64',
    runtimeKind: null,
  };
  assert.equal(artifacts.verify(manifest, expected).ok, true);
  assert.equal(artifacts.validate({
    ...manifest,
    applicationId: 'not a package id',
  }), 'artifact manifest is invalid');
  assert.equal(artifacts.validate({
    ...manifest,
    buildInputFingerprint: 'sha256:' + 'f'.repeat(64),
  }), 'artifact manifest is invalid');
  assert.equal(artifacts.verify(manifest, { ...expected, allowedBuildRoot: otherRoot }).error, 'artifact-path-mismatch');
  assert.equal(artifacts.verify(manifest, {
    ...expected, toolchainFingerprint: 'sha256:' + '3'.repeat(64),
  }).error, 'artifact-toolchain-mismatch');
  assert.equal(artifacts.verify(manifest, { ...expected, targetArchitecture: 'arm64-v8a' }).error,
    'artifact-architecture-mismatch');
  fs.writeFileSync(apk, Buffer.from('artifact-two'));
  assert.equal(artifacts.verify(manifest, expected).error, 'artifact-hash-mismatch');
  fs.writeFileSync(apk, Buffer.from('artifact-one'));
  const movedApkRoot = path.join(path.dirname(apkRoot), 'debug-real');
  fs.renameSync(apkRoot, movedApkRoot);
  fs.symlinkSync(movedApkRoot, apkRoot);
  assert.equal(artifacts.verify(manifest, expected).error, 'artifact-invalid');
  fs.unlinkSync(apkRoot);
  fs.renameSync(movedApkRoot, apkRoot);
  const linkedApk = path.join(apkRoot, 'linked.apk');
  fs.linkSync(apk, linkedApk);
  assert.equal(artifacts.verify(manifest, expected).error, 'artifact-invalid');
  fs.unlinkSync(linkedApk);
  const symlinkApk = path.join(apkRoot, 'symlink.apk');
  fs.symlinkSync(apk, symlinkApk);
  const symlinkManifest = { ...manifest, artifactRelativePath: path.relative(root, symlinkApk) };
  assert.equal(artifacts.verify(symlinkManifest, expected).error, 'artifact-invalid');
  fs.unlinkSync(symlinkApk);
  storage.writeJson(paths.APP_RUN_ARTIFACTS_DIR, manifest.artifactId, {
    ...manifest,
    unexpectedField: true,
  });
  assert.ok(artifacts.prune().includes(manifest.artifactId));
  assert.equal(fs.existsSync(storage.fileFor(
    paths.APP_RUN_ARTIFACTS_DIR, manifest.artifactId, '.json',
  )), true, 'corrupt artifact authority must be preserved as recovery evidence');
  storage.writeJson(paths.APP_RUN_ARTIFACTS_DIR, manifest.artifactId, manifest);

  assert.equal(validation.MAX_ITEMS, 200);
  assert.equal(typeof validation.canonicalTask, 'function');
  assert.equal(validation.validateSaveBody({}), 'bad-validation-request');
  const manual = validation.parseManual(`## Acceptance
### Manual
- Open the app
  - Confirm the title
- Rotate the device
### Automated
- ignored
`, 'sha256:' + 'a'.repeat(64));
  assert.equal(manual.length, 2);
  assert.deepEqual(manual[0].notes, ['Confirm the title']);
  assert.throws(() => validation.parseManual(`## Acceptance
### Manual
- Direct check
#### Nested heading
- Must not become a direct manual item
`, sourceHash), /non-bullet structural content/);
  const duplicateValidationBody = {
    taskStem: 'TASK_1_fixture',
    expectedTaskSourceRevision: sourceHash,
    sessionId: 'session-' + '3'.repeat(36),
    expectedSessionRevision: 1,
    validationRevision: runConfigHash,
    items: [
      { itemId: 'manual-' + '4'.repeat(24), result: 'pass', note: null, screenshotIds: [] },
      { itemId: 'manual-' + '4'.repeat(24), result: 'pass', note: null, screenshotIds: [] },
    ],
    acknowledgeStaleTask: false,
    idempotencyKey: 'validation-duplicate',
  };
  assert.equal(validation.validateSaveBody(duplicateValidationBody), 'bad-validation-request');
  const receipt = {
    schemaVersion: 1,
    receiptId: 'receipt-' + '4'.repeat(36),
    taskStem: 'TASK_1_fixture',
    taskSourceRevision: sourceHash,
    runJobId: 'job-' + '5'.repeat(36),
    sessionId: 'session-' + '5'.repeat(36),
    platform: 'android',
    deviceSummary: 'Pixel 8',
    artifactId: manifest.artifactId,
    appProjectSourceRevision: sourceHash,
    checklist: [{
      itemId: 'manual-' + '6'.repeat(24),
      result: 'pass',
      note: null,
      screenshotIds: [],
    }],
    overall: 'passed',
    staleSource: false,
    staleTask: false,
    createdAt: new Date().toISOString(),
  };
  assert.equal(validation.validateReceipt(receipt), null);
  assert.match(validation.validateReceipt({ ...receipt, deviceSummary: '' }), /invalid/);
  assert.match(validation.validateReceipt({ ...receipt, overall: 'partial' }), /invalid/);
  const corruptReceiptId = storage.randomId('receipt');
  storage.writeJson(paths.APP_RUN_HISTORY_DIR, corruptReceiptId, {
    ...receipt,
    receiptId: corruptReceiptId,
    unexpectedField: true,
  }, 128 * 1024);
  assert.ok(validation.pruneHistory(null).includes(corruptReceiptId));
  assert.equal(fs.existsSync(storage.fileFor(
    paths.APP_RUN_HISTORY_DIR, corruptReceiptId, '.json',
  )), true, 'corrupt validation authority must be preserved as recovery evidence');
  assert.throws(() => validation.history(null, 20),
    (error) => error && error.code === 'validation-history-invalid' &&
      error.recordIds.includes(corruptReceiptId),
    'history readers must fail closed instead of silently omitting corrupt receipts');
  storage.remove(paths.APP_RUN_HISTORY_DIR, corruptReceiptId, '.json', 128 * 1024);

  const derived = ios.derivedDataPath({ runConfigHash: 'sha256:' + 'b'.repeat(64) });
  assert.ok(derived.startsWith(paths.APP_RUN_DIR));
  const productRoot = path.join(derived, 'Build', 'Products', 'Debug-iphonesimulator');
  const product = path.join(productRoot, 'Fixture.app');
  fs.mkdirSync(product, { recursive: true });
  const plistBytes = Buffer.from('bplist00\u0000fixture-plist', 'utf8');
  fs.writeFileSync(path.join(product, 'Info.plist'), plistBytes);
  fs.writeFileSync(path.join(product, 'Fixture'), Buffer.from('ios-product-binary'));
  let inspectedPlist = null;
  const resolvedIosArtifact = ios.resolveArtifact({
    runConfigHash: 'sha256:' + 'b'.repeat(64),
    applicationId: 'com.example.fixture',
    tools: { plutil: '/fixture/plutil' },
    commandRunner: {
      runSync(spec) {
        inspectedPlist = spec;
        return {
          ok: true,
          status: 0,
          signal: null,
          timedOut: false,
          stdout: 'com.example.fixture\n',
          stderr: '',
          errorCode: null,
        };
      },
    },
  }, {
    project: 'iosApp/Fixture.xcodeproj',
    scheme: 'Fixture',
    id: 'debug',
    configuration: 'Debug',
  }, derived);
  assert.equal(resolvedIosArtifact.applicationId, 'com.example.fixture');
  assert.equal(inspectedPlist.argv.at(-1), '-');
  assert.deepEqual(inspectedPlist.input, plistBytes,
    'plutil must inspect descriptor-proven bytes through stdin, never a racy bundle path');
  fs.unlinkSync(path.join(product, 'Info.plist'));
  fs.symlinkSync('/tmp/foreign.plist', path.join(product, 'Info.plist'));
  assert.throws(() => ios.resolveArtifact({
    runConfigHash: 'sha256:' + 'b'.repeat(64),
    applicationId: 'com.example.fixture',
    tools: { plutil: '/fixture/plutil' },
    commandRunner: {
      runSync() {
        throw new Error('an unsafe plist must be rejected before plutil');
      },
    },
  }, {
    project: 'iosApp/Fixture.xcodeproj',
    scheme: 'Fixture',
    id: 'debug',
    configuration: 'Debug',
  }, derived), /symlink|unsafe/);

  const app = path.join(root, 'iosApp', 'Fixture.app');
  fs.mkdirSync(app, { recursive: true });
  const binary = path.join(app, 'Fixture');
  fs.writeFileSync(binary, Buffer.from('ios-binary'));
  assert.match(ios.hashAppTree(app).hash, /^sha256:[a-f0-9]{64}$/);
  const appLink = path.join(app, 'unsafe-link');
  fs.symlinkSync(binary, appLink);
  assert.throws(() => ios.hashAppTree(app), /symlink/);
  fs.unlinkSync(appLink);
  const appHardlink = path.join(app, 'unsafe-hardlink');
  fs.linkSync(binary, appHardlink);
  assert.throws(() => ios.hashAppTree(app), /unsafe/);
} finally {
  fs.rmSync(root, { recursive: true, force: true });
}
