// Built-in adapter registry. Config selects a shipped kind; this module maps
// the kind to its extractor implementation. No user-supplied executable,
// module path, or command ever reaches this table (REQ-ADAPT-001).
//
// Extractor contract (pure, deterministic, read-only):
//   extractTokens({ files, tokensConfig, adapterId }) -> {
//     tokens:        project-token-inventory tokens[] rows (without the
//                    envelope; source.fileHash filled from files),
//     parseFailures: [{ path, reason }],
//     limitations:   [string]
//   }
// where files = [{ path, text, hash }] is the exact input snapshot the
// runner read. Extractors never touch the filesystem, environment, network,
// or clock; same inputs must serialize to the same output bytes.
import { typedError } from './typed-error.mjs'
import { ADAPTER_ERROR_CODES } from './error-codes.mjs'
import { extractTokens as extractKotlinComposeTokens, KOTLIN_COMPOSE_EXTRACTOR_VERSION } from '../adapters/kotlin-compose/tokens.mjs'
import { extractTokens as extractJsonTokens, JSON_TOKENS_EXTRACTOR_VERSION } from '../adapters/json-tokens/tokens.mjs'
import { extractComponents as extractKotlinComposeComponents, KOTLIN_COMPOSE_COMPONENTS_EXTRACTOR_VERSION } from '../adapters/kotlin-compose/components.mjs'
import { extractComponents as extractManifestComponents, COMPONENT_MANIFEST_EXTRACTOR_VERSION } from '../adapters/component-manifest/components.mjs'

const REGISTRY = Object.freeze({
  'kotlin-compose': Object.freeze({
    version: 2,
    extractorVersion: KOTLIN_COMPOSE_EXTRACTOR_VERSION,
    extractTokens: extractKotlinComposeTokens,
    componentsExtractorVersion: KOTLIN_COMPOSE_COMPONENTS_EXTRACTOR_VERSION,
    extractComponents: extractKotlinComposeComponents
  }),
  'json-tokens': Object.freeze({
    version: 2,
    extractorVersion: JSON_TOKENS_EXTRACTOR_VERSION,
    extractTokens: extractJsonTokens
  }),
  'component-manifest': Object.freeze({
    version: 2,
    componentsExtractorVersion: COMPONENT_MANIFEST_EXTRACTOR_VERSION,
    extractComponents: extractManifestComponents
  })
})

export function adapterImplementation(kind) {
  const implementation = REGISTRY[kind]
  if (!implementation) {
    throw typedError(ADAPTER_ERROR_CODES.PROJECT_ADAPTER_UNKNOWN, `no built-in adapter implements kind ${JSON.stringify(kind)}`)
  }
  return implementation
}
