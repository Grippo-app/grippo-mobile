#!/usr/bin/env python3
"""Small openat filesystem boundary for task-state and task-lock.

JavaScript has no portable openat API.  This helper keeps every directory
descriptor in an absolute path pinned, rejects symlink components, and returns
only JSON-safe exact stat proofs (decimal dev/ino and nanosecond timestamps).
It is intentionally policy-free: callers own task/lock schemas and recovery.
"""

from __future__ import annotations

import base64
import binascii
import ctypes
import errno
import json
import os
import re
import secrets
import stat
import sys
import unicodedata
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


MAX_REQUEST = 16 * 1024 * 1024
MAX_PATH_PARTS = 96


class BoundaryError(Exception):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


def fail(code: str, message: str) -> None:
    raise BoundaryError(code, str(message)[:1200])


def identity(st: os.stat_result) -> Tuple[int, int, int, int, int, int]:
    return (st.st_dev, st.st_ino, st.st_mode, st.st_size,
            st.st_mtime_ns, st.st_ctime_ns)


def generation(st: os.stat_result) -> Tuple[int, int]:
    return (st.st_dev, st.st_ino)


def kind(st: os.stat_result) -> str:
    if stat.S_ISREG(st.st_mode):
        return "file"
    if stat.S_ISDIR(st.st_mode):
        return "directory"
    if stat.S_ISLNK(st.st_mode):
        return "symlink"
    return "other"


def proof(st: os.stat_result) -> Dict[str, Any]:
    return {
        "dev": str(st.st_dev),
        "ino": str(st.st_ino),
        "mode": st.st_mode,
        "size": st.st_size,
        "mtimeNs": str(st.st_mtime_ns),
        "ctimeNs": str(st.st_ctime_ns),
        "kind": kind(st),
    }


def parse_expected(value: Any, label: str) -> Optional[Tuple[int, int]]:
    if value is None:
        return None
    if not isinstance(value, dict):
        fail("ARGUMENT_INVALID", f"{label} proof is not an object")
    try:
        dev = int(value["dev"])
        ino = int(value["ino"])
    except (KeyError, TypeError, ValueError):
        fail("ARGUMENT_INVALID", f"{label} proof is malformed")
    if dev < 0 or ino < 0 or str(dev) != str(value["dev"]) or str(ino) != str(value["ino"]):
        fail("ARGUMENT_INVALID", f"{label} proof is non-canonical")
    return (dev, ino)


def directory_flags() -> int:
    return os.O_RDONLY | getattr(os, "O_DIRECTORY", 0) | getattr(os, "O_NOFOLLOW", 0)


def verify_chain(chain: List[Dict[str, Any]], allow_leaf_metadata_drift: bool = False) -> None:
    for index, item in enumerate(chain):
        opened = os.fstat(item["fd"])
        leaf = allow_leaf_metadata_drift and index == len(chain) - 1
        same_opened = (generation(opened) == generation(item["opened"])) if leaf else (
            identity(opened) == identity(item["opened"]))
        if not stat.S_ISDIR(opened.st_mode) or not same_opened:
            fail("DIRECTORY_CHANGED", "a pinned directory generation changed")
        try:
            if index == 0:
                live = item["path"].lstat()
            else:
                live = os.stat(item["name"], dir_fd=chain[index - 1]["fd"], follow_symlinks=False)
        except FileNotFoundError:
            fail("DIRECTORY_CHANGED", "a pinned directory component disappeared")
        same_live = generation(live) == generation(opened) if leaf else identity(live) == identity(opened)
        if stat.S_ISLNK(live.st_mode) or not stat.S_ISDIR(live.st_mode) or not same_live:
            fail("DIRECTORY_CHANGED", "a path component no longer resolves to its pinned generation")


def verify_target(chain: List[Dict[str, Any]], target: Path,
                  allow_leaf_metadata_drift: bool = False) -> None:
    verify_chain(chain, allow_leaf_metadata_drift)
    try:
        live = target.lstat()
    except FileNotFoundError:
        fail("DIRECTORY_CHANGED", "the canonical directory path disappeared")
    opened = os.fstat(chain[-1]["fd"])
    same_live = generation(live) == generation(opened) if allow_leaf_metadata_drift else identity(live) == identity(opened)
    if stat.S_ISLNK(live.st_mode) or not stat.S_ISDIR(live.st_mode) or not same_live:
        fail("DIRECTORY_CHANGED", "the canonical path no longer resolves to the pinned directory")
    verify_chain(chain, allow_leaf_metadata_drift)


def open_dir(target: Path, authority: Path, create: bool = False) -> Tuple[List[Dict[str, Any]], int]:
    try:
        relative = target.relative_to(authority)
    except ValueError:
        fail("PATH_OUTSIDE_AUTHORITY", "path escapes its declared authority root")
    parts = relative.parts
    if len(parts) > MAX_PATH_PARTS or any(part in ("", ".", "..") for part in parts):
        fail("PATH_UNSAFE", "path has unsafe components")
    chain: List[Dict[str, Any]] = []
    try:
        before = authority.lstat()
        if stat.S_ISLNK(before.st_mode) or not stat.S_ISDIR(before.st_mode):
            fail("DIRECTORY_UNSAFE", "authority root is not a real directory")
        root_fd = os.open(authority, directory_flags())
        opened = os.fstat(root_fd)
        if identity(before) != identity(opened):
            os.close(root_fd)
            fail("DIRECTORY_CHANGED", "authority root changed while opening")
        chain.append({"fd": root_fd, "opened": opened, "path": authority, "name": None})
        current = authority
        for part in parts:
            parent_fd = chain[-1]["fd"]
            current = current / part
            try:
                child_before = os.stat(part, dir_fd=parent_fd, follow_symlinks=False)
            except FileNotFoundError:
                if not create:
                    fail("PATH_MISSING", "directory path is missing")
                try:
                    os.mkdir(part, 0o700, dir_fd=parent_fd)
                    os.fsync(parent_fd)
                    chain[-1]["opened"] = os.fstat(parent_fd)
                except FileExistsError:
                    pass
                child_before = os.stat(part, dir_fd=parent_fd, follow_symlinks=False)
            if stat.S_ISLNK(child_before.st_mode) or not stat.S_ISDIR(child_before.st_mode):
                fail("DIRECTORY_UNSAFE", "directory component is not a real directory")
            child_fd = os.open(part, directory_flags(), dir_fd=parent_fd)
            child_opened = os.fstat(child_fd)
            if identity(child_before) != identity(child_opened):
                os.close(child_fd)
                fail("DIRECTORY_CHANGED", "directory component changed while opening")
            chain.append({"fd": child_fd, "opened": child_opened,
                          "path": current, "name": part})
        verify_target(chain, target)
        return chain, chain[-1]["fd"]
    except BaseException:
        for item in reversed(chain):
            try:
                os.close(item["fd"])
            except OSError:
                pass
        raise


def close_chain(chain: List[Dict[str, Any]]) -> None:
    for item in reversed(chain):
        try:
            os.close(item["fd"])
        except OSError:
            pass


def refresh_generation(chains: List[List[Dict[str, Any]]], directory_fd: int) -> None:
    current = os.fstat(directory_fd)
    current_generation = generation(current)
    for chain in chains:
        for item in chain:
            if generation(os.fstat(item["fd"])) == current_generation:
                item["opened"] = os.fstat(item["fd"])


