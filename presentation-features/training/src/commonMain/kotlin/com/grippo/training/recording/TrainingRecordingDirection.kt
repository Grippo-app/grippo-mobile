package com.grippo.training.recording

import com.grippo.core.models.BaseDirection

public sealed interface TrainingRecordingDirection : BaseDirection {
    public data object ToSuccess : TrainingRecordingDirection
    public data object Back : TrainingRecordingDirection
}