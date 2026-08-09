'use strict';

// ---------------------------------------------------------------------------
// HTTP surface: JSON helpers, the request-body reader (with the 256 KB cap),
// the setup-value sanitizer, every /api/* handler, and the top-level request
// dispatch. Exports a single `handle(req, res)` the entry (server.js) wires
// into http.createServer.
//
// Security guards preserved verbatim: path-traversal confinement (static.js
// safeResolve), the 256 KB body cap, the setup-key allow-list, and the
// id/stem regex guards in locks/requests.
// ---------------------------------------------------------------------------

var fs     = require('fs');
var crypto = require('crypto');
var Readable = require('stream').Readable;
var paths  = require('./paths');

var state       = require('./state');
var persistence = require('./persistence');
var validators  = require('./validators');
var locksMod    = require('./locks');
var requestsMod = require('./requests');
var sse         = require('./sse');
var runner      = require('./runner');
var cliMod      = require('./cli');
var figmaMod    = require('./figma');
var figmaSessionActionsMod = require('./figma-session-actions');
var figmaScreensMod = require('./figma-screens');
var figmaEvidenceMod = require('./figma-evidence');
var figmaIntegrationMod = require('./figma-integration');
var figmaTestJobMod = require('./figma-test-job');
var figmaSyncMod = require('./figma-sync');
var figmaTaskPublicationMod = require('./figma-task-publication');
var figmaSyncHistoryMod = require('./figma-sync-history');
var designCatalogMod = require('./design-catalog');
var designMappingsMod = require('./design-mappings');
var designTokenSourcesMod = require('./design-token-sources');
var designComponentMappingsMod = require('./design-component-mappings');
var designOverviewMod = require('./design-overview');
var designHistoryMod = require('./design-history');
var designTaskActionsMod = require('./design-task-actions');
var designComparisonMod = require('./design-comparison');
var tasksLogMod     = require('./tasks-log');
var archMod     = require('./arch');
var architectureGenerationMod = require('./architecture-generation');
var architectureTaskActionsMod = require('./architecture-task-actions');
var sessionsMod = require('./sessions');
var staticMod   = require('./static');
var gitMod      = require('./git');
var finalizationsMod = require('./finalizations');
var pixelReviewMod = require('./pixel-review');
var backlogCreateMod = require('./backlog-create');
var taskInboxMod = require('./task-inbox');
var shallowIntakeMod = require('./shallow-intake');
var taskIntegrityMod = require('./task-integrity');
var backendIntegrationMod = require('./backend-integration');
var backendEnvironmentsMod = require('./backend-environments');
var backendCredentialsMod = require('./backend-credentials');
var contractJobMod = require('./contract-job');
var contractHistoryMod = require('./contract-history');
var reviewerMod = require('./reviewer');
var taskSourceMod = require('./task-source');
var taskSummaryMod = require('./task-summary');
var taskActionsMod = require('./task-actions');
var taskFilesMod = require('./task-files');
var taskDetailsMod = require('./task-details');
var taskActivityMod = require('./task-activity');
var taskArtifactsMod = require('./task-artifacts');
var taskCheckpointsMod = require('./task-checkpoints');
var taskActionPromptPreviewMod = require('./task-action-prompt-preview');
var taskActionReceiptsMod = require('./task-action-receipts');
var apiOverviewMod = require('./api-overview');
var apiCatalogMod = require('./api-catalog');
var apiChangesMod = require('./api-changes');
var apiChangeReviewsMod = require('./api-change-reviews');
var apiTaskActionsMod = require('./api-task-actions');
var apiMockMod = require('./api-mock');
var appRunnerMod = require('./app-runner');

var ORCHESTRATOR_DIR = paths.ORCHESTRATOR_DIR;
var REQUESTS_DIR     = paths.REQUESTS_DIR;
var STEP_VALIDATORS  = validators.STEP_VALIDATORS;
// App-run schemas used to live at the orchestrator root. Keep those URLs, and
// the paths declared by their stable $id values, readable while the canonical
// files live with the Site feature contracts.
var APP_RUN_SCHEMA_ALIASES = Object.freeze({
  '/app-run.schema.json': '/site/contracts/app-run/config.schema.json',
  '/app-run-job.schema.json': '/site/contracts/app-run/job.schema.json',
  '/app-run-validation.schema.json': '/site/contracts/app-run/validation-receipt.schema.json',
  '/schemas/app-run.schema.json': '/site/contracts/app-run/config.schema.json',
  '/schemas/app-run-job.schema.json': '/site/contracts/app-run/job.schema.json',
  '/schemas/app-run-validation.schema.json': '/site/contracts/app-run/validation-receipt.schema.json'
});
// Manual-override keys the Setup panel's escape-hatch posts (in addition to the
// wizard step ids above) — one per FS setup gate, OR-combined back into each
// gate in state.js deriveState. Whitelisted here so the /api/state-patch merge
// doesn't silently drop them. MUST match the keys state.js reads.
var SETUP_GATE_OVERRIDE_KEYS = {
  'setup:requirementsVerified': 1,
  'setup:yamlPasted': 1,
  'setup:agentsInstalled': 1
};
var validTaskStem    = locksMod.validTaskStem;
var REQUEST_ACTIONS  = requestsMod.REQUEST_ACTIONS;
var REQUEST_PROMPT_MAX = requestsMod.REQUEST_PROMPT_MAX;
var deriveState      = state.deriveState;
var CSRF_TOKEN       = crypto.randomBytes(24).toString('hex');

function jsonResponse(res, status, obj) {
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'cache-control': 'no-store'
  });
  res.end(JSON.stringify(obj));
}

function hostNameOnly(value) {
  var host = String(value || '').trim().toLowerCase();
  if (!host || /[,\s\/@\\]/.test(host)) return '';
  function validPort(suffix) {
    if (!suffix) return true;
    if (!/^:\d{1,5}$/.test(suffix)) return false;
    var port = Number(suffix.slice(1));
    return port >= 1 && port <= 65535;
  }
  if (host.charAt(0) === '[') {
    var close = host.indexOf(']');
    if (close < 0 || !validPort(host.slice(close + 1))) return '';
    return host.slice(1, close);
  }
  var colon = host.lastIndexOf(':');
  if (colon >= 0) {
    if (host.indexOf(':') !== colon || !validPort(host.slice(colon))) return '';
    host = host.slice(0, colon);
  }
  return host;
}

function isLocalHost(value) {
  var h = hostNameOnly(value);
  return h === 'localhost' || h === '127.0.0.1' || h === '::1';
}

function rejectMutation(req, res, code, error) {
  jsonResponse(res, code, { error: error });
  return false;
}

function validateMutationRequest(req, res) {
  if (req.method !== 'POST') return true;
  if (!isLocalHost(req.headers.host || '')) return rejectMutation(req, res, 403, 'bad-host');
  var origin = req.headers.origin;
  if (origin) {
    try {
      var o = new URL(origin);
      if (!isLocalHost(o.host)) return rejectMutation(req, res, 403, 'bad-origin');
    } catch (e) { return rejectMutation(req, res, 403, 'bad-origin'); }
  }
  var site = String(req.headers['sec-fetch-site'] || '').toLowerCase();
  if (site && site !== 'same-origin' && site !== 'same-site' && site !== 'none') return rejectMutation(req, res, 403, 'cross-site');
  if (!/^application\/json(?:;|$)/i.test(String(req.headers['content-type'] || ''))) return rejectMutation(req, res, 415, 'json-required');
  if (String(req.headers['x-orchestrator-csrf'] || '') !== CSRF_TOKEN) return rejectMutation(req, res, 403, 'bad-csrf');
  return true;
}

// 256 KB. Localhost-only dev tool. Sized so the largest realistic payload — an
// `answers` request whose prompt embeds a filled questions sidecar — fits in ANY
// locale: REQUEST_PROMPT_MAX is 60000 CHARS, which is ~120 KB as 2-byte UTF-8
// (Cyrillic) plus the JSON envelope, so a 64 KB cap rejected valid non-ASCII
// prompts with a misleading "body too large". The per-prompt char limit
// (REQUEST_PROMPT_MAX) remains the meaningful gate.
var MAX_BODY_BYTES = 262144;

function readJsonBody(req) {
  return new Promise(function (resolve, reject) {
    var chunks = [];
    var total = 0;
    var done = false;
    req.on('data', function (c) {
      if (done) return;
      total += c.length;
      if (total > MAX_BODY_BYTES) {
        done = true;
        // Keep the socket alive long enough to deliver a structured 413. The
        // request is already in flowing mode; the guarded data listener drains
        // remaining bytes without buffering them.
        chunks = [];
        req.resume();
        var tooLarge = new Error('body too large');
        tooLarge.code = 'bad-json'; tooLarge.httpStatus = 413;
        reject(tooLarge);
        return;
      }
      chunks.push(c);
    });
    req.on('end', function () {
      if (done) return;
      done = true;
      try {
        var txt = Buffer.concat(chunks).toString('utf8');
        resolve(txt ? JSON.parse(txt) : {});
      } catch (e) {
        e.code = 'bad-json'; e.httpStatus = 400;
        reject(e);
      }
    });
    req.on('error', function (e) {
      if (done) return;
      done = true;
      reject(e);
    });
  });
}

// Allow-list for body.setup keys. Any other key is silently dropped.
// Values must be string (max SETUP_STRING_MAX chars, no control chars — incl.
// tab/newline/CR, which would inject extra lines into the YAML frontmatter built
// from these values), boolean, or array.
// One shared key/type contract with persistence: adding or retiring a Setup
// field cannot make the write path and the durable-state validator drift.
var SETUP_KEY_WHITELIST = persistence.SETUP_KEY_TYPES;
var SETUP_STRING_MAX = 200;
var SETUP_ARRAY_MAX = 20;

function sanitizeSetupValue(key, value) {
  var expectedType = SETUP_KEY_WHITELIST[key];
  if (!expectedType) return undefined;
  // screenshotPixelGate is a three-value string {strict,advisory,off}; the server is the authority.
  if (key === 'screenshotPixelGate') return (value === 'strict' || value === 'advisory' || value === 'off') ? value : undefined;
  if (expectedType === 'boolean') return value === true ? true : value === false ? false : undefined;
  if (expectedType === 'string') {
    if (typeof value !== 'string') return undefined;
    // Reject NUL and ALL control chars, including tab/newline/CR: no setup field
    // is legitimately multi-line, and a newline injects extra YAML frontmatter lines.
    if (/[\x00-\x1F\x7F]/.test(value)) return undefined;
    if (value.length > SETUP_STRING_MAX) return undefined;
    return value;
  }
  if (expectedType === 'array') {
    if (!Array.isArray(value)) return undefined;
    if (value.length > SETUP_ARRAY_MAX) return undefined;
    var validated = [];
    for (var i = 0; i < value.length; i++) {
      var el = value[i];
      // Skip individual invalid elements rather than rejecting the whole array,
      // so a single bad entry from a future client version doesn't silently
      // discard valid locale/auth selections the user intended to save.
      if (typeof el !== 'string') continue;
      if (el.length > SETUP_STRING_MAX) continue;
      if (/[\x00-\x1F\x7F]/.test(el)) continue;
      validated.push(el);
    }
    return validated;
  }
  return undefined;
}

function handleState(req, res) {
  var snap = deriveState();
  snap.csrfToken = CSRF_TOKEN;
  jsonResponse(res, 200, snap);
}

function reviewerResponse(res, result) {
  jsonResponse(res, result && result.status || (result && result.ok ? 200 : 500),
    result || { ok: false, error: 'reviewer-unavailable' });
}

function handleReviewerStatus(req, res) {
  reviewerMod.status(false).then(function (result) {
    jsonResponse(res, 200, result);
  }, function () {
    jsonResponse(res, 500, { error: 'reviewer-unavailable' });
  });
}

function handleReviewerSettings(req, res) {
  readJsonBody(req).then(function (body) {
    return reviewerMod.settings(body).then(function (result) {
      if (result.ok) sse.pollLoop();
      reviewerResponse(res, result);
    }, function () {
      jsonResponse(res, 500, { error: 'reviewer-settings-failed' });
    });
  }, function () {
    jsonResponse(res, 400, { error: 'bad-json' });
  });
}

function handleReviewerRecheck(req, res) {
  readJsonBody(req).then(function (body) {
    if (!body || typeof body !== 'object' || Array.isArray(body) || Object.keys(body).length) {
      reviewerResponse(res, { ok: false, status: 400, error: 'bad-reviewer-recheck' });
      return;
    }
    return reviewerMod.recheck().then(function (result) {
      if (result.ok) sse.pollLoop();
      reviewerResponse(res, result);
    }, function () {
      jsonResponse(res, 500, { error: 'reviewer-unavailable' });
    });
  }, function () {
    jsonResponse(res, 400, { error: 'bad-json' });
  });
}

function handleReviewerActivity(req, res, url) {
  var allowed = { state: true, cursor: true, limit: true };
  var invalid = false;
  url.searchParams.forEach(function (_, key) {
    if (!allowed[key] || url.searchParams.getAll(key).length !== 1) invalid = true;
  });
  var rawLimit = url.searchParams.get('limit');
  var rawState = url.searchParams.get('state');
  var rawCursor = url.searchParams.get('cursor');
  if (invalid || (rawState !== null && !rawState) || (rawCursor !== null && !rawCursor) ||
      (rawLimit !== null && !/^[1-9][0-9]{0,2}$/.test(rawLimit))) {
    jsonResponse(res, 400, { ok: false, error: 'bad-activity-query' });
    return;
  }
  var limit = rawLimit === null ? undefined : Number(rawLimit);
  var result = reviewerMod.activity(
    rawState || 'all',
    rawCursor,
    limit
  );
  jsonResponse(res, result.ok ? 200 : 400, result);
}

function handleFigmaIntegration(req, res) {
  jsonResponse(res, 200, { integration: figmaIntegrationMod.get() });
}

function handleFigmaIntegrationReset(req, res) {
  readJsonBody(req).then(function (body) {
    return sse.serializeStateWrite(function () { return figmaIntegrationMod.reset(body); });
  }, function () {
    return { ok: false, status: 400, error: 'bad-json' };
  }).then(function (result) {
    if (result.ok) { sse.broadcast('figma-integration', { changed: true }); sse.pollLoop(); }
    figmaJobResponse(res, result);
  }, function () {
    jsonResponse(res, 500, { ok: false, error: 'internal' });
  });
}

function figmaJobResponse(res, result) {
  jsonResponse(res, result && result.status || (result && result.ok ? 200 : 500), result || { ok: false, error: 'internal' });
}

function handleFigmaTest(req, res) {
  readJsonBody(req).then(function (body) { figmaJobResponse(res, figmaTestJobMod.start(body)); }, function (error) {
    jsonResponse(res, error.httpStatus || 400, { error: 'bad-json' });
  });
}

function handleFigmaFileVerify(req, res) {
  readJsonBody(req).then(function (body) { figmaJobResponse(res, figmaTestJobMod.verifyCandidate(body)); }, function (error) {
    jsonResponse(res, error.httpStatus || 400, { error: 'bad-json' });
  });
}

function handleFigmaFileSave(req, res) {
  readJsonBody(req).then(function (body) {
    var result = figmaTestJobMod.saveCandidate(body);
    if (!result.ok) { figmaJobResponse(res, result); return; }
    sse.pollLoop();
    jsonResponse(res, 200, { ok: true, revision: result.revision, integration: figmaIntegrationMod.get() });
  }, function (error) { jsonResponse(res, error.httpStatus || 400, { error: 'bad-json' }); });
}

function handleFigmaSyncPlan(req, res) {
  readJsonBody(req).then(function (body) { figmaJobResponse(res, figmaSyncMod.plan(body)); }, function (error) {
    jsonResponse(res, error.httpStatus || 400, { error: 'bad-json' });
  });
}

function handleFigmaSyncStart(req, res) {
  readJsonBody(req).then(function (body) { figmaJobResponse(res, figmaSyncMod.start(body)); }, function (error) {
    jsonResponse(res, error.httpStatus || 400, { error: 'bad-json' });
  });
}

function handleFigmaSyncCancel(req, res) {
  readJsonBody(req).then(function (body) { figmaJobResponse(res, figmaSyncMod.cancel(body)); }, function (error) {
    jsonResponse(res, error.httpStatus || 400, { error: 'bad-json' });
  });
}

function handleFigmaSyncJob(req, res, url) {
  var match = /^\/api\/figma\/sync\/jobs\/(fsj-[a-f0-9]{32})$/.exec(url.pathname);
  if (!match) { jsonResponse(res, 400, { error: 'bad-sync-job-id' }); return; }
  var job = figmaSyncMod.get(match[1]);
  if (!job) { jsonResponse(res, 404, { error: 'sync-job-not-found' }); return; }
  jsonResponse(res, 200, { job: job });
}

