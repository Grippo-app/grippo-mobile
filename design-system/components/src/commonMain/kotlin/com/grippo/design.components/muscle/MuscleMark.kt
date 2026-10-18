package com.grippo.design.components.muscle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

/** Anatomical identity beside an existing localized muscle or group label. */
@Composable
public fun MuscleMark(
    modifier: Modifier = Modifier,
    group: MuscleGroupEnumState,
) {
    MuscleMarkContent(modifier = modifier, icon = group.icon())
}

@Composable
public fun MuscleMark(
    modifier: Modifier = Modifier,
    muscle: MuscleEnumState,
) {
    MuscleMarkContent(modifier = modifier, icon = muscle.icon())
}

@Composable
private fun MuscleMarkContent(
    modifier: Modifier,
    icon: ImageVector,
) {
    val shape = RoundedCornerShape(AppTokens.dp.muscle.markRadius)
    Image(
        modifier = modifier
            .size(AppTokens.dp.muscle.markSize)
            .clip(shape)
            .background(AppTokens.colors.background.card)
            .padding(AppTokens.dp.muscle.markPadding),
        imageVector = icon,
        contentDescription = null,
    )
}

@AppPreview
@Composable
private fun MuscleMarkGroupsPreview() {
    PreviewContainer {
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
            MuscleGroupEnumState.entries.forEach { group ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    MuscleMark(group = group)
                    Text(
                        text = group.title().text(),
                        color = AppTokens.colors.text.primary,
                        style = AppTokens.typography.b14Med(),
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun MuscleMarkMusclesPreview() {
    PreviewContainer {
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
            listOf(
                MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
                MuscleEnumState.TRAPEZIUS,
                MuscleEnumState.RECTUS_ABDOMINIS,
                MuscleEnumState.QUADRICEPS,
                MuscleEnumState.FOREARM,
                MuscleEnumState.POSTERIOR_DELTOID,
            ).forEach { muscle ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    MuscleMark(muscle = muscle)
                    Text(
                        text = muscle.title().text(),
                        color = AppTokens.colors.text.primary,
                        style = AppTokens.typography.b14Med(),
                    )
                }
            }
        }
    }
}
