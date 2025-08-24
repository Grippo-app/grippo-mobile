package com.grippo.profile

import com.grippo.core.BaseViewModel

public class ProfileViewModel :
    BaseViewModel<ProfileState, ProfileDirection, ProfileLoader>(ProfileState), ProfileContract {

    override fun onBack() {
        navigateTo(ProfileDirection.Back)
    }
}