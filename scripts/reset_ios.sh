#!/bin/bash
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "🧹 Cleaning Gradle build..."
(cd "$PROJECT_ROOT" && ./gradlew clean)

echo "🗑 Removing iOS Pods, Podfile.lock, DerivedData..."
rm -rf "$PROJECT_ROOT/iosApp/Pods"
rm -rf "$PROJECT_ROOT/iosApp/Podfile.lock"
rm -rf "$PROJECT_ROOT/iosApp/build"
rm -rf "$PROJECT_ROOT/shared/build"
rm -rf ~/Library/Developer/Xcode/DerivedData

echo "🧹 Deintegrating old CocoaPods..."
(cd "$PROJECT_ROOT/iosApp" && pod deintegrate || true)

echo "📦 Generating dummy framework for CocoaPods..."
(cd "$PROJECT_ROOT" && ./gradlew :shared:generateDummyFramework)

echo "🏗 Building XCFrameworks for CocoaPods (Debug)..."
(cd "$PROJECT_ROOT" && ./gradlew :shared:podPublishDebugXCFramework)

echo "📦 Installing CocoaPods..."
(cd "$PROJECT_ROOT/iosApp" && pod install)

echo "✅ iOS project reset + framework build complete!"
