#!/usr/bin/env python3
"""Pinned-descriptor, kernel-locked conditional publisher for task INDEX data.

The process is a small line-delimited JSON server.  A publish session keeps the
repository, task directory, runtime directory and lock inode open for the full
scan -> CAS -> postcheck window.  Mutations are relative to those descriptors;
the JavaScript caller never receives a pathname-based write primitive.

INDEX replacement uses a durable detach -> no-clobber-link transaction.  The
incumbent is moved into a private WAL before the candidate can reach the target
name.  A last-window foreign generation is therefore restored or retained as
bounded evidence, never overwritten.  An uncommitted transaction is rolled
back automatically by the next publisher.  ``inspect`` is strictly read-only:
it neither creates a lock nor repairs WAL state.
"""

from __future__ import annotations

import base64
import binascii
import ctypes
import errno
import hashlib
import json
import os
import re
import secrets
import stat
import sys
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

if os.name == "nt":
    import msvcrt
else:
    import fcntl


MAX_REQUEST = 16 * 1024 * 1024
MAX_INDEX_BYTES = 8 * 1024 * 1024
MAX_DIAGNOSTIC_BYTES = 256 * 1024
MAX_MANIFEST_BYTES = 16 * 1024
MAX_ACTIVE = 8
MAX_EVIDENCE = 32
MAX_COMPONENTS = 96
MAX_DIRECTORY_ENTRIES = 20_000
HASH_RE = re.compile(r"^sha256:[a-f0-9]{64}$")
DECIMAL_RE = re.compile(r"^(?:0|[1-9][0-9]*)$")
TOKEN_RE = re.compile(r"^[a-f0-9]{48}$")
PROOF_FIELDS = {"dev", "ino", "mode", "size", "mtimeNs", "ctimeNs"}
MANIFEST_FIELDS = {
    "candidateHash", "candidateSize", "expected", "kind", "operationId",
    "targetName", "version",
}
COMMIT_FIELDS = {"candidateHash", "candidateProof", "version"}
TEST_HOOK_ENV = (
    "TASK_INDEX_TEST_CRASH_AT",
    "TASK_INDEX_TEST_REPLACE_TARGET_WITH",
    "TASK_INDEX_TEST_APPEAR_TARGET_WITH",
    "TASK_INDEX_TEST_REPLACE_PUBLISHED_WITH",
    "TASK_INDEX_TEST_DIAGNOSTIC_REPLACE_TARGET_WITH",
    "TASK_INDEX_TEST_DIAGNOSTIC_APPEAR_TARGET_WITH",
    "TASK_INDEX_TEST_DIAGNOSTIC_REPLACE_PUBLISHED_WITH",
)


class BoundaryError(Exception):
    def __init__(self, code: str, message: str):
        super().__init__(str(message)[:1000])
        self.code = code


def fail(code: str, message: str) -> None:
    raise BoundaryError(code, message)


def identity(value: os.stat_result) -> Tuple[int, int, int, int, int, int]:
    return (value.st_dev, value.st_ino, value.st_mode, value.st_size,
            value.st_mtime_ns, value.st_ctime_ns)


def generation(value: os.stat_result) -> Tuple[int, int]:
    return value.st_dev, value.st_ino


def proof(value: os.stat_result) -> Dict[str, Any]:
    return {
        "dev": str(value.st_dev),
        "ino": str(value.st_ino),
        "mode": value.st_mode,
        "size": value.st_size,
        "mtimeNs": str(value.st_mtime_ns),
        "ctimeNs": str(value.st_ctime_ns),
    }


def validate_proof(value: Any) -> Dict[str, Any]:
    if not isinstance(value, dict) or set(value) != PROOF_FIELDS:
        fail("MANIFEST_INVALID", "exact stat proof fields are invalid")
    for field in ("dev", "ino", "mtimeNs", "ctimeNs"):
        if not isinstance(value[field], str) or not DECIMAL_RE.fullmatch(value[field]):
            fail("MANIFEST_INVALID", "exact stat proof decimal fields are invalid")
    for field in ("mode", "size"):
        if (not isinstance(value[field], int) or isinstance(value[field], bool) or
                value[field] < 0):
            fail("MANIFEST_INVALID", "exact stat proof numeric fields are invalid")
    return value


def same_proof(value: os.stat_result, expected: Dict[str, Any]) -> bool:
    current = proof(value)
    return all(str(current[key]) == str(expected[key]) for key in PROOF_FIELDS)


def canonical_bytes(value: Any) -> bytes:
    return (json.dumps(value, sort_keys=True, ensure_ascii=True,
                       separators=(",", ":")) + "\n").encode("utf-8")


def digest(value: bytes) -> str:
    return "sha256:" + hashlib.sha256(value).hexdigest()


def directory_flags() -> int:
    return os.O_RDONLY | getattr(os, "O_DIRECTORY", 0) | getattr(os, "O_NOFOLLOW", 0)


def within(authority: Path, target: Path) -> bool:
    try:
        target.relative_to(authority)
        return True
    except ValueError:
        return False


class Chain:
    def __init__(self, target: Path, authority: Path, create: bool = False):
        self.target = target
        self.authority = authority
        self.items: List[Dict[str, Any]] = []
        if not target.is_absolute() or not authority.is_absolute() or not within(authority, target):
            fail("PATH_OUTSIDE_AUTHORITY", "path escapes its declared authority")
        relative = target.relative_to(authority)
        if len(relative.parts) > MAX_COMPONENTS or any(part in ("", ".", "..") for part in relative.parts):
            fail("PATH_UNSAFE", "path has unsafe components")
        try:
            before = authority.lstat()
            if stat.S_ISLNK(before.st_mode) or not stat.S_ISDIR(before.st_mode):
                fail("DIRECTORY_UNSAFE", "authority is not a real directory")
            root_fd = os.open(authority, directory_flags())
            opened = os.fstat(root_fd)
            if identity(before) != identity(opened):
                os.close(root_fd)
                fail("DIRECTORY_CHANGED", "authority changed while opening")
            self.items.append({"fd": root_fd, "opened": opened, "name": None,
                               "path": authority})
            current = authority
            for component in relative.parts:
                parent_fd = self.items[-1]["fd"]
                current = current / component
                try:
                    child = os.stat(component, dir_fd=parent_fd, follow_symlinks=False)
                except FileNotFoundError:
                    if not create:
                        fail("PATH_MISSING", "directory path is missing")
                    try:
                        os.mkdir(component, 0o700, dir_fd=parent_fd)
                        os.fsync(parent_fd)
                        self.items[-1]["opened"] = os.fstat(parent_fd)
                    except FileExistsError:
                        pass
                    child = os.stat(component, dir_fd=parent_fd, follow_symlinks=False)
                if stat.S_ISLNK(child.st_mode) or not stat.S_ISDIR(child.st_mode):
                    fail("DIRECTORY_UNSAFE", "directory component is not real")
                child_fd = os.open(component, directory_flags(), dir_fd=parent_fd)
                child_opened = os.fstat(child_fd)
                if identity(child) != identity(child_opened):
                    os.close(child_fd)
                    fail("DIRECTORY_CHANGED", "directory component changed while opening")
                self.items.append({"fd": child_fd, "opened": child_opened,
                                   "name": component, "path": current})
            self.verify()
        except BaseException:
            self.close()
            raise

    @property
    def fd(self) -> int:
        return self.items[-1]["fd"]

    def refresh(self) -> None:
        for item in self.items:
            item["opened"] = os.fstat(item["fd"])

    def verify(self) -> None:
        for index, item in enumerate(self.items):
            opened = os.fstat(item["fd"])
            if not stat.S_ISDIR(opened.st_mode) or identity(opened) != identity(item["opened"]):
                fail("DIRECTORY_CHANGED", "a pinned directory generation changed")
            try:
                live = (item["path"].lstat() if index == 0 else
                        os.stat(item["name"], dir_fd=self.items[index - 1]["fd"],
                                follow_symlinks=False))
            except FileNotFoundError:
                fail("DIRECTORY_CHANGED", "a pinned directory component disappeared")
            if (stat.S_ISLNK(live.st_mode) or not stat.S_ISDIR(live.st_mode) or
                    identity(live) != identity(opened)):
                fail("DIRECTORY_CHANGED", "a path no longer resolves to its pinned generation")

    def close(self) -> None:
        for item in reversed(self.items):
            try:
                os.close(item["fd"])
            except OSError:
                pass
        self.items = []


