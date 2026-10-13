package com.grippo.profile.social

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.menu.SocialMenu
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.link.opener.LinkOpener

internal class ProfileSocialViewModel(
    private val analytics: ProfileAnalytics,
    private val linkOpener: LinkOpener
) : BaseViewModel<ProfileSocialState, ProfileSocialDirection, ProfileSocialLoader>(
    ProfileSocialState
), ProfileSocialContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(ProfileScreen.Social, reason)
    }

    override fun onOpenLink(value: SocialMenu) {
        value.urls.any { url -> linkOpener.open(url).isOpened }
    }

    override fun onBack() {
        navigateTo(ProfileSocialDirection.Back)
    }
}
