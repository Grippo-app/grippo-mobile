package com.grippo.analytics.authorization.internal.operation

import com.grippo.analytics.authorization.internal.events.AuthPayloadEvent
import com.grippo.analytics.authorization.internal.models.AuthAction
import com.grippo.analytics.authorization.internal.models.ProfileAction
import com.grippo.analytics.authorization.internal.schema.AuthOperationSchema
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.Method
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.schema.AnalyticsEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsParameter
import com.grippo.toolkit.analytics.values.DurationMillis

internal class AuthOperationEvents<T>(private val operation: AuthOperation) : AnalyticsOperationEvents<T> {
    override fun started(): AnalyticsEvent {
        val schema = when (operation) {
            is AuthOperation.Login, AuthOperation.Register -> AuthOperationSchema.AttemptStarted
            AuthOperation.SessionCheck -> AuthOperationSchema.SessionCheckStarted
            AuthOperation.CreateProfile, AuthOperation.Logout -> AuthOperationSchema.ProfileStarted
        }
        return AuthPayloadEvent(schema, parameters())
    }

    override fun completed(result: T, duration: DurationMillis): AnalyticsEvent {
        val schema = when (operation) {
            is AuthOperation.Login, AuthOperation.Register -> AuthOperationSchema.AttemptSucceeded
            AuthOperation.SessionCheck -> AuthOperationSchema.SessionCheckCompleted
            AuthOperation.CreateProfile, AuthOperation.Logout -> AuthOperationSchema.ProfileCompleted
        }
        return terminal(schema, duration)
    }

    override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent {
        val schema = when (operation) {
            is AuthOperation.Login, AuthOperation.Register -> AuthOperationSchema.AttemptFailed
            AuthOperation.SessionCheck -> AuthOperationSchema.SessionCheckFailed
            AuthOperation.CreateProfile, AuthOperation.Logout -> AuthOperationSchema.ProfileFailed
        }
        return terminal(schema, duration)
    }

    override fun interrupted(duration: DurationMillis): AnalyticsEvent {
        val schema = when (operation) {
            is AuthOperation.Login, AuthOperation.Register -> AuthOperationSchema.AttemptInterrupted
            AuthOperation.SessionCheck -> AuthOperationSchema.SessionCheckInterrupted
            AuthOperation.CreateProfile, AuthOperation.Logout -> AuthOperationSchema.ProfileInterrupted
        }
        return terminal(schema, duration)
    }

    private fun terminal(schema: AnalyticsEventSchema, duration: DurationMillis): AnalyticsEvent {
        return AuthPayloadEvent(schema, parameters() + AuthOperationSchema.Duration.value(duration))
    }

    private fun parameters(): List<AnalyticsParameter> {
        return when (val operation = operation) {
            is AuthOperation.Login -> listOf(
                AuthOperationSchema.Method.value(operation.method),
                AuthOperationSchema.Action.value(AuthAction.Login),
            )
            AuthOperation.Register -> listOf(
                AuthOperationSchema.Method.value(Method.Email),
                AuthOperationSchema.Action.value(AuthAction.Register),
            )
            AuthOperation.SessionCheck -> emptyList()
            AuthOperation.CreateProfile -> listOf(AuthOperationSchema.ProfileAction.value(ProfileAction.Create))
            AuthOperation.Logout -> listOf(AuthOperationSchema.ProfileAction.value(ProfileAction.Logout))
        }
    }
}
