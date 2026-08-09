#!/usr/bin/env python3
"""Kernel finalization mutex and anchored task-filesystem boundary.

The default mode is the long-lived flock protocol used by
``finalize-task.mjs``. ``fs-op`` is a short-lived, policy-free openat worker
for Node callers: every path component is opened without following symlinks,
directory descriptors stay pinned for the whole operation, and every proof
uses lossless decimal dev/ino/nanosecond strings.

Node does not expose openat/unlinkat/renameat with directory descriptors.  A
separate worker is therefore the smallest portable boundary that prevents an
ancestor swap (including swap-away/swap-back) from redirecting a scan or a
mutation after admission.
"""

from __future__ import annotations

import base64
import errno
import hashlib
import json
import os
import re
import socket
import stat
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

if os.name == "nt":
    import msvcrt
else:
    import fcntl


MAX_REQUEST = 40 * 1024 * 1024
MAX_PATH_PARTS = 128
MAX_RESULT_BYTES = 32 * 1024 * 1024
DECIMAL_FIELDS = ("dev", "ino", "mtimeNs", "ctimeNs")


class BoundaryError(Exception):
    def __init__(self, code: str, message: str):
        super().__init__(str(message)[:1200])
        self.code = code


def boundary_fail(code: str, message: str) -> None:
    raise BoundaryError(code, message)


def acquire_lock(fh):
    if os.name == "nt":
        fh.seek(0, os.SEEK_END)
        if fh.tell() == 0:
            fh.write("\n")
            fh.flush()
        fh.seek(0)
        while True:
            try:
                msvcrt.locking(fh.fileno(), msvcrt.LK_NBLCK, 1)
                break
            except OSError as exc:
                if exc.errno not in (errno.EACCES, errno.EAGAIN, errno.EDEADLK):
                    raise
                time.sleep(0.05)
    else:
        fcntl.flock(fh.fileno(), fcntl.LOCK_EX)


def release_lock(fh):
    if os.name == "nt":
        fh.seek(0)
        msvcrt.locking(fh.fileno(), msvcrt.LK_UNLCK, 1)
    else:
        fcntl.flock(fh.fileno(), fcntl.LOCK_UN)


