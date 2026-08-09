#!/usr/bin/env python3
"""Generate, validate, diff, and atomically publish Architecture Map v2.

Run from the project root:

  python3 orchestrator/tasks/regen-arch.py
  python3 orchestrator/tasks/regen-arch.py --check
  python3 orchestrator/tasks/regen-arch.py --check-json
  python3 orchestrator/tasks/regen-arch.py --revision-json
  python3 orchestrator/tasks/regen-arch.py \
    --trigger task-finalization --trigger-id <transaction-id> --task-stem TASK_1_name

The canonical file and every Architecture consumer use only the strict v2
contract; unsupported shapes are rejected.
"""

from __future__ import annotations

import json
import os
import re
import stat
import subprocess
import sys
from pathlib import Path

from architecture_analysis import (
    ArchitectureError,
    HASH_RE,
    OUT,
    SETTINGS,
    TASK_STEM_RE,
    TRIGGER_ID_RE,
    build_map,
    generation_lock,
    publish_generation,
    read_existing_map,
    validate_map,
)


def fail(message: str, code: int = 1) -> int:
    sys.stderr.write(message.rstrip("\n") + "\n")
    return code


def parse_args(argv: list[str]) -> dict[str, object]:
    result: dict[str, object] = {
        "mode": "generate",
        "trigger": "manual-refresh",
        "triggerId": None,
        "taskStem": None,
        "expectedSourceRevision": None,
    }
    index = 0
    while index < len(argv):
        arg = argv[index]
        if arg in {"--check", "--check-json", "--revision-json"}:
            if result["mode"] != "generate":
                raise ArchitectureError("usage", "only one read-only mode may be selected")
            result["mode"] = arg[2:]
        elif arg in {
            "--trigger",
            "--trigger-id",
            "--task-stem",
            "--expected-source-revision",
        }:
            index += 1
            if index >= len(argv):
                raise ArchitectureError("usage", arg + " requires a value")
            key = {
                "--trigger": "trigger",
                "--trigger-id": "triggerId",
                "--task-stem": "taskStem",
                "--expected-source-revision": "expectedSourceRevision",
            }[arg]
            result[key] = argv[index]
        else:
            raise ArchitectureError("usage", "unsupported argument: " + arg)
        index += 1
    if result["mode"] != "generate" and any(
        result[key] is not None
        for key in ("triggerId", "taskStem", "expectedSourceRevision")
    ):
        raise ArchitectureError("usage", "read-only modes do not accept publication identity")
    if result["mode"] != "generate" and result["trigger"] != "manual-refresh":
        raise ArchitectureError("usage", "read-only modes do not accept a trigger")
    return result


def absent_payload() -> dict[str, object]:
    return {
        "version": 2,
        "status": "absent",
        "fresh": True,
        "path": OUT,
        "actualHash": None,
        "expectedHash": None,
        "actualRevision": None,
        "expectedRevision": None,
        "reason": "pre-bootstrap",
    }


