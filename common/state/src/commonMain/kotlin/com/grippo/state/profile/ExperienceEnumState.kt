package com.grippo.state.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.grippo.design.resources.provider.icons.Emoji
import com.grippo.design.resources.provider.icons.EmojiLookTop
import com.grippo.design.resources.provider.icons.EmojiSatisfied
import com.grippo.design.resources.provider.icons.EmojiTalkingHappy
import com.grippo.state.formatters.UiText

@Immutable
public enum class ExperienceEnumState {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    PRO;

    public fun title(): UiText {
        return when (this) {
            BEGINNER -> UiText.Res(Res.string.experience_beginner_title)
            INTERMEDIATE -> UiText.Res(Res.string.experience_intermediate_title)
            ADVANCED -> UiText.Res(Res.string.experience_advanced_title)
            PRO -> UiText.Res(Res.string.experience_pro_title)
        }
    }

    public fun description(): UiText {
        return when (this) {
            BEGINNER -> UiText.Res(Res.string.experience_beginner_description)
            INTERMEDIATE -> UiText.Res(Res.string.experience_intermediate_description)
            ADVANCED -> UiText.Res(Res.string.experience_advanced_description)
            PRO -> UiText.Res(Res.string.experience_pro_description)
        }
    }

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
            BEGINNER -> AppTokens.icons.EmojiLookTop
            INTERMEDIATE -> AppTokens.icons.Emoji
            ADVANCED -> AppTokens.icons.EmojiSatisfied
            PRO -> AppTokens.icons.EmojiTalkingHappy
        }
    }
}