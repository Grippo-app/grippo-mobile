package com.grippo.toolkit.local.notification

public data class AppNotification(
    val id: Int,
    val title: String,
    val body: String,
    val deeplink: String? = null,
)
