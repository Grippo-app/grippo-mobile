# iOS app project (`:iosApp`) — drop-in files

`:iosApp` is an Xcode project, **not** a Gradle module. Its `project.pbxproj` is a
machine-managed object graph (cross-referenced UUIDs) — do **not** hand-author it.
Copy the fences below **verbatim** to their `Target path`, then substitute placeholders.
This is a self-contained build-time reference. (For *why* the Xcode side needs the
`Compile Kotlin Framework` run-script + `FRAMEWORK_SEARCH_PATHS`, see the iOS SwiftPackage
reference, `ios-framework.md`.)

## How to apply (referenced from `launch.md` Step 11)

1. Create each file below at its `Target path` (relative to the project root).
2. Substitute every placeholder with the `orchestrator/project-config.md` value:
   - `<Product>` — product display name (also the built `<Product>.app` and `CFBundleName`).
   - `<bundleId>` — iOS bundle identifier; same as `applicationId` (`com.<org>.<product>`, no platform suffix).
   - `<iosFrameworkName>` — `iosFrameworkName` (default `shared`).
   - `<org>` — organization name.
   - `<TEAM_ID>` — Apple Developer Team ID; leave empty for the simulator, set it for device builds.
3. **Firebase gate** (`firebaseEnabled`):
   - `true` → use **`project.pbxproj` (firebaseEnabled: true)**; keep `IosFirebase*.swift` +
     `GoogleService-Info.plist`; keep the `// region firebase-conditional … // endregion
     firebase-conditional` blocks in `AppDelegate.swift` (the marker comment lines may stay).
   - `false` → use **`project.pbxproj` (firebaseEnabled: false)**; omit the three `IosFirebase*.swift`
     files and `GoogleService-Info.plist`; strip everything between the
     `// region firebase-conditional` / `// endregion firebase-conditional` markers in
     `AppDelegate.swift`.
4. `chmod +x iosApp/run-ios.sh`.
5. Build the framework once so Xcode can resolve `import <iosFrameworkName>`:
   `./gradlew :shared:assemble<IosFrameworkName>DebugXCFramework`.

The shared scheme (`iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`) is what makes the
`iosApp` run configuration appear in Android Studio and lets `xcodebuild -scheme iosApp` resolve.

The template intentionally omits product-specific pieces (push/APNs, notification deeplinks, Google
Sign-In `Info.plist` keys, entitlements, real app icons) — add them when the product needs them.

---

## Swift sources

### iOSApp.swift

_Target path:_ `iosApp/iosApp/iOSApp.swift`

```swift
import SwiftUI
import <iosFrameworkName>

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate: AppDelegate

    init() {
        // Koin.init — `Koin` is a Kotlin `object`, so the generated Swift name is `Koin`, not `KoinKt`.
        Koin().doInit(appDeclaration: { _ in })
    }

    var body: some Scene {
        WindowGroup {
            RootView(root: appDelegate.root, backDispatcher: appDelegate.backDispatcher)
                .ignoresSafeArea(edges: .all) // https://youtrack.jetbrains.com/issue/CMP-3621
        }
    }
}
```

### AppDelegate.swift

Firebase blocks are marked `// region firebase-conditional`; strip them when `firebaseEnabled: false`.

_Target path:_ `iosApp/iosApp/AppDelegate.swift`

```swift
import UIKit
import <iosFrameworkName>
// region firebase-conditional
import FirebaseCore
// endregion firebase-conditional

/// Owns the root `RootComponent` and the Essenty `BackDispatcher`.
///
/// The root is constructed here (not in `iOSApp.init`) so the `AppDelegate` is fully attached to
/// `UIApplication` before first use.
// region firebase-conditional
/// This also lets Firebase's `AppDelegateSwizzler` find the delegate: configure Firebase from
/// `application(_:didFinishLaunchingWithOptions:)`, since `iOSApp.init` runs too early for it.
// endregion firebase-conditional
class AppDelegate: NSObject, UIApplicationDelegate {

    let backDispatcher: BackDispatcher = BackDispatcherKt.BackDispatcher()

    lazy var root: RootComponent = RootComponent(
        componentContext: DefaultComponentContext(
            lifecycle: ApplicationLifecycle(),
            stateKeeper: nil,
            instanceKeeper: nil,
            backHandler: backDispatcher
        ),
        close: {
            // Soft-hide the app (iOS has no supported "exit" — sending to background is the norm).
            UIApplication.shared.perform(#selector(NSXPCConnection.suspend))
        },
        deeplink: nil
    )

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // region firebase-conditional
        FirebaseApp.configure()
        FirebaseProvider.shared.setup(
            analytics: IosFirebaseAnalytics(),
            crashlytics: IosFirebaseCrashlytics(),
            messaging: IosFirebaseMessaging()
        )
        // endregion firebase-conditional
        return true
    }
}
```

### RootView.swift

_Target path:_ `iosApp/iosApp/RootView.swift`

```swift
import SwiftUI
import <iosFrameworkName>

/// Bridges the Kotlin-side `rootViewController(root:backDispatcher:)` (exported from
/// `:<iosFrameworkName>` / iosMain) into SwiftUI. The `RootComponent` is constructed once in
/// `AppDelegate` and injected here.
struct RootView: UIViewControllerRepresentable {
    let root: RootComponent
    let backDispatcher: BackDispatcher

    func makeUIViewController(context: Context) -> UIViewController {
        RootViewControllerKt.rootViewController(root: root, backDispatcher: backDispatcher)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

## Firebase providers (only when `firebaseEnabled: true`)

### IosFirebaseAnalytics.swift

_Target path:_ `iosApp/iosApp/IosFirebaseAnalytics.swift`

```swift
import Foundation
import <iosFrameworkName>
import FirebaseAnalytics

final class IosFirebaseAnalytics: FirebaseAnalyticsProvider {

    func logEvent(name: String, params: [String: String]) {
        Analytics.logEvent(name, parameters: params)
    }
}
```

### IosFirebaseCrashlytics.swift

_Target path:_ `iosApp/iosApp/IosFirebaseCrashlytics.swift`

```swift
import Foundation
import <iosFrameworkName>
import FirebaseCrashlytics

final class IosFirebaseCrashlytics: FirebaseCrashlyticsProvider {

    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func recordException(
        throwable: KotlinThrowable,
        metadata: [String: String]
    ) {
        let crashlytics = Crashlytics.crashlytics()

        metadata.forEach { key, value in
            crashlytics.setCustomValue(value, forKey: key)
        }

        let stackTraceArray = throwable.getStackTrace()

        var stackTraceLines: [String] = []
        stackTraceLines.reserveCapacity(Int(stackTraceArray.size))

        for i in 0..<stackTraceArray.size {
            if let element = stackTraceArray.get(index: i) {
                stackTraceLines.append(String(describing: element))
            }
        }

        let stackTrace = stackTraceLines.joined(separator: "\n")

        let description =
            throwable.message ??
                String(describing: type(of: throwable))

        let nsError = NSError(
            domain: "KotlinException.\(String(describing: type(of: throwable)))",
            code: 1,
            userInfo: [
                NSLocalizedDescriptionKey: description,
                "kotlin_stacktrace": stackTrace
            ]
        )

        crashlytics.record(error: nsError)
    }
}
```

### IosFirebaseMessaging.swift

_Target path:_ `iosApp/iosApp/IosFirebaseMessaging.swift`

```swift
import FirebaseMessaging
import Foundation
import <iosFrameworkName>

