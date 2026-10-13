package com.grippo.services.backend.dto.auth

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class GoogleBody(
    @SerialName("idToken")
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val idToken: String? = null,
)
