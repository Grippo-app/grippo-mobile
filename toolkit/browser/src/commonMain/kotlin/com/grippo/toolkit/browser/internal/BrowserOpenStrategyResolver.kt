package com.grippo.toolkit.browser.internal

import com.grippo.toolkit.browser.BrowserOpenPolicy
import com.grippo.toolkit.browser.BrowserOpenStrategy

internal object BrowserOpenStrategyResolver {

    fun resolve(url: String, policy: BrowserOpenPolicy): BrowserOpenStrategy {
        if (policy != BrowserOpenPolicy.Automatic) {
            return policy.toStrategy()
        }

        return hostStrategy(extractHost(url))
    }

    private fun hostStrategy(host: String?): BrowserOpenStrategy {
        val normalizedHost = host?.lowercase().orEmpty()

        return when {
            normalizedHost.contains("youtube.com") || normalizedHost.contains("youtu.be") ->
                BrowserOpenStrategy.AppFirst

            normalizedHost.contains("instagram.com") ->
                BrowserOpenStrategy.AppFirst

            normalizedHost.contains("tiktok.com") ->
                BrowserOpenStrategy.AppFirst

            else -> BrowserOpenStrategy.BrowserFirst
        }
    }

    private fun BrowserOpenPolicy.toStrategy(): BrowserOpenStrategy {
        return when (this) {
            BrowserOpenPolicy.Automatic -> BrowserOpenStrategy.BrowserFirst
            BrowserOpenPolicy.BrowserFirst -> BrowserOpenStrategy.BrowserFirst
            BrowserOpenPolicy.AppFirst -> BrowserOpenStrategy.AppFirst
        }
    }

    private fun extractHost(url: String): String? {
        val schemeSeparatorIndex = url.indexOf("://")
        if (schemeSeparatorIndex < 0) return null

        val afterSchemeIndex = schemeSeparatorIndex + "://".length
        if (afterSchemeIndex >= url.length) return null

        val hostEndIndex = buildList {
            add(url.indexOf('/', afterSchemeIndex))
            add(url.indexOf('?', afterSchemeIndex))
            add(url.indexOf('#', afterSchemeIndex))
        }.filter { it >= 0 }.minOrNull() ?: url.length

        if (hostEndIndex <= afterSchemeIndex) return null

        return url.substring(afterSchemeIndex, hostEndIndex)
            .substringBefore('@')
            .substringBefore(':')
            .trim()
            .takeIf(String::isNotEmpty)
    }
}
