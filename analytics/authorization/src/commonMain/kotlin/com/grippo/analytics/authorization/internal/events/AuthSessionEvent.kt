package com.grippo.analytics.authorization.internal.events

import com.grippo.analytics.authorization.internal.schema.AuthEventSchema
import com.grippo.analytics.authorization.models.FailureCategory
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.values.DurationMillis

internal sealed interface AuthSessionEvent : AnalyticsEvent {
    class Interrupted(
        private val duration: DurationMillis
    ) : AuthSessionEvent {
        override val schema = AuthEventSchema.SessionCheckInterrupted
        override fun parameters(): List<AnalyticsParameter> =
            listOf(AuthEventSchema.Duration.value(duration))
    }

    class Completed(
        private val duration: DurationMillis
    ) : AuthSessionEvent {
        override val schema = AuthEventSchema.SessionCheckCompleted
        override fun parameters(): List<AnalyticsParameter> = listOf(
            AuthEventSchema.Duration.value(duration),
        )
    }

    object Started : AuthSessionEvent {
        override val schema = AuthEventSchema.SessionCheckStarted
        override fun parameters(): List<AnalyticsParameter> = listOf()
    }

    class Failed(
        private val duration: DurationMillis,
        private val category: FailureCategory
    ) : AuthSessionEvent {
        override val schema = AuthEventSchema.SessionCheckFailed
        override fun parameters(): List<AnalyticsParameter> = listOf(
            AuthEventSchema.Duration.value(duration),
            AuthEventSchema.Category.value(category),
        )
    }
}
