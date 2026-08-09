#!/usr/bin/env node
// ship-done.mjs <stem> — the sanctioned todo→done move plus the mechanical
// visual-evidence interlock used by task-orchestrator Step 6b.
//
// "Compare before done" is a hard gate: the durable `- Figma meta:` digest is
// code-emitted and the actor being audited cannot author or omit it.
//
//   * For a UI task (figmaEnabled: true AND a non-none `## Design`), it runs the FINAL
//     evidence bundle ITSELF, refuses to move unless the bundle is shippable (PASS, or a
//     reviewed WARN), and writes the CODE-EMITTED digest into the appendix's
//     `### Execution log`. The agent can no longer author or omit the digest.
//   * Before the move it also persists COMMITTED ship receipts (byte-verbatim copies of the
//     three certification files the digest binds) under
//     orchestrator/tasks/evidence/figma-ship/<stem>/, so verify-done.mjs can re-bind the
//     shipped digest in a fresh CI clone where the gitignored reports cache does not exist.
//   * Non-UI tasks move straight through (no Figma gate applies).
//
// Exit: 0 = moved; 2 = blocked by the gate (do NOT move; route to the fix path); 1 = usage
// or precondition error.

import { closeSync, copyFileSync, existsSync, fsyncSync, linkSync, lstatSync, mkdirSync, openSync, readFileSync, readdirSync, realpathSync, renameSync, statSync, unlinkSync, writeFileSync } from 'node:fs'
import { basename, dirname, join, parse, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { execFileSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import { hostname } from 'node:os'
import { createRequire } from 'node:module'
import { PROJECT_ROOT, figmaPath, figmaScreensRoot, pipelineRunId, readConfig, outcomeAppendixStatus } from './_util.mjs'
import { componentProvenanceEntries } from './lib/design-components.mjs'
import { parseFigmaMeta, isShippableOverall, caveatsHaveContent } from './figma-meta.mjs'
import { assertTaskStem, fileHash } from './report-utils.mjs'
import { injectOutcomeFigmaMeta, inspectOutcomeFigmaMeta, logicalTaskText, outcomeShapeError } from './outcome-shape.mjs'
import { buildTaskObservationReceipt, validateCommittedTaskObservationReceipt } from '../tokens/task-observation-receipt.mjs'
import { buildTaskIngestionIntent, validateTaskIngestionIntent } from '../tokens/task-ingestion-intent.mjs'

const require = createRequire(import.meta.url)
const { hasPullableDesign, hasMalformedDesign, uiTaskWithoutDesign, suggestedDesignBullet } = require('./design-parser.cjs')

// fileURLToPath, NOT URL.pathname: the pathname form keeps percent-encoding, so a
// checkout under a path with a space/non-ASCII char would ENOENT every HERE-relative
// read (the same fix _util.mjs line 7 already carries).
const HERE = dirname(fileURLToPath(import.meta.url))
const CANONICAL_FINAL_REPORTS = ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot']
const fail = (msg) => { console.error(`ship-done: ${msg}`); process.exit(1) }
// Design-EDIT blocks (fix the task's `## Design` section) tag `task-shape`; cache/evidence/pipeline
// blocks keep `figma-screens` (a pull / re-run is the remedy). The board renders a "Pull Figma
// screens" button + hint only for `figma-screens`, so a design-edit block must NOT use that tag or
// it drives the user toward a pull that cannot clear it.
// Colon after the bracket is load-bearing: the frozen tag grammar (run-loop.md `BLOCKED[<type>]:`)
// and the board parser (board.js BLOCKED_TAG_RE) both require it — without it a verbatim surface
// of this stderr line loses the type routing (no contextual recovery button).
const block = (msg, type = 'figma-screens') => { console.error(`BLOCKED[${type}]: ${msg}`); process.exit(2) }
// Atomic copy into the COMMITTED receipt tree: write to a sibling `.tmp` then rename over the
// final name. The receipt tree is committed by an EXTERNAL commit-only watcher (orchestrator/
// DEV-NOTES.md) that `git add`s tracked changes on its own schedule — a bare copyFileSync straight
// to the final name lets that watcher stage a HALF-written receipt, which verify-done then reads as
// a corrupt/hash-mismatched cert (a false RED on a legitimately-shipped task). rename is atomic on
// one filesystem (tmp + dst share the receipt dir); a stray `.tmp` from a crash is inert (every
// receipt reader uses exact names / an `.endsWith('.spec.json')` filter, never a bare `.tmp`).
let tempCounter = 0
const uniqueTemp = (dst) => `${dst}.tmp.${process.pid}.${Date.now()}.${++tempCounter}`
const syncDir = (dir) => {
  let fd
  try { fd = openSync(dir, 'r'); fsyncSync(fd) } catch { /* unsupported filesystems: rename is still atomic */ }
  finally { if (fd !== undefined) try { closeSync(fd) } catch {} }
}
const syncFile = (path) => {
  let fd
  try { fd = openSync(path, 'r'); fsyncSync(fd) } finally { if (fd !== undefined) closeSync(fd) }
}
const ensureRealDirectory = (path, label) => {
  const reject = (message) => { throw new Error(message) }
  const absolute = resolve(path)
  const root = parse(absolute).root
  let current = root
  let rootStat
  try { rootStat = lstatSync(root) } catch (e) { reject(`cannot inspect ${label} root ${root}: ${e.message}`) }
  if (rootStat.isSymbolicLink() || !rootStat.isDirectory()) reject(`${label} root must be a real directory`)
  const segments = absolute.slice(root.length).split(/[\\/]+/).filter(Boolean)
  for (let index = 0; index < segments.length; index++) {
    const segment = segments[index]
    current = join(current, segment)
    let st
    try { st = lstatSync(current) }
    catch (e) {
      if (!e || e.code !== 'ENOENT') reject(`cannot inspect ${label} component ${current}: ${e.message}`)
      try { mkdirSync(current, { mode: 0o700 }) }
      catch (mkdirError) { if (!mkdirError || mkdirError.code !== 'EEXIST') reject(`cannot create ${label} component ${current}: ${mkdirError.message}`) }
      try { st = lstatSync(current) } catch (inspectError) { reject(`cannot inspect created ${label} component ${current}: ${inspectError.message}`) }
    }
    // macOS exposes system-owned top-level aliases such as /var -> /private/var.
    // Canonicalize only that filesystem-root entry; every lower component is
    // still rejected if it is a symlink, including any caller-controlled
    // receipt ancestor inside /var/folders/.../T.
    if (index === 0 && st.isSymbolicLink() && st.uid === 0) {
      let canonical, canonicalStat
      try { canonical = realpathSync(current); canonicalStat = lstatSync(canonical) }
      catch (e) { reject(`cannot resolve ${label} system root alias ${current}: ${e.message}`) }
      if (!canonicalStat.isDirectory() || canonicalStat.isSymbolicLink()) reject(`${label} system root alias must resolve to a real directory`)
      current = canonical
      continue
    }
    if (st.isSymbolicLink() || !st.isDirectory()) reject(`${label} must be a real directory tree; unsafe component ${current} is a symlink or special file`)
  }
}
const atomicCopy = (src, dst) => {
  ensureRealDirectory(dirname(dst), 'atomic-copy destination')
  const tmp = uniqueTemp(dst)
  try {
    copyFileSync(src, tmp)
    syncFile(tmp)
    renameSync(tmp, dst)
    syncDir(dirname(dst))
  } catch (e) {
    try { unlinkSync(tmp) } catch {}
    throw e
  }
}
const atomicWriteExact = (bytes, dst) => {
  ensureRealDirectory(dirname(dst), 'immutable publication destination')
  const expected = Buffer.from(bytes)
  const existing = inspectRegular(dst, 'immutable publication file')
  if (existing) {
    if (sha256(readFileSync(dst)) !== sha256(expected)) throw new Error(`immutable publication conflict at ${dst}`)
    return
  }
  const tmp = uniqueTemp(dst)
  let fd
  try {
    fd = openSync(tmp, 'wx', 0o600)
    writeFileSync(fd, expected)
    fsyncSync(fd)
    closeSync(fd); fd = undefined
    linkSync(tmp, dst)
    syncDir(dirname(dst))
  } catch (error) {
    if (!error || error.code !== 'EEXIST' || sha256(readFileSync(dst)) !== sha256(expected)) throw error
  } finally {
    if (fd !== undefined) try { closeSync(fd) } catch {}
    try { unlinkSync(tmp); syncDir(dirname(tmp)) } catch {}
  }
}
const screensRootOrFail = () => {
  try { return figmaScreensRoot() } catch (e) { fail(e.message) }
}

let stem
try { stem = assertTaskStem(process.argv[2]) } catch (e) { fail(e.message || 'usage: node scripts/ship-done.mjs <stem>') }
if (process.argv.length !== 3) fail('usage: node scripts/ship-done.mjs <stem> (invoked by finalize-task.mjs only)')

const tasksDir = join(PROJECT_ROOT, 'orchestrator', 'tasks')
const todoFile = join(tasksDir, 'todo', `${stem}.md`)
const doneFile = join(tasksDir, 'done', `${stem}.md`)
const finalization = authorizeFinalization(stem)
const publicationFile = publicationProofPath(doneFile, finalization)
cleanupPublicationTemps(publicationFile)
if (recoverPublication(todoFile, doneFile, publicationFile, finalization)) {
  console.log(`ship-done: recovered interrupted no-clobber move/publication for ${stem}.md → done/`)
  process.exit(0)
}
safeTaskFile(todoFile, true)
safeTaskFile(doneFile, false)

let md = readFileSync(todoFile, 'utf8')
if (sha256(Buffer.from(md)) !== finalization.source.intendedHash) {
  fail('todo task bytes do not match the active finalization intent at ship start')
}
if (md.indexOf('## Outcome') === -1) fail(`todo/${stem}.md has no ## Outcome appendix — write it before shipping`)
// The appendix must also PARSE — the shared outcome contract and the board projection
// flip the done badge to `malformed`
// on any drift from the frozen shape, and done/ is immutable, so a malformed appendix must be
// caught HERE, before the move (exit 1: a fixable precondition — repair the appendix per the
// task-orchestrator outcome-appendix reference and re-run). Gates mirror the parsers exactly
// (presence + non-empty sections + valid key values; heading order is NOT gated there either).
let shapeErr
try { shapeErr = outcomeShapeError(md) } catch (e) { fail(e.message) }
if (shapeErr) fail(`todo/${stem}.md ## Outcome appendix is malformed: ${shapeErr} — the board would render this done task as malformed; repair the appendix (task-orchestrator outcome-appendix reference) and re-run ship-done`)

const figmaEnabled = readConfig('figmaEnabled') === 'true'
const liveConfigHash = sha256(readFileSync(join(PROJECT_ROOT, 'orchestrator', 'project-config.md')))
if (figmaEnabled !== finalization.figma.enabled || liveConfigHash !== finalization.figma.configHash) {
  fail(`project-config.md changed after finalization intent (figmaEnabled ${finalization.figma.enabled} -> ${figmaEnabled}); gate applicability/config may not change mid-transaction`)
}
// Fail-open guard: a MALFORMED `## Design` (typo'd separator, invalid screen name, bad/invalid
// Figma URL, or a non-`none` value with no pullable node) would otherwise make hasPullableDesign
// false → the task looks non-UI → it slips past the screenshot gate entirely. Block it instead —
// a broken design ref must be fixed, never bypassed. (Exit 2: routes to the fix path.)
if (figmaEnabled && hasMalformedDesign(md)) {
  block(`## Design is malformed (unparseable bullet / invalid screen / invalid or non-pullable Figma URL) — fix the design bullets; a broken design ref cannot bypass the mandatory screenshot gate as a non-UI task`, 'task-shape')
}
// UI-by-evidence backstop (W1 + B1): a task that PROVABLY targets a Figma node — a STRONG signal
// (a cited node URL, or a `figmaNodeId:`/matrix component snapshot) — yet declares no pullable
// `## Design` bullet is a real UI task self-classifying non-UI; BLOCK it (it cannot ship
// uncompared). The WEAK filename-only signal (a `*Screen.kt` edit with no node cited) is advisory
// (a non-visual copy/callback edit must not be hard-blocked): emit a NOTE and continue. An audited
// `- <Name> — none (<why no mock>)` bullet suppresses ONLY the weak filename tier — the strong
// tiers block regardless (a node that provably exists must be declared + compared); their remedy
// is adding the design bullet or removing the stale node citation, never a none opt-out.
if (figmaEnabled) {
  // Pass the committed mapping-registry provenance so the tier can hold a node-backed component
  // even after its in-text snapshot was stripped (the escape-hatch closer). No-op when absent.
  const uiVerdict = uiTaskWithoutDesign(md, { stem, inventory: componentProvenanceEntries() })
  if (uiVerdict && uiVerdict.level === 'block') {
    // B5: append a ready-to-paste `## Design` bullet so the fix is one paste, not hand-authoring.
    const paste = suggestedDesignBullet(md)
    block(`${uiVerdict.reason}\n\nPaste this into the task \`## Design\` section (fill the URL if it is a placeholder):\n${paste}`, 'task-shape')
  } else if (uiVerdict && uiVerdict.level === 'warn') console.error(`ship-done: NOTE ${uiVerdict.reason} (advisory — filename-only signal, not blocking)`)
}
const isUiTask = figmaEnabled && hasPullableDesign(md)

// De-UI guard: a task whose screens were PULLED but whose `## Design` no longer carries a
// pullable bullet (all switched to `— none`, bullets deleted, or the section removed) is the
// other fail-open — editing the design refs away after the pull would reclassify a real UI
// task as non-UI and ship it uncompared. The pulled cache is the witness. A legitimate
// de-scope must remove the task's screens cache dir by hand (nothing in the pipeline deletes
// it — re-pull only writes, and evidence-clean does NOT touch screens/), which is exactly the
// deliberate step this guard forces.
if (figmaEnabled && !isUiTask) {
  const screensRoot = screensRootOrFail()
  const screensDir = join(screensRoot, stem)
  const idxPath = join(screensDir, 'index.json')
  if (existsSync(idxPath)) {
    let pulledNodes = null
    try { pulledNodes = JSON.parse(readFileSync(idxPath, 'utf8')).nodes } catch {}
    if (pulledNodes && typeof pulledNodes === 'object' && Object.keys(pulledNodes).length) {
      block(`## Design carries no pullable bullet but ${Object.keys(pulledNodes).length} screen(s) were pulled for this task — design refs removed after pull cannot bypass the comparison gate. Either re-add the design bullets, or (if the de-scope is intentional) run the sanctioned tool: node orchestrator/figma/scripts/descope-task.mjs ${stem} --reason "<why>" --yes — it rewrites the bullets to the audited none form, clears the screens cache CONFINED, and writes a committed de-scope receipt (never hand-rm the cache).`, 'task-shape')
    }
  }
}

if (isUiTask) {
  // 1. Run the FINAL bundle ourselves — this is the authoritative comparison + the digest
  //    emitter. Bind the run-id ONCE and pass it to the child, so the digest the child emits
  //    and our step-3 interlock agree even when FIGMA_PIPELINE_RUN_ID is not exported into our
  //    own env (a bare shell assignment is not inherited; without this, parent and child would
  //    each mint a different timestamp id and the interlock would block every ship).
  const runId = pipelineRunId(stem)
  const finalDriver = join(HERE, 'run-figma-gates.mjs')
  let bundleOk = true
  try {
    // The driver re-runs census when a registry entry consulted during prebuild
    // changed. Calling evidence-bundle directly would certify a stale consult
    // after finalize-task's task-scoped registry reconciliation.
    execFileSync(process.execPath, [finalDriver, stem, '--stage', 'final'], { stdio: 'inherit', env: { ...process.env, FIGMA_PIPELINE_RUN_ID: runId } })
  } catch { bundleOk = false }

  // 2. Read this run's verdict from the bundle report; require a shippable overall.
  const reportsDir = process.env.FIGMA_REPORTS_DIR || figmaPath('reports')
  const evidencePath = join(reportsDir, `evidence-${stem}.json`)
  if (!existsSync(evidencePath)) block(`final evidence bundle did not produce evidence-${stem}.json — comparison did not run`)
  let evidence
  try { evidence = JSON.parse(readFileSync(evidencePath, 'utf8')) } catch (e) { block(`evidence-${stem}.json unreadable: ${e.message}`) }
  // Pin the parsed report to THIS ship attempt (mirrors the step-3 digest pinning below).
  // Nothing serializes a concurrent figma:screens pull session against this run: its
  // `--stage prebuild --fresh` bundle's atomic rename can land on evidence-<stem>.json in
  // the window between our child exiting and this read, silently swapping the verdict.
  // Checked BEFORE the overall/bundleOk verdict so a swapped report is diagnosed as the
  // stale/foreign bundle it is, not as a verdict failure.
  if (String(evidence.stage || '') !== 'final') {
    block(`evidence-${stem}.json stage ${JSON.stringify(evidence.stage)} is not final — another bundle overwrote the final report in the read window; re-run ship-done`)
  }
  if (evidence.taskStem !== stem) {
    block(`evidence-${stem}.json taskStem ${JSON.stringify(evidence.taskStem)} does not match ${JSON.stringify(stem)} — a bundle from another task cannot certify this one`)
  }
  if (evidence.pipelineRunId !== runId) {
    block(`evidence-${stem}.json pipelineRunId ${JSON.stringify(evidence.pipelineRunId)} does not match this ship's run ${JSON.stringify(runId)} — a concurrent bundle overwrote the verdict; re-run ship-done`)
  }
  if (JSON.stringify(evidence.requiredReports) !== JSON.stringify(CANONICAL_FINAL_REPORTS)) {
    block(`evidence-${stem}.json requiredReports is not the canonical final set ${CANONICAL_FINAL_REPORTS.join(', ')} — a subset bundle cannot certify shipment`)
  }
  const overall = String(evidence.overall || '').toUpperCase()
  if (!bundleOk || !isShippableOverall(overall)) {
    block(`final visual evidence is ${overall || 'MISSING'} (need PASS or a reviewed WARN) — UI task may not ship uncompared`)
  }
  // A non-PASS (reviewed WARN) overall may ship ONLY as a recorded caveat — enforce the SAME
  // coupling verify-done.mjs audits at commit time, so ship-done never moves a WARN task that
  // would then be flagged stuck-in-done/. (isShippableOverall already rejected BLOCKER/INCOMPLETE.)
  if (overall !== 'PASS') {
    const status = outcomeAppendixStatus(md).toLowerCase()   // appendix-anchored, never a stray body `**Status**`
    if (status !== 'completed-with-caveats') {
      // A FIXABLE precondition (edit the appendix), NOT a screens/gate failure — so `fail`
      // (exit 1), never `block` (exit 2 → BLOCKED[figma-screens] → goto Step 4). This matches
      // the Step-6b exit-1 remediation in outcome-appendix.md: fix the Status field + ### Caveats.
      fail(`overall=${overall} (reviewed WARN) requires "Status: completed-with-caveats" + a ### Caveats note before the move, got ${JSON.stringify(status || '(missing)')}`)
    }
    // The caveat must be REAL — a WARN can't ship with an empty `### Caveats\n- none`.
    if (!caveatsHaveContent(md)) {
      fail(`overall=${overall} (reviewed WARN) requires at least one real "### Caveats" bullet (not "- none") describing why the WARN is accepted`)
    }
    console.error(`ship-done: NOTE overall=${overall}; recorded as completed-with-caveats`)
  }

  // 3. Read the code-emitted digest and confirm it is valid + bound to THIS run.
  const digestPath = join(reportsDir, `figma-meta-${stem}.txt`)
  if (!existsSync(digestPath)) block(`final bundle emitted no figma-meta-${stem}.txt digest — cannot certify the comparison`)
  const digestLine = readFileSync(digestPath, 'utf8').trim()
  const meta = parseFigmaMeta(digestLine)
  if (!meta) block(`emitted digest is not a valid final Figma-meta line: ${digestLine}`)
  if (meta.pipelineRunId !== runId) block(`digest pipelineRunId ${meta.pipelineRunId} != current run ${runId}`)
  if (meta.taskStem !== stem) block(`digest taskStem ${meta.taskStem} != current task ${stem} — a digest from another task cannot certify this one`)

  // 4. Stage the ship receipts BEFORE publication — byte-verbatim copies of the
  //    three certification files the digest binds (evidence report, screenshot report,
  //    emitted digest). The reports cache is gitignored/ephemeral: in a fresh CI clone
  //    verify-done.mjs would have NOTHING to re-bind the shipped digest against, so a
  //    structurally-valid green digest REPLAYED from a previous run of the same task
  //    (design unchanged, code changed) would pass every cache-independent check. Receipts
  //    eventually live under orchestrator/tasks/evidence/figma-ship/<stem>/ (committed — the same
  //    precedent as the frozen component binding evidence), so the re-bind works everywhere.
  //    Byte-verbatim is load-bearing: the digest's evidenceReportHash/screenshotReportHash
  //    must still match the receipts' sha256. Staging is transaction-scoped and cannot
  //    overwrite a previous ship's committed receipts. Only after the no-clobber done link
  //    succeeds do we publish the staged set to its canonical location. This ordering keeps
  //    a concurrent EEXIST loser from corrupting the winner's certification.
  const receiptsRoot = process.env.FIGMA_SHIP_RECEIPTS_DIR || join(PROJECT_ROOT, 'orchestrator', 'tasks', 'evidence', 'figma-ship')
  const receiptsDir = receiptDestination(receiptsRoot)
  const stageDir = receiptStage(receiptsRoot, finalization)
	  const screenshotPath = join(reportsDir, `screenshot-${stem}.json`)
	  try {
	    ensureRealDirectory(stageDir, 'transaction receipt stage')
    atomicCopy(evidencePath, join(stageDir, `evidence-${stem}.json`))
    atomicCopy(screenshotPath, join(stageDir, `screenshot-${stem}.json`))
    atomicCopy(digestPath, join(stageDir, `figma-meta-${stem}.txt`))
    const tokenReceipt = buildTaskObservationReceipt({
      taskStem: stem,
      transactionId: finalization.transactionId,
      screensRoot: screensRootOrFail(),
    })
    if (tokenReceipt.manifestHash !== meta.tokenObservationManifestHash) {
      throw new Error('token observation manifest changed after the final evidence digest was emitted')
    }
    atomicWriteExact(tokenReceipt.manifestBytes, join(stageDir, 'token-observations-manifest.json'))
    const tokenSidecarsStage = join(stageDir, 'token-observations')
    ensureRealDirectory(tokenSidecarsStage, 'transaction token observation receipt stage')
    for (const sidecar of tokenReceipt.sidecars) {
      atomicWriteExact(sidecar.bytes, join(tokenSidecarsStage, sidecar.basename))
    }
    const receiptManifestPath = `orchestrator/tasks/evidence/figma-ship/${stem}/token-observations-manifest.json`
    const ingestionIntent = buildTaskIngestionIntent({
      receipt: tokenReceipt,
      expectedGenerationRevision: currentGenerationRevision(),
      receiptManifestPath,
    })
    atomicWriteExact(ingestionIntent.bytes, join(stageDir, 'token-source-ingestion-intent.json'))
    // Census receipt: the census report lives in the gitignored .cache and any later
    // re-pull/prune deletes it — snapshot it so the site's done-view can prefer the
    // receipt when the live report is gone (the bundle pins its hash, so the copy is
    // verifiable). Best effort like the specs baseline: census is advisory prep data,
    // its absence never blocks a ship.
    try {
      const censusPath = join(reportsDir, `census-${stem}.json`)
      if (existsSync(censusPath)) atomicCopy(censusPath, join(stageDir, `census-${stem}.json`))
    } catch { /* advisory — never blocks the ship */ }
    // C1 drift-auto-stale baseline: snapshot the per-screen `<Screen>.spec.json` set (the design
    // the code was CERTIFIED against) into the committed receipt. The live screens cache can be
    // re-pulled/edited after ship, so only this frozen copy is a trustworthy baseline for detecting
    // a post-ship Figma MOVE (sweep-done-drift.mjs diffs it against a fresh shadow re-pull). Best
    // effort: a task with no specs (pixel-only / no `## Design` spec) simply gets no baseline and
    // the sweep skips it. NEVER fails the ship — a stale-detection baseline is not certification.
    try {
      const specsSrc = join(screensRootOrFail(), stem)
      if (existsSync(specsSrc)) {
        const specFiles = readdirSync(specsSrc).filter((f) => f.endsWith('.spec.json'))
        if (specFiles.length) {
          const specsDst = join(stageDir, 'specs')
	          ensureRealDirectory(specsDst, 'transaction receipt specs stage')
          for (const f of specFiles) atomicCopy(join(specsSrc, f), join(specsDst, f))
        }
      }
    } catch { /* baseline is advisory — a copy failure here never blocks the ship */ }
  } catch (e) {
    fail(`failed to stage ship receipts under ${stageDir}: ${e.message} — an unreceipted ship must not move (verify-done could not re-bind the digest in a fresh clone)`)
  }
  // Re-verify the STAGED receipts against the digest: a mismatch means a concurrent bundle
  // overwrote a report in the window between the step-2/3 pins above and the copy (the same
  // race those pins diagnose) — the receipt would then certify different bytes than the
  // digest claims, poisoning every future verify-done re-bind for this task.
  const receipt = validateReceiptSet(stageDir, digestLine, meta)
  if (!receipt.evidenceHash || receipt.evidenceHash.toLowerCase() !== String(meta.evidenceReportHash).toLowerCase()) {
    block(`ship receipt evidence-${stem}.json hash does not match the digest's evidenceReportHash — a concurrent bundle overwrote the report in the copy window; re-run ship-done`)
  }
  if (!receipt.screenshotHash || receipt.screenshotHash.toLowerCase() !== String(meta.screenshotReportHash).toLowerCase()) {
    block(`ship receipt screenshot-${stem}.json hash does not match the digest's screenshotReportHash — a concurrent bundle overwrote the report in the copy window; re-run ship-done`)
  }
  if (receipt.digestLine !== digestLine) {
    block(`ship receipt figma-meta-${stem}.txt does not byte-match the emitted digest — a concurrent bundle overwrote it in the copy window; re-run ship-done`)
  }
  preflightReceiptDestination(receiptsDir)
  console.log(`ship-done: staged ship receipts and token ingestion intent → ${stageDir}`)

  // 5. Inject the machine digest into the appendix's ### Execution log (replace any
  //    pre-existing Figma-meta line so the agent can never substitute a hand-typed one).
  try { md = injectOutcomeFigmaMeta(md, digestLine) } catch (e) { fail(e.message) }
  preparePublicationFile(publicationFile, md, statSync(todoFile).mode & 0o777)
  console.log(`ship-done: prepared immutable publication with code-emitted Figma-meta digest (overall=${overall})`)
}
if (!isUiTask) preparePublicationFile(publicationFile, md, statSync(todoFile).mode & 0o777)
if (process.env.SHIP_DONE_FAILPOINT === 'after-candidate') {
  console.error('ship-done: injected abrupt failure after immutable publication preparation')
  process.exit(97)
}

// 6. The sanctioned no-clobber move. A hard link publishes `done/` with
// O_EXCL-like EEXIST semantics; unlike POSIX rename it can never overwrite a
// concurrently-created done file. If the process dies between link and unlink,
// the active finalization transaction recognizes the two names as the same
// inode and safely finishes the unlink on retry.
const publicationAuth = authorizeFinalization(stem)
if (publicationAuth.transactionId !== finalization.transactionId) fail('finalization transaction changed while ship gates were running')
safeTaskFile(todoFile, true)
if (sha256(readFileSync(todoFile)) !== publicationAuth.source.intendedHash) {
  fail('todo task changed while ship gates were running; refusing to publish stale or foreign bytes')
}
if (sha256(readFileSync(join(PROJECT_ROOT, 'orchestrator', 'project-config.md'))) !== publicationAuth.figma.configHash) {
  fail('project-config.md changed while ship gates were running; refusing to publish under stale gate applicability')
}
const sourceFile = publicationFile
verifyPublicationFile(sourceFile, publicationAuth)
try {
  linkSync(sourceFile, doneFile)
  const linkedTodo = lstatSync(sourceFile)
  const linkedDone = lstatSync(doneFile)
  if (linkedTodo.isSymbolicLink() || linkedDone.isSymbolicLink() || !linkedTodo.isFile() || !linkedDone.isFile() ||
      linkedTodo.dev !== linkedDone.dev || linkedTodo.ino !== linkedDone.ino) {
    fail('no-clobber publication did not produce two regular names for the same task inode; todo was preserved')
  }
  syncDir(dirname(doneFile))
} catch (e) {
  if (e && e.code === 'EEXIST') fail(`done/${stem}.md appeared during publication; refusing to overwrite it`)
  fail(`could not publish done/${stem}.md without clobbering: ${e.message}`)
}
if (process.env.SHIP_DONE_FAILPOINT === 'after-link') {
  console.error('ship-done: injected abrupt failure after no-clobber link')
  process.exit(97)
}
if (isUiTask) {
  const receiptsRoot = process.env.FIGMA_SHIP_RECEIPTS_DIR || join(PROJECT_ROOT, 'orchestrator', 'tasks', 'evidence', 'figma-ship')
  publishStagedReceipts(receiptStage(receiptsRoot, publicationAuth), receiptDestination(receiptsRoot), publicationAuth)
}
safeDetachTodo(todoFile, doneFile, publicationAuth)
console.log(`ship-done: moved ${stem}.md → done/`)

function sha256(value) { return `sha256:${createHash('sha256').update(value).digest('hex')}` }
function publicationProofPath(done, marker) {
  return join(tasksDir, 'todo', `.finalize-${stem}-${marker.transactionId}.ship`)
}
function detachProofPath(todo, marker) {
  return join(dirname(todo), `.finalize-${stem}-${marker.transactionId}.detach.md`)
}
function receiptDestination(root) { return join(root, stem) }
function receiptStage(_root, marker) {
  const stateDir = process.env.FINALIZE_STATE_DIR || join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'finalizations')
  return join(stateDir, `.finalize-${stem}-${marker.transactionId}.receipts`)
}
function inspectRegular(path, label, required = false) {
  let st
  try { st = lstatSync(path) } catch (e) {
    if (e && e.code === 'ENOENT' && !required) return null
    if (e && e.code === 'ENOENT') fail(`${label} is missing`)
    fail(`cannot inspect ${label}: ${e.message}`)
  }
  if (st.isSymbolicLink() || !st.isFile()) fail(`${label} must be a regular file, not a symlink or special file`)
  return st
}
function sameIdentity(a, b) { return a && b && a.dev === b.dev && a.ino === b.ino }
function cleanupPublicationTemps(path) {
  const dir = dirname(path)
  const prefix = `${basename(path)}.tmp.`
  let removed = false
  for (const name of readdirSync(dir)) {
    if (!name.startsWith(prefix)) continue
    const tmp = join(dir, name)
    inspectRegular(tmp, 'transaction publication temporary proof', true)
    unlinkSync(tmp)
    removed = true
  }
  if (removed) syncDir(dir)
}
function preparePublicationFile(path, text, mode) {
  cleanupPublicationTemps(path)
  const expected = Buffer.from(text)
  const existing = inspectRegular(path, 'transaction publication proof')
  if (existing) {
    if (sha256(readFileSync(path)) !== sha256(expected)) fail('transaction publication proof differs from the freshly certified task; refusing to replace it')
    return
  }
  const tmp = uniqueTemp(path)
  let fd
  try {
    fd = openSync(tmp, 'wx', mode)
    writeFileSync(fd, expected)
    fsyncSync(fd)
    closeSync(fd); fd = undefined
    // A hard link is the no-clobber commit for the proof itself. rename(tmp, path)
    // would overwrite a same-transaction proof created by a surviving process.
    linkSync(tmp, path)
    syncDir(dirname(path))
  } catch (e) {
    if (e && e.code === 'EEXIST') {
      const committed = inspectRegular(path, 'transaction publication proof', true)
      if (!committed || sha256(readFileSync(path)) !== sha256(expected)) fail('a different transaction publication proof already exists')
    } else throw e
  } finally {
    if (fd !== undefined) try { closeSync(fd) } catch {}
    try { unlinkSync(tmp); syncDir(dirname(tmp)) } catch {}
  }
}
function verifyPublicationFile(path, marker) {
  inspectRegular(path, 'transaction publication proof', true)
  const text = readFileSync(path, 'utf8')
  if (sha256(logicalTaskText(text)) !== marker.source.intendedLogicalHash) {
    fail('transaction publication proof does not match the finalization intent')
  }
  return text
}
function digestFromPublication(text) {
  const inspected = inspectOutcomeFigmaMeta(text)
  if (!inspected.hasOutcome || inspected.executionHeaders.length !== 1 || inspected.misplacedLines.length || inspected.executionLines.length !== 1) {
    fail(`transaction publication must contain exactly one code-emitted Figma meta line inside the final Outcome Execution log; found ${inspected.executionLines.length} authoritative and ${inspected.misplacedLines.length} misplaced`)
  }
  const line = inspected.executionLines[0].line.trim()
  const meta = parseFigmaMeta(line)
  if (!meta || meta.taskStem !== stem) fail('transaction publication contains an invalid or foreign Figma meta line')
  return { line, meta }
}
function validateReceiptSet(dir, digestLine, meta) {
  const evidence = join(dir, `evidence-${stem}.json`)
  const screenshot = join(dir, `screenshot-${stem}.json`)
  const digest = join(dir, `figma-meta-${stem}.txt`)
  inspectRegular(evidence, 'staged evidence receipt', true)
  inspectRegular(screenshot, 'staged screenshot receipt', true)
  inspectRegular(digest, 'staged Figma digest receipt', true)
  const receipt = {
    evidenceHash: fileHash(evidence),
    screenshotHash: fileHash(screenshot),
    digestLine: readFileSync(digest, 'utf8').trim(),
  }
  if (receipt.evidenceHash.toLowerCase() !== String(meta.evidenceReportHash || '').toLowerCase() ||
      receipt.screenshotHash.toLowerCase() !== String(meta.screenshotReportHash || '').toLowerCase() ||
      receipt.digestLine !== digestLine) {
    fail('transaction receipt set does not match the code-emitted digest; refusing publication')
  }
  let tokenReceipt
  try {
    tokenReceipt = validateCommittedTaskObservationReceipt({
      taskStem: stem,
      transactionId: finalization.transactionId,
      receiptDirectory: dir,
      expectedManifestHash: meta.tokenObservationManifestHash,
    })
    const intentPath = join(dir, 'token-source-ingestion-intent.json')
    inspectRegular(intentPath, 'staged token ingestion intent', true)
    const intent = JSON.parse(readFileSync(intentPath, 'utf8'))
    validateTaskIngestionIntent(intent)
    if (intent.originTransactionId !== finalization.transactionId ||
        intent.taskStem !== stem ||
        intent.receiptManifestHash !== tokenReceipt.manifestHash ||
        intent.receiptManifestPath !== `orchestrator/tasks/evidence/figma-ship/${stem}/token-observations-manifest.json`) {
      fail('transaction token ingestion intent does not match its receipt')
    }
    receipt.tokenObservationManifestHash = tokenReceipt.manifestHash
    receipt.tokenIngestionIntentId = intent.intentId
    receipt.tokenIngestionIntentHash = fileHash(intentPath)
  } catch (error) {
    fail(`transaction token observation receipt is invalid: ${error.message}`)
  }
  return receipt
}
function preflightReceiptDestination(receiptsDir) {
  try { ensureRealDirectory(receiptsDir, 'ship receipts destination') }
  catch (e) { fail(`failed to write ship receipts under ${receiptsDir}: ${e.message} — task remains unpublished`) }
}
function publishStagedReceipts(stageDir, receiptsDir, marker) {
  const publication = verifyPublicationFile(publicationProofPath(doneFile, marker), marker)
  const { line, meta } = digestFromPublication(publication)
  validateReceiptSet(stageDir, line, meta)
  try {
    preflightReceiptDestination(receiptsDir)
    for (const name of [`evidence-${stem}.json`, `screenshot-${stem}.json`, `figma-meta-${stem}.txt`, `census-${stem}.json`]) {
      const source = join(stageDir, name)
      if (existsSync(source)) atomicCopy(source, join(receiptsDir, name))
    }
    const specs = join(stageDir, 'specs')
    if (existsSync(specs)) {
      const target = join(receiptsDir, 'specs')
      ensureRealDirectory(target, 'ship receipt specs destination')
      for (const name of readdirSync(specs).filter((item) => item.endsWith('.spec.json'))) atomicCopy(join(specs, name), join(target, name))
    }
    atomicWriteExact(readFileSync(join(stageDir, 'token-observations-manifest.json')),
      join(receiptsDir, 'token-observations-manifest.json'))
    const tokenSidecars = join(stageDir, 'token-observations')
    const tokenTarget = join(receiptsDir, 'token-observations')
    ensureRealDirectory(tokenTarget, 'ship token observation receipt destination')
    for (const name of readdirSync(tokenSidecars).sort()) {
      atomicWriteExact(readFileSync(join(tokenSidecars, name)), join(tokenTarget, name))
    }
    const intentBytes = readFileSync(join(stageDir, 'token-source-ingestion-intent.json'))
    atomicWriteExact(intentBytes, join(receiptsDir, 'token-source-ingestion-intent.json'))
    const intent = JSON.parse(intentBytes.toString('utf8'))
    validateTaskIngestionIntent(intent)
    const outbox = process.env.FIGMA_TOKEN_INGESTION_DIR ||
      join(PROJECT_ROOT, 'orchestrator', '.cache', 'figma', 'token-source-ingestion')
    ensureRealDirectory(outbox, 'token source ingestion outbox')
    atomicWriteExact(intentBytes, join(outbox, `${intent.intentId}.json`))
  } catch (e) {
    fail(`failed to write ship receipts under ${receiptsDir}: ${e.message} — recovery authority remains active`)
  }
  validateReceiptSet(receiptsDir, line, meta)
  console.log(`ship-done: published ship receipts and committed token ingestion intent → ${receiptsDir}`)
}
function currentGenerationRevision() {
  const pointerPath = join(PROJECT_ROOT, 'orchestrator', 'figma', 'manifests', 'current-generation.json')
  const pointerStat = inspectRegular(pointerPath, 'current Figma generation pointer')
  if (!pointerStat) return 'none'
  if (pointerStat.nlink !== 1) fail('current Figma generation pointer must be a single-link regular file')
  let pointer
  try { pointer = JSON.parse(readFileSync(pointerPath, 'utf8')) } catch (error) {
    fail(`current Figma generation pointer is invalid: ${error.message}`)
  }
  if (pointer.schemaVersion !== 2 || !/^gen-[a-f0-9]{32}$/.test(String(pointer.generationId || '')) ||
      !/^sha256:[a-f0-9]{64}$/.test(String(pointer.manifestHash || ''))) {
    fail('current Figma generation pointer is invalid')
  }
  return pointer.manifestHash
}
function restoreDetachedConflict(detach, todo) {
  if (!existsSync(todo)) {
    try { linkSync(detach, todo); syncDir(dirname(todo)) } catch (e) {
      if (!e || e.code !== 'EEXIST') fail(`could not restore concurrently replaced todo task from ${detach}: ${e.message}`)
    }
  }
}
function reconcileDetach(todo, done, marker) {
  const detach = detachProofPath(todo, marker)
  const detachedStat = inspectRegular(detach, 'transaction detachment proof')
  if (!detachedStat) return false
  const doneStat = inspectRegular(done, 'published done task', true)
  const detachedBytes = readFileSync(detach)
  const owned = sha256(detachedBytes) === marker.source.intendedHash
  const publicationStat = inspectRegular(publicationProofPath(done, marker), 'transaction publication proof')
  const doneText = readFileSync(done, 'utf8')
  const doneOwned = publicationStat && sameIdentity(publicationStat, doneStat) &&
    sha256(logicalTaskText(doneText)) === marker.source.intendedLogicalHash
  if (!owned || !doneOwned) {
    restoreDetachedConflict(detach, todo)
    fail(`${!owned ? 'todo task changed during final publication' : 'published done task no longer belongs to this transaction'}; intended todo bytes were preserved at ${detach} and recovery remains active`)
  }
  // The detached inode is either still held by done/ or byte-identical to the
  // immutable intended snapshot. Removing this private proof cannot discard the
  // task; the authoritative done file already carries the same logical intent.
  unlinkSync(detach)
  syncDir(dirname(detach))
  return true
}
function safeDetachTodo(todo, done, marker) {
  reconcileDetach(todo, done, marker)
  const current = inspectRegular(todo, 'todo task')
  if (!current) return
  const detach = detachProofPath(todo, marker)
  if (existsSync(detach)) fail('transaction detachment proof unexpectedly still exists')
  try { renameSync(todo, detach); syncDir(dirname(todo)) }
  catch (e) { fail(`could not atomically detach todo task without deleting it: ${e.message}`) }
  if (process.env.SHIP_DONE_FAILPOINT === 'after-detach') {
    console.error('ship-done: injected abrupt failure after safe todo detachment')
    process.exit(97)
  }
  reconcileDetach(todo, done, marker)
  if (existsSync(todo)) fail('todo task reappeared during final publication; recovery remains active')
}
function recoverPublication(todo, done, publication, marker) {
  // A crash after todo -> private-detach rename leaves no canonical todo path.
  // Reconcile that proof before trusting/rejecting the visible done file, so a
  // replaced done artifact can never cause the last intended todo bytes to be
  // discarded or stranded silently.
  reconcileDetach(todo, done, marker)
  const todoStat = inspectRegular(todo, 'todo task')
  let doneStat = inspectRegular(done, 'done task')
  const publicationStat = inspectRegular(publication, 'transaction publication proof')

  if (publicationStat) {
    const publicationText = verifyPublicationFile(publication, marker)
    const liveHash = sha256(readFileSync(join(PROJECT_ROOT, 'orchestrator', 'project-config.md')))
    if (liveHash !== marker.figma.configHash) fail('transaction publication proof is inconsistent with the frozen Figma configuration')
    if (!doneStat) {
      if (!todoStat || sha256(readFileSync(todo)) !== marker.source.intendedHash) {
        fail('todo task changed before interrupted publication could be resumed; refusing to publish over the conflict')
      }
      try { linkSync(publication, done); syncDir(dirname(done)) }
      catch (e) {
        if (!e || e.code !== 'EEXIST') fail(`could not resume no-clobber done publication: ${e.message}`)
      }
      doneStat = inspectRegular(done, 'done task', true)
    }
    if (!sameIdentity(publicationStat, doneStat)) fail('done task is not the transaction-owned publication; refusing to overwrite either artifact')
    if (marker.figma.enabled && hasPullableDesign(publicationText)) {
      const receiptsRoot = process.env.FIGMA_SHIP_RECEIPTS_DIR || join(PROJECT_ROOT, 'orchestrator', 'tasks', 'evidence', 'figma-ship')
      publishStagedReceipts(receiptStage(receiptsRoot, marker), receiptDestination(receiptsRoot), marker)
    }
    safeDetachTodo(todo, done, marker)
    return true
  }

  if (todoStat && doneStat) {
    if (!sameIdentity(todoStat, doneStat)) fail(`both todo/${stem}.md and done/${stem}.md exist with different identities; refusing to choose or overwrite either`)
    const doneText = readFileSync(done, 'utf8')
    if (sha256(logicalTaskText(doneText)) !== marker.source.intendedLogicalHash) fail('interrupted publication does not match the finalization intent')
    safeDetachTodo(todo, done, marker)
    return true
  }
  reconcileDetach(todo, done, marker)
  return false
}
function safeTaskFile(path, required) {
  let st
  try { st = lstatSync(path) } catch (e) {
    if (e && e.code === 'ENOENT' && !required) return null
    if (e && e.code === 'ENOENT') fail(`todo/${stem}.md not found (move only ships a task already in todo/)`)
    fail(`cannot inspect task file ${path}: ${e.message}`)
  }
  if (st.isSymbolicLink() || !st.isFile()) fail(`${path} must be a regular file, not a symlink or special file`)
  if (!required) fail(`done/${stem}.md already exists (stem must live in exactly one column)`)
  return st
}
function authorizeFinalization(taskStem) {
  const transactionId = String(process.env.FINALIZE_TRANSACTION_ID || '')
  if (!transactionId) fail('missing FINALIZE_TRANSACTION_ID; ship-done may only run inside finalize-task.mjs')
  const stateDir = process.env.FINALIZE_STATE_DIR || join(PROJECT_ROOT, 'orchestrator', '.cache', 'tasks', 'finalizations')
  const path = join(stateDir, `${taskStem}.json`)
  let st
  try { st = lstatSync(path) } catch (e) {
    if (e && e.code === 'ENOENT') fail(`active finalization marker for ${taskStem} is missing; direct ship-done invocation is forbidden`)
    fail(`cannot inspect finalization marker: ${e.message}`)
  }
  if (st.isSymbolicLink() || !st.isFile() || st.size > 256 * 1024) fail('finalization marker must be a regular file no larger than 256 KiB')
  let marker
  try { marker = JSON.parse(readFileSync(path, 'utf8')) } catch (e) { fail(`finalization marker is corrupt: ${e.message}`) }
  if (!marker || marker.version !== 1 || marker.stem !== taskStem || marker.transactionId !== transactionId ||
      !/^[A-Za-z0-9][A-Za-z0-9._-]{0,164}$/.test(String(marker.transactionId || '')) ||
      marker.status !== 'running' || marker.phase !== 'ship' || !marker.phases || !marker.phases.ship || marker.phases.ship.state !== 'running' ||
      !marker.owner || marker.owner.pid !== process.ppid || marker.owner.hostname !== hostname() ||
      !marker.source || !/^sha256:[a-f0-9]{64}$/.test(String(marker.source.intendedHash || '')) || !/^sha256:[a-f0-9]{64}$/.test(String(marker.source.intendedLogicalHash || '')) ||
      !marker.figma || typeof marker.figma.enabled !== 'boolean' || !/^sha256:[a-f0-9]{64}$/.test(String(marker.figma.configHash || ''))) {
    fail('finalization marker does not authorize this process/transaction in the running ship phase')
  }
  return marker
}
