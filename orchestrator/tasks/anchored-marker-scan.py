#!/usr/bin/env python3
"""Read-only, dir-fd-anchored snapshot for the JS edit-marker contract.

The caller validates marker semantics.  This helper owns only the filesystem
boundary: one pinned directory generation, bounded enumeration, no-follow
regular-file reads, exact before/open/after identity checks, and a bounded JSON
envelope.  It never creates or edits project state outside its explicitly
fixture-only swap-back hook.
"""

from __future__ import annotations

import base64
import json
import os
import re
import stat
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional


NAME_RE = re.compile(r"^(TASK_[0-9]+_[A-Za-z0-9_]+)\.json$")


def emit(value: dict) -> None:
    sys.stdout.write(json.dumps(value, ensure_ascii=True, separators=(",", ":")) + "\n")
    sys.stdout.flush()


def fail(code: str, message: str) -> None:
    raise RuntimeError(json.dumps({"code": code, "message": str(message)[:1200]}))


def kind(st: os.stat_result) -> str:
    if stat.S_ISREG(st.st_mode):
        return "file"
    if stat.S_ISDIR(st.st_mode):
        return "directory"
    if stat.S_ISLNK(st.st_mode):
        return "symlink"
    return "other"


def identity(st: os.stat_result) -> tuple:
    return (st.st_dev, st.st_ino, st.st_mode, st.st_size,
            st.st_mtime_ns, st.st_ctime_ns)


def _directory_flags() -> int:
    return os.O_RDONLY | getattr(os, "O_DIRECTORY", 0) | getattr(os, "O_NOFOLLOW", 0)


def verify_chain(chain: List[Dict[str, Any]]) -> None:
    """Reprove every pinned component through its pinned parent."""
    for index, item in enumerate(chain):
        opened = os.fstat(item["fd"])
        if not stat.S_ISDIR(opened.st_mode) or identity(opened) != identity(item["opened"]):
            fail("DIRECTORY_CHANGED", "marker directory ancestry changed while being scanned")
        if index == 0:
            live = item["path"].lstat()
        else:
            parent_fd = chain[index - 1]["fd"]
            live = os.stat(item["name"], dir_fd=parent_fd, follow_symlinks=False)
        if stat.S_ISLNK(live.st_mode) or not stat.S_ISDIR(live.st_mode) or identity(live) != identity(opened):
            fail("DIRECTORY_CHANGED", "marker directory no longer resolves to its pinned generation")


def verify_canonical_target(chain: List[Dict[str, Any]], target: Path) -> None:
    """Bind the path-wide target proof between two anchored-chain proofs."""
    verify_chain(chain)
    try:
        live_target = target.lstat()
    except FileNotFoundError:
        fail("DIRECTORY_CHANGED", "marker directory vanished during its anchored check")
    final = os.fstat(chain[-1]["fd"])
    if stat.S_ISLNK(live_target.st_mode) or not stat.S_ISDIR(live_target.st_mode) or \
            identity(live_target) != identity(final):
        fail("DIRECTORY_CHANGED", "canonical marker path does not resolve to the pinned directory")
    verify_chain(chain)


def open_directory_anchored(directory: Path, authority_root: Path) -> tuple:
    """Open a directory by no-follow openat walk from one explicit trust root.

    All component descriptors remain pinned until the snapshot completes so a
    rename/symlink swap in any ancestor is detected on return.  A missing final
    component is also decided through its pinned parent rather than a path-wide
    exists() race.
    """
    try:
        relative = directory.relative_to(authority_root)
    except ValueError:
        fail("DIRECTORY_UNSAFE", "marker directory escapes its authority root")
    parts = relative.parts
    if len(parts) > 64 or any(part in ("", ".", "..") for part in parts):
        fail("DIRECTORY_UNSAFE", "marker directory has unsafe path components")
    chain: List[Dict[str, Any]] = []
    try:
        root_before = authority_root.lstat()
        if stat.S_ISLNK(root_before.st_mode) or not stat.S_ISDIR(root_before.st_mode):
            fail("DIRECTORY_UNSAFE", "marker authority root is not a real directory")
        root_fd = os.open(authority_root, _directory_flags())
        root_opened = os.fstat(root_fd)
        if identity(root_opened) != identity(root_before):
            os.close(root_fd)
            fail("DIRECTORY_CHANGED", "marker authority root changed while being opened")
        chain.append({"fd": root_fd, "opened": root_opened,
                      "path": authority_root, "name": None})
        current = authority_root
        for index, part in enumerate(parts):
            parent_fd = chain[-1]["fd"]
            current = current / part
            try:
                before = os.stat(part, dir_fd=parent_fd, follow_symlinks=False)
            except FileNotFoundError:
                # Freeze the exact missing component relative to its pinned
                # parent.  A path-wide target.lstat() here would resolve
                # unpinned descendants and reopen the ancestor-swap bug.
                created_fixture = False
                try:
                    created_fixture = fixture_create_missing_component(
                        directory, authority_root, parent_fd, part)
                    verify_chain(chain)
                    try:
                        os.stat(part, dir_fd=parent_fd, follow_symlinks=False)
                    except FileNotFoundError:
                        pass
                    else:
                        fail("DIRECTORY_CHANGED", "missing marker component appeared during its anchored check")
                    verify_chain(chain)
                finally:
                    if created_fixture:
                        try:
                            os.rmdir(part, dir_fd=parent_fd)
                        except OSError:
                            pass
                return chain, None
            if stat.S_ISLNK(before.st_mode) or not stat.S_ISDIR(before.st_mode):
                fail("DIRECTORY_UNSAFE", "marker directory component is not a real directory")
            child_fd = os.open(part, _directory_flags(), dir_fd=parent_fd)
            opened = os.fstat(child_fd)
            if identity(opened) != identity(before):
                os.close(child_fd)
                fail("DIRECTORY_CHANGED", "marker directory component changed while opening")
            chain.append({"fd": child_fd, "opened": opened,
                          "path": current, "name": part})
        verify_canonical_target(chain, directory)
        return chain, chain[-1]["fd"]
    except BaseException:
        for item in reversed(chain):
            try:
                os.close(item["fd"])
            except OSError:
                pass
        raise


