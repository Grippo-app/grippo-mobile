package com.grippo.toolkit.analytics.core.internal.schema

/** Shared wire schema. Feature parameters cannot shadow these fields. */
internal enum class AnalyticsContextField(val wireName: String) {
    SchemaVersion("schema_version"),
    LaunchSource("launch_source"),
    Screen("screen_id"),
    ReferrerScreen("referrer_screen"),
    NavigationReason("navigation_reason"),
    Dialog("dialog_id"),
    // Unused attribution names remain reserved; current APIs never encode them.
    Flow("flow_id"),
    EntryPoint("entry_point"),
    OriginScreen("origin_screen"),
    Operation("operation_id");

    companion object {
        val reservedNames: Set<String> = entries.mapTo(HashSet(entries.size)) { it.wireName }.also {
            check(it.size == entries.size) { "Duplicate analytics context field" }
        }
    }
}