def fixture_swap(target: Path, opened_fd: int, request: Dict[str, Any]) -> Optional[Dict[str, Path]]:
    swap_target_raw = os.environ.get("TASK_FS_TEST_SWAP_PATH", "")
    swap_with_raw = os.environ.get("TASK_FS_TEST_SWAP_WITH", "")
    fixture_root_raw = os.environ.get("TASK_FS_TEST_ROOT", "")
    if not swap_target_raw and not swap_with_raw:
        return None
    if not swap_target_raw or not swap_with_raw or not fixture_root_raw:
        fail("TEST_HOOK_INVALID", "swap hook requires target, replacement, and fixture root")
    swap_target = Path(os.path.abspath(swap_target_raw))
    swap_with = Path(os.path.abspath(swap_with_raw))
    fixture_root = Path(os.path.abspath(fixture_root_raw))
    canonical_root = Path(os.path.abspath(request.get("canonicalRoot", "")))
    if target != swap_target:
        return None
    try:
        target.relative_to(fixture_root)
        swap_with.relative_to(fixture_root)
    except ValueError:
        fail("TEST_HOOK_INVALID", "swap hook escapes its fixture root")
    if not request.get("fixture", False) or fixture_root == canonical_root:
        fail("TEST_HOOK_INVALID", "swap hook is disabled outside isolated fixtures")
    try:
        target.relative_to(canonical_root)
        fail("TEST_HOOK_INVALID", "swap hook is inert in the canonical workspace")
    except ValueError:
        pass
    if generation(os.fstat(opened_fd)) != generation(target.lstat()):
        fail("TEST_HOOK_INVALID", "swap target is not the pinned directory")
    replacement = swap_with.lstat()
    if stat.S_ISLNK(replacement.st_mode) or not stat.S_ISDIR(replacement.st_mode):
        fail("TEST_HOOK_INVALID", "swap replacement is not a real directory")
    backup = target.parent / (target.name + ".task-fs-original")
    try:
        backup.lstat()
        fail("TEST_HOOK_INVALID", "swap backup already exists")
    except FileNotFoundError:
        pass
    os.rename(target, backup)
    try:
        os.rename(swap_with, target)
    except BaseException:
        os.rename(backup, target)
        raise
    return {"target": target, "replacement": swap_with, "backup": backup}


def restore_swap(state: Optional[Dict[str, Path]]) -> None:
    if state is None:
        return
    os.rename(state["target"], state["replacement"])
    os.rename(state["backup"], state["target"])


def fixture_replace_publish_temporary(target: Path, temporary: str, temp_fd: int,
                                      parent_fd: int, request: Dict[str, Any]) -> None:
    """Replace a private publication name only inside an isolated test fixture.

    The hook models the exact cleanup race that matters here: an adversarial
    generation reaches the unpredictable temporary pathname after our opened
    inode has been renamed away.  Production callers can never enable it.
    """
    replacement_raw = os.environ.get("TASK_FS_TEST_REPLACE_PUBLISH_TEMP_WITH", "")
    if not replacement_raw:
        return
    fixture_root_raw = os.environ.get("TASK_FS_TEST_ROOT", "")
    if not fixture_root_raw:
        fail("TEST_HOOK_INVALID", "temporary replacement hook requires a fixture root")
    replacement = Path(os.path.abspath(replacement_raw))
    fixture_root = Path(os.path.abspath(fixture_root_raw))
    canonical_root = Path(os.path.abspath(request.get("canonicalRoot", "")))
    try:
        target.relative_to(fixture_root)
        replacement.relative_to(fixture_root)
    except ValueError:
        fail("TEST_HOOK_INVALID", "temporary replacement hook escapes its fixture root")
    if (not request.get("fixture", False) or fixture_root == canonical_root or
            target == canonical_root or canonical_root in target.parents):
        fail("TEST_HOOK_INVALID", "temporary replacement hook is disabled outside isolated fixtures")
    if replacement.parent != target.parent or replacement.name in (target.name, temporary):
        fail("TEST_HOOK_INVALID", "temporary replacement must be a distinct sibling file")
    replacement_stat = replacement.lstat()
    if stat.S_ISLNK(replacement_stat.st_mode) or not stat.S_ISREG(replacement_stat.st_mode):
        fail("TEST_HOOK_INVALID", "temporary replacement must be a real file")
    current_temp = os.stat(temporary, dir_fd=parent_fd, follow_symlinks=False)
    if identity(current_temp) != identity(os.fstat(temp_fd)):
        fail("TEST_HOOK_INVALID", "publication temporary is not the opened fixture inode")
    owned_name = temporary + ".owned-" + secrets.token_hex(18)
    os.rename(temporary, owned_name, src_dir_fd=parent_fd, dst_dir_fd=parent_fd)
    try:
        os.rename(replacement.name, temporary, src_dir_fd=parent_fd, dst_dir_fd=parent_fd)
    except BaseException:
        os.rename(owned_name, temporary, src_dir_fd=parent_fd, dst_dir_fd=parent_fd)
        raise
    os.fsync(parent_fd)


def fixture_publish_rename_target(target: Path, target_parent_fd: int,
                                  request: Dict[str, Any]) -> None:
    replacement_raw = os.environ.get("TASK_FS_TEST_RENAME_TARGET_WITH", "")
    if not replacement_raw:
        return
    fixture_root_raw = os.environ.get("TASK_FS_TEST_ROOT", "")
    if not fixture_root_raw:
        fail("TEST_HOOK_INVALID", "rename-target hook requires a fixture root")
    replacement = Path(os.path.abspath(replacement_raw))
    fixture_root = Path(os.path.abspath(fixture_root_raw))
    canonical_root = Path(os.path.abspath(request.get("canonicalRoot", "")))
    try:
        target.relative_to(fixture_root)
        replacement.relative_to(fixture_root)
    except ValueError:
        fail("TEST_HOOK_INVALID", "rename-target hook escapes its fixture root")
    if (not request.get("fixture", False) or fixture_root == canonical_root or
            target == canonical_root or canonical_root in target.parents):
        fail("TEST_HOOK_INVALID", "rename-target hook is disabled outside isolated fixtures")
    if replacement.parent != target.parent or replacement.name == target.name:
        fail("TEST_HOOK_INVALID", "rename-target replacement must be a distinct sibling file")
    replacement_stat = os.stat(replacement.name, dir_fd=target_parent_fd, follow_symlinks=False)
    if stat.S_ISLNK(replacement_stat.st_mode) or not stat.S_ISREG(replacement_stat.st_mode):
        fail("TEST_HOOK_INVALID", "rename-target replacement must be a real file")
    try:
        os.link(replacement.name, target.name, src_dir_fd=target_parent_fd,
                dst_dir_fd=target_parent_fd, follow_symlinks=False)
    except FileExistsError:
        fail("TEST_HOOK_INVALID", "rename-target destination already exists")
    linked = os.stat(target.name, dir_fd=target_parent_fd, follow_symlinks=False)
    current = os.stat(replacement.name, dir_fd=target_parent_fd, follow_symlinks=False)
    if generation(linked) != generation(current):
        fail("TEST_HOOK_INVALID", "rename-target fixture publication lost its inode lineage")
    os.unlink(replacement.name, dir_fd=target_parent_fd)
    os.fsync(target_parent_fd)


