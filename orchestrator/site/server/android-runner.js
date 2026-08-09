'use strict';

var fs = require('fs');
var path = require('path');
var crypto = require('crypto');
var paths = require('./paths');
var processRunner = require('./app-run-process');

var COMMAND_TIMEOUT = 15000;

function displayText(value, fallback, maximum) {
  var text = String(value || '').replace(/[\x00-\x1f\x7f]+/g, ' ').trim();
  if (!text) return fallback;
  var output = '', bytes = 0;
  for (var character of text) {
    var width = Buffer.byteLength(character, 'utf8');
    if (bytes + width > maximum) break;
    output += character;
    bytes += width;
  }
  return output || fallback;
}

function readStableRegular(file, maximum) {
  var before = fs.lstatSync(file);
  if (!before.isFile() || before.isSymbolicLink() || String(before.nlink) !== '1' ||
      before.size > maximum) throw new Error('Android build metadata is unsafe');
  var flags = fs.constants.O_RDONLY;
  if (fs.constants.O_NOFOLLOW) flags |= fs.constants.O_NOFOLLOW;
  var fd;
  try {
    fd = fs.openSync(file, flags);
    var opened = fs.fstatSync(fd);
    if (!opened.isFile() || String(opened.nlink) !== '1' ||
        String(opened.dev) !== String(before.dev) || String(opened.ino) !== String(before.ino) ||
        opened.size !== before.size || Number(opened.mtimeMs) !== Number(before.mtimeMs) ||
        Number(opened.ctimeMs) !== Number(before.ctimeMs)) throw new Error('Android build metadata changed');
    var bytes = Buffer.alloc(opened.size), offset = 0;
    while (offset < bytes.length) {
      var count = fs.readSync(fd, bytes, offset, bytes.length - offset, offset);
      if (!count) throw new Error('Android build metadata changed');
      offset += count;
    }
    var final = fs.fstatSync(fd);
    if (String(final.dev) !== String(opened.dev) || String(final.ino) !== String(opened.ino) ||
        final.size !== opened.size || Number(final.mtimeMs) !== Number(opened.mtimeMs) ||
        Number(final.ctimeMs) !== Number(opened.ctimeMs)) throw new Error('Android build metadata changed');
    return bytes;
  } finally {
    if (fd !== undefined) fs.closeSync(fd);
  }
}

function existingDirectories(rows) {
  var seen = Object.create(null), out = [];
  rows.forEach(function (row) {
    if (!row || !path.isAbsolute(row)) return;
    try {
      var real = fs.realpathSync(row);
      var stat = fs.lstatSync(real);
      if (stat.isDirectory() && !stat.isSymbolicLink() && !seen[real]) {
        seen[real] = true; out.push(real);
      }
    } catch (_) {}
  });
  return out;
}

function canonicalDirectory(value) {
  if (!value || typeof value !== 'string' || !path.isAbsolute(value)) return null;
  try {
    var real = fs.realpathSync(value);
    var stat = fs.lstatSync(real);
    return stat.isDirectory() && !stat.isSymbolicLink() ? real : null;
  } catch (_) { return null; }
}

function decodedProperty(value) {
  var output = '';
  for (var i = 0; i < value.length; i++) {
    if (value[i] !== '\\') {
      output += value[i];
      continue;
    }
    i++;
    if (i >= value.length || value[i] === 'u' || /[nrtf]/.test(value[i])) return null;
    output += value[i];
  }
  return output;
}

