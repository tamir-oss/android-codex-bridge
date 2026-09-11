#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
source_skill="$repo_root/skills/android-local-control/SKILL.md"
codex_dir="${CODEX_HOME:-$HOME/.codex}"
target_dir="$codex_dir/skills/android-local-control"
legacy_dir="$codex_dir/skills/android-structured-ui"

if [[ ! -f "$source_skill" ]]; then
  echo "Missing skill source: $source_skill" >&2
  exit 1
fi

if [[ ! -f "$target_dir/scripts/phone-control" ]]; then
  echo 'Existing android-local-control/scripts/phone-control is required; no changes made.' >&2
  exit 1
fi
if [[ -L "$target_dir" || -L "$legacy_dir" || -L "$target_dir/SKILL.md" ]]; then
  echo 'Refusing to migrate symlinked skill paths; inspect the installation first.' >&2
  exit 1
fi

# Keep backups outside skill discovery. Never replace the existing shell tool.
umask 077
mkdir -p "$codex_dir/skill-backups"
backup_dir=$(mktemp -d "$codex_dir/skill-backups/android-local-control.XXXXXX")
if [[ -f "$target_dir/SKILL.md" ]]; then
  cp -p "$target_dir/SKILL.md" "$backup_dir/android-local-control.SKILL.md"
fi
if [[ -d "$legacy_dir" ]]; then
  cp -a "$legacy_dir" "$backup_dir/android-structured-ui"
fi
new_skill=$(mktemp "$target_dir/.SKILL.XXXXXX")
trap 'rm -f "$new_skill"' EXIT
install -m 600 "$source_skill" "$new_skill"
mv "$new_skill" "$target_dir/SKILL.md"
if [[ -d "$legacy_dir" ]]; then
  mv "$legacy_dir" "$backup_dir/retired-android-structured-ui"
fi
printf 'Installed Codex skill: %s\n' "$target_dir/SKILL.md"
printf 'Previous instructions are recoverable in: %s\n' "$backup_dir"
