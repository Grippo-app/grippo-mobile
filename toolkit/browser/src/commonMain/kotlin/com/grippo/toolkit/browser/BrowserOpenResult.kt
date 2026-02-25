package com.grippo.toolkit.browser

public data class BrowserOpenResult(
    public val isOpened: Boolean,
    public val strategy: BrowserOpenStrategy,
    public val route: BrowserOpenRoute? = null,
    public val resolvedHandler: String? = null,
)

public enum class BrowserOpenStrategy {
    BrowserFirst,
    AppFirst,
}

public enum class BrowserOpenRoute {
    Browser,
    App,
    System,
}