function handleFigmaSyncHistory(req, res, url) {
  var rawLimit = url.searchParams.get('limit');
  if (rawLimit !== null && !/^(?:[1-9]|[1-9][0-9]|100)$/.test(rawLimit)) {
    jsonResponse(res, 400, { ok: false, error: 'bad-limit' }); return;
  }
  var limit = rawLimit === null ? 20 : Number(rawLimit);
  var result = figmaSyncHistoryMod.list(url.searchParams.get('cursor'), limit);
  if (result.ok) result = {
    ok: true,
    items: result.items.map(figmaSyncHistoryMod.publicRecord).filter(Boolean),
    nextCursor: result.nextCursor
  };
  jsonResponse(res, result.ok ? 200 : 400, result);
}

function designQuery(url) {
  var out = Object.create(null);
  var allowed = Object.assign(Object.create(null), {
    query: 1, type: 1, status: 1, theme: 1, platform: 1, locale: 1,
    changed: 1, changedSide: 1, mappingState: 1, hasTask: 1, cursor: 1, limit: 1,
    expectedGenerationRevision: 1, variantOffset: 1, variantLimit: 1
  });
  url.searchParams.forEach(function (value, key) {
    if (!Object.prototype.hasOwnProperty.call(allowed, key) ||
        Object.prototype.hasOwnProperty.call(out, key)) {
      out.__invalidQuery = key;
      return;
    }
    out[key] = value;
  });
  if (out.changed === 'true') {
    var latestDesignHistory = designHistoryMod.latest();
    out._changedGenerationId = latestDesignHistory && latestDesignHistory.generationId || null;
    out._changedIds = designHistoryMod.changedIds(out._changedGenerationId);
  }
  return out;
}
function exactSearchParams(url, keys) {
  var seen = Object.create(null), valid = true;
  url.searchParams.forEach(function (_value, key) {
    if (keys.indexOf(key) < 0 || seen[key]) valid = false;
    seen[key] = true;
  });
  return valid && keys.every(function (key) { return seen[key]; });
}
function designResponse(res, result) {
  jsonResponse(res, result && result.status || (result && result.ok ? 200 : 500),
    result || { ok: false, error: 'design-unavailable' });
}
function handleDesignOverview(req, res, url) {
  designResponse(res, designOverviewMod.get(designQuery(url)));
}
function handleDesignTokens(req, res, url) {
  designResponse(res, designComparisonMod.attach(designCatalogMod.listTokens(designQuery(url)), 'tokens'));
}
function handleDesignProjectOnlyTokens(req, res, url) {
  designResponse(res, designComparisonMod.attach(designCatalogMod.listProjectOnlyTokens(designQuery(url)), 'tokens'));
}
function tokenSourceQuery(url) {
  var out = Object.create(null), allowed = {
    query: 1, status: 1, cursor: 1, limit: 1, expectedGenerationRevision: 1
  };
  url.searchParams.forEach(function (value, key) {
    if (!allowed[key] || Object.prototype.hasOwnProperty.call(out, key)) out.__invalidQuery = key;
    else out[key] = value;
  });
  return out;
}
function handleDesignTokenSources(req, res, url) {
  designResponse(res, designTokenSourcesMod.list(tokenSourceQuery(url)));
}
function handleDesignTokenSourcesMutate(req, res) {
  readJsonBody(req).then(function (body) {
    return designTokenSourcesMod.mutate(body).then(function (result) {
      if (result && result.ok) {
        sse.broadcast('design-overview', { changed: true });
        sse.pollLoop();
      }
      designResponse(res, result);
    }, function (error) {
      jsonResponse(res, 500, { ok: false, error: designTokenSourcesMod.publicErrorCode(error) });
    });
  }, function () {
    jsonResponse(res, 400, { ok: false, error: 'bad-json' });
  });
}
function handleDesignTokenDetail(req, res, url) {
  var match = /^\/api\/design\/tokens\/((?:tok|tokp)-[a-f0-9]{24})$/.exec(url.pathname);
  if (!match) { jsonResponse(res, 400, { ok: false, error: 'bad-token-id' }); return; }
  designResponse(res, designCatalogMod.tokenDetail(match[1], designQuery(url)));
}
function handleDesignTokenMappingsGet(req, res) {
  designMappingsMod.get().then(function (result) {
    designResponse(res, result);
  }, function (error) {
    jsonResponse(res, 500, { ok: false, error: 'token-mapping-read-failed' });
  });
}
function handleDesignTokenMappingsMutate(req, res) {
  readJsonBody(req).then(function (body) {
    return designMappingsMod.mutate(body).then(function (result) {
      if (result && result.ok) {
        sse.broadcast('design-overview', { changed: true });
        sse.pollLoop();
      }
      designResponse(res, result);
    }, function (error) {
      jsonResponse(res, 500, { ok: false, error: designMappingsMod.publicErrorCode(error) });
    });
  }, function () {
    jsonResponse(res, 400, { ok: false, error: 'bad-json' });
  });
}
function handleDesignComponentMappingsGet(req, res) {
  designComponentMappingsMod.get().then(function (result) {
    designResponse(res, result);
  }, function (error) {
    jsonResponse(res, 500, { ok: false, error: 'component-mapping-read-failed' });
  });
}
function handleDesignComponentMappingsMutate(req, res) {
  readJsonBody(req).then(function (body) {
    return designComponentMappingsMod.mutate(body).then(function (result) {
      if (result && result.ok) {
        sse.broadcast('design-overview', { changed: true });
        sse.pollLoop();
      }
      designResponse(res, result);
    }, function (error) {
      jsonResponse(res, 500, { ok: false, error: designComponentMappingsMod.publicErrorCode(error) });
    });
  }, function () {
    jsonResponse(res, 400, { ok: false, error: 'bad-json' });
  });
}
function handleDesignComponents(req, res, url) {
  designResponse(res, designComparisonMod.attach(designCatalogMod.listComponents(designQuery(url)), 'components'));
}
function handleDesignProjectOnlyComponents(req, res, url) {
  designResponse(res, designComparisonMod.attach(designCatalogMod.listProjectOnlyComponents(designQuery(url)), 'components'));
}
function handleDesignComponent(req, res, url) {
  var match = /^\/api\/design\/components\/((?:cmp|cmpp)-[a-f0-9]{24})$/.exec(url.pathname);
  if (!match) { jsonResponse(res, 400, { ok: false, error: 'bad-component-id' }); return; }
  designResponse(res, designCatalogMod.componentDetail(match[1], designQuery(url), designHistoryMod));
}
function handleDesignComponentImage(req, res, url) {
  if (!exactSearchParams(url, ['componentId', 'image', 'expectedGenerationRevision']) ||
      !/^cmp-[a-f0-9]{24}$/.test(String(url.searchParams.get('componentId') || '')) ||
      !/^[a-f0-9]{32}$/.test(String(url.searchParams.get('image') || ''))) {
    jsonResponse(res, 400, { ok: false, error: 'bad-design-image-request' }); return;
  }
  var expectedRevision = url.searchParams.get('expectedGenerationRevision');
  if (!expectedRevision || !/^sha256:[a-f0-9]{64}$/.test(expectedRevision)) {
    jsonResponse(res, 400, { ok: false, error: 'bad-design-image-revision' }); return;
  }
  var result = designCatalogMod.componentImage(
    url.searchParams.get('componentId'),
    url.searchParams.get('image'),
    expectedRevision
  );
  if (!result.ok) { designResponse(res, result); return; }
  staticMod.sendFile(res, result.file);
}
function handleDesignSurfaces(req, res, url) {
  designResponse(res, designCatalogMod.listSurfaces(designQuery(url)));
}
function handleDesignSurface(req, res, url) {
  var match = /^\/api\/design\/surfaces\/(srf-[a-f0-9]{24})$/.exec(url.pathname);
  if (!match) { jsonResponse(res, 400, { ok: false, error: 'bad-surface-id' }); return; }
  designResponse(res, designCatalogMod.surfaceDetail(match[1], designQuery(url), designHistoryMod));
}
function handleDesignSurfaceImage(req, res, url) {
  if (!exactSearchParams(url, ['surfaceId', 'variantId', 'expectedGenerationRevision']) ||
      !/^srf-[a-f0-9]{24}$/.test(String(url.searchParams.get('surfaceId') || '')) ||
      !/^var-[a-f0-9]{24}$/.test(String(url.searchParams.get('variantId') || ''))) {
    jsonResponse(res, 400, { ok: false, error: 'bad-design-image-request' }); return;
  }
  var expectedRevision = url.searchParams.get('expectedGenerationRevision');
  if (!expectedRevision || !/^sha256:[a-f0-9]{64}$/.test(expectedRevision)) {
    jsonResponse(res, 400, { ok: false, error: 'bad-design-image-revision' }); return;
  }
  var result = designCatalogMod.surfaceImage(
    url.searchParams.get('surfaceId'),
    url.searchParams.get('variantId'),
    expectedRevision
  );
  if (!result.ok) { designResponse(res, result); return; }
  staticMod.sendFile(res, result.file);
}
function handleDesignTaskPreview(req, res) {
  readJsonBody(req).then(function (body) {
    designResponse(res, designTaskActionsMod.preview(body));
  }, function (error) {
    jsonResponse(res, error.httpStatus || 400, { ok: false, error: 'bad-json' });
  });
}
function handleDesignTaskCreate(req, res) {
  readJsonBody(req).then(function (body) {
    return designTaskActionsMod.create(body).then(function (result) {
      if (result && (result.status === 200 || result.status === 207)) {
        sse.broadcast('design-overview', { changed: true });
        sse.pollLoop();
      }
      designResponse(res, result);
    }, function () {
      jsonResponse(res, 500, { ok: false, error: 'design-task-create-failed' });
    });
  }, function () {
    jsonResponse(res, 400, { ok: false, error: 'bad-json' });
  });
}
function handleDesignTaskCancel(req, res) {
  readJsonBody(req).then(function (body) {
    designResponse(res, designTaskActionsMod.cancel(body));
  }, function (error) {
    jsonResponse(res, error.httpStatus || 400, { ok: false, error: 'bad-json' });
  });
}
function handleDesignComparison(req, res) {
  readJsonBody(req).then(function (body) {
    designResponse(res, designComparisonMod.start(body));
  }, function (error) {
    jsonResponse(res, error.httpStatus || 400, { ok: false, error: 'bad-json' });
  });
}

// POST /api/figma/pixel-review {stem, screen, theme?, verdict: pass|minor|fail, note?} — the
// Owner's one-click verdict for a REVIEW_REQUIRED screenshot row. Every hash binding is
// derived SERVER-SIDE from the sealed report + artifacts on disk (pixel-review.js); the final
// evidence bundle re-validates the receipt, so a crafted body can at worst record a verdict
// for pixels that really exist. CSRF-guarded like every mutation.
function handleFigmaPixelReview(req, res) {
  readJsonBody(req).then(function (body) {
    var stem = body && typeof body.stem === 'string' ? body.stem : '';
    var screen = body && typeof body.screen === 'string' ? body.screen.trim() : '';
    var theme = body && typeof body.theme === 'string' ? body.theme : 'primary';
    var verdict = body && typeof body.verdict === 'string' ? body.verdict : '';
    var note = body && typeof body.note === 'string' ? body.note.replace(/[\x00-\x1f\x7f]+/g, ' ') : '';
    if (!validTaskStem(stem)) { jsonResponse(res, 400, { error: 'bad-stem' }); return; }
    if (!screen || screen.length > 200) { jsonResponse(res, 400, { error: 'bad-screen' }); return; }
    pixelReviewMod.applyVerdict(stem, screen, theme, verdict, note, function (err, out) {
      if (err && err.code === 'bad-request') { jsonResponse(res, 400, { error: 'bad-verdict' }); return; }
      if (err && err.code === 'not-reviewable') { jsonResponse(res, 409, { error: 'not-reviewable' }); return; }
      if (err) { jsonResponse(res, 500, { error: 'pixel-review-failed' }); return; }
      jsonResponse(res, 200, out);
    });
  }, function (err) {
    jsonResponse(res, 400, { error: 'bad-body' });
  });
}


// GET /api/figma/screens?stem=<stem> — the per-task screen-cache summary for the
// task modal's Screens section (the figma:screens cache under
// .cache/figma/screens/<stem>/). Read-only local JSON; never calls Figma.
// { present:false } until a pull has populated the cache.
function handleFigmaScreens(req, res, url) {
  var result = figmaScreensMod.screensIndex(url.searchParams.get('stem'));
  if (result.error) {
    jsonResponse(res, result.error === 'bad-stem' ? 400 : 409, { ok: false, error: result.error });
    return;
  }
  jsonResponse(res, 200, result);
}

// GET /api/figma/ship-drift?stem=<stem> — the committed post-ship drift-auto-stale marker
// (orchestrator/tasks/evidence/figma-ship/<stem>/drift-stale-<stem>.json, written by
// sweep-done-drift.mjs). Read-only local JSON; never calls Figma. { present:false } until a
// sweep has marked this SHIPPED task's certificate stale (its Figma design moved since ship).
function handleFigmaShipDrift(req, res, url) {
  var result = figmaScreensMod.shipDrift(url.searchParams.get('stem'));
  if (result.error) {
    jsonResponse(res, result.error === 'bad-stem' ? 400 : 409, { ok: false, error: result.error });
    return;
  }
  jsonResponse(res, 200, result);
}

// GET /api/figma/evidence?stem=<stem> — compact, read-only summary of the
// local Figma gate reports for one task. The browser passes only a task stem;
// figma-evidence synthesizes the known report filenames under .cache and never
// exposes arbitrary file streaming.
function handleFigmaEvidence(req, res, url) {
  jsonResponse(res, 200, figmaEvidenceMod.readEvidence(url.searchParams.get('stem')));
}

// GET /api/figma/screen-image?stem=<stem>&screen=<ScreenName> — streams one
// cached screenshot PNG. screen is whitelisted against the task's index.json
// (figma-screens.screenImageFile), so the filename is never user-controlled; any
// miss is a plain 404.
function handleFigmaScreenImage(req, res, url) {
  var theme = url.searchParams.get('theme');
  var file = figmaScreensMod.screenImageFile(
    url.searchParams.get('stem'),
    url.searchParams.get('screen'),
    theme === 'dark' ? 'dark' : null
  );
  if (!file) { res.writeHead(404); res.end('not found'); return; }
  staticMod.sendFile(res, file);
}

// GET /api/figma/compare-artifact?stem=<stem>&id=<artifact-id>&reportHash=<sha256> — streams one
// vetted screenshot-compare artifact PNG. The browser never supplies a file
// path; figma-evidence re-reads screenshot-<stem>.json, maps id -> artifact,
// verifies the report hash plus artifact hash/confinement under
// .cache/figma/artifacts/screenshot, then returns the single PNG.
function handleFigmaCompareArtifact(req, res, url) {
  var artifact = figmaEvidenceMod.compareArtifactFile(url.searchParams.get('stem'), url.searchParams.get('id'), url.searchParams.get('reportHash'));
  if (!artifact) { res.writeHead(404); res.end('not found'); return; }
  staticMod.sendBuffer(res, artifact.file, artifact.bytes);
}

function architectureResponse(res, result) {
  var status = result && Number.isInteger(result.status)
    ? result.status : result && result.ok === false ? 500 : 200;
  var body = result && typeof result === 'object' ? Object.assign({}, result) : {
    ok: false, error: 'architecture-response-invalid'
  };
  delete body.status;
  var encoded;
  try { encoded = JSON.stringify(body); }
  catch (error) {
    jsonResponse(res, 503, { ok: false, error: 'architecture-response-invalid' });
    return;
  }
  if (Buffer.byteLength(encoded, 'utf8') > 1024 * 1024) {
    jsonResponse(res, 503, { ok: false, error: 'architecture-response-too-large' });
    return;
  }
  jsonResponse(res, status, body);
}

function architectureQuery(url, allowed) {
  var out = {}, valid = true;
  url.searchParams.forEach(function (value, key) {
    if (allowed.indexOf(key) < 0 || url.searchParams.getAll(key).length !== 1) valid = false;
    else out[key] = value;
  });
  if (!valid) throw Object.assign(new Error('architecture query is invalid'), {
    code: 'architecture-filter-invalid', httpStatus: 400
  });
  if (out.limit !== undefined) {
    if (!/^[1-9][0-9]{0,2}$/.test(out.limit) || Number(out.limit) > 500) {
      throw Object.assign(new Error('architecture limit is invalid'), {
        code: 'architecture-filter-invalid', httpStatus: 400
      });
    }
    out.limit = Number(out.limit);
  }
  if (out.changed !== undefined) {
    if (out.changed !== 'true' && out.changed !== 'false') {
      throw Object.assign(new Error('architecture changed filter is invalid'), {
        code: 'architecture-filter-invalid', httpStatus: 400
      });
    }
    out.changed = out.changed === 'true';
  }
  return out;
}

function architectureRead(res, operation) {
  try { architectureResponse(res, operation()); }
  catch (error) {
    jsonResponse(res, Number(error && error.httpStatus) || 503, {
      ok: false,
      error: typeof error.code === 'string' && /^architecture-[a-z0-9-]+$/.test(error.code)
        ? error.code : 'architecture-read-unavailable'
    });
  }
}

function handleArchitectureOverview(req, res) {
  architectureRead(res, archMod.overview);
}

