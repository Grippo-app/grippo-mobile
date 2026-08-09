#!/usr/bin/env python3
"""Append one structured event to a task's pipeline journal.

The journal is the source-of-truth event stream behind Task Details → Activity:
one JSONL file per task stem at
``orchestrator/.cache/tasks/journal/<STEM>.jsonl``, keyed by stem so it survives
every allow-listed lifecycle transition owned by ``transition-task-state.mjs``
or ``finalize-task.mjs`` (the same stable-key trick as
``orchestrator/.cache/tasks/locks/<STEM>.json``). Gitignored runtime state — machine
local, dropped on a clean checkout. The PERMANENT digest of a shipped task
lives instead in the done file's ``## Outcome`` -> ``### Execution log``
subsection; this JSONL is the rich live/recent layer.

Invoked from the REPO ROOT (like ``regen-index.py``), by the pipeline agents:

  - task-orchestrator skill — one event per Step boundary + every BLOCKED /
                              retry / gate; ``ship`` at 6b/6d.
  - task-prep skill         — prep-start, question rounds, task-split,
                              promote, release.
  - task-orchestrator ``references/task-drop.md`` — emits NO event; the
                              canonical Drop transition detaches the task's
                              journal only after the impact fence passes. Drop
                              refuses an existing task lock rather than deleting it.
  - the ``figma:screens`` pull session — design-pulled / design-pull-failed.

Discipline: only the PARENT orchestrator / task-prep session emits events,
never the parallel-spawned sub-agents (the Step 1a / 3 / 4 batches), so appends
never interleave — the parent serialises naturally between phases. Callers
append ``|| true`` so a logging hiccup can never fail the surrounding step;
this script is also best-effort internally (see below).

CLI::

  python3 orchestrator/tasks/log-event.py <STEM> <KIND> \
      [--phase PHASE] [--status STATUS] [--column COLUMN] \
      [--duration-ms N] [--detail "one line"] [--meta KEY=VALUE ...]

Each line is one JSON object (compact, no spaces)::

  {"ts","stem","kind","phase"?,"status"?,"column"?,"durationMs"?,"detail"?,"meta"?}

  ts         ISO8601 UTC, stamped HERE — never the file mtime (git checkout
             resets mtimes; the journal needs real wall-clock).
  kind       phase-start | phase-end | stop | retry | gate | design-pulled
             | design-pull-failed | task-split | follow-up | note
  phase      lock | prep | intake | preflight | screen-preflight | planner
             | builders | diff-sanity | validators | assemble-gate
             | runtime-verify | screenshot-gate | review | security-review
             | ship | design-pull
  status     ok | blocked | escalate | fail | skipped | info
  column     backlog | pending | todo | done
  durationMs integer ms. Optional — the read side normally DERIVES it by
             pairing a phase-end with its phase-start, so the caller need not
             do timestamp arithmetic in bash; pass it only to override.
  detail     one short line (trimmed, <=200 chars; newlines collapsed).
  meta       bounded allow-listed k=v pairs. Reviewer events use reviewer,
             reviewAttempt, selectionReason and reasonCode. Shared task-detail
             evolution additionally permits checkpointId, reportId and
             retryPolicy. Existing blockType/children/gate/round/screens stay
             accepted by the current journal contract.

ENUM CONTRACT:
  ``task-journal-contract.cjs`` is the machine source of truth shared by the
  writer and ``orchestrator/site/server/tasks-log.js``. The sets below mirror it
  for argparse/help before delegation. The ``column`` values also match the band
  names in ``reconcileLifecycle`` (``server/timing.js``), and the phase labels
  are rendered by ``orchestrator/site/scripts/panels/board.js``.

Best-effort by design: an unknown enum/meta value or a filesystem refusal is
WARNED and the event is dropped (exit 0), because logging must never break the
pipeline. The append itself is delegated to ``task-journal.mjs``: that owner
validates the exact public event schema and performs a rooted, bounded,
no-symlink/no-hardlink append. The only hard failures are a non-canonical stem
and a malformed invocation (argparse) — both caller bugs, exit non-zero so they
surface in development.
"""
import argparse, datetime, json, os, re, subprocess, sys
from pathlib import Path

UTC = datetime.timezone.utc
DETAIL_MAX = 200
META_VALUE_MAX = 500
EVENT_MAX_BYTES = 2048

KINDS = {
    "phase-start", "phase-end", "stop", "retry", "gate",
    "design-pulled", "design-pull-failed", "task-split", "follow-up", "note",
}
PHASES = {
    "lock", "prep", "intake", "preflight", "screen-preflight", "planner",
    "builders", "diff-sanity", "validators", "assemble-gate",
    "runtime-verify", "screenshot-gate", "review", "security-review",
    "ship", "design-pull",
}
STATUSES = {"ok", "blocked", "escalate", "fail", "skipped", "info"}
COLUMNS = {"backlog", "pending", "todo", "done"}
META_KEYS = {
    "blockType", "checkpointId", "children", "gate", "reasonCode", "reportId",
    "retryPolicy", "reviewAttempt", "reviewer", "round", "screens",
    "selectionReason",
}

