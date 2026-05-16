# Template Conventions

`requirements/` is a project-agnostic template. Code blocks, example names, and module paths in chapters MUST use the placeholders defined here. Concrete reference-repo names (`GrippoApi`, `TrainingResponse`, `WeightFormatState`, ...) are template leakage and get rewritten by `invalidate-templatize.md`.

This file is the **single source of truth** for the templatize loop. The two regular audits (`invalidate.md`, `invalidate-sub.md`) read it to know which substitutions are intentional and must not be flagged as drift.

## 1. Slot placeholders

Vary 1:1 with the per-project bootstrap. Each project edits these in `03-project-config.md` once; chapters use the placeholder form.

| Placeholder | Reference repo literal | Substitution at bootstrap |
|---|---|---|
| `<Product>` | `Grippo` (class prefix) | The product's PascalCase name |
| `<product>` | `grippo` (id slug) | Kebab/lowercase form |
| `<org>` | `grippo` (package root org segment) | Org segment |
| `<product-domain>` | `grippo-app.com` | Backend host |
| `com.<org>.<product>` | `com.grippo` | Android namespace root |
| `com.<org>.<product>.<area>` | `com.grippo.<area>` | Sub-package |
| `<product>-mobile` | `grippo-mobile` | Repo / Gradle root project name |

Use these wherever the value is project-level identity:

- `<Product>Api` (was `GrippoApi`)
- `https://<product-domain>` (was `https://grippo-app.com`)
- `package com.<org>.<product>.services.backend.dto.<area>`
- `applicationId = "com.<org>.<product>.android"`

## 2. Canonical example types

When a code block illustrates a pattern (Repository, Mapper, Entity, Feature, Screen, Dialog), use one of these instead of inventing names or borrowing the reference repo's domain types.

| Type | Use when | Replaces in reference repo |
|---|---|---|
| `Note` | Primary list/detail entity for the bulk of pattern examples | `Training`, `Exercise`, `WeightHistory`, `Goal`, ... |
| `Tag` | Secondary related entity (illustrates `@Relation`, joins) | `Muscle`, `Equipment`, `ExerciseExample`, ... |
| `Item` | Third generic entity if genuinely needed | other domain types |

A pattern walkthrough should pick **one** primary type and use it consistently end-to-end (DTO → Entity → Pack → Repository → Feature → Screen). Mix `Note` + `Tag` only when the pattern itself involves a relationship.

Class-name fan-out for the primary type `Note`:

- DTO: `NoteResponse`, `NoteBody`
- Entity: `NoteEntity`, `NoteDao`, `NotePack`
- Domain: `Note`, `NoteRepository`, `NoteFeature`, `NoteRepositoryImpl`, `NoteFeatureImpl`
- DI: `NotesFeatureModule`
- UI: `NotesRouter`, `NotesComponent` (or `NotesRootComponent` if the feature has a same-name sub-screen), `NotesState`, `NotesContract`, `NotesDirection`, `NotesLoader`, `NotesViewModel`, `NotesScreen`
- Sub-screens: `NoteDetail*`, `NoteArchive*`, `NoteEditor*`

For the secondary type `Tag`: `TagResponse`, `TagEntity`, `TagDao`, `Tag`, `TagRepository`, etc. Used in `@Embedded`/`@Relation` examples (`NotePack` containing `List<TagEntity>`).

## 3. Canonical worked example

The reference repo's cross-file worked example is "Workout history" — adds a `ProfileWorkoutHistoryScreen` to `:ui-screen-features:profile`, with `WorkoutHistoryFeature` from `:data-features:feature-api`. It appears in:

- `14-cookbook/01-add-screen.md`
- `14-cookbook/08-add-cross-feature-nav.md`
- `tasks/README.md`
- `sub-agents/README.md`

Templatized form: **"Note archive"** — adds a `ProfileNoteArchiveScreen` to `:ui-screen-features:profile`, with `NoteFeature` from `:data-features:feature-api`.

