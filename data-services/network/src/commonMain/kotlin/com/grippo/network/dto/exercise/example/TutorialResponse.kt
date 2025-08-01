package com.grippo.network.dto.exercise.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class TutorialResponse(
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("exerciseExampleId")
    val exerciseExampleId: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("language")
    val language: String? = null,
    @SerialName("value")
    val value: String? = null,
    @SerialName("author")
    val author: String? = null,
    @SerialName("resourceType")
    val resourceType: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null
)