# Canonical stem shape — mirrors task-journal-contract.cjs. Non-canonical
# identity is a caller bug and is refused before delegation.
STEM_RE = re.compile(r"^TASK_\d+_[A-Za-z0-9_]+$")


def warn(msg: str) -> None:
    sys.stderr.write("log-event.py: " + msg + "\n")


def safe_stem(stem: str) -> bool:
    if not isinstance(stem, str) or len(stem) > 120 or not STEM_RE.fullmatch(stem):
        return False
    try:
        number = int(stem.split("_", 2)[1], 10)
    except (ValueError, IndexError):
        return False
    return 0 < number <= 9_007_199_254_740_991 and stem.split("_", 2)[1] == str(number)


def build_event(args) -> dict:
    event = {
        "ts": datetime.datetime.now(UTC).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "stem": args.stem,
        "kind": args.kind,
    }
    if args.phase is not None:
        event["phase"] = args.phase
    if args.status is not None:
        event["status"] = args.status
    if args.column is not None:
        event["column"] = args.column
    if args.duration_ms is not None and args.duration_ms >= 0:
        event["durationMs"] = args.duration_ms
    if args.detail is not None:
        detail = " ".join(args.detail.split())
        if len(detail) > DETAIL_MAX:
            detail = detail[:DETAIL_MAX - 1] + "…"
        if detail:
            event["detail"] = detail
    meta = {}
    for kv in args.meta:
        if "=" not in kv:
            warn(f"ignoring --meta {kv!r} (expected KEY=VALUE)")
            continue
        key, value = kv.split("=", 1)
        key = key.strip()
        value = " ".join(value.split())
        if key not in META_KEYS:
            warn(f"ignoring unsupported --meta key {key!r}")
            continue
        if not value or len(value) > META_VALUE_MAX:
            warn(f"ignoring invalid --meta value for {key!r}")
            continue
        meta[key] = value
    if meta:
        event["meta"] = meta
    return event


def main(argv) -> int:
    parser = argparse.ArgumentParser(
        prog="log-event.py",
        description="Append one event to a task's pipeline journal.",
    )
    parser.add_argument("stem", help="task stem, e.g. TASK_7_dark_mode_toggle")
    parser.add_argument("kind", help="event kind (%s)" % " | ".join(sorted(KINDS)))
    parser.add_argument("--phase")
    parser.add_argument("--status")
    parser.add_argument("--column")
    parser.add_argument("--duration-ms", type=int, dest="duration_ms")
    parser.add_argument("--detail")
    parser.add_argument("--meta", action="append", default=[], metavar="KEY=VALUE")
    args = parser.parse_args(argv)

    if not safe_stem(args.stem):
        warn(f"refusing non-canonical stem {args.stem!r}")
        return 2

    # Unknown enum values are never persisted into the browser-visible stream.
    invalid = False
    if args.kind not in KINDS:
        warn(f"unknown kind {args.kind!r} — event dropped")
        invalid = True
    if args.phase is not None and args.phase not in PHASES:
        warn(f"unknown phase {args.phase!r} — event dropped")
        invalid = True
    if args.status is not None and args.status not in STATUSES:
        warn(f"unknown status {args.status!r} — event dropped")
        invalid = True
    if args.column is not None and args.column not in COLUMNS:
        warn(f"unknown column {args.column!r} — event dropped")
        invalid = True
    if args.duration_ms is not None and not 0 <= args.duration_ms <= 86_400_000:
        warn("duration is outside the canonical one-day bound — event dropped")
        invalid = True
    if invalid:
        return 0

    line = json.dumps(build_event(args), ensure_ascii=False, separators=(",", ":")) + "\n"
    if len(line.encode("utf-8")) > EVENT_MAX_BYTES:
        warn("encoded event exceeds its byte limit — event dropped")
        return 0

    project_root = Path(os.environ.get("ORCHESTRATOR_PROJECT_ROOT") or Path(__file__).resolve().parents[2]).resolve()
    helper = Path(__file__).resolve().with_name("task-journal.mjs")
    node = os.environ.get("TASK_JOURNAL_NODE", "node")
    env = os.environ.copy()
    env.setdefault("ORCHESTRATOR_PROJECT_ROOT", str(project_root))
    try:
        result = subprocess.run(
            [node, str(helper), "append", "--stem", args.stem],
            input=line,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            cwd=str(project_root),
            env=env,
            timeout=15,
            check=False,
        )
    except (OSError, subprocess.SubprocessError) as exc:
        warn(f"append helper failed ({exc}) — event dropped")
        return 0
    if result.returncode != 0:
        detail = (result.stdout or result.stderr or "append refused").replace("\n", " ").strip()[:300]
        warn(f"append refused ({detail}) — event dropped")
        return 0
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
