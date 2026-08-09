import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const android = require('../server/android-runner.js');
const ios = require('../server/ios-runner.js');

const sdkFixture = fs.mkdtempSync(path.join(os.tmpdir(), 'app-run-sdk-order-'));
const previousAndroidHome = process.env.ANDROID_HOME;
const previousAndroidSdkRoot = process.env.ANDROID_SDK_ROOT;
try {
  delete process.env.ANDROID_HOME;
  process.env.ANDROID_SDK_ROOT = sdkFixture;
  for (const version of ['9.0.0', '10.0.0']) {
    const tool = path.join(sdkFixture, 'build-tools', version, 'aapt');
    fs.mkdirSync(path.dirname(tool), { recursive: true });
    fs.writeFileSync(tool, '#!/bin/sh\nexit 0\n');
    fs.chmodSync(tool, 0o755);
  }
  assert.equal(android.resolveTools().aapt,
    fs.realpathSync(path.join(sdkFixture, 'build-tools', '10.0.0', 'aapt')),
  'SDK tool versions must use numeric ordering instead of lexicographic ordering');
} finally {
  if (previousAndroidHome === undefined) delete process.env.ANDROID_HOME;
  else process.env.ANDROID_HOME = previousAndroidHome;
  if (previousAndroidSdkRoot === undefined) delete process.env.ANDROID_SDK_ROOT;
  else process.env.ANDROID_SDK_ROOT = previousAndroidSdkRoot;
  fs.rmSync(sdkFixture, { recursive: true, force: true });
}

const adb = android.parseAdbDevices(`List of devices attached
emulator-5554 device product:sdk_gphone model:Pixel_7 transport_id:1
R58M123 device product:physical model:Phone transport_id:2
emulator-5556 offline transport_id:3
malformed row with too many unsupported tokens
`);
assert.equal(adb.devices.length, 3);
assert.equal(adb.devices[0].emulator, true);
assert.equal(adb.devices[1].emulator, false);
assert.equal(adb.devices[2].state, 'offline');
assert.equal(adb.malformed, 1);
const duplicateAdb = android.parseAdbDevices(
  'emulator-5554 device model:One\nemulator-5554 device model:Two\n',
);
assert.equal(duplicateAdb.devices.length, 1);
assert.equal(duplicateAdb.malformed, 1);
const oversizedAdb = android.parseAdbDevices(
  `emulator-5554 device model:${'x'.repeat(1200)}\n`,
);
assert.equal(oversizedAdb.devices.length, 0);
assert.equal(oversizedAdb.malformed, 1);

const parsedAvds = android.parseAvdList('Pixel_7_API_35\nbad avd name\nPixel_7_API_35\n');
assert.deepEqual(parsedAvds.names, ['Pixel_7_API_35']);
assert.equal(parsedAvds.malformed, 2);
assert.deepEqual(android.parseAvdmanagerDevices(`
id: 0 or "pixel_7"
    Name: Pixel 7
id: 1 or "pixel_tablet"
    Name: Pixel Tablet
`).map((row) => row.deviceId), ['pixel_7', 'pixel_tablet']);
const duplicateProfiles = android.parseAvdmanagerDevices(`
id: 0 or "pixel_7"
    Name: Pixel 7
id: 1 or "pixel_7"
    Name: Duplicate Pixel 7
id: malformed
`);
assert.equal(duplicateProfiles.length, 1);
assert.equal(duplicateProfiles.malformed, 2);

const avdNameCommands = [];
const avdNameRunner = {
  runSync(spec) {
    avdNameCommands.push(spec.argv.join(' '));
    if (spec.argv.includes('emu')) {
      return { ok: true, status: 0, stdout: '', stderr: '', errorCode: null };
    }
    return {
      ok: true, status: 0, stdout: 'Medium_Phone\n', stderr: '', errorCode: null,
    };
  },
};
assert.equal(
  android.resolveAvdName(avdNameRunner, '/fixture/adb', 'emulator-5554'),
  'Medium_Phone',
);
assert.deepEqual(avdNameCommands, [
  '-s emulator-5554 emu avd name',
  '-s emulator-5554 shell getprop ro.boot.qemu.avd_name',
]);
assert.equal(android.resolveAvdName({
  runSync() {
    return {
      ok: true, status: 0, stdout: '../../unsafe\n', stderr: '', errorCode: null,
    };
  },
}, '/fixture/adb', 'emulator-5554'), null);

