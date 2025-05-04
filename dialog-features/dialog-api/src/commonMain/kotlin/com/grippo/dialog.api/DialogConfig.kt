package com.grippo.dialog.api

import kotlinx.serialization.Serializable

@Serializable
public sealed interface DialogConfig {
    @Serializable
    public data class WeightPicker(val initial: Float) : DialogConfig

    @Serializable
    public data class HeightPicker(val initial: Int) : DialogConfig
}