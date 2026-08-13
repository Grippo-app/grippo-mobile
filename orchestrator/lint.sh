#!/usr/bin/env bash
# Verifies the durable orchestrator/ surfaces (tasks/, site/, figma/,
# api-contract/, and the derived architecture map) against template-wide
# mechanical drift.
#   1. The four task subdirectories (backlog/, pending/, todo/, done/) all
#      exist under orchestrator/tasks/.
#   2. orchestrator/tasks/INDEX.json parses as JSON and has the expected
#      top-level keys (version, generatedAt, backlog, pending, todo, done).
#   3. Every pending sidecar (orchestrator/tasks/pending/<stem>.questions.md)
#      has a matching orchestrator/tasks/backlog/<stem>.md backlog file.
#   4. When a product project exists (settings.gradle.kts present), the derived
#      architecture map orchestrator/.arch-map.json exists and is fresh
#      (regen-arch.py --check passes). Skipped pre-bootstrap.
#   5. The canonical Site i18n aggregate exposes the same key SET for
#      en/ru/uk. Importing the aggregate also exercises its duplicate, missing
#      domain, and value-shape guards.
#   6. Every figma:/contract: npm script target exists, workspace/Node pins are
#      coherent, Git tracks no runtime/dependency/cache/account/secret junk,
#      and template exclusions agree with launch/root ignore policy without
#      hiding committed Figma sources.
#   7. Runtime/UI enum MIRRORS stay in sync. (a) Every
#      log-event.py journal PHASE, and every STATUS the board renders as a label,
#      has a `taskDetails.phase.*`/`taskDetails.status.*` key in every locale (the
#      `info` status is exempt — it is a CSS tone marker, not a rendered label),
#      so a newly-added journal enum can't render unlabeled. (b) The malformed-
#      `## Design`-kind set is byte-equal between design-parser.cjs and the
#      state.js board "Design: broken" chip mirror (or its source alias).
#      Skipped gracefully when a mirror file is absent.
#   8. The Step-6b done-gate block in outcome-appendix.md (the single load-bearing
#      prose anchor of the mandatory visual-evidence gate, which invokes the
#      ship-done.mjs interlock) carries no gate-weakening token
#      (advisory|optional|may-skip|self-skip). Scoped to that block only.
#   9. Wizard/template prompt surfaces are coherent: unique step ids, one prompt
#      source per step, matching launch headings, no accidental duplicate lines,
#      and (in the fresh source template) an exact project-config body mirror.
set -uo pipefail
# Absolute path to THIS script, captured before the cd, so the final echo can
# self-derive its own check count (the runtime line can't lie about how many ran).
SELF="$(cd "$(dirname "$0")" && pwd)/$(basename "$0")"
cd "$(dirname "$0")/.."

fail=0

# Check 1: the four task subdirectories exist under orchestrator/tasks/.
task_dirs_missing=0
for sub in backlog pending todo done; do
  if [ ! -d "orchestrator/tasks/$sub" ]; then
    echo "MISSING TASK SUBDIR: orchestrator/tasks/$sub"
    task_dirs_missing=$((task_dirs_missing + 1))
  fi
done
if [ $task_dirs_missing -gt 0 ]; then
  echo "FAIL: $task_dirs_missing task subdirectory/-ies missing."
  fail=$((fail + 1))
fi

# Check 2: INDEX.json validates and has the expected top-level keys.
if [ ! -f orchestrator/tasks/INDEX.json ]; then
  echo "MISSING: orchestrator/tasks/INDEX.json"
  fail=$((fail + 1))
elif ! python3 -c "
import json, sys
try:
  d = json.load(open('orchestrator/tasks/INDEX.json'))
except json.JSONDecodeError as e:
  sys.stderr.write('INDEX.json is not valid JSON: ' + str(e) + '\n')
  sys.exit(1)
missing = [k for k in ['version','generatedAt','backlog','pending','todo','done'] if k not in d]
if missing:
  sys.stderr.write('INDEX.json missing keys: ' + ','.join(missing) + '\n')
  sys.exit(1)
sys.exit(0)
" 2>&1; then
  echo "FAIL: orchestrator/tasks/INDEX.json is invalid JSON or missing required keys."
  fail=$((fail + 1))
fi

