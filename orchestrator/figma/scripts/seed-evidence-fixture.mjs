import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { FIGMA_CACHE_ROOT, artifactSegment, loadScreenshotThresholds } from './_util.mjs';
import { readSupportedLocales } from './lib/design-locale.mjs';
import { CAPTURE_CONFIG_DISCOVERY_KEY, captureConfigDiscovery } from './lib/capture-config-discovery.mjs';
import { assertTaskStem } from './report-utils.mjs';

const cache = FIGMA_CACHE_ROOT;
const reportsDir = path.join(cache, 'reports');
const artifactsDir = path.join(cache, 'artifacts/screenshot');
const args = process.argv.slice(2);
const flags = new Set(args.filter((arg) => String(arg).startsWith('--')));
const positional = args.filter((arg) => !String(arg).startsWith('--'));
const stem = positional[0] || 'TASK_9999_figma_evidence_fixture';
const runId = positional[1] || 'fixture-run-1';
const cleanup = flags.has('--cleanup');
const generatedAt = new Date().toISOString();
const artifactSetId = `${stem}_Home_primary`;
const artifactStem = artifactSegment(stem);
const artifactRunId = artifactSegment(runId);
const thresholds = loadScreenshotThresholds();

try { assertTaskStem(stem); }
catch {
  console.error('seed-evidence-fixture: stem must be canonical, like TASK_9999_figma_evidence_fixture');
  process.exit(2);
}
// First char must be alphanumeric so a single-segment runId can never be `.`
// or `..` — otherwise `--cleanup TASK_… ..` would resolve the rm target up to
// artifacts/screenshot/ and recursively delete every task's compare artifacts.
if (!/^[A-Za-z0-9][A-Za-z0-9_.-]{0,119}$/.test(runId)) {
  console.error('seed-evidence-fixture: run id must start with a letter or number and contain only letters, numbers, dot, underscore, or dash');
  process.exit(2);
}
const unknownFlag = [...flags].find((flag) => flag !== '--cleanup');
if (unknownFlag) {
  console.error(`seed-evidence-fixture: unknown flag ${unknownFlag}`);
  process.exit(2);
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function sha256(buf) {
  return 'sha256:' + crypto.createHash('sha256').update(buf).digest('hex');
}

function writeJson(file, data) {
  ensureDir(path.dirname(file));
  fs.writeFileSync(file, JSON.stringify(data, null, 2) + '\n');
}

function removeIfExists(file) {
  try { fs.rmSync(file, { recursive: true, force: true }); } catch {}
}

if (cleanup) {
  for (const name of ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot', 'evidence']) {
    removeIfExists(path.join(reportsDir, `${name}-${stem}.json`));
  }
  removeIfExists(path.join(cache, 'inputs', stem));
  // Containment guard on the recursive rm: never let the target escape the
  // per-task artifacts dir even if the runId validation above is ever loosened.
  const artifactRunDir = path.join(artifactsDir, artifactStem, artifactRunId);
  const artifactRel = path.relative(artifactsDir, artifactRunDir);
  if (artifactRel && !artifactRel.startsWith('..') && !path.isAbsolute(artifactRel)) {
    removeIfExists(artifactRunDir);
  }
  console.log(JSON.stringify({ stem, runId, cleanup: true }, null, 2));
  process.exit(0);
}

function fixtureInputHash(name) {
  const rel = `inputs/${stem}/${name}.txt`;
  const file = path.join(cache, rel);
  ensureDir(path.dirname(file));
  const bytes = Buffer.from(`${name}:${stem}:${runId}\n`);
  fs.writeFileSync(file, bytes);
  return { rel, hash: sha256(bytes) };
}

function writeArtifact(kind, bytes) {
  const rel = `artifacts/screenshot/${artifactStem}/${artifactRunId}/${artifactSetId}/${kind}.png`;
  const file = path.join(cache, rel);
  ensureDir(path.dirname(file));
  fs.writeFileSync(file, bytes);
  return {
    id: `${stem}_${kind}`,
    kind,
    path: rel,
    hash: sha256(bytes),
    bytes: bytes.length,
    width: 1,
    height: 1,
    mime: 'image/png',
  };
}

function writeManifest() {
  const rel = `artifacts/screenshot/${artifactStem}/${artifactRunId}/${artifactSetId}/manifest.json`;
  const file = path.join(cache, rel);
  const bytes = Buffer.from(JSON.stringify({ schemaVersion: 1, stem, runId, artifactSetId }, null, 2) + '\n');
  ensureDir(path.dirname(file));
  fs.writeFileSync(file, bytes);
  return {
    id: `${stem}_manifest`,
    kind: 'manifest',
    path: rel,
    hash: sha256(bytes),
    bytes: bytes.length,
    mime: 'application/json',
  };
}

ensureDir(reportsDir);
ensureDir(path.join(artifactsDir, artifactStem, artifactRunId, artifactSetId));

// 1x1 transparent PNG.
const png = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=',
  'base64'
);
const figma = writeArtifact('figma', png);
const actual = writeArtifact('actual', png);
const diff = writeArtifact('diff', png);
const overlay = writeArtifact('overlay', png);
const manifest = writeManifest();

