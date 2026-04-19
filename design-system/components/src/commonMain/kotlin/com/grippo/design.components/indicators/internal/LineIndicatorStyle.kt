package com.grippo.design.components.indicators.internal

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.design.resources.provider.AppColor

@Immutable
internal sealed interface LineIndicatorStyle {

    val track: Color

    @Immutable
    data class Solid(
        val color: Color,
        override val track: Color,
    ) : LineIndicatorStyle

    @Immutable
    data class Gradient(
        val colors: List<Color>,
        val stops: List<Float>? = null,
        override val track: Color,
    ) : LineIndicatorStyle {
        init {
            require(colors.size >= 2) {
                "LineIndicatorStyle.Gradient requires at least 2 colors."
            }
            if (stops != null) {
                require(stops.size == colors.size) {
                    "LineIndicatorStyle.Gradient stops size (${stops.size}) " +
                            "must match colors size (${colors.size})."
                }
            }
        }
    }
}

internal fun AppColor.Charts.IndicatorColors.IndicatorColors.toStyle(): LineIndicatorStyle {
    require(colors.isNotEmpty()) { "IndicatorColors.colors must not be empty." }
    return if (colors.size == 1) {
        LineIndicatorStyle.Solid(color = colors.first(), track = track)
    } else {
        LineIndicatorStyle.Gradient(colors = colors, stops = stops, track = track)
    }
}