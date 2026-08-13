'use strict';

// Narrow compare-and-swap updater for orchestrator/project-config.md. It edits
// one allowlisted scalar in-place without reformatting the remaining
// frontmatter or Markdown body. Callers receive a capability, not a generic
// frontmatter patch surface.

var crypto = require('crypto');
var fs = require('fs');
var path = require('path');
var paths = require('./paths');
var fileGuards = require('./file-guards');
var finalizations = require('./finalizations');

var MAX_BYTES = 1024 * 1024;
var READ_ERRORS = Object.freeze({
  'project-config-unsafe': 1,
  'project-config-frontmatter-missing': 1,
  'project-config-frontmatter-malformed': 1,
  'project-config-duplicate-key': 1
});
var CAPABILITIES = {
  figma: { fields: { figmaLibraryUrl: validateFigmaLibraryUrl } },
  reviewer: { fields: { codexEnabled: validateCodexEnabled } }
};

function sha(bytes) {
  return 'sha256:' + crypto.createHash('sha256').update(bytes).digest('hex');
}
function readErrorCode(error) {
  var code = error && typeof error.message === 'string' ? error.message : '';
  return Object.prototype.hasOwnProperty.call(READ_ERRORS, code)
    ? code : 'project-config-unavailable';
}

function exactObject(value) {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function validateCodexEnabled(value) {
  return value === 'auto' || value === 'true' || value === 'false' ? value : null;
}

function normalizeFigmaInput(value) {
  if (typeof value !== 'string' || /[\x00-\x1f\x7f]/.test(value)) return null;
  var raw = value.trim();
  if (!raw || raw.length > 500) return null;
  if (/^[A-Za-z0-9]{8,200}$/.test(raw)) return {
    key: raw,
    url: 'https://www.figma.com/design/' + raw
  };
  var url;
  try { url = new URL(raw); } catch (error) { return null; }
  if (url.protocol !== 'https:' || url.username || url.password || url.port || !/^(www\.)?figma\.com$/i.test(url.hostname)) return null;
  var match = /^\/(?:design|file)\/([A-Za-z0-9]+)(?:\/[^?#]*)?$/.exec(url.pathname);
  if (!match || match[1].length < 8 || match[1].length > 200) return null;
  var canonical = 'https://www.figma.com/design/' + match[1];
  var nodeIds = url.searchParams.getAll('node-id');
  if (nodeIds.length > 1) return null;
  var nodeId = nodeIds.length ? nodeIds[0] : null;
  if (nodeId) {
    if (!/^[0-9]+[:-][0-9]+$/.test(nodeId)) return null;
    canonical += '?node-id=' + encodeURIComponent(nodeId.replace(':', '-'));
  }
  return { key: match[1], url: canonical };
}

function validateFigmaLibraryUrl(value) {
  var normalized = normalizeFigmaInput(value);
  return normalized ? normalized.url : null;
}

function stableRegularFile(file) {
  var parent = path.dirname(file);
  var hit = fileGuards.boundedRegularFileUnder(paths.PROJECT_ROOT, parent, file, MAX_BYTES);
  if (!hit || !hit.stat || String(hit.stat.nlink) !== '1') throw new Error('project-config-unsafe');
  return hit;
}

function stableRegularBytes(file) { return stableRegularFile(file).bytes; }

function splitDocument(text) {
  var bom = text.charAt(0) === '\uFEFF' ? '\uFEFF' : '';
  var body = bom ? text.slice(1) : text;
  var newline = body.indexOf('\r\n') >= 0 ? '\r\n' : '\n';
  if (body.slice(0, 3) !== '---') throw new Error('project-config-frontmatter-missing');
  var close = body.indexOf(newline + '---', 3);
  if (close < 0) throw new Error('project-config-frontmatter-malformed');
  var closeEnd = close + newline.length + 3;
  var tail = body.slice(closeEnd);
  if (tail && tail.slice(0, newline.length) !== newline) throw new Error('project-config-frontmatter-malformed');
  return {
    bom: bom,
    newline: newline,
    prefix: body.slice(0, 3 + newline.length),
    frontmatter: body.slice(3 + newline.length, close),
    suffix: body.slice(close)
  };
}

function scalarRows(frontmatter, key) {
  var escaped = key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  var lines = frontmatter.split(/\r?\n/);
  var rows = [];
  var pattern = new RegExp('^(' + escaped + ')([ \\t]*:[ \\t]*)(.*)$');
  for (var i = 0; i < lines.length; i++) {
    var match = pattern.exec(lines[i]);
    if (match) {
      var tail = match[3];
      var commentAt = -1;
      for (var c = 0; c < tail.length; c++) {
        if (tail[c] === '#' && (c === 0 || /[ \t]/.test(tail[c - 1]))) {
          commentAt = c;
          break;
        }
      }
      var commentStart = commentAt;
      while (commentStart > 0 && /[ \t]/.test(tail[commentStart - 1])) commentStart--;
      rows.push({
        index: i,
        match: match,
        value: commentAt < 0 ? tail : tail.slice(0, commentStart),
        comment: commentAt < 0 ? '' : (commentStart === 0 ? ' ' : '') + tail.slice(commentStart)
      });
    }
  }
  return { lines: lines, rows: rows };
}

function read() {
  try {
    var inspected = fileGuards.inspectEntryUnder(paths.PROJECT_ROOT, path.dirname(paths.PROJECT_CONFIG_FILE), paths.PROJECT_CONFIG_FILE);
    if (inspected && inspected.status === 'missing') {
      return {
        ok: true, revision: null, figmaLibraryUrl: null, figmaFileKey: null,
        figmaFieldState: 'missing', hasFigmaField: false,
        productPackage: null, figmaEnabled: false, figmaEnabledState: 'missing',
        codexEnabled: null, codexFieldState: 'missing', hasCodexField: false
      };
    }
    var bytes = stableRegularBytes(paths.PROJECT_CONFIG_FILE);
    var text = bytes.toString('utf8');
    var doc = splitDocument(text);
    var figmaRows = scalarRows(doc.frontmatter, 'figmaLibraryUrl').rows;
    if (figmaRows.length > 1) throw new Error('project-config-duplicate-key');
    var packageRows = scalarRows(doc.frontmatter, 'productPackage').rows;
    if (packageRows.length > 1) throw new Error('project-config-duplicate-key');
    var figmaEnabledRows = scalarRows(doc.frontmatter, 'figmaEnabled').rows;
    if (figmaEnabledRows.length > 1) throw new Error('project-config-duplicate-key');
    var codexRows = scalarRows(doc.frontmatter, 'codexEnabled').rows;
    if (codexRows.length > 1) throw new Error('project-config-duplicate-key');
    var raw = figmaRows.length ? figmaRows[0].value.trim() : null;
    var placeholder = raw !== null && /^<[^>]+>$/.test(raw);
    var normalized = raw && !placeholder ? normalizeFigmaInput(raw) : null;
    var figmaFieldState = normalized ? 'selected' : (!raw || placeholder ? 'missing' : 'invalid');
    var rawPackage = packageRows.length ? packageRows[0].value.trim() : null;
    var productPackage = rawPackage && !/^<[^>]+>$/.test(rawPackage) &&
      /^[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)+$/.test(rawPackage)
      ? rawPackage : null;
    var rawFigmaEnabled = figmaEnabledRows.length ? figmaEnabledRows[0].value.trim() : null;
    var figmaEnabledState = rawFigmaEnabled === 'true' || rawFigmaEnabled === 'false'
      ? 'selected' : (!rawFigmaEnabled || /^<[^>]+>$/.test(rawFigmaEnabled) ? 'missing' : 'invalid');
    var rawCodex = codexRows.length ? codexRows[0].value.trim() : null;
    var codexEnabled = validateCodexEnabled(rawCodex);
    var codexFieldState = codexEnabled ? 'selected' : (!rawCodex || /^<[^>]+>$/.test(rawCodex) ? 'missing' : 'invalid');
    return {
      ok: true,
      revision: sha(bytes),
      figmaLibraryUrl: normalized ? normalized.url : null,
      figmaFileKey: normalized ? normalized.key : null,
      figmaFieldState: figmaFieldState,
      hasFigmaField: figmaRows.length === 1,
      productPackage: productPackage,
      figmaEnabled: figmaEnabledState === 'selected' && rawFigmaEnabled === 'true',
      figmaEnabledState: figmaEnabledState,
      codexEnabled: codexEnabled,
      codexFieldState: codexFieldState,
      hasCodexField: codexRows.length === 1
    };
  } catch (error) {
    return { ok: false, error: readErrorCode(error), revision: null };
  }
}

function update(request) {
  if (!exactObject(request)) return { ok: false, status: 400, error: 'bad-config-update' };
  var keys = Object.keys(request).sort();
  if (keys.join('\0') !== ['capability', 'expectedRevision', 'field', 'value'].sort().join('\0')) {
    return { ok: false, status: 400, error: 'bad-config-update-fields' };
  }
  var capability = CAPABILITIES[request.capability];
  var validator = capability && capability.fields[request.field];
  if (!validator) return { ok: false, status: 403, error: 'config-field-forbidden' };
  if (!/^sha256:[a-f0-9]{64}$/.test(String(request.expectedRevision || ''))) {
    return { ok: false, status: 400, error: 'bad-config-revision' };
  }
  var normalized = validator(request.value);
  if (normalized === null) return { ok: false, status: 400, error: 'config-value-invalid' };

  var before, originalMode;
  try {
    var beforeHit = stableRegularFile(paths.PROJECT_CONFIG_FILE);
    before = beforeHit.bytes;
    originalMode = Number(BigInt(beforeHit.stat.modeExact) & 0o777n);
  }
  catch (readError) { return { ok: false, status: 409, error: readErrorCode(readError) }; }
  if (sha(before) !== request.expectedRevision) {
    return { ok: false, status: 409, error: 'project-config-revision-conflict', currentRevision: sha(before) };
  }
  var doc;
  try { doc = splitDocument(before.toString('utf8')); }
  catch (parseError) { return { ok: false, status: 409, error: readErrorCode(parseError) }; }
  var located = scalarRows(doc.frontmatter, request.field);
  if (located.rows.length > 1) {
    return { ok: false, status: 409, error: 'project-config-duplicate-key' };
  }
  if (!located.rows.length) {
    if (request.capability !== 'reviewer' || request.field !== 'codexEnabled') {
      return { ok: false, status: 409, error: 'project-config-field-missing' };
    }
    var insertAt = located.lines.length;
    while (insertAt > 0 && located.lines[insertAt - 1] === '') insertAt--;
    located.lines.splice(insertAt, 0, request.field + ': ' + normalized);
  } else {
    var row = located.rows[0];
    located.lines[row.index] = row.match[1] + row.match[2] + normalized + row.comment;
  }
  var nextText = doc.bom + doc.prefix + located.lines.join(doc.newline) + doc.suffix;
  var nextBytes = Buffer.from(nextText, 'utf8');
  if (nextBytes.length > MAX_BYTES) return { ok: false, status: 413, error: 'project-config-size-limit' };

  var lease = finalizations.beginMutation({
    kind: 'site-config',
    key: 'project-config:' + request.capability + ':' + request.field,
    pendingChild: false,
    requireSoleWriter: true
  });
  if (!lease.ok) return { ok: false, status: 409, error: lease.error, detail: lease.detail || '' };
  var after = null;
  var operationError = null;
  try {
    var current = stableRegularBytes(paths.PROJECT_CONFIG_FILE);
    if (sha(current) !== request.expectedRevision) {
      operationError = {
        ok: false,
        status: 409,
        error: 'project-config-revision-conflict',
        currentRevision: sha(current)
      };
    } else {
      var published = fileGuards.atomicReplaceRegularFileResult(
        paths.PROJECT_ROOT,
        path.dirname(paths.PROJECT_CONFIG_FILE),
        paths.PROJECT_CONFIG_FILE,
        nextBytes,
        { create: false, mode: originalMode, maxBytes: MAX_BYTES }
      );
      if (!published.ok) {
        operationError = {
          ok: false,
          status: 500,
          error: 'project-config-write-failed'
        };
      } else {
        after = read();
        var afterValue = after && after.ok
          ? (request.field === 'figmaLibraryUrl' ? after.figmaLibraryUrl : after.codexEnabled)
          : null;
        if (!after || !after.ok || after.revision !== sha(nextBytes) || afterValue !== normalized) {
          operationError = {
            ok: false,
            status: 500,
            error: 'project-config-postcondition-failed'
          };
        }
      }
    }
  } catch (writeError) {
    operationError = {
      ok: false,
      status: 500,
      error: 'project-config-write-failed'
    };
  }
  if (!finalizations.endMutation(lease.handle)) {
    return {
      ok: false,
      status: 503,
      error: 'writer-lease-release-failed',
      currentRevision: after && after.ok ? after.revision : null
    };
  }
  if (operationError) return operationError;
  return {
    ok: true,
    status: 200,
    revision: after.revision,
    field: request.field,
    value: request.field === 'figmaLibraryUrl' ? after.figmaLibraryUrl : after.codexEnabled
  };
}

function clearFigmaLibraryUrl(expectedRevision, outerLease) {
  if (expectedRevision !== null && !/^sha256:[a-f0-9]{64}$/.test(String(expectedRevision || ''))) {
    return { ok: false, status: 400, error: 'bad-config-revision' };
  }
  var beforeState = read();
  if (!beforeState.ok) return { ok: false, status: 409, error: beforeState.error || 'project-config-unavailable' };
  if (beforeState.revision !== expectedRevision) {
    return { ok: false, status: 409, error: 'project-config-revision-conflict', currentRevision: beforeState.revision };
  }
  // A project without a config file has no binding to clear.
  if (beforeState.revision === null) return { ok: true, status: 200, revision: null, field: 'figmaLibraryUrl', value: null };
  // Older/custom configs may omit the optional field entirely. Absence is
  // already the required post-reset state; do not reformat or inject it.
  if (!beforeState.hasFigmaField) {
    return { ok: true, status: 200, revision: beforeState.revision, field: 'figmaLibraryUrl', value: null };
  }

  var before, originalMode, doc, located;
  try {
    var beforeHit = stableRegularFile(paths.PROJECT_CONFIG_FILE);
    before = beforeHit.bytes;
    originalMode = Number(BigInt(beforeHit.stat.modeExact) & 0o777n);
    if (sha(before) !== expectedRevision) {
      return { ok: false, status: 409, error: 'project-config-revision-conflict', currentRevision: sha(before) };
    }
    doc = splitDocument(before.toString('utf8'));
    located = scalarRows(doc.frontmatter, 'figmaLibraryUrl');
  } catch (readError) {
    return { ok: false, status: 409, error: readErrorCode(readError) };
  }
  if (located.rows.length > 1) return { ok: false, status: 409, error: 'project-config-duplicate-key' };
  if (!located.rows.length) return { ok: false, status: 409, error: 'project-config-field-missing' };
  var row = located.rows[0];
  located.lines[row.index] = row.match[1] + row.match[2] + '<figma-library-url>' + row.comment;
  var nextBytes = Buffer.from(doc.bom + doc.prefix + located.lines.join(doc.newline) + doc.suffix, 'utf8');
  if (nextBytes.length > MAX_BYTES) return { ok: false, status: 413, error: 'project-config-size-limit' };

  var lease = outerLease || finalizations.beginMutation({
    kind: 'site-config', key: 'project-config:figma:figmaLibraryUrl',
    pendingChild: false, requireSoleWriter: true
  });
  var ownsLease = !outerLease;
  if (!lease || lease.ok === false) {
    return { ok: false, status: 409, error: lease && lease.error || 'writer-lease-unavailable', detail: lease && lease.detail || '' };
  }
  var handle = ownsLease ? lease.handle : lease;
  var result;
  try {
    var current = stableRegularBytes(paths.PROJECT_CONFIG_FILE);
    if (sha(current) !== expectedRevision) {
      result = { ok: false, status: 409, error: 'project-config-revision-conflict', currentRevision: sha(current) };
    } else if (current.equals(nextBytes)) {
      result = { ok: true, status: 200 };
    } else {
      var published = fileGuards.atomicReplaceRegularFileResult(
        paths.PROJECT_ROOT, path.dirname(paths.PROJECT_CONFIG_FILE), paths.PROJECT_CONFIG_FILE,
        nextBytes, { create: false, mode: originalMode, maxBytes: MAX_BYTES }
      );
      result = published.ok ? { ok: true, status: 200 } : { ok: false, status: 500, error: 'project-config-write-failed' };
    }
  } catch (writeError) {
    result = { ok: false, status: 500, error: 'project-config-write-failed' };
  }
  if (ownsLease && !finalizations.endMutation(handle)) {
    return { ok: false, status: 503, error: 'writer-lease-release-failed' };
  }
  if (!result.ok) return result;
  var after = read();
  if (!after.ok || after.figmaFieldState !== 'missing' || after.figmaLibraryUrl !== null || after.revision !== sha(nextBytes)) {
    return { ok: false, status: 500, error: 'project-config-postcondition-failed' };
  }
  return { ok: true, status: 200, revision: after.revision, field: 'figmaLibraryUrl', value: null };
}

module.exports = {
  MAX_BYTES: MAX_BYTES,
  normalizeFigmaInput: normalizeFigmaInput,
  read: read,
  update: update,
  clearFigmaLibraryUrl: clearFigmaLibraryUrl
};
