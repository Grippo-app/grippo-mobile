package com.grippo.core.state.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.formatters.UiText
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.experience_advanced_description
import com.grippo.design.resources.provider.experience_advanced_title
import com.grippo.design.resources.provider.experience_beginner_description
import com.grippo.design.resources.provider.experience_beginner_title
import com.grippo.design.resources.provider.experience_intermediate_description
import com.grippo.design.resources.provider.experience_intermediate_title
import com.grippo.design.resources.provider.experience_pro_description
import com.grippo.design.resources.provider.experience_pro_title
import com.grippo.design.resources.provider.icons.Advanced
import com.grippo.design.resources.provider.icons.Beginner
import com.grippo.design.resources.provider.icons.Intermediate
import com.grippo.design.resources.provider.icons.Trophy

@Immutable
public enum class ExperienceEnumState {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    PRO;

    public fun title(): UiText = TITLES.getValue(this)
    public fun description(): UiText = DESCRIPTIONS.getValue(this)

    @Composable
    public fun color(): Color {
        return when (this) {
            BEGINNER -> AppTokens.colors.profile.experience.beginner
            INTERMEDIATE -> AppTokens.colors.profile.experience.intermediate
            ADVANCED -> AppTokens.colors.profile.experience.advanced
            PRO -> AppTokens.colors.profile.experience.pro
        }
    }

    @Composable
    public fun icon(): ImageVector {
        return when (this) {
            BEGINNER -> AppTokens.icons.Beginner
            INTERMEDIATE -> AppTokens.icons.Intermediate
            ADVANCED -> AppTokens.icons.Advanced
            PRO -> AppTokens.icons.Trophy
        }
    }

    public companion object {
        private val TITLES: Map<ExperienceEnumState, UiText> = entries.associateWith {
            UiText.Res(
                when (it) {
                    BEGINNER -> Res.string.experience_beginner_title
                    INTERMEDIATE -> Res.string.experience_intermediate_title
                    ADVANCED -> Res.string.experience_advanced_title
                    PRO -> Res.string.experience_pro_title
                }
            )
        }
        private val DESCRIPTIONS: Map<ExperienceEnumState, UiText> = entries.associateWith {
            UiText.Res(
                when (it) {
                    BEGINNER -> Res.string.experience_beginner_description
                    INTERMEDIATE -> Res.string.experience_intermediate_description
                    ADVANCED -> Res.string.experience_advanced_description
                    PRO -> Res.string.experience_pro_description
                }
            )
        }
    }
}