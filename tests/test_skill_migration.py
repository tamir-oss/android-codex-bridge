"""Verify migration preserves the working backend and recoverable instructions."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class SkillMigrationTests(unittest.TestCase):
    def test_migration_preserves_backend_and_is_repeatable(self):
        with tempfile.TemporaryDirectory() as directory:
            codex = Path(directory)
            old = codex / 'skills/android-local-control'
            legacy = codex / 'skills/android-structured-ui'
            (old / 'scripts').mkdir(parents=True)
            legacy.mkdir(parents=True)
            backend = old / 'scripts/phone-control'
            backend.write_text('existing backend\n')
            backend.chmod(0o700)
            (old / 'SKILL.md').write_text('original local instructions\n')
            (legacy / 'SKILL.md').write_text('original accessibility instructions\n')
            environment = {**os.environ, 'CODEX_HOME': directory}
            for _ in range(2):
                subprocess.run(['bash', str(ROOT / 'scripts/install-codex-skill.sh')],
                               env=environment, check=True, capture_output=True)
            self.assertEqual(backend.read_text(), 'existing backend\n')
            self.assertEqual(backend.stat().st_mode & 0o777, 0o700)
            self.assertFalse(legacy.exists())
            self.assertEqual((old / 'SKILL.md').read_bytes(),
                             (ROOT / 'skills/android-local-control/SKILL.md').read_bytes())
            backups = list((codex / 'skill-backups').rglob('*.md'))
            contents = [p.read_text() for p in backups]
            self.assertIn('original local instructions\n', contents)
            self.assertIn('original accessibility instructions\n', contents)

    def test_missing_backend_leaves_installation_untouched(self):
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run(['bash', str(ROOT / 'scripts/install-codex-skill.sh')],
                                    env={**os.environ, 'CODEX_HOME': directory},
                                    capture_output=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(list(Path(directory).iterdir()), [])
