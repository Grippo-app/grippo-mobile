package com.grippo.toolkit.analytics

/** Features depend on this interface, never on the transport. */
public interface Analytics {
    public fun track(event: AnalyticsEvent, flow: FlowContext? = null, operation: OperationId? = null)
    public fun updateContext(context: AnalyticsContext)
    public fun updateIdentity(identity: AnalyticsIdentity)
    public fun updateCollection(collection: AnalyticsCollection)
}

public enum class AnalyticsCollection { Unknown, Denied, Granted }

public interface AnalyticsEvent {
    public val definition: EventDefinition
    /** Pure, bounded encoding. No IO, mutable shared state or analytics callbacks. */
    public fun parameters(): List<AnalyticsParameter>
}

/** Stable wire values, independent of enum.name and UI translations. */
public interface AnalyticsCode { public val analyticsCode: String }

/** Only declaration/encoding code creates keys. Event constructors carry business types. */
public class ParameterKey<T> private constructor(
    public val name: String,
    public val required: Boolean,
    private val encode: (T) -> AnalyticsValue,
) {
    public fun value(value: T): AnalyticsParameter = AnalyticsParameter(this, encode(value))

    public companion object {
        public fun <T : AnalyticsCode> code(name: String, required: Boolean = true): ParameterKey<T> =
            ParameterKey(name, required) { AnalyticsValue.Text(it.analyticsCode) }
        public fun count(name: String, required: Boolean = true): ParameterKey<AnalyticsCount> =
            ParameterKey(name, required) { AnalyticsValue.Integer(it.value) }
        public fun duration(name: String, required: Boolean = true): ParameterKey<DurationMillis> =
            ParameterKey(name, required) { AnalyticsValue.Integer(it.value) }
        public fun number(name: String, required: Boolean = true): ParameterKey<AnalyticsNumber> =
            ParameterKey(name, required) { AnalyticsValue.Decimal(it.value) }
        public fun flag(name: String, required: Boolean = true): ParameterKey<Boolean> =
            ParameterKey(name, required) { AnalyticsValue.Integer(if (it) 1 else 0) }
    }
}

public class AnalyticsParameter internal constructor(
    public val key: ParameterKey<*>,
    public val value: AnalyticsValue,
)

public class EventDefinition(
    public val name: String,
    public val version: Int,
    parameters: List<ParameterKey<*>>,
) {
    public val parameters: List<ParameterKey<*>> = parameters.toList()
}

/** Each feature can contribute one named Koin binding without depending on analytics core. */
public class AnalyticsDefinitions(
    events: List<EventDefinition>,
    properties: List<UserPropertyKey<*>> = emptyList(),
) {
    public val events: List<EventDefinition> = events.toList()
    public val properties: List<UserPropertyKey<*>> = properties.toList()
}

public class AnalyticsCount(public val value: Long) { init { require(value >= 0) } }
public class DurationMillis(public val value: Long) { init { require(value >= 0) } }
public class AnalyticsNumber(public val value: Double) { init { require(value.isFinite()) } }

/** Transport representation; raw maps never cross the feature API. */
public sealed class AnalyticsValue {
    public data class Text(public val value: String) : AnalyticsValue()
    public data class Integer(public val value: Long) : AnalyticsValue()
    public data class Decimal(public val value: Double) : AnalyticsValue()
}

public class AnalyticsPayload(public val name: String, parameters: Map<String, AnalyticsValue>) {
    public val parameters: Map<String, AnalyticsValue> = parameters.toMap()
}

/**
 * Synchronous, non-blocking SDK submission; core invokes methods serially.
 * Returning means accepted by SDK, not delivered remotely. Exceptions report transport failures.
 */
public interface AnalyticsSink {
    public fun setCollection(collection: AnalyticsCollection)
    public fun setIdentity(identity: AnalyticsIdentityPayload)
    public fun send(payload: AnalyticsPayload)
}

/** Explicit clears prevent properties from surviving logout or app restart inside the SDK. */
public class AnalyticsIdentityPayload(
    public val userId: AnalyticsUserId?,
    properties: Map<String, String>,
    clearedPropertyNames: List<String>,
) {
    public val properties: Map<String, String> = properties.toMap()
    public val clearedPropertyNames: List<String> = clearedPropertyNames.toList()
}
