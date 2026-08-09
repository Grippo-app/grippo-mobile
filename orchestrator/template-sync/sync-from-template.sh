#!/usr/bin/env bash
# sync-from-template.sh — MANUAL vendored-copy sync, dry-run by default.
#
#   sync-from-template.sh <template-root> [<product-root>] [--apply]
#
# Compares the product's orchestrator/** against a live template checkout using
# the SAME hashing + exclusion rules as the integrity manifest (both sides are
# hashed by _generate_template_manifest.py --print — the rules live in exactly
# one place), prints the full diff, and only with --apply copies the
# changed/missing template-owned files into the product and re-stamps the
# manifest. It NEVER deletes product files (extras are reported, deletion is the
# owner's explicit move), never touches git, never runs without showing the diff.
#
# <product-root> defaults to this script's own copy root (../../ from here) —
# pass it explicitly when running the source template's copy against a product
# that does not carry template-sync/.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
GEN_REL="orchestrator/template-sync/_generate_template_manifest.py"

TEMPLATE_ROOT="${1:?usage: sync-from-template.sh <template-root> [<product-root>] [--apply]}"
shift
PRODUCT_ROOT="$(cd "$HERE/../.." && pwd -P)"
APPLY=0
for arg in "$@"; do
  case "$arg" in
    --apply) APPLY=1 ;;
    -*) echo "unknown flag: $arg (expected --apply)" >&2; exit 2 ;;
    *) PRODUCT_ROOT="$(cd "$arg" && pwd -P)" ;;
  esac
done
# pwd -P everywhere: logical pwd keeps symlink spellings, so /tmp/x vs
# /private/tmp/x would defeat the same-path guard below.
TEMPLATE_ROOT="$(cd "$TEMPLATE_ROOT" && pwd -P)"

GEN="$TEMPLATE_ROOT/$GEN_REL"
[ -f "$GEN" ] || { echo "ERROR: $GEN not found — is <template-root> really the template checkout?" >&2; exit 2; }
[ -d "$PRODUCT_ROOT/orchestrator" ] || { echo "ERROR: no orchestrator/ under $PRODUCT_ROOT" >&2; exit 2; }
[ "$TEMPLATE_ROOT" != "$PRODUCT_ROOT" ] || { echo "ERROR: template and product roots are the same path" >&2; exit 2; }
# Directionality guard. The source template keeps placeholder identity values;
# a configured product does not. This prevents reversed arguments without
# relying on a repository name, path, remote, or branch.
TEMPLATE_CONFIG="$TEMPLATE_ROOT/orchestrator/project-config.md"
PRODUCT_CONFIG="$PRODUCT_ROOT/orchestrator/project-config.md"
[ -f "$TEMPLATE_CONFIG" ] || { echo "ERROR: no project-config.md under source template" >&2; exit 2; }
[ -f "$PRODUCT_CONFIG" ] || { echo "ERROR: no project-config.md under product root" >&2; exit 2; }
grep -q '^productName: <Product>$' "$TEMPLATE_CONFIG" || { echo "ERROR: source does not have template identity placeholders — are the arguments reversed?" >&2; exit 2; }
if grep -q '^productName: <Product>$' "$PRODUCT_CONFIG"; then
  echo "ERROR: destination still has template identity placeholders; configure the product before syncing" >&2
  exit 2
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
# Hash BOTH sides with the template's generator (one rule set; --root keeps it
# read-only against the product).
python3 "$GEN" --print --root "$TEMPLATE_ROOT" > "$TMP/template.json"
python3 "$GEN" --print --root "$PRODUCT_ROOT" > "$TMP/product.json"

# A to-copy file whose product copy also diverged from the product's own
# stamped manifest is a local hotfix candidate — --apply
# would silently revert it. Such files are marked `COPY!` and their full unified diff is
# printed so the operator ports the fix TEMPLATE-WARD FIRST (see README "Harvest-back"),
# then re-syncs. A product with no stamp yet (first-ever sync) has no hotfix signal — every
# line stays plain COPY.
python3 - "$TMP/template.json" "$TMP/product.json" "$PRODUCT_ROOT/orchestrator/template-manifest.json" > "$TMP/plan" <<'PY'
import json, os, sys
t = json.load(open(sys.argv[1]))["files"]
p = json.load(open(sys.argv[2]))["files"]
stamp = {}
if len(sys.argv) > 3 and os.path.exists(sys.argv[3]):
    try:
        stamp = json.load(open(sys.argv[3])).get("files", {}) or {}
    except Exception:
        stamp = {}
