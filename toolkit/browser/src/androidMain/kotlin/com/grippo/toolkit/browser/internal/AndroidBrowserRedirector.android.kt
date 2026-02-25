package com.grippo.toolkit.browser.internal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.grippo.toolkit.browser.BrowserOpenRequest
import com.grippo.toolkit.browser.BrowserOpenResult
import com.grippo.toolkit.browser.BrowserOpenTarget
import com.grippo.toolkit.browser.BrowserRedirector

internal class AndroidBrowserRedirector(
    private val context: Context,
) : BrowserRedirector {

    override fun tryOpen(request: BrowserOpenRequest): BrowserOpenResult {
        val uri = runCatching { Uri.parse(request.url) }
            .getOrNull()
            ?.takeIf { !it.scheme.isNullOrBlank() }
            ?: return BrowserOpenResult(
                isOpened = false,
                target = request.target,
            )

        val result = when (request.target) {
            BrowserOpenTarget.Browser -> openInBrowser(uri)
            BrowserOpenTarget.System -> openSystem(uri)
        }

        return result ?: BrowserOpenResult(
            isOpened = false,
            target = request.target,
        )
    }

    private fun openSystem(uri: Uri): BrowserOpenResult? {
        val pm = context.packageManager
        val intent = baseViewIntent(uri)

        if (intent.resolveActivity(pm) == null) {
            return null
        }

        val resolvedPackage = runCatching {
            intent.resolveActivityInfo(pm, PackageManager.MATCH_DEFAULT_ONLY)?.packageName
        }.getOrNull()

        return startActivity(
            intent = intent,
            target = BrowserOpenTarget.System,
            resolvedHandler = resolvedPackage,
        )
    }

    private fun openInBrowser(uri: Uri): BrowserOpenResult? {
        val pm = context.packageManager
        val browserPackage = findBrowserPackage(pm = pm, uri = uri)
            ?: return null
        val intent = Intent(baseViewIntent(uri)).apply { `package` = browserPackage }

        if (intent.resolveActivity(pm) == null) {
            return null
        }

        return startActivity(
            intent = intent,
            target = BrowserOpenTarget.Browser,
            resolvedHandler = browserPackage,
        )
    }

    private fun startActivity(
        intent: Intent,
        target: BrowserOpenTarget,
        resolvedHandler: String?
    ): BrowserOpenResult? {
        return runCatching {
            context.startActivity(intent)
            BrowserOpenResult(isOpened = true, target = target, resolvedHandler = resolvedHandler)
        }.getOrNull()
    }

    private fun baseViewIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun findBrowserPackage(pm: PackageManager, uri: Uri): String? {
        val handlers = pm.queryIntentActivities(
            Intent(Intent.ACTION_VIEW, uri).apply { addCategory(Intent.CATEGORY_BROWSABLE) },
            PackageManager.MATCH_DEFAULT_ONLY,
        ).mapNotNull { it.activityInfo?.packageName }
            .distinct()

        val filteredHandlers = handlers.filterNot(::isKnownNonBrowserHandler)

        return KnownBrowserPackages.firstOrNull(filteredHandlers::contains)
    }

    private fun isKnownNonBrowserHandler(packageName: String): Boolean {
        return packageName.contains("youtube", ignoreCase = true) ||
                packageName.contains("instagram", ignoreCase = true) ||
                packageName.contains("tiktok", ignoreCase = true)
    }

    private companion object {
        private val KnownBrowserPackages = listOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser",
            "com.sec.android.app.sbrowser",
        )
    }
}
