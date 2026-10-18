# Performance trend details coverage

Scope: `:ui-dialog-features:performance-trend-details`; Home opens the selected
metric and selected training period through `DialogConfig.PerformanceTrendDetails`.

## Surface and observation inventory

The shared DialogContentComponent creates PerformanceTrendDetailsComponent and
its retained PerformanceTrendDetailsViewModel renders PerformanceTrendDetailsScreen.
This read-only bottom sheet has no nested flows or independent screens and keeps
the default null screenPresentation. Dialog sessions have no presentation contract.

| Route/surface | Component | ViewModel | Screen hook / classification |
|---|---|---|---|
| DialogConfig.PerformanceTrendDetails / sheet | PerformanceTrendDetailsComponent | Retained PerformanceTrendDetailsViewModel | Default null under the live-dialog policy; no independent screen |

| Owner / callback | Observation | Eligibility / payload |
|---|---|---|
| PerformanceTrendDetailsViewModel.onBack | DismissRequested / performance_trend_dismiss_requested v1 | Existing Back callback, empty parameters before the existing direction |
| HomeViewModel.onPerformanceMetricClick | HomeEvent.PerformanceMetricClicked | Existing valid-range guard; parameterless event. Metric type is a business dialog input omitted from telemetry; owned by Home |
| Training observation and history calculation | Deliberate omission | Data-backed rebuilding and pure local calculation, no finite request or submission |

Back observes a dismissal request, not proof of delivered dismissal. Swipe/outside
dismissal may bypass it and is omitted. Dialog views/close, scrolling, chart gestures
and read-only history cards have no events. No range, values, identity, body
measurements, text or exceptions are reported. The typed stateless analytics service
retains its existing injection, schema and shared module registration.

Performance calculations and presentation change no event/operation models,
parameters, schemas, encoders, screen enums/hooks, tracking calls, consent or
transport. The live admin synchronization trigger therefore does not apply.

## Calculation and presentation contract

The same pure PerformanceTrendUseCase produces Home and every history entry.
Sessions are ordered by creation date and identity. Each entry compares that
session with available positive finite values of earlier sessions in the selected
period. No earlier session means an unavailable comparison, not a fabricated zero.
An unavailable latest measurement remains unavailable; an older measurement never
replaces it. Missing measurements are excluded independently for each metric.

All five metrics describe workout aggregates. Percentages and statuses use the
same current-versus-previous-average comparison, with a minimum 5% threshold widened
by twice the historical sample standard deviation relative to the mean. Only a
strict increase beyond the previous maximum earns a period-best status. Ties do
not. Duration stays neutral; cards show neither a status nor a best-duration claim.
Dates and period labels resolve through canonical composable formatters, so a locale
change updates retained history and chart labels without a ViewModel reload.
Descriptions explain exercise/session composition limits without inferring fatigue,
strength or effort. Values retain calculation precision and use output formatting
without input-field limits. Repetition means may be fractional.

History includes the first session and up to ten newest sessions; each prefix
uses all its available predecessors, even when older cards fall outside the ten
shown. Stable training IDs key history cards. The chart shows available values in
workout order with original index spacing and actual date labels. Missing values
are not replaced with zero. Lines connect available observations; they are not
measurements of the intervening workouts. Fewer than two points hides the chart,
while the session cards remain visible. An empty period has explicit explanatory copy.

## State preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| PerformanceTrendDetailsComponent / VM | range and metricType | Runtime only for live dialog | Caller inputs and retainedInstance; no draft key | Retained VM survives ordinary recreation; fresh opening uses current Home inputs |
| PerformanceTrendDetailsViewModel | history, training identity, dates and metric presentation | Rebuild | TrainingFeature.observeTrainings, PerformanceTrendUseCase and domain-to-state mapper | Starts empty; rebuilds all available session cards, including a single session |
| Dialog host | Session, callbacks and pending Back | Runtime only | Slot/step serializer = null | Process death discards the session |
| Foundation / widgets | Jobs, loaders and scroll/gesture state | Runtime only | Existing foundation and widget owners | No business snapshot or synthetic input events |

No editable fields, selections, filters, picker results, secrets or parent drafts
are introduced. Calculated values are rebuilt; no StateKeeper supplier, extra
lifecycle job or reporting state is added. Home owns its selected period.
