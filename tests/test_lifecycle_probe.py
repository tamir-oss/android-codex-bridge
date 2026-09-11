import importlib.util
from pathlib import Path
import unittest

SPEC = importlib.util.spec_from_file_location('lifecycle', Path(__file__).resolve().parents[1] / 'scripts/check-companion-lifecycle.py')
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class UsbProbeTests(unittest.TestCase):
    def test_current_state_precedes_historical_connected_devices(self):
        self.assertFalse(MODULE.usb_connected('      connected=false\n        connected=true\n'))

    def test_connected(self):
        self.assertTrue(MODULE.usb_connected('      connected=true\n      configured=true\n'))

    def test_missing_usb_state_is_not_assumed_disconnected(self):
        with self.assertRaises(RuntimeError):
            MODULE.usb_connected('Permission denied')
