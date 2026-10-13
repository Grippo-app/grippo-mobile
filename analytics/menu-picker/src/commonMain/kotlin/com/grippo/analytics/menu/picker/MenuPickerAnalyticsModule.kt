package com.grippo.analytics.menu.picker

import com.grippo.analytics.menu.picker.internal.schema.MenuPickerEventMatrix
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan
public class MenuPickerAnalyticsModule {
    @Single
    @Named("menu_picker_analytics")
    internal fun provideSchema(): AnalyticsSchema = AnalyticsSchema(MenuPickerEventMatrix.all)
}