for rel in sorted(t):
    if p.get(rel) == t[rel]:
        continue
    hot = rel in p and rel in stamp and p[rel] != stamp[rel]
    print(("COPY! " if hot else "COPY ") + rel)
for rel in sorted(r for r in p if r not in t):
    print("EXTRA " + rel)
PY

COPY_COUNT=$(grep -cE '^COPY!? ' "$TMP/plan" || true)
HOT_COUNT=$(grep -c '^COPY! ' "$TMP/plan" || true)
EXTRA_COUNT=$(grep -c '^EXTRA ' "$TMP/plan" || true)
echo "template : $TEMPLATE_ROOT"
echo "product  : $PRODUCT_ROOT"
echo "to copy  : $COPY_COUNT file(s) (template-owned, changed or missing in the product; $HOT_COUNT locally-modified hotfix candidate(s) marked !)"
grep '^COPY ' "$TMP/plan" | sed 's/^COPY /  <- /' || true
grep '^COPY! ' "$TMP/plan" | sed 's/^COPY! /  <-! /' || true
echo "extra    : $EXTRA_COUNT product file(s) the template never shipped (NEVER touched by this tool)"
grep '^EXTRA ' "$TMP/plan" | sed 's/^EXTRA /  ?? /' || true

if [ "$HOT_COUNT" -gt 0 ]; then
  echo
  echo "!! $HOT_COUNT file(s) were modified IN THE PRODUCT since its stamp — --apply would REVERT these local hotfixes."
  echo "!! Port them template-ward first (README 'Harvest-back'), then re-sync. Diffs (product vs template):"
  while IFS= read -r rel; do
    echo
    echo "--- hotfix candidate: $rel"
    diff -u "$PRODUCT_ROOT/$rel" "$TEMPLATE_ROOT/$rel" || true
  done < <(grep '^COPY! ' "$TMP/plan" | sed 's/^COPY! //')
fi

if [ "$APPLY" -ne 1 ]; then
  echo
  echo "DRY-RUN (nothing written). Re-run with --apply to copy the $COPY_COUNT file(s) and re-stamp the manifest."
  exit 0
fi

if [ "$COPY_COUNT" -gt 0 ]; then
  while IFS= read -r rel; do
    dst="$PRODUCT_ROOT/$rel"
    # A path that exists as a DIRECTORY in the product cannot be clobbered by
    # cp (it would silently copy INTO it and the stamp would then lie) —
    # surface it for a manual resolve instead.
    if [ -d "$dst" ] && [ ! -L "$dst" ]; then
      echo "ERROR: $rel is a DIRECTORY in the product — resolve it manually, then re-run" >&2
      exit 1
    fi
    # rm -f first: cp writes THROUGH a destination symlink (clobbering its
    # target, possibly outside the product tree); removing the link/file and
    # copying fresh replaces the path itself.
    rm -f "$dst"
    mkdir -p "$(dirname "$dst")"
    cp -p "$TEMPLATE_ROOT/$rel" "$dst"
  done < <(grep -E '^COPY!? ' "$TMP/plan" | sed -E 's/^COPY!? //')
fi
# Re-stamp AFTER a successful apply with the TEMPLATE-side manifest we already
# computed — NOT a re-hash of the product tree: hashing the product now would
# grandfather its `extra` files into the baseline (masking them forever) and
# record the PRODUCT's own commit as the stamp provenance (actively misleading —
# the reader would look for that sha in the template repo). The template map IS
# what the copy now carries for every template-owned path, and its stampCommit/
# date are the honest provenance of this sync. Written atomically (tmp + mv) so
# an interrupt can never leave a truncated stamp that parses as neither
# stamped nor honestly unstamped.
cp "$TMP/template.json" "$PRODUCT_ROOT/orchestrator/template-manifest.json.tmp"
mv "$PRODUCT_ROOT/orchestrator/template-manifest.json.tmp" "$PRODUCT_ROOT/orchestrator/template-manifest.json"
echo "applied: $COPY_COUNT file(s) copied; manifest re-stamped from the template side. Review + commit in the product repo."
