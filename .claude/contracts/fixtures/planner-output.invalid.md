# Implementation plan — TASK_7_profile_note_archive

## Summary
- Builders covered: screen-builder

## Names (canonical)

### Names (canonical) — owned by `screen-builder`

- **Stem**: `ProfileNoteArchive`

## Files to create

| Path | Class / Object | Builder |
|---|---|---|
| `.../ProfileNoteArchiveScreen.kt` | `ProfileNoteArchiveScreen` | `screen-builder` |

## Files to modify

| Path | Change | Builder |
|---|---|---|
| `.../ProfileRouter.kt` | add route | `screen-builder` |

## Acceptance mapping

### Automated

| # | Acceptance bullet (verbatim) | Files / changes that satisfy it | Owner builder |
|---|---|---|---|
| 1 | New route | modify `ProfileRouter.kt` | `screen-builder` |

## Behavioral edge-cases (reviewer-check items)

- none

## Builder contracts

#### `screen-builder`
- **Scope**: listed files only.

## Open assumptions

- none

## Test contract

### Behavior changes
- none

### Tests to create or modify
- none

### Regression suites
- none

### Platform lanes
- none

### Test dependencies
- none

### Test applicability
- test-not-applicable: simple-change
