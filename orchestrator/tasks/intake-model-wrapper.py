#!/usr/bin/env python3
"""Handshake and non-escapable containment for one intake model process.

It starts as the detached process-group leader, but does not launch the model
until the server has durably recorded that PID/PGID. If the server dies before
GO, EOF exits without launching Claude. After spawn it reports the direct PID
over a nonce-authenticated wrapper-only descriptor and accepts BOUND only after
the server has exact-CAS-bound that model generation; the model inherits neither
the descriptor nor nonce. The wrapper independently enforces the bounded timeout
and stays alive until its containment is empty:

* Linux pins the inspected executable, binds the direct model to wrapper death
  with ``PDEATHSIG=SIGKILL``, and installs a seccomp filter which forbids new
  process generations, process-group escape, and weakening the death signal.
  A subreaper drain remains as fail-closed defence in depth.
* macOS executes an exact, bounded private copy of the inspected model through
  the root-owned system sandbox. Native model binaries may fork, but
  ``process-exec`` is denied for every executable except that unique copy and
  the fixed, root-owned Keychain helper. Script fixtures retain the stronger
  no-fork profile. The wrapper uses ``libproc`` to prove both that its isolated
  process group is empty and that no post-baseline generation executing either
  allowed path remains anywhere on the host before reporting drain.

Wrapper-PGID disappearance is therefore an empty-containment proof, not merely
the death of one process-group generation.
"""

from __future__ import annotations

import argparse
import ctypes
import errno
import hashlib
import os
import re
import select
import shutil
import signal
import stat
import subprocess
import sys
import time
from pathlib import Path


READY = "INTAKE_POSIX_MODEL_READY"
DRAINED = "INTAKE_POSIX_MODEL_DRAINED"
EMPTY = "INTAKE_POSIX_MODEL_EMPTY"
CONTROL_FD = -1


def open_control() -> tuple[int, str]:
    """Open the wrapper-only proof channel inherited from the site process."""
    raw_fd = os.environ.get("SHALLOW_INTAKE_POSIX_CONTROL_FD", "")
    nonce = os.environ.get("SHALLOW_INTAKE_POSIX_CONTROL_NONCE", "")
    if raw_fd != "3" or re.fullmatch(r"[a-f0-9]{48}", nonce) is None:
        raise RuntimeError("dedicated model-identity control channel is unavailable")
    fd = int(raw_fd)
    opened = os.fstat(fd)
    if not (stat.S_ISFIFO(opened.st_mode) or stat.S_ISSOCK(opened.st_mode)):
        raise RuntimeError("model-identity control descriptor is not a pipe")
    # Popen below uses close_fds as well, but removing inheritability here makes
    # the parent-only authority explicit even if its launch details change.
    os.set_inheritable(fd, False)
    return fd, nonce


def control(fd: int, kind: str, nonce: str, value: int | None = None) -> None:
    suffix = f" {value}" if value is not None else ""
    data = f"{kind} {nonce}{suffix}\n".encode("ascii")
    try:
        while data:
            written = os.write(fd, data)
            if written <= 0:
                return
            data = data[written:]
    except OSError:
        # Losing the reader is never an empty-containment proof. The wrapper
        # still terminates and drains its direct child below.
        return


def await_binding(token: str, nonce: str, model_pid: int, timeout_ms: int,
                  should_stop: object, child: object) -> bool:
    """Wait for the site to acknowledge its durable exact-identity CAS."""
    expected = f"BOUND {token} {nonce} {model_pid}\n".encode("ascii")
    deadline = time.monotonic() + min(10.0, timeout_ms / 1000.0)
    fd = sys.stdin.fileno()
    while time.monotonic() < deadline and not should_stop():
        if child.poll() is not None:
            return False
        readable, _, _ = select.select([fd], [], [], 0.05)
        if not readable:
            continue
        line = sys.stdin.buffer.readline(len(expected) + 1)
        return line == expected
    return False


class GatedChild:
    """One forked PID which cannot exec model code until its parent releases it."""

    def __init__(self, pid: int, gate_fd: int) -> None:
        self.pid = pid
        self.gate_fd: int | None = gate_fd
        self.returncode: int | None = None

    def _record_status(self, status: int) -> int:
        self.returncode = os.waitstatus_to_exitcode(status)
        return self.returncode

    def poll(self) -> int | None:
        if self.returncode is not None:
            return self.returncode
        try:
            pid, status = os.waitpid(self.pid, os.WNOHANG)
        except ChildProcessError:
            return self.returncode
        return None if pid == 0 else self._record_status(status)

    def wait(self, timeout: float | None = None) -> int:
        if self.returncode is not None:
            return self.returncode
        deadline = None if timeout is None else time.monotonic() + timeout
        while True:
            value = self.poll()
            if value is not None:
                return value
            if deadline is not None and time.monotonic() >= deadline:
                raise subprocess.TimeoutExpired("gated-model", timeout)
            time.sleep(0.02)

    def terminate(self) -> None:
        os.kill(self.pid, signal.SIGTERM)

    def kill(self) -> None:
        os.kill(self.pid, signal.SIGKILL)

    def release(self, nonce: str) -> None:
        fd, self.gate_fd = self.gate_fd, None
        if fd is None:
            raise RuntimeError("model exec gate is already closed")
        data = f"EXEC {nonce}\n".encode("ascii")
        try:
            while data:
                written = os.write(fd, data)
                if written <= 0:
                    raise BrokenPipeError("model exec gate closed")
                data = data[written:]
        finally:
            os.close(fd)

    def cancel(self) -> None:
        fd, self.gate_fd = self.gate_fd, None
        if fd is not None:
            os.close(fd)


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
    if not value.command or not (8 <= len(value.token) <= 160):
        parser.error("invalid command/token")
    if value.timeout_ms < 1000 or value.timeout_ms > 10 * 60 * 1000:
        parser.error("invalid timeout")
    if re.fullmatch(r"sha256:[a-f0-9]{64}", value.prompt_sha256) is None:
        parser.error("invalid prompt hash")
    for field in ("dev", "ino", "mode", "nlink", "size", "mtime_ns", "ctime_ns"):
        raw = getattr(value, f"prompt_{field}")
        if re.fullmatch(r"(?:0|[1-9][0-9]*)", raw) is None:
            parser.error("invalid prompt proof")
    return value


