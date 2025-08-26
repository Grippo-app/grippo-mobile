package com.grippo.state.muscles

import androidx.compose.runtime.Immutable
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_group_abdominal_muscles
import com.grippo.design.resources.provider.muscle_group_arms_and_forearms
import com.grippo.design.resources.provider.muscle_group_back_muscles
import com.grippo.design.resources.provider.muscle_group_chest_muscles
import com.grippo.design.resources.provider.muscle_group_legs
import com.grippo.design.resources.provider.muscle_group_shoulder_muscles
import com.grippo.state.formatters.UiText

@Immutable
public enum class MuscleGroupEnumState {
    CHEST_MUSCLES,
    BACK_MUSCLES,
    ABDOMINAL_MUSCLES,
    LEGS,
    ARMS_AND_FOREARMS,
    SHOULDER_MUSCLES;

    public fun title(): UiText {
        val r = when (this) {
            CHEST_MUSCLES -> Res.string.muscle_group_chest_muscles
            BACK_MUSCLES -> Res.string.muscle_group_back_muscles
            ABDOMINAL_MUSCLES -> Res.string.muscle_group_abdominal_muscles
            LEGS -> Res.string.muscle_group_legs
            ARMS_AND_FOREARMS -> Res.string.muscle_group_arms_and_forearms
            SHOULDER_MUSCLES -> Res.string.muscle_group_shoulder_muscles
        }
        return UiText.Res(r)
    }
}