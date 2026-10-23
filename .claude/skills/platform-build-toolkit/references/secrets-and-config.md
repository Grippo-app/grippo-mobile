# Secrets and configuration

Owner of the rules for `secure/`, `local.properties`, synced platform configs
and logging of sensitive data.

## What lives where

| Item | Location | Tracked | Purpose |
|---|---|---|---|
| Sync task and copy logic | `secure/secure.gradle.kts` (applied from the root `build.gradle.kts`) | yes | `./gradlew syncSecureConfigs` copies Firebase configs and updates Google identifiers in the app shells |
| Firebase Android config | `secure/firebase/google-services.json` | no (gitignored) | synced into `androidApp/` for the google-services plugin |
| Firebase iOS config | `secure/firebase/GoogleService-Info.plist` | no (gitignored) | synced into `iosApp/` for `FirebaseApp.configure()` |
| Google sign-in properties | `secure/google/google-auth.properties` | no (gitignored) | server client id placeholders for both shells |
| Android signing inputs | `secure/keystore/` | no (gitignored) | sensitive local material; the checked-in build does not configure release signing ([release](release.md#release-builds)) |
| SDK paths | `local.properties` | no (gitignored) | Android SDK location |

The synced Firebase files inside `androidApp/` and `iosApp/` are generated
outputs: never edit them by hand or treat their presence as proof that the
secure inputs are current. The sync task also rewrites Google identifiers in
tracked `androidApp/build.gradle.kts` and `iosApp/iosApp/Info.plist`. The main
session owns any needed sync and reviews those source changes under
[change attribution](../../task-orchestrator/references/change-attribution.md);
read-only verifiers report missing inputs instead of running the task.

Missing secure inputs block synchronization. A missing synced Android Firebase
file blocks the google-services step; a missing `secure/secure.gradle.kts`
blocks Gradle configuration itself. Report the actual missing input rather
than inventing a config.

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
- Release and Crashlytics upload authorization follows the
  [release workflow](release.md#ios-testflight-release).
