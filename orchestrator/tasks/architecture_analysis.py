#!/usr/bin/env python3
"""Deterministic Architecture Map v2 analysis and publication primitives."""

from __future__ import annotations

import datetime
import contextlib
import hashlib
import json
import os
import re
import secrets
import stat
from collections import deque
from pathlib import Path
from typing import Any, Callable, Iterable

try:
    import fcntl
except ImportError:  # Windows
    fcntl = None  # type: ignore[assignment]

try:
    import msvcrt
except ImportError:  # POSIX
    msvcrt = None  # type: ignore[assignment]


SCHEMA_VERSION = 2
GENERATOR_VERSION = "architecture"
PROFILE_VERSION = 1
MAP_MAX_BYTES = 5 * 1024 * 1024
INPUT_MAX_FILES = 20_000
INPUT_MAX_BYTES = 512 * 1024 * 1024
NODE_MAX = 10_000
EDGE_MAX = 50_000
FINDING_MAX = 5_000
HISTORY_MAX = 100
TASK_HISTORY_MAX = 50
HASH_RE = re.compile(r"^sha256:[0-9a-f]{64}$")
ID_RE = re.compile(r"^[a-z][a-z0-9-]{0,31}:[A-Za-z0-9._~/-]{1,147}$")
RULE_ID_RE = re.compile(r"^[a-z][a-z0-9.-]{0,95}$")
GENERATOR_ID_RE = re.compile(r"^[a-z][a-z0-9.-]{0,63}$")
TRIGGER_ID_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,159}$")
TASK_STEM_RE = re.compile(r"^TASK_[1-9][0-9]*_[A-Za-z0-9_]+$")
PLATFORMS = {"shared", "android", "ios", "tooling", "unknown"}
LAYERS = {"ui", "domain", "data", "infrastructure", "build", "unknown"}
NODE_KINDS = {
    "module", "feature", "screen", "component", "repository", "data-source",
    "api", "database-entity",
}
EDGE_KINDS = {
    "depends-on", "owns", "implements", "consumes", "renders", "persists",
    "navigates-to",
}
FINDING_TYPES = {
    "dependency-cycle", "forbidden-dependency", "orphan-module",
    "unused-repository", "screen-without-owner",
}
SEVERITIES = {"error", "warning", "info"}
CONFIDENCES = {"exact", "derived", "heuristic"}
FINDING_REASON_CODES = {
    "forbidden-dependency",
    "module-dependency-cycle",
    "module-has-no-incoming-relation",
    "repository-has-no-proven-consumer",
    "screen-has-no-owner",
}
LIMITATIONS = {
    "analysis-coverage-partial",
    "api-class-not-resolved",
    "database-schema-not-resolved",
    "dependency-target-not-in-settings",
    "unsupported-gradle-dependency-expression",
}

SETTINGS = "settings.gradle.kts"
CONFIG = "orchestrator/project-config.md"
RULES = "orchestrator/architecture-rules.json"
OUT = "orchestrator/.arch-map.json"
CACHE_ROOT = "orchestrator/.cache/architecture"
HISTORY_DIR = CACHE_ROOT + "/history"
HISTORY_INDEX = CACHE_ROOT + "/history-index.json"
LATEST_DIFF = CACHE_ROOT + "/latest-diff.json"
LATEST_TASK_DIFF = CACHE_ROOT + "/latest-task-diff.json"
INPUT_RECEIPT = CACHE_ROOT + "/input-receipt.json"

SCREEN_API = ":ui-screen-features:screen-api"
FEATURE_API = ":data-features:feature-api"
BACKEND_MOD = ":data-services:backend"
DATABASE_MOD = ":data-services:database"

MODULE_ID_BY_GRADLE: dict[str, str] = {}


class ArchitectureError(RuntimeError):
    """A typed, public-safe generator failure."""

    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


def utc_now() -> str:
    return datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def exact_utc_second(value: Any) -> bool:
    if not isinstance(value, str) or not re.fullmatch(
        r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z",
        value,
    ):
        return False
    try:
        return (
            datetime.datetime.strptime(value, "%Y-%m-%dT%H:%M:%SZ")
            .strftime("%Y-%m-%dT%H:%M:%SZ")
            == value
        )
    except ValueError:
        return False


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")


def sha256(value: bytes | str) -> str:
    raw = value if isinstance(value, bytes) else value.encode("utf-8")
    return "sha256:" + hashlib.sha256(raw).hexdigest()


def structural_payload(value: dict[str, Any]) -> dict[str, Any]:
    return {
        "analysisCapabilities": value["analysis"]["capabilities"],
        "edges": value["edges"],
        "findings": value["findings"],
        "nodes": value["nodes"],
    }


def structural_hash(value: dict[str, Any]) -> str:
    return sha256(canonical_bytes(structural_payload(value)))


def normalize_rel(value: str) -> str:
    raw = str(value).replace("\\", "/")
    if (
        not raw
        or raw.startswith("/")
        or re.match(r"^[A-Za-z]:/", raw)
        or "\x00" in raw
        or any(ord(ch) < 32 or ord(ch) == 127 for ch in raw)
    ):
        raise ArchitectureError("unsafe-path", "architecture input path is unsafe")
    parts = raw.split("/")
    if any(part in ("", ".", "..") for part in parts):
        raise ArchitectureError("unsafe-path", "architecture input path is not normalized")
    return "/".join(parts)


def contained_path(root: Path, relative: str) -> Path:
    normalized = normalize_rel(relative)
    candidate = root.joinpath(*normalized.split("/"))
    try:
        candidate.relative_to(root)
    except ValueError as exc:
        raise ArchitectureError("unsafe-path", "architecture path escapes project root") from exc
    return candidate


def ensure_safe_parent_chain(root: Path, relative: str) -> None:
    """Reject symlink/non-directory ancestors before opening a trusted input."""
    normalized = normalize_rel(relative)
    current = root
    for part in normalized.split("/")[:-1]:
        current = current / part
        try:
            info = os.lstat(current)
        except OSError as exc:
            raise ArchitectureError(
                "input-unreadable",
                f"architecture input parent is unavailable: {normalized}",
            ) from exc
        if stat.S_ISLNK(info.st_mode) or not stat.S_ISDIR(info.st_mode):
            raise ArchitectureError(
                "input-root-unsafe",
                f"architecture input parent is unsafe: {normalized}",
            )


def safe_read(root: Path, relative: str, max_bytes: int = 16 * 1024 * 1024) -> bytes:
    normalized = normalize_rel(relative)
    ensure_safe_parent_chain(root, normalized)
    file = contained_path(root, normalized)
    try:
        before = os.lstat(file)
    except OSError as exc:
        raise ArchitectureError("input-unreadable", f"required input is unavailable: {normalized}") from exc
    if (
        not stat.S_ISREG(before.st_mode)
        or stat.S_ISLNK(before.st_mode)
        or before.st_nlink != 1
        or before.st_size > max_bytes
    ):
        raise ArchitectureError("input-unsafe", f"input is not a bounded single-link regular file: {normalized}")
    flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)
    fd = None
    try:
        fd = os.open(file, flags)
        opened = os.fstat(fd)
        if not stat.S_ISREG(opened.st_mode) or opened.st_nlink != 1 or opened.st_size > max_bytes:
            raise ArchitectureError("input-unsafe", f"input changed before read: {normalized}")
        chunks: list[bytes] = []
        total = 0
        while True:
            chunk = os.read(fd, min(64 * 1024, max_bytes + 1 - total))
            if not chunk:
                break
            total += len(chunk)
            if total > max_bytes:
                raise ArchitectureError("input-too-large", f"input exceeds its byte limit: {normalized}")
            chunks.append(chunk)
        after = os.fstat(fd)
    except OSError as exc:
        raise ArchitectureError("input-unreadable", f"input could not be read safely: {normalized}") from exc
    finally:
        if fd is not None:
            os.close(fd)
    live = os.lstat(file)
    identity = lambda row: (
        row.st_dev, row.st_ino, row.st_mode, row.st_nlink, row.st_size,
        row.st_mtime_ns, row.st_ctime_ns,
    )
    if identity(before) != identity(opened) or identity(opened) != identity(after) or identity(after) != identity(live):
        raise ArchitectureError("input-raced", f"input changed during read: {normalized}")
    return b"".join(chunks)


def safe_text(root: Path, relative: str, max_bytes: int = 16 * 1024 * 1024) -> str:
    try:
        return safe_read(root, relative, max_bytes).decode("utf-8")
    except UnicodeDecodeError as exc:
        raise ArchitectureError("input-encoding", f"input is not UTF-8: {relative}") from exc


def optional_text(root: Path, relative: str, max_bytes: int = 16 * 1024 * 1024) -> str | None:
    file = contained_path(root, relative)
    try:
        os.lstat(file)
    except FileNotFoundError:
        return None
    return safe_text(root, relative, max_bytes)


def stable_id(kind: str, raw: str) -> str:
    value = re.sub(r"[^A-Za-z0-9._~/-]+", "-", str(raw).strip().replace(":", "/"))
    value = re.sub(r"-+", "-", value).strip("-/")
    if not value:
        value = "h-" + hashlib.sha256(str(raw).encode("utf-8")).hexdigest()[:24]
    out = kind + ":" + value
    if len(out.encode("ascii", "ignore")) > 180 or not ID_RE.fullmatch(out):
        out = kind + ":h-" + hashlib.sha256(str(raw).encode("utf-8")).hexdigest()[:32]
    if not ID_RE.fullmatch(out) or len(out.encode("ascii")) > 180:
        raise ArchitectureError("id-invalid", "an analyzer generated an invalid stable id")
    return out


def edge_id(kind: str, source: str, target: str, discriminator: str = "") -> str:
    digest = hashlib.sha256(
        (kind + "\0" + source + "\0" + target + "\0" + discriminator).encode("utf-8")
    ).hexdigest()[:32]
    return stable_id("edge", kind + "/" + digest)


def module_dir(module_path: str) -> str:
    return module_path.lstrip(":").replace(":", "/")


def pascal(value: str) -> str:
    return "".join(part[:1].upper() + part[1:] for part in re.split(r"[-_]", value) if part)


def line_number(text: str, offset: int) -> int:
    return text.count("\n", 0, max(0, offset)) + 1


def mask_comments_and_strings(text: str, *, remove_strings: bool) -> str:
    """Return an offset-preserving Kotlin mask with nested comments handled."""
    output: list[str] = []
    index = 0
    state = "code"
    block_depth = 0

    def removed(value: str) -> str:
        return "".join("\n" if char == "\n" else " " for char in value)

    while index < len(text):
        if state == "code":
            if text.startswith("//", index):
                output.append("  "); index += 2; state = "line-comment"; continue
            if text.startswith("/*", index):
                output.append("  "); index += 2; state = "block-comment"; block_depth = 1; continue
            if text.startswith('"""', index):
                output.append("   " if remove_strings else '"""')
                index += 3; state = "raw-string-remove" if remove_strings else "raw-string-keep"; continue
            char = text[index]
            if char == '"':
                output.append(" " if remove_strings else char)
                index += 1; state = "string-remove" if remove_strings else "string-keep"; continue
            if char == "'":
                output.append(" " if remove_strings else char)
                index += 1; state = "char-remove" if remove_strings else "char-keep"; continue
            output.append(char); index += 1; continue
        if state == "line-comment":
            char = text[index]
            output.append("\n" if char == "\n" else " ")
            index += 1
            if char == "\n":
                state = "code"
            continue
        if state == "block-comment":
            if text.startswith("/*", index):
                output.append("  "); index += 2; block_depth += 1; continue
            if text.startswith("*/", index):
                output.append("  "); index += 2; block_depth -= 1
                if block_depth == 0:
                    state = "code"
                continue
            output.append("\n" if text[index] == "\n" else " "); index += 1; continue
        if state in {"raw-string-remove", "raw-string-keep"}:
            keep = state.endswith("keep")
            if text.startswith('"""', index):
                output.append('"""' if keep else "   "); index += 3; state = "code"; continue
            output.append(text[index] if keep else removed(text[index])); index += 1; continue
        keep = state.endswith("keep")
        quote = "'" if state.startswith("char-") else '"'
        char = text[index]
        if char == "\\" and index + 1 < len(text):
            pair = text[index:index + 2]
            output.append(pair if keep else removed(pair)); index += 2; continue
        output.append(char if keep else removed(char)); index += 1
        if char == quote:
            state = "code"
    if state not in {"code", "line-comment"}:
        raise ArchitectureError(
            "source-lexical-invalid",
            "architecture source contains an unterminated comment or string",
        )
    return "".join(output)