/// Implements the KMP `FirebaseMessagingProvider` interface using the native Firebase Messaging SDK.
/// The FCM token is available only after APNs registration completes. Wiring the APNs token and
/// `MessagingDelegate` (token refresh, foreground banners, deeplink taps) is product-specific and
/// intentionally left out of the bootstrap shell — add it when the product needs push.
final class IosFirebaseMessaging: FirebaseMessagingProvider {

    func getToken(completionHandler: @escaping (String?, Error?) -> Void) {
        Messaging.messaging().token { token, error in
            if let token {
                completionHandler(token, nil)
                return
            }
            // Firebase may be asked for the FCM token before APNs registration finishes.
            // Treat that as "not ready yet" rather than a hard error.
            _ = error
            completionHandler(nil, nil)
        }
    }
}
```

## Resources & config

### Info.plist

_Target path:_ `iosApp/iosApp/Info.plist`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<!-- Required by Compose Multiplatform on iOS, otherwise the app crashes on launch.
	     https://youtrack.jetbrains.com/issue/CMP-6849 -->
	<key>CADisableMinimumFrameDurationOnPhone</key>
	<true/>
	<key>CFBundleDevelopmentRegion</key>
	<string>en</string>
	<key>CFBundleExecutable</key>
	<string>$(EXECUTABLE_NAME)</string>
	<key>CFBundleIdentifier</key>
	<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>
	<key>CFBundleInfoDictionaryVersion</key>
	<string>6.0</string>
	<key>CFBundleName</key>
	<string>$(PRODUCT_NAME)</string>
	<key>CFBundlePackageType</key>
	<string>$(PRODUCT_BUNDLE_PACKAGE_TYPE)</string>
	<key>CFBundleShortVersionString</key>
	<string>$(MARKETING_VERSION)</string>
	<key>CFBundleVersion</key>
	<string>$(CURRENT_PROJECT_VERSION)</string>
	<key>LSRequiresIPhoneOS</key>
	<true/>
	<key>UIApplicationSceneManifest</key>
	<dict>
		<key>UIApplicationSupportsMultipleScenes</key>
		<false/>
	</dict>
	<key>UILaunchScreen</key>
	<dict/>
	<key>UISupportedInterfaceOrientations</key>
	<array>
		<string>UIInterfaceOrientationPortrait</string>
		<string>UIInterfaceOrientationLandscapeLeft</string>
		<string>UIInterfaceOrientationLandscapeRight</string>
	</array>
	<key>UISupportedInterfaceOrientations~ipad</key>
	<array>
		<string>UIInterfaceOrientationPortrait</string>
		<string>UIInterfaceOrientationPortraitUpsideDown</string>
		<string>UIInterfaceOrientationLandscapeLeft</string>
		<string>UIInterfaceOrientationLandscapeRight</string>
	</array>
	<key>ITSAppUsesNonExemptEncryption</key>
	<false/>
</dict>
</plist>
```

### Config.xcconfig

_Target path:_ `iosApp/Configuration/Config.xcconfig`

```ini
// Project configuration. The bootstrap substitutes the placeholders below from project-config.
// PRODUCT_NAME also defines the built product name and CFBundleName.

TEAM_ID = <TEAM_ID>

PRODUCT_NAME = <Product>
PRODUCT_BUNDLE_IDENTIFIER = <bundleId>

CURRENT_PROJECT_VERSION = 1
MARKETING_VERSION = 1.0
```

### Assets.xcassets/Contents.json

_Target path:_ `iosApp/iosApp/Assets.xcassets/Contents.json`

```json
{
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
```

### Assets.xcassets/AppIcon.appiconset/Contents.json

_Target path:_ `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json`

```json
{
  "images" : [
    {
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
```

### Assets.xcassets/AccentColor.colorset/Contents.json

_Target path:_ `iosApp/iosApp/Assets.xcassets/AccentColor.colorset/Contents.json`

```json
{
  "colors" : [
    {
      "idiom" : "universal"
    }
  ],
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
```

### Preview Content/Preview Assets.xcassets/Contents.json

_Target path:_ `iosApp/iosApp/Preview Content/Preview Assets.xcassets/Contents.json`

```json
{
  "info" : {
    "author" : "xcode",
    "version" : 1
  }
}
```

### GoogleService-Info.plist (firebaseEnabled: true only — placeholder)

_Target path:_ `iosApp/GoogleService-Info.plist`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<!-- PLACEHOLDER. Replace with the real GoogleService-Info.plist from the Firebase console
     (Project settings -> your iOS app -> Download). The app builds with this stub, but
     FirebaseApp.configure() will not connect to a real project until it is replaced. -->
<plist version="1.0">
<dict>
	<key>API_KEY</key>
	<string>REPLACE_ME</string>
	<key>GCM_SENDER_ID</key>
	<string>000000000000</string>
	<key>PLIST_VERSION</key>
	<string>1</string>
	<key>BUNDLE_ID</key>
	<string><bundleId></string>
	<key>PROJECT_ID</key>
	<string>replace-me</string>
	<key>STORAGE_BUCKET</key>
	<string>replace-me.appspot.com</string>
	<key>IS_ADS_ENABLED</key>
	<false/>
	<key>IS_ANALYTICS_ENABLED</key>
	<false/>
	<key>IS_APPINVITE_ENABLED</key>
	<true/>
	<key>IS_GCM_ENABLED</key>
	<true/>
	<key>IS_SIGNIN_ENABLED</key>
	<true/>
	<key>GOOGLE_APP_ID</key>
	<string>1:000000000000:ios:0000000000000000000000</string>
</dict>
</plist>
```

## Xcode project metadata

### iosApp.xcscheme (SHARED scheme)

_Target path:_ `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Scheme
   LastUpgradeVersion = "2610"
   version = "1.7">
   <BuildAction
      parallelizeBuildables = "YES"
      buildImplicitDependencies = "YES">
      <BuildActionEntries>
         <BuildActionEntry
            buildForTesting = "YES"
            buildForRunning = "YES"
            buildForProfiling = "YES"
            buildForArchiving = "YES"
            buildForAnalyzing = "YES">
            <BuildableReference
               BuildableIdentifier = "primary"
               BlueprintIdentifier = "7555FF7A242A565900829871"
               BuildableName = "<Product>.app"
               BlueprintName = "iosApp"
               ReferencedContainer = "container:iosApp.xcodeproj">
            </BuildableReference>
         </BuildActionEntry>
      </BuildActionEntries>
   </BuildAction>
   <TestAction
      buildConfiguration = "Debug"
      selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB"
      selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB"
      shouldUseLaunchSchemeArgsEnv = "YES">
   </TestAction>
   <LaunchAction
      buildConfiguration = "Debug"
      selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB"
      selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB"
      launchStyle = "0"
      useCustomWorkingDirectory = "NO"
      ignoresPersistentStateOnLaunch = "NO"
      debugDocumentVersioning = "YES"
      debugServiceExtension = "internal"
      allowLocationSimulation = "YES">
      <BuildableProductRunnable
         runnableDebuggingMode = "0">
         <BuildableReference
            BuildableIdentifier = "primary"
            BlueprintIdentifier = "7555FF7A242A565900829871"
            BuildableName = "<Product>.app"
            BlueprintName = "iosApp"
            ReferencedContainer = "container:iosApp.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </LaunchAction>
   <ProfileAction
      buildConfiguration = "Release"
      shouldUseLaunchSchemeArgsEnv = "YES"
      savedToolIdentifier = ""
      useCustomWorkingDirectory = "NO"
      debugDocumentVersioning = "YES">
      <BuildableProductRunnable
         runnableDebuggingMode = "0">
         <BuildableReference
            BuildableIdentifier = "primary"
            BlueprintIdentifier = "7555FF7A242A565900829871"
            BuildableName = "<Product>.app"
            BlueprintName = "iosApp"
            ReferencedContainer = "container:iosApp.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </ProfileAction>
   <AnalyzeAction
      buildConfiguration = "Debug">
   </AnalyzeAction>
   <ArchiveAction
      buildConfiguration = "Release"
      revealArchiveInOrganizer = "YES">
   </ArchiveAction>
