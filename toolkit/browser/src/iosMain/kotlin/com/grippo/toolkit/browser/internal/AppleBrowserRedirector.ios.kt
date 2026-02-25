package com.grippo.toolkit.browser.internal

import com.grippo.toolkit.browser.BrowserOpenRequest
import com.grippo.toolkit.browser.BrowserOpenResult
import com.grippo.toolkit.browser.BrowserOpenRoute
import com.grippo.toolkit.browser.BrowserRedirector
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal class AppleBrowserRedirector : BrowserRedirector {

    override fun tryOpen(request: BrowserOpenRequest): BrowserOpenResult {
        val strategy = BrowserOpenStrategyResolver.resolve(request.url, request.policy)
        val nsUrl = NSURL(string = request.url)
        val application = UIApplication.sharedApplication

        if (!application.canOpenURL(nsUrl)) {
            return BrowserOpenResult(
                isOpened = false,
                strategy = strategy,
            )
        }

        val opened = application.openURL(nsUrl)

        return BrowserOpenResult(
            isOpened = opened,
            strategy = strategy,
            route = BrowserOpenRoute.System,
        )
    }
}