def fixture_create_missing_component(directory: Path, authority_root: Path,
                                     parent_fd: int, name: str) -> bool:
    if os.environ.get("EDIT_MARKER_SCAN_TEST_CREATE_MISSING") != "1":
        return False
    test_root_raw = os.environ.get("EDIT_MARKER_SCAN_TEST_ROOT", "")
    if not test_root_raw:
        fail("TEST_HOOK_INVALID", "missing-component hook requires a fixture root")
    test_root = Path(os.path.abspath(test_root_raw))
    canonical_root = Path(os.path.abspath(Path(__file__).resolve().parents[2]))
    try:
        directory.relative_to(test_root)
        authority_root.relative_to(test_root)
    except ValueError:
        fail("TEST_HOOK_INVALID", "missing-component hook escapes the fixture root")
    if test_root == canonical_root:
        fail("TEST_HOOK_INVALID", "missing-component hook is inert in the canonical workspace")
    os.mkdir(name, 0o700, dir_fd=parent_fd)
    return True


def read_regular(directory_fd: int, name: str, before: os.stat_result,
                 max_file_bytes: int) -> bytes:
    flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)
    fd: Optional[int] = None
    try:
        fd = os.open(name, flags, dir_fd=directory_fd)
        opened = os.fstat(fd)
        if not stat.S_ISREG(opened.st_mode) or identity(opened) != identity(before):
            fail("ENTRY_CHANGED", f"marker changed identity while opening: {name}")
        if opened.st_size > max_file_bytes:
            fail("ENTRY_TOO_LARGE", f"marker exceeds its byte limit: {name}")
        chunks: List[bytes] = []
        total = 0
        while True:
            chunk = os.read(fd, min(65536, max_file_bytes + 1 - total))
            if not chunk:
                break
            total += len(chunk)
            if total > max_file_bytes:
                fail("ENTRY_TOO_LARGE", f"marker exceeds its byte limit: {name}")
            chunks.append(chunk)
        data = b"".join(chunks)
        after_fd = os.fstat(fd)
        after_name = os.stat(name, dir_fd=directory_fd, follow_symlinks=False)
        if identity(after_fd) != identity(opened) or identity(after_name) != identity(opened):
            fail("ENTRY_CHANGED", f"marker changed while being read: {name}")
        return data
    finally:
        if fd is not None:
            os.close(fd)


def fixture_swap(directory: Path, directory_fd: int, canonical_root: Path) -> Optional[Dict[str, Path]]:
    foreign_raw = os.environ.get("EDIT_MARKER_SCAN_TEST_SWAP_WITH", "")
    test_root_raw = os.environ.get("EDIT_MARKER_SCAN_TEST_ROOT", "")
    if not foreign_raw and not test_root_raw:
        return None
    if not foreign_raw or not test_root_raw:
        fail("TEST_HOOK_INVALID", "swap-back hook requires both fixture paths")
    foreign = Path(os.path.abspath(foreign_raw))
    test_root = Path(os.path.abspath(test_root_raw))
    try:
        directory.relative_to(test_root)
        foreign.relative_to(test_root)
    except ValueError:
        fail("TEST_HOOK_INVALID", "swap-back hook paths escape the fixture root")
    try:
        directory.relative_to(canonical_root)
        fail("TEST_HOOK_INVALID", "swap-back hook is inert in the canonical workspace")
    except ValueError:
        pass
    if test_root == canonical_root:
        fail("TEST_HOOK_INVALID", "canonical workspace cannot be a fixture root")
    opened = os.fstat(directory_fd)
    live = directory.lstat()
    foreign_stat = foreign.lstat()
    if (identity(opened) != identity(live) or stat.S_ISLNK(foreign_stat.st_mode) or
            not stat.S_ISDIR(foreign_stat.st_mode)):
        fail("TEST_HOOK_INVALID", "swap-back fixture directories are unsafe")
    backup = directory.parent / (directory.name + ".anchored-scan-original")
    if backup.exists() or backup.is_symlink():
        fail("TEST_HOOK_INVALID", "swap-back backup path already exists")
    os.rename(directory, backup)
    try:
        os.rename(foreign, directory)
    except BaseException:
        os.rename(backup, directory)
        raise
    return {"directory": directory, "foreign": foreign, "backup": backup}


