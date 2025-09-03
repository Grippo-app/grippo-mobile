# Setup and Installation Guide

## Prerequisites

Before you begin, ensure you have the following installed:

- **JDK** (recommended version in gradle.properties)
- **Android Studio** (recent version)
- **Xcode** (recent version, for iOS development)
- **CocoaPods** (for iOS development)
- **Git**

## Initial Setup

### 1. Clone the Repository

```bash
git clone https://github.com/your-organization/grippo-mobile.git
cd grippo-mobile
```

### 2. Android Setup

1. Open Android Studio
2. Select "Open an Existing Project" and navigate to the cloned repository
3. Wait for the Gradle sync to complete
4. Android Studio should prompt you to install any missing SDK components
    - Required SDK versions are defined in the project's build files
    - Make sure to install any recommended components

### 3. iOS Setup

1. Install CocoaPods if you haven't already:
   ```bash
   sudo gem install cocoapods
   ```

2. Navigate to the iOS project directory:
   ```bash
   cd iosApp
   ```

3. Install pods:
   ```bash
   pod install
   ```

4. Open the generated `.xcworkspace` file:
   ```bash
   open iosApp.xcworkspace
   ```

## Building the Project

### Building for Android

From the command line:

```bash
./gradlew :androidApp:assembleDebug
```

Or in Android Studio:

1. Select the `androidApp` configuration
2. Click the "Run" button

### Building for iOS

From the command line:

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
cd iosApp
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp -configuration Debug -sdk iphonesimulator
```

Or in Xcode:

1. Open `iosApp.xcworkspace`
2. Select the `iosApp` scheme
3. Select a simulator or device
4. Click the "Run" button

## Troubleshooting

### Common Issues

#### Gradle Sync Failed

If Gradle sync fails:

1. Ensure you have the correct JDK version
2. Try invalidating caches: `File > Invalidate Caches / Restart`
3. Check your internet connection for dependency downloads

#### iOS Build Errors

If you encounter iOS build errors:

1. Ensure CocoaPods is installed correctly
2. Try cleaning the build: `Product > Clean Build Folder` in Xcode
3. Rebuild the shared module: `./gradlew :shared:embedAndSignAppleFrameworkForXcode`

#### KMM Plugin Issues

If the KMM plugin isn't working properly:

1. Ensure you have the latest version installed
2. Restart Android Studio
3. Check for compatibility issues with your Android Studio version

## Full Clean Build

If you encounter persistent build issues, you may need to perform a full clean:

1. **Clean Gradle Build Files**
    - Run `./gradlew clean --no-daemon`
    - Remove build directories and Gradle caches

2. **Clean iOS Build Files** (for iOS development)
    - Remove Pods directory and Podfile.lock
    - Clean Xcode derived data
    - Reinstall pods

A helper script for this process is available in the project root. See the README for more details.