'use strict';

// Canonical, migration-free metadata embedded in API work-package tasks.
// This shared task-layer contract is consumed by creators, lifecycle
// validation and site projections so package aliases cannot drift or disappear
// as a task moves between columns.

var crypto = require('crypto');
var designParser = require('../figma/scripts/design-parser.cjs');

var SCHEMA_VERSION = 1;
var MAX_SOURCES = 2048;
var PACKAGE_ID_RE = /^pkg-[a-f0-9]{24}$/;
var GROUP_KEY_RE = /^(?:area|model|operation|change-set|mixed):[a-z0-9][a-z0-9-]{0,63}$/;
var SOURCE_ID_RE = /^(?:api:missing:[A-Za-z0-9][A-Za-z0-9._:-]{0,199}|api:change:chg-[a-f0-9]{24}|api:mismatch:mismatch-[a-f0-9]{24})$/;
var FIELDS = ['groupKey', 'packageId', 'schemaVersion', 'sourceIds'];

function exact(value, keys) {
  return !!value && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}

function hash(value) {
  return crypto.createHash('sha256').update(String(value), 'utf8').digest('hex');
}

function normalizedSourceIds(value) {
  if (!Array.isArray(value) || value.length < 2 || value.length > MAX_SOURCES ||
      value.some(function (sourceId) {
        return !SOURCE_ID_RE.test(String(sourceId || ''));
      }) ||
      new Set(value).size !== value.length) return null;
  var sorted = value.slice().sort();
  return sorted.every(function (sourceId, index) {
    return sourceId === value[index];
  }) ? sorted : null;
}

function packageIdFor(groupKey, sourceIds) {
  return 'pkg-' + hash(JSON.stringify({
    schemaVersion: SCHEMA_VERSION,
    groupKey: groupKey,
    sourceIds: sourceIds
  })).slice(0, 24);
}

function normalize(value) {
  if (!exact(value, FIELDS) || value.schemaVersion !== SCHEMA_VERSION ||
      !GROUP_KEY_RE.test(String(value.groupKey || '')) ||
      !PACKAGE_ID_RE.test(String(value.packageId || ''))) return null;
  var sourceIds = normalizedSourceIds(value.sourceIds);
  if (!sourceIds ||
      value.packageId !== packageIdFor(value.groupKey, sourceIds)) return null;
  return {
    schemaVersion: SCHEMA_VERSION,
    packageId: value.packageId,
    groupKey: value.groupKey,
    sourceIds: sourceIds
  };
}

function create(groupKey, sourceIds) {
  var sorted = Array.isArray(sourceIds) ? sourceIds.slice().sort() : sourceIds;
  var value = {
    schemaVersion: SCHEMA_VERSION,
    packageId: packageIdFor(groupKey, sorted || []),
    groupKey: groupKey,
    sourceIds: sorted
  };
  var clean = normalize(value);
  if (!clean) {
    throw Object.assign(new Error('API work package metadata is invalid'), {
      code: 'api-work-package-invalid'
    });
  }
  return clean;
}

function render(value) {
  var clean = normalize(value);
  if (!clean) {
    throw Object.assign(new Error('API work package metadata is invalid'), {
      code: 'api-work-package-invalid'
    });
  }
  return [
    '## API Work Package',
    '',
    '```json',
    JSON.stringify(clean),
    '```'
  ].join('\n');
}

function parse(markdown) {
  if (typeof markdown !== 'string') {
    return {
      present: false,
      valid: false,
      issue: 'api-work-package-text-invalid'
    };
  }
  var text = markdown
    .replace(/^\uFEFF/, '')
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n');
  var headings = designParser
    .scanAtxHeadings(text, 2)
    .headings
    .filter(function (heading) {
      return heading.name === 'API Work Package';
    });
  if (!headings.length) {
    return {
      present: false,
      valid: false,
      issue: 'api-work-package-missing'
    };
  }
  if (headings.length !== 1 || markdown !== text) {
    return {
      present: true,
      valid: false,
      issue: 'api-work-package-malformed'
    };
  }
  var tail = text.slice(headings[0].start);
  var match = /^## API Work Package\n\n```json\n([^\n]+)\n```(?=\n\n|\n?$)/.exec(tail);
  if (!match) {
    return {
      present: true,
      valid: false,
      issue: 'api-work-package-malformed'
    };
  }
  var value;
  try {
    value = JSON.parse(match[1]);
  } catch (error) {
    return {
      present: true,
      valid: false,
      issue: 'api-work-package-json-invalid'
    };
  }
  var clean = normalize(value);
  if (!clean || JSON.stringify(clean) !== match[1]) {
    return {
      present: true,
      valid: false,
      issue: 'api-work-package-invalid'
    };
  }
  return {
    present: true,
    valid: true,
    value: clean,
    block: match[0],
    start: headings[0].start,
    end: headings[0].start + match[0].length
  };
}

module.exports = Object.freeze({
  SCHEMA_VERSION: SCHEMA_VERSION,
  MAX_SOURCES: MAX_SOURCES,
  PACKAGE_ID_RE: PACKAGE_ID_RE,
  SOURCE_ID_RE: SOURCE_ID_RE,
  create: create,
  normalize: normalize,
  render: render,
  parse: parse
});