function handleArchitectureNodes(req, res, url) {
  architectureRead(res, function () {
    return archMod.nodes(architectureQuery(url, [
      'search', 'kind', 'platform', 'layer', 'ownership', 'changed', 'cursor', 'limit'
    ]));
  });
}

function handleArchitectureNode(req, res, url, encodedId) {
  architectureRead(res, function () {
    var nodeId;
    try { nodeId = decodeURIComponent(encodedId); }
    catch (error) {
      throw Object.assign(new Error('architecture node id is invalid'), {
        code: 'architecture-node-id-invalid', httpStatus: 400
      });
    }
    if (typeof nodeId !== 'string' || nodeId.length > 180 || !/^[a-z][a-z0-9-]{0,31}:[A-Za-z0-9._~/-]{1,147}$/.test(nodeId)) {
      throw Object.assign(new Error('architecture node id is invalid'), {
        code: 'architecture-node-id-invalid', httpStatus: 400
      });
    }
    return archMod.nodeDetail(nodeId, architectureQuery(url, [
      'incomingCursor', 'outgoingCursor', 'limit'
    ]));
  });
}

function handleArchitectureFindings(req, res, url) {
  architectureRead(res, function () {
    return archMod.findings(architectureQuery(url, [
      'search', 'type', 'severity', 'confidence', 'platform', 'layer',
      'ownership', 'changed', 'cursor', 'limit'
    ]));
  });
}

function handleArchitectureGraph(req, res, url) {
  architectureRead(res, function () {
    return archMod.graph(architectureQuery(url, [
      'search', 'kind', 'platform', 'layer', 'ownership', 'changed'
    ]));
  });
}

function handleArchitectureDiff(req, res, url) {
  architectureRead(res, function () {
    var query = architectureQuery(url, ['scope', 'selector']);
    if (query.scope && ['latest', 'task'].indexOf(query.scope) < 0) {
      throw Object.assign(new Error('architecture diff scope is invalid'), {
        code: 'architecture-filter-invalid', httpStatus: 400
      });
    }
    if (query.selector && (query.selector.length > 180 || /[\x00-\x1f\x7f]/.test(query.selector))) {
      throw Object.assign(new Error('architecture diff selector is invalid'), {
        code: 'architecture-filter-invalid', httpStatus: 400
      });
    }
    return archMod.readDiff(query.scope || 'latest', query.selector || null);
  });
}

function handleArchitectureGenerate(req, res) {
  readJsonBody(req).then(function (body) {
    architectureResponse(res, architectureGenerationMod.start(body));
  }, function (error) {
    jsonResponse(res, Number(error && error.httpStatus) || 400, {
      ok: false, error: 'bad-json'
    });
  }).catch(function () {
    jsonResponse(res, 500, { ok: false, error: 'architecture-generation-failed' });
  });
}

function handleArchitectureJob(req, res, encodedId) {
  var jobId;
  try { jobId = decodeURIComponent(encodedId); } catch (error) { jobId = ''; }
  var job = architectureGenerationMod.get(jobId);
  if (!job) { jsonResponse(res, 404, { ok: false, error: 'architecture-job-not-found' }); return; }
  jsonResponse(res, 200, { ok: true, job: job });
}

function handleArchitectureTaskPreview(req, res) {
  readJsonBody(req).then(function (body) {
    architectureResponse(res, architectureTaskActionsMod.preview(body));
  }, function () {
    jsonResponse(res, 400, { ok: false, error: 'bad-json' });
  });
}

function handleArchitectureTaskCreate(req, res) {
  readJsonBody(req).then(function (body) {
    return architectureTaskActionsMod.create(body).then(function (result) {
      if (result && result.ok) sse.pollLoop();
      architectureResponse(res, result);
    });
  }, function () {
    jsonResponse(res, 400, { ok: false, error: 'bad-json' });
  }).catch(function () {
    jsonResponse(res, 500, { ok: false, error: 'architecture-task-create-failed' });
  });
}

function handleArchitectureTaskCancel(req, res) {
  readJsonBody(req).then(function (body) {
    architectureResponse(res, architectureTaskActionsMod.cancel(body));
  }, function () {
    jsonResponse(res, 400, { ok: false, error: 'bad-json' });
  });
}

// GET /api/tasks/log?stem=TASK_<N>_<title> — the per-task pipeline journal
// (phase timeline) for Task Details → Activity. Delegates to the read-only
// tasks-log reader, which stem-guards + tolerates a partial/missing file.
function handleTasksLog(req, res, url) {
  jsonResponse(res, 200, tasksLogMod.readLog(url.searchParams.get('stem')));
}

function taskSummaryOptions(url) {
  var options = {};
  ['column', 'search', 'origin', 'blocker', 'dependency', 'cursor', 'context', 'sort'].forEach(function (key) {
    var value = url.searchParams.get(key);
    if (value != null && value !== '') options[key] = value;
  });
  if (url.searchParams.get('needsAction') === 'true') options.needsAction = true;
  var limit = url.searchParams.get('limit');
  if (limit != null && limit !== '') options.limit = Number(limit);
  if (options.column && ['backlog', 'pending', 'todo', 'done'].indexOf(options.column) < 0) throw Object.assign(new Error('unsupported column filter'), { code: 'bad-task-summary-filter', httpStatus: 400 });
  if (options.origin && ['manual', 'figma', 'api', 'follow-up'].indexOf(options.origin) < 0) throw Object.assign(new Error('unsupported origin filter'), { code: 'bad-task-summary-filter', httpStatus: 400 });
  if (options.blocker && ['blocked', 'unblocked'].indexOf(options.blocker) < 0) throw Object.assign(new Error('unsupported blocker filter'), { code: 'bad-task-summary-filter', httpStatus: 400 });
  if (options.dependency && ['blocked', 'satisfied', 'none'].indexOf(options.dependency) < 0) throw Object.assign(new Error('unsupported dependency filter'), { code: 'bad-task-summary-filter', httpStatus: 400 });
  if (options.context && ['figma', 'api', 'follow-up'].indexOf(options.context) < 0) throw Object.assign(new Error('unsupported context filter'), { code: 'bad-task-summary-filter', httpStatus: 400 });
  if (options.sort && ['board', 'recent', 'number'].indexOf(options.sort) < 0) throw Object.assign(new Error('unsupported sort mode'), { code: 'bad-task-summary-filter', httpStatus: 400 });
  if (options.search && (options.search.length > 200 || options.search.indexOf('\0') >= 0)) throw Object.assign(new Error('search filter is too long'), { code: 'bad-task-summary-filter', httpStatus: 400 });
  if (options.limit !== undefined && (!Number.isSafeInteger(options.limit) || options.limit < 1 || options.limit > taskSummaryMod.MAX_LIMIT)) throw Object.assign(new Error('summary limit is invalid'), { code: 'bad-task-summary-filter', httpStatus: 400 });
  return options;
}

function handleTasksSummary(req, res, url) {
  try { jsonResponse(res, 200, taskSummaryMod.build(taskSummaryOptions(url))); }
  catch (error) {
    var status = Number(error && error.httpStatus) || (error && error.code === 'task-summary-cursor-stale' ? 409 : 503);
    jsonResponse(res, status, { error: taskSummaryMod.publicErrorCode(error) });
  }
}

function taskDetailStem(pathname, suffix) {
  var pattern = new RegExp('^/api/tasks/(TASK_[1-9][0-9]*_[A-Za-z0-9_]+)/' + suffix + '$');
  var match = pattern.exec(pathname);
  return match ? match[1] : null;
}

function detailQuery(url, allowed) {
  var out = {}, valid = true;
  url.searchParams.forEach(function (value, key) {
    if (allowed.indexOf(key) < 0 || url.searchParams.getAll(key).length !== 1) valid = false;
    else out[key] = value;
  });
  return valid ? out : null;
}

function detailResponse(res, result) {
  var status = result && Number.isInteger(result.status)
    ? result.status : result && result.ok ? 200 : 500;
  if (result && result.ok) {
    var body = Object.assign({}, result);
    delete body.ok;
    delete body.status;
    jsonResponse(res, status, body);
    return;
  }
  jsonResponse(res, status, result || { error: 'task-details-unavailable' });
}

function handleTaskDetails(req, res, url, stem) {
  if (!detailQuery(url, [])) {
    jsonResponse(res, 400, { error: 'bad-task-details-query' });
    return;
  }
  detailResponse(res, taskDetailsMod.build(stem));
}

function handleTaskActivity(req, res, url, stem) {
  var query = detailQuery(url, ['cursor', 'limit']);
  if (!query) { jsonResponse(res, 400, { error: 'bad-task-activity-query' }); return; }
  if (query.limit !== undefined &&
      (!/^[1-9][0-9]*$/.test(query.limit) ||
       Number(query.limit) > taskActivityMod.MAX_LIMIT)) {
    jsonResponse(res, 400, { error: 'bad-task-activity-query' });
    return;
  }
  detailResponse(res, taskActivityMod.build(stem, query));
}

function handleTaskArtifacts(req, res, url, stem) {
  var query = detailQuery(url, ['kind', 'cursor', 'limit']);
  if (!query) { jsonResponse(res, 400, { error: 'bad-task-artifacts-query' }); return; }
  if (query.limit !== undefined &&
      (!/^[1-9][0-9]*$/.test(query.limit) ||
       Number(query.limit) > taskArtifactsMod.MAX_LIMIT)) {
    jsonResponse(res, 400, { error: 'bad-task-artifacts-query' });
    return;
  }
  detailResponse(res, taskArtifactsMod.build(stem, query));
}

function handleTaskAdvanced(req, res, url, stem) {
  var query = detailQuery(url, ['sections']);
  if (!query) { jsonResponse(res, 400, { error: 'bad-task-advanced-query' }); return; }
  var sections = query.sections ? query.sections.split(',') : null;
  if (sections && (query.sections.length > 200 ||
      sections.some(function (item) { return !item; }) ||
      new Set(sections).size !== sections.length)) {
    jsonResponse(res, 400, { error: 'bad-task-advanced-query' });
    return;
  }
  detailResponse(res, taskDetailsMod.advanced(stem, sections));
}

function handleTaskCheckpoints(req, res, url, stem) {
  if (!detailQuery(url, [])) {
    jsonResponse(res, 400, { error: 'bad-task-checkpoints-query' });
    return;
  }
  var body = taskCheckpointsMod.list(stem);
  if (!body.stem) { jsonResponse(res, 400, { error: 'bad-task-stem' }); return; }
  jsonResponse(res, 200, body);
}

function handleTaskActionPrompt(req, res, url, stem) {
  var query = detailQuery(url, ['actionRevision']);
  detailResponse(res, query
    ? taskActionPromptPreviewMod.build(stem, query)
    : { ok: false, status: 400, error: 'bad-action-prompt-query' });
}

function handleTaskAnswerPrompt(req, res, stem) {
  readJsonBody(req).then(function (body) {
    detailResponse(res, taskActionPromptPreviewMod.buildAnswers(stem, body));
  }, function () {
    jsonResponse(res, 400, { error: 'bad-json' });
  }).catch(function () {
    jsonResponse(res, 500, { error: 'task-action-prompt-failed' });
  });
}

function handleTaskRetryPreview(req, res, stem) {
  readJsonBody(req).then(function (body) {
    var summary = taskSummaryMod.single(stem);
    if (!summary) { jsonResponse(res, 404, { error: 'task-not-found' }); return; }
    detailResponse(res, taskCheckpointsMod.preview(stem, body, summary));
  }, function () {
    jsonResponse(res, 400, { error: 'bad-json' });
  }).catch(function () {
    jsonResponse(res, 500, { error: 'retry-preview-failed' });
  });
}

function internalJsonRequest(body) {
  var bytes = Buffer.from(JSON.stringify(body), 'utf8');
  var request = Readable.from([bytes]);
  request.method = 'POST';
  request.headers = { 'content-length': String(bytes.length), 'content-type': 'application/json' };
  return request;
}

function taskActionSuccess(stem, kind, status, requestId, sessionId, replay) {
  var resolved = null;
  try { resolved = taskSummaryMod.single(stem); } catch (error) {}
  var actionRevision = resolved && resolved.task && resolved.task.primaryAction &&
    resolved.task.primaryAction.actionRevision;
  if (!resolved || typeof resolved.revision !== 'string' ||
      typeof actionRevision !== 'string') return null;
  return {
    schemaVersion: 1,
    action: kind,
    status: status,
    requestId: requestId || null,
    sessionId: sessionId || null,
    resultingActionRevision: actionRevision,
    taskSummaryRevision: resolved.revision,
    idempotentReplay: replay === true
  };
}

// Typed Task Details actions expose a deliberately closed success envelope
// without prompt, queue internals, or a whole application state snapshot.
function releaseTaskActionReceipt(res, handle) {
  if (taskActionReceiptsMod.release(handle)) return true;
  jsonResponse(res, 503, { error: 'task-action-idempotency-unverified' });
  return false;
}

function typedQueueResponse(res, stem, kind, receiptHandle) {
  var statusCode = 500;
  var proxy = Object.create(res);
  proxy.writeHead = function (status) { statusCode = status; return proxy; };
  proxy.end = function (bytes) {
    var body = null;
    try { body = JSON.parse(String(bytes || 'null')); } catch (error) {}
    if (statusCode >= 200 && statusCode < 300 && body && typeof body === 'object') {
      var envelope = taskActionSuccess(
        stem, kind, body.deduped ? 'already-active' : 'accepted',
        body.id || null, null, body.deduped === true
      );
      if (!envelope) {
        jsonResponse(res, 503, { error: 'task-action-result-unverified' });
        return;
      }
      if (!taskActionReceiptsMod.complete(receiptHandle, envelope)) {
        jsonResponse(res, 503, { error: 'task-action-idempotency-unverified' });
        return;
      }
      jsonResponse(res, statusCode, envelope);
      return;
    }
    if (!(body && body.error === 'write-durability-unverified')) {
      if (!releaseTaskActionReceipt(res, receiptHandle)) return;
    }
    jsonResponse(res, statusCode, body || { error: 'task-action-response-invalid' });
  };
  return proxy;
}

function handleTaskAction(req, res) {
  readJsonBody(req).then(function (body) {
    var clean = taskActionsMod.validateRequest(body);
    if (!clean) {
      jsonResponse(res, 400, { error: 'bad-task-action-request' });
      return;
    }
    var receipt = taskActionReceiptsMod.reserve(clean);
    if (!receipt.ok) {
      jsonResponse(res, receipt.status || 409, { error: receipt.error });
      return;
    }
    if (receipt.replay) {
      jsonResponse(res, 200, receipt.response);
      return;
    }
    var inspected = taskActionsMod.inspect(body);
    if (!inspected.ok) {
      if (!releaseTaskActionReceipt(res, receipt.handle)) return;
      jsonResponse(res, inspected.status || 409, {
        error: inspected.error,
        currentAction: inspected.currentAction || null
      });
      return;
    }
    if (inspected.operation === 'continue-live') {
      var continued = sessionsMod.continueLive(
        inspected.request.key,
        inspected.request.text,
        inspected.request.sessionId,
        inspected.request.sessionRevision
      );
      if (!continued.sent) {
        if (!releaseTaskActionReceipt(res, receipt.handle)) return;
        jsonResponse(res, 409, {
          error: continued.error || 'recovery-required',
          lockReason: continued.lockReason || null
        });
        return;
      }
      sse.pollLoop();
      var continuedEnvelope = taskActionSuccess(
        clean.stem, clean.kind, 'continued', null,
        inspected.request.sessionId, false
      );
      if (!continuedEnvelope) {
        jsonResponse(res, 503, { error: 'task-action-result-unverified' });
        return;
      }
      if (!taskActionReceiptsMod.complete(receipt.handle, continuedEnvelope)) {
        jsonResponse(res, 503, { error: 'task-action-idempotency-unverified' });
        return;
      }
      jsonResponse(res, 200, continuedEnvelope);
      return;
    }
    if (inspected.operation === 'enqueue') {
      // Reuse the exact queue admission path, but feed it only a server-owned
      // prompt and action derived above. No browser prompt crosses this boundary.
      enqueueServerOwnedTaskRequest(
        internalJsonRequest(inspected.request),
        typedQueueResponse(res, clean.stem, clean.kind, receipt.handle)
      );
      return;
    }
    // The inspector is a closed typed contract. Never interpret a new or
    // malformed operation as queue admission by default.
    if (!releaseTaskActionReceipt(res, receipt.handle)) return;
    jsonResponse(res, 500, { error: 'task-action-operation-invalid' });
  }, function (error) {
    jsonResponse(res, 400, { error: 'bad-json' });
  }).catch(function (error) {
    jsonResponse(res, 500, { error: 'task-action-failed' });
  });
}

function apiResponse(res, result) {
  jsonResponse(res, result && result.status || (result && result.ok ? 200 : 500),
    result || { ok: false, error: 'api-unavailable' });
}

function apiQuery(url, allowed) {
  var out = {}, valid = true;
  url.searchParams.forEach(function (value, key) {
    if (allowed.indexOf(key) < 0 || url.searchParams.getAll(key).length !== 1) valid = false;
    else out[key] = value;
  });
  return valid ? out : null;
}

