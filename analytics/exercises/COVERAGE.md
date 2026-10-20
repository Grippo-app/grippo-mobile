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
| `ExercisesRouter.Library(mode)` rendered by `ExercisesRootComponent(initial, close)`. Picking modes (`Mode.Default(preselectedMuscleGroupId, resultKey)`, `Mode.SimilarTo(targetExerciseExampleId, resultKey)`) are pushed inside TrainingComponent as `TrainingRouter.Exercises`: Default by the training recording screen with key `exercise-library-add`, SimilarTo by the training exercise editor with key `exercise-library:<exerciseId>`. `Mode.Browse` (no result key) is the Library tab: MainComponent's `createLibrary` factory builds the root with `ExercisesRouter.Library(Mode.Browse)` inside the bottom-bar frame, toolbar without Back. All modes render one library surface (toolbar, optional target card, filter header, empty state, paged list), including restored stacks | ExerciseLibraryComponent → ExerciseLibraryViewModel | `ExercisesScreen.Library` / `exercise_library`; one screenPresentation; the only enum entry; the mode is not a screen parameter |
| Exercises root stack host | ExercisesRootComponent → ExercisesRootViewModel | Default null; renders only the child stack; forwards Back to close |

The leaf resolves ExercisesAnalytics in `retainedInstance`; foundation binds and
closes the presentation and assigns Initial/Restore. No feature lifecycle bridge
or saved telemetry provenance exists. `ExercisesRouter.Library.mode` has no
default; the result keys inside the picking modes serialize by address, keeping
the route serializable. The ViewModel receives the route `ExercisesRouter.Mode`
as its caller input; the UI `ExerciseLibraryMode` in State (`Pick` = Default,
SimilarTo; `Browse`) is derived from it by `ExerciseLibraryMode.of` and carries
no key, so previews and the archive never see one.

## Callbacks and operations

| Callback | Observation / eligibility |
|---|---|
| onSuggestionsClick | FilterClicked(Suggestions) → `exercises_filter_clicked` {library_filter = suggestions} after the SimilarTo-mode guard; toggle semantics unchanged; the chip renders only when suggestions can be shown |
| onMuscleGroupClick(id) | FilterClicked(MuscleGroup) → {library_filter = muscle_group}; group IDs omitted; repeated clicks still toggle |
| onExerciseExampleSelectClick(id) in a Pick mode | ExerciseSelected → `exercises_exercise_selected` only after the current card is found, then BackWithResult(resultKey, example) → `sendResult` on the receiver-owned key read from the route `Mode.Pick` held by the ViewModel |
| onExerciseExampleSelectClick(id) in Browse mode | ExerciseDetailsClicked → `exercises_exercise_details_clicked` only after the current card is found, then `DialogController.open(DialogConfig.ExerciseExampleDetails(title, id))`; the dialog's own events belong to [exercise-example](../exercise-example/COVERAGE.md); no result is sent |
| onLoadNextPage | LoadNextPage → `exercises_load_next_page` after the existing end-reached/loading guards (checked before the event and again inside the state update); an automatic list-end callback, not a user-click claim |
| onBack (toolbar Back in Pick modes; system back in every mode) | DismissRequested → `exercises_dismiss_requested`; root forwarding adds no duplicate event. In Browse mode the toolbar has no Back control; a system back is forwarded by the root to MainComponent, which selects Home |
| onQueryChange | Exact text updates the draft; telemetry deliberately omitted: search has no existing validation status and raw text/length is never reported |

The five custom version-1 schemas (`exercises_filter_clicked` with the bounded
`library_filter`, and four parameterless) are registered through one qualified
`exercises_analytics` provider; the stateless service is bound once and listed in
`shared/.../Koin.kt`. Built-in screen schemas are reused. There are no operations:
the API has no `trackOperation`, and the catalog, target, search and
draft-training sources are continuous Flows rebuilt as business subscriptions,
not finite operations.
Ordinary reporting exceptions are isolated; business updates and navigation stay
independent. No entity IDs, target/group IDs, query text/length, errors or
request/response bodies enter telemetry.

Home startup refresh rebuilds the same Room catalog and search prefixes. Locale
changes and foreground resumes do not start a Root refresh. List and detail
responses pin the full effective locale. The repository checks the locale
immediately before calling the DAO; each existing `@Transaction` method writes
the catalog rows and search prefixes atomically. There is no explicit repository
cancellation checkpoint or second locale check during the DAO transaction: if
the locale changes after the pre-write check, that response can still commit its
captured language. Failed requests retain the last successful snapshot, which can
remain in the previous language offline or until a fresh Home startup. These
data updates leave the library query/filter draft, accepted parent selections and
analytics callbacks unchanged; they add no Exercises events or operations.

## Omissions and privacy

