# Grippo API and DTO rules

Source: `data-services/backend/src/commonMain/kotlin/com/grippo/services/backend/GrippoApi.kt`
and its sibling `dto/`. The flat `GrippoApi` owns one public throwing suspend method per
existing backend operation, grouped by service section. Use its private `request` helper;
do not split the API into subgroup services. Transport is described in
[backend transport](backend-client.md).

The private `request` helper forwards request options, including an explicit locale,
to `BackendClient.invoke` and returns `response.body<T>()` directly. The exercise-example
requests supply that locale; other requests use the transport's locale default.
Repositories receive the decoded value or the original exception,
including `AppError` from the shared validator. Apply the
[throwing suspend contract](operation-outcomes.md#throwing-suspend-contracts-canonical).

All DTO fields - response, request body and collections - use nullable types, default `= null`
and explicit `@SerialName` matching the exact wire key. Kotlin
property names may differ: `@SerialName("created_at") val createdAt: String? = null` preserves the contract.

## Omitted versus explicit null

The shared `Json` (`toolkit/serialization/.../SerializationModule.kt`, injected into
`BackendClient`) sets `useAlternativeNames = false`, `ignoreUnknownKeys = true`, `isLenient = true`
and `prettyPrint = true`, and keeps the library defaults `encodeDefaults = false` and
`explicitNulls = true`. Consequences:

- A nullable request property left at its `null` default is omitted from the request body.
- A property annotated `@EncodeDefault(EncodeDefault.Mode.ALWAYS)` is written as explicit `null`.
  That is the convention for fields the backend must receive even when empty
  (`dto/training/TrainingBody.kt` annotates every field; `SetGoalBody`
  annotates only `primaryGoal`). Decide per field from the verified contract, never per file:
  a `null` that carries meaning for the server (clear/reset a value) is encoded `ALWAYS`; a
  `null` that means "unchanged" or "not provided" is left to be omitted.
- Reading ignores unknown keys and accepts lenient JSON.

The serializer configuration affects every endpoint. Change it only when the
[authorized scope](../../../../CLAUDE.md#scope-discipline) covers that shared behavior.

Collection property nullability does not prescribe element nullability: use element types from the
verified backend contract. Validate/compose server-required values in the owning domain operation
before sending; nullable Kotlin types do not permit missing required values.

DTOs contain no business logic. Wire dictionaries remain raw strings, dates raw strings and
durations numeric; the mapper promotes them to domain types. Use an existing area file pattern
and avoid duplicating types.

Quirks to keep until a wire task covers them: `updateWeightHistory(value)` posts a
`WeightHistoryResponse(weight = value)` as its body; `updateTraining(id, body)` is `PUT /trainings`
with `id` as a query parameter; `getRecentExercisesByExampleId`/`getAchievementsByExampleId` use
`exercise-metrics/exercise-example/{id}/...` paths without the leading `/` the other paths have.

For DTO/API changes MUST apply the canonical
[backend evidence trigger](../../backend-contract-client/SKILL.md#evidence-trigger-canonical).
Review method/path/query/body/result shape and all consumers; never invent a new endpoint.