def rename_directory_noreplace(source_parent_fd: int, source_name: str,
                               target_parent_fd: int, target_name: str) -> None:
    """Atomic no-clobber directory rename on supported POSIX kernels."""
    libc = ctypes.CDLL(None, use_errno=True)
    source_bytes = os.fsencode(source_name)
    target_bytes = os.fsencode(target_name)
    if sys.platform.startswith("linux") and hasattr(libc, "renameat2"):
        operation = libc.renameat2
        operation.argtypes = [ctypes.c_int, ctypes.c_char_p, ctypes.c_int, ctypes.c_char_p, ctypes.c_uint]
        operation.restype = ctypes.c_int
        result = operation(source_parent_fd, source_bytes, target_parent_fd, target_bytes, 1)  # RENAME_NOREPLACE
    elif sys.platform == "darwin" and hasattr(libc, "renameatx_np"):
        operation = libc.renameatx_np
        operation.argtypes = [ctypes.c_int, ctypes.c_char_p, ctypes.c_int, ctypes.c_char_p, ctypes.c_uint]
        operation.restype = ctypes.c_int
        result = operation(source_parent_fd, source_bytes, target_parent_fd, target_bytes, 0x00000004)  # RENAME_EXCL
    else:
        fail("NO_CLOBBER_UNSUPPORTED", "atomic no-clobber directory rename is unavailable")
    if result == 0:
        return
    code = ctypes.get_errno()
    if code in (errno.EEXIST, errno.ENOTEMPTY):
        raise FileExistsError(code, os.strerror(code), target_name)
    if code == errno.ENOSYS:
        fail("NO_CLOBBER_UNSUPPORTED", "atomic no-clobber directory rename is unavailable")
    raise OSError(code, os.strerror(code), source_name)


def list_directory(request: Dict[str, Any]) -> Dict[str, Any]:
    target = Path(os.path.abspath(request["path"]))
    authority = Path(os.path.abspath(request["authorityRoot"]))
    maximum = request.get("maxEntries")
    if not isinstance(maximum, int) or isinstance(maximum, bool) or maximum < 1 or maximum > 100_000:
        fail("ARGUMENT_INVALID", "maxEntries is outside its bound")
    expected = parse_expected(request.get("expected"), "directory")
    try:
        chain, fd = open_dir(target, authority)
    except BoundaryError as exc:
        if expected is not None and exc.code == "PATH_MISSING":
            fail("DIRECTORY_CHANGED", "frozen directory disappeared")
        raise
    swap: Optional[Dict[str, Path]] = None
    try:
        opened = os.fstat(fd)
        if expected is not None and generation(opened) != expected:
            fail("DIRECTORY_CHANGED", "directory generation differs from its frozen proof")
        swap = fixture_swap(target, fd, request)
        names: List[str] = []
        with os.scandir(fd) as iterator:
            for entry in iterator:
                if len(names) > maximum:
                    break
                names.append(entry.name)
        names.sort()
        truncated = len(names) > maximum
        if truncated:
            names = names[:maximum]
        entries: Dict[str, Any] = {}
        for name in names:
            try:
                entries[name] = proof(os.stat(name, dir_fd=fd, follow_symlinks=False))
            except FileNotFoundError:
                fail("ENTRY_CHANGED", f"directory entry disappeared: {name}")
        after = os.fstat(fd)
        if identity(after) != identity(opened):
            fail("DIRECTORY_CHANGED", "directory changed while enumerating")
        if os.environ.get("TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY") == "1":
            restore_swap(swap)
            swap = None
        verify_target(chain, target)
        return {"stat": proof(after), "names": names, "truncated": truncated,
                "entries": entries}
    finally:
        try:
            restore_swap(swap)
        finally:
            close_chain(chain)


def read_file(request: Dict[str, Any]) -> Dict[str, Any]:
    target = Path(os.path.abspath(request["path"]))
    authority = Path(os.path.abspath(request["authorityRoot"]))
    maximum = request.get("maxBytes")
    if not isinstance(maximum, int) or isinstance(maximum, bool) or maximum < 1 or maximum > 32 * 1024 * 1024:
        fail("ARGUMENT_INVALID", "maxBytes is outside its bound")
    expected_parent = parse_expected(request.get("expectedParent"), "parent")
    expected_file = exact_requested(request["expectedFile"]) if request.get("expectedFile") is not None else None
    parent = target.parent
    try:
        chain, parent_fd = open_dir(parent, authority)
    except BoundaryError as exc:
        if expected_parent is not None and exc.code == "PATH_MISSING":
            fail("DIRECTORY_CHANGED", "frozen file parent disappeared")
        raise
    swap: Optional[Dict[str, Path]] = None
    try:
        parent_opened = os.fstat(parent_fd)
        if expected_parent is not None and generation(parent_opened) != expected_parent:
            fail("DIRECTORY_CHANGED", "file parent differs from its frozen generation")
        swap = fixture_swap(parent, parent_fd, request)
        try:
            before = os.stat(target.name, dir_fd=parent_fd, follow_symlinks=False)
        except FileNotFoundError:
            if expected_file is not None:
                fail("ENTRY_CHANGED", "frozen file disappeared")
            fail("PATH_MISSING", "file path is missing")
        if expected_file is not None:
            if any(str(proof(before).get(key)) != str(expected_file.get(key))
                    for key in ("dev", "ino", "mode", "size", "mtimeNs", "ctimeNs")):
                fail("ENTRY_CHANGED", "file differs from its frozen exact proof")
        if not stat.S_ISREG(before.st_mode) or stat.S_ISLNK(before.st_mode):
            verify_target(chain, parent)
            return {"stat": proof(before), "unsafe": True, "tooLarge": False}
        if before.st_size > maximum:
            verify_target(chain, parent)
            return {"stat": proof(before), "unsafe": False, "tooLarge": True}
        fd: Optional[int] = None
        try:
            try:
                fd = os.open(target.name, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0), dir_fd=parent_fd)
            except FileNotFoundError:
                fail("ENTRY_CHANGED", "file disappeared while opening")
            opened = os.fstat(fd)
            if not stat.S_ISREG(opened.st_mode) or identity(opened) != identity(before):
                fail("ENTRY_CHANGED", "file changed while opening")
            chunks: List[bytes] = []
            total = 0
            while True:
                chunk = os.read(fd, min(65536, maximum + 1 - total))
                if not chunk:
                    break
                total += len(chunk)
                if total > maximum:
                    fail("ENTRY_TOO_LARGE", "file grew beyond its byte bound")
                chunks.append(chunk)
            data = b"".join(chunks)
            after_fd = os.fstat(fd)
            try:
                after_name = os.stat(target.name, dir_fd=parent_fd, follow_symlinks=False)
            except FileNotFoundError:
                fail("ENTRY_CHANGED", "file disappeared while reading")
            if identity(after_fd) != identity(opened) or identity(after_name) != identity(opened):
                fail("ENTRY_CHANGED", "file changed while reading")
        finally:
            if fd is not None:
                os.close(fd)
        if os.environ.get("TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY") == "1":
            restore_swap(swap)
            swap = None
        verify_target(chain, parent)
        return {"stat": proof(opened), "unsafe": False, "tooLarge": False,
                "rawBase64": base64.b64encode(data).decode("ascii")}
    finally:
        try:
            restore_swap(swap)
        finally:
            close_chain(chain)


def exact_requested(raw: Any) -> Dict[str, Any]:
    required = ("dev", "ino", "mode", "size", "mtimeNs", "ctimeNs")
    if not isinstance(raw, dict) or set(raw) != set(required):
        fail("ARGUMENT_INVALID", "exact stat proof is missing")
    for field in ("dev", "ino", "mtimeNs", "ctimeNs"):
        value = raw[field]
        if not isinstance(value, str) or not re.fullmatch(r"(?:0|[1-9][0-9]*)", value):
            fail("ARGUMENT_INVALID", "exact stat proof has a non-canonical decimal field")
    if (not isinstance(raw["mode"], int) or isinstance(raw["mode"], bool) or raw["mode"] < 0 or
            not isinstance(raw["size"], int) or isinstance(raw["size"], bool) or raw["size"] < 0):
        fail("ARGUMENT_INVALID", "exact stat proof has an invalid numeric field")
    return raw


def same_requested(st: os.stat_result, raw: Dict[str, Any]) -> bool:
    current = proof(st)
    return all(str(current[key]) == str(raw[key]) for key in
               ("dev", "ino", "mode", "size", "mtimeNs", "ctimeNs"))


