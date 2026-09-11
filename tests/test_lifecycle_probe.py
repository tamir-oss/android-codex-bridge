import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location('lifecycle', Path(__file__).resolve().parents[1] / 'scripts/check-companion-lifecycle.py')
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class UsbProbeTests(unittest.TestCase):
    def test_probe_retries_incomplete_output_without_assuming_disconnect(self):
        with patch.object(MODULE.TEST, 'rish', side_effect=['', 'partial', 'connected=true']), patch.object(MODULE.time, 'sleep'):
            self.assertTrue(MODULE.read_usb_connected())

    def test_probe_fails_closed_after_bounded_retries(self):
        with patch.object(MODULE.TEST, 'rish', return_value='') as read, patch.object(MODULE.time, 'sleep'):
            with self.assertRaises(RuntimeError):
                MODULE.read_usb_connected()
            self.assertEqual(read.call_count, 5)

    def test_current_state_precedes_historical_connected_devices(self):
        self.assertFalse(MODULE.usb_connected('      connected=false\n        connected=true\n'))

    def test_connected(self):
        self.assertTrue(MODULE.usb_connected('      connected=true\n      configured=true\n'))

    def test_trimmed_single_line_from_rish(self):
        self.assertTrue(MODULE.usb_connected('connected=true'))
        self.assertFalse(MODULE.usb_connected('connected=false'))

    def test_missing_usb_state_is_not_assumed_disconnected(self):
        with self.assertRaises(RuntimeError):
            MODULE.usb_connected('Permission denied')
