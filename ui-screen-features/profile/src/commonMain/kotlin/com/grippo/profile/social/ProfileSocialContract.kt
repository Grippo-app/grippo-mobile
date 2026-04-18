package com.grippo.profile.social

import androidx.compose.runtime.Immutable
import com.grippo.core.state.menu.SocialMenu

@Immutable
internal interface ProfileSocialContract {
    fun onOpenLink(value: SocialMenu)
    fun onBack()

    @Immutable
    companion object Empty : ProfileSocialContract {
        override fun onOpenLink(value: SocialMenu) {}
        override fun onBack() {}
    }
}
