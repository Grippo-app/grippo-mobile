# Google Auth Service

Multiplatform wrapper around Google Sign-In that hides the platform-specific UI flows and exposes a
single API surface.

## Table of contents
1. [Overview](#overview)
2. [Integration at a glance](#integration-at-a-glance)
3. [Google Cloud setup](#google-cloud-setup)
4. [Android configuration](#android-configuration)
5. [iOS configuration](#ios-configuration)
6. [Compose usage](#compose-usage)
7. [Troubleshooting](#troubleshooting)

---

## Overview

The module provides everything required to start and consume the Google Sign-In UI flow:

- `GoogleAuthProvider` – injected via Koin; turns the platform UI result into a `GoogleAccount`.
- `GoogleAuthUiProvider` – the bridge implementation used internally by `GoogleAuthProvider`.
- `rememberGoogleAuthUiContext()` – Compose helper that prepares the UI context for the sign-in
  sheet.

`GoogleAccount` contains the ID token and optional profile data (display name + avatar URL).
`GoogleAuthUiProvider.signIn()` returns `GoogleAccount` and throws on failure. User cancellation
retains `GoogleAuthException.Cancelled`; coroutine cancellation and fatal errors propagate unchanged.
SDK classification retains the original Kotlin exception as its cause; native errors retain
their available domain, code and description.
Check `googleAuthProvider.isSupported` before rendering a
"Continue with Google" button – the provider reports `false` when the platform is missing the
required identifiers.

When configured, the UI layer simply calls `googleAuthProvider.getUiProvider(context).signIn()` and
uses the existing `safeLaunch` error pipeline.

---

## Integration at a glance

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataServices.googleAuth)
    }
}

modules(
    // other modules …
    GoogleAuthModule().module,
)
```

- `GoogleAuthModule` already bundles the required context/http/serialization modules – no extra DI
  bindings are needed.
- On logout call `GoogleAuthProvider.signOut()` (Android clears Credential Manager state, iOS clears
  Google cookies).
- Ensure the Android manifest placeholder and iOS Info.plist entries from the sections below are in
  place before presenting the action to users.

---

## Google Cloud setup

Set up the OAuth consent screen and client IDs once per project – Android and iOS share the same
Google Cloud project.

### 1. Prepare the consent screen

1. Go to **APIs & Services ▸ OAuth consent screen**.
2. Create an Internal/External consent screen, add the `email`, `profile`, and `openid` scopes, and
   list every package/bundle ID that will request tokens.
3. Publish the consent screen; unverified apps get throttled by Google Identity Services.
4. If you keep the screen in testing, list every tester email in the **Test users** tab so Google
   lets them finish the flow.

### 2. Create the OAuth client IDs

In **APIs & Services ▸ Credentials** create:

1. **Web application** – the "server" client ID. Android requests ID tokens for this audience;
   provide it to the Android app and the backend.
2. **Android application** – enter the package name and SHA-1 fingerprints (debug + release). Google
   Identity Services uses this to issue tokens to your APKs.
3. **iOS application** – enter the bundle identifier. The console shows both the client ID and a
   `REVERSED_CLIENT_ID` string – both go into Info.plist.

Re-download the credentials whenever package/bundle IDs or signing keys change.

### 3. Know where each client ID is used

| Usage | OAuth client type | Where to paste |
| --- | --- | --- |
| Requesting ID tokens on Android | Web application | `GOOGLE_SERVER_CLIENT_ID` manifest placeholder |
| Requesting ID tokens on iOS | iOS application | `GIDClientID` and redirect scheme in `Info.plist` |
| Backend token verification | Web application for Android; iOS application for iOS | Backend's allowed Google audiences |

The iOS provider uses `ASWebAuthenticationSession` with PKCE S256 and exchanges the authorization code
using `GIDClientID`. Its ID token audience is the iOS client ID. Configure the backend to validate
the Web client ID for Android tokens and the iOS client ID for iOS tokens.

---

## Android configuration

### 1. Provide the server client ID (app module)

The library manifest declares `<meta-data android:name="com.grippo.google.SERVER_CLIENT_ID"/>` and
expects the **app** module to supply the placeholder:

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["GOOGLE_SERVER_CLIENT_ID"] =
            "<web-client-id>.apps.googleusercontent.com"
    }
}
```

Use your own OAuth server client ID, or source it from `local.properties`/CI secrets. The library
consumes the placeholder during manifest merge – no further manifest edits are needed.

### 2. Optional sign-out

Call `GoogleAuthProvider.signOut()` when the user logs out. This clears Credential Manager state so
subsequent launches present the Google account picker again.

---

## iOS configuration

Add the Google identifiers to `Info.plist`:

```xml
<key>GIDClientID</key>
<string>1234567890-abcdefg1234567890abcdefg123456.apps.googleusercontent.com</string>
<key>GIDRedirectURI</key>
<string>com.googleusercontent.apps.1234567890-abcdefg1234567890abcdefg123456:/oauthredirect</string>
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.googleusercontent.apps.1234567890-abcdefg1234567890abcdefg123456</string>
        </array>
    </dict>
</array>
```

- `GIDClientID` – iOS application client ID from the Google Cloud console.
- `GIDRedirectURI` and URL scheme – must match the iOS client ID (`REVERSED_CLIENT_ID`).
- Sign-out is the same as Android: call `GoogleAuthProvider.signOut()` to remove stored cookies so
  the sheet shows the picker again.

---

## Compose usage

```kotlin
val googleUiContext = rememberGoogleAuthUiContext()
val canStartGoogleSignIn =
    googleUiContext != null && googleAuthProvider.isSupported && !isGoogleFlowRunning

FilledTonalButton(
    enabled = canStartGoogleSignIn,
    onClick = { googleUiContext?.let(onGoogleClick) },
) {
    Text("Continue with Google")
}
```

`onGoogleClick` should accept a `GoogleAuthUiContext` and delegate to the state holder or ViewModel
running your authentication flow.

The LoginViewModel's provider operation stays inside its existing `safeLaunch` and analytics block:

```kotlin
safeLaunch(
    loader = LoginLoader.LoginByGoogleButton,
    mapError = ::mapAuthError,
) {
    analytics.trackEvent(AuthEvent.Login.Submit(method = Method.Google))

    val hasProfile = analytics.trackOperation(AuthOperation.Login(method = Method.Google)) {
        val account = googleAuthProvider.getUiProvider(context).signIn()
        loginUseCase.loginGoogle(token = account.token)
    }

    navigateAfterLogin(hasProfile)
}
```

---

## Troubleshooting

| Problem | Fix |
| --- | --- |
| `Unknown manifest placeholder GOOGLE_SERVER_CLIENT_ID` | Ensure the application module defines the placeholder (see [Android configuration](#android-configuration)). |
| `Google server client id is missing` exception | Usually indicates the meta-data value resolved to blank. Double-check the placeholder value and the merged manifest path that Gradle prints. |
| iOS sign-in window does not return | Confirm the redirect URI and URL scheme match the `GIDClientID`. |
| `googleAuthProvider.isSupported == false` | Platform configuration is missing (manifest placeholder on Android, Info.plist keys on iOS). Repeat the relevant setup steps above. |
