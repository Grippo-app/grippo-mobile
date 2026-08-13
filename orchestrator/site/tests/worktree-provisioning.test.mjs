#!/usr/bin/env node
// Phase 2 provisioning + Git mutation owner (pipeline improvement 01).
// Everything runs in temp git repos: the manager mints one execution
// generation per run, the mutation owner is the sole writer of worktrees/
// refs, and every §9.2 crash outcome is classifiable (resume-create,
// recovery-required, proven-absent) without age.

import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { chmodSync, existsSync, lstatSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, realpathSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { createHash } from 'node:crypto'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const contract = require('../../tasks/worktree-record-contract.cjs')
const repoRoot = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..')

let checks = 0
function check(name, fn) { fn(); checks++; console.log(`ok ${checks} - ${name}`) }
const roots = []
function git(cwd, ...args) {
  return execFileSync('git', args, { cwd, encoding: 'utf8', env: { ...process.env, GIT_TERMINAL_PROMPT: '0' } })
}
// Each fixture runs in a child node process so paths.js resolves against the
// fixture's ORCHESTRATOR_PROJECT_ROOT (module-load-time binding).
function runInFixture(body, extraEnv = {}) {
  const parent = mkdtempSync(join(tmpdir(), 'wt-prov-'))
  roots.push(parent)
  const root = join(parent, 'repo с пробелами')
  mkdirSync(root)
  git(root, 'init', '-q', '-b', 'main')
  git(root, 'config', 'user.email', 'fixture@test.invalid')
  git(root, 'config', 'user.name', 'Fixture')
  git(root, 'config', 'commit.gpgsign', 'false')
  mkdirSync(join(root, 'orchestrator', 'tasks', 'todo'), { recursive: true })
  mkdirSync(join(root, 'orchestrator', 'skills'), { recursive: true })
  writeFileSync(join(root, 'orchestrator', 'tasks', 'todo', 'TASK_7_probe.md'), '# Task 7\n## Goal\nprobe\n')
  writeFileSync(join(root, 'orchestrator', 'skills', 'install-skills.sh'), '#!/usr/bin/env bash\ntrue\n')
  writeFileSync(join(root, 'orchestrator', 'project-config.md'), 'config: v\n')
  git(root, 'add', '.')
  git(root, 'commit', '-q', '-m', 'init')
  const script = `
    const assert = require('node:assert/strict');
    process.env.ORCHESTRATOR_PROJECT_ROOT = ${JSON.stringify(root)};
    process.env.ORCHESTRATOR_WORKTREE_HOME = ${JSON.stringify(join(parent, '.orchestrator-worktrees'))};
    const managerTarget = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/worktree-manager.js'))});
    const taskIntegrity = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/task-integrity.js'))});
    const manager = new Proxy(managerTarget, { get(target, property) {
      if (property !== 'provision') return target[property];
      return (options) => target.provision(Object.assign({}, options, {
        sourceRevision: options.sourceRevision || taskIntegrity.validateAction('run', options.stem, 'fixture').sourceRevision
      }));
    }});
    const gitMutations = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/git-mutations.js'))});
    const contract = require(${JSON.stringify(join(repoRoot, 'orchestrator/tasks/worktree-record-contract.cjs'))});
    const { execFileSync, spawnSync } = require('node:child_process');
    const fs = require('node:fs');
    const git = (cwd, ...a) => execFileSync('git', a, { cwd, encoding: 'utf8' });
    const ROOT = ${JSON.stringify(root)};
    const PARENT = ${JSON.stringify(parent)};
    ${body}
  `
  const out = execFileSync(process.execPath, ['-e', script], {
    encoding: 'utf8', env: { ...process.env, ...extraEnv },
    cwd: root, maxBuffer: 8 * 1024 * 1024,
  })
  return { root, parent, out: JSON.parse(out) }
}

try {
  check('provision creates one verified execution generation classified managed', () => {
    const { out } = runInFixture(`
      const p = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000000-r1', requestId: '1700000000000-q1' });
      const d = manager.discover();
      const row = d.worktrees.find((w) => w.path === p.executionRoot);
      const manifest = JSON.parse(fs.readFileSync(p.manifestFile, 'utf8'));
      const record = JSON.parse(fs.readFileSync(require('node:path').join(ROOT, 'orchestrator', '.cache', 'tasks', 'worktrees', p.worktreeId + '.json'), 'utf8'));
      const snapshot = fs.readFileSync(p.taskSnapshotFile, 'utf8');
      const onCandidate = git(p.executionRoot, 'symbolic-ref', '-q', 'HEAD').trim();
      console.log(JSON.stringify({
        ok: p.ok, classification: row && row.classification, findings: d.findings.map((f) => f.code),
        manifestOk: manifest.manifestHash.startsWith('sha256:') && manifest.worktreeId === p.worktreeId,
        generationPinsMatch: manifest.taskSourceRevision === record.taskSourceRevision &&
          manifest.dependencySnapshotHash === record.dependencySnapshotHash &&
          manifest.figmaGenerationHash === record.figmaGenerationHash &&
          manifest.apiGenerationHash === record.apiGenerationHash,
        snapshotMatches: snapshot === '# Task 7\\n## Goal\\nprobe\\n',
        onCandidate, candidateRef: p.candidateRef,
        insideHome: p.executionRoot.indexOf(fs.realpathSync.native(PARENT).normalize('NFC')) === 0,
      }));
    `)
    assert.equal(out.ok, true)
    assert.equal(out.classification, 'managed')
    assert.deepEqual(out.findings, [])
    assert.equal(out.manifestOk, true)
    assert.equal(out.generationPinsMatch, true)
    assert.equal(out.snapshotMatches, true)
    assert.equal(out.onCandidate, out.candidateRef)
    assert.equal(out.insideHome, true)
  })

  check('execution helpers prove the exact manifest, task lock, and canonical target identity', () => {
    const { out } = runInFixture(`
      const p = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000000-r1', requestId: '1700000000000-q1' });
      const sessionId = 'ws-' + 'a'.repeat(32);
      const startedAt = new Date().toISOString();
      const lock = {
        version: 1, stem: 'TASK_7_probe', stage: 'orchestrator', runId: p.runId,
        sessionId, startedAt,
        owner: { kind: 'direct', id: 'fixture:' + sessionId, pid: process.pid,
          processStartId: null, hostname: require('node:os').hostname(), startedAt },
      };
      const locksDir = require('node:path').join(ROOT, 'orchestrator', '.cache', 'tasks', 'locks');
      fs.mkdirSync(locksDir, { recursive: true });
      fs.writeFileSync(require('node:path').join(locksDir, 'TASK_7_probe.json'),
        JSON.stringify(lock, null, 2) + '\\n');
      Object.assign(process.env, {
        ORCHESTRATOR_PROJECT_ROOT: ROOT,
        ORCHESTRATOR_EXECUTION_ROOT: p.executionRoot,
        ORCHESTRATOR_EXECUTION_MANIFEST: p.manifestFile,
        ORCHESTRATOR_TASK_SNAPSHOT_FILE: p.taskSnapshotFile,
        ORCHESTRATOR_TASK_SNAPSHOT_HASH: p.taskSnapshotHash,
        ORCHESTRATOR_WORKTREE_ID: p.worktreeId,
        ORCHESTRATOR_RUN_ID: p.runId,
        ORCHESTRATOR_WRITER_STEM: 'TASK_7_probe',
        ORCHESTRATOR_WRITER_SESSION_ID: sessionId,
      });
      const verified = manager.executionEnvironmentContext(process.env);
      const wrong = manager.executionEnvironmentContext({ ...process.env,
        ORCHESTRATOR_EXECUTION_MANIFEST: p.manifestFile + '.foreign' });
      const projectIdentity = require(${JSON.stringify(join(repoRoot, 'orchestrator/figma/runtime/project-identity.cjs'))});
      const branchKeyBeforeTaskRace = projectIdentity.projectBranchKey(p.executionRoot);
      import(require('node:url').pathToFileURL(
        ${JSON.stringify(join(repoRoot, 'orchestrator/figma/scripts/_util.mjs'))}
      ).href + '?task-source=' + Date.now()).then((figmaUtil) => {
        process.env.FIGMA_APP_TOKENS = require('node:path').join(
          ROOT, 'orchestrator', 'project-config.md');
        let scalarRuntimeInputError = null;
        const scalarProbe = import(require('node:url').pathToFileURL(
          ${JSON.stringify(join(repoRoot, 'orchestrator/figma/scripts/_util.mjs'))}
        ).href + '?scalar-runtime-input=' + Date.now()).catch((error) => {
          scalarRuntimeInputError = error.message;
        }).finally(() => { delete process.env.FIGMA_APP_TOKENS; });
        let captureRuntimeInputError = null;
        return scalarProbe.then(async () => {
          process.env.ROBORAZZI_OUTPUT_DIR = PARENT;
          try {
            await import(require('node:url').pathToFileURL(
              ${JSON.stringify(join(repoRoot, 'orchestrator/figma/scripts/_util.mjs'))}
            ).href + '?capture-runtime-input=' + Date.now());
          } catch (error) { captureRuntimeInputError = error.message; }
          finally { delete process.env.ROBORAZZI_OUTPUT_DIR; }
          fs.writeFileSync(require('node:path').join(ROOT, 'orchestrator', 'tasks', 'todo', 'TASK_7_probe.md'),
            '# Task 7\\n## Goal\\ncontrol changed after helper proof\\n');
          fs.writeFileSync(require('node:path').join(p.executionRoot, 'orchestrator', 'project-config.md'),
            'config: changed after helper proof\\n');
          const figmaConfigValue = figmaUtil.readConfig('config');
          const originalArgv = process.argv;
          let externalRootError = null;
          try {
            process.argv = ['node', 'fixture', '--code-root', ROOT];
            figmaUtil.parseCli({ allowedFlags: ['--code-root'], valueFlags: ['--code-root'] });
          } catch (error) { externalRootError = error.message; }
          finally { process.argv = originalArgv; }
          const escapeLink = require('node:path').join(p.executionRoot, 'control-link');
          fs.symlinkSync(ROOT, escapeLink);
          let symlinkInputError = null;
          try { figmaUtil.executionProductInputPath(escapeLink); }
          catch (error) { symlinkInputError = error.message; }
          const figmaCache = figmaUtil.FIGMA_CACHE_ROOT;
          const reportEscapeLink = require('node:path').join(figmaCache, 'report-escape-link');
          const escapedReportDir = require('node:path').join(PARENT, 'figma-report-escape');
          fs.mkdirSync(figmaCache, { recursive: true });
          fs.mkdirSync(escapedReportDir);
          fs.symlinkSync(escapedReportDir, reportEscapeLink, 'dir');
          let symlinkReportError = null;
          try {
            figmaUtil.writeFigmaRuntimeFile(
              require('node:path').join(reportEscapeLink, 'probe.json'), '{}\\n');
          } catch (error) { symlinkReportError = error.message; }
          const escapedReportPresent = fs.existsSync(
            require('node:path').join(escapedReportDir, 'probe.json'));
          fs.unlinkSync(reportEscapeLink);
          fs.writeFileSync(require('node:path').join(
            ROOT, 'orchestrator', 'tasks', 'todo', 'TASK_7_probe.md'),
            '# Task 7\\n## Goal\\nprobe\\n');
          fs.writeFileSync(require('node:path').join(
            p.executionRoot, 'orchestrator', 'project-config.md'), 'config: v\\n');
          const externalModel = require('node:path').join(PARENT, 'external-implementation-model.json');
          fs.writeFileSync(externalModel, '{}\\n');
          const figmaReports = require('node:path').join(figmaCache, 'reports');
          fs.mkdirSync(figmaReports, { recursive: true });
          fs.writeFileSync(require('node:path').join(figmaReports, 'spec-compare-TASK_7_probe.json'),
            JSON.stringify({ inputs: { implementationModel: externalModel } }) + '\\n');
          const writeSpec = spawnSync(process.execPath,
            [${JSON.stringify(join(repoRoot, 'orchestrator/figma/scripts/write-spec-report.mjs'))},
              'TASK_7_probe', '--screen', 'Home=PASS'], {
              cwd: p.executionRoot, env: { ...process.env }, encoding: 'utf8',
              stdio: ['ignore', 'pipe', 'pipe'],
            });
          console.log(JSON.stringify({
            verified: verified.ok,
            verifiedTarget: verified.context && verified.context.targetRef,
            wrongCode: wrong.code,
            branchKey: branchKeyBeforeTaskRace,
            candidateRef: p.candidateRef,
            figmaTaskSource: fs.readFileSync(figmaUtil.FIGMA_TASK_SOURCE_FILE, 'utf8'),
            figmaTaskSourceExplicit: figmaUtil.FIGMA_TASK_SOURCE_EXPLICIT,
            figmaConfigValue,
            externalRootError,
            symlinkInputError,
            scalarRuntimeInputError,
            captureRuntimeInputError,
            symlinkReportError,
            escapedReportPresent,
            recordedExternalModelError: writeSpec.stderr,
          }));
        });
      }).catch((error) => { console.error(error); process.exitCode = 1; });
    `)
    assert.equal(out.verified, true)
    assert.equal(out.verifiedTarget, 'refs/heads/main')
    assert.equal(out.wrongCode, 'EXECUTION_ENVIRONMENT_MISMATCH')
    assert.equal(out.branchKey, 'refs/heads/main',
      'a manager candidate branch must use its verified canonical target identity')
    assert.notEqual(out.branchKey, out.candidateRef)
    assert.equal(out.figmaTaskSource, '# Task 7\n## Goal\nprobe\n',
      'Figma gates must read the immutable task snapshot, not the changed control task')
    assert.equal(out.figmaTaskSourceExplicit, false)
    assert.equal(out.figmaConfigValue, 'v',
      'Figma helpers must keep the manifest-pinned project-config snapshot after helper proof')
    assert.match(out.externalRootError, /must stay under/,
      'task Figma CLI roots must not redirect a gate to the control tree')
    assert.match(out.symlinkInputError, /regular single-link|escapes/,
      'task Figma inputs must not follow a candidate symlink to control data')
    assert.match(out.scalarRuntimeInputError, /must stay under/,
      'task Figma scalar runtime inputs must not redirect to control or external files')
    assert.match(out.captureRuntimeInputError, /must stay under/,
      'task screenshot capture roots must not redirect to host or control files')
    assert.match(out.symlinkReportError, /unsafe output ancestor|publication directory is unsafe/,
      'task Figma report publication must reject a symlinked cache namespace')
    assert.equal(out.escapedReportPresent, false,
      'task Figma report publication must not write through a symlinked cache namespace')
    assert.match(out.recordedExternalModelError, /must stay under/,
      'a task Figma report reader must re-confine paths recorded in mutable machine evidence')
  })

  check('API task helpers read the manifest-pinned checkout generation after control refreshes', () => {
    const { out } = runInFixture(`
      (async () => {
        const path = require('node:path');
        const generation = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/contract-generation.js'))});
        const stage = path.join(ROOT, 'orchestrator', '.cache', 'api-fixture-stage');
        function publish(expected, title) {
          fs.mkdirSync(path.join(stage, 'areas'), { recursive: true });
          fs.writeFileSync(path.join(stage, 'inventory.json'), JSON.stringify({
            schemaVersion: 1, source: { kind: 'openapi', title,
              fetchedAt: new Date().toISOString(), openApiUrl: null, openApiVersion: '3.1.0',
              postmanImportedAt: null, specHash: null },
            areas: { notes: ['listNotes'] }, endpoints: [{
              area: 'notes', auth: null, deprecated: false, errors: [],
              examples: { request: false, response: false }, method: 'GET', operationId: 'listNotes',
              path: '/notes', request: { body: null, pathParams: [], query: [] },
              response: { '200': { array: false, schemaRef: null } }, summary: null,
            }], stats: { endpoints: 1, areas: 1, schemas: 0 },
          }) + '\\n');
          fs.writeFileSync(path.join(stage, 'areas', 'notes.json'),
            JSON.stringify({ schemaVersion: 1, area: 'notes', schemas: {} }) + '\\n');
          fs.writeFileSync(path.join(stage, 'openapi.json'),
            JSON.stringify({ openapi: '3.1.0', info: { title, version: '1' }, paths: { '/notes': {} } }) + '\\n');
          return generation.publish({ generationId: generation.createGenerationId(), environmentId: 'dev',
            sourceKind: 'openapi', sourceFingerprint: 'sha256:' + '1'.repeat(64),
            expectedSnapshotHash: expected, inventoryFile: path.join(stage, 'inventory.json'),
            areasDir: path.join(stage, 'areas'), specFile: path.join(stage, 'openapi.json') });
        }
        const pinned = publish(null, 'Pinned');
        assert.equal(pinned.ok, true);
        git(ROOT, 'add', 'orchestrator/api-contract');
        git(ROOT, 'commit', '-q', '-m', 'pin api generation');
        fs.rmSync(stage, { recursive: true, force: true });
        const p = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000000-r1', requestId: '1700000000000-q1' });
        const sessionId = 'ws-' + 'b'.repeat(32);
        const startedAt = new Date().toISOString();
        const lock = {
          version: 1, stem: 'TASK_7_probe', stage: 'orchestrator', runId: p.runId,
          sessionId, startedAt,
          owner: { kind: 'direct', id: 'fixture:' + sessionId, pid: process.pid,
            processStartId: null, hostname: require('node:os').hostname(), startedAt },
        };
        const locksDir = path.join(ROOT, 'orchestrator', '.cache', 'tasks', 'locks');
        fs.mkdirSync(locksDir, { recursive: true });
        fs.writeFileSync(path.join(locksDir, 'TASK_7_probe.json'), JSON.stringify(lock, null, 2) + '\\n');
        Object.assign(process.env, {
          ORCHESTRATOR_PROJECT_ROOT: ROOT,
          ORCHESTRATOR_EXECUTION_ROOT: p.executionRoot,
          ORCHESTRATOR_EXECUTION_MANIFEST: p.manifestFile,
          ORCHESTRATOR_TASK_SNAPSHOT_FILE: p.taskSnapshotFile,
          ORCHESTRATOR_TASK_SNAPSHOT_HASH: p.taskSnapshotHash,
          ORCHESTRATOR_WORKTREE_ID: p.worktreeId,
          ORCHESTRATOR_RUN_ID: p.runId,
          ORCHESTRATOR_WRITER_STEM: 'TASK_7_probe',
          ORCHESTRATOR_WRITER_SESSION_ID: sessionId,
        });
        const advanced = publish(pinned.currentHash, 'Advanced');
        assert.equal(advanced.ok, true);
        fs.writeFileSync(path.join(ROOT, 'orchestrator', 'project-config.md'), 'config: advanced\\n');
        const util = await import(require('node:url').pathToFileURL(
          ${JSON.stringify(join(repoRoot, 'orchestrator/api-contract/scripts/_util.mjs'))}
        ).href + '?fixture=' + Date.now());
        const current = util.currentContractFiles();
        fs.writeFileSync(path.join(p.executionRoot, 'orchestrator', 'project-config.md'),
          'config: changed after helper proof\\n');
        const configAfterExecutionRace = util.readConfig('config');
        fs.writeFileSync(path.join(p.executionRoot, 'orchestrator', 'project-config.md'), 'config: v\\n');
        fs.writeFileSync(path.join(p.executionRoot, 'CandidateOnly.kt'),
          'class CandidateOnly { val marker = "execution" }\\n');
        const diffScript = ${JSON.stringify(join(repoRoot, 'orchestrator/api-contract/scripts/diff.mjs'))};
        execFileSync(process.execPath, [diffScript], {
          cwd: p.executionRoot, env: { ...process.env }, encoding: 'utf8',
          stdio: ['ignore', 'pipe', 'pipe'],
        });
        const analyzeScript = ${JSON.stringify(join(repoRoot, 'orchestrator/api-contract/scripts/analyze-project.mjs'))};
        execFileSync(process.execPath, [analyzeScript], {
          cwd: p.executionRoot, env: { ...process.env }, encoding: 'utf8',
          stdio: ['ignore', 'pipe', 'pipe'],
        });
        const reportFile = path.join(ROOT, 'orchestrator', '.cache', 'api-contract', 'reports',
          'executions', p.worktreeId, p.runId, 'drift.json');
        const report = JSON.parse(fs.readFileSync(reportFile, 'utf8'));
        const implementationReportFile = path.join(path.dirname(reportFile), 'implementation-map.json');
        const implementationReport = JSON.parse(fs.readFileSync(implementationReportFile, 'utf8'));
        const relations = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/api-relations.js'))});
        const taskSnapshot = relations.snapshot({ stem: 'TASK_7_probe', freshInputs: true });
        const previousCacheRoot = process.env.ORCHESTRATOR_CACHE_DIR;
        process.env.ORCHESTRATOR_CACHE_DIR = PARENT;
        let cacheOverrideError = null;
        try {
          await import(require('node:url').pathToFileURL(
            ${JSON.stringify(join(repoRoot, 'orchestrator/api-contract/scripts/_util.mjs'))}
          ).href + '?unsafe-cache=' + Date.now());
        } catch (error) { cacheOverrideError = error.message; }
        if (previousCacheRoot === undefined) delete process.env.ORCHESTRATOR_CACHE_DIR;
        else process.env.ORCHESTRATOR_CACHE_DIR = previousCacheRoot;

        const reportNamespace = path.dirname(reportFile);
        const savedNamespace = reportNamespace + '.saved';
        const escapedNamespace = path.join(PARENT, 'api-report-escape');
        fs.renameSync(reportNamespace, savedNamespace);
        fs.mkdirSync(escapedNamespace);
        fs.symlinkSync(escapedNamespace, reportNamespace, 'dir');
        const suggest = spawnSync(process.execPath,
          [${JSON.stringify(join(repoRoot, 'orchestrator/api-contract/scripts/suggest-endpoint-tasks.mjs'))}], {
            cwd: p.executionRoot, env: { ...process.env }, encoding: 'utf8',
            stdio: ['ignore', 'pipe', 'pipe'],
          });
        const escapedSuggestionPresent = fs.existsSync(path.join(escapedNamespace, 'suggested-endpoints.json'));
        fs.unlinkSync(reportNamespace);
        fs.renameSync(savedNamespace, reportNamespace);
        const projectInputs = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/api-project-inputs.js'))});
        const executionInputs = projectInputs.collect(p.executionRoot, { includeText: false });
        const controlInputs = projectInputs.collect(ROOT, { includeText: false });
        console.log(JSON.stringify({
          mode: current.mode,
          generationId: current.committedGenerationId,
          pinnedGenerationId: pinned.generationId,
          advancedGenerationId: advanced.generationId,
          inventoryInExecution: current.inventory.startsWith(p.executionRoot + path.sep),
          configValue: util.readConfig('config'),
          configAfterExecutionRace,
          reportRevision: report.projectCodeRevision,
          implementationRevision: implementationReport.projectCodeRevision,
          executionRevision: executionInputs.projectCodeRevision,
          controlRevision: controlInputs.projectCodeRevision,
          reportFile,
          globalReportPresent: fs.existsSync(path.join(
            ROOT, 'orchestrator', '.cache', 'api-contract', 'reports', 'drift.json')),
          globalImplementationReportPresent: fs.existsSync(path.join(
            ROOT, 'orchestrator', '.cache', 'api-contract', 'reports', 'implementation-map.json')),
          relationOk: taskSnapshot.ok,
          relationError: taskSnapshot.error || null,
          relationCurrentError: taskSnapshot.current && taskSnapshot.current.error || null,
          relationLimitations: taskSnapshot.limitations,
          relationGenerationId: taskSnapshot.committedGenerationId,
          relationDriftCurrent: taskSnapshot.drift && taskSnapshot.drift.current,
          relationImplementationRevision: taskSnapshot.implementation &&
            taskSnapshot.implementation.projectCodeRevision,
          cacheOverrideError,
          suggestSymlinkStatus: suggest.status,
          escapedSuggestionPresent,
        }));
      })().catch((error) => { console.error(error); process.exitCode = 1; });
    `)
    assert.equal(out.mode, 'generation')
    assert.equal(out.generationId, out.pinnedGenerationId)
    assert.notEqual(out.generationId, out.advancedGenerationId)
    assert.equal(out.inventoryInExecution, true)
    assert.equal(out.configValue, 'v', 'task helpers must read the checkout copy pinned at provisioning')
    assert.equal(out.configAfterExecutionRace, 'v',
      'API helpers must keep the manifest-pinned project-config snapshot after helper proof')
    assert.equal(out.reportRevision, out.executionRevision)
    assert.equal(out.implementationRevision, out.executionRevision)
    assert.notEqual(out.reportRevision, out.controlRevision)
    assert.match(out.reportFile, /\/reports\/executions\/wt-[^/]+\/1700000000000-r1\/drift\.json$/)
    assert.equal(out.globalReportPresent, false,
      'a task run must not overwrite the singleton control-plane drift report')
    assert.equal(out.globalImplementationReportPresent, false,
      'a task analyzer must not overwrite the singleton control-plane implementation report')
    assert.equal(out.relationOk, true, JSON.stringify(out))
    assert.equal(out.relationGenerationId, out.pinnedGenerationId,
      'task relations must read the checkout generation, not the refreshed control generation')
    assert.equal(out.relationDriftCurrent, true,
      'task relations must read the exact report namespace produced by that run')
    assert.equal(out.relationImplementationRevision, out.executionRevision,
      `task relations must consume the implementation analysis produced from that checkout: ${JSON.stringify(out)}`)
    assert.match(out.cacheOverrideError, /must equal the manager-owned control cache/,
      'a task API helper must not redirect reports into a caller-selected cache root')
    assert.notEqual(out.suggestSymlinkStatus, 0,
      'a task API report writer must reject a symlinked run namespace')
    assert.equal(out.escapedSuggestionPresent, false,
      'a task API report writer must not publish through a symlinked run namespace')
  })

  check('Figma task helpers reject generation bytes changed inside the execution checkout', () => {
    const { out } = runInFixture(`
      (async () => {
        const path = require('node:path');
        const pointer = path.join(ROOT, 'orchestrator', 'figma', 'manifests', 'current-generation.json');
        fs.mkdirSync(path.dirname(pointer), { recursive: true });
        fs.writeFileSync(pointer, JSON.stringify({ generationId: 'figma-pinned' }) + '\\n');
        git(ROOT, 'add', 'orchestrator/figma/manifests/current-generation.json');
        git(ROOT, 'commit', '-q', '-m', 'pin figma generation');
        const p = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000000-r1', requestId: '1700000000000-q1' });
        const sessionId = 'ws-' + 'c'.repeat(32);
        const startedAt = new Date().toISOString();
        const lock = {
          version: 1, stem: 'TASK_7_probe', stage: 'orchestrator', runId: p.runId,
          sessionId, startedAt,
          owner: { kind: 'direct', id: 'fixture:' + sessionId, pid: process.pid,
            processStartId: null, hostname: require('node:os').hostname(), startedAt },
        };
        const locksDir = path.join(ROOT, 'orchestrator', '.cache', 'tasks', 'locks');
        fs.mkdirSync(locksDir, { recursive: true });
        fs.writeFileSync(path.join(locksDir, 'TASK_7_probe.json'), JSON.stringify(lock, null, 2) + '\\n');
        Object.assign(process.env, {
          ORCHESTRATOR_PROJECT_ROOT: ROOT,
          ORCHESTRATOR_EXECUTION_ROOT: p.executionRoot,
          ORCHESTRATOR_EXECUTION_MANIFEST: p.manifestFile,
          ORCHESTRATOR_TASK_SNAPSHOT_FILE: p.taskSnapshotFile,
          ORCHESTRATOR_TASK_SNAPSHOT_HASH: p.taskSnapshotHash,
          ORCHESTRATOR_WORKTREE_ID: p.worktreeId,
          ORCHESTRATOR_RUN_ID: p.runId,
          ORCHESTRATOR_WRITER_STEM: 'TASK_7_probe',
          ORCHESTRATOR_WRITER_SESSION_ID: sessionId,
        });
        fs.writeFileSync(path.join(p.executionRoot, 'orchestrator', 'figma', 'manifests', 'current-generation.json'),
          JSON.stringify({ generationId: 'figma-tampered' }) + '\\n');
        let error = null;
        try {
          await import(require('node:url').pathToFileURL(
            ${JSON.stringify(join(repoRoot, 'orchestrator/figma/scripts/_util.mjs'))}
          ).href + '?fixture=' + Date.now());
        } catch (caught) { error = caught && caught.message; }
        console.log(JSON.stringify({ error }));
      })().catch((error) => { console.error(error); process.exitCode = 1; });
    `)
    assert.match(out.error || '', /FIGMA_EXECUTION_GENERATION_MISMATCH/)
  })

  check('every immutable provisioning artifact uses the crash-safe no-clobber publisher', () => {
    const { out } = runInFixture(`
      const guards = require(${JSON.stringify(join(repoRoot, 'orchestrator/site/server/file-guards.js'))});
      const originalPublish = guards.publishNoClobberRegularFileUnder;
      const published = [];
      guards.publishNoClobberRegularFileUnder = function (root, dir, target) {
        published.push(require('node:path').basename(target));
        return originalPublish.apply(this, arguments);
      };
      const p = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000000-r1', requestId: '1700000000000-q1' });
      console.log(JSON.stringify({ ok: p.ok, published,
        record: published.includes(p.worktreeId + '.json'),
        manifest: published.filter((name) => name === p.worktreeId + '.json').length === 2,
        snapshot: published.includes(p.taskSnapshotHash.slice('sha256:'.length) + '.md') }));
    `)
    assert.equal(out.ok, true)
    assert.equal(out.record, true)
    assert.equal(out.manifest, true)
    assert.equal(out.snapshot, true)
  })

  check('provision refuses a queue source revision that does not match the exact control task generation', () => {
    const { out } = runInFixture(`
      const blocked = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000000-r1',
        requestId: '1700000000000-q1', sourceRevision: 'sha256:' + 'f'.repeat(64) });
      const dir = require('node:path').join(ROOT, 'orchestrator', '.cache', 'tasks', 'worktrees');
      console.log(JSON.stringify({ code: blocked.code,
        records: fs.existsSync(dir) ? fs.readdirSync(dir).filter((name) => name.endsWith('.json')).length : 0 }));
    `)
    assert.equal(out.code, 'PROVISION_SOURCE_REVISION_MISMATCH')
    assert.equal(out.records, 0)
  })

  check('ready resume rejects a tampered execution manifest even when two convenient fields still match', () => {
    const { out } = runInFixture(`
      const first = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000000-r1', requestId: '1700000000000-q1' });
      const manifest = JSON.parse(fs.readFileSync(first.manifestFile, 'utf8'));
      manifest.requestId = '1700000000999-foreign';
      manifest.extra = true;
      fs.writeFileSync(first.manifestFile, JSON.stringify(manifest) + '\\n');
      const resumed = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000001-r2', requestId: '1700000000001-q2' });
      const recordFile = require('node:path').join(ROOT, 'orchestrator', '.cache', 'tasks', 'worktrees', first.worktreeId + '.json');
      console.log(JSON.stringify({ resumed: resumed.ok, code: resumed.code,
        status: JSON.parse(fs.readFileSync(recordFile, 'utf8')).status }));
    `)
    assert.equal(out.resumed, false)
    assert.equal(out.code, 'PROVISION_RESUME_FAILED')
    assert.equal(out.status, 'recovery-required')
  })

  check('a second live generation is refused; a ready generation resumes; release frees it', () => {
    const { out } = runInFixture(`
      const a = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000000-r1', requestId: '1700000000000-q1' });
      const busy = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000001-r2', requestId: '1700000000001-q2' });
      const release = manager.release(a.worktreeId);
      const afterFindings = manager.discover().findings.map((f) => f.code);
      const reprovision = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000002-r3', requestId: '1700000000002-q3' });
      console.log(JSON.stringify({
        ready: busy.ok === true && busy.resumed === true && busy.worktreeId === a.worktreeId,
        release: release.ok, afterFindings, reprovision: reprovision.ok,
        checkoutGone: !fs.existsSync(a.executionRoot),
        refGone: (() => { try { git(ROOT, 'rev-parse', '--verify', a.candidateRef); return false; } catch { return true; } })(),
      }));
    `)
    assert.equal(out.ready, true, 'a ready generation resumes rather than erroring')
    assert.equal(out.release, true)
    assert.deepEqual(out.afterFindings, [])
    assert.equal(out.reprovision, true, 'a released generation frees the task for a new run')
    assert.equal(out.checkoutGone, true)
    assert.equal(out.refGone, true)
  })

  check('a non-ready active generation blocks a new provision', () => {
    const { out } = runInFixture(`
      const a = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000000-r1', requestId: '1700000000000-q1' });
      // Force the record into a mid-flight status the manager cannot resume.
      const recFile = ${JSON.stringify('placeholder')};
      const dir = require('node:path').join(ROOT, 'orchestrator', '.cache', 'tasks', 'worktrees');
      const file = require('node:path').join(dir, a.worktreeId + '.json');
      const rec = JSON.parse(fs.readFileSync(file, 'utf8'));
      const sealing = Object.assign({}, rec, { status: 'sealing', updatedAt: new Date().toISOString() });
      sealing.recordHash = contract.recordHash(sealing);
      fs.writeFileSync(file, JSON.stringify(sealing) + '\\n');
      const blocked = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000001-r2', requestId: '1700000000001-q2' });
      console.log(JSON.stringify({ code: blocked.code }));
    `)
    assert.equal(out.code, 'PROVISION_GENERATION_ACTIVE')
  })

  check('crash after intent with no footprint resumes creation; a partial footprint is recovery-required', () => {
    const { out } = runInFixture(`
      const past = '2026-08-01T00:00:00.000Z';
      const recDir = require('node:path').join(ROOT, 'orchestrator', '.cache', 'tasks', 'worktrees');
      fs.mkdirSync(recDir, { recursive: true });
      const crypto = require('node:crypto');
      function commonId() {
        const real = fs.realpathSync.native(require('node:path').join(ROOT, '.git'));
        const st = fs.lstatSync(real, { bigint: true });
        return { path: real.normalize('NFC'), dev: String(st.dev), ino: String(st.ino) };
      }
      function controlRoot() {
        const real = fs.realpathSync.native(ROOT);
        const st = fs.lstatSync(real, { bigint: true });
        return { path: real.normalize('NFC'), dev: String(st.dev), ino: String(st.ino) };
      }
      function intentFor(stem, wtHex, refHex, taskFile) {
        fs.writeFileSync(require('node:path').join(ROOT, 'orchestrator', 'tasks', 'todo', taskFile), '# ' + stem + '\\n');
        const bytes = fs.readFileSync(require('node:path').join(ROOT, 'orchestrator', 'tasks', 'todo', taskFile));
        const h = 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex');
        const number = /TASK_([0-9]+)_/.exec(stem)[1];
        const cid = commonId();
        const rec = {
          version: 1, worktreeId: 'wt-' + wtHex, runId: '1700000000000-x1', requestId: '1700000000000-y1',
          stem, status: 'create-intent', controlProjectId: contract.digest(cid), gitCommonDirIdentity: cid,
          controlRoot: controlRoot(), executionRoot: null, targetRef: 'refs/heads/main',
          candidateRef: 'refs/heads/orchestrator/task/TASK_' + number + '-' + refHex + '/x1',
          baseCommit: git(ROOT, 'rev-parse', 'HEAD').trim(), baseTree: git(ROOT, 'rev-parse', 'HEAD^{tree}').trim(),
          taskState: 'todo', taskSourceRevision: taskIntegrity.validateAction('run', stem, 'fixture').sourceRevision, taskSnapshotHash: h,
          projectConfigHash: 'sha256:' + crypto.createHash('sha256').update(Buffer.alloc(0)).digest('hex'),
          dependencySnapshotHash: 'sha256:c213f0364a9d65c84f208816f8b1f4a841747584ee2af680272093612b4297ad',
          figmaGenerationHash: null, apiGenerationHash: null,
          capabilities: [], owner: { hostname: 'x', pid: 1, processStartId: null, startedAt: past },
          createdAt: past, updatedAt: past, recordHash: 'sha256:' + '0'.repeat(64),
        };
        rec.recordHash = contract.recordHash(rec);
        contract.validate(rec);
        fs.writeFileSync(require('node:path').join(recDir, rec.worktreeId + '.json'), JSON.stringify(rec) + '\\n');
        return rec;
      }
      const clean = intentFor('TASK_8_second', 'cd'.repeat(16), 'cd'.repeat(6), 'TASK_8_second.md');
      const resumed = manager.provision({ stem: 'TASK_8_second', runId: '1700000000000-x1', requestId: '1700000000000-y1' });
      const partial = intentFor('TASK_9_third', 'ef'.repeat(16), 'ef'.repeat(6), 'TASK_9_third.md');
      git(ROOT, 'branch', partial.candidateRef.slice('refs/heads/'.length), 'HEAD');
      const recovery = manager.provision({ stem: 'TASK_9_third', runId: '1700000000000-x1', requestId: '1700000000000-y1' });
      const recFile = require('node:path').join(recDir, partial.worktreeId + '.json');
      const recStatus = JSON.parse(fs.readFileSync(recFile, 'utf8')).status;

      const checkoutPartial = intentFor('TASK_10_fourth', 'ab'.repeat(16), 'ab'.repeat(6), 'TASK_10_fourth.md');
      const repoKey = checkoutPartial.controlProjectId.slice('sha256:'.length, 'sha256:'.length + 16);
      const partialPath = require('node:path').join(PARENT, '.orchestrator-worktrees', repoKey, checkoutPartial.worktreeId);
      fs.mkdirSync(require('node:path').dirname(partialPath), { recursive: true });
      const added = gitMutations.addWorktree({ candidateRef: checkoutPartial.candidateRef,
        targetPath: partialPath, baseCommit: checkoutPartial.baseCommit });
      const checkoutRecovery = manager.provision({ stem: 'TASK_10_fourth',
        runId: '1700000000000-x1', requestId: '1700000000000-y1' });
      const checkoutRecordFile = require('node:path').join(recDir, checkoutPartial.worktreeId + '.json');
      const checkoutRecord = JSON.parse(fs.readFileSync(checkoutRecordFile, 'utf8'));
      const checkoutReleased = manager.releaseFor('TASK_10_fourth');

      const absent = intentFor('TASK_11_fifth', '12'.repeat(16), '12'.repeat(6), 'TASK_11_fifth.md');
      absent.status = 'recovery-required';
      absent.updatedAt = new Date().toISOString();
      absent.recordHash = contract.recordHash(absent);
      fs.writeFileSync(require('node:path').join(recDir, absent.worktreeId + '.json'), JSON.stringify(absent) + '\\n');
      const absentReleased = manager.releaseFor('TASK_11_fifth');
      let absentMarker = null;
      try { absentMarker = git(ROOT, 'rev-parse', '-q', '--verify',
        'refs/orchestrator/releases/' + absent.worktreeId).trim(); } catch (error) {}
      console.log(JSON.stringify({
        resumedOk: resumed.ok, resumedSameId: resumed.worktreeId === clean.worktreeId,
        recoveryCode: recovery.code, recoveryStatus: recStatus,
        branchUntouched: (() => { try { return git(ROOT, 'rev-parse', '--verify', partial.candidateRef).trim().length === 40; } catch { return false; } })(),
        checkoutAdded: added.ok, checkoutRecoveryCode: checkoutRecovery.code,
        checkoutIdentityPinned: checkoutRecord.executionRoot !== null,
        checkoutReleased: checkoutReleased.ok, checkoutGone: !fs.existsSync(partialPath),
        absentReleased: absentReleased.ok, absentMarker,
      }));
    `)
    assert.equal(out.resumedOk, true, 'a footprint-free intent resumes creation of the same generation')
    assert.equal(out.resumedSameId, true)
    assert.equal(out.recoveryCode, 'PROVISION_RECOVERY_REQUIRED')
    assert.equal(out.recoveryStatus, 'recovery-required')
    assert.equal(out.branchUntouched, true, 'a partial footprint is never destroyed, only flagged')
    assert.equal(out.checkoutAdded, true)
    assert.equal(out.checkoutRecoveryCode, 'PROVISION_RECOVERY_REQUIRED')
    assert.equal(out.checkoutIdentityPinned, true,
      'classification must pin the exact partial checkout it later authorizes release to remove')
    assert.equal(out.checkoutReleased, true)
    assert.equal(out.checkoutGone, true)
    assert.equal(out.absentReleased, true,
      'a recovery generation with a proven absent ref still needs an ownership-safe exit')
    assert.match(out.absentMarker || '', /^[a-f0-9]{40}$/)
  })

  check('the mutation owner refuses foreign refs, collisions, out-of-home removal and force flags', () => {
    const { out } = runInFixture(`
      const p = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000000-r1', requestId: '1700000000000-q1' });
      const base = git(ROOT, 'rev-parse', 'HEAD').trim();
      const foreignRef = gitMutations.addWorktree({ candidateRef: 'refs/heads/main', targetPath: PARENT + '/x', baseCommit: base });
      const collision = gitMutations.addWorktree({ candidateRef: p.candidateRef, targetPath: PARENT + '/y', baseCommit: base });
      const outside = gitMutations.removeOwnedWorktree({ targetPath: ROOT });
      const refMoved = gitMutations.releaseOwnedRef({ worktreeId: p.worktreeId,
        candidateRef: p.candidateRef, expectedCommit: 'e'.repeat(40) });
      const receipts = fs.readdirSync(gitMutations.RECEIPTS_DIR).length;
      console.log(JSON.stringify({
        foreignRef: foreignRef.code, collision: collision.code, outside: outside.code, refMoved: refMoved.code,
        receiptsPositive: receipts > 0,
      }));
    `)
    assert.equal(out.foreignRef, 'MUTATION_REF_NOT_ALLOWED')
    assert.equal(out.collision, 'MUTATION_REF_COLLISION')
    assert.equal(out.outside, 'MUTATION_PATH_NOT_OWNED')
    assert.equal(out.refMoved, 'MUTATION_REF_MOVED')
    assert.equal(out.receiptsPositive, true)
  })

  check('the Git mutation owner rejects a redirected receipt directory before add-worktree', () => {
    if (process.platform === 'win32') return
    const { out } = runInFixture(`
      const path = require('node:path');
      const receiptParent = path.dirname(gitMutations.RECEIPTS_DIR);
      const outside = path.join(PARENT, 'foreign-mutation-receipts');
      const target = path.join(PARENT, 'redirected-receipt-checkout');
      fs.mkdirSync(receiptParent, { recursive: true });
      fs.mkdirSync(outside);
      fs.symlinkSync(outside, gitMutations.RECEIPTS_DIR, 'dir');
      const base = git(ROOT, 'rev-parse', 'HEAD').trim();
      const result = gitMutations.addWorktree({
        candidateRef: 'refs/heads/orchestrator/task/TASK_7-aaaaaaaaaaaa/r1',
        targetPath: target,
        baseCommit: base,
      });
      console.log(JSON.stringify({
        ok: result.ok === true,
        code: result.code || null,
        externalNames: fs.readdirSync(outside),
        targetExists: fs.existsSync(target),
      }));
    `)
    assert.equal(out.ok, false)
    assert.equal(out.code, 'MUTATION_RECEIPT_UNWRITABLE')
    assert.deepEqual(out.externalNames, [],
      'a redirected receipt namespace must never receive intent or outcome bytes')
    assert.equal(out.targetExists, false,
      'Git must not start after the durable intent authority was refused')
  })

  check('environment blockers refuse provisioning before any mutation', () => {
    const { out } = runInFixture(`
      fs.writeFileSync(require('node:path').join(ROOT, '.gitmodules'), '[submodule "x"]\\n');
      const blocked = manager.provision({ stem: 'TASK_7_probe', runId: '1700000000000-r1', requestId: '1700000000000-q1' });
      const worktreesDir = require('node:path').join(ROOT, 'orchestrator', '.cache', 'tasks', 'worktrees');
      const records = fs.existsSync(worktreesDir) ? fs.readdirSync(worktreesDir).length : 0;
      console.log(JSON.stringify({ code: blocked.code, records }));
    `)
    assert.equal(out.code, 'PROVISION_ENVIRONMENT_BLOCKED')
    assert.equal(out.records, 0, 'a blocked environment publishes no create-intent record')
  })

  console.log(`worktree-provisioning: ${checks} checks passed`)
} finally {
  while (roots.length) {
    const dir = roots.pop()
    try { chmodSync(dir, 0o700) } catch {}
    rmSync(dir, { recursive: true, force: true })
  }
}
