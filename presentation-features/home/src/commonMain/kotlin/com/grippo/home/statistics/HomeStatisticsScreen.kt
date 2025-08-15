package com.grippo.home.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height as BoxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.date.utils.DateFormat
import com.grippo.design.components.chart.AreaChart
import com.grippo.design.components.chart.BarChart
import com.grippo.design.components.chart.HeatmapChart
import com.grippo.design.components.chart.ProgressChart
import com.grippo.design.components.chart.RadarChart
import com.grippo.design.components.chart.Sparkline
import com.grippo.design.components.datetime.PeriodPicker
import com.grippo.design.components.statistics.ChartCard
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.statistics
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun HomeStatisticsScreen(
    state: HomeStatisticsState,
    loaders: ImmutableSet<HomeStatisticsLoader>,
    contract: HomeStatisticsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.statistics),
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = AppTokens.dp.contentPadding.content,
                        start = AppTokens.dp.screen.horizontalPadding,
                        end = AppTokens.dp.screen.horizontalPadding
                    ),
            ) {
                PeriodPicker(
                    value = state.period,
                    format = DateFormat.MMM_dd,
                    onClick = contract::selectPeriod
                )
            }
        }
    )

    LazyVerticalStaggeredGrid(
        modifier = Modifier.fillMaxWidth()
            .weight(1f),
        columns = StaggeredGridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalItemSpacing = AppTokens.dp.contentPadding.content,
        contentPadding = PaddingValues(
            horizontal = AppTokens.dp.screen.horizontalPadding,
            vertical = AppTokens.dp.contentPadding.content
        )
    ) {
        // Hero area chart — full width
        item(span = StaggeredGridItemSpan.FullLine) {
            ChartCard(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.8f),
                content = {
                    Text(
                        text = AppTokens.strings.res(Res.string.statistics),
                        style = AppTokens.typography.b11Med().copy(color = AppTokens.colors.text.secondary)
                    )
                    Spacer(modifier = Modifier.BoxHeight(8.dp))
                    AreaChart(modifier = Modifier.fillMaxSize())
                }
            )
        }

        // Bar and Sparkline — side by side
        item {
            ChartCard(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                content = {
                    Text(
                        text = "Weekly Volume",
                        style = AppTokens.typography.b11Med().copy(color = AppTokens.colors.text.secondary)
                    )
                    Spacer(modifier = Modifier.BoxHeight(8.dp))
                    BarChart(modifier = Modifier.fillMaxSize())
                }
            )
        }
        item {
            ChartCard(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                content = {
                    Text(
                        text = "Trend",
                        style = AppTokens.typography.b11Med().copy(color = AppTokens.colors.text.secondary)
                    )
                    Spacer(modifier = Modifier.BoxHeight(8.dp))
                    Sparkline(modifier = Modifier.fillMaxSize())
                }
            )
        }

        // Heatmap — full width
        item(span = StaggeredGridItemSpan.FullLine) {
            ChartCard(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.5f),
                content = {
                    Text(
                        text = "Activity Heatmap",
                        style = AppTokens.typography.b11Med().copy(color = AppTokens.colors.text.secondary)
                    )
                    Spacer(modifier = Modifier.BoxHeight(8.dp))
                    HeatmapChart(modifier = Modifier.fillMaxSize())
                }
            )
        }

        // Radar and Progress — side by side (progress fixed height for readability)
        item {
            ChartCard(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                content = {
                    Text(
                        text = "Muscle Balance",
                        style = AppTokens.typography.b11Med().copy(color = AppTokens.colors.text.secondary)
                    )
                    Spacer(modifier = Modifier.BoxHeight(8.dp))
                    RadarChart(modifier = Modifier.fillMaxSize())
                }
            )
        }
        item {
            ChartCard(
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Text(
                        text = "Progress",
                        style = AppTokens.typography.b11Med().copy(color = AppTokens.colors.text.secondary)
                    )
                    Spacer(modifier = Modifier.BoxHeight(8.dp))
                    ProgressChart(modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            )
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        HomeStatisticsScreen(
            state = HomeStatisticsState(),
            loaders = persistentSetOf(),
            contract = HomeStatisticsContract.Empty
        )
    }
}