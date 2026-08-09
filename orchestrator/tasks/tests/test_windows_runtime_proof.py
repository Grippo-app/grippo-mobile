#!/usr/bin/env python3
"""Platform-neutral unit checks for the Windows proof protocol helpers."""

from __future__ import annotations

import importlib.util
import io
import json
from pathlib import Path
import sys
import unittest
from unittest import mock


MODULE_PATH = Path(__file__).resolve().parent.parent / "windows-runtime-proof.py"
SPEC = importlib.util.spec_from_file_location("windows_runtime_proof", MODULE_PATH)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class WindowsRuntimeProofTests(unittest.TestCase):
    def test_process_digest_is_stable_and_domain_separated(self):
        self.assertEqual(
            MODULE.process_start_digest("00112233445566778899aabbccddeeff", 42, 123456789012345678),
            "psid-v1:win32:7c7c85e6353aa58fa1beec356c40fc9b608c9fff1010090e188ba58167c1d963",
        )
        boot = "00112233445566778899aabbccddeeff"
        self.assertNotEqual(MODULE.process_start_digest(boot, 42, 1), MODULE.process_start_digest(boot, 43, 1))
        self.assertNotEqual(MODULE.process_start_digest(boot, 42, 1), MODULE.process_start_digest(boot, 42, 2))
        self.assertNotEqual(
            MODULE.process_start_digest(boot, 42, 1),
            MODULE.process_start_digest("10112233445566778899aabbccddeeff", 42, 1),
        )
        with self.assertRaises(ValueError):
            MODULE.process_start_digest("not-a-guid", 42, 1)

    def test_protocol_envelopes_have_exact_nullable_identity_fields(self):
        self.assertEqual(
            MODULE.process_verdict("dead", 7, None, "not-found"),
            {"pid": 7, "processStartId": None, "reason": "not-found", "status": "dead", "version": 1},
        )
        self.assertEqual(
            MODULE.path_verdict("missing", "missing"),
            {"dev": None, "ino": None, "pathType": None, "reason": "missing", "status": "missing", "version": 1},
        )
        self.assertEqual(
            MODULE.ancestry_verdict("match", 8, 7, 1),
            {"ancestorPid": 7, "depth": 1, "descendantPid": 8, "reason": "ok", "status": "match", "version": 1},
        )

    def test_canonical_output_is_sorted_compact_and_single_line(self):
        output = io.StringIO()
        with mock.patch.object(sys, "stdout", output):
            MODULE.canonical({"z": 1, "a": "x"})
        self.assertEqual(output.getvalue(), '{"a":"x","z":1}\n')
        self.assertEqual(json.loads(output.getvalue()), {"a": "x", "z": 1})

    def test_replaced_path_is_rejected_before_any_security_operation(self):
        class FakeApi:
            def __init__(self):
                self.closed = []

            def close(self, handle):
                self.closed.append(handle)

        api = FakeApi()
        with mock.patch.object(MODULE, "_open_path", return_value=(77, None)), mock.patch.object(
            MODULE, "_identity", return_value=("file", "10", "99", False)
        ), mock.patch.object(
            MODULE, "_current_and_well_known_sids", side_effect=AssertionError("must not inspect or mutate DACL")
        ):
            verdict = MODULE.inspect_path(api, r"C:\authority\owner.json", True, ("10", "20", "file"))
        self.assertEqual(
            verdict,
            {"dev": "10", "ino": "99", "pathType": "file", "reason": "identity-changed", "status": "unsafe", "version": 1},
        )
        self.assertEqual(api.closed, [77])

    @unittest.skipIf(sys.platform == "win32", "non-Windows fail-closed behavior only")
    def test_native_commands_are_explicitly_unsupported_off_windows(self):
        self.assertEqual(MODULE.main([str(MODULE_PATH), "process", "1"]), 3)
        self.assertEqual(
            MODULE.main([str(MODULE_PATH), "ancestry", "2", "psid-v1:win32:" + "a" * 64,
                         "1", "psid-v1:win32:" + "b" * 64]), 3
        )
        self.assertEqual(
            MODULE.main([str(MODULE_PATH), "private-path", str(MODULE_PATH.resolve()), "1", "2", "file"]), 3
        )


if __name__ == "__main__":
    unittest.main()
