package com.grippo.core.models

import androidx.compose.runtime.Immutable

@Immutable
public enum class BackPriority(internal val value: Int) {
    Priority0(value = 0),
    Priority10(value = 10),
    Priority20(value = 20),
    Priority30(value = 30),
}
