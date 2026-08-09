'use strict';

// ---------------------------------------------------------------------------
// The owner's pixel-review verdict writer (POST /api/figma/pixel-review).
// A REVIEW_REQUIRED screenshot row (a canvas/glass component the pixel metric is
// blind on, routed by compare-screenshots) is resolved by one owner click:
// pass | minor | fail. This module derives EVERY binding server-side FROM DISK —
// the sealed screenshot report's bytes + run id and the reviewed figma/actual
// artifact pixels — and writes the hash-bound receipt the final evidence bundle
// re-validates (orchestrator/tasks/evidence/pixel-review/<stem>.json). The
// client sends only {stem, screen, theme, verdict, note}: a crafted request can
// never pin hashes the disk does not carry. The same click feeds the
// calibration-corpus INBOX (label + report copy) — data for future threshold
// work; NOTHING auto-applies (bands stay a deliberate owner calibration commit).
// Never calls Figma; local file I/O only (golden invariant).
// ---------------------------------------------------------------------------

var fs     = require('fs');
var path   = require('path');
var crypto = require('crypto');
var paths  = require('./paths');
var fileGuards = require('./file-guards');
var artifactPathContract = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'scripts', 'lib', 'artifact-path.cjs'));

var VERDICTS = { pass: 1, minor: 1, fail: 1 };
var MAX_ARTIFACT_BYTES = 32 * 1024 * 1024;

function sha256File(p) {
  return 'sha256:' + crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
}

function safeArtifactAbs(stem, relPath) {
  if (typeof relPath !== 'string' || !relPath) return null;
  var s = relPath.replace(/\\/g, '/');
  if (path.isAbsolute(s)) return null;
  var parts = s.split('/');
  if (parts.some(function (seg) { return !seg || seg === '.' || seg === '..'; })) return null;
  if (parts[0] !== 'artifacts' || parts[1] !== 'screenshot' || parts[2] !== artifactPathContract.artifactSegment(stem)) return null;
  if (path.extname(s).toLowerCase() !== '.png') return null;
  var safe = fileGuards.realFileUnder(paths.FIGMA_CACHE_DIR, path.resolve(paths.FIGMA_CACHE_DIR, s), { maxBytes: MAX_ARTIFACT_BYTES });
  return safe ? safe.path : null;
}

function atomicWriteJson(file, obj) {
  var tmp = file + '.tmp.' + process.pid + '.' + Date.now();
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(tmp, JSON.stringify(obj, null, 2) + '\n');
  fs.renameSync(tmp, file);
}

// Upsert the calibration-inbox label and copy the sealed report for the sweep. Label
// shape = calibration-labels.schema.json (screen/theme/expect/note/source only — the schema
// is closed); the report copy carries the scores calibrate-thresholds needs. Best-effort:
// an inbox failure must not void the receipt (the gate consumes the receipt, not the inbox).
function appendInboxLabel(stem, row, verdict, note, artifactDirRel, reportFile) {
  try {
    var inboxDir = path.join(paths.PROJECT_ROOT, 'orchestrator', 'figma', 'calibration-corpus', 'inbox');
    var reportsDir = path.join(inboxDir, 'reports');
    fs.mkdirSync(reportsDir, { recursive: true });
    var reportCopy = path.join(reportsDir, 'screenshot-' + stem + '.json');
    if (!fs.existsSync(reportCopy)) fs.copyFileSync(reportFile, reportCopy);
    var labelsFile = path.join(inboxDir, 'labels.json');
    var labels = [];
    try {
      var existing = JSON.parse(fs.readFileSync(labelsFile, 'utf8'));
      labels = Array.isArray(existing && existing.labels) ? existing.labels : [];
    } catch (e) { /* first label */ }
    var next = {
      screen: String(row.screen),
      theme: String(row.theme || row.themeKey || 'primary'),
      expect: verdict === 'fail' ? 'fail' : verdict,
      note: 'owner review click (site)' + (note ? ' — ' + note : ''),
      source: artifactDirRel || undefined
    };
    if (!next.source) delete next.source;
    var key = function (l) { return [l.screen, l.theme || '', l.source || ''].join('\u0001'); };
    var replaced = false;
    for (var i = 0; i < labels.length; i++) {
      if (key(labels[i]) === key(next)) { labels[i] = next; replaced = true; break; }
    }
    if (!replaced) labels.push(next);
    atomicWriteJson(labelsFile, { labels: labels });
  } catch (e) { /* advisory channel — never fail the verdict over it */ }
}

