package com.grippo.analytics.authorization.models

import com.grippo.toolkit.analytics.schema.AnalyticsCode

public enum class FailureCategory(public override val analyticsCode: String) : AnalyticsCode {
    Offline("offline"),
    Timeout("timeout"),
    Rejected("rejected"),
    Server("server"),
    ProviderConfiguration("provider_configuration"),
    ProviderCredential("provider_credential"),
    ProviderInterrupted("provider_interrupted"),
    Unexpected("unexpected")
}
