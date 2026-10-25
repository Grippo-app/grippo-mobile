package com.grippo.toolkit.crashreporting.transport

public object CrashStackTracePolicy {
    public val maximumCauseDepth: Int = 32
    public val maximumStackFrames: Int = 128
}
