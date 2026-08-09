#!/usr/bin/env python3
"""Run one shallow-intake model inside a drain-proven Windows Job Object.

The server durably binds this wrapper PID before sending ``GO <token>``.  The
model is then created suspended and assigned to a kill-on-close Job Object. It
is resumed only after the server durably binds that exact model PID and returns
``BOUND <token> <nonce> <pid>``. Wrapper/leader exit is never treated as
descendant-death proof: a nonce-authenticated DRAINED record is emitted only after Job
``ActiveProcesses == 0``.  stdin EOF (including a server crash) and TERMINATE
both terminate the complete Job tree.
"""

from __future__ import annotations

import argparse
import ctypes
from ctypes import wintypes
import hashlib
import os
import re
import shutil
import stat
import subprocess
import sys
import threading
import time
from pathlib import Path


READY = "INTAKE_WINDOWS_JOB_READY"
DRAINED = "INTAKE_WINDOWS_JOB_DRAINED"
UNVERIFIED = "INTAKE_WINDOWS_JOB_UNVERIFIED"
CONTROL_FD = -1


def control(kind: str, nonce: str, value="") -> None:
    if CONTROL_FD < 0:
        return
    suffix = f" {value}" if value != "" else ""
    data = f"{kind} {nonce}{suffix}\n".encode("ascii")
    try:
        while data:
            written = os.write(CONTROL_FD, data)
            if written <= 0:
                return
            data = data[written:]
    except OSError:
        # The site/control reader is gone. Job cleanup must still run.
        return


def expected_bound(token: str, nonce: str, pid: int) -> bytes:
    return f"BOUND {token} {nonce} {pid}\n".encode("ascii")


def read_exact_bound(stream, token: str, nonce: str, pid: int) -> bool:
    expected = expected_bound(token, nonce, pid)
    return stream.readline(len(expected) + 1) == expected


def bound_resume_state(stream, token: str, nonce: str, pid: int,
                       start_watcher, stop_is_set, resume_thread) -> str:
    """Authorize exactly one resume, with no callback before exact BOUND."""
    if not read_exact_bound(stream, token, nonce, pid):
        return "invalid"
    start_watcher()
    if stop_is_set():
        return "stopped"
    return "resume-failed" if resume_thread() == 0xFFFFFFFF else "resumed"


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(add_help=False)
    parser.add_argument("--token", required=True)
    parser.add_argument("--prompt", required=True)
    parser.add_argument("--prompt-sha256", required=True)
    for field in ("dev", "ino", "mode", "nlink", "size", "mtime-ns", "ctime-ns"):
        parser.add_argument(f"--prompt-{field}", required=True)
    parser.add_argument("--timeout-ms", required=True, type=int)
    parser.add_argument("command", nargs=argparse.REMAINDER)
    value = parser.parse_args()
    if value.command and value.command[0] == "--":
        value.command = value.command[1:]
    if not value.command or re.fullmatch(r"[A-Za-z0-9._:-]{8,160}", value.token) is None:
        parser.error("invalid command/token")
    if value.timeout_ms < 1000 or value.timeout_ms > 10 * 60 * 1000:
        parser.error("invalid timeout")
    if re.fullmatch(r"sha256:[a-f0-9]{64}", value.prompt_sha256) is None:
        parser.error("invalid prompt hash")
    for field in ("dev", "ino", "mode", "nlink", "size", "mtime_ns", "ctime_ns"):
        if re.fullmatch(r"(?:0|[1-9][0-9]*)", getattr(value, f"prompt_{field}")) is None:
            parser.error("invalid prompt proof")
    return value


