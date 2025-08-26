package com.grippo.state.exercise.examples

import androidx.compose.runtime.Immutable
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.resource_type_text
import com.grippo.design.resources.provider.resource_type_video
import com.grippo.design.resources.provider.resource_type_youtube_video
import com.grippo.state.formatters.UiText

@Immutable
public enum class ResourceTypeEnumState {
    YOUTUBE_VIDEO,
    VIDEO,
    TEXT;

    public fun title(): UiText {
        val r = when (this) {
            YOUTUBE_VIDEO -> Res.string.resource_type_youtube_video
            VIDEO -> Res.string.resource_type_video
            TEXT -> Res.string.resource_type_text
        }
        return UiText.Res(r)
    }
}