#!/usr/bin/env python3
"""Crash-recoverable compare-and-swap edits for idle backlog tasks.

The edit marker is a write-ahead intent and a per-stem writer gate.  It is
durable before the task body can be replaced.  Recovery always completes the
recorded edit forward, then regenerates and verifies the canonical INDEX; it
never guesses from a half-published filesystem state.

Protocol:
  edit-backlog.py                 READY + one request/result JSON
  edit-backlog.py --recover-all   READY + one recovery summary JSON

Production invocations must carry the writer lease id published by the site in
EDIT_BACKLOG_OWN_WRITER_LEASE_ID.  Isolated fixture roots may opt out explicitly
with EDIT_BACKLOG_TEST_ALLOW_UNLEASED=1; that flag is inert in the checked-out
canonical workspace.
"""

from __future__ import annotations

import base64
import datetime as _datetime
import json
import os
import re
import runpy
import secrets
import socket
import stat
import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional

HERE = Path(__file__).resolve().parent
LIB = runpy.run_path(str(HERE / "create-backlog.py"), run_name="create_backlog_library")

CreateError = LIB["CreateError"]
KernelMutex = LIB["KernelMutex"]
DependencyGraphMutex = LIB["DependencyGraphMutex"]
PROJECT_ROOT = LIB["PROJECT_ROOT"]
TASKS_DIR = LIB["TASKS_DIR"]
CACHE_DIR = LIB["CACHE_DIR"]
MUTEX_PATH = LIB["MUTEX_PATH"]
TASK_MAX_BYTES = LIB["TASK_MAX_BYTES"]
MAX_REQUEST_BYTES = LIB["MAX_REQUEST_BYTES"]
MAX_SAFE_TASK_NUMBER = LIB["MAX_SAFE_TASK_NUMBER"]
STEM_RE = LIB["STEM_RE"]
HASH_RE = LIB["HASH_RE"]
bounded = LIB["bounded"]
ensure_real_dir = LIB["ensure_real_dir"]
read_regular = LIB["read_regular"]
publish_exclusive = LIB["publish_exclusive"]
run_regen = LIB["run_regen"]
run_task_state_validation = LIB["run_task_state_validation"]
read_index = LIB["read_index"]
index_locations = LIB["index_locations"]
read_all_creation_markers = LIB["read_all_markers_for_recovery"]
sha256 = LIB["sha256"]
now = LIB["now"]
fail = LIB["fail"]
process_start_identity = LIB["process_start_identity"]
bounded_directory_names = LIB["bounded_directory_names"]
cas_replace_bytes = LIB["cas_replace_bytes"]
recover_cas_operations = LIB["recover_cas_operations"]
fixture_mode_enabled = LIB["_fixture_mode_enabled"]
require_supported_task_platform = LIB["require_supported_task_platform"]
configured_dependency_graph_mutex_timeout = LIB["configured_dependency_graph_mutex_timeout"]

VERSION = 1
MAX_SAFE_INTEGER = 9_007_199_254_740_991
MARKER_MAX_BYTES = 160 * 1024
MARKER_CORPUS_MAX_BYTES = 8 * 1024 * 1024
WRITER_SCAN_MAX_BYTES = 1024 * 1024
WRITER_SCAN_TIMEOUT_SECONDS = 10
EDITS_DIR = Path(os.path.abspath(os.fspath(
    os.environ.get("EDIT_BACKLOG_EDITS_DIR", CACHE_DIR / "edits")
)))
FINALIZATIONS_DIR = Path(os.path.abspath(os.fspath(
    os.environ.get("EDIT_BACKLOG_FINALIZATIONS_DIR", CACHE_DIR / "finalizations")
)))
WRITER_SCAN_SCRIPT = Path(os.path.abspath(os.fspath(
    os.environ.get("EDIT_BACKLOG_WRITER_SCAN_SCRIPT", HERE / "writer-lease.mjs")
)))

TX_RE = re.compile(r"^[a-f0-9]{32}$")
TIMESTAMP_RE = re.compile(r"^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:\.[0-9]{1,6})?Z$")
MARKER_NAME_RE = re.compile(r"^(TASK_[0-9]+_[A-Za-z0-9_]+)\.json$")
LEASE_ID_RE = re.compile(r"^wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159}$")
PHASES = {
    "claimed", "writing-file", "file-published", "regenerating-index",
    "index-published", "verifying", "completed",
}
EFFECTS = {None, "changed", "unchanged", "aborted"}
MARKER_FIELDS = {
    "version", "transactionId", "stem", "expectedSourceHash",
    "requestedSourceHash", "recoveryMarkdownBase64", "status", "phase",
    "effect", "sourceHash", "createdAt", "updatedAt", "revision",
    "lastError",
}
FAILPOINTS = {"after-marker", "after-cas-manifest", "after-detach", "after-file", "after-index", "after-complete"}


