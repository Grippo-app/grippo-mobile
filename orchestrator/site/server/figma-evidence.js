'use strict';

// ---------------------------------------------------------------------------
// Read-only, path-safe summaries of per-task Figma evidence reports. The site
// never streams arbitrary .cache files and never accepts a report path from the
// browser; callers pass only a task stem and this module synthesizes the known
// report filenames under orchestrator/.cache/figma/reports/.
// ---------------------------------------------------------------------------

var fs     = require('fs');
var path   = require('path');
var crypto = require('crypto');
var paths  = require('./paths');
var locks  = require('./locks');
var fileGuards = require('./file-guards');
var taskSource = require('./task-source');
var designParser = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'scripts', 'design-parser.cjs'));
var artifactPathContract = require(path.join(paths.ORCHESTRATOR_DIR, 'figma', 'scripts', 'lib', 'artifact-path.cjs'));

var REPORTS_DIR = path.join(paths.FIGMA_CACHE_DIR, 'reports');
var COMPARE_ARTIFACTS_DIR = path.join(paths.FIGMA_CACHE_DIR, 'artifacts', 'screenshot');
var REPORT_PREFIXES = ['screen-cache', 'check-spec', 'capture-config', 'census', 'spec', 'spec-compare', 'screenshot'];
var MAX_TOP_ISSUES = 5;
var MAX_GLOBAL_ISSUES = 30;
// Done "Figma" tab shows EVERY built component (task-detail IA redesign). The full
// readEvidence → /api/figma/evidence payload carries one entry per screen×theme; this is the
// presentational row cap (readEvidenceLite carries counts only, so it's unaffected). A hard
// ceiling stays so an untrusted report can't bloat the response. The per-row trust gate
// (trustedFinalVisualEvidence/compareArtifactFile) is independent of this cap.
var MAX_VISUAL_CHECKS = 48;
var MAX_VISUAL_SCAN = 256;   // hard bound on result rows inspected from an untrusted report
var MAX_ZONES = 12;          // per-screen Figma-node zones surfaced to the client (worst first)
var MAX_ZONE_SCAN = 512;     // hard bound on how many raw zones we inspect from an untrusted report
var MAX_ARTIFACT_REFS = 2048;
var ARTIFACT_ID_RE = /^[A-Za-z0-9_-]{1,180}$/;
var SHA256_RE = /^sha256:[a-f0-9]{64}$/i;
var ARTIFACT_FILE_BY_KIND = { figma: 'figma.png', actual: 'actual.png', diff: 'diff.png', overlay: 'overlay.png' };
var INPUT_HASH_MAX_BYTES = 64 * 1024 * 1024;
var liteCache = Object.create(null);
var artifactIndexCache = Object.create(null);
var artifactStampCache = Object.create(null);

function sha256Bytes(buf) {
  return 'sha256:' + crypto.createHash('sha256').update(buf).digest('hex');
}

function fileHash(file) {
  try {
    return sha256Bytes(fs.readFileSync(file));
  } catch (e) {
    return null;
  }
}

function readJsonBuffer(file) {
  try {
    var buf = fs.readFileSync(file);
    return { data: JSON.parse(buf.toString('utf8')), hash: sha256Bytes(buf), bytes: buf };
  } catch (e) {
    return null;
  }
}

// Steady-state perf memo: the SSE poll re-summarizes every task's reports each
// tick, but a report only actually changes when a pipeline run rewrites it.
// Memoize parse results keyed by absolute path, validated by {mtimeMs,size}
// (stat-first — same stamp discipline as artifactStampCache). The parsed data
// and the file hash ALWAYS come from the same single readFileSync buffer, so a
// a concurrent atomic rename can never produce a torn data/hash pair.
// Bounded: small insertion-order LRU so a long-lived server can't grow without
// limit. TRUST NOTE: the byte-serving path (artifactIndexForReport →
// compareArtifactFile) deliberately does NOT use this memo — it re-reads and
// re-hashes the report + artifact bytes on every serve.
var PARSE_MEMO_MAX = 200;
var parseMemo = new Map();   // absPath -> { mtimeMs, size, data, hash }
function readJsonCached(file) {
  var st;
  try {
    st = fs.statSync(file);
    if (!st.isFile()) { parseMemo.delete(file); return null; }
  } catch (e) {
    parseMemo.delete(file);
    return null;
  }
  var hit = parseMemo.get(file);
  if (hit && hit.mtimeMs === st.mtimeMs && hit.size === st.size) {
    // Map iterates in insertion order — re-insert so recently-used entries sit
    // at the tail and the eviction below always drops the least-recently used.
    parseMemo.delete(file);
    parseMemo.set(file, hit);
    return hit;
  }
  var loaded = readJsonBuffer(file);   // ONE read: data + hash from the same bytes
  if (!loaded) { parseMemo.delete(file); return null; }
  var entry = { mtimeMs: st.mtimeMs, size: st.size, data: loaded.data, hash: loaded.hash };
  parseMemo.set(file, entry);
  while (parseMemo.size > PARSE_MEMO_MAX) {
    parseMemo.delete(parseMemo.keys().next().value);
  }
  return entry;
}

function trimText(value, max) {
  if (value == null) return null;
  var s = String(value);
  if (s.length <= max) return s;
  return s.slice(0, Math.max(0, max - 3)) + '...';
}

function publicPath(value) {
  if (typeof value !== 'string' || !value) return null;
  var s = value.replace(/\\/g, '/');
  if (!path.isAbsolute(value)) return trimText(s, 240);
  var abs = path.resolve(value);
  var root = paths.PROJECT_ROOT;
  if (abs === root || abs.indexOf(root + path.sep) === 0) {
    return trimText(path.relative(root, abs).replace(/\\/g, '/'), 240);
  }
  return trimText(path.basename(abs), 240);
}

function artifactRef(ref) {
  if (!ref || typeof ref !== 'object') return null;
  if (typeof ref.id !== 'string' || !ref.id || typeof ref.path !== 'string' || !ref.path) return null;
  return {
    id: trimText(ref.id, 180),
    kind: trimText(ref.kind || '', 40),
    hash: typeof ref.hash === 'string' ? trimText(ref.hash, 90) : null,
    bytes: typeof ref.bytes === 'number' ? ref.bytes : null,
    width: typeof ref.width === 'number' ? ref.width : null,
    height: typeof ref.height === 'number' ? ref.height : null,
    mime: trimText(ref.mime || '', 80)
  };
}

