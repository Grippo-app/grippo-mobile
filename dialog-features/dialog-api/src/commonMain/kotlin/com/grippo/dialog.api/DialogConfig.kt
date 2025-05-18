package com.grippo.dialog.api

import kotlinx.serialization.Serializable

@Serializable
public sealed class DialogConfig(public open val onDismiss: (() -> Unit)?) {
    @Serializable
    public data class ErrorDisplay(
        val title: String,
        val description: String,
        val onClose: () -> Unit,
    ) : DialogConfig(onClose)

    @Serializable
    public data class WeightPicker(
        val initial: Float,
        val onResult: (value: Float) -> Unit,
    ) : DialogConfig(null)

    @Serializable
    public data class HeightPicker(
        val initial: Int,
        val onResult: (value: Int) -> Unit,
    ) : DialogConfig(null)
}