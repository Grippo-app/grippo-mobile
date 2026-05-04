package com.grippo.design.components.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
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
import com.grippo.design.resources.provider.onboarding_facts_equipment_all
import com.grippo.design.resources.provider.onboarding_facts_equipment_missing
import com.grippo.design.resources.provider.onboarding_facts_muscles_all
import com.grippo.design.resources.provider.onboarding_facts_muscles_excluded

@Composable
public fun OnboardingProfileFacts(
    modifier: Modifier = Modifier,
    heightDisplay: String,
    weightDisplay: String,
    excludedMusclesCount: Int,
    missingEquipmentCount: Int,
) {
    val heightUnit = AppTokens.strings.res(Res.string.cm)
    val weightUnit = AppTokens.strings.res(Res.string.kg)

    val musclesText = when {
        excludedMusclesCount <= 0 -> AppTokens.strings.res(Res.string.onboarding_facts_muscles_all)
        else -> AppTokens.strings.res(
            Res.string.onboarding_facts_muscles_excluded,
            excludedMusclesCount
        )
    }

    val equipmentText = when {
        missingEquipmentCount <= 0 -> AppTokens.strings.res(Res.string.onboarding_facts_equipment_all)
        else -> AppTokens.strings.res(
            Res.string.onboarding_facts_equipment_missing,
            missingEquipmentCount
        )
    }

    val gap = AppTokens.dp.onboarding.factChip.gap

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            FactChip(
                modifier = Modifier.weight(1f),
                icon = AppTokens.icons.Height,
                text = "$heightDisplay $heightUnit",
                tint = AppTokens.colors.icon.secondary,
            )

            FactChip(
                modifier = Modifier.weight(1f),
                icon = AppTokens.icons.Weight,
                text = "$weightDisplay $weightUnit",
                tint = AppTokens.colors.icon.secondary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            FactChip(
                modifier = Modifier.weight(1f),
                icon = AppTokens.icons.Muscle,
                text = musclesText,
                tint = AppTokens.colors.brand.color2,
            )

            FactChip(
                modifier = Modifier.weight(1f),
                icon = AppTokens.icons.Dumbbell,
                text = equipmentText,
                tint = AppTokens.colors.brand.color3,
            )
        }
    }
}

@Composable
private fun FactChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    tint: Color,
) {
    Row(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.onboarding.factChip.radius)
            )
            .padding(
                horizontal = AppTokens.dp.onboarding.factChip.horizontalPadding,
                vertical = AppTokens.dp.onboarding.factChip.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.onboarding.factChip.space)
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.onboarding.factChip.icon),
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
private fun OnboardingProfileFactsAllAvailablePreview() {
    PreviewContainer {
        OnboardingProfileFacts(
            modifier = Modifier.fillMaxWidth(),
            heightDisplay = "175",
            weightDisplay = "70.0",
            excludedMusclesCount = 0,
            missingEquipmentCount = 0,
        )
    }
}

@AppPreview
@Composable
private fun OnboardingProfileFactsCustomizedPreview() {
    PreviewContainer {
        OnboardingProfileFacts(
            modifier = Modifier.fillMaxWidth(),
            heightDisplay = "179",
            weightDisplay = "80.0",
            excludedMusclesCount = 3,
            missingEquipmentCount = 5,
        )
    }
}
