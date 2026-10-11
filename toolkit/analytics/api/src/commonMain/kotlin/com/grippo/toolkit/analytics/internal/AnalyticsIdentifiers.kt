package com.grippo.toolkit.analytics.internal

internal val opaqueIdPattern = Regex("[A-Za-z0-9_-]{1,64}")
private val codePattern = Regex("[a-z][a-z0-9_]{0,99}")

internal fun requireCode(value: String) {
    require(codePattern.matches(value)) { "Expected a stable analytics code" }
}
