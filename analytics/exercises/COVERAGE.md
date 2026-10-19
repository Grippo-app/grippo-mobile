# Exercises library analytics coverage

Record owner: [feature-analytics](../../.claude/skills/feature-analytics/SKILL.md)
and [state-preservation](../../.claude/skills/state-preservation/SKILL.md); records
policy: [CLAUDE.md](../../CLAUDE.md#records). Cross-feature contract and
verification limits: [current analytics contract review](../COVERAGE.md#current-analytics-contract-review-2026-10-14);
admin reconciliation: [Live GA4 setup review](../COVERAGE.md#live-ga4-setup-review--2026-10-14).
Rows describe the current code of `:ui-screen-features:exercises`, `ExercisesRouter`
in `:ui-screen-features:screen-api` and `:analytics:exercises`; replace them when
that code changes. Typed dismissal observations use DismissRequested with
`dismiss_requested` wire names; the app is unreleased and every schema is version 1.

## Surfaces and screen hooks

| Route / surface | Component → VM | Typed screen / hook |
|---|---|---|
| `TrainingRouter.Exercises(ExercisesRouter.Library(mode, resultKey))`, hosted by TrainingComponent as `ExercisesRootComponent(initial, close)`: Default mode (optional preselected muscle group) pushed by the training recording screen with key `exercise-library-add`, SimilarTo mode (target exercise example) pushed by the training exercise editor with key `exercise-library:<exerciseId>`; both render one library surface (toolbar, optional target card, filter header, empty state, paged list), including restored stacks | ExerciseLibraryComponent → ExerciseLibraryViewModel | `ExercisesScreen.Library` / `exercise_library`; one screenPresentation; the only enum entry |
| Exercises root stack host | ExercisesRootComponent → ExercisesRootViewModel | Default null; renders only the child stack; forwards Back to close |

The leaf resolves ExercisesAnalytics in `retainedInstance`; foundation binds and
closes the presentation and assigns Initial/Restore. No feature lifecycle bridge
or saved telemetry provenance exists. `ExercisesRouter.Library.mode` defaults to
`Mode.Default()`; the result key serializes by address, keeping the route
serializable.

## Callbacks and operations

| Callback | Observation / eligibility |
|---|---|
| onSuggestionsClick | FilterClicked(Suggestions) → `exercises_filter_clicked` {library_filter = suggestions} after the SimilarTo-mode guard; toggle semantics unchanged; the chip renders only when suggestions can be shown |
| onMuscleGroupClick(id) | FilterClicked(MuscleGroup) → {library_filter = muscle_group}; group IDs omitted; repeated clicks still toggle |
| onExerciseExampleSelectClick(id) | ExerciseSelected → `exercises_exercise_selected` only after the current card is found, then BackWithResult → `sendResult` on the receiver-owned key |
| onLoadNextPage | LoadNextPage → `exercises_load_next_page` after the existing end-reached/loading guards (checked before the event and again inside the state update); an automatic list-end callback, not a user-click claim |
| onBack (toolbar or system) | DismissRequested → `exercises_dismiss_requested`; root forwarding adds no duplicate event |
| onQueryChange | Exact text updates the draft; telemetry deliberately omitted: search has no existing validation status and raw text/length is never reported |

The four custom version-1 schemas (`exercises_filter_clicked` with the bounded
`library_filter`, and three parameterless) are registered through one qualified
`exercises_analytics` provider; the stateless service is bound once and listed in
`shared/.../Koin.kt`. Built-in screen schemas are reused. There are no operations:
the API has no `trackOperation`, and the catalog, target, search and
draft-training sources are continuous Flows rebuilt as business subscriptions,
not finite operations.
Ordinary reporting exceptions are isolated; business updates and navigation stay
independent. No entity IDs, target/group IDs, query text/length, errors or
request/response bodies enter telemetry.

## Omissions and privacy

Focus/IME/clear, scroll position, the programmatic Suggestions → All fallback on
an empty first SimilarTo page (no cached target, or every similar candidate
already in the draft workout), the vanished-group reset, the target card
observation, dialog sessions (none exist in this feature) and referrer/flow
attribution are deliberately outside this contract; filter values are the closed
`LibraryFilter` enum only.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| ExerciseLibraryComponent / ExerciseLibraryViewModel | `queries.name`, including whitespace and explicitly empty values | Preserve | `DRAFT = PersistenceKey("ExerciseLibraryDraft", ExerciseLibraryState.serializer())`; nullable initialState; `saveState` of the latest VM state | The saved value wins (`initialState?.copy(mode = mode)`); trimming happens only in the request |
| Same | `queries.filter`: All, Suggestions or Group(id) | Preserve | Serializable sealed QueryFilter | The saved value, including All, wins over route preselection; the fresh default comes from the mode (SimilarTo → Suggestions, Default(group) → Group, else All); a vanished group is dropped only on a nonempty catalog, resetting to All with restartable pagination; an empty first SimilarTo page (no cached target, or every similar candidate already in the draft workout) falls back to All and clears the suggestions flag |
| Same | `mode` and target identity | Rebuild from route | `@Transient`; serializable route Mode mapped by the root component | Reapplied on every restore; the target card is observed from ExerciseExampleFeature |
| Same | `exerciseExamples`, `queries.muscleGroups`, `hasSuggestionsResults`, `canShowSuggestions` | Rebuild | `@Transient` fields (the derived `canShowSuggestions` is a class-body value recomputed by the mode copy); user exercise examples use case under flatMapLatest — Default mode pages the DAO catalog, SimilarTo mode combines the unpaged similar candidates with the observed target example and the Room draft training (`TrainingFeature.getDraftTraining()`), excludes examples already in the draft workout, ranks in Kotlin (muscle-bundle overlap, load model, prior/recent use, category, force type) keeping the top 10 or overlap ≥ 0.3, and slices the requested page in memory; MuscleFeature | A stale-params guard drops emissions for superseded parameters |
| Same | `pagination` (page/limit/loading/end) | Runtime only | `@Transient` default first page (chunk page one, limit 20) | Loading restarts from page one with the restored query/filter; end reached when a page is shorter than the limit; old pages are not replayed; in SimilarTo mode the page is an in-memory `ExamplePage.slice` of the ranked shortlist, so pages are still at most `limit` long and a short page ends loading |
| Same | Loaders (none defined), focus, list position, cached card target | Runtime only | Compose-only state | Not archived |
| ExerciseExampleCardLarge | Read-only equipment and last-used labels | Rebuild in composition | Distinct equipment types from the existing example; declared weight type when equipment is empty; `DateCompose` with the typed date and `DateTimeFormatState.format` | Equipment names resolve in the current locale. Empty or blank history adds no footer; other date states resolve the current locale when a typed value exists and keep the existing display otherwise. Query, filter and accepted selection are unchanged |
| ExercisesRootComponent | Stack and `resultKey` | Preserve (navigation) | `ExercisesRouter.serializer()`, key `ExercisesRootComponent`; ResultKey serialized by address | Data-object root state; no parent editable draft |
| Callers outside the module | Accepted selection | Preserve at parent | `sendResult(resultKey, example)`; receiver-owned keys in the training recording and exercise ViewModels | Pop and re-entry without an archive re-seed from fresh route modes (`Default(lastTargetMuscleGroupId())`, `SimilarTo(exampleId)`) |

The Component restores inside `retainedInstance` and registers one lazy supplier of
the latest VM state. Nonempty catalogs drop vanished group IDs; empty startup
emissions never erase selection intent. No secret fields exist in this feature; no
`var` members, restoration flags or caches exist in these ViewModels. The selected
exercise is sent through the existing result key and its accepted business value
belongs to the caller ([training](../training/COVERAGE.md)).

## Verification

Source gates for the current revision were reconciled against the route/render/owner
inventory, exactly one typed leaf hook and the null container, retained
injection/foundation binding, the complete custom catalog, eligibility, reporting
isolation, local supplier/serializer reachability, saved-empty precedence,
transient runtime/data fields, empty/nonempty catalog behavior and the absence of
restoration flags, caches or synthetic callback replay (read-only audit on
2026-10-15; no build was part of it). Checks recorded for the implementing
revision (2026-10-13): source/diff review, inline screen/preservation/privacy/
dependency invariants and `git diff --check` passed; the combined
`:ui-screen-features:exercises:compileKotlinIosSimulatorArm64`,
`:shared:compileKotlinIosSimulatorArm64` and `:androidApp:assembleDebug` run
passed (BUILD SUCCESSFUL, 12m 25s); generated Koin source confirms the
ExercisesAnalytics binding and single qualified schema catalog; existing
opt-in/expect-actual and other project warnings remain; the initial sandboxed
wrapper attempt could not write its cache lock and the authorized retry
succeeded. No automated test sources, dependencies or infrastructure were
introduced.

Not verified: the restoration roundtrip in both modes (exact/empty query, saved
All over route preselection, a selected group through an initial empty catalog,
vanished groups); the Essenty codec's default encoding of the queries; root/child
BackCallback precedence; resubscription after the whileActive grace;
reporting-failure injection; SDK delivery; generated Koin content at runtime;
device restoration and native framework linking/runtime. Manual checks still
required: save → serialization roundtrip → destroy lifecycle and InstanceKeeper →
fresh context/VM restore in both modes, Back/result business inputs, pop and
re-entry, pause/resume beyond the subscription grace. Android saved-state and iOS
completed background checkpoints are the supported shell boundaries; abrupt
foreground iOS termination is not guaranteed.