def open_prompt(path: Path, args: argparse.Namespace) -> int:
    fd = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
    try:
        st = os.fstat(fd)
        expected = (
            int(args.prompt_dev), int(args.prompt_ino), int(args.prompt_mode), int(args.prompt_nlink),
            int(args.prompt_size), int(args.prompt_mtime_ns), int(args.prompt_ctime_ns),
        )
        observed = (st.st_dev, st.st_ino, st.st_mode, st.st_nlink, st.st_size, st.st_mtime_ns, st.st_ctime_ns)
        if (not stat.S_ISREG(st.st_mode) or st.st_nlink != 1 or st.st_size > 128 * 1024
                or stat.S_IMODE(st.st_mode) & 0o077
                or (hasattr(os, "geteuid") and st.st_uid != os.geteuid())
                or observed != expected):
            raise RuntimeError("unsafe, changed, or oversized prompt")
        digest = hashlib.sha256()
        remaining = st.st_size
        while remaining:
            chunk = os.read(fd, min(65536, remaining))
            if not chunk:
                raise RuntimeError("prompt read ended early")
            digest.update(chunk)
            remaining -= len(chunk)
        after = os.fstat(fd)
        if observed != (after.st_dev, after.st_ino, after.st_mode, after.st_nlink, after.st_size,
                        after.st_mtime_ns, after.st_ctime_ns):
            raise RuntimeError("prompt changed while hashing")
        if "sha256:" + digest.hexdigest() != args.prompt_sha256:
            raise RuntimeError("prompt hash does not match its publication proof")
        os.lseek(fd, 0, os.SEEK_SET)
        return fd
    except BaseException:
        os.close(fd)
        raise


DARWIN_SANDBOX = "/usr/bin/sandbox-exec"
DARWIN_SECURITY = "/usr/bin/security"
DARWIN_NO_FORK_PROFILE = "(version 1)(allow default)(deny process-fork)"
DARWIN_PINNED_EXECUTABLE = ".model-executable"
MAX_MODEL_EXECUTABLE_BYTES = 512 * 1024 * 1024
MAX_DARWIN_PROCESS_GROUP_BYTES = 1024 * 1024
PROC_ALL_PIDS = 1
PROC_PGRP_ONLY = 2
PROC_PIDPATHINFO_MAXSIZE = 4096
PR_SET_CHILD_SUBREAPER = 36
PR_GET_CHILD_SUBREAPER = 37
PR_SET_PDEATHSIG = 1
PR_GET_PDEATHSIG = 2
PR_SET_SECCOMP = 22
PR_GET_SECCOMP = 21
PR_SET_NO_NEW_PRIVS = 38
PR_GET_NO_NEW_PRIVS = 39
SECCOMP_MODE_FILTER = 2
SECCOMP_RET_KILL_PROCESS = 0x80000000
SECCOMP_RET_ERRNO = 0x00050000
SECCOMP_RET_ALLOW = 0x7FFF0000
CLONE_THREAD = 0x00010000


class SockFilter(ctypes.Structure):
    _fields_ = [
        ("code", ctypes.c_ushort),
        ("jt", ctypes.c_ubyte),
        ("jf", ctypes.c_ubyte),
        ("k", ctypes.c_uint32),
    ]


class SockFprog(ctypes.Structure):
    _fields_ = [("length", ctypes.c_ushort), ("filters", ctypes.POINTER(SockFilter))]


def linux_syscall_profile() -> tuple[int, int, int | None, int | None, int, int, int, int]:
    machine = os.uname().machine.lower()
    if machine in ("x86_64", "amd64"):
        return 0xC000003E, 56, 57, 58, 435, 157, 109, 112
    if machine in ("aarch64", "arm64"):
        return 0xC00000B7, 220, None, None, 435, 167, 154, 157
    raise RuntimeError("Linux no-fork containment is unsupported on this architecture")


