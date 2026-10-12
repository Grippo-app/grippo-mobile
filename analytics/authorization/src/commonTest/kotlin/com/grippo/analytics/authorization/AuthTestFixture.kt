package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.AuthAnalytics.FailureClassifier
import com.grippo.analytics.authorization.internal.scenario.AuthAnalyticsImpl
import com.grippo.analytics.authorization.models.EntryPoint
import com.grippo.analytics.authorization.models.FailureCategory
import com.grippo.toolkit.analytics.context.FlowId

internal class TestFailure(val category: FailureCategory, val cancelled: Boolean = false) : Exception("private")
internal object TestClassifier : FailureClassifier {
    override fun category(error: Exception): FailureCategory = (error as? TestFailure)?.category ?: FailureCategory.Unexpected
    override fun isProviderCancellation(error: Exception): Boolean = (error as? TestFailure)?.cancelled == true
}
internal fun authorization(recorder: RecordingAuthAnalytics) =
    AuthAnalyticsImpl(recorder, TestClassifier).authorization(EntryPoint.TokenAbsent, FlowId.create(), null)
internal fun session(recorder: RecordingAuthAnalytics) = AuthAnalyticsImpl(recorder, TestClassifier).session(null)
