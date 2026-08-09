#!/usr/bin/env python3
"""Canonical Python entrypoint for task INDEX publication and checking.

All derivation, validation, locking, and publication live in ``task-index.mjs``
and ``task-state-core.cjs``. This wrapper exists for Python-based lifecycle
writers and exposes only the current CLI contract: no import-time parsers or
alternate INDEX implementation.
"""

import json
import os
import re
import subprocess
import sys


BASE = "orchestrator/tasks"
HERE = os.path.dirname(os.path.abspath(__file__))
OUTCOME_SHAPE_PATH = os.path.normpath(
    os.path.join(HERE, "..", "contracts", "outcome-shape.json"))
TASK_STATE_PREFIX = "[task-state] "
TASK_STATE_MAX_STDERR = 256 * 1024
TASK_STATE_FIELDS = {
    "version", "event", "caller", "scope", "action", "transition", "phase",
    "observedState", "expectedState", "snapshotHash", "durationMs",
    "slowThresholdMs", "slow", "scanMode", "taskBodyReads",
    "architectureStatus", "findings", "findingsTruncated", "result", "ok",
    "overallOk",
}


def bounded_int(value, maximum):
    return value if type(value) is int and 0 <= value <= maximum else None


def project_task_state_event(value, expected_action):
    """Return the exact privacy projection or None; preserve no unknown data."""
    if (not isinstance(value, dict) or set(value) != TASK_STATE_FIELDS or
            value.get("version") != 1 or
            value.get("event") != "task-state-validation" or
            value.get("caller") != "server" or value.get("scope") != "all" or
            value.get("action") != expected_action or
            value.get("phase") not in ("pre", "post") or
            value.get("transition") is not None or
            value.get("observedState") not in
            ("absent", "backlog", "pending", "todo", "done", "corrupt", None) or
            value.get("expectedState") not in
            ("absent", "backlog", "pending", "todo", "done", "corrupt", None) or
            value.get("result") not in ("valid", "invalid") or
            type(value.get("ok")) is not bool or
            type(value.get("overallOk")) is not bool or
            type(value.get("slow")) is not bool or
            type(value.get("findingsTruncated")) is not bool):
        return None
    snapshot = value.get("snapshotHash")
    if snapshot is not None and (not isinstance(snapshot, str) or
            re.fullmatch(r"sha256:[a-f0-9]{64}", snapshot) is None):
        return None
    duration = bounded_int(value.get("durationMs"), 3_600_000)
    threshold = bounded_int(value.get("slowThresholdMs"), 60_000)
    reads = bounded_int(value.get("taskBodyReads"), 10_000_000)
    if duration is None or threshold is None or reads is None or value.get("slow") != (duration >= threshold):
        return None
    scan_mode = value.get("scanMode")
    if scan_mode not in (None, "full", "stem-closure"):
        return None
    architecture = value.get("architectureStatus")
    if architecture is not None and (not isinstance(architecture, str) or
            re.fullmatch(r"[a-z][a-z0-9-]{0,79}", architecture) is None):
        return None
    raw_findings = value.get("findings")
    if not isinstance(raw_findings, list) or len(raw_findings) > 100:
        return None
    findings = []
    for item in raw_findings:
        if (not isinstance(item, dict) or set(item) != {"code", "severity"} or
                not isinstance(item.get("code"), str) or
                re.fullmatch(r"[A-Za-z0-9_.:-]{1,80}", item["code"]) is None or
                item.get("severity") not in ("warning", "error", "blocker")):
            return None
        findings.append({"code": item["code"], "severity": item["severity"]})
    overall_ok = value["overallOk"]
    if value["result"] != ("valid" if overall_ok else "invalid"):
        return None
    return {
        "version": 1, "event": "task-state-validation", "caller": "server",
        "scope": "all", "action": expected_action, "transition": None,
        "phase": value["phase"], "observedState": value["observedState"],
        "expectedState": value["expectedState"], "snapshotHash": snapshot,
        "durationMs": duration, "slowThresholdMs": threshold,
        "slow": duration >= threshold, "scanMode": scan_mode,
        "taskBodyReads": reads, "architectureStatus": architecture,
        "findings": findings, "findingsTruncated": value["findingsTruncated"],
        "result": value["result"], "ok": value["ok"], "overallOk": overall_ok,
    }


