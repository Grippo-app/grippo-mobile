# Implementation plan — TASK_7_profile_note_archive

## Summary
- Builders covered: feature-module-scaffold-builder, screen-builder
- New files: 8
- Modified files: 2
- Acceptance bullets: 5 (### Automated — structural: 2 / resource-gated: 0 / build-gated: 1 / spec-gated: 0 / screenshot-gated: 0 / test-gated: 1; ### Manual: 1)

## Names (canonical)

### Names (canonical) — owned by `screen-builder`

- **Stem**: `ProfileNoteArchive`
- **Route**: `ProfileRouter.NoteArchive`
- **Screen composable**: `ProfileNoteArchiveScreen` (file `ProfileNoteArchiveScreen.kt`)

## Files to create

| Path | Class / Object | Builder |
|---|---|---|
| `ui-screen-features/profile/src/commonMain/kotlin/<package-root>/profile/notearchive/ProfileNoteArchiveScreen.kt` | `ProfileNoteArchiveScreen` (composable) | `screen-builder` |

## Files to modify

| Path | Change | Builder |
|---|---|---|
| `ui-screen-features/screen-api/src/commonMain/kotlin/<package-root>/profile/ProfileRouter.kt` | add `data class NoteArchive(...)` | `screen-builder` |

## Public signatures

```kotlin
@Serializable
public data class NoteArchive(
    public val initialRange: DateRange,
) : ProfileRouter()
```

## Acceptance mapping

### Automated

| # | Acceptance bullet (verbatim) | Files / changes that satisfy it | Owner builder |
|---|---|---|---|
| 1 | New route `ProfileRouter.NoteArchive(initialRange: DateRange)` | modify `ProfileRouter.kt` | `screen-builder` |
| 2 | Seven MVI files following the add-screen recipe | create 7 files under `profile/notearchive/` | `screen-builder` |
| 3 | iOS XCFramework + Android debug both build green | gated by `build-validator` | — (build-gated) |
| 4 | `test:archive-card-opens-note-archive` — Tapping the archive card pushes `ProfileRouter.NoteArchive` | new ViewModel test + router mapping test | `screen-builder` |

### Manual

| # | Manual bullet (verbatim) | How the user verifies |
|---|---|---|
| 1 | Owner accepts the archive screen's visual density on a physical device | subjective design approval on device |

## Test contract

### Behavior changes

- `test:archive-card-opens-note-archive` — tapping the archive card emits the
  `NoteArchive` direction with the tapped range; owner builder `screen-builder`;
  owner module `:ui-screen-features:profile`; layer `common`; rationale: new
  navigation branch.

### Tests to create or modify

- `ui-screen-features/profile/src/commonTest/kotlin/<package-root>/profile/notearchive/ProfileNoteArchiveViewModelTest.kt`
  — case `archiveCardPushesNoteArchive` (happy) + `secondTapWhileNavigatingIsNoOp` (negative).

### Regression suites

- direct: `profile`; affected consumers: `shared`.

### Platform lanes

- `test:archive-card-opens-note-archive` → `common` (host + iOS simulator via commonTest).

### Test dependencies

- capabilities: `coroutines`, `flow` (already applied on `:ui-screen-features:profile`).

### Test applicability

- executable

## Behavioral edge-cases (reviewer-check items)

- **empty** — `ProfileNoteArchiveState.notes` starts `persistentListOf()`; render the empty state. (owner: `screen-builder`)

## Builder contracts

#### `screen-builder`
- **Scope**: ONLY the files listed above.

## Open assumptions

- none
