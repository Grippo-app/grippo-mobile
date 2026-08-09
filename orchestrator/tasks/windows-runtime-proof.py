#!/usr/bin/env python3
"""Native Windows proofs for shallow-intake process and filesystem authority.

The helper deliberately exposes a tiny, canonical JSON protocol.  It opens
filesystem objects by handle without following the final reparse point and it
never treats an API/permission failure as absence or privacy.
"""

from __future__ import annotations

import ctypes
from ctypes import wintypes
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import time


VERSION = 1
MAX_PID = 0x7FFFFFFF
FILE_ALL_ACCESS = 0x001F01FF
GENERIC_ALL = 0x10000000


class ACE_HEADER(ctypes.Structure):
    _fields_ = [
        ("AceType", ctypes.c_ubyte),
        ("AceFlags", ctypes.c_ubyte),
        ("AceSize", ctypes.c_ushort),
    ]


class ACL_SIZE_INFORMATION(ctypes.Structure):
    _fields_ = [
        ("AceCount", wintypes.DWORD),
        ("AclBytesInUse", wintypes.DWORD),
        ("AclBytesFree", wintypes.DWORD),
    ]


class BY_HANDLE_FILE_INFORMATION(ctypes.Structure):
    _fields_ = [
        ("dwFileAttributes", wintypes.DWORD),
        ("ftCreationTime", wintypes.FILETIME),
        ("ftLastAccessTime", wintypes.FILETIME),
        ("ftLastWriteTime", wintypes.FILETIME),
        ("dwVolumeSerialNumber", wintypes.DWORD),
        ("nFileSizeHigh", wintypes.DWORD),
        ("nFileSizeLow", wintypes.DWORD),
        ("nNumberOfLinks", wintypes.DWORD),
        ("nFileIndexHigh", wintypes.DWORD),
        ("nFileIndexLow", wintypes.DWORD),
    ]


class SID_AND_ATTRIBUTES(ctypes.Structure):
    _fields_ = [("Sid", ctypes.c_void_p), ("Attributes", wintypes.DWORD)]


class TOKEN_USER(ctypes.Structure):
    _fields_ = [("User", SID_AND_ATTRIBUTES)]


class PROCESS_BASIC_INFORMATION(ctypes.Structure):
    _fields_ = [
        ("Reserved1", ctypes.c_void_p),
        ("PebBaseAddress", ctypes.c_void_p),
        ("Reserved2", ctypes.c_void_p * 2),
        ("UniqueProcessId", ctypes.c_size_t),
        ("InheritedFromUniqueProcessId", ctypes.c_size_t),
    ]


def process_start_digest(boot_id: str, pid: int, creation_ticks: int) -> str:
    """Return the shared writer-process identity for a Windows generation."""
    if len(boot_id) != 32 or any(ch not in "0123456789abcdef" for ch in boot_id):
        raise ValueError("invalid Windows boot identity")
    body = "\0".join(("writer-process-v1", "win32", boot_id, str(pid), str(creation_ticks)))
    return "psid-v1:win32:" + hashlib.sha256(body.encode("utf-8")).hexdigest()


def canonical(value: dict) -> None:
    sys.stdout.write(json.dumps(value, sort_keys=True, separators=(",", ":")) + "\n")


def process_verdict(status: str, pid: int, process_start_id=None, reason="ok") -> dict:
    return {
        "pid": pid,
        "processStartId": process_start_id,
        "reason": reason,
        "status": status,
        "version": VERSION,
    }


def path_verdict(status: str, reason: str, path_type=None, dev=None, ino=None) -> dict:
    return {
        "dev": dev,
        "ino": ino,
        "pathType": path_type,
        "reason": reason,
        "status": status,
        "version": VERSION,
    }


def ancestry_verdict(status: str, descendant_pid: int, ancestor_pid: int,
                     depth=None, reason="ok") -> dict:
    return {
        "ancestorPid": ancestor_pid,
        "depth": depth,
        "descendantPid": descendant_pid,
        "reason": reason,
        "status": status,
        "version": VERSION,
    }


