'use strict';

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var processRunner = require('./app-run-process');
var storage = require('./app-run-storage');
var UDID_RE = /^[A-Fa-f0-9]{8}(?:-[A-Fa-f0-9]{4}){3}-[A-Fa-f0-9]{12}$/;

function displayText(value, fallback, maximum) {
  var text = String(value || '').replace(/[\x00-\x1f\x7f]+/g, ' ').trim();
  text = text || fallback;
  var output = '', bytes = 0;
  for (var character of text) {
    var width = Buffer.byteLength(character, 'utf8');
    if (bytes + width > maximum) break;
    output += character;
    bytes += width;
  }
  return output;
}

function readStableRegular(file, maximum) {
  var before = fs.lstatSync(file);
  if (!before.isFile() || before.isSymbolicLink() || String(before.nlink) !== '1' ||
      before.size < 1 || before.size > maximum) {
    throw new Error('iOS bundle metadata is unsafe');
  }
  var flags = fs.constants.O_RDONLY;
  if (fs.constants.O_NOFOLLOW) flags |= fs.constants.O_NOFOLLOW;
  var fd;
  try {
    fd = fs.openSync(file, flags);
    var opened = fs.fstatSync(fd);
    if (!opened.isFile() || String(opened.nlink) !== '1' ||
        String(opened.dev) !== String(before.dev) || String(opened.ino) !== String(before.ino) ||
        opened.size !== before.size || Number(opened.mtimeMs) !== Number(before.mtimeMs) ||
        Number(opened.ctimeMs) !== Number(before.ctimeMs)) {
      throw new Error('iOS bundle metadata changed before reading');
    }
    var bytes = Buffer.alloc(opened.size), offset = 0;
    while (offset < bytes.length) {
      var count = fs.readSync(fd, bytes, offset, bytes.length - offset, offset);
      if (!count) throw new Error('iOS bundle metadata changed while reading');
      offset += count;
    }
    var final = fs.fstatSync(fd);
    if (String(final.dev) !== String(opened.dev) || String(final.ino) !== String(opened.ino) ||
        String(final.nlink) !== '1' || final.size !== opened.size ||
        Number(final.mtimeMs) !== Number(opened.mtimeMs) ||
        Number(final.ctimeMs) !== Number(opened.ctimeMs)) {
      throw new Error('iOS bundle metadata changed while reading');
    }
    return bytes;
  } finally {
    if (fd !== undefined) fs.closeSync(fd);
  }
}

function exactAvailable(value) {
  if (!value || typeof value !== 'object') return false;
  if (Object.prototype.hasOwnProperty.call(value, 'isAvailable')) return value.isAvailable === true;
  if (Object.prototype.hasOwnProperty.call(value, 'availability')) return value.availability === '(available)';
  return false;
}

