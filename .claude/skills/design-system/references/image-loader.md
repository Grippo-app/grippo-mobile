# Images and placeholders

Inspect the actual [ExerciseExampleImage component](../../../../design-system/components/src/commonMain/kotlin/com/grippo/design.components/example/ExerciseExampleImage.kt)
and its callers before changing the image contract. It uses Coil AsyncImage,
AppTokens for product styling and an existing inset-vector painter adapter.

Reuse actual public component token groups for size/radius/padding and real AppIcon
placeholders. Add a new demanded token through the design-system workflow only
when no existing token fits. [Resource policy](resources.md) owns imagery formats.

Review content scale, clipping, loading/error appearance and accessibility in real
consumers. Existing Coil setup owns platform engines/cache; a component does not
create its own HttpClient or credential propagation policy. See
[platform utilities](../../platform-build-toolkit/references/toolkit-utilities-platform.md).