def process_start_id(pid: int) -> Optional[str]:
    """Return the same stable process-generation vocabulary as writer leases."""
    if sys.platform.startswith("linux"):
        try:
            raw = Path(f"/proc/{pid}/stat").read_text(encoding="ascii")
            close = raw.rfind(")")
            fields = raw[close + 2:].split()
            # /proc stat field 22; fields starts at field 3.
            ticks = fields[19]
            boot_id = Path("/proc/sys/kernel/random/boot_id").read_text(encoding="ascii").strip().lower()
            if ticks.isdigit() and boot_id:
                payload = "\0".join(("writer-process-v1", "linux", boot_id, str(pid), ticks))
                return "psid-v1:linux:" + hashlib.sha256(payload.encode("utf-8")).hexdigest()
        except (OSError, IndexError, UnicodeError):
            return None
    if sys.platform == "darwin":
        try:
            import ctypes
            import subprocess

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
            count = proc_pidinfo(pid, 3, 0, ctypes.byref(info), ctypes.sizeof(info))
            boot_id = subprocess.check_output(
                ["/usr/sbin/sysctl", "-n", "kern.bootsessionuuid"],
                stderr=subprocess.DEVNULL, text=True, timeout=2,
            ).strip().lower()
            if count == ctypes.sizeof(info) == 136 and info.pid == pid and info.sec > 0 and info.usec < 1000000 and boot_id:
                payload = "\0".join(("writer-process-v1", "darwin", boot_id, str(pid),
                                      str(info.sec), str(info.usec)))
                return "psid-v1:darwin:" + hashlib.sha256(payload.encode("utf-8")).hexdigest()
        except (OSError, subprocess.SubprocessError, AttributeError):
            return None
    if sys.platform == "win32":
        try:
            helper = Path(__file__).resolve().with_name("windows-runtime-proof.py")
            env = {key: os.environ[key] for key in
                   ("SystemRoot", "WINDIR", "PATH", "PATHEXT", "TEMP", "TMP")
                   if key in os.environ}
            env.update({"PYTHONIOENCODING": "utf-8:strict", "PYTHONUTF8": "1"})
            result = subprocess.run(
                [sys.executable, "-I", "-B", str(helper), "process", str(pid)],
                cwd=os.environ.get("TEMP") or os.environ.get("TMP") or str(helper.parent),
                env=env, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                stderr=subprocess.PIPE, timeout=5, check=False,
            )
            if result.returncode != 0 or len(result.stdout) > 16 * 1024:
                return None
            value = json.loads(result.stdout.decode("utf-8"))
            canonical_bytes = (json.dumps(value, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")
            identity_value = value.get("processStartId") if isinstance(value, dict) else None
            if (result.stdout == canonical_bytes and set(value) == {
                    "pid", "processStartId", "reason", "status", "version"} and
                    value.get("version") == 1 and value.get("pid") == pid and
                    value.get("status") == "live" and value.get("reason") == "ok" and
                    isinstance(identity_value, str) and
                    re.fullmatch(r"psid-v1:win32:[a-f0-9]{64}", identity_value)):
                return identity_value
        except (OSError, ValueError, UnicodeError, json.JSONDecodeError, subprocess.SubprocessError):
            return None
    return None


def stamp(invocation_id: str, started_at: str, start_id: Optional[str], released=False):
    return {
        "version": 1,
        "pid": os.getpid(),
        "processStartId": start_id,
        "hostname": socket.gethostname(),
        "invocationId": invocation_id,
        "startedAt": started_at,
        "released": released,
    }


def write_record(fh, record):
    fh.seek(0)
    fh.truncate(0)
    fh.write(json.dumps(record, separators=(",", ":")) + "\n")
    fh.flush()
    os.fsync(fh.fileno())


def mutex_main(argv: List[str]) -> int:
    if len(argv) < 3:
        print("usage: finalize-lock.py <lock-file> <invocation-id> <authority-root>", file=sys.stderr)
        return 2
    lock_path = argv[0]
    invocation_id = argv[1]
    authority_root = argv[2]
    chain: List[Dict[str, Any]] = []
    try:
        chain, fd = open_anchored_mutex(lock_path, authority_root)
    except BoundaryError as exc:
        print(f"{exc.code}: {exc}", file=sys.stderr)
        return 2
    started_at = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    start_id = process_start_id(os.getpid())
    if (sys.platform.startswith("linux") or sys.platform in ("darwin", "win32")) and not start_id:
        os.close(fd)
        close_chain(chain)
        print("exact mutex process identity is unavailable", file=sys.stderr)
        return 2
    try:
        with os.fdopen(fd, "r+", encoding="utf-8") as fh:
            acquire_lock(fh)
            claimed = False
            try:
                verify_anchored_mutex_path(chain, fh.fileno(), lock_path, authority_root)
                write_record(fh, stamp(invocation_id, started_at, start_id, False))
                claimed = True
                # A waiter may have held this fd while the previous owner ran.
                # Re-prove the canonical name after acquiring flock and
                # publishing our record, before admitting Node work.
                verify_anchored_mutex_path(chain, fh.fileno(), lock_path, authority_root)
                print("LOCKED", flush=True)
                sys.stdin.buffer.read()
            except BoundaryError as exc:
                print(f"{exc.code}: {exc}", file=sys.stderr)
                return 2
            finally:
                if claimed:
                    write_record(fh, stamp(invocation_id, started_at, start_id, True))
                release_lock(fh)
    finally:
        # Keep every runtime ancestor pinned for the complete kernel-lock lease.
        close_chain(chain)
    return 0


# ---- Anchored filesystem worker -------------------------------------------------

def identity(st: os.stat_result) -> Tuple[int, int, int, int, int, int]:
    return (st.st_dev, st.st_ino, st.st_mode, st.st_size,
            st.st_mtime_ns, st.st_ctime_ns)


def generation(st: os.stat_result) -> Tuple[int, int]:
    return (st.st_dev, st.st_ino)


def directory_generation(st: os.stat_result) -> Tuple[int, int, int]:
    return (st.st_dev, st.st_ino, st.st_mode)


def kind(st: os.stat_result) -> str:
    if stat.S_ISREG(st.st_mode):
        return "file"
    if stat.S_ISDIR(st.st_mode):
        return "directory"
    if stat.S_ISLNK(st.st_mode):
        return "symlink"
    return "other"


def stat_proof(st: os.stat_result, digest: Optional[str] = None) -> Dict[str, Any]:
    value: Dict[str, Any] = {
        "dev": str(st.st_dev),
        "ino": str(st.st_ino),
        "mode": st.st_mode,
        "size": st.st_size,
        "mtimeNs": str(st.st_mtime_ns),
        "ctimeNs": str(st.st_ctime_ns),
        "kind": kind(st),
    }
    if digest is not None:
        value["hash"] = "sha256:" + digest
    return value


def directory_flags() -> int:
    return os.O_RDONLY | getattr(os, "O_DIRECTORY", 0) | getattr(os, "O_NOFOLLOW", 0)


def absolute_path(value: Any, label: str) -> Path:
    if not isinstance(value, str) or not value or "\x00" in value or not os.path.isabs(value):
        boundary_fail("ARGUMENT_INVALID", f"{label} must be an absolute path")
    return Path(os.path.abspath(value))


def under(root: Path, target: Path, label: str) -> Tuple[str, ...]:
    try:
        parts = target.relative_to(root).parts
    except ValueError:
        boundary_fail("PATH_OUTSIDE_AUTHORITY", f"{label} escapes its authority root")
    if len(parts) > MAX_PATH_PARTS or any(part in ("", ".", "..") for part in parts):
        boundary_fail("PATH_UNSAFE", f"{label} has unsafe path components")
    return parts


def close_chain(chain: List[Dict[str, Any]]) -> None:
    for item in reversed(chain):
        try:
            os.close(item["fd"])
        except OSError:
            pass


def verify_chain(chain: List[Dict[str, Any]]) -> None:
    for index, item in enumerate(chain):
        opened = os.fstat(item["fd"])
        if not stat.S_ISDIR(opened.st_mode) or directory_generation(opened) != directory_generation(item["opened"]):
            boundary_fail("DIRECTORY_CHANGED", "a pinned directory generation changed")
        try:
            if index == 0:
                live = item["path"].lstat()
            else:
                live = os.stat(item["name"], dir_fd=chain[index - 1]["fd"], follow_symlinks=False)
        except FileNotFoundError:
            boundary_fail("DIRECTORY_CHANGED", "a pinned directory component disappeared")
        if stat.S_ISLNK(live.st_mode) or not stat.S_ISDIR(live.st_mode) or directory_generation(live) != directory_generation(opened):
            boundary_fail("DIRECTORY_CHANGED", "a path component no longer resolves to its pinned generation")


def verify_target(chain: List[Dict[str, Any]], target: Path) -> None:
    verify_chain(chain)
    try:
        live = target.lstat()
    except FileNotFoundError:
        boundary_fail("DIRECTORY_CHANGED", "the canonical directory path disappeared")
    opened = os.fstat(chain[-1]["fd"])
    if stat.S_ISLNK(live.st_mode) or not stat.S_ISDIR(live.st_mode) or directory_generation(live) != directory_generation(opened):
        boundary_fail("DIRECTORY_CHANGED", "the canonical path no longer resolves to the pinned directory")
    verify_chain(chain)


def open_dir(target: Path, authority: Path, create: bool = False) -> Tuple[List[Dict[str, Any]], int]:
    parts = under(authority, target, "directory")
    chain: List[Dict[str, Any]] = []
    try:
        before = authority.lstat()
        if stat.S_ISLNK(before.st_mode) or not stat.S_ISDIR(before.st_mode):
            boundary_fail("DIRECTORY_UNSAFE", "authority root is not a real directory")
        root_fd = os.open(authority, directory_flags())
        opened = os.fstat(root_fd)
        # Directory contents may change legitimately while concurrent mutex
        # contenders open the same authority. Bind the stable directory
        # generation, not size/mtime/ctime metadata changed by those entries.
        if directory_generation(before) != directory_generation(opened):
            os.close(root_fd)
            boundary_fail("DIRECTORY_CHANGED", "authority root changed while opening")
        chain.append({"fd": root_fd, "opened": opened, "path": authority, "name": None})
        current = authority
        for part in parts:
            parent_fd = chain[-1]["fd"]
            current = current / part
            try:
                child_before = os.stat(part, dir_fd=parent_fd, follow_symlinks=False)
            except FileNotFoundError:
                if not create:
                    boundary_fail("PATH_MISSING", "directory path is missing")
                try:
                    os.mkdir(part, 0o700, dir_fd=parent_fd)
                    os.fsync(parent_fd)
                except FileExistsError:
                    pass
                child_before = os.stat(part, dir_fd=parent_fd, follow_symlinks=False)
            if stat.S_ISLNK(child_before.st_mode) or not stat.S_ISDIR(child_before.st_mode):
                boundary_fail("DIRECTORY_UNSAFE", "directory component is not a real directory")
            child_fd = os.open(part, directory_flags(), dir_fd=parent_fd)
            child_opened = os.fstat(child_fd)
            if directory_generation(child_before) != directory_generation(child_opened):
                os.close(child_fd)
                boundary_fail("DIRECTORY_CHANGED", "directory component changed while opening")
            chain.append({"fd": child_fd, "opened": child_opened, "path": current, "name": part})
        verify_target(chain, target)
        return chain, chain[-1]["fd"]
    except BaseException:
        close_chain(chain)
        raise


def open_anchored_mutex(lock_path_raw: str, authority_raw: str) -> Tuple[List[Dict[str, Any]], int]:
    """Open the stable mutex inode through a project-root-anchored parent."""
    authority = absolute_path(authority_raw, "authority root")
    target = absolute_path(lock_path_raw, "lock path")
    under(authority, target, "lock path")
    chain, parent_fd = open_dir(target.parent, authority, create=True)
    fd: Optional[int] = None
    try:
        try:
            before = os.stat(target.name, dir_fd=parent_fd, follow_symlinks=False)
            if stat.S_ISLNK(before.st_mode) or not stat.S_ISREG(before.st_mode):
                boundary_fail("ENTRY_UNSAFE", "mutex path is not a regular file")
        except FileNotFoundError:
            before = None
        open_flags = os.O_RDWR | getattr(os, "O_NOFOLLOW", 0)
        creation_race = before is None
        if before is None:
            # On Darwin, two concurrent openat(O_CREAT|O_NOFOLLOW) calls for
            # the same absent final component can make the loser receive
            # ENOENT.  O_EXCL gives the creation race one unambiguous winner;
            # the loser then opens that winner's proven canonical inode
            # without O_CREAT.  We never retry through a new parent or create
            # after observing an existing generation disappear.
            try:
                fd = os.open(target.name, open_flags | os.O_CREAT | os.O_EXCL,
                             0o600, dir_fd=parent_fd)
            except FileExistsError:
                try:
                    before = os.stat(target.name, dir_fd=parent_fd, follow_symlinks=False)
                except FileNotFoundError:
                    boundary_fail("ENTRY_CHANGED", "concurrent mutex creator disappeared before open")
                if stat.S_ISLNK(before.st_mode) or not stat.S_ISREG(before.st_mode):
                    boundary_fail("ENTRY_UNSAFE", "mutex path is not a regular file")
                try:
                    fd = os.open(target.name, open_flags, dir_fd=parent_fd)
                except FileNotFoundError:
                    boundary_fail("ENTRY_CHANGED", "concurrent mutex generation disappeared before open")
        else:
            try:
                fd = os.open(target.name, open_flags, dir_fd=parent_fd)
            except FileNotFoundError:
                boundary_fail("ENTRY_CHANGED", "existing mutex generation disappeared before open")
        opened = os.fstat(fd)
        try:
            current = os.stat(target.name, dir_fd=parent_fd, follow_symlinks=False)
        except FileNotFoundError:
            boundary_fail("ENTRY_CHANGED", "mutex generation disappeared while it was being opened")
        if (not stat.S_ISREG(opened.st_mode) or not stat.S_ISREG(current.st_mode) or
                generation(opened) != generation(current) or
                (before is not None and generation(before) != generation(opened))):
            boundary_fail("ENTRY_CHANGED", "mutex inode changed while it was being opened")
        if creation_race:
            os.fsync(parent_fd)
        verify_target(chain, target.parent)
        return chain, fd
    except BaseException:
        if fd is not None:
            os.close(fd)
        close_chain(chain)
        raise


def verify_anchored_mutex_path(chain: List[Dict[str, Any]], fd: int,
                               lock_path_raw: str, authority_raw: str) -> None:
    """Fence a waited-on flock fd back to its canonical anchored name."""
    authority = absolute_path(authority_raw, "authority root")
    target = absolute_path(lock_path_raw, "lock path")
    under(authority, target, "lock path")
    verify_target(chain, target.parent)
    opened = os.fstat(fd)
    try:
        current = os.stat(target.name, dir_fd=chain[-1]["fd"], follow_symlinks=False)
    except FileNotFoundError:
        boundary_fail("ENTRY_CHANGED", "canonical mutex name disappeared while waiting for flock")
    if (not stat.S_ISREG(opened.st_mode) or not stat.S_ISREG(current.st_mode) or
            generation(opened) != generation(current)):
        boundary_fail("ENTRY_CHANGED", "canonical mutex generation changed while waiting for flock")
    verify_target(chain, target.parent)


def canonical_decimal(value: Any, signed: bool = False) -> Optional[int]:
    if not isinstance(value, str) or not value:
        return None
    if value == "-0" or (value[0] == "-" and not signed):
        return None
    digits = value[1:] if value.startswith("-") else value
    if not digits.isdigit() or (len(digits) > 1 and digits.startswith("0")):
        return None
    try:
        parsed = int(value)
    except ValueError:
        return None
    if not signed and parsed < 0:
        return None
    return parsed


def parse_expected(value: Any, label: str) -> Dict[str, Any]:
    if not isinstance(value, dict):
        boundary_fail("ARGUMENT_INVALID", f"{label} proof is missing")
    for field in ("dev", "ino"):
        if canonical_decimal(value.get(field), False) is None:
            boundary_fail("ARGUMENT_INVALID", f"{label} proof has invalid {field}")
    for field in ("mtimeNs", "ctimeNs"):
        if canonical_decimal(value.get(field), True) is None:
            boundary_fail("ARGUMENT_INVALID", f"{label} proof has invalid {field}")
    for field in ("mode", "size"):
        item = value.get(field)
        if isinstance(item, bool) or not isinstance(item, int) or item < 0 or item > 2 ** 53 - 1:
            boundary_fail("ARGUMENT_INVALID", f"{label} proof has invalid {field}")
    if value.get("kind") not in ("file", "directory", "symlink", "other"):
        boundary_fail("ARGUMENT_INVALID", f"{label} proof has invalid kind")
    digest = value.get("hash")
    if digest is not None and (not isinstance(digest, str) or
                               len(digest) != 71 or not digest.startswith("sha256:") or
                               any(c not in "0123456789abcdef" for c in digest[7:])):
        boundary_fail("ARGUMENT_INVALID", f"{label} proof has invalid hash")
    return value


def proof_matches(st: os.stat_result, expected: Dict[str, Any], data: Optional[bytes] = None) -> bool:
    if (str(st.st_dev) != expected["dev"] or str(st.st_ino) != expected["ino"] or
            st.st_mode != expected["mode"] or st.st_size != expected["size"] or
            str(st.st_mtime_ns) != expected["mtimeNs"] or str(st.st_ctime_ns) != expected["ctimeNs"] or
            kind(st) != expected["kind"]):
        return False
    if expected.get("hash") is not None:
        if data is None:
            return False
        return "sha256:" + hashlib.sha256(data).hexdigest() == expected["hash"]
    return True


def moved_proof_matches(st: os.stat_result, expected: Dict[str, Any],
                        data: Optional[bytes]) -> bool:
    """A rename/link-count change may legitimately advance ctime only."""
    if (str(st.st_dev) != expected["dev"] or str(st.st_ino) != expected["ino"] or
            st.st_mode != expected["mode"] or st.st_size != expected["size"] or
            str(st.st_mtime_ns) != expected["mtimeNs"] or kind(st) != expected["kind"]):
        return False
    return expected.get("hash") is None or "sha256:" + hashlib.sha256(data).hexdigest() == expected["hash"]


def decode_bytes(request: Dict[str, Any]) -> bytes:
    raw = request.get("rawBase64")
    if not isinstance(raw, str):
        boundary_fail("ARGUMENT_INVALID", "rawBase64 is missing")
    try:
        data = base64.b64decode(raw.encode("ascii"), validate=True)
    except (ValueError, UnicodeError):
        boundary_fail("ARGUMENT_INVALID", "rawBase64 is invalid")
    if len(data) > MAX_RESULT_BYTES or base64.b64encode(data).decode("ascii") != raw:
        boundary_fail("ARGUMENT_INVALID", "rawBase64 exceeds its bound or is non-canonical")
    return data


def read_fd(fd: int, maximum: int) -> bytes:
    chunks: List[bytes] = []
    total = 0
    while True:
        chunk = os.read(fd, min(65536, maximum + 1 - total))
        if not chunk:
            return b"".join(chunks)
        chunks.append(chunk)
        total += len(chunk)
        if total > maximum:
            boundary_fail("ENTRY_TOO_LARGE", "file grew beyond its byte bound")


def read_at(parent_fd: int, name: str, maximum: int) -> Tuple[bytes, os.stat_result]:
    try:
        before = os.stat(name, dir_fd=parent_fd, follow_symlinks=False)
    except FileNotFoundError:
        boundary_fail("PATH_MISSING", "file path is missing")
    if stat.S_ISLNK(before.st_mode) or not stat.S_ISREG(before.st_mode):
        boundary_fail("ENTRY_UNSAFE", "entry is not a regular file")
    if before.st_size > maximum:
        boundary_fail("ENTRY_TOO_LARGE", "file exceeds its byte bound")
    fd = os.open(name, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0), dir_fd=parent_fd)
    try:
        opened = os.fstat(fd)
        if identity(opened) != identity(before) or not stat.S_ISREG(opened.st_mode):
            boundary_fail("ENTRY_CHANGED", "file changed while opening")
        data = read_fd(fd, maximum)
        after_fd = os.fstat(fd)
        after_name = os.stat(name, dir_fd=parent_fd, follow_symlinks=False)
        if identity(after_fd) != identity(opened) or identity(after_name) != identity(opened):
            boundary_fail("ENTRY_CHANGED", "file changed while reading")
        return data, opened
    finally:
        os.close(fd)


def fixture_hook(request: Dict[str, Any], stage: str, target: Path,
                 parent_fd: Optional[int] = None, name: Optional[str] = None) -> None:
    """One-shot adversarial replacement hook, inert outside isolated fixtures."""
    wanted_stage = os.environ.get("FINALIZE_FS_TEST_STAGE", "")
    wanted_target = os.environ.get("FINALIZE_FS_TEST_TARGET", "")
    replacement_raw = os.environ.get("FINALIZE_FS_TEST_REPLACEMENT", "")
    fixture_root_raw = os.environ.get("FINALIZE_FS_TEST_ROOT", "")
    sentinel_raw = os.environ.get("FINALIZE_FS_TEST_SENTINEL", "")
    # The fixture root is shared by independent failpoint families.  It must
    # not activate this hook by itself.
    if not any((wanted_stage, wanted_target, replacement_raw, sentinel_raw)):
        return
    if not all((wanted_stage, wanted_target, replacement_raw, fixture_root_raw, sentinel_raw)):
        boundary_fail("TEST_HOOK_INVALID", "filesystem test hook is incomplete")
    if stage != wanted_stage or target != Path(os.path.abspath(wanted_target)):
        return
    fixture_root = Path(os.path.abspath(fixture_root_raw))
    replacement = Path(os.path.abspath(replacement_raw))
    sentinel = Path(os.path.abspath(sentinel_raw))
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    canonical = Path(os.path.abspath(request.get("canonicalRoot", "")))
    if not request.get("fixture") or authority == canonical:
        boundary_fail("TEST_HOOK_INVALID", "filesystem hook is disabled outside isolated fixtures")
    under(fixture_root, target, "test target")
    under(fixture_root, replacement, "test replacement")
    under(fixture_root, sentinel, "test sentinel")
    try:
        token = os.open(sentinel, os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0), 0o600)
        os.close(token)
    except FileExistsError:
        return
    displaced = target.parent / (target.name + ".test-displaced")
    try:
        displaced.lstat()
        boundary_fail("TEST_HOOK_INVALID", "test displaced path already exists")
    except FileNotFoundError:
        pass
    if parent_fd is not None and name is not None:
        os.rename(name, displaced.name, src_dir_fd=parent_fd, dst_dir_fd=parent_fd)
        os.rename(replacement, target)
    else:
        os.rename(target, displaced)
        os.rename(replacement, target)


