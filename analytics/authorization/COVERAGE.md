# Authorization analytics coverage

Record owner: [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md)
and [state-preservation](../../.claude/skills/state-preservation/SKILL.md); records
policy: [CLAUDE.md](../../CLAUDE.md#records). Cross-feature contract and
verification limits: [current analytics contract review](../COVERAGE.md#current-analytics-contract-review-2026-10-14);
admin reconciliation: [Live GA4 setup review](../COVERAGE.md#live-ga4-setup-review--2026-10-14).
Rows describe the current code of `:ui-screen-features:authorization`, its routers
in `:ui-screen-features:screen-api` (`RootRouter.Auth`, `AuthRouter`,
`AuthProcessRouter`, `ProfileCreationRouter`) and `:analytics:authorization`;
replace them when that code changes. Typed dismissal observations use
DismissRequested with `dismiss_requested` wire codes; operation schemas omit
visible-screen context and correlate outcomes by `operation_id`; the typed catalog is not yet in a store build and every schema is version 1. The historical route-entry reason codes
(`AuthorizationEntryPoint`) are no longer part of the contract.

## Startup and navigation

- Splash requests no catalog: it navigates to
  `resolveDirection()` under the `AppContent` loader with `ErrorRecovery.Retry`, so
  a cold start resolves offline from local data unless a session exists without a
  cached user. The error popup's Retry repeats the resolution; closing it leaves
  Splash in place (system Back exits).
  Resolution order: onboarding not yet shown → Onboarding; no token → AuthProcess;
  `hasProfile()` → Home, otherwise ProfileCreation. The SessionCheck decorator
  lives inside `hasProfile()` and encloses the cached-user read
  (`observeUser().firstOrNull()`, early true) and the optional
  `getUser()` call, once per invocation. It therefore runs only after
  onboarding has been shown and a token exists.
- `AuthRouter.Onboarding` is a payload-free serializable object; no destination
  enum, generic Continue or cross-leaf dispatcher exists. Onboarding is the
  one-time first screen: Get started and Back both continue to Login through
  `AuthViewModel.toAuthProcess` → `AuthDirection.AuthProcess`, which AuthComponent
  handles with `replaceAll(AuthRouter.AuthProcess())` instead of pushing the
  placeholder into Back history. A restored Onboarding
  route stays interactive and continues to Login the same way.
- Onboarding preparation reads the durable shown flag; on first entry it
  initializes absent analytics consent to true through the atomic conditional
  `setDefaultAnalyticsConsent(true)` write (absent key only, inside
  `DataStore.edit`), then marks onboarding shown. Existing saved true/false values
  are preserved. Shown is an entry-based flag: closing or restarting after that
  write never shows onboarding again. Consent is observed with
  `Processing.Infinity` from `init`, in parallel with preparation, so on a first
  onboarding it may emit Denied before the default write lands and Granted after it.
  A failed preparation does not mark shown; the error popup's Retry repeats it and
  closing leaves the screen usable with the observed consent. A failed consent read is
  re-collected by Retry; closing it while the value is still unknown keeps Get started
  and Back inactive until the app restarts (local storage failure only, accepted).
- AuthProcess starts at Login; Registration is pushed with the optional valid
  email seed. Alternative entry: `RootDirection.Login` opens
  `Auth(AuthRouter.AuthProcess())` from Root's token-null observer when the active
  Root route is not Auth. ProfileCreationViewModel observes its own session prerequisite
  and emits the existing ToLogin direction when the token is null, including a restored
  ProfileCreation route; the existing Component callback chain reaches AuthProcess.
  Root does not inspect the Auth child's internal routes. Splash, Onboarding and an existing
  AuthProcess remain in place on the initial null token.
  A failed token observation restarts only through its error popup's Retry; by the
  owner's decision a closed popup leaves token-loss routing inactive until the owner
  is recreated. The host retains pending errors and their callbacks FIFO, and a later
  opening does not close an active error.
  queued deeplinks still resolve at Home. Login and
  Registration use ordinary push/pop.
- ProfileCreation runs User → Experience → ExcludedMuscles → MissingEquipments →
  Completed; the parent stores accepted step values and seeds fresh children from
  them. Completed runs CreateProfile from `init`; User Back performs Logout and the
  parent replaces the stack with AuthProcess. Successful login or registration
  navigates to Home or ProfileCreation without adding screen owners; Root replaces
  authorization with Home.
- A fresh HomeViewModel owns the startup catalog refresh after entering Home,
  including after successful auth/profile completion: one tracked operation
  (`refresh_catalogs`) that refreshes muscles, then equipment, then the exercise
  examples. Root owns no catalog request, Job, route guard or locale/foreground
  refresh subscription. Ordinary
  foreground or locale changes do not repeat the request on a retained Home VM.
  Failed/offline refreshes keep the last Room snapshot; the error popup's Retry
  repeats the whole block, closing it does not. Home continues observing Room
  ([home](../home/COVERAGE.md)).
  LoginUseCase and RegisterUseCase refresh muscles, then equipment, before the
  credential request, so a stored session always has a local catalog (also when
  Splash later routes a profile-less session to ProfileCreation) and the excluded-set
  writes in their preparation find their foreign-key parents. A successful sign-in
  refreshes both again in Home's startup operation. CreateProfileUseCase writes
  exclusions chosen from that local catalog. None of the three injects
  ExerciseExampleFeature or requests examples; their Koin factories are reconciled.
  Required profile, exclusion, weight-history, goal and push-token steps keep
  their original error contracts. Login's consent UI, callback,
  observation, loader, transient field and localized label were removed; its
  authentication button group is unchanged.

## Surfaces and screen hooks

| Route / rendered surface | Component → ViewModel | AuthScreen / wire identity | Hook |
|---|---|---|---|
| `RootRouter.Auth(AuthRouter.Splash)` (Root initial), full-screen BrandSplash with loader | SplashComponent → SplashViewModel | Splash / `auth_splash` | Typed screenPresentation |
| `AuthRouter.Onboarding`, one-time welcome (intro illustration, three BannerCards, compact consent switch row, Get started) | OnboardingComponent → OnboardingViewModel | Onboarding / `auth_onboarding` | Typed screenPresentation |
| `AuthProcessRouter.Login`, email/password form, Email/Google/Apple buttons, Register link | LoginComponent → LoginViewModel | Login / `auth_login` | Typed screenPresentation |
| `AuthProcessRouter.Registration(email?)`, email/password form, Create account | RegistrationComponent → RegistrationViewModel | Registration / `auth_registration` | Typed screenPresentation |
| `ProfileCreationRouter.User`, name input, weight/height pickers, Continue | UserComponent → UserViewModel | User / `profile_creation_user` | Typed screenPresentation |
| `ProfileCreationRouter.Experience`, four selectable cards, Continue | ExperienceComponent → ExperienceViewModel | Experience / `profile_creation_experience` | Typed screenPresentation |
| `ProfileCreationRouter.ExcludedMuscles`, muscle groups and items, Continue | ExcludedMusclesComponent → ExcludedMusclesViewModel | ExcludedMuscles / `profile_creation_excluded_muscles` | Typed screenPresentation |
| `ProfileCreationRouter.MissingEquipments`, equipment groups and items, Continue | MissingEquipmentsComponent → MissingEquipmentsViewModel | MissingEquipments / `profile_creation_missing_equipments` | Typed screenPresentation |
| `ProfileCreationRouter.Completed`, full-screen BrandSplash with loader during profile creation | ProfileCompletedComponent → ProfileCompletedViewModel | Completed / `profile_creation_completed` | Typed screenPresentation |
| Auth graph (`childStack` key `AuthComponent`) | AuthComponent → AuthViewModel | Default null | Child stack host only |
| AuthProcess graph (key `AuthProcessComponent`) | AuthProcessComponent → AuthProcessViewModel | Default null | Login/Registration stack host |
| ProfileCreation graph (key `ProfileCreationComponent`) | ProfileCreationComponent → ProfileCreationViewModel | Default null | Child stack plus shared StepProgressIndicator chrome |
| Root host (`:shared`) | RootComponent → RootViewModel | Default null | Outside this module; confirmed container |

All nine `AuthScreen` entries have exactly one owner; the nine leaf factories
resolve AuthAnalytics only inside `retainedInstance`. BaseComponent binds the VM
hook before activation/navigation bridging; foundation presents Initial on the
first resume and Restore afterwards, closes on pause/destroy and emits the
built-in `screen_view` with `screen_id`/`navigation_reason` context. Containers
resolve no screen service and carry no analytics dependency. Restored stacks use
the same retained factories and hooks. The AuthProcess `FlowId` is route identity,
not telemetry.

## Callbacks, inputs and operations

| Owner | Observed callbacks / inputs → typed event → wire | Complete operations and eligibility |
|---|---|---|
| SplashViewModel | System Back → Splash.DismissRequested → `auth_control_clicked` {auth_splash, dismiss_requested}; no busy guard | SessionCheck → `auth_session_check_*`, no parameters besides terminal `duration_ms`; wraps `hasProfile()` only (see above); cache hit and remote lookup both complete normally; the error popup's Retry repeats the resolution with a new SessionCheck; closing it emits nothing |
| OnboardingViewModel | Accepted Get started → Onboarding.GetStarted → `auth_control_clicked` {auth_onboarding, next}; Back → Onboarding.DismissRequested → {auth_onboarding, dismiss_requested}; both require a known consent source (non-null) and no active loaders, then navigate to Login | None. Preparation initializes absent consent and marks shown without persistence telemetry; `onAnalyticsConsentChange` rejects busy, pending and unchanged values, then persists directly inside safeLaunch and deliberately emits nothing |
| LoginViewModel | Email/password validation-status transitions → Login.InputChanged → `auth_input_changed` {auth_login, input_field, input_status}; focus → `auth_input_focused`; email clear → `auth_input_cleared`; Next/Done IME → `auth_keyboard_action` {field, next/done}; password visibility → `auth_password_visibility_changed` {password_visible}; Submit by Email/Google/Apple → `auth_submit_clicked` {auth_method} after the guards and before safeLaunch, so the error popup's Retry does not re-emit it (a Google/Apple Retry reuses the captured UI context: after an Activity recreation it fails into a new popup); Register → `auth_control_clicked` {register} after the busy guard, passing the valid email display as the route seed; Back → {dismiss_requested}, no guard | Login(Email) → `auth_attempt_*` {auth_method=email, auth_action=login}: busy guard plus valid email and password; block is the throwing `loginEmail` use case (Boolean), which first refreshes muscles and equipment, before the credential request. Login(Google)/Login(Apple): busy guard; block includes direct throwing provider UI sign-in and `loginGoogle(token)`/`loginApple(code)`. `mapAuthError` is presentation-only |
| RegistrationViewModel | Same input family under Registration.* with `interaction_screen=auth_registration`; Submit → `auth_submit_clicked` {email} emitted after busy and validity guards, before safeLaunch; toolbar/system Back → {dismiss_requested}, no guard | Register → `auth_attempt_*` {email, register}; block is `createAccount(email, password)` including the muscle and equipment refresh before the registration request and its private preparation; `ErrorRecovery.Close`: the account may already exist when a later step fails, so the error popup offers only Got it |
| UserViewModel | Name status transitions → `auth_input_changed` {profile_creation_user, name, status}; focus, clear, Done IME as above; Weight/Height picker open → User.PickerOpened → `auth_picker_opened` {input_field} after the busy guard; each accepted picker result → state update, then User.PickerConfirmed → `auth_picker_confirmed` {input_field, input_status} (including unchanged values); Next → `auth_control_clicked` {next} after the busy guard and valid name/weight/height; Back → {dismiss_requested} after the busy guard | Logout → `profile_operation_*` {profile_action=logout}; block is `logoutUseCase.execute()`, then Back → parent replaces the stack with AuthProcess |
| ExperienceViewModel | Selection → Experience.Selected → `auth_experience_selected` {profile_experience}; Next → {next} only with a selected experience; Back → {dismiss_requested} | None |
| ExcludedMusclesViewModel | Group click → `auth_control_clicked` {group_selected}; item toggle → Muscles.SelectionChanged → `auth_selection_changed` {selected, selected_count}; Next → {next}, no VM guard; Back → {dismiss_requested} | None; catalog observations rebuild state without interaction events |
| MissingEquipmentsViewModel | Group click, item toggle, Next, Back as for muscles with `interaction_screen=profile_creation_missing_equipments` | None |
| ProfileCompletedViewModel | System Back → `auth_control_clicked` {profile_creation_completed, dismiss_requested}, no busy guard | CreateProfile → `profile_operation_*` {profile_action=create}, started from `init` only when the accepted experience maps to a domain value (absent experience navigates Back without an operation); block builds the profile request, runs the throwing `createProfileUseCase.execute()` (create, reload user and related data) and directly writes the local home welcome status; `ErrorRecovery.Close` (the profile may already exist when a later step fails): the error popup offers only Got it and its `onClose` navigates Back |
| Container VMs | No synthetic navigation/click facts | No analytics operations |

Login/Register preparation calls throwing commands directly. A malformed token
response or profile-present user response that cannot produce the complete domain
User fails before its cache write. Unknown role keys fail required dictionary promotion;
they do not become DEFAULT, including the profile-absent routing branch.
All four account/session exchanges share
`AuthorizationRepositoryImpl.saveSession`; blank required IDs/access/refresh values
fail before token storage or active-session publication. Every user response must match the active
account captured before its request. A valid absent profile returns false only
when no same-account profile is cached; otherwise synchronization fails while
preserving the cached profile and local draft (deleting User would cascade it).
Push-token Unavailable remains optional; a returned typed Failed outcome throws with
its reason. Explicit provider fetch exceptions and cancellation propagate unchanged;
the Swift provider passes Firebase's Error through the existing completion error slot.
The separate app-lifetime callback synchronization queue keeps its coarse status
contract. Existing operation types and boundaries remain.

Google/Apple UI providers return their account directly and throw on failure inside
the same complete Login operation. Kotlin failures retain their cause when SDK
classification is required; coroutine cancellation and fatal errors propagate unchanged.
Native failures retain the available domain/code/description. Required credential/token
decoding failures do not become successful accounts with fallback values. Optional
display fields retain their declared defaults when absent.
Google availability tests whether its primary configuration key exists. Supplied
invalid, blank or wrong-type configuration fails inside the existing Login operation;
an absent optional iOS redirect still uses the established derived URI.

Refresh ownership and waiter attachment are selected together under a short mutex.
Incomplete attempts are keyed by the captured credential snapshot; a delayed old-session
request cannot displace a running refresh and permit a duplicate request for its token.
Each waiter retains its attempt's completion and rejected token snapshot; a later attempt
cannot overwrite that failure. The owner publishes success only after its timeout block returns.
Refresh waiters return true only for a new nonblank token with no recorded failure;
failed owners complete that captured attempt exceptionally with the original failure.
Requests pin their original session locally; refresh and Ktor's header replay cannot bind
an old request to another account. Conditional DAO update/delete matches the captured
credentials and active owner atomically, preserving replacement credentials across logout/login.
No updated row means refresh failed. A same-owner changed nonblank access already stored
before Ktor's version publication needs no second refresh.
Ordinary cleanup errors remain suppressed on that failure, while cleanup cancellation
or fatal errors remain primary. Cleanup targets the captured failing session, not a
new active account. Caller/owner-timeout cancellation does not initiate token deletion.
Malformed nonmissing refresh IDs or blank credentials fail before storage. The existing
missing-refresh-ID AuthProvider false outcome remains a failure with no deletion.
Ordinary file-sink failures cannot replace the original error or prevent the shared error dialog.

Text status events compare the existing old/new validation status, omit equal
statuses (including Empty → Empty) and keep no cache. Clear is a separate callback
from its resulting status transition. Picker confirmations fire for each accepted
result, including unchanged values; opening and confirmation remain separate
facts. Selection counts describe selected (available) items, not the complementary
exclusion list sent on Next. Repeated group and experience clicks remain observed
callbacks. Operation terminals: completed means the block returned;
`CancellationException` → interrupted; any other exception → failed; the original
error is rethrown. Ordinary reporting exceptions are swallowed in `trackEvent`.

## Catalog and reporting decision

`AuthAnalyticsModule` (`@Module @ComponentScan`) binds `AuthAnalyticsImpl` as
`AuthAnalytics` and registers one qualified `authorization_analytics` schema
provider (`AuthOperationSchema.all + AuthInteractionSchema.all`); `shared/.../Koin.kt`
lists it once, and the module depends only on `analytics-api`. The 23 version-1
schemas:

- Interaction: `auth_control_clicked` [interaction_screen, interaction_action =
  dismiss_requested/next/register/group_selected], `auth_submit_clicked`
  [interaction_screen, auth_method = email/google/apple], `auth_input_changed`
  [interaction_screen, input_field = email/password/name/height/weight,
  input_status = empty/invalid/valid], `auth_input_focused`, `auth_input_cleared`
  [interaction_screen, input_field], `auth_keyboard_action` [.., input_field,
  keyboard_action = next/done], `auth_password_visibility_changed`
  [interaction_screen, password_visible], `auth_picker_opened` [.., input_field],
  `auth_picker_confirmed` [.., input_field, input_status],
  `auth_experience_selected` [.., profile_experience =
  beginner/intermediate/advanced/pro], `auth_selection_changed` [.., selected,
  selected_count].
- Operations: `auth_attempt_started/succeeded/failed/interrupted` [auth_method,
  auth_action = login/register, terminal duration_ms] for Login and Register;
  `auth_session_check_started/completed/failed/interrupted` [terminal duration_ms]
  for SessionCheck; `profile_operation_started/completed/failed/interrupted`
  [profile_action = create/logout, terminal duration_ms] for CreateProfile and
  Logout. Operation payloads carry `operation_id` and omit screen context.

Reporting decision: height/weight accepted picker results belong exclusively to
`auth_picker_confirmed`; `auth_input_changed` observes only existing text
validation-status transitions for email, password and name. Dismissal action
codes use `dismiss_requested` consistently. The typed catalog has not shipped in a
store build, so no typed reporting population requires a migration; the legacy
store-build events (`registration_completed`, `workout_*`) are retained separately
from the current operation definitions. Durations and failure populations across older installed
builds are not an unchanged baseline: exercise-example refresh failures belong to
Home; Splash requests no catalog, so offline cold starts reach Onboarding,
SessionCheck, Login or Home instead of stopping there, and Login/Register attempts include the muscle
and equipment refresh before the credential request (Google/Apple: after the
provider sign-in UI), so `auth_attempt_failed` no longer implies a rejected
credential. Admin consumers of `auth_attempt_*`, `auth_session_check_*`,
`profile_operation_*`, `auth_control_clicked` and `screen_view` were reconciled
before this change in the
[Live GA4 setup review](../COVERAGE.md#live-ga4-setup-review--2026-10-14); the
`Grippo Auth method`, `События и экраны` and `Сбои операций` descriptions were
updated for this change on 2026-10-28 (root SessionCheck and auth_attempt rows).

## Omissions and privacy

No dedicated picker/dialog view or close contract, dismissal or provider-cancel
reason, error classifier, session-presence/profile-presence result, destination
or route-entry attribution, group/item ID or transition/funnel event is emitted.
Catalog loads, direction resolution, retry, error dialogs, consent switch
toggles, default-consent and shown writes, preparation, programmatic startup,
restoration, stack replacement and automatic Home navigation fabricate no user
clicks; rejected (busy, invalid, not-ready) callbacks emit nothing. Get started is
continuation, not an opt-in event; collection follows the separately saved
installation preference, applied by the shared ApplicationBootstrap observer, and
no suppressed earlier operation or view is replayed after the default grant. The
password field has no clear button or callback. Focus reports actual UI focus,
including focus movement, without inferred intent. Consent, identity and SDK
transport remain app/core-owned. Payloads contain only bounded codes, booleans,
counts and durations: no raw input or length, email, name, measurements,
credentials/tokens, entity IDs, exception messages, URLs or request/response
bodies. The transport layer no longer applies auth-specific response sanitation
or diagnostic/redaction request markers (explicit user decision); logging stays enabled in all build
modes, and that transport change adds no screen, preserved input or payload.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| SplashComponent / SplashViewModel | No editable data (`SplashState` is a data object); loading | Runtime only / Rebuild | No DRAFT; `AppContent` loader | A fresh VM re-resolves the direction from `init` using DataStore, token and user; no catalog request |
| OnboardingViewModel / OnboardingState | `analyticsConsent: Boolean?` | Rebuild | `observeAnalyticsConsent()` with Processing.Infinity, started in `init` in parallel with preparation; no DRAFT, State not serializable | Null while pending; switch checked unless false, enabled only with a known source and no loaders; accepted toggles commit immediately, so no unfinished draft exists; a failed source read keeps the last value (null before the first emission) and disables dependent controls while null; Retry re-collects the source, closing the popup keeps that state; a failed write retains the actual stored value |
| OnboardingViewModel | Shown flag read in preparation | Rebuild | `observeOnboardingShown().first()` | A restored route re-runs preparation; writes are skipped when already shown; the screen stays interactive |
| Onboarding | `Preparation`/`AnalyticsConsent` loaders, error and Back sessions | Runtime only | OnboardingLoader | Not archived |
| LoginComponent / LoginState | `email: EmailFormatState` (display and value, including invalid/empty) | Preserve | `DRAFT = PersistenceKey("LoginDraft", LoginState.serializer())` | `restoreState(DRAFT) ?: LoginState()` (non-null); `saveState` in init |
| LoginState | `password` | Runtime only (secret) | `@Transient`, default Empty | Excluded from the archive; fresh restore starts empty |
| LoginState | `isGoogleLoginAvailable`, `isAppleLoginAvailable` | Rebuild | `@Transient`; recomputed from the providers' `isSupported` in the VM constructor | Overrides any restored value |
| RegistrationComponent / RegistrationState | `email` | Preserve | `DRAFT = PersistenceKey("RegistrationDraft", RegistrationState.serializer())` | `restoreState(DRAFT) ?: RegistrationState(email = EmailFormatState.of(seed))`: a saved draft wins over the route seed |
| RegistrationState | `password` | Runtime only (secret) | `@Transient` | Excluded |
| ProfileCreationComponent / ProfileCreationState | Accepted step values: name, weight, height, experience, excludedMuscleIds, missingEquipmentIds | Preserve | `DRAFT = PersistenceKey("ProfileCreationDraft", ProfileCreationState.serializer())`; lists via ImmutableListSerializer | `restoreState(DRAFT) ?: ProfileCreationState()`; `saveState` registered before the child stack is built, so children are seeded from restored parent values; values written only on accepted Next callbacks |
| UserComponent / UserState | `name`, `weight`, `height` formats, including accepted picker results | Preserve | `DRAFT = PersistenceKey("UserDraft", UserState.serializer())` | `restoreState(DRAFT) ?: UserState()` with static fresh defaults (70.0 kg, 175 cm); picker `onResult` updates the draft before the confirmation event; User is the stack root, so Back from later steps pops to the retained instance |
| UserViewModel | Weight/height picker sessions, `Logout` loader | Runtime only | DialogConfig.WeightPicker/HeightPicker; host slot and step stack `serializer = null` | Unsaved; only the accepted result lands in UserDraft |
| ExperienceComponent / ExperienceState | `selected` | Preserve | `DRAFT = PersistenceKey("ExperienceDraft", ExperienceState.serializer())` | `restoreState(DRAFT) ?: ExperienceState(selected = parent experience)`; re-entry after pop seeds from the accepted parent value |
| ExperienceState | `suggestions` | Rebuild | `@Transient`, enum entries | Static |
| ExcludedMusclesComponent / ExcludedMusclesState | `selectedMuscleIds`, `knownMuscleIds`, `selectedGroupId` | Preserve | `DRAFT = PersistenceKey("ExcludedMusclesDraft", ExcludedMusclesState.serializer())`; ImmutableListSerializer | `restoreState(DRAFT) ?: ExcludedMusclesState(knownMuscleIds = parent exclusions)`; a catalog with muscle IDs keeps selected known IDs, selects new IDs, drops absent IDs and re-validates the group; emissions with no muscle IDs update only suggestions, preserving known/selected IDs and the selected group; Next sends known minus selected |
| ExcludedMusclesState | `suggestions` | Rebuild | `@Transient`; non-null empty ImmutableList; observeMuscles() → provideMuscles in a plain observation | MuscleSelection receives the actual groups, including a real empty list, without a catalog loader, additional cache read or readiness state. Continue stays enabled in its inline bottom slot. Provider reconciliation preserves accepted/restored selected/known IDs |
| MissingEquipmentsComponent / MissingEquipmentsState | `selectedEquipmentIds`, `knownEquipmentIds`, `selectedGroupId` | Preserve | `DRAFT = PersistenceKey("MissingEquipmentsDraft", MissingEquipmentsState.serializer())` | A nonempty flattened equipment catalog keeps selected known IDs, selects new IDs, drops absent IDs and re-validates the group; empty flattened catalogs update only suggestions, preserving known/selected IDs and the selected group; Next sends known minus selected |
| MissingEquipmentsState | `suggestions` | Rebuild | `@Transient`; non-null empty ImmutableList; observeEquipments() → provideEquipments in a plain observation | EquipmentSelection receives the actual groups, including a real empty list, without a catalog loader, additional cache read or readiness state. Continue stays enabled in its inline bottom slot. All accepted/restored selected/known-ID behavior remains unchanged |
| ProfileCompletedComponent | Name/weight/height/experience/exclusion inputs | Rebuild from parent | Constructor arguments from ProfileCreationState; no DRAFT; data-object State | A fresh VM re-runs CreateProfile from init, including after restoration; the `ProfileCreation` loader is runtime only |
| AuthComponent stack | `AuthRouter` (Splash, Onboarding, AuthProcess(flowId), ProfileCreation) | Preserve (navigation) | `childStack(serializer = AuthRouter.serializer(), key = "AuthComponent")`; `flowId` is `@EncodeDefault` | Initial stack from `RootRouter.Auth(value)`; mutated only by replaceAll |
| AuthProcessComponent stack | `AuthProcessRouter` (Login, Registration(email)) | Preserve (navigation plus nonsecret seed) | `AuthProcessRouter.serializer()`, key `AuthProcessComponent` | Initial Login; Registration pushed with the email seed |
| ProfileCreationComponent stack | `ProfileCreationRouter` (five objects) | Preserve (navigation) | `ProfileCreationRouter.serializer()`, key `ProfileCreationComponent` | Initial User; push/pop |
| RootComponent stack | `RootRouter.Auth(AuthRouter)` | Preserve (navigation) | `RootRouter.serializer()`, key `RootComponent` | Initial `Auth(AuthRouter.Splash)` |
| RootViewModel / ProfileCreationViewModel | Session prerequisite and token observation | Rebuild / Runtime | Existing AuthorizationFeature.getToken source; no token in State, routes or drafts | Each retained owner observes its own prerequisite and re-subscribes only through the error popup's Retry (a closed popup leaves the observation inactive until the owner is recreated); a fresh restored ProfileCreation rechecks the source |
| RootComponent / RootViewModel | Catalog startup ownership | None in Root | HomeViewModel starts the muscle, equipment and `ExerciseExampleFeature.getExerciseExamples` refresh from init | Root only routes the completed auth/profile flow to Home; a fresh Home VM owns its startup request. Root has no catalog dependency, Job, locale/foreground subscription or route guard. Existing token-loss routing is unchanged; Root renders its stack immediately; `AppLocale.isResolved` only keeps the Android system splash (MainActivity) until the stored language is read (or the read fails), and date formatting follows `AppLocale` with no install step; no catalog metadata enters an archive |
| LocalSettingsRepository (DataStore) | `onboarding_shown` | Durable installation preference | `booleanPreferencesKey("onboarding_shown")` | Absent → false; one-way true write; `clearUserSettings` clears account preferences and restores it atomically with `analytics_consent` and `language` across logout/delete |
| LocalSettingsRepository (DataStore) | `analytics_consent` | Durable installation preference | `booleanPreferencesKey("analytics_consent")` | Absent/false → Denied, true → Granted; `setDefaultAnalyticsConsent` writes only when absent; `setAnalyticsConsent` writes unconditionally; preserved across `clearUserSettings`, which LogoutUseCase and DeleteProfileUseCase call; applied to collection by ApplicationBootstrap |
| All authorization VMs | Restoration flags, previous-source caches | None | No `var`/`lateinit` member in any authorization ViewModel | Conditions derive from State |

All seven DRAFT keys take non-null initial state through `?:` fallbacks. Secrets
never enter archives or route payloads: both password fields are `@Transient`,
and the preparation/consent writes hold no secret. Restoration initializes state
directly without replaying input or control callbacks, synthetic events or
auto-submitted authentication; restarting Splash loading and the Completed
CreateProfile operation is existing business behavior. Analytics provenance,
loaders, jobs, dialog sessions and focus are runtime only. Display-only one-time
onboarding semantics intentionally do not promise resuming a future unfinished
multi-step onboarding. Shell limits: Android supplies saved state through
retainedComponent; iOS restores one completed background checkpoint, and abrupt
active termination is not a guaranteed restoration path.

## Verification

Source gates for the current revision were reconciled against actual routes,
Component factories, rendered bodies, retained injection, foundation binding,
callback eligibility, complete operation blocks, reporting isolation,
cancellation propagation, local suppliers/serializers, child seeding from parent
values, provider-availability recomputation, the four stack keys and the durable
LocalSettings owners (read-only audit on 2026-10-15; no build or runtime check
was part of that audit). Checks recorded for the implementing revisions:

- 2026-10-13 picker-encoding repair (internal encoding/schema and documentation
  only): `git diff --check` and inline invariants passed (preserved enum codes,
  dedicated picker schema/mapping/catalog, eight unique leaf hooks and three null
  containers at that revision); `:analytics:authorization:compileKotlinIosSimulatorArm64`
  passed with unresolved global opt-in marker warnings for libraries absent from
  the pure module; the first sandboxed attempt could not write the Gradle wrapper
  cache lock and the permitted retry succeeded. Android compilation, full
  app/generated DI builds and device runtime checks were not run for that repair.
  Independent source review, scoped `git diff --check` and Kotlin iOS Simulator
  compilation of authorization and shared passed for the route cleanup that
  removed `AuthorizationEntryPoint`; metadata-only compile tasks were skipped and
  do not count as compilation evidence.
- One-time onboarding and Login cleanup (2026-10-14, superseded
  destination-carrying revision): source/diff self-review, screen and
  preservation gates (seven MVI files, typed route/factory/animation wiring, 24
  independent screen hooks, null containers, shown-source gate, readiness/error/
  retry guards, durable cleanup retention, cancellation boundaries and the exact
  Login button source), EN/UK/RU resource XML/key parity, scoped authorization
  `compileAndroidMain --offline` and `git diff --check` passed. No compilation of
  the full app, rendering, native restoration, SDK sends or automated tests were
  run in that pass, and its independent review was unperformed.
- Onboarding welcome revision (2026-10-14): source/diff, screen ownership,
  durable-state/error/cancellation gates and EN/UK/RU XML/key parity passed;
  authorization and local-settings Android and iOS Simulator Kotlin compilation
  passed. Nine strings were added in EN/UK/RU; screen previews covered on/off,
  preparation, writing, light/dark EN/UK phone configurations; Android Studio
  rendered the standard full-screen EN dark and UK light previews before the
  compact-footer follow-up. The visual-only follow-up that removed the consent
  card and long description changed no persistence, control eligibility, screen
  hook or event contract. That revision reused the design-system `Check`
  component for the consent checkbox. The current screen composition is recorded
  in the surface table; detailed enlarged-text, interactive toggle, process
  recreation, live app and iOS visual checks were unverified in that pass.
- Catalog revision (the revision the current code matches): Splash requests no
  catalog, Login/Register refresh muscles and equipment before the credential
  request, Home's startup operation became `refresh_catalogs`. On the final tree
  `./gradlew :androidApp:assembleDebug` and
  `./gradlew :shared:assembleSharedDebugXCFramework` passed and the independent code
  review passed; the gate audit ran on the snapshot before the rename. Runtime
  behavior (offline cold start, sign-in, ProfileCreation after a restart) was not
  run.
- Sequential startup revision (2026-10-14; Splash's catalog requests were removed
  later): source checks covered three destinations with and without onboarding,
  captured route restoration inputs, accepted/busy callbacks, replacement
  callbacks, obsolete wiring/dependencies, leaf/container hooks and unchanged
  storage/error ownership. Authorization Android and iOS Simulator Kotlin
  compilation passed; `git diff --check` passed;
  `:shared:assembleSharedDebugXCFramework --offline` succeeded for device and
  simulator (6m 46s). The later product clarification that removed the onboarding
  destination and restored the shown-first gate passed Android and iOS Simulator
  compilation; full XCFramework linking and device restoration were not repeated
  after it. Compilation and linking do not prove device navigation, restoration
  or SDK delivery. Independent review was unavailable under the delegation
  restrictions of those sessions; no automated test sources or infrastructure
  were added.

Manual scenarios still unrun: first launch, then restart before and after Get
started; a saved Onboarding route; logged-in startup; Back from onboarding;
read/write errors and dialog dismissal; logout/delete retention of
`onboarding_shown` and `analytics_consent`; login providers and provider
cancellation; queued deep links; small/large-font CTA and insets on both
platforms; device archive roundtrip for the seven drafts; payload delivery;
lifecycle timing of Initial/Restore presentations; reporting-failure injection.
Source checks do not prove runtime behavior.