class WindowsApi:
    def __init__(self) -> None:
        self.kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
        self.advapi32 = ctypes.WinDLL("advapi32", use_last_error=True)
        self.ntdll = ctypes.WinDLL("ntdll", use_last_error=True)

        self.kernel32.OpenProcess.argtypes = [wintypes.DWORD, wintypes.BOOL, wintypes.DWORD]
        self.kernel32.OpenProcess.restype = wintypes.HANDLE
        self.kernel32.GetProcessTimes.argtypes = [
            wintypes.HANDLE,
            ctypes.POINTER(wintypes.FILETIME),
            ctypes.POINTER(wintypes.FILETIME),
            ctypes.POINTER(wintypes.FILETIME),
            ctypes.POINTER(wintypes.FILETIME),
        ]
        self.kernel32.GetProcessTimes.restype = wintypes.BOOL
        self.kernel32.GetExitCodeProcess.argtypes = [wintypes.HANDLE, ctypes.POINTER(wintypes.DWORD)]
        self.kernel32.GetExitCodeProcess.restype = wintypes.BOOL
        self.kernel32.CloseHandle.argtypes = [wintypes.HANDLE]
        self.kernel32.CloseHandle.restype = wintypes.BOOL
        self.kernel32.GetCurrentProcess.argtypes = []
        self.kernel32.GetCurrentProcess.restype = wintypes.HANDLE
        self.kernel32.CreateFileW.argtypes = [
            wintypes.LPCWSTR,
            wintypes.DWORD,
            wintypes.DWORD,
            ctypes.c_void_p,
            wintypes.DWORD,
            wintypes.DWORD,
            wintypes.HANDLE,
        ]
        self.kernel32.CreateFileW.restype = wintypes.HANDLE
        self.kernel32.GetFileInformationByHandle.argtypes = [
            wintypes.HANDLE,
            ctypes.POINTER(BY_HANDLE_FILE_INFORMATION),
        ]
        self.kernel32.GetFileInformationByHandle.restype = wintypes.BOOL
        self.kernel32.LocalFree.argtypes = [ctypes.c_void_p]
        self.kernel32.LocalFree.restype = ctypes.c_void_p

        self.advapi32.OpenProcessToken.argtypes = [
            wintypes.HANDLE,
            wintypes.DWORD,
            ctypes.POINTER(wintypes.HANDLE),
        ]
        self.advapi32.OpenProcessToken.restype = wintypes.BOOL
        self.advapi32.GetTokenInformation.argtypes = [
            wintypes.HANDLE,
            ctypes.c_int,
            ctypes.c_void_p,
            wintypes.DWORD,
            ctypes.POINTER(wintypes.DWORD),
        ]
        self.advapi32.GetTokenInformation.restype = wintypes.BOOL
        self.advapi32.CreateWellKnownSid.argtypes = [
            ctypes.c_int,
            ctypes.c_void_p,
            ctypes.c_void_p,
            ctypes.POINTER(wintypes.DWORD),
        ]
        self.advapi32.CreateWellKnownSid.restype = wintypes.BOOL
        self.advapi32.EqualSid.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
        self.advapi32.EqualSid.restype = wintypes.BOOL
        self.advapi32.IsValidSid.argtypes = [ctypes.c_void_p]
        self.advapi32.IsValidSid.restype = wintypes.BOOL
        self.advapi32.GetLengthSid.argtypes = [ctypes.c_void_p]
        self.advapi32.GetLengthSid.restype = wintypes.DWORD
        self.advapi32.GetSecurityInfo.argtypes = [
            wintypes.HANDLE,
            ctypes.c_int,
            wintypes.DWORD,
            ctypes.POINTER(ctypes.c_void_p),
            ctypes.POINTER(ctypes.c_void_p),
            ctypes.POINTER(ctypes.c_void_p),
            ctypes.POINTER(ctypes.c_void_p),
            ctypes.POINTER(ctypes.c_void_p),
        ]
        self.advapi32.GetSecurityInfo.restype = wintypes.DWORD
        self.advapi32.GetSecurityDescriptorControl.argtypes = [
            ctypes.c_void_p,
            ctypes.POINTER(ctypes.c_ushort),
            ctypes.POINTER(wintypes.DWORD),
        ]
        self.advapi32.GetSecurityDescriptorControl.restype = wintypes.BOOL
        self.advapi32.GetAclInformation.argtypes = [
            ctypes.c_void_p,
            ctypes.c_void_p,
            wintypes.DWORD,
            ctypes.c_int,
        ]
        self.advapi32.GetAclInformation.restype = wintypes.BOOL
        self.advapi32.GetAce.argtypes = [
            ctypes.c_void_p,
            wintypes.DWORD,
            ctypes.POINTER(ctypes.c_void_p),
        ]
        self.advapi32.GetAce.restype = wintypes.BOOL
        self.advapi32.InitializeAcl.argtypes = [ctypes.c_void_p, wintypes.DWORD, wintypes.DWORD]
        self.advapi32.InitializeAcl.restype = wintypes.BOOL
        self.advapi32.AddAccessAllowedAceEx.argtypes = [
            ctypes.c_void_p,
            wintypes.DWORD,
            wintypes.DWORD,
            wintypes.DWORD,
            ctypes.c_void_p,
        ]
        self.advapi32.AddAccessAllowedAceEx.restype = wintypes.BOOL
        self.advapi32.SetSecurityInfo.argtypes = [
            wintypes.HANDLE,
            ctypes.c_int,
            wintypes.DWORD,
            ctypes.c_void_p,
            ctypes.c_void_p,
            ctypes.c_void_p,
            ctypes.c_void_p,
        ]
        self.advapi32.SetSecurityInfo.restype = wintypes.DWORD
        self.ntdll.NtQuerySystemInformation.argtypes = [
            ctypes.c_int,
            ctypes.c_void_p,
            wintypes.ULONG,
            ctypes.POINTER(wintypes.ULONG),
        ]
        self.ntdll.NtQuerySystemInformation.restype = ctypes.c_long
        self.ntdll.NtQueryInformationProcess.argtypes = [
            wintypes.HANDLE,
            ctypes.c_int,
            ctypes.c_void_p,
            wintypes.ULONG,
            ctypes.POINTER(wintypes.ULONG),
        ]
        self.ntdll.NtQueryInformationProcess.restype = ctypes.c_long

    def close(self, handle) -> None:
        if handle:
            self.kernel32.CloseHandle(handle)