def strip_kotlin_comments_and_strings(text: str) -> str:
    return mask_comments_and_strings(text, remove_strings=True)


def strip_gradle_comments(text: str) -> str:
    return mask_comments_and_strings(text, remove_strings=False)


def parse_settings(text: str) -> list[str]:
    clean = strip_gradle_comments(text)
    modules: list[str] = []
    calls = list(re.finditer(r"(?m)^[ \t]*include[ \t]*\(([\s\S]*?)\)", clean))
    include_lines = len(re.findall(r"(?m)^[ \t]*include\b", clean))
    if include_lines != len(calls):
        raise ArchitectureError(
            "settings-expression-unsupported",
            "settings.gradle.kts contains an unsupported include expression",
        )
    token = r"""["'](:[A-Za-z0-9_.:-]+)["']"""
    supported_body = re.compile(
        r"^\s*" + token + r"(?:\s*,\s*" + token + r")*\s*,?\s*$"
    )
    for call in calls:
        body = call.group(1)
        if not supported_body.fullmatch(body):
            raise ArchitectureError(
                "settings-expression-unsupported",
                "settings.gradle.kts contains an unsupported include expression",
            )
        quoted = re.findall(token, body)
        modules.extend(quoted)
    return list(dict.fromkeys(modules))


def read_project_config(root: Path) -> dict[str, Any]:
    text = optional_text(root, CONFIG, 1024 * 1024)
    result: dict[str, Any] = {
        "apiClassName": None,
        "featuresWithRootComponentSuffix": set(),
    }
    if text is None:
        return result
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        return result
    frontmatter: list[str] = []
    for row in lines[1:]:
        if row.strip() == "---":
            break
        frontmatter.append(row)
    for index, row in enumerate(frontmatter):
        match = re.match(r"^apiClassName:\s*(.*?)\s*$", row)
        if match:
            result["apiClassName"] = match.group(1).split(" #", 1)[0].strip().strip("\"'")
        suffix = re.match(r"^featuresWithRootComponentSuffix:\s*(.*?)\s*$", row)
        if suffix:
            raw = suffix.group(1).strip()
            if raw.startswith("[") and raw.endswith("]"):
                result["featuresWithRootComponentSuffix"] = {
                    item.strip().strip("\"'") for item in raw[1:-1].split(",") if item.strip()
                }
            else:
                for following in frontmatter[index + 1:]:
                    item = re.match(r"^\s+-\s*(.*?)\s*$", following)
                    if not item:
                        if following.strip() and not following.startswith(" "):
                            break
                        continue
                    result["featuresWithRootComponentSuffix"].add(item.group(1).strip().strip("\"'"))
    return result


def walk_regular_files(
    root: Path,
    relative_root: str,
    suffixes: tuple[str, ...],
    *,
    exclude_tests: bool = False,
) -> list[str]:
    normalized = normalize_rel(relative_root)
    directory = contained_path(root, normalized)
    try:
        initial = os.lstat(directory)
    except FileNotFoundError:
        return []
    if stat.S_ISLNK(initial.st_mode) or not stat.S_ISDIR(initial.st_mode):
        raise ArchitectureError("input-root-unsafe", f"analysis root is unsafe: {normalized}")
    rows: list[str] = []
    for current, dirs, files in os.walk(directory, topdown=True, followlinks=False):
        rel_current = Path(current).relative_to(root).as_posix()
        kept_dirs = []
        for name in sorted(dirs):
            candidate = Path(current) / name
            info = os.lstat(candidate)
            if stat.S_ISLNK(info.st_mode):
                raise ArchitectureError("input-symlink", f"symlinked analysis directory is forbidden: {candidate.relative_to(root).as_posix()}")
            if name in {"build", ".gradle", ".git", ".idea", "node_modules"}:
                continue
            if exclude_tests and "test" in name.lower() and Path(current).name == "src":
                continue
            kept_dirs.append(name)
        dirs[:] = kept_dirs
        for name in sorted(files):
            if not name.endswith(suffixes):
                continue
            relative = (Path(rel_current) / name).as_posix()
            info = os.lstat(root / relative)
            if stat.S_ISLNK(info.st_mode) or not stat.S_ISREG(info.st_mode) or info.st_nlink != 1:
                raise ArchitectureError("input-file-unsafe", f"analysis file is unsafe: {relative}")
            if exclude_tests:
                parts = [part.lower() for part in Path(relative).parts]
                if any("test" in part for part in parts if part.endswith("test")):
                    continue
            rows.append(relative)
    return sorted(set(rows))


def source_sets(paths: Iterable[str]) -> list[str]:
    found = set()
    for relative in paths:
        parts = Path(relative).parts
        if "src" in parts:
            index = parts.index("src")
            if index + 1 < len(parts):
                found.add(parts[index + 1])
    return sorted(found)


def platform_for(module: str, sets: Iterable[str]) -> str:
    lower = module.lower()
    lowered_sets = [item.lower() for item in sets]
    if "android" in lower and "shared" not in lower:
        return "android"
    if "ios" in lower and "shared" not in lower:
        return "ios"
    if lower.startswith(":tool") or "build-logic" in lower or "tooling" in lower:
        return "tooling"
    if any("commonmain" == item for item in lowered_sets):
        return "shared"
    if lowered_sets and all("android" in item for item in lowered_sets):
        return "android"
    if lowered_sets and all("ios" in item for item in lowered_sets):
        return "ios"
    return "unknown"


def layer_for(module: str) -> str:
    lower = module.lower()
    if lower.startswith(":ui-") or ":ui:" in lower or "screen" in lower or "dialog" in lower:
        return "ui"
    if lower.startswith(":domain") or ":domain:" in lower:
        return "domain"
    if lower.startswith(":data-features") or "database" in lower:
        return "data"
    if lower.startswith(":data-services") or any(token in lower for token in ("network", "backend", "infrastructure")):
        return "infrastructure"
    if any(token in lower for token in ("build-logic", "toolkit", "gradle-plugin")):
        return "build"
    return "unknown"


def default_rules() -> dict[str, Any]:
    return {
        "schemaVersion": 1,
        "dependencyRules": [
            {
                "id": "ui-must-not-depend-on-infrastructure",
                "fromLayer": "ui",
                "disallowToLayer": "infrastructure",
                "fromPlatform": None,
                "toPlatform": None,
                "fromModule": None,
                "toModule": None,
                "severity": "error",
            },
            {
                "id": "domain-must-not-depend-on-ui",
                "fromLayer": "domain",
                "disallowToLayer": "ui",
                "fromPlatform": None,
                "toPlatform": None,
                "fromModule": None,
                "toModule": None,
                "severity": "error",
            },
        ],
        "rootModules": ["composeApp", "androidApp", "iosApp", "server", "tooling/**"],
        "standaloneModules": [],
    }


def validate_glob(value: Any) -> bool:
    return (
        isinstance(value, str)
        and 1 <= len(value) <= 180
        and not value.startswith("/")
        and ".." not in value.split("/")
        and re.fullmatch(r"[A-Za-z0-9._~/*-]+", value) is not None
    )


def validate_rules(value: Any) -> dict[str, Any]:
    fields = {"schemaVersion", "dependencyRules", "rootModules", "standaloneModules"}
    if not isinstance(value, dict) or set(value) != fields or value["schemaVersion"] != 1:
        raise ArchitectureError("rules-invalid", "architecture rules do not match the exact v1 contract")
    rules = value["dependencyRules"]
    roots = value["rootModules"]
    standalone = value["standaloneModules"]
    if (
        not isinstance(rules, list)
        or len(rules) > 200
        or not isinstance(roots, list)
        or len(roots) > 200
        or not isinstance(standalone, list)
        or len(standalone) > 200
        or len(set(roots)) != len(roots)
        or len(set(standalone)) != len(standalone)
        or any(not validate_glob(item) for item in roots + standalone)
    ):
        raise ArchitectureError("rules-invalid", "architecture rule patterns are invalid or exceed their bounds")
    ids = set()
    expected = {
        "id", "fromLayer", "disallowToLayer", "fromPlatform", "toPlatform",
        "fromModule", "toModule", "severity",
    }
    for rule in rules:
        if not isinstance(rule, dict) or set(rule) != expected:
            raise ArchitectureError("rules-invalid", "dependency rule fields do not match the exact contract")
        if (
            not RULE_ID_RE.fullmatch(str(rule["id"]))
            or rule["id"] in ids
            or rule["severity"] not in SEVERITIES
            or rule["fromLayer"] not in LAYERS | {None}
            or rule["disallowToLayer"] not in LAYERS | {None}
            or rule["fromPlatform"] not in PLATFORMS | {None}
            or rule["toPlatform"] not in PLATFORMS | {None}
            or rule["fromModule"] is not None and not validate_glob(rule["fromModule"])
            or rule["toModule"] is not None and not validate_glob(rule["toModule"])
        ):
            raise ArchitectureError("rules-invalid", "dependency rule values are invalid")
        if not any(rule[key] is not None for key in ("fromLayer", "disallowToLayer", "fromPlatform", "toPlatform", "fromModule", "toModule")):
            raise ArchitectureError("rules-invalid", "a dependency rule must constrain at least one predicate")
        ids.add(rule["id"])
    return value


def load_rules(root: Path) -> tuple[dict[str, Any], str, str]:
    text = optional_text(root, RULES, 1024 * 1024)
    if text is None:
        value = default_rules()
        return value, sha256(canonical_bytes(value)), "builtin:architecture-rules"
    try:
        value = json.loads(text)
    except json.JSONDecodeError as exc:
        raise ArchitectureError("rules-invalid", "architecture-rules.json is not valid JSON") from exc
    value = validate_rules(value)
    return value, sha256(canonical_bytes(value)), RULES


def glob_match(pattern: str, module_path: str) -> bool:
    parts = pattern.split("/")
    target = module_path.lstrip(":").replace(":", "/").split("/")

    def match(pi: int, ti: int) -> bool:
        while pi < len(parts):
            token = parts[pi]
            if token == "**":
                if pi + 1 == len(parts):
                    return True
                return any(match(pi + 1, skip) for skip in range(ti, len(target) + 1))
            if ti >= len(target):
                return False
            if token != "*" and token != target[ti]:
                return False
            pi += 1
            ti += 1
        return ti == len(target)

    return match(0, 0)


