# Training goal details coverage

Scope: `:ui-dialog-features:training-goal-details`, 2026-10-13.

## Surface and observation inventory

HomeViewModel.onOpenGoalDetails opens DialogConfig.TrainingGoalDetails after its
valid-range guard. DialogContentComponent constructs TrainingGoalDetailsComponent,
whose retained TrainingGoalDetailsViewModel renders TrainingGoalDetailsScreen.
The sole surface is a read-only bottom sheet, with no nested flow or independent
screen. Screen coverage gate: config → factory → Component → VM → rendered content
reviewed; default null screenPresentation is appropriate for this dialog. No
screen enum or dedicated dialog presentation contract is introduced.

| Owner / callback | Observation | Eligibility / payload |
|---|---|---|
| TrainingGoalDetailsViewModel.onBack | DismissRequested / training_goal_details_dismiss v1 | Existing callback without guards; empty payload before Back direction |
| TrainingGoalDetailsViewModel.onAddGoal | AddGoalClicked / training_goal_details_add_goal v1 | Existing callback without guards; GoalCard update click before AddGoal direction; empty payload |
| HomeViewModel.onOpenGoalDetails | Existing HomeEvent.GoalDetailsClicked | Existing valid-range guard; parent-owned open intent unchanged |
| HomeViewModel config onAddGoal closure | Existing HomeEvent.AddGoalClicked | Parent observes its own accepted navigation callback; unchanged, separate from dialog click intent |
| Training observation / GoalFollowingUseCase / insight calculation | Deliberate omission | Reactive rebuilding and local calculations, no finite command or submission |

BackHandler invokes onBack. DismissRequested is an observed request, not proof of
cancellation or delivered dismissal. Swipe/outside dismissal can bypass the VM and
is omitted. Dialog views/close, scrolling, insight/card impressions and navigation
completion are omitted. There are no editable inputs or picker-result callbacks.
No operation API is introduced for reactive data observation or pure computation.
No telemetry carries dates/range, goals, scores, training IDs, raw text, body
measurements or errors. No reporting context is transported through routes.

One stateless typed service is injected inside the normal retained VM factory.
Its internal exhaustive matrix encodes two versioned parameterless schemas, and
its uniquely qualified catalog is registered through the annotated module in
shared. Ordinary reporting Exceptions cannot block navigation; fatal Errors remain
fatal. No extra jobs, lifecycle wiring, persistence or business guards are added.
These are new v1 events; existing Home event meanings/populations remain unchanged.

## State preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or source | Restore/default and check |
|---|---|---|---|---|
| TrainingGoalDetailsComponent / VM | Immutable range seed | Runtime only for live dialog | Caller range, retainedInstance | Ordinary retained recreation uses the same VM; reopening receives current caller range |
| TrainingGoalDetailsViewModel | progress and insights | Rebuild | TrainingFeature.observeTrainings, GoalFollowingUseCase, current time and existing mapping/calculation | Fresh VM starts with null progress/empty insights, then rebuilds; null adherence produces NoData |
| Rendered content | Localized range, reasons, tips and goal presentation | Rebuild | Existing formatters/resources and rebuilt progress | No saved presentation cache |
| Dialog host | Session, callbacks, pending directions | Runtime only | DialogComponent slot and DialogContentComponent stack use serializer = null | Actual process death closes the dialog; callbacks are never archived |
| Foundation / widgets | Jobs, loaders, scroll state and telemetry provenance | Runtime only | Existing foundation/widget ownership | No business snapshot or synthetic interaction replay |

Source preservation gate: no editable fields, selections, filters, accepted picker
results or parent drafts are owned here. Range is an immutable caller seed. No
secrets are present. A StateKeeper supplier would save rebuilt presentation for a
session intentionally excluded from restoration, so no key/serializer is needed.
Initialization, pause/resume subscription policy, Back and parent navigation remain
unchanged. Parent Home drafts and shell restoration are outside this change.

## Verification

Source/diff self-review covered actual callbacks, eligibility, null dialog screen
hook, complete API consumers, exhaustive encoding, privacy, retained injection,
settings/shared registration and preservation ownership. No automated test sources,
infrastructure or verification-only production hooks were introduced.
Independent review is unperformed: agent delegation is not authorized, and the
available review connector provides only PR CI diagnostics.

Runtime checks remain unrun: each callback reports its intent before its existing
direction; injected reporting Exception does not block navigation; swipe/outside
produces no inferred VM event; ordinary retained recreation keeps the live VM;
actual process death closes the dialog; reopening rebuilds current data for the
caller range, including NoData. Source checks/builds cannot prove device lifecycle,
SDK delivery, parent navigation execution or resubscription after backgrounding.

Actual checks: `git diff --check` and inline source invariants passed (wire-name
bounds/pattern/reserved prefixes, implementation dependency boundary, two direct
callback consumers, default null dialog hook and exactly one settings/shared
module registration). KSP output was inspected: it binds the typed service and
registers the qualified catalog. Analytics Android compilation passed in the first
scoped attempt. The combined Android/iOS Simulator attempt failed in the unrelated
`:ui-core:state:transformNativeMainDependenciesMetadata`: missing generated
`build/kotlinTransformedMetadataLibraries/commonMain/commonMain.json`. It did not
complete UI compilation or the iOS tasks. No cache, dependency or unrelated source
was changed to work around this failure. Shared composition-root compilation/runtime
and independent review remain unverified.

Follow-up compilation succeeded without source/cache/dependency changes:
`:ui-dialog-features:training-goal-details:compileAndroidMain` and
`:analytics:training-goal-details:compileKotlinIosSimulatorArm64` completed with
BUILD SUCCESSFUL (1m 24s). A separate
`:ui-dialog-features:training-goal-details:compileKotlinIosSimulatorArm64` completed
with BUILD SUCCESSFUL (1m 10s). Together with the first analytics Android task,
both affected modules now have Android and iOS Simulator compilation evidence;
this supersedes the first attempt's incomplete scoped compilation limitation.
Warnings remain in existing conventions/dependencies. Shared full compilation,
independent review and runtime/SDK checks above remain unperformed.

## Residual review (2026-10-13)

Re-read the actual scope and traced the only caller, factory, retained VM and
rendered controls. No scoped dangling APIs, duplicate registrations, schema-name
collisions, template identities/placeholders or required preservation owners were
found. Both typed variants have a VM consumer and an exhaustive encoder; their
empty schemas satisfy transport name constraints. Reporting isolation preserves
both existing directions. The dialog and reactive rebuilding classification remains
valid, including parent-owned Home intent/navigation observations.

Fresh inline source checks and `git diff --check` passed. Production code is
unchanged by this review, so the earlier scoped Android/iOS compilation evidence
still applies to this implementation; no redundant rebuild was run. Shared full
compilation, independent review and device/SDK runtime checks remain unperformed.