def fixture_directory_swap(request: Dict[str, Any], target: Path, opened_fd: int) -> Optional[Dict[str, Path]]:
    swap_target_raw = os.environ.get("TASK_FS_TEST_SWAP_PATH", "")
    swap_with_raw = os.environ.get("TASK_FS_TEST_SWAP_WITH", "")
    fixture_root_raw = os.environ.get("TASK_FS_TEST_ROOT", "")
    if not any((swap_target_raw, swap_with_raw, fixture_root_raw)):
        return None
    if not all((swap_target_raw, swap_with_raw, fixture_root_raw)):
        boundary_fail("TEST_HOOK_INVALID", "directory swap hook is incomplete")
    swap_target = Path(os.path.abspath(swap_target_raw))
    if target != swap_target:
        return None
    swap_with = Path(os.path.abspath(swap_with_raw))
    fixture_root = Path(os.path.abspath(fixture_root_raw))
    canonical = Path(os.path.abspath(request.get("canonicalRoot", "")))
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    if not request.get("fixture") or authority == canonical:
        boundary_fail("TEST_HOOK_INVALID", "directory swap hook is disabled outside isolated fixtures")
    under(fixture_root, swap_target, "swap target")
    under(fixture_root, swap_with, "swap replacement")
    if generation(os.fstat(opened_fd)) != generation(swap_target.lstat()):
        boundary_fail("TEST_HOOK_INVALID", "swap target is not the pinned directory")
    replacement = swap_with.lstat()
    if stat.S_ISLNK(replacement.st_mode) or not stat.S_ISDIR(replacement.st_mode):
        boundary_fail("TEST_HOOK_INVALID", "swap replacement is not a real directory")
    backup = target.parent / (target.name + ".fs-boundary-original")
    try:
        backup.lstat()
        boundary_fail("TEST_HOOK_INVALID", "swap backup already exists")
    except FileNotFoundError:
        pass
    os.rename(target, backup)
    try:
        os.rename(swap_with, target)
    except BaseException:
        os.rename(backup, target)
        raise
    return {"target": target, "replacement": swap_with, "backup": backup}


def restore_directory_swap(value: Optional[Dict[str, Path]]) -> None:
    if value is None:
        return
    os.rename(value["target"], value["replacement"])
    os.rename(value["backup"], value["target"])


def list_action(request: Dict[str, Any]) -> Dict[str, Any]:
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    target = absolute_path(request.get("path"), "path")
    maximum = request.get("maxEntries")
    if isinstance(maximum, bool) or not isinstance(maximum, int) or maximum < 1 or maximum > 100_000:
        boundary_fail("ARGUMENT_INVALID", "maxEntries is outside its bound")
    try:
        chain, fd = open_dir(target, authority)
    except BoundaryError as exc:
        if exc.code == "PATH_MISSING" and request.get("allowMissing") is True:
            return {"missing": True, "names": [], "entries": {}}
        raise
    swap: Optional[Dict[str, Path]] = None
    try:
        opened = os.fstat(fd)
        swap = fixture_directory_swap(request, target, fd)
        names: List[str] = []
        with os.scandir(fd) as iterator:
            for entry in iterator:
                names.append(entry.name)
                if len(names) > maximum:
                    boundary_fail("DIRECTORY_TOO_LARGE", "directory exceeds its bounded entry limit")
        names.sort()
        entries: Dict[str, Any] = {}
        for entry_name in names:
            try:
                entries[entry_name] = stat_proof(os.stat(entry_name, dir_fd=fd, follow_symlinks=False))
            except FileNotFoundError:
                boundary_fail("ENTRY_CHANGED", f"directory entry disappeared: {entry_name}")
        after = os.fstat(fd)
        if identity(after) != identity(opened):
            boundary_fail("DIRECTORY_CHANGED", "directory changed while enumerating")
        if os.environ.get("TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY") == "1":
            restore_directory_swap(swap)
            swap = None
        verify_target(chain, target)
        return {"missing": False, "stat": stat_proof(after), "names": names, "entries": entries}
    finally:
        try:
            restore_directory_swap(swap)
        finally:
            close_chain(chain)


def stat_action(request: Dict[str, Any]) -> Dict[str, Any]:
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    target = absolute_path(request.get("path"), "path")
    under(authority, target, "path")
    try:
        chain, parent_fd = open_dir(target.parent, authority)
    except BoundaryError as exc:
        if exc.code == "PATH_MISSING" and request.get("allowMissing") is True:
            return {"missing": True}
        raise
    try:
        try:
            current = os.stat(target.name, dir_fd=parent_fd, follow_symlinks=False)
        except FileNotFoundError:
            if request.get("allowMissing") is True:
                verify_target(chain, target.parent)
                return {"missing": True}
            boundary_fail("PATH_MISSING", "entry is missing")
        verify_target(chain, target.parent)
        return {"missing": False, "stat": stat_proof(current)}
    finally:
        close_chain(chain)


