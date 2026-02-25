package com.grippo.toolkit.browser

public interface BrowserRedirector {
    public fun open(url: String): BrowserOpenResult
}
