#!/usr/bin/env node

import assert from 'node:assert/strict'
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { createSchemaRegistry, schemaError } from '../../figma/runtime/schema-registry.mjs'
import { loadAdapterConfig } from '../../figma/runtime/adapter-config.mjs'
import { extractProjectTokens } from '../../figma/runtime/token-extraction.mjs'
import { extractProjectComponents } from '../../figma/runtime/component-extraction.mjs'

const REPO = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..')
const BOOTSTRAP = join(REPO, 'orchestrator', 'site', 'server', 'project-adapters-bootstrap.js')
const SCHEMAS = join(REPO, 'orchestrator', 'figma', 'schemas')
const validateAdapters = createSchemaRegistry(SCHEMAS).validate('project-adapters')
const scratch = mkdtempSync(join(tmpdir(), 'project-adapters-bootstrap-'))

function write(file, value = '') {
  mkdirSync(dirname(file), { recursive: true })
  writeFileSync(file, value)
}

function fixture(name, figmaEnabled = true) {
  const root = join(scratch, name)
  const pkg = 'com.example.demo'
  const packagePath = join('com', 'example', 'demo')
  write(join(root, 'orchestrator', 'project-config.md'), [
    '---',
    `productPackage: ${pkg}`,
    `figmaEnabled: ${figmaEnabled}`,
    '---',
    '',
  ].join('\n'))
  mkdirSync(join(root, 'orchestrator', 'figma'), { recursive: true })
  write(join(root, 'design-system', 'resources', 'provider', 'src', 'commonMain', 'kotlin',
    packagePath, 'design', 'system', 'resources', 'provider', 'AppColor.kt'), [
    `package ${pkg}.design.system.resources.provider`,
    'public interface AppColor {',
    '    public val primary: Color',
    '}',
    '',
  ].join('\n'))
  write(join(root, 'design-system', 'resources', 'provider', 'src', 'commonMain', 'kotlin',
    packagePath, 'design', 'system', 'resources', 'provider', 'AppDp.kt'), [
    `package ${pkg}.design.system.resources.provider`,
    'public data object AppDp {',
    '    public val screenPadding: Dp = 16.dp',
    '}',
    '',
  ].join('\n'))
  write(join(root, 'design-system', 'core', 'src', 'commonMain', 'kotlin',
    packagePath, 'design', 'system', 'core', 'AppTokens.kt'), [
    `package ${pkg}.design.system.core`,
    `import ${pkg}.design.system.resources.provider.AppDp`,
    'public object AppTokens {',
    '    public val dp: AppDp get() = AppDp',
    '}',
    '',
  ].join('\n'))
  write(join(root, 'design-system', 'core', 'src', 'commonMain', 'kotlin',
    packagePath, 'design', 'system', 'core', 'LightAppColors.kt'), [
    `package ${pkg}.design.system.core`,
    `import ${pkg}.design.system.resources.provider.AppColor`,
    'public data object LightAppColors : AppColor {',
    '    override val primary: Color = Color(0xFFFFFFFF)',
    '}',
    '',
  ].join('\n'))
  write(join(root, 'design-system', 'core', 'src', 'commonMain', 'kotlin',
    packagePath, 'design', 'system', 'core', 'DarkAppColors.kt'), [
    `package ${pkg}.design.system.core`,
    `import ${pkg}.design.system.resources.provider.AppColor`,
    'public data object DarkAppColors : AppColor {',
    '    override val primary: Color = Color(0xFF000000)',
    '}',
    '',
  ].join('\n'))
  write(join(root, 'design-system', 'components', 'src', 'commonMain', 'kotlin',
    packagePath, 'design', 'system', 'components', 'Button.kt'), [
    `package ${pkg}.design.system.components`,
    'import androidx.compose.runtime.Composable',
    '@Composable',
    'public fun Button(label: String = ""): Unit = Unit',
    '',
  ].join('\n'))
  mkdirSync(join(root, 'design-system', 'preview', 'src', 'commonMain', 'kotlin'), { recursive: true })
  return root
}

function ensure(root) {
  const script = [
    `const bootstrap = require(${JSON.stringify(BOOTSTRAP)});`,
    'process.stdout.write(JSON.stringify(bootstrap.ensure()));',
  ].join('')
  const result = spawnSync(process.execPath, ['-e', script], {
    cwd: root,
    encoding: 'utf8',
    env: {
      ...process.env,
      ORCHESTRATOR_PROJECT_ROOT: root,
      ORCHESTRATOR_CACHE_DIR: join(root, 'orchestrator', '.cache'),
    },
  })
  assert.equal(result.status, 0, result.stderr)
  return JSON.parse(result.stdout)
}

try {
  const root = fixture('fresh-project')
  const first = ensure(root)
  assert.equal(first.ok, true, JSON.stringify(first))
  assert.equal(first.state, 'created')

  const configFile = join(root, 'orchestrator', 'figma', 'project-adapters.json')
  const document = JSON.parse(readFileSync(configFile, 'utf8'))
  assert.equal(schemaError(validateAdapters, document), null)
  assert.deepEqual(document.adapters[0].capabilities, ['tokens', 'components'])
  assert.deepEqual(document.adapters[0].tokens.modes, ['shared', 'light', 'dark'])
  assert.deepEqual(document.adapters[0].components.previewRoots,
    ['design-system/preview/src/commonMain/kotlin'])
  const loaded = loadAdapterConfig({ projectRoot: root, schemaValidate: validateAdapters })
  assert.equal(loaded.state, 'configured')
  const tokens = extractProjectTokens({
    projectRoot: root,
    config: loaded.config,
    configHash: loaded.tokenConfigHash,
  })
  assert.equal(tokens.inventories[0].witness.complete, true)
  assert.ok(tokens.inventories[0].tokens.length >= 2)
  const components = extractProjectComponents({
    projectRoot: root,
    config: loaded.config,
    configHash: loaded.componentConfigHash,
  })
  assert.equal(components.inventories[0].witness.complete, true)
  assert.equal(components.inventories[0].components.length, 1)

  const original = readFileSync(configFile, 'utf8')
  const second = ensure(root)
  assert.equal(second.ok, true)
  assert.equal(second.state, 'existing')
  assert.equal(readFileSync(configFile, 'utf8'), original,
    'a later sync must never rewrite the product-owned adapter config')

  const customRoot = fixture('custom-project')
  const customFile = join(customRoot, 'orchestrator', 'figma', 'project-adapters.json')
  write(customFile, '{"owner":"product"}\n')
  assert.equal(ensure(customRoot).state, 'existing')
  assert.equal(readFileSync(customFile, 'utf8'), '{"owner":"product"}\n')

  const disabledRoot = fixture('figma-disabled', false)
  assert.deepEqual(ensure(disabledRoot), { ok: false, state: 'disabled' })
  assert.throws(() => readFileSync(join(disabledRoot, 'orchestrator', 'figma', 'project-adapters.json')),
    { code: 'ENOENT' })
} finally {
  rmSync(scratch, { recursive: true, force: true })
}

console.log('ok - fresh projects auto-create one schema-valid, no-clobber Figma adapter config')