def same_moved(st: os.stat_result, raw: Dict[str, Any]) -> bool:
    current = proof(st)
    return all(str(current[key]) == str(raw[key]) for key in
               ("dev", "ino", "mode", "size", "mtimeNs"))


def ensure_directory(request: Dict[str, Any]) -> Dict[str, Any]:
    target = Path(os.path.abspath(request["path"]))
    authority = Path(os.path.abspath(request["authorityRoot"]))
    chain, fd = open_dir(target, authority, create=True)
    try:
        verify_target(chain, target)
        return {"stat": proof(os.fstat(fd))}
    finally:
        close_chain(chain)


def mkdir_exclusive(request: Dict[str, Any]) -> Dict[str, Any]:
    target = Path(os.path.abspath(request["path"]))
    authority = Path(os.path.abspath(request["authorityRoot"]))
    chain, parent_fd = open_dir(target.parent, authority)
    swap: Optional[Dict[str, Path]] = None
    try:
        swap = fixture_swap(target.parent, parent_fd, request)
        try:
            os.mkdir(target.name, 0o700, dir_fd=parent_fd)
        except FileExistsError:
            fail("EEXIST", "destination already exists")
        os.fsync(parent_fd)
        chain[-1]["opened"] = os.fstat(parent_fd)
        created = os.stat(target.name, dir_fd=parent_fd, follow_symlinks=False)
        if not stat.S_ISDIR(created.st_mode) or stat.S_ISLNK(created.st_mode):
            fail("ENTRY_CHANGED", "created path is not a real directory")
        if os.environ.get("TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY") == "1":
            restore_swap(swap)
            swap = None
        verify_target(chain, target.parent)
        return {"stat": proof(created)}
    finally:
        try:
            restore_swap(swap)
        finally:
            close_chain(chain)


def publish_file(request: Dict[str, Any]) -> Dict[str, Any]:
    target = Path(os.path.abspath(request["path"]))
    authority = Path(os.path.abspath(request["authorityRoot"]))
    try:
        data = base64.b64decode(request["rawBase64"], validate=True)
    except (KeyError, ValueError, binascii.Error):
        fail("ARGUMENT_INVALID", "publication bytes are invalid")
    maximum = request.get("maxBytes")
    if not isinstance(maximum, int) or isinstance(maximum, bool) or len(data) > maximum or maximum > 32 * 1024 * 1024:
        fail("ARGUMENT_INVALID", "publication bytes exceed their bound")
    chain, parent_fd = open_dir(target.parent, authority, create=request.get("createParents") is True)
    temporary = "." + target.name + ".publish-" + secrets.token_hex(18)
    temp_fd: Optional[int] = None
    temp_stat: Optional[os.stat_result] = None
    published = False
    swap: Optional[Dict[str, Path]] = None
    cleanup_ambiguous = False
    try:
        swap = fixture_swap(target.parent, parent_fd, request)
        flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0)
        temp_fd = os.open(temporary, flags, 0o600, dir_fd=parent_fd)
        offset = 0
        while offset < len(data):
            offset += os.write(temp_fd, data[offset:])
        os.fsync(temp_fd)
        temp_stat = os.fstat(temp_fd)
        try:
            os.link(temporary, target.name, src_dir_fd=parent_fd, dst_dir_fd=parent_fd,
                    follow_symlinks=False)
        except FileExistsError:
            fixture_replace_publish_temporary(target, temporary, temp_fd, parent_fd, request)
            fail("EEXIST", "destination already exists")
        published = True
        os.fsync(parent_fd)
        linked_stat = os.fstat(temp_fd)
        target_stat = os.stat(target.name, dir_fd=parent_fd, follow_symlinks=False)
        if identity(target_stat) != identity(linked_stat):
            fail("ENTRY_CHANGED", "published file differs from its exact candidate")
        # The temporary name is unpredictable and private. Reprove it through
        # the pinned parent immediately before unlink; any ambiguity retains it
        # and fails closed instead of deleting by pathname.
        current_temp = os.stat(temporary, dir_fd=parent_fd, follow_symlinks=False)
        if identity(current_temp) != identity(linked_stat):
            fail("ENTRY_CHANGED", "publication temporary was replaced")
        os.unlink(temporary, dir_fd=parent_fd)
        os.fsync(parent_fd)
        chain[-1]["opened"] = os.fstat(parent_fd)
        target_stat = os.stat(target.name, dir_fd=parent_fd, follow_symlinks=False)
        if os.environ.get("TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY") == "1":
            restore_swap(swap)
            swap = None
        # Another no-clobber publisher may safely create/unlink a private name
        # in this same directory.  Such activity changes only the leaf
        # directory timestamps, not its pinned dev/inode generation.  Every
        # ancestor remains exact, so a rename/swap (including swap-back) still
        # fails closed while cooperative concurrent publication can converge.
        verify_target(chain, target.parent, allow_leaf_metadata_drift=True)
        return {"stat": proof(target_stat)}
    except BoundaryError:
        raise
    except BaseException:
        raise
    finally:
        if not published and temp_stat is not None:
            try:
                current = os.stat(temporary, dir_fd=parent_fd, follow_symlinks=False)
            except FileNotFoundError:
                current = None
            opened = os.fstat(temp_fd) if temp_fd is not None else None
            if current is not None and opened is not None and identity(current) == identity(opened) == identity(temp_stat):
                os.unlink(temporary, dir_fd=parent_fd)
                os.fsync(parent_fd)
            elif current is not None:
                cleanup_ambiguous = True
        if temp_fd is not None:
            os.close(temp_fd)
        try:
            restore_swap(swap)
        finally:
            close_chain(chain)
        if cleanup_ambiguous:
            fail("TEMP_CLEANUP_AMBIGUOUS", "publication temporary changed; foreign generation was retained")


