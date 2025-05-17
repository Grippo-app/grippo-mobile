package com.grippo.error.provider

public interface ErrorProvider {
    public fun display(
        title: String,
        description: String,
    )
}