def architecture_publication_guard(root: Path) -> None:
    """Fail closed unless the bound site writer lease is still exact."""
    node = os.environ.get("ORCHESTRATOR_NODE_EXECUTABLE", "")
    helper_raw = os.environ.get("ORCHESTRATOR_ARCHITECTURE_LEASE_VERIFIER", "")
    helper = Path(helper_raw)
    if (
        not node
        or not os.path.isabs(node)
        or not helper_raw
        or not helper.is_absolute()
    ):
        raise ArchitectureError(
            "writer-lease-lost",
            "architecture writer runtime is unavailable",
        )
    try:
        node_info = os.lstat(node)
        helper_info = os.lstat(helper)
    except OSError as exc:
        raise ArchitectureError(
            "writer-lease-lost",
            "architecture writer verifier is unavailable",
        ) from exc
    if (
        not stat.S_ISREG(node_info.st_mode)
        or stat.S_ISLNK(node_info.st_mode)
        or not node_info.st_mode & stat.S_IXUSR
        or not stat.S_ISREG(helper_info.st_mode)
        or stat.S_ISLNK(helper_info.st_mode)
        or helper_info.st_nlink != 1
    ):
        raise ArchitectureError(
            "writer-lease-lost",
            "architecture writer verifier is unsafe",
        )
    try:
        result = subprocess.run(
            [node, str(helper)],
            cwd=root,
            env=os.environ.copy(),
            stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            timeout=10,
            check=False,
        )
    except (OSError, subprocess.SubprocessError) as exc:
        raise ArchitectureError(
            "writer-lease-lost",
            "architecture writer lease could not be verified",
        ) from exc
    if result.returncode != 0 or len(result.stdout) > 4096:
        raise ArchitectureError(
            "writer-lease-lost",
            "architecture writer lease ownership was lost",
        )
    try:
        value = json.loads(result.stdout)
    except json.JSONDecodeError as exc:
        raise ArchitectureError(
            "writer-lease-lost",
            "architecture writer lease proof is invalid",
        ) from exc
    if (
        not isinstance(value, dict)
        or set(value) != {"version", "verified", "leaseId", "expiresAt"}
        or value["version"] != 1
        or value["verified"] is not True
        or value["leaseId"]
            != os.environ.get("ORCHESTRATOR_ARCHITECTURE_LEASE_ID")
    ):
        raise ArchitectureError(
            "writer-lease-lost",
            "architecture writer lease proof is invalid",
        )


