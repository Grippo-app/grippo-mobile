package com.grippo.data.features.api.authorization

import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.user.UserFeature

public class DeleteProfileUseCase(
    private val localSettingsFeature: LocalSettingsFeature,
    private val userFeature: UserFeature
) {
    public suspend fun execute() {
        localSettingsFeature.clear()
        userFeature.deleteProfile().getOrThrow()
    }
}