const fallbackDiscovery = android.discover({
  tools: {
    sdkRoot: null,
    adb: '/fixture/adb',
    emulator: '/fixture/emulator',
    avdmanager: null,
    sdkmanager: null,
    apkanalyzer: '/fixture/apkanalyzer',
    aapt: null,
    gradlew: '/fixture/gradlew',
  },
  commandRunner: {
    runSync(spec) {
      const args = spec.argv.join(' ');
      if (args === 'devices -l') {
        return {
          ok: true, status: 0,
          stdout: 'emulator-5554 device model:Pixel transport_id:1\n',
          stderr: '', errorCode: null,
        };
      }
      if (args === '-list-avds') {
        return {
          ok: true, status: 0, stdout: 'Medium_Phone\n',
          stderr: '', errorCode: null,
        };
      }
      if (args.includes('emu avd name')) {
        return { ok: true, status: 0, stdout: '', stderr: '', errorCode: null };
      }
      if (args.includes('ro.boot.qemu.avd_name')) {
        return {
          ok: true, status: 0, stdout: 'Medium_Phone\n',
          stderr: '', errorCode: null,
        };
      }
      if (args.includes('ro.build.version.release')) {
        return { ok: true, status: 0, stdout: '16\n', stderr: '', errorCode: null };
      }
      if (args.includes('ro.product.cpu.abi')) {
        return { ok: true, status: 0, stdout: 'arm64-v8a\n', stderr: '', errorCode: null };
      }
      return { ok: false, status: 1, stdout: '', stderr: '', errorCode: null };
    },
  },
});
assert.equal(fallbackDiscovery.devices.length, 1,
  'a running AVD resolved through the boot property must not also appear stopped');
assert.equal(fallbackDiscovery.devices[0].avdName, 'Medium_Phone');
assert.equal(fallbackDiscovery.devices[0].state, 'running');

const simctl = ios.parseSimctlJson(JSON.stringify({
  runtimes: [
    { identifier: 'com.apple.CoreSimulator.SimRuntime.iOS-18-0', name: 'iOS 18.0', version: '18.0', isAvailable: true },
    { identifier: 'com.apple.CoreSimulator.SimRuntime.iOS-17-0', name: 'iOS 17.0', version: '17.0', isAvailable: false },
  ],
  devicetypes: [
    { identifier: 'com.apple.CoreSimulator.SimDeviceType.iPhone-16', name: 'iPhone 16' },
  ],
  devices: {
    'com.apple.CoreSimulator.SimRuntime.iOS-18-0': [
      { udid: 'AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE', name: 'iPhone 16', state: 'Booted', isAvailable: true },
      { udid: '11111111-2222-3333-4444-555555555555', name: 'Unavailable', state: 'Shutdown', isAvailable: false },
    ],
  },
}));
assert.equal(simctl.runtimes.length, 1);
assert.equal(simctl.devices.length, 1);
assert.equal(simctl.devices[0].state, 'running');
assert.equal(simctl.deviceTypes[0].name, 'iPhone 16');
assert.equal(ios.parseSimctlJson(JSON.stringify({
  runtimes: [
    { identifier: 'com.apple.CoreSimulator.SimRuntime.iOS-18-0', name: 'iOS 18.0', version: '18.0' },
  ],
  devicetypes: [],
  devices: {},
})).runtimes.length, 0,
'a runtime without an explicit availability proof must never become creatable');
assert.throws(() => ios.parseSimctlJson('{"devices":{}}'), /incomplete/);
assert.throws(() => ios.parseSimctlJson(JSON.stringify({
  runtimes: [
    { identifier: 'com.apple.CoreSimulator.SimRuntime.iOS-18-0', name: 'iOS 18', version: '18.0', isAvailable: true },
    { identifier: 'com.apple.CoreSimulator.SimRuntime.iOS-18-0', name: 'iOS 18 duplicate', version: '18.0', isAvailable: true },
  ],
  devicetypes: [],
  devices: {},
})), /duplicated/);
const sanitizedSimctl = ios.parseSimctlJson(JSON.stringify({
  runtimes: [
    { identifier: 'com.apple.CoreSimulator.SimRuntime.iOS-18-0', name: 'iOS\n18', version: '18.0', isAvailable: true },
  ],
  devicetypes: [
    { identifier: 'com.apple.CoreSimulator.SimDeviceType.iPhone-16', name: 'iPhone\n16' },
  ],
  devices: {
    'com.apple.CoreSimulator.SimRuntime.iOS-18-0': [
      { udid: 'AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE', name: 'Phone\nName', state: 'Booted', isAvailable: true },
    ],
  },
}));
assert.equal(sanitizedSimctl.devices[0].displayName.includes('\n'), false);
assert.equal(sanitizedSimctl.deviceTypes[0].name.includes('\n'), false);

