package com.grippo.network.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class IdsBody(
    @SerialName("ids")
    val ids: List<String?>? = null
)