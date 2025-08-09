package com.grippo.state.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.icons.Emoji
import com.grippo.design.resources.icons.EmojiLookTop
import com.grippo.design.resources.icons.EmojiSatisfied
import com.grippo.design.resources.icons.EmojiTalkingHappy
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.experience_advanced_description
import com.grippo.design.resources.provider.experience_advanced_title
import com.grippo.design.resources.provider.experience_beginner_description
import com.grippo.design.resources.provider.experience_beginner_title
import com.grippo.design.resources.provider.experience_intermediate_description
import com.grippo.design.resources.provider.experience_intermediate_title
import com.grippo.design.resources.provider.experience_pro_description
import com.grippo.design.resources.provider.experience_pro_title

@Immutable
public enum class ExperienceEnumState {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    PRO;

    @Composable
    public fun title(): String {
        return when (this) {
            BEGINNER -> AppTokens.strings.res(Res.string.experience_beginner_title)
            INTERMEDIATE -> AppTokens.strings.res(Res.string.experience_intermediate_title)
            ADVANCED -> AppTokens.strings.res(Res.string.experience_advanced_title)
            PRO -> AppTokens.strings.res(Res.string.experience_pro_title)
        }
    }

    @Composable
    public fun description(): String {
        return when (this) {
            BEGINNER -> AppTokens.strings.res(Res.string.experience_beginner_description)
            INTERMEDIATE -> AppTokens.strings.res(Res.string.experience_intermediate_description)
            ADVANCED -> AppTokens.strings.res(Res.string.experience_advanced_description)
            PRO -> AppTokens.strings.res(Res.string.experience_pro_description)
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