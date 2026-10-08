# Dialog sessions

`DialogController.open(config)` starts a new bottom-sheet session. If one is visible,
it closes before the new one opens. When multiple openings arrive during closing,
the latest opening wins. Opening never silently pushes into the existing sheet.

Dialog ViewModels emit feature intentions through their existing `Direction`
channel: `PickBodyWeight(initial)`, `PickDuration(initial)`, or
`ShowExampleDetails(id)`. They do not depend on `DialogConfig` or the dialog API.
Their components forward these intentions via typed callbacks. The host chooses
which dialog and title to use, binds navigation to the originating step's session,
and connects the selection callback to the feature's result handler. Delayed
requests from removed steps are ignored. Another host can fulfill the same
intention using a different UI.

Only `DialogController` is injected into outside screens. An action callback
declared by an outside screen receives `DialogController.Session` as its callback
argument, allowing it to choose push, replacement, back, or close:

```kotlin
dialogs.open(
    DialogConfig.ExerciseExample(
        title = UiText.Res(Res.string.exercise_details_btn),
        id = exampleId,
        mode = DialogConfig.ExerciseExample.Mode.Action(
            title = UiText.Res(Res.string.change_btn),
            onClick = { navigation ->
                navigation.replaceCurrent(
                    DialogConfig.ExerciseExamplePicker.SimilarTo(
                        title = UiText.Res(Res.string.exercise_example_picker_title_replace),
                        targetExerciseExampleId = exampleId,
                        onResult = ::onExerciseSelected,
                    )
                )
            },
        ),
    )
)
```

Navigation methods run on the UI thread:

- `push(config)` retains the current step for Back.
- `replaceCurrent(config)` removes the current step, retaining earlier steps and
  the same sheet. Replacing the first step also keeps the same sheet.
- `back()` pops one step; at the root, it closes the sheet.
- `close()` closes the whole session.

Navigation belongs to the originating session and step. Removed steps and closed
sessions cannot navigate, even if a later session contains identical configurations.
Duplicate configurations already in the stack are ignored to avoid reopening
the same content. Result callbacks run after popping a nested step or after
closing the root sheet; dismissal callbacks run once when their step is removed.
An action callback runs while its step is still open, so it can explicitly choose
push, replace, back, or close.

Titles use `UiText`, with no localization coroutine needed at opening time:

```kotlin
UiText.Res(Res.string.weight_picker_title)
UiText.Str(exercise.name)
UiText.Res(Res.string.set_value, persistentListOf(number))
```

Resource arguments can include another `UiText`; both resolve using the current
UI locale. Titles and dialog configurations are runtime values and are not
serialized. Duplicate requests are compared by typed content and title, excluding
callbacks. Dialog sessions are kept in memory during ordinary screen recreation, including
their live callbacks. After process death the dialog host starts empty: neither
the sheet nor its nested stack is restored from saved state. This prevents
restoring interactive dialogs with lost callback closures.
