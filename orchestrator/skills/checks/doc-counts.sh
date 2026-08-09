#!/usr/bin/env bash
# doc-counts.sh — derive-and-assert every hard-coded documentation count against disk.
#
# STANDALONE (deliberately NOT a lint.sh check — that would be "a check that counts
# checks", and lint.sh's own count is one of the things this asserts), wired into
# run-all.sh. A documentation "N lint checks / N gates / N skills /
# N server-modules" claim that
# drifts from disk fails HERE instead of rotting silently until a human audit.
#
# Mechanism: re-derive each real count from disk, then for every doc line whose
# keyword-anchored pattern states that count, assert the stated number equals the
# real one. Reports `DOC COUNT DRIFT: <file>:<line> claims N …, disk has M` + exit 1.
# Files absent pre-bootstrap are skipped, like the sibling gates.
set -uo pipefail
cd "$(dirname "$0")/../../.."   # -> repo root

python3 - <<'PY'
import os, re, glob, sys, json

def read(p):
    try:
        with open(p, encoding="utf-8") as f:
            return f.read()
    except FileNotFoundError:
        return None

# --- real counts, re-derived from disk (never a remembered number) ---
lint_src = read("orchestrator/lint.sh") or ""
LINT   = len(re.findall(r'^# Check \d', lint_src, re.M))
runall = read("orchestrator/skills/checks/run-all.sh") or ""
GATES  = len(re.findall(r'^\s*"[a-z][a-z0-9-]*\|', runall, re.M))
SKILLS = len(glob.glob("orchestrator/skills/*/SKILL.md"))
SERVER = len(glob.glob("orchestrator/site/server/*.js"))

# --- (label, real, file, pattern with ONE capture group = the doc's claimed count) ---
# Each pattern is keyword-anchored so a bare "8"/"13"/"11" elsewhere never false-matches.
CHECKS = [
    ("lint checks",      LINT,   "orchestrator/skills/README.md",               r'lint is[^\n]*?\((\d+)\s+checks?\)'),
    ("lint checks",      LINT,   "README.md",                                    r'\((\d+)\s+checks?\b'),
    ("gates",            GATES,  "orchestrator/skills/README.md",                r'\b(\d+)\s+gates?\b'),
    ("installed skills", SKILLS, "README.md",                                    r'\b(\d+)\s+(?:self-contained |core )?skills\b'),
    ("installed skills", SKILLS, "orchestrator/README.md",                       r'\b(\d+)\s+(?:self-contained |core )?skills\b'),
    ("installed skills", SKILLS, "orchestrator/skills/README.md",                r'\b(\d+)\s+(?:self-contained |core )?skills\b'),
    ("server modules",   SERVER, "README.md",                                    r'Server architecture \((\d+)\s+single-purpose'),
    ("server modules",   SERVER, "orchestrator/site/server.js",                  r'lives in the (\d+) CommonJS'),
]

fail = 0
for label, real, path, pat in CHECKS:
    src = read(path)
    if src is None:
        continue   # absent (pre-bootstrap / partial checkout) — skip like sibling gates
    for m in re.finditer(pat, src):
        claimed = int(m.group(1))
        if claimed != real:
            line = src.count("\n", 0, m.start()) + 1
            print("DOC COUNT DRIFT: %s:%d claims %d %s, disk has %d" % (path, line, claimed, label, real))
            fail += 1

sys.exit(1 if fail else 0)
PY
rc=$?
if [ $rc -ne 0 ]; then
  echo "FAIL: documentation count(s) drifted from disk."
  exit 1
fi
echo "OK: doc counts match disk (lint checks, gates, installed skills, server modules)."
