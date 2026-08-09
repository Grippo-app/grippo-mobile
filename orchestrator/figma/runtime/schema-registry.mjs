import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'
import Ajv from 'ajv'

function isoDateTime(value) {
  if (typeof value !== 'string' ||
      !/^\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])T(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d(?:\.\d{3})?Z$/.test(value)) return false
  const parsed = new Date(value)
  if (!Number.isFinite(parsed.getTime())) return false
  return parsed.toISOString() === (value.includes('.') ? value : value.replace('Z', '.000Z'))
}

export function createSchemaRegistry(schemasDir, options = {}) {
  const ajv = new Ajv({ allErrors: true, strict: false, ...options })
  ajv.addFormat('date-time', { type: 'string', validate: isoDateTime })
  const documents = readdirSync(schemasDir)
    .filter((name) => name.endsWith('.schema.json'))
    .sort()
    .map((name) => ({
      name,
      schema: JSON.parse(readFileSync(join(schemasDir, name), 'utf8'))
    }))
  for (const { name, schema } of documents) {
    ajv.addSchema(schema, schema.$id || name)
  }
  return Object.freeze({
    names: Object.freeze(documents.map(({ name }) => name.replace('.schema.json', ''))),
    validate(name) {
      const file = name.endsWith('.schema.json') ? name : `${name}.schema.json`
      const validate = ajv.getSchema(file)
      if (!validate) throw new Error(`unknown schema ${file}`)
      return validate
    }
  })
}

export function schemaError(validate, value) {
  if (validate(value)) return null
  const first = (validate.errors || [])[0]
  return `schema: ${(first && (first.instancePath || '/') + ' ' + first.message) || 'invalid document'}`
}