def rename_exact(request: Dict[str, Any]) -> Dict[str, Any]:
    source = Path(os.path.abspath(request["sourcePath"]))
    target = Path(os.path.abspath(request["targetPath"]))
    authority = Path(os.path.abspath(request["authorityRoot"]))
    expected = exact_requested(request.get("expectedSource"))
    source_chain, source_parent_fd = open_dir(source.parent, authority)
    target_chain: List[Dict[str, Any]] = []
    target_parent_fd: Optional[int] = None
    source_fd: Optional[int] = None
    swap: Optional[Dict[str, Path]] = None
    try:
        target_chain, target_parent_fd = open_dir(target.parent, authority)
        before = os.stat(source.name, dir_fd=source_parent_fd, follow_symlinks=False)
        if not same_requested(before, expected):
            fail("ENTRY_CHANGED", "rename source differs from its exact proof")
        # The hook targets the source parent and is accepted only for an
        # isolated temp fixture. All mutation still uses the pinned descriptor.
        swap = fixture_swap(source.parent, source_parent_fd, request)
        fixture_publish_rename_target(target, target_parent_fd, request)
        if stat.S_ISREG(before.st_mode) and not stat.S_ISLNK(before.st_mode):
            source_fd = os.open(source.name, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0),
                                dir_fd=source_parent_fd)
            opened = os.fstat(source_fd)
            if not same_requested(opened, expected):
                fail("ENTRY_CHANGED", "rename source changed while opening its exact generation")
            try:
                os.link(source.name, target.name, src_dir_fd=source_parent_fd,
                        dst_dir_fd=target_parent_fd, follow_symlinks=False)
            except FileExistsError:
                fail("EEXIST", "rename destination already exists")
            os.fsync(target_parent_fd)
            linked_open = os.fstat(source_fd)
            linked_source = os.stat(source.name, dir_fd=source_parent_fd, follow_symlinks=False)
            linked_target = os.stat(target.name, dir_fd=target_parent_fd, follow_symlinks=False)
            if identity(linked_open) != identity(linked_source) or identity(linked_open) != identity(linked_target):
                fail("ENTRY_CHANGED", "rename link publication lost its exact source lineage")
            # Delete only the still-open source generation. If its pathname was
            # replaced, retain both names and let the owning WAL reconcile.
            current_source = os.stat(source.name, dir_fd=source_parent_fd, follow_symlinks=False)
            current_open = os.fstat(source_fd)
            if identity(current_source) != identity(current_open):
                fail("ENTRY_CHANGED", "rename source changed before authenticated unlink")
            os.unlink(source.name, dir_fd=source_parent_fd)
            os.fsync(source_parent_fd)
        elif stat.S_ISDIR(before.st_mode) and not stat.S_ISLNK(before.st_mode):
            try:
                rename_directory_noreplace(source_parent_fd, source.name, target_parent_fd, target.name)
            except FileExistsError:
                fail("EEXIST", "rename destination already exists")
            os.fsync(source_parent_fd)
            if target_parent_fd != source_parent_fd:
                os.fsync(target_parent_fd)
        else:
            fail("ENTRY_CHANGED", "rename source is neither a real file nor a real directory")
        refresh_generation([source_chain, target_chain], source_parent_fd)
        refresh_generation([source_chain, target_chain], target_parent_fd)
        moved = os.stat(target.name, dir_fd=target_parent_fd, follow_symlinks=False)
        if not same_moved(moved, expected):
            fail("ENTRY_CHANGED", "a different generation reached the rename boundary")
        if source_fd is not None and identity(moved) != identity(os.fstat(source_fd)):
            fail("ENTRY_CHANGED", "rename target differs from the authenticated open generation")
        try:
            os.stat(source.name, dir_fd=source_parent_fd, follow_symlinks=False)
            fail("ENTRY_CHANGED", "rename source name unexpectedly remains")
        except FileNotFoundError:
            pass
        if os.environ.get("TASK_FS_TEST_SWAP_RESTORE_BEFORE_VERIFY") == "1":
            restore_swap(swap)
            swap = None
        verify_target(source_chain, source.parent)
        verify_target(target_chain, target.parent)
        return {"stat": proof(moved)}
    finally:
        if source_fd is not None:
            os.close(source_fd)
        try:
            restore_swap(swap)
        finally:
            if target_chain:
                close_chain(target_chain)
            close_chain(source_chain)


STEM_RE = re.compile(r"^TASK_([1-9][0-9]*)_[A-Za-z0-9_]+$")
CAS_RE = re.compile(r"^\.durable-cas-[a-f0-9]{16}-[a-f0-9]{16}-[a-f0-9]{16}$")
TASK_PREFIX_RE = re.compile(r"^TASK_", re.IGNORECASE)
CLAIM_RE = re.compile(r"^\.(TASK_[1-9][0-9]*_[A-Za-z0-9_]+)\.md\.claim\.[1-9][0-9]*\.[a-f0-9]{12}$")
TRANSITION_TMP_RE = re.compile(r"^\.transition-[a-f0-9]{36}\.tmp$")
CREATE_EDIT_TMP_RE = re.compile(r"^\.(?:create|edit)-[a-f0-9]{32}\.tmp$")


def canonical_stem(value: str) -> bool:
    match = STEM_RE.fullmatch(value)
    if not match or len(value) > 120:
        return False
    number = int(match.group(1))
    return 0 < number <= 9007199254740991 and match.group(1) == str(number)


HTML_BLOCK_TAGS = (r"(?:address|article|aside|base|basefont|blockquote|body|caption|center|col|colgroup|"
                   r"dd|details|dialog|dir|div|dl|dt|fieldset|figcaption|figure|footer|form|frame|frameset|"
                   r"h[1-6]|head|header|hr|html|iframe|legend|li|link|main|menu|menuitem|nav|noframes|ol|"
                   r"optgroup|option|p|param|search|section|summary|table|tbody|td|tfoot|th|thead|title|tr|track|ul)")
HTML_TAG_NAME = r"[A-Za-z][A-Za-z0-9-]*"
HTML_ATTR_NAME = r"[A-Za-z_:][A-Za-z0-9_.:-]*"
HTML_ATTR_VALUE = r'''(?:[^\s"'=<>`]+|'[^']*'|"[^"]*")'''
HTML_ATTRIBUTE = r"[ \t]+" + HTML_ATTR_NAME + r"(?:[ \t]*=[ \t]*" + HTML_ATTR_VALUE + r")?"
HTML_COMPLETE_OPEN_RE = re.compile(r"^ {0,3}<(" + HTML_TAG_NAME + r")(?:(?:" + HTML_ATTRIBUTE + r"))*[ \t]*/?>[ \t]*$", re.IGNORECASE)
HTML_COMPLETE_CLOSE_RE = re.compile(r"^ {0,3}</" + HTML_TAG_NAME + r"[ \t]*>[ \t]*$", re.IGNORECASE)
HTML_TYPE6_RE = re.compile(r"^ {0,3}</?" + HTML_BLOCK_TAGS + r"(?:[ \t]|/?>|$)", re.IGNORECASE)


def html_block_start(line: str, paragraph_open: bool) -> Optional[Tuple[str, Optional[re.Pattern[str]]]]:
    if re.match(r"^ {0,3}<(?:pre|script|style|textarea)(?:[ \t>]|$)", line, re.IGNORECASE):
        return ("end", re.compile(r"</(?:pre|script|style|textarea)>", re.IGNORECASE))
    if re.match(r"^ {0,3}<!--", line):
        return ("end", re.compile(r"-->"))
    if re.match(r"^ {0,3}<\?", line):
        return ("end", re.compile(r"\?>"))
    if re.match(r"^ {0,3}<![A-Z]", line):
        return ("end", re.compile(r">"))
    if re.match(r"^ {0,3}<!\[CDATA\[", line):
        return ("end", re.compile(r"\]\]>"))
    if HTML_TYPE6_RE.match(line):
        return ("blank", None)
    if not paragraph_open:
        opening = HTML_COMPLETE_OPEN_RE.match(line)
        if ((opening and not re.fullmatch(r"(?:pre|script|style|textarea)", opening.group(1), re.IGNORECASE)) or
                HTML_COMPLETE_CLOSE_RE.match(line)):
            return ("blank", None)
    return None


def html_block_line_ends(block: Tuple[str, Optional[re.Pattern[str]]], line: str) -> bool:
    kind, pattern = block
    return bool(re.fullmatch(r"[ \t]*", line)) if kind == "blank" else bool(pattern and pattern.search(line))


def escaped_punctuation_at(line: str, index: int) -> bool:
    slashes = 0
    cursor = index - 1
    while cursor >= 0 and line[cursor] == "\\":
        slashes += 1
        cursor -= 1
    return slashes % 2 == 1


def code_span_intervals_in_range(source: str, start: int, end: int) -> List[Tuple[int, int]]:
    """Pair equal maximal backtick runs in linear time within one block range."""
    runs: List[Tuple[int, int, int, bool]] = []
    cursor = start
    while cursor < end:
        if source[cursor] != "`":
            cursor += 1
            continue
        run_start = cursor
        while cursor < end and source[cursor] == "`":
            cursor += 1
        slashes = 0
        before = run_start - 1
        while before >= start and source[before] == "\\":
            slashes += 1
            before -= 1
        runs.append((run_start, cursor, cursor - run_start, slashes % 2 == 1))

    next_same = [-1] * len(runs)
    last_by_length: Dict[int, int] = {}
    for index in range(len(runs) - 1, -1, -1):
        length = runs[index][2]
        next_same[index] = last_by_length.get(length, -1)
        last_by_length[length] = index

    intervals: List[Tuple[int, int]] = []
    opener = 0
    while opener < len(runs):
        closer = next_same[opener]
        if runs[opener][3] or closer < 0:
            opener += 1
            continue
        intervals.append((runs[opener][0], runs[closer][1]))
        opener = closer + 1
    return intervals


