#!/usr/bin/env python3
"""Generate install-manifest.json from the authored skill packages.

implement-figma is recorded as the external-source exception because its
canonical source stays under orchestrator/figma/skill/.
"""
import hashlib
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
SKILLS = os.path.normpath(os.path.join(HERE, ".."))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
SKIP = {"_index", "checks"}


def sha(path):
    return hashlib.sha256(open(path, "rb").read()).hexdigest() if os.path.isfile(path) else None


def fm(path):
    t = open(path, encoding="utf-8").read()
    m = re.match(r"^---\n(.*?)\n---\n", t, re.DOTALL)
    d = {}
    if m:
        lines = m.group(1).split("\n")
        i = 0
        while i < len(lines):
            ln = lines[i]
            km = re.match(r"^([A-Za-z][A-Za-z0-9_-]*):[ \t]*(.*)$", ln)
            if km:
                key, value = km.group(1), km.group(2).strip()
                if value in {">", ">-", ">+", "|", "|-", "|+"}:
                    block = []
                    i += 1
                    while i < len(lines) and not re.match(r"^[A-Za-z][A-Za-z0-9_-]*:[ \t]*", lines[i]):
                        block.append(lines[i])
                        i += 1
                    if value.startswith(">"):
                        d[key] = " ".join(part.strip() for part in block if part.strip())
                    else:
                        d[key] = "\n".join(part[2:] if part.startswith("  ") else part for part in block)
                    continue
                d[key] = value
            i += 1
    return d


def reference_hashes(skill_dir):
    refs = {}
    refdir = os.path.join(skill_dir, "references")
    if os.path.isdir(refdir):
        for cur, _, files in os.walk(refdir):
            for rf in sorted(files):
                if rf.endswith(".md"):
                    p = os.path.join(cur, rf)
                    refs[os.path.relpath(p, skill_dir)] = sha(p)
    return dict(sorted(refs.items()))


def main():
    skills = []
    for name in sorted(os.listdir(SKILLS)):
        d = os.path.join(SKILLS, name)
        if name in SKIP or not os.path.isdir(d):
            continue
        sk = os.path.join(d, "SKILL.md")
        if not os.path.isfile(sk):
            continue
        f = fm(sk)
        refs = reference_hashes(d)
        skills.append({
            "folderName": name,
            "frontmatterName": f.get("name", ""),
            "frontmatterDescriptionSha256": hashlib.sha256(f.get("description", "").encode()).hexdigest(),
            "sourcePath": f"orchestrator/skills/{name}/SKILL.md",
            "installPath": f".claude/skills/{name}/SKILL.md",
            "sourceSha256": sha(sk),
            "referenceSha256s": refs,
        })
    # external exception: implement-figma keeps its existing canonical source
    figma_src = os.path.join(ROOT, "figma", "skill", "SKILL.md")
    figma_fm = fm(figma_src)
    skills.append({
        "folderName": "implement-figma",
        "frontmatterName": "implement-figma",
        "frontmatterDescriptionSha256": hashlib.sha256(figma_fm.get("description", "").encode()).hexdigest(),
        "sourcePath": "orchestrator/figma/skill/SKILL.md",
        "installPath": ".claude/skills/implement-figma/SKILL.md",
        "sourceSha256": sha(figma_src),
        "externalSourceException": True,
        "note": "canonical source stays orchestrator/figma/skill/SKILL.md",
    })
    out = os.path.join(HERE, "install-manifest.json")
    json.dump({"version": 1, "count": len(skills), "skills": skills}, open(out, "w"), indent=2)
    open(out, "a").write("\n")
    print(f"wrote install-manifest.json: {len(skills)} skills "
          f"({len(skills)-1} authored skeletons + implement-figma external)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
