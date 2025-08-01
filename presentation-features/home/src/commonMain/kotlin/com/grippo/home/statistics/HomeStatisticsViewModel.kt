package com.grippo.home.statistics

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.datetime.PeriodState

internal class HomeStatisticsViewModel(
    private val dialogController: DialogController
) : BaseViewModel<HomeStatisticsState, HomeStatisticsDirection, HomeStatisticsLoader>(
    HomeStatisticsState()
), HomeStatisticsContract {

    override fun selectPeriod() {
        val available = listOf(
            PeriodState.DAILY,
            PeriodState.WEEKLY,
            PeriodState.MONTHLY,
            PeriodState.CUSTOM(range = state.value.period.range),
        )

        val dialog = DialogConfig.PeriodPicker(
            initial = state.value.period,
            available = available,
            onResult = { value -> update { it.copy(period = value) } }
        )

        dialogController.show(dialog)
    }

    override fun back() {
        navigateTo(HomeStatisticsDirection.Back)
    }
}