# Check 3: every pending sidecar has a matching backlog file.
orphan_pending=0
shopt -s nullglob
for sidecar in orchestrator/tasks/pending/*.questions.md; do
  [ -f "$sidecar" ] || continue
  base=$(basename "$sidecar")
  stem="${base%.questions.md}"
  if [ ! -f "orchestrator/tasks/backlog/$stem.md" ]; then
    echo "ORPHAN PENDING SIDECAR: $sidecar (no orchestrator/tasks/backlog/$stem.md)"
    orphan_pending=$((orphan_pending + 1))
  fi
done
if [ $orphan_pending -gt 0 ]; then
  echo "FAIL: $orphan_pending orphan pending sidecar(s)."
  fail=$((fail + 1))
fi

# Check 4: the derived architecture map is fresh (only when a product project exists).
# Mirrors check 2's "call python to self-validate" shape. Skipped pre-bootstrap, where
# regen-arch.py would no-op anyway (no settings.gradle.kts → nothing to map).
if [ -f settings.gradle.kts ]; then
  if [ ! -f orchestrator/.arch-map.json ]; then
    echo "MISSING: orchestrator/.arch-map.json (run: python3 orchestrator/tasks/regen-arch.py)"
    fail=$((fail + 1))
  elif ! python3 orchestrator/tasks/regen-arch.py --check >/dev/null 2>&1; then
    echo "STALE: orchestrator/.arch-map.json out of date (run: python3 orchestrator/tasks/regen-arch.py)"
    fail=$((fail + 1))
  fi
fi

# Check 5: importing the one canonical dictionary aggregate must succeed, and
# en/ru/uk must expose the exact same key set. The aggregate itself rejects
# duplicate domains, duplicate keys, missing locale domains, and non-string
# values before this comparison can report green.
i18n_report=$(node --input-type=module <<'NODE' 2>&1
import dictionaries from './orchestrator/site/scripts/i18n/dictionaries/index.js'

const locales = ['en', 'ru', 'uk']
const expected = Object.keys(dictionaries.en).sort()
let bad = 0
for (const locale of locales.slice(1)) {
  const actual = Object.keys(dictionaries[locale]).sort()
  const actualSet = new Set(actual)
  const expectedSet = new Set(expected)
  const missing = expected.filter((key) => !actualSet.has(key))
  const extra = actual.filter((key) => !expectedSet.has(key))
  if (missing.length) {
    console.log(`I18N PARITY: key(s) in en but not ${locale}:`)
    for (const key of missing) console.log(`  ${key}`)
    bad += 1
  }
  if (extra.length) {
    console.log(`I18N PARITY: key(s) in ${locale} but not en:`)
    for (const key of extra) console.log(`  ${key}`)
    bad += 1
  }
}
process.exit(bad ? 1 : 0)
NODE
)
if [ $? -ne 0 ]; then
  printf '%s\n' "$i18n_report"
  echo "FAIL: canonical en / ru / uk dictionary key sets disagree or the aggregate is invalid."
  fail=$((fail + 1))
fi

# Check 6: every figma:/contract: npm script's `node scripts/<file>.mjs|.cjs` target exists,
# every composite `npm run <alias>` references a script key that exists in the same package,
# and every committed Node pin agrees with the transport-safe orchestrator/.nvmrc.
# A renamed/removed script leaves a dangling alias that only fails when a Run button invokes it;
# a renamed sub-alias in `figma:verify` is just as bad, but the file-target grep cannot see it.
script_target_missing=0
script_alias_missing=0
for pkg in package.json orchestrator/figma/package.json orchestrator/api-contract/package.json \
  orchestrator/site/package.json orchestrator/tasks/package.json; do
  [ -f "$pkg" ] || continue
  pkg_dir=$(dirname "$pkg")
  while IFS= read -r rel; do
    [ -z "$rel" ] && continue
    if [ ! -f "$pkg_dir/$rel" ]; then
      echo "DANGLING SCRIPT ALIAS in $pkg: node $rel (no $pkg_dir/$rel)"
      script_target_missing=$((script_target_missing + 1))
    fi
  done < <(grep -oE 'node scripts/[A-Za-z0-9._/-]+\.(mjs|cjs)' "$pkg" | sed -E 's/^node //' | sort -u)

  alias_report=$(python3 - "$pkg" <<'PY' 2>&1
import json, re, sys
pkg = sys.argv[1]
with open(pkg, encoding="utf-8") as f:
    data = json.load(f)
scripts = data.get("scripts") or {}
missing = []
for name, cmd in scripts.items():
    for ref in re.findall(r'(?:^|[;&|]\s*)npm\s+run\s+([A-Za-z0-9:_-]+)', str(cmd)):
        if ref not in scripts:
            missing.append((name, ref))
for name, ref in missing:
    print(f"DANGLING SCRIPT ALIAS in {pkg}: script {name!r} runs missing alias {ref!r}")
sys.exit(1 if missing else 0)
PY
)
  if [ $? -ne 0 ]; then
    printf '%s\n' "$alias_report"
    script_alias_missing=$((script_alias_missing + 1))
  fi
done
if [ $script_target_missing -gt 0 ]; then
  echo "FAIL: $script_target_missing npm script alias(es) point at a missing script file."
  fail=$((fail + 1))
fi
if [ $script_alias_missing -gt 0 ]; then
  echo "FAIL: $script_alias_missing package.json file(s) contain dangling npm run alias reference(s)."
  fail=$((fail + 1))
fi

workspace_report=$(node orchestrator/tests/run-suite.mjs --check 2>&1)
if [ $? -ne 0 ]; then
  printf '%s\n' "$workspace_report"
  echo "FAIL: root workspace or test ownership contract is invalid."
  fail=$((fail + 1))
fi

node_pin="orchestrator/.nvmrc"
if [ ! -f "$node_pin" ] || [ "$(tr -d '\r\n' < "$node_pin" 2>/dev/null)" != "22" ]; then
  echo "NODE PIN INVALID: $node_pin must contain exactly the supported major 22"
  fail=$((fail + 1))
else
  for pin in .nvmrc orchestrator/figma/.nvmrc orchestrator/api-contract/.nvmrc; do
    if [ -f "$pin" ] && ! cmp -s "$node_pin" "$pin"; then
      echo "NODE PIN DRIFT: $pin differs from $node_pin"
      fail=$((fail + 1))
    fi
  done
fi

if ! node orchestrator/template-sync/check-tracked-runtime-artifacts.mjs .; then
  echo "FAIL: Git tracks generated runtime, dependency, cache, account, or secret artifacts."
  fail=$((fail + 1))
fi
if ! node orchestrator/template-sync/check-template-exclusion-contract.mjs .; then
  echo "FAIL: template manifest exclusions, launch instructions, and root .gitignore disagree."
  fail=$((fail + 1))
fi

# Check 7: runtime/UI enum MIRRORS stay in sync. Families of cross-language
# constants whose drift only surfaces as an unlabeled journal row long after
# the edit:
#   7a. Every log-event.py journal PHASE — and every STATUS that the board
#       RENDERS as a label — has a matching `taskDetails.phase.*` / `taskDetails.status.*`
#       i18n key in every locale, so a newly-added journal enum can't
#       render unlabeled (board.js phaseLabel/statusLabel fall back to the raw
#       token). The `info` status is intentionally exempt: it is a CSS tone
#       marker, never a rendered status label, so it has no `taskDetails.status.info`
#       key by design (board.js line ~1125). The check still asserts all
#       locales agree on which journal-enum label keys they cover.
#   7b. The malformed-`## Design`-kind set is byte-equal between design-parser.cjs
#       (the gate source of truth, `MALFORMED_DESIGN_KINDS`) and
#       site/server/state.js (the board's "Design: broken" chip mirror). The
#       preferred wiring aliases the source set directly; a literal mirror is
#       also checked for exact equality.
# Mirrors checks 2/4's "call python to self-validate" shape. Skipped gracefully
# when a file is absent (pre-bootstrap / partial checkout), like sibling checks.
LOG_EVENT="orchestrator/tasks/log-event.py"
DP="orchestrator/figma/scripts/design-parser.cjs"
STATE="orchestrator/site/server/state.js"
mirror_drift=0

# 7b: the malformed-`## Design`-kind set is byte-equal between design-parser.cjs
# (the gate source of truth) and state.js (the board's "Design: broken" chip).
if [ -f "$DP" ] && [ -f "$STATE" ]; then
  malformed_report=$(python3 - "$DP" "$STATE" <<'PY' 2>&1
import re, sys
dp_path, state_path = sys.argv[1], sys.argv[2]
dp = open(dp_path, encoding="utf-8").read()
st = open(state_path, encoding="utf-8").read()

def objkeys(src, name):
    # var NAME = { KEY: 1, ... }  or  var NAME = Object.freeze({ ... })  — bareword OR
    # quoted keys, value side ignored.
    m = re.search(r'\b%s\s*=\s*(?:Object\.freeze\()?\{(.*?)\}' % re.escape(name), src, re.S)
    if not m:
        return None
    body = m.group(1)
    keys = set(re.findall(r'([A-Za-z_$][\w$]*)\s*:', body))
    keys |= set(re.findall(r"'([^']*)'\s*:", body)) | set(re.findall(r'"([^"]*)"\s*:', body))
    return keys

# design-parser: prefer the exported set; accept the function-local form when present.
dp_set = objkeys(dp, "MALFORMED_DESIGN_KINDS")
if dp_set is None:
    dp_set = objkeys(dp, "badKind")
# state.js: either a literal set or the single-source alias to design-parser.
st_set = objkeys(st, "MALFORMED_DESIGN_KINDS")
st_alias = re.search(r'MALFORMED_DESIGN_KINDS\s*=\s*[A-Za-z_$][\w$.]*\.MALFORMED_DESIGN_KINDS', st) is not None

bad = 0
if dp_set is None:
    print("MIRROR: could not extract the malformed-design set (MALFORMED_DESIGN_KINDS/badKind) from %s" % dp_path); bad += 1
elif st_set is None and not st_alias:
    print("MIRROR: could not extract MALFORMED_DESIGN_KINDS from %s (no literal and no design-parser alias)" % state_path); bad += 1
elif st_set is not None and dp_set != st_set:
    only_dp = ", ".join(sorted(dp_set - st_set)) or "(none)"
    only_st = ", ".join(sorted(st_set - dp_set)) or "(none)"
    print("MIRROR DRIFT: design-parser MALFORMED_DESIGN_KINDS != state.js MALFORMED_DESIGN_KINDS — "
          "only-parser: %s ; only-state: %s" % (only_dp, only_st)); bad += 1
sys.exit(1 if bad else 0)
PY
  )
  if [ -n "$malformed_report" ]; then
    echo "$malformed_report"
    mirror_drift=$((mirror_drift + 1))
  fi
fi

# 7b: every journal PHASE + every RENDERED STATUS has a label key in every locale.
if [ -f "$LOG_EVENT" ]; then
  label_report=$(node --input-type=module - "$LOG_EVENT" <<'NODE' 2>&1
import { readFileSync } from 'node:fs'
import dictionaries from './orchestrator/site/scripts/i18n/dictionaries/index.js'

const logPath = process.argv[2]
const log = readFileSync(logPath, 'utf8')
function pythonSet(name) {
  const match = log.match(new RegExp(`^${name}\\s*=\\s*\\{([^}]*)\\}`, 'm'))
  if (!match) return null
  return new Set([
    ...match[1].matchAll(/\x22([^\x22]*)\x22/g),
    ...match[1].matchAll(/\x27([^\x27]*)\x27/g),
  ].map((entry) => entry[1]))
}
function labelKeys(locale, prefix) {
  return new Set(Object.keys(dictionaries[locale])
    .filter((key) => key.startsWith(prefix))
    .map((key) => key.slice(prefix.length)))
}

const phases = pythonSet('PHASES')
const statuses = pythonSet('STATUSES')
if (!phases || !statuses) {
  console.log(`LABEL: could not extract PHASES/STATUSES from ${logPath} — has the set literal shape drifted?`)
  process.exit(1)
}
const phaseLabels = new Set([...phases].map((phase) => phase.replaceAll('-', '_')))
const neededStatuses = new Set([...statuses].filter((status) => status !== 'info'))
const locales = ['en', 'ru', 'uk']
let bad = 0
function difference(left, right) {
  return [...left].filter((value) => !right.has(value)).sort()
}
for (const locale of locales) {
  const phaseKeys = labelKeys(locale, 'taskDetails.phase.')
  const statusKeys = labelKeys(locale, 'taskDetails.status.')
  for (const [kind, missing] of [
    ['PHASE(s)', difference(phaseLabels, phaseKeys)],
    ['STATUS(es)', difference(neededStatuses, statusKeys)],
  ]) {
    if (!missing.length) continue
    console.log(`LABEL MISSING: journal ${kind} ${JSON.stringify(missing)} have no key in ${locale} dictionary`)
    bad += 1
  }
}
for (const kind of ['phase', 'status']) {
  const prefix = `taskDetails.${kind}.`
  const expected = labelKeys('en', prefix)
  const enumKeys = kind === 'phase' ? phaseLabels : statuses
  for (const locale of locales.slice(1)) {
    const actual = labelKeys(locale, prefix)
    const onlyEn = difference(new Set([...expected].filter((key) => enumKeys.has(key))), actual)
    const onlyLocale = difference(new Set([...actual].filter((key) => enumKeys.has(key))), expected)
    if (onlyEn.length) {
      console.log(`LABEL PARITY: ${prefix} key(s) ${JSON.stringify(onlyEn)} in en but not ${locale}`)
      bad += 1
    }
    if (onlyLocale.length) {
      console.log(`LABEL PARITY: ${prefix} key(s) ${JSON.stringify(onlyLocale)} in ${locale} but not en`)
      bad += 1
    }
  }
}
process.exit(bad ? 1 : 0)
NODE
  )
  if [ -n "$label_report" ]; then
    echo "$label_report"
    mirror_drift=$((mirror_drift + 1))
  fi
fi

if [ $mirror_drift -gt 0 ]; then
  echo "FAIL: outcome-shape contract / mirror check(s) failed (stale generated module, invalid contract JSON, journal label keys, or the Design-kind alias)."
  fail=$((fail + 1))
fi

# Check 8: the Step-6b–6d finalization block carries NO gate-weakening token. This block in
# outcome-appendix.md is the single load-bearing prose anchor of the mandatory
# visual-evidence gate — the `ship-done.mjs` interlock it invokes is the real enforcement,
# but an edit that slipped `advisory`/`optional`/`may-skip`/`self-skip` into THIS block
# could re-narrate the gate as skippable. Scoped strictly to the block (the rest of the file
# uses these words legitimately, e.g. the "optional 7th heading" field rule), so the
# false-positive surface is near-zero. This is the H5 anti-drift guard.
appendix="orchestrator/skills/task-orchestrator/references/outcome-appendix.md"
if [ -f "$appendix" ]; then
  step6b=$(awk '/^## Steps? 6b/{f=1} f' "$appendix")
  if [ -z "$step6b" ]; then
    echo "GATE PROSE: could not locate the Step-6b finalization block in $appendix"
    fail=$((fail + 1))
  elif echo "$step6b" | grep -niE 'advisory|optional|may[ -]skip|self[ -]skip' >/dev/null; then
    echo "GATE PROSE: Step-6b done-gate block contains a gate-weakening token (advisory|optional|may-skip|self-skip):"
    echo "$step6b" | grep -niE 'advisory|optional|may[ -]skip|self[ -]skip' | sed 's/^/  /'
    fail=$((fail + 1))
  fi
fi

# Check 9: run the explicitly owned tooling suite. Browser template surfaces
# belong only to the aggregate Site suite; the tooling suite proves that a
# configured consumer stamps cleanly, detects drift/unsafe entries, and
# previews/applies an explicit safe sync.
if ! node orchestrator/tests/run-suite.mjs tooling; then
  echo "FAIL: vendored-copy template integrity/sync behavior is inconsistent."
  fail=$((fail + 1))
fi

if [ $fail -gt 0 ]; then
  echo "FAIL: $fail check(s) failed."
  exit 1
fi
n_checks=$(grep -cE '^# Check [0-9]' "$SELF")
echo "OK: all $n_checks lint checks passed (task subdirectories, INDEX.json validity, pending sidecar parity, arch-map freshness, i18n locale parity, npm/workspace/Node/tracked-runtime hygiene, outcome-shape contract + journal-label/Design-kind mirrors, Step-6b gate-prose integrity, vendored-copy tooling E2E)."
