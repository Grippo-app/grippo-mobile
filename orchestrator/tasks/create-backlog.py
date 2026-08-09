#!/usr/bin/env python3
"""Deterministic, crash-recoverable backlog task creation.

Protocol (stdout is deliberately machine-readable):

    READY\n
    <default mode: read one JSON object from stdin until EOF>
    {"ok":true,...}\n        # or {"ok":false,"error":{...}}\n
Recovery mode (`--recover-all`) emits READY, then waits for an empty stdin EOF
readiness signal before any mutex acquisition or writer scan.  The controller
closes that pipe only after durably binding the helper PID to its writer lease.
Recovery then validates the entire receipt set and resumes every incomplete
marker from its bound intent.
The idempotency receipt is also the write-ahead recovery marker.  A process
crash releases the kernel mutex automatically; retrying the same key resumes
the recorded number/stem and reconciles every already-observed effect.

Production defaults are resolved from this file's repository.  Tests and
vendored launchers may override roots without patching the script:

  CREATE_BACKLOG_PROJECT_ROOT   repository root
  CREATE_BACKLOG_TASKS_DIR      task directory (normally <root>/orchestrator/tasks)
  CREATE_BACKLOG_CACHE_DIR      task cache (normally <root>/orchestrator/.cache/tasks)
  CREATE_BACKLOG_REGEN_INDEX    canonical regen-index.py path
  CREATE_BACKLOG_TASK_VALIDATOR canonical validate-task-state.mjs path
  CREATE_BACKLOG_FINALIZATIONS_DIR / CREATE_BACKLOG_OWN_WRITER_LEASE_ID
                                authenticated controller-owned writer state
  CREATE_BACKLOG_PARENT_STEM / ORCHESTRATOR_WRITER_SESSION_ID
  ORCHESTRATOR_WRITER_LEASE_ID / _DELEGATION_TOKEN
                                inherited attached site parent authority;
                                token plaintext exists only in child environment
  CREATE_BACKLOG_PARENT_WRITER_LEASE_ID / _TOKEN / _SESSION_ID
                                exact bounded standby/direct parent receipt;
                                nested creation supplements either parent with
                                a global deterministic publication guard
  CREATE_BACKLOG_NODE           Node runtime used for the shared lease scan
  CREATE_BACKLOG_MUTEX_TIMEOUT_MS
  CREATE_BACKLOG_FAILPOINT      after-marker|after-number|mid-task-write|
                                after-task-candidate|after-task-link|
                                after-task-proof|after-task-cleanup|after-file|
                                after-index|after-complete|
                                after-stale-guard-reconcile

No shell is invoked.  The canonical regen-index.py is executed with runpy in
this Python process, first in write mode and then with --check.
"""

from __future__ import annotations

import contextlib
import ctypes
import base64
import datetime as _datetime
import errno
import hashlib
import io
import json
import os
import re
import runpy
import secrets
import socket
import stat
import subprocess
import sys
import time
import unicodedata
import urllib.parse
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple

if os.name == "nt":  # pragma: no cover - exercised by the same contract on Windows
    import msvcrt
else:
    import fcntl


VERSION = 1
MARKER_VERSION = 2
MAX_REQUEST_BYTES = 256 * 1024
TITLE_MAX_CHARS = 200
TITLE_MAX_BYTES = 512
BODY_MAX_CHARS = 64 * 1024
BODY_MAX_BYTES = 64 * 1024
KEY_MAX = 240
STEM_MAX = 120
SLUG_MAX = 80
# Incomplete markers carry the canonical request intent so startup recovery can
# resume without the original client key/body.  Completed receipts shed that
# intent but retain payloadHash for replay/conflict detection.
MARKER_MAX_BYTES = MAX_REQUEST_BYTES + 32 * 1024
INDEX_MAX_BYTES = 8 * 1024 * 1024
TASK_MAX_BYTES = BODY_MAX_BYTES + 4096
MAX_SAFE_TASK_NUMBER = 9_007_199_254_740_991
MAX_RUNTIME_ENTRIES = 10_000
MAX_MARKER_CORPUS_BYTES = 8 * 1024 * 1024
MAX_CREATION_STAGE_CORPUS_BYTES = 8 * 1024 * 1024

KEY_RE = re.compile(r"^[A-Za-z0-9_.:-]{16,240}$")
DEDUP_KEY_RE = re.compile(r"^[A-Za-z0-9_.:-]{1,240}$")
HASH_RE = re.compile(r"^sha256:[a-f0-9]{64}$")
SOURCE_TYPE_RE = re.compile(r"^[a-z][a-z0-9-]{0,63}$")
SOURCE_FIELDS = {"kind", "type", "ref", "fingerprint"}
SOURCE_TYPES = {
    "manual": {"manual", "architecture-finding"},
    "figma": {"design-finding", "figma-drift", "figma-missing-component", "figma-component-split"},
    "api": {"api-missing", "api-change", "api-mismatch", "api-work-package"},
    "follow-up": {"outcome-follow-up", "reviewer-follow-up", "task-split", "test-foundation-prerequisite"},
}
STEM_RE = re.compile(r"^TASK_([0-9]+)_([A-Za-z0-9_]+)$")
TASK_FILE_RE = re.compile(r"^TASK_([0-9]+)_(.+)\.md$")
QUESTIONS_FILE_RE = re.compile(r"^TASK_([0-9]+)_(.+)\.questions\.md$")
SENTINEL_RE = re.compile(r"^([0-9]+)\.lock$")
MARKER_NAME_RE = re.compile(r"^[a-f0-9]{64}\.json$")
TX_RE = re.compile(r"^[a-f0-9]{32}$")
CREATION_STAGE_RE = re.compile(r"^\.create-([a-f0-9]{32})\.(partial|candidate)$")
LEASE_ID_RE = re.compile(r"^wr-[A-Za-z0-9][A-Za-z0-9._-]{15,159}$")
SESSION_ID_RE = re.compile(r"^ws-[A-Za-z0-9][A-Za-z0-9._-]{15,159}$")
WRITER_TOKEN_RE = re.compile(r"^[a-f0-9]{32,128}$")
PROCESS_START_ID_RE = re.compile(r"^psid-v1:(?:linux|darwin|win32):[a-f0-9]{64}$")
DELEGATION_TOKEN_RE = re.compile(r"^[a-f0-9]{48}$")
TIMESTAMP_RE = re.compile(r"^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:\.[0-9]{1,6})?Z$")
FAILPOINTS = {
    "after-marker", "after-number", "mid-task-write", "after-task-candidate",
    "after-task-link", "after-task-proof", "after-task-cleanup", "after-file",
    "after-index", "after-complete", "after-stale-guard-reconcile"
}
PHASES = {
    "claimed", "reserving-number", "number-reserved", "publishing-file",
    "file-published", "regenerating-index", "index-published", "verifying",
    "completed",
}
MARKER_BASE_FIELDS = {
    "version", "transactionId", "keyHash", "payloadHash", "intent",
    "status", "phase", "effect", "number", "slug", "stem", "sourceHash",
    "column", "createdAt", "updatedAt", "revision", "lastError",
}
MARKER_FIELDS = MARKER_BASE_FIELDS | {"targetProof"}


class CreateError(Exception):
    def __init__(self, code: str, message: str, *, exit_code: int = 1,
                 recoverable: bool = False, details: Optional[dict] = None):
        super().__init__(message)
        self.code = code
        self.exit_code = exit_code
        self.recoverable = recoverable
        self.details = details or {}


def fail(code: str, message: str, *, exit_code: int = 1,
         recoverable: bool = False, details: Optional[dict] = None) -> None:
    raise CreateError(code, message, exit_code=exit_code,
                      recoverable=recoverable, details=details)


def now() -> str:
    return _datetime.datetime.now(_datetime.timezone.utc).isoformat().replace("+00:00", "Z")


def sha256(data: bytes) -> str:
    return "sha256:" + hashlib.sha256(data).hexdigest()


def canonical_json(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True,
                      separators=(",", ":")).encode("utf-8")


def bounded(value: Any, limit: int = 1200) -> str:
    text = str(value)
    return text if len(text) <= limit else text[:limit - 1] + "…"


def bounded_directory_names(directory: Path, *, code: str,
                            recoverable: bool = False,
                            limit: int = MAX_RUNTIME_ENTRIES) -> List[str]:
    """Enumerate lazily, reject partial/changed snapshots, and cap work."""
    fd: Optional[int] = None
    try:
        fd = open_directory_anchored(directory, code=code)
        before = os.fstat(fd)
        names: List[str] = []
        with os.scandir(fd) as entries:
            for entry in entries:
                if len(names) >= limit:
                    fail(code, f"directory exceeds the {limit}-entry limit: {directory}", recoverable=recoverable)
                names.append(entry.name)
        after = os.fstat(fd)
        live = directory.lstat()
    except CreateError:
        raise
    except OSError as exc:
        fail(code, f"cannot scan {directory}: {bounded(exc)}", recoverable=recoverable)
    finally:
        if fd is not None:
            try:
                os.close(fd)
            except OSError:
                pass
    before_snapshot = (before.st_dev, before.st_ino, before.st_mode, before.st_size,
                       before.st_mtime_ns, before.st_ctime_ns)
    after_snapshot = (after.st_dev, after.st_ino, after.st_mode, after.st_size,
                      after.st_mtime_ns, after.st_ctime_ns)
    if (stat.S_ISLNK(live.st_mode) or not stat.S_ISDIR(live.st_mode) or
            (live.st_dev, live.st_ino) != (after.st_dev, after.st_ino) or
            before_snapshot != after_snapshot):
        fail(code, f"directory changed while being scanned: {directory}", recoverable=recoverable)
    return sorted(names)


def valid_iso8601(value: Any) -> bool:
    if not isinstance(value, str) or not TIMESTAMP_RE.fullmatch(value):
        return False
    try:
        _datetime.datetime.fromisoformat(value.removesuffix("Z") + "+00:00")
        return True
    except ValueError:
        return False


HERE = Path(__file__).resolve().parent
DEFAULT_PROJECT_ROOT = HERE.parent.parent


def absolute_path(value: Any) -> Path:
    # Deliberately do not realpath() caller-controlled roots.  Resolving here
    # would erase the evidence that TASKS_DIR/CACHE_DIR itself is a symlink
    # before ensure_real_dir gets a chance to reject it.
    return Path(os.path.abspath(os.fspath(value)))


PROJECT_ROOT = absolute_path(os.environ.get("CREATE_BACKLOG_PROJECT_ROOT", DEFAULT_PROJECT_ROOT))
AUTHORITY_ROOT = absolute_path(os.environ.get("CREATE_BACKLOG_AUTHORITY_ROOT", PROJECT_ROOT))
TASKS_DIR = absolute_path(os.environ.get("CREATE_BACKLOG_TASKS_DIR", PROJECT_ROOT / "orchestrator" / "tasks"))
CACHE_DIR = absolute_path(os.environ.get("CREATE_BACKLOG_CACHE_DIR", PROJECT_ROOT / "orchestrator" / ".cache" / "tasks"))
CREATIONS_DIR = CACHE_DIR / "creations"
TASKNO_DIR = CACHE_DIR / ".taskno"
MUTEX_PATH = CREATIONS_DIR / ".mutex"
REGEN_INDEX = absolute_path(os.environ.get("CREATE_BACKLOG_REGEN_INDEX", HERE / "regen-index.py"))
FINALIZATIONS_DIR = absolute_path(os.environ.get("CREATE_BACKLOG_FINALIZATIONS_DIR", CACHE_DIR / "finalizations"))
WRITER_SCAN_SCRIPT = absolute_path(os.environ.get("CREATE_BACKLOG_WRITER_SCAN_SCRIPT", HERE / "writer-lease.mjs"))
TASK_STATE_VALIDATOR = absolute_path(os.environ.get("CREATE_BACKLOG_TASK_VALIDATOR", HERE / "validate-task-state.mjs"))
WRITER_SCAN_MAX_BYTES = 256 * 1024
WRITER_SCAN_TIMEOUT_SECONDS = 10
TASK_STATE_MAX_BYTES = 2 * 1024 * 1024
TASK_STATE_TIMEOUT_SECONDS = 30
_PROCESS_BOOT_ID: Optional[str] = None


def _process_start_digest(platform: str, *parts: str) -> str:
    canonical = "\0".join(("writer-process-v1", platform, *parts)).encode("utf-8")
    return f"psid-v1:{platform}:" + hashlib.sha256(canonical).hexdigest()


def _bounded_kernel_read(path: Path, limit: int) -> bytes:
    flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)
    fd = os.open(path, flags)
    try:
        chunks: List[bytes] = []
        total = 0
        while True:
            chunk = os.read(fd, min(4096, limit + 1 - total))
            if not chunk:
                return b"".join(chunks)
            total += len(chunk)
            if total > limit:
                raise ValueError("process metadata exceeds the size limit")
            chunks.append(chunk)
    finally:
        os.close(fd)


def _process_boot_id(platform: str) -> str:
    global _PROCESS_BOOT_ID
    if _PROCESS_BOOT_ID is not None:
        return _PROCESS_BOOT_ID
    if platform == "linux":
        value = _bounded_kernel_read(
            Path("/proc/sys/kernel/random/boot_id"), 128
        ).decode("ascii").strip().lower()
        if not re.fullmatch(r"[a-f0-9-]{16,80}", value):
            raise ValueError("Linux boot identity is invalid")
        _PROCESS_BOOT_ID = value
        return value
    result = subprocess.run(
        ["/usr/sbin/sysctl", "-n", "kern.bootsessionuuid"],
        stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        timeout=1, check=False, env={
            "PATH": "/usr/bin:/bin:/usr/sbin:/sbin", "LANG": "C", "LC_ALL": "C",
        },
    )
    if result.returncode != 0 or len(result.stdout) > 16 * 1024 or len(result.stderr) > 16 * 1024:
        raise ValueError("cannot read Darwin boot identity")
    value = result.stdout.decode("ascii").strip().lower()
    if not re.fullmatch(
            r"[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", value):
        raise ValueError("Darwin boot identity is invalid")
    _PROCESS_BOOT_ID = value
    return _PROCESS_BOOT_ID


def process_start_identity(pid: int) -> Optional[str]:
    """Return the same bounded process-generation identity as writer-leases.cjs."""
    if not isinstance(pid, int) or pid <= 0 or sys.platform not in ("linux", "darwin", "win32"):
        return None
    try:
        if sys.platform == "win32":
            helper = HERE / "windows-runtime-proof.py"
            env = {key: os.environ[key] for key in
                   ("SystemRoot", "WINDIR", "PATH", "PATHEXT", "TEMP", "TMP")
                   if key in os.environ}
            env.update({"PYTHONIOENCODING": "utf-8:strict", "PYTHONUTF8": "1"})
            result = subprocess.run(
                [sys.executable, "-I", "-B", str(helper), "process", str(pid)],
                cwd=os.fspath(Path(os.environ.get("TEMP") or os.environ.get("TMP") or HERE)),
                env=env, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                stderr=subprocess.PIPE, timeout=5, check=False,
            )
            if result.returncode != 0 or len(result.stdout) > 16 * 1024:
                return None
            value = json.loads(result.stdout.decode("utf-8"))
            canonical = (json.dumps(value, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")
            if result.stdout != canonical or set(value) != {
                    "pid", "processStartId", "reason", "status", "version"}:
                return None
            identity = value.get("processStartId")
            return identity if (value.get("version") == 1 and value.get("pid") == pid and
                                value.get("status") == "live" and value.get("reason") == "ok" and
                                PROCESS_START_ID_RE.fullmatch(str(identity or ""))) else None
        if sys.platform == "linux":
            raw = _bounded_kernel_read(Path(f"/proc/{pid}/stat"), 16 * 1024).decode("utf-8").strip()
            close = raw.rfind(")")
            if close <= 0 or not re.fullmatch(r"\d+ \(.*\)", raw[:close + 1], flags=re.S):
                return None
            if int(raw.split(" ", 1)[0]) != pid:
                return None
            fields = raw[close + 1:].strip().split()
            if len(fields) < 20 or not all(fields[index].isdigit() for index in (1, 2, 19)):
                return None
            return _process_start_digest("linux", _process_boot_id("linux"), str(pid), fields[19])
        class ProcBsdInfo(ctypes.Structure):
            _fields_ = [
                ("flags", ctypes.c_uint32), ("status", ctypes.c_uint32),
                ("xstatus", ctypes.c_uint32), ("pid", ctypes.c_uint32),
                ("ppid", ctypes.c_uint32), ("uid", ctypes.c_uint32),
                ("gid", ctypes.c_uint32), ("ruid", ctypes.c_uint32),
                ("rgid", ctypes.c_uint32), ("svuid", ctypes.c_uint32),
                ("svgid", ctypes.c_uint32), ("rfu", ctypes.c_uint32),
                ("comm", ctypes.c_char * 16), ("name", ctypes.c_char * 32),
                ("nfiles", ctypes.c_uint32), ("pgid", ctypes.c_uint32),
                ("pjobc", ctypes.c_uint32), ("tdev", ctypes.c_uint32),
                ("tpgid", ctypes.c_uint32), ("nice", ctypes.c_int32),
                ("sec", ctypes.c_uint64), ("usec", ctypes.c_uint64),
            ]
        info = ProcBsdInfo()
        proc_pidinfo = ctypes.CDLL("/usr/lib/libproc.dylib", use_errno=True).proc_pidinfo
        proc_pidinfo.argtypes = [ctypes.c_int, ctypes.c_int, ctypes.c_uint64,
                                 ctypes.c_void_p, ctypes.c_int]
        proc_pidinfo.restype = ctypes.c_int
        size = ctypes.sizeof(info)
        if (size != 136 or proc_pidinfo(pid, 3, 0, ctypes.byref(info), size) != 136 or
                info.pid != pid or info.sec <= 0 or not 0 <= info.usec < 1_000_000):
            return None
        return _process_start_digest(
            "darwin", _process_boot_id("darwin"), str(pid), str(info.sec), str(info.usec)
        )
    except (AttributeError, FileNotFoundError, PermissionError, OSError, UnicodeError, ValueError,
            subprocess.SubprocessError):
        return None


def sync_dir(path: Path) -> None:
    try:
        fd = open_directory_anchored(path, code="DIRECTORY_SYNC_FAILED")
    except (CreateError, OSError):
        return
    try:
        os.fsync(fd)
    except OSError:
        pass
    finally:
        os.close(fd)


def ensure_real_dir(path: Path, *, create: bool, code: str = "UNSAFE_DIRECTORY",
                    root: Optional[Path] = None) -> None:
    """Walk a directory beneath an explicit trust root without following links.

    pathlib.mkdir(parents=True) follows a symlink hidden in any ancestor.  All
    authority-bearing roots instead create one lexical component at a time,
    lstat every component, and recheck the captured inode chain before return.
    The root itself is trusted by configuration but must still be a real dir.
    """
    path = absolute_path(path)
    root = absolute_path(root or AUTHORITY_ROOT)
    try:
        relative = path.relative_to(root)
    except ValueError:
        fail(code, f"directory escapes its authority root {root}: {path}")
    identities: List[Tuple[Path, int, int]] = []
    try:
        root_st = root.lstat()
        if stat.S_ISLNK(root_st.st_mode) or not stat.S_ISDIR(root_st.st_mode):
            fail(code, f"authority root must be a real directory: {root}")
        identities.append((root, root_st.st_dev, root_st.st_ino))
        current = root
        for part in relative.parts:
            if part in ("", ".", ".."):
                fail(code, f"directory path has an unsafe component: {path}")
            current = current / part
            try:
                st = current.lstat()
            except FileNotFoundError:
                if not create:
                    fail(code, f"required directory is missing: {current}")
                try:
                    current.mkdir(mode=0o700)
                except FileExistsError:
                    pass
                st = current.lstat()
            if stat.S_ISLNK(st.st_mode) or not stat.S_ISDIR(st.st_mode):
                fail(code, f"directory component must be real, not a symlink or special file: {current}")
            identities.append((current, st.st_dev, st.st_ino))
        for candidate, expected_dev, expected_ino in identities:
            observed = candidate.lstat()
            if (stat.S_ISLNK(observed.st_mode) or not stat.S_ISDIR(observed.st_mode) or
                    observed.st_dev != expected_dev or observed.st_ino != expected_ino):
                fail(code, f"directory ancestry changed identity while being checked: {candidate}")
    except FileNotFoundError:
        fail(code, f"required directory is missing: {path}")
    except OSError as exc:
        fail(code, f"cannot prepare directory {path}: {bounded(exc)}")


def open_nofollow(path: Path, flags: int, mode: int = 0o600) -> int:
    if hasattr(os, "O_NOFOLLOW"):
        flags |= os.O_NOFOLLOW
    try:
        return os.open(path, flags, mode)
    except OSError as exc:
        if exc.errno in (errno.ELOOP, errno.EMLINK):
            fail("UNSAFE_FILE", f"refusing symlink path: {path}")
        raise


def open_directory_anchored(path: Path, *, code: str) -> int:
    """Open an absolute directory one component at a time without symlinks.

    Returning the final directory descriptor makes every subsequent file
    operation independent of pathname ancestor swaps.  The final pathname is
    rechecked against that descriptor so a concurrent detach/swap fails closed
    instead of silently redirecting authority.
    """
    path = absolute_path(path)
    flags = os.O_RDONLY | getattr(os, "O_DIRECTORY", 0) | getattr(os, "O_NOFOLLOW", 0)
    fd: Optional[int] = None
    try:
        try:
            relative = path.relative_to(AUTHORITY_ROOT)
            anchor = AUTHORITY_ROOT
            parts = relative.parts
        except ValueError:
            # Executable/script inputs may intentionally live outside the
            # workspace authority root.  Open the complete configured path and
            # still reject a symlink at that authority boundary; system aliases
            # such as macOS /var -> /private/var are outside our control.
            anchor = path
            parts = ()
        fd = os.open(anchor, flags)
        for part in parts:
            if part in ("", ".", ".."):
                fail(code, f"directory has an unsafe component: {path}", recoverable=True)
            next_fd = os.open(part, flags, dir_fd=fd)
            os.close(fd)
            fd = next_fd
        opened = os.fstat(fd)
        live = path.lstat()
        if (not stat.S_ISDIR(opened.st_mode) or stat.S_ISLNK(live.st_mode) or
                not stat.S_ISDIR(live.st_mode) or
                (opened.st_dev, opened.st_ino) != (live.st_dev, live.st_ino)):
            fail(code, f"directory changed identity while being opened: {path}", recoverable=True)
        return fd
    except CreateError:
        if fd is not None:
            os.close(fd)
        raise
    except OSError as exc:
        if fd is not None:
            try:
                os.close(fd)
            except OSError:
                pass
        fail(code, f"cannot open directory safely {path}: {bounded(exc)}", recoverable=True)
    raise AssertionError("unreachable")


def _proof_from_stat(st: os.stat_result, data: bytes) -> dict:
    # JavaScript cannot represent 64-bit inode/device identities or nanosecond
    # timestamps losslessly as JSON numbers.  Keep the wire proof canonical and
    # exact across Python/JS by serializing those four fields as canonical
    # decimal strings (device/inode unsigned; timestamps may be pre-epoch).
    # Mode and bounded file size remain safe JSON integers.
    return {
        "dev": str(st.st_dev), "ino": str(st.st_ino),
        "mode": stat.S_IMODE(st.st_mode), "size": st.st_size,
        "mtimeNs": str(st.st_mtime_ns), "ctimeNs": str(st.st_ctime_ns),
        "hash": sha256(data),
    }


def _same_generation(left: dict, right: dict, *, after_rename: bool = False) -> bool:
    fields = ("dev", "ino", "mode", "size", "mtimeNs", "hash")
    if not after_rename:
        fields += ("ctimeNs",)
    return all(left.get(field) == right.get(field) for field in fields)


def read_regular_proof(path: Path, *, max_bytes: int, code: str,
                       required: bool = True) -> Tuple[Optional[bytes], Optional[dict]]:
    parent_fd: Optional[int] = None
    fd: Optional[int] = None
    try:
        parent_fd = open_directory_anchored(path.parent, code=code)
        flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)
        try:
            fd = os.open(path.name, flags, dir_fd=parent_fd)
        except FileNotFoundError:
            if required:
                fail(code, f"required file is missing: {path}")
            return None, None
        opened = os.fstat(fd)
        if not stat.S_ISREG(opened.st_mode):
            fail(code, f"file must be regular, not a symlink or special file: {path}")
        if opened.st_size > max_bytes:
            fail(code, f"file exceeds {max_bytes} bytes: {path}")
        chunks: List[bytes] = []
        total = 0
        while True:
            chunk = os.read(fd, min(65536, max_bytes + 1 - total))
            if not chunk:
                break
            total += len(chunk)
            if total > max_bytes:
                fail(code, f"file exceeds {max_bytes} bytes: {path}")
            chunks.append(chunk)
        data = b"".join(chunks)
        after_fd = os.fstat(fd)
        live = os.stat(path.name, dir_fd=parent_fd, follow_symlinks=False)
        if (not stat.S_ISREG(live.st_mode) or
                _proof_from_stat(opened, data) != _proof_from_stat(after_fd, data) or
                (after_fd.st_dev, after_fd.st_ino) != (live.st_dev, live.st_ino)):
            fail(code, f"file changed while being read: {path}")
        parent_live = path.parent.lstat()
        parent_opened = os.fstat(parent_fd)
        if (stat.S_ISLNK(parent_live.st_mode) or not stat.S_ISDIR(parent_live.st_mode) or
                (parent_live.st_dev, parent_live.st_ino) !=
                (parent_opened.st_dev, parent_opened.st_ino)):
            fail(code, f"file parent changed identity while being read: {path.parent}")
        return data, _proof_from_stat(after_fd, data)
    except CreateError:
        raise
    except OSError as exc:
        fail(code, f"cannot read {path}: {bounded(exc)}")
    finally:
        if fd is not None:
            try:
                os.close(fd)
            except OSError:
                pass
        if parent_fd is not None:
            try:
                os.close(parent_fd)
            except OSError:
                pass
    raise AssertionError("unreachable")


def read_regular(path: Path, *, max_bytes: int, code: str,
                 required: bool = True) -> Optional[bytes]:
    data, _proof = read_regular_proof(
        path, max_bytes=max_bytes, code=code, required=required)
    return data


def read_regular_proof_at(directory_fd: int, name: str, *, max_bytes: int,
                          code: str, required: bool = True) -> Tuple[Optional[bytes], Optional[dict]]:
    """Read one no-follow regular file from an already pinned directory."""
    if not isinstance(name, str) or name in ("", ".", "..") or "/" in name or "\x00" in name:
        fail(code, "anchored file name is unsafe", recoverable=True)
    fd: Optional[int] = None
    try:
        try:
            fd = os.open(name, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0), dir_fd=directory_fd)
        except FileNotFoundError:
            if required:
                fail(code, f"required anchored file is missing: {name}", recoverable=True)
            return None, None
        opened = os.fstat(fd)
        if not stat.S_ISREG(opened.st_mode) or opened.st_size > max_bytes:
            fail(code, f"anchored file is not a bounded regular file: {name}", recoverable=True)
        chunks: List[bytes] = []
        total = 0
        while True:
            chunk = os.read(fd, min(65536, max_bytes + 1 - total))
            if not chunk:
                break
            total += len(chunk)
            if total > max_bytes:
                fail(code, f"anchored file exceeds {max_bytes} bytes: {name}", recoverable=True)
            chunks.append(chunk)
        data = b"".join(chunks)
        after_fd = os.fstat(fd)
        live = os.stat(name, dir_fd=directory_fd, follow_symlinks=False)
        opened_proof = _proof_from_stat(opened, data)
        after_proof = _proof_from_stat(after_fd, data)
        if (not stat.S_ISREG(live.st_mode) or opened_proof != after_proof or
                (live.st_dev, live.st_ino, live.st_mode, live.st_size,
                 live.st_mtime_ns, live.st_ctime_ns) !=
                (after_fd.st_dev, after_fd.st_ino, after_fd.st_mode, after_fd.st_size,
                 after_fd.st_mtime_ns, after_fd.st_ctime_ns)):
            fail(code, f"anchored file changed while being read: {name}", recoverable=True)
        return data, after_proof
    except CreateError:
        raise
    except OSError as exc:
        fail(code, f"cannot read anchored file {name}: {bounded(exc)}", recoverable=True)
    finally:
        if fd is not None:
            try:
                os.close(fd)
            except OSError:
                pass
    raise AssertionError("unreachable")


