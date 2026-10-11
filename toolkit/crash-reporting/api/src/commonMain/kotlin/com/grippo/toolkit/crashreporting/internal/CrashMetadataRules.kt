package com.grippo.toolkit.crashreporting.internal

/** Shared diagnostic vocabulary and per-report metadata limits. */
internal object CrashMetadataRules {
    const val MAX_ENTRIES = 32
    private val codePattern = Regex("[a-z][a-z0-9_]{0,63}")

    fun requireCode(value: String) {
        require(codePattern.matches(value)) { "Expected a stable crash diagnostic code" }
    }
}