def collect_input_receipt(
    root: Path,
    modules: list[str],
    module_files: dict[str, list[str]],
    rules_hash: str,
) -> dict[str, Any]:
    files = {SETTINGS}
    for optional in (
        CONFIG,
        RULES,
        "orchestrator/tasks/regen-arch.py",
        "orchestrator/tasks/architecture_analysis.py",
        "orchestrator/contracts/architecture-map.schema.json",
        "orchestrator/contracts/architecture-rules.schema.json",
    ):
        if contained_path(root, optional).exists():
            files.add(optional)
    for rows in module_files.values():
        files.update(rows)
    if len(files) > INPUT_MAX_FILES:
        raise ArchitectureError("input-file-limit", "architecture input file count exceeds 20000")
    receipt_rows = []
    total = 0
    normalized_seen = set()
    for relative in sorted(files):
        normalized = normalize_rel(relative)
        folded = normalized.casefold()
        if folded in normalized_seen:
            raise ArchitectureError("input-path-collision", "architecture inputs contain a normalized path collision")
        normalized_seen.add(folded)
        raw = safe_read(root, normalized, INPUT_MAX_BYTES)
        total += len(raw)
        if total > INPUT_MAX_BYTES:
            raise ArchitectureError("input-byte-limit", "architecture inputs exceed 512 MiB")
        info = os.lstat(contained_path(root, normalized))
        receipt_rows.append({
            "path": normalized,
            "contentHash": sha256(raw),
            "executable": bool(info.st_mode & 0o111),
        })
    allowlist_roots = sorted(
        {module_dir(module) for module in modules}
        | {
            SETTINGS,
            CONFIG,
            RULES,
            "orchestrator/contracts",
            "orchestrator/tasks",
        }
    )
    revision_basis = {
        "profileVersion": PROFILE_VERSION,
        "allowlistRoots": allowlist_roots,
        "files": receipt_rows,
        "generatorVersion": GENERATOR_VERSION,
        "rulesHash": rules_hash,
    }
    return {
        "schemaVersion": 1,
        "profileVersion": PROFILE_VERSION,
        "generatedAtRevision": sha256(canonical_bytes(revision_basis)),
        "generatorVersion": GENERATOR_VERSION,
        "rulesSchemaVersion": 1,
        "rulesHash": rules_hash,
        "allowlistRoots": allowlist_roots,
        "files": receipt_rows,
        "fileCount": len(receipt_rows),
        "totalBytes": total,
    }


def evidence(source_path: str, line: int | None, analyzer: str, confidence: str) -> dict[str, Any]:
    return {
        "sourcePath": normalize_rel(source_path),
        "line": line,
        "analyzer": analyzer,
        "confidence": confidence,
    }


def make_edge(
    source: str,
    target: str,
    kind: str,
    source_path: str,
    line: int | None,
    analyzer: str,
    confidence: str,
    discriminator: str = "",
) -> dict[str, Any]:
    return {
        "id": edge_id(kind, source, target, discriminator or source_path + ":" + str(line)),
        "from": source,
        "to": target,
        "kind": kind,
        "evidence": evidence(source_path, line, analyzer, confidence),
    }


def class_name(value: Any) -> bool:
    return isinstance(value, str) and re.fullmatch(r"[A-Z][A-Za-z0-9_]{0,199}", value) is not None


def sorted_identifiers(value: Any, pattern: str, maximum: int = 100) -> bool:
    return (
        isinstance(value, list)
        and len(value) <= maximum
        and value == sorted(set(value))
        and all(isinstance(item, str) and re.fullmatch(pattern, item) for item in value)
    )


def valid_metadata(kind: str, values: Any) -> bool:
    if not isinstance(values, dict):
        return False
    if kind == "module":
        return (
            set(values) == {"gradlePath", "sourceSets"}
            and isinstance(values["gradlePath"], str)
            and len(values["gradlePath"]) <= 180
            and re.fullmatch(r":[A-Za-z0-9_.:-]+", values["gradlePath"]) is not None
            and sorted_identifiers(values["sourceSets"], r"[A-Za-z][A-Za-z0-9]{0,79}")
        )
    if kind == "feature":
        return (
            set(values) == {"ownershipId", "interfaceClass"}
            and isinstance(values["ownershipId"], str)
            and 1 <= len(values["ownershipId"]) <= 180
            and re.fullmatch(r"[A-Za-z0-9._~-]+", values["ownershipId"]) is not None
            and (values["interfaceClass"] is None or class_name(values["interfaceClass"]))
        )
    if kind == "screen":
        return (
            set(values) == {"routes", "rootSuffix"}
            and sorted_identifiers(values["routes"], r"[A-Z][A-Za-z0-9_]{0,199}")
            and isinstance(values["rootSuffix"], bool)
        )
    if kind == "component":
        return set(values) == {"className"} and class_name(values["className"])
    if kind == "repository":
        return (
            set(values) == {"className", "role"}
            and class_name(values["className"])
            and values["role"] in {"interface", "implementation"}
        )
    if kind == "data-source":
        return (
            set(values) == {"className", "sourceType"}
            and class_name(values["className"])
            and values["sourceType"] in {
                "RemoteDataSource", "LocalDataSource", "DataSource", "Store", "Dao"
            }
        )
    if kind == "api":
        return (
            set(values) == {"className", "methods"}
            and class_name(values["className"])
            and sorted_identifiers(values["methods"], r"[a-z][A-Za-z0-9_]{0,199}")
        )
    if kind == "database-entity":
        return (
            set(values) == {"entityClass", "database", "version"}
            and class_name(values["entityClass"])
            and class_name(values["database"])
            and isinstance(values["version"], int)
            and not isinstance(values["version"], bool)
            and 0 <= values["version"] <= 0x7FFFFFFF
        )
    return False


def metadata_for(kind: str, values: dict[str, Any]) -> dict[str, Any]:
    if not valid_metadata(kind, values):
        raise ArchitectureError("metadata-invalid", f"metadata is invalid for {kind}")
    return values


