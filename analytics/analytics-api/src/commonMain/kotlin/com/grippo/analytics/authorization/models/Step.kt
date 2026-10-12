package com.grippo.analytics.authorization.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode
import kotlinx.serialization.Serializable

@Serializable
public enum class Step(public override val analyticsCode: String) : AnalyticsCode {
    Login("login"),
    Registration("registration")
}
