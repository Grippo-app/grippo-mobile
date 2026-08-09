'use strict';

// ---------------------------------------------------------------------------
// Tiny filesystem helpers shared by the validators, agents, locks, and
// requests modules. Pure (fs/path only) so it sits at the bottom of the
// require graph.
// ---------------------------------------------------------------------------

var fs   = require('fs');
var path = require('path');

// Directories the FS walkers never descend into: build artefacts, vendor
// caches, VCS, IDE state. Anything outside this set is fair game.
var EXCLUDED_DIRS = new Set([
  'build', '.gradle', 'node_modules', '.git', '.idea', '.svn'
]);

function exists(p) {
  try { fs.accessSync(p); return true; } catch (e) { return false; }
}

function readUtf8(p) {
  try { return fs.readFileSync(p, 'utf8'); } catch (e) { return null; }
}

function fileContains(p, needle) {
  var s = readUtf8(p);
  return s != null && s.indexOf(needle) >= 0;
}

function countFilesIn(dir, predicate) {
  try {
    var entries = fs.readdirSync(dir);
    var n = 0;
    for (var i = 0; i < entries.length; i++) {
      if (!predicate || predicate(entries[i])) n++;
    }
    return n;
  } catch (e) { return 0; }
}

function findRecursive(dir, fileName, maxDepth) {
  if (maxDepth === null || maxDepth === undefined) maxDepth = 12;
  if (maxDepth < 0) return false;
  var entries;
  try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch (e) { return false; }
  for (var i = 0; i < entries.length; i++) {
    var e = entries[i];
    if (EXCLUDED_DIRS.has(e.name)) continue;
    var p = path.join(dir, e.name);
    if (e.isDirectory()) {
      if (findRecursive(p, fileName, maxDepth - 1)) return true;
    } else if (e.name === fileName) {
      return true;
    }
  }
  return false;
}

// Bounded content scan used by validators whose contract is semantic rather
// than tied to a canonical filename.
function findContentMatch(dir, regex, maxDepth) {
  if (maxDepth === null || maxDepth === undefined) maxDepth = 12;
  if (maxDepth < 0) return false;
  var entries;
  try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch (e) { return false; }
  for (var i = 0; i < entries.length; i++) {
    var e = entries[i];
    if (EXCLUDED_DIRS.has(e.name)) continue;
    var p = path.join(dir, e.name);
    if (e.isDirectory()) {
      if (findContentMatch(p, regex, maxDepth - 1)) return true;
    } else if (e.name.endsWith('.kt') || e.name.endsWith('.swift')) {
      var content = readUtf8(p);
      if (content && regex.test(content)) return true;
    }
  }
  return false;
}

module.exports = {
  EXCLUDED_DIRS: EXCLUDED_DIRS,
  exists: exists,
  readUtf8: readUtf8,
  fileContains: fileContains,
  countFilesIn: countFilesIn,
  findRecursive: findRecursive,
  findContentMatch: findContentMatch
};
