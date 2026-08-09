#!/usr/bin/env bash
# Regression guard for the task-state core's canonical `## Outcome` parser.
# `task-state-core.cjs` drives INDEX derivation and the Site consumes its
# server-side projection; there is deliberately no browser parser mirror.
# regen-index.py is only a side-effect-free-on-import Python entrypoint;
# task-index.mjs owns the verified kernel lock.
#
# Specifically proves the optional `### Execution log` digest (the pipeline-
# journal feature) is parser-ADDITIVE: appended after `### Files touched` it
# never flips a valid appendix to `malformed`, while placing it before
# `**Status**` DOES (the head-slice rule). Run from
# the repo root (or anywhere; it resolves paths from its own location):
#
#   bash orchestrator/tasks/tests/test-regen-index.sh
set -u
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
PY="$ROOT/orchestrator/tasks/regen-index.py"
CORE="$ROOT/orchestrator/tasks/task-state-core.cjs"
SHAPE="$ROOT/orchestrator/contracts/outcome-shape.json"
# Suppress test-only Python bytecode so no cache is left beside the scripts.
export PYTHONDONTWRITEBYTECODE=1
pass=0; fail=0
ok()  { echo "  ok: $1"; pass=$((pass+1)); }
bad() { echo "  FAIL: $1"; fail=$((fail+1)); }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
# Direct facade calls below publish only inside this throwaway fixture. Import
# behavior itself is covered as a strict zero-mutation contract by the INDEX
# fail-closed suite.
mkdir -p "$TMP/orchestrator/tasks/backlog" "$TMP/orchestrator/tasks/pending" \
         "$TMP/orchestrator/tasks/todo" "$TMP/orchestrator/tasks/done"

HEAD='# TASK 1 — demo

## Goal
g

---

## Outcome

**Status**: completed
**Completed at**: 2026-01-01T00:00:00Z
**Reviewer**: codex
**Review iterations**: 1

### Build gates
- `x` — pass

### Runtime verify
- Gate: skipped (no runtime-observable change)
- Result: n/a — none

### Acceptance trace
- `a` — verified — ok

### Caveats
- none

### Follow-ups
- none

### Files touched
- `f` — modified'

EXEC='

### Execution log
- Phases: intake, builders, ship
- Totals: 5m · stops 0 · retries 0
- Figma: final PASS · 2 screenshots · run run-1 · report sha256:111111111111… · fresh
- Figma meta: schema=figma-comparison-v2; taskStem=TASK_1_fixture; stage=final; overall=PASS; pipelineRunId=run-1; evidenceReportHash=sha256:1111111111111111111111111111111111111111111111111111111111111111; screenshotReportHash=sha256:2222222222222222222222222222222222222222222222222222222222222222; generatedAt=2026-01-01T00:00:00Z; stale=false; visualChecks=2; problemCount=0; designHash=sha256:3333333333333333333333333333333333333333333333333333333333333333; gatePolicyVersion=1; tokenObservationManifestHash=sha256:4444444444444444444444444444444444444444444444444444444444444444; rows=Home/primary:PASS'

# Variant A — Execution log AFTER Files touched → must stay VALID.
printf '%s%s\n' "$HEAD" "$EXEC" > "$TMP/A.md"
# Variant C — a non-UI done file without an Execution log → must stay VALID.
printf '%s\n' "$HEAD" > "$TMP/C.md"
# Variant D — invalid Reviewer → must be MALFORMED in both parsers.
printf '%s\n' "$HEAD" | sed 's/^\*\*Reviewer\*\*: codex$/**Reviewer**: human/' > "$TMP/D.md"
# Variant E — a required heading exists but its body is empty → MALFORMED.
printf '%s\n' "$HEAD" | awk '
  skip_next_none && $0 == "- none" { skip_next_none = 0; next }
  { print; skip_next_none = ($0 == "### Caveats") }
' > "$TMP/E.md"
# Variant K — acceptance verdict is contract-owned and an unknown token is
# malformed in both the canonical core and browser projection.
printf '%s\n' "$HEAD" | sed 's/ — verified — / — guessed — /' > "$TMP/K.md"
# Variant B — Execution log BEFORE **Status** (right after ## Outcome) → must be
# MALFORMED (it becomes the first ### heading, so the head-slice ends before
# Status → Status invisible). Built explicitly.
cat > "$TMP/B.md" <<'EOF'
# TASK 1 — demo

## Goal
g

---

## Outcome

### Execution log
- Phases: intake

**Status**: completed
**Reviewer**: codex

### Build gates
- `x` — pass

### Runtime verify
- Gate: skipped (no runtime-observable change)
- Result: n/a — none

### Acceptance trace
- `a` — verified — ok

