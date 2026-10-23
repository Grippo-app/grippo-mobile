# Release, versioning and compatibility bumps

Owner of where version numbers live and which changes require a bump.
Also owns the iOS TestFlight release workflow, including Crashlytics symbols.
Version changes, release builds and store uploads require the user's request.
An explicit request to upload to TestFlight, including a destination already
settled in the task, authorizes the version update, signed archive, TestFlight
upload and matching Crashlytics symbols without repeated confirmation.
Local build/archive work grants no store upload; editing these rules starts no release.

## Where versions live

| Platform | Field | File |
|---|---|---|
| Android | `versionCode`, `versionName` | `androidApp/build.gradle.kts` (`defaultConfig`) |
| iOS | `MARKETING_VERSION`, `CURRENT_PROJECT_VERSION` | `iosApp/Configuration/Config.xcconfig` and `iosApp/iosApp.xcodeproj/project.pbxproj` (both configurations) |
| iOS restoration archive | `formatVersion` | `iosApp/iosApp/ApplicationStateStore.swift` |
| Room schema | `@Database(version = N)` + exported JSON | `data-services/database/.../Database.kt`, `data-services/database/schemas/` |
| Analytics schemas | per-schema version in each `:analytics:<feature>` implementation | see [feature-analytics](../../feature-analytics/SKILL.md) |

Android and iOS versions are maintained independently and may differ.

## Compatibility bumps

- **Room**: any entity/column/index change needs a migration object registered in
  `DatabaseMigrations.all`, a version bump and the regenerated schema JSON
  committed. Without a migration the destructive fallback drops user data.
- **Additive serialized fields**: adding an optional property with a default to a
  `@Serializable` State or route is backward-compatible and needs no bump;
  removing, renaming or retyping a field does.
- **iOS restoration archive**: when a serializer, `PersistenceKey` address,
  router shape or Essenty key changes so that a saved checkpoint can no longer
  decode, increment `formatVersion` (or the build version) so the old checkpoint
  is discarded instead of failing. Ordinary refactors keep keys unchanged.
- **Analytics**: released schemas keep their meaning; semantic changes need a
  reporting decision before a version bump (owner: feature-analytics).
- **Public Swift API**: changes to exported `:shared`, `:toolkit:*:api` or
  Decompose-facing types require the XCFramework build and an `iosApp` compile.

## Release builds

- Android release uses minification and resource shrinking with
  `androidApp/proguard-rules.pro`; signing comes from `secure/keystore/`.
- Agents may check Android release compilation when requested; they do not
  produce or upload Android store artifacts.
- An iOS TestFlight request covers the workflow below. App Store review
  submission and public release require their own request.

## iOS TestFlight release

Use terminal commands and existing configured authentication. Routine work opens
no Xcode, Organizer, Keychain Access, Simulator or browser windows. The main
session owns archive/upload commands; verifiers retain their compile-only rights.
Keep logs, export settings, archives and derived data in an absolute task scratchpad.

