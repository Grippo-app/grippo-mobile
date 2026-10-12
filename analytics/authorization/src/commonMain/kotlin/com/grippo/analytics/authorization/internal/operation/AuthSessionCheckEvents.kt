package com.grippo.analytics.authorization.internal.operation

import com.grippo.analytics.authorization.AuthAnalytics.FailureClassifier
import com.grippo.analytics.authorization.internal.events.AuthSessionEvent
import com.grippo.toolkit.analytics.operation.AnalyticsOperationEvents
import com.grippo.toolkit.analytics.values.DurationMillis

internal class AuthSessionCheckEvents<T>(private val classifier: FailureClassifier) :
    AnalyticsOperationEvents<T> {
    override fun started() = AuthSessionEvent.Started
    override fun completed(result: T, duration: DurationMillis) = AuthSessionEvent.Completed(duration)
    override fun failed(error: Exception, duration: DurationMillis) =
        AuthSessionEvent.Failed(duration, classifier.category(error))
    override fun interrupted(duration: DurationMillis) = AuthSessionEvent.Interrupted(duration)
}