### Caveats
- none

### Follow-ups
- none

### Files touched
- `f` — modified
EOF

core_status() {
  node -e "
const fs = require('node:fs');
const core = require(process.argv[1]);
const shape = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const parsed = core.parseOutcome(fs.readFileSync(process.argv[3], 'utf8'), shape);
console.log(parsed.valid ? parsed.status : 'malformed');
" "$CORE" "$SHAPE" "$1"
}
check() { # $1 label  $2 fixture  $3 expected
  local lbl="$1" fx="$2" want="$3" core
  core="$(core_status "$fx")"
  [ "$core" = "$want" ] && ok "$lbl — Core = $want" || bad "$lbl — Core = '$core' (want '$want')"
}

echo "Canonical Outcome-appendix parser:"
check "A: Execution log after Files touched" "$TMP/A.md" "completed"
check "C: no Execution log"                  "$TMP/C.md" "completed"
check "B: Execution log before Status"        "$TMP/B.md" "malformed"
check "D: invalid Reviewer"                   "$TMP/D.md" "malformed"
check "E: empty required section"             "$TMP/E.md" "malformed"
check "K: invalid acceptance verdict"          "$TMP/K.md" "malformed"

cat > "$TMP/Q.md" <<'EOF'
---
forTask: TASK_1_demo
createdAt: 2026-01-01T00:00:00Z
round: 1
---

## Q1 — Em dash question
**Type**: text

### Answer

Pending

## Q2 - ASCII hyphen question
**Type**: text

### Answer

Pending
EOF
core_q_count="$(node -e "
const fs = require('node:fs');
const core = require(process.argv[1]);
console.log(core.parsePending(fs.readFileSync(process.argv[2], 'utf8')).questions.length);
" "$CORE" "$TMP/Q.md")"
[ "$core_q_count" = "2" ] && ok "H: Core counts em dash + ASCII hyphen questions" || bad "H: Core question count = '$core_q_count'"

# Done entries preserve dependencies so Task Details can show the
# completed task's chain after the file moves out of todo/.
mkdir -p "$TMP/orchestrator/tasks/done"
cat > "$TMP/orchestrator/tasks/done/TASK_2_dep_done.md" <<'EOF'
# TASK 2 — dep done

## Source

- Kind: manual
- Type: manual
- Ref: fixture:regen-index-done
- Fingerprint: sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

## Goal
g

## Depends on-call
- TASK_9_false_positive

## Depends on (optional)
- `TASK_1_parent`

---

## Outcome

**Status**: completed
**Completed at**: 2026-01-01T00:00:00Z
**Reviewer**: codex
**Review iterations**: 1

### Build gates
- `x` — pass

### Runtime verify
- Gate: skipped (no runtime-observable change)
- Result: n/a — none

### Acceptance trace
- `a` — verified — ok

### Caveats
- none

### Follow-ups
- none

### Files touched
- `f` — modified
EOF
depends_json="$(cd "$TMP" && python3 "$PY" >/dev/null && python3 - <<'PY'
import json
with open('orchestrator/tasks/INDEX.json', encoding='utf-8') as f:
    idx = json.load(f)
row = next((e for e in idx.get('done', []) if e.get('stem') == 'TASK_2_dep_done'), {})
print(json.dumps(row.get('dependsOn', [])))
PY
)"
[ "$depends_json" = '["TASK_1_parent"]' ] && ok "F: done INDEX preserves dependsOn" || bad "F: done INDEX dependsOn = '$depends_json'"

cat > "$TMP/orchestrator/tasks/todo/TASK_3_dep_todo.md" <<'EOF'
# TASK 3 — dep todo

## Source

- Kind: manual
- Type: manual
- Ref: fixture:regen-index-todo
- Fingerprint: sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb

## Goal
g

## Inputs
- Existing repository contracts.

## Depends on
- `TASK_4_parent`

## Acceptance

### Automated
- Run `node test/contract.mjs`.

### Manual
- Inspect the result.

## Out of scope
- Unrelated refactors.
EOF
todo_depends_json="$(cd "$TMP" && python3 "$PY" >/dev/null && python3 - <<'PY'
import json
with open('orchestrator/tasks/INDEX.json', encoding='utf-8') as f:
    idx = json.load(f)
row = next((e for e in idx.get('todo', []) if e.get('stem') == 'TASK_3_dep_todo'), {})
print(json.dumps(row.get('dependsOn', [])))
PY
)"
[ "$todo_depends_json" = '["TASK_4_parent"]' ] && ok "G: todo INDEX preserves dependsOn" || bad "G: todo INDEX dependsOn = '$todo_depends_json'"

echo "test-regen-index.sh: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
