package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.ResourceTypeEnum
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.exercise.example.models.ResourceTypeEnumState

public fun ResourceTypeEnum.toState(): ResourceTypeEnumState? {
    return when (this) {
        ResourceTypeEnum.YOUTUBE_VIDEO -> ResourceTypeEnumState.YOUTUBE_VIDEO
        ResourceTypeEnum.VIDEO -> ResourceTypeEnumState.VIDEO
        ResourceTypeEnum.TEXT -> ResourceTypeEnumState.TEXT
        ResourceTypeEnum.UNIDENTIFIED -> AppLogger.checkOrLog(null) {
            "ResourceTypeEnum.UNIDENTIFIED cannot be mapped to state"
        }
    }
}