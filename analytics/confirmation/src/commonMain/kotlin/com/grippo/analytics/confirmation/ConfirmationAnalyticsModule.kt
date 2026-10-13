package com.grippo.analytics.confirmation

import com.grippo.analytics.confirmation.internal.schema.ConfirmationEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class ConfirmationAnalyticsModule {
    @Single
    @Named("confirmation_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(ConfirmationEventMatrix.all)
}
