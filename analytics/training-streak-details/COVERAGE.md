# Training streak details coverage

Scope: `:ui-dialog-features:training-streak-details`, 2026-10-13.

## Surface and analytics gate

Actual entry: HomeViewModel.onOpenTrainingStreak checks the current range, emits
its existing HomeEvent.TrainingStreakClicked and opens DialogConfig.TrainingStreakDetails.
DialogContentComponent constructs TrainingStreakDetailsComponent with that range;
its retained TrainingStreakDetailsViewModel renders TrainingStreakDetailsScreen.
This is one read-only bottom sheet, with no nested flow or independent screen.
The VM keeps default null screenPresentation. No screen enum or dedicated dialog
presentation contract is required or introduced. Surface coverage gate passes source
review of config, factory, Component, VM and rendered content.

| Owner / callback | Observation | Eligibility / payload |
|---|---|---|
| TrainingStreakDetailsViewModel.onBack | DismissRequested / training_streak_details_dismiss v1 | Existing callback has no guard; empty parameters, emitted before existing Back direction |
| HomeViewModel.onOpenTrainingStreak | Existing HomeEvent.TrainingStreakClicked | Nonnull range guard; unchanged parent-owned opening intent |
| observeTrainings / provideTrainingStreak | Deliberate omission | Reactive data rebuilding and metric calculation with current user experience, no explicit request or submission to decorate |

The Component registers a Decompose BackCallback targeting the VM onBack.
However, DialogScreen also installs a Compose BackHandler targeting the host
DialogViewModel.onDismiss(null); toolbar Back and Close are host-owned too.
These paths, swipe and scrim dismissal can bypass the leaf VM and emit no
training_streak_details_dismiss event. The event therefore observes only the VM
callback request, not every system Back, close, delivered dismissal or cancellation.
Views/close, scrolling and passive cards are omitted. No callbacks, route context,
operation scaffolding, lifecycle jobs or reporting state are added solely for telemetry.
No ranges, dates, training IDs, metric values, raw text or exceptions are encoded.
The internal exhaustive encoder owns one versioned empty schema. The annotated
module registers its qualified training_streak_details_analytics catalog; shared
wires the module and UI depends only on analytics-api. Ordinary reporting Exceptions
cannot prevent Back navigation; fatal Errors propagate. API consumers and retained
VM injection are reconciled against actual source.

## State preservation gate

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| Component / VM | Immutable range input and its display | Runtime only for live dialog | Caller range, DateRangeFormatState.of, retainedInstance | Ordinary recreation retains VM; reopening uses current caller range |
| VM | streak, insights and timeline | Rebuild | TrainingFeature.observeTrainings, TrainingStreakUseCase.fromTrainings (reads UserFeature experience), existing toState mapper | Fresh VM starts with null streak; empty trainings clear it; nonempty data rebuild presentation |
| Dialog host | Session, callbacks and Back direction | Runtime only | Slot and step serializer = null | Process death discards live dialog; reopening constructs a fresh session |
| Foundation / widgets | Jobs, loaders, scrolling and telemetry provenance | Runtime only | Existing scope and widget ownership | No business draft supplier or synthetic callbacks |

No editable inputs, selections, filters, accepted results, parent drafts or secrets
are owned here. The immutable range is a seed, not an editable filter. Source gate
passes with no StateKeeper snapshot required: session persistence would conflict
with the dialog policy, while presentation is rebuilt from its existing data source.
Initialization, empty data handling, Back and parent opening behavior remain unchanged.
No feature storage, serialization plugin or platform lifecycle bridge is introduced.

## Initial verification (superseded compilation limits)

Self-review of actual source/diff covered observations, null dialog screen hook,
complete API consumers, schema/privacy, reporting isolation, module boundaries,
retained injection and single settings/shared registration. Inline static checks
and git diff --check passed. No automated test sources or infrastructure were added.
Independent review remains unperformed: delegation is not authorized and the available
review connector only reads PR CI diagnostics.

Scoped Android and iOS Simulator compilation of analytics and UI was attempted
with --offline --no-configuration-cache. The sandbox initially denied the Gradle
wrapper cache lock; an authorized escalated attempt then failed during shared build
script configuration on an unrelated unresolved projects.analytics.weightPicker
reference. After the missing weight-picker registration appeared in concurrent changes, a
repeat attempt started a new daemon but did not reach feature compilation. Its
Daemon worker thread dump confirmed DefaultFileLockManager waiting while hashing
build inputs. Only this attempt's wrapper process was terminated; unrelated Gradle
processes and caches were not modified. Feature compilation, generated Koin and
shared runtime wiring remain unverified.

Manual runtime checks remain unrun: Back reports its sole request and still closes
when reporting throws an Exception; host Back/Close and swipe/scrim do not synthesize a VM event;
ordinary recreation retains the dialog VM; process death removes the live dialog;
reopening rebuilds current streak and handles empty trainings. Device lifecycle,
restoration and SDK delivery are not proved by source checks.

## Residual review (2026-10-13)

Rechecked API consumers, all module/DI registrations, event catalog uniqueness,
privacy, reporting isolation, actual entry/render/Back owners and state ownership.
No scoped production defect or dangling API/template was found. Source checks
confirmed the v1 name meets the actual 40-character, lowercase pattern and
reserved-name/prefix rules; its sole definition and registrations are unique.
The empty payload and exception boundary preserve the existing Back navigation.

Corrected coverage to distinguish the leaf VM callback from DialogScreen's
host-owned Compose/system Back and toolbar Back/Close paths, which bypass that
callback. Host closure observations remain deliberate omissions; no route/config
context or shared-host telemetry is added. Also made the use case's current
UserFeature experience read explicit in the rebuilt-state inventory. The dialog
still has no editable draft or accepted result requiring a local StateKeeper.

The earlier compile command used a nonexistent compileKotlinAndroid task. A fresh
run reached configuration and exposed this mistake; Gradle tasks --all completed
successfully. Verification now uses compileAndroidMain and
compileKotlinIosSimulatorArm64 for both the analytics and UI modules.

Final scoped build passed: BUILD SUCCESSFUL in 3m 2s, exit 0, for all four
requested compile tasks:

```sh
./gradlew :analytics:training-streak-details:compileAndroidMain :ui-dialog-features:training-streak-details:compileAndroidMain :analytics:training-streak-details:compileKotlinIosSimulatorArm64 :ui-dialog-features:training-streak-details:compileKotlinIosSimulatorArm64 --offline --no-configuration-cache --no-watch-fs --configure-on-demand --project-cache-dir /private/tmp/grippo-training-streak-review-cache
```

This supersedes the earlier unverified feature compilation and Koin generation
claims. Actual generated Koin source was reviewed: one service singleton bound to
TrainingStreakDetailsAnalytics and one qualified AnalyticsSchema singleton.
The build emitted existing convention opt-in/dependency and upstream source
warnings; no scoped compilation errors. No production or build configuration
changes were needed for this review. git diff --check passed after report edits.
Shared application compilation/runtime DI resolution, device lifecycle, actual
reporting-failure injection, SDK delivery and independent review remain unverified.
