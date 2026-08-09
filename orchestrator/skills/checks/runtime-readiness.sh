#!/usr/bin/env bash
# skills:runtime-readiness — skills runtime is ready when the install manifest +
# capabilities + skeletons are consistent (derived from manifests, not '>=40 agents').
set -uo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$DIR/../../.." && pwd)"
python3 - "$ROOT" <<'PY'
import json,os,sys
root=sys.argv[1]
man=json.load(open(os.path.join(root,"orchestrator/skills/_index/install-manifest.json")))
cap=json.load(open(os.path.join(root,"orchestrator/skills/_index/capabilities.json")))
fail=0
authored=[s for s in man["skills"] if not s.get("externalSourceException")]
if man.get("count") != len(man["skills"]): print(f"    FAIL: manifest count {man.get('count')} != skills length {len(man['skills'])}"); fail=1
for s in authored:
    if not os.path.isfile(os.path.join(root,s["sourcePath"])):
        print(f"    FAIL: skill source missing {s['sourcePath']}"); fail=1
if len(authored)<11: print(f"    FAIL: only {len(authored)} authored skills (<11)"); fail=1
if len(authored)!=11: print(f"    FAIL: expected exactly 11 authored skills, got {len(authored)}"); fail=1
if len(man["skills"])!=12: print(f"    FAIL: expected 12 manifest skills including implement-figma, got {len(man['skills'])}"); fail=1
expected_ops={"task.prep","task.answers","task.run","task.drop","figma.run","contract.run","setup.bootstrap"}
actual_ops=[op.get("operation") for op in cap["operations"]]
if len(actual_ops)!=len(set(actual_ops)) or set(actual_ops)!=expected_ops:
    print(f"    FAIL: capabilities mismatch: expected {sorted(expected_ops)}, got {sorted(actual_ops)}"); fail=1
if not fail: print(f"    ok: runtime-ready — {len(authored)} skills + {len(cap['operations'])} operations, manifest-derived")
sys.exit(fail)
PY