def read_action(request: Dict[str, Any]) -> Dict[str, Any]:
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    target = absolute_path(request.get("path"), "path")
    under(authority, target, "path")
    maximum = request.get("maxBytes")
    if isinstance(maximum, bool) or not isinstance(maximum, int) or maximum < 1 or maximum > MAX_RESULT_BYTES:
        boundary_fail("ARGUMENT_INVALID", "maxBytes is outside its bound")
    try:
        chain, parent_fd = open_dir(target.parent, authority)
    except BoundaryError as exc:
        if exc.code == "PATH_MISSING" and request.get("allowMissing") is True:
            return {"missing": True}
        raise
    try:
        try:
            data, opened = read_at(parent_fd, target.name, maximum)
        except BoundaryError as exc:
            if exc.code == "PATH_MISSING" and request.get("allowMissing") is True:
                verify_target(chain, target.parent)
                return {"missing": True}
            raise
        expected = request.get("expected")
        if expected is not None and not proof_matches(opened, parse_expected(expected, "file"), data):
            boundary_fail("ENTRY_CHANGED", "file differs from its frozen exact proof")
        verify_target(chain, target.parent)
        digest = hashlib.sha256(data).hexdigest()
        return {"missing": False, "stat": stat_proof(opened, digest),
                "rawBase64": base64.b64encode(data).decode("ascii")}
    finally:
        close_chain(chain)


def ensure_dir_action(request: Dict[str, Any]) -> Dict[str, Any]:
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    target = absolute_path(request.get("path"), "path")
    chain, fd = open_dir(target, authority, create=True)
    try:
        verify_target(chain, target)
        return {"stat": stat_proof(os.fstat(fd))}
    finally:
        close_chain(chain)


def mode_value(request: Dict[str, Any]) -> int:
    mode = request.get("mode", 0o600)
    if isinstance(mode, bool) or not isinstance(mode, int) or mode < 0 or mode > 0o777:
        boundary_fail("ARGUMENT_INVALID", "mode is invalid")
    return mode


def write_exclusive_action(request: Dict[str, Any]) -> Dict[str, Any]:
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    target = absolute_path(request.get("path"), "path")
    under(authority, target, "path")
    data = decode_bytes(request)
    chain, parent_fd = open_dir(target.parent, authority)
    fd: Optional[int] = None
    try:
        flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0)
        try:
            fd = os.open(target.name, flags, mode_value(request), dir_fd=parent_fd)
        except FileExistsError:
            boundary_fail("TARGET_EXISTS", "exclusive publication target already exists")
        offset = 0
        while offset < len(data):
            offset += os.write(fd, data[offset:])
        os.fsync(fd)
        opened = os.fstat(fd)
        current = os.stat(target.name, dir_fd=parent_fd, follow_symlinks=False)
        if identity(opened) != identity(current):
            boundary_fail("ENTRY_CHANGED", "exclusive publication path changed before commit")
        os.fsync(parent_fd)
        verify_target(chain, target.parent)
        return {"stat": stat_proof(opened, hashlib.sha256(data).hexdigest())}
    finally:
        if fd is not None:
            os.close(fd)
        close_chain(chain)


def read_expected_at(parent_fd: int, name: str, expected: Dict[str, Any], maximum: int) -> Tuple[bytes, os.stat_result]:
    data, opened = read_at(parent_fd, name, maximum)
    if not proof_matches(opened, expected, data):
        boundary_fail("ENTRY_CHANGED", "entry differs from its frozen exact proof")
    return data, opened


def stat_expected_at(parent_fd: int, name: str,
                     expected: Dict[str, Any]) -> os.stat_result:
    """Verify an exact regular-file generation without reading its body."""
    try:
        opened = os.stat(name, dir_fd=parent_fd, follow_symlinks=False)
    except FileNotFoundError:
        boundary_fail("PATH_MISSING", "file path is missing")
    if stat.S_ISLNK(opened.st_mode) or not stat.S_ISREG(opened.st_mode):
        boundary_fail("ENTRY_UNSAFE", "entry must be a regular non-symlink file")
    if not proof_matches(opened, expected):
        boundary_fail("ENTRY_CHANGED", "entry differs from its frozen exact proof")
    return opened


def unlink_owned_generation(parent_fd: int, name: str,
                            owned: Optional[os.stat_result]) -> bool:
    """Unlink only the inode generation created by this worker invocation."""
    if owned is None:
        return False
    try:
        current = os.stat(name, dir_fd=parent_fd, follow_symlinks=False)
    except FileNotFoundError:
        return True
    if stat.S_ISLNK(current.st_mode) or generation(current) != generation(owned):
        return False
    os.unlink(name, dir_fd=parent_fd)
    return True


REPLACE_WAL_SUFFIX = ".replace-wal.json"
REPLACE_RESERVATION_SUFFIX = ".replace-reservation.json"
REPLACE_TOKEN_RE = re.compile(r"^[a-f0-9]{32}$")
REPLACE_MUTEX_NAME = ".task-state-replace.lock"


def replace_mutex_safe(parent_fd: int, fd: int) -> bool:
    """Bind a waited-on replace mutex fd back to one private canonical inode."""
    try:
        opened = os.fstat(fd)
        named = os.stat(REPLACE_MUTEX_NAME, dir_fd=parent_fd, follow_symlinks=False)
        parent = os.fstat(parent_fd)
    except (FileNotFoundError, OSError):
        return False
    effective_uid = os.geteuid() if hasattr(os, "geteuid") else opened.st_uid
    return (stat.S_ISREG(opened.st_mode) and stat.S_ISREG(named.st_mode) and
            generation(opened) == generation(named) and opened.st_nlink == 1 and
            named.st_nlink == 1 and opened.st_dev == parent.st_dev and
            opened.st_uid == effective_uid and named.st_uid == effective_uid and
            stat.S_IMODE(opened.st_mode) == 0o600 and
            stat.S_IMODE(named.st_mode) == 0o600)


def acquire_replace_mutex(parent_fd: int):
    """Acquire the stable per-directory fence shared by replace and recovery."""
    flags = os.O_RDWR | getattr(os, "O_NOFOLLOW", 0) | getattr(os, "O_CLOEXEC", 0)
    fd: Optional[int] = None
    try:
        try:
            fd = os.open(REPLACE_MUTEX_NAME, flags | os.O_CREAT | os.O_EXCL,
                         0o600, dir_fd=parent_fd)
            os.fchmod(fd, 0o600)
            os.fsync(fd)
            os.fsync(parent_fd)
        except FileExistsError:
            fd = os.open(REPLACE_MUTEX_NAME, flags, dir_fd=parent_fd)
        if not replace_mutex_safe(parent_fd, fd):
            boundary_fail("RECOVERY_REQUIRED", "replace mutex is unsafe or changed during open")
        fh = os.fdopen(fd, "r+b", buffering=0)
        fd = None
        try:
            acquire_lock(fh)
            # A contender may have waited.  Re-bind the acquired kernel lock to
            # the still-canonical name before it can inspect or mutate any WAL.
            if not replace_mutex_safe(parent_fd, fh.fileno()):
                boundary_fail("RECOVERY_REQUIRED", "replace mutex generation changed while waiting")
            return fh
        except BaseException:
            try:
                release_lock(fh)
            except BaseException:
                pass
            fh.close()
            raise
    finally:
        if fd is not None:
            os.close(fd)


def release_replace_mutex(fh) -> None:
    try:
        release_lock(fh)
    finally:
        fh.close()


def replace_wal_name(target_name: str) -> str:
    return f".{target_name}{REPLACE_WAL_SUFFIX}"


def replace_reservation_name(target_name: str) -> str:
    return f".{target_name}{REPLACE_RESERVATION_SUFFIX}"


def canonical_json_bytes(value: Dict[str, Any]) -> bytes:
    return (json.dumps(value, ensure_ascii=True, sort_keys=True,
                       separators=(",", ":")) + "\n").encode("utf-8")


def write_regular_at(parent_fd: int, name: str, data: bytes, mode: int) -> os.stat_result:
    fd: Optional[int] = None
    try:
        fd = os.open(name, os.O_WRONLY | os.O_CREAT | os.O_EXCL |
                     getattr(os, "O_NOFOLLOW", 0), mode, dir_fd=parent_fd)
        offset = 0
        while offset < len(data):
            offset += os.write(fd, data[offset:])
        os.fsync(fd)
        opened = os.fstat(fd)
        current = os.stat(name, dir_fd=parent_fd, follow_symlinks=False)
        if not stat.S_ISREG(opened.st_mode) or identity(opened) != identity(current):
            boundary_fail("ENTRY_CHANGED", "private replace artifact changed while being written")
        return opened
    finally:
        if fd is not None:
            os.close(fd)


def read_optional_at(parent_fd: int, name: str, maximum: int) -> Optional[Tuple[bytes, os.stat_result]]:
    try:
        return read_at(parent_fd, name, maximum)
    except BoundaryError as exc:
        if exc.code == "PATH_MISSING":
            return None
        raise


def replace_crash_hook(request: Dict[str, Any], stage: str, target: Path) -> None:
    """Hard-crash failpoint, enabled only inside an explicitly fenced fixture."""
    wanted_stage = os.environ.get("FINALIZE_FS_TEST_CRASH_STAGE", "")
    wanted_target = os.environ.get("FINALIZE_FS_TEST_CRASH_TARGET", "")
    fixture_root_raw = os.environ.get("FINALIZE_FS_TEST_ROOT", "")
    sentinel_raw = os.environ.get("FINALIZE_FS_TEST_CRASH_SENTINEL", "")
    # The fixture root is shared by the compare-at-mutation hooks.  Only this
    # hook's own selector/sentinel fields activate the hard-crash path.
    if not any((wanted_stage, wanted_target, sentinel_raw)):
        return
    if not all((wanted_stage, wanted_target, fixture_root_raw, sentinel_raw)):
        boundary_fail("TEST_HOOK_INVALID", "replace crash hook is incomplete")
    if stage != wanted_stage or target != Path(os.path.abspath(wanted_target)):
        return
    fixture_root = Path(os.path.abspath(fixture_root_raw))
    sentinel = Path(os.path.abspath(sentinel_raw))
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    canonical = Path(os.path.abspath(request.get("canonicalRoot", "")))
    if not request.get("fixture") or authority == canonical:
        boundary_fail("TEST_HOOK_INVALID", "replace crash hook is disabled outside isolated fixtures")
    under(fixture_root, target, "replace crash target")
    under(fixture_root, sentinel, "replace crash sentinel")
    try:
        fd = os.open(sentinel, os.O_WRONLY | os.O_CREAT | os.O_EXCL |
                     getattr(os, "O_NOFOLLOW", 0), 0o600)
        try:
            os.fsync(fd)
        finally:
            os.close(fd)
    except FileExistsError:
        return
    # This deliberately bypasses finally blocks and the JSON envelope.  The
    # caller recognizes status 88 only in an isolated fixture and exits too,
    # which models loss of the whole mutator at the durable boundary.
    os._exit(88)


