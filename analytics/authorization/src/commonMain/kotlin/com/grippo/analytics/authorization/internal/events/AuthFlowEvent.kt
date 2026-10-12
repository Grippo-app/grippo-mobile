package com.grippo.analytics.authorization.internal.events

import com.grippo.analytics.authorization.internal.schema.AuthEventSchema
import com.grippo.analytics.authorization.models.ExitReason
import com.grippo.analytics.authorization.models.Step
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsParameter

internal sealed interface AuthFlowEvent : AnalyticsEvent {
    class StepChanged(
        private val fromStep: Step,
        private val toStep: Step,
        private val transition: NavigationReason
    ) : AuthFlowEvent {
        override val schema = AuthEventSchema.StepChanged
        override fun parameters(): List<AnalyticsParameter> = listOf(
            AuthEventSchema.FromStep.value(fromStep),
            AuthEventSchema.ToStep.value(toStep),
            AuthEventSchema.Transition.value(transition),
        )
    }

    class FlowExited(
        private val exitReason: ExitReason,
        private val step: Step
    ) : AuthFlowEvent {
        override val schema = AuthEventSchema.FlowExited
        override fun parameters(): List<AnalyticsParameter> = listOf(
            AuthEventSchema.ExitReason.value(exitReason),
            AuthEventSchema.Step.value(step),
        )
    }

    object FlowStarted : AuthFlowEvent {
        override val schema = AuthEventSchema.FlowStarted
        override fun parameters(): List<AnalyticsParameter> = listOf()
    }
}