function summarizeArtifactSet(set) {
  if (!set || typeof set !== 'object') return null;
  var artifacts = {};
  var raw = set.artifacts && typeof set.artifacts === 'object' ? set.artifacts : {};
  ['figma', 'actual', 'diff', 'overlay'].forEach(function (kind) {
    var ref = artifactRef(raw[kind]);
    if (ref) artifacts[kind] = ref;
  });
  var keys = Object.keys(artifacts);
  if (!keys.length) return null;
  return {
    schemaVersion: typeof set.schemaVersion === 'number' ? set.schemaVersion : null,
    id: trimText(set.id || '', 180),
    screen: trimText(set.screen || '', 120),
    theme: trimText(set.theme || set.themeKey || '', 40),
    status: rowStatus(set.status || 'UNKNOWN'),
    score: typeof set.score === 'number' ? set.score : null,
    coverage: typeof set.coverage === 'number' ? set.coverage : null,
    dimensions: set.dimensions && typeof set.dimensions === 'object' ? {
      width: typeof set.dimensions.width === 'number' ? set.dimensions.width : null,
      height: typeof set.dimensions.height === 'number' ? set.dimensions.height : null
    } : null,
    manifest: artifactRef(set.manifest),
    artifacts: artifacts
  };
}

function reportPath(name, stem) {
  return path.join(REPORTS_DIR, name + '-' + stem + '.json');
}

function reportPublicPath(name, stem) {
  return 'reports/' + name + '-' + stem + '.json';
}

function statusRank(status) {
  var s = String(status || '').toUpperCase();
  if (s === 'BLOCKER' || s === 'FAIL' || s === 'ERROR') return 5;
  if (s === 'INCOMPLETE' || s === 'REVIEW_REQUIRED') return 4;
  if (s === 'WARN' || s === 'WARNING' || s === 'MAJOR') return 3;
  if (s === 'MINOR' || s === 'SKIPPED') return 2;
  if (s === 'MISSING') return 1;
  if (s === 'PASS') return 0;
  return 5;
}

function canonicalStatus(status) {
  var s = String(status || '').toUpperCase();
  if (s === 'WARNING') return 'WARN';
  if (s === 'FAIL' || s === 'ERROR') return 'BLOCKER';
  if (s === 'PASS' || s === 'WARN' || s === 'MINOR' || s === 'MAJOR' ||
      s === 'SKIPPED' || s === 'INCOMPLETE' || s === 'REVIEW_REQUIRED' ||
      s === 'BLOCKER' || s === 'MISSING') return s;
  return 'UNKNOWN';
}

// A per-SCREEN / per-zone / per-artifactSet row can carry a specific compare sub-status (WHY a
// screen bailed) that the Done view renders as a friendly diagnostic badge. canonicalStatus
// collapses those to 'UNKNOWN' — correct for the OVERALL severity rollup (effectiveOverall/
// overallOf), wrong for a row's identity — so preserve the raw value for the known diagnostic
// statuses the client (evidenceStatusClass/evidenceStatusLabel + the `status.<raw>` i18n keys)
// knows how to render, and canonicalize everything else. statusRank treats these as a problem
// (unknown-status fallback rank 5), so problemCount / grouping are unaffected either way.
// The compare sub-statuses that CAN land on a per-screen row and that the client renders with a
// friendly `status.<raw>` badge. Every entry here has a matching i18n label. (PRIMARY_STATE_UNCONFIRMED
// is deliberately absent: that condition sets the row status to REVIEW_REQUIRED — the raw string only
// appears in an issue, never on a row — so preserving it would be dead + label-less.)
var DIAGNOSTIC_ROW_STATUSES = {
  ASPECT_MISMATCH: 1, MISSING_CAPTURE: 1, MISSING_ORACLE: 1, UNREPRESENTABLE_OVERLAY: 1,
  LOW_CONTENT_ORACLE: 1, STALE_CAPTURE: 1, DUPLICATE_CAPTURE: 1, CAPTURE_LOCALE_MISMATCH: 1,
  NO_INDEXED_SCREENS: 1,
};
function rowStatus(status) {
  var s = String(status || '').toUpperCase();
  return DIAGNOSTIC_ROW_STATUSES[s] ? s : canonicalStatus(status || 'UNKNOWN');
}

function issueSeverity(issue) {
  return String((issue && issue.severity) || '').toUpperCase();
}

function issueCounts(issues) {
  var rows = Array.isArray(issues) ? issues : [];
  var out = { blockingCount: 0, warningCount: 0, reviewCount: 0 };
  for (var i = 0; i < rows.length; i++) {
    var s = issueSeverity(rows[i]);
    if (s === 'BLOCKER' || s === 'ERROR' || s === 'FAIL') out.blockingCount++;
    else if (s === 'WARN' || s === 'WARNING' || s === 'MINOR' || s === 'MAJOR' || s === 'REVIEW_REQUIRED') {
      out.warningCount++;
      if (s === 'REVIEW_REQUIRED') out.reviewCount++;
    }
  }
  return out;
}

function effectiveOverall(raw, issues) {
  var counts = issueCounts(issues);
  var base = canonicalStatus(raw || 'UNKNOWN');
  if (counts.blockingCount > 0 && statusRank(base) < statusRank('BLOCKER')) return 'BLOCKER';
  if (counts.warningCount > 0 && statusRank(base) < statusRank('WARN')) return 'WARN';
  return base;
}

function overallOf(rows) {
  var max = -1;
  var status = 'MISSING';
  for (var i = 0; i < rows.length; i++) {
    var s = rows[i] && rows[i].overall;
    var r = statusRank(s);
    if (r > max) { max = r; status = canonicalStatus(s); }
  }
  return max < 0 ? 'MISSING' : status;
}

function summarizeIssue(issue, reportName) {
  // Guard: issues[] comes from an untrusted report — normalize null/primitive
  // elements to an empty object before dereferencing (same contract as the
  // results[] scan in screenshotVisualChecks).
  if (!issue || typeof issue !== 'object') issue = {};
  return {
    report: reportName || issue.reportName || null,
    severity: canonicalStatus(issue.severity || 'UNKNOWN'),
    issueKind: trimText(issue.issueKind || issue.kind || 'ISSUE', 80),
    message: trimText(issue.message || '', 500),
    screen: trimText(issue.screen || '', 120),
    theme: trimText(issue.theme || '', 40),
    file: publicPath(issue.file),
    path: publicPath(issue.path),
    stableId: trimText(issue.stableId || '', 120),
    elementName: trimText(issue.elementName || '', 160),
    component: trimText(issue.component || issue.name || '', 160),
    artifactSet: summarizeArtifactSet(issue.artifactSet)
  };
}

function sortedIssues(issues, reportName) {
  return (Array.isArray(issues) ? issues : [])
    .map(function (issue) { return summarizeIssue(issue, reportName); })
    .sort(function (a, b) {
      var d = statusRank(b.severity) - statusRank(a.severity);
      if (d) return d;
      return [
        a.report || '', a.screen || '', a.theme || '', a.issueKind || '', a.file || '', a.path || '', a.message || ''
      ].join('\u0001').localeCompare([
        b.report || '', b.screen || '', b.theme || '', b.issueKind || '', b.file || '', b.path || '', b.message || ''
      ].join('\u0001'));
    });
}

