package com.grippo.analytics.authorization.internal.schema

import com.grippo.analytics.authorization.internal.models.AuthAction
import com.grippo.analytics.authorization.internal.models.ProfileAction
import com.grippo.analytics.authorization.models.Method
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object AuthOperationSchema {
    val Method = AnalyticsParameterKey.code<Method>("auth_method")
    val Action = AnalyticsParameterKey.code<AuthAction>("auth_action")
    val ProfileAction = AnalyticsParameterKey.code<ProfileAction>("profile_action")
    val Duration = AnalyticsParameterKey.duration("duration_ms")

    val AttemptStarted = AnalyticsEventSchema("auth_attempt_started", 3, listOf(Method, Action))
    val AttemptSucceeded = AnalyticsEventSchema("auth_attempt_succeeded", 3, listOf(Method, Action, Duration))
    val AttemptFailed = AnalyticsEventSchema("auth_attempt_failed", 5, listOf(Method, Action, Duration))
    val AttemptInterrupted = AnalyticsEventSchema("auth_attempt_interrupted", 3, listOf(Method, Action, Duration))
    val SessionCheckStarted = AnalyticsEventSchema("auth_session_check_started", 3, emptyList())
    val SessionCheckCompleted = AnalyticsEventSchema("auth_session_check_completed", 3, listOf(Duration))
    val SessionCheckFailed = AnalyticsEventSchema("auth_session_check_failed", 4, listOf(Duration))
    val SessionCheckInterrupted = AnalyticsEventSchema("auth_session_check_interrupted", 3, listOf(Duration))
    val ProfileStarted = AnalyticsEventSchema("profile_operation_started", 1, listOf(ProfileAction))
    val ProfileCompleted = AnalyticsEventSchema("profile_operation_completed", 1, listOf(ProfileAction, Duration))
    val ProfileFailed = AnalyticsEventSchema("profile_operation_failed", 2, listOf(ProfileAction, Duration))
    val ProfileInterrupted = AnalyticsEventSchema("profile_operation_interrupted", 1, listOf(ProfileAction, Duration))

    val all = listOf(
        AttemptStarted,
        AttemptSucceeded,
        AttemptFailed,
        AttemptInterrupted,
        SessionCheckStarted,
        SessionCheckCompleted,
        SessionCheckFailed,
        SessionCheckInterrupted,
        ProfileStarted,
        ProfileCompleted,
        ProfileFailed,
        ProfileInterrupted,
    )
}
