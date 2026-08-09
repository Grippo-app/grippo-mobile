import assert from 'node:assert/strict'
import { readdirSync, readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { COMPONENT_ERROR_CODES } from '../components/error-codes.mjs'
import { TOKEN_ERROR_CODES } from '../tokens/error-codes.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = join(HERE, '..', '..', '..')
let checks = 0

function check(name, fn) {
  fn()
  checks++
  console.log(`PASS ${name}`)
}

function sourceFiles(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const file = join(directory, entry.name)
    if (entry.isDirectory()) return sourceFiles(file)
    if (!/\.(?:mjs|cjs|js)$/.test(entry.name) || /^test/.test(entry.name)) return []
    return [file]
  })
}

const productionFiles = [
  join(REPO, 'orchestrator', 'figma', 'tokens'),
  join(REPO, 'orchestrator', 'figma', 'components'),
  join(REPO, 'orchestrator', 'figma', 'runtime'),
  join(REPO, 'orchestrator', 'site', 'server'),
  join(REPO, 'orchestrator', 'tasks')
].flatMap(sourceFiles).filter((file) => !file.endsWith('/error-codes.mjs'))

function usedIdentifiers(prefix) {
  const pattern = new RegExp(`\\b(${prefix}_[A-Z0-9_]+)\\b`, 'g')
  const found = new Set()
  for (const file of productionFiles) {
    for (const match of readFileSync(file, 'utf8').matchAll(pattern)) found.add(match[1])
  }
  return found
}

const tokenNonErrors = new Set([
  'TOKEN_CHANGED_SIDE',
  'TOKEN_COMPARISON_REPORT_DIR',
  'TOKEN_DIR',
  'TOKEN_DRIFT_FIXED_ROLES',
  'TOKEN_ERROR_CODES',
  'TOKEN_IDENTITY_QUALITY',
  'TOKEN_KEY_RE',
  'TOKEN_LIMITS',
  'TOKEN_MAPPINGS_PATH',
  'TOKEN_PROVIDER_CAPABILITY',
  'TOKEN_RE',
  'TOKEN_SOURCE_BUCKET_COUNT',
  'TOKEN_SOURCE_HEALTH_',
  'TOKEN_SOURCE_RE',
  'TOKEN_STATUS'
])
const componentNonErrors = new Set([
  'COMPONENT_CHANGED_SIDE',
  'COMPONENT_COMPARATOR_VERSION',
  'COMPONENT_COMPARISON_REPORT_DIR',
  'COMPONENT_DIR',
  'COMPONENT_DRIFT_FIXED_ROLES',
  'COMPONENT_ERROR_CODES',
  'COMPONENT_INVENTORY_LOGICAL_PATH',
  'COMPONENT_LIMITS',
  'COMPONENT_MANIFEST_EXTRACTOR_VERSION',
  'COMPONENT_MAPPINGS_PATH',
  'COMPONENT_MAPPINGS_RELATIVE_PATH',
  'COMPONENT_MAPPING_STATE',
  'COMPONENT_STATUS',
  'COMPONENT_VISUAL',
  'COMPONENT_VISUAL_LOGICAL_DIR'
])

function assertRegistry(sourceFile, registry, prefix, nonErrors) {
  const source = readFileSync(sourceFile, 'utf8')
  const declarations = [...source.matchAll(new RegExp(`\\b(${prefix}_[A-Z0-9_]+)\\s*:`, 'g'))]
    .map((match) => match[1])
  assert.equal(new Set(declarations).size, declarations.length, 'registry contains duplicate keys')
  for (const [key, value] of Object.entries(registry)) assert.equal(value, key)
  const missing = [...usedIdentifiers(prefix)]
    .filter((code) => !nonErrors.has(code) && !Object.hasOwn(registry, code))
    .sort()
  assert.deepEqual(missing, [])
}

check('token production error vocabulary has one complete exact registry', () => {
  assertRegistry(
    join(REPO, 'orchestrator', 'figma', 'tokens', 'error-codes.mjs'),
    TOKEN_ERROR_CODES,
    'TOKEN',
    tokenNonErrors
  )
})

check('component production error vocabulary has one complete exact registry', () => {
  assertRegistry(
    join(REPO, 'orchestrator', 'figma', 'components', 'error-codes.mjs'),
    COMPONENT_ERROR_CODES,
    'COMPONENT',
    componentNonErrors
  )
})

console.log(`error-code registry: ${checks} checks passed`)
