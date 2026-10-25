package com.grippo.toolkit.crashreporting.metadata

import com.grippo.toolkit.crashreporting.internal.CrashMetadataRules
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode

public class CrashMetadataKey<T> private constructor(
    public val name: String,
    private val encode: (CrashMetadataKey<T>, T) -> CrashMetadataEntry,
) {
    init {
        CrashMetadataRules.requireCode(name)
    }

    public fun value(value: T): CrashMetadataEntry = encode(this, value)

    public companion object {
        public fun code(name: String): CrashMetadataKey<CrashDiagnosticCode> =
            CrashMetadataKey(name) { key, value -> CrashMetadataEntry.Code(key, value) }

        public fun count(name: String): CrashMetadataKey<Long> =
            CrashMetadataKey(name) { key, value -> CrashMetadataEntry.Count(key, value) }

        public fun flag(name: String): CrashMetadataKey<Boolean> =
            CrashMetadataKey(name) { key, value -> CrashMetadataEntry.Flag(key, value) }
    }
}