let staleTargetStarts = 0;
const reusedSerial = await android.ensureStarted({
  tools: { emulator: '/fixture/emulator', adb: '/fixture/adb' },
  commandRunner: {
    runSync(spec) {
      const args = spec.argv.join(' ');
      if (args === 'devices') {
        return {
          ok: true, status: 0, stdout: 'emulator-5554\tdevice\n',
          stderr: '', errorCode: null,
        };
      }
      if (args.includes('emu avd name')) {
        return { ok: true, status: 0, stdout: '', stderr: '', errorCode: null };
      }
      if (args.includes('ro.boot.qemu.avd_name')) {
        return {
          ok: true, status: 0, stdout: 'Medium_Phone\n',
          stderr: '', errorCode: null,
        };
      }
      if (args.includes('sys.boot_completed')) {
        return { ok: true, status: 0, stdout: '1\n', stderr: '', errorCode: null };
      }
      return { ok: false, status: 1, stdout: '', stderr: '', errorCode: null };
    },
    startDetached() {
      staleTargetStarts++;
      throw new Error('an already-ready AVD must not be started again');
    },
  },
  signal: new AbortController().signal,
  onProcess() {},
}, {
  state: 'stopped',
  rawIdentifier: 'Medium_Phone',
  avdName: 'Medium_Phone',
});
assert.equal(reusedSerial, 'emulator-5554');
assert.equal(staleTargetStarts, 0);

const cancelled = new AbortController();
cancelled.abort();
let terminatedIdentity = null;
await assert.rejects(
  android.ensureStarted({
    tools: { emulator: '/fixture/emulator', adb: '/fixture/adb' },
    commandRunner: {
      startDetached() {
        return { pid: 4242, processStartId: 'psid-v1:' + process.platform + ':' + 'a'.repeat(64) };
      },
      terminateIdentity(identity) {
        terminatedIdentity = identity;
        return true;
      },
      runSync() {
        throw new Error('an already-aborted boot must not poll adb');
      },
    },
    signal: cancelled.signal,
    onProcess() {},
  }, {
    state: 'stopped',
    rawIdentifier: 'Pixel_8_API_35',
    avdName: 'Pixel_8_API_35',
  }),
  (error) => error && error.code === 'cancelled',
);
assert.equal(terminatedIdentity.pid, 4242);

let rejectedIdentityTerminated = null;
await assert.rejects(
  android.ensureStarted({
    tools: { emulator: '/fixture/emulator', adb: '/fixture/adb' },
    commandRunner: {
      startDetached() {
        return { pid: 4343, processStartId: null };
      },
      terminateIdentity(identity) {
        rejectedIdentityTerminated = identity;
        return true;
      },
      runSync() {
        throw new Error('a rejected process identity must not poll adb');
      },
    },
    signal: new AbortController().signal,
    onProcess() {
      throw new Error('identity fence rejected');
    },
  }, {
    state: 'stopped',
    rawIdentifier: 'Pixel_8_API_35',
    avdName: 'Pixel_8_API_35',
  }),
  /identity fence rejected/,
);
assert.equal(rejectedIdentityTerminated.pid, 4343,
  'an emulator rejected by the durable identity fence must be terminated immediately');

const launchInvocations = [];
await assert.rejects(
  android.launch({
    tools: { adb: '/fixture/adb' },
    commandRunner: {
      async run(spec) {
        launchInvocations.push(spec.argv);
        return {
          ok: true,
          status: 0,
          signal: null,
          timedOut: false,
          stdout: 'unexpected resolver prose\n',
          stderr: '',
          errorCode: null,
        };
      },
    },
    applicationId: 'com.example.fixture',
    signal: new AbortController().signal,
    onLine() {},
  }, 'emulator-5554'),
  (error) => error && error.code === 'launch-failed',
);
assert.equal(launchInvocations.length, 1,
  'a malformed successful resolver response must not fall back to monkey');

