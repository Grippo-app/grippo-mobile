// Shared, side-effect-free validator for the frozen task Outcome appendix.
//
// This is intentionally consumed by both finalize-task.mjs and ship-done.mjs.
// Keeping the validator here prevents a recovery attempt from accepting an
// appendix that the sanctioned todo -> done interlock would reject later.

import { readFileSync } from 'node:fs'
import { dirname, join, posix } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createRequire } from 'node:module'

const HERE = dirname(fileURLToPath(import.meta.url))
const require = createRequire(import.meta.url)
const { parseAtxHeadingLine, structuralText } = require('./design-parser.cjs')
const DEFAULT_OUTCOME_SHAPE_PATH = join(HERE, '..', '..', 'contracts', 'outcome-shape.json')

// `figmaEnabled` is a transaction credential, not a permissive YAML-ish
// convenience value: finalize-task freezes it while ship/verify decide which
// gates apply.  Keep one side-effect-free parser shared by both paths so a
// malformed physical line cannot be interpreted as true by one process and
// false by another.  The surrounding project config may contain prose, but
// the field itself must be one unique, canonical, physical-line declaration.
export function parseFigmaEnabledConfig(text) {
  const lines = String(text == null ? '' : text).replace(/\r\n?/g, '\n').split('\n')
  const declarations = lines.filter((line) => /^[ \t]*figmaEnabled[ \t]*:/.test(line))
  const canonical = declarations.filter((line) => /^figmaEnabled:[ \t]+(true|false)[ \t]*$/.test(line))
  if (declarations.length !== 1 || canonical.length !== 1) {
    throw new Error('project-config.md must contain exactly one canonical physical-line `figmaEnabled: true|false` field')
  }
  return canonical[0].match(/^figmaEnabled:[ \t]+(true|false)[ \t]*$/)[1] === 'true'
}

function atxHeadingRecords(value, level) {
  const records = []
  let start = 0
  while (start <= value.length) {
    const newline = value.indexOf('\n', start)
    const end = newline < 0 ? value.length : newline
    const contentEnd = end > start && value[end - 1] === '\r' ? end - 1 : end
    const parsed = parseAtxHeadingLine(value.slice(start, contentEnd))
    if (parsed && (level == null || parsed.level === level)) {
      records.push({ ...parsed, start, headEnd: contentEnd })
    }
    if (newline < 0) break
    start = newline + 1
  }
  return records
}

function structuralOutcomeView(text) {
  const source = String(text == null ? '' : text)
  const canonicalText = !source.startsWith('\uFEFF') && !source.includes('\r')
  const structural = canonicalText ? structuralText(source) : ''
  const separators = []
  const separatorRe = /^---[ \t]*$/gm
  for (let match; (match = separatorRe.exec(structural)) != null;) separators.push(match.index)
  const lastSeparator = separators.length ? separators[separators.length - 1] : -1
  const h2 = atxHeadingRecords(structural, 2)
  const outcomes = h2.filter((heading) => heading.name === 'Outcome')
  const outcome = outcomes.length === 1 ? outcomes[0] : null
  const directlyAnchored = !!outcome && lastSeparator >= 0 && outcome.start > lastSeparator &&
    /^---[ \t]*\n(?:[ \t]*\n)*$/.test(structural.slice(lastSeparator, outcome.start))
  const unexpectedTrailingH2 = !!outcome && h2.some((heading) => heading.start > outcome.start)
  return {
    source,
    structural,
    lastSeparator,
    h2,
    outcomes,
    outcome,
    directlyAnchored,
    unexpectedTrailingH2,
    canonicalText,
    validAnchor: canonicalText && directlyAnchored && !unexpectedTrailingH2,
  }
}

function lineRecords(value) {
  const records = []
  let cursor = 0
  while (cursor < value.length) {
    const cr = value.indexOf('\r', cursor)
    const lf = value.indexOf('\n', cursor)
    const contentEnd = cr < 0 ? lf : lf < 0 ? cr : Math.min(cr, lf)
    const end = contentEnd < 0 ? value.length
      : contentEnd + (value[contentEnd] === '\r' && value[contentEnd + 1] === '\n' ? 2 : 1)
    const lineEnd = contentEnd < 0 ? end : contentEnd
    records.push({ text: value.slice(cursor, lineEnd), start: cursor, contentEnd: lineEnd, end, eol: value.slice(lineEnd, end) })
    cursor = end
  }
  return records
}