class Analyzer:
    def __init__(self, root: Path):
        self.root = root
        self.nodes: dict[str, dict[str, Any]] = {}
        self.edges: dict[str, dict[str, Any]] = {}
        self.findings: list[dict[str, Any]] = []
        self.limitations: set[str] = set()
        self.capabilities: set[str] = {
            "gradle-modules",
            "gradle-dependencies",
            "screen-ownership",
            "repositories",
            "reverse-usage",
            "architecture-findings",
        }
        self.coverage = {
            "schemaVersion": 1,
            "gradleFiles": 0,
            "dependencyExpressions": 0,
            "unsupportedDependencyExpressions": 0,
            "kotlinFiles": 0,
            "parserErrors": 0,
            "excludedModules": 0,
            "supportedPersistencePatterns": ["room-database"],
            "supportedApiPatterns": ["kotlin-suspend-class"],
        }
        self.modules: list[str] = []
        self.module_files: dict[str, list[str]] = {}
        self.module_kotlin: dict[str, list[tuple[str, str]]] = {}
        self.rules: dict[str, Any] = {}
        self.rules_hash = ""
        self.rules_source = ""
        self.config: dict[str, Any] = {}
        self.revision = ""

    def add_node(
        self,
        kind: str,
        raw_id: str,
        name: str,
        path: str | None,
        platform: str,
        layer: str,
        metadata: dict[str, Any],
    ) -> str:
        node_id = stable_id(kind, raw_id)
        node = {
            "id": node_id,
            "kind": kind,
            "name": str(name)[:200],
            "path": normalize_rel(path) if path else None,
            "platform": platform if platform in PLATFORMS else "unknown",
            "layer": layer if layer in LAYERS else "unknown",
            "metadata": metadata_for(kind, metadata),
        }
        existing = self.nodes.get(node_id)
        if existing is not None and existing != node:
            raise ArchitectureError("node-id-collision", f"conflicting architecture node id: {node_id}")
        self.nodes[node_id] = node
        return node_id

    def add_edge(self, row: dict[str, Any]) -> None:
        existing = self.edges.get(row["id"])
        if existing is not None and existing != row:
            raise ArchitectureError("edge-id-collision", f"conflicting architecture edge id: {row['id']}")
        self.edges[row["id"]] = row

    def prepare_inputs(self) -> dict[str, Any]:
        settings_text = safe_text(self.root, SETTINGS, 4 * 1024 * 1024)
        self.modules = parse_settings(settings_text)
        global MODULE_ID_BY_GRADLE
        MODULE_ID_BY_GRADLE = {
            module: stable_id("module", module_dir(module)) for module in self.modules
        }
        self.config = read_project_config(self.root)
        self.rules, self.rules_hash, self.rules_source = load_rules(self.root)
        for module in self.modules:
            directory = module_dir(module)
            files = walk_regular_files(
                self.root,
                directory,
                (".kt", ".kts"),
                exclude_tests=True,
            )
            build_file = directory + "/build.gradle.kts"
            if contained_path(self.root, build_file).exists() and build_file not in files:
                files.append(build_file)
            self.module_files[module] = sorted(set(files))
            kotlin_rows = []
            for relative in self.module_files[module]:
                if relative.endswith(".kt"):
                    kotlin_rows.append((relative, safe_text(self.root, relative, 8 * 1024 * 1024)))
            self.module_kotlin[module] = kotlin_rows
        self.coverage["kotlinFiles"] = sum(len(rows) for rows in self.module_kotlin.values())
        receipt = collect_input_receipt(
            self.root, self.modules, self.module_files, self.rules_hash
        )
        self.revision = receipt["generatedAtRevision"]
        return receipt

    def analyze_modules(self) -> None:
        settings_text = safe_text(self.root, SETTINGS, 4 * 1024 * 1024)
        for module in self.modules:
            files = self.module_files[module]
            sets = source_sets(files)
            build_file = module_dir(module) + "/build.gradle.kts"
            node_path = build_file if build_file in files else module_dir(module)
            self.add_node(
                "module",
                module_dir(module),
                module,
                node_path,
                platform_for(module, sets),
                layer_for(module),
                {"gradlePath": module, "sourceSets": sets},
            )
        include_lines = {}
        for match in re.finditer(r"""["'](:[A-Za-z0-9_.:-]+)["']""", settings_text):
            include_lines.setdefault(match.group(1), line_number(settings_text, match.start()))
        dependency_re = re.compile(
            r"(?P<config>[A-Za-z][A-Za-z0-9_]*)\s*\(\s*(?P<project>project)\s*\(\s*(?:path\s*=\s*)?[\"'](?P<path>:[A-Za-z0-9_.:-]+)[\"']\s*\)\s*\)"
        )
        project_call_re = re.compile(r"\bproject\s*\(")
        for module in self.modules:
            build_file = module_dir(module) + "/build.gradle.kts"
            if build_file not in self.module_files[module]:
                continue
            self.coverage["gradleFiles"] += 1
            text = safe_text(self.root, build_file, 4 * 1024 * 1024)
            clean = strip_gradle_comments(text)
            project_mask = strip_kotlin_comments_and_strings(text)
            consumed_offsets = set()
            for match in dependency_re.finditer(clean):
                self.coverage["dependencyExpressions"] += 1
                consumed_offsets.add(match.start("project"))
                configuration = match.group("config")
                if "test" in configuration.lower() or "debug" in configuration.lower():
                    continue
                target = match.group("path")
                if target not in MODULE_ID_BY_GRADLE:
                    self.coverage["unsupportedDependencyExpressions"] += 1
                    self.limitations.add("dependency-target-not-in-settings")
                    continue
                row = make_edge(
                    MODULE_ID_BY_GRADLE[module],
                    MODULE_ID_BY_GRADLE[target],
                    "depends-on",
                    build_file,
                    line_number(clean, match.start()),
                    "gradle-project-v1",
                    "exact",
                    configuration,
                )
                self.add_edge(row)
            for match in project_call_re.finditer(project_mask):
                if match.start() not in consumed_offsets:
                    self.coverage["unsupportedDependencyExpressions"] += 1
            if self.coverage["unsupportedDependencyExpressions"]:
                self.limitations.add("unsupported-gradle-dependency-expression")

    def analyze_screens_and_features(self) -> None:
        route_re = re.compile(
            r"@Serializable\s+(?:public\s+)?(?:data\s+(?:object|class)|object)\s+([A-Z][A-Za-z0-9_]*)"
        )
        component_re = re.compile(r"\b(?:class|interface|object)\s+([A-Z][A-Za-z0-9_]*Component)\b")
        routes_by_feature: dict[str, list[str]] = {}
        route_evidence: dict[tuple[str, str], tuple[str, int]] = {}
        if SCREEN_API in self.module_kotlin:
            for relative, text in self.module_kotlin[SCREEN_API]:
                if not relative.endswith("Router.kt"):
                    continue
                feature_name = Path(relative).name[:-len("Router.kt")]
                clean = strip_kotlin_comments_and_strings(text)
                for match in route_re.finditer(clean):
                    routes_by_feature.setdefault(feature_name, []).append(match.group(1))
                    route_evidence[(feature_name, match.group(1))] = (
                        relative, line_number(clean, match.start())
                    )
        for module in self.modules:
            if not module.startswith(":ui-screen-features:") or module == SCREEN_API:
                continue
            ownership = module.split(":")[-1]
            feature_name = pascal(ownership)
            module_node = MODULE_ID_BY_GRADLE[module]
            component_hits: list[tuple[str, str, int]] = []
            for relative, text in self.module_kotlin[module]:
                clean = strip_kotlin_comments_and_strings(text)
                for match in component_re.finditer(clean):
                    component_hits.append(
                        (relative, match.group(1), line_number(clean, match.start()))
                    )
            source_path = (
                component_hits[0][0]
                if component_hits
                else (module_dir(module) + "/build.gradle.kts" if module_dir(module) + "/build.gradle.kts" in self.module_files[module] else SETTINGS)
            )
            feature_id = self.add_node(
                "feature",
                ownership,
                ownership,
                source_path,
                self.nodes[module_node]["platform"],
                "ui",
                {"ownershipId": ownership, "interfaceClass": None},
            )
            routes = sorted(set(routes_by_feature.get(feature_name, [])))
            screen_id = self.add_node(
                "screen",
                ownership,
                feature_name,
                source_path,
                self.nodes[module_node]["platform"],
                "ui",
                {
                    "routes": routes,
                    "rootSuffix": ownership in self.config["featuresWithRootComponentSuffix"],
                },
            )
            self.add_edge(make_edge(
                feature_id, screen_id, "owns", source_path, 1,
                "screen-convention-v1", "derived",
            ))
            self.add_edge(make_edge(
                screen_id, module_node, "renders", source_path, 1,
                "screen-convention-v1", "derived",
            ))
            for relative, class_name, line in component_hits:
                component_id = self.add_node(
                    "component",
                    relative + "~" + class_name,
                    class_name,
                    relative,
                    self.nodes[module_node]["platform"],
                    "ui",
                    {"className": class_name},
                )
                self.add_edge(make_edge(
                    screen_id, component_id, "renders", relative, line,
                    "kotlin-component-v1", "exact", class_name,
                ))
            for route in routes:
                relative, line = route_evidence[(feature_name, route)]
                self.add_edge(make_edge(
                    screen_id, screen_id, "navigates-to", relative, line,
                    "kotlin-route-v1", "derived", route,
                ))

    def analyze_data(self) -> tuple[bool, bool]:
        feature_re = re.compile(r"\binterface\s+([A-Z][A-Za-z0-9_]*Feature)\b")
        repository_re = re.compile(
            r"\b(?P<role>interface|class|object)\s+(?P<name>[A-Z][A-Za-z0-9_]*Repository)\b"
        )
        data_source_re = re.compile(
            r"\b(?:interface|class|object)\s+(?P<name>[A-Z][A-Za-z0-9_]*(?:RemoteDataSource|LocalDataSource|DataSource|Store|Dao))\b"
        )
        feature_by_stem: dict[str, str] = {}
        if FEATURE_API in self.module_kotlin:
            for relative, text in self.module_kotlin[FEATURE_API]:
                clean = strip_kotlin_comments_and_strings(text)
                for match in feature_re.finditer(clean):
                    class_name = match.group(1)
                    stem = class_name[:-len("Feature")]
                    ownership = re.sub(r"(?<!^)(?=[A-Z])", "-", stem).lower()
                    node_id = stable_id("feature", ownership)
                    existing = self.nodes.get(node_id)
                    if existing is not None and existing["metadata"]["interfaceClass"] is None:
                        # Same ownership already registered by the :ui-screen-features:
                        # pass — one product feature spans both surfaces. Keep the UI
                        # node; the domain pass contributes only the interface class.
                        existing["metadata"]["interfaceClass"] = class_name
                        feature_by_stem[stem.lower()] = node_id
                        continue
                    feature_by_stem[stem.lower()] = self.add_node(
                        "feature",
                        ownership,
                        ownership,
                        relative,
                        "shared",
                        "domain",
                        {"ownershipId": ownership, "interfaceClass": class_name},
                    )
        repository_nodes: list[tuple[str, str, str, str]] = []
        data_source_nodes: list[tuple[str, str, str, str]] = []
        all_kotlin: list[tuple[str, str, str]] = []
        for module in self.modules:
            for relative, text in self.module_kotlin[module]:
                all_kotlin.append((module, relative, text))
                clean = strip_kotlin_comments_and_strings(text)
                for match in repository_re.finditer(clean):
                    class_name = match.group("name")
                    node_id = self.add_node(
                        "repository",
                        relative + "~" + class_name,
                        class_name,
                        relative,
                        self.nodes[MODULE_ID_BY_GRADLE[module]]["platform"],
                        "data",
                        {
                            "className": class_name,
                            "role": "interface" if match.group("role") == "interface" else "implementation",
                        },
                    )
                    repository_nodes.append((node_id, class_name, relative, module))
                    stem = class_name[:-len("Repository")].lower()
                    if stem in feature_by_stem:
                        self.add_edge(make_edge(
                            feature_by_stem[stem], node_id, "implements", relative,
                            line_number(clean, match.start()), "kotlin-repository-v1",
                            "derived", class_name,
                        ))
                for match in data_source_re.finditer(clean):
                    class_name = match.group("name")
                    suffix = next(
                        token for token in ("RemoteDataSource", "LocalDataSource", "DataSource", "Store", "Dao")
                        if class_name.endswith(token)
                    )
                    node_id = self.add_node(
                        "data-source",
                        relative + "~" + class_name,
                        class_name,
                        relative,
                        self.nodes[MODULE_ID_BY_GRADLE[module]]["platform"],
                        "data",
                        {"className": class_name, "sourceType": suffix},
                    )
                    data_source_nodes.append((node_id, class_name, relative, module))
        seen_consumers: set[tuple[str, str]] = set()
        for node_id, class_name, declaration, _decl_module in repository_nodes + data_source_nodes:
            reference = re.compile(r"\b" + re.escape(class_name) + r"\b")
            for module, relative, text in all_kotlin:
                if relative == declaration:
                    continue
                # One consumes edge per (module, node): several files of the same
                # module may reference the class; first evidence wins.
                if (module, node_id) in seen_consumers:
                    continue
                clean = strip_kotlin_comments_and_strings(text)
                match = reference.search(clean)
                if not match:
                    continue
                seen_consumers.add((module, node_id))
                self.add_edge(make_edge(
                    MODULE_ID_BY_GRADLE[module], node_id, "consumes", relative,
                    line_number(clean, match.start()), "kotlin-reference-v1",
                    "exact", class_name,
                ))
        return bool(repository_nodes), bool(data_source_nodes)

    def analyze_api_and_database(self) -> tuple[bool, bool]:
        api_supported = BACKEND_MOD not in self.modules
        database_supported = DATABASE_MOD not in self.modules
        api_class_name = self.config.get("apiClassName")
        api_node: str | None = None
        api_path: str | None = None
        if BACKEND_MOD in self.module_kotlin and api_class_name:
            class_re = re.compile(r"\bclass\s+" + re.escape(api_class_name) + r"\b")
            method_re = re.compile(r"\bsuspend\s+fun\s+([a-z][A-Za-z0-9_]*)\b")
            for relative, text in self.module_kotlin[BACKEND_MOD]:
                clean = strip_kotlin_comments_and_strings(text)
                match = class_re.search(clean)
                if not match:
                    continue
                methods = sorted(set(method_re.findall(clean)))
                api_node = self.add_node(
                    "api",
                    api_class_name,
                    api_class_name,
                    relative,
                    self.nodes[MODULE_ID_BY_GRADLE[BACKEND_MOD]]["platform"],
                    "infrastructure",
                    {"className": api_class_name, "methods": methods},
                )
                api_path = relative
                api_supported = True
                self.capabilities.add("api-consumers")
                break
        if BACKEND_MOD in self.modules and not api_supported:
            self.limitations.add("api-class-not-resolved")
        if api_node and api_path:
            reference = re.compile(r"\b" + re.escape(api_class_name) + r"\b")
            for module in self.modules:
                for relative, text in self.module_kotlin[module]:
                    if relative == api_path:
                        continue
                    clean = strip_kotlin_comments_and_strings(text)
                    match = reference.search(clean)
                    if match:
                        self.add_edge(make_edge(
                            MODULE_ID_BY_GRADLE[module], api_node, "consumes", relative,
                            line_number(clean, match.start()), "kotlin-api-consumer-v1",
                            "exact", api_class_name,
                        ))
                        # One consumes edge per module; first evidence wins.
                        break

        database_re = re.compile(r"@Database\s*\((?P<body>[\s\S]*?)\)\s*(?:abstract\s+)?class\s+(?P<name>[A-Z][A-Za-z0-9_]*)")
        version_re = re.compile(r"\bversion\s*=\s*(\d+)")
        entities_re = re.compile(r"\bentities\s*=\s*\[([^\]]*)\]", re.DOTALL)
        entity_ref_re = re.compile(r"([A-Z][A-Za-z0-9_]*)::class")
        entity_nodes: list[tuple[str, str]] = []
        if DATABASE_MOD in self.module_kotlin:
            for relative, text in self.module_kotlin[DATABASE_MOD]:
                clean = strip_kotlin_comments_and_strings(text)
                match = database_re.search(clean)
                if not match:
                    continue
                body = match.group("body")
                version_match = version_re.search(body)
                entities_match = entities_re.search(body)
                if not version_match or not entities_match:
                    continue
                version = int(version_match.group(1))
                database_name = match.group("name")
                for entity_class in sorted(set(entity_ref_re.findall(entities_match.group(1)))):
                    node_id = self.add_node(
                        "database-entity",
                        entity_class,
                        entity_class,
                        relative,
                        self.nodes[MODULE_ID_BY_GRADLE[DATABASE_MOD]]["platform"],
                        "data",
                        {
                            "entityClass": entity_class,
                            "database": database_name,
                            "version": version,
                        },
                    )
                    entity_nodes.append((node_id, entity_class))
                database_supported = True
                self.capabilities.add("database-entities")
                break
        if DATABASE_MOD in self.modules and not database_supported:
            self.limitations.add("database-schema-not-resolved")
        repositories = [node for node in self.nodes.values() if node["kind"] == "repository"]
        for entity_id, entity_class in entity_nodes:
            reference = re.compile(r"\b" + re.escape(entity_class) + r"\b")
            stem = entity_class[:-len("Entity")] if entity_class.endswith("Entity") else entity_class
            ownership_key = re.sub(r"(?<!^)(?=[A-Z])", "-", stem).lower()
            feature_id = stable_id("feature", ownership_key)
            if feature_id in self.nodes:
                self.add_edge(make_edge(
                    feature_id, entity_id, "owns",
                    self.nodes[entity_id]["path"] or SETTINGS, 1,
                    "entity-ownership-v1", "derived",
                ))
            for repository in repositories:
                if not repository["path"]:
                    continue
                text = safe_text(self.root, repository["path"], 8 * 1024 * 1024)
                clean = strip_kotlin_comments_and_strings(text)
                match = reference.search(clean)
                if match:
                    self.add_edge(make_edge(
                        repository["id"], entity_id, "persists", repository["path"],
                        line_number(clean, match.start()), "room-reference-v1",
                        "exact", entity_class,
                    ))
        return api_supported, database_supported

    def add_finding(
        self,
        finding_type: str,
        severity: str,
        title: str,
        summary: str,
        affected: list[str],
        evidence_rows: list[dict[str, Any]],
        rule_id: str,
        previous_first_seen: dict[str, str],
    ) -> None:
        affected_sorted = sorted(set(affected))[:100]
        evidence_bounded = sorted(
            evidence_rows,
            key=lambda row: canonical_bytes(row),
        )[:20]
        identity = [
            {
                "sourcePath": row["sourcePath"],
                "line": row["line"],
                "edgeId": row["edgeId"],
                "nodeId": row["nodeId"],
                "reasonCode": row["reasonCode"],
            }
            for row in evidence_bounded
        ]
        fingerprint = sha256(canonical_bytes({
            "type": finding_type,
            "ruleId": rule_id,
            "affectedNodeIds": affected_sorted,
            "evidence": identity,
        }))
        finding_id = stable_id("finding", finding_type + "/" + fingerprint[-24:])
        self.findings.append({
            "id": finding_id,
            "type": finding_type,
            "severity": severity,
            "title": title[:200],
            "summary": summary[:1000],
            "affectedNodeIds": affected_sorted,
            "evidence": evidence_bounded,
            "ruleId": rule_id,
            "firstSeenRevision": previous_first_seen.get(fingerprint, self.revision),
            "fingerprint": fingerprint,
        })

    @staticmethod
    def finding_evidence(
        source_path: str,
        line: int | None,
        reason_code: str,
        *,
        edge_id_value: str | None = None,
        node_id: str | None = None,
    ) -> dict[str, Any]:
        return {
            "sourcePath": normalize_rel(source_path),
            "line": line,
            "edgeId": edge_id_value,
            "nodeId": node_id,
            "reasonCode": reason_code,
        }

    def dependency_sccs(self) -> list[list[str]]:
        graph = {node_id: [] for node_id, node in self.nodes.items() if node["kind"] == "module"}
        for row in self.edges.values():
            if row["kind"] == "depends-on" and row["from"] in graph and row["to"] in graph:
                graph[row["from"]].append(row["to"])
        index = 0
        stack: list[str] = []
        on_stack: set[str] = set()
        indexes: dict[str, int] = {}
        low: dict[str, int] = {}
        components: list[list[str]] = []

        def visit(node_id: str) -> None:
            nonlocal index
            indexes[node_id] = index
            low[node_id] = index
            index += 1
            stack.append(node_id)
            on_stack.add(node_id)
            for target in sorted(graph[node_id]):
                if target not in indexes:
                    visit(target)
                    low[node_id] = min(low[node_id], low[target])
                elif target in on_stack:
                    low[node_id] = min(low[node_id], indexes[target])
            if low[node_id] == indexes[node_id]:
                component = []
                while stack:
                    item = stack.pop()
                    on_stack.remove(item)
                    component.append(item)
                    if item == node_id:
                        break
                components.append(sorted(component))

        for node_id in sorted(graph):
            if node_id not in indexes:
                visit(node_id)
        return components

    def shortest_cycle_edges(self, component: list[str]) -> list[dict[str, Any]]:
        allowed = set(component)
        outgoing: dict[str, list[dict[str, Any]]] = {node_id: [] for node_id in component}
        for row in self.edges.values():
            if row["kind"] == "depends-on" and row["from"] in allowed and row["to"] in allowed:
                outgoing[row["from"]].append(row)
        best: list[dict[str, Any]] | None = None
        for start in sorted(component):
            queue = deque([(start, [])])
            visited = {start: 0}
            while queue:
                current, path = queue.popleft()
                for row in sorted(outgoing[current], key=lambda item: item["id"]):
                    next_path = path + [row]
                    if row["to"] == start:
                        if best is None or len(next_path) < len(best):
                            best = next_path
                        queue.clear()
                        break
                    if len(next_path) >= (len(best) if best else len(component) + 1):
                        continue
                    if visited.get(row["to"], 10**9) <= len(next_path):
                        continue
                    visited[row["to"]] = len(next_path)
                    queue.append((row["to"], next_path))
        return best or []

    def analyze_findings(self, previous_first_seen: dict[str, str]) -> None:
        for component in self.dependency_sccs():
            cycle_edges = self.shortest_cycle_edges(component)
            self_loop = len(component) == 1 and any(
                row["from"] == component[0] and row["to"] == component[0]
                for row in cycle_edges
            )
            if len(component) <= 1 and not self_loop:
                continue
            ev = [
                self.finding_evidence(
                    row["evidence"]["sourcePath"],
                    row["evidence"]["line"],
                    "module-dependency-cycle",
                    edge_id_value=row["id"],
                )
                for row in cycle_edges
            ]
            labels = [self.nodes[item]["name"] for item in component]
            self.add_finding(
                "dependency-cycle",
                "error",
                "Module dependency cycle",
                "A proven production dependency cycle connects " + " → ".join(labels) + ".",
                component,
                ev,
                "builtin.dependency-cycle",
                previous_first_seen,
            )
        module_nodes = {
            node_id: node for node_id, node in self.nodes.items() if node["kind"] == "module"
        }
        for edge in sorted(self.edges.values(), key=lambda row: row["id"]):
            if edge["kind"] != "depends-on":
                continue
            source = module_nodes[edge["from"]]
            target = module_nodes[edge["to"]]
            for rule in self.rules["dependencyRules"]:
                if rule["fromLayer"] is not None and source["layer"] != rule["fromLayer"]:
                    continue
                if rule["disallowToLayer"] is not None and target["layer"] != rule["disallowToLayer"]:
                    continue
                if rule["fromPlatform"] is not None and source["platform"] != rule["fromPlatform"]:
                    continue
                if rule["toPlatform"] is not None and target["platform"] != rule["toPlatform"]:
                    continue
                if rule["fromModule"] is not None and not glob_match(rule["fromModule"], source["metadata"]["gradlePath"]):
                    continue
                if rule["toModule"] is not None and not glob_match(rule["toModule"], target["metadata"]["gradlePath"]):
                    continue
                self.add_finding(
                    "forbidden-dependency",
                    rule["severity"],
                    "Forbidden module dependency",
                    source["name"] + " depends on " + target["name"] + " in violation of " + rule["id"] + ".",
                    [source["id"], target["id"]],
                    [self.finding_evidence(
                        edge["evidence"]["sourcePath"],
                        edge["evidence"]["line"],
                        "forbidden-dependency",
                        edge_id_value=edge["id"],
                    )],
                    rule["id"],
                    previous_first_seen,
                )
        incoming = {node_id: [] for node_id in self.nodes}
        for edge in self.edges.values():
            incoming[edge["to"]].append(edge)
        for node_id, node in sorted(module_nodes.items()):
            gradle_path = node["metadata"]["gradlePath"]
            if any(glob_match(pattern, gradle_path) for pattern in self.rules["rootModules"]):
                continue
            if any(glob_match(pattern, gradle_path) for pattern in self.rules["standaloneModules"]):
                continue
            relevant = [row for row in incoming[node_id] if row["kind"] in {"depends-on", "renders", "implements", "owns"}]
            if relevant:
                continue
            source_path = node["path"] if node["path"] and node["path"].endswith((".kt", ".kts")) else SETTINGS
            self.add_finding(
                "orphan-module",
                "warning",
                "Orphan module",
                node["name"] + " has no proven incoming dependency or ownership relation.",
                [node_id],
                [self.finding_evidence(source_path, 1, "module-has-no-incoming-relation", node_id=node_id)],
                "builtin.orphan-module",
                previous_first_seen,
            )
        dependency_complete = self.coverage["unsupportedDependencyExpressions"] == 0
        for node_id, node in sorted(self.nodes.items()):
            if node["kind"] == "repository":
                consumers = [row for row in incoming[node_id] if row["kind"] == "consumes"]
                if consumers:
                    continue
                self.add_finding(
                    "unused-repository",
                    "warning" if dependency_complete else "info",
                    "Possible unused repository" if not dependency_complete else "Unused repository",
                    node["name"] + (
                        " has no proven consumer in complete supported coverage."
                        if dependency_complete
                        else " has no proven consumer, but analysis coverage is partial."
                    ),
                    [node_id],
                    [self.finding_evidence(
                        node["path"] or SETTINGS, 1,
                        "repository-has-no-proven-consumer",
                        node_id=node_id,
                    )],
                    "builtin.unused-repository",
                    previous_first_seen,
                )
            if node["kind"] == "screen":
                owners = [row for row in incoming[node_id] if row["kind"] == "owns"]
                if owners:
                    continue
                self.add_finding(
                    "screen-without-owner",
                    "warning",
                    "Screen without feature ownership",
                    node["name"] + " has no canonical feature owns edge.",
                    [node_id],
                    [self.finding_evidence(
                        node["path"] or SETTINGS, 1,
                        "screen-has-no-owner",
                        node_id=node_id,
                    )],
                    "builtin.screen-ownership",
                    previous_first_seen,
                )

    def build(
        self,
        previous_first_seen: dict[str, str],
    ) -> tuple[dict[str, Any], dict[str, Any]]:
        receipt = self.prepare_inputs()
        self.analyze_modules()
        self.analyze_screens_and_features()
        self.analyze_data()
        api_supported, database_supported = self.analyze_api_and_database()
        if self.coverage["unsupportedDependencyExpressions"] or self.coverage["parserErrors"]:
            self.limitations.add("analysis-coverage-partial")
        self.analyze_findings(previous_first_seen)
        limitations = sorted(self.limitations)
        analysis = {
            "status": "partial" if limitations else "complete",
            "capabilities": sorted(self.capabilities),
            "limitations": limitations,
            "coverage": {
                **self.coverage,
                "supportedPersistencePatterns": sorted(self.coverage["supportedPersistencePatterns"]),
                "supportedApiPatterns": sorted(self.coverage["supportedApiPatterns"]),
            },
        }
        nodes = sorted(self.nodes.values(), key=lambda row: row["id"])
        edges = sorted(self.edges.values(), key=lambda row: row["id"])
        findings = sorted(self.findings, key=lambda row: row["id"])
        counts = {severity: 0 for severity in ("error", "warning", "info")}
        for row in findings:
            counts[row["severity"]] += 1
        value: dict[str, Any] = {
            "schemaVersion": SCHEMA_VERSION,
            "generatedAt": utc_now(),
            "generatedAtRevision": self.revision,
            "structuralHash": "sha256:" + "0" * 64,
            "generatorVersion": GENERATOR_VERSION,
            "analysis": analysis,
            "summary": {
                "modules": sum(row["kind"] == "module" for row in nodes),
                "features": sum(row["kind"] == "feature" for row in nodes),
                "screens": sum(row["kind"] == "screen" for row in nodes),
                "dataSources": sum(row["kind"] in {"repository", "data-source"} for row in nodes),
                "databaseEntities": (
                    sum(row["kind"] == "database-entity" for row in nodes)
                    if database_supported else None
                ),
                "findingsBySeverity": counts,
            },
            "nodes": nodes,
            "edges": edges,
            "findings": findings,
        }
        if not api_supported:
            value["analysis"]["status"] = "partial"
        value["structuralHash"] = structural_hash(value)
        validate_map(value)
        return value, receipt


