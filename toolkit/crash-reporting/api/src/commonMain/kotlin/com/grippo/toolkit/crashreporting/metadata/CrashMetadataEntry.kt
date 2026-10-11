package com.grippo.toolkit.crashreporting.metadata

import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode

public sealed class CrashMetadataEntry(public val key: CrashMetadataKey<*>) {
    public class Code internal constructor(
        key: CrashMetadataKey<CrashDiagnosticCode>,
        public val value: CrashDiagnosticCode
    ) : CrashMetadataEntry(key)

    public class Count internal constructor(
        key: CrashMetadataKey<Long>,
        public val value: Long
    ) : CrashMetadataEntry(key) {

        init {
            require(value >= 0)
        }
    }

    public class Flag internal constructor(
        key: CrashMetadataKey<Boolean>,
        public val value: Boolean
    ) : CrashMetadataEntry(key)
}
