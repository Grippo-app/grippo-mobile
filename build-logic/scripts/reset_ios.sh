#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
GRADLEW="$PROJECT_ROOT/gradlew"

log_step() {
  printf "\n%s\n" "$1"
}

fail() {
  printf "❌ %s\n" "$1" >&2
  exit 1
}

[[ -x "$GRADLEW" ]] || fail "Gradle wrapper not found at $GRADLEW"

log_step "🧹 Cleaning Gradle build outputs..."
"$GRADLEW" -p "$PROJECT_ROOT" clean

log_step "🧽 Clearing iOS caches & build artifacts..."
declare -a CLEAN_TARGETS=(
  "$PROJECT_ROOT/iosApp/build"
  "$PROJECT_ROOT/shared/build"
  "$HOME/Library/Developer/Xcode/DerivedData"
)

for target in "${CLEAN_TARGETS[@]}"; do
  if [ -e "$target" ]; then
    rm -rf "$target"
    printf "  • removed %s\n" "$target"
  fi
done

log_step "📦 Building shared.xcframework (Debug + Release)..."
"$GRADLEW" -p "$PROJECT_ROOT" \
  :shared:assembleSharedDebugXCFramework \
  :shared:assembleSharedReleaseXCFramework

declare -a EXPECTED_FRAMEWORKS=(
  "$PROJECT_ROOT/shared/build/XCFrameworks/debug/shared.xcframework"
  "$PROJECT_ROOT/shared/build/XCFrameworks/release/shared.xcframework"
)

for framework in "${EXPECTED_FRAMEWORKS[@]}"; do
  [ -d "$framework" ] || fail "Expected framework not found at $framework"
done

log_step "✅ iOS project reset complete and XCFrameworks ready."
