package com.grippo.wheel.picker.internal

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue

internal fun calculateAnimatedAlpha(
    layoutInfo: LazyListLayoutInfo,
    index: Int,
    rowCount: Int
): Float {
    val item = layoutInfo.visibleItemsInfo.find { it.index == index } ?: return 0.2f
    val center = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
    val itemCenter = item.offset + item.size / 2
    val distance = (itemCenter - center).absoluteValue
    val maxDistance = layoutInfo.viewportSize.height.toFloat() / rowCount

    return if (distance <= maxDistance) {
        lerp(0.2f, 1.0f, 1f - (distance / maxDistance))
    } else 0.2f
}

internal fun calculateAnimatedRotationX(
    layoutInfo: LazyListLayoutInfo,
    index: Int,
    rowCount: Int
): Float {
    val item = layoutInfo.visibleItemsInfo.find { it.index == index } ?: return 0f
    val center = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
    val itemCenter = item.offset + item.size / 2
    val distance = itemCenter - center
    val maxDistance = layoutInfo.viewportSize.height.toFloat() / rowCount

    return (-20f * (distance / maxDistance)).coerceIn(-90f, 90f)
}

internal fun LazyListLayoutInfo.closestItemToCenter(): LazyListItemInfo? {
    val center = viewportStartOffset + viewportSize.height / 2
    return visibleItemsInfo.minByOrNull { item ->
        val itemCenter = item.offset + item.size / 2
        (itemCenter - center).absoluteValue
    }
}