function parseSimctlJson(text) {
  var parsed = JSON.parse(String(text || ''));
  if (!parsed || !parsed.devices || !Array.isArray(parsed.runtimes) || !Array.isArray(parsed.devicetypes)) {
    throw new Error('simctl list JSON is incomplete');
  }
  if (parsed.runtimes.length > 500 || parsed.devicetypes.length > 500) {
    throw new Error('simctl runtime or device type list exceeds its limit');
  }
  var deviceRuntimeKeys = Object.keys(parsed.devices).sort();
  if (deviceRuntimeKeys.length > 500) throw new Error('simctl device runtime list exceeds its limit');
  var runtimes = parsed.runtimes.filter(function (runtime) {
    return exactAvailable(runtime) && typeof runtime.identifier === 'string' &&
      /^com\.apple\.CoreSimulator\.SimRuntime\.iOS-[A-Za-z0-9.-]{1,120}$/
        .test(runtime.identifier);
  }).map(function (runtime) {
    return {
      identifier: runtime.identifier,
      name: displayText(runtime.name, runtime.identifier, 120),
      version: displayText(runtime.version, '', 40)
    };
  });
  if (runtimes.length > 100) throw new Error('simctl available runtime list exceeds its limit');
  var runtimeMap = Object.create(null);
  runtimes.sort(function (a, b) { return a.identifier < b.identifier ? -1 : a.identifier > b.identifier ? 1 : 0; });
  runtimes.forEach(function (runtime) {
    if (runtimeMap[runtime.identifier]) throw new Error('simctl runtime identity is duplicated');
    runtimeMap[runtime.identifier] = runtime;
  });
  var types = parsed.devicetypes.filter(function (type) {
    return type && typeof type.identifier === 'string' && typeof type.name === 'string' &&
      /^com\.apple\.CoreSimulator\.SimDeviceType\.[A-Za-z0-9.-]{1,120}$/
        .test(type.identifier);
  }).map(function (type) {
    return { identifier: type.identifier, name: displayText(type.name, 'iOS Simulator', 120) };
  });
  if (types.length > 100) throw new Error('simctl available device type list exceeds its limit');
  var typeIds = Object.create(null);
  types.sort(function (a, b) { return a.identifier < b.identifier ? -1 : a.identifier > b.identifier ? 1 : 0; });
  types.forEach(function (type) {
    if (typeIds[type.identifier]) throw new Error('simctl device type identity is duplicated');
    typeIds[type.identifier] = true;
  });
  var devices = [], deviceIds = Object.create(null);
  deviceRuntimeKeys.forEach(function (runtimeId) {
    if (!runtimeMap[runtimeId] || !Array.isArray(parsed.devices[runtimeId])) return;
    if (parsed.devices[runtimeId].length > 500) {
      throw new Error('simctl devices for one runtime exceed their limit');
    }
    parsed.devices[runtimeId].forEach(function (device) {
      if (!exactAvailable(device) || !device || !UDID_RE.test(String(device.udid || ''))) return;
      if (device.state !== 'Booted' && device.state !== 'Shutdown') return;
      if (devices.length >= 200) throw new Error('simctl available device list exceeds its limit');
      if (deviceIds[device.udid.toLowerCase()]) throw new Error('simctl device identity is duplicated');
      deviceIds[device.udid.toLowerCase()] = true;
      devices.push({
        rawIdentifier: device.udid,
        runtimeId: runtimeId,
        deviceTypeId: /^com\.apple\.CoreSimulator\.SimDeviceType\.[A-Za-z0-9.-]{1,120}$/
          .test(String(device.deviceTypeIdentifier || ''))
          ? device.deviceTypeIdentifier : null,
        stableMaterial: 'ios:' + device.udid,
        displayName: displayText(device.name, 'iOS Simulator', 120),
        state: device.state === 'Booted' ? 'running' : 'stopped',
        osVersion: runtimeMap[runtimeId].version,
        model: displayText(device.name, 'iOS Simulator', 120),
        architecture: null,
        managedByOrchestrator: /^Orchestrator /.test(String(device.name || '')),
        capabilities: device.state === 'Booted'
          ? ['install', 'launch', 'stop', 'screenshot'] : ['start']
      });
    });
  });
  devices.sort(function (a, b) {
    if (a.state !== b.state) return a.state === 'running' ? -1 : 1;
    if (a.displayName !== b.displayName) return a.displayName < b.displayName ? -1 : 1;
    return a.rawIdentifier < b.rawIdentifier ? -1 : a.rawIdentifier > b.rawIdentifier ? 1 : 0;
  });
  return { runtimes: runtimes, deviceTypes: types, devices: devices };
}

function resolveTools() {
  return {
    xcrun: processRunner.resolveExecutable('xcrun', ['/usr/bin/xcrun']),
    xcodebuild: processRunner.resolveExecutable('xcodebuild', ['/usr/bin/xcodebuild']),
    open: processRunner.resolveExecutable('open', ['/usr/bin/open']),
    plutil: processRunner.resolveExecutable('plutil', ['/usr/bin/plutil'])
  };
}

