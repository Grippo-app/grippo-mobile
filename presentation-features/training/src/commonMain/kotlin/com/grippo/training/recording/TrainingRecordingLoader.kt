package com.grippo.training.recording

import com.grippo.core.models.BaseLoader

internal sealed interface TrainingRecordingLoader : BaseLoader {
    data object Training : TrainingRecordingLoader
}