def _boot_id(api: WindowsApi) -> str:
    # SYSTEM_BOOT_ENVIRONMENT_INFORMATION begins with BootIdentifier (GUID).
    # Binding the process creation FILETIME to this kernel-generated boot GUID
    # prevents a wall-clock rollback plus PID reuse across reboots from
    # impersonating an earlier process generation.
    SYSTEM_BOOT_ENVIRONMENT_INFORMATION = 90
    buffer = ctypes.create_string_buffer(64)
    returned = wintypes.ULONG()
    status = api.ntdll.NtQuerySystemInformation(
        SYSTEM_BOOT_ENVIRONMENT_INFORMATION, buffer, len(buffer), ctypes.byref(returned)
    )
    if status != 0 or returned.value < 16:
        raise OSError("NtQuerySystemInformation(SystemBootEnvironmentInformation)")
    return bytes(buffer.raw[:16]).hex()


def _inspect_open_process(api: WindowsApi, pid: int, handle):
    """Return (canonical verdict, creation ticks) for one already-open generation."""
    STILL_ACTIVE = 259
    exit_code = wintypes.DWORD()
    if not api.kernel32.GetExitCodeProcess(handle, ctypes.byref(exit_code)):
        return process_verdict("unknown", pid, None, "query-failed"), None
    if exit_code.value != STILL_ACTIVE:
        return process_verdict("dead", pid, None, "exited"), None
    created = wintypes.FILETIME()
    exited = wintypes.FILETIME()
    kernel = wintypes.FILETIME()
    user = wintypes.FILETIME()
    if not api.kernel32.GetProcessTimes(
        handle, ctypes.byref(created), ctypes.byref(exited), ctypes.byref(kernel), ctypes.byref(user)
    ):
        return process_verdict("unknown", pid, None, "query-failed"), None
    ticks = (int(created.dwHighDateTime) << 32) | int(created.dwLowDateTime)
    if ticks <= 0:
        return process_verdict("unknown", pid, None, "invalid-time"), None
    try:
        boot_id = _boot_id(api)
    except OSError:
        return process_verdict("unknown", pid, None, "boot-query-failed"), None
    return process_verdict("live", pid, process_start_digest(boot_id, pid, ticks), "ok"), ticks