function decodedApiId(value, maximum) {
  try {
    var decoded = decodeURIComponent(value);
    return decoded && decoded.length <= maximum && !/[\x00-\x1f\x7f]/.test(decoded)
      ? decoded : null;
  } catch (error) { return null; }
}

function handleApiOverview(req, res, url) {
  var query = apiQuery(url, ['expectedGenerationId']);
  apiResponse(res, query ? apiOverviewMod.overview(query.expectedGenerationId) :
    { ok: false, status: 400, error: 'bad-api-query' });
}

function handleApiEndpoints(req, res, url) {
  var query = apiQuery(url, [
    'query', 'area', 'method', 'implementation', 'auth', 'hasTask',
    'changeSeverity', 'mismatch', 'consumers', 'cursor', 'limit',
    'expectedGenerationId'
  ]);
  apiResponse(res, query ? apiCatalogMod.list(query) :
    { ok: false, status: 400, error: 'bad-api-query' });
}

function handleApiEndpoint(req, res, url, encodedId) {
  var query = apiQuery(url, ['expectedGenerationId']);
  var id = decodedApiId(encodedId, 200);
  apiResponse(res, query && id ? apiCatalogMod.detail(id, query.expectedGenerationId) :
    { ok: false, status: 400, error: 'bad-api-endpoint-id' });
}

function handleApiChanges(req, res, url) {
  var query = apiQuery(url, [
    'severity', 'kind', 'query', 'operationId', 'modelId', 'hasTask', 'cursor', 'limit',
    'expectedGenerationId'
  ]);
  apiResponse(res, query ? apiChangesMod.list(query) :
    { ok: false, status: 400, error: 'bad-api-changes-query' });
}

function handleApiChangeReview(req, res) {
  readJsonBody(req).then(function (body) {
    var result = apiChangeReviewsMod.mark(body);
    if (result && result.ok) {
      sse.broadcast('api-overview', { changed: true });
      sse.pollLoop();
    }
    apiResponse(res, result);
  }, function (error) {
    apiResponse(res, { ok: false, status: error.httpStatus || 400, error: 'bad-json' });
  });
}

function handleApiModel(req, res, url, encodedId) {
  var query = apiQuery(url, ['expectedGenerationId']);
  var id = decodedApiId(encodedId, 200);
  apiResponse(res, query && id ? apiCatalogMod.modelDetail(id, query.expectedGenerationId) :
    { ok: false, status: 400, error: 'bad-api-model-id' });
}

function handleApiDiagnostics(req, res, url) {
  var query = apiQuery(url, ['expectedGenerationId', 'sourceId']);
  apiResponse(res, query ? apiCatalogMod.diagnostics(
    query.expectedGenerationId, query.sourceId
  ) :
    { ok: false, status: 400, error: 'bad-api-query' });
}

function handleApiTaskPreview(req, res) {
  readJsonBody(req).then(function (body) {
    apiResponse(res, apiTaskActionsMod.preview(body));
  }, function (error) {
    apiResponse(res, { ok: false, status: error.httpStatus || 400, error: 'bad-json' });
  });
}

function handleApiTaskCreate(req, res) {
  readJsonBody(req).then(function (body) {
    return apiTaskActionsMod.create(body).then(function (result) {
      if (result && (result.status === 200 || result.status === 207)) {
        sse.broadcast('api-overview', { changed: true });
        sse.pollLoop();
      }
      apiResponse(res, result);
    });
  }, function (error) {
    apiResponse(res, { ok: false, status: error.httpStatus || 400, error: 'bad-json' });
  }).catch(function () {
    apiResponse(res, { ok: false, status: 500, error: 'api-task-create-failed' });
  });
}

function handleApiTaskCancel(req, res) {
  readJsonBody(req).then(function (body) {
    apiResponse(res, apiTaskActionsMod.cancel(body));
  }, function (error) {
    apiResponse(res, { ok: false, status: error.httpStatus || 400, error: 'bad-json' });
  });
}

function handleApiMockStatus(req, res, url) {
  var query = apiQuery(url, []);
  if (!query) { apiResponse(res, { ok: false, status: 400, error: 'bad-api-mock-query' }); return; }
  apiMockMod.status().then(function (result) {
    apiResponse(res, result);
  }).catch(function () {
    apiResponse(res, { ok: false, status: 500, error: 'api-mock-status-failed' });
  });
}

function handleApiMockStart(req, res) {
  readJsonBody(req).then(function (body) {
    return apiMockMod.start(body).then(function (result) {
      if (result && result.ok) sse.broadcast('api-mock', { changed: true });
      apiResponse(res, result);
    });
  }, function (error) {
    apiResponse(res, { ok: false, status: error.httpStatus || 400, error: 'bad-json' });
  }).catch(function () {
    apiResponse(res, { ok: false, status: 500, error: 'api-mock-start-failed' });
  });
}

function handleApiMockStop(req, res) {
  readJsonBody(req).then(function (body) {
    return apiMockMod.stop(body).then(function (result) {
      if (result && result.ok) sse.broadcast('api-mock', { changed: true });
      apiResponse(res, result);
    });
  }, function (error) {
    apiResponse(res, { ok: false, status: error.httpStatus || 400, error: 'bad-json' });
  }).catch(function () {
    apiResponse(res, { ok: false, status: 500, error: 'api-mock-stop-failed' });
  });
}

function handleApiMockLogs(req, res, url) {
  var query = apiQuery(url, ['serverId', 'cursor', 'limit']);
  apiResponse(res, query ? apiMockMod.logs(query) :
    { ok: false, status: 400, error: 'bad-api-mock-logs-query' });
}

function handleTaskSummary(req, res, stem) {
  try {
    var result = taskSummaryMod.single(stem);
    if (!result) { jsonResponse(res, 404, { error: 'task-summary-not-found' }); return; }
    jsonResponse(res, 200, result);
  } catch (error) {
    var status = error && error.code === 'task-summary-stem-invalid' ? 400 : 503;
    jsonResponse(res, status, { error: taskSummaryMod.publicErrorCode(error) });
  }
}

function handleTaskFile(req, res, url) {
  try {
    var result = taskFilesMod.read(url.searchParams.get('column'), url.searchParams.get('stem'));
    res.writeHead(200, {
      'content-type': 'text/markdown; charset=utf-8',
      'content-length': result.bytes.length,
      'cache-control': 'no-store',
      'x-content-type-options': 'nosniff'
    });
    res.end(result.bytes);
  } catch (error) {
    jsonResponse(res, Number(error && error.httpStatus) || 503, {
      error: taskFilesMod.publicErrorCode(error)
    });
  }
}

// GET /api/tasks/integrity — fresh, strictly read-only canonical filesystem +
// INDEX diagnostics.  Output is bounded by task-integrity.js and never exposes
// task bodies or the core's internal parser model.
function handleTasksIntegrity(req, res) {
  try {
    jsonResponse(res, 200, taskIntegrityMod.publicResult(taskIntegrityMod.validateAll()));
  } catch (error) {
    var response = taskIntegrityMod.errorResponse(error);
    jsonResponse(res, response.status, response.body);
  }
}

function lockRecoveryError(res, error) {
  var code = error && error.code;
  var status = Number(error && error.httpStatus) || 500;
  var publicCode = code === 'bad-stem' || code === 'INVOCATION_INVALID' ? 'bad-stem' :
    (code === 'LOCK_NOT_FOUND' ? 'task-lock-not-found' : 'task-lock-recovery-unavailable');
  jsonResponse(res, status, {
    error: publicCode,
    reasonCode: locksMod.publicRecoveryReasonCode(code)
  });
}

// GET is phase one and strictly read-only: the canonical helper inspects the
// exact local process generation plus writer-tree authority and returns the
// lock hash that binds phase two.  No age/session heuristic can enable release.
function handleTaskLockRecoveryInspect(req, res, url) {
  locksMod.inspectOwnerRecovery(url.searchParams.get('stem')).then(function (result) {
    jsonResponse(res, 200, result);
  }, function (error) {
    lockRecoveryError(res, error);
  });
}

// POST is phase two.  A lock-writer lease is published before the finalization
// re-check, so a finalizer and dead-owner recovery cannot cross in the gap
// between browser inspection and the exact canonical detach.
function handleTaskLockRecovery(req, res) {
  readJsonBody(req).then(function (body) {
    var fields = body && typeof body === 'object' && !Array.isArray(body) ? Object.keys(body).sort() : [];
    if (fields.join('\0') !== ['expectedLockHash', 'stem'].join('\0') ||
        !validTaskStem(body.stem) ||
        !/^sha256:[a-f0-9]{64}$/.test(String(body.expectedLockHash || ''))) {
      jsonResponse(res, 400, { error: 'bad-lock-recovery-request' });
      return;
    }
    var sessionId = finalizationsMod.createWriterSessionId();
    var leaseStart = finalizationsMod.beginMutation({
      kind: 'lock-writer',
      stem: body.stem,
      sessionId: sessionId,
      key: 'task-lock-recovery:' + body.stem,
      pendingChild: false
    });
    if (!leaseStart.ok) {
      jsonResponse(res, 409, {
        error: leaseStart.error || 'task-lock-recovery-busy',
        detail: String(leaseStart.detail || 'Another exact workspace owner blocks lock recovery.').slice(0, 500)
      });
      return;
    }
    var lease = leaseStart.handle;
    locksMod.recoverOwner(body.stem, body.expectedLockHash, lease).then(function (result) {
      var settled = finalizationsMod.endMutation(lease);
      sse.pollLoop();
      if (!settled) {
        jsonResponse(res, 500, {
          error: 'task-lock-recovered-lease-unsettled',
          recovered: true
        });
        return;
      }
      jsonResponse(res, 200, result);
    }, function (error) {
      var settled = finalizationsMod.endMutation(lease);
      sse.pollLoop();
      if (!settled) {
        jsonResponse(res, 500, {
          error: 'task-lock-recovery-lease-unsettled',
          recovered: false
        });
        return;
      }
      lockRecoveryError(res, error);
    });
  }, function (error) {
    jsonResponse(res, Number(error && error.httpStatus) || 400, {
      error: 'bad-json'
    });
  }).catch(function (error) {
    jsonResponse(res, 500, { error: 'task-lock-recovery-internal' });
  });
}

// GET /api/tasks/drop-impact?stem=TASK_... — phase one of Drop. The response
// binds the exact source revision and complete live-dependent set into a
// deterministic impact hash. It is read-only; mutation is still owned by the
// transition helper after the user has seen and acknowledged this snapshot.
function handleTaskDropImpact(req, res, url) {
  var stem = url.searchParams.get('stem');
  try {
    var lock = locksMod.lockPresence(stem);
    if (!lock.validStem) { jsonResponse(res, 400, { error: 'bad-stem' }); return; }
    if (lock.present) {
      jsonResponse(res, 409, {
        error: 'task-lock-present',
        detail: 'Drop is unavailable while any canonical task-lock generation exists. Continue or recover the exact owner; Drop never cancels or clears a lock.'
      });
      return;
    }
    var inspected = taskIntegrityMod.inspectDrop(stem);
    var result = inspected.result;
    if (!inspected.admission || !inspected.admission.ok) {
      jsonResponse(res, 409, {
        error: 'task-integrity',
        integrity: taskIntegrityMod.publicResult(result)
      });
      return;
    }
    jsonResponse(res, 200, inspected.impact);
  } catch (error) {
    if (error && error.exitCode === 2) {
      jsonResponse(res, 400, { error: 'bad-stem' });
      return;
    }
    var response = taskIntegrityMod.errorResponse(error);
    jsonResponse(res, response.status, response.body);
  }
}

function handleEvents(req, res) {
  res.writeHead(200, {
    'content-type':  'text/event-stream; charset=utf-8',
    'cache-control': 'no-cache, no-transform',
    'connection':    'keep-alive',
    'x-accel-buffering': 'no'
  });
  // Reconnect backoff hint. Kept at/under the 1500ms SSE poll (sse.POLL_MS) so a
  // dropped connection is re-established within one poll cycle — a longer retry
  // would let reconnect latency exceed a poll and miss a 'change' tick.
  res.write('retry: 1500\n\n');
  // Send current state immediately so a new client doesn't have to wait
  // for the next change to populate.
  res.write('event: change\ndata: ' + JSON.stringify({ t: Date.now(), initial: true }) + '\n\n');
  sse.sseClients.add(res);
  req.on('close', function () { sse.sseClients.delete(res); });
}

// Serialize state-mutating handlers so two concurrent POSTs can't read the
// same snapshot, each merge its delta, and have the later write clobber the
// earlier — a real risk because readPersisted → mutate → writePersisted is a
// read-modify-write sequence without a file lock. Shared with the SSE timing
// reconciler (sse.serializeStateWrite) so those two writers can't interleave
// across event-loop turns either.
function handleStatePatch(req, res) {
  sse.serializeStateWrite(function () {
    return handleStatePatchSerialized(req, res);
  });
}

function handleStatePatchSerialized(req, res) {
  return readJsonBody(req).then(function (body) {
    var persisted = persistence.readPersisted();
    var dirty = false;
    var droppedKeys = [];

    if (body && body.setup && typeof body.setup === 'object' && !Array.isArray(body.setup)) {
      var clean = Object.assign({}, persisted.setupForm || {});
      var setupDirty = false;
      Object.keys(body.setup).forEach(function (k) {
        var v = sanitizeSetupValue(k, body.setup[k]);
        if (v !== undefined) { clean[k] = v; setupDirty = true; }
        else droppedKeys.push(k);
      });
      persisted.setupForm = clean;
      if (setupDirty) dirty = true;
    }
    if (body && body.manualSteps && typeof body.manualSteps === 'object' && !Array.isArray(body.manualSteps)) {
      var ms = Object.assign({}, persisted.manualSteps || {});
      var msDirty = false;
      Object.keys(body.manualSteps).forEach(function (k) {
        // Accept wizard step ids (STEP_VALIDATORS) AND the three setup-gate
        // override keys ('setup:<gate>'); ignore anything else.
        if (!Object.prototype.hasOwnProperty.call(STEP_VALIDATORS, k) &&
            !Object.prototype.hasOwnProperty.call(SETUP_GATE_OVERRIDE_KEYS, k)) { droppedKeys.push(k); return; }
        ms[k] = body.manualSteps[k] === true;
        msDirty = true;
      });
      persisted.manualSteps = ms;
      // Only mark dirty when a key was actually accepted — a patch whose keys all
      // got dropped must not trigger a redundant disk write + SSE broadcast.
      if (msDirty) dirty = true;
    }
    // UI language ('en'|'ru'|'uk') — a TOP-LEVEL persisted key, deliberately outside
    // setupForm/SETUP_KEY_WHITELIST: it is a per-user UI preference, not a
    // project setup field, and must never leak into the bootstrap YAML
    // frontmatter that setupForm feeds. Persisted server-side because the
    // client's localStorage copy is origin-bound and changes with an explicit port.
    if (body && typeof body.uiLang === 'string') {
      if (persistence.UI_LANGS.indexOf(body.uiLang) >= 0) {
        if (persisted.uiLang !== body.uiLang) { persisted.uiLang = body.uiLang; dirty = true; }
      } else {
        droppedKeys.push('uiLang');
      }
    }

    if (dirty) {
      persistence.writePersisted(persisted);
      sse.pollLoop(); // immediate broadcast so the client doesn't wait for the next poll tick
    }
    var responseBody = deriveState();
    if (droppedKeys.length > 0) responseBody = Object.assign({}, responseBody, { warnings: { droppedSetupKeys: droppedKeys } });
    jsonResponse(res, 200, responseBody);
  }, function (e) {
    jsonResponse(res, 400, { error: 'bad-json' });
  }).catch(function (e) {
    jsonResponse(res, 500, { error: 'internal' });
    return Promise.reject(e);
  });
}

function handleReset(req, res) {
  sse.serializeStateWrite(function () {
    try {
      persistence.writePersisted(persistence.clone(persistence.DEFAULT_PERSISTED));
    } catch (e) {
      jsonResponse(res, 500, { error: 'internal' });
      return;
    }
    sse.pollLoop();
    jsonResponse(res, 200, deriveState());
  });
}

