package com.grippo.state.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_load_high
import com.grippo.design.resources.provider.muscle_load_low
import com.grippo.design.resources.provider.muscle_load_medium

@Immutable
public enum class MuscleLoadEnumState {
    HIGH,
    MEDIUM,
    LOW;

    @Composable
    public fun title(): String {
        val r = when (this) {
            HIGH -> Res.string.muscle_load_high
            MEDIUM -> Res.string.muscle_load_medium
            LOW -> Res.string.muscle_load_low
        }
        return AppTokens.strings.res(r)
    }
}