function isAtxHeading(line, level, name) {
  const parsed = parseAtxHeadingLine(line)
  return !!parsed && parsed.level === level && parsed.name === name
}

// Match one Outcome field per physical line. A multiline `^\s*...\s*$`
// expression can retry across every following blank line when a field is
// absent, which is quadratic on an otherwise bounded task document.
function outcomeFieldMatches(head, name) {
  const escapedName = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const lineRe = new RegExp(`^[ \\t]*\\*\\*${escapedName}\\*\\*[ \\t]*:[ \\t]*(.+)[ \\t]*\\r?$`)
  const matches = []
  for (const line of String(head).split('\n')) {
    const match = lineRe.exec(line)
    if (match) matches.push(match)
  }
  return matches
}

export function loadOutcomeShape(contractPath = DEFAULT_OUTCOME_SHAPE_PATH) {
  let shape
  try {
    shape = JSON.parse(readFileSync(contractPath, 'utf8'))
  } catch (e) {
    throw new Error('required outcome-shape contract is unreadable or malformed')
  }
  const arrays = [shape && shape.headings, shape && shape.statusValid, shape && shape.reviewerValid,
    shape && shape.acceptanceVerdicts, shape && shape.buildGateVerdicts,
    shape && shape.runtimeGates, shape && shape.runtimeResults,
    shape && shape.followUpColumns, shape && shape.fileChanges]
  if (!arrays.every((a) => Array.isArray(a) && a.length > 0 && a.every((v) => typeof v === 'string' && v))) {
    throw new Error('required outcome-shape contract has an invalid schema')
  }
  return shape
}

function strictSectionBullets(section) {
  const values = []
  for (const line of String(section || '').split(/\r?\n/)) {
    if (!line.trim()) continue
    const match = /^ {0,3}-[ \t]+(.+?)[ \t]*$/.exec(line)
    if (!match) return null
    values.push(match[1])
  }
  return values
}

function buildGateError(section, shape) {
  const bullets = strictSectionBullets(section)
  if (!bullets || !bullets.length) return 'build-gates must contain canonical bullets'
  if (bullets.length === 1 && /^none$/i.test(bullets[0])) return null
  if (bullets.some((bullet) => /^none$/i.test(bullet))) return 'build-gates cannot combine `none` with gate bullets'
  for (const bullet of bullets) {
    const match = /^`([^`\r\n]+)`[ \t]+—[ \t]+([a-z]+)(?:[ \t]+\(([^()\r\n]{1,200})\))?$/.exec(bullet)
    if (!match || !shape.buildGateVerdicts.includes(match[2]) || (match[2] === 'skipped') !== !!match[3]) {
      return `invalid build-gate bullet ${JSON.stringify(bullet.slice(0, 120))}`
    }
  }
  return null
}

function runtimeVerifyError(section, shape) {
  const bullets = strictSectionBullets(section)
  if (!bullets || bullets.length !== 2) return 'runtime-verify must contain exactly Gate then Result bullets'
  const gate = /^Gate:[ \t]+([a-z]+)(?:[ \t]+\(([^()\r\n]{1,200})\))?$/.exec(bullets[0])
  const result = /^Result:[ \t]+(pass|fail|n\/a)[ \t]+—[ \t]+([^\r\n]{1,500})$/.exec(bullets[1])
  if (!gate || !shape.runtimeGates.includes(gate[1]) || (gate[1] === 'ran') === !!gate[2]) {
    return `invalid runtime Gate bullet ${JSON.stringify(bullets[0].slice(0, 120))}`
  }
  if (!result || !shape.runtimeResults.includes(result[1])) {
    return `invalid runtime Result bullet ${JSON.stringify(bullets[1].slice(0, 120))}`
  }
  if ((gate[1] === 'ran') !== (result[1] === 'pass' || result[1] === 'fail')) {
    return 'runtime Gate and Result verdicts are inconsistent'
  }
  return null
}

function singletonNoneOrBullets(section, label) {
  const bullets = strictSectionBullets(section)
  if (!bullets || !bullets.length) return { error: `${label} must contain canonical bullets`, bullets: [] }
  if (bullets.length === 1 && /^none$/i.test(bullets[0])) return { error: null, bullets: [] }
  if (bullets.some((bullet) => /^none$/i.test(bullet))) return { error: `${label} cannot combine \`none\` with other bullets`, bullets: [] }
  return { error: null, bullets }
}

