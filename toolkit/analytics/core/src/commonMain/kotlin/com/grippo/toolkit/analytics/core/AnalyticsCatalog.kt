package com.grippo.toolkit.analytics.core

import com.grippo.toolkit.analytics.*

/** App composition registers every definition once. Unknown/mismatched definitions are rejected. */
public class AnalyticsCatalog(
    definitions: List<EventDefinition> = listOf(ScreenViewed.Definition, DialogViewed.Definition),
    properties: List<UserPropertyKey<*>> = emptyList(),
) {
    private val definitions = definitions.associateBy { it.name }
    private val properties = properties.associateBy { it.name }

    init {
        require(definitions.size <= 500 && this.definitions.size == definitions.size)
        require(properties.size <= 25 && this.properties.size == properties.size)
        definitions.forEach { definition ->
            require(validName(definition.name, 40))
            require(definition.version > 0)
            require(definition.parameters.size + CONTEXT_KEYS.size <= 25)
            require(definition.parameters.map { it.name }.distinct().size == definition.parameters.size)
            definition.parameters.forEach {
                require(validName(it.name, 40) && it.name !in CONTEXT_KEYS)
            }
        }
        properties.forEach { require(validName(it.name, 24) && it.name !in setOf("first_open_time", "last_deep_link_referrer", "user_id")) }
    }

    internal fun encode(
        event: AnalyticsEvent,
        context: AnalyticsContext,
        flow: FlowContext?,
        operation: OperationId?,
    ): AnalyticsPayload {
        val definition = event.definition
        require(definitions[definition.name] === definition) { "Unregistered event definition" }
        val parameters = event.parameters().toList()
        require(parameters.map { it.key.name }.distinct().size == parameters.size) { "Duplicate parameters" }
        parameters.forEach { parameter ->
            require(definition.parameters.any { it === parameter.key }) { "Undeclared parameter" }
            validateValue(parameter.value)
        }
        definition.parameters.filter { it.required }.forEach { key ->
            require(parameters.any { it.key === key }) { "Missing required parameter" }
        }
        val values = parameters.associate { it.key.name to it.value }.toMutableMap()
        values["schema_version"] = AnalyticsValue.Integer(definition.version.toLong())
        values["launch_source"] = AnalyticsValue.Text(context.launchSource.analyticsCode)
        context.navigation?.let {
            values["screen_id"] = AnalyticsValue.Text(it.screen.analyticsCode)
            it.referrer?.let { referrer -> values["referrer_screen"] = AnalyticsValue.Text(referrer.analyticsCode) }
            values["navigation_reason"] = AnalyticsValue.Text(it.reason.analyticsCode)
            it.dialog?.let { dialog -> values["dialog_id"] = AnalyticsValue.Text(dialog.analyticsCode) }
        }
        flow?.let {
            values["flow_id"] = AnalyticsValue.Text(it.id.value)
            values["entry_point"] = AnalyticsValue.Text(it.entryPoint.analyticsCode)
            it.originScreen?.let { screen -> values["origin_screen"] = AnalyticsValue.Text(screen.analyticsCode) }
        }
        operation?.let { values["operation_id"] = AnalyticsValue.Text(it.value) }
        require(values.size <= 25)
        return AnalyticsPayload(definition.name, values)
    }

    internal fun validateIdentity(identity: AnalyticsIdentity) {
        require(identity.properties.size <= 25)
        require(identity.properties.map { it.key.name }.distinct().size == identity.properties.size)
        identity.properties.forEach {
            require(properties[it.key.name] === it.key) { "Unregistered user property" }
            require(it.value.length in 1..36 && Regex("[a-z][a-z0-9_]*").matches(it.value))
        }
    }

    internal fun encodeIdentity(identity: AnalyticsIdentity): AnalyticsIdentityPayload {
        val values = identity.properties.associate { it.key.name to it.value }
        return AnalyticsIdentityPayload(identity.userId, values, properties.keys.filter { it !in values })
    }

    private fun validateValue(value: AnalyticsValue) {
        when (value) {
            is AnalyticsValue.Text -> require(value.value.length in 1..100 && Regex("[a-z][a-z0-9_]*").matches(value.value))
            is AnalyticsValue.Integer -> Unit
            is AnalyticsValue.Decimal -> require(value.value.isFinite())
        }
    }

    private fun validName(name: String, length: Int): Boolean =
        name.length <= length && Regex("[a-z][a-z0-9_]*").matches(name) &&
            listOf("firebase_", "google_", "ga_").none(name::startsWith) &&
            name !in RESERVED_NAMES

    private companion object {
        val CONTEXT_KEYS = setOf("schema_version", "launch_source", "screen_id", "referrer_screen",
            "navigation_reason", "dialog_id", "flow_id", "entry_point", "origin_screen", "operation_id")
        val RESERVED_NAMES = setOf("app_clear_data", "app_exception", "app_remove", "app_store_refund",
            "app_store_subscription_cancel", "app_store_subscription_convert", "app_store_subscription_renew",
            "app_update", "app_upgrade", "error", "first_open", "first_visit", "in_app_purchase", "notification_dismiss",
            "notification_foreground", "notification_open", "notification_receive", "os_update",
            "session_start", "session_start_with_rollout", "user_engagement", "ad_activeview", "ad_click", "ad_exposure", "ad_query",
            "ad_reward", "adunit_exposure", "firebase_conversion", "dynamic_link_app_open",
            "dynamic_link_app_update", "dynamic_link_first_open")
    }
}
