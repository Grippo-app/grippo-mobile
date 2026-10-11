package com.grippo.toolkit.analytics.core.internal

import com.grippo.toolkit.analytics.AnalyticsDefinitions
import com.grippo.toolkit.analytics.AnalyticsContext
import com.grippo.toolkit.analytics.AnalyticsEvent
import com.grippo.toolkit.analytics.AnalyticsIdentity
import com.grippo.toolkit.analytics.AnalyticsIdentityPayload
import com.grippo.toolkit.analytics.AnalyticsPayload
import com.grippo.toolkit.analytics.AnalyticsValue
import com.grippo.toolkit.analytics.DialogViewed
import com.grippo.toolkit.analytics.EventDefinition
import com.grippo.toolkit.analytics.FlowContext
import com.grippo.toolkit.analytics.OperationId
import com.grippo.toolkit.analytics.ScreenViewed
import com.grippo.toolkit.analytics.UserPropertyKey

/** App composition registers every definition once. Unknown/mismatched definitions are rejected. */
internal class AnalyticsCatalog(
    definitions: List<EventDefinition> = BUILT_IN_EVENTS,
    properties: List<UserPropertyKey<*>> = emptyList(),
) {
    private val definitions: Map<String, RegisteredEvent>
    private val properties: Map<String, UserPropertyKey<*>>

    init {
        // Reject oversized declarations before reading elements or allocating lookup tables.
        require(definitions.size <= MAX_EVENTS)
        require(properties.size <= MAX_PROPERTIES)
        definitions.forEach { definition ->
            require(validName(definition.name, 40) && definition.name !in RESERVED_EVENTS)
            require(definition.version > 0)
            require(definition.parameters.size + CONTEXT_KEYS.size <= 25)
            val names = HashSet<String>(definition.parameters.size)
            definition.parameters.forEach {
                require(validName(it.name, 40) && it.name !in CONTEXT_KEYS)
                require(names.add(it.name))
            }
        }
        properties.forEach { require(validName(it.name, 24) && it.name !in RESERVED_PROPERTIES) }
        this.definitions = definitions.associate { it.name to RegisteredEvent(it) }
        this.properties = properties.associateBy { it.name }
        require(this.definitions.size == definitions.size)
        require(this.properties.size == properties.size)
    }

    internal fun encode(
        event: AnalyticsEvent,
        context: AnalyticsContext,
        flow: FlowContext?,
        operation: OperationId?,
    ): AnalyticsPayload {
        val definition = event.definition
        val registered = definitions[definition.name]
        require(registered != null && registered.definition === definition) { "Unregistered event definition" }
        val parameters = event.parameters()
        val values = LinkedHashMap<String, AnalyticsValue>(registered.keys.size + CONTEXT_KEYS.size)
        parameters.forEach { parameter ->
            require(registered.keys[parameter.key.name] === parameter.key) { "Undeclared parameter" }
            require(parameter.key.name !in values) { "Duplicate parameters" }
            validateValue(parameter.value)
            values[parameter.key.name] = parameter.value
        }
        registered.requiredNames.forEach { name ->
            require(name in values) { "Missing required parameter" }
        }
        values["schema_version"] = registered.version
        values["launch_source"] = AnalyticsValue.Text(context.launchSource.analyticsCode)
        context.navigation?.let {
            values["screen_id"] = AnalyticsValue.Text(it.screen.analyticsCode)
            it.referrer?.let { referrer ->
                values["referrer_screen"] = AnalyticsValue.Text(referrer.analyticsCode)
            }
            values["navigation_reason"] = AnalyticsValue.Text(it.reason.analyticsCode)
            it.dialog?.let { dialog ->
                values["dialog_id"] = AnalyticsValue.Text(dialog.analyticsCode)
            }
        }
        flow?.let {
            values["flow_id"] = AnalyticsValue.Text(it.id.value)
            values["entry_point"] = AnalyticsValue.Text(it.entryPoint.analyticsCode)
            it.originScreen?.let { screen ->
                values["origin_screen"] = AnalyticsValue.Text(screen.analyticsCode)
            }
        }
        operation?.let { values["operation_id"] = AnalyticsValue.Text(it.value) }
        require(values.size <= 25)
        return AnalyticsPayload(definition.name, values)
    }

    internal fun validateIdentity(identity: AnalyticsIdentity) {
        require(identity.properties.size <= MAX_PROPERTIES)
        val names = HashSet<String>(identity.properties.size)
        identity.properties.forEach {
            require(names.add(it.key.name)) { "Duplicate user property" }
            require(properties[it.key.name] === it.key) { "Unregistered user property" }
            require(it.value.length in 1..36 && CODE_PATTERN.matches(it.value))
        }
    }

    internal fun encodeIdentity(identity: AnalyticsIdentity): AnalyticsIdentityPayload {
        validateIdentity(identity)
        val values = identity.properties.associate { it.key.name to it.value }
        return AnalyticsIdentityPayload(
            identity.userId,
            values,
            properties.keys.filter { it !in values })
    }

    private fun validateValue(value: AnalyticsValue) {
        when (value) {
            is AnalyticsValue.Text -> require(
                value.value.length in 1..100 && CODE_PATTERN.matches(
                    value.value
                )
            )

            is AnalyticsValue.Integer -> Unit
            is AnalyticsValue.Decimal -> require(value.value.isFinite())
        }
    }

    private fun validName(name: String, length: Int): Boolean =
        name.length <= length && CODE_PATTERN.matches(name) && RESERVED_PREFIXES.none(name::startsWith)

    private class RegisteredEvent(val definition: EventDefinition) {
        val keys = definition.parameters.associateBy { it.name }
        val requiredNames = definition.parameters.mapNotNull { if (it.required) it.name else null }
        val version = AnalyticsValue.Integer(definition.version.toLong())
    }

    companion object {
        private val BUILT_IN_EVENTS = listOf(ScreenViewed.Definition, DialogViewed.Definition)
        private const val MAX_EVENTS = 500
        private const val MAX_PROPERTIES = 25

        fun fromDefinitions(groups: List<AnalyticsDefinitions>): AnalyticsCatalog {
            // Include built-in definitions before allocating flattened feature collections.
            val eventCount = BUILT_IN_EVENTS.size.toLong() + groups.sumOf { it.events.size.toLong() }
            val propertyCount = groups.sumOf { it.properties.size.toLong() }
            require(eventCount <= MAX_EVENTS) { "Too many analytics event definitions" }
            require(propertyCount <= MAX_PROPERTIES) { "Too many analytics user properties" }
            val events = ArrayList<EventDefinition>(eventCount.toInt()).apply {
                addAll(BUILT_IN_EVENTS)
                groups.forEach { addAll(it.events) }
            }
            val properties = ArrayList<UserPropertyKey<*>>(propertyCount.toInt()).apply {
                groups.forEach { addAll(it.properties) }
            }
            return AnalyticsCatalog(events, properties)
        }

        private val CODE_PATTERN = Regex("[a-z][a-z0-9_]*")
        private val RESERVED_PREFIXES = listOf("firebase_", "google_", "ga_")
        private val RESERVED_PROPERTIES = setOf("first_open_time", "last_deep_link_referrer", "user_id")
        private val CONTEXT_KEYS = setOf(
            "schema_version",
            "launch_source",
            "screen_id",
            "referrer_screen",
            "navigation_reason",
            "dialog_id",
            "flow_id",
            "entry_point",
            "origin_screen",
            "operation_id"
        )
        private val RESERVED_EVENTS = setOf(
            "app_clear_data",
            "app_exception",
            "app_remove",
            "app_store_refund",
            "app_store_subscription_cancel",
            "app_store_subscription_convert",
            "app_store_subscription_renew",
            "app_update",
            "app_upgrade",
            "error",
            "first_open",
            "first_visit",
            "in_app_purchase",
            "notification_dismiss",
            "notification_foreground",
            "notification_open",
            "notification_receive",
            "os_update",
            "session_start",
            "session_start_with_rollout",
            "user_engagement",
            "ad_activeview",
            "ad_click",
            "ad_exposure",
            "ad_query",
            "ad_reward",
            "adunit_exposure",
            "firebase_conversion",
            "dynamic_link_app_open",
            "dynamic_link_app_update",
            "dynamic_link_first_open"
        )
    }
}
