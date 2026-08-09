import assert from 'node:assert/strict';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import path from 'node:path';

import {
  crashSiteTestNames,
  crashSuiteNames,
  crashTaskTestNames,
  delegatedWorkspaceScripts,
  expectedOwnershipSummary,
  leafSuiteNames,
  requiredRootScripts,
  toolingTests,
  workspacePaths
} from './suite-contract.mjs';

const ignoredDiscoveryDirectories = new Set([
  '.cache',
  '.git',
  '__pycache__',
  'node_modules'
]);

function readJson(projectRoot, relativePath) {
  return JSON.parse(readFileSync(path.join(projectRoot, relativePath), 'utf8'));
}

function discover(projectRoot, directory, predicate) {
  const files = [];
  function visit(relativeDirectory) {
    for (const directoryEntry of readdirSync(
      path.join(projectRoot, relativeDirectory),
      { withFileTypes: true }
    )) {
      const relativePath = path.posix.join(relativeDirectory, directoryEntry.name);
      if (directoryEntry.isDirectory()) {
        if (!ignoredDiscoveryDirectories.has(directoryEntry.name)) visit(relativePath);
      } else if (
        directoryEntry.isFile() &&
        predicate(directoryEntry.name, relativePath)
      ) {
        files.push(relativePath);
      }
    }
  }
  visit(directory);
  return files.sort();
}

function discoverSymlinks(projectRoot, directory) {
  const links = [];
  function visit(relativeDirectory) {
    for (const directoryEntry of readdirSync(
      path.join(projectRoot, relativeDirectory),
      { withFileTypes: true }
    )) {
      const relativePath = path.posix.join(relativeDirectory, directoryEntry.name);
      if (ignoredDiscoveryDirectories.has(directoryEntry.name)) {
        continue;
      }
      if (directoryEntry.isSymbolicLink()) {
        links.push(relativePath);
      } else if (directoryEntry.isDirectory()) {
        visit(relativePath);
      }
    }
  }
  visit(directory);
  return links.sort();
}

function isTestLike(name) {
  return name.endsWith('.test.mjs') ||
    /^test[-_].+\.(?:mjs|py|sh)$/.test(name);
}

function isUnsupportedTestLike(name) {
  const commonTestName =
    /\.(?:test|spec)\.(?:mjs|cjs|js|ts|mts|cts|py|sh)$/i.test(name) ||
    /^test[-_].+\.(?:mjs|cjs|js|ts|mts|cts|py|sh)$/i.test(name) ||
    /_test\.(?:mjs|cjs|js|ts|mts|cts|py|sh)$/i.test(name);
  return commonTestName && !isTestLike(name);
}

function groupSets(groups) {
  return Object.fromEntries(
    Object.entries(groups).map(([name, files]) => [name, new Set(files)])
  );
}

function crashSuiteFor(file, groups) {
  const name = path.posix.basename(file);
  return crashSuiteNames.find((suiteName) => groups[suiteName].has(name)) || null;
}

function siteSuite(file, crashSiteTests) {
  const name = path.posix.basename(file);
  const crashSuite = crashSuiteFor(file, crashSiteTests);
  if (crashSuite) return crashSuite;
  if (name.startsWith('app-run-')) return 'app-run';
  if (name.startsWith('backend-') ||
      name.startsWith('api-')) return 'api';
  if (name.startsWith('design-') ||
      name.startsWith('figma-') ||
      name.startsWith('observed-token-') ||
      name.startsWith('token-source-') ||
      name === 'project-adapters-bootstrap.test.mjs' ||
      name === 'session-answer-gate.test.mjs' ||
      name === 'ui-error-classification.test.mjs') return 'figma';
  return 'site';
}

function taskSuite(file, crashTaskTests) {
  return crashSuiteFor(file, crashTaskTests) || 'tasks';
}

function entry(projectRoot, nodeExecutable, file, cwd = '.') {
  const extension = path.posix.extname(file);
  let command = nodeExecutable;
  if (extension === '.py') command = 'python3';
  if (extension === '.sh') command = 'bash';
  return Object.freeze({
    label: file,
    command,
    args: Object.freeze([path.join(projectRoot, file)]),
    cwd
  });
}