def exact_keys(value: Any, keys: set[str]) -> bool:
    return isinstance(value, dict) and set(value) == keys


def valid_relative_path(value: Any) -> bool:
    if not isinstance(value, str) or len(value.encode("utf-8")) > 512:
        return False
    try:
        return normalize_rel(value) == value
    except ArchitectureError:
        return False


def validate_map(value: Any) -> dict[str, Any]:
    top = {
        "schemaVersion", "generatedAt", "generatedAtRevision", "structuralHash",
        "generatorVersion", "analysis", "summary", "nodes", "edges", "findings",
    }
    if not exact_keys(value, top) or value["schemaVersion"] != 2:
        raise ArchitectureError("map-contract-invalid", "architecture map top-level fields are invalid")
    if (
        not exact_utc_second(value["generatedAt"])
        or not HASH_RE.fullmatch(str(value["generatedAtRevision"]))
        or not HASH_RE.fullmatch(str(value["structuralHash"]))
        or not GENERATOR_ID_RE.fullmatch(str(value["generatorVersion"]))
    ):
        raise ArchitectureError("map-contract-invalid", "architecture map envelope is invalid")
    analysis = value["analysis"]
    coverage_keys = {
        "schemaVersion", "gradleFiles", "dependencyExpressions",
        "unsupportedDependencyExpressions", "kotlinFiles", "parserErrors",
        "excludedModules", "supportedPersistencePatterns", "supportedApiPatterns",
    }
    if (
        not exact_keys(analysis, {"status", "capabilities", "limitations", "coverage"})
        or analysis["status"] not in {"complete", "partial"}
        or not isinstance(analysis["capabilities"], list)
        or analysis["capabilities"] != sorted(set(analysis["capabilities"]))
        or len(analysis["capabilities"]) > 32
        or any(not GENERATOR_ID_RE.fullmatch(str(item)) for item in analysis["capabilities"])
        or not isinstance(analysis["limitations"], list)
        or analysis["limitations"] != sorted(set(analysis["limitations"]))
        or len(analysis["limitations"]) > 100
        or any(item not in LIMITATIONS for item in analysis["limitations"])
        or not exact_keys(analysis["coverage"], coverage_keys)
        or analysis["coverage"]["schemaVersion"] != 1
    ):
        raise ArchitectureError("map-contract-invalid", "architecture analysis metadata is invalid")
    if analysis["status"] != ("partial" if analysis["limitations"] else "complete"):
        raise ArchitectureError(
            "map-contract-invalid",
            "architecture analysis status does not match its limitations",
        )
    coverage = analysis["coverage"]
    count_limits = {
        "gradleFiles": INPUT_MAX_FILES,
        "dependencyExpressions": 1_000_000,
        "unsupportedDependencyExpressions": 1_000_000,
        "kotlinFiles": INPUT_MAX_FILES,
        "parserErrors": 10_000,
        "excludedModules": 10_000,
    }
    if any(
        not isinstance(coverage[key], int)
        or isinstance(coverage[key], bool)
        or not 0 <= coverage[key] <= maximum
        for key, maximum in count_limits.items()
    ):
        raise ArchitectureError("map-contract-invalid", "architecture coverage counts are invalid")
    for key in ("supportedPersistencePatterns", "supportedApiPatterns"):
        if (
            not isinstance(coverage[key], list)
            or coverage[key] != sorted(set(coverage[key]))
            or len(coverage[key]) > 20
            or any(not GENERATOR_ID_RE.fullmatch(str(item)) for item in coverage[key])
        ):
            raise ArchitectureError("map-contract-invalid", "architecture coverage pattern ids are invalid")
    summary = value["summary"]
    if not exact_keys(summary, {
        "modules", "features", "screens", "dataSources", "databaseEntities",
        "findingsBySeverity",
    }) or not exact_keys(summary["findingsBySeverity"], {"error", "warning", "info"}):
        raise ArchitectureError("map-contract-invalid", "architecture summary fields are invalid")
    for key in ("modules", "features", "screens", "dataSources", "databaseEntities"):
        if summary[key] is not None and (
            not isinstance(summary[key], int) or isinstance(summary[key], bool) or summary[key] < 0
        ):
            raise ArchitectureError("map-contract-invalid", "architecture summary count is invalid")
    for severity in SEVERITIES:
        if (
            not isinstance(summary["findingsBySeverity"][severity], int)
            or isinstance(summary["findingsBySeverity"][severity], bool)
            or summary["findingsBySeverity"][severity] < 0
        ):
            raise ArchitectureError("map-contract-invalid", "architecture finding summary is invalid")
    nodes, edges, findings = value["nodes"], value["edges"], value["findings"]
    def ordered_rows(rows: Any, maximum: int) -> bool:
        if (
            not isinstance(rows, list)
            or len(rows) > maximum
            or any(not isinstance(row, dict) or not isinstance(row.get("id"), str) for row in rows)
        ):
            return False
        ids = [row["id"] for row in rows]
        return ids == sorted(ids) and len(ids) == len(set(ids))

    if (
        not ordered_rows(nodes, NODE_MAX)
        or not ordered_rows(edges, EDGE_MAX)
        or not ordered_rows(findings, FINDING_MAX)
    ):
        raise ArchitectureError("map-order-invalid", "architecture arrays are not bounded and sorted")
    node_ids: set[str] = set()
    for node in nodes:
        if (
            not exact_keys(node, {"id", "kind", "name", "path", "platform", "layer", "metadata"})
            or not ID_RE.fullmatch(str(node["id"]))
            or len(str(node["id"]).encode("ascii", "ignore")) > 180
            or node["id"] in node_ids
            or node["kind"] not in NODE_KINDS
            or not isinstance(node["name"], str) or not 1 <= len(node["name"]) <= 200
            or node["path"] is not None and not valid_relative_path(node["path"])
            or node["platform"] not in PLATFORMS
            or node["layer"] not in LAYERS
            or not valid_metadata(node["kind"], node["metadata"])
        ):
            raise ArchitectureError("node-invalid", "architecture node is invalid")
        node_ids.add(node["id"])
    edge_ids: set[str] = set()
    owners: dict[str, str] = {}
    node_kind_by_id = {node["id"]: node["kind"] for node in nodes}
    allowed_edge_pairs = {
        "depends-on": {("module", "module")},
        "owns": {
            ("feature", "screen"),
            ("feature", "database-entity"),
        },
        "implements": {("feature", "repository")},
        "consumes": {
            ("module", "repository"),
            ("module", "data-source"),
            ("module", "api"),
        },
        "renders": {
            ("screen", "module"),
            ("screen", "component"),
        },
        "persists": {("repository", "database-entity")},
        "navigates-to": {("screen", "screen")},
    }
    evidence_keys = {"sourcePath", "line", "analyzer", "confidence"}
    for edge in edges:
        ev = edge["evidence"] if isinstance(edge, dict) else None
        if (
            not exact_keys(edge, {"id", "from", "to", "kind", "evidence"})
            or not ID_RE.fullmatch(str(edge["id"]))
            or edge["id"] in edge_ids
            or edge["from"] not in node_ids
            or edge["to"] not in node_ids
            or edge["kind"] not in EDGE_KINDS
            or (
                node_kind_by_id[edge["from"]],
                node_kind_by_id[edge["to"]],
            ) not in allowed_edge_pairs[edge["kind"]]
            or not exact_keys(ev, evidence_keys)
            or not valid_relative_path(ev["sourcePath"])
            or ev["line"] is not None and (
                not isinstance(ev["line"], int)
                or isinstance(ev["line"], bool)
                or not 1 <= ev["line"] <= 10_000_000
            )
            or not GENERATOR_ID_RE.fullmatch(str(ev["analyzer"]))
            or ev["confidence"] not in CONFIDENCES
        ):
            raise ArchitectureError("edge-invalid", "architecture edge is invalid")
        if edge["kind"] == "owns":
            if edge["to"] in owners and owners[edge["to"]] != edge["from"]:
                raise ArchitectureError("ownership-conflict", "architecture entity has conflicting owners")
            owners[edge["to"]] = edge["from"]
        edge_ids.add(edge["id"])
    finding_ids: set[str] = set()
    severity_counts = {severity: 0 for severity in SEVERITIES}
    finding_evidence_keys = {"sourcePath", "line", "edgeId", "nodeId", "reasonCode"}
    for finding in findings:
        if (
            not exact_keys(finding, {
                "id", "type", "severity", "title", "summary", "affectedNodeIds",
                "evidence", "ruleId", "firstSeenRevision", "fingerprint",
            })
            or not ID_RE.fullmatch(str(finding["id"]))
            or finding["id"] in finding_ids
            or finding["type"] not in FINDING_TYPES
            or finding["severity"] not in SEVERITIES
            or not isinstance(finding["title"], str) or not 1 <= len(finding["title"]) <= 200
            or not isinstance(finding["summary"], str) or not 1 <= len(finding["summary"]) <= 1000
            or not isinstance(finding["affectedNodeIds"], list)
            or not 1 <= len(finding["affectedNodeIds"]) <= 100
            or finding["affectedNodeIds"] != sorted(set(finding["affectedNodeIds"]))
            or any(item not in node_ids for item in finding["affectedNodeIds"])
            or not isinstance(finding["evidence"], list)
            or not 1 <= len(finding["evidence"]) <= 20
            or not RULE_ID_RE.fullmatch(str(finding["ruleId"]))
            or not HASH_RE.fullmatch(str(finding["firstSeenRevision"]))
            or not HASH_RE.fullmatch(str(finding["fingerprint"]))
        ):
            raise ArchitectureError("finding-invalid", "architecture finding is invalid")
        for ev in finding["evidence"]:
            if (
                not exact_keys(ev, finding_evidence_keys)
                or not valid_relative_path(ev["sourcePath"])
                or ev["line"] is not None and (
                    not isinstance(ev["line"], int)
                    or isinstance(ev["line"], bool)
                    or not 1 <= ev["line"] <= 10_000_000
                )
                or ev["edgeId"] is not None and ev["edgeId"] not in edge_ids
                or ev["nodeId"] is not None and ev["nodeId"] not in node_ids
                or ev["reasonCode"] not in FINDING_REASON_CODES
            ):
                raise ArchitectureError("finding-evidence-invalid", "architecture finding evidence is invalid")
        expected_fingerprint = sha256(canonical_bytes({
            "type": finding["type"],
            "ruleId": finding["ruleId"],
            "affectedNodeIds": finding["affectedNodeIds"],
            "evidence": [{
                "sourcePath": row["sourcePath"],
                "line": row["line"],
                "edgeId": row["edgeId"],
                "nodeId": row["nodeId"],
                "reasonCode": row["reasonCode"],
            } for row in finding["evidence"]],
        }))
        if finding["fingerprint"] != expected_fingerprint:
            raise ArchitectureError(
                "finding-fingerprint-invalid",
                "architecture finding fingerprint is invalid",
            )
        severity_counts[finding["severity"]] += 1
        finding_ids.add(finding["id"])
    expected_counts = {
        "modules": sum(node["kind"] == "module" for node in nodes),
        "features": sum(node["kind"] == "feature" for node in nodes),
        "screens": sum(node["kind"] == "screen" for node in nodes),
        "dataSources": sum(node["kind"] in {"repository", "data-source"} for node in nodes),
    }
    if any(summary[key] != expected_counts[key] for key in expected_counts):
        raise ArchitectureError("summary-invalid", "architecture summary does not match nodes")
    database_unknown = "database-schema-not-resolved" in analysis["limitations"]
    if (
        (summary["databaseEntities"] is None) != database_unknown
        or summary["databaseEntities"] is not None
        and summary["databaseEntities"] != sum(node["kind"] == "database-entity" for node in nodes)
    ):
        raise ArchitectureError("summary-invalid", "database entity summary does not match nodes")
    if summary["findingsBySeverity"] != severity_counts:
        raise ArchitectureError("summary-invalid", "finding severity summary does not match findings")
    if value["structuralHash"] != structural_hash(value):
        raise ArchitectureError("structural-hash-invalid", "architecture structural hash is invalid")
    encoded = json.dumps(value, ensure_ascii=False, indent=2).encode("utf-8") + b"\n"
    if len(encoded) > MAP_MAX_BYTES:
        raise ArchitectureError("map-too-large", "architecture map exceeds 5 MiB")
    return value


