package com.grippo.toolkit.crashreporting.context

import com.grippo.toolkit.crashreporting.internal.CrashMetadataRules
import com.grippo.toolkit.crashreporting.metadata.CrashMetadataEntry

public class CrashReportContext(metadata: List<CrashMetadataEntry> = emptyList()) {
    public val metadata: List<CrashMetadataEntry>

    init {
        require(metadata.size <= CrashMetadataRules.MAX_ENTRIES)
        this.metadata = metadata.toList()
        if (this.metadata.isNotEmpty()) {
            val keys = HashSet<String>(this.metadata.size)
            require(this.metadata.all { keys.add(it.key.name) })
        }
    }

    public companion object {
        public val Empty: CrashReportContext = CrashReportContext()
    }
}
