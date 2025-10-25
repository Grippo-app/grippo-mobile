package com.grippo.design.components.achievement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.state.achevements.AchievementState
import com.grippo.core.state.achevements.stubAchievements
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList

@Composable
public fun AchievementsCard(
    modifier: Modifier = Modifier,
    value: ImmutableList<AchievementState>,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        contentPadding = contentPadding
    ) {
        items(items = value, key = { it.key }) { item ->
            AchievementCard(
                value = item
            )
        }
    }
}

@AppPreview
@Composable
private fun AchievementsCardPreview() {
    PreviewContainer {
        AchievementsCard(
            value = stubAchievements()
        )
    }
}