def write_exclusive(path: Path, data: bytes, *, mode: int = 0o600) -> None:
    ensure_real_dir(path.parent, create=True)
    parent_fd: Optional[int] = None
    fd: Optional[int] = None
    created = False
    complete = False
    try:
        parent_fd = open_directory_anchored(path.parent, code="UNSAFE_DIRECTORY")
        flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0)
        fd = os.open(path.name, flags, mode, dir_fd=parent_fd)
        created = True
        view = memoryview(data)
        while view:
            written = os.write(fd, view)
            if written <= 0:
                raise OSError("short write")
            view = view[written:]
        os.fsync(fd)
        complete = True
    finally:
        if fd is not None:
            try:
                os.close(fd)
            except OSError:
                pass
        if created and not complete:
            try:
                os.unlink(path.name, dir_fd=parent_fd)
            except OSError:
                pass
        if parent_fd is not None:
            try:
                os.close(parent_fd)
            except OSError:
                pass


def publish_exclusive(path: Path, data: bytes, *, mode: int = 0o600) -> None:
    """Publish complete bytes with O_EXCL semantics and no partial target."""
    ensure_real_dir(path.parent, create=True)
    temp = path.parent / f".{path.name}.claim.{os.getpid()}.{secrets.token_hex(6)}"
    parent_fd: Optional[int] = None
    staged = False
    try:
        write_exclusive(temp, data, mode=mode)
        staged = True
        parent_fd = open_directory_anchored(path.parent, code="UNSAFE_DIRECTORY")
        os.link(temp.name, path.name, src_dir_fd=parent_fd, dst_dir_fd=parent_fd,
                follow_symlinks=False)  # first claimant wins; never overwrites
        os.fsync(parent_fd)
        sync_dir(path.parent)
    finally:
        if staged:
            try:
                if parent_fd is None:
                    parent_fd = open_directory_anchored(path.parent, code="UNSAFE_DIRECTORY")
                os.unlink(temp.name, dir_fd=parent_fd)
                sync_dir(temp.parent)
            except (OSError, CreateError):
                pass
        if parent_fd is not None:
            try:
                os.close(parent_fd)
            except OSError:
                pass


CAS_PREFIX = ".durable-cas-"
CAS_NAME_RE = re.compile(r"^\.durable-cas-[a-f0-9]{16}-[a-f0-9]{16}-[a-f0-9]{16}$")
CAS_MANIFEST_FIELDS = {
    "version", "targetName", "owner", "expectedProof", "candidateHash", "maxBytes",
}
CAS_PROOF_FIELDS = {"dev", "ino", "mode", "size", "mtimeNs", "ctimeNs", "hash"}
CAS_UNSIGNED_DECIMAL_PROOF_FIELDS = {"dev", "ino"}
CAS_SIGNED_DECIMAL_PROOF_FIELDS = {"mtimeNs", "ctimeNs"}
CAS_UNSIGNED_DECIMAL_RE = re.compile(r"^(?:0|[1-9][0-9]{0,19})$")
CAS_SIGNED_DECIMAL_RE = re.compile(r"^-?(?:0|[1-9][0-9]{0,19})$")


def _cas_manifest_bytes(value: dict) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True,
                      separators=(",", ":")).encode("utf-8") + b"\n"


def _cas_operation_path(path: Path, owner: str) -> Path:
    target_digest = hashlib.sha256(path.name.encode("utf-8")).hexdigest()[:16]
    owner_digest = hashlib.sha256(owner.encode("utf-8")).hexdigest()[:16]
    return path.parent / f"{CAS_PREFIX}{target_digest}-{owner_digest}-{secrets.token_hex(8)}"


def _validate_cas_manifest(value: Any) -> dict:
    if not isinstance(value, dict) or set(value) != CAS_MANIFEST_FIELDS or value.get("version") != 1:
        fail("CAS_MANIFEST_INVALID", "durable CAS manifest fields are invalid", recoverable=True)
    target = value.get("targetName")
    if (not isinstance(target, str) or len(target) > 180 or target in ("", ".", "..") or
            "/" in target or "\\" in target or "\x00" in target):
        fail("CAS_MANIFEST_INVALID", "durable CAS target name is unsafe", recoverable=True)
    owner = value.get("owner")
    if not isinstance(owner, str) or not re.fullmatch(r"[A-Za-z0-9_.:-]{1,240}", owner):
        fail("CAS_MANIFEST_INVALID", "durable CAS owner is invalid", recoverable=True)
    proof = value.get("expectedProof")
    if not isinstance(proof, dict) or set(proof) != CAS_PROOF_FIELDS:
        fail("CAS_MANIFEST_INVALID", "durable CAS source proof is invalid", recoverable=True)
    for field in CAS_UNSIGNED_DECIMAL_PROOF_FIELDS:
        if not isinstance(proof.get(field), str) or not CAS_UNSIGNED_DECIMAL_RE.fullmatch(proof[field]):
            fail("CAS_MANIFEST_INVALID", f"durable CAS proof {field} is invalid", recoverable=True)
    for field in CAS_SIGNED_DECIMAL_PROOF_FIELDS:
        if not isinstance(proof.get(field), str) or not CAS_SIGNED_DECIMAL_RE.fullmatch(proof[field]) or proof[field] == "-0":
            fail("CAS_MANIFEST_INVALID", f"durable CAS proof {field} is invalid", recoverable=True)
    if (isinstance(proof.get("mode"), bool) or not isinstance(proof.get("mode"), int) or
            proof["mode"] < 0 or proof["mode"] > 0o7777 or
            isinstance(proof.get("size"), bool) or not isinstance(proof.get("size"), int) or
            proof["size"] < 0):
        fail("CAS_MANIFEST_INVALID", "durable CAS proof mode/size is invalid", recoverable=True)
    if not HASH_RE.fullmatch(str(proof.get("hash", ""))) or not HASH_RE.fullmatch(str(value.get("candidateHash", ""))):
        fail("CAS_MANIFEST_INVALID", "durable CAS hashes are invalid", recoverable=True)
    if (isinstance(value.get("maxBytes"), bool) or not isinstance(value.get("maxBytes"), int) or
            value["maxBytes"] < 1 or value["maxBytes"] > 16 * 1024 * 1024 or
            proof["size"] > value["maxBytes"]):
        fail("CAS_MANIFEST_INVALID", "durable CAS byte bound is invalid", recoverable=True)
    return value


def _read_cas_manifest_at(operation_fd: int) -> dict:
    raw, _proof = read_regular_proof_at(
        operation_fd, "manifest.json", max_bytes=16 * 1024,
        code="CAS_MANIFEST_INVALID")
    try:
        value = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail("CAS_MANIFEST_INVALID", f"durable CAS manifest is invalid JSON: {bounded(exc)}",
             recoverable=True)
    value = _validate_cas_manifest(value)
    if raw != _cas_manifest_bytes(value):
        fail("CAS_MANIFEST_INVALID", "durable CAS manifest is not canonical", recoverable=True)
    return value


def _cas_entries_at(operation_fd: int) -> Set[str]:
    before = os.fstat(operation_fd)
    scan_fd: Optional[int] = None
    names: List[str] = []
    try:
        scan_fd = os.open(".", os.O_RDONLY | getattr(os, "O_DIRECTORY", 0) |
                          getattr(os, "O_NOFOLLOW", 0), dir_fd=operation_fd)
        with os.scandir(scan_fd) as entries:
            for entry in entries:
                if len(names) >= 8:
                    fail("CAS_OPERATION_INVALID", "durable CAS operation exceeds eight artifacts",
                         recoverable=True)
                names.append(entry.name)
    except CreateError:
        raise
    except OSError as exc:
        fail("CAS_OPERATION_INVALID", f"cannot enumerate durable CAS operation: {bounded(exc)}",
             recoverable=True)
    finally:
        if scan_fd is not None:
            try:
                os.close(scan_fd)
            except OSError:
                pass
    after = os.fstat(operation_fd)
    if (before.st_dev, before.st_ino, before.st_mode, before.st_size,
            before.st_mtime_ns, before.st_ctime_ns) != (
            after.st_dev, after.st_ino, after.st_mode, after.st_size,
            after.st_mtime_ns, after.st_ctime_ns):
        fail("CAS_OPERATION_INVALID", "durable CAS operation changed while enumerating",
             recoverable=True)
    name_set = set(names)
    if any(name not in {"manifest.json", "candidate", "source"} and
           not re.fullmatch(r"\.(?:manifest|candidate)-partial-[a-f0-9]{16}", name)
           for name in name_set):
        fail("CAS_OPERATION_INVALID", "durable CAS operation has unexpected artifacts",
             recoverable=True)
    return name_set


def _operation_path_matches(operation: Path, operation_fd: int, parent_fd: int) -> bool:
    try:
        live = os.stat(operation.name, dir_fd=parent_fd, follow_symlinks=False)
    except FileNotFoundError:
        return False
    opened = os.fstat(operation_fd)
    return (stat.S_ISDIR(live.st_mode) and not stat.S_ISLNK(live.st_mode) and
            (live.st_dev, live.st_ino) == (opened.st_dev, opened.st_ino))


def _remove_cas_operation(operation: Path, operation_fd: int, *,
                          source_safe_to_remove: bool) -> None:
    names = _cas_entries_at(operation_fd)
    if "source" in names and not source_safe_to_remove:
        fail("CAS_CONFLICT", "durable CAS retained a source generation for manual-safe recovery",
             recoverable=True)
    parent_fd = open_directory_anchored(operation.parent, code="CAS_OPERATION_INVALID")
    try:
        if not _operation_path_matches(operation, operation_fd, parent_fd):
            fail("CAS_OPERATION_CHANGED",
                 "durable CAS operation path changed before cleanup", recoverable=True)
        ordered = (["source", "candidate", "manifest.json"] +
                   sorted(name for name in names if name.startswith(".")))
        for name in ordered:
            if name not in names:
                continue
            st = os.stat(name, dir_fd=operation_fd, follow_symlinks=False)
            if not stat.S_ISREG(st.st_mode):
                fail("CAS_OPERATION_INVALID", f"durable CAS artifact {name} is not regular",
                     recoverable=True)
            os.unlink(name, dir_fd=operation_fd)
        os.fsync(operation_fd)
        if not _operation_path_matches(operation, operation_fd, parent_fd):
            fail("CAS_OPERATION_CHANGED",
                 "durable CAS operation path changed during cleanup", recoverable=True)
        os.rmdir(operation.name, dir_fd=parent_fd)
        os.fsync(parent_fd)
    finally:
        os.close(parent_fd)


def _cas_link(operation_fd: int, source_name: str, target: Path) -> None:
    parent_fd = open_directory_anchored(target.parent, code="CAS_OPERATION_INVALID")
    try:
        os.link(source_name, target.name, src_dir_fd=operation_fd, dst_dir_fd=parent_fd,
                follow_symlinks=False)
        os.fsync(parent_fd)
    finally:
        os.close(parent_fd)


def _cas_detach(target: Path, operation_fd: int) -> None:
    parent_fd = open_directory_anchored(target.parent, code="CAS_OPERATION_INVALID")
    try:
        try:
            os.stat("source", dir_fd=operation_fd, follow_symlinks=False)
            fail("CAS_OPERATION_INVALID", "durable CAS source already exists before detach",
                 recoverable=True)
        except FileNotFoundError:
            pass
        os.rename(target.name, "source", src_dir_fd=parent_fd, dst_dir_fd=operation_fd)
        os.fsync(parent_fd)
        os.fsync(operation_fd)
    except FileNotFoundError:
        fail("CAS_SOURCE_CHANGED", f"CAS source vanished before detach: {target}", recoverable=True)
    finally:
        os.close(parent_fd)


def _fixture_mode_enabled(variable: str) -> bool:
    """Test knobs can never weaken the checked-out canonical workspace."""
    return (os.environ.get(variable) == "1" and PROJECT_ROOT != DEFAULT_PROJECT_ROOT and
            AUTHORITY_ROOT == PROJECT_ROOT)


def _fixture_swap_before_detach(target: Path, variable: Optional[str]) -> None:
    if not variable or not _fixture_mode_enabled(variable):
        return
    encoded = os.environ.get(variable + "_BASE64", "")
    try:
        data = base64.b64decode(encoded.encode("ascii"), validate=True)
    except (UnicodeEncodeError, ValueError):
        fail("TEST_HOOK_INVALID", f"{variable}_BASE64 is invalid", exit_code=2)
    temp = target.parent / f".fixture-swap-{secrets.token_hex(8)}"
    write_exclusive(temp, data)
    parent_fd = open_directory_anchored(target.parent, code="TEST_HOOK_INVALID")
    try:
        os.replace(temp.name, target.name, src_dir_fd=parent_fd, dst_dir_fd=parent_fd)
        os.fsync(parent_fd)
    finally:
        os.close(parent_fd)


def _fixture_claim_after_detach(target: Path, variable: Optional[str]) -> None:
    if not variable or not _fixture_mode_enabled(variable):
        return
    encoded = os.environ.get(variable + "_BASE64", "")
    try:
        data = base64.b64decode(encoded.encode("ascii"), validate=True)
    except (UnicodeEncodeError, ValueError):
        fail("TEST_HOOK_INVALID", f"{variable}_BASE64 is invalid", exit_code=2)
    try:
        publish_exclusive(target, data)
    except FileExistsError:
        fail("TEST_HOOK_INVALID", "fixture target was unexpectedly occupied", exit_code=2)


def _resume_cas_operation(operation: Path, *, detach_failpoint: Optional[str] = None,
                          detach_exit_code: int = 88,
                          test_after_detach_variable: Optional[str] = None,
                          allowed_targets: Optional[Set[str]] = None) -> None:
    operation_fd = open_directory_anchored(operation, code="CAS_OPERATION_INVALID")
    try:
        names = _cas_entries_at(operation_fd)
        if "manifest.json" not in names:
            if "source" in names:
                fail("CAS_OPERATION_INVALID", "durable CAS source has no manifest",
                     recoverable=True)
            # The operation was never armed: only random partial/candidate
            # bytes exist and the canonical source was never detached.
            _remove_cas_operation(
                operation, operation_fd, source_safe_to_remove=True)
            return
        manifest = _read_cas_manifest_at(operation_fd)
        if allowed_targets is not None and manifest["targetName"] not in allowed_targets:
            fail("CAS_RECOVERY_REQUIRED",
                 f"durable CAS recovery is required for {manifest['targetName']}",
                 recoverable=True)
        target = operation.parent / manifest["targetName"]
        if "candidate" not in names:
            if "source" in names:
                fail("CAS_OPERATION_INVALID", "durable CAS detached a source without a candidate",
                     recoverable=True)
            target_data, target_proof = read_regular_proof(
                target, max_bytes=manifest["maxBytes"], code="CAS_SOURCE_CHANGED", required=False)
            if target_proof is not None and (
                    _same_generation(target_proof, manifest["expectedProof"]) or
                    sha256(target_data) == manifest["candidateHash"]):
                _remove_cas_operation(
                    operation, operation_fd, source_safe_to_remove=True)
                return
            fail("CAS_OPERATION_INVALID",
                 "incomplete durable CAS staging no longer has its source generation",
                 recoverable=True)
        candidate, candidate_proof = read_regular_proof_at(
            operation_fd, "candidate", max_bytes=manifest["maxBytes"],
            code="CAS_CANDIDATE_INVALID")
        if candidate_proof["hash"] != manifest["candidateHash"]:
            fail("CAS_CANDIDATE_INVALID", "durable CAS candidate hash changed", recoverable=True)

        source_present = "source" in names
        target_data, target_proof = read_regular_proof(
            target, max_bytes=manifest["maxBytes"], code="CAS_SOURCE_CHANGED", required=False)
        if not source_present:
            if target_proof is not None and _same_generation(
                    target_proof, candidate_proof, after_rename=True):
                _remove_cas_operation(
                    operation, operation_fd, source_safe_to_remove=True)
                return
            if target_proof is None or not _same_generation(
                    target_proof, manifest["expectedProof"]):
                _remove_cas_operation(
                    operation, operation_fd, source_safe_to_remove=True)
                fail("CAS_SOURCE_CHANGED", "canonical CAS source is not the expected exact generation",
                     recoverable=True)
            # Reconfirm the entire generation immediately before the
            # destructive rename.  The detached inode/hash proof is checked
            # again below from the same pinned operation directory.
            reconfirmed_data, reconfirmed_proof = read_regular_proof(
                target, max_bytes=manifest["maxBytes"], code="CAS_SOURCE_CHANGED")
            if reconfirmed_data != target_data or not _same_generation(
                    reconfirmed_proof, manifest["expectedProof"]):
                _remove_cas_operation(
                    operation, operation_fd, source_safe_to_remove=True)
                fail("CAS_SOURCE_CHANGED", "CAS source changed at final reconfirmation",
                     recoverable=True)
            _cas_detach(target, operation_fd)

        _source, source_proof = read_regular_proof_at(
            operation_fd, "source", max_bytes=manifest["maxBytes"],
            code="CAS_SOURCE_CHANGED")
        if not _same_generation(
                source_proof, manifest["expectedProof"], after_rename=True):
            # The rename may have caught a foreign last-moment replacement.
            # Put that exact inode back only if the canonical name is empty;
            # never overwrite a concurrently published generation.
            target_now, _target_now_proof = read_regular_proof(
                target, max_bytes=manifest["maxBytes"], code="CAS_SOURCE_CHANGED", required=False)
            if target_now is None:
                _cas_link(operation_fd, "source", target)
                _remove_cas_operation(
                    operation, operation_fd, source_safe_to_remove=True)
            fail("CAS_SOURCE_CHANGED", "detached CAS source was not the expected exact generation",
                 recoverable=True)

        if detach_failpoint and os.environ.get(detach_failpoint, "") == "after-detach":
            sys.stdout.flush()
            sys.stderr.flush()
            os._exit(detach_exit_code)

        _fixture_claim_after_detach(target, test_after_detach_variable)

        target_data, target_proof = read_regular_proof(
            target, max_bytes=manifest["maxBytes"], code="CAS_TARGET_CONFLICT", required=False)
        if target_proof is None:
            try:
                _cas_link(operation_fd, "candidate", target)
            except FileExistsError:
                pass
            target_data, target_proof = read_regular_proof(
                target, max_bytes=manifest["maxBytes"], code="CAS_TARGET_CONFLICT")
        if target_data != candidate or not _same_generation(
                target_proof, candidate_proof, after_rename=True):
            fail("CAS_TARGET_CONFLICT",
                 "canonical name was claimed by a foreign generation during CAS publication",
                 recoverable=True)
        _remove_cas_operation(operation, operation_fd, source_safe_to_remove=True)
    finally:
        os.close(operation_fd)


