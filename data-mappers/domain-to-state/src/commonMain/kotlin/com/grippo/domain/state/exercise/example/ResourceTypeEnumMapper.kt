package com.grippo.domain.state.exercise.example

import com.grippo.core.state.examples.ResourceTypeEnumState
import com.grippo.data.features.api.exercise.example.models.ResourceTypeEnum

public fun ResourceTypeEnum.toState(): ResourceTypeEnumState {
    return when (this) {
        ResourceTypeEnum.YOUTUBE_VIDEO -> ResourceTypeEnumState.YOUTUBE_VIDEO
        ResourceTypeEnum.VIDEO -> ResourceTypeEnumState.VIDEO
        ResourceTypeEnum.TEXT -> ResourceTypeEnumState.TEXT
    }
}