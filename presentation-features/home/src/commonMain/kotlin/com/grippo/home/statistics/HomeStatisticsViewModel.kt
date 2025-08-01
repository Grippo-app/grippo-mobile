package com.grippo.home.statistics

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.datetime.PeriodState
import kotlinx.collections.immutable.persistentListOf

internal class HomeStatisticsViewModel(
    private val dialogController: DialogController
) : BaseViewModel<HomeStatisticsState, HomeStatisticsDirection, HomeStatisticsLoader>(
    HomeStatisticsState()
), HomeStatisticsContract {

    override fun selectPeriod() {
        persistentListOf(
            PeriodState.DAILY
        )
        val dialog = DialogConfig.PeriodPicker(
            initial = state.value.period,
            available = persistentListOf(),
            onResult = { value -> update { it.copy(period = value) } }
        )

        dialogController.show(dialog)
    }

    override fun back() {
        navigateTo(HomeStatisticsDirection.Back)
    }
}