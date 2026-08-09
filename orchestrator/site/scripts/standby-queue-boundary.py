#!/usr/bin/env python3
"""Descriptor-pinned transaction boundary for the standby request drainer.

The public CLI validates queue records with the server's canonical JavaScript
contract.  This helper owns only the filesystem transaction which Node cannot
express safely across two directories.  Input and output are one bounded JSON
object; importing this module is inert.
"""

from __future__ import annotations

import ctypes
import errno
import hashlib
import json
import os
import re
import stat
import sys
import tempfile
from datetime import datetime, timezone


INPUT_MAX = 1024 * 1024
OUTPUT_MAX = 2 * 1024 * 1024
REQUEST_MAX = 256 * 1024
DIRECTORY_MAX = 10_000
OP_MAX = 64
OP_ENTRY_MAX = 24
RECEIPT_MAX = 64 * 1024
REQUEST_RE = re.compile(r"^([0-9]+-[a-z0-9]+)\.json$")
ID_RE = re.compile(r"^[0-9]+-[a-z0-9]+$")
OP_RE = re.compile(r"^\.standby-([0-9]+-[a-z0-9]+)-sq-([a-f0-9]{48})$")
HANDLE_RE = re.compile(r"^[a-f0-9]{64}$")
SHA_RE = re.compile(r"^sha256:[a-f0-9]{64}$")
STEM_RE = re.compile(r"^TASK_([1-9][0-9]*)_[A-Za-z0-9_]+$")
LEASE_RE = re.compile(r"^wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159}$")
SESSION_RE = re.compile(r"^ws-[A-Za-z0-9][A-Za-z0-9._-]{15,159}$")
RUN_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._:-]{7,159}$")
DECIMAL_RE = re.compile(r"^(?:0|[1-9][0-9]*)$")
PROOF_FIELDS = ("ctimeNs", "dev", "ino", "mode", "mtimeNs", "nlink", "size", "type")
IDENTITY_FIELDS = ("dev", "ino", "mode", "type")
RECEIPTS = (
    "intent", "claimed", "offered", "invalid", "execution", "disclosed",
    "restore", "restored", "consume", "detached", "consumed",
)
ALLOWED_ENTRIES = {"request.claim", "consumed.claim"}
for _receipt in RECEIPTS:
    ALLOWED_ENTRIES.add(_receipt + ".json")
    ALLOWED_ENTRIES.add("." + _receipt + ".tmp")

O_DIRECTORY = getattr(os, "O_DIRECTORY", 0)
O_NOFOLLOW = getattr(os, "O_NOFOLLOW", 0)
O_CLOEXEC = getattr(os, "O_CLOEXEC", 0)
DIR_FLAGS = os.O_RDONLY | O_DIRECTORY | O_NOFOLLOW | O_CLOEXEC
READ_FLAGS = os.O_RDONLY | O_NOFOLLOW | O_CLOEXEC


class BoundaryError(Exception):
    def __init__(self, code: str):
        super().__init__(code)
        self.code = code


def _fail(code: str) -> None:
    raise BoundaryError(code)


def _exact_keys(value, keys) -> bool:
    return isinstance(value, dict) and set(value) == set(keys)


