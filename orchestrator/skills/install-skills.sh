#!/usr/bin/env bash
# Deploy step (launch Step 14 analogue): install authored skills into a target
# project's .claude/skills/<name>/. Usage:
#   install-skills.sh <target-root> [--symlink] [--no-hooks]
# Flags are position-independent. --symlink links instead of copying; --no-hooks
# skips the git core.hooksPath wiring (for targets that manage hooks themselves).
set -euo pipefail
SRC="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$SRC/../.." && pwd)"
TARGET="${1:?usage: install-skills.sh <target-root> [--symlink] [--no-hooks]}"
MODE="copy"
WIRE_HOOKS=1
for arg in "${@:2}"; do
  case "$arg" in
    --symlink) MODE="--symlink" ;;
    --no-hooks) WIRE_HOOKS=0 ;;
    *) echo "unknown flag: $arg (expected --symlink and/or --no-hooks)" >&2; exit 2 ;;
  esac
done
MAN="$ROOT/orchestrator/skills/_index/install-manifest.json"
python3 "$ROOT/orchestrator/skills/_index/validate-install-manifest.py" "$ROOT" >/dev/null
for name in $(python3 -c "import json;[print(s['folderName']) for s in json.load(open('$MAN'))['skills'] if not s.get('externalSourceException')]"); do
  dst="$TARGET/.claude/skills/$name"
  src="$ROOT/orchestrator/skills/$name"
  if [ "$MODE" = "--symlink" ]; then
    mkdir -p "$dst"
    ln -sf "$src/SKILL.md" "$dst/SKILL.md"
    # symlink the whole references/ dir too — a bare SKILL.md without its
    # references routes into an empty dir = broken skill.
    rm -rf "$dst/references"
    [ -d "$src/references" ] && ln -sf "$src/references" "$dst/references"
  else
    rm -rf "$dst/references"
    mkdir -p "$dst/references"
    rm -f "$dst/SKILL.md"
    cp "$src/SKILL.md" "$dst/SKILL.md"
    cp -R "$src/references/." "$dst/references/"
  fi
done
echo "installed $(python3 -c "import json;print(sum(1 for s in json.load(open('$MAN'))['skills'] if not s.get('externalSourceException')))") skills into $TARGET/.claude/skills/"

# ── Manifest-owned installed files: frozen contracts plus the queue command
# and launch configuration. The contracts tree is wholly owned and replaced so
# retired files cannot survive an idempotent reinstall. Other .claude folders
# may contain user-owned entries; only the exact command and launch paths are
# replaced there.
rm -rf "$TARGET/.claude/contracts"
while IFS=$'\t' read -r source_path install_path; do
  [ -n "$source_path" ] || continue
  src="$ROOT/$source_path"
  dst="$TARGET/$install_path"
  mkdir -p "$(dirname "$dst")"
  rm -f "$dst"
  if [ "$MODE" = "--symlink" ]; then
    ln -s "$src" "$dst"
  else
    cp "$src" "$dst"
  fi
done < <(python3 -c "import json;[print(f['sourcePath']+'\\t'+f['installPath']) for f in json.load(open('$MAN'))['files']]")
echo "installed $(python3 -c "import json;print(len(json.load(open('$MAN'))['files']))") manifest-owned files into $TARGET/.claude/"

# ── Enforcement wiring (screenshot gate): a bootstrapped product has NO
# mechanical gate until (a) git executes the tracked pre-commit hook and
# (b) CI carries the figma-gate workflow. Wire both here, idempotently.
# --no-hooks skips (a) for targets that manage their own hooks.
if [ "$WIRE_HOOKS" -eq 0 ]; then
  echo "skipped core.hooksPath wiring (--no-hooks); run manually if desired: git -C \"$TARGET\" config core.hooksPath orchestrator/skills/checks/hooks"
elif git -C "$TARGET" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git -C "$TARGET" config core.hooksPath orchestrator/skills/checks/hooks
  echo "wired core.hooksPath -> orchestrator/skills/checks/hooks (pre-commit verify-done gate is live)"
else
  echo "ACTION REQUIRED: $TARGET is not a git repo — after 'git init' run: git config core.hooksPath orchestrator/skills/checks/hooks"
fi
