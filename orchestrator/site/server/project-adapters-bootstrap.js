'use strict';

// A fresh template project has one declared Compose design-system layout.
// Persist its adapter contract on the first Figma sync so the normal setup
// path never asks the user to author project-adapters.json by hand. Existing
// product-owned configs are never replaced; non-standard projects keep the
// explicit config as their escape hatch.

var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var configUpdate = require('./project-config-update');

var CONFIG_MAX = 256 * 1024;
var CONFIG_FILE = path.join(paths.PROJECT_ROOT, 'orchestrator', 'figma', 'project-adapters.json');
var CONFIG_DIR = path.dirname(CONFIG_FILE);
var PACKAGE_RE = /^[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)+$/;

function relativeFileExists(relative) {
  var file = path.join(paths.PROJECT_ROOT, relative);
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, path.dirname(file), file, 4 * 1024 * 1024);
  return !!(hit && hit.stat && String(hit.stat.nlink) === '1');
}

function relativeDirectoryExists(relative) {
  var directory = path.join(paths.PROJECT_ROOT, relative);
  var hit = fileGuards.realDirectoryUnder(paths.PROJECT_ROOT, directory);
  return !!(hit && hit.exists && hit.stat);
}

function configPresence() {
  var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, CONFIG_DIR, CONFIG_FILE);
  if (inspected && inspected.status === 'missing') return 'missing';
  if (inspected && inspected.status === 'present' && inspected.stat &&
      inspected.stat.isFile() && !inspected.stat.isSymbolicLink() &&
      String(inspected.stat.nlink) === '1') return 'present';
  return 'unsafe';
}

function themeCandidates(corePackagePath, productPackage) {
  var candidates = [
    { mode: 'light', name: 'LightAppColors' },
    { mode: 'light', name: 'LightColor' },
    { mode: 'dark', name: 'DarkAppColors' },
    { mode: 'dark', name: 'DarkColor' }
  ];
  var grouped = Object.create(null);
  candidates.forEach(function (candidate) {
    var relative = corePackagePath + '/' + candidate.name + '.kt';
    if (!relativeFileExists(relative)) return;
    var group = grouped[candidate.mode] = grouped[candidate.mode] || {
      mode: candidate.mode,
      symbols: [],
      files: []
    };
    group.symbols.push(productPackage + '.design.system.core.' + candidate.name);
    group.files.push('**/' + candidate.name + '.kt');
  });
  return ['light', 'dark'].map(function (mode) { return grouped[mode] || null; }).filter(Boolean);
}

function buildDocument(options) {
  var tokenInclude = ['**/AppColor.kt', '**/AppDp.kt', '**/AppTokens.kt'];
  options.themes.forEach(function (theme) {
    theme.files.forEach(function (file) {
      if (tokenInclude.indexOf(file) < 0) tokenInclude.push(file);
    });
  });
  if (options.palette) tokenInclude.push('**/AppPalette.kt');

  var colorAuthority = {
    contracts: [options.productPackage + '.design.system.resources.provider.AppColor'],
    implementations: options.themes.map(function (theme) {
      return { mode: theme.mode, symbols: theme.symbols.slice() };
    })
  };
  if (options.palette) {
    colorAuthority.primitiveContainers = [
      options.productPackage + '.design.system.core.AppPalette'
    ];
  }

  var components = {
    roots: ['design-system/components/src/commonMain/kotlin'],
    include: ['**/*.kt'],
    exclude: ['**/build/**'],
    visibility: ['public'],
    previewRoots: options.preview ? ['design-system/preview/src/commonMain/kotlin'] : [],
    screenshotTestRoots: options.screenshotTests
      ? ['design-system/components/src/androidHostTest/kotlin'] : []
  };

  return {
    schemaVersion: 2,
    adapters: [{
      id: 'compose-design-system',
      kind: 'kotlin-compose',
      version: 2,
      enabled: true,
      capabilities: ['tokens', 'components'],
      platform: 'shared',
      authority: 'handwritten',
      tokens: {
        roots: [
          'design-system/core/src/commonMain/kotlin',
          'design-system/resources/provider/src/commonMain/kotlin'
        ],
        include: tokenInclude,
        exclude: ['**/build/**'],
        modes: ['shared'].concat(options.themes.map(function (theme) { return theme.mode; })),
        authorities: {
          color: colorAuthority,
          dimension: {
            // AppTokens is the stable project contract; AppDp owns the concrete
            // nested Dp values consumed through that facade.
            contracts: [options.productPackage + '.design.system.core.AppTokens'],
            primitiveContainers: [
              options.productPackage + '.design.system.resources.provider.AppDp'
            ]
          }
        },
        contextMap: options.themes.map(function (theme) {
          return { when: { theme: theme.mode }, projectMode: theme.mode };
        }),
        // Provider naming is project/design specific. Never invent bindings;
        // the UI can confirm explicit mappings after observed tokens exist.
        bindingRules: []
      },
      components: components
    }]
  };
}

