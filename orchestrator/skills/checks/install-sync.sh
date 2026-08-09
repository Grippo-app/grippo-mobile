#!/usr/bin/env bash
# skills:install-sync — if skills are installed at <target>/.claude/skills/, verify
# each installed SKILL.md matches its canonical source (install-manifest hash).
# Default target = repo root; on the template (no product skills installed) it
# passes with "nothing installed yet" (install happens at launch Step 14).
set -uo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$DIR/../../.." && pwd)"
TARGET="${1:-$ROOT}"
python3 - "$ROOT" "$TARGET" <<'PY'
import json,os,hashlib,sys
root,target=sys.argv[1],sys.argv[2]
man=json.load(open(os.path.join(root,"orchestrator/skills/_index/install-manifest.json")))
fail=0; checked=0
def sha(path):
    return hashlib.sha256(open(path,'rb').read()).hexdigest()
def actual_refs(base):
    refdir=os.path.join(base,"references")
    out=set()
    if os.path.isdir(refdir):
        for cur,_,files in os.walk(refdir):
            for f in files:
                p=os.path.join(cur,f)
                out.add(os.path.relpath(p,base))
    return out
required=[s for s in man["skills"] if not s.get("externalSourceException")]
any_required_installed=any(os.path.isfile(os.path.join(target,s["installPath"])) for s in required)
if any_required_installed:
    for s in required:
        if not os.path.isfile(os.path.join(target,s["installPath"])):
            print(f"    FAIL: partial skill install: {s['folderName']} is missing"); fail=1
for s in man["skills"]:
    inst=os.path.join(target,s["installPath"])
    if not os.path.isfile(inst): continue
    checked+=1
    cur=sha(inst)
    if s.get("sourceSha256") and cur!=s["sourceSha256"]:
        print(f"    FAIL: installed {s['folderName']} drifts from canonical source"); fail=1
    base=os.path.dirname(inst)
    expected=set((s.get("referenceSha256s") or {}).keys())
    actual=actual_refs(base)
    for rel in sorted(actual-expected):
        print(f"    FAIL: installed {s['folderName']}/{rel} is not in install-manifest"); fail=1
    for rel in sorted(expected-actual):
        print(f"    FAIL: installed {s['folderName']}/{rel} is missing"); fail=1
    for rel,h in (s.get("referenceSha256s") or {}).items():
        p=os.path.join(base,rel)
        if os.path.isfile(p) and sha(p)!=h:
            print(f"    FAIL: installed {s['folderName']}/{rel} drifts from canonical source"); fail=1
if not fail:
    print(f"    ok: {checked} installed skill(s) match source" if checked else
          "    ok: no product skills installed in this checkout (deploy at launch Step 14) — nothing to drift")
sys.exit(fail)
PY
