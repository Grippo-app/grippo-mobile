package com.grippo.toolkit.browser.internal

import com.grippo.toolkit.browser.BrowserOpenResult
import com.grippo.toolkit.browser.BrowserRedirector
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal class AppleBrowserRedirector : BrowserRedirector {

    override fun open(url: String): BrowserOpenResult {
        val nsUrl = NSURL(string = url)
        val application = UIApplication.sharedApplication

        if (!application.canOpenURL(nsUrl)) {
            return BrowserOpenResult(
                isOpened = false,
            )
        }

        val opened = runCatching {
            application.openURL(
                url = nsUrl,
                options = emptyMap<Any?, Any?>(),
                completionHandler = null,
            )
            true
        }.getOrDefault(false)

        return BrowserOpenResult(
            isOpened = opened,
        )
    }
}
