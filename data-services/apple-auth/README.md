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
`AppleAccount` and throws on failure. A user cancellation throws `AppleAuthException` with
`AppleAuthFailureReason.Cancelled`; coroutine cancellation and fatal errors propagate unchanged.
Check `appleAuthProvider.isSupported` before rendering a "Continue with Apple" button – the
provider reports `false` on Android and `true` on iOS.

When configured, the UI layer simply calls `appleAuthProvider.getUiProvider(context).signIn()` and
uses the existing `safeLaunch` error pipeline.

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

- `AppleAuthModule` provides the zero-argument provider; it needs no context, HTTP or JSON bindings.
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

The LoginViewModel's provider operation stays inside its existing `safeLaunch` and analytics block:

```kotlin
safeLaunch(
    loader = LoginLoader.LoginByAppleButton,
    mapError = ::mapAuthError,
) {
    analytics.trackEvent(AuthEvent.Login.Submit(method = Method.Apple))

    val hasProfile = analytics.trackOperation(AuthOperation.Login(method = Method.Apple)) {
        val account = appleAuthProvider.getUiProvider(context).signIn()
        loginUseCase.loginApple(code = account.authorizationCode)
    }

    navigateAfterLogin(hasProfile)
}
```

---

## Troubleshooting

| Problem                                  | Fix                                                                                               |
|------------------------------------------|---------------------------------------------------------------------------------------------------|
| `appleAuthProvider.isSupported == false` | Sign in with Apple is only supported on iOS.                                                      |
| Apple sign-in fails immediately          | Ensure the Sign in with Apple capability is enabled and the device is signed in with an Apple ID. |