</Scheme>
```

### project.xcworkspace/contents.xcworkspacedata

_Target path:_ `iosApp/iosApp.xcodeproj/project.xcworkspace/contents.xcworkspacedata`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Workspace
   version = "1.0">
   <FileRef
      location = "self:">
   </FileRef>
</Workspace>
```

### WorkspaceSettings.xcsettings

_Target path:_ `iosApp/iosApp.xcodeproj/project.xcworkspace/xcshareddata/WorkspaceSettings.xcsettings`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>BuildSystemType</key>
	<string>Latest</string>
	<key>IDEWorkspaceSharedSettings_AutocreateContextsIfNeeded</key>
	<false/>
</dict>
</plist>
```

## Run helper

### run-ios.sh

Headless build + simulator launch. `chmod +x` after copying.

_Target path:_ `iosApp/run-ios.sh`

```bash
#!/usr/bin/env bash
#
# Build and launch the app on an iOS simulator — headless, no Xcode GUI needed.
# This is the "how do I run iOS?" entry point. From Android Studio you can instead
# pick the `iosApp` run configuration (the shared scheme makes it appear).
#
# Usage:
#   ./run-ios.sh --udid AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE
#   ./run-ios.sh --udid AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE --build-only
#   ./run-ios.sh --udid AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE --install-only
#   ./run-ios.sh --udid AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE \
#     --configuration-id debug \
#     --derived-data-path /absolute/project/orchestrator/.cache/runtime/app-run/headless-ios
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # <project>/iosApp
PROJECT_ROOT="$(cd "$HERE/.." && pwd)"
BUNDLE_ID="<bundleId>"
SCHEME="iosApp"
UDID=""
CONFIGURATION_ID="debug"
DD="$PROJECT_ROOT/orchestrator/.cache/runtime/app-run/headless-ios"
BUILD_ONLY=false
INSTALL_ONLY=false

