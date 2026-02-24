package com.grippo.profile.social

import com.grippo.core.foundation.BaseViewModel
import com.grippo.toolkit.browser.BrowserRedirector

internal class ProfileSocialViewModel(
    private val browserRedirector: BrowserRedirector
) :
    BaseViewModel<ProfileSocialState, ProfileSocialDirection, ProfileSocialLoader>(
        ProfileSocialState
    ),
    ProfileSocialContract {

    override fun onOpenLink(value: SocialChannel) {
        value.urls.any(browserRedirector::open)
    }

    override fun onBack() {
        navigateTo(ProfileSocialDirection.Back)
    }
}
