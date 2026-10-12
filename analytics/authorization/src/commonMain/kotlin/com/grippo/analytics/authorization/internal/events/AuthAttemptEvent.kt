package com.grippo.analytics.authorization.internal.events

import com.grippo.analytics.authorization.internal.models.AuthAction
import com.grippo.analytics.authorization.internal.schema.AuthEventSchema
import com.grippo.analytics.authorization.models.FailureCategory
import com.grippo.analytics.authorization.models.Method
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.values.DurationMillis

internal sealed interface AuthAttemptEvent : AnalyticsEvent {
    class Cancelled(
        private val method: Method,
        private val action: AuthAction,
        private val duration: DurationMillis
    ) : AuthAttemptEvent {
        override val schema = AuthEventSchema.AttemptCancelled
        override fun parameters(): List<AnalyticsParameter> = listOf(
            AuthEventSchema.Method.value(method),
            AuthEventSchema.Action.value(action),
            AuthEventSchema.Duration.value(duration),
        )
    }

    class Started(
        private val method: Method,
        private val action: AuthAction
    ) : AuthAttemptEvent {
        override val schema = AuthEventSchema.AttemptStarted
        override fun parameters(): List<AnalyticsParameter> = listOf(
            AuthEventSchema.Method.value(method),
            AuthEventSchema.Action.value(action),
        )
    }

    class Succeeded(
        private val method: Method,
        private val action: AuthAction,
        private val duration: DurationMillis
    ) : AuthAttemptEvent {
        override val schema = AuthEventSchema.AttemptSucceeded
        override fun parameters(): List<AnalyticsParameter> = listOf(
            AuthEventSchema.Method.value(method),
            AuthEventSchema.Action.value(action),
            AuthEventSchema.Duration.value(duration),
        )
    }

    class Failed(
        private val method: Method,
        private val action: AuthAction,
        private val duration: DurationMillis,
        private val category: FailureCategory
    ) : AuthAttemptEvent {
        override val schema = AuthEventSchema.AttemptFailed
        override fun parameters(): List<AnalyticsParameter> = listOf(
            AuthEventSchema.Method.value(method),
            AuthEventSchema.Action.value(action),
            AuthEventSchema.Duration.value(duration),
            AuthEventSchema.Category.value(category),
        )
    }

    class Interrupted(
        private val method: Method,
        private val action: AuthAction,
        private val duration: DurationMillis
    ) : AuthAttemptEvent {
        override val schema = AuthEventSchema.AttemptInterrupted
        override fun parameters(): List<AnalyticsParameter> = listOf(
            AuthEventSchema.Method.value(method),
            AuthEventSchema.Action.value(action),
            AuthEventSchema.Duration.value(duration),
        )
    }
}
