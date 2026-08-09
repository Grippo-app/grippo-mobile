#!/usr/bin/env python3
"""skills:harness-fidelity — prove the harness detects a
triggered skill + its file reads on a known-good fixture AND does NOT fabricate
them on a known-bad (nothing-triggered) fixture.

NOTE: this proves the harness LOGIC is faithful. Whether the real Claude Code
runtime EMITS such a run-record (triggered skill + ordered reads) is the separate
runtime-signal question — verify that before relying on harness-observed results
outside this fixture gate.
"""
import json, os, sys
HERE=os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
# load observe() directly
ns={}
exec(open(os.path.join(HERE,"observability-harness.py")).read(), ns)
observe=ns["observe"]
FIX=os.path.join(HERE,"fixtures")
fail=0
good=json.load(open(os.path.join(FIX,"harness-good.json")))
ok,info=observe(good)
if not (ok and info.get("entrypoint")=="ui-feature" and len(info.get("sourceReads",[]))==2):
    print(f"    FAIL: harness did not faithfully observe the good fixture: {info}", file=sys.stderr); fail=1
bad=json.load(open(os.path.join(FIX,"harness-bad.json")))
okb,_=observe(bad)
if okb:
    print("    FAIL: harness fabricated a trigger on the nothing-triggered fixture", file=sys.stderr); fail=1
if not fail:
    print("    ok: harness observes good (entrypoint+reads) and rejects bad (no fabrication)")
    print("    note: fixture-only harness check; verify live runtime signals before using observed results elsewhere")
sys.exit(fail)
