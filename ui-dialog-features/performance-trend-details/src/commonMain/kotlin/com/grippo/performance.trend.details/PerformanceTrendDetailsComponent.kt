package com.grippo.performance.trend.details

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.toolkit.date.utils.DateRange

public class PerformanceTrendDetailsComponent(
    componentContext: ComponentContext,
    range: DateRange,
    metricType: PerformanceMetricTypeState,
    private val back: () -> Unit,
) : BaseComponent<PerformanceTrendDetailsDirection>(componentContext) {

    override val viewModel: PerformanceTrendDetailsViewModel = componentContext.retainedInstance {
        PerformanceTrendDetailsViewModel(
            range = range,
            metricType = metricType,
            trainingFeature = getKoin().get(),
            performanceTrendUseCase = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: PerformanceTrendDetailsDirection) {
        when (direction) {
            PerformanceTrendDetailsDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        PerformanceTrendDetailsScreen(state.value, loaders.value, viewModel)
    }
}
