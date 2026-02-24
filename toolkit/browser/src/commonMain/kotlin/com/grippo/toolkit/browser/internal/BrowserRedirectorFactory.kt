package com.grippo.toolkit.browser.internal

import com.grippo.toolkit.browser.BrowserRedirector
import com.grippo.toolkit.context.NativeContext

internal expect fun NativeContext.getBrowserRedirector(): BrowserRedirector
