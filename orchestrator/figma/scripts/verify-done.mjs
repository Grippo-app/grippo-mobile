#!/usr/bin/env node
// verify-done.mjs — headless after-the-fact auditor for shipped tasks (H2's CI net behind
// the ship-done.mjs pre-`mv` interlock).
//
// Scans orchestrator/tasks/done/*.md. EVERY UI task (figmaEnabled: true AND a non-none
// `## Design`) MUST carry a structurally valid, code-emitted `- Figma meta:` digest whose
// overall is shippable, and — for a WARN verdict — a `Status: completed-with-caveats`
// (anchored on the structured field the appendix already carries). A UI task in done/
// without a valid digest is a
// violation, full stop. NON-UI done tasks are audited for the frozen Outcome APPENDIX SHAPE
// (ship-done validates it for every move, UI or not — this mirror closes the hand-mv escape
// for the non-UI class).
//
// Beyond the grammar net, two content bindings:
//   * designHash (when the digest carries it) is re-computed from the done file's CURRENT
//     `## Design` section — a post-ship design edit flips the certification red instead of
//     keeping a stale GREEN. Cache-independent: digest + done file are both committed.
//   * Content re-bind: the in-task digest must byte-match the code-emitted
//     figma-meta-<stem>.txt; the evidence/screenshot report hashes must match the digest's;
//     and on the hash-VERIFIED same-run evidence report the digest's `overall` must equal
//     the report's recorded `overall` (else a two-file forgery that flips a red run green in
//     both the digest line and figma-meta-<stem>.txt, leaving the hash fields on the
//     untouched red reports, would slip past the hash re-bind).
//
//     The re-bind source is the COMMITTED ship receipts ship-done.mjs copies at ship time
//     (orchestrator/tasks/evidence/figma-ship/<stem>/ — present in a fresh CI clone). EVERY UI
//     ship writes them (ship-done step 4 FAILS the ship if it cannot), so a UI done task MUST
//     have a receipts dir — its absence means the digest was replayed from another run or the
//     receipts were deleted after ship (a violation, not a skip). Committed receipts are the
//     one authoritative re-bind source.
//
// Exit: 0 = clean (every UI done task carries a valid digest); 2 = ≥1 violation.

