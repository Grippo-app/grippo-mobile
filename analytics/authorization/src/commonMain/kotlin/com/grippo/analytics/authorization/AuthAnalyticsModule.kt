package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.internal.schema.AuthEventSchema
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class AuthAnalyticsModule {
    @Single
    @Named("authorization_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(AuthEventSchema.all)
}