function discover(options) {
  options = options || {};
  var runner = options.commandRunner || processRunner;
  var config = options.config;
  var tools = options.tools || resolveTools();
  if (process.platform !== 'darwin' && !options.forcePlatform) {
    return {
      platform: 'ios', availability: 'unavailable', reasonCode: 'ios-requires-macos',
      toolchain: { xcrun: false, xcodebuild: false }, devices: [], profiles: [], warnings: [],
      private: { tools: tools }
    };
  }
  if (!config.project.iosEnabled) {
    return {
      platform: 'ios', availability: 'unavailable', reasonCode: 'ios-disabled',
      toolchain: { xcrun: !!tools.xcrun, xcodebuild: !!tools.xcodebuild },
      devices: [], profiles: [], warnings: [], private: { tools: tools }
    };
  }
  if (!tools.xcrun || !tools.xcodebuild || !tools.plutil) {
    return {
      platform: 'ios', availability: 'unavailable', reasonCode: 'xcode-missing',
      toolchain: { xcrun: !!tools.xcrun, xcodebuild: !!tools.xcodebuild, plutil: !!tools.plutil },
      devices: [], profiles: [], warnings: [], private: { tools: tools }
    };
  }
  var project = config.manifest.ios && path.join(paths.PROJECT_ROOT, config.manifest.ios.project);
  var projectExists = false;
  try {
    var projectStat = fs.lstatSync(project);
    projectExists = projectStat.isDirectory() && !projectStat.isSymbolicLink();
  } catch (_) {}
  var result;
  try {
    result = runner.runSync({
      executable: tools.xcrun, argv: ['simctl', 'list', '--json'],
      cwd: paths.PROJECT_ROOT, timeoutMs: 30000
    });
  } catch (error) {
    result = { ok: false, stdout: '', stderr: '', errorCode: String(error && error.code || 'invocation-invalid') };
  }
  var parsed = { runtimes: [], deviceTypes: [], devices: [] }, reason = null, warnings = [];
  if (!result.ok) reason = 'xcode-setup-incomplete';
  else {
    try { parsed = parseSimctlJson(result.stdout); }
    catch (_) { reason = 'simctl-output-invalid'; }
  }
  if (!reason && !parsed.runtimes.length) reason = 'no-ios-runtime';
  if (!reason && !projectExists) reason = 'ios-project-missing';
  var profiles = [];
  parsed.deviceTypes.slice(0, 30).forEach(function (type) {
    parsed.runtimes.slice(0, 30).forEach(function (runtime) {
      profiles.push({
        rawProfileId: type.identifier,
        rawRuntimeId: runtime.identifier,
        stableMaterial: 'ios-profile:' + type.identifier + ':' + runtime.identifier,
        displayName: type.name,
        runtimeName: runtime.name,
        osVersion: runtime.version,
        architecture: null,
        estimatedBytes: 0
      });
    });
  });
  if (profiles.length > 200) warnings.push('creation-profiles-partial');
  return {
    platform: 'ios',
    availability: reason ? (projectExists ? 'partial' : 'unavailable') : 'available',
    reasonCode: reason,
    toolchain: { xcrun: true, xcodebuild: true, plutil: true },
    devices: parsed.devices,
    profiles: profiles.slice(0, 200),
    warnings: warnings,
    private: { tools: tools }
  };
}

async function ensureStarted(context, target) {
  if (target.state !== 'running') {
    var boot = await context.commandRunner.run({
      executable: context.tools.xcrun, argv: ['simctl', 'boot', target.rawIdentifier],
      cwd: paths.PROJECT_ROOT, timeoutMs: 60000, signal: context.signal, onLine: context.onLine
    });
    if (!boot.ok && !/Unable to boot device in current state: Booted/i.test(boot.stderr)) {
      var error = new Error('iOS Simulator could not be booted'); error.code = 'ios-boot-failed'; throw error;
    }
  }
  var status = await context.commandRunner.run({
    executable: context.tools.xcrun, argv: ['simctl', 'bootstatus', target.rawIdentifier, '-b'],
    cwd: paths.PROJECT_ROOT, timeoutMs: 180000, signal: context.signal, onLine: context.onLine
  });
  if (!status.ok) { var timeout = new Error('iOS Simulator did not finish starting'); timeout.code = 'ios-boot-timeout'; throw timeout; }
  if (context.tools.open) {
    context.commandRunner.run({
      executable: context.tools.open, argv: ['-a', 'Simulator'], cwd: paths.PROJECT_ROOT, timeoutMs: 15000
    }).catch(function () {});
  }
  return target.rawIdentifier;
}

// §26: the product root is always PROVEN by the caller. A missing one is a
// typed refusal, never a silent fall back to the control root — that would
// build or read the shared tree and label the result with a task's identity.
function assertProductRoot(context) {
  if (!context || typeof context.executionRoot !== 'string' || context.executionRoot === '') {
    var error = new Error('the job carries no proven product root');
    error.code = 'artifact-invalid';
    throw error;
  }
}

function derivedDataPath(context) {
  // Task-local by construction (§14): the same scope the artifact expectation
  // allows, so a build and its verification can never disagree about where the
  // product was produced.
  var scope = context.runConfigHash.replace(/^sha256:/, '').slice(0, 24);
  var directory = path.join(paths.APP_RUN_DIR, 'derived-data',
    context.worktreeId ? scope + '-' + String(context.worktreeId).slice(-12) : scope);
  storage.ensureDirectory(directory);
  return directory;
}

