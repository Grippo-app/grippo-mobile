# Analytics coverage

Record owner: [feature-analytics](../.claude/skills/feature-analytics/SKILL.md);
records policy: [CLAUDE.md](../CLAUDE.md#records).

This file holds the cross-feature analytics contract, the index of per-feature
coverage files and live admin synchronization evidence. Each feature's current
coverage matrix (route -> Component -> ViewModel -> screen hook or container
reason, callbacks/inputs/operations, omissions), its state-preservation
inventory and its feature-level verification notes live in
`analytics/<feature>/COVERAGE.md`; rows there are replaced to match the current
code, never appended as a dated journal. Dated design and visual-pass narratives
live in `docs/records/`.

## Per-feature coverage files

- [authorization](authorization/COVERAGE.md)
- [confirmation](confirmation/COVERAGE.md)
- [date-picker](date-picker/COVERAGE.md)
- [debug](debug/COVERAGE.md)
- [draft-training](draft-training/COVERAGE.md)
- [duration-picker](duration-picker/COVERAGE.md)
- [error-display](error-display/COVERAGE.md)
- [exercise](exercise/COVERAGE.md)
- [exercise-example](exercise-example/COVERAGE.md)
- [exercises](exercises/COVERAGE.md)
- [goal-setup-suggestion](goal-setup-suggestion/COVERAGE.md)
- [height-picker](height-picker/COVERAGE.md)
- [home](home/COVERAGE.md)
- [iteration-picker](iteration-picker/COVERAGE.md)
- [main](main/COVERAGE.md)
- [menu-picker](menu-picker/COVERAGE.md)
- [month-picker](month-picker/COVERAGE.md)
- [muscle-loading-details](muscle-loading-details/COVERAGE.md)
- [performance-trend-details](performance-trend-details/COVERAGE.md)
- [period-picker](period-picker/COVERAGE.md)
- [primary-goal-picker](primary-goal-picker/COVERAGE.md)
- [profile](profile/COVERAGE.md)
- [secondary-goal-picker](secondary-goal-picker/COVERAGE.md)
- [start-training](start-training/COVERAGE.md)
- [statistics](statistics/COVERAGE.md)
- [training](training/COVERAGE.md)
- [training-goal-details](training-goal-details/COVERAGE.md)
- [training-streak-details](training-streak-details/COVERAGE.md)
- [trainings](trainings/COVERAGE.md)
- [weight-picker](weight-picker/COVERAGE.md)

Every `:analytics:<name>` implementation module has a file. The
confirm-training-completion dialog reports through `TrainingAnalytics` and is
covered in [training](training/COVERAGE.md).

## Admin synchronization evidence

The current reconciliation of code with the live analytics admin (property,
streams, definitions, key events, reports) is the
[Live GA4 setup review](#live-ga4-setup-review--2026-10-14) section below.
Replace its rows when the admin or the emitted contract changes; do not add a
second dated copy.

## Dated records moved out

- [2026-10-14-design-visual-passes.md](../docs/records/2026-10-14-design-visual-passes.md)
- [2026-10-15-design-visual-passes.md](../docs/records/2026-10-15-design-visual-passes.md)

---
# Current analytics contract review (2026-10-14)

This section defines the current cross-feature contract. Feature matrices,
preservation inventories and feature-level verification live in the per-feature
files listed above; admin reconciliation lives in the
[Live GA4 setup review](#live-ga4-setup-review--2026-10-14).

## Collection policy and Settings

Collection starts disabled until a saved grant is applied: the core and both
shells start at Unknown, and only Granted passes events. `analytics_consent` is
a Boolean preference in the existing LocalSettings/DataStore owner; absence and
saved false map to Denied. Onboarding initializes absent consent to true on first
entry and preserves a known false ([authorization](authorization/COVERAGE.md)).
ApplicationBootstrap owns one application-lifetime observation before platform
sink attachment. It applies Granted/Denied through Analytics; read failures deny
collection while the existing preference observation is retried with a delay
increasing from one to thirty seconds. Platform shells still initialize SDK
collection as Unknown before attachment; the Android/iOS startup flags and
adapters disable automatic screen tracking and deny advertising consents. No
feature enables SDK collection. Events observed before consent is known are
discarded, not buffered/replayed; granting consent does not synthesize a screen
view. Adapter acceptance is not remote delivery; early SDK scheduling remains a
runtime check.

Profile Settings owns the only in-app consent control; its guards, loader
eligibility, project Toggle and deliberate absence of a consent-control event are
recorded in [profile](profile/COVERAGE.md). Consent is immediately committed to
the durable owner, not an unfinished form, and is rebuilt through an Infinity
observation; no StateKeeper copy or restore cache exists. LogoutUseCase and
DeleteProfileUseCase call clearUserSettings, atomically preserving this
installation's saved consent and `onboarding_shown` while clearing account
preferences. New installs remain disabled until first onboarding
initialization; its default grant then applies. Jobs/loaders/focus, telemetry
IDs and live dialogs are runtime-only.

## Screens and central actions

Every independent screen ViewModel overrides `screenPresentation` with its typed
FeatureScreen; graph/container ViewModels keep the default null hook, and dialog
ViewModels keep null under the dialog policy. Source inventory: 24 independent
screen VMs have one hook each; nine feature graph containers (Auth, AuthProcess,
ProfileCreation, HomeRoot, Main, Profile, ExercisesRoot, Training, TrainingsRoot)
and the three shared hosts (Root, Dialog, DialogContent) keep null; the 23 dialog
VMs keep null. [Debug](debug/COVERAGE.md) supplies its independent screen hook;
[Main](main/COVERAGE.md) observes the actual central-button and tab callbacks and
keeps the null container hook. Per-feature files hold each owner.

## Reporting decision and compatibility

Operation events identify their owner through feature-specific operation/action
codes and correlate outcomes by operation_id. They omit visible-screen fields
(screen_id, navigation_reason, referrer_screen, dialog_id), because construction,
background work and pause/navigation do not establish operation ownership. The user
confirmed that the app has never been released: every analytics schema is reset to
initial version 1. No released-report compatibility or migration is required.
No substitute origin, referrer, saved provenance or reporting context propagation
is introduced.

Unused FlowContext/EntryPoint/toolkit FlowId and Action/Back/External reasons are
removed from the API. Unused attribution names remain reserved against accidental
custom reuse. The distinct screen-api FlowId remains necessary for route
identity/archive compatibility and is unchanged. No built-in dialog view schema
exists; dialogs have no view or close tracking, and this contract adds no dialog
view API.

Typed screen/dialog dismissal observations and internal actions consistently use
DismissRequested. Their wire event names/action codes use dismiss_requested, including
former back/dismiss encodings. The two long details-dialog event names use bounded
prefixes: performance_trend_dismiss_requested and training_streak_dismiss_requested.
Business callbacks/Direction.Back, guards, confirmation decisions and navigation
remain independently necessary and unchanged. A request is never reported as a
delivered dismissal; host-managed exits remain explicit scoped omissions.
Click intents use Clicked; wheel state changes use SelectionChanged, including
programmatic correction. Parameter shapes and event populations are unchanged.

## Cross-feature omissions and limitations

- `launch_source` is written on every encoded payload with the value `unknown`:
  no caller sets it (`Analytics.updateContext` has no callers), so it is not
  interpreted as attribution or registered as a reporting dimension.
- Dialog host exits/views and undocumented provenance stay explicit omissions,
  not inferred completions.
- Training create completion can return a nullable ID and the goal operation can
  return false; neither is relabelled as a proven business success.
- Onboarding Get started and the shown flag do not themselves grant consent.
  First entry initializes absent consent to true; saved false is preserved;
  events observed before that source update remain discarded. The shown-flag
  policy is entry-based, not completion-based.
- A fact that would need reporting context transported between business or
  navigation owners stays an omission under the feature-analytics observation
  boundary; no context propagation mechanism exists for reporting.

## Pre-release baseline

User decision: no app release exists. All literal analytics schemas, including
built-ins and auth_input_changed, use version 1 (see Verification for the count).
Dismissal observations are named DismissRequested across all public events, common
ControlAction, internal actions/encoders and owning VM callers; obsolete
Back/back/dismiss analytics encodings are removed. This is a telemetry-only
contract cleanup: actual routes/screen hooks, business callbacks/guards,
input/selection/draft owners and navigation are unchanged. No additional state
archive or synthetic callback is introduced. Comparing the UI source before and
after that cleanup confirmed that only typed analytics references changed.
Legacy personal self-test populations are retained separately from the current
operation definitions; no release cutoff is invented.

## Verification

Current source inventory (re-counted on 2026-10-15, read-only): 24
`screenPresentation` overrides, one per entry of the seven FeatureScreen enums (24
entries), all presenting typed values; 59 BaseViewModel subclasses = 24 screen VMs
+ 12 containers + 23 dialog VMs; 148 `AnalyticsEventSchema` literals (147 feature
schemas plus the built-in `screen_view`), all version 1, unique, lowercase and
underscore-only, longest 39 characters against the 40-character limit; 30
`@Module` analytics modules, every one listed in `shared/.../Koin.kt`, with 29
distinct schema qualifiers (Debug registers no schema); DismissRequested declared
in 28 event files (41 variants) plus the common ControlAction, encoded as 25
`<feature>_dismiss_requested` schema names, four prefixed `training_interaction`
action codes and the bare `dismiss_requested` action code of the auth/profile
control schemas; 17 `DRAFT = PersistenceKey(...)` owners (ten with a non-null
`?:` fallback, seven passing a nullable initial state); eight
`@JsonIgnoreUnknownKeys` usages in seven files. Event-name prefixes do not map
one-to-one to modules (`profile_operation_*` lives in authorization,
`exercise_example_details_clicked` in exercise), but names remain unique.

Cross-feature source checks for the previous contract audit (2026-10-14): all 182
public event variants have a real UI reference and an implementation mapping,
with no unused observation variant or open raw-text/error fact; 204 direct
observation sites and 47 operation call sites were inspected; 149 literal schemas
(147 feature schemas plus the two toolkit built-ins of that revision, one of which
was retired on 2026-10-15) had unique,
lowercase, underscore-only names bounded to 40 characters (longest 39) and
version 1; the 30
feature implementation modules are registered once in `shared/.../Koin.kt` with
unique schema qualifiers; implementation visibility, lifetimes, dependency
boundaries and event-failure isolation were reviewed. All 24 typed screen
identities have exactly one leaf owner; the nine feature containers keep null.
Operation adapters expose started/completed/failed/interrupted without raw
errors; complete auth/profile/training caller contracts were traced, including
Result unwraps and nullable/Boolean outcomes, and operations omit mutable
navigation attribution while preserving business results, errors and
cancellation. No raw credential/body/entity/error fields or obsolete
Back/provenance API facts were found. Generated Koin output confirms the
MainAnalytics/DebugAnalytics bindings and the unique main_analytics catalog; the
screen-api route FlowId/archive identity is preserved. Consent
revision/queue/configuration checks, stale presentation close and worker
cancellation/error boundaries were inspected in real source; EN/UK/RU XML
resources and the disabled Android/iOS SDK defaults were inspected. Literal
schema/context parameter budgets and `git diff --check` passed.

Builds for that revision: `:shared:compileAndroidMain
:shared:assembleSharedDebugXCFramework` passed offline without configuration
cache (exit 0, BUILD SUCCESSFUL, 3m 17s), including device and simulator
frameworks. An earlier integration found a missing explicit API type on
DebugViewModel's public hook; after its correction and the final UI
colors/accessibility, source retry and internal schema-name cleanup, the exact
final source passed `:androidApp:assembleDebug :shared:assembleSharedDebugXCFramework`
with `--offline --no-configuration-cache` (exit 0, BUILD SUCCESSFUL, 1m 18s);
both device and simulator debug frameworks were linked and packaged. Existing
global opt-in/deprecation warnings remain. This validates compilation/export,
not SDK delivery.

Not verified for that revision: generated DI/runtime, rendered widgets, native
restoration, provider sign-in, reporting-failure and cancellation injection,
remote event delivery, opt-in/off across restart, settings write/read failure,
installation consent retention across logout/delete and native lifecycle. No
automated test infrastructure was introduced. Independent review was unavailable
under that chat's delegation restrictions; self-review is not independent review.

---
# Live GA4 setup review — 2026-10-14

Owner-confirmed release boundary: the mobile app is unreleased; existing history
is personal testing, not a released-user cohort or a product baseline. No launch
date, production migration or historical backfill is assumed. Record the actual
release boundary when it occurs. This entry is setup evidence, not project policy.
Later admin reconciliations are merged into this section; it is replaced, not
copied, when the admin or the emitted contract changes.

Verified property: Grippo Analytics / `grippo-e3960`, property `519002689`.
Affected streams: Android `com.grippo.android` (`G-0YXY3NRJ7J`) and iOS
`com.grippo.app` (`G-PBKNNR7JHE`); account `379805710`, Android stream
`13265245917`, iOS stream `13348167981`; web stream `13688917680` is outside
app reports. Saved detail reports and the Explore funnel
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
| `home_task_started/completed/failed/interrupted`, `home_operation` = `refresh_trainings/refresh_exercise_examples/save_period` | Bounded Event-scoped dimension `Grippo Home operation` (`home_operation`), registered 2026-10-14 as the twelfth Grippo event dimension and as the thirteenth primary selector of diagnostic report `События и экраны` (16123956244); the saved definition was reopened and verified (name, description, Event scope, immutable parameter). Refresh failures are separable by their actual Home owner; exercise-example first loading belongs to Home, not auth/profile. No new event, key mark or derived success rule. Registration establishes no historical backfill; receipt of the parameter from the current Android/iOS build is unverified. |
| `exercise_task_started/completed/failed/interrupted`, `exercise_operation=load_history` (version 1); terminal `duration_ms`; existing parameterless `exercise_example_details_clicked` / `exercise_dismiss_requested` | **Keep; live consumer reconciliation verified.** Account `Grippo Analytics` (`379805710`) / property `grippo-e3960` (`519002689`) and both mobile streams were matched to the synced Firebase app identifiers. Saved diagnostics `События и экраны` (`16123956244`) uses Event name, Event count and Total users with only Platform regex `^(Android\|iOS)$`, without an event allowlist; all six Exercise event names are eligible when delivered. Keep the registered Event-scoped Milliseconds metric `duration_ms` for separate mobile analysis of `^exercise_task_(completed\|failed\|interrupted)$`; this diagnostics report does not display duration, and no saved Exercise latency report is claimed. `exercise_operation` is unregistered and is not an available report filter: this sole operation is identified by its unique event family. Deliberately omit a dimension until other operations require reassessment, and omit a derived rule/key mark. Neither mobile stream has modification rules; their two exact profile/training derived rules do not match Exercise. Seven existing key events, the two audiences, profile/training report and training exploration/funnel have no Exercise dependency. No admin write required or made. The operation covers refresh, reconciled reads and mapping; completion does not establish a prior result or a rendered comparison. Current-build SDK receipt remains unobserved. |
| `auth_session_check_started/completed/failed/interrupted` | Raw event-name diagnostics only. The decorator measures `hasProfile` and runs only after the shown-onboarding gate when a token exists; first onboarding always routes to Login, so no session check precedes it. Collection requires granted consent; completion does not prove successful navigation. No destination/provenance dimension. |
| `auth_attempt_started/succeeded/failed/interrupted` with `auth_action`/`auth_method`; `profile_operation_*` with `profile_action` | Existing bounded definitions describe the complete revised caller blocks. Login/Register/CreateProfile no longer request exercise examples (that refresh is Home's), so older test builds' auth/profile failure populations and durations are not an unchanged baseline. The `auth_action` description was corrected to describe the invoked email/provider login versus email registration scenario without inferring account novelty; its saved description, Event scope and unchanged parameter were re-opened and verified. |
| `auth_control_clicked` for Onboarding `next`/`dismiss_requested`; built-in screen views | Existing interaction/screen definitions cover them. Checkbox/default consent writes deliberately emit no event; navigation changes add no synthetic click/view. |
| Bounded event parameters | Event dimensions: `auth_action`, `auth_method`, `profile_action`, `profile_operation`, `screen_id`, `training_operation`, `option`, `training_action`, `interaction_screen`, `interaction_action`, `main_tab`, `home_operation` (twelve). Saved rows verified. |
| `duration_ms` | Saved event-scoped custom metric `Grippo Operation duration ms`, unit Milliseconds. Terminal-operation elapsed time, not workout length. Filter the event/operation population and choose aggregation explicitly; copied source/derived values are not separate operations. |
| `duration_picker_submit_clicked` v1 (empty parameters); `training_interaction` v1 with `training_action=completion_confirmation_confirm` | Source eligibility requires `DurationFormatState.Valid` before either observation and its result navigation; Empty/Invalid callbacks emit nothing. Schemas, encoders, consent and transport are unchanged; no workout duration is sent. Current Android/iOS stream rules, key events, reports, funnels, audiences and SDK receipt are uninspected under the requested source-only review. Existing saved `training_action` registration is historical evidence, not a current synchronization claim; no admin action or migration conclusion is established. |
| `draft_training_continue_clicked` / `draft_training_start_new_clicked` v1 (empty parameters); `draft_training_task_started/completed/failed/interrupted` v1 with `draft_training_operation=delete_draft` and terminal `duration_ms` | Source guards reject callbacks while opening or deleting the draft, before click observations or deletion operations; an absent draft closes programmatically without a dismissal observation. Schemas and encoders are unchanged. Current Android/iOS definitions, stream rules, key events, reports, funnels, audiences and SDK receipt are uninspected under the requested source-only review; no admin action or synchronization conclusion is established. |
| Random operation IDs, secrets, unused identity/context | No custom definitions. `launch_source` remains unknown without a real caller; do not interpret it as attribution. |
| Built-in `dialog_viewed` (`dialog_name`) | Retired from `BUILT_IN_EVENTS` on 2026-10-15 by the owner's decision. Never emitted by any build in repository history (no constructor call in any commit), so no received data from this codebase exists. The 2026-10-14 live review recorded no definition, derived rule, key event, dimension or report for it. Current admin state not inspected: the owner explicitly restricted this change to source-only scope. |
| Legacy `registration_completed`, `workout_started`, `workout_completed` | Historical observations have initialization/entry semantics; do not merge them with the new successful-operation population. Raw history retained. |
| Goal operation with Boolean result | No goal-success key event: successful return may contain false. |
| `start_training_task_started/completed/failed/interrupted` v1 with `start_training_operation=build_options` and terminal `duration_ms`; separate `start_training_option_confirmed` v1 with `option=empty/preset/recent` | BuildOptions retains its local boundary: one cache snapshot, the complete preset use case, mapping and State update. The required remote refresh precedes this operation; its Result now propagates failures through the shared error pipeline, before any BuildOptions fact. Completion may contain no generated preset and proves neither availability nor workout creation. Custom selection is available independently while options load; unknown keys emit no selection observation. Schemas/encoders are unchanged. Current Android/iOS stream rules, definitions, key events, reports, funnels, audiences and SDK receipt are uninspected under the requested source-only review. The saved `option` registration is historical evidence, not current reconciliation; no admin action, generation-success key or availability rate is inferred. |
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
  read back from the ordinary published report after saving. A thirteenth
  selector, `Grippo Home operation` (`home_operation`), was added in the later
  startup follow-up; after saving, the reopened report showed the Home selector,
  the original twelve selectors, the Event name default, Event count / Total
  users and the `^(Android|iOS)$` platform filter. Saving report data cleared
  its Library description again; it was restored and extended (event/user
  units, failure versus interruption, possible already-created profile,
  operation selectors, separate Home exercise refresh, the shown-onboarding
  SessionCheck gate, the older-build semantic boundary and screen/SDK context)
  and the complete saved text was re-read in Rename. This makes the
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
  dimensions cover them. Later readbacks for the sequential-startup and
  onboarding-welcome revisions found that the existing screen/control
  definitions, both key events and the Library descriptions of `События и
  экраны` and `Профиль и тренировки` still describe observed views/events and
  complete operation blocks, without claiming exercise-cache readiness, an
  explicit opt-in event or a consent parameter; no admin object needed a
  mutation or deletion for those revisions, and only the Home dimension above
  was added. At that earlier inspection the placeholder had no opt-in before login
  and default Denied discarded onboarding/auth telemetry; the current product
  rule initializes absent consent to granted on first onboarding entry (see the
  Onboarding row above), while a saved denial still suppresses events. No
  complete fresh-install denominator or delivery is asserted, and no concurrent
  feature work was changed for an admin report.
- During the original onboarding source pass a dedicated Chrome browser binding
  was unavailable; native automation followed the active window, and a separate
  tab/window attempt was switched by parallel editing. That pass inspected the
  property, project `grippo-e3960`, the observed profile→training funnel and
  its Android/iOS filter, matched local app configs to the project, and wrote or
  removed no admin object; the authorized review recorded here completed the
  reconciliation.
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
  selected. Rules, key marks and units were retained. Neither mobile stream has
  event modification rules, and saved audiences/segments have no custom event
  dependencies.
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
  names (148 after the 2026-10-15 retirement of the built-in dialog schema), all
  satisfying lowercase/underscore naming and the 40-character limit
  (longest 39); this is a source-literal check, not compiled catalogue or receipt
  evidence.

Further review on 2026-10-14 reconciled the current authorization summary with
the nine-owner inventory, including Onboarding's actual guards/callbacks and
Splash's shown-state gate. Removed route-entry reason codes are no longer
described as current telemetry contracts; the eight-hook/build evidence remains
explicitly historical to the October 13 repair. The prerelease migration statement
now preserves the separate received legacy self-test population. Independent
re-review found no remaining discrepancies in these corrections.

Fresh live readback confirmed the eleven Event dimensions registered at that
time (`home_operation` was added afterwards, see the table), the operation-duration
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
cover 147 feature schemas plus one toolkit built-in, `screen_view`, after the
2026-10-15 retirement of the built-in dialog schema (148 unique, all v1, longest
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
