package com.grippo.toolkit.link.opener

public data class LinkOpenResult(
    public val isOpened: Boolean,
    public val resolvedHandler: String? = null,
)