def recover_cas_operations(parent: Path, *, allowed_targets: Optional[Set[str]] = None) -> None:
    ensure_real_dir(parent, create=True)
    operations: List[Path] = []
    for name in bounded_directory_names(parent, code="CAS_RECOVERY_SCAN_FAILED",
                                        recoverable=True):
        if not name.startswith(CAS_PREFIX):
            continue
        if not CAS_NAME_RE.fullmatch(name):
            fail("CAS_RECOVERY_SCAN_FAILED", f"unsafe durable CAS artifact: {bounded(name, 180)}",
                 recoverable=True)
        operation = parent / name
        st = operation.lstat()
        if stat.S_ISLNK(st.st_mode) or not stat.S_ISDIR(st.st_mode):
            fail("CAS_RECOVERY_SCAN_FAILED", f"durable CAS operation is not a real directory: {name}",
                 recoverable=True)
        operations.append(operation)
    for operation in operations:
        _resume_cas_operation(operation, allowed_targets=allowed_targets)


def cas_replace_bytes(path: Path, expected_data: bytes, candidate: bytes, *,
                      max_bytes: int, owner: str,
                      test_swap_variable: Optional[str] = None,
                      test_after_detach_variable: Optional[str] = None,
                      detach_failpoint: Optional[str] = None,
                      detach_exit_code: int = 88,
                      stage_failpoint: Optional[str] = None) -> None:
    if len(candidate) > max_bytes:
        fail("CAS_CANDIDATE_INVALID", "CAS candidate exceeds its byte bound", recoverable=True)
    current, expected_proof = read_regular_proof(
        path, max_bytes=max_bytes, code="CAS_SOURCE_CHANGED")
    if current != expected_data:
        fail("CAS_SOURCE_CHANGED", "CAS source bytes changed before staging", recoverable=True)
    operation = _cas_operation_path(path, owner)
    parent_fd: Optional[int] = None
    try:
        parent_fd = open_directory_anchored(path.parent, code="CAS_STAGE_FAILED")
        os.mkdir(operation.name, 0o700, dir_fd=parent_fd)
        os.fsync(parent_fd)
    except OSError as exc:
        fail("CAS_STAGE_FAILED", f"cannot create durable CAS operation: {bounded(exc)}",
             recoverable=True)
    finally:
        if parent_fd is not None:
            os.close(parent_fd)
    sync_dir(operation.parent)
    manifest = {
        "version": 1, "targetName": path.name, "owner": owner,
        "expectedProof": expected_proof, "candidateHash": sha256(candidate),
        "maxBytes": max_bytes,
    }
    try:
        manifest_partial = operation / f".manifest-partial-{secrets.token_hex(8)}"
        write_exclusive(manifest_partial, _cas_manifest_bytes(manifest))
        operation_fd = open_directory_anchored(operation, code="CAS_STAGE_FAILED")
        try:
            os.rename(manifest_partial.name, "manifest.json",
                      src_dir_fd=operation_fd, dst_dir_fd=operation_fd)
            os.fsync(operation_fd)
        finally:
            os.close(operation_fd)
        if stage_failpoint and os.environ.get(stage_failpoint) == "after-cas-manifest":
            sys.stdout.flush()
            sys.stderr.flush()
            os._exit(detach_exit_code)
        candidate_partial = operation / f".candidate-partial-{secrets.token_hex(8)}"
        write_exclusive(candidate_partial, candidate)
        staged, staged_proof = read_regular_proof(
            candidate_partial, max_bytes=max_bytes, code="CAS_CANDIDATE_INVALID")
        if staged != candidate or staged_proof["hash"] != manifest["candidateHash"]:
            fail("CAS_CANDIDATE_INVALID", "CAS staged candidate failed exact verification",
                 recoverable=True)
        operation_fd = open_directory_anchored(operation, code="CAS_STAGE_FAILED")
        try:
            os.rename(candidate_partial.name, "candidate",
                      src_dir_fd=operation_fd, dst_dir_fd=operation_fd)
            os.fsync(operation_fd)
        finally:
            os.close(operation_fd)
        sync_dir(operation)
        sync_dir(operation.parent)
        _fixture_swap_before_detach(path, test_swap_variable)
        _resume_cas_operation(operation, detach_failpoint=detach_failpoint,
                              detach_exit_code=detach_exit_code,
                              test_after_detach_variable=test_after_detach_variable)
    except CreateError:
        raise
    except OSError as exc:
        fail("CAS_PUBLICATION_FAILED", f"durable CAS publication failed: {bounded(exc)}",
             recoverable=True)