async function build(context, variant) {
  assertProductRoot(context);
  var derived = derivedDataPath(context);
  var result = await context.commandRunner.run({
    executable: context.tools.xcodebuild,
    argv: [
      'build', '-project', path.join(context.executionRoot, variant.project),
      '-scheme', variant.scheme, '-configuration', variant.configuration,
      '-sdk', 'iphonesimulator', '-destination', 'generic/platform=iOS Simulator',
      '-derivedDataPath', derived, 'CODE_SIGNING_ALLOWED=NO'
    ],
    // The product is compiled where the product IS.
    cwd: context.executionRoot, timeoutMs: 30 * 60 * 1000,
    signal: context.signal, onLine: context.onLine,
    beforeSpawn: context.beforeBuildSpawn,
    onSpawn: context.onBuildSpawn
  });
  if (!result.ok) { var error = new Error('iOS app build failed'); error.code = 'build-failed'; error.result = result; throw error; }
  return derived;
}

function hashAppTree(root) {
  var rootReal = fs.realpathSync(root), rootInitial = fs.lstatSync(rootReal);
  var pending = [rootReal], entries = [], directoryEntries = [];
  var total = 0, files = 0, directories = 0;
  while (pending.length) {
    var directory = pending.pop();
    directories++;
    if (directories > 20000) throw new Error('iOS app directory count exceeds scan limits');
    if (fs.realpathSync(directory) !== directory) {
      throw new Error('iOS app directory ancestor changed while hashing');
    }
    var directoryBefore = fs.lstatSync(directory);
    if (!directoryBefore.isDirectory() || directoryBefore.isSymbolicLink()) {
      throw new Error('iOS app contains an unsafe directory');
    }
    var names = fs.readdirSync(directory).sort();
    if (names.length > 10000) throw new Error('iOS app directory entry limit exceeded');
    names.forEach(function (name) {
      var file = path.join(directory, name);
      var rel = path.relative(rootReal, file).split(path.sep).join('/');
      var stat = fs.lstatSync(file);
      if (stat.isSymbolicLink()) throw new Error('iOS app contains a symlink');
      if (fs.realpathSync(file) !== file) throw new Error('iOS app entry ancestor changed while hashing');
      if (stat.isDirectory()) { pending.push(file); return; }
      if (!stat.isFile() || String(stat.nlink) !== '1') throw new Error('iOS app contains an unsafe file');
      files++; total += stat.size;
      if (files > 20000 || total > 512 * 1024 * 1024) throw new Error('iOS app tree exceeds scan limits');
      entries.push({
        file: file, rel: rel.normalize('NFC'),
        mode: stat.mode, size: stat.size, stat: stat
      });
    });
    var directoryAfter = fs.lstatSync(directory);
    if (String(directoryBefore.dev) !== String(directoryAfter.dev) ||
        String(directoryBefore.ino) !== String(directoryAfter.ino) ||
        Number(directoryBefore.mtimeMs) !== Number(directoryAfter.mtimeMs) ||
        Number(directoryBefore.ctimeMs) !== Number(directoryAfter.ctimeMs)) {
      throw new Error('iOS app directory changed while hashing');
    }
    directoryEntries.push({ file: directory, stat: directoryAfter });
  }
  entries.sort(function (a, b) { return a.rel < b.rel ? -1 : a.rel > b.rel ? 1 : 0; });
  for (var duplicate = 1; duplicate < entries.length; duplicate++) {
    if (entries[duplicate - 1].rel === entries[duplicate].rel) {
      throw new Error('iOS app contains colliding normalized paths');
    }
  }
  var hash = crypto.createHash('sha256');
  entries.forEach(function (entry) {
    var flags = fs.constants.O_RDONLY;
    if (fs.constants.O_NOFOLLOW) flags |= fs.constants.O_NOFOLLOW;
    var fd;
    try {
      fd = fs.openSync(entry.file, flags);
      var opened = fs.fstatSync(fd);
      if (!opened.isFile() || String(opened.nlink) !== '1' ||
          String(opened.dev) !== String(entry.stat.dev) || String(opened.ino) !== String(entry.stat.ino) ||
          opened.size !== entry.stat.size || Number(opened.mtimeMs) !== Number(entry.stat.mtimeMs) ||
          Number(opened.ctimeMs) !== Number(entry.stat.ctimeMs)) {
        throw new Error('iOS app file changed before hashing');
      }
      var content = crypto.createHash('sha256');
      var buffer = Buffer.alloc(Math.min(1024 * 1024, Math.max(1, opened.size)));
      var offset = 0;
      while (offset < opened.size) {
        var count = fs.readSync(fd, buffer, 0, Math.min(buffer.length, opened.size - offset), offset);
        if (!count) throw new Error('iOS app file changed while hashing');
        content.update(buffer.subarray(0, count));
        offset += count;
      }
      var final = fs.fstatSync(fd);
      if (String(final.dev) !== String(opened.dev) || String(final.ino) !== String(opened.ino) ||
          final.size !== opened.size || Number(final.mtimeMs) !== Number(opened.mtimeMs) ||
          Number(final.ctimeMs) !== Number(opened.ctimeMs)) {
        throw new Error('iOS app file changed while hashing');
      }
    } finally {
      if (fd !== undefined) fs.closeSync(fd);
    }
    hash.update(entry.rel + '\0' + ((entry.mode & 0o111) ? '1' : '0') + '\0' +
      content.digest('hex') + '\0');
  });
  entries.forEach(function (entry) {
    var finalFile = fs.lstatSync(entry.file);
    if (fs.realpathSync(entry.file) !== entry.file ||
        !finalFile.isFile() || finalFile.isSymbolicLink() ||
        String(finalFile.nlink) !== '1' ||
        String(finalFile.dev) !== String(entry.stat.dev) ||
        String(finalFile.ino) !== String(entry.stat.ino) ||
        finalFile.size !== entry.stat.size ||
        Number(finalFile.mtimeMs) !== Number(entry.stat.mtimeMs) ||
        Number(finalFile.ctimeMs) !== Number(entry.stat.ctimeMs)) {
      throw new Error('iOS app file changed after hashing');
    }
  });
  directoryEntries.forEach(function (entry) {
    var finalDirectory = fs.lstatSync(entry.file);
    if (fs.realpathSync(entry.file) !== entry.file ||
        !finalDirectory.isDirectory() || finalDirectory.isSymbolicLink() ||
        String(finalDirectory.dev) !== String(entry.stat.dev) ||
        String(finalDirectory.ino) !== String(entry.stat.ino) ||
        Number(finalDirectory.mtimeMs) !== Number(entry.stat.mtimeMs) ||
        Number(finalDirectory.ctimeMs) !== Number(entry.stat.ctimeMs)) {
      throw new Error('iOS app directory changed after hashing');
    }
  });
  var rootFinal = fs.lstatSync(rootReal);
  if (String(rootFinal.dev) !== String(rootInitial.dev) || String(rootFinal.ino) !== String(rootInitial.ino) ||
      Number(rootFinal.mtimeMs) !== Number(rootInitial.mtimeMs) ||
      Number(rootFinal.ctimeMs) !== Number(rootInitial.ctimeMs)) {
    throw new Error('iOS app root changed while hashing');
  }
  return { hash: 'sha256:' + hash.digest('hex'), size: total };
}