def replace_pause_hook(request: Dict[str, Any], stage: str, target: Path) -> None:
    """Bounded concurrency failpoint, fenced to an isolated fixture."""
    wanted_stage = os.environ.get("FINALIZE_FS_TEST_PAUSE_STAGE", "")
    wanted_target = os.environ.get("FINALIZE_FS_TEST_PAUSE_TARGET", "")
    fixture_root_raw = os.environ.get("FINALIZE_FS_TEST_ROOT", "")
    sentinel_raw = os.environ.get("FINALIZE_FS_TEST_PAUSE_SENTINEL", "")
    delay_raw = os.environ.get("FINALIZE_FS_TEST_PAUSE_MS", "")
    if not any((wanted_stage, wanted_target, sentinel_raw, delay_raw)):
        return
    if not all((wanted_stage, wanted_target, fixture_root_raw, sentinel_raw, delay_raw)):
        boundary_fail("TEST_HOOK_INVALID", "replace pause hook is incomplete")
    if stage != wanted_stage or target != Path(os.path.abspath(wanted_target)):
        return
    if re.fullmatch(r"[1-9][0-9]{0,3}", delay_raw) is None or int(delay_raw) > 5000:
        boundary_fail("TEST_HOOK_INVALID", "replace pause duration is invalid")
    fixture_root = Path(os.path.abspath(fixture_root_raw))
    sentinel = Path(os.path.abspath(sentinel_raw))
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    canonical = Path(os.path.abspath(request.get("canonicalRoot", "")))
    if not request.get("fixture") or authority == canonical:
        boundary_fail("TEST_HOOK_INVALID", "replace pause hook is disabled outside isolated fixtures")
    under(fixture_root, target, "replace pause target")
    under(fixture_root, sentinel, "replace pause sentinel")
    try:
        fd = os.open(sentinel, os.O_WRONLY | os.O_CREAT | os.O_EXCL |
                     getattr(os, "O_NOFOLLOW", 0), 0o600)
        try:
            os.fsync(fd)
        finally:
            os.close(fd)
    except FileExistsError:
        return
    time.sleep(int(delay_raw) / 1000.0)


def parse_replace_journal(raw: bytes, wal_name: str,
                          requested_target: Optional[str] = None) -> Tuple[Dict[str, Any], bytes]:
    try:
        value = json.loads(raw.decode("utf-8"))
    except (UnicodeError, json.JSONDecodeError):
        boundary_fail("RECOVERY_REQUIRED", "replace WAL is not canonical JSON")
    expected_keys = ["candidate", "candidateProof", "detached", "expected", "maxBytes",
                     "mode", "rawBase64", "target", "token", "version"]
    if not isinstance(value, dict) or sorted(value.keys()) != expected_keys or value.get("version") != 1:
        boundary_fail("RECOVERY_REQUIRED", "replace WAL has an invalid schema")
    canonical = canonical_json_bytes(value)
    if raw != canonical:
        boundary_fail("RECOVERY_REQUIRED", "replace WAL is not canonically encoded")
    target_name = value.get("target")
    token = value.get("token")
    if (not isinstance(target_name, str) or not target_name or target_name in (".", "..") or
            "/" in target_name or "\\" in target_name or "\x00" in target_name or
            requested_target is not None and target_name != requested_target or
            wal_name != replace_wal_name(target_name) or
            not isinstance(token, str) or not REPLACE_TOKEN_RE.fullmatch(token)):
        boundary_fail("RECOVERY_REQUIRED", "replace WAL identity is invalid")
    candidate = f".{target_name}.replace-candidate-{token}"
    detached = f".{target_name}.replace-detached-{token}"
    if value.get("candidate") != candidate or value.get("detached") != detached:
        boundary_fail("RECOVERY_REQUIRED", "replace WAL private names are invalid")
    maximum = value.get("maxBytes")
    mode = value.get("mode")
    if (isinstance(maximum, bool) or not isinstance(maximum, int) or maximum < 1 or
            maximum > MAX_RESULT_BYTES or isinstance(mode, bool) or not isinstance(mode, int) or
            mode < 0 or mode > 0o777):
        boundary_fail("RECOVERY_REQUIRED", "replace WAL bounds are invalid")
    proof_keys = ["ctimeNs", "dev", "hash", "ino", "kind", "mode", "mtimeNs", "size"]
    candidate_raw = value.get("candidateProof")
    if not isinstance(candidate_raw, dict) or sorted(candidate_raw.keys()) != proof_keys:
        boundary_fail("RECOVERY_REQUIRED", "replace WAL candidate proof has unexpected fields")
    candidate_proof = parse_expected(candidate_raw, "replace candidate")
    if candidate_proof.get("kind") != "file" or candidate_proof.get("hash") is None:
        boundary_fail("RECOVERY_REQUIRED", "replace WAL candidate proof is invalid")
    expected = value.get("expected")
    if expected is not None:
        if not isinstance(expected, dict) or sorted(expected.keys()) != proof_keys:
            boundary_fail("RECOVERY_REQUIRED", "replace WAL source proof has unexpected fields")
        expected = parse_expected(expected, "replace source")
        if expected.get("kind") != "file" or expected.get("hash") is None:
            boundary_fail("RECOVERY_REQUIRED", "replace WAL source proof is invalid")
    try:
        data = base64.b64decode(value.get("rawBase64", "").encode("ascii"), validate=True)
    except (ValueError, UnicodeError, AttributeError):
        boundary_fail("RECOVERY_REQUIRED", "replace WAL payload is invalid")
    if (len(data) > maximum or base64.b64encode(data).decode("ascii") != value.get("rawBase64") or
            candidate_proof.get("hash") != "sha256:" + hashlib.sha256(data).hexdigest() or
            candidate_proof.get("size") != len(data)):
        boundary_fail("RECOVERY_REQUIRED", "replace WAL payload differs from its frozen candidate")
    value["expected"] = expected
    value["candidateProof"] = candidate_proof
    return value, data


def parse_replace_reservation(raw: bytes, reservation_name: str,
                              requested_target: Optional[str] = None) -> Tuple[Dict[str, Any], bytes]:
    try:
        value = json.loads(raw.decode("utf-8"))
    except (UnicodeError, json.JSONDecodeError):
        boundary_fail("RECOVERY_REQUIRED", "replace reservation is not canonical JSON")
    expected_keys = ["candidate", "detached", "expected", "maxBytes", "mode",
                     "rawBase64", "target", "token", "version"]
    if not isinstance(value, dict) or sorted(value.keys()) != expected_keys or value.get("version") != 1:
        boundary_fail("RECOVERY_REQUIRED", "replace reservation has an invalid schema")
    if raw != canonical_json_bytes(value):
        boundary_fail("RECOVERY_REQUIRED", "replace reservation is not canonically encoded")
    target_name = value.get("target")
    token = value.get("token")
    if (not isinstance(target_name, str) or not target_name or target_name in (".", "..") or
            "/" in target_name or "\\" in target_name or "\x00" in target_name or
            requested_target is not None and target_name != requested_target or
            reservation_name != replace_reservation_name(target_name) or
            not isinstance(token, str) or not REPLACE_TOKEN_RE.fullmatch(token)):
        boundary_fail("RECOVERY_REQUIRED", "replace reservation identity is invalid")
    candidate = f".{target_name}.replace-candidate-{token}"
    detached = f".{target_name}.replace-detached-{token}"
    if value.get("candidate") != candidate or value.get("detached") != detached:
        boundary_fail("RECOVERY_REQUIRED", "replace reservation private names are invalid")
    maximum = value.get("maxBytes")
    mode = value.get("mode")
    if (isinstance(maximum, bool) or not isinstance(maximum, int) or maximum < 1 or
            maximum > MAX_RESULT_BYTES or isinstance(mode, bool) or not isinstance(mode, int) or
            mode < 0 or mode > 0o777):
        boundary_fail("RECOVERY_REQUIRED", "replace reservation bounds are invalid")
    proof_keys = ["ctimeNs", "dev", "hash", "ino", "kind", "mode", "mtimeNs", "size"]
    expected = value.get("expected")
    if expected is not None:
        if not isinstance(expected, dict) or sorted(expected.keys()) != proof_keys:
            boundary_fail("RECOVERY_REQUIRED", "replace reservation source proof has unexpected fields")
        expected = parse_expected(expected, "replace reservation source")
        if expected.get("kind") != "file" or expected.get("hash") is None:
            boundary_fail("RECOVERY_REQUIRED", "replace reservation source proof is invalid")
    try:
        data = base64.b64decode(value.get("rawBase64", "").encode("ascii"), validate=True)
    except (ValueError, UnicodeError, AttributeError):
        boundary_fail("RECOVERY_REQUIRED", "replace reservation payload is invalid")
    if len(data) > maximum or base64.b64encode(data).decode("ascii") != value.get("rawBase64"):
        boundary_fail("RECOVERY_REQUIRED", "replace reservation payload exceeds its bound")
    value["expected"] = expected
    return value, data


def moved_entry_matches(value: Optional[Tuple[bytes, os.stat_result]],
                        expected: Dict[str, Any]) -> bool:
    return value is not None and moved_proof_matches(value[1], expected, value[0])


def target_replace_artifacts(parent_fd: int, target_name: str) -> List[str]:
    prefix = f".{target_name}.replace-"
    names = os.listdir(parent_fd)
    if len(names) > 100000:
        boundary_fail("DIRECTORY_TOO_LARGE", "replace directory exceeds its recovery bound")
    return sorted(name for name in names if name.startswith(prefix))


