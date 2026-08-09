'use strict';

var crypto = require('crypto');
var appRunConfig = require('./app-run-config');
var sourceRevision = require('../../tasks/project-source-revision.cjs');
var paths = require('./paths');
var android = require('./android-runner');
var ios = require('./ios-runner');

var CACHE_TTL_MS = 15000;
var secret = crypto.randomBytes(32);
var cache = null;

function opaque(prefix, revision, material) {
  return prefix + '-' + crypto.createHmac('sha256', secret)
    .update(revision + '\0' + material, 'utf8').digest('hex').slice(0, 32);
}

function stableHint(material) {
  return 'hint-' + crypto.createHash('sha256')
    .update('app-run-stable-hint-v1\0' + String(material), 'utf8')
    .digest('hex').slice(0, 32);
}

function projectVariants(config, platform) {
  if (!config.ok) return [];
  if (platform === 'android') {
    return (config.manifest.android && config.manifest.android.variants || []).map(function (row) {
      return { id: row.id, label: row.label };
    });
  }
  return (config.manifest.ios && config.manifest.ios.configurations || []).map(function (row) {
    return { id: row.id, label: row.label };
  });
}

function projectPublic(platform, raw, revision, config, source) {
  var deviceMap = Object.create(null), profileMap = Object.create(null);
  var devices = raw.devices.map(function (row) {
    var id = opaque('target', revision, row.stableMaterial);
    deviceMap[id] = row;
    return {
      id: id,
      platform: platform,
      displayName: row.displayName,
      state: row.state,
      osVersion: row.osVersion,
      model: row.model,
      architecture: row.architecture,
      managedByOrchestrator: row.managedByOrchestrator === true,
      capabilities: row.capabilities.slice(),
      stableHint: stableHint(row.stableMaterial)
    };
  });
  var profiles = raw.profiles.map(function (row) {
    var id = opaque('profile', revision, row.stableMaterial);
    var runtimeId = opaque('runtime', revision, row.rawRuntimeId);
    profileMap[id + '\0' + runtimeId] = row;
    return {
      id: id,
      runtimeId: runtimeId,
      displayName: row.displayName,
      runtimeName: row.runtimeName,
      osVersion: row.osVersion,
      architecture: row.architecture,
      estimatedBytes: row.estimatedBytes
    };
  });
  var sourceUnavailable = !source || source.available !== true;
  return {
    public: {
      id: platform,
      availability: sourceUnavailable ? 'unavailable' : raw.availability,
      reasonCode: config.ok
        ? sourceUnavailable ? source.reasonCode : raw.reasonCode
        : config.error,
      toolchain: raw.toolchain,
      devices: devices,
      creatableProfiles: profiles,
      variants: projectVariants(config, platform),
      diagnostics: {
        warnings: raw.warnings.slice(0, 20),
        unsupportedPhysicalDevices: platform === 'android' ? raw.physicalDeviceCount : 0
      }
    },
    private: {
      tools: raw.private.tools,
      devices: deviceMap,
      profiles: profileMap
    }
  };
}

function failedPlatform(platform, config, error) {
  return {
    platform: platform,
    availability: 'unavailable',
    reasonCode: config.ok ? 'discovery-failed' : config.error,
    toolchain: {},
    devices: [],
    profiles: [],
    physicalDeviceCount: 0,
    warnings: ['discovery-failed'],
    private: { tools: {} },
    diagnostic: String(error && error.message || error || 'discovery failed').slice(0, 240)
  };
}

function unconfiguredPlatform(platform, reasonCode) {
  return {
    platform: platform,
    availability: 'unavailable',
    reasonCode: reasonCode,
    toolchain: {},
    devices: [],
    profiles: [],
    physicalDeviceCount: 0,
    warnings: [],
    private: { tools: {} }
  };
}

