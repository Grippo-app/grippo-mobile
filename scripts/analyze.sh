#!/usr/bin/env bash
set -euo pipefail

# --- Config (edit if needed) ---
# Comma-separated TODO tags (case-insensitive)
TODO_TAGS="TODO,FIXME,HACK,XXX,BUG,NOTE"

# Optional: path to Android Studio inspect runner (leave empty to skip)
# macOS example: /Applications/Android\ Studio.app/Contents/bin/inspect.sh
INSPECT_SH="${INSPECT_SH:-}"

# Optional: path to an exported inspection profile XML (from IDE)
INSPECT_PROFILE="${INSPECT_PROFILE:-scripts/inspect-profile.xml}"
# --------------------------------

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GRADLEW="$ROOT/gradlew"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUTDIR="$ROOT/build/analyze-report/$TIMESTAMP"
TODODIR="$OUTDIR/todo"
LINTDIR="$OUTDIR/lint"
LOGDIR="$OUTDIR/logs"
INSPECT_OUT="$OUTDIR/inspect"

mkdir -p "$TODODIR" "$LINTDIR" "$LOGDIR"

log() { printf "\033[1;34m[analyze]\033[0m %s\n" "$*" >&2; }
warn(){ printf "\033[1;33m[warn]\033[0m %s\n" "$*" >&2; }
err() { printf "\033[1;31m[err]\033[0m %s\n" "$*" >&2; }

# Detect git to limit grep scope to tracked files (faster, fewer false hits)
git_tracked_files() {
  if command -v git >/dev/null 2>&1 && git -C "$ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    (cd "$ROOT" && git ls-files)
  else
    # Fallback to find; exclude common noise
    (cd "$ROOT" && find . -type f \
      -not -path "./.git/*" \
      -not -path "./.gradle/*" \
      -not -path "./**/build/*" \
      -not -path "./**/.idea/*" \
      -not -path "./**/.fleet/*" \
      -not -path "./**/.settings/*")
  fi
}

scan_todos() {
  log "Scanning TODOs…"
  IFS=',' read -r -a TAGS <<< "$TODO_TAGS"
  # Build case-insensitive regex like: (TODO|FIXME|HACK|…)
  local re="("
  for i in "${!TAGS[@]}"; do
    tag="${TAGS[$i]}"
    tag="${tag// /}" # trim
    re+="${i:+|}$tag"
  done
  re+=")"

  local list_file="$TODODIR/todos.txt"
  : > "$list_file"

  # Limit to relevant extensions to reduce noise (bash 3.2 friendly)
  local tmp_list="$TODODIR/_todo_files.txt"
  git_tracked_files | grep -E '\.(kt|kts|java|xml|gradle|pro|properties|md|txt|yaml|yml|sh)$' > "$tmp_list" 2>/dev/null || true

  if [ ! -s "$tmp_list" ]; then
    warn "No files detected for TODO scan."
    return 0
  fi

  # BSD xargs has no -r; guard by checking file count first
  local files_count
  files_count="$(wc -l < "$tmp_list" | tr -d ' ')"
  if [ "$files_count" -gt 0 ]; then
    # Grep TODO-like tags across the file list
    cat "$tmp_list" | xargs grep -nEI "$re" > "$list_file" 2>/dev/null || true
  fi

  local count
  count="$(wc -l < "$list_file" | tr -d ' ')"
  log "TODO scan done. Found: $count"
}

run_lint() {
  log "Running Gradle Lint (all modules)…"
  # Keep console plain to simplify logs parsing
  if ! "$GRADLEW" --console=plain lint >"$LOGDIR/gradle-lint.out" 2>"$LOGDIR/gradle-lint.err"; then
    warn "Gradle lint task returned non-zero. Continuing (reports may still exist)."
  fi

  # Collect all lint reports into $LINTDIR
  # HTML & XML (variant-specific)
  while IFS= read -r -d '' f; do
    cp -f "$f" "$LINTDIR/$(basename "$f")"
  done < <(find "$ROOT" -type f \( -name "lint-results*.html" -o -name "lint-results*.xml" -o -name "lint-report*.sarif" \) -print0 2>/dev/null || true)

  # Summaries
  local html_count xml_count sarif_count
  html_count=$(find "$LINTDIR" -maxdepth 1 -name "lint-results*.html" | wc -l | tr -d ' ')
  xml_count=$(find "$LINTDIR" -maxdepth 1 -name "lint-results*.xml"  | wc -l | tr -d ' ')
  sarif_count=$(find "$LINTDIR" -maxdepth 1 -name "lint-report*.sarif" | wc -l | tr -d ' ')
  log "Lint reports: HTML=$html_count, XML=$xml_count, SARIF=$sarif_count"

  # Heuristic counts for key issues
  local unused_res=0
  if [ "$xml_count" -gt 0 ]; then
    unused_res=$(grep -Rhc '<issue id="UnusedResources"' "$LINTDIR" || true | awk '{s+=$1} END {print s+0}')
  fi

  printf "%s\n" "$unused_res" > "$LINTDIR/_unused_resources.count"
}

