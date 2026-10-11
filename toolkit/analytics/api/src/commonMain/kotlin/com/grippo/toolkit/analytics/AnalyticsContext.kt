package com.grippo.toolkit.analytics

import kotlinx.serialization.Serializable

private val codePattern = Regex("[a-z][a-z0-9_]{0,99}")
private val opaqueIdPattern = Regex("[A-Za-z0-9_-]{1,64}")

private fun requireCode(value: String) {
    require(codePattern.matches(value)) { "Expected a stable analytics code" }
}

@Serializable
public data class ScreenId(override val analyticsCode: String) : AnalyticsCode {
    init {
        requireCode(analyticsCode)
    }
}

@Serializable
public data class DialogId(override val analyticsCode: String) : AnalyticsCode {
    init {
        requireCode(analyticsCode)
    }
}

@Serializable
public data class EntryPoint(override val analyticsCode: String) : AnalyticsCode {
    init {
        requireCode(analyticsCode)
    }
}

@Serializable
public data class FlowId(public val value: String) {
    init {
        require(opaqueIdPattern.matches(value))
    }
}

@Serializable
public data class OperationId(public val value: String) {
    init {
        require(opaqueIdPattern.matches(value))
    }
}

/** Opaque backend identifier. Never email, name, token, or a hash of these. */
public data class AnalyticsUserId(public val value: String) {
    init {
        require(opaqueIdPattern.matches(value))
    }
}

@Serializable
public enum class LaunchSource(override val analyticsCode: String) : AnalyticsCode {
    Unknown("unknown"), Launcher("launcher"), Notification("notification"), DeepLink("deep_link")
}

@Serializable
public enum class NavigationReason(override val analyticsCode: String) : AnalyticsCode {
    Initial("initial"), Action("action"), Back("back"), Restore("restore"), External("external")
}

@Serializable
public data class NavigationContext(
    public val screen: ScreenId,
    public val referrer: ScreenId? = null,
    public val reason: NavigationReason,
    public val dialog: DialogId? = null,
)

@Serializable
public data class AnalyticsContext(
    public val navigation: NavigationContext? = null,
    public val launchSource: LaunchSource = LaunchSource.Unknown,
)

/** Keep this with the flow/route/draft, rather than a global last-entry-point. */
@Serializable
public data class FlowContext(
    public val id: FlowId,
    public val entryPoint: EntryPoint,
    public val originScreen: ScreenId?,
)

public class UserPropertyKey<T : AnalyticsCode>(public val name: String) {
    public fun value(value: T): AnalyticsUserProperty =
        AnalyticsUserProperty(this, value.analyticsCode)
}

public class AnalyticsUserProperty internal constructor(
    public val key: UserPropertyKey<*>,
    public val value: String,
)

public class AnalyticsIdentity(
    public val userId: AnalyticsUserId? = null,
    properties: List<AnalyticsUserProperty> = emptyList(),
) {
    public val properties: List<AnalyticsUserProperty> = properties.toList()
}

/** Definitions are reusable; creating an event never sends it automatically. */
public class ScreenViewed(public val screen: ScreenId) : AnalyticsEvent {
    override val definition: EventDefinition = Definition
    override fun parameters(): List<AnalyticsParameter> = listOf(Screen.value(screen))

    public companion object {
        private val Screen: ParameterKey<ScreenId> = ParameterKey.code("screen_name")
        public val Definition: EventDefinition = EventDefinition("screen_view", 1, listOf(Screen))
    }
}

public class DialogViewed(public val dialog: DialogId) : AnalyticsEvent {
    override val definition: EventDefinition = Definition
    override fun parameters(): List<AnalyticsParameter> = listOf(Dialog.value(dialog))

    public companion object {
        private val Dialog: ParameterKey<DialogId> = ParameterKey.code("dialog_name")
        public val Definition: EventDefinition = EventDefinition("dialog_viewed", 1, listOf(Dialog))
    }
}
