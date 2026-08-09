#!/usr/bin/env python3
"""Observability harness for structured skill run records.

Parses a run-record (the structured trace a fresh-chat/shadow run emits) and
reports what the gates need to trust: which skill triggered (`entrypoint`), the
ordered files the agent read (`sourceReads`), the validators selected, and the
output contract emitted. A run-record that does not carry these markers is
reported as un-observed (not silently "triggered nothing").

Run-record JSON shape:
  {"triggered": true, "entrypoint": "ui-feature",
   "sourceReads": ["orchestrator/skills/ui-feature/SKILL.md", ...],
   "validators": [...], "outputContract": "builder-report"}

`observe(record)` returns (observed: bool, info: dict). `observed` is False when
the record lacks the trigger/read markers (the harness must not fabricate them).
"""
import json
import sys


def observe(record):
    if not isinstance(record, dict):
        return False, {"reason": "not an object"}
    triggered = record.get("triggered", False)
    entry = record.get("entrypoint")
    reads = record.get("sourceReads")
    if (
        not triggered
        or not entry
        or not isinstance(reads, list)
        or any(not isinstance(read, str) for read in reads)
    ):
        return False, {"reason": "no trigger/entrypoint/sourceReads markers", "triggered": triggered}
    return True, {
        "entrypoint": entry,
        "sourceReads": list(reads),
        "validators": record.get("validators", []),
        "outputContract": record.get("outputContract"),
    }


def main():
    if len(sys.argv) < 2:
        print("usage: observability-harness.py <run-record.json>", file=sys.stderr)
        return 2
    rec = json.load(open(sys.argv[1]))
    ok, info = observe(rec)
    print(json.dumps({"observed": ok, **info}))
    return 0


if __name__ == "__main__":
    sys.exit(main())