function projectSdkRoot() {
  var file = path.join(paths.PROJECT_ROOT, 'local.properties');
  var text;
  try { text = readStableRegular(file, 64 * 1024).toString('utf8'); }
  catch (error) {
    if (error && error.code === 'ENOENT') return { present: false, root: null, error: null };
    return { present: true, root: null, error: 'android-sdk-invalid' };
  }
  var values = [], malformed = false;
  text.replace(/^\uFEFF/, '').replace(/\r\n?/g, '\n').split('\n').forEach(function (line) {
    if (/^[ \t]*[#!]/.test(line) || /^[ \t]*$/.test(line)) return;
    var match = /^[ \t]*sdk\.dir[ \t]*[:=][ \t]*(.*)$/.exec(line);
    if (match) values.push(match[1].trim());
    else if (/^[ \t]*sdk\.dir(?:[ \t:=]|$)/.test(line)) malformed = true;
  });
  if (malformed) return { present: true, root: null, error: 'android-sdk-invalid' };
  if (!values.length) return { present: false, root: null, error: null };
  if (values.length !== 1) return { present: true, root: null, error: 'android-sdk-invalid' };
  var decoded = decodedProperty(values[0]);
  var root = decoded && canonicalDirectory(decoded);
  return root
    ? { present: true, root: root, error: null }
    : { present: true, root: null, error: 'android-sdk-invalid' };
}

function sdkRoots() {
  var home = process.env.HOME || '';
  var configuredValues = [
    process.env.ANDROID_SDK_ROOT,
    process.env.ANDROID_HOME
  ].filter(function (value) { return typeof value === 'string' && value.length > 0; });
  var configured = existingDirectories(configuredValues);
  if (configuredValues.some(function (value) { return canonicalDirectory(value) === null; })) {
    return { roots: [], error: 'android-sdk-invalid' };
  }
  if (configured.length > 1) return { roots: [], error: 'android-sdk-ambiguous' };
  if (configured.length === 1) return { roots: configured, error: null };
  var projectConfigured = projectSdkRoot();
  if (projectConfigured.error) return { roots: [], error: projectConfigured.error };
  if (projectConfigured.present) return { roots: [projectConfigured.root], error: null };
  var standard = existingDirectories([
    home && path.join(home, 'Library', 'Android', 'sdk'),
    home && path.join(home, 'Android', 'Sdk'),
    process.platform === 'win32' && process.env.LOCALAPPDATA
      ? path.join(process.env.LOCALAPPDATA, 'Android', 'Sdk') : null
  ]);
  if (standard.length > 1) return { roots: [], error: 'android-sdk-ambiguous' };
  return { roots: standard, error: null };
}

function versionedCandidates(root, group, executable) {
  var parent = path.join(root, group), rows = [];
  try {
    fs.readdirSync(parent).filter(function (name) { return /^[A-Za-z0-9._-]{1,80}$/.test(name); })
      .sort(function (left, right) {
        if (left === 'latest' || right === 'latest') return left === 'latest' ? -1 : 1;
        var a = left.split(/[._-]/), b = right.split(/[._-]/);
        for (var i = 0; i < Math.max(a.length, b.length); i++) {
          if (a[i] === b[i]) continue;
          var an = /^(?:0|[1-9]\d*)$/.test(a[i] || '') ? BigInt(a[i]) : null;
          var bn = /^(?:0|[1-9]\d*)$/.test(b[i] || '') ? BigInt(b[i]) : null;
          if (an !== null && bn !== null && an !== bn) return an > bn ? -1 : 1;
          return String(a[i] || '') < String(b[i] || '') ? 1 : -1;
        }
        return left < right ? 1 : left > right ? -1 : 0;
      }).slice(0, 50).forEach(function (name) {
        rows.push(path.join(parent, name, 'bin', executable));
        rows.push(path.join(parent, name, executable));
      });
  } catch (_) {}
  return rows;
}

function resolveTools() {
  var sdk = sdkRoots();
  var roots = sdk.roots;
  var exe = process.platform === 'win32' ? '.exe' : '';
  var adb = [], emulator = [], avdmanager = [], sdkmanager = [], apkanalyzer = [], aapt = [];
  roots.forEach(function (root) {
    adb.push(path.join(root, 'platform-tools', 'adb' + exe));
    emulator.push(path.join(root, 'emulator', 'emulator' + exe));
    avdmanager = avdmanager.concat(versionedCandidates(root, 'cmdline-tools', 'avdmanager' + (process.platform === 'win32' ? '.bat' : '')));
    sdkmanager = sdkmanager.concat(versionedCandidates(root, 'cmdline-tools', 'sdkmanager' + (process.platform === 'win32' ? '.bat' : '')));
    apkanalyzer = apkanalyzer.concat(versionedCandidates(root, 'cmdline-tools', 'apkanalyzer' + (process.platform === 'win32' ? '.bat' : '')));
    aapt = aapt.concat(versionedCandidates(root, 'build-tools', 'aapt' + exe));
  });
  var gradlew = process.platform === 'win32'
    ? path.join(paths.PROJECT_ROOT, 'gradlew.bat') : path.join(paths.PROJECT_ROOT, 'gradlew');
  return {
    sdkRoot: roots[0] || null,
    resolutionError: sdk.error,
    adb: processRunner.resolveExecutable('adb' + exe, adb),
    emulator: processRunner.resolveExecutable('emulator' + exe, emulator),
    avdmanager: process.platform === 'win32' ? null : processRunner.resolveExecutable('avdmanager', avdmanager),
    sdkmanager: process.platform === 'win32' ? null : processRunner.resolveExecutable('sdkmanager', sdkmanager),
    apkanalyzer: process.platform === 'win32' ? null : processRunner.resolveExecutable('apkanalyzer', apkanalyzer),
    aapt: processRunner.resolveExecutable('aapt' + exe, aapt),
    gradlew: process.platform === 'win32' ? null : processRunner.resolveExecutable(gradlew, [gradlew])
  };
}

function parseAdbDevices(text) {
  var devices = [], malformed = 0, seen = Object.create(null);
  String(text || '').replace(/\r\n?/g, '\n').split('\n').forEach(function (line) {
    line = line.trim();
    if (!line || /^List of devices attached/.test(line) || /^\*/.test(line)) return;
    if (Buffer.byteLength(line, 'utf8') > 1000) { malformed++; return; }
    var match = /^([^\s]{1,200})\s+(device|offline|unauthorized)(?:\s+(.*))?$/.exec(line);
    if (!match) { malformed++; return; }
    if (seen[match[1]]) { malformed++; return; }
    seen[match[1]] = true;
    var attrs = Object.create(null);
    String(match[3] || '').split(/\s+/).forEach(function (pair) {
      var idx = pair.indexOf(':');
      var key = idx > 0 ? pair.slice(0, idx) : '';
      var value = idx > 0 ? pair.slice(idx + 1) : '';
      if (/^[A-Za-z0-9._-]{1,80}$/.test(key) && Buffer.byteLength(value, 'utf8') <= 240) {
        attrs[key] = value;
      }
    });
    if (devices.length >= 200) { malformed++; return; }
    devices.push({
      serial: match[1],
      state: match[2],
      emulator: /^emulator-\d+$/.test(match[1]),
      model: attrs.model || null,
      product: attrs.product || null,
      transportId: attrs.transport_id || null
    });
  });
  return { devices: devices, malformed: malformed };
}

function parseAvdList(text) {
  var seen = Object.create(null), rows = [], malformed = 0;
  String(text || '').replace(/\r\n?/g, '\n').split('\n').forEach(function (line) {
    var name = line.trim();
    if (!name) return;
    if (!/^[A-Za-z0-9._-]{1,120}$/.test(name)) { malformed++; return; }
    if (seen[name]) { malformed++; return; }
    if (rows.length >= 200) { malformed++; return; }
    seen[name] = true;
    rows.push(name);
  });
  return { names: rows.sort(), malformed: malformed };
}

function parseAvdmanagerDevices(text) {
  var rows = [], current = null, seen = Object.create(null), malformed = 0;
  String(text || '').replace(/\r\n?/g, '\n').split('\n').forEach(function (line) {
    var idLine = /^\s*id:/.test(line);
    var id = line.match(/^\s*id:\s*\d+\s+or\s+"([^"]+)"\s*$/);
    if (id && /^[A-Za-z0-9._-]{1,100}$/.test(id[1])) {
      if (seen[id[1]]) { malformed++; current = null; return; }
      seen[id[1]] = true;
      if (rows.length >= 100) { malformed++; current = null; return; }
      current = { deviceId: id[1], name: id[1] }; rows.push(current); return;
    }
    if (idLine) { malformed++; current = null; return; }
    var name = current && line.match(/^\s*Name:\s*(.{1,120})\s*$/);
    if (name) current.name = displayText(name[1], current.name, 120);
  });
  Object.defineProperty(rows, 'malformed', { value: malformed, enumerable: false });
  return rows;
}

function safeDirectoryUnder(root, candidate) {
  try {
    var stat = fs.lstatSync(candidate);
    if (!stat.isDirectory() || stat.isSymbolicLink()) return false;
    var real = fs.realpathSync(candidate);
    var rel = path.relative(root, real);
    return real === candidate && rel !== '..' && rel.indexOf('..' + path.sep) !== 0 &&
      !path.isAbsolute(rel);
  } catch (_) { return false; }
}

function installedImages(root, warnings) {
  var rows = [];
  if (!root) return rows;
  var base = path.join(root, 'system-images');
  if (!safeDirectoryUnder(root, base)) return rows;
  function limitedNames(directory) {
    var names = fs.readdirSync(directory).sort();
    if (names.length > 50 && warnings.indexOf('system-images-partial') < 0) {
      warnings.push('system-images-partial');
    }
    return names.slice(0, 50);
  }
  try {
    limitedNames(base).forEach(function (api) {
      if (!/^android-\d{1,3}$/.test(api)) return;
      var apiDir = path.join(base, api);
      if (!safeDirectoryUnder(root, apiDir)) return;
      limitedNames(apiDir).forEach(function (vendor) {
        if (!/^[A-Za-z0-9._-]{1,80}$/.test(vendor)) return;
        var vendorDir = path.join(apiDir, vendor);
        if (!safeDirectoryUnder(root, vendorDir)) return;
        limitedNames(vendorDir).forEach(function (arch) {
          if (!/^[A-Za-z0-9._-]{1,80}$/.test(arch)) return;
          var imageDir = path.join(vendorDir, arch);
          if (safeDirectoryUnder(root, imageDir)) {
            rows.push({
              packageId: ['system-images', api, vendor, arch].join(';'),
              label: api + ' · ' + vendor + ' · ' + arch,
              osVersion: api.replace('android-', ''),
              architecture: arch,
              estimatedBytes: 0
            });
          }
        });
      });
    });
  } catch (_) {}
  return rows.slice(0, 100);
}

function sync(runner, executable, argv) {
  try {
    return runner.runSync({
      executable: executable, argv: argv, cwd: paths.PROJECT_ROOT,
      timeoutMs: COMMAND_TIMEOUT
    });
  } catch (error) {
    return {
      ok: false, status: null, signal: null, timedOut: false,
      stdout: '', stderr: '', errorCode: String(error && error.code || 'invocation-invalid')
    };
  }
}

function avdNameFromOutput(value) {
  var first = String(value || '').replace(/\r\n?/g, '\n').split('\n')[0].trim();
  return /^[A-Za-z0-9._-]{1,120}$/.test(first) ? first : null;
}

function resolveAvdName(runner, adb, serial) {
  if (!runner || !adb || !/^emulator-\d+$/.test(String(serial || ''))) return null;
  var consoleName = sync(runner, adb, ['-s', serial, 'emu', 'avd', 'name']);
  var name = consoleName.ok ? avdNameFromOutput(consoleName.stdout) : null;
  if (name) return name;
  // Emulator 36.4 can return a successful but empty console response while
  // still publishing the canonical AVD name as a read-only boot property.
  var bootName = sync(runner, adb, [
    '-s', serial, 'shell', 'getprop', 'ro.boot.qemu.avd_name'
  ]);
  return bootName.ok ? avdNameFromOutput(bootName.stdout) : null;
}

function discover(options) {
  options = options || {};
  var runner = options.commandRunner || processRunner;
  var tools = options.tools || resolveTools();
  var warnings = [];
  var devices = [], physicalCount = 0, blockedEmulatorCount = 0;
  var unresolvedEmulatorCount = 0;
  if (tools.adb) {
    var adb = sync(runner, tools.adb, ['devices', '-l']);
    if (adb.ok) {
      var parsed = parseAdbDevices(adb.stdout);
      if (parsed.malformed) warnings.push('adb-output-partial');
      parsed.devices.slice(0, 50).forEach(function (row) {
        if (!row.emulator) { physicalCount++; return; }
        if (row.state !== 'device') {
          blockedEmulatorCount++;
          warnings.push('emulator-' + row.state);
          return;
        }
        var avdName = resolveAvdName(runner, tools.adb, row.serial);
        if (!avdName) unresolvedEmulatorCount++;
        var version = sync(runner, tools.adb, ['-s', row.serial, 'shell', 'getprop', 'ro.build.version.release']);
        var abi = sync(runner, tools.adb, ['-s', row.serial, 'shell', 'getprop', 'ro.product.cpu.abi']);
        var architecture = abi.ok ? abi.stdout.trim() : '';
        devices.push({
          rawIdentifier: row.serial,
          avdName: avdName,
          stableMaterial: 'android:' + (avdName || row.serial),
          displayName: displayText(avdName || row.model, 'Android emulator', 120),
          state: 'running',
          osVersion: version.ok ? displayText(version.stdout, null, 40) : null,
          model: displayText(row.model || avdName, 'Android Emulator', 120),
          architecture: /^[A-Za-z0-9._-]{1,40}$/.test(architecture) ? architecture : null,
          managedByOrchestrator: false,
          capabilities: ['install', 'launch', 'stop', 'screenshot']
        });
      });
    } else warnings.push('adb-devices-unavailable');
  }
  if (tools.emulator) {
    var avds = sync(runner, tools.emulator, ['-list-avds']);
    if (avds.ok) {
      var list = parseAvdList(avds.stdout);
      if (list.malformed) warnings.push('avd-list-partial');
      list.names.forEach(function (name) {
        // An offline/unauthorized emulator cannot be mapped safely back to its
        // AVD through adb. Do not risk launching a second process for any
        // configured AVD until the ambiguous emulator generation is gone.
        if (blockedEmulatorCount > 0 || unresolvedEmulatorCount > 0) return;
        if (devices.some(function (row) { return row.avdName === name; })) return;
        devices.push({
          rawIdentifier: name, avdName: name, stableMaterial: 'android:' + name,
          displayName: name, state: 'stopped', osVersion: null, model: 'Android Emulator',
          architecture: null, managedByOrchestrator: /^orchestrator_/.test(name),
          capabilities: ['start']
        });
      });
    } else warnings.push('avd-list-unavailable');
  }
  var profiles = [];
  if (tools.avdmanager && tools.emulator && tools.sdkRoot) {
    // The verbose form is intentional: the strict parser below consumes the
    // documented `id: N or "..."` / `Name:` records. Compact output has a
    // different one-line shape and must not be guessed as the same contract.
    var rawProfiles = sync(runner, tools.avdmanager, ['list', 'device']);
    var deviceProfiles = rawProfiles.ok ? parseAvdmanagerDevices(rawProfiles.stdout) : [];
    if (!rawProfiles.ok) warnings.push('avdmanager-device-list-unavailable');
    if (deviceProfiles.malformed) warnings.push('avdmanager-device-list-partial');
    var images = installedImages(tools.sdkRoot, warnings);
    if (deviceProfiles.length > 30 || images.length > 30) {
      warnings.push('creation-profiles-partial');
    }
    deviceProfiles.slice(0, 30).forEach(function (profile) {
      images.slice(0, 30).forEach(function (image) {
        profiles.push({
          rawProfileId: profile.deviceId,
          rawRuntimeId: image.packageId,
          stableMaterial: 'android-profile:' + profile.deviceId + ':' + image.packageId,
          displayName: profile.name,
          runtimeName: image.label,
          osVersion: image.osVersion,
          architecture: image.architecture,
          estimatedBytes: image.estimatedBytes
        });
      });
    });
  }
  if (profiles.length > 200) warnings.push('creation-profiles-partial');
  var generated = fs.existsSync(path.join(paths.PROJECT_ROOT, 'androidApp'));
  var missing = [];
  if (!tools.adb) missing.push('adb');
  if (!tools.emulator) missing.push('emulator');
  if (!tools.gradlew) missing.push('gradle-wrapper');
  if (!tools.apkanalyzer && !tools.aapt) missing.push('artifact-inspector');
  var availability = !generated || tools.resolutionError || !tools.adb || !tools.gradlew || (!tools.apkanalyzer && !tools.aapt)
    ? 'unavailable'
    : missing.length ? 'partial' : 'available';
  return {
    platform: 'android',
    availability: availability,
    reasonCode: !generated ? 'android-project-missing'
      : tools.resolutionError || (missing.length ? 'android-toolchain-incomplete' : null),
    toolchain: {
      adb: !!tools.adb, emulator: !!tools.emulator, avdmanager: !!tools.avdmanager,
      gradleWrapper: !!tools.gradlew, artifactInspector: !!(tools.apkanalyzer || tools.aapt)
    },
    devices: devices,
    profiles: profiles.slice(0, 200),
    physicalDeviceCount: physicalCount,
    warnings: warnings,
    private: { tools: tools }
  };
}

function delay(ms) { return new Promise(function (resolve) { setTimeout(resolve, ms); }); }
function cancellableDelay(ms, signal) {
  if (!signal) return delay(ms);
  if (signal.aborted) return Promise.resolve();
  return new Promise(function (resolve) {
    var timer = setTimeout(done, ms);
    function done() {
      clearTimeout(timer);
      signal.removeEventListener('abort', done);
      resolve();
    }
    signal.addEventListener('abort', done, { once: true });
  });
}

async function ensureStarted(context, target) {
  if (target.state === 'running') return target.rawIdentifier;
  var tools = context.tools;
  if (!tools.emulator || !tools.adb || !target.avdName) {
    var missing = new Error('Android emulator cannot be started');
    missing.code = 'adb-unavailable'; throw missing;
  }
  if (!(context.signal && context.signal.aborted)) {
    var existing = readyExistingAvd(context, target);
    if (existing) return existing;
  }
  var identity = context.commandRunner.startDetached({
    executable: tools.emulator,
    argv: ['-avd', target.avdName, '-no-snapshot-save', '-no-boot-anim'],
    cwd: paths.PROJECT_ROOT
  });
  if (context.onProcess) {
    try {
      context.onProcess('emulator', identity);
    } catch (error) {
      if (typeof context.commandRunner.terminateIdentity === 'function') {
        context.commandRunner.terminateIdentity(identity);
      }
      throw error;
    }
  }
  var deadline = Date.now() + 180000;
  while (Date.now() < deadline) {
    if (context.signal && context.signal.aborted) {
      if (typeof context.commandRunner.terminateIdentity === 'function') {
        context.commandRunner.terminateIdentity(identity);
      }
      var cancelled = new Error('Android emulator start was cancelled');
      cancelled.code = 'cancelled';
      throw cancelled;
    }
    var list = context.commandRunner.runSync({ executable: tools.adb, argv: ['devices'], cwd: paths.PROJECT_ROOT, timeoutMs: 10000 });
    if (list.ok) {
      var parsed = parseAdbDevices(list.stdout);
      for (var i = 0; i < parsed.devices.length; i++) {
        var row = parsed.devices[i];
        if (!row.emulator || row.state !== 'device') continue;
        if (resolveAvdName(context.commandRunner, tools.adb, row.serial) !==
            target.avdName) continue;
        var boot = context.commandRunner.runSync({
          executable: tools.adb, argv: ['-s', row.serial, 'shell', 'getprop', 'sys.boot_completed'],
          cwd: paths.PROJECT_ROOT, timeoutMs: 10000
        });
        if (boot.ok && boot.stdout.trim() === '1') return row.serial;
      }
    }
    await cancellableDelay(1500, context.signal);
  }
  if (typeof context.commandRunner.terminateIdentity === 'function') {
    context.commandRunner.terminateIdentity(identity);
  }
  var timeout = new Error('Android emulator did not finish starting');
  timeout.code = 'android-boot-timeout'; throw timeout;
}

function readyExistingAvd(context, target) {
  if (!target || !target.avdName) return null;
  var list = sync(context.commandRunner, context.tools.adb, ['devices']);
  if (!list.ok) return null;
  var parsed = parseAdbDevices(list.stdout);
  for (var index = 0; index < parsed.devices.length; index++) {
    var row = parsed.devices[index];
    if (!row.emulator || row.state !== 'device' ||
        resolveAvdName(context.commandRunner, context.tools.adb, row.serial) !==
          target.avdName) continue;
    var boot = sync(context.commandRunner, context.tools.adb, [
      '-s', row.serial, 'shell', 'getprop', 'sys.boot_completed'
    ]);
    if (boot.ok && boot.stdout.trim() === '1') return row.serial;
  }
  return null;
}

async function build(context, variant, rebuild) {
  var argv = [variant.assembleTask, '--no-daemon', '--stacktrace'];
  if (rebuild) argv.push('--rerun-tasks');
  var result = await context.commandRunner.run({
    executable: context.tools.gradlew, argv: argv, cwd: paths.PROJECT_ROOT,
    timeoutMs: 30 * 60 * 1000, signal: context.signal, onLine: context.onLine,
    beforeSpawn: context.beforeBuildSpawn,
    onSpawn: context.onBuildSpawn
  });
  if (!result.ok) {
    var error = new Error('Android app build failed'); error.code = 'build-failed'; error.result = result; throw error;
  }
}

function regularContained(root, candidate) {
  var sourceStat = fs.lstatSync(candidate);
  if (!sourceStat.isFile() || sourceStat.isSymbolicLink() || String(sourceStat.nlink) !== '1') {
    throw new Error('artifact is unsafe');
  }
  var resolvedRoot = fs.realpathSync(root);
  var resolved = fs.realpathSync(candidate);
  var rel = path.relative(resolvedRoot, resolved);
  if (rel === '..' || rel.indexOf('..' + path.sep) === 0 || path.isAbsolute(rel)) throw new Error('artifact escaped build root');
  var stat = fs.lstatSync(resolved);
  if (!stat.isFile() || stat.isSymbolicLink() || String(stat.nlink) !== '1') throw new Error('artifact is unsafe');
  return { file: resolved, stat: stat };
}

function hashStableRegular(held, maximum) {
  if (held.stat.size < 1 || held.stat.size > maximum) throw new Error('Android artifact exceeds scan limits');
  var flags = fs.constants.O_RDONLY;
  if (fs.constants.O_NOFOLLOW) flags |= fs.constants.O_NOFOLLOW;
  var fd;
  try {
    fd = fs.openSync(held.file, flags);
    var opened = fs.fstatSync(fd);
    if (!opened.isFile() || String(opened.nlink) !== '1' ||
        String(opened.dev) !== String(held.stat.dev) || String(opened.ino) !== String(held.stat.ino) ||
        opened.size !== held.stat.size || Number(opened.mtimeMs) !== Number(held.stat.mtimeMs) ||
        Number(opened.ctimeMs) !== Number(held.stat.ctimeMs)) throw new Error('Android artifact changed before hashing');
    var digest = crypto.createHash('sha256');
    var buffer = Buffer.alloc(1024 * 1024), offset = 0;
    while (offset < opened.size) {
      var count = fs.readSync(fd, buffer, 0, Math.min(buffer.length, opened.size - offset), offset);
      if (!count) throw new Error('Android artifact changed while hashing');
      digest.update(buffer.subarray(0, count));
      offset += count;
    }
    var final = fs.fstatSync(fd);
    if (String(final.dev) !== String(opened.dev) || String(final.ino) !== String(opened.ino) ||
        final.size !== opened.size || Number(final.mtimeMs) !== Number(opened.mtimeMs) ||
        Number(final.ctimeMs) !== Number(opened.ctimeMs)) throw new Error('Android artifact changed while hashing');
    return 'sha256:' + digest.digest('hex');
  } finally {
    if (fd !== undefined) fs.closeSync(fd);
  }
}

function resolveArtifact(context, variant) {
  var outputRoot = path.join(paths.PROJECT_ROOT, variant.module, 'build', 'outputs', 'apk', variant.id);
  var metadataFile = path.join(outputRoot, 'output-metadata.json');
  var metadata = JSON.parse(readStableRegular(metadataFile, 1024 * 1024).toString('utf8'));
  if (!metadata || !Array.isArray(metadata.elements) || metadata.elements.length < 1 || metadata.elements.length > 100) {
    throw new Error('Android output metadata is invalid');
  }
  var candidates = metadata.elements.map(function (element) {
    if (!element || typeof element.outputFile !== 'string' || element.outputFile.indexOf('/') >= 0 ||
        element.outputFile.indexOf('\\') >= 0 || !/^[A-Za-z0-9._-]+\.apk$/.test(element.outputFile) ||
        !Array.isArray(element.filters) || element.filters.length > 20 ||
        element.filters.some(function (filter) {
          return !filter || typeof filter.filterType !== 'string' ||
            !/^[A-Z_]{1,40}$/.test(filter.filterType) ||
            typeof filter.value !== 'string' ||
            !/^[A-Za-z0-9._-]{1,80}$/.test(filter.value);
        })) return null;
    var filters = element.filters;
    var architectures = filters.filter(function (filter) {
      return filter && filter.filterType === 'ABI' && /^[A-Za-z0-9._-]{1,80}$/.test(String(filter.value || ''));
    }).map(function (filter) { return filter.value; });
    return {
      outputFile: element.outputFile,
      universal: filters.length === 0,
      targetArchitectures: architectures
    };
  });
  if (candidates.some(function (candidate) { return candidate === null; })) {
    throw new Error('Android output metadata contains an invalid element');
  }
  var seenOutputs = Object.create(null);
  if (candidates.some(function (candidate) {
    if (seenOutputs[candidate.outputFile]) return true;
    seenOutputs[candidate.outputFile] = true;
    return false;
  })) {
    throw new Error('Android output metadata contains duplicate artifact identities');
  }
  candidates.sort(function (a, b) {
    if (a.universal !== b.universal) return a.universal ? -1 : 1;
    return a.outputFile < b.outputFile ? -1 : 1;
  });
  var universalCandidates = candidates.filter(function (candidate) { return candidate.universal; });
  if (!universalCandidates.length) {
    var split = new Error('Android split APK outputs are not supported by the typed installer');
    split.code = 'artifact-architecture-mismatch';
    throw split;
  }
  candidates = universalCandidates;
  if (context.targetArchitecture) {
    var compatible = candidates.filter(function (candidate) {
      return candidate.universal || candidate.targetArchitectures.indexOf(context.targetArchitecture) >= 0;
    });
    if (compatible.length) candidates = compatible;
  }
  if (!candidates.length) throw new Error('Android output metadata has no supported APK');
  var held = regularContained(outputRoot, path.join(outputRoot, candidates[0].outputFile));
  var applicationId = null;
  if (context.tools.apkanalyzer) {
    var result = context.commandRunner.runSync({
      executable: context.tools.apkanalyzer,
      argv: ['manifest', 'application-id', held.file],
      cwd: paths.PROJECT_ROOT, timeoutMs: 30000
    });
    if (result.ok) applicationId = result.stdout.trim();
  }
  if (!applicationId && context.tools.aapt) {
    var aapt = context.commandRunner.runSync({
      executable: context.tools.aapt, argv: ['dump', 'badging', held.file],
      cwd: paths.PROJECT_ROOT, timeoutMs: 30000
    });
    var match = aapt.ok && aapt.stdout.match(/^package:\s+name='([^']+)'/m);
    if (match) applicationId = match[1];
  }
  if (applicationId !== context.applicationId) {
    var mismatch = new Error(applicationId ? 'APK application id does not match project config' : 'No trusted APK manifest inspector is available');
    mismatch.code = 'artifact-invalid'; throw mismatch;
  }
  return {
    path: held.file,
    buildRoot: outputRoot,
    hash: hashStableRegular(held, 512 * 1024 * 1024),
    size: held.stat.size,
    applicationId: applicationId,
    targetArchitectures: candidates[0].targetArchitectures
  };
}

