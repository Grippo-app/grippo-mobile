package com.grippo.home.statistics

import com.grippo.core.BaseViewModel
import com.grippo.date.utils.DateTimeUtils
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.datetime.PeriodState

internal class HomeStatisticsViewModel(
    private val dialogController: DialogController
) : BaseViewModel<HomeStatisticsState, HomeStatisticsDirection, HomeStatisticsLoader>(
    HomeStatisticsState()
), HomeStatisticsContract {

    override fun selectPeriod() {
        val custom = (state.value.period as? PeriodState.CUSTOM) ?: PeriodState.CUSTOM(
            range = DateTimeUtils.thisDay(),
            limitations = DateTimeUtils.trailingYear()
        )

        val available = listOf(
            PeriodState.ThisDay,
            PeriodState.ThisWeek,
            PeriodState.ThisMonth,
            custom
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