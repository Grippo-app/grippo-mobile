# `:toolkit:permission-manager` — System Permissions

Request and check system permissions (notifications, camera, microphone, etc.) through a unified API.

## API

```kotlin
public interface PermissionManager {
    public suspend fun check(permission: AppPermission): PermissionStatus
    public suspend fun request(permission: AppPermission): PermissionStatus
}

public enum class AppPermission {
    Notifications,
    // Add new entries here as features need them.
}

public sealed class PermissionStatus {
    public data object Granted : PermissionStatus()
    public data object Denied : PermissionStatus()
    public data object DeniedPermanently : PermissionStatus()
}
```

- `check(...)` returns the current status without showing the system dialog.
- `request(...)` shows the dialog when status is `Denied`; for `Granted` and `DeniedPermanently` it short-circuits and returns the current status.
- The reference enum has only `Notifications`. Add `Camera`/`Microphone`/`Location` etc. when a feature actually needs them — adding now means each new entry needs Android + iOS impls.

## Usage

```kotlin
internal class WeightHistoryViewModel(
    private val permissionManager: PermissionManager,
    private val notificationManager: NotificationManager,
) : BaseViewModel<...>(...) {

    override fun onEnableRemindersClick() {
        safeLaunch {
            when (permissionManager.request(AppPermission.Notifications)) {
                PermissionStatus.Granted -> {
                    update { it.copy(remindersEnabled = true) }
                    scheduleReminder()
                }
                PermissionStatus.Denied -> {
                    // show inline message; user may try again
                }
                PermissionStatus.DeniedPermanently -> {
                    dialogController.show(DialogConfig.OpenSettingsPrompt)
                }
            }
        }
    }
}
```

Request permission **at the moment the user wants the feature**, not at app start. Boot-time permission requests cause permission fatigue.

## `AppPermission.Notifications` Android specifics

- **API 33+ (Android 13)**: `POST_NOTIFICATIONS` is a runtime permission. Request via this interface.
- **API < 33**: notifications work without runtime permission (auto-granted). `check(AppPermission.Notifications)` returns `Granted`.

iOS: `UNUserNotificationCenter.requestAuthorization(...)`. The permission persists across app launches.

## Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.permission.manager" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.toolkit.context)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)   // ActivityResultContracts.RequestMultiplePermissions
        }
    }
}
```

## Implementation outlines

### Android

```kotlin
// androidMain
internal class AndroidPermissionManager(
    private val context: Context,
) : PermissionManager {
    override suspend fun check(permission: AppPermission): PermissionStatus { /* checkSelfPermission */ }
    override suspend fun request(permission: AppPermission): PermissionStatus { /* launches PermissionRequestActivity, awaits result via PermissionResultHolder */ }
}
```

The Android impl launches an internal `PermissionRequestActivity` to host the system prompt (the call site doesn't need to be an Activity) and uses a `PermissionResultHolder` to bridge the result back into the coroutine. `DeniedPermanently` is detected by `!shouldShowRequestPermissionRationale(...)` after the user denies.

### iOS

```kotlin
// iosMain
internal class IosPermissionManager : PermissionManager {
    override suspend fun check(permission: AppPermission): PermissionStatus = when (permission) {
        AppPermission.Notifications -> currentNotificationStatus()
    }
    override suspend fun request(permission: AppPermission): PermissionStatus = when (permission) {
        AppPermission.Notifications -> requestNotificationAuthorization()
    }
    // requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionBadge) { granted, _ -> ... }
}
```

## Rules

- **Request at user-intent moments**, never at app start.
- **`check(...)` is non-blocking** — it queries the current status without showing UI.
- **`request(...)` is suspendable** — it shows the system dialog (when applicable) and awaits the user's response.
- **`DeniedPermanently` triggers a "open Settings" prompt** — link via `:toolkit:link-opener`.

## Anti-patterns

- **Requesting all permissions at app start.** Causes immediate uninstalls.
- **`request(...)` without a user-visible reason.** Always pair with a UI message explaining why.
- **Ignoring `DeniedPermanently`.** The user has dismissed the prompt twice; show a Settings deeplink or graceful fallback.
- **Caching `request(...)` results forever.** The user may revoke in Settings; re-check via `check(...)`.
- **Coupling `PermissionManager` to specific features.** Keep it generic; features call it.
