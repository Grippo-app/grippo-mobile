'use strict';

// Pure, zero-I/O validation shared by API-contract producers and Site readers.
// Keeping this beside the report producers prevents command-line analyzers from
// importing the Site/task runtime (or reaching through a private `_test` export).

var path = require('path');

var HASH_RE = /^sha256:[a-f0-9]{64}$/;
var GENERATION_RE = /^gen-[0-9]{8}T[0-9]{6}Z-[a-f0-9]{12}$/;

function plain(value) {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}
function exact(value, keys) {
  return plain(value) &&
    Object.keys(value).sort().join('\0') === keys.slice().sort().join('\0');
}
function safeString(value, maximum) {
  if (typeof value !== 'string') return null;
  var clean = value.normalize('NFC')
    .replace(/[\x00-\x1f\x7f-\x9f\u2028\u2029]+/g, ' ')
    .replace(/\s+/g, ' ').trim();
  return clean ? Array.from(clean).slice(0, maximum || 500).join('') : null;
}
function boundedLine(value, maximum) {
  return typeof value === 'string' && value.length > 0 && value.length <= maximum &&
    safeString(value, maximum) === value;
}
function boundedLines(value, maximumItems, maximumLength) {
  return Array.isArray(value) && value.length <= maximumItems &&
    value.every(function (row) { return boundedLine(row, maximumLength); });
}
function safePath(value) {
  return boundedLine(value, 500) && value.indexOf('\\') < 0 &&
    !path.posix.isAbsolute(value) && !/^[A-Za-z]:/.test(value) &&
    value.split('/').every(function (part) {
      return !!part && part !== '.' && part !== '..';
    });
}
function projectSummary(value) {
  return safeString(value, 500);
}
function validDrift(report) {
  if (!exact(report, [
    'analyzerVersion', 'checkedAt', 'committedGenerationId', 'contractHash',
    'environmentId', 'findings', 'limitations', 'projectCodeRevision',
    'schemaVersion', 'specHash', 'summary'
  ]) || report.schemaVersion !== 1 ||
      !Number.isFinite(Date.parse(report.checkedAt)) ||
      !GENERATION_RE.test(String(report.committedGenerationId || '')) ||
      !HASH_RE.test(String(report.contractHash || '')) ||
      !HASH_RE.test(String(report.projectCodeRevision || '')) ||
      (report.specHash !== null && !HASH_RE.test(String(report.specHash || ''))) ||
      !boundedLine(report.environmentId, 100) ||
      !boundedLine(report.analyzerVersion, 100) ||
      !boundedLines(report.limitations, 50, 100) ||
      !exact(report.summary, ['errors', 'infos', 'warnings']) ||
      !Array.isArray(report.findings) || report.findings.length > 10000) return false;
  var counts = { errors: 0, warnings: 0, infos: 0 };
  if (!report.findings.every(function (finding) {
    if (!exact(finding, [
      'area', 'dtoFile', 'field', 'kind', 'message', 'operationId',
      'schemaRef', 'severity', 'suggestion'
    ]) || ['ERROR', 'WARNING', 'INFO'].indexOf(finding.severity) < 0 ||
        !boundedLine(finding.kind, 100) || !boundedLine(finding.message, 1000) ||
        (finding.area !== null && !boundedLine(finding.area, 100)) ||
        (finding.schemaRef !== null && !boundedLine(finding.schemaRef, 200)) ||
        (finding.operationId !== null && !boundedLine(finding.operationId, 200)) ||
        (finding.field !== null && !boundedLine(finding.field, 200)) ||
        (finding.dtoFile !== null && !safePath(finding.dtoFile)) ||
        (finding.suggestion !== null && !boundedLine(finding.suggestion, 1000))) return false;
    counts[finding.severity === 'ERROR'
      ? 'errors' : finding.severity === 'WARNING' ? 'warnings' : 'infos']++;
    return true;
  })) return false;
  return Object.keys(counts).every(function (key) {
    return Number.isSafeInteger(report.summary[key]) && report.summary[key] === counts[key];
  });
}
function currentDrift(report, context) {
  return validDrift(report) && plain(context) &&
    report.analyzerVersion === context.analyzerVersion &&
    report.committedGenerationId === context.committedGenerationId &&
    report.contractHash === context.contractHash &&
    report.environmentId === context.environmentId &&
    report.projectCodeRevision === context.projectCodeRevision &&
    report.specHash === context.specHash;
}
function completeDrift(report) {
  return validDrift(report) && !report.limitations.some(function (row) {
    return row === 'drift-finding-byte-cap' || row === 'drift-finding-count-cap';
  });
}
function driftCountsByArea(report) {
  if (!validDrift(report)) return null;
  var counts = Object.create(null);
  report.findings.forEach(function (finding) {
    if ((finding.severity === 'ERROR' || finding.severity === 'WARNING') && finding.area) {
      counts[finding.area] = (counts[finding.area] || 0) + 1;
    }
  });
  return counts;
}

module.exports = {
  completeDrift: completeDrift,
  currentDrift: currentDrift,
  driftCountsByArea: driftCountsByArea,
  projectSummary: projectSummary,
  validDrift: validDrift
};
