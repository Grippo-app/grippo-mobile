package com.grippo.services.backend.dto.auth

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class AppleBody(
    @SerialName("code")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val code: String? = null,
)
