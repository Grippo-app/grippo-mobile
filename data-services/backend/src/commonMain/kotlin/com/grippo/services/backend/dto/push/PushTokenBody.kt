package com.grippo.services.backend.dto.push

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class PushTokenBody(
    @SerialName("token")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val token: String? = null,
)