def remove_exact_private_at(parent_fd: int, name: str, frozen: Tuple[bytes, os.stat_result],
                            maximum: int, label: str) -> None:
    live = read_optional_at(parent_fd, name, maximum)
    if live is None or identity(live[1]) != identity(frozen[1]) or live[0] != frozen[0]:
        boundary_fail("RECOVERY_REQUIRED", f"{label} changed before exact cleanup")
    os.unlink(name, dir_fd=parent_fd)


def recover_replace_reservation(request: Dict[str, Any], target: Path, parent_fd: int,
                                maximum: int) -> Dict[str, Any]:
    """Discard or promote only a candidate authorized by a durable pre-WAL intent."""
    reservation_name = replace_reservation_name(target.name)
    reservation_value = read_optional_at(parent_fd, reservation_name, MAX_REQUEST)
    if reservation_value is None:
        return {"recovered": False}
    reservation_raw, reservation_stat = reservation_value
    parent_stat = os.fstat(parent_fd)
    effective_uid = os.geteuid() if hasattr(os, "geteuid") else reservation_stat.st_uid
    if (reservation_stat.st_nlink != 1 or reservation_stat.st_dev != parent_stat.st_dev or
            reservation_stat.st_uid != effective_uid or
            stat.S_IMODE(reservation_stat.st_mode) & ~0o600):
        boundary_fail("RECOVERY_REQUIRED", "replace reservation is not an owned private artifact")
    reservation, intended = parse_replace_reservation(
        reservation_raw, reservation_name, target.name)
    if reservation["maxBytes"] != maximum:
        boundary_fail("RECOVERY_REQUIRED", "replace reservation retry uses a different byte bound")

    wal_name = replace_wal_name(target.name)
    wal_value = read_optional_at(parent_fd, wal_name, MAX_REQUEST)
    allowed_artifacts = {reservation_name, reservation["candidate"]}
    if wal_value is not None:
        allowed_artifacts.update((wal_name, reservation["detached"]))
    unexpected_artifacts = [name for name in target_replace_artifacts(parent_fd, target.name)
                            if name not in allowed_artifacts]
    if unexpected_artifacts:
        boundary_fail("RECOVERY_REQUIRED", "unexpected private artifacts block reservation ownership inference")
    if wal_value is not None:
        journal, _ = parse_replace_journal(wal_value[0], wal_name, target.name)
        shared = ["candidate", "detached", "expected", "maxBytes", "mode",
                  "rawBase64", "target", "token", "version"]
        if any(journal.get(key) != reservation.get(key) for key in shared):
            boundary_fail("RECOVERY_REQUIRED", "replace WAL differs from its durable reservation")
        remove_exact_private_at(parent_fd, reservation_name, reservation_value,
                                MAX_REQUEST, "replace reservation")
        os.fsync(parent_fd)
        return {"recovered": True, "promoted": True}

    detached = read_optional_at(parent_fd, reservation["detached"], maximum)
    if detached is not None:
        boundary_fail("RECOVERY_REQUIRED", "pre-WAL reservation unexpectedly has a detached source")
    current = read_optional_at(parent_fd, target.name, maximum)
    expected = reservation["expected"]
    if expected is None:
        if current is not None:
            boundary_fail("RECOVERY_REQUIRED", "foreign canonical generation blocks reservation cleanup")
    elif current is None or not proof_matches(current[1], expected, current[0]):
        boundary_fail("RECOVERY_REQUIRED", "canonical source changed before reservation cleanup")

    candidate = read_optional_at(parent_fd, reservation["candidate"], maximum)
    expected_private = {reservation_name}
    if candidate is not None:
        expected_private.add(reservation["candidate"])
    if set(target_replace_artifacts(parent_fd, target.name)) != expected_private:
        boundary_fail("RECOVERY_REQUIRED", "pre-WAL private namespace changed before cleanup")
    if candidate is not None:
        candidate_stat = candidate[1]
        owned_mode = stat.S_IMODE(candidate_stat.st_mode)
        effective_uid = os.geteuid() if hasattr(os, "geteuid") else candidate_stat.st_uid
        # No candidate inode exists when the reservation is made, so pre-WAL
        # authority deliberately belongs to the exact random private name,
        # not to a later inode number.  The writer emits the intended payload
        # strictly from byte zero upward while holding the directory replace
        # mutex.  After a hard crash, an owned sole-link prefix (including an
        # empty file) is therefore the only safely discardable partial write;
        # a non-prefix claimant remains preserved fail-closed.
        if (not intended.startswith(candidate[0]) or candidate_stat.st_nlink != 1 or
                candidate_stat.st_dev != parent_stat.st_dev or
                candidate_stat.st_uid != effective_uid or
                owned_mode & ~reservation["mode"]):
            boundary_fail("RECOVERY_REQUIRED", "pre-WAL candidate is not the exact reservation-owned generation")
        remove_exact_private_at(parent_fd, reservation["candidate"], candidate,
                                maximum, "pre-WAL candidate")
        # Make candidate disappearance durable while the reservation still
        # authorizes its namespace.  Only then may the reservation itself be
        # removed; otherwise a crash could resurrect an authority-less orphan.
        os.fsync(parent_fd)
        if set(target_replace_artifacts(parent_fd, target.name)) != {reservation_name}:
            boundary_fail("RECOVERY_REQUIRED", "pre-WAL private namespace changed after candidate cleanup")
    remove_exact_private_at(parent_fd, reservation_name, reservation_value,
                            MAX_REQUEST, "replace reservation")
    os.fsync(parent_fd)
    return {"recovered": True, "promoted": False}


def recover_replace_wal(request: Dict[str, Any], target: Path, parent_fd: int,
                        maximum: int) -> Dict[str, Any]:
    """Roll an immutable replace intent forward without overwriting a stranger."""
    wal_name = replace_wal_name(target.name)
    wal_value = read_optional_at(parent_fd, wal_name, MAX_REQUEST)
    if wal_value is None:
        if target_replace_artifacts(parent_fd, target.name):
            boundary_fail("RECOVERY_REQUIRED", "orphan replace artifacts block a new replacement")
        return {"recovered": False}
    wal_raw, wal_stat = wal_value
    journal, intended = parse_replace_journal(wal_raw, wal_name, target.name)
    if journal["maxBytes"] != maximum:
        boundary_fail("RECOVERY_REQUIRED", "replace retry uses a different byte bound")
    candidate_name = journal["candidate"]
    detached_name = journal["detached"]
    expected = journal["expected"]
    candidate_proof = journal["candidateProof"]
    allowed_artifacts = {wal_name, candidate_name, detached_name}
    unexpected_artifacts = [name for name in target_replace_artifacts(parent_fd, target.name)
                            if name not in allowed_artifacts]
    if unexpected_artifacts:
        boundary_fail("RECOVERY_REQUIRED", "unexpected private replace artifacts block ownership inference")

    candidate = read_optional_at(parent_fd, candidate_name, maximum)
    detached = read_optional_at(parent_fd, detached_name, maximum)
    current = read_optional_at(parent_fd, target.name, maximum)
    if candidate is not None and not moved_entry_matches(candidate, candidate_proof):
        boundary_fail("RECOVERY_REQUIRED", "replace candidate contains a foreign generation")
    if detached is not None and (expected is None or not moved_entry_matches(detached, expected)):
        boundary_fail("RECOVERY_REQUIRED", "replace detachment contains a foreign generation")

    target_is_new = moved_entry_matches(current, candidate_proof)
    target_is_old = expected is not None and moved_entry_matches(current, expected)
    if current is not None and not target_is_new and not target_is_old:
        boundary_fail("RECOVERY_REQUIRED", "foreign canonical generation blocks replace recovery")

    replace_crash_hook(request, "after-replace-wal", target)
    if not target_is_new:
        if candidate is None:
            boundary_fail("RECOVERY_REQUIRED", "replace candidate disappeared before publication")
        if target_is_old:
            if detached is None:
                try:
                    os.link(target.name, detached_name, src_dir_fd=parent_fd, dst_dir_fd=parent_fd,
                            follow_symlinks=False)
                except FileExistsError:
                    boundary_fail("RECOVERY_REQUIRED", "foreign replace detachment blocks source fencing")
                os.fsync(parent_fd)
                detached = read_optional_at(parent_fd, detached_name, maximum)
            # The no-clobber hard link keeps an owned old name durable before
            # the canonical name is unlinked.  A crash between these steps is
            # therefore recoverable with both names present.
            current = read_optional_at(parent_fd, target.name, maximum)
            if not moved_entry_matches(current, expected) or not moved_entry_matches(detached, expected):
                boundary_fail("RECOVERY_REQUIRED", "replace source differs from its frozen linked detachment")
            os.unlink(target.name, dir_fd=parent_fd)
            os.fsync(parent_fd)
        elif expected is not None and detached is None:
            boundary_fail("RECOVERY_REQUIRED", "replace source disappeared without its durable detachment")
        replace_crash_hook(request, "after-replace-detach", target)
        try:
            os.link(candidate_name, target.name, src_dir_fd=parent_fd, dst_dir_fd=parent_fd,
                    follow_symlinks=False)
        except FileExistsError:
            boundary_fail("RECOVERY_REQUIRED", "racing canonical generation blocks replace publication")
        current = read_optional_at(parent_fd, target.name, maximum)
        if not moved_entry_matches(current, candidate_proof):
            boundary_fail("RECOVERY_REQUIRED", "published replace generation differs from its candidate")
        os.fsync(parent_fd)
        target_is_new = True
    replace_crash_hook(request, "after-replace-publish", target)

    # Cleanup is exact and idempotent.  Ctime may advance when a hard link is
    # added or removed, so generation/content/mode/mtime remain the ownership
    # fence while every foreign generation is preserved.
    detached = read_optional_at(parent_fd, detached_name, maximum)
    if detached is not None:
        if expected is None or not moved_entry_matches(detached, expected):
            boundary_fail("RECOVERY_REQUIRED", "replace detachment changed before cleanup")
        os.unlink(detached_name, dir_fd=parent_fd)
    candidate = read_optional_at(parent_fd, candidate_name, maximum)
    if candidate is not None:
        if not moved_entry_matches(candidate, candidate_proof):
            boundary_fail("RECOVERY_REQUIRED", "replace candidate changed before cleanup")
        os.unlink(candidate_name, dir_fd=parent_fd)
    # Persist removal of every data alias while the WAL still exists.  Only
    # after that ordering point may disappearance of the WAL become durable.
    os.fsync(parent_fd)
    live_wal = read_optional_at(parent_fd, wal_name, MAX_REQUEST)
    if live_wal is None or identity(live_wal[1]) != identity(wal_stat) or live_wal[0] != wal_raw:
        boundary_fail("RECOVERY_REQUIRED", "replace WAL changed before cleanup")
    os.unlink(wal_name, dir_fd=parent_fd)
    os.fsync(parent_fd)
    current = read_optional_at(parent_fd, target.name, maximum)
    if not moved_entry_matches(current, candidate_proof):
        boundary_fail("RECOVERY_REQUIRED", "replace target changed after recovery")
    return {"recovered": True,
            "stat": stat_proof(current[1], hashlib.sha256(current[0]).hexdigest())}


