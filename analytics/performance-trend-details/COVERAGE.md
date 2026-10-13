# Performance trend details coverage

Scope: `:ui-dialog-features:performance-trend-details`, 2026-10-13.

## Surface and observation inventory

`HomeViewModel.onPerformanceMetricClick` opens `DialogConfig.PerformanceTrendDetails`.
The shared DialogContentComponent factory constructs PerformanceTrendDetailsComponent,
whose retained PerformanceTrendDetailsViewModel renders PerformanceTrendDetailsScreen.
There are no nested flows or independent screens in this module. This read-only
bottom sheet keeps the default null screenPresentation: no screen enum or dedicated
presentation contract is introduced. Screen coverage gate: dialog classification
traced through actual config, factory, Component, VM and rendered content.

| Owner / callback | Observation | Eligibility / payload |
|---|---|---|
| PerformanceTrendDetailsViewModel.onBack | DismissRequested / performance_trend_dismiss_requested v1 | Existing Back callback, no guard; empty parameters before existing Back direction |
| HomeViewModel.onPerformanceMetricClick | Existing HomeEvent.PerformanceMetricClicked | Existing valid-range guard; unchanged, owned by Home |
| Training/user observations and history calculation | Deliberate omission | Reactive data rebuilding and local pure calculation, no finite business request or submission |

The existing BackHandler callback invokes onBack. This event observes a request,
not proof of delivered dismissal or user cancellation. Swipe/outside dismissal may
bypass it and is omitted. Dialog views/close, scrolling and chart gestures are also
omitted; no widget callbacks or reporting context are added. History cards are
read-only. No event carries range, metric values, training identity, body measurements,
raw text or exception data. No operation API/decorator is needed for this scope.

One stateless typed service is resolved inside the existing retained VM factory.
The implementation has an exhaustive event encoder, versioned empty schema and
qualified `performance_trend_details_analytics` catalog, registered through the
annotated module in shared. Ordinary reporting Exceptions are isolated at the
implementation boundary and cannot prevent navigation; fatal Errors propagate.
No lifecycle observer, extra job, analytics state or route changes are introduced.

## State preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| PerformanceTrendDetailsComponent / VM | range and metricType | Runtime only for live dialog | Existing caller inputs and retainedInstance; no draft key | Ordinary recreation uses retained VM; fresh opening gets current Home range/type |
| PerformanceTrendDetailsViewModel | history and localized date/metric presentation | Rebuild | TrainingFeature.observeTrainings, UserFeature.observeUser, PerformanceTrendUseCase and existing state mappers | Fresh VM starts empty and rebuilds current history; fewer than two trainings yields empty history |
| Dialog host | Live session, callbacks and pending Back | Runtime only | Slot and step navigation serializer = null | Process death discards the dialog; no session restoration |
| Foundation / widgets | Subscription jobs, loaders, scroll/gesture state and telemetry provenance | Runtime only | Existing foundation scope and widget ownership | No business snapshot or synthetic input events |

Source preservation gate: no editable fields, selections, filters, accepted picker
results or parent drafts are owned by this module. Range/type are immutable input
seeds, not editable dialog filters. No secrets exist here. Adding StateKeeper would
persist data-backed presentation for a session that is deliberately unsaved; no
supplier or serializer is added. Existing initialization, Back and data rebuilding
remain unchanged. Parent Home state and shell restoration are outside this change.

## Verification

Source/diff self-review reconciled the actual surface, sole callback, schema/API
consumer, retained DI, shared registration, privacy and state ownership. Independent
review is unperformed: agent delegation is not authorized and the available review
connector supplies only PR CI diagnostics. No automated test sources, infrastructure
or verification-only production hooks were added.

Runtime checks remain unrun: Back emits exactly its observed request; reporting
Exception does not prevent Back; swipe/outside emits no inferred VM dismissal;
ordinary recreation retains the live VM; actual process death closes this dialog;
reopening rebuilds current history, including empty data. Device lifecycle and SDK
delivery cannot be proved by source review or compilation.

Final checks: `git diff --check` and inline source invariants passed for the sole
callback, null dialog screen hook, exhaustive encoding, dependency boundaries and
single settings/shared DI registration. Scoped Android/iOS Simulator compilation
of both modules was attempted offline without configuration cache. It remained
blocked before feature tasks: the daemon worker thread dump confirmed waiting in
DefaultFileLockManager while hashing Kotlin DSL inputs during configuration.
Multiple concurrent Gradle daemons were active. Only this attempt's wrapper process
was terminated; no cache or unrelated infrastructure was changed. Compilation,
generated Koin output and shared integration remain unverified.

## Residual review (2026-10-13)

Found and fixed a blocking catalog defect: the initial event name had 43 characters,
exceeding AnalyticsSchemaRules.MAX_NAME_LENGTH = 40. The catalog validates names at
construction, so this could reject the registered catalog before event reporting.
The wire name is now `performance_trend_dismiss_requested` (33 characters); the public
DismissRequested fact and existing callback/navigation behavior are unchanged.
This is a correction of the newly introduced, unverified v1 schema, with no prior
verified delivery/report compatibility to preserve. Coverage uses the corrected name.

Source review found no additional scoped dangling APIs, template leftovers, duplicate
registrations, missing event consumers, reporting-only route fields or preservation
owner gaps. Inline checks confirmed transport length/pattern/reserved-prefix rules
and exactly one settings/shared dependency/DI registration. `git diff --check` passed.
The repeat Android/iOS Simulator offline compilation again waited during project
configuration; its daemon thread dump confirmed DefaultFileLockManager waiting.
Only the repeat attempt's wrapper was terminated. Compilation/generated DI/shared
runtime integration remain unverified. Independent review and device/SDK checks
remain unperformed under the limitations above.