def install_linux_child_contract(expected_parent: int) -> None:
    """Bind the direct model to this wrapper and forbid process descendants."""
    audit_arch, clone_nr, fork_nr, vfork_nr, clone3_nr, prctl_nr, setpgid_nr, setsid_nr = linux_syscall_profile()
    libc = ctypes.CDLL(None, use_errno=True)
    libc.prctl.argtypes = [ctypes.c_int, ctypes.c_ulong, ctypes.c_ulong, ctypes.c_ulong, ctypes.c_ulong]
    libc.prctl.restype = ctypes.c_int

    def checked_prctl(option: int, arg2: object = 0) -> int:
        raw_arg = arg2 if isinstance(arg2, int) else ctypes.cast(arg2, ctypes.c_void_p).value
        result = libc.prctl(option, int(raw_arg or 0), 0, 0, 0)
        if result < 0:
            raise OSError(ctypes.get_errno(), f"prctl({option}) failed")
        return int(result)

    checked_prctl(PR_SET_PDEATHSIG, signal.SIGKILL)
    death_signal = ctypes.c_int(0)
    checked_prctl(PR_GET_PDEATHSIG, ctypes.byref(death_signal))
    if death_signal.value != signal.SIGKILL or os.getppid() != expected_parent:
        os.kill(os.getpid(), signal.SIGKILL)

    checked_prctl(PR_SET_NO_NEW_PRIVS, 1)
    if checked_prctl(PR_GET_NO_NEW_PRIVS) != 1:
        raise RuntimeError("Linux no_new_privs could not be verified")

    # Classic BPF over struct seccomp_data. Unknown architectures are killed;
    # clone3 reports ENOSYS so libc can fall back to clone for pthreads; clone
    # itself is allowed only for CLONE_THREAD generations in this process.
    instructions: list[tuple[int, int, int, int]] = [
        (0x20, 0, 0, 4),                         # LD  arch
        (0x15, 1, 0, audit_arch),                # JEQ expected, skip kill
        (0x06, 0, 0, SECCOMP_RET_KILL_PROCESS),
        (0x20, 0, 0, 0),                         # LD  nr
    ]
    if audit_arch == 0xC000003E:
        instructions.extend([
            (0x45, 0, 1, 0x40000000),             # JSET __X32_SYSCALL_BIT
            (0x06, 0, 0, SECCOMP_RET_KILL_PROCESS),
        ])
    instructions.extend([
        # The child may set names and inspect prctl state, but it cannot clear
        # or replace the wrapper-death signal installed above.
        (0x15, 0, 3, prctl_nr),
        (0x20, 0, 0, 16),                         # LD  args[0] low word
        (0x15, 0, 1, PR_SET_PDEATHSIG),
        (0x06, 0, 0, SECCOMP_RET_ERRNO | errno.EPERM),
        (0x20, 0, 0, 0),                          # LD  nr again
        (0x15, 0, 1, clone3_nr),
        (0x06, 0, 0, SECCOMP_RET_ERRNO | errno.ENOSYS),
    ])
    for syscall_nr in (fork_nr, vfork_nr):
        if syscall_nr is not None:
            instructions.extend([
                (0x15, 0, 1, syscall_nr),
                (0x06, 0, 0, SECCOMP_RET_ERRNO | errno.EPERM),
            ])
    for syscall_nr in (setpgid_nr, setsid_nr):
        instructions.extend([
            (0x15, 0, 1, syscall_nr),
            (0x06, 0, 0, SECCOMP_RET_ERRNO | errno.EPERM),
        ])
    instructions.extend([
        (0x15, 0, 4, clone_nr),                  # non-clone -> ALLOW
        (0x20, 0, 0, 16),                        # LD  args[0] low word
        (0x54, 0, 0, CLONE_THREAD),              # AND CLONE_THREAD
        (0x15, 0, 1, 0),                         # zero -> deny
        (0x06, 0, 0, SECCOMP_RET_ERRNO | errno.EPERM),
        (0x06, 0, 0, SECCOMP_RET_ALLOW),
    ])
    filters = (SockFilter * len(instructions))(*(SockFilter(*row) for row in instructions))
    program = SockFprog(len(instructions), filters)
    checked_prctl(PR_SET_SECCOMP, ctypes.byref(program))
    if checked_prctl(PR_GET_SECCOMP) != SECCOMP_MODE_FILTER:
        raise RuntimeError("Linux seccomp filter could not be verified")


StatIdentity = tuple[int, int, int, int, int, int, int]
PinnedExecutable = tuple[Path, StatIdentity]
PinnedExecutables = tuple[PinnedExecutable, ...]
MACH_O_MAGICS = frozenset({
    b"\xfe\xed\xfa\xce", b"\xce\xfa\xed\xfe",
    b"\xfe\xed\xfa\xcf", b"\xcf\xfa\xed\xfe",
    b"\xca\xfe\xba\xbe", b"\xbe\xba\xfe\xca",
    b"\xca\xfe\xba\xbf", b"\xbf\xba\xfe\xca",
})


def stat_identity(value: os.stat_result) -> StatIdentity:
    return (value.st_dev, value.st_ino, value.st_mode, value.st_nlink,
            value.st_size, value.st_mtime_ns, value.st_ctime_ns)