def remove_action(request: Dict[str, Any]) -> Dict[str, Any]:
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    target = absolute_path(request.get("path"), "path")
    under(authority, target, "path")
    expected = parse_expected(request.get("expected"), "delete")
    maximum = request.get("maxBytes", MAX_RESULT_BYTES)
    if isinstance(maximum, bool) or not isinstance(maximum, int) or maximum < 1 or maximum > MAX_RESULT_BYTES:
        boundary_fail("ARGUMENT_INVALID", "maxBytes is outside its bound")
    chain, parent_fd = open_dir(target.parent, authority)
    try:
        try:
            opened = (stat_expected_at(parent_fd, target.name, expected)
                      if expected.get("hash") is None
                      else read_expected_at(parent_fd, target.name, expected, maximum)[1])
        except BoundaryError as exc:
            if exc.code == "PATH_MISSING" and request.get("allowMissing") is True:
                verify_target(chain, target.parent)
                return {"removed": False}
            raise
        fixture_hook(request, "before-remove", target, parent_fd, target.name)
        # Compare once more at the exact unlink boundary.  The adversarial hook
        # above deliberately runs between proof and this comparison.
        if expected.get("hash") is None:
            live = stat_expected_at(parent_fd, target.name, expected)
            matches = identity(live) == identity(opened)
        else:
            data, live = read_at(parent_fd, target.name, maximum)
            matches = identity(live) == identity(opened) and proof_matches(live, expected, data)
        if not matches:
            boundary_fail("ENTRY_CHANGED", "foreign replacement appeared before delete; it was preserved")
        os.unlink(target.name, dir_fd=parent_fd)
        os.fsync(parent_fd)
        verify_target(chain, target.parent)
        return {"removed": True}
    finally:
        close_chain(chain)


def link_action(request: Dict[str, Any]) -> Dict[str, Any]:
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    source = absolute_path(request.get("source"), "source")
    target = absolute_path(request.get("target"), "target")
    under(authority, source, "source")
    under(authority, target, "target")
    expected = parse_expected(request.get("expected"), "source")
    maximum = request.get("maxBytes", MAX_RESULT_BYTES)
    source_chain, source_fd = open_dir(source.parent, authority)
    target_chain, target_fd = open_dir(target.parent, authority)
    try:
        stat_only = expected.get("hash") is None
        if stat_only:
            data = None
            opened = stat_expected_at(source_fd, source.name, expected)
        else:
            data, opened = read_expected_at(source_fd, source.name, expected, maximum)
        fixture_hook(request, "before-link", source, source_fd, source.name)
        if stat_only:
            live = stat_expected_at(source_fd, source.name, expected)
            matches = identity(live) == identity(opened)
        else:
            data2, live = read_at(source_fd, source.name, maximum)
            matches = identity(live) == identity(opened) and proof_matches(live, expected, data2)
        if not matches:
            boundary_fail("ENTRY_CHANGED", "source changed before no-clobber link")
        try:
            os.link(source.name, target.name, src_dir_fd=source_fd, dst_dir_fd=target_fd,
                    follow_symlinks=False)
        except FileExistsError:
            boundary_fail("TARGET_EXISTS", "no-clobber link target already exists")
        published = os.stat(target.name, dir_fd=target_fd, follow_symlinks=False)
        if generation(published) != generation(live):
            boundary_fail("ENTRY_CHANGED", "no-clobber link did not publish the source generation")
        os.fsync(target_fd)
        verify_target(source_chain, source.parent)
        verify_target(target_chain, target.parent)
        return {"stat": stat_proof(
            published,
            None if data is None else hashlib.sha256(data).hexdigest())}
    finally:
        close_chain(target_chain)
        close_chain(source_chain)


def move_action(request: Dict[str, Any]) -> Dict[str, Any]:
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    source = absolute_path(request.get("source"), "source")
    target = absolute_path(request.get("target"), "target")
    under(authority, source, "source")
    under(authority, target, "target")
    expected = parse_expected(request.get("expected"), "source")
    maximum = request.get("maxBytes", MAX_RESULT_BYTES)
    source_chain, source_fd = open_dir(source.parent, authority)
    target_chain, target_fd = open_dir(target.parent, authority)
    try:
        stat_only = expected.get("hash") is None
        if stat_only:
            data = None
            opened = stat_expected_at(source_fd, source.name, expected)
        else:
            data, opened = read_expected_at(source_fd, source.name, expected, maximum)
        try:
            os.stat(target.name, dir_fd=target_fd, follow_symlinks=False)
            boundary_fail("TARGET_EXISTS", "no-clobber move target already exists")
        except FileNotFoundError:
            pass
        fixture_hook(request, "before-move", source, source_fd, source.name)
        if stat_only:
            live = stat_expected_at(source_fd, source.name, expected)
            matches = identity(live) == identity(opened)
        else:
            data2, live = read_at(source_fd, source.name, maximum)
            matches = identity(live) == identity(opened) and proof_matches(live, expected, data2)
        if not matches:
            boundary_fail("ENTRY_CHANGED", "foreign replacement appeared before move; it was preserved")
        os.rename(source.name, target.name, src_dir_fd=source_fd, dst_dir_fd=target_fd)
        if stat_only:
            moved_data = None
            moved = os.stat(target.name, dir_fd=target_fd, follow_symlinks=False)
        else:
            moved_data, moved = read_at(target_fd, target.name, maximum)
        if not moved_proof_matches(moved, expected, moved_data):
            # The raced generation is preserved. Restore its canonical name only
            # if no newer canonical entry appeared meanwhile.
            try:
                os.link(target.name, source.name, src_dir_fd=target_fd, dst_dir_fd=source_fd,
                        follow_symlinks=False)
                restored = True
            except FileExistsError:
                restored = False
            if not restored:
                boundary_fail("RECOVERY_REQUIRED", "moved foreign generation is retained at the private target")
            boundary_fail("ENTRY_CHANGED", "moved generation differs from the frozen source")
        os.fsync(source_fd)
        if target_fd != source_fd:
            os.fsync(target_fd)
        verify_target(source_chain, source.parent)
        verify_target(target_chain, target.parent)
        return {"stat": stat_proof(
            moved,
            None if data is None else hashlib.sha256(data).hexdigest())}
    finally:
        close_chain(target_chain)
        close_chain(source_chain)


def replace_action(request: Dict[str, Any]) -> Dict[str, Any]:
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    target = absolute_path(request.get("path"), "path")
    under(authority, target, "path")
    data = decode_bytes(request)
    expected_raw = request.get("expected")
    expected = None if expected_raw is None else parse_expected(expected_raw, "replace")
    maximum = request.get("maxBytes", MAX_RESULT_BYTES)
    if isinstance(maximum, bool) or not isinstance(maximum, int) or maximum < 1 or maximum > MAX_RESULT_BYTES:
        boundary_fail("ARGUMENT_INVALID", "maxBytes is outside its bound")
    chain, parent_fd = open_dir(target.parent, authority)
    replace_mutex = acquire_replace_mutex(parent_fd)
    token = os.urandom(16).hex()
    candidate = f".{target.name}.replace-candidate-{token}"
    detached = f".{target.name}.replace-detached-{token}"
    wal_name = replace_wal_name(target.name)
    reservation_name = replace_reservation_name(target.name)
    candidate_fd: Optional[int] = None
    candidate_stat: Optional[os.stat_result] = None
    candidate_present = False
    wal_published = False
    try:
        recover_replace_reservation(request, target, parent_fd, maximum)
        recovered = recover_replace_wal(request, target, parent_fd, maximum)
        if recovered["recovered"]:
            # The interrupted intent is authoritative.  It satisfies this
            # call only when the requested bytes are identical; otherwise the
            # caller must re-open the recovered generation and retry from a
            # fresh ownership proof.
            requested_hash = "sha256:" + hashlib.sha256(data).hexdigest()
            if recovered["stat"].get("hash") == requested_hash:
                return {"stat": recovered["stat"], "recovered": True}
            boundary_fail("ENTRY_CHANGED", "an earlier replace intent was recovered; retry from its fresh proof")
        if expected is None:
            try:
                os.stat(target.name, dir_fd=parent_fd, follow_symlinks=False)
                boundary_fail("TARGET_EXISTS", "no-clobber replace target already exists")
            except FileNotFoundError:
                pass
            frozen_expected = None
        else:
            old_data, old_stat = read_expected_at(parent_fd, target.name, expected, maximum)
            frozen_expected = stat_proof(old_stat, hashlib.sha256(old_data).hexdigest())
        candidate_mode = mode_value(request)
        reservation = {
            "candidate": candidate,
            "detached": detached,
            "expected": frozen_expected,
            "maxBytes": maximum,
            "mode": candidate_mode,
            "rawBase64": base64.b64encode(data).decode("ascii"),
            "target": target.name,
            "token": token,
            "version": 1,
        }
        reservation_bytes = canonical_json_bytes(reservation)
        if len(reservation_bytes) > MAX_REQUEST:
            boundary_fail("ARGUMENT_INVALID", "replace reservation exceeds its byte bound")
        try:
            reservation_stat = write_regular_at(parent_fd, reservation_name, reservation_bytes, 0o600)
        except FileExistsError:
            boundary_fail("RECOVERY_REQUIRED", "another replace reservation appeared during publication")
        # The reservation owns the exact private candidate namespace before
        # that candidate can become visible.  A crash from this point through
        # WAL publication is therefore boundedly discardable on retry.
        os.fsync(parent_fd)
        flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0)
        candidate_fd = os.open(candidate, flags, candidate_mode, dir_fd=parent_fd)
        candidate_present = True
        offset = 0
        while offset < len(data):
            offset += os.write(candidate_fd, data[offset:])
        os.fsync(candidate_fd)
        candidate_stat = os.fstat(candidate_fd)
        os.close(candidate_fd)
        candidate_fd = None
        replace_pause_hook(request, "after-replace-candidate", target)
        replace_crash_hook(request, "after-replace-candidate", target)
        candidate_proof = stat_proof(candidate_stat, hashlib.sha256(data).hexdigest())
        journal = {
            "candidate": candidate,
            "candidateProof": candidate_proof,
            "detached": detached,
            "expected": frozen_expected,
            "maxBytes": maximum,
            "mode": candidate_mode,
            "rawBase64": reservation["rawBase64"],
            "target": target.name,
            "token": token,
            "version": 1,
        }
        journal_bytes = canonical_json_bytes(journal)
        if len(journal_bytes) > MAX_REQUEST:
            boundary_fail("ARGUMENT_INVALID", "replace WAL exceeds its byte bound")
        try:
            write_regular_at(parent_fd, wal_name, journal_bytes, 0o600)
        except FileExistsError:
            boundary_fail("RECOVERY_REQUIRED", "another replace WAL appeared during publication")
        wal_published = True
        os.fsync(parent_fd)
        live_reservation = read_optional_at(parent_fd, reservation_name, MAX_REQUEST)
        if (live_reservation is None or identity(live_reservation[1]) != identity(reservation_stat) or
                live_reservation[0] != reservation_bytes):
            boundary_fail("RECOVERY_REQUIRED", "replace reservation changed before WAL handoff")
        remove_exact_private_at(parent_fd, reservation_name, live_reservation,
                                MAX_REQUEST, "replace reservation")
        os.fsync(parent_fd)
        fixture_hook(request, "before-replace", target, parent_fd, target.name)
        committed = recover_replace_wal(request, target, parent_fd, maximum)
        candidate_present = False  # recovery removed the exact owned alias
        wal_published = False
        if not committed["recovered"]:
            boundary_fail("RECOVERY_REQUIRED", "durable replace WAL disappeared before commit")
        verify_target(chain, target.parent)
        return {"stat": committed["stat"]}
    finally:
        if candidate_fd is not None:
            if candidate_stat is None:
                try:
                    candidate_stat = os.fstat(candidate_fd)
                except OSError:
                    pass
            os.close(candidate_fd)
        if candidate_present and not wal_published:
            try:
                unlink_owned_generation(parent_fd, candidate, candidate_stat)
            except OSError:
                pass
        release_replace_mutex(replace_mutex)
        close_chain(chain)