def read_existing_map(root: Path) -> dict[str, Any] | None:
    file = contained_path(root, OUT)
    try:
        os.lstat(file)
    except FileNotFoundError:
        return None
    try:
        value = json.loads(safe_text(root, OUT, MAP_MAX_BYTES))
        return validate_map(value)
    except (ArchitectureError, json.JSONDecodeError):
        return None


def previous_first_seen(value: dict[str, Any] | None) -> dict[str, str]:
    if not value:
        return {}
    return {
        row["fingerprint"]: row["firstSeenRevision"]
        for row in value["findings"]
        if HASH_RE.fullmatch(str(row.get("fingerprint", "")))
        and HASH_RE.fullmatch(str(row.get("firstSeenRevision", "")))
    }


def compact_diff(
    previous: dict[str, Any] | None,
    current: dict[str, Any],
    trigger: str,
    trigger_id: str,
    task_stem: str | None,
) -> dict[str, Any]:
    previous_nodes = {row["id"]: row for row in previous["nodes"]} if previous else {}
    current_nodes = {row["id"]: row for row in current["nodes"]}
    previous_edges = {row["id"]: row for row in previous["edges"]} if previous else {}
    current_edges = {row["id"]: row for row in current["edges"]}
    previous_findings = {row["fingerprint"]: row for row in previous["findings"]} if previous else {}
    current_findings = {row["fingerprint"]: row for row in current["findings"]}

    def ownership(value: dict[str, Any] | None) -> dict[str, str]:
        if not value:
            return {}
        return {
            row["to"]: row["from"] for row in value["edges"]
            if row["kind"] == "owns"
        }

    before_owners, after_owners = ownership(previous), ownership(current)
    ownership_changes = [
        {"nodeId": node_id, "before": before_owners.get(node_id), "after": after_owners.get(node_id)}
        for node_id in sorted(set(before_owners) | set(after_owners))
        if before_owners.get(node_id) != after_owners.get(node_id)
    ]
    baseline = previous is None
    complete_changes = {
        "nodesAdded": [] if baseline else sorted(set(current_nodes) - set(previous_nodes)),
        "nodesRemoved": [] if baseline else sorted(set(previous_nodes) - set(current_nodes)),
        "edgesAdded": [] if baseline else sorted(set(current_edges) - set(previous_edges)),
        "edgesRemoved": [] if baseline else sorted(set(previous_edges) - set(current_edges)),
        "findingsIntroduced": [] if baseline else sorted(
            current_findings[key]["id"] for key in set(current_findings) - set(previous_findings)
        ),
        "findingsResolved": [] if baseline else sorted(
            previous_findings[key]["id"] for key in set(previous_findings) - set(current_findings)
        ),
        "ownershipChanges": [] if baseline else ownership_changes,
        "capabilitiesAdded": [] if baseline else sorted(
            set(current["analysis"]["capabilities"]) - set(previous["analysis"]["capabilities"])
        ),
        "capabilitiesRemoved": [] if baseline else sorted(
            set(previous["analysis"]["capabilities"]) - set(current["analysis"]["capabilities"])
        ),
    }
    limits = {
        "nodesAdded": 2000,
        "nodesRemoved": 2000,
        "edgesAdded": 5000,
        "edgesRemoved": 5000,
        "findingsIntroduced": 2000,
        "findingsResolved": 2000,
        "ownershipChanges": 1000,
        "capabilitiesAdded": 32,
        "capabilitiesRemoved": 32,
    }
    changes = {
        key: rows[:limits[key]]
        for key, rows in complete_changes.items()
    }
    change_totals = {
        key: len(rows)
        for key, rows in complete_changes.items()
    }
    truncated = any(
        change_totals[key] > len(changes[key])
        for key in changes
    )
    diff_id = stable_id(
        "diff",
        trigger + "/" + trigger_id + "/" + current["structuralHash"][-24:],
    )
    return {
        "schemaVersion": 2,
        "id": diff_id,
        "trigger": trigger,
        "triggerId": trigger_id,
        "taskStem": task_stem,
        "createdAt": utc_now(),
        "previousHash": previous["structuralHash"] if previous else None,
        "currentHash": current["structuralHash"],
        "previousRevision": previous["generatedAtRevision"] if previous else None,
        "currentRevision": current["generatedAtRevision"],
        "baselineCreated": baseline,
        "generatorChanged": bool(previous and previous["generatorVersion"] != current["generatorVersion"]),
        "truncated": truncated,
        "changeTotals": change_totals,
        "changes": changes,
    }


