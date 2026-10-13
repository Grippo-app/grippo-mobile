package com.grippo.analytics.draft.training

import com.grippo.analytics.draft.training.internal.schema.DraftTrainingEventMatrix
import com.grippo.analytics.draft.training.internal.schema.DraftTrainingOperationSchema
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class DraftTrainingAnalyticsModule {
    @Single
    @Named("draft_training_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(DraftTrainingEventMatrix.all + DraftTrainingOperationSchema.all)
}