def _utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def _exact_utc(value) -> bool:
    if not isinstance(value, str) or re.fullmatch(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z", value) is None:
        return False
    try:
        parsed = datetime.strptime(value, "%Y-%m-%dT%H:%M:%S.%fZ").replace(tzinfo=timezone.utc)
    except ValueError:
        return False
    return parsed.isoformat(timespec="milliseconds").replace("+00:00", "Z") == value


def _canonical(value) -> bytes:
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")


def _proof(st) -> dict:
    if stat.S_ISDIR(st.st_mode):
        kind = "directory"
    elif stat.S_ISREG(st.st_mode):
        kind = "file"
    elif stat.S_ISLNK(st.st_mode):
        kind = "symlink"
    else:
        kind = "other"
    return {
        "ctimeNs": str(st.st_ctime_ns), "dev": str(st.st_dev), "ino": str(st.st_ino),
        "mode": str(st.st_mode), "mtimeNs": str(st.st_mtime_ns), "nlink": str(st.st_nlink),
        "size": str(st.st_size), "type": kind,
    }


def _valid_proof(value, kind=None) -> bool:
    return (
        _exact_keys(value, PROOF_FIELDS)
        and value["type"] in ("directory", "file", "symlink", "other")
        and all(isinstance(value[k], str) and len(value[k]) <= 32 and DECIMAL_RE.fullmatch(value[k]) for k in PROOF_FIELDS if k != "type")
        and (kind is None or value["type"] == kind)
    )


def _same(left, right, fields=PROOF_FIELDS) -> bool:
    return _valid_proof(left) and _valid_proof(right) and all(left[k] == right[k] for k in fields)


def _private_mode(proof, directory=False) -> bool:
    if sys.platform == "win32":
        return True
    expected = 0o700 if directory else 0o600
    return int(proof["mode"]) & 0o777 == expected


def _strict_relative(value: str) -> list[str]:
    if not isinstance(value, str) or not value or len(value) > 4096 or os.path.isabs(value) or "\x00" in value:
        _fail("invalid-relative-path")
    parts = value.split("/")
    if len(parts) > 256 or any(not part or len(os.fsencode(part)) > 255 or part in (".", "..") or "/" in part for part in parts):
        _fail("invalid-relative-path")
    return parts


def _list(fd: int, limit: int) -> list[str]:
    names = []
    try:
        with os.scandir(fd) as entries:
            for entry in entries:
                names.append(entry.name)
                if len(names) > limit:
                    _fail("directory-scan-limit")
    except OSError:
        _fail("directory-unreadable")
    if any(not isinstance(name, str) or "\x00" in name for name in names):
        _fail("directory-name-invalid")
    return names


def _fsync(fd: int, code="directory-sync-failed") -> None:
    try:
        os.fsync(fd)
    except OSError:
        _fail(code)


def _lstat(fd: int, name: str):
    try:
        return os.stat(name, dir_fd=fd, follow_symlinks=False)
    except FileNotFoundError:
        return None
    except OSError:
        _fail("entry-unreadable")


def _open_dir_at(parent: int, name: str) -> int:
    try:
        fd = os.open(name, DIR_FLAGS, dir_fd=parent)
    except OSError:
        _fail("directory-unsafe")
    proof = _proof(os.fstat(fd))
    if proof["type"] != "directory":
        os.close(fd)
        _fail("directory-unsafe")
    return fd


def _walk(root_fd: int, parts: list[str], expected: dict) -> int:
    current = os.dup(root_fd)
    try:
        for part in parts:
            nxt = _open_dir_at(current, part)
            os.close(current)
            current = nxt
        observed = _proof(os.fstat(current))
        if not _same(observed, expected, IDENTITY_FIELDS) or observed["type"] != "directory":
            _fail("directory-proof-changed")
        return current
    except Exception:
        os.close(current)
        raise


def _read_file(fd: int, name: str, max_bytes: int, require_private=True) -> tuple[bytes, dict]:
    before_st = _lstat(fd, name)
    if before_st is None:
        _fail("file-missing")
    before = _proof(before_st)
    if before["type"] != "file" or before["nlink"] != "1" or int(before["size"]) > max_bytes:
        _fail("file-unsafe")
    if require_private and not _private_mode(before):
        _fail("file-mode-unsafe")
    opened = None
    try:
        opened = os.open(name, READ_FLAGS, dir_fd=fd)
        opened_proof = _proof(os.fstat(opened))
        if not _same(before, opened_proof):
            _fail("file-changed")
        remaining = int(before["size"])
        chunks = []
        while remaining:
            block = os.read(opened, min(65536, remaining))
            if not block:
                _fail("file-short-read")
            chunks.append(block)
            remaining -= len(block)
        if os.read(opened, 1):
            _fail("file-grew")
        after_fd = _proof(os.fstat(opened))
        after_st = _lstat(fd, name)
        if after_st is None or not _same(opened_proof, after_fd) or not _same(opened_proof, _proof(after_st)):
            _fail("file-changed")
        return b"".join(chunks), opened_proof
    finally:
        if opened is not None:
            os.close(opened)


def _sha(bytes_value: bytes) -> str:
    return "sha256:" + hashlib.sha256(bytes_value).hexdigest()


def _test_enabled() -> bool:
    hook = os.environ.get("ORCHESTRATOR_STANDBY_TEST_HOOK")
    root = os.environ.get("ORCHESTRATOR_STANDBY_TEST_ROOT")
    project = os.environ.get("ORCHESTRATOR_PROJECT_ROOT")
    if not hook:
        return False
    if not root or root != project:
        return False
    try:
        common = os.path.commonpath((os.path.realpath(root), os.path.realpath(tempfile.gettempdir())))
    except ValueError:
        return False
    return common == os.path.realpath(tempfile.gettempdir()) and os.path.realpath(root) != os.path.realpath(tempfile.gettempdir())


def _crash(label: str) -> None:
    if _test_enabled() and os.environ.get("ORCHESTRATOR_STANDBY_TEST_HOOK") == label:
        os._exit(86)


def _rename_noreplace(src_fd: int, src: str, dst_fd: int, dst: str) -> None:
    src_b, dst_b = os.fsencode(src), os.fsencode(dst)
    libc = ctypes.CDLL(None, use_errno=True)
    result = -1
    if sys.platform == "darwin" and hasattr(libc, "renameatx_np"):
        fn = libc.renameatx_np
        fn.argtypes = (ctypes.c_int, ctypes.c_char_p, ctypes.c_int, ctypes.c_char_p, ctypes.c_uint)
        fn.restype = ctypes.c_int
        result = fn(src_fd, src_b, dst_fd, dst_b, 0x00000004)  # RENAME_EXCL
    elif sys.platform.startswith("linux") and hasattr(libc, "renameat2"):
        fn = libc.renameat2
        fn.argtypes = (ctypes.c_int, ctypes.c_char_p, ctypes.c_int, ctypes.c_char_p, ctypes.c_uint)
        fn.restype = ctypes.c_int
        result = fn(src_fd, src_b, dst_fd, dst_b, 1)  # RENAME_NOREPLACE
    else:
        _fail("rename-noreplace-unsupported")
    if result != 0:
        code = ctypes.get_errno()
        if code == errno.EEXIST:
            _fail("target-exists")
        if code == errno.ENOENT:
            _fail("source-missing")
        raise BoundaryError("rename-failed")


def _decode_json(bytes_value: bytes):
    try:
        text = bytes_value.decode("utf-8", "strict")
        value = _strict_json_loads(text)
    except (UnicodeError, json.JSONDecodeError, ValueError):
        _fail("receipt-invalid")
    if _canonical(value) != bytes_value:
        _fail("receipt-noncanonical")
    return value


def _strict_json_loads(text: str):
    def object_hook(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("duplicate JSON key")
            result[key] = value
        return result

    def reject_constant(_value):
        raise ValueError("non-standard JSON number")

    return json.loads(text, object_pairs_hook=object_hook, parse_constant=reject_constant)


def _receipt_shape(name: str, value: dict, op_id: str) -> bool:
    common = (
        isinstance(value, dict)
        and type(value.get("version")) is int
        and value.get("version") == 1
        and value.get("requestId") == op_id
    )
    if not common:
        return False
    if name == "intent":
        return _exact_keys(value, ("version", "requestId", "sourceName", "sourceProof", "sourceSha256", "createdAt")) and value["sourceName"] == op_id + ".json" and _valid_proof(value["sourceProof"]) and (value["sourceSha256"] is None or SHA_RE.fullmatch(str(value["sourceSha256"] or "")) is not None) and _exact_utc(value["createdAt"])
    if name == "claimed":
        return _exact_keys(value, ("version", "requestId", "claimProof", "claimSha256", "claimedAt")) and _valid_proof(value["claimProof"]) and SHA_RE.fullmatch(str(value["claimSha256"] or "")) is not None and _exact_utc(value["claimedAt"])
    if name == "offered":
        return _exact_keys(value, ("version", "requestId", "handle", "claimSha256", "fingerprint", "promptHash", "request", "offeredAt")) and HANDLE_RE.fullmatch(str(value["handle"] or "")) is not None and SHA_RE.fullmatch(str(value["claimSha256"] or "")) is not None and SHA_RE.fullmatch(str(value["fingerprint"] or "")) is not None and SHA_RE.fullmatch(str(value["promptHash"] or "")) is not None and _projection_shape(value["request"], op_id) and _exact_utc(value["offeredAt"])
    if name == "invalid":
        return _exact_keys(value, ("version", "requestId", "code", "invalidAt")) and value["code"] in ("unsafe-claimed-entry", "contract-invalid") and _exact_utc(value["invalidAt"])
    if name in ("execution", "disclosed"):
        return _exact_keys(value, ("version", "requestId", "handle", "claimSha256", "fence", name + "At")) and HANDLE_RE.fullmatch(str(value["handle"] or "")) is not None and SHA_RE.fullmatch(str(value["claimSha256"] or "")) is not None and _fence_shape(value["fence"]) and _exact_utc(value[name + "At"])
    if name == "restore":
        return _exact_keys(value, ("version", "requestId", "handle", "claimSha256", "claimProof", "targetName", "restoreAt")) and HANDLE_RE.fullmatch(str(value["handle"] or "")) is not None and SHA_RE.fullmatch(str(value["claimSha256"] or "")) is not None and _valid_proof(value["claimProof"], "file") and value["targetName"] == op_id + ".json" and _exact_utc(value["restoreAt"])
    if name == "restored":
        return _exact_keys(value, ("version", "requestId", "handle", "claimSha256", "targetProof", "restoredAt")) and HANDLE_RE.fullmatch(str(value["handle"] or "")) is not None and SHA_RE.fullmatch(str(value["claimSha256"] or "")) is not None and _valid_proof(value["targetProof"], "file") and _exact_utc(value["restoredAt"])
    if name == "consume":
        return _exact_keys(value, ("version", "requestId", "handle", "claimSha256", "claimProof", "kind", "consumeAt")) and HANDLE_RE.fullmatch(str(value["handle"] or "")) is not None and SHA_RE.fullmatch(str(value["claimSha256"] or "")) is not None and _valid_proof(value["claimProof"], "file") and value["kind"] in ("executed", "superseded") and _exact_utc(value["consumeAt"])
    if name == "detached":
        return _exact_keys(value, ("version", "requestId", "handle", "claimSha256", "detachedProof", "detachedAt")) and HANDLE_RE.fullmatch(str(value["handle"] or "")) is not None and SHA_RE.fullmatch(str(value["claimSha256"] or "")) is not None and _valid_proof(value["detachedProof"], "file") and _exact_utc(value["detachedAt"])
    if name == "consumed":
        return _exact_keys(value, ("version", "requestId", "handle", "claimSha256", "kind", "consumedAt")) and HANDLE_RE.fullmatch(str(value["handle"] or "")) is not None and SHA_RE.fullmatch(str(value["claimSha256"] or "")) is not None and value["kind"] in ("executed", "superseded") and _exact_utc(value["consumedAt"])
    return False


def _fence_shape(value) -> bool:
    fields = ("version", "action", "stem", "expectedState", "sourceRevision", "snapshotHash", "indexStatus", "leaseId", "sessionId", "taskLock")
    return (
        _exact_keys(value, fields)
        and type(value["version"]) is int and value["version"] == 1
        and value["action"] in ("prep", "answers", "run", "drop", "reopen")
        and _canonical_stem(value["stem"])
        and value["expectedState"] in ("backlog", "pending", "todo", "done", "corrupt")
        and SHA_RE.fullmatch(str(value["sourceRevision"] or "")) is not None
        and SHA_RE.fullmatch(str(value["snapshotHash"] or "")) is not None
        and value["indexStatus"] in ("fresh", "stale", "invalid")
        and LEASE_RE.fullmatch(str(value["leaseId"] or "")) is not None
        and SESSION_RE.fullmatch(str(value["sessionId"] or "")) is not None
        and _task_lock_shape(value["taskLock"], value["action"])
    )


def _canonical_stem(value) -> bool:
    if not isinstance(value, str) or len(value) > 120:
        return False
    match = STEM_RE.fullmatch(value)
    if match is None:
        return False
    number = int(match.group(1))
    return 0 < number <= 9_007_199_254_740_991 and match.group(1) == str(number)


def _task_lock_shape(value, action: str) -> bool:
    # A standby request is fenced by its exact active writer capability, not by
    # a lock pre-acquired on behalf of the selected skill. The canonical
    # task-lock helper rejects every foreign acquire while that lease is live
    # and lets the selected skill publish its own lock only with the exact
    # private lease capability after disclosure.
    return action in ("prep", "answers", "run", "drop", "reopen") and value is None


def _projection_shape(value, request_id: str) -> bool:
    fields = ("id", "version", "action", "stem", "expectedState", "sourceRevision", "createdAt", "fingerprint", "promptHash")
    return (
        _exact_keys(value, fields) and value["id"] == request_id
        and type(value["version"]) is int and value["version"] == 2
        and isinstance(value["action"], str) and isinstance(value["stem"], str)
        and isinstance(value["expectedState"], str) and isinstance(value["sourceRevision"], str)
        and isinstance(value["createdAt"], str) and SHA_RE.fullmatch(str(value["fingerprint"] or "")) is not None
        and SHA_RE.fullmatch(str(value["promptHash"] or "")) is not None
        and not any(key.lower() in ("prompt", "dedupkey", "dedupreport", "body") for key in value)
    )


def _read_receipt(op_fd: int, name: str, op_id: str):
    final = name + ".json"
    temp = "." + name + ".tmp"
    final_st, temp_st = _lstat(op_fd, final), _lstat(op_fd, temp)
    if final_st is not None and temp_st is not None:
        _fail("receipt-duplicate-evidence")
    selected = final if final_st is not None else temp if temp_st is not None else None
    if selected is None:
        return None
    bytes_value, _ = _read_file(op_fd, selected, RECEIPT_MAX)
    value = _decode_json(bytes_value)
    if not _receipt_shape(name, value, op_id):
        _fail("receipt-contract-invalid")
    if selected == temp:
        _rename_noreplace(op_fd, temp, op_fd, final)
        _crash(name + "-published")
        _fsync(op_fd)
        _crash(name + "-durable")
    return value


def _write_receipt(op_fd: int, name: str, op_id: str, value: dict) -> dict:
    existing = _read_receipt(op_fd, name, op_id)
    if existing is not None:
        return existing
    if not _receipt_shape(name, value, op_id):
        _fail("receipt-contract-invalid")
    bytes_value = _canonical(value)
    if len(bytes_value) > RECEIPT_MAX:
        _fail("receipt-too-large")
    temp = "." + name + ".tmp"
    fd = None
    try:
        fd = os.open(temp, os.O_WRONLY | os.O_CREAT | os.O_EXCL | O_NOFOLLOW | O_CLOEXEC, 0o600, dir_fd=op_fd)
        offset = 0
        while offset < len(bytes_value):
            written = os.write(fd, bytes_value[offset:])
            if written <= 0:
                _fail("receipt-write-failed")
            offset += written
        os.fsync(fd)
    except FileExistsError:
        return _read_receipt(op_fd, name, op_id)
    except OSError:
        _fail("receipt-write-failed")
    finally:
        if fd is not None:
            os.close(fd)
    _crash(name + "-temp-durable")
    _rename_noreplace(op_fd, temp, op_fd, name + ".json")
    _crash(name + "-published")
    _fsync(op_fd)
    _crash(name + "-durable")
    return value


def _intent_from_source(requests_fd: int, op_id: str) -> dict:
    source = op_id + ".json"
    observed = _lstat(requests_fd, source)
    if observed is None:
        _fail("source-missing")
    observed_proof = _proof(observed)
    source_sha = None
    if observed_proof["type"] == "file" and observed_proof["nlink"] == "1" and int(observed_proof["size"]) <= REQUEST_MAX and _private_mode(observed_proof):
        try:
            source_bytes, stable_source = _read_file(requests_fd, source, REQUEST_MAX)
            if _same(stable_source, observed_proof):
                source_sha = _sha(source_bytes)
        except BoundaryError:
            source_sha = None
    return {
        "version": 1, "requestId": op_id, "sourceName": source,
        "sourceProof": observed_proof, "sourceSha256": source_sha, "createdAt": _utc_now(),
    }


def _oldest(requests_fd: int):
    names = _list(requests_fd, DIRECTORY_MAX)
    canonical = sorted(name for name in names if REQUEST_RE.fullmatch(name))
    return canonical[0] if canonical else None


def _read_claim(op_fd: int, claimed: dict) -> tuple[bytes, dict]:
    bytes_value, proof = _read_file(op_fd, "request.claim", REQUEST_MAX)
    if not _same(proof, claimed["claimProof"]) or _sha(bytes_value) != claimed["claimSha256"]:
        _fail("claim-replaced")
    return bytes_value, proof


def _initialize_or_recover_claim(requests_fd: int, runs_fd: int, op_fd: int, op_name: str, op_id: str):
    names = _list(op_fd, OP_ENTRY_MAX)
    if any(name not in ALLOWED_ENTRIES for name in names):
        _fail("foreign-op-evidence")
    intent = _read_receipt(op_fd, "intent", op_id)
    if intent is None:
        if names:
            _fail("intent-missing")
        if _oldest(requests_fd) != op_id + ".json":
            _fail("oldest-changed")
        intent = _write_receipt(op_fd, "intent", op_id, _intent_from_source(requests_fd, op_id))
    source_st = _lstat(requests_fd, intent["sourceName"])
    claim_st = _lstat(op_fd, "request.claim")
    claimed = _read_receipt(op_fd, "claimed", op_id)
    if claim_st is None:
        if claimed is not None:
            _fail("claim-disappeared")
        if source_st is None:
            _abandon_unclaimed(runs_fd, op_fd, op_name, op_id)
            return {"status": "retry", "code": "claim-lost-race"}
        if not _same(_proof(source_st), intent["sourceProof"]):
            _fail("source-replaced")
        if _oldest(requests_fd) != intent["sourceName"]:
            if _lstat(requests_fd, intent["sourceName"]) is None:
                _abandon_unclaimed(runs_fd, op_fd, op_name, op_id)
                return {"status": "retry", "code": "claim-lost-race"}
            _fail("oldest-changed")
        try:
            _rename_noreplace(requests_fd, intent["sourceName"], op_fd, "request.claim")
        except BoundaryError as error:
            if error.code == "source-missing":
                _abandon_unclaimed(runs_fd, op_fd, op_name, op_id)
                return {"status": "retry", "code": "claim-lost-race"}
            raise
        _crash("claim-renamed")
        _fsync(requests_fd)
        _fsync(op_fd)
        _crash("claim-dirs-durable")
        claim_st = _lstat(op_fd, "request.claim")
        source_st = _lstat(requests_fd, intent["sourceName"])
    if source_st is not None:
        _fail("source-and-claim-both-present")
    claim_proof = _proof(claim_st)
    source_proof = intent["sourceProof"]
    if not _same(claim_proof, source_proof, ("dev", "ino", "mode", "nlink", "size", "mtimeNs", "type")):
        invalid = {"version": 1, "requestId": op_id, "code": "unsafe-claimed-entry", "invalidAt": _utc_now()}
        _write_receipt(op_fd, "invalid", op_id, invalid)
        return {"status": "retained", "code": "unsafe-claimed-entry"}
    if claim_proof["type"] != "file" or claim_proof["nlink"] != "1" or int(claim_proof["size"]) > REQUEST_MAX or not _private_mode(claim_proof):
        invalid = {"version": 1, "requestId": op_id, "code": "unsafe-claimed-entry", "invalidAt": _utc_now()}
        _write_receipt(op_fd, "invalid", op_id, invalid)
        return {"status": "retained", "code": "unsafe-claimed-entry"}
    if claimed is None:
        try:
            bytes_value, stable = _read_file(op_fd, "request.claim", REQUEST_MAX)
        except BoundaryError:
            invalid = {"version": 1, "requestId": op_id, "code": "unsafe-claimed-entry", "invalidAt": _utc_now()}
            _write_receipt(op_fd, "invalid", op_id, invalid)
            return {"status": "retained", "code": "unsafe-claimed-entry"}
        claimed = _write_receipt(op_fd, "claimed", op_id, {
            "version": 1, "requestId": op_id, "claimProof": stable,
            "claimSha256": _sha(bytes_value), "claimedAt": _utc_now(),
        })
    if intent["sourceSha256"] is not None and claimed["claimSha256"] != intent["sourceSha256"]:
        invalid = {"version": 1, "requestId": op_id, "code": "unsafe-claimed-entry", "invalidAt": _utc_now()}
        _write_receipt(op_fd, "invalid", op_id, invalid)
        return {"status": "retained", "code": "source-content-changed"}
    if _read_receipt(op_fd, "invalid", op_id) is not None:
        return {"status": "retained", "code": "invalid-private-claim"}
    bytes_value, _ = _read_claim(op_fd, claimed)
    try:
        _strict_json_loads(bytes_value.decode("utf-8", "strict"))
    except (UnicodeError, json.JSONDecodeError, ValueError):
        invalid = {"version": 1, "requestId": op_id, "code": "contract-invalid", "invalidAt": _utc_now()}
        _write_receipt(op_fd, "invalid", op_id, invalid)
        return {"status": "retained", "code": "contract-invalid"}
    return {
        "status": "claimed", "op": op_name, "id": op_id,
        "bytes": bytes_value.hex(), "claimSha256": claimed["claimSha256"],
    }


def _op_names(runs_fd: int):
    names = _list(runs_fd, DIRECTORY_MAX)
    exact = []
    for name in names:
        match = OP_RE.fullmatch(name)
        if match:
            exact.append((name, match.group(1)))
        elif name.startswith(".standby-"):
            _fail("foreign-standby-evidence")
    if len(exact) > OP_MAX:
        _fail("operation-scan-limit")
    return sorted(exact)


def _open_op(runs_fd: int, name: str) -> int:
    fd = _open_dir_at(runs_fd, name)
    proof = _proof(os.fstat(fd))
    if not _private_mode(proof, directory=True):
        os.close(fd)
        _fail("operation-mode-unsafe")
    return fd


def _assert_operation_entries(op_fd: int) -> list[str]:
    names = _list(op_fd, OP_ENTRY_MAX)
    if any(name not in ALLOWED_ENTRIES for name in names):
        _fail("foreign-op-evidence")
    return names


def _find_handle(runs_fd: int, handle: str):
    matches = []
    for name, op_id in _op_names(runs_fd):
        op_fd = _open_op(runs_fd, name)
        try:
            _assert_operation_entries(op_fd)
            offered = _read_receipt(op_fd, "offered", op_id)
            if offered is not None and offered["handle"] == handle:
                matches.append((name, op_id, op_fd, offered))
                op_fd = None
        finally:
            if op_fd is not None:
                os.close(op_fd)
    if len(matches) != 1:
        for _, _, fd, _ in matches:
            os.close(fd)
        _fail("handle-not-found" if not matches else "handle-ambiguous")
    return matches[0]


def _phase_state(op_fd: int, op_id: str):
    return {name: _read_receipt(op_fd, name, op_id) for name in ("invalid", "execution", "disclosed", "restore", "restored", "consume", "detached", "consumed")}


def _validate_receipt_links(op_fd: int, op_id: str, intent=None, claimed=None, offered=None, phases=None):
    intent = intent if intent is not None else _read_receipt(op_fd, "intent", op_id)
    claimed = claimed if claimed is not None else _read_receipt(op_fd, "claimed", op_id)
    offered = offered if offered is not None else _read_receipt(op_fd, "offered", op_id)
    phases = phases if phases is not None else _phase_state(op_fd, op_id)
    if intent is None:
        _fail("intent-missing")
    if claimed is not None:
        if not _same(claimed["claimProof"], intent["sourceProof"], ("dev", "ino", "mode", "nlink", "size", "mtimeNs", "type")):
            _fail("claimed-lineage-mismatch")
        if intent["sourceSha256"] is not None and claimed["claimSha256"] != intent["sourceSha256"]:
            _fail("claimed-hash-mismatch")
    if offered is not None:
        if claimed is None or offered["claimSha256"] != claimed["claimSha256"] or offered["fingerprint"] != offered["request"]["fingerprint"] or offered["promptHash"] != offered["request"]["promptHash"]:
            _fail("offered-lineage-mismatch")
    for phase_name in ("execution", "disclosed", "restore", "consume"):
        phase = phases[phase_name]
        if phase is not None and (offered is None or claimed is None or phase["handle"] != offered["handle"] or phase["claimSha256"] != claimed["claimSha256"]):
            _fail(phase_name + "-lineage-mismatch")
    for phase_name in ("execution", "disclosed"):
        phase = phases[phase_name]
        if phase is not None:
            fence = phase["fence"]
            request = offered["request"] if offered is not None else None
            if request is None or any(fence[key] != request[key] for key in ("action", "stem", "expectedState", "sourceRevision")):
                _fail(phase_name + "-fence-request-mismatch")
    if phases["execution"] is not None and phases["disclosed"] is not None:
        execution_fence = phases["execution"]["fence"]
        disclosure_fence = phases["disclosed"]["fence"]
        # INDEX is derived diagnostics and may legitimately change between
        # execution preparation and prompt disclosure. The task's exact source
        # revision and writer capability remain the authority-bearing fields.
        stable_fields = ("version", "action", "stem", "expectedState", "sourceRevision", "leaseId", "sessionId")
        if any(execution_fence[key] != disclosure_fence[key] for key in stable_fields) or execution_fence["taskLock"] != disclosure_fence["taskLock"]:
            _fail("disclosure-fence-lineage-mismatch")
    if phases["disclosed"] is not None and phases["execution"] is None:
        _fail("disclosure-without-execution")
    if phases["restore"] is not None:
        if phases["disclosed"] is not None or phases["consume"] is not None or not _same(phases["restore"]["claimProof"], claimed["claimProof"]):
            _fail("restore-lineage-mismatch")
    if phases["consume"] is not None:
        if phases["restore"] is not None or not _same(phases["consume"]["claimProof"], claimed["claimProof"]):
            _fail("consume-lineage-mismatch")
        if phases["consume"]["kind"] == "executed" and phases["disclosed"] is None:
            _fail("consume-without-disclosure")
    if phases["detached"] is not None:
        if phases["consume"] is None or phases["detached"]["handle"] != phases["consume"]["handle"] or phases["detached"]["claimSha256"] != phases["consume"]["claimSha256"]:
            _fail("detached-lineage-mismatch")
    if phases["restored"] is not None:
        if phases["restore"] is None or phases["restored"]["handle"] != phases["restore"]["handle"] or phases["restored"]["claimSha256"] != phases["restore"]["claimSha256"]:
            _fail("restored-lineage-mismatch")
    if phases["consumed"] is not None:
        if phases["consume"] is None or phases["consumed"]["handle"] != phases["consume"]["handle"] or phases["consumed"]["claimSha256"] != phases["consume"]["claimSha256"] or phases["consumed"]["kind"] != phases["consume"]["kind"]:
            _fail("consumed-lineage-mismatch")
    return intent, claimed, offered, phases


def _load_handle(runs_fd: int, handle: str):
    name, op_id, op_fd, offered = _find_handle(runs_fd, handle)
    try:
        claimed = _read_receipt(op_fd, "claimed", op_id)
        if claimed is None or claimed["claimSha256"] != offered["claimSha256"]:
            _fail("claim-receipt-missing")
        phases = _phase_state(op_fd, op_id)
        _validate_receipt_links(op_fd, op_id, claimed=claimed, offered=offered, phases=phases)
        if phases["invalid"] is not None:
            _fail("invalid-private-claim")
        if phases["restored"] is not None or phases["consumed"] is not None:
            _fail("handle-terminal")
        bytes_value, _ = _read_claim(op_fd, claimed)
        return name, op_id, op_fd, offered, claimed, phases, bytes_value
    except Exception:
        os.close(op_fd)
        raise


def _delete_exact(op_fd: int, name: str, op_id: str) -> None:
    receipt_name = name[:-5] if name.endswith(".json") else None
    if receipt_name not in RECEIPTS:
        _fail("cleanup-entry-invalid")
    value = _read_receipt(op_fd, receipt_name, op_id)
    if value is None:
        return
    try:
        os.unlink(name, dir_fd=op_fd)
    except FileNotFoundError:
        return
    except OSError:
        _fail("cleanup-unlink-failed")
    _fsync(op_fd)


def _remove_empty_operation(runs_fd: int, op_fd: int, op_name: str) -> None:
    if _list(op_fd, OP_ENTRY_MAX):
        _fail("operation-not-empty")
    try:
        os.rmdir(op_name, dir_fd=runs_fd)
    except FileNotFoundError:
        pass
    except OSError:
        _fail("operation-rmdir-failed")
    _fsync(runs_fd)


def _abandon_unclaimed(runs_fd: int, op_fd: int, op_name: str, op_id: str) -> None:
    if _lstat(op_fd, "request.claim") is not None or _lstat(op_fd, "consumed.claim") is not None:
        _fail("unclaimed-cleanup-has-claim")
    _delete_exact(op_fd, "intent.json", op_id)
    _remove_empty_operation(runs_fd, op_fd, op_name)


def _cleanup_terminal(runs_fd: int, op_fd: int, op_name: str, op_id: str, terminal: str) -> None:
    names = _list(op_fd, OP_ENTRY_MAX)
    if any(name not in ALLOWED_ENTRIES for name in names) or "request.claim" in names or "consumed.claim" in names:
        _fail("terminal-cleanup-blocked")
    order = [name + ".json" for name in RECEIPTS if name not in ("intent", terminal)] + ["intent.json", terminal + ".json"]
    for name in order:
        _delete_exact(op_fd, name, op_id)
        _crash("cleanup-" + name[:-5])
    if _list(op_fd, OP_ENTRY_MAX):
        _fail("terminal-cleanup-evidence")
    os.close(op_fd)
    try:
        os.rmdir(op_name, dir_fd=runs_fd)
    except FileNotFoundError:
        pass
    except OSError:
        _fail("operation-rmdir-failed")
    _fsync(runs_fd)
    _crash("cleanup-durable")


def _recover_restore(requests_fd: int, runs_fd: int, op_fd: int, op_name: str, op_id: str, restore: dict):
    target_st = _lstat(requests_fd, restore["targetName"])
    claim_st = _lstat(op_fd, "request.claim")
    restored = _read_receipt(op_fd, "restored", op_id)
    if restored is not None:
        if claim_st is not None or target_st is None or not _same(_proof(target_st), restored["targetProof"]):
            _fail("restored-target-replaced")
        _cleanup_terminal(runs_fd, op_fd, op_name, op_id, "restored")
        return {"status": "restored", "id": op_id}
    if target_st is not None:
        if claim_st is not None:
            return {"status": "blocked", "code": "restore-collision"}
        target_proof = _proof(target_st)
        if not _same(target_proof, restore["claimProof"], ("dev", "ino", "mode", "nlink", "size", "mtimeNs", "type")):
            _fail("restore-target-foreign")
        target_bytes, stable_target = _read_file(requests_fd, restore["targetName"], REQUEST_MAX)
        if not _same(stable_target, target_proof) or _sha(target_bytes) != restore["claimSha256"]:
            _fail("restore-target-foreign")
        _write_receipt(op_fd, "restored", op_id, {
            "version": 1, "requestId": op_id, "handle": restore["handle"],
            "claimSha256": restore["claimSha256"], "targetProof": stable_target, "restoredAt": _utc_now(),
        })
        _cleanup_terminal(runs_fd, op_fd, op_name, op_id, "restored")
        return {"status": "restored", "id": op_id}
    if claim_st is None:
        _fail("restore-claim-disappeared")
    if not _same(_proof(claim_st), restore["claimProof"]):
        _fail("restore-claim-replaced")
    _rename_noreplace(op_fd, "request.claim", requests_fd, restore["targetName"])
    _crash("restore-renamed")
    _fsync(op_fd)
    _fsync(requests_fd)
    _crash("restore-dirs-durable")
    target_st = _lstat(requests_fd, restore["targetName"])
    if target_st is None:
        _fail("restored-target-missing")
    target_proof = _proof(target_st)
    if not _same(target_proof, restore["claimProof"], ("dev", "ino", "mode", "nlink", "size", "mtimeNs", "type")):
        _fail("restored-target-replaced")
    restored = _write_receipt(op_fd, "restored", op_id, {
        "version": 1, "requestId": op_id, "handle": restore["handle"],
        "claimSha256": restore["claimSha256"], "targetProof": target_proof, "restoredAt": _utc_now(),
    })
    _cleanup_terminal(runs_fd, op_fd, op_name, op_id, "restored")
    return {"status": "restored", "id": op_id}


def _recover_consume(runs_fd: int, op_fd: int, op_name: str, op_id: str, consume: dict):
    claim_st = _lstat(op_fd, "request.claim")
    held_st = _lstat(op_fd, "consumed.claim")
    detached = _read_receipt(op_fd, "detached", op_id)
    consumed = _read_receipt(op_fd, "consumed", op_id)
    if consumed is not None:
        if claim_st is not None or held_st is not None:
            _fail("consumed-claim-reappeared")
        _cleanup_terminal(runs_fd, op_fd, op_name, op_id, "consumed")
        return {"status": "consumed", "id": op_id, "kind": consume["kind"]}
    if detached is None:
        if held_st is not None:
            held_proof = _proof(held_st)
            if claim_st is not None or not _same(held_proof, consume["claimProof"], ("dev", "ino", "mode", "nlink", "size", "mtimeNs", "type")):
                _fail("consume-detach-foreign")
            held_bytes, stable_held = _read_file(op_fd, "consumed.claim", REQUEST_MAX)
            if not _same(stable_held, held_proof) or _sha(held_bytes) != consume["claimSha256"]:
                _fail("consume-detach-foreign")
        else:
            if claim_st is None:
                _fail("consume-claim-disappeared")
            if not _same(_proof(claim_st), consume["claimProof"]):
                _fail("consume-claim-replaced")
            _rename_noreplace(op_fd, "request.claim", op_fd, "consumed.claim")
            _crash("consume-detached")
            _fsync(op_fd)
            _crash("consume-detached-durable")
            held_st = _lstat(op_fd, "consumed.claim")
        detached = _write_receipt(op_fd, "detached", op_id, {
            "version": 1, "requestId": op_id, "handle": consume["handle"],
            "claimSha256": consume["claimSha256"], "detachedProof": _proof(held_st), "detachedAt": _utc_now(),
        })
    claim_st = _lstat(op_fd, "request.claim")
    if claim_st is not None:
        _fail("consume-source-reappeared")
    held_st = _lstat(op_fd, "consumed.claim")
    if held_st is not None:
        if not _same(_proof(held_st), detached["detachedProof"]):
            _fail("consume-held-replaced")
        bytes_value, proof = _read_file(op_fd, "consumed.claim", REQUEST_MAX)
        if not _same(proof, detached["detachedProof"]) or _sha(bytes_value) != consume["claimSha256"]:
            _fail("consume-held-replaced")
        os.unlink("consumed.claim", dir_fd=op_fd)
        _crash("consume-unlinked")
        _fsync(op_fd)
        _crash("consume-unlink-durable")
    consumed = _write_receipt(op_fd, "consumed", op_id, {
        "version": 1, "requestId": op_id, "handle": consume["handle"],
        "claimSha256": consume["claimSha256"], "kind": consume["kind"], "consumedAt": _utc_now(),
    })
    _cleanup_terminal(runs_fd, op_fd, op_name, op_id, "consumed")
    return {"status": "consumed", "id": op_id, "kind": consume["kind"]}


def _recover_ops(requests_fd: int, runs_fd: int):
    pending = []
    recovered_terminal = 0
    for op_name, op_id in _op_names(runs_fd):
        op_fd = _open_op(runs_fd, op_name)
        try:
            names = _assert_operation_entries(op_fd)
            if not names:
                _remove_empty_operation(runs_fd, op_fd, op_name)
                recovered_terminal += 1
                continue
            restored_terminal = _read_receipt(op_fd, "restored", op_id)
            consumed_terminal = _read_receipt(op_fd, "consumed", op_id)
            if restored_terminal is not None and consumed_terminal is not None:
                _fail("conflicting-terminal-receipts")
            if restored_terminal is not None:
                target_st = _lstat(requests_fd, op_id + ".json")
                if target_st is None or not _same(_proof(target_st), restored_terminal["targetProof"]):
                    _fail("terminal-restore-evidence-mismatch")
                target_bytes, target_proof = _read_file(requests_fd, op_id + ".json", REQUEST_MAX)
                if not _same(target_proof, restored_terminal["targetProof"]) or _sha(target_bytes) != restored_terminal["claimSha256"]:
                    _fail("terminal-restore-evidence-mismatch")
                _cleanup_terminal(runs_fd, op_fd, op_name, op_id, "restored")
                op_fd = None
                recovered_terminal += 1
                continue
            if consumed_terminal is not None:
                if _lstat(op_fd, "request.claim") is not None or _lstat(op_fd, "consumed.claim") is not None:
                    _fail("terminal-consume-evidence-mismatch")
                _cleanup_terminal(runs_fd, op_fd, op_name, op_id, "consumed")
                op_fd = None
                recovered_terminal += 1
                continue
            intent = _read_receipt(op_fd, "intent", op_id)
            if intent is None:
                _fail("intent-missing")
            restore = _read_receipt(op_fd, "restore", op_id)
            consume = _read_receipt(op_fd, "consume", op_id)
            if restore is not None and consume is not None:
                _fail("conflicting-terminal-receipts")
            if restore is not None or consume is not None:
                _validate_receipt_links(op_fd, op_id, intent=intent)
            if restore is not None:
                result = _recover_restore(requests_fd, runs_fd, op_fd, op_name, op_id, restore)
                if result.get("status") == "blocked":
                    pending.append((op_name, op_id, op_fd))
                    op_fd = None
                    continue
                recovered_terminal += 1
                op_fd = None
                continue
            if consume is not None:
                result = _recover_consume(runs_fd, op_fd, op_name, op_id, consume)
                recovered_terminal += 1
                op_fd = None
                continue
            if _lstat(op_fd, "request.claim") is None and _lstat(requests_fd, intent["sourceName"]) is None:
                _abandon_unclaimed(runs_fd, op_fd, op_name, op_id)
                recovered_terminal += 1
                continue
            pending.append((op_name, op_id, op_fd))
            op_fd = None
        finally:
            if op_fd is not None:
                os.close(op_fd)
    if len(pending) > 1:
        # Concurrent drainers may both durably publish an intent before either
        # reaches the single source rename. If (and only if) every operation is
        # the same exact unclaimed source generation, elect the lexical WAL and
        # remove the other empty intents. Any claim, id/proof disagreement, or
        # foreign evidence remains fail-closed.
        first_id = pending[0][1]
        source_st = _lstat(requests_fd, first_id + ".json")
        source_proof = _proof(source_st) if source_st is not None else None
        electable = source_proof is not None and all(op_id == first_id and _lstat(fd, "request.claim") is None for _, op_id, fd in pending)
        if electable:
            for _, op_id, fd in pending:
                intent = _read_receipt(fd, "intent", op_id)
                if intent is None or not _same(intent["sourceProof"], source_proof):
                    electable = False
                    break
        if electable:
            pending.sort(key=lambda row: row[0])
            winner = pending[0]
            for op_name, op_id, fd in pending[1:]:
                _abandon_unclaimed(runs_fd, fd, op_name, op_id)
                os.close(fd)
                recovered_terminal += 1
            pending = [winner]
        else:
            for _, _, fd in pending:
                os.close(fd)
            _fail("multiple-active-operations")
    return pending, recovered_terminal


def _root_context(request):
    common = ("version", "action", "projectRoot", "requestsRelative", "runsRelative", "rootProof", "requestsProof", "runsProof")
    if not isinstance(request, dict) or type(request.get("version")) is not int or request.get("version") != 1:
        _fail("input-contract-invalid")
    if not isinstance(request.get("projectRoot"), str) or len(request["projectRoot"]) > 4096 or not os.path.isabs(request["projectRoot"]) or "\x00" in request["projectRoot"]:
        _fail("input-contract-invalid")
    for name in ("rootProof", "requestsProof", "runsProof"):
        if not _valid_proof(request.get(name), "directory"):
            _fail("input-proof-invalid")
    root_fd = None
    try:
        root_fd = os.open(request["projectRoot"], DIR_FLAGS)
        root_observed = _proof(os.fstat(root_fd))
        if not _same(root_observed, request["rootProof"], IDENTITY_FIELDS):
            _fail("root-proof-changed")
        requests_fd = _walk(root_fd, _strict_relative(request["requestsRelative"]), request["requestsProof"])
        try:
            runs_fd = _walk(root_fd, _strict_relative(request["runsRelative"]), request["runsProof"])
        except Exception:
            os.close(requests_fd)
            raise
        return root_fd, requests_fd, runs_fd, set(common)
    except Exception:
        if root_fd is not None:
            os.close(root_fd)
        raise


def _dispatch(request):
    root_fd, requests_fd, runs_fd, common = _root_context(request)
    try:
        action = request["action"]
        if action == "claim":
            if set(request) != common | {"candidate", "nonce"}:
                _fail("input-contract-invalid")
            candidate, nonce = request["candidate"], request["nonce"]
            if candidate is not None and (not isinstance(candidate, str) or REQUEST_RE.fullmatch(candidate) is None):
                _fail("input-contract-invalid")
            if nonce is not None and (not isinstance(nonce, str) or re.fullmatch(r"[a-f0-9]{48}", nonce) is None):
                _fail("input-contract-invalid")
            pending, recovered_terminal = _recover_ops(requests_fd, runs_fd)
            if pending:
                op_name, op_id, op_fd = pending[0]
                try:
                    offered = _read_receipt(op_fd, "offered", op_id)
                    if offered is not None:
                        return {"ok": True, "status": "blocked", "code": "active-offer"}
                    return {"ok": True, **_initialize_or_recover_claim(requests_fd, runs_fd, op_fd, op_name, op_id)}
                finally:
                    os.close(op_fd)
            if recovered_terminal:
                return {"ok": True, "status": "recovered"}
            if candidate is None:
                return {"ok": True, "status": "none"}
            if _oldest(requests_fd) != candidate:
                return {"ok": True, "status": "retry", "code": "oldest-changed"}
            op_id = candidate[:-5]
            op_name = ".standby-" + op_id + "-sq-" + nonce
            try:
                os.mkdir(op_name, 0o700, dir_fd=runs_fd)
            except FileExistsError:
                _fail("operation-name-collision")
            except OSError:
                _fail("operation-create-failed")
            _fsync(runs_fd)
            _crash("op-created")
            op_fd = _open_op(runs_fd, op_name)
            try:
                return {"ok": True, **_initialize_or_recover_claim(requests_fd, runs_fd, op_fd, op_name, op_id)}
            finally:
                os.close(op_fd)
        # Every public operation first completes any receipt-authorized terminal
        # transaction.  Non-terminal offers are merely reopened below; no age,
        # PID, or timestamp grants recovery authority.
        recovered_pending, _ = _recover_ops(requests_fd, runs_fd)
        for _, _, recovered_fd in recovered_pending:
            os.close(recovered_fd)
        if action == "offer":
            if set(request) != common | {"op", "handle", "claimSha256", "fingerprint", "promptHash", "request"}:
                _fail("input-contract-invalid")
            match = OP_RE.fullmatch(str(request["op"] or ""))
            if not match or not HANDLE_RE.fullmatch(str(request["handle"] or "")) or not SHA_RE.fullmatch(str(request["claimSha256"] or "")) or not SHA_RE.fullmatch(str(request["fingerprint"] or "")) or not SHA_RE.fullmatch(str(request["promptHash"] or "")):
                _fail("input-contract-invalid")
            op_id = match.group(1)
            if not _projection_shape(request["request"], op_id) or request["request"]["fingerprint"] != request["fingerprint"] or request["request"]["promptHash"] != request["promptHash"]:
                _fail("projection-invalid")
            op_fd = _open_op(runs_fd, request["op"])
            try:
                claimed = _read_receipt(op_fd, "claimed", op_id)
                if claimed is None or claimed["claimSha256"] != request["claimSha256"]:
                    _fail("claim-receipt-mismatch")
                _read_claim(op_fd, claimed)
                offered = _write_receipt(op_fd, "offered", op_id, {
                    "version": 1, "requestId": op_id, "handle": request["handle"],
                    "claimSha256": request["claimSha256"], "fingerprint": request["fingerprint"],
                    "promptHash": request["promptHash"], "request": request["request"], "offeredAt": _utc_now(),
                })
                if offered["handle"] != request["handle"] or offered["request"] != request["request"]:
                    _fail("offer-already-exists")
                return {"ok": True, "status": "offered", "handle": offered["handle"], "request": offered["request"]}
            finally:
                os.close(op_fd)
        if action == "mark-invalid":
            if set(request) != common | {"op", "code"} or request.get("code") != "contract-invalid":
                _fail("input-contract-invalid")
            match = OP_RE.fullmatch(str(request["op"] or ""))
            if not match:
                _fail("input-contract-invalid")
            op_fd = _open_op(runs_fd, request["op"])
            try:
                _write_receipt(op_fd, "invalid", match.group(1), {
                    "version": 1, "requestId": match.group(1), "code": "contract-invalid", "invalidAt": _utc_now(),
                })
                return {"ok": True, "status": "retained", "code": "contract-invalid"}
            finally:
                os.close(op_fd)
        if action in ("load", "phase", "restore", "consume"):
            extra = {"handle"}
            if action == "phase": extra |= {"phase", "fence"}
            if action == "consume": extra |= {"kind"}
            if set(request) != common | extra or not HANDLE_RE.fullmatch(str(request.get("handle") or "")):
                _fail("input-contract-invalid")
            op_name, op_id, op_fd, offered, claimed, phases, bytes_value = _load_handle(runs_fd, request["handle"])
            if action == "load":
                os.close(op_fd)
                return {
                    "ok": True, "status": "loaded", "id": op_id, "bytes": bytes_value.hex(),
                    "request": offered["request"],
                    "phases": {key: value is not None for key, value in phases.items()},
                    "executionFence": phases["execution"]["fence"] if phases["execution"] is not None else None,
                    "disclosureFence": phases["disclosed"]["fence"] if phases["disclosed"] is not None else None,
                }
            if action == "phase":
                phase = request.get("phase")
                if phase not in ("execution", "disclosed") or not _fence_shape(request.get("fence")):
                    os.close(op_fd); _fail("input-contract-invalid")
                if phases["restore"] is not None or phases["consume"] is not None:
                    os.close(op_fd); _fail("terminal-phase-active")
                if phase == "disclosed" and phases["execution"] is None:
                    os.close(op_fd); _fail("execution-receipt-required")
                existing = phases[phase]
                if existing is not None:
                    os.close(op_fd)
                    return {"ok": True, "status": "already-" + phase, "fence": existing["fence"]}
                value = {
                    "version": 1, "requestId": op_id, "handle": request["handle"],
                    "claimSha256": claimed["claimSha256"], "fence": request["fence"], phase + "At": _utc_now(),
                }
                published_phase = _write_receipt(op_fd, phase, op_id, value)
                phases[phase] = published_phase
                _validate_receipt_links(op_fd, op_id, claimed=claimed, offered=offered, phases=phases)
                os.close(op_fd)
                return {"ok": True, "status": phase, "fence": published_phase["fence"]}
            if action == "restore":
                if phases["disclosed"] is not None or phases["consume"] is not None:
                    os.close(op_fd); _fail("restore-after-disclosure")
                restore = phases["restore"] or _write_receipt(op_fd, "restore", op_id, {
                    "version": 1, "requestId": op_id, "handle": request["handle"],
                    "claimSha256": claimed["claimSha256"], "claimProof": claimed["claimProof"],
                    "targetName": op_id + ".json", "restoreAt": _utc_now(),
                })
                phases["restore"] = restore
                _validate_receipt_links(op_fd, op_id, claimed=claimed, offered=offered, phases=phases)
                result = _recover_restore(requests_fd, runs_fd, op_fd, op_name, op_id, restore)
                op_fd = None
                return {"ok": True, **result}
            if action == "consume":
                kind = request.get("kind")
                if kind not in ("executed", "superseded"):
                    os.close(op_fd); _fail("input-contract-invalid")
                if phases["restore"] is not None:
                    os.close(op_fd); _fail("restore-phase-active")
                if kind == "executed" and phases["disclosed"] is None:
                    os.close(op_fd); _fail("disclosure-receipt-required")
                consume = phases["consume"] or _write_receipt(op_fd, "consume", op_id, {
                    "version": 1, "requestId": op_id, "handle": request["handle"],
                    "claimSha256": claimed["claimSha256"], "claimProof": claimed["claimProof"],
                    "kind": kind, "consumeAt": _utc_now(),
                })
                if consume["kind"] != kind:
                    os.close(op_fd); _fail("consume-kind-mismatch")
                phases["consume"] = consume
                _validate_receipt_links(op_fd, op_id, claimed=claimed, offered=offered, phases=phases)
                result = _recover_consume(runs_fd, op_fd, op_name, op_id, consume)
                op_fd = None
                return {"ok": True, **result}
            os.close(op_fd)
        _fail("input-action-invalid")
    finally:
        os.close(runs_fd)
        os.close(requests_fd)
        os.close(root_fd)


def main() -> int:
    try:
        raw = sys.stdin.buffer.read(INPUT_MAX + 1)
        if not raw or len(raw) > INPUT_MAX:
            _fail("input-size-invalid")
        try:
            request = _strict_json_loads(raw.decode("utf-8", "strict"))
        except (UnicodeError, json.JSONDecodeError, ValueError):
            _fail("input-json-invalid")
        response = _dispatch(request)
        encoded = _canonical(response)
        if len(encoded) > OUTPUT_MAX:
            _fail("output-size-invalid")
        sys.stdout.buffer.write(encoded)
        return 0
    except BoundaryError as error:
        encoded = _canonical({"ok": False, "code": error.code})
        sys.stdout.buffer.write(encoded)
        return 2
    except Exception:
        encoded = _canonical({"ok": False, "code": "boundary-internal-error"})
        sys.stdout.buffer.write(encoded)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
