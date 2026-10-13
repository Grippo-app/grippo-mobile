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