- Route: `ProfileRouter.NoteArchive`
- Component: `ProfileNoteArchiveComponent`
- ViewModel: `ProfileNoteArchiveViewModel`
- Files: `ProfileNoteArchive{Component, ViewModel, Contract, State, Direction, Loader, Screen}.kt`
- Cross-feature entry: from `:ui-screen-features:home` (`HomeRootComponent` exposes `toNoteArchive: () -> Unit`).

The recipe steps are identical; only the names change. The four files that share this example MUST be rewritten in a single coordinated pass (cross-file consistency). The templatize loop flags this work but does NOT auto-patch it; finish it with one manual cleanup prompt at the end.

## 4. Format-state policy

Three buckets:

### 4.1 Required infrastructure — keep, generic

Every project has these; they wrap product-neutral values:

- `EmailFormatState` — sealed `Empty | Invalid(value) | Valid(value)`, regex-validated.
- `PasswordFormatState` — sealed `Empty | Invalid(value, reason) | Valid(value)`, with `hint(): String`.
- `NameFormatState` — sealed `Empty | Invalid(value) | Valid(value)`.
- `DateFormatState`, `DateTimeFormatState`, `DateRangeFormatState` — date/time wrappers.

Reference these by name in chapter examples.

### 4.2 Generic numeric example

For chapters that illustrate "numeric field with units, sealed Empty/Invalid/Valid", use one generic class:

- `AmountFormatState(value: Double?, unit: String?)` — covers the pattern without leaking a product unit.

Don't introduce additional generic numeric variants; one is enough to teach the pattern.

### 4.3 Product-specific — REMOVE from chapters

These are reference-repo domain types. Chapters MUST NOT enumerate them as if they were infrastructure:

- `WeightFormatState`, `HeightFormatState`, `DurationFormatState`, `VolumeFormatState`, `PercentageFormatState`, `IntensityFormatState`, `DensityFormatState`, `MultiplierFormatState`, `RepetitionsFormatState`.