import { existsSync, lstatSync, readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { createRequire } from 'node:module'
import { PROJECT_ROOT, figmaScreensRoot, readConfig, outcomeAppendixStatus } from './_util.mjs'
import { parseFigmaMeta, isShippableOverall, caveatsHaveContent } from './figma-meta.mjs'
import { assertTaskStem, fileHash } from './report-utils.mjs'
import { inspectOutcomeFigmaMeta, outcomeShapeError } from './outcome-shape.mjs'

const require = createRequire(import.meta.url)
const { hasPullableDesign, hasMalformedDesign, parseDesignSources, uiTaskWithoutDesign, auditedNoneCount } = require('./design-parser.cjs')
const CANONICAL_FINAL_REPORTS = ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot']

const argv = process.argv.slice(2)
let targetStem = ''
for (let i = 0; i < argv.length; i++) {
  if (argv[i] === '--stem') {
    targetStem = String(argv[++i] || '')
  } else {
    console.error(`verify-done: unknown argument ${JSON.stringify(argv[i])}`)
    process.exit(1)
  }
}
if (targetStem) try { assertTaskStem(targetStem) }
catch {
  console.error(`verify-done: --stem must be canonical, got ${JSON.stringify(targetStem)}`)
  process.exit(1)
}
let cachedScreensRoot = null
const screensRootOrExit = () => {
  if (cachedScreensRoot) return cachedScreensRoot
  try { cachedScreensRoot = figmaScreensRoot(); return cachedScreensRoot } catch (e) {
    console.error(`verify-done: ${e.message}`)
    process.exit(1)
  }
}

const doneDir = join(PROJECT_ROOT, 'orchestrator', 'tasks', 'done')
if (!existsSync(doneDir)) { console.log('verify-done: no done/ dir — nothing to audit'); process.exit(0) }

const figmaEnabled = readConfig('figmaEnabled') === 'true'
let inventoryComponents = []
let validateCommittedTaskObservationReceipt = null
let validateTaskIngestionIntent = null
let shipDriftContract = null
if (figmaEnabled) {
  const [components, observationReceipt, ingestionIntent] = await Promise.all([
    import('./lib/design-components.mjs'),
    import('../tokens/task-observation-receipt.mjs'),
    import('../tokens/task-ingestion-intent.mjs'),
  ])
  inventoryComponents = components.componentProvenanceEntries()
  validateCommittedTaskObservationReceipt = observationReceipt.validateCommittedTaskObservationReceipt
  validateTaskIngestionIntent = ingestionIntent.validateTaskIngestionIntent
  shipDriftContract = require('./ship-drift-contract.cjs')
}
// Committed ship receipts (written by ship-done.mjs before every UI move) — the ONE re-bind
// source (present in a fresh CI clone); same env-override pattern as the other gate scripts'
// FIGMA_SUGGEST_EVIDENCE_DIR.
const receiptsRoot = process.env.FIGMA_SHIP_RECEIPTS_DIR || join(PROJECT_ROOT, 'orchestrator', 'tasks', 'evidence', 'figma-ship')
let files = readdirSync(doneDir).filter((f) => f.endsWith('.md')).sort()
if (targetStem) {
  const targetFile = `${targetStem}.md`
  if (!files.includes(targetFile)) {
    console.error(`verify-done: done/${targetFile} not found`)
    process.exit(2)
  }
  files = [targetFile]
}

let audited = 0
let nonUiAudited = 0
const violations = []
const notes = []   // advisory (B1 filename-tier) — printed after the loop, never affects exit
// W3-6 visibility: retired Figma anchors release the provenance backstop for their sources —
// the audit must SAY so (a silent release would look like a hole). Advisory, never affects exit.
const retiredComponents = inventoryComponents.filter((e) => e && e.figmaNodeRetired && typeof e.figmaNodeRetired === 'object' && String(e.figmaNodeRetired.reason || '').trim())
for (const e of retiredComponents) {
  notes.push(['component-mapping', `figma anchor RETIRED for ${e.component} (${e.figmaNodeRetired.reason}${e.figmaNodeRetired.at ? `, ${e.figmaNodeRetired.at}` : ''}) — the provenance backstop no longer holds its source; retired through the CAS mapping registry`])
}
// W3-5 visibility: a committed de-scope receipt is the auditable witness that a task's Figma
// comparison was DELIBERATELY dropped (the audit must say so, or the sanctioned act looks like
// a silent hole in the net). Advisory, never affects exit.
const descopeRoot = process.env.FIGMA_DESCOPE_RECEIPTS_DIR || join(PROJECT_ROOT, 'orchestrator', 'tasks', 'evidence', 'descope')
try {
  for (const f of readdirSync(descopeRoot).filter((f) => f.endsWith('.json')).sort()) {
    try {
      const r = JSON.parse(readFileSync(join(descopeRoot, f), 'utf8'))
      notes.push(['descope-receipt', `task ${r.stem || f.replace(/\.json$/, '')} was sanctioned-descoped (${String(r.reason || 'no reason recorded')}${r.at ? `, ${r.at}` : ''}${r.by ? `, by ${r.by}` : ''}) — receipt at tasks/evidence/descope/${f}`])
    } catch { notes.push(['descope-receipt', `unreadable de-scope receipt tasks/evidence/descope/${f} — the auditable witness is corrupt; re-run descope-task.mjs or fix the JSON`]) }
  }
} catch { /* absent dir = no de-scopes recorded */ }
// B2 erosion detector — the audited `— none (<why>)` opt-out is legitimate, but a product could
// quietly opt EVERY screen out one `— none` at a time until the visual-comparison guarantee is
// hollow. Count them across done/ and surface the erosion (advisory only, never affects exit).
// Both thresholds are guesses → env-overridable.
const nonePerTask = Number(process.env.FIGMA_NONE_PER_TASK || 4)
const noneCorpus = Number(process.env.FIGMA_NONE_CORPUS || 20)
let corpusNone = 0
const noneHeavy = []

for (const file of files) {
  const md = readFileSync(join(doneDir, file), 'utf8')
  if (!figmaEnabled) continue   // non-figma project — gate does not apply
  const stem = file.replace(/\.md$/, '')
  // B2 erosion tally (advisory) — count this task's audited `— none (<why>)` opt-outs before any
  // classification `continue`, so every figma done file is measured regardless of UI verdict.
  const noneN = auditedNoneCount(md)
  if (noneN > 0) { corpusNone += noneN; if (noneN >= nonePerTask) noneHeavy.push([file, noneN]) }
  // A MALFORMED `## Design` (typo/invalid URL) makes hasPullableDesign false, which would look
  // non-UI and skip the audit — the exact fail-open. Flag it: a broken design ref must be fixed,
  // it cannot classify a real UI task out of the mandatory-comparison audit.
  if (hasMalformedDesign(md)) { audited++; violations.push([file, 'UI task has a MALFORMED `## Design` (unparseable bullet / invalid screen / invalid or non-pullable Figma URL) — fix the design bullets; a broken ref cannot bypass the comparison audit']); continue }
  if (!hasPullableDesign(md)) {
    // UI-by-evidence backstop (W1 + B1): a task that PROVABLY targets a Figma node — a STRONG
    // signal (a cited node URL, or a `figmaNodeId:`/matrix component snapshot) — yet declares no
    // pullable `## Design` bullet is a real UI task self-classifying non-UI → VIOLATION. The WEAK
    // filename-only signal (a `*Screen.kt` edit, no node cited) is advisory:
    // it records a `notes[]` line but never a violation (a non-visual screen-file edit must not
    // fail the audit). Cache-independent (reads the committed task md), so it fires in a fresh CI clone.
    // Pass stem + committed provenance (mapping registry + inventories) so the provenance tier
    // catches a node-backed component whose in-text snapshot was stripped (the mapping survives).
    // Committed truth, so it fires in a fresh CI clone too. No-op when nothing is mapped.
    const uiVerdict = uiTaskWithoutDesign(md, { stem, inventory: inventoryComponents })
    if (uiVerdict && uiVerdict.level === 'block') { audited++; violations.push([file, uiVerdict.reason]); continue }
    if (uiVerdict && uiVerdict.level === 'warn') notes.push([file, uiVerdict.reason])
    // Warn-tier advisory falls through to the de-UI net + non-UI skip.
    // De-UI net: no pullable design, but screens were PULLED for this stem — the design refs
    // were edited away after the pull (the other fail-open the malformed check can't see).
    // Best-effort: the cache is ephemeral/gitignored, so this fires in the local/auto-commit
    // flow (where the bypass happens) and is silent in a fresh CI clone.
    const idxPath = join(screensRootOrExit(), file.replace(/\.md$/, ''), 'index.json')
    if (existsSync(idxPath)) {
      let pulledNodes = null
      try { pulledNodes = JSON.parse(readFileSync(idxPath, 'utf8')).nodes } catch {}
      if (pulledNodes && typeof pulledNodes === 'object' && Object.keys(pulledNodes).length) {
        audited++
        violations.push([file, `no pullable \`## Design\` bullet but ${Object.keys(pulledNodes).length} pulled screen(s) exist in the cache — design refs removed after pull (de-UI bypass); restore the bullets, or run the sanctioned de-scope: node orchestrator/figma/scripts/descope-task.mjs <stem> --reason "<why>" --yes (audited-none bullets + confined cache removal + committed receipt under orchestrator/tasks/evidence/descope/)`])
        continue
      }
    }
    // Genuinely non-UI task — the FIGMA gate does not apply, but the frozen Outcome appendix
    // does: ship-done validates the shape for EVERY task pre-move (UI + non-UI), so this
    // headless mirror must too — else a hand-mv'd or free-form non-UI done file escapes the
    // net entirely (the TASK_193 class: an improvised Outcome shipped past the parsers).
    nonUiAudited++
    let nonUiShapeError
    try { nonUiShapeError = outcomeShapeError(md) } catch (e) {
      console.error(`verify-done: ${e.message}`)
      process.exit(1)
    }
    if (nonUiShapeError) violations.push([file, `Outcome appendix is malformed (non-UI tasks carry the same frozen shape — ship-done validates it for every move): ${nonUiShapeError}`])
    continue
  }
  audited++
  // A UI task lacking `## Outcome` is NOT skipped — that absence is itself a hand-mv tell (a
  // finalized task always has the appendix), and it has no digest, so it is flagged below.

  let doneShapeError
  try { doneShapeError = outcomeShapeError(md) } catch (e) {
    console.error(`verify-done: ${e.message}`)
    process.exit(1)
  }
  if (doneShapeError) { violations.push([file, `Outcome appendix is malformed: ${doneShapeError}`]); continue }

  const digestInspection = inspectOutcomeFigmaMeta(md)
  if (digestInspection.misplacedLines.length) { violations.push([file, '`- Figma meta:` appears outside the final Outcome `### Execution log`; body/other-section text is not a ship credential']); continue }
  if (digestInspection.executionHeaders.length !== 1) { violations.push([file, `final Outcome must contain exactly one \`### Execution log\` section; found ${digestInspection.executionHeaders.length}`]); continue }
  if (digestInspection.executionLines.length !== 1) { violations.push([file, `final Outcome Execution log must contain exactly one \`- Figma meta:\` digest; found ${digestInspection.executionLines.length}`]); continue }
  const digestLine = digestInspection.executionLines[0].line
  const meta = parseFigmaMeta(digestLine)
  if (!meta) { violations.push([file, 'Figma-meta digest is structurally invalid']); continue }
  // Bind the digest to THIS task — a valid PASS digest copied verbatim from another shipped task
  // (a hand-`mv` forge) has the wrong taskStem and is caught here.
  const fileStem = file.replace(/\.md$/, '')
  if (meta.taskStem !== fileStem) { violations.push([file, `digest taskStem ${JSON.stringify(meta.taskStem)} != task ${fileStem} — a digest from another task cannot certify this one`]); continue }
  if (!isShippableOverall(meta.overall)) { violations.push([file, `digest overall=${meta.overall} is not shippable (BLOCKER/INCOMPLETE must never reach done/)`]); continue }

  const status = outcomeAppendixStatus(md).toLowerCase()   // appendix-anchored, never a stray body `**Status**`
  if (String(meta.overall).toUpperCase() !== 'PASS' && !caveatsHaveContent(md)) {
    violations.push([file, `digest overall=${meta.overall} (WARN) requires at least one real "### Caveats" bullet (not "- none")`])
  }
  if (String(meta.overall).toUpperCase() !== 'PASS' && status !== 'completed-with-caveats') {
    violations.push([file, `digest overall=${meta.overall} (WARN) requires Status: completed-with-caveats, got ${JSON.stringify(status || '(missing)')}`])
  }

  // Design binding: the digest certifies a SPECIFIC `## Design` section. If the done file's
  // current section hashes differently, the refs were edited after certification.
  const currentDesignHash = String(parseDesignSources([md]).sourceHash || '')
  if (String(meta.designHash).toLowerCase() !== currentDesignHash.toLowerCase()) {
    violations.push([file, `## Design was edited after certification (digest designHash ${meta.designHash.slice(0, 18)}… != current ${currentDesignHash.slice(0, 18)}…) — re-run the Figma comparison against the current design refs`])
  }

  // Content re-bind (see header). SINGLE scenario: every UI ship writes committed receipts (ship-done
  // step 4 FAILS the ship if it cannot), so a UI done task with a valid digest MUST have a receipts
  // dir. Its absence means
  // the digest was replayed from another run or the receipts were deleted after ship — a violation.
  // Receipts are the sole re-bind source (they exist in a fresh CI clone too), so every check
  // below is unconditional.
  const bindDir = join(receiptsRoot, fileStem)
  if (!existsSync(bindDir)) {
    violations.push([file, 'no committed ship receipts for this task — ship-done writes them before every move, so their absence means the digest was replayed from another run or the receipts were deleted after ship'])
    continue
  }
  const emittedDigestPath = join(bindDir, `figma-meta-${fileStem}.txt`)
  if (existsSync(emittedDigestPath)) {
    let emittedLine = ''
    try { emittedLine = readFileSync(emittedDigestPath, 'utf8').trim() } catch {}
    const emitted = emittedLine ? parseFigmaMeta(emittedLine) : null
    if (!emitted) {
      violations.push([file, 'ship receipt figma-meta digest is unreadable or structurally invalid — the receipt set was corrupted after ship; re-run ship-done to re-certify'])
    } else if (emitted.pipelineRunId !== meta.pipelineRunId) {
      violations.push([file, `in-task digest pipelineRunId ${JSON.stringify(meta.pipelineRunId)} does not match the ship receipt's ${JSON.stringify(emitted.pipelineRunId)} — the receipt is written by the same ship that injects the digest, so the digest was replaced after the ship (a digest replayed from another run)`])
    } else if (emittedLine !== digestLine.trim()) {
      violations.push([file, 'in-task Figma-meta digest does not byte-match the code-emitted figma-meta digest from the same pipeline run — the injected line was edited or forged'])
    }
  } else {
    violations.push([file, 'ship receipts dir exists but the figma-meta receipt is missing — ship-done writes all three receipts before the move, so a missing piece means the receipt set was tampered with after ship'])
  }
  for (const [reportName, hashField] of [['evidence', 'evidenceReportHash'], ['screenshot', 'screenshotReportHash']]) {
    const reportPath = join(bindDir, `${reportName}-${fileStem}.json`)
    if (!existsSync(reportPath)) {
      violations.push([file, `ship receipts dir exists but the ${reportName} receipt is missing — ship-done writes all three receipts before the move, so a missing piece means the receipt set was tampered with after ship`])
      continue
    }
    let reportJson = null
    try { reportJson = JSON.parse(readFileSync(reportPath, 'utf8')) } catch {
      violations.push([file, `ship receipt ${reportName}-${fileStem}.json is unreadable — the receipt set was corrupted after ship; re-run ship-done to re-certify`])
      continue
    }
    if (!reportJson || reportJson.pipelineRunId !== meta.pipelineRunId) {
      violations.push([file, `in-task digest pipelineRunId ${JSON.stringify(meta.pipelineRunId)} does not match the ship receipt ${reportName} report's ${JSON.stringify(reportJson && reportJson.pipelineRunId)} — the digest was replaced after the ship (a digest replayed from another run)`])
      continue
    }
    const actual = fileHash(reportPath)
    const hashOk = actual && actual.toLowerCase() === String(meta[hashField] || '').toLowerCase()
    if (!hashOk) {
      violations.push([file, `digest ${hashField} does not match the ship receipt ${reportName} report from the same pipeline run — report edited after certification or digest forged`])
      continue
    }
    // Overall cross-check: the digest's `overall` is emitted straight from the evidence
    // bundle's computed overall, so on a hash-VERIFIED same-run evidence report it must equal
    // that report's recorded `overall`. Without this, a two-file forgery — flip a genuinely
    // red run's `overall` INCOMPLETE→PASS in BOTH the injected digest line AND the code-emitted
    // figma-meta-<stem>.txt, leaving the hash fields pointing at the untouched red reports —
    // passes the byte-match + hash re-bind (the hashes still match the real reports) yet ships
    // a red comparison green. The hash proves the report is authentic; its overall is therefore
    // authoritative over the digest's claim.
    if (reportName === 'evidence') {
      if (JSON.stringify(reportJson.requiredReports) !== JSON.stringify(CANONICAL_FINAL_REPORTS)) {
        violations.push([file, `hash-verified evidence receipt requiredReports is not the canonical final set ${CANONICAL_FINAL_REPORTS.join(', ')} — a subset bundle cannot certify a done task`])
      }
      const reportOverall = String(reportJson.overall || '').toUpperCase()
      if (reportOverall && reportOverall !== String(meta.overall || '').toUpperCase()) {
        violations.push([file, `digest overall=${meta.overall} contradicts the hash-verified same-run evidence report overall=${reportOverall} — the digest's verdict was forged (a red run relabeled green)`])
      }
    }
  }
  try {
    const rawManifest = JSON.parse(readFileSync(join(bindDir, 'token-observations-manifest.json'), 'utf8'))
    const tokenReceipt = validateCommittedTaskObservationReceipt({
      taskStem: fileStem,
      transactionId: rawManifest.originTransactionId,
      receiptDirectory: bindDir,
      expectedManifestHash: meta.tokenObservationManifestHash,
    })
    const intentBytes = readFileSync(join(bindDir, 'token-source-ingestion-intent.json'))
    const intent = JSON.parse(intentBytes.toString('utf8'))
    validateTaskIngestionIntent(intent)
    if (intent.taskStem !== fileStem ||
        intent.originTransactionId !== tokenReceipt.manifest.originTransactionId ||
        intent.receiptManifestHash !== tokenReceipt.manifestHash ||
        intent.receiptManifestPath !== `orchestrator/tasks/evidence/figma-ship/${fileStem}/token-observations-manifest.json`) {
      throw new Error('token ingestion intent does not bind the committed task receipt')
    }
  } catch (error) {
    violations.push([file, `token observation receipt/ingestion intent is missing, unsafe, or invalid: ${error.message}`])
  }
}

// C1 drift-auto-stale (advisory — a SEPARATE tally, NEVER a violation, NEVER affects the exit
// code). A committed drift-stale-<stem>.json means the post-ship drift sweep (sweep-done-drift.mjs)
// found the Figma design MOVED since this task was certified. Staleness ≠ forgery: it does not
// accuse the shipped task of anything, it flags that the cert is now out of date and an
// actualization task is warranted — so it stays strictly out of the violations/exit-2 path.
const staleness = []
for (const file of files) {
  const fileStem = file.replace(/\.md$/, '')
  const stalePath = join(receiptsRoot, fileStem, `drift-stale-${fileStem}.json`)
  if (!existsSync(stalePath)) continue
  let stale = null
  try {
    const stat = lstatSync(stalePath)
    if (!stat.isFile() || stat.isSymbolicLink() || stat.nlink !== 1 || stat.size > shipDriftContract.MAX_BYTES) {
      throw new Error('marker must be one bounded regular file')
    }
    stale = JSON.parse(readFileSync(stalePath, 'utf8'))
    if (!shipDriftContract.validMarker(stale, fileStem)) throw new Error('marker violates the current contract')
  } catch {
    violations.push([file, 'committed post-ship drift marker is unsafe, unreadable, or invalid'])
    continue
  }
  staleness.push([file, `${stale.driftedScreens.length} screen(s) drifted in Figma since ship (${stale.driftedScreens.map((s) => s.screen).join(', ')}); staleAt ${stale.staleAt} — create an actualization task to re-certify`])
}

console.log(`verify-done: audited ${audited} UI task(s) + ${nonUiAudited} non-UI appendix shape(s), ${violations.length} violation(s)`)
for (const [file, why] of violations) console.error(`  ✗ ${file}: ${why}`)
// Advisory (B1 filename-tier): a done task touches a screen/dialog file but declares no `## Design`
// and cites no node. Not a violation (a non-visual screen-file edit is legitimate) — surfaced so a
// genuinely-visual one that slipped through can be noticed. Never affects the exit code.
if (notes.length) {
  console.error(`verify-done: ${notes.length} advisory note(s) (filename-only, not blocking):`)
  for (const [file, why] of notes) console.error(`  ⚠ ${file}: ${why}`)
}
// B2 erosion report (advisory — never affects exit). Surface the corpus opt-out count, per-task
// heavy opt-outs, and a corpus-over-threshold erosion warning.
if (corpusNone > 0) {
  console.error(`verify-done: ${corpusNone} audited \`— none (<why>)\` opt-out(s) across done/ (design-comparison opt-outs; env FIGMA_NONE_PER_TASK=${nonePerTask}, FIGMA_NONE_CORPUS=${noneCorpus})`)
  for (const [file, n] of noneHeavy) console.error(`  ⚠ ${file}: ${n} audited \`— none\` opt-out(s) in one task (>= FIGMA_NONE_PER_TASK ${nonePerTask})`)
  if (corpusNone >= noneCorpus) console.error(`  ⚠ corpus audited-none count ${corpusNone} >= FIGMA_NONE_CORPUS ${noneCorpus} — the visual-comparison guarantee is eroding (screens opted out one \`— none\` at a time); review whether these opt-outs are still justified`)
}
// C1 staleness (advisory — printed AFTER the violation summary, never affects exit).
if (staleness.length) {
  console.error(`verify-done: ${staleness.length} shipped task(s) have a STALE certificate — the Figma design moved since ship (advisory, NOT a violation):`)
  for (const [file, why] of staleness) console.error(`  ⚠ ${file}: ${why}`)
}
process.exit(violations.length ? 2 : 0)