function countByStatus(rows) {
  var out = { total: 0 };
  if (!Array.isArray(rows)) return out;
  for (var i = 0; i < rows.length; i++) {
    var key = String((rows[i] && rows[i].status) || 'unknown').toLowerCase();
    out[key] = (out[key] || 0) + 1;
    out.total++;
  }
  return out;
}

function facetsFor(name, data) {
  if (!data || typeof data !== 'object') return null;
  if (name === 'screen-cache') return { screens: countByStatus(data.screens) };
  if (name === 'check-spec') return { files: countByStatus(data.files) };
  if (name === 'census') return { components: countByStatus(data.components || data.results) };
  if (name === 'spec-compare') return { comparisons: countByStatus(data.comparisons) };
  if (name === 'screenshot') return { screenshots: countByStatus(data.results) };
  if (name === 'evidence') return {
    // Guard: a non-array `reports` in an untrusted bundle must not crash (`.map`
    // on a truthy non-array throws); countByStatus itself is already array-guarded.
    reports: countByStatus((Array.isArray(data.reports) ? data.reports : []).map(function (r) {
      return { status: (r && (r.overall || (r.exists === false ? 'MISSING' : 'UNKNOWN'))) };
    }))
  };
  return null;
}

// Per-Figma-node zones for one screenshot result. The report is written by the
// trusted pipeline but treated as untrusted here (mirrors the rest of this module):
// bounded scan, trimmed strings, number-validated scores, worst (lowest SSIM) first,
// capped. Returns null when there are no usable zones.
function summarizeZones(zones) {
  if (!Array.isArray(zones) || !zones.length) return null;
  var out = [];
  var scan = Math.min(zones.length, MAX_ZONE_SCAN);
  for (var i = 0; i < scan; i++) {
    var z = zones[i];
    if (!z || typeof z !== 'object') continue;
    // bboxPx is intentionally NOT forwarded — the client renders name + score bar
    // only, so the pixel rect would be dead weight on the wire.
    out.push({
      stableId: trimText(z.stableId || '', 120),
      role: trimText(z.role || '', 60),
      name: trimText(z.name || '', 120),
      ssim: typeof z.ssim === 'number' && isFinite(z.ssim) ? z.ssim : null,
      deltaE: typeof z.deltaE === 'number' && isFinite(z.deltaE) ? z.deltaE : null,
      status: rowStatus(z.status || 'UNKNOWN')
    });
  }
  out.sort(function (a, b) {
    var sa = a.ssim == null ? 2 : a.ssim, sb = b.ssim == null ? 2 : b.ssim;   // nulls last
    return sa - sb;
  });
  return out.length ? out.slice(0, MAX_ZONES) : null;
}

function screenshotVisualChecks(data, reportHash) {
  var results = Array.isArray(data && data.results) ? data.results : [];
  var totalResults = results.length;
  var thresholds = data && data.thresholds && typeof data.thresholds === 'object' ? data.thresholds : {};
  var pixelGate = ['strict', 'advisory', 'off'].indexOf(String(thresholds.pixelGate || '')) >= 0 ? String(thresholds.pixelGate) : null;
  // Bound the per-row work (each row runs the zone scan + sort): a corrupt report
  // with a huge results[] must not amplify into O(N×MAX_ZONE_SCAN). The output is
  // separately capped at MAX_VISUAL_CHECKS; `total` below stays the true count.
  var rows = results.slice(0, MAX_VISUAL_SCAN).map(function (row) {
    // Untrusted-report guard: a null/primitive element in results[] must not
    // crash the summary (JSON.stringify([undefined]) serializes to [null], so
    // the shape is reachable without hand-editing). Mirrors the per-entry guard
    // in summarizeZones/countByStatus — normalize, then read fields safely.
    if (!row || typeof row !== 'object') row = {};
    // Keep EVERY comparison row, even one with no artifact set. A failed comparison
    // that bailed early (ASPECT_MISMATCH / MISSING_CAPTURE) produces no images, but its
    // per-screen STATUS is exactly the diagnostic the Done view needs ("which screen
    // broke and how"). The client renders such a row as a status line (badge + reason)
    // with no images; the trust gate still governs whether any images that DO exist are
    // shown. artifactCount / withArtifacts below count only rows that carry real images.
    var set = summarizeArtifactSet(row && row.artifactSet);
    if (set) set.reportHash = reportHash || null;
    var status = rowStatus(row.status || 'UNKNOWN');
    var pixelStatus = row.pixelStatus ? rowStatus(row.pixelStatus) : null;
    if (pixelStatus === 'UNKNOWN' || pixelStatus === status) pixelStatus = null;
    return {
      screen: trimText(row.screen || '', 120),
      theme: trimText(row.theme || row.themeKey || '', 40),
      status: status,
      pixelStatus: pixelStatus,
      pixelGate: pixelStatus ? pixelGate : null,
      score: typeof row.score === 'number' && isFinite(row.score) ? row.score : null,
      coverage: typeof row.coverage === 'number' && isFinite(row.coverage) ? row.coverage : null,
      reason: trimText(row.reason || row.error || '', 260),
      // Advisory per-element axes (masked-ssim-luma-v2). Additive; never gate trust.
      colorStatus: (row.colorStatus === 'PASS' || row.colorStatus === 'REVIEW') ? row.colorStatus : null,
      worstRegionDeltaE: typeof row.worstRegionDeltaE === 'number' && isFinite(row.worstRegionDeltaE) ? row.worstRegionDeltaE : null,
      zones: summarizeZones(row.zones),
      artifactSet: set || null
    };
  });
  rows.sort(function (a, b) {
    var d = statusRank(b.pixelStatus || b.status) - statusRank(a.pixelStatus || a.status);
    if (d) return d;
    return String(a.screen || '').localeCompare(String(b.screen || '')) ||
      String(a.theme || '').localeCompare(String(b.theme || ''));
  });
  var artifactCount = rows.reduce(function (sum, row) {
    return sum + Object.keys((row.artifactSet && row.artifactSet.artifacts) || {}).length;
  }, 0);
  var problemCount = rows.filter(function (row) { return statusRank(row && (row.pixelStatus || row.status)) > 0; }).length;
  return {
    total: totalResults,
    withArtifacts: rows.filter(function (row) { return !!row.artifactSet; }).length,
    artifactCount: artifactCount,
    problemCount: problemCount,
    pixelGate: pixelGate,
    reportHash: reportHash || null,
    entries: rows.slice(0, MAX_VISUAL_CHECKS),
    truncated: rows.length > MAX_VISUAL_CHECKS
  };
}

function receiptReportPath(name, stem) {
  return path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'evidence', 'figma-ship', stem, name + '-' + stem + '.json');
}

function isDoneStem(stem) {
  try { return fs.existsSync(path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'done', stem + '.md')); } catch (e) { return false; }
}

