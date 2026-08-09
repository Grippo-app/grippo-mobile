// check-direct-figma-access.mjs — guard the golden invariant:
// sidecar/site code must not call Figma REST or use token fallback directly.
//
// Scope: this is a DRIFT GUARD, not an anti-exfiltration control. It catches honest/verbatim
// references and whitespace/quote/+/array-split forms of api.figma.com and the removed REST
// token fallback; it does NOT defeat deliberate obfuscation (template interpolation, .concat,
// url-/base64-encoding). Runtime secret prevention is childEnv() in site/server/child-env.js,
// which strips Figma token env from every spawned child regardless of how a host is spelled.

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { delimiter } from 'node:path'
import { join, relative } from 'node:path'
import { PROJECT_ROOT, failMsg, ok, summary } from './_util.mjs'

const DEFAULT_ROOTS = [
  join(PROJECT_ROOT, 'orchestrator', 'figma', 'scripts'),
  join(PROJECT_ROOT, 'orchestrator', 'site', 'server.js'),
  join(PROJECT_ROOT, 'orchestrator', 'site', 'server'),
  join(PROJECT_ROOT, 'orchestrator', 'site', 'scripts'),
]
const ROOTS = process.env.FIGMA_SECURITY_GREP_ROOTS
  ? process.env.FIGMA_SECURITY_GREP_ROOTS.split(delimiter).filter(Boolean)
  : DEFAULT_ROOTS
const SKIP_DIRS = new Set(['node_modules', '.git', '.cache'])
const CODE_EXT = /\.(mjs|cjs|js)$/i
const TERMS = [
  { kind: 'figmaRestHost', re: /api\.figma\.com/i, compactRe: /api\.?figma\.?com|apifigma\.?com|apifigmacom/i },
  { kind: 'figmaAccessToken', re: /\bFIGMA_ACCESS_TOKEN\b/, compactRe: /FIGMA_ACCESS_TOKEN/ },
  { kind: 'figmaHeaderToken', re: /\bX-Figma-Token\b/i, compactRe: /X-?Figma-?Token|XFigmaToken/i },
  { kind: 'mcpGetDesignContext', re: /\bget_design_context\b/, compactRe: /get_design_context/ },
  { kind: 'mcpGetMetadata', re: /\bget_metadata\b/, compactRe: /get_metadata/ },
  { kind: 'mcpGetScreenshot', re: /\bget_screenshot\b/, compactRe: /get_screenshot/ },
  { kind: 'mcpGetVariableDefs', re: /\bget_variable_defs\b/, compactRe: /get_variable_defs/ },
]
const ALLOW = new Map([
  ['orchestrator/site/server/sessions.js', [{ kind: 'figmaHeaderToken', re: /replace\(.+X-Figma-Token.+redacted/i }]],
  ['orchestrator/site/server/figma.js', [{ kind: 'figmaHeaderToken', re: /\/\/.*X-Figma-Token.*401/i }]],
])

function files(root, out = []) {
  if (!existsSync(root)) return out
  let st
  try { st = statSync(root) } catch { return out }
  if (st.isFile()) {
    if (CODE_EXT.test(root)) out.push(root)
    return out
  }
  if (!st.isDirectory()) return out
  let entries = []
  try { entries = readdirSync(root, { withFileTypes: true }) } catch { return out }
  for (const entry of entries) {
    const p = join(root, entry.name)
    if (entry.isDirectory()) {
      if (!SKIP_DIRS.has(entry.name)) files(p, out)
    } else if (entry.isFile() && CODE_EXT.test(entry.name)) {
      out.push(p)
    }
  }
  return out
}

function compactLine(line) {
  return String(line || '').replace(/['"`\s+[\],]/g, '')
}

function matchedTerms(line) {
  const compact = compactLine(line)
  return TERMS.filter((term) => term.re.test(line) || (term.compactRe && term.compactRe.test(compact)))
}

function isMcpTerm(term) {
  return ['mcpGetDesignContext', 'mcpGetMetadata', 'mcpGetScreenshot', 'mcpGetVariableDefs'].includes(term.kind)
}

function allowed(rel, line, window, term) {
  if (isMcpTerm(term) && rel === 'orchestrator/site/scripts/figma-actions.js' && /Figma MCP|THIS session calls the MCP|session calls the MCP/i.test(window)) return true
  if (isMcpTerm(term) && /^orchestrator\/site\/scripts\/i18n\/(?:en|ru)\.js$/.test(rel)) return true
  return (ALLOW.get(rel) || []).some((rule) => rule.kind === term.kind && rule.re.test(line))
}

for (const file of ROOTS.flatMap((r) => files(r))) {
  const rel = relative(PROJECT_ROOT, file)
  if (rel === 'orchestrator/figma/scripts/check-direct-figma-access.mjs') continue
  const lines = readFileSync(file, 'utf8').split(/\r?\n/)
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const lineTerms = matchedTerms(line)
    for (const term of lineTerms) {
      if (!allowed(rel, line, line, term)) failMsg(`${rel}:${i + 1}: direct Figma token/API/MCP tool reference (${term.kind})`)
    }
    const window = lines.slice(i, i + 8).join(' ')
    for (const term of matchedTerms(window)) {
      if (lineTerms.some((t) => t.kind === term.kind)) continue
      const futureWholeLine = lines.slice(i + 1, i + 8).some((future) => matchedTerms(future).some((t) => t.kind === term.kind))
      if (futureWholeLine) continue
      if (!allowed(rel, line, window, term)) failMsg(`${rel}:${i + 1}: direct Figma token/API/MCP tool reference (${term.kind})`)
    }
  }
}

ok('no direct Figma REST/token/tool references in sidecar/site executable code')
process.exit(summary('figma:security:grep'))
