package com.grippo.analytics.authorization.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

public enum class AuthScreen(public override val analyticsCode: String) : AnalyticsCode {
    Splash("auth_splash"),
    Login("auth_login"),
    Registration("auth_registration"),
    User("profile_creation_user"),
    Experience("profile_creation_experience"),
    ExcludedMuscles("profile_creation_excluded_muscles"),
    MissingEquipments("profile_creation_missing_equipments"),
    Completed("profile_creation_completed"),
}
