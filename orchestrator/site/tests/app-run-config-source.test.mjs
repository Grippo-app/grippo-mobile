import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'app-run-config-'));
const orchestrator = path.join(root, 'orchestrator');
fs.mkdirSync(orchestrator, { recursive: true });
fs.writeFileSync(path.join(orchestrator, 'project-config.md'), `---
productName: Sample
applicationId: com.example.sample
iosEnabled: true
androidAssembleTask: :androidApp:assembleDebug
---
`);
fs.writeFileSync(path.join(orchestrator, 'app-run.json'), JSON.stringify({
  schemaVersion: 1,
  android: {
    module: 'androidApp',
    variants: [{ id: 'debug', label: 'Debug', assembleTaskRef: 'project-config.androidAssembleTask' }],
  },
  ios: {
    project: 'iosApp/iosApp.xcodeproj',
    scheme: 'iosApp',
    configurations: [{ id: 'debug', label: 'Debug', configuration: 'Debug' }],
  },
}, null, 2));
fs.mkdirSync(path.join(root, 'androidApp', 'src', 'main'), { recursive: true });
fs.writeFileSync(path.join(root, 'androidApp', 'src', 'main', 'App.kt'), 'fun app() = 1\n');
fs.mkdirSync(path.join(root, 'androidApp', 'build'), { recursive: true });
fs.writeFileSync(path.join(root, 'androidApp', 'build', 'ignored.bin'), 'derived');
fs.mkdirSync(path.join(orchestrator, 'site'), { recursive: true });
fs.writeFileSync(path.join(orchestrator, 'site', 'fixture.js'), 'export const fixture = 1;\n');
fs.writeFileSync(path.join(orchestrator, 'launch.md'), '# Launch contract\n');
fs.writeFileSync(path.join(root, 'settings.gradle.kts'), 'include(":androidApp")\n');
process.env.ORCHESTRATOR_PROJECT_ROOT = root;
process.env.ORCHESTRATOR_CACHE_DIR = path.join(orchestrator, '.cache');

const config = require('../server/app-run-config.js');
const revision = require('../../tasks/project-source-revision.cjs');
const discovery = require('../server/device-discovery.js');