function summarizeReport(name, stem) {
  var file = reportPath(name, stem);
  var source = 'live';
  var exists = false;
  var st = null;
  try { st = fs.statSync(file); exists = st.isFile(); } catch (e) {}
  // Receipts-first for a DONE task's census: the live report is gitignored cache that a
  // later re-pull/eviction deletes, while ship-done snapshots the sealed census into the
  // committed receipts (tasks/evidence/figma-ship/<stem>/). The bundle pinned the sealed
  // bytes' hash, so the receipt copy re-verifies through the same evidenceHashDrift check —
  // a tampered receipt reads as integrity drift, never as trusted. Census only: the other
  // required reports keep their live-read semantics (their receipt copies are verify-done's
  // re-bind inputs, not display substitutes).
  if (!exists && name === 'census' && isDoneStem(stem)) {
    var receiptFile = receiptReportPath(name, stem);
    try {
      var rst = fs.statSync(receiptFile);
      if (rst.isFile()) { file = receiptFile; st = rst; exists = true; source = 'receipt'; }
    } catch (e) {}
  }
  if (!exists) {
    return {
      name: name,
      present: false,
      exists: false,
      overall: 'MISSING',
      rawOverall: 'MISSING',
      rank: statusRank('MISSING'),
      reportPath: reportPublicPath(name, stem),
      topIssues: [],
      issueCount: 0,
      truncatedIssues: false
    };
  }

  // Single read: data + hash come from one readFileSync buffer via the
  // memo, so a concurrent atomic rename can never pair OLD rows with the NEW
  // file's hash. The memo also makes the steady-state poll stat-only.
  var loaded = readJsonCached(file);
  var data = loaded ? loaded.data : null;
  if (!data || typeof data !== 'object') {
    return {
      name: name,
      present: true,
      exists: true,
      unreadable: true,
      overall: 'BLOCKER',
      rawOverall: 'UNREADABLE',
      rank: statusRank('BLOCKER'),
      reportPath: reportPublicPath(name, stem),
      hash: fileHash(file),
      mtimeMs: st ? st.mtimeMs : 0,
      blockingCount: 1,
      warningCount: 0,
      issueCount: 1,
      topIssues: [{
        report: name,
        severity: 'BLOCKER',
        issueKind: 'REPORT_UNREADABLE',
        message: 'Report JSON could not be parsed.',
        screen: null,
        theme: null,
        file: reportPublicPath(name, stem),
        path: null,
        stableId: null,
        elementName: null,
        component: null
      }],
      truncatedIssues: false
    };
  }

  var reportHash = loaded.hash;
  var rawIssues = Array.isArray(data.issues) ? data.issues : [];
  var counts = issueCounts(rawIssues);
  var top = sortedIssues(rawIssues, name);
  var overall = effectiveOverall(data.overall, rawIssues);
  var visualChecks = name === 'screenshot' ? screenshotVisualChecks(data, reportHash) : null;
  return {
    name: name,
    present: true,
    exists: true,
    overall: overall,
    rawOverall: canonicalStatus(data.overall || 'UNKNOWN'),
    rank: statusRank(overall),
    stage: typeof data.stage === 'string' ? data.stage : null,
    mode: typeof data.mode === 'string' ? data.mode : null,
    taskStem: typeof data.taskStem === 'string' ? data.taskStem : null,
    pipelineRunId: typeof data.pipelineRunId === 'string' ? data.pipelineRunId : null,
    generatedAt: typeof data.generatedAt === 'string' ? data.generatedAt : null,
    reportPath: reportPublicPath(name, stem),
    source: source,
    hash: reportHash,
    mtimeMs: st ? st.mtimeMs : 0,
    blockingCount: typeof data.blockingCount === 'number' ? data.blockingCount : counts.blockingCount,
    warningCount: typeof data.warningCount === 'number' ? data.warningCount : counts.warningCount,
    reviewCount: counts.reviewCount,
    issueCount: rawIssues.length,
    topIssues: top.slice(0, MAX_TOP_ISSUES),
    truncatedIssues: top.length > MAX_TOP_ISSUES,
    facets: facetsFor(name, data),
    visualChecks: visualChecks,
    requiredReports: name === 'evidence' && Array.isArray(data.requiredReports)
      ? data.requiredReports.filter(function (r) { return typeof r === 'string' && REPORT_PREFIXES.indexOf(r) >= 0; })
      : null
  };
}

function newestIso(rows) {
  var best = null;
  var bestTs = 0;
  for (var i = 0; i < rows.length; i++) {
    var iso = rows[i] && rows[i].generatedAt;
    var ts = iso ? Date.parse(iso) : NaN;
    if (isFinite(ts) && ts > bestTs) { bestTs = ts; best = iso; }
  }
  return best;
}

function unique(values) {
  var seen = Object.create(null), out = [];
  for (var i = 0; i < values.length; i++) {
    var v = values[i];
    if (!v || seen[v]) continue;
    seen[v] = true;
    out.push(v);
  }
  return out;
}

function uniqueIssues(issues) {
  var seen = Object.create(null), out = [];
  for (var i = 0; i < issues.length; i++) {
    var it = issues[i] || {};
    var key = [
      it.severity || '', it.issueKind || '', it.message || '', it.screen || '',
      it.theme || '', it.file || '', it.path || '', it.stableId || '',
      it.elementName || '', it.component || ''
    ].join('\u0001');
    if (seen[key]) continue;
    seen[key] = true;
    out.push(it);
  }
  return out;
}

function evidenceHashDrift(bundleData, reports) {
  var hashes = bundleData && bundleData.inputHashes;
  if (!hashes || typeof hashes !== 'object' || Array.isArray(hashes)) {
    return bundleData ? [{ name: 'evidence', reason: 'missing-input-hashes' }] : [];
  }
  var drift = [];
  reports.forEach(function (report) {
    var expected = hashes[report.reportPath] || hashes['orchestrator/.cache/figma/' + report.reportPath];
    if (!expected) {
      if (report.present) drift.push({ name: report.name, reason: 'missing-input-hash' });
      return;
    }
    if (!report.present) {
      drift.push({ name: report.name, reason: 'missing' });
    } else if (report.hash && report.hash !== expected) {
      drift.push({ name: report.name, reason: 'hash-mismatch' });
    }
  });
  return drift;
}

function trustedInputFile(inputPath) {
  var p = String(inputPath || '');
  if (!p) return null;
  var candidates = path.isAbsolute(p)
    ? [p]
    : [path.resolve(paths.PROJECT_ROOT, p), path.resolve(paths.FIGMA_CACHE_DIR, p)];
  var roots = [paths.PROJECT_ROOT, paths.FIGMA_CACHE_DIR];
  for (var i = 0; i < candidates.length; i++) {
    for (var j = 0; j < roots.length; j++) {
      var safe = fileGuards.realFileUnder(roots[j], candidates[i], { maxBytes: INPUT_HASH_MAX_BYTES });
      if (safe) return safe;
    }
  }
  return null;
}

