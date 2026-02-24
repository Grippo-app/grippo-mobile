package com.grippo.toolkit.browser.internal

import com.grippo.toolkit.browser.BrowserRedirector
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal class AppleBrowserRedirector : BrowserRedirector {

    override fun open(url: String): Boolean {
        val nsUrl = NSURL(string = url)
        val application = UIApplication.sharedApplication

        if (!application.canOpenURL(nsUrl)) return false

        return application.openURL(nsUrl)
    }
}
