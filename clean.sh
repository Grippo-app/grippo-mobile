#!/bin/bash

set -e

echo "🧹 Step 1: Cleaning Gradle..."
./gradlew clean --no-daemon

echo "🧹 Step 2: Deleting build/, .gradle/, .podspec..."
find . -type d -name "build" -exec rm -rf {} + || true
find . -type d -name ".gradle" -exec rm -rf {} + || true
find . -type f -name "*.podspec" -exec rm -f {} + || true

echo "🧹 Step 3: Cleaning local Gradle caches..."
rm -rf ~/.gradle/caches/modules-2/modules-2.lock
rm -rf ~/.gradle/caches/transforms-*

echo "⚙️  Step 4: Finding all :generateDummyFramework tasks..."
DUMMY_TASKS=$(./gradlew tasks --all | grep "^:" | grep "generateDummyFramework" | awk '{print $1}')

if [ -n "$DUMMY_TASKS" ]; then
  echo "✅ Found tasks:"
  echo "$DUMMY_TASKS"
  echo "🚀 Executing all generateDummyFramework tasks..."
  ./gradlew $DUMMY_TASKS --no-daemon
else
  echo "⚠️  No generateDummyFramework tasks found. Are cocoapods {} blocks configured?"
fi

echo "🍏 Step 5: Cleaning iOS project..."
cd iosApp || exit 1
rm -rf Pods Podfile.lock build
rm -rf ~/Library/Developer/Xcode/DerivedData

echo "🍏 Step 6: CocoaPods deintegrate & install..."
pod deintegrate || true
pod install

cd ..

echo "✅ DONE: Project fully cleaned and prepared for cold start."