async function install(context, serial, artifact) {
  var result = await context.commandRunner.run({
    executable: context.tools.adb,
    argv: ['-s', serial, 'install', '-r', '-t', artifact.path],
    cwd: paths.PROJECT_ROOT, timeoutMs: 180000, signal: context.signal, onLine: context.onLine
  });
  if (!result.ok) {
    var code = /INSTALL_FAILED_([A-Z0-9_]+)/.exec(result.stdout + '\n' + result.stderr);
    var error = new Error('Android app install failed' + (code ? ': ' + code[0] : ''));
    error.code = 'install-failed'; error.result = result; throw error;
  }
}

async function launch(context, serial) {
  var resolve = await context.commandRunner.run({
    executable: context.tools.adb,
    argv: ['-s', serial, 'shell', 'cmd', 'package', 'resolve-activity', '--brief', context.applicationId],
    cwd: paths.PROJECT_ROOT, timeoutMs: 30000, signal: context.signal, onLine: context.onLine
  });
  var component = String(resolve.stdout || '').split(/\r?\n/).map(function (x) { return x.trim(); })
    .find(function (x) { return /^[A-Za-z][A-Za-z0-9_.]*\/[A-Za-z0-9_.$]+$/.test(x); });
  if (resolve.ok && !component) {
    var malformed = new Error('Android launcher resolution returned an invalid component');
    malformed.code = 'launch-failed';
    throw malformed;
  }
  var resolverUnavailable = !resolve.ok && !resolve.timedOut && !resolve.errorCode &&
    /(?:^|\n)(?:\/system\/bin\/sh:\s*)?cmd:\s*not found(?:\r?$|\n)|Unknown command:\s*resolve-activity(?:\r?$|\n)/i
      .test(String(resolve.stdout || '') + '\n' + String(resolve.stderr || ''));
  if (!resolve.ok && !resolverUnavailable) {
    var unresolved = new Error('Android launcher resolution failed without a supported unavailable-command proof');
    unresolved.code = 'launch-failed';
    throw unresolved;
  }
  var argv = component
    ? ['-s', serial, 'shell', 'am', 'start', '-n', component]
    : ['-s', serial, 'shell', 'monkey', '-p', context.applicationId, '-c', 'android.intent.category.LAUNCHER', '1'];
  var started = await context.commandRunner.run({
    executable: context.tools.adb, argv: argv, cwd: paths.PROJECT_ROOT,
    timeoutMs: 30000, signal: context.signal, onLine: context.onLine
  });
  if (!started.ok) { var error = new Error('Android app launch failed'); error.code = 'launch-failed'; throw error; }
  var pid = await context.commandRunner.run({
    executable: context.tools.adb, argv: ['-s', serial, 'shell', 'pidof', context.applicationId],
    cwd: paths.PROJECT_ROOT, timeoutMs: 15000, signal: context.signal, onLine: context.onLine
  });
  if (!pid.ok || !/^\s*\d+/.test(pid.stdout)) {
    var verify = new Error('Android app process was not observed after launch'); verify.code = 'launch-failed'; throw verify;
  }
  return { pid: Number(pid.stdout.trim().split(/\s+/)[0]), component: component || null };
}