// Apply one owner verdict. cb(err, out). Validation errors carry err.code = 'bad-request' /
// 'not-reviewable' so the HTTP layer can map 4xx vs 5xx.
function applyVerdict(stem, screen, theme, verdict, note, cb) {
  try {
    if (!VERDICTS[verdict]) { return cb(reject('bad-request', "verdict must be 'pass' | 'minor' | 'fail'")); }
    var reportFile = path.join(paths.FIGMA_CACHE_DIR, 'reports', 'screenshot-' + stem + '.json');
    if (!fs.existsSync(reportFile)) return cb(reject('not-reviewable', 'no sealed screenshot report for ' + stem + ' — run the pipeline first'));
    var report = JSON.parse(fs.readFileSync(reportFile, 'utf8'));
    var wantTheme = String(theme || 'primary').toLowerCase();
    var row = (Array.isArray(report.results) ? report.results : []).find(function (r) {
      return r && r.screen === screen && String(r.theme || r.themeKey || 'primary').toLowerCase() === wantTheme;
    });
    if (!row) return cb(reject('not-reviewable', 'screen ' + screen + '/' + wantTheme + ' has no row in the sealed report'));
    if (row.status !== 'REVIEW_REQUIRED') return cb(reject('not-reviewable', 'row ' + screen + '/' + wantTheme + ' is ' + row.status + ', not awaiting review'));
    var arts = row.artifactSet && row.artifactSet.artifacts;
    var figmaRel = arts && arts.figma && (arts.figma.path || arts.figma.file);
    var actualRel = arts && arts.actual && (arts.actual.path || arts.actual.file);
    var figmaAbs = safeArtifactAbs(stem, figmaRel);
    var actualAbs = safeArtifactAbs(stem, actualRel);
    if (!figmaAbs || !actualAbs) return cb(reject('not-reviewable', 'the reviewed artifacts are missing or unsafe — re-run the pipeline'));

    var receiptDir = path.join(paths.PROJECT_ROOT, 'orchestrator', 'tasks', 'evidence', 'pixel-review');
    var receiptFile = path.join(receiptDir, stem + '.json');
    var receipt = { schemaVersion: 1, stem: stem, rows: [] };
    try {
      var prior = JSON.parse(fs.readFileSync(receiptFile, 'utf8'));
      if (prior && Array.isArray(prior.rows)) receipt.rows = prior.rows;
    } catch (e) { /* first verdict for this stem */ }
    var entry = {
      screen: String(screen),
      theme: wantTheme,
      verdict: verdict,
      note: String(note || '').slice(0, 500),
      pipelineRunId: String(report.pipelineRunId || ''),
      reportHash: sha256File(reportFile),
      figmaHash: sha256File(figmaAbs),
      actualHash: sha256File(actualAbs),
      at: new Date().toISOString(),
      by: 'site-owner'
    };
    var kept = receipt.rows.filter(function (r) {
      return !(r && r.screen === entry.screen && String(r.theme || 'primary').toLowerCase() === wantTheme);
    });
    kept.push(entry);
    receipt.rows = kept;
    atomicWriteJson(receiptFile, receipt);

    var artifactDirRel = typeof figmaRel === 'string' ? figmaRel.replace(/\\/g, '/').split('/').slice(0, -1).join('/') : null;
    appendInboxLabel(stem, row, verdict, entry.note, artifactDirRel, reportFile);
    cb(null, { ok: true, receipt: path.relative(paths.PROJECT_ROOT, receiptFile), row: entry });
  } catch (e) {
    cb(e);
  }
}

function reject(code, detail) {
  var err = new Error(detail);
  err.code = code;
  return err;
}

module.exports = { applyVerdict: applyVerdict };