def rename_noreplace(source_fd: int, source: str, target_fd: int, target: str) -> None:
    libc = ctypes.CDLL(None, use_errno=True)
    source_bytes, target_bytes = os.fsencode(source), os.fsencode(target)
    if sys.platform.startswith("linux") and hasattr(libc, "renameat2"):
        operation = libc.renameat2
        operation.argtypes = [ctypes.c_int, ctypes.c_char_p, ctypes.c_int,
                              ctypes.c_char_p, ctypes.c_uint]
        operation.restype = ctypes.c_int
        result = operation(source_fd, source_bytes, target_fd, target_bytes, 1)
    elif sys.platform == "darwin" and hasattr(libc, "renameatx_np"):
        operation = libc.renameatx_np
        operation.argtypes = [ctypes.c_int, ctypes.c_char_p, ctypes.c_int,
                              ctypes.c_char_p, ctypes.c_uint]
        operation.restype = ctypes.c_int
        result = operation(source_fd, source_bytes, target_fd, target_bytes, 0x00000004)
    else:
        fail("NO_CLOBBER_UNSUPPORTED", "atomic no-clobber rename is unavailable")
    if result == 0:
        return
    code = ctypes.get_errno()
    if code in (errno.EEXIST, errno.ENOTEMPTY):
        raise FileExistsError(code, os.strerror(code), target)
    if code == errno.ENOENT:
        raise FileNotFoundError(code, os.strerror(code), source)
    if code == errno.ENOSYS:
        fail("NO_CLOBBER_UNSUPPORTED", "atomic no-clobber rename is unavailable")
    raise OSError(code, os.strerror(code), source)


def read_regular(parent_fd: int, name: str, maximum: int) -> Dict[str, Any]:
    try:
        before = os.stat(name, dir_fd=parent_fd, follow_symlinks=False)
    except FileNotFoundError:
        fail("ENTRY_MISSING", "required file is missing")
    if stat.S_ISLNK(before.st_mode) or not stat.S_ISREG(before.st_mode):
        fail("ENTRY_UNSAFE", "entry is not a real regular file")
    if before.st_size > maximum:
        fail("ENTRY_TOO_LARGE", "entry exceeds its byte bound")
    fd: Optional[int] = None
    try:
        fd = os.open(name, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0), dir_fd=parent_fd)
        opened = os.fstat(fd)
        if identity(opened) != identity(before):
            fail("ENTRY_CHANGED", "entry changed while opening")
        chunks: List[bytes] = []
        total = 0
        while True:
            chunk = os.read(fd, min(65536, maximum + 1 - total))
            if not chunk:
                break
            total += len(chunk)
            if total > maximum:
                fail("ENTRY_TOO_LARGE", "entry grew beyond its byte bound")
            chunks.append(chunk)
        data = b"".join(chunks)
        after_fd = os.fstat(fd)
        after_name = os.stat(name, dir_fd=parent_fd, follow_symlinks=False)
        if identity(after_fd) != identity(opened) or identity(after_name) != identity(opened):
            fail("ENTRY_CHANGED", "entry changed while reading")
        return {"proof": proof(opened), "hash": digest(data), "bytes": data}
    finally:
        if fd is not None:
            os.close(fd)


def optional_snapshot(parent_fd: int, name: str, maximum: int) -> Optional[Dict[str, Any]]:
    try:
        os.stat(name, dir_fd=parent_fd, follow_symlinks=False)
    except FileNotFoundError:
        return None
    return read_regular(parent_fd, name, maximum)


