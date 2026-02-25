package com.grippo.toolkit.browser.internal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.grippo.toolkit.browser.BrowserOpenRequest
import com.grippo.toolkit.browser.BrowserOpenResult
import com.grippo.toolkit.browser.BrowserOpenRoute
import com.grippo.toolkit.browser.BrowserOpenStrategy
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
                strategy = BrowserOpenStrategyResolver.resolve(request.url, request.policy),
            )

        val strategy = BrowserOpenStrategyResolver.resolve(request.url, request.policy)

        val result = when (strategy) {
            BrowserOpenStrategy.BrowserFirst -> openInBrowser(uri, strategy)
                ?: openGeneric(uri, strategy)

            BrowserOpenStrategy.AppFirst -> openGeneric(uri, strategy)
                ?: openInBrowser(uri, strategy)
        }

        return result ?: BrowserOpenResult(
            isOpened = false,
            strategy = strategy,
        )
    }

    private fun openGeneric(uri: Uri, strategy: BrowserOpenStrategy): BrowserOpenResult? {
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
            strategy = strategy,
            strategyRoute = BrowserOpenRoute.App,
            resolvedHandler = resolvedPackage,
        )
    }

    private fun openInBrowser(uri: Uri, strategy: BrowserOpenStrategy): BrowserOpenResult? {
        val pm = context.packageManager
        val baseIntent = baseViewIntent(uri)
        val browserPackage = findBrowserPackage(pm = pm, uri = uri)

        val intent = if (browserPackage != null) {
            Intent(baseIntent).apply { `package` = browserPackage }
        } else {
            baseIntent
        }

        if (intent.resolveActivity(pm) == null) {
            return null
        }

        val route =
            if (browserPackage == null) BrowserOpenRoute.System else BrowserOpenRoute.Browser

        return startActivity(
            intent = intent,
            strategy = strategy,
            strategyRoute = route,
            resolvedHandler = browserPackage,
        )
    }

    private fun startActivity(
        intent: Intent,
        strategy: BrowserOpenStrategy,
        strategyRoute: BrowserOpenRoute,
        resolvedHandler: String?,
    ): BrowserOpenResult? {
        return runCatching {
            context.startActivity(intent)
            BrowserOpenResult(
                isOpened = true,
                strategy = strategy,
                route = strategyRoute,
                resolvedHandler = resolvedHandler,
            )
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
            ?: filteredHandlers.firstOrNull(::looksLikeBrowserPackage)
            ?: filteredHandlers.firstOrNull()
    }

    private fun isKnownNonBrowserHandler(packageName: String): Boolean {
        return packageName == "com.google.android.youtube" ||
                packageName.contains("youtube", ignoreCase = true) ||
                packageName.contains("instagram", ignoreCase = true) ||
                packageName.contains("tiktok", ignoreCase = true)
    }

    private fun looksLikeBrowserPackage(packageName: String): Boolean {
        val candidates = listOf(
            "browser",
            "chrome",
            "firefox",
            "brave",
            "opera",
            "vivaldi",
            "duckduckgo",
            "edge",
            "emmx",
            "sbrowser",
        )

        return candidates.any { token ->
            packageName.contains(token, ignoreCase = true)
        }
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
