#!/usr/bin/env bash
# Run the complete skill/tooling consistency suite.
set -uo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"

if [ $# -ne 0 ]; then
  echo "usage: run-all.sh" >&2
  exit 2
fi

gates=(
  "links|$DIR/links.sh"
  "docs-map|$DIR/docs-map.sh"
  "doc-counts|$DIR/doc-counts.sh"
  "prompt-surfaces|$DIR/prompt-surfaces.sh"
  "skeleton|$DIR/skeleton.sh"
  "reference-hash|$DIR/reference-hash.sh"
  "runtime-readiness|$DIR/runtime-readiness.sh"
  "self-contained-content|$DIR/self-contained-content.sh"
  "install-sync|$DIR/install-sync.sh"
  "capabilities-complete|$DIR/capabilities-complete.sh"
  "payload-schemas|$DIR/payload-schemas.sh"
  "harness-fidelity|$DIR/harness-fidelity.sh"
  "trigger-fixtures|$DIR/trigger-fixtures.sh"
  "wiring|$DIR/wiring.sh"
)

pass=0
fail=0
GATE_OUT="$(mktemp "${TMPDIR:-/tmp}/orchestrator-gate.XXXXXX")" || exit 2
trap 'rm -f "$GATE_OUT"' EXIT

echo "=== skill gates ==="
for g in "${gates[@]}"; do
  label="${g%%|*}"
  script="${g#*|}"
  echo "- $label"
  if bash "$script" >"$GATE_OUT" 2>&1; then
    sed 's/^/  /' "$GATE_OUT"
    pass=$((pass + 1))
  else
    sed 's/^/  /' "$GATE_OUT"
    echo "  -> FAILED"
    fail=$((fail + 1))
  fi
done

echo "gates: $pass passed, $fail failed"
[ "$fail" -eq 0 ]