def synthetic_task_state_events(action, count):
    phases = (["pre", "post"] if action == "index-check" else
              ["pre", "pre", "pre", "post", "post", "post"])
    return [{
        "version": 1, "event": "task-state-validation", "caller": "server",
        "scope": "all", "action": action, "transition": None,
        "phase": phases[index] if index < len(phases) else "post",
        "observedState": None, "expectedState": None, "snapshotHash": None,
        "durationMs": 0, "slowThresholdMs": 100, "slow": False,
        "scanMode": "full", "taskBodyReads": 0, "architectureStatus": None,
        "findings": [{"code": "INDEX_OBSERVATION_INVALID", "severity": "blocker"}],
        "findingsTruncated": False, "result": "invalid", "ok": False,
        "overallOk": False,
    } for index in range(count)]


def relay_task_state_events(stderr, action, expected_count=None):
    """Strictly parse and reproject child observations; discard other stderr."""
    projected = []
    malformed = False
    if not isinstance(stderr, str) or len(stderr.encode("utf-8", "replace")) > TASK_STATE_MAX_STDERR:
        malformed = True
    else:
        for line in stderr.splitlines():
            if not line.startswith(TASK_STATE_PREFIX):
                continue
            try:
                value = json.loads(line[len(TASK_STATE_PREFIX):])
            except (ValueError, UnicodeError, RecursionError):
                malformed = True
                continue
            event = project_task_state_event(value, action)
            if event is None:
                malformed = True
            else:
                projected.append(event)
    if expected_count is not None and (malformed or len(projected) != expected_count):
        projected = synthetic_task_state_events(action, expected_count)
    elif malformed:
        projected.append(synthetic_task_state_events(action, 1)[0])
    for event in projected:
        sys.stderr.write(TASK_STATE_PREFIX + json.dumps(
            event, separators=(",", ":"), ensure_ascii=True) + "\n")


def delegate(check_only=False):
    publisher = os.path.join(HERE, "task-index.mjs")
    if not os.path.isfile(publisher):
        sys.exit(f"regen-index.py: canonical publisher is missing: {publisher}")
    command = [os.environ.get("NODE", "node"), publisher]
    if check_only:
        command.append("--check")
    env = dict(os.environ)
    env["ORCHESTRATOR_PROJECT_ROOT"] = os.path.abspath(os.getcwd())
    env["ORCHESTRATOR_TASKS_DIR"] = os.path.abspath(BASE)
    env["ORCHESTRATOR_OUTCOME_SHAPE_PATH"] = OUTCOME_SHAPE_PATH
    env["ORCHESTRATOR_OUTCOME_SHAPE_AUTHORITY_ROOT"] = os.path.dirname(OUTCOME_SHAPE_PATH)
    try:
        completed = subprocess.run(
            command, env=env, check=False, stdout=subprocess.PIPE,
            stderr=subprocess.PIPE, text=True)
    except OSError as error:
        sys.exit(f"regen-index.py: cannot start canonical publisher: {error}")
    action = "index-check" if check_only else "index-publish"
    if completed.returncode != 0:
        relay_task_state_events(completed.stderr, action)
        sys.stderr.write(
            f"regen-index.py: canonical publisher failed (exit {completed.returncode})\n")
        raise SystemExit(completed.returncode)
    relay_task_state_events(completed.stderr, action, 2 if check_only else 6)
    if check_only and completed.stdout == "regen-index.py --check: INDEX.json is structurally fresh\n":
        sys.stdout.write(completed.stdout)


def main(argv=None):
    args = list(sys.argv[1:] if argv is None else argv)
    if args not in ([], ["--check"]):
        sys.exit("usage: python3 orchestrator/tasks/regen-index.py [--check]")
    delegate(args == ["--check"])


if __name__ == "__main__":
    main()