async function stop(context, serial) {
  return context.commandRunner.run({
    executable: context.tools.adb,
    argv: ['-s', serial, 'shell', 'am', 'force-stop', context.applicationId],
    cwd: paths.PROJECT_ROOT, timeoutMs: 30000, signal: context.signal, onLine: context.onLine
  });
}

async function screenshot(context, serial) {
  return context.commandRunner.runBinary({
    executable: context.tools.adb,
    argv: ['-s', serial, 'exec-out', 'screencap', '-p'],
    cwd: paths.PROJECT_ROOT, timeoutMs: 30000, maxBytes: 25 * 1024 * 1024,
    signal: context.signal
  });
}

async function createDevice(context, preview) {
  var name = preview.generatedName;
  var result = await context.commandRunner.run({
    executable: context.tools.avdmanager,
    argv: ['create', 'avd', '-n', name, '-k', preview.rawRuntimeId, '-d', preview.rawProfileId],
    cwd: paths.PROJECT_ROOT, timeoutMs: 120000, input: 'no\n',
    signal: context.signal, onLine: context.onLine
  });
  if (!result.ok) { var error = new Error('Android virtual device creation failed'); error.code = 'device-create-failed'; throw error; }
  var listed = context.commandRunner.runSync({
    executable: context.tools.emulator,
    argv: ['-list-avds'],
    cwd: paths.PROJECT_ROOT,
    timeoutMs: 30000
  });
  var parsed = listed.ok ? parseAvdList(listed.stdout) : { names: [] };
  if (!listed.ok || parsed.names.indexOf(name) < 0) {
    var unverified = new Error('Android virtual device was not observed after creation');
    unverified.code = 'device-create-failed';
    throw unverified;
  }
  return { displayName: name, stableMaterial: 'android:' + name };
}

module.exports = {
  sdkRoots: sdkRoots,
  resolveTools: resolveTools,
  parseAdbDevices: parseAdbDevices,
  parseAvdList: parseAvdList,
  parseAvdmanagerDevices: parseAvdmanagerDevices,
  resolveAvdName: resolveAvdName,
  discover: discover,
  ensureStarted: ensureStarted,
  build: build,
  resolveArtifact: resolveArtifact,
  install: install,
  launch: launch,
  stop: stop,
  screenshot: screenshot,
  createDevice: createDevice
};