function trustedInputStamp(inputPath) {
  var safe = trustedInputFile(inputPath);
  return safe ? fileStamp(safe.path) : 'untrusted-or-missing';
}

function trustedInputHash(inputPath) {
  var safe = trustedInputFile(inputPath);
  return safe ? { path: safe.path, hash: fileHash(safe.path) } : null;
}

function reportInputHashDrift(stem, reports, requiredReports) {
  var required = Object.create(null);
  (requiredReports || []).forEach(function (name) { required[name] = true; });
  var drift = [];
  reports.forEach(function (report) {
    if (!report || !report.present || !required[report.name]) return;
    var loaded = readJsonCached(reportPath(report.name, stem));
    var hashes = loaded && loaded.data && loaded.data.inputHashes;
    if (!hashes || typeof hashes !== 'object' || Array.isArray(hashes)) return;
    Object.keys(hashes).forEach(function (input) {
      // `virtual:` keys are recompute-only digest pins (e.g. the census registry-consult
      // digest), not file paths — evidence-bundle owns their re-verification; re-hashing
      // them as files here would flag every such report as untrusted-or-missing.
      if (String(input).indexOf('virtual:') === 0) return;
      var expected = hashes[input];
      if (typeof expected !== 'string' || !expected.startsWith('sha256:')) return;
      var actual = trustedInputHash(input);
      if (!actual) {
        drift.push({ name: report.name, reason: 'input-hash-untrusted-or-missing', file: publicPath(input) || input });
        return;
      }
      if (actual.hash !== expected) drift.push({ name: report.name, reason: 'input-hash-mismatch', file: publicPath(actual.path) || input });
    });
  });
  return drift;
}

function taskFilesForDesignHash(stem) {
  var files = [
    path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'todo', stem + '.md'),
    path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'backlog', stem + '.md'),
    path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'pending', stem + '.questions.md')
  ].filter(function (p) { try { return fs.existsSync(p); } catch (e) { return false; } });
  if (!files.length) {
    var done = path.join(paths.ORCHESTRATOR_DIR, 'tasks', 'done', stem + '.md');
    try { if (fs.existsSync(done)) files.push(done); } catch (e) {}
  }
  return files;
}

function currentDesignSourceHash(stem) {
  var bodies = [];
  var unavailable = false;
  taskFilesForDesignHash(stem).forEach(function (file) {
    var column = path.basename(path.dirname(file));
    var task = taskSource.readTask(column, stem);
    if (!task) { unavailable = true; return; }
    bodies.push(task.text);
  });
  return unavailable || !bodies.length
    ? { ok: false, hash: null }
    : { ok: true, hash: designParser.parseDesignSources(bodies).sourceHash };
}

function designSourceHashDrift(stem, reports) {
  var screenCache = reports.find(function (r) { return r && r.name === 'screen-cache' && r.present; });
  if (!screenCache) return [];
  var loaded = readJsonCached(reportPath('screen-cache', stem));
  var recorded = loaded && loaded.data && loaded.data.inputs && loaded.data.inputs.designSourceHash;
  if (!recorded || typeof recorded !== 'string') return [{ name: 'screen-cache', reason: 'design-source-hash-missing' }];
  var current = currentDesignSourceHash(stem);
  if (!current.ok) return [{ name: 'screen-cache', reason: 'design-source-unavailable' }];
  return current.hash === recorded ? [] : [{ name: 'screen-cache', reason: 'design-source-hash-mismatch' }];
}

function fileStamp(file) {
  try {
    var st = fs.statSync(file);
    return st.isFile() ? (st.mtimeMs + ':' + st.size) : 'not-file';
  } catch (e) {
    return 'missing';
  }
}

function evidenceExternalInputsStamp(stem) {
  var seen = Object.create(null), parts = [];
  function add(label, file) {
    if (!file || seen[file]) return;
    seen[file] = true;
    parts.push(label + ':' + fileStamp(file));
  }
  taskFilesForDesignHash(stem).forEach(function (file) { add('task:' + path.basename(file), file); });
  REPORT_PREFIXES.forEach(function (name) {
    var loaded = readJsonCached(reportPath(name, stem));
    var hashes = loaded && loaded.data && loaded.data.inputHashes;
    if (!hashes || typeof hashes !== 'object' || Array.isArray(hashes)) return;
    Object.keys(hashes).slice(0, 512).forEach(function (input) {
      if (!input || seen[name + ':' + input]) return;
      seen[name + ':' + input] = true;
      parts.push(name + ':' + input + ':' + trustedInputStamp(input));
    });
  });
  return parts.join('|');
}

// Collapse the freshness signals (stage / runIds / hashDrift / missingRequired)
// into one authoritative state, computed once. The modal/board render results as
// authoritative ONLY when this is 'READY'; every other state renders as collapsed
// diagnostics, never as "not run" + a live blocker list. READY requires final · !mixed ·
// !hashDrift · !missingRequired · no unreadable/SKIPPED required — a STRICT SUBSET of the
// byte-serving gate `trustedFinalVisualEvidence`, which permits missingRequired/
// SKIPPED-required completeness states but keeps every
// integrity guard. READY is the DISPLAY authority; the byte-serving gate is independently
// re-verified. Never let READY loosen its own integrity axes.
function computeEvidenceState(ctx) {
  if (!ctx.bundlePresent) return 'NOT_RUN';

  function requiredIncomplete() {
    if (ctx.missingRequiredReports.length) return true;
    for (var i = 0; i < ctx.requiredReports.length; i++) {
      var r = ctx.byName[ctx.requiredReports[i]];
      if (!r || !r.present || r.unreadable) return true;
      if (String(r.overall || '').toUpperCase() === 'SKIPPED') return true;
    }
    return false;
  }

  if (ctx.bundleStage === 'prebuild') {
    // A prebuild bundle is never authoritative. A fresh-run report sitting beside it (a
    // different runId) is the classic superseded half-state; otherwise it is simply not-yet-final.
    var shot = ctx.screenshotReport;
    if (shot && shot.present && shot.pipelineRunId && ctx.bundleRunId && shot.pipelineRunId !== ctx.bundleRunId) return 'SUPERSEDED';
    return 'NOT_RUN';
  }

  if (ctx.bundleStage === 'final') {
    if (ctx.finalRequiredReportSetMismatch) return 'INCOMPLETE';
    if (requiredIncomplete()) return 'INCOMPLETE';
    if (ctx.runIds.length > 1) return 'MIXED_RUNS';
    // Integrity drift only (the sealed REPORT files changed) — benign input drift (shared
    // .kt / registry moved after ship) is surfaced as an advisory badge, never as STALE.
    if (ctx.hashDrift.length) return 'STALE';
    return 'READY';
  }

  return 'NOT_RUN';   // present bundle with an unknown/missing stage → not authoritative
}