def valid_compact_diff(value: Any) -> bool:
    fields = {
        "schemaVersion", "id", "trigger", "triggerId", "taskStem", "createdAt",
        "previousHash", "currentHash", "previousRevision", "currentRevision",
        "baselineCreated", "generatorChanged", "truncated", "changeTotals",
        "changes",
    }
    change_fields = {
        "nodesAdded", "nodesRemoved", "edgesAdded", "edgesRemoved",
        "findingsIntroduced", "findingsResolved", "ownershipChanges",
        "capabilitiesAdded", "capabilitiesRemoved",
    }
    limits = {
        "nodesAdded": 2000,
        "nodesRemoved": 2000,
        "edgesAdded": 5000,
        "edgesRemoved": 5000,
        "findingsIntroduced": 2000,
        "findingsResolved": 2000,
        "ownershipChanges": 1000,
        "capabilitiesAdded": 32,
        "capabilitiesRemoved": 32,
    }
    total_limits = {
        "nodesAdded": NODE_MAX,
        "nodesRemoved": NODE_MAX,
        "edgesAdded": EDGE_MAX,
        "edgesRemoved": EDGE_MAX,
        "findingsIntroduced": FINDING_MAX,
        "findingsResolved": FINDING_MAX,
        "ownershipChanges": NODE_MAX,
        "capabilitiesAdded": 32,
        "capabilitiesRemoved": 32,
    }
    if (
        not exact_keys(value, fields)
        or value["schemaVersion"] != 2
        or not ID_RE.fullmatch(str(value["id"]))
        or value["trigger"] not in {"manual-refresh", "task-finalization"}
        or not TRIGGER_ID_RE.fullmatch(str(value["triggerId"]))
        or value["trigger"] == "task-finalization"
        and not TASK_STEM_RE.fullmatch(str(value["taskStem"]))
        or value["trigger"] == "manual-refresh"
        and value["taskStem"] is not None
        or not exact_utc_second(value["createdAt"])
        or not HASH_RE.fullmatch(str(value["currentHash"]))
        or not HASH_RE.fullmatch(str(value["currentRevision"]))
        or value["previousHash"] is not None
        and not HASH_RE.fullmatch(str(value["previousHash"]))
        or value["previousRevision"] is not None
        and not HASH_RE.fullmatch(str(value["previousRevision"]))
        or (value["previousHash"] is None) != (value["previousRevision"] is None)
        or not isinstance(value["baselineCreated"], bool)
        or value["baselineCreated"]
        != (value["previousHash"] is None and value["previousRevision"] is None)
        or not isinstance(value["generatorChanged"], bool)
        or not isinstance(value["truncated"], bool)
        or not exact_keys(value["changeTotals"], change_fields)
        or not exact_keys(value["changes"], change_fields)
    ):
        return False
    for key in change_fields - {"ownershipChanges"}:
        rows = value["changes"][key]
        pattern = GENERATOR_ID_RE if key.startswith("capabilities") else ID_RE
        if (
            not isinstance(rows, list)
            or len(rows) > limits[key]
            or rows != sorted(set(rows))
            or any(not pattern.fullmatch(str(item)) for item in rows)
        ):
            return False
    ownership_rows = value["changes"]["ownershipChanges"]
    if not isinstance(ownership_rows, list) or len(ownership_rows) > limits["ownershipChanges"]:
        return False
    prior = ""
    for row in ownership_rows:
        if (
            not exact_keys(row, {"nodeId", "before", "after"})
            or not ID_RE.fullmatch(str(row["nodeId"]))
            or row["before"] is not None and not ID_RE.fullmatch(str(row["before"]))
            or row["after"] is not None and not ID_RE.fullmatch(str(row["after"]))
            or row["before"] == row["after"]
            or row["nodeId"] <= prior
        ):
            return False
        prior = row["nodeId"]
    truncated = False
    for key in change_fields:
        total = value["changeTotals"][key]
        if (
            not isinstance(total, int)
            or isinstance(total, bool)
            or total < len(value["changes"][key])
            or total > total_limits[key]
            or value["baselineCreated"] and total != 0
        ):
            return False
        truncated = truncated or total > len(value["changes"][key])
    return value["truncated"] == truncated


def ensure_safe_directory(root: Path, relative: str) -> Path:
    normalized = normalize_rel(relative)
    current = root
    private_cache = False
    for part in normalized.split("/"):
        current = current / part
        private_cache = private_cache or part == ".cache"
        try:
            info = os.lstat(current)
        except FileNotFoundError:
            os.mkdir(current, 0o700 if private_cache else 0o755)
            info = os.lstat(current)
        if stat.S_ISLNK(info.st_mode) or not stat.S_ISDIR(info.st_mode):
            raise ArchitectureError("publication-root-unsafe", f"publication directory is unsafe: {normalized}")
        if private_cache and os.name != "nt":
            if not hasattr(os, "geteuid") or info.st_uid != os.geteuid():
                raise ArchitectureError("publication-root-unsafe", f"publication cache is not owned by the current user: {normalized}")
            mode = stat.S_IMODE(info.st_mode)
            if mode & 0o022:
                raise ArchitectureError("publication-root-unsafe", f"publication cache is group/world writable: {normalized}")
            flags = os.O_RDONLY | getattr(os, "O_DIRECTORY", 0) | getattr(os, "O_NOFOLLOW", 0)
            fd = None
            try:
                fd = os.open(current, flags)
                opened = os.fstat(fd)
                if (not stat.S_ISDIR(opened.st_mode) or opened.st_dev != info.st_dev or
                        opened.st_ino != info.st_ino or opened.st_uid != info.st_uid):
                    raise ArchitectureError("publication-root-unsafe", f"publication cache changed before privacy hardening: {normalized}")
                if stat.S_IMODE(opened.st_mode) != 0o700:
                    os.fchmod(fd, 0o700)
                hardened = os.fstat(fd)
                if (stat.S_IMODE(hardened.st_mode) != 0o700 or hardened.st_dev != opened.st_dev or
                        hardened.st_ino != opened.st_ino or hardened.st_uid != opened.st_uid):
                    raise ArchitectureError("publication-root-unsafe", f"publication cache privacy hardening failed: {normalized}")
                live = os.lstat(current)
                if (stat.S_ISLNK(live.st_mode) or live.st_dev != hardened.st_dev or
                        live.st_ino != hardened.st_ino or live.st_uid != hardened.st_uid):
                    raise ArchitectureError("publication-root-unsafe", f"publication cache changed after privacy hardening: {normalized}")
            finally:
                if fd is not None:
                    os.close(fd)
    return current


@contextlib.contextmanager
def generation_lock(root: Path):
    """Serialize one complete analysis/publication bundle in this project."""
    directory = ensure_safe_directory(root, CACHE_ROOT)
    lock_path = directory / "generation.lock"
    flags = os.O_RDWR | os.O_CREAT | getattr(os, "O_NOFOLLOW", 0)
    try:
        fd = os.open(lock_path, flags, 0o600)
    except OSError as exc:
        raise ArchitectureError(
            "generation-lock-unavailable",
            "architecture generation lock is unavailable",
        ) from exc
    try:
        info = os.fstat(fd)
        if not stat.S_ISREG(info.st_mode) or info.st_nlink != 1:
            raise ArchitectureError(
                "generation-lock-unsafe",
                "architecture generation lock is unsafe",
            )
        if fcntl is not None:
            fcntl.flock(fd, fcntl.LOCK_EX)
        elif msvcrt is not None:
            if info.st_size == 0:
                os.write(fd, b"\0")
                os.fsync(fd)
            os.lseek(fd, 0, os.SEEK_SET)
            msvcrt.locking(fd, msvcrt.LK_LOCK, 1)
        else:
            raise ArchitectureError(
                "generation-lock-unavailable",
                "architecture generation lock is unsupported on this platform",
            )
        yield
    except OSError as exc:
        raise ArchitectureError(
            "generation-lock-unavailable",
            "architecture generation lock could not be acquired",
        ) from exc
    finally:
        try:
            try:
                if fcntl is not None:
                    fcntl.flock(fd, fcntl.LOCK_UN)
                elif msvcrt is not None:
                    os.lseek(fd, 0, os.SEEK_SET)
                    msvcrt.locking(fd, msvcrt.LK_UNLCK, 1)
            except OSError:
                pass
        finally:
            os.close(fd)


