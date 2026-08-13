#!/usr/bin/env python3
"""Fail-closed validator for the skill/install manifest consumed by installers."""

import hashlib
import json
import os
import re
import sys


SHA_RE = re.compile(r"^[a-f0-9]{64}$")
FOLDER_RE = re.compile(r"^[a-z0-9][a-z0-9-]*$")


def fail(message):
    raise ValueError(message)


def exact(value, keys, label):
    if not isinstance(value, dict) or set(value) != set(keys):
        fail(f"{label} has non-current fields")


def relative_path(value, label):
    if not isinstance(value, str) or not value or "\\" in value or "\0" in value:
        fail(f"{label} is not a canonical relative path")
    parts = value.split("/")
    if value.startswith("/") or any(part in {"", ".", ".."} for part in parts):
        fail(f"{label} is not a canonical relative path")
    return value


def regular_source(root, relative, expected_hash, label):
    relative_path(relative, label)
    absolute = os.path.join(root, *relative.split("/"))
    try:
        stat = os.lstat(absolute)
    except OSError as error:
        fail(f"{label} is unavailable: {error}")
    if os.path.islink(absolute) or not os.path.isfile(absolute):
        fail(f"{label} is not a regular non-symlink file")
    if not SHA_RE.fullmatch(str(expected_hash or "")):
        fail(f"{label} has an invalid sha256")
    with open(absolute, "rb") as handle:
        actual_hash = hashlib.sha256(handle.read()).hexdigest()
    if actual_hash != expected_hash:
        fail(f"{label} hash differs from the manifest")
    return stat


def contract_mappings(root):
    mappings = {
        "orchestrator/skills/_index/install-surfaces/commands/serve-queue.md":
            ".claude/commands/serve-queue.md",
        "orchestrator/skills/_index/install-surfaces/launch.json":
            ".claude/launch.json",
    }
    contracts = os.path.join(root, "orchestrator", "contracts")
    for current, directories, names in os.walk(contracts, followlinks=False):
        directories.sort()
        names.sort()
        for directory in directories:
            if os.path.islink(os.path.join(current, directory)):
                fail("contracts source contains a symlink directory")
        for name in names:
            absolute = os.path.join(current, name)
            if os.path.islink(absolute) or not os.path.isfile(absolute):
                fail("contracts source contains a non-regular entry")
            suffix = os.path.relpath(absolute, contracts).replace(os.sep, "/")
            mappings["orchestrator/contracts/" + suffix] = ".claude/contracts/" + suffix
    return mappings


def validate(root, manifest_path):
    with open(manifest_path, encoding="utf-8") as handle:
        manifest = json.load(handle)
    exact(manifest, ["version", "count", "skills", "files"], "install manifest")
    if manifest["version"] != 1 or not isinstance(manifest["skills"], list) or \
            manifest["count"] != len(manifest["skills"]) or not isinstance(manifest["files"], list):
        fail("install manifest version/count/collections are invalid")

    seen_folders = set()
    for skill in manifest["skills"]:
        external = skill.get("externalSourceException") is True
        fields = [
            "folderName", "frontmatterName", "frontmatterDescriptionSha256",
            "sourcePath", "installPath", "sourceSha256",
        ]
        fields += ["externalSourceException", "note"] if external else ["referenceSha256s"]
        exact(skill, fields, "skill entry")
        folder = skill["folderName"]
        if not FOLDER_RE.fullmatch(str(folder or "")) or folder in seen_folders:
            fail("skill folder names are invalid or duplicated")
        seen_folders.add(folder)
        if skill["frontmatterName"] != folder:
            fail(f"skill {folder} frontmatter name differs from its folder")
        expected_source = ("orchestrator/figma/skill/SKILL.md" if external else
                           f"orchestrator/skills/{folder}/SKILL.md")
        expected_install = f".claude/skills/{folder}/SKILL.md"
        if skill["sourcePath"] != expected_source or skill["installPath"] != expected_install:
            fail(f"skill {folder} source/install mapping is non-canonical")
        if external and (folder != "implement-figma" or
                         skill["note"] != "canonical source stays orchestrator/figma/skill/SKILL.md"):
            fail("external skill exception is non-canonical")
        regular_source(root, skill["sourcePath"], skill["sourceSha256"], f"skill {folder}")
        if not SHA_RE.fullmatch(str(skill["frontmatterDescriptionSha256"] or "")):
            fail(f"skill {folder} description hash is invalid")
        if not external:
            references = skill["referenceSha256s"]
            if not isinstance(references, dict):
                fail(f"skill {folder} references are invalid")
            reference_root = os.path.join(root, "orchestrator", "skills", folder, "references")
            actual = set()
            for current, directories, names in os.walk(reference_root, followlinks=False):
                directories.sort()
                names.sort()
                for directory in directories:
                    if os.path.islink(os.path.join(current, directory)):
                        fail(f"skill {folder} contains a symlink reference directory")
                for name in names:
                    absolute = os.path.join(current, name)
                    if os.path.islink(absolute) or not os.path.isfile(absolute):
                        fail(f"skill {folder} contains a non-regular reference")
                    actual.add(os.path.relpath(absolute, os.path.join(root, "orchestrator", "skills", folder))
                               .replace(os.sep, "/"))
            if actual != set(references):
                fail(f"skill {folder} reference inventory differs from the manifest")
            for relative, digest in references.items():
                if not relative.startswith("references/"):
                    fail(f"skill {folder} reference path is non-canonical")
                regular_source(
                    os.path.join(root, "orchestrator", "skills", folder),
                    relative, digest, f"skill {folder} reference",
                )

    authored_root = os.path.join(root, "orchestrator", "skills")
    authored_folders = {
        name for name in os.listdir(authored_root)
        if name not in {"_index", "checks"} and
        os.path.isfile(os.path.join(authored_root, name, "SKILL.md"))
    }
    if seen_folders != authored_folders | {"implement-figma"}:
        fail("skill inventory is incomplete or contains a retired entry")

    expected_mappings = contract_mappings(root)
    actual_mappings = {}
    for entry in manifest["files"]:
        exact(entry, ["sourcePath", "installPath", "sourceSha256"], "installed-file entry")
        source = relative_path(entry["sourcePath"], "installed-file source")
        install = relative_path(entry["installPath"], "installed-file destination")
        if not install.startswith(".claude/") or source in actual_mappings or \
                install in actual_mappings.values():
            fail("installed-file mappings are unsafe or duplicated")
        actual_mappings[source] = install
        regular_source(root, source, entry["sourceSha256"], "installed-file source")
    if actual_mappings != expected_mappings:
        fail("installed-file source/destination mappings are incomplete or non-canonical")


def main():
    if len(sys.argv) not in {2, 3}:
        print("usage: validate-install-manifest.py <repo-root> [manifest]", file=sys.stderr)
        return 2
    root = os.path.realpath(sys.argv[1])
    manifest = (sys.argv[2] if len(sys.argv) == 3 else
                os.path.join(root, "orchestrator", "skills", "_index", "install-manifest.json"))
    try:
        validate(root, manifest)
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"INSTALL_MANIFEST_INVALID: {error}", file=sys.stderr)
        return 1
    print("ok - install manifest paths, hashes, inventories, and destinations are canonical")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