def _open_process(api: WindowsApi, pid: int):
    PROCESS_QUERY_LIMITED_INFORMATION = 0x1000
    ERROR_INVALID_PARAMETER = 87
    handle = api.kernel32.OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, False, pid)
    if not handle:
        error = ctypes.get_last_error()
        if error == ERROR_INVALID_PARAMETER:
            return None, process_verdict("dead", pid, None, "not-found")
        return None, process_verdict("unknown", pid, None, "open-failed")
    return handle, None


def inspect_process(api: WindowsApi, pid: int) -> dict:
    handle, error = _open_process(api, pid)
    if not handle:
        return error
    try:
        return _inspect_open_process(api, pid, handle)[0]
    finally:
        api.close(handle)


def _parent_pid(api: WindowsApi, handle) -> int:
    PROCESS_BASIC_INFORMATION_CLASS = 0
    value = PROCESS_BASIC_INFORMATION()
    returned = wintypes.ULONG()
    status = api.ntdll.NtQueryInformationProcess(
        handle, PROCESS_BASIC_INFORMATION_CLASS, ctypes.byref(value),
        ctypes.sizeof(value), ctypes.byref(returned),
    )
    parent = int(value.InheritedFromUniqueProcessId)
    if status != 0 or returned.value < ctypes.sizeof(value) or parent < 1 or parent > MAX_PID:
        raise OSError("NtQueryInformationProcess(ProcessBasicInformation)")
    return parent


def inspect_ancestry(api: WindowsApi, descendant_pid: int, descendant_start_id: str,
                     ancestor_pid: int, ancestor_start_id: str) -> dict:
    current_pid = descendant_pid
    child_ticks = None
    seen = set()
    chain = []
    deadline = time.monotonic() + 2.0
    for depth in range(65):
        if time.monotonic() > deadline:
            return ancestry_verdict("unknown", descendant_pid, ancestor_pid, None, "deadline")
        if current_pid in seen:
            return ancestry_verdict("mismatch", descendant_pid, ancestor_pid, None, "cycle")
        seen.add(current_pid)
        handle, open_error = _open_process(api, current_pid)
        if not handle:
            status = "mismatch" if open_error["status"] == "dead" else "unknown"
            return ancestry_verdict(status, descendant_pid, ancestor_pid, None, "process-" + open_error["reason"])
        try:
            verdict, ticks = _inspect_open_process(api, current_pid, handle)
            if verdict["status"] != "live" or ticks is None:
                status = "mismatch" if verdict["status"] == "dead" else "unknown"
                return ancestry_verdict(status, descendant_pid, ancestor_pid, None, "process-" + verdict["reason"])
            if depth == 0 and verdict["processStartId"] != descendant_start_id:
                return ancestry_verdict("mismatch", descendant_pid, ancestor_pid, None, "descendant-generation")
            # A real parent generation predates the child. This rejects a dead
            # parent's numeric PID after it has been reused by a newer process.
            if child_ticks is not None and ticks > child_ticks:
                return ancestry_verdict("mismatch", descendant_pid, ancestor_pid, None, "parent-newer")
            chain.append((current_pid, verdict["processStartId"]))
            if current_pid == ancestor_pid:
                if verdict["processStartId"] != ancestor_start_id:
                    return ancestry_verdict("mismatch", descendant_pid, ancestor_pid, None, "ancestor-generation")
                for check_pid, check_start in chain:
                    checked = inspect_process(api, check_pid)
                    if checked["status"] != "live" or checked["processStartId"] != check_start:
                        return ancestry_verdict("unknown", descendant_pid, ancestor_pid, None, "ancestry-changed")
                return ancestry_verdict("match", descendant_pid, ancestor_pid, depth, "ok")
            try:
                parent = _parent_pid(api, handle)
            except OSError:
                return ancestry_verdict("unknown", descendant_pid, ancestor_pid, None, "parent-query-failed")
            child_ticks = ticks
            current_pid = parent
        finally:
            api.close(handle)
    return ancestry_verdict("mismatch", descendant_pid, ancestor_pid, None, "depth-limit")