def pin_darwin_executable(executable: str, directory: Path,
                          target_name: str = DARWIN_PINNED_EXECUTABLE,
                          kind: str = "model") -> PinnedExecutable:
    """Publish an exact private executable copy from one stable source fd."""
    parent = os.lstat(directory)
    if (not stat.S_ISDIR(parent.st_mode) or stat.S_ISLNK(parent.st_mode)
            or (hasattr(os, "geteuid") and parent.st_uid != os.geteuid())
            or stat.S_IMODE(parent.st_mode) & 0o077):
        raise RuntimeError(f"macOS {kind} pin directory is unsafe")
    target = directory / target_name
    source_fd = os.open(executable, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
    target_fd: int | None = None
    directory_fd: int | None = None
    created_file_id: tuple[int, int] | None = None
    published = False
    try:
        before = os.fstat(source_fd)
        if (not stat.S_ISREG(before.st_mode) or not before.st_mode & 0o111
                or before.st_mode & (stat.S_ISUID | stat.S_ISGID)
                or before.st_size < 1 or before.st_size > MAX_MODEL_EXECUTABLE_BYTES):
            raise RuntimeError(f"macOS {kind} executable is unsafe or oversized")
        target_fd = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL
                            | getattr(os, "O_NOFOLLOW", 0), 0o700)
        created = os.fstat(target_fd)
        created_file_id = (created.st_dev, created.st_ino)
        os.fchmod(target_fd, 0o700)
        copied_digest = hashlib.sha256()
        remaining = before.st_size
        while remaining:
            chunk = os.read(source_fd, min(1024 * 1024, remaining))
            if not chunk:
                raise RuntimeError(f"macOS {kind} executable ended during copy")
            copied_digest.update(chunk)
            remaining -= len(chunk)
            offset = 0
            while offset < len(chunk):
                written = os.write(target_fd, chunk[offset:])
                if written <= 0:
                    raise RuntimeError(f"macOS {kind} executable copy made no progress")
                offset += written
        os.fsync(target_fd)
        copied = os.fstat(target_fd)
        if (not stat.S_ISREG(copied.st_mode) or copied.st_nlink != 1
                or copied.st_size != before.st_size
                or stat.S_IMODE(copied.st_mode) != 0o700
                or (hasattr(os, "geteuid") and copied.st_uid != os.geteuid())):
            raise RuntimeError(f"macOS pinned {kind} executable is unsafe")
        directory_fd = os.open(directory, os.O_RDONLY | getattr(os, "O_DIRECTORY", 0)
                               | getattr(os, "O_NOFOLLOW", 0))
        opened_parent = os.fstat(directory_fd)
        if (opened_parent.st_dev != parent.st_dev or opened_parent.st_ino != parent.st_ino):
            raise RuntimeError(f"macOS {kind} pin directory changed")
        os.fsync(directory_fd)

        # A second bounded pass proves the source generation did not change
        # while it was copied. Metadata identity alone is insufficient for a
        # writer which can mutate and restore file length/mtime.
        os.lseek(source_fd, 0, os.SEEK_SET)
        verified_digest = hashlib.sha256()
        remaining = before.st_size
        while remaining:
            chunk = os.read(source_fd, min(1024 * 1024, remaining))
            if not chunk:
                raise RuntimeError(f"macOS {kind} executable ended during verification")
            verified_digest.update(chunk)
            remaining -= len(chunk)
        after = os.fstat(source_fd)
        if stat_identity(after) != stat_identity(before) or verified_digest.digest() != copied_digest.digest():
            raise RuntimeError(f"macOS {kind} executable changed while being pinned")
        proof = stat_identity(copied)
        published = True
        return target, proof
    finally:
        if target_fd is not None:
            os.close(target_fd)
        if directory_fd is not None:
            os.close(directory_fd)
        os.close(source_fd)
        if not published and created_file_id is not None:
            try:
                failed_target = os.lstat(target)
                if (stat.S_ISREG(failed_target.st_mode) and
                        (failed_target.st_dev, failed_target.st_ino) == created_file_id):
                    os.unlink(target)
            except FileNotFoundError:
                pass


def cleanup_pinned_executable(pinned: PinnedExecutable | None) -> bool:
    """Remove only the exact private copy generation created by this wrapper."""
    if pinned is None:
        return True
    path, expected = pinned
    try:
        observed = os.lstat(path)
    except FileNotFoundError:
        return True
    if (not stat.S_ISREG(observed.st_mode) or stat.S_ISLNK(observed.st_mode)
            or observed.st_nlink != 1 or stat_identity(observed) != expected):
        return False
    try:
        os.unlink(path)
        directory_fd = os.open(path.parent, os.O_RDONLY | getattr(os, "O_DIRECTORY", 0)
                               | getattr(os, "O_NOFOLLOW", 0))
        try:
            os.fsync(directory_fd)
        finally:
            os.close(directory_fd)
        try:
            os.lstat(path)
            return False
        except FileNotFoundError:
            return True
    except OSError:
        return False


def cleanup_pinned_executables(pinned: PinnedExecutables | None) -> bool:
    """Clean every exact per-request executable without hiding a failed proof."""
    if pinned is None:
        return True
    clean = True
    for item in reversed(pinned):
        clean = cleanup_pinned_executable(item) and clean
    return clean


def darwin_sandbox_literal(value: str) -> str:
    """Encode one already-resolved path as an SBPL string, without injection."""
    if not value or "\x00" in value or any(ord(character) < 0x20 for character in value):
        raise RuntimeError("macOS sandbox path contains an unsafe character")
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'


