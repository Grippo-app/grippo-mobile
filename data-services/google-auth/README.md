# Google Auth Service

Multiplatform wrapper around Google Sign-In. The module exposes:

- `GoogleAuthProvider` – injected via Koin, knows how to start the platform UI flow and convert its
  result into a `GoogleAccount`.
- `GoogleAuthUiProvider` – the platform bridge used by `GoogleAuthProvider`.
- `rememberGoogleAuthUiContext()` – Compose helper that provides the UI context required when
  launching the Google sign-in sheet.

`GoogleAccount` gives you the ID token together with optional profile data (display name and avatar
URL). `GoogleAuthUiProvider.signIn()` returns `Result<GoogleAccount>` so UI code can distinguish
between genuine failures and user cancellations. Call `googleAuthProvider.isSupported` before
showing a "Continue with Google" action – the provider reports `false` if the required client IDs
are missing on the current platform.

Once included, UI code can simply call `googleAuthProvider.getUiProvider(context).signIn()` and act
on the `Result`.

---

## 1. Dependency & DI

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

`GoogleAuthModule` already includes the required context/http/serialization modules, so nothing else
is needed.

---

## 2. Google Cloud console

Set up the OAuth consent screen and client IDs once per project – both the Android and iOS targets
use the same Google Cloud project.

### 2.1 Prepare the consent screen

1. Go to **APIs & Services ▸ OAuth consent screen**.
2. Create an Internal/External consent screen, add the `email`, `profile`, and `openid` scopes, and
   list every package/bundle ID that will request tokens.
3. Publish the consent screen; unverified apps are throttled by Google Identity Services.

### 2.2 Create the OAuth client IDs

In **APIs & Services ▸ Credentials** create the following OAuth client IDs:

1. **Web application** – this is the "server" client ID. Use it for backend token verification and
   provide the value to the mobile apps (see Section 3.1 and Info.plist below).
2. **Android application** – enter the package name and the SHA-1 fingerprints of every signing key
   (debug + release). This client authorises Google Identity Services to issue tokens to your APKs.
3. **iOS application** – enter the bundle identifier. The console presents both the client ID and a
   `REVERSED_CLIENT_ID` string – you will use both in Info.plist.

Re-download the credentials whenever you rotate signing keys or bundle IDs.

### 2.3 Where each client ID is used

| Usage | OAuth client type | Where to paste |
| --- | --- | --- |
| Requesting ID tokens on Android | Web application | `GOOGLE_SERVER_CLIENT_ID` manifest placeholder (Section 3.1) |
| Requesting ID tokens on iOS | iOS application | `GIDClientID` (`Info.plist`) and redirect scheme |
| Backend token verification / exchanging auth codes | Web application | `GIDServerClientID` (`Info.plist`) and your backend configuration |

Share the Web client ID with the backend that validates `sub`/`aud` claims – Google will set the
`aud` field of the ID token to this value.

---

## 3. Android setup

### 3.1 Server client ID placeholder (app module)
The library manifest declares `<meta-data android:name="com.grippo.google.SERVER_CLIENT_ID"/>` and
expects the **app** module to provide the value. Add the placeholder in the application project:

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["GOOGLE_SERVER_CLIENT_ID"] =
            "<web-client-id>.apps.googleusercontent.com"
    }
}
```

Use your own OAuth server client ID, or source it from `local.properties`/CI secrets. No additional
manifest edits are required – the library will pick up the placeholder during merge.

### 3.2 Optional sign-out (Android & iOS)
Call `GoogleAuthProvider.signOut()` whenever a user logs out. On Android this clears the Credential
Manager state; on iOS it removes Google cookies so the web sheet will offer the account picker
again.

---

## 4. iOS setup

Add the Google identifiers to `Info.plist`:

```xml
<key>GIDClientID</key>
<string>1234567890-abcdefg1234567890abcdefg123456.apps.googleusercontent.com</string>
<key>GIDServerClientID</key>
<string><web-client-id>.apps.googleusercontent.com</string>
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
- `GIDServerClientID` – Web (server) client ID from Section 2.3.
- `GIDRedirectURI` and URL scheme – must match the iOS client ID (Google console exposes the
  `REVERSED_CLIENT_ID` that already contains this value).

---

## 5. Compose usage example

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

`onGoogleClick` should accept a `GoogleAuthUiContext` and call into whatever state holder or
ViewModel orchestrates your authentication flow.

Example ViewModel function:

```kotlin
suspend fun handleGoogleLogin(context: GoogleAuthUiContext) {
    val result = googleAuthProvider
        .getUiProvider(context)
        .signIn()

    result.onSuccess { account ->
        // Use account.token in your domain layer (exchange for backend session, etc.)
    }.onFailure { error ->
        // Show an error message / report to analytics
    }
}
```

---

## 6. Troubleshooting

| Problem | Fix |
| --- | --- |
| `Unknown manifest placeholder GOOGLE_SERVER_CLIENT_ID` | Ensure the application module defines `manifestPlaceholders` (Section 3.1). |
| `Google server client id is missing` exception | Usually indicates the meta-data value resolved to blank. Check the placeholder and confirm the manifest merge picked it up (Gradle prints merged manifest path). |
| iOS sign-in window does not return | Confirm redirect URI and URL scheme match the `GIDClientID`. |
| `googleAuthProvider.isSupported == false` | Configuration is missing (manifest placeholder on Android, Info.plist keys on iOS). Follow Sections 2 and 3/4. |
