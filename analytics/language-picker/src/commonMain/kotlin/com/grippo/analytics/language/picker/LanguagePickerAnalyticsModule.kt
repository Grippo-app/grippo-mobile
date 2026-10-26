package com.grippo.analytics.language.picker

import com.grippo.analytics.language.picker.internal.schema.LanguagePickerEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class LanguagePickerAnalyticsModule {
    @Single
    @Named("language_picker_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(LanguagePickerEventMatrix.all)
}
