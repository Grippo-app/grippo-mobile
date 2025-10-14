package com.grippo.platform.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.preferredLanguages
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

public actual object LocalAppLocale {

    public actual val current: String
        @Composable get() {
            // Otherwise follow the system (and re-read on foreground/active)
            val systemTag = rememberSystemTag()
            return normalizeTag(systemTag)
        }

    public actual fun current(): String = systemTagNow()
}

// ---------- helpers ----------

@Composable
private fun rememberSystemTag(): String {
    var tag by remember { mutableStateOf(systemTagNow()) }

    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observers = mutableListOf<Any>()
        fun observe(name: String) {
            observers += center.addObserverForName(name, `object` = null, queue = null) { _ ->
                tag = systemTagNow()
            }
        }
        UIApplicationWillEnterForegroundNotification?.let { observe(it) }
        UIApplicationDidBecomeActiveNotification?.let { observe(it) }
        onDispose { observers.forEach { center.removeObserver(it) } }
    }
    return tag
}

private fun systemTagNow(): String {
    // NSLocale.preferredLanguages returns BCP-47 tags like "uk-UA"
    val first = NSLocale.preferredLanguages.firstOrNull() as? String
    return first ?: "en"
}

private fun normalizeTag(tag: String): String {
    // Best-effort normalization for iOS (tags are already BCP-47).
    return tag.replace('_', '-')
}
