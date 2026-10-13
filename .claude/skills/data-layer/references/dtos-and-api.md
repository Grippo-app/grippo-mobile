# Grippo API and DTO rules

Source: `data-services/backend/src/commonMain/kotlin/com/grippo/services/backend/GrippoApi.kt`
and its sibling `dto/`. The flat GrippoApi owns one public method per existing backend operation.
Use its existing private request helper; do not split the API into subgroup services.

All DTO fields — response, request body and collections — use nullable types, default `= null`
and explicit `@SerialName` matching the exact wire key. Kotlin property names may differ:
`@SerialName("created_at") val createdAt: String? = null` preserves the contract.

```kotlin
@Serializable
public data class IdsBody(
    @SerialName("ids") val ids: List<String?>? = null,
)
```

This mirrors the existing IdsBody shape. Required request values must be validated/composed by
existing domain operations before sending; nullable serialization is not permission to omit
server-required fields. Distinguish omitted, explicit null and empty list according to the
contract and current Json configuration. Do not silently change serializer behavior.

DTOs contain no business logic. Wire dictionaries remain raw strings, dates raw strings and
durations numeric; the mapper promotes them to domain types. Use an existing area file pattern
and avoid duplicating types. Existing non-null bodies/collections are deviations to address only
when authorized, not examples overriding the standard.

Before changing DTO/API shape read [backend-contract-client](../../backend-contract-client/SKILL.md).
Review method/path/query/body/result shape and all consumers; never invent a new endpoint.
