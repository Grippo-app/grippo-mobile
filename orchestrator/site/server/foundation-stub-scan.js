'use strict';

// ---------------------------------------------------------------------------
// Foundation-integrity stub scan — the single implementation behind BOTH
// consumers of launch.md Step 12's gate:
//
//   - `validators.js` validateStep12 (the wizard ✓, polled every ~1.5 s), and
//   - the bootstrap agent, which runs this file as a CLI from the product
//     root (`node orchestrator/site/server/foundation-stub-scan.js`).
//
// One implementation means the agent's gate and the server's ✓ can never
// disagree: the moment the CLI passes, the wizard marks Step 12 done on its
// own — no manual "Mark done" and no auto-run "unverified" pause.
//
// The scan is comment/string-aware. A raw regex over the sources flags the
// very comments the bootstrap docs prime agents to write ("do not stub the
// DialogConfig.ErrorDisplay → createChild branch — it crashes with
// NotImplementedError"), permanently wedging the ✓. Kotlin trivia (line/block
// comments, string/char literals) is masked before matching, so only real
// code-position `TODO(...)`/`NotImplementedError` occurrences fail the gate.
// ---------------------------------------------------------------------------

var fs = require('fs');
var path = require('path');
var fsutil = require('./fsutil');

// The foundation surface launch.md Step 12 globs: these four module roots,
// commonMain Kotlin only.
var FOUNDATION_ROOTS = ['shared', 'ui-core', 'ui-dialog-features', 'ui-screen-features'];

