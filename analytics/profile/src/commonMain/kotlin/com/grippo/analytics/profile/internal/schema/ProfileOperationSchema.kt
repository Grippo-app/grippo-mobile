package com.grippo.analytics.profile.internal.schema

import com.grippo.analytics.profile.internal.encoding.ProfileEncoding
import com.grippo.analytics.profile.models.ProfileOperation
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object ProfileOperationSchema {
    val Operation = AnalyticsParameterKey.code<ProfileOperation>("profile_operation", encode = ProfileEncoding::code)
    val Duration = AnalyticsParameterKey.duration("duration_ms")

    val Started = AnalyticsEventSchema("profile_task_started", 1, listOf(Operation))
    val Completed = AnalyticsEventSchema("profile_task_completed", 1, listOf(Operation, Duration))
    val Failed = AnalyticsEventSchema("profile_task_failed", 1, listOf(Operation, Duration))
    val Interrupted = AnalyticsEventSchema("profile_task_interrupted", 1, listOf(Operation, Duration))
    val all = listOf(Started, Completed, Failed, Interrupted)
}
