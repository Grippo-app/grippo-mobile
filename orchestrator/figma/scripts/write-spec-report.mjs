// write-spec-report.mjs — CLI authoring for the agent spec report (W4-1).
//
// The final evidence bundle hard-requires `spec-<stem>.json` in gate mode with non-empty
// inputHashes under the shared pipeline run id — yet before this CLI NO script wrote
// `name:'spec'`: the figma-spec-validator hand-authored the full envelope (sha256 hashes,
// run id, UPPERCASE severity enum, count math), and every clerical slip was a fail-closed
// BLOCKER at final (REPORT_INPUT_HASHES_EMPTY / REPORT_BODY_SCHEMA_INVALID /
// REPORT_*_COUNT_MISMATCH / REPORT_STALE_RUN). Worse, any machine re-run of
// compare-screen-spec in a fix cycle changed `spec-compare-<stem>.json` bytes and silently
// invalidated the hand-computed hash. This CLI owns all of that clerical surface:
//
//   node scripts/write-spec-report.mjs <stem> \
//     --screen "Name=PASS|FAIL[:note]" [--screen ...] \
//     --issue  "SEVERITY:rule_id:screen:message" [--issue ...] \
//     [--verdict-file verdicts.json]        # structured alternative to --screen/--issue
//
// - severities are the gate's agent taxonomy: BLOCKER | MAJOR | MINOR (case-insensitive
//   input, stored UPPERCASE — the envelope enum). rule_id (spec.inventory / spec.value /
//   spec.token / spec.literal / ...) is stored as the issue's `issueKind`.
// - `overall` is COMPUTED, never passed: BLOCKER when any routed (BLOCKER/MAJOR) finding
//   exists, WARN when only Minors, PASS when clean — the spec-fidelity-gate §3 verdict rule.
// - verdict↔issue consistency is enforced both ways: a FAIL screen must carry a routed
//   finding, a routed finding forces its screen to FAIL, and an issue naming an undeclared
//   screen is a typo caught here, not at the final bundle. Component-internal escalations
//   are NOT issues[] rows (routing guard: they go to chat as an escalation bullet).
// - inputHashes pin `spec-compare-<stem>.json` (this report's machine baseline — REQUIRED,
//   the CLI refuses to write without it) + the implementation model the baseline recorded,
//   when present. writeReport supplies pipelineRunId (file-pinned), counts, gatePolicyVersion.
// - the assembled envelope is ajv-validated against spec-report.schema.json BEFORE the
//   write (buildReport/persistReport split) — a schema-invalid report never lands on disk.
//
// Exit codes: 0 = report written (even when overall=BLOCKER — authoring findings is the
// CLI succeeding; the BUNDLE gates), 1 = invalid input / missing baseline / schema failure.
import { existsSync, readFileSync } from 'node:fs'
import { isAbsolute, join, resolve } from 'node:path'
import { FIGMA_CACHE_ROOT, PROJECT_ROOT, cacheRelative, displayPath, figmaPath, parseCli, projectRelative } from './_util.mjs'
import { assertTaskStem, buildReport, compileSchema, fileHash, persistReport } from './report-utils.mjs'

const USAGE = `usage: write-spec-report.mjs <stem>
  --screen "Name=PASS|FAIL[:note]"          per compared screen (repeatable)
  --issue  "SEVERITY:rule_id:screen:message" per routed/advisory finding (repeatable;
           SEVERITY = BLOCKER|MAJOR|MINOR; message may contain ':')
  --verdict-file <path>                      JSON {screens:[{screen,verdict,note?,minors?}],
           issues:[{severity,ruleId,screen,message,file?}]} — alternative to --screen/--issue`

const AGENT_SEVERITIES = new Set(['BLOCKER', 'MAJOR', 'MINOR'])
const ROUTED = new Set(['BLOCKER', 'MAJOR'])
const RULE_ID_RE = /^[A-Za-z][A-Za-z0-9_.-]*$/

function die(msg) {
  console.error(`write-spec-report: ${msg}`)
  process.exit(1)
}

