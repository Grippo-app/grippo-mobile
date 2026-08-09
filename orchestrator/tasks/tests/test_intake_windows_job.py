#!/usr/bin/env python3
"""Platform-neutral contract checks for the Windows model BOUND gate."""

from __future__ import annotations

import importlib.util
import io
import os
from pathlib import Path
import unittest


MODULE_PATH = Path(__file__).resolve().parent.parent / "intake-windows-job.py"
SPEC = importlib.util.spec_from_file_location("intake_windows_job", MODULE_PATH)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class IntakeWindowsBoundContractTests(unittest.TestCase):
    TOKEN = "intake-0123456789abcdef"
    NONCE = "a" * 48
    PID = 4242

    def test_control_record_is_complete_and_authenticated(self):
        read_fd, write_fd = os.pipe()
        previous = MODULE.CONTROL_FD
        try:
            MODULE.CONTROL_FD = write_fd
            MODULE.control(MODULE.READY, self.NONCE, self.PID)
            MODULE.control(MODULE.DRAINED, self.NONCE)
            os.close(write_fd)
            write_fd = -1
            self.assertEqual(
                os.read(read_fd, 4096),
                (
                    f"{MODULE.READY} {self.NONCE} {self.PID}\n"
                    f"{MODULE.DRAINED} {self.NONCE}\n"
                ).encode("ascii"),
            )
        finally:
            MODULE.CONTROL_FD = previous
            if write_fd >= 0:
                os.close(write_fd)
            os.close(read_fd)

    def test_exact_bound_record_is_the_only_resume_authority(self):
        line = MODULE.expected_bound(self.TOKEN, self.NONCE, self.PID)
        self.assertEqual(line, f"BOUND {self.TOKEN} {self.NONCE} {self.PID}\n".encode("ascii"))
        self.assertTrue(MODULE.read_exact_bound(io.BytesIO(line), self.TOKEN, self.NONCE, self.PID))

    def test_changed_partial_extended_and_missing_bound_are_rejected(self):
        valid = MODULE.expected_bound(self.TOKEN, self.NONCE, self.PID)
        invalid = (
            b"",
            valid[:-1],
            valid[:-1] + b" trailing\n",
            valid[:-1] + b"\r\n",
            MODULE.expected_bound(self.TOKEN + "x", self.NONCE, self.PID),
            MODULE.expected_bound(self.TOKEN, "b" * 48, self.PID),
            MODULE.expected_bound(self.TOKEN, self.NONCE, self.PID + 1),
            b"TERMINATE\n",
        )
        for candidate in invalid:
            with self.subTest(candidate=candidate):
                self.assertFalse(MODULE.read_exact_bound(io.BytesIO(candidate), self.TOKEN, self.NONCE, self.PID))

    def test_bound_read_is_strictly_bounded(self):
        expected = MODULE.expected_bound(self.TOKEN, self.NONCE, self.PID)

        class RecordingStream:
            def __init__(self):
                self.limit = None

            def readline(self, limit):
                self.limit = limit
                return b"X" * limit

        stream = RecordingStream()
        self.assertFalse(MODULE.read_exact_bound(stream, self.TOKEN, self.NONCE, self.PID))
        self.assertEqual(stream.limit, len(expected) + 1)

    def test_invalid_bound_has_zero_resume_or_watcher_side_effects(self):
        events = []
        state = MODULE.bound_resume_state(
            io.BytesIO(b"BOUND wrong\n"), self.TOKEN, self.NONCE, self.PID,
            lambda: events.append("watcher"),
            lambda: events.append("stop-check") or False,
            lambda: events.append("resume") or 0,
        )
        self.assertEqual(state, "invalid")
        self.assertEqual(events, [])

    def test_exact_bound_starts_watcher_before_one_fast_resume(self):
        events = []
        state = MODULE.bound_resume_state(
            io.BytesIO(MODULE.expected_bound(self.TOKEN, self.NONCE, self.PID)),
            self.TOKEN, self.NONCE, self.PID,
            lambda: events.append("watcher"),
            lambda: events.append("stop-check") or False,
            lambda: events.append("resume") or 1,
        )
        self.assertEqual(state, "resumed")
        self.assertEqual(events, ["watcher", "stop-check", "resume"])

    def test_eof_observed_after_bound_prevents_resume(self):
        events = []
        state = MODULE.bound_resume_state(
            io.BytesIO(MODULE.expected_bound(self.TOKEN, self.NONCE, self.PID)),
            self.TOKEN, self.NONCE, self.PID,
            lambda: events.append("watcher"),
            lambda: events.append("stop-check") or True,
            lambda: events.append("resume") or 0,
        )
        self.assertEqual(state, "stopped")
        self.assertEqual(events, ["watcher", "stop-check"])

    def test_resume_failure_is_reported_after_exact_bound(self):
        events = []
        state = MODULE.bound_resume_state(
            io.BytesIO(MODULE.expected_bound(self.TOKEN, self.NONCE, self.PID)),
            self.TOKEN, self.NONCE, self.PID,
            lambda: events.append("watcher"),
            lambda: events.append("stop-check") or False,
            lambda: events.append("resume") or 0xFFFFFFFF,
        )
        self.assertEqual(state, "resume-failed")
        self.assertEqual(events, ["watcher", "stop-check", "resume"])


if __name__ == "__main__":
    unittest.main()