def main(argv: list[str]) -> int:
    root = Path.cwd().resolve()
    try:
        orchestrator = os.lstat(root / "orchestrator")
    except OSError:
        orchestrator = None
    if orchestrator is None or not stat.S_ISDIR(orchestrator.st_mode) or stat.S_ISLNK(orchestrator.st_mode):
        return fail("regen-arch.py must be run from the repo root (safe ./orchestrator directory not found)")
    try:
        options = parse_args(argv)
    except ArchitectureError as exc:
        return fail("regen-arch.py: " + str(exc), 2)
    settings = root / SETTINGS
    try:
        settings_stat = os.lstat(settings)
    except FileNotFoundError:
        settings_stat = None
    if settings_stat is None:
        if options["mode"] in {"check-json", "revision-json"}:
            print(json.dumps(absent_payload(), sort_keys=True, separators=(",", ":")))
        else:
            print(f"notice: no {SETTINGS} at repo root — no product to map; skipping.")
        return 0
    if not stat.S_ISREG(settings_stat.st_mode) or stat.S_ISLNK(settings_stat.st_mode) or settings_stat.st_nlink != 1:
        return fail("regen-arch.py: settings.gradle.kts is not a safe regular file")
    lock_context = generation_lock(root)
    locked = False
    try:
        lock_context.__enter__()
        locked = True
        candidate, receipt, previous = build_map(root)
        mode = str(options["mode"])
        expected_source_revision = options["expectedSourceRevision"]
        if (
            expected_source_revision is not None
            and not HASH_RE.fullmatch(str(expected_source_revision))
        ):
            raise ArchitectureError(
                "source-conflict",
                "expected architecture source revision is invalid",
            )
        if (
            expected_source_revision is not None
            and candidate["generatedAtRevision"] != expected_source_revision
        ):
            raise ArchitectureError(
                "source-conflict",
                "architecture inputs changed before analysis started",
            )
        if mode == "revision-json":
            print(json.dumps({
                "version": 2,
                "status": "ready",
                "fresh": previous is not None and previous.get("generatedAtRevision") == candidate["generatedAtRevision"],
                "path": OUT,
                "actualHash": previous.get("structuralHash") if previous else None,
                "expectedHash": candidate["structuralHash"],
                "actualRevision": previous.get("generatedAtRevision") if previous else None,
                "expectedRevision": candidate["generatedAtRevision"],
                "reason": None if previous and previous.get("generatedAtRevision") == candidate["generatedAtRevision"] else "source-revision-drift",
            }, sort_keys=True, separators=(",", ":")))
            return 0
        if mode in {"check", "check-json"}:
            current = read_existing_map(root)
            fresh = bool(
                current
                and current["generatedAtRevision"] == candidate["generatedAtRevision"]
                and current["structuralHash"] == candidate["structuralHash"]
            )
            reason = None
            if current is None:
                reason = "missing-or-invalid"
            elif current["generatedAtRevision"] != candidate["generatedAtRevision"]:
                reason = "source-revision-drift"
            elif current["structuralHash"] != candidate["structuralHash"]:
                reason = "structural-drift"
            payload = {
                "version": 2,
                "status": "fresh" if fresh else "stale",
                "fresh": fresh,
                "path": OUT,
                "actualHash": current["structuralHash"] if current else None,
                "expectedHash": candidate["structuralHash"],
                "actualRevision": current["generatedAtRevision"] if current else None,
                "expectedRevision": candidate["generatedAtRevision"],
                "reason": reason,
            }
            if mode == "check-json":
                print(json.dumps(payload, sort_keys=True, separators=(",", ":")))
            elif fresh:
                print(f"OK: {OUT} is fresh.")
            else:
                sys.stderr.write(
                    f"STALE: {OUT} ({reason}) — run: python3 orchestrator/tasks/regen-arch.py\n"
                )
            return 0 if fresh else 1
        trigger = str(options["trigger"])
        trigger_id = options["triggerId"]
        task_stem = options["taskStem"]
        if trigger not in {"manual-refresh", "task-finalization"}:
            raise ArchitectureError("trigger-invalid", "trigger must be manual-refresh or task-finalization")
        if trigger_id is None:
            trigger_id = "cli-" + candidate["generatedAt"].replace("-", "").replace(":", "") + "-" + str(os.getpid())
        if not TRIGGER_ID_RE.fullmatch(str(trigger_id)):
            raise ArchitectureError("trigger-invalid", "trigger id has an invalid shape")
        if trigger == "task-finalization":
            if not isinstance(task_stem, str) or not TASK_STEM_RE.fullmatch(task_stem):
                raise ArchitectureError("task-stem-invalid", "task-finalization requires --task-stem")
        elif task_stem is not None:
            raise ArchitectureError("task-stem-invalid", "manual-refresh does not accept --task-stem")
        validate_map(candidate)
        guarded_site_job = bool(
            trigger == "manual-refresh"
            and re.fullmatch(r"archjob-[a-f0-9]{32}", str(trigger_id))
        )
        if guarded_site_job and not all(os.environ.get(key) for key in (
            "ORCHESTRATOR_ARCHITECTURE_LEASE_ID",
            "ORCHESTRATOR_ARCHITECTURE_LEASE_TOKEN",
            "ORCHESTRATOR_ARCHITECTURE_WRITER_DIR",
            "ORCHESTRATOR_ARCHITECTURE_WRITER_AUTHORITY",
            "ORCHESTRATOR_ARCHITECTURE_LEASE_VERIFIER",
            "ORCHESTRATOR_NODE_EXECUTABLE",
        )):
            raise ArchitectureError(
                "writer-lease-lost",
                "typed architecture generation requires an exact writer lease",
            )
        diff = publish_generation(
            root,
            candidate,
            receipt,
            previous,
            trigger=trigger,
            trigger_id=str(trigger_id),
            task_stem=str(task_stem) if task_stem is not None else None,
            publication_guard=(
                (lambda: architecture_publication_guard(root))
                if guarded_site_job else None
            ),
        )
        print(json.dumps({
            "version": 2,
            "status": "published",
            "path": OUT,
            "structuralHash": candidate["structuralHash"],
            "generatedAtRevision": candidate["generatedAtRevision"],
            "diffId": diff["id"],
        }, sort_keys=True, separators=(",", ":")))
        return 0
    except ArchitectureError as exc:
        return fail(f"regen-arch.py: {exc.code}: {exc}")
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        return fail("regen-arch.py: generation-failed: " + str(exc))
    except Exception as exc:
        return fail(
            "regen-arch.py: generation-failed: unexpected " +
            type(exc).__name__
        )
    finally:
        if locked:
            lock_context.__exit__(None, None, None)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
