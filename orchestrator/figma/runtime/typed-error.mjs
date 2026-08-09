// Typed pipeline errors. Every expected failure family carries a stable
// machine code, a bounded safe detail string, and a retryability flag; codes
// are owned by their domain module (tokens/error-codes.mjs), never invented at
// throw sites. Messages must stay free of secrets, absolute host paths, and
// raw source contents.
const CODE_RE = /^[A-Z][A-Z0-9_]{2,79}$/
const TYPED_ERROR_BRAND = Symbol.for('bootstrapper-orchestrator.figma.typed-error.v1')

export class TypedError extends Error {
  constructor(code, detail, options) {
    if (!CODE_RE.test(String(code || ''))) throw new Error(`typed error code invalid: ${JSON.stringify(code)}`)
    const safeDetail = String(detail == null ? '' : detail).slice(0, 500)
    super(safeDetail ? `${code}: ${safeDetail}` : code)
    this.name = 'TypedError'
    this.code = code
    this.detail = safeDetail
    this.retryable = !!(options && options.retryable)
    Object.defineProperty(this, TYPED_ERROR_BRAND, { value: true })
    if (options && options.path !== undefined) this.path = String(options.path).slice(0, 300)
  }
}

export function typedError(code, detail, options) {
  return new TypedError(code, detail, options)
}

export function isTypedError(value) {
  return value instanceof TypedError || (!!value && typeof value === 'object' &&
    value[TYPED_ERROR_BRAND] === true && CODE_RE.test(String(value.code || '')) &&
    typeof value.detail === 'string' && typeof value.message === 'string')
}

export function errorCode(value, fallback) {
  if (value && CODE_RE.test(String(value.code || ''))) return value.code
  return fallback
}
