# Google Auth Service

Multiplatform wrapper around Google Sign-In. The module exposes:

- `GoogleAuthProvider` – injected via Koin, knows how to start the platform UI flow and convert its
  result into a `GoogleAccount`.
- `GoogleAuthUiProvider` – the platform bridge used by `GoogleAuthProvider`.
- `rememberGoogleAuthUiContext()` – Compose helper that provides the UI context required when
  launching the Google sign-in sheet.

Once included, UI code can simply call `googleAuthProvider.getUiProvider(context).signIn()` and work
with the returned token.

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

## 2. Android setup

### 2.1 Server client ID placeholder (app module)
The library manifest declares `<meta-data android:name="com.grippo.google.SERVER_CLIENT_ID"/>` and
expects the **app** module to provide the value. Add the placeholder in the application project:

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["GOOGLE_SERVER_CLIENT_ID"] =
            "28935847922-s0ebsvd1u368v68kdvi28s079q7b9vnt.apps.googleusercontent.com"
    }
}
```

Use your own OAuth server client ID, or source it from `local.properties`/CI secrets. No additional
manifest edits are required – the library will pick up the placeholder during merge.

### 2.2 Optional sign-out
Call `GoogleAuthProvider.signOut()` to clear the Credential Manager state whenever you need to log
out the Google session.

---

## 3. iOS setup

Add the Google identifiers to `Info.plist`:

```xml
<key>GIDClientID</key>
<string>28935847922-pubg212r7pr5pkr2ju2la6dqhslp3vhg.apps.googleusercontent.com</string>
<key>GIDServerClientID</key>
<string>28935847922-s0ebsvd1u368v68kdvi28s079q7b9vnt.apps.googleusercontent.com</string>
<key>GIDRedirectURI</key>
<string>com.googleusercontent.apps.28935847922-pubg212r7pr5pkr2ju2la6dqhslp3vhg:/oauthredirect</string>
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.googleusercontent.apps.28935847922-pubg212r7pr5pkr2ju2la6dqhslp3vhg</string>
        </array>
    </dict>
</array>
```

- `GIDClientID` – iOS client ID from Google Cloud console.
- `GIDServerClientID` – server client ID used to exchange tokens.
- `GIDRedirectURI` and URL scheme – must match the iOS client ID (Google console provides the value).

---

## 4. Compose usage example

```kotlin
val googleUiContext = rememberGoogleAuthUiContext()
val canStartGoogleSignIn = googleUiContext != null && !isGoogleFlowRunning

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
    val account = googleAuthProvider
        .getUiProvider(context)
        .signIn()
        ?: return

    // Use account.token in your domain layer (exchange for backend session, etc.)
}
```

---

## 5. Troubleshooting

| Problem | Fix |
| --- | --- |
| `Unknown manifest placeholder GOOGLE_SERVER_CLIENT_ID` | Ensure the application module defines `manifestPlaceholders` (Section 2.1). |
| `Google server client id is missing` exception | Usually indicates the meta-data value resolved to blank. Check the placeholder and confirm the manifest merge picked it up (Gradle prints merged manifest path). |
| iOS sign-in window does not return | Confirm redirect URI and URL scheme match the `GIDClientID`. |