// Internal queue admission for server-owned task prompts. This is deliberately
// not routed as HTTP: the browser can submit only the exact typed action DTO,
// and the server derives every executable byte before entering this function.
// `action` ∈ REQUEST_ACTIONS and `stem` is required for every action.
// The id is generated server-side (never trust a client path). A durable
// per-stem reservation serializes admission across Site processes. Retries are
// deduped only by the exact full-intent fingerprint (prompt + action + admitted
// state/revision + dedup metadata), never by a coarse action/stem label.
function enqueueServerOwnedTaskRequest(req, res) {
  readJsonBody(req).then(function (body) {
    var action = body && typeof body.action === 'string' ? body.action : null;
    if (!action || !REQUEST_ACTIONS.has(action)) {
      jsonResponse(res, 400, { error: 'bad-action' }); return;
    }
    var stem = body && typeof body.stem === 'string' ? body.stem : null;
    var prompt = body && typeof body.prompt === 'string' ? body.prompt : '';
    var dedupKey = body && typeof body.dedupKey === 'string' ? body.dedupKey : null;
    if (dedupKey && !requestsMod.REQUEST_DEDUP_KEY_RE.test(dedupKey)) {
      jsonResponse(res, 400, { error: 'bad-dedup-key' }); return;
    }
    var dedupReport = body && typeof body.dedupReport === 'string' ? body.dedupReport : null;
    if (dedupReport && !requestsMod.REQUEST_DEDUP_REPORT_RE.test(dedupReport)) {
      jsonResponse(res, 400, { error: 'bad-dedup-report' }); return;
    }
    // Validate all client-controlled queue fields before touching the task
    // corpus. expectedState/sourceRevision below are server stamps, so the
    // provisional values are deliberately valid placeholders and are never
    // persisted.
    var provisionalRecord = {
      version: requestsMod.REQUEST_VERSION,
      action: action,
      stem: stem,
      expectedState: taskIntegrityMod.ACTION_STATES[action][0],
      sourceRevision: 'sha256:' + '0'.repeat(64),
      dedupKey: dedupKey,
      dedupReport: dedupReport,
      projectRoot: paths.PROJECT_ROOT,
      prompt: prompt,
      createdAt: new Date().toISOString()
    };
    var contractIssue = runner.claimedRequestIssue(provisionalRecord, paths.PROJECT_ROOT);
    if (contractIssue) {
      jsonResponse(res, 400, { error: 'bad-request-contract', detail: contractIssue }); return;
    }
    if (finalizationsMod.mutationBlocked(stem)) {
      jsonResponse(res, 409, { error: 'finalization-active', detail: 'This task has an unfinished deterministic finalization. Resume it instead of starting another task action.' });
      return;
    }
    var lockPresence = locksMod.lockPresence(stem);
    if (!lockPresence.validStem) {
      jsonResponse(res, 400, { error: 'bad-request-contract', detail: 'request stem is not canonical' }); return;
    }
    if (lockPresence.present) {
      jsonResponse(res, 409, {
        error: 'task-lock-present',
        detail: 'A canonical task owner is already attached to this stem. Continue or recover that exact owner before starting another action.'
      });
      return;
    }
    // Visual Run-gate: refuse to queue only a task for which the screenshot
    // enforcement net actually applies while that net is unwired. The runner
    // would only hold it, and an immediate typed 409 with the wiring command
    // beats a silently-parked chip. Non-visual runs bypass this gate.
    // sessions.start()/send() enforce the same gate for anything already queued.
    if (action === 'run') {
      var runGateErr = sessionsMod.runGateError(stem);
      if (runGateErr) {
        jsonResponse(res, 409, { error: 'figma-net-unwired', detail: runGateErr });
        return;
      }
    }

    // Admission is based on a fresh task-scoped canonical snapshot; the browser
    // cannot claim a state/revision. INDEX is a derived projection and does not
    // invalidate an otherwise-safe task action. Drop has its own repair
    // admission so malformed task content remains deletable.
    var admission;
    try { admission = taskIntegrityMod.validateAction(action, stem); }
    catch (integrityError) {
      var unavailable = taskIntegrityMod.errorResponse(integrityError);
      jsonResponse(res, unavailable.status, unavailable.body);
      return;
    }
    var scopedAdmission = taskIntegrityMod.admissionForAction(admission, stem);
    if (!scopedAdmission.ok) {
      var staleOnly = scopedAdmission.blockers.length > 0 &&
        scopedAdmission.blockers.every(function (item) {
          return item.code === 'REQUEST_ACTION_STATE_MISMATCH';
        });
      jsonResponse(res, 409, {
        error: staleOnly ? 'stale-task-state' : 'task-integrity',
        integrity: taskIntegrityMod.publicResult(admission)
      });
      return;
    }
    var requestRecord = Object.assign({}, provisionalRecord, {
      expectedState: admission.observedState,
      sourceRevision: admission.sourceRevision,
      createdAt: new Date().toISOString()
    });
    contractIssue = runner.claimedRequestIssue(requestRecord, paths.PROJECT_ROOT);
    if (contractIssue) {
      jsonResponse(res, 500, { error: 'task-state-contract', detail: contractIssue }); return;
    }

    var id = Date.now() + '-' + crypto.randomBytes(8).toString('hex');
    var fingerprint = requestsMod.requestFingerprint(requestRecord);
    var reservation = requestsMod.acquireRequestReservation(id, requestRecord);

    // Another Site process won the per-stem linearization point. Only an
    // already-visible queue/claim with the exact full-intent fingerprint may be
    // reported as a dedup. Same action/state with different prompt bytes
    // (notably different answers) is a conflict, never a false 200.
    if (!reservation.ok) {
      if (reservation.code === 'request-reservation-active' && reservation.existing) {
        var observedQueueScan = requestsMod.scanRequests();
        var observedClaimScan = requestsMod.scanActiveClaims();
        if (!observedQueueScan.ok || !observedClaimScan.ok) {
          jsonResponse(res, 409, {
            error: 'request-admission-scan-unsafe',
            detail: !observedQueueScan.ok ? observedQueueScan.code : observedClaimScan.code
          });
          return;
        }
        var observedQueued = observedQueueScan.rows;
        var observedClaims = observedClaimScan.rows;
        var visible = observedQueued.find(function (row) {
          return row.id === reservation.existing.requestId && row.stem === stem;
        });
        var visiblePhase = 'queued';
        if (!visible) {
          visible = observedClaims.find(function (row) {
            return row.id === reservation.existing.requestId && row.stem === stem;
          });
          visiblePhase = 'claimed';
        }
        if (visible && reservation.existing.fingerprint === fingerprint && visible.fingerprint === fingerprint) {
          jsonResponse(res, 200, {
            id: reservation.existing.requestId, deduped: true,
            active: visiblePhase === 'claimed', state: deriveState()
          });
        } else {
          jsonResponse(res, 409, {
            error: 'task-action-active',
            active: {
              id: reservation.existing.requestId,
              action: visible && visible.action || null,
              phase: visible ? visiblePhase : 'reserved'
            }
          });
        }
        return;
      }
      var reservationStatus = reservation.code === 'request-reservation-unsafe' ? 409 : 500;
      jsonResponse(res, reservationStatus, {
        error: requestsMod.publicReservationErrorCode(reservation),
        detail: reservation.detail || 'per-stem request reservation could not be acquired'
      });
      return;
    }

    var reservationHeld = true;
    var published = false;
    function respondAfterReservationRelease(status, payload) {
      if (!reservationHeld || !requestsMod.releaseRequestReservation(reservation.handle)) {
        reservationHeld = false;
        jsonResponse(res, 500, {
          error: 'request-reservation-release-failed',
          detail: 'The request was not published; inspect the per-stem reservation before retrying.'
        });
        return;
      }
      reservationHeld = false;
      jsonResponse(res, status, payload);
    }

    try {
      // The reservation stays held across every snapshot below and the final
      // no-clobber queue publication, closing scan→write across Site processes.
      var queueScan = requestsMod.scanRequests();
      if (!queueScan.ok) {
        respondAfterReservationRelease(409, { error: 'request-admission-scan-unsafe', detail: queueScan.code });
        return;
      }
      var queued = queueScan.rows;
      for (var q = 0; q < queued.length; q++) {
        if (queued[q].stem !== stem) continue;
        if (queued[q].fingerprint === fingerprint) {
          respondAfterReservationRelease(200, { id: queued[q].id, deduped: true, state: deriveState() });
        } else {
          respondAfterReservationRelease(409, { error: 'task-action-active', active: { id: queued[q].id, action: queued[q].action, phase: 'queued' } });
        }
        return;
      }
      var claimScan = requestsMod.scanActiveClaims();
      if (!claimScan.ok) {
        respondAfterReservationRelease(409, { error: 'request-admission-scan-unsafe', detail: claimScan.code });
        return;
      }
      var claimed = claimScan.rows;
      for (var c = 0; c < claimed.length; c++) {
        if (claimed[c].stem !== stem) continue;
        if (claimed[c].fingerprint === fingerprint) {
          respondAfterReservationRelease(200, { id: claimed[c].id, deduped: true, active: true, state: deriveState() });
        } else {
          respondAfterReservationRelease(409, { error: 'task-action-active', active: { id: claimed[c].id, action: claimed[c].action, phase: 'claimed' } });
        }
        return;
      }
      var liveInfo = runner.runInfoForStem(stem);
      if (liveInfo && liveInfo.busy) {
        // Live sidecars deliberately omit the executable prompt, so they cannot
        // prove the full fingerprint even when their action label matches.
        respondAfterReservationRelease(409, { error: 'task-action-active', active: { action: liveInfo.action, phase: 'running' } });
        return;
      }

      if (dedupKey) {
        for (var d = 0; d < queued.length; d++) {
          if (queued[d].action !== action || queued[d].dedupKey !== dedupKey) continue;
          if (queued[d].fingerprint === fingerprint) {
            respondAfterReservationRelease(200, { id: queued[d].id, deduped: true, state: deriveState() });
          } else {
            respondAfterReservationRelease(409, { error: 'dedup-key-conflict', active: { id: queued[d].id, action: queued[d].action, phase: 'queued' } });
          }
          return;
        }
        var liveDedup = runner.runIdForDedupKey(action, dedupKey);
        if (liveDedup) {
          respondAfterReservationRelease(409, { error: 'dedup-key-active', active: { key: liveDedup, action: action, phase: 'running' } });
          return;
        }
      }

      // Mandatory second half of reservation→writer handoff. A runner in
      // another Site process may have published its writer lease, completed the
      // final fence, released the previous reservation and consumed its claim
      // after our early admission check. Re-scan writer/finalization authority
      // while this new reservation is held, immediately before queue publish.
      // The active attached lease then closes release→prompt→lock visibility.
      if (finalizationsMod.mutationBlocked(stem)) {
        respondAfterReservationRelease(409, {
          error: 'task-action-active',
          active: { action: null, phase: 'writer-handoff' }
        });
        return;
      }

      // Final canonical admission fence under the per-stem reservation. The
      // initial lock/task snapshot can drift while we wait to acquire that
      // cross-process linearization point. Re-read immediately before publish;
      // no queued byte may carry an already-stale state/revision or overlap a
      // task-lock generation that appeared after the first check.
      var finalLockPresence = locksMod.lockPresence(stem);
      if (!finalLockPresence.validStem || finalLockPresence.present) {
        respondAfterReservationRelease(409, {
          error: 'task-lock-present',
          detail: 'A canonical task owner appeared during request admission. Retry only after that exact owner finishes.'
        });
        return;
      }
      var finalAdmission;
      try { finalAdmission = taskIntegrityMod.validateAction(action, stem); }
      catch (finalIntegrityError) {
        var finalUnavailable = taskIntegrityMod.errorResponse(finalIntegrityError);
        respondAfterReservationRelease(finalUnavailable.status, finalUnavailable.body);
        return;
      }
      var finalScopedAdmission = taskIntegrityMod.admissionForAction(finalAdmission, stem);
      if (!finalScopedAdmission.ok) {
        var finalStaleOnly = finalScopedAdmission.blockers.length > 0 &&
          finalScopedAdmission.blockers.every(function (item) {
            return item.code === 'REQUEST_ACTION_STATE_MISMATCH';
          });
        respondAfterReservationRelease(409, {
          error: finalStaleOnly ? 'stale-task-state' : 'task-integrity',
          integrity: taskIntegrityMod.publicResult(finalAdmission)
        });
        return;
      }
      if (finalAdmission.observedState !== requestRecord.expectedState ||
          finalAdmission.sourceRevision !== requestRecord.sourceRevision) {
        respondAfterReservationRelease(409, {
          error: 'stale-task-state',
          detail: 'The canonical task snapshot changed during request admission.',
          integrity: taskIntegrityMod.publicResult(finalAdmission)
        });
        return;
      }

      var ok = requestsMod.writeRequestFile(id, requestRecord);
      if (!ok) {
        // The no-clobber link may be visible even when the final directory
        // durability proof failed. Keep its exact reservation in that
        // ambiguous state; releasing it would expose claimable bytes without
        // the per-stem handoff fence. A retry can dedupe the exact visible id.
        if (requestsMod.requestFileMatches(id, requestRecord)) {
          published = true;
          reservationHeld = false;
          jsonResponse(res, 503, { error: 'write-durability-unverified', id: id });
          return;
        }
        respondAfterReservationRelease(500, { error: 'write-failed' });
        return;
      }
      published = true;
      reservationHeld = false; // ownership now travels with queue/claim bytes
    } catch (enqueueError) {
      if (reservationHeld && !published) requestsMod.releaseRequestReservation(reservation.handle);
      throw enqueueError;
    }
    // Drain immediately so the interactive session spawns within this request
    // (the run-control flips to ⊡ Terminal at once instead of after the next
    // 2s poll). No-op when the runner is dormant (no `claude` on PATH) — the
    // request then waits for the /loop worker, exactly as before.
    try { runner.tick(); } catch (e) {}
    sse.pollLoop();
    jsonResponse(res, 200, { id: id, state: deriveState() });
  }, function (e) {
    jsonResponse(res, 400, { error: 'bad-json' });
  }).catch(function (e) {
    jsonResponse(res, 500, { error: 'enqueue-failed' });
  });
}

// POST /api/tasks/backlog — deterministic, durable creation. The response is
// independent of Claude availability; the advisory shallow intake is scheduled
// separately after publication and can fail without changing this result.
function handleBacklogCreate(req, res) {
  readJsonBody(req).then(function (body) {
    if (!body || typeof body !== 'object' || Array.isArray(body) ||
        Object.prototype.hasOwnProperty.call(body, 'source')) {
      throw Object.assign(new Error('The public backlog endpoint does not accept a raw Source envelope.'), {
        code: 'bad-task-source', httpStatus: 400
      });
    }
    var intentId = typeof body.idempotencyKey === 'string'
      ? 'intent-' + crypto.createHash('sha256').update(body.idempotencyKey, 'utf8').digest('hex')
      : 'none';
    body.source = body.originStem
      ? taskSourceMod.followUp(body.originStem, 'task-split', intentId)
      : taskSourceMod.manualForIntent(intentId, 'manual', intentId);
    return backlogCreateMod.create(body).then(function (result) {
      // Advisory scheduling is deliberately after the durable create result.
      // Its failure is represented on the preview only and cannot turn a
      // successfully-created task into an HTTP creation failure.
      try { result.intake = shallowIntakeMod.schedule(result.stem, result.replayed ? 'creation-replay' : 'creation'); }
      catch (intakeError) {
        result.intake = shallowIntakeMod.recordFailure(result.stem, intakeError) ||
          { status: 'failed', errorCode: 'INTAKE_SCHEDULE_FAILED', retryable: true };
      }
      result.intake = shallowIntakeMod.publicProjection(result.intake);
      jsonResponse(res, result.created ? 201 : 200, result);
      // The advisory record is already durable and represented in the HTTP
      // result. A full state/integrity poll can be comparatively expensive;
      // run it on the next turn so it cannot hold the deterministic creation
      // response behind advisory reconciliation or model-related work.
      setImmediate(function () { sse.pollLoop(); });
    });
  }).catch(function (error) {
    var status = Number(error && error.httpStatus) || 500;
    if (status < 400 || status > 599) status = 500;
    jsonResponse(res, status, {
      error: backlogCreateMod.publicCreateErrorCode(error),
      recoverable: !!(error && error.result && error.result.recoverable),
      stem: error && error.result && error.result.stem || null
    });
  });
}

// The pre-Setup inbox is a separate durable namespace. Saving here never
// creates a canonical task or exposes an execution action.
function handleTaskInboxList(req, res) {
  try {
    jsonResponse(res, 200, taskInboxMod.list());
  } catch (error) {
    jsonResponse(res, Number(error && error.httpStatus) || 500, {
      error: taskInboxMod.publicErrorCode(error)
    });
  }
}

function handleTaskInboxSave(req, res) {
  readJsonBody(req).then(function (body) {
    var result = taskInboxMod.save(body);
    jsonResponse(res, result.created ? 201 : 200, result);
  }).catch(function (error) {
    jsonResponse(res, Number(error && error.httpStatus) || 500, {
      error: taskInboxMod.publicErrorCode(error)
    });
  });
}

function handleTaskInboxPublish(req, res) {
  readJsonBody(req).then(function (body) {
    if (!body || typeof body !== 'object' || Array.isArray(body) ||
        Object.keys(body).sort().join(',') !== 'id') {
      throw Object.assign(new Error('bad-task-inbox-request'), {
        code: 'bad-task-inbox-request', httpStatus: 400
      });
    }
    var snapshot = deriveState();
    if (!snapshot.progress || snapshot.progress.setupDone !== true) {
      throw Object.assign(new Error('setup-incomplete'), {
        code: 'setup-incomplete', httpStatus: 409
      });
    }
    return taskInboxMod.publish(body.id).then(function (result) {
      // Match ordinary backlog creation: shallow intake is advisory and starts
      // only after the canonical publication has durably completed.
      if (result && result.stem && Object.prototype.hasOwnProperty.call(result, 'created')) {
        try {
          result.intake = shallowIntakeMod.schedule(
            result.stem, result.replayed ? 'creation-replay' : 'creation'
          );
        } catch (intakeError) {
          result.intake = shallowIntakeMod.recordFailure(result.stem, intakeError) ||
            { status: 'failed', errorCode: 'INTAKE_SCHEDULE_FAILED', retryable: true };
        }
        result.intake = shallowIntakeMod.publicProjection(result.intake);
      }
      jsonResponse(res, result && result.created ? 201 : 200, result);
      setImmediate(function () { sse.pollLoop(); });
    });
  }).catch(function (error) {
    jsonResponse(res, Number(error && error.httpStatus) || 500, {
      error: error && error.code === 'setup-incomplete'
        ? 'setup-incomplete' : taskInboxMod.publicErrorCode(error)
    });
  });
}

