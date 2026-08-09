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
    cp -R "$src/references/." "$dst/references/" 2>/dev/null || true
  fi
done
echo "installed $(python3 -c "import json;print(sum(1 for s in json.load(open('$MAN'))['skills'] if not s.get('externalSourceException')))") skills into $TARGET/.claude/skills/"

# ── Frozen contracts: skills cite ../../contracts/* (from SKILL.md) and
# ../../../contracts/* (from references/) — both resolve to .claude/contracts/
# from the install dir, so the contracts must land there or every cite dangles.
CON_SRC="$ROOT/orchestrator/contracts"
CON_DST="$TARGET/.claude/contracts"
if [ -d "$CON_SRC" ]; then
  mkdir -p "$TARGET/.claude"
  rm -rf "$CON_DST"
  if [ "$MODE" = "--symlink" ]; then
    ln -sf "$CON_SRC" "$CON_DST"
  else
    mkdir -p "$CON_DST"
    cp -R "$CON_SRC/." "$CON_DST/"
  fi
  echo "installed contracts into $TARGET/.claude/contracts/ (mode: ${MODE#--})"
fi

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
