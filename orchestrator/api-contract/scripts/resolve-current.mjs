// Read-only locator for the validated current contract snapshot. Consumers use
// these returned project-relative paths instead of guessing a generation id.
import { isAbsolute, relative, sep } from 'node:path'

if (process.argv.length !== 2) {
  process.stderr.write('contract:paths accepts no arguments\n')
  process.exit(2)
}
if (process.env.ORCHESTRATOR_API_CONTRACT_DATA_DIR) {
  process.stderr.write('contract:paths refuses the internal staging data override\n')
  process.exit(2)
}

const { currentContractFiles, EXECUTION_ROOT } = await import('./_util.mjs')

function projectRelative(file) {
  if (!file) return null
  const rel = relative(EXECUTION_ROOT, file)
  if (!rel || rel === '..' || rel.startsWith(`..${sep}`) || isAbsolute(rel)) {
    throw new Error('resolved contract artifact escapes the project root')
  }
  return rel.split(sep).join('/')
}

const current = currentContractFiles()
if (current.invalid) {
  const errorCode = typeof current.error === 'string' && current.error
    ? current.error
    : 'generation-reader-contract-invalid'
  process.stderr.write(`current contract generation is invalid: ${errorCode}\n`)
  process.exit(1)
}

const output = {
  schemaVersion: 1,
  present: current.mode !== 'none',
  mode: current.mode,
  snapshotHash: current.snapshotHash || null,
  inventory: projectRelative(current.inventory),
  areasDir: projectRelative(current.areasDir),
  spec: projectRelative(current.spec),
}
process.stdout.write(`${JSON.stringify(output)}\n`)