export function verifyWorkspaceContract(projectRoot) {
  const rootPackage = readJson(projectRoot, 'package.json');
  assert.equal(rootPackage.private, true, 'root package must stay private');
  assert.equal(Object.hasOwn(rootPackage, 'version'), false,
    'private root workspace must not introduce a package version');
  assert.equal(rootPackage.engines?.node, '>=22',
    'root Node engine policy changed');
  assert.deepEqual(rootPackage.workspaces, workspacePaths,
    'root workspace ownership changed');
  for (const [name, command] of Object.entries(requiredRootScripts)) {
    assert.equal(rootPackage.scripts?.[name], command,
      `root package script ${name} drifted`);
  }

  const rootLock = readJson(projectRoot, 'package-lock.json');
  assert.equal(rootLock.lockfileVersion, 3, 'root lockfile must use npm lockfile v3');
  assert.deepEqual(rootLock.packages?.['']?.workspaces, workspacePaths,
    'root lockfile workspace ownership drifted');
  assert.equal(
    readFileSync(path.join(projectRoot, '.npmrc'), 'utf8'),
    'install-strategy=nested\n',
    'nested workspace installs are required for copied sidecar fixtures'
  );
  for (const pin of [
    '.nvmrc',
    'orchestrator/.nvmrc',
    'orchestrator/api-contract/.nvmrc',
    'orchestrator/figma/.nvmrc'
  ]) {
    assert.equal(readFileSync(path.join(projectRoot, pin), 'utf8'), '22\n',
      `${pin} must pin Node 22 exactly`);
  }

  for (const workspace of workspacePaths) {
    const manifest = readJson(projectRoot, path.posix.join(workspace, 'package.json'));
    assert.equal(manifest.private, true, `${workspace} must stay private`);
    assert.equal(Object.hasOwn(manifest, 'version'), false,
      `${workspace} must not introduce a package version`);
    assert.equal(existsSync(path.join(projectRoot, workspace, 'package-lock.json')), false,
      `${workspace} must not carry a competing package-lock.json`);
    assert(rootLock.packages?.[workspace], `${workspace} is absent from the root lockfile`);
    for (const [name, command] of Object.entries(delegatedWorkspaceScripts[workspace])) {
      assert.equal(manifest.scripts?.[name], command,
        `${workspace} script ${name} drifted`);
    }
  }

  assert.deepEqual(
    discover(projectRoot, 'orchestrator', (name) => name === 'package-lock.json'),
    [],
    'only the root package-lock.json is allowed'
  );
  for (const manifestPath of discover(
    projectRoot,
    'orchestrator',
    (name) => name === 'package.json'
  )) {
    const manifest = readJson(projectRoot, manifestPath);
    assert.equal(manifest.private, true, `${manifestPath} must stay private`);
    assert.equal(Object.hasOwn(manifest, 'version'), false,
      `${manifestPath} must not introduce a package version`);
  }
}

