#!/usr/bin/env node
import { existsSync, lstatSync, readFileSync, readdirSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import { join, relative, resolve, sep } from 'node:path'

const root = resolve(process.argv[2] || process.cwd())
const generator = join(root, 'orchestrator', 'template-sync', '_generate_template_manifest.py')
const launchFile = join(root, 'orchestrator', 'launch.md')
const ignoreFile = join(root, '.gitignore')
const failures = []

for (const file of [generator, launchFile, ignoreFile]) {
  if (!existsSync(file)) failures.push(`missing required contract surface: ${relative(root, file)}`)
}

function meaningfulLines(text) {
  return new Set(text.split(/\r?\n/).map((line) => line.trim()).filter((line) => line && !line.startsWith('#')))
}

function excluded(exclusions, rel) {
  const name = rel.split('/').at(-1)
  return exclusions.exact.includes(rel) || exclusions.prefixes.some((prefix) => rel.startsWith(prefix)) ||
    exclusions.basenames.includes(name) || exclusions.basenameSuffixes.some((suffix) => name.endsWith(suffix)) ||
    exclusions.basenamePrefixes.some((prefix) => name.startsWith(prefix))
}

if (!failures.length) {
  const launch = readFileSync(launchFile, 'utf8')
  const ignoreSource = readFileSync(ignoreFile, 'utf8')
  const ignoreBlock = ignoreSource.match(/# BEGIN ORCHESTRATOR RUNTIME IGNORE CONTRACT\s*\n([\s\S]*?)\n# END ORCHESTRATOR RUNTIME IGNORE CONTRACT/)
  if (!ignoreBlock) failures.push('root .gitignore has no exact orchestrator runtime contract block')
  const ignore = meaningfulLines(ignoreBlock ? ignoreBlock[1] : '')
  const step = launch.match(/## Step 2\.5[^\n]*\n[\s\S]*?```\n([\s\S]*?)\n```/)
  if (!step) failures.push('orchestrator/launch.md has no parseable Step 2.5 .gitignore block')
  const launchBlock = (step ? step[1] : '').match(/# BEGIN ORCHESTRATOR RUNTIME IGNORE CONTRACT\s*\n([\s\S]*?)\n# END ORCHESTRATOR RUNTIME IGNORE CONTRACT/)
  if (!launchBlock) failures.push('launch Step 2.5 has no exact orchestrator runtime contract block')
  const launchIgnore = meaningfulLines(launchBlock ? launchBlock[1] : '')
  for (const pattern of [...ignore].filter((pattern) => !launchIgnore.has(pattern)).sort()) {
    failures.push(`root .gitignore runtime pattern is absent from launch Step 2.5: ${pattern}`)
  }
  for (const pattern of [...launchIgnore].filter((pattern) => !ignore.has(pattern)).sort()) {
    failures.push(`launch Step 2.5 runtime pattern is absent from root .gitignore: ${pattern}`)
  }
  const sharedRuntimePatterns = [
    '.claude/settings.local.json',
    'node_modules/',
    '.env',
    '__pycache__/',
    '*.pyc',
    'orchestrator/figma/.account.json',
    'orchestrator/api-contract/.secrets/',
    'orchestrator/.cache/',
    'orchestrator/tasks/todo/.finalize-*.ship',
    'orchestrator/tasks/todo/.finalize-*.ship.tmp.*',
    'orchestrator/tasks/todo/.finalize-*.detach.md',
  ]
  for (const pattern of sharedRuntimePatterns) {
    if (!ignore.has(pattern)) failures.push(`root .gitignore misses launch runtime pattern: ${pattern}`)
    if (!launchIgnore.has(pattern)) failures.push(`launch Step 2.5 misses root runtime pattern: ${pattern}`)
  }

  const result = spawnSync('python3', [generator, '--print-exclusions'], { encoding: 'utf8' })
  if (result.status !== 0) {
    failures.push(`cannot read canonical template exclusions: ${(result.stderr || result.stdout || '').trim()}`)
  } else {
    let exclusions
    try { exclusions = JSON.parse(result.stdout) }
    catch (error) { failures.push(`canonical template exclusions are invalid JSON: ${error.message}`) }
    if (exclusions) {
      const requiredExclusions = [
        ['prefixes', 'orchestrator/.cache/'],
        ['prefixes', 'orchestrator/api-contract/.secrets/'],
        ['prefixes', 'orchestrator/tasks/todo/'],
        ['exact', 'orchestrator/figma/.account.json'],
        ['basenames', 'node_modules'],
        ['basenames', '.env'],
        ['basenames', '__pycache__'],
        ['basenameSuffixes', '.pyc'],
      ]
      for (const [key, value] of requiredExclusions) {
        if (!Array.isArray(exclusions[key]) || !exclusions[key].includes(value)) {
          failures.push(`template exclusions miss ${key} entry: ${value}`)
        }
      }

      for (const relRoot of ['orchestrator/figma/tokens', 'orchestrator/figma/manifests']) {
        const absoluteRoot = join(root, ...relRoot.split('/'))
        const pending = existsSync(absoluteRoot) ? [absoluteRoot] : []
        while (pending.length) {
          const current = pending.pop()
          for (const name of readdirSync(current)) {
            const absolute = join(current, name)
            const stat = lstatSync(absolute)
            if (stat.isDirectory() && !stat.isSymbolicLink()) pending.push(absolute)
            else if (stat.isFile()) {
              const rel = relative(root, absolute).split(sep).join('/')
              if (excluded(exclusions, rel)) failures.push(`committed Figma source is hidden from template sync: ${rel}`)
            }
          }
        }
      }
    }
  }
}

if (failures.length) {
  for (const failure of failures) console.error(`TEMPLATE_EXCLUSION_CONTRACT: ${failure}`)
  process.exit(1)
}
console.log('ok - template exclusions, launch instructions, and root ignore policy agree')
