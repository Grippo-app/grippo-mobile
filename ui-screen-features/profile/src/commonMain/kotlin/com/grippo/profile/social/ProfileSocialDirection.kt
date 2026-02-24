package com.grippo.profile.social

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface ProfileSocialDirection : BaseDirection {
    data object Back : ProfileSocialDirection
}
