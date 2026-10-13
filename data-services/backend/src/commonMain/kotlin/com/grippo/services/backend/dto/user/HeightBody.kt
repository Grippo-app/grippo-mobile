package com.grippo.services.backend.dto.user

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class HeightBody(
    @SerialName("height")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val height: Int? = null,
)
