package com.grippo.toolkit.link.opener.internal

import com.grippo.toolkit.link.opener.LinkOpenResult
import com.grippo.toolkit.link.opener.LinkOpener
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal class AppleLinkOpener : LinkOpener {

    override fun open(url: String): LinkOpenResult {
        val nsUrl = NSURL(string = url)
        val application = UIApplication.sharedApplication

        if (!application.canOpenURL(nsUrl)) {
            return LinkOpenResult(
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

        return LinkOpenResult(
            isOpened = opened,
        )
    }
}