function discover(options) {
  options = options || {};
  var now = Date.now();
  if (!options.refresh && cache && now - cache.createdMs < CACHE_TTL_MS) return cache;
  var config = appRunConfig.load();
  var revision = 'discovery-' + crypto.randomBytes(18).toString('hex');
  var source = sourceRevision.compute(paths.PROJECT_ROOT, {
    profile: 'app-build',
    appRoots: appRunConfig.sourceRoots(config)
  });
  var androidRaw, iosRaw;
  try {
    androidRaw = config.ok
      ? config.manifest.android
        ? android.discover({ config: config, commandRunner: options.commandRunner, tools: options.androidTools })
        : unconfiguredPlatform('android', 'android-run-config-missing')
      : failedPlatform('android', config);
  } catch (androidError) {
    androidRaw = failedPlatform('android', config, androidError);
  }
  try {
    iosRaw = config.ok
      ? !config.project.iosEnabled
        ? ios.discover({ config: config, commandRunner: options.commandRunner,
          tools: options.iosTools, forcePlatform: options.forceIos })
        : config.manifest.ios
          ? ios.discover({ config: config, commandRunner: options.commandRunner,
            tools: options.iosTools, forcePlatform: options.forceIos })
          : unconfiguredPlatform('ios', 'ios-run-config-missing')
      : failedPlatform('ios', config);
  } catch (iosError) {
    iosRaw = failedPlatform('ios', config, iosError);
  }
  var ap = projectPublic('android', androidRaw, revision, config, source);
  var ip = projectPublic('ios', iosRaw, revision, config, source);
  var completedMs = Date.now();
  cache = {
    createdMs: completedMs,
    discoveryRevision: revision,
    expiresAt: new Date(completedMs + CACHE_TTL_MS).toISOString(),
    sourceRevision: source.available ? source.revision : null,
    sourceStatus: source,
    config: config,
    public: {
      schemaVersion: 1,
      discoveryRevision: revision,
      expiresAt: new Date(completedMs + CACHE_TTL_MS).toISOString(),
      projectSourceRevision: source.available ? source.revision : null,
      platforms: [ap.public, ip.public]
    },
    private: { android: ap.private, ios: ip.private }
  };
  return cache;
}

function targets(platform, refresh, options) {
  options = Object.assign({}, options || {}, { refresh: refresh === true });
  var current = discover(options);
  if (!platform) return current.public;
  var hit = current.public.platforms.find(function (row) { return row.id === platform; });
  return {
    schemaVersion: 1,
    discoveryRevision: current.discoveryRevision,
    expiresAt: current.expiresAt,
    projectSourceRevision: current.public.projectSourceRevision,
    platforms: hit ? [hit] : []
  };
}

function resolveTarget(platform, targetId, revision, options) {
  var current = discover(options);
  if (current.discoveryRevision !== revision) return { ok: false, error: 'stale-discovery' };
  var publicPlatform = current.public.platforms.find(function (row) { return row.id === platform; });
  if (!publicPlatform || publicPlatform.availability === 'unavailable') {
    return {
      ok: false,
      error: publicPlatform && publicPlatform.reasonCode || 'discovery-failed'
    };
  }
  var bucket = current.private[platform];
  var target = bucket && bucket.devices[targetId];
  if (!target) return { ok: false, error: 'target-not-found' };
  return { ok: true, target: target, tools: bucket.tools, snapshot: current };
}

function resolveProfile(platform, profileId, runtimeId, revision, options) {
  var current = discover(options);
  if (current.discoveryRevision !== revision) return { ok: false, error: 'stale-discovery' };
  var publicPlatform = current.public.platforms.find(function (row) { return row.id === platform; });
  if (!publicPlatform || publicPlatform.availability === 'unavailable') {
    return {
      ok: false,
      error: publicPlatform && publicPlatform.reasonCode || 'discovery-failed'
    };
  }
  var bucket = current.private[platform];
  var profile = bucket && bucket.profiles[profileId + '\0' + runtimeId];
  if (!profile) return { ok: false, error: 'profile-not-found' };
  return { ok: true, profile: profile, tools: bucket.tools, snapshot: current };
}

function invalidate() { cache = null; }

module.exports = {
  CACHE_TTL_MS: CACHE_TTL_MS,
  discover: discover,
  targets: targets,
  resolveTarget: resolveTarget,
  resolveProfile: resolveProfile,
  stableHint: stableHint,
  invalidate: invalidate
};
