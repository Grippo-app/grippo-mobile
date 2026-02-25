package com.grippo.toolkit.browser.internal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.grippo.toolkit.browser.BrowserOpenResult
import com.grippo.toolkit.browser.BrowserRedirector

internal class AndroidBrowserRedirector(private val context: Context) : BrowserRedirector {

    override fun open(url: String): BrowserOpenResult {
        val uri = runCatching { Uri.parse(url) }
            .getOrNull()
            ?.takeIf { !it.scheme.isNullOrBlank() }
            ?: return BrowserOpenResult(
                isOpened = false,
            )

        val result = openSystem(uri)

        return result ?: BrowserOpenResult(
            isOpened = false,
        )
    }

    private fun openSystem(uri: Uri): BrowserOpenResult? {
        val pm = context.packageManager
        val intent = baseViewIntent(uri)

        val resolvedPackage = runCatching {
            intent.resolveActivityInfo(pm, PackageManager.MATCH_DEFAULT_ONLY)?.packageName
        }.getOrNull()

        return startActivity(
            intent = intent,
            resolvedHandler = resolvedPackage,
        )
    }

    private fun startActivity(
        intent: Intent,
        resolvedHandler: String?,
    ): BrowserOpenResult? {
        return runCatching {
            context.startActivity(intent)
            BrowserOpenResult(isOpened = true, resolvedHandler = resolvedHandler)
        }.getOrNull()
    }

    private fun baseViewIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