def prompt_fd(path: Path, args: argparse.Namespace) -> int:
    fd = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
    opened = os.fstat(fd)
    expected = (
        int(args.prompt_dev), int(args.prompt_ino), int(args.prompt_mode), int(args.prompt_nlink),
        int(args.prompt_size), int(args.prompt_mtime_ns), int(args.prompt_ctime_ns),
    )
    observed = (opened.st_dev, opened.st_ino, opened.st_mode, opened.st_nlink, opened.st_size,
                opened.st_mtime_ns, opened.st_ctime_ns)
    if not stat.S_ISREG(opened.st_mode) or opened.st_nlink != 1 or opened.st_size > 128 * 1024 or observed != expected:
        os.close(fd)
        raise RuntimeError("unsafe, changed, or oversized prompt")
    digest = hashlib.sha256()
    remaining = opened.st_size
    while remaining:
        chunk = os.read(fd, min(65536, remaining))
        if not chunk:
            os.close(fd)
            raise RuntimeError("prompt read ended early")
        digest.update(chunk)
        remaining -= len(chunk)
    after = os.fstat(fd)
    if (after.st_dev, after.st_ino, after.st_mode, after.st_nlink, after.st_size,
            after.st_mtime_ns, after.st_ctime_ns) != observed or "sha256:" + digest.hexdigest() != args.prompt_sha256:
        os.close(fd)
        raise RuntimeError("prompt proof changed while hashing")
    os.lseek(fd, 0, os.SEEK_SET)
    os.set_inheritable(fd, True)
    return fd


def windows_command(argv: list[str]) -> list[str]:
    resolved = shutil.which(argv[0]) or argv[0]
    command = [resolved, *argv[1:]]
    if Path(resolved).suffix.lower() in (".cmd", ".bat"):
        comspec = os.environ.get("COMSPEC") or shutil.which("cmd.exe") or "cmd.exe"
        return [comspec, "/d", "/s", "/c", subprocess.list2cmdline(command)]
    return command


