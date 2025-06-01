package com.grippo.authorization.registration.missing.equipment.internal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.SelectableCardSkeleton
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.components.segment.SegmentSkeleton

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
                SelectableCardSkeleton(
                    modifier = Modifier.fillMaxWidth(),
                    style = SelectableCardStyle.Small("")
                )
            }
        }
    }
}