def inline_code_span_intervals(source: str, lines: List[str], line_starts: List[int]) -> List[Tuple[int, int]]:
    """Collect code spans from inline-capable block/paragraph ranges only."""
    intervals: List[Tuple[int, int]] = []
    fence: Optional[Tuple[str, int]] = None
    html_block: Optional[Tuple[str, Optional[re.Pattern[str]]]] = None
    paragraph_open = False
    paragraph_start = -1
    paragraph_end = -1

    def append_range(start: int, end: int) -> None:
        if start >= 0 and end > start:
            intervals.extend(code_span_intervals_in_range(source, start, end))

    def flush_paragraph() -> None:
        nonlocal paragraph_start, paragraph_end
        append_range(paragraph_start, paragraph_end)
        paragraph_start = -1
        paragraph_end = -1

    for line_index, line in enumerate(lines):
        line_start = line_starts[line_index]
        line_end = line_start + len(line)

        if html_block is not None:
            flush_paragraph()
            if html_block_line_ends(html_block, line):
                html_block = None
            if re.fullmatch(r"[ \t]*", line):
                paragraph_open = False
            continue

        opening = re.match(r"^ {0,3}(`{3,}|~{3,})(.*)$", line)
        if opening and opening.group(1).startswith("`") and "`" in opening.group(2):
            opening = None
        if fence is None and opening:
            flush_paragraph()
            token = opening.group(1)
            fence = (token[0], len(token))
            paragraph_open = False
            continue
        if fence is not None and re.match(r"^ {0,3}" + re.escape(fence[0]) +
                                          "{" + str(fence[1]) + r",}[ \t]*$", line):
            flush_paragraph()
            fence = None
            paragraph_open = False
            continue
        if fence is not None:
            flush_paragraph()
            continue

        if not paragraph_open and (re.match(r"^ {4}", line) or line.startswith("\t")):
            flush_paragraph()
            continue

        block = html_block_start(line, paragraph_open)
        if block is not None:
            flush_paragraph()
            if not html_block_line_ends(block, line):
                html_block = block
            paragraph_open = False
            continue

        blank = bool(re.fullmatch(r"[ \t]*", line))
        atx = bool(re.match(r"^ {0,3}#{1,6}(?:[ \t]+|$)", line))
        thematic = (bool(re.match(r"^ {0,3}(?:\*[ \t]*){3,}$", line)) or
                    bool(re.match(r"^ {0,3}(?:-[ \t]*){3,}$", line)) or
                    bool(re.match(r"^ {0,3}(?:_[ \t]*){3,}$", line)))
        if blank or atx or thematic:
            flush_paragraph()
            # Heading text still has inline content, but code spans cannot
            # cross its block boundary in either direction.
            if atx:
                append_range(line_start, line_end)
            paragraph_open = False
            continue

        if paragraph_start < 0:
            paragraph_start = line_start
        paragraph_end = line_end
        paragraph_open = True

    flush_paragraph()
    return intervals


def code_span_contains(intervals: List[Tuple[int, int]], state: List[int], position: int) -> bool:
    while state[0] < len(intervals) and intervals[state[0]][1] <= position:
        state[0] += 1
    return (state[0] < len(intervals) and intervals[state[0]][0] <= position < intervals[state[0]][1])


def discard_code_spans_started_before(intervals: List[Tuple[int, int]], state: List[int], position: int) -> None:
    while state[0] < len(intervals) and intervals[state[0]][0] < position:
        state[0] += 1


def js_offset_blank(text: str) -> str:
    """Blank text by UTF-16 code units so Python matches both JS mirrors."""
    return "".join("  " if ord(character) > 0xFFFF else " " for character in text)


def mask_inline_html_comments(line: str, starts_inside_comment: bool, line_start: int,
                              code_intervals: List[Tuple[int, int]], code_state: List[int]) -> Tuple[str, bool]:
    chars = list(line)
    cursor = 0
    opened = starts_inside_comment
    while cursor < len(line):
        if opened:
            close = line.find("-->", cursor)
            end = len(line) if close < 0 else close + 3
            for index in range(cursor, end):
                chars[index] = js_offset_blank(chars[index])
            cursor = end
            # Runs swallowed by a real raw comment cannot protect a later
            # opener as though those runs had been parsed as code delimiters.
            discard_code_spans_started_before(code_intervals, code_state, line_start + end)
            if close < 0:
                return ("".join(chars), True)
            opened = False
            continue
        opening = line.find("<!--", cursor)
        if opening < 0:
            break
        if (escaped_punctuation_at(line, opening) or
                code_span_contains(code_intervals, code_state, line_start + opening)):
            cursor = opening + 4
            continue
        opened = True
        cursor = opening
    return ("".join(chars), opened)


def structural_text(text: str) -> str:
    text = text.lstrip("\ufeff").replace("\r\n", "\n").replace("\r", "\n")
    lines = text.split("\n")
    line_starts: List[int] = []
    offset = 0
    for line in lines:
        line_starts.append(offset)
        offset += len(line) + 1
    code_intervals = inline_code_span_intervals(text, lines, line_starts)
    code_state = [0]
    fence: Optional[Tuple[str, int]] = None
    out: List[str] = []
    html_block: Optional[Tuple[str, Optional[re.Pattern[str]]]] = None
    html_comment = False
    paragraph_open = False
    for line_index, line in enumerate(lines):
        if html_block is not None:
            if html_block_line_ends(html_block, line):
                html_block = None
            if re.fullmatch(r"[ \t]*", line):
                paragraph_open = False
            out.append(js_offset_blank(line))
            continue
        if html_comment:
            # A line that starts inside an inline comment remains part of the
            # preceding paragraph/container. Its suffix after `-->` cannot
            # become block structure on that same physical line. Rescan only
            # to carry a chained comment, then mask the whole line and retain
            # the preceding paragraph state exactly like the JS authority.
            _visible, html_comment = mask_inline_html_comments(
                line, True, line_starts[line_index], code_intervals, code_state)
            out.append(js_offset_blank(line))
            continue
        # Keep the dependency-closure interpretation aligned with the
        # shared JavaScript CommonMark scanner used by final admission. A
        # fenced-code delimiter may have at most three literal U+0020 spaces
        # before it; a tab or four spaces is indented code, not a fence. A
        # backtick opener also cannot carry a backtick in its info string.
        opening = re.match(r"^ {0,3}(`{3,}|~{3,})(.*)$", line)
        if opening and opening.group(1).startswith("`") and "`" in opening.group(2):
            opening = None
        if fence is None and opening:
            token = opening.group(1)
            fence = (token[0], len(token))
            paragraph_open = False
            out.append(js_offset_blank(line))
            continue
        if fence is not None and re.match(r"^ {0,3}" + re.escape(fence[0]) +
                                          "{" + str(fence[1]) + r",}[ \t]*$", line):
            fence = None
            paragraph_open = False
            out.append(js_offset_blank(line))
            continue
        if fence is not None:
            out.append(js_offset_blank(line))
            continue
        if not paragraph_open and (re.match(r"^ {4}", line) or line.startswith("\t")):
            out.append(js_offset_blank(line))
            continue
        block = html_block_start(line, paragraph_open)
        if block is not None:
            if not html_block_line_ends(block, line):
                html_block = block
            paragraph_open = False
            out.append(js_offset_blank(line))
            continue
        visible, html_comment = mask_inline_html_comments(
            line, False, line_starts[line_index], code_intervals, code_state)
        if (re.fullmatch(r"[ \t]*", visible) or re.match(r"^ {0,3}#{1,6}(?:[ \t]+|$)", visible) or
                re.match(r"^ {0,3}(?:\*[ \t]*){3,}$", visible) or
                re.match(r"^ {0,3}(?:-[ \t]*){3,}$", visible) or
                re.match(r"^ {0,3}(?:_[ \t]*){3,}$", visible)):
            paragraph_open = False
        else:
            paragraph_open = True
        out.append(visible)
    return "\n".join(out)