def main() -> int:
    global CONTROL_FD
    args = arguments()
    nonce = os.environ.get("SHALLOW_INTAKE_JOB_NONCE", "")
    if os.name != "nt":
        print("intake-windows-job: Windows Job Objects are unavailable", file=sys.stderr)
        return 125
    if not nonce or len(nonce) > 128 or any(ch not in "0123456789abcdef" for ch in nonce):
        print("intake-windows-job: SHALLOW_INTAKE_JOB_NONCE is missing or invalid", file=sys.stderr)
        return 125
    try:
        CONTROL_FD = int(os.environ.get("SHALLOW_INTAKE_JOB_CONTROL_FD", ""))
        if CONTROL_FD != 3:
            raise ValueError("unexpected descriptor")
        os.fstat(CONTROL_FD)
        # CreateProcessW below inherits selected stdio handles. The proof channel
        # is wrapper-only and must never be reachable by the model process.
        os.set_inheritable(CONTROL_FD, False)
    except (OSError, TypeError, ValueError):
        print("intake-windows-job: dedicated control descriptor is unavailable", file=sys.stderr)
        return 125

    expected = f"GO {args.token}\n".encode("ascii")
    if sys.stdin.buffer.readline(len(expected) + 1) != expected:
        # No model has been created; this is an authenticated empty-tree proof.
        control(DRAINED, nonce)
        return 72

    try:
        source_fd = prompt_fd(Path(args.prompt), args)
    except (OSError, RuntimeError) as error:
        print(f"intake-windows-job: {error}", file=sys.stderr)
        control(DRAINED, nonce)
        return 125
    try:

        stop = threading.Event()

        kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)

        class IO_COUNTERS(ctypes.Structure):
            _fields_ = [
                ("ReadOperationCount", ctypes.c_ulonglong), ("WriteOperationCount", ctypes.c_ulonglong),
                ("OtherOperationCount", ctypes.c_ulonglong), ("ReadTransferCount", ctypes.c_ulonglong),
                ("WriteTransferCount", ctypes.c_ulonglong), ("OtherTransferCount", ctypes.c_ulonglong),
            ]

        class JOBOBJECT_BASIC_LIMIT_INFORMATION(ctypes.Structure):
            _fields_ = [
                ("PerProcessUserTimeLimit", ctypes.c_longlong), ("PerJobUserTimeLimit", ctypes.c_longlong),
                ("LimitFlags", wintypes.DWORD), ("MinimumWorkingSetSize", ctypes.c_size_t),
                ("MaximumWorkingSetSize", ctypes.c_size_t), ("ActiveProcessLimit", wintypes.DWORD),
                ("Affinity", ctypes.c_size_t), ("PriorityClass", wintypes.DWORD),
                ("SchedulingClass", wintypes.DWORD),
            ]

        class JOBOBJECT_EXTENDED_LIMIT_INFORMATION(ctypes.Structure):
            _fields_ = [
                ("BasicLimitInformation", JOBOBJECT_BASIC_LIMIT_INFORMATION),
                ("IoInfo", IO_COUNTERS), ("ProcessMemoryLimit", ctypes.c_size_t),
                ("JobMemoryLimit", ctypes.c_size_t), ("PeakProcessMemoryUsed", ctypes.c_size_t),
                ("PeakJobMemoryUsed", ctypes.c_size_t),
            ]

        class JOBOBJECT_BASIC_ACCOUNTING_INFORMATION(ctypes.Structure):
            _fields_ = [
                ("TotalUserTime", ctypes.c_longlong), ("TotalKernelTime", ctypes.c_longlong),
                ("ThisPeriodTotalUserTime", ctypes.c_longlong), ("ThisPeriodKernelTime", ctypes.c_longlong),
                ("TotalPageFaultCount", wintypes.DWORD), ("TotalProcesses", wintypes.DWORD),
                ("ActiveProcesses", wintypes.DWORD), ("TotalTerminatedProcesses", wintypes.DWORD),
            ]

        class STARTUPINFOW(ctypes.Structure):
            _fields_ = [
                ("cb", wintypes.DWORD), ("lpReserved", wintypes.LPWSTR), ("lpDesktop", wintypes.LPWSTR),
                ("lpTitle", wintypes.LPWSTR), ("dwX", wintypes.DWORD), ("dwY", wintypes.DWORD),
                ("dwXSize", wintypes.DWORD), ("dwYSize", wintypes.DWORD),
                ("dwXCountChars", wintypes.DWORD), ("dwYCountChars", wintypes.DWORD),
                ("dwFillAttribute", wintypes.DWORD), ("dwFlags", wintypes.DWORD),
                ("wShowWindow", wintypes.WORD), ("cbReserved2", wintypes.WORD),
                ("lpReserved2", ctypes.POINTER(ctypes.c_byte)), ("hStdInput", wintypes.HANDLE),
                ("hStdOutput", wintypes.HANDLE), ("hStdError", wintypes.HANDLE),
            ]

        class PROCESS_INFORMATION(ctypes.Structure):
            _fields_ = [
                ("hProcess", wintypes.HANDLE), ("hThread", wintypes.HANDLE),
                ("dwProcessId", wintypes.DWORD), ("dwThreadId", wintypes.DWORD),
            ]

        kernel32.CreateJobObjectW.argtypes = [ctypes.c_void_p, wintypes.LPCWSTR]
        kernel32.CreateJobObjectW.restype = wintypes.HANDLE
        kernel32.SetInformationJobObject.argtypes = [wintypes.HANDLE, ctypes.c_int, ctypes.c_void_p, wintypes.DWORD]
        kernel32.SetInformationJobObject.restype = wintypes.BOOL
        kernel32.AssignProcessToJobObject.argtypes = [wintypes.HANDLE, wintypes.HANDLE]
        kernel32.AssignProcessToJobObject.restype = wintypes.BOOL
        kernel32.QueryInformationJobObject.argtypes = [wintypes.HANDLE, ctypes.c_int, ctypes.c_void_p, wintypes.DWORD, ctypes.c_void_p]
        kernel32.QueryInformationJobObject.restype = wintypes.BOOL
        kernel32.TerminateJobObject.argtypes = [wintypes.HANDLE, wintypes.UINT]
        kernel32.TerminateJobObject.restype = wintypes.BOOL
        kernel32.CreateProcessW.argtypes = [
            wintypes.LPCWSTR, wintypes.LPWSTR, ctypes.c_void_p, ctypes.c_void_p, wintypes.BOOL,
            wintypes.DWORD, ctypes.c_void_p, wintypes.LPCWSTR,
            ctypes.POINTER(STARTUPINFOW), ctypes.POINTER(PROCESS_INFORMATION),
        ]
        kernel32.CreateProcessW.restype = wintypes.BOOL
        kernel32.ResumeThread.argtypes = [wintypes.HANDLE]
        kernel32.ResumeThread.restype = wintypes.DWORD
        kernel32.WaitForSingleObject.argtypes = [wintypes.HANDLE, wintypes.DWORD]
        kernel32.WaitForSingleObject.restype = wintypes.DWORD
        kernel32.GetExitCodeProcess.argtypes = [wintypes.HANDLE, ctypes.POINTER(wintypes.DWORD)]
        kernel32.GetExitCodeProcess.restype = wintypes.BOOL
        kernel32.TerminateProcess.argtypes = [wintypes.HANDLE, wintypes.UINT]
        kernel32.TerminateProcess.restype = wintypes.BOOL
        kernel32.CloseHandle.argtypes = [wintypes.HANDLE]
        kernel32.CloseHandle.restype = wintypes.BOOL
        kernel32.GetStdHandle.argtypes = [wintypes.DWORD]
        kernel32.GetStdHandle.restype = wintypes.HANDLE
        kernel32.SetHandleInformation.argtypes = [wintypes.HANDLE, wintypes.DWORD, wintypes.DWORD]
        kernel32.SetHandleInformation.restype = wintypes.BOOL

        JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x00002000
        JOB_EXTENDED = 9
        JOB_ACCOUNTING = 1
        CREATE_SUSPENDED = 0x00000004
        CREATE_NEW_PROCESS_GROUP = 0x00000200
        CREATE_UNICODE_ENVIRONMENT = 0x00000400
        STARTF_USESTDHANDLES = 0x00000100
        STD_OUTPUT_HANDLE = ctypes.c_ulong(-11).value
        STD_ERROR_HANDLE = ctypes.c_ulong(-12).value
        HANDLE_FLAG_INHERIT = 0x1
        WAIT_OBJECT_0 = 0
        WAIT_TIMEOUT = 258

        job = kernel32.CreateJobObjectW(None, None)
        process_info = PROCESS_INFORMATION()
        assigned = False
        parent_exit = None
        exit_code = 125
        timed_out = False
        drained = False
        if not job:
            print(f"intake-windows-job: CreateJobObjectW failed ({ctypes.get_last_error()})", file=sys.stderr)
            control(DRAINED, nonce)
            return exit_code
        try:
            limits = JOBOBJECT_EXTENDED_LIMIT_INFORMATION()
            limits.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
            if not kernel32.SetInformationJobObject(job, JOB_EXTENDED, ctypes.byref(limits), ctypes.sizeof(limits)):
                raise RuntimeError(f"SetInformationJobObject failed ({ctypes.get_last_error()})")

            import msvcrt
            in_handle = wintypes.HANDLE(msvcrt.get_osfhandle(source_fd))
            out_handle = kernel32.GetStdHandle(STD_OUTPUT_HANDLE)
            err_handle = kernel32.GetStdHandle(STD_ERROR_HANDLE)
            for handle in (in_handle, out_handle, err_handle):
                if not kernel32.SetHandleInformation(handle, HANDLE_FLAG_INHERIT, HANDLE_FLAG_INHERIT):
                    raise RuntimeError(f"standard handle is not inheritable ({ctypes.get_last_error()})")

            startup = STARTUPINFOW()
            startup.cb = ctypes.sizeof(startup)
            startup.dwFlags = STARTF_USESTDHANDLES
            startup.hStdInput = in_handle
            startup.hStdOutput = out_handle
            startup.hStdError = err_handle

            child_env = dict(os.environ)
            child_env.pop("SHALLOW_INTAKE_JOB_NONCE", None)
            child_env.pop("SHALLOW_INTAKE_JOB_CONTROL_FD", None)
            env_block = "\0".join(f"{key}={value}" for key, value in sorted(child_env.items(), key=lambda item: item[0].upper())) + "\0\0"
            env_buffer = ctypes.create_unicode_buffer(env_block)
            command_buffer = ctypes.create_unicode_buffer(subprocess.list2cmdline(windows_command(args.command)))
            flags = CREATE_SUSPENDED | CREATE_NEW_PROCESS_GROUP | CREATE_UNICODE_ENVIRONMENT
            if not kernel32.CreateProcessW(None, command_buffer, None, None, True, flags,
                                           ctypes.cast(env_buffer, ctypes.c_void_p), os.getcwd(),
                                           ctypes.byref(startup), ctypes.byref(process_info)):
                raise RuntimeError(f"CreateProcessW failed ({ctypes.get_last_error()})")
            if not kernel32.AssignProcessToJobObject(job, process_info.hProcess):
                kernel32.TerminateProcess(process_info.hProcess, 125)
                kernel32.WaitForSingleObject(process_info.hProcess, 5000)
                raise RuntimeError(f"AssignProcessToJobObject failed ({ctypes.get_last_error()})")
            assigned = True
            control(READY, nonce, process_info.dwProcessId)
            # The primary thread is still suspended here. A missing, changed,
            # partial, or extended acknowledgement has no authority to run one
            # instruction of model code; cleanup below terminates the Job and
            # emits DRAINED only after ActiveProcesses reaches zero.
            def watch_control() -> None:
                try:
                    # TERMINATE or pipe EOF (site crash) owns the same outcome.
                    sys.stdin.buffer.readline()
                finally:
                    stop.set()

            gate_state = bound_resume_state(
                sys.stdin.buffer, args.token, nonce, process_info.dwProcessId,
                lambda: threading.Thread(target=watch_control, daemon=True).start(),
                stop.is_set,
                lambda: kernel32.ResumeThread(process_info.hThread),
            )
            if gate_state == "invalid":
                exit_code = 72
            elif gate_state == "stopped":
                exit_code = 143
            elif gate_state == "resume-failed":
                raise RuntimeError(f"ResumeThread failed ({ctypes.get_last_error()})")
            else:
                deadline = time.monotonic() + args.timeout_ms / 1000.0
                while True:
                    if stop.is_set():
                        exit_code = 143
                        break
                    if time.monotonic() >= deadline:
                        timed_out = True
                        exit_code = 73
                        break
                    wait = kernel32.WaitForSingleObject(process_info.hProcess, 100)
                    if wait == WAIT_OBJECT_0:
                        code = wintypes.DWORD()
                        if kernel32.GetExitCodeProcess(process_info.hProcess, ctypes.byref(code)):
                            parent_exit = int(code.value)
                            exit_code = min(255, max(0, parent_exit))
                        break
                    if wait != WAIT_TIMEOUT:
                        raise RuntimeError("WaitForSingleObject failed")
        except Exception as error:
            print(f"intake-windows-job: {error}", file=sys.stderr)
            exit_code = 125
        finally:
            if process_info.hThread:
                kernel32.CloseHandle(process_info.hThread)
                process_info.hThread = None
            if process_info.hProcess:
                if assigned:
                    # A successful model parent may still have descendants.  End
                    # the complete Job and prove the accounting count reached 0.
                    kernel32.TerminateJobObject(job, exit_code if exit_code != 0 else 1)
                else:
                    kernel32.TerminateProcess(process_info.hProcess, 125)
                direct_gone = assigned or kernel32.WaitForSingleObject(process_info.hProcess, 5000) == WAIT_OBJECT_0
                drain_raw = os.environ.get("SHALLOW_INTAKE_JOB_DRAIN_TIMEOUT_MS", "10000")
                try:
                    drain_ms = int(drain_raw)
                except ValueError:
                    drain_ms = 10000
                deadline = time.monotonic() + max(1.0, min(60.0, drain_ms / 1000.0))
                while time.monotonic() < deadline:
                    accounting = JOBOBJECT_BASIC_ACCOUNTING_INFORMATION()
                    if direct_gone and kernel32.QueryInformationJobObject(job, JOB_ACCOUNTING, ctypes.byref(accounting), ctypes.sizeof(accounting), None) and accounting.ActiveProcesses == 0:
                        drained = True
                        break
                    time.sleep(0.05)
                kernel32.CloseHandle(process_info.hProcess)
                process_info.hProcess = None
            else:
                # No process crossed CreateProcessW, hence the Job is empty.
                drained = True
            if drained:
                control(DRAINED, nonce)
            else:
                control(UNVERIFIED, nonce)
            kernel32.CloseHandle(job)
        return 73 if timed_out else exit_code
    finally:
        os.close(source_fd)


if __name__ == "__main__":
    raise SystemExit(main())