function safeTaskStem(value) {
  if (typeof value !== 'string' || value.length > 120) return false
  const match = /^TASK_([1-9][0-9]*)_[A-Za-z0-9_]+$/.exec(value)
  return !!match && Number.isSafeInteger(Number(match[1]))
}

function safeOutcomePath(value) {
  const normalized = String(value || '')
  const pieces = normalized.split('/')
  return !!normalized && normalized.length <= 300 && !normalized.includes('\\') && !normalized.includes('\0') &&
    !posix.isAbsolute(normalized) && !/^[A-Za-z][A-Za-z0-9+.-]*:/.test(normalized) &&
    normalized === posix.normalize(normalized) && !normalized.endsWith('/') &&
    !pieces.includes('') && !pieces.includes('.') && !pieces.includes('..')
}

function ancillarySectionsError(sections, shape) {
  const caveats = singletonNoneOrBullets(sections['Caveats'], 'caveats')
  if (caveats.error) return caveats.error
  if (caveats.bullets.some((bullet) => Array.from(bullet).length > 120 || /[\u0000-\u001f\u007f]/.test(bullet))) {
    return 'caveats contain an invalid or overlong bullet'
  }
  const followUps = singletonNoneOrBullets(sections['Follow-ups'], 'follow-ups')
  if (followUps.error) return followUps.error
  for (const bullet of followUps.bullets) {
    const match = /^`(TASK_[1-9][0-9]*_[A-Za-z0-9_]+)`[ \t]+—[ \t]+([a-z]+)$/.exec(bullet)
    if (!match || !safeTaskStem(match[1]) || !shape.followUpColumns.includes(match[2])) return `invalid follow-up bullet ${JSON.stringify(bullet.slice(0, 120))}`
  }
  const files = singletonNoneOrBullets(sections['Files touched'], 'files-touched')
  if (files.error) return files.error
  for (const bullet of files.bullets) {
    const match = /^`([^`\r\n]+)`[ \t]+—[ \t]+([a-z]+)$/.exec(bullet)
    if (!match || !safeOutcomePath(match[1]) || !shape.fileChanges.includes(match[2])) {
      return `invalid files-touched bullet ${JSON.stringify(bullet.slice(0, 120))}`
    }
  }
  if (Object.prototype.hasOwnProperty.call(sections, 'Execution log')) {
    const execution = strictSectionBullets(sections['Execution log'])
    if (!execution || !execution.length || execution.length > 6) return 'execution-log must contain 1..6 canonical bullets'
  }
  return null
}

// Returns the first contract error, or null when the appendix is valid.
export function outcomeShapeError(text, options = {}) {
  const shape = options.shape || loadOutcomeShape(options.contractPath)
  const view = structuralOutcomeView(text)
  if (!view.canonicalText) return 'task text must use canonical UTF-8 without BOM and LF line endings'
  if (view.lastSeparator === -1) return 'no `---` separator line (the appendix is anchored on the last `---` in the file)'
  if (view.outcomes.length !== 1) return `expected exactly one structural \`## Outcome\` heading in the task; found ${view.outcomes.length}`
  if (!view.directlyAnchored) return '`## Outcome` must be anchored directly after the final structural `---` separator with only blank lines between them'
  if (view.unexpectedTrailingH2) return 'unexpected structural H2 after the final `## Outcome` appendix'

  const body = view.structural.slice(view.outcome.headEnd)

  const subRe = /^###[ \t]+(\S.*?)[ \t]*\r?$/gm
  const subs = []
  for (let m; (m = subRe.exec(body)) != null;) {
    subs.push({ name: m[1].trim(), start: m.index, headEnd: subRe.lastIndex })
  }
  const sections = Object.create(null)
  const sectionCounts = Object.create(null)
  for (let i = 0; i < subs.length; i++) {
    sectionCounts[subs[i].name] = Number(sectionCounts[subs[i].name] || 0) + 1
    sections[subs[i].name] = body.slice(subs[i].headEnd, i + 1 < subs.length ? subs[i + 1].start : body.length)
  }
  const expectedSections = shape.headings.concat(subs.length === shape.headings.length + 1 ? ['Execution log'] : [])
  if (subs.length !== expectedSections.length || subs.some((sub, index) => sub.name !== expectedSections[index])) {
    return 'Outcome subsections must match the exact current order, with only an optional final `Execution log`'
  }
  for (const heading of shape.headings) {
    if (Number(sectionCounts[heading] || 0) !== 1) return `expected exactly one \`### ${heading}\` heading; found ${Number(sectionCounts[heading] || 0)}`
    if (!sections[heading].split(/\r?\n/).some((line) => line.trim())) return `empty \`### ${heading}\` section (write \`- none\`)`
  }

  const head = subs.length ? body.slice(0, subs[0].start) : body
  const statusMatches = outcomeFieldMatches(head, 'Status')
  if (statusMatches.length !== 1) return `expected exactly one \`**Status**:\` field; found ${statusMatches.length}`
  const statusMatch = statusMatches[0]
  const status = statusMatch[1].trim().toLowerCase()
  if (!shape.statusValid.includes(status)) {
    return `invalid \`**Status**\` value ${JSON.stringify(statusMatch[1].trim())} (allowed: ${shape.statusValid.join(' | ')})`
  }
  const reviewerMatches = outcomeFieldMatches(head, 'Reviewer')
  if (reviewerMatches.length !== 1) return `expected exactly one \`**Reviewer**:\` field; found ${reviewerMatches.length}`
  const reviewerMatch = reviewerMatches[0]
  const reviewer = reviewerMatch[1].trim().toLowerCase()
  if (!shape.reviewerValid.includes(reviewer)) {
    return `invalid \`**Reviewer**\` value ${JSON.stringify(reviewerMatch[1].trim())} (allowed: ${shape.reviewerValid.join(' | ')})`
  }

  const buildError = buildGateError(sections['Build gates'], shape)
  if (buildError) return buildError
  const runtimeError = runtimeVerifyError(sections['Runtime verify'], shape)
  if (runtimeError) return runtimeError

  const acceptanceVerdicts = shape.acceptanceVerdicts.map((v) => v.toLowerCase())
  const traceSection = sections['Acceptance trace'] || ''
  let traceBullets = 0
  let noneBullets = 0
  for (const line of traceSection.split(/\r?\n/)) {
    if (!line.trim()) continue
    const bulletMatch = /^ {0,3}-[ \t]+(.+?)[ \t]*$/.exec(line)
    if (!bulletMatch) return 'acceptance-trace contains non-bullet structural content'
    traceBullets++
    const bullet = bulletMatch[1]
    if (/^none$/i.test(bullet.trim())) { noneBullets++; continue }
    const leadingCode = /^[ \t]*`[^`]*`/.exec(bullet)
    const separatorIndex = bullet.indexOf(' — ', leadingCode ? leadingCode[0].length : 0)
    if (separatorIndex < 0) {
      return `acceptance-trace bullet has no \` — \` verdict separator: ${JSON.stringify(bullet.slice(0, 80))} (shape: \`<verbatim bullet>\` — ${acceptanceVerdicts.join(' | ')} — <note>)`
    }
    const rest = bullet.slice(separatorIndex + 3)
    const nextSeparator = rest.indexOf(' — ')
    const verdict = (nextSeparator < 0 ? rest : rest.slice(0, nextSeparator)).trim().toLowerCase()
    if (!acceptanceVerdicts.includes(verdict)) {
      return `invalid acceptance-trace verdict ${JSON.stringify(verdict)} in ${JSON.stringify(bullet.slice(0, 80))} (allowed: ${acceptanceVerdicts.join(' | ')}; quote the acceptance bullet in backticks so an em-dash inside it cannot shift the verdict segment)`
    }
  }
  if (!traceBullets) return 'acceptance-trace must contain at least one structural bullet'
  if (traceBullets > 1 && noneBullets) return 'acceptance-trace cannot combine `none` with other bullets'
  const ancillaryError = ancillarySectionsError(sections, shape)
  if (ancillaryError) return ancillaryError
  return null
}