def recover_replaces_action(request: Dict[str, Any]) -> Dict[str, Any]:
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    directory = absolute_path(request.get("path"), "path")
    under(authority, directory, "path")
    maximum = request.get("maxBytes", MAX_RESULT_BYTES)
    max_entries = request.get("maxEntries", 10000)
    if (isinstance(maximum, bool) or not isinstance(maximum, int) or maximum < 1 or maximum > MAX_RESULT_BYTES or
            isinstance(max_entries, bool) or not isinstance(max_entries, int) or max_entries < 1 or max_entries > 100000):
        boundary_fail("ARGUMENT_INVALID", "replace recovery bounds are invalid")
    chain, parent_fd = open_dir(directory, authority)
    replace_mutex = acquire_replace_mutex(parent_fd)
    try:
        names = sorted(os.listdir(parent_fd))
        if len(names) > max_entries:
            boundary_fail("DIRECTORY_TOO_LARGE", "replace recovery directory exceeds its entry bound")
        reservation_names = [name for name in names if name.startswith(".") and
                             name.endswith(REPLACE_RESERVATION_SUFFIX)]
        wal_names = [name for name in names if name.startswith(".") and name.endswith(REPLACE_WAL_SUFFIX)]
        private_pattern = re.compile(r"^\.(.+)\.replace-(?:candidate|detached)-[a-f0-9]{32}$")
        private_names = [name for name in names if private_pattern.fullmatch(name)]
        malformed = [name for name in names if name.startswith(".") and ".replace-" in name and
                     name not in reservation_names and name not in wal_names and name not in private_names]
        if malformed:
            boundary_fail("RECOVERY_REQUIRED", "malformed replace artifact name blocks recovery")
        for reservation_name in reservation_names:
            target_name = reservation_name[1:-len(REPLACE_RESERVATION_SUFFIX)]
            if not target_name or target_name in (".", "..") or "/" in target_name or "\\" in target_name:
                boundary_fail("RECOVERY_REQUIRED", "replace reservation filename is invalid")
            recover_replace_reservation(request, directory / target_name, parent_fd, maximum)
        # Reservation recovery can discard an uncommitted candidate or hand a
        # committed one off to its WAL.  Re-snapshot before orphan inference.
        names = sorted(os.listdir(parent_fd))
        if len(names) > max_entries:
            boundary_fail("DIRECTORY_TOO_LARGE", "replace recovery directory exceeds its entry bound")
        wal_names = [name for name in names if name.startswith(".") and name.endswith(REPLACE_WAL_SUFFIX)]
        private_names = [name for name in names if private_pattern.fullmatch(name)]
        wal_set = set(wal_names)
        for private_name in private_names:
            match = private_pattern.fullmatch(private_name)
            if match is None or replace_wal_name(match.group(1)) not in wal_set:
                boundary_fail("RECOVERY_REQUIRED", "orphan replace artifact blocks automatic recovery")
        recovered: List[Dict[str, Any]] = []
        for wal_name in wal_names:
            target_name = wal_name[1:-len(REPLACE_WAL_SUFFIX)]
            if not target_name or target_name in (".", "..") or "/" in target_name or "\\" in target_name:
                boundary_fail("RECOVERY_REQUIRED", "replace WAL filename is invalid")
            value = recover_replace_wal(request, directory / target_name, parent_fd, maximum)
            if value["recovered"]:
                recovered.append({"target": target_name, "stat": value["stat"]})
        remaining = [name for name in os.listdir(parent_fd)
                     if name.startswith(".") and ".replace-" in name]
        if remaining:
            boundary_fail("RECOVERY_REQUIRED", "replace artifacts remain after bounded recovery")
        verify_target(chain, directory)
        return {"recovered": recovered}
    finally:
        release_replace_mutex(replace_mutex)
        close_chain(chain)


def remove_empty_dir_action(request: Dict[str, Any]) -> Dict[str, Any]:
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    target = absolute_path(request.get("path"), "path")
    under(authority, target, "path")
    expected = parse_expected(request.get("expected"), "directory delete")
    if expected.get("kind") != "directory" or expected.get("hash") is not None:
        boundary_fail("ARGUMENT_INVALID", "directory delete proof must identify a directory without a content hash")
    chain, fd = open_dir(target, authority)
    parent_chain, parent_fd = open_dir(target.parent, authority)
    try:
        opened = os.fstat(fd)
        if not proof_matches(opened, expected):
            boundary_fail("DIRECTORY_CHANGED", "directory differs from its frozen exact proof")
        with os.scandir(fd) as iterator:
            if next(iterator, None) is not None:
                boundary_fail("DIRECTORY_NOT_EMPTY", "directory is not empty")
        fixture_hook(request, "before-remove-empty-dir", target, parent_fd, target.name)
        verify_target(chain, target)
        current = os.stat(target.name, dir_fd=parent_fd, follow_symlinks=False)
        if identity(current) != identity(opened) or not proof_matches(current, expected):
            boundary_fail("DIRECTORY_CHANGED", "directory changed before removal")
        os.rmdir(target.name, dir_fd=parent_fd)
        os.fsync(parent_fd)
        verify_target(parent_chain, target.parent)
        return {"removed": True}
    finally:
        close_chain(parent_chain)
        close_chain(chain)


def fsync_action(request: Dict[str, Any]) -> Dict[str, Any]:
    authority = absolute_path(request.get("authorityRoot"), "authorityRoot")
    target = absolute_path(request.get("path"), "path")
    chain, fd = open_dir(target, authority)
    try:
        os.fsync(fd)
        verify_target(chain, target)
        return {"synced": True}
    finally:
        close_chain(chain)


def fs_main() -> int:
    try:
        raw = sys.stdin.buffer.read(MAX_REQUEST + 1)
        if len(raw) > MAX_REQUEST:
            boundary_fail("ARGUMENT_INVALID", "request exceeds its byte bound")
        request = json.loads(raw.decode("utf-8"))
        if not isinstance(request, dict) or request.get("version") != 1:
            boundary_fail("ARGUMENT_INVALID", "request envelope is invalid")
        action = request.get("action")
        handlers = {
            "list": list_action,
            "stat": stat_action,
            "read": read_action,
            "ensure-dir": ensure_dir_action,
            "write-exclusive": write_exclusive_action,
            "replace": replace_action,
            "recover-replaces": recover_replaces_action,
            "link": link_action,
            "move": move_action,
            "remove": remove_action,
            "remove-empty-dir": remove_empty_dir_action,
            "fsync-dir": fsync_action,
        }
        if action not in handlers:
            boundary_fail("ARGUMENT_INVALID", "unsupported filesystem action")
        result = handlers[action](request)
        envelope = {"ok": True, "version": 1, "result": result}
    except BoundaryError as exc:
        envelope = {"ok": False, "version": 1,
                    "error": {"code": exc.code, "message": str(exc)}}
    except BaseException as exc:
        envelope = {"ok": False, "version": 1,
                    "error": {"code": "BOUNDARY_FAILED",
                              "message": f"{type(exc).__name__}: {str(exc)[:1000]}"}}
    sys.stdout.write(json.dumps(envelope, ensure_ascii=True, separators=(",", ":")) + "\n")
    return 0


def main() -> int:
    if len(sys.argv) >= 2 and sys.argv[1] == "fs-op":
        return fs_main()
    return mutex_main(sys.argv[1:])


if __name__ == "__main__":
    raise SystemExit(main())