def restore_swap(state: Optional[Dict[str, Path]]) -> None:
    if state is None:
        return
    directory = state["directory"]
    foreign = state["foreign"]
    backup = state["backup"]
    os.rename(directory, foreign)
    os.rename(backup, directory)


def scan(directory: Path, authority_root: Path, max_entries: int,
         max_file_bytes: int, max_total_bytes: int,
         canonical_root: Path) -> dict:
    chain, directory_fd = open_directory_anchored(directory, authority_root)
    if directory_fd is None:
        for item in reversed(chain):
            os.close(item["fd"])
        return {"version": 1, "missing": True, "entries": []}
    swap_state: Optional[Dict[str, Path]] = None
    try:
        opened = os.fstat(directory_fd)
        if not stat.S_ISDIR(opened.st_mode):
            fail("DIRECTORY_UNSAFE", "marker path is not a real directory")
        swap_state = fixture_swap(directory, directory_fd, canonical_root)
        names: List[str] = []
        with os.scandir(directory_fd) as iterator:
            for entry in iterator:
                if len(names) >= max_entries:
                    fail("ENTRY_LIMIT", f"marker directory exceeds the {max_entries}-entry limit")
                try:
                    entry.name.encode("utf-8")
                except UnicodeEncodeError:
                    fail("ENTRY_NAME_INVALID", "marker filename is not valid Unicode")
                names.append(entry.name)
        rows: List[dict] = []
        total_bytes = 0
        for name in sorted(names):
            before = os.stat(name, dir_fd=directory_fd, follow_symlinks=False)
            row: Dict[str, Any] = {"name": name, "kind": kind(before), "size": before.st_size}
            if NAME_RE.fullmatch(name) and stat.S_ISREG(before.st_mode):
                if before.st_size > max_file_bytes:
                    row["oversized"] = True
                else:
                    total_bytes += before.st_size
                    if total_bytes > max_total_bytes:
                        fail("TOTAL_BYTES_LIMIT",
                             f"marker corpus exceeds the {max_total_bytes}-byte limit")
                    raw = read_regular(directory_fd, name, before, max_file_bytes)
                    row["rawBase64"] = base64.b64encode(raw).decode("ascii")
            rows.append(row)
        after = os.fstat(directory_fd)
        if identity(after) != identity(opened):
            fail("DIRECTORY_CHANGED", "marker directory changed while being scanned")
        verify_canonical_target(chain, directory)
        return {"version": 1, "missing": False, "entries": rows}
    finally:
        try:
            restore_swap(swap_state)
        finally:
            for item in reversed(chain):
                try:
                    os.close(item["fd"])
                except OSError:
                    pass


def positive(raw: str, label: str, maximum: int) -> int:
    try:
        value = int(raw)
    except ValueError:
        fail("ARGUMENT_INVALID", f"{label} is not an integer")
    if value < 1 or value > maximum:
        fail("ARGUMENT_INVALID", f"{label} is outside its bound")
    return value


def main() -> int:
    try:
        if len(sys.argv) != 7:
            fail("ARGUMENT_INVALID", "expected directory, authority root, and four scan bounds")
        directory = Path(os.path.abspath(sys.argv[1]))
        authority_root = Path(os.path.abspath(sys.argv[2]))
        max_entries = positive(sys.argv[3], "maxEntries", 100_000)
        max_file_bytes = positive(sys.argv[4], "maxFileBytes", 16 * 1024 * 1024)
        max_total_bytes = positive(sys.argv[5], "maxTotalBytes", 64 * 1024 * 1024)
        canonical_root = Path(os.path.abspath(sys.argv[6]))
        emit({"ok": True, "snapshot": scan(
            directory, authority_root, max_entries, max_file_bytes,
            max_total_bytes, canonical_root)})
        return 0
    except RuntimeError as exc:
        try:
            detail = json.loads(str(exc))
        except json.JSONDecodeError:
            detail = {"code": "SCAN_FAILED", "message": str(exc)[:1200]}
        emit({"ok": False, "error": detail})
        return 0
    except BaseException as exc:
        emit({"ok": False, "error": {
            "code": "SCAN_FAILED", "message": f"{type(exc).__name__}: {str(exc)[:1000]}"}})
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