def _current_and_well_known_sids(api: WindowsApi):
    TOKEN_QUERY = 0x0008
    TOKEN_USER_CLASS = 1
    ERROR_INSUFFICIENT_BUFFER = 122
    token = wintypes.HANDLE()
    if not api.advapi32.OpenProcessToken(api.kernel32.GetCurrentProcess(), TOKEN_QUERY, ctypes.byref(token)):
        raise OSError("OpenProcessToken")
    try:
        required = wintypes.DWORD()
        api.advapi32.GetTokenInformation(token, TOKEN_USER_CLASS, None, 0, ctypes.byref(required))
        if ctypes.get_last_error() != ERROR_INSUFFICIENT_BUFFER or required.value < ctypes.sizeof(TOKEN_USER):
            raise OSError("GetTokenInformation(size)")
        user_buffer = ctypes.create_string_buffer(required.value)
        if not api.advapi32.GetTokenInformation(
            token, TOKEN_USER_CLASS, user_buffer, required.value, ctypes.byref(required)
        ):
            raise OSError("GetTokenInformation")
        user_sid = ctypes.cast(user_buffer, ctypes.POINTER(TOKEN_USER)).contents.User.Sid
        if not user_sid or not api.advapi32.IsValidSid(user_sid):
            raise OSError("invalid token SID")

        sid_buffers = []
        sid_values = [user_sid]
        # WELL_KNOWN_SID_TYPE: WinLocalSystemSid, WinBuiltinAdministratorsSid.
        for sid_type in (22, 26):
            size = wintypes.DWORD(68)
            buffer = ctypes.create_string_buffer(size.value)
            if not api.advapi32.CreateWellKnownSid(sid_type, None, buffer, ctypes.byref(size)):
                raise OSError("CreateWellKnownSid")
            sid_buffers.append(buffer)
            sid_values.append(ctypes.cast(buffer, ctypes.c_void_p).value)
        return user_buffer, sid_buffers, sid_values
    finally:
        api.close(token)


def _open_path(api: WindowsApi, path: str, write_dacl: bool):
    READ_CONTROL = 0x00020000
    WRITE_DAC = 0x00040000
    FILE_SHARE_ALL = 0x00000007
    OPEN_EXISTING = 3
    FILE_FLAG_OPEN_REPARSE_POINT = 0x00200000
    FILE_FLAG_BACKUP_SEMANTICS = 0x02000000
    INVALID_HANDLE_VALUE = ctypes.c_void_p(-1).value
    access = READ_CONTROL | (WRITE_DAC if write_dacl else 0)
    handle = api.kernel32.CreateFileW(
        path,
        access,
        FILE_SHARE_ALL,
        None,
        OPEN_EXISTING,
        FILE_FLAG_OPEN_REPARSE_POINT | FILE_FLAG_BACKUP_SEMANTICS,
        None,
    )
    if not handle or handle == INVALID_HANDLE_VALUE:
        error = ctypes.get_last_error()
        if error in (2, 3):
            return None, "missing"
        return None, "open-failed"
    return handle, None


def _identity(api: WindowsApi, handle):
    FILE_ATTRIBUTE_DIRECTORY = 0x00000010
    FILE_ATTRIBUTE_REPARSE_POINT = 0x00000400
    info = BY_HANDLE_FILE_INFORMATION()
    if not api.kernel32.GetFileInformationByHandle(handle, ctypes.byref(info)):
        raise OSError("GetFileInformationByHandle")
    path_type = "directory" if info.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY else "file"
    dev = str(int(info.dwVolumeSerialNumber))
    ino = str((int(info.nFileIndexHigh) << 32) | int(info.nFileIndexLow))
    return path_type, dev, ino, bool(info.dwFileAttributes & FILE_ATTRIBUTE_REPARSE_POINT)


