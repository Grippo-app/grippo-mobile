package com.grippo.authorization.registration.missing.equipment.internal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.SelectableCardSkeleton
import com.grippo.design.components.cards.SelectableCardStyle
import com.grippo.design.components.modifiers.shimmerAnimation
import com.grippo.design.components.segment.SegmentSkeleton
import com.grippo.design.core.AppTokens

@Composable
internal fun EquipmentsSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SegmentSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        )

        Spacer(modifier = Modifier.size(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .weight(1f)
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(6) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.shimmerAnimation(
                            visible = true,
                            radius = AppTokens.dp.shape.small
                        ).size(AppTokens.dp.size.componentHeight)
                    )

                    SelectableCardSkeleton(
                        modifier = Modifier.fillMaxWidth(),
                        style = SelectableCardStyle.Small("")
                    )
                }
            }
        }
    }
}