// inputHashes key: repo-relative or cache-relative when containable (the forms
// resolveInputPath on the bundle side and trustedInputFile on the site side both resolve),
// absolute otherwise (fixtures in tmpdirs). Never reduce it to a bare basename —
// a basename key would not resolve to a file at recheck time.
function hashKey(abs) {
  return cacheRelative(abs) || projectRelative(abs) || abs
}

function resolveRecordedPath(p) {
  const s = String(p || '')
  if (!s) return ''
  if (isAbsolute(s)) return s
  const candidates = [resolve(PROJECT_ROOT, s), resolve(FIGMA_CACHE_ROOT, s)]
  return candidates.find((c) => existsSync(c)) || candidates[0]
}

function parseScreenArg(raw) {
  const eq = String(raw).indexOf('=')
  if (eq <= 0) die(`--screen must be "Name=PASS|FAIL[:note]", got ${JSON.stringify(raw)}\n${USAGE}`)
  const screen = String(raw).slice(0, eq).trim()
  let rest = String(raw).slice(eq + 1)
  const colon = rest.indexOf(':')
  const note = colon >= 0 ? rest.slice(colon + 1).trim() : ''
  const verdict = (colon >= 0 ? rest.slice(0, colon) : rest).trim().toUpperCase()
  if (!screen) die(`--screen has an empty screen name: ${JSON.stringify(raw)}`)
  if (verdict !== 'PASS' && verdict !== 'FAIL') die(`--screen verdict must be PASS or FAIL, got ${JSON.stringify(verdict)} in ${JSON.stringify(raw)}`)
  const entry = { screen, verdict }
  if (note) entry.note = note
  return entry
}

function parseIssueArg(raw) {
  const s = String(raw)
  const p1 = s.indexOf(':')
  const p2 = p1 >= 0 ? s.indexOf(':', p1 + 1) : -1
  const p3 = p2 >= 0 ? s.indexOf(':', p2 + 1) : -1
  if (p3 < 0) die(`--issue must be "SEVERITY:rule_id:screen:message" (4 colon-separated fields; message may contain ':'), got ${JSON.stringify(raw)}\n${USAGE}`)
  return normalizeIssue({
    severity: s.slice(0, p1),
    ruleId: s.slice(p1 + 1, p2),
    screen: s.slice(p2 + 1, p3),
    message: s.slice(p3 + 1),
  }, `--issue ${JSON.stringify(raw)}`)
}

function normalizeIssue(rawIssue, label) {
  const severity = String(rawIssue.severity || '').trim().toUpperCase()
  const ruleId = String(rawIssue.ruleId || rawIssue.issueKind || '').trim()
  const screen = String(rawIssue.screen || '').trim()
  const message = String(rawIssue.message || '').trim()
  if (!AGENT_SEVERITIES.has(severity)) die(`${label}: severity must be one of BLOCKER|MAJOR|MINOR (the spec-fidelity-gate agent taxonomy), got ${JSON.stringify(rawIssue.severity)}`)
  if (!RULE_ID_RE.test(ruleId)) die(`${label}: rule_id must match ${RULE_ID_RE} (e.g. spec.value), got ${JSON.stringify(ruleId)}`)
  if (!screen) die(`${label}: screen must be non-empty (component-internal escalations are NOT report issues — see the routing guard)`)
  if (!message) die(`${label}: message must be non-empty`)
  const issue = { severity, issueKind: ruleId, screen, message }
  if (rawIssue.file) issue.file = String(rawIssue.file)
  return issue
}

function loadVerdictFile(path) {
  let data
  try { data = JSON.parse(readFileSync(path, 'utf8')) } catch (e) {
    die(`--verdict-file ${path} is unreadable or not JSON: ${e.message}`)
  }
  if (!data || typeof data !== 'object' || Array.isArray(data)) die(`--verdict-file must be a JSON object {screens, issues}`)
  // Unknown top-level keys fail closed: a typo'd "Issues"/"findings" key would otherwise
  // silently yield issues=[] and author PASS where the dropped rows demanded WARN/BLOCKER —
  // bypassing the very consistency net this CLI exists for.
  for (const k of Object.keys(data)) {
    if (k !== 'screens' && k !== 'issues') die(`--verdict-file has unknown top-level key ${JSON.stringify(k)} (expected exactly {screens, issues}) — a typo here would silently drop findings`)
  }
  if (!Array.isArray(data.screens)) die(`--verdict-file must carry a screens[] array`)
  if (data.issues !== undefined && !Array.isArray(data.issues)) die(`--verdict-file issues must be an array when present`)
  const screens = data.screens.map((row, i) => {
    if (!row || typeof row !== 'object') die(`--verdict-file screens[${i}] must be an object`)
    const screen = String(row.screen || '').trim()
    const verdict = String(row.verdict || '').trim().toUpperCase()
    if (!screen) die(`--verdict-file screens[${i}].screen must be non-empty`)
    if (verdict !== 'PASS' && verdict !== 'FAIL') die(`--verdict-file screens[${i}].verdict must be PASS or FAIL, got ${JSON.stringify(row.verdict)}`)
    const entry = { screen, verdict }
    if (row.note) entry.note = String(row.note)
    if (row.minors !== undefined) {
      if (!Number.isInteger(row.minors) || row.minors < 0) die(`--verdict-file screens[${i}].minors must be a non-negative integer`)
      entry.minors = row.minors
    }
    return entry
  })
  const issues = (data.issues || []).map((row, i) => normalizeIssue(row || {}, `--verdict-file issues[${i}]`))
  return { screens, issues }
}

