package com.grippo.profile.social

import com.grippo.core.foundation.BaseViewModel
import com.grippo.toolkit.link.opener.LinkOpener

internal class ProfileSocialViewModel(
    private val linkOpener: LinkOpener
) : BaseViewModel<ProfileSocialState, ProfileSocialDirection, ProfileSocialLoader>(
    ProfileSocialState
), ProfileSocialContract {

    override fun onOpenLink(value: SocialChannel) {
        value.urls.any { url -> linkOpener.open(url).isOpened }
    }

    override fun onBack() {
        navigateTo(ProfileSocialDirection.Back)
    }
}