function resolveArtifact(context, variant, derived) {
  derived = derived || derivedDataPath(context);
  var root = path.join(derived, 'Build', 'Products', variant.configuration + '-iphonesimulator');
  var productNames = fs.readdirSync(root);
  if (productNames.length > 1000) throw new Error('iOS build product directory exceeds its entry limit');
  var names = productNames.filter(function (name) {
    return /^[A-Za-z0-9._ -]{1,160}\.app$/.test(name);
  }).sort();
  if (names.length !== 1) throw new Error('expected exactly one iOS .app product');
  var app = path.join(root, names[0]);
  var stat = fs.lstatSync(app);
  if (!stat.isDirectory() || stat.isSymbolicLink()) throw new Error('iOS app product is unsafe');
  var plist = path.join(app, 'Info.plist');
  // Reject a symlinked/unsafe bundle before any platform tool is allowed to
  // read from it. The resulting tree proof is reused as the artifact identity.
  var tree = hashAppTree(app);
  var plistBytes = readStableRegular(plist, 4 * 1024 * 1024);
  var result = context.commandRunner.runSync({
    executable: context.tools.plutil,
    argv: ['-extract', 'CFBundleIdentifier', 'raw', '-o', '-', '-'],
    cwd: paths.PROJECT_ROOT, timeoutMs: 30000, input: plistBytes
  });
  if (!result.ok || result.stdout.trim() !== context.applicationId) {
    var mismatch = new Error('iOS bundle identifier does not match project config'); mismatch.code = 'artifact-invalid'; throw mismatch;
  }
  return {
    path: app, buildRoot: root, hash: tree.hash, size: tree.size,
    applicationId: context.applicationId, targetArchitectures: ['iphonesimulator']
  };
}

