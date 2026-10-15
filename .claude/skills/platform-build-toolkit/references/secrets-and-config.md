# Secrets and configuration

Owner of the rules for `secure/`, `local.properties`, synced platform configs
and logging of sensitive data.

## What lives where

| Item | Location | Tracked | Purpose |
|---|---|---|---|
| Sync task and copy logic | `secure/secure.gradle.kts` (applied from the root `build.gradle.kts`) | yes | `./gradlew syncSecureConfigs` copies the files below into the app shells |
| Firebase Android config | `secure/firebase/google-services.json` | no (gitignored) | synced into `androidApp/` for the google-services plugin |
| Firebase iOS config | `secure/firebase/GoogleService-Info.plist` | no (gitignored) | synced into `iosApp/` for `FirebaseApp.configure()` |
| Google sign-in properties | `secure/google/google-auth.properties` | no (gitignored) | server client id placeholders for both shells |
| Release signing | `secure/keystore/release.jks`, `keystore.properties`, base APK | no (gitignored) | release builds only |
| SDK paths | `local.properties` | no (gitignored) | Android SDK location |

The synced copies inside `androidApp/` and `iosApp/` are generated outputs: never
edit them by hand and never treat their presence as proof that `secure/` is
current. A missing `secure/` makes the Android build fail at the google-services
step; report that instead of inventing a config.

## Rules

- MUST NOT edit `secure/**` or `local.properties` unless the user asks for that
  exact change. MUST NOT commit, paste into replies, records or rules, or log
  any value from them (keys, client secrets, keystore passwords).
- Public identifiers (the OAuth client id placeholder in
  `androidApp/build.gradle.kts`, bundle ids) are not secrets; still do not copy
  them into documentation beyond what the build needs.
- Tokens and credentials are never written to analytics, StateKeeper archives,
  route payloads or the app log. `BackendClient` logs requests at `INFO`
  including auth endpoints; do not widen that logging, and do not add request or
  response bodies to logs.
- Push/provider tokens are not logged by service callbacks; verification of
  messaging must not print them.
- The analytics property and streams are identified from the synced Firebase
  configs and the user's selection in the admin console; never hardcode account
  or property ids in rules or code.
- Crashlytics symbol upload runs only in the iOS Release build phase; agents do
  not run release builds or uploads unless asked.
