# `:toolkit:notification-manager` — Local Notifications

Schedule and cancel **local** notifications (not push) on Android and iOS.

## API

```kotlin
public interface NotificationManager {
    public fun show(notification: AppNotification, delay: Duration = Duration.ZERO): NotificationKey
    public fun cancel(id: NotificationKey)
    public suspend fun isPending(id: NotificationKey): Boolean
}

public data class AppNotification(
    val id: NotificationKey,
    val title: String,
    val body: String,
    val deeplink: String? = null,
)

public sealed class NotificationKey(public open val key: Int) {
    public data object ChangeWeight : NotificationKey(1)
    public data object FinishWorkout : NotificationKey(2)
    public data class Custom(override val key: Int) : NotificationKey(key)
}
```

Replace `ChangeWeight`/`FinishWorkout` with product-specific notification kinds. The `Custom(Int)` subtype is the escape hatch for dynamic IDs.

## Usage

```kotlin
internal class WeightHistoryViewModel(
    private val notificationManager: NotificationManager,
    private val stringProvider: StringProvider,
) : BaseViewModel<...>(...), ... {

    override fun onSaveWeightClick() {
        safeLaunch(loader = WeightHistoryLoader.SavingWeight) {
            feature.saveWeight(...).getOrThrow()
            scheduleReminder()
        }
    }

    private suspend fun scheduleReminder() {
        val notification = AppNotification(
            id = NotificationKey.ChangeWeight,
            title = stringProvider.get(Res.string.notification_weight_title),
            body = stringProvider.get(Res.string.notification_weight_description),
            deeplink = Deeplink.WeightHistory.key,
        )
        notificationManager.show(notification, 7.days)
    }
}
```

The strings come from `StringProvider` (not `AppTokens.strings` — we're in a VM, not a Composable). The deeplink is a key the OS notification carries; tapping the notification opens the app with this deeplink.

## Deeplink handling

When the user taps a notification:

- **Android**: `MainActivity` is launched with an `Intent` containing the deeplink. `onNewIntent` parses it and calls `rootComponent.applyDeeplink(deeplink)`.
- **iOS**: `UNUserNotificationCenterDelegate.didReceiveResponse` extracts the deeplink and passes it to the Kotlin layer via a bridge.

See `02-module-structure/03-app-shells.md` for the wiring.

## `NotificationKey`

```kotlin
public sealed class NotificationKey(public open val key: Int) {
    public data object ChangeWeight : NotificationKey(1)
    public data object FinishWorkout : NotificationKey(2)
    public data class Custom(override val key: Int) : NotificationKey(key)
}
```

- **`Int key`** — Android `NotificationManager.notify(id, ...)` and iOS `UNNotificationRequest`'s identifier both use integers (iOS uses strings; the wrapper converts).
- **`sealed class` with data objects** for known kinds — type-safe + auto-deduplicated.
- **`Custom(Int)`** for dynamic IDs (e.g. one notification per training reminder).

Showing twice with the same key **replaces** the prior notification:

```kotlin
notificationManager.show(AppNotification(id = NotificationKey.ChangeWeight, ...), 1.days)
notificationManager.show(AppNotification(id = NotificationKey.ChangeWeight, ...), 3.days)
// Only the second is pending; the first is overwritten.
```

## Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.notification.manager" }

    androidLibrary {
        androidResources.enable = true     // notification icons/strings live in androidMain res
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)
    }
}
```

Android implementation uses `AlarmManager` with a `BroadcastReceiver` (`ScheduledNotificationReceiver`) to post the notification at the scheduled time. iOS uses `UNUserNotificationCenter` with `UNTimeIntervalNotificationTrigger`.

## Permissions

- **Android 13+**: `POST_NOTIFICATIONS` runtime permission. Request via `:toolkit:permission-manager`.
- **iOS**: `UNUserNotificationCenter.requestAuthorization(options:)`.

Request permission at a **user-intent moment** (when the user toggles "Remind me to log weight"), not on app start. The `PermissionManager` interface handles this.

## Rules

- **`NotificationKey` values are stable** across app versions. Don't renumber existing keys.
- **`NotificationKey.Custom(id)` IDs must not collide** with sealed constants.
- **Strings come from `StringProvider`** (not `AppTokens.strings` — VM is not `@Composable`).
- **`title` and `body` are plain text.** No HTML, no markdown.
- **`deeplink` is a stable string key** (typically `Deeplink.<X>.key`). Don't ship a notification with a deeplink that points to a route you might rename.

## Anti-patterns

- **Pre-localized title/body without `StringProvider`.** The notification won't update if the user changes the system locale.
- **`NotificationKey.Custom(Long)` style.** The key is `Int` for cross-platform compatibility.
- **Showing a notification synchronously without `safeLaunch`.** If the call blocks (rare), the UI thread stalls.
- **Forgetting to request the notification permission.** On Android 13+ and iOS, notifications are silently dropped without permission.
- **Push notifications via this module.** Push goes through Firebase Messaging (Android) / APNs (iOS) — separate path. `:toolkit:notification-manager` is local-only.
