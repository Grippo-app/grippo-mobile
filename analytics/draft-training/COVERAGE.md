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
| `DialogConfig.DraftTraining(title, onContinue, onStartNew)` → DialogContentComponent factory → DraftTrainingScreen (fixed introduction, exercise/filled-set/saved-time summary and contents heading; scrolling display-only exercise rows; pinned Continue and Start new over the shared bottom gradient with stage-specific discard guidance; Draft and Add stages share copy) | DraftTrainingComponent → retained DraftTrainingViewModel | Live bottom sheet; default null screen hook, no screen enum, no nested independent screen or container in the module | Stateless service and TrainingFeature injected inside retainedInstance; Essenty BackCallback → `onBack`; event listener maps Continue/StartNew/Back to the config closures and host back |

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
| onContinue (Continue button) | ContinueClicked / `draft_training_continue_clicked` v1 | The screen disables Continue while any loader is active; the VM rejects the callback before analytics under the same condition; empty payload; accepted Continue direction → Main navigates to `Training(StageState.Draft)` |
| onStartNew (Start new button) | StartNewClicked / `draft_training_start_new_clicked` v1 | The screen disables Start new during the opening query and displays the shared button loading state during deletion; the VM rejects callbacks before analytics while any loader is active. After successful deletion, the continuation re-enters `resumeOrStartTraining` after host release, re-reads a null draft and starts a new training (goal suggestion or `Training(Add)`) without a second central-button click |
| StartNew deletion | DeleteDraft / `draft_training_task_started` [draft_training_operation = delete_draft] and `draft_training_task_completed/failed/interrupted` [draft_training_operation, duration_ms] v1 | Decorates the complete existing `deleteDraftTraining().getOrThrow()` inside safeLaunch with `DeleteDraft` loader; navigation follows successful deletion after the decorator in the same safeLaunch; failure goes to the existing error pipeline with no navigation. The repository returns success unconditionally, so `failed` reflects only a thrown DAO exception |
| onBack (the Component's Essenty BackCallback; the screen has no Cancel) | DismissRequested / `draft_training_dismiss_requested` v1 | Empty payload; request only |
| Opening draft query, programmatic navigation, exercise cards; host dismissal routes (swipe/outside, toolbar close, the host's Compose BackHandler and Essenty callbacks) | Deliberate omission | No interaction events for the passive summary/rows and no input/status/picker facts; an absent draft closes through Back without a synthetic dismissal event; host routes do not call this VM; which system-Back route reaches the child BackCallback is not established from source |

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
| DraftTrainingDao (durable owner) | Draft training, exercises and iterations | Preserve in the existing durable owner | `get(): Flow`, insert-or-replace (delete plus insert), `delete()`; `setDraftTraining` returns success without writing when no active profile exists | Survives process recreation; written by the recording screen's draft saves; removed by Start new; no duplicate local archive |
| DraftTrainingViewModel / DraftTrainingState (`@Immutable`, not serializable) | `stage` (default Draft), `exercises` (default empty), `duration` (default Empty); summary counts are derived from exercises | Rebuild | One `getDraftTraining().first()` query in VM-owned safeLaunch → existing domain/state mapping with presentation IDs generated once for the live sheet; stage becomes Add or Edit(trainingId); DurationFormatState.of maps saved whole-minute duration, including Empty for known zero | A fresh VM reads the current durable draft once. An absent draft emits Back instead of retaining or substituting a draft. The read-only decision snapshot stays stable during ordinary pause/resume and deletion; reopening reads the durable source again. No duplicate DRAFT supplier, initialization flags or shadow source cache |
| DraftTrainingViewModel | `Draft` and `DeleteDraft` loaders, opening query and deletion job | Runtime only | Foundation `loaders` and VM-owned safeLaunch | `Draft` hides loaded content and disables actions until the query completes; `DeleteDraft` disables Continue and makes Start new loading. VM guards reject actions during either operation before emitting events. Shared error/cancellation handling releases loaders; no local fallback or retry wrapper |
| Dialog host / component | Live dialog session, `onContinue`/`onStartNew`/back closures, pending navigation | Runtime only | Host slot/step navigation `serializer = null`; retainedInstance | Ordinary recreation retains the VM while InstanceKeeper survives; process death closes the session; fresh reopening queries the DAO |
| MainViewModel / MainComponent | No draft (data-object state) | Runtime only | `StartTraining` loader only while the launch runs; the tab stack is owned by MainTabNavigation | The loader disables the central button only until `open` returns |
| TrainingRecording (Continue target) | Draft loaded through `StageState.Draft` | Rebuild at that owner, then preserved in `TrainingRecordingDraft` | LoadDraft runs only when the restored exercises are null | An existing Recording draft wins over a reload ([training](../training/COVERAGE.md)) |

There are no editable inputs, filters, selections or accepted child results in
this dialog; Continue and Back modify no parent draft. A StateKeeper supplier
would duplicate data-backed presentation and cannot restore the unsaved dialog
session, so none exists. Random presentation IDs are rebuilt and are not saved
business identity. The displayed duration and counts are rebuilt facts, not a
second draft owner. No shell or navigation serialization changes are required.

## Admin impact

| Source contract / consumer | Current impact | Admin state |
|---|---|---|
| ContinueClicked, StartNewClicked and DeleteDraft operation population | Event/operation models, payloads, schemas, encoders, hooks, consent and transport are unchanged. Callback guards precede analytics; attempts while opening or deleting emit no accepted-click or operation facts | Uninspected under the requested source-only review; no live configuration or synchronization conclusion |
| Missing draft on opening | Programmatic Back emits no DismissRequested; no new reporting contract | Uninspected; no live configuration change claimed |