export function outcomeAppendixStart(text) {
  const view = structuralOutcomeView(text)
  return view.validAnchor ? view.lastSeparator : -1
}

// Read the authoritative Outcome Status through the same structural/ATX view
// as task-state admission. This deliberately returns an empty string unless
// there is exactly one real Outcome H2 anchored directly after the final
// separator; fenced/raw-HTML decoys and indented-code pseudo-headings cannot
// become a ship credential.
export function outcomeAppendixStatus(text) {
  const view = structuralOutcomeView(text)
  if (!view.validAnchor) return ''
  const body = view.structural.slice(view.outcome.headEnd)
  const firstSub = atxHeadingRecords(body, 3)[0]
  const head = firstSub ? body.slice(0, firstSub.start) : body
  const matches = outcomeFieldMatches(head, 'Status')
  return matches.length === 1 ? matches[0][1].trim() : ''
}

const FIGMA_META_RE = /^[ \t]*-[ \t]*Figma meta:/

// Parse only the final, contract-authoritative Outcome appendix. A similarly
// named heading or bullet in the task body is ordinary user content and must
// never be used as a ship credential or removed from the logical task hash.
export function inspectOutcomeFigmaMeta(text) {
  const view = structuralOutcomeView(text)
  const value = view.source
  const records = lineRecords(value)
  const lines = records.map((record) => record.text)
  const structuralRecords = lineRecords(view.structural)
  const structuralLines = structuralRecords.map((record) => record.text)
  const separator = structuralRecords.findIndex((record) => record.start === view.lastSeparator)
  const outcome = view.outcome
    ? structuralRecords.findIndex((record) => record.start === view.outcome.start)
    : -1
  if (!view.validAnchor || separator < 0 || outcome < 0 || structuralLines.length !== lines.length) {
    return { value, records, lines, hasOutcome: false, separator, outcome: -1, executionHeaders: [], executionSections: [], executionLines: [], misplacedLines: [] }
  }

  const executionHeaders = []
  for (let i = outcome + 1; i < structuralLines.length; i++) {
    if (/^###[ \t]+Execution log[ \t]*$/.test(structuralLines[i])) executionHeaders.push(i)
  }
  const executionIndexes = new Set()
  const executionSections = []
  for (const header of executionHeaders) {
    let end = lines.length
    for (let i = header + 1; i < lines.length; i++) {
      if (/^#{1,3}[ \t]+/.test(structuralLines[i])) { end = i; break }
    }
    executionSections.push({ header, start: header + 1, end })
    for (let i = header + 1; i < end; i++) if (FIGMA_META_RE.test(structuralLines[i])) executionIndexes.add(i)
  }

  const executionLines = [...executionIndexes].sort((a, b) => a - b).map((index) => ({
    index, line: lines[index], start: records[index].start, end: records[index].end,
  }))
  const misplacedLines = []
  for (let i = separator; i < lines.length; i++) {
    if (FIGMA_META_RE.test(structuralLines[i]) && !executionIndexes.has(i)) misplacedLines.push({
      index: i, line: lines[i], start: records[i].start, end: records[i].end,
    })
  }
  return { value, records, lines, hasOutcome: true, separator, outcome, executionHeaders, executionSections, executionLines, misplacedLines }
}

function removeRanges(value, ranges) {
  let out = '', at = 0
  for (const range of [...ranges].sort((a, b) => a.start - b.start)) {
    out += value.slice(at, range.start)
    at = range.end
  }
  return out + value.slice(at)
}

export function logicalTaskText(text) {
  const inspected = inspectOutcomeFigmaMeta(text)
  return removeRanges(inspected.value, inspected.executionLines)
}

export function outcomeSectionLines(text, heading) {
  const inspected = inspectOutcomeFigmaMeta(text)
  if (!inspected.hasOutcome) return null
  const structuralLines = structuralText(inspected.value).split('\n').slice(0, inspected.lines.length)
  const headers = []
  for (let i = inspected.outcome + 1; i < inspected.lines.length; i++) {
    const match = /^###[ \t]+(\S.*?)[ \t]*$/.exec(structuralLines[i])
    if (match && match[1].trim() === heading) headers.push(i)
  }
  if (headers.length !== 1) return null
  let end = inspected.lines.length
  for (let i = headers[0] + 1; i < inspected.lines.length; i++) {
    if (/^#{1,3}[ \t]+/.test(structuralLines[i])) { end = i; break }
  }
  return inspected.lines.slice(headers[0] + 1, end)
}

export function injectOutcomeFigmaMeta(text, digestLine) {
  if (!FIGMA_META_RE.test(String(digestLine || ''))) throw new Error('Figma digest must be a `- Figma meta:` bullet')
  const source = String(text == null ? '' : text)
  if (source.startsWith('\uFEFF') || source.includes('\r')) {
    throw new Error('task text must use canonical UTF-8 without BOM and LF line endings')
  }
  const inspected = inspectOutcomeFigmaMeta(source)
  if (!inspected.hasOutcome) throw new Error('cannot inject Figma metadata without the final Outcome appendix')
  if (inspected.executionHeaders.length > 1) throw new Error('Outcome appendix contains more than one `### Execution log` section')
  if (inspected.misplacedLines.length) throw new Error('Outcome appendix contains `- Figma meta:` outside `### Execution log`')

  const withoutDigest = removeRanges(inspected.value, inspected.executionLines)
  const clean = inspectOutcomeFigmaMeta(withoutDigest)
  const preferredEol = clean.records.find((record) => record.eol)?.eol || '\n'
  const digest = String(digestLine).trim()
  if (!clean.executionHeaders.length) {
    throw new Error('final Outcome must contain `### Execution log` before Figma certification; refusing to add structural task content during ship')
  }

  const section = clean.executionSections[0]
  const sectionRecords = clean.records.slice(section.start, section.end)
  let anchor = null
  for (const record of sectionRecords) if (record.text.trim()) anchor = record
  if (!anchor && sectionRecords.length) anchor = sectionRecords[0]
  if (!anchor) anchor = clean.records[section.header]
  if (!anchor.eol) {
    if (anchor === clean.records[section.header]) {
      throw new Error('final Outcome `### Execution log` heading must end with a newline before Figma certification')
    }
    // Put the digest before an unterminated final content line. Appending a
    // separator newline would mutate that user-authored line's byte identity.
    return withoutDigest.slice(0, anchor.start) + `${digest}${preferredEol}` + withoutDigest.slice(anchor.start)
  }
  const insertion = `${digest}${preferredEol}`
  return withoutDigest.slice(0, anchor.end) + insertion + withoutDigest.slice(anchor.end)
}

// Accept either a complete `---` + `## Outcome` trailer or a draft beginning
// at `## Outcome`. If the task already has an Outcome trailer, replace it.
export function installOutcomeDraft(taskText, outcomeDraft) {
  let draft = String(outcomeDraft == null ? '' : outcomeDraft)
  if (draft.startsWith('\uFEFF') || draft.includes('\r')) {
    throw new Error('outcome draft must use canonical UTF-8 without BOM and LF line endings')
  }
  draft = draft.trim()
  const firstStructuralLine = structuralText(draft).split('\n').find((line) => line.trim()) || ''
  if (/^---[ \t]*$/.test(firstStructuralLine)) {
    // Complete draft; structuralOutcomeView below proves the direct anchor.
  } else if (isAtxHeading(firstStructuralLine, 2, 'Outcome')) {
    draft = `---\n\n${draft}`
  } else {
    throw new Error('outcome draft must begin with `## Outcome`, or with a single `---` separator immediately followed by `## Outcome`')
  }
  const draftView = structuralOutcomeView(draft)
  if (!draftView.validAnchor) {
    throw new Error('outcome draft must contain exactly one directly anchored structural Outcome appendix')
  }
  const base = String(taskText == null ? '' : taskText)
  if (base.startsWith('\uFEFF') || base.includes('\r')) {
    throw new Error('task text must use canonical UTF-8 without BOM and LF line endings')
  }
  const existingStart = outcomeAppendixStart(base)
  const before = (existingStart >= 0 ? base.slice(0, existingStart) : base).trimEnd()
  return `${before}\n\n${draft}\n`
}
