# Draft-training dialog coverage

Record owner: [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md)
and [state-preservation](../../.claude/skills/state-preservation/SKILL.md); records
policy: [CLAUDE.md](../../CLAUDE.md#records). Cross-feature contract and
verification limits: [current analytics contract review](../COVERAGE.md#current-analytics-contract-review-2026-10-14);
admin reconciliation: [Live GA4 setup review](../COVERAGE.md#live-ga4-setup-review--2026-10-14).
Rows describe the current code of `:ui-dialog-features:draft-training` and
`:analytics:draft-training`; replace them when that code changes. Typed dismissal
observations use DismissRequested with `dismiss_requested` wire names; operation
schemas omit visible-screen context and correlate outcomes by `operation_id`; the
app is unreleased and every schema is version 1.

## Surfaces and screen hooks

| Route / rendered surface | Component → ViewModel | Presentation | Coverage |
|---|---|---|---|
| `DialogConfig.DraftTraining(title, onContinue, onStartNew)` → DialogContentComponent factory → DraftTrainingScreen (durable draft summary, display-only exercise cards, Start new, Continue; Draft and Add stages render identical strings) | DraftTrainingComponent → retained DraftTrainingViewModel | Live bottom sheet; default null screen hook, no screen enum, no nested independent screen or container in the module | Stateless service and TrainingFeature injected inside retainedInstance; Essenty BackCallback → `onBack`; event listener maps Continue/StartNew/Back to the config closures and host back |

MainViewModel opens the dialog from `resumeOrStartTraining()` inside
`safeLaunch(loader = StartTraining)` when `getDraftTraining().first()` is nonnull;
that method runs after Main's own central-button click (`StartTrainingClicked`,
[main](../main/COVERAGE.md)) and again as the dialog's `onStartNew` closure. The
config defines no `onDismiss`, so Main learns nothing on swipe; any two
DraftTraining configs compare as the same content. The `training_draft` deeplink
opens Recording in the Draft stage directly and never shows this dialog.

## Callbacks and operations

| Callback / operation | Observation | Eligibility and payload |
|---|---|---|
| onContinue (Continue button) | ContinueClicked / `draft_training_continue_clicked` v1 | No eligibility/busy guard; empty payload; then the Continue direction → Main navigates to `Training(StageState.Draft)` |
| onStartNew (Start new button) | StartNewClicked / `draft_training_start_new_clicked` v1 | No guard and no loader; the button is never disabled, so repeated taps emit repeated clicks and start repeated DeleteDraft operations; the continuation re-enters `resumeOrStartTraining` after the host release, re-reads a null draft and starts a new training (goal suggestion or `Training(Add)`) without a second central-button click |
| StartNew deletion | DeleteDraft / `draft_training_task_started` [draft_training_operation = delete_draft] and `draft_training_task_completed/failed/interrupted` [draft_training_operation, duration_ms] v1 | Decorates the complete existing `deleteDraftTraining().getOrThrow()` inside safeLaunch; navigation follows successful deletion after the decorator in the same safeLaunch; failure goes to the existing error pipeline with no navigation. The repository returns success unconditionally, so `failed` reflects only a thrown DAO exception |
| onBack (the Component's Essenty BackCallback; the screen has no Cancel) | DismissRequested / `draft_training_dismiss_requested` v1 | Empty payload; request only |
| Programmatic navigation, draft observations, exercise cards; host dismissal routes (swipe/outside, toolbar close, the host's Compose BackHandler and Essenty callbacks) | Deliberate omission | No interaction events and no input/status/picker facts; host routes do not call this VM; which system-Back route reaches the child BackCallback is not established from source |

The shared decorator supplies operation ID, monotonic duration and
started/completed/failed/interrupted outcomes, preserving results and exception
identity with isolated reporting failures (`trackEvent` catches Exception; the
failed payload ignores exception content). No training IDs, exercise values, text
or exception details are reported; operation payloads receive the operation-id
context and no visible-screen context.

## Catalog

`DraftTrainingAnalyticsModule` (`@Module @ComponentScan`) binds
`DraftTrainingAnalyticsImpl` as `DraftTrainingAnalytics` and registers the three
parameter-free interaction schemas and the four operation schemas (seven in total)
through one qualified `draft_training_analytics` provider; `shared/.../Koin.kt`
lists it once, and the module depends only on `analytics-api`. The API exposes
`trackEvent` and `trackOperation`; `DraftTrainingOperation` has the single value
DeleteDraft.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| DraftTrainingDao (durable owner) | Draft training, exercises and iterations | Preserve in the existing durable owner | `get(): Flow`, insert-or-replace (delete plus insert), `delete()`; `setDraftTraining` returns success without writing when no active profile exists | Survives process recreation; written by the recording screen's draft saves; removed by Start new; no duplicate local snapshot |
| DraftTrainingViewModel / DraftTrainingState (`@Immutable`, not serializable) | `stage` (default Draft) and `exercises` (default empty) | Rebuild | `getDraftTraining()` observed with the default WhileActive processing → existing domain/state mapping with fresh random presentation IDs; stage becomes Add or Edit(trainingId) | A fresh VM rebuilds from the current durable draft; a null emission is ignored and does not clear previously rendered exercises; no DRAFT, loader members or `var` |
| Dialog host / component | Live dialog session, `onContinue`/`onStartNew`/back closures, pending navigation | Runtime only | Host slot/step navigation `serializer = null`; retainedInstance | Ordinary recreation retains the VM while InstanceKeeper survives; process death closes the session; fresh reopening re-subscribes to the DAO |
| MainViewModel / MainComponent | No draft (data-object state) | Runtime only | `StartTraining` loader only while the launch runs; the tab stack is owned by MainTabNavigation | The loader disables the central button only until `open` returns |
| TrainingRecording (Continue target) | Draft loaded through `StageState.Draft` | Rebuild at that owner, then preserved in `TrainingRecordingDraft` | LoadDraft runs only when the restored exercises are null | An existing Recording draft wins over a reload ([training](../training/COVERAGE.md)) |

There are no editable inputs, filters, selections or accepted child results in
this dialog; Continue and Back modify no parent draft. A StateKeeper supplier
would duplicate data-backed presentation and cannot restore the unsaved dialog
session, so none exists. Random presentation IDs are rebuilt and are not saved
business identity. No shell, navigation serialization, state initialization or UI
layout change belongs to this feature.

## Verification

Source gates for the current revision were reconciled against the opener, the
deeplink bypass, all three event consumers, exhaustive event/operation encoding,
the decorator placement, DI registration, privacy and the dialog
screen/preservation classifications (read-only audit on 2026-10-15; no build was
part of it). Checks recorded for the implementing revision (2026-10-13):
source/diff review and independent read-only review found no blocking scoped
findings; generated Koin code confirmed the DraftTrainingAnalytics binding and
qualified schema registration; both draft-training modules compiled for Android
(`compileAndroidMain`) and iOS Simulator (`compileKotlinIosSimulatorArm64`) in a
scoped offline build without configuration cache (BUILD SUCCESSFUL, 15s; fresh
rerun 9s). The residual review found no orphan APIs/files/events, duplicate
registration, template remnants, missing event consumers or preservation-owner
gaps. Shared integration compilation stayed unverified by this feature's builds:
the combined attempt failed on missing generated screen-api metadata, the isolated
shared retry on missing google-auth metadata (.klib), and a later retry on missing
generated exercises metadata (`build/classes/kotlin/metadata/commonMain/default/linkdata`).
Concurrent builds were active; configuration-cache lock waiting was confirmed with
a thread dump and avoided with the command-line flag only. No unrelated
infrastructure or source was changed to resolve those failures; no automated test
sources or infrastructure were introduced.

Manual checks pending: Continue, Start new (including repeated taps) and Back;
delete success/failure/cancellation; reporting-failure injection; ordinary
component recreation; fresh process restart with a closed dialog followed by
reopening the durable draft; Room first-emission timing relative to the sheet;
which system-Back route reaches the child BackCallback; SDK delivery. Compilation
alone does not prove these runtime scenarios; no feature/session restoration
guarantee beyond the inventory above is claimed.
