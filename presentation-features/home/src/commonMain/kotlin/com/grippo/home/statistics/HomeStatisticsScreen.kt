package com.grippo.home.statistics

import androidx.compose.runtime.Composable
import com.grippo.core.BaseComposeScreen
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableSet

@Composable
internal fun HomeStatisticsScreen(
    state: StatisticsState,
    loaders: ImmutableSet<HomeStatisticsLoader>,
    contract: HomeStatisticsContract
) = BaseComposeScreen {
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {

    }
}