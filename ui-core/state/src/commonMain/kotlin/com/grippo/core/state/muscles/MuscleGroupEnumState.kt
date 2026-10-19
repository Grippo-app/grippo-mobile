package com.grippo.core.state.muscles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_group_abdominal_muscles
import com.grippo.design.resources.provider.muscle_group_arms_and_forearms
import com.grippo.design.resources.provider.muscle_group_back_muscles
import com.grippo.design.resources.provider.muscle_group_chest_muscles
import com.grippo.design.resources.provider.muscle_group_legs
import com.grippo.design.resources.provider.muscle_group_shoulder_muscles
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.design.resources.provider.muscles.bodyBack
import com.grippo.design.resources.provider.muscles.bodyFront
import com.grippo.design.resources.provider.muscles.bodySplit
import com.grippo.design.resources.provider.muscles.legsSplit

@Immutable
public enum class MuscleGroupEnumState {
    CHEST_MUSCLES,
    BACK_MUSCLES,
    ABDOMINAL_MUSCLES,
    LEGS,
    ARMS_AND_FOREARMS,
    SHOULDER_MUSCLES;

    public fun title(): UiText = TITLES.getValue(this)

    @Composable
    public fun color(): Color {
        return AppTokens.colors.muscle.highlight
    }

    @Composable
    public fun icon(color: Color = color()): ImageVector {
        val colors = AppTokens.colors.muscle
        val identity = color
        val preset = remember(this, colors, identity) {
            MuscleEnumState.colorPreset(
                fallback = colors.inactive,
                outline = colors.outline,
                background = colors.background,
                colorFor = { muscle ->
                    if (muscle.group() == this) identity else colors.inactive
                },
            )
        }
        return image(preset)
    }

    @Composable
    public fun image(preset: MuscleColorPreset): ImageVector = remember(this, preset) {
        when (this) {
            CHEST_MUSCLES -> bodyFront(preset)
            BACK_MUSCLES -> bodyBack(preset)
            ABDOMINAL_MUSCLES -> bodyFront(preset)
            LEGS -> legsSplit(preset)
            ARMS_AND_FOREARMS -> bodySplit(preset)
            SHOULDER_MUSCLES -> bodySplit(preset)
        }
    }

    public companion object {
        private val TITLES: Map<MuscleGroupEnumState, UiText> = entries.associateWith {
            UiText.Res(
                when (it) {
                    CHEST_MUSCLES -> Res.string.muscle_group_chest_muscles
                    BACK_MUSCLES -> Res.string.muscle_group_back_muscles
                    ABDOMINAL_MUSCLES -> Res.string.muscle_group_abdominal_muscles
                    LEGS -> Res.string.muscle_group_legs
                    ARMS_AND_FOREARMS -> Res.string.muscle_group_arms_and_forearms
                    SHOULDER_MUSCLES -> Res.string.muscle_group_shoulder_muscles
                }
            )
        }
    }
}
