import assert from 'node:assert/strict'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { validateAdapterConfig } from '../runtime/adapter-config.mjs'
import { createSchemaRegistry } from '../runtime/schema-registry.mjs'

const HERE = dirname(fileURLToPath(import.meta.url))
const schemas = createSchemaRegistry(join(HERE, '..', 'schemas'))
const validateSchema = schemas.validate('project-adapters')
let passed = 0
function check(name, fn) {
  try {
    fn()
    passed++
    console.log(`PASS ${name}`)
  } catch (error) {
    console.error(`FAIL ${name}\n  ${error.stack || error.message}`)
    process.exitCode = 1
  }
}
function config() {
  return {
    schemaVersion: 2,
    adapters: [{
      id: 'compose-design-system',
      kind: 'kotlin-compose',
      version: 2,
      enabled: true,
      capabilities: ['tokens', 'components'],
      platform: 'android',
      authority: 'handwritten',
      tokens: {
        roots: ['design-system/src'],
        include: ['**/*.kt'],
        exclude: ['**/build/**'],
        modes: ['light', 'dark'],
        authorities: {
          color: {
            contracts: ['com.example.design.AppColor'],
            implementations: [
              { mode: 'light', symbols: ['com.example.design.LightColor'] },
              { mode: 'dark', symbols: ['com.example.design.DarkColor'] }
            ]
          }
        },
        contextMap: [
          { when: { theme: 'light', platform: 'shared' }, projectMode: 'light' },
          { when: { theme: 'dark', platform: 'shared' }, projectMode: 'dark' }
        ],
        bindingRules: [{
          ruleId: 'color-prefix',
          kind: 'prefix-map',
          tokenKind: 'color',
          providerPrefix: ['color'],
          projectPrefix: ['AppColor'],
          caseTransform: 'camel',
          excludeExact: [['color', 'retired']],
          excludePrefix: [['color', 'brand']]
        }, {
          ruleId: 'brand-primary',
          kind: 'exact-path',
          tokenKind: 'color',
          providerPath: ['color', 'brand', 'primary'],
          projectPath: ['AppColor', 'brandPrimary']
        }]
      },
      components: {
        roots: ['design-system/src'],
        include: ['**/*.kt'],
        exclude: ['**/build/**'],
        visibility: ['public'],
        previewRoots: ['design-system/previews'],
        screenshotTestRoots: []
      }
    }]
  }
}

check('strict adapter config validates shared token/component capability sections', () => {
  const root = mkdtempSync(join(tmpdir(), 'adapter-contract-'))
  try {
    const document = config()
    assert.equal(validateSchema(document), true, JSON.stringify(validateSchema.errors))
    const result = validateAdapterConfig(document, { projectRoot: root, schemaValidate: validateSchema })
    assert.equal(result.schemaVersion, 2)
    assert.equal(result.enabledTokenAdapters.length, 1)
    assert.equal(result.enabledComponentAdapters.length, 1)
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

check('unknown versions and priority fields fail schema validation', () => {
  const old = config()
  old.schemaVersion = 1
  assert.equal(validateSchema(old), false)
  const priority = config()
  priority.adapters[0].tokens.bindingRules[0].priority = 10
  assert.equal(validateSchema(priority), false)
})

check('overlapping context rules fail semantic validation', () => {
  const root = mkdtempSync(join(tmpdir(), 'adapter-contract-'))
  try {
    const document = config()
    document.adapters[0].tokens.contextMap.push({
      when: { theme: 'light' },
      projectMode: 'light'
    })
    assert.throws(() => validateAdapterConfig(document, { projectRoot: root, schemaValidate: validateSchema }), /contextMap rules .* overlap/)
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

check('context map cannot target an undeclared project mode', () => {
  const root = mkdtempSync(join(tmpdir(), 'adapter-contract-'))
  try {
    const document = config()
    document.adapters[0].tokens.contextMap[0].projectMode = 'high-contrast'
    assert.throws(() => validateAdapterConfig(document, { projectRoot: root, schemaValidate: validateSchema }), /outside tokens.modes/)
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

check('prefix rules require exact exclusions before an exception rule may coexist', () => {
  const root = mkdtempSync(join(tmpdir(), 'adapter-contract-'))
  try {
    const document = config()
    document.adapters[0].tokens.bindingRules[0].excludePrefix = []
    assert.throws(() => validateAdapterConfig(document, { projectRoot: root, schemaValidate: validateSchema }), /binding rules .* overlap/)
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

check('duplicate explicit-table provider paths fail closed', () => {
  const root = mkdtempSync(join(tmpdir(), 'adapter-contract-'))
  try {
    const document = config()
    document.adapters[0].tokens.bindingRules = [{
      ruleId: 'table',
      kind: 'explicit-table',
      tokenKind: 'color',
      entries: [
        { providerPath: ['color', 'primary'], projectPath: ['AppColor', 'primary'] },
        { providerPath: ['color', 'primary'], projectPath: ['AppColor', 'other'] }
      ]
    }]
    assert.throws(() => validateAdapterConfig(document, { projectRoot: root, schemaValidate: validateSchema }), /repeats providerPath/)
  } finally {
    rmSync(root, { recursive: true, force: true })
  }
})

if (!process.exitCode) console.log(`adapter contract: ${passed} checks passed`)