def _equal_sid(api: WindowsApi, left, right) -> bool:
    return bool(left and right and api.advapi32.IsValidSid(left) and api.advapi32.IsValidSid(right)
                and api.advapi32.EqualSid(left, right))


def _security(api: WindowsApi, handle):
    SE_FILE_OBJECT = 1
    OWNER_SECURITY_INFORMATION = 0x00000001
    DACL_SECURITY_INFORMATION = 0x00000004
    owner = ctypes.c_void_p()
    dacl = ctypes.c_void_p()
    descriptor = ctypes.c_void_p()
    result = api.advapi32.GetSecurityInfo(
        handle,
        SE_FILE_OBJECT,
        OWNER_SECURITY_INFORMATION | DACL_SECURITY_INFORMATION,
        ctypes.byref(owner),
        None,
        ctypes.byref(dacl),
        None,
        ctypes.byref(descriptor),
    )
    if result != 0 or not descriptor.value:
        raise OSError("GetSecurityInfo")
    return owner, dacl, descriptor


def _private_dacl(api: WindowsApi, owner, dacl, descriptor, allowed_sids) -> str:
    SE_DACL_PROTECTED = 0x1000
    ACL_SIZE_INFORMATION_CLASS = 2
    ACCESS_ALLOWED_ACE_TYPE = 0
    if not _equal_sid(api, owner.value, allowed_sids[0]):
        return "owner"
    if not dacl.value:
        return "null-dacl"
    control = ctypes.c_ushort()
    revision = wintypes.DWORD()
    if not api.advapi32.GetSecurityDescriptorControl(descriptor, ctypes.byref(control), ctypes.byref(revision)):
        raise OSError("GetSecurityDescriptorControl")
    if not control.value & SE_DACL_PROTECTED:
        return "dacl-unprotected"
    acl_info = ACL_SIZE_INFORMATION()
    if not api.advapi32.GetAclInformation(
        dacl, ctypes.byref(acl_info), ctypes.sizeof(acl_info), ACL_SIZE_INFORMATION_CLASS
    ):
        raise OSError("GetAclInformation")
    current_user_full = False
    for index in range(int(acl_info.AceCount)):
        ace = ctypes.c_void_p()
        if not api.advapi32.GetAce(dacl, index, ctypes.byref(ace)) or not ace.value:
            raise OSError("GetAce")
        header = ctypes.cast(ace, ctypes.POINTER(ACE_HEADER)).contents
        # The hardened descriptor contains only ordinary allow ACEs.  Rejecting
        # every other ACE type is conservative: object/callback/compound forms
        # must never become an unparsed access grant.
        if header.AceType != ACCESS_ALLOWED_ACE_TYPE or header.AceSize < 12:
            return "ace-type"
        mask = ctypes.c_uint32.from_address(ace.value + 4).value
        sid = ctypes.c_void_p(ace.value + 8)
        if not api.advapi32.IsValidSid(sid):
            return "ace-sid"
        sid_length = int(api.advapi32.GetLengthSid(sid))
        if sid_length <= 0 or 8 + sid_length > int(header.AceSize):
            return "ace-size"
        principal = next((i for i, allowed in enumerate(allowed_sids) if _equal_sid(api, sid, allowed)), -1)
        if principal < 0:
            return "ace-principal"
        if principal == 0 and ((mask & FILE_ALL_ACCESS) == FILE_ALL_ACCESS or mask & GENERIC_ALL):
            current_user_full = True
    return "ok" if current_user_full else "current-user-access"


def _build_private_acl(api: WindowsApi, path_type: str, allowed_sids):
    ACL_REVISION = 2
    OBJECT_INHERIT_ACE = 0x01
    CONTAINER_INHERIT_ACE = 0x02
    sizes = [int(api.advapi32.GetLengthSid(sid)) for sid in allowed_sids]
    if any(size <= 0 for size in sizes):
        raise OSError("GetLengthSid")
    total = 8 + sum(8 + size for size in sizes)
    acl_buffer = ctypes.create_string_buffer(total)
    if not api.advapi32.InitializeAcl(acl_buffer, total, ACL_REVISION):
        raise OSError("InitializeAcl")
    flags = OBJECT_INHERIT_ACE | CONTAINER_INHERIT_ACE if path_type == "directory" else 0
    for sid in allowed_sids:
        if not api.advapi32.AddAccessAllowedAceEx(
            acl_buffer, ACL_REVISION, flags, FILE_ALL_ACCESS, sid
        ):
            raise OSError("AddAccessAllowedAceEx")
    return acl_buffer


