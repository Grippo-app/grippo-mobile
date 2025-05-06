package com.grippo.dialog.api

import kotlinx.serialization.Serializable

@Serializable
public sealed interface DialogConfig {
    @Serializable
    public data class WeightPicker(
        val initial: Float,
        val onResult: (value: Float) -> Unit
    ) : DialogConfig

    @Serializable
    public data class HeightPicker(
        val initial: Int,
        val onResult: (value: Int) -> Unit
    ) : DialogConfig
}