function handleBacklogSource(req, res, url) {
  var stem = url.searchParams.get('stem') || '';
  try {
    var source = shallowIntakeMod.sourceState(stem);
    if (!source.eligible) { jsonResponse(res, 409, { error: 'task-not-idle-backlog' }); return; }
    jsonResponse(res, 200, { stem: stem, sourceHash: source.sourceHash, markdown: source.text });
  } catch (error) {
    jsonResponse(res, Number(error && error.httpStatus) || 500, {
      error: shallowIntakeMod.publicErrorCode(error, 'backlog-read-failed')
    });
  }
}

function handleBacklogEdit(req, res) {
  readJsonBody(req).then(function (body) {
    // Fail before publishing a writer lease when prep/drop already owns the
    // task. The Python helper repeats exact hash/column checks under its mutex.
    var source = shallowIntakeMod.sourceState(body && body.stem);
    if (!source.eligible) throw Object.assign(new Error('task is not an idle backlog item: ' + source.reason), { code: 'task-not-idle-backlog', httpStatus: 409 });
    if (source.sourceHash !== (body && body.expectedSourceHash)) throw Object.assign(new Error('task changed since it was loaded'), { code: 'source-changed', httpStatus: 409 });
    var beforeProvenance = taskSourceMod.parse(source.text);
    var afterProvenance = taskSourceMod.parse(body && body.markdown);
    var sourcePreserved = beforeProvenance.valid && afterProvenance.valid &&
      beforeProvenance.present === afterProvenance.present &&
      (!beforeProvenance.present || taskSourceMod.same(beforeProvenance.source, afterProvenance.source));
    if (!sourcePreserved) throw Object.assign(new Error('task Source provenance is immutable'), {
      code: 'task-source-immutable', httpStatus: 409
    });
    return backlogCreateMod.edit(body).then(function (result) {
      if (result.changed) {
        try { result.intake = shallowIntakeMod.schedule(result.stem, 'source-edited', { force: true }); }
        catch (intakeError) {
          result.intake = shallowIntakeMod.recordFailure(result.stem, intakeError) ||
            { status: 'failed', errorCode: 'INTAKE_SCHEDULE_FAILED', retryable: true };
        }
        result.intake = shallowIntakeMod.publicProjection(result.intake);
      }
      sse.pollLoop();
      jsonResponse(res, 200, result);
    });
  }).catch(function (error) {
    jsonResponse(res, Number(error && error.httpStatus) || 500, {
      error: backlogCreateMod.publicEditErrorCode(error)
    });
  });
}

function handleShallowIntakeRetry(req, res) {
  readJsonBody(req).then(function (body) {
    var result = shallowIntakeMod.retry(body && body.stem, body && body.expectedSourceHash);
    sse.pollLoop();
    jsonResponse(res, 202, { intake: shallowIntakeMod.publicProjection(result) });
  }).catch(function (error) {
    jsonResponse(res, Number(error && error.httpStatus) || 500, {
      error: shallowIntakeMod.publicErrorCode(error, 'intake-retry-failed')
    });
  });
}

function handleShallowIntakeDismiss(req, res) {
  readJsonBody(req).then(function (body) {
    var result = shallowIntakeMod.dismiss(body && body.stem, body && body.expectedSourceHash);
    sse.pollLoop();
    jsonResponse(res, 200, { intake: shallowIntakeMod.publicProjection(result) });
  }).catch(function (error) {
    jsonResponse(res, Number(error && error.httpStatus) || 500, {
      error: shallowIntakeMod.publicErrorCode(error, 'intake-dismiss-failed')
    });
  });
}

// --- Claude CLI control (header install/login buttons) -------------------
function handleCliInstall(req, res) {
  var job = cliMod.install();
  cliMod.invalidate(); sse.pollLoop();
  if (!job || !job.running || job.error) {
    jsonResponse(res, 409, { error: 'cli-install-start-refused' });
    return;
  }
  jsonResponse(res, 200, { job: job, cli: cliMod.status() });
}
function handleCliLogin(req, res) {
  readJsonBody(req).then(function (body) {
    // fresh: re-login — clear the stored (expired/revoked) credentials first.
    var job = cliMod.login({ fresh: !!(body && body.fresh) });
    sse.pollLoop();
    if (!job || !job.running || job.error) {
      jsonResponse(res, 409, { error: 'cli-login-start-refused' });
      return;
    }
    jsonResponse(res, 200, { job: job });
  }, function (e) { jsonResponse(res, 400, { error: 'bad-json' }); })
  .catch(function (e) { jsonResponse(res, 500, { error: 'internal' }); });
}
function handleCliLoginCode(req, res) {
  readJsonBody(req).then(function (body) {
    var code = body && typeof body.code === 'string' ? body.code : '';
    if (!code || !code.trim()) { jsonResponse(res, 400, { error: 'no-code' }); return; }
    var ok = cliMod.loginSubmitCode(code);
    if (!ok) { jsonResponse(res, 409, { error: 'cli-login-code-refused' }); return; }
    jsonResponse(res, 200, { submitted: true });
  }, function (e) { jsonResponse(res, 400, { error: 'bad-json' }); })
  .catch(function (e) { jsonResponse(res, 500, { error: 'internal' }); });
}
function handleCliLoginCancel(req, res) {
  cliMod.loginCancel(); sse.pollLoop();
  jsonResponse(res, 200, { canceled: true });
}
function handleCliLog(req, res, url) {
  var kind = url.searchParams.get('kind');
  var text = cliMod.readJobLog(kind);
  if (text === null || text === undefined) { jsonResponse(res, 400, { error: 'bad-kind' }); return; }
  jsonResponse(res, 200, { kind: kind, text: text });
}

// --- Figma MCP connector (header pill + Figma panel) ----------------------

// Queue an immediate re-probe of `claude mcp list`. invalidate() is intentionally
// asynchronous, so the response labels the returned snapshot as cached instead of
// claiming it is the fresh probe result; SSE publishes the result when it lands.
function handleFigmaRecheck(req, res) {
  if (finalizationsMod.mutationBlocked(null)) {
    jsonResponse(res, 409, { error: 'finalization-active', detail: 'Wait for the active task finalization before re-checking the Figma identity.' }); return;
  }
  var active = activeFigmaOrTaskSession();
  if (active) { jsonResponse(res, 409, { error: 'figma-session-active', detail: 'Wait for the active Figma/task session or open it from Sessions.' }); return; }
  figmaMod.invalidateIdentity();
  sse.pollLoop();
  jsonResponse(res, 202, { queued: true, figma: figmaMod.status() });
}

// Bind this project: add a local-scoped "figma" MCP server (a `claude mcp add`
// config write, not a Figma call). OAuth itself is still the user's interactive
// `/mcp` step; this only scaffolds the server. Re-probes so the panel flips to
// "needs authentication" without a manual Re-check.
function activeFigmaOrTaskSession() {
  var syncJob = figmaSyncMod.active();
  if (syncJob) return 'sync:' + syncJob.id;
  if (figmaSyncMod.recoveryState() === 'failed') return 'sync:recovery-failed';
  if (figmaSyncMod.busy()) return 'sync:recovering';
  if (figmaTaskPublicationMod.recoveryState() === 'failed') return 'task-publication:recovery-failed';
  if (figmaTaskPublicationMod.busy()) return 'task-publication:active';
  var testJob = figmaTestJobMod.currentJob();
  if (testJob) return 'test:' + testJob.id;
  if (figmaTestJobMod.busy()) return 'file-verification-active';
  try {
    var sessions = sessionsMod.list();
    return Object.keys(sessions).find(function (key) {
      var value = sessions[key];
      return value && value.running && (key.indexOf('task:') === 0 || key.indexOf('figma:') === 0);
    }) || null;
  } catch (error) { return 'sessions:unavailable'; }
}

function handleFigmaAddLocal(req, res) {
  if (finalizationsMod.mutationBlocked(null)) {
    jsonResponse(res, 409, { error: 'finalization-active', detail: 'Figma configuration mutations are blocked while any durable task finalization needs recovery.' }); return;
  }
  if (activeFigmaOrTaskSession()) {
    jsonResponse(res, 409, { error: 'figma-session-active', detail: 'Wait for the active Figma/task session or open it from Sessions.' }); return;
  }
  figmaMod.addLocalServer(function (err, out) {
    if (err) { jsonResponse(res, 500, { error: 'add-failed' }); return; }
    figmaMod.invalidate();
    sse.pollLoop();
    jsonResponse(res, 200, { added: true, figma: figmaMod.status() });
  });
}

// Open the native Terminal already in the project dir with `claude` running (macOS only) so the user
// only has to type /mcp → Authenticate. Not a Figma call — just `osascript`.
function handleFigmaOpenTerminal(req, res) {
  if (finalizationsMod.mutationBlocked(null)) {
    jsonResponse(res, 409, { error: 'finalization-active', detail: 'Wait for the active task finalization before changing the Figma account.' }); return;
  }
  var active = activeFigmaOrTaskSession();
  if (active) { jsonResponse(res, 409, { error: 'figma-session-active', detail: 'Wait for the active Figma/task session or open it from Sessions.' }); return; }
  figmaMod.openTerminal(function (err) {
    if (err) {
      var msg = String((err && err.message) || err);
      var code = msg === 'unsupported-platform' ? 400 : 500;
      jsonResponse(res, code, { error: code === 400 ? 'unsupported-platform' : 'open-failed' }); return;
    }
    figmaMod.invalidateIdentity();
    sse.pollLoop();
    jsonResponse(res, 200, { opened: true });
  });
}


// --- Generic interactive sessions (Setup / Wizard / Board task terminals) ---
// sessions.js owns the exact key contract because it must enforce the same allowlist
// for HTTP starts, internal starts and persisted sidecars.
function validSessionKey(key) {
  return sessionsMod.validSessionKey(key);
}

function figmaStemForKey(key) {
  if (key.indexOf('figma:screens:') === 0) return key.slice('figma:screens:'.length);
  if (key.indexOf('figma:rebundle:') === 0) return key.slice('figma:rebundle:'.length);
  return null;
}

var SESSION_START_ERRORS = Object.freeze({
  'invalid-session-key': 1,
  'finalization-active': 1,
  'figma-net-unwired': 1,
  'writer-termination-pending': 1,
  'conversation-only-contract-invalid': 1,
  'runtime-only-contract-invalid': 1,
  'session-runtime-unsafe': 1,
  'workspace-writer-lease-refused': 1,
  'workspace-writer-lease-attach-failed': 1,
  'session-spawn-failed': 1,
  'initial-prompt-refused': 1
});
function publicSessionStartError(status) {
  var code = status && typeof status.error === 'string' ? status.error : '';
  return SESSION_START_ERRORS[code] ? code : 'session-start-refused';
}

function deliverFigmaAction(key, resolved, res) {
  var current = sessionsMod.status(key);
  if (current && current.running && (current.awaitingTurn || current.askedThisTurn)) {
    jsonResponse(res, 409, { error: 'session-busy' });
    return;
  }
  var meta = { action: resolved.action };
  var stem = figmaStemForKey(key);
  if (stem) meta.stem = stem;
  if (resolved.screenTokenPlanId) meta.screenTokenPlanId = resolved.screenTokenPlanId;

  if (current && current.running) {
    var sent = sessionsMod.send(key, resolved.prompt, meta);
    sse.pollLoop();
    if (!sent) {
      figmaSessionActionsMod.abortPreparedScreenPlan(resolved, 'TOKEN_TASK_SESSION_DELIVERY_FAILED');
      jsonResponse(res, 409, { error: 'session-busy' });
      return;
    }
    jsonResponse(res, 200, { sent: true, status: sessionsMod.status(key) });
    return;
  }

  meta.prompt = resolved.prompt;
  var st = sessionsMod.start(key, meta);
  sse.pollLoop();
  if (!st || !st.running || st.error) {
    figmaSessionActionsMod.abortPreparedScreenPlan(resolved, 'TOKEN_TASK_SESSION_START_FAILED');
    jsonResponse(res, 409, { error: publicSessionStartError(st) });
    return;
  }
  jsonResponse(res, 200, { sent: true, status: st });
}

function handleSessionStart(req, res) {
  readJsonBody(req).then(function (body) {
    var key = body && typeof body.key === 'string' ? body.key : '';
    if (!validSessionKey(key)) { jsonResponse(res, 400, { error: 'bad-key' }); return; }
    // Task turns enter through the typed action endpoint, where canonical
    // admission, per-stem reservation and final under-lease fencing apply.
    if (key.indexOf('task:') === 0) {
      jsonResponse(res, 400, { error: 'task-session-start-forbidden', detail: 'Submit a canonical task action through /api/tasks/actions.' });
      return;
    }
    if (finalizationsMod.mutationBlocked(null)) {
      jsonResponse(res, 409, { error: 'finalization-active', detail: 'Interactive workspace sessions are blocked while durable task finalization needs recovery.' }); return;
    }
    if (key.indexOf('figma:') === 0) {
      if (body && Object.prototype.hasOwnProperty.call(body, 'prompt')) {
        jsonResponse(res, 400, { error: 'figma-client-prompt-forbidden', detail: 'Send an exact figmaAction; the server owns executable Figma prompts.' });
        return;
      }
      var figmaFields = body && typeof body === 'object' && !Array.isArray(body) ? Object.keys(body).sort() : [];
      if (figmaFields.length !== 2 || figmaFields[0] !== 'figmaAction' || figmaFields[1] !== 'key') {
        jsonResponse(res, 400, { error: 'bad-figma-action-request', detail: 'Figma action requests contain exactly key and figmaAction.' });
        return;
      }
      if (figmaSyncMod.recoveryState() === 'failed') {
        jsonResponse(res, 409, { error: 'figma-sync-recovery-failed', detail: 'Repair the unsafe or invalid Figma sync history or committed generation state and restart the site.' });
        return;
      }
      if (figmaSyncMod.busy()) {
        jsonResponse(res, 409, { error: 'figma-sync-active', detail: 'Wait for Figma sync recovery or the active sync job to finish.' });
        return;
      }
      if (figmaTestJobMod.busy()) {
        jsonResponse(res, 409, { error: 'figma-test-active', detail: 'Wait for the active Figma test or file verification to finish.' });
        return;
      }
      var figmaAdmission = figmaMod.sessionAdmission(key);
      if (figmaAdmission) { jsonResponse(res, 409, figmaAdmission); return; }
      var figmaAction = body && typeof body.figmaAction === 'string' ? body.figmaAction : '';
      return figmaSessionActionsMod.resolveAction(key, figmaAction).then(function (resolved) {
        if (!resolved.ok) { jsonResponse(res, resolved.status || 400, resolved); return; }
        deliverFigmaAction(key, resolved, res);
      });
    }
    var prompt = body && typeof body.prompt === 'string' ? body.prompt : '';
    if (prompt && prompt.length > REQUEST_PROMPT_MAX) { jsonResponse(res, 400, { error: 'bad-prompt' }); return; }
    var meta = {};
    // A "task:<stem>" key carries the stem so the session snapshot can label
    // it and the board can match it by stem.
    if (key.indexOf('task:') === 0) meta.stem = key.slice('task:'.length);
    if (prompt) meta.prompt = prompt;
    var st = sessionsMod.start(key, meta);
    sse.pollLoop();
    if (!st || !st.running || st.error) {
      jsonResponse(res, 409, { error: publicSessionStartError(st) });
      return;
    }
    jsonResponse(res, 200, { status: st });
  }, function (e) { jsonResponse(res, 400, { error: 'bad-json' }); })
  .catch(function (e) { jsonResponse(res, 500, { error: 'internal' }); });
}
function handleSessionSend(req, res) {
  readJsonBody(req).then(function (body) {
    var key = body && typeof body.key === 'string' ? body.key : '';
    if (!validSessionKey(key)) { jsonResponse(res, 400, { error: 'bad-key' }); return; }
    var text = body && typeof body.text === 'string' ? body.text : '';
    if (!text || !text.trim()) { jsonResponse(res, 400, { error: 'no-text' }); return; }
    if (text.length > REQUEST_PROMPT_MAX) { jsonResponse(res, 400, { error: 'bad-prompt' }); return; }
    // Safe live task answers retain their exact lock/session proof. Busy input
    // is queued, while ended/unsafe task and Figma sessions continue read-only;
    // sessions.js owns those boundaries and never derives mutation authority
    // from a persisted transcript.
    var r = sessionsMod.sendOrResume(key, text);
    sse.pollLoop();
    jsonResponse(res, r.error ? 409 : 200, {
      sent: !!r.sent, resumed: !!r.resumed, queued: !!r.queued, busy: !!r.busy,
      error: r.error || null, lockReason: r.lockReason || null,
      status: sessionsMod.status(key)
    });
  }, function (e) { jsonResponse(res, 400, { error: 'bad-json' }); })
  .catch(function (e) { jsonResponse(res, 500, { error: 'internal' }); });
}
function handleSessionCancel(req, res) {
  readJsonBody(req).then(function (body) {
    var key = body && typeof body.key === 'string' ? body.key : '';
    if (!validSessionKey(key)) { jsonResponse(res, 400, { error: 'bad-key' }); return; }
    var ok = sessionsMod.cancel(key);
    sse.pollLoop();
    jsonResponse(res, 200, { canceled: ok });
  }, function (e) { jsonResponse(res, 400, { error: 'bad-json' }); })
  .catch(function (e) { jsonResponse(res, 500, { error: 'internal' }); });
}
function handleSessionEvents(req, res, url) {
  var key = url.searchParams.get('key') || '';
  if (!validSessionKey(key)) { jsonResponse(res, 400, { error: 'bad-key' }); return; }
  var since = parseInt(url.searchParams.get('since') || '0', 10);
  if (!Number.isFinite(since) || since < 0) since = 0;
  jsonResponse(res, 200, { events: sessionsMod.eventsSince(key, since), status: sessionsMod.status(key) });
}
function handleSessionList(req, res) {
  jsonResponse(res, 200, { sessions: sessionsMod.list() });
}

