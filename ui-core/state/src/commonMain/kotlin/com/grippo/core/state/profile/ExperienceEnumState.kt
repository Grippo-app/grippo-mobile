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
import com.grippo.design.resources.provider.icons.ExperienceAdvanced
import com.grippo.design.resources.provider.icons.ExperienceBeginner
import com.grippo.design.resources.provider.icons.ExperienceIntermediate
import com.grippo.design.resources.provider.icons.ExperiencePro
import com.grippo.design.resources.provider.welcome_motto_advanced
import com.grippo.design.resources.provider.welcome_motto_beginner
import com.grippo.design.resources.provider.welcome_motto_intermediate
import com.grippo.design.resources.provider.welcome_motto_pro

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
            BEGINNER -> AppTokens.icons.ExperienceBeginner
            INTERMEDIATE -> AppTokens.icons.ExperienceIntermediate
            ADVANCED -> AppTokens.icons.ExperienceAdvanced
            PRO -> AppTokens.icons.ExperiencePro
        }
    }

    @Composable
    public fun motto(): String {
        val resource = when (this) {
            BEGINNER -> Res.string.welcome_motto_beginner
            INTERMEDIATE -> Res.string.welcome_motto_intermediate
            ADVANCED -> Res.string.welcome_motto_advanced
            PRO -> Res.string.welcome_motto_pro
        }
        return AppTokens.strings.res(resource)
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