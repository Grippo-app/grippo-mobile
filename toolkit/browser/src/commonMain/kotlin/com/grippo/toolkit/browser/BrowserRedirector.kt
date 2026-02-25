package com.grippo.toolkit.browser

public interface BrowserRedirector {
    public fun open(url: String): Boolean = tryOpen(BrowserOpenRequest(url)).isOpened
    public fun tryOpen(request: BrowserOpenRequest): BrowserOpenResult
}
