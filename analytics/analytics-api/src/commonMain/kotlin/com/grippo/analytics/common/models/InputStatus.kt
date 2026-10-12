package com.grippo.analytics.common.models

/** Bounded validation facts; original input values never cross the analytics boundary. */
public enum class InputStatus {
    Empty,
    Invalid,
    Valid,
}
