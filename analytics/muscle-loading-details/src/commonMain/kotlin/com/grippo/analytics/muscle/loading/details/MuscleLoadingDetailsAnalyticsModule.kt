package com.grippo.analytics.muscle.loading.details

import com.grippo.analytics.muscle.loading.details.internal.schema.MuscleLoadingDetailsEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class MuscleLoadingDetailsAnalyticsModule {
    @Single
    @Named("muscle_loading_details_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(MuscleLoadingDetailsEventMatrix.all)
}
