#!/usr/bin/env bash
# skills:reference-hash — skill sources + reference files match the hashes recorded in install-manifest.
set -uo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"; ROOT="$(cd "$DIR/../../.." && pwd)"
python3 - "$ROOT" <<'PY'
import json,os,hashlib,sys
root=sys.argv[1]
man=json.load(open(os.path.join(root,"orchestrator/skills/_index/install-manifest.json")))
fail=0
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
for s in man["skills"]:
    source=s.get("sourcePath")
    if source:
        p=os.path.join(root,source)
        if not os.path.isfile(p): print(f"    FAIL: {source} missing"); fail=1
        else:
            cur=sha(p)
            if cur!=s.get("sourceSha256"): print(f"    FAIL: {source} hash drift vs manifest"); fail=1
    base=os.path.dirname(os.path.join(root,source)) if source else os.path.join(root,"orchestrator/skills",s["folderName"])
    expected=set((s.get("referenceSha256s") or {}).keys())
    actual=actual_refs(base)
    for rel in sorted(actual-expected):
        print(f"    FAIL: {s['folderName']}/{rel} exists but is missing from install-manifest"); fail=1
    for rel in sorted(expected-actual):
        print(f"    FAIL: {s['folderName']}/{rel} listed in install-manifest but missing on disk"); fail=1
    for rel,h in (s.get("referenceSha256s") or {}).items():
        p=os.path.join(base,rel)
        if not os.path.isfile(p): print(f"    FAIL: {s['folderName']}/{rel} missing"); fail=1; continue
        cur=sha(p)
        if cur!=h: print(f"    FAIL: {s['folderName']}/{rel} hash drift vs manifest"); fail=1
files=man.get("files") or []
expected_sources={
    'orchestrator/skills/_index/install-surfaces/commands/serve-queue.md',
    'orchestrator/skills/_index/install-surfaces/launch.json',
}
contracts=os.path.join(root,'orchestrator/contracts')
for cur,dirs,names in os.walk(contracts):
    for name in names:
        expected_sources.add(os.path.relpath(os.path.join(cur,name),root).replace(os.sep,'/'))
manifest_sources=[f.get('sourcePath') for f in files]
manifest_destinations=[f.get('installPath') for f in files]
if len(manifest_sources)!=len(set(manifest_sources)) or len(manifest_destinations)!=len(set(manifest_destinations)):
    print("    FAIL: install-manifest file source/destination paths are not unique"); fail=1
if set(manifest_sources)!=expected_sources:
    print("    FAIL: install-manifest file surface is incomplete or contains retired sources"); fail=1
for f in files:
    source=f.get('sourcePath')
    p=os.path.join(root,source or '')
    if not source or not os.path.isfile(p): print(f"    FAIL: installed-file source missing {source}"); fail=1
    elif sha(p)!=f.get('sourceSha256'): print(f"    FAIL: {source} hash drift vs manifest"); fail=1
if not fail: print("    ok: all skill, reference, contract, command, and launch sources match install-manifest hashes")
sys.exit(fail)
PY
