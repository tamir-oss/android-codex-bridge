import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import contextlib
import io


SCRIPT = Path(__file__).parents[1] / "scripts" / "companion-control.py"
SPEC = importlib.util.spec_from_file_location("companion_control", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class ProtocolTests(unittest.TestCase):
    def test_awake_cli_routes_and_defaults(self):
        cases = [(["awake-start"], {"action": "start", "seconds": 120}),
                 (["awake-renew", "lease", "--seconds", "30"],
                  {"action": "renew", "lease_id": "lease", "seconds": 30}),
                 (["awake-stop", "lease"], {"action": "stop", "lease_id": "lease"})]
        for argv, expected in cases:
            with self.subTest(argv=argv), patch.object(MODULE, "request", return_value={"ok": True}) as call:
                with contextlib.redirect_stdout(io.StringIO()):
                    self.assertEqual(MODULE.main(argv), 0)
                call.assert_called_once_with("awake", expected)

    def test_awake_invalid_duration_never_connects(self):
        for seconds in ("0", "601", "1.5"):
            with patch.object(MODULE, "request") as call, contextlib.redirect_stderr(io.StringIO()):
                with self.assertRaises(SystemExit):
                    MODULE.main(["awake-start", "--seconds", seconds])
                call.assert_not_called()

    def test_unicode_request_is_preserved(self):
        encoded = MODULE.encode_request(
            "perform",
            {"node": "n1-2", "action": "set_text", "text": "שלום Hello 123"},
            request_id="test-id",
            token="a" * 64,
        )
        decoded = json.loads(encoded.decode("utf-8"))
        self.assertEqual(decoded["args"]["text"], "שלום Hello 123")
        self.assertEqual(decoded["token"], "a" * 64)
        self.assertTrue(encoded.endswith(b"\n"))

    def test_response_requires_complete_line(self):
        with self.assertRaises(ValueError):
            MODULE.decode_response(b'{"version":1,"ok":true}')

    def test_valid_response(self):
        response = MODULE.decode_response(b'{"version":1,"ok":true,"result":{}}\n')
        self.assertTrue(response["ok"])

    def test_token_is_stored_owner_only(self):
        previous = MODULE.TOKEN_FILE
        try:
            with tempfile.TemporaryDirectory() as directory:
                MODULE.TOKEN_FILE = Path(directory) / "config" / "token"
                MODULE.save_token("b" * 64)
                self.assertEqual(MODULE.load_token(), "b" * 64)
                self.assertEqual(os.stat(MODULE.TOKEN_FILE).st_mode & 0o777, 0o600)
        finally:
            MODULE.TOKEN_FILE = previous

    def test_invalid_token_format_is_rejected(self):
        previous = MODULE.TOKEN_FILE
        try:
            with tempfile.TemporaryDirectory() as directory:
                MODULE.TOKEN_FILE = Path(directory) / "token"
                with self.assertRaises(ValueError):
                    MODULE.save_token("not-a-token")
        finally:
            MODULE.TOKEN_FILE = previous


if __name__ == "__main__":
    unittest.main()
