'use strict';

// ---------------------------------------------------------------------------
// Header "Skills" pill feed: is the authored skill set actually INSTALLED in
// this project? install-skills.sh materializes three things — every manifest
// skill into .claude/skills/<name>/ (SKILL.md + references/), the frozen
// contracts into .claude/contracts/, and the screenshot-gate enforcement net
// (git core.hooksPath). This module OBSERVES all three (read-only fs stats;
// the wiring read is git.js enforcementWiring(), the same TTL-cached probe
// the run-gate consumes) and reduces them to one pill state:
//   'ok'      — every manifest skill + the contracts are installed, and the
//               net is wired (or deliberately opted out, FIGMA_WIRING_GATE=0)
//   'missing' — at least one skill (or the contracts dir) is not installed
//   'unwired' — skills installed, but core.hooksPath is not set
//   'unknown' — the install manifest is unreadable
// TTL-cached: deriveState() calls this on the SSE poll (~1.5 s) and ~25 stat
// calls per tick is waste; 10 s also means an install-skills.sh run is picked
// up within seconds, no server restart.
// ---------------------------------------------------------------------------

var fs     = require('fs');
var path   = require('path');
var paths  = require('./paths');
var gitMod = require('./git');

var ROOT          = paths.PROJECT_ROOT;
var MANIFEST      = path.join(ROOT, 'orchestrator', 'skills', '_index', 'install-manifest.json');
var SRC_SKILLS    = path.join(ROOT, 'orchestrator', 'skills');
var SRC_CONTRACTS = path.join(ROOT, 'orchestrator', 'contracts');
var DST_SKILLS    = path.join(ROOT, '.claude', 'skills');
var DST_CONTRACTS = path.join(ROOT, '.claude', 'contracts');

var TTL_MS = 10 * 1000;
var cache = null;   // { at, value }

// The manifest's installable roster. externalSourceException entries (e.g.
// implement-figma, which lives at figma/skill/) are deliberately NOT installed
// by install-skills.sh — mirror that exclusion, or the pill could never go
// green. Returns null when the manifest is unreadable.
function manifestSkillNames() {
  try {
    var man = JSON.parse(fs.readFileSync(MANIFEST, 'utf8'));
    if (!man || !Array.isArray(man.skills)) return null;
    var names = [];
    for (var i = 0; i < man.skills.length; i++) {
      var s = man.skills[i];
      if (s && typeof s.folderName === 'string' && s.folderName && !s.externalSourceException) {
        names.push(s.folderName);
      }
    }
    return names;
  } catch (e) {
    return null;
  }
}

// Installed = SKILL.md present AND, when the SOURCE skill ships references/,
// the installed references/ present too — a bare SKILL.md whose references
// dangle is a broken skill (install-skills.sh's own invariant). existsSync
// follows symlinks, so a --symlink install with a deleted target reads missing.
function skillInstalled(name) {
  if (!fs.existsSync(path.join(DST_SKILLS, name, 'SKILL.md'))) return false;
  if (fs.existsSync(path.join(SRC_SKILLS, name, 'references')) &&
      !fs.existsSync(path.join(DST_SKILLS, name, 'references'))) return false;
  return true;
}

function probe() {
  var checkedAt = new Date().toISOString();
  var names = manifestSkillNames();
  if (!names) {
    return { state: 'unknown', installed: 0, total: 0, missing: [],
             contractsOk: false, wiring: null, checkedAt: checkedAt };
  }
  var missing = [];
  for (var i = 0; i < names.length; i++) {
    if (!skillInstalled(names[i])) missing.push(names[i]);
  }
  // Contracts count as missing only when the template actually ships them.
  var contractsOk = !fs.existsSync(SRC_CONTRACTS) || fs.existsSync(DST_CONTRACTS);
  var w = gitMod.enforcementWiring();
  var optOut = process.env.FIGMA_WIRING_GATE === '0';
  var state = (missing.length || !contractsOk) ? 'missing'
            : (!w.wired && !optOut)            ? 'unwired'
            : 'ok';
  return {
    state: state,
    installed: names.length - missing.length,
    total: names.length,
    missing: missing,
    contractsOk: contractsOk,
    wiring: { inGit: w.inGit, wired: w.wired, hooksPath: w.hooksPath, expected: w.expected, optOut: optOut },
    checkedAt: checkedAt
  };
}

function status() {
  var now = Date.now();
  if (cache && (now - cache.at) < TTL_MS) return cache.value;
  cache = { at: now, value: probe() };
  return cache.value;
}

module.exports = { status: status };