// POST /api/finalizations/resume { stem, expectedRevision, expectedEtag }
// Starts the deterministic finalizer asynchronously. Optimistic concurrency
// prevents a stale Board tab from resuming a marker whose phase already moved.
function handleFinalizationResume(req, res) {
  readJsonBody(req).then(function (body) {
    var stem = body && typeof body.stem === 'string' ? body.stem : '';
    var revision = body && body.expectedRevision;
    var etag = body && typeof body.expectedEtag === 'string' ? body.expectedEtag : '';
    var liveInfo = stem ? sessionsMod.runningInfoForStem(stem) : null;
    if (liveInfo && liveInfo.busy) {
      jsonResponse(res, 409, { error: 'task-session-busy' });
      return;
    }
    var result = finalizationsMod.resume(stem, revision, etag);
    if (!result.ok) {
      jsonResponse(res, result.statusCode || 500, {
        error: result.error || 'finalization-resume-failed',
        current: finalizationsMod.publicProjection(result.current)
      });
      return;
    }
    // Only after marker + optimistic-concurrency validation succeeds may we
    // close a warm idle session. A stale/forged token must have zero effects.
    if (liveInfo && !liveInfo.busy) try { sessionsMod.cancel(liveInfo.key); } catch (e0) {}
    sse.pollLoop();
    jsonResponse(res, 202, {
      accepted: !!result.accepted,
      alreadyRunning: !!result.alreadyRunning,
      finalization: finalizationsMod.publicProjection(result.finalization),
      state: deriveState()
    });
  }, function (e) { jsonResponse(res, 400, { error: 'bad-json' }); })
  .catch(function (e) { jsonResponse(res, 500, { error: 'finalization-resume-failed' }); });
}

// GET /api/git/status — read-only working-tree summary (branch + changed
// files) for the board's "re-run a stopped task" flow. Strictly observational:
// see server/git.js (the server never mutates the tree).
function handleGitStatus(req, res) {
  jsonResponse(res, 200, gitMod.statusSummary());
}

// --- Integrations -> Backend typed API ---------------------------------
function backendResponse(res, result) {
  var status = result && Number.isInteger(result.status) ? result.status : (result && result.ok ? 200 : 500);
  jsonResponse(res, status, result);
}
function handleBackendIntegration(req, res) {
  jsonResponse(res, 200, backendIntegrationMod.get());
}
function handleBackendEnvironmentSelect(req, res) {
  if (backendIntegrationMod.resetting()) { backendResponse(res, { ok: false, status: 409, error: 'writer-lease-conflict' }); return; }
  readJsonBody(req).then(function (body) {
    return sse.serializeStateWrite(function () {
      if (backendIntegrationMod.resetting()) return backendResponse(res, { ok: false, status: 409, error: 'writer-lease-conflict' });
      var result = backendIntegrationMod.select(body);
      if (result.ok) { sse.broadcast('backend-integration', { changed: true }); sse.pollLoop(); }
      backendResponse(res, result);
    });
  }, function () { jsonResponse(res, 400, { ok: false, error: 'bad-json' }); })
  .catch(function () { jsonResponse(res, 500, { ok: false, error: 'internal' }); });
}
function handleBackendEnvironments(req, res) {
  if (backendIntegrationMod.resetting()) { backendResponse(res, { ok: false, status: 409, error: 'writer-lease-conflict' }); return; }
  readJsonBody(req).then(function (body) {
    if (backendIntegrationMod.resetting()) { backendResponse(res, { ok: false, status: 409, error: 'writer-lease-conflict' }); return; }
    var result = backendEnvironmentsMod.mutate(body);
    if (!result.ok) { backendResponse(res, result); return; }
    return sse.serializeStateWrite(function () { backendIntegrationMod.repairSelection(); }).then(function () {
      sse.broadcast('backend-integration', { changed: true }); sse.pollLoop();
      backendResponse(res, result);
    });
  }, function () { jsonResponse(res, 400, { ok: false, error: 'bad-json' }); })
  .catch(function () { jsonResponse(res, 500, { ok: false, error: 'internal' }); });
}
function handleBackendCredential(req, res) {
  if (backendIntegrationMod.resetting()) { backendResponse(res, { ok: false, status: 409, error: 'writer-lease-conflict' }); return; }
  readJsonBody(req).then(function (body) {
    if (backendIntegrationMod.resetting()) { backendResponse(res, { ok: false, status: 409, error: 'writer-lease-conflict' }); return; }
    return backendCredentialsMod.mutate(body).then(function (result) {
      if (result.ok) { sse.broadcast('backend-integration', { changed: true }); sse.pollLoop(); }
      backendResponse(res, result);
    }, function () {
      jsonResponse(res, 500, { ok: false, error: 'credential-state-invalid' });
    });
  }, function () {
    jsonResponse(res, 400, { ok: false, error: 'bad-json' });
  });
}
function handleBackendTest(req, res) {
  if (backendIntegrationMod.resetting()) { backendResponse(res, { ok: false, status: 409, error: 'writer-lease-conflict' }); return; }
  readJsonBody(req).then(function (body) {
    if (backendIntegrationMod.resetting()) return { ok: false, status: 409, error: 'writer-lease-conflict' };
    return contractJobMod.startProbe(body);
  }, function () {
    jsonResponse(res, 400, { ok: false, error: 'bad-json' });
    return null;
  }).then(function (result) {
    if (result) backendResponse(res, result);
  }, function () {
    jsonResponse(res, 500, { ok: false, error: 'internal' });
  });
}
function handleBackendRefresh(req, res) {
  if (backendIntegrationMod.resetting()) { backendResponse(res, { ok: false, status: 409, error: 'writer-lease-conflict' }); return; }
  readJsonBody(req).then(function (body) {
    if (backendIntegrationMod.resetting()) return { ok: false, status: 409, error: 'writer-lease-conflict' };
    return contractJobMod.startRefresh(body);
  }, function () {
    jsonResponse(res, 400, { ok: false, error: 'bad-json' });
    return null;
  }).then(function (result) {
    if (result) backendResponse(res, result);
  }, function () {
    jsonResponse(res, 500, { ok: false, error: 'internal' });
  });
}
function handleBackendIntegrationReset(req, res) {
  readJsonBody(req).then(function (body) {
    return sse.serializeStateWrite(function () { return backendIntegrationMod.reset(body); });
  }, function () {
    return { ok: false, status: 400, error: 'bad-json' };
  }).then(function (result) {
    if (result.ok) { sse.broadcast('backend-integration', { changed: true }); sse.pollLoop(); }
    backendResponse(res, result);
  }, function () {
    jsonResponse(res, 500, { ok: false, error: 'internal' });
  });
}
function handleBackendJob(req, res, url) {
  var match = /^\/api\/backend\/jobs\/(job-[a-f0-9]{32})$/.exec(url.pathname);
  if (!match) { jsonResponse(res, 400, { error: 'bad-job-id' }); return; }
  var job = contractJobMod.get(match[1]);
  if (!job) { jsonResponse(res, 404, { error: 'job-not-found' }); return; }
  jsonResponse(res, 200, { job: job });
}
function handleBackendHistory(req, res, url) {
  var limit = Number(url.searchParams.get('limit') || 20);
  if (!Number.isInteger(limit)) limit = 20;
  var result = contractHistoryMod.list(url.searchParams.get('cursor'), limit);
  jsonResponse(res, result.ok ? 200 : 400, result);
}

function appRunResponse(res, result) {
  result = result || { ok: false, status: 500, error: 'internal' };
  var projected = Object.assign({}, result);
  delete projected.detail;
  delete projected.diagnostic;
  jsonResponse(res, projected.status || (projected.ok ? 200 : 500), projected);
}
function appRunBody(req, res, handler) {
  readJsonBody(req).then(function (body) {
    return handler(body);
  }).then(function (result) {
    appRunResponse(res, result);
  }, function (error) {
    appRunResponse(res, {
      ok: false,
      status: error && error.httpStatus === 413 ? 413 : error && error.httpStatus === 400 ? 400 : 500,
      error: error && error.code === 'bad-json' ? 'bad-json' : 'internal'
    });
  });
}
function appRunQueryAllowed(url, allowed) {
  var keys = Array.from(url.searchParams.keys());
  return keys.every(function (key) {
    return allowed.indexOf(key) >= 0 && url.searchParams.getAll(key).length === 1;
  });
}
function handleAppRunStatus(req, res, url) {
  if (!appRunQueryAllowed(url, [])) {
    appRunResponse(res, { ok: false, status: 400, error: 'bad-app-run-query' }); return;
  }
  appRunResponse(res, appRunnerMod.status());
}
function handleAppRunTargets(req, res, url) {
  if (!appRunQueryAllowed(url, ['platform', 'refresh'])) {
    appRunResponse(res, { ok: false, status: 400, error: 'bad-app-run-query' }); return;
  }
  var platform = url.searchParams.get('platform');
  if (platform !== null && platform !== 'android' && platform !== 'ios') {
    appRunResponse(res, { ok: false, status: 400, error: 'bad-platform' }); return;
  }
  var refreshValue = url.searchParams.get('refresh');
  if (refreshValue !== null && refreshValue !== '1') {
    appRunResponse(res, { ok: false, status: 400, error: 'bad-refresh' }); return;
  }
  var refresh = refreshValue === '1';
  appRunResponse(res, appRunnerMod.targets(platform, refresh));
}
function handleAppRunStart(req, res) { appRunBody(req, res, appRunnerMod.start); }
function handleAppRunCancel(req, res) { appRunBody(req, res, appRunnerMod.cancel); }
function handleAppRunStop(req, res) { appRunBody(req, res, appRunnerMod.stop); }
function handleAppRunRestart(req, res) { appRunBody(req, res, appRunnerMod.restart); }
function handleAppRunScreenshot(req, res) { appRunBody(req, res, appRunnerMod.screenshot); }
function handleAppRunDevicePreview(req, res) { appRunBody(req, res, appRunnerMod.devicePreview); }
function handleAppRunDeviceCreate(req, res) { appRunBody(req, res, appRunnerMod.createDevice); }
function handleAppRunLogs(req, res, url) {
  if (!appRunQueryAllowed(url, ['jobId', 'sessionId', 'cursor', 'limit'])) {
    appRunResponse(res, { ok: false, status: 400, error: 'bad-log-query' }); return;
  }
  appRunResponse(res, appRunnerMod.logs({
    jobId: url.searchParams.get('jobId'),
    sessionId: url.searchParams.get('sessionId'),
    cursor: url.searchParams.get('cursor'),
    limit: url.searchParams.get('limit')
  }));
}
function handleAppRunScreenshotFile(req, res, url) {
  if (!appRunQueryAllowed(url, [])) {
    jsonResponse(res, 400, { ok: false, error: 'bad-screenshot-query' }); return;
  }
  var match = /^\/api\/app-run\/screenshots\/(shot-[a-f0-9]{36})$/.exec(url.pathname);
  var read = match && appRunnerMod.screenshotRead(match[1]);
  if (!read || !read.ok) {
    appRunResponse(res, read || { ok: false, status: 404, error: 'screenshot-not-found' });
    return;
  }
  var hit = read.hit;
  res.writeHead(200, {
    'content-type': 'image/png',
    'content-length': hit.bytes.length,
    'cache-control': 'private, no-store',
    'x-content-type-options': 'nosniff'
  });
  res.end(hit.bytes);
}
function handleAppRunHistory(req, res, url) {
  if (!appRunQueryAllowed(url, ['limit', 'cursor'])) {
    appRunResponse(res, { ok: false, status: 400, error: 'bad-history-query' }); return;
  }
  appRunResponse(res, appRunnerMod.history(url.searchParams.get('limit'), url.searchParams.get('cursor')));
}
function handleAppRunValidationGet(req, res, url) {
  if (!appRunQueryAllowed(url, ['taskStem', 'sessionId'])) {
    appRunResponse(res, { ok: false, status: 400, error: 'bad-validation-query' }); return;
  }
  var taskStem = url.searchParams.get('taskStem');
  var sessionId = url.searchParams.get('sessionId');
  if (!taskSourceMod.safeTaskStem(String(taskStem || '')) ||
      (sessionId !== null && !/^session-[a-f0-9]{36}$/.test(sessionId))) {
    appRunResponse(res, { ok: false, status: 400, error: 'bad-validation-query' }); return;
  }
  appRunResponse(res, appRunnerMod.validationGet({ taskStem: taskStem, sessionId: sessionId }));
}
function handleAppRunValidationSave(req, res) { appRunBody(req, res, appRunnerMod.validationSave); }