class KernelMutex:
    def __init__(self, path: Path, timeout_ms: int):
        self.path = path
        self.timeout_ms = timeout_ms
        self.fd: Optional[int] = None

    def __enter__(self) -> "KernelMutex":
        ensure_real_dir(self.path.parent, create=True)
        try:
            prior = self.path.lstat()
            if stat.S_ISLNK(prior.st_mode) or not stat.S_ISREG(prior.st_mode):
                fail("CREATE_MUTEX_UNSAFE", f"mutex path must be a regular file: {self.path}")
        except FileNotFoundError:
            pass
        try:
            self.fd = open_nofollow(self.path, os.O_RDWR | os.O_CREAT, 0o600)
            opened = os.fstat(self.fd)
            if not stat.S_ISREG(opened.st_mode):
                fail("CREATE_MUTEX_UNSAFE", f"mutex path is not a regular file: {self.path}")
            if os.name == "nt":
                if opened.st_size == 0:
                    os.write(self.fd, b"\n")
                    os.fsync(self.fd)
                os.lseek(self.fd, 0, os.SEEK_SET)
            deadline = time.monotonic() + self.timeout_ms / 1000.0
            while True:
                try:
                    if os.name == "nt":
                        msvcrt.locking(self.fd, msvcrt.LK_NBLCK, 1)
                    else:
                        fcntl.flock(self.fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
                    break
                except OSError as exc:
                    if exc.errno not in (errno.EACCES, errno.EAGAIN, errno.EDEADLK):
                        raise
                    if time.monotonic() >= deadline:
                        fail("CREATE_BUSY", f"creation mutex was not acquired within {self.timeout_ms}ms")
                    time.sleep(0.025)
            return self
        except CreateError:
            self._close()
            raise
        except OSError as exc:
            self._close()
            fail("CREATE_MUTEX_FAILED", f"cannot acquire creation mutex: {bounded(exc)}")
        return self

    def _close(self) -> None:
        if self.fd is not None:
            try:
                os.close(self.fd)
            except OSError:
                pass
            self.fd = None

    def __exit__(self, exc_type, exc, tb) -> None:
        if self.fd is not None:
            try:
                if os.name == "nt":
                    os.lseek(self.fd, 0, os.SEEK_SET)
                    msvcrt.locking(self.fd, msvcrt.LK_UNLCK, 1)
                else:
                    fcntl.flock(self.fd, fcntl.LOCK_UN)
            except OSError:
                pass
        self._close()


_FINALIZE_LOCK_LIBRARY: Optional[dict] = None
_ACTIVE_DEPENDENCY_GRAPH_MUTEX: Optional["DependencyGraphMutex"] = None


def _finalize_lock_library() -> dict:
    global _FINALIZE_LOCK_LIBRARY
    if _FINALIZE_LOCK_LIBRARY is None:
        _FINALIZE_LOCK_LIBRARY = runpy.run_path(
            str(HERE / "finalize-lock.py"), run_name="dependency_graph_lock_library")
    return _FINALIZE_LOCK_LIBRARY


class DependencyGraphMutex:
    """Bounded direct owner of the finalizer's anchored kernel mutex."""

    def __init__(self, path: Path, timeout_ms: int, invocation_id: str):
        self.path = absolute_path(path)
        self.timeout_ms = timeout_ms
        self.invocation_id = invocation_id
        self.chain: List[dict] = []
        self.fh = None
        self.acquired = False
        self.started_at: Optional[str] = None
        self.start_id: Optional[str] = None
        self.lib: Optional[dict] = None

    def _record(self, *, released: bool) -> bytes:
        return (json.dumps({
            "version": 1, "pid": os.getpid(),
            "processStartId": self.start_id, "hostname": socket.gethostname(),
            "invocationId": self.invocation_id, "startedAt": self.started_at,
            "released": released,
        }, separators=(",", ":")) + "\n").encode("utf-8")

    def _write_record(self, *, released: bool) -> None:
        if self.fh is None:
            raise OSError("dependency-graph mutex fd is closed")
        data = self._record(released=released)
        os.lseek(self.fh.fileno(), 0, os.SEEK_SET)
        os.ftruncate(self.fh.fileno(), 0)
        view = memoryview(data)
        while view:
            written = os.write(self.fh.fileno(), view)
            if written <= 0:
                raise OSError("short dependency-graph mutex record write")
            view = view[written:]
        os.fsync(self.fh.fileno())

    def assert_held(self) -> None:
        if not self.acquired or self.lib is None or self.fh is None:
            fail("DEPENDENCY_GRAPH_MUTEX_OWNERSHIP_LOST",
                 "dependency-graph mutex is not held", recoverable=True)
        try:
            self.lib["verify_anchored_mutex_path"](
                self.chain, self.fh.fileno(), str(self.path), str(AUTHORITY_ROOT))
            observed = os.fstat(self.fh.fileno())
            if not stat.S_ISREG(observed.st_mode) or observed.st_size > 4096:
                raise OSError("dependency-graph mutex record is unsafe or oversized")
            os.lseek(self.fh.fileno(), 0, os.SEEK_SET)
            chunks: List[bytes] = []
            total = 0
            while True:
                chunk = os.read(self.fh.fileno(), min(4096, 4097 - total))
                if not chunk:
                    break
                total += len(chunk)
                if total > 4096:
                    raise OSError("dependency-graph mutex record exceeds its bound")
                chunks.append(chunk)
            if b"".join(chunks) != self._record(released=False):
                raise OSError("dependency-graph mutex owner record changed")
            self.lib["verify_anchored_mutex_path"](
                self.chain, self.fh.fileno(), str(self.path), str(AUTHORITY_ROOT))
        except BaseException as error:
            if isinstance(error, CreateError):
                raise
            fail("DEPENDENCY_GRAPH_MUTEX_OWNERSHIP_LOST",
                 f"dependency-graph mutex identity changed: {bounded(error)}",
                 recoverable=True)

    def _close(self, *, release: bool) -> None:
        if self.fh is not None:
            if release and self.acquired and self.lib is not None:
                try:
                    self.lib["release_lock"](self.fh)
                except OSError:
                    pass
            try:
                self.fh.close()
            except OSError:
                pass
            self.fh = None
        if self.lib is not None and self.chain:
            self.lib["close_chain"](self.chain)
        self.chain = []
        self.acquired = False

    def __enter__(self) -> "DependencyGraphMutex":
        self.lib = _finalize_lock_library()
        fd: Optional[int] = None
        try:
            self.chain, fd = self.lib["open_anchored_mutex"](
                str(self.path), str(AUTHORITY_ROOT))
            self.fh = os.fdopen(fd, "r+", encoding="utf-8")
            fd = None
            if os.name == "nt":
                self.fh.seek(0, os.SEEK_END)
                if self.fh.tell() == 0:
                    self.fh.write("\n")
                    self.fh.flush()
                    os.fsync(self.fh.fileno())
                self.fh.seek(0)
            deadline = time.monotonic() + self.timeout_ms / 1000.0
            while True:
                try:
                    if os.name == "nt":
                        msvcrt.locking(self.fh.fileno(), msvcrt.LK_NBLCK, 1)
                    else:
                        fcntl.flock(self.fh.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
                    self.acquired = True
                    break
                except OSError as lock_error:
                    if lock_error.errno not in (errno.EACCES, errno.EAGAIN, errno.EDEADLK):
                        raise
                    if time.monotonic() >= deadline:
                        self._close(release=False)
                        fail("DEPENDENCY_GRAPH_BUSY",
                             "dependency-graph mutex was not acquired within the bounded wait",
                             recoverable=True)
                    time.sleep(0.025)
            self.lib["verify_anchored_mutex_path"](
                self.chain, self.fh.fileno(), str(self.path), str(AUTHORITY_ROOT))
            self.started_at = now()
            self.start_id = self.lib["process_start_id"](os.getpid())
            if sys.platform in ("linux", "darwin", "win32") and not self.start_id:
                fail("DEPENDENCY_GRAPH_MUTEX_IDENTITY_UNAVAILABLE",
                     "exact dependency-graph mutex process identity is unavailable",
                     recoverable=True)
            self._write_record(released=False)
            self.lib["verify_anchored_mutex_path"](
                self.chain, self.fh.fileno(), str(self.path), str(AUTHORITY_ROOT))
            global _ACTIVE_DEPENDENCY_GRAPH_MUTEX
            _ACTIVE_DEPENDENCY_GRAPH_MUTEX = self
            return self
        except CreateError:
            self._close(release=True)
            raise
        except BaseException as error:
            if fd is not None:
                try:
                    os.close(fd)
                except OSError:
                    pass
            self._close(release=True)
            fail("DEPENDENCY_GRAPH_MUTEX_UNSAFE",
                 f"dependency-graph mutex acquisition failed: {bounded(error)}",
                 recoverable=True)

    def __exit__(self, exc_type, exc, tb) -> None:
        global _ACTIVE_DEPENDENCY_GRAPH_MUTEX
        release_error: Optional[BaseException] = None
        try:
            self.assert_held()
            self._write_record(released=True)
        except BaseException as error:
            release_error = error
        finally:
            if _ACTIVE_DEPENDENCY_GRAPH_MUTEX is self:
                _ACTIVE_DEPENDENCY_GRAPH_MUTEX = None
            self._close(release=True)
        if release_error is not None and exc_type is None:
            fail("DEPENDENCY_GRAPH_MUTEX_RELEASE_FAILED",
                 f"cannot publish dependency-graph mutex release: {bounded(release_error)}",
                 recoverable=True)


def assert_dependency_graph_mutex_held() -> None:
    if _ACTIVE_DEPENDENCY_GRAPH_MUTEX is None:
        fail("DEPENDENCY_GRAPH_MUTEX_OWNERSHIP_LOST",
             "dependency-graph mutation has no active kernel fence",
             recoverable=True)
    _ACTIVE_DEPENDENCY_GRAPH_MUTEX.assert_held()


CYRILLIC = {
    "а": "a", "б": "b", "в": "v", "г": "g", "ґ": "g", "д": "d",
    "е": "e", "ё": "yo", "є": "ye", "ж": "zh", "з": "z", "и": "i",
    "і": "i", "ї": "yi", "й": "y", "к": "k", "л": "l", "м": "m",
    "н": "n", "о": "o", "п": "p", "р": "r", "с": "s", "т": "t",
    "у": "u", "ф": "f", "х": "kh", "ц": "ts", "ч": "ch", "ш": "sh",
    "щ": "shch", "ъ": "", "ы": "y", "ь": "", "э": "e", "ю": "yu",
    "я": "ya",
}


def slugify(title: str) -> str:
    # Compose first so Cyrillic letters with diacritics retain their identity:
    # NFKD("й") is "и" + breve and NFKD("ё") is "е" + diaeresis, which
    # would silently transliterate them as i/e.  We map the composed Cyrillic
    # character first, and decompose only non-Cyrillic characters (e.g. é→e).
    source = unicodedata.normalize("NFC", title).lower()
    pieces: List[str] = []
    separator = False
    unknown_nonspace: List[str] = []
    for original in source:
        category = unicodedata.category(original)
        if category.startswith("M"):
            continue
        if original in CYRILLIC:
            value = CYRILLIC[original]
            if value:
                if separator and pieces and pieces[-1] != "_":
                    pieces.append("_")
                separator = False
                pieces.append(value)
            continue
        emitted = False
        for char in unicodedata.normalize("NFKD", original):
            sub_category = unicodedata.category(char)
            if sub_category.startswith("M"):
                continue
            if "a" <= char <= "z" or "0" <= char <= "9":
                if separator and pieces and pieces[-1] != "_":
                    pieces.append("_")
                separator = False
                pieces.append(char)
                emitted = True
                continue
            if sub_category.startswith("L") or sub_category.startswith("N"):
                if separator and pieces and pieces[-1] != "_":
                    pieces.append("_")
                separator = False
                pieces.append("u" + format(ord(char), "x"))
                emitted = True
                continue
        if emitted:
            continue
        if not original.isspace() and not category.startswith("C") and not category.startswith("M"):
            unknown_nonspace.append("u" + format(ord(original), "x"))
        separator = True
    slug = "".join(pieces)
    slug = re.sub(r"_+", "_", slug).strip("_")
    if not slug:
        slug = "_".join(unknown_nonspace)
    if not slug:
        # Validation rejects an actually empty title.  This branch only covers
        # non-empty strings composed entirely of zero-width/control-like code
        # points; bind the fallback to the canonical title instead of saying
        # "untitled" and losing identity.
        slug = "u_" + hashlib.sha256(title.encode("utf-8")).hexdigest()[:16]
    if len(slug) > SLUG_MAX:
        suffix = hashlib.sha256(title.encode("utf-8")).hexdigest()[:10]
        slug = slug[:SLUG_MAX - len(suffix) - 1].rstrip("_") + "_" + suffix
    slug = re.sub(r"[^a-z0-9_]+", "_", slug).strip("_")
    if not slug:
        fail("SLUG_INVALID", "title did not produce a safe stable slug")
    return slug


def validate_text_utf8(value: Any, *, field: str, max_chars: int,
                       max_bytes: int, allow_newlines: bool) -> str:
    if not isinstance(value, str):
        fail("INVALID_REQUEST", f"{field} must be a string", exit_code=2)
    if len(value) > max_chars:
        fail(f"{field.upper()}_TOO_LARGE", f"{field} exceeds {max_chars} characters", exit_code=2)
    try:
        encoded = value.encode("utf-8")
    except UnicodeEncodeError:
        fail("INVALID_UTF8", f"{field} contains an unpaired Unicode surrogate", exit_code=2)
    if len(encoded) > max_bytes:
        fail(f"{field.upper()}_TOO_LARGE", f"{field} exceeds {max_bytes} UTF-8 bytes", exit_code=2)
    if "\x00" in value:
        fail("INVALID_REQUEST", f"{field} must not contain NUL", exit_code=2)
    if allow_newlines:
        for char in value:
            if ord(char) < 32 and char not in ("\n", "\r", "\t"):
                fail("INVALID_REQUEST", f"{field} contains a forbidden control character", exit_code=2)
    elif any(unicodedata.category(char) in ("Cc", "Zl", "Zp") or
             "\u202a" <= char <= "\u202e" or "\u2066" <= char <= "\u2069"
             for char in value):
        fail("INVALID_REQUEST", f"{field} must be one line without control characters", exit_code=2)
    return value


def _source_ref_valid(value: Any) -> bool:
    if not isinstance(value, str) or value != unicodedata.normalize("NFC", value) or value != value.strip():
        return False
    encoded = value.encode("utf-8")
    if not encoded or len(encoded) > 256 or any(ord(char) < 32 or ord(char) == 127 for char in value):
        return False
    if re.match(r"^(?:[A-Za-z]:[\\/]|/|\\\\)", value):
        return False
    if re.search(r"(?:^|[?&;,\s])(?:access[_-]?token|api[_-]?key|authorization|password|secret)=", value, re.I):
        return False
    try:
        parsed = urllib.parse.urlsplit(value)
    except ValueError:
        return False
    if parsed.username is not None or parsed.password is not None:
        return False
    return True


def _canonical_task_stem(value: Any) -> bool:
    if not isinstance(value, str) or len(value) > STEM_MAX:
        return False
    match = STEM_RE.fullmatch(value)
    if not match:
        return False
    number = int(match.group(1))
    return 1 <= number <= MAX_SAFE_TASK_NUMBER and match.group(1) == str(number)


def validate_task_source(value: Any, *, origin: Optional[str]) -> dict:
    if not isinstance(value, dict) or set(value) != SOURCE_FIELDS:
        fail("SOURCE_INVALID", "source fields do not match the canonical contract", exit_code=2)
    kind = value.get("kind")
    source_type = value.get("type")
    ref = value.get("ref")
    fingerprint = value.get("fingerprint")
    if (not isinstance(kind, str) or kind not in SOURCE_TYPES or
            not isinstance(source_type, str) or not SOURCE_TYPE_RE.fullmatch(source_type) or
            source_type not in SOURCE_TYPES[kind] or not _source_ref_valid(ref) or
            not isinstance(fingerprint, str) or not HASH_RE.fullmatch(fingerprint)):
        fail("SOURCE_INVALID", "source is not canonical or contains an unsafe value", exit_code=2)
    if kind == "follow-up" and not _canonical_task_stem(ref):
        fail("SOURCE_INVALID", "follow-up source ref must be a canonical task stem", exit_code=2)
    if kind == "follow-up" and source_type in ("task-split", "test-foundation-prerequisite") and ref != origin:
        fail("SOURCE_ORIGIN_CONFLICT", "%s source must match originStem" % source_type, exit_code=2)
    return {"kind": kind, "type": source_type, "ref": ref, "fingerprint": fingerprint}


def validate_request(value: Any) -> dict:
    if not isinstance(value, dict):
        fail("INVALID_REQUEST", "stdin JSON must be an object", exit_code=2)
    allowed = {"version", "title", "body", "key", "originStem", "dedupKey", "dedupReport", "source"}
    unknown = sorted(set(value) - allowed)
    if unknown:
        fail("INVALID_REQUEST", "unknown request field(s): " + ", ".join(unknown), exit_code=2)
    if type(value.get("version")) is not int or value.get("version") != VERSION:
        fail("INVALID_REQUEST", "version must be 1", exit_code=2)
    title = validate_text_utf8(value.get("title"), field="title", max_chars=TITLE_MAX_CHARS,
                               max_bytes=TITLE_MAX_BYTES, allow_newlines=False)
    title = unicodedata.normalize("NFC", title).strip()
    if not title:
        fail("TITLE_EMPTY", "title must not be empty", exit_code=2)
    body = validate_text_utf8(value.get("body"), field="body", max_chars=BODY_MAX_CHARS,
                              max_bytes=BODY_MAX_BYTES, allow_newlines=True)
    body = body.replace("\r\n", "\n").replace("\r", "\n")
    key = value.get("key")
    if not isinstance(key, str) or not KEY_RE.fullmatch(key):
        fail("KEY_INVALID", f"key must match {KEY_RE.pattern} and be at most {KEY_MAX} characters", exit_code=2)
    origin = value.get("originStem")
    if origin is not None:
        if not _canonical_task_stem(origin):
            fail("ORIGIN_INVALID", "originStem must be a canonical ASCII TASK_<N>_<slug> stem", exit_code=2)
        if re.search(r"(?m)^##[ \t]+Origin[ \t]*$", body):
            fail("ORIGIN_CONFLICT", "body already contains an Origin section", exit_code=2)
    dedup_key = value.get("dedupKey")
    if dedup_key is not None and (not isinstance(dedup_key, str) or not DEDUP_KEY_RE.fullmatch(dedup_key)):
        fail("DEDUP_KEY_INVALID", "dedupKey has an invalid shape", exit_code=2)
    dedup_report = value.get("dedupReport")
    if dedup_report is not None and (not isinstance(dedup_report, str) or not HASH_RE.fullmatch(dedup_report.lower())):
        fail("DEDUP_REPORT_INVALID", "dedupReport must be sha256:<64 hex characters>", exit_code=2)
    if dedup_report is not None and dedup_key is None:
        fail("DEDUP_REPORT_INVALID", "dedupReport requires dedupKey", exit_code=2)
    source = validate_task_source(value.get("source"), origin=origin)
    canonical = {
        "version": VERSION,
        "title": title,
        "body": body,
        "originStem": origin,
        "dedupKey": dedup_key,
        "dedupReport": dedup_report.lower() if dedup_report else None,
        "source": source,
    }
    canonical["key"] = key
    return canonical


INTENT_FIELDS = ("version", "title", "body", "originStem", "dedupKey", "dedupReport", "source")


def request_intent(req: dict) -> dict:
    return {k: req[k] for k in INTENT_FIELDS}


def request_payload_hash(req: dict) -> str:
    return sha256(canonical_json(request_intent(req)))


def validate_recovery_intent(value: Any, expected_payload_hash: str) -> dict:
    if not isinstance(value, dict) or set(value) != set(INTENT_FIELDS):
        fail("MARKER_INVALID", "incomplete creation marker has no canonical recovery intent", recoverable=True)
    # Reuse the public validator with a synthetic safe key, then require exact
    # canonical equality.  This catches oversized/corrupt fields without ever
    # persisting the original idempotency key.
    candidate = dict(value)
    candidate["key"] = "recovery.intent.key"
    try:
        canonical = validate_request(candidate)
    except CreateError as exc:
        fail("MARKER_INVALID", f"creation recovery intent is invalid: {bounded(exc)}", recoverable=True)
    intent = {k: canonical[k] for k in INTENT_FIELDS}
    if intent != value or sha256(canonical_json(intent)) != expected_payload_hash:
        fail("MARKER_INVALID", "creation recovery intent does not match payloadHash", recoverable=True)
    return canonical


def marker_path(key_hash: str) -> Path:
    return CREATIONS_DIR / (key_hash.removeprefix("sha256:") + ".json")


def marker_bytes(marker: dict) -> bytes:
    data = json.dumps(marker, ensure_ascii=False, indent=2, sort_keys=True).encode("utf-8") + b"\n"
    if len(data) > MARKER_MAX_BYTES:
        fail("MARKER_TOO_LARGE", "creation marker exceeds its size limit", recoverable=True)
    return data


def _validate_creation_target_proof(value: Any, *, source_hash: Optional[str]) -> Optional[dict]:
    if value is None:
        return None
    if not isinstance(value, dict) or set(value) != CAS_PROOF_FIELDS:
        fail("MARKER_INVALID", "creation target proof fields are invalid", recoverable=True)
    for field in CAS_UNSIGNED_DECIMAL_PROOF_FIELDS:
        if not isinstance(value.get(field), str) or not CAS_UNSIGNED_DECIMAL_RE.fullmatch(value[field]):
            fail("MARKER_INVALID", f"creation target proof {field} is invalid", recoverable=True)
    for field in CAS_SIGNED_DECIMAL_PROOF_FIELDS:
        if (not isinstance(value.get(field), str) or
                not CAS_SIGNED_DECIMAL_RE.fullmatch(value[field]) or value[field] == "-0"):
            fail("MARKER_INVALID", f"creation target proof {field} is invalid", recoverable=True)
    if (isinstance(value.get("mode"), bool) or not isinstance(value.get("mode"), int) or
            value["mode"] != 0o600 or isinstance(value.get("size"), bool) or
            not isinstance(value.get("size"), int) or value["size"] < 1 or
            value["size"] > TASK_MAX_BYTES or not HASH_RE.fullmatch(str(value.get("hash", ""))) or
            value.get("hash") != source_hash):
        fail("MARKER_INVALID", "creation target proof does not match its source identity", recoverable=True)
    return value


def validate_marker(marker: Any, *, expected_key_hash: str, expected_payload_hash: str) -> dict:
    if (not isinstance(marker, dict) or type(marker.get("version")) is not int or
            marker.get("version") != MARKER_VERSION):
        fail("MARKER_INVALID", "creation marker must use the current v2 contract", recoverable=True)
    if set(marker) != MARKER_FIELDS:
        fail("MARKER_INVALID", "creation marker fields do not match the current contract", recoverable=True)
    if marker.get("keyHash") != expected_key_hash:
        fail("MARKER_INVALID", "creation marker key hash does not match its filename", recoverable=True)
    if marker.get("payloadHash") != expected_payload_hash:
        fail("IDEMPOTENCY_CONFLICT", "the same key was already used with a different payload",
             exit_code=2, details={"existingPayloadHash": marker.get("payloadHash")})
    if not TX_RE.fullmatch(str(marker.get("transactionId", ""))):
        fail("MARKER_INVALID", "creation marker transaction id is invalid", recoverable=True)
    if not HASH_RE.fullmatch(str(marker.get("keyHash", ""))) or not HASH_RE.fullmatch(str(marker.get("payloadHash", ""))):
        fail("MARKER_INVALID", "creation marker hashes are invalid", recoverable=True)
    if marker.get("status") not in ("incomplete", "completed") or marker.get("phase") not in PHASES:
        fail("MARKER_INVALID", "creation marker state is invalid", recoverable=True)
    if (marker.get("status") == "completed") != (marker.get("phase") == "completed"):
        fail("MARKER_INVALID", "creation marker completed status/phase are inconsistent", recoverable=True)
    if isinstance(marker.get("revision"), bool) or not isinstance(marker.get("revision"), int) or \
            marker.get("revision") < 1 or marker.get("revision") > MAX_SAFE_TASK_NUMBER:
        fail("MARKER_INVALID", "creation marker revision is invalid", recoverable=True)
    if not valid_iso8601(marker.get("createdAt")) or not valid_iso8601(marker.get("updatedAt")):
        fail("MARKER_INVALID", "creation marker timestamps are invalid", recoverable=True)
    if _datetime.datetime.fromisoformat(marker["updatedAt"].removesuffix("Z") + "+00:00") < \
            _datetime.datetime.fromisoformat(marker["createdAt"].removesuffix("Z") + "+00:00"):
        fail("MARKER_INVALID", "creation marker timestamps are out of order", recoverable=True)
    if marker.get("column") not in (None, "backlog", "pending", "todo", "done"):
        fail("MARKER_INVALID", "creation marker column is invalid", recoverable=True)
    last_error = marker.get("lastError")
    if last_error is not None and (not isinstance(last_error, dict) or set(last_error) != {"code", "message", "at"} or
                                   not isinstance(last_error.get("code"), str) or not last_error.get("code") or
                                   len(last_error.get("code", "")) > 120 or
                                   not isinstance(last_error.get("message"), str) or
                                   len(last_error.get("message", "")) > 1200 or
                                   not valid_iso8601(last_error.get("at"))):
        fail("MARKER_INVALID", "creation marker lastError is invalid", recoverable=True)
    if last_error is not None:
        try:
            last_error["code"].encode("utf-8")
            last_error["message"].encode("utf-8")
        except UnicodeEncodeError:
            fail("MARKER_INVALID", "creation marker lastError contains an invalid Unicode scalar", recoverable=True)
        error_at = _datetime.datetime.fromisoformat(last_error["at"].removesuffix("Z") + "+00:00")
        created_at = _datetime.datetime.fromisoformat(marker["createdAt"].removesuffix("Z") + "+00:00")
        updated_at = _datetime.datetime.fromisoformat(marker["updatedAt"].removesuffix("Z") + "+00:00")
        if error_at < created_at or error_at > updated_at:
            fail("MARKER_INVALID", "creation marker lastError timestamp is out of bounds", recoverable=True)
    number = marker.get("number")
    if number is not None and (isinstance(number, bool) or not isinstance(number, int) or
                               number < 1 or number > MAX_SAFE_TASK_NUMBER):
        fail("MARKER_INVALID", "creation marker number is invalid", recoverable=True)
    stem = marker.get("stem")
    if stem is not None and not _canonical_task_stem(stem):
        fail("MARKER_INVALID", "creation marker stem is invalid", recoverable=True)
    if (number is None) != (stem is None):
        fail("MARKER_INVALID", "creation marker number/stem are inconsistent", recoverable=True)
    if stem is not None:
        stem_match = STEM_RE.fullmatch(stem)
        if int(stem_match.group(1)) != number:
            fail("MARKER_INVALID", "creation marker stem number disagrees with its number field", recoverable=True)
    slug = marker.get("slug")
    if slug is not None and (not isinstance(slug, str) or len(slug) > SLUG_MAX or not re.fullmatch(r"[a-z0-9_]+", slug)):
        fail("MARKER_INVALID", "creation marker slug is invalid", recoverable=True)
    if stem is not None and marker.get("effect") != "domain-dedup":
        if slug is None or stem != f"TASK_{number}_{slug}":
            fail("MARKER_INVALID", "creation marker stem disagrees with its slug", recoverable=True)
    source_hash = marker.get("sourceHash")
    if source_hash is not None and not HASH_RE.fullmatch(str(source_hash)):
        fail("MARKER_INVALID", "creation marker source hash is invalid", recoverable=True)
    target_proof = _validate_creation_target_proof(marker.get("targetProof"), source_hash=source_hash)
    if marker.get("effect") not in (None, "created", "domain-dedup"):
        fail("MARKER_INVALID", "creation marker effect is invalid", recoverable=True)
    if marker.get("effect") == "domain-dedup" and marker.get("status") != "completed":
        fail("MARKER_INVALID", "domain-dedup may appear only in a completed receipt", recoverable=True)
    if marker.get("effect") == "created" and (stem is None or not HASH_RE.fullmatch(str(source_hash or ""))):
        fail("MARKER_INVALID", "created effect has no bound stem/source hash", recoverable=True)
    if marker.get("status") == "incomplete":
        validate_recovery_intent(marker.get("intent"), expected_payload_hash)
        phase = marker["phase"]
        if phase == "claimed" and any(marker.get(field) is not None for field in
                                       ("number", "stem", "slug", "sourceHash", "targetProof",
                                        "effect", "column")):
            fail("MARKER_INVALID", "claimed creation marker already records effects", recoverable=True)
        if phase == "reserving-number" and (
                slug is None or source_hash is not None or marker.get("effect") is not None or
                marker.get("column") is not None or target_proof is not None):
            fail("MARKER_INVALID", "number reservation marker has an impossible effect lattice", recoverable=True)
        if phase == "number-reserved" and (
                number is None or slug is None or source_hash is not None or
                marker.get("effect") is not None or marker.get("column") is not None or
                target_proof is not None):
            fail("MARKER_INVALID", "reserved-number marker has an impossible effect lattice", recoverable=True)
        if phase == "publishing-file" and (
                number is None or slug is None or source_hash is None or
                marker.get("effect") is not None or marker.get("column") is not None):
            fail("MARKER_INVALID", "file-publication marker has an impossible effect lattice", recoverable=True)
        if phase in ("file-published", "regenerating-index", "index-published", "verifying") and (
                number is None or slug is None or source_hash is None or
                marker.get("effect") != "created" or marker.get("column") is not None or
                target_proof is None):
            fail("MARKER_INVALID", "published creation marker has an impossible effect lattice", recoverable=True)
    else:
        if marker.get("revision", 0) < 2:
            fail("MARKER_INVALID", "completed receipt cannot be the initial marker generation", recoverable=True)
        if marker.get("intent") is not None or marker.get("lastError") is not None:
            fail("MARKER_INVALID", "completed receipt retained recovery-only state", recoverable=True)
        if number is None or stem is None or source_hash is None or marker.get("column") is None:
            fail("MARKER_INVALID", "completed receipt has no exact published identity", recoverable=True)
        if marker.get("effect") == "created":
            if (slug is None or marker.get("column") != "backlog" or
                    target_proof is None):
                fail("MARKER_INVALID", "completed created receipt has an impossible result lattice", recoverable=True)
        elif marker.get("effect") == "domain-dedup":
            if slug is not None or target_proof is not None:
                fail("MARKER_INVALID", "completed domain-dedup receipt must not claim a generated slug", recoverable=True)
        else:
            fail("MARKER_INVALID", "completed creation marker has no effect", recoverable=True)
    return marker


def read_marker(path: Path, *, key_hash: str, payload_hash: str) -> dict:
    raw = read_regular(path, max_bytes=MARKER_MAX_BYTES, code="MARKER_INVALID")
    try:
        parsed = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail("MARKER_INVALID", f"creation marker is not valid UTF-8 JSON: {bounded(exc)}", recoverable=True)
    marker = validate_marker(parsed, expected_key_hash=key_hash, expected_payload_hash=payload_hash)
    if raw != marker_bytes(marker):
        fail("MARKER_INVALID", "creation marker JSON is not canonical", recoverable=True)
    return marker


def write_marker(marker: dict) -> None:
    path = marker_path(marker["keyHash"])
    current = read_marker(path, key_hash=marker["keyHash"], payload_hash=marker["payloadHash"])
    if current.get("transactionId") != marker.get("transactionId"):
        fail("MARKER_OWNERSHIP_CHANGED", "creation marker belongs to another transaction", recoverable=True)
    if current.get("revision") != marker.get("revision"):
        fail("MARKER_GENERATION_CHANGED", "creation marker revision advanced unexpectedly", recoverable=True)
    marker["revision"] = int(marker.get("revision", 0)) + 1
    marker["updatedAt"] = now()
    validate_marker(marker, expected_key_hash=marker["keyHash"], expected_payload_hash=marker["payloadHash"])
    cas_replace_bytes(
        path, marker_bytes(current), marker_bytes(marker), max_bytes=MARKER_MAX_BYTES,
        owner="creation-marker:" + marker["transactionId"],
        test_swap_variable="CREATE_BACKLOG_TEST_SWAP_MARKER_BEFORE_DETACH")


def claim_or_read_marker(req: dict) -> Tuple[dict, bool]:
    ensure_real_dir(CREATIONS_DIR, create=True)
    key_hash = sha256(req["key"].encode("ascii"))
    payload_hash = request_payload_hash(req)
    path = marker_path(key_hash)
    if read_regular(path, max_bytes=MARKER_MAX_BYTES,
                    code="MARKER_INVALID", required=False) is not None:
        return read_marker(path, key_hash=key_hash, payload_hash=payload_hash), False
    marker = {
        "version": MARKER_VERSION,
        "transactionId": secrets.token_hex(16),
        "keyHash": key_hash,
        "payloadHash": payload_hash,
        "intent": request_intent(req),
        "status": "incomplete",
        "phase": "claimed",
        "effect": None,
        "number": None,
        "slug": None,
        "stem": None,
        "sourceHash": None,
        "targetProof": None,
        "column": None,
        "createdAt": now(),
        "updatedAt": now(),
        "revision": 1,
        "lastError": None,
    }
    try:
        publish_exclusive(path, marker_bytes(marker))
        sync_dir(CREATIONS_DIR)
        return marker, True
    except FileExistsError:
        return read_marker(path, key_hash=key_hash, payload_hash=payload_hash), False
    except OSError as exc:
        fail("MARKER_CLAIM_FAILED", f"cannot claim idempotency marker: {bounded(exc)}")
    raise AssertionError("unreachable")


def parse_task_number(stem: str, *, source: str) -> int:
    match = STEM_RE.fullmatch(stem)
    if not match or not _canonical_task_stem(stem):
        fail("TASK_NUMBER_INVALID", f"invalid task stem in {source}: {bounded(stem, 180)}")
    number = int(match.group(1))
    if number < 1 or number > MAX_SAFE_TASK_NUMBER:
        fail("TASK_NUMBER_INVALID", f"task number is outside the supported exact range in {source}: {number}")
    return number


def list_task_artifacts() -> List[Tuple[str, str, int, Path]]:
    artifacts: List[Tuple[str, str, int, Path]] = []
    specs = (
        ("backlog", TASK_FILE_RE, ".md"),
        ("pending", QUESTIONS_FILE_RE, ".questions.md"),
        ("todo", TASK_FILE_RE, ".md"),
        ("done", TASK_FILE_RE, ".md"),
    )
    for column, pattern, suffix in specs:
        directory = TASKS_DIR / column
        ensure_real_dir(directory, create=False)
        names = bounded_directory_names(directory, code="TASK_SCAN_FAILED")
        for name in names:
            if name.startswith(".") or name.endswith(".example"):
                continue
            match = pattern.fullmatch(name)
            if not match:
                continue
            path = directory / name
            try:
                st = path.lstat()
            except OSError as exc:
                fail("UNSAFE_TASK_ARTIFACT", f"cannot inspect {path}: {bounded(exc)}")
            if stat.S_ISLNK(st.st_mode) or not stat.S_ISREG(st.st_mode):
                fail("UNSAFE_TASK_ARTIFACT", f"task artifact must be a regular file: {path}")
            number = int(match.group(1))
            stem = name[:-len(suffix)]
            if not _canonical_task_stem(stem):
                fail("TASK_NUMBER_INVALID", f"task stem is not canonical: {name}")
            artifacts.append((column, stem, number, path))
    return artifacts


def read_index() -> dict:
    path = TASKS_DIR / "INDEX.json"
    raw = read_regular(path, max_bytes=INDEX_MAX_BYTES, code="INDEX_INVALID")
    try:
        value = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail("INDEX_INVALID", f"INDEX.json is not valid UTF-8 JSON: {bounded(exc)}", recoverable=True)
    index_fields = {"version", "generatedAt", "backlog", "pending", "todo", "done"}
    row_fields = {"stem", "title", "state", "sourceRevision", "createdAt", "doneAt", "origin",
                  "dependsOn", "splitFrom", "outcomeStatus", "questionsCount", "round"}
    def task_instant(item: Any) -> bool:
        match = re.fullmatch(r"([0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2})(?:\.([0-9]{1,3}))?Z", str(item or ""))
        if not match:
            return False
        try:
            _datetime.datetime.fromisoformat(match.group(1) + "+00:00")
            return True
        except ValueError:
            return False
    if not isinstance(value, dict) or set(value) != index_fields or value.get("version") != 2 or not task_instant(value.get("generatedAt")):
        fail("INDEX_INVALID", "INDEX.json must use the canonical version-2 schema", recoverable=True)
    if any(not isinstance(value.get(column), list) for column in ("backlog", "pending", "todo", "done")) or \
            sum(len(value[column]) for column in ("backlog", "pending", "todo", "done")) > MAX_RUNTIME_ENTRIES:
        fail("INDEX_INVALID", "INDEX.json exceeds the canonical row bound", recoverable=True)
    seen: Set[str] = set()
    for column in ("backlog", "pending", "todo", "done"):
        if not isinstance(value.get(column), list):
            fail("INDEX_INVALID", f"INDEX.json.{column} must be an array", recoverable=True)
        for entry in value[column]:
            if not isinstance(entry, dict) or set(entry) != row_fields:
                fail("INDEX_INVALID", f"INDEX.json.{column} contains an invalid entry", recoverable=True)
            stem = entry.get("stem")
            match = STEM_RE.fullmatch(str(stem or ""))
            canonical_stem = _canonical_task_stem(stem)
            number = parse_task_number(stem, source=f"INDEX.json.{column}") if canonical_stem else 0
            origin = entry.get("origin")
            split_from = entry.get("splitFrom")
            source_valid = False
            try:
                validate_task_source(origin, origin=split_from)
                source_valid = True
            except CreateError:
                pass
            valid = bool(canonical_stem and match and match.group(1) == str(number) and stem not in seen and entry.get("state") == column and
                         isinstance(entry.get("title"), str) and entry["title"] and
                         isinstance(entry.get("sourceRevision"), str) and HASH_RE.fullmatch(entry["sourceRevision"]) and
                         task_instant(entry.get("createdAt")) and
                         (task_instant(entry.get("doneAt")) if column == "done" else entry.get("doneAt") is None) and
                         source_valid and isinstance(entry.get("dependsOn"), list) and
                         all(_canonical_task_stem(dep) for dep in entry["dependsOn"]) and
                         len(set(entry["dependsOn"])) == len(entry["dependsOn"]) and
                         (split_from is None or _canonical_task_stem(split_from)) and
                         (entry.get("outcomeStatus") in {"completed", "completed-with-caveats", "partially-completed"}
                          if column == "done" else entry.get("outcomeStatus") is None))
            if column == "pending":
                valid = valid and type(entry.get("questionsCount")) is int and entry["questionsCount"] >= 0 and type(entry.get("round")) is int and entry["round"] >= 1
            else:
                valid = valid and entry.get("questionsCount") is None and entry.get("round") is None
            if not valid:
                fail("INDEX_INVALID", f"INDEX.json.{column} contains an invalid entry", recoverable=True)
            seen.add(stem)
    return value


def scan_numbers(*, current_marker: Optional[dict] = None) -> Tuple[Set[int], Dict[int, Set[str]]]:
    numbers: Set[int] = set()
    owners: Dict[int, Set[str]] = {}

    def add(number: int, owner: str) -> None:
        numbers.add(number)
        owners.setdefault(number, set()).add(owner)

    for _column, stem, number, _path in list_task_artifacts():
        add(number, stem)
    index = read_index()
    for column in ("backlog", "pending", "todo", "done"):
        for entry in index[column]:
            stem = entry["stem"]
            add(parse_task_number(stem, source=f"INDEX.json.{column}"), stem)

    ensure_real_dir(TASKNO_DIR, create=True)
    for name in bounded_directory_names(TASKNO_DIR, code="TASKNO_SCAN_FAILED"):
        if name.startswith("."):
            continue
        match = SENTINEL_RE.fullmatch(name)
        if not match:
            if name.endswith(".lock"):
                fail("TASKNO_SENTINEL_INVALID", f"invalid task-number sentinel name: {name}")
            continue
        path = TASKNO_DIR / name
        st = path.lstat()
        if stat.S_ISLNK(st.st_mode) or not stat.S_ISREG(st.st_mode):
            fail("TASKNO_SENTINEL_INVALID", f"task-number sentinel must be regular: {path}")
        number = int(match.group(1))
        if number < 1 or number > MAX_SAFE_TASK_NUMBER:
            fail("TASK_NUMBER_INVALID", f"sentinel number is outside the exact range: {name}")
        add(number, f"sentinel:{number}")

    ensure_real_dir(CREATIONS_DIR, create=True)
    marker_transactions: Set[str] = set()
    marker_bytes_total = 0
    for name in bounded_directory_names(CREATIONS_DIR, code="MARKER_SCAN_FAILED", recoverable=True):
        if name == ".mutex" or name.startswith("."):
            continue
        if not MARKER_NAME_RE.fullmatch(name):
            if name.endswith(".json"):
                fail("MARKER_INVALID", f"unexpected creation marker filename: {name}", recoverable=True)
            continue
        path = CREATIONS_DIR / name
        raw = read_regular(path, max_bytes=MARKER_MAX_BYTES, code="MARKER_INVALID")
        try:
            marker = json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            fail("MARKER_INVALID", f"creation marker {name} is malformed: {bounded(exc)}", recoverable=True)
        if not isinstance(marker, dict):
            fail("MARKER_INVALID", f"creation marker {name} must contain an object", recoverable=True)
        marker = validate_marker(
            marker,
            expected_key_hash="sha256:" + name[:-len(".json")],
            expected_payload_hash=str(marker.get("payloadHash", "")),
        )
        if raw != marker_bytes(marker):
            fail("MARKER_INVALID", f"creation marker {name} is not canonical", recoverable=True)
        if marker["transactionId"] in marker_transactions:
            fail("MARKER_INVALID", "creation marker transaction ids are not unique", recoverable=True)
        marker_transactions.add(marker["transactionId"])
        marker_bytes_total += len(raw)
        if marker_bytes_total > MAX_MARKER_CORPUS_BYTES:
            fail("MARKER_SCAN_FAILED", "creation marker corpus exceeds its total byte bound",
                 recoverable=True)
        number = marker.get("number") if isinstance(marker, dict) else None
        if number is not None:
            if isinstance(number, bool) or not isinstance(number, int) or number < 1 or number > MAX_SAFE_TASK_NUMBER:
                fail("MARKER_INVALID", f"creation marker {name} has an invalid number", recoverable=True)
            add(number, str(marker.get("stem") or f"marker:{name}"))

    # The same logical stem may legitimately be represented by backlog + its
    # pending sidecar.  A number owned by two distinct task stems is corruption;
    # sentinel/marker bookkeeping owners are excluded from this comparison.
    for number, values in owners.items():
        task_stems = {v for v in values if not v.startswith("sentinel:") and not v.startswith("marker:")}
        if len(task_stems) > 1:
            fail("TASK_NUMBER_CONFLICT", f"task number {number} is already owned by multiple stems: {', '.join(sorted(task_stems))}")
    return numbers, owners


def sentinel_path(number: int) -> Path:
    return TASKNO_DIR / f"{number}.lock"


def sentinel_payload(marker: dict, number: int) -> bytes:
    return canonical_json({
        "version": VERSION,
        "number": number,
        "transactionId": marker["transactionId"],
        "keyHash": marker["keyHash"],
    }) + b"\n"


def sentinel_owned(marker: dict, number: int) -> bool:
    path = sentinel_path(number)
    raw = read_regular(path, max_bytes=4096, code="TASKNO_SENTINEL_INVALID", required=False)
    if raw is None:
        return False
    if not raw.strip():
        return False  # malformed empty sentinel: reserved fail-closed, never attributable
    try:
        value = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        return False
    return value == {
        "version": VERSION,
        "number": number,
        "transactionId": marker["transactionId"],
        "keyHash": marker["keyHash"],
    }


def claim_number(marker: dict, slug: str) -> None:
    ensure_real_dir(TASKNO_DIR, create=True)
    if marker.get("number") is not None:
        number = marker["number"]
        expected_stem = f"TASK_{number}_{slug}"
        if marker.get("stem") != expected_stem:
            fail("MARKER_INVALID", "recorded stem does not match its number and slug", recoverable=True)
        if sentinel_owned(marker, number):
            if marker.get("phase") == "reserving-number":
                marker["phase"] = "number-reserved"
                write_marker(marker)
            scan_numbers(current_marker=marker)
            return
        if not (sentinel_path(number).exists() or sentinel_path(number).is_symlink()):
            try:
                publish_exclusive(sentinel_path(number), sentinel_payload(marker, number))
                sync_dir(TASKNO_DIR)
            except FileExistsError:
                pass
            if sentinel_owned(marker, number):
                # Reconstructing a missing permanent sentinel must never roll
                # an already-published file receipt back to number-reserved;
                # that would retain sourceHash/effect in an impossible lattice
                # and wedge exact forward recovery.  Only the pre-effect
                # reserving phase advances here.
                if marker.get("phase") == "reserving-number":
                    marker["phase"] = "number-reserved"
                    write_marker(marker)
                scan_numbers(current_marker=marker)
                return
        # A foreign claimant may beat only a pre-effect candidate intent.  Once
        # number-reserved/file effects exist, ownership loss is a hard conflict.
        if marker.get("phase") != "reserving-number" or marker.get("effect") is not None or marker.get("sourceHash") is not None:
            fail("NUMBER_OWNERSHIP_CONFLICT", f"reserved number {number} is no longer owned by this transaction", recoverable=True)
        marker["number"] = None
        marker["stem"] = None
        write_marker(marker)

    marker["phase"] = "reserving-number"
    marker["slug"] = slug
    write_marker(marker)
    numbers, _owners = scan_numbers(current_marker=marker)
    candidate = max(numbers, default=0) + 1
    while candidate <= MAX_SAFE_TASK_NUMBER:
        stem = f"TASK_{candidate}_{slug}"
        if len(stem) > STEM_MAX:
            fail("STEM_TOO_LARGE", f"generated stem exceeds {STEM_MAX} characters", recoverable=True)
        # Write-ahead candidate identity BEFORE the O_EXCL effect.  A crash
        # immediately before/after sentinel publication therefore retries this
        # same N instead of silently abandoning it.
        marker["number"] = candidate
        marker["stem"] = stem
        marker["phase"] = "reserving-number"
        write_marker(marker)
        path = sentinel_path(candidate)
        try:
            publish_exclusive(path, sentinel_payload(marker, candidate))
            sync_dir(TASKNO_DIR)
        except FileExistsError:
            candidate += 1
            continue
        except OSError as exc:
            fail("NUMBER_RESERVATION_FAILED", f"cannot reserve task number {candidate}: {bounded(exc)}", recoverable=True)
        marker["phase"] = "number-reserved"
        write_marker(marker)
        return
    fail("TASK_NUMBER_EXHAUSTED", "no exact task number remains in the supported range", recoverable=True)


def source_bytes(req: dict, marker: dict) -> bytes:
    body = req["body"].rstrip("\n")
    lines = [f"# TASK {marker['number']} — {req['title']}"]
    source = req["source"]
    lines.extend([
        "", "## Source", "",
        f"- Kind: {source['kind']}",
        f"- Type: {source['type']}",
        f"- Ref: {source['ref']}",
        f"- Fingerprint: {source['fingerprint']}",
    ])
    if body:
        lines.extend(["", body])
    if req["originStem"]:
        lines.extend(["", "## Origin", f"- split from {req['originStem']}"])
    return ("\n".join(lines).rstrip("\n") + "\n").encode("utf-8")


def task_path(stem: str) -> Path:
    if not _canonical_task_stem(stem):
        fail("STEM_INVALID", "unsafe task stem", recoverable=True)
    return TASKS_DIR / "backlog" / f"{stem}.md"


def _creation_stage_expected(marker: dict) -> Optional[bytes]:
    """Return the exact candidate bytes retained by an incomplete marker.

    Completed receipts intentionally shed their request body.  They can still
    prove an exact full candidate by sourceHash, but never a partial prefix.
    """
    if marker.get("status") == "completed":
        return None
    if (marker.get("number") is None or marker.get("stem") is None or
            marker.get("slug") is None or marker.get("sourceHash") is None):
        fail("CREATION_STAGING_ORPHANED",
             "creation staging exists before its marker records an exact candidate identity",
             recoverable=True)
    req = validate_recovery_intent(marker.get("intent"), marker["payloadHash"])
    expected = source_bytes(req, marker)
    if len(expected) > TASK_MAX_BYTES or sha256(expected) != marker.get("sourceHash"):
        fail("CREATION_STAGING_ORPHANED",
             "creation staging marker cannot reproduce its bound source bytes",
             recoverable=True)
    return expected


def _validate_creation_stage_payload(marker: dict, kind: str, data: bytes) -> None:
    expected = _creation_stage_expected(marker)
    if expected is None:
        if (marker.get("effect") != "created" or kind != "candidate" or
                sha256(data) != marker.get("sourceHash")):
            fail("CREATION_STAGING_ORPHANED",
                 "completed creation receipt cannot prove ownership of this staging artifact",
                 recoverable=True)
        return
    if kind == "partial":
        if len(data) > len(expected) or not expected.startswith(data):
            fail("CREATION_STAGING_CONTENT_INVALID",
                 "partial creation staging bytes are not an exact prefix of the durable intent",
                 recoverable=True)
    elif data != expected:
        fail("CREATION_STAGING_CONTENT_INVALID",
             "creation candidate bytes do not exactly match the durable intent",
             recoverable=True)


def scan_creation_staging(markers: List[dict], *,
                          only_transactions: Optional[Set[str]] = None) -> List[dict]:
    """Validate every selected hidden create artifact without mutating it."""
    marker_by_tx = {marker["transactionId"]: marker for marker in markers}
    records: List[dict] = []
    total = 0
    roots = ((CREATIONS_DIR, CREATION_STAGE_RE),)
    for directory, pattern in roots:
        names = bounded_directory_names(
            directory, code="CREATION_STAGING_SCAN_FAILED", recoverable=True)
        for name in names:
            if not name.startswith(".create-"):
                continue
            match = pattern.fullmatch(name)
            if not match:
                fail("CREATION_STAGING_NAME_INVALID",
                     f"unexpected creation staging filename: {bounded(name, 180)}",
                     recoverable=True)
            transaction_id, kind = match.groups()
            if only_transactions is not None and transaction_id not in only_transactions:
                continue
            marker = marker_by_tx.get(transaction_id)
            if marker is None:
                fail("CREATION_STAGING_ORPHANED",
                     f"creation staging artifact has no matching durable transaction: {bounded(name, 180)}",
                     recoverable=True)
            raw, proof = read_regular_proof(
                directory / name, max_bytes=TASK_MAX_BYTES,
                code="CREATION_STAGING_UNSAFE")
            total += len(raw)
            if total > MAX_CREATION_STAGE_CORPUS_BYTES:
                fail("CREATION_STAGING_SCAN_LIMIT",
                     "creation staging corpus exceeds its aggregate byte bound",
                     recoverable=True)
            _validate_creation_stage_payload(marker, kind, raw)
            records.append({
                "name": name, "directory": directory,
                "transactionId": transaction_id, "kind": kind,
                "data": raw, "proof": proof, "marker": marker,
            })
    return records


def _unlink_creation_stage(record: dict) -> None:
    """Unlink only the stable generation re-proven from its marker and bytes."""
    directory_fd = open_directory_anchored(
        record["directory"], code="CREATION_STAGING_CLEANUP_FAILED")
    try:
        live, proof = read_regular_proof_at(
            directory_fd, record["name"], max_bytes=TASK_MAX_BYTES,
            code="CREATION_STAGING_CLEANUP_FAILED")
        _validate_creation_stage_payload(record["marker"], record["kind"], live)
        if not _same_generation(proof, record["proof"]):
            fail("CREATION_STAGING_GENERATION_CHANGED",
                 "creation staging generation changed before cleanup",
                 recoverable=True)
        os.unlink(record["name"], dir_fd=directory_fd)
        os.fsync(directory_fd)
        try:
            os.stat(record["name"], dir_fd=directory_fd, follow_symlinks=False)
        except FileNotFoundError:
            return
        fail("CREATION_STAGING_CLEANUP_FAILED",
             "creation staging name still exists after exact cleanup",
             recoverable=True)
    except CreateError:
        raise
    except OSError as exc:
        fail("CREATION_STAGING_CLEANUP_FAILED",
             f"cannot remove exact owned creation staging: {bounded(exc)}",
             recoverable=True)
    finally:
        os.close(directory_fd)


def reconcile_creation_staging(markers: List[dict], *,
                               only_transactions: Set[str]) -> None:
    """Remove selected, exactly-proven crash artifacts and verify absence."""
    # Every unlink of a hard-link alias advances ctime for all remaining names
    # of that inode.  Never reuse a corpus-wide proof after such a mutation:
    # Re-scan before each exact unlink while
    # retaining the initial bounded corpus size as a strict progress cap, so a
    # concurrent name injector cannot turn cleanup into an unbounded loop.
    initial = scan_creation_staging(
        markers, only_transactions=only_transactions)
    removal_limit = len(initial)
    removed = 0
    while True:
        live = scan_creation_staging(
            markers, only_transactions=only_transactions)
        if not live:
            return
        if removed >= removal_limit:
            fail("CREATION_STAGING_CLEANUP_FAILED",
                 "creation staging corpus did not shrink within its exact initial bound",
                 recoverable=True)
        _unlink_creation_stage(live[0])
        removed += 1


def _same_inode_after_link_count_change(current: dict, previous: dict) -> bool:
    """Match one inode across a hard-link add/remove operation.

    A link-count change may advance ctime and nothing else.  The exact
    device/inode pair prevents a byte-identical foreign generation from
    satisfying a durable publication proof.
    """
    fields = ("dev", "ino", "mode", "size", "mtimeNs", "hash")
    try:
        ctime_advanced = int(current.get("ctimeNs", "")) >= int(previous.get("ctimeNs", ""))
    except (TypeError, ValueError):
        return False
    return ctime_advanced and all(current.get(field) == previous.get(field) for field in fields)


def _candidate_matches_target(candidate: dict, target_proof: dict) -> bool:
    return _same_generation(candidate.get("proof", {}), target_proof)


def refresh_completed_target_proofs(markers: List[dict]) -> None:
    """Refresh only the same inode after removing a completed hard-link witness."""
    for marker in markers:
        if (marker.get("version") != MARKER_VERSION or
                marker.get("status") != "completed" or marker.get("effect") != "created"):
            continue
        target_data, target_proof = read_regular_proof(
            task_path(marker["stem"]), max_bytes=TASK_MAX_BYTES,
            code="CREATION_TARGET_PROOF_MISSING", required=False)
        if (target_data is None or sha256(target_data) != marker.get("sourceHash") or
                marker.get("targetProof") is None):
            continue
        if _same_generation(target_proof, marker["targetProof"]):
            continue
        if not _same_inode_after_link_count_change(target_proof, marker["targetProof"]):
            fail("TASK_TARGET_GENERATION_CHANGED",
                 "completed creation target changed while cleaning its retained witness",
                 recoverable=True)
        marker["targetProof"] = target_proof
        write_marker(marker)


def atomic_publish_task(marker: dict, data: bytes) -> None:
    target = task_path(marker["stem"])
    expected_hash = sha256(data)
    transaction_ids = {marker["transactionId"]}
    if marker.get("version") != MARKER_VERSION:
        fail("CREATION_TARGET_PROOF_MISSING",
             "task publication requires the proof-carrying creation marker contract",
             recoverable=True)
    if marker.get("sourceHash") is None:
        marker["sourceHash"] = expected_hash
        marker["targetProof"] = None
        marker["phase"] = "publishing-file"
        marker["effect"] = None
        write_marker(marker)
    elif marker.get("sourceHash") != expected_hash:
        fail("TASK_TARGET_CONFLICT", "creation marker source hash changed", recoverable=True)

    # A short write is never a publication witness.  Exact prefix ownership is
    # enough to remove it, after which candidates are re-read so an unlink-
    # induced ctime change cannot invalidate the lineage comparison below.
    records = scan_creation_staging([marker], only_transactions=transaction_ids)
    for record in [item for item in records if item["kind"] == "partial"]:
        _unlink_creation_stage(record)
    records = scan_creation_staging([marker], only_transactions=transaction_ids)
    candidates = [item for item in records if item["kind"] == "candidate"]

    existing, target_proof = read_regular_proof(
        target, max_bytes=TASK_MAX_BYTES, code="TASK_TARGET_CONFLICT", required=False)
    published_phase = marker.get("phase") in (
        "file-published", "regenerating-index", "index-published", "verifying")
    if published_phase or marker.get("effect") == "created":
        matching_candidates = [
            item for item in candidates if _candidate_matches_target(item, target_proof or {})
        ]
        exact_proof = (marker.get("targetProof") is not None and
                       _same_generation(target_proof or {}, marker["targetProof"]))
        linked_count_proof = (marker.get("targetProof") is not None and
                              bool(matching_candidates) and
                              _same_inode_after_link_count_change(
                                  target_proof or {}, marker["targetProof"]))
        detached_proof_match = (marker.get("targetProof") is not None and not candidates and
                                _same_inode_after_link_count_change(
                                    target_proof or {}, marker["targetProof"]))
        if existing != data or not (exact_proof or linked_count_proof or detached_proof_match):
            fail("TASK_TARGET_GENERATION_CHANGED",
                 "durably published task no longer matches its exact target generation",
                 recoverable=True)
        if candidates:
            if not matching_candidates:
                fail("CREATION_STAGING_GENERATION_CHANGED",
                     "published task staging is not its exact target generation",
                     recoverable=True)
            if not exact_proof:
                marker["targetProof"] = target_proof
                write_marker(marker)
            linked_proof = marker["targetProof"]
            reconcile_creation_staging([marker], only_transactions=transaction_ids)
            published, detached_proof = read_regular_proof(
                target, max_bytes=TASK_MAX_BYTES, code="TASK_TARGET_CONFLICT")
            if (published != data or not _same_inode_after_link_count_change(
                    detached_proof, linked_proof)):
                fail("TASK_TARGET_GENERATION_CHANGED",
                     "published task changed while cleaning its retained witness",
                     recoverable=True)
            marker["targetProof"] = detached_proof
            write_marker(marker)
        elif not exact_proof:
            marker["targetProof"] = target_proof
            write_marker(marker)
        return

    if existing is None and marker.get("targetProof") is not None:
        fail("TASK_TARGET_GENERATION_CHANGED",
             "proved task target disappeared before publication commit", recoverable=True)

    if existing is None and not candidates:
        ensure_real_dir(CREATIONS_DIR, create=True)
        partial = CREATIONS_DIR / f".create-{marker['transactionId']}.partial"
        candidate = CREATIONS_DIR / f".create-{marker['transactionId']}.candidate"
        if os.environ.get("CREATE_BACKLOG_FAILPOINT") == "mid-task-write":
            staging_fd = open_directory_anchored(CREATIONS_DIR, code="TASK_WRITE_FAILED")
            try:
                fd = os.open(partial.name,
                             os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0),
                             0o600, dir_fd=staging_fd)
                try:
                    os.write(fd, data[:max(1, len(data) // 2)])
                    os.fsync(fd)
                finally:
                    os.close(fd)
                os.fsync(staging_fd)
            finally:
                os.close(staging_fd)
            sys.stdout.flush()
            sys.stderr.flush()
            os._exit(86)
        try:
            write_exclusive(partial, data)
        except FileExistsError:
            fail("CREATION_STAGING_GENERATION_CHANGED",
                 "creation partial name was claimed by another generation",
                 recoverable=True)
        staged, staged_proof = read_regular_proof(
            partial, max_bytes=TASK_MAX_BYTES, code="TASK_TEMP_INVALID")
        if staged != data or staged_proof["hash"] != expected_hash:
            fail("TASK_TEMP_INVALID", "completed task candidate failed exact verification",
                 recoverable=True)
        staging_fd = open_directory_anchored(CREATIONS_DIR, code="TASK_WRITE_FAILED")
        try:
            try:
                os.link(partial.name, candidate.name, src_dir_fd=staging_fd,
                        dst_dir_fd=staging_fd, follow_symlinks=False)
            except FileExistsError:
                fail("CREATION_STAGING_GENERATION_CHANGED",
                     "creation candidate name was claimed by another generation",
                     recoverable=True)
            os.fsync(staging_fd)
        except OSError as exc:
            fail("TASK_WRITE_FAILED", f"cannot retain task candidate: {bounded(exc)}",
                 recoverable=True)
        finally:
            os.close(staging_fd)
        completed, completed_proof = read_regular_proof(
            candidate, max_bytes=TASK_MAX_BYTES, code="TASK_TEMP_INVALID")
        if (completed != data or completed_proof["hash"] != expected_hash or
                not _same_inode_after_link_count_change(completed_proof, staged_proof)):
            fail("TASK_TEMP_INVALID", "completed task candidate changed before publication",
                 recoverable=True)
        partial_records = [record for record in scan_creation_staging(
            [marker], only_transactions=transaction_ids) if record["name"] == partial.name]
        if len(partial_records) != 1:
            fail("CREATION_STAGING_GENERATION_CHANGED",
                 "creation partial generation vanished before exact detach",
                 recoverable=True)
        _unlink_creation_stage(partial_records[0])
        maybe_failpoint("after-task-candidate")
        records = scan_creation_staging([marker], only_transactions=transaction_ids)
        candidates = [item for item in records if item["kind"] == "candidate"]

    if existing is not None:
        if existing != data:
            fail("TASK_TARGET_CONFLICT",
                 f"target already exists with different bytes: {target}", recoverable=True)
        matches = [item for item in candidates if _candidate_matches_target(item, target_proof)]
        if candidates and not matches:
            fail("TASK_TARGET_OWNERSHIP_UNPROVEN",
                 "byte-identical target is not the retained candidate generation",
                 recoverable=True)
        if not candidates:
            linked = marker.get("targetProof")
            if linked is None or not _same_inode_after_link_count_change(target_proof, linked):
                fail("TASK_TARGET_OWNERSHIP_UNPROVEN",
                     "byte-identical target has no durable creation lineage proof",
                     recoverable=True)
        elif marker.get("targetProof") is not None and not _same_generation(
                target_proof, marker["targetProof"]):
            fail("TASK_TARGET_GENERATION_CHANGED",
                 "retained candidate no longer matches the durable target proof",
                 recoverable=True)
        elif marker.get("targetProof") is None:
            marker["targetProof"] = target_proof
            write_marker(marker)
    else:
        if not candidates:
            fail("CREATION_STAGING_ORPHANED",
                 "task publication has neither a target nor an exact retained candidate",
                 recoverable=True)
        chosen = candidates[0]
        staging_fd = open_directory_anchored(CREATIONS_DIR, code="TASK_WRITE_FAILED")
        target_fd = open_directory_anchored(target.parent, code="TASK_WRITE_FAILED")
        try:
            try:
                os.link(chosen["name"], target.name, src_dir_fd=staging_fd,
                        dst_dir_fd=target_fd, follow_symlinks=False)
            except FileExistsError:
                pass
            os.fsync(target_fd)
        except OSError as exc:
            fail("TASK_WRITE_FAILED", f"cannot publish task without clobber: {bounded(exc)}",
                 recoverable=True)
        finally:
            os.close(target_fd)
            os.close(staging_fd)
        existing, target_proof = read_regular_proof(
            target, max_bytes=TASK_MAX_BYTES, code="TASK_TARGET_CONFLICT")
        candidates = [item for item in scan_creation_staging(
            [marker], only_transactions=transaction_ids) if item["kind"] == "candidate"]
        live_chosen = next(
            (item for item in candidates if item["name"] == chosen["name"]), None)
        if (existing != data or live_chosen is None or
                not _same_inode_after_link_count_change(live_chosen["proof"], chosen["proof"]) or
                not _candidate_matches_target(live_chosen, target_proof)):
            fail("TASK_TARGET_OWNERSHIP_UNPROVEN",
                 "published name is not an exact hard link to the retained candidate",
                 recoverable=True)
        maybe_failpoint("after-task-link")
        marker["targetProof"] = target_proof
        write_marker(marker)

    maybe_failpoint("after-task-proof")
    linked_proof = marker.get("targetProof")
    if linked_proof is None:
        fail("CREATION_TARGET_PROOF_MISSING",
             "task target proof was not durably recorded", recoverable=True)
    reconcile_creation_staging([marker], only_transactions=transaction_ids)
    maybe_failpoint("after-task-cleanup")
    published, detached_proof = read_regular_proof(
        target, max_bytes=TASK_MAX_BYTES, code="TASK_TARGET_CONFLICT")
    if (published != data or not _same_inode_after_link_count_change(
            detached_proof, linked_proof)):
        fail("TASK_TARGET_GENERATION_CHANGED",
             "task target changed while detaching its retained candidate", recoverable=True)
    marker["targetProof"] = detached_proof
    marker["phase"] = "file-published"
    marker["effect"] = "created"
    write_marker(marker)


def run_regen(check: bool) -> Tuple[str, str]:
    read_regular(REGEN_INDEX, max_bytes=2 * 1024 * 1024, code="INDEX_REGEN_SCRIPT_INVALID")
    expected_tasks = PROJECT_ROOT / "orchestrator" / "tasks"
    if REGEN_INDEX == (HERE / "regen-index.py").resolve() and TASKS_DIR != absolute_path(expected_tasks):
        fail("INDEX_REGEN_CONFIG_INVALID", "canonical regen-index.py requires TASKS_DIR under PROJECT_ROOT/orchestrator/tasks", recoverable=True)
    old_cwd = Path.cwd()
    old_argv = sys.argv[:]
    stdout = io.StringIO()
    stderr = io.StringIO()
    child_exit = None
    try:
        os.chdir(PROJECT_ROOT)
        sys.argv = [str(REGEN_INDEX)] + (["--check"] if check else [])
        with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
            try:
                runpy.run_path(str(REGEN_INDEX), run_name="__main__")
            except SystemExit as exc:
                code = exc.code
                if code not in (None, 0):
                    child_exit = code
    except CreateError:
        raise
    except BaseException as exc:
        fail("INDEX_CHECK_FAILED" if check else "INDEX_REGEN_FAILED",
             f"regen-index.py {'--check ' if check else ''}raised {type(exc).__name__}",
             recoverable=True)
    finally:
        sys.argv = old_argv
        os.chdir(old_cwd)
    if child_exit is not None:
        _relay_regen_task_state_observations(stderr.getvalue(), check=check, success=False)
        message = f"exit {child_exit}" if type(child_exit) is int else "nonzero exit"
        fail("INDEX_CHECK_FAILED" if check else "INDEX_REGEN_FAILED",
             f"regen-index.py {'--check ' if check else ''}failed: {bounded(message)}",
             recoverable=True)
    _relay_regen_task_state_observations(stderr.getvalue(), check=check, success=True)
    expected_stdout = "regen-index.py --check: INDEX.json is structurally fresh\n"
    safe_stdout = expected_stdout if check and stdout.getvalue() == expected_stdout else ""
    return safe_stdout, ""


def _bind_active_runtime_context(child_env: dict, active_runtime: Optional[dict]) -> None:
    """Bind one exact, generation-checked marker owner to a validator child.

    This is not an exemption token.  The runtime inspector independently
    re-proves the referenced writer lease/publication guard and matches the
    marker's transaction, revision and exact content hash.  Reading the live
    marker here immediately before spawn prevents a stale in-memory receipt
    from authorizing a last-moment foreign replacement.
    """
    if active_runtime is None:
        child_env.pop("ORCHESTRATOR_ACTIVE_CREATION_CONTEXT", None)
        child_env.pop("ORCHESTRATOR_ACTIVE_EDIT_CONTEXT", None)
        return
    expected_fields = {"kind", "path", "marker", "mode", "authorityLeaseId",
                       "publicationGuardLeaseId", "publicationKey"}
    if not isinstance(active_runtime, dict) or set(active_runtime) != expected_fields:
        fail("TASK_STATE_ACTIVE_CONTEXT_INVALID", "active runtime context shape is invalid",
             recoverable=True)
    kind = active_runtime.get("kind")
    marker = active_runtime.get("marker")
    path = active_runtime.get("path")
    if kind not in ("creation", "edit") or not isinstance(marker, dict) or not isinstance(path, Path):
        fail("TASK_STATE_ACTIVE_CONTEXT_INVALID", "active runtime marker identity is invalid",
             recoverable=True)
    expected = json.dumps(marker, ensure_ascii=False, indent=2,
                          sort_keys=True).encode("utf-8") + b"\n"
    max_bytes = MARKER_MAX_BYTES if kind == "creation" else 160 * 1024
    if len(expected) > max_bytes:
        fail("TASK_STATE_ACTIVE_CONTEXT_INVALID", "active runtime marker exceeds its byte bound",
             recoverable=True)
    live = read_regular(path, max_bytes=max_bytes,
                        code="TASK_STATE_ACTIVE_MARKER_CHANGED")
    if live != expected:
        fail("TASK_STATE_ACTIVE_MARKER_CHANGED",
             "active runtime marker changed before canonical validation", recoverable=True)
    process_start_id = process_start_identity(os.getpid())
    if not PROCESS_START_ID_RE.fullmatch(str(process_start_id or "")):
        fail("TASK_STATE_ACTIVE_CONTEXT_INVALID",
             "cannot bind active runtime context to this process generation", recoverable=True)
    common = {
        "version": 1,
        "transactionId": marker.get("transactionId"),
        "revision": marker.get("revision"),
        "contentHash": sha256(live),
        "pid": os.getpid(),
        "processStartId": process_start_id,
        "authorityLeaseId": active_runtime.get("authorityLeaseId"),
        "mode": active_runtime.get("mode"),
        "publicationKey": active_runtime.get("publicationKey"),
    }
    if kind == "creation":
        context = dict(common, keyHash=marker.get("keyHash"),
                       publicationGuardLeaseId=active_runtime.get("publicationGuardLeaseId"))
        variable = "ORCHESTRATOR_ACTIVE_CREATION_CONTEXT"
        child_env.pop("ORCHESTRATOR_ACTIVE_EDIT_CONTEXT", None)
    else:
        if active_runtime.get("publicationGuardLeaseId") is not None:
            fail("TASK_STATE_ACTIVE_CONTEXT_INVALID", "edit context cannot carry a publication guard",
                 recoverable=True)
        context = dict(common, stem=marker.get("stem"))
        variable = "ORCHESTRATOR_ACTIVE_EDIT_CONTEXT"
        child_env.pop("ORCHESTRATOR_ACTIVE_CREATION_CONTEXT", None)
    child_env[variable] = canonical_json(context).decode("utf-8")


def _project_task_state_observation(stderr: bytes, fallback_code: str,
                                    elapsed_ms: int, *, fallback_scope: Optional[str] = None,
                                    fallback_action: Optional[str] = None,
                                    fallback_phase: Optional[str] = None) -> dict:
    """Project exactly one child event onto the public privacy-bounded schema."""
    parsed: List[dict] = []
    if len(stderr) <= 64 * 1024:
        try:
            text = stderr.decode("utf-8")
        except UnicodeDecodeError:
            text = ""
        for line in text.splitlines():
            if not line.startswith("[task-state] "):
                continue
            try:
                value = json.loads(line[len("[task-state] "):])
            except (ValueError, RecursionError):
                continue
            if isinstance(value, dict) and value.get("version") == 1 and value.get("event") == "task-state-validation":
                parsed.append(value)

    source = parsed[0] if len(parsed) == 1 else {}
    safe_text = lambda value, limit: (str(value)[:limit] if isinstance(value, str) else None)
    safe_match = lambda value, pattern, limit: (
        str(value)[:limit] if isinstance(value, str) and
        re.fullmatch(pattern, str(value)[:limit]) is not None else None)
    caller = safe_text(source.get("caller"), 40)
    if not caller or re.fullmatch(r"[a-z][a-z0-9-]{0,39}", caller) is None:
        caller = "server"
    findings = []
    raw_findings = source.get("findings") if isinstance(source.get("findings"), list) else []
    for item in raw_findings[:100]:
        if not isinstance(item, dict):
            continue
        finding_code = safe_text(item.get("code"), 80) or "UNKNOWN"
        if re.fullmatch(r"[A-Za-z0-9_.:-]{1,80}", finding_code) is None:
            finding_code = "UNKNOWN"
        severity = item.get("severity") if item.get("severity") in ("warning", "error", "blocker") else "error"
        findings.append({"code": finding_code, "severity": severity})
    if len(parsed) != 1:
        fallback = safe_text(fallback_code, 80) or "TASK_STATE_UNAVAILABLE"
        findings = [{"code": fallback if re.fullmatch(r"[A-Za-z0-9_.:-]{1,80}", fallback) else "TASK_STATE_UNAVAILABLE",
                     "severity": "blocker"}]

    def safe_int(value: Any, fallback: int, maximum: int) -> int:
        return value if type(value) is int and 0 <= value <= maximum else fallback

    duration = safe_int(source.get("durationMs"), min(max(elapsed_ms, 0), 3_600_000), 3_600_000)
    threshold = safe_int(source.get("slowThresholdMs"), 100, 60_000)
    valid = len(parsed) == 1 and source.get("result") == "valid" and source.get("overallOk") is True
    scope = safe_match(source.get("scope"), r"(?:all|TASK_[1-9][0-9]*_[A-Za-z0-9_]+)", 160)
    if scope != "all" and not _canonical_task_stem(scope):
        scope = None
    if scope is None:
        scope = safe_match(fallback_scope, r"(?:all|TASK_[1-9][0-9]*_[A-Za-z0-9_]+)", 160)
        if scope != "all" and not _canonical_task_stem(scope):
            scope = None
    action = safe_match(source.get("action"),
                        r"(?:create|edit|prep|answers|run|drop|reopen|finalize|index-check|index-publish)", 20)
    if action is None:
        action = safe_match(fallback_action, r"(?:create|edit|index-check|index-publish)", 20)
    transition = safe_match(source.get("transition"),
                            r"(?:absent|backlog|pending|todo|done|corrupt):(?:absent|backlog|pending|todo|done|corrupt)", 32)
    phase = source.get("phase") if source.get("phase") in ("pre", "post") else (
        fallback_phase if fallback_phase in ("pre", "post") else None)
    observed_state = source.get("observedState") if source.get("observedState") in (
        "absent", "backlog", "pending", "todo", "done", "corrupt") else None
    expected_state = source.get("expectedState") if source.get("expectedState") in (
        "absent", "backlog", "pending", "todo", "done", "corrupt") else None
    snapshot_hash = safe_match(source.get("snapshotHash"), r"sha256:[a-f0-9]{64}", 96)
    return {
        "version": 1, "event": "task-state-validation", "caller": caller,
        "scope": scope, "action": action, "transition": transition, "phase": phase,
        "observedState": observed_state, "expectedState": expected_state,
        "snapshotHash": snapshot_hash,
        "durationMs": duration, "slowThresholdMs": threshold,
        "slow": duration >= threshold,
        "scanMode": source.get("scanMode") if source.get("scanMode") in ("full", "stem-closure") else None,
        "taskBodyReads": safe_int(source.get("taskBodyReads"), 0, 10_000_000),
        "architectureStatus": safe_match(source.get("architectureStatus"), r"[a-z][a-z0-9-]{0,79}", 80),
        "findings": findings,
        "findingsTruncated": bool(source.get("findingsTruncated")) or len(raw_findings) > 100,
        "result": "valid" if valid else "invalid",
        "ok": len(parsed) == 1 and source.get("ok") is True,
        "overallOk": valid,
    }


def _emit_task_state_observation(stderr: bytes, fallback_code: str,
                                 elapsed_ms: int, *, fallback_scope: Optional[str] = None,
                                 fallback_action: Optional[str] = None,
                                 fallback_phase: Optional[str] = None) -> None:
    """Write one reprojected event; arbitrary child stderr is never forwarded."""
    event = _project_task_state_observation(
        stderr, fallback_code, elapsed_ms, fallback_scope=fallback_scope,
        fallback_action=fallback_action, fallback_phase=fallback_phase)
    sys.stderr.write("[task-state] " + json.dumps(event, separators=(",", ":"), ensure_ascii=True) + "\n")


_TASK_STATE_PUBLIC_FIELDS = {
    "version", "event", "caller", "scope", "action", "transition", "phase",
    "observedState", "expectedState", "snapshotHash", "durationMs",
    "slowThresholdMs", "slow", "scanMode", "taskBodyReads",
    "architectureStatus", "findings", "findingsTruncated", "result", "ok",
    "overallOk",
}


def _relay_regen_task_state_observations(stderr: str, *, check: bool,
                                         success: bool) -> None:
    """Relay only exact, reprojected task-index observations from regen-index."""
    action = "index-check" if check else "index-publish"
    expected_count = 2 if check else 6
    accepted: List[bytes] = []
    malformed = len(stderr.encode("utf-8", "replace")) > 256 * 1024
    if not malformed:
        for line in stderr.splitlines():
            if not line.startswith("[task-state] "):
                continue
            payload = line[len("[task-state] "):]
            try:
                source = json.loads(payload)
            except (ValueError, RecursionError):
                malformed = True
                continue
            if (not isinstance(source, dict) or set(source) != _TASK_STATE_PUBLIC_FIELDS or
                    source.get("caller") != "server" or source.get("scope") != "all" or
                    source.get("action") != action):
                malformed = True
                continue
            raw = ("[task-state] " + json.dumps(
                source, separators=(",", ":"), ensure_ascii=True) + "\n").encode("ascii")
            projected = _project_task_state_observation(raw, "INDEX_OBSERVATION_INVALID", 0)
            if projected != source:
                malformed = True
                continue
            accepted.append(raw)
    if success and (malformed or len(accepted) != expected_count):
        phases = (["pre", "post"] if check else
                  ["pre", "pre", "pre", "post", "post", "post"])
        for phase in phases:
            _emit_task_state_observation(
                b"", "INDEX_OBSERVATION_INVALID", 0, fallback_scope="all",
                fallback_action=action, fallback_phase=phase)
        return
    for raw in accepted:
        _emit_task_state_observation(raw, "INDEX_OBSERVATION_INVALID", 0)
    if malformed:
        _emit_task_state_observation(
            b"", "INDEX_OBSERVATION_INVALID", 0, fallback_scope="all",
            fallback_action=action, fallback_phase="post")


def run_task_state_validation(*, stem: Optional[str] = None,
                              expect: Optional[str] = None,
                              transition: Optional[str] = None,
                              phase: Optional[str] = None,
                              check_index: bool,
                              code: str,
                              proposal: Optional[bytes] = None,
                              proposal_state: Optional[str] = None,
                              proposal_from_state: Optional[str] = None,
                              active_runtime: Optional[dict] = None,
                              observation_action: Optional[str] = None) -> dict:
    """Run the canonical, read-only task-state boundary with bounded I/O.

    The deterministic publisher remains implemented in Python for its
    crash-recovery protocol, but lifecycle truth is owned by the shared Node
    validator.  This adapter deliberately invokes one process for the complete
    snapshot; it never reimplements parsers or decides from partial output.
    """
    read_regular(TASK_STATE_VALIDATOR, max_bytes=2 * 1024 * 1024,
                 code="TASK_STATE_VALIDATOR_INVALID")
    args = [os.environ.get("CREATE_BACKLOG_NODE", "node"),
            str(TASK_STATE_VALIDATOR)]
    if stem is None:
        args.append("--all")
    else:
        args.extend(["--stem", stem])
    if proposal is not None:
        from_state = proposal_from_state or proposal_state
        same_state = from_state == proposal_state
        create_candidate = from_state == "absent" and proposal_state == "backlog"
        if (stem is None or transition is not None or check_index or
                proposal_state not in ("backlog", "pending", "todo", "done") or
                (not same_state and not create_candidate)):
            fail("TASK_STATE_VALIDATOR_INVALID",
                 "proposal validation requires same-state bytes or absent:backlog and no INDEX/transition check",
                 recoverable=True)
        args.extend(["--proposal", "-", "--proposal-state", proposal_state])
        if from_state != proposal_state:
            args.extend(["--proposal-from-state", from_state])
    if transition is not None:
        if phase not in ("pre", "post"):
            fail("TASK_STATE_VALIDATOR_INVALID", "transition validation requires pre/post phase", recoverable=True)
        args.extend(["--transition", transition, "--phase", phase])
    elif stem is not None and expect is not None:
        args.extend(["--expect", expect])
    if check_index:
        args.append("--check-index")
    args.extend(["--json", "--caller", "server"])
    child_env = dict(os.environ)
    child_env["ORCHESTRATOR_PROJECT_ROOT"] = str(PROJECT_ROOT)
    child_env["ORCHESTRATOR_TASKS_DIR"] = str(TASKS_DIR)
    # Runtime-integrity collectors are part of the canonical validator.  Bind
    # every collector to this publisher's configured cache root; inheriting a
    # controller's/canonical checkout paths would mix two projects in one
    # verdict and makes isolated recovery fixtures fail closed for the wrong
    # workspace.
    child_env["ORCHESTRATOR_LOCKS_DIR"] = str(CACHE_DIR / "locks")
    child_env["ORCHESTRATOR_REQUESTS_DIR"] = str(CACHE_DIR / "requests")
    child_env["ORCHESTRATOR_REQUEST_RESERVATIONS_DIR"] = str(CACHE_DIR / "request-reservations")
    child_env["ORCHESTRATOR_RUNS_DIR"] = str(CACHE_DIR / "runs")
    child_env["ORCHESTRATOR_FINALIZATIONS_DIR"] = str(FINALIZATIONS_DIR)
    child_env["ORCHESTRATOR_TASK_CREATIONS_DIR"] = str(CREATIONS_DIR)
    child_env["ORCHESTRATOR_TASK_EDITS_DIR"] = str(CACHE_DIR / "edits")
    child_env["ORCHESTRATOR_TASK_INTAKE_DIR"] = str(CACHE_DIR / "intake")
    child_env["ORCHESTRATOR_TRANSITIONS_DIR"] = str(CACHE_DIR / "transitions")
    _bind_active_runtime_context(child_env, active_runtime)
    run_io = ({"input": proposal} if proposal is not None else
              {"stdin": subprocess.DEVNULL})
    validation_started = time.monotonic_ns()
    try:
        result = subprocess.run(
            args, cwd=str(PROJECT_ROOT), env=child_env,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            timeout=TASK_STATE_TIMEOUT_SECONDS, check=False,
            **run_io,
        )
    except (OSError, subprocess.SubprocessError) as exc:
        _emit_task_state_observation(b"", code,
                                     (time.monotonic_ns() - validation_started) // 1_000_000,
                                     fallback_action=observation_action)
        fail(code, f"canonical task-state validator could not run: {bounded(exc)}", recoverable=True)
    elapsed_ms = (time.monotonic_ns() - validation_started) // 1_000_000
    if len(result.stdout) > TASK_STATE_MAX_BYTES or len(result.stderr) > 64 * 1024:
        _emit_task_state_observation(b"", code, elapsed_ms,
                                     fallback_action=observation_action)
        fail(code, "canonical task-state validator exceeded its output bound", recoverable=True)
    _emit_task_state_observation(result.stderr, code, elapsed_ms,
                                 fallback_action=observation_action)
    try:
        value = json.loads(result.stdout.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail(code, f"canonical task-state validator returned invalid JSON: {bounded(exc)}", recoverable=True)
    if not isinstance(value, dict) or value.get("version") != 1 or type(value.get("ok")) is not bool:
        fail(code, "canonical task-state validator returned an invalid result envelope", recoverable=True)
    if result.returncode != 0 or not value["ok"]:
        findings = value.get("findings") if isinstance(value.get("findings"), list) else []
        codes = [str(item.get("code")) for item in findings[:12] if isinstance(item, dict)]
        fail(code, "canonical task-state validation failed" +
             (f" ({', '.join(codes)})" if codes else ""), recoverable=True,
             details={"taskState": {
                 "observedState": value.get("observedState"),
                 "sourceRevision": value.get("sourceRevision"),
                 "snapshotHash": value.get("snapshotHash"),
                 "findingCodes": codes,
             }})
    return value


def exact_task_locations(stem: str) -> List[Tuple[str, Path]]:
    candidates = [
        ("backlog", TASKS_DIR / "backlog" / f"{stem}.md"),
        ("pending", TASKS_DIR / "pending" / f"{stem}.questions.md"),
        ("todo", TASKS_DIR / "todo" / f"{stem}.md"),
        ("done", TASKS_DIR / "done" / f"{stem}.md"),
    ]
    out: List[Tuple[str, Path]] = []
    for column, path in candidates:
        if read_regular(path, max_bytes=TASK_MAX_BYTES,
                        code="TASK_VERIFY_FAILED", required=False) is not None:
            out.append((column, path))
    # backlog + pending sidecar is one logical pending representation.
    if len(out) == 2 and {x[0] for x in out} == {"backlog", "pending"}:
        return [("pending", TASKS_DIR / "backlog" / f"{stem}.md")]
    return out


def index_locations(index: dict, stem: str) -> List[Tuple[str, dict]]:
    out: List[Tuple[str, dict]] = []
    for column in ("backlog", "pending", "todo", "done"):
        for entry in index[column]:
            if entry.get("stem") == stem:
                out.append((column, entry))
    return out


def verify_created(req: dict, marker: dict, data: bytes) -> dict:
    target = task_path(marker["stem"])
    live, live_proof = read_regular_proof(
        target, max_bytes=TASK_MAX_BYTES, code="TASK_VERIFY_FAILED")
    if (live != data or sha256(live) != marker.get("sourceHash") or
            marker.get("targetProof") is None or
            not _same_generation(live_proof, marker["targetProof"])):
        fail("TASK_VERIFY_FAILED",
             "backlog task no longer matches the exact created target generation",
             recoverable=True)
    index = read_index()
    locations = index_locations(index, marker["stem"])
    if len(locations) != 1 or locations[0][0] != "backlog":
        fail("INDEX_VERIFY_FAILED", f"created stem must appear exactly once in INDEX backlog; found {[x[0] for x in locations]}", recoverable=True)
    entry = locations[0][1]
    if entry.get("title") != req["title"]:
        fail("INDEX_VERIFY_FAILED", "INDEX title does not match the created task heading", recoverable=True)
    if req["originStem"] and entry.get("splitFrom") != req["originStem"]:
        fail("INDEX_VERIFY_FAILED", "INDEX splitFrom does not match originStem", recoverable=True)
    # Includes files, INDEX, permanent sentinels, and every durable receipt;
    # raises if the number is now owned by another logical stem.
    scan_numbers(current_marker=marker)
    return entry


COMMENT_RE = re.compile(r"<!--\s*(figma-visual-fix|figma-actualize)\s+([^>]*?)-->", re.I)
ATTR_RE = re.compile(r"(?:^|\s)(key|report)=([^\s>]+)", re.I)


def domain_dedup(req: dict) -> Optional[dict]:
    key = req.get("dedupKey")
    if not key:
        return None
    pending_stems = {stem for column, stem, _number, _path in list_task_artifacts() if column == "pending"}
    matches: Dict[str, dict] = {}
    for column, stem, number, path in list_task_artifacts():
        raw = read_regular(path, max_bytes=TASK_MAX_BYTES, code="UNSAFE_TASK_ARTIFACT")
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError:
            fail("UNSAFE_TASK_ARTIFACT", f"task is not valid UTF-8: {path}")
        logical_column = "pending" if column == "backlog" and stem in pending_stems else column
        for comment in COMMENT_RE.finditer(text):
            attrs = {m.group(1).lower(): m.group(2) for m in ATTR_RE.finditer(comment.group(2))}
            if attrs.get("key") != key:
                continue
            report = attrs.get("report")
            eligible = logical_column != "done" or req.get("dedupReport") is None or report == req.get("dedupReport")
            if eligible:
                matches[stem] = {"stem": stem, "number": number, "column": logical_column,
                                 "sourceHash": sha256(raw), "report": report}
    if len(matches) > 1:
        fail("DEDUP_CONFLICT", "dedupKey matches multiple eligible tasks: " + ", ".join(sorted(matches)))
    return next(iter(matches.values())) if matches else None


def verify_existing_effect(stem: str, number: int, expected_hash: Optional[str] = None,
                           expected_column: Optional[str] = None,
                           active_runtime: Optional[dict] = None) -> Tuple[str, dict]:
    """Rebuild derived state, then prove one exact logical task/index identity."""
    assert_dependency_graph_mutex_held()
    run_task_state_validation(stem=stem, expect=expected_column, check_index=False,
                              code="TASK_STATE_POSTCONDITION_FAILED",
                              active_runtime=active_runtime,
                              observation_action="create")
    assert_dependency_graph_mutex_held()
    run_regen(False)
    assert_dependency_graph_mutex_held()
    run_regen(True)
    run_task_state_validation(stem=stem, check_index=True,
                              code="TASK_STATE_INDEX_POSTCONDITION_FAILED",
                              active_runtime=active_runtime,
                              observation_action="create")
    assert_dependency_graph_mutex_held()
    locations = exact_task_locations(stem)
    if len(locations) != 1:
        fail("TASK_VERIFY_FAILED", f"completed creation effect {stem} has {len(locations)} logical locations", recoverable=True)
    column, body_path = locations[0]
    if expected_column is not None and column != expected_column:
        fail("TASK_VERIFY_FAILED", f"completed creation effect moved from expected {expected_column} to {column}", recoverable=True)
    live = read_regular(body_path, max_bytes=TASK_MAX_BYTES, code="TASK_VERIFY_FAILED")
    if expected_hash is not None and sha256(live) != expected_hash:
        fail("TASK_VERIFY_FAILED", "completed creation effect bytes no longer match the bound source hash", recoverable=True)
    if parse_task_number(stem, source="completed creation effect") != number:
        fail("TASK_VERIFY_FAILED", "completed creation effect number disagrees with its stem", recoverable=True)
    index = read_index()
    indexed = index_locations(index, stem)
    if len(indexed) != 1 or indexed[0][0] != column:
        fail("INDEX_VERIFY_FAILED", f"completed creation effect must appear once in INDEX {column}", recoverable=True)
    scan_numbers()
    return column, indexed[0][1]


def completed_response(marker: dict, *, replay: bool) -> dict:
    stem = marker.get("stem")
    if not stem:
        fail("MARKER_INVALID", "completed creation receipt has no stem", recoverable=True)
    # Idempotent replay never creates a duplicate, but it also never reports a
    # false success from stale/corrupt derived state. Canonical regeneration is
    # safe under the creation mutex and repairs a merely stale INDEX.
    column, task_entry = verify_existing_effect(stem, marker["number"])
    return {
        "ok": True,
        "created": marker.get("effect") == "created" and not replay,
        "deduped": replay or marker.get("effect") == "domain-dedup",
        "effect": marker.get("effect"),
        "stem": stem,
        "number": marker.get("number"),
        "column": column,
        "task": task_entry,
        "sourceHash": marker.get("sourceHash"),
        "transactionId": marker.get("transactionId"),
        "replayed": replay,
    }


def maybe_failpoint(name: str) -> None:
    configured = os.environ.get("CREATE_BACKLOG_FAILPOINT", "")
    if configured and configured not in FAILPOINTS:
        fail("FAILPOINT_INVALID", f"unknown CREATE_BACKLOG_FAILPOINT {configured!r}", exit_code=2)
    if configured == name:
        sys.stdout.flush()
        sys.stderr.flush()
        os._exit(86)  # kernel releases the mutex; durable writes were fsynced


def configured_mutex_timeout() -> int:
    timeout_raw = os.environ.get("CREATE_BACKLOG_MUTEX_TIMEOUT_MS", "30000")
    try:
        timeout_ms = int(timeout_raw)
    except ValueError:
        fail("MUTEX_TIMEOUT_INVALID", "CREATE_BACKLOG_MUTEX_TIMEOUT_MS must be an integer", exit_code=2)
    return max(1, min(timeout_ms, 10 * 60 * 1000))


def configured_dependency_graph_mutex_timeout() -> int:
    """Return the bounded wait shared by every dependency-graph publisher."""
    timeout_raw = os.environ.get(
        "ORCHESTRATOR_DEPENDENCY_GRAPH_MUTEX_TIMEOUT_MS", "600000")
    if re.fullmatch(r"[1-9][0-9]{0,9}", timeout_raw) is None:
        fail("DEPENDENCY_GRAPH_MUTEX_TIMEOUT_INVALID",
             "ORCHESTRATOR_DEPENDENCY_GRAPH_MUTEX_TIMEOUT_MS must be an exact positive decimal",
             exit_code=2)
    timeout_ms = int(timeout_raw)
    return max(1, min(timeout_ms, 60 * 60 * 1000))


def ensure_roots() -> None:
    ensure_real_dir(TASKS_DIR, create=False)
    for name in ("backlog", "pending", "todo", "done"):
        ensure_real_dir(TASKS_DIR / name, create=False)
    ensure_real_dir(CACHE_DIR, create=True)


def nested_parent_credentials() -> Optional[dict]:
    own = os.environ.get("CREATE_BACKLOG_OWN_WRITER_LEASE_ID", "")
    parent_stem = os.environ.get("CREATE_BACKLOG_PARENT_STEM", "")
    site_session = os.environ.get("ORCHESTRATOR_WRITER_SESSION_ID", "")
    site_lease = os.environ.get("ORCHESTRATOR_WRITER_LEASE_ID", "")
    site_delegation = os.environ.get("ORCHESTRATOR_WRITER_DELEGATION_TOKEN", "")
    parent_lease = os.environ.get("CREATE_BACKLOG_PARENT_WRITER_LEASE_ID", "")
    parent_token = os.environ.get("CREATE_BACKLOG_PARENT_WRITER_LEASE_TOKEN", "")
    bounded_session = os.environ.get("CREATE_BACKLOG_PARENT_WRITER_SESSION_ID", "")
    supplied = any((parent_stem, site_session, site_lease, site_delegation,
                    parent_lease, parent_token, bounded_session))
    if own and supplied:
        fail("CREATE_WRITER_AUTHORITY_CONFLICT",
             "create helper received both workspace and nested parent authority", recoverable=True)
    if not supplied:
        return None
    site_mode = any((site_session, site_lease, site_delegation))
    bounded_mode = any((parent_lease, parent_token, bounded_session))
    if (
            not _canonical_task_stem(parent_stem) or
            site_mode == bounded_mode or
            (site_mode and (
                not SESSION_ID_RE.fullmatch(site_session) or
                not LEASE_ID_RE.fullmatch(site_lease) or
                not DELEGATION_TOKEN_RE.fullmatch(site_delegation)
            )) or
            (bounded_mode and (
                not LEASE_ID_RE.fullmatch(parent_lease) or
                not WRITER_TOKEN_RE.fullmatch(parent_token) or
                not SESSION_ID_RE.fullmatch(bounded_session)
            ))):
        fail("CREATE_PARENT_WRITER_INVALID",
             "nested create requires one complete site or bounded parent receipt", recoverable=True)
    return {
        "mode": "site" if site_mode else "bounded",
        "stem": parent_stem,
        "sessionId": site_session if site_mode else bounded_session,
        "leaseId": site_lease if site_mode else parent_lease,
        "token": None if site_mode else parent_token,
        "delegationToken": site_delegation if site_mode else None,
    }


def writer_cli_env() -> dict:
    child_env = dict(os.environ)
    child_env["FINALIZE_PROJECT_ROOT"] = str(PROJECT_ROOT)
    child_env["FINALIZE_STATE_DIR"] = str(FINALIZATIONS_DIR)
    child_env["FINALIZE_WRITER_AUTHORITY_ROOT"] = str(AUTHORITY_ROOT)
    child_env["FINALIZE_CREATIONS_DIR"] = str(CREATIONS_DIR)
    child_env["FINALIZE_EDITS_DIR"] = str(CACHE_DIR / "edits")
    return child_env


def acquire_nested_publication_guard(parent: dict, *, recovery: bool,
                                     creation_key_hash: Optional[str]) -> dict:
    ensure_real_dir(FINALIZATIONS_DIR, create=False, code="CREATE_WRITER_LEASE_UNSAFE")
    read_regular(WRITER_SCAN_SCRIPT, max_bytes=256 * 1024, code="CREATE_WRITER_SCAN_FAILED")
    publication_key = "task:recover-backlog-creations" if recovery else "task:create-backlog"
    args = [
        os.environ.get("CREATE_BACKLOG_NODE", "node"), str(WRITER_SCAN_SCRIPT),
        "acquire-publication-guard", "--key", publication_key,
        "--owner-pid", str(os.getpid()), "--parent-stem", parent["stem"],
        "--parent-session-id", parent["sessionId"],
    ]
    args.extend(["--parent-lease-id", parent["leaseId"]])
    if parent["mode"] == "bounded":
        args.extend(["--parent-token", parent["token"]])
    if recovery:
        args.append("--allow-all-creation-recovery")
    else:
        if not HASH_RE.fullmatch(str(creation_key_hash or "")):
            fail("CREATE_PUBLICATION_GUARD_INVALID",
                 "nested create publication guard requires an exact idempotency key hash",
                 recoverable=True)
        args.extend(["--creation-key-hash", creation_key_hash])
    try:
        result = subprocess.run(
            args, cwd=str(PROJECT_ROOT), env=writer_cli_env(), stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            timeout=WRITER_SCAN_TIMEOUT_SECONDS, check=False,
        )
    except (OSError, subprocess.SubprocessError) as exc:
        fail("CREATE_PUBLICATION_GUARD_FAILED",
             f"cannot acquire nested publication guard: {bounded(exc)}", recoverable=True)
    if result.returncode != 0 or len(result.stdout) > 64 * 1024 or len(result.stderr) > 16 * 1024:
        detail = result.stderr.decode("utf-8", "replace").strip()
        fail("CREATE_PUBLICATION_GUARD_REFUSED",
             "nested publication guard refused acquisition" +
             (f": {bounded(detail)}" if detail else ""), recoverable=True)
    try:
        value = json.loads(result.stdout.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail("CREATE_PUBLICATION_GUARD_INVALID",
             f"nested publication guard returned invalid JSON: {bounded(exc)}", recoverable=True)
    expected_fields = {
        "version", "leaseId", "token", "kind", "stem", "sessionId", "key",
        "expiresAt", "delegationKind", "delegationToken", "callerPid",
        "callerProcessStartId", "authorityPid", "authorityProcessStartId",
        "parentLeaseId", "parentStem", "parentSessionId",
    }
    current_process_start_id = process_start_identity(os.getpid())
    if (
            not isinstance(value, dict) or set(value) != expected_fields or
            value.get("version") != 1 or not LEASE_ID_RE.fullmatch(str(value.get("leaseId", ""))) or
            not WRITER_TOKEN_RE.fullmatch(str(value.get("token", ""))) or
            value.get("kind") != "lock-writer" or value.get("stem") is not None or
            value.get("sessionId") != parent["sessionId"] or value.get("key") != publication_key or
            value.get("expiresAt") is not None or
            value.get("delegationKind") != (
                "site-process-tree" if parent["mode"] == "site" else "bounded-receipt") or
            not DELEGATION_TOKEN_RE.fullmatch(str(value.get("delegationToken", ""))) or
            value.get("callerPid") != os.getpid() or
            not PROCESS_START_ID_RE.fullmatch(str(value.get("callerProcessStartId", ""))) or
            value.get("callerProcessStartId") != current_process_start_id or
            not isinstance(value.get("authorityPid"), int) or value.get("authorityPid") <= 0 or
            not PROCESS_START_ID_RE.fullmatch(str(value.get("authorityProcessStartId", ""))) or
            not LEASE_ID_RE.fullmatch(str(value.get("parentLeaseId", ""))) or
            value.get("parentStem") != parent["stem"] or
            value.get("parentSessionId") != parent["sessionId"] or
            value.get("parentLeaseId") != parent["leaseId"]):
        fail("CREATE_PUBLICATION_GUARD_INVALID",
             "nested publication guard returned a mismatched receipt", recoverable=True)
    return value


def release_nested_publication_guard(guard: dict) -> None:
    args = [
        os.environ.get("CREATE_BACKLOG_NODE", "node"), str(WRITER_SCAN_SCRIPT),
        "release", "--lease-id", guard["leaseId"], "--token", guard["token"],
    ]
    try:
        result = subprocess.run(
            args, cwd=str(PROJECT_ROOT), env=writer_cli_env(), stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            timeout=WRITER_SCAN_TIMEOUT_SECONDS, check=False,
        )
    except (OSError, subprocess.SubprocessError) as exc:
        fail("CREATE_PUBLICATION_GUARD_RELEASE_FAILED",
             f"cannot release nested publication guard: {bounded(exc)}", recoverable=True)
    try:
        value = json.loads(result.stdout.decode("utf-8")) if result.stdout else None
    except (UnicodeDecodeError, json.JSONDecodeError):
        value = None
    if (
            result.returncode != 0 or len(result.stdout) > 64 * 1024 or
            len(result.stderr) > 16 * 1024 or value != {
                "released": True, "leaseId": guard["leaseId"],
            }):
        detail = result.stderr.decode("utf-8", "replace").strip()
        fail("CREATE_PUBLICATION_GUARD_RELEASE_FAILED",
             "nested publication guard could not be ownership-safely released" +
             (f": {bounded(detail)}" if detail else ""), recoverable=True)


@contextlib.contextmanager
def nested_publication_guard(parent: Optional[dict], *, recovery: bool,
                             creation_key_hash: Optional[str]):
    if parent is None:
        yield None
        return
    guard = acquire_nested_publication_guard(
        parent, recovery=recovery, creation_key_hash=creation_key_hash)
    original: Optional[BaseException] = None
    try:
        yield guard
    except BaseException as exc:
        original = exc
        raise
    finally:
        try:
            release_nested_publication_guard(guard)
        except CreateError as release_error:
            if original is None:
                raise
            if isinstance(original, CreateError):
                original.details.setdefault("publicationGuardRelease", {
                    "code": release_error.code, "message": bounded(release_error),
                })


def writer_scan(*, recovery: bool, parent: Optional[dict] = None,
                publication_guard: Optional[dict] = None) -> dict:
    own = os.environ.get("CREATE_BACKLOG_OWN_WRITER_LEASE_ID", "")
    # The bypass is intentionally test-only and is ignored whenever a
    # controller supplied an ownership credential.
    if _fixture_mode_enabled("CREATE_BACKLOG_TEST_ALLOW_UNLEASED") and not own and parent is None:
        return {"active": [], "stale": [], "issues": [], "testUnleased": True}
    if own and parent is not None:
        fail("CREATE_WRITER_AUTHORITY_CONFLICT",
             "create helper received both workspace and nested parent authority", recoverable=True)
    if not LEASE_ID_RE.fullmatch(own) and parent is None:
        fail("CREATE_WRITER_LEASE_MISSING", "create helper has no authenticated writer lease", recoverable=True)
    if parent is not None and publication_guard is None:
        fail("CREATE_PUBLICATION_GUARD_MISSING",
             "nested create has no supplemental global publication guard", recoverable=True)
    if parent is None and publication_guard is not None:
        fail("CREATE_PUBLICATION_GUARD_INVALID",
             "supplemental publication guard has no parent authority", recoverable=True)
    ensure_real_dir(FINALIZATIONS_DIR, create=False, code="CREATE_WRITER_LEASE_UNSAFE")
    read_regular(WRITER_SCAN_SCRIPT, max_bytes=256 * 1024, code="CREATE_WRITER_SCAN_FAILED")
    node = os.environ.get("CREATE_BACKLOG_NODE", "node")
    try:
        result = subprocess.run(
            [node, str(WRITER_SCAN_SCRIPT), "scan"], cwd=str(PROJECT_ROOT), env=writer_cli_env(),
            stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
            timeout=WRITER_SCAN_TIMEOUT_SECONDS, check=False,
        )
    except (OSError, subprocess.SubprocessError) as exc:
        fail("CREATE_WRITER_SCAN_FAILED", f"cannot inspect writer leases: {bounded(exc)}", recoverable=True)
    if len(result.stdout) > WRITER_SCAN_MAX_BYTES or len(result.stderr) > 16 * 1024 or result.returncode != 0:
        fail("CREATE_WRITER_SCAN_FAILED", "writer lease scan failed or exceeded its output bound", recoverable=True)
    try:
        value = json.loads(result.stdout.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail("CREATE_WRITER_SCAN_FAILED", f"writer lease scan returned invalid JSON: {bounded(exc)}", recoverable=True)
    if not isinstance(value, dict) or set(value) != {"active", "stale", "issues"} or not all(
        isinstance(value.get(key), list) for key in ("active", "stale", "issues")
    ):
        fail("CREATE_WRITER_SCAN_FAILED", "writer lease scan returned an invalid contract", recoverable=True)
    if value["issues"]:
        issue = value["issues"][0]
        detail = issue.get("message") if isinstance(issue, dict) else issue
        fail("CREATE_WRITER_LEASE_UNSAFE", f"workspace writer lease state is unsafe: {bounded(detail)}", recoverable=True)
    authority_id = publication_guard["parentLeaseId"] if parent is not None else own
    own_rows = [row for row in value["active"] if isinstance(row, dict) and row.get("leaseId") == authority_id]
    if len(own_rows) != 1:
        fail("CREATE_WRITER_LEASE_MISSING", "authenticated create writer lease is not active exactly once", recoverable=True)
    row = own_rows[0]
    current_process_start_id = process_start_identity(os.getpid())
    if sys.platform in ("linux", "darwin", "win32") and current_process_start_id is None:
        fail("CREATE_WRITER_LEASE_UNSAFE",
             "cannot prove this helper process generation", recoverable=True)
    allowed_ids = {authority_id}
    if parent is not None:
        expiry = row.get("expiresAt")
        bounded_expiry_ok = False
        if parent["mode"] == "bounded" and valid_iso8601(expiry):
            try:
                bounded_expiry_ok = _datetime.datetime.fromisoformat(
                    expiry.removesuffix("Z") + "+00:00") > _datetime.datetime.now(_datetime.timezone.utc)
            except ValueError:
                bounded_expiry_ok = False
        parent_shape_ok = (
            row.get("kind") == "task-session" and row.get("unverified") is False and
            row.get("stem") == parent["stem"] and row.get("sessionId") == parent["sessionId"] and
            not isinstance(row.get("owner"), list) and isinstance(row.get("owner"), dict) and
            row["owner"].get("hostname") == socket.gethostname()
        )
        if parent["mode"] == "site":
            parent_shape_ok = parent_shape_ok and (
                row.get("expiresAt") is None and isinstance(row.get("childPid"), int) and
                row.get("childPid") == publication_guard["authorityPid"] and
                row.get("childProcessStartId") == publication_guard["authorityProcessStartId"] and
                row.get("key") == f"task:{parent['stem']}"
            )
        else:
            parent_shape_ok = parent_shape_ok and (
                row.get("leaseId") == parent["leaseId"] and row.get("token") == parent["token"] and
                row.get("owner", {}).get("pid") == publication_guard["authorityPid"] and
                row.get("owner", {}).get("processStartId") == publication_guard["authorityProcessStartId"] and
                bounded_expiry_ok
            )
        guard_rows = [record for record in value["active"] if isinstance(record, dict) and
                      record.get("leaseId") == publication_guard["leaseId"]]
        guard_row = guard_rows[0] if len(guard_rows) == 1 else None
        guard_shape_ok = guard_row is not None and (
            guard_row.get("token") == publication_guard["token"] and
            guard_row.get("kind") == "lock-writer" and guard_row.get("stem") is None and
            guard_row.get("sessionId") == parent["sessionId"] and
            guard_row.get("key") == ("task:recover-backlog-creations" if recovery else "task:create-backlog") and
            guard_row.get("expiresAt") is None and guard_row.get("childPid") is None and
            guard_row.get("childProcessStartId") is None and
            guard_row.get("delegationHash") == sha256(
                publication_guard["delegationToken"].encode("ascii")) and
            guard_row.get("unverified") is False and isinstance(guard_row.get("owner"), dict) and
            guard_row["owner"].get("pid") == publication_guard["callerPid"] == os.getpid() and
            guard_row["owner"].get("processStartId") == publication_guard["callerProcessStartId"] == current_process_start_id and
            guard_row["owner"].get("hostname") == socket.gethostname()
        )
        if not parent_shape_ok:
            fail("CREATE_PARENT_WRITER_INVALID", "nested create authority does not own the parent task", recoverable=True)
        if not guard_shape_ok:
            fail("CREATE_PUBLICATION_GUARD_INVALID",
                 "nested create global publication guard is not active exactly once", recoverable=True)
        allowed_ids.add(publication_guard["leaseId"])
    else:
        expected_key = "task:recover-backlog-creations" if recovery else "task:create-backlog"
        if (
            row.get("kind") != "workspace-session" or row.get("childPid") != os.getpid() or
            row.get("childProcessStartId") != current_process_start_id or
            row.get("unverified") is not False or row.get("stem") is not None or
            row.get("key") != expected_key or row.get("expiresAt") is not None or
            not isinstance(row.get("owner"), dict) or row["owner"].get("hostname") != socket.gethostname()
        ):
            fail("CREATE_WRITER_LEASE_INVALID", "create writer lease is not attached to this helper", recoverable=True)
    foreign = [record for record in value["active"]
               if not isinstance(record, dict) or record.get("leaseId") not in allowed_ids]
    if foreign:
        label = foreign[0].get("stem") if isinstance(foreign[0], dict) else None
        fail("WORKSPACE_WRITER_ACTIVE", "another workspace writer is active" + (f" for {label}" if label else ""), recoverable=True)
    return value


def _process_generation_proven_gone(owner: Any) -> bool:
    """Prove that one exact local process generation can no longer own state."""
    if (not isinstance(owner, dict) or owner.get("hostname") != socket.gethostname() or
            isinstance(owner.get("pid"), bool) or not isinstance(owner.get("pid"), int) or
            owner["pid"] <= 0 or
            not PROCESS_START_ID_RE.fullmatch(str(owner.get("processStartId", "")))):
        return False
    observed = process_start_identity(owner["pid"])
    if observed is not None:
        # A different valid start identity proves PID reuse; the recorded
        # generation is gone even though a new process now owns the number.
        return observed != owner["processStartId"]
    try:
        os.kill(owner["pid"], 0)
    except ProcessLookupError:
        return True
    except (PermissionError, OSError):
        return False
    return False


def reconcile_stale_nested_publication_guards(
        writer_state: dict, *, parent: Optional[dict],
        publication_guard: Optional[dict]) -> dict:
    """Recover-all-only exact reconciliation for dead nested guard leases.

    A fresh guard and its still-active parent have already been proven by
    writer_scan.  We release only stale lock-writer generations from the same
    parent session, with a canonical create/recover key and a locally proven
    dead/reused owner generation.  release_nested_publication_guard performs
    the token-bound detach/verify/no-clobber CAS.  Any ambiguous or foreign row
    remains visible and the mandatory rescan keeps recovery fail-closed.
    """
    if parent is None or publication_guard is None:
        return writer_state
    allowed_keys = {"task:create-backlog", "task:recover-backlog-creations"}
    for row in writer_state.get("stale", []):
        exact_shape = (
            isinstance(row, dict) and row.get("kind") == "lock-writer" and
            row.get("key") in allowed_keys and row.get("stem") is None and
            row.get("sessionId") == parent.get("sessionId") == publication_guard.get("parentSessionId") and
            row.get("expiresAt") is None and row.get("childPid") is None and
            row.get("childProcessStartId") is None and row.get("unverified") is False and
            row.get("unverifiedReason") is None and
            HASH_RE.fullmatch(str(row.get("delegationHash", ""))) and
            LEASE_ID_RE.fullmatch(str(row.get("leaseId", ""))) and
            WRITER_TOKEN_RE.fullmatch(str(row.get("token", ""))) and
            row.get("leaseId") not in {parent.get("leaseId"), publication_guard.get("leaseId")} and
            _process_generation_proven_gone(row.get("owner"))
        )
        if not exact_shape:
            continue
        release_nested_publication_guard({
            "leaseId": row["leaseId"], "token": row["token"],
        })
        # The stale generation is already durably removed.  A crash here must
        # be safe: the new recovery guard becomes stale in turn and the next
        # recover-all proves/releases it through the same exact protocol.
        maybe_failpoint("after-stale-guard-reconcile")
    return writer_scan(recovery=True, parent=parent,
                       publication_guard=publication_guard)


def creation_runtime_authority(writer_state: dict, *, recovery: bool,
                               parent: Optional[dict],
                               publication_guard: Optional[dict]) -> dict:
    publication_key = "task:recover-backlog-creations" if recovery else "task:create-backlog"
    if writer_state.get("testUnleased") is True:
        return {"mode": "fixture-unleased", "authorityLeaseId": None,
                "publicationGuardLeaseId": None, "publicationKey": publication_key}
    if publication_guard is not None:
        return {"mode": "nested-guard", "authorityLeaseId": parent.get("leaseId") if parent else None,
                "publicationGuardLeaseId": publication_guard.get("leaseId"),
                "publicationKey": publication_key}
    return {"mode": "owned-lease",
            "authorityLeaseId": os.environ.get("CREATE_BACKLOG_OWN_WRITER_LEASE_ID"),
            "publicationGuardLeaseId": None, "publicationKey": publication_key}


def creation_active_runtime(marker: dict, authority: dict) -> dict:
    return {
        "kind": "creation", "path": marker_path(marker["keyHash"]), "marker": marker,
        "mode": authority["mode"], "authorityLeaseId": authority["authorityLeaseId"],
        "publicationGuardLeaseId": authority["publicationGuardLeaseId"],
        "publicationKey": authority["publicationKey"],
    }


def prevalidate_fresh_creation(req: dict) -> Optional[dict]:
    """Validate a never-claimed create intent before any durable reservation.

    Domain-dedup requests publish no candidate body, so their existing effect is
    verified by the dedicated path. Every actual create validates its exact
    rendered absent→backlog bytes in the canonical core before publishing an
    idempotency marker.  The global runtime/WAL precondition deliberately stays
    in ``resume_transaction`` after claim (and still before number allocation):
    only that boundary has the combined recovery authority needed to admit a
    legitimate sibling EDIT WAL.  This split keeps malformed proposal bytes at
    zero durable effects without breaking deterministic EDIT+CREATE recovery.
    """
    if domain_dedup(req) is not None:
        return None
    slug = slugify(req["title"])
    try:
        numbers, _owners = scan_numbers()
    except CreateError:
        # Preserve the canonical integrity envelope for a broken allocation
        # snapshot without making global runtime admission part of the normal
        # pre-marker path.  This diagnostic call is reached only after the
        # read-only snapshot has already failed; a healthy simultaneous EDIT
        # WAL therefore remains eligible for combined recovery after claim.
        run_task_state_validation(
            check_index=True, code="TASK_STATE_CREATE_PRECONDITION_FAILED",
            observation_action="create")
        raise
    number = max(numbers, default=0) + 1
    if number > MAX_SAFE_TASK_NUMBER:
        fail("TASK_NUMBER_EXHAUSTED",
             "no exact task number remains in the supported range",
             recoverable=True)
    stem = f"TASK_{number}_{slug}"
    if len(stem) > STEM_MAX:
        fail("STEM_TOO_LARGE",
             f"generated stem exceeds {STEM_MAX} characters", recoverable=True)
    data = source_bytes(req, {"number": number})
    if len(data) > TASK_MAX_BYTES:
        fail("TASK_TOO_LARGE", f"rendered task exceeds {TASK_MAX_BYTES} bytes", exit_code=2)
    verdict = run_task_state_validation(
        stem=stem, check_index=False,
        code="TASK_STATE_CREATE_PROPOSAL_INVALID",
        proposal=data, proposal_state="backlog", proposal_from_state="absent",
        observation_action="create")
    return {
        "number": number, "stem": stem, "data": data,
        "snapshotHash": verdict.get("snapshotHash"),
        "sourceRevision": verdict.get("sourceRevision"),
    }


def resume_transaction(req: dict, marker: dict, *, claimed: bool,
                       runtime_authority: dict,
                       prevalidated: Optional[dict] = None) -> dict:
    try:
        assert_dependency_graph_mutex_held()
        maybe_failpoint("after-marker")
        if marker.get("number") is None:
            existing = domain_dedup(req)
            if existing:
                verify_existing_effect(existing["stem"], existing["number"],
                                       expected_hash=existing["sourceHash"],
                                       expected_column=existing["column"],
                                       active_runtime=creation_active_runtime(marker, runtime_authority))
                marker.update({
                    "status": "completed", "phase": "completed", "effect": "domain-dedup",
                    "number": existing["number"], "stem": existing["stem"], "slug": None,
                    "sourceHash": existing["sourceHash"], "column": existing["column"],
                    "lastError": None, "intent": None,
                })
                write_marker(marker)
                maybe_failpoint("after-complete")
                return completed_response(marker, replay=not claimed)

        # Validate the complete corpus and last published INDEX before identity
        # allocation.  Recovery after the owned task effect intentionally skips
        # this pre-state: its stale INDEX is the effect this transaction must
        # finish publishing, not evidence for allocating another identity.
        if marker.get("number") is None:
            run_task_state_validation(check_index=True,
                                      code="TASK_STATE_CREATE_PRECONDITION_FAILED",
                                      active_runtime=creation_active_runtime(marker, runtime_authority),
                                      observation_action="create")

        slug = marker.get("slug") or slugify(req["title"])
        assert_dependency_graph_mutex_held()
        claim_number(marker, slug)
        maybe_failpoint("after-number")

        data = source_bytes(req, marker)
        if len(data) > TASK_MAX_BYTES:
            fail("TASK_TOO_LARGE", f"rendered task exceeds {TASK_MAX_BYTES} bytes", exit_code=2)
        if prevalidated is not None and (
                prevalidated.get("number") != marker.get("number") or
                prevalidated.get("stem") != marker.get("stem") or
                prevalidated.get("data") != data):
            fail("TASK_STATE_CREATE_PREFLIGHT_CHANGED",
                 "reserved candidate differs from the exact pre-marker validation",
                 recoverable=True)
        target_before = read_regular(task_path(marker["stem"]), max_bytes=TASK_MAX_BYTES,
                                     code="TASK_TARGET_CONFLICT", required=False)
        if target_before is None:
            run_task_state_validation(
                stem=marker["stem"], transition="absent:backlog", phase="pre",
                check_index=True, code="TASK_STATE_CREATE_PRECONDITION_FAILED",
                active_runtime=creation_active_runtime(marker, runtime_authority),
                observation_action="create")
            run_task_state_validation(
                stem=marker["stem"], check_index=False,
                code="TASK_STATE_CREATE_PROPOSAL_INVALID",
                proposal=data, proposal_state="backlog", proposal_from_state="absent",
                active_runtime=creation_active_runtime(marker, runtime_authority),
                observation_action="create")
        assert_dependency_graph_mutex_held()
        atomic_publish_task(marker, data)
        maybe_failpoint("after-file")

        # The filesystem shape must be valid before derived state is allowed to
        # advance.  A failure leaves the durable creation marker incomplete for
        # exact forward recovery and never reports success.
        run_task_state_validation(
            stem=marker["stem"], transition="absent:backlog", phase="post",
            check_index=False, code="TASK_STATE_CREATE_POSTCONDITION_FAILED",
            active_runtime=creation_active_runtime(marker, runtime_authority),
            observation_action="create")

        marker["phase"] = "regenerating-index"
        write_marker(marker)
        assert_dependency_graph_mutex_held()
        run_regen(False)
        assert_dependency_graph_mutex_held()
        marker["phase"] = "index-published"
        write_marker(marker)
        maybe_failpoint("after-index")

        marker["phase"] = "verifying"
        write_marker(marker)
        run_regen(True)
        run_task_state_validation(
            stem=marker["stem"], transition="absent:backlog", phase="post",
            check_index=True, code="TASK_STATE_CREATE_INDEX_POSTCONDITION_FAILED",
            active_runtime=creation_active_runtime(marker, runtime_authority),
            observation_action="create")
        assert_dependency_graph_mutex_held()
        entry = verify_created(req, marker, data)
        marker.update({
            "status": "completed", "phase": "completed", "effect": "created",
            "column": "backlog", "lastError": None, "intent": None,
        })
        write_marker(marker)
        assert_dependency_graph_mutex_held()
        maybe_failpoint("after-complete")
        response = completed_response(marker, replay=not claimed)
        response["task"] = entry
        # A recovered transaction did perform the original creation even if
        # this invocation only reconciled it; `replayed` communicates that
        # distinction without pretending a duplicate was created.
        response["created"] = claimed
        response["deduped"] = not claimed
        return response
    except CreateError as exc:
        if (exc.code not in {"DEPENDENCY_GRAPH_MUTEX_OWNERSHIP_LOST",
                            "DEPENDENCY_GRAPH_MUTEX_RELEASE_FAILED"} and
                marker.get("status") != "completed"):
            try:
                marker["status"] = "incomplete"
                marker["lastError"] = {"code": exc.code, "message": bounded(exc), "at": now()}
                write_marker(marker)
            except Exception:
                pass
        if marker.get("stem"):
            exc.details.setdefault("stem", marker["stem"])
            exc.details.setdefault("number", marker.get("number"))
            exc.details.setdefault("transactionId", marker.get("transactionId"))
        exc.recoverable = True
        raise


def execute_graph_locked(req: dict, key_hash: str,
                         runtime_authority: dict) -> dict:
    assert_dependency_graph_mutex_held()
    recover_cas_operations(
        CREATIONS_DIR,
        allowed_targets={marker_path(key_hash).name})
    assert_dependency_graph_mutex_held()
    # A normal invocation may resume its own exact crashed intent, but it must
    # not create a second active transaction beside a foreign recovery owner.
    markers = read_all_markers_for_recovery()
    completed_created = [
        item for item in markers
        if item.get("status") == "completed" and item.get("effect") == "created"
    ]
    if completed_created:
        assert_dependency_graph_mutex_held()
        reconcile_creation_staging(
            completed_created,
            only_transactions={item["transactionId"] for item in completed_created})
        refresh_completed_target_proofs(completed_created)
        assert_dependency_graph_mutex_held()
    foreign_incomplete = [
        item for item in markers
        if item["status"] == "incomplete" and item["keyHash"] != key_hash
    ]
    if foreign_incomplete:
        fail("CREATION_RECOVERY_REQUIRED",
             "recover incomplete deterministic creation before starting another request",
             recoverable=True,
             details={"keyHashes": [item["keyHash"] for item in foreign_incomplete[:20]]})
    own = next((item for item in markers if item["keyHash"] == key_hash), None)
    prevalidated = None if own is not None else prevalidate_fresh_creation(req)
    assert_dependency_graph_mutex_held()
    marker, claimed = claim_or_read_marker(req)
    if marker["status"] == "completed":
        assert_dependency_graph_mutex_held()
        reconcile_creation_staging(
            [marker], only_transactions={marker["transactionId"]})
        assert_dependency_graph_mutex_held()
        return completed_response(marker, replay=True)
    return resume_transaction(
        req, marker, claimed=claimed, runtime_authority=runtime_authority,
        prevalidated=prevalidated)


def execute(req: dict) -> dict:
    ensure_roots()
    parent = nested_parent_credentials()
    key_hash = sha256(req["key"].encode("ascii"))
    # Canonical order is publication authority -> local create/edit mutex ->
    # dependency-graph mutex.  The finalizer owns only the last lock, so no
    # reverse edge exists in the wait graph.
    with KernelMutex(MUTEX_PATH, configured_mutex_timeout()):
        with nested_publication_guard(
                parent, recovery=False, creation_key_hash=key_hash) as publication_guard:
            writer_state = writer_scan(
                recovery=False, parent=parent, publication_guard=publication_guard)
            runtime_authority = creation_runtime_authority(
                writer_state, recovery=False, parent=parent,
                publication_guard=publication_guard)
            invocation = "dependency-graph:create:" + secrets.token_hex(12)
            with DependencyGraphMutex(
                    FINALIZATIONS_DIR / ".mutex.json",
                    configured_dependency_graph_mutex_timeout(), invocation):
                return execute_graph_locked(req, key_hash, runtime_authority)


def read_all_markers_for_recovery() -> List[dict]:
    ensure_real_dir(CREATIONS_DIR, create=True)
    markers: List[dict] = []
    total_bytes = 0
    for name in bounded_directory_names(CREATIONS_DIR, code="RECOVERY_SCAN_FAILED", recoverable=True):
        if name == ".mutex" or name.startswith("."):
            continue
        if not MARKER_NAME_RE.fullmatch(name):
            if name.endswith(".json"):
                fail("RECOVERY_SCAN_FAILED", f"unsafe creation marker filename: {name}", recoverable=True)
            continue
        path = CREATIONS_DIR / name
        raw = read_regular(path, max_bytes=MARKER_MAX_BYTES, code="RECOVERY_SCAN_FAILED")
        total_bytes += len(raw)
        if total_bytes > MAX_MARKER_CORPUS_BYTES:
            fail("RECOVERY_SCAN_FAILED", "creation marker corpus exceeds its total byte bound",
                 recoverable=True)
        try:
            parsed = json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            fail("RECOVERY_SCAN_FAILED", f"creation marker {name} is malformed: {bounded(exc)}", recoverable=True)
        if not isinstance(parsed, dict):
            fail("RECOVERY_SCAN_FAILED", f"creation marker {name} must contain an object", recoverable=True)
        try:
            marker = validate_marker(
                parsed,
                expected_key_hash="sha256:" + name[:-len(".json")],
                expected_payload_hash=str(parsed.get("payloadHash", "")),
            )
        except CreateError as exc:
            fail("RECOVERY_SCAN_FAILED", f"creation marker {name} is unsafe: {bounded(exc)}", recoverable=True)
        markers.append(marker)
    transactions = [marker["transactionId"] for marker in markers]
    if len(set(transactions)) != len(transactions):
        fail("RECOVERY_SCAN_FAILED", "creation marker transaction ids are not unique",
             recoverable=True)
    # Hidden stage files are part of the recovery corpus.  Validate their
    # exact marker/content binding even in a read-only scan so malformed or
    # orphaned names can never disappear behind the dotfile filter.
    scan_creation_staging(markers)
    return markers


def recover_all_graph_locked(runtime_authority: dict) -> dict:
    assert_dependency_graph_mutex_held()
    recover_cas_operations(CREATIONS_DIR)
    assert_dependency_graph_mutex_held()
    # Validate the complete marker set before mutating any transaction. A
    # corrupt receipt therefore blocks the complete recovery pass.
    markers = read_all_markers_for_recovery()
    completed_at_start = [
        marker for marker in markers
        if marker.get("status") == "completed" and marker.get("effect") == "created"
    ]
    if completed_at_start:
        assert_dependency_graph_mutex_held()
        reconcile_creation_staging(
            completed_at_start,
            only_transactions={marker["transactionId"] for marker in completed_at_start})
        refresh_completed_target_proofs(completed_at_start)
        assert_dependency_graph_mutex_held()
    incomplete = [m for m in markers if m["status"] == "incomplete"]
    recovered: List[dict] = []
    for marker in incomplete:
        req = validate_recovery_intent(marker.get("intent"), marker["payloadHash"])
        try:
            result = resume_transaction(
                req, marker, claimed=False, runtime_authority=runtime_authority)
        except CreateError as exc:
            exc.details.setdefault("mode", "recover-all")
            exc.details.setdefault("recovered", recovered)
            exc.details.setdefault("failedKeyHash", marker["keyHash"])
            raise
        recovered.append({
            "stem": result.get("stem"),
            "number": result.get("number"),
            "effect": result.get("effect"),
            "transactionId": result.get("transactionId"),
        })
    completed_created = [
        marker for marker in markers
        if marker.get("status") == "completed" and marker.get("effect") == "created"
    ]
    if completed_created:
        assert_dependency_graph_mutex_held()
        reconcile_creation_staging(
            completed_created,
            only_transactions={marker["transactionId"] for marker in completed_created})
        refresh_completed_target_proofs(completed_created)
        assert_dependency_graph_mutex_held()
    return {
        "ok": True,
        "mode": "recover-all",
        "scanned": len(markers),
        "alreadyCompleted": len(markers) - len(incomplete),
        "recoveredCount": len(recovered),
        "recovered": recovered,
    }


def recover_all() -> dict:
    ensure_roots()
    parent = nested_parent_credentials()
    with KernelMutex(MUTEX_PATH, configured_mutex_timeout()):
        with nested_publication_guard(
                parent, recovery=True, creation_key_hash=None) as publication_guard:
            writer_state = writer_scan(
                recovery=True, parent=parent, publication_guard=publication_guard)
            writer_state = reconcile_stale_nested_publication_guards(
                writer_state, parent=parent, publication_guard=publication_guard)
            runtime_authority = creation_runtime_authority(
                writer_state, recovery=True, parent=parent,
                publication_guard=publication_guard)
            invocation = "dependency-graph:create-recovery:" + secrets.token_hex(12)
            with DependencyGraphMutex(
                    FINALIZATIONS_DIR / ".mutex.json",
                    configured_dependency_graph_mutex_timeout(), invocation):
                return recover_all_graph_locked(runtime_authority)


def read_request() -> dict:
    raw = sys.stdin.buffer.read(MAX_REQUEST_BYTES + 1)
    if len(raw) > MAX_REQUEST_BYTES:
        fail("REQUEST_TOO_LARGE", f"stdin JSON exceeds {MAX_REQUEST_BYTES} bytes", exit_code=2)
    if not raw.strip():
        fail("INVALID_JSON", "stdin is empty", exit_code=2)
    try:
        value = json.loads(raw.decode("utf-8"))
    except UnicodeDecodeError:
        fail("INVALID_UTF8", "stdin is not valid UTF-8", exit_code=2)
    except json.JSONDecodeError as exc:
        fail("INVALID_JSON", f"stdin is not valid JSON: {bounded(exc)}", exit_code=2)
    return validate_request(value)


def await_recovery_readiness() -> None:
    """Fence recovery behind the controller's durable child-lease binding.

    EOF is the only valid control message.  Reading one byte is deliberately
    bounded: an open pipe blocks, a closed empty pipe authorizes recovery, and
    any byte fails before mutex acquisition, writer scanning, or mutation.
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


def require_supported_task_platform() -> None:
    if sys.platform not in ("linux", "darwin"):
        fail(
            "PLATFORM_UNSUPPORTED",
            f"canonical task lifecycle is unsupported on native {sys.platform}; use Linux, macOS, or WSL",
            exit_code=3,
        )


def main() -> int:
    sys.stdout.write("READY\n")
    sys.stdout.flush()
    try:
        require_supported_task_platform()
        args = sys.argv[1:]
        if args == ["--recover-all"]:
            await_recovery_readiness()
            emit(recover_all())
        elif not args:
            req = read_request()
            emit(execute(req))
        else:
            fail("USAGE", "usage: create-backlog.py [--recover-all]", exit_code=2)
        return 0
    except CreateError as exc:
        payload = {
            "ok": False,
            "error": {"code": exc.code, "message": bounded(exc)},
            "recoverable": bool(exc.recoverable),
        }
        payload.update(exc.details)
        emit(payload)
        return exc.exit_code
    except BaseException as exc:
        emit({
            "ok": False,
            "error": {"code": "CREATE_INTERNAL_ERROR", "message": bounded(f"{type(exc).__name__}: {exc}")},
            "recoverable": False,
        })
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
