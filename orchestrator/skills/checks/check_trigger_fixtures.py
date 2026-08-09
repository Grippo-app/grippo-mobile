#!/usr/bin/env python3
"""Verify trigger fixtures and reject duplicate primary skill triggers."""
import json, os, sys, collections
HERE=os.path.dirname(os.path.abspath(__file__))
SKILLS=os.path.normpath(os.path.join(HERE,".."))
SKIP={"_index","checks"}
fail=0
primary=collections.defaultdict(list)
n_prompts=0
skills=[d for d in sorted(os.listdir(SKILLS)) if os.path.isdir(os.path.join(SKILLS,d)) and d not in SKIP]
for name in skills:
    fx=os.path.join(SKILLS,name,"fixtures")
    pm=os.path.join(fx,"prompts.md"); er=os.path.join(fx,"expected-routing.json")
    if not os.path.isfile(pm): print(f"    FAIL: {name} missing fixtures/prompts.md",file=sys.stderr); fail=1
    if not os.path.isfile(er): print(f"    FAIL: {name} missing fixtures/expected-routing.json",file=sys.stderr); fail=1; continue
    try: d=json.load(open(er))
    except Exception as e: print(f"    FAIL: {name} bad JSON: {e}",file=sys.stderr); fail=1; continue
    if d.get("skill")!=name: print(f"    FAIL: {name} expected-routing skill!={name}",file=sys.stderr); fail=1
    for t in d.get("primaryTriggers",[]):
        primary[t.strip().lower()].append(name)
    n_prompts+=len(d.get("prompts",[]))
# cross-skill duplicate primary triggers
dups={t:s for t,s in primary.items() if len(set(s))>1}
for t,s in dups.items():
    print(f"    FAIL: primary trigger '{t}' claimed by {sorted(set(s))}",file=sys.stderr); fail=1
# collision report
if not os.path.isfile(os.path.join(SKILLS,"_index","trigger-collision.md")):
    print("    FAIL: _index/trigger-collision.md missing",file=sys.stderr); fail=1
if not fail:
    print(f"    ok: {len(skills)} skills, {n_prompts} prompts, 0 cross-skill duplicate primary triggers, collision report present")
sys.exit(fail)
