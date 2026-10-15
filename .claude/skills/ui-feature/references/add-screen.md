# Recipe — Add a Screen (sub-screen inside an existing feature)

Read [MVI](mvi-contract.md), [foundation usage](base-classes.md),
[Compose rules](compose-rules.md), and actual neighboring feature files.
NoteArchive below is an educational, nonexistent API, not a current route catalog.

## 1. Establish ownership

Inspect existing routes, entry paths, parent flow inputs and dependencies.
A leaf lives in its feature's sub-screen package with the seven MVI files.
Choose UI extraction under [Compose structure](../../compose-components/references/structure-and-runtime.md#read-from-top-to-bottom)
and place extracted composables under [feature file layout](module-structure.md#file-layout-for-a-feature).
MUST NOT add a separate feature/module/root graph merely for one sub-screen.

## 2. Implement the seven-file contract

Apply [mvi-contract.md](mvi-contract.md) instead of copying a complete scaffold.
The ViewModel owns State/update, validated inputs, loaders, business operations
and Directions; the Component resolves dependencies inside retainedInstance,
collects state/loaders and translates every Direction through ordinary callbacks.
Required Result calls unwrap inside safeLaunch; errors/cancellation follow the
[shared pipeline](error-pipeline.md). Apply the canonical multiline business style.

For lists, rebuild presentation through existing Feature Flows and directional
mappers. Route inputs initialize filters; a saved local draft wins over that seed.
A changing filter drives its existing source and refresh operation; observers
MUST NOT overwrite editable inputs or accepted choices.

## 3. Preserve the latest draft

Apply [state-preservation](../../state-preservation/SKILL.md), including its field
inventory and source gate. For example, a range is saved while data-backed items
are transient and rebuilt. Use stable defaults and the module serialization plugin.

```kotlin
initialState = restoreState(DRAFT)
    ?: ProfileNoteArchiveState(range = DateRangeFormatState.of(initialRange))
// Register once during Component construction, not on every input change.
saveState(DRAFT) { viewModel.state.value }
```

This is a focused ownership excerpt, not a complete Component. Define the private
DRAFT PersistenceKey in the Component's final companion. Follow
[process-death.md](process-death.md#editable-form-drafts) for exact construction,
accepted parent inputs, secret exclusions and supported shell restoration.

## 4. Wire navigation and results

Add the serializable sub-route in screen-api, the parent's createChild branch and
Child subtype; preserve its existing keyed childStack. Thread route inputs and
ordinary navigation callbacks through construction. Child Back goes to the
parent's normal handler: pop a previous child, exit the feature only at its root.
Inspect [ProfileComponent](../../../../ui-screen-features/profile/src/commonMain/kotlin/com/grippo/profile/ProfileComponent.kt).
MUST NOT mutate the parent's stack from the leaf or introduce a global navigator.
Cross-feature destinations follow [navigation.md](navigation.md); result-producing
screens follow [results.md](results.md), including receiver-owned keys/sendResult→Back.
Update all required callers and actual module/DI registrations within scope.

## 5. Render and preview

Use BaseComposeScreen with screen background and the existing screen Toolbar.
Render entity UI through its State model; real reusable model stubs belong in
ui-core:state. Use AppTokens for product styling and localized resources.
Add private AppPreview inside PreviewContainer with Contract.Empty for the
significant states ([definitions](../../../../CLAUDE.md#definitions)). For supplied designs use their actual content and declared states;
[visual evidence](../../design-system/references/visual-consistency.md#evidence) owns permitted comparison.

## 6. Verify

Apply [feature analytics](../../feature-analytics/SKILL.md): every independent
screen declares its typed VM screenPresentation and containers keep null. Resolve
analytics inside retained VM construction, update actual coverage and run its
mandatory screen gate. This recipe does not replace that integration.

Review every affected source/caller against both analytics and preservation gates.
Use relevant checks/builds under [verification policy](../../../../CLAUDE.md#verification-policy).
Report actual results and unverified platform/runtime/restoration states. Retention
or a State-only copy does not prove a fresh-component archive restoration.
Do not introduce automated tests/infrastructure without an explicit user request.
