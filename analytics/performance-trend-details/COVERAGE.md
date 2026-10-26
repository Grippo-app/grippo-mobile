# Performance trend details coverage

Scope: `:ui-dialog-features:performance-trend-details`; Home opens the selected
metric and selected training period, and the Completed workout screen opens the
tapped growth-bar metric and its comparison period (the Home period's kind anchored
at the saved workout's date; for an edited workout it ends at the end of that
workout's day, so workouts from later days are excluded), through the same
`DialogConfig.PerformanceTrendDetails`, which also carries the comparison reference:
Home always passes `Average`, Completed the reference of its visible comparison.

## Surface and observation inventory

The shared DialogContentComponent creates PerformanceTrendDetailsComponent and
its retained PerformanceTrendDetailsViewModel renders PerformanceTrendDetailsScreen.
This read-only bottom sheet has no nested flows or independent screens and keeps
the default null screenPresentation. Dialog sessions have no presentation contract.

| Route/surface | Component | ViewModel | Screen hook / classification |
|---|---|---|---|
| DialogConfig.PerformanceTrendDetails / sheet | PerformanceTrendDetailsComponent | Retained PerformanceTrendDetailsViewModel | Default null under the live-dialog policy; no independent screen |
| `HomeRouter.Home` / Dashboard PerformanceMetricCard entry | HomeComponent | Retained HomeViewModel | Existing typed `HomeScreen.Home` hook; full inventory in `analytics/home/COVERAGE.md` |
| `TrainingRouter.Completed` / PerformanceComparisonCard growth-bar entry | TrainingCompletedComponent | Retained TrainingCompletedViewModel | Existing typed `TrainingScreen.Completed` hook; full inventory in `analytics/training/COVERAGE.md` |

| Owner / callback | Observation | Eligibility / payload |
|---|---|---|
| PerformanceTrendDetailsViewModel.onBack | DismissRequested / performance_trend_dismiss_requested v1 | Existing Back callback, empty parameters before the existing direction |
| HomeViewModel.onPerformanceMetricClick | HomeEvent.PerformanceMetricClicked | Existing valid-range guard; parameterless event. Metric type and the `Average` reference are business dialog inputs omitted from telemetry; owned by Home |
| TrainingCompletedViewModel.onMetricClick | TrainingEvent.Completed.MetricOpened / completed_metric_opened v1 | Computed-comparison guard (`performance.period.value`); parameterless event. The tapped metric type, the Completed period and its computed reference are business dialog inputs (title `value_performance_trend(rangeLabel, metric)` or `performance_trend(metric)`), not encoded; owned by Completed |
| Training observation and history calculation | Deliberate omission | Data-backed rebuilding and pure local calculation, no finite request or submission |

The leaf Back callback observes a dismissal request, not delivered dismissal.
The shared host handles toolbar Back/Close, Compose/system Back and swipe/outside
dismissal without invoking that callback; those paths are omitted. Dialog views/close,
scrolling, chart gestures, read-only history rows and the local methodology
disclosure have no events. No range, values, identity, body
measurements, text or exceptions are reported. The typed stateless analytics service
retains its existing injection, schema and shared module registration.

Performance calculations and presentation change no event/operation models,
parameters, schemas, encoders, screen enums/hooks, tracking calls, consent or
transport. The live admin synchronization trigger therefore does not apply.

## Calculation and presentation contract

The same pure PerformanceTrendUseCase produces Home, the Completed comparison and
every history entry (Completed feeds it the saved workout plus the period trainings
created before it, or at the same instant with a lower id; see `analytics/training/COVERAGE.md`).
Sessions are ordered by creation date and identity. Each entry compares that
session with available nonnegative finite values of earlier sessions in the selected
period. No earlier session means an unavailable comparison, not a fabricated zero.
An unavailable latest measurement remains unavailable; an older measurement never
replaces it. Missing measurements are excluded independently for each metric.
Recorded zero remains available; a zero earlier value has no percentage comparison.
Density requires a positive duration denominator, including when volume is zero.

All five metrics describe workout aggregates. Percentages and statuses use the
same current-versus-previous-average comparison, with a minimum 5% threshold widened
by twice the historical sample standard deviation relative to the mean. Only a
strict increase beyond the previous maximum earns a period-best status. Ties do
not. Duration stays neutral; cards show neither a status nor a best-duration claim.
This is the use case's Average reference, which Home always passes. The sheet
computes every entry against the reference its caller passes, so opened from a
Completed comparison under Previous each entry compares only with the workout
before it in the period, with a fixed ±5 % band, and never reports a period best.
Dates and period labels resolve through canonical composable formatters, so a locale
change updates retained history and chart labels without a ViewModel reload.
Descriptions explain exercise/session composition limits without inferring fatigue,
strength or effort. Values retain calculation precision and use output formatting
without input-field limits. Repetition means may be fractional.

The sheet's Detailed metric card is one neutral summary of the latest session and
its available earlier value under the passed reference. The value label ("Earlier
average" or "Previous workout") establishes the comparison reference once; the
adjacent percentage uses a short change label, and the missing and zero earlier-value
notes name the same reference. The Overview
card keeps its existing friendly metric names and resolves them through the same
PerformanceMetricTypeState label mapping as the dialog title. Label and labelText
remain resource-backed, so locale changes do not freeze the names.
History uses compact date/value rows for previous sessions only; the latest
session appears once in the summary. Same-day sessions include their time in both
summary and rows. A single session has no history heading or empty list section.
The initially collapsed methodology disclosure contains the metric definition and
its limits and one comparison rule worded for the passed reference. A separate chart-scope
sentence is included only when the chart is shown. The same locally derived
hasChart predicate (at least two available chart values) controls both the chart and
that sentence, including empty, single-session and partially measured histories.
No visibility field is stored in State or a draft.

Before the first cached-source emission, `history` is null and the sheet shows
initial loading rather than an empty-period claim. The first emission resolves
history to a populated or empty immutable list. The sheet observes the same cached
selected-period source as before; it makes no separate refresh request or remote
freshness claim.

History retains up to ten newest sessions, including a first session when it is
within that limit; each prefix uses all its available predecessors, even when older
entries fall outside the ten retained. The screen derives up to nine previous-session
rows without changing that history. Stable training IDs
key history rows. The compact chart keeps the full retained history and shows
available values in workout order with elapsed-day spacing and actual date labels. If a timestamp is
unavailable, the whole series falls back to workout index spacing. Missing metric
values are not replaced with zero. Distinct first/last dates are shown below the chart; exact values
retain their metric's units in the local tooltip and accessibility description. Lines connect available observations; they are not
measurements of the intervening workouts. Fewer than two points hides the chart,
while the available session values remain accessible. An empty period has explicit
explanatory copy. An available earlier value is determined from the metric's
actual nullable average, so a recorded zero earlier value remains distinct from having
no earlier measurement.

## State preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| PerformanceTrendDetailsComponent / VM | range, metricType and reference | Runtime only for live dialog | Caller inputs and retainedInstance; no draft key | Retained VM survives ordinary recreation; a fresh opening uses the caller's current inputs (Home's selected period, metric and `Average`, or Completed's comparison period, tapped metric and computed reference) |
| PerformanceTrendDetailsViewModel | history, training identity, dates and metric presentation | Rebuild | TrainingFeature.observeTrainings for the selected range, PerformanceTrendUseCase and domain-to-state mapper | History starts null; the first emission resolves populated/empty data in one update. At most ten newest entries are retained for the summary and chart, with up to nine previous-session rows derived locally; their comparisons still use all predecessors |
| PerformanceTrendDetailsScreen | Methodology disclosure state | Runtime only | Local rememberSaveable presentation state in the live dialog | Fresh opening starts collapsed; ordinary recomposition/recreation retains the current disclosure. No business draft or process-death dialog restoration |
| Dialog host | Session, callbacks and pending Back | Runtime only | Slot/step serializer = null | Process death discards the session |
| Foundation / widgets | Jobs, loaders and scroll/gesture state | Runtime only | Existing foundation and widget owners | No business snapshot or synthetic input events |

No editable business fields, selections, filters, picker results, secrets or parent
drafts are introduced. Calculated values are rebuilt; no StateKeeper supplier, extra
lifecycle job or reporting state is added. Each caller owns its period: Home its
selected range, Completed its comparison period: the Home period's kind (from
`LocalSettingsFeature.observeRange()`) anchored at the saved workout's date, ending
at the end of an edited workout's day.
