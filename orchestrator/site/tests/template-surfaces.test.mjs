import assert from 'node:assert/strict';
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { dirname, join, relative, resolve, sep } from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

import { helpers, wizardSteps } from '../scripts/data/wizard-steps.js';
import {
  dictionaryFor,
  localeText
} from './i18n-test-helpers.mjs';

const siteDir = join(dirname(fileURLToPath(import.meta.url)), '..');
const orchestratorDir = join(siteDir, '..');
const launch = readFileSync(join(orchestratorDir, 'launch.md'), 'utf8');
const setupSource = readFileSync(join(siteDir, 'scripts', 'panels', 'setup.js'), 'utf8');
const enSource = localeText('en');
const ruSource = localeText('ru');
const ukSource = localeText('uk');
const packageReference = readFileSync(join(orchestratorDir, 'skills', 'design-system', 'references', 'packages.md'), 'utf8');
const templateConventions = readFileSync(join(orchestratorDir, 'skills', 'launch-readiness', 'references', 'template-conventions.md'), 'utf8');

function filesUnder(directory, extension) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    if (entry.isDirectory() && ['node_modules', '.cache'].includes(entry.name)) return [];
    const absolute = join(directory, entry.name);
    return entry.isDirectory() ? filesUnder(absolute, extension) : (absolute.endsWith(extension) ? [absolute] : []);
  });
}

function resolveLocalModule(fromFile, specifier) {
  if (!specifier.startsWith('.')) return null;
  const base = resolve(dirname(fromFile), specifier);
  const candidates = [base, `${base}.js`, `${base}.mjs`, `${base}.cjs`, join(base, 'index.js')];
  return candidates.find((candidate) => existsSync(candidate)) || null;
}

function unreachableModules(files, roots, dependencyPattern) {
  const fileSet = new Set(files);
  const edges = new Map(files.map((file) => {
    const source = readFileSync(file, 'utf8');
    const dependencies = [...source.matchAll(dependencyPattern)]
      .map((match) => resolveLocalModule(file, match[1]))
      .filter((dependency) => dependency && fileSet.has(dependency));
    return [file, [...new Set(dependencies)]];
  }));
  const reachable = new Set();
  const pending = roots.slice();
  while (pending.length) {
    const file = pending.pop();
    if (reachable.has(file)) continue;
    reachable.add(file);
    pending.push(...(edges.get(file) || []));
  }
  return files
    .filter((file) => !reachable.has(file))
    .map((file) => relative(siteDir, file).replaceAll('\\', '/'))
    .sort();
}

function occurrences(source, value) {
  let count = 0;
  let offset = 0;
  while ((offset = source.indexOf(value, offset)) >= 0) {
    count += 1;
    offset += value.length;
  }
  return count;
}

function exportedNames(source) {
  const names = [];
  for (const match of source.matchAll(/\bexport\s+(?:default\s+)?(?:async\s+)?(?:function|class|const|let|var)\s+([A-Za-z_$][\w$]*)/g)) {
    names.push(match[1]);
  }
  for (const match of source.matchAll(/\bexport\s*\{([^}]+)\}/g)) {
    for (const part of match[1].split(',')) {
      const name = (part.trim().split(/\s+as\s+/).pop() || '').trim();
      if (/^[A-Za-z_$][\w$]*$/.test(name)) names.push(name);
    }
  }
  for (const block of source.matchAll(/module\.exports\s*=\s*\{([\s\S]*?)\};/g)) {
    for (const match of block[1].matchAll(/(?:^|,)\s*([A-Za-z_$][\w$]*)\s*(?=[:,])/gm)) names.push(match[1]);
  }
  return [...new Set(names)];
}

