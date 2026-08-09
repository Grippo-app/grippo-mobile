import { createHash } from 'node:crypto'

const CLASSIFIER_VERSION = 'api-change-classifier-v1'
const CHANGE_COUNT_MAX = 10000
const CHANGE_BYTES_MAX = 8 * 1024 * 1024

const SEVERITY_RANK = Object.freeze({
  info: 0,
  compatible: 1,
  'potentially-breaking': 2,
  breaking: 3,
})

function stable(value) {
  if (Array.isArray(value)) return value.map(stable)
  if (!value || typeof value !== 'object') return value
  const out = Object.create(null)
  for (const key of Object.keys(value).sort()) out[key] = stable(value[key])
  return out
}

function hash(value) {
  return 'sha256:' + createHash('sha256')
    .update(JSON.stringify(stable(value)), 'utf8').digest('hex')
}

function bounded(value, limit = 500) {
  const clean = String(value == null ? '' : value)
    .normalize('NFC')
    .replace(/[\u0000-\u001f\u007f-\u009f\u2028\u2029]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  return Array.from(clean).slice(0, limit).join('')
}

function routeKey(endpoint) {
  return `${endpoint.method} ${endpoint.path}`
}

function fieldMap(schema) {
  return new Map((schema && Array.isArray(schema.fields) ? schema.fields : [])
    .filter((field) => field && typeof field.jsonName === 'string')
    .map((field) => [field.jsonName, field]))
}

function enumSet(field) {
  return Array.isArray(field && field.enum)
    ? new Set(field.enum.map((value) => JSON.stringify(stable(value))))
    : null
}

function semanticConstraints(field) {
  const constraints = field && field.constraints
  if (!constraints || typeof constraints !== 'object' || Array.isArray(constraints)) return null
  const meaningful = Object.fromEntries(Object.entries(constraints)
    .filter(([, value]) => value !== null && value !== undefined && value !== ''))
  return Object.keys(meaningful).length ? stable(meaningful) : null
}

function lowerBound(constraints) {
  const candidates = []
  if (typeof constraints.minimum === 'number') {
    candidates.push({ value: constraints.minimum, exclusive: false })
  }
  if (typeof constraints.exclusiveMinimum === 'number') {
    candidates.push({ value: constraints.exclusiveMinimum, exclusive: true })
  }
  return candidates.sort((left, right) =>
    right.value - left.value || Number(right.exclusive) - Number(left.exclusive))[0] ||
    { value: Number.NEGATIVE_INFINITY, exclusive: false }
}

function upperBound(constraints) {
  const candidates = []
  if (typeof constraints.maximum === 'number') {
    candidates.push({ value: constraints.maximum, exclusive: false })
  }
  if (typeof constraints.exclusiveMaximum === 'number') {
    candidates.push({ value: constraints.exclusiveMaximum, exclusive: true })
  }
  return candidates.sort((left, right) =>
    left.value - right.value || Number(right.exclusive) - Number(left.exclusive))[0] ||
    { value: Number.POSITIVE_INFINITY, exclusive: false }
}

function constraintRelation(beforeField, afterField) {
  const before = semanticConstraints(beforeField) || {}
  const after = semanticConstraints(afterField) || {}
  let narrowed = false
  let widened = false
  const oldLower = lowerBound(before)
  const nextLower = lowerBound(after)
  if (nextLower.value > oldLower.value ||
      nextLower.value === oldLower.value && nextLower.exclusive && !oldLower.exclusive) {
    narrowed = true
  }
  if (nextLower.value < oldLower.value ||
      nextLower.value === oldLower.value && !nextLower.exclusive && oldLower.exclusive) {
    widened = true
  }
  const oldUpper = upperBound(before)
  const nextUpper = upperBound(after)
  if (nextUpper.value < oldUpper.value ||
      nextUpper.value === oldUpper.value && nextUpper.exclusive && !oldUpper.exclusive) {
    narrowed = true
  }
  if (nextUpper.value > oldUpper.value ||
      nextUpper.value === oldUpper.value && !nextUpper.exclusive && oldUpper.exclusive) {
    widened = true
  }
  const lower = ['minLength', 'minItems', 'minProperties']
  const upper = ['maxLength', 'maxItems', 'maxProperties']
  for (const key of lower) {
    const left = before[key] == null ? Number.NEGATIVE_INFINITY : before[key]
    const right = after[key] == null ? Number.NEGATIVE_INFINITY : after[key]
    if (right > left) narrowed = true
    if (right < left) widened = true
  }
  for (const key of upper) {
    const left = before[key] == null ? Number.POSITIVE_INFINITY : before[key]
    const right = after[key] == null ? Number.POSITIVE_INFINITY : after[key]
    if (right < left) narrowed = true
    if (right > left) widened = true
  }
  const beforePattern = before.patternHash || before.pattern || null
  const afterPattern = after.patternHash || after.pattern || null
  if (!beforePattern && afterPattern) narrowed = true
  else if (beforePattern && !afterPattern) widened = true
  else if (beforePattern !== afterPattern) {
    narrowed = true
    widened = true
  }
  if (narrowed && widened) return 'changed'
  if (narrowed) return 'narrowed'
  if (widened) return 'widened'
  return 'same'
}

function setRelation(before, after) {
  if (!before && !after) return 'same'
  if (!before && after) return 'narrowed'
  if (before && !after) return 'widened'
  const removed = [...before].some((value) => !after.has(value))
  const added = [...after].some((value) => !before.has(value))
  if (removed && !added) return 'narrowed'
  if (added && !removed) return 'widened'
  return removed || added ? 'changed' : 'same'
}

function typeCompatible(before, after) {
  if (before === after) return true
  return before === 'integer' && after === 'number'
}

function authStrength(before, after) {
  const left = before == null || before === 'none' ? 'none' : String(before)
  const right = after == null || after === 'none' ? 'none' : String(after)
  if (left === right) return 'same'
  if (left === 'none' && right !== 'none') return 'strengthened'
  if (left !== 'none' && right === 'none') return 'relaxed'
  return 'changed'
}

function changeId(input) {
  return 'chg-' + hash({
    classifierVersion: CLASSIFIER_VERSION,
    previousHash: input.previousHash,
    currentHash: input.currentHash,
    kind: input.kind,
    operationId: input.operationId || null,
    modelId: input.modelId || null,
    evidence: input.evidence,
  }).slice('sha256:'.length, 'sha256:'.length + 24)
}

function changeRow(input) {
  const evidence = (Array.isArray(input.evidence) ? input.evidence : [])
    .slice(0, 20)
    .map((value) => bounded(value, 500))
    .filter(Boolean)
  const row = {
    id: null,
    kind: bounded(input.kind, 80),
    severity: Object.prototype.hasOwnProperty.call(SEVERITY_RANK, input.severity)
      ? input.severity : 'info',
    operationId: input.operationId ? bounded(input.operationId, 200) : null,
    modelId: input.modelId ? bounded(input.modelId, 200) : null,
    beforeSummary: input.beforeSummary ? bounded(input.beforeSummary, 500) : null,
    afterSummary: input.afterSummary ? bounded(input.afterSummary, 500) : null,
    affectedConsumers: [],
    affectedImplementation: null,
    linkedTasks: [],
    evidence,
  }
  row.id = changeId({ ...input, evidence })
  return row
}

function endpointModels(endpoint) {
  const requestIds = []
  const responseIds = []
  if (endpoint && endpoint.request && endpoint.request.body && endpoint.request.body.schemaRef) {
    requestIds.push(endpoint.request.body.schemaRef)
  }
  for (const response of Object.values(endpoint && endpoint.response || {})) {
    if (response && response.schemaRef && !responseIds.includes(response.schemaRef)) {
      responseIds.push(response.schemaRef)
    }
  }
  return { requestIds, responseIds }
}

function referencedModels(schema) {
  const out = []
  for (const field of schema && Array.isArray(schema.fields) ? schema.fields : []) {
    if (typeof field.type === 'string' && field.type.startsWith('ref:')) {
      out.push(field.type.slice(4))
    }
    if (typeof field.itemsRef === 'string' && field.itemsRef) out.push(field.itemsRef)
  }
  return out
}

function reachableModels(rootIds, schemas, analysis) {
  const found = new Set()
  const pending = [...rootIds]
  while (pending.length && found.size < 10000) {
    const id = pending.shift()
    if (!id || found.has(id)) continue
    found.add(id)
    const schema = schemas && Object.prototype.hasOwnProperty.call(schemas, id)
      ? schemas[id] : null
    for (const referenced of referencedModels(schema)) {
      if (!found.has(referenced)) pending.push(referenced)
    }
  }
  if (analysis && pending.some((id) => id && !found.has(id))) {
    analysis.modelGraphTruncated = true
  }
  return found
}

function compareFields(context, beforeSchema, afterSchema, usage) {
  const before = fieldMap(beforeSchema)
  const after = fieldMap(afterSchema)
  const changes = []
  const modelId = context.modelId
  for (const [name, oldField] of before) {
    const nextField = after.get(name)
    if (!nextField) {
      const severity = usage === 'response' ? 'breaking' : 'potentially-breaking'
      changes.push(changeRow({
        ...context,
        kind: usage === 'response' ? 'response-field-removed' : 'request-field-removed',
        severity,
        modelId,
        beforeSummary: `${modelId}.${name} was present`,
        afterSummary: `${modelId}.${name} was removed`,
        evidence: [`${usage} field ${modelId}.${name} was removed`],
      }))
      continue
    }
    const typeIsCompatible = usage === 'request'
      ? typeCompatible(oldField.type, nextField.type)
      : typeCompatible(nextField.type, oldField.type)
    if (!typeIsCompatible) {
      changes.push(changeRow({
        ...context,
        kind: `${usage}-field-type-changed`,
        severity: 'breaking',
        modelId,
        beforeSummary: `${modelId}.${name}: ${oldField.type}`,
        afterSummary: `${modelId}.${name}: ${nextField.type}`,
        evidence: [`${usage} field type changed from ${oldField.type} to ${nextField.type}`],
      }))
    } else if (oldField.type !== nextField.type) {
      changes.push(changeRow({
        ...context,
        kind: `${usage}-field-type-compatible`,
        severity: 'compatible',
        modelId,
        beforeSummary: `${modelId}.${name}: ${oldField.type}`,
        afterSummary: `${modelId}.${name}: ${nextField.type}`,
        evidence: [`${usage} field value domain changed compatibly from ${oldField.type} to ${nextField.type}`],
      }))
    }
    if (oldField.type === 'array' && nextField.type === 'array' &&
        (oldField.itemsRef || null) !== (nextField.itemsRef || null)) {
      changes.push(changeRow({
        ...context,
        kind: `${usage}-array-item-reference-changed`,
        severity: 'breaking',
        modelId,
        beforeSummary: `${modelId}.${name} items: ${oldField.itemsRef || 'unmodeled'}`,
        afterSummary: `${modelId}.${name} items: ${nextField.itemsRef || 'unmodeled'}`,
        evidence: [`${usage} array item model changed for ${modelId}.${name}`],
      }))
    }
    const nullableNarrowed = oldField.nullable_declared === true &&
      nextField.nullable_declared === false
    const nullableWidened = oldField.nullable_declared !== true &&
      nextField.nullable_declared === true
    if ((usage === 'request' && nullableNarrowed) ||
        (usage === 'response' && nullableWidened)) {
      changes.push(changeRow({
        ...context,
        kind: `${usage}-nullability-${nullableNarrowed ? 'narrowed' : 'widened'}`,
        severity: 'breaking',
        modelId,
        beforeSummary: `${modelId}.${name} ${oldField.nullable_declared === true ? 'allowed' : 'did not allow'} null`,
        afterSummary: `${modelId}.${name} ${nextField.nullable_declared === true ? 'allows' : 'no longer allows'} null`,
        evidence: [`${usage} nullability ${nullableNarrowed ? 'narrowed' : 'widened'} for ${modelId}.${name}`],
      }))
    } else if (nullableNarrowed || nullableWidened) {
      changes.push(changeRow({
        ...context,
        kind: `${usage}-nullability-compatible`,
        severity: 'compatible',
        modelId,
        beforeSummary: `${modelId}.${name} ${oldField.nullable_declared === true ? 'allowed' : 'did not allow'} null`,
        afterSummary: `${modelId}.${name} ${nextField.nullable_declared === true ? 'allows' : 'no longer allows'} null`,
        evidence: [`${usage} nullability changed compatibly for ${modelId}.${name}`],
      }))
    }
    const enumRelation = setRelation(enumSet(oldField), enumSet(nextField))
    if (enumRelation === 'changed' ||
        (usage === 'request' && enumRelation === 'narrowed') ||
        (usage === 'response' && enumRelation === 'widened')) {
      changes.push(changeRow({
        ...context,
        kind: `${usage}-enum-${enumRelation}`,
        severity: usage === 'response' ? 'potentially-breaking' : 'breaking',
        modelId,
        beforeSummary: `${modelId}.${name} allowed values changed`,
        afterSummary: `${modelId}.${name} has a different value domain`,
        evidence: [
          `${usage} enum values were ${enumRelation} for ${modelId}.${name}`,
          usage === 'response'
            ? 'Exhaustive consumers may require an update'
            : 'Previously valid requests may be rejected',
        ],
      }))
    } else if (enumRelation !== 'same') {
      changes.push(changeRow({
        ...context,
        kind: `${usage}-enum-${enumRelation}`,
        severity: 'compatible',
        modelId,
        beforeSummary: `${modelId}.${name} allowed values changed`,
        afterSummary: `${modelId}.${name} has a narrower compatible value domain`,
        evidence: [
          `${usage} enum values were ${enumRelation} for ${modelId}.${name}`,
        ],
      }))
    }
    const constraints = constraintRelation(oldField, nextField)
    if (constraints !== 'same') {
      const incompatible = constraints === 'changed' ||
        (usage === 'request' && constraints === 'narrowed') ||
        (usage === 'response' && constraints === 'widened')
      changes.push(changeRow({
        ...context,
        kind: `${usage}-constraints-${constraints}`,
        severity: incompatible ? 'potentially-breaking' : 'compatible',
        modelId,
        beforeSummary: `${modelId}.${name} validation constraints changed`,
        afterSummary: `${modelId}.${name} has a different accepted value domain`,
        evidence: [
          `${usage} validation constraints ${constraints} for ${modelId}.${name}`,
        ],
      }))
    }
    if ((oldField.format || null) !== (nextField.format || null)) {
      changes.push(changeRow({
        ...context,
        kind: `${usage}-format-changed`,
        severity: 'potentially-breaking',
        modelId,
        beforeSummary: `${modelId}.${name} format ${oldField.format || 'unspecified'}`,
        afterSummary: `${modelId}.${name} format ${nextField.format || 'unspecified'}`,
        evidence: [`semantic format changed for ${modelId}.${name}`],
      }))
    }
    if (usage === 'request' && oldField.required !== true && nextField.required === true) {
      changes.push(changeRow({
        ...context,
        kind: 'request-field-became-required',
        severity: 'breaking',
        modelId,
        beforeSummary: `${modelId}.${name} was optional`,
        afterSummary: `${modelId}.${name} is required`,
        evidence: [`request field ${modelId}.${name} became required`],
      }))
    } else if (usage === 'response' && oldField.required === true && nextField.required !== true) {
      changes.push(changeRow({
        ...context,
        kind: 'response-field-became-optional',
        severity: 'breaking',
        modelId,
        beforeSummary: `${modelId}.${name} was required`,
        afterSummary: `${modelId}.${name} may now be omitted`,
        evidence: [`response field ${modelId}.${name} became optional`],
      }))
    } else if ((usage === 'response' && oldField.required !== true && nextField.required === true) ||
        (usage === 'request' && oldField.required === true && nextField.required !== true)) {
      changes.push(changeRow({
        ...context,
        kind: `${usage}-requiredness-compatible`,
        severity: 'compatible',
        modelId,
        beforeSummary: `${modelId}.${name} was ${oldField.required === true ? 'required' : 'optional'}`,
        afterSummary: `${modelId}.${name} is ${nextField.required === true ? 'required' : 'optional'}`,
        evidence: [`${usage} requiredness changed compatibly for ${modelId}.${name}`],
      }))
    }
  }
  for (const [name, nextField] of after) {
    if (before.has(name)) continue
    let severity = 'compatible'
    let kind = `${usage}-optional-field-added`
    if (usage === 'request' && nextField.required === true) {
      severity = 'breaking'
      kind = 'request-required-field-added'
    } else if (usage === 'response' && nextField.required === true) {
      severity = 'potentially-breaking'
      kind = 'response-required-field-added'
    }
    changes.push(changeRow({
      ...context,
      kind,
      severity,
      modelId,
      beforeSummary: `${modelId}.${name} was absent`,
      afterSummary: `${modelId}.${name} was added`,
      evidence: [`${usage} ${nextField.required ? 'required' : 'optional'} field ${modelId}.${name} was added`],
    }))
  }
  return changes
}

function schemasFor(areas, area) {
  const slice = areas && Object.hasOwn(areas, area) ? areas[area] : null
  return slice && slice.schemas && typeof slice.schemas === 'object'
    ? slice.schemas : {}
}

function compareModels(context, oldEndpoint, nextEndpoint, previousAreas, nextAreas) {
  const changes = []
  const beforeModels = endpointModels(oldEndpoint)
  const afterModels = endpointModels(nextEndpoint)
  const beforeSchemas = schemasFor(previousAreas, oldEndpoint.area)
  const afterSchemas = schemasFor(nextAreas, nextEndpoint.area)
  const usages = [
    ['request', beforeModels.requestIds, afterModels.requestIds],
    ['response', beforeModels.responseIds, afterModels.responseIds],
  ]
  for (const [usage, beforeIds, afterIds] of usages) {
    if (usage === 'request' &&
        JSON.stringify([...beforeIds].sort()) !== JSON.stringify([...afterIds].sort())) {
      changes.push(changeRow({
        ...context,
        kind: `${usage}-model-reference-changed`,
        severity: 'breaking',
        modelId: afterIds[0] || beforeIds[0] || null,
        beforeSummary: `${usage} models: ${beforeIds.join(', ') || 'none'}`,
        afterSummary: `${usage} models: ${afterIds.join(', ') || 'none'}`,
        evidence: [`${usage} model reference changed`],
      }))
    }
    const beforeReachable = reachableModels(beforeIds, beforeSchemas, context.analysis)
    const afterReachable = reachableModels(afterIds, afterSchemas, context.analysis)
    const all = [...new Set([...beforeReachable, ...afterReachable])]
    for (const id of all) {
      // A graph edge change is already represented by the parent field, request
      // body, response status, or response schema row. Only compare definitions
      // which remain reachable on both sides to avoid duplicate work items.
      if (!beforeReachable.has(id) || !afterReachable.has(id)) continue
      const beforeSchema = beforeSchemas[id]
      const afterSchema = afterSchemas[id]
      if (!beforeSchema || !afterSchema) {
        if (beforeSchema !== afterSchema) {
          changes.push(changeRow({
            ...context,
            kind: `${usage}-model-changed`,
            severity: 'breaking',
            modelId: id,
            beforeSummary: beforeSchema ? `${id} was referenced` : `${id} was absent`,
            afterSummary: afterSchema ? `${id} is referenced` : `${id} is no longer referenced`,
            evidence: [`${usage} model reference changed for ${id}`],
          }))
        }
        continue
      }
      changes.push(...compareFields({ ...context, modelId: id }, beforeSchema, afterSchema, usage))
    }
  }
  return changes
}

function compareParameters(context, oldEndpoint, nextEndpoint) {
  const changes = []
  for (const group of ['pathParams', 'query']) {
    const before = new Map((oldEndpoint.request && oldEndpoint.request[group] || [])
      .map((parameter) => [parameter.name, parameter]))
    const after = new Map((nextEndpoint.request && nextEndpoint.request[group] || [])
      .map((parameter) => [parameter.name, parameter]))
    for (const [name, parameter] of after) {
      const old = before.get(name)
      if (!old) {
        changes.push(changeRow({
          ...context,
          kind: parameter.required
            ? 'request-required-parameter-added'
            : 'request-optional-parameter-added',
          severity: parameter.required ? 'breaking' : 'compatible',
          beforeSummary: `${group}.${name} was absent`,
          afterSummary: `${group}.${name} is ${parameter.required ? 'required' : 'optional'}`,
          evidence: [
            `${parameter.required ? 'required' : 'optional'} ${group} parameter ${name} was added`,
          ],
        }))
      } else if (old && (!typeCompatible(old.type, parameter.type) ||
          old.required !== true && parameter.required === true)) {
        changes.push(changeRow({
          ...context,
          kind: 'request-parameter-narrowed',
          severity: 'breaking',
          beforeSummary: `${group}.${name}: ${old.type}${old.required ? ' required' : ' optional'}`,
          afterSummary: `${group}.${name}: ${parameter.type}${parameter.required ? ' required' : ' optional'}`,
          evidence: [`request parameter ${name} became incompatible or required`],
        }))
      } else if (old.type !== parameter.type || old.required !== parameter.required) {
        changes.push(changeRow({
          ...context,
          kind: 'request-parameter-compatible',
          severity: 'compatible',
          beforeSummary: `${group}.${name}: ${old.type}${old.required ? ' required' : ' optional'}`,
          afterSummary: `${group}.${name}: ${parameter.type}${parameter.required ? ' required' : ' optional'}`,
          evidence: [`request parameter ${name} changed compatibly`],
        }))
      }
    }
    for (const [name, parameter] of before) {
      if (after.has(name)) continue
      changes.push(changeRow({
        ...context,
        kind: 'request-parameter-removed',
        severity: 'potentially-breaking',
        beforeSummary: `${group}.${name}: ${parameter.type}${parameter.required ? ' required' : ' optional'}`,
        afterSummary: `${group}.${name} was removed`,
        evidence: [`previously accepted ${group} parameter ${name} was removed`],
      }))
    }
  }
  return changes
}

function compareBodyAndResponses(context, oldEndpoint, nextEndpoint) {
  const changes = []
  const oldBody = oldEndpoint.request && oldEndpoint.request.body
  const nextBody = nextEndpoint.request && nextEndpoint.request.body
  if (!!oldBody !== !!nextBody) {
    changes.push(changeRow({
      ...context,
      kind: nextBody ? 'request-body-added' : 'request-body-removed',
      severity: 'breaking',
      modelId: (nextBody && nextBody.schemaRef) || (oldBody && oldBody.schemaRef) || null,
      beforeSummary: oldBody ? 'request body was accepted' : 'request had no modeled body',
      afterSummary: nextBody ? 'request body is now required by the contract' : 'request body was removed',
      evidence: [nextBody ? 'request body was added' : 'previously accepted request body was removed'],
    }))
  } else if (oldBody && nextBody &&
      (oldBody.contentType || null) !== (nextBody.contentType || null)) {
    changes.push(changeRow({
      ...context,
      kind: 'request-body-content-type-changed',
      severity: 'potentially-breaking',
      modelId: nextBody.schemaRef || oldBody.schemaRef || null,
      beforeSummary: `request content type: ${oldBody.contentType || 'unspecified'}`,
      afterSummary: `request content type: ${nextBody.contentType || 'unspecified'}`,
      evidence: ['accepted request body content type changed'],
    }))
  }
  const oldResponses = oldEndpoint.response || {}
  const nextResponses = nextEndpoint.response || {}
  for (const status of Object.keys(oldResponses)) {
    if (!Object.prototype.hasOwnProperty.call(nextResponses, status)) continue
    const before = oldResponses[status] || {}
    const after = nextResponses[status] || {}
    if ((before.schemaRef || null) !== (after.schemaRef || null)) {
      changes.push(changeRow({
        ...context,
        kind: 'response-schema-reference-changed',
        severity: 'breaking',
        modelId: after.schemaRef || before.schemaRef || null,
        beforeSummary: `${status} response model: ${before.schemaRef || 'none'}`,
        afterSummary: `${status} response model: ${after.schemaRef || 'none'}`,
        evidence: [`documented ${status} response model changed`],
      }))
    }
    if ((before.array === true) !== (after.array === true)) {
      changes.push(changeRow({
        ...context,
        kind: 'response-shape-changed',
        severity: 'breaking',
        modelId: after.schemaRef || before.schemaRef || null,
        beforeSummary: `${status} response was ${before.array ? 'an array' : 'a single value'}`,
        afterSummary: `${status} response is ${after.array ? 'an array' : 'a single value'}`,
        evidence: [`documented ${status} response changed between a single value and an array`],
      }))
    }
  }
  return changes
}

function compareEndpoint(context, oldEndpoint, nextEndpoint, previousAreas, nextAreas) {
  const changes = []
  const auth = authStrength(oldEndpoint.auth, nextEndpoint.auth)
  if (auth !== 'same') {
    changes.push(changeRow({
      ...context,
      kind: 'auth-requirement-changed',
      severity: auth === 'relaxed' ? 'compatible' : 'breaking',
      beforeSummary: `auth: ${oldEndpoint.auth || 'none'}`,
      afterSummary: `auth: ${nextEndpoint.auth || 'none'}`,
      evidence: [`authentication requirement ${auth}`],
    }))
  }
  changes.push(...compareParameters(context, oldEndpoint, nextEndpoint))
  changes.push(...compareBodyAndResponses(context, oldEndpoint, nextEndpoint))
  const oldStatuses = new Set(Object.keys(oldEndpoint.response || {}).concat(oldEndpoint.errors || []))
  const nextStatuses = new Set(Object.keys(nextEndpoint.response || {}).concat(nextEndpoint.errors || []))
  const statusRelation = setRelation(oldStatuses, nextStatuses)
  if (statusRelation !== 'same') {
    changes.push(changeRow({
      ...context,
      kind: 'status-code-set-changed',
      severity: 'potentially-breaking',
      beforeSummary: `statuses: ${[...oldStatuses].sort().join(', ') || 'none'}`,
      afterSummary: `statuses: ${[...nextStatuses].sort().join(', ') || 'none'}`,
      evidence: [
        `documented response status code set ${statusRelation}`,
      ],
    }))
  }
  changes.push(...compareModels(context, oldEndpoint, nextEndpoint, previousAreas, nextAreas))
  const oldDocumentation = {
    area: oldEndpoint.area,
    summary: oldEndpoint.summary,
    deprecated: oldEndpoint.deprecated,
    examples: oldEndpoint.examples,
  }
  const nextDocumentation = {
    area: nextEndpoint.area,
    summary: nextEndpoint.summary,
    deprecated: nextEndpoint.deprecated,
    examples: nextEndpoint.examples,
  }
  if (!changes.length &&
      JSON.stringify(stable(oldDocumentation)) !== JSON.stringify(stable(nextDocumentation))) {
    changes.push(changeRow({
      ...context,
      kind: 'documentation-changed',
      severity: 'compatible',
      beforeSummary: oldEndpoint.summary || 'Endpoint documentation changed',
      afterSummary: nextEndpoint.summary || 'Endpoint documentation changed',
      evidence: ['description, example, deprecation, or other non-shape metadata changed'],
    }))
  }
  return changes
}

export function classifyChanges(options) {
  const previousInventory = options.previousInventory || null
  const nextInventory = options.nextInventory || { endpoints: [] }
  const previousAreas = options.previousAreas || Object.create(null)
  const nextAreas = options.nextAreas || Object.create(null)
  const previousHash = options.previousHash || null
  const currentHash = options.currentHash
  const environmentId = options.environmentId
  const generatedAt = options.generatedAt || new Date().toISOString()
  const beforeList = previousInventory && Array.isArray(previousInventory.endpoints)
    ? previousInventory.endpoints : []
  const afterList = Array.isArray(nextInventory.endpoints) ? nextInventory.endpoints : []
  const beforeByOperation = new Map(beforeList.map((endpoint) => [endpoint.operationId, endpoint]))
  const afterByOperation = new Map(afterList.map((endpoint) => [endpoint.operationId, endpoint]))
  const beforeByRoute = new Map(beforeList.map((endpoint) => [routeKey(endpoint), endpoint]))
  const afterByRoute = new Map(afterList.map((endpoint) => [routeKey(endpoint), endpoint]))
  const changes = []
  const analysis = { modelGraphTruncated: false }

  if (!previousInventory) {
    for (const endpoint of afterList) {
      changes.push(changeRow({
        previousHash,
        currentHash,
        kind: 'endpoint-added',
        severity: 'compatible',
        operationId: endpoint.operationId,
        beforeSummary: null,
        afterSummary: routeKey(endpoint),
        evidence: ['endpoint added while establishing the first baseline'],
      }))
    }
  } else {
    for (const oldEndpoint of beforeList) {
      const sameOperation = afterByOperation.get(oldEndpoint.operationId)
      const sameRoute = afterByRoute.get(routeKey(oldEndpoint))
      if (sameOperation && routeKey(sameOperation) !== routeKey(oldEndpoint)) {
        const aliasChanges = sameRoute && sameRoute !== sameOperation
          ? compareEndpoint({
            previousHash,
            currentHash,
            operationId: sameRoute.operationId,
            analysis,
          }, oldEndpoint, sameRoute, previousAreas, nextAreas)
          : []
        const compatibleAlias = !!sameRoute && !aliasChanges.some((row) =>
          row.severity === 'breaking' || row.severity === 'potentially-breaking')
        changes.push(changeRow({
          previousHash,
          currentHash,
          kind: compatibleAlias
            ? 'endpoint-route-changed-with-compatible-alias'
            : 'endpoint-route-changed',
          severity: compatibleAlias ? 'compatible' : 'breaking',
          operationId: oldEndpoint.operationId,
          beforeSummary: routeKey(oldEndpoint),
          afterSummary: routeKey(sameOperation),
          evidence: [compatibleAlias
            ? 'method or path changed while a shape-compatible previous route remains'
            : 'method or path changed without a compatible alias'],
        }))
      }
      if (!sameOperation && sameRoute && sameRoute.operationId !== oldEndpoint.operationId) {
        changes.push(changeRow({
          previousHash,
          currentHash,
          kind: 'operation-id-changed',
          severity: 'potentially-breaking',
          operationId: sameRoute.operationId,
          beforeSummary: oldEndpoint.operationId,
          afterSummary: sameRoute.operationId,
          evidence: [`operation id changed for ${routeKey(oldEndpoint)}`],
        }))
      }
      if (!sameOperation && !sameRoute) {
        changes.push(changeRow({
          previousHash,
          currentHash,
          kind: 'endpoint-removed',
          severity: 'breaking',
          operationId: oldEndpoint.operationId,
          beforeSummary: routeKey(oldEndpoint),
          afterSummary: null,
          evidence: ['contract endpoint was removed'],
        }))
        continue
      }
      const nextEndpoint = sameOperation || sameRoute
      changes.push(...compareEndpoint({
        previousHash,
        currentHash,
        operationId: nextEndpoint.operationId,
        analysis,
      }, oldEndpoint, nextEndpoint, previousAreas, nextAreas))
    }
    for (const endpoint of afterList) {
      if (beforeByOperation.has(endpoint.operationId) || beforeByRoute.has(routeKey(endpoint))) continue
      changes.push(changeRow({
        previousHash,
        currentHash,
        kind: 'endpoint-added',
        severity: 'compatible',
        operationId: endpoint.operationId,
        beforeSummary: null,
        afterSummary: routeKey(endpoint),
        evidence: ['contract endpoint was added'],
      }))
    }
  }

  const deduped = []
  const seen = new Set()
  for (const change of changes) {
    if (seen.has(change.id)) continue
    seen.add(change.id)
    deduped.push(change)
  }
  deduped.sort((left, right) => {
    const severity = SEVERITY_RANK[right.severity] - SEVERITY_RANK[left.severity]
    return severity || String(left.operationId || left.modelId || left.id)
      .localeCompare(String(right.operationId || right.modelId || right.id)) ||
      left.kind.localeCompare(right.kind)
  })
  const countBounded = deduped.slice(0, CHANGE_COUNT_MAX)
  const capped = []
  let changeBytes = 2
  for (const row of countBounded) {
    const rowBytes = Buffer.byteLength(JSON.stringify(row), 'utf8') + 1
    if (changeBytes + rowBytes > CHANGE_BYTES_MAX) break
    capped.push(row)
    changeBytes += rowBytes
  }
  const summary = {
    breaking: capped.filter((row) => row.severity === 'breaking').length,
    potentiallyBreaking: capped.filter((row) => row.severity === 'potentially-breaking').length,
    compatible: capped.filter((row) => row.severity === 'compatible').length,
    info: capped.filter((row) => row.severity === 'info').length,
    total: capped.length,
  }
  const changeSetId = 'changes-' + hash({
    classifierVersion: CLASSIFIER_VERSION,
    environmentId,
    previousHash,
    currentHash,
    changes: capped.map((row) => row.id),
  }).slice('sha256:'.length, 'sha256:'.length + 24)
  return {
    schemaVersion: 2,
    classifierVersion: CLASSIFIER_VERSION,
    changeSetId,
    environmentId,
    previousHash,
    currentHash,
    generatedAt,
    summary,
    changes: capped,
    limitations: (previousInventory ? [] : ['baseline-established'])
      .concat(deduped.length > CHANGE_COUNT_MAX ? ['change-count-cap'] : [])
      .concat(capped.length < countBounded.length ? ['change-byte-cap'] : [])
      .concat(analysis.modelGraphTruncated ? ['model-graph-cap'] : []),
  }
}

export const _test = {
  stable,
  setRelation,
  typeCompatible,
  authStrength,
  endpointModels,
  reachableModels,
}