function bodyFor(name) {
  if (name === 'screen-cache') return { screens: [{ screen: 'Home', status: 'complete', themes: {} }] };
  if (name === 'check-spec') return { files: [{ file: 'Home.spec.json', status: 'PASS' }] };
  if (name === 'capture-config') return {
    version: 1,
    fixed: [],
    designLocale: { language: null, reason: 'no-signal' },
    designLocaleEnvOverrides: [],
  };
  if (name === 'census') return {
    version: 2,
    screens: {},
    components: [],
    missing: [],
    incomplete: [],
    ambiguous: [],
    unsupported: [],
    retired: [],
    sourceStale: [],
  };
  if (name === 'spec') return { screens: [{ screen: 'Home', verdict: 'PASS' }] };
  if (name === 'spec-compare') return {
    engineVersion: 'spec-compare-v1',
    unresolvedRefs: [],
    implementationModel: null,
    comparisons: [],
    widgetClasses: {},
    implementation: { files: [], screenMap: {}, tokenCount: 0, tokenValueCount: 0, rawColorCount: 0, rawDpCount: 0 },
  };
  return {};
}

const baseReport = (name, overall = 'PASS', mode = 'gate') => {
  const input = fixtureInputHash(name);
  const report = {
    schemaVersion: 1,
    gatePolicyVersion: thresholds.version,
    taskStem: stem,
    pipelineRunId: runId,
    mode,
    generatedAt,
    inputs: name === 'screenshot' ? { captureStartedAt: generatedAt } : {},
    inputHashes: name === 'census' || name === 'evidence' ? {} : { [input.rel]: input.hash },
    overall,
    blockingCount: 0,
    warningCount: 0,
    issues: [],
    reportPath: `reports/${name}-${stem}.json`,
    ...bodyFor(name),
  };
  if (name === 'capture-config') {
    const discoveryRoot = path.join(cache, 'inputs', stem);
    const discoveryScreensDir = path.join(cache, 'screens', stem);
    const discovery = captureConfigDiscovery({ codeRoots: [discoveryRoot], screensDir: discoveryScreensDir, supportedLocales: readSupportedLocales() });
    report.inputs = {
      screensDir: `screens/${stem}`,
      codeRoots: [`inputs/${stem}`],
      specs: 1,
      testFilesScanned: discovery.files.length,
      fixed: 0,
      captureDiscovery: { version: discovery.version, roots: discovery.roots, screensDir: discovery.screensDir, digest: discovery.digest },
    };
    report.inputHashes[CAPTURE_CONFIG_DISCOVERY_KEY] = discovery.digest;
  }
  return report;
};

for (const name of ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare']) {
  writeJson(path.join(reportsDir, `${name}-${stem}.json`), baseReport(name));
}

const screenshot = {
  ...baseReport('screenshot', 'WARN'),
  metric: thresholds.metric,
  thresholds: {
    pass: thresholds.pass,
    minor: thresholds.minor,
    major: thresholds.major,
    aspectTolerance: thresholds.aspectTolerance,
    minCoverage: thresholds.minCoverage,
    bgTolerance: thresholds.bgTolerance,
    deltaEPass: thresholds.deltaEPass,
    majorBand: 'block',
    colorAxis: true,
    extraContentBand: 'warn',
    extraContentWarn: thresholds.extraContentWarn,
    extraContentDeltaE: thresholds.extraContentDeltaE,
    extraContentRingPx: thresholds.extraContentRingPx,
    shiftRadius: thresholds.shiftRadius,
    gaussianSigma: thresholds.gaussianSigma,
    aaTolerance: thresholds.aaTolerance,
    varFloor: thresholds.varFloor,
    maskMode: 'variance',
    deltaEStride: thresholds.deltaEStride,
    regionGrid: '8x4',
    zoneGate: thresholds.zoneGate,
    zoneBlocker: thresholds.zoneBlocker,
    zoneTextBlocker: thresholds.zoneTextBlocker,
    minRegionPx: thresholds.minRegionPx,
    statusBarDp: thresholds.statusBarDp,
    navBarDp: thresholds.navBarDp,
    pixelGate: 'strict',
  },
  semantic: { enabled: false, status: 'DISABLED', promoted: false, zones: [], findings: [] },
  warningCount: 1,
  issues: [{
    severity: 'WARN',
    issueKind: 'SSIM_MINOR',
    message: 'Fixture visual difference for local API smoke.',
    screen: 'Home',
    theme: 'primary',
    artifactSet: null,
  }],
  results: [{
    screen: 'Home',
    theme: 'primary',
    themeKey: 'primary',
    status: 'MINOR',
    score: 0.97,
    coverage: 1,
    reason: 'Fixture visual difference for local API smoke.',
    artifactSet: {
      schemaVersion: 1,
      id: artifactSetId,
      screen: 'Home',
      theme: 'primary',
      status: 'MINOR',
      score: 0.97,
      coverage: 1,
      dimensions: { width: 1, height: 1 },
      manifest,
      artifacts: { figma, actual, diff, overlay },
    },
  }],
};
screenshot.issues[0].artifactSet = screenshot.results[0].artifactSet;
writeJson(path.join(reportsDir, `screenshot-${stem}.json`), screenshot);

const requiredReports = ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot'];
const evidence = {
  ...baseReport('evidence', 'WARN', 'transport'),
  stage: 'final',
  requiredReports,
  inputHashes: Object.fromEntries(requiredReports.map((name) => {
    const rel = `reports/${name}-${stem}.json`;
    return [rel, sha256(fs.readFileSync(path.join(reportsDir, `${name}-${stem}.json`)))];
  })),
  reports: requiredReports.map((name) => ({
    name,
    exists: true,
    overall: name === 'screenshot' ? 'WARN' : 'PASS',
    path: `orchestrator/.cache/figma/reports/${name}-${stem}.json`,
  })),
};
writeJson(path.join(reportsDir, `evidence-${stem}.json`), evidence);

console.log(JSON.stringify({ stem, runId, reportsDir, artifactsDir: path.join(artifactsDir, artifactStem, artifactRunId, artifactSetId) }, null, 2));
