package com.grippo.toolkit.crashreporting.values

import com.grippo.toolkit.crashreporting.internal.CrashMetadataRules

public data class CrashDiagnosticCode(public val value: String) {
    init {
        CrashMetadataRules.requireCode(value)
    }
}
