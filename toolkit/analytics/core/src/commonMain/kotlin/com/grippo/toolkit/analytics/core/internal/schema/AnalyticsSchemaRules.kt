package com.grippo.toolkit.analytics.core.internal.schema

/** Transport constraints, distinct from feature declarations and shared context fields. */
internal object AnalyticsSchemaRules {
    const val MAX_EVENTS = 500
    const val MAX_PROPERTIES = 25
    const val MAX_PARAMETERS = 25
    const val MAX_NAME_LENGTH = 40
    const val MAX_PROPERTY_NAME_LENGTH = 24
    const val MAX_PROPERTY_VALUE_LENGTH = 36
    const val MAX_TEXT_LENGTH = 100

    val codePattern = Regex("[a-z][a-z0-9_]*")
    val reservedPrefixes = listOf("firebase_", "google_", "ga_")
    val reservedProperties = setOf("first_open_time", "last_deep_link_referrer", "user_id")
    val reservedEvents = setOf(
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
