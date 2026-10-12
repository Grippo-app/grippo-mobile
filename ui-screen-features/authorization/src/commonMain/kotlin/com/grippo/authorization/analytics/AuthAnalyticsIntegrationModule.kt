package com.grippo.authorization.analytics

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.AuthAnalytics.FailureClassifier
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
public class AuthAnalyticsIntegrationModule {
    @Single
    internal fun provideAuthAnalyticsProvider(analytics: AuthAnalytics): AuthAnalyticsProvider =
        AuthAnalyticsProvider(analytics)

    @Single
    internal fun provideFailureClassifier(): FailureClassifier = AuthFailureClassifierImpl
}
