#!/usr/bin/env python3
"""Verify that installed skills read only their self-contained reference packs."""

import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SKILLS = os.path.normpath(os.path.join(HERE, ".."))
ROOT = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
MANIFEST = os.path.join(SKILLS, "_index", "install-manifest.json")

# Bare numbered chapters and role-file paths are rejected so an incomplete
# relative link cannot bypass the self-containment check.
FORBIDDEN = re.compile(
    r"\b\d{2}-[a-z][a-z-]+/"
    r"|\b\d{2}-[a-z][a-z0-9-]+\.md"
    r"|\bsub-agents/"
    r"|\b(?:helpers|builders|validators)/[a-z][a-z0-9-]*\.md"
)
COMMAND = re.compile(
    r"\[\s*-[fed]\s|`?node\s|\brg\s+-|\$\(|FIGMA_|=<|python3?\s|bash\s|\.sh\b|\.mjs\b|\.py\b|scripts/"
)


def scan(path):
    hits = []
    with open(path, encoding="utf-8") as handle:
        for line_number, line in enumerate(handle, 1):
            if FORBIDDEN.search(line) and not COMMAND.search(line):
                hits.append((line_number, line.strip()[:110]))
    return hits


def skill_entries():
    with open(MANIFEST, encoding="utf-8") as handle:
        manifest = json.load(handle)
    entries = []
    for item in manifest.get("skills", []):
        source = item.get("sourcePath")
        if not source:
            continue
        skill_path = os.path.join(ROOT, source)
        skill_dir = os.path.dirname(skill_path)
        entries.append((item.get("folderName") or os.path.basename(skill_dir), skill_dir, skill_path))
    return sorted(entries)


def main():
    checked = []
    failed = False
    for name, directory, skill_file in skill_entries():
        checked.append(name)
        files = [skill_file]
        references = os.path.join(directory, "references")
        if os.path.isdir(references):
            for current, _, names in os.walk(references):
                files.extend(os.path.join(current, filename) for filename in names if filename.endswith(".md"))
        for path in files:
            for line_number, text in scan(path):
                relative = os.path.relpath(path, SKILLS)
                print(f"    unsupported reference read: {relative}:{line_number}  {text}", file=sys.stderr)
                failed = True
    print(f"    manifest skills scanned: {len(checked)}")
    if not failed:
        print(f"    ok: {len(checked)} skills use only self-contained references")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