function readEvidence(stem) {
  if (!locks.validTaskStem(stem)) return { present: false };
  var bundle = summarizeReport('evidence', stem);
  // Memo-backed: on the (near-universal) unchanged-stat hit this returns the
  // very parse summarizeReport('evidence') just used — bundle.stage/runId and
  // the inputHashes consumed by evidenceHashDrift come from one read, not two.
  var bundleLoaded = bundle.present ? readJsonCached(reportPath('evidence', stem)) : null;
  var bundleData = bundleLoaded ? bundleLoaded.data : null;
  var reports = REPORT_PREFIXES.map(function (name) { return summarizeReport(name, stem); });
  var screenshotReport = reports.find(function (r) { return r.name === 'screenshot'; });
  var presentReports = reports.filter(function (r) { return r.present; });
  if (!bundle.present && !presentReports.length) {
    return { present: false, stem: stem, reports: reports, evidenceState: 'NOT_RUN' };
  }

  var allPresent = (bundle.present ? [bundle] : []).concat(presentReports);
  // A final bundle cannot define its own smaller trust boundary. evidence-bundle supports
  // --require for diagnostics, but ship/verify and the UI must agree that only the canonical
  // seven-report set is authoritative. Prebuild keeps its deliberately smaller declared set.
  var declaredRequiredReports = (bundle.requiredReports && bundle.requiredReports.length) ? bundle.requiredReports : REPORT_PREFIXES.slice();
  var requiredReports = bundle.stage === 'final' ? REPORT_PREFIXES.slice() : declaredRequiredReports;
  var rawDeclaredRequiredReports = bundleData && bundleData.requiredReports;
  var finalRequiredReportSetMismatch = bundle.stage === 'final' &&
    (!Array.isArray(rawDeclaredRequiredReports) || JSON.stringify(rawDeclaredRequiredReports) !== JSON.stringify(REPORT_PREFIXES));
  var byName = Object.create(null);
  reports.forEach(function (r) { byName[r.name] = r; });
  var missingRequiredReports = requiredReports.filter(function (name) { return !byName[name] || !byName[name].present; });
  var runIds = unique(allPresent.map(function (r) { return r.pipelineRunId; }));
  // Two DISTINCT drift axes (they used to be one, which killed done-task artifact serving
  // and the fix button whenever ANY later task moved a shared input):
  //   integrityDrift — the bundle-pinned REPORT files themselves changed after the bundle
  //     sealed (tamper witness; per-stem; meaningful forever) → stale/STALE, blocks serving.
  //   inputDrift — a report's recorded INPUT files (shared product .kt, the task's ## Design)
  //     changed after the report was written. For a shipped task this is "the world moved
  //     on", not forgery — the ship-time certification is receipt-bound and the artifacts are
  //     independently hash-bound per serve. Advisory badge only; never flips stale/STALE.
  //     (The in-flight enforcement lives in evidence-bundle --fresh, which re-verifies
  //     inputs — incl. the census registry-consult digest — before ship-done moves anything.)
  var integrityDrift = evidenceHashDrift(bundleData, reports);
  var inputDrift = reportInputHashDrift(stem, reports, requiredReports)
    .concat(designSourceHashDrift(stem, reports));
  var hashDrift = integrityDrift;
  var issues = [];
  allPresent.forEach(function (r) {
    issues = issues.concat((r.topIssues || []).map(function (issue) {
      return Object.assign({}, issue, { report: issue.report || r.name });
    }));
  });
  if (!bundle.present && missingRequiredReports.length) {
    issues.push({
      report: 'evidence',
      severity: 'REVIEW_REQUIRED',
      issueKind: 'REPORTS_MISSING',
      message: 'Evidence bundle is absent and required reports are missing: ' + missingRequiredReports.join(', '),
      screen: null,
      theme: null,
      file: null,
      path: null,
      stableId: null,
      elementName: null,
      component: null
    });
  }
  if (finalRequiredReportSetMismatch) {
    issues.push({
      report: 'evidence',
      severity: 'BLOCKER',
      issueKind: 'FINAL_REQUIRED_REPORT_SET_NONCANONICAL',
      message: 'Final evidence declares a non-canonical required report set; regenerate the final bundle with all seven required reports.',
      screen: null,
      theme: null,
      file: reportPublicPath('evidence', stem),
      path: null,
      stableId: null,
      elementName: null,
      component: null
    });
  }
  issues = uniqueIssues(issues).sort(function (a, b) {
    var d = statusRank(b.severity) - statusRank(a.severity);
    if (d) return d;
    return String(a.report || '').localeCompare(String(b.report || ''));
  });

  var overallRows = allPresent.slice();
  if (!bundle.present && missingRequiredReports.length) overallRows.push({ overall: 'INCOMPLETE' });
  if (finalRequiredReportSetMismatch) overallRows.push({ overall: 'INCOMPLETE' });
  var overall = overallOf(overallRows);
  var countRows = presentReports.length ? presentReports : allPresent;
  var blockingCount = 0, warningCount = 0, issueCount = 0;
  for (var i = 0; i < countRows.length; i++) {
    blockingCount += Number(countRows[i].blockingCount || 0);
    warningCount += Number(countRows[i].warningCount || 0);
    issueCount += Number(countRows[i].issueCount || 0);
  }
  var syntheticCounts = issueCounts(issues);
  blockingCount = Math.max(blockingCount, syntheticCounts.blockingCount);
  warningCount = Math.max(warningCount, syntheticCounts.warningCount);
  issueCount = Math.max(issueCount, issues.length);

  var evidenceState = computeEvidenceState({
    bundlePresent: bundle.present,
    bundleStage: bundle.stage || null,
    bundleRunId: bundle.pipelineRunId || null,
    requiredReports: requiredReports,
    byName: byName,
    missingRequiredReports: missingRequiredReports,
    finalRequiredReportSetMismatch: finalRequiredReportSetMismatch,
    runIds: runIds,
    hashDrift: hashDrift,
    screenshotReport: screenshotReport
  });

  // When the evidence is NOT authoritative-as-results (no bundle / prebuild leftover beside a
  // newer run), route the per-report issues OUT of topIssues into supersededIssues — this is
  // what kills the "comparison not run" banner rendered ON TOP of a live SSIM-blocker list.
  var allIssues = issues.slice(0, MAX_GLOBAL_ISSUES);
  var orphaned = (evidenceState === 'NOT_RUN' || evidenceState === 'SUPERSEDED');
  var topIssuesOut = orphaned ? [] : allIssues;
  var supersededIssues = orphaned ? allIssues : [];

  return {
    schemaVersion: 1,
    kind: 'figma-evidence-summary',
    present: true,
    stem: stem,
    stage: bundle.stage || null,
    evidenceState: evidenceState,
    overall: overall,
    rank: statusRank(overall),
    pipelineRunId: bundle.pipelineRunId || runIds[0] || null,
    generatedAt: bundle.generatedAt || newestIso(allPresent),
    stale: runIds.length > 1 || hashDrift.length > 0,
    runIds: runIds,
    hashDriftReports: hashDrift,
    inputDriftReports: inputDrift,
    blockingCount: blockingCount,
    warningCount: warningCount,
    issueCount: issueCount,
    requiredReports: requiredReports,
    missingRequiredReports: missingRequiredReports,
    finalRequiredReportSetMismatch: finalRequiredReportSetMismatch,
    bundle: bundle.present ? bundle : null,
    reports: reports,
    visualChecks: screenshotReport && screenshotReport.visualChecks ? screenshotReport.visualChecks : null,
    // The owner-review state from the final bundle (extra.pixelReview) — `pending` rows
    // render as review rows with verdict buttons, `resolved` carry the audited verdict/by/at.
    // null until a final bundle that saw routed rows ran.
    pixelReview: bundleData && bundleData.pixelReview && typeof bundleData.pixelReview === 'object'
      ? { pending: Array.isArray(bundleData.pixelReview.pending) ? bundleData.pixelReview.pending : [], resolved: Array.isArray(bundleData.pixelReview.resolved) ? bundleData.pixelReview.resolved : [] }
      : null,
    topIssues: topIssuesOut,
    supersededIssues: supersededIssues,
    truncatedIssues: allIssues.length === MAX_GLOBAL_ISSUES && issues.length > MAX_GLOBAL_ISSUES
  };
}

