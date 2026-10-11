package com.grippo.toolkit.crashreporting.values

import com.grippo.toolkit.crashreporting.internal.CrashMetadataRules

/** Stable diagnostic codes only; never user input, tokens or personal data. */
public data class CrashDiagnosticCode(public val value: String) {
    init {
        CrashMetadataRules.requireCode(value)
    }
}
