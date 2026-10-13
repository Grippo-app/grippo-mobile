package com.grippo.analytics.profile

import com.grippo.analytics.profile.internal.schema.ProfileInteractionSchema
import com.grippo.analytics.profile.internal.schema.ProfileOperationSchema
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class ProfileAnalyticsModule {
    @Single
    @Named("profile_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(
        ProfileInteractionSchema.all + ProfileOperationSchema.all
    )
}
