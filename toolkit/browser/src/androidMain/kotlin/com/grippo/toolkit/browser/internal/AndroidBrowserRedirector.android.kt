package com.grippo.toolkit.browser.internal

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.grippo.toolkit.browser.BrowserRedirector

internal class AndroidBrowserRedirector(
    private val context: Context,
) : BrowserRedirector {

    override fun open(url: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resolved = intent.resolveActivity(context.packageManager) != null
            if (!resolved) return false

            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
