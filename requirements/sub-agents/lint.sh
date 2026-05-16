#!/usr/bin/env bash
# Verifies sub-agent files against five mechanical drift checks:
#   1. Every chapter-style `requirements/<NN>-<area>/<NN>-<topic>.md` link
#      points at a file that exists. Top-level refs (launch.md, README.md,
#      tasks/README.md) are checked separately in Check 5.
#   2. Every agent file has frontmatter `name`, `description`, `tools`, `model`.
#   3. Every agent file is listed in `requirements/sub-agents/README.md`.
#   4. Every agent named in the README inventory tables exists as a file.
#   5. Every top-level doc referenced from sub-agent files exists at the
#      requirements/ root (launch.md, README.md, tasks/README.md).
set -uo pipefail
cd "$(dirname "$0")/../.."

fail=0

# Check 1: every requirements/* link in sub-agent files points at an existing file.
refs=$(grep -rhoE 'requirements/[0-9]+-[a-z-]+/[0-9]+-[a-z-]+\.md' requirements/sub-agents/ | sort -u)
if [ -z "$refs" ]; then
  echo "FAIL: no references found — has the regex drifted?"
  fail=$((fail + 1))
else
  missing=0
  while IFS= read -r ref; do
    if [ ! -f "$ref" ]; then
      echo "DEAD LINK: $ref"
      missing=$((missing + 1))
    fi
  done <<< "$refs"
  if [ $missing -gt 0 ]; then
    echo "FAIL: $missing dead links."
    fail=$((fail + 1))
  fi
fi

# Check 2: every agent file has frontmatter with name, description, tools, model.
fm_missing=0
for f in requirements/sub-agents/builders/*.md \
         requirements/sub-agents/validators/*.md \
         requirements/sub-agents/helpers/*.md; do
  [ -f "$f" ] || continue
  head -10 "$f" | grep -q '^name:'        || { echo "FRONTMATTER missing 'name': $f";        fm_missing=$((fm_missing + 1)); }
  head -10 "$f" | grep -q '^description:' || { echo "FRONTMATTER missing 'description': $f"; fm_missing=$((fm_missing + 1)); }
  head -10 "$f" | grep -q '^tools:'       || { echo "FRONTMATTER missing 'tools': $f";       fm_missing=$((fm_missing + 1)); }
  head -10 "$f" | grep -q '^model:'       || { echo "FRONTMATTER missing 'model': $f";       fm_missing=$((fm_missing + 1)); }
done
if [ $fm_missing -gt 0 ]; then
  echo "FAIL: $fm_missing missing frontmatter fields."
  fail=$((fail + 1))
fi

# Check 3: every agent in the folders is mentioned in README.
inv_missing=0
for f in requirements/sub-agents/builders/*.md \
         requirements/sub-agents/validators/*.md \
         requirements/sub-agents/helpers/*.md; do
  [ -f "$f" ] || continue
  name=$(grep -m1 '^name:' "$f" | awk '{print $2}')
  [ -z "$name" ] && continue
  if ! grep -q "\`$name\`" requirements/sub-agents/README.md; then
    echo "README missing inventory entry for: $name"
    inv_missing=$((inv_missing + 1))
  fi
done
if [ $inv_missing -gt 0 ]; then
  echo "FAIL: $inv_missing agent(s) absent from README inventory."
  fail=$((fail + 1))
fi

# Check 4: every name in README inventory tables exists as a file.
# Scope to the "## Agent inventory" section only — other tables may list
# non-agent values (e.g. codexEnabled = auto|true|false) that share the same
# row syntax.
orphan=0
inventory=$(awk '/^## Agent inventory/{found=1; next} found && /^## /{exit} found{print}' requirements/sub-agents/README.md)
while IFS= read -r name; do
  [ -z "$name" ] && continue
  found=0
  for dir in builders validators helpers; do
    [ -f "requirements/sub-agents/$dir/$name.md" ] && found=1
  done
  if [ $found -eq 0 ]; then
    echo "README mentions agent that doesn't exist as file: $name"
    orphan=$((orphan + 1))
  fi
done < <(echo "$inventory" | grep -oE '^\| `[a-z-]+`' | sed 's/^| `\(.*\)`$/\1/')
if [ $orphan -gt 0 ]; then
  echo "FAIL: $orphan README inventory entries point at non-existent files."
  fail=$((fail + 1))
fi

# Check 5: top-level docs referenced from sub-agent files exist at requirements/ root.
TOPLEVEL_DOCS="launch.md README.md tasks/README.md"
toplevel_missing=0
for doc in $TOPLEVEL_DOCS; do
  if [ ! -f "requirements/$doc" ]; then
    echo "MISSING TOP-LEVEL DOC: requirements/$doc"
    toplevel_missing=$((toplevel_missing + 1))
  fi
done
if [ $toplevel_missing -gt 0 ]; then
  echo "FAIL: $toplevel_missing top-level doc(s) missing."
  fail=$((fail + 1))
fi

if [ $fail -gt 0 ]; then
  echo "FAIL: $fail check(s) failed."
  exit 1
fi
echo "OK: all 5 lint checks passed (links, frontmatter, README inventory, file existence, top-level docs)."
