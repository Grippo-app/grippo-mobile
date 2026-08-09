#!/usr/bin/env bash
# skills:prompt-surfaces — every executable prompt factory exists.
set -uo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$DIR/../../.." && pwd)"
python3 - "$ROOT" <<'PY'
import json,os,sys
root=sys.argv[1]
d=json.load(open(os.path.join(root,"orchestrator/skills/_index/prompt-surfaces.json")))
miss=[s["path"] for s in d["surfaces"] if not os.path.isfile(os.path.join(root,s["path"]))]
for m in miss: print(f"    FAIL: prompt surface missing {m}")
if not miss: print(f"    ok: all {len(d['surfaces'])} prompt surfaces present")
sys.exit(1 if miss else 0)
PY