def parse_atx_heading_line(line: str) -> Optional[Tuple[int, str]]:
    """Parse one CommonMark ATX heading without accepting indented code.

    Opening hashes may have zero to three literal spaces before them and must
    be followed by a space, tab, or end-of-line. A whitespace-delimited closing
    hash sequence is syntax rather than part of the heading name. This mirrors
    design-parser.cjs:parseAtxHeadingLine, which owns the final JS admission
    interpretation after this boundary has frozen the scoped read closure.
    """
    match = re.fullmatch(r" {0,3}(#{1,6})(?=$|[ \t])([ \t]*)(.*)", line)
    if not match:
        return None
    raw = match.group(2) + match.group(3)
    # This is exactly the shared JS closing-sequence rule, implemented as a
    # right scan. The equivalent unanchored whitespace regex is quadratic on
    # a long heading with no closing hash, which would turn the 8 MiB bounded
    # task input into an admission denial of service.
    trailing = len(raw)
    while trailing > 0 and raw[trailing - 1] in " \t":
        trailing -= 1
    hashes = trailing
    while hashes > 0 and raw[hashes - 1] == "#":
        hashes -= 1
    if hashes != trailing and hashes > 0 and raw[hashes - 1] in " \t":
        whitespace = hashes
        while whitespace > 0 and raw[whitespace - 1] in " \t":
            whitespace -= 1
        raw = raw[:whitespace]
    return (len(match.group(1)), raw.strip(" \t"))


def dependencies(raw: bytes) -> List[str]:
    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError:
        return []
    structural = structural_text(text)
    headings: List[Tuple[str, int, int]] = []
    start = 0
    while start <= len(structural):
        newline = structural.find("\n", start)
        head_end = len(structural) if newline < 0 else newline
        parsed = parse_atx_heading_line(structural[start:head_end])
        if parsed is not None and parsed[0] == 2:
            headings.append((parsed[1], start, head_end))
        if newline < 0:
            break
        start = newline + 1
    result: List[str] = []
    for index, heading in enumerate(headings):
        if not re.fullmatch(r"depends on(?:[ \t]+\(optional\))?", heading[0].lower()):
            continue
        end = headings[index + 1][1] if index + 1 < len(headings) else len(structural)
        body = structural[heading[2]:end]
        for line in body.split("\n"):
            if re.fullmatch(r"[ \t]*", line):
                continue
            # A thematic separator is residue/final appendix syntax, never a
            # command to stop closure discovery. Keep scanning so an edge
            # below malformed residue cannot disappear from admission.
            if re.fullmatch(r"[ \t]*---[ \t]*", line):
                continue
            # Dependency edges are top-level CommonMark bullets only. Generic
            # `\s` would accept tabs/VT/FF and four-space indented code as an
            # executable graph credential.
            bullet = re.match(r"^ {0,3}-[ \t]+(.+?)[ \t]*$", line)
            if not bullet:
                continue
            match = re.fullmatch(
                r"`?(TASK_[1-9][0-9]*_[A-Za-z0-9_]+)`?(?:[ \t]*[\u2014-][ \t]*(.*))?",
                bullet.group(1))
            note = match.group(2) if match and match.group(2) else ""
            if (match and canonical_stem(match.group(1)) and
                    re.search(r"TASK_[0-9]+_", unicodedata.normalize("NFKC", note), re.IGNORECASE) is None):
                result.append(match.group(1))
    return result


