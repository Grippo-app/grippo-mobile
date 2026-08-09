#!/usr/bin/env python3
"""skills:skeleton — validate authored skill packages + install manifest.

For each skill dir under orchestrator/skills/ (excluding _index, checks):
- SKILL.md exists with YAML frontmatter; `name` == folder name.
- non-empty `description`.
- required operational sections present.
- references/index.md exists.
And the install-manifest lists every authored skill with matching folder/name.
"""
import json
import hashlib
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SKILLS = os.path.normpath(os.path.join(HERE, ".."))
ROOT = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
MANIFEST = os.path.join(SKILLS, "_index", "install-manifest.json")
SKIP = {"_index", "checks"}
REQUIRED_SECTIONS = [
    "## When to use",
    "## Required inputs",
    "## Workflow",
    "## Stop and ask",
    "## References to read",
    "## Validators / gates",
    "## Output contract",
]


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
    return d, t


def main():
    fail = 0
    authored = []
    for name in sorted(os.listdir(SKILLS)):
        d = os.path.join(SKILLS, name)
        if name in SKIP or not os.path.isdir(d):
            continue
        authored.append(name)
        sk = os.path.join(d, "SKILL.md")
        if not os.path.isfile(sk):
            print(f"    FAIL: {name}/SKILL.md missing", file=sys.stderr); fail = 1; continue
        f, body = fm(sk)
        if set(f.keys()) != {"name", "description"}:
            print(f"    FAIL: {name} frontmatter keys {sorted(f.keys())} != ['description', 'name']", file=sys.stderr); fail = 1
        if f.get("name") != name:
            print(f"    FAIL: {name} frontmatter name={f.get('name')!r} != folder", file=sys.stderr); fail = 1
        if not f.get("description"):
            print(f"    FAIL: {name} empty description", file=sys.stderr); fail = 1
        for sec in REQUIRED_SECTIONS:
            if sec not in body:
                print(f"    FAIL: {name} missing section {sec}", file=sys.stderr); fail = 1
        # Every skill is self-contained and must route its references through index.md.
        ix = os.path.join(d, "references", "index.md")
        if not os.path.isfile(ix):
            print(f"    FAIL: {name} has no routing index (references/index.md)", file=sys.stderr); fail = 1

    # manifest cross-check
    if not os.path.isfile(MANIFEST):
        print("    FAIL: install-manifest.json missing", file=sys.stderr); fail = 1
    else:
        man = json.load(open(MANIFEST))
        manskills = {s["folderName"] for s in man["skills"]}
        for name in authored:
            if name not in manskills:
                print(f"    FAIL: {name} not in install-manifest", file=sys.stderr); fail = 1
        for s in man["skills"]:
            if s["folderName"] != s["frontmatterName"]:
                print(f"    FAIL: manifest {s['folderName']} folder!=frontmatterName", file=sys.stderr); fail = 1
            source = s.get("sourcePath")
            if source:
                source_path = os.path.join(ROOT, source)
                if os.path.isfile(source_path):
                    sfm, _ = fm(source_path)
                    desc_hash = hashlib.sha256(sfm.get("description", "").encode()).hexdigest()
                    if s.get("frontmatterDescriptionSha256") != desc_hash:
                        print(f"    FAIL: manifest {s['folderName']} description hash drift vs source", file=sys.stderr); fail = 1
            # reverse cross-check: a manifest skill (unless it sources externally,
            # e.g. implement-figma) must have an on-disk skill dir.
            if not s.get("externalSourceException") and not os.path.isdir(os.path.join(SKILLS, s["folderName"])):
                print(f"    FAIL: manifest skill {s['folderName']} has no on-disk dir", file=sys.stderr); fail = 1

    if not fail:
        print(f"    ok: {len(authored)} skeletons valid (folder==name, sections, refs); manifest consistent")
    return fail


if __name__ == "__main__":
    sys.exit(main())