def inspect_path(api: WindowsApi, raw_path: str, harden: bool, expected) -> dict:
    handle, open_error = _open_path(api, raw_path, harden)
    if handle is None:
        return path_verdict("missing", "missing") if open_error == "missing" else path_verdict("unknown", open_error)
    descriptor = None
    try:
        path_type, dev, ino, reparse = _identity(api, handle)
        if reparse:
            return path_verdict("unsafe", "reparse", path_type, dev, ino)
        if (dev, ino, path_type) != expected:
            # Most importantly, this check precedes SetSecurityInfo: a path
            # replacement can be reported but can never have its DACL changed
            # through an authority proof captured for another generation.
            return path_verdict("unsafe", "identity-changed", path_type, dev, ino)
        user_buffer, sid_buffers, allowed_sids = _current_and_well_known_sids(api)
        # Keep backing buffers alive until every SID comparison/API call ends.
        _ = (user_buffer, sid_buffers)
        owner, dacl, descriptor = _security(api, handle)
        reason = _private_dacl(api, owner, dacl, descriptor, allowed_sids)
        api.kernel32.LocalFree(descriptor)
        descriptor = None
        if harden:
            if reason == "owner":
                return path_verdict("unsafe", reason, path_type, dev, ino)
            acl_buffer = _build_private_acl(api, path_type, allowed_sids)
            SE_FILE_OBJECT = 1
            DACL_SECURITY_INFORMATION = 0x00000004
            PROTECTED_DACL_SECURITY_INFORMATION = 0x80000000
            result = api.advapi32.SetSecurityInfo(
                handle,
                SE_FILE_OBJECT,
                DACL_SECURITY_INFORMATION | PROTECTED_DACL_SECURITY_INFORMATION,
                None,
                None,
                acl_buffer,
                None,
            )
            if result != 0:
                return path_verdict("unknown", "set-dacl", path_type, dev, ino)
            owner, dacl, descriptor = _security(api, handle)
            reason = _private_dacl(api, owner, dacl, descriptor, allowed_sids)
        return path_verdict("private" if reason == "ok" else "unsafe", reason, path_type, dev, ino)
    except (OSError, ValueError, OverflowError):
        return path_verdict("unknown", "api")
    finally:
        if descriptor is not None and descriptor.value:
            api.kernel32.LocalFree(descriptor)
        api.close(handle)


def main(argv: list[str]) -> int:
    if len(argv) < 2 or argv[1] not in ("process", "ancestry", "private-path", "harden-path"):
        return 64
    if os.name != "nt":
        return 3
    api = WindowsApi()
    if argv[1] == "process":
        if len(argv) != 3 or not argv[2].isdigit():
            return 64
        pid = int(argv[2])
        if pid < 1 or pid > MAX_PID:
            return 64
        canonical(inspect_process(api, pid))
        return 0
    if argv[1] == "ancestry":
        if (len(argv) != 6 or not argv[2].isdigit() or not argv[4].isdigit() or
                not re.fullmatch(r"psid-v1:win32:[a-f0-9]{64}", argv[3]) or
                not re.fullmatch(r"psid-v1:win32:[a-f0-9]{64}", argv[5])):
            return 64
        descendant_pid, ancestor_pid = int(argv[2]), int(argv[4])
        if not (1 <= descendant_pid <= MAX_PID and 1 <= ancestor_pid <= MAX_PID):
            return 64
        canonical(inspect_ancestry(api, descendant_pid, argv[3], ancestor_pid, argv[5]))
        return 0
    if (len(argv) != 6 or not Path(argv[2]).is_absolute() or "\0" in argv[2]
            or not argv[3].isdigit() or not argv[4].isdigit() or argv[5] not in ("file", "directory")):
        return 64
    canonical(inspect_path(api, argv[2], argv[1] == "harden-path", (argv[3], argv[4], argv[5])))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
