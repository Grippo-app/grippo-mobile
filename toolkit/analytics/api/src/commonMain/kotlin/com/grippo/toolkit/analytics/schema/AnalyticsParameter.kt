package com.grippo.toolkit.analytics.schema

import com.grippo.toolkit.analytics.transport.AnalyticsValue

public class AnalyticsParameter internal constructor(
    public val key: AnalyticsParameterKey<*>,
    public val value: AnalyticsValue,
)