async function install(context, udid, artifact) {
  var result = await context.commandRunner.run({
    executable: context.tools.xcrun, argv: ['simctl', 'install', udid, artifact.path],
    cwd: paths.PROJECT_ROOT, timeoutMs: 180000, signal: context.signal, onLine: context.onLine
  });
  if (!result.ok) { var error = new Error('iOS app install failed'); error.code = 'install-failed'; throw error; }
  var verify = await context.commandRunner.run({
    executable: context.tools.xcrun,
    argv: ['simctl', 'get_app_container', udid, context.applicationId, 'app'],
    cwd: paths.PROJECT_ROOT, timeoutMs: 30000, signal: context.signal, onLine: context.onLine
  });
  if (!verify.ok || !String(verify.stdout || '').trim()) {
    var missing = new Error('iOS app container was not observed after installation');
    missing.code = 'install-failed';
    throw missing;
  }
}

async function launch(context, udid) {
  var result = await context.commandRunner.run({
    executable: context.tools.xcrun, argv: ['simctl', 'launch', udid, context.applicationId],
    cwd: paths.PROJECT_ROOT, timeoutMs: 30000, signal: context.signal, onLine: context.onLine
  });
  if (!result.ok) { var error = new Error('iOS app launch failed'); error.code = 'launch-failed'; throw error; }
  var match = result.stdout.match(/:\s*(\d+)\s*$/m);
  if (!match || !Number.isSafeInteger(Number(match[1])) || Number(match[1]) <= 0) {
    var unverified = new Error('iOS app process was not observed after launch');
    unverified.code = 'launch-failed';
    throw unverified;
  }
  return { pid: Number(match[1]), component: null };
}

async function stop(context, udid) {
  var result = await context.commandRunner.run({
    executable: context.tools.xcrun, argv: ['simctl', 'terminate', udid, context.applicationId],
    cwd: paths.PROJECT_ROOT, timeoutMs: 30000, signal: context.signal, onLine: context.onLine
  });
  if (!result.ok && !/not running/i.test(result.stderr)) return result;
  return Object.assign({}, result, { ok: true });
}

async function screenshot(context, udid, output) {
  return context.commandRunner.run({
    executable: context.tools.xcrun, argv: ['simctl', 'io', udid, 'screenshot', output],
    cwd: paths.PROJECT_ROOT, timeoutMs: 30000, signal: context.signal, onLine: context.onLine
  });
}

async function createDevice(context, preview) {
  var result = await context.commandRunner.run({
    executable: context.tools.xcrun,
    argv: ['simctl', 'create', preview.generatedName, preview.rawProfileId, preview.rawRuntimeId],
    cwd: paths.PROJECT_ROOT, timeoutMs: 60000, signal: context.signal, onLine: context.onLine
  });
  var udid = result.stdout.trim();
  if (!result.ok || !UDID_RE.test(udid)) {
    var error = new Error('iOS Simulator creation failed'); error.code = 'device-create-failed'; throw error;
  }
  var listed = context.commandRunner.runSync({
    executable: context.tools.xcrun,
    argv: ['simctl', 'list', '--json'],
    cwd: paths.PROJECT_ROOT,
    timeoutMs: 30000
  });
  var created = null;
  if (listed.ok) {
    try {
      created = parseSimctlJson(listed.stdout).devices.find(function (device) {
        return device.rawIdentifier.toLowerCase() === udid.toLowerCase() &&
          device.runtimeId === preview.rawRuntimeId &&
          device.deviceTypeId === preview.rawProfileId;
      });
    } catch (_) {}
  }
  if (!created) {
    var unverified = new Error('iOS Simulator was not observed after creation');
    unverified.code = 'device-create-failed';
    throw unverified;
  }
  return { displayName: preview.generatedName, stableMaterial: 'ios:' + udid };
}

module.exports = {
  parseSimctlJson: parseSimctlJson,
  resolveTools: resolveTools,
  discover: discover,
  ensureStarted: ensureStarted,
  build: build,
  derivedDataPath: derivedDataPath,
  hashAppTree: hashAppTree,
  resolveArtifact: resolveArtifact,
  install: install,
  launch: launch,
  stop: stop,
  screenshot: screenshot,
  createDevice: createDevice
};