def valid_stem(value: Any) -> bool:
    if not isinstance(value, str) or len(value) > 120:
        return False
    match = STEM_RE.fullmatch(value)
    if not match:
        return False
    number = int(match.group(1))
    return 1 <= number <= MAX_SAFE_TASK_NUMBER and match.group(1) == str(number)


def configured_mutex_timeout() -> int:
    raw = os.environ.get("CREATE_BACKLOG_MUTEX_TIMEOUT_MS", "30000")
    try:
        value = int(raw)
    except ValueError:
        fail("MUTEX_TIMEOUT_INVALID", "mutex timeout must be an integer", exit_code=2)
    return max(1, min(value, 600_000))


def request() -> dict:
    raw = sys.stdin.buffer.read(MAX_REQUEST_BYTES + 1)
    if len(raw) > MAX_REQUEST_BYTES:
        fail("REQUEST_TOO_LARGE", "edit request exceeds its byte limit", exit_code=2)
    if not raw.strip():
        fail("INVALID_JSON", "edit request is empty", exit_code=2)
    try:
        value = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail("INVALID_JSON", f"edit request is not valid JSON: {bounded(exc)}", exit_code=2)
    if not isinstance(value, dict) or set(value) != {"version", "stem", "expectedSourceHash", "markdown"}:
        fail("INVALID_REQUEST", "edit request must contain exactly version, stem, expectedSourceHash, markdown", exit_code=2)
    if type(value.get("version")) is not int or value.get("version") != VERSION:
        fail("INVALID_REQUEST", "version must be 1", exit_code=2)
    stem = value.get("stem")
    if not valid_stem(stem):
        fail("STEM_INVALID", "stem is not canonical", exit_code=2)
    expected = value.get("expectedSourceHash")
    if not isinstance(expected, str) or not HASH_RE.fullmatch(expected):
        fail("SOURCE_HASH_INVALID", "expectedSourceHash is invalid", exit_code=2)
    markdown = value.get("markdown")
    if not isinstance(markdown, str):
        fail("MARKDOWN_INVALID", "markdown must be a string", exit_code=2)
    try:
        data = markdown.replace("\r\n", "\n").replace("\r", "\n").encode("utf-8")
    except UnicodeEncodeError:
        fail("MARKDOWN_INVALID", "markdown contains an unpaired Unicode surrogate", exit_code=2)
    if not data or b"\x00" in data:
        fail("MARKDOWN_INVALID", "markdown is empty or contains NUL", exit_code=2)
    if not data.endswith(b"\n"):
        data += b"\n"
    if len(data) > TASK_MAX_BYTES:
        fail("MARKDOWN_INVALID", "canonical markdown exceeds its byte limit", exit_code=2)
    validate_heading(stem, data, code="MARKDOWN_HEADING_INVALID", exit_code=2)
    return {"stem": stem, "expected": expected, "data": data}


def validate_heading(stem: str, data: bytes, *, code: str, exit_code: int = 1) -> str:
    try:
        first = data.decode("utf-8").split("\n", 1)[0]
    except (UnicodeDecodeError, IndexError):
        fail(code, "task markdown must be valid UTF-8 with a heading", exit_code=exit_code, recoverable=exit_code != 2)
    # Keep this acceptance boundary identical to the canonical INDEX title.
    # Once an edit marker is durable, every accepted body
    # must be guaranteed to produce the canonical non-empty INDEX title; a
    # merely "readable" variant such as `#TASK` would otherwise fail only
    # after replacing the task and leave recovery wedged on the same body.
    heading = re.fullmatch(r"# TASK ([0-9]+) — (\S(?:[^\r\n]*\S)?)", first)
    match = STEM_RE.fullmatch(stem)
    number = int(match.group(1)) if match else -1
    if not heading or heading.group(1) != str(number):
        fail(code, f"first line must keep # TASK {number} — <title>", exit_code=exit_code, recoverable=exit_code != 2)
    title = heading.group(2)
    if any(
        ord(char) < 32 or 0x7F <= ord(char) <= 0x9F or ord(char) in (0x2028, 0x2029) or
        0x202A <= ord(char) <= 0x202E or 0x2066 <= ord(char) <= 0x2069 or ord(char) == 0xFEFF
        for char in title
    ):
        fail(code, "task heading title contains a forbidden control or line-separator character",
             exit_code=exit_code, recoverable=exit_code != 2)
    return title


def marker_path(stem: str) -> Path:
    if not valid_stem(stem):
        fail("EDIT_MARKER_INVALID", "edit marker stem is invalid", recoverable=True)
    return EDITS_DIR / f"{stem}.json"