Focus/IME/clear, scroll position, the programmatic Suggestions → All fallback on
an empty first SimilarTo page (no cached target, or every similar candidate
already in the draft workout), the vanished-group reset, the target card
observation, the Browse details dialog session (its open intent is the
`ExerciseDetailsClicked` fact; dialog view/close telemetry stays omitted under the
dialog policy and the dialog's own events belong to exercise-example) and
referrer/flow attribution are deliberately outside this contract; filter values are the closed
`LibraryFilter` enum only.

## Preservation inventory

| Owner | Field/group | Preserve / Rebuild / Runtime only | Serializer/key or data source | Restore/default and check |
|---|---|---|---|---|
| ExerciseLibraryComponent / ExerciseLibraryViewModel | `queries.name`, including whitespace and explicitly empty values | Preserve | `DRAFT = PersistenceKey("ExerciseLibraryDraft", ExerciseLibraryState.serializer())`; nullable initialState; `saveState` of the latest VM state | The saved value wins (`initialState?.copy(mode = mode)`); trimming happens only in the request |
| Same | `queries.filter`: All, Suggestions or Group(id) | Preserve | Serializable sealed QueryFilter | The saved value, including All, wins over route preselection; the fresh default comes from the mode (SimilarTo → Suggestions, Default(group) → Group, Browse and Default(null) → All); a vanished group is dropped only on a nonempty catalog, resetting to All with restartable pagination; an empty first SimilarTo page (no cached target, or every similar candidate already in the draft workout) falls back to All and clears the suggestions flag |
| Same | `mode` and target identity | Rebuild from route | `@Transient`; derived from the serializable route Mode by `ExerciseLibraryMode.of` (the result key stays on the route, outside State) | Reapplied on every restore (`initialState?.copy(mode = ExerciseLibraryMode.of(mode))`); the target card is observed from ExerciseExampleFeature |
| Same | `exerciseExamples`, `queries.muscleGroups`, `hasSuggestionsResults`, `canShowSuggestions` | Rebuild | `@Transient` fields (the derived `canShowSuggestions` is a class-body value recomputed by the mode copy); user exercise examples use case under flatMapLatest — Default mode pages the DAO catalog, SimilarTo mode combines the unpaged similar candidates with the observed target example and the Room draft training (`TrainingFeature.getDraftTraining()`), excludes examples already in the draft workout, ranks in Kotlin (muscle-bundle overlap, load model, prior/recent use, category, force type) keeping the top 10 or overlap ≥ 0.3, and slices the requested page in memory; MuscleFeature | A stale-params guard drops emissions for superseded parameters |
| Same | `pagination` (page/limit/loading/end) | Runtime only | `@Transient` default first page (chunk page one, limit 20) | Loading restarts from page one with the restored query/filter. Each observation requests the entire loaded prefix (page one, limit = chunk limit × loaded pages), replacing all displayed cards when Room changes locale or ranking; end reached when that prefix is shorter than its requested limit. SimilarTo uses the same prefix via in-memory `ExamplePage.slice` of its ranked shortlist |
| Same | Loaders (none defined), focus, list position, cached card target, the Browse details dialog session | Runtime only | Compose-only state; in-memory `DialogController` session (`Mode.Default`, no result) | Not archived; the dialog returns nothing to the draft |
| ExerciseExampleCardLarge | Read-only equipment and last-used labels | Rebuild in composition | Distinct equipment types from the existing example; declared weight type when equipment is empty; `DateCompose` with the typed date and `DateTimeFormatState.format` | Equipment names resolve in the current locale. Empty or blank history adds no footer; other date states resolve the current locale when a typed value exists and keep the existing display otherwise. Query, filter and accepted selection are unchanged |
| ExercisesRootComponent | Stack and the mode's `resultKey` | Preserve (navigation) | `ExercisesRouter.serializer()`, key `ExercisesRootComponent`; ResultKey serialized by address | Data-object root state; no parent editable draft. As the Library tab the root lives under MainComponent's `MainTabs` child context, so its keys do not collide with a Training-hosted root |
| Callers outside the module | Accepted selection | Preserve at parent | `sendResult(resultKey, example)`; receiver-owned keys in the training recording and exercise ViewModels | Pop and re-entry without an archive re-seed from fresh route modes (`Default(lastTargetMuscleGroupId())`, `SimilarTo(exampleId)`) |

The Component restores inside `retainedInstance` and registers one lazy supplier of
the latest VM state. Nonempty catalogs drop vanished group IDs; empty startup
emissions never erase selection intent. No secret fields exist in this feature; no
`var` members, restoration flags or caches exist in these ViewModels. The selected
exercise is sent through the existing result key and its accepted business value
belongs to the caller ([training](../training/COVERAGE.md)).

## Verification

Source gates for the current code were reconciled against the route/render/owner
inventory (both hosts of the root: the Training stack for the picking modes and
the Library tab for Browse), exactly one typed leaf hook and the null containers,
retained injection/foundation binding, the complete five-schema custom catalog,
eligibility of every callback branch (Pick → selected + result, Browse → details
clicked + dialog), reporting isolation, local supplier/serializer reachability,
saved-empty precedence, transient runtime/data fields (the mode with its key is
never archived), empty/nonempty catalog behavior and the absence of restoration
flags, caches or synthetic callback replay. `git diff --check` and
`:androidApp:assembleDebug` pass for the current code; generated Koin source
confirms the ExercisesAnalytics binding and the single qualified schema catalog.
No automated test sources, dependencies or infrastructure exist.

Not verified from source: the restoration roundtrip in every mode (exact/empty
query, saved All over route preselection, a selected group through an initial
empty catalog, vanished groups, the Library tab draft under `MainTabs`); the
Essenty codec's default encoding of the queries; library/root/Main BackCallback
precedence; resubscription after the whileActive grace; reporting-failure
injection; SDK delivery; generated Koin content at runtime; native framework
linking/runtime. Runtime evidence for a given revision (tab hosting, Browse
dialog, Back-to-Home, process-death restore of the tab) lives in the task reply
that produced it. Android saved-state and iOS completed background checkpoints
are the supported shell boundaries; abrupt foreground iOS termination is not
guaranteed.