function deriveDocument() {
  var config = configUpdate.read();
  if (!config.ok || config.figmaEnabled !== true) {
    return { ok: false, state: config.ok ? 'disabled' : 'config-unavailable' };
  }
  var productPackage = config.productPackage;
  if (!PACKAGE_RE.test(String(productPackage || ''))) {
    return { ok: false, state: 'project-package-unavailable' };
  }

  var packagePath = productPackage.split('.').join('/');
  var coreRoot = 'design-system/core/src/commonMain/kotlin';
  var providerRoot = 'design-system/resources/provider/src/commonMain/kotlin';
  var corePackagePath = coreRoot + '/' + packagePath + '/design/system/core';
  var providerPackagePath = providerRoot + '/' + packagePath + '/design/system/resources/provider';
  var required = [
    providerPackagePath + '/AppColor.kt',
    providerPackagePath + '/AppDp.kt',
    corePackagePath + '/AppTokens.kt'
  ];
  var themes = themeCandidates(corePackagePath, productPackage);
  var componentsReady = relativeDirectoryExists('design-system/components/src/commonMain/kotlin');
  if (required.some(function (file) { return !relativeFileExists(file); }) ||
      !themes.length || !componentsReady) {
    return { ok: false, state: 'design-system-not-ready' };
  }

  return {
    ok: true,
    state: 'ready',
    document: buildDocument({
      productPackage: productPackage,
      themes: themes,
      palette: relativeFileExists(corePackagePath + '/AppPalette.kt'),
      preview: relativeDirectoryExists('design-system/preview/src/commonMain/kotlin'),
      screenshotTests: relativeDirectoryExists('design-system/components/src/androidHostTest/kotlin')
    })
  };
}

function ensure() {
  var presence = configPresence();
  if (presence === 'present') return { ok: true, state: 'existing', path: CONFIG_FILE };
  if (presence === 'unsafe') return { ok: false, state: 'config-unsafe', error: 'figma-adapter-config-unsafe' };

  var derived = deriveDocument();
  if (!derived.ok) return derived;
  var bytes = Buffer.from(JSON.stringify(derived.document, null, 2) + '\n', 'utf8');
  if (bytes.length > CONFIG_MAX) {
    return { ok: false, state: 'config-too-large', error: 'figma-adapter-config-invalid' };
  }
  var published = fileGuards.publishNoClobberRegularFileUnder(
    paths.PROJECT_ROOT,
    CONFIG_DIR,
    CONFIG_FILE,
    bytes,
    { create: false, mode: 0o600, maxBytes: CONFIG_MAX }
  );
  if (!published.ok) {
    // A concurrent first-sync may have won the no-clobber race. Preserve and
    // accept that product-owned generation instead of replacing it.
    if (configPresence() === 'present') return { ok: true, state: 'existing', path: CONFIG_FILE };
    return {
      ok: false,
      state: 'write-failed',
      error: 'figma-adapter-config-write-failed'
    };
  }
  var verified = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, CONFIG_DIR, CONFIG_FILE, CONFIG_MAX);
  if (!verified || !verified.bytes.equals(bytes) || !verified.stat || String(verified.stat.nlink) !== '1') {
    return { ok: false, state: 'postcondition-failed', error: 'figma-adapter-config-write-failed' };
  }
  return { ok: true, state: 'created', path: CONFIG_FILE, document: derived.document };
}

module.exports = {
  ensure: ensure
};
