package com.grippo.design.components.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.cm
import com.grippo.design.resources.provider.icons.Dumbbell
import com.grippo.design.resources.provider.icons.Height
import com.grippo.design.resources.provider.icons.Muscle
import com.grippo.design.resources.provider.icons.Weight
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.welcome_facts_equipment_all
import com.grippo.design.resources.provider.welcome_facts_equipment_missing
import com.grippo.design.resources.provider.welcome_facts_muscles_all
import com.grippo.design.resources.provider.welcome_facts_muscles_excluded

@Composable
public fun WelcomeBlock(
    modifier: Modifier = Modifier,
    experience: ExperienceEnumState,
    heightDisplay: String,
    weightDisplay: String,
    excludedMusclesCount: Int,
    missingEquipmentCount: Int,
) {
    val description = experience.description().text()
    val motto = experience.motto()
    val accent = experience.color()
    val cardColor = AppTokens.colors.background.card

    val gradient = remember(accent) {
        Brush.verticalGradient(
            colors = listOf(
                accent.copy(alpha = 0.18f),
                Color.Transparent,
            )
        )
    }

    val shape = RoundedCornerShape(AppTokens.dp.welcome.card.radius)

    val heightUnit = AppTokens.strings.res(Res.string.cm)
    val weightUnit = AppTokens.strings.res(Res.string.kg)

    val musclesText = when {
        excludedMusclesCount <= 0 -> AppTokens.strings.res(Res.string.welcome_facts_muscles_all)
        else -> AppTokens.strings.res(
            Res.string.welcome_facts_muscles_excluded,
            excludedMusclesCount
        )
    }

    val equipmentText = when {
        missingEquipmentCount <= 0 -> AppTokens.strings.res(Res.string.welcome_facts_equipment_all)
        else -> AppTokens.strings.res(
            Res.string.welcome_facts_equipment_missing,
            missingEquipmentCount
        )
    }

    val gap = AppTokens.dp.welcome.fact.gap

    Column(
        modifier = modifier
            .background(color = cardColor, shape = shape)
            .background(brush = gradient, shape = shape)
            .padding(
                horizontal = AppTokens.dp.welcome.card.horizontalPadding,
                vertical = AppTokens.dp.welcome.card.verticalPadding
            ),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = motto,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = description,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                FactRow(
                    modifier = Modifier.weight(1f),
                    icon = AppTokens.icons.Height,
                    text = "$heightDisplay $heightUnit",
                    tint = AppTokens.colors.context.body,
                )

                FactRow(
                    modifier = Modifier.weight(1f),
                    icon = AppTokens.icons.Weight,
                    text = "$weightDisplay $weightUnit",
                    tint = AppTokens.colors.context.body,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                FactRow(
                    modifier = Modifier.weight(1f),
                    icon = AppTokens.icons.Muscle,
                    text = musclesText,
                    tint = AppTokens.colors.context.muscle,
                )

                FactRow(
                    modifier = Modifier.weight(1f),
                    icon = AppTokens.icons.Dumbbell,
                    text = equipmentText,
                    tint = AppTokens.colors.context.equipment,
                )
            }
        }
    }
}

@Composable
private fun FactRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    tint: Color,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.welcome.fact.space),
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.welcome.fact.icon),
            imageVector = icon,
            contentDescription = null,
            tint = tint,
        )

        Text(
            text = text,
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@AppPreview
@Composable
private fun WelcomeBlockBeginnerPreview() {
    PreviewContainer {
        WelcomeBlock(
            modifier = Modifier.fillMaxWidth(),
            experience = ExperienceEnumState.BEGINNER,
            heightDisplay = "175",
            weightDisplay = "70.0",
            excludedMusclesCount = 0,
            missingEquipmentCount = 0,
        )
    }
}

@AppPreview
@Composable
private fun WelcomeBlockProPreview() {
    PreviewContainer {
        WelcomeBlock(
            modifier = Modifier.fillMaxWidth(),
            experience = ExperienceEnumState.PRO,
            heightDisplay = "179",
            weightDisplay = "80.0",
            excludedMusclesCount = 3,
            missingEquipmentCount = 5,
        )
    }
}
