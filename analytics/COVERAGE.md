# Illustration integration review (2026-10-14)

Subsequent presentation refactor: Illustration owns decorative image layout and
three existing token sizes; IllustratedEmptyState also accepts an optional title
with supporting text. All eight surfaces use the shared image component. Assets,
business callbacks, telemetry and user-owned state are unchanged.

Visual-only scope: onboarding, daily training list, exercise library, recording
exercise list, start-training empty option, empty sets, Home welcome and error
dialog. Five shared WebP assets have eight production consumers. Resource reuse,
decorative accessibility, token-owned sizes and removed icon/component callers
were reviewed. No screen identities, callbacks, business operations, consent,
navigation or user-owned fields changed; no new events, admin synchronization or
StateKeeper data is needed. Error dialog keeps a simple non-scrolling illustration/message/button layout
at the owner's request; exceptional long-message/compact-screen adaptation is
outside the requested scope. Additional compact/font-scale and long-message preview fixtures were removed
at the owner's request; existing standard previews remain. They are not rendered
verification.
Asset alpha, unique file contents, live source references and project-skill links
were checked. Whole-screen layout/runtime and iOS rendering remain unverified.
Independent review was not performed under the current delegation restrictions.

# Onboarding illustration and visual polish (2026-10-14)

Visual-only change: generated transparent dumbbell/progress artwork is bundled as
onboarding_hero.webp (768px, about 104KB), with a token-owned warm gradient in both
LightColor/DarkColor, a smaller logo and compact accent benefit icons. Existing
localized title/descriptions, checkbox/CTA, busy/source eligibility, callbacks,
LocalSettings, navigation, requests and analytics hooks remain unchanged. No
event/consent/state impact, new admin object or live synchronization needed.
Screen previews retain consent on/off/preparing/saving, light/dark and UK/EN
configurations; Intro/Benefit now have their own AppPreview fixtures.
Generated artwork and alpha/export were inspected. Source/diff review passed;
initial Android/iOS Simulator compile passed. Native Studio whole-screen preview
attempts were interrupted by user app changes; device/font-scale/layout rendering
remains unverified. No test/capture infrastructure or runtime state added.
Final Android compilation and shared debug XCFramework assembly passed (device
and simulator, 10m55s). The first full build failed on a missing transformed KLIB
in confirm-training-completion; only that generated build cache was regenerated
before the successful retry. No unrelated source/infrastructure repair performed.
Independent review was unperformed under the current delegation restrictions.

# Startup analytics admin follow-up (2026-10-14)

Scope: reconcile the sequential Splash/onboarding continuation and removal of
exercise-example preloads from Login/Register/CreateProfile with live reporting.
No app instrumentation change was needed. The preceding implementation's source
checks/build evidence remains scoped below; this follow-up changed admin settings
and this coverage record only.

Property 519002689/account 379805710 was inspected in native Chrome. The previously
verified mobile mappings remain Android stream 13265245917/com.grippo.android and
iOS stream 13348167981/com.grippo.app; web stream 13688917680 is outside app reports.

| Emitting contract | Admin consumer / decision |
|---|---|
| `home_task_started/completed/failed/interrupted`, `home_operation` = `refresh_trainings/refresh_exercise_examples/save_period` | Added bounded Event-scoped `Grippo Home operation` (`home_operation`) and a thirteenth primary selector to existing diagnostic report `События и экраны` (16123956244). Refresh failures can now be separated by their actual Home owner. No new event, key mark or derived success rule. |
| `auth_session_check_*` | Retained raw event-name diagnostics. The existing decorator measures `hasProfile` only after the shown-onboarding gate and with a token. The latest product rule always routes first onboarding to Login; no session check occurs before it. Collection still requires granted consent; completion does not prove successful navigation. No destination/provenance dimension. |
| `auth_attempt_started/succeeded/failed/interrupted` with `auth_action/auth_method`, `profile_operation_*` with `profile_action` | Existing bounded definitions remain accurate for complete revised caller blocks. Exercise failures now belong to Home, so old test builds' auth/profile failure populations and durations are not an unchanged baseline. |
| `auth_control_clicked`, Onboarding `next/dismiss_requested`; built-in screen views | Existing interaction/screen definitions retained. Checkbox/default writes deliberately emit no event; navigation changes add no synthetic click/view. Leaf and container hook inventory is unchanged from the sequential-startup record below. |
| `profile_operation_completed` + `profile_action=create` → `grippo_profile_created` | Existing exact mobile-derived rules/key-event counting retained: completion of setup/readbacks/local status, not exercise-cache readiness or unique account/entity creation. Existing `Профиль и тренировки` and observed funnel descriptions still express that contract. |

Read-back: reopened the saved Home definition and verified name, description,
Event scope and immutable `home_operation` parameter. Reopened report 16123956244
after saving and verified Home selector, original twelve selectors, Event name
default, Event count/Total users and `Platform matches regex '^(Android|iOS)$'`.
The existing published `Grippo — главное` collection remains the report consumer.
Report-data saving cleared its Library description; restored and updated it, then
reopened Rename to verify the complete saved text: event/user units, failure versus
interruption, possible already-created profile, operation selectors, separate Home
exercise refresh, the shown-onboarding SessionCheck gate, older-build semantic boundary
and screen/SDK context. No raw history, legacy report, rule or key event was removed.

Current unreleased catalog remains v1 by the owner's decision. Historical personal
test events are preserved, not relabelled or treated as current SDK delivery.
New custom-dimension registration does not establish historical backfill; actual
parameter processing/receipt from the current Android/iOS build remains unverified.
No synthetic sends, SDK sign-in or release deployment performed. The original admin-only follow-up had no state/UI impact. Subsequent product
clarification removed onboarding destination and restored the shown-first gate;
its Android/iOS Simulator compilation passed. Full XCFramework linking and device
restoration were not repeated after that clarification.
Source/mapping self-review and `git diff --check` passed. Independent review remains
unperformed under the current delegation restrictions.

---

# Analytics flow follow-up audit (2026-10-14)

Scope: current feature event/callback matrices, operation decorators and complete
business contracts, screen lifecycle coverage, schema/DI catalogs, installation
consent and platform SDK attachment, plus the new one-time onboarding/Login cleanup.
Historical reports/builds below are evidence only for their original revisions.

Source/diff self-review found and corrected one current consistency defect:
Profile Settings used Material Switch instead of the existing project Toggle.
Toggle now accepts optional enabled/contentDescription inputs, retains enabled=true
for existing callers, owns native Switch/toggleable-state/disabled semantics and
uses the existing disabled color tokens. Hardcoded English On/Off state descriptions
were removed. Settings retains its previous source-ready/write-loader eligibility
and direct durable-source rendering. No event population, wire schema, account
operation or Login button layout was changed by this repair.

The repeated source audit covers all 182 public event variants: each has a real UI
reference and an implementation mapping, with no unused observation variant or
open raw-text/error fact. Checks passed across 204 direct observation sites and
47 operation call sites: 149 literal schemas have unique valid bounded names and v1;
30 feature implementation modules are registered once with unique schema qualifiers;
implementation visibility/lifetimes/dependencies and event-failure isolation were
reviewed. All 24 typed screen identities have exactly one leaf owner; nine real
containers keep null. Operation adapters expose started/completed/failed/interrupted
without raw errors; complete auth/profile/training caller contracts were traced,
including Result unwraps and nullable/Boolean outcomes. Current operations omit
mutable navigation attribution and preserve business results/errors/cancellation.
No raw credential/body/entity/error fields or obsolete Back/provenance API facts
were found. Literal schema/context parameter budgets and git diff --check passed.

Consent starts denied on absence or read failure. The app-lifetime source controls
core collection; pre-consent events are discarded, never replayed. Android/iOS
startup flags and adapters disable automatic screen tracking and deny advertising
consents. Revision/queue/configuration checks, stale presentation close and worker
cancellation/error boundaries were inspected in real source. Adapter acceptance
is not remote delivery; early SDK scheduling remains a runtime check.

Explicit limitations remain: launch_source has no actual caller and stays unknown;
it is not interpreted as attribution or registered as a reporting dimension.
Dialog host exits/views and undocumented provenance stay explicit omissions, not
inferred completions. Training create completion can return a nullable ID; goal
operation return can be false; neither is relabelled as a proven business success.
Onboarding Get started/display flags do not themselves grant consent. First entry
now initializes absent consent to true immediately, per the product decision below;
saved false is preserved. Initial events before that source update remain discarded.
Shown-flag policy remains entry-based, not completion-based.

The repeated audit inspected live property 519002689/account 379805710 and matched
both mobile streams' package/bundle and Firebase app IDs to local configuration.
Both derived rules retain exact event + operation/action conditions and copied
parameters; both product key events use once-per-event counting with no monetary
default. Neither mobile stream has event modification rules. All eleven Grippo
event dimensions and the event-scoped Milliseconds metric were inspected; IDs and
unused attribution remain unregistered. Saved audiences/segments have no custom
event dependencies. The app comparison is exactly iOS OR Android. The only data
filter is Internal Traffic / Exclude / Testing; no active exclusion was listed.
Personal test tagging is not established by that configuration.

One admin semantic defect was corrected: the auth_action description claimed
login meant an existing account. It now describes the invoked email/provider
login versus email registration scenario without inferring account novelty.
The saved description, Event scope and unchanged parameter were re-opened and
verified. No wire event, schema, collection policy or raw history changed.