def verify_input_receipt(root: Path, receipt: dict[str, Any]) -> None:
    """Re-prove all analyzed inputs immediately before publication."""
    files = receipt.get("files")
    if (
        not isinstance(files, list)
        or len(files) != receipt.get("fileCount")
        or len(files) > INPUT_MAX_FILES
    ):
        raise ArchitectureError("input-receipt-invalid", "architecture input receipt is invalid")
    total = 0
    for row in files:
        if (
            not isinstance(row, dict)
            or set(row) != {"path", "contentHash", "executable"}
            or not valid_relative_path(row["path"])
            or not HASH_RE.fullmatch(str(row["contentHash"]))
            or not isinstance(row["executable"], bool)
        ):
            raise ArchitectureError("input-receipt-invalid", "architecture input receipt row is invalid")
        raw = safe_read(root, row["path"], INPUT_MAX_BYTES)
        total += len(raw)
        if (
            total > INPUT_MAX_BYTES
            or sha256(raw) != row["contentHash"]
            or bool(os.lstat(contained_path(root, row["path"])).st_mode & 0o111)
                != row["executable"]
        ):
            raise ArchitectureError(
                "input-changed-before-publication",
                "architecture inputs changed before publication",
            )
    if total != receipt.get("totalBytes"):
        raise ArchitectureError(
            "input-changed-before-publication",
            "architecture input byte total changed before publication",
        )


def atomic_write(root: Path, relative: str, raw: bytes, max_bytes: int) -> None:
    if len(raw) > max_bytes:
        raise ArchitectureError("publication-too-large", f"publication exceeds its byte bound: {relative}")
    normalized = normalize_rel(relative)
    directory = ensure_safe_directory(root, str(Path(normalized).parent).replace("\\", "/"))
    target = contained_path(root, normalized)
    temp = directory / ("." + target.name + "." + str(os.getpid()) + "." + secrets.token_hex(12) + ".tmp")
    fd = None
    try:
        flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0)
        fd = os.open(temp, flags, 0o644)
        written = 0
        while written < len(raw):
            written += os.write(fd, raw[written:])
        os.fsync(fd)
        os.close(fd)
        fd = None
        os.replace(temp, target)
        directory_fd = os.open(directory, os.O_RDONLY)
        try:
            os.fsync(directory_fd)
        finally:
            os.close(directory_fd)
    except OSError as exc:
        try:
            os.unlink(temp)
        except OSError:
            pass
        raise ArchitectureError("publication-failed", f"atomic publication failed: {normalized}") from exc
    finally:
        if fd is not None:
            os.close(fd)


def immutable_write(root: Path, relative: str, raw: bytes, max_bytes: int) -> None:
    if len(raw) > max_bytes:
        raise ArchitectureError(
            "publication-too-large",
            f"immutable publication exceeds its byte bound: {relative}",
        )
    normalized = normalize_rel(relative)
    directory = ensure_safe_directory(root, str(Path(normalized).parent).replace("\\", "/"))
    target = contained_path(root, normalized)
    try:
        existing = safe_read(root, normalized, max_bytes)
    except ArchitectureError as exc:
        if exc.code != "input-unreadable":
            raise
        existing = None
    if existing is not None:
        if existing != raw:
            raise ArchitectureError("history-conflict", "immutable architecture history id already exists with different bytes")
        return
    flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0)
    try:
        fd = os.open(target, flags, 0o644)
        try:
            written = 0
            while written < len(raw):
                count = os.write(fd, raw[written:])
                if count <= 0:
                    raise OSError("short immutable history write")
                written += count
            os.fsync(fd)
        finally:
            os.close(fd)
        directory_fd = os.open(directory, os.O_RDONLY)
        try:
            os.fsync(directory_fd)
        finally:
            os.close(directory_fd)
    except FileExistsError:
        existing = safe_read(root, normalized, max_bytes)
        if existing != raw:
            raise ArchitectureError("history-conflict", "architecture history raced with a conflicting writer")


def reusable_history_diff(
    root: Path,
    relative: str,
    current: dict[str, Any],
    trigger: str,
    trigger_id: str,
    task_stem: str | None,
) -> dict[str, Any] | None:
    try:
        value = json.loads(safe_text(root, relative, 2 * 1024 * 1024))
    except ArchitectureError as exc:
        if exc.code == "input-unreadable":
            return None
        raise
    except json.JSONDecodeError as exc:
        raise ArchitectureError(
            "history-conflict",
            "immutable architecture history is malformed",
        ) from exc
    expected_fields = {
        "schemaVersion", "id", "trigger", "triggerId", "taskStem", "createdAt",
        "previousHash", "currentHash", "previousRevision", "currentRevision",
        "baselineCreated", "generatorChanged", "truncated", "changeTotals",
        "changes",
    }
    if (
        not exact_keys(value, expected_fields)
        or not valid_compact_diff(value)
        or value["trigger"] != trigger
        or value["triggerId"] != trigger_id
        or value["taskStem"] != task_stem
        or value["currentHash"] != current["structuralHash"]
        or value["currentRevision"] != current["generatedAtRevision"]
        or value["id"] != stable_id(
            "diff",
            trigger + "/" + trigger_id + "/" + current["structuralHash"][-24:],
        )
        or not isinstance(value["changes"], dict)
    ):
        raise ArchitectureError(
            "history-conflict",
            "immutable architecture history conflicts with this generation",
        )
    return value


def history_names(root: Path) -> list[str]:
    directory = ensure_safe_directory(root, HISTORY_DIR)
    try:
        names = sorted(os.listdir(directory))
    except OSError as exc:
        raise ArchitectureError(
            "history-unavailable",
            "architecture history could not be listed",
        ) from exc
    if len(names) > 1000:
        raise ArchitectureError(
            "history-limit",
            "architecture history exceeds its bounded recovery limit",
        )
    return names


def prune_history(root: Path, kept_paths: set[str]) -> None:
    directory = ensure_safe_directory(root, HISTORY_DIR)
    kept_names = {Path(relative).name for relative in kept_paths}
    for name in history_names(root):
        if name in kept_names or not re.fullmatch(
            r"[A-Za-z0-9][A-Za-z0-9._-]{0,184}-[a-f0-9]{24}\.json",
            name,
        ):
            continue
        target = directory / name
        try:
            info = os.lstat(target)
            if stat.S_ISREG(info.st_mode) and not stat.S_ISLNK(info.st_mode) and info.st_nlink == 1:
                os.unlink(target)
        except OSError:
            # Cache cleanup is best-effort after the current authoritative
            # map/pointers have already been committed.
            continue


def read_history_index(root: Path) -> dict[str, Any]:
    try:
        value = json.loads(safe_text(root, HISTORY_INDEX, 1024 * 1024))
    except (ArchitectureError, json.JSONDecodeError):
        return {"schemaVersion": 1, "entries": []}
    if (
        not exact_keys(value, {"schemaVersion", "entries"})
        or value["schemaVersion"] != 1
        or not isinstance(value["entries"], list)
    ):
        return {"schemaVersion": 1, "entries": []}
    entries = []
    for row in value["entries"][:HISTORY_MAX]:
        if (
            isinstance(row, dict)
            and set(row) == {"id", "path", "trigger", "triggerId", "taskStem", "createdAt", "currentHash"}
            and ID_RE.fullmatch(str(row["id"]))
            and valid_relative_path(row["path"])
            and row["trigger"] in {"manual-refresh", "task-finalization"}
            and TRIGGER_ID_RE.fullmatch(str(row["triggerId"]))
            and (row["taskStem"] is None or TASK_STEM_RE.fullmatch(str(row["taskStem"])))
            and HASH_RE.fullmatch(str(row["currentHash"]))
        ):
            entries.append(row)
    return {"schemaVersion": 1, "entries": entries}


def publish_generation(
    root: Path,
    value: dict[str, Any],
    receipt: dict[str, Any],
    previous: dict[str, Any] | None,
    *,
    trigger: str,
    trigger_id: str,
    task_stem: str | None,
    publication_guard: Callable[[], None] | None = None,
) -> dict[str, Any]:
    if trigger not in {"manual-refresh", "task-finalization"}:
        raise ArchitectureError("trigger-invalid", "architecture trigger is invalid")
    if not TRIGGER_ID_RE.fullmatch(trigger_id):
        raise ArchitectureError("trigger-invalid", "architecture trigger id is invalid")
    if trigger == "task-finalization":
        if not task_stem or not TASK_STEM_RE.fullmatch(task_stem):
            raise ArchitectureError("task-stem-invalid", "task finalization requires a valid task stem")
    elif task_stem is not None:
        raise ArchitectureError("task-stem-invalid", "manual refresh cannot carry a task stem")
    validate_map(value)
    history_name = (
        trigger_id + "-" + value["structuralHash"].split(":", 1)[1][:24] + ".json"
    )
    history_relative = HISTORY_DIR + "/" + history_name
    # Refuse an already-unbounded/unsafe runtime directory before changing the
    # current map, then re-prove that the analyzed source set is still exact.
    history_names(root)
    verify_input_receipt(root, receipt)
    if publication_guard is not None:
        publication_guard()
    diff = reusable_history_diff(
        root, history_relative, value, trigger, trigger_id, task_stem
    )
    if diff is None:
        diff = compact_diff(previous, value, trigger, trigger_id, task_stem)
    diff_raw = json.dumps(diff, ensure_ascii=False, indent=2).encode("utf-8") + b"\n"
    # Publication ordering is deliberate: immutable history first, then the
    # canonical map, then movable pointers/index. A crash can leave an orphan
    # history record, never a pointer claiming a map that was not committed.
    immutable_write(root, history_relative, diff_raw, 2 * 1024 * 1024)
    if publication_guard is not None:
        publication_guard()
    map_raw = json.dumps(value, ensure_ascii=False, indent=2).encode("utf-8") + b"\n"
    atomic_write(root, OUT, map_raw, MAP_MAX_BYTES)
    pointer = dict(diff)
    pointer["historyPath"] = history_relative
    pointer["followedByChanges"] = False
    pointer_raw = json.dumps(pointer, ensure_ascii=False, indent=2).encode("utf-8") + b"\n"
    atomic_write(root, LATEST_DIFF, pointer_raw, 2 * 1024 * 1024)
    if trigger == "task-finalization":
        atomic_write(root, LATEST_TASK_DIFF, pointer_raw, 2 * 1024 * 1024)
    index = read_history_index(root)
    entry = {
        "id": diff["id"],
        "path": history_relative,
        "trigger": trigger,
        "triggerId": trigger_id,
        "taskStem": task_stem,
        "createdAt": diff["createdAt"],
        "currentHash": diff["currentHash"],
    }
    entries = [entry] + [row for row in index["entries"] if row["id"] != entry["id"]]
    task_count = 0
    bounded_entries = []
    for row in entries:
        if row["trigger"] == "task-finalization":
            task_count += 1
            if task_count > TASK_HISTORY_MAX:
                continue
        bounded_entries.append(row)
        if len(bounded_entries) >= HISTORY_MAX:
            break
    index_raw = json.dumps(
        {"schemaVersion": 1, "entries": bounded_entries},
        ensure_ascii=False,
        indent=2,
    ).encode("utf-8") + b"\n"
    atomic_write(root, HISTORY_INDEX, index_raw, 1024 * 1024)
    receipt_raw = json.dumps(receipt, ensure_ascii=False, indent=2).encode("utf-8") + b"\n"
    atomic_write(root, INPUT_RECEIPT, receipt_raw, 8 * 1024 * 1024)
    prune_history(root, {row["path"] for row in bounded_entries})
    return diff


def build_map(root: Path) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any] | None]:
    previous = read_existing_map(root)
    analyzer = Analyzer(root)
    value, receipt = analyzer.build(previous_first_seen(previous))
    return value, receipt, previous
