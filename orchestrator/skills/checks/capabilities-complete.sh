#!/usr/bin/env bash
# skills:capabilities-complete — every operation maps to an existing skill; site
# action/lock-stage/session keys match the expected inventory (no drift).
set -uo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$DIR/../../.." && pwd)"
python3 - "$ROOT" <<'PY'
import json,os,sys
root=sys.argv[1]
cap=json.load(open(os.path.join(root,"orchestrator/skills/_index/capabilities.json")))
man=json.load(open(os.path.join(root,"orchestrator/skills/_index/install-manifest.json")))
skills={s["folderName"] for s in man["skills"]}
ACTIONS={"prep","answers","run","drop",None}                           # frozen REQUEST_ACTIONS
STAGES={"task-prep","orchestrator",None}                                # frozen LOCK_STAGES
SESSION_KEYS={"task:<stem>","figma:*","contract:*","setup",None}
EXPECTED={
    "task.prep": ("prep", "task-prep", "task:<stem>", "task-prep"),
    "task.answers": ("answers", "task-prep", "task:<stem>", "task-prep"),
    "task.run": ("run", "orchestrator", "task:<stem>", "task-orchestrator"),
    "task.drop": ("drop", None, None, "task-orchestrator"),
    "figma.run": (None, None, "figma:*", "implement-figma"),
    "contract.run": (None, None, "contract:*", "backend-contract-client"),
    "setup.bootstrap": (None, None, "setup", "launch-readiness"),
}
fail=0
seen=set()
for o in cap["operations"]:
    op=o.get("operation")
    if op in seen:
        print(f"    FAIL: duplicate operation {op}"); fail=1
    seen.add(op)
    expected=EXPECTED.get(op)
    if not expected:
        print(f"    FAIL: unexpected operation {op}"); fail=1
    elif (o.get("requestAction"), o.get("lockStage"), o.get("sessionKeyPattern"), o.get("skill")) != expected:
        print(f"    FAIL: operation {op} tuple drifted"); fail=1
    if o["skill"] not in skills:
        print(f"    FAIL: operation {o['operation']} -> unknown skill {o['skill']}"); fail=1
    if o["requestAction"] not in ACTIONS:
        print(f"    FAIL: {o['operation']} requestAction {o['requestAction']} not in frozen set"); fail=1
    if o["lockStage"] not in STAGES:
        print(f"    FAIL: {o['operation']} lockStage {o['lockStage']} not in frozen set"); fail=1
    if o.get("sessionKeyPattern") not in SESSION_KEYS:
        print(f"    FAIL: {o['operation']} sessionKeyPattern {o.get('sessionKeyPattern')} not in frozen set"); fail=1
missing=set(EXPECTED)-seen
if missing:
    print(f"    FAIL: missing operation(s): {', '.join(sorted(missing))}"); fail=1
if not fail: print(f"    ok: {len(cap['operations'])} operations map to real skills; site action/lock keys match the expected inventory")
sys.exit(fail)
PY
