package com.grippo.toolkit.link.opener.internal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.grippo.toolkit.link.opener.LinkOpenResult
import com.grippo.toolkit.link.opener.LinkOpener

internal class AndroidLinkOpener(private val context: Context) : LinkOpener {

    override fun open(url: String): LinkOpenResult {
        val uri = runCatching { Uri.parse(url) }
            .getOrNull()
            ?.takeIf { !it.scheme.isNullOrBlank() }
            ?: return LinkOpenResult(
                isOpened = false,
            )

        val result = openSystem(uri)

        return result ?: LinkOpenResult(
            isOpened = false,
        )
    }

    private fun openSystem(uri: Uri): LinkOpenResult? {
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
    ): LinkOpenResult? {
        return runCatching {
            context.startActivity(intent)
            LinkOpenResult(isOpened = true, resolvedHandler = resolvedHandler)
        }.getOrNull()
    }

    private fun baseViewIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