function handleApi(req, res, url) {
  if (!isLocalHost(req.headers.host || '')) {
    jsonResponse(res, 403, { ok: false, error: 'bad-host' });
    return true;
  }
  if (req.method === 'POST' && !validateMutationRequest(req, res)) return true;
  if (req.method === 'POST' && /^\/api\/figma\//.test(url.pathname) &&
      url.pathname !== '/api/figma/integration/reset' && figmaIntegrationMod.resetting()) {
    jsonResponse(res, 409, { ok: false, error: 'writer-lease-conflict' }); return true;
  }
  if (url.pathname === '/api/app-run/status' && req.method === 'GET') { handleAppRunStatus(req, res, url); return true; }
  if (url.pathname === '/api/app-run/targets' && req.method === 'GET') { handleAppRunTargets(req, res, url); return true; }
  if (url.pathname === '/api/app-run/start' && req.method === 'POST') { handleAppRunStart(req, res); return true; }
  if (url.pathname === '/api/app-run/cancel' && req.method === 'POST') { handleAppRunCancel(req, res); return true; }
  if (url.pathname === '/api/app-run/stop' && req.method === 'POST') { handleAppRunStop(req, res); return true; }
  if (url.pathname === '/api/app-run/restart' && req.method === 'POST') { handleAppRunRestart(req, res); return true; }
  if (url.pathname === '/api/app-run/screenshot' && req.method === 'POST') { handleAppRunScreenshot(req, res); return true; }
  if (url.pathname === '/api/app-run/devices/preview' && req.method === 'POST') { handleAppRunDevicePreview(req, res); return true; }
  if (url.pathname === '/api/app-run/devices/create' && req.method === 'POST') { handleAppRunDeviceCreate(req, res); return true; }
  if (url.pathname === '/api/app-run/logs' && req.method === 'GET') { handleAppRunLogs(req, res, url); return true; }
  if (/^\/api\/app-run\/screenshots\//.test(url.pathname) && req.method === 'GET') { handleAppRunScreenshotFile(req, res, url); return true; }
  if (url.pathname === '/api/app-run/history' && req.method === 'GET') { handleAppRunHistory(req, res, url); return true; }
  if (url.pathname === '/api/app-run/validation' && req.method === 'GET') { handleAppRunValidationGet(req, res, url); return true; }
  if (url.pathname === '/api/app-run/validation' && req.method === 'POST') { handleAppRunValidationSave(req, res); return true; }
  if (url.pathname === '/api/session/start' && req.method === 'POST')  { handleSessionStart(req, res);      return true; }
  if (url.pathname === '/api/session/send' && req.method === 'POST')   { handleSessionSend(req, res);       return true; }
  if (url.pathname === '/api/session/cancel' && req.method === 'POST') { handleSessionCancel(req, res);     return true; }
  if (url.pathname === '/api/session/events' && req.method === 'GET')  { handleSessionEvents(req, res, url); return true; }
  if (url.pathname === '/api/session/list' && req.method === 'GET')    { handleSessionList(req, res);       return true; }
  if (url.pathname === '/api/finalizations/resume' && req.method === 'POST') { handleFinalizationResume(req, res); return true; }
  if (url.pathname === '/api/git/status' && req.method === 'GET')      { handleGitStatus(req, res);        return true; }
  if (url.pathname === '/api/backend/integration' && req.method === 'GET') { handleBackendIntegration(req, res); return true; }
  if (url.pathname === '/api/backend/integration/reset' && req.method === 'POST') { handleBackendIntegrationReset(req, res); return true; }
  if (url.pathname === '/api/backend/environment/select' && req.method === 'POST') { handleBackendEnvironmentSelect(req, res); return true; }
  if (url.pathname === '/api/backend/environments' && req.method === 'POST') { handleBackendEnvironments(req, res); return true; }
  if (url.pathname === '/api/backend/credential' && req.method === 'POST') { handleBackendCredential(req, res); return true; }
  if (url.pathname === '/api/backend/test' && req.method === 'POST') { handleBackendTest(req, res); return true; }
  if (url.pathname === '/api/backend/refresh' && req.method === 'POST') { handleBackendRefresh(req, res); return true; }
  if (/^\/api\/backend\/jobs\//.test(url.pathname) && req.method === 'GET') { handleBackendJob(req, res, url); return true; }
  if (url.pathname === '/api/backend/history' && req.method === 'GET') { handleBackendHistory(req, res, url); return true; }
  if (url.pathname === '/api/cli/install' && req.method === 'POST')      { handleCliInstall(req, res);     return true; }
  if (url.pathname === '/api/cli/login' && req.method === 'POST')        { handleCliLogin(req, res);       return true; }
  if (url.pathname === '/api/cli/login/code' && req.method === 'POST')   { handleCliLoginCode(req, res);   return true; }
  if (url.pathname === '/api/cli/login/cancel' && req.method === 'POST') { handleCliLoginCancel(req, res); return true; }
  if (url.pathname === '/api/cli/log' && req.method === 'GET')           { handleCliLog(req, res, url);    return true; }
  if (url.pathname === '/api/figma/recheck' && req.method === 'POST')    { handleFigmaRecheck(req, res);   return true; }
  if (url.pathname === '/api/figma/integration' && req.method === 'GET') { handleFigmaIntegration(req, res); return true; }
  if (url.pathname === '/api/figma/integration/reset' && req.method === 'POST') { handleFigmaIntegrationReset(req, res); return true; }
  if (url.pathname === '/api/figma/test' && req.method === 'POST') { handleFigmaTest(req, res); return true; }
  if (url.pathname === '/api/figma/file/verify' && req.method === 'POST') { handleFigmaFileVerify(req, res); return true; }
  if (url.pathname === '/api/figma/file' && req.method === 'POST') { handleFigmaFileSave(req, res); return true; }
  if (url.pathname === '/api/figma/sync/plan' && req.method === 'POST') { handleFigmaSyncPlan(req, res); return true; }
  if (url.pathname === '/api/figma/sync/start' && req.method === 'POST') { handleFigmaSyncStart(req, res); return true; }
  if (url.pathname === '/api/figma/sync/cancel' && req.method === 'POST') { handleFigmaSyncCancel(req, res); return true; }
  if (/^\/api\/figma\/sync\/jobs\//.test(url.pathname) && req.method === 'GET') { handleFigmaSyncJob(req, res, url); return true; }
  if (url.pathname === '/api/figma/sync/history' && req.method === 'GET') { handleFigmaSyncHistory(req, res, url); return true; }
  if (url.pathname === '/api/figma/add-local' && req.method === 'POST')  { handleFigmaAddLocal(req, res);  return true; }
  if (url.pathname === '/api/figma/open-terminal' && req.method === 'POST') { handleFigmaOpenTerminal(req, res); return true; }
  if (url.pathname === '/api/figma/pixel-review' && req.method === 'POST') { handleFigmaPixelReview(req, res); return true; }
  if (url.pathname === '/api/figma/screens' && req.method === 'GET')      { handleFigmaScreens(req, res, url); return true; }
  if (url.pathname === '/api/figma/ship-drift' && req.method === 'GET') { handleFigmaShipDrift(req, res, url); return true; }
  if (url.pathname === '/api/figma/evidence' && req.method === 'GET')    { handleFigmaEvidence(req, res, url); return true; }
  if (url.pathname === '/api/figma/screen-image' && req.method === 'GET') { handleFigmaScreenImage(req, res, url); return true; }
  if (url.pathname === '/api/figma/compare-artifact' && req.method === 'GET') { handleFigmaCompareArtifact(req, res, url); return true; }
  if (url.pathname === '/api/design/overview' && req.method === 'GET') { handleDesignOverview(req, res, url); return true; }
  if (url.pathname === '/api/design/tokens' && req.method === 'GET') { handleDesignTokens(req, res, url); return true; }
  if (url.pathname === '/api/design/tokens/project-only' && req.method === 'GET') { handleDesignProjectOnlyTokens(req, res, url); return true; }
  if (url.pathname === '/api/design/token-sources' && req.method === 'GET') { handleDesignTokenSources(req, res, url); return true; }
  if (url.pathname === '/api/design/token-sources/mutate' && req.method === 'POST') { handleDesignTokenSourcesMutate(req, res); return true; }
  if (/^\/api\/design\/tokens\//.test(url.pathname) && req.method === 'GET') { handleDesignTokenDetail(req, res, url); return true; }
  if (url.pathname === '/api/design/token-mappings' && req.method === 'GET') { handleDesignTokenMappingsGet(req, res); return true; }
  if (url.pathname === '/api/design/token-mappings' && req.method === 'POST') { handleDesignTokenMappingsMutate(req, res); return true; }
  if (url.pathname === '/api/design/component-mappings' && req.method === 'GET') { handleDesignComponentMappingsGet(req, res); return true; }
  if (url.pathname === '/api/design/component-mappings' && req.method === 'POST') { handleDesignComponentMappingsMutate(req, res); return true; }
  if (url.pathname === '/api/design/components' && req.method === 'GET') { handleDesignComponents(req, res, url); return true; }
  if (url.pathname === '/api/design/components/project-only' && req.method === 'GET') { handleDesignProjectOnlyComponents(req, res, url); return true; }
  if (url.pathname === '/api/design/component-image' && req.method === 'GET') { handleDesignComponentImage(req, res, url); return true; }
  if (/^\/api\/design\/components\//.test(url.pathname) && req.method === 'GET') { handleDesignComponent(req, res, url); return true; }
  if (url.pathname === '/api/design/surfaces' && req.method === 'GET') { handleDesignSurfaces(req, res, url); return true; }
  if (/^\/api\/design\/surfaces\//.test(url.pathname) && req.method === 'GET') { handleDesignSurface(req, res, url); return true; }
  if (url.pathname === '/api/design/surface-image' && req.method === 'GET') { handleDesignSurfaceImage(req, res, url); return true; }
  if (url.pathname === '/api/design/tasks/preview' && req.method === 'POST') { handleDesignTaskPreview(req, res); return true; }
  if (url.pathname === '/api/design/tasks/create' && req.method === 'POST') { handleDesignTaskCreate(req, res); return true; }
  if (url.pathname === '/api/design/tasks/cancel' && req.method === 'POST') { handleDesignTaskCancel(req, res); return true; }
  if (url.pathname === '/api/design/compare' && req.method === 'POST') { handleDesignComparison(req, res); return true; }
  if (url.pathname === '/api/api/overview' && req.method === 'GET') { handleApiOverview(req, res, url); return true; }
  if (url.pathname === '/api/api/endpoints' && req.method === 'GET') { handleApiEndpoints(req, res, url); return true; }
  var apiEndpointMatch = /^\/api\/api\/endpoints\/([^/]+)$/.exec(url.pathname);
  if (apiEndpointMatch && req.method === 'GET') { handleApiEndpoint(req, res, url, apiEndpointMatch[1]); return true; }
  if (url.pathname === '/api/api/changes' && req.method === 'GET') { handleApiChanges(req, res, url); return true; }
  if (url.pathname === '/api/api/changes/review' && req.method === 'POST') { handleApiChangeReview(req, res); return true; }
  var apiModelMatch = /^\/api\/api\/models\/([^/]+)$/.exec(url.pathname);
  if (apiModelMatch && req.method === 'GET') { handleApiModel(req, res, url, apiModelMatch[1]); return true; }
  if (url.pathname === '/api/api/diagnostics' && req.method === 'GET') { handleApiDiagnostics(req, res, url); return true; }
  if (url.pathname === '/api/api/tasks/preview' && req.method === 'POST') { handleApiTaskPreview(req, res); return true; }
  if (url.pathname === '/api/api/tasks/create' && req.method === 'POST') { handleApiTaskCreate(req, res); return true; }
  if (url.pathname === '/api/api/tasks/cancel' && req.method === 'POST') { handleApiTaskCancel(req, res); return true; }
  if (url.pathname === '/api/api-mock/status' && req.method === 'GET') { handleApiMockStatus(req, res, url); return true; }
  if (url.pathname === '/api/api-mock/start' && req.method === 'POST') { handleApiMockStart(req, res); return true; }
  if (url.pathname === '/api/api-mock/stop' && req.method === 'POST') { handleApiMockStop(req, res); return true; }
  if (url.pathname === '/api/api-mock/logs' && req.method === 'GET') { handleApiMockLogs(req, res, url); return true; }
  if (url.pathname === '/api/reviewer/status' && req.method === 'GET') { handleReviewerStatus(req, res); return true; }
  if (url.pathname === '/api/reviewer/settings' && req.method === 'POST') { handleReviewerSettings(req, res); return true; }
  if (url.pathname === '/api/reviewer/recheck' && req.method === 'POST') { handleReviewerRecheck(req, res); return true; }
  if (url.pathname === '/api/reviewer/activity' && req.method === 'GET') { handleReviewerActivity(req, res, url); return true; }
  if (url.pathname === '/api/state' && req.method === 'GET')        { handleState(req, res);      return true; }
  if (url.pathname === '/api/architecture/overview' && req.method === 'GET') { handleArchitectureOverview(req, res); return true; }
  if (url.pathname === '/api/architecture/nodes' && req.method === 'GET') { handleArchitectureNodes(req, res, url); return true; }
  var architectureNodeMatch = /^\/api\/architecture\/nodes\/([^/]+)$/.exec(url.pathname);
  if (architectureNodeMatch && req.method === 'GET') { handleArchitectureNode(req, res, url, architectureNodeMatch[1]); return true; }
  if (url.pathname === '/api/architecture/findings' && req.method === 'GET') { handleArchitectureFindings(req, res, url); return true; }
  if (url.pathname === '/api/architecture/graph' && req.method === 'GET') { handleArchitectureGraph(req, res, url); return true; }
  if (url.pathname === '/api/architecture/diff' && req.method === 'GET') { handleArchitectureDiff(req, res, url); return true; }
  if (url.pathname === '/api/architecture/generate' && req.method === 'POST') { handleArchitectureGenerate(req, res); return true; }
  var architectureJobMatch = /^\/api\/architecture\/jobs\/([^/]+)$/.exec(url.pathname);
  if (architectureJobMatch && req.method === 'GET') { handleArchitectureJob(req, res, architectureJobMatch[1]); return true; }
  if (url.pathname === '/api/architecture/tasks/preview' && req.method === 'POST') { handleArchitectureTaskPreview(req, res); return true; }
  if (url.pathname === '/api/architecture/tasks/create' && req.method === 'POST') { handleArchitectureTaskCreate(req, res); return true; }
  if (url.pathname === '/api/architecture/tasks/cancel' && req.method === 'POST') { handleArchitectureTaskCancel(req, res); return true; }
  if (url.pathname === '/api/tasks/log' && req.method === 'GET')    { handleTasksLog(req, res, url); return true; }
  if (url.pathname === '/api/tasks/file' && req.method === 'GET')   { handleTaskFile(req, res, url); return true; }
  if (url.pathname === '/api/tasks/summary' && req.method === 'GET') { handleTasksSummary(req, res, url); return true; }
  var taskSummaryMatch = /^\/api\/tasks\/(TASK_[1-9][0-9]*_[A-Za-z0-9_]+)\/summary$/.exec(url.pathname);
  if (taskSummaryMatch && req.method === 'GET') { handleTaskSummary(req, res, taskSummaryMatch[1]); return true; }
  var taskDetailsStem = taskDetailStem(url.pathname, 'details');
  if (taskDetailsStem && req.method === 'GET') { handleTaskDetails(req, res, url, taskDetailsStem); return true; }
  var taskActivityStem = taskDetailStem(url.pathname, 'activity');
  if (taskActivityStem && req.method === 'GET') { handleTaskActivity(req, res, url, taskActivityStem); return true; }
  var taskArtifactsStem = taskDetailStem(url.pathname, 'artifacts');
  if (taskArtifactsStem && req.method === 'GET') { handleTaskArtifacts(req, res, url, taskArtifactsStem); return true; }
  var taskAdvancedStem = taskDetailStem(url.pathname, 'advanced');
  if (taskAdvancedStem && req.method === 'GET') { handleTaskAdvanced(req, res, url, taskAdvancedStem); return true; }
  var taskCheckpointsStem = taskDetailStem(url.pathname, 'checkpoints');
  if (taskCheckpointsStem && req.method === 'GET') { handleTaskCheckpoints(req, res, url, taskCheckpointsStem); return true; }
  var taskActionPromptStem = taskDetailStem(url.pathname, 'action-prompt');
  if (taskActionPromptStem && req.method === 'GET') { handleTaskActionPrompt(req, res, url, taskActionPromptStem); return true; }
  if (taskActionPromptStem && req.method === 'POST') { handleTaskAnswerPrompt(req, res, taskActionPromptStem); return true; }
  var taskRetryPreviewStem = taskDetailStem(url.pathname, 'retry-preview');
  if (taskRetryPreviewStem && req.method === 'POST') { handleTaskRetryPreview(req, res, taskRetryPreviewStem); return true; }
  if (url.pathname === '/api/tasks/actions' && req.method === 'POST') { handleTaskAction(req, res); return true; }
  if (url.pathname === '/api/tasks/integrity' && req.method === 'GET') { handleTasksIntegrity(req, res); return true; }
  if (url.pathname === '/api/tasks/lock-recovery' && req.method === 'GET') { handleTaskLockRecoveryInspect(req, res, url); return true; }
  if (url.pathname === '/api/tasks/lock-recovery' && req.method === 'POST') { handleTaskLockRecovery(req, res); return true; }
  if (url.pathname === '/api/tasks/drop-impact' && req.method === 'GET') { handleTaskDropImpact(req, res, url); return true; }
  if (url.pathname === '/api/events' && req.method === 'GET')       { handleEvents(req, res);     return true; }
  if (url.pathname === '/api/state-patch' && req.method === 'POST') { handleStatePatch(req, res); return true; }
  if (url.pathname === '/api/reset' && req.method === 'POST')       { handleReset(req, res);      return true; }
  if (url.pathname === '/api/tasks/inbox' && req.method === 'GET') { handleTaskInboxList(req, res); return true; }
  if (url.pathname === '/api/tasks/inbox' && req.method === 'POST') { handleTaskInboxSave(req, res); return true; }
  if (url.pathname === '/api/tasks/inbox/publish' && req.method === 'POST') { handleTaskInboxPublish(req, res); return true; }
  if (url.pathname === '/api/tasks/backlog' && req.method === 'POST') { handleBacklogCreate(req, res); return true; }
  if (url.pathname === '/api/tasks/backlog' && req.method === 'GET') { handleBacklogSource(req, res, url); return true; }
  if (url.pathname === '/api/tasks/backlog/edit' && req.method === 'POST') { handleBacklogEdit(req, res); return true; }
  if (url.pathname === '/api/tasks/intake/retry' && req.method === 'POST') { handleShallowIntakeRetry(req, res); return true; }
  if (url.pathname === '/api/tasks/intake/dismiss' && req.method === 'POST') { handleShallowIntakeDismiss(req, res); return true; }
  return false;
}

// Top-level request dispatch — wired into http.createServer by server.js.
function handle(req, res) {
  var url;
  try { url = new URL(req.url, 'http://localhost'); } catch (e) {
    res.writeHead(400); res.end('bad request'); return;
  }

  if (url.pathname.indexOf('/api/') === 0) {
    if (handleApi(req, res, url)) return;
    res.writeHead(404); res.end('not found');
    return;
  }

  // Root → /site/
  if (url.pathname === '/') {
    res.writeHead(302, { 'location': '/site/' });
    res.end();
    return;
  }

  // Static — everything resolves under ORCHESTRATOR_DIR.
  // GET / HEAD only; reject anything else.
  if (req.method !== 'GET' && req.method !== 'HEAD') {
    res.writeHead(405); res.end('method not allowed');
    return;
  }
  var staticPath = APP_RUN_SCHEMA_ALIASES[url.pathname] || url.pathname;
  var target = staticMod.safeResolve(ORCHESTRATOR_DIR, staticPath);
  if (target === null || target === undefined) { res.writeHead(403); res.end('forbidden'); return; }
  staticMod.serveStatic(req, res, target);
}

module.exports = {
  handle: handle
};
