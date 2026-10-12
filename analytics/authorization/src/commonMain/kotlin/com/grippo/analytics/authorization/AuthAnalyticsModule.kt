package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.internal.schema.AuthInteractionSchema
import com.grippo.analytics.authorization.internal.schema.AuthOperationSchema
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
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(
        AuthOperationSchema.all + AuthInteractionSchema.all
    )
}