function artifactStamp(stem) {
  var file = reportPath('screenshot', stem);
  var st;
  try {
    st = fs.statSync(file);
    if (!st.isFile()) return 'none';
  } catch (e) {
    return 'none';
  }
  var reportStamp = st.mtimeMs + ':' + st.size;
  var cached = artifactStampCache[stem];
  if (cached && cached.reportStamp === reportStamp && Date.now() - cached.checkedAt < 10000) return cached.value;

  var loaded = readJsonCached(file);   // memo: reuse the parse when only the 10s TTL expired
  var refs = artifactRefsFromReport(loaded && loaded.data, MAX_ARTIFACT_REFS + 1);
  if (!refs.length) return 'none';
  var seen = Object.create(null);
  var parts = [];
  for (var i = 0; i < refs.length && parts.length < MAX_ARTIFACT_REFS; i++) {
    var ref = refs[i] || {};
    var key = [ref.id || '', ref.path || '', ref.hash || '', ref.kind || ''].join('\u0001');
    if (seen[key]) continue;
    seen[key] = true;
    if (!isSafeArtifactRelPath(stem, ref.path)) {
      parts.push((ref.id || '?') + ':invalid');
      continue;
    }
    var target = path.resolve(paths.FIGMA_CACHE_DIR, ref.path);
    try {
      var st = fs.statSync(target);
      parts.push((ref.id || '?') + ':' + st.mtimeMs + ':' + st.size);
    } catch (e) {
      parts.push((ref.id || '?') + ':missing');
    }
  }
  if (refs.length > MAX_ARTIFACT_REFS) parts.push('truncated:' + refs.length);
  var value = parts.join(',');
  artifactStampCache[stem] = { reportStamp: reportStamp, checkedAt: Date.now(), value: value };
  return value;
}

function readEvidenceLite(stem) {
  if (!locks.validTaskStem(stem)) return null;
  var names = ['evidence'].concat(REPORT_PREFIXES);
  var parts = [];
  var any = false;
  for (var i = 0; i < names.length; i++) {
    var file = reportPath(names[i], stem);
    try {
      var st = fs.statSync(file);
      if (st.isFile()) {
        any = true;
        parts.push(names[i] + ':' + st.mtimeMs + ':' + st.size);
      } else {
        parts.push(names[i] + ':not-file');
      }
    } catch (e) {
      parts.push(names[i] + ':missing');
    }
  }
  parts.push('artifacts:' + artifactStamp(stem));
  parts.push('inputs:' + evidenceExternalInputsStamp(stem));
  if (!any) { delete liteCache[stem]; return null; }
  var stamp = parts.join('|');
  var cached = liteCache[stem];
  if (cached && cached.stamp === stamp) return cached.value;

  var full = readEvidence(stem);
  if (!full || !full.present) { delete liteCache[stem]; return null; }
  var screenshotReport = Array.isArray(full.reports) ? full.reports.find(function (r) { return r && r.name === 'screenshot'; }) : null;
  var value = {
    present: true,
    evidenceState: full.evidenceState || null,
    overall: full.overall,
    stage: full.stage || null,
    pipelineRunId: full.pipelineRunId || null,
    generatedAt: full.generatedAt || null,
    screenshotReportHash: screenshotReport && screenshotReport.hash || null,
    stale: !!full.stale,
    supersededCount: Array.isArray(full.supersededIssues) ? full.supersededIssues.length : 0,
    missingRequiredCount: Array.isArray(full.missingRequiredReports) ? full.missingRequiredReports.length : 0,
    blockingCount: typeof full.blockingCount === 'number' ? full.blockingCount : 0,
    warningCount: typeof full.warningCount === 'number' ? full.warningCount : 0,
    issueCount: typeof full.issueCount === 'number' ? full.issueCount : 0,
    hasCompareArtifacts: !!(full.visualChecks && full.visualChecks.artifactCount),
    artifactCount: full.visualChecks && typeof full.visualChecks.artifactCount === 'number' ? full.visualChecks.artifactCount : 0,
    screenshotCount: full.visualChecks && typeof full.visualChecks.total === 'number' ? full.visualChecks.total : 0,
    // Rows awaiting the owner's pixel review — the board card's amber chip count.
    reviewPending: full.pixelReview && Array.isArray(full.pixelReview.pending) ? full.pixelReview.pending.length : 0
  };
  liteCache[stem] = { stamp: stamp, value: value };
  return value;
}

function pushArtifactRefs(out, set, limit) {
  limit = typeof limit === 'number' ? limit : Infinity;
  if (!set || typeof set !== 'object') return;
  var artifacts = set.artifacts && typeof set.artifacts === 'object' ? set.artifacts : {};
  Object.keys(artifacts).forEach(function (kind) {
    if (out.length >= limit) return;
    var ref = artifacts[kind];
    if (ref && typeof ref === 'object') out.push(ref);
  });
}

function artifactRefsFromReport(data, limit) {
  limit = typeof limit === 'number' ? limit : Infinity;
  var out = [];
  if (!data || typeof data !== 'object') return out;
  if (data.artifactSet && Array.isArray(data.artifactSet.entries)) {
    data.artifactSet.entries.forEach(function (ref) {
      if (out.length >= limit) return;
      if (ref && typeof ref === 'object') out.push(ref);
    });
  }
  if (Array.isArray(data.artifactSets)) data.artifactSets.forEach(function (set) { if (out.length < limit) pushArtifactRefs(out, set, limit); });
  if (Array.isArray(data.results)) data.results.forEach(function (row) { if (out.length < limit) pushArtifactRefs(out, row && row.artifactSet, limit); });
  if (Array.isArray(data.issues)) data.issues.forEach(function (row) { if (out.length < limit) pushArtifactRefs(out, row && row.artifactSet, limit); });
  return out.filter(function (ref) { return ref && typeof ref.id === 'string'; });
}

