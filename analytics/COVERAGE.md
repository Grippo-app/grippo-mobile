# Scoped Home and Training flows coverage

Reviewed during the project-skill cleanup on 2026-10-13. This is an inventory of
actual affected owners, not a template or a claim of global feature/SDK coverage.
Update it when these owners or their instrumentation change.

| Surface / route | Component → ViewModel | Presentation identity | Source coverage |
|---|---|---|---|
| Home dashboard (main Home tab) | HomeComponent → HomeViewModel | HomeScreen.Home / home | Typed VM hook, retained-factory injection, foundation lifecycle binding |
| Home graph | HomeRootComponent → HomeRootViewModel | Default null | Container renders child; no duplicate presentation |
| Training calendar, Daily/Monthly modes | TrainingsComponent → TrainingsViewModel | TrainingsScreen.Calendar / trainings_calendar | Typed VM hook; one owner for both display modes |
| Training calendar graph | TrainingsRootComponent → TrainingsRootViewModel | Default null | Container renders its child; no duplicate presentation |
| Training recording, Add/Edit/Draft entry | TrainingRecordingComponent → TrainingRecordingViewModel | TrainingScreen.Recording / training_recording | Typed VM hook; one screen for all modes |
| Training exercise editor | TrainingExerciseComponent → TrainingExerciseViewModel | TrainingScreen.Exercise / training_exercise | Typed VM hook; original navigation/result owner |
| Training graph | TrainingComponent → TrainingViewModel | Default null | Container renders stack; no duplicate presentation |
| DraftTraining, StartTraining, TrainingGoalDetails dialogs | Respective dialog VM | No dedicated presentation contract | Live dialog sessions remain outside independent-screen tracking |

HomeAnalytics, TrainingAnalytics and TrainingsAnalytics expose only presentScreen. Implementations
encode existing built-in presentation schemas; no event/operation scaffolding,
UI/domain imports or feature lifecycle observers are added. Shared registers the
generated annotated modules. Foundation supplies Initial/Restore and closes on
pause/destroy. The unchanged completed/library pages are outside this affected
inventory; no screen enum entry claims them.

The following observations are deliberately uninstrumented in this change:
Home controls/filter/goal operations, calendar date/period/menu controls,
recording submit/add/edit/delete/reorder,
exercise field/iteration callbacks and dialog open/accepted-result callbacks.
This task repairs mapping/ownership/preservation and adds required affected-screen
presentations; it does not define new behavioral telemetry or change eligibility.
No raw input, entity ID, body measurement, credential or exception payload is
encoded by the new feature analytics APIs.

## Preservation inventory

| Owner | Field/group | Policy | Source/key and restoration |
|---|---|---|---|
| HomeViewModel | Range/filter selection | Rebuild from existing persistent owner | LocalSettings observation; no duplicate StateKeeper range owner |
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

Reviewed and reconciled with actual routes, rendered surfaces, Components and VMs
on 2026-10-13. This section covers authorization and its profile-creation children.

| Route / surface | Component → ViewModel | AuthScreen / wire identity | Hook |
|---|---|---|---|
| AuthRouter.Splash / launch loading | SplashComponent → SplashViewModel | Splash / auth_splash | Typed screenPresentation |
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

AuthProcess entry reasons (LaunchWithoutSession, TokenAbsent, SessionCheckFailed,
ProfileSetupReturn and legacy RestoredScenario) share the Login owner; route identity and entry reason are not telemetry.
Splash can navigate to Home, AuthProcess or ProfileCreation. Successful login or
registration can navigate to Home or ProfileCreation; these paths do not add
screen owners. Restored stacks use the same retained factories and typed hooks.
All eight leaf factories resolve AuthAnalytics only inside retainedInstance.
BaseComponent binds the VM hook before activation/navigation bridging; foundation
owns Initial/Restore and close on pause/destroy. Containers resolve no screen service.

| Owner | Observed callbacks / inputs | Complete operations and eligibility |
|---|---|---|
| SplashViewModel | Back | SessionCheck wraps hasProfile only when a session token exists; cache hit and remote lookup both complete normally; startup retry remains business-owned |
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

From this change, AuthEvent.User.PickerConfirmed encodes `auth_picker_confirmed`
version 1 with interaction_screen, input_field and input_status. It no longer
encodes `auth_input_changed`. `auth_input_changed` version 2 has the same parameter
shape as version 1 but a narrower population: text validation-status transitions
for email/password/name only. Existing version-1 height/weight rows represent
accepted picker results, not validation transitions. Reports spanning releases
must count picker confirmations as version-1 auth_input_changed filtered to
height/weight plus auth_picker_confirmed; text-transition reports must filter to
email/password/name across both versions. Do not compare unfiltered input-change
counts across the version boundary. No dual emission or historical rewriting is
performed. Existing field/action wire codes are unchanged by exhaustive encoding.

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

This repair changes only internal event encoding/schema and coverage documentation.
It introduces no screen, callback, editable field, navigation change or state owner.
UserViewModel still updates its accepted picker input before emitting the typed
confirmation; UserComponent saves name/weight/height through UserDraft and
UserState.serializer(). No telemetry state or restoration callback replay is added.
Existing LoginDraft/RegistrationDraft exclude passwords and provider availability;
profile parent draft, experience and catalog selections remain owned by their
existing local StateKeeper suppliers. No archive/schema migration is needed.

Source review covers the eight leaf hooks, three null container hooks, retained
injection, catalog registration, actual callback eligibility, complete operation
blocks, reporting isolation and cancellation propagation. Static verification
checks the dedicated picker mapping, complete schema catalog, exhaustive encoding
with unchanged codes and diff whitespace. No automated test infrastructure was
introduced. Manual device restoration, payload delivery, lifecycle timing and
reporting-failure injection were not run; source checks do not prove runtime behavior.

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
clicks; system stack handling may navigate without invoking a leaf Back callback.

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
Follow-up verification: source invariants and git diff --check passed; final
:ui-screen-features:profile:compileKotlinIosSimulatorArm64 and
:shared:compileKotlinIosSimulatorArm64 passed (BUILD SUCCESSFUL). Device runtime
and Android compilation were not rerun for this contract-only refinement.

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
| onBack | Back from toolbar/system callback; root forwarding adds no duplicate event |
| onQueryChange | Exact text updates draft; telemetry deliberately omitted: search has no existing validation status, raw text/length is never reported |

Four custom version-1 schemas: exercises_filter_clicked (bounded library_filter),
exercises_exercise_selected, exercises_load_next_page, exercises_back (no fields).
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
