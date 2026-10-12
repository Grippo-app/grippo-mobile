package com.grippo.analytics.authorization.internal.operation

import com.grippo.analytics.authorization.AuthAnalytics.FailureClassifier
import com.grippo.analytics.authorization.internal.events.AuthAttemptEvent
import com.grippo.analytics.authorization.internal.models.AuthAction
import com.grippo.analytics.authorization.models.Method
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import com.grippo.toolkit.analytics.values.DurationMillis

internal class AuthAttemptEvents<T>(
    private val method: Method,
    private val action: AuthAction,
    private val classifier: FailureClassifier,
) : AnalyticsOperationEvents<T> {
    override fun started() = AuthAttemptEvent.Started(method, action)

    override fun completed(result: T, duration: DurationMillis) =
        AuthAttemptEvent.Succeeded(method, action, duration)

    override fun failed(error: Exception, duration: DurationMillis): AnalyticsEvent {
        return if (classifier.isProviderCancellation(error)) {
            AuthAttemptEvent.Cancelled(method, action, duration)
        } else {
            AuthAttemptEvent.Failed(method, action, duration, classifier.category(error))
        }
    }

    override fun interrupted(duration: DurationMillis) =
        AuthAttemptEvent.Interrupted(method, action, duration)
}