def task_snapshot(request: Dict[str, Any]) -> Dict[str, Any]:
    repo = Path(os.path.abspath(request["repoRoot"]))
    tasks = Path(os.path.abspath(request["tasksDir"]))
    outcome = Path(os.path.abspath(request["outcomeShapePath"]))
    if not isinstance(request.get("fullCorpus"), bool):
        fail("ARGUMENT_INVALID", "fullCorpus must be boolean")
    full = request["fullCorpus"]
    scoped = request.get("stem")
    if scoped is not None and (not isinstance(scoped, str) or not canonical_stem(scoped)):
        fail("ARGUMENT_INVALID", "snapshot stem is invalid")
    max_entries = request.get("maxDirectoryEntries")
    max_files = request.get("maxFiles")
    max_total = request.get("maxCorpusBytes")
    for value, label, ceiling in ((max_entries, "maxDirectoryEntries", 100_000),
                                  (max_files, "maxFiles", 100_000),
                                  (max_total, "maxCorpusBytes", 256 * 1024 * 1024)):
        if not isinstance(value, int) or isinstance(value, bool) or value < 1 or value > ceiling:
            fail("ARGUMENT_INVALID", f"{label} is outside its bound")
    common = {"canonicalRoot": request.get("canonicalRoot", ""),
              "fixture": request.get("fixture", False)}
    directories: Dict[str, Any] = {}
    files: Dict[str, Any] = {}
    initial_directories: Dict[str, Dict[str, Any]] = {}
    total_bytes = 0

    def authority_for(target: Path, preferred: Path = repo) -> Path:
        try:
            target.relative_to(preferred)
            return preferred
        except ValueError:
            return target.parent

    def list_one(target: Path, authority: Path, limit: int = max_entries + 1,
                 missing_ok: bool = False, unsafe_ok: bool = False) -> Optional[Dict[str, Any]]:
        payload = {**common, "path": str(target), "authorityRoot": str(authority),
                   "maxEntries": limit}
        try:
            row = list_directory(payload)
        except BoundaryError as exc:
            if missing_ok and exc.code == "PATH_MISSING":
                directories[str(target)] = {"missing": True}
                return None
            if unsafe_ok and exc.code in ("PATH_MISSING", "PATH_UNSAFE", "PATH_OUTSIDE_AUTHORITY",
                                          "DIRECTORY_UNSAFE"):
                directories[str(target)] = {"error": {"code": exc.code, "message": str(exc)}}
                return None
            raise
        directories[str(target)] = row
        initial_directories[str(target)] = {"row": row, "authority": authority, "limit": limit}
        return row

    def read_one(target: Path, maximum: int, parent_row: Optional[Dict[str, Any]] = None,
                 authority: Optional[Path] = None, count_total: bool = True) -> Dict[str, Any]:
        nonlocal total_bytes
        payload: Dict[str, Any] = {**common, "path": str(target),
                                  "authorityRoot": str(authority or authority_for(target)),
                                  "maxBytes": maximum}
        if parent_row is not None:
            payload["expectedParent"] = parent_row["stat"]
            entry = parent_row.get("entries", {}).get(target.name)
            if entry is not None:
                payload["expectedFile"] = {key: entry[key] for key in
                                           ("dev", "ino", "mode", "size", "mtimeNs", "ctimeNs")}
        row = read_file(payload)
        if isinstance(row.get("rawBase64"), str):
            size = len(base64.b64decode(row["rawBase64"], validate=True))
            if count_total:
                total_bytes += size
                if total_bytes > max_total:
                    fail("TOTAL_BYTES_LIMIT", "task snapshot exceeds its bounded byte budget")
        files[str(target)] = row
        return row

    # Required contract is independently anchored when it is outside a fixture
    # repo (the normal unit-test topology).
    read_one(outcome, 256 * 1024, authority=authority_for(outcome), count_total=False)
    root = list_one(tasks, repo, unsafe_ok=True)
    if root is None:
        return {"directories": directories, "files": files,
                "selectedStems": [], "totalBytes": total_bytes}
    columns: Dict[str, Dict[str, Any]] = {}
    groups: Dict[str, Dict[str, Path]] = {}
    for column in ("backlog", "pending", "todo", "done"):
        directory = tasks / column
        row = list_one(directory, repo, unsafe_ok=True)
        if row is None:
            continue
        columns[column] = row
        suffix = ".questions.md" if column == "pending" else ".md"
        for name in row["names"]:
            if not name.endswith(suffix):
                continue
            stem = name[:-len(suffix)]
            if canonical_stem(stem) and name == stem + suffix:
                groups.setdefault(stem, {})[column] = directory / name

    def selected_misc(column: str, name: str) -> bool:
        if name == ".gitkeep":
            return False
        allowed = bool(TRANSITION_TMP_RE.fullmatch(name) or
                       (column == "backlog" and (CREATE_EDIT_TMP_RE.fullmatch(name) or CLAIM_RE.fullmatch(name))))
        if allowed:
            claim = CLAIM_RE.fullmatch(name)
            return full or bool(claim and claim.group(1) == scoped)
        normalized = unicodedata.normalize("NFKC", name).casefold()
        task_like = normalized.startswith("task_")
        looks_temp = normalized.startswith(".task_") or name.lower().startswith((".create-", ".edit-")) or \
            ".claim." in name or name.endswith(".tmp")
        return full and (task_like or looks_temp)

    # Read the root-level task-like blockers only in full mode, matching the JS
    # accounting contract.
    if full:
        for name in root["names"]:
            task_like = unicodedata.normalize("NFKC", name).casefold().startswith("task_")
            if task_like:
                read_one(tasks / name, 8 * 1024 * 1024, root, repo)
    for column, row in columns.items():
        directory = tasks / column
        for name in row["names"]:
            if selected_misc(column, name):
                read_one(directory / name, 8 * 1024 * 1024, row, repo)

    if full:
        selected = sorted(groups)
    else:
        selected = []
        queue = [scoped] if scoped else []
        seen = set()
        proposal_raw: Optional[bytes] = None
        proposal = request.get("proposal")
        if isinstance(proposal, dict) and proposal.get("stem") == scoped and isinstance(proposal.get("rawBase64"), str):
            try:
                proposal_raw = base64.b64decode(proposal["rawBase64"], validate=True)
            except (ValueError, binascii.Error):
                fail("ARGUMENT_INVALID", "proposal bytes are invalid")
        while queue:
            stem = queue.pop(0)
            if stem in seen:
                continue
            seen.add(stem)
            selected.append(stem)
            group = groups.get(stem, {})
            for column, target in group.items():
                read_one(target, 8 * 1024 * 1024, columns[column], repo)
            source = group.get("backlog") or group.get("todo") or group.get("done")
            source_bytes: Optional[bytes] = proposal_raw if stem == scoped and proposal_raw is not None else None
            if source_bytes is None and source is not None:
                row = files.get(str(source), {})
                if isinstance(row.get("rawBase64"), str):
                    source_bytes = base64.b64decode(row["rawBase64"], validate=True)
            if source_bytes is not None:
                for dependency in dependencies(source_bytes):
                    if dependency not in seen:
                        queue.append(dependency)
    if full:
        for stem in selected:
            for column, target in groups[stem].items():
                read_one(target, 8 * 1024 * 1024, columns[column], repo)

    index_path = tasks / "INDEX.json"
    if request.get("checkIndex") is True and "INDEX.json" in root["names"]:
        read_one(index_path, 8 * 1024 * 1024, root, repo, count_total=False)

    # Durable CAS owner state is always part of action admission. Missing
    # external stores are allowed; the backlog parent is already pinned.
    cas_parents: List[Tuple[Path, Path, Optional[Dict[str, Any]]]] = [(tasks / "backlog", repo, columns.get("backlog"))]
    for field, authority_field in (("taskCreationsDir", "taskCreationsAuthorityRoot"),
                                   ("taskEditsDir", "taskEditsAuthorityRoot")):
        raw_target = request.get(field)
        raw_authority = request.get(authority_field)
        if isinstance(raw_target, str) and isinstance(raw_authority, str):
            cas_parents.append((Path(os.path.abspath(raw_target)), Path(os.path.abspath(raw_authority)), None))
    seen_parents = set()
    for parent, authority, known in cas_parents:
        if str(parent) in seen_parents:
            continue
        seen_parents.add(str(parent))
        parent_row = known or list_one(parent, authority, missing_ok=True, unsafe_ok=True)
        if parent_row is None:
            continue
        for name in parent_row["names"]:
            if not name.startswith(".durable-cas-"):
                continue
            target = parent / name
            entry = parent_row["entries"].get(name)
            if not CAS_RE.fullmatch(name) or not entry or entry.get("kind") != "directory":
                if entry and entry.get("kind") == "file":
                    read_one(target, 16 * 1024 * 1024, parent_row, authority)
                continue
            operation = list_one(target, authority, 9)
            if operation is None:
                continue
            for artifact_name in operation["names"]:
                maximum = 16 * 1024 if artifact_name == "manifest.json" or artifact_name.startswith(".manifest-partial-") else 16 * 1024 * 1024
                read_one(target / artifact_name, maximum, operation, authority)

    if request.get("includeRuntime") is True and isinstance(request.get("locksDir"), str):
        locks = Path(os.path.abspath(request["locksDir"]))
        lock_row = list_one(locks, repo, request.get("maxRuntimeFiles", max_files) + 1,
                            missing_ok=True, unsafe_ok=True)
        if lock_row is not None:
            for name in lock_row["names"]:
                if not name.endswith(".json"):
                    continue
                stem = name[:-5]
                if full or stem == scoped or not canonical_stem(stem):
                    read_one(locks / name, 32 * 1024, lock_row, repo, count_total=False)

    # Re-enumerate every directory with its exact generation and compare the
    # complete entry proofs. This catches content-only replacement via entry
    # ctimeNs as well as name/dir changes; no path is reopened without its
    # frozen dev+ino parent proof.
    for target_raw, frozen in list(initial_directories.items()):
        target = Path(target_raw)
        fresh = list_directory({**common, "path": target_raw,
                                "authorityRoot": str(frozen["authority"]),
                                "maxEntries": frozen["limit"],
                                "expected": frozen["row"]["stat"]})
        if fresh != frozen["row"]:
            fail("DIRECTORY_CHANGED", f"directory snapshot changed: {target.name}")
    return {"directories": directories, "files": files,
            "selectedStems": selected, "totalBytes": total_bytes}


def main() -> int:
    try:
        raw = sys.stdin.buffer.read(MAX_REQUEST + 1)
        if len(raw) > MAX_REQUEST:
            fail("ARGUMENT_INVALID", "request exceeds its byte bound")
        request = json.loads(raw.decode("utf-8"))
        if not isinstance(request, dict) or request.get("version") != 1:
            fail("ARGUMENT_INVALID", "request envelope is invalid")
        action = request.get("action")
        if action == "list":
            result = list_directory(request)
        elif action == "read":
            result = read_file(request)
        elif action == "task-snapshot":
            result = task_snapshot(request)
        elif action == "ensure-dir":
            result = ensure_directory(request)
        elif action == "mkdir":
            result = mkdir_exclusive(request)
        elif action == "publish":
            result = publish_file(request)
        elif action == "rename-exact":
            result = rename_exact(request)
        else:
            fail("ARGUMENT_INVALID", "unsupported filesystem action")
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


if __name__ == "__main__":
    raise SystemExit(main())
