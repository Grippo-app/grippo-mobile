package com.grippo.toolkit.browser.internal

import com.grippo.toolkit.browser.BrowserOpenRequest
import com.grippo.toolkit.browser.BrowserOpenResult
import com.grippo.toolkit.browser.BrowserRedirector
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal class AppleBrowserRedirector : BrowserRedirector {

    override fun tryOpen(request: BrowserOpenRequest): BrowserOpenResult {
        val nsUrl = NSURL(string = request.url)
        val application = UIApplication.sharedApplication

        if (!application.canOpenURL(nsUrl)) {
            return BrowserOpenResult(
                isOpened = false,
                target = request.target,
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
            target = request.target,
        )
    }
}
