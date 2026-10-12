package com.grippo.analytics.authorization.internal.schema

import com.grippo.analytics.authorization.internal.models.AuthAction
import com.grippo.analytics.authorization.models.ExitReason
import com.grippo.analytics.authorization.models.FailureCategory
import com.grippo.analytics.authorization.models.Method
import com.grippo.analytics.authorization.models.Step
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameterKey

internal object AuthEventSchema {
    val Method = AnalyticsParameterKey.code<Method>("auth_method")
    val Action = AnalyticsParameterKey.code<AuthAction>("auth_action")
    val Duration = AnalyticsParameterKey.duration("duration_ms")
    val Category = AnalyticsParameterKey.code<FailureCategory>("failure_category")
    val ExitReason = AnalyticsParameterKey.code<ExitReason>("exit_reason")
    val Step = AnalyticsParameterKey.code<Step>("auth_step")
    val FromStep = AnalyticsParameterKey.code<Step>("from_step")
    val ToStep = AnalyticsParameterKey.code<Step>("to_step")
    val Transition = AnalyticsParameterKey.code<NavigationReason>("transition_reason")

    val AttemptStarted = AnalyticsEventSchema("auth_attempt_started", 3, listOf(Method, Action))
    val AttemptSucceeded =
        AnalyticsEventSchema("auth_attempt_succeeded", 3, listOf(Method, Action, Duration))
    val AttemptFailed = AnalyticsEventSchema(
        "auth_attempt_failed",
        4,
        listOf(Method, Action, Duration, Category)
    )
    val AttemptInterrupted =
        AnalyticsEventSchema("auth_attempt_interrupted", 3, listOf(Method, Action, Duration))
    val SessionCheckStarted = AnalyticsEventSchema("auth_session_check_started", 3, listOf())
    val SessionCheckCompleted =
        AnalyticsEventSchema("auth_session_check_completed", 3, listOf(Duration))
    val SessionCheckFailed =
        AnalyticsEventSchema("auth_session_check_failed", 3, listOf(Duration, Category))
    val SessionCheckInterrupted =
        AnalyticsEventSchema("auth_session_check_interrupted", 3, listOf(Duration))
    val AttemptCancelled =
        AnalyticsEventSchema("auth_attempt_cancelled", 3, listOf(Method, Action, Duration))
    val FlowStarted = AnalyticsEventSchema("auth_flow_started", 1, listOf())
    val FlowExited = AnalyticsEventSchema("auth_flow_exited", 1, listOf(ExitReason, Step))
    val StepChanged =
        AnalyticsEventSchema("auth_step_changed", 1, listOf(FromStep, ToStep, Transition))

    val all = listOf(
        AttemptStarted,
        AttemptSucceeded,
        AttemptFailed,
        AttemptInterrupted,
        SessionCheckStarted,
        SessionCheckCompleted,
        SessionCheckFailed,
        SessionCheckInterrupted,
        AttemptCancelled,
        FlowStarted,
        FlowExited,
        StepChanged,
    )
}
