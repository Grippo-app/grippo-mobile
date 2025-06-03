package com.grippo.home.statistics

import androidx.compose.runtime.Composable
import com.grippo.core.BaseComposeScreen
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun StatisticsScreen(
    state: StatisticsState,
    loaders: ImmutableSet<StatisticsLoader>,
    contract: StatisticsContract
) = BaseComposeScreen {
}