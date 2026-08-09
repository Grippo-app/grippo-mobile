#!/usr/bin/env python3
"""Run one command in a kill-on-close Windows Job Object.

The child is created SUSPENDED, assigned to the Job, and only then resumed, so
there is no assign-after-spawn window in which it can create an untracked
descendant. stdout/stderr are inherited; stdin belongs to this wrapper as a
termination control pipe. A nonce-authenticated control line is written to
stderr only after assignment and again only after ActiveProcesses reaches 0.
"""

import ctypes
from ctypes import wintypes
import os
import subprocess
import sys
import threading
import time


READY = "WINDOWS_JOB_READY"
DRAINED = "WINDOWS_JOB_DRAINED"
UNVERIFIED = "WINDOWS_JOB_UNVERIFIED"


def control(kind, nonce, value=""):
    sys.stderr.write(f"{kind} {nonce}{(' ' + str(value)) if value != '' else ''}\n")
    sys.stderr.flush()


def main():
    if os.name != "nt":
        print("windows-job: Windows Job Objects are unavailable on this platform", file=sys.stderr)
        return 125
    argv = sys.argv[1:]
    if argv and argv[0] == "--":
        argv = argv[1:]
    if not argv:
        print("windows-job: expected command after --", file=sys.stderr)
        return 2
    nonce = os.environ.get("FINALIZATION_JOB_NONCE", "")
    if not nonce or len(nonce) > 128 or not all(ch in "0123456789abcdef" for ch in nonce):
        print("windows-job: FINALIZATION_JOB_NONCE is missing or invalid", file=sys.stderr)
        return 125

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
            ("ThisPeriodTotalUserTime", ctypes.c_longlong), ("ThisPeriodTotalKernelTime", ctypes.c_longlong),
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
    kernel32.CreateFileW.argtypes = [wintypes.LPCWSTR, wintypes.DWORD, wintypes.DWORD, ctypes.c_void_p, wintypes.DWORD, wintypes.DWORD, wintypes.HANDLE]
    kernel32.CreateFileW.restype = wintypes.HANDLE
    kernel32.SetHandleInformation.argtypes = [wintypes.HANDLE, wintypes.DWORD, wintypes.DWORD]
    kernel32.SetHandleInformation.restype = wintypes.BOOL

    JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x00002000
    JobObjectExtendedLimitInformation = 9
    JobObjectBasicAccountingInformation = 1
    CREATE_SUSPENDED = 0x00000004
    CREATE_NEW_PROCESS_GROUP = 0x00000200
    CREATE_UNICODE_ENVIRONMENT = 0x00000400
    STARTF_USESTDHANDLES = 0x00000100
    STD_OUTPUT_HANDLE = ctypes.c_ulong(-11).value
    STD_ERROR_HANDLE = ctypes.c_ulong(-12).value
    GENERIC_READ = 0x80000000
    FILE_SHARE_READ = 0x00000001
    FILE_SHARE_WRITE = 0x00000002
    OPEN_EXISTING = 3
    FILE_ATTRIBUTE_NORMAL = 0x80
    HANDLE_FLAG_INHERIT = 0x1
    WAIT_OBJECT_0 = 0
    WAIT_TIMEOUT = 258
    INVALID_HANDLE_VALUE = wintypes.HANDLE(-1).value

    job = kernel32.CreateJobObjectW(None, None)
    if not job:
        print(f"windows-job: CreateJobObjectW failed ({ctypes.get_last_error()})", file=sys.stderr)
        return 125
    process_info = PROCESS_INFORMATION()
    nul = wintypes.HANDLE()
    drained = False
    try:
        limits = JOBOBJECT_EXTENDED_LIMIT_INFORMATION()
        limits.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
        if not kernel32.SetInformationJobObject(job, JobObjectExtendedLimitInformation, ctypes.byref(limits), ctypes.sizeof(limits)):
            print(f"windows-job: SetInformationJobObject failed ({ctypes.get_last_error()})", file=sys.stderr)
            return 125

        out_handle = kernel32.GetStdHandle(STD_OUTPUT_HANDLE)
        err_handle = kernel32.GetStdHandle(STD_ERROR_HANDLE)
        nul = kernel32.CreateFileW("NUL", GENERIC_READ, FILE_SHARE_READ | FILE_SHARE_WRITE, None, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, None)
        if not nul or nul == INVALID_HANDLE_VALUE:
            print(f"windows-job: opening NUL failed ({ctypes.get_last_error()})", file=sys.stderr)
            return 125
        for handle in (out_handle, err_handle, nul):
            if not kernel32.SetHandleInformation(handle, HANDLE_FLAG_INHERIT, HANDLE_FLAG_INHERIT):
                print(f"windows-job: standard handle is not inheritable ({ctypes.get_last_error()})", file=sys.stderr)
                return 125

        startup = STARTUPINFOW()
        startup.cb = ctypes.sizeof(startup)
        startup.dwFlags = STARTF_USESTDHANDLES
        startup.hStdInput = nul
        startup.hStdOutput = out_handle
        startup.hStdError = err_handle

        child_env = dict(os.environ)
        child_env.pop("FINALIZATION_JOB_NONCE", None)
        env_block = "\0".join(f"{key}={value}" for key, value in sorted(child_env.items(), key=lambda item: item[0].upper())) + "\0\0"
        env_buffer = ctypes.create_unicode_buffer(env_block)
        command_buffer = ctypes.create_unicode_buffer(subprocess.list2cmdline(argv))
        flags = CREATE_SUSPENDED | CREATE_NEW_PROCESS_GROUP | CREATE_UNICODE_ENVIRONMENT
        if not kernel32.CreateProcessW(None, command_buffer, None, None, True, flags,
                                       ctypes.cast(env_buffer, ctypes.c_void_p), os.getcwd(),
                                       ctypes.byref(startup), ctypes.byref(process_info)):
            print(f"windows-job: CreateProcessW failed ({ctypes.get_last_error()})", file=sys.stderr)
            return 125
        if not kernel32.AssignProcessToJobObject(job, process_info.hProcess):
            kernel32.TerminateProcess(process_info.hProcess, 125)
            kernel32.WaitForSingleObject(process_info.hProcess, 5000)
            print(f"windows-job: AssignProcessToJobObject failed ({ctypes.get_last_error()})", file=sys.stderr)
            return 125
        if kernel32.ResumeThread(process_info.hThread) == 0xFFFFFFFF:
            kernel32.TerminateJobObject(job, 125)
            print(f"windows-job: ResumeThread failed ({ctypes.get_last_error()})", file=sys.stderr)
            return 125
        kernel32.CloseHandle(process_info.hThread)
        process_info.hThread = None
        control(READY, nonce, process_info.dwProcessId)

        stop = threading.Event()

        def watch_control():
            try:
                # TERMINATE or pipe EOF (server crash) both close the whole job.
                sys.stdin.buffer.readline()
            finally:
                stop.set()

        threading.Thread(target=watch_control, daemon=True).start()
        parent_exit = None
        while True:
            if stop.is_set():
                kernel32.TerminateJobObject(job, 143)
                break
            wait = kernel32.WaitForSingleObject(process_info.hProcess, 100)
            if wait == WAIT_OBJECT_0:
                code = wintypes.DWORD()
                if kernel32.GetExitCodeProcess(process_info.hProcess, ctypes.byref(code)):
                    parent_exit = int(code.value)
                break
            if wait != WAIT_TIMEOUT:
                kernel32.TerminateJobObject(job, 125)
                break

        # The finalizer parent is not allowed to leave background descendants.
        # TerminateJobObject is idempotent if every process already exited.
        kernel32.TerminateJobObject(job, parent_exit if parent_exit not in (None, 0) else 1)
        configured = int(os.environ.get("FINALIZATION_JOB_DRAIN_TIMEOUT_MS", "10000") or "10000")
        deadline = time.monotonic() + max(1.0, min(60.0, configured / 1000.0))
        while time.monotonic() < deadline:
            accounting = JOBOBJECT_BASIC_ACCOUNTING_INFORMATION()
            if kernel32.QueryInformationJobObject(job, JobObjectBasicAccountingInformation, ctypes.byref(accounting), ctypes.sizeof(accounting), None) and accounting.ActiveProcesses == 0:
                drained = True
                break
            time.sleep(0.05)
        if drained:
            control(DRAINED, nonce, parent_exit if parent_exit is not None else 143)
        else:
            control(UNVERIFIED, nonce)
        if not drained:
            return 125
        if stop.is_set() and parent_exit is None:
            return 143
        return min(255, max(0, parent_exit if parent_exit is not None else 125))
    finally:
        if process_info.hThread:
            kernel32.CloseHandle(process_info.hThread)
        if process_info.hProcess:
            kernel32.CloseHandle(process_info.hProcess)
        if nul and nul != INVALID_HANDLE_VALUE:
            kernel32.CloseHandle(nul)
        # KILL_ON_JOB_CLOSE is the final safety net if any earlier path failed.
        kernel32.CloseHandle(job)


if __name__ == "__main__":
    raise SystemExit(main())