while (($#)); do
  case "$1" in
    --udid)
      (($# >= 2)) || { echo "ERROR: --udid requires a value" >&2; exit 2; }
      UDID="$2"
      shift 2
      ;;
    --configuration-id)
      (($# >= 2)) || { echo "ERROR: --configuration-id requires a value" >&2; exit 2; }
      CONFIGURATION_ID="$2"
      shift 2
      ;;
    --derived-data-path)
      (($# >= 2)) || { echo "ERROR: --derived-data-path requires a value" >&2; exit 2; }
      DD="$2"
      shift 2
      ;;
    --build-only)
      BUILD_ONLY=true
      shift
      ;;
    --install-only)
      INSTALL_ONLY=true
      shift
      ;;
    *)
      echo "ERROR: unsupported argument: $1" >&2
      exit 2
      ;;
  esac
done

[[ "$UDID" =~ ^[A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}$ ]] ||
  { echo "ERROR: --udid must be an exact Simulator UDID" >&2; exit 2; }
[[ "$CONFIGURATION_ID" == "debug" ]] ||
  { echo "ERROR: unsupported configuration id" >&2; exit 2; }
[[ "$BUILD_ONLY" != true || "$INSTALL_ONLY" != true ]] ||
  { echo "ERROR: --build-only and --install-only are mutually exclusive" >&2; exit 2; }
[[ "$DD" == /* && "$DD" != *"/../"* && "$DD" != *"/.." ]] ||
  { echo "ERROR: --derived-data-path must be a normalized absolute path" >&2; exit 2; }
case "$DD/" in
  "$PROJECT_ROOT/orchestrator/.cache/runtime/app-run/"*) ;;
  *)
    echo "ERROR: --derived-data-path must stay in the project app-run runtime directory" >&2
    exit 2
    ;;
esac

CONFIGURATION="Debug"
mkdir -p "$DD"
[[ -d "$DD" && ! -L "$DD" ]] ||
  { echo "ERROR: derived data directory is unsafe" >&2; exit 1; }
ALLOWED_DD_ROOT="$(cd "$PROJECT_ROOT/orchestrator/.cache/runtime/app-run" && pwd -P)"
DD_REAL="$(cd "$DD" && pwd -P)"
[[ "$DD_REAL" == "$DD" && "$DD_REAL/" == "$ALLOWED_DD_ROOT/"* ]] ||
  { echo "ERROR: derived data path must be canonical and contain no symlink ancestor" >&2; exit 2; }

if [[ "$INSTALL_ONLY" != true ]]; then
  echo "==> Building $SCHEME ($CONFIGURATION, iphonesimulator)…"
  xcodebuild build \
    -project "$HERE/iosApp.xcodeproj" \
    -scheme "$SCHEME" \
    -configuration "$CONFIGURATION" \
    -sdk iphonesimulator \
    -destination "generic/platform=iOS Simulator" \
    -derivedDataPath "$DD" \
    CODE_SIGNING_ALLOWED=NO
fi

if [[ "$BUILD_ONLY" == true ]]; then
  echo "==> Build completed."
  exit 0
fi

PRODUCTS="$DD/Build/Products/$CONFIGURATION-iphonesimulator"
shopt -s nullglob
APPS=("$PRODUCTS"/*.app)
shopt -u nullglob
[[ ${#APPS[@]} -eq 1 && -d "${APPS[0]}" && ! -L "${APPS[0]}" ]] ||
  { echo "ERROR: expected exactly one regular .app under $PRODUCTS" >&2; exit 1; }
APP="${APPS[0]}"

echo "==> Booting exact simulator $UDID…"
if ! xcrun simctl boot "$UDID"; then
  echo "==> Boot request did not complete; verifying the exact simulator state…"
fi
xcrun simctl bootstatus "$UDID" -b
open -a Simulator >/dev/null 2>&1 || true
xcrun simctl install "$UDID" "$APP"
xcrun simctl launch "$UDID" "$BUNDLE_ID"
echo "==> Launched $BUNDLE_ID on $UDID."
```

## `project.pbxproj`

Use exactly one of the two variants below at `iosApp/iosApp.xcodeproj/project.pbxproj`, per `firebaseEnabled`.

### project.pbxproj (firebaseEnabled: true)

_Target path:_ `iosApp/iosApp.xcodeproj/project.pbxproj`

```text
// !$*UTF8*$!
{
	archiveVersion = 1;
	classes = {
	};
	objectVersion = 54;
	objects = {

/* Begin PBXBuildFile section */
		0377D59153A64B33A7C93C96 /* FirebaseMessaging in Frameworks */ = {isa = PBXBuildFile; productRef = 7D30A510752A46AD81BF482F /* FirebaseMessaging */; };
		058557BB273AAA24004C7B11 /* Assets.xcassets in Resources */ = {isa = PBXBuildFile; fileRef = 058557BA273AAA24004C7B11 /* Assets.xcassets */; };
		058557D9273AAEEB004C7B11 /* Preview Assets.xcassets in Resources */ = {isa = PBXBuildFile; fileRef = 058557D8273AAEEB004C7B11 /* Preview Assets.xcassets */; };
		19B4E580717143CBAA477F29 /* IosFirebaseMessaging.swift in Sources */ = {isa = PBXBuildFile; fileRef = 2F7AD9C81ABB4D679CDC57B2 /* IosFirebaseMessaging.swift */; };
		2152FB042600AC8F00CF470E /* iOSApp.swift in Sources */ = {isa = PBXBuildFile; fileRef = 2152FB032600AC8F00CF470E /* iOSApp.swift */; };
		BA393ADD2F11A8AF00DE3DF3 /* FirebaseAnalytics in Frameworks */ = {isa = PBXBuildFile; productRef = BA393ADC2F11A8AF00DE3DF3 /* FirebaseAnalytics */; };
		BA393ADF2F11A8AF00DE3DF3 /* FirebaseAnalyticsCore in Frameworks */ = {isa = PBXBuildFile; productRef = BA393ADE2F11A8AF00DE3DF3 /* FirebaseAnalyticsCore */; };
		BA393AE12F11A8AF00DE3DF3 /* FirebaseCore in Frameworks */ = {isa = PBXBuildFile; productRef = BA393AE02F11A8AF00DE3DF3 /* FirebaseCore */; };
		BA393AE32F11A8AF00DE3DF3 /* FirebaseCrashlytics in Frameworks */ = {isa = PBXBuildFile; productRef = BA393AE22F11A8AF00DE3DF3 /* FirebaseCrashlytics */; };
		BA393AE72F11B00700DE3DF3 /* GoogleService-Info.plist in Resources */ = {isa = PBXBuildFile; fileRef = BA393AE62F11B00700DE3DF3 /* GoogleService-Info.plist */; };
		BA853F4A2DC657D900A34846 /* AppDelegate.swift in Sources */ = {isa = PBXBuildFile; fileRef = BA853F492DC657D300A34846 /* AppDelegate.swift */; };
		BA853F4C2DC658B000A34846 /* RootView.swift in Sources */ = {isa = PBXBuildFile; fileRef = BA853F4B2DC658AF00A34846 /* RootView.swift */; };
		BACA7A892F124F4E003B6963 /* IosFirebaseCrashlytics.swift in Sources */ = {isa = PBXBuildFile; fileRef = BACA7A882F124F4E003B6963 /* IosFirebaseCrashlytics.swift */; };
		BAFDC8BD2F11697600971CAB /* IosFirebaseAnalytics.swift in Sources */ = {isa = PBXBuildFile; fileRef = BAFDC8BC2F11697600971CAB /* IosFirebaseAnalytics.swift */; };
		D3D0B9802FA23C2C00AA1234 /* libsqlite3.tbd in Frameworks */ = {isa = PBXBuildFile; fileRef = D3D0B97F2FA23C2C00AA1234 /* libsqlite3.tbd */; };
/* End PBXBuildFile section */

/* Begin PBXFileReference section */
		058557BA273AAA24004C7B11 /* Assets.xcassets */ = {isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = Assets.xcassets; sourceTree = "<group>"; };
		058557D8273AAEEB004C7B11 /* Preview Assets.xcassets */ = {isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = "Preview Assets.xcassets"; sourceTree = "<group>"; };
		2152FB032600AC8F00CF470E /* iOSApp.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = iOSApp.swift; sourceTree = "<group>"; };
		2F7AD9C81ABB4D679CDC57B2 /* IosFirebaseMessaging.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = IosFirebaseMessaging.swift; sourceTree = "<group>"; };
		7555FF7B242A565900829871 /* <Product>.app */ = {isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = "<Product>.app"; sourceTree = BUILT_PRODUCTS_DIR; };
		7555FF8C242A565B00829871 /* Info.plist */ = {isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = "<group>"; };
		BA393AE62F11B00700DE3DF3 /* GoogleService-Info.plist */ = {isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = "GoogleService-Info.plist"; sourceTree = "<group>"; };
		BA853F492DC657D300A34846 /* AppDelegate.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = AppDelegate.swift; sourceTree = "<group>"; };
		BA853F4B2DC658AF00A34846 /* RootView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = RootView.swift; sourceTree = "<group>"; };
		BACA7A882F124F4E003B6963 /* IosFirebaseCrashlytics.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = IosFirebaseCrashlytics.swift; sourceTree = "<group>"; };
		BAFDC8BC2F11697600971CAB /* IosFirebaseAnalytics.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = IosFirebaseAnalytics.swift; sourceTree = "<group>"; };
		D3D0B97F2FA23C2C00AA1234 /* libsqlite3.tbd */ = {isa = PBXFileReference; lastKnownFileType = "sourcecode.text-based-dylib-definition"; name = libsqlite3.tbd; path = usr/lib/libsqlite3.tbd; sourceTree = SDKROOT; };
		F0054F872EE5B3C300F40B1C /* Config.xcconfig */ = {isa = PBXFileReference; lastKnownFileType = text.xcconfig; path = Config.xcconfig; sourceTree = "<group>"; };
/* End PBXFileReference section */

/* Begin PBXFrameworksBuildPhase section */
		22C4659C9711D113BD9C1DBA /* Frameworks */ = {
			isa = PBXFrameworksBuildPhase;
			buildActionMask = 2147483647;
			files = (
				BA393ADD2F11A8AF00DE3DF3 /* FirebaseAnalytics in Frameworks */,
				D3D0B9802FA23C2C00AA1234 /* libsqlite3.tbd in Frameworks */,
				BA393ADF2F11A8AF00DE3DF3 /* FirebaseAnalyticsCore in Frameworks */,
				BA393AE32F11A8AF00DE3DF3 /* FirebaseCrashlytics in Frameworks */,
				BA393AE12F11A8AF00DE3DF3 /* FirebaseCore in Frameworks */,
				0377D59153A64B33A7C93C96 /* FirebaseMessaging in Frameworks */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXFrameworksBuildPhase section */

/* Begin PBXGroup section */
		058557D7273AAEEB004C7B11 /* Preview Content */ = {
			isa = PBXGroup;
			children = (
				058557D8273AAEEB004C7B11 /* Preview Assets.xcassets */,
			);
			path = "Preview Content";
			sourceTree = "<group>";
		};
		7555FF72242A565900829871 = {
			isa = PBXGroup;
			children = (
				7555FF7D242A565900829871 /* iosApp */,
				F0054F862EE5B3C300F40B1C /* Configuration */,
				7555FF7C242A565900829871 /* Products */,
				D3D0B9812FA23C2C00AA1234 /* Frameworks */,
				BA393AE62F11B00700DE3DF3 /* GoogleService-Info.plist */,
			);
			sourceTree = "<group>";
		};
		7555FF7C242A565900829871 /* Products */ = {
			isa = PBXGroup;
			children = (
				7555FF7B242A565900829871 /* <Product>.app */,
			);
			name = Products;
			sourceTree = "<group>";
		};
		7555FF7D242A565900829871 /* iosApp */ = {
			isa = PBXGroup;
			children = (
				BAFDC8BC2F11697600971CAB /* IosFirebaseAnalytics.swift */,
				BACA7A882F124F4E003B6963 /* IosFirebaseCrashlytics.swift */,
				2F7AD9C81ABB4D679CDC57B2 /* IosFirebaseMessaging.swift */,
				BA853F4B2DC658AF00A34846 /* RootView.swift */,
				BA853F492DC657D300A34846 /* AppDelegate.swift */,
				058557BA273AAA24004C7B11 /* Assets.xcassets */,
				7555FF8C242A565B00829871 /* Info.plist */,
				2152FB032600AC8F00CF470E /* iOSApp.swift */,
				058557D7273AAEEB004C7B11 /* Preview Content */,
			);
			path = iosApp;
			sourceTree = "<group>";
		};
		D3D0B9812FA23C2C00AA1234 /* Frameworks */ = {
			isa = PBXGroup;
			children = (
				D3D0B97F2FA23C2C00AA1234 /* libsqlite3.tbd */,
			);
			name = Frameworks;
			sourceTree = "<group>";
		};
		F0054F862EE5B3C300F40B1C /* Configuration */ = {
			isa = PBXGroup;
			children = (
				F0054F872EE5B3C300F40B1C /* Config.xcconfig */,
			);
			path = Configuration;
			sourceTree = "<group>";
		};
/* End PBXGroup section */

/* Begin PBXNativeTarget section */
		7555FF7A242A565900829871 /* iosApp */ = {
			isa = PBXNativeTarget;
			buildConfigurationList = 7555FFA5242A565B00829871 /* Build configuration list for PBXNativeTarget "iosApp" */;
			buildPhases = (
				BB1A2B3C4D5E6F0011223344 /* Compile Kotlin Framework */,
				7555FF77242A565900829871 /* Sources */,
				7555FF79242A565900829871 /* Resources */,
				22C4659C9711D113BD9C1DBA /* Frameworks */,
			);
			buildRules = (
			);
			dependencies = (
			);
			name = iosApp;
			packageProductDependencies = (
				BA393ADC2F11A8AF00DE3DF3 /* FirebaseAnalytics */,
				BA393ADE2F11A8AF00DE3DF3 /* FirebaseAnalyticsCore */,
				BA393AE02F11A8AF00DE3DF3 /* FirebaseCore */,
				BA393AE22F11A8AF00DE3DF3 /* FirebaseCrashlytics */,
				7D30A510752A46AD81BF482F /* FirebaseMessaging */,
			);
			productName = iosApp;
			productReference = 7555FF7B242A565900829871 /* <Product>.app */;
			productType = "com.apple.product-type.application";
		};
/* End PBXNativeTarget section */

/* Begin PBXProject section */
		7555FF73242A565900829871 /* Project object */ = {
			isa = PBXProject;
			attributes = {
				BuildIndependentTargetsInParallel = YES;
				LastSwiftUpdateCheck = 1130;
				LastUpgradeCheck = 2610;
				ORGANIZATIONNAME = "<org>";
				TargetAttributes = {
					7555FF7A242A565900829871 = {
						CreatedOnToolsVersion = 11.3.1;
					};
				};
			};
			buildConfigurationList = 7555FF76242A565900829871 /* Build configuration list for PBXProject "iosApp" */;
			compatibilityVersion = "Xcode 9.3";
			developmentRegion = en;
			hasScannedForEncodings = 0;
			knownRegions = (
				en,
				Base,
			);
			mainGroup = 7555FF72242A565900829871;
			packageReferences = (
				BA393ADB2F11A8AF00DE3DF3 /* XCRemoteSwiftPackageReference "firebase-ios-sdk" */,
			);
			productRefGroup = 7555FF7C242A565900829871 /* Products */;
			projectDirPath = "";
			projectRoot = "";
			targets = (
				7555FF7A242A565900829871 /* iosApp */,
			);
		};
/* End PBXProject section */

/* Begin PBXResourcesBuildPhase section */
		7555FF79242A565900829871 /* Resources */ = {
			isa = PBXResourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				058557D9273AAEEB004C7B11 /* Preview Assets.xcassets in Resources */,
				058557BB273AAA24004C7B11 /* Assets.xcassets in Resources */,
				BA393AE72F11B00700DE3DF3 /* GoogleService-Info.plist in Resources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXResourcesBuildPhase section */

/* Begin PBXShellScriptBuildPhase section */
		BB1A2B3C4D5E6F0011223344 /* Compile Kotlin Framework */ = {
			isa = PBXShellScriptBuildPhase;
			alwaysOutOfDate = 1;
			buildActionMask = 2147483647;
			files = (
			);
			inputFileListPaths = (
			);
			inputPaths = (
			);
			name = "Compile Kotlin Framework";
			outputFileListPaths = (
			);
			outputPaths = (
			);
			runOnlyForDeploymentPostprocessing = 0;
			shellPath = /bin/sh;
			shellScript = "# Builds, copies and signs the Kotlin/Native framework for the current Xcode\n# configuration/SDK/arch. Output lands in <fw>/build/xcode-frameworks/$CONFIGURATION/$SDK_NAME,\n# which FRAMEWORK_SEARCH_PATHS points to. The framework module map autolinks it.\nif [ \"YES\" = \"$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED\" ]; then\n  echo \"Skipping Gradle build (OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES)\"\n  exit 0\nfi\ncd \"$SRCROOT/..\"\n./gradlew :shared:embedAndSignAppleFrameworkForXcode\n";
		};
/* End PBXShellScriptBuildPhase section */

/* Begin PBXSourcesBuildPhase section */
		7555FF77242A565900829871 /* Sources */ = {
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				BAFDC8BD2F11697600971CAB /* IosFirebaseAnalytics.swift in Sources */,
				BA853F4C2DC658B000A34846 /* RootView.swift in Sources */,
				2152FB042600AC8F00CF470E /* iOSApp.swift in Sources */,
				BA853F4A2DC657D900A34846 /* AppDelegate.swift in Sources */,
				BACA7A892F124F4E003B6963 /* IosFirebaseCrashlytics.swift in Sources */,
				19B4E580717143CBAA477F29 /* IosFirebaseMessaging.swift in Sources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXSourcesBuildPhase section */

/* Begin XCBuildConfiguration section */
		7555FFA3242A565B00829871 /* Debug */ = {
			isa = XCBuildConfiguration;
			baseConfigurationReference = F0054F872EE5B3C300F40B1C /* Config.xcconfig */;
			buildSettings = {
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ANALYZER_NONNULL = YES;
				CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;
				CLANG_CXX_LANGUAGE_STANDARD = "gnu++14";
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				CLANG_ENABLE_OBJC_WEAK = YES;
				CLANG_WARN_BOOL_CONVERSION = YES;
				CLANG_WARN_CONSTANT_CONVERSION = YES;
				CLANG_WARN_DOCUMENTATION_COMMENTS = YES;
				CLANG_WARN_EMPTY_BODY = YES;
				CLANG_WARN_ENUM_CONVERSION = YES;
				CLANG_WARN_INFINITE_RECURSION = YES;
				CLANG_WARN_INT_CONVERSION = YES;
				CLANG_WARN_UNREACHABLE_CODE = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				ENABLE_TESTABILITY = YES;
				ENABLE_USER_SCRIPT_SANDBOXING = NO;
				GCC_C_LANGUAGE_STANDARD = gnu11;
				GCC_DYNAMIC_NO_PIC = NO;
				GCC_NO_COMMON_BLOCKS = YES;
				GCC_OPTIMIZATION_LEVEL = 0;
				GCC_PREPROCESSOR_DEFINITIONS = (
					"DEBUG=1",
					"$(inherited)",
				);
				GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
				GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
				GCC_WARN_UNUSED_FUNCTION = YES;
				GCC_WARN_UNUSED_VARIABLE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				MTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;
				MTL_FAST_MATH = YES;
				ONLY_ACTIVE_ARCH = YES;
				SDKROOT = iphoneos;
				SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG;
				SWIFT_OPTIMIZATION_LEVEL = "-Onone";
			};
			name = Debug;
		};
		7555FFA4242A565B00829871 /* Release */ = {
			isa = XCBuildConfiguration;
			baseConfigurationReference = F0054F872EE5B3C300F40B1C /* Config.xcconfig */;
			buildSettings = {
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ANALYZER_NONNULL = YES;
				CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;
				CLANG_CXX_LANGUAGE_STANDARD = "gnu++14";
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				CLANG_ENABLE_OBJC_WEAK = YES;
				CLANG_WARN_BOOL_CONVERSION = YES;
				CLANG_WARN_CONSTANT_CONVERSION = YES;
				CLANG_WARN_DOCUMENTATION_COMMENTS = YES;
				CLANG_WARN_EMPTY_BODY = YES;
				CLANG_WARN_ENUM_CONVERSION = YES;
				CLANG_WARN_INFINITE_RECURSION = YES;
				CLANG_WARN_INT_CONVERSION = YES;
				CLANG_WARN_UNREACHABLE_CODE = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
				ENABLE_NS_ASSERTIONS = NO;
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				ENABLE_USER_SCRIPT_SANDBOXING = NO;
				GCC_C_LANGUAGE_STANDARD = gnu11;
				GCC_NO_COMMON_BLOCKS = YES;
				GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
				GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
				GCC_WARN_UNUSED_FUNCTION = YES;
				GCC_WARN_UNUSED_VARIABLE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				MTL_ENABLE_DEBUG_INFO = NO;
				MTL_FAST_MATH = YES;
				SDKROOT = iphoneos;
				SWIFT_COMPILATION_MODE = wholemodule;
				SWIFT_OPTIMIZATION_LEVEL = "-O";
				VALIDATE_PRODUCT = YES;
			};
			name = Release;
		};
		7555FFA6242A565B00829871 /* Debug */ = {
			isa = XCBuildConfiguration;
			baseConfigurationReference = F0054F872EE5B3C300F40B1C /* Config.xcconfig */;
			buildSettings = {
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				CODE_SIGN_STYLE = Automatic;
				DEVELOPMENT_ASSET_PATHS = "\"iosApp/Preview Content\"";
				DEVELOPMENT_TEAM = "<TEAM_ID>";
				ENABLE_PREVIEWS = YES;
				FRAMEWORK_SEARCH_PATHS = (
					"$(inherited)",
					"$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
				);
				INFOPLIST_FILE = iosApp/Info.plist;
				INFOPLIST_KEY_CFBundleDisplayName = "<Product>";
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				OTHER_LDFLAGS = (
					"$(inherited)",
					"-lsqlite3",
				);
				PRODUCT_BUNDLE_IDENTIFIER = "<bundleId>";
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
			};
			name = Debug;
		};
		7555FFA7242A565B00829871 /* Release */ = {
			isa = XCBuildConfiguration;
			baseConfigurationReference = F0054F872EE5B3C300F40B1C /* Config.xcconfig */;
			buildSettings = {
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				CODE_SIGN_STYLE = Automatic;
				DEVELOPMENT_ASSET_PATHS = "\"iosApp/Preview Content\"";
				DEVELOPMENT_TEAM = "<TEAM_ID>";
				ENABLE_PREVIEWS = YES;
				FRAMEWORK_SEARCH_PATHS = (
					"$(inherited)",
					"$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
				);
				INFOPLIST_FILE = iosApp/Info.plist;
				INFOPLIST_KEY_CFBundleDisplayName = "<Product>";
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				OTHER_LDFLAGS = (
					"$(inherited)",
					"-lsqlite3",
				);
				PRODUCT_BUNDLE_IDENTIFIER = "<bundleId>";
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
			};
			name = Release;
		};
/* End XCBuildConfiguration section */

/* Begin XCConfigurationList section */
		7555FF76242A565900829871 /* Build configuration list for PBXProject "iosApp" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				7555FFA3242A565B00829871 /* Debug */,
				7555FFA4242A565B00829871 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
		7555FFA5242A565B00829871 /* Build configuration list for PBXNativeTarget "iosApp" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				7555FFA6242A565B00829871 /* Debug */,
				7555FFA7242A565B00829871 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
/* End XCConfigurationList section */

/* Begin XCRemoteSwiftPackageReference section */
		BA393ADB2F11A8AF00DE3DF3 /* XCRemoteSwiftPackageReference "firebase-ios-sdk" */ = {
			isa = XCRemoteSwiftPackageReference;
			repositoryURL = "https://github.com/firebase/firebase-ios-sdk";
			requirement = {
				kind = upToNextMajorVersion;
				minimumVersion = 12.7.0;
			};
		};
/* End XCRemoteSwiftPackageReference section */

/* Begin XCSwiftPackageProductDependency section */
		7D30A510752A46AD81BF482F /* FirebaseMessaging */ = {
			isa = XCSwiftPackageProductDependency;
			package = BA393ADB2F11A8AF00DE3DF3 /* XCRemoteSwiftPackageReference "firebase-ios-sdk" */;
			productName = FirebaseMessaging;
		};
		BA393ADC2F11A8AF00DE3DF3 /* FirebaseAnalytics */ = {
			isa = XCSwiftPackageProductDependency;
			package = BA393ADB2F11A8AF00DE3DF3 /* XCRemoteSwiftPackageReference "firebase-ios-sdk" */;
			productName = FirebaseAnalytics;
		};
		BA393ADE2F11A8AF00DE3DF3 /* FirebaseAnalyticsCore */ = {
			isa = XCSwiftPackageProductDependency;
			package = BA393ADB2F11A8AF00DE3DF3 /* XCRemoteSwiftPackageReference "firebase-ios-sdk" */;
			productName = FirebaseAnalyticsCore;
		};
		BA393AE02F11A8AF00DE3DF3 /* FirebaseCore */ = {
			isa = XCSwiftPackageProductDependency;
			package = BA393ADB2F11A8AF00DE3DF3 /* XCRemoteSwiftPackageReference "firebase-ios-sdk" */;
			productName = FirebaseCore;
		};
		BA393AE22F11A8AF00DE3DF3 /* FirebaseCrashlytics */ = {
			isa = XCSwiftPackageProductDependency;
			package = BA393ADB2F11A8AF00DE3DF3 /* XCRemoteSwiftPackageReference "firebase-ios-sdk" */;
			productName = FirebaseCrashlytics;
		};
/* End XCSwiftPackageProductDependency section */
	};
	rootObject = 7555FF73242A565900829871 /* Project object */;
}
```

### project.pbxproj (firebaseEnabled: false)

Use **instead of** the variant above when `firebaseEnabled: false`.

_Target path:_ `iosApp/iosApp.xcodeproj/project.pbxproj`

```text
// !$*UTF8*$!
{
	archiveVersion = 1;
	classes = {
	};
	objectVersion = 54;
	objects = {

/* Begin PBXBuildFile section */
		058557BB273AAA24004C7B11 /* Assets.xcassets in Resources */ = {isa = PBXBuildFile; fileRef = 058557BA273AAA24004C7B11 /* Assets.xcassets */; };
		058557D9273AAEEB004C7B11 /* Preview Assets.xcassets in Resources */ = {isa = PBXBuildFile; fileRef = 058557D8273AAEEB004C7B11 /* Preview Assets.xcassets */; };
		2152FB042600AC8F00CF470E /* iOSApp.swift in Sources */ = {isa = PBXBuildFile; fileRef = 2152FB032600AC8F00CF470E /* iOSApp.swift */; };
		BA853F4A2DC657D900A34846 /* AppDelegate.swift in Sources */ = {isa = PBXBuildFile; fileRef = BA853F492DC657D300A34846 /* AppDelegate.swift */; };
		BA853F4C2DC658B000A34846 /* RootView.swift in Sources */ = {isa = PBXBuildFile; fileRef = BA853F4B2DC658AF00A34846 /* RootView.swift */; };
		D3D0B9802FA23C2C00AA1234 /* libsqlite3.tbd in Frameworks */ = {isa = PBXBuildFile; fileRef = D3D0B97F2FA23C2C00AA1234 /* libsqlite3.tbd */; };
/* End PBXBuildFile section */

/* Begin PBXFileReference section */
		058557BA273AAA24004C7B11 /* Assets.xcassets */ = {isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = Assets.xcassets; sourceTree = "<group>"; };
		058557D8273AAEEB004C7B11 /* Preview Assets.xcassets */ = {isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; path = "Preview Assets.xcassets"; sourceTree = "<group>"; };
		2152FB032600AC8F00CF470E /* iOSApp.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = iOSApp.swift; sourceTree = "<group>"; };
		7555FF7B242A565900829871 /* <Product>.app */ = {isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = "<Product>.app"; sourceTree = BUILT_PRODUCTS_DIR; };
		7555FF8C242A565B00829871 /* Info.plist */ = {isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = "<group>"; };
		BA853F492DC657D300A34846 /* AppDelegate.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = AppDelegate.swift; sourceTree = "<group>"; };
		BA853F4B2DC658AF00A34846 /* RootView.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = RootView.swift; sourceTree = "<group>"; };
		D3D0B97F2FA23C2C00AA1234 /* libsqlite3.tbd */ = {isa = PBXFileReference; lastKnownFileType = "sourcecode.text-based-dylib-definition"; name = libsqlite3.tbd; path = usr/lib/libsqlite3.tbd; sourceTree = SDKROOT; };
		F0054F872EE5B3C300F40B1C /* Config.xcconfig */ = {isa = PBXFileReference; lastKnownFileType = text.xcconfig; path = Config.xcconfig; sourceTree = "<group>"; };
/* End PBXFileReference section */

/* Begin PBXFrameworksBuildPhase section */
		22C4659C9711D113BD9C1DBA /* Frameworks */ = {
			isa = PBXFrameworksBuildPhase;
			buildActionMask = 2147483647;
			files = (
				D3D0B9802FA23C2C00AA1234 /* libsqlite3.tbd in Frameworks */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXFrameworksBuildPhase section */

/* Begin PBXGroup section */
		058557D7273AAEEB004C7B11 /* Preview Content */ = {
			isa = PBXGroup;
			children = (
				058557D8273AAEEB004C7B11 /* Preview Assets.xcassets */,
			);
			path = "Preview Content";
			sourceTree = "<group>";
		};
		7555FF72242A565900829871 = {
			isa = PBXGroup;
			children = (
				7555FF7D242A565900829871 /* iosApp */,
				F0054F862EE5B3C300F40B1C /* Configuration */,
				7555FF7C242A565900829871 /* Products */,
				D3D0B9812FA23C2C00AA1234 /* Frameworks */,
			);
			sourceTree = "<group>";
		};
		7555FF7C242A565900829871 /* Products */ = {
			isa = PBXGroup;
			children = (
				7555FF7B242A565900829871 /* <Product>.app */,
			);
			name = Products;
			sourceTree = "<group>";
		};
		7555FF7D242A565900829871 /* iosApp */ = {
			isa = PBXGroup;
			children = (
				BA853F4B2DC658AF00A34846 /* RootView.swift */,
				BA853F492DC657D300A34846 /* AppDelegate.swift */,
				058557BA273AAA24004C7B11 /* Assets.xcassets */,
				7555FF8C242A565B00829871 /* Info.plist */,
				2152FB032600AC8F00CF470E /* iOSApp.swift */,
				058557D7273AAEEB004C7B11 /* Preview Content */,
			);
			path = iosApp;
			sourceTree = "<group>";
		};
		D3D0B9812FA23C2C00AA1234 /* Frameworks */ = {
			isa = PBXGroup;
			children = (
				D3D0B97F2FA23C2C00AA1234 /* libsqlite3.tbd */,
			);
			name = Frameworks;
			sourceTree = "<group>";
		};
		F0054F862EE5B3C300F40B1C /* Configuration */ = {
			isa = PBXGroup;
			children = (
				F0054F872EE5B3C300F40B1C /* Config.xcconfig */,
			);
			path = Configuration;
			sourceTree = "<group>";
		};
/* End PBXGroup section */

/* Begin PBXNativeTarget section */
		7555FF7A242A565900829871 /* iosApp */ = {
			isa = PBXNativeTarget;
			buildConfigurationList = 7555FFA5242A565B00829871 /* Build configuration list for PBXNativeTarget "iosApp" */;
			buildPhases = (
				BB1A2B3C4D5E6F0011223344 /* Compile Kotlin Framework */,
				7555FF77242A565900829871 /* Sources */,
				7555FF79242A565900829871 /* Resources */,
				22C4659C9711D113BD9C1DBA /* Frameworks */,
			);
			buildRules = (
			);
			dependencies = (
			);
			name = iosApp;
			productName = iosApp;
			productReference = 7555FF7B242A565900829871 /* <Product>.app */;
			productType = "com.apple.product-type.application";
		};
/* End PBXNativeTarget section */

/* Begin PBXProject section */
		7555FF73242A565900829871 /* Project object */ = {
			isa = PBXProject;
			attributes = {
				BuildIndependentTargetsInParallel = YES;
				LastSwiftUpdateCheck = 1130;
				LastUpgradeCheck = 2610;
				ORGANIZATIONNAME = "<org>";
				TargetAttributes = {
					7555FF7A242A565900829871 = {
						CreatedOnToolsVersion = 11.3.1;
					};
				};
			};
			buildConfigurationList = 7555FF76242A565900829871 /* Build configuration list for PBXProject "iosApp" */;
			compatibilityVersion = "Xcode 9.3";
			developmentRegion = en;
			hasScannedForEncodings = 0;
			knownRegions = (
				en,
				Base,
			);
			mainGroup = 7555FF72242A565900829871;
			productRefGroup = 7555FF7C242A565900829871 /* Products */;
			projectDirPath = "";
			projectRoot = "";
			targets = (
				7555FF7A242A565900829871 /* iosApp */,
			);
		};
/* End PBXProject section */

/* Begin PBXResourcesBuildPhase section */
		7555FF79242A565900829871 /* Resources */ = {
			isa = PBXResourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				058557D9273AAEEB004C7B11 /* Preview Assets.xcassets in Resources */,
				058557BB273AAA24004C7B11 /* Assets.xcassets in Resources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXResourcesBuildPhase section */

/* Begin PBXShellScriptBuildPhase section */
		BB1A2B3C4D5E6F0011223344 /* Compile Kotlin Framework */ = {
			isa = PBXShellScriptBuildPhase;
			alwaysOutOfDate = 1;
			buildActionMask = 2147483647;
			files = (
			);
			inputFileListPaths = (
			);
			inputPaths = (
			);
			name = "Compile Kotlin Framework";
			outputFileListPaths = (
			);
			outputPaths = (
			);
			runOnlyForDeploymentPostprocessing = 0;
			shellPath = /bin/sh;
			shellScript = "# Builds, copies and signs the Kotlin/Native framework for the current Xcode\n# configuration/SDK/arch. Output lands in <fw>/build/xcode-frameworks/$CONFIGURATION/$SDK_NAME,\n# which FRAMEWORK_SEARCH_PATHS points to. The framework module map autolinks it.\nif [ \"YES\" = \"$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED\" ]; then\n  echo \"Skipping Gradle build (OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES)\"\n  exit 0\nfi\ncd \"$SRCROOT/..\"\n./gradlew :shared:embedAndSignAppleFrameworkForXcode\n";
		};
/* End PBXShellScriptBuildPhase section */

/* Begin PBXSourcesBuildPhase section */
		7555FF77242A565900829871 /* Sources */ = {
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				BA853F4C2DC658B000A34846 /* RootView.swift in Sources */,
				2152FB042600AC8F00CF470E /* iOSApp.swift in Sources */,
				BA853F4A2DC657D900A34846 /* AppDelegate.swift in Sources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		};
/* End PBXSourcesBuildPhase section */

/* Begin XCBuildConfiguration section */
		7555FFA3242A565B00829871 /* Debug */ = {
			isa = XCBuildConfiguration;
			baseConfigurationReference = F0054F872EE5B3C300F40B1C /* Config.xcconfig */;
			buildSettings = {
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ANALYZER_NONNULL = YES;
				CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;
				CLANG_CXX_LANGUAGE_STANDARD = "gnu++14";
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				CLANG_ENABLE_OBJC_WEAK = YES;
				CLANG_WARN_BOOL_CONVERSION = YES;
				CLANG_WARN_CONSTANT_CONVERSION = YES;
				CLANG_WARN_EMPTY_BODY = YES;
				CLANG_WARN_ENUM_CONVERSION = YES;
				CLANG_WARN_INFINITE_RECURSION = YES;
				CLANG_WARN_INT_CONVERSION = YES;
				CLANG_WARN_UNREACHABLE_CODE = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				ENABLE_TESTABILITY = YES;
				ENABLE_USER_SCRIPT_SANDBOXING = NO;
				GCC_C_LANGUAGE_STANDARD = gnu11;
				GCC_DYNAMIC_NO_PIC = NO;
				GCC_NO_COMMON_BLOCKS = YES;
				GCC_OPTIMIZATION_LEVEL = 0;
				GCC_PREPROCESSOR_DEFINITIONS = (
					"DEBUG=1",
					"$(inherited)",
				);
				GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
				GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
				GCC_WARN_UNUSED_FUNCTION = YES;
				GCC_WARN_UNUSED_VARIABLE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				MTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;
				MTL_FAST_MATH = YES;
				ONLY_ACTIVE_ARCH = YES;
				SDKROOT = iphoneos;
				SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG;
				SWIFT_OPTIMIZATION_LEVEL = "-Onone";
			};
			name = Debug;
		};
		7555FFA4242A565B00829871 /* Release */ = {
			isa = XCBuildConfiguration;
			baseConfigurationReference = F0054F872EE5B3C300F40B1C /* Config.xcconfig */;
			buildSettings = {
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ANALYZER_NONNULL = YES;
				CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;
				CLANG_CXX_LANGUAGE_STANDARD = "gnu++14";
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				CLANG_ENABLE_OBJC_WEAK = YES;
				CLANG_WARN_BOOL_CONVERSION = YES;
				CLANG_WARN_CONSTANT_CONVERSION = YES;
				CLANG_WARN_EMPTY_BODY = YES;
				CLANG_WARN_ENUM_CONVERSION = YES;
				CLANG_WARN_INFINITE_RECURSION = YES;
				CLANG_WARN_INT_CONVERSION = YES;
				CLANG_WARN_UNREACHABLE_CODE = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
				ENABLE_NS_ASSERTIONS = NO;
				ENABLE_STRICT_OBJC_MSGSEND = YES;
				ENABLE_USER_SCRIPT_SANDBOXING = NO;
				GCC_C_LANGUAGE_STANDARD = gnu11;
				GCC_NO_COMMON_BLOCKS = YES;
				GCC_WARN_ABOUT_RETURN_TYPE = YES_ERROR;
				GCC_WARN_UNINITIALIZED_AUTOS = YES_AGGRESSIVE;
				GCC_WARN_UNUSED_FUNCTION = YES;
				GCC_WARN_UNUSED_VARIABLE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				MTL_ENABLE_DEBUG_INFO = NO;
				MTL_FAST_MATH = YES;
				SDKROOT = iphoneos;
				SWIFT_COMPILATION_MODE = wholemodule;
				SWIFT_OPTIMIZATION_LEVEL = "-O";
				VALIDATE_PRODUCT = YES;
			};
			name = Release;
		};
		7555FFA6242A565B00829871 /* Debug */ = {
			isa = XCBuildConfiguration;
			baseConfigurationReference = F0054F872EE5B3C300F40B1C /* Config.xcconfig */;
			buildSettings = {
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				CODE_SIGN_STYLE = Automatic;
				DEVELOPMENT_ASSET_PATHS = "\"iosApp/Preview Content\"";
				DEVELOPMENT_TEAM = "<TEAM_ID>";
				ENABLE_PREVIEWS = YES;
				FRAMEWORK_SEARCH_PATHS = (
					"$(inherited)",
					"$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
				);
				INFOPLIST_FILE = iosApp/Info.plist;
				INFOPLIST_KEY_CFBundleDisplayName = "<Product>";
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				OTHER_LDFLAGS = (
					"$(inherited)",
					"-lsqlite3",
				);
				PRODUCT_BUNDLE_IDENTIFIER = "<bundleId>";
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
			};
			name = Debug;
		};
		7555FFA7242A565B00829871 /* Release */ = {
			isa = XCBuildConfiguration;
			baseConfigurationReference = F0054F872EE5B3C300F40B1C /* Config.xcconfig */;
			buildSettings = {
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				CODE_SIGN_STYLE = Automatic;
				DEVELOPMENT_ASSET_PATHS = "\"iosApp/Preview Content\"";
				DEVELOPMENT_TEAM = "<TEAM_ID>";
				ENABLE_PREVIEWS = YES;
				FRAMEWORK_SEARCH_PATHS = (
					"$(inherited)",
					"$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
				);
				INFOPLIST_FILE = iosApp/Info.plist;
				INFOPLIST_KEY_CFBundleDisplayName = "<Product>";
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				OTHER_LDFLAGS = (
					"$(inherited)",
					"-lsqlite3",
				);
				PRODUCT_BUNDLE_IDENTIFIER = "<bundleId>";
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = "1,2";
			};
			name = Release;
		};
/* End XCBuildConfiguration section */

/* Begin XCConfigurationList section */
		7555FF76242A565900829871 /* Build configuration list for PBXProject "iosApp" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				7555FFA3242A565B00829871 /* Debug */,
				7555FFA4242A565B00829871 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
		7555FFA5242A565B00829871 /* Build configuration list for PBXNativeTarget "iosApp" */ = {
			isa = XCConfigurationList;
			buildConfigurations = (
				7555FFA6242A565B00829871 /* Debug */,
				7555FFA7242A565B00829871 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		};
/* End XCConfigurationList section */
	};
	rootObject = 7555FF73242A565900829871 /* Project object */;
}
```
