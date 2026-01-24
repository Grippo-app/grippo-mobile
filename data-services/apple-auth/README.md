# Apple Auth Service

Multiplatform wrapper around Sign in with Apple that hides the platform-specific UI flows and
exposes a single API surface.

## Table of contents

1. [Overview](#overview)
2. [Integration at a glance](#integration-at-a-glance)
3. [Android configuration](#android-configuration)
4. [iOS configuration](#ios-configuration)
5. [Compose usage](#compose-usage)
6. [Troubleshooting](#troubleshooting)

---

## Overview

The module provides everything required to start and consume the Apple Sign-In UI flow:

- `AppleAuthProvider` – injected via Koin; turns the platform UI result into an `AppleAccount`.
- `AppleAuthUiProvider` – the bridge implementation used internally by `AppleAuthProvider`.
- `rememberAppleAuthUiContext()` – Compose helper that prepares the UI context for the sign-in
  sheet.

`AppleAccount` contains the authorization code only. `AppleAuthUiProvider.signIn()` returns
`Result<AppleAccount>` so UI code can differentiate between user cancellations and actual errors.
Check `appleAuthProvider.isSupported` before rendering a "Continue with Apple" button – the
provider reports `false` on Android and `true` on iOS.

When configured, the UI layer simply calls `appleAuthProvider.getUiProvider(context).signIn()` and
handles the `Result`.

---

## Integration at a glance

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataServices.appleAuth)
    }
}

modules(
    // other modules …
    AppleAuthModule().module,
)
```

- `AppleAuthModule` already bundles the required context/http/serialization modules – no extra DI
  bindings are needed.
- On logout call `AppleAuthProvider.signOut()` (no-op at the moment, but it keeps API parity).
- Ensure the iOS capability from the section below is enabled before presenting the action to users.

---

## Android configuration

Sign in with Apple is not supported on Android. The provider always returns
`appleAuthProvider.isSupported == false` and throws if you call `getUiProvider`.

---

## iOS configuration

Enable **Sign in with Apple** in the Apple Developer portal for the target App ID and make sure the
corresponding capability is enabled in Xcode for the iOS target.

Apple Developer (Certificates, Identifiers & Profiles):
- App ID → Capabilities → enable **Sign in with Apple** for your bundle ID.
- Regenerate and download provisioning profiles after enabling the capability.

Xcode:
- Target → Signing & Capabilities → add **Sign in with Apple** capability.
- Ensure the selected team and bundle identifier match the App ID you enabled in the portal.

---

## Compose usage

```kotlin
val appleUiContext = rememberAppleAuthUiContext()
val canStartAppleSignIn =
    appleUiContext != null && appleAuthProvider.isSupported && !isAppleFlowRunning

FilledTonalButton(
    enabled = canStartAppleSignIn,
    onClick = { appleUiContext?.let(onAppleClick) },
) {
    Text("Continue with Apple")
}
```

`onAppleClick` should accept a `AppleAuthUiContext` and delegate to the state holder or ViewModel
running your authentication flow.

Example ViewModel function:

```kotlin
suspend fun handleAppleLogin(context: AppleAuthUiContext) {
    val result = appleAuthProvider
        .getUiProvider(context)
        .signIn()

    result.onSuccess { account ->
        // Use account.authorizationCode in your domain layer.
    }.onFailure { error ->
        // Show an error message / report to analytics
    }
}
```

---

## Troubleshooting

| Problem                                  | Fix                                                                                               |
|------------------------------------------|---------------------------------------------------------------------------------------------------|
| `appleAuthProvider.isSupported == false` | Sign in with Apple is only supported on iOS.                                                      |
| Apple sign-in fails immediately          | Ensure the Sign in with Apple capability is enabled and the device is signed in with an Apple ID. |