def decode_recovery(value: Any) -> bytes:
    if not isinstance(value, str) or len(value) > 4 * ((TASK_MAX_BYTES + 2) // 3):
        fail("EDIT_MARKER_INVALID", "edit marker recovery body is invalid", recoverable=True)
    try:
        raw = base64.b64decode(value.encode("ascii"), validate=True)
    except (UnicodeEncodeError, ValueError):
        fail("EDIT_MARKER_INVALID", "edit marker recovery body is not canonical base64", recoverable=True)
    if base64.b64encode(raw).decode("ascii") != value or not raw or len(raw) > TASK_MAX_BYTES or b"\x00" in raw:
        fail("EDIT_MARKER_INVALID", "edit marker recovery body is unsafe", recoverable=True)
    if not raw.endswith(b"\n") or b"\r" in raw:
        fail("EDIT_MARKER_INVALID", "edit marker recovery body is not canonical Markdown", recoverable=True)
    return raw


def valid_marker_time(value: Any) -> bool:
    if not isinstance(value, str) or not TIMESTAMP_RE.fullmatch(value):
        return False
    try:
        _datetime.datetime.fromisoformat(value.removesuffix("Z") + "+00:00")
        return True
    except ValueError:
        return False


def valid_unicode_scalar_string(value: Any, *, max_chars: int, nonempty: bool) -> bool:
    if not isinstance(value, str) or len(value) > max_chars or (nonempty and not value):
        return False
    try:
        value.encode("utf-8")
        return True
    except UnicodeEncodeError:
        return False


def validate_marker(value: Any, *, expected_stem: str) -> dict:
    if not isinstance(value, dict) or set(value) != MARKER_FIELDS or \
            type(value.get("version")) is not int or value.get("version") != VERSION:
        fail("EDIT_MARKER_INVALID", "edit marker fields do not match the exact v1 contract", recoverable=True)
    if value.get("stem") != expected_stem or not valid_stem(expected_stem):
        fail("EDIT_MARKER_INVALID", "edit marker stem does not match its filename", recoverable=True)
    if not isinstance(value.get("transactionId"), str) or not TX_RE.fullmatch(value["transactionId"]):
        fail("EDIT_MARKER_INVALID", "edit marker transaction id is invalid", recoverable=True)
    for field in ("expectedSourceHash", "requestedSourceHash"):
        if not isinstance(value.get(field), str) or not HASH_RE.fullmatch(value[field]):
            fail("EDIT_MARKER_INVALID", f"edit marker {field} is invalid", recoverable=True)
    if value.get("sourceHash") is not None and (
        not isinstance(value.get("sourceHash"), str) or not HASH_RE.fullmatch(value["sourceHash"])
    ):
        fail("EDIT_MARKER_INVALID", "edit marker sourceHash is invalid", recoverable=True)
    if value.get("status") not in ("incomplete", "completed") or value.get("phase") not in PHASES:
        fail("EDIT_MARKER_INVALID", "edit marker state is invalid", recoverable=True)
    if (value.get("status") == "completed") != (value.get("phase") == "completed"):
        fail("EDIT_MARKER_INVALID", "edit marker status/phase are inconsistent", recoverable=True)
    if value.get("effect") not in EFFECTS:
        fail("EDIT_MARKER_INVALID", "edit marker effect is invalid", recoverable=True)
    if type(value.get("revision")) is not int or value.get("revision") < 1 or value.get("revision") > MAX_SAFE_INTEGER:
        fail("EDIT_MARKER_INVALID", "edit marker revision is invalid", recoverable=True)
    if not valid_marker_time(value.get("createdAt")) or not valid_marker_time(value.get("updatedAt")):
        fail("EDIT_MARKER_INVALID", "edit marker timestamps are invalid", recoverable=True)
    if _datetime.datetime.fromisoformat(value["updatedAt"].removesuffix("Z") + "+00:00") < \
            _datetime.datetime.fromisoformat(value["createdAt"].removesuffix("Z") + "+00:00"):
        fail("EDIT_MARKER_INVALID", "edit marker timestamps are out of order", recoverable=True)
    last_error = value.get("lastError")
    if last_error is not None and (
        not isinstance(last_error, dict) or set(last_error) != {"code", "message", "at"} or
        not valid_unicode_scalar_string(last_error.get("code"), max_chars=120, nonempty=True) or
        not valid_unicode_scalar_string(last_error.get("message"), max_chars=1200, nonempty=False) or
        not valid_marker_time(last_error.get("at"))
    ):
        fail("EDIT_MARKER_INVALID", "edit marker lastError is invalid", recoverable=True)
    if last_error is not None:
        created_key = _datetime.datetime.fromisoformat(value["createdAt"].removesuffix("Z") + "+00:00")
        updated_key = _datetime.datetime.fromisoformat(value["updatedAt"].removesuffix("Z") + "+00:00")
        error_key = _datetime.datetime.fromisoformat(last_error["at"].removesuffix("Z") + "+00:00")
        if error_key < created_key or error_key > updated_key:
            fail("EDIT_MARKER_INVALID", "edit marker lastError timestamp is out of bounds", recoverable=True)

    if value["status"] == "incomplete":
        if value.get("effect") is not None or value.get("sourceHash") is not None:
            fail("EDIT_MARKER_INVALID", "incomplete edit marker has a final effect", recoverable=True)
        data = decode_recovery(value.get("recoveryMarkdownBase64"))
        validate_heading(expected_stem, data, code="EDIT_MARKER_INVALID")
        if sha256(data) != value["requestedSourceHash"]:
            fail("EDIT_MARKER_INVALID", "edit recovery body does not match requestedSourceHash", recoverable=True)
    else:
        if value["revision"] < 2:
            fail("EDIT_MARKER_INVALID", "completed edit marker revision is invalid", recoverable=True)
        if value.get("recoveryMarkdownBase64") is not None:
            fail("EDIT_MARKER_INVALID", "completed edit marker retained its recovery body", recoverable=True)
        effect = value.get("effect")
        if effect not in ("changed", "unchanged", "aborted"):
            fail("EDIT_MARKER_INVALID", "completed edit marker has no final effect", recoverable=True)
        if effect == "changed" and (
            value.get("sourceHash") != value["requestedSourceHash"] or
            value["expectedSourceHash"] == value["requestedSourceHash"] or last_error is not None
        ):
            fail("EDIT_MARKER_INVALID", "changed edit receipt is inconsistent", recoverable=True)
        if effect == "unchanged" and not (
            value["expectedSourceHash"] == value["requestedSourceHash"] == value.get("sourceHash") and
            last_error is None
        ):
            fail("EDIT_MARKER_INVALID", "unchanged edit receipt hashes are inconsistent", recoverable=True)
        if effect == "aborted" and (value.get("sourceHash") is not None or last_error is None):
            fail("EDIT_MARKER_INVALID", "aborted edit receipt is inconsistent", recoverable=True)
    return value


def marker_bytes(marker: dict) -> bytes:
    data = json.dumps(marker, ensure_ascii=False, indent=2, sort_keys=True).encode("utf-8") + b"\n"
    if len(data) > MARKER_MAX_BYTES:
        fail("EDIT_MARKER_TOO_LARGE", "edit marker exceeds its byte limit", recoverable=True)
    return data


def read_marker(path: Path, expected_stem: str) -> dict:
    raw = read_regular(path, max_bytes=MARKER_MAX_BYTES, code="EDIT_MARKER_INVALID")
    try:
        parsed = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail("EDIT_MARKER_INVALID", f"edit marker is not valid UTF-8 JSON: {bounded(exc)}", recoverable=True)
    marker = validate_marker(parsed, expected_stem=expected_stem)
    if raw != marker_bytes(marker):
        fail("EDIT_MARKER_INVALID", "edit marker JSON is not in its canonical serialized form", recoverable=True)
    return marker


def write_marker(marker: dict, *, replacing_transaction: bool = False) -> None:
    ensure_real_dir(EDITS_DIR, create=True, code="EDIT_MARKER_DIR_UNSAFE")
    path = marker_path(marker["stem"])
    existing_raw = read_regular(path, max_bytes=MARKER_MAX_BYTES,
                                code="EDIT_MARKER_INVALID", required=False)
    if existing_raw is None:
        if not replacing_transaction or marker.get("revision") != 0:
            fail("EDIT_MARKER_OWNERSHIP_CHANGED", "owned edit marker vanished", recoverable=True)
        marker["revision"] = 1
        marker["updatedAt"] = now()
        validate_marker(marker, expected_stem=marker["stem"])
        try:
            publish_exclusive(path, marker_bytes(marker))
        except FileExistsError:
            fail("EDIT_MARKER_OWNERSHIP_CHANGED",
                 "edit marker name was claimed by another transaction", recoverable=True)
        return
    current = read_marker(path, marker["stem"])
    if replacing_transaction:
        if current.get("status") != "completed":
            fail("EDIT_RECOVERY_REQUIRED", "an incomplete edit must be recovered before a new edit", recoverable=True)
        if marker.get("revision") != 0:
            fail("EDIT_MARKER_GENERATION_CHANGED", "new edit marker has an unexpected revision", recoverable=True)
        marker["revision"] = 1
    else:
        if current.get("transactionId") != marker.get("transactionId"):
            fail("EDIT_MARKER_OWNERSHIP_CHANGED", "edit marker belongs to another transaction", recoverable=True)
        if current.get("revision") != marker.get("revision"):
            fail("EDIT_MARKER_GENERATION_CHANGED", "edit marker revision advanced unexpectedly", recoverable=True)
        marker["revision"] = int(marker.get("revision", 0)) + 1
    marker["updatedAt"] = now()
    validate_marker(marker, expected_stem=marker["stem"])
    cas_replace_bytes(
        path, marker_bytes(current), marker_bytes(marker), max_bytes=MARKER_MAX_BYTES,
        owner="edit-marker:" + marker["transactionId"],
        test_swap_variable="EDIT_BACKLOG_TEST_SWAP_MARKER_BEFORE_DETACH")


def read_all_markers() -> List[dict]:
    ensure_real_dir(EDITS_DIR, create=True, code="EDIT_MARKER_DIR_UNSAFE")
    names = bounded_directory_names(
        EDITS_DIR, code="EDIT_RECOVERY_SCAN_FAILED", recoverable=True
    )
    markers: List[dict] = []
    total_bytes = 0
    for name in names:
        if name.startswith("."):
            continue
        match = MARKER_NAME_RE.fullmatch(name)
        if not match:
            if name.endswith(".json"):
                fail("EDIT_RECOVERY_SCAN_FAILED", f"unsafe edit marker filename: {bounded(name, 180)}", recoverable=True)
            continue
        try:
            marker = read_marker(EDITS_DIR / name, match.group(1))
            total_bytes += len(marker_bytes(marker))
            if total_bytes > MARKER_CORPUS_MAX_BYTES:
                fail("EDIT_RECOVERY_SCAN_FAILED",
                     "edit marker corpus exceeds its total byte bound", recoverable=True)
            markers.append(marker)
        except CreateError as exc:
            fail("EDIT_RECOVERY_SCAN_FAILED", f"edit marker {name} is unsafe: {bounded(exc)}", recoverable=True)
    transactions = [marker["transactionId"] for marker in markers]
    if len(set(transactions)) != len(transactions):
        fail("EDIT_RECOVERY_SCAN_FAILED", "edit marker transaction ids are not unique", recoverable=True)
    return markers


def ensure_no_incomplete_creation() -> None:
    markers = read_all_creation_markers()
    if any(marker.get("status") == "incomplete" for marker in markers):
        fail("CREATION_INCOMPLETE", "recover deterministic task creation before editing", recoverable=True)


def locations(stem: str) -> List[str]:
    specs = [
        ("backlog", TASKS_DIR / "backlog" / f"{stem}.md"),
        ("pending", TASKS_DIR / "pending" / f"{stem}.questions.md"),
        ("todo", TASKS_DIR / "todo" / f"{stem}.md"),
        ("done", TASKS_DIR / "done" / f"{stem}.md"),
    ]
    found: List[str] = []
    for column, item in specs:
        if read_regular(item, max_bytes=TASK_MAX_BYTES,
                        code="TASK_STATE_UNSAFE", required=False) is not None:
            found.append(column)
    return found


def maybe_failpoint(name: str) -> None:
    configured = os.environ.get("EDIT_BACKLOG_FAILPOINT", "")
    if configured and configured not in FAILPOINTS:
        fail("FAILPOINT_INVALID", f"unknown EDIT_BACKLOG_FAILPOINT {configured!r}", exit_code=2)
    if configured == name:
        sys.stdout.flush()
        sys.stderr.flush()
        os._exit(87)


def writer_scan(expected_stem: Optional[str]) -> dict:
    own = os.environ.get("EDIT_BACKLOG_OWN_WRITER_LEASE_ID", "")
    # A production controller always supplies a lease id.  Even if a developer
    # accidentally exported the test knob in their shell, presence of that id
    # forces the authenticated scan; the knob only supports direct isolated
    # helper tests that have no controller at all.
    if fixture_mode_enabled("EDIT_BACKLOG_TEST_ALLOW_UNLEASED") and not own:
        return {"active": [], "stale": [], "issues": [], "testUnleased": True}
    if not LEASE_ID_RE.fullmatch(own):
        fail("EDIT_WRITER_LEASE_MISSING", "edit helper has no authenticated writer lease", recoverable=True)
    ensure_real_dir(FINALIZATIONS_DIR, create=False, code="EDIT_WRITER_LEASE_UNSAFE")
    read_regular(WRITER_SCAN_SCRIPT, max_bytes=256 * 1024, code="EDIT_WRITER_SCAN_FAILED")
    node = os.environ.get("EDIT_BACKLOG_NODE", "node")
    child_env = dict(os.environ)
    child_env["FINALIZE_PROJECT_ROOT"] = str(PROJECT_ROOT)
    child_env["FINALIZE_STATE_DIR"] = str(FINALIZATIONS_DIR)
    try:
        result = subprocess.run(
            [node, str(WRITER_SCAN_SCRIPT), "scan"], cwd=str(PROJECT_ROOT), env=child_env,
            stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            timeout=WRITER_SCAN_TIMEOUT_SECONDS, check=False,
        )
    except (OSError, subprocess.SubprocessError) as exc:
        fail("EDIT_WRITER_SCAN_FAILED", f"cannot inspect writer leases: {bounded(exc)}", recoverable=True)
    if len(result.stdout) > WRITER_SCAN_MAX_BYTES or len(result.stderr) > 16 * 1024 or result.returncode != 0:
        fail("EDIT_WRITER_SCAN_FAILED", "writer lease scan failed or exceeded its output bound", recoverable=True)
    try:
        value = json.loads(result.stdout.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail("EDIT_WRITER_SCAN_FAILED", f"writer lease scan returned invalid JSON: {bounded(exc)}", recoverable=True)
    if not isinstance(value, dict) or set(value) != {"active", "stale", "issues"} or not all(
        isinstance(value.get(key), list) for key in ("active", "stale", "issues")
    ):
        fail("EDIT_WRITER_SCAN_FAILED", "writer lease scan returned an invalid contract", recoverable=True)
    if value["issues"]:
        issue = value["issues"][0]
        detail = issue.get("message") if isinstance(issue, dict) else issue
        fail("EDIT_WRITER_LEASE_UNSAFE", f"workspace writer lease state is unsafe: {bounded(detail)}", recoverable=True)
    own_rows = [row for row in value["active"] if isinstance(row, dict) and row.get("leaseId") == own]
    if len(own_rows) != 1:
        fail("EDIT_WRITER_LEASE_MISSING", "authenticated edit writer lease is not active exactly once", recoverable=True)
    row = own_rows[0]
    current_process_start_id = process_start_identity(os.getpid())
    if sys.platform in ("linux", "darwin", "win32") and current_process_start_id is None:
        fail("EDIT_WRITER_LEASE_UNSAFE",
             "cannot prove this helper process generation", recoverable=True)
    expected_key = "task:recover-backlog-edits" if expected_stem is None else f"task:edit-backlog:{expected_stem}"
    if (
        row.get("kind") != "workspace-session" or row.get("childPid") != os.getpid() or
        row.get("childProcessStartId") != current_process_start_id or
        row.get("unverified") is not False or row.get("stem") != expected_stem or
        row.get("key") != expected_key or row.get("expiresAt") is not None or
        not isinstance(row.get("owner"), dict) or row["owner"].get("hostname") != socket.gethostname()
    ):
        fail("EDIT_WRITER_LEASE_INVALID", "edit writer lease is not attached to this helper", recoverable=True)
    foreign = [record for record in value["active"] if not isinstance(record, dict) or record.get("leaseId") != own]
    if foreign:
        label = foreign[0].get("stem") if isinstance(foreign[0], dict) else None
        fail("WORKSPACE_WRITER_ACTIVE", "another workspace writer is active" + (f" for {label}" if label else ""), recoverable=True)
    return value


def edit_runtime_authority(writer_state: dict, *, expected_stem: Optional[str]) -> dict:
    publication_key = ("task:recover-backlog-edits" if expected_stem is None else
                       f"task:edit-backlog:{expected_stem}")
    return {
        "mode": "fixture-unleased" if writer_state.get("testUnleased") is True else "owned-lease",
        "authorityLeaseId": None if writer_state.get("testUnleased") is True else
                            os.environ.get("EDIT_BACKLOG_OWN_WRITER_LEASE_ID"),
        "publicationGuardLeaseId": None,
        "publicationKey": publication_key,
    }


def edit_active_runtime(marker: dict, authority: dict) -> dict:
    return {
        "kind": "edit", "path": marker_path(marker["stem"]), "marker": marker,
        "mode": authority["mode"], "authorityLeaseId": authority["authorityLeaseId"],
        "publicationGuardLeaseId": None, "publicationKey": authority["publicationKey"],
    }


def new_marker(req: dict) -> dict:
    created = now()
    return {
        "version": VERSION,
        "transactionId": secrets.token_hex(16),
        "stem": req["stem"],
        "expectedSourceHash": req["expected"],
        "requestedSourceHash": sha256(req["data"]),
        "recoveryMarkdownBase64": base64.b64encode(req["data"]).decode("ascii"),
        "status": "incomplete",
        "phase": "claimed",
        "effect": None,
        "sourceHash": None,
        "createdAt": created,
        "updatedAt": created,
        "revision": 0,
        "lastError": None,
    }


def complete(marker: dict, effect: str, source_hash: Optional[str]) -> None:
    marker.update({
        "status": "completed", "phase": "completed", "effect": effect,
        "sourceHash": source_hash, "recoveryMarkdownBase64": None,
        "lastError": None,
    })
    write_marker(marker)


def abort_before_mutation(marker: dict, exc: CreateError) -> None:
    marker["lastError"] = {"code": exc.code, "message": bounded(exc), "at": now()}
    marker.update({
        "status": "completed", "phase": "completed", "effect": "aborted",
        "sourceHash": None, "recoveryMarkdownBase64": None,
    })
    write_marker(marker)


def verify_result(marker: dict, data: bytes) -> dict:
    target = TASKS_DIR / "backlog" / f"{marker['stem']}.md"
    live = read_regular(target, max_bytes=TASK_MAX_BYTES, code="EDIT_VERIFY_FAILED")
    if live != data or sha256(live) != marker["requestedSourceHash"]:
        fail("EDIT_VERIFY_FAILED", "saved task bytes do not match the durable edit intent", recoverable=True)
    index = read_index()
    found = index_locations(index, marker["stem"])
    if len(found) != 1 or found[0][0] != "backlog":
        fail("EDIT_INDEX_VERIFY_FAILED", "edited task must appear exactly once in INDEX backlog", recoverable=True)
    expected_title = validate_heading(marker["stem"], data, code="EDIT_MARKER_INVALID")
    if found[0][1].get("title") != expected_title:
        fail("EDIT_INDEX_VERIFY_FAILED", "INDEX title does not match edited Markdown", recoverable=True)
    return found[0][1]


def resume(marker: dict, *, recovered: bool, runtime_authority: dict) -> dict:
    data = decode_recovery(marker["recoveryMarkdownBase64"])
    target = TASKS_DIR / "backlog" / f"{marker['stem']}.md"
    try:
        if locations(marker["stem"]) != ["backlog"]:
            fail("TASK_NOT_IDLE_BACKLOG", "task left idle backlog while an edit was incomplete", recoverable=True)
        live = read_regular(target, max_bytes=TASK_MAX_BYTES, code="TASK_STATE_UNSAFE")
        live_hash = sha256(live)
        if live_hash not in (marker["expectedSourceHash"], marker["requestedSourceHash"]):
            fail("EDIT_TARGET_DIVERGED", "task bytes match neither the old nor requested edit hash", recoverable=True)

        effect = "unchanged" if marker["expectedSourceHash"] == marker["requestedSourceHash"] else "changed"
        if live_hash == marker["expectedSourceHash"] and live != data:
            marker["phase"] = "writing-file"
            write_marker(marker)
            cas_replace_bytes(
                target, live, data, max_bytes=TASK_MAX_BYTES,
                owner="edit-source:" + marker["transactionId"],
                test_swap_variable="EDIT_BACKLOG_TEST_SWAP_SOURCE_BEFORE_DETACH",
                test_after_detach_variable="EDIT_BACKLOG_TEST_CLAIM_TARGET_AFTER_DETACH",
                detach_failpoint="EDIT_BACKLOG_FAILPOINT", detach_exit_code=87,
                stage_failpoint="EDIT_BACKLOG_FAILPOINT")
            live = read_regular(target, max_bytes=TASK_MAX_BYTES, code="EDIT_VERIFY_FAILED")
        if live != data:
            fail("EDIT_TARGET_DIVERGED", "task hash collision or non-canonical bytes prevent recovery", recoverable=True)
        marker["phase"] = "file-published"
        write_marker(marker)
        maybe_failpoint("after-file")

        run_task_state_validation(
            stem=marker["stem"], expect="backlog", check_index=False,
            code="TASK_STATE_EDIT_POSTCONDITION_FAILED",
            active_runtime=edit_active_runtime(marker, runtime_authority),
            observation_action="edit")

        marker["phase"] = "regenerating-index"
        write_marker(marker)
        run_regen(False)
        marker["phase"] = "index-published"
        write_marker(marker)
        maybe_failpoint("after-index")

        marker["phase"] = "verifying"
        write_marker(marker)
        run_regen(True)
        run_task_state_validation(
            stem=marker["stem"], expect="backlog", check_index=True,
            code="TASK_STATE_EDIT_INDEX_POSTCONDITION_FAILED",
            active_runtime=edit_active_runtime(marker, runtime_authority),
            observation_action="edit")
        entry = verify_result(marker, data)
        complete(marker, effect, marker["requestedSourceHash"])
        maybe_failpoint("after-complete")
        return {
            "ok": True, "changed": effect == "changed", "stem": marker["stem"],
            "column": "backlog", "previousSourceHash": marker["expectedSourceHash"],
            "sourceHash": marker["requestedSourceHash"], "transactionId": marker["transactionId"],
            "recovered": recovered, "task": entry,
        }
    except CreateError as exc:
        if marker.get("status") != "completed":
            try:
                marker["status"] = "incomplete"
                marker["lastError"] = {"code": exc.code, "message": bounded(exc), "at": now()}
                write_marker(marker)
            except Exception:
                pass
        exc.recoverable = True
        exc.details.setdefault("stem", marker.get("stem"))
        exc.details.setdefault("transactionId", marker.get("transactionId"))
        raise


def ensure_roots() -> None:
    ensure_real_dir(TASKS_DIR, create=False)
    for name in ("backlog", "pending", "todo", "done"):
        ensure_real_dir(TASKS_DIR / name, create=False)
    ensure_real_dir(CACHE_DIR, create=True)
    ensure_real_dir(EDITS_DIR, create=True, code="EDIT_MARKER_DIR_UNSAFE")


def publish_graph_locked(req: dict, runtime_authority: dict) -> dict:
    # A prior crashed CAS belongs to recover-all authority, not to a new
    # per-stem edit request. Validate/fail before any new marker claim.
    recover_cas_operations(EDITS_DIR, allowed_targets=set())
    recover_cas_operations(TASKS_DIR / "backlog", allowed_targets=set())
    ensure_no_incomplete_creation()
    markers = read_all_markers()
    incomplete = [marker for marker in markers if marker["status"] == "incomplete"]
    if incomplete:
        fail("EDIT_RECOVERY_REQUIRED", "recover incomplete backlog edits before starting a new edit", recoverable=True,
             details={"stems": [marker["stem"] for marker in incomplete]})
    if locations(req["stem"]) != ["backlog"]:
        fail("TASK_NOT_IDLE_BACKLOG", "task must exist only as an idle backlog body")
    target = TASKS_DIR / "backlog" / f"{req['stem']}.md"
    old = read_regular(target, max_bytes=TASK_MAX_BYTES, code="TASK_STATE_UNSAFE")
    old_hash = sha256(old)
    if old_hash != req["expected"]:
        fail("SOURCE_CHANGED", "task changed since it was loaded", exit_code=2)

    marker = new_marker(req)
    write_marker(marker, replacing_transaction=True)
    maybe_failpoint("after-marker")
    try:
        run_task_state_validation(
            stem=req["stem"], expect="backlog", check_index=True,
            code="TASK_STATE_EDIT_PRECONDITION_FAILED",
            active_runtime=edit_active_runtime(marker, runtime_authority),
            observation_action="edit")
        run_task_state_validation(
            stem=req["stem"], expect="backlog", check_index=False,
            code="TASK_STATE_EDIT_PROPOSAL_INVALID",
            proposal=req["data"], proposal_state="backlog",
            active_runtime=edit_active_runtime(marker, runtime_authority),
            observation_action="edit")
    except CreateError as exc:
        try:
            abort_before_mutation(marker, exc)
        except CreateError:
            pass
        raise
    return resume(marker, recovered=False, runtime_authority=runtime_authority)


def publish(req: dict) -> dict:
    ensure_roots()
    with KernelMutex(MUTEX_PATH, configured_mutex_timeout()):
        writer_state = writer_scan(req["stem"])
        runtime_authority = edit_runtime_authority(
            writer_state, expected_stem=req["stem"])
        invocation = "dependency-graph:edit:" + secrets.token_hex(12)
        with DependencyGraphMutex(
                FINALIZATIONS_DIR / ".mutex.json",
                configured_dependency_graph_mutex_timeout(), invocation):
            return publish_graph_locked(req, runtime_authority)


def recover_all_graph_locked(runtime_authority: dict) -> dict:
    recover_cas_operations(EDITS_DIR)
    recover_cas_operations(TASKS_DIR / "backlog")
    ensure_no_incomplete_creation()
    markers = read_all_markers()
    incomplete = [marker for marker in markers if marker["status"] == "incomplete"]
    recovered: List[dict] = []
    for marker in incomplete:
        result = resume(marker, recovered=True,
                        runtime_authority=runtime_authority)
        recovered.append({
            "stem": result["stem"], "sourceHash": result["sourceHash"],
            "changed": result["changed"], "transactionId": result["transactionId"],
        })
    return {
        "ok": True, "mode": "recover-all", "scanned": len(markers),
        "alreadyCompleted": len(markers) - len(incomplete),
        "recoveredCount": len(recovered), "recovered": recovered,
    }


def recover_all() -> dict:
    ensure_roots()
    with KernelMutex(MUTEX_PATH, configured_mutex_timeout()):
        writer_state = writer_scan(None)
        runtime_authority = edit_runtime_authority(
            writer_state, expected_stem=None)
        invocation = "dependency-graph:edit-recovery:" + secrets.token_hex(12)
        with DependencyGraphMutex(
                FINALIZATIONS_DIR / ".mutex.json",
                configured_dependency_graph_mutex_timeout(), invocation):
            return recover_all_graph_locked(runtime_authority)


def await_recovery_readiness() -> None:
    """Fence recovery behind the controller's durable child-lease binding.

    EOF is the only valid control message.  A bounded one-byte read blocks an
    early scan, accepts an empty closed pipe, and rejects every non-empty
    control stream before mutex acquisition or filesystem mutation.
    """
    if sys.stdin.buffer.read(1):
        fail(
            "RECOVERY_READINESS_INVALID",
            "--recover-all readiness stdin must close without data",
            exit_code=2,
        )


def emit(value: dict) -> None:
    sys.stdout.write(json.dumps(value, ensure_ascii=False, separators=(",", ":")) + "\n")
    sys.stdout.flush()


def main() -> int:
    sys.stdout.write("READY\n")
    sys.stdout.flush()
    try:
        require_supported_task_platform()
        if sys.argv[1:] == ["--recover-all"]:
            await_recovery_readiness()
            emit(recover_all())
        elif not sys.argv[1:]:
            emit(publish(request()))
        else:
            fail("INVALID_ARGUMENTS", "usage: edit-backlog.py [--recover-all]", exit_code=2)
        return 0
    except CreateError as exc:
        result: Dict[str, Any] = {
            "ok": False,
            "error": {"code": exc.code, "message": bounded(exc)},
            "recoverable": bool(exc.recoverable),
        }
        if exc.details:
            result["details"] = exc.details
        emit(result)
        return exc.exit_code
    except BaseException as exc:
        emit({
            "ok": False,
            "error": {"code": "EDIT_INTERNAL_ERROR", "message": bounded(f"{type(exc).__name__}: {exc}")},
            "recoverable": False,
        })
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
