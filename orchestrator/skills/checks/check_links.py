#!/usr/bin/env python3
"""skills:links — repo-wide relative markdown link existence check.

For every tracked .md under the scanned roots, find `[text](target)` links whose
target is a relative file path (not http(s)/mailto, not a pure #anchor) and
assert the target file exists (anchor suffix stripped). Reports dead links;
exit non-zero if any.
"""
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", "..", ".."))
LINK_RE = re.compile(r"\[[^\]]*\]\(([^)]+)\)")
SCAN_PREFIXES = ("orchestrator/", ".claude/", "README.md")


def tracked_md():
    # Scan the working tree, not just the index: tracked .md PLUS untracked-but-not-
    # gitignored .md under the roots, so a dead link in a WIP doc is caught before commit.
    tracked = subprocess.run(["git", "ls-files"], cwd=ROOT, capture_output=True, text=True).stdout
    untracked = subprocess.run(["git", "ls-files", "--others", "--exclude-standard"],
                               cwd=ROOT, capture_output=True, text=True).stdout
    files = {f for f in (tracked + "\n" + untracked).split("\n") if f.endswith(".md")}
    return sorted(f for f in files if f.startswith(SCAN_PREFIXES))


def main():
    dead = []
    for rel in tracked_md():
        full = os.path.join(ROOT, rel)
        base = os.path.dirname(full)
        try:
            text = open(full, encoding="utf-8").read()
        except OSError:
            continue
        for m in LINK_RE.finditer(text):
            tgt = m.group(1).strip()
            if tgt.startswith(("http://", "https://", "mailto:", "#")):
                continue
            if tgt.startswith("<") and tgt.endswith(">"):
                tgt = tgt[1:-1]
            path = tgt.split("#")[0].split("?")[0]
            if not path or path.startswith(("http", "mailto:")):
                continue
            # skip obvious placeholders / globs
            if any(c in path for c in "*<>") or "{" in path:
                continue
            cand = os.path.normpath(os.path.join(base, path))
            if not os.path.exists(cand):
                dead.append(f"{rel} -> {tgt}")
    if dead:
        for d in dead[:40]:
            print(f"    FAIL dead link: {d}", file=sys.stderr)
        print(f"    {len(dead)} dead relative markdown link(s)", file=sys.stderr)
        return 1
    print("    ok: no dead relative markdown links in scanned roots")
    return 0


if __name__ == "__main__":
    sys.exit(main())