export function verifyFigmaTestAliasContract(projectRoot, figmaTests) {
  const manifest = readJson(projectRoot, 'orchestrator/figma/package.json');
  const targets = [];
  for (const [name, command] of Object.entries(manifest.scripts || {})) {
    if (!name.startsWith('figma:test:')) continue;
    for (const segment of command.split(/\s*&&\s*/)) {
      if (!/(?:^|\s)tests\//.test(segment)) continue;
      const match = segment.match(
        /^node(?:\s+--test)?\s+(tests\/[A-Za-z0-9._/-]+\.test\.mjs)$/
      );
      assert(match, `${name} has a non-canonical Figma test command: ${segment}`);
      targets.push(path.posix.join('orchestrator/figma', match[1]));
    }
  }
  assert.deepEqual(
    targets.sort(),
    [...figmaTests].sort(),
    'Every Figma test must have exactly one canonical direct package alias'
  );
}

export function createSuiteCatalog(projectRoot, nodeExecutable) {
  const siteTests = discover(
    projectRoot,
    'orchestrator/site/tests',
    (name) => name.endsWith('.test.mjs')
  );
  const taskTests = discover(
    projectRoot,
    'orchestrator/tasks/tests',
    (name) => /^test[-_].+\.(?:mjs|py|sh)$/.test(name)
  );
  const figmaTests = discover(
    projectRoot,
    'orchestrator/figma/tests',
    (name) => name.endsWith('.test.mjs')
  );
  const discoveredProjectTests = discover(
    projectRoot,
    'orchestrator',
    (name) => isTestLike(name)
  );
  const unsupportedProjectTests = discover(
    projectRoot,
    'orchestrator',
    (name) => isUnsupportedTestLike(name)
  );
  assert.deepEqual(
    unsupportedProjectTests,
    [],
    'Unsupported test-like filenames would be skipped; use *.test.mjs or test[-_]*.(mjs|py|sh)'
  );
  assert.deepEqual(
    discoverSymlinks(projectRoot, 'orchestrator'),
    [],
    'Symlinks are forbidden under orchestrator/ outside ignored dependency/cache directories'
  );

  const crashSiteTests = groupSets(crashSiteTestNames);
  const crashTaskTests = groupSets(crashTaskTestNames);
  const discoveredSiteNames = new Set(siteTests.map((file) => path.posix.basename(file)));
  const discoveredTaskNames = new Set(taskTests.map((file) => path.posix.basename(file)));
  for (const suiteName of crashSuiteNames) {
    for (const name of crashSiteTests[suiteName]) {
      assert(discoveredSiteNames.has(name), `${suiteName} references missing Site test ${name}`);
    }
    for (const name of crashTaskTests[suiteName]) {
      assert(discoveredTaskNames.has(name), `${suiteName} references missing task test ${name}`);
    }
  }

  const leafSuites = Object.create(null);
  leafSuiteNames.forEach((name) => { leafSuites[name] = []; });

  for (const file of siteTests) {
    leafSuites[siteSuite(file, crashSiteTests)].push(
      entry(projectRoot, nodeExecutable, file)
    );
  }
  for (const file of taskTests) {
    leafSuites[taskSuite(file, crashTaskTests)].push(
      entry(projectRoot, nodeExecutable, file)
    );
  }
  for (const file of figmaTests) {
    leafSuites.figma.push(
      entry(projectRoot, nodeExecutable, file, 'orchestrator/figma')
    );
  }
  for (const file of toolingTests) {
    leafSuites.tooling.push(entry(projectRoot, nodeExecutable, file));
  }

  leafSuites.figma.unshift(
    Object.freeze({
      label: 'figma:doctor',
      command: nodeExecutable,
      args: Object.freeze([
        path.join(projectRoot, 'orchestrator/figma/scripts/doctor.mjs')
      ]),
      cwd: 'orchestrator/figma'
    }),
    Object.freeze({
      label: 'figma:security:grep',
      command: nodeExecutable,
      args: Object.freeze([
        path.join(projectRoot, 'orchestrator/figma/scripts/check-direct-figma-access.mjs')
      ]),
      cwd: 'orchestrator/figma'
    })
  );
  leafSuites.figma.push(Object.freeze({
    label: 'figma:verify-done',
    command: nodeExecutable,
    args: Object.freeze([
      path.join(projectRoot, 'orchestrator/figma/scripts/verify-done.mjs')
    ]),
    cwd: '.'
  }));

  const ownedTestFiles = new Map();
  for (const [suiteName, entries] of Object.entries(leafSuites)) {
    for (const item of entries) {
      if (!isTestLike(path.posix.basename(item.label))) continue;
      assert(!ownedTestFiles.has(item.label),
        `${item.label} is owned by both ${ownedTestFiles.get(item.label)} and ${suiteName}`);
      ownedTestFiles.set(item.label, suiteName);
    }
  }
  assert.deepEqual(
    [...ownedTestFiles.keys()].sort(),
    discoveredProjectTests,
    'Every test-like file under orchestrator/ must have exactly one suite owner'
  );

  const ownershipSummary = Object.freeze(Object.fromEntries(
    leafSuiteNames.map((name) => [
      name,
      leafSuites[name].filter((item) => ownedTestFiles.has(item.label)).length
    ])
  ));
  assert.deepEqual(
    ownershipSummary,
    expectedOwnershipSummary,
    'Mandatory suite ownership counts changed'
  );

  const entriesBySuite = Object.create(null);
  for (const name of leafSuiteNames) {
    entriesBySuite[name] = Object.freeze([...leafSuites[name]]);
  }
  entriesBySuite['crash-recovery'] = Object.freeze(
    crashSuiteNames.flatMap((name) => leafSuites[name])
  );

  return Object.freeze({
    availableSuiteNames: Object.freeze([...leafSuiteNames, 'crash-recovery']),
    discoveredProjectTests: Object.freeze([...discoveredProjectTests]),
    entriesBySuite: Object.freeze(entriesBySuite),
    figmaTests: Object.freeze([...figmaTests]),
    ownershipSummary
  });
}
