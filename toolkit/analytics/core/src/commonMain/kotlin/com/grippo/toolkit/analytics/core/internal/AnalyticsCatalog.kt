package com.grippo.toolkit.analytics.core.internal

import com.grippo.toolkit.analytics.context.AnalyticsContext
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.core.internal.schema.AnalyticsContextField
import com.grippo.toolkit.analytics.core.internal.schema.AnalyticsSchemaRules
import com.grippo.toolkit.analytics.events.ScreenViewed
import com.grippo.toolkit.analytics.identity.AnalyticsIdentity
import com.grippo.toolkit.analytics.identity.AnalyticsUserPropertyKey
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import com.grippo.toolkit.analytics.transport.AnalyticsIdentityPayload
import com.grippo.toolkit.analytics.transport.AnalyticsPayload
import com.grippo.toolkit.analytics.transport.AnalyticsValue

/** App composition registers every definition once. Unknown/mismatched definitions are rejected. */
internal class AnalyticsCatalog(
    definitions: List<AnalyticsEventSchema> = BUILT_IN_EVENTS,
    properties: List<AnalyticsUserPropertyKey<*>> = emptyList(),
) {
    private val definitions: Map<String, RegisteredEvent>
    private val properties: Map<String, AnalyticsUserPropertyKey<*>>

    init {
        // Reject oversized declarations before reading elements or allocating lookup tables.
        require(definitions.size <= AnalyticsSchemaRules.MAX_EVENTS)
        require(properties.size <= AnalyticsSchemaRules.MAX_PROPERTIES)
        definitions.forEach { definition ->
            require(
                validName(definition.name, AnalyticsSchemaRules.MAX_NAME_LENGTH) &&
                    definition.name !in AnalyticsSchemaRules.reservedEvents
            )
            require(definition.version > 0)
            require(
                definition.parameters.size + AnalyticsContextField.reservedNames.size <=
                    AnalyticsSchemaRules.MAX_PARAMETERS
            )
            val names = HashSet<String>(definition.parameters.size)
            definition.parameters.forEach {
                require(
                    validName(it.name, AnalyticsSchemaRules.MAX_NAME_LENGTH) &&
                        it.name !in AnalyticsContextField.reservedNames
                )
                require(names.add(it.name))
            }
        }
        properties.forEach {
            require(
                validName(it.name, AnalyticsSchemaRules.MAX_PROPERTY_NAME_LENGTH) &&
                    it.name !in AnalyticsSchemaRules.reservedProperties
            )
        }
        this.definitions = definitions.associate { it.name to RegisteredEvent(it) }
        this.properties = properties.associateBy { it.name }
        require(this.definitions.size == definitions.size)
        require(this.properties.size == properties.size)
    }

    internal fun encode(
        event: AnalyticsEvent,
        context: AnalyticsContext,
        operation: OperationId?,
    ): AnalyticsPayload {
        val definition = event.schema
        val registered = definitions[definition.name]
        require(registered != null && registered.schema === definition) { "Unregistered event definition" }
        val parameters = event.parameters()
        val values = LinkedHashMap<String, AnalyticsValue>(registered.keys.size + AnalyticsContextField.reservedNames.size)
        parameters.forEach { parameter ->
            require(registered.keys[parameter.key.name] === parameter.key) { "Undeclared parameter" }
            require(parameter.key.name !in values) { "Duplicate parameters" }
            validateValue(parameter.value)
            values[parameter.key.name] = parameter.value
        }
        registered.requiredNames.forEach { name ->
            require(name in values) { "Missing required parameter" }
        }
        values[AnalyticsContextField.SchemaVersion.wireName] = registered.version
        values[AnalyticsContextField.LaunchSource.wireName] = AnalyticsValue.Text(context.launchSource.analyticsCode)
        // Operations are feature-owned; visible-screen context is not operation attribution.
        context.navigation?.takeIf { operation == null }?.let {
            values[AnalyticsContextField.Screen.wireName] = AnalyticsValue.Text(it.screen.analyticsCode)
            it.referrer?.let { referrer ->
                values[AnalyticsContextField.ReferrerScreen.wireName] = AnalyticsValue.Text(referrer.analyticsCode)
            }
            values[AnalyticsContextField.NavigationReason.wireName] = AnalyticsValue.Text(it.reason.analyticsCode)
            it.dialog?.let { dialog ->
                values[AnalyticsContextField.Dialog.wireName] = AnalyticsValue.Text(dialog.analyticsCode)
            }
        }
        operation?.let { values[AnalyticsContextField.Operation.wireName] = AnalyticsValue.Text(it.value) }
        require(values.size <= AnalyticsSchemaRules.MAX_PARAMETERS)
        return AnalyticsPayload(definition.name, values)
    }

    private fun validateIdentity(identity: AnalyticsIdentity) {
        require(identity.properties.size <= AnalyticsSchemaRules.MAX_PROPERTIES)
        val names = HashSet<String>(identity.properties.size)
        identity.properties.forEach {
            require(names.add(it.key.name)) { "Duplicate user property" }
            require(properties[it.key.name] === it.key) { "Unregistered user property" }
            require(
                it.value.length in 1..AnalyticsSchemaRules.MAX_PROPERTY_VALUE_LENGTH &&
                    AnalyticsSchemaRules.codePattern.matches(it.value)
            )
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
                value.value.length in 1..AnalyticsSchemaRules.MAX_TEXT_LENGTH && AnalyticsSchemaRules.codePattern.matches(
                    value.value
                )
            )

            is AnalyticsValue.Integer -> Unit
            is AnalyticsValue.Decimal -> require(value.value.isFinite())
        }
    }

    private fun validName(name: String, length: Int): Boolean =
        name.length <= length && AnalyticsSchemaRules.codePattern.matches(name) &&
            AnalyticsSchemaRules.reservedPrefixes.none(name::startsWith)

    private class RegisteredEvent(val schema: AnalyticsEventSchema) {
        val keys = schema.parameters.associateBy { it.name }
        val requiredNames = schema.parameters.mapNotNull { if (it.required) it.name else null }
        val version = AnalyticsValue.Integer(schema.version.toLong())
    }

    companion object {
        private val BUILT_IN_EVENTS = listOf(ScreenViewed.Schema)

        fun fromSchemas(groups: List<AnalyticsSchema>): AnalyticsCatalog {
            // Include built-in definitions before allocating flattened feature collections.
            val eventCount = BUILT_IN_EVENTS.size.toLong() + groups.sumOf { it.events.size.toLong() }
            val propertyCount = groups.sumOf { it.properties.size.toLong() }
            require(eventCount <= AnalyticsSchemaRules.MAX_EVENTS) { "Too many analytics event definitions" }
            require(propertyCount <= AnalyticsSchemaRules.MAX_PROPERTIES) { "Too many analytics user properties" }
            val events = ArrayList<AnalyticsEventSchema>(eventCount.toInt()).apply {
                addAll(BUILT_IN_EVENTS)
                groups.forEach { addAll(it.events) }
            }
            val properties = ArrayList<AnalyticsUserPropertyKey<*>>(propertyCount.toInt()).apply {
                groups.forEach { addAll(it.properties) }
            }
            return AnalyticsCatalog(events, properties)
        }
    }
}
