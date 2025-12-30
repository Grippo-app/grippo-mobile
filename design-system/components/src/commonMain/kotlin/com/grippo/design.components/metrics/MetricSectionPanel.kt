package com.grippo.design.components.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens

@Composable
public fun MetricSectionPanel(
    modifier: Modifier = Modifier,
    decoration: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .height(intrinsicSize = IntrinsicSize.Min)
            .background(
                AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.home.highlights.panel.radius)
            )
    ) {
        decoration?.invoke(this)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTokens.dp.contentPadding.content),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            content = content
        )
    }
}
