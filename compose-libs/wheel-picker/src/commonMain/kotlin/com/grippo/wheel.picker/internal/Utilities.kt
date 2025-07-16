package com.grippo.wheel.picker.internal

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue

internal fun calculateAnimatedAlpha(
    visibleItems: List<LazyListItemInfo>,
    index: Int,
    center: Int,
    maxDistance: Float
): Float {
    val item = visibleItems.find { it.index == index } ?: return 0.2f
    val itemCenter = item.offset + item.size / 2
    val distance = (itemCenter - center).absoluteValue
    return if (distance <= maxDistance) {
        lerp(0.2f, 1.0f, 1f - (distance / maxDistance))
    } else 0.2f
}

internal fun calculateAnimatedRotationX(
    visibleItems: List<LazyListItemInfo>,
    index: Int,
    center: Int,
    maxDistance: Float
): Float {
    val item = visibleItems.find { it.index == index } ?: return 0f
    val itemCenter = item.offset + item.size / 2
    val distance = itemCenter - center
    return (-20f * (distance / maxDistance)).coerceIn(-90f, 90f)
}

internal fun LazyListLayoutInfo.closestItemToCenter(): LazyListItemInfo? {
    val center = viewportStartOffset + viewportSize.height / 2
    return visibleItemsInfo.minByOrNull { item ->
        val itemCenter = item.offset + item.size / 2
        (itemCenter - center).absoluteValue
    }
}