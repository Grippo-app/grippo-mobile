# Grippo API and DTO rules

Source: `data-services/backend/src/commonMain/kotlin/com/grippo/services/backend/GrippoApi.kt`
and its sibling `dto/`. The flat GrippoApi owns one public method per existing backend operation.
Use its existing private request helper; do not split the API into subgroup services.

All DTO fields — response, request body and collections — use nullable types, default `= null`
and explicit `@SerialName` matching the exact wire key. Kotlin property names may differ:
`@SerialName("created_at") val createdAt: String? = null` preserves the contract.

Collection property nullability does not prescribe element nullability. MUST use element types
from the verified backend contract. MUST validate/compose server-required values in the owning
domain operation before sending. Nullable Kotlin types do not permit missing required values.
MUST distinguish omitted, explicit null and empty according to the contract and current Json
configuration; serializer behavior MUST NOT change silently.

DTOs contain no business logic. Wire dictionaries remain raw strings, dates raw strings and
durations numeric; the mapper promotes them to domain types. Use an existing area file pattern
and avoid duplicating types. Existing non-null bodies/collections are deviations to address only
when authorized, not examples overriding the standard.

For DTO/API changes MUST apply the canonical
[backend evidence trigger](../../backend-contract-client/SKILL.md#evidence-trigger-canonical).
Review method/path/query/body/result shape and all consumers; never invent a new endpoint.
