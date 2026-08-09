#!/usr/bin/env python3
"""Generate script-manifest.json — sha256 pins for the figma gate SCRIPTS plus the
enforcement hook + CI workflow.

The gate SCRIPTS *are* the enforcement (design-parser.cjs / ship-done.mjs /
verify-done.mjs / compare-screenshots.mjs / evidence-bundle.mjs / …). Nothing else
detects a silent edit or on-disk corruption of one of them — `figma:doctor` does
presence checks only. This mirrors the proven skills manifest
(skills/_index/_generate_install_manifest.py + reference-hash.sh): doctor asserts
every pin matches AND that no `.mjs`/`.cjs` under scripts/ or top-level `.mjs`
test-infrastructure module is unpinned ("added trusted code, forgot to pin it").

Walk `scripts/**` for `.mjs`/`.cjs`, SKIP only the generated `_index/`;
pin the trusted runtime and every top-level test-infrastructure module plus the
enforcement `hooks/pre-commit`. Paths are repo-root-relative
(the runner modules and hook live outside `scripts/`). Byte grammar blobs
(`.wasm`/`.so`) are naturally excluded by the `.mjs`/`.cjs` filter
(per-machine drift).
"""
import hashlib
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))                    # figma/scripts/_index
SCRIPTS = os.path.normpath(os.path.join(HERE, ".."))                 # figma/scripts
ROOT = os.path.normpath(os.path.join(HERE, "..", "..", "..", ".."))  # repo root
TEST_INFRA = os.path.join(ROOT, "orchestrator", "tests")
SKIP_DIRS = {"_index"}


def sha(path):
    return hashlib.sha256(open(path, "rb").read()).hexdigest()


def rel(path):
    return os.path.relpath(path, ROOT).replace(os.sep, "/")


def main():
    files = {}
    for cur, dirs, names in os.walk(SCRIPTS):
        # Prune only the generated _index/ at the top level. Tests and fixtures
        # live outside scripts/, so every production .mjs/.cjs below scripts/
        # remains inside the content-hash enforcement net.
        if cur == SCRIPTS:
            dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for n in sorted(names):
            if n.endswith(".mjs") or n.endswith(".cjs"):
                p = os.path.join(cur, n)
                files[rel(p)] = sha(p)
    # The top-level test infrastructure owns discovery, suite contracts, and
    # child-process execution. Pin the complete directory convention so a new
    # module cannot silently move trusted runner logic outside the hash net.
    for name in sorted(os.listdir(TEST_INFRA)):
        extra = os.path.join(TEST_INFRA, name)
        if name.endswith(".mjs") and os.path.isfile(extra):
            files[rel(extra)] = sha(extra)
    # Trusted runtime + enforcement hook (outside scripts/).
    for extra in (
        os.path.join(ROOT, "orchestrator", "figma", "runtime", "run-plan.mjs"),
        os.path.join(ROOT, "orchestrator", "skills", "checks", "hooks", "pre-commit"),
    ):
        if os.path.isfile(extra):
            files[rel(extra)] = sha(extra)
    files = dict(sorted(files.items()))
    out = os.path.join(HERE, "script-manifest.json")
    json.dump({"version": 1, "count": len(files), "files": files}, open(out, "w"), indent=2)
    open(out, "a").write("\n")
    print(f"wrote script-manifest.json: {len(files)} files pinned")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
