package com.grippo.analytics.training

import com.grippo.analytics.training.internal.schema.TrainingEventMatrix
import com.grippo.analytics.training.internal.schema.TrainingOperationSchema
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class TrainingAnalyticsModule {
    @Single
    @Named("training_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(
        TrainingEventMatrix.all + TrainingOperationSchema.all
    )
}