Both published detail reports retain their app-only filters, Event count / Total
users and source-accurate Library descriptions. The diagnostic report's twelve
primary selectors include the bounded operation/action dimensions. The closed
Explore funnel has exact profile-setup-completed → training-create-completed
steps, an enabled seven-day indirect sequence and web exclusion. Its diagnostic
tab retains Event name rows, App version columns, both metrics and all three
AND filters (create_training, Android/iOS, four operation states); saved settings
were checked again after reload. Legacy copies remain labelled and unassigned;
no deletion was justified or performed. The detailed consumer mapping remains in
[Live GA4 setup review](#live-ga4-setup-review--2026-10-14).

Onboarding reuses existing screen/control definitions; no new admin object is
required. Configuration reconciliation is complete for this scope. New derived
events have no received data in the inspected historical period; neither empty
reports nor legacy test traffic verifies current SDK delivery. Default-denied
startup discards events before onboarding initializes absent consent; saved denial
still suppresses events. No retrospective replay is claimed.

No compilation, generated DI/runtime, rendered widget, native restoration, provider
sign-in, reporting-failure injection or remote event-delivery scenarios were run.
No automated test infrastructure was added. Independent review is unperformed:
no permitted reviewer was available under the current delegation restrictions.
Toggle previews include enabled/disabled on/off; material UI fidelity/contrast
is unverified. Login authentication button source was compared without edits.

---

# Sequential authorization startup (2026-10-14)

Latest product clarification: onboarding is the one-time first screen and always
continues to Login, including Back. Splash loads muscles/equipment, then its flat
resolveDirection checks onboarding_shown first. If shown: no token → AuthProcess;
token + profile → Home; token + no profile → ProfileCreation. SessionCheck therefore
runs only with a token after onboarding has already been shown. No generic Continue,
Destination enum/payload, cross-leaf dispatcher or onboarding session requests remain.
SessionCheck's decorator now lives inside hasProfile; it still encloses the same
cached-user read and optional getUser/getOrThrow call, once per invocation. Its
eligibility, result/error/cancellation and event population remain unchanged;
this extraction needs no admin change or additional saved state.
AuthProcess/ProfileCreation replace the previous authorization stage; Root replaces
authorization with Home. Login↔Registration keeps ordinary push/pop. Retry Splash
may repeat its successful earlier catalog requests; no additional caching was added.

Exercise-example first loading belongs to HomeViewModel. LoginUseCase,
RegisterUseCase and CreateProfileUseCase no longer request exercise examples or
inject that Feature; their three Koin factories are reconciled. Required profile,
exclusion, weight-history, goal and push-token steps retain their original error
contracts. Home's RefreshExerciseExamples still decorates its actual API request.
Later workout/library operations keep their independently needed refreshes.
Home training presentation observes Room's exercise_example/iteration/exercise/
training tables (confirmed in generated TrainingDao); arrival of examples rebuilds
that data-backed presentation rather than requiring an auth preload.

Screen coverage: SplashComponent/VM → AuthScreen.Splash; OnboardingComponent/VM
→ AuthScreen.Onboarding; LoginComponent/VM → AuthScreen.Login. Each retains one
foundation presentation hook. Auth/AuthProcess/Root/ProfileCreation hosts remain
containers with null hooks; profile-creation leaves keep their existing hooks.
Get started/Back still observe next/dismiss_requested with original source/busy
guards. SessionCheck still measures hasProfile only, and now runs after the shown
onboarding gate when a token exists. Consent still controls event eligibility;
no suppressed earlier operation/view is replayed after the onboarding default grant.
Login/Register/CreateProfile decorators keep their whole revised business blocks;
a failed exercise refresh now belongs to Home, not auth/profile creation. Durations
and failure populations across older installed builds are not an unchanged baseline.

Preservation inventory: AuthRouter.Onboarding is again a payload-free serializable
object with its original serial name and unchanged AuthComponent stack key.
JsonIgnoreUnknownKeys is scoped to that object for the intermediate development
route's obsolete destination field; no compatibility business field or secret is
retained. Restored onboarding routes stay interactive and always continue to Login.
Consent rebuilds from LocalSettings, with no duplicate StateKeeper copy. Existing
saved false remains protected by atomic default initialization. Device archive
roundtrip remains unverified; Android/iOS Simulator compilation passed after removal.

Admin reconciliation: fresh native Chrome readback on property
519002689/account 379805710 confirmed existing Library descriptions for События и
экраны and Профиль и тренировки describe observed views/events and complete current
operation blocks, without claiming exercise-cache readiness or an explicit opt-in.
Profile creation still includes its required data readbacks/local welcome status.
Existing screen/control definitions and key events need no new rule/mark/report;
no schema, admin mutation or deletion was introduced in that implementation slice.
The subsequent Home reporting addition is recorded in Startup analytics admin
follow-up above. Current schemas remain v1.
SDK receipt and deployment are unverified; older builds retain their own startup
and auth-preparation populations, with no invented release cutoff or raw-data rewrite.

Source checks cover three destinations with/without onboarding, captured route
restoration inputs, accepted/busy callbacks, replacement callbacks, obsolete
wiring/dependencies, leaf/container hooks and unchanged storage/error ownership.
Authorization Android and iOS Simulator Kotlin compilation passed; git diff --check
passed. :shared:assembleSharedDebugXCFramework --offline also succeeded for device
and simulator (6m 46s), producing shared.xcframework. Compilation/linking does not
prove device navigation, restoration or SDK delivery.
Independent review is unavailable under the current delegation restrictions.
No automated test sources or infrastructure added.

# Onboarding welcome and immediate analytics preference (2026-10-14)

Product decision: the checkbox is on by default, saved immediately on first entry
and on every accepted change. This is opt-out behavior, not confirmation through
Get started. LocalSettings.setDefaultAnalyticsConsent writes only when its Boolean
key is absent, within DataStore.edit; known grants and denials survive first entry,
retry, restoration and account cleanup. The shown flag stays entry-based. Already
shown saved Onboarding routes rebuild their source and remain interactive instead
of redirecting before the choice is available. Ordinary cold starts still use the
Splash shown-state gate. No backend, SDK-default or analytics schema change.

Coverage: AuthRouter.Onboarding → OnboardingComponent → retained OnboardingViewModel
→ OnboardingScreen retains exactly one AuthScreen.Onboarding presentation. Splash,
Login and their Auth/Root containers keep their existing hooks/null classification.
Get started/Back still observe next/dismiss_requested through auth_control_clicked;
both require a known source and no loaders. Checkbox changes reject
pending, unchanged and busy input, persist via safeLaunch/getOrThrow and deliberately
emit no event. Initialization is also deliberately untracked. Collection is applied
by the existing ApplicationBootstrap observer; pre-grant observations are discarded,
never replayed or turned into a synthetic screen view. Delivery timing after a
saved preference remains an asynchronous core/SDK concern, not a UI guarantee.

Preservation inventory: analyticsConsent is nullable rebuilt presentation of an
immediately committed installation preference, not an unfinished draft. Preparation
is represented by its loader; there is no separate readiness flag or StateKeeper copy. Jobs/loaders,
error sessions and presentation provenance are runtime-only; no secret inputs added.
Observation uses Processing.Infinity; source retry clears pending presentation.
A failed write retains the actual stored value, and a failed initial write prevents
marking onboarding shown or continuing. Cancellation is rethrown at storage boundaries.

UI uses existing AppTokens, GrippoLogo, Check and Primary Button, with scrollable
welcome/benefit content and a pinned compact checkbox/text row immediately above
the continuation action. Nine new strings
are translated in EN/UK/RU. Screen previews cover on/off, preparation, writing,
light/dark EN/UK phone configurations.
Visual-only follow-up removes the consent card and long description; persistence,
control eligibility, screen hooks and event contracts are unchanged. No analytics
admin or state-preservation change is required for this layout/text-only edit.
Before the compact follow-up, Android Studio rendered the standard full-screen
EN dark and UK light previews; only their overview was inspected. The compact
footer revision and detailed enlarged-text, interactive toggle,
process recreation, live app and iOS visual checks remain unverified.

Live admin readback: property 519002689/account 379805710; Android stream
13265245917 (com.grippo.android), iOS stream 13348167981 (com.grippo.app).
Existing Event definitions screen_id, interaction_screen and interaction_action
match auth_onboarding/next/dismiss_requested. Key events remain
Grippo profile-created and training-create-completed; onboarding/consent requires
no key mark or derived event. Library reports События и экраны, Профиль и тренировки
and the saved profile→training path retain source-accurate descriptions and explain
permitted analytics/GA4 users/test history. None claims an explicit opt-in event or
requires a consent parameter; no affected admin object needs a mutation or deletion.
No admin configuration was changed. This reconciliation does not prove delivery of
the new revision. Old installed builds/historical self-test traffic keep their old
collection population; do not compare before/after consent policy as an unchanged
baseline or invent a release cutoff. Current schemas remain v1.

Checks: source/diff, screen ownership, durable-state/error/cancellation gates and
EN/UK/RU XML/key parity passed. authorization and local-settings Android and iOS
Simulator Kotlin compilation passed. For that welcome-only revision, full shared XCFramework linking, device
restoration, SDK collection/delivery and independent review were unperformed;
the later routing revision above now has successful XCFramework evidence, while
the device/delivery/independent-review limitations remain;
no permitted independent reviewer is available under delegation restrictions.
No automated test sources, dependencies or capture infrastructure added.

# One-time onboarding and Login cleanup (2026-10-14)

## Route and behavior coverage

| Surface/route | Owner | Screen identity/hook |
|---|---|---|
| RootRouter.Auth(AuthRouter.Splash), startup loading | SplashComponent → retained SplashViewModel | AuthScreen.Splash / auth_splash; one hook |
| AuthRouter.Onboarding, welcome content + analytics checkbox + Get started | OnboardingComponent → retained OnboardingViewModel | AuthScreen.Onboarding / auth_onboarding; one hook |
| AuthProcessRouter.Login, Email/Google/Apple/Registration callbacks | LoginComponent → retained LoginViewModel | AuthScreen.Login / auth_login; existing hook |
| Auth and AuthProcess hosts, Root host | Existing container VMs | Default null; child pages own views |

Splash loads muscles and equipment sequentially and resolves Login/ProfileCreation/
Home from the token/profile before reading observeOnboardingShown().first().
False/missing directs to Onboarding carrying that destination; true continues
directly to the resolved screen. Auth replaces its stack rather than pushing the
placeholder into Back history. Root's token-null observer does not replace an
already-active Authorization child; queued deeplinks still resolve at Home.

Catalog ordering update: every Splash initialization/retry loads both catalogs.
Onboarding now continues directly to its saved destination, so it does not recreate Splash or
repeat catalog loading. Errors propagate through the existing
safeLaunch dialog/retry before any onward navigation. Analytics contracts, hooks,
callbacks and the hasProfile-only SessionCheck block are unchanged; catalog work
remains untracked and requires no admin definition change. Preservation assessment:
Splash has no user draft, catalogs stay Room-backed and loaders/jobs stay runtime-only.
Source ordering/error/screen ownership review, scoped authorization compileAndroidMain
with --offline and git diff --check passed. Device startup/error/retry and iOS
runtime remain unverified; independent review was unavailable under delegation restrictions.

Onboarding reads the durable shown flag during preparation. On first entry it
initializes absent analytics consent to true through an atomic conditional
LocalSettings write, then marks onboarding shown. Existing saved true/false values
are preserved. Shown remains an entry-based flag: closing/restarting after its
successful write does not show onboarding again. A restored Onboarding route stays
on its screen and rebuilds its checkbox from LocalSettings. Initialization failure
uses safeLaunch retry after dismissal; consent stays pending until preparation
succeeds. Cancellation propagates.

The checkbox and Get started/Back require a non-null source after successful preparation
and no active loaders. Accepted changed checkbox values are saved immediately through
safeLaunch; the Infinity source observation supplies actual display state. Consent
controls/initialization emit no analytics event. Failed source reads clear the
presentation to null before retry and disable dependent controls. Existing shared
ApplicationBootstrap applies the saved choice to collection; UI never calls the SDK.

Accepted Get started emits AuthEvent.Onboarding.GetStarted, encoded by the existing
v1 auth_control_clicked schema with interaction_screen=auth_onboarding and
interaction_action=next, then continues to the saved Login/Home/ProfileCreation
destination. Back observes DismissRequested
(encoded dismiss_requested) and follows the same continuation. Busy/not-ready
callbacks emit nothing. Presentation uses only the existing foundation hook.
No persistence operation telemetry or new wire schema is added; permission still
controls actual collection. Get started is continuation, not an opt-in event;
collection follows the separately saved installation preference.

Login's consent UI, callback, observation, injection, loader, transient field and
localized label were removed. Its authentication button-group source remains
identical to the pre-change copy. Existing email LoginDraft ownership, password
exclusion and old-draft compatibility remain. Check stays a reusable design-system
component from the earlier explicit request and is now reused in onboarding.

## Preservation inventory

| Owner | Field/group | Policy / source |
|---|---|---|
| LocalSettingsRepository | onboarding_shown | Durable installation Boolean; absent false, one-way true write; clearUserSettings preserves it atomically with analytics_consent across logout/delete |
| LocalSettingsRepository | analytics_consent | Durable installation Boolean; first onboarding initializes absent true only, existing true/false preserved atomically; accepted checkbox changes persist immediately |
| OnboardingState | analyticsConsent | Rebuild nullable durable consent through Infinity observation after preparation; no readiness flag, unfinished draft or StateKeeper copy |
| Onboarding | content, jobs/loaders, error/Back sessions | Localized welcome/benefit/consent content; jobs/loaders/errors remain runtime-only; no secrets or restoration flags |
| AuthRouter/stack | Onboarding page identity / destination | Serializable route and closed continuation enum; existing keyed stack/serializer; old missing destination defaults to Login |
| Splash | shown-state read and existing loading | Rebuild through existing VM startup/error lifecycle; no source cache or draft |
| LoginState | email / password / provider availability | Existing email draft retained; password excluded, availability rebuilt; removed consent presentation is not archived |

Scope is one existing feature module with seven new MVI files, no new module, DI
registry or startup shell. LocalSettings keeps its API/repository/feature ownership.
EN/UK/RU Get started resources and ready/preparation previews are supplied. There
is an immediately saved consent choice in onboarding, editable later in Settings.
Display-only one-time semantics intentionally do not promise
resuming a future unfinished multi-step onboarding.

## Verification

Source/diff self-review, screen and preservation source gates passed: seven MVI
files, typed route/factory/animation wiring, 24 independent screen hooks, null
containers, shown-source gate, current readiness/error/retry guards, durable
cleanup retention, cancellation boundaries and exact Login button source were
inspected. EN/UK/RU resource XML/key parity and git diff --check passed.
Affected admin configuration was subsequently reconciled in the
[Live GA4 setup review](#live-ga4-setup-review--2026-10-14); existing definitions
cover the onboarding values. SDK receipt remains unverified. No compilation,
rendering, native restoration, SDK sends/sign-ins or automated tests were run in
this original source pass. Its independent review is unperformed: delegation
restrictions then permitted no independent reviewer. Manual scenarios: first launch then
restart before/after Get started, saved Onboarding route, logged-in startup, Back,
read/write errors and dialog dismissal, logout/delete retention, login providers,
queued deep links and small/large-font CTA/insets on both platforms.

During the original source pass, native Chrome inspected Grippo property 519002689/account 379805710,
Firebase project name grippo-e3960, existing observed profile→training funnel and
its Android/iOS filter. Local app configs match project grippo-e3960. Dedicated
Chrome browser binding is unavailable; native automation follows the active window.
A separate tab and window were attempted, but parallel editing switched the target.
No admin object was written or removed in that pass; its source-only scope/admin
isolation decision was then pending. The subsequent authorized
[Live GA4 setup review](#live-ga4-setup-review--2026-10-14) completed the affected
saved-configuration reconciliation. SDK delivery and the original widget/runtime
checks remain unverified; this does not imply they were performed by the source pass.

---

# Current analytics contract review (2026-10-14)

This section defines the current changes; dated build/review records below remain
historical evidence for their original revisions, not verification of this revision.

## Collection policy and Settings

Collection starts disabled until a saved grant is applied. Onboarding now initializes
absent consent to true on entry; known false is preserved. `analytics_consent` is a Boolean preference in the
existing LocalSettings/DataStore owner; absence and saved false map to Denied. ApplicationBootstrap
owns one application-lifetime observation before platform sink attachment. It applies
Granted/Denied through Analytics; read failures deny collection while the existing
preference observation is retried with a delay increasing from one to thirty seconds. Platform shells still initialize
SDK collection as Unknown before attachment. No feature enables SDK collection. Events observed before consent is known are
discarded, not buffered/replayed; granting consent does not synthesize a screen view.

ProfileRouter.Settings → ProfileSettingsComponent → retained ProfileSettingsViewModel
→ ProfileSettingsScreen retains ProfileScreen.Settings / profile_settings and its
single foundation hook. The project Toggle uses AppTokens colors, an accessible label, scrollable Settings content
and title/description in EN/UK/RU. Its nullable
presentation waits for the durable source; unknown/pending writes disable the control.
onAnalyticsConsentChange accepts changed values only after source acquisition and
outside AnalyticsConsent loader, then saves with getOrThrow inside safeLaunch.
Write failures follow the normal error pipeline; a failed source subscription
restarts only after its error dialog is dismissed and keeps the switch disabled
until the source is known again. Consent actions deliberately emit no
tracking event: denial must not require or report a consent-control interaction.
Existing Settings Logout/Delete/Back coverage remains unchanged.

Preservation inventory: consent is immediately committed to the existing durable
owner, not an unfinished form; Settings rebuilds it through an Infinity observation.
No StateKeeper copy or restore cache is needed. LogoutUseCase/DeleteProfileUseCase call clearUserSettings, atomically preserving
this installation's saved consent while clearing account preferences. New installs
remain disabled until first onboarding initialization; its default grant then applies. Jobs/loaders/focus, telemetry IDs and live dialogs are runtime-only.

## Screens and central actions

[Debug](debug/COVERAGE.md) now supplies the missing independent screen hook.
[Main](main/COVERAGE.md) observes actual central-button and tab callbacks and keeps
the null container hook. Source inventory: 24 independent screen VMs have one hook;
nine graph/container VMs keep null. Shared root/dialog hosts remain containers.

## Reporting decision and compatibility

Operation events identify their owner through feature-specific operation/action
codes and correlate outcomes by operation_id. They now omit visible-screen fields
(screen_id, navigation_reason, referrer_screen, dialog_id), because construction,
background work and pause/navigation do not establish operation ownership. The user
confirmed that the app has never been released: every analytics schema is reset to
initial version 1. No released-report compatibility or migration is required.
No substitute origin, referrer, saved provenance or reporting context propagation
is introduced.

Unused FlowContext/EntryPoint/toolkit FlowId and Action/Back/External reasons are
removed from the API. Unused attribution names remain reserved against accidental custom reuse. The distinct
screen-api FlowId remains necessary for route identity/archive compatibility and is
unchanged. Built-in DialogViewed remains reusable; this change adds no dialog view API.

Typed screen/dialog dismissal observations and internal actions consistently use
DismissRequested. Their wire event names/action codes use dismiss_requested, including
former back/dismiss encodings. The two long details-dialog event names use bounded
prefixes: performance_trend_dismiss_requested and training_streak_dismiss_requested.
Business callbacks/Direction.Back, guards, confirmation decisions and navigation
remain independently necessary and unchanged. A request is never reported as a
delivered dismissal; host-managed exits remain explicit scoped omissions.
Click intents use Clicked; wheel state changes use SelectionChanged, including
programmatic correction. Parameter shapes and event populations are unchanged.

## Pre-release baseline follow-up (2026-10-14)

User decision: no app release exists. All 149 literal analytics schemas, including
built-ins and auth_input_changed, use version 1. Dismissal observations are renamed
across all public events, common ControlAction, internal actions/encoders and owning
VM callers; obsolete Back/back/dismiss analytics encodings are removed.
This is a telemetry-only contract cleanup: actual routes/screen hooks, business
callbacks/guards, input/selection/draft owners and navigation are unchanged. No
additional state archive or synthetic callback is introduced. Final source/diff checks
passed: all 149 schemas use version 1 with unique names bounded to 40 characters;
no Back/back/dismiss analytics models/codes or stale UI consumers remain. Comparing
the UI source before/after confirms that only typed analytics references changed.
`:shared:compileAndroidMain :shared:assembleSharedDebugXCFramework` passed offline
without configuration cache (exit 0, BUILD SUCCESSFUL, 3m 17s), including device and
simulator frameworks. No runtime SDK delivery or independent review was performed;
earlier build records below describe their original source revisions.

## Verification of the earlier review revision

Final source/diff gate passed: 23 independent hooks and nine null containers; all
30 feature analytics modules registered once; 149 unique, bounded literal schemas
including built-ins; interaction parameter shapes; default-deny/readiness/write guards
and cancellation propagation;
EN/UK/RU XML resources and disabled Android/iOS SDK defaults. Generated Koin output
confirms MainAnalytics/DebugAnalytics bindings and the unique main_analytics catalog.
The existing screen-api route FlowId/archive identity is preserved.

Initial integration found a missing explicit API type on DebugViewModel's new public
hook; it was corrected. The next Android/XCFramework integration passed. After final
UI colors/accessibility, source retry and internal schema-name cleanup, the exact final
source passed `:androidApp:assembleDebug :shared:assembleSharedDebugXCFramework`
with `--offline --no-configuration-cache` (exit 0, BUILD SUCCESSFUL, 1m 18s).
Both device and simulator debug frameworks were linked and packaged. Existing global
opt-in/deprecation warnings remain. This validates compilation/export, not SDK delivery.

No automated test infrastructure is introduced. Independent local review is unavailable
under this chat's agent restrictions; self-review is not independent review.
Manual scenarios remain unrun: opt-in/off across restart, settings write/read failure,
installation consent retention across logout/delete, native lifecycle/restoration, error/cancellation injection,
rendered Settings layout and actual SDK delivery. Build success alone does not prove them.

---

# Scoped Home and Training flows coverage

Reviewed during the project-skill cleanup on 2026-10-13. This is an inventory of
actual affected owners, not a template or a claim of global feature/SDK coverage.
Update it when these owners or their instrumentation change.

Start training dialog instrumentation and its runtime-session / accepted-parent-state
assessment are recorded in [Start training coverage](start-training/COVERAGE.md).

Goal setup suggestion actions and its no-draft assessment are recorded in
[Goal setup suggestion coverage](goal-setup-suggestion/COVERAGE.md).

Height picker dialog instrumentation and state assessment are recorded in
[Height picker coverage](height-picker/COVERAGE.md).

Date picker dialog instrumentation and its retained/accepted-state policy are
recorded in [Date picker coverage](date-picker/COVERAGE.md). It emits scoped submit
and dismiss callbacks without a dedicated dialog presentation contract.

Exercise example dialog instrumentation and its no-draft assessment are recorded
in [Exercise example coverage](exercise-example/COVERAGE.md).

Error display callback instrumentation and its runtime-only error/session policy
are recorded in [Error display coverage](error-display/COVERAGE.md).

Iteration picker interactions and accepted-parent-state preservation are recorded
in [Iteration picker coverage](iteration-picker/COVERAGE.md).

Muscle loading details dialog callbacks and its runtime-only selection policy are
recorded in [Muscle loading details coverage](muscle-loading-details/COVERAGE.md).

| Surface / route | Component → ViewModel | Presentation identity | Source coverage |
|---|---|---|---|
| Home dashboard (main Home tab) | HomeComponent → HomeViewModel | HomeScreen.Home / home | Typed VM hook, retained-factory injection, foundation lifecycle binding |
| Home graph | HomeRootComponent → HomeRootViewModel | Default null | Container renders child; no duplicate presentation |
| Training calendar, Daily/Monthly modes | TrainingsComponent → TrainingsViewModel | TrainingsScreen.Calendar / trainings_calendar | Typed VM hook; one owner for both display modes |
| Training calendar graph | TrainingsRootComponent → TrainingsRootViewModel | Default null | Container renders its child; no duplicate presentation |
| Training recording, Add/Edit/Draft entry | TrainingRecordingComponent → TrainingRecordingViewModel | TrainingScreen.Recording / training_recording | Typed VM hook; one screen for all modes |
| Training exercise editor | TrainingExerciseComponent → TrainingExerciseViewModel | TrainingScreen.Exercise / training_exercise | Typed VM hook; original navigation/result owner |
| Training completion, saving loader and summary | TrainingCompletedComponent → TrainingCompletedViewModel | TrainingScreen.Completed / training_completed | Typed VM hook; saved outcome ID and rebuilt presentation |
| Training graph | TrainingComponent → TrainingViewModel | Default null | Container renders stack; no duplicate presentation |
| DraftTraining, StartTraining, TrainingGoalDetails dialogs | Respective dialog VM | No dedicated presentation contract | Live dialog sessions remain outside independent-screen tracking |

The initial presentation integration established typed leaf hooks and null
container hooks. Foundation supplies Initial/Restore and closes on pause/destroy;
shared registers the generated annotated modules. Later scoped sections below
record the current behavioral instrumentation and additional affected surfaces.
Home now observes its controls and three operations; Training has its current
[expanded coverage](#training-analytics-and-recreation-preservation) for three screens,
36 interaction observations (including four completion-confirmation dialog observations) and seven operation kinds. TrainingsAnalytics now observes calendar controls and two operation kinds; its
[scoped coverage](#trainings-calendar-analytics-and-recreation-preservation) is below. No raw inputs, entity IDs, measurements,
credentials or exception payloads enter the feature telemetry.

## Preservation inventory

| Owner | Field/group | Policy | Source/key and restoration |
|---|---|---|---|
| HomeViewModel | Range/filter selection | Preserve in existing persistent owner; rebuild formatted date bounds | LocalSettings/DataStore `range`; initial Empty waits for the persisted selection, null resolves Last30Days; no duplicate StateKeeper owner |
| HomeViewModel | Dashboard, goals, adherence presentation | Rebuild | Existing Feature/UseCase flows and caller-provided current time |
| Trainings calendar | Daily/Monthly selection and selected date range | Preserve | Component DRAFT / TrainingsDraft; required date has no dynamic serializer default |
| Trainings calendar | Limitations, timelines, digest, weeks and localized labels | Rebuild | Current date limits and existing DAO/UseCase observations; transient presentation fields |
| TrainingRecording | stage, exercises/iteration IDs/order/unfinished values, startAt | Preserve | Component DRAFT / TrainingRecordingDraft, typed serializer; null exercises mean required initial data is pending, a nonnull empty list is a real empty draft |
| TrainingRecording | Catalogs and available muscle/example data | Rebuild | Transient fields repopulated by existing observations |
| TrainingExercise | Current ExerciseState, field formats, iterations/IDs/order | Preserve | Component DRAFT / TrainingExerciseDraft, typed serializer; local archive overrides route seed |
| TrainingExercise | Example/history/validation artifacts | Rebuild | Transient values recomputed from current sources and restored exercise |
| Dialogs | Unaccepted dialog inputs and sessions | Runtime only | Session/config stack remains unsaved |
| Owning recording screen | Accepted dialog/library/editor result | Preserve at parent | Existing result callback updates recording draft; fresh child uses accepted business inputs |
| All affected owners | Jobs, loaders, focus, error/dialog sessions and analytics provenance | Runtime only | Excluded from draft archive |

Recording restoration must resume pending source acquisition without overwriting
a loaded empty or edited draft; archive existence alone is insufficient. Fresh
Add initialization retains its existing business behavior. Exercise restoration
rebuilds validation and presentation without replaying edit callbacks. These
archives contain no auth secrets.

Accepted risk (explicit user decision): Add remains available before the initial
Edit/Draft source load finishes. Early Add can establish an empty draft and skip
the original source load. No guard or disabled state was added. This behavior is
retained by request, not a fixed finding or a preservation PASS.

## Verification limits

The follow-up review removed auth-specific response sanitation and request markers
by explicit user decision. Logging remains enabled in all build modes. This
transport change adds no screen, preserved input or analytics payload. The review
also removed redundant example snapshot IDs. Required exercise-example snapshots validate completely before
writing. Training list refresh updates valid records, retains cached incomplete
records and returns the shared Network.IncompleteResponse failure for incomplete hydration; missing IDs prevent range
deletion. Training set/update preserve the existing successful server-write outcome
when optional readback cannot hydrate. These data-boundary changes introduce no
new screen draft or event/operation payload. Statistics and StartTraining dialogs
remain runtime sessions; StartTraining continues its existing cached fallback.
Calendar presentation rebuilds do not restart requests for an unchanged range.

Source/diff and independent review cover hooks, route ownership, descriptor/key
wiring, transient fields, caller identity creation and cancellation boundaries.
Final authorized builds, rerun after adding IncompleteResponse and removing auth sanitation, passed on 2026-10-13: `:androidApp:assembleDebug` and
`:shared:assembleSharedDebugXCFramework` (device and simulator frameworks).
Fresh native process
restoration, pause/resume timing, actual request serialization and SDK presentation
delivery have not been exercised manually; successful compilation would not prove them.

Incomplete required hydration uses the shared Network.IncompleteResponse type and
standard ErrorProvider mapping. No new screen/state owner or event payload is added.

## Authorization analytics

Reviewed against actual routes, rendered surfaces, Components and VMs on
2026-10-13, with the current Onboarding route/callback follow-up reconciled on
2026-10-14. This section covers authorization and its profile-creation children;
the earlier build evidence does not verify the later Onboarding revision.

| Route / surface | Component → ViewModel | AuthScreen / wire identity | Hook |
|---|---|---|---|
| AuthRouter.Splash / launch loading | SplashComponent → SplashViewModel | Splash / auth_splash | Typed screenPresentation |
| AuthRouter.Onboarding / one-time welcome and consent | OnboardingComponent → OnboardingViewModel | Onboarding / auth_onboarding | Typed screenPresentation |
| AuthProcessRouter.Login | LoginComponent → LoginViewModel | Login / auth_login | Typed screenPresentation |
| AuthProcessRouter.Registration / optional email seed | RegistrationComponent → RegistrationViewModel | Registration / auth_registration | Typed screenPresentation |
| ProfileCreationRouter.User | UserComponent → UserViewModel | User / profile_creation_user | Typed screenPresentation |
| ProfileCreationRouter.Experience | ExperienceComponent → ExperienceViewModel | Experience / profile_creation_experience | Typed screenPresentation |
| ProfileCreationRouter.ExcludedMuscles | ExcludedMusclesComponent → ExcludedMusclesViewModel | ExcludedMuscles / profile_creation_excluded_muscles | Typed screenPresentation |
| ProfileCreationRouter.MissingEquipments | MissingEquipmentsComponent → MissingEquipmentsViewModel | MissingEquipments / profile_creation_missing_equipments | Typed screenPresentation |
| ProfileCreationRouter.Completed / profile creation loading | ProfileCompletedComponent → ProfileCompletedViewModel | Completed / profile_creation_completed | Typed screenPresentation |
| Auth graph | AuthComponent → AuthViewModel | Default null | Child stack host |
| AuthProcess graph | AuthProcessComponent → AuthProcessViewModel | Default null | Login/Registration stack host |
| ProfileCreation graph | ProfileCreationComponent → ProfileCreationViewModel | Default null | Child stack and shared progress chrome |

AuthProcess uses one Login owner for its actual entry paths. Its FlowId is route
identity, not telemetry; AuthorizationEntryPoint and its former reason codes are
no longer part of the current contract.
Splash loads catalogs and checks the session before navigating to Onboarding or
the selected Home/AuthProcess/ProfileCreation destination. Onboarding carries and
continues the same selected destination. Successful login or
registration can navigate to Home or ProfileCreation; these paths do not add
screen owners. Restored stacks use the same retained factories and typed hooks.
All nine leaf factories resolve AuthAnalytics only inside retainedInstance.
BaseComponent binds the VM hook before activation/navigation bridging; foundation
owns Initial/Restore and close on pause/destroy. Containers resolve no screen service.

| Owner | Observed callbacks / inputs | Complete operations and eligibility |
|---|---|---|
| SplashViewModel | Back | SessionCheck wraps hasProfile only when a session token exists; cache hit and remote lookup both complete normally; startup retry remains business-owned |
| OnboardingViewModel | Accepted Get started / Back → Next / DismissRequested | Both callbacks require a known consent source and no active loaders; preparation initializes absent consent and marks shown without persistence telemetry; restored routes rebuild saved consent; checkbox writes are deliberately unreported |
| LoginViewModel | Email/password status transitions, focus, email clear, Next/Done IME, password visibility, submit by Email/Google/Apple, Register, Back | Login includes the whole use case and provider sign-in for Google/Apple; submit follows busy guard and email/password validity for Email; Register follows busy guard |
| RegistrationViewModel | Email/password status transitions, focus, email clear, Next/Done IME, password visibility, Submit, Back | Register wraps createAccount including private preparation; submit follows busy and validity guards |
| UserViewModel | Name status transitions/focus/clear/Done; Height/Weight picker open and accepted confirmation; Next, Back | Logout wraps execute on accepted Back; picker open and Back follow busy guard; Next also requires valid name/weight/height |
| ExperienceViewModel | Experience selection, Next, Back | Next requires selected experience; no business operation |
| ExcludedMusclesViewModel | Group click, item toggle (selected flag/count), Next, Back | Catalog observations rebuild state without interaction events; no business operation |
| MissingEquipmentsViewModel | Group click, item toggle (selected flag/count), Next, Back | Catalog observations rebuild state without interaction events; no business operation |
| ProfileCompletedViewModel | Back | CreateProfile includes full use case and welcome-status write; absent experience navigates back without an operation; error navigation follows existing dismissal pipeline |
| Container VMs | No synthetic navigation/click facts | No analytics operations |

Text status events compare existing old/new validation status, omit equal statuses
(including Empty → Empty) and keep no cache. Clear is a separate callback from its
resulting status transition. Picker confirmations fire for each accepted result,
including unchanged values; opening and confirmation remain separate facts.
Selection counts describe selected/available items, not the complementary exclusion
list. Repeated group/experience clicks remain observed callbacks.

### Reporting decision

AuthEvent.User.PickerConfirmed encodes auth_picker_confirmed version 1 with
interaction_screen, input_field and input_status. Auth input_changed version 1
observes only existing text validation-status transitions for email/password/name.
Height/weight accepted picker results belong exclusively to auth_picker_confirmed.
The app has not been released, so no released reporting population requires a
production migration. Legacy self-test populations do exist and are retained
separately from the current operation definitions. Dismissal action codes use
dismiss_requested consistently.

### Omissions and privacy

No dedicated picker/dialog view or close contract, dismissal/provider-cancel reason,
error classifier, session-presence/profile-presence result, group/item ID, route
entry attribution or transition/funnel event is emitted. No-session catalog fetches
are not SessionCheck operations. Programmatic startup, retry, restoration and
navigation do not fabricate user clicks. Password has no clear button/callback.
Focus reports actual UI focus, including focus movement, without inferred intent.
Consent/identity/transport remain app/core-owned. Payloads contain only bounded
codes, booleans, counts and durations; no raw input/length, email/name, measurements,
credentials/tokens, entity IDs, exception messages, URLs or request/response bodies.

### State impact and verification

Authorization route cleanup: unused `AuthorizationEntryPoint` and its forwarding
through Root/Auth/Splash navigation are removed. AuthProcess keeps its existing
FlowId identity; its serializer ignores the obsolete entryPoint field in older
saved routes. No editable state, draft supplier, analytics event or screen hook
changes. Fresh routes still open the same destination. Source review covers the
affected Splash leaf and null Auth/AuthProcess/Root containers; device restoration
of an older archive remains unverified.
Independent source review and scoped `git diff --check` passed. Kotlin iOS Simulator
compilation passed for authorization and shared; metadata-only compile tasks were
skipped and do not count as compilation evidence.

The 2026-10-13 picker-encoding repair changed only internal event encoding/schema
and coverage documentation.
It introduces no screen, callback, editable field, navigation change or state owner.
UserViewModel still updates its accepted picker input before emitting the typed
confirmation; UserComponent saves name/weight/height through UserDraft and
UserState.serializer(). No telemetry state or restoration callback replay is added.
Existing LoginDraft/RegistrationDraft exclude passwords and provider availability;
profile parent draft, experience and catalog selections remain owned by their
existing local StateKeeper suppliers. No archive/schema migration is needed.

Source review for that 2026-10-13 repair covered eight leaf hooks, three null container hooks, retained
injection, catalog registration, actual callback eligibility, complete operation
blocks, reporting isolation and cancellation propagation. Static verification
checks the dedicated picker mapping, complete schema catalog, exhaustive encoding
with unchanged codes and diff whitespace. No automated test infrastructure was
introduced. Manual device restoration, payload delivery, lifecycle timing and
reporting-failure injection were not run; source checks do not prove runtime behavior.
That verification predates the Onboarding revision; the current nine-owner
inventory and callback eligibility are recorded above.

Authorization repair verification on 2026-10-13: `git diff --check` and inline
source invariants passed (preserved enum codes, dedicated picker schema/mapping/
catalog, eight unique leaf hooks and three null containers).
`:analytics:authorization:compileKotlinIosSimulatorArm64` passed, with unresolved
global opt-in marker warnings for libraries absent from this pure module. The first
sandboxed attempt could not write the Gradle wrapper cache lock; the permitted
retry completed successfully. Android compilation, full app/generated DI builds
and device runtime checks were not run for this repair.

## Profile analytics

Full profile-module instrumentation added on 2026-10-13 using the existing
ProfileAnalytics API/module and retained VM injection. Current routes and rendered
surfaces, rather than the enum alone, define the following screen inventory.

| Route / surface | Component → ViewModel | ProfileScreen / wire identity | Hook |
|---|---|---|---|
| ProfileRouter.Menu / main Profile tab | ProfileMenuComponent → ProfileMenuViewModel | Menu / profile_menu | Typed screenPresentation |
| ProfileRouter.Body / including WeightHistory deeplink | ProfileBodyComponent → ProfileBodyViewModel | Body / profile_body | Typed screenPresentation |
| ProfileRouter.Goal | ProfileGoalComponent → ProfileGoalViewModel | Goal / profile_goal | Typed screenPresentation |
| ProfileRouter.Experience | ProfileExperienceComponent → ProfileExperienceViewModel | Experience / profile_experience | Typed screenPresentation |
| ProfileRouter.Muscles | ProfileMusclesComponent → ProfileMusclesViewModel | Muscles / profile_muscles | Typed screenPresentation |
| ProfileRouter.Equipments | ProfileEquipmentsComponent → ProfileEquipmentsViewModel | Equipments / profile_equipments | Typed screenPresentation |
| ProfileRouter.Settings | ProfileSettingsComponent → ProfileSettingsViewModel | Settings / profile_settings | Typed screenPresentation |
| ProfileRouter.Social | ProfileSocialComponent → ProfileSocialViewModel | Social / profile_social | Typed screenPresentation |
| Profile graph / arbitrary initial ProfileRouter | ProfileComponent → ProfileViewModel | Default null | Child stack host; no duplicate presentation |
| Height/Weight/Date/PrimaryGoal/SecondaryGoal/Confirmation dialogs | Existing dialog components/VMs | No dedicated profile presentation contract | Runtime overlays; owning profile VM reports open and accepted result |

All eight screen owners inject the existing stateless service inside their normal
retainedInstance factory. Containers add neither binding nor service resolution.
Foundation alone binds screenPresentation, assigns Initial/Restore and closes on
pause/destroy. Alternate initial routes and restored child stacks use the same
owners. Profile's parent navigation and the main tab shell emit no fake menu/Back
clicks. Editor, Settings and Social system Back callbacks invoke their owning VMs;
Menu has no leaf Back control/event, so container stack handling remains unreported.

| Owner | Observed callbacks / payload facts | Complete operations |
|---|---|---|
| Menu | Profile/Settings menu target (Body, Goal, Experience, Muscles, Equipments, Settings, Social, Debug) | RefreshUser, RefreshGoal, RefreshMuscles, RefreshEquipments |
| Body | Height/Weight picker open; each accepted result with existing validation status; Apply, Back | RefreshUser, RefreshWeightHistory; SaveBody includes conditional weight/height writes, existing delay and reminder scheduling |
| Goal | Primary/Secondary/TargetDate picker open; each accepted result with presence/status; personalization toggle selected/count; Save, Back | RefreshGoal; SaveGoal wraps setGoal with getOrThrow |
| Experience | Selected bounded experience level, Apply, Back | RefreshUser; SaveExperience wraps setExperience with getOrThrow |
| Muscles | Group click; item toggle selected/count; Apply, Back | RefreshMuscles; SaveMuscles wraps setExcludedMuscles with getOrThrow |
| Equipments | Group click; item toggle selected/count; Apply, Back | RefreshEquipments; SaveEquipments wraps setExcludedEquipments with getOrThrow |
| Settings | Logout click; deletion confirmation open and accepted result as distinct facts; Back | Complete LogoutUseCase and DeleteProfileUseCase |
| Social | Link click/channel, returned any-URL-opened boolean/channel, Back | Existing synchronous URL fallback remains business-owned; no artificial suspend operation |

Eligibility is unchanged. Body Apply returns before intent/operation when weight,
height or user is absent. Experience Apply returns when no experience is selected.
Muscles/Equipments Apply return for empty catalogs. Goal constructs its request
inside the existing safeLaunch and emits Save only after required primary/target
values pass the existing returns. These rejected paths emit no accepted intent or
operation. Existing callbacks have no additional VM busy guards; UI loader behavior
and repeated callback acceptance are preserved. No analytics-driven validation,
retry, fallback, navigation, dispatcher or job is added.

Refresh operations wrap the original explicit startup requests, not ongoing data
observations. All Result-returning calls unwrap inside the decorator; direct-value
use cases retain their throwing contracts. Started/Completed/Failed/Interrupted
use the shared decorator's fresh OperationId and monotonic duration. Completed
means the whole caller block returned, including cached/false read outcomes or an
accepted Body Apply with no changed measurements; it does not infer a server write.
Navigation follows the block. Ordinary reporting Exceptions are isolated without
replacing results/original business exceptions; cancellation propagates and fatal
Errors remain fatal. Source review is not reporting-failure injection proof.

### Catalog and reporting decision

The single qualified profile_analytics schema provider registers nine interaction
schemas and four operation schemas through the existing generated module, already
included by shared. New custom schemas start at version 1: profile_control_clicked,
profile_menu_selected, profile_picker_opened, profile_picker_confirmed,
profile_experience_selected, profile_selection_changed,
profile_personalization_changed, profile_link_clicked, profile_link_result, and
profile_task_started/completed/failed/interrupted. The profile_task names avoid
colliding with authorization's existing profile_operation schemas. Existing built-in
screen schemas and all eight presentation wire identities are unchanged.
This is additive instrumentation with no historical behavioral-event backfill.

Public events are closed observations with direct typed facts; Body and Goal have
separate picker enums, menu targets never contain navigation routers, and operation
values are feature-owned. Wire strings and exhaustive encoders stay internal.
Picker confirmation is distinct from input-status change and emits even for an
unchanged accepted value. Primary/Secondary results are nonnull enum values and
therefore Valid; date/height/weight reuse their current formatter status.
Counts describe selected items/personalizations, not complementary excluded lists.

### Deliberate omissions and privacy

No raw measurement, date, name/email, input length, entity/group/item ID, URL,
credential, request/response body or exception message is sent. Goal categories and
personalization content are deliberately omitted; the latter can describe health
and lifestyle limitations. Only picker field/status and toggle selected/count are
observed. No dedicated dialog views/close/dismissal/cancel reason, error classifier,
referrer/entry attribution, transition/funnel event or navigation-generated click
is added. Scroll/chart gestures and internal unaccepted dialog choices have no
profile Contract callback and remain outside owning-screen observations. Debug's
entry click is covered; the destination is outside the profile module.
Consent, identity and SDK transport remain app/core-owned.

### Preservation inventory and impact

| Owner | Field/group | Policy | Existing key/source and restore behavior |
|---|---|---|---|
| Body | Height/weight formats, including accepted picker results | Preserve | ProfileBodyDraft / ProfileBodyState.serializer; saved state wins over initial user values |
| Body | User/history | Rebuild | Transient fields repopulated from UserFeature/WeightHistoryFeature observations |
| Goal | Primary/secondary goals, target formatter, personalization selection | Preserve | ProfileGoalDraft / ProfileGoalState.serializer; restore before fresh defaults/one-time source initialization |
| Goal | Date limitations/personalization catalog | Rebuild | Transient current limitations and enum suggestions |
| Experience | Selected experience | Preserve | ProfileExperienceDraft / ProfileExperienceState.serializer; source seed only without archive |
| Muscles | Group, known/selected IDs | Preserve | ProfileMusclesDraft / ProfileMusclesState.serializer; empty catalog retains selection intent |
| Equipments | Group, known/selected IDs | Preserve | ProfileEquipmentsDraft / ProfileEquipmentsState.serializer; empty catalog retains selection intent |
| Muscles/Equipments | Suggestions | Rebuild | Transient catalog fields; current feature observations |
| Menu | User/goal/exclusion summaries | Rebuild | Current feature observations; no editable draft |
| Settings/Social/Profile container | No editable draft | Runtime only | Existing stateless screen/container state |
| All owners | Dialogs, focus, loaders/jobs and telemetry provenance | Runtime only | No new snapshot, key or analytics retention |

This implementation changes no State, Component, route seed, serializer or archive
address. Accepted picker values still update the owning draft before reporting;
restoration/source initialization never replays those callbacks. Local suppliers
remain the latest source for archive saves. Popped editors rebuild from persistent
business sources on re-entry; profile has no multi-step parent draft to synchronize.
The preservation gate for this telemetry-only change is a source/impact assessment;
no new persistence mechanism or runtime restoration claim is introduced.

### Verification results

On 2026-10-13, actual source/diff review and `git diff --check` passed. Inline static
checks confirmed eight unique leaf hooks with retained injection, the null container,
callers for all twelve operations, complete schema registration, unique custom event
names across feature catalogs and implementation dependency boundaries. Generated
profile DI registers exactly one qualified schema provider and the service binding.
The final `:androidApp:assembleDebug` and `:shared:compileKotlinIosSimulatorArm64`
run passed (BUILD SUCCESSFUL). Earlier scoped analytics/profile and UI/profile
compilation also passed. Existing global opt-in/deprecation warnings remain.
No automated test source or infrastructure was added. Android/iOS device execution,
SDK delivery, reporting-failure injection, actual cancellation/lifecycle timing and
fresh-process restoration were not run. iOS compilation does not prove framework
linking or native runtime behavior; no Firebase sign-in/send/upload was performed.

Profile contract follow-up on 2026-10-13: Goal.PrimaryConfirmed and
Goal.SecondaryConfirmed are parameterless observations; their nonnull picker
results imply Valid internally. Goal.TargetDateConfirmed alone accepts the existing
formatter status. GoalPicker remains the closed selector for open callbacks only.
The profile_picker_confirmed version-1 wire payload, fields and status values are
unchanged; no report migration, dual emission or StateKeeper change is needed.
Contract follow-up verification: source invariants and git diff --check passed;
:ui-screen-features:profile:compileKotlinIosSimulatorArm64 and
:shared:compileKotlinIosSimulatorArm64 passed (BUILD SUCCESSFUL). Device runtime
and Android compilation were not rerun for that contract-only refinement.

### Profile restoration and Back follow-up (2026-10-13)

The eight leaf routes and null-hook Profile container above were reconciled with
the current child factory, rendered surfaces, retained VM injection and foundation
binding. All twelve operation values have actual VM callers; existing typed event
schemas and privacy omissions remain unchanged.

Body now registers the same leaf BackCallback as the other profile editors:
system Back invokes Body.DismissRequested before the existing parent pop, just like toolbar
Back. Parent/programmatic navigation still does not synthesize user events.

Body's fresh height/weight population consumes the first nonnull user observation
once in the VM-owned Processing.Infinity subscription. Nondefault picker values
entered before that observation win. A restored snapshot, including empty or
invalid formats, skips population entirely. The separate lifecycle-aware user
and history subscriptions rebuild presentation without rewriting inputs; ordinary
pause/resume does not restart population. No edit flags, source caches, new keys
or archive schema changes were introduced. The five local draft suppliers and
serializers in the inventory above remain the restoration owners. Menu rebuilds
its summaries; Settings, Social and the container need no editable-state supplier.

Manual scenarios still required: save an unfinished draft in each of the five
editors, destroy the old lifecycle/InstanceKeeper, restore the archive through
fresh Components and confirm selections plus final save arguments. Include empty
and invalid Body formats, accepted picker results, an empty first catalog emission,
changed catalogs, a delayed first user observation and pause/resume beyond the
foundation disconnect grace. Confirm toolbar/system Back reports once and pops
once, and re-entry after pop initializes from current business data. Dialog
sessions, jobs, loaders and telemetry provenance remain unsaved. Android needs
shell-supplied saved state; iOS needs a completed compatible background checkpoint.
Abrupt active termination is not a guaranteed restoration path.

Follow-up verification: scoped `:ui-screen-features:profile:compileAndroidMain`,
`:analytics:profile:compileAndroidMain` and
`:ui-screen-features:profile:compileKotlinIosSimulatorArm64` finished with
BUILD SUCCESSFUL. Source/diff review and `git diff --check` passed; generated
profile service/schema bindings were inspected. Existing opt-in warnings remain.
No automated tests/infrastructure were added. Device restoration, native checkpoint
delivery, runtime Back/lifecycle timing, payload delivery and reporting-failure
injection remain unverified; the manual scenarios above are pending.
Independent source review of the profile owners and Body diff found no blocking
defects; it did not execute runtime restoration or lifecycle scenarios.

## Exercises analytics and process restoration

Added on 2026-10-13. Actual ExercisesRouter has one Library route, entered in
Default (optional preselected group) or SimilarTo (target exercise) mode through
ExercisesRootComponent. Both render the same independent library surface.

| Route / surface | Component → VM | Typed screen / hook |
|---|---|---|
| ExercisesRouter.Library, both modes and restored stacks | ExerciseLibraryComponent → ExerciseLibraryViewModel | ExercisesScreen.Library / exercise_library / screenPresentation |
| Exercises root stack host | ExercisesRootComponent → ExercisesRootViewModel | Default null; only renders its child |

The leaf resolves ExercisesAnalytics in retainedInstance. Foundation binds and
closes the presentation, assigning Initial/Restore; no feature lifecycle bridge
or saved telemetry provenance is introduced. Shared includes the generated module
and its single qualified exercises_analytics catalog provider.

| Callback | Observation / eligibility |
|---|---|
| onSuggestionsClick | FilterClicked(Suggestions) after existing SimilarTo guard; toggle semantics unchanged |
| onMuscleGroupClick | FilterClicked(MuscleGroup); group IDs omitted; repeated clicks still toggle |
| onExerciseExampleSelectClick | ExerciseSelected only after finding the current card; result/navigation unchanged |
| onLoadNextPage | LoadNextPage only after existing loading/end guards; automatic list-end callback, not a user-click claim |
| onBack | DismissRequested from toolbar/system callback; root forwarding adds no duplicate event |
| onQueryChange | Exact text updates draft; telemetry deliberately omitted: search has no existing validation status, raw text/length is never reported |

Four custom version-1 schemas: exercises_filter_clicked (bounded library_filter),
exercises_exercise_selected, exercises_load_next_page, exercises_dismiss_requested (no fields).
Built-in screen schemas are reused. This is additive instrumentation with no
historical backfill or changed existing wire identities. Ordinary reporting
Exceptions are isolated; business updates/navigation remain independent. No entity
IDs, target/group IDs, query text/length, errors or request/response bodies enter
telemetry. Continuous catalog/target/search Flows are rebuilt business
subscriptions, not finite operations; no artificial operation decorator or jobs
are added. Focus/IME/clear, scroll position, dialog sessions and referrer/flow
attribution are deliberately outside this contract.

| Owner | Field/group | Policy | Serializer/key or source; restore/default |
|---|---|---|---|
| Library | queries.name, including whitespace and explicitly empty values | Preserve | ExerciseLibraryDraft / ExerciseLibraryState.serializer → Queries; saved value wins |
| Library | queries.filter: All, Suggestions or Group(id) | Preserve | Same local draft; saved All wins over route preselection; empty muscle emissions retain Group intent |
| Library | mode and target identity | Rebuild from route | Existing serialized ExercisesRouter; mode supplied before VM initialization, target card observed from ExerciseExampleFeature |
| Library | exerciseExamples, muscleGroups, target presentation, hasSuggestionsResults | Rebuild | Transient fields; current UserExerciseExamplesUseCase/MuscleFeature/ExerciseExampleFeature |
| Library | pagination including loading/end/page | Runtime only | Transient default first page; old cards/pages are not replayed |
| Root | navigation identity/result key | Existing stack preservation | ExercisesRouter.serializer and existing keyed childStack; no parent editable draft |
| All | loaders/jobs, focus, list position, result callbacks, analytics provenance | Runtime only | Not added to archive |

Component restores inside retainedInstance and registers one lazy supplier of the
latest VM state. On a fresh VM the saved draft is used before route filter defaults,
then mode is rebuilt from the restored route. No saved draft means the original
Suggestions/preselected-group/All defaults. Nonempty current muscle catalogs drop
vanished group IDs; empty startup emissions never erase selection intent. Loading
starts from page one with the restored query/filter. Programmatic source fallback
from empty Suggestions to All produces no synthetic interaction. No secret fields
exist in this feature. Popping and re-entering uses fresh route seeds; the selected
exercise is sent through the existing result key and its accepted business value
belongs to the caller, outside this module.

Source gates reviewed: actual route/render/owner inventory, exactly one typed leaf
hook and null container, retained injection/foundation binding, complete custom
catalog, eligibility, reporting isolation, local supplier/serializer reachability,
saved-empty precedence, transient runtime/data fields, empty/nonempty catalog
behavior and absence of restoration flags/caches or synthetic callback replay.
No automated test sources/dependencies/infrastructure were introduced.
Manual checks still required: save → serialization roundtrip → destroy lifecycle
and InstanceKeeper → fresh context/VM restore in both modes; exact/empty query,
All overriding route preselection, selected group through initial empty catalog,
vanished groups, Back/result business inputs, pop/re-entry, pause/resume beyond
subscription grace, actual lifecycle/cancellation/reporting-failure injection and
SDK delivery. Android saved-state and iOS completed background checkpoints are the
supported shell boundaries; abrupt foreground iOS termination is not guaranteed.

Verification on 2026-10-13: source/diff review, inline screen/preservation/privacy/
dependency invariants and `git diff --check` passed. The combined
`:ui-screen-features:exercises:compileKotlinIosSimulatorArm64`,
`:shared:compileKotlinIosSimulatorArm64` and `:androidApp:assembleDebug` run passed
(BUILD SUCCESSFUL, 12m 25s). Generated Koin source confirms the ExercisesAnalytics
binding and single qualified schema catalog. Existing opt-in/expect-actual and
other project warnings remain. The initial sandboxed wrapper attempt could not
write its cache lock; the authorized retry succeeded. Device restoration, native
framework linking/runtime, SDK delivery and failure-injection checks were not run.

## Home interactions and restoration (2026-10-13)

Scope: `ui-screen-features/home`. Actual route `HomeRouter.Home`, reached through
main's Home tab, creates HomeComponent and its retained HomeViewModel. Loading,
WelcomeHomeContent and DashboardHomeContent are variants of that same surface:
exactly one `HomeScreen.Home` presentation hook covers all of them. HomeRootScreen
only renders its child stack; HomeRootViewModel keeps the default null hook.
The existing shared HomeAnalyticsModule registration now contributes the uniquely
qualified `home_analytics` catalog; foundation still owns lifecycle presentation.

| Observation owner / callback | Typed event / operation | Eligibility / wire payload |
|---|---|---|
| HomeViewModel.onOpenTrainings | TrainingsClicked | Existing unconditional navigation; no parameters |
| onOpenExample | ExerciseClicked | Existing dialog opening; exercise ID excluded |
| onOpenMuscleLoading | MuscleLoadingClicked | Only after nonnull range guard |
| onOpenTrainingStreak | TrainingStreakClicked | Only after nonnull range guard |
| onOpenGoalDetails | GoalDetailsClicked | Only after nonnull range guard |
| onPerformanceMetricClick | PerformanceMetricClicked | Only after nonnull range guard; metric values/type omitted |
| onOpenPeriodPicker | PeriodPickerClicked | Existing dialog opening |
| PeriodPicker.onResult in HomeViewModel | PeriodConfirmed / SavePeriod | Accepted result; complete setRange/getOrThrow block |
| onAddGoal / TrainingGoalDetails.onAddGoal closure | AddGoalClicked | Existing navigation at the callback owner; no reporting context propagated |
| onBack | DismissRequested | Existing Back callback |
| Range-driven mapLatest / withLoader | RefreshTrainings | Complete getTrainings/getOrThrow block; waits for resolved persisted range |
| Existing startup safeLaunch | RefreshExerciseExamples | Complete getExerciseExamples/getOrThrow block |

All interaction schemas are version 1 and parameterless. Each operation uses
version 1 home_task_started/completed/failed/interrupted schemas with the closed
home_operation enum; terminal events add duration_ms. Toolkit decoration owns
fresh operation IDs, monotonic timing, original return values/exceptions and
cancellation. Ordinary reporting exceptions are isolated by the feature event
boundary or toolkit operation decorator. Failed Result values now reach the
standard error pipeline instead of being silently discarded by startup refreshes.
No entity IDs, selected date bounds, raw input, measurements or errors are encoded.

Deliberate omissions: dialog presentation/close/cancellation, metric type and
selected period attribution, notification permission/scheduling and welcome
celebration operations. onWelcomeConfettiShown is a rendering callback, not a
user click. Metric aggregation remains data-backed computation, not a newly
tracked operation. Root forwarding callbacks do not duplicate leaf events.

| Owner | Field/group | Preserve / Rebuild / Runtime only | Source / restore and checks |
|---|---|---|---|
| HomeRootComponent | Child navigation | Preserve | Existing HomeRouter serializer and HomeRootComponent childStack key; no route inputs/drafts |
| HomeRootViewModel | HomeRootState | Runtime only | Stateless container; no supplier required |
| HomeViewModel | Accepted range kind | Preserve via existing persistent owner | LocalSettings setRange → DataStore `range`; observeRange resolves saved kind before training observation/request; absent preference selects Last30Days |
| HomeViewModel | Range date bounds / display | Rebuild | DateRangeFormatState.of(kind) uses current date/locale; no stale formatted snapshot |
| HomeViewModel | user, lastTraining, muscleLoad, streak, performance, goalProgress, spotlights | Rebuild | UserFeature, TrainingFeature observations and existing metric use cases |
| HomeViewModel | excludedMusclesCount, missingEquipmentCount, hasGoal | Rebuild | Existing excluded-muscle/equipment and goal observations |
| HomeViewModel | showWelcomeConfetti | Runtime only | Existing persistent HomeWelcomeStatus prevents ordinary replay; unfinished animation is not a draft |
| Home and its dialogs | Loaders/jobs, focus, callbacks, error/dialog sessions, analytics provenance | Runtime only | No StateKeeper archive or secret-bearing feature fields |

Source gates: route → rendered variants → retained VM → typed hook reconciled;
container has no hook; no telemetry-only navigation/state arguments; every new
model is consumed. Preservation inventory contains no unfinished screen form,
accepted parent draft or local filter without a persistent owner. Consequently no
local StateKeeper supplier or HomeState serializer is added. Existing DataStore
owns the user's accepted selection and is not duplicated in an archive.

Permitted manual checks still unrun: choose a nondefault period, wait for save,
destroy lifecycle/InstanceKeeper and construct fresh components with the shell
archive; verify the persisted period and current date bounds, then Back/re-entry.
Check a fresh installation's Last30Days fallback, pause/resume including the
foundation subscription disconnect grace, pending/failed period saves, range
changes cancelling refreshes, reporter-failure injection, and one presentation
for loading/welcome/dashboard. Dialog unfinished inputs are expected to disappear.
Android saved-state navigation and iOS completed-background-checkpoint restoration
retain their existing shell limits; abrupt iOS termination before checkpoint is
not guaranteed. No runtime restoration or Firebase delivery is claimed.

Home verification result: `git diff --check` passed. Scoped
`:analytics:home:compileAndroidMain :ui-screen-features:home:compileAndroidMain
--offline` passed, including common KSP; generated HomeAnalyticsModule contains
both HomeAnalytics binding and the qualified home_analytics schema. Existing
unresolved global opt-in warnings remain. `compileKotlinMetadata` was skipped by
Gradle and is not counted as compilation evidence. Review covered event guards,
parameterless payloads, all operation terminal schemas and toolkit isolation.
No automated test sources/infrastructure were introduced. iOS compilation and
device/runtime/manual checks above were not run.

## Training analytics and recreation preservation

Current training scope reviewed on 2026-10-13; this section supersedes the earlier
screen-only training omissions. Actual TrainingRouter has Recording (Add, Edit,
Draft), Exercise (with an exercise seed/result key), Exercises (delegated library)
and Completed (stage/exercises/startAt). All are serializable stack routes.

| Route / rendered surface | Component → VM | Typed presentation / ownership |
|---|---|---|
| Recording, Add/Edit/Draft including draft deeplink | TrainingRecordingComponent → TrainingRecordingViewModel | TrainingScreen.Recording / training_recording; existing typed hook |
| Exercise, new/edit/replacement-library return | TrainingExerciseComponent → TrainingExerciseViewModel | TrainingScreen.Exercise / training_exercise; existing typed hook |
| Completed, saving loader and summary | TrainingCompletedComponent → TrainingCompletedViewModel | TrainingScreen.Completed / training_completed; new typed hook |
| Exercises / Library | ExercisesRootComponent → ExerciseLibraryComponent → ExerciseLibraryViewModel | Delegated ExercisesScreen.Library / exercise_library; existing owning feature hook |
| Training stack host | TrainingComponent → TrainingViewModel | Default null; renders only child content |
| Exercises stack host | ExercisesRootComponent → ExercisesRootViewModel | Default null; renders only child content |

The three training leaves resolve TrainingAnalytics in their retainedInstance
factories. Foundation remains the sole lifecycle binding owner and assigns
Initial/Restore; no lifecycle listener, reporting state or route attribution is added.

### Observations and operation coverage

| Owner / callbacks | Observation and eligibility |
|---|---|
| Recording onAddExercise/onEditExercise/onDeleteExercise | Add/Edit/Delete intent; Edit emits only after finding the exercise. Empty-start confirmation invokes shared library navigation without a synthetic Add click. Existing Delete has no eligibility guard. |
| Recording reorder start/end | Preserve existing ID/index guards and reorder mutations; report ReorderExercises at accepted nonempty end, avoiding per-drag event volume. Invalid/no-op start is omitted. |
| Recording onSave/onBack | Existing Save/Back intent; no new guards or eligibility changes. Confirmations remain business-owned. |
| Recording StartTraining | Open after delay/current-draft guard; confirmed only on empty-start action or accepted nonempty exercise list. No unaccepted dialog values are saved. |
| Recording unfinished/completion/discard dialogs | Separate opened and confirmed facts in owning callbacks; completion opens only when completed sets exist. |
| Recording library/editor result | LibrarySelected and ExerciseUpdated on actual accepted callbacks, without IDs/values. Editor Remove calls removeExercise business path and does not fake a Recording Delete click. |
| Exercise add/edit iteration | Add after current example exists; EditVolume/EditRepetitions after example/iteration guards. IterationOpened when constructing/opening the dialog, IterationConfirmed on accepted result. |
| Exercise delete/reorder/end/save/back | Existing Delete/Save/Back intent; reorder emits after existing ID/index guards; end emits separately. No extra business guards or loaders. |
| Exercise details/replacement library | ExampleOpened, ReplaceExample on actual detail action, ExampleSelected after different-ID guard; IDs omitted. |
| Completed summary/exercise/back | SummaryOpened after training-ID guard, ExerciseOpened/Back in existing callbacks. Error-dismissal navigation bypasses the user Back event. |

Seven typed operations decorate complete existing Feature/UseCase calls:
DeleteDraft (including initialization/clear/discard/completed cleanup), LoadDraft,
LoadTraining (recording initial edit/completed summary), SaveDraft, CreateTraining,
UpdateTraining, RefreshExamples. Result-returning calls unwrap inside the block;
finite source observations keep their existing firstOrNull contract. Null local
read results and null successful write IDs remain returned values, not invented
failures. Continuous catalogs, validation, total calculations, weight lookup and
notification scheduling are not new artificial operations. No coroutine/retry,
dispatcher, cancellation guard or business branch is added for telemetry.

TrainingAnalyticsModule registers one qualified training_analytics catalog with
training_interaction (closed training_action codes) and training_task_started,
training_task_completed, training_task_failed, training_task_interrupted, all
version 1. Internal exhaustive encoders own wire codes. Operation lifecycle uses
the common fresh OperationId/monotonic duration decorator, preserving caller values,
original errors and cancellation. Ordinary reporting Exceptions are isolated;
fatal Errors remain fatal. Existing Recording/Exercise wire identities are unchanged;
Completed and behavioral schemas are additive, without historical backfill.

No raw weights/repetitions/duration/date, exercise names, entity IDs, input lengths,
credentials, errors/messages, URLs or bodies enter reporting. The training_action
code embeds the observed owner, without transported cross-screen reporting context.
Dialog presentation/close/dismiss/cancel outcomes, internal unaccepted picker edits,
focus/IME, referrer/funnel attribution and per-drag telemetry are deliberate omissions.
Consent, identity and SDK delivery remain application/core-owned.

### Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or source | Restore/default and check |
|---|---|---|---|---|
| Recording | stage, startAt, exercises, iteration values/IDs/order including pending and empty values | Preserve | Existing TrainingRecordingDraft / TrainingRecordingState.serializer | Local archive wins; null exercises resumes existing initialization; nonnull empty remains empty. Required startAt has no dynamic serialization default. |
| Recording | catalogs/examples/muscles | Rebuild | Existing observations; transient fields | Populate current presentation without replacing loaded draft. |
| Exercise | ExerciseState, current example identity, iterations/formats/IDs/order | Preserve | Existing TrainingExerciseDraft / TrainingExerciseState.serializer | Local draft wins over route seed; accepted parent values seed fresh editor on re-entry. |
| Exercise | current example presentation/history/validation artifacts | Rebuild | Existing example/context observations; transient fields | Recompute from restored exercise without replaying interaction callbacks. |
| Completed | ID of successful saved training | Preserve | New TrainingCompletedDraft / TrainingCompletedState.serializer | ID is committed to current State immediately after successful nonnull save; restored ID bypasses Create/Update and resumes cleanup/presentation rebuild. |
| Completed | timeline, training summary, muscleLoad | Rebuild | Existing TrainingFeature observation and timeline/distribution use cases; transient fields | Load by restored ID; no serialized localized/display snapshots. |
| Training/container | stack, completed accepted exercises/stage/startAt, editor seed/result key | Existing navigation preservation | Existing TrainingRouter.serializer / TrainingComponent childStack | No duplicate container draft; route inputs remain ordinary business inputs. |
| Owning recording/editor | accepted picker/library/editor values | Preserve at existing owner | Existing result/config callbacks and latest lazy suppliers | Child result updates parent draft; popped child local draft is discarded; fresh editor receives accepted parent exercise. |
| Dialogs/all owners | unfinished dialog input, sessions/errors, loaders/jobs, focus and telemetry provenance | Runtime only | Excluded from archives | Fresh restoration opens no old live session; pending Add initialization may reopen its business start dialog. |

Components restore inside retainedInstance factories and register one local lazy
supplier of current State. No new source/edit flags, previous-source caches, global
store, callback synchronization or route mutation is introduced. These drafts have
no authentication secrets. Existing recording/editor addresses and serialized
fields remain unchanged. The new completed supplier is additive; old archives
without it use fresh state and the existing save behavior. Existing iOS build/format
validation and Android saved-state boundaries remain unchanged.

Completed checkpointing is not transactional server idempotency: process termination
or a checkpoint made before the successful ID is captured can still replay an
in-flight write. This task preserves a known completed business outcome; it does
not change backend contracts or invent an idempotency mechanism. The existing
early Add during initial Edit/Draft loading risk recorded above remains unchanged.

### Source gates and manual verification

Review covers actual routes/render bodies, exactly three training leaf hooks and
null containers, delegated Library ownership, retained dependency injection, common
foundation binding, complete catalog/encoders/callers, current eligibility and
no synthetic init/error-navigation interactions. Preservation source gate covers
local suppliers/serializers, restored-empty precedence, accepted parent seeds,
transient data/runtime fields and completed outcome rebuilding. UI layout is unchanged;
existing previews remain applicable. No automated test sources or infrastructure added.

Manual checks not run: StateKeeper.save → platform serialization roundtrip → destroy
old lifecycle/InstanceKeeper → fresh contexts/VMs; Add/Edit/Draft exact and empty
exercise lists, pending iteration formats/order and accepted dialog values; editor
replacement-library/Back/pop/re-entry; pause/resume beyond subscription grace;
Completed restore after saved ID with no second write; restore during an in-flight
write; reporting-failure/cancellation injection and SDK delivery. Android saved state
and completed iOS background checkpoints are supported shell boundaries; abrupt
active iOS termination has no guaranteed checkpoint. Compilation does not prove these.

Verification results on 2026-10-13: final source/diff review and `git diff --check`
passed. Inline source invariants confirmed all 32 event consumers/exhaustive matrix
entries, seven operation consumers, three leaf hooks/draft suppliers, null training
container, transient completed presentation, analytics dependency boundaries and
shared module registration. Independent read-only reviewer found no blocking source
findings and confirmed final coverage reconciliation and absence of synthetic Add/Back
interaction events. Generated Koin source has the TrainingAnalytics service binding
and one qualified training_analytics AnalyticsSchema provider.
Final combined `:analytics:training:compileKotlinIosSimulatorArm64`,
`:ui-screen-features:training:compileKotlinIosSimulatorArm64`,
`:shared:compileKotlinIosSimulatorArm64`, `:androidApp:assembleDebug` passed
(BUILD SUCCESSFUL, final rerun 15s; initial integrated run 1m 15s).
The initial sandboxed Gradle wrapper could not write its cache lock; authorized
build execution succeeded. No device execution, framework linking, fresh-process
restoration, reporting-failure injection or SDK delivery was run; the manual
scenarios above remain unverified. Unrelated concurrent home/profile/transport
working-tree changes were preserved.

Home follow-up review: the fresh VM alone seeds an unresolved Empty range;
HomeState retains a resolved default for previews. HomeScreen shows its existing
loader until the persistent range is resolved, including returning-user dashboards,
so no default-period picker can open before restoration. Range resolution releases
the same Home screen presentation without replaying callbacks. No additional
screen, event, saved field or lifecycle wiring is introduced. All ten event variants
and three operation values have VM consumers and exhaustive encoders; no obsolete
Home helper/model was found. The opening coverage summary now describes the current
behavioral integrations instead of claiming they are presentation-only.

Follow-up verification: scoped Android compilation of home and analytics/home
passed (BUILD SUCCESSFUL, 3s); `git diff --check` and inline source gates passed.
Device restoration, UI preview rendering and iOS compilation were not run in
this follow-up. Concurrent changes outside Home were preserved.

Training residual review on 2026-10-13: current source and independent read-only
review found no orphan event/operation declarations, unused new imports, missing
local draft suppliers or synthetic restoration/control callbacks. All 32 events
and seven operation kinds have consumers and exhaustive encoders. The introductory
inventory now includes Completed and references current training instrumentation.
Source/diff checks passed; this follow-up changes documentation only, so application
builds were not repeated. Runtime limitations above remain.


## Confirm training completion dialog — 2026-10-13

Scope: `DialogConfig.ConfirmTrainingCompletion` → DialogContentComponent child
factory → ConfirmTrainingCompletionComponent → retained ConfirmTrainingCompletionViewModel.
This rendered surface is a live bottom sheet, not an independent screen. Its
foundation screenPresentation remains default null; no screen enum is added.
The nested DurationPicker remains a live picker outside this instrumentation scope.
Recording and Completed retain their existing typed screen hooks.

| Callback / observation | Typed event / owner | Coverage |
|---|---|---|
| Duration input click | CompletionConfirmation.DurationOpened / dialog VM | Before existing PickDuration direction |
| Accepted nested picker result | CompletionConfirmation.DurationSelected / dialog VM | Existing onDurationSelected; no duration value encoded |
| Confirm button | CompletionConfirmation.Confirm / dialog VM | Before existing Confirm direction; existing UI disables invalid/empty duration |
| Cancel button / system Back | CompletionConfirmation.DismissRequested / dialog VM | Shared existing onBack; no inferred dismissal reason |
| Open / accepted dialog result | Recording.CompletionOpened / CompletionConfirmed / recording VM | Existing parent observations; distinct from local confirm intent |

Four bounded action codes extend the existing training_interaction v1 schema
additively; existing action meanings and populations are unchanged. No raw values,
IDs, duration measurements or error payloads are added. No business operation
exists in this dialog, so no operation model/decorator is added. Existing service
exception isolation and shared TrainingAnalyticsModule registration are reused.
Dedicated dialog view/close, scrim/swipe/toolbar dismissal and nested picker edits
remain explicit omissions: those facts are not exposed at this VM boundary.
No synthetic events are emitted during initialization or recreation.

### State preservation inventory

The user explicitly selected the existing unsaved-dialog policy for this task.

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or source | Restore/default and check |
|---|---|---|---|---|
| Completion dialog VM | Current duration, including accepted nested-picker value before final confirmation | Runtime only | Existing retainedInstance and State | Ordinary retained recreation keeps VM; process death closes session |
| Completion dialog | Initial duration | Rebuild | Parent startAt → elapsed duration, existing one-minute minimum | Fresh dialog seeds from current training; no archived dialog draft |
| Recording screen | Finally confirmed duration represented by startAt, cleaned exercises | Preserve | TrainingRecordingDraft / TrainingRecordingState.serializer() | Existing parent callback updates state before navigation; local supplier reads latest state |
| Completed screen | Accepted stage/exercises/startAt and known saved outcome | Existing preservation | TrainingRouter.Completed serializer and TrainingCompletedDraft | Existing business route and completed owner restore accepted data |
| Dialog/host | Callbacks, nested session, loaders/focus and telemetry | Runtime only | Session and step serializers remain null | No secrets or runtime objects enter archives |

Source gate: actual host factory/result delivery, retained VM injection, parent
callback update, recording restore/save supplier and typed state serialization were
reviewed. No new StateKeeper owner is needed under the chosen policy. Existing
previews still apply; rendering is unchanged. No automated tests/infrastructure
were introduced.

Unrun manual checks: retained Activity recreation with changed duration; actual
process death after confirmation and after navigation; fresh StateKeeper roundtrip
with destroyed lifecycle/InstanceKeeper; cancel/Back/nested picker return; payload
and reporting-failure injection; Firebase delivery. Android saved archives and
completed iOS background checkpoints have their existing limits.

Scoped verification result: source invariants and `git diff --check` passed.
`:ui-dialog-features:confirm-training-completion:compileAndroidMain`,
`:analytics:training:compileAndroidMain`,
`:ui-dialog-features:confirm-training-completion:compileKotlinIosSimulatorArm64`
and `:analytics:training:compileKotlinIosSimulatorArm64` passed together
(BUILD SUCCESSFUL, 5m 12s). Existing opt-in warnings remain. The initial sandbox
cache-lock denial was resolved by authorized scoped build execution. Compilation
does not prove fresh-process restoration or SDK delivery. Concurrent unrelated
working-tree changes were preserved.

Follow-up source review: all four event consumers and exhaustive encoders,
retained injection, generated service/schema bindings and the accepted parent
state path were rechecked. No unused declarations or blocking source findings
were found. The build result above was moved into its owning section; this
follow-up changes documentation only, so compilation was not repeated.
`git diff --check` and scoped source gates passed; runtime limits remain.

## Confirmation dialog analytics and recreation assessment

Scope: `ui-dialog-features:confirmation`, reviewed 2026-10-13.

| Route / surface | Component → VM | Presentation / source gate |
|---|---|---|
| `DialogConfig.Confirmation` → shared dialog content child → ConfirmationScreen | ConfirmationComponent → ConfirmationViewModel | Runtime overlay, default null screenPresentation; no independent screen or dedicated dialog presentation contract |
| DialogComponent / DialogContentComponent | Existing shared host VMs | Containers; no presentation hook added |

All current entries (profile deletion, training save and training exit) use the
same shared child factory and retained VM. The Component resolves the single
ConfirmationAnalytics API within retainedInstance; shared registers its annotated
implementation module. No routing/reporting context or lifecycle wiring is added.

| Callback / observation | Version-1 event | Meaning / exclusions |
|---|---|---|
| `onConfirm` | `confirmation_confirm` | Confirm intent before the existing Confirm direction; no claim of downstream operation success |
| `onBack` | `confirmation_dismiss_requested` | Existing Cancel button or registered hardware Back callback; source is intentionally not inferred |

Both events have no parameters. Description, title, callbacks, action context and
entity IDs never enter payloads. Ordinary reporting Exceptions cannot block either
direction; fatal Errors retain their existing semantics. There are no inputs,
validation/busy guards or asynchronous business operations in this VM. Downstream
operations remain owned by the caller, without an extra operation decorator here.
Swipe/outside dismissal does not call this VM's onBack and is not reported as Back.
Dialog viewed/closed telemetry is deliberately omitted: this scope adds callback
observations, not a separate dialog presentation contract or screen_view.

### Preservation inventory and source gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or source | Restore/default and check |
|---|---|---|---|---|
| ConfirmationViewModel | `description: UiText?` (read-only) | Runtime only | Existing DialogConfig input; retainedInstance | Same VM retains nullable description on ordinary recreation; no editable draft or StateKeeper supplier required |
| Shared dialog host | Session, title, onResult/back callbacks | Runtime only | Existing retained host; slot/stack `serializer = null` | Process restart closes session; callbacks are never serialized or replayed |
| Calling screen | Confirmed action / business outcome | Existing owner outside this module | ProfileSettings / TrainingRecording callbacks and existing operations | Confirmation returns Unit, no accepted selection/input to preserve in this dialog |
| Confirmation VM/foundation | Directions, loaders, telemetry provenance | Runtime only | Existing foundation ownership | No persistence keys, shadow flags, jobs, secrets or restoration-generated events added |

The module owns no editable input, selection, filter or parent draft. Consequently
its state-preservation gate requires this assessment, not serializing read-only
presentation or changing the intentionally unsaved dialog host. Opening a new
confirmation constructs its state from the caller; rotation retains the existing
VM. Supported application archive restoration does not restore a live dialog.

Source/diff review covers actual route → rendered overlay → Component → retained
VM, callback order, parameter-free schemas, exception isolation, DI registration
and host serializer boundaries. `git diff --check` is the whitespace check.
Build and review results are recorded below after verification. Unrun runtime checks:
Android recreation with the dialog open; real process death/iOS checkpoint restart
closing the session without invoking onResult; Confirm, Cancel and hardware Back
with a reporting failure; SDK delivery. These require application execution and
are not established by source review or compilation.

### Confirmation verification and residual review (2026-10-13)

Source/diff review found no orphan event models, missing callers, duplicate
schemas/service registration, reporting-only routing fields or accidental draft
serialization. Both typed events have one VM consumer and an exhaustive encoder;
generated Koin code binds ConfirmationAnalytics and its uniquely named schema.
The existing retained ownership and unsaved dialog policy remain unchanged.
Moved this feature's verification record into its own section; no production
change was needed during the residual review.

- `git diff --check` passed.
- Isolated `:ui-dialog-features:confirmation:compileAndroidMain` and
  `:analytics:confirmation:compileAndroidMain` with `--no-configuration-cache`
  passed together (`BUILD SUCCESSFUL`, 9s, 267 tasks).
- The fresh combined UI / analytics / shared Android attempt compiled Confirmation
  Android and common metadata, then failed outside this scope in
  `:ui-dialog-features:iteration-picker:transformNativeMainDependenciesMetadata`
  on a missing transformed material-icons-core klib. Shared compilation remains
  unverified; no unrelated cache or infrastructure change was made.
- Earlier attempts failed on concurrently incomplete module registration and
  unresolved Compose references, or stalled while saving configuration cache;
  those historical attempts do not supersede the isolated successful build above.
- Independent review was not performed: no permitted independent reviewer was
  available. Self-review and source gates are not a substitute.
- Native runtime recreation, reporting-failure injection and SDK delivery remain
  unverified. No automated test infrastructure was added.



Duration picker residual review on 2026-10-13: no orphan events/callbacks,
placeholder operations, duplicate service/catalog registrations or saved telemetry
fields were found. Removed a stale selection reference from the reporting-isolation
comment and an unfinished verification sentence. Two typed events each have one
VM consumer and one exhaustive encoder; retained ownership and unsaved dialog
policy remain unchanged. Scoped `git diff --check` and inline source invariants
passed. Final isolated Android analytics + duration-picker UI compilation passed
(`BUILD SUCCESSFUL`, 24s, 282 tasks). The combined shared Android attempt progressed
through duration-picker compilation but failed outside this scope in
`:data-services:google-auth:transformNativeMainDependenciesMetadata` on a missing
transformed ktor-network-tls klib. Shared compilation, native runtime restoration,
SDK delivery and independent review remain unverified; no unrelated caches or
infrastructure were modified.


## Trainings calendar analytics and recreation preservation

Current scoped inventory on 2026-10-13. This section supersedes the original
presentation-only omission for TrainingsAnalytics. Existing screen identity and
archive addresses are unchanged; new interaction/task schemas start at version 1.
No historical interaction population exists to migrate.

| Route / rendered surface | Component → VM | Typed screen / hook |
|---|---|---|
| MainRouter.Calendar tab, RootRouter.Trainings → TrainingsRouter.Trainings; Daily and Monthly, including loading/empty content | TrainingsComponent → TrainingsViewModel | TrainingsScreen.Calendar / trainings_calendar; exactly one screenPresentation |
| Trainings root stack | TrainingsRootComponent → TrainingsRootViewModel | Default null; renders child stack only |
| Date/MonthPicker, TrainingMenuPicker, Exercise, Statistics.Trainings/Training | Existing dialog owners | Live overlays; no dedicated presentation contract added |

TrainingsComponent resolves analytics inside retainedInstance. Foundation alone
binds lifecycle, emits Initial/Restore and closes on pause/destroy. Editing a
training leaves this module via the existing callback to the Training flow; that
flow's own presentation is outside this change. Calendar modes are one surface,
not separate screens. Root/main containers retain their null hooks.

| VM observation boundary | Typed event | Eligibility / facts |
|---|---|---|
| onTrainingMenuClick | TrainingMenuClicked | Existing callback; no entity ID |
| Menu onResult | DeleteSelected / EditSelected / DetailsSelected | Accepted menu result before existing command/navigation/overview |
| onExerciseClick | ExerciseClicked | Existing callback; no exercise ID |
| onDailyDigestViewStats | DigestClicked | Opens statistics with existing business range; range not encoded |
| onSelectPeriod | DailyPeriodSelected / MonthlyPeriodSelected | Only after same-period and unknown-ID rejection |
| onOpenDateSelector | DatePickerClicked / MonthPickerClicked | Actual current mode selects picker |
| Date/MonthPicker onResult | DateConfirmed / MonthConfirmed | Accepted result, including unchanged value; no date encoded |
| shiftDate from Next/Previous callbacks | NextDateSelected / PreviousDateSelected | After unchanged/coerced-range rejection; no event at date limits |
| onOpenDaily | DailySelected | Existing calendar day callback; UI limits clicks to current-month days with training records |
| onBack | DismissRequested | Leaf callback only; container forwards navigation without synthetic event |
| rangeFlow mapLatest / withLoader | RefreshTrainings operation | Entire getTrainings/getOrThrow block; same effective range does not retrigger from presentation rebuild |
| deleteTraining safeLaunch | DeleteTraining operation | Entire DeleteTrainingUseCase.execute/getOrThrow block |

All 16 interactions are parameterless schemas. Two closed operation codes accompany
started/completed/failed/interrupted schemas; terminal events include monotonic
duration. Toolkit supplies fresh operation identity, reporting exception isolation,
original result/error identity and CancellationException propagation. No new
coroutine, retry, busy guard or business validation was introduced. Source review
traced successful Result unwrap, ordinary failure, cancellation and reporting
exceptions through the actual toolkit implementation; these were not exercised
with runtime failure injection.

Omissions: dedicated dialog views/closes, dismissal reasons, DAO observation
operations, local digest/timeline formatting operations, referrers/flow attribution,
transition events and programmatic restore clicks. No raw dates, inputs, lengths,
entity IDs, measurements, secrets, exception messages or request bodies are sent.
Qualified trainings_analytics registers the complete custom catalog once; existing
shared module enumeration and analytics-api dependency remain sufficient.

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| TrainingsComponent / VM | Daily/Monthly mode and selected date range, including accepted picker results and day/arrow selection | Preserve | TrainingsState.serializer(), private DRAFT / TrainingsDraft | Existing retained VM wins; otherwise restoreState inside factory wins over fresh current-month default; date has no default and cannot be elided |
| Trainings VM | Monthly calendar month reference, weeks and empty digest range | Rebuild | TrainingsState.selectRange → Monthly.from(saved date, empty timeline), then existing observations | Initial calendar now uses saved month before DAO data arrives rather than transient current-month defaults |
| Trainings VM | Training list, timelines, aggregate digest and localized labels | Rebuild | observeTrainings plus TrainingDigest/TrainingTimelineUseCase | Transient fields excluded; re-fetch uses restored effective range; later observations change presentation only |
| Trainings VM | Date limitations | Rebuild | DateTimeUtils.trailingYear() transient default | Current supported limits; existing daily range coercion retained |
| TrainingsRoot | Child navigation stack | Preserve | Existing TrainingsRouter serializer, TrainingsRootComponent stack key | Serializable Trainings route; no parent draft or editable root state |
| Dialogs / VM | Unconfirmed dialog inputs, sessions/callbacks, loaders/jobs/errors/focus, analytics provenance | Runtime only | No draft fields | Live dialogs close after process death; accepted date/month remains in screen draft |

The existing local supplier saves viewModel.state.value lazily on StateKeeper.save;
no second owner, new archive key or storage is added. Dynamic initial date is
computed only without a saved draft. No secrets exist in this draft. Restoration
initializes state directly without invoking control callbacks; init restarts the
existing range request/observation. Back/pop may destroy a child draft; fresh
re-entry without an archive uses the existing default, with no parent draft to seed.
Pause/resume uses the existing VM activation/subscription policy; no form
initialization or restoration flags/caches were added.

Source screen coverage and preservation gates were reconciled with actual routes,
rendered bodies, retained factory, foundation binding, serializable fields and
callback consumers. Self-review found no remaining blocking source defects.
Independent review was not performed: no authorized separate reviewer is available
in this chat; agent delegation was not requested and the review connector only
provides PR CI diagnostics.

Verification and manual limits: Android scoped compilation and generated Koin
schema registration are checked for this change; final build result below.
Metadata-only compile tasks were SKIPPED and do not count as compilation.
No automated test sources/infrastructure or verification-only production hooks
were introduced. Runtime/native restoration, pause/resume disconnect timing,
reporting-failure injection and SDK delivery remain unverified.

Permitted manual checks: select a historical month, switch to Daily and pick a date;
save and serialize the shell archive, destroy lifecycle and InstanceKeeper,
construct fresh contexts/VMs from it and verify exact mode/range, rebuilt records,
initial saved-month calendar, Back/edit-and-return and final refresh inputs. Confirm
unchanged period/unknown ID and arrow limits emit no accepted selection, while
unchanged picker confirmation emits its observed confirmation. Verify unconfirmed
pickers disappear; range replacement/destroy interrupts refresh, failed delete
uses the standard error pipeline, and throwing reporter does not block actions.
Android requires a delivered saved-state archive; iOS requires a completed valid
background checkpoint from the same build. Abrupt active iOS termination has no
restoration guarantee. Neither retention nor a State-only copy proves fresh wiring.

Final verification for this trainings change: inline source invariants and scoped
`git diff --check` passed. `:analytics:trainings:compileAndroidMain
:ui-screen-features:trainings:compileAndroidMain --offline` passed on the final
revision, including common KSP and generated qualified trainings_analytics catalog.
A preceding final-revision attempt failed in resources:provider cache cleanup
while another build wrote new files; a scoped retry passed without deleting caches
or changing unrelated source. Existing global opt-in and metadata warnings remain.
At initial implementation, iOS native compilation and device/runtime checks were not run. Prior full-app
build results elsewhere in this document are historical, not evidence for this change.


Follow-up quality review on 2026-10-13: inspected every scoped route, rendered
mode/control, callback guard, event/schema consumer, complete operation boundary,
retained injection, generated DI, draft field, source initialization and lifecycle
subscription. Existing whileActive disconnect grace is one second; resume restarts
range observations/refresh after that grace without replacing the user's draft.
No orphaned new public models, copied Home identifiers, TODO stubs, duplicate
screen hooks, hidden restore flags, new tests or extra analytics coroutine was found.

Resolved finding: date arrows and accepted date/month changes previously updated
State.date while leaving old Daily items or Monthly grid/digest until a DAO
emission. TrainingsState.selectRange now atomically clears rebuilt records and
constructs the Monthly grid/empty digest for the new range. Initialization, period
selection, changed picker confirmation and arrow navigation reuse that state
transition. Unchanged picker confirmation still emits its real accepted result but
retains current presentation and avoids a new request. No business guard, date
coercion policy, wire schema or serialized field/key changed. Source review confirms
the new helper is used at all four affected boundaries; opening Daily already
constructs its own empty Daily variant. No unresolved scoped source finding remains.

Final follow-up checks: source invariants and scoped git diff --check passed;
:analytics:trainings:compileAndroidMain,
:ui-screen-features:trainings:compileAndroidMain,
:analytics:trainings:compileKotlinIosSimulatorArm64 and
:ui-screen-features:trainings:compileKotlinIosSimulatorArm64 passed together offline
(BUILD SUCCESSFUL, 1m 12s). Generated Koin retains the typed service binding and
qualified catalog; remaining compiler warnings belong to existing global opt-ins.
These checks supersede the earlier iOS compile omission for these two modules,
not the unrun device restoration, failure-injection, lifecycle or SDK checks.
Independent review remains unperformed under the same reviewer limitation.

## Draft-training dialog analytics and preservation (2026-10-13)

Scope: `DialogConfig.DraftTraining` → DialogContentComponent factory →
DraftTrainingComponent → retained DraftTrainingViewModel → DraftTrainingScreen.
MainViewModel opens this surface when the durable draft exists. This is a live
bottom sheet, with no nested independent screen or container in the affected module.
The VM keeps the default null screen hook; no screen enum is introduced. Dedicated
dialog presentation/close and host gesture dismissal remain explicit omissions.
The three existing Contract callbacks observe ContinueClicked, StartNewClicked and
Back; programmatic navigation and draft observations emit no interaction events.
There are no input/status/picker facts. Existing callbacks have no eligibility/busy
guards; this change preserves that behavior. Exercise cards are display-only.

DeleteDraft decorates the complete existing deleteDraftTraining().getOrThrow()
inside safeLaunch. Navigation follows successful deletion outside the decorator;
failure/cancellation follows the existing foundation pipeline. The shared decorator
supplies operation ID, monotonic duration and started/completed/failed/interrupted
outcomes, preserving results and exception identity with isolated reporting failures.
The custom catalog has three parameter-free interaction schemas and four operation
schemas with only the bounded DeleteDraft code/duration; no training IDs, exercise
values, text or exception details are reported. A qualified draft_training_analytics
schema and stateless service are registered once through the generated Koin module.

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| Training data owner | Accepted training draft and exercise/iteration values | Preserve in existing durable owner | DraftTrainingDao; setDraftTraining inserts draft pack | Existing database survives process recreation; no duplicate local snapshot |
| DraftTrainingViewModel | stage and exercises | Rebuild | getDraftTraining → DAO Flow → existing domain/state mapping | Fresh VM starts Draft/empty and rebuilds Add/Edit and exercise presentation from the current durable draft |
| Dialog host / component | Live dialog session, callbacks, pending navigation | Runtime only | Host slot/step navigation serializer = null; retainedInstance | Ordinary recreation retains the VM when InstanceKeeper survives; process death closes the session |
| DraftTrainingViewModel | Jobs, telemetry, loaders | Runtime only | Existing foundation VM scope | No state supplier, archive fields, synthetic callbacks or saved provenance |

Preservation source gate: there are no editable inputs, filters, selections or
accepted child results in this dialog. Its route has only live callbacks; no parent
draft is modified by Continue/Back. StartNew removes the durable draft as before.
A StateKeeper supplier would duplicate data-backed presentation and cannot restore
the unsaved dialog session; none is added. Fresh reopening subscribes to the current
DAO. Random presentation IDs are rebuilt and are not saved business identity.
No shell, navigation serialization, state initialization or UI layout changes.

Source/diff review covers all three event consumers, exhaustive event/operation
encoding, DI registration, privacy and the dialog screen/preservation classifications.
No automated test sources or infrastructure were introduced.
Manual checks pending: Continue/StartNew/Back, delete success/failure/cancellation,
reporting-failure injection, ordinary component recreation, fresh process restart
with closed dialog followed by reopening the durable draft, and SDK delivery.
Compilation alone does not prove these runtime scenarios.

Verification results: source/diff gate and independent read-only review found no
blocking scoped findings. Generated Koin code confirms the DraftTrainingAnalytics
binding and qualified schema registration. Both draft-training modules compiled for
Android (`compileAndroidMain`) and iOS Simulator (`compileKotlinIosSimulatorArm64`):
scoped offline build without configuration cache succeeded in 15s. Shared integration
compilation remains unverified: the combined attempt failed on missing generated
screen-api metadata; the isolated shared retry failed on missing google-auth
metadata (.klib). Concurrent builds were active; configuration-cache lock waiting
was confirmed with a thread dump and avoided using the command-line flag only.
No unrelated infrastructure or source was changed to resolve these build failures.
Device restoration, reporting-failure/cancellation injection and SDK delivery remain
unrun. No feature/session restoration guarantee beyond the inventory above is claimed.

Residual review on 2026-10-13: source and independent read-only review found no
orphan APIs/files/events, duplicate registration, template remnants, missing event
consumers or preservation-owner gaps in draft-training. Fresh scoped Android and
iOS Simulator compilation of both modules passed (BUILD SUCCESSFUL in 9s).
Shared retry remains unverified: it failed on missing generated exercises metadata
(`build/classes/kotlin/metadata/commonMain/default/linkdata`), after progressing
past the previous metadata failures. No unrelated source/cache cleanup was applied.
The runtime/manual limitations above remain unchanged.

## Duration picker analytics and recreation policy

Scope reviewed on 2026-10-13: `DialogConfig.DurationPicker` renders
DurationPickerComponent → retained DurationPickerViewModel → DurationPickerScreen.
The shared dialog content factory supports direct opening and the nested
ConfirmTrainingCompletion duration step. Both use the same VM owner. This is a
live bottom-sheet surface, not an independent screen: default null
screenPresentation, no screen enum or dedicated dialog presentation contract.
No independent screen or container behavior is changed by this integration.

| Callback owner | Observation | Eligibility and payload |
|---|---|---|
| DurationPickerViewModel.onSubmitClick | SubmitClicked / duration_picker_submit_clicked v1 | Existing unconditional submit; emitted before the existing BackWithResult direction; empty payload, no duration value |
| DurationPickerViewModel.onDismiss | DismissRequested / duration_picker_dismiss_requested v1 | Existing VM Back callback; empty payload; request only, no inferred cancellation or delivered close |
| DurationPickerViewModel.onSelectDuration | Deliberate omission | Wheel invokes selection during initialization/programmatic synchronization as well as scrolling. No interaction provenance is available; no synthetic user event or telemetry history is added |

No async business operation exists in this VM, so no operation API/decorator is
introduced. Open intent and accepted result remain facts at the caller boundary;
shared host/config plumbing does not report them. Swipe/outside/host dismissal may
bypass this VM and is explicitly not covered by DismissRequested. No view/close,
raw duration, route ID, input text, secret or exception payload is emitted.
One stateless DurationPickerAnalytics service is injected inside retainedInstance;
shared registers its generated module and unique duration_picker_analytics schema.
Ordinary reporting Exceptions cannot prevent navigation; fatal Errors propagate.

### Preservation inventory and source gate

The user explicitly chose the existing dialog policy: retain the live selection
through Activity recreation; discard the session after process death. No local
StateKeeper supplier, serializer, session restoration or shell change is needed.

| Owner | Field/group | Policy | Source and restoration |
|---|---|---|---|
| DurationPickerComponent / VM | Current DurationFormatState value, including unfinished selection | Retain in live session; runtime only in process archive | Existing retainedInstance owns VM State; initial input is evaluated only for a fresh VM. Fresh dialog receives the caller's business initial duration |
| DurationPickerState | Hours/minutes bounds | Rebuild | Existing DurationLimitation-derived lists |
| ConfirmTrainingCompletion parent dialog | Accepted picker duration | Runtime session value | Existing onResult → onDurationSelected updates parent State; reopening the picker uses that value. Parent dialog is also discarded on process death |
| All dialog owners | Callbacks, session stack, jobs/loaders, focus/widget state, analytics provenance | Runtime only | Shared childSlot and childStack use serializer = null; no secrets or telemetry values are serialized |

Source review traced both factories, result delivery, VM initialization and retained
ownership, unchanged wheel synchronization and the host's unsaved session boundary.
No new fields, restore/edit flags, source caches or callback replay are introduced.
Screen classification and preservation source gates pass for this scoped dialog;
this is not a claim of process-death persistence for unconfirmed selections.

Manual checks not run: change duration → recreate Activity → submit exact retained
value; nested picker → confirm → reopen using accepted parent duration; actual
process death closes both dialogs; swipe/outside dismissal emits no inferred VM
close; reporting Exception injection and real SDK delivery. Independent review
was unavailable in this chat (no permitted independent local reviewer); self-review
covers the changed source and final diff. Build results are recorded below;
compilation alone does not prove the manual scenarios.

Verification results on 2026-10-13: `git diff --check` and inline source gates
passed (two event consumers/exhaustive encoders, retained VM injection, null dialog
screen hook, implementation dependency boundaries and shared module registration).
`:analytics:duration-picker:compileAndroidMain` and
`:analytics:duration-picker:compileKotlinIosSimulatorArm64` passed together
(`BUILD SUCCESSFUL`, 8s). The integrated duration-picker/shared compilation was
attempted twice with configuration cache disabled after an initial cache-lock wait;
both attempts failed in the unchanged `:ui-core:foundation:kspCommonMainKotlinMetadata`
because its KSP cache/generated CoreModule output was missing during concurrent
build activity. UI/shared compilation therefore remains unverified; no unrelated
infrastructure/cache deletion was performed. No automated tests were introduced.

## Exercise detail dialog analytics and recreation assessment

Scope: `:ui-dialog-features:exercise`, 2026-10-13. The read-only Exercise overlay
now reports two parameterless typed observations: `exercise_example_details_clicked`
after the existing example guard, and `exercise_dismiss_requested` from its existing VM
callback. Host-managed exits are explicitly omitted. No independent screen hook
or finite operation is applicable. Data is rebuilt from TrainingFeature; live
dialog sessions remain unsaved, with no editable draft requiring StateKeeper.
See [Exercise coverage](exercise/COVERAGE.md) for the actual surface/callback matrix,
state inventory, privacy boundaries and verification limits.


## Menu picker dialog (2026-10-13)

[Scoped coverage and preservation inventory](menu-picker/COVERAGE.md) records
ItemClicked and DismissRequested, the live overlay presentation classification,
parent-owned accepted actions, deliberate omissions and verification limits.


## Month picker scoped coverage (2026-10-13)

[Month picker coverage and preservation inventory](month-picker/COVERAGE.md) records
SubmitClicked and DismissRequested, empty payloads, retained dialog selection,
parent-owned accepted month restoration and verification limits. Earlier Date/MonthPicker
overlay rows still apply: no dedicated dialog presentation contract is introduced.
Tail review added Valid-only submit eligibility and exact-time boundary synchronization;
Android/iOS Simulator analytics, picker UI and shared compilation passed. Native
restoration and SDK delivery remain manual checks.

## Performance trend details dialog

The read-only performance-trend-details dialog reports its existing VM Back request
through a typed, parameterless DismissRequested event. Live sessions remain unsaved;
range/type are caller inputs and history is rebuilt from current data. No editable
draft requires StateKeeper. See [scoped coverage](performance-trend-details/COVERAGE.md)
for the surface/callback inventory, omissions, preservation gate and checks.

## Primary goal picker

Current callback coverage and dialog-state preservation assessment: [primary-goal-picker](primary-goal-picker/COVERAGE.md).


## Period picker (2026-10-13)

Period picker callback telemetry is now scoped in [period-picker/COVERAGE.md](period-picker/COVERAGE.md).
Two empty-payload events observe range selection and dismissal requests. Earlier
Home rows remain parent open/accepted-result/save coverage; dialog visibility and
host-driven closure remain uninstrumented. Live sessions stay unsaved; the accepted
period already uses durable LocalSettings/DataStore with no extra StateKeeper copy.

## Secondary goal picker scoped coverage (2026-10-13)

[Secondary goal picker coverage](secondary-goal-picker/COVERAGE.md) records
parameterless GoalClicked and DismissRequested observations, the overlay screen
classification, parent-owned accepted selection preservation and verification limits.
The existing profile-owned opening/accepted-result observations remain unchanged.

## Training streak details (2026-10-13)

The read-only training-streak-details dialog reports its existing VM Back request
through a typed, parameter-free event. Opening intent remains owned by Home.
Dialog views/close, host-handled system Back/toolbar closure and swipe/scrim
dismissal are omitted; no user-owned draft requires StateKeeper.
See [scoped coverage](training-streak-details/COVERAGE.md)
for the actual surface, preservation inventory and verification limitations.

Statistics dialog analytics and state assessment: see [statistics/COVERAGE.md](statistics/COVERAGE.md). Both modes remain runtime dialogs; the scoped implementation tracks Back requests and the existing finite range refresh.

## Training goal details dialog

[Scoped coverage and preservation inventory](training-goal-details/COVERAGE.md)
records parameterless AddGoalClicked and DismissRequested at the dialog VM.
The earlier overlay presentation omission remains valid: no independent screen
or dedicated dialog presentation contract is added. Live sessions remain unsaved;
range is a caller seed and progress/insights rebuild from current training data.
There is no editable draft requiring StateKeeper. Existing Home events are unchanged.


## Weight picker scoped coverage

[Weight picker coverage and preservation inventory](weight-picker/COVERAGE.md)
records changed-selection observations, submit clicks and VM dismissal requests
with empty payloads. The live overlay remains outside independent-screen tracking;
unconfirmed selection is runtime-only. Accepted direct results remain in UserDraft
or ProfileBodyDraft; nested results stay in the live iteration until its Submit
updates TrainingExerciseDraft.
Wheel synchronization can change selection without a user gesture; the selection
event does not claim gesture provenance. Host-managed exits remain omitted.

## Live GA4 setup review — 2026-10-14

Owner-confirmed release boundary: the mobile app is unreleased; existing history
is personal testing, not a released-user cohort or a product baseline. No launch
date, production migration or historical backfill is assumed. Record the actual
release boundary when it occurs. This entry is setup evidence, not project policy.

Verified property: Grippo Analytics / `grippo-e3960`, property `519002689`.
Affected streams: Android `com.grippo.android` (`G-0YXY3NRJ7J`) and iOS
`com.grippo.app` (`G-PBKNNR7JHE`). Saved detail reports and the Explore funnel
exclude web; Reports snapshot excludes web only with its mobile comparison selected.
Property details displayed a fixed `(GMT+02:00) GMT+02:00` reporting timezone;
Reporting identity displayed `Blended`. Both were inspected without changing them.
Daily report boundaries follow the property's reporting timezone, not the workout
date or each device's local calendar. [GA4 property semantics](https://developers.google.com/analytics/devguides/config/admin/v1/rest/v1beta/properties)
document that distinction. Current GA4 user counts are not a registered-account
count, including under the selected reporting identity.

Additional read-only Admin inspection found one data filter: `Internal Traffic`,
Exclude where `traffic_type` exactly matches `internal`, state `Testing` (not
Active). No developer-traffic filter was listed. Testing identifies matching data
rather than applying exclusion; this is not evidence that personal mobile tests
are tagged or removed. The actual release-date boundary separates earlier tests,
but does not automatically exclude continued personal testing after release.
No filter was created or activated. [GA4 data filters](https://support.google.com/analytics/answer/13296761?hl=en)
document the permanent processing effect of active exclusion, distinct from report
filters and historical data.

Data retention displayed Event data `2 months`, User data `14 months`, and Reset
on new user activity On; all were inspected unchanged. The 14-month user setting
is not a guarantee that every detailed event remains available that long. The
current 7-day funnel and 28-day diagnostics fit the observed ordinary-event window.
[GA4 retention](https://support.google.com/analytics/answer/7667196?hl=en) distinguishes
explorations/funnel retention from standard aggregated reports, which this setting
does not truncate to two months; key-event data follows user-data retention.

| Current wire contract | Saved admin consumer / deliberate omission |
|---|---|
| `profile_operation_completed`, `profile_action=create` | Derived `grippo_profile_created` on both mobile streams, copying parameters; key event once per event, no monetary default. Completion covers the full profile setup block, including related data reads and the local welcome status, not only the backend creation write. |
| `training_task_completed`, `training_operation=create_training` | Derived `grippo_training_create_completed` on both mobile streams, copying parameters; key event once per event, no monetary default. This proves operation completion; its nullable result does not guarantee a returned training ID. |
| Bounded event parameters | Event dimensions: `auth_action`, `auth_method`, `profile_action`, `profile_operation`, `screen_id`, `training_operation`, `option`, `training_action`, `interaction_screen`, `interaction_action`, `main_tab`. Saved rows verified. |
| `duration_ms` | Saved event-scoped custom metric `Grippo Operation duration ms`, unit Milliseconds. Terminal-operation elapsed time, not workout length. Filter the event/operation population and choose aggregation explicitly; copied source/derived values are not separate operations. |
| Random operation IDs, secrets, unused identity/context | No custom definitions. `launch_source` remains unknown without a real caller; do not interpret it as attribution. |
| Legacy `registration_completed`, `workout_started`, `workout_completed` | Historical observations have initialization/entry semantics; do not merge them with the new successful-operation population. Raw history retained. |
| Goal operation with Boolean result | No goal-success key event: successful return may contain false. |
| `start_training_task_completed` / `option=preset` | Building start options may complete with no generated preset. Keep option selection as an observation; no generation-success key or availability rate is inferred. |
| SDK identity / screen context | No app caller updates analytics identity; current user counts are GA4 identities, not registered accounts. Native SDK screen context does not prove an operation's owner/origin; operation payload deliberately omits the app's navigation context. |
| Onboarding `screen_view` / `auth_control_clicked` | Existing `screen_id=auth_onboarding` and `interaction_screen=auth_onboarding`, `interaction_action=next/dismiss_requested` are covered by existing Event dimensions. Native `screen_name` needs no new custom definition. No new derived rule or key event is required; receipt is unverified; first onboarding now initializes absent consent to granted, preserving saved denial. |

Saved and inspected reporting configuration:

- Published `Grippo — главное` collection retains Acquisition, Engagement and
  Retention. Engagement now opens the two working reports below. The save
  confirmations named only this published collection. Business objectives and
  Data import stay unpublished; no deletion.
- Current Reports snapshot is `Grippo — обзор (до релиза: тесты)`, with a saved
  description identifying personal testing. Four hero metrics and seven useful
  cards cover activity, sources, platform, app versions, retention and stability.
  `Grippo — приложения` comparison selects Android/iOS. Stability
  card is unavailable with a comparison and can be viewed without it / in Firebase.
  Saved description tells the reader to select that comparison, explains that a
  fresh tab may use All Users and points to `Профиль и тренировки` for results.
  Further saved-description review clarified GA4 users versus accounts and added
  the exact Explore / `Создание тренировки — события` path for diagnostics; the
  complete description was read back in Library after saving.
- Closed Explore funnel `Grippo — профиль → создание тренировки`, tab
  `Путь GA4: профиль → тренировка, 7 дней`, starts at exact `grippo_profile_created`, then
  exact `grippo_training_create_completed`, indirectly followed within 7 days.
  Platform breakdown and `Platform does not exactly match web` filter; no first_open denominator.
  Tab title and closed/app-filter settings were re-read after reload.
  Its former "first training" title was corrected to operation-creation wording,
  without implying first-ever persisted training.
- The same Explore now has a saved Free form tab `Создание тренировки — события`:
  rows Event name, columns App version, values Event count and Total users;
  filters are AND-combined `training_operation exactly matches create_training`,
  `Platform matches regex ^(Android|iOS)$` and
  `Event name matches regex ^training_task_(started|completed|failed|interrupted)$`.
  Tab names, dimensions, two values and all three filters were read back after
  reload. The original closed two-step funnel and enabled 7 Days constraint were
  re-read again beside it. No new custom definitions, derived rules or business
  events were added for this diagnostics tab.
  Each row counts that state's events; the total across started and terminal
  states is not a count of unique attempts. It is not an attempt success/error
  percentage: date boundaries, consent changes and retries can separate states.
  A further live re-open verified Event name rows, App version columns, both values
  and all three exact filters. The shared Variables period was `Last 28 days`,
  Sep 16–Oct 13 during the October 14 review: it excluded today's events. The date
  picker was inspected and canceled without changing this relative period. A
  same-day delivery check must use the appropriate Realtime/DebugView scenario
  separately; absence from this historical Explore range is not a transport test.
- Published detail report `Профиль и тренировки` (`16123745061`) replaces the
  collection's former Events item and filters
  event name `^(grippo_profile_created|grippo_training_create_completed)$` AND
  platform `^(Android|iOS)$`. Metrics are Event count and Total users, with no
  invented revenue. Its normal menu link, report ID, saved filters, two table
  columns and description were re-opened after saving.
  Description distinguishes GA4 users from accounts, event completions from
  unique persisted entities, and event date from the date of an actual workout.
  A further source review traced the complete `AuthOperation.CreateProfile` block:
  [ProfileCompletedViewModel](../ui-screen-features/authorization/src/commonMain/kotlin/com/grippo/authorization/profile/creation/completed/ProfileCompletedViewModel.kt)
  awaits [CreateProfileUseCase](../data-features/feature-api/src/commonMain/kotlin/com/grippo/data.features.api/user/CreateProfileUseCase.kt)
  (create, reload user and related data), then saves the local welcome status.
  `profile_operation_failed` / `profile_action=create` can therefore occur after
  the server has created the profile; it does not prove creation rejection or
  rollback. The saved report now says completion of profile setup and explicitly
  describes this late-failure possibility. No business behavior or rule changed.
  Completion also precedes Home navigation; it does not prove Home or the welcome
  animation was displayed.
- Published diagnostic detail report `События и экраны` (`16123956244`)
  replaces the collection's former Pages and screens item. It
  uses only `Platform matches regex ^(Android|iOS)$`, default Event name and
  optional App version / Grippo Screen (`screen_id`) dimensions, preserving all
  four previous native page/screen selectors. A further review added existing
  `training_operation`, `auth_method`, `auth_action`, `profile_action` and
  `profile_operation` as optional primary dimensions; all twelve selectors were
  read back from the ordinary published report after saving. This makes the
  shared operation families distinguishable without new custom definitions.
  Event count / Total users are its
  only metrics. Its normal menu link, report ID, saved filter, dimension menu,
  two table columns, chart metric labels and saved description were re-opened.
  `screen_id` groups the context of all events; actual screen-view counts require
  `event_name=screen_view`. It includes
  received legacy history for transport diagnosis, not a current-schema baseline.
  Its description explains `failed` versus `interrupted` and personal test data.
  The report-data editor reset that description; it was restored with operation
  selection guidance and read back in Library. The canonical admin gate now
  explicitly checks saved names/descriptions after report-data edits.
  Further description review identifies Total users as GA4 identities, not
  accounts, and native screen selectors as SDK context, not operation-origin
  attribution. Source and [Firebase screenview behavior](https://firebase.google.com/docs/analytics/screenviews)
  support this interpretation; receipt of current operation payloads remains
  unverified. [Firebase user identity](https://firebase.google.com/docs/analytics/userid)
  documents the distinct optional user-ID mechanism; current app callers do not
  wire it, so account-level deduplication is not asserted.
  Its further saved-description revision identifies `failed` as failure of the
  entire decorated block and warns that profile setup may fail after creation.
  The complete 480-character description was verified after a full Library reload.
- Unassigned copies `16123829170` and `16123873705` are now labelled
  `Архив — черновик профиля и тренировок` and
  `Архив — черновик событий приложения`. Saved descriptions point to the working
  report names/IDs; Library rows confirm neither belongs to a collection.
  They are retained copies, not a product-provided archive/recovery mechanism.
- The generic key-event-name card was removed from this custom snapshot after
  further UX review: its historical `first_open` count could be mistaken for a
  product result, and immutable default keys would continue mixing meanings after
  release. The strict profile/training report remains the results path. The save
  confirmation named only Reports snapshot; seven retained cards and absence of
  the generic card were verified after a full reload. Its former `first_open`
  drilldown was checked before removal and still uses the original
  `events-overview`; no raw events, default key marks or source reports were deleted.
- Descriptions of the existing `profile_operation`, `training_operation` and
  `interaction_action` definitions were corrected and read back after full reload:
  goal completion may return false; training creation completion does not guarantee
  an ID; `dismiss_requested` is a request, not completed closure. Event scope,
  parameter keys and dimension names stayed unchanged. Descriptions fit the
  observed field limit, and were checked for truncation.
  A subsequent `profile_action` description correction distinguishes full setup
  from a single creation write and explains late `failed/create`. The observed
  150-character field limit was respected with a complete 149-character description;
  Event scope, the `profile_action` parameter and the dimension name were re-read
  after reload. The two published report descriptions and Library funnel's first
  step description were also corrected and verified together after full reload.
- The existing duration metric's complete 130-character description was corrected
  and read back after reload: operation time is not workout length, sum is not mean,
  and source events must be separated from their derived copies for operation-time
  analysis. Name, Event scope, `duration_ms` and Milliseconds were retained.
  Source factories emit duration on terminal completed/succeeded, failed and
  interrupted events, not started. The two Grippo derived rules copy parameters;
  combining source and derived matching completions can repeat a duration in the
  sum and parameter-occurrence count. Uniform duplication does not necessarily
  change an average; mixing duplicated completions with uncopied outcomes can
  instead alter its weights. No numerical report defect or received duplication
  is asserted: both published detail reports and the training diagnostics tab
  currently use Event count / Total users, not the duration metric.
  [GA4 Data API metric variants](https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema#custom_metric_variants)
  distinguish `customEvent:duration_ms` (sum), `averageCustomEvent:duration_ms`
  (average) and `countCustomEvent:duration_ms` (parameter occurrences). This does
  not establish average-metric availability in the native UI. Any derived average
  needs duration occurrences from the same filtered population; Event count with
  started/unrelated events is not a valid denominator.
- Old saved funnel `16123774523` is labelled `Архив — тестовые первые запуски`,
  explicitly describes its first_open/consent/test limitations, and is unpublished.
- Annotation dated October 14, 2026, `До релиза: личное тестирование`, was saved
  and read back in Admin > Annotations. It records review status, not a launch date.

Outstanding / verification limits:

- New operation-derived events have not been observed from an authorized real
  app scenario. Saved configuration is verified; SDK delivery and processing of
  the current catalogue remain unverified. Empty new reports are expected until
  real consent-enabled scenarios are received; no synthetic events were sent.
- A fresh review corrected the earlier diagnosis of disabled Library saving:
  the unsupported Explore regex filter caused it, not empty data. Replacing it
  with `Platform does not exactly match web` enabled Save while data stayed empty.
  The funnel was saved to Library as `Grippo — путь к созданию тренировки`
  (`16123817061`); saved exact event steps, closed setting and app filter were
  re-opened. Explore's enabled 7 Days time constraint was separately read back.
  The Library viewer does not expose the duration value; its saved description
  scopes the independently verified 7 Days claim to Explore rather than asserting
  unverified Library constraint persistence. Its description now explicitly calls
  this a GA4-user path, not an account path or a percentage of successful attempts;
  the complete saved text was read back in Library. Further review clarified its
  first step as completion of profile setup, without changing step conditions.
- The saved mobile comparison is selectable, not a permanent property filter.
  A fresh browser tab reopened Reports snapshot with All Users; select
  `Grippo — приложения` for the mobile-only overview. The detail report and
  Explore funnel have their own saved app filters independent of this selection.
- Native drag-and-drop could not insert copied reports into a new topic. The
  unfinished empty Results draft was closed without saving, then the collection
  was freshly reopened: Acquisition, Engagement and Retention plus the two working
  report names were confirmed, with no empty topic. Updating existing menu items
  completed the usable reporting path without drag-and-drop; the earlier manual
  drag request is obsolete. The Explore list opens the named current funnel;
  its exact two events, closed setting, web exclusion and enabled 7 Days constraint
  were read back again from the saved research. The Library funnel remains
  unassigned; the verified Explore entry is the supported path to its 7-day view.
- Concurrent authorization changes add `auth_onboarding` through the existing
  `screen_view` and `auth_control_clicked` contracts (`interaction_screen=
  auth_onboarding`, `interaction_action=next/dismiss_requested`). Existing custom
  dimensions cover them. At that earlier inspection the placeholder had no opt-in before login;
  default Denied discards onboarding/auth telemetry until a real opt-in. No
  complete fresh-install denominator or delivery is asserted, and no concurrent
  feature work was changed for an admin report.
- Read-only device inventory confirmed Android `emulator-5554` and no booted iOS
  simulator. Android Studio's Running Devices panel exposed no device UI, so no
  authorized app scenario was executed through the available computer controls.
  The previously found iOS binary is dated October 12 and cannot prove October 14
  source delivery. No app was installed, no accounts/training were created, and
  collection consent was not changed.
  A further read-only UI check again found an empty Running Devices panel and no
  running standalone emulator or Simulator in the enabled app inventory. The
  selected Pixel device/run configuration is not evidence of an accessible app UI.
- Read-only artifact review found the Android debug APK dated October 14,
  01:14 +03:00 (`2.0`, version code `19`), with 38 production inputs newer than
  that artifact, including current onboarding/consent UI. Its DEX contains the
  earlier typed auth/training event names but not `auth_onboarding`; it cannot
  verify delivery of the current complete source. It has no embedded Git SHA or
  source provenance. No stale artifact was installed or launched as evidence of
  current-schema delivery.
- Live Admin > DebugView reported no development-device debug events in the last
  30 minutes. This is not proof that normal SDK collection is broken: debug mode
  is separate. Neither new catalogue delivery nor the derived outcomes were
  observed during this review.
  A separate ordinary Realtime overview with All Users showed 0 active users in
  the last 30 and 5 minutes and no event rows at inspection time. This describes
  that observation window, not a transport failure or proof of future delivery.
  A later fresh Realtime observation showed 2 active GA4 users in the last
  30 minutes and received `registration_completed` alongside `screen_view`,
  `user_engagement`, `first_open` and `session_start`. In All Users, the legacy
  event's visible parameter keys were `firebase_event_origin`,
  `firebase_screen_class` and `firebase_screen_id`; origin was `app`. Identifier
  values were not copied into this record. This is receipt evidence for legacy
  telemetry, not the current typed catalogue or a successful profile creation.
  The mobile comparison could be selected, but event-parameter drilldown explicitly
  reported that Realtime data was not supported for that comparison; All Users
  was restored for the parameter inspection. No stream-specific/current-binary
  payload claim follows from that drilldown. Typed operation families and both
  Grippo-derived outcomes were not observed in this later window. The earlier
  zero-activity snapshot is historical, not the latest receipt status.
- Fresh Events > Key events inspection verified both Grippo keys enabled, both
  counting Once per event, and both using Don't set a default key event value
  (disabled default amount 0). Both show No stream data detected. Legacy
  registration/workout initialization events are absent from the key-event list.
  GA4's five default keys, including first_open and purchase families, have
  disabled status toggles; they are not additional verified product outcomes.
  A further stream-by-stream inspection re-opened all four derived rules: both
  exact AND conditions and Copy parameters from the source event remained correct
  on Android and iOS. The operation metric was separately re-read as Event scope,
  `duration_ms`, Milliseconds. Both Grippo key-event settings were separately
  re-opened again: Once per event and Don't set a default key event value remained
  selected. Rules, key marks and units were retained.
- Four independent agents reviewed caller semantics, journeys and policy/setup
  gaps; actionable prerelease/funnel/dimension findings were corrected. Final
  independent review found no remaining defects in the instruction changes or
  the final configuration record. Source/diff review, three skills' YAML metadata
  and placeholder checks, 71 local instruction links and `git diff --check` passed.
  A subsequent full coverage-link check also verified all 23 local links in this
  record. Stale pending-admin statements in the original source/onboarding pass
  were reconciled with the later live review, preserving that pass's separate
  widget/independent-review/runtime limitations.
  Bundled `quick_validate.py` could not run because neither available Python
  runtime has PyYAML; metadata/reference checks used existing Ruby YAML instead,
  with no dependency installation. No app runtime scenarios, builds or
  automated test infrastructure were introduced for this documentation/admin task.
  An additional inline lexical check found 149 unique literal `AnalyticsEventSchema`
  names, all satisfying lowercase/underscore naming and the 40-character limit
  (longest 39); this is a source-literal check, not compiled catalogue or receipt
  evidence.

Further review on 2026-10-14 reconciled the current authorization summary with
the nine-owner inventory, including Onboarding's actual guards/callbacks and
Splash's shown-state gate. Removed route-entry reason codes are no longer
described as current telemetry contracts; the eight-hook/build evidence remains
explicitly historical to the October 13 repair. The prerelease migration statement
now preserves the separate received legacy self-test population. Independent
re-review found no remaining discrepancies in these corrections.

Fresh live readback confirmed the eleven Event dimensions, the operation-duration
metric's Event scope/parameter/unit/description, both enabled Grippo key marks,
the published collection and current report descriptions. The current Explore
funnel's step labels were clarified to `Настройка профиля завершена` and
`Вызов создания тренировки завершён`; a full reload and reopened builder confirmed
the same exact event conditions, indirect transition, enabled 7 Days constraint,
closed setting and web exclusion. Its range remained Last 28 days (September 16
through October 13). The earlier unassigned Library export retains read-only step
labels and its verified explanatory description; the current Explore is the
supported 7-day path. No event rule, schema, metric or population changed.
Source/diff review, three skill frontmatters and agent YAML, 71 instruction links,
23 coverage links and `git diff --check` passed. Current literal naming checks
cover 147 feature schemas plus two toolkit built-ins (149 unique, all v1, longest
39 characters). These checks and saved admin readback do not verify current mobile
SDK receipt; the runtime limitations above remain.

A subsequent independent review corrected the canonical collection reference's
stale consent-owner inventory at that revision: Profile Settings was the control
owner and Login/onboarding did not mutate consent. The later onboarding policy
below supersedes that owner inventory. The control rejects a pending source,
unchanged values and an ongoing consent write; accepted changes use the existing
persistence error pipeline. Re-review found no
remaining defect in that correction. Fresh live inspection again verified all
four Android/iOS derived rules, their exact AND discriminators and enabled source
parameter copying. Both mobile stream rows reported receiving traffic in the
past 48 hours. Internal Traffic remained Testing with `traffic_type=internal`,
not an active exclusion or proof that mobile self-tests are classified.
The later All Users Realtime window showed zero active users in both 30 and five
minutes and no event rows; earlier legacy receipt remains historical evidence,
not evidence of a current payload or a transport failure. Running Devices was
still empty; the device picker offered virtual-device launches, but no current
app scenario was executed or fresh artifact installed. No live admin drift
requiring a mutation was found in this pass.
The two normal published menu reports were re-opened with their saved mobile
filters and Event count / Total users columns; the results report retained its
exact two-derived-event regex, while diagnostics retained received legacy history.
A fresh fourth reviewer traced feature-change, prerelease-report, editorial-only
and retirement requests through the canonical workflow and found no defect.

The requested prerelease admin configuration and its usable reporting paths are
saved and verified. Actual delivery of the current mobile schema and derived
outcomes remains a separate unverified runtime check on both platforms.
