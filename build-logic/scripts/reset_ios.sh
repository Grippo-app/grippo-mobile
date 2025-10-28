#!/bin/bash
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "🧹 Cleaning Gradle build..."
(cd "$PROJECT_ROOT" && ./gradlew clean)

echo "🗑 Removing legacy CocoaPods artifacts..."
rm -rf "$PROJECT_ROOT/iosApp/Pods"
rm -f "$PROJECT_ROOT/iosApp/Podfile"
rm -f "$PROJECT_ROOT/iosApp/Podfile.lock"
rm -rf "$PROJECT_ROOT/iosApp/iosApp.xcworkspace"

echo "🧽 Clearing iOS build outputs..."
rm -rf "$PROJECT_ROOT/iosApp/build"
rm -rf "$PROJECT_ROOT/shared/build"
rm -rf ~/Library/Developer/Xcode/DerivedData

echo "📦 Building XCFrameworks for Swift Package Manager (Debug + Release)..."
(cd "$PROJECT_ROOT" && ./gradlew :shared:syncSharedDebugXCFrameworkForSPM :shared:syncSharedReleaseXCFrameworkForSPM)

echo "✅ iOS project reset + Swift package assets prepared!"
