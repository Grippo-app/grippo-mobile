package com.grippo.design.components.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import com.grippo.design.resources.provider.icons.Dumbbell
import com.grippo.design.resources.provider.icons.Muscle
import com.grippo.design.resources.provider.onboarding_recap_equipment_all
import com.grippo.design.resources.provider.onboarding_recap_equipment_missing
import com.grippo.design.resources.provider.onboarding_recap_muscles_all
import com.grippo.design.resources.provider.onboarding_recap_muscles_excluded

@Composable
public fun OnboardingRecapChips(
    modifier: Modifier = Modifier,
    excludedMusclesCount: Int,
    missingEquipmentCount: Int,
) {
    val musclesText = when {
        excludedMusclesCount <= 0 -> AppTokens.strings.res(Res.string.onboarding_recap_muscles_all)
        else -> AppTokens.strings.res(
            Res.string.onboarding_recap_muscles_excluded,
            excludedMusclesCount
        )
    }

    val equipmentText = when {
        missingEquipmentCount <= 0 -> AppTokens.strings.res(Res.string.onboarding_recap_equipment_all)
        else -> AppTokens.strings.res(
            Res.string.onboarding_recap_equipment_missing,
            missingEquipmentCount
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.onboarding.recapChip.gap)
    ) {
        RecapChip(
            modifier = Modifier.weight(1f),
            icon = AppTokens.icons.Muscle,
            text = musclesText,
            tint = AppTokens.colors.brand.color2
        )

        RecapChip(
            modifier = Modifier.weight(1f),
            icon = AppTokens.icons.Dumbbell,
            text = equipmentText,
            tint = AppTokens.colors.brand.color3
        )
    }
}

@Composable
private fun RecapChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    tint: Color,
) {
    Row(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.onboarding.recapChip.radius)
            )
            .padding(
                horizontal = AppTokens.dp.onboarding.recapChip.horizontalPadding,
                vertical = AppTokens.dp.onboarding.recapChip.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.onboarding.recapChip.space)
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.onboarding.recapChip.icon),
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
private fun OnboardingRecapChipsAllAvailablePreview() {
    PreviewContainer {
        OnboardingRecapChips(
            excludedMusclesCount = 0,
            missingEquipmentCount = 0,
        )
    }
}

@AppPreview
@Composable
private fun OnboardingRecapChipsCustomizedPreview() {
    PreviewContainer {
        OnboardingRecapChips(
            excludedMusclesCount = 3,
            missingEquipmentCount = 5,
        )
    }
}
