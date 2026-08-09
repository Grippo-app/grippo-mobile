#!/usr/bin/env bash
# skills:docs-map — every normative doc surface exists.
set -uo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$DIR/../../.." && pwd)"
python3 - "$ROOT" <<'PY'
import json,os,glob,subprocess,sys
root=sys.argv[1]
d=json.load(open(os.path.join(root,"orchestrator/skills/_index/docs-map.json")))
tracked=set(subprocess.run(
    ["git","ls-files"], cwd=root, capture_output=True, text=True
).stdout.splitlines())
fail=0
checked=0
for x in d["docs"]:
    if x.get("scope") == "template-only" and x["path"] not in tracked: continue
    checked+=1
    if not os.path.isfile(os.path.join(root,x["path"])): print(f"    FAIL: doc missing {x['path']}"); fail=1
for g in d["globs"]:
    if not glob.glob(os.path.join(root,g["glob"])): print(f"    FAIL: glob matched nothing {g['glob']}"); fail=1
if not fail: print(f"    ok: {checked} docs + {len(d['globs'])} globs all present")
sys.exit(fail)
PY