try {
  assert.match('com.example.sample', config.APP_ID_RE);
  assert.match(':androidApp:assembleDebug', config.TASK_RE);
  assert.equal(config.safeRelativeProjectPath('iosApp/iosApp.xcodeproj', '.xcodeproj'), true);
  assert.equal(config.safeRelativeProjectPath('../outside.xcodeproj', '.xcodeproj'), false);
  assert.equal(config.safeRelativeProjectPath('orchestrator/unsafe.xcodeproj', '.xcodeproj'), false);
  assert.equal(config.loadProjectConfig().ok, true);
  const loaded = config.load();
  assert.equal(loaded.ok, true);
  assert.equal(loaded.source, 'manifest');
  assert.deepEqual(config.sourceRoots(loaded), ['androidApp', 'iosApp']);
  assert.equal(config.resolveVariant(loaded, 'android', 'debug').assembleTask, ':androidApp:assembleDebug');
  assert.match(loaded.runConfigHash, /^sha256:[a-f0-9]{64}$/);
  const manifestFile = path.join(orchestrator, 'app-run.json');
  const manifestBytes = fs.readFileSync(manifestFile);
  fs.unlinkSync(manifestFile);
  const conventional = config.load();
  assert.equal(conventional.ok, true);
  assert.equal(conventional.source, 'default',
    'only a genuinely absent optional manifest may use the canonical project convention');
  fs.symlinkSync('/tmp/foreign-app-run.json', manifestFile);
  assert.equal(config.load().error, 'app-run-config-invalid',
    'an unsafe manifest must never be reinterpreted as an absent optional manifest');
  fs.unlinkSync(manifestFile);
  fs.writeFileSync(manifestFile, manifestBytes);

  const unknown = structuredClone(loaded.manifest);
  unknown.android.variants[0].command = './gradlew';
  assert.equal(config.validateManifest(unknown).ok, false);
  const duplicate = structuredClone(loaded.manifest);
  duplicate.android.variants.push(structuredClone(duplicate.android.variants[0]));
  assert.equal(config.validateManifest(duplicate).ok, false);
  const reservedRoot = structuredClone(loaded.manifest);
  reservedRoot.ios.project = 'orchestrator/unsafe.xcodeproj';
  assert.equal(config.validateManifest(reservedRoot).ok, false);
  const projectConfigFile = path.join(orchestrator, 'project-config.md');
  const canonicalProjectConfig = fs.readFileSync(projectConfigFile, 'utf8');
  fs.writeFileSync(projectConfigFile,
    canonicalProjectConfig.replace('applicationId: com.example.sample',
      'applicationId: com.example.sample\napplicationId: com.example.shadow'));
  assert.equal(config.loadProjectConfig().error, 'project-config-invalid');
  fs.writeFileSync(projectConfigFile,
    canonicalProjectConfig.replace(':androidApp:assembleDebug', ':otherApp:assembleDebug'));
  assert.equal(config.load().error, 'app-run-config-invalid',
    'the configured module and canonical Gradle task must describe the same project root');
  fs.writeFileSync(projectConfigFile,
    canonicalProjectConfig.replace('androidAssembleTask: :androidApp:assembleDebug\n', ''));
  assert.equal(config.loadProjectConfig().error, 'project-config-invalid',
    'the build task must never fall back when canonical project configuration omits it');
  fs.writeFileSync(projectConfigFile, canonicalProjectConfig);

  const sourceOptions = { profile: 'app-build', appRoots: config.sourceRoots(loaded) };
  const first = revision.compute(root, sourceOptions);
  assert.equal(first.available, true);
  fs.writeFileSync(path.join(root, 'androidApp', 'build', 'ignored.bin'), 'changed-derived-output');
  assert.equal(revision.compute(root, sourceOptions).revision, first.revision);
  const sourceBuildDirectory = path.join(root, 'androidApp', 'src', 'main', 'kotlin', 'sample', 'build');
  fs.mkdirSync(sourceBuildDirectory, { recursive: true });
  const sourceBuildFile = path.join(sourceBuildDirectory, 'BuildModel.kt');
  fs.writeFileSync(sourceBuildFile, 'class BuildModel\n');
  assert.notEqual(revision.compute(root, sourceOptions).revision, first.revision,
    'a legitimate source package named build must not be mistaken for a module output directory');
  fs.rmSync(path.join(root, 'androidApp', 'src', 'main', 'kotlin'), { recursive: true });
  assert.equal(revision.compute(root, sourceOptions).revision, first.revision);
  fs.writeFileSync(path.join(root, 'androidApp', 'src', 'main', 'App.kt'), 'fun app() = 2\n');
  assert.notEqual(revision.compute(root, sourceOptions).revision, first.revision);

  fs.symlinkSync('/tmp', path.join(root, 'androidApp', 'src', 'linked'));
  const unsafe = revision.compute(root, sourceOptions);
  assert.equal(unsafe.available, false);
  assert.equal(unsafe.reasonCode, 'symlink-input');
  const unavailableSnapshot = discovery.discover({
    refresh: true,
    androidTools: {},
    iosTools: {},
    forceIos: true,
    commandRunner: {
      runSync() {
        return {
          ok: false, status: 1, signal: null, timedOut: false,
          stdout: '', stderr: '', errorCode: 'fixture-unavailable',
        };
      },
    },
  });
  const unavailableTargets = unavailableSnapshot.public;
  assert.equal(unavailableTargets.projectSourceRevision, null);
  assert.ok(unavailableTargets.platforms.every((platform) =>
    platform.availability === 'unavailable' && platform.reasonCode === 'symlink-input'),
  'source integrity failures must be explicit on every platform instead of a silently disabled Run');
  assert.equal(discovery.resolveProfile(
    'android',
    `profile-${'a'.repeat(32)}`,
    `runtime-${'b'.repeat(32)}`,
    unavailableSnapshot.discoveryRevision,
  ).error, 'symlink-input',
  'the device-creation API must not bypass an unavailable platform');
  fs.unlinkSync(path.join(root, 'androidApp', 'src', 'linked'));
  discovery.invalidate();

  fs.symlinkSync('/tmp', path.join(root, 'unrelated-link'));
  assert.equal(revision.compute(root, sourceOptions).available, true,
    'paths outside the versioned app-build allowlist must not become build inputs');
  fs.unlinkSync(path.join(root, 'unrelated-link'));

  const hardlink = path.join(root, 'androidApp', 'src', 'main', 'Hardlinked.kt');
  fs.linkSync(path.join(root, 'androidApp', 'src', 'main', 'App.kt'), hardlink);
  assert.equal(revision.compute(root, sourceOptions).reasonCode, 'hardlink-input');
  fs.unlinkSync(hardlink);

  const customRoot = path.join(root, 'customApp');
  fs.mkdirSync(customRoot);
  fs.writeFileSync(path.join(customRoot, 'Custom.kt'), 'fun custom() = 1\n');
  const customOptions = { profile: 'app-build', appRoots: ['customApp'] };
  const customBefore = revision.compute(root, customOptions);
  fs.writeFileSync(path.join(customRoot, 'Custom.kt'), 'fun custom() = 2\n');
  assert.notEqual(revision.compute(root, customOptions).revision, customBefore.revision,
    'every configured product root must participate in the source fence');
  assert.equal(revision.compute(root, { profile: 'app-build', appRoots: ['orchestrator'] }).reasonCode,
    'invalid-app-root');

  const symlinkRoot = root + '-symlink';
  fs.symlinkSync(root, symlinkRoot, 'dir');
  assert.equal(revision.compute(symlinkRoot, sourceOptions).reasonCode, 'unsafe-project-root',
    'the configured project root itself must never be canonicalized through a symlink');
  fs.unlinkSync(symlinkRoot);

  const bytecodeDirectory = path.join(orchestrator, 'site', '__pycache__');
  fs.mkdirSync(bytecodeDirectory);
  const bytecodeFile = path.join(bytecodeDirectory, 'derived.pyc');
  fs.writeFileSync(bytecodeFile, 'derived-one');
  const taskRun = revision.compute(root, { profile: 'task-run' });
  assert.equal(taskRun.available, true);
  fs.writeFileSync(bytecodeFile, 'derived-two');
  assert.equal(revision.compute(root, { profile: 'task-run' }).revision, taskRun.revision,
    'derived interpreter bytecode must not change a task-run checkpoint');
  fs.writeFileSync(path.join(orchestrator, 'site', 'fixture.js'), 'export const fixture = 2;\n');
  assert.notEqual(revision.compute(root, { profile: 'task-run' }).revision, taskRun.revision);
  const taskRunAfterSite = revision.compute(root, { profile: 'task-run' });
  fs.writeFileSync(path.join(orchestrator, 'launch.md'), '# Updated launch contract\n');
  assert.notEqual(revision.compute(root, { profile: 'task-run' }).revision, taskRunAfterSite.revision,
    'root-level orchestrator rules must participate in task-run checkpoints');
  assert.equal(revision.compute(root, {
    profile: 'task-run',
    taskStem: 'TASK_9007199254740992_unsafe',
  }).reasonCode, 'invalid-task-stem');
  assert.equal(revision.compute(root, { profile: 'unknown' }).reasonCode, 'unknown-profile');
} finally {
  fs.rmSync(root, { recursive: true, force: true });
}
