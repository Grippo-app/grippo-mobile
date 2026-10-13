package com.grippo.analytics.trainings

import com.grippo.analytics.trainings.internal.schema.TrainingsEventMatrix
import com.grippo.analytics.trainings.internal.schema.TrainingsOperationSchema
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class TrainingsAnalyticsModule {
    @Single
    @Named("trainings_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(TrainingsEventMatrix.all + TrainingsOperationSchema.all)
}
