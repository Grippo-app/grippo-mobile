package com.grippo

import org.gradle.api.plugins.PluginManager

internal fun PluginManager.applySafely(pluginId: String) {
    if (!hasPlugin(pluginId)) {
        apply(pluginId)
    }
}