run_build_warns() {
  log "Compiling (to collect compiler/Gradle warnings)…"
  # Assemble debug to keep it relatively fast; add --warning-mode=all to surface everything
  if ! "$GRADLEW" --console=plain --warning-mode=all assembleDebug >"$LOGDIR/gradle-build.out" 2>"$LOGDIR/gradle-build.err"; then
    warn "Assemble returned non-zero. See logs for details."
  fi

  # Extract 'warning:' lines as a quick summary
  (grep -nE "warning:|w: " "$LOGDIR/gradle-build.out" || true) > "$OUTDIR/warnings-summary.txt"
}

run_inspect_headless() {
  # Optional branch: requires Android Studio's inspect.sh and a profile XML
  if [ -z "${INSPECT_SH}" ]; then
    return 0
  fi
  if [ ! -x "${INSPECT_SH}" ]; then
    warn "INSPECT_SH is set but not executable: ${INSPECT_SH}"
    return 0
  fi
  if [ ! -f "$ROOT/.idea/misc.xml" ]; then
    warn "No .idea project files found. Headless inspections skipped."
    return 0
  fi
  if [ ! -f "$ROOT/$INSPECT_PROFILE" ]; then
    warn "Inspection profile not found: $ROOT/$INSPECT_PROFILE (export one from IDE to enable headless inspections)."
    return 0
  fi

  log "Running headless IDE inspections via inspect.sh…"
  mkdir -p "$INSPECT_OUT"
  # Args: <project-path> <profile-path> <output-path>
  if ! "${INSPECT_SH}" "$ROOT" "$ROOT/$INSPECT_PROFILE" "$INSPECT_OUT" >"$LOGDIR/inspect.out" 2>"$LOGDIR/inspect.err"; then
    warn "inspect.sh returned non-zero. See logs."
  fi
}

print_summary() {
  log "----- SUMMARY -----"
  local todo_count unused_res_count warn_count
  todo_count=$( [ -f "$TODODIR/todos.txt" ] && wc -l < "$TODODIR/todos.txt" | tr -d ' ' || echo 0 )
  unused_res_count=$( [ -f "$LINTDIR/_unused_resources.count" ] && cat "$LINTDIR/_unused_resources.count" || echo 0 )
  warn_count=$( [ -f "$OUTDIR/warnings-summary.txt" ] && wc -l < "$OUTDIR/warnings-summary.txt" | tr -d ' ' || echo 0 )

  echo "Report dir: $OUTDIR"
  echo "  TODO/FIXME/etc:   $todo_count  →  $TODODIR/todos.txt"
  echo "  Lint reports:      $(ls -1 "$LINTDIR" 2>/dev/null | wc -l | tr -d ' ') files → $LINTDIR/"
  echo "    UnusedResources: $unused_res_count (from XML, if present)"
  echo "  Build warnings:    $warn_count  →  $OUTDIR/warnings-summary.txt"

  # Try to open HTML lint reports on macOS for convenience
 if command -v open >/dev/null 2>&1; then
     first_html="$(find "$LINTDIR" -maxdepth 1 -name "lint-results*.html" | sort | head -n 1)"
     if [ -n "${first_html:-}" ]; then
       log "Opening first lint HTML report…"
       open "$first_html" || true
     fi
   fi
}

main() {
  log "Output directory: $OUTDIR"
  scan_todos
  run_lint
  run_build_warns
  run_inspect_headless
  print_summary
}

main "$@"
