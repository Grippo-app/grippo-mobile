package com.grippo.state.exercise.examples

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.resource_type_text
import com.grippo.design.resources.provider.resource_type_video
import com.grippo.design.resources.provider.resource_type_youtube_video

@Immutable
public enum class ResourceTypeEnumState {
    YOUTUBE_VIDEO,
    VIDEO,
    TEXT;

    @Composable
    public fun title(): String {
        val r = when (this) {
            YOUTUBE_VIDEO -> Res.string.resource_type_youtube_video
            VIDEO -> Res.string.resource_type_video
            TEXT -> Res.string.resource_type_text
        }
        return AppTokens.strings.res(r)
    }
}