1. **Inspect.** Capture the [baseline](../../task-orchestrator/references/change-attribution.md).
   Read the version files, actual scheme and Release settings using
   `xcodebuild -list -project iosApp/iosApp.xcodeproj` and `-showBuildSettings`
   with the selected project/scheme. Follow [CLI scheme recovery](ios-framework.md#cli-scheme-recovery)
   if needed. Reuse the configured Apple account or API-key reference under
   [secret handling](secrets-and-config.md); inspect only relevant signing identities.
2. **Select the version once.** Keep the marketing version unless another was
   requested. Use a fixed requested build; otherwise start at current build + 1,
   reconciling already available upload history before choosing the number.
   Reconcile source assignments and effective Release settings. Missing read access alone
   does not block a normal first upload; handle actual conflicts below.
3. **Keep the inputs consistent.** Serialize builds sharing this checkout's
   framework outputs; resume a running job for this request instead of starting
   another. After version edits, save HEAD and the content state of release inputs,
   including relevant untracked sources, in the scratchpad. A dirty-file count
   alone is insufficient. Recheck inputs after framework packaging/archiving and
   before upload. Relevant concurrent edits invalidate affected checks/artifacts:
   resolve overlap under change attribution and rebuild those stages. Preserve
   other writers' work; do not silently include their new changes.
4. **Build, then archive.** Apply the [verification policy](../../../../CLAUDE.md#verification-policy)
   to these stages and any additional checks; reuse results only for unchanged
   inputs. Build `./gradlew :shared:assembleSharedReleaseXCFramework --console=plain`.
   Require exit 0 and `BUILD SUCCESSFUL`; a single framework link omits the complete
   package's resource integration. Record the packaged framework's content checksum
   before archiving. Run the following command separately, only after build success.
   Replace bracketed paths; add credential/provisioning flags only when configured
   signing requires them, using the installed `xcodebuild -help`.

   ```sh
   xcodebuild archive \
     -project iosApp/iosApp.xcodeproj -scheme iosApp \
     -configuration Release -destination 'generic/platform=iOS' \
     -archivePath '<scratchpad>/Grippo-<version>-<build>.xcarchive' \
     -derivedDataPath '<scratchpad>/ios-derived'
   ```

5. **Verify before TestFlight export.** Require successful required checks, archive exit 0
   and `ARCHIVE SUCCEEDED`.
   Check the archived app's bundle ID, version/build and code signature, matching
   binary/dSYM UUIDs, and unchanged source/framework content from step 3/4. An
   integrity failure blocks TestFlight export/upload; an older archive at the
   same path is not a successful result of this attempt.
6. **Send Firebase symbols.** Follow [Crashlytics symbols](#crashlytics-symbols)
   for this archive. A Firebase transport failure leaves that result incomplete;
   continue the independent TestFlight upload of the verified archive.
7. **Upload to TestFlight.** After step 5 passes, prepare scratchpad
   `ExportOptions.plist`: `method=app-store-connect`, `destination=upload`,
   `manageAppVersionAndBuildNumber=false`, `uploadSymbols=true`, plus inspected
   team/signing values. Validate options with installed `xcodebuild -help`;
   reuse authentication and add `-allowProvisioningUpdates` only if needed.
   Run export separately from the build/archive commands and wait for its result.

   ```sh
   xcodebuild -exportArchive \
     -archivePath '<scratchpad>/Grippo-<version>-<build>.xcarchive' \
     -exportPath '<scratchpad>/export' \
     -exportOptionsPlist '<scratchpad>/ExportOptions.plist'
   ```

8. **Report evidence.** Give version/build, inspected source state, archive/log
   paths and separate TestFlight/Firebase results. Apple acceptance, processing
   and tester availability are distinct. Use existing CLI/API access for readback;
   report unobserved processing/availability as pending or unverified.

### Crashlytics symbols

Inspect the project's `Upload Crashlytics symbols` phase and resolved SDK's
`Crashlytics/run`: current Release device archiving validates synchronously but
uploads in the background. Archive success does not confirm Firebase delivery.
The phase also runs for archive-only work; if external uploads are forbidden,
use an inspected, supported suppression mechanism or report that archive step
blocked. Do not invent skip flags or disable the phase permanently.

Compare app and required framework dSYM UUIDs with archived binaries using
`xcrun dwarfdump --uuid`. Inspect `iosApp/scripts/generate-embedded-framework-dsyms.sh`:
current static resource stubs have matching dSYMs; their code symbols belong to
app dSYM. Preserve that distinction; do not fabricate symbols for executable code.
If completed automatic upload is not evidenced, run this build's resolved SDK
`Crashlytics/upload-symbols` in the foreground with `-gsp` pointing to the archived
app's `GoogleService-Info.plist`, `-p ios` and this archive's `dSYMs` directory.
Require successful output and exit 0. Apple `uploadSymbols=true` sends no symbols
to Firebase. A later Firebase retry reuses this archive's matching symbols.

### Resume, recovery and questions

| Observed state | Action under the existing release request |
|---|---|
| Current job running, accepted or processing | Continue/report that attempt; preserve its archive and build number. A status question starts no new release. |
| Upload result unknown | Inspect saved logs and available Apple status for this attempt before resubmission; do not renumber or blindly retry. |
| Definitive failure with a reusable number | Fix the reported cause; rebuild affected artifacts only if needed and keep the valid number when Apple permits reuse. |
| Number occupied by another upload | Reconcile history. For automatic numbering, choose an unused higher number from evidence and rebuild; a fixed user-selected value requires resolving the conflict. Do not guess through repeated collisions. |
| Only Firebase upload failed | Retry matching symbols from the same archive; do not repeat the successful Apple upload or bump the build. |

After correcting inputs, capture their updated state and repeat affected checks
before upload.

Ask only for a still-missing decision/action: ambiguous destination/app/team,
an unusable fixed version/build, unresolved upload status, unavailable credentials,
required Apple login/2FA or an account-owner agreement. Use evidence first; never reconfirm
settled choices or request passwords/key contents in chat. UI is reserved for a
proven account/authentication blocker with no configured CLI solution, or the
user's explicit UI request. Return to terminal afterward. Stop an unchanged
failure instead of polling it or creating replacement releases while Apple processes.

### Release completion gate

- Source/input state and packaged framework remained consistent through checks,
  archive and pre-upload verification; selected version/build and signature match.
- Required checks, complete Release XCFramework and this signed archive succeeded.
- Symbol UUID checks passed and completed Firebase upload has successful evidence.
- Completed TestFlight upload confirms acceptance; processing/tester availability
  is claimed only when separately observed. Any failed/missing outcome stays explicit.
- Routine work stayed in terminal with the established authorization; remaining
  blockers name the failed step and minimal required action.

References: [Apple command-line archive/export](https://developer.apple.com/library/archive/technotes/tn2339/_index.html),
[Apple upload statuses](https://developer.apple.com/help/app-store-connect/manage-builds/view-builds-and-metadata/),
installed `xcodebuild -help`, [Firebase dSYM guidance](https://firebase.google.com/docs/crashlytics/ios/get-deobfuscated-reports).
