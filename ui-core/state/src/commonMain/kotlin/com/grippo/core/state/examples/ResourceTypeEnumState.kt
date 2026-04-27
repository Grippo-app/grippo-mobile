package com.grippo.core.state.examples

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.resource_type_text
import com.grippo.design.resources.provider.resource_type_video
import com.grippo.design.resources.provider.resource_type_youtube_video

@Immutable
public enum class ResourceTypeEnumState {
    YOUTUBE_VIDEO,
    VIDEO,
    TEXT;

    public fun title(): UiText = TITLES.getValue(this)

    public companion object {
        private val TITLES: Map<ResourceTypeEnumState, UiText> = entries.associateWith {
            UiText.Res(
                when (it) {
                    YOUTUBE_VIDEO -> Res.string.resource_type_youtube_video
                    VIDEO -> Res.string.resource_type_video
                    TEXT -> Res.string.resource_type_text
                }
            )
        }
    }
}