async function main() {
  let cli
  try {
    cli = parseCli({
      allowedFlags: ['--screen', '--issue', '--verdict-file'],
      valueFlags: ['--screen', '--issue', '--verdict-file'],
      usage: USAGE,
    })
  } catch (e) { die(e.message) }
  let stem
  try { stem = assertTaskStem(cli.positional[0]) }
  catch { die(`exactly one canonical <stem> positional is required\n${USAGE}`) }
  if (cli.positional.length !== 1) die(`exactly one canonical <stem> positional is required\n${USAGE}`)

  const reportsDir = process.env.FIGMA_REPORTS_DIR || figmaPath('reports')
  const specComparePath = join(reportsDir, `spec-compare-${stem}.json`)
  if (!existsSync(specComparePath)) {
    die(`machine baseline missing: ${displayPath(specComparePath)} — run compare-screen-spec.mjs <stem> --gate first (spec-fidelity-gate Step 0); the agent report certifies ON TOP of that baseline, never instead of it`)
  }

  // --verdict-file is singular by contract; parseCli accumulates repeats silently, and
  // first-wins would drop every screen/finding in the later file(s) — the same ambiguity
  // class as mixing the two input forms, so it dies the same way.
  if (cli.valuesFor('--verdict-file').length > 1) {
    die(`--verdict-file was passed ${cli.valuesFor('--verdict-file').length} times — it is a single structured input; merge the files (only --screen/--issue are repeatable)`)
  }
  const verdictFile = cli.value('--verdict-file')
  const flagScreens = cli.valuesFor('--screen')
  const flagIssues = cli.valuesFor('--issue')
  if (verdictFile && (flagScreens.length || flagIssues.length)) {
    die(`--verdict-file and --screen/--issue are alternative input forms — pass one or the other, not both`)
  }
  const { screens, issues } = verdictFile
    ? loadVerdictFile(verdictFile)
    : { screens: flagScreens.map(parseScreenArg), issues: flagIssues.map(parseIssueArg) }

  if (!screens.length) die(`at least one --screen (or a verdict-file screens[] entry) is required — an empty screens list certifies nothing and the final bundle rejects it\n${USAGE}`)
  const byName = new Map()
  for (const s of screens) {
    if (byName.has(s.screen)) die(`duplicate screen verdict for ${JSON.stringify(s.screen)}`)
    byName.set(s.screen, s)
  }
  // Verdict↔issue consistency — the exact clerical slips that used to surface as
  // fail-closed blockers at the final bundle (or worse, as a PASS that certified nothing).
  for (const i of issues) {
    if (!byName.has(i.screen)) die(`issue references undeclared screen ${JSON.stringify(i.screen)} — every issue's screen must have a --screen verdict (typo, or a missing screen entry)`)
    if (ROUTED.has(i.severity) && byName.get(i.screen).verdict !== 'FAIL') {
      die(`screen ${JSON.stringify(i.screen)} carries a routed ${i.severity} finding but its verdict is PASS — a routed finding forces FAIL (spec-fidelity-gate §3)`)
    }
  }
  for (const s of screens) {
    if (s.verdict === 'FAIL' && !issues.some((i) => i.screen === s.screen && ROUTED.has(i.severity))) {
      die(`screen ${JSON.stringify(s.screen)} is marked FAIL but carries no BLOCKER/MAJOR issue — record the finding (--issue), or the verdict is PASS (Minors alone do not FAIL a screen)`)
    }
    // A `minors` count that is not backed by MINOR issue rows would mint PASS where the
    // §3 contract demands WARN + a ### Caveats bullet per Minor — the count and the rows
    // must agree exactly (record each Minor as an issues[] row; the count is derived, not
    // a substitute).
    if (s.minors !== undefined) {
      const minorRows = issues.filter((i) => i.screen === s.screen && i.severity === 'MINOR').length
      if (s.minors !== minorRows) {
        die(`screen ${JSON.stringify(s.screen)} declares minors=${s.minors} but carries ${minorRows} MINOR issue row(s) — record each Minor finding as an issues[] row (the count must match; Minors are the ### Caveats channel, not an annotation)`)
      }
    }
  }

  // spec-fidelity-gate §4 verdict rule: BLOCKER on any routed finding, WARN on Minors only.
  const overall = issues.some((i) => ROUTED.has(i.severity)) ? 'BLOCKER' : issues.length ? 'WARN' : 'PASS'

  const inputs = { specCompareReport: hashKey(specComparePath) }
  const inputHashes = { [hashKey(specComparePath)]: fileHash(specComparePath) }
  // Pin only the implementation model path the machine baseline explicitly recorded. It
  // remains optional for direct report-authoring diagnostics; no conventional-path inference.
  let implModelPath = ''
  let specCompare
  try {
    specCompare = JSON.parse(readFileSync(specComparePath, 'utf8'))
    implModelPath = resolveRecordedPath(specCompare && specCompare.inputs && specCompare.inputs.implementationModel)
  } catch (error) {
    die(`machine baseline is unreadable or not valid JSON: ${error.message}`)
  }
  if (specCompare && specCompare.inputs && specCompare.inputs.implementationModel && !implModelPath) {
    die('machine baseline carries an invalid implementationModel path')
  }
  if (implModelPath && !existsSync(implModelPath)) {
    die('machine baseline implementationModel is missing')
  }
  if (implModelPath) {
    inputs.implementationModel = hashKey(implModelPath)
    inputHashes[hashKey(implModelPath)] = fileHash(implModelPath)
  }

  let built
  try {
    built = buildReport({
      name: 'spec',
      taskStem: stem,
      mode: 'gate',
      inputs,
      inputHashes,
      overall,
      issues,
      extra: { screens, authoredBy: 'write-spec-report-cli' },
    })
  } catch (e) { die(e.message) }

  // Validate the EXACT object that will land on disk before writing it (ajv is a committed
  // dependency of this sidecar; a missing ajv here is an install defect, so fail loud).
  const validate = await compileSchema(figmaPath('token-schemas', 'spec-report.schema.json'), { gate: true })
  if (!validate(built.report)) {
    const details = (validate.errors || []).map((err) => `${err.instancePath || '/'}: ${err.message}`).join('; ')
    die(`assembled report violates spec-report.schema.json (${details}) — nothing was written`)
  }
  persistReport(built)

  console.log(`write-spec-report: ${stem} ${overall} (${screens.length} screen(s), ${issues.length} issue(s))`)
  for (const i of issues) console.log(`  [${i.severity}] ${i.issueKind} ${i.screen}: ${i.message}`)
  console.log(`Report: ${built.reportPath}`)
}

main().catch((e) => {
  console.error(`FATAL: ${e.message}`)
  process.exit(1)
})