const browserModules = filesUnder(join(siteDir, 'scripts'), '.js');
assert.deepEqual(
  unreachableModules(browserModules, [join(siteDir, 'scripts', 'app.js')], /\bimport\s+(?:[^'";]+?\s+from\s+)?['"]([^'"]+)['"]/g),
  [],
  'every browser module must be reachable from scripts/app.js',
);
const serverModules = [join(siteDir, 'server.js'), ...filesUnder(join(siteDir, 'server'), '.js')];
const fileGuardWorker = join(siteDir, 'server', 'file-guard-worker.js');
const apiMockWorker = join(siteDir, 'server', 'api-mock-worker.js');
assert.match(readFileSync(join(siteDir, 'server', 'file-guards.js'), 'utf8'), /file-guard-worker\.js/, 'file-guard worker must remain a subprocess entrypoint');
assert.match(readFileSync(join(siteDir, 'server', 'api-mock.js'), 'utf8'), /api-mock-worker\.js/, 'API mock worker must remain a subprocess entrypoint');
assert.deepEqual(
  unreachableModules(serverModules, [join(siteDir, 'server.js'), fileGuardWorker, apiMockWorker], /\brequire\(['"]([^'"]+)['"]\)/g),
  [],
  'every server module must be reachable from server.js or an explicit subprocess entrypoint',
);

const repositoryModules = ['.js', '.mjs', '.cjs'].flatMap((extension) => filesUnder(orchestratorDir, extension));
const repositorySources = repositoryModules.map((file) => [file, readFileSync(file, 'utf8')]);
const unconsumedExports = repositorySources.flatMap(([file, source]) => exportedNames(source).flatMap((name) => {
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const consumerPattern = new RegExp(`(^|[^\\w$])${escaped}([^\\w$]|$)`);
  const consumed = repositorySources.some(([candidate, candidateSource]) => candidate !== file && consumerPattern.test(candidateSource));
  return consumed ? [] : [`${relative(dirname(orchestratorDir), file).replaceAll('\\', '/')}: ${name}`];
})).sort();
assert.deepEqual(unconsumedExports, [], 'self-contained modules must not export symbols with no repository consumer');

const enKeys = Object.keys(dictionaryFor('en')).sort();
const ruKeys = Object.keys(dictionaryFor('ru')).sort();
const ukKeys = Object.keys(dictionaryFor('uk')).sort();
assert.deepEqual([...new Set(enKeys)], enKeys, 'EN locale keys must be unique');
assert.deepEqual([...new Set(ruKeys)], ruKeys, 'RU locale keys must be unique');
assert.deepEqual([...new Set(ukKeys)], ukKeys, 'UK locale keys must be unique');
assert.deepEqual(ruKeys, enKeys, 'EN and RU locales must expose the same keys');
assert.deepEqual(ukKeys, enKeys, 'EN and UK locales must expose the same keys');
for (const file of [join(siteDir, 'index.html'), ...browserModules]) {
  const source = readFileSync(file, 'utf8');
  assert.doesNotMatch(
    source,
    /\b[a-z][a-z0-9_-]*\.v[1-9][0-9]*\./i,
    `${relative(siteDir, file)} must not use versioned UI or localization namespaces`,
  );
  assert.doesNotMatch(
    source,
    /\b[a-z][a-z0-9_.-]*V[1-9][0-9]*\b/,
    `${relative(siteDir, file)} must not use version-suffixed UI or localization keys`,
  );
}
for (const file of filesUnder(join(siteDir, 'styles'), '.css')) {
  assert.doesNotMatch(
    readFileSync(file, 'utf8'),
    /\b[a-z][a-z0-9_-]*-v[1-9][0-9]*(?:\b|-)/i,
    `${relative(siteDir, file)} must not use versioned component class names`,
  );
}
const browserRuntimeSource = [
  readFileSync(join(siteDir, 'index.html'), 'utf8'),
  ...browserModules
    .filter((file) => !file.startsWith(join(siteDir, 'scripts', 'i18n') + sep))
    .map((file) => readFileSync(file, 'utf8')),
].join('\n');
const dynamicI18nPrefixes = [...new Set(
  [
    ...browserRuntimeSource.matchAll(/['"]([A-Za-z0-9_.-]+\.)['"]\s*\+/g),
    ...browserRuntimeSource.matchAll(/\blocalizedEnum\(\s*['"]([A-Za-z0-9_.-]+\.)['"]/g),
  ].map((match) => match[1]),
)];
const pluralFamilies = [...new Set(
  [...browserRuntimeSource.matchAll(/\bplural(?:Label|Template)\(['"]([^'"]+)['"]/g)].map((match) => match[1]),
)];
const unexplainedLocaleKeys = enKeys.filter((key) => {
  const pluralPrefixReferences = pluralFamilies.includes(key) ? 1 : 0;
  const hasStaticUse = occurrences(browserRuntimeSource, key) > pluralPrefixReferences;
  const hasDynamicUse = dynamicI18nPrefixes.some((prefix) => key.startsWith(prefix));
  const hasPluralUse = pluralFamilies.some((prefix) => key.startsWith(`${prefix}.`));
  return !hasStaticUse && !hasDynamicUse && !hasPluralUse;
});
assert.deepEqual(unexplainedLocaleKeys, [], 'locale keys must have a static, dynamic-prefix, or plural-family consumer');

const illustrativeIdentity = {
  productName: 'SampleApp',
  orgName: 'example',
  applicationId: 'com.example.sampleapp',
};
assert.match(setupSource, /name: 'productName',[\s\S]{0,100}placeholder: 'SampleApp'/, 'Setup product example must stay neutral');
assert.match(setupSource, /name: 'orgName',[\s\S]{0,100}placeholder: 'example'/, 'Setup organization example must stay neutral');
assert.match(setupSource, /name: 'applicationId',[\s\S]{0,100}placeholder: 'com\.example\.sampleapp'/, 'Setup application id example must stay neutral');
assert.match(launch, new RegExp(`Product name.*${illustrativeIdentity.productName}`), 'launch.md must mirror the Setup product example');
assert.match(launch, new RegExp(`Organization name.*${illustrativeIdentity.orgName}`), 'launch.md must mirror the Setup organization example');
assert.match(launch, new RegExp(`Application ID.*${illustrativeIdentity.applicationId.replaceAll('.', '\\.')}`), 'launch.md must mirror the Setup application id example');
assert.match(enSource, new RegExp(illustrativeIdentity.applicationId.replaceAll('.', '\\.')), 'EN application id hint must mirror the Setup example');
assert.match(ruSource, new RegExp(illustrativeIdentity.applicationId.replaceAll('.', '\\.')), 'RU application id hint must mirror the Setup example');
assert.match(packageReference, /organization root \(e\.g\. `example`\)/, 'package reference must use the neutral organization example');
assert.match(packageReference, /product name \(e\.g\. `sampleapp`\)/, 'package reference must use the neutral product example');
assert.match(templateConventions, /`com\.example\.sampleapp`/, 'template conventions must mirror the neutral application id');
for (const source of [launch, setupSource, enSource, ruSource, packageReference, templateConventions]) {
  assert.doesNotMatch(source, /placeholder: 'Pulse'|Product name.*`pulse`|com\.acme\.pulse|com\.pulse\b|\bFitTrack\b|\bfittrack(?:-app)?\b|\bacme\b/, 'template surfaces must not carry a product-specific example');
}

const ids = wizardSteps.map((step) => step.id);
assert.equal(new Set(ids).size, ids.length, 'wizard step ids must be unique');

const setup = {
  productName: 'Example',
  orgName: 'example',
  backendHost: 'api.example.com',
  applicationId: 'com.example.app',
  iosFrameworkName: 'shared',
  typefaceFactory: 'inter',
  firstDomain: 'Item',
  supportedLocales: ['en'],
  authMethods: [],
  iosEnabled: true,
  firebaseEnabled: true,
  codexEnabled: 'auto',
  prelaunch: true,
  figmaEnabled: false,
  screenshotPixelGate: 'strict',
};

for (const step of wizardSteps) {
  const surfaces = Number(typeof step.promptTemplate === 'string') + Number(typeof step.build === 'function');
  assert.equal(surfaces, 1, `wizard step ${step.id} must define exactly one prompt surface`);
  const escapedId = step.id.replace('.', '\\.');
  assert.match(step.title, new RegExp(`^Step ${escapedId} — `), `wizard step ${step.id} title must match its id`);
  assert.equal(typeof step.verifyHint, 'string', `wizard step ${step.id} must have a verify hint`);
  assert.ok(step.verifyHint.trim(), `wizard step ${step.id} verify hint must not be blank`);
  assert.match(launch, new RegExp(`^## Step ${escapedId}\\b`, 'm'), `launch.md must define wizard step ${step.id}`);

  const prompt = typeof step.build === 'function' ? step.build(setup) : step.promptTemplate;
  assert.ok(prompt.trim(), `wizard step ${step.id} prompt must not be blank`);
  const lines = prompt.replace(/\r\n/g, '\n').split('\n');
  for (let index = 1; index < lines.length; index += 1) {
    if (lines[index].trim() && lines[index] === lines[index - 1]) {
      assert.fail(`wizard step ${step.id} repeats line ${index + 1}: ${lines[index]}`);
    }
  }
}

// The bootstrap is unattended: every step prompt sent to the setup session must
// carry the shared autonomy footer (a step ending its turn with a question
// wedges auto-run in awaiting-input). helpers.stepPrompt is the ONE renderer —
// display, Copy, per-step Run, and auto-run all go through it — and the
// normative docs carry the matching rule.
assert.ok(typeof helpers.autonomyFooter === 'string' && helpers.autonomyFooter.includes('never ask'),
  'wizard-steps.js must export the unattended-run autonomy footer');
assert.ok(helpers.stepPrompt(wizardSteps[0], setup).endsWith(`\n\n${helpers.autonomyFooter}`),
  'helpers.stepPrompt must append the autonomy footer to every step prompt');
const wizardPanelSource = readFileSync(join(siteDir, 'scripts', 'panels', 'wizard.js'), 'utf8');
const autoRunSource = readFileSync(join(siteDir, 'scripts', 'auto-run.js'), 'utf8');
assert.match(wizardPanelSource, /helpers\.stepPrompt\(/, 'panels/wizard.js must render step prompts via helpers.stepPrompt');
assert.match(autoRunSource, /helpers\.stepPrompt\(/, 'auto-run.js must send step prompts via helpers.stepPrompt');
assert.match(launch, /^## Unattended run — never ask$/m, 'launch.md must carry the unattended-run ground rule');

// Step 12's stub gate is ONE implementation (foundation-stub-scan.js) shared
// by the agent-run CLI and the server ✓ validator — the step marks itself done
// the moment the gate passes; a raw rg would false-positive on comments.
const step12Prompt = wizardSteps.find((step) => step.id === '12').build(setup);
assert.match(step12Prompt, /node orchestrator\/site\/server\/foundation-stub-scan\.js/,
  'step 12 gate must run the shared foundation-stub-scan CLI');
assert.doesNotMatch(step12Prompt, /rg -n/, 'step 12 gate must not re-derive the scan with raw rg');
assert.match(launch, /node orchestrator\/site\/server\/foundation-stub-scan\.js/,
  'launch.md Step 12 must run the shared foundation-stub-scan CLI');
assert.doesNotMatch(launch, /ask before replacing|only after asking the user|confirms replacement/,
  'launch.md must not instruct the bootstrap to interview the user (Step 13 is autonomous)');
for (const step of wizardSteps) {
  const prompt = typeof step.build === 'function' ? step.build(setup) : step.promptTemplate;
  assert.doesNotMatch(prompt, /ask before replacing|only after asking the user|asks? the user for/i,
    `wizard step ${step.id} prompt must not instruct asking the user`);
}

// project-config.md is product-owned after bootstrap, so this exact mirror
// check applies only to the fresh source template (identified by its required
// identity placeholder). Product copies still run the wizard checks above.
const projectConfig = readFileSync(join(orchestratorDir, 'project-config.md'), 'utf8').replace(/\r\n/g, '\n');
if (/^productName: <Product>$/m.test(projectConfig)) {
  const configMatch = projectConfig.match(/^---\n[\s\S]*?\n---\n([\s\S]*)$/);
  assert.ok(configMatch, 'project-config.md must contain YAML frontmatter followed by a body');

  const mirrorMatch = setupSource.match(/var CONFIG_BODY = (\[[\s\S]*?\n\s*\])\.join\('\\n'\);/);
  assert.ok(mirrorMatch, 'setup.js must define the CONFIG_BODY mirror');
  const mirroredBody = vm.runInNewContext(`(${mirrorMatch[1]}).join('\\n')`);
  const normalize = (value) => value.replace(/\r\n/g, '\n').replace(/^\n+/, '').replace(/\n+$/, '');
  assert.equal(normalize(mirroredBody), normalize(configMatch[1]), 'setup.js CONFIG_BODY must match project-config.md');
}

console.log(`ok - ${wizardSteps.length} wizard steps and template mirrors`);