def darwin_native_executable(path: Path) -> bool:
    fd = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
    try:
        return os.read(fd, 4) in MACH_O_MAGICS
    finally:
        os.close(fd)


def darwin_exec_profile(model: Path) -> str:
    """Allow only the unique model copy and fixed root-owned helper."""
    helper = os.lstat(DARWIN_SECURITY)
    if (not stat.S_ISREG(helper.st_mode) or stat.S_ISLNK(helper.st_mode)
            or helper.st_uid != 0 or stat.S_IMODE(helper.st_mode) & 0o022
            or not helper.st_mode & 0o111):
        raise RuntimeError("macOS Keychain helper is unsafe")
    return "".join((
        "(version 1)(allow default)(deny process-exec)",
        f"(allow process-exec (literal {darwin_sandbox_literal(os.path.realpath(model))}))",
        f"(allow process-exec (literal {darwin_sandbox_literal(DARWIN_SECURITY)}))",
        f"(deny file-write* (subpath {darwin_sandbox_literal(os.path.realpath(model.parent))}))",
    ))


def contained_command(command: list[str], pin_directory: Path) -> tuple[list[str], bool, int | None, PinnedExecutables | None]:
    """Return the fixed command and its platform-specific executable pin."""
    executable = shutil.which(command[0])
    if executable is None:
        raise FileNotFoundError(command[0])
    executable = os.path.realpath(executable)
    executable_stat = os.stat(executable, follow_symlinks=False)
    if (not stat.S_ISREG(executable_stat.st_mode) or not os.access(executable, os.X_OK)
            or executable_stat.st_mode & (stat.S_ISUID | stat.S_ISGID)):
        raise FileNotFoundError(command[0])
    command = [executable, *command[1:]]
    if sys.platform.startswith("linux"):
        linux_syscall_profile()
        executable_fd = os.open(executable, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
        pinned = os.fstat(executable_fd)
        if (not stat.S_ISREG(pinned.st_mode) or not pinned.st_mode & 0o111
                or pinned.st_mode & (stat.S_ISUID | stat.S_ISGID)):
            os.close(executable_fd)
            raise RuntimeError("Linux model executable is unsafe")
        libc = ctypes.CDLL(None, use_errno=True)
        libc.prctl.argtypes = [ctypes.c_int, ctypes.c_ulong, ctypes.c_ulong, ctypes.c_ulong, ctypes.c_ulong]
        libc.prctl.restype = ctypes.c_int
        if libc.prctl(PR_SET_CHILD_SUBREAPER, 1, 0, 0, 0) != 0:
            os.close(executable_fd)
            raise RuntimeError(f"cannot enable Linux child subreaper ({ctypes.get_errno()})")
        enabled = ctypes.c_int(0)
        enabled_pointer = ctypes.cast(ctypes.byref(enabled), ctypes.c_void_p).value
        if libc.prctl(PR_GET_CHILD_SUBREAPER, int(enabled_pointer or 0), 0, 0, 0) != 0 or enabled.value != 1:
            os.close(executable_fd)
            raise RuntimeError("Linux child subreaper could not be verified")
        return [f"/proc/self/fd/{executable_fd}", *command[1:]], True, executable_fd, None
    if sys.platform == "darwin":
        sandbox = os.lstat(DARWIN_SANDBOX)
        if (not stat.S_ISREG(sandbox.st_mode) or sandbox.st_uid != 0 or
                stat.S_IMODE(sandbox.st_mode) & 0o022):
            raise RuntimeError("macOS containment binary is unsafe")
        if os.getpgrp() != os.getpid():
            raise RuntimeError("macOS containment wrapper lacks an isolated process group")
        model = pin_darwin_executable(executable, pin_directory)
        pinned: PinnedExecutables = (model,)
        profile = darwin_exec_profile(model[0]) if darwin_native_executable(model[0]) else DARWIN_NO_FORK_PROFILE
        return [DARWIN_SANDBOX, "-p", profile, str(model[0]), *command[1:]], False, None, pinned
    raise RuntimeError("shallow-intake containment is unsupported on this platform")


def gated_child_exec(command: list[str], model_env: dict[str, str], prompt_fd: int,
                     gate_read: int, gate_write: int, control_fd: int, nonce: str,
                     linux_containment: bool, executable_fd: int | None,
                     expected_parent: int) -> None:
    """Trusted pre-exec gate. Model bytes cannot run before the exact BOUND."""
    try:
        signal.signal(signal.SIGTERM, signal.SIG_DFL)
        signal.signal(signal.SIGINT, signal.SIG_DFL)
        os.close(gate_write)
        os.close(control_fd)
        if linux_containment:
            install_linux_child_contract(expected_parent)
        expected = f"EXEC {nonce}\n".encode("ascii")
        received = b""
        while len(received) <= len(expected):
            chunk = os.read(gate_read, len(expected) + 1 - len(received))
            if not chunk:
                break
            received += chunk
            if b"\n" in received:
                break
        if received != expected:
            os._exit(126)
        os.close(gate_read)
        if prompt_fd != 0:
            os.dup2(prompt_fd, 0, inheritable=True)
            os.close(prompt_fd)
        if executable_fd is not None:
            # `/proc/self/fd/N` is the already-inspected executable authority.
            # Keep it through exec path resolution, as Popen(pass_fds) did.
            os.set_inheritable(executable_fd, True)
        os.execve(command[0], command, model_env)
    except BaseException as error:
        try:
            message = f"intake-model-wrapper: gated exec failed: {error}\n".encode("utf-8", "replace")[:1024]
            os.write(2, message)
        except BaseException:
            pass
        os._exit(126)


def spawn_gated_child(command: list[str], model_env: dict[str, str], prompt_fd: int,
                      control_fd: int, nonce: str, linux_containment: bool,
                      executable_fd: int | None) -> GatedChild:
    gate_read, gate_write = os.pipe()
    expected_parent = os.getpid()
    try:
        pid = os.fork()
    except BaseException:
        os.close(gate_read)
        os.close(gate_write)
        raise
    if pid == 0:
        gated_child_exec(command, model_env, prompt_fd, gate_read, gate_write,
                         control_fd, nonce, linux_containment, executable_fd,
                         expected_parent)
        os._exit(126)
    os.close(gate_read)
    return GatedChild(pid, gate_write)


def reap_linux_children() -> bool:
    """Reap exited adopted children; return True while at least one is live."""
    while True:
        try:
            pid, _status = os.waitpid(-1, os.WNOHANG)
        except ChildProcessError:
            return False
        if pid == 0:
            return True


def linux_direct_children() -> list[int]:
    source = Path(f"/proc/{os.getpid()}/task/{os.getpid()}/children")
    with source.open("rb") as stream:
        raw = stream.read(1024 * 1024 + 1)
    if len(raw) > 1024 * 1024:
        raise RuntimeError("Linux child inventory exceeds its containment limit")
    try:
        tokens = raw.decode("ascii").split()
    except UnicodeDecodeError as exc:
        raise RuntimeError("Linux child inventory is malformed") from exc
    if len(tokens) > 131072 or any(not token.isdigit() for token in tokens):
        raise RuntimeError("Linux child inventory is malformed or oversized")
    pids = [int(token) for token in tokens]
    if any(pid <= 0 or pid > 0x7FFFFFFF for pid in pids) or len(set(pids)) != len(pids):
        raise RuntimeError("Linux child inventory contains an invalid PID")
    return pids


def drain_linux_descendants() -> None:
    """Terminate every adopted generation and do not exit until none remain."""
    phase = signal.SIGTERM
    kill_at = time.monotonic() + 1.0
    while reap_linux_children():
        try:
            children = linux_direct_children()
        except (OSError, RuntimeError):
            # A failed self-/proc proof is not permission to let the wrapper
            # disappear. Keep containment alive and retry indefinitely.
            time.sleep(0.05)
            continue
        for pid in children:
            try:
                os.kill(pid, phase)
            except ProcessLookupError:
                pass
            except (PermissionError, OSError):
                # These are our direct children. Any inability to signal is an
                # unknown drain state; retry and keep the wrapper alive.
                pass
        if phase == signal.SIGTERM and time.monotonic() >= kill_at:
            phase = signal.SIGKILL
        time.sleep(0.02)


def darwin_process_group_members(process_group: int) -> list[int]:
    """Return one complete bounded libproc snapshot for an isolated group."""
    library = ctypes.CDLL("/usr/lib/libproc.dylib", use_errno=True)
    listing = library.proc_listpids
    listing.argtypes = [ctypes.c_uint32, ctypes.c_uint32, ctypes.c_void_p, ctypes.c_int]
    listing.restype = ctypes.c_int
    estimated = listing(PROC_PGRP_ONLY, process_group, None, 0)
    if estimated <= 0:
        raise RuntimeError("macOS process-group inventory is unavailable")
    capacity = max(4096, estimated + 4096)
    while capacity <= MAX_DARWIN_PROCESS_GROUP_BYTES:
        buffer = ctypes.create_string_buffer(capacity)
        used = listing(PROC_PGRP_ONLY, process_group, buffer, capacity)
        if used < 0 or used > capacity or used % ctypes.sizeof(ctypes.c_int):
            raise RuntimeError("macOS process-group inventory is malformed")
        if used < capacity:
            count = used // ctypes.sizeof(ctypes.c_int)
            raw = (ctypes.c_int * count).from_buffer_copy(buffer.raw[:used]) if count else []
            pids = [int(pid) for pid in raw if int(pid) != 0]
            if (len(pids) != len(set(pids)) or any(pid <= 0 or pid > 0x7FFFFFFF for pid in pids)):
                raise RuntimeError("macOS process-group inventory contains an invalid PID")
            return pids
        capacity *= 2
    raise RuntimeError("macOS process-group inventory exceeds its containment limit")


class DarwinProcBsdInfo(ctypes.Structure):
    _fields_ = [
        ("pbi_flags", ctypes.c_uint32), ("pbi_status", ctypes.c_uint32),
        ("pbi_xstatus", ctypes.c_uint32), ("pbi_pid", ctypes.c_uint32),
        ("pbi_ppid", ctypes.c_uint32), ("pbi_uid", ctypes.c_uint32),
        ("pbi_gid", ctypes.c_uint32), ("pbi_ruid", ctypes.c_uint32),
        ("pbi_rgid", ctypes.c_uint32), ("pbi_svuid", ctypes.c_uint32),
        ("pbi_svgid", ctypes.c_uint32), ("rfu_1", ctypes.c_uint32),
        ("pbi_comm", ctypes.c_char * 16), ("pbi_name", ctypes.c_char * 32),
        ("pbi_nfiles", ctypes.c_uint32), ("pbi_pgid", ctypes.c_uint32),
        ("pbi_pjobc", ctypes.c_uint32), ("e_tdev", ctypes.c_uint32),
        ("e_tpgid", ctypes.c_uint32), ("pbi_nice", ctypes.c_int32),
        ("pbi_start_tvsec", ctypes.c_uint64), ("pbi_start_tvusec", ctypes.c_uint64),
    ]


DarwinProcessIdentity = tuple[int, int, int]


DarwinExecutableSnapshot = tuple[
    dict[bytes, set[DarwinProcessIdentity]], frozenset[DarwinProcessIdentity]
]


def darwin_executable_snapshot(paths: set[Path]) -> DarwinExecutableSnapshot:
    """Find target generations plus same-UID processes whose path is unknown."""
    library = ctypes.CDLL("/usr/lib/libproc.dylib", use_errno=True)
    listing = library.proc_listpids
    listing.argtypes = [ctypes.c_uint32, ctypes.c_uint32, ctypes.c_void_p, ctypes.c_int]
    listing.restype = ctypes.c_int
    pidpath = library.proc_pidpath
    pidpath.argtypes = [ctypes.c_int, ctypes.c_void_p, ctypes.c_uint32]
    pidpath.restype = ctypes.c_int
    pidinfo = library.proc_pidinfo
    pidinfo.argtypes = [ctypes.c_int, ctypes.c_int, ctypes.c_uint64, ctypes.c_void_p, ctypes.c_int]
    pidinfo.restype = ctypes.c_int
    estimated = listing(PROC_ALL_PIDS, 0, None, 0)
    if estimated <= 0:
        raise RuntimeError("macOS global process inventory is unavailable")
    capacity = max(4096, estimated + 4096)
    while capacity <= MAX_DARWIN_PROCESS_GROUP_BYTES:
        buffer = ctypes.create_string_buffer(capacity)
        used = listing(PROC_ALL_PIDS, 0, buffer, capacity)
        if used < 0 or used > capacity or used % ctypes.sizeof(ctypes.c_int):
            raise RuntimeError("macOS global process inventory is malformed")
        if used < capacity:
            count = used // ctypes.sizeof(ctypes.c_int)
            raw = (ctypes.c_int * count).from_buffer_copy(buffer.raw[:used]) if count else []
            pids = [int(pid) for pid in raw if int(pid) != 0]
            if (len(pids) != len(set(pids)) or any(pid <= 0 or pid > 0x7FFFFFFF for pid in pids)):
                raise RuntimeError("macOS global process inventory contains an invalid PID")
            expected = {os.fsencode(os.path.realpath(item)) for item in paths}
            generations: dict[bytes, set[DarwinProcessIdentity]] = {item: set() for item in expected}
            uninspectable: set[DarwinProcessIdentity] = set()
            for pid in pids:
                info = DarwinProcBsdInfo()
                info_size = pidinfo(pid, 3, 0, ctypes.byref(info), ctypes.sizeof(info))
                info_valid = info_size == ctypes.sizeof(info) and info.pbi_pid == pid
                path_buffer = ctypes.create_string_buffer(PROC_PIDPATHINFO_MAXSIZE)
                path_size = pidpath(pid, path_buffer, PROC_PIDPATHINFO_MAXSIZE)
                if path_size <= 0 or path_size >= PROC_PIDPATHINFO_MAXSIZE:
                    if info_valid and info.pbi_uid == os.geteuid():
                        uninspectable.add((pid, int(info.pbi_start_tvsec), int(info.pbi_start_tvusec)))
                    continue
                observed = path_buffer.raw[:path_size].split(b"\x00", 1)[0]
                if observed in expected:
                    if not info_valid:
                        raise RuntimeError("macOS target process identity is unavailable")
                    generations[observed].add((pid, int(info.pbi_start_tvsec), int(info.pbi_start_tvusec)))
            return generations, frozenset(uninspectable)
        capacity *= 2
    raise RuntimeError("macOS global process inventory exceeds its containment limit")


def drain_darwin_process_group(pinned: PinnedExecutables,
                               security_baseline: frozenset[DarwinProcessIdentity],
                               uninspectable_baseline: frozenset[DarwinProcessIdentity]) -> None:
    """Stay alive until both PGID and unique-executable inventories are empty."""
    process_group = os.getpgrp()
    if process_group != os.getpid():
        raise RuntimeError("macOS process-group identity changed")
    empty_passes = 0
    while True:
        try:
            group_members = darwin_process_group_members(process_group)
            executable_generations, uninspectable = darwin_executable_snapshot(
                {item[0] for item in pinned} | {Path(DARWIN_SECURITY)})
        except (OSError, RuntimeError):
            # Failed inventory is never an empty-containment proof. Retain the
            # durable wrapper generation and retry instead of publishing drain.
            time.sleep(0.05)
            continue
        model_paths = {os.fsencode(os.path.realpath(item[0])) for item in pinned}
        model_generations = set().union(*(executable_generations.get(item, set()) for item in model_paths))
        security_path = os.fsencode(os.path.realpath(DARWIN_SECURITY))
        new_security = executable_generations.get(security_path, set()) - security_baseline
        new_uninspectable = uninspectable - uninspectable_baseline
        empty = (os.getpid() in group_members and
                 all(pid == os.getpid() for pid in group_members) and
                 not model_generations and not new_security and not new_uninspectable)
        empty_passes = empty_passes + 1 if empty else 0
        if empty_passes >= 3:
            return
        time.sleep(0.02)


def main() -> int:
    args = arguments()
    stopping = False

    def stop(_signum: int, _frame: object) -> None:
        nonlocal stopping
        stopping = True

    # Install containment-owner handlers before model creation. A fast child
    # must not expose a window in which a site TERM kills the wrapper itself.
    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    try:
        control_fd, control_nonce = open_control()
    except (OSError, RuntimeError, ValueError) as error:
        print(f"intake-model-wrapper: {error}", file=sys.stderr)
        return 74
    expected = f"GO {args.token}\n".encode("ascii")
    if sys.stdin.buffer.readline(len(expected) + 1) != expected:
        control(control_fd, EMPTY, control_nonce)
        return 72
    if stopping:
        control(control_fd, EMPTY, control_nonce)
        return 72
    prompt = Path(args.prompt)
    try:
        prompt_fd = open_prompt(prompt, args)
    except (OSError, RuntimeError) as error:
        print(f"intake-model-wrapper: prompt proof failed: {error}", file=sys.stderr)
        control(control_fd, EMPTY, control_nonce)
        return 74
    try:
        try:
            executable_fd: int | None = None
            pinned_executables: PinnedExecutables | None = None
            security_baseline: frozenset[DarwinProcessIdentity] = frozenset()
            uninspectable_baseline: frozenset[DarwinProcessIdentity] = frozenset()
            try:
                command, linux_containment, executable_fd, pinned_executables = contained_command(
                    args.command, prompt.parent)
                model_env = dict(os.environ)
                model_env.pop("SHALLOW_INTAKE_POSIX_CONTROL_FD", None)
                model_env.pop("SHALLOW_INTAKE_POSIX_CONTROL_NONCE", None)
                if sys.platform == "darwin" and pinned_executables:
                    security_path = os.fsencode(os.path.realpath(DARWIN_SECURITY))
                    baseline, uninspectable_baseline = darwin_executable_snapshot({Path(DARWIN_SECURITY)})
                    security_baseline = frozenset(baseline.get(security_path, set()))
                    if darwin_native_executable(pinned_executables[0][0]):
                        model_env["PATH"] = "/usr/bin"
                child = spawn_gated_child(command, model_env, prompt_fd, control_fd,
                                          control_nonce, linux_containment,
                                          executable_fd)
            finally:
                if executable_fd is not None:
                    os.close(executable_fd)
        except (FileNotFoundError, RuntimeError, OSError, subprocess.SubprocessError) as error:
            cleanup_pinned_executables(pinned_executables)
            print(f"intake-model-wrapper: containment launch failed: {error}", file=sys.stderr)
            control(control_fd, EMPTY, control_nonce)
            return 74
    finally:
        os.close(prompt_fd)

    # The model never receives fd 3 or its nonce. The site acknowledges only
    # after captureProcessStartId + ancestry proof + exact worker-record CAS.
    # EOF, timeout, a malformed acknowledgement, or site death all take the
    # same fail-closed path: the gate exits without executing any model byte.
    control(control_fd, READY, control_nonce, child.pid)
    bound = await_binding(args.token, control_nonce, child.pid, args.timeout_ms,
                          lambda: stopping, child)
    if bound:
        try:
            child.release(control_nonce)
        except (OSError, RuntimeError):
            bound = False
    if not bound:
        child.cancel()
        stopping = True

    deadline = time.monotonic() + args.timeout_ms / 1000.0
    while child.poll() is None and not stopping and time.monotonic() < deadline:
        time.sleep(0.05)
    timed_out = child.poll() is None and not stopping and time.monotonic() >= deadline
    if child.poll() is None:
        try:
            child.terminate()
        except ProcessLookupError:
            pass
        try:
            child.wait(timeout=1.0)
        except subprocess.TimeoutExpired:
            try:
                child.kill()
            except ProcessLookupError:
                pass
            # Do not abandon a process stuck in an uninterruptible state. The
            # wrapper itself is the durable containment proof and must stay
            # alive until the direct child is actually reaped.
            child.wait()
    code = int(child.returncode or 0)
    if linux_containment:
        drain_linux_descendants()
    elif sys.platform == "darwin":
        if pinned_executables is None:
            raise RuntimeError("macOS executable containment proof is missing")
        drain_darwin_process_group(pinned_executables, security_baseline, uninspectable_baseline)
    pinned_clean = cleanup_pinned_executables(pinned_executables)
    control(control_fd, DRAINED, control_nonce, child.pid)
    if not pinned_clean:
        print("intake-model-wrapper: exact pinned executable cleanup failed", file=sys.stderr)
        return 75
    return 73 if timed_out else code


if __name__ == "__main__":
    raise SystemExit(main())
