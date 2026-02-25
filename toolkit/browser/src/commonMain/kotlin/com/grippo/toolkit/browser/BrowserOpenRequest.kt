package com.grippo.toolkit.browser

public data class BrowserOpenRequest(
    public val url: String,
    public val target: BrowserOpenTarget = BrowserOpenTarget.Browser,
)
