package com.grippo.toolkit.analytics.transport

import com.grippo.toolkit.analytics.identity.AnalyticsUserId

/** Explicit clears prevent properties from surviving logout or app restart inside the SDK. */
public class AnalyticsIdentityPayload(
    public val userId: AnalyticsUserId?,
    properties: Map<String, String>,
    clearedPropertyNames: List<String>,
) {
    public val properties: Map<String, String> = properties.toMap()
    public val clearedPropertyNames: List<String> = clearedPropertyNames.toList()
}