def public_snapshot(value: Optional[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
    if value is None:
        return None
    return {"proof": value["proof"], "hash": value["hash"]}


def write_exclusive(parent_fd: int, name: str, data: bytes, mode: int = 0o600) -> Dict[str, Any]:
    fd: Optional[int] = None
    try:
        fd = os.open(name, os.O_WRONLY | os.O_CREAT | os.O_EXCL |
                     getattr(os, "O_NOFOLLOW", 0), mode, dir_fd=parent_fd)
        # os.open applies the process umask.  Publication modes are part of the
        # artifact contract, so set them explicitly before the durable fsync.
        os.fchmod(fd, mode)
        offset = 0
        while offset < len(data):
            written = os.write(fd, data[offset:])
            if written <= 0:
                fail("WRITE_INCOMPLETE", "durable write made no progress")
            offset += written
        os.fsync(fd)
        opened = os.fstat(fd)
        live = os.stat(name, dir_fd=parent_fd, follow_symlinks=False)
        if identity(opened) != identity(live):
            fail("ENTRY_CHANGED", "new durable entry changed before publication")
        return {"proof": proof(opened), "hash": digest(data)}
    finally:
        if fd is not None:
            os.close(fd)


def manifest_value(kind: str, operation_id: str, target_name: str,
                   candidate: bytes, expected: Optional[Dict[str, Any]]) -> Dict[str, Any]:
    value = {
        "version": 1,
        "kind": kind,
        "operationId": operation_id,
        "targetName": target_name,
        "candidateHash": digest(candidate),
        "candidateSize": len(candidate),
        "expected": expected,
    }
    validate_manifest(value, kind, target_name)
    return value


def validate_manifest(value: Any, kind: str, target_name: str) -> Dict[str, Any]:
    if (not isinstance(value, dict) or set(value) != MANIFEST_FIELDS or
            value.get("version") != 1 or value.get("kind") != kind or
            value.get("targetName") != target_name or
            not isinstance(value.get("operationId"), str) or
            not TOKEN_RE.fullmatch(value["operationId"]) or
            not isinstance(value.get("candidateSize"), int) or
            isinstance(value.get("candidateSize"), bool) or
            value["candidateSize"] < 0 or
            not isinstance(value.get("candidateHash"), str) or
            not HASH_RE.fullmatch(value["candidateHash"])):
        fail("MANIFEST_INVALID", "durable INDEX manifest is invalid")
    expected = value.get("expected")
    if expected is not None:
        if (not isinstance(expected, dict) or set(expected) != {"proof", "hash"} or
                not isinstance(expected.get("hash"), str) or
                not HASH_RE.fullmatch(expected["hash"])):
            fail("MANIFEST_INVALID", "durable INDEX expected generation is invalid")
        validate_proof(expected.get("proof"))
    return value


class CasStore:
    def __init__(self, chain: Chain, kind: str, target_name: str, active_prefix: str,
                 committed_prefix: str, evidence_prefix: str, maximum: int, fixture: bool,
                 canonical_root: Path):
        self.chain = chain
        self.kind = kind
        self.target_name = target_name
        self.active_prefix = active_prefix
        self.committed_prefix = committed_prefix
        self.evidence_prefix = evidence_prefix
        self.maximum = maximum
        self.fixture = fixture
        self.canonical_root = canonical_root
        self.current: Optional[str] = None
        self.admitted_destination: Optional[Dict[str, Any]] = None
        self.admission_bound = False

    @property
    def fd(self) -> int:
        return self.chain.fd

    def _active_name(self, token: str) -> str:
        return self.active_prefix + token

    def _evidence_name(self, token: str) -> str:
        return self.evidence_prefix + token

    def _committed_name(self, token: str) -> str:
        return self.committed_prefix + token

    def _classify(self, name: str) -> Optional[str]:
        if name.startswith(self.active_prefix):
            token = name[len(self.active_prefix):]
            return "active" if TOKEN_RE.fullmatch(token) else "unsafe"
        if name.startswith(self.committed_prefix):
            token = name[len(self.committed_prefix):]
            return "committed" if TOKEN_RE.fullmatch(token) else "unsafe"
        if name.startswith(self.evidence_prefix):
            token = name[len(self.evidence_prefix):]
            return "evidence" if TOKEN_RE.fullmatch(token) else "unsafe"
        # Any lookalike within this store's namespace is fail-closed.
        namespace = self.active_prefix.split("cas-")[0]
        if name.startswith(namespace):
            return "unsafe"
        return None

    def inventory(self) -> Dict[str, List[str]]:
        self.chain.verify()
        opened = os.fstat(self.fd)
        names: List[str] = []
        with os.scandir(self.fd) as iterator:
            for entry in iterator:
                names.append(entry.name)
                if len(names) > MAX_DIRECTORY_ENTRIES:
                    fail("DIRECTORY_ENTRY_LIMIT", "publication directory exceeds its bounded entry limit")
        names.sort()
        after = os.fstat(self.fd)
        if identity(after) != identity(opened):
            fail("DIRECTORY_CHANGED", "publication directory changed during inventory")
        self.chain.verify()
        result = {"active": [], "committed": [], "evidence": []}
        for name in names:
            category = self._classify(name)
            if category == "unsafe":
                fail("RECOVERY_NAME_UNSAFE", "malformed hidden INDEX recovery name")
            if category:
                result[category].append(name)
        if (len(result["active"]) + len(result["committed"]) > MAX_ACTIVE or
                len(result["evidence"]) > MAX_EVIDENCE):
            fail("RECOVERY_LIMIT", "bounded INDEX recovery inventory was exceeded")
        return result

    def _open_operation(self, name: str) -> Tuple[int, os.stat_result]:
        before = os.stat(name, dir_fd=self.fd, follow_symlinks=False)
        if stat.S_ISLNK(before.st_mode) or not stat.S_ISDIR(before.st_mode):
            fail("RECOVERY_UNSAFE", "INDEX recovery entry is not a real directory")
        op_fd = os.open(name, directory_flags(), dir_fd=self.fd)
        opened = os.fstat(op_fd)
        live = os.stat(name, dir_fd=self.fd, follow_symlinks=False)
        if identity(before) != identity(opened) or identity(opened) != identity(live):
            os.close(op_fd)
            fail("RECOVERY_CHANGED", "INDEX recovery directory changed while opening")
        return op_fd, opened

    def _entries(self, op_fd: int) -> List[str]:
        opened = os.fstat(op_fd)
        names: List[str] = []
        with os.scandir(op_fd) as iterator:
            for entry in iterator:
                names.append(entry.name)
                if len(names) > 5:
                    fail("RECOVERY_ARTIFACT_UNSAFE", "INDEX recovery artifacts exceed their bound")
        names.sort()
        if identity(os.fstat(op_fd)) != identity(opened):
            fail("RECOVERY_CHANGED", "INDEX recovery directory changed during inventory")
        allowed = {"manifest.json", "candidate", "source", "published", "commit.json"}
        if len(names) > len(allowed) or any(name not in allowed for name in names):
            fail("RECOVERY_ARTIFACT_UNSAFE", "INDEX recovery artifacts are malformed or unbounded")
        for name in names:
            entry = os.stat(name, dir_fd=op_fd, follow_symlinks=False)
            if stat.S_ISLNK(entry.st_mode) or not stat.S_ISREG(entry.st_mode):
                fail("RECOVERY_ARTIFACT_UNSAFE", "INDEX recovery artifact is not a real file")
            maximum = MAX_MANIFEST_BYTES if name in ("manifest.json", "commit.json") else self.maximum
            if entry.st_size > maximum:
                fail("RECOVERY_ARTIFACT_TOO_LARGE", "INDEX recovery artifact exceeds its bound")
        return names

    def _operation_token(self, name: str) -> str:
        for prefix in (self.active_prefix, self.committed_prefix, self.evidence_prefix):
            if name.startswith(prefix):
                token = name[len(prefix):]
                if TOKEN_RE.fullmatch(token):
                    return token
        fail("RECOVERY_NAME_UNSAFE", "recovery directory has no canonical operation token")

    def _load_manifest(self, op_fd: int, names: List[str], operation_name: str) -> Optional[Dict[str, Any]]:
        if "manifest.json" not in names:
            if names:
                fail("RECOVERY_MANIFEST_MISSING", "INDEX recovery artifacts have no manifest")
            return None
        row = read_regular(op_fd, "manifest.json", MAX_MANIFEST_BYTES)
        try:
            value = json.loads(row["bytes"].decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            fail("MANIFEST_INVALID", "durable INDEX manifest is not canonical JSON")
        validate_manifest(value, self.kind, self.target_name)
        if value["operationId"] != self._operation_token(operation_name):
            fail("MANIFEST_INVALID", "durable INDEX manifest is not bound to its recovery directory")
        if (value["candidateSize"] > self.maximum or
                (value["expected"] is not None and value["expected"]["proof"]["size"] > self.maximum)):
            fail("MANIFEST_INVALID", "durable INDEX manifest exceeds its store byte bound")
        if row["bytes"] != canonical_bytes(value):
            fail("MANIFEST_INVALID", "durable INDEX manifest bytes are not canonical")
        return value

    def _load_commit(self, op_fd: int, names: List[str],
                     manifest: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        if "commit.json" not in names:
            return None
        row = read_regular(op_fd, "commit.json", MAX_MANIFEST_BYTES)
        try:
            value = json.loads(row["bytes"].decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            fail("RECOVERY_COMMIT_INVALID", "INDEX recovery commit marker is not canonical JSON")
        if (not isinstance(value, dict) or set(value) != COMMIT_FIELDS or
                value.get("version") != 1 or
                value.get("candidateHash") != manifest["candidateHash"]):
            fail("RECOVERY_COMMIT_INVALID", "INDEX recovery commit marker is invalid")
        validate_proof(value.get("candidateProof"))
        if row["bytes"] != canonical_bytes(value):
            fail("RECOVERY_COMMIT_INVALID", "INDEX recovery commit marker bytes are not canonical")
        return value

    def _matches(self, row: Optional[Dict[str, Any]], expected: Optional[Dict[str, Any]]) -> bool:
        if row is None or expected is None:
            return row is None and expected is None
        return row["proof"] == expected["proof"] and row["hash"] == expected["hash"]

    def _matches_moved(self, row: Optional[Dict[str, Any]], expected: Optional[Dict[str, Any]]) -> bool:
        """Match a renamed/hard-linked generation whose ctime legitimately changed."""
        if row is None or expected is None or row["hash"] != expected["hash"]:
            return row is None and expected is None
        return all(str(row["proof"][field]) == str(expected["proof"][field])
                   for field in ("dev", "ino", "mode", "size", "mtimeNs"))

    def _unlink_exact(self, op_fd: int, name: str, maximum: int) -> None:
        row = read_regular(op_fd, name, maximum)
        live = os.stat(name, dir_fd=op_fd, follow_symlinks=False)
        if not same_proof(live, row["proof"]):
            fail("RECOVERY_CHANGED", "recovery artifact changed before authenticated cleanup")
        os.unlink(name, dir_fd=op_fd)
        os.fsync(op_fd)

    def _cleanup(self, name: str, op_fd: int, opened: os.stat_result,
                 committed: bool = False) -> None:
        names = self._entries(op_fd)
        for artifact in ("published", "source", "candidate", "commit.json", "manifest.json"):
            if artifact in names:
                self._unlink_exact(op_fd, artifact,
                                   MAX_MANIFEST_BYTES if artifact in ("manifest.json", "commit.json") else self.maximum)
                if committed:
                    self._crash("cleanup-after-" + artifact.replace(".json", ""))
        if self._entries(op_fd):
            fail("RECOVERY_ARTIFACT_UNSAFE", "recovery directory retained unknown artifacts")
        current = os.stat(name, dir_fd=self.fd, follow_symlinks=False)
        if generation(current) != generation(opened) or generation(os.fstat(op_fd)) != generation(opened):
            fail("RECOVERY_CHANGED", "recovery directory changed before cleanup")
        os.rmdir(name, dir_fd=self.fd)
        os.fsync(self.fd)
        self.chain.refresh()

    def _quarantine(self, name: str, opened: os.stat_result) -> str:
        token = self._operation_token(name)
        target = self._evidence_name(token)
        rename_noreplace(self.fd, name, self.fd, target)
        os.fsync(self.fd)
        self.chain.refresh()
        moved = os.stat(target, dir_fd=self.fd, follow_symlinks=False)
        if generation(moved) != generation(opened):
            fail("RECOVERY_CHANGED", "a different generation reached evidence quarantine")
        return target

    def _restore_source(self, op_fd: int, expected: Dict[str, Any]) -> bool:
        try:
            source = read_regular(op_fd, "source", self.maximum)
        except BoundaryError as error:
            if error.code == "ENTRY_MISSING":
                return False
            raise
        if not self._matches_moved(source, expected):
            return False
        try:
            rename_noreplace(op_fd, "source", self.fd, self.target_name)
        except FileExistsError:
            return False
        os.fsync(op_fd)
        os.fsync(self.fd)
        self.chain.refresh()
        target = optional_snapshot(self.fd, self.target_name, self.maximum)
        if not self._matches_moved(target, expected):
            fail("RECOVERY_CHANGED", "restored incumbent differs from its durable proof")
        return True

    def _restore_any_source(self, op_fd: int) -> bool:
        try:
            source = read_regular(op_fd, "source", self.maximum)
        except BoundaryError as error:
            if error.code == "ENTRY_MISSING":
                return False
            raise
        try:
            rename_noreplace(op_fd, "source", self.fd, self.target_name)
        except FileExistsError:
            return False
        os.fsync(op_fd)
        os.fsync(self.fd)
        self.chain.refresh()
        target = read_regular(self.fd, self.target_name, self.maximum)
        if not self._matches_moved(target, source):
            fail("RECOVERY_CHANGED", "foreign destination changed while being preserved")
        return True

    def _restore_named_target(self, op_fd: int, name: str,
                              expected: Dict[str, Any]) -> bool:
        """Put an unexpectedly detached target back without clobbering.

        This is used only after a conditional rollback discovers that the
        generation moved out of the public name was not our candidate.  The
        exact pre-rename snapshot prevents a same-bytes foreign inode from
        being mistaken for transaction-owned cleanup material.
        """
        try:
            current = read_regular(op_fd, name, self.maximum)
        except BoundaryError as error:
            if error.code == "ENTRY_MISSING":
                return False
            raise
        if not self._matches(current, expected):
            return False
        try:
            rename_noreplace(op_fd, name, self.fd, self.target_name)
        except FileExistsError:
            return False
        os.fsync(op_fd)
        os.fsync(self.fd)
        self.chain.refresh()
        target = read_regular(self.fd, self.target_name, self.maximum)
        if not self._matches_moved(target, expected):
            fail("RECOVERY_CHANGED", "foreign destination changed while being restored")
        return True

    def _move_target_into(self, op_fd: int, name: str) -> Optional[Dict[str, Any]]:
        try:
            rename_noreplace(self.fd, self.target_name, op_fd, name)
        except FileNotFoundError:
            return None
        except FileExistsError:
            fail("RECOVERY_ARTIFACT_UNSAFE", "recovery destination unexpectedly exists")
        os.fsync(self.fd)
        os.fsync(op_fd)
        self.chain.refresh()
        return read_regular(op_fd, name, self.maximum)

    def _recover_one(self, name: str, category: str = "active") -> str:
        op_fd, opened = self._open_operation(name)
        try:
            names = self._entries(op_fd)
            if category == "committed":
                # The active -> committed directory rename is the durable
                # decision.  Before deleting rollback authority, reprove that
                # the public name still resolves to the exact committed inode.
                # Late cleanup states are deliberately narrow and ordered.
                if not names:
                    self._cleanup(name, op_fd, opened, committed=True)
                    return "completed-commit-cleanup"
                manifest = self._load_manifest(op_fd, names, name)
                assert manifest is not None
                marker = self._load_commit(op_fd, names, manifest)
                if marker is None:
                    if names != ["manifest.json"]:
                        fail("RECOVERY_COMMIT_INVALID", "committed INDEX cleanup state is impossible")
                    self._cleanup(name, op_fd, opened, committed=True)
                    return "completed-commit-cleanup"
                if "published" in names or ("source" in names and "candidate" not in names):
                    fail("RECOVERY_COMMIT_INVALID", "committed INDEX artifacts violate cleanup order")
                committed_generation = {
                    "hash": marker["candidateHash"],
                    "proof": marker["candidateProof"],
                }
                target = optional_snapshot(self.fd, self.target_name, self.maximum)
                if not self._matches_moved(target, committed_generation):
                    self._quarantine(name, opened)
                    return "retained-committed-evidence"
                if "candidate" in names:
                    candidate = read_regular(op_fd, "candidate", self.maximum)
                    if (candidate["hash"] != manifest["candidateHash"] or
                            candidate["proof"]["size"] != manifest["candidateSize"] or
                            not self._matches_moved(candidate, committed_generation)):
                        self._quarantine(name, opened)
                        return "retained-committed-candidate-evidence"
                if "source" in names:
                    source = read_regular(op_fd, "source", self.maximum)
                    if (manifest["expected"] is None or
                            not self._matches_moved(source, manifest["expected"])):
                        self._quarantine(name, opened)
                        return "retained-committed-source-evidence"
                self._cleanup(name, op_fd, opened, committed=True)
                return "completed-commit-cleanup"
            manifest = self._load_manifest(op_fd, names, name)
            if manifest is None:
                self._cleanup(name, op_fd, opened)
                return "discarded-empty"
            expected = manifest["expected"]
            candidate = None
            if "candidate" in names:
                candidate = read_regular(op_fd, "candidate", self.maximum)
                if (candidate["hash"] != manifest["candidateHash"] or
                        candidate["proof"]["size"] != manifest["candidateSize"]):
                    self._quarantine(name, opened)
                    return "retained-candidate-evidence"
            # A marker inside an *active* operation is prepared but not
            # committed.  It is validated, then follows the ordinary rollback
            # path.  Only the atomic directory rename changes recovery policy.
            self._load_commit(op_fd, names, manifest)
            target = optional_snapshot(self.fd, self.target_name, self.maximum)
            # Hash/size equality is insufficient: a no-op regeneration can
            # produce bytes identical to the incumbent.  Only the candidate's
            # exact inode lineage proves that the published hard link reached
            # the target name.
            target_is_candidate = (candidate is not None and
                                   self._matches_moved(target, candidate))
            target_is_expected = self._matches_moved(target, expected)

            published = read_regular(op_fd, "published", self.maximum) if "published" in names else None
            if published is not None and (candidate is None or
                                          not self._matches_moved(published, candidate)):
                self._quarantine(name, opened)
                return "retained-published-evidence"

            if candidate is None:
                if target_is_expected:
                    self._cleanup(name, op_fd, opened)
                    return "discarded-staging"
                self._quarantine(name, opened)
                return "retained-staging-evidence"

            if target_is_candidate:
                if published is not None:
                    self._quarantine(name, opened)
                    return "retained-duplicate-publication-evidence"
                moved = self._move_target_into(op_fd, "published")
                if moved is None:
                    self._quarantine(name, opened)
                    return "retained-target-evidence"
                if not self._matches_moved(moved, candidate):
                    # A foreign generation won the tiny window after the
                    # target proof.  Restore it if the public name is still
                    # empty; otherwise retain it inside the evidence WAL.
                    self._restore_named_target(op_fd, "published", moved)
                    self._quarantine(name, opened)
                    return "retained-target-evidence"
                if expected is not None and not self._restore_source(op_fd, expected):
                    self._quarantine(name, opened)
                    return "retained-rollback-evidence"
                self._cleanup(name, op_fd, opened)
                return "rolled-back"

            if target is None and expected is not None and "source" in names:
                if self._restore_source(op_fd, expected):
                    self._cleanup(name, op_fd, opened)
                    return "restored-incumbent"
                if self._restore_any_source(op_fd):
                    self._quarantine(name, opened)
                    return "restored-foreign-evidence"
                self._quarantine(name, opened)
                return "retained-restore-evidence"

            if target_is_expected:
                self._cleanup(name, op_fd, opened)
                return "discarded-pre-publication"

            self._quarantine(name, opened)
            return "retained-foreign-evidence"
        finally:
            os.close(op_fd)

    def inspect(self) -> Dict[str, Any]:
        inventory = self.inventory()
        for name in inventory["active"] + inventory["committed"] + inventory["evidence"]:
            op_fd, _ = self._open_operation(name)
            try:
                names = self._entries(op_fd)
                if name in inventory["active"] or name in inventory["evidence"]:
                    self._load_manifest(op_fd, names, name)
            finally:
                os.close(op_fd)
        target = optional_snapshot(self.fd, self.target_name, self.maximum)
        return {
            "destination": public_snapshot(target),
            "activeRecovery": len(inventory["active"]) + len(inventory["committed"]),
            "retainedEvidence": len(inventory["evidence"]),
        }

    def recover(self) -> Dict[str, Any]:
        inventory = self.inventory()
        recovered: List[str] = []
        for name in inventory["active"]:
            recovered.append(self._recover_one(name))
        for name in inventory["committed"]:
            recovered.append(self._recover_one(name, "committed"))
        after = self.inventory()
        return {"recovered": recovered, "retainedEvidence": len(after["evidence"]),
                "destination": public_snapshot(optional_snapshot(self.fd, self.target_name, self.maximum))}

    def _crash(self, label: str) -> None:
        requested = os.environ.get("TASK_INDEX_TEST_CRASH_AT", "")
        if requested != label:
            return
        fixture_root = os.environ.get("TASK_FS_TEST_ROOT", "")
        if (not self.fixture or not fixture_root or
                Path(os.path.abspath(fixture_root)) == self.canonical_root or
                not within(Path(os.path.abspath(fixture_root)), self.chain.target)):
            fail("TEST_HOOK_INVALID", "INDEX crash hook is disabled outside an isolated fixture")
        os._exit(97)

    def _replace_target_fixture(self) -> None:
        variable = ("TASK_INDEX_TEST_DIAGNOSTIC_REPLACE_TARGET_WITH"
                    if self.kind == "task-index-diagnostic" else
                    "TASK_INDEX_TEST_REPLACE_TARGET_WITH")
        replacement_raw = os.environ.get(variable, "")
        if not replacement_raw:
            return
        replacement = Path(os.path.abspath(replacement_raw))
        fixture_root_raw = os.environ.get("TASK_FS_TEST_ROOT", "")
        fixture_root = Path(os.path.abspath(fixture_root_raw)) if fixture_root_raw else None
        if (not self.fixture or fixture_root is None or fixture_root == self.canonical_root or
                replacement.parent != self.chain.target or not within(fixture_root, replacement)):
            fail("TEST_HOOK_INVALID", "INDEX target replacement hook is disabled outside an isolated fixture")
        token = secrets.token_hex(24)
        preserved = "index-test-incumbent-" + token
        try:
            rename_noreplace(self.fd, self.target_name, self.fd, preserved)
        except FileNotFoundError:
            pass
        rename_noreplace(self.fd, replacement.name, self.fd, self.target_name)
        os.fsync(self.fd)
        self.chain.refresh()

    def _appear_target_fixture(self) -> None:
        variable = ("TASK_INDEX_TEST_DIAGNOSTIC_APPEAR_TARGET_WITH"
                    if self.kind == "task-index-diagnostic" else
                    "TASK_INDEX_TEST_APPEAR_TARGET_WITH")
        replacement_raw = os.environ.get(variable, "")
        if not replacement_raw:
            return
        replacement = Path(os.path.abspath(replacement_raw))
        fixture_root_raw = os.environ.get("TASK_FS_TEST_ROOT", "")
        fixture_root = Path(os.path.abspath(fixture_root_raw)) if fixture_root_raw else None
        if (not self.fixture or fixture_root is None or fixture_root == self.canonical_root or
                replacement.parent != self.chain.target or not within(fixture_root, replacement)):
            fail("TEST_HOOK_INVALID", "INDEX target appearance hook is disabled outside an isolated fixture")
        rename_noreplace(self.fd, replacement.name, self.fd, self.target_name)
        os.fsync(self.fd)
        self.chain.refresh()

    def _replace_published_fixture(self, op_fd: int) -> None:
        variable = ("TASK_INDEX_TEST_DIAGNOSTIC_REPLACE_PUBLISHED_WITH"
                    if self.kind == "task-index-diagnostic" else
                    "TASK_INDEX_TEST_REPLACE_PUBLISHED_WITH")
        replacement_raw = os.environ.get(variable, "")
        if not replacement_raw:
            return
        replacement = Path(os.path.abspath(replacement_raw))
        fixture_root_raw = os.environ.get("TASK_FS_TEST_ROOT", "")
        fixture_root = Path(os.path.abspath(fixture_root_raw)) if fixture_root_raw else None
        if (not self.fixture or fixture_root is None or fixture_root == self.canonical_root or
                replacement.parent != self.chain.target or not within(fixture_root, replacement)):
            fail("TEST_HOOK_INVALID", "published-target replacement hook is disabled outside an isolated fixture")
        rename_noreplace(self.fd, self.target_name, op_fd, "published")
        rename_noreplace(self.fd, replacement.name, self.fd, self.target_name)
        os.fsync(op_fd)
        os.fsync(self.fd)
        self.chain.refresh()

    def stage(self, candidate: bytes, admitted: Any = ...) -> Dict[str, Any]:
        if self.current is not None:
            fail("SESSION_STATE_INVALID", "a publication is already staged")
        if len(candidate) > self.maximum:
            fail("OUTPUT_TOO_LARGE", "publication candidate exceeds its bounded output limit")
        self.chain.verify()
        expected = (self.admitted_destination if admitted is ... else admitted)
        if admitted is ... and not self.admission_bound:
            # Non-index stores admit their generation immediately before the
            # one-shot diagnostic CAS.
            expected = public_snapshot(optional_snapshot(self.fd, self.target_name, self.maximum))
        token = secrets.token_hex(24)
        name = self._active_name(token)
        try:
            os.mkdir(name, 0o700, dir_fd=self.fd)
        except FileExistsError:
            fail("RECOVERY_COLLISION", "durable INDEX operation id collided")
        os.fsync(self.fd)
        self.chain.refresh()
        self._crash("after-mkdir")
        op_fd, opened = self._open_operation(name)
        self.current = name
        try:
            manifest = manifest_value(self.kind, token, self.target_name, candidate, expected)
            write_exclusive(op_fd, "manifest.json", canonical_bytes(manifest))
            os.fsync(op_fd)
            self._crash("after-manifest")
            write_exclusive(op_fd, "candidate", candidate,
                            0o644 if self.kind == "task-index" else 0o600)
            os.fsync(op_fd)
            self._crash("after-candidate")
            self._replace_target_fixture()

            if expected is not None:
                try:
                    rename_noreplace(self.fd, self.target_name, op_fd, "source")
                except FileNotFoundError:
                    self._recover_one(name)
                    self.current = None
                    fail("DESTINATION_CHANGED", "publication destination disappeared before conditional detach")
                os.fsync(self.fd)
                os.fsync(op_fd)
                self.chain.refresh()
                self._crash("after-detach")
                source = read_regular(op_fd, "source", self.maximum)
                if not self._matches_moved(source, expected):
                    self._recover_one(name)
                    self.current = None
                    fail("DESTINATION_CHANGED", "publication destination changed before conditional detach")
            elif optional_snapshot(self.fd, self.target_name, self.maximum) is not None:
                self._recover_one(name)
                self.current = None
                fail("DESTINATION_CHANGED", "publication destination appeared before no-clobber publish")

            self._appear_target_fixture()
            try:
                os.link("candidate", self.target_name, src_dir_fd=op_fd,
                        dst_dir_fd=self.fd, follow_symlinks=False)
            except FileExistsError:
                self._recover_one(name)
                self.current = None
                fail("DESTINATION_CHANGED", "publication destination appeared in the final no-clobber window")
            os.fsync(self.fd)
            self.chain.refresh()
            self._replace_published_fixture(op_fd)
            self._crash("after-publish")
            target = read_regular(self.fd, self.target_name, self.maximum)
            candidate_row = read_regular(op_fd, "candidate", self.maximum)
            if not self._matches_moved(target, candidate_row):
                fail("DESTINATION_CHANGED", "published target differs from its exact candidate")
            return {"operationId": token, "destination": public_snapshot(target),
                    "previous": expected}
        except BaseException:
            if isinstance(sys.exc_info()[1], BoundaryError):
                # Keep the WAL for explicit abort/recovery unless the branch
                # above already reconciled it and cleared current.
                pass
            raise
        finally:
            os.close(op_fd)

    def verify_current(self) -> Dict[str, Any]:
        if self.current is None:
            fail("SESSION_STATE_INVALID", "no staged publication exists")
        op_fd, _ = self._open_operation(self.current)
        try:
            names = self._entries(op_fd)
            manifest = self._load_manifest(op_fd, names, self.current)
            if manifest is None:
                fail("RECOVERY_MANIFEST_MISSING", "staged publication lost its manifest")
            target = read_regular(self.fd, self.target_name, self.maximum)
            candidate = read_regular(op_fd, "candidate", self.maximum)
            if (candidate["hash"] != manifest["candidateHash"] or
                    candidate["proof"]["size"] != manifest["candidateSize"] or
                    not self._matches_moved(target, candidate)):
                fail("DESTINATION_CHANGED", "destination changed after conditional publication")
            self.chain.verify()
            return {"destination": public_snapshot(target)}
        finally:
            os.close(op_fd)

    def commit(self) -> Dict[str, Any]:
        self.verify_current()
        assert self.current is not None
        name = self.current
        op_fd, opened = self._open_operation(name)
        try:
            manifest = self._load_manifest(op_fd, self._entries(op_fd), name)
            assert manifest is not None
            candidate_for_marker = read_regular(op_fd, "candidate", self.maximum)
            if (candidate_for_marker["hash"] != manifest["candidateHash"] or
                    candidate_for_marker["proof"]["size"] != manifest["candidateSize"]):
                fail("DESTINATION_CHANGED", "candidate changed before the durable commit marker")
            marker = canonical_bytes({
                "candidateHash": manifest["candidateHash"],
                "candidateProof": candidate_for_marker["proof"],
                "version": 1,
            })
            write_exclusive(op_fd, "commit.json", marker)
            os.fsync(op_fd)
            self._crash("after-commit-marker")
            committed_name = self._committed_name(manifest["operationId"])
            rename_noreplace(self.fd, name, self.fd, committed_name)
            os.fsync(self.fd)
            self.chain.refresh()
            moved = os.stat(committed_name, dir_fd=self.fd, follow_symlinks=False)
            if generation(moved) != generation(opened):
                fail("RECOVERY_CHANGED", "a different operation reached the durable commit boundary")
            self.current = committed_name
            self._crash("after-commit-rename")
            # Reprove the candidate lineage after the durable decision rename
            # and before any rollback evidence is cleaned.
            target_before_cleanup = read_regular(self.fd, self.target_name, self.maximum)
            candidate_before_cleanup = read_regular(op_fd, "candidate", self.maximum)
            if not self._matches_moved(target_before_cleanup, candidate_before_cleanup):
                self._quarantine(committed_name, opened)
                self.current = None
                fail("DESTINATION_CHANGED", "destination changed at the durable commit boundary")
            self._cleanup(committed_name, op_fd, opened, committed=True)
            self.current = None
            target_after_cleanup = read_regular(self.fd, self.target_name, self.maximum)
            if not self._matches_moved(target_after_cleanup, target_before_cleanup):
                fail("DESTINATION_CHANGED", "destination changed while finalizing the durable commit")
            return {"destination": public_snapshot(target_after_cleanup)}
        finally:
            os.close(op_fd)

    def abort(self) -> Dict[str, Any]:
        if self.current is None:
            return {"recovered": False}
        name = self.current
        category = "committed" if name.startswith(self.committed_prefix) else "active"
        result = self._recover_one(name, category)
        self.current = None
        return {"recovered": True, "result": result,
                "destination": public_snapshot(optional_snapshot(self.fd, self.target_name, self.maximum))}

    def replace_once(self, candidate: bytes) -> Dict[str, Any]:
        staged = self.stage(candidate)
        committed = self.commit()
        return {"staged": staged, "committed": committed}


class Session:
    def __init__(self, request: Dict[str, Any]):
        self.chains: List[Chain] = []
        self.lock_fd: Optional[int] = None
        self.index_store: Optional[CasStore] = None
        self.diagnostic_store: Optional[CasStore] = None
        self.diagnostic_error: Optional[str] = None
        self.closed = False
        self.repo = Path(os.path.abspath(request.get("repoRoot", "")))
        self.tasks = Path(os.path.abspath(request.get("tasksDir", "")))
        self.canonical_root = Path(os.path.abspath(request.get("canonicalRoot", "")))
        self.fixture = request.get("fixture") is True
        if (not request.get("repoRoot") or not request.get("tasksDir") or
                not request.get("canonicalRoot") or not within(self.repo, self.tasks)):
            fail("ARGUMENT_INVALID", "publication roots are invalid")
        if any(os.environ.get(name, "") for name in TEST_HOOK_ENV) and not self.fixture:
            fail("TEST_HOOK_INVALID", "INDEX mutation hooks require isolated-fixture authority")
        timeout_ms = request.get("timeoutMs", 30000)
        if (not isinstance(timeout_ms, int) or isinstance(timeout_ms, bool) or
                timeout_ms < 100 or timeout_ms > 60000):
            fail("ARGUMENT_INVALID", "lock timeout is outside its bound")

        task_chain = Chain(self.tasks, self.repo)
        self.chains.append(task_chain)
        lock_dir = self.repo / "orchestrator" / ".cache" / "tasks"
        lock_chain = Chain(lock_dir, self.repo, create=True)
        self.chains.append(lock_chain)
        self._acquire_lock(lock_chain, timeout_ms)
        self._verify_all()

        self.index_store = CasStore(task_chain, "task-index", "INDEX.json",
                                    ".task-index-cas-", ".task-index-committed-",
                                    ".task-index-evidence-",
                                    MAX_INDEX_BYTES, self.fixture, self.canonical_root)
        index_recovery = self.index_store.recover()
        self.index_store.admitted_destination = index_recovery["destination"]
        self.index_store.admission_bound = True
        self._verify_all()

        diagnostic_path = self.repo / "orchestrator" / ".cache" / "tasks" / "integrity"
        try:
            diagnostic_chain = Chain(diagnostic_path, self.repo, create=True)
            self.chains.append(diagnostic_chain)
            self._refresh_all()
            self.diagnostic_store = CasStore(
                diagnostic_chain, "task-index-diagnostic", "index.json",
                ".task-index-diagnostic-cas-", ".task-index-diagnostic-committed-",
                ".task-index-diagnostic-evidence-",
                MAX_DIAGNOSTIC_BYTES, self.fixture, self.canonical_root)
            self.diagnostic_store.recover()
        except BoundaryError as error:
            self.diagnostic_error = error.code
            self.diagnostic_store = None
        self._verify_all()
        self.ready = {
            "sessionId": secrets.token_hex(24),
            "lock": public_snapshot(optional_snapshot(lock_chain.fd, "index.lock", MAX_MANIFEST_BYTES)),
            "destination": index_recovery["destination"],
            "recovered": index_recovery["recovered"],
            "retainedEvidence": index_recovery["retainedEvidence"],
            "diagnosticAvailable": self.diagnostic_store is not None,
            "diagnosticError": self.diagnostic_error,
        }

    def _refresh_all(self) -> None:
        for chain in self.chains:
            chain.refresh()

    def _verify_all(self) -> None:
        for chain in self.chains:
            chain.verify()
        if self.lock_fd is not None:
            opened = os.fstat(self.lock_fd)
            lock_chain = self.chains[1]
            live = os.stat("index.lock", dir_fd=lock_chain.fd, follow_symlinks=False)
            if (stat.S_ISLNK(opened.st_mode) or not stat.S_ISREG(opened.st_mode) or
                    identity(opened) != identity(live)):
                fail("LOCK_CHANGED", "INDEX lock path no longer resolves to the owned inode")

    def _acquire_lock(self, chain: Chain, timeout_ms: int) -> None:
        flags = os.O_RDWR | os.O_CREAT | getattr(os, "O_NOFOLLOW", 0)
        fd = os.open("index.lock", flags, 0o600, dir_fd=chain.fd)
        os.fsync(chain.fd)
        opened = os.fstat(fd)
        live = os.stat("index.lock", dir_fd=chain.fd, follow_symlinks=False)
        if (stat.S_ISLNK(opened.st_mode) or not stat.S_ISREG(opened.st_mode) or
                identity(opened) != identity(live)):
            os.close(fd)
            fail("LOCK_UNSAFE", "INDEX lock is not a stable regular file")
        chain.refresh()
        deadline = time.monotonic() + timeout_ms / 1000
        while True:
            try:
                if os.name == "nt":
                    if opened.st_size == 0:
                        os.write(fd, b"\0")
                        os.fsync(fd)
                        opened = os.fstat(fd)
                        chain.refresh()
                    os.lseek(fd, 0, os.SEEK_SET)
                    msvcrt.locking(fd, msvcrt.LK_NBLCK, 1)
                else:
                    fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
                break
            except OSError as error:
                if error.errno not in (errno.EACCES, errno.EAGAIN, errno.EDEADLK):
                    os.close(fd)
                    raise
                if time.monotonic() >= deadline:
                    os.close(fd)
                    fail("LOCK_TIMEOUT", "INDEX publication lock timed out")
                time.sleep(0.025)
        self.lock_fd = fd
        self._refresh_all()
        self._verify_all()

    def command(self, request: Dict[str, Any]) -> Dict[str, Any]:
        self._verify_all()
        action = request.get("action")
        if action == "stage":
            assert self.index_store is not None
            data = decode_candidate(request, MAX_INDEX_BYTES)
            result = self.index_store.stage(data)
        elif action == "verify":
            assert self.index_store is not None
            result = self.index_store.verify_current()
        elif action == "commit":
            assert self.index_store is not None
            result = self.index_store.commit()
        elif action == "abort":
            assert self.index_store is not None
            result = self.index_store.abort()
        elif action == "snapshot":
            assert self.index_store is not None
            result = {"destination": public_snapshot(optional_snapshot(
                self.index_store.fd, self.index_store.target_name, self.index_store.maximum))}
        elif action == "diagnostic":
            if self.diagnostic_store is None:
                fail("DIAGNOSTIC_UNAVAILABLE", self.diagnostic_error or "diagnostic boundary is unavailable")
            data = decode_candidate(request, MAX_DIAGNOSTIC_BYTES)
            result = self.diagnostic_store.replace_once(data)
        elif action == "close":
            self.close()
            result = {"closed": True}
        else:
            fail("ARGUMENT_INVALID", "unsupported publication session action")
        if not self.closed:
            self._verify_all()
        return result

    def close(self) -> None:
        if self.closed:
            return
        if self.index_store is not None and self.index_store.current is not None:
            try:
                self.index_store.abort()
            except BaseException:
                pass
        if self.lock_fd is not None:
            try:
                if os.name == "nt":
                    os.lseek(self.lock_fd, 0, os.SEEK_SET)
                    msvcrt.locking(self.lock_fd, msvcrt.LK_UNLCK, 1)
                else:
                    fcntl.flock(self.lock_fd, fcntl.LOCK_UN)
            finally:
                os.close(self.lock_fd)
                self.lock_fd = None
        for chain in reversed(self.chains):
            chain.close()
        self.chains = []
        self.closed = True


def decode_candidate(request: Dict[str, Any], maximum: int) -> bytes:
    raw = request.get("rawBase64")
    if not isinstance(raw, str) or len(raw) > ((maximum + 2) // 3) * 4 + 8:
        fail("ARGUMENT_INVALID", "publication bytes are invalid or oversized")
    try:
        data = base64.b64decode(raw, validate=True)
    except (ValueError, binascii.Error):
        fail("ARGUMENT_INVALID", "publication bytes are invalid")
    if len(data) > maximum:
        fail("OUTPUT_TOO_LARGE", "publication bytes exceed their bound")
    return data


def read_request(line: bytes) -> Dict[str, Any]:
    if len(line) > MAX_REQUEST:
        fail("ARGUMENT_INVALID", "request exceeds its byte bound")
    try:
        value = json.loads(line.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        fail("ARGUMENT_INVALID", "request is not valid JSON")
    if not isinstance(value, dict) or value.get("version") != 1:
        fail("ARGUMENT_INVALID", "request envelope is invalid")
    return value


def envelope(ok: bool, value: Any) -> bytes:
    payload = {"ok": ok, "version": 1}
    payload["result" if ok else "error"] = value
    return canonical_bytes(payload)


def emit(ok: bool, value: Any) -> None:
    sys.stdout.buffer.write(envelope(ok, value))
    sys.stdout.buffer.flush()


def fixture_allowed(request: Dict[str, Any], repo: Path, canonical: Path) -> bool:
    if request.get("fixture") is not True or repo == canonical:
        return False
    fixture_raw = os.environ.get("TASK_FS_TEST_ROOT", "")
    if not fixture_raw:
        return False
    fixture = Path(os.path.abspath(fixture_raw))
    return fixture != canonical and within(fixture, repo)


def inspect_request(request: Dict[str, Any]) -> Dict[str, Any]:
    repo = Path(os.path.abspath(request.get("repoRoot", "")))
    tasks = Path(os.path.abspath(request.get("tasksDir", "")))
    canonical = Path(os.path.abspath(request.get("canonicalRoot", "")))
    if not request.get("repoRoot") or not request.get("tasksDir") or not within(repo, tasks):
        fail("ARGUMENT_INVALID", "inspection roots are invalid")
    chain = Chain(tasks, repo)
    try:
        store = CasStore(chain, "task-index", "INDEX.json", ".task-index-cas-",
                         ".task-index-committed-", ".task-index-evidence-", MAX_INDEX_BYTES,
                         fixture_allowed(request, repo, canonical), canonical)
        return store.inspect()
    finally:
        chain.close()


def main() -> int:
    session: Optional[Session] = None
    try:
        first = sys.stdin.buffer.readline(MAX_REQUEST + 2)
        if not first:
            return 0
        try:
            request = read_request(first)
            if request.get("action") == "inspect":
                emit(True, inspect_request(request))
                return 0
            if request.get("action") != "open":
                fail("ARGUMENT_INVALID", "first action must open or inspect")
            session = Session(request)
            emit(True, session.ready)
        except BoundaryError as error:
            emit(False, {"code": error.code, "message": str(error)})
            return 0
        while session is not None and not session.closed:
            line = sys.stdin.buffer.readline(MAX_REQUEST + 2)
            if not line:
                break
            try:
                request = read_request(line)
                emit(True, session.command(request))
            except BoundaryError as error:
                emit(False, {"code": error.code, "message": str(error)})
            except BaseException as error:
                emit(False, {"code": "BOUNDARY_FAILED",
                             "message": (type(error).__name__ + ": " + str(error))[:1000]})
        return 0
    finally:
        if session is not None:
            session.close()


if __name__ == "__main__":
    raise SystemExit(main())
