import { createHash } from 'node:crypto'
import { readFileSync, writeFileSync, mkdirSync, renameSync, readdirSync } from 'node:fs'
import { basename, dirname, join } from 'node:path'
import { displayPath, figmaPath, loadScreenshotThresholds, pipelineRunId } from './_util.mjs'

export function assertTaskStem(raw, label = 'taskStem') {
  const match = typeof raw === 'string' && raw.length <= 120
    ? /^TASK_([1-9][0-9]*)_[A-Za-z0-9_]+$/.exec(raw)
    : null
  let canonical = false
  if (match) {
    try {
      const number = BigInt(match[1])
      canonical = number > 0n && number <= BigInt(Number.MAX_SAFE_INTEGER) && String(number) === match[1]
    } catch {}
  }
  if (!canonical) {
    throw new Error(`${label} must be a canonical safe-integer task stem; got ${JSON.stringify(raw)}`)
  }
  return raw
}

export const sha256Text = (text) => 'sha256:' + createHash('sha256').update(String(text ?? '')).digest('hex')
export const sha256Bytes = (bytes) => 'sha256:' + createHash('sha256').update(bytes).digest('hex')

export function fileHash(path) {
  try { return sha256Bytes(readFileSync(path)) } catch { return null }
}

export function issueSeverity(issue) {
  return String((issue && issue.severity) || '').toUpperCase()
}

function summarizeIssues(issues) {
  const rows = Array.isArray(issues) ? issues : []
  return {
    blockingCount: rows.filter((i) => ['BLOCKER', 'ERROR', 'FAIL'].includes(issueSeverity(i))).length,
    warningCount: rows.filter((i) => ['WARN', 'WARNING', 'MINOR', 'MAJOR', 'REVIEW_REQUIRED'].includes(issueSeverity(i))).length,
  }
}

// Assemble the report envelope WITHOUT writing it. Split out of writeReport so a caller
// (write-spec-report.mjs) can schema-validate the exact object before it persists —
// validating a copy assembled by hand would drift from what actually lands on disk.
export function buildReport({ name, taskStem, mode, inputs = {}, inputHashes = {}, overall, issues = [], extra = {}, outPath = '' }) {
  const safeTaskStem = assertTaskStem(taskStem)
  const dir = process.env.FIGMA_REPORTS_DIR || figmaPath('reports')
  const reportPath = outPath || join(dir, `${name}-${safeTaskStem}.json`)
  const reportRelPath = displayPath(reportPath)
  const counts = summarizeIssues(issues)
  const reserved = new Set([
    'schemaVersion',
    'gatePolicyVersion',
    'taskStem',
    'pipelineRunId',
    'mode',
    'inputs',
    'inputHashes',
    'overall',
    'blockingCount',
    'warningCount',
    'issues',
    'reportPath',
    'reportRelPath',
    'generatedAt',
  ])
  for (const key of Object.keys(extra || {})) {
    if (reserved.has(key)) throw new Error(`writeReport extra must not override reserved envelope field: ${key}`)
  }
  const report = Object.assign({
    schemaVersion: 1,
    // The gate-policy version from the committed screenshot-thresholds.json (W4-3):
    // schemaVersion says what SHAPE this report has; gatePolicyVersion says which
    // STRICTNESS regime certified it. Bumped there on any strictness change, so a done/
    // task's immutable receipts record the policy that certified them; they
    // never redefine current threshold enforcement.
    gatePolicyVersion: loadScreenshotThresholds().version,
    taskStem: safeTaskStem,
    pipelineRunId: pipelineRunId(safeTaskStem),
    mode,
    inputs,
    inputHashes,
    overall,
    blockingCount: counts.blockingCount,
    warningCount: counts.warningCount,
    issues: issues.slice().sort((a, b) => JSON.stringify(a).localeCompare(JSON.stringify(b))),
    reportPath: reportRelPath,
    reportRelPath,
    generatedAt: new Date().toISOString(),
  }, extra)
  return { report, reportPath }
}

export function persistReport({ report, reportPath }) {
  mkdirSync(dirname(reportPath), { recursive: true })
  const tmp = reportPath + '.tmp'
  writeFileSync(tmp, JSON.stringify(report, null, 2) + '\n')
  renameSync(tmp, reportPath)
  return { report, reportPath }
}

export function writeReport(args) {
  return persistReport(buildReport(args))
}

export async function compileSchema(schemaPath, { gate = false } = {}) {
  try {
    const { default: Ajv } = await import('ajv')
    const ajv = new Ajv({ allErrors: true, allowUnionTypes: true })
    ajv.addFormat('date-time', {
      type: 'string',
      validate: (value) => {
        if (typeof value !== 'string' ||
            !/^\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:\.\d{3})?Z$/.test(value)) return false
        const parsed = new Date(value)
        if (!Number.isFinite(parsed.getTime())) return false
        return parsed.toISOString() === (value.includes('.') ? value : value.replace('Z', '.000Z'))
      }
    })
    const schemaDir = dirname(schemaPath)
    const files = readdirSync(schemaDir).filter((f) => f.endsWith('.schema.json')).sort()
    for (const file of files) {
      ajv.addSchema(JSON.parse(readFileSync(join(schemaDir, file), 'utf8')), file)
    }
    const validate = ajv.getSchema(basename(schemaPath))
    if (!validate) throw new Error(`schema not loaded: ${basename(schemaPath)}`)
    return validate
  } catch (e) {
    if (gate) throw e
    return null
  }
}

export function schemaIssues(validate, data, prefix = '') {
  if (!validate) return []
  if (validate(data)) return []
  return (validate.errors || []).map((err) => ({
    severity: 'ERROR',
    issueKind: 'SCHEMA_INVALID',
    path: `${prefix}${err.instancePath || '/'}`,
    message: err.message || String(err),
  }))
}
