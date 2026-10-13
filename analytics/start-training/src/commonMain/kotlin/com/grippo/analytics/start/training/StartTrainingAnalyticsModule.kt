package com.grippo.analytics.start.training

import com.grippo.analytics.start.training.internal.schema.StartTrainingEventMatrix
import com.grippo.analytics.start.training.internal.schema.StartTrainingOperationSchema
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class StartTrainingAnalyticsModule {
    @Single
    @Named("start_training_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(StartTrainingEventMatrix.all + StartTrainingOperationSchema.all)
}