const failedResolverInvocations = [];
await assert.rejects(
  android.launch({
    tools: { adb: '/fixture/adb' },
    commandRunner: {
      async run(spec) {
        failedResolverInvocations.push(spec.argv);
        return {
          ok: false, status: 1, signal: null, timedOut: false,
          stdout: '', stderr: 'error: device offline', errorCode: null,
        };
      },
    },
    applicationId: 'com.example.fixture',
    signal: new AbortController().signal,
    onLine() {},
  }, 'emulator-5554'),
  (error) => error && error.code === 'launch-failed',
);
assert.equal(failedResolverInvocations.length, 1,
  'an unclassified resolver failure must never fall back to monkey');

const unavailableResolverInvocations = [];
const unavailableLaunch = await android.launch({
  tools: { adb: '/fixture/adb' },
  commandRunner: {
    async run(spec) {
      unavailableResolverInvocations.push(spec.argv);
      if (unavailableResolverInvocations.length === 1) {
        return {
          ok: false, status: 127, signal: null, timedOut: false,
          stdout: '', stderr: '/system/bin/sh: cmd: not found\n', errorCode: null,
        };
      }
      if (spec.argv.includes('pidof')) {
        return {
          ok: true, status: 0, signal: null, timedOut: false,
          stdout: '4242\n', stderr: '', errorCode: null,
        };
      }
      return {
        ok: true, status: 0, signal: null, timedOut: false,
        stdout: 'Events injected: 1\n', stderr: '', errorCode: null,
      };
    },
  },
  applicationId: 'com.example.fixture',
  signal: new AbortController().signal,
  onLine() {},
}, 'emulator-5554');
assert.equal(unavailableLaunch.component, null);
assert.equal(unavailableResolverInvocations[1].includes('monkey'), true,
  'monkey is allowed only for an exact resolver-unavailable proof');

const createdAndroid = await android.createDevice({
  tools: { avdmanager: '/fixture/avdmanager', emulator: '/fixture/emulator' },
  commandRunner: {
    async run() {
      return { ok: true, status: 0, stdout: '', stderr: '', errorCode: null };
    },
    runSync() {
      return { ok: true, status: 0, stdout: 'orchestrator_fixture_01\n', stderr: '', errorCode: null };
    },
  },
  signal: new AbortController().signal,
  onLine() {},
}, {
  generatedName: 'orchestrator_fixture_01',
  rawRuntimeId: 'system-images;android-35;google_apis;x86_64',
  rawProfileId: 'pixel_7',
});
assert.equal(createdAndroid.stableMaterial, 'android:orchestrator_fixture_01');
await assert.rejects(android.createDevice({
  tools: { avdmanager: '/fixture/avdmanager', emulator: '/fixture/emulator' },
  commandRunner: {
    async run() {
      return { ok: true, status: 0, stdout: '', stderr: '', errorCode: null };
    },
    runSync() {
      return { ok: true, status: 0, stdout: 'some_other_avd\n', stderr: '', errorCode: null };
    },
  },
  signal: new AbortController().signal,
  onLine() {},
}, {
  generatedName: 'orchestrator_fixture_01',
  rawRuntimeId: 'system-images;android-35;google_apis;x86_64',
  rawProfileId: 'pixel_7',
}), (error) => error && error.code === 'device-create-failed');

const createdUdid = 'AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE';
const createdRuntime = 'com.apple.CoreSimulator.SimRuntime.iOS-18-0';
const createdType = 'com.apple.CoreSimulator.SimDeviceType.iPhone-16';
const createdIos = await ios.createDevice({
  tools: { xcrun: '/fixture/xcrun' },
  commandRunner: {
    async run() {
      return { ok: true, status: 0, stdout: `${createdUdid}\n`, stderr: '', errorCode: null };
    },
    runSync() {
      return {
        ok: true,
        status: 0,
        stderr: '',
        errorCode: null,
        stdout: JSON.stringify({
          runtimes: [{
            identifier: createdRuntime,
            name: 'iOS 18.0',
            version: '18.0',
            isAvailable: true,
          }],
          devicetypes: [{ identifier: createdType, name: 'iPhone 16' }],
          devices: {
            [createdRuntime]: [{
              udid: createdUdid,
              name: 'Orchestrator Fixture',
              state: 'Shutdown',
              isAvailable: true,
              deviceTypeIdentifier: createdType,
            }],
          },
        }),
      };
    },
  },
  signal: new AbortController().signal,
  onLine() {},
}, {
  generatedName: 'Orchestrator Fixture',
  rawRuntimeId: createdRuntime,
  rawProfileId: createdType,
});
assert.equal(createdIos.stableMaterial, `ios:${createdUdid}`);
