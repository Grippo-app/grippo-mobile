package com.grippo.profile.social

import androidx.compose.runtime.Immutable

@Immutable
internal interface ProfileSocialContract {
    fun onOpenLink(value: SocialChannel)
    fun onBack()

    @Immutable
    companion object Empty : ProfileSocialContract {
        override fun onOpenLink(value: SocialChannel) {}
        override fun onBack() {}
    }
}