function isSafeArtifactRelPath(stem, relPath) {
  if (typeof relPath !== 'string' || !relPath) return false;
  var s = relPath.replace(/\\/g, '/');
  if (path.isAbsolute(s)) return false;
  var parts = s.split('/');
  if (parts.some(function (seg) { return !seg || seg === '.' || seg === '..'; })) return false;
  if (parts[0] !== 'artifacts' || parts[1] !== 'screenshot') return false;
  if (parts[2] !== artifactPathContract.artifactSegment(stem)) return false;
  if (path.extname(s).toLowerCase() !== '.png') return false;
  return true;
}

function artifactRefMatchesFinalRun(stem, ref, expectedRunId) {
  if (!ref || !isSafeArtifactRelPath(stem, ref.path)) return false;
  var kind = String(ref.kind || '');
  if (!Object.prototype.hasOwnProperty.call(ARTIFACT_FILE_BY_KIND, kind)) return false;
  var s = ref.path.replace(/\\/g, '/');
  var parts = s.split('/');
  if (parts.length < 5) return false;
  if (parts[3] !== artifactPathContract.artifactSegment(expectedRunId)) return false;
  return path.basename(s) === ARTIFACT_FILE_BY_KIND[kind];
}

function trustedFinalVisualEvidence(stem, expectedReportHash) {
  if (typeof expectedReportHash !== 'string' || !SHA256_RE.test(expectedReportHash)) return null;
  expectedReportHash = expectedReportHash.toLowerCase();
  var summary = readEvidence(stem);
  if (!summary || summary.present === false) return null;
  if (summary.schemaVersion !== 1 || summary.kind !== 'figma-evidence-summary') return null;
  if (summary.stage !== 'final' || summary.stale) return null;
  if (Array.isArray(summary.hashDriftReports) && summary.hashDriftReports.length) return null;
  // NOTE: a missing REQUIRED report (e.g. the spec report) is a COMPLETENESS issue, not an
  // integrity one — it makes the bundle non-READY (computeEvidenceState handles that, and the
  // ship gate still blocks it), but it does NOT taint the screenshot ARTIFACTS. The server may
  // therefore serve images for a failed/incomplete-but-genuine final comparison so it can
  // show its per-screen images for diagnosis. Every integrity guard stays — the screenshot report
  // must be present and hash-bound (below), each artifact is bound to that report hash + run id,
  // path-/symlink-safe, and its bytes are re-hashed against the recorded value in compareArtifactFile.
  var reports = Array.isArray(summary.reports) ? summary.reports : [];
  for (var i = 0; i < reports.length; i++) {
    if (reports[i] && reports[i].unreadable) return null;
  }
  var screenshot = reports.find(function (r) { return r && r.name === 'screenshot'; });
  if (!screenshot || !screenshot.present || String(screenshot.hash || '').toLowerCase() !== expectedReportHash) return null;
  if (!summary.visualChecks || String(summary.visualChecks.reportHash || '').toLowerCase() !== expectedReportHash) return null;
  if (!summary.pipelineRunId || screenshot.pipelineRunId !== summary.pipelineRunId) return null;
  return summary;
}

function artifactIndexForReport(stem, expectedReportHash, expectedRunId) {
  if (!locks.validTaskStem(stem)) return null;
  if (typeof expectedReportHash !== 'string' || !SHA256_RE.test(expectedReportHash)) return null;
  if (typeof expectedRunId !== 'string' || !expectedRunId) return null;
  expectedReportHash = expectedReportHash.toLowerCase();
  var file = reportPath('screenshot', stem);
  var st;
  try {
    st = fs.statSync(file);
    if (!st.isFile()) return null;
  } catch (e) {
    return null;
  }
  var loaded = readJsonBuffer(file);
  if (!loaded || !loaded.data || typeof loaded.data !== 'object') return null;
  var reportHash = loaded.hash;
  if (reportHash !== expectedReportHash) return null;
  var stamp = st.mtimeMs + ':' + st.size + ':' + reportHash + ':' + expectedRunId;
  var cached = artifactIndexCache[stem];
  if (cached && cached.stamp === stamp) return cached.index;
  var refs = artifactRefsFromReport(loaded.data, MAX_ARTIFACT_REFS + 1);
  if (refs.length > MAX_ARTIFACT_REFS) return null;
  var byId = Object.create(null);
  var ambiguous = Object.create(null);
  for (var i = 0; i < refs.length; i++) {
    var ref = refs[i];
    if (!ref || typeof ref.id !== 'string' || !ARTIFACT_ID_RE.test(ref.id)) continue;
    if (!artifactRefMatchesFinalRun(stem, ref, expectedRunId)) continue;
    var prior = byId[ref.id];
    var refHash = typeof ref.hash === 'string' ? ref.hash.toLowerCase() : ref.hash;
    if (prior && (prior.path !== ref.path || prior.hash !== refHash || prior.kind !== ref.kind)) {
      ambiguous[ref.id] = true;
      continue;
    }
    byId[ref.id] = Object.assign({}, ref, { hash: refHash });
  }
  var index = { byId: byId, ambiguous: ambiguous, reportHash: reportHash };
  artifactIndexCache[stem] = { stamp: stamp, index: index };
  return index;
}

function compareArtifactFile(stem, id, reportHash) {
  if (typeof id !== 'string' || !ARTIFACT_ID_RE.test(id)) return null;
  var summary = trustedFinalVisualEvidence(stem, reportHash);
  if (!summary) return null;
  var index = artifactIndexForReport(stem, reportHash, summary.pipelineRunId);
  if (!index || index.ambiguous[id]) return null;
  var ref = index.byId[id];
  if (!ref || !isSafeArtifactRelPath(stem, ref.path)) return null;
  var target = path.resolve(paths.FIGMA_CACHE_DIR, ref.path);
  var root = path.resolve(COMPARE_ARTIFACTS_DIR);
  if (target !== root && target.indexOf(root + path.sep) !== 0) return null;
  try {
    var lst = fs.lstatSync(target);
    if (!lst.isFile()) return null;
    var real = fs.realpathSync(target);
    var realRoot = fs.realpathSync(root);
    if (real !== realRoot && real.indexOf(realRoot + path.sep) !== 0) return null;
    if (typeof ref.hash !== 'string' || !SHA256_RE.test(ref.hash)) return null;
    var bytes = fs.readFileSync(real);
    if (sha256Bytes(bytes) !== ref.hash.toLowerCase()) return null;
    return { file: real, bytes: bytes };
  } catch (e) {
    return null;
  }
}

module.exports = {
  readEvidence: readEvidence,
  readEvidenceLite: readEvidenceLite,
  compareArtifactFile: compareArtifactFile
};