When found in a chapter:
- In code examples → replace with `AmountFormatState`.
- In "must exist" lists or glossary tables → remove (don't list them as infrastructure).

## 5. Module list policy

Chapters MUST NOT enumerate the reference repo's product features as if they were a fixed set. Specifically:

- **`02-module-structure/08-data-feature-modules.md`** — "Reference areas" table listing 12 fitness features → replace with one line: "Per-project. Each module is `:data-features:<feature>` and follows the pattern below."
- **`06-data-layer/04-database.md`** — `@Database(entities = [...])` listing 16 fitness entities → use placeholder list `[<Entity>Entity, <RelatedEntity>Entity, ...]`.
- **`02-module-structure/07-ui-feature-modules.md`** — concrete feature list (`:profile`, `:training`, ...) → keep at most two as illustrative (one + Note archive), mark "Examples from reference repo; replace per product."
- **`02-module-structure/05-design-system-modules.md`** — `WeightHistoryChart`, `MuscleLoadHeatmap` as component examples → use generic component names from §6 below or remove.

## 6. Generic component name examples

When chapters illustrate "specialized design-system component built on primitives" (charts, heatmaps, complex cards):

- `<Entity>Chart`, `<Entity>Timeline`, `<Entity>Heatmap`, `<Entity>SummaryCard`, `<Entity>HistoryCard`.

E.g. `NoteTimelineChart`, `TagSummaryCard`. Don't invent unique example names — the pattern is what teaches, not the name.

## 7. Reserved names (unchanged from glossary)

These are infrastructure-stable across projects. Do NOT substitute:

- Base classes: `BaseViewModel`, `BaseComponent`, `BaseScreen`, `BaseComposeScreen`, `BaseDirection`, `BaseLoader`, `BaseRouter`, `BaseResult`, `ComponentIdentifier`.
- Infrastructure: `OperationManager`, `ResultManager`, `ResultEmitter`, `ResultKey`, `ResultKeys`, `Processing`.
- Design tokens: `AppTokens`, `AppTheme`, `AppColor`, `AppDp`, `AppTypography`, `AppString`, `AppDrawable`, `AppIcon`, `AppPreview`, `PreviewContainer`, `AppLocale`.
- Resources: `StringProvider`, `UiText`, `Format*State` (the marker — instances are policy-bucketed in §4).
- Errors: `AppError`, `ErrorProvider`, `AppErrorState`.
- Dialogs: `DialogConfig`, `DialogController`, `DialogComponent`, `DialogProvider`.
- Toolkit: `AppLogger`, `DateTimeUtils`, `DateRange`, `DateRangeKind`, `DateRangePresets`, `DateFormat`, `DateFormatting`, `NativeContext`, `Connectivity`, `NotificationManager`, `PermissionManager`, `LinkOpener`.

## 8. Leakage signatures (grep targets)

The templatize loop greps each chapter for these tokens (case-insensitive). Each hit is a `PRODUCT_LEAKAGE` finding.

Identifier-level:

```
GrippoApi | TrainingResponse | TrainingBody | TrainingEntity | TrainingDao | TrainingPack
TrainingFeature | TrainingRepository | TrainingRouter | TrainingComponent | TrainingRootComponent
TrainingRecordingComponent | TrainingsRootComponent | TrainingIdentifier
ExerciseResponse | ExerciseEntity | ExercisePack | ExerciseExampleEntity
IterationEntity | IterationResponse
MuscleEntity | MuscleLoadHeatmap | MuscleLoadSummary | MuscleLoadSummaryState
EquipmentEntity | ExcludedMuscleEntity | ExcludedEquipmentEntity
WeightHistoryEntity | WeightHistoryFeature | WeightHistoryChart
WeightFormatState | HeightFormatState | DurationFormatState | VolumeFormatState
DensityFormatState | IntensityFormatState | PercentageFormatState
MultiplierFormatState | RepetitionsFormatState
ProfileBody | ProfileBodyState | ProfileBodyScreen | ProfileBodyContract
WorkoutHistory | WorkoutHistoryFeature | ProfileWorkoutHistory
RatingPicker | StartTraining | FinishWorkout | ChangeWeight
TrainingStreakState | GoalProgressState | GoalEntity | SecondaryGoal
stubTraining | stubTrainings | stubWeightHistoryList | stubUser
```

Path/literal-level:

```
grippo-app.com | com.grippo
:data-features:trainings | :data-features:muscle | :data-features:weight-history
:data-features:goal | :data-features:equipment | :data-features:excluded-muscles
:data-features:excluded-equipments | :data-features:exercise-examples
:data-features:exercise-metrics
:ui-screen-features:training | :ui-screen-features:trainings
:ui-dialog-features:weight-picker
Res.string.weight | Res.string.height | Res.string.training | Res.string.workout
Res.string.muscle | Res.string.exercise
notification_weight | notification_workout | finishWorkout
Deeplink.WeightHistory | Deeplink.TrainingDraft | Deeplink.StartTraining
```

The reference word `Grippo` is leakage in chapter files but legitimate in:
- `00-overview/03-project-config.md` (the per-project edit point — frontmatter values).
- `04-glossary.md` "Reserved names" / "Product-specific names" tables where it appears as the worked illustration of the rule.
- This file (`05-template-conventions.md`) where it documents what to replace.

## 9. Out of scope for templatize

The loop genericizes chapter content. It does NOT:

- Rename `.md` files (e.g. `06-data-layer/03-grippo-api-and-dtos.md` → `03-product-api-and-dtos.md`). Flag; handle in the final cleanup prompt.
- Rewrite the cross-file worked example (§3). Flag; handle in the final cleanup prompt.
- Reorganize chapters or move content between files.
- Touch `00-overview/03-project-config.md` frontmatter — those are the per-project values, not template content.
- Touch live code under `:shared`, `:data-services`, etc. — that's the actual Grippo project.

## 10. Done criteria

The templatize work is done when, for every `requirements/<chapter>/*.md` and every `requirements/{sub-agents,tasks}/**/*.md`:

```
rg -i -E '(grippo|training|workout|muscle|weightformatstate|profilebody|workouthistory)' requirements/<file>
```

returns either zero matches OR only matches in the documented exceptions (project-config.md frontmatter, glossary reserved-names table, this conventions file).