var STUB_RE = /TODO\(|NotImplementedError/;

// Single-pass Kotlin trivia masker — line comments, nesting block comments,
// double-quoted + triple-quoted (raw) strings, and char literals become
// spaces; newlines survive so line numbers keep pointing at the source.
// Ported (algorithm-identical) from the canonical masker in
// orchestrator/figma/scripts/extract-app-tokens.mjs — if that tokenizer
// changes, re-sync this port.
function maskKotlinTrivia(text) {
  var s = String(text || '');
  var n = s.length;
  var out = new Array(n);
  var blank = function (i) { out[i] = (s[i] === '\n' || s[i] === '\r') ? s[i] : ' '; };
  var i = 0;
  while (i < n) {
    var c = s[i], c2 = s[i + 1];
    if (c === '/' && c2 === '/') {                         // line comment
      while (i < n && s[i] !== '\n' && s[i] !== '\r') { out[i] = ' '; i++; }
      continue;
    }
    if (c === '/' && c2 === '*') {                         // block comment (nesting)
      var depth = 1;
      out[i] = ' '; out[i + 1] = ' '; i += 2;
      while (i < n && depth > 0) {
        if (s[i] === '/' && s[i + 1] === '*') { out[i] = ' '; out[i + 1] = ' '; i += 2; depth++; }
        else if (s[i] === '*' && s[i + 1] === '/') { out[i] = ' '; out[i + 1] = ' '; i += 2; depth--; }
        else { blank(i); i++; }
      }
      continue;
    }
    if (c === '"' && c2 === '"' && s[i + 2] === '"') {     // triple-quoted (raw) string
      out[i] = ' '; out[i + 1] = ' '; out[i + 2] = ' '; i += 3;
      while (i < n) {
        if (s[i] === '"' && s[i + 1] === '"' && s[i + 2] === '"') { out[i] = ' '; out[i + 1] = ' '; out[i + 2] = ' '; i += 3; break; }
        blank(i); i++;
      }
      continue;
    }
    if (c === '"') {                                       // double-quoted string
      out[i] = ' '; i++;
      while (i < n) {
        if (s[i] === '\\') { out[i] = ' '; if (i + 1 < n) blank(i + 1); i += 2; continue; }
        if (s[i] === '"') { out[i] = ' '; i++; break; }
        if (s[i] === '\n' || s[i] === '\r') { out[i] = s[i]; i++; break; }   // unterminated: stop at EOL
        out[i] = ' '; i++;
      }
      continue;
    }
    if (c === "'") {                                       // char literal
      out[i] = ' '; i++;
      while (i < n) {
        if (s[i] === '\\') { out[i] = ' '; if (i + 1 < n) blank(i + 1); i += 2; continue; }
        if (s[i] === "'") { out[i] = ' '; i++; break; }
        if (s[i] === '\n' || s[i] === '\r') { out[i] = s[i]; i++; break; }
        out[i] = ' '; i++;
      }
      continue;
    }
    out[i] = c; i++;                                       // code
  }
  return out.join('');
}

// Walk `dir`, collecting every `commonMain` directory under it. Modules nest
// one level deep (e.g. ui-core/foundation/src/commonMain), so the source set
// can't be reached by a fixed join — find the commonMain roots, then scan each.
function collectCommonMainDirs(dir, out, maxDepth) {
  if (maxDepth === null || maxDepth === undefined) maxDepth = 12;
  if (maxDepth < 0) return;
  var entries;
  try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch (e) { return; }
  for (var i = 0; i < entries.length; i++) {
    var e = entries[i];
    if (!e.isDirectory()) continue;
    if (fsutil.EXCLUDED_DIRS.has(e.name)) continue;
    var p = path.join(dir, e.name);
    if (e.name === 'commonMain') { out.push(p); continue; }   // don't descend further
    collectCommonMainDirs(p, out, maxDepth - 1);
  }
}

function scanKtFiles(dir, hits, projectRoot, maxDepth) {
  if (maxDepth === null || maxDepth === undefined) maxDepth = 12;
  if (maxDepth < 0) return;
  var entries;
  try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch (e) { return; }
  for (var i = 0; i < entries.length; i++) {
    var e = entries[i];
    if (fsutil.EXCLUDED_DIRS.has(e.name)) continue;
    var p = path.join(dir, e.name);
    if (e.isDirectory()) { scanKtFiles(p, hits, projectRoot, maxDepth - 1); continue; }
    if (!e.name.endsWith('.kt')) continue;
    var content = fsutil.readUtf8(p);
    if (!content || !STUB_RE.test(content)) continue;      // cheap pre-check
    var masked = maskKotlinTrivia(content).split('\n');
    var original = content.split('\n');
    for (var line = 0; line < masked.length; line++) {
      if (STUB_RE.test(masked[line])) {
        hits.push({
          file: path.relative(projectRoot, p),
          line: line + 1,
          text: (original[line] || '').trim()
        });
      }
    }
  }
}

// → [{file, line, text}] — every real (non-comment, non-string) TODO(...) /
// NotImplementedError in foundation commonMain Kotlin under `projectRoot`.
function scanFoundationStubs(projectRoot) {
  var hits = [];
  for (var i = 0; i < FOUNDATION_ROOTS.length; i++) {
    var dirs = [];
    collectCommonMainDirs(path.join(projectRoot, FOUNDATION_ROOTS[i]), dirs);
    for (var j = 0; j < dirs.length; j++) scanKtFiles(dirs[j], hits, projectRoot);
  }
  return hits;
}

module.exports = {
  FOUNDATION_ROOTS: FOUNDATION_ROOTS,
  maskKotlinTrivia: maskKotlinTrivia,
  scanFoundationStubs: scanFoundationStubs
};

// CLI — the launch.md Step 12 gate. Run from the product root:
//   node orchestrator/site/server/foundation-stub-scan.js
if (require.main === module) {
  var PROJECT_ROOT = require('./paths').PROJECT_ROOT;
  var found = scanFoundationStubs(PROJECT_ROOT);
  if (found.length === 0) {
    console.log('OK: no foundation stubs (comment/string-aware scan over '
      + FOUNDATION_ROOTS.join('/, ') + '/ commonMain)');
    process.exit(0);
  }
  console.error('FAIL: foundation stub found — implement it before declaring the bootstrap done');
  for (var k = 0; k < found.length; k++) {
    console.error('  ' + found[k].file + ':' + found[k].line + ': ' + found[k].text);
  }
  process.exit(1);
}
