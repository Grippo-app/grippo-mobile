package com.grippo.analytics.profile.internal.encoding

import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.toolkit.analytics.context.ScreenId

/** Stable wire identifiers stay internal; public screens describe rendered surfaces. */
internal object ProfileEncoding {
    fun screen(value: ProfileScreen): ScreenId {
        return ScreenId(
            when (value) {
                ProfileScreen.Menu -> "profile_menu"
                ProfileScreen.Body -> "profile_body"
                ProfileScreen.Goal -> "profile_goal"
                ProfileScreen.Experience -> "profile_experience"
                ProfileScreen.Muscles -> "profile_muscles"
                ProfileScreen.Equipments -> "profile_equipments"
                ProfileScreen.Settings -> "profile_settings"
                ProfileScreen.Social -> "profile_social"
